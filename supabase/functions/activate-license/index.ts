import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import {
  adminClient,
  clientIp,
  corsHeaders,
  jsonResponse,
  logEvent,
  normalizeLicenseKey,
  sha256Hex,
  signLicenseJwt,
} from "../_shared/license.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders() });
  }
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  try {
    const body = await req.json();
    const licenseKey = body.license_key as string | undefined;
    const deviceHash = body.device_hash as string | undefined;
    const deviceLabel = (body.device_label as string | undefined) ?? null;
    const appVersion = (body.app_version as string | undefined) ?? null;

    if (!licenseKey || !deviceHash || deviceHash.length < 16) {
      return jsonResponse({ error: "license_key and device_hash are required" }, 400);
    }

    const normalized = normalizeLicenseKey(licenseKey);
    const keyHash = await sha256Hex(normalized);
    const ip = clientIp(req);
    const supabase = adminClient();

    const { data: keyRow, error: keyErr } = await supabase
      .from("receiptvault_license_keys")
      .select("*")
      .eq("key_hash", keyHash)
      .maybeSingle();

    if (keyErr) return jsonResponse({ error: keyErr.message }, 500);
    if (!keyRow) return jsonResponse({ error: "Invalid license key" }, 403);
    if (keyRow.revoked_at) {
      return jsonResponse({ error: "License key has been revoked" }, 403);
    }
    if (keyRow.expires_at && new Date(keyRow.expires_at) < new Date()) {
      return jsonResponse({ error: "License key has expired" }, 403);
    }

    const { data: activations, error: actErr } = await supabase
      .from("receiptvault_license_activations")
      .select("*")
      .eq("license_key_id", keyRow.id);

    if (actErr) return jsonResponse({ error: actErr.message }, 500);

    const existing = activations?.find((a) => a.device_hash === deviceHash);
    const otherDevices = activations?.filter((a) => a.device_hash !== deviceHash) ?? [];

    if (!existing && otherDevices.length >= keyRow.max_devices) {
      await logEvent(supabase, {
        license_key_id: keyRow.id,
        event_type: "activate",
        device_hash: deviceHash,
        client_ip: ip,
        details: { rejected: true, reason: "max_devices" },
      });
      return jsonResponse(
        { error: "License key is already activated on another device" },
        409,
      );
    }

    const now = new Date().toISOString();
    let activationId: string;

    if (existing) {
      const { data: updated, error: updErr } = await supabase
        .from("receiptvault_license_activations")
        .update({
          last_seen_at: now,
          last_seen_ip: ip,
          app_version: appVersion,
          device_label: deviceLabel,
        })
        .eq("id", existing.id)
        .select("id")
        .single();
      if (updErr) return jsonResponse({ error: updErr.message }, 500);
      activationId = updated.id;
    } else {
      const { data: inserted, error: insErr } = await supabase
        .from("receiptvault_license_activations")
        .insert({
          license_key_id: keyRow.id,
          device_hash: deviceHash,
          device_label: deviceLabel,
          app_version: appVersion,
          activated_ip: ip,
          last_seen_ip: ip,
        })
        .select("id")
        .single();
      if (insErr) return jsonResponse({ error: insErr.message }, 500);
      activationId = inserted.id;
    }

    const offlineDays = 30;
    const tokenExpires = Math.floor(Date.now() / 1000) + offlineDays * 86400;
    const tierExpires = keyRow.tier === "lifetime"
      ? null
      : keyRow.expires_at
      ? Math.floor(new Date(keyRow.expires_at).getTime() / 1000)
      : tokenExpires;

    const token = await signLicenseJwt({
      sub: keyRow.id,
      act: activationId,
      dev: deviceHash,
      tier: keyRow.tier,
      exp: tokenExpires,
      tier_exp: tierExpires,
    });

    await logEvent(supabase, {
      license_key_id: keyRow.id,
      activation_id: activationId,
      event_type: existing ? "check" : "activate",
      device_hash: deviceHash,
      client_ip: ip,
      details: { tier: keyRow.tier },
    });

    return jsonResponse({
      ok: true,
      tier: keyRow.tier,
      license_key_id: keyRow.id,
      activation_id: activationId,
      token,
      token_expires_at: tokenExpires * 1000,
      offline_grace_days: offlineDays,
    });
  } catch (e) {
    return jsonResponse({ error: String(e) }, 500);
  }
});

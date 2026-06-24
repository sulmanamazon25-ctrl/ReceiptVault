import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import {
  adminClient,
  clientIp,
  corsHeaders,
  jsonResponse,
  logEvent,
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
    const licenseKeyId = body.license_key_id as string | undefined;
    const deviceHash = body.device_hash as string | undefined;

    if (!licenseKeyId || !deviceHash) {
      return jsonResponse({ error: "license_key_id and device_hash are required" }, 400);
    }

    const ip = clientIp(req);
    const supabase = adminClient();

    const { data: keyRow, error: keyErr } = await supabase
      .from("receiptvault_license_keys")
      .select("*")
      .eq("id", licenseKeyId)
      .maybeSingle();

    if (keyErr) return jsonResponse({ error: keyErr.message }, 500);
    if (!keyRow || keyRow.revoked_at) {
      return jsonResponse({ ok: false, valid: false, error: "License revoked or not found" }, 403);
    }
    if (keyRow.expires_at && new Date(keyRow.expires_at) < new Date()) {
      return jsonResponse({ ok: false, valid: false, error: "License expired" }, 403);
    }

    const { data: activation, error: actErr } = await supabase
      .from("receiptvault_license_activations")
      .select("*")
      .eq("license_key_id", licenseKeyId)
      .eq("device_hash", deviceHash)
      .maybeSingle();

    if (actErr) return jsonResponse({ error: actErr.message }, 500);
    if (!activation) {
      return jsonResponse({ ok: false, valid: false, error: "Device not bound to this license" }, 403);
    }

    const now = new Date().toISOString();
    await supabase
      .from("receiptvault_license_activations")
      .update({ last_seen_at: now, last_seen_ip: ip })
      .eq("id", activation.id);

    await logEvent(supabase, {
      license_key_id: licenseKeyId,
      activation_id: activation.id,
      event_type: "check",
      device_hash: deviceHash,
      client_ip: ip,
    });

    return jsonResponse({
      ok: true,
      valid: true,
      tier: keyRow.tier,
      last_validated_at: Date.now(),
    });
  } catch (e) {
    return jsonResponse({ error: String(e) }, 500);
  }
});

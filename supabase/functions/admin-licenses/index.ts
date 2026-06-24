import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import {
  adminClient,
  corsHeaders,
  jsonResponse,
  logEvent,
  sha256Hex,
} from "../_shared/license.ts";

function randomSegment(len: number): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.getRandomValues(new Uint8Array(len));
  return Array.from(bytes, (b) => chars[b % chars.length]).join("");
}

function formatKey(raw: string): string {
  return `RV-${raw.slice(0, 4)}-${raw.slice(4, 8)}-${raw.slice(8, 12)}`;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders() });
  }
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  const adminSecret = Deno.env.get("LICENSE_ADMIN_SECRET");
  const headerSecret = req.headers.get("x-admin-secret");
  if (!adminSecret || headerSecret !== adminSecret) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  try {
    const body = await req.json();
    const action = body.action as string;

    const supabase = adminClient();

    if (action === "create") {
      const tier = (body.tier as string) ?? "lifetime";
      const maxDevices = (body.max_devices as number) ?? 1;
      const notes = (body.notes as string) ?? null;
      const createdBy = (body.created_by as string) ?? "admin";
      const expiresAt = body.expires_at ?? null;

      const raw = randomSegment(12);
      const plaintext = formatKey(raw);
      const normalized = raw;
      const keyHash = await sha256Hex(normalized);
      const keyPrefix = plaintext.slice(0, 7);

      const { data: row, error } = await supabase
        .from("receiptvault_license_keys")
        .insert({
          key_hash: keyHash,
          key_prefix: keyPrefix,
          tier,
          max_devices: maxDevices,
          expires_at: expiresAt,
          created_by: createdBy,
          notes,
        })
        .select("id, key_prefix, tier, max_devices, created_at")
        .single();

      if (error) return jsonResponse({ error: error.message }, 500);

      await logEvent(supabase, {
        license_key_id: row.id,
        event_type: "create",
        details: { tier, max_devices: maxDevices },
      });

      return jsonResponse({
        ok: true,
        license_key: plaintext,
        key_id: row.id,
        key_prefix: row.key_prefix,
        tier: row.tier,
        max_devices: row.max_devices,
      });
    }

    if (action === "revoke") {
      const keyId = body.key_id as string;
      if (!keyId) return jsonResponse({ error: "key_id required" }, 400);
      const { error } = await supabase
        .from("receiptvault_license_keys")
        .update({ revoked_at: new Date().toISOString() })
        .eq("id", keyId);
      if (error) return jsonResponse({ error: error.message }, 500);
      await logEvent(supabase, {
        license_key_id: keyId,
        event_type: "revoke",
      });
      return jsonResponse({ ok: true });
    }

    if (action === "unbind-device") {
      const keyId = body.key_id as string;
      const deviceHash = body.device_hash as string;
      if (!keyId || !deviceHash) {
        return jsonResponse({ error: "key_id and device_hash required" }, 400);
      }
      const { error } = await supabase
        .from("receiptvault_license_activations")
        .delete()
        .eq("license_key_id", keyId)
        .eq("device_hash", deviceHash);
      if (error) return jsonResponse({ error: error.message }, 500);
      await logEvent(supabase, {
        license_key_id: keyId,
        event_type: "unbind",
        device_hash: deviceHash,
      });
      return jsonResponse({ ok: true });
    }

    if (action === "list") {
      const { data: keys, error } = await supabase
        .from("receiptvault_license_keys")
        .select("id, key_prefix, tier, max_devices, expires_at, revoked_at, created_at, notes")
        .order("created_at", { ascending: false })
        .limit(100);
      if (error) return jsonResponse({ error: error.message }, 500);

      const { data: activations } = await supabase
        .from("receiptvault_license_activations")
        .select("license_key_id, device_hash, device_label, activated_at, last_seen_at, activated_ip");

      return jsonResponse({ ok: true, keys, activations: activations ?? [] });
    }

    return jsonResponse({ error: "Unknown action" }, 400);
  } catch (e) {
    return jsonResponse({ error: String(e) }, 500);
  }
});

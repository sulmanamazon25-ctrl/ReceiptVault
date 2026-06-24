import { createClient } from "jsr:@supabase/supabase-js@2";

export function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers":
      "authorization, x-client-info, apikey, content-type, x-admin-secret",
  };
}

export function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders(), "Content-Type": "application/json" },
  });
}

export function adminClient() {
  const url = Deno.env.get("SUPABASE_URL")!;
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  return createClient(url, key, { auth: { persistSession: false } });
}

export async function sha256Hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input);
  const hash = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(hash))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export function normalizeLicenseKey(key: string): string {
  return key.trim().toUpperCase().replace(/[^A-Z0-9]/g, "").replace(/^RV/, "");
}

export function clientIp(req: Request): string | null {
  return (
    req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ??
    req.headers.get("x-real-ip")
  );
}

const JWT_SECRET = () =>
  Deno.env.get("LICENSE_JWT_SECRET") ??
  Deno.env.get("SUPABASE_JWT_SECRET") ??
  "receiptvault-dev-secret-change-me";

function base64UrlEncode(data: Uint8Array): string {
  return btoa(String.fromCharCode(...data))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

export async function signLicenseJwt(payload: Record<string, unknown>): Promise<string> {
  const header = { alg: "HS256", typ: "JWT" };
  const enc = (obj: unknown) =>
    base64UrlEncode(new TextEncoder().encode(JSON.stringify(obj)));
  const headerPart = enc(header);
  const payloadPart = enc(payload);
  const signingInput = `${headerPart}.${payloadPart}`;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(JWT_SECRET()),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(signingInput),
  );
  return `${signingInput}.${base64UrlEncode(new Uint8Array(sig))}`;
}

export async function logEvent(
  supabase: ReturnType<typeof adminClient>,
  event: {
    license_key_id?: string;
    activation_id?: string;
    event_type: string;
    device_hash?: string;
    client_ip?: string | null;
    details?: Record<string, unknown>;
  },
) {
  await supabase.from("receiptvault_license_events").insert({
    license_key_id: event.license_key_id ?? null,
    activation_id: event.activation_id ?? null,
    event_type: event.event_type,
    device_hash: event.device_hash ?? null,
    client_ip: event.client_ip ?? null,
    details: event.details ?? {},
  });
}

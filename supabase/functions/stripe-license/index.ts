import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, stripe-signature",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const stripeSecret = Deno.env.get("STRIPE_WEBHOOK_SECRET");
  const adminSecret = Deno.env.get("LICENSE_ADMIN_SECRET");
  if (!stripeSecret || !adminSecret) {
    return new Response(JSON.stringify({ error: "Not configured" }), { status: 500, headers: corsHeaders });
  }

  try {
    const body = await req.text();
    const event = JSON.parse(body);

    if (event.type !== "checkout.session.completed") {
      return new Response(JSON.stringify({ received: true }), { headers: corsHeaders });
    }

    const session = event.data.object;
    const tier = session.metadata?.tier ?? "lifetime";
    const email = session.customer_details?.email ?? session.customer_email ?? "stripe";

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const adminUrl = `${Deno.env.get("SUPABASE_URL")}/functions/v1/admin-licenses`;
    const createRes = await fetch(adminUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-admin-secret": adminSecret,
        Authorization: `Bearer ${Deno.env.get("SUPABASE_ANON_KEY")}`,
      },
      body: JSON.stringify({
        action: "create",
        tier,
        max_devices: 1,
        notes: `Stripe ${session.id} ${email}`,
        created_by: "stripe",
      }),
    });

    const created = await createRes.json();
    if (!createRes.ok) {
      return new Response(JSON.stringify(created), { status: 500, headers: corsHeaders });
    }

  // TODO: send email with created.license_key via Resend/SendGrid when configured
    console.log("License created for", email, "key prefix", created.key_id);

    return new Response(JSON.stringify({ ok: true, key_id: created.key_id }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 400, headers: corsHeaders });
  }
});

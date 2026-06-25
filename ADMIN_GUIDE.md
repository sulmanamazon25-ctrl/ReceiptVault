# ReceiptVault Admin Guide (English)

## Overview

ReceiptVault v2 supports two ways to unlock Pro:

1. **Google Play Billing** — public users subscribe in-app (Play Store install required).
2. **Admin license keys** — you generate keys; **1 key = 1 device** (device fingerprint binding). Client IP is logged on activate/check for audit.

Receipt data stays **on the device**. Only license activation sends a device hash and IP to Supabase.

---

## Dashboards

| Service | URL |
|---------|-----|
| GitHub repo | https://github.com/sulmanamazon25-ctrl/ReceiptVault |
| GitHub Actions (CI builds) | https://github.com/sulmanamazon25-ctrl/ReceiptVault/actions |
| Supabase project | https://supabase.com/dashboard/project/bkfybqwtbaecqfnzcqva |
| Legal pages (Privacy/Terms) | Hosted on Supabase Storage |

---

## Generate a license key (1 device)

### One-time setup

1. Set Supabase Edge Function secret:
   ```powershell
   supabase secrets set LICENSE_ADMIN_SECRET=your-strong-secret-here --project-ref bkfybqwtbaecqfnzcqva
   supabase secrets set LICENSE_JWT_SECRET=your-jwt-secret-here --project-ref bkfybqwtbaecqfnzcqva
   ```
2. Deploy Edge Functions:
   ```powershell
   cd E:\scanoff
   supabase functions deploy activate-license --project-ref bkfybqwtbaecqfnzcqva
   supabase functions deploy check-license --project-ref bkfybqwtbaecqfnzcqva
   supabase functions deploy admin-licenses --project-ref bkfybqwtbaecqfnzcqva
   ```
3. Apply DB migration:
   ```powershell
   supabase db query --linked -f supabase/migrations/20260625120000_license_keys.sql
   ```
4. Set local secret (gitignored) in `supabase/.project-secrets.local`:
   ```
   LICENSE_ADMIN_SECRET=your-strong-secret-here
   ```

### Create a key

```powershell
cd E:\scanoff
.\scripts\generate-license.ps1 -Tier lifetime -MaxDevices 1 -Note "Admin phone"
```

**Save the printed key immediately** — it is shown once. Format: `RV-XXXX-XXXX-XXXX`.

### Give key to user

User opens app → **Settings** → **ReceiptVault Pro** → enters key → **Activate**.

---

## Revoke or unbind a device

### Via Supabase SQL Editor

List keys and activations:
```sql
select k.id, k.key_prefix, k.tier, k.max_devices, k.revoked_at,
       a.device_hash, a.device_label, a.activated_ip, a.last_seen_at
from receiptvault_license_keys k
left join receiptvault_license_activations a on a.license_key_id = k.id
order by k.created_at desc;
```

Revoke a key:
```sql
update receiptvault_license_keys
set revoked_at = now()
where id = 'KEY-UUID-HERE';
```

Unbind one device (allows key on a new phone):
```sql
delete from receiptvault_license_activations
where license_key_id = 'KEY-UUID-HERE'
  and device_hash = 'DEVICE-HASH-HERE';
```

### Via admin API

```powershell
$headers = @{
  "Content-Type" = "application/json"
  "x-admin-secret" = $env:LICENSE_ADMIN_SECRET
}
Invoke-RestMethod -Uri "https://bkfybqwtbaecqfnzcqva.supabase.co/functions/v1/admin-licenses" `
  -Method Post -Headers $headers `
  -Body '{"action":"revoke","key_id":"KEY-UUID"}'
```

---

## Google Play (public channel)

1. Play Developer account ($25 one-time).
2. Create in-app products matching the app:
   - `pro_monthly` (subscription)
   - `pro_yearly` (subscription)
   - `pro_lifetime` (one-time)
3. Upload **AAB** from GitHub Actions artifact `receiptvault-release-aab`.
4. Internal testing → closed → production.

Sideloaded APK users cannot use Play Billing; use license keys instead.

---

## Release a new app version

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Commit and push to `master`.
3. GitHub Actions builds signed APK + AAB.
4. Download artifacts or upload AAB to Play Console.

**Keystore:** back up `release.keystore` and passwords offline. CI secrets are already on GitHub.

---

## Security notes

- **1 key = 1 device:** enforced by unique `(license_key_id, device_hash)` in the database.
- **IP logging:** `activated_ip` and `last_seen_ip` stored for fraud audit; binding uses device hash, not IP (mobile IPs change).
- **Offline grace:** app keeps Pro for 30 days after last successful online check.
- **License does not transfer** with encrypted backup to a new phone — issue a new key or unbind the old device.

---

## Launch pricing (v3.1 — Jun–Sep 2026)

| Tier | Play launch (mo 1–3) | Play standard (mo 4+) | License key (20% off standard) |
|------|----------------------|------------------------|--------------------------------|
| Monthly | $1.99 | $2.99 | $2.39 |
| Yearly | $11.99 | $19.99 | $15.99 |
| Lifetime | $24.99 | $39.99 | **$31.99** |

- **License rule:** sideload/APK keys are always **0.8 × Play standard** price.
- **Launch promo ends:** 2026-09-24 (`BuildConfig.LAUNCH_PROMO_END`).
- **Public pricing page:** [docs/pricing.html](docs/pricing.html) — host on Supabase Storage `legal/pricing.html`.
- **Stripe (phase 2):** `supabase/functions/stripe-license` webhook → `admin-licenses` `create` action.

For APK launch before Stripe: sell lifetime keys manually at **$31.99** via `scripts/generate-license.ps1`.

---

## Support

- Email: support@receiptvault.app
- User guide: [USER_GUIDE.md](USER_GUIDE.md)

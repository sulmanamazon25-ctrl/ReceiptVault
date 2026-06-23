# ReceiptVault Supabase (separate project)

ReceiptVault is **offline-first**. Supabase is optional infrastructure for:

- Remote app config (min version, maintenance mode)
- Play purchase audit log (future webhook)
- Support messages from a landing page

**Do not** apply these migrations to your existing Supabase project (`rxldsntexstyyurnhwht` / Pinquill/ViralEdge). Use a **new** project.

## Your ReceiptVault project (live)

| Item | Value |
|------|-------|
| Name | `receiptvault` |
| Ref | `bkfybqwtbaecqfnzcqva` |
| URL | https://bkfybqwtbaecqfnzcqva.supabase.co |
| Region | us-east-1 |
| Dashboard | https://supabase.com/dashboard/project/bkfybqwtbaecqfnzcqva |

Schema is applied (`receiptvault_app_config`, `receiptvault_play_entitlements`, `receiptvault_support_messages`). Your other Supabase projects were **not** modified.

MCP entry `supabase-receiptvault` was added alongside your existing `supabase` MCP (Pinquill/Voxi).

## Create the new project

1. Log in once:

```powershell
supabase login
```

2. Run the creation script (creates `receiptvault` in `us-east-1`, links repo, pushes migrations):

```powershell
cd E:\scanoff
.\scripts\create-supabase-project.ps1
```

3. Credentials are saved to `supabase/.project-secrets.local` (gitignored).

## MCP (optional)

Keep your existing MCP server unchanged. Add a **second** entry in `~/.cursor/mcp.json`:

```json
"supabase-receiptvault": {
  "type": "http",
  "url": "https://mcp.supabase.com/mcp?project_ref=YOUR_NEW_PROJECT_REF"
}
```

Replace `YOUR_NEW_PROJECT_REF` with the ref printed by the script.

## Manual alternative (Dashboard)

1. [supabase.com/dashboard](https://supabase.com/dashboard) → **New project**
2. Name: `receiptvault`, region: US East, strong DB password
3. Link and push:

```powershell
supabase link --project-ref YOUR_NEW_REF
supabase db push
```

## Schema

| Table | Purpose |
|-------|---------|
| `receiptvault_app_config` | Min version, maintenance, billing product IDs |
| `receiptvault_play_entitlements` | Play purchase audit (service role only) |
| `receiptvault_support_messages` | Future support form |

RLS blocks public writes; anon may read `release` config only (for future update checks).

## Android app

The MVP APK does **not** call Supabase yet (no `INTERNET` permission). Backend is ready for post-MVP features.

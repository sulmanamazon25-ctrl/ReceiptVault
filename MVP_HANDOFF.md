# ReceiptVault MVP — Final handoff (2026-06-24)

## Install the app now

Copy to your Android phone and install (enable **Install unknown apps**):

```
E:\scanoff\app\build\outputs\apk\release\app-release.apk
```

## GitHub (one-time — requires your login)

```powershell
cd E:\scanoff
gh auth login
.\scripts\publish-github.ps1
```

This creates **https://github.com/sulmanamazon25-ctrl/ReceiptVault**, pushes code, and enables Pages at:

**https://sulmanamazon25-ctrl.github.io/ReceiptVault/**

Optional CI signing secrets: see `DEPLOYMENT.md` (`scripts\encode-keystore.ps1` for base64).

## Supabase (done)

| | |
|---|---|
| Project | receiptvault |
| Ref | `bkfybqwtbaecqfnzcqva` |
| URL | https://bkfybqwtbaecqfnzcqva.supabase.co |
| Dashboard | https://supabase.com/dashboard/project/bkfybqwtbaecqfnzcqva |

Other Supabase projects were not modified.

## Local git

- Commit: `119fc9b` on branch `master`
- Keystore (backup offline): `E:\scanoff\release.keystore` (gitignored)

## Test on phone

Use checklist in `TEST_REPORT.md`.

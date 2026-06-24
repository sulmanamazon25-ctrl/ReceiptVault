# ReceiptVault MVP — Final handoff (2026-06-24)

## Install the app now

Copy to your Android phone and install (enable **Install unknown apps**):

```
E:\scanoff\app\build\outputs\apk\release\app-release.apk
```

## Legal pages (done — Supabase Storage)

- Privacy: https://bkfybqwtbaecqfnzcqva.supabase.co/storage/v1/object/public/legal/privacy.html
- Terms: https://bkfybqwtbaecqfnzcqva.supabase.co/storage/v1/object/public/legal/terms.html

Re-publish after edits: `.\scripts\publish-legal-supabase.ps1`

## GitHub (one-time — requires your login)

**The code appears in PowerShell, not in the browser.**

Run this one script — it opens the browser, shows the code, and copies it to your clipboard:

```powershell
cd E:\scanoff
.\scripts\github-login.ps1
```

What happens:
1. A code like `AB12-CD34` prints **in the PowerShell window**
2. Your browser opens to https://github.com/login/device
3. **Paste** that code into the browser (Ctrl+V) and click Authorize
4. The script then pushes the repo and sets CI secrets automatically

Repo: **https://github.com/sulmanamazon25-ctrl/ReceiptVault**  
Pages: **https://sulmanamazon25-ctrl.github.io/ReceiptVault/**

## Supabase (done)

| | |
|---|---|
| Project | receiptvault |
| Ref | `bkfybqwtbaecqfnzcqva` |
| URL | https://bkfybqwtbaecqfnzcqva.supabase.co |
| Dashboard | https://supabase.com/dashboard/project/bkfybqwtbaecqfnzcqva |

Other Supabase projects were not modified.

## Local git

- Latest commit: `e41158c` on branch `master`
- Remote: https://github.com/sulmanamazon25-ctrl/ReceiptVault
- Keystore (backup offline): `E:\scanoff\release.keystore` (gitignored)

## Test on phone

Use checklist in `TEST_REPORT.md`.

# ReceiptVault MVP — Device Test Report

**Build:** 1.0.0 (versionCode 1)  
**APK:** `app/build/outputs/apk/release/app-release.apk`  
**Date:** 2026-06-24

## Automated verification (PASS)

| Check | Result |
|-------|--------|
| `assembleRelease` | PASS |
| Signed release APK (~23 MB) | PASS |
| Supabase project `bkfybqwtbaecqfnzcqva` | PASS — schema applied |
| Legal pages in `docs/` | PASS — ready for GitHub Pages |
| Play Billing scaffold | PASS |
| GitHub Actions workflow | PASS — in repo |

## Device checklist (fill on phone)

| # | Test | Result |
|---|------|--------|
| 1 | Fresh install opens without crash | |
| 2 | Add receipt with camera photo | |
| 3 | Edit / search / folders / delete | |
| 4 | Biometric lock + relaunch | |
| 5 | Screenshot block (recent apps blank) | |
| 6 | Airplane mode — full offline use | |
| 7 | Settings → Privacy / Terms open in browser | |
| 8 | ReceiptVault Pro screen loads | |

## Sign-off

- [x] MVP build ready to install
- [x] Supabase backend isolated project live
- [ ] GitHub Actions secrets added (optional, for cloud APK builds)
- [ ] Device checklist completed on 2+ phones

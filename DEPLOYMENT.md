# Deployment Guide

## 1. Push to GitHub

```powershell
cd E:\scanoff
git init
git add .
git commit -m "ReceiptVault MVP: CI build, signing, legal pages, billing scaffold"
git branch -M main
git remote add origin https://github.com/sulmanamazon25-ctrl/ReceiptVault.git
git push -u origin main
```

## 2. GitHub Actions secrets (signed release APK)

Generate a release keystore (once, back up securely):

```powershell
keytool -genkey -v -keystore release.keystore -alias receiptvault `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass YOUR_STORE_PASSWORD -keypass YOUR_KEY_PASSWORD `
  -dname "CN=ReceiptVault, OU=Mobile, O=ReceiptVault, L=City, ST=State, C=US"
```

Add these **Repository secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `ANDROID_KEYSTORE_BASE64` | `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))` |
| `ANDROID_KEYSTORE_PASSWORD` | Your store password |
| `ANDROID_KEY_ALIAS` | `receiptvault` |
| `ANDROID_KEY_PASSWORD` | Your key password |

Trigger **Actions → Android Build → Run workflow**. Download `receiptvault-release-apk` from the run artifacts.

## 3. GitHub Pages (legal URLs)

1. Repo **Settings → Pages → Build and deployment → Source: Deploy from branch**
2. Branch: `main`, folder: `/docs`
3. Legal URLs are set in `app/build.gradle.kts` for GitHub Pages at `sulmanamazon25-ctrl.github.io/ReceiptVault`

## 4. Install APK on Android

1. Transfer `app-release.apk` to the phone
2. Enable **Install unknown apps** for your file manager or browser
3. Open the APK and install (Android 8.0 / API 26+)

## 5. Google Play (post-MVP)

1. Create Play Developer account ($25)
2. Create products: `pro_monthly`, `pro_yearly`, `pro_lifetime` (match `BillingProducts.kt`)
3. Upload AAB from CI artifact `receiptvault-release-aab`
4. Complete Data safety form, content rating, store listing
5. Internal testing → closed → production

## Local release build

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_KEYSTORE_PASSWORD = "your_password"
$env:ANDROID_KEY_ALIAS = "receiptvault"
$env:ANDROID_KEY_PASSWORD = "your_password"
.\gradlew.bat assembleRelease
# Output: app\build\outputs\apk\release\app-release.apk
```

Without signing env vars, release APK is signed with the debug keystore (fine for personal testing only).

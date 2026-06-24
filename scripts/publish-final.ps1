# Publish only — run after `gh auth status` succeeds in THIS terminal.
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

gh auth status | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "GitHub CLI is not logged in on this machine." -ForegroundColor Red
    Write-Host "Run: .\scripts\github-login.ps1"
    exit 1
}

Write-Host "Publishing repo + Pages + CI secrets..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "publish-github.ps1")

$ks = Join-Path $PSScriptRoot "..\release.keystore"
if (Test-Path $ks) {
    Write-Host "Setting GitHub Actions signing secrets..."
    $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($ks))
    $b64 | gh secret set ANDROID_KEYSTORE_BASE64
    "ReceiptVault2026!" | gh secret set ANDROID_KEYSTORE_PASSWORD
    "receiptvault" | gh secret set ANDROID_KEY_ALIAS
    "ReceiptVault2026!" | gh secret set ANDROID_KEY_PASSWORD
    Write-Host "Secrets configured." -ForegroundColor Green
}

Write-Host ""
Write-Host "=== DONE ===" -ForegroundColor Green
Write-Host "Repo:  https://github.com/sulmanamazon25-ctrl/ReceiptVault"
Write-Host "Pages: https://sulmanamazon25-ctrl.github.io/ReceiptVault/"
Write-Host "APK:   app\build\outputs\apk\release\app-release.apk"

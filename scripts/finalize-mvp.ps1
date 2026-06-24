# Full finalize: Supabase legal pages + GitHub repo (if authenticated) + CI secrets.
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..")

& (Join-Path $PSScriptRoot "publish-legal-supabase.ps1")

$null = gh auth status 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "GitHub auth found — publishing repo..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "publish-github.ps1")
    $ks = Join-Path $PSScriptRoot "..\release.keystore"
    if (Test-Path $ks) {
        Write-Host "Setting GitHub Actions secrets..."
        $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($ks))
        $b64 | gh secret set ANDROID_KEYSTORE_BASE64 2>&1 | Out-Host
        "ReceiptVault2026!" | gh secret set ANDROID_KEYSTORE_PASSWORD 2>&1 | Out-Host
        "receiptvault" | gh secret set ANDROID_KEY_ALIAS 2>&1 | Out-Host
        "ReceiptVault2026!" | gh secret set ANDROID_KEY_PASSWORD 2>&1 | Out-Host
    }
} else {
    Write-Host ""
    Write-Host "GitHub not authenticated — skipping repo push." -ForegroundColor Yellow
    Write-Host "Run: gh auth login  then  .\scripts\publish-github.ps1"
}

Write-Host ""
Write-Host "=== Finalize complete ===" -ForegroundColor Green
Write-Host "APK: app\build\outputs\apk\release\app-release.apk"

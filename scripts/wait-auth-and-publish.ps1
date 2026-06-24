# Waits for `gh auth login` to complete, then publishes repo + Pages + CI secrets.
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..")

Write-Host "Waiting for GitHub CLI auth (complete login at https://github.com/login/device)..." -ForegroundColor Cyan
for ($i = 0; $i -lt 120; $i++) {
    $null = gh auth status 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "GitHub auth detected." -ForegroundColor Green
        & (Join-Path $PSScriptRoot "publish-github.ps1")
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
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
        exit 0
    }
    if ($i % 6 -eq 0) { Write-Host "  still waiting... ($($i * 5)s)" }
    Start-Sleep -Seconds 5
}
Write-Host "Timed out waiting for gh auth. Run: gh auth login" -ForegroundColor Red
exit 1

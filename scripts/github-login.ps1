# GitHub login helper - shows the code clearly, copies it, opens the browser.
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..")

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  GITHUB LOGIN - WHERE IS THE CODE?" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  The code does NOT appear in the browser." -ForegroundColor Yellow
Write-Host "  It appears HERE in this PowerShell window in a few seconds."
Write-Host ""
Write-Host "  STEPS:"
Write-Host "    1. Wait for the code below (format: XXXX-XXXX)"
Write-Host "    2. Browser opens to github.com/login/device"
Write-Host "    3. PASTE the code from this window into the browser"
Write-Host "    4. Click Authorize GitHub CLI"
Write-Host "    5. Come back here - login finishes automatically"
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

Start-Process "https://github.com/login/device"

gh auth login --web --git-protocol https 2>&1 | ForEach-Object {
    $line = $_.ToString()
    Write-Host $line

    if ($line -match 'one-time code:\s*(\S+)') {
        $code = $Matches[1]
        Set-Clipboard -Value $code
        Write-Host ""
        Write-Host "  *** YOUR CODE:  $code  ***" -ForegroundColor Green
        Write-Host "  *** Copied to clipboard. Paste on the GitHub page. ***" -ForegroundColor Green
        Write-Host ""
    }
}

$null = gh auth status 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "GitHub login successful!" -ForegroundColor Green
    Write-Host "Running finalize (push repo + CI secrets)..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "finalize-mvp.ps1")
} else {
    Write-Host ""
    Write-Host "Login did not complete. Run this script again:" -ForegroundColor Red
    Write-Host "  .\scripts\github-login.ps1"
    exit 1
}

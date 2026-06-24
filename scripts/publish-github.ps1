# One-shot publish: repo + push + GitHub Pages (run after github-login.ps1)
$ErrorActionPreference = "Stop"
$Owner = "sulmanamazon25-ctrl"
$Repo = "ReceiptVault"

Set-Location (Join-Path $PSScriptRoot "..")

gh auth status | Out-Null

$Branch = git branch --show-current

$hasOrigin = [bool](git remote 2>$null | Where-Object { $_ -eq "origin" })

if (-not $hasOrigin) {
    gh repo create $Repo --public --source=. --remote=origin --description "ReceiptVault offline encrypted receipt scanner for Android"
    git push -u origin $Branch
} else {
    git push -u origin $Branch
}

gh api --method PUT "/repos/$Owner/$Repo/pages" `
    -f build_type=legacy `
    -f "source[branch]=$Branch" `
    -f "source[path]=/docs"

Write-Host ""
Write-Host "=== Published ===" -ForegroundColor Green
Write-Host "Repo: https://github.com/$Owner/$Repo"
Write-Host "Pages: https://$Owner.github.io/$Repo/"
Write-Host "Actions: https://github.com/$Owner/$Repo/actions"

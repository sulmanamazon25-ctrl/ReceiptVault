# Publishes legal docs to Supabase Storage (no GitHub required).
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..")

Write-Host "Applying legal storage migration (skip if already applied)..."
$migration = Join-Path $PSScriptRoot "..\supabase\migrations\20260624130000_legal_storage_bucket.sql"
& supabase db query --linked -f $migration 2>&1 | Out-Host

Write-Host "Uploading legal pages (--experimental required for storage cp)..."
$docs = (Resolve-Path (Join-Path $PSScriptRoot "..\docs")).Path
foreach ($name in @("privacy.html", "terms.html", "index.html")) {
    $file = Join-Path $docs $name
    & supabase storage cp --experimental $file "ss:///legal/$name" --linked --content-type "text/html" 2>&1 | Out-Host
}

Write-Host ""
Write-Host "=== Legal pages live ===" -ForegroundColor Green
Write-Host "https://bkfybqwtbaecqfnzcqva.supabase.co/storage/v1/object/public/legal/privacy.html"
Write-Host "https://bkfybqwtbaecqfnzcqva.supabase.co/storage/v1/object/public/legal/terms.html"

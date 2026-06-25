# Generates a ReceiptVault license key via the admin-licenses Edge Function.
param(
    [ValidateSet("monthly", "yearly", "lifetime")]
    [string]$Tier = "lifetime",
    [int]$MaxDevices = 1,
    [string]$Note = "",
    [string]$CreatedBy = "admin"
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

$secretsFile = Join-Path $PSScriptRoot "..\supabase\.project-secrets.local"
$projectRef = "bkfybqwtbaecqfnzcqva"
$adminSecret = $env:LICENSE_ADMIN_SECRET

if (-not $adminSecret -and (Test-Path $secretsFile)) {
    $content = Get-Content $secretsFile -Raw
    if ($content -match 'LICENSE_ADMIN_SECRET=(.+)') {
        $adminSecret = $Matches[1].Trim()
    }
}

if (-not $adminSecret) {
    Write-Host "Set LICENSE_ADMIN_SECRET env var or add to supabase/.project-secrets.local" -ForegroundColor Red
    Write-Host "  LICENSE_ADMIN_SECRET=your-secret-here"
    Write-Host ""
    Write-Host "Also set the same secret in Supabase:"
    Write-Host "  supabase secrets set LICENSE_ADMIN_SECRET=your-secret-here --project-ref $projectRef"
    exit 1
}

$body = @{
    action     = "create"
    tier       = $Tier
    max_devices = $MaxDevices
    notes      = $Note
    created_by = $CreatedBy
} | ConvertTo-Json

$url = "https://$projectRef.supabase.co/functions/v1/admin-licenses"
$headers = @{
    "Content-Type"   = "application/json"
    "x-admin-secret" = $adminSecret
    "Authorization"  = "Bearer $($env:SUPABASE_ANON_KEY)"
}

# Try to get anon key from supabase CLI if not set
if (-not $headers.Authorization -or $headers.Authorization -eq "Bearer ") {
    $keys = supabase projects api-keys --project-ref $projectRef -o json 2>$null | ConvertFrom-Json
    $anon = ($keys | Where-Object { $_.id -eq "anon" }).api_key
    if ($anon) { $headers.Authorization = "Bearer $anon" }
}

$response = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body

Write-Host ""
Write-Host "=== License key created ===" -ForegroundColor Green
Write-Host "Key (save now - shown once): $($response.license_key)" -ForegroundColor Yellow
Write-Host "Key ID: $($response.key_id)"
Write-Host "Tier: $($response.tier)"
Write-Host "Max devices: $($response.max_devices)"
Write-Host ""
Write-Host "User activates in app: Settings -> ReceiptVault Pro -> Enter license key"

# Finishes ReceiptVault Supabase setup after project creation (handles PowerShell stderr quirks).
param(
    [string]$ProjectRef = "bkfybqwtbaecqfnzcqva"
)

$ErrorActionPreference = "Continue"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

function Invoke-Supabase {
    param([string[]]$Args)
    $output = & supabase @Args 2>&1
    return @{ Output = ($output | Out-String).Trim(); Code = $LASTEXITCODE }
}

Write-Host "Linking project $ProjectRef..."
Set-Location $RepoRoot
$link = Invoke-Supabase @("link", "--project-ref", $ProjectRef)
Write-Host $link.Output
if ($link.Code -ne 0) { exit $link.Code }

Write-Host "Applying migrations via Management API..."
$migrationFile = Join-Path $RepoRoot "supabase/migrations/20260624120000_receiptvault_initial.sql"
$push = Invoke-Supabase @("db", "query", "--linked", "-f", $migrationFile)
Write-Host $push.Output
if ($push.Code -ne 0) { exit $push.Code }

Write-Host "Verifying schema..."
$verify = Invoke-Supabase @("db", "query", "--linked", "select key from public.receiptvault_app_config order by key;")
Write-Host $verify.Output

Write-Host ""
Write-Host "=== ReceiptVault Supabase ready ===" -ForegroundColor Green
Write-Host "Project ref: $ProjectRef"
Write-Host "URL: https://${ProjectRef}.supabase.co"
Write-Host "Dashboard: https://supabase.com/dashboard/project/$ProjectRef"
Write-Host "MCP: https://mcp.supabase.com/mcp?project_ref=$ProjectRef"

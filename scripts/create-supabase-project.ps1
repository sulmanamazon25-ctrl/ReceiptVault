# Creates a NEW Supabase project named "receiptvault" without touching existing projects.
# Prerequisites: supabase login (once)
#
# Usage:
#   supabase login
#   .\scripts\create-supabase-project.ps1

$ErrorActionPreference = "Continue"
$ProjectName = "receiptvault"
$Region = "us-east-1"
$DbPassword = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })

Write-Host "Checking Supabase CLI auth..."
$orgsJson = supabase orgs list -o json 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Not logged in. Run: supabase login" -ForegroundColor Yellow
    exit 1
}

$orgs = $orgsJson | ConvertFrom-Json
if (-not $orgs -or $orgs.Count -eq 0) {
    Write-Host "No organizations found on your Supabase account." -ForegroundColor Red
    exit 1
}

$orgId = $orgs[0].id
$orgName = $orgs[0].name
Write-Host "Using organization: $orgName ($orgId)"

Write-Host "Creating project '$ProjectName' in $Region..."
$createOut = & supabase projects create $ProjectName `
    --org-id $orgId `
    --db-password $DbPassword `
    --region $Region `
    -o json 2>&1 | Out-String

if ($LASTEXITCODE -ne 0 -and $createOut -notmatch '"id"\s*:') {
    Write-Host $createOut
    Write-Host "Project creation failed. If name is taken, edit ProjectName in this script." -ForegroundColor Red
    exit 1
}

# PowerShell may treat stderr as error even when create succeeds — parse JSON from output.
$jsonLine = ($createOut -split "`n" | Where-Object { $_ -match '^\s*\{' }) -join "`n"
if (-not $jsonLine) { $jsonLine = $createOut }
$project = $jsonLine | ConvertFrom-Json
$ref = $project.id
if (-not $ref) {
    # Project may already exist from a prior partial run — look it up by name.
    $list = supabase projects list -o json 2>$null | ConvertFrom-Json
    $existing = $list | Where-Object { $_.name -eq $ProjectName }
    if ($existing) {
        $ref = $existing.ref
        Write-Host "Project '$ProjectName' already exists: $ref" -ForegroundColor Yellow
    } else {
        Write-Host $createOut
        exit 1
    }
}
Write-Host "Created project ref: $ref" -ForegroundColor Green
Write-Host "Project URL: https://${ref}.supabase.co"

$secretsPath = Join-Path $PSScriptRoot ".." "supabase" ".project-secrets.local"
$secretsContent = @(
    "# Generated $(Get-Date -Format o) - DO NOT COMMIT"
    "SUPABASE_PROJECT_REF=$ref"
    "SUPABASE_DB_PASSWORD=$DbPassword"
    "SUPABASE_URL=https://${ref}.supabase.co"
)
$secretsContent | Set-Content -Path $secretsPath -Encoding UTF8

Write-Host "Saved credentials to supabase/.project-secrets.local (gitignored)" -ForegroundColor Cyan

Set-Location (Join-Path $PSScriptRoot "..")
Write-Host "Linking local repo to new project..."
& supabase link --project-ref $ref 2>&1 | Out-Host

Write-Host "Applying migrations via Management API..."
$migrationFile = Join-Path (Get-Location) "supabase/migrations/20260624120000_receiptvault_initial.sql"
& supabase db query --linked -f $migrationFile 2>&1 | Out-Host

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green
Write-Host "Project ref: $ref"
Write-Host "MCP URL: https://mcp.supabase.com/mcp?project_ref=$ref"
Write-Host "Dashboard: https://supabase.com/dashboard/project/$ref"
Write-Host "API keys: https://supabase.com/dashboard/project/$ref/settings/api"
Write-Host "DB password saved in supabase/.project-secrets.local"

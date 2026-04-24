param(
    [string]$Workspace = (Get-Location).Path
)

$source = Join-Path $Workspace "codex-setup\skills\self-improve"
$target = Join-Path $env:USERPROFILE ".codex\skills\self-improve"

if (-not (Test-Path $source)) {
    throw "Missing repo source: $source"
}

New-Item -ItemType Directory -Force -Path $target | Out-Null
Copy-Item -Recurse -Force (Join-Path $source "*") $target
Write-Host "Deployed self-improve to $target"

param(
    [string]$Workspace = (Get-Location).Path
)

$source = Join-Path $Workspace "codex-setup\skills\self-improve"
$target = Join-Path $env:USERPROFILE ".codex\skills\self-improve"
$rulesSource = Join-Path $Workspace "codex-setup\rules"
$rulesTarget = Join-Path $env:USERPROFILE ".codex\rules"

if (-not (Test-Path $source)) {
    throw "Missing repo source: $source"
}

New-Item -ItemType Directory -Force -Path $target | Out-Null
Copy-Item -Recurse -Force (Join-Path $source "*") $target
Write-Host "Deployed self-improve to $target"

if (Test-Path $rulesSource) {
    New-Item -ItemType Directory -Force -Path $rulesTarget | Out-Null
    foreach ($ruleSource in Get-ChildItem -Path $rulesSource -Filter "*.md" -File) {
        Copy-Item -Force $ruleSource.FullName (Join-Path $rulesTarget $ruleSource.Name)
        Write-Host "Deployed Codex rule $($ruleSource.Name) to $rulesTarget"
    }
}

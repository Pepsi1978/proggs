param(
    [string]$Workspace = (Get-Location).Path
)

$ErrorActionPreference = "Stop"

function Copy-DirectoryChildren {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Target
    )

    if (-not (Test-Path $Source)) {
        return 0
    }

    New-Item -ItemType Directory -Force -Path $Target | Out-Null
    $count = 0
    Get-ChildItem -Path $Source -Force | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $Target $_.Name) -Recurse -Force
        $count++
    }
    return $count
}

$setupRoot = Join-Path $Workspace "codex-setup"
if (-not (Test-Path $setupRoot)) {
    throw "Missing codex-setup directory: $setupRoot"
}

$codexHome = Join-Path $env:USERPROFILE ".codex"
New-Item -ItemType Directory -Force -Path $codexHome | Out-Null

git config --global pull.rebase true
git config --global rebase.autoStash true
git config --global rerere.enabled true

$rulesCopied = Copy-DirectoryChildren `
    -Source (Join-Path $setupRoot "rules") `
    -Target (Join-Path $codexHome "rules")
$skillsCopied = Copy-DirectoryChildren `
    -Source (Join-Path $setupRoot "skills") `
    -Target (Join-Path $codexHome "skills")
$agentsCopied = Copy-DirectoryChildren `
    -Source (Join-Path $setupRoot "agents") `
    -Target (Join-Path $codexHome "agents")
$hooksCopied = Copy-DirectoryChildren `
    -Source (Join-Path $setupRoot "hooks") `
    -Target (Join-Path $codexHome "hooks")

Write-Host "Codex setup restored to $codexHome"
Write-Host "Rules entries copied: $rulesCopied"
Write-Host "Skill entries copied: $skillsCopied"
Write-Host "Agent entries copied: $agentsCopied"
Write-Host "Hook entries copied: $hooksCopied"
Write-Host "Git config pull.rebase=$(git config --global --get pull.rebase)"
Write-Host "Git config rebase.autoStash=$(git config --global --get rebase.autoStash)"
Write-Host "Git config rerere.enabled=$(git config --global --get rerere.enabled)"

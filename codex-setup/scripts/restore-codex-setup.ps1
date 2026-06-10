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
        $destination = Join-Path $Target $_.Name
        if ($_.PSIsContainer) {
            if (Test-Path $destination) {
                Remove-Item -LiteralPath $destination -Recurse -Force
            }
            New-Item -ItemType Directory -Force -Path $destination | Out-Null
            Get-ChildItem -Path $_.FullName -Force | ForEach-Object {
                Copy-Item -Path $_.FullName -Destination $destination -Recurse -Force
            }
        } else {
            Copy-Item -Path $_.FullName -Destination $destination -Force
        }
        $count++
    }
    return $count
}

function ConvertTo-TomlBasicString {
    param([Parameter(Mandatory = $true)][string]$Value)
    return $Value.Replace('\', '\\').Replace('"', '\"')
}

function Set-TomlSectionKey {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Content,
        [Parameter(Mandatory = $true)][string]$Section,
        [Parameter(Mandatory = $true)][string]$Key,
        [Parameter(Mandatory = $true)][string]$Value
    )

    $lines = @($Content -split "\r?\n")
    if ($lines.Count -eq 1 -and $lines[0] -eq "") {
        $lines = @()
    }

    $sectionHeader = "[$Section]"
    $sectionIndex = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i].Trim() -eq $sectionHeader) {
            $sectionIndex = $i
            break
        }
    }

    if ($sectionIndex -lt 0) {
        $prefix = if ($lines.Count -gt 0 -and $lines[-1].Trim() -ne "") { @("") } else { @() }
        return (($lines + $prefix + @($sectionHeader, "$Key = $Value")) -join "`n").TrimEnd() + "`n"
    }

    $nextSectionIndex = $lines.Count
    for ($i = $sectionIndex + 1; $i -lt $lines.Count; $i++) {
        if ($lines[$i].TrimStart().StartsWith("[")) {
            $nextSectionIndex = $i
            break
        }
    }

    $keyPattern = "^\s*$([regex]::Escape($Key))\s*="
    for ($i = $sectionIndex + 1; $i -lt $nextSectionIndex; $i++) {
        if ($lines[$i] -match $keyPattern) {
            $lines[$i] = "$Key = $Value"
            return ($lines -join "`n").TrimEnd() + "`n"
        }
    }

    $before = if ($sectionIndex -ge 0) { $lines[0..$sectionIndex] } else { @() }
    $after = if ($sectionIndex + 1 -lt $lines.Count) { $lines[($sectionIndex + 1)..($lines.Count - 1)] } else { @() }
    return (($before + @("$Key = $Value") + $after) -join "`n").TrimEnd() + "`n"
}

function Ensure-CodexGitLockConfig {
    param([Parameter(Mandatory = $true)][string]$ConfigPath)

    $configDir = Split-Path -Parent $ConfigPath
    New-Item -ItemType Directory -Force -Path $configDir | Out-Null

    $content = ""
    if (Test-Path $ConfigPath) {
        $content = Get-Content -LiteralPath $ConfigPath -Raw -Encoding UTF8
    }

    $binPath = ConvertTo-TomlBasicString (Join-Path $env:USERPROFILE "bin")
    $localBinPath = ConvertTo-TomlBasicString (Join-Path $env:USERPROFILE ".local\bin")
    $pathValue = '"' + $binPath + ";" + $localBinPath + ';${PATH}"'

    $content = Set-TomlSectionKey -Content $content -Section "shell_environment_policy" -Key "inherit" -Value '"all"'
    $content = Set-TomlSectionKey -Content $content -Section "shell_environment_policy.set" -Key "Path" -Value $pathValue

    $hookPath = Join-Path $env:USERPROFILE ".codex\hooks\codex-git-multi-session-lock.cmd"
    $hookCommand = 'cmd /d /s /c ""{0}""' -f $hookPath
    $hookCommandPattern = '(?m)^command\s*=.*codex-git-multi-session-lock\.(?:cmd|ps1|sh).*$'
    if ($content -match $hookCommandPattern) {
        $content = [regex]::Replace($content, $hookCommandPattern, "command = '$hookCommand'")
    } elseif ($content -notmatch [regex]::Escape("codex-git-multi-session-lock.cmd")) {
        $content = $content.TrimEnd() + @"

[[hooks.PreToolUse]]
matcher = "^Bash$"

[[hooks.PreToolUse.hooks]]
type = "command"
command = '$hookCommand'
timeout = 130
statusMessage = "Checking git multi-session lock"
"@ + "`n"
    }

    Set-Content -LiteralPath $ConfigPath -Value $content -Encoding UTF8NoBOM
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
$binCopied = Copy-DirectoryChildren `
    -Source (Join-Path $setupRoot "bin") `
    -Target (Join-Path $env:USERPROFILE "bin")
$configPath = Join-Path $codexHome "config.toml"
Ensure-CodexGitLockConfig -ConfigPath $configPath

Write-Host "Codex setup restored to $codexHome"
Write-Host "Rules entries copied: $rulesCopied"
Write-Host "Skill entries copied: $skillsCopied"
Write-Host "Agent entries copied: $agentsCopied"
Write-Host "Hook entries copied: $hooksCopied"
Write-Host "Bin entries copied: $binCopied"
Write-Host "Codex config git lock ensured: $configPath"
Write-Host "Git config pull.rebase=$(git config --global --get pull.rebase)"
Write-Host "Git config rebase.autoStash=$(git config --global --get rebase.autoStash)"
Write-Host "Git config rerere.enabled=$(git config --global --get rerere.enabled)"

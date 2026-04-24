param(
    [string]$Workspace = (Get-Location).Path
)

Push-Location $Workspace
try {
    git config --global pull.rebase true
    git config --global rebase.autoStash true
    git config --global rerere.enabled true

    git fetch origin
    git diff --stat HEAD..origin/main
    git diff --name-status HEAD..origin/main
    git pull --rebase --autostash origin main

    $mcpSource = Join-Path $Workspace "codex-setup\mcp-windows.json"
    $mcpTarget = Join-Path $Workspace ".mcp.json"
    if (Test-Path $mcpSource) {
        Copy-Item $mcpSource $mcpTarget -Force
    }

    $rulesSource = Join-Path $Workspace "codex-setup\rules"
    $rulesTarget = Join-Path $env:USERPROFILE ".codex\rules"
    if (Test-Path $rulesSource) {
        New-Item -ItemType Directory -Force -Path $rulesTarget | Out-Null
        foreach ($ruleSource in Get-ChildItem -Path $rulesSource -Filter "*.md" -File) {
            Copy-Item -Force $ruleSource.FullName (Join-Path $rulesTarget $ruleSource.Name)
            Write-Host "Codex rule $($ruleSource.Name) synced to $rulesTarget"
        }
    }

    $repairScript = Join-Path $Workspace "codex-setup\scripts\repair_codex_skill_visibility.py"
    $reportPath = Join-Path $Workspace "codex-setup\state\skill-visibility-report.json"
    if (Test-Path $repairScript) {
        $python = Get-Command python -ErrorAction SilentlyContinue
        if (-not $python) {
            $python = Get-Command py -ErrorAction SilentlyContinue
        }
        if ($python) {
            try {
                & $python.Source $repairScript "--report-path" $reportPath
            } catch {
                Write-Warning "Skill visibility repair failed: $($_.Exception.Message)"
            }
        }
    }
} finally {
    Pop-Location
}

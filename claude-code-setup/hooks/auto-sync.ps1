# Auto-Sync: Syncs Claude Code config from GitHub on every session start
# Runs as SessionStart hook
# stdout → AI context (system-reminder), stderr → user terminal
# Platform: Windows (PowerShell 7+)

. "$PSScriptRoot/hook-log.ps1"
. "$PSScriptRoot/whiteboard-insert.ps1"

# Write to both stdout (AI context) and stderr (user-visible terminal)
function Write-Status {
    param([string]$Message)
    Write-Output $Message
    [Console]::Error.WriteLine($Message)
}

$RepoDir = Join-Path $env:USERPROFILE "proggs"
$SetupDir = Join-Path $RepoDir "claude-code-setup"
$ClaudeDir = Join-Path $env:USERPROFILE ".claude"

# Check if repo exists
if (-not (Test-Path (Join-Path $RepoDir ".git"))) {
    Hook-LogWarn "~/proggs repo not found — skipped"
    Write-Status "Auto-Sync: ~/proggs Repo nicht gefunden -- uebersprungen."
    exit 0
}

Push-Location $RepoDir -ErrorAction Stop

try {

# Fetch latest from remote
$null = git fetch --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Hook-LogWarn "git fetch failed — no internet?"
    Write-Status "Auto-Sync: Keine Internetverbindung -- uebersprungen."
    exit 0
}

# Compare local vs remote
$local = git rev-parse HEAD 2>$null
$remote = git rev-parse '@{u}' 2>$null

# Guard against no upstream configured (e.g. detached HEAD or new branch)
if (-not $remote) {
    Hook-LogWarn "no upstream tracking branch — skipped"
    exit 0
}

if ($local -eq $remote) {
    Write-Status "Auto-Sync: Alle Dateien aktuell."
    exit 0
}

# Updates available!
$behind = git rev-list --count "HEAD..@{u}" 2>$null
Write-Status "Auto-Sync: $behind neue Commits auf GitHub gefunden -- aktualisiere..."

# Preview: Show what's coming before pulling (ported from Codex session-start-sync)
$diffStat = git diff --stat "HEAD..@{u}" 2>$null
$diffNames = git diff --name-status "HEAD..@{u}" 2>$null
if ($diffStat) {
    Write-Status "Auto-Sync: Eingehende Aenderungen:"
    Write-Status $diffStat
}

# Discard uncommitted changes so rebase can proceed cleanly.
# At session start, uncommitted changes are only hook artifacts (MEMORY.md etc.)
# — all real work was committed and pushed in the previous session.
$dirty = git status --porcelain 2>$null
if ($dirty) {
    $null = git checkout -- . 2>&1
    Write-Status "Auto-Sync: Hook-Reste verworfen (nur Schmierzettel, nichts Wichtiges)."
}

# Pull with rebase
$null = git pull --rebase --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Hook-LogError "git pull --rebase failed — merge conflict?"
    $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: auto-sync.ps1 — git pull --rebase fehlgeschlagen (Merge-Konflikt?) — Status: OFFEN"
    Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
    Write-Status "Auto-Sync: FEHLER beim Pull (Merge-Konflikt?). Bitte manuell pruefen: cd ~/proggs; git status"
    exit 0  # SessionStart hooks must NEVER return exit 1 — error already logged to whiteboard
}

Write-Status "Auto-Sync: Git Pull erfolgreich."

# C2 (ported from Gemini): Write cross-CLI delta summary to whiteboard after pull
# This makes incoming changes visible for /self-improve and future sessions
try {
    $codexChanges = git log --oneline "$local..$remote" -- codex-setup/ 2>$null | Measure-Object | Select-Object -ExpandProperty Count
    $geminiChanges = git log --oneline "$local..$remote" -- Gemini-Setup/ 2>$null | Measure-Object | Select-Object -ExpandProperty Count
    if ($codexChanges -gt 0 -or $geminiChanges -gt 0) {
        $parts = @()
        if ($codexChanges -gt 0) { $parts += "Codex($codexChanges)" }
        if ($geminiChanges -gt 0) { $parts += "Gemini($geminiChanges)" }
        $cliSummary = $parts -join ", "
        $ts = Get-Date -Format "yyyy-MM-dd HH:mm"
        $entry = "- **[$ts] Cross-CLI Delta:** $cliSummary neue Commits — Bruecke starten fuer Details."
        Replace-WhiteboardEntry -Section "Forschung & Intelligence" -MatchPattern "Cross-CLI Delta" -Entry $entry
        Write-Status "Auto-Sync: Cross-CLI Delta erkannt: $cliSummary"
    }
} catch {
    Hook-LogWarn "Cross-CLI delta summary failed: $_"
}

# --- Sync config files from setup backup to active Claude Code config ---

$synced = @()

# Rules
$rulesDir = Join-Path $SetupDir "rules"
if (Test-Path $rulesDir) {
    New-Item -ItemType Directory -Force -Path (Join-Path $ClaudeDir "rules") | Out-Null
    $rules = @(Get-ChildItem "$rulesDir\*.md" -ErrorAction SilentlyContinue)
    if ($rules.Count -gt 0) {
        Copy-Item "$rulesDir\*.md" (Join-Path $ClaudeDir "rules") -Force
        $synced += "Rules($($rules.Count))"
    }
}

# Agents
$agentsDir = Join-Path $SetupDir "agents"
if (Test-Path $agentsDir) {
    New-Item -ItemType Directory -Force -Path (Join-Path $ClaudeDir "agents") | Out-Null
    $agents = @(Get-ChildItem "$agentsDir\*.md" -ErrorAction SilentlyContinue)
    if ($agents.Count -gt 0) {
        Copy-Item "$agentsDir\*.md" (Join-Path $ClaudeDir "agents") -Force
        $synced += "Agents($($agents.Count))"
    }
}

# Commands (including subdirectories like self-improve-ref/)
$commandsDir = Join-Path $SetupDir "commands"
if (Test-Path $commandsDir) {
    $destCommands = Join-Path $ClaudeDir "commands"
    New-Item -ItemType Directory -Force -Path $destCommands | Out-Null
    # Copy top-level .md files
    $commands = @(Get-ChildItem "$commandsDir\*.md" -ErrorAction SilentlyContinue)
    if ($commands.Count -gt 0) {
        Copy-Item "$commandsDir\*.md" $destCommands -Force
    }
    # Copy subdirectories recursively (e.g. self-improve-ref/)
    $subdirs = @(Get-ChildItem $commandsDir -Directory -ErrorAction SilentlyContinue)
    foreach ($subdir in $subdirs) {
        Copy-Item $subdir.FullName $destCommands -Recurse -Force
    }
    $totalCount = $commands.Count + $subdirs.Count
    if ($totalCount -gt 0) {
        $cmdLabel = "Commands($($commands.Count)+$($subdirs.Count) dirs)"
        $synced += $cmdLabel
    }
}

# Hooks (PowerShell + TypeScript + Bash scripts + subdirectories)
# Content-based sync: only update hooks that actually changed in incoming commits.
# Preserves local modifications that haven't been pushed to the setup-repo yet.
$hooksDir = Join-Path $SetupDir "hooks"
if (Test-Path $hooksDir) {
    $destHooks = Join-Path $ClaudeDir "hooks"
    New-Item -ItemType Directory -Force -Path $destHooks | Out-Null

    # Build list of hooks that actually changed in the incoming commits
    $changedHooks = @(git diff --name-only "$local" "$remote" -- claude-code-setup/hooks/ 2>$null | ForEach-Object { Split-Path $_ -Leaf })

    $hookCopied = 0
    $hookPreserved = 0
    $preservedNames = @()

    $allHooks = @(Get-ChildItem $hooksDir -File -ErrorAction SilentlyContinue | Where-Object { $_.Extension -in '.ps1','.ts','.sh' })
    foreach ($hook in $allHooks) {
        $localFile = Join-Path $destHooks $hook.Name

        if (-not (Test-Path $localFile)) {
            # New hook — always copy
            Copy-Item $hook.FullName $destHooks -Force
            $hookCopied++
        } elseif ((Get-FileHash $hook.FullName).Hash -eq (Get-FileHash $localFile).Hash) {
            # Content identical — already in sync, skip
        } elseif ($changedHooks -contains $hook.Name) {
            # Content differs AND hook changed in incoming commits — update from repo
            Copy-Item $hook.FullName $destHooks -Force
            $hookCopied++
            Hook-Log "Hook updated from repo: $($hook.Name)"
        } else {
            # Content differs but hook was NOT changed in pull — preserve local version
            $hookPreserved++
            $preservedNames += $hook.Name
        }
    }

    # Copy hook subdirectories (e.g. prompt-injection-defender/)
    # Guard: skip nested duplicates where a subdir has the same name as its parent
    # (prevents recursive nesting like defender/defender/defender/...)
    $hookSubdirs = @(Get-ChildItem $hooksDir -Directory -ErrorAction SilentlyContinue)
    foreach ($subdir in $hookSubdirs) {
        # Remove self-nested duplicates from SOURCE before copying
        $nestedSelf = Join-Path $subdir.FullName $subdir.Name
        if (Test-Path $nestedSelf) {
            Remove-Item $nestedSelf -Recurse -Force -ErrorAction SilentlyContinue
            Hook-Log "Removed self-nested duplicate: $nestedSelf"
        }
        Copy-Item $subdir.FullName $destHooks -Recurse -Force
    }

    if ($hookCopied -gt 0 -or $hookSubdirs.Count -gt 0) {
        $synced += "Hooks(${hookCopied}upd/$($allHooks.Count)total+$($hookSubdirs.Count)dirs)"
    } else {
        $synced += "Hooks(0upd/$($allHooks.Count)total+$($hookSubdirs.Count)dirs)"
    }

    if ($hookPreserved -gt 0) {
        $names = $preservedNames -join " "
        Hook-Log "Preserved $hookPreserved local hook(s) with uncommitted changes: $names"
        Write-Status "Auto-Sync: $hookPreserved Hook(s) mit lokalen Aenderungen beibehalten (commit+push to sync): $names"
    }
}

# CLAUDE.md — use the REPO version (~/proggs/CLAUDE.md) as authoritative source,
# NOT the setup backup (which may be older if another platform pushed changes).
# The repo copy is always pulled fresh by git pull above.
$repoClaude = Join-Path $RepoDir "CLAUDE.md"
if (Test-Path $repoClaude) {
    Copy-Item $repoClaude (Join-Path $env:USERPROFILE "CLAUDE.md") -Force
    $synced += "CLAUDE.md(from-repo)"
}

# Skills
$skillsDir = Join-Path $SetupDir "skills"
if (Test-Path $skillsDir) {
    $destSkills = Join-Path $ClaudeDir "skills"
    New-Item -ItemType Directory -Force -Path $destSkills | Out-Null
    # Copy skill directories recursively (each skill is a folder with SKILL.md)
    $skillSubdirs = @(Get-ChildItem $skillsDir -Directory -ErrorAction SilentlyContinue)
    foreach ($subdir in $skillSubdirs) {
        Copy-Item $subdir.FullName $destSkills -Recurse -Force
    }
    # Copy top-level skill files
    $skillFiles = @(Get-ChildItem "$skillsDir\*.md" -ErrorAction SilentlyContinue)
    if ($skillFiles.Count -gt 0) {
        Copy-Item "$skillsDir\*.md" $destSkills -Force
    }
    $totalSkills = $skillSubdirs.Count + $skillFiles.Count
    if ($totalSkills -gt 0) {
        $synced += "Skills($totalSkills)"
    }
}

# .gitignore_global
$gitignore = Join-Path $SetupDir ".gitignore_global"
if (Test-Path $gitignore) {
    Copy-Item $gitignore (Join-Path $env:USERPROFILE ".gitignore_global") -Force
    $synced += ".gitignore"
}

# .mcp.json — platform-specific sync (Windows version from mcp-windows.json)
# The .mcp.json is NOT tracked in git anymore — each platform generates its own.
$mcpSource = Join-Path $SetupDir "mcp-windows.json"
$mcpDest = Join-Path $RepoDir ".mcp.json"
if (Test-Path $mcpSource) {
    $needsCopy = $true
    if (Test-Path $mcpDest) {
        $srcHash = (Get-FileHash $mcpSource).Hash
        $dstHash = (Get-FileHash $mcpDest).Hash
        if ($srcHash -eq $dstHash) { $needsCopy = $false }
    }
    if ($needsCopy) {
        Copy-Item $mcpSource $mcpDest -Force
        $synced += ".mcp.json(windows)"
        Hook-Log ".mcp.json synced from mcp-windows.json"
    }
}

# GitHub Personal Access Token — auto-refresh from gh CLI
# Writes to settings.local.json (NOT settings.json — that gets committed to the repo)
try {
    $ghToken = & gh auth token 2>$null
    if ($LASTEXITCODE -eq 0 -and $ghToken) {
        $localSettings = Join-Path $ClaudeDir "settings.local.json"
        $localData = @{}
        if (Test-Path $localSettings) {
            $localData = Get-Content $localSettings -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable
        }
        if (-not $localData.ContainsKey("env")) { $localData["env"] = @{} }
        $currentToken = $localData["env"]["GITHUB_PERSONAL_ACCESS_TOKEN"]
        if ($currentToken -ne $ghToken) {
            $localData["env"]["GITHUB_PERSONAL_ACCESS_TOKEN"] = $ghToken
            # Atomic write via temp file
            $tmpFile = "$localSettings.tmp"
            $localData | ConvertTo-Json -Depth 10 | Out-File -FilePath $tmpFile -Encoding UTF8 -NoNewline
            Move-Item -Path $tmpFile -Destination $localSettings -Force
            $synced += "GH-Token(refreshed)"
            Hook-Log "GitHub PAT refreshed in settings.local.json"
        }
    }
} catch {
    Hook-LogWarn "GitHub token refresh failed: $_"
}

$syncedStr = $synced -join " "
Hook-Log "sync complete: $syncedStr"
Write-Status "Auto-Sync: Lokale Konfiguration aktualisiert: $syncedStr"
Write-Status "Auto-Sync: Hinweis -- CLAUDE.md und Rules werden erst nach Neustart von Claude Code wirksam."

} finally {
    Pop-Location
}

exit 0

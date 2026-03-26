# whiteboard-safe-pull.ps1 — Prevents MEMORY.md merge conflicts
# Called by whiteboard-insert.ps1 BEFORE writing to MEMORY.md
# Strategy: pull remote changes first, retry on conflict with auto-resolve
# Created: 2026-03-25 by Intelligenz-Vorschlag #715

$ErrorActionPreference = "SilentlyContinue"

$RepoDir = Join-Path $env:USERPROFILE "proggs"
$Whiteboard = ".claude/agent-memory/shared/MEMORY.md"

Set-Location $RepoDir -ErrorAction SilentlyContinue
if (-not $?) { exit 0 }

# Only act if we're in a git repo
$null = git rev-parse --is-inside-work-tree 2>&1
if ($LASTEXITCODE -ne 0) { exit 0 }

# Fetch remote
$null = git fetch origin main --quiet 2>&1
if ($LASTEXITCODE -ne 0) { exit 0 }

$local = git rev-parse HEAD 2>&1
$remote = git rev-parse origin/main 2>&1

# Nothing to do if up to date
if ($local -eq $remote) { exit 0 }

# Check if MEMORY.md has local changes
$diffFiles = git diff --name-only 2>&1
$stagedFiles = git diff --cached --name-only 2>&1
$hasLocalChanges = ($diffFiles -match $Whiteboard) -or ($stagedFiles -match $Whiteboard)

if ($hasLocalChanges) {
    # Stash whiteboard changes
    $null = git stash push -m "whiteboard-safe-pull auto-stash" -- $Whiteboard 2>&1

    # Pull remote
    $null = git pull --rebase origin main --quiet 2>&1

    # Re-apply
    $null = git stash pop --quiet 2>&1
    if ($LASTEXITCODE -ne 0) {
        # Conflict: accept theirs, we'll re-append our changes
        $null = git checkout --theirs $Whiteboard 2>&1
        $null = git add $Whiteboard 2>&1
        $null = git stash drop --quiet 2>&1
    }
} else {
    # No local changes, just pull
    $null = git pull --rebase origin main --quiet 2>&1
}

exit 0

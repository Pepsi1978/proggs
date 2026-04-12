# pre-push-fetch-rebase.ps1 — Automatic fetch + rebase before push
# Prevents push rejections when other CLIs (Codex, Gemini) pushed in the meantime
#
# Installation:
#   Copy to .git/hooks/pre-push (or register in settings.json)
#
# Windows counterpart of pre-push-fetch-rebase.sh

param(
    [string]$Remote = "origin"
)

# Only for pushes to origin
if ($Remote -ne "origin") {
    exit 0
}

Write-Host "[pre-push] Syncing with origin/main..."

# Fetch latest
$fetchResult = git fetch origin 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[pre-push] Warning: fetch failed, continuing with push."
    exit 0
}

# Check if rebase is needed
$remoteMain = git rev-parse origin/main 2>$null
if ($LASTEXITCODE -ne 0) { exit 0 }

$base = git merge-base HEAD origin/main 2>$null
if ($LASTEXITCODE -ne 0) { exit 0 }

if ($base -eq $remoteMain) {
    Write-Host "[pre-push] Already up-to-date."
    exit 0
}

Write-Host "[pre-push] Behind origin/main — rebasing..."

# Stash dirty changes
$dirty = $false
$diffStatus = git diff --quiet 2>$null
$diffCachedStatus = git diff --cached --quiet 2>$null
if ($LASTEXITCODE -ne 0) {
    git stash --quiet 2>$null
    $dirty = $true
}

# Rebase
git rebase origin/main --quiet 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "[pre-push] Rebase successful."
} else {
    Write-Host "[pre-push] ERROR: Rebase conflict! Push aborted."
    Write-Host "[pre-push] Fix manually: git rebase --abort && git fetch origin && git rebase origin/main"
    git rebase --abort 2>$null
    if ($dirty) { git stash pop --quiet 2>$null }
    exit 1
}

# Restore stashed changes
if ($dirty) { git stash pop --quiet 2>$null }

exit 0

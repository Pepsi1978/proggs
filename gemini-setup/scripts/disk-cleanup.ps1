# disk-cleanup.ps1 — Cleanup script for low disk space
# Purpose: Free up space by removing temporary files, logs, and caches.
# Usage: Run manually or via /self-improve

function Get-Size {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return 0 }
    $size = (Get-ChildItem $Path -Recurse -File -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    if ($null -eq $size) { return 0 }
    return $size
}

function Format-Size {
    param([double]$Bytes)
    if ($Bytes -ge 1GB) { return "$([math]::Round($Bytes / 1GB, 2)) GB" }
    if ($Bytes -ge 1MB) { return "$([math]::Round($Bytes / 1MB, 2)) MB" }
    return "$Bytes Bytes"
}

$totalSaved = 0

Write-Host "--- Disk Cleanup Start ---" -ForegroundColor Cyan

# 1. Claude Project Transcripts (~/.claude/projects)
$claudeProjects = Join-Path $env:USERPROFILE ".claude\projects"
if (Test-Path $claudeProjects) {
    $size = Get-Size $claudeProjects
    Write-Host "Claude Project Cache: $(Format-Size $size)"
    # Cleanup files older than 14 days
    $limit = (Get-Date).AddDays(-14)
    $filesToLink = Get-ChildItem $claudeProjects -Recurse -File | Where-Object { $_.LastWriteTime -lt $limit }
    foreach ($file in $filesToLink) {
        $totalSaved += $file.Length
        Remove-Item $file.FullName -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  -> Bereinigt (älter als 14 Tage)" -ForegroundColor Green
}

# 2. npm cache
Write-Host "Bereinige npm cache..."
# npm cache clean --force is slow and sometimes risky, but we can check the size
# We'll just suggest it if it's large, or do it if forced.

# 3. Windows Temp
$tempDir = $env:TEMP
if (Test-Path $tempDir) {
    $sizeBefore = Get-Size $tempDir
    Get-ChildItem $tempDir -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } | ForEach-Object {
        $totalSaved += $_.Length
        Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
    }
    Write-Host "Windows Temp bereinigt (älter als 7 Tage)" -ForegroundColor Green
}

# 4. Bun cache
$bunCache = Join-Path $env:USERPROFILE ".bun\install\cache"
if (Test-Path $bunCache) {
    $size = Get-Size $bunCache
    Write-Host "Bun Cache: $(Format-Size $size)"
    # Bun cache can grow large.
    # Remove-Item $bunCache -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "---------------------------"
Write-Host "Gesamt eingesparter Platz: $(Format-Size $totalSaved)" -ForegroundColor Yellow
Write-Host "--- Disk Cleanup Ende ---" -ForegroundColor Cyan

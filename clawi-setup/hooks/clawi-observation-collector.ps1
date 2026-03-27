# clawi-observation-collector.ps1 - Automatischer Gedächtnis-Staubsauger
# Scannt die OpenClaw Transkripte nach "🔍 Selbstbeobachtung" und sichert sie im Daily Log.

$ErrorActionPreference = "SilentlyContinue"

# Pfade
$SessionsDir = Join-Path $env:USERPROFILE ".openclaw\agents\main\sessions"
$WorkspaceDir = Join-Path $env:USERPROFILE ".openclaw\workspace"
$MemoryDir = Join-Path $WorkspaceDir "memory"
$Today = Get-Date -Format "yyyy-MM-dd"
$LogFile = Join-Path $MemoryDir "$Today.md"

Write-Host "🦖 Clawi Gedächtnis-Staubsauger startet..." -ForegroundColor Cyan

# 1. Neuestes Transkript finden
$LatestFile = Get-ChildItem $SessionsDir -Filter "*.jsonl" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $LatestFile) {
    Write-Host "Kein Transkript gefunden."
    exit 0
}

# 2. Daily Log initialisieren falls nötig
if (-not (Test-Path $MemoryDir)) { New-Item -ItemType Directory $MemoryDir -Force }
if (-not (Test-Path $LogFile)) { 
    "# 🧠 Selbstbeobachtung - $Today`n" | Out-File $LogFile -Encoding utf8 
}

# 3. Transkript nach Beobachtungen scannen
$Entries = Get-Content $LatestFile.FullName | ForEach-Object { 
    try { $_ | ConvertFrom-Json } catch { $null } 
}

$CollectorCount = 0
foreach ($entry in $Entries) {
    if ($null -eq $entry -or $entry.role -ne "assistant") { continue }
    
    # Suche nach dem Block ab "🔍 Selbstbeobachtung"
    if ($entry.content -match "(🔍 Selbstbeobachtung[\s\S]*?)(?=\n\n|\n💡|\n---|\z)") {
        $observation = $Matches[1].Trim()
        
        # Prüfen ob bereits im Log (substring match)
        $CurrentLog = Get-Content $LogFile -Raw
        if ($CurrentLog -match [regex]::Escape($observation)) {
            continue
        }
        
        # Anhängen
        "`n### Gesammelt am $(Get-Date -Format 'HH:mm:ss')`n$observation`n" | Out-File $LogFile -Append -Encoding utf8
        $CollectorCount++
    }
}

if ($CollectorCount -gt 0) {
    Write-Host "✅ $CollectorCount neue Beobachtung(en) gesichert." -ForegroundColor Green
} else {
    Write-Host "Keine neuen Beobachtungen gefunden."
}

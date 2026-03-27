# clawi-observation-collector.ps1 - Automatischer Gedächtnis-Staubsauger (v3 - Deep Context)
# Scannt Transkripte nach "Selbstbeobachtung" und sichert User- + Assistant-Kontext.

$ErrorActionPreference = "SilentlyContinue"

# Unicode-Konstanten für Emojis (Windows-Resilienz)
$Magnifier = [char]::ConvertFromUtf32(0x1F50D)
$Bulb = [char]::ConvertFromUtf32(0x1F4A1)
$Dino = [char]::ConvertFromUtf32(0x1F996)
$Calendar = [char]::ConvertFromUtf32(0x1F5D3)
$Brain = [char]::ConvertFromUtf32(0x1F9E0)
$Target = [char]::ConvertFromUtf32(0x1F3AF)
$Rocket = [char]::ConvertFromUtf32(0x1F680)

# Pfade
$SessionsDir = Join-Path $env:USERPROFILE ".openclaw\agents\main\sessions"
$WorkspaceDir = Join-Path $env:USERPROFILE ".openclaw\workspace"
$MemoryDir = Join-Path $WorkspaceDir "memory"
$Today = Get-Date -Format "yyyy-MM-dd"
$LogFile = Join-Path $MemoryDir "$Today.md"

Write-Host "$Dino Clawi Gedächtnis-Staubsauger (v3) startet..." -ForegroundColor Cyan

# 1. Neueste Transkript finden
$LatestFile = Get-ChildItem $SessionsDir -Filter "*.jsonl" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $LatestFile) {
    Write-Host "Kein Transkript gefunden."
    exit 0
}

# 2. Daily Log initialisieren
if (-not (Test-Path $MemoryDir)) { New-Item -ItemType Directory $MemoryDir -Force }
if (-not (Test-Path $LogFile)) { 
    "# $Brain Selbstbeobachtung - $Today`n" | Out-File $LogFile -Encoding utf8 
}

# 3. Transkript laden
$Entries = Get-Content $LatestFile.FullName | ForEach-Object { 
    try { $_ | ConvertFrom-Json } catch { $null } 
}

$CollectorCount = 0
for ($i = 0; $i -lt $Entries.Count; $i++) {
    $entry = $Entries[$i]
    if ($null -eq $entry -or $entry.role -ne "assistant") { continue }
    
    # Suche nach Selbstbeobachtung
    $Pattern = [regex]::Escape($Magnifier) + " Selbstbeobachtung"
    if ($entry.content -match "($Pattern[\s\S]*?)(?=\n\n|\n$Bulb|\n---|$)") {
        $observation = $Matches[1].Trim()
        
        # Prüfen ob bereits im Log
        $CurrentLog = Get-Content $LogFile -Raw
        if ($CurrentLog -match [regex]::Escape($observation)) {
            continue
        }

        # --- KONTEXT-EXTRAKTION ---
        $userContext = "Unbekannt"
        $assistantThought = "Nicht dokumentiert"
        
        for ($j = $i - 1; $j -ge 0; $j--) {
            if ($Entries[$j].role -eq "user") {
                $userContext = $Entries[$j].content.Trim()
                if ($userContext.Length -gt 300) { $userContext = $userContext.Substring(0, 297) + "..." }
                break
            }
        }
        
        if ($entry.thought) {
            $assistantThought = $entry.thought.Trim()
        } elseif ($entry.content -match "<think>([\s\S]*?)</think>") {
            $assistantThought = $Matches[1].Trim()
        }
        if ($assistantThought.Length -gt 500) { $assistantThought = $assistantThought.Substring(0, 497) + "..." }

        # --- LOG-BLOCK BAUEN ---
        $logBlock = "`n### $Calendar Gesammelt am $(Get-Date -Format 'HH:mm:ss')`n"
        $logBlock += "**$Target User-Trigger:** $userContext`n`n"
        $logBlock += "**$Brain Clawi-Reasoning (Warum mir das auffiel):**`n$assistantThought`n`n"
        $logBlock += "**$Magnifier Erkenntnis:**`n$observation`n"
        $logBlock += "**$Rocket Prävention:** (Abgeleitet in ENVIRONMENT-FIXES.md oder DIREKTIVEN.md)`n"
        $logBlock += "---`n"
        
        $logBlock | Out-File $LogFile -Append -Encoding utf8
        $CollectorCount++
    }
}

if ($CollectorCount -gt 0) {
    Write-Host "OK. $CollectorCount neue Beobachtung(en) gesichert." -ForegroundColor Green
} else {
    Write-Host "Keine neuen Beobachtungen gefunden."
}

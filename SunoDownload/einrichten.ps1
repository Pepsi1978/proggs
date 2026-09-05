#Requires -Version 5.1
<#
    Richtet SunoDownload auf einem frischen Rechner ein.

    Nach `git clone` genügt ein Doppelklick auf Einrichten.cmd: Das Skript prüft die
    Voraussetzungen, holt die Abhängigkeiten und legt beide Desktop-Symbole an — mit
    dem Pfad, an dem das Repository hier tatsächlich liegt. Es ist gefahrlos
    wiederholbar; nichts wird gelöscht, vorhandene Verknüpfungen werden überschrieben.
#>

param(
    # Wohin die Songs geladen werden. Der Ordner wird angelegt, falls er fehlt.
    [string]$Ziel = 'C:\Suno Backup',
    # Ohne Nachfrage am Ende — für automatische Läufe.
    [switch]$Still
)

$ErrorActionPreference = 'Continue'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$PROJEKT = $PSScriptRoot
$Host.UI.RawUI.WindowTitle = 'SunoDownload einrichten'

function Schreib([string]$Text, [string]$Farbe = 'Gray') { Write-Host $Text -ForegroundColor $Farbe }

Write-Host ''
Schreib '  ════════════════════════════════════════════════════════════════' Cyan
Schreib '   SunoDownload einrichten' Cyan
Schreib '  ════════════════════════════════════════════════════════════════' Cyan
Schreib "   Projektordner : $PROJEKT" DarkGray
Schreib "   Zielordner    : $Ziel" DarkGray
Write-Host ''

$warnungen = @()

# ---------------------------------------------------------------- Node prüfen

Schreib '  [1/4] Node.js prüfen …' Gray
$node = Get-Command node.exe -ErrorAction SilentlyContinue
if (-not $node) {
    Schreib '        ❗ Node.js ist nicht installiert.' Red
    Schreib '           Nötig ist Version 24 oder neuer — sie führt TypeScript direkt aus,' DarkGray
    Schreib '           ohne Übersetzungsschritt. Herunterladen: https://nodejs.org' DarkGray
    Write-Host ''
    if (-not $Still) { Read-Host '  Enter zum Schließen' | Out-Null }
    exit 1
}
$fassung = (& node --version 2>&1 | Select-Object -First 1)
$haupt = 0
if ("$fassung" -match '^v(\d+)') { $haupt = [int]$Matches[1] }
if ($haupt -lt 24) {
    Schreib "        ❗ Node $fassung ist zu alt — nötig ist 24 oder neuer." Red
    Schreib '           Ältere Fassungen können TypeScript nicht direkt ausführen.' DarkGray
    Write-Host ''
    if (-not $Still) { Read-Host '  Enter zum Schließen' | Out-Null }
    exit 1
}
Schreib "        Node $fassung — passt." Green

# ---------------------------------------------------------------- Abhängigkeiten

Schreib '  [2/4] Abhängigkeiten holen (npm install) …' Gray
if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
    Schreib '        ❗ npm wurde nicht gefunden, obwohl Node da ist.' Red
    Schreib '           Node.js bitte noch einmal installieren, npm gehört dazu.' DarkGray
    Write-Host ''
    if (-not $Still) { Read-Host '  Enter zum Schließen' | Out-Null }
    exit 1
}
Push-Location $PROJEKT
$npmAusgabe = & npm.cmd install --no-audit --no-fund 2>&1 | ForEach-Object { "$_" }
$npmCode = $LASTEXITCODE
Pop-Location
if ($npmCode -ne 0) {
    Schreib '        ❗ npm install ist fehlgeschlagen:' Red
    foreach ($z in ($npmAusgabe | Select-Object -Last 8)) { Schreib "           $z" DarkGray }
    Write-Host ''
    if (-not $Still) { Read-Host '  Enter zum Schließen' | Out-Null }
    exit 1
}
Schreib '        node_modules steht bereit.' Green

# ---------------------------------------------------------------- Zielordner

Schreib '  [3/4] Zielordner …' Gray
if (Test-Path -LiteralPath $Ziel) {
    $anzahl = @(Get-ChildItem -LiteralPath $Ziel -Filter *.mp3 -File -ErrorAction SilentlyContinue).Count
    Schreib "        $Ziel gibt es schon ($anzahl MP3-Dateien)." Green
} else {
    New-Item -ItemType Directory -Path $Ziel -Force | Out-Null
    Schreib "        $Ziel angelegt." Green
}
# Weicht das Ziel vom eingebauten Standard ab, muss es beim Aufruf mitgegeben werden.
if ($Ziel -ne 'C:\Suno Backup') {
    $warnungen += "Der Zielordner weicht vom Standard ab. Den Downloader mit dem Pfad aufrufen: node downloader.ts `"$Ziel`""
}

# ---------------------------------------------------------------- Desktop-Symbole

Schreib '  [4/4] Desktop-Symbole anlegen …' Gray
$desktop = [Environment]::GetFolderPath('Desktop')
$wsh = New-Object -ComObject WScript.Shell

$symbole = @(
    @{ Name = 'Suno Backup';         Start = 'Neue-Songs-holen.cmd'; Symbol = 'suno-backup.ico';    Text = 'Holt neue Songs aus der Suno-Bibliothek' },
    @{ Name = 'Suno Handy-Abgleich'; Start = 'Handy-Abgleich.cmd';   Symbol = 'handy-abgleich.ico'; Text = 'Kopiert neue MP3s auf das Handy' }
)
foreach ($s in $symbole) {
    $startdatei = Join-Path $PROJEKT $s.Start
    if (-not (Test-Path -LiteralPath $startdatei)) {
        $warnungen += "$($s.Start) fehlt im Projektordner, Symbol $($s.Name) wurde nicht angelegt."
        continue
    }
    $lnk = $wsh.CreateShortcut((Join-Path $desktop "$($s.Name).lnk"))
    $lnk.TargetPath = $startdatei
    $lnk.WorkingDirectory = $PROJEKT
    $lnk.IconLocation = (Join-Path $PROJEKT $s.Symbol) + ',0'
    $lnk.Description = $s.Text
    $lnk.Save()
    Schreib "        $($s.Name) liegt auf dem Desktop." Green
}

# ---------------------------------------------------------------- adb (nur Hinweis)

$adb = Get-Command adb.exe -ErrorAction SilentlyContinue
if (-not $adb) {
    foreach ($o in @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ProgramFiles\Android\platform-tools\adb.exe",
        'C:\platform-tools\adb.exe')) {
        if (Test-Path $o) { $adb = $o; break }
    }
}
if (-not $adb) {
    $warnungen += 'adb wurde nicht gefunden — der Handy-Abgleich braucht die Android platform-tools. Das Songs-Holen läuft auch ohne.'
}

# ---------------------------------------------------------------- Bilanz

Write-Host ''
Schreib '  ════════════════════════════════════════════════════════════════' Cyan
if ($warnungen.Count -eq 0) {
    Schreib '   Fertig. Doppelklick auf „Suno Backup" holt die Songs,' Green
    Schreib '   „Suno Handy-Abgleich" kopiert sie aufs Handy.' Green
} else {
    Schreib '   Fertig — mit Anmerkungen:' Yellow
    foreach ($w in $warnungen) { Schreib "    • $w" Yellow }
}
Schreib '  ════════════════════════════════════════════════════════════════' Cyan

Write-Host ''
if (-not $Still) { Read-Host '  Enter zum Schließen' | Out-Null }
exit 0

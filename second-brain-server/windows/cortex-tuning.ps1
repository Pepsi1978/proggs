# cortex-tuning.ps1 - Windows-SMB-Client fuer den Cortex-Tunnel (10.8.0.1) optimieren.
#
# WARUM: Der Weg zum VPS in Paris hat rund 48 ms Latenz. Windows ist ab Werk fuer ein
# LAN mit unter 1 ms eingestellt: winzige Metadaten-Caches, kuenstliche Durchsatzbremse,
# wenige gleichzeitig ausstehende Befehle. Ueber einen Tunnel bedeutet jede dieser
# Voreinstellungen zusaetzliche Round-Trips - und genau die kosten die Zeit.
#
# Das Skript hebt die Client-Grenzen an. Es ist idempotent, zeigt vorher/nachher und
# kann mit -Zuruecksetzen die Windows-Standardwerte wiederherstellen.
#
# BENUTZUNG (als Administrator):
#   .\cortex-tuning.ps1                 # anwenden
#   .\cortex-tuning.ps1 -Zeigen         # nur den aktuellen Stand anzeigen
#   .\cortex-tuning.ps1 -Zuruecksetzen  # Windows-Standard wiederherstellen
#
# WICHTIG: Das hier beschleunigt das STOEBERN und mittelgrosse Kopien im Explorer.
# Fuer viele kleine Dateien bleibt der grosse Hebel das parallele Kopieren
# (cortex-paste.ps1 / cortex-copy.ps1) - Faktor 20 statt Faktor 2.

param(
    [switch]$Zuruecksetzen,
    [switch]$Zeigen
)

$ErrorActionPreference = "Stop"

$LogDir = Join-Path $env:LOCALAPPDATA "cortex-copy"
$Log = Join-Path $LogDir "cortex-tuning.log"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

function Schreibe-Log {
    param([string]$Stufe, [string]$Text)
    $zeile = '{{"ts":"{0}","level":"{1}","module":"cortex-tuning","msg":"{2}"}}' -f `
        (Get-Date -Format "yyyy-MM-ddTHH:mm:sszzz"), $Stufe, ($Text -replace '"', '\"')
    Add-Content -Path $Log -Value $zeile -Encoding UTF8
}

# --- Die Werte -------------------------------------------------------------------------------
#
# Jeder Wert mit Begruendung. "Schnell" = auf hohe Latenz ausgelegt, "Standard" = Windows ab Werk.
#
#  DirectoryCacheLifetime     Wie lange ein Verzeichnis-Listing gilt. Ab Werk 10 s - danach fragt
#                             der Explorer bei jedem Blick erneut den Server. Ueber den Tunnel ist
#                             das der Hauptgrund fuer das traege Gefuehl beim Navigieren.
#  FileInfoCacheLifetime      Dito fuer Datei-Metadaten (Groesse, Datum, Attribute).
#  FileNotFoundCacheLifetime  Merkt sich "gibt es nicht". Programme fragen sehr oft nach nicht
#                             existierenden Dateien (Sidecar-Dateien, Thumbnails).
#  DirectoryCacheEntriesMax   Wie viele Verzeichnisse gleichzeitig im Cache liegen duerfen.
#  FileInfoCacheEntriesMax    Dito fuer Datei-Metadaten.
#  FileNotFoundCacheEntriesMax  Dito fuer Negativ-Treffer.
#  MaxCmds                    Wie viele SMB-Befehle gleichzeitig unbeantwortet unterwegs sein
#                             duerfen. Ab Werk 50. Bei 48 ms Latenz ist das die Pipeline-Tiefe -
#                             mehr ausstehende Befehle heisst mehr Arbeit pro Round-Trip.
#  EnableBandwidthThrottling  Windows drosselt SMB ABSICHTLICH, um andere Verbindungen zu schonen.
#                             Auf einer WAN-Strecke kostet das direkt Durchsatz. Aus.
#  DormantFileLimit           Wie viele Datei-Handles nach dem Schliessen offen gehalten werden -
#                             ein erneutes Oeffnen spart dann den kompletten Open-Round-Trip.
#  EnableLargeMtu             Grosse SMB-Lese-/Schreibeinheiten (1 MB statt 64 KB). Ab Werk schon
#                             an, wird nur zur Sicherheit gesetzt.
#  UseOpportunisticLocking    Erlaubt Client-seitiges Caching von Dateiinhalten. Muss an bleiben.
#
# NICHT angefasst: RequireSecuritySignature / smb encrypt. Signing und Verschluesselung kosten
# CPU, aber KEINE Round-Trips - und der Server verlangt sie ohnehin (smb encrypt = required).
# Abschalten braechte also kaum Tempo, wuerde aber Sicherheit aufgeben.

$Schnell = @{
    DirectoryCacheLifetime      = 60
    FileInfoCacheLifetime       = 60
    FileNotFoundCacheLifetime   = 30
    MaxCmds                     = 2048
    DormantFileLimit            = 4096
    EnableBandwidthThrottling   = $false
    EnableLargeMtu              = $true
    UseOpportunisticLocking     = $true
}

$Standard = @{
    DirectoryCacheLifetime      = 10
    FileInfoCacheLifetime       = 10
    FileNotFoundCacheLifetime   = 5
    MaxCmds                     = 50
    DormantFileLimit            = 1023
    EnableBandwidthThrottling   = $true
    EnableLargeMtu              = $true
    UseOpportunisticLocking     = $true
}

# Diese drei kennt Set-SmbClientConfiguration nicht, sie sitzen nur in der Registry.
$RegPfad = "HKLM:\SYSTEM\CurrentControlSet\Services\LanmanWorkstation\Parameters"
$RegSchnell = @{
    DirectoryCacheEntriesMax    = 4096
    FileInfoCacheEntriesMax     = 32768
    FileNotFoundCacheEntriesMax = 32768
}
$RegStandard = @{
    DirectoryCacheEntriesMax    = 16
    FileInfoCacheEntriesMax     = 64
    FileNotFoundCacheEntriesMax = 128
}

function Zeige-Stand {
    param([string]$Titel)
    $c = Get-SmbClientConfiguration
    Write-Host ""
    Write-Host $Titel -ForegroundColor Cyan
    Write-Host ("  {0,-28} {1}" -f "Verzeichnis-Cache (s)",   $c.DirectoryCacheLifetime)
    Write-Host ("  {0,-28} {1}" -f "Datei-Info-Cache (s)",    $c.FileInfoCacheLifetime)
    Write-Host ("  {0,-28} {1}" -f "Nicht-gefunden-Cache (s)",$c.FileNotFoundCacheLifetime)
    Write-Host ("  {0,-28} {1}" -f "Gleichzeitige Befehle",   $c.MaxCmds)
    Write-Host ("  {0,-28} {1}" -f "Offen gehaltene Handles", $c.DormantFileLimit)
    Write-Host ("  {0,-28} {1}" -f "Durchsatzbremse an",      $c.EnableBandwidthThrottling)
    Write-Host ("  {0,-28} {1}" -f "Grosse Uebertragungen",   $c.EnableLargeMtu)
    foreach ($name in $RegSchnell.Keys) {
        $wert = (Get-ItemProperty -Path $RegPfad -Name $name -ErrorAction SilentlyContinue).$name
        if ($null -eq $wert) { $wert = "(Windows-Standard)" }
        Write-Host ("  {0,-28} {1}" -f $name, $wert)
    }
    Write-Host ""
}

if ($Zeigen) {
    Zeige-Stand "Aktueller SMB-Client-Stand:"
    exit 0
}

# --- Rechte pruefen --------------------------------------------------------------------------
$ich = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $ich.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host ""
    Write-Host "[FEHLER] Dieses Skript braucht Administratorrechte." -ForegroundColor Red
    Write-Host "         Rechtsklick auf PowerShell -> 'Als Administrator ausfuehren', dann:"
    Write-Host "         powershell -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    Write-Host ""
    exit 1
}

$werte    = if ($Zuruecksetzen) { $Standard }    else { $Schnell }
$regWerte = if ($Zuruecksetzen) { $RegStandard } else { $RegSchnell }
$modus    = if ($Zuruecksetzen) { "Windows-Standard" } else { "Tunnel-Profil (hohe Latenz)" }

Zeige-Stand "Vorher:"

Write-Host "Setze: $modus" -ForegroundColor Cyan
Schreibe-Log "info" "tuning start: $modus"

Set-SmbClientConfiguration @werte -Force

foreach ($name in $regWerte.Keys) {
    if ($Zuruecksetzen) {
        # Die Windows-Voreinstellung ist "Wert existiert gar nicht" - also entfernen statt setzen.
        Remove-ItemProperty -Path $RegPfad -Name $name -ErrorAction SilentlyContinue
    }
    else {
        New-ItemProperty -Path $RegPfad -Name $name -Value $regWerte[$name] `
                         -PropertyType DWord -Force | Out-Null
    }
}

Zeige-Stand "Nachher:"

Write-Host "[OK] Uebernommen." -ForegroundColor Green
Write-Host ""
Write-Host "Die Cache-Groessen aus der Registry greifen erst nach einem Neustart des Dienstes" -ForegroundColor Yellow
Write-Host "'Arbeitsstationsdienst' (LanmanWorkstation) - am einfachsten per Windows-Neustart." -ForegroundColor Yellow
Write-Host "Alles andere wirkt sofort."
Write-Host ""
Write-Host "Rueckgaengig jederzeit mit:  .\cortex-tuning.ps1 -Zuruecksetzen"
Write-Host "Protokoll: $Log"
Schreibe-Log "info" "tuning fertig: $modus"

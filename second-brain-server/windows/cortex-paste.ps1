# cortex-paste.ps1 - "Hier schnell einfuegen" fuer die Cortex-Laufwerke Y: und Z:.
#
# WARUM: Der Explorer kopiert ueber SMB Datei fuer Datei und wartet jeden Round-Trip
# voll ab. Bei rund 48 ms Latenz zum VPS in Paris schafft er so nur etwa 1,5 Dateien/s.
# Dasselbe Material parallel uebertragen (rclone, 64 gleichzeitig) schafft 29,8/s -
# rund Faktor 20. Gemessen 27.08.2026, siehe bugs/server/samba-wireguard.md §14.
#
# Dieses Skript nimmt die normale Windows-Zwischenablage entgegen. Der Ablauf bleibt
# also genau wie gewohnt:
#   1. Dateien/Ordner markieren, Strg+C (kopieren) oder Strg+X (verschieben)
#   2. Zielordner oeffnen - egal ob lokal oder auf Y:/Z:
#   3. Rechtsklick ins Leere -> "Cortex: Hier schnell einfuegen"
#
# Es erkennt selbst, ob hoch- oder heruntergeladen wird, ob kopiert oder verschoben
# werden soll, und waehlt das Uebertragungsprofil nach der mittleren Dateigroesse.
#
# Kontextmenue einrichten:  .\cortex-menu-install.ps1
# Direkter Aufruf:          .\cortex-paste.ps1 -Ziel "Y:\Filme"

param(
    [Parameter(Mandatory = $true)][string]$Ziel
)

$ErrorActionPreference = "Stop"

$LogDir = Join-Path $env:LOCALAPPDATA "cortex-copy"
$Log = Join-Path $LogDir "cortex-paste.log"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

function Schreibe-Log {
    param([string]$Stufe, [string]$Text)
    $zeile = '{{"ts":"{0}","level":"{1}","module":"cortex-paste","msg":"{2}"}}' -f `
        (Get-Date -Format "yyyy-MM-ddTHH:mm:sszzz"), $Stufe, ($Text -replace '"', '\"')
    Add-Content -Path $Log -Value $zeile -Encoding UTF8
}

function Abbruch {
    param([string]$Text)
    Write-Host ""
    Write-Host "[FEHLER] $Text" -ForegroundColor Red
    Schreibe-Log "error" $Text
    Write-Host ""
    Read-Host "Mit Enter schliessen"
    exit 1
}

$Host.UI.RawUI.WindowTitle = "Cortex: schnell einfuegen"
Write-Host "Cortex - schnell einfuegen" -ForegroundColor Cyan
Write-Host "Protokoll: $Log"
Write-Host ""

# --- Zwischenablage lesen -------------------------------------------------------------------
# Get-Clipboard -Format FileDropList liefert nur die Dateiliste, nicht die Absicht.
# Ob kopiert oder ausgeschnitten wurde, steht im Clipboard-Format "Preferred DropEffect":
# ein DWORD mit Bit 1 = Kopieren (2), Bit 2 = Verschieben (5 = Move|Link).
Add-Type -AssemblyName System.Windows.Forms

$eintraege = @()
$verschieben = $false
try {
    $daten = [System.Windows.Forms.Clipboard]::GetDataObject()
    if ($daten -and $daten.GetDataPresent([System.Windows.Forms.DataFormats]::FileDrop)) {
        $eintraege = @($daten.GetData([System.Windows.Forms.DataFormats]::FileDrop))
    }
    if ($daten -and $daten.GetDataPresent("Preferred DropEffect")) {
        $strom = $daten.GetData("Preferred DropEffect")
        $puffer = New-Object byte[] 4
        $null = $strom.Read($puffer, 0, 4)
        $effekt = [BitConverter]::ToInt32($puffer, 0)
        # DragDropEffects.Move = 2, Copy = 1 (die Shell setzt Move meist als 2, Copy als 5)
        $verschieben = (($effekt -band 2) -ne 0) -and (($effekt -band 1) -eq 0)
    }
}
catch {
    Abbruch "Zwischenablage nicht lesbar: $($_.Exception.Message)"
}

if ($eintraege.Count -eq 0) {
    Abbruch "In der Zwischenablage sind keine Dateien. Erst mit Strg+C kopieren oder Strg+X ausschneiden."
}

# --- Pfade auf rclone-Remotes abbilden -------------------------------------------------------
# Y: = \\10.8.0.1\daten  ->  cortex-daten:daten\...
# Z: = \\10.8.0.1\gedanken -> cortex-gedanken:gedanken\...
# Die Zuordnung wird aus den echten SMB-Mappings gelesen, nicht geraten - dann stimmt sie
# auch, wenn die Laufwerksbuchstaben mal andere sind.

$ShareVonBuchstabe = @{}
try {
    foreach ($m in (Get-SmbMapping -ErrorAction SilentlyContinue)) {
        if ($m.RemotePath -match '^\\\\10\.8\.0\.1\\([^\\]+)') {
            $ShareVonBuchstabe[$m.LocalPath.TrimEnd('\').ToUpper()] = $Matches[1]
        }
    }
}
catch { }

if ($ShareVonBuchstabe.Count -eq 0) {
    # Rueckfall auf die bekannte Standardbelegung, falls Get-SmbMapping nichts liefert.
    $ShareVonBuchstabe["Y:"] = "daten"
    $ShareVonBuchstabe["Z:"] = "gedanken"
}

# Liefert @{ IstRemote; Spez } - Spez ist entweder ein lokaler Pfad oder "cortex-<share>:<share>/<rest>".
function ZuSpez {
    param([string]$Pfad)

    $voll = $Pfad
    # UNC direkt auf den Server?
    if ($voll -match '^\\\\10\.8\.0\.1\\([^\\]+)\\?(.*)$') {
        $share = $Matches[1]
        $rest = $Matches[2] -replace '\\', '/'
        return @{ IstRemote = $true; Spez = "cortex-${share}:$share/$rest".TrimEnd('/'); Share = $share }
    }

    $buchstabe = ""
    if ($voll.Length -ge 2 -and $voll[1] -eq ':') { $buchstabe = $voll.Substring(0, 2).ToUpper() }

    if ($buchstabe -and $ShareVonBuchstabe.ContainsKey($buchstabe)) {
        $share = $ShareVonBuchstabe[$buchstabe]
        $rest = $voll.Substring(2).TrimStart('\') -replace '\\', '/'
        return @{ IstRemote = $true; Spez = "cortex-${share}:$share/$rest".TrimEnd('/'); Share = $share }
    }

    return @{ IstRemote = $false; Spez = $voll; Share = "" }
}

$zielInfo = ZuSpez $Ziel
$quelleIstRemote = (ZuSpez $eintraege[0]).IstRemote

if (-not $zielInfo.IstRemote -and -not $quelleIstRemote) {
    Write-Host "Weder Quelle noch Ziel liegt auf Y: oder Z: - hier bringt der schnelle Weg nichts." -ForegroundColor Yellow
    Write-Host "Bitte ganz normal mit Strg+V einfuegen."
    Write-Host ""
    Read-Host "Mit Enter schliessen"
    exit 0
}

# --- Vorbedingungen -------------------------------------------------------------------------
if (-not (Get-Command rclone -ErrorAction SilentlyContinue)) {
    Abbruch "rclone fehlt. Installieren mit:  winget install Rclone.Rclone"
}

$RcConf = Join-Path $env:USERPROFILE ".config\rclone\rclone.conf"
if (-not ((Test-Path $RcConf) -and (Select-String -Path $RcConf -Pattern '^\[cortex-' -Quiet))) {
    Abbruch "rclone-Remotes fehlen. Einmalig anlegen lassen mit:  .\cortex-copy.ps1 ls daten:"
}

if (-not (Test-Connection -ComputerName 10.8.0.1 -Count 2 -Quiet -ErrorAction SilentlyContinue)) {
    Abbruch "Server 10.8.0.1 antwortet nicht. Laeuft der WireGuard-Tunnel?"
}

# --- Profil nach mittlerer Dateigroesse ------------------------------------------------------
# Gemessen, nicht geraten: viele kleine Dateien brauchen Gleichzeitigkeit (64 Transfers),
# wenige grosse brauchen mehrere Streams pro Datei (sonst bleibt eine Datei bei ~25 Mbit/s).

$BasisOpts = @(
    "--buffer-size", "32M",
    "--smb-idle-timeout", "5m",
    "--retries", "5",
    "--low-level-retries", "20",
    "--stats", "3s",
    "--stats-one-line",
    "--progress",
    "--log-file", $Log,
    "--log-level", "INFO"
)

function MittlereGroesseKB {
    param([string[]]$Pfade)
    $summe = 0.0
    $anzahl = 0
    foreach ($p in $Pfade) {
        if (-not (Test-Path -LiteralPath $p)) { continue }
        $e = Get-Item -LiteralPath $p
        if ($e.PSIsContainer) {
            $mess = Get-ChildItem -LiteralPath $p -Recurse -File -ErrorAction SilentlyContinue |
                    Measure-Object -Property Length -Sum
            if ($mess -and $mess.Count -gt 0) { $summe += $mess.Sum; $anzahl += $mess.Count }
        }
        else { $summe += $e.Length; $anzahl += 1 }
    }
    if ($anzahl -eq 0) { return 0 }
    return [int](($summe / $anzahl) / 1KB)
}

# Beim Herunterladen liegt die Quelle auf dem Server - dann die Gegenseite fragen.
if ($quelleIstRemote) {
    $groesseKB = 0
    try {
        $j = (& rclone size (ZuSpez $eintraege[0]).Spez --json 2>$null) | ConvertFrom-Json
        if ($j.count -gt 0) { $groesseKB = [int](($j.bytes / $j.count) / 1KB) }
    } catch { $groesseKB = 0 }
}
else {
    $groesseKB = MittlereGroesseKB $eintraege
}

if ($groesseKB -gt 0 -and $groesseKB -lt 2048) {
    $profilText = "viele kleine Dateien (Schnitt $groesseKB KB) -> 64 gleichzeitig"
    $Opts = @("--transfers", "64", "--checkers", "64") + $BasisOpts
}
else {
    if ($groesseKB -gt 0) { $profilText = "grosse Dateien (Schnitt $([int]($groesseKB/1024)) MB) -> 8 gleichzeitig, je 8 Streams" }
    else { $profilText = "Standard -> 8 gleichzeitig, je 8 Streams" }
    $Opts = @("--transfers", "8", "--checkers", "8",
              "--multi-thread-streams", "8", "--multi-thread-cutoff", "16M") + $BasisOpts
}

# --- Uebersicht + Nachfrage beim Verschieben -------------------------------------------------
$aktion = if ($verschieben) { "Verschieben" } else { "Kopieren" }
$rcBefehl = if ($verschieben) { "move" } else { "copy" }

Write-Host "Aktion:  $aktion von $($eintraege.Count) Eintraegen" -ForegroundColor Cyan
Write-Host "Ziel:    $Ziel"
Write-Host "Profil:  $profilText" -ForegroundColor DarkCyan
Write-Host ""
Schreibe-Log "info" "$rcBefehl start: $($eintraege.Count) eintraege -> $Ziel ($profilText)"

if ($verschieben) {
    Write-Host "ACHTUNG: Verschieben loescht die Quelle nach erfolgreicher Uebertragung." -ForegroundColor Yellow
    $antwort = Read-Host "Fortfahren? (J/n)"
    if ($antwort -and $antwort -notmatch '^[jJyY]') {
        Write-Host "Abgebrochen, nichts veraendert."
        Schreibe-Log "info" "vom benutzer abgebrochen"
        exit 0
    }
    Write-Host ""
}

# --- Uebertragen -----------------------------------------------------------------------------
#
# rclone nimmt pro Aufruf genau EINE Quelle. Einzelne Dateien wuerden damit nacheinander
# laufen - und genau das ist ja das Problem, das wir loesen wollen. Deshalb:
#   - Dateien werden nach ihrem Elternordner GRUPPIERT und pro Gruppe mit --files-from
#     in EINEM Aufruf uebertragen, also alle gleichzeitig.
#   - Ordner bekommen je einen eigenen Aufruf (rclone parallelisiert darin selbst).

$zielSpez = $zielInfo.Spez
$fehler = 0
$tempDateien = @()

# Windows PowerShell 5.1 kann "Split-Path" mit -LiteralPath NICHT mit -Leaf kombinieren -
# der Parametersatz laesst sich nicht aufloesen, und genau diese 5.1 startet der
# Kontextmenue-Eintrag. Ohne -LiteralPath wiederum wuerde Split-Path eckige Klammern im
# Dateinamen als Platzhalter deuten. Darum direkt ueber [IO.Path]: kennt weder das eine
# noch das andere Problem.
function Eltern { param([string]$p) return [IO.Path]::GetDirectoryName($p.TrimEnd('')) }
function Blatt  { param([string]$p) return [IO.Path]::GetFileName($p.TrimEnd('')) }

# Nach Ordner/Datei trennen. Bei Remote-Quellen kann Test-Path je nach Mapping-Zustand
# haengen - deshalb dort ueber das Attribut des SMB-Pfades gehen, der ja gemappt ist.
$ordner = @()
$dateien = @()
foreach ($e in $eintraege) {
    if (Test-Path -LiteralPath $e -PathType Container) { $ordner += $e }
    elseif (Test-Path -LiteralPath $e) { $dateien += $e }
    else { Write-Host "Uebersprungen (nicht gefunden): $e" -ForegroundColor Yellow }
}

# Dateien nach Elternordner gruppieren. Bewusst von Hand statt mit Group-Object: dessen
# Skriptblock-Form ohne -Property bindet in Windows PowerShell 5.1 den Parametersatz nicht
# (und 5.1 ist genau das, was der Kontextmenue-Eintrag startet).
$gruppen = @{}
foreach ($d in $dateien) {
    $eltern = Eltern $d
    if (-not $gruppen.ContainsKey($eltern)) { $gruppen[$eltern] = @() }
    $gruppen[$eltern] += $d
}

foreach ($eltern in $gruppen.Keys) {
    $gruppe = $gruppen[$eltern]
    $quelleSpez = (ZuSpez $eltern).Spez
    $liste = Join-Path $env:TEMP ("cortex-paste-" + [Guid]::NewGuid().ToString("N") + ".txt")
    $tempDateien += $liste
    # rclone erwartet die Namen relativ zur Quelle, eine pro Zeile, UTF-8 ohne BOM.
    $namen = $gruppe | ForEach-Object { Blatt $_ }
    [IO.File]::WriteAllLines($liste, $namen, (New-Object Text.UTF8Encoding($false)))

    Write-Host "-> $($gruppe.Count) Datei(en) aus $eltern" -ForegroundColor DarkCyan
    & rclone $rcBefehl $quelleSpez $zielSpez --files-from $liste @Opts
    if ($LASTEXITCODE -ne 0) { $fehler++ }
}

# Ordner einzeln - jeweils in einen gleichnamigen Unterordner am Ziel, wie es der Explorer tut.
foreach ($o in $ordner) {
    $quelleSpez = (ZuSpez $o).Spez
    $name = Blatt $o
    $unterZiel = if ($zielInfo.IstRemote) { "$zielSpez/$name" } else { Join-Path $zielSpez $name }

    Write-Host "-> Ordner $name" -ForegroundColor DarkCyan
    & rclone $rcBefehl $quelleSpez $unterZiel @Opts
    if ($LASTEXITCODE -ne 0) { $fehler++ }
}

foreach ($t in $tempDateien) {
    if (Test-Path -LiteralPath $t) { [IO.File]::Delete($t) }
}

Write-Host ""
if ($fehler -eq 0) {
    Write-Host "[OK] Fertig." -ForegroundColor Green
    Schreibe-Log "info" "fertig ohne fehler"
    Start-Sleep -Seconds 3
}
else {
    Write-Host "[FEHLER] $fehler Uebertragung(en) fehlgeschlagen. Details: $Log" -ForegroundColor Red
    Schreibe-Log "error" "$fehler uebertragungen fehlgeschlagen"
    Read-Host "Mit Enter schliessen"
    exit 1
}

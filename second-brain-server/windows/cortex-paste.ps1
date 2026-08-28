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
# werden soll, und waehlt Profil UND Transportweg nach der mittleren Dateigroesse:
#
#   Schnitt <  2 MB  -> SMB, 64 gleichzeitig   (viele kleine Dateien: 18,7 statt 4,4 Dateien/s)
#   Schnitt >= 2 MB  -> SFTP, 4 gleichzeitig   (eine grosse Datei: 37,1 statt 10,2 Mbit/s)
#
# Der zweite Fall ist die Nachmessung vom 27.08.2026: rclone fuehrt --multi-thread-streams
# ueber SMB nicht wirklich parallel, eine einzelne grosse Datei bleibt dort bei rund
# 10 Mbit/s haengen. Ueber SFTP laeuft dieselbe Datei nahe am Tunnel-Maximum (Faktor 3,6).
# Umgekehrt kostet SFTP pro Datei viel Vorlauf - bei vielen kleinen gewinnt SMB klar.
# Fehlt der SSH-Schluessel oder antwortet SSH nicht, faellt alles auf SMB zurueck.
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

# Zugangsdaten und Serverlayout fuer den SFTP-Weg. Muss VOR ZuSpez stehen, weil dort
# schon der echte Serverpfad mitberechnet wird.
$SshKey = Join-Path $env:USERPROFILE "SK\second-brain\id_ed25519"
$SshUser = "root"
$SftpBasis = "/srv/samba"        # dort liegen die Freigaben daten/ und gedanken/
$ShareBesitzer = "frank:frank"

# Liefert @{ IstRemote; Spez; Share; Serverpfad } - Spez ist ein lokaler Pfad oder
# "cortex-<share>:<share>/<rest>", Serverpfad der echte Pfad auf dem Server (fuer SFTP).
function ZuSpez {
    param([string]$Pfad)

    $voll = $Pfad
    # UNC direkt auf den Server?
    if ($voll -match '^\\\\10\.8\.0\.1\\([^\\]+)\\?(.*)$') {
        $share = $Matches[1]
        $rest = $Matches[2] -replace '\\', '/'
        return @{ IstRemote = $true; Spez = "cortex-${share}:$share/$rest".TrimEnd('/'); Share = $share; Serverpfad = "$SftpBasis/$share/$rest".TrimEnd('/') }
    }

    $buchstabe = ""
    if ($voll.Length -ge 2 -and $voll[1] -eq ':') { $buchstabe = $voll.Substring(0, 2).ToUpper() }

    if ($buchstabe -and $ShareVonBuchstabe.ContainsKey($buchstabe)) {
        $share = $ShareVonBuchstabe[$buchstabe]
        $rest = $voll.Substring(2).TrimStart('\') -replace '\\', '/'
        return @{ IstRemote = $true; Spez = "cortex-${share}:$share/$rest".TrimEnd('/'); Share = $share; Serverpfad = "$SftpBasis/$share/$rest".TrimEnd('/') }
    }

    return @{ IstRemote = $false; Spez = $voll; Share = ""; Serverpfad = "" }
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

# --- SFTP-Transportweg: verfuegbar? ----------------------------------------------------------
# Windows-Falle: OpenSSH verweigert einen privaten Schluessel, auf den mehr als der eigene
# Benutzer zugreifen darf ("UNPROTECTED PRIVATE KEY FILE") - und Dateien in %USERPROFILE%
# erben ab Werk Rechte fuer SYSTEM und Administratoren. Einmal geradeziehen statt daran
# scheitern. rclone selbst prueft das nicht, nur ssh.exe (das wir fuer chown brauchen).
function Schluessel-Rechte-Richten {
    param([string]$Pfad)
    try {
        $acl = Get-Acl $Pfad
        $ich = [Security.Principal.WindowsIdentity]::GetCurrent().Name
        if (($acl.Access | Where-Object { $_.IdentityReference -ne $ich }).Count -eq 0) { return }
        & icacls.exe $Pfad /inheritance:r /grant:r "${ich}:R" | Out-Null
        Schreibe-Log "info" "schluessel-rechte auf nur $ich gesetzt"
    }
    catch { Schreibe-Log "warn" "schluessel-rechte nicht setzbar: $($_.Exception.Message)" }
}

# Erst pruefen, wenn der SFTP-Weg wirklich gebraucht wird - bei vielen kleinen Dateien
# nehmen wir ohnehin SMB und sparen uns den Verbindungsaufbau.
$script:SftpMoeglich = -1   # -1 = ungeprueft, 0 = nein, 1 = ja
function Sftp-Verfuegbar {
    if ($script:SftpMoeglich -ge 0) { return ($script:SftpMoeglich -eq 1) }
    if (-not (Test-Path -LiteralPath $SshKey)) {
        $script:SftpMoeglich = 0
        Write-Host "SFTP-Weg nicht verfuegbar (Schluessel fehlt) - nutze SMB." -ForegroundColor Yellow
        return $false
    }
    Schluessel-Rechte-Richten $SshKey
    & ssh.exe -i $SshKey -o BatchMode=yes -o ConnectTimeout=6 -o StrictHostKeyChecking=accept-new `
              "$SshUser@10.8.0.1" "true" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $script:SftpMoeglich = 1; return $true }
    $script:SftpMoeglich = 0
    Write-Host "SFTP-Weg nicht verfuegbar (SSH antwortet nicht) - nutze SMB." -ForegroundColor Yellow
    Schreibe-Log "warn" "sftp nicht verfuegbar, rueckfall auf smb"
    return $false
}

$SftpFlags = @("--sftp-host", "10.8.0.1", "--sftp-user", $SshUser,
               "--sftp-key-file", $SshKey,
               "--sftp-concurrency", "64", "--sftp-set-modtime=false")

# SSH laeuft als root, die Freigaben gehoeren frank:frank. Ohne Nacharbeit gehoerten per
# SFTP geschriebene Dateien root:root und waeren ueber den SMB-Mount nicht mehr aenderbar.
function Sftp-Besitzer-Richten {
    param([string]$Serverpfad)
    if (-not $Serverpfad) { return }
    $q = "'" + ($Serverpfad -replace "'", "'''") + "'"
    & ssh.exe -i $SshKey -o BatchMode=yes -o ConnectTimeout=6 "$SshUser@10.8.0.1" `
              "chown -R $ShareBesitzer $q 2>/dev/null" 2>$null | Out-Null
    Schreibe-Log "info" "besitzer gerichtet: $Serverpfad"
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

$Transport = "smb"

if ($groesseKB -gt 0 -and $groesseKB -lt 2048) {
    # Viele kleine Dateien: hier gewinnt SMB klar (18,7 gegen 4,4 Dateien/s), weil SFTP pro
    # Datei viel mehr Vorlauf kostet. Es zaehlt allein die Gleichzeitigkeit.
    $profilText = "viele kleine Dateien (Schnitt $groesseKB KB) -> SMB, 64 gleichzeitig"
    $Opts = @("--transfers", "64", "--checkers", "64") + $BasisOpts
}
elseif (Sftp-Verfuegbar) {
    # Grosse Dateien: ueber SMB bleibt EINE Datei bei rund 10 Mbit/s haengen, weil rclone die
    # Streams dort nicht wirklich parallel fuehrt. Ueber SFTP laeuft sie nahe am Maximum.
    $Transport = "sftp"
    if ($groesseKB -gt 0) { $profilText = "grosse Dateien (Schnitt $([int]($groesseKB/1024)) MB) -> SFTP, 4 gleichzeitig" }
    else { $profilText = "Standard -> SFTP, 4 gleichzeitig" }
    $Opts = @("--transfers", "4", "--checkers", "8") + $SftpFlags + $BasisOpts
}
else {
    if ($groesseKB -gt 0) { $profilText = "grosse Dateien (Schnitt $([int]($groesseKB/1024)) MB) -> SMB (SFTP nicht verfuegbar)" }
    else { $profilText = "Standard -> SMB (SFTP nicht verfuegbar)" }
    $Opts = @("--transfers", "8", "--checkers", "8",
              "--multi-thread-streams", "8", "--multi-thread-cutoff", "16M") + $BasisOpts
}

# Ein Ort auf dem Server sieht je nach Transportweg anders aus: als rclone-Remote
# ("cortex-daten:daten/x") oder als echter Pfad im Dateisystem (":sftp:/srv/samba/daten/x").
function FuerTransport {
    param($info)
    if (-not $info.IstRemote) { return $info.Spez }
    if ($Transport -eq "sftp") { return ":sftp:" + $info.Serverpfad }
    return $info.Spez
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

$zielSpez = FuerTransport $zielInfo
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
    $quelleSpez = FuerTransport (ZuSpez $eltern)
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
    $quelleSpez = FuerTransport (ZuSpez $o)
    $name = Blatt $o
    $unterZiel = if ($zielInfo.IstRemote) { "$zielSpez/$name" } else { Join-Path $zielSpez $name }

    Write-Host "-> Ordner $name" -ForegroundColor DarkCyan
    & rclone $rcBefehl $quelleSpez $unterZiel @Opts
    if ($LASTEXITCODE -ne 0) { $fehler++ }
}

foreach ($t in $tempDateien) {
    if (Test-Path -LiteralPath $t) { [IO.File]::Delete($t) }
}

# Ueber SFTP schreibt root - Besitzer zurueckdrehen, sonst gehoeren die Dateien am
# SMB-Mount niemandem, den Frank aendern darf.
if ($fehler -eq 0 -and $Transport -eq "sftp" -and $zielInfo.IstRemote) {
    Sftp-Besitzer-Richten $zielInfo.Serverpfad
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

# cortex-copy.ps1 - Schnelles Kopieren zum/vom Cortex-Server (10.8.0.1) ueber WireGuard.
#
# WARUM DIESES SKRIPT (gemessen 27.08.2026, siehe bugs/server/samba-wireguard.md §14):
#   Der Engpass zum VPS in Paris ist die LATENZ (rund 48 ms), nicht die Bandbreite.
#   Explorer/SMB arbeitet pro Datei seriell und wartet jeden Round-Trip voll ab:
#     - VIELE KLEINE Dateien (200 x 50 KB): Finder 1,5 Dateien/s und brach mit Timeouts ab;
#       rclone mit 64 gleichzeitigen Uebertragungen 29,8 Dateien/s -> rund Faktor 20.
#     - WENIGE GROSSE Dateien: Finder 33,1 Mbit/s, rclone 36,4 Mbit/s -> kaum Unterschied,
#       hier ist schlicht die Leitung voll (rund 60 Mbit/s Uplink).
#   Das Skript waehlt das passende Profil automatisch anhand der mittleren Dateigroesse.
#
# BENUTZUNG:
#   .\cortex-copy.ps1 push <lokaler-pfad> <daten:ziel>      # hochladen
#   .\cortex-copy.ps1 pull <daten:quelle> <lokaler-pfad>    # herunterladen
#   .\cortex-copy.ps1 sync <lokaler-pfad> <daten:ziel>      # abgleichen (Ziel wird zur Kopie!)
#   .\cortex-copy.ps1 ls   <daten:pfad>                     # auflisten
#   .\cortex-copy.ps1 bench                                 # Durchsatz messen
#
#   Server-Pfade beginnen mit dem Laufwerk: daten:... oder gedanken:...
#   Beispiel:  .\cortex-copy.ps1 push C:\Filme daten:Filme
#
# ACHTUNG bei "sync": macht das Ziel zur exakten Kopie der Quelle, loescht dort also
#   Dateien die es in der Quelle nicht mehr gibt. Das Skript fragt vorher nach.

param(
    [Parameter(Position = 0)][string]$Befehl = "",
    [Parameter(Position = 1)][string]$Erstes = "",
    [Parameter(Position = 2)][string]$Zweites = ""
)

$ErrorActionPreference = "Stop"

$LogDir = Join-Path $env:LOCALAPPDATA "cortex-copy"
$Log = Join-Path $LogDir "cortex-copy.log"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }

function Schreibe-Log {
    param([string]$Stufe, [string]$Text)
    $zeile = '{{"ts":"{0}","level":"{1}","module":"cortex-copy","msg":"{2}"}}' -f `
        (Get-Date -Format "yyyy-MM-ddTHH:mm:sszzz"), $Stufe, ($Text -replace '"', '\"')
    Add-Content -Path $Log -Value $zeile -Encoding UTF8
}

function Abbruch {
    param([string]$Text)
    Write-Host ""
    Write-Host "[FEHLER] $Text" -ForegroundColor Red
    Schreibe-Log "error" $Text
    exit 1
}

Write-Host "Protokoll: $Log"

# --- Vorbedingungen pruefen -----------------------------------------------------------------
$rclone = Get-Command rclone -ErrorAction SilentlyContinue
if (-not $rclone) {
    Abbruch "rclone fehlt. Installieren mit:  winget install Rclone.Rclone"
}

$RcConf = Join-Path $env:USERPROFILE ".config\rclone\rclone.conf"

# Selbstheilung (Direktive #3): fehlen die Remotes, werden sie aus dem SK-Ordner angelegt,
# statt nur zu meckern. Zugangsdaten kommen NUR aus SK (secrets-in-sk-folder.md), nie aus dem Repo.
$habenRemote = (Test-Path $RcConf) -and (Select-String -Path $RcConf -Pattern '^\[cortex-daten\]' -Quiet)
if (-not $habenRemote) {
    $SkEnv = Join-Path $env:USERPROFILE "SK\second-brain\samba.env"
    if (-not (Test-Path $SkEnv)) {
        Abbruch "rclone-Remotes fehlen UND $SkEnv ist nicht da. Ohne Zugangsdaten geht nichts."
    }
    $zeilen = Get-Content $SkEnv
    $benutzer = ($zeilen | Where-Object { $_ -like "SAMBA_USER=*" } | Select-Object -First 1)
    $kennwort = ($zeilen | Where-Object { $_ -like "SAMBA_PASS=*" } | Select-Object -First 1)
    if (-not $benutzer -or -not $kennwort) { Abbruch "SAMBA_USER/SAMBA_PASS fehlen in $SkEnv" }
    $benutzer = $benutzer.Substring(11)
    $kennwort = $kennwort.Substring(11)

    Write-Host "rclone-Remotes fehlen, lege sie aus $SkEnv an..." -ForegroundColor Cyan
    $rcDir = Split-Path $RcConf -Parent
    if (-not (Test-Path $rcDir)) { New-Item -ItemType Directory -Path $rcDir -Force | Out-Null }
    if (Test-Path $RcConf) {
        Copy-Item $RcConf ($RcConf + ".bak-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
    }
    $verdeckt = (& rclone obscure $kennwort)
    foreach ($share in @("daten", "gedanken")) {
        $block = @"

[cortex-$share]
type = smb
host = 10.8.0.1
user = $benutzer
pass = $verdeckt
share = $share
"@
        Add-Content -Path $RcConf -Value $block -Encoding UTF8
    }
    Remove-Variable kennwort, verdeckt -ErrorAction SilentlyContinue
    Write-Host "[OK] Remotes cortex-daten und cortex-gedanken angelegt." -ForegroundColor Green
    Schreibe-Log "info" "rclone-Remotes automatisch aus SK angelegt"
}

# Tunnel lebendig? Ohne WireGuard ist alles andere sinnlos.
if (-not (Test-Connection -ComputerName 10.8.0.1 -Count 2 -Quiet -ErrorAction SilentlyContinue)) {
    Abbruch "Server 10.8.0.1 antwortet nicht. Laeuft der WireGuard-Tunnel?"
}

# --- Parameter nach Dateigroesse waehlen (alles gemessen, nicht geraten) ---------------------
#
# Der Engpass ist die LATENZ (rund 48 ms), nicht die Bandbreite. Daraus folgen zwei voellig
# verschiedene Faelle, deshalb waehlt das Skript das Profil automatisch:
#
#   VIELE KLEINE Dateien: jede Datei kostet mehrere Round-Trips. Hier hilft nur, sehr viele
#     Dateien GLEICHZEITIG zu uebertragen. Gemessen (200 Dateien x 50 KB):
#       Explorer/SMB 1,5 Dateien/s (brach mit Timeouts ab) | rclone 8 parallel: 6,2/s
#       rclone 32 parallel: 21,0/s | 48: 26,2/s | 64: 29,8/s  -> rund Faktor 20.
#   WENIGE GROSSE Dateien: hier ist die Leitung schon mit wenigen Streams voll. Gemessen
#     (24 MB, leere Leitung): Explorer/SMB 33,1 Mbit/s vs. rclone 36,4 Mbit/s.
#     Wichtig ist hier --multi-thread-streams, sonst bleibt EINE grosse Datei bei rund 25 Mbit/s.

$BasisOpts = @(
    "--buffer-size", "32M",
    "--smb-idle-timeout", "5m",
    "--retries", "5",
    "--low-level-retries", "20",
    "--stats", "5s",
    "--progress",
    "--log-file", $Log,
    "--log-level", "INFO"
)

# Mittlere Dateigroesse eines lokalen Pfades in KB (0 = unbekannt).
function MittlereGroesseKB {
    param([string]$Pfad)
    if (-not (Test-Path $Pfad)) { return 0 }
    $eintrag = Get-Item $Pfad
    if (-not $eintrag.PSIsContainer) { return [int]($eintrag.Length / 1KB) }
    $mess = Get-ChildItem -Path $Pfad -Recurse -File -ErrorAction SilentlyContinue |
            Measure-Object -Property Length -Sum
    if (-not $mess -or $mess.Count -eq 0) { return 0 }
    return [int](($mess.Sum / $mess.Count) / 1KB)
}

# Setzt $script:RcloneOpts passend zur mittleren Dateigroesse.
function WaehleProfil {
    param([int]$KB)
    if ($KB -gt 0 -and $KB -lt 2048) {
        $text = "viele kleine Dateien (Schnitt $KB KB) -> 64 gleichzeitig"
        $script:RcloneOpts = @("--transfers", "64", "--checkers", "64") + $BasisOpts
    }
    else {
        if ($KB -gt 0) { $text = "grosse Dateien (Schnitt $([int]($KB/1024)) MB) -> 8 gleichzeitig, je 8 Streams" }
        else { $text = "Standard -> 8 gleichzeitig, je 8 Streams" }
        $script:RcloneOpts = @("--transfers", "8", "--checkers", "8",
                               "--multi-thread-streams", "8", "--multi-thread-cutoff", "16M") + $BasisOpts
    }
    Write-Host "Profil: $text" -ForegroundColor DarkCyan
    Schreibe-Log "info" "profil: $text"
}

$RcloneOpts = @()   # wird pro Befehl von WaehleProfil gesetzt

function ZuRemote {
    param([string]$Pfad)
    if ($Pfad -like "daten:*") {
        return "cortex-daten:daten/" + $Pfad.Substring(6)
    }
    elseif ($Pfad -like "gedanken:*") {
        return "cortex-gedanken:gedanken/" + $Pfad.Substring(9)
    }
    else {
        Abbruch "Server-Pfad muss mit 'daten:' oder 'gedanken:' beginnen, bekommen: '$Pfad'"
    }
}

switch ($Befehl) {
    "push" {
        if (-not $Erstes -or -not $Zweites) { Abbruch "Aufruf: cortex-copy.ps1 push <lokaler-pfad> <daten:ziel>" }
        if (-not (Test-Path $Erstes)) { Abbruch "Lokaler Pfad existiert nicht: $Erstes" }
        $ziel = ZuRemote $Zweites
        Schreibe-Log "info" "push start: $Erstes -> $ziel"
        Write-Host "Hochladen: $Erstes  ->  $Zweites" -ForegroundColor Cyan
        WaehleProfil (MittlereGroesseKB $Erstes)
        & rclone copy $Erstes $ziel @RcloneOpts
        if ($LASTEXITCODE -eq 0) { Write-Host "[OK] Fertig." -ForegroundColor Green }
        else { Abbruch "Fehlgeschlagen (Code $LASTEXITCODE), Details im Protokoll." }
    }

    "pull" {
        if (-not $Erstes -or -not $Zweites) { Abbruch "Aufruf: cortex-copy.ps1 pull <daten:quelle> <lokaler-pfad>" }
        $quelle = ZuRemote $Erstes
        Schreibe-Log "info" "pull start: $quelle -> $Zweites"
        Write-Host "Herunterladen: $Erstes  ->  $Zweites" -ForegroundColor Cyan
        # Groesse der Gegenseite erfragen, damit dasselbe Profil wie beim Hochladen greift
        $groesseKB = 0
        try {
            $j = (& rclone size $quelle --json 2>$null) | ConvertFrom-Json
            if ($j.count -gt 0) { $groesseKB = [int](($j.bytes / $j.count) / 1KB) }
        } catch { $groesseKB = 0 }
        WaehleProfil $groesseKB
        & rclone copy $quelle $Zweites @RcloneOpts
        if ($LASTEXITCODE -eq 0) { Write-Host "[OK] Fertig." -ForegroundColor Green }
        else { Abbruch "Fehlgeschlagen (Code $LASTEXITCODE), Details im Protokoll." }
    }

    "sync" {
        if (-not $Erstes -or -not $Zweites) { Abbruch "Aufruf: cortex-copy.ps1 sync <lokaler-pfad> <daten:ziel>" }
        if (-not (Test-Path $Erstes)) { Abbruch "Lokaler Pfad existiert nicht: $Erstes" }
        $ziel = ZuRemote $Zweites
        Write-Host ""
        Write-Host "ACHTUNG: Abgleich macht '$Zweites' zur exakten Kopie von '$Erstes'." -ForegroundColor Yellow
        Write-Host "         Dateien die dort liegen aber nicht in der Quelle werden GELOESCHT." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Zuerst der Trockenlauf, es wird noch nichts veraendert:"
        & rclone sync $Erstes $ziel --dry-run --transfers 8 --checkers 8
        Write-Host ""
        $antwort = Read-Host "Wirklich so ausfuehren? (tippe JA)"
        if ($antwort -ne "JA") {
            Write-Host "Abgebrochen, nichts veraendert."
            Schreibe-Log "info" "sync abgebrochen"
            exit 0
        }
        Schreibe-Log "info" "sync start: $Erstes -> $ziel"
        WaehleProfil (MittlereGroesseKB $Erstes)
        & rclone sync $Erstes $ziel @RcloneOpts
        if ($LASTEXITCODE -eq 0) { Write-Host "[OK] Abgleich fertig." -ForegroundColor Green }
        else { Abbruch "Fehlgeschlagen (Code $LASTEXITCODE)" }
    }

    "ls" {
        if (-not $Erstes) { Abbruch "Aufruf: cortex-copy.ps1 ls <daten:pfad>" }
        & rclone lsl (ZuRemote $Erstes) --max-depth 1
    }

    "bench" {
        Write-Host "=== Durchsatzmessung zum Cortex-Server ==="
        $tmp = Join-Path $env:TEMP ("cortexbench-" + [System.Guid]::NewGuid().ToString("N"))
        New-Item -ItemType Directory -Path $tmp -Force | Out-Null
        try {
            Write-Host "Erzeuge 6 x 8 MB Testdaten..."
            $puffer = New-Object byte[] (8MB)
            $zufall = [System.Random]::new(42)
            for ($i = 1; $i -le 6; $i++) {
                $zufall.NextBytes($puffer)
                [System.IO.File]::WriteAllBytes((Join-Path $tmp "t$i.bin"), $puffer)
            }
            Write-Host "Lade 48 MB hoch (8 parallele Streams)..."
            $start = Get-Date
            & rclone copy $tmp cortex-daten:daten/__bench --transfers 8 --checkers 8 `
                --multi-thread-streams 8 --buffer-size 32M --stats 5s --stats-one-line
            $dauer = ((Get-Date) - $start).TotalSeconds
            if ($dauer -lt 1) { $dauer = 1 }
            Write-Host ""
            Write-Host ("  Durchsatz: {0:N2} MB/s  =  {1:N1} Mbit/s" -f (48 / $dauer), (384 / $dauer))
            Write-Host "  (Zum Vergleich: Explorer/SMB schafft hier typisch rund 0,7 MB/s = 5 Mbit/s)"
            & rclone purge cortex-daten:daten/__bench 2>$null
            Schreibe-Log "info" ("bench: {0:N1}s fuer 48 MB" -f $dauer)
        }
        finally {
            Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
        }
    }

    default {
        Get-Content $PSCommandPath | Select-Object -Skip 1 -First 22 | ForEach-Object { $_ -replace '^# ?', '' }
        exit 1
    }
}

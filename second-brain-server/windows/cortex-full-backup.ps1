# cortex-full-backup.ps1 — zieht ein KOMPLETT-Backup des zweiten Gehirns auf DIESEN PC.
#
# Frank-Wunsch 2026-06-24: EIN Klick (Doppelklick auf Cortex-Backup.cmd, oder Stream-Deck) -> saemtliche
# Server-Daten werden gezogen und LOKAL auf diesem Rechner gespeichert. Komplett server-UNABHAENGIG:
# liegt das Backup hier, kann der Server bei Totalausfall damit neu aufgesetzt werden — auch wenn der
# Server gerade tot ist (DIESER PC zieht aktiv, statt dass der Server pusht).
#
# So funktioniert es: per SSH wird auf dem Server scripts/full-backup-create.sh angestossen (es baut EIN
# konsistentes .tar.gz: Qdrant-Gehirn-Snapshot + .env + agent-data + compose + Caddy-Volumes + wg0.conf).
# Danach holt scp dieses Archiv in einen datierten Ordner unter LOCAL_DEST.
#
# EINRICHTUNG (einmalig): siehe windows/BACKUP-RESTORE-README.md. Kurz: lege
#   %USERPROFILE%\SK\second-brain\backup.env  an mit (Beispiel):
#     SERVER_HOST=DEINE_SERVER_IP        # oeffentliche VPS-IP ODER 10.8.0.1 (WireGuard)
#     SSH_USER=root
#     SSH_PORT=22
#     SSH_KEY=%USERPROFILE%\SK\second-brain\id_cortex   # optional (sonst Passwort-Eingabe)
#     REMOTE_APP_DIR=/opt/second-brain
#     LOCAL_DEST=C:\CortexBackup
# (Liegt ausserhalb des Repos — secrets-in-sk-folder-Regel.)

$ErrorActionPreference = 'Stop'
function Info($m){ Write-Host "  $m" -ForegroundColor Gray }
function Ok($m){ Write-Host "  OK  $m" -ForegroundColor Green }
function Warn($m){ Write-Host "  !!  $m" -ForegroundColor Yellow }
function Die($m){ Write-Host "" ; Write-Host "FEHLER: $m" -ForegroundColor Red; Write-Host "Fenster bleibt offen — Enter zum Schliessen."; [void](Read-Host); exit 1 }

Write-Host ""
Write-Host "==== Cortex KOMPLETT-Backup auf diesen PC ====" -ForegroundColor Cyan

# ── 1) Konfiguration aus dem SK-Ordner lesen ────────────────────────────────────────────────────
$envFile = Join-Path $env:USERPROFILE 'SK\second-brain\backup.env'
if (-not (Test-Path $envFile)) { Die "Konfig fehlt: $envFile`nBitte nach windows\BACKUP-RESTORE-README.md anlegen." }
$cfg = @{}
foreach ($line in (Get-Content -LiteralPath $envFile)) {
  if ($line -match '^\s*#') { continue }
  if ($line -match '^\s*([A-Za-z_]+)\s*=\s*(.+?)\s*$') { $cfg[$matches[1]] = [Environment]::ExpandEnvironmentVariables($matches[2]) }
}
$SERVER  = $cfg['SERVER_HOST']
$SSHUSER = if ($cfg['SSH_USER']) { $cfg['SSH_USER'] } else { 'root' }
$SSHPORT = if ($cfg['SSH_PORT']) { $cfg['SSH_PORT'] } else { '22' }
$SSHKEY  = $cfg['SSH_KEY']
$APPDIR  = if ($cfg['REMOTE_APP_DIR']) { $cfg['REMOTE_APP_DIR'] } else { '/opt/second-brain' }
$DEST    = if ($cfg['LOCAL_DEST']) { $cfg['LOCAL_DEST'] } else { Join-Path $env:USERPROFILE 'CortexBackup' }
if (-not $SERVER) { Die "SERVER_HOST fehlt in $envFile" }

# ── 2) Falls ueber WireGuard (10.8.0.1): Tunnel kurz pruefen ─────────────────────────────────────
if ($SERVER -eq '10.8.0.1') {
  Info "Pruefe WireGuard-Tunnel (10.8.0.1:$SSHPORT)…"
  $c = $null
  try { $c = New-Object System.Net.Sockets.TcpClient; if (-not ($c.ConnectAsync($SERVER,[int]$SSHPORT).Wait(2500) -and $c.Connected)) { Die "WireGuard-Tunnel nicht erreichbar. Erst verbinden (Z:/Y: sichtbar?), dann erneut." } }
  catch { Die "WireGuard-Tunnel nicht erreichbar." } finally { if ($c){ $c.Close(); $c.Dispose() } }
  Ok "Tunnel steht."
}

# ── 3) SSH-Argumente bauen ──────────────────────────────────────────────────────────────────────
$ssh = (Get-Command ssh.exe -ErrorAction SilentlyContinue).Source
$scp = (Get-Command scp.exe -ErrorAction SilentlyContinue).Source
if (-not $ssh -or -not $scp) { Die "OpenSSH (ssh.exe/scp.exe) fehlt. In Windows: Einstellungen > Apps > Optionale Features > 'OpenSSH-Client' installieren." }
$sshArgs = @('-p', $SSHPORT, '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=15')
$scpArgs = @('-P', $SSHPORT, '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=15')
if ($SSHKEY -and (Test-Path $SSHKEY)) { $sshArgs += @('-i', $SSHKEY); $scpArgs += @('-i', $SSHKEY); Info "SSH-Key: $SSHKEY" }
else { Warn "Kein SSH-Key konfiguriert — ggf. Passwort-Eingabe noetig." }
$target = "$SSHUSER@$SERVER"

# ── 4) Server-Backup anstossen, Archiv-Pfad zurueckbekommen ─────────────────────────────────────
Info "Stosse Server-Backup an (kann 1-2 Min dauern)…"
$remoteCmd = "bash $APPDIR/scripts/full-backup-create.sh"
# ssh mischt seine (voellig normalen) stderr-Logzeilen via 2>&1 in die Pipeline. Bei
# ErrorActionPreference='Stop' wuerde JEDE dieser stderr-Zeilen als terminierender
# NativeCommandError geworfen -> der Backup-Knopf stirbt, obwohl das Server-Skript sauber lief
# (full-backup-create.sh loggt JEDE Zeile nach stderr). Darum hier temporaer 'Continue'.
$prevEAP = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$out = & $ssh @sshArgs $target $remoteCmd 2>&1
$ErrorActionPreference = $prevEAP
$out | ForEach-Object { if ($_ -match 'FERTIG|FEHLER|WARN') { Info $_ } }
# Die LETZTE nicht-leere stdout-Zeile des Skripts ist der Archiv-Pfad.
$archive = ($out | Where-Object { $_ -match '^/.+\.tar\.gz$' } | Select-Object -Last 1)
if (-not $archive) { Die "Server lieferte keinen Archiv-Pfad. Lief das Skript? (SSH-Ausgabe oben pruefen.)" }
Ok "Server-Archiv: $archive"

# ── 5) Archiv lokal ziehen ──────────────────────────────────────────────────────────────────────
$stamp = Get-Date -Format 'yyyy-MM-dd_HHmmss'
$localDir = Join-Path $DEST $stamp
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
$localFile = Join-Path $localDir (Split-Path $archive -Leaf)
Info "Ziehe Archiv nach $localFile …"
# scp schreibt Fortschritt/Meldungen ebenfalls nach stderr — bei EAP='Stop' droht derselbe
# NativeCommandError wie oben. Temporaer 'Continue'; das Ergebnis pruefen wir explizit unten
# (Test-Path + Groesse), ein echter Fehlschlag geht also nicht verloren.
$prevEAP = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& $scp @scpArgs "${target}:${archive}" $localFile
$ErrorActionPreference = $prevEAP
if (-not (Test-Path $localFile) -or (Get-Item $localFile).Length -lt 2000) { Die "Download fehlgeschlagen oder Datei zu klein." }
$mb = [math]::Round((Get-Item $localFile).Length/1MB, 2)
Ok "Lokal gespeichert: $localFile ($mb MB)"

# ── 6) Restore-Hilfe danebenlegen + Ordner oeffnen ──────────────────────────────────────────────
$readme = Join-Path $PSScriptRoot 'BACKUP-RESTORE-README.md'
if (Test-Path $readme) { Copy-Item $readme (Join-Path $localDir 'WIE-WIEDERHERSTELLEN.md') -Force -ErrorAction SilentlyContinue }
Write-Host ""
Ok "Backup vollstaendig. Mit dieser Datei kann der Server komplett neu aufgesetzt werden."
Info "Wiederherstellen: Datei auf den (frischen) Server kopieren -> 'sudo bash full-restore.sh <datei>'."
try { Start-Process explorer.exe $localDir } catch {}
Write-Host ""
Write-Host "Fertig. Enter zum Schliessen." -ForegroundColor Cyan
[void](Read-Host)

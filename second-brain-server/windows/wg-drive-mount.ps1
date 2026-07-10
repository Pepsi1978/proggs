# wg-drive-mount.ps1
# Mappt die "Gehirn"-Netzlaufwerke Z: (gedanken) und Y: (daten), sobald der WireGuard-Tunnel
# (10.8.0.1) erreichbar ist. Das ist der PRIMAERE, sichtbare Mapping-Weg (Direktive #3, robusteste
# Version): er laeuft im NICHT-erhoehten Benutzer-Token (Aufgabe "WG-Drive-Mount", RunLevel Limited)
# -> die Laufwerke erscheinen SOFORT in Franks normalem Explorer, OHNE EnableLinkedConnections und
# OHNE die UAC-Token-Isolation (siehe bugs/server/samba-wireguard.md SS10/SS11).
#
# Warum getrennt vom erhoehten wg-drive-reconnect.ps1 (Microsoft MapDrives-Linie, Recherche 2026-06-25):
# Microsoft mappt im offiziellen MapDrives.ps1 bewusst NICHT-elevated, weil im erhoehten Token gemappte
# Laufwerke im normalen Explorer unsichtbar sind und EnableLinkedConnections (das sie sichtbar macht) bei
# UAC "Prompt for credentials" versagt. Dieses Skript braucht KEIN Admin: Credentials lesen, HKCU-Cleanup,
# WNetAddConnection2-Mapping und cmdkey laufen alle im Benutzer-Kontext. Dienst-Start + EnableLinkedConnections
# (beides braucht Admin) macht der erhoehte wg-drive-reconnect.ps1; dort wird DIESES Skript zusaetzlich als
# Backup aufgerufen (Defense in Depth: zwei unabhaengige Wege, die Laufwerke zu mounten).
$ErrorActionPreference = 'SilentlyContinue'
$log = Join-Path $env:LOCALAPPDATA 'wg-drive-mount.log'
function Log($m) { try { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $m" | Out-File -FilePath $log -Append -Encoding utf8 } catch {} }

# Ist der Tunnel oben UND der SMB-Server da? Schneller TCP-Test auf den SMB-Port 445 von 10.8.0.1.
# Genau der Port, von dem Z:/Y: abhaengen - NIE ein fremder Dienst-Port. $c.Connected statt nur
# Wait()-Rueckgabe, Socket immer disposen.
function WgUp {
    $c = $null
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $done = $c.ConnectAsync('10.8.0.1', 445).Wait(2000)
        return ($done -and $c.Connected)
    } catch { return $false }
    finally { if ($c) { $c.Close(); $c.Dispose() } }
}

# WNetAddConnection2 / WNetCancelConnection2 (mpr.dll) - Netzlaufwerk-API OHNE interaktiven Prompt.
# Kein Subprozess (kein net.exe), kann nicht haengen. Ohne CONNECT_INTERACTIVE nie ein Dialog.
$mpr = @'
using System;
using System.Runtime.InteropServices;
public class Mpr {
  [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
  public struct NETRESOURCE {
    public int dwScope; public int dwType; public int dwDisplayType; public int dwUsage;
    public string lpLocalName; public string lpRemoteName; public string lpComment; public string lpProvider;
  }
  [DllImport("mpr.dll", CharSet=CharSet.Unicode)]
  public static extern int WNetAddConnection2(ref NETRESOURCE r, string password, string username, int flags);
  [DllImport("mpr.dll", CharSet=CharSet.Unicode)]
  public static extern int WNetCancelConnection2(string name, int flags, bool force);
}
'@
try { Add-Type -TypeDefinition $mpr -ErrorAction Stop } catch { Log "Add-Type Mpr FEHLER: $($_.Exception.Message)" }

# Lesbare Win32-Fehlertexte fuer das Log (Observability - Fehlercode allein sagt wenig).
function ErrText($code) {
    switch ($code) {
        0     { 'OK' }
        53    { 'Netzwerkpfad nicht gefunden (53) - Tunnel/Server weg?' }
        67    { 'Netzwerkname nicht gefunden (67) - Freigabe-Name falsch?' }
        85    { 'Laufwerksbuchstabe belegt (85)' }
        86    { 'Falsches Netzwerk-Passwort (86) - samba.env pruefen' }
        1219  { 'Credential-Konflikt (1219) - bestehende Sitzung mit anderem User' }
        1326  { 'Anmeldung fehlgeschlagen (1326) - User/Passwort falsch (samba.env)' }
        default { "Win32-Fehler $code" }
    }
}

# Credentials aus dem SK-Ordner lesen (NICHT im Repo - secrets-in-sk-folder-Regel).
$smbUser = $null; $smbPass = $null
$envFile = Join-Path $env:USERPROFILE 'SK\second-brain\samba.env'
if (Test-Path $envFile) {
    foreach ($line in (Get-Content -LiteralPath $envFile)) {
        if ($line -match '^\s*SAMBA_USER\s*=\s*(.+?)\s*$') { $smbUser = $matches[1] }
        if ($line -match '^\s*SAMBA_PASS\s*=\s*(.+?)\s*$') { $smbPass = $matches[1] }
    }
}
if ($smbUser -and $smbPass) { Log "Credentials geladen (User: $smbUser) [nicht-elevated Mount]" }
else { Log "WARNUNG: $envFile fehlt/unvollstaendig - Mapping ohne explizite Credentials (kann scheitern)" }

# Persistente HKCU:\Network-Eintraege entfernen (verhindert, dass Windows beim Login VOR dem Tunnel
# voreilig verbindet -> totes rotes-X-Mapping + Fehler 1219). Dieses Skript ist die Mapping-Quelle.
foreach ($drv in 'Y', 'Z') {
    $rp = "HKCU:\Network\$drv"
    if (Test-Path $rp) {
        Remove-Item -Path $rp -Recurse -Force -ErrorAction SilentlyContinue
        Log "${drv}: persistenten HKCU-Login-Eintrag entfernt (verhindert Boot-Race vor dem Tunnel)"
    }
}

# Auf den Tunnel warten (Handshake braucht ein paar Sekunden) - max ~120 s.
$up = $false
for ($i = 0; $i -lt 60; $i++) { if (WgUp) { $up = $true; break }; Start-Sleep -Seconds 2 }
if (-not $up) { Log 'WireGuard (445) nicht erreichbar - Mount abgebrochen'; exit 0 }

# cmdkey-Eintrag im Benutzer-Tresor setzen (Defense in Depth - schadet nicht, wenn schon vorhanden).
if ($smbUser -and $smbPass) { & cmdkey.exe /add:10.8.0.1 /user:$smbUser /pass:$smbPass 2>$null | Out-Null }

# Laufwerke pruefen + bei Bedarf neu verbinden - gesunde NICHT anfassen (kein Blinken).
# Reihenfolge bewusst Z: ZUERST (siehe 1219-Abraeumung unten). NICHT-persistent (Flag 0) -> kein neuer
# HKCU:\Network-Eintrag, den Windows beim naechsten Login voreilig verbinden wuerde; KEIN
# CONNECT_INTERACTIVE -> nie ein Prompt. Das Skript laeuft bei Login + alle 5 Min und stellt die
# Mappings tunnel-bewusst selbst her.
$map = [ordered]@{ 'Z:' = '\\10.8.0.1\gedanken'; 'Y:' = '\\10.8.0.1\daten' }
$RESOURCETYPE_DISK = 0x1
$FLAGS = 0

function Map-Drive($d, $remote) {
    $nr = New-Object Mpr+NETRESOURCE
    $nr.dwType = $RESOURCETYPE_DISK
    $nr.lpLocalName = $d
    $nr.lpRemoteName = $remote
    if ($smbUser -and $smbPass) { return [Mpr]::WNetAddConnection2([ref]$nr, $smbPass, $smbUser, $FLAGS) }
    else { return [Mpr]::WNetAddConnection2([ref]$nr, $null, $null, $FLAGS) }   # Fallback: Tresor
}

foreach ($d in $map.Keys) {
    $remote = $map[$d]
    $m = Get-SmbMapping -LocalPath $d -ErrorAction SilentlyContinue
    if ($m -and $m.RemotePath -eq $remote -and $m.Status -eq 'OK') { Log "$d ist OK"; continue }
    [Mpr]::WNetCancelConnection2($d, 0, $true) | Out-Null
    $code = Map-Drive $d $remote
    # Fehler 1219 = "mehrere Benutzernamen": noch eine Sitzung zum SELBEN Server mit anderen Credentials.
    # Alle Sitzungen zu 10.8.0.1 hart abraeumen (beide Buchstaben + Server-UNC + IPC$), dann neu mappen.
    if ($code -eq 1219) {
        Log "$d Konflikt 1219 (mehrere Benutzernamen) -> raeume alle Sitzungen zu 10.8.0.1 ab + Neuversuch"
        foreach ($k in $map.Keys) { [Mpr]::WNetCancelConnection2($k, 0, $true) | Out-Null }
        [Mpr]::WNetCancelConnection2('\\10.8.0.1', 0, $true) | Out-Null
        [Mpr]::WNetCancelConnection2('\\10.8.0.1\IPC$', 0, $true) | Out-Null
        Start-Sleep -Milliseconds 500
        $code = Map-Drive $d $remote
    }
    if ($code -eq 0) { Log "$d war nicht verbunden -> mount OK ($remote)" }
    else { Log "$d mount FEHLGESCHLAGEN: $(ErrText $code) [$remote]" }
}

# Explorer-Anzeigenamen setzen (Frank 2026-07-10): "Gedanken (Z:)" / "Daten (Y:)" statt
# "gedanken (\\10.8.0.1) (Z:)". Der String-Wert _LabelFromReg unter
# MountPoints2\##host#share ueberschreibt den im Explorer angezeigten Namen UND blendet den
# UNC-Pfad aus. Liegt in HKCU -> bleibt ueber Neustarts. Hier bei JEDEM Mount idempotent gesetzt,
# damit das Label unverlierbar ist, selbst wenn der MountPoints2-Cache mal geleert wird.
$labels = [ordered]@{ '\\10.8.0.1\gedanken' = 'Gedanken'; '\\10.8.0.1\daten' = 'Daten' }
$mp2 = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\MountPoints2'
foreach ($remote in $labels.Keys) {
    $key = Join-Path $mp2 ($remote -replace '\\', '#')
    try {
        if (-not (Test-Path $key)) { New-Item -Path $key -Force -ErrorAction Stop | Out-Null }
        New-ItemProperty -Path $key -Name '_LabelFromReg' -Value $labels[$remote] -PropertyType String -Force -ErrorAction Stop | Out-Null
        Log "Explorer-Label gesetzt: $($labels[$remote]) ($remote)"
    } catch { Log "Label-Set FEHLER fuer $remote : $($_.Exception.Message)" }
}
exit 0

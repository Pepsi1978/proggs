# wg-drive-reconnect.ps1
# Verbindet die "Gehirn"-Netzlaufwerke Z: (gedanken) und Y: (daten) automatisch neu, sobald der
# WireGuard-Tunnel (10.8.0.1) erreichbar ist. Behebt das rote X / "Nicht verfuegbar" nach Neustart
# oder Standby. Wird KOMPLETT UNSICHTBAR ueber wg-drive-reconnect.vbs durch eine Aufgabenplanung
# (Anmeldung + Wiederholung) gestartet - keine Fenster, keine Popups, kein Passwort-Dialog.
# Schreibt nur eine kleine Log-Datei (kein UI).
#
# === ROOT-CAUSE-FIX 2026-06-24 (#2 nach #47132) - warum das nach Neustart IMMER WIEDER versagte ===
# Die geplante Aufgabe laeuft mit RunLevel HIGHEST (elevated Token). Der Windows-Credential-Tresor ist
# TOKEN-GETRENNT: der im normalen (nicht-elevated) Kontext gespeicherte Eintrag "Domain:target=10.8.0.1"
# ist im ELEVATED Kontext der Aufgabe NICHT sichtbar. Folge: das alte `net use \\10.8.0.1\share /persistent:yes`
# (OHNE explizite Credentials) fand keinen Tresor-Eintrag -> wollte INTERAKTIV nach dem Benutzernamen fragen
# -> im unsichtbaren (wscript/Hidden) Kontext gibt es kein Eingabefenster -> net use HING EWIG ("Geben Sie den
# Benutzernamen ein"). Jeder 5-Min-Trigger erzeugte so einen weiteren haengenden net.exe-Prozess (Prozess-Leak),
# und Z:/Y: wurden NIE gemappt -> Laufwerke fehlten im Explorer. Interaktiv (nicht-elevated) lief es durch, weil
# dort der Tresor-Eintrag sichtbar war -> der Bug war im normalen Test unsichtbar.
# FIX (Poka-Yoke Stufe 3 - Prompt-Haengen KONZEPTIONELL unmoeglich): Wir mappen mit der API WNetAddConnection2
# (mpr.dll) und uebergeben die Credentials EXPLIZIT (aus SK\second-brain\samba.env, ausserhalb des Repos). Ohne
# das Flag CONNECT_INTERACTIVE kann diese API NIEMALS einen Dialog oeffnen -> sie gibt stattdessen einen
# Win32-Fehlercode zurueck. Kein Prompt, kein Haengen, kein Prozess-Leak - unabhaengig vom Token/Tresor.
$ErrorActionPreference = 'SilentlyContinue'
$log = Join-Path $env:LOCALAPPDATA 'wg-drive-reconnect.log'
function Log($m) { try { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $m" | Out-File -FilePath $log -Append -Encoding utf8 } catch {} }

# Ist der Tunnel oben UND der SMB-Server da? Schneller TCP-Test auf den SMB-Port 445 von 10.8.0.1.
# WICHTIG (Root-Cause-Fix 2026-06-24, #47132): Frueher wurde Port 8000 (brain-api) getestet -> Reconnect haing von
# einem FREMDEN Dienst ab. Jetzt genau der Port, von dem Z:/Y: abhaengen: 445 (SMB). $c.Connected statt nur
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
# Einmalig pro Lauf via Add-Type laden. Kein Subprozess (kein net.exe/net1.exe), kann nicht haengen.
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
if ($smbUser -and $smbPass) { Log "Credentials geladen (User: $smbUser)" }
else { Log "WARNUNG: $envFile fehlt/unvollstaendig - Mapping ohne explizite Credentials (kann scheitern)" }

# 0a) EnableLinkedConnections=1 sicherstellen (ZWEITER ROOT-CAUSE 2026-06-24) - der Grund, warum Frank
# die Laufwerke trotz erfolgreichem Mapping NICHT im Explorer sah. Diese Aufgabe laeuft ELEVATED (Highest)
# und mappt damit im erhoehten Token. Ohne diesen Registry-Wert sind erhoehte und normale (nicht-elevated)
# Netzlaufwerk-Mappings GETRENNT (UAC-Token-Isolation) -> Franks Explorer (nicht-elevated) sieht ein im
# Task gemapptes Z:/Y: NICHT. EnableLinkedConnections=1 verknuepft beide Token, sodass das Mapping sichtbar
# wird. Wirksam ab dem NAECHSTEN Login/Reboot. Idempotent (setzt nur, wenn fehlend/falsch). Quelle: MS KB.
try {
    $polPath = 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System'
    $cur = (Get-ItemProperty -Path $polPath -Name 'EnableLinkedConnections' -ErrorAction SilentlyContinue).EnableLinkedConnections
    if ($cur -ne 1) {
        New-ItemProperty -Path $polPath -Name 'EnableLinkedConnections' -Value 1 -PropertyType DWord -Force -ErrorAction Stop | Out-Null
        Log 'EnableLinkedConnections=1 gesetzt (wirksam ab naechstem Login/Reboot - macht erhoehte Mappings im Explorer sichtbar)'
    }
} catch { Log "EnableLinkedConnections konnte nicht gesetzt werden (nicht elevated?): $($_.Exception.Message)" }

# 0b) WireGuard-Dienst SICHERSTELLEN - der Tunnel-Dienst startet beim Booten manchmal nicht von selbst.
# Diese Aufgabe laeuft erhoeht (RunLevel Highest), darf den Dienst also starten + Auto-Wiederherstellung setzen.
$svc = 'WireGuardTunnel$pc'
try {
    $s = Get-Service -Name $svc -ErrorAction Stop
    Log "WireGuard-Dienst Status: $($s.Status)"
    if ($s.Status -ne 'Running') {
        & sc.exe failure $svc reset= 86400 actions= restart/5000/restart/30000/restart/60000 2>$null | Out-Null
        Start-Service -Name $svc -ErrorAction SilentlyContinue
        Log "WireGuard-Dienst war $($s.Status) -> Recovery gesetzt + gestartet"
    }
} catch { Log "WireGuard-Dienst nicht zugaenglich: $($_.Exception.Message)" }

# 1) Auf den Tunnel warten (Handshake braucht ein paar Sekunden) - max ~120 s
$up = $false
for ($i = 0; $i -lt 60; $i++) { if (WgUp) { $up = $true; break }; Start-Sleep -Seconds 2 }
if (-not $up) { Log 'WireGuard nach Start-Versuch nicht erreichbar - abgebrochen'; exit 0 }

# 2) cmdkey-Eintrag im AKTUELLEN (elevated) Token setzen - Defense in Depth: damit auch Windows-eigenes
# persistent-Reconnect (das VOR diesem Skript beim Login laeuft) im elevated Kontext einen Tresor-Eintrag
# findet und nicht ins Leere greift. Schadet nicht, wenn schon vorhanden.
if ($smbUser -and $smbPass) { & cmdkey.exe /add:10.8.0.1 /user:$smbUser /pass:$smbPass 2>$null | Out-Null }

# 2c) ROOT-CAUSE-FIX 2026-06-25 (#3) - das WINDOWS-EIGENE persistente Login-Reconnect ABSCHALTEN.
# WARUM es trotz #1/#2 nach Neustart IMMER WIEDER ein rotes X (und "mehrere Benutzernamen"/1219) gab:
# Y/Z standen als PERSISTENTE Mappings in HKCU:\Network. Windows verbindet persistente Netzlaufwerke
# SOFORT beim Login - da ist der WireGuard-Tunnel aber noch NICHT oben (Race). Ergebnis: ein TOTES
# "Nicht verfuegbar"-Mapping (rotes X) im NORMALEN (nicht-elevated) Token. Dieses Skript mappt zwar
# danach im ELEVATED Token frisch + korrekt (Log: "reconnect OK"), aber das tote NORMALE Mapping bleibt
# im Explorer als X stehen und KOLLIDIERT beim Anklicken (Fehler 1219: zwei Sitzungen zum selben Server
# 10.8.0.1 mit verschiedenen Credentials). Beweis im Log 2026-06-25: 10:35:21 beide "reconnect OK",
# trotzdem sah Frank Z mit X -> Token-getrennte Sicht. FIX (Poka-Yoke Stufe 3 - Race KONZEPTIONELL
# unmoeglich): die persistenten HKCU:\Network-Eintraege entfernen, sodass Windows beim Login GAR NICHT
# mehr voreilig (vor dem Tunnel) verbindet. Dieses Skript ist ab jetzt die EINZIGE Mapping-Quelle - es
# laeuft bei Login + alle 5 Min und mappt erst, wenn der Tunnel (445) oben ist.
foreach ($drv in 'Y', 'Z') {
    $rp = "HKCU:\Network\$drv"
    if (Test-Path $rp) {
        Remove-Item -Path $rp -Recurse -Force -ErrorAction SilentlyContinue
        Log "${drv}: persistenten HKCU-Login-Eintrag entfernt (verhindert Boot-Race vor dem Tunnel)"
    }
}

# 3) Laufwerke pruefen + bei Bedarf neu verbinden - gesunde NICHT anfassen (kein Blinken).
# Reihenfolge bewusst Z: ZUERST, dann Y: - falls Z einen 1219-Konflikt wirft und dabei alle Sitzungen
# abgeraeumt werden, wird das danach folgende Y: ohnehin frisch neu gemappt.
$map = [ordered]@{ 'Z:' = '\\10.8.0.1\gedanken'; 'Y:' = '\\10.8.0.1\daten' }
$RESOURCETYPE_DISK = 0x1
# Flag 0: NICHT CONNECT_UPDATE_PROFILE (0x1) -> nicht-persistent, also KEIN neuer HKCU:\Network-Eintrag,
# den Windows beim naechsten Login wieder voreilig (vor dem Tunnel) verbinden wuerde. KEIN
# CONNECT_INTERACTIVE -> die API kann NIEMALS einen Passwort-/Benutzer-Dialog oeffnen (kein Hang).
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
    # Ist das Laufwerk bereits gesund verbunden? Dann in Ruhe lassen.
    $m = Get-SmbMapping -LocalPath $d -ErrorAction SilentlyContinue
    if ($m -and $m.RemotePath -eq $remote -and $m.Status -eq 'OK') { Log "$d ist OK"; continue }
    # Alte (ggf. tote) Verbindung loesen - Fehler hier ist normal (war nicht verbunden).
    [Mpr]::WNetCancelConnection2($d, 0, $true) | Out-Null
    $code = Map-Drive $d $remote
    # Fehler 1219 = "Mehrfache Verbindungen ... mit mehreren Benutzernamen sind nicht zulaessig":
    # Es existiert noch eine Sitzung zum SELBEN Server 10.8.0.1 mit anderen/impliziten Credentials.
    # Windows erlaubt pro Server nur EINEN Credential-Satz. Loesung: ALLE Sitzungen zu 10.8.0.1 hart
    # abraeumen (beide Buchstaben + der nackte Server-UNC + IPC$), dann mit unseren expliziten
    # Credentials neu mappen. Genau das, was Frank manuell mit `net use ... /delete` gemacht hat.
    if ($code -eq 1219) {
        Log "$d Konflikt 1219 (mehrere Benutzernamen) -> raeume alle Sitzungen zu 10.8.0.1 ab + Neuversuch"
        foreach ($k in $map.Keys) { [Mpr]::WNetCancelConnection2($k, 0, $true) | Out-Null }
        [Mpr]::WNetCancelConnection2('\\10.8.0.1', 0, $true) | Out-Null
        [Mpr]::WNetCancelConnection2('\\10.8.0.1\IPC$', 0, $true) | Out-Null
        Start-Sleep -Milliseconds 500
        $code = Map-Drive $d $remote
    }
    if ($code -eq 0) { Log "$d war nicht verbunden -> reconnect OK ($remote)" }
    else { Log "$d reconnect FEHLGESCHLAGEN: $(ErrText $code) [$remote]" }
}
exit 0

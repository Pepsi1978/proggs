# Startet Codex Desktop (OpenAI.Codex, MSIX) bei der Windows-Anmeldung und
# schickt das Fenster anschliessend ins Benachrichtigungsfeld (Tray).
#
# Hintergrund: Das Paket deklariert keinen windows.startupTask und die App
# kennt kein Startflag fuer "versteckt starten". Beim Schliessen des Fensters
# (WM_CLOSE) bleibt der Prozess aber mit Tray-Symbol resident - genau das
# nutzt dieses Skript.

$ErrorActionPreference = 'Stop'

$AppId   = 'shell:AppsFolder\OpenAI.Codex_2p2nqsd0c76g0!App'
$Prozess = 'ChatGPT'
$LogPfad = Join-Path $env:LOCALAPPDATA 'CodexAutostart\start.log'

function Schreibe-Log([string]$Text) {
    try {
        $ordner = Split-Path $LogPfad -Parent
        if (-not (Test-Path $ordner)) { New-Item -ItemType Directory -Path $ordner -Force | Out-Null }
        "$(Get-Date -Format 'dd.MM.yyyy HH:mm:ss')  $Text" | Add-Content -Path $LogPfad -Encoding UTF8
    } catch { }
}

Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class CodexFenster {
    [DllImport("user32.dll")]
    public static extern bool PostMessage(IntPtr hWnd, uint msg, IntPtr wParam, IntPtr lParam);
    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll", EntryPoint = "GetWindowLongPtrW")]
    public static extern IntPtr GetWindowLongPtr(IntPtr hWnd, int nIndex);
}
"@

# Codex haelt ein transparentes Overlay-Hilfsfenster (WS_EX_TOOLWINDOW) offen.
# Das darf nicht mit dem echten Hauptfenster verwechselt werden.
function Hole-Hauptfenster {
    Get-Process -Name $Prozess -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 } |
        Where-Object {
            $exStyle = [int64][CodexFenster]::GetWindowLongPtr($_.MainWindowHandle, -20)
            ($exStyle -band 0x80) -eq 0   # kein WS_EX_TOOLWINDOW
        } |
        Select-Object -First 1
}

# Laeuft die App schon? Dann nichts tun.
if (Get-Process -Name $Prozess -ErrorAction SilentlyContinue) {
    Schreibe-Log 'Codex laeuft bereits - nichts zu tun.'
    return
}

# Nach der Anmeldung ist das System noch beschaeftigt; kurz Luft lassen.
Start-Sleep -Seconds 20

try {
    Start-Process $AppId
} catch {
    Schreibe-Log "Start fehlgeschlagen: $($_.Exception.Message)"
    return
}

# Auf das erste echte Fenster warten (Electron braucht ein paar Sekunden).
$fenster = $null
for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 1
    $fenster = Hole-Hauptfenster
    if ($fenster) { break }
}

if (-not $fenster) {
    Schreibe-Log 'Kein Hauptfenster gefunden - App bleibt so wie sie ist.'
    return
}

# Kurz durchatmen lassen, damit das Tray-Symbol registriert ist.
Start-Sleep -Seconds 3

$handle = $fenster.MainWindowHandle
[CodexFenster]::PostMessage($handle, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null  # WM_CLOSE
Start-Sleep -Seconds 4

$laeuft = Get-Process -Id $fenster.Id -ErrorAction SilentlyContinue
if ($laeuft) {
    Schreibe-Log 'Codex laeuft im Tray.'
} else {
    # Sicherheitsnetz: Falls eine kuenftige Version bei WM_CLOSE wirklich beendet,
    # neu starten und nur minimieren statt schliessen.
    Schreibe-Log 'WM_CLOSE hat die App beendet - starte neu und minimiere nur.'
    Start-Process $AppId
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 1
        $fenster = Hole-Hauptfenster
        if ($fenster) { break }
    }
    if ($fenster) {
        [CodexFenster]::ShowWindow($fenster.MainWindowHandle, 6) | Out-Null  # SW_MINIMIZE
        Schreibe-Log 'Codex laeuft minimiert in der Taskleiste.'
    } else {
        Schreibe-Log 'Neustart ohne Fenster - nichts minimiert.'
    }
}

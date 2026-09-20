#requires -Version 5.1
<#
    Startet Codex Desktop (MSIX-Paket OpenAI.Codex) dauerhaft mit Administratorrechten.

    Warum ein eigener Launcher noetig ist
    -------------------------------------
    1. Codex Desktop ist eine Electron/Chromium-App. Chromium beendet sich unter Windows
       selbst, wenn es erhoeht gestartet wurde, und startet sich sofort unerhoeht neu
       (de-elevation). Dagegen hilft genau ein Schalter: --do-not-de-elevate.
    2. Das MSIX-Manifest deklariert kein allowElevation. Ueber die normale
       Paket-Aktivierung (shell:AppsFolder) kann die App deshalb NIE erhoeht laufen.
       Der Start geht daher direkt ueber app\ChatGPT.exe im Paketordner.

    Grundregel fuer alles, was mit dem Fenster zu tun hat
    ----------------------------------------------------
    NIEMALS ShowWindow/SetForegroundWindow auf das Electron-Fenster anwenden. Electron
    verwaltet Sichtbarkeit und Eingabe-Routing selbst. Ein per Win32 sichtbar gemachtes
    Fenster ist zwar zu sehen, aber tot: der Renderer zeichnet nicht und nimmt keine
    Mausklicks an. Stattdessen:
      - Fenster zeigen  -> die .exe ein zweites Mal starten. Electrons
                           Single-Instance-Sperre meldet das der laufenden Instanz,
                           die daraufhin ihr Fenster selbst zeigt.
      - Fenster ins Tray -> WM_CLOSE posten. Codex bleibt dabei mit Tray-Symbol
                           resident; die App fuehrt das selbst sauber aus.

    Aufrufe
    -------
      .\Start-CodexAdmin.ps1               Fenster sichtbar starten (Desktop-Verknuepfung)
      .\Start-CodexAdmin.ps1 -Background   Beim Anmelden ins Tray starten (Autostart-Aufgabe)
#>
# Version 1.1.0 - 20.09.2026, 12:01 Uhr

param(
    [switch]$Background,
    # Beendet eine laufende Instanz ohne Rueckfrage und startet sichtbar neu.
    # Fuer den Fall, dass Codex haengt oder unerhoeht laeuft.
    [switch]$Neustart
)

$ErrorActionPreference = 'Stop'

# Chromium-Schalter, der die Selbst-De-Elevation verhindert. Ohne ihn ist ein
# erhoehter Start technisch unmoeglich.
$KeinDeElevate = '--do-not-de-elevate'

$StatusPfad = Join-Path $env:LOCALAPPDATA 'CodexAutostart\last-launch.json'

# Sammelt Zwischenschritte, damit im Fehlerfall nachvollziehbar ist, was der Launcher
# vorgefunden hat. Landet mit in der Statusdatei.
$script:Spur = New-Object System.Collections.ArrayList
function Notiere([string]$Text) { [void]$script:Spur.Add("$(Get-Date -Format 'HH:mm:ss')  $Text") }

function Schreibe-Status([bool]$Erfolg, [string]$Meldung, $Prozess) {
    try {
        $ordner = Split-Path $StatusPfad -Parent
        if (-not (Test-Path $ordner)) { New-Item -ItemType Directory -Path $ordner -Force | Out-Null }
        [pscustomobject]@{
            Success    = $Erfolg
            Message    = $Meldung
            Pid        = $Prozess
            Background = [bool]$Background
            Time       = (Get-Date -Format o)
            Verlauf    = @($script:Spur)
        } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $StatusPfad -Encoding UTF8
    } catch { }
}

function Zeige-Meldung([string]$Text, [string]$Titel) {
    if ($Background) { return }
    Add-Type -AssemblyName System.Windows.Forms
    [void][System.Windows.Forms.MessageBox]::Show($Text, $Titel)
}

function Frage-Nutzer([string]$Text, [string]$Titel) {
    if ($Background) { return $false }
    Add-Type -AssemblyName System.Windows.Forms
    $antwort = [System.Windows.Forms.MessageBox]::Show(
        $Text, $Titel,
        [System.Windows.Forms.MessageBoxButtons]::YesNo,
        [System.Windows.Forms.MessageBoxIcon]::Question)
    return ($antwort -eq [System.Windows.Forms.DialogResult]::Yes)
}

try {
    # --- Schritt 1: selbst erhoehen, falls noetig -------------------------------
    $identitaet = [Security.Principal.WindowsIdentity]::GetCurrent()
    $istAdmin = (New-Object Security.Principal.WindowsPrincipal($identitaet)).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)

    if (-not $istAdmin) {
        $argumente = '-NoLogo -NoProfile -NonInteractive -WindowStyle Hidden -File "' + $PSCommandPath + '"'
        if ($Background) { $argumente += ' -Background' }
        if ($Neustart)   { $argumente += ' -Neustart' }
        Start-Process -FilePath (Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe') `
                      -ArgumentList $argumente -Verb RunAs -WindowStyle Hidden
        exit 0
    }

    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class CodexAdminCheck {
 [DllImport("kernel32.dll", SetLastError=true)] static extern IntPtr OpenProcess(uint a,bool b,int c);
 [DllImport("advapi32.dll", SetLastError=true)] static extern bool OpenProcessToken(IntPtr p,uint a,out IntPtr t);
 [DllImport("advapi32.dll", SetLastError=true)] static extern bool GetTokenInformation(IntPtr t,int c,out int v,int n,out int l);
 [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);
 [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h,uint m,IntPtr w,IntPtr l);
 [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
 [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
 [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc f, IntPtr l);
 [DllImport("user32.dll")] static extern int GetWindowThreadProcessId(IntPtr h, out int pid);
 [DllImport("user32.dll", EntryPoint="GetWindowLongPtrW")] static extern IntPtr GetWindowLongPtr(IntPtr h, int i);
 [DllImport("user32.dll")] static extern IntPtr GetWindow(IntPtr h, uint c);
 [DllImport("user32.dll", CharSet=CharSet.Unicode)] static extern int GetClassNameW(IntPtr h, System.Text.StringBuilder s, int n);
 [DllImport("user32.dll", CharSet=CharSet.Unicode)] static extern int GetWindowTextLengthW(IntPtr h);
 delegate bool EnumProc(IntPtr h, IntPtr l);

 // Sucht das echte Hauptfenster - auch wenn es versteckt oder minimiert ist.
 // Process.MainWindowHandle findet nur SICHTBARE Fenster und ist bei einer Tray-App 0.
 // Codex haelt mehrere Chrome_WidgetWin_1-Fenster: transparente Overlays (WS_EX_TOOLWINDOW)
 // und Eigentuemer-Fenster werden aussortiert; das Hauptfenster traegt einen Titel.
 public static IntPtr FindeFenster(int pid) {
   IntPtr treffer = IntPtr.Zero;
   EnumWindows(delegate(IntPtr h, IntPtr l) {
     int p; GetWindowThreadProcessId(h, out p);
     if (p != pid) return true;
     if (GetWindow(h, 4) != IntPtr.Zero) return true;              // kein Eigentuemer-Fenster
     long ex = (long)GetWindowLongPtr(h, -20);
     if ((ex & 0x80) != 0) return true;                            // kein WS_EX_TOOLWINDOW
     var k = new System.Text.StringBuilder(64);
     GetClassNameW(h, k, 64);
     if (k.ToString() != "Chrome_WidgetWin_1") return true;
     if (GetWindowTextLengthW(h) == 0) return true;                // nur das betitelte Fenster
     treffer = h;
     return false;
   }, IntPtr.Zero);
   return treffer;
 }

 public static bool Elevated(int pid) {
   IntPtr p=OpenProcess(0x1000,false,pid), t=IntPtr.Zero;
   if(p==IntPtr.Zero) return false;
   try{
     if(!OpenProcessToken(p,8,out t)) return false;
     int v,l;
     if(!GetTokenInformation(t,20,out v,4,out l)) return false;
     return v==1;
   } finally { if(t!=IntPtr.Zero) CloseHandle(t); CloseHandle(p); }
 }
}
'@

    # --- Schritt 2: Paket und Programmdatei ermitteln ---------------------------
    $paket = Get-AppxPackage -Name OpenAI.Codex | Select-Object -First 1
    if (-not $paket) { throw 'Codex Desktop ist nicht installiert.' }
    $exe = Join-Path $paket.InstallLocation 'app\ChatGPT.exe'
    if (-not (Test-Path -LiteralPath $exe)) { throw "Programmdatei nicht gefunden: $exe" }

    # Codex startet viele Hilfsprozesse (Renderer, GPU, Utility). Der Hauptprozess ist
    # der einzige, dessen Elternprozess KEIN ChatGPT.exe ist.
    # Bewusst NICHT ueber die Kommandozeile unterscheiden: die Kommandozeile erhoehter
    # Prozesse ist je nach Kontext nicht lesbar und kommt dann leer zurueck.
    function Hole-Hauptprozesse {
        $alle = @(Get-CimInstance Win32_Process -Filter "Name='ChatGPT.exe'")
        $ids  = @($alle | ForEach-Object { [int]$_.ProcessId })
        @($alle | Where-Object { $ids -notcontains [int]$_.ParentProcessId })
    }

    # Startet die .exe erhoeht. UseShellExecute=false: CreateProcess erbt das erhoehte
    # Token. Ueber ShellExecute wuerde Windows die App als Paket aktivieren und dabei
    # auf normale Rechte zurueckfallen.
    function Starte-Exe {
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = $exe
        $info.Arguments = $KeinDeElevate
        $info.UseShellExecute = $false
        $info.WorkingDirectory = $env:USERPROFILE
        [Diagnostics.Process]::Start($info)
    }

    # --- Schritt 3: laufende Instanzen pruefen ----------------------------------
    # Electron laesst nur eine Instanz zu. Eine bereits laufende unerhoehte Instanz
    # blockiert jeden erhoehten Start - sie muss vorher weg.
    # @() beim Aufruf ist Pflicht: PowerShell entrollt Arrays beim Rueckgabewert, und ein
    # einzelnes CimInstance-Objekt hat keine .Count-Eigenschaft - die Abfrage liefe ins Leere.
    $laufend = @(Hole-Hauptprozesse)
    $unerhoeht = @($laufend | Where-Object { -not [CodexAdminCheck]::Elevated([int]$_.ProcessId) })
    Notiere ("Hauptprozesse: " + $laufend.Count + " (davon unerhoeht: " + $unerhoeht.Count + ") - PIDs: " +
             (($laufend | ForEach-Object { $_.ProcessId }) -join ', '))

    # Harter Neustart auf Ansage: keine Rueckfrage, keine Ruecksicht auf den Rechtestand.
    if ($Neustart -and $laufend.Count) {
        Notiere 'Neustart angefordert - beende alle Codex-Prozesse.'
        Get-Process -Name ChatGPT -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
        $frist = (Get-Date).AddSeconds(15)
        while (@(Hole-Hauptprozesse).Count -and (Get-Date) -lt $frist) { Start-Sleep -Milliseconds 300 }
        if (@(Hole-Hauptprozesse).Count) { throw 'Codex liess sich nicht beenden. Bitte manuell schliessen und erneut versuchen.' }
        Start-Sleep -Seconds 2
        $laufend = @()
        $unerhoeht = @()
    }

    if ($laufend.Count -and -not $unerhoeht.Count) {
        # Laeuft schon erhoeht. Beim manuellen Start soll das Fenster nach vorne kommen.
        # Dafuer die .exe ein zweites Mal starten: Electrons Single-Instance-Sperre
        # uebergibt das an die laufende Instanz, die ihr Fenster selbst zeigt und dabei
        # auch das Eingabe-Routing wieder herstellt. Der zweite Prozess beendet sich selbst.
        if (-not $Background) {
            $zweit = Starte-Exe
            Notiere ("Zweitstart als Fenster-Signal: PID " + $zweit.Id)
            Start-Sleep -Seconds 6

            # Sicherung: Der Zweitprozess MUSS sich selbst beenden. Tut er es nicht,
            # laufen zwei erhoehte Instanzen auf demselben Profil - das waere echter
            # Schaden, also hart beenden.
            $zweit.Refresh()
            if (-not $zweit.HasExited) {
                Notiere ("Zweitprozess " + $zweit.Id + " lebt noch - wird beendet.")
                try { $zweit.Kill() } catch { }
                Start-Sleep -Seconds 2
            }

            $fenster = [CodexAdminCheck]::FindeFenster([int]$laufend[0].ProcessId)
            $sichtbar = ($fenster -ne [IntPtr]::Zero) -and
                        [CodexAdminCheck]::IsWindowVisible($fenster) -and
                        -not [CodexAdminCheck]::IsIconic($fenster)
            Notiere ("Fenster nach Zweitstart: hwnd=" + $fenster + " sichtbar=" + $sichtbar)

            if (-not $sichtbar) {
                Zeige-Meldung ("Codex laeuft bereits mit Administratorrechten, das Fenster kam aber nicht " +
                               "nach vorne.`n`nBitte das Codex-Symbol im Benachrichtigungsfeld (unten rechts " +
                               "neben der Uhr) anklicken.") 'Codex - Administratorstart'
            }
        }
        Schreibe-Status $true 'Codex laeuft bereits mit Administratorrechten.' $laufend[0].ProcessId
        exit 0
    }

    if ($unerhoeht.Count) {
        $text = "Codex laeuft gerade OHNE Administratorrechte.`n`n" +
                "Soll Codex jetzt beendet und mit Administratorrechten neu gestartet werden?`n" +
                "(Nicht gespeicherte Eingaben im Codex-Fenster gehen dabei verloren.)"
        if (-not (Frage-Nutzer $text 'Codex - Administratorstart')) {
            Schreibe-Status $false 'Neustart vom Benutzer abgelehnt.' $null
            exit 0
        }
        # Alle Codex-Prozesse beenden, auch die Renderer-Kindprozesse.
        Get-Process -Name ChatGPT -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
        $frist = (Get-Date).AddSeconds(15)
        while (@(Hole-Hauptprozesse).Count -and (Get-Date) -lt $frist) { Start-Sleep -Milliseconds 300 }
        if (@(Hole-Hauptprozesse).Count) { throw 'Codex liess sich nicht beenden. Bitte manuell schliessen und erneut versuchen.' }
        Start-Sleep -Seconds 2
    }

    # --- Schritt 4: erhoeht starten ---------------------------------------------
    if ($Background) { $env:CODEX_ELECTRON_START_IN_BACKGROUND = '1' }
    else { Remove-Item Env:\CODEX_ELECTRON_START_IN_BACKGROUND -ErrorAction SilentlyContinue }

    $app = Starte-Exe
    Notiere ("Neu gestartet: PID " + $app.Id + " aus " + $exe)

    Start-Sleep -Seconds 5
    $app.Refresh()
    if ($app.HasExited) {
        throw 'Der erhoehte Codex-Prozess wurde sofort beendet. Bitte pruefen, ob eine andere Codex-Instanz laeuft.'
    }
    if (-not [CodexAdminCheck]::Elevated($app.Id)) {
        throw 'Codex laeuft, aber ohne Administratorrechte.'
    }

    # --- Schritt 5: im Autostart ins Tray schicken ------------------------------
    # Nur wenn die App ueberhaupt ein sichtbares Fenster aufgemacht hat. WM_CLOSE laesst
    # Codex selbst ins Benachrichtigungsfeld gehen - die App bleibt resident und voll
    # bedienbar. SW_HIDE waere hier falsch: Electron wuesste davon nichts, das Fenster
    # waere spaeter zwar sichtbar, aber tot.
    if ($Background) {
        $frist = (Get-Date).AddSeconds(45)
        $fenster = [IntPtr]::Zero
        do {
            $app.Refresh()
            if ($app.HasExited) { throw 'Codex wurde waehrend des Starts beendet.' }
            $kandidat = [CodexAdminCheck]::FindeFenster($app.Id)
            if ($kandidat -ne [IntPtr]::Zero -and [CodexAdminCheck]::IsWindowVisible($kandidat)) {
                $fenster = $kandidat
                break
            }
            Start-Sleep -Milliseconds 500
        } while ((Get-Date) -lt $frist)

        if ($fenster -eq [IntPtr]::Zero) {
            Notiere 'Kein sichtbares Fenster - Codex ist von selbst im Hintergrund geblieben.'
        } else {
            Start-Sleep -Seconds 3   # dem Tray-Symbol Zeit geben, sich zu registrieren
            [void][CodexAdminCheck]::PostMessage($fenster, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)  # WM_CLOSE
            Start-Sleep -Seconds 5
            $app.Refresh()
            if ($app.HasExited) {
                # Kuenftige Codex-Version beendet sich bei WM_CLOSE wirklich: dann lieber
                # sichtbar laufen lassen als gar nicht.
                Notiere 'WM_CLOSE hat die App beendet - starte erneut und lasse sie sichtbar.'
                Remove-Item Env:\CODEX_ELECTRON_START_IN_BACKGROUND -ErrorAction SilentlyContinue
                $app = Starte-Exe
                Start-Sleep -Seconds 5
            } else {
                Notiere 'Codex liegt im Benachrichtigungsfeld.'
            }
        }
    }

    Schreibe-Status $true 'Codex laeuft mit Administratorrechten.' $app.Id
    exit 0

} catch {
    Schreibe-Status $false $_.Exception.Message $null
    Zeige-Meldung $_.Exception.Message 'Codex - Administratorstart'
    exit 1
}

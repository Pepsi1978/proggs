#requires -Version 5.1
<#
    Startet Codex Desktop (MSIX-Paket OpenAI.Codex) dauerhaft mit Administratorrechten.

    Warum das vorher nicht ging
    ---------------------------
    Codex Desktop ist eine Electron/Chromium-App. Chromium beendet sich unter Windows
    selbst, wenn es erhoeht gestartet wurde, und startet sich sofort unerhoeht neu
    (de-elevation). Der Launcher sah davon nur den sterbenden Prozess und meldete
    "Der erhoehte Codex-Prozess wurde beendet".

    Chromium kennt dafuer genau einen Schalter: --do-not-de-elevate. Damit bleibt der
    erhoehte Prozess bestehen. Der Schalter muss beim Start mitgegeben werden.

    Zusaetzlich gilt: Das MSIX-Manifest deklariert kein allowElevation, deshalb kann die
    App NICHT ueber die normale Paket-Aktivierung (shell:AppsFolder) erhoeht werden.
    Der Start laeuft daher direkt ueber app\ChatGPT.exe im Paketordner.

    Aufrufe
    -------
      .\Start-CodexAdmin.ps1               Fenster sichtbar starten (Desktop-Verknuepfung)
      .\Start-CodexAdmin.ps1 -Background   Beim Anmelden ins Tray starten (Autostart-Aufgabe)
#>
# Version 1.0.1 - 20.09.2026, 11:50 Uhr

param([switch]$Background)

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
 [DllImport("user32.dll")] public static extern bool ShowWindowAsync(IntPtr h,int n);
 [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
 [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
 [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
 [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc f, IntPtr l);
 [DllImport("user32.dll")] static extern int GetWindowThreadProcessId(IntPtr h, out int pid);
 [DllImport("user32.dll", EntryPoint="GetWindowLongPtrW")] static extern IntPtr GetWindowLongPtr(IntPtr h, int i);
 [DllImport("user32.dll")] static extern IntPtr GetWindow(IntPtr h, uint c);
 delegate bool EnumProc(IntPtr h, IntPtr l);

 // Auch VERSTECKTE Fenster finden. Process.MainWindowHandle liefert nur sichtbare
 // Fenster und ist daher nutzlos, sobald Codex im Tray liegt.
 public static IntPtr FindeFenster(int pid) {
   IntPtr treffer = IntPtr.Zero;
   EnumWindows(delegate(IntPtr h, IntPtr l) {
     int p; GetWindowThreadProcessId(h, out p);
     if (p != pid) return true;
     if (GetWindow(h, 4) != IntPtr.Zero) return true;              // kein Eigentuemer-Fenster
     long ex = (long)GetWindowLongPtr(h, -20);
     if ((ex & 0x80) != 0) return true;                            // kein WS_EX_TOOLWINDOW (Overlay)
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

    # --- Schritt 3: laufende Instanzen pruefen ----------------------------------
    # Electron laesst nur eine Instanz zu. Eine bereits laufende unerhoehte Instanz
    # blockiert jeden erhoehten Start - sie muss vorher weg.
    # @() beim Aufruf ist Pflicht: PowerShell entrollt Arrays beim Rueckgabewert, und ein
    # einzelnes CimInstance-Objekt hat keine .Count-Eigenschaft - die Abfrage liefe ins Leere.
    $laufend = @(Hole-Hauptprozesse)
    $unerhoeht = @($laufend | Where-Object { -not [CodexAdminCheck]::Elevated([int]$_.ProcessId) })
    Notiere ("Hauptprozesse: " + $laufend.Count + " (davon unerhoeht: " + $unerhoeht.Count + ") - PIDs: " +
             (($laufend | ForEach-Object { $_.ProcessId }) -join ', '))

    if ($laufend.Count -and -not $unerhoeht.Count) {
        # Laeuft schon erhoeht. Beim manuellen Start soll das Fenster nach vorne kommen -
        # sonst passiert beim Klick auf die Verknuepfung scheinbar gar nichts, solange
        # Codex im Tray liegt.
        if (-not $Background) {
            $gezeigt = $false
            foreach ($prozess in $laufend) {
                $fenster = [CodexAdminCheck]::FindeFenster([int]$prozess.ProcessId)
                if ($fenster -eq [IntPtr]::Zero) { continue }
                if ([CodexAdminCheck]::IsIconic($fenster)) {
                    [void][CodexAdminCheck]::ShowWindowAsync($fenster, 9)    # SW_RESTORE
                } else {
                    [void][CodexAdminCheck]::ShowWindowAsync($fenster, 5)    # SW_SHOW
                }
                Start-Sleep -Milliseconds 400
                [void][CodexAdminCheck]::SetForegroundWindow($fenster)
                # Erfolg nur zaehlen, wenn das Fenster danach wirklich sichtbar und nicht
                # minimiert ist - ein "true" der API allein sagt darueber nichts aus.
                if ([CodexAdminCheck]::IsWindowVisible($fenster) -and -not [CodexAdminCheck]::IsIconic($fenster)) {
                    $gezeigt = $true
                    break
                }
            }
            if (-not $gezeigt) {
                Zeige-Meldung ("Codex laeuft bereits mit Administratorrechten, das Fenster liess sich aber " +
                               "nicht nach vorne holen.`n`nBitte das Codex-Symbol im Benachrichtigungsfeld " +
                               "(unten rechts neben der Uhr) anklicken.") 'Codex - Administratorstart'
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

    # UseShellExecute=false: CreateProcess erbt das erhoehte Token. Ueber ShellExecute
    # wuerde Windows die App als Paket aktivieren und dabei auf normale Rechte zurueckfallen.
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $exe
    $info.Arguments = $KeinDeElevate
    $info.UseShellExecute = $false
    $info.WorkingDirectory = $env:USERPROFILE
    $app = [Diagnostics.Process]::Start($info)
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
    if ($Background) {
        $frist = (Get-Date).AddSeconds(120)
        $fenster = [IntPtr]::Zero
        do {
            $app.Refresh()
            if ($app.HasExited) { throw 'Codex wurde waehrend des Starts beendet.' }
            $fenster = $app.MainWindowHandle
            if ($fenster -ne [IntPtr]::Zero) { break }
            Start-Sleep -Milliseconds 500
        } while ((Get-Date) -lt $frist)

        if ($fenster -ne [IntPtr]::Zero) {
            # Mehrfach verstecken: Electron zeigt das Fenster beim Start teils erneut an.
            1..6 | ForEach-Object {
                [void][CodexAdminCheck]::ShowWindowAsync($fenster, 0)   # SW_HIDE
                Start-Sleep -Milliseconds 500
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

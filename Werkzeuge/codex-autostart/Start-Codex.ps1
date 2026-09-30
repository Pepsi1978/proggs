#requires -Version 5.1
<#
    Startet Codex Desktop (MSIX-Paket OpenAI.Codex) ganz normal, OHNE Administratorrechte.

    Warum ueber die Paket-Aktivierung
    ---------------------------------
    Codex wird ausschliesslich ueber shell:AppsFolder\<PackageFamilyName>!App gestartet.
    Nur so laeuft die App mit Paket-Identitaet. Wird app\ChatGPT.exe direkt gestartet
    (frueher: Start-CodexAdmin.ps1, erhoeht), fehlt die Identitaet und der eingebaute
    Updater bricht ab mit:
      "Updater initialization skipped: missing current Windows Package Family"

    Grundregel fuer alles, was mit dem Fenster zu tun hat
    ----------------------------------------------------
    NIEMALS ShowWindow/SetForegroundWindow auf das Electron-Fenster anwenden (Fenster
    waere sichtbar, aber tot). Stattdessen:
      - Fenster zeigen  -> Paket erneut aktivieren. Electrons Single-Instance-Sperre
                           meldet das der laufenden Instanz, die ihr Fenster selbst zeigt.
      - Fenster ins Tray -> WM_CLOSE posten. Codex bleibt mit Tray-Symbol resident.

    Aufrufe
    -------
      .\Start-Codex.ps1               Fenster sichtbar starten (Desktop-Verknuepfung)
      .\Start-Codex.ps1 -Background   Beim Anmelden ins Tray starten (Autostart-Aufgabe)
#>
# Version 2.0.0 - 30.09.2026, 18:00 Uhr

param(
    [switch]$Background,
    # Beendet eine laufende Instanz ohne Rueckfrage und startet sichtbar neu.
    [switch]$Neustart
)

$ErrorActionPreference = 'Stop'

$StatusPfad = Join-Path $env:LOCALAPPDATA 'CodexAutostart\last-launch.json'

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
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class CodexStartCheck {
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

 // Echtes Hauptfenster suchen, auch wenn es versteckt ist: keine Overlays
 // (WS_EX_TOOLWINDOW), keine Eigentuemer-Fenster, nur das betitelte Chrome_WidgetWin_1.
 public static IntPtr FindeFenster(int pid) {
   IntPtr treffer = IntPtr.Zero;
   EnumWindows(delegate(IntPtr h, IntPtr l) {
     int p; GetWindowThreadProcessId(h, out p);
     if (p != pid) return true;
     if (GetWindow(h, 4) != IntPtr.Zero) return true;
     long ex = (long)GetWindowLongPtr(h, -20);
     if ((ex & 0x80) != 0) return true;
     var k = new System.Text.StringBuilder(64);
     GetClassNameW(h, k, 64);
     if (k.ToString() != "Chrome_WidgetWin_1") return true;
     if (GetWindowTextLengthW(h) == 0) return true;
     treffer = h;
     return false;
   }, IntPtr.Zero);
   return treffer;
 }

 // Laeuft der Prozess erhoeht? Ein Token, das ein normaler Prozess nicht lesen darf,
 // gehoert zu einem erhoehten Prozess und zaehlt deshalb als erhoeht.
 public static bool Elevated(int pid) {
   IntPtr p=OpenProcess(0x1000,false,pid), t=IntPtr.Zero;
   if(p==IntPtr.Zero) return true;
   try{
     if(!OpenProcessToken(p,8,out t)) return true;
     int v,l;
     if(!GetTokenInformation(t,20,out v,4,out l)) return true;
     return v==1;
   } finally { if(t!=IntPtr.Zero) CloseHandle(t); CloseHandle(p); }
 }
}
'@

    $paket = Get-AppxPackage -Name OpenAI.Codex | Select-Object -First 1
    if (-not $paket) { throw 'Codex Desktop ist nicht installiert.' }
    $appId = 'shell:AppsFolder\' + $paket.PackageFamilyName + '!App'

    # Hauptprozess = der einzige ChatGPT.exe, dessen Elternprozess kein ChatGPT.exe ist.
    function Hole-Hauptprozesse {
        $alle = @(Get-CimInstance Win32_Process -Filter "Name='ChatGPT.exe'" |
                  Where-Object { $_.ExecutablePath -notmatch 'OpenAI\.ChatGPT-Desktop' })
        $ids  = @($alle | ForEach-Object { [int]$_.ProcessId })
        @($alle | Where-Object { $ids -notcontains [int]$_.ParentProcessId })
    }

    function Beende-Codex {
        Get-CimInstance Win32_Process -Filter "Name='ChatGPT.exe'" |
            Where-Object { $_.ExecutablePath -notmatch 'OpenAI\.ChatGPT-Desktop' } |
            ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        $frist = (Get-Date).AddSeconds(15)
        while (@(Hole-Hauptprozesse).Count -and (Get-Date) -lt $frist) { Start-Sleep -Milliseconds 300 }
        if (@(Hole-Hauptprozesse).Count) {
            throw 'Codex liess sich nicht beenden. Laeuft es noch mit Adminrechten, bitte ueber das Tray-Symbol beenden und erneut starten.'
        }
        Start-Sleep -Seconds 2
    }

    # Normaler Paketstart - mit Paket-Identitaet, ohne Adminrechte.
    function Starte-Paket { Start-Process -FilePath $appId }

    # --- laufende Instanzen pruefen ---------------------------------------------
    $laufend = @(Hole-Hauptprozesse)
    $erhoeht = @($laufend | Where-Object { [CodexStartCheck]::Elevated([int]$_.ProcessId) })
    Notiere ("Hauptprozesse: " + $laufend.Count + " (davon erhoeht: " + $erhoeht.Count + ")")

    if ($Neustart -and $laufend.Count) {
        Notiere 'Neustart angefordert.'
        Beende-Codex
        $laufend = @(); $erhoeht = @()
    }

    # Eine erhoehte Altinstanz (frueherer Admin-Launcher) blockiert per Single-Instance-Sperre
    # jeden normalen Start und hat keinen funktionierenden Updater.
    if ($erhoeht.Count) {
        $text = "Codex laeuft gerade MIT Administratorrechten.`n`n" +
                "Soll Codex jetzt beendet und normal neu gestartet werden?`n" +
                "(Nicht gespeicherte Eingaben im Codex-Fenster gehen dabei verloren.)"
        if (-not (Frage-Nutzer $text 'Codex')) {
            Schreibe-Status $false 'Erhoehte Instanz laeuft, Neustart abgelehnt oder Hintergrundstart.' $null
            exit 0
        }
        Beende-Codex
        $laufend = @()
    }

    # Update eingespielt, aber die alte Fassung laeuft noch?
    $veraltet = @($laufend | Where-Object { $_.ExecutablePath -and ($_.ExecutablePath -notlike ($paket.InstallLocation + '*')) })
    if ($veraltet.Count -and -not $Background) {
        $text = "Codex wurde aktualisiert, es laeuft aber noch die alte Fassung.`n`n" +
                "Soll Codex jetzt neu gestartet werden, damit das Update wirksam wird?`n" +
                "(Nicht gespeicherte Eingaben im Codex-Fenster gehen dabei verloren.)"
        if (Frage-Nutzer $text 'Codex') { Beende-Codex; $laufend = @() }
    }

    if ($laufend.Count) {
        # Laeuft schon: beim manuellen Start das Fenster per erneuter Aktivierung holen.
        if (-not $Background) { Starte-Paket; Notiere 'Laufende Instanz per Aktivierung nach vorne geholt.' }
        Schreibe-Status $true 'Codex laeuft bereits.' $laufend[0].ProcessId
        exit 0
    }

    # --- normal starten -----------------------------------------------------------
    Starte-Paket
    Notiere ("Gestartet ueber " + $appId)

    # Hauptprozess abwarten.
    $frist = (Get-Date).AddSeconds(30)
    do { Start-Sleep -Milliseconds 500; $haupt = @(Hole-Hauptprozesse) } while (-not $haupt.Count -and (Get-Date) -lt $frist)
    if (-not $haupt.Count) { throw 'Codex ist nicht gestartet.' }
    $appPid = [int]$haupt[0].ProcessId

    # --- im Autostart ins Tray schicken ------------------------------------------
    if ($Background) {
        $frist = (Get-Date).AddSeconds(45)
        $fenster = [IntPtr]::Zero
        do {
            $kandidat = [CodexStartCheck]::FindeFenster($appPid)
            if ($kandidat -ne [IntPtr]::Zero -and [CodexStartCheck]::IsWindowVisible($kandidat)) { $fenster = $kandidat; break }
            Start-Sleep -Milliseconds 500
        } while ((Get-Date) -lt $frist)

        if ($fenster -eq [IntPtr]::Zero) {
            Notiere 'Kein sichtbares Fenster - Codex ist von selbst im Hintergrund geblieben.'
        } else {
            Start-Sleep -Seconds 3   # dem Tray-Symbol Zeit geben, sich zu registrieren
            [void][CodexStartCheck]::PostMessage($fenster, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)  # WM_CLOSE
            Start-Sleep -Seconds 5
            if (-not @(Hole-Hauptprozesse).Count) {
                Notiere 'WM_CLOSE hat die App beendet - starte erneut und lasse sie sichtbar.'
                Starte-Paket
            } else {
                Notiere 'Codex liegt im Benachrichtigungsfeld.'
            }
        }
    }

    Schreibe-Status $true 'Codex laeuft (normale Rechte).' $appPid
    exit 0

} catch {
    Schreibe-Status $false $_.Exception.Message $null
    Zeige-Meldung $_.Exception.Message 'Codex'
    exit 1
}

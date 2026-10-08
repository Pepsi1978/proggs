# blinkschutz.ps1
# Waechter gegen das Symbol-Blinken im Explorer und auf dem Desktop (SHCNE_ASSOCCHANGED-Sturm).
# Hintergrund und Diagnose: bugs/desktop/windows-explorer-blinken-assocchanged.md
#
# Ursache des Blinkens: Ein Programm (bisher immer ein msedgewebview2.exe, z. B. von Lively Wallpaper)
# versucht alle 500 ms, eine ungueltige Standard-App-Zuordnung (z. B. .pdf) zurueckzusetzen. Der
# Schreibzugriff scheitert am UCPD-Schutz, die Aenderungsmeldung geht trotzdem raus, der Explorer laedt
# alle Symbole neu. Windows protokolliert jeden Versuch als Ereignis 62441 im Protokoll
# Microsoft-Windows-Shell-Core/AppDefaults - samt PID des Absenders.
#
# Was dieses Skript tut (Aufgabe "Explorer-Blinkschutz", siehe einrichten.ps1):
#   1. Sturm erkennen: mindestens $Schwelle Ereignisse 62441 in den letzten 10 Sekunden.
#   2. Absender aufloesen (bei msedgewebview2.exe das Wirtsprogramm hinter --webview-exe-name=).
#   3. Wirtsprogramm beenden -> Blinken hoert auf. Geschuetzte Prozesse werden nie beendet.
#   4. Merken, was beendet wurde, die Standard-Apps-Seite oeffnen und Frank sagen, was zu tun ist.
#   5. Sobald die Zuordnung repariert ist (UserChoice geaendert), das Wirtsprogramm wieder starten.
# Die Datei bleibt bewusst reines ASCII (laeuft so auch unter Windows PowerShell 5.1 ohne BOM).
param([int]$Schwelle = 6, [switch]$OhneMeldung)

$ErrorActionPreference = 'SilentlyContinue'
$dir = Join-Path $env:LOCALAPPDATA 'explorer-blinkschutz'
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$log = Join-Path $dir 'blinkschutz.log'
$stateFile = Join-Path $dir 'gestoppt.json'
$stampFile = Join-Path $dir 'letzter-eingriff.txt'
$logName = 'Microsoft-Windows-Shell-Core/AppDefaults'
# Nie beenden: ohne diese Prozesse ist der Rechner nicht bedienbar oder Frank verliert offene Arbeit.
$geschuetzt = 'explorer', 'SystemSettings', 'msedge', 'chrome', 'firefox', 'svchost', 'dwm', 'sihost',
              'ShellExperienceHost', 'StartMenuExperienceHost', 'SearchHost', 'RuntimeBroker', 'winlogon',
              'csrss', 'lsass', 'services', 'System', 'Idle', 'pwsh', 'powershell', 'wscript', 'WindowsTerminal'

function Log($m) {
    try {
        if ((Test-Path $log) -and (Get-Item $log).Length -gt 512KB) { Move-Item $log "$log.alt" -Force }
        "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $m" | Out-File -FilePath $log -Append -Encoding utf8
    } catch {}
}

# Nur eine Instanz gleichzeitig - der Ereignis-Ausloeser feuert im Sturm zweimal pro Sekunde.
$mutex = New-Object System.Threading.Mutex($false, 'Local\ExplorerBlinkschutz')
if (-not $mutex.WaitOne(0)) { exit 0 }

function Resets-Seit($sek) {
    @(Get-WinEvent -FilterHashtable @{ LogName = $logName; Id = 62441; StartTime = (Get-Date).AddSeconds(-$sek) } -ErrorAction SilentlyContinue)
}

# Fingerabdruck der Zuordnung: aendert er sich, hat jemand die Standard-App neu gesetzt.
function Zuordnung($ext) {
    $b = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\FileExts\$ext"
    $u = Get-ItemProperty "$b\UserChoice"
    $l = Get-ItemProperty "$b\UserChoiceLatest"
    $lp = Get-ItemProperty "$b\UserChoiceLatest\ProgId"
    "$($u.ProgId)|$($u.Hash)|$($lp.ProgId)|$($l.Hash)"
}

function Melden($titel, $text) {
    if ($OhneMeldung) { return }
    try {
        Add-Type -AssemblyName System.Windows.Forms
        $f = New-Object System.Windows.Forms.Form -Property @{ TopMost = $true }
        [System.Windows.Forms.MessageBox]::Show($f, $text, $titel, 'OK', 'Warning') | Out-Null
        $f.Dispose()
    } catch { Log "Meldung FEHLER: $($_.Exception.Message)" }
}

try {
    $ue = [char]0xFC; $ae = [char]0xE4; $oe = [char]0xF6
    $resets = Resets-Seit 10

    if ($resets.Count -lt $Schwelle) {
        # Kein Sturm. Wurde frueher ein Programm gestoppt und ist die Zuordnung seitdem neu gesetzt
        # worden, dann das Programm wieder starten (Funktion erhalten). Kommt der Sturm zurueck,
        # greift der Waechter erneut und merkt sich den neuen Stand - keine Endlosschleife.
        if (Test-Path $stateFile) {
            $st = Get-Content $stateFile -Raw | ConvertFrom-Json
            if ($st -and (Zuordnung $st.ext) -ne $st.zuordnung) {
                Log "Zuordnung $($st.ext) wurde neu gesetzt -> starte $($st.name) wieder ($($st.start))"
                Remove-Item $stateFile -Force
                if ($st.start) { Start-Process explorer.exe -ArgumentList $st.start }
            }
        }
        exit 0
    }

    # Abklingzeit: nach einem Eingriff 20 s nichts tun (Ereignisse laufen kurz nach).
    if ((Test-Path $stampFile) -and ((Get-Date) - (Get-Item $stampFile).LastWriteTime).TotalSeconds -lt 20) { exit 0 }

    $absenderPid = ($resets | Group-Object ProcessId | Sort-Object Count -Descending | Select-Object -First 1).Name
    $ext = '.pdf'
    $sd = Get-WinEvent -FilterHashtable @{ LogName = $logName; Id = 62443; StartTime = (Get-Date).AddSeconds(-10) } -ErrorAction SilentlyContinue |
        Where-Object { $_.ProcessId -eq $absenderPid -and $_.Message -match 'Association=([^,\s]+)' } | Select-Object -First 1
    if ($sd -and $sd.Message -match 'Association=([^,\s]+)') { $ext = $matches[1] }

    $p = Get-CimInstance Win32_Process -Filter "ProcessId=$absenderPid"
    if (-not $p) { Log "Sturm ($($resets.Count) Resets/10 s, $ext), Absender-PID $absenderPid existiert nicht mehr"; exit 0 }
    $wirt = [IO.Path]::GetFileNameWithoutExtension($p.Name)
    if ($p.Name -eq 'msedgewebview2.exe' -and $p.CommandLine -match '--webview-exe-name=("?)([^"\s]+)\1') {
        $wirt = [IO.Path]::GetFileNameWithoutExtension($matches[2])
    }
    Log "STURM: $($resets.Count) Resets in 10 s fuer $ext, Absender PID $absenderPid ($($p.Name)), Wirtsprogramm $wirt"
    Set-Content -Path $stampFile -Value (Get-Date -Format o)

    if ($geschuetzt -contains $wirt) {
        Log "Wirtsprogramm $wirt ist geschuetzt -> nicht beendet, nur Meldung"
        Start-Process "ms-settings:defaultapps"
        Melden 'Explorer-Blinkschutz' "Die Symbole blinken, weil $wirt die Standard-App f${ue}r $ext nicht setzen kann.`n`n$wirt wird nicht automatisch beendet. Bitte in den ge${oe}ffneten Einstellungen nach $ext suchen und die Standard-App neu ausw${ae}hlen."
        exit 0
    }

    # Zu beendende Prozesse. Lively hat einen Wachhund, der es sofort neu startet: der muss zuerst weg.
    $namen = @($wirt)
    if ($wirt -like 'Lively*') { $namen = @('Lively.Watchdog', 'Lively') + @(Get-Process -Name 'Lively.*' | Select-Object -ExpandProperty Name -Unique | Where-Object { $_ -ne 'Lively.Watchdog' }) }

    # Startbefehl fuer spaeter merken: Store-Apps ueber ihre AppID, sonst ueber den Programmpfad.
    $start = $null; $anzeige = $wirt
    $erster = Get-Process -Name $namen | Where-Object { $_.Path } | Select-Object -First 1
    $app = Get-StartApps | Where-Object { $_.Name -like "*$(($wirt -split '\.')[0])*" } | Select-Object -First 1
    if ($app) { $start = "shell:AppsFolder\$($app.AppID)"; $anzeige = $app.Name }
    elseif ($erster) { $start = "`"$($erster.Path)`"" }

    foreach ($n in $namen) { Stop-Process -Name $n -Force }
    Start-Sleep -Seconds 4
    $rest = (Resets-Seit 3).Count
    if ($rest -eq 0) { Log "Blinken gestoppt: $($namen -join ', ') beendet" }
    else { Log "WARNUNG: nach dem Beenden von $($namen -join ', ') noch $rest Resets in 3 s" }

    @{ name = $anzeige; start = $start; ext = $ext; zuordnung = (Zuordnung $ext); zeit = (Get-Date -Format o) } |
        ConvertTo-Json | Set-Content -Path $stateFile -Encoding utf8

    Start-Process "ms-settings:defaultapps"
    Melden 'Explorer-Blinkschutz' "Das Symbol-Blinken wurde gestoppt: $anzeige wurde beendet.`n`nUrsache: Die Standard-App f${ue}r $ext ist ung${ue}ltig (meist nach einem Edge-Update).`n`nBitte einmal reparieren: In den ge${oe}ffneten Einstellungen oben nach $ext suchen und die Standard-App neu ausw${ae}hlen. Steht dort schon die richtige App, einmal eine andere w${ae}hlen und wieder zur${ue}ckstellen.`n`n$anzeige startet danach innerhalb von 5 Minuten von selbst wieder."
}
catch { Log "FEHLER: $($_.Exception.Message)" }
finally { try { $mutex.ReleaseMutex() } catch {}; $mutex.Dispose() }
exit 0

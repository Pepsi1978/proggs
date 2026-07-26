param(
    [Parameter(Mandatory = $true)]
    [string]$ExecutablePath,
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
$ExecutablePath = (Resolve-Path -LiteralPath $ExecutablePath).Path

$existing = @(Get-Process -Name 'OpenLauncher' -ErrorAction SilentlyContinue)
if ($existing.Count -gt 0) {
    throw "Close existing OpenLauncher processes before running this test: $($existing.Id -join ', ')"
}

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class OpenLauncherWindowTestNative
{
    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hwnd, int command);

    [DllImport("user32.dll")]
    public static extern bool IsIconic(IntPtr hwnd);

    [DllImport("user32.dll")]
    public static extern bool IsZoomed(IntPtr hwnd);

    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    public static extern bool PostMessage(IntPtr hwnd, uint message, IntPtr wParam, IntPtr lParam);

    // Die Windows-Shell schickt beim Taskleisten-Klick KEIN WM_SYSCOMMAND/SC_RESTORE, sondern ruft
    // ShowWindow/SwitchToThisWindow direkt auf. Der Test muss genau diesen Weg nachstellen, sonst
    // prueft er einen Pfad, den die Realitaet nie nimmt.
    [DllImport("user32.dll")]
    public static extern void SwitchToThisWindow(IntPtr hwnd, bool altTab);
}
'@

function Wait-Until([scriptblock]$Condition, [string]$Failure, [int]$TimeoutMs = 5000) {
    $watch = [Diagnostics.Stopwatch]::StartNew()
    while ($watch.ElapsedMilliseconds -lt $TimeoutMs) {
        if (& $Condition) { return }
        Start-Sleep -Milliseconds 50
    }
    throw $Failure
}

function Get-LiveHandle([int]$ProcessId) {
    $process = Get-Process -Id $ProcessId -ErrorAction Stop
    $process.Refresh()
    return $process.MainWindowHandle
}

function Assert-Restored([int]$ProcessId, [bool]$ExpectedMaximized, [string]$Scenario) {
    Wait-Until { -not [OpenLauncherWindowTestNative]::IsIconic((Get-LiveHandle $ProcessId)) } `
        "$Scenario did not restore the minimized window."
    Wait-Until {
        $handle = Get-LiveHandle $ProcessId
        [OpenLauncherWindowTestNative]::GetForegroundWindow() -eq $handle
    } "$Scenario did not activate the launcher HWND."

    $handle = Get-LiveHandle $ProcessId
    if ([OpenLauncherWindowTestNative]::IsZoomed($handle) -ne $ExpectedMaximized) {
        throw "$Scenario changed the pre-minimize maximized state."
    }
}

function Get-LauncherLogPath {
    Join-Path $env:APPDATA ("OpenLauncher\logs\launcher_{0:yyyyMMdd}.jsonl" -f (Get-Date))
}

function Get-LauncherLogLineCount {
    $path = Get-LauncherLogPath
    if (-not (Test-Path -LiteralPath $path)) { return 0 }
    return @(Get-Content -LiteralPath $path).Count
}

function Get-LauncherLogSince([int]$SkipLines) {
    $path = Get-LauncherLogPath
    if (-not (Test-Path -LiteralPath $path)) { return @() }
    return @(Get-Content -LiteralPath $path | Select-Object -Skip $SkipLines)
}

function Invoke-ShellStyleActivation([int]$ProcessId) {
    # Exakter Nachbau des Taskleisten-Klicks auf ein minimiertes Fenster.
    $handle = Get-LiveHandle $ProcessId
    [void][OpenLauncherWindowTestNative]::ShowWindow($handle, $SW_RESTORE)
    [OpenLauncherWindowTestNative]::SwitchToThisWindow($handle, $true)
}

function Assert-StaysMinimized([int]$ProcessId, [string]$Scenario, [int]$HoldMs = 2000) {
    # Regression gegen den Selbstkonflikt: ein verzoegerter Aktivierungs-Callback der App darf ein
    # bewusst minimiertes Fenster nicht zurueckreissen. Die Haltezeit liegt ueber allen App-Guards.
    $watch = [Diagnostics.Stopwatch]::StartNew()
    while ($watch.ElapsedMilliseconds -lt $HoldMs) {
        if (-not [OpenLauncherWindowTestNative]::IsIconic((Get-LiveHandle $ProcessId))) {
            throw "$Scenario restored the window although it was minimized on purpose."
        }
        Start-Sleep -Milliseconds 100
    }
}

$SW_MINIMIZE = 6
$SW_RESTORE = 9
$WM_SYSCOMMAND = 0x0112
$SC_RESTORE = 0xF120
$WM_CLOSE = 0x0010
$ActivationCycles = 5
$primary = $null

try {
    $primary = Start-Process -FilePath $ExecutablePath -PassThru
    try { [void]$primary.WaitForInputIdle(10000) } catch { [Console]::Error.WriteLine("WaitForInputIdle skipped: $($_.Exception.Message)") }
    Wait-Until { (Get-LiveHandle $primary.Id) -ne [IntPtr]::Zero } 'Launcher did not create a main window.' 10000

    $handle = Get-LiveHandle $primary.Id
    $wasMaximized = [OpenLauncherWindowTestNative]::IsZoomed($handle)

    [void][OpenLauncherWindowTestNative]::ShowWindow($handle, $SW_MINIMIZE)
    Wait-Until { [OpenLauncherWindowTestNative]::IsIconic((Get-LiveHandle $primary.Id)) } 'Launcher did not minimize.'
    if (-not [OpenLauncherWindowTestNative]::PostMessage($handle, $WM_SYSCOMMAND, [IntPtr]$SC_RESTORE, [IntPtr]::Zero)) {
        throw 'SC_RESTORE could not be posted.'
    }
    Assert-Restored $primary.Id $wasMaximized 'SC_RESTORE'

    # Kernszenario: der echte Taskleisten-Klick. Mehrfach wiederholt, weil der gemeldete Defekt
    # intermittierend war und sich erst nach mehreren Aktivierungszyklen zeigte (haengende
    # Reentranz-Sperre bzw. nicht geloeste AttachThreadInput-Kopplung an den Shell-Thread).
    $logBefore = Get-LauncherLogLineCount
    for ($cycle = 1; $cycle -le $ActivationCycles; $cycle++) {
        $handle = Get-LiveHandle $primary.Id
        [void][OpenLauncherWindowTestNative]::ShowWindow($handle, $SW_MINIMIZE)
        Wait-Until { [OpenLauncherWindowTestNative]::IsIconic((Get-LiveHandle $primary.Id)) } `
            "Launcher did not minimize before shell-style activation cycle $cycle."
        Invoke-ShellStyleActivation $primary.Id
        Assert-Restored $primary.Id $wasMaximized "Shell-style taskbar activation (cycle $cycle)"
    }

    # Kernassertion der Fehlerklasse. Das eigentliche Leck (AttachThreadInput-Kopplung an den
    # Shell-Thread) ist ein Race und laesst sich programmgesteuert nicht zuverlaessig ausloesen —
    # pruefbar ist aber deterministisch, ob die App die Vordergrund-Sequenz bei einem gewoehnlichen
    # Shell-Restore ueberhaupt noch anwirft. Genau das darf sie nicht: Windows aktiviert selbst.
    # Diese Assertion faengt auch ein Wiedereinbauen des Activated/StateChanged-Pushs ab, das in
    # diesem Fenster-Code bereits zweimal passiert ist.
    $shellLog = Get-LauncherLogSince $logBefore
    $selfPush = @($shellLog | Where-Object { $_ -match '"fn":"BringToTaskbarForeground"' -and $_ -match '"reason":"(activated|state-changed)"' })
    if ($selfPush.Count -gt 0) {
        throw "App forced its own foreground sequence on a plain shell restore ($($selfPush.Count) log entries). Windows must own that path; see reason=activated/state-changed."
    }

    # Gegenprobe: der Taskleisten-Button ist ein Schalter. Minimiert die Shell das aktive Fenster,
    # darf die App es nicht selbsttaetig wieder nach vorn holen.
    $handle = Get-LiveHandle $primary.Id
    [void][OpenLauncherWindowTestNative]::ShowWindow($handle, $SW_MINIMIZE)
    Wait-Until { [OpenLauncherWindowTestNative]::IsIconic((Get-LiveHandle $primary.Id)) } `
        'Launcher did not minimize for the toggle check.'
    Assert-StaysMinimized $primary.Id 'Taskbar toggle minimize'

    $handle = Get-LiveHandle $primary.Id
    [void][OpenLauncherWindowTestNative]::ShowWindow($handle, $SW_MINIMIZE)
    Wait-Until { [OpenLauncherWindowTestNative]::IsIconic((Get-LiveHandle $primary.Id)) } 'Launcher did not minimize before second-instance activation.'
    $secondary = Start-Process -FilePath $ExecutablePath -PassThru
    if (-not $secondary.WaitForExit(10000)) {
        $secondary.Kill($true)
        throw 'Secondary launcher instance did not exit.'
    }
    $logBeforeSecondInstance = $logBefore
    Assert-Restored $primary.Id $wasMaximized 'Second-instance activation'

    # Funktionserhaltung: der legitime Pfad muss weiterhin laufen. Ohne diese Gegenprobe koennte man
    # die Assertion oben trivial erfuellen, indem man die Vordergrundaktivierung ganz ausbaut.
    $externalLog = @(Get-LauncherLogSince $logBeforeSecondInstance |
        Where-Object { $_ -match '"fn":"BringToTaskbarForeground"' -and $_ -match '"reason":"external-activation"' })
    if ($externalLog.Count -eq 0) {
        throw 'Second-instance activation did not run the foreground sequence (no reason=external-activation log entry).'
    }

    [pscustomobject]@{
        Executable = $ExecutablePath
        Version = (Get-Item -LiteralPath $ExecutablePath).VersionInfo.ProductVersion
        ProcessId = $primary.Id
        WasMaximized = $wasMaximized
        RestoreScenario = 'passed'
        ShellActivationScenario = "passed ($ActivationCycles cycles)"
        ToggleMinimizeScenario = 'passed'
        SecondInstanceScenario = 'passed'
        ForegroundHandle = ('0x{0:X}' -f [OpenLauncherWindowTestNative]::GetForegroundWindow().ToInt64())
    } | Format-List
}
finally {
    if ($primary -and -not $primary.HasExited -and -not $KeepRunning) {
        $handle = Get-LiveHandle $primary.Id
        [void][OpenLauncherWindowTestNative]::PostMessage($handle, $WM_CLOSE, [IntPtr]::Zero, [IntPtr]::Zero)
        if (-not $primary.WaitForExit(5000)) {
            $primary.Kill($true)
        }
    }
}

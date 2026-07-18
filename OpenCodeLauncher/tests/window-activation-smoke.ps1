param(
    [Parameter(Mandatory = $true)]
    [string]$ExecutablePath,
    [switch]$KeepRunning
)

$ErrorActionPreference = 'Stop'
$ExecutablePath = (Resolve-Path -LiteralPath $ExecutablePath).Path

$existing = @(Get-Process -Name 'OpenCodeLauncher' -ErrorAction SilentlyContinue)
if ($existing.Count -gt 0) {
    throw "Close existing OpenCodeLauncher processes before running this test: $($existing.Id -join ', ')"
}

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class OpenCodeLauncherWindowTestNative
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
    Wait-Until { -not [OpenCodeLauncherWindowTestNative]::IsIconic((Get-LiveHandle $ProcessId)) } `
        "$Scenario did not restore the minimized window."
    Wait-Until {
        $handle = Get-LiveHandle $ProcessId
        [OpenCodeLauncherWindowTestNative]::GetForegroundWindow() -eq $handle
    } "$Scenario did not activate the launcher HWND."

    $handle = Get-LiveHandle $ProcessId
    if ([OpenCodeLauncherWindowTestNative]::IsZoomed($handle) -ne $ExpectedMaximized) {
        throw "$Scenario changed the pre-minimize maximized state."
    }
}

$SW_MINIMIZE = 6
$WM_SYSCOMMAND = 0x0112
$SC_RESTORE = 0xF120
$WM_CLOSE = 0x0010
$primary = $null

try {
    $primary = Start-Process -FilePath $ExecutablePath -PassThru
    try { [void]$primary.WaitForInputIdle(10000) } catch { [Console]::Error.WriteLine("WaitForInputIdle skipped: $($_.Exception.Message)") }
    Wait-Until { (Get-LiveHandle $primary.Id) -ne [IntPtr]::Zero } 'Launcher did not create a main window.' 10000

    $handle = Get-LiveHandle $primary.Id
    $wasMaximized = [OpenCodeLauncherWindowTestNative]::IsZoomed($handle)

    [void][OpenCodeLauncherWindowTestNative]::ShowWindow($handle, $SW_MINIMIZE)
    Wait-Until { [OpenCodeLauncherWindowTestNative]::IsIconic((Get-LiveHandle $primary.Id)) } 'Launcher did not minimize.'
    if (-not [OpenCodeLauncherWindowTestNative]::PostMessage($handle, $WM_SYSCOMMAND, [IntPtr]$SC_RESTORE, [IntPtr]::Zero)) {
        throw 'SC_RESTORE could not be posted.'
    }
    Assert-Restored $primary.Id $wasMaximized 'SC_RESTORE'

    $handle = Get-LiveHandle $primary.Id
    [void][OpenCodeLauncherWindowTestNative]::ShowWindow($handle, $SW_MINIMIZE)
    Wait-Until { [OpenCodeLauncherWindowTestNative]::IsIconic((Get-LiveHandle $primary.Id)) } 'Launcher did not minimize before second-instance activation.'
    $secondary = Start-Process -FilePath $ExecutablePath -PassThru
    if (-not $secondary.WaitForExit(10000)) {
        $secondary.Kill($true)
        throw 'Secondary launcher instance did not exit.'
    }
    Assert-Restored $primary.Id $wasMaximized 'Second-instance activation'

    [pscustomobject]@{
        Executable = $ExecutablePath
        Version = (Get-Item -LiteralPath $ExecutablePath).VersionInfo.ProductVersion
        ProcessId = $primary.Id
        WasMaximized = $wasMaximized
        RestoreScenario = 'passed'
        SecondInstanceScenario = 'passed'
        ForegroundHandle = ('0x{0:X}' -f [OpenCodeLauncherWindowTestNative]::GetForegroundWindow().ToInt64())
    } | Format-List
}
finally {
    if ($primary -and -not $primary.HasExited -and -not $KeepRunning) {
        $handle = Get-LiveHandle $primary.Id
        [void][OpenCodeLauncherWindowTestNative]::PostMessage($handle, $WM_CLOSE, [IntPtr]::Zero, [IntPtr]::Zero)
        if (-not $primary.WaitForExit(5000)) {
            $primary.Kill($true)
        }
    }
}

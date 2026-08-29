# voice-overlay-proc-info.ps1 — Prozess-Pfad und Kommandozeile zuverlaessig ermitteln
#
# Warum diese Datei?
# update.ps1 der Voice-Overlays identifiziert die zu beendende Instanz ueber ihren
# EXE-Pfad (nur Prozesse aus dem kanonischen publish-Ordner duerfen beendet werden) und
# den UI-Prozess ueber "--run" in der Kommandozeile. Beides kam bisher ausschliesslich aus
# Win32_Process (WMI).
#
# Vorfall 29.08.2026: Aus einer nicht erhoehten Konsole liefert WMI fuer genau diese
# Prozesse LEERE Werte fuer ExecutablePath UND CommandLine (auch Get-Process .Path und
# .MainWindowHandle bleiben leer). Folge: Get-TargetOverlayProcesses fand nichts, die alte
# Instanz wurde nicht beendet, die neue EXE beendete sich am Single-Instance-Lock
# ("Overlay already running, exiting") und update.ps1 rollte jedes Mal zurueck — mit der
# irrefuehrenden Meldung "Die neue EXE startete keinen UI-Prozess mit --run".
#
# Die Loesung ist NICHT, die Pfadpruefung fallen zu lassen (dann wuerden fremde Prozesse
# gleichen Namens mitbeendet). Stattdessen werden dieselben Angaben ueber die Win32-/NT-API
# geholt: QueryFullProcessImageNameW und NtQueryInformationProcess kommen mit
# PROCESS_QUERY_LIMITED_INFORMATION aus und liefern die Daten auch dort, wo WMI schweigt.
# Die Pfadpruefung im Aufrufer bleibt damit unveraendert scharf.
#
# Verwendung:
#   . (Join-Path $PSScriptRoot '..\voice-overlay-proc-info.ps1')
#   Get-OverlayProcessInfo -Name 'TerminalVoiceOverlay.exe'
#     -> Objekte mit ProcessId, ExecutablePath, CommandLine (gleiche Feldnamen wie Win32_Process)

if (-not ('VoiceOverlayProcInfo' -as [type])) {
    Add-Type -Language CSharp -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;

/// <summary>
/// Liest EXE-Pfad und Kommandozeile eines Prozesses ueber die Win32-/NT-API. Beide Aufrufe
/// kommen mit PROCESS_QUERY_LIMITED_INFORMATION aus (dafuer wurde das Recht eingefuehrt) und
/// funktionieren deshalb auch dort, wo WMI leere Felder liefert. Gibt bei jedem Problem null
/// zurueck — der Aufrufer behandelt das wie "unbekannt" und fasst den Prozess nicht an.
/// </summary>
public static class VoiceOverlayProcInfo
{
    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern IntPtr OpenProcess(int access, bool inherit, int pid);

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CloseHandle(IntPtr handle);

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool QueryFullProcessImageName(IntPtr handle, int flags, StringBuilder text, ref int size);

    [DllImport("ntdll.dll")]
    private static extern int NtQueryInformationProcess(IntPtr handle, int cls, IntPtr buffer, int length, out int returned);

    private const int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
    private const int ProcessCommandLineInformation = 60;   // ab Windows 8.1

    public static string GetPath(int pid)
    {
        IntPtr h = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
        if (h == IntPtr.Zero) return null;
        try
        {
            int capacity = 1024;
            var sb = new StringBuilder(capacity);
            return QueryFullProcessImageName(h, 0, sb, ref capacity) ? sb.ToString() : null;
        }
        catch { return null; }
        finally { CloseHandle(h); }
    }

    public static string GetCommandLine(int pid)
    {
        IntPtr h = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
        if (h == IntPtr.Zero) return null;
        IntPtr buffer = IntPtr.Zero;
        try
        {
            int needed;
            NtQueryInformationProcess(h, ProcessCommandLineInformation, IntPtr.Zero, 0, out needed);
            if (needed <= 0) return null;

            buffer = Marshal.AllocHGlobal(needed);
            if (NtQueryInformationProcess(h, ProcessCommandLineInformation, buffer, needed, out needed) != 0)
                return null;

            // UNICODE_STRING { ushort Length; ushort MaximumLength; IntPtr Buffer; }
            ushort byteLength = (ushort)Marshal.ReadInt16(buffer, 0);
            IntPtr text = Marshal.ReadIntPtr(buffer, IntPtr.Size);
            if (text == IntPtr.Zero || byteLength == 0) return string.Empty;
            return Marshal.PtrToStringUni(text, byteLength / 2);
        }
        catch { return null; }
        finally
        {
            if (buffer != IntPtr.Zero) Marshal.FreeHGlobal(buffer);
            CloseHandle(h);
        }
    }
}
'@
}

<#
.SYNOPSIS
Liefert zu allen laufenden Prozessen mit den angegebenen Namen ProcessId, ExecutablePath und
CommandLine — WMI zuerst, Win32-/NT-API als Rueckfallebene fuer leere Felder.

.PARAMETER Name
Ein oder mehrere Prozessnamen, mit oder ohne ".exe" (z.B. 'TerminalVoiceOverlay.exe').

.OUTPUTS
PSCustomObject mit ProcessId, ExecutablePath, CommandLine. Feldnamen absichtlich identisch zu
Win32_Process, damit vorhandene Filter unveraendert weiterverwendet werden koennen. Felder, die
sich auch ueber die API nicht ermitteln lassen, bleiben leer — der Aufrufer darf einen solchen
Prozess dann NICHT als Treffer werten.
#>
function Get-OverlayProcessInfo {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string[]] $Name
    )

    $bare = @($Name | ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_) } | Where-Object { $_ })
    if ($bare.Count -eq 0) { return @() }

    $processes = @(Get-Process -Name $bare -ErrorAction SilentlyContinue)
    if ($processes.Count -eq 0) { return @() }

    # WMI zuerst versuchen — wenn es Werte liefert, sind sie ohne P/Invoke zu haben.
    $wmi = @{}
    try {
        $filter = ($bare | ForEach-Object { "Name = '$_.exe'" }) -join ' OR '
        foreach ($item in Get-CimInstance Win32_Process -Filter $filter -ErrorAction Stop) {
            $wmi[[int]$item.ProcessId] = $item
        }
    } catch {
        # WMI nicht verfuegbar -> die API uebernimmt vollstaendig.
    }

    foreach ($process in $processes) {
        $path = $null
        $commandLine = $null
        if ($wmi.ContainsKey($process.Id)) {
            $path = $wmi[$process.Id].ExecutablePath
            $commandLine = $wmi[$process.Id].CommandLine
        }
        if ([string]::IsNullOrWhiteSpace($path))        { $path = [VoiceOverlayProcInfo]::GetPath($process.Id) }
        if ([string]::IsNullOrWhiteSpace($commandLine)) { $commandLine = [VoiceOverlayProcInfo]::GetCommandLine($process.Id) }

        [PSCustomObject]@{
            ProcessId      = $process.Id
            ExecutablePath = $path
            CommandLine    = $commandLine
        }
    }
}

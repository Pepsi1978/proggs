# Gemeinsamer Starthelfer: startet ein Programm, das weiterlaufen soll, ohne dass es die
# Standard-Handles (stdin/stdout/stderr) des aufrufenden Skripts erbt.
#
# Warum: [Diagnostics.Process]::Start mit UseShellExecute=$false und Start-Process -NoNewWindow
# rufen CreateProcess mit bInheritHandles=TRUE auf. Das Kind erbt dann die Ausgabe-Pipe eines
# Agenten (Claude Code, Codex, OpenCode), der das Skript aufgerufen hat. Solange das Kind lebt,
# sieht der Agent kein Dateiende und wartet bis in sein Timeout, obwohl das Skript laengst fertig
# ist. Almanach: bugs/werkzeuge/update-skript-modaler-dialog.md (Falle 3).
#
# Nutzung:
#   . (Join-Path $PSScriptRoot '..\Werkzeuge\start-ohne-pipe\Start-OhnePipe.ps1')
#   $prozess = Start-ProzessOhnePipe -StartInfo $psi
# $psi darf UseShellExecute=$false haben (z. B. fuer eine bereinigte Umgebung), aber keine
# Umleitung von stdout/stderr -- die Ausgabe eines Langlaeufers liest niemand.

if (-not ('ProggsStartOhnePipe.Native' -as [type])) {
    Add-Type -Namespace ProggsStartOhnePipe -Name Native -MemberDefinition @'
[DllImport("kernel32.dll", SetLastError = true)] public static extern System.IntPtr GetStdHandle(int nStdHandle);
[DllImport("kernel32.dll", SetLastError = true)] public static extern bool SetHandleInformation(System.IntPtr hObject, uint dwMask, uint dwFlags);
'@
}

function Disable-StdHandleVererbung {
    # Markiert die eigenen Standard-Handles als nicht vererbbar. Gibt die Kennungen zurueck, bei
    # denen das nicht ging (leer = alles in Ordnung). Bricht nie ab.
    $fehler = @()
    foreach ($stdHandleId in @(-10, -11, -12)) {
        $stdHandle = [ProggsStartOhnePipe.Native]::GetStdHandle($stdHandleId)
        if ($stdHandle -eq [IntPtr]::Zero -or $stdHandle -eq [IntPtr]::new(-1)) { continue }
        if (-not [ProggsStartOhnePipe.Native]::SetHandleInformation($stdHandle, 1, 0)) {
            $fehler += '{0} (Win32-Fehler {1})' -f $stdHandleId, [Runtime.InteropServices.Marshal]::GetLastWin32Error()
        }
    }
    return $fehler
}

function Start-ProzessOhnePipe {
    param([Parameter(Mandatory)][Diagnostics.ProcessStartInfo]$StartInfo)

    if ($StartInfo.RedirectStandardOutput -or $StartInfo.RedirectStandardError) {
        throw 'Start-ProzessOhnePipe ist fuer Langlaeufer ohne Ausgabe-Umleitung gedacht.'
    }
    if (-not $StartInfo.UseShellExecute) {
        $fehler = @(Disable-StdHandleVererbung)
        if ($fehler.Count -gt 0) {
            # Kein Abbruch: der Start selbst funktioniert, nur der Aufrufer kann haengen bleiben.
            Write-Warning ('Handle-Vererbung nicht abschaltbar: {0} -- der Aufruf kann erst mit dem gestarteten Programm enden.' -f ($fehler -join ', '))
        }
    }
    return [Diagnostics.Process]::Start($StartInfo)
}

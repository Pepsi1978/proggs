using System.IO;

namespace UpdateZentrale.Services;

/// <summary>
/// Autostart with administrator rights.
///
/// The compatibility flag RUNASADMIN makes Windows elevate a program from every shortcut, from
/// the start menu and from a double click -- but the HKCU Run key silently skips programs that
/// would need elevation. The only supported way to still start them at logon is a scheduled task
/// with "run with highest privileges", which is exactly what this creates.
/// </summary>
public static class Aufgabenplanung
{
    /// <summary>Prefix keeps every task this app creates recognisable in the Task Scheduler.</summary>
    public const string Praefix = "UpdateZentrale - ";

    public static string AufgabenName(string id) => Praefix + id;

    public static async Task<bool> ExistiertAsync(string id, CancellationToken abbruch = default)
    {
        var lauf = await Kommandozeile.AusfuehrenAsync("schtasks.exe",
            "/Query /TN \"" + AufgabenName(id) + "\"", TimeSpan.FromSeconds(30), abbruch: abbruch);
        return lauf.ExitCode == 0;
    }

    /// <summary>
    /// Creates a logon task that starts the program elevated and without a UAC prompt.
    /// Requires the app itself to run elevated -- otherwise schtasks refuses /RL HIGHEST.
    /// </summary>
    /// <param name="befehlszeile">
    /// The exact command the autostart used before. It must be taken over verbatim: CVO for
    /// example starts through wscript.exe with its watcher script, and launching the exe directly
    /// would bypass the watchdog that rebuild-overlay.ps1 relies on.
    /// </param>
    public static async Task<(bool Erfolg, string Ausgabe)> AnlegenAsync(
        string id, string befehlszeile, CancellationToken abbruch = default)
    {
        if (string.IsNullOrWhiteSpace(befehlszeile)) return (false, "Kein Startbefehl bekannt.");

        // schtasks passes /TR through a second parser, so inner quotes need escaping.
        var befehl = befehlszeile.Replace("\"", "\\\"");

        var args = "/Create /F /SC ONLOGON /RL HIGHEST"
                 + " /TN \"" + AufgabenName(id) + "\""
                 + " /TR \"" + befehl + "\"";

        var lauf = await Kommandozeile.AusfuehrenAsync("schtasks.exe", args, TimeSpan.FromMinutes(1), abbruch: abbruch);
        return (lauf.ExitCode == 0, lauf.Ausgabe);
    }

    /// <summary>The id of the task that starts the hidden check at logon.</summary>
    public const string HintergrundId = "Hintergrund";

    /// <summary>
    /// Logon task for the hidden check: 45 s after logon (the network is up by then), elevated
    /// without a prompt, and -- unlike a task made by schtasks -- also on battery and without the
    /// three-day kill. Registered through PowerShell because schtasks cannot set those.
    /// </summary>
    public static async Task<(bool Erfolg, string Ausgabe)> HintergrundAnlegenAsync(string exe, CancellationToken abbruch = default)
    {
        static string Q(string text) => "'" + text.Replace("'", "''") + "'";

        var skript =
            "$ErrorActionPreference='Stop';"
            + "$ich=[Security.Principal.WindowsIdentity]::GetCurrent().Name;"
            + "$a=New-ScheduledTaskAction -Execute " + Q(exe) + " -Argument " + Q(App.HintergrundSchalter) + ";"
            + "$t=New-ScheduledTaskTrigger -AtLogOn -User $ich;$t.Delay='PT45S';"
            + "$s=New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable "
            + "-ExecutionTimeLimit (New-TimeSpan -Seconds 0);"
            + "$p=New-ScheduledTaskPrincipal -UserId $ich -LogonType Interactive -RunLevel Highest;"
            + "Register-ScheduledTask -TaskName " + Q(AufgabenName(HintergrundId))
            + " -Action $a -Trigger $t -Settings $s -Principal $p -Force | Out-Null";

        var kodiert = Convert.ToBase64String(System.Text.Encoding.Unicode.GetBytes(skript));
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe",
            "-NoProfile -NonInteractive -ExecutionPolicy Bypass -EncodedCommand " + kodiert,
            TimeSpan.FromMinutes(1), abbruch: abbruch);
        return (lauf.ExitCode == 0, lauf.Ausgabe);
    }

    public static async Task<(bool Erfolg, string Ausgabe)> EntfernenAsync(string id, CancellationToken abbruch = default)
    {
        var lauf = await Kommandozeile.AusfuehrenAsync("schtasks.exe",
            "/Delete /F /TN \"" + AufgabenName(id) + "\"", TimeSpan.FromSeconds(30), abbruch: abbruch);
        return (lauf.ExitCode == 0, lauf.Ausgabe);
    }
}

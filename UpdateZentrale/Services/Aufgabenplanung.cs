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
    public static async Task<(bool Erfolg, string Ausgabe)> AnlegenAsync(
        string id, string exe, string? argumente, CancellationToken abbruch = default)
    {
        if (!File.Exists(exe)) return (false, "Programmdatei nicht gefunden: " + exe);

        var befehl = "\\\"" + exe + "\\\"";
        if (!string.IsNullOrWhiteSpace(argumente)) befehl += " " + argumente;

        var args = "/Create /F /SC ONLOGON /RL HIGHEST"
                 + " /TN \"" + AufgabenName(id) + "\""
                 + " /TR \"" + befehl + "\"";

        var lauf = await Kommandozeile.AusfuehrenAsync("schtasks.exe", args, TimeSpan.FromMinutes(1), abbruch: abbruch);
        return (lauf.ExitCode == 0, lauf.Ausgabe);
    }

    public static async Task<(bool Erfolg, string Ausgabe)> EntfernenAsync(string id, CancellationToken abbruch = default)
    {
        var lauf = await Kommandozeile.AusfuehrenAsync("schtasks.exe",
            "/Delete /F /TN \"" + AufgabenName(id) + "\"", TimeSpan.FromSeconds(30), abbruch: abbruch);
        return (lauf.ExitCode == 0, lauf.Ausgabe);
    }
}

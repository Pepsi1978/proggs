using System.Diagnostics;
using System.IO;

namespace UpdateZentrale.Services;

/// <summary>
/// The built-in command line. Commands run through PowerShell in the repo root, with the same
/// privileges the app itself has -- so an elevated UpdateZentrale gives an elevated shell.
/// </summary>
public static class Terminal
{
    public static string Arbeitsverzeichnis { get; set; } = Pfade.RepoWurzel;

    public static async Task<string> AusfuehrenAsync(string befehl, CancellationToken abbruch = default)
    {
        if (string.IsNullOrWhiteSpace(befehl)) return "";

        var shell = PowerShellPfad();

        // -Command with the raw text keeps pipes, redirections and quoting working the way the
        // user typed them.
        var lauf = await Kommandozeile.AusfuehrenAsync(
            shell,
            "-NoProfile -NonInteractive -Command " + Zitieren(befehl),
            TimeSpan.FromMinutes(10),
            Arbeitsverzeichnis,
            abbruch);

        return Auswerten(lauf);
    }

    /// <summary>
    /// The text shown for a run. A cancel is a cancel -- not "code -1" and not a time limit; a
    /// problem while ending the process tree stays visible.
    /// </summary>
    internal static string Auswerten(BefehlErgebnis lauf)
    {
        var ausgabe = string.IsNullOrWhiteSpace(lauf.Ausgabe)
            ? "(keine Ausgabe)"
            : lauf.Ausgabe;

        if (lauf.Abgebrochen) ausgabe += "\n[Abgebrochen]";
        else if (lauf.Abgelaufen) ausgabe += "\n[Zeitlimit von 10 Minuten überschritten]";
        else if (lauf.ExitCode != 0) ausgabe += "\n[Beendet mit Code " + lauf.ExitCode + "]";

        if (!string.IsNullOrWhiteSpace(lauf.BeendenProblem)) ausgabe += "\n[Beenden unvollständig: " + lauf.BeendenProblem + "]";

        return ausgabe;
    }

    /// <summary>Opens a real terminal window, elevated when the app itself is elevated.</summary>
    public static bool FensterOeffnen(string? verzeichnis = null)
    {
        var ordner = string.IsNullOrWhiteSpace(verzeichnis) ? Arbeitsverzeichnis : verzeichnis;

        // Windows Terminal when present, otherwise plain PowerShell.
        var wt = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Microsoft", "WindowsApps", "wt.exe");

        try
        {
            if (File.Exists(wt))
            {
                using var _ = Process.Start(new ProcessStartInfo
                {
                    FileName = wt,
                    Arguments = "-d \"" + ordner + "\"",
                    UseShellExecute = true
                });
            }
            else
            {
                using var _ = Process.Start(new ProcessStartInfo
                {
                    FileName = PowerShellPfad(),
                    WorkingDirectory = ordner,
                    UseShellExecute = true
                });
            }
            return true;
        }
        catch
        {
            return false;
        }
    }

    private static string PowerShellPfad()
    {
        var kandidaten = new[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PowerShell", "7", "pwsh.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Microsoft", "WindowsApps", "pwsh.exe")
        };
        return kandidaten.FirstOrDefault(File.Exists) ?? "powershell.exe";
    }

    /// <summary>Wraps the command so PowerShell receives it as one argument.</summary>
    private static string Zitieren(string befehl) => "\"" + befehl.Replace("\"", "\\\"") + "\"";
}

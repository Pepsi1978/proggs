using System.Diagnostics;
using System.IO;
using System.Text;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Hands a failed run to a fresh Claude Code session in the terminal: Opus 5.5, profile minimal,
/// fast mode, effort high. Goes through OpenLauncher's windowless start (--jarvis-auftrag), so the
/// session is set up exactly like the launcher's start button, with the problem as first message.
/// </summary>
public static class ClaudeUebergabe
{
    private const int ProtokollZeilen = 400;

    /// <summary>Null on success, otherwise why nothing was started.</summary>
    public static string? Starten(ProgrammEintrag eintrag, UpdateBericht bericht)
    {
        var launcher = LauncherFinden();
        if (launcher is null) return "OpenLauncher.exe nicht gefunden – erst den Launcher bauen.";

        try
        {
            var ordner = Path.Combine(Pfade.BenutzerOrdner, "uebergaben");
            Directory.CreateDirectory(ordner);
            var stempel = DateTime.Now.ToString("yyyyMMdd-HHmmss");
            var datei = Path.Combine(ordner, "fehler-" + eintrag.Id + "-" + stempel + ".txt");
            File.WriteAllText(datei, AuftragText(eintrag, bericht), new UTF8Encoding(false));

            var argumente = new[]
            {
                "--jarvis-auftrag", datei,
                "--name", "Update-Zentrale: " + bericht.Name,
                "--profil", "minimal",
                "--modus", "schnell",
                "--modell", "claude-opus-5-5[1m]",
                "--effort", "high",
                "--ordner", Pfade.RepoWurzel
            };

            if (Rechte.IstErhoeht) return OhneAdminStarten(launcher, argumente, ordner, stempel);

            var start = new ProcessStartInfo(launcher) { UseShellExecute = false, WorkingDirectory = Pfade.RepoWurzel };
            foreach (var a in argumente) start.ArgumentList.Add(a);
            using var _ = Process.Start(start);
            return null;
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Warnung);
            return "Übergabe an Claude Code fehlgeschlagen: " + diagAusnahme.Message;
        }
    }

    /// <summary>
    /// An elevated UpdateZentrale would pass its admin token to the session. Explorer runs as the
    /// normal user, so a small batch file opened through it starts the launcher without elevation.
    /// </summary>
    private static string? OhneAdminStarten(string launcher, string[] argumente, string ordner, string stempel)
    {
        var cmd = Path.Combine(ordner, "start-" + stempel + ".cmd");
        var zeile = "start \"\" " + string.Join(" ", new[] { launcher }.Concat(argumente).Select(a => "\"" + a + "\""));
        File.WriteAllText(cmd, "@echo off\r\n" + zeile + "\r\n", new UTF8Encoding(false));
        using var _ = Process.Start(new ProcessStartInfo { FileName = "explorer.exe", Arguments = "\"" + cmd + "\"", UseShellExecute = true });
        return null;
    }

    private static string? LauncherFinden()
    {
        var release = Path.Combine(Pfade.RepoWurzel, "OpenLauncher", "bin", "Release");
        if (!Directory.Exists(release)) return null;
        return Directory.GetFiles(release, "OpenLauncher.exe", SearchOption.AllDirectories)
            .OrderByDescending(File.GetLastWriteTime)
            .FirstOrDefault();
    }

    private static string AuftragText(ProgrammEintrag eintrag, UpdateBericht bericht)
    {
        var sb = new StringBuilder();
        sb.AppendLine("Auftrag aus der Update-Zentrale (Windows): Ein Update ist fehlgeschlagen. Analysiere die Ursache und behebe das Problem.");
        sb.AppendLine("Das kann am Update-Weg in der Update-Zentrale liegen (C:\\Users\\barwa\\proggs\\UpdateZentrale, Katalog programs.json) oder am Programm bzw. dessen Update-Skript selbst.");
        sb.AppendLine();
        sb.AppendLine("Programm: " + bericht.Name + " (Id " + eintrag.Id + ", Art " + eintrag.Art + ")");
        sb.AppendLine("Zeitpunkt: " + bericht.Zeit.ToString("dd.MM.yyyy, HH:mm:ss"));
        sb.AppendLine("Ergebnis: " + bericht.ErgebnisText + ", Exit-Code " + bericht.ExitCode + (bericht.Erhoeht ? ", lief mit Administratorrechten" : ""));
        if (!string.IsNullOrWhiteSpace(bericht.VersionVorher) || !string.IsNullOrWhiteSpace(bericht.VersionNachher))
            sb.AppendLine("Version vorher/nachher: " + bericht.VersionVorher + " → " + bericht.VersionNachher);
        if (!string.IsNullOrWhiteSpace(bericht.Befehl)) sb.AppendLine("Befehl: " + bericht.Befehl);
        sb.AppendLine("Meldung: " + bericht.Meldung);
        sb.AppendLine("Protokolldatei: " + bericht.ProtokollDatei);
        sb.AppendLine("Weitere Protokolle (Tagesprotokolle, Diagnose-JSONL, verlauf.jsonl): " + Protokollierung.Ordner);

        if (File.Exists(bericht.ProtokollDatei))
        {
            try
            {
                var zeilen = File.ReadAllLines(bericht.ProtokollDatei);
                var rest = zeilen.Length > ProtokollZeilen ? zeilen[^ProtokollZeilen..] : zeilen;
                sb.AppendLine();
                sb.AppendLine(zeilen.Length > ProtokollZeilen
                    ? "Protokoll (letzte " + ProtokollZeilen + " von " + zeilen.Length + " Zeilen, vollständig in der Datei oben):"
                    : "Protokoll:");
                sb.AppendLine("----");
                foreach (var z in rest) sb.AppendLine(z);
                sb.AppendLine("----");
            }
            catch (Exception diagAusnahme)
            {
                Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Info);
            }
        }

        return sb.ToString();
    }
}

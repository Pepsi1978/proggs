using System.IO;
using System.Text;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;

namespace UpdateZentrale.Services;

/// <summary>
/// Hands a failed run to a fresh Claude Code session in the terminal: Opus 5.5, profile minimal,
/// fast mode, effort high. Uses the same OpenLauncher start as the OpenCode session card
/// (--jarvis-auftrag, unelevated, exit code checked), with the problem as first message.
/// </summary>
public static class ClaudeUebergabe
{
    /// <summary>
    /// The launcher passes the whole task to claude as one command-line argument, and Windows caps
    /// a command line at 32767 characters. The log tail stays well below that; the full log is
    /// named by path.
    /// </summary>
    private const int ProtokollZeichen = 14000;

    /// <summary>Null on success, otherwise why no session was started.</summary>
    public static async Task<string?> StartenAsync(ProgrammEintrag eintrag, UpdateBericht bericht)
    {
        if (!File.Exists(OpenCodeSitzungAktualisierer.LauncherExe))
            return "OpenLauncher nicht gefunden: " + OpenCodeSitzungAktualisierer.LauncherExe;

        try
        {
            var ordner = Directory.CreateDirectory(Path.Combine(Pfade.BenutzerOrdner, "auftraege")).FullName;
            var datei = Path.Combine(ordner, "fehler-" + eintrag.Id + "-" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + ".md");
            await File.WriteAllTextAsync(datei, AuftragText(eintrag, bericht), new UTF8Encoding(false));

            // The id, not the display name: the elevated path writes the arguments into a .cmd,
            // which cmd.exe reads in the OEM code page and would garble umlauts in.
            var args = OpenCodeSitzungAktualisierer.Argumente(datei, "UpdateZentrale: Fehler " + eintrag.Id);
            var code = await OpenCodeSitzungAktualisierer.LauncherStartenAsync(args, ordner, new Progress<string>(_ => { }), CancellationToken.None);
            return code switch
            {
                null => "Der OpenLauncher hat sich nach zwei Minuten nicht zurückgemeldet – ob die Sitzung läuft, ist unklar.",
                0 => null,
                _ => "Der OpenLauncher konnte die Sitzung nicht starten (Code " + code + ") – Einzelheiten im OpenLauncher-Log."
            };
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Warnung);
            return "Übergabe an Claude Code fehlgeschlagen: " + diagAusnahme.Message;
        }
    }

    private static string AuftragText(ProgrammEintrag eintrag, UpdateBericht bericht)
    {
        var sb = new StringBuilder();
        sb.AppendLine("Auftrag von der UpdateZentrale (Windows): Ein Update ist fehlgeschlagen. Analysiere die Ursache und behebe das Problem.");
        sb.AppendLine("Das kann am Update-Weg der UpdateZentrale liegen (" + Path.Combine(Pfade.RepoWurzel, "UpdateZentrale")
                      + ", Katalog programs.json) oder am Programm bzw. dessen Update-Skript selbst.");
        sb.AppendLine();
        sb.AppendLine("Programm: " + bericht.Name + " (Id " + eintrag.Id + ", Art " + eintrag.Art + ")");
        sb.AppendLine("Zeitpunkt: " + bericht.Zeit.ToString("dd.MM.yyyy, HH:mm:ss"));
        sb.AppendLine("Ergebnis: " + bericht.ErgebnisText + ", Exit-Code " + bericht.ExitCode + (bericht.Erhoeht ? ", lief mit Administratorrechten" : ""));
        if (!string.IsNullOrWhiteSpace(bericht.VersionVorher) || !string.IsNullOrWhiteSpace(bericht.VersionNachher))
            sb.AppendLine("Version vorher/nachher: " + bericht.VersionVorher + " → " + bericht.VersionNachher);
        if (!string.IsNullOrWhiteSpace(bericht.Befehl)) sb.AppendLine("Befehl: " + bericht.Befehl);
        sb.AppendLine("Meldung: " + bericht.Meldung);
        sb.AppendLine("Protokolldatei (vollständig): " + bericht.ProtokollDatei);
        sb.AppendLine("Weitere Protokolle (Tagesprotokolle, Diagnose-JSONL, verlauf.jsonl): " + Protokollierung.Ordner);

        if (File.Exists(bericht.ProtokollDatei))
        {
            try
            {
                var zeilen = Protokollierung.LiesAlleZeilen(bericht.ProtokollDatei).ToList();
                var ende = new List<string>();
                var laenge = 0;
                for (var i = zeilen.Count - 1; i >= 0 && laenge + zeilen[i].Length < ProtokollZeichen; i--)
                {
                    ende.Insert(0, zeilen[i]);
                    laenge += zeilen[i].Length + 1;
                }
                sb.AppendLine();
                sb.AppendLine(ende.Count < zeilen.Count
                    ? "Protokoll (letzte " + ende.Count + " von " + zeilen.Count + " Zeilen, den Rest in der Datei oben lesen):"
                    : "Protokoll:");
                sb.AppendLine("----");
                foreach (var z in ende) sb.AppendLine(z);
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

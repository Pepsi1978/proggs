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

    /// <summary>Automatische Uebergaben je Programm hoechstens einmal in dieser Frist (Klick geht immer).</summary>
    public static readonly TimeSpan Sperrfrist = TimeSpan.FromHours(6);

    /// <summary>Nie zwei automatische Sitzungen zugleich: sie aenderten dieselben Dateien und pushten auf main.</summary>
    private static int _automatischLaeuft;

    /// <summary>Nur echte Fehlschlaege; "nicht verifiziert" heisst oft nur "Version nicht feststellbar".</summary>
    public static bool AutomatischRelevant(UpdateBericht bericht) => bericht.Ergebnis == LaufErgebnis.Fehlgeschlagen;

    /// <summary>
    /// Startet EINE Sitzung fuer alle Fehler ausserhalb der Sperrfrist und merkt sie erst nach gelungenem
    /// Start vor. Null, wenn nichts zu tun war oder es klappte, sonst das Problem.
    /// </summary>
    public static async Task<string?> AutomatischStartenAsync(IReadOnlyList<(ProgrammEintrag Eintrag, UpdateBericht Bericht)> faelle,
                                                              Einstellungen einstellungen)
    {
        var offen = faelle.Where(f => AutomatischRelevant(f.Bericht)
                                      && (einstellungen.Fuer(f.Eintrag.Id).AutomatischUebergebenAm is not { } zuletzt
                                          || DateTime.Now - zuletzt >= Sperrfrist)).ToList();
        if (offen.Count == 0 || Interlocked.Exchange(ref _automatischLaeuft, 1) == 1)
        {
            if (faelle.Count > 0)
                Diagnose.Ereignis(Schwere.Info, "uebergabe", "auto.gesperrt", "Sperrfrist oder laufende Übergabe", null,
                                  new Dictionary<string, object?> { ["programme"] = string.Join(",", faelle.Select(f => f.Eintrag.Id)) });
            return null;
        }
        try
        {
            var problem = await StartenAsync(offen);
            if (problem is null)
            {
                foreach (var f in offen) einstellungen.Fuer(f.Eintrag.Id).AutomatischUebergebenAm = DateTime.Now;
                einstellungen.Speichern();
            }
            return problem;
        }
        finally
        {
            Volatile.Write(ref _automatischLaeuft, 0);
        }
    }

    public static Task<string?> StartenAsync(ProgrammEintrag eintrag, UpdateBericht bericht)
        => StartenAsync(new[] { (eintrag, bericht) });

    /// <summary>Null on success, otherwise why no session was started.</summary>
    public static async Task<string?> StartenAsync(IReadOnlyList<(ProgrammEintrag Eintrag, UpdateBericht Bericht)> faelle)
    {
        if (faelle.Count == 0) return null;
        if (!File.Exists(OpenCodeSitzungAktualisierer.LauncherExe))
            return "OpenLauncher nicht gefunden: " + OpenCodeSitzungAktualisierer.LauncherExe;

        try
        {
            var kennung = faelle.Count == 1 ? faelle[0].Eintrag.Id : "sammel-" + faelle.Count;
            var ordner = Directory.CreateDirectory(Path.Combine(Pfade.BenutzerOrdner, "auftraege")).FullName;
            var datei = Path.Combine(ordner, "fehler-" + kennung + "-" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + ".md");
            await File.WriteAllTextAsync(datei, AuftragText(faelle), new UTF8Encoding(false));

            // The id, not the display name: the elevated path writes the arguments into a .cmd,
            // which cmd.exe reads in the OEM code page and would garble umlauts in.
            var args = OpenCodeSitzungAktualisierer.Argumente(datei, "UpdateZentrale: Fehler " + kennung);
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

    private static string AuftragText(IReadOnlyList<(ProgrammEintrag Eintrag, UpdateBericht Bericht)> faelle)
    {
        var sb = new StringBuilder();
        sb.AppendLine("Auftrag von der UpdateZentrale (Windows): " + (faelle.Count == 1 ? "Ein Update ist" : faelle.Count + " Updates sind")
                      + " fehlgeschlagen. Analysiere die Ursache und behebe das Problem.");
        sb.AppendLine("Das kann am Update-Weg der UpdateZentrale liegen (" + Path.Combine(Pfade.RepoWurzel, "UpdateZentrale")
                      + ", Katalog programs.json) oder am Programm bzw. dessen Update-Skript selbst.");
        sb.AppendLine();
        sb.AppendLine("Gehe so vor:");
        sb.AppendLine("1. Finde die Ursache im Protokoll unten (bei mehreren Fehlern: jeden einzeln, bei Bedarf in den Protokolldateien).");
        sb.AppendLine("2. Bring das Update zu Ende. Nutze schon geladene Installer und lade große Dateien nicht unnötig erneut.");
        sb.AppendLine("3. Sorge dafür, dass dieser Fehler nicht wieder auftritt: Bau die Vorbeugung dauerhaft in die UpdateZentrale ein "
                      + "(programs.json, Update-Weg im Code oder das Update-Skript des Programms), möglichst für die ganze Fehlerart, nicht nur für diesen einen Fall.");
        sb.AppendLine("4. Halte Ursache und Vorbeugung im passenden Bug-Almanach unter proggs\\bugs fest, damit die UpdateZentrale aus jedem Fehler lernt.");
        foreach (var (eintrag, bericht) in faelle)
        {
        sb.AppendLine();
        if (faelle.Count > 1) sb.AppendLine("=== " + bericht.Name + " ===");
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
        }

        return sb.ToString();
    }
}

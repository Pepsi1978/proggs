using System.IO;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;
using System.Text.Json.Serialization;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;

namespace UpdateZentrale.Services;

/// <summary>
/// Hands a failed run or a whole pending update to a fresh Claude Code session in the terminal:
/// Opus 5.5, profile minimal, fast mode, effort high. Uses the same OpenLauncher start as the
/// OpenCode session card (--jarvis-auftrag, unelevated, exit code checked), with the task as first message.
/// </summary>
public static class ClaudeUebergabe
{
    private static readonly JsonSerializerOptions KatalogOptionen = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingDefault,
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    /// <summary>
    /// The launcher passes the whole task to claude as one command-line argument, and Windows caps
    /// a command line at 32767 characters. The log tail stays well below that; the full log is
    /// named by path.
    /// </summary>
    private const int ProtokollZeichen = 14000;

    /// <summary>
    /// Only keeps a second session from working on the same failure while the first one still runs:
    /// a program that fails again after that repair must reach a new session right away. A click always goes through.
    /// </summary>
    public static readonly TimeSpan Sperrfrist = TimeSpan.FromHours(1);

    /// <summary>What a card or the batch line says when the lock kept a failure from a new session.</summary>
    public static string SperrHinweis => "Dieser Fehler ging in den letzten " + (int)Sperrfrist.TotalMinutes
                                         + " Minuten schon an Claude Code – „An Claude Code übergeben“ startet eine neue Sitzung.";

    /// <summary>
    /// Serialises the launcher starts, so the lock is read after the previous start has marked its
    /// programs. Sessions for different programs can still run side by side; the task text therefore
    /// tells them how to share main. Who comes later waits and is then checked against the lock.
    /// </summary>
    private static readonly SemaphoreSlim AutomatischSchleuse = new(1, 1);

    /// <summary>Nur echte Fehlschlaege; "nicht verifiziert" heisst oft nur "Version nicht feststellbar".</summary>
    /// A line too slow for the tool's own download deadline is no code problem: no session for that.
    public static bool AutomatischRelevant(UpdateBericht bericht)
        => bericht.Ergebnis == LaufErgebnis.Fehlgeschlagen
           && !(bericht.Meldung ?? "").StartsWith(CliAktualisierer.LangsamesNetzMeldung, StringComparison.Ordinal);

    /// <param name="Gestartet">Eine Sitzung wurde (vermutlich) gestartet.</param>
    /// <param name="Problem">Warum nicht, oder was unklar blieb; null = alles gut oder nichts zu tun.</param>
    public sealed record AutoErgebnis(bool Gestartet, string? Problem);

    /// <summary>
    /// Startet EINE Sitzung fuer alle Fehler ausserhalb der Sperrfrist und merkt sie nach dem Start vor --
    /// auch wenn der Launcher nur nicht rechtzeitig antwortete, denn dann laeuft die Sitzung meist doch.
    /// </summary>
    public static async Task<AutoErgebnis> AutomatischStartenAsync(IReadOnlyList<(ProgrammEintrag Eintrag, UpdateBericht Bericht)> faelle,
                                                                   Einstellungen einstellungen)
    {
        if (!faelle.Any(f => AutomatischRelevant(f.Bericht))) return new AutoErgebnis(false, null);
        await AutomatischSchleuse.WaitAsync();
        try
        {
            var offen = faelle.Where(f => AutomatischRelevant(f.Bericht)
                                          && (einstellungen.Fuer(f.Eintrag.Id).AutomatischUebergebenAm is not { } zuletzt
                                              || DateTime.Now - zuletzt >= Sperrfrist)).ToList();
            if (offen.Count == 0)
            {
                Diagnose.Ereignis(Schwere.Info, "uebergabe", "auto.gesperrt", "Sperrfrist läuft", null,
                                  new Dictionary<string, object?>
                                  {
                                      ["programme"] = string.Join(",", faelle.Where(f => AutomatischRelevant(f.Bericht)).Select(f => f.Eintrag.Id))
                                  });
                return new AutoErgebnis(false, null);
            }
            // VOR dem Start vorgemerkt: endet die App waehrend des Launcher-Starts (Selbst-Update), gilt
            // die Sperrfrist trotzdem. Scheitert der Start sicher, wird die Marke zurueckgenommen.
            var bisher = offen.Select(f => einstellungen.Fuer(f.Eintrag.Id).AutomatischUebergebenAm).ToList();
            foreach (var f in offen) einstellungen.Fuer(f.Eintrag.Id).AutomatischUebergebenAm = DateTime.Now;
            einstellungen.Speichern();
            var (problem, vielleicht) = await StartenKernAsync(offen);
            // Unclear (the launcher did not answer): the session usually runs anyway, so the mark stays.
            if (problem is not null && !vielleicht)
            {
                for (var i = 0; i < offen.Count; i++)
                    einstellungen.Fuer(offen[i].Eintrag.Id).AutomatischUebergebenAm = bisher[i];
                einstellungen.Speichern();
            }
            return new AutoErgebnis(problem is null || vielleicht, problem);
        }
        finally
        {
            AutomatischSchleuse.Release();
        }
    }

    public static Task<string?> StartenAsync(ProgrammEintrag eintrag, UpdateBericht bericht)
        => StartenAsync(new[] { (eintrag, bericht) });

    /// <summary>Null on success, otherwise why no session was started.</summary>
    public static async Task<string?> StartenAsync(IReadOnlyList<(ProgrammEintrag Eintrag, UpdateBericht Bericht)> faelle)
        => (await StartenKernAsync(faelle)).Problem;

    /// <param name="Vielleicht">Der Launcher antwortete nicht rechtzeitig -- die Sitzung laeuft womoeglich doch.</param>
    private static async Task<(string? Problem, bool Vielleicht)> StartenKernAsync(IReadOnlyList<(ProgrammEintrag Eintrag, UpdateBericht Bericht)> faelle)
    {
        if (faelle.Count == 0) return (null, false);
        var kennung = faelle.Count == 1 ? faelle[0].Eintrag.Id : "sammel-" + faelle.Count;
        return await SitzungStartenAsync("fehler-" + kennung, "UpdateZentrale: Fehler " + kennung, () => AuftragText(faelle));
    }

    /// <summary>Hands a whole pending update to a new session.</summary>
    /// <param name="befehl">The way this card would run the update itself.</param>
    /// <param name="beendenZugestimmt">The program is running and the user agreed to closing it.</param>
    /// <param name="skriptZugestimmt">The script has no question of its own and the user already answered the app's.</param>
    /// <returns>Problem: why no session was started; Vielleicht: the launcher did not answer, it may run anyway.</returns>
    public static Task<(string? Problem, bool Vielleicht)> UpdateStartenAsync(ProgrammEintrag eintrag, string installiert, string verfuegbar,
                                                                              string befehl, bool beendenZugestimmt, bool skriptZugestimmt)
        => SitzungStartenAsync("update-" + eintrag.Id, "UpdateZentrale: Update " + eintrag.Id,
                               () => UpdateAuftragText(eintrag, installiert, verfuegbar, befehl, beendenZugestimmt, skriptZugestimmt));

    /// <param name="dateiName">Task file name without time stamp and extension.</param>
    /// <param name="sitzungsName">
    /// Built from the id, not the display name: the elevated path writes the arguments into a .cmd,
    /// which cmd.exe reads in the OEM code page and would garble umlauts in.
    /// </param>
    /// <param name="auftrag">The task text.</param>
    private static async Task<(string? Problem, bool Vielleicht)> SitzungStartenAsync(string dateiName, string sitzungsName, Func<string> auftrag)
    {
        if (!File.Exists(OpenCodeSitzungAktualisierer.LauncherExe))
            return ("OpenLauncher nicht gefunden: " + OpenCodeSitzungAktualisierer.LauncherExe, false);

        try
        {
            var ordner = Directory.CreateDirectory(Path.Combine(Pfade.BenutzerOrdner, "auftraege")).FullName;
            var datei = Path.Combine(ordner, dateiName + "-" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + ".md");
            await File.WriteAllTextAsync(datei, auftrag(), new UTF8Encoding(false));

            var args = OpenCodeSitzungAktualisierer.Argumente(datei, sitzungsName);
            var code = await OpenCodeSitzungAktualisierer.LauncherStartenAsync(args, ordner, new Progress<string>(_ => { }), CancellationToken.None);
            return code switch
            {
                null => ("Der OpenLauncher hat sich nach zwei Minuten nicht zurückgemeldet – ob die Sitzung läuft, ist unklar.", true),
                0 => (null, false),
                _ => ("Der OpenLauncher konnte die Sitzung nicht starten (Code " + code + ") – Einzelheiten im OpenLauncher-Log.", false)
            };
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Warnung);
            return ("Übergabe an Claude Code fehlgeschlagen: " + diagAusnahme.Message, false);
        }
    }

    private const string OhneAdminHinweis = "Diese Sitzung läuft ohne Administratorrechte. Eine Windows-Rechteabfrage bei Installern unter „Program Files“ ist deshalb normal und kein Fehler des Update-Wegs.";
    private const string BeendenHinweis = "Beende andere laufende Programme nicht ohne Rückfrage.";
    private const string RueckfrageHinweis = "Ein Nein oder keine Antwort in einem Rückfragefenster (cancelled, no-answer) ist kein Klemmen: nicht erneut starten, sondern melden.";

    private static string UpdateAuftragText(ProgrammEintrag eintrag, string installiert, string verfuegbar, string befehl,
                                            bool beendenZugestimmt, bool skriptZugestimmt)
    {
        static string Anzeige(string version) => string.IsNullOrWhiteSpace(version) ? "unbekannt" : version;

        var projekt = Path.Combine(Pfade.RepoWurzel, "UpdateZentrale");
        var sb = new StringBuilder();
        sb.AppendLine("Auftrag von der UpdateZentrale (Windows): Führe das Update von " + eintrag.Name + " vollständig aus ("
                      + Anzeige(installiert) + " → " + Anzeige(verfuegbar) + "). Die Versionen stammen von der letzten Prüfung, lies den Stand zuerst frisch.");
        sb.AppendLine();
        sb.AppendLine("Gehe so vor:");
        sb.AppendLine("1. Führe das Update über den Weg der UpdateZentrale aus: " + befehl + ". Einzelheiten stehen im Katalogeintrag unten, die Umsetzung in "
                      + Path.Combine(projekt, "Providers") + ". Eigene Werkzeuge (OpenLauncher, TVO, CVO, UpdateZentrale) nur über ihre Update-Skripte nach den Regeln der CLAUDE.md.");
        sb.AppendLine("2. Klemmt dieser Weg: Finde die Ursache und bring das Update auf einem anderen sicheren Weg zu Ende. " + RueckfrageHinweis);
        sb.AppendLine("3. Prüfe am Ende die installierte Version nach und melde das Ergebnis.");
        sb.AppendLine("4. Hat der Weg der UpdateZentrale versagt: Bau die Vorbeugung dauerhaft ein (" + projekt
                      + ", Katalog programs.json) und halte Ursache und Vorbeugung im passenden Bug-Almanach unter proggs\\bugs fest.");
        sb.AppendLine();
        sb.AppendLine(OhneAdminHinweis);
        sb.AppendLine(BeendenHinweis);
        if (eintrag.BeendenVorUpdate)
            sb.AppendLine(beendenZugestimmt
                ? eintrag.Name + " läuft und muss fürs Update beendet werden, dem habe ich beim Klick zugestimmt."
                : eintrag.Name + " muss fürs Update beendet sein. Beim Klick lief es nicht; läuft es inzwischen, frag vor dem Beenden nach.");
        if (skriptZugestimmt) sb.AppendLine("Dem Neubau über das Skript habe ich beim Klick zugestimmt.");
        if (eintrag.KannCliBeenden)
        {
            sb.AppendLine();
            sb.AppendLine("Achtung, dieses Update kann laufende CLI-Sitzungen beenden, auch diese hier: " + eintrag.CliRisiko);
            sb.AppendLine("Starte es abgekoppelt, damit es auch dann zu Ende läuft, wenn diese Sitzung endet. Verlangt es geschlossene Terminalfenster, melde das, statt es zu erzwingen. "
                          + "Endet die Sitzung vorher, bestätigt die nächste Prüfung der UpdateZentrale das Ergebnis.");
        }
        sb.AppendLine();
        sb.AppendLine("Protokolle der UpdateZentrale: " + Protokollierung.Ordner);
        sb.AppendLine();
        sb.AppendLine("Katalogeintrag:");
        sb.AppendLine("----");
        sb.AppendLine(JsonSerializer.Serialize(eintrag, KatalogOptionen));
        sb.AppendLine("----");
        return sb.ToString();
    }

    internal static string AuftragText(IReadOnlyList<(ProgrammEintrag Eintrag, UpdateBericht Bericht)> faelle)
    {
        var projekt = Path.Combine(Pfade.RepoWurzel, "UpdateZentrale");
        var sb = new StringBuilder();
        sb.AppendLine("Auftrag von der UpdateZentrale (Windows): " + (faelle.Count == 1 ? "Ein Update ist" : faelle.Count + " Updates sind")
                      + " fehlgeschlagen. Finde die Ursache, repariere sie dauerhaft und bring danach das Update zu Ende.");
        sb.AppendLine("Das kann am Update-Weg der UpdateZentrale liegen (" + projekt
                      + ", Katalog programs.json) oder am Programm bzw. dessen Update-Skript selbst.");
        sb.AppendLine("REVIEW: ja");
        sb.AppendLine();
        sb.AppendLine("Arbeite diese Schritte automatisch und genau in dieser Reihenfolge ab:");
        sb.AppendLine("1. Finde die Ursache im Protokoll unten (bei mehreren Fehlern: jeden einzeln, bei Bedarf in den Protokolldateien).");
        sb.AppendLine("2. Repariere dauerhaft, damit dieser Fehler beim nächsten Mal nicht mehr auftritt: Bau die Vorbeugung in die UpdateZentrale ein "
                      + "(programs.json, Update-Weg im Code oder das Update-Skript des Programms), möglichst für die ganze Fehlerart, nicht nur für diesen einen Fall. "
                      + "Kann dieselbe Ursache auch unter macOS auftreten, zieh " + Path.Combine(Pfade.RepoWurzel, "UpdaterZentrale-macOS") + " mit.");
        sb.AppendLine("3. Halte Ursache und Vorbeugung im passenden Bug-Almanach unter proggs\\bugs fest, damit die UpdateZentrale aus jedem Fehler lernt. Committe und pushe die Änderungen.");
        sb.AppendLine("4. Führe danach das Update jeder gescheiterten Software zu Ende und prüfe die installierte Version nach. Nutze schon geladene Installer und lade große Dateien nicht unnötig erneut. "
                      + "Wirkt die Reparatur erst in der neu gebauten UpdateZentrale, führe den reparierten Weg hier selbst aus.");
        sb.AppendLine("5. Führe danach ohne Rückfrage das Standard-Code-Review über die Änderungen dieser Sitzung aus (Skill /code-review), behebe die Befunde, committe und pushe. Ohne Codeänderung entfällt das Review.");
        sb.AppendLine("6. Baue als allerletzten Schritt die UpdateZentrale neu, sobald sich in " + projekt + " etwas geändert hat: pwsh -File " + Path.Combine(projekt, "update-zentrale.ps1")
                      + ", mit dem höchsten erlaubten Zeitlimit. Das Skript zeigt mir ein Ja/Nein-Fenster und schließt die laufende UpdateZentrale. "
                      + "Prüfe danach in " + Path.Combine(Protokollierung.Ordner, "selbstupdate.log") + ", ob die neue Fassung gestartet ist.");
        sb.AppendLine();
        sb.AppendLine("Regeln für diese Sitzung:");
        sb.AppendLine("- " + OhneAdminHinweis);
        sb.AppendLine("- " + BeendenHinweis + " Eigene Werkzeuge (OpenLauncher, TVO, CVO, UpdateZentrale) nur über ihre Update-Skripte nach den Regeln der CLAUDE.md.");
        sb.AppendLine("- " + RueckfrageHinweis);
        sb.AppendLine("- Andere Sitzungen können parallel an diesem Repo arbeiten: Lies die Versionsnummer direkt vor dem Bump frisch und committe nur deine eigenen Pfade.");
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
            if (eintrag.Id.Equals(ViewModels.Sammelplan.SelbstId, StringComparison.OrdinalIgnoreCase))
                sb.AppendLine("Das ist die UpdateZentrale selbst: Schritt 4 entfällt für sie, ihr Update erledigt Schritt 6, auch ohne Änderung in ihrem Ordner.");
            if (eintrag.BeendenVorUpdate)
                sb.AppendLine(eintrag.Name + " muss fürs Update beendet sein. Läuft es gerade, frag vor dem Beenden nach.");
            if (eintrag.KannCliBeenden)
                sb.AppendLine("Achtung, dieses Update kann laufende CLI-Sitzungen beenden, auch diese hier: " + eintrag.CliRisiko
                              + " Führe es deshalb erst nach den Schritten 5 und 6 aus und starte es abgekoppelt, damit es auch dann zu Ende läuft, wenn diese Sitzung endet. "
                              + "Verlangt es geschlossene Terminalfenster, melde das, statt es zu erzwingen.");

            if (File.Exists(bericht.ProtokollDatei))
            {
                try
                {
                    // The daily log is shared by all programs; only this program's lines count.
                    var alleZeilen = Protokollierung.LiesAlleZeilen(bericht.ProtokollDatei).ToList();
                    var zeilen = ZeilenDesProgramms(alleZeilen, eintrag.Id);
                    var gefiltert = zeilen.Count > 0;
                    if (!gefiltert) zeilen = alleZeilen;     // nothing tagged in this part: the plain tail as before
                    var ende = new List<string>();
                    var laenge = 0;
                    var budget = ProtokollZeichen / faelle.Count;
                    for (var i = zeilen.Count - 1; i >= 0; i--)
                    {
                        var kurz = Kappen(zeilen[i], 2000);
                        if (laenge + kurz.Length >= budget)
                        {
                            // The last line is usually the decisive error: keep it, shortened.
                            if (ende.Count > 0) break;
                            kurz = Kappen(zeilen[i], Math.Max(200, budget - 2));
                        }
                        ende.Insert(0, kurz);
                        laenge += kurz.Length + 1;
                        if (laenge >= budget) break;
                    }
                    var titel = gefiltert ? "Protokoll von " + eintrag.Id
                                          : "Protokoll (keine Zeilen von " + eintrag.Id + " in dieser Datei, daher das Dateiende)";
                    sb.AppendLine();
                    sb.AppendLine(ende.Count < zeilen.Count
                        ? titel + " (letzte " + ende.Count + " von " + zeilen.Count + " Zeilen, den Rest in der Datei oben lesen):"
                        : titel + ":");
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

    private static readonly System.Text.RegularExpressions.Regex Zeilenkopf =
        new(@"^\d\d:\d\d:\d\d  \[(.+?)\]  ", System.Text.RegularExpressions.RegexOptions.Compiled);

    /// <summary>
    /// The lines that belong to one program. A tagged line ("HH:mm:ss  [id]  ...") names its
    /// owner; untagged lines -- fragments after a bare \r that ReadLine split off, and the run
    /// header/footer from LaufBeginnen/LaufBeenden -- belong to the last owner, and the header and
    /// footer lines that name a program switch the owner themselves.
    /// </summary>
    internal static List<string> ZeilenDesProgramms(IReadOnlyList<string> zeilen, string id)
    {
        var ergebnis = new List<string>();
        var eigen = false;
        for (var i = 0; i < zeilen.Count; i++)
        {
            var zeile = zeilen[i];
            var kopf = Zeilenkopf.Match(zeile);
            if (kopf.Success)
                eigen = kopf.Groups[1].Value == id;
            else if (zeile.StartsWith("Update-Lauf: ", StringComparison.Ordinal))
            {
                eigen = zeile.Contains("(" + id + ", ", StringComparison.Ordinal);
                // The "=====" line just before the header belongs to this run too.
                if (eigen && i > 0 && zeilen[i - 1].StartsWith("====", StringComparison.Ordinal)) ergebnis.Add(zeilen[i - 1]);
            }
            else if (zeile.StartsWith("Programm: ", StringComparison.Ordinal))
                eigen = zeile.TrimEnd().EndsWith("(" + id + ")", StringComparison.Ordinal);

            if (eigen) ergebnis.Add(zeile);
        }
        return ergebnis;
    }

    /// <summary>At most <paramref name="max"/> characters; the ellipsis only when something was cut.</summary>
    private static string Kappen(string zeile, int max)
        => zeile.Length <= max ? zeile : zeile[..Math.Max(0, max - 2)] + " …";
}

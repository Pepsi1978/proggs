using System.IO;
using System.Net.NetworkInformation;
using System.Text;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// A CHECK that ends in an error (a version query timing out, a registry answering garbage, an
/// exception) reaches a Claude Code session too, not only a failed update. Failures are collected:
/// "check all" runs every card at once, so one session gets every failure of that pass instead of
/// one session per card. Each program has its own lock, separate from the update failures.
/// Everything here runs on the UI thread (started from the cards, continued on their
/// SynchronizationContext): <see cref="Einstellungen"/> is not thread-safe.
/// </summary>
public static class PruefFehlerUebergabe
{
    /// <param name="NochAktuell">True while the card still shows exactly this failure.</param>
    /// <param name="Melden">Tells the card what happened to its failure (text, started).</param>
    public sealed record Fall(ProgrammEintrag Eintrag, string Meldung, string Protokoll, DateTime Zeit,
                              Func<bool> NochAktuell, Action<string, bool> Melden);

    /// <summary>Quiet time after the last new failure before the session starts.</summary>
    internal static TimeSpan Ruhezeit { get; set; } = TimeSpan.FromSeconds(20);

    /// <summary>Set by "check all" and the startup check: the collection waits for the pass to end.</summary>
    public static volatile bool SammelpruefungLaeuft;

    /// <summary>Any other batch (e.g. "update all"): no session starts while it runs.</summary>
    public static Func<bool>? SammelLaeuft { get; set; }

    /// <summary>Off in the test run: a failing test check must not start a real session.</summary>
    internal static bool Aktiv { get; set; } = true;

    private const int ProtokollZeichen = 12000;
    private const int MeldungZeichen = 600;

    /// <summary>The launcher passes the task as one argument; Windows caps a command line at 32767 characters.</summary>
    internal const int AuftragZeichen = 25000;

    private static readonly Dictionary<string, Fall> Offen = new(StringComparer.OrdinalIgnoreCase);
    private static DateTime _letzterEingang;
    private static Task? _sammler;

    /// <summary>Queues a check failure; a session starts once the failures stop coming.</summary>
    public static void Melden(Fall fall, Einstellungen einstellungen)
    {
        if (!Aktiv) return;
        if (Gesperrt(fall.Eintrag.Id, einstellungen))
        {
            Diagnose.Ereignis(Schwere.Info, "uebergabe", "pruefung.gesperrt", "Sperrfrist läuft", null,
                              new Dictionary<string, object?> { ["programm"] = fall.Eintrag.Id });
            return;
        }
        Offen[fall.Eintrag.Id] = fall;
        _letzterEingang = DateTime.Now;
        _sammler ??= SammelnAsync(einstellungen);
    }

    /// <summary>A later check of the same program did not fail: its queued failure is void.</summary>
    public static void Erledigt(string programmId) => Offen.Remove(programmId);

    private static bool Gesperrt(string id, Einstellungen einstellungen)
        => einstellungen.Fuer(id).PruefFehlerUebergebenAm is { } zuletzt && DateTime.Now - zuletzt < ClaudeUebergabe.Sperrfrist;

    private static async Task SammelnAsync(Einstellungen einstellungen)
    {
        try
        {
            while (SammelpruefungLaeuft || SammelLaeuft?.Invoke() == true || DateTime.Now - _letzterEingang < Ruhezeit)
                await Task.Delay(TimeSpan.FromSeconds(2));
            var faelle = Offen.Values.ToList();
            Offen.Clear();
            _sammler = null;
            await StartenAsync(faelle, einstellungen);
        }
        catch (Exception diagAusnahme)
        {
            _sammler = null;
            Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Warnung);
        }
    }

    private static async Task StartenAsync(List<Fall> faelle, Einstellungen einstellungen)
    {
        // A fresh check may have fixed it meanwhile, or another failure replaced it on the card.
        faelle = faelle.Where(f => f.NochAktuell() && !Gesperrt(f.Eintrag.Id, einstellungen)).ToList();
        if (faelle.Count == 0) return;
        if (!NetworkInterface.GetIsNetworkAvailable())
        {
            Diagnose.Ereignis(Schwere.Info, "uebergabe", "pruefung.offline", "Kein Netz – Prüffehler nicht übergeben");
            return;
        }

        // Same gate as the update failures: launcher starts run one after the other.
        await ClaudeUebergabe.AutomatischSchleuse.WaitAsync();
        string? problem;
        bool vielleicht;
        try
        {
            faelle = faelle.Where(f => !Gesperrt(f.Eintrag.Id, einstellungen)).ToList();
            if (faelle.Count == 0) return;
            var bisher = faelle.Select(f => einstellungen.Fuer(f.Eintrag.Id).PruefFehlerUebergebenAm).ToList();
            foreach (var f in faelle) einstellungen.Fuer(f.Eintrag.Id).PruefFehlerUebergebenAm = DateTime.Now;
            einstellungen.Speichern();

            var kennung = faelle.Count == 1 ? faelle[0].Eintrag.Id : "sammel-" + faelle.Count;
            (problem, vielleicht) = await ClaudeUebergabe.SitzungStartenAsync("pruefung-" + kennung,
                "UpdateZentrale: Prueffehler " + kennung, () => AuftragText(faelle));
            Diagnose.Ereignis(problem is null ? Schwere.Info : Schwere.Warnung, "uebergabe", "pruefung.start",
                              problem ?? "Sitzung gestartet", null,
                              new Dictionary<string, object?> { ["programme"] = string.Join(",", faelle.Select(f => f.Eintrag.Id)) });
            if (problem is not null && !vielleicht)
            {
                for (var i = 0; i < faelle.Count; i++) einstellungen.Fuer(faelle[i].Eintrag.Id).PruefFehlerUebergebenAm = bisher[i];
                einstellungen.Speichern();
            }
        }
        finally
        {
            ClaudeUebergabe.AutomatischSchleuse.Release();
        }

        var gestartet = problem is null || vielleicht;
        foreach (var f in faelle)
        {
            try
            {
                f.Melden(gestartet
                    ? "Prüffehler automatisch an Claude Code übergeben – die Sitzung läuft im Terminal und beugt dem Fehler künftig vor. Meldung: " + f.Meldung
                    : "Prüffehler konnte nicht an Claude Code übergeben werden: " + problem + " Meldung: " + f.Meldung, gestartet);
            }
            catch (Exception diagAusnahme) { Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Debug); }
        }
    }

    private static string Kappen(string text, int max)
        => text.Length <= max ? text : text[..Math.Max(0, max - 2)] + " …";

    internal static string AuftragText(IReadOnlyList<Fall> faelle)
    {
        var projekt = Path.Combine(Pfade.RepoWurzel, "UpdateZentrale");
        var sb = new StringBuilder();
        sb.AppendLine("Auftrag von der UpdateZentrale (Windows): Die Prüfung " + (faelle.Count == 1 ? "eines Programms ist" : "von " + faelle.Count + " Programmen ist")
                      + " fehlgeschlagen (Versionsabfrage, nicht das Update). Finde die Ursache und repariere sie dauerhaft, damit dieser Prüffehler nicht wieder auftritt.");
        sb.AppendLine();
        sb.AppendLine("Arbeite diese Schritte automatisch und genau in dieser Reihenfolge ab:");
        sb.AppendLine("1. Finde die Ursache aus Meldung und Protokoll unten, bei Bedarf in den Protokolldateien. Auch ein vorübergehender Netz- oder Serverfehler (Timeout, HTTP 5xx, Ratenlimit) zählt: "
                      + "Mach die Prüfung dann robuster (Wiederholung mit Pause, längere Frist, Ausweichquelle), statt ihn nur als Zufall abzutun.");
        sb.AppendLine("2. Bau die Vorbeugung dauerhaft in die UpdateZentrale ein (" + projekt + ": Providers, Katalog programs.json), möglichst für die ganze Fehlerart, nicht nur für dieses Programm. "
                      + "Kann dieselbe Ursache unter macOS auftreten, zieh " + Path.Combine(Pfade.RepoWurzel, "UpdaterZentrale-macOS") + " mit.");
        sb.AppendLine("3. Halte Ursache und Vorbeugung im passenden Bug-Almanach unter proggs\\bugs fest.");
        sb.AppendLine("4. Prüfe die betroffenen Programme mit der reparierten Logik erneut und bestätige, dass die Prüfung jetzt sauber durchläuft. Ein Update selbst ist nicht Teil dieses Auftrags.");
        sb.AppendLine("5. Führe ohne Rückfrage das Standard-Code-Review über die Änderungen dieser Sitzung aus (Skill /code-review) und behebe die Befunde. Ohne Codeänderung entfällt das Review.");
        sb.AppendLine("6. Version bumpen, bauen, committen, pushen. Baue als allerletzten Schritt die UpdateZentrale neu, sobald sich in " + projekt + " etwas geändert hat: pwsh -File "
                      + Path.Combine(projekt, "update-zentrale.ps1") + ", mit dem höchsten erlaubten Zeitlimit (das Skript zeigt mir ein Ja/Nein-Fenster). "
                      + "Läuft laut letzter Zeile von " + Protokollierung.SammelDatei + " noch ein Sammellauf („plan“ oder „fortsetzung“), warte vorher auf sein Ende, höchstens zwei Stunden.");
        sb.AppendLine();
        sb.AppendLine("Regeln für diese Sitzung:");
        sb.AppendLine("- " + ClaudeUebergabe.OhneAdminHinweis);
        sb.AppendLine("- " + ClaudeUebergabe.BeendenHinweis + " Eigene Werkzeuge (OpenLauncher, TVO, CVO, UpdateZentrale) nur über ihre Update-Skripte nach den Regeln der CLAUDE.md.");
        sb.AppendLine("- " + ClaudeUebergabe.RueckfrageHinweis);
        sb.AppendLine("- Andere Sitzungen können parallel an diesem Repo arbeiten: Lies die Versionsnummer direkt vor dem Bump frisch und committe nur deine eigenen Pfade.");
        sb.AppendLine("- Weitere Protokolle (Tagesprotokolle, Diagnose-JSONL): " + Protokollierung.Ordner);

        var budget = ProtokollZeichen / Math.Max(1, faelle.Count);
        for (var i = 0; i < faelle.Count; i++)
        {
            var f = faelle[i];
            var teil = new StringBuilder();
            teil.AppendLine();
            if (faelle.Count > 1) teil.AppendLine("=== " + f.Eintrag.Name + " ===");
            teil.AppendLine("Programm: " + f.Eintrag.Name + " (Id " + f.Eintrag.Id + ", Art " + f.Eintrag.Art + ")");
            teil.AppendLine("Zeitpunkt der Prüfung: " + f.Zeit.ToString("dd.MM.yyyy, HH:mm:ss"));
            teil.AppendLine("Meldung: " + Kappen(string.IsNullOrWhiteSpace(f.Meldung) ? "(keine Meldung)" : f.Meldung, MeldungZeichen));
            var protokoll = (f.Protokoll ?? "").Trim();
            if (protokoll.Length > 0)
            {
                if (protokoll.Length > budget) protokoll = "…" + protokoll[^budget..];
                teil.AppendLine("Protokoll der Karte:");
                teil.AppendLine("----");
                teil.AppendLine(protokoll);
                teil.AppendLine("----");
            }
            // Hard cap: the rest is named, the session reads it in the log files.
            if (sb.Length + teil.Length > AuftragZeichen - 300)
            {
                sb.AppendLine();
                sb.AppendLine("Weitere " + (faelle.Count - i) + " Programme mit Prüffehler (Einzelheiten in den Tagesprotokollen): "
                              + Kappen(string.Join(", ", faelle.Skip(i).Select(x => x.Eintrag.Id)), 250));
                break;
            }
            sb.Append(teil);
        }
        return sb.ToString();
    }
}

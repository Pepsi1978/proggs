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
/// </summary>
public static class PruefFehlerUebergabe
{
    /// <param name="Melden">Tells the card what happened to its failure.</param>
    public sealed record Fall(ProgrammEintrag Eintrag, string Meldung, string Protokoll, DateTime Zeit, Action<string> Melden);

    /// <summary>Quiet time after the last new failure before the session starts.</summary>
    internal static TimeSpan Ruhezeit { get; set; } = TimeSpan.FromSeconds(20);

    /// <summary>Set by "check all": the collection waits for the pass to end.</summary>
    public static volatile bool SammelpruefungLaeuft;

    /// <summary>Off in the test run: a failing test check must not start a real session.</summary>
    internal static bool Aktiv { get; set; } = true;

    private const int ProtokollZeichen = 12000;

    private static readonly object Sperre = new();
    private static readonly Dictionary<string, Fall> Offen = new(StringComparer.OrdinalIgnoreCase);
    private static DateTime _letzterEingang;
    private static Task? _sammler;

    /// <summary>Queues a check failure; a session starts once the failures stop coming.</summary>
    public static void Melden(Fall fall, Einstellungen einstellungen)
    {
        if (!Aktiv) return;
        if (einstellungen.Fuer(fall.Eintrag.Id).PruefFehlerUebergebenAm is { } zuletzt
            && DateTime.Now - zuletzt < ClaudeUebergabe.Sperrfrist)
        {
            Diagnose.Ereignis(Schwere.Info, "uebergabe", "pruefung.gesperrt", "Sperrfrist läuft", null,
                              new Dictionary<string, object?> { ["programm"] = fall.Eintrag.Id });
            return;
        }
        lock (Sperre)
        {
            Offen[fall.Eintrag.Id] = fall;
            _letzterEingang = DateTime.Now;
            _sammler ??= Task.Run(() => SammelnAsync(einstellungen));
        }
    }

    private static async Task SammelnAsync(Einstellungen einstellungen)
    {
        List<Fall> faelle;
        while (true)
        {
            await Task.Delay(TimeSpan.FromSeconds(2));
            lock (Sperre)
            {
                if (SammelpruefungLaeuft || DateTime.Now - _letzterEingang < Ruhezeit) continue;
                faelle = Offen.Values.ToList();
                Offen.Clear();
                _sammler = null;
                break;
            }
        }
        try
        {
            await StartenAsync(faelle, einstellungen);
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Warnung);
        }
    }

    private static async Task StartenAsync(List<Fall> faelle, Einstellungen einstellungen)
    {
        // A fresh check may have fixed it meanwhile: the card no longer reports the error then.
        faelle = faelle.Where(f => einstellungen.Fuer(f.Eintrag.Id).PruefFehlerUebergebenAm is not { } z
                                   || DateTime.Now - z >= ClaudeUebergabe.Sperrfrist).ToList();
        if (faelle.Count == 0) return;
        if (!NetworkInterface.GetIsNetworkAvailable())
        {
            Diagnose.Ereignis(Schwere.Info, "uebergabe", "pruefung.offline", "Kein Netz – Prüffehler nicht übergeben");
            return;
        }

        var bisher = faelle.Select(f => einstellungen.Fuer(f.Eintrag.Id).PruefFehlerUebergebenAm).ToList();
        foreach (var f in faelle) einstellungen.Fuer(f.Eintrag.Id).PruefFehlerUebergebenAm = DateTime.Now;
        einstellungen.Speichern();

        var kennung = faelle.Count == 1 ? faelle[0].Eintrag.Id : "sammel-" + faelle.Count;
        var (problem, vielleicht) = await ClaudeUebergabe.SitzungStartenAsync("pruefung-" + kennung,
            "UpdateZentrale: Prueffehler " + kennung, () => AuftragText(faelle));
        Diagnose.Ereignis(problem is null ? Schwere.Info : Schwere.Warnung, "uebergabe", "pruefung.start",
                          problem ?? "Sitzung gestartet", null,
                          new Dictionary<string, object?> { ["programme"] = string.Join(",", faelle.Select(f => f.Eintrag.Id)) });
        if (problem is not null && !vielleicht)
        {
            for (var i = 0; i < faelle.Count; i++) einstellungen.Fuer(faelle[i].Eintrag.Id).PruefFehlerUebergebenAm = bisher[i];
            einstellungen.Speichern();
        }

        var text = problem is null || vielleicht
            ? "Prüffehler automatisch an Claude Code übergeben – die Sitzung läuft im Terminal und beugt dem Fehler künftig vor."
            : problem;
        foreach (var f in faelle)
        {
            try { f.Melden(text); }
            catch (Exception diagAusnahme) { Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Debug); }
        }
    }

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
        sb.AppendLine("- Andere Sitzungen können parallel an diesem Repo arbeiten: Lies die Versionsnummer direkt vor dem Bump frisch und committe nur deine eigenen Pfade.");
        sb.AppendLine("- Weitere Protokolle (Tagesprotokolle, Diagnose-JSONL): " + Protokollierung.Ordner);

        var budget = ProtokollZeichen / Math.Max(1, faelle.Count);
        foreach (var f in faelle)
        {
            sb.AppendLine();
            if (faelle.Count > 1) sb.AppendLine("=== " + f.Eintrag.Name + " ===");
            sb.AppendLine("Programm: " + f.Eintrag.Name + " (Id " + f.Eintrag.Id + ", Art " + f.Eintrag.Art + ")");
            sb.AppendLine("Zeitpunkt der Prüfung: " + f.Zeit.ToString("dd.MM.yyyy, HH:mm:ss"));
            sb.AppendLine("Meldung: " + f.Meldung);
            var protokoll = (f.Protokoll ?? "").Trim();
            if (protokoll.Length > 0)
            {
                if (protokoll.Length > budget) protokoll = "…" + protokoll[^budget..];
                sb.AppendLine("Protokoll der Karte:");
                sb.AppendLine("----");
                sb.AppendLine(protokoll);
                sb.AppendLine("----");
            }
        }
        return sb.ToString();
    }
}

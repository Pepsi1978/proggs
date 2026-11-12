using System.IO;
using System.Net.Http;
namespace JarvisWindows;

/// <summary>Die Regel fürs Live-Gedächtnis wie am Handy (`Merkdatei.KNAPP` in RegelnFaehigkeit.kt), Wortlaut unverändert: Jeder Prompt, der
/// Gedächtnistext schreibt (Lernen, Verdichten), setzt genau diese Konstante ein, nie eine eigene Formulierung.</summary>
public static class Merkregel
{
    public const string KnappText = "So knapp wie möglich, so vollständig wie nötig: Ein fremdes Modell muss den Text ohne Vorwissen richtig verstehen. " +
        "Behalte Namen, Zahlen, Daten, Fristen, Geltung, Auslöser, Ausnahmen und Vorbehalte („laut Frank“, „nicht verifiziert“). " +
        "Streiche Füllwörter, Wiederholungen, Beispiele, Begründungen ohne Einfluss auf das Verhalten und alles, was schon anderswo steht.";
    public const string Knapp = KnappText + " Ein Thema je Eintrag.";
}

/// <summary>Lernen am Laptop: Ruht ein Gespräch 10 Minuten, prüft ein kleines Modell, ob Frank darin neue dauerhafte Angaben
/// oder ausdrückliche Entscheidungen gemacht hat, die Jarvis noch nicht kennt – und gibt sie mit jarvis_merken an
/// das gemeinsame Gedächtnis zurück. So fließt, was am Laptop passiert, ins Live-Gedächtnis aller Geräte.</summary>
public sealed class Lernen
{
    public static Lernen Shared { get; } = new();
    private readonly Dictionary<Guid, CancellationTokenSource> zeitgeber = new();

    private Lernen() { }

    public void MerkeAktivitaet(Gespraech g)
    {
        Haupt.Post(() =>
        {
            // Im Kanalgespräch spricht Jarvis am Handy, nicht Frank: Daraus wird nichts über Frank gelernt.
            if (!Einstellungen.Shared.LernenAn || g.Titel == AgentenKanal.Titel) return;
            if (zeitgeber.TryGetValue(g.Id, out var alt)) alt.Cancel();
            var neu = new CancellationTokenSource();
            zeitgeber[g.Id] = neu;
            _ = Warte(g, neu.Token);
        });
    }

    private async Task Warte(Gespraech g, CancellationToken ct)
    {
        try
        {
            await Task.Delay(TimeSpan.FromSeconds(600), ct);
            zeitgeber.Remove(g.Id);
            await Haupt.Auf(() => Lerne(g));
        }
        catch (OperationCanceledException) { }
        catch (Exception ex) { Log("fehler", "Lernen gescheitert", wo: "Lernen", fehler: ex.Message); }
    }

    private async Task Lerne(Gespraech g)
    {
        // Gelöscht: Dazu gehören auch die Läufe der Helfer. Dort redet nicht Frank, daraus wird nichts über ihn gelernt.
        if (g.Geloescht) return;
        // Läuft gerade etwas, später noch einmal versuchen statt diese Phase zu verlieren.
        if (g.Arbeitet) { MerkeAktivitaet(g); return; }
        var neu = g.Nachrichten.Skip(Math.Min(g.GelerntBis, g.Nachrichten.Count)).ToList();
        if (!neu.Any(n => n.Rolle == Rolle.Frank)) return;
        var stand = g.Nachrichten.Count;
        var verlauf = string.Join("\n", neu.TakeLast(30).Select(n => $"{(n.Rolle == Rolle.Frank ? "Frank" : "Jarvis")}: {n.Text.Kappe(1500)}"));
        var bekannt = await McpKlient.Shared.Text("jarvis_gemerktes_lesen", new JSONObjekt()) ?? "";
        var prompt = $$"""
        Gespräch zwischen Frank und Jarvis am Laptop:
        {{verlauf}}

        Bereits gemerkt:
        {{bekannt.Kappe(30000)}}

        Finde nur NEUE dauerhafte Angaben über Frank (Fakten, Vorlieben, Ziele, Regeln für Jarvis) oder AUSDRÜCKLICHE Entscheidungen, die Frank selbst gesagt hat und die oben noch nicht stehen. Keine Vermutungen, keine Vorschläge von Jarvis, nichts Vorübergehendes, nichts, was Jarvis im Gespräch schon selbst gemerkt hat.
        Schreibregel für jeden Text: {{Merkregel.Knapp}}
        Antworte nur mit JSON:
        {"eintraege":[{"art":"info|regel|ziel|entscheidung|stil","text":"Eintrag in der dritten Person"}]}
        Ist nichts Neues dabei: {"eintraege":[]}
        """;
        var antwort = await Kurzauftrag.Frage(prompt, modell: "claude-sonnet-5-5");
        if (antwort == null) return;
        try
        {
            var a = antwort.IndexOf('{');
            var b = antwort.LastIndexOf('}');
            if (a < 0 || b < 0 || a >= b) return;
            var o = JSON.Objekt(antwort[a..(b + 1)]);
            if (o == null || o.Liste("eintraege") == null) return;
            foreach (var e in o.Objekte("eintraege").Take(5))
            {
                var text = e.Text("text");
                if (string.IsNullOrEmpty(text)) continue;
                var art = e.Text("art") ?? "info";
                // „gelernt“ bleibt am Text erkennbar: Es ist nicht Franks ausdrückliche Vorgabe und begründet keine Rechte. Der Anlass steht nur beim Lesen und Pflegen.
                var r = await McpKlient.Shared.Rufe("jarvis_merken", new JSONObjekt { ["art"] = art, ["text"] = text + " (gelernt am Laptop)", ["anlass"] = ("Gespräch am Laptop: " + g.Titel).Kappe(120) });
                Log("system", $"Gelernt am Laptop ({art}): {text} → {(r.Fehler ? "Fehler" : "gemerkt")}");
            }
        }
        finally
        {
            g.GelerntBis = stand;
            g.Speichern();
        }
    }
}

/// <summary>Konsolidiert das Gemerkte nach der neuen Regel wie `jarvis_gemerktes_verdichten` am Handy: Ein Modell schlägt je Art knappere Fassungen und
/// zusammengelegte Dubletten vor; Frank sieht die Vorschläge und übernimmt sie mit einem Klick (das Werkzeug läuft nur auf seinen ausdrücklichen Wunsch).
/// Herkunft, Geltung, Frist und Anlass hält das Handy selbst fest.</summary>
public sealed class Verdichter : Beobachtbar
{
    public static Verdichter Shared { get; } = new();
    public sealed record Neufassung(long Id, string Alt, string Neu);
    public sealed record Zusammen(long Id, long In, string Alt);
    public sealed record Vorschlag(string Art, string Titel, List<Neufassung> Neu, List<Zusammen> Weg);

    private static readonly (string Art, string Kopf)[] Arten =
    {
        ("regel", "Regel (art regel):"), ("ziel", "Ziel (art ziel):"), ("info", "Notiz (art info):"), ("programmierregel", "Programmierregel (art programmierregel):"),
        ("stil", "Stilregel (art stil):"), ("entscheidung", "Entscheidung (art entscheidung):"),
    };

    private bool _laeuft;
    public bool Laeuft { get => _laeuft; private set => Setze(ref _laeuft, value); }
    private string? _meldung;
    public string? Meldung { get => _meldung; private set => Setze(ref _meldung, value); }
    private List<Vorschlag> _vorschlaege = new();
    public List<Vorschlag> Vorschlaege { get => _vorschlaege; private set => Setze(ref _vorschlaege, value); }

    /// <summary>Liest das Gemerkte und lässt je Art Vorschläge rechnen. Ändert noch nichts.</summary>
    public async Task Vorschlagen()
    {
        if (Laeuft) return;
        Laeuft = true;
        Meldung = "Liest das Gemerkte und rechnet Vorschläge …";
        Vorschlaege = new();
        try
        {
            var text = await McpKlient.Shared.Text("jarvis_gemerktes_lesen", new JSONObjekt());
            if (text == null) { Meldung = "Das Handy-Jarvis ist gerade nicht erreichbar."; return; }
            var bloecke = Bloecke(McpKlient.OhneKontextHinweis(text));
            var aufgaben = bloecke.Where(b => b.Eintraege.Count > 1).Select(async b =>
            {
                var liste = string.Join("\n", b.Eintraege.Select(e => $"[{e.Id}] {e.Text}"));
                var prompt = $$"""
                Gemerkte Einträge der Art „{{b.Art}}“ aus Jarvis' Gedächtnis:
                {{liste}}

                Schreibe zu lang geratene Einträge knapper und lege echte Dubletten (gleiches Thema) zusammen. Schreibregel: {{Merkregel.Knapp}}
                Klammerzusätze wie „(gelernt …)“, „(gilt nur …)“ oder „(Anlass …)“ sind Herkunftsangaben, die Jarvis selbst führt: nicht in die Neufassung übernehmen und nicht verändern.
                Inhalt nie ändern, nichts hinzuerfinden, nichts weglassen, was das Verhalten beeinflusst. Unveränderte Einträge nicht nennen.
                Antworte nur mit JSON: {"eintraege":[{"id":1,"text":"Neufassung"}],"zusammenlegen":[{"id":5,"in":1}]}
                Bei zusammenlegen steht der Inhalt von id danach in der Neufassung von in (die dann in eintraege stehen muss). Ist nichts zu tun: {"eintraege":[],"zusammenlegen":[]}
                """;
                var antwort = await Kurzauftrag.Frage(prompt, modell: "claude-opus-5-5");
                if (antwort == null) return null;
                var a = antwort.IndexOf('{');
                var z = antwort.LastIndexOf('}');
                if (a < 0 || z <= a || JSON.Objekt(antwort[a..(z + 1)]) is not { } o) return null;
                var alt = b.Eintraege.ToDictionary(e => e.Id, e => e.Text);
                var neu = o.Objekte("eintraege").Where(e => e.Ganz("id") is { } id && alt.ContainsKey(id) && (e.Text("text") ?? "").Trim().Length > 0)
                    .Select(e => new Neufassung(e.Ganz("id")!.Value, alt[e.Ganz("id")!.Value], MitWoertlichemZusatz(alt[e.Ganz("id")!.Value], e.Text("text")!.Trim()))).Where(n => n.Neu != OhneZusatz(n.Alt)).ToList();
                var weg = o.Objekte("zusammenlegen").Where(e => e.Ganz("id") is { } id && e.Ganz("in") is { } ziel && id != ziel && alt.ContainsKey(id) && neu.Any(n => n.Id == ziel))
                    .Select(e => new Zusammen(e.Ganz("id")!.Value, e.Ganz("in")!.Value, alt[e.Ganz("id")!.Value])).ToList();
                return neu.Count + weg.Count == 0 ? null : new Vorschlag(b.Art, b.Titel, neu, weg);
            }).ToList();
            var ergebnis = (await Task.WhenAll(aufgaben)).Where(v => v != null).Select(v => v!).ToList();
            Vorschlaege = ergebnis;
            Meldung = ergebnis.Count == 0 ? "Nichts zu verdichten: Das Gemerkte ist schon knapp." : $"{ergebnis.Sum(v => v.Neu.Count)} Neufassung(en), {ergebnis.Sum(v => v.Weg.Count)} Zusammenlegung(en) vorgeschlagen. Erst „Übernehmen“ ändert etwas.";
        }
        finally { Laeuft = false; }
    }

    /// <summary>Übernimmt einen Vorschlag am Handy (jarvis_gemerktes_verdichten).</summary>
    public async Task Uebernehmen(Vorschlag v)
    {
        if (Laeuft) return;
        Laeuft = true;
        try
        {
            var r = await McpKlient.Shared.Rufe("jarvis_gemerktes_verdichten", new JSONObjekt
            {
                ["art"] = v.Art,
                ["eintraege"] = v.Neu.Select(n => (object?)new JSONObjekt { ["id"] = n.Id, ["text"] = n.Neu }).ToList(),
                ["zusammenlegen"] = v.Weg.Select(w => (object?)new JSONObjekt { ["id"] = w.Id, ["in"] = w.In }).ToList(),
            });
            Meldung = McpKlient.OhneKontextHinweis(r.Text).Trim();
            Log("gedaechtnis", $"Gemerktes verdichtet ({v.Art})", wo: "Gedächtnis", fehler: r.Fehler ? r.Text : null);
            if (!r.Fehler) Vorschlaege = Vorschlaege.Where(x => x != v).ToList();
        }
        finally { Laeuft = false; }
    }

    public void Verwerfen(Vorschlag v) => Vorschlaege = Vorschlaege.Where(x => x != v).ToList();

    /// <summary>„(gelernt am Laptop)“ und „(gelernt am Mac)“ stehen wörtlich im Text (die Rechner schreiben sie so): Sie bleiben an der Neufassung.</summary>
    private static string MitWoertlichemZusatz(string alt, string neu)
    {
        var m = System.Text.RegularExpressions.Regex.Match(alt, @"\(gelernt am (Laptop|Mac)\)\s*$");
        return m.Success && !neu.EndsWith(m.Value.Trim(), StringComparison.Ordinal) ? neu + " " + m.Value.Trim() : neu;
    }

    private static string OhneZusatz(string t) => System.Text.RegularExpressions.Regex.Replace(t, @"\s*\((gelernt|gilt nur|Anlass)[^)]*\)\s*$", "").Trim();

    private sealed record Block(string Art, string Titel, List<(long Id, string Text)> Eintraege);

    private static List<Block> Bloecke(string text)
    {
        var aus = new List<Block>();
        Block? b = null;
        foreach (var roh in text.Split('\n'))
        {
            var z = roh.Trim();
            var kopf = Arten.FirstOrDefault(a => z == a.Kopf);
            if (kopf.Art != null) { b = new Block(kopf.Art, kopf.Kopf.Split(" (")[0], new()); aus.Add(b); continue; }
            var m = System.Text.RegularExpressions.Regex.Match(z, @"^\[(\d+)\]\s+(.+)$");
            if (b != null && m.Success) b.Eintraege.Add((long.Parse(m.Groups[1].Value), m.Groups[2].Value));
        }
        return aus;
    }
}

using System.IO;
using System.Net.Http;
namespace JarvisWindows;

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
            await Haupt.Auf(() => Lerne(g));
        }
        catch (OperationCanceledException) { }
        catch (Exception ex) { Log("fehler", "Lernen gescheitert", wo: "Lernen", fehler: ex.Message); }
    }

    private async Task Lerne(Gespraech g)
    {
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

        Finde nur NEUE dauerhafte Angaben über Frank (Fakten, Vorlieben, Ziele, Regeln für Jarvis) oder AUSDRÜCKLICHE Entscheidungen, die Frank selbst gesagt hat und die oben noch nicht stehen. Keine Vermutungen, keine Vorschläge von Jarvis, nichts Vorübergehendes, nichts, was Jarvis im Gespräch schon selbst gemerkt hat. Antworte nur mit JSON:
        {"eintraege":[{"art":"info|regel|ziel|entscheidung|stil","text":"ein vollständiger Satz in der dritten Person"}]}
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
                var r = await McpKlient.Shared.Rufe("jarvis_merken", new JSONObjekt { ["art"] = art, ["text"] = text + " (gelernt am Laptop)" });
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

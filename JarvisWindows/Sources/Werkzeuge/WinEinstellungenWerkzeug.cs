using System.Globalization;

namespace JarvisWindows;

/// <summary>Einstellungen von Jarvis am PC per Sprache lesen und ändern (Gegenstück zum EinstellungsKatalog der Handy-App).
/// Zugangsschlüssel, Passwörter und Server-Adressen gehören bewusst nicht hinein. Läuft auf dem Hauptfaden.</summary>
public static class WinEinstellungsKatalog
{
    /// <param name="Setzen">null = ok, sonst Fehlermeldung.</param>
    public sealed record Eintrag(string Kennung, string Bereich, string Titel, string Beschreibung, string Werte, Func<string> Lesen, Func<string, string?> Setzen);

    public static bool? Wahr(string s) => s.ToLowerInvariant() switch
    {
        "true" or "an" or "ein" or "ja" or "1" or "on" => true,
        "false" or "aus" or "nein" or "0" or "off" => false,
        _ => null,
    };

    private static Eintrag Stillezeit(string kennung, string titel, string beschreibung, Func<double> lies, Action<double> setze)
        => new(kennung, "Sprache", titel, beschreibung, "1 bis 15 Sekunden, auch mit Komma, z. B. 2,5", () => Einstellungen.StilleText(lies()), v =>
        {
            if (Einstellungen.StilleAus(v) is not { } s) return "Erlaubt: 1 bis 15 Sekunden, auch mit Komma";
            setze(s);
            return null;
        });

    private static string AnAus(bool b) => b ? "an" : "aus";

    private static string? Schalte(string v, Action<bool> setze)
    {
        if (Wahr(v) is not { } b) return "an oder aus";
        setze(b);
        return null;
    }

    public static IReadOnlyList<Eintrag> Alle
    {
        get
        {
            var e = Einstellungen.Shared;
            var v = VorleseEinstellungen.Shared;
            var d = DesignStand.Shared;
            var tk = TranskriptionsEinstellungen.Shared;
            string Stille() => string.Join(", ", LiveOptionen.StilleEnde.Select(x => x.Minuten.ToString()));
            return new List<Eintrag>
            {
                new("modell", "Jarvis' Kopf", "Modell", "Welches Modell am PC denkt.", string.Join(" | ", ModellWahl.Alle.Select(m => m.Id)), () => e.Modell, w =>
                {
                    if (ModellWahl.Alle.FirstOrDefault(m => m.Id == w || m.Name.Equals(w, StringComparison.OrdinalIgnoreCase)) is not { } m) return "Unbekanntes Modell";
                    e.Modell = m.Id;
                    return null;
                }),
                new("denkstufe", "Jarvis' Kopf", "Denkstufe", "Wie gründlich das Modell nachdenkt.", "low | medium | high | xhigh | max", () => e.Effort, w =>
                {
                    if (!new[] { "low", "medium", "high", "xhigh", "max" }.Contains(w)) return "Erlaubt: low, medium, high, xhigh, max";
                    e.Effort = w;
                    return null;
                }),
                new("schrittlimit", "Jarvis' Kopf", "Schrittlimit", "Werkzeugschritte je Anfrage, 0 = unbegrenzt.", "0–200", () => e.SchrittLimit.ToString(), w =>
                {
                    if (!int.TryParse(w, out var n) || n is < 0 or > 200) return "Zahl 0–200";
                    e.SchrittLimit = n;
                    return null;
                }),
                new("helfer_automatik", "Eigene Tools", "Helfer selbst bauen lassen", "Aus = Pause: Jarvis beauftragt keine neuen Helfer, vorhandene laufen weiter.", "an | aus", () => AnAus(e.HelferAutomatik), w => Schalte(w, b => e.HelferAutomatik = b)),
                new("recherche_modell", "Eigene Tools", "Recherche-Modell", "Modell für gründliche Recherchen beim Helferbau; keine kleinen Modelle.", string.Join(" | ", ModellWahl.FuerRecherche.Select(m => m.Id)), () => e.RechercheModell, w =>
                {
                    if (ModellWahl.FuerRecherche.FirstOrDefault(m => m.Id == w || m.Name.Equals(w, StringComparison.OrdinalIgnoreCase)) is not { } m) return "Für Recherchen nicht wählbar";
                    e.RechercheModell = m.Id;
                    return null;
                }),
                new("recherche_effort", "Eigene Tools", "Recherche-Effort", "Wie gründlich die Recherche-Agenten denken.", "medium | high | xhigh | max", () => e.RechercheEffort, w =>
                {
                    if (!new[] { "medium", "high", "xhigh", "max" }.Contains(w)) return "Erlaubt: medium, high, xhigh, max";
                    e.RechercheEffort = w;
                    return null;
                }),
                new("lernen", "Jarvis' Kopf", "Lernen am PC", "Neue Angaben aus ruhenden PC-Gesprächen ins gemeinsame Gedächtnis.", "an | aus", () => AnAus(e.LernenAn), w => Schalte(w, b => e.LernenAn = b)),
                new("vorlesen", "Stimme", "Antworten vorlesen", "Im Sprachgespräch die Antworten vorlesen.", "an | aus", () => AnAus(e.Vorlesen), w => Schalte(w, b => e.Vorlesen = b)),
                new("vorlese_anbieter", "Stimme", "Vorlese-Anbieter", "Wer vorliest.", string.Join(" | ", VorleseAnbieter.AllCases.Select(a => a.RawValue)), () => v.Anbieter.RawValue, w =>
                {
                    foreach (var a in VorleseAnbieter.AllCases)
                        if (a.RawValue == w || a.Titel.Contains(w, StringComparison.OrdinalIgnoreCase)) { v.Anbieter = a; return null; }
                    return "Unbekannter Anbieter";
                }),
                new("tempo", "Stimme", "Sprechtempo", "Tempo des gewählten Vorlese-Anbieters.", $"{VorleseEinstellungen.TempoMin.ToString(CultureInfo.InvariantCulture)}–{VorleseEinstellungen.TempoMax.ToString(CultureInfo.InvariantCulture)}",
                    () => v.Tempo(v.Anbieter).ToString("0.00", CultureInfo.InvariantCulture), w =>
                {
                    if (!double.TryParse(w.Replace(',', '.'), NumberStyles.Float, CultureInfo.InvariantCulture, out var x) || x < VorleseEinstellungen.TempoMin || x > VorleseEinstellungen.TempoMax) return "Zahl außerhalb des Bereichs";
                    v.SetzeTempo(x, fuer: v.Anbieter);
                    return null;
                }),
                new("stimme", "Stimme", "Stimme", "Stimme des gewählten Vorlese-Anbieters.", string.Join(" | ", v.StimmenListe(v.Anbieter).Take(40).Select(s => s.Id)), () => v.Stimme(v.Anbieter), w =>
                {
                    if (v.StimmenListe(v.Anbieter).FirstOrDefault(s => s.Id == w || s.Name.Contains(w, StringComparison.OrdinalIgnoreCase)) is not { } s) return "Unbekannte Stimme";
                    v.SetzeStimme(s.Id, fuer: v.Anbieter);
                    return null;
                }),
                new("weckwort_an", "Sprache", "Weckwort", "Auf das Weckwort lauschen.", "an | aus", () => AnAus(e.WeckwortAn), w => Schalte(w, b =>
                {
                    e.WeckwortAn = b;
                    if (b) Weckwort.Shared.Starten(); else Weckwort.Shared.Stoppen();
                })),
                Stillezeit("weckwort_stille_app", "Stillezeit in der App", "Nach so viel Stille seit Franks letztem Wort schließt das Mikrofon, wenn das Gespräch bei Jarvis vorn begann und er noch vorn ist, per Weckwort oder Strg+Alt+J; nicht im Live-Modus. Sagt Frank nur „die Stillezeit“, ist diese gemeint.", () => e.StilleInAppSek, s => e.StilleInAppSek = s),
                Stillezeit("weckwort_stille_aussen", "Stillezeit außerhalb der App", "Nach so viel Stille seit Franks letztem Wort schließt das Mikrofon in allen anderen Fällen (Gespräch im Hintergrund begonnen, Jarvis nicht mehr vorn oder Bildschirm gesperrt), per Weckwort oder Strg+Alt+J; nicht im Live-Modus.", () => e.StilleAussenSek, s => e.StilleAussenSek = s),
                new("weckwort", "Sprache", "Weckwort-Phrase", "z. B. „Hey Jarvis“ oder „Okay Computer“.", "Text", () => e.Weckwort, w =>
                {
                    if (w.Length == 0) return "Leer geht nicht";
                    e.Weckwort = w;
                    Weckwort.Shared.Stoppen();
                    if (e.WeckwortAn) Weckwort.Shared.Starten();
                    return null;
                }),
                new("transkription", "Sprache", "Transkription", "Welche Spracherkennung das Diktat nutzt.", "parakeet | groq | windows", () => e.Transkription, w =>
                {
                    var t = w.ToLowerInvariant();
                    if (!new[] { "windows", "groq", "parakeet" }.Contains(t)) return "Erlaubt: parakeet, groq, windows";
                    e.Transkription = t;
                    return null;
                }),
                new("parakeet_woerterbuch_an", "Sprache", "Parakeet-Wörterbuch", "Eigene Begriffe bei der Offline-Transkription mit Parakeet bevorzugen.", "an | aus", () => AnAus(tk.ParakeetWoerterbuchAn), w => Schalte(w, b => tk.ParakeetWoerterbuchAn = b)),
                new("parakeet_begriffe", "Sprache", "Parakeet-Begriffe", "Begriffe des Parakeet-Wörterbuchs. Ersetzt die ganze Liste.", "Begriffe durch Komma oder Zeilenumbruch getrennt, höchstens 100", () => tk.ParakeetWoerterbuch, w =>
                {
                    tk.ParakeetWoerterbuch = w;
                    return null;
                }),
                new("tastenkuerzel", "Sprache", "Globale Tastenkürzel", "Strg+Alt+J sprechen, Strg+Alt+K Fenster.", "an | aus", () => AnAus(e.GlobalesKuerzel), w => Schalte(w, b =>
                {
                    e.GlobalesKuerzel = b;
                    if (b) AppSteuerung.Shared.KuerzelRegistrieren(); else AppSteuerung.Shared.KuerzelAbmelden();
                })),
                new("live_anbieter", "Live-Gespräch", "Live-Anbieter", "OpenAI oder Gemini.", "openai | gemini", () => e.LiveAnbieter, w =>
                {
                    if (w is not ("openai" or "gemini")) return "openai oder gemini";
                    e.LiveAnbieter = w;
                    return null;
                }),
                new("live_stille_ende", "Live-Gespräch", "Bei Stille beenden", "Beendet das Live-Gespräch nach so vielen Minuten Stille; 0 = nie.", string.Join(" | ", LiveOptionen.StilleEnde.Select(x => x.Minuten.ToString())), () => e.LiveStilleMinuten.ToString(), w =>
                {
                    if (!int.TryParse(w, out var m) || !LiveOptionen.StilleEnde.Any(x => x.Minuten == m)) return "Erlaubt: " + Stille();
                    e.LiveStilleMinuten = m;
                    return null;
                }),
                new("live_modell_openai", "Live-Gespräch", "OpenAI-Live-Modell", "", string.Join(" | ", LiveOptionen.OpenAIModelle.Select(x => x.Id)), () => e.LiveModellOpenAI, w => LiveWahl.Setze(LiveOptionen.OpenAIModelle.Select(x => x.Id), w, v => e.LiveModellOpenAI = v)),
                new("live_stimme_openai", "Live-Gespräch", "OpenAI-Live-Stimme", "", string.Join(" | ", LiveOptionen.LiveStimmen.Select(x => x.Id)), () => e.LiveStimmeOpenAI, w => LiveWahl.Setze(LiveOptionen.LiveStimmen.Select(x => x.Id), w, v => e.LiveStimmeOpenAI = v)),
                new("live_modell_gemini", "Live-Gespräch", "Gemini-Live-Modell", "", string.Join(" | ", LiveOptionen.GeminiModelle.Select(x => x.Id)), () => e.LiveModellGemini, w => LiveWahl.Setze(LiveOptionen.GeminiModelle.Select(x => x.Id), w, v => e.LiveModellGemini = v)),
                new("live_stimme_gemini", "Live-Gespräch", "Gemini-Live-Stimme", "", string.Join(" | ", LiveOptionen.GeminiStimmen.Select(x => x.Id)), () => e.LiveStimmeGemini, w => LiveWahl.Setze(LiveOptionen.GeminiStimmen.Select(x => x.Id), w, v => e.LiveStimmeGemini = v)),
                new("darstellung", "Darstellung", "Darstellung", "Hell, dunkel oder automatisch nach Sonnenauf- und -untergang in Berlin.", "hell | automatik | dunkel", () => d.Modus.ToString().ToLowerInvariant(), w =>
                {
                    switch (w.ToLowerInvariant())
                    {
                        case "hell": d.Modus = DesignModus.Hell; return null;
                        case "automatik": d.Modus = DesignModus.Automatik; return null;
                        case "dunkel": d.Modus = DesignModus.Dunkel; return null;
                        default: return "hell, automatik oder dunkel";
                    }
                }),
                // Bewusst nicht per Werkzeug änderbar: sonst könnte das Modell eine ausgeschaltete Sperre selbst aufheben.
                new("vollzugriff", "PC", "Vollzugriff", "Jarvis darf am PC alles ohne Rückfrage.", "nur lesen", () => AnAus(e.Vollzugriff), _ => "Den Vollzugriff schaltet nur Frank selbst um (Einstellungen → Windows & Rechte)."),
            };
        }
    }
}

/// <summary>Ein Tippfehler darf nicht dauerhaft gespeichert werden: Das Live-Gespräch scheiterte sonst bei jedem Start.</summary>
internal static class LiveWahl
{
    public static string? Setze(IEnumerable<string> erlaubt, string wert, Action<string> setzen)
    {
        var liste = erlaubt.ToList();
        if (!liste.Contains(wert)) return "Erlaubt: " + string.Join(", ", liste);
        setzen(wert);
        return null;
    }
}

public static partial class WinWerkzeuge
{
    public static readonly IReadOnlyList<LokalesWerkzeug> Weitere = new[]
    {
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_einstellungen",
            "Auch die Darstellung (Kennung darstellung: hell, automatik, dunkel). Jarvis am PC: eigene Einstellungen lesen (aktion lesen, optional bereich) oder ändern (aktion aendern mit kennung und wert): Modell, Denkstufe, Schrittlimit, Lernen, Vorlesen, Tempo, Stimme, Weckwort, Transkription, Parakeet-Wörterbuch, Tastenkürzel, Live-Anbieter/-Modell/-Stimme, Vollzugriff. Für die Einstellungen des Handys bleibt das Werkzeug einstellungen.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["aktion"] = new JSONObjekt { ["type"] = "string", ["enum"] = new List<string> { "lesen", "aendern" } },
                    ["bereich"] = new JSONObjekt { ["type"] = "string", ["description"] = "optional: nur dieser Bereich" },
                    ["kennung"] = new JSONObjekt { ["type"] = "string" },
                    ["wert"] = new JSONObjekt { ["type"] = "string" },
                },
                ["required"] = new List<string> { "aktion" },
            }),
            a => Task.FromResult(Haupt.Wert(() =>
            {
                var katalog = WinEinstellungsKatalog.Alle;
                if (a.Text("aktion") == "aendern")
                {
                    var k = a.Text("kennung") ?? "";
                    if (katalog.FirstOrDefault(x => x.Kennung == k) is not { } e) return new WerkzeugErgebnis($"Unbekannte Kennung {k}", Fehler: true);
                    if (e.Setzen((a.Text("wert") ?? "").Trim()) is { } f) return new WerkzeugErgebnis($"{e.Titel}: {f}. Mögliche Werte: {e.Werte}", Fehler: true);
                    return new WerkzeugErgebnis($"{e.Titel} steht jetzt auf {e.Lesen()}.");
                }
                var b = (a.Text("bereich") ?? "").ToLowerInvariant();
                return new WerkzeugErgebnis(string.Join("\n", katalog.Where(x => b.Length == 0 || x.Bereich.ToLowerInvariant().Contains(b))
                    .Select(x => $"{x.Kennung} [{x.Bereich}] {x.Titel} = {x.Lesen()}{(x.Beschreibung.Length == 0 ? "" : " – " + x.Beschreibung)} (Werte: {x.Werte})")));
            }))),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_funktionen_lesen",
            "Jarvis am PC: Übersicht, was die PC-Version kann, dazu die Reiter der Handy-App, die Zusammenarbeit mit dem CLI und die Regeln für ChatGPT Live (nur bei Bedarf, z. B. „Kannst du am PC …?“). Für die vollständigen Funktionen des Handys funktionen_lesen.",
            new JSONObjekt { ["type"] = "object", ["properties"] = new JSONObjekt() }),
            _ => Task.FromResult(new WerkzeugErgebnis(Pfade.Mitgeliefert("funktionen") + "\n\n" + Pfade.Mitgeliefert("jarvis-wissen")))),
    };

    public static IEnumerable<LokalesWerkzeug> AlleLokal => Alle.Concat(Weitere).Concat(WinDialoge.Werkzeuge).Concat(RepoWerkzeuge.Alle).Concat(AgentenKanal.Werkzeuge).Concat(HelferWerkzeuge.Alle);
}

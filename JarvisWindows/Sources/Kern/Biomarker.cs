using System.Globalization;
using System.Text.RegularExpressions;

namespace JarvisWindows;

/// <summary>Eine Messgröße des Biomarker-Bereichs mit Kennung des Handy-Werkzeugs, Anzeige und, wo es eine gibt, Ampel wie am Handy.</summary>
public sealed record BioGroesse(string Id, string Titel, string Gruppe);

/// <summary>Eine Zeile aus biomarker_auswertung: aktueller Wert, Vergleich, Einordnung.</summary>
public sealed record BioWert(string Name, string Quelle, string Wert, string Einordnung, string? SchnittDavor, string? Schnitte, string Richtung, string? Datum);

/// <summary>Ein Verlauf aus biomarker_verlauf: Tageswerte mit Einheit, Schnitt und Spanne.</summary>
public sealed record BioVerlauf(string Name, string Einheit, string Quelle, double? Schnitt, List<(DateTime Tag, double Wert)> Punkte);

/// <summary>Der Biomarker-Bereich am Laptop. Wie das Tablet ein reiner Spiegel des Handys: Das Handy ist der einzige Abrufer bei Whoop, Oura, Waage
/// und Health Connect (Whoop verträgt nur einen); der Laptop liest dessen Auswertung und Verläufe über die Handy-Werkzeuge und hält den letzten Stand
/// auf der Platte, damit der Bereich auch ohne Handy etwas zeigt. „Frische Werte holen“ bittet das Handy um einen Abruf (biomarker_aktualisieren).</summary>
public sealed class BioDaten : Beobachtbar
{
    public static BioDaten Shared { get; } = new();

    public static readonly BioGroesse[] Groessen =
    {
        new("erholung", "Erholung", "Erholung"), new("hrv", "HRV", "Erholung"), new("ruhepuls", "Ruhepuls", "Erholung"), new("vo2max", "VO2max", "Erholung"),
        new("schlafdauer", "Schlafzeit", "Schlaf"), new("schlafperformance", "Schlafperformance", "Schlaf"), new("tiefschlaf_prozent", "Tiefschlaf %", "Schlaf"), new("rem_prozent", "REM %", "Schlaf"),
        new("wach_min", "Wachzeit im Bett", "Schlaf"), new("erholsamer_schlaf_prozent", "Erholsamer Schlaf", "Schlaf"), new("schlafeffizienz", "Schlafeffizienz", "Schlaf"),
        new("schlafkonsistenz", "Schlafregelmäßigkeit", "Schlaf"), new("schlafdefizit_min", "Schlafdefizit", "Schlaf"),
        new("tagesbelastung", "Tagesbelastung", "Aktivität"), new("energie_kcal", "Energie", "Aktivität"), new("schritte", "Schritte", "Aktivität"),
        new("atemfrequenz", "Atemfrequenz", "Körper"), new("hauttemperatur", "Hauttemperatur", "Körper"),
        new("oura_readiness", "Readiness (Oura)", "Oura"), new("oura_schlafscore", "Schlafscore (Oura)", "Oura"), new("oura_resilienz", "Resilienz (Oura)", "Oura"), new("oura_aktivitaet", "Aktivität (Oura)", "Oura"),
        new("gewicht", "Gewicht", "Waage"), new("koerperfett", "Körperfett", "Waage"), new("muskelmasse", "Muskelmasse", "Waage"), new("viszeralfett", "Viszeralfett", "Waage"), new("bmi", "BMI", "Waage"), new("wasseranteil", "Wasseranteil", "Waage"),
    };

    /// <summary>Die Kerngrößen der Startkarte, wie BiomarkerStartKarte am Handy.</summary>
    public static readonly string[] Kern = { "erholung", "hrv", "ruhepuls", "tiefschlaf_prozent", "schlafdauer", "vo2max" };

    private string? _auswertungText, _tagText;
    public string? AuswertungText { get => _auswertungText; private set => Setze(ref _auswertungText, value); }
    /// <summary>Alle Werte eines Tages (biomarker_tag), für den Reiter „Tageswerte“.</summary>
    public string? TagText { get => _tagText; private set => Setze(ref _tagText, value); }
    public string TagDatum { get; private set; } = "";
    public IReadOnlyList<BioWert> Werte { get; private set; } = Array.Empty<BioWert>();
    public IReadOnlyDictionary<string, BioVerlauf> Verlaeufe { get; private set; } = new Dictionary<string, BioVerlauf>();
    /// <summary>Kopfzeilen der Auswertung: Whoop-Stand, letzter Abruf je Quelle, Hinweise.</summary>
    public IReadOnlyList<string> Kopf { get; private set; } = Array.Empty<string>();
    private bool _laedt, _holtFrisch;
    public bool Laedt { get => _laedt; private set => Setze(ref _laedt, value); }
    public bool HoltFrisch { get => _holtFrisch; private set => Setze(ref _holtFrisch, value); }
    private DateTime? _zuletzt;
    public DateTime? Zuletzt { get => _zuletzt; private set => Setze(ref _zuletzt, value); }
    private string? _fehler, _abrufMeldung;
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }
    public string? AbrufMeldung { get => _abrufMeldung; private set => Setze(ref _abrufMeldung, value); }
    private int _stand;
    public int Stand { get => _stand; private set => Setze(ref _stand, value); }
    public int Tage { get; private set; } = Einstellungen.Shared.BiomarkerTage is 7 or 30 or 90 ? Einstellungen.Shared.BiomarkerTage : 30;

    private readonly string datei = Path.Combine(Pfade.Unterstuetzung, "biomarker.json");
    private readonly Dictionary<string, string> verlaufRoh = new();
    /// <summary>Kennung → genauer Name laut Katalog des Handys.</summary>
    private readonly Dictionary<string, string> katalogNamen = new();

    private BioDaten() { LadeSpeicher(); }

    private void LadeSpeicher()
    {
        try
        {
            if (JSON.Objekt(File.ReadAllBytes(datei)) is not { } o) return;
            if (o.Ganz("zeit") is { } z) Zuletzt = DateTimeOffset.FromUnixTimeMilliseconds(z).LocalDateTime;
            if (o.Obj("verlauf") is { } v) foreach (var (k, w) in v) if (w is string s) verlaufRoh[k] = s;
            if (o.Obj("katalog") is { } kn) foreach (var (k, w) in kn) if (w is string s2) katalogNamen[k] = s2;
            Uebernimm(o.Text("auswertung"));
            TagText = o.Text("tag");
            TagDatum = o.Text("tag_datum") ?? "";
        }
        catch { }
    }

    private void Speichern()
    {
        var o = new JSONObjekt
        {
            ["tage"] = (long)Tage, ["zeit"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), ["auswertung"] = AuswertungText, ["tag"] = TagText, ["tag_datum"] = TagDatum,
            ["verlauf"] = verlaufRoh.ToDictionary(kv => kv.Key, kv => (object?)kv.Value),
            ["katalog"] = katalogNamen.ToDictionary(kv => kv.Key, kv => (object?)kv.Value),
        };
        var ziel = datei;
        Task.Run(() => { try { File.WriteAllText(ziel + ".teil", JSON.Text(o)); File.Move(ziel + ".teil", ziel, true); } catch { } });
    }

    /// <summary>Liest Auswertung und Verläufe vom Handy. Ohne erzwingen höchstens alle fünf Minuten (Öffnen des Bereichs).</summary>
    public async Task Laden(bool erzwingen = false, int? tage = null)
    {
        if (Laedt) return;
        if (tage is { } t && t != Tage) { Tage = t; erzwingen = true; if (Einstellungen.Shared.BiomarkerTage != t) Einstellungen.Shared.BiomarkerTage = t; }
        if (!erzwingen && Zuletzt is { } z && (DateTime.Now - z).TotalMinutes < 5 && Werte.Count > 0) return;
        Laedt = true;
        try
        {
            var bis = DateTime.Today;
            var von = bis.AddDays(-(Tage - 1)).ToString("yyyy-MM-dd");
            var auswertung = McpKlient.Shared.Text("biomarker_auswertung", new JSONObjekt { ["tage"] = (long)Tage, ["metriken"] = Groessen.Select(g => (object?)g.Id).ToList() });
            var stuecke = Groessen.Select(g => g.Id).Chunk(5).Select(teil => McpKlient.Shared.Text("biomarker_verlauf", new JSONObjekt
            {
                ["metriken"] = teil.Cast<object?>().ToList(), ["von"] = von, ["bis"] = bis.ToString("yyyy-MM-dd"), ["aufloesung"] = Tage > 120 ? "woche" : "tag",
            })).ToList();
            // Der Katalog nennt je Kennung den genauen Namen, mit dem Auswertung und Verlauf die Größe schreiben.
            var katalog = katalogNamen.Count == 0 ? McpKlient.Shared.Text("biomarker_verlauf", new JSONObjekt()) : Task.FromResult<string?>(null);
            await Task.WhenAll(stuecke.Append(auswertung).Append(katalog));
            if (katalog.Result is { } kt)
                foreach (Match km in Regex.Matches(kt, @"^- ([a-z0-9_]+): (.+?) \[", RegexOptions.Multiline)) katalogNamen[km.Groups[1].Value] = km.Groups[2].Value.Trim();
            var a = auswertung.Result;
            if (a == null && stuecke.All(s => s.Result == null))
            {
                Fehler = "Das Handy-Jarvis ist gerade nicht erreichbar, du siehst den letzten Stand.";
                return;
            }
            Fehler = null;
            var i = 0;
            foreach (var teil in Groessen.Select(g => g.Id).Chunk(5))
            {
                if (stuecke[i].Result is { } r) foreach (var id in teil) verlaufRoh[id] = r;
                i++;
            }
            if (a != null) Uebernimm(a); else BaueVerlaeufe();
            Zuletzt = DateTime.Now;
            Speichern();
        }
        finally { Laedt = false; }
    }

    public async Task TagLaden(string datum)
    {
        var t = await McpKlient.Shared.Text("biomarker_tag", new JSONObjekt { ["datum"] = datum });
        if (t == null) { TagText ??= "Das Handy-Jarvis ist gerade nicht erreichbar."; return; }
        TagDatum = datum;
        TagText = McpKlient.OhneKontextHinweis(t).Trim();
        Speichern();
    }

    /// <summary>Bittet das Handy, jetzt bei den Quellen abzurufen (dauert bis zu einer Minute), und liest danach neu.</summary>
    public async Task FrischHolen()
    {
        if (HoltFrisch) return;
        HoltFrisch = true;
        AbrufMeldung = null;
        try
        {
            var t = await McpKlient.Shared.Text("biomarker_aktualisieren", new JSONObjekt(), sekunden: 120);
            AbrufMeldung = t == null ? "Das Handy hat nicht geantwortet." : McpKlient.OhneKontextHinweis(t).Trim();
            Log("biomarker", "Frische Biomarker beim Handy angefordert", wo: "Biomarker", fehler: t == null ? "keine Antwort" : null);
            await Laden(erzwingen: true);
        }
        finally { HoltFrisch = false; }
    }

    private void Uebernimm(string? text)
    {
        if (text == null) { BaueVerlaeufe(); return; }
        text = McpKlient.OhneKontextHinweis(text).Trim();
        AuswertungText = text;
        var kopf = new List<string>();
        var werte = new List<BioWert>();
        var imKopf = true;
        foreach (var roh in text.Split('\n'))
        {
            var z = roh.Trim();
            if (z.Length == 0) continue;
            if (z.StartsWith("Biomarker-Auswertung", StringComparison.Ordinal)) { imKopf = false; continue; }
            if (imKopf) { kopf.Add(z); continue; }
            var m = Regex.Match(z, @"^- (.+) \(([^()]+)\): ([^;]+);(.*)$");
            if (!m.Success) continue;
            var rest = m.Groups[4].Value;
            var wert = m.Groups[3].Value.Trim();
            string? datum = null;
            var dm = Regex.Match(wert, @"^(.*?)\s*\(Messung vom (\d{4}-\d{2}-\d{2}), [^)]*\)$");
            if (dm.Success) { wert = dm.Groups[1].Value; datum = dm.Groups[2].Value; }
            werte.Add(new BioWert(m.Groups[1].Value.Trim(), m.Groups[2].Value.Trim(), wert,
                Regex.Match(rest, @"→\s*([^;]+)").Groups[1].Value.Trim(),
                Regex.Match(rest, @"Schnitt davor ([^,;]+)") is { Success: true } s ? s.Groups[1].Value.Trim() : null,
                Regex.Match(rest, @"Schnitte 7/30/90 Tage: ([^;]+)") is { Success: true } s2 ? s2.Groups[1].Value.Trim() : null,
                Regex.Match(rest, @"\[höher ist (besser|schlechter|neutral)\]") is { Success: true } r ? r.Groups[1].Value : "neutral", datum));
        }
        Kopf = kopf;
        Werte = werte;
        BaueVerlaeufe();
    }

    private void BaueVerlaeufe()
    {
        var v = new Dictionary<string, BioVerlauf>();
        foreach (var text in verlaufRoh.Values.Distinct())
            foreach (var b in LiesVerlauf(text)) v[b.Name] = b;
        Verlaeufe = v;
        Stand++;
    }

    /// <summary>Blöcke „Name [Einheit, Quelle], N Tage mit Daten, Schnitt X, Spanne a–b:“ und darunter „JJJJ-MM-TT Wert; …“.</summary>
    private static IEnumerable<BioVerlauf> LiesVerlauf(string text)
    {
        var zeilen = text.Split('\n').Select(z => z.Trim()).ToList();
        for (var i = 0; i < zeilen.Count - 1; i++)
        {
            var m = Regex.Match(zeilen[i], @"^(.+?) \[([^,\]]*), ([^\]]+)\](.*):$");
            if (!m.Success) continue;
            var schnitt = Regex.Match(m.Groups[4].Value, @"Schnitt (-?[\d.,]+)") is { Success: true } s ? Zahl(s.Groups[1].Value) : null;
            var punkte = new List<(DateTime, double)>();
            foreach (var teil in zeilen[i + 1].Split(';'))
            {
                var p = Regex.Match(teil.Trim(), @"^(\d{4}-\d{2}-\d{2})(?:\s*\S*)?\s+(-?[\d.,]+)$");
                if (!p.Success) p = Regex.Match(teil.Trim(), @"^(\d{4}-\d{2}-\d{2})\S*\s+(-?[\d.,]+)");
                if (p.Success && DateTime.TryParseExact(p.Groups[1].Value, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var tag) && Zahl(p.Groups[2].Value) is { } w)
                    punkte.Add((tag, w));
            }
            yield return new BioVerlauf(m.Groups[1].Value.Trim(), m.Groups[2].Value.Trim(), m.Groups[3].Value.Trim(), schnitt, punkte);
        }
    }

    private static double? Zahl(string t) => double.TryParse(t.Replace(".", "").Replace(',', '.'), NumberStyles.Float, CultureInfo.InvariantCulture, out var d) ? d : null;

    /// <summary>Wert und Verlauf zu einer Kennung: über den Anzeigenamen, den das Handy für beide Werkzeuge gleich schreibt.</summary>
    public (BioWert? Wert, BioVerlauf? Verlauf) Zu(BioGroesse g)
    {
        BioVerlauf? verlauf = null;
        // Der Verlauf wird über den Namen gefunden; den Namen kennt das Handy, nicht der Laptop: erst exakter Treffer in der Auswertung, sonst Teilwort.
        katalogNamen.TryGetValue(g.Id, out var genau);
        bool P(string name) => genau != null ? name.Equals(genau, StringComparison.OrdinalIgnoreCase) : Passt(name, g);
        var wert = Werte.FirstOrDefault(w => P(w.Name));
        if (wert != null) Verlaeufe.TryGetValue(wert.Name, out verlauf);
        verlauf ??= Verlaeufe.Values.FirstOrDefault(v => P(v.Name));
        return (wert, verlauf);
    }

    private static readonly Dictionary<string, string[]> Namen = new()
    {
        ["erholung"] = new[] { "Erholung (Recovery)", "Erholung" }, ["hrv"] = new[] { "HRV" }, ["ruhepuls"] = new[] { "Ruhepuls" }, ["vo2max"] = new[] { "VO2max" },
        ["schlafdauer"] = new[] { "Schlafzeit", "Schlafdauer" }, ["schlafperformance"] = new[] { "Schlafperformance" }, ["tiefschlaf_prozent"] = new[] { "Tiefschlaf-Anteil" },
        ["rem_prozent"] = new[] { "REM-Anteil" }, ["wach_min"] = new[] { "Wachzeit im Bett" }, ["erholsamer_schlaf_prozent"] = new[] { "Erholsamer Schlaf (Tief + REM)", "Erholsamer Schlaf" },
        ["schlafeffizienz"] = new[] { "Schlafeffizienz" }, ["schlafkonsistenz"] = new[] { "Schlafregelmäßigkeit" }, ["schlafdefizit_min"] = new[] { "Schlafdefizit" },
        ["tagesbelastung"] = new[] { "Tagesbelastung (Strain)", "Tagesbelastung" }, ["energie_kcal"] = new[] { "Energieumsatz" }, ["schritte"] = new[] { "Schritte" },
        ["atemfrequenz"] = new[] { "Atemfrequenz" }, ["hauttemperatur"] = new[] { "Hauttemperatur" },
        ["oura_readiness"] = new[] { "Readiness" }, ["oura_schlafscore"] = new[] { "Schlafscore" }, ["oura_resilienz"] = new[] { "Resilienz" }, ["oura_aktivitaet"] = new[] { "Aktivitätsscore", "Aktivität" },
        ["gewicht"] = new[] { "Gewicht" }, ["koerperfett"] = new[] { "Körperfett" }, ["muskelmasse"] = new[] { "Muskelmasse" }, ["viszeralfett"] = new[] { "Viszerales Fett" }, ["bmi"] = new[] { "BMI" }, ["wasseranteil"] = new[] { "Wasseranteil", "Körperwasser" },
    };

    private static bool Passt(string name, BioGroesse g)
    {
        if (!Namen.TryGetValue(g.Id, out var kandidaten)) return name.Equals(g.Titel, StringComparison.OrdinalIgnoreCase);
        return kandidaten.Any(k => name.Equals(k, StringComparison.OrdinalIgnoreCase) || name.StartsWith(k + " (", StringComparison.OrdinalIgnoreCase));
    }

    /// <summary>Reihenfolge der Karten im Bereich: eigene Reihenfolge am Laptop, neue Größen hinten.</summary>
    public static List<string> Reihenfolge()
    {
        var gespeichert = (Vorgaben.Text("biomarker.reihenfolge") ?? "").Split(',', StringSplitOptions.RemoveEmptyEntries).Where(id => Groessen.Any(g => g.Id == id)).ToList();
        return gespeichert.Concat(Groessen.Select(g => g.Id).Where(id => !gespeichert.Contains(id))).ToList();
    }
    public static void ReihenfolgeSpeichern(IEnumerable<string> ids) => Vorgaben.Setze("biomarker.reihenfolge", string.Join(",", ids));
}

/// <summary>Victron-Solarbatterie wie die Startkarte am Handy (VictronStartKarte.kt): BMV-712 und SmartSolar-Laderegler, gelesen über das Handy-Werkzeug
/// batterie_lesen (direkt per Bluetooth am Handy, unterwegs über den Rechner zu Hause). Einmal je App-Start von selbst, sonst per Knopf oder im eingestellten Takt.</summary>
public sealed class SolarDaten : Beobachtbar
{
    public static SolarDaten Shared { get; } = new();
    private readonly string datei = Path.Combine(Pfade.Unterstuetzung, "solarbatterie.json");
    private JSONObjekt? _stand;
    public JSONObjekt? Stand { get => _stand; private set => Setze(ref _stand, value); }
    private bool _laeuft;
    public bool Laeuft { get => _laeuft; private set => Setze(ref _laeuft, value); }
    private string? _fehler;
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }
    public bool AutomatischGelesen { get; set; }

    private SolarDaten()
    {
        try { Stand = JSON.Objekt(File.ReadAllBytes(datei)); } catch { }
    }

    public async Task Lesen()
    {
        if (Laeuft) return;
        Laeuft = true;
        try
        {
            var t = await McpKlient.Shared.Text("batterie_lesen", new JSONObjekt(), sekunden: 60);
            var roh = t == null ? null : McpKlient.OhneKontextHinweis(t).Trim();
            var a = roh?.IndexOf('{') ?? -1;
            var b = roh?.LastIndexOf('}') ?? -1;
            var o = a >= 0 && b > a ? JSON.Objekt(roh![a..(b + 1)]) : null;
            if (o == null || o.Objekte("geraete").Count == 0)
            {
                Fehler = t == null ? "Das Handy-Jarvis ist gerade nicht erreichbar." : (roh is { Length: > 0 } ? roh.Kappe(200) : "Die Anlage hat keine Werte geliefert.");
                return;
            }
            o["gelesen"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            Fehler = null;
            Stand = o;
            var ziel = datei;
            _ = Task.Run(() => { try { File.WriteAllText(ziel, JSON.Text(o)); } catch { } });
        }
        finally { Laeuft = false; }
    }

    public JSONObjekt? Geraet(string art) => Stand?.Objekte("geraete").FirstOrDefault(g => g.Text("art") == art);

    private static string Z(double d, int stellen = 1) => d.ToString("N" + stellen, new CultureInfo("de-DE"));

    /// <summary>Zeilen (Name, Wert) der Batterie, wie VictronFaehigkeit.batterieZeilen.</summary>
    public static List<(string, string)> BatterieZeilen(JSONObjekt b)
    {
        var z = new List<(string, string)>();
        if (b.Zahl("ladezustand_prozent") is { } soc) z.Add(("Ladezustand", Z(soc) + " %"));
        if (b.Zahl("spannung_v") is { } v) z.Add(("Spannung", Z(v, 2) + " V"));
        if (b.Zahl("strom_a") is { } a) z.Add(("Strom", a > 0.05 ? $"lädt mit {Z(a)} A" : a < -0.05 ? $"gibt {Z(-a)} A ab" : "kein nennenswerter Strom"));
        if (b.Zahl("restlaufzeit_min") is { } r) { var m = (int)r; z.Add(("Restlaufzeit", m >= 1440 ? $"etwa {m / 1440} Tage" : $"{m / 60} h {m % 60} min")); }
        if (b.Zahl("verbraucht_ah") is { } ah) z.Add(("Entnommen", Z(-ah) + " Ah"));
        return z;
    }

    /// <summary>Zeilen des Ladereglers, wie VictronFaehigkeit.reglerZeilen.</summary>
    public static List<(string, string)> ReglerZeilen(JSONObjekt l)
    {
        var z = new List<(string, string)>();
        if (l.Zahl("solarleistung_w") is { } w) z.Add(("Solarleistung", $"{(int)w} W"));
        if (l.Zahl("ladestrom_a") is { } a) z.Add(("Ladestrom", Z(a) + " A"));
        if (l.Zahl("ertrag_heute_kwh") is { } k) z.Add(("Ertrag heute", Z(k, 2) + " kWh"));
        if (l.Zahl("spannung_v") is { } v) z.Add(("Batteriespannung", Z(v, 2) + " V"));
        if (l.Text("zustand") is { Length: > 0 } s) z.Add(("Ladephase", s));
        return z;
    }

    /// <summary>„Stand 14:05 Uhr · direkt per Bluetooth (Handy)“ wie alterKurz am Handy.</summary>
    public string StandText
    {
        get
        {
            if (Stand == null) return Fehler ?? "Noch kein Stand";
            var zeit = Stand.Objekte("geraete").Select(g => g.Ganz("zeit") ?? 0).DefaultIfEmpty(0).Max();
            var quelle = Stand.Text("quelle") ?? "";
            if (zeit <= 0) return quelle;
            var uhr = DateTimeOffset.FromUnixTimeSeconds(zeit).LocalDateTime;
            var text = (DateTime.Now - uhr).TotalSeconds < 120 ? "gerade eben" : uhr.Date == DateTime.Today ? uhr.ToString("HH:mm") + " Uhr" : uhr.ToString("dd.MM. HH:mm") + " Uhr";
            return "Stand " + text + (quelle.Length > 0 ? " · " + quelle : "");
        }
    }
}

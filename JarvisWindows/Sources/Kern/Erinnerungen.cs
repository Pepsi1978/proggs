using System.Globalization;
using System.Text.RegularExpressions;

namespace JarvisWindows;

// MARK: - Daten (Dokumente „erinnerung“ und „quittung“ der gemeinsamen Ablage, siehe AblageAbgleich)

/// <summary>Ein eingestellter Erinnerungszeitpunkt: Minuten vor dem Termin; mit Uhrzeit (nur Termine ohne Uhrzeit) Minuten / 1440 Tage vorher um diese Zeit.</summary>
public sealed record Ausloesung(string Id, int Minuten, string? Uhrzeit = null, long Angelegt = 0);

/// <summary>Eine einzelne Auslösung einer Erinnerung. Die Kennung trägt die Zeit: Wird der Termin verschoben, ist es eine neue Auslösung.</summary>
public sealed record Faellig(Erinnerung Erinnerung, string Kennung, long Zeit, Ausloesung? Ausloesung, string Anzeige);

/// <summary>Bestätigungen stehen als eigene Dokumente neben den Erinnerungen und kommen nur dazu: Keine Bearbeitung auf einem anderen Gerät kann sie überschreiben.</summary>
public sealed record Quittung(string Id, long Zeit, string Von, string Erinnerung);

/// <summary>Eine Erinnerung. Zeile = Listenzeile (etwa sechs Wörter), Kurz = Kurzfassung zum Vorlesen, Text = ausführlicher Inhalt.
/// Uhrzeit null = Frank hat keine genannt; dann löst sie um OhneUhrzeitUm aus, angezeigt wird aber nur das Datum.</summary>
public sealed record Erinnerung
{
    public const string FormMeldung = "meldung";
    public const string FormWecker = "wecker";

    public string Id { get; init; } = "";
    public bool Allgemein { get; init; }
    public string Zeile { get; init; } = "";
    public string Kurz { get; init; } = "";
    public string Text { get; init; } = "";
    public string? Datum { get; init; }
    public string? Uhrzeit { get; init; }
    public string OhneUhrzeitUm { get; init; } = "10:00";
    public string Form { get; init; } = FormMeldung;
    public IReadOnlyList<Ausloesung> Ausloesungen { get; init; } = Array.Empty<Ausloesung>();
    public long Erledigt { get; init; }
    public long HinweisUm { get; init; }
    public long ZuletztErwaehnt { get; init; }
    public string Quelle { get; init; } = "";
    public long Erstellt { get; init; }
    public long Geaendert { get; init; }

    public bool Wecker => Form == FormWecker;

    public DateOnly? Tag() => Datum != null && DateOnly.TryParseExact(Datum, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var d) ? d : null;

    /// <summary>Der Termin selbst, zum Ordnen und Rechnen; bei reinem Datum die Uhrzeit, zu der er auslöst.</summary>
    public long? Termin() => Tag() is { } d ? ErinnerungZeit.Ms(d.ToDateTime(ErinnerungZeit.Uhr(Uhrzeit ?? OhneUhrzeitUm))) : null;

    public long? ZeitVon(Ausloesung a)
    {
        if (Tag() is not { } d) return null;
        if (a.Uhrzeit != null) return ErinnerungZeit.Ms(d.AddDays(-(a.Minuten / 1440)).ToDateTime(ErinnerungZeit.Uhr(a.Uhrzeit)));
        return ErinnerungZeit.Ms(d.ToDateTime(ErinnerungZeit.Uhr(Uhrzeit ?? OhneUhrzeitUm)).AddMinutes(-a.Minuten));
    }

    /// <summary>Alle Auslösungen, aufsteigend. Rechnet auf allen Geräten gleich: Die Kennung ist Teil des Abgleichs.</summary>
    public List<Faellig> Termine()
    {
        if (Allgemein)
            return HinweisUm > 0 ? new List<Faellig> { new(this, $"{Id}_h_{HinweisUm / 60_000}", HinweisUm, null, "Hinweis von Jarvis") } : new List<Faellig>();
        var r = new List<Faellig>();
        foreach (var a in Ausloesungen)
            if (ZeitVon(a) is { } z) r.Add(new Faellig(this, $"{Id}_{a.Id}_{z / 60_000}", z, a, VorlaufText(a)));
        return r.OrderBy(f => f.Zeit).ToList();
    }

    private static string TageText(int n) => n == 1 ? "1 Tag" : $"{n} Tage";
    private static string StundenText(int n) => n == 1 ? "1 Stunde" : $"{n} Stunden";

    public string VorlaufText(Ausloesung a)
    {
        if (a.Uhrzeit != null) return (a.Minuten == 0 ? "Am Termintag" : TageText(a.Minuten / 1440) + " vorher") + $" um {a.Uhrzeit} Uhr";
        if (a.Minuten == 0) return Uhrzeit != null ? "Zum Termin" : "Am Termintag";
        if (a.Minuten % 1440 == 0) return TageText(a.Minuten / 1440) + " vorher";
        if (a.Minuten % 60 == 0) return StundenText(a.Minuten / 60) + " vorher";
        return a.Minuten == 1 ? "1 Minute vorher" : $"{a.Minuten} Minuten vorher";
    }

    /// <summary>Verschobener Termin oder wieder geöffnet: Was jetzt schon vorbei ist, meldet sich nicht nachträglich; Künftiges bleibt.</summary>
    public Erinnerung NachAenderung(Erinnerung alt, long zeit)
    {
        var wiederOffen = alt.Erledigt != 0 && Erledigt == 0;
        if (alt.Termin() == Termin() && !wiederOffen) return this;
        return this with { Ausloesungen = Ausloesungen.Select(a => a with { Angelegt = zeit }).ToList(), HinweisUm = wiederOffen ? 0 : HinweisUm };
    }

    /// <summary>Fällt dieser Zeitpunkt auf einen schon eingestellten? Dann gäbe es zwei Meldungen in derselben Minute.</summary>
    public bool Doppelt(Ausloesung a)
    {
        var z = ZeitVon(a);
        return Ausloesungen.Any(x => ZeitVon(x) == z);
    }

    /// <summary>Meldet sich diese Erinnerung noch von selbst? Sonst sagt das Werkzeug es, statt still nichts zu tun.</summary>
    public bool MeldetSichNoch(long jetzt) => Allgemein || Termine().Any(t => t.Zeit > jetzt);

    /// <summary>Erste Zeile des Eintrags: Datum und nur eine ausdrücklich genannte Uhrzeit.</summary>
    public string DatumAnzeige() => Tag() is { } d ? ErinnerungZeit.TagText(d) + (Uhrzeit != null ? $" · {Uhrzeit} Uhr" : "") : "Ohne festen Termin";

    public JSONObjekt AlsJson() => new()
    {
        ["allgemein"] = Allgemein, ["zeile"] = Zeile, ["kurz"] = Kurz, ["text"] = Text, ["datum"] = Datum, ["uhrzeit"] = Uhrzeit, ["ohne_uhrzeit_um"] = OhneUhrzeitUm, ["form"] = Form,
        ["ausloesungen"] = Ausloesungen.Select(a => new JSONObjekt { ["id"] = a.Id, ["minuten"] = a.Minuten, ["uhrzeit"] = a.Uhrzeit, ["angelegt"] = a.Angelegt }).ToList(),
        ["erledigt"] = Erledigt, ["hinweis_um"] = HinweisUm, ["zuletzt_erwaehnt"] = ZuletztErwaehnt, ["quelle"] = Quelle.Trim().Length == 0 ? "Jarvis Handy" : Quelle, ["erstellt"] = Erstellt,
    };

    public static Erinnerung Aus(string id, JSONObjekt w, long geaendert)
    {
        static string? Nullbar(JSONObjekt o, string k) => o.Text(k) is { } s && s.Trim().Length > 0 ? s : null;
        var ausl = new List<Ausloesung>();
        foreach (var a in w.Objekte("ausloesungen"))
            if (a.Text("id") is { Length: > 0 } aid)
                ausl.Add(new Ausloesung(aid, (int)AblageAbgleich.Zahl(a.GetValueOrDefault("minuten")), Nullbar(a, "uhrzeit"), AblageAbgleich.Zahl(a.GetValueOrDefault("angelegt"))));
        return new Erinnerung
        {
            Id = id, Allgemein = w.Wahr("allgemein") ?? false, Zeile = w.Text("zeile") ?? "", Kurz = w.Text("kurz") ?? "", Text = w.Text("text") ?? "", Datum = Nullbar(w, "datum"), Uhrzeit = Nullbar(w, "uhrzeit"),
            OhneUhrzeitUm = w.Text("ohne_uhrzeit_um") ?? "10:00", Form = w.Text("form") ?? FormMeldung, Ausloesungen = ausl,
            Erledigt = AblageAbgleich.Zahl(w.GetValueOrDefault("erledigt")), HinweisUm = AblageAbgleich.Zahl(w.GetValueOrDefault("hinweis_um")), ZuletztErwaehnt = AblageAbgleich.Zahl(w.GetValueOrDefault("zuletzt_erwaehnt")),
            Quelle = w.Text("quelle") ?? "", Erstellt = AblageAbgleich.Zahl(w.GetValueOrDefault("erstellt")), Geaendert = geaendert,
        };
    }
}

/// <summary>Zeitrechnung wie java.time am Handy: lokale Wanduhrzeit; in der Umstellungslücke um die Lücke später, in der doppelten Stunde die frühere. Sonst wiche die Kennung einer Auslösung zwischen den Geräten ab.</summary>
public static class ErinnerungZeit
{
    private static readonly string[] Wochentage = { "So", "Mo", "Di", "Mi", "Do", "Fr", "Sa" };
    private static readonly CultureInfo Deutsch = CultureInfo.GetCultureInfo("de-DE");

    public static long Jetzt => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    /// <summary>„ · 13.11.2026, 20:15 Uhr“ hinter der Quelle von Prompts und Erinnerungen (wie am Handy). Vor 2000 ist kein echter Zeitpunkt (fehlende Zeiten nummeriert das Handy 1, 2, 3 …): dann leer.</summary>
    public static string Gespeichert(long ms) => ms < 946_684_800_000L ? "" : " · " + DateTimeOffset.FromUnixTimeMilliseconds(ms).LocalDateTime.ToString("dd.MM.yyyy, HH:mm 'Uhr'", Deutsch);

    public static TimeOnly Uhr(string t) => TimeOnly.TryParseExact(t, new[] { "HH:mm", "HH:mm:ss" }, CultureInfo.InvariantCulture, DateTimeStyles.None, out var u) ? u : new TimeOnly(10, 0);

    public static long Ms(DateTime wand)
    {
        var lokal = DateTime.SpecifyKind(wand, DateTimeKind.Unspecified);
        var zone = TimeZoneInfo.Local;
        TimeSpan versatz;
        if (zone.IsInvalidTime(lokal)) versatz = zone.GetUtcOffset(lokal.AddDays(-1));
        else if (zone.IsAmbiguousTime(lokal)) versatz = zone.GetAmbiguousTimeOffsets(lokal).Max();
        else versatz = zone.GetUtcOffset(lokal);
        return new DateTimeOffset(lokal, versatz).ToUnixTimeMilliseconds();
    }

    /// <summary>„Mo, 12. August 2027“.</summary>
    public static string TagText(DateOnly d) => $"{Wochentage[(int)d.DayOfWeek]}, {d.ToString("d. MMMM yyyy", Deutsch)}";

    public static string ZeitText(long ms)
    {
        var t = DateTimeOffset.FromUnixTimeMilliseconds(ms).LocalDateTime;
        return $"{TagText(DateOnly.FromDateTime(t))}, {t.ToString("HH:mm", CultureInfo.InvariantCulture)} Uhr";
    }

    public static string NeueId() => Guid.NewGuid().ToString("N")[..20];
    public static Ausloesung NeueAusloesung(int minuten, string? uhrzeit = null) => new(Guid.NewGuid().ToString("N")[..8], minuten, uhrzeit, Jetzt);

    private static readonly Regex Leer = new(@"\s+", RegexOptions.Compiled);
    private static string[] Woerter(string text) => Leer.Split(text.Trim());

    /// <summary>Listenzeile aus dem Text, wenn keine angegeben ist: die ersten sechs Wörter.</summary>
    public static string ZeileAus(string text) => string.Join(" ", Woerter(text).Take(6)).TrimEnd(',', ';', ':', '.');
    public static string KurzAus(string text)
    {
        var w = Woerter(text);
        return string.Join(" ", w.Take(35)) + (w.Length > 35 ? " …" : "");
    }

    /// <summary>Wie Datum am Handy: JJJJ-MM-TT oder TT.MM.JJJJ.</summary>
    public static DateOnly? DatumAus(string t)
    {
        var s = t.Trim();
        if (DateOnly.TryParseExact(s, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var d)) return d;
        var m = Regex.Match(s, @"(\d{1,2})\.(\d{1,2})\.(\d{2,4})");
        if (!m.Success) return null;
        var j = int.Parse(m.Groups[3].Value, CultureInfo.InvariantCulture);
        if (m.Groups[3].Length == 2) j += 2000;
        try { return new DateOnly(j, int.Parse(m.Groups[2].Value, CultureInfo.InvariantCulture), int.Parse(m.Groups[1].Value, CultureInfo.InvariantCulture)); }
        catch (ArgumentOutOfRangeException) { return null; }
    }

    /// <summary>Wie Mitdenken.uhrzeitMinuten am Handy: „7“, „7:30“, „0730“, „7.30 Uhr“. Ergebnis HH:mm.</summary>
    public static string? UhrzeitAus(string t)
    {
        if (t.Trim().Length == 0) return null;
        var m = Regex.Match(t, @"^\s*(2[0-3]|[01]?\d)(?:[:.]?([0-5]\d))?(?!\d)");
        if (!m.Success) return null;
        var h = int.Parse(m.Groups[1].Value, CultureInfo.InvariantCulture);
        var mi = m.Groups[2].Success ? int.Parse(m.Groups[2].Value, CultureInfo.InvariantCulture) : 0;
        return string.Create(CultureInfo.InvariantCulture, $"{h:00}:{mi:00}");
    }
}

using System.Globalization;
using System.IO;
using System.Media;
using System.Net.Http;
using System.Runtime.InteropServices;
using System.Text.RegularExpressions;
using System.Windows.Threading;

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
        return $"{a.Minuten} Minuten vorher";
    }

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

// MARK: - Zustellung

/// <summary>Lässt fällige Erinnerungen erscheinen, im Hauptfaden wie der Rest. Der Relay kennt das zuletzt benutzte Gerät (Anwesenheit): Ist das ein anderes, bleibt der Laptop still und fragt je Minute nach.
/// Eine Quittung von anderswo, Löschen, Erledigen oder ein verschobener Termin kommen über den Abgleich herein und beenden Banner, Ton und Stimme sofort.</summary>
public sealed class ErinnerungPlaner : Beobachtbar
{
    public static ErinnerungPlaner Shared { get; } = new();

    /// <summary>Älteres, nie Bestätigtes meldet sich nicht mehr von selbst, es bleibt in der Liste stehen.</summary>
    public const long VerpasstNachMs = 7 * 24 * 3_600_000L;

    private int _stand;
    /// <summary>Zählt jede lokale Änderung an Bestätigungen: Offline Bestätigtes steht sonst in keinem Neuaufbau des Abgleichs.</summary>
    public int Stand { get => _stand; private set => Setze(ref _stand, value); }

    private readonly string ordner = Path.Combine(Pfade.Unterstuetzung, "Erinnerungen");
    private string GezeigtDatei => Path.Combine(ordner, "gezeigt.json");
    private string QuittungenDatei => Path.Combine(ordner, "offene-quittungen.json");

    private readonly HashSet<string> gezeigt = new();
    private readonly Dictionary<string, Quittung> offeneQuittungen = new();
    private readonly Dictionary<string, CancellationTokenSource> stimmen = new();
    private readonly HashSet<string> klingelt = new();
    private CancellationTokenSource? sprichtFuer;
    private bool gestartet, pruefLaeuft, nochmal, geplant, sendetQuittungen;
    private DateTime naechsteFrage = DateTime.MinValue;
    private SoundPlayer? ton;
    private DispatcherTimer? notTon;

    private static AblageAbgleich Abgleich => AblageAbgleich.Shared;

    private ErinnerungPlaner() { }

    public void Starten() => Haupt.Post(() =>
    {
        if (gestartet) return;
        gestartet = true;
        Laden();
        Abgleich.PropertyChanged += (_, e) => { if (e.PropertyName == nameof(AblageAbgleich.AufbauStand)) PruefenBald(); };
        var pruefUhr = new DispatcherTimer { Interval = TimeSpan.FromSeconds(20) };
        pruefUhr.Tick += (_, _) => _ = Pruefen();
        pruefUhr.Start();
        var anwesenheitUhr = new DispatcherTimer { Interval = TimeSpan.FromSeconds(60) };
        anwesenheitUhr.Tick += (_, _) => { if (Aktiv()) _ = Anwesenheit(true); };
        anwesenheitUhr.Start();
        _ = Pruefen();
    });

    // MARK: Lesen

    public Quittung? QuittungZu(string kennung) => Abgleich.Quittungen.TryGetValue(kennung, out var q) ? q : offeneQuittungen.GetValueOrDefault(kennung);

    /// <summary>Datierte: das als Nächstes Kommende oben, danach das Vergangene, das Älteste ganz unten. Maßgeblich ist der Termin selbst.</summary>
    public static List<Erinnerung> Datiert(long jetzt)
    {
        var d = Abgleich.Erinnerungen.Where(e => !e.Allgemein).ToList();
        var kommend = d.Where(e => (e.Termin() ?? 0) >= jetzt && e.Erledigt == 0).OrderBy(e => e.Termin());
        var vorbei = d.Where(e => !((e.Termin() ?? 0) >= jetzt && e.Erledigt == 0)).OrderByDescending(e => e.Termin());
        return kommend.Concat(vorbei).ToList();
    }

    public static List<Erinnerung> Allgemeine() => Abgleich.Erinnerungen.Where(e => e.Allgemein).OrderBy(e => e.Erledigt != 0).ThenByDescending(e => e.Erstellt).ToList();

    /// <summary>Wird diese Auslösung noch von selbst gemeldet? Vor dem Anlegen vergangene Zeitpunkte nicht: Sie waren beim Einstellen schon vorbei.</summary>
    public static bool Meldbar(Faellig f, long jetzt) => f.Erinnerung.Erledigt == 0 && jetzt - f.Zeit < VerpasstNachMs && (f.Ausloesung == null || f.Zeit >= f.Ausloesung.Angelegt - 60_000);

    /// <summary>Offene, schon fällige Auslösungen, die noch niemand bestätigt hat.</summary>
    public List<Faellig> Faellige(long jetzt) => Abgleich.Erinnerungen.SelectMany(e => e.Termine()).Where(f => f.Zeit <= jetzt && QuittungZu(f.Kennung) == null && Meldbar(f, jetzt)).OrderBy(f => f.Zeit).ToList();

    // MARK: Bestätigen

    /// <summary>Bestätigt genau eine Auslösung, die übrigen bleiben geplant. Gilt sofort, auch ohne Netz: Die Quittung liegt in einer Datei und geht bei jedem Lauf erneut an den Server.</summary>
    public void Bestaetige(Faellig f)
    {
        if (QuittungZu(f.Kennung) != null) return;
        var q = new Quittung(f.Kennung, ErinnerungZeit.Jetzt, AblageAbgleich.Quelle, f.Erinnerung.Id);
        offeneQuittungen[q.Id] = q;
        SichereQuittungen();
        Verstummen(f.Kennung);
        if (gezeigt.Remove(f.Kennung)) SichereGezeigt();
        Stand += 1;
        Abgleichen(Faellige(ErinnerungZeit.Jetzt));
        _ = SendeOffene();
    }

    private async Task SendeOffene()
    {
        if (sendetQuittungen || offeneQuittungen.Count == 0) return;
        sendetQuittungen = true;
        try
        {
            foreach (var q in offeneQuittungen.Values.ToList())
            {
                if (!Abgleich.Quittungen.ContainsKey(q.Id))
                {
                    try { await Abgleich.QuittungSenden(q); }
                    // Auf dem Server liegt schon eine: Ein anderes Gerät war schneller, das Ziel ist erreicht.
                    catch (AblageFehler ex) when (ex.Message == AblageFehler.NeuereFassung.Message) { }
                    catch (Exception ex) { Log("ablage", "Quittung noch nicht angekommen, nächster Lauf versucht es erneut", wo: "Erinnerungen", fehler: ex.Message); break; }
                }
                offeneQuittungen.Remove(q.Id);
                SichereQuittungen();
            }
        }
        finally { sendetQuittungen = false; }
    }

    // MARK: Prüfen

    private async void PruefenBald()
    {
        if (geplant) return;
        geplant = true;
        await Task.Delay(300);
        geplant = false;
        await Pruefen();
    }

    private async Task Pruefen()
    {
        if (pruefLaeuft) { nochmal = true; return; }
        pruefLaeuft = true;
        try
        {
            do
            {
                nochmal = false;
                await Lauf();
            } while (nochmal);
        }
        catch (Exception ex) { Log("fehler", "Erinnerungen prüfen gescheitert", wo: "Erinnerungen", fehler: ex.Message); }
        finally { pruefLaeuft = false; }
    }

    private async Task Lauf()
    {
        _ = SendeOffene();
        // Vor dem ersten Stand wäre alles „gelöscht“: Bereits Gezeigtes käme sonst nach jedem Start neu.
        if (Abgleich.AufbauStand == 0 || !Abgleich.Bereit) return;
        var faellig = Faellige(ErinnerungZeit.Jetzt);
        var offen = faellig.Select(f => f.Kennung).ToHashSet();
        // Anderswo bestätigt, gelöscht, erledigt oder der Termin verschoben: hier sofort Schluss.
        var weg = gezeigt.Where(k => !offen.Contains(k)).ToList();
        foreach (var k in weg) { Verstummen(k); gezeigt.Remove(k); }
        foreach (var k in stimmen.Keys.Concat(klingelt).Where(k => !offen.Contains(k)).Distinct().ToList()) Verstummen(k);
        if (weg.Count > 0) SichereGezeigt();

        if (faellig.Any(f => !gezeigt.Contains(f.Kennung)) && DateTime.Now >= naechsteFrage)
        {
            var (ziel, erreichbar) = await Anwesenheit(Aktiv());
            // Server nicht erreichbar: nur, wenn Frank gerade hier sitzt.
            var hier = erreichbar ? ziel == AblageAbgleich.Quelle : LetzteEingabeSekunden() < 300;
            if (hier)
            {
                // Während der Frage kann eine Bestätigung angekommen sein.
                faellig = Faellige(ErinnerungZeit.Jetzt);
                var neu = faellig.Where(f => !gezeigt.Contains(f.Kennung)).ToList();
                foreach (var f in neu) { gezeigt.Add(f.Kennung); Zeige(f); }
                if (neu.Count > 0) SichereGezeigt();
                naechsteFrage = DateTime.MinValue;
            }
            else naechsteFrage = DateTime.Now.AddSeconds(60);
        }
        Abgleichen(faellig);
    }

    /// <summary>Banner und Wecker folgen dem Stand: Ein Neustart beendet das Klingeln, nicht die Pflicht zum Ausschalten; eine andere Form gilt auch für Laufendes.</summary>
    private void Abgleichen(List<Faellig> faellig)
    {
        var aktiv = faellig.Where(f => gezeigt.Contains(f.Kennung)).ToList();
        foreach (var f in aktiv)
        {
            if (f.Erinnerung.Wecker && !klingelt.Contains(f.Kennung)) Klingeln(f);
            else if (!f.Erinnerung.Wecker && klingelt.Contains(f.Kennung)) Verstummen(f.Kennung);
        }
        ErinnerungFenster.Abgleichen(aktiv);
    }

    private void Zeige(Faellig f)
    {
        var e = f.Erinnerung;
        Log("system", $"Erinnerung fällig: {e.Zeile} ({e.Form})", wo: "Erinnerungen");
        _ = Shell.Mitteilung(e.Zeile, e.Kurz);
        if (e.Wecker) return;   // Der Abgleich danach lässt ihn klingeln.
        var ab = new CancellationTokenSource();
        stimmen[f.Kennung] = ab;
        _ = VorleseLauf(e.Kurz, ab);
    }

    private void Klingeln(Faellig f)
    {
        Verstummen(f.Kennung);
        klingelt.Add(f.Kennung);
        var ab = new CancellationTokenSource();
        stimmen[f.Kennung] = ab;
        TonAbgleichen();
        AppSteuerung.Shared.FensterZeigen();
        _ = KlingelLauf(f, ab);
    }

    private void Verstummen(string kennung)
    {
        if (stimmen.Remove(kennung, out var ab))
        {
            ab.Cancel();
            if (ReferenceEquals(sprichtFuer, ab)) { sprichtFuer = null; Stimme.Shared.Stopp(); }
        }
        klingelt.Remove(kennung);
        TonAbgleichen();
    }

    // MARK: Stimme und Ton

    /// <summary>Ein laufendes Gespräch, Live, eine Aufnahme oder eine andere Ansage wird abgewartet: Sprich würde sie sonst abschneiden.</summary>
    private bool StimmeBelegt() => LiveGespraech.Shared.Aktiv || SprachSchleife.Shared.Zustand != SprachZustand.Aus || Diktat.Shared.Aktiv || Stimme.Shared.Spricht || sprichtFuer != null;

    private async Task Vorlesen(string text, CancellationTokenSource ab)
    {
        while (StimmeBelegt()) await Task.Delay(1000, ab.Token);
        sprichtFuer = ab;
        TonAbgleichen();
        try { await Stimme.Shared.Sprich(text); }
        finally
        {
            if (ReferenceEquals(sprichtFuer, ab)) sprichtFuer = null;
            TonAbgleichen();
        }
    }

    private async Task VorleseLauf(string text, CancellationTokenSource ab)
    {
        try { await Vorlesen(text, ab); }
        catch (OperationCanceledException) { }
        catch (Exception ex) { Log("fehler", "Erinnerung vorlesen gescheitert", wo: "Erinnerungen", fehler: ex.Message); }
    }

    /// <summary>Kein Zeitlimit: Die Kurzfassung kommt alle zwei Minuten, bis ausdrücklich „Aus“ geklickt oder anderswo bestätigt ist.</summary>
    private async Task KlingelLauf(Faellig f, CancellationTokenSource ab)
    {
        try
        {
            while (!ab.IsCancellationRequested)
            {
                var kurz = Abgleich.Erinnerungen.FirstOrDefault(e => e.Id == f.Erinnerung.Id)?.Kurz ?? f.Erinnerung.Kurz;
                await Vorlesen(kurz, ab);
                await Task.Delay(TimeSpan.FromMinutes(2), ab.Token);
            }
        }
        catch (OperationCanceledException) { }
        catch (Exception ex) { Log("fehler", "Wecker-Erinnerung gescheitert", wo: "Erinnerungen", fehler: ex.Message); }
    }

    /// <summary>Der Dauerton läuft, solange etwas klingelt, und pausiert nur für die Ansage.</summary>
    private void TonAbgleichen()
    {
        var soll = klingelt.Count > 0 && sprichtFuer == null;
        if (!soll)
        {
            ton?.Stop();
            ton = null;
            notTon?.Stop();
            notTon = null;
            return;
        }
        if (ton != null || notTon != null) return;
        var medien = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Windows), "Media");
        foreach (var name in new[] { "Alarm01.wav", "Alarm02.wav", "Ring01.wav", "notify.wav" })
        {
            var pfad = Path.Combine(medien, name);
            if (!File.Exists(pfad)) continue;
            try
            {
                var p = new SoundPlayer(pfad);
                p.PlayLooping();
                ton = p;
                return;
            }
            catch (Exception ex) { Log("fehler", "Weckton nicht abspielbar: " + name, wo: "Erinnerungen", fehler: ex.Message); }
        }
        // Ohne Tondatei der Systemton in Schleife.
        notTon = new DispatcherTimer { Interval = TimeSpan.FromSeconds(2) };
        notTon.Tick += (_, _) => SystemSounds.Exclamation.Play();
        notTon.Start();
        SystemSounds.Exclamation.Play();
    }

    // MARK: Anwesenheit

    [StructLayout(LayoutKind.Sequential)]
    private struct LASTINPUTINFO { public uint cbSize; public uint dwTime; }

    [DllImport("user32.dll")]
    private static extern bool GetLastInputInfo(ref LASTINPUTINFO info);

    private static double LetzteEingabeSekunden()
    {
        var i = new LASTINPUTINFO { cbSize = (uint)Marshal.SizeOf<LASTINPUTINFO>() };
        if (!GetLastInputInfo(ref i)) return double.MaxValue;
        return unchecked((uint)Environment.TickCount - i.dwTime) / 1000.0;
    }

    private static bool Aktiv() => LetzteEingabeSekunden() < 120 && !JarvisApp.BildschirmAus;

    /// <summary>Name des Geräts, das jetzt dran ist; unerreichbarer Server oder unlesbare Antwort: nicht erreichbar.</summary>
    private static async Task<(string? Ziel, bool Erreichbar)> Anwesenheit(bool aktiv)
    {
        var basis = AblageAbgleich.AblageBasis;
        if (basis == null) return (null, false);
        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Get, $"{basis}/anwesenheit?geraet={Uri.EscapeDataString(AblageAbgleich.Quelle)}&aktiv={(aktiv ? 1 : 0)}");
            req.Headers.TryAddWithoutValidation("User-Agent", $"JarvisWindows/{AppInfo.Version}");
            var (code, daten) = await GehirnNetz.Anfrage(req, 4);
            if (code != 200) return (null, false);
            return JSON.Objekt(daten)?.Text("ziel") is { Length: > 0 } z ? (z, true) : (null, false);
        }
        catch (Exception) { return (null, false); }
    }

    // MARK: Dateien

    private void Laden()
    {
        Directory.CreateDirectory(ordner);
        try
        {
            if (File.Exists(GezeigtDatei) && JSON.Objekt(File.ReadAllBytes(GezeigtDatei)) is { } g)
                foreach (var k in g.Texte("gezeigt")) gezeigt.Add(k);
        }
        catch (Exception ex) { Log("fehler", "Gezeigte Erinnerungen nicht lesbar", wo: "Erinnerungen", fehler: ex.Message); }
        try
        {
            if (File.Exists(QuittungenDatei) && JSON.Objekt(File.ReadAllBytes(QuittungenDatei)) is { } q)
                foreach (var o in q.Objekte("offen"))
                    if (o.Text("id") is { Length: > 0 } id)
                        offeneQuittungen[id] = new Quittung(id, AblageAbgleich.Zahl(o.GetValueOrDefault("zeit")), o.Text("von") ?? AblageAbgleich.Quelle, o.Text("erinnerung") ?? "");
        }
        catch (Exception ex) { Log("fehler", "Offene Quittungen nicht lesbar", wo: "Erinnerungen", fehler: ex.Message); }
    }

    private void SichereGezeigt()
    {
        try { GehirnHilfe.SchreibeAtomar(GezeigtDatei, JSON.Daten(new JSONObjekt { ["gezeigt"] = gezeigt.ToList() })); }
        catch (Exception ex) { Log("fehler", "Gezeigte Erinnerungen nicht gespeichert", wo: "Erinnerungen", fehler: ex.Message); }
    }

    private void SichereQuittungen()
    {
        var liste = offeneQuittungen.Values.Select(q => new JSONObjekt { ["id"] = q.Id, ["zeit"] = q.Zeit, ["von"] = q.Von, ["erinnerung"] = q.Erinnerung }).ToList();
        try { GehirnHilfe.SchreibeAtomar(QuittungenDatei, JSON.Daten(new JSONObjekt { ["offen"] = liste })); }
        catch (Exception ex) { Log("fehler", "Offene Quittungen nicht gespeichert", wo: "Erinnerungen", fehler: ex.Message); }
    }
}

// MARK: - Werkzeuge des Modells (gleiche Parameter, Aktionen und Antworttexte wie ErinnerungenFaehigkeit am Handy)

public static class ErinnerungWerkzeuge
{
    private static AblageAbgleich Abgleich => AblageAbgleich.Shared;
    private static ErinnerungPlaner Planer => ErinnerungPlaner.Shared;
    private const string OhneUhrzeitVorgabe = "10:00";

    private static string S(JSONObjekt a, string k) => a.GetValueOrDefault(k) switch
    {
        null => "",
        string s => s,
        bool b => b ? "true" : "false",
        IFormattable f => f.ToString(null, CultureInfo.InvariantCulture),
        var x => x.ToString() ?? "",
    };
    private static bool Gesetzt(JSONObjekt a, string k) => S(a, k).Trim().Length > 0;

    private static WerkzeugErgebnis Ok(string t) => new(t);
    private static WerkzeugErgebnis Fehler(string t) => new(t, true);

    private static (Erinnerung? E, string? Fehler) Finde(JSONObjekt a)
    {
        var alle = Abgleich.Erinnerungen;
        if (Gesetzt(a, "id"))
        {
            var e = alle.FirstOrDefault(x => x.Id == S(a, "id"));
            return (e, e == null ? "Diese Erinnerung gibt es nicht." : null);
        }
        var wort = S(a, "suche").Trim().ToLowerInvariant();
        if (wort.Length == 0) return (null, "Es fehlt id oder suche.");
        var treffer = alle.Where(x => x.Zeile.ToLowerInvariant().Contains(wort) || x.Kurz.ToLowerInvariant().Contains(wort) || x.Text.ToLowerInvariant().Contains(wort)).ToList();
        if (treffer.Count == 1) return (treffer[0], null);
        if (treffer.Count == 0) return (null, $"Keine Erinnerung passt zu „{wort}“.");
        return (null, "Mehrere passen, bitte genauer: " + string.Join("; ", treffer.Take(8).Select(x => x.Zeile)));
    }

    private static List<int> VorabAus(string t) => t.Split(',', ';').Select(x => int.TryParse(x.Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var m) && m >= 0 ? (int?)m : null).Where(x => x != null).Select(x => x!.Value).Distinct().ToList();

    private static string Kopf(Erinnerung e) => $"{e.DatumAnzeige()}: {e.Zeile} [id {e.Id}]" + (e.Wecker ? " (Wecker)" : "") + (e.Erledigt != 0 ? " (erledigt)" : "");

    private static string StandZeile(Faellig f, long jetzt)
    {
        var q = Planer.QuittungZu(f.Kennung);
        var stand = q != null ? $"bestätigt auf {q.Von}"
            : f.Zeit > jetzt ? "geplant"
            : !ErinnerungPlaner.Meldbar(f, jetzt) ? "verpasst oder beim Einstellen schon vorbei"
            : "fällig, noch nicht bestätigt";
        return $"- {f.Anzeige}: {ErinnerungZeit.ZeitText(f.Zeit)} — {stand}" + (f.Ausloesung != null ? $" [ausloesung_id {f.Ausloesung.Id}]" : "");
    }

    private static string Voll(Erinnerung e)
    {
        var jetzt = ErinnerungZeit.Jetzt;
        var t = $"{Kopf(e)}\nKurzfassung: {e.Kurz}\n\n{e.Text}";
        if (e.Allgemein)
        {
            t += "\n\nAllgemeine Erinnerung ohne Termin.";
            if (e.ZuletztErwaehnt > 0) t += $" Zuletzt erwähnt: {ErinnerungZeit.ZeitText(e.ZuletztErwaehnt)}.";
        }
        var termine = e.Termine();
        if (termine.Count > 0) t += "\n\nZeitpunkte:\n" + string.Join("\n", termine.Select(x => StandZeile(x, jetzt)));
        return t;
    }

    public static WerkzeugErgebnis Lesen(JSONObjekt a)
    {
        if (Gesetzt(a, "id") || Gesetzt(a, "suche"))
        {
            var (e, f) = Finde(a);
            return e != null ? Ok(Voll(e)) : Fehler(f ?? "");
        }
        var art = Gesetzt(a, "art") ? S(a, "art") : "alle";
        var jetzt = ErinnerungZeit.Jetzt;
        var mit = a.Wahr("mit_erledigten") == true || S(a, "mit_erledigten") == "true";
        var datiert = art == "allgemein" ? new List<Erinnerung>() : ErinnerungPlaner.Datiert(jetzt).Where(e =>
            mit || (e.Erledigt == 0 && (e.Termin() ?? 0) >= jetzt) || e.Termine().Any(t => Planer.QuittungZu(t.Kennung) == null && t.Zeit <= jetzt && ErinnerungPlaner.Meldbar(t, jetzt))).ToList();
        var allgemein = art == "datiert" ? new List<Erinnerung>() : ErinnerungPlaner.Allgemeine().Where(e => mit || e.Erledigt == 0).ToList();
        if (datiert.Count == 0 && allgemein.Count == 0) return Ok("Keine Erinnerungen.");
        var teile = new List<string>();
        if (datiert.Count > 0) teile.Add("Mit Datum:\n" + string.Join("\n", datiert.Select(e => "- " + Kopf(e))));
        if (allgemein.Count > 0) teile.Add("Allgemein:\n" + string.Join("\n", allgemein.Select(e => $"- {e.Zeile} [id {e.Id}]" + (e.Erledigt != 0 ? " (erledigt)" : ""))));
        return Ok(string.Join("\n", teile));
    }

    public static async Task<WerkzeugErgebnis> Speichern(JSONObjekt a)
    {
        var aktion = S(a, "aktion");
        if (aktion == "anlegen") return await Anlegen(a);
        var (e, f) = Finde(a);
        if (e == null) return Fehler(f ?? "");
        switch (aktion)
        {
            case "aendern":
            {
                var datum = e.Tag();
                if (Gesetzt(a, "datum"))
                {
                    datum = ErinnerungZeit.DatumAus(S(a, "datum"));
                    if (datum == null) return Fehler($"„{S(a, "datum")}“ ist kein Datum.");
                }
                var uhrzeit = e.Uhrzeit;
                if (Gesetzt(a, "uhrzeit"))
                {
                    if (S(a, "uhrzeit").Trim().ToLowerInvariant() == "keine") uhrzeit = null;
                    else
                    {
                        uhrzeit = ErinnerungZeit.UhrzeitAus(S(a, "uhrzeit"));
                        if (uhrzeit == null) return Fehler($"„{S(a, "uhrzeit")}“ ist keine Uhrzeit.");
                    }
                }
                var form = S(a, "form") is Erinnerung.FormWecker or Erinnerung.FormMeldung ? S(a, "form") : e.Form;
                var neu = e with
                {
                    Text = S(a, "text").Trim() is { Length: > 0 } t ? t : e.Text,
                    Kurz = S(a, "kurz").Trim() is { Length: > 0 } k ? k : e.Kurz,
                    Zeile = S(a, "zeile").Trim() is { Length: > 0 } z ? z : e.Zeile,
                    Datum = datum?.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture), Uhrzeit = uhrzeit, Form = form, Allgemein = datum == null,
                    Ausloesungen = datum != null && e.Ausloesungen.Count == 0 ? new List<Ausloesung> { ErinnerungZeit.NeueAusloesung(0) } : e.Ausloesungen,
                };
                await Abgleich.ErinnerungSpeichern(neu);
                return Ok($"Geändert: {neu.DatumAnzeige()}, {neu.Zeile}, Form {(neu.Wecker ? "Wecker" : "Benachrichtigung")}. Das ist keine Bestätigung.");
            }
            case "loeschen":
                await Abgleich.ErinnerungLoeschen(e.Id);
                return Ok($"Gelöscht: {e.Zeile}.");
            case "ausloesung_hinzufuegen":
            {
                if (e.Allgemein) return Fehler("Das ist eine allgemeine Erinnerung ohne Termin; dafür gibt es keine Zeitpunkte. Gib ihr erst ein Datum (aendern).");
                string? u = null;
                if (Gesetzt(a, "ausloesung_uhrzeit"))
                {
                    u = ErinnerungZeit.UhrzeitAus(S(a, "ausloesung_uhrzeit"));
                    if (u == null) return Fehler($"Keine Uhrzeit: {S(a, "ausloesung_uhrzeit")}");
                }
                var neu = ErinnerungZeit.NeueAusloesung((int)Math.Clamp(AblageAbgleich.Zahl(a.GetValueOrDefault("minuten")), 0, int.MaxValue), u);
                if (e.ZeitVon(neu) is not { } zeit) return Fehler("Der Termin ist nicht lesbar.");
                if (zeit <= ErinnerungZeit.Jetzt) return Fehler($"Dieser Zeitpunkt ({ErinnerungZeit.ZeitText(zeit)}) ist schon vorbei, nichts eingestellt.");
                await Abgleich.ErinnerungSpeichern(e with { Ausloesungen = e.Ausloesungen.Append(neu).ToList() });
                return Ok($"Zusätzlich erinnere ich dich {e.VorlaufText(neu)}: {ErinnerungZeit.ZeitText(zeit)}.");
            }
            case "ausloesung_entfernen":
            {
                var id = S(a, "ausloesung_id");
                if (e.Ausloesungen.All(x => x.Id != id)) return Fehler("Diesen Zeitpunkt gibt es nicht.");
                await Abgleich.ErinnerungSpeichern(e with { Ausloesungen = e.Ausloesungen.Where(x => x.Id != id).ToList() });
                return Ok("Zeitpunkt entfernt.");
            }
            case "bestaetigen":
            {
                var jetzt = ErinnerungZeit.Jetzt;
                var offen = e.Termine().Where(t => Planer.QuittungZu(t.Kennung) == null).ToList();
                Faellig? ziel;
                if (Gesetzt(a, "ausloesung_id"))
                {
                    ziel = offen.FirstOrDefault(t => t.Ausloesung?.Id == S(a, "ausloesung_id"));
                    if (ziel == null) return Fehler("Dieser Zeitpunkt ist nicht offen.");
                }
                else
                {
                    ziel = offen.FirstOrDefault(t => t.Zeit <= jetzt) ?? offen.FirstOrDefault();
                    if (ziel == null) return Ok("Bei dieser Erinnerung ist nichts offen.");
                }
                Planer.Bestaetige(ziel);
                var rest = offen.Count - 1;
                return Ok($"Bestätigt: {ziel.Anzeige}. " + (rest > 0 ? $"{rest} weitere Zeitpunkte bleiben geplant." : "Weitere Zeitpunkte sind nicht offen."));
            }
            case "erledigt":
                await Abgleich.ErinnerungSpeichern(e with { Erledigt = ErinnerungZeit.Jetzt });
                return Ok($"Als erledigt markiert: {e.Zeile}.");
            case "wieder_offen":
                await Abgleich.ErinnerungSpeichern(e with { Erledigt = 0 });
                return Ok($"Wieder offen: {e.Zeile}.");
            case "erwaehnt":
                await Abgleich.ErinnerungSpeichern(e with { ZuletztErwaehnt = ErinnerungZeit.Jetzt });
                return Ok("Vermerkt.");
            default:
                return Fehler($"Unbekannte aktion „{aktion}“.");
        }
    }

    private static async Task<WerkzeugErgebnis> Anlegen(JSONObjekt a)
    {
        var text = S(a, "text").Trim();
        if (text.Length == 0) return Fehler("Der Inhalt fehlt.");
        DateOnly? datum = null;
        if (Gesetzt(a, "datum"))
        {
            datum = ErinnerungZeit.DatumAus(S(a, "datum"));
            if (datum == null) return Fehler($"„{S(a, "datum")}“ ist kein Datum.");
        }
        string? uhrzeit = null;
        if (datum != null && Gesetzt(a, "uhrzeit"))
        {
            uhrzeit = ErinnerungZeit.UhrzeitAus(S(a, "uhrzeit"));
            if (uhrzeit == null) return Fehler($"„{S(a, "uhrzeit")}“ ist keine Uhrzeit.");
        }
        var vorab = VorabAus(S(a, "vorab_minuten")).Select(m => ErinnerungZeit.NeueAusloesung(m)).ToList();
        var e = Neu(text, S(a, "zeile"), S(a, "kurz"), datum, uhrzeit, S(a, "form") == Erinnerung.FormWecker ? Erinnerung.FormWecker : Erinnerung.FormMeldung, vorab);
        await Abgleich.ErinnerungSpeichern(e);
        return Ok(e.Allgemein
            ? $"Als allgemeine Erinnerung gespeichert: {e.Zeile}. Jarvis meldet sich, wenn es passt."
            : $"Gespeichert für {e.DatumAnzeige()}: {e.Zeile}. Form: {(e.Wecker ? "Wecker" : "Benachrichtigung")}. Zeitpunkte: " + string.Join(", ", e.Ausloesungen.Select(e.VorlaufText)) + ".");
    }

    /// <summary>Eine neue Erinnerung; leere Kurzfassung und Zeile entstehen aus dem Text, ein Termin ohne Zeitpunkt erinnert zum Termin.</summary>
    public static Erinnerung Neu(string text, string zeile, string kurz, DateOnly? datum, string? uhrzeit, string form, IReadOnlyList<Ausloesung>? vorab = null)
    {
        var sauber = text.Trim();
        var jetzt = ErinnerungZeit.Jetzt;
        return new Erinnerung
        {
            Id = ErinnerungZeit.NeueId(), Allgemein = datum == null, Zeile = zeile.Trim().Length > 0 ? zeile.Trim() : ErinnerungZeit.ZeileAus(sauber), Kurz = kurz.Trim().Length > 0 ? kurz.Trim() : ErinnerungZeit.KurzAus(sauber),
            Text = sauber, Datum = datum?.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture), Uhrzeit = datum == null ? null : uhrzeit, OhneUhrzeitUm = OhneUhrzeitVorgabe, Form = form,
            Ausloesungen = datum == null ? Array.Empty<Ausloesung>() : vorab is { Count: > 0 } ? vorab : new List<Ausloesung> { ErinnerungZeit.NeueAusloesung(0) },
            Quelle = AblageAbgleich.Quelle, Erstellt = jetzt, Geaendert = jetzt,
        };
    }
}

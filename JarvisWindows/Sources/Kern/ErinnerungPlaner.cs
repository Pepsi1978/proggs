using System.IO;
using System.Media;
using System.Net.Http;
using System.Runtime.InteropServices;
using System.Windows.Threading;

namespace JarvisWindows;

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
        Abgleich.PropertyChanged += (_, e) => { if (e.PropertyName is nameof(AblageAbgleich.AufbauStand) or nameof(AblageAbgleich.Vollstaendig)) PruefenBald(); };
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
        // Beim allerersten Abgleich erst nach der letzten Seite: Quittungen kommen sonst nach ihren Erinnerungen.
        if (Abgleich.AufbauStand == 0 || !Abgleich.Vollstaendig) return;
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
    private bool StimmeBelegt() => SprachSchleife.Shared.Zustand != SprachZustand.Aus || Diktat.Shared.Aktiv || Stimme.Shared.Spricht || sprichtFuer != null;

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

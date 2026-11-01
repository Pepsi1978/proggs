using System.IO;
using System.Net.Http;
using System.Collections.ObjectModel;
using System.Globalization;

namespace JarvisWindows;

public sealed class Schritt : Beobachtbar
{
    public Guid Id { get; }
    public string Werkzeug { get; }
    public string Kurz { get; }
    private bool? _ok;
    /// <summary>null = läuft noch.</summary>
    public bool? Ok { get => _ok; set => Setze(ref _ok, value); }
    public bool AmHandy { get; }

    public Schritt(string werkzeug, string kurz, bool amHandy, bool? ok = null, Guid? id = null)
    {
        Id = id ?? Guid.NewGuid();
        Werkzeug = werkzeug;
        Kurz = kurz;
        AmHandy = amHandy;
        _ok = ok;
    }

    internal JSONObjekt Daten() => new()
    {
        ["id"] = Id.ToString(), ["werkzeug"] = Werkzeug, ["kurz"] = Kurz, ["ok"] = Ok, ["amHandy"] = AmHandy,
    };

    internal static Schritt Aus(JSONObjekt o) => new(o.Text("werkzeug") ?? "", o.Text("kurz") ?? "", o.Wahr("amHandy") ?? false, o.Wahr("ok"),
        Guid.TryParse(o.Text("id"), out var i) ? i : null);
}

/// <summary>Nachricht.Rolle der Mac-Fassung; als eigener Typ, weil C# die Eigenschaft Rolle sonst nicht zulässt.</summary>
public enum Rolle { Frank, Jarvis, Hinweis }

public sealed class Nachricht : Beobachtbar
{
    public Guid Id { get; }
    public Rolle Rolle { get; }
    private string _text;
    public string Text { get => _text; set => Setze(ref _text, value); }
    public DateTime Zeit { get; }
    public ObservableCollection<Schritt> Schritte { get; }
    private bool _gesprochen;
    public bool Gesprochen { get => _gesprochen; set => Setze(ref _gesprochen, value); }
    private byte[]? _bild;
    public byte[]? Bild { get => _bild; set => Setze(ref _bild, value); }
    private bool _laeuft;
    public bool Laeuft { get => _laeuft; set => Setze(ref _laeuft, value); }

    public Nachricht(Rolle rolle, string text, bool gesprochen = false, byte[]? bild = null, bool laeuft = false, DateTime? zeit = null, Guid? id = null, IEnumerable<Schritt>? schritte = null)
    {
        Id = id ?? Guid.NewGuid();
        Rolle = rolle;
        _text = text;
        _gesprochen = gesprochen;
        _bild = bild;
        _laeuft = laeuft;
        Zeit = zeit ?? DateTime.Now;
        Schritte = schritte != null ? new ObservableCollection<Schritt>(schritte) : new ObservableCollection<Schritt>();
    }

    /// <summary>Stand zum Speichern: wandert in den Hintergrund, während das Original sich weiter ändert.</summary>
    internal NachrichtStand Stand() => new(Id, Rolle, Text, Zeit, Schritte.Select(s => s.Daten()).ToList(), Gesprochen, Bild, Laeuft);

    internal static Nachricht Aus(JSONObjekt o)
    {
        byte[]? bild = null;
        if (o.Text("bild") is { } b)
        {
            try { bild = Convert.FromBase64String(b); } catch (FormatException) { }
        }
        var rolle = Enum.TryParse<Rolle>(o.Text("rolle"), true, out var r) ? r : Rolle.Hinweis;
        return new Nachricht(rolle, o.Text("text") ?? "", o.Wahr("gesprochen") ?? false, bild, false, GespraechsDatei.Zeit(o.Text("zeit")),
            Guid.TryParse(o.Text("id"), out var i) ? i : null, o.Objekte("schritte").Select(Schritt.Aus));
    }
}

internal sealed record NachrichtStand(Guid Id, Rolle Rolle, string Text, DateTime Zeit, List<JSONObjekt> Schritte, bool Gesprochen, byte[]? Bild, bool Laeuft)
{
    public JSONObjekt Daten() => new()
    {
        ["id"] = Id.ToString(), ["rolle"] = Rolle.ToString().ToLowerInvariant(), ["text"] = Text, ["zeit"] = GespraechsDatei.Zeit(Zeit),
        ["schritte"] = Schritte, ["gesprochen"] = Gesprochen, ["bild"] = Bild != null ? Convert.ToBase64String(Bild) : null, ["laeuft"] = Laeuft,
    };
}

/// <summary>Eine eingelesene Datei: Das teure JSON läuft im Hintergrund, angelegt wird das Gespräch auf dem Hauptfaden.</summary>
internal sealed record GeleseneDatei(Guid Id, string Titel, DateTime Erstellt, DateTime Geaendert, List<JSONObjekt> Nachrichten, List<JSONObjekt> Verlauf,
                                     string Kontext, int GelerntBis, string? Projekt, string? AuftragId, List<string> GesendeteNachtraege);

internal static class GespraechsDatei
{
    public static string Zeit(DateTime d) => d.ToString("o", CultureInfo.InvariantCulture);
    public static DateTime Zeit(string? s) => DateTime.TryParse(s, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind, out var d) ? d : DateTime.Now;
}

public sealed class Gespraech : Beobachtbar
{
    public Guid Id { get; }
    private string _titel;
    public string Titel { get => _titel; set => Setze(ref _titel, value); }
    private DateTime _erstellt;
    public DateTime Erstellt { get => _erstellt; set => Setze(ref _erstellt, value); }
    private DateTime _geaendert;
    public DateTime Geaendert { get => _geaendert; set => Setze(ref _geaendert, value); }
    public ObservableCollection<Nachricht> Nachrichten { get; } = new();
    /// <summary>Verlauf im Messages-Format (kanonisch), wie er an das Modell geht. Einträge nie ändern, nur ersetzen: Hintergrundfäden lesen sie.</summary>
    public List<JSONObjekt> Verlauf { get; set; } = new();
    private string _kontext = "";
    /// <summary>Live-Gedächtnis des Handys zum Gesprächsbeginn (bleibt im Gespräch fest, damit der Prompt-Cache trägt).</summary>
    public string Kontext { get => _kontext; set => Setze(ref _kontext, value); }
    private bool _arbeitet;
    public bool Arbeitet { get => _arbeitet; set => Setze(ref _arbeitet, value); }
    /// <summary>Bis zu dieser Nachrichtenzahl hat „Lernen am Laptop“ das Gespräch schon ausgewertet.</summary>
    public int GelerntBis { get; set; }
    private string? _projekt;
    /// <summary>Projekt, zu dem das Gespräch gehört (null = normales Gespräch).</summary>
    public string? Projekt { get => _projekt; set => Setze(ref _projekt, value); }
    private string? _auftragId;
    /// <summary>Nummer des Programmierauftrags, der aus diesem Gespräch gestartet wurde: das Ziel für „Nachtrag“.</summary>
    public string? AuftragId { get => _auftragId; set => Setze(ref _auftragId, value); }
    /// <summary>Per Knopf gesendete Nachträge, die das Modell noch nicht kennt; sie gehen mit der nächsten Frage mit.</summary>
    public List<string> GesendeteNachtraege { get; set; } = new();
    private bool _istProgrammierung;
    /// <summary>Gespräch mit Programmierauftrag – steht im Leistenreiter „Programmierung“. Gemerkt statt gerechnet: Die Seitenleiste
    /// liest es für alle Gespräche und würde sonst bei jedem gestreamten Textstück neu zeichnen.</summary>
    public bool IstProgrammierung { get => _istProgrammierung; private set => Setze(ref _istProgrammierung, value); }
    /// <summary>Einmal gelöscht, schreibt auch eine noch laufende Antwort die Datei nicht wieder.</summary>
    public bool Geloescht { get; set; }

    public Gespraech(Guid? id = null, string titel = "Neues Gespräch")
    {
        Id = id ?? Guid.NewGuid();
        _titel = titel;
        _erstellt = DateTime.Now;
        _geaendert = DateTime.Now;
    }

    private static readonly HashSet<string> ProgrammierWerkzeuge = new() { "programmieren", "openlauncher_sitzung", "programmier_nachtrag", "programmierer_sprechen" };

    public void ProgrammierungPruefen()
    {
        IstProgrammierung = Nachrichten.Any(n => n.Schritte.Any(s => ProgrammierWerkzeuge.Contains(s.Werkzeug)));
    }

    public string DateiURL => Path.Combine(Pfade.Gespraeche, Id.ToString().ToUpperInvariant() + ".json");

    // Alle Schreibzugriffe der Reihe nach, damit ein späteres Löschen nie von einem älteren Speichern überholt wird.
    private static readonly object SchreiberSchloss = new();
    private static Task schreiber = Task.CompletedTask;
    internal static void Schreiber(Action arbeit)
    {
        lock (SchreiberSchloss)
            schreiber = schreiber.ContinueWith(_ => { try { arbeit(); } catch (Exception ex) { Log("fehler", "Gespräch nicht geschrieben", wo: "Gespräche", fehler: ex.Message); } },
                CancellationToken.None, TaskContinuationOptions.None, TaskScheduler.Default);
    }
    internal static Task SchreiberLeer() { lock (SchreiberSchloss) return schreiber; }

    // Zählt je Gespräch die Speicherwünsche: Von mehreren wartenden schreibt nur der jüngste, alles davor wäre überholt.
    private static readonly object StandSchloss = new();
    private static readonly Dictionary<Guid, int> StandJe = new();
    private static int StandNeu(Guid id) { lock (StandSchloss) { var n = StandJe.GetValueOrDefault(id) + 1; StandJe[id] = n; return n; } }
    private static bool StandAktuell(Guid id, int nr) { lock (StandSchloss) return StandJe.GetValueOrDefault(id) == nr; }

    /// <summary>Ein Gespräch ohne Inhalt bleibt ein Entwurf: keine Datei und kein Eintrag in der Leiste.</summary>
    public void Speichern()
    {
        if (Haupt.D is { } d && !d.CheckAccess()) { Haupt.Post(Speichern); return; }
        if (Nachrichten.Count == 0 || Geloescht) return;
        Geaendert = DateTime.Now;
        GespraechsSpeicher.Shared.Aufnehmen(this);
        var nachrichten = Nachrichten.Select(n => n.Stand()).ToList();
        var verlauf = new List<JSONObjekt>(Verlauf);
        var nachtraege = new List<string>(GesendeteNachtraege);
        var (id, titel, erstellt, geaendert, kontext, gelerntBis, projekt, auftragId, url) = (Id, Titel, Erstellt, Geaendert, Kontext, GelerntBis, Projekt, AuftragId, DateiURL);
        // Das Gespräch samt Bildern kodieren und schreiben dauert: nicht auf dem Hauptfaden, sonst stocken Oberfläche und Pegel.
        var nr = StandNeu(id);
        Schreiber(() =>
        {
            if (!StandAktuell(id, nr)) return;
            var o = new JSONObjekt
            {
                ["id"] = id.ToString(), ["titel"] = titel, ["erstellt"] = GespraechsDatei.Zeit(erstellt), ["geaendert"] = GespraechsDatei.Zeit(geaendert),
                ["nachrichten"] = nachrichten.Select(n => n.Daten()).ToList(), ["verlauf"] = verlauf, ["kontext"] = kontext, ["gelerntBis"] = gelerntBis,
                ["projekt"] = projekt, ["auftragId"] = auftragId, ["gesendeteNachtraege"] = nachtraege,
            };
            GehirnHilfe.SchreibeAtomar(url, JSON.Daten(o));
        });
    }

    internal Gespraech(GeleseneDatei d)
    {
        Id = d.Id;
        _titel = d.Titel;
        _erstellt = d.Erstellt;
        _geaendert = d.Geaendert;
        foreach (var n in d.Nachrichten) Nachrichten.Add(Nachricht.Aus(n));
        Verlauf = d.Verlauf;
        _kontext = d.Kontext;
        GelerntBis = d.GelerntBis;
        _projekt = d.Projekt;
        _auftragId = d.AuftragId;
        GesendeteNachtraege = d.GesendeteNachtraege;
        ProgrammierungPruefen();
    }

    /// <summary>Leere Altbestände verschwinden dabei samt Datei.</summary>
    internal static GeleseneDatei? Lesen(string url)
    {
        JSONObjekt? o;
        try { o = JSON.Objekt(File.ReadAllBytes(url)); } catch { return null; }
        if (o == null || !Guid.TryParse(o.Text("id"), out var id)) return null;
        var nachrichten = o.Objekte("nachrichten");
        if (nachrichten.Count == 0)
        {
            try { File.Delete(url); } catch { }
            return null;
        }
        return new GeleseneDatei(id, o.Text("titel") ?? "", GespraechsDatei.Zeit(o.Text("erstellt")), GespraechsDatei.Zeit(o.Text("geaendert")), nachrichten, o.Objekte("verlauf"),
            o.Text("kontext") ?? "", (int)(o.Ganz("gelerntBis") ?? 0), o.Text("projekt"), o.Text("auftragId"), o.Texte("gesendeteNachtraege"));
    }
}

public sealed class GespraechsSpeicher : Beobachtbar
{
    public static GespraechsSpeicher Shared { get; } = new();
    public ObservableCollection<Gespraech> Alle { get; } = new();
    private bool _geladen;
    /// <summary>Die gespeicherten Gespräche sind von der Platte da (der Agentenkanal sucht sein Gespräch darin).</summary>
    public bool Geladen { get => _geladen; private set => Setze(ref _geladen, value); }

    private GespraechsSpeicher()
    {
        // Im Hintergrund und parallel: Gespräche mit Bildern würden den Start sonst sichtbar aufhalten.
        Task.Run(() =>
        {
            var gelesen = new List<GeleseneDatei>();
            try
            {
                var dateien = Directory.GetFiles(Pfade.Gespraeche, "*.json");
                Parallel.ForEach(dateien, d =>
                {
                    var g = Gespraech.Lesen(d);
                    if (g != null) lock (gelesen) gelesen.Add(g);
                });
            }
            catch (Exception ex) { Log("fehler", "Gespräche nicht gelesen", wo: "Gespräche", fehler: ex.Message); }
            Haupt.Post(() => Uebernehmen(gelesen));
        });
    }

    private void Uebernehmen(List<GeleseneDatei> gelesen)
    {
        var schonDa = Alle.Select(g => g.Id).ToHashSet();
        var neu = Alle.Concat(gelesen.Where(g => !schonDa.Contains(g.Id)).Select(g => new Gespraech(g))).OrderByDescending(g => g.Geaendert).ToList();
        Alle.Clear();
        foreach (var g in neu) Alle.Add(g);
        Geladen = true;
    }

    /// <summary>Ein Entwurf: In die Liste kommt das Gespräch erst mit seiner ersten Nachricht.</summary>
    public Gespraech Neu() => new();

    public void Aufnehmen(Gespraech g)
    {
        if (!Alle.Any(x => x.Id == g.Id)) Alle.Insert(0, g);
    }

    public void Loeschen(Gespraech g)
    {
        g.Geloescht = true;
        AppSteuerung.Shared.GespraechBeendet(g);
        var da = Alle.FirstOrDefault(x => x.Id == g.Id);
        if (da != null) Alle.Remove(da);
        var url = g.DateiURL;
        Gespraech.Schreiber(() => File.Delete(url));
    }

    public void NachOben(Gespraech g)
    {
        if (g.Geloescht) return;
        var i = Alle.IndexOf(g);
        if (i < 0) Alle.Insert(0, g);
        else if (i > 0) Alle.Move(i, 0);
    }

    /// <summary>Beim Beenden der App: Was noch in der Warteschlange steht, muss auf die Platte.</summary>
    public static void Abschliessen() => Gespraech.SchreiberLeer().Wait(TimeSpan.FromSeconds(10));
}

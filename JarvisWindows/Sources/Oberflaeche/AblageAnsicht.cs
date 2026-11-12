using System.Globalization;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;
using Microsoft.Win32;
using static JarvisWindows.Bau;

namespace JarvisWindows;

// MARK: - Daten (gemeinsame Ablage auf dem Server, siehe AblageAbgleich)

public sealed record AblageDatei
{
    public AblageDatei() { }
    public AblageDatei(string id, string name, string art, string kategorie, string endung, long groesse, string sha, string beschreibung = "")
    {
        Id = id; Name = name; Art = art; Kategorie = kategorie; Endung = endung; Groesse = groesse; Sha = sha; Beschreibung = beschreibung;
    }
    public string Id { get; init; } = "";
    public string Name { get; init; } = "";
    /// <summary>BILD, PDF, TEXT … (Art der Android-App)</summary>
    public string Art { get; init; } = "";
    /// <summary>BILDER, DOKUMENTE, TEXTE …</summary>
    public string Kategorie { get; init; } = "";
    public string Endung { get; init; } = "";
    public long Groesse { get; init; }
    public string Sha { get; init; } = "";
    public string Beschreibung { get; set; } = "";
}

public sealed record AblageEintrag
{
    public AblageEintrag() { }
    public AblageEintrag(string id, string titel, DateTime geaendert, bool hatText, bool recherche, IReadOnlyList<AblageDatei> dateien, string? textSha, long textGroesse = 0, string quelle = "")
    {
        Id = id; Titel = titel; Geaendert = geaendert; HatText = hatText; Recherche = recherche; Dateien = dateien; TextSha = textSha; TextGroesse = textGroesse; Quelle = quelle;
    }
    public string Id { get; init; } = "";
    public string Titel { get; init; } = "";
    public DateTime Geaendert { get; init; }
    public bool HatText { get; init; }
    public bool Recherche { get; init; }
    public IReadOnlyList<AblageDatei> Dateien { get; init; } = Array.Empty<AblageDatei>();
    public string? TextSha { get; init; }
    public long TextGroesse { get; set; }
    /// <summary>Die Jarvis-Version, von der der Eintrag stammt.</summary>
    public string Quelle { get; init; } = "";
}

/// <summary>Reihenfolge der Jarvis-Versionen beim Sortieren nach Quelle; gilt nur auf diesem Gerät.</summary>
public static class QuellenOrdnung
{
    public static readonly string[] Alle = { "Jarvis Handy", "Jarvis Mac OS", AppInfo.Name };
    public static event Action? Geaendert;
    public static string Wert
    {
        get => Vorgaben.Text("ablage.quellen") ?? string.Join(",", Alle);
        set { Vorgaben.Setze("ablage.quellen", value); Geaendert?.Invoke(); }
    }
    public static List<string> Liste(string wert)
    {
        var gewaehlt = wert.Split(',', StringSplitOptions.RemoveEmptyEntries).Where(Alle.Contains).ToList();
        return gewaehlt.Concat(Alle.Where(x => !gewaehlt.Contains(x))).ToList();
    }
    /// <summary>Unbekannte Quellen (eine spätere Version) stehen hinter allen bekannten.</summary>
    public static int Rang(IList<string> ordnung, string quelle) { var i = ordnung.IndexOf(quelle); return i >= 0 ? i : ordnung.Count; }
}

/// <summary>Oben steht, was zuerst angezeigt wird.</summary>
public sealed class QuellenListe : UserControl
{
    public QuellenListe() { Zeichne(); }

    private void Zeichne()
    {
        var liste = QuellenOrdnung.Liste(QuellenOrdnung.Wert);
        var s = V(8, T("Reihenfolge der Quellen", 13, FontWeights.SemiBold));
        for (var i = 0; i < liste.Count; i++)
        {
            var nr = i;
            var name = T($"{i + 1}. {liste[i]}", 13);
            name.Width = 150;
            var hoch = Klein("chevron.up", "Nach oben", () => Tausche(liste, nr, nr - 1));
            var runter = Klein("chevron.down", "Nach unten", () => Tausche(liste, nr, nr + 1));
            hoch.IsEnabled = i != 0;
            runter.IsEnabled = i != liste.Count - 1;
            VAnfuegen(s, H(6, name, hoch, runter), 8);
        }
        s.Margin = new Thickness(14);
        Content = s;
    }

    private void Tausche(List<string> l, int a, int b)
    {
        var n = l.ToList();
        (n[a], n[b]) = (n[b], n[a]);
        QuellenOrdnung.Wert = string.Join(",", n);
        Zeichne();
    }
}

/// <summary>Ein Ordner des Explorers, wie die Filter der Handy-Ablage; PDFs und Recherchen sind eigene Bereiche.</summary>
public enum AblageBereich { Bilder, Pdfs, Prompts, Erinnerung, Listen, Recherchen, Texte, Dokumente, Tabellen, Praesentationen, Audio, Video, Sonstige }

public static class AblageBereichMehr
{
    public static string Titel(this AblageBereich b) => b switch
    {
        AblageBereich.Bilder => "Bilder", AblageBereich.Pdfs => "PDFs", AblageBereich.Prompts => "Prompts", AblageBereich.Erinnerung => "Erinnerung", AblageBereich.Listen => "Listen", AblageBereich.Recherchen => "Recherchen", AblageBereich.Texte => "Texte",
        AblageBereich.Dokumente => "Dokumente", AblageBereich.Tabellen => "Tabellen", AblageBereich.Praesentationen => "Präsentationen", AblageBereich.Audio => "Audio",
        AblageBereich.Video => "Video", _ => "Sonstige",
    };
    public static string Symbol(this AblageBereich b) => b switch
    {
        AblageBereich.Bilder => "photo.fill", AblageBereich.Pdfs => "doc.richtext.fill", AblageBereich.Prompts => "bolt.fill", AblageBereich.Erinnerung => "bell.fill", AblageBereich.Listen => "checklist", AblageBereich.Recherchen => "magnifyingglass.circle.fill",
        AblageBereich.Texte => "doc.text.fill", AblageBereich.Dokumente => "doc.fill", AblageBereich.Tabellen => "tablecells.fill", AblageBereich.Praesentationen => "rectangle.on.rectangle.angled",
        AblageBereich.Audio => "waveform", AblageBereich.Video => "film.fill", _ => "archivebox.fill",
    };
    public static object Farbton(this AblageBereich b) => b switch
    {
        AblageBereich.Bilder or AblageBereich.Video => Farbe.Kupfer,
        AblageBereich.Pdfs or AblageBereich.Audio => Farbe.Warnung,
        AblageBereich.Prompts or AblageBereich.Tabellen or AblageBereich.Listen => Farbe.Gruen,
        AblageBereich.Recherchen or AblageBereich.Praesentationen or AblageBereich.Erinnerung => Farbe.Gold,
        _ => Farbe.Haupt,
    };
}

/// <summary>Was im Explorer als ein Element erscheint: eine Datei oder ein reiner Texteintrag.</summary>
public sealed record AblageElement(AblageEintrag Eintrag, AblageDatei? Datei)
{
    public string Id => Eintrag.Id + "/" + (Datei?.Id ?? "text");
    public string Name => Datei?.Name ?? Eintrag.Titel;
}

public sealed class AblageDaten : Beobachtbar
{
    public AblageAbgleich Quelle { get; } = AblageAbgleich.Shared;
    private int zaehlerStand = -1;
    private Dictionary<AblageBereich, int> zaehler = new();
    private int suchNummer;

    /// <summary>Einmal je Neuaufbau gezählt, nicht bei jedem Neuzeichnen und jedem Tastendruck in der Suche.</summary>
    public int Anzahl(AblageBereich b)
    {
        if (zaehlerStand != Quelle.AufbauStand)
        {
            zaehlerStand = Quelle.AufbauStand;
            zaehler = Enum.GetValues<AblageBereich>().ToDictionary(x => x, x => Elemente(x, "").Count);
        }
        if (b == AblageBereich.Prompts) return Quelle.Prompts.Count;
        if (b == AblageBereich.Erinnerung) return Quelle.Erinnerungen.Count;
        if (b == AblageBereich.Listen) return ListenSpeicher.Shared.OffenGesamt;
        return zaehler.TryGetValue(b, out var n) ? n : 0;
    }
    public IEnumerable<AblageEintrag> Eintraege => Quelle.Eintraege;
    public string? Fehler => Quelle.Fehler;
    private string? _meldung;
    private bool _laedt;
    private HashSet<string>? _treffer;
    public string? Meldung { get => _meldung; set => Setze(ref _meldung, value); }
    public bool Laedt { get => _laedt; set => Setze(ref _laedt, value); }
    public HashSet<string>? Treffer { get => _treffer; set { _treffer = value; Melde(); } }

    /// <summary>Volltextsuche: auch Wörter aus Berichten und Texten, nicht nur Namen.</summary>
    public async Task Suchen(string wort)
    {
        var nr = ++suchNummer;
        var w = wort.Trim();
        if (w.Length == 0) { Treffer = null; return; }
        var r = await Quelle.Suche(w);
        // Eine ältere Suche darf das Ergebnis der neueren nicht überschreiben.
        if (nr == suchNummer) Treffer = new HashSet<string>(r);
    }

    public async Task Laden()
    {
        Laedt = true;
        try { await Quelle.JetztAbgleichen(); } finally { Laedt = false; }
    }

    public List<AblageElement> Elemente(AblageBereich b, string suche)
    {
        var kategorie = b switch
        {
            AblageBereich.Bilder => "BILDER", AblageBereich.Tabellen => "TABELLEN", AblageBereich.Praesentationen => "PRAESENTATIONEN",
            AblageBereich.Audio => "AUDIO", AblageBereich.Video => "VIDEO", AblageBereich.Sonstige => "SONSTIGE", _ => null,
        };
        var r = new List<AblageElement>();
        foreach (var e in Eintraege)
        {
            switch (b)
            {
                case AblageBereich.Prompts or AblageBereich.Erinnerung or AblageBereich.Listen: break;
                case AblageBereich.Recherchen:
                    if (e.Recherche) r.Add(new AblageElement(e, null));
                    break;
                case AblageBereich.Texte:
                    // Auch der Text von Einträgen mit Dateien: Sonst wäre er am PC nicht mehr erreichbar.
                    if (!e.Recherche && e.HatText) r.Add(new AblageElement(e, null));
                    r.AddRange(e.Dateien.Where(d => d.Kategorie == "TEXTE").Select(d => new AblageElement(e, d)));
                    break;
                case AblageBereich.Pdfs:
                    r.AddRange(e.Dateien.Where(d => d.Art == "PDF").Select(d => new AblageElement(e, d)));
                    break;
                case AblageBereich.Dokumente:
                    r.AddRange(e.Dateien.Where(d => d.Kategorie == "DOKUMENTE" && d.Art != "PDF").Select(d => new AblageElement(e, d)));
                    break;
                default:
                    r.AddRange(e.Dateien.Where(d => d.Kategorie == kategorie).Select(d => new AblageElement(e, d)));
                    break;
            }
        }
        var s = suche.Trim().ToLowerInvariant();
        return s.Length == 0 ? r : r.Where(x => x.Name.ToLowerInvariant().Contains(s) || x.Eintrag.Titel.ToLowerInvariant().Contains(s) || Treffer?.Contains(x.Eintrag.Id) == true).ToList();
    }
}

/// <summary>Die Inhalte liegen als Kopie der gemeinsamen Ablage am PC (AblageAbgleich); Vorschauen entstehen hier, Dateien zum Öffnen als benannte Kopie.</summary>
public static class AblageLader
{
    public static readonly string Ordner = Erstelle();
    private static readonly SemaphoreSlim VorschauTakt = new(4);
    private static string Erstelle() { var u = Path.Combine(Pfade.Temp, "Ablage"); Directory.CreateDirectory(u); return u; }

    private static BitmapSource? LiesBild(string pfad)
    {
        try
        {
            var b = new BitmapImage();
            b.BeginInit();
            b.CacheOption = BitmapCacheOption.OnLoad;
            b.CreateOptions = BitmapCreateOptions.IgnoreColorProfile;
            b.UriSource = new Uri(pfad);
            b.EndInit();
            b.Freeze();
            return b;
        }
        catch { return null; }
    }

    /// <summary>Verkleinert, Exif-Drehung angewandt (kCGImageSourceCreateThumbnailWithTransform).</summary>
    private static BitmapSource? Verkleinert(string pfad, int laengste)
    {
        using var s = new FileStream(pfad, FileMode.Open, FileAccess.Read, FileShare.Read);
        var frame = BitmapDecoder.Create(s, BitmapCreateOptions.PreservePixelFormat | BitmapCreateOptions.IgnoreColorProfile, BitmapCacheOption.OnLoad).Frames[0];
        var drehung = 1;
        try { if (frame.Metadata is BitmapMetadata m && m.ContainsQuery("System.Photo.Orientation") && m.GetQuery("System.Photo.Orientation") is ushort o) drehung = o; } catch { }
        var faktor = Math.Min(1.0, (double)laengste / Math.Max(frame.PixelWidth, frame.PixelHeight));
        var gruppe = new TransformGroup();
        gruppe.Children.Add(new ScaleTransform(faktor, faktor));
        switch (drehung)
        {
            case 2: gruppe.Children.Add(new ScaleTransform(-1, 1)); break;
            case 3: gruppe.Children.Add(new RotateTransform(180)); break;
            case 4: gruppe.Children.Add(new ScaleTransform(1, -1)); break;
            case 5: gruppe.Children.Add(new ScaleTransform(-1, 1)); gruppe.Children.Add(new RotateTransform(270)); break;
            case 6: gruppe.Children.Add(new RotateTransform(90)); break;
            case 7: gruppe.Children.Add(new ScaleTransform(-1, 1)); gruppe.Children.Add(new RotateTransform(90)); break;
            case 8: gruppe.Children.Add(new RotateTransform(270)); break;
        }
        var t = new TransformedBitmap(frame, gruppe);
        t.Freeze();
        return t;
    }

    private static async Task<BitmapSource?> PdfSeite(string pfad, int laengste)
    {
        var datei = await Windows.Storage.StorageFile.GetFileFromPathAsync(pfad);
        var doc = await Windows.Data.Pdf.PdfDocument.LoadFromFileAsync(datei);
        if (doc.PageCount == 0) return null;
        using var seite = doc.GetPage(0);
        using var strom = new Windows.Storage.Streams.InMemoryRandomAccessStream();
        var quer = seite.Size.Width >= seite.Size.Height;
        await seite.RenderToStreamAsync(strom, new Windows.Data.Pdf.PdfPageRenderOptions { DestinationWidth = quer ? (uint)laengste : 0, DestinationHeight = quer ? 0 : (uint)laengste });
        strom.Seek(0);
        using var netz = strom.AsStream();
        var kopie = new MemoryStream();
        await netz.CopyToAsync(kopie);
        kopie.Position = 0;
        var frame = BitmapDecoder.Create(kopie, BitmapCreateOptions.None, BitmapCacheOption.OnLoad).Frames[0];
        frame.Freeze();
        return frame;
    }

    private static void SchreibeJpeg(BitmapSource bild, string ziel)
    {
        try
        {
            var kodierer = new JpegBitmapEncoder { QualityLevel = 80 };
            kodierer.Frames.Add(BitmapFrame.Create(bild));
            var teil = ziel + ".teil";
            using (var f = File.Create(teil)) kodierer.Save(f);
            File.Move(teil, ziel, true);
        }
        catch { }
    }

    public static async Task<ImageSource?> Vorschau(AblageElement e, int laengste)
    {
        var d = e.Datei;
        if (d == null) return null;
        // Einmal erzeugt bleibt sie liegen: Sonst dekodierte jede Kachel beim Erscheinen wieder das Original.
        var cache = Path.Combine(Ordner, $"{d.Sha}-{laengste}.jpg");
        await VorschauTakt.WaitAsync();
        try
        {
            // Alles abseits des Hauptfadens, auch Kodieren und Schreiben: Sonst ruckelte das erste Öffnen von „Bilder“.
            var da = await Task.Run(() => File.Exists(cache) ? LiesBild(cache) : null);
            if (da != null) return da;
            string u;
            try { u = Pfad(await AblageAbgleich.Shared.Inhalt(d.Sha)); } catch { return null; }
            var art = d.Art;
            if (art == "SVG") return null;   // WPF zeichnet kein SVG; die große Ansicht zeigt es über WebView2.
            if (art == "ANIMATION")
            {
                if (d.Groesse > 5_000_000) return null;
                return await Task.Run(() => LiesBild(u));
            }
            return await Task.Run(async () =>
            {
                try
                {
                    BitmapSource? bild = art == "PDF" ? await PdfSeite(u, laengste) : art == "BILD" ? Verkleinert(u, laengste) : null;
                    if (bild != null) SchreibeJpeg(bild, cache);
                    return (ImageSource?)bild;
                }
                catch { return null; }
            });
        }
        finally { VorschauTakt.Release(); }
    }

    /// <summary>Die Datei unter ihrem Namen im Zwischenordner, damit andere Programme sie öffnen können.</summary>
    public static async Task<string> Datei(AblageElement e)
    {
        var d = e.Datei ?? throw new InvalidOperationException("Dieses Element ist keine Datei.");
        var quelle = Pfad(await AblageAbgleich.Shared.Inhalt(d.Sha));
        var zielOrdner = Path.Combine(Ordner, d.Sha);
        var name = Path.GetFileName(d.Name.Replace('/', '\\'));
        foreach (var c in Path.GetInvalidFileNameChars()) name = name.Replace(c, '_');
        if (name.Length == 0 || name == "." || name == "..") name = "Datei";
        var url = Path.Combine(zielOrdner, name);
        // Immer frisch aus der Ablage: Ein anderes Programm kann die Kopie verändert haben. Abseits des Hauptfadens: Eine große Datei legte sonst die ganze App still.
        await Task.Run(() =>
        {
            Directory.CreateDirectory(zielOrdner);
            if (File.Exists(url)) { File.SetAttributes(url, FileAttributes.Normal); File.Delete(url); }
            File.Copy(quelle, url);
            // Wie beim Browser-Download: Windows warnt beim Öffnen von Programmen und Skripten aus der Ablage.
            try { File.WriteAllText(url + ":Zone.Identifier", "[ZoneTransfer]\r\nZoneId=3\r\n"); } catch { }
        });
        return url;
    }

    /// <summary>Benannte Kopien nicht ewig liegen lassen.</summary>
    public static void Aufraeumen()
    {
        var grenze = DateTime.Now.AddDays(-7);
        try
        {
            foreach (var u in Directory.EnumerateFileSystemEntries(Ordner))
                try
                {
                    if (File.GetLastWriteTime(u) >= grenze) continue;
                    if (Directory.Exists(u)) Directory.Delete(u, true); else File.Delete(u);
                }
                catch { }
        }
        catch { }
    }

    public static void Vergessen(AblageDatei d)
    {
        if (d.Sha.Length == 0) return;
        try
        {
            foreach (var u in Directory.EnumerateFileSystemEntries(Ordner, d.Sha + "*"))
                try { if (Directory.Exists(u)) Directory.Delete(u, true); else File.Delete(u); } catch { }
        }
        catch { }
    }
}

// MARK: - Explorer

public sealed class AblageAnsicht : UserControl
{
    private readonly AblageDaten daten = new();
    private AblageBereich bereich = AblageBereich.Bilder;
    private string suche = "";
    private string Sortierung { get => Vorgaben.Text("ablage.sortierung") ?? "neueste"; set => Vorgaben.Setze("ablage.sortierung", value); }

    private readonly AdaptivGitter ordner = new() { Minimum = 150, Abstand = 10 };
    private readonly StackPanel status = new();
    private readonly ContentControl inhalt = new();
    private readonly ContentControl lade = new() { VerticalAlignment = VerticalAlignment.Center };
    private readonly DispatcherTimer suchUhr = new() { Interval = TimeSpan.FromSeconds(0.4) };
    private readonly DispatcherTimer listenUhr = new() { Interval = TimeSpan.FromMilliseconds(120) };
    private PromptsAnsicht? prompts;
    private ErinnerungenAnsicht? erinnerungen;
    private ListenAnsicht? listenAnsicht;
    private NeuerTextEintrag? neuerText;

    public AblageAnsicht()
    {
        var feld = Eingabe(null, 13, null, false);
        feld.Background = Brushes.Transparent;
        feld.BorderThickness = new Thickness(0);
        feld.Padding = new Thickness(0);
        var platz = T("In diesem Bereich suchen, auch in Texten …", 13, null, Farbe.Text3, 1);
        platz.IsHitTestVisible = false;
        platz.VerticalAlignment = VerticalAlignment.Center;
        var feldGitter = new Grid();
        feldGitter.Children.Add(feld);
        feldGitter.Children.Add(platz);
        feld.TextChanged += (_, _) =>
        {
            suche = feld.Text;
            platz.Visibility = suche.Length == 0 ? Visibility.Visible : Visibility.Collapsed;
            suchUhr.Stop(); suchUhr.Start();
            listenUhr.Stop(); listenUhr.Start();
        };
        suchUhr.Tick += async (_, _) => { suchUhr.Stop(); await daten.Suchen(suche); };
        listenUhr.Tick += (_, _) => { listenUhr.Stop(); BaueInhalt(); };

        var sortKnopf = (Button?)null;
        sortKnopf = Klick(Sy("arrow.up.arrow.down", 13, Farbe.Haupt), () => SortierMenue(sortKnopf!), "Sortieren");

        var leiste = Flaeche(H(10,
            Sy("magnifyingglass", 13, Farbe.Text3),
            Fuell(feldGitter),
            lade,
            sortKnopf,
            Rund("plus", DateienWaehlen, "Dateien vom PC in die Ablage legen", groesse: 28),
            Rund("arrow.clockwise", async () => await daten.Laden(), "Neu laden", groesse: 28)), Farbe.Glas(0.05), 14, 12, 12);

        var seite = V(18,
            new BereichsKopf("tray.full.fill", "Ablage", "Dieselbe Ablage auf allen Jarvis-Versionen, automatisch abgeglichen."),
            leiste,
            ordner,
            status,
            inhalt);
        seite.Margin = new Thickness(36);
        Content = new ScrollViewer { Content = seite, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled };

        Loaded += (_, _) =>
        {
            daten.PropertyChanged += DatenGeaendert;
            daten.Quelle.PropertyChanged += QuelleGeaendert;
            daten.Quelle.Hochladend.CollectionChanged += HochladendGeaendert;
            QuellenOrdnung.Geaendert += BaueInhalt;
            BaueAlles();
        };
        Unloaded += (_, _) =>
        {
            daten.PropertyChanged -= DatenGeaendert;
            daten.Quelle.PropertyChanged -= QuelleGeaendert;
            daten.Quelle.Hochladend.CollectionChanged -= HochladendGeaendert;
            QuellenOrdnung.Geaendert -= BaueInhalt;
            suchUhr.Stop(); listenUhr.Stop();
        };
    }

    private void DatenGeaendert(object? s, System.ComponentModel.PropertyChangedEventArgs e)
    {
        switch (e.PropertyName)
        {
            case nameof(AblageDaten.Treffer): BaueInhalt(); break;
            case nameof(AblageDaten.Laedt): BaueLade(); BaueInhalt(); break;
            case nameof(AblageDaten.Meldung): BaueStatus(); break;
        }
    }

    private void QuelleGeaendert(object? s, System.ComponentModel.PropertyChangedEventArgs e)
    {
        var q = daten.Quelle;
        switch (e.PropertyName)
        {
            // InhalteStand holt sich jede Kachel ohne Bild selbst.
            case nameof(q.Hochladend) or nameof(q.Ausstehend) or nameof(q.Fehler): BaueStatus(); break;
            case nameof(q.Eintraege) or nameof(q.AufbauStand) or nameof(q.Bereit): BaueOrdner(); BaueInhalt(); BaueStatus(); break;
        }
    }

    private void HochladendGeaendert(object? s, System.Collections.Specialized.NotifyCollectionChangedEventArgs e) => BaueStatus();

    private void BaueAlles() { BaueLade(); BaueOrdner(); BaueStatus(); BaueInhalt(); }
    private void BaueLade() => lade.Content = daten.Laedt ? Drehkreis() : null;

    private void BaueOrdner()
    {
        ordner.Children.Clear();
        foreach (var b in Enum.GetValues<AblageBereich>()) ordner.Children.Add(OrdnerKachel(b));
    }

    private void BaueStatus()
    {
        status.Children.Clear();
        foreach (var n in daten.Quelle.Hochladend)
            VAnfuegen(status, H(8, Drehkreis(), T($"„{n}“ geht in die Ablage …", 12, null, Farbe.Text2), Spacer()), 8);
        if (daten.Quelle.Ausstehend > 0) VAnfuegen(status, T($"Holt noch {daten.Quelle.Ausstehend} Datei(en) vom Server …", 12, null, Farbe.Text3), 8);
        if ((daten.Meldung ?? daten.Fehler) is { } f) VAnfuegen(status, T(f, 13, null, Farbe.Warnung), 8);
        status.Visibility = status.Children.Count == 0 ? Visibility.Collapsed : Visibility.Visible;
    }

    private UIElement OrdnerKachel(AblageBereich b)
    {
        var aktiv = bereich == b;
        var farbe = b.Farbton();
        var bild = Flaeche(Sy(b.Symbol(), 15, farbe, FontWeights.SemiBold), Farbton13(farbe), 10);
        bild.Width = 34; bild.Height = 34;
        var name = T(b.Titel(), 13, FontWeights.SemiBold, null, 1);
        var karte = Flaeche(H(10, bild, Fuell(V(1, name, T(daten.Anzahl(b).ToString(), 11, null, Farbe.Text3)))),
            Farbe.Glas(aktiv ? 0.13 : 0.04), 14, 10, 10, aktiv ? MitDeckkraft(farbe, 0.7) : Farbe.Glas(0.06));
        return Klick(karte, () =>
        {
            if (bereich == b) return;
            bereich = b;
            Log("ablage", $"Bereich {b.Titel()} geöffnet", wo: "Ablage");
            BaueOrdner();
            BaueInhalt();
        });
    }

    private static object Farbton13(object farbe) => MitDeckkraft(farbe, 0.13);
    private static Brush MitDeckkraft(object farbe, double a)
    {
        var k = B(farbe).CloneCurrentValue();
        k.Opacity = a;
        k.Freeze();
        return k;
    }

    private void BaueInhalt()
    {
        // Der Texteintrag behält seinen Zustand: Vor dem Neuaufbau aus der alten Liste lösen, sonst hätte er zwei Eltern.
        if (inhalt.Content is StackPanel alt && neuerText != null) alt.Children.Remove(neuerText);
        if (bereich == AblageBereich.Prompts) { inhalt.Content = prompts ??= new PromptsAnsicht(); return; }
        if (bereich == AblageBereich.Erinnerung) { inhalt.Content = erinnerungen ??= new ErinnerungenAnsicht(); return; }
        if (bereich == AblageBereich.Listen) { inhalt.Content = listenAnsicht ??= new ListenAnsicht(); return; }
        var liste = Sortiert(daten.Elemente(bereich, suche));
        var s = new StackPanel();
        if (bereich == AblageBereich.Texte) VAnfuegen(s, neuerText ??= new NeuerTextEintrag(t => daten.Meldung = t), 18);
        if (liste.Count == 0)
        {
            VAnfuegen(s, Rand(T(daten.Laedt || !daten.Quelle.Bereit ? "Der Abgleich läuft …" : "Hier liegt noch nichts.", 14, null, Farbe.Text2), o: 8), 18);
        }
        else if (bereich == AblageBereich.Bilder)
        {
            var g = new AdaptivGitter { Minimum = 170, Abstand = 12 };
            foreach (var e in liste) g.Children.Add(new BildKachel(e, () => Oeffne_(e)));
            VAnfuegen(s, g, 18);
        }
        else
        {
            var l = new StackPanel();
            foreach (var e in liste) VAnfuegen(l, DateiZeile(e), 6);
            VAnfuegen(s, l, 18);
        }
        inhalt.Content = s;
    }

    private void Oeffne_(AblageElement e) => AblageDetail.Zeige(e);

    private UIElement DateiZeile(AblageElement e)
    {
        var symbol = Sy(bereich.Symbol(), 14, bereich.Farbton());
        symbol.Width = 24;
        var zweite = e.Datei != null && e.Eintrag.Titel != e.Name ? $"{e.Eintrag.Titel} · {e.Eintrag.Quelle}" : e.Eintrag.Quelle;
        var zeile = Flaeche(H(12,
            symbol,
            Fuell(V(2, T(e.Name, 14, FontWeights.Medium, null, 1), T(zweite, 11, null, Farbe.Text3, 1))),
            e.Datei is { } d ? T(Groesse(d.Groesse), 11, null, Farbe.Text3) : null,
            T(Datum(e.Eintrag.Geaendert), 11, null, Farbe.Text3)), Farbe.Glas(0.04), 12, 12, 10);
        return Klick(zeile, () => Oeffne_(e));
    }

    private List<AblageElement> Sortiert(List<AblageElement> l)
    {
        switch (Sortierung)
        {
            case "aelteste": return l.OrderBy(a => a.Eintrag.Geaendert).ToList();
            case "name": return l.OrderBy(a => a.Name, StringComparer.Create(Deutsch, true)).ToList();
            case "groesse": return l.OrderByDescending(a => a.Datei?.Groesse ?? a.Eintrag.TextGroesse).ToList();
            case "quelle":
                // Erst die Versionen in der gewählten Reihenfolge, innerhalb einer Version das Neueste zuerst.
                var o = QuellenOrdnung.Liste(QuellenOrdnung.Wert);
                return l.OrderBy(a => QuellenOrdnung.Rang(o, a.Eintrag.Quelle)).ThenByDescending(a => a.Eintrag.Geaendert).ToList();
            default: return l.OrderByDescending(a => a.Eintrag.Geaendert).ToList();
        }
    }

    private void SortierMenue(FrameworkElement anker)
    {
        MenueZeile Z(string name, string wert) => new(name, () => { Sortierung = wert; BaueInhalt(); }, false, Sortierung == wert);
        OeffneMenue(anker, new[]
        {
            Z("Neueste zuerst", "neueste"), Z("Älteste zuerst", "aelteste"), Z("Name A–Z", "name"), Z("Größte zuerst", "groesse"), Z("Nach Quelle", "quelle"),
            MenueZeile.Trenner,
            new MenueZeile("Reihenfolge der Quellen …", () => OeffnePopup(anker, new QuellenListe())),
        });
    }

    private async void DateienWaehlen()
    {
        var panel = new OpenFileDialog { Multiselect = true };
        if (panel.ShowDialog(AktivesFenster()) != true) return;
        var urls = panel.FileNames;
        daten.Meldung = null;
        foreach (var u in urls)
        {
            try { await AblageAbgleich.Shared.DateiHinzufuegen(u); }
            catch (Exception ex) { daten.Meldung = $"„{Path.GetFileName(u)}“: {ex.Message}"; }
        }
    }

    public static string Groesse(long b)
    {
        // ByteCountFormatter .file: dezimal, wie der Explorer sie nicht rechnet, aber wie am Mac angezeigt.
        if (b < 1000) return b + " Byte";
        string[] einheit = { "KB", "MB", "GB", "TB" };
        double x = b; var i = -1;
        do { x /= 1000; i++; } while (x >= 1000 && i < einheit.Length - 1);
        return x.ToString(i == 0 ? "0" : "0.#", Deutsch) + " " + einheit[i];
    }

    public static string Datum(DateTime d) => d.ToString("d. MMM yyyy", Deutsch);
}

/// <summary>Eigene Ansicht, damit Tippen hier nicht jedes Mal alle Bereiche und die ganze Liste neu auswertet.</summary>
public sealed class NeuerTextEintrag : UserControl
{
    private readonly Action<string?> meldung;
    private bool neuOffen;
    private string neuTitel = "", neuText = "";
    private readonly TextBindung titelB, textB;
    private readonly ContentControl aufklapp = new();
    private readonly ContentControl pfeil = new() { VerticalAlignment = VerticalAlignment.Center };

    public NeuerTextEintrag() : this(_ => { }) { }
    public NeuerTextEintrag(Action<string?> meldung)
    {
        this.meldung = meldung;
        titelB = new TextBindung(() => neuTitel, v => neuTitel = v);
        textB = new TextBindung(() => neuText, v => neuText = v);
        var kopf = Klick(H(0, Titel("square.and.pencil", "Neuer Texteintrag", Farbe.Gruen), Spacer(), pfeil), Umschalten);
        Content = Karte(V(10, kopf, aufklapp), 14);
        Zeichne();
    }

    private void Umschalten() { neuOffen = !neuOffen; Zeichne(); }

    private void Zeichne()
    {
        pfeil.Content = Sy(neuOffen ? "chevron.up" : "chevron.down", 12, Farbe.Text3);
        if (!neuOffen) { aufklapp.Content = null; aufklapp.Visibility = Visibility.Collapsed; return; }
        aufklapp.Visibility = Visibility.Visible;
        var editor = Flaeche(Feld(textB, "", 13, mehrzeilig: true), Farbe.Tiefe(0.25), 10, 6, 6);
        editor.MinHeight = 120;
        aufklapp.Content = V(10,
            Feld(titelB, "Titel"),
            editor,
            H(0, Kapsel("tray.and.arrow.down.fill", "In die Ablage", async () =>
            {
                var t = neuTitel; var x = neuText;
                try
                {
                    await AblageAbgleich.Shared.TextSpeichern(t, x);
                    meldung(null); titelB.Wert = ""; textB.Wert = ""; neuOffen = false; Zeichne();
                }
                catch (Exception ex) { meldung(ex.Message); }
            }, hervorgehoben: true), Spacer()));
    }
}

public sealed class BildKachel : UserControl
{
    private readonly AblageElement element;
    private readonly ContentControl flaeche = new();
    private readonly Border rahmen;
    private ImageSource? bild;
    private bool fertig;
    private bool laedt;

    public BildKachel(AblageElement element, Action aktion)
    {
        this.element = element;
        flaeche.Content = Drehkreis();
        flaeche.HorizontalAlignment = HorizontalAlignment.Center;
        flaeche.VerticalAlignment = VerticalAlignment.Center;
        var innen = new Grid { Height = 140 };
        innen.Children.Add(flaeche);
        rahmen = new Border { Height = 140, CornerRadius = new CornerRadius(12), Background = B(Farbe.Glas(0.05)), BorderBrush = B(Farbe.Glas(0.06)), BorderThickness = new Thickness(1), Child = innen, ClipToBounds = true };
        Content = Klick(V(6,
            rahmen,
            T(element.Name, 11, FontWeights.Medium, Farbe.Text2, 1),
            T(element.Eintrag.Quelle, 10, null, Farbe.Text3, 1)), aktion);
        MouseEnter += (_, _) => rahmen.BorderBrush = B(Farbe.Glas(0.3));
        MouseLeave += (_, _) => rahmen.BorderBrush = B(Farbe.Glas(0.06));
        Loaded += (_, _) =>
        {
            AblageAbgleich.Shared.PropertyChanged += QuelleGeaendert;
            _ = Lade();
        };
        Unloaded += (_, _) => AblageAbgleich.Shared.PropertyChanged -= QuelleGeaendert;
    }

    // Neu bei neuem Inhalt, und solange kein Bild da ist, auch wenn eine Datei eintrifft.
    private void QuelleGeaendert(object? s, System.ComponentModel.PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(AblageAbgleich.InhalteStand) && bild == null) _ = Lade();
    }

    private async Task Lade()
    {
        if (laedt || bild != null) return;
        laedt = true;
        try
        {
            var b = await AblageLader.Vorschau(element, 420);
            if (b != null)
            {
                bild = b;
                flaeche.HorizontalAlignment = HorizontalAlignment.Stretch;
                flaeche.VerticalAlignment = VerticalAlignment.Stretch;
                flaeche.Content = new Image { Source = b, Stretch = Stretch.UniformToFill, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                // Die abgerundeten Ecken müssen das Bild wirklich abschneiden.
                rahmen.Clip = new RectangleGeometry(new Rect(0, 0, ActualWidth, 140), 12, 12);
                SizeChanged += (_, _) => rahmen.Clip = new RectangleGeometry(new Rect(0, 0, ActualWidth, 140), 12, 12);
            }
            else if (!fertig)
            {
                fertig = true;
                flaeche.Content = Sy("photo", 22, Farbe.Text3);
            }
        }
        finally { laedt = false; }
    }
}

// MARK: - Ansicht einer Datei

/// <summary>Das Blatt mit der Vorschau einer Datei, 900 × 660. Zum Zeigen: AblageDetail.Zeige(element).</summary>
public sealed class AblageDetail : UserControl
{
    private readonly AblageElement start;
    private readonly Action geloescht;
    private readonly AblageAbgleich abgleich = AblageAbgleich.Shared;
    private readonly ContentControl vorschau = new();
    private readonly ContentControl kopf = new();
    private readonly TextBlock fehlerText = T("", 13, null, Farbe.Warnung);
    private readonly ContentControl arbeitet = new();
    private Task? dateiArbeit;
    private readonly CancellationTokenSource ende = new();
    private string letzterStand = "\0";
    private WebView2? web;

    /// <summary>Immer der aktuelle Stand: Ändert ein anderes Gerät den Eintrag, zeigt das offene Fenster die neue Fassung; ist er weg, schließt es sich.</summary>
    private AblageElement Element
    {
        get
        {
            var e = abgleich.Eintraege.FirstOrDefault(x => x.Id == start.Eintrag.Id);
            if (e == null) return start;
            return new AblageElement(e, (start.Datei == null ? null : e.Dateien.FirstOrDefault(d => d.Id == start.Datei.Id)) ?? start.Datei);
        }
    }
    private bool Weg
    {
        get
        {
            var e = abgleich.Eintraege.FirstOrDefault(x => x.Id == start.Eintrag.Id);
            if (e == null) return true;
            return start.Datei != null && !e.Dateien.Any(d => d.Id == start.Datei.Id);
        }
    }
    private AblageDatei? DateiJetzt => Element.Datei;

    public static void Zeige(AblageElement e)
    {
        Window? w = null;
        var eigner = AktivesFenster();
        var inhalt = new AblageDetail(e, () => w?.Close());
        w = new Window
        {
            Content = inhalt, Title = e.Name, Width = 900, Height = 660, ResizeMode = ResizeMode.NoResize, WindowStyle = WindowStyle.None, Owner = eigner, ShowInTaskbar = false,
            WindowStartupLocation = eigner == null ? WindowStartupLocation.CenterScreen : WindowStartupLocation.CenterOwner, Background = B(Farbe.Grund2),
        };
        w.KeyDown += (_, a) => { if (a.Key == Key.Escape) w.Close(); };
        w.ShowDialog();
    }

    public AblageDetail(AblageElement start, Action geloescht)
    {
        this.start = start;
        this.geloescht = geloescht;
        fehlerText.Visibility = Visibility.Collapsed;
        var g = new Grid { Margin = new Thickness(22), Background = null };
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        var oben = V(14, kopf, fehlerText, arbeitet);
        oben.Margin = new Thickness(0, 0, 0, 14);
        g.Children.Add(oben);
        Grid.SetRow(vorschau, 1);
        vorschau.HorizontalContentAlignment = HorizontalAlignment.Stretch;
        vorschau.VerticalContentAlignment = VerticalAlignment.Stretch;
        g.Children.Add(vorschau);
        Background = B(Farbe.Grund2);
        Content = g;
        BaueKopf();
        Loaded += async (_, _) =>
        {
            abgleich.PropertyChanged += Geaendert;
            await Laden();
        };
        Unloaded += (_, _) =>
        {
            abgleich.PropertyChanged -= Geaendert;
            ende.Cancel();
            try { web?.Dispose(); } catch { }
        };
    }

    private void Geaendert(object? s, System.ComponentModel.PropertyChangedEventArgs e)
    {
        if (e.PropertyName is not (nameof(AblageAbgleich.Eintraege) or nameof(AblageAbgleich.AufbauStand))) return;
        if (Weg) { Schliessen(); return; }
        BaueKopf();
        var stand = (Element.Eintrag.TextSha ?? "") + (DateiJetzt?.Sha ?? "");
        if (stand != letzterStand) _ = Laden();
    }

    private void Schliessen() => Window.GetWindow(this)?.Close();

    private void BaueKopf()
    {
        var e = Element;
        kopf.Content = H(10,
            Fuell(V(2, T(e.Name, 18, FontWeights.Bold, null, 1), T($"Von {e.Eintrag.Quelle} · {e.Eintrag.Geaendert.ToString("d. MMM yyyy, HH:mm", Deutsch)}", 12, null, Farbe.Text3, 1))),
            DateiJetzt != null ? Kapsel("arrow.up.forward.app", "Öffnen", () => Starte(Oeffnen)) : null,
            DateiJetzt != null ? Kapsel("square.and.arrow.down", "Sichern", () => Starte(Sichern)) : null,
            Kapsel("trash", "Löschen", Frage),
            Rund("xmark", Schliessen, "Schließen", groesse: 30));
    }

    private void ZeigeFehler(string? f)
    {
        fehlerText.Text = f ?? "";
        fehlerText.Visibility = string.IsNullOrEmpty(f) ? Visibility.Collapsed : Visibility.Visible;
    }

    private string LoeschenText
    {
        get
        {
            var e = Element;
            if (DateiJetzt != null) return $"Datei „{e.Name}“ löschen?";
            var n = e.Eintrag.Dateien.Count;
            return n == 0 ? $"„{e.Name}“ löschen?" : $"Den ganzen Eintrag „{e.Name}“ mit {n} Datei(en) löschen?";
        }
    }

    private async void Frage() { if (Bestaetigen(LoeschenText, "", "Löschen")) await Loeschen(); }

    /// <summary>Nur eine Dateiarbeit zur Zeit; sie endet mit dem Schließen des Fensters.</summary>
    private async void Starte(Func<Task> arbeit)
    {
        if (dateiArbeit != null) return;
        arbeitet.Content = Drehkreis();
        dateiArbeit = arbeit();
        try { await dateiArbeit; } finally { dateiArbeit = null; arbeitet.Content = null; }
    }

    private void ZeigeLaedt()
    {
        var mitte = V(10, Drehkreis(), T("Hole die Datei vom Server …", 13, null, Farbe.Text2));
        mitte.HorizontalAlignment = HorizontalAlignment.Center;
        mitte.VerticalAlignment = VerticalAlignment.Center;
        ((FrameworkElement)mitte.Children[0]).HorizontalAlignment = HorizontalAlignment.Center;
        vorschau.Content = mitte;
    }

    private void ZeigeBild(ImageSource b) => vorschau.Content = new Image { Source = b, Stretch = Stretch.Uniform, StretchDirection = StretchDirection.DownOnly };

    private void ZeigeText(string t) => vorschau.Content = new ScrollViewer
    {
        Content = Rand((FrameworkElement)Md(t, 14), 4, 4, 4, 4), VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled,
    };

    private async Task ZeigeWeb(string pfad)
    {
        try
        {
            web?.Dispose();
            web = new WebView2 { DefaultBackgroundColor = System.Drawing.Color.Transparent, CreationProperties = new CoreWebView2CreationProperties { UserDataFolder = Path.Combine(Pfade.Unterstuetzung, "WebView2") } };
            vorschau.Content = web;
            await web.EnsureCoreWebView2Async();
            web.CoreWebView2.Settings.IsStatusBarEnabled = false;
            web.CoreWebView2.Navigate(new Uri(pfad).AbsoluteUri);
        }
        catch (Exception ex)
        {
            ZeigeFehler("Die Vorschau braucht die WebView2-Laufzeit: " + ex.Message);
            ZeigeKeine();
        }
    }

    private void ZeigeKeine()
    {
        var t = T("Für diese Datei gibt es hier keine Vorschau. Öffne oder sichere sie am PC.", 14, null, Farbe.Text2);
        t.HorizontalAlignment = HorizontalAlignment.Center;
        t.VerticalAlignment = VerticalAlignment.Center;
        vorschau.Content = t;
    }

    private async Task Laden()
    {
        var e = Element;
        letzterStand = (e.Eintrag.TextSha ?? "") + (e.Datei?.Sha ?? "");
        ZeigeLaedt();
        Log("ablage", $"Geöffnet: {e.Name}", wo: "Ablage");
        var d = e.Datei;
        if (d == null)
        {
            try { ZeigeText(await abgleich.Text(e.Eintrag)); }
            catch (Exception ex) { ZeigeFehler(ex.Message); ZeigeKeine(); }
            return;
        }
        try
        {
            if (d.Art == "BILD")
            {
                var b = await AblageLader.Vorschau(e, 1600);
                if (ende.IsCancellationRequested) return;
                if (b != null) { ZeigeBild(b); return; }
            }
            if (new[] { "BILD", "PDF", "TEXT", "MARKDOWN", "CODE", "DATEN", "TABELLE_TEXT", "HTML", "ANIMATION", "SVG" }.Contains(d.Art) && d.Groesse <= 40_000_000)
            {
                var u = await AblageLader.Datei(e);
                if (ende.IsCancellationRequested) return;
                switch (d.Art)
                {
                    case "PDF" or "SVG" or "ANIMATION" or "BILD":
                        // PDF über WebView2 (Chromium-Betrachter), SVG und Animationen zeichnet nur der Browser richtig.
                        await ZeigeWeb(u);
                        break;
                    default:
                        ZeigeText(await Task.Run(() => { try { return File.ReadAllText(u); } catch { return "(Kein lesbarer Text)"; } }));
                        break;
                }
                return;
            }
            ZeigeKeine();
        }
        catch (Exception ex) { ZeigeFehler(ex.Message); ZeigeKeine(); }
    }

    private async Task Oeffnen()
    {
        try { Oeffne(await AblageLader.Datei(Element)); }
        catch (Exception ex) { ZeigeFehler(ex.Message); }
    }

    private async Task Sichern()
    {
        try
        {
            var e = Element;
            var u = await AblageLader.Datei(e);
            var panel = new SaveFileDialog { FileName = Path.GetFileName(u), Filter = "Alle Dateien|*.*" };
            if (panel.ShowDialog(Window.GetWindow(this)) != true) return;
            var ziel = panel.FileName;
            // Über eine Zwischendatei und in einem Schritt: Scheitert die Kopie, bleibt eine vorhandene Datei am Ziel erhalten.
            await Task.Run(() =>
            {
                var teil = Path.Combine(Path.GetDirectoryName(ziel)!, "." + Path.GetFileName(ziel) + ".teil");
                File.Copy(u, teil, true);
                File.Move(teil, ziel, true);
            });
            Log("ablage", $"Gesichert: {e.Name} nach {ziel}", wo: "Ablage");
        }
        catch (Exception ex) { ZeigeFehler(ex.Message); }
    }

    private async Task Loeschen()
    {
        var e = Element;
        try { await abgleich.Loeschen(e.Eintrag.Id, e.Datei?.Id); }
        catch (Exception ex) { ZeigeFehler(ex.Message); return; }
        foreach (var d in e.Datei != null ? new[] { e.Datei } : e.Eintrag.Dateien.ToArray()) AblageLader.Vergessen(d);
        geloescht();
    }
}

/// <summary>PDF-Anzeige über WebView2 (Gegenstück zu PDFView).</summary>
public sealed class PDFAnzeige : UserControl
{
    public PDFAnzeige() { }
    public PDFAnzeige(string url)
    {
        var web = new WebView2 { CreationProperties = new CoreWebView2CreationProperties { UserDataFolder = Path.Combine(Pfade.Unterstuetzung, "WebView2") } };
        Content = web;
        Loaded += async (_, _) =>
        {
            try { await web.EnsureCoreWebView2Async(); web.CoreWebView2.Navigate(new Uri(url).AbsoluteUri); }
            catch (Exception ex) { Log("fehler", "PDF-Anzeige gescheitert", wo: "Ablage", fehler: ex.Message); }
        };
        Unloaded += (_, _) => { try { web.Dispose(); } catch { } };
    }
}

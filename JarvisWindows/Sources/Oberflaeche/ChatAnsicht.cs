using System.Collections.Specialized;
using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using static JarvisWindows.Ui;

namespace JarvisWindows;

/// <summary>Die Bild-Hilfen der Mac-Fassung (NSImage-Erweiterung).</summary>
public static class BildHelfer
{
    /// <summary>Ein gespeichertes Bild für die Anzeige; beim Dekodieren schon auf die Anzeigehöhe verkleinert.</summary>
    public static BitmapSource? Bild(byte[] daten, int? hoehe = null)
    {
        try
        {
            var b = new BitmapImage();
            b.BeginInit();
            b.CacheOption = BitmapCacheOption.OnLoad;
            b.StreamSource = new MemoryStream(daten);
            if (hoehe != null) b.DecodePixelHeight = hoehe.Value;
            b.EndInit();
            b.Freeze();
            return b;
        }
        catch { return null; }
    }

    /// <summary>Verkleinert auf höchstens 1600 px und speichert als JPEG – hält Anhänge im Verlauf klein.</summary>
    public static byte[]? JpgDaten(byte[] daten, int grenze = 1600, int qualitaet = 70)
    {
        var quelle = Bild(daten);
        return quelle == null ? null : JpgDaten(quelle, grenze, qualitaet);
    }

    public static byte[]? JpgDaten(BitmapSource quelle, int grenze = 1600, int qualitaet = 70)
    {
        try
        {
            var gross = Math.Max(quelle.PixelWidth, quelle.PixelHeight);
            BitmapSource ziel = quelle;
            if (gross > grenze) { var f = grenze / (double)gross; ziel = new TransformedBitmap(quelle, new ScaleTransform(f, f)); }
            // JPEG kennt keine Transparenz.
            if (ziel.Format.BitsPerPixel == 32 || ziel.Format == PixelFormats.Bgra32) ziel = new FormatConvertedBitmap(ziel, PixelFormats.Bgr24, null, 0);
            var e = new JpegBitmapEncoder { QualityLevel = qualitaet };
            e.Frames.Add(BitmapFrame.Create(ziel));
            using var m = new MemoryStream();
            e.Save(m);
            return m.ToArray();
        }
        catch { return null; }
    }

    public static byte[]? PngDaten(BitmapSource quelle)
    {
        try
        {
            var e = new PngBitmapEncoder();
            e.Frames.Add(BitmapFrame.Create(quelle));
            using var m = new MemoryStream();
            e.Save(m);
            return m.ToArray();
        }
        catch { return null; }
    }
}

public sealed class ChatAnsicht : Grid
{
    private const int Seite = 30;
    private readonly Gespraech gespraech;
    private readonly StackPanel liste = new();
    private readonly Scroller scroller;
    private readonly Decorator leerPlatz = new();
    private readonly Dictionary<Nachricht, NachrichtAnsicht> ansichten = new();
    private int ersteGezeigt;
    private bool angemeldet, scrollWartet, bereit;

    public ChatAnsicht(Gespraech g)
    {
        gespraech = g;
        RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        RowDefinitions.Add(new RowDefinition());
        RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        var kopf = Kopf();
        Children.Add(kopf);
        var innen = new StackPanel();
        leerPlatz.Child = null;
        innen.Children.Add(leerPlatz);
        innen.Children.Add(liste);
        innen.Children.Add(new Border { Height = 8 });
        var aussen = new Border { Child = innen, MaxWidth = 964, Padding = new Thickness(64, 24, 64, 24) };
        scroller = new Scroller(aussen, indikator: true);
        SetRow(scroller, 1);
        Children.Add(scroller);
        var blase = new EingabeBlase(g);
        SetRow(blase, 2);
        Children.Add(blase);
        scroller.Sv.ScrollChanged += (_, _) => AelterePruefen();
        ListeAufbauen();
        AllowDrop = true;
        Background = Farbe.Klar;
        Drop += Abgelegt;
        Loaded += (_, _) =>
        {
            if (angemeldet) return;
            angemeldet = true;
            gespraech.Nachrichten.CollectionChanged += NachrichtenGeaendert;
            Spaeter(() => { scroller.Sv.ScrollToEnd(); Spaeter(() => bereit = true, DispatcherPriority.ContextIdle); }, DispatcherPriority.Loaded);
        };
        Unloaded += (_, _) =>
        {
            angemeldet = false;
            gespraech.Nachrichten.CollectionChanged -= NachrichtenGeaendert;
        };
    }

    private async void Abgelegt(object sender, DragEventArgs e)
    {
        byte[]? jpg = null;
        if (e.Data.GetData(DataFormats.FileDrop) is string[] { Length: > 0 } dateien)
        {
            // Ein großes Foto zu lesen und zu kodieren dauert: nicht auf dem Hauptfaden, sonst stocken Oberfläche und Live.
            var pfad = dateien[0];
            jpg = await Task.Run(() => { try { return BildHelfer.JpgDaten(File.ReadAllBytes(pfad)); } catch { return null; } });
        }
        else if (e.Data.GetData(DataFormats.Bitmap) is BitmapSource b) jpg = BildHelfer.JpgDaten(b);
        if (jpg != null) AppSteuerung.Shared.Anhang = jpg;
    }

    private UIElement Kopf()
    {
        var verlauf = new LinearGradientBrush(Farbe.MitWert(Farbe.Wert(Farbe.Grund), 0.7), Farbe.MitWert(Farbe.Wert(Farbe.Grund), 0), new Point(0, 0), new Point(0, 1));
        verlauf.Freeze();
        var d = new Reaktiv(() =>
        {
            UIElement? laeuft = null;
            if (gespraech.Arbeitet)
                laeuft = Kapsel(H(8, new Spinner(16), Tx("Jarvis arbeitet …", 12, FontWeights.Medium, Farbe.Text2)), Farbe.Glas(0.06), 12, 6);
            return H(12, V(2, Tx(gespraech.Titel, 17, FontWeights.SemiBold, Farbe.Text, zeilen: 1), Tx(gespraech.Erstellt.ToString("d. MMM yyyy, HH:mm", Deutsch), 11, null, Farbe.Text3, zeilen: 1)).Fuell(), laeuft);
        }, new[] { nameof(Gespraech.Titel), nameof(Gespraech.Arbeitet) }, gespraech);
        return new Border { Child = d, Padding = new Thickness(28, 30, 28, 12), Background = verlauf };
    }

    private UIElement Leer()
    {
        var a = Tx("Was kann ich für dich tun?", 22, FontWeights.SemiBold);
        var b = Tx("Ich habe dasselbe Gedächtnis wie auf deinem Handy und kann hier am PC alles für dich erledigen.", 14, null, Farbe.Text2);
        a.TextAlignment = b.TextAlignment = TextAlignment.Center;
        return V(14, new JarvisKugel(0, JarvisKugel.KugelZustand.Ruhe, 120).Mittig(), a, b).Rand(0, 80, 0, 0);
    }

    /// <summary>Nur die letzten Nachrichten sofort, ältere beim Hochscrollen: Ein langes Gespräch öffnet genauso schnell wie ein kurzes.</summary>
    private void ListeAufbauen()
    {
        liste.Children.Clear();
        ansichten.Clear();
        var n = gespraech.Nachrichten;
        ersteGezeigt = Math.Max(0, n.Count - Seite);
        for (var i = ersteGezeigt; i < n.Count; i++) Anfuegen(n[i], liste.Children.Count);
        leerPlatz.Child = n.Count == 0 ? Leer() : null;
    }

    private void Anfuegen(Nachricht n, int stelle)
    {
        var a = new NachrichtAnsicht(n, gespraech);
        a.Geaendert += ZumEnde;
        if (stelle > 0) a.Margin = new Thickness(0, 18, 0, 0);
        ansichten[n] = a;
        liste.Children.Insert(stelle, a);
        if (stelle == 0 && liste.Children.Count > 1 && liste.Children[1] is FrameworkElement folgende) folgende.Margin = new Thickness(0, 18, 0, 0);
    }

    private void AelterePruefen()
    {
        // Erst nach dem ersten Sprung ans Ende: Vorher steht der Verlauf noch oben und holte sonst alles auf einmal.
        if (!bereit || ersteGezeigt == 0 || scroller.Sv.VerticalOffset > 400) return;
        var hoeheVorher = scroller.Sv.ExtentHeight;
        var offsetVorher = scroller.Sv.VerticalOffset;
        var n = gespraech.Nachrichten;
        var neueErste = Math.Max(0, ersteGezeigt - Seite);
        for (var i = ersteGezeigt - 1; i >= neueErste; i--) Anfuegen(n[i], 0);
        ersteGezeigt = neueErste;
        // Die Stelle im Verlauf bleibt stehen, auch wenn oben Nachrichten dazukommen.
        Spaeter(() => scroller.Sv.ScrollToVerticalOffset(offsetVorher + scroller.Sv.ExtentHeight - hoeheVorher), DispatcherPriority.Loaded);
    }

    private void NachrichtenGeaendert(object? s, NotifyCollectionChangedEventArgs e)
    {
        var alle = gespraech.Nachrichten;
        switch (e.Action)
        {
            case NotifyCollectionChangedAction.Add when e.NewStartingIndex >= alle.Count - (e.NewItems?.Count ?? 0):
                foreach (Nachricht n in e.NewItems!) Anfuegen(n, liste.Children.Count);
                break;
            case NotifyCollectionChangedAction.Remove:
                foreach (Nachricht n in e.OldItems!)
                    if (ansichten.Remove(n, out var a)) { a.Geaendert -= ZumEnde; liste.Children.Remove(a); }
                ersteGezeigt = Math.Max(0, alle.Count - liste.Children.Count);
                break;
            default:
                ListeAufbauen();
                break;
        }
        leerPlatz.Child = alle.Count == 0 ? Leer() : null;
        ZumEnde();
    }

    private void ZumEnde()
    {
        if (scrollWartet) return;
        scrollWartet = true;
        Haupt.D?.BeginInvoke(() => { scrollWartet = false; scroller.Sv.ScrollToEnd(); }, DispatcherPriority.Loaded);
    }
}

public sealed class PegelBalken : Stapel2
{
    private readonly Func<float> pegel;
    private readonly Border[] balken = new Border[5];

    public float Pegel { set { wert = value; } }
    private float wert;

    public PegelBalken(float pegel) : this(null, pegel) { }
    public PegelBalken(Func<float> pegel) : this(pegel, 0) { }
    private PegelBalken(Func<float>? quelle, float start)
    {
        wert = start;
        pegel = quelle ?? (() => wert);
        var r = new StackPanel { Orientation = Orientation.Horizontal, Height = 22, VerticalAlignment = VerticalAlignment.Center };
        for (var i = 0; i < 5; i++)
        {
            balken[i] = new Border { Width = 3, Height = 4, CornerRadius = new CornerRadius(1.5), Background = Farbe.Gruen, Margin = new Thickness(i > 0 ? 3 : 0, 0, 0, 0), VerticalAlignment = VerticalAlignment.Center };
            r.Children.Add(balken[i]);
        }
        Inhalt = r;
        Takt.Binde(this, () => 1.0 / 30, t =>
        {
            var p = pegel();
            for (var i = 0; i < 5; i++) balken[i].Height = Math.Max(4, 4 + p * 18 * (0.6 + 0.4 * Math.Sin(t * 9 + i)));
        });
    }
}

public sealed class NachrichtAnsicht : Border
{
    private readonly Nachricht n;
    private readonly Gespraech gespraech;
    private readonly DispatcherTimer sammler = new(DispatcherPriority.Render) { Interval = TimeSpan.FromMilliseconds(50) };
    public event Action? Geaendert;
    private bool angemeldet;
    private readonly HashSet<Schritt> beobachteteSchritte = new();

    // Jarvis-Teile
    private Decorator? kugelPlatz;
    private bool? kugelLaeuft;
    private Decorator? schrittePlatz;
    private Decorator? textPlatz;
    private MarkdownText? markdown;
    private DenkPunkte? punkte;
    private Decorator? aktionenPlatz;
    private bool? bewertung;
    private bool schritteOffen;
    private bool schwebt;

    public NachrichtAnsicht(Nachricht n, Gespraech g)
    {
        this.n = n;
        gespraech = g;
        if (n.Rolle == Rolle.Frank) Child = Frank();
        else
        {
            Child = Jarvis();
            Background = Farbe.Klar;
            this.Schwebe(an => { schwebt = an; if (aktionenPlatz != null) aktionenPlatz.Opacity = an ? 1 : 0; });
            Zeichne();
        }
        // Höchstens 20-mal je Sekunde: Beim Streamen sammeln sich die Textstücke, gezeichnet wird nur diese eine Nachricht.
        sammler.Tick += (_, _) =>
        {
            sammler.Stop();
            Zeichne();
            // Nur die neueste Nachricht zieht den Verlauf nach unten.
            if (ReferenceEquals(gespraech.Nachrichten.LastOrDefault(), n)) Geaendert?.Invoke();
        };
        Loaded += (_, _) =>
        {
            if (angemeldet) return;
            angemeldet = true;
            n.PropertyChanged += NachrichtGeaendert;
            n.Schritte.CollectionChanged += SchritteGeaendert;
            foreach (var s in n.Schritte) if (beobachteteSchritte.Add(s)) s.PropertyChanged += SchrittGeaendert;
            Zeichne();
        };
        Unloaded += (_, _) =>
        {
            angemeldet = false;
            sammler.Stop();
            n.PropertyChanged -= NachrichtGeaendert;
            n.Schritte.CollectionChanged -= SchritteGeaendert;
            foreach (var s in beobachteteSchritte) s.PropertyChanged -= SchrittGeaendert;
            beobachteteSchritte.Clear();
        };
    }

    private void NachrichtGeaendert(object? s, PropertyChangedEventArgs e) => Vormerken();
    private void SchritteGeaendert(object? s, NotifyCollectionChangedEventArgs e)
    {
        foreach (var x in n.Schritte) if (beobachteteSchritte.Add(x)) x.PropertyChanged += SchrittGeaendert;
        Vormerken();
    }
    private void SchrittGeaendert(object? s, PropertyChangedEventArgs e) => Vormerken();
    private void Vormerken() { if (!sammler.IsEnabled) sammler.Start(); }

    private string FrageVorher
    {
        get
        {
            var i = gespraech.Nachrichten.IndexOf(n);
            if (i < 0) return "";
            for (var k = i - 1; k >= 0; k--) if (gespraech.Nachrichten[k].Rolle == Rolle.Frank) return gespraech.Nachrichten[k].Text;
            return "";
        }
    }

    private UIElement Frank()
    {
        var v = new StackPanel { HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(120, 0, 0, 0) };
        if (n.Bild is { } b && BildHelfer.Bild(b, 440) is { } bild)
        {
            var im = RundBild(bild, 220, 14);
            im.HorizontalAlignment = HorizontalAlignment.Right;
            im.Margin = new Thickness(0, 0, 0, 6);
            v.Children.Add(im);
        }
        var farbe = new SolidColorBrush(Color.FromArgb(224, 0, 0, 0));
        var text = new WaehlbarerText { Text = n.Text, FontSize = 15, FontFamily = Ui.Schrift, Foreground = farbe, TextWrapping = TextWrapping.Wrap };
        UIElement inhalt = text;
        if (n.Gesprochen)
        {
            var zeile = new DockPanel();
            var welle = Sym.Bild("waveform", 11, new SolidColorBrush(Color.FromArgb(128, 0, 0, 0)));
            welle.VerticalAlignment = VerticalAlignment.Top;
            welle.Margin = new Thickness(0, 3, 6, 0);
            DockPanel.SetDock(welle, Dock.Left);
            zeile.Children.Add(welle);
            zeile.Children.Add(text);
            inhalt = zeile;
        }
        v.Children.Add(new Border { Child = inhalt, Padding = new Thickness(16, 11, 16, 11), CornerRadius = new CornerRadius(20), Background = Farbe.Akzent, HorizontalAlignment = HorizontalAlignment.Right });
        return v;
    }

    private UIElement Jarvis()
    {
        kugelPlatz = new Decorator { Width = 30, Height = 30, VerticalAlignment = VerticalAlignment.Top };
        schrittePlatz = new Decorator();
        textPlatz = new Decorator();
        aktionenPlatz = new Decorator { Opacity = 0 };
        return HOben(14, kugelPlatz, V(10, schrittePlatz, textPlatz, aktionenPlatz).Fuell().Rand(0, 0, 40, 0));
    }

    private void Zeichne()
    {
        if (n.Rolle == Rolle.Frank) return;
        // Abgeschlossene Antworten mit stehender Kugel: Jede laufende Kugel rechnet Unschärfe auf dem Grafikchip.
        if (kugelLaeuft != n.Laeuft)
        {
            kugelLaeuft = n.Laeuft;
            kugelPlatz!.Child = new JarvisKugel(0, n.Laeuft ? JarvisKugel.KugelZustand.Denkt : JarvisKugel.KugelZustand.Ruhe, 30, still: !n.Laeuft);
        }
        SchritteZeichnen();
        if (n.Text.Length == 0 && n.Laeuft)
        {
            if (punkte == null) { punkte = new DenkPunkte(); markdown = null; textPlatz!.Child = punkte; }
        }
        else
        {
            if (markdown == null) { markdown = new MarkdownText(n.Text, 15); punkte = null; textPlatz!.Child = markdown; }
            else markdown.Text = n.Text;
        }
        AktionenZeichnen();
    }

    private void AktionenZeichnen()
    {
        if (n.Laeuft || n.Text.Length == 0) { aktionenPlatz!.Child = null; aktionenPlatz.Visibility = Visibility.Collapsed; return; }
        aktionenPlatz!.Visibility = Visibility.Visible;
        aktionenPlatz.Opacity = schwebt ? 1 : 0;
        var runter = new KleinKnopf(bewertung == false ? "hand.thumbsdown.fill" : "hand.thumbsdown", "Gefällt mir nicht – Jarvis lernt daraus", () =>
        {
            if (bewertung == false) return;
            bewertung = false; StilLernen.Bewerte(n.Text, FrageVorher, false);
            AktionenZeichnen();
        }, bewertung == false ? Farbe.Haupt : null);
        var hoch = new KleinKnopf(bewertung == true ? "hand.thumbsup.fill" : "hand.thumbsup", "Gefällt mir – Jarvis lernt daraus", () =>
        {
            if (bewertung == true) return; // je Antwort nur einmal lernen
            bewertung = true; StilLernen.Bewerte(n.Text, FrageVorher, true);
            AktionenZeichnen();
        }, bewertung == true ? Farbe.Haupt : null);
        var teilen = new TeilenKnopf(n.Text) { Width = 26, Height = 24 };
        aktionenPlatz.Child = H(6,
            new KleinKnopf("doc.on.doc", "Kopieren", () => Bau.Kopiere(n.Text)),
            new KleinKnopf("speaker.wave.2", "Vorlesen", () => Starte(() => Stimme.Shared.Sprich(n.Text))),
            hoch, runter, teilen,
            new KleinKnopf("arrow.turn.up.right", "In neuem Gespräch fortsetzen", () =>
            {
                AppSteuerung.Shared.NeuesGespraech();
                AppSteuerung.Shared.Entwurf = $"Zur Fortsetzung, deine letzte Antwort war:\n\n{n.Text.Schnitt(3000)}\n\nWeiter: ";
            }),
            new KleinKnopf("arrow.clockwise", "Erneut fragen", () =>
            {
                if (gespraech.Nachrichten.LastOrDefault(x => x.Rolle == Rolle.Frank)?.Text is { } frage)
                {
                    AppSteuerung.Shared.Gespraech = gespraech;
                    AppSteuerung.Shared.Sende(frage);
                }
            }),
            Tx(n.Zeit.ToString("HH:mm", Deutsch), 10, null, Farbe.Text3).Rand(4, 0, 0, 0));
    }

    private int schritteStand = -1;
    private void SchritteZeichnen()
    {
        if (n.Schritte.Count == 0) { schrittePlatz!.Child = null; schrittePlatz.Visibility = Visibility.Collapsed; return; }
        schrittePlatz!.Visibility = Visibility.Visible;
        var stand = HashCode.Combine(n.Schritte.Count, schritteOffen, n.Schritte.Count(s => s.Ok == true), n.Schritte.Count(s => s.Ok == false));
        if (stand == schritteStand) return;
        schritteStand = stand;
        var anzahl = n.Schritte.Count;
        var kopf = H(6, Sym.Bild("bolt.fill", 10, Farbe.Gold),
            Tx($"{anzahl} {(anzahl == 1 ? "Schritt" : "Schritte")}", 12, FontWeights.SemiBold, Farbe.Text2),
            !schritteOffen && n.Schritte.LastOrDefault() is { } letzter ? Tx("· " + Anzeigename(letzter.Werkzeug), 12, null, Farbe.Text3, zeilen: 1) : null,
            Sym.Bild(schritteOffen ? "chevron.up" : "chevron.down", 9, Farbe.Text3));
        kopf.Klick(() => { schritteOffen = !schritteOffen; SchritteZeichnen(); });
        var v = V(6, kopf);
        if (schritteOffen)
            foreach (var s in n.Schritte)
            {
                UIElement zustand = s.Ok is { } ok
                    ? Sym.Bild(ok ? "checkmark.circle.fill" : "exclamationmark.triangle.fill", 11, ok ? Farbe.Gruen : Farbe.Warnung, 14)
                    : new Spinner(10).Rand(2, 0);
                v.Children.Add(H(8,
                    Sym.Bild(s.AmHandy ? "iphone" : "macbook", 10, s.AmHandy ? Farbe.Gold : Farbe.Haupt, 14), zustand,
                    Tx(Anzeigename(s.Werkzeug), 12, FontWeights.SemiBold, Farbe.Text2, zeilen: 1),
                    Tx(s.Kurz, 11, null, Farbe.Text3, zeilen: 1, mono: true).Fuell()));
            }
        schrittePlatz.Child = new Border { Child = v, Padding = new Thickness(12, 8, 12, 8), CornerRadius = new CornerRadius(12), Background = Farbe.Glas(0.04), HorizontalAlignment = HorizontalAlignment.Left };
    }

    public static string Anzeigename(string w)
    {
        var t = w.Replace("_", " ").Replace("ae", "ä").Replace("oe", "ö").Replace("ue", "ü");
        return Deutsch.TextInfo.ToTitleCase(t.ToLower(Deutsch));
    }
}

public sealed class KleinKnopf : Border
{
    public KleinKnopf(string symbol, string hilfe, Action aktion, Brush? farbe = null)
    {
        Width = 26; Height = 24;
        CornerRadius = new CornerRadius(7);
        Background = Farbe.Glas(0.06);
        Child = Sym.Bild(symbol, 11, farbe ?? Farbe.Text2);
        this.Hilfe(hilfe);
        this.Klick(aktion);
    }
}

public sealed class DenkPunkte : Stapel2
{
    private readonly (Border B, ScaleTransform S)[] kreise = new (Border, ScaleTransform)[3];
    public DenkPunkte()
    {
        var r = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(0, 6, 0, 6), HorizontalAlignment = HorizontalAlignment.Left };
        for (var i = 0; i < 3; i++)
        {
            var s = new ScaleTransform();
            var b = new Border { Width = 7, Height = 7, CornerRadius = new CornerRadius(3.5), Background = Farbe.Akzent, RenderTransformOrigin = new Point(0.5, 0.5), RenderTransform = s, Margin = new Thickness(i > 0 ? 6 : 0, 0, 0, 0) };
            kreise[i] = (b, s);
            r.Children.Add(b);
        }
        Inhalt = r;
        Takt.Binde(this, () => 1.0 / 30, t =>
        {
            for (var i = 0; i < 3; i++)
            {
                var w = Math.Max(0, Math.Sin(t * 4 - i * 0.7));
                kreise[i].B.Opacity = 0.3 + 0.7 * w;
                kreise[i].S.ScaleX = kreise[i].S.ScaleY = 0.8 + 0.3 * w;
            }
        });
    }
}

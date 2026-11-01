using System.Collections;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Diagnostics;
using System.Globalization;
using System.Reflection;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Effects;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using System.Windows.Threading;
using Pfad = System.Windows.Shapes.Path;

namespace JarvisWindows;

/// <summary>Jarvis-Design: Schwarz mit Orange im Dunkelmodus, warmes Hell mit Orange im Hellmodus. Umgeschaltet über DesignStand.
/// Alle Werte sind eingefrorene Brushes. Beim Wechsel Hell/Dunkel baut HauptFenster seinen ganzen Inhalt neu; Ansichten lesen die Farben also einfach beim Bauen.</summary>
public static class Farbe
{
    private static bool D => DesignStand.Shared.Dunkel;
    private static byte Kanal(double v) => (byte)Math.Round(Math.Clamp(v, 0, 1) * 255);
    public static Color C(double r, double g, double b, double a = 1) => Color.FromArgb(Kanal(a), Kanal(r), Kanal(g), Kanal(b));
    private static SolidColorBrush S(Color c) { var b = new SolidColorBrush(c); b.Freeze(); return b; }
    private static SolidColorBrush S(double r, double g, double b, double a = 1) => S(C(r, g, b, a));

    private static readonly Brush GrundD = S(0, 0, 0), GrundH = S(1.0, 0.975, 0.94), Grund2D = S(0.065, 0.06, 0.055), Grund2H = S(1.0, 0.95, 0.885),
        HauptD = S(1.0, 0.55, 0.10), HauptH = S(0.92, 0.43, 0.03), GoldD = S(1.0, 0.73, 0.28), GoldH = S(0.82, 0.52, 0.02),
        KupferD = S(1.0, 0.38, 0.16), KupferH = S(0.80, 0.29, 0.07), GruenD = S(0.30, 0.92, 0.58), GruenH = S(0.09, 0.58, 0.30),
        WarnungD = S(1.0, 0.82, 0.28), WarnungH = S(0.70, 0.42, 0.0), RotD = S(1.0, 0.36, 0.40), RotH = S(0.80, 0.15, 0.17),
        TextD = S(1, 1, 1, 0.94), TextH = S(0, 0, 0, 0.88), Text2D = S(1, 1, 1, 0.62), Text2H = S(0, 0, 0, 0.62), Text3D = S(1, 1, 1, 0.38), Text3H = S(0, 0, 0, 0.42),
        LinieD = S(1, 1, 1, 0.10), LinieH = S(0, 0, 0, 0.10);

    public static Brush Grund => D ? GrundD : GrundH;
    public static Brush Grund2 => D ? Grund2D : Grund2H;
    public static Brush Haupt => D ? HauptD : HauptH;
    public static Brush Gold => D ? GoldD : GoldH;
    public static Brush Kupfer => D ? KupferD : KupferH;
    public static Brush Gruen => D ? GruenD : GruenH;
    public static Brush Warnung => D ? WarnungD : WarnungH;
    public static Brush Rot => D ? RotD : RotH;
    public static Brush Text => D ? TextD : TextH;
    public static Brush Text2 => D ? Text2D : Text2H;
    public static Brush Text3 => D ? Text3D : Text3H;
    public static Brush Linie => D ? LinieD : LinieH;
    /// <summary>Color.black / Color.white der Mac-Fassung (etwa auf Akzentflächen).</summary>
    public static readonly Brush Schwarz = S(0, 0, 0), Weiss = S(1, 1, 1), Klar = S(Colors.Transparent);

    private static readonly Dictionary<(bool, double), Brush> GlasVorrat = new(), TiefeVorrat = new();
    /// <summary>Flächen und Ränder: heller als der Grund im Dunkelmodus, dunkler im Hellmodus.</summary>
    public static Brush Glas(double a)
    {
        var k = (D, a);
        if (GlasVorrat.TryGetValue(k, out var b)) return b;
        return GlasVorrat[k] = D ? S(1, 1, 1, a) : S(0, 0, 0, a * 0.8);
    }
    /// <summary>Vertiefte Felder wie Texteditoren.</summary>
    public static Brush Tiefe(double a)
    {
        var k = (D, a);
        if (TiefeVorrat.TryGetValue(k, out var b)) return b;
        return TiefeVorrat[k] = D ? S(0, 0, 0, a) : S(1, 1, 1, Math.Min(1, a * 2.4));
    }
    private static Brush Verlauf(Color von, Color bis) { var b = new LinearGradientBrush(von, bis, new Point(0, 0), new Point(1, 1)); b.Freeze(); return b; }
    private static readonly Brush AkzentD = Verlauf(Wert(HauptD), Wert(GoldD)), AkzentH = Verlauf(Wert(HauptH), Wert(GoldH)),
        AkzentWarmD = Verlauf(Wert(KupferD), Wert(HauptD)), AkzentWarmH = Verlauf(Wert(KupferH), Wert(HauptH));
    public static Brush Akzent => D ? AkzentD : AkzentH;
    public static Brush AkzentWarm => D ? AkzentWarmD : AkzentWarmH;

    /// <summary>Der Farbwert einer Fläche.</summary>
    public static Color Wert(Brush b) => b is SolidColorBrush s ? s.Color : Colors.Transparent;
    /// <summary>farbe.opacity(a).</summary>
    public static Brush Mit(Brush b, double a) { var c = Wert(b); return S(Color.FromArgb(Kanal(a), c.R, c.G, c.B)); }
    public static Color MitWert(Color c, double a) => Color.FromArgb(Kanal(a), c.R, c.G, c.B);
    /// <summary>Verlauf von oben nach unten (etwa für den Glasrand).</summary>
    public static Brush VonOben(Brush a, Brush b) { var v = new LinearGradientBrush(Wert(a), Wert(b), new Point(0, 0), new Point(0, 1)); v.Freeze(); return v; }
}

/// <summary>SF Symbols → Segoe Fluent Icons.</summary>
public static class Sym
{
    public static readonly FontFamily Schrift = new("Segoe Fluent Icons, Segoe MDL2 Assets");
    private const string Rueckfall = ""; // Fragezeichen im Kreis
    private static readonly Dictionary<string, string> Tafel = new()
    {
        ["antenna.radiowaves.left.and.right"] = "", ["archivebox.fill"] = "", ["arrow.clockwise"] = "", ["arrow.down"] = "", ["arrow.down.circle.fill"] = "",
        ["arrow.right"] = "", ["arrow.triangle.2.circlepath"] = "", ["arrow.triangle.branch"] = "", ["arrow.turn.up.right"] = "", ["arrow.up"] = "",
        ["arrow.up.arrow.down"] = "", ["arrow.up.forward.app"] = "", ["arrow.up.left.and.arrow.down.right"] = "", ["arrow.up.right.square"] = "", ["arrow.uturn.backward"] = "",
        ["bolt.fill"] = "", ["bolt.heart.fill"] = "", ["bolt.shield.fill"] = "", ["bookmark.fill"] = "", ["brain"] = "", ["brain.head.profile.fill"] = "",
        ["bubble.left.and.bubble.right.fill"] = "", ["bubble.left.and.text.bubble.right.fill"] = "", ["bubble.left.fill"] = "", ["calendar"] = "", ["checklist"] = "",
        ["checkmark"] = "", ["checkmark.circle.fill"] = "", ["checkmark.seal.fill"] = "", ["checkmark.shield.fill"] = "", ["chevron.down"] = "", ["chevron.left"] = "",
        ["chevron.left.forwardslash.chevron.right"] = "", ["chevron.right"] = "", ["chevron.up"] = "", ["circle.lefthalf.filled"] = "", ["cloud.sun.fill"] = "",
        ["creditcard.fill"] = "", ["doc.fill"] = "", ["doc.on.doc"] = "", ["doc.plaintext.fill"] = "", ["doc.richtext.fill"] = "", ["doc.text.fill"] = "",
        ["exclamationmark.circle.fill"] = "", ["exclamationmark.triangle.fill"] = "", ["eye.fill"] = "", ["film.fill"] = "", ["folder.badge.plus"] = "", ["folder.fill"] = "",
        ["gauge.with.dots.needle.67percent"] = "", ["gearshape.fill"] = "", ["hand.raised.fill"] = "", ["hand.thumbsdown"] = "", ["hand.thumbsdown.fill"] = "",
        ["hand.thumbsup"] = "", ["hand.thumbsup.fill"] = "", ["house.fill"] = "", ["info.circle.fill"] = "", ["iphone"] = "", ["iphone.radiowaves.left.and.right"] = "",
        ["key.fill"] = "", ["key.horizontal.fill"] = "", ["link"] = "", ["list.bullet"] = "", ["list.bullet.rectangle.portrait.fill"] = "", ["lock.fill"] = "",
        ["lock.open.fill"] = "", ["macbook"] = "", ["magnifyingglass"] = "", ["magnifyingglass.circle.fill"] = "", ["mic.fill"] = "", ["moon.fill"] = "",
        ["paperplane.fill"] = "", ["pause.fill"] = "", ["person.badge.key.fill"] = "", ["person.badge.shield.checkmark.fill"] = "", ["person.wave.2.fill"] = "",
        ["photo"] = "", ["photo.fill"] = "", ["play.fill"] = "", ["minus"] = "", ["plus"] = "", ["plus.bubble"] = "", ["plus.bubble.fill"] = "", ["plus.circle.fill"] = "",
        ["questionmark.circle"] = "", ["rectangle.on.rectangle.angled"] = "", ["rectangle.portrait.and.arrow.right"] = "", ["sparkles"] = "", ["speaker.wave.2"] = "",
        ["speaker.wave.2.fill"] = "", ["speaker.wave.3.fill"] = "", ["square.and.arrow.down"] = "", ["square.and.arrow.down.fill"] = "", ["square.and.arrow.up"] = "",
        ["square.and.pencil"] = "", ["square.grid.2x2.fill"] = "", ["square.stack.3d.up.fill"] = "", ["star.fill"] = "", ["stop.fill"] = "", ["sun.horizon.fill"] = "",
        ["sun.max.fill"] = "", ["tablecells.fill"] = "", ["terminal.fill"] = "", ["text.bubble.fill"] = "", ["touchid"] = "", ["trash"] = "", ["trash.fill"] = "",
        ["tray.and.arrow.down.fill"] = "", ["tray.full.fill"] = "", ["waveform"] = "", ["waveform.circle.fill"] = "", ["wrench.adjustable.fill"] = "",
        ["wrench.and.screwdriver.fill"] = "", ["xmark"] = "", ["xmark.circle"] = "", ["xmark.circle.fill"] = "",
    };

    /// <summary>Das Zeichen zu einem SF-Symbolnamen; unbekannte Namen ergeben ein festes Ersatzzeichen, nie einen Fehler.</summary>
    public static string Zeichen(string name) => Tafel.TryGetValue(name, out var z) ? z : Rueckfall;

    /// <summary>Image(systemName:) in der gegebenen Größe.</summary>
    public static TextBlock Bild(string name, double groesse, Brush? farbe = null, double? breite = null)
    {
        var t = new TextBlock
        {
            Text = Zeichen(name), FontFamily = Schrift, FontSize = groesse, Foreground = farbe ?? Farbe.Text, TextAlignment = TextAlignment.Center,
            HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center, IsHitTestVisible = false,
        };
        if (breite != null) t.Width = breite.Value;
        return t;
    }
}

/// <summary>Stapel wie HStack/VStack: fester Abstand, Kinder mit Stapel.Fuellt teilen sich den Rest (Spacer).</summary>
public sealed class Stapel : Panel
{
    public static readonly DependencyProperty FuelltProperty = DependencyProperty.RegisterAttached("Fuellt", typeof(bool), typeof(Stapel),
        new FrameworkPropertyMetadata(false, FrameworkPropertyMetadataOptions.AffectsParentMeasure));
    public static void SetFuellt(UIElement e, bool v) => e.SetValue(FuelltProperty, v);
    public static bool GetFuellt(UIElement e) => (bool)e.GetValue(FuelltProperty);

    public bool Waagerecht { get; }
    public double Abstand { get; set; }
    /// <summary>Nur waagerecht: oben statt mittig ausrichten (alignment: .top).</summary>
    public bool Oben { get; set; }

    public Stapel(bool waagerecht, double abstand) { Waagerecht = waagerecht; Abstand = abstand; }

    private IEnumerable<UIElement> Sichtbar => InternalChildren.Cast<UIElement>().Where(c => c != null && c.Visibility != Visibility.Collapsed);

    protected override Size MeasureOverride(Size verfuegbar)
    {
        double achse = Waagerecht ? verfuegbar.Width : verfuegbar.Height, quer = Waagerecht ? verfuegbar.Height : verfuegbar.Width;
        var kinder = Sichtbar.ToList();
        foreach (UIElement c in InternalChildren) if (c.Visibility == Visibility.Collapsed) c.Measure(new Size(0, 0));
        var luecken = Math.Max(0, kinder.Count - 1) * Abstand;
        double benutzt = 0, hoechst = 0;
        foreach (var c in kinder.Where(k => !GetFuellt(k)))
        {
            var rest = double.IsInfinity(achse) ? double.PositiveInfinity : Math.Max(0, achse - benutzt - luecken);
            c.Measure(Waagerecht ? new Size(rest, quer) : new Size(quer, rest));
            benutzt += Waagerecht ? c.DesiredSize.Width : c.DesiredSize.Height;
            hoechst = Math.Max(hoechst, Waagerecht ? c.DesiredSize.Height : c.DesiredSize.Width);
        }
        var fuell = kinder.Where(GetFuellt).ToList();
        foreach (var c in fuell)
        {
            var teil = double.IsInfinity(achse) ? double.PositiveInfinity : Math.Max(0, achse - benutzt - luecken) / fuell.Count;
            c.Measure(Waagerecht ? new Size(teil, quer) : new Size(quer, teil));
            hoechst = Math.Max(hoechst, Waagerecht ? c.DesiredSize.Height : c.DesiredSize.Width);
        }
        double gesamt;
        if (fuell.Count > 0 && !double.IsInfinity(achse)) gesamt = achse;
        else gesamt = benutzt + luecken + fuell.Sum(c => Waagerecht ? c.DesiredSize.Width : c.DesiredSize.Height);
        return Waagerecht ? new Size(gesamt, hoechst) : new Size(hoechst, gesamt);
    }

    protected override Size ArrangeOverride(Size flaeche)
    {
        var kinder = Sichtbar.ToList();
        var luecken = Math.Max(0, kinder.Count - 1) * Abstand;
        double achse = Waagerecht ? flaeche.Width : flaeche.Height;
        double fest = kinder.Where(k => !GetFuellt(k)).Sum(c => Waagerecht ? c.DesiredSize.Width : c.DesiredSize.Height);
        int nFuell = kinder.Count(GetFuellt);
        double teil = nFuell > 0 ? Math.Max(0, achse - fest - luecken) / nFuell : 0;
        double pos = 0;
        foreach (var c in kinder)
        {
            double l = GetFuellt(c) ? teil : (Waagerecht ? c.DesiredSize.Width : c.DesiredSize.Height);
            if (Waagerecht)
            {
                var h = c.DesiredSize.Height;
                var y = Oben ? 0 : Math.Max(0, (flaeche.Height - h) / 2);
                c.Arrange(new Rect(pos, y, l, h));
            }
            else c.Arrange(new Rect(0, pos, flaeche.Width, l));
            pos += l + Abstand;
        }
        return flaeche;
    }
}

/// <summary>Baukasten für die Oberfläche ohne XAML. Mit `using static JarvisWindows.Ui;` liest es sich wie SwiftUI.</summary>
public static class Ui
{
    public static readonly FontFamily Schrift = new("Segoe UI Variable Text, Segoe UI");
    public static readonly FontFamily Mono = new("Cascadia Mono, Consolas");

    /// <summary>Text(t).font(.jarvis(groesse, gewicht)).foregroundStyle(farbe). zeilen 1 = eine Zeile mit …, n = höchstens n Zeilen, 0 = frei umbrechend.</summary>
    public static TextBlock Tx(string text, double groesse = 13, FontWeight? gewicht = null, Brush? farbe = null, int zeilen = 0, bool mono = false)
    {
        var t = new TextBlock { Text = text, FontSize = groesse, FontFamily = mono ? Mono : Schrift, FontWeight = gewicht ?? FontWeights.Regular, Foreground = farbe ?? Farbe.Text };
        Zeilen(t, zeilen);
        return t;
    }

    public static void Zeilen(TextBlock t, int zeilen)
    {
        if (zeilen == 1) { t.TextWrapping = TextWrapping.NoWrap; t.TextTrimming = TextTrimming.CharacterEllipsis; }
        else
        {
            t.TextWrapping = TextWrapping.Wrap;
            if (zeilen > 1)
            {
                t.TextTrimming = TextTrimming.WordEllipsis;
                t.LineStackingStrategy = LineStackingStrategy.BlockLineHeight;
                t.LineHeight = t.FontSize * 1.35;
                t.MaxHeight = t.LineHeight * zeilen;
            }
        }
    }

    /// <summary>Wie `.tracking(n)`: Buchstaben mit festem Abstand.</summary>
    public static FrameworkElement Gesperrt(string text, double groesse, FontWeight gewicht, double abstand, Brush? farbe = null)
    {
        var s = new StackPanel { Orientation = Orientation.Horizontal };
        foreach (var z in text) s.Children.Add(new TextBlock { Text = z.ToString(), FontSize = groesse, FontFamily = Schrift, FontWeight = gewicht, Foreground = farbe ?? Farbe.Text, Margin = new Thickness(0, 0, abstand, 0) });
        return s;
    }

    public static Stapel H(double abstand, params UIElement?[] kinder) => Fuelle(new Stapel(true, abstand), kinder);
    public static Stapel V(double abstand, params UIElement?[] kinder) => Fuelle(new Stapel(false, abstand), kinder);
    public static Stapel HOben(double abstand, params UIElement?[] kinder) { var s = H(abstand, kinder); s.Oben = true; return s; }
    private static Stapel Fuelle(Stapel s, UIElement?[] kinder) { foreach (var k in kinder) if (k != null) s.Children.Add(k); return s; }

    /// <summary>Spacer(minLength:).</summary>
    public static FrameworkElement Spacer(double min = 0) { var f = new Border { MinWidth = min, MinHeight = 0 }; Stapel.SetFuellt(f, true); return f; }
    /// <summary>Nimmt in einem Stapel den freien Platz ein (frame(maxWidth: .infinity)); lange Texte brechen dann um.</summary>
    public static T Fuell<T>(this T e) where T : UIElement { Stapel.SetFuellt(e, true); return e; }

    public static T Rand<T>(this T e, double alle) where T : FrameworkElement { e.Margin = new Thickness(alle); return e; }
    public static T Rand<T>(this T e, double h, double v) where T : FrameworkElement { e.Margin = new Thickness(h, v, h, v); return e; }
    public static T Rand<T>(this T e, double l, double o, double r, double u) where T : FrameworkElement { e.Margin = new Thickness(l, o, r, u); return e; }
    public static T Breite<T>(this T e, double b) where T : FrameworkElement { e.Width = b; return e; }
    public static T Hoehe<T>(this T e, double h) where T : FrameworkElement { e.Height = h; return e; }
    public static T Links<T>(this T e) where T : FrameworkElement { e.HorizontalAlignment = HorizontalAlignment.Left; return e; }
    public static T Rechts<T>(this T e) where T : FrameworkElement { e.HorizontalAlignment = HorizontalAlignment.Right; return e; }
    public static T Mittig<T>(this T e) where T : FrameworkElement { e.HorizontalAlignment = HorizontalAlignment.Center; return e; }
    public static T Sichtbar<T>(this T e, bool ja) where T : UIElement { e.Visibility = ja ? Visibility.Visible : Visibility.Collapsed; return e; }

    /// <summary>padding + background(RoundedRectangle(cornerRadius).fill) + optional strokeBorder.</summary>
    public static Border Box(UIElement? inhalt, Brush? flaeche = null, double ecke = 0, double h = 0, double v = 0, Brush? rand = null, double randStaerke = 1) => new()
    {
        Child = inhalt, Background = flaeche, CornerRadius = new CornerRadius(ecke), Padding = new Thickness(h, v, h, v), BorderBrush = rand, BorderThickness = rand != null ? new Thickness(randStaerke) : new Thickness(0),
    };

    /// <summary>Kapselform (Capsule).</summary>
    public static Border Kapsel(UIElement? inhalt, Brush? flaeche = null, double h = 0, double v = 0, Brush? rand = null) => Box(inhalt, flaeche, 0, h, v, rand).KapselForm();

    /// <summary>Halbrunde Enden in jeder Höhe. Ein übergroßer CornerRadius ergäbe bei WPF eine Ellipse.</summary>
    public static T KapselForm<T>(this T b) where T : Border
    {
        void Stelle() => b.CornerRadius = new CornerRadius(b.ActualHeight / 2);
        b.SizeChanged += (_, _) => Stelle();
        return b;
    }

    /// <summary>.help(text).</summary>
    public static T Hilfe<T>(this T e, string text) where T : FrameworkElement
    {
        if (!string.IsNullOrEmpty(text))
            e.ToolTip = new ToolTip
            {
                Content = text, Background = Farbe.Grund2, Foreground = Farbe.Text, BorderBrush = Farbe.Linie, BorderThickness = new Thickness(1), FontFamily = Schrift, FontSize = 12, Padding = new Thickness(8, 5, 8, 5),
                MaxWidth = 420,
            };
        return e;
    }

    /// <summary>Klickbar wie ein Button mit .plain: kein Fokusraub, kein Chrom. Das Element bekommt eine durchsichtige Fläche, falls es keine hat.</summary>
    public static T Klick<T>(this T e, Action aktion) where T : FrameworkElement
    {
        if (e is Border b && b.Background == null) b.Background = Farbe.Klar;
        if (e is Panel p && p.Background == null) p.Background = Farbe.Klar;
        if (e is TextBlock t && t.Background == null) t.Background = Farbe.Klar;
        e.IsHitTestVisible = true;
        var gedrueckt = false;
        e.MouseLeftButtonDown += (_, a) => { if (!e.IsEnabled) return; gedrueckt = true; e.CaptureMouse(); a.Handled = true; };
        e.MouseLeftButtonUp += (_, a) =>
        {
            if (!gedrueckt) return;
            gedrueckt = false;
            e.ReleaseMouseCapture();
            a.Handled = true;
            var q = a.GetPosition(e);
            if (q.X >= 0 && q.Y >= 0 && q.X <= e.ActualWidth && q.Y <= e.ActualHeight) aktion();
        };
        e.LostMouseCapture += (_, _) => gedrueckt = false;
        e.IsEnabledChanged += (_, _) => e.Opacity = e.IsEnabled ? 1 : 0.4;
        return e;
    }

    /// <summary>.onHover.</summary>
    public static T Schwebe<T>(this T e, Action<bool> wechsel) where T : FrameworkElement
    {
        e.MouseEnter += (_, _) => wechsel(true);
        e.MouseLeave += (_, _) => wechsel(false);
        return e;
    }

    /// <summary>Hintergrund, der beim Schweben sanft wechselt (.animation(.easeOut(duration:)...)).</summary>
    public static T SchwebeFlaeche<T>(this T e, Brush normal, Brush schwebend, double ms = 150) where T : Border
    {
        var f = new SolidColorBrush(Farbe.Wert(normal));
        e.Background = f;
        var z = new Duration(TimeSpan.FromMilliseconds(ms));
        e.Schwebe(an => f.BeginAnimation(SolidColorBrush.ColorProperty, new ColorAnimation(Farbe.Wert(an ? schwebend : normal), z) { EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut } }));
        return e;
    }

    /// <summary>Bild mit runden Ecken in fester Höhe (.scaledToFit().frame(height:).clipShape(RoundedRectangle)).</summary>
    public static Border RundBild(BitmapSource bild, double hoehe, double ecke)
    {
        var h = Math.Min(hoehe, Math.Max(1, bild.PixelHeight));
        var pinsel = new ImageBrush(bild) { Stretch = Stretch.UniformToFill };
        pinsel.Freeze();
        return new Border { Height = h, Width = h * bild.PixelWidth / Math.Max(1, bild.PixelHeight), CornerRadius = new CornerRadius(ecke), Background = pinsel };
    }

    /// <summary>Text fürs Datum wie .formatted(.relative(presentation: .named)).</summary>
    public static string Relativ(DateTime d)
    {
        var s = (DateTime.Now - d).TotalSeconds;
        if (s < 60) return "gerade eben";
        if (s < 3600) { var m = (int)(s / 60); return m == 1 ? "vor 1 Minute" : $"vor {m} Minuten"; }
        if (s < 86400 && d.Date == DateTime.Today) { var h = (int)(s / 3600); return h == 1 ? "vor 1 Stunde" : $"vor {h} Stunden"; }
        var t = (DateTime.Today - d.Date).Days;
        if (t <= 1) return "gestern";
        if (t == 2) return "vorgestern";
        if (t < 7) return $"vor {t} Tagen";
        if (t < 14) return "letzte Woche";
        if (t < 30) return $"vor {t / 7} Wochen";
        if (t < 60) return "letzten Monat";
        if (t < 365) return $"vor {t / 30} Monaten";
        return t < 730 ? "letztes Jahr" : $"vor {t / 365} Jahren";
    }

    public static readonly CultureInfo Deutsch = new("de-DE");

    /// <summary>Hängt asynchrone Arbeit an den Hauptfaden (Swift: Task { }); Fehler landen im Logbuch statt die App zu beenden.</summary>
    public static async void Starte(Func<Task> f)
    {
        try { await f(); }
        catch (OperationCanceledException) { }
        catch (Exception ex) { Log("fehler", "Oberfläche: " + ex.Message, wo: "Oberfläche", fehler: ex.ToString()); }
    }

    /// <summary>Nach dem nächsten Layout ausführen.</summary>
    public static void Spaeter(Action a, DispatcherPriority p = DispatcherPriority.Input) => Haupt.D?.BeginInvoke(a, p);
}

/// <summary>Baut seinen Inhalt neu, sobald eine Quelle sich ändert (wie der body einer SwiftUI-Ansicht, gesammelt je Bild). Für kleine Teile, die vom Zustand abhängen; Textfelder gehören nicht hinein.</summary>
public sealed class Reaktiv : Decorator
{
    private readonly Func<UIElement?> bau;
    private readonly object[] quellen;
    private readonly HashSet<string>? nur;
    private bool angemeldet, wartet, erstesMal = true;

    public Reaktiv(Func<UIElement?> bau, params object[] quellen) : this(bau, null, quellen) { }
    /// <param name="nur">Nur diese Eigenschaften lösen Neubau aus.</param>
    public Reaktiv(Func<UIElement?> bau, IEnumerable<string>? nur, params object[] quellen)
    {
        this.bau = bau; this.quellen = quellen; this.nur = nur?.ToHashSet();
        Neu();
        Loaded += (_, _) => { Anmelden(); if (!erstesMal) Neu(); erstesMal = false; };
        Unloaded += (_, _) => Abmelden();
    }

    private void Anmelden()
    {
        if (angemeldet) return;
        angemeldet = true;
        foreach (var q in quellen)
        {
            if (q is INotifyPropertyChanged p) p.PropertyChanged += Geaendert;
            if (q is INotifyCollectionChanged c) c.CollectionChanged += ListeGeaendert;
        }
    }
    private void Abmelden()
    {
        if (!angemeldet) return;
        angemeldet = false;
        foreach (var q in quellen)
        {
            if (q is INotifyPropertyChanged p) p.PropertyChanged -= Geaendert;
            if (q is INotifyCollectionChanged c) c.CollectionChanged -= ListeGeaendert;
        }
    }
    private void Geaendert(object? s, PropertyChangedEventArgs e) { if (nur == null || e.PropertyName == null || nur.Contains(e.PropertyName)) Vormerken(); }
    private void ListeGeaendert(object? s, NotifyCollectionChangedEventArgs e) => Vormerken();
    private void Vormerken()
    {
        if (wartet) return;
        wartet = true;
        Haupt.D?.BeginInvoke(() => { wartet = false; if (angemeldet) Neu(); }, DispatcherPriority.Render);
    }
    /// <summary>Ohne Inhalt nimmt die Ansicht keinen Platz ein, auch keinen Abstand im Stapel (wie ein leeres if in SwiftUI).</summary>
    private void Neu()
    {
        Child = bau();
        Visibility = Child == null ? Visibility.Collapsed : Visibility.Visible;
    }
}

/// <summary>Gemeinsamer Bildtakt über CompositionTarget.Rendering: läuft nur, solange ein angemeldetes Element sichtbar ist und das Fenster vorn steht. Entspricht TimelineView(.animation(minimumInterval:, paused:)).</summary>
public static class Takt
{
    private sealed class Eintrag { public required FrameworkElement El; public required Func<double> Intervall; public required Action<double> Tick; public double Naechste; }
    private static readonly List<Eintrag> Aktiv = new();
    private static bool abonniert;
    private static readonly Stopwatch Uhr = Stopwatch.StartNew();
    /// <summary>Sekunden seit dem Start (timeIntervalSinceReferenceDate).</summary>
    public static double Jetzt => Uhr.Elapsed.TotalSeconds;

    public static void Binde(FrameworkElement el, Func<double> intervall, Action<double> tick)
    {
        var e = new Eintrag { El = el, Intervall = intervall, Tick = tick };
        void Pruefe()
        {
            if (el.IsVisible) { if (!Aktiv.Contains(e)) { Aktiv.Add(e); tick(Jetzt); } }
            else Aktiv.Remove(e);
            Abgleichen();
        }
        el.IsVisibleChanged += (_, _) => Pruefe();
        el.Unloaded += (_, _) => { Aktiv.Remove(e); Abgleichen(); };
        el.Loaded += (_, _) => Pruefe();
    }

    /// <summary>Nach Wechsel von AppVorn aufrufen.</summary>
    public static void Abgleichen()
    {
        var soll = Aktiv.Count > 0 && AppSteuerung.Shared.AppVorn;
        if (soll == abonniert) return;
        abonniert = soll;
        if (soll) CompositionTarget.Rendering += Bild; else CompositionTarget.Rendering -= Bild;
    }

    private static void Bild(object? s, EventArgs a)
    {
        var t = Jetzt;
        for (var i = Aktiv.Count - 1; i >= 0; i--)
        {
            if (i >= Aktiv.Count) continue;
            var e = Aktiv[i];
            if (t < e.Naechste) continue;
            e.Naechste = t + e.Intervall() * 0.9;
            e.Tick(t);
        }
    }
}

/// <summary>Hintergrund mit langsam wandernden Lichtflecken. Höchstens zehn Bilder je Sekunde; im Hintergrund und während Sprache und Live steht der Verlauf ganz still.</summary>
public sealed class JarvisHintergrund : Canvas
{
    private readonly (Ellipse E, TranslateTransform T, double Tempo, double Phase)[] flecken;
    private readonly DispatcherTimer uhr = new(DispatcherPriority.Render) { Interval = TimeSpan.FromMilliseconds(100) };
    private readonly Stopwatch zeit = Stopwatch.StartNew();
    private bool angemeldet;

    public JarvisHintergrund()
    {
        IsHitTestVisible = false;
        ClipToBounds = true;
        Background = Farbe.Grund;
        var werte = new (Brush F, double St, double Te, double Ph)[] { (Farbe.Haupt, 0.10, 0.07, 0.0), (Farbe.Gold, 0.07, 0.05, 2.1), (Farbe.Kupfer, 0.06, 0.04, 4.2) };
        flecken = werte.Select(w =>
        {
            var c = Farbe.Wert(w.F);
            var pinsel = new RadialGradientBrush(Farbe.MitWert(c, w.St), Farbe.MitWert(c, 0));
            pinsel.Freeze();
            var t = new TranslateTransform();
            var e = new Ellipse { Fill = pinsel, RenderTransform = t, CacheMode = new BitmapCache() };
            Children.Add(e);
            return (e, t, w.Te, w.Ph);
        }).ToArray();
        uhr.Tick += (_, _) => Bewege();
        SizeChanged += (_, _) => Bewege();
        Loaded += (_, _) =>
        {
            if (!angemeldet)
            {
                angemeldet = true;
                AppSteuerung.Shared.PropertyChanged += Geaendert;
                SprachSchleife.Shared.PropertyChanged += Geaendert;
                LiveGespraech.Shared.PropertyChanged += Geaendert;
            }
            Abgleich();
            Bewege();
        };
        Unloaded += (_, _) =>
        {
            angemeldet = false;
            uhr.Stop();
            AppSteuerung.Shared.PropertyChanged -= Geaendert;
            SprachSchleife.Shared.PropertyChanged -= Geaendert;
            LiveGespraech.Shared.PropertyChanged -= Geaendert;
        };
    }

    private void Geaendert(object? s, PropertyChangedEventArgs e) { if (e.PropertyName is "AppVorn" or "Zustand" or null) Abgleich(); }

    private void Abgleich()
    {
        var bewegt = AppSteuerung.Shared.AppVorn && SprachSchleife.Shared.Zustand == SprachZustand.Aus && !LiveGespraech.Shared.Aktiv;
        if (bewegt) uhr.Start(); else uhr.Stop();
    }

    private void Bewege()
    {
        double b = ActualWidth, h = ActualHeight;
        if (b <= 0 || h <= 0) return;
        var t = 700_000_000 + zeit.Elapsed.TotalSeconds;
        var r = Math.Max(b, h) * 0.55;
        foreach (var f in flecken)
        {
            if (Math.Abs(f.E.Width - r * 2) > 0.5) { f.E.Width = r * 2; f.E.Height = r * 2; }
            f.T.X = b * (0.5 + 0.38 * Math.Cos(t * f.Tempo + f.Phase)) - r;
            f.T.Y = h * (0.5 + 0.34 * Math.Sin(t * f.Tempo * 1.3 + f.Phase)) - r;
        }
    }
}

/// <summary>Glas-Karte im Liquid-Glass-Stil: halbtransparente Fläche mit Verlauf-Rand.</summary>
public sealed class GlasKarte : Border
{
    public GlasKarte(UIElement? inhalt, double ecke = 22, double abstand = 18)
    {
        Child = inhalt;
        CornerRadius = new CornerRadius(ecke);
        Padding = new Thickness(abstand);
        Background = Farbe.Glas(0.055);
        BorderBrush = Farbe.VonOben(Farbe.Glas(0.16), Farbe.Glas(0.03));
        BorderThickness = new Thickness(1);
    }
}

public sealed class KartenTitel : Stapel2
{
    /// <param name="farbe">Brush oder Color; ohne Angabe Farbe.Haupt.</param>
    public KartenTitel(string symbol, string titel, object? farbe = null)
    {
        var f = farbe == null ? Farbe.Haupt : Bau.B(farbe);
        var kreis = new Border { Width = 30, Height = 30, CornerRadius = new CornerRadius(15), Background = Farbe.Mit(f, 0.14), Child = Sym.Bild(symbol, 14, f) };
        Inhalt = Ui.H(10, kreis, Ui.Tx(titel, 15, FontWeights.SemiBold, Farbe.Text, zeilen: 1));
    }
}

/// <summary>Grundlage für Ansichten aus genau einem Kind.</summary>
public abstract class Stapel2 : Decorator
{
    protected UIElement? Inhalt { get => Child; set => Child = value; }
}

/// <summary>Die Jarvis-Kugel: runde, seitlich schimmernde Lichtkugel ohne Rahmen. Vibriert beim Sprechen, zeigt beim Zuhören Wellen und ein Mikrofon.
/// Zeichnet über den gemeinsamen Takt; alles je Bild sind nur Transformationen und Deckkraft auf zwischengespeicherten Ebenen.</summary>
public class JarvisKugel : Grid
{
    public enum KugelZustand { Ruhe, Hoert, Denkt, Spricht }

    public float Pegel { get; set; }
    public KugelZustand Zustand { get; set; }
    public double Groesse { get; }
    public bool Still { get; }

    private readonly Canvas buehne;
    private readonly ScaleTransform bebenSkala = new();
    private readonly TranslateTransform bebenWeg = new();
    private readonly Ellipse hof;
    private readonly ScaleTransform hofSkala = new();
    private readonly (Ellipse E, RotateTransform D, TranslateTransform W)[] schichten = new (Ellipse, RotateTransform, TranslateTransform)[3];
    private readonly Ellipse kern;
    private readonly (Pfad P, RotateTransform D)[] boegen = new (Pfad, RotateTransform)[2];
    private readonly Wellen wellen;
    private readonly TextBlock? mikro;
    private readonly ScaleTransform mikroSkala = new();
    private KugelZustand? gesetzt;

    public JarvisKugel(float pegel = 0, KugelZustand zustand = KugelZustand.Ruhe, double groesse = 220, bool still = false) : this(pegel, zustand, groesse, still, false) { }

    private JarvisKugel(float pegel, KugelZustand zustand, double groesse, bool still, bool roh)
    {
        Pegel = pegel; Zustand = zustand; Groesse = groesse; Still = still;
        Width = groesse; Height = groesse;
        ClipToBounds = false;
        Background = Farbe.Klar;
        buehne = new Canvas { Width = groesse, Height = groesse, IsHitTestVisible = false, ClipToBounds = false };
        Children.Add(buehne);
        if (still)
        {
            var b = StillBild(groesse, zustand);
            var bild = new Image { Source = b, Width = groesse * 2.3, Height = groesse * 2.3, Stretch = Stretch.Fill };
            Canvas.SetLeft(bild, -groesse * 0.65);
            Canvas.SetTop(bild, -groesse * 0.65);
            buehne.Children.Add(bild);
            hof = kern = null!;
            wellen = null!;
            return;
        }
        var g = groesse;
        buehne.RenderTransformOrigin = new Point(0.5, 0.5);
        buehne.RenderTransform = new TransformGroup { Children = { bebenSkala, bebenWeg } };

        hofSkala.CenterX = hofSkala.CenterY = g * 0.8;
        hof = new Ellipse { Width = g * 1.6, Height = g * 1.6, RenderTransform = hofSkala };
        Lege(hof, g * 1.6);
        wellen = new Wellen(g);
        Lege(wellen, g);

        for (var i = 0; i < 3; i++)
        {
            var d = g * (0.82 - 0.14 * i);
            var dreh = new RotateTransform { CenterX = d / 2, CenterY = d / 2 };
            var weg = new TranslateTransform();
            var e = new Ellipse { Width = d, Height = d, Opacity = 0.75 - i * 0.12, RenderTransform = new TransformGroup { Children = { dreh, weg } }, Effect = new BlurEffect { Radius = g * (0.07 - i * 0.015) * 2, RenderingBias = RenderingBias.Performance } };
            if (!roh) e.CacheMode = new BitmapCache();
            Lege(e, d);
            schichten[i] = (e, dreh, weg);
        }
        var kd = g * 0.62;
        var glanz = new RadialGradientBrush(Color.FromArgb(217, 255, 255, 255), Color.FromArgb(0, 255, 255, 255)) { Center = new Point(0.42, 0.38), GradientOrigin = new Point(0.42, 0.38), RadiusX = 0.32 / 0.62, RadiusY = 0.32 / 0.62 };
        glanz.Freeze();
        kern = new Ellipse { Width = kd, Height = kd, Fill = glanz };
        Lege(kern, kd);

        var bd = g * 0.78;
        var bogen = new PathGeometry(new[] { new PathFigure(new Point(bd, bd / 2), new[] { new ArcSegment(new Point(bd / 2 + bd / 2 * Math.Cos(72 * Math.PI / 180), bd / 2 + bd / 2 * Math.Sin(72 * Math.PI / 180)), new Size(bd / 2, bd / 2), 0, false, SweepDirection.Clockwise, true) }, false) });
        bogen.Freeze();
        for (var i = 0; i < 2; i++)
        {
            var dreh = new RotateTransform { CenterX = bd / 2, CenterY = bd / 2 };
            var p = new Pfad
            {
                Data = bogen, Width = bd, Height = bd, Stretch = Stretch.None, StrokeThickness = g * 0.035, StrokeStartLineCap = PenLineCap.Round, StrokeEndLineCap = PenLineCap.Round, RenderTransform = dreh,
                Effect = new BlurEffect { Radius = g * 0.05, RenderingBias = RenderingBias.Performance },
            };
            if (!roh) p.CacheMode = new BitmapCache();
            Lege(p, bd);
            boegen[i] = (p, dreh);
        }
        if (g >= 80)
        {
            mikroSkala.CenterX = g * 0.12; mikroSkala.CenterY = g * 0.12;
            mikro = new TextBlock
            {
                Text = Sym.Zeichen("mic.fill"), FontFamily = Sym.Schrift, FontSize = g * 0.16, Foreground = new SolidColorBrush(Color.FromArgb(242, 255, 255, 255)), Width = g * 0.24, Height = g * 0.24, TextAlignment = TextAlignment.Center,
                LineHeight = g * 0.24, LineStackingStrategy = LineStackingStrategy.BlockLineHeight, RenderTransform = mikroSkala, Visibility = Visibility.Collapsed,
                Effect = new DropShadowEffect { Color = Farbe.Wert(Farbe.Gruen), BlurRadius = g * 0.06, ShadowDepth = 0, Opacity = 1 },
            };
            Lege(mikro, g * 0.24);
        }
        if (!roh && !still) Takt.Binde(this, () => Zustand == KugelZustand.Ruhe ? 1.0 / 15 : 1.0 / 30, Zeichne);
        Zeichne(700_000_000 + Takt.Jetzt);
    }

    private void Lege(FrameworkElement e, double seite)
    {
        Canvas.SetLeft(e, (Groesse - seite) / 2);
        Canvas.SetTop(e, (Groesse - seite) / 2);
        buehne.Children.Add(e);
    }

    /// <summary>Wird vor jedem Bild aufgerufen; KugelLive liest hier den Stand.</summary>
    protected virtual void Aktualisieren() { }

    private void FarbenSetzen()
    {
        if (gesetzt == Zustand) return;
        gesetzt = Zustand;
        var farben = FarbenZu(Zustand);
        hof.Fill = HofPinsel(farben[0], Groesse);
        var winkel = Winkelverlauf(Zustand, farben);
        foreach (var s in schichten) s.E.Fill = winkel;
        var weiss = new SolidColorBrush(Color.FromArgb(191, 255, 255, 255)); weiss.Freeze();
        boegen[0].P.Stroke = weiss;
        var f0 = new SolidColorBrush(Farbe.MitWert(farben[0], 0.85)); f0.Freeze();
        boegen[1].P.Stroke = f0;
        wellen.Visibility = Zustand == KugelZustand.Hoert ? Visibility.Visible : Visibility.Collapsed;
        if (mikro != null) mikro.Visibility = Zustand == KugelZustand.Hoert ? Visibility.Visible : Visibility.Collapsed;
    }

    private static Brush HofPinsel(Color c, double g)
    {
        var b = new RadialGradientBrush { GradientStops = { new GradientStop(Farbe.MitWert(c, 0.45), 0), new GradientStop(Farbe.MitWert(c, 0), 0.75 / 0.8) } };
        b.Freeze();
        return b;
    }

    private static Color[] FarbenZu(KugelZustand z)
    {
        Color H = Farbe.Wert(Farbe.Haupt), G = Farbe.Wert(Farbe.Gold), K = Farbe.Wert(Farbe.Kupfer), N = Farbe.Wert(Farbe.Gruen);
        return z switch
        {
            KugelZustand.Ruhe => new[] { H, G, H },
            KugelZustand.Hoert => new[] { N, H, N },
            KugelZustand.Denkt => new[] { G, K, H, G },
            _ => new[] { H, K, G, H },
        };
    }

    private static readonly Dictionary<(KugelZustand, bool), ImageBrush> Winkel = new();
    /// <summary>WPF kennt keinen Winkelverlauf: einmal je Zustand und Design als Bild gerechnet, die Drehung macht danach die Transformation.</summary>
    private static Brush Winkelverlauf(KugelZustand z, Color[] farben)
    {
        var k = (z, DesignStand.Shared.Dunkel);
        if (Winkel.TryGetValue(k, out var da)) return da;
        const int n = 128;
        var px = new byte[n * n * 4];
        for (var y = 0; y < n; y++)
            for (var x = 0; x < n; x++)
            {
                var w = Math.Atan2(y - n / 2.0 + 0.5, x - n / 2.0 + 0.5) / (2 * Math.PI);
                if (w < 0) w += 1;
                var p = w * (farben.Length - 1);
                var i = Math.Min((int)p, farben.Length - 2);
                var f = p - i;
                Color a = farben[i], b = farben[i + 1];
                var o = (y * n + x) * 4;
                px[o] = (byte)(a.B + (b.B - a.B) * f);
                px[o + 1] = (byte)(a.G + (b.G - a.G) * f);
                px[o + 2] = (byte)(a.R + (b.R - a.R) * f);
                px[o + 3] = 255;
            }
        var bmp = BitmapSource.Create(n, n, 96, 96, PixelFormats.Bgra32, null, px, n * 4);
        bmp.Freeze();
        var pinsel = new ImageBrush(bmp) { Stretch = Stretch.Fill };
        pinsel.Freeze();
        return Winkel[k] = pinsel;
    }

    private static readonly Dictionary<(double, KugelZustand, bool), ImageSource> StillBilder = new();
    /// <summary>Stehende Kugeln (neben fertigen Antworten): einmal gerendert, dann nur noch ein Bild. Jede laufende Kugel rechnet sonst Unschärfe auf dem Grafikchip.</summary>
    private static ImageSource StillBild(double g, KugelZustand z)
    {
        var k = (g, z, DesignStand.Shared.Dunkel);
        if (StillBilder.TryGetValue(k, out var da)) return da;
        var kugel = new JarvisKugel(0, z, g, false, true);
        var seite = g * 2.3;
        var host = new Grid { Width = seite, Height = seite };
        kugel.HorizontalAlignment = HorizontalAlignment.Center;
        kugel.VerticalAlignment = VerticalAlignment.Center;
        host.Children.Add(kugel);
        host.Measure(new Size(seite, seite));
        host.Arrange(new Rect(0, 0, seite, seite));
        host.UpdateLayout();
        var rtb = new RenderTargetBitmap((int)Math.Ceiling(seite * 2), (int)Math.Ceiling(seite * 2), 192, 192, PixelFormats.Pbgra32);
        rtb.Render(host);
        rtb.Freeze();
        return StillBilder[k] = rtb;
    }

    private void Zeichne(double uhr)
    {
        if (Still) return;
        Aktualisieren();
        FarbenSetzen();
        var t = 700_000_000 % 100_000 + uhr;
        var g = Groesse;
        var p = (double)Math.Clamp(Pegel, 0f, 1f);
        var tempo = Zustand == KugelZustand.Denkt ? 2.4 : (Zustand == KugelZustand.Ruhe ? 0.5 : 1.2);
        var atmen = 1 + 0.03 * Math.Sin(t * 1.6) + p * 0.22;
        var beben = Zustand == KugelZustand.Spricht ? Math.Max(p, 0.4) : 0;
        hofSkala.ScaleX = hofSkala.ScaleY = atmen;
        if (Zustand == KugelZustand.Hoert) wellen.Setze(t, p);
        for (var i = 0; i < 3; i++)
        {
            var (_, d, w) = schichten[i];
            d.Angle = (t * 40 * tempo * (i + 1) + i * 70) % 360;
            w.X = Math.Cos(t * tempo + i * 2) * g * 0.035 * (1 + p * 2);
            w.Y = Math.Sin(t * tempo * 1.2 + i) * g * 0.035 * (1 + p * 2);
        }
        kern.Opacity = 0.55 + p * 0.4;
        for (var i = 0; i < 2; i++)
        {
            double seite = i * 180;
            boegen[i].D.Angle = seite - 36 + 22 * Math.Sin(t * 0.9 + seite);
            boegen[i].P.Opacity = Math.Clamp(0.5 + 0.35 * Math.Sin(t * 1.7 + seite), 0, 1);
        }
        if (mikro != null && Zustand == KugelZustand.Hoert) mikroSkala.ScaleX = mikroSkala.ScaleY = 1 + p * 0.35;
        bebenSkala.ScaleX = 1 + 0.07 * beben * Math.Sin(t * 33);
        bebenSkala.ScaleY = 1 + 0.07 * beben * Math.Cos(t * 41);
        bebenSkala.CenterX = bebenSkala.CenterY = g / 2;
        bebenWeg.X = g * 0.018 * beben * Math.Sin(t * 47);
        bebenWeg.Y = g * 0.018 * beben * Math.Cos(t * 53);
    }

    /// <summary>Eigene Zuhör-Animation: Wellen laufen vom Rand der Kugel nach außen, lauter gesprochen = kräftiger.</summary>
    private sealed class Wellen : FrameworkElement
    {
        private readonly double g;
        private double t, p;
        private readonly Color farbe = Farbe.Wert(Farbe.Gruen);
        public Wellen(double groesse) { g = groesse; Width = g; Height = g; Visibility = Visibility.Collapsed; }
        public void Setze(double zeit, double pegel) { t = zeit; p = pegel; InvalidateVisual(); }
        protected override void OnRender(DrawingContext dc)
        {
            var mitte = new Point(g / 2, g / 2);
            for (var i = 0; i < 3; i++)
            {
                var phase = (t * 0.55 + i / 3.0) % 1;
                var stift = new Pen(new SolidColorBrush(Farbe.MitWert(farbe, Math.Clamp((1 - phase) * (0.45 + p * 0.5), 0, 1))), g * 0.012);
                var r = g * (0.8 + 0.75 * phase) / 2;
                dc.DrawEllipse(null, stift, mitte, r, r);
            }
        }
    }
}

/// <summary>Kugel, die selbst auf Sprechen und Zuhören reagiert.</summary>
public sealed class KugelLive : JarvisKugel
{
    public KugelLive(double groesse) : base(0, KugelZustand.Ruhe, groesse) { }
    protected override void Aktualisieren()
    {
        Zustand = KugelStand.Zustand;
        Pegel = KugelStand.Pegel;
    }
}

/// <summary>Zustand und Pegel der Kugel aus allem, was gerade spricht oder zuhört (Sprachschleife, Live, reines Vorlesen).</summary>
public static class KugelStand
{
    public static JarvisKugel.KugelZustand Zustand
    {
        get
        {
            var schleife = SprachSchleife.Shared.Zustand;
            var live = LiveGespraech.Shared.Zustand;
            if (Stimme.Shared.Spricht || schleife == SprachZustand.Spricht || live == LiveZustand.Spricht) return JarvisKugel.KugelZustand.Spricht;
            if (schleife == SprachZustand.Denkt || live == LiveZustand.Denkt) return JarvisKugel.KugelZustand.Denkt;
            if (schleife == SprachZustand.Hoert || live == LiveZustand.Hoert) return JarvisKugel.KugelZustand.Hoert;
            return JarvisKugel.KugelZustand.Ruhe;
        }
    }
    public static float Pegel => Zustand switch
    {
        JarvisKugel.KugelZustand.Spricht => Math.Max(Stimme.Shared.Pegel, LiveGespraech.Shared.PegelAus),
        JarvisKugel.KugelZustand.Hoert => Math.Max(Diktat.Shared.Pegel, LiveGespraech.Shared.PegelEin),
        _ => 0,
    };
}

public sealed class RundKnopf : Border
{
    /// <param name="farbe">Brush oder Color; ohne Angabe Farbe.Haupt.</param>
    public RundKnopf(string symbol, Action aktion, string hilfe = "", bool aktiv = false, object? farbe = null, double groesse = 34)
    {
        Width = groesse; Height = groesse;
        CornerRadius = new CornerRadius(groesse / 2);
        BorderBrush = Farbe.Glas(0.10);
        BorderThickness = new Thickness(1);
        Child = Sym.Bild(symbol, groesse * 0.42, aktiv ? Farbe.Schwarz : Farbe.Text);
        if (aktiv) Background = farbe == null ? Farbe.Haupt : Bau.B(farbe);
        else this.SchwebeFlaeche(Farbe.Glas(0.07), Farbe.Glas(0.14));
        this.Hilfe(hilfe);
        this.Klick(aktion);
    }
}

public sealed class KapselKnopf : Border
{
    private readonly ScaleTransform skala = new();
    public KapselKnopf(string symbol, string titel, Action aktion, bool hervorgehoben = false, bool voll = false)
    {
        var farbe = hervorgehoben ? Farbe.Schwarz : Farbe.Text;
        var zeile = Ui.H(8, Sym.Bild(symbol, 13, farbe), Ui.Tx(titel, 13, FontWeights.SemiBold, farbe, zeilen: 1));
        Child = zeile;
        this.KapselForm();
        Padding = new Thickness(16, 10, 16, 10);
        BorderBrush = Farbe.Glas(0.10);
        BorderThickness = new Thickness(1);
        HorizontalAlignment = voll ? HorizontalAlignment.Stretch : HorizontalAlignment.Left;
        if (hervorgehoben) Background = Farbe.Akzent; else this.SchwebeFlaeche(Farbe.Glas(0.07), Farbe.Glas(0.13));
        RenderTransformOrigin = new Point(0.5, 0.5);
        RenderTransform = skala;
        if (!voll)
            this.Schwebe(an =>
            {
                var a = new DoubleAnimation(an ? 1.02 : 1, new Duration(TimeSpan.FromMilliseconds(250))) { EasingFunction = new BackEase { EasingMode = EasingMode.EaseOut, Amplitude = 0.3 } };
                skala.BeginAnimation(ScaleTransform.ScaleXProperty, a);
                skala.BeginAnimation(ScaleTransform.ScaleYProperty, a);
            });
        this.Klick(aktion);
    }
}

/// <summary>ProgressView: kleiner drehender Ring (controlSize .mini = 10, .small = 16).</summary>
public sealed class Spinner : Grid
{
    public Spinner(double groesse = 16, Brush? farbe = null)
    {
        Width = groesse; Height = groesse;
        var r = groesse / 2 - 1;
        var bogen = new PathGeometry(new[] { new PathFigure(new Point(groesse / 2, 1), new[] { new ArcSegment(new Point(groesse - 1, groesse / 2), new Size(r, r), 0, false, SweepDirection.Clockwise, true) }, false) });
        var dreh = new RotateTransform { CenterX = groesse / 2, CenterY = groesse / 2 };
        Children.Add(new Pfad { Data = bogen, Stroke = farbe ?? Farbe.Text2, StrokeThickness = Math.Max(1.5, groesse * 0.14), StrokeStartLineCap = PenLineCap.Round, StrokeEndLineCap = PenLineCap.Round, RenderTransform = dreh });
        Children.Insert(0, new Ellipse { Stroke = Farbe.Glas(0.1), StrokeThickness = Math.Max(1.5, groesse * 0.14), Margin = new Thickness(1) });
        dreh.BeginAnimation(RotateTransform.AngleProperty, new DoubleAnimation(0, 360, new Duration(TimeSpan.FromSeconds(0.9))) { RepeatBehavior = RepeatBehavior.Forever });
    }
}

/// <summary>ScrollView: Inhalt mit Rad, ohne die Standard-Leisten von Windows. Mit Indikator erscheint beim Schweben ein dünner Griff (.scrollIndicators(.automatic)).</summary>
public sealed class Scroller : Grid
{
    public ScrollViewer Sv { get; }
    private readonly Border? griff;

    public Scroller(UIElement? inhalt, bool indikator = false, bool waagerecht = false)
    {
        Sv = new ScrollViewer
        {
            Content = inhalt, Focusable = false, VerticalScrollBarVisibility = waagerecht ? ScrollBarVisibility.Disabled : ScrollBarVisibility.Hidden,
            HorizontalScrollBarVisibility = waagerecht ? ScrollBarVisibility.Hidden : ScrollBarVisibility.Disabled, PanningMode = PanningMode.Both,
        };
        Children.Add(Sv);
        if (waagerecht)
        {
            // Das Rad gehört dem äußeren Verlauf, nicht dem Codeblock.
            Sv.PreviewMouseWheel += (_, e) =>
            {
                e.Handled = true;
                if (VisualTreeHelper.GetParent(this) is UIElement oben)
                    oben.RaiseEvent(new MouseWheelEventArgs(e.MouseDevice, e.Timestamp, e.Delta) { RoutedEvent = MouseWheelEvent, Source = this });
            };
            return;
        }
        if (!indikator) return;
        griff = new Border { Width = 5, CornerRadius = new CornerRadius(2.5), Background = Farbe.Glas(0.28), HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Top, Margin = new Thickness(0, 0, 3, 0), Opacity = 0, RenderTransform = new TranslateTransform() };
        Children.Add(griff);
        Sv.ScrollChanged += (_, _) => GriffStellen();
        MouseEnter += (_, _) => { GriffStellen(); griff.BeginAnimation(OpacityProperty, new DoubleAnimation(1, TimeSpan.FromMilliseconds(120))); };
        MouseLeave += (_, _) => { if (!griff.IsMouseCaptured) griff.BeginAnimation(OpacityProperty, new DoubleAnimation(0, TimeSpan.FromMilliseconds(250))); };
        double start = 0, offset = 0;
        griff.MouseLeftButtonDown += (_, e) => { start = e.GetPosition(this).Y; offset = Sv.VerticalOffset; griff.CaptureMouse(); e.Handled = true; };
        griff.MouseMove += (_, e) =>
        {
            if (!griff.IsMouseCaptured || Sv.ViewportHeight <= 0 || griff.Height >= Sv.ViewportHeight) return;
            var weg = (e.GetPosition(this).Y - start) * Sv.ScrollableHeight / (Sv.ViewportHeight - griff.Height);
            Sv.ScrollToVerticalOffset(offset + weg);
        };
        griff.MouseLeftButtonUp += (_, _) => griff.ReleaseMouseCapture();
    }

    private void GriffStellen()
    {
        if (griff == null) return;
        if (Sv.ScrollableHeight <= 0) { griff.Visibility = Visibility.Collapsed; return; }
        griff.Visibility = Visibility.Visible;
        var h = Math.Max(28, Sv.ViewportHeight * Sv.ViewportHeight / Math.Max(1, Sv.ExtentHeight));
        griff.Height = h;
        ((TranslateTransform)griff.RenderTransform).Y = (Sv.ViewportHeight - h) * Sv.VerticalOffset / Sv.ScrollableHeight;
    }
}

/// <summary>Einfaches Textfeld im Jarvis-Stil mit Platzhalter (TextField(.plain)). Text zweiseitig über `lesen`/`schreiben`; Enter löst `absenden` aus.</summary>
public sealed class TextFeld : Grid
{
    public TextBox Box { get; }
    private readonly TextBlock platzhalter;
    public string Text { get => Box.Text; set { if (Box.Text != value) Box.Text = value; } }
    public event Action<string>? Geaendert;

    public TextFeld(string platzhalter, double groesse = 13, bool mehrzeilig = false, string text = "", Action? absenden = null, Action<string>? schreiben = null)
    {
        Box = new TextBox
        {
            Text = text, Background = Farbe.Klar, Foreground = Farbe.Text, CaretBrush = Farbe.Haupt, SelectionBrush = Farbe.Haupt, BorderThickness = new Thickness(0), FontFamily = Ui.Schrift, FontSize = groesse,
            Padding = new Thickness(0), VerticalContentAlignment = mehrzeilig ? VerticalAlignment.Top : VerticalAlignment.Center, AcceptsReturn = mehrzeilig, TextWrapping = mehrzeilig ? TextWrapping.Wrap : TextWrapping.NoWrap,
            FocusVisualStyle = null,
        };
        this.platzhalter = Ui.Tx(platzhalter, groesse, null, Farbe.Text3, zeilen: 1);
        this.platzhalter.IsHitTestVisible = false;
        this.platzhalter.VerticalAlignment = mehrzeilig ? VerticalAlignment.Top : VerticalAlignment.Center;
        this.platzhalter.Margin = new Thickness(2, 0, 0, 0);
        this.platzhalter.Visibility = text.Length == 0 ? Visibility.Visible : Visibility.Collapsed;
        Children.Add(this.platzhalter);
        Children.Add(Box);
        Box.TextChanged += (_, _) =>
        {
            this.platzhalter.Visibility = Box.Text.Length == 0 ? Visibility.Visible : Visibility.Collapsed;
            schreiben?.Invoke(Box.Text);
            Geaendert?.Invoke(Box.Text);
        };
        if (absenden != null && !mehrzeilig) Box.KeyDown += (_, e) => { if (e.Key == Key.Enter) { e.Handled = true; absenden(); } };
    }
}

/// <summary>Picker(.segmented).</summary>
public sealed class SegmentWahl : Border
{
    private readonly (string Wert, string Titel)[] optionen;
    private string wert;
    private readonly Action<string> wahl;
    private readonly Grid raster = new();

    public SegmentWahl(IEnumerable<(string Wert, string Titel)> optionen, string wert, Action<string> wahl)
    {
        this.optionen = optionen.ToArray(); this.wert = wert; this.wahl = wahl;
        CornerRadius = new CornerRadius(9);
        Background = Farbe.Glas(0.07);
        Padding = new Thickness(2);
        Child = raster;
        foreach (var _ in this.optionen) raster.ColumnDefinitions.Add(new ColumnDefinition());
        Bauen();
    }

    public string Wert { get => wert; set { if (wert != value) { wert = value; Bauen(); } } }

    private void Bauen()
    {
        raster.Children.Clear();
        for (var i = 0; i < optionen.Length; i++)
        {
            var o = optionen[i];
            var an = o.Wert == wert;
            var t = Ui.Tx(o.Titel, 12, an ? FontWeights.SemiBold : FontWeights.Medium, an ? Farbe.Text : Farbe.Text2, zeilen: 1);
            t.HorizontalAlignment = HorizontalAlignment.Center;
            var feld = new Border { Child = t, Padding = new Thickness(8, 5, 8, 5), CornerRadius = new CornerRadius(7), Background = an ? Farbe.Glas(0.14) : Farbe.Klar }
                .Klick(() => { Wert = o.Wert; wahl(o.Wert); });
            Grid.SetColumn(feld, i);
            raster.Children.Add(feld);
        }
    }
}

public sealed record MenuEintrag(string Titel, Action? Aktion = null, bool Trenner = false)
{
    public static MenuEintrag Linie => new("", null, true);
}

/// <summary>Menu { } als Klappmenü im Jarvis-Stil.</summary>
public static class JarvisMenue
{
    public static void Zeige(FrameworkElement anker, IEnumerable<MenuEintrag> eintraege, PlacementMode lage = PlacementMode.Top)
    {
        var popup = new Popup { PlacementTarget = anker, Placement = lage, AllowsTransparency = true, StaysOpen = false, PopupAnimation = PopupAnimation.Fade, VerticalOffset = lage == PlacementMode.Top ? -6 : 6 };
        var liste = new StackPanel();
        foreach (var e in eintraege)
        {
            if (e.Trenner) { liste.Children.Add(new Border { Height = 1, Background = Farbe.Linie, Margin = new Thickness(4, 5, 4, 5) }); continue; }
            var zeile = new Border { Padding = new Thickness(10, 6, 16, 6), CornerRadius = new CornerRadius(7), Child = Ui.Tx(e.Titel, 13, null, e.Aktion != null ? Farbe.Text : Farbe.Text2, zeilen: 1) };
            if (e.Aktion != null)
            {
                zeile.SchwebeFlaeche(Farbe.Klar, Farbe.Mit(Farbe.Haupt, 0.22), 80);
                zeile.MouseLeftButtonUp += (_, _) => { popup.IsOpen = false; e.Aktion(); };
            }
            liste.Children.Add(zeile);
        }
        popup.Child = new Border
        {
            Background = Farbe.Grund2, BorderBrush = Farbe.Glas(0.16), BorderThickness = new Thickness(1), CornerRadius = new CornerRadius(12), Padding = new Thickness(6), MinWidth = 180, MaxHeight = 560,
            Margin = new Thickness(10), Effect = new DropShadowEffect { BlurRadius = 18, ShadowDepth = 3, Opacity = 0.35 }, Child = new ScrollViewer { Content = liste, VerticalScrollBarVisibility = ScrollBarVisibility.Hidden, Focusable = false },
        };
        popup.IsOpen = true;
    }
}

public sealed record DialogKnopf(string Titel, Action? Aktion = null, bool Destruktiv = false, bool Abbruch = false);

/// <summary>.alert/.confirmationDialog/.sheet als Ebene über dem Hauptfenster.</summary>
public static class Dialog
{
    public static void Zeige(string titel, string? text, params DialogKnopf[] knoepfe)
    {
        var fenster = HauptFenster.Aktuell;
        if (fenster == null) { knoepfe.FirstOrDefault(k => k.Abbruch)?.Aktion?.Invoke(); return; }
        Action zu = () => { };
        var reihe = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
        foreach (var k in knoepfe)
        {
            var farbe = k.Destruktiv ? Farbe.Rot : (k.Abbruch ? Farbe.Text2 : Farbe.Text);
            var b = new Border { Child = Ui.Tx(k.Titel, 13, FontWeights.SemiBold, farbe), Padding = new Thickness(16, 8, 16, 8), CornerRadius = new CornerRadius(10), Margin = new Thickness(8, 0, 0, 0) };
            b.SchwebeFlaeche(Farbe.Glas(0.08), Farbe.Glas(0.16));
            b.Klick(() => { zu(); k.Aktion?.Invoke(); });
            reihe.Children.Add(b);
        }
        var inhalt = Ui.V(10, Ui.Tx(titel, 16, FontWeights.SemiBold), text != null ? Ui.Tx(text, 13, null, Farbe.Text2) : null, reihe.Rand(0, 8, 0, 0));
        zu = fenster.Ueberlagerung(new Border { Child = inhalt, Width = 420, Padding = new Thickness(22), CornerRadius = new CornerRadius(18), Background = Farbe.Grund2, BorderBrush = Farbe.Glas(0.16), BorderThickness = new Thickness(1), Effect = new DropShadowEffect { BlurRadius = 30, ShadowDepth = 4, Opacity = 0.4 } },
            () => knoepfe.FirstOrDefault(k => k.Abbruch)?.Aktion?.Invoke());
    }

    /// <summary>.sheet: eine Ansicht als Blatt über dem Fenster. Gibt die Funktion zum Schließen zurück.</summary>
    public static Action Blatt(FrameworkElement inhalt, Action? beimSchliessen = null)
    {
        var fenster = HauptFenster.Aktuell;
        if (fenster == null) return () => { };
        return fenster.Ueberlagerung(new Border { Child = inhalt, Padding = new Thickness(0), CornerRadius = new CornerRadius(18), Background = Farbe.Grund2, BorderBrush = Farbe.Glas(0.16), BorderThickness = new Thickness(1), ClipToBounds = true, Effect = new DropShadowEffect { BlurRadius = 30, ShadowDepth = 4, Opacity = 0.4 } }, beimSchliessen);
    }
}

/// <summary>TextBlock mit Auswahl per Maus (.textSelection(.enabled)), über denselben internen Editor wie TextBox. Geht das nicht, bleibt es ein normaler TextBlock.</summary>
public class WaehlbarerText : TextBlock
{
    private static readonly Type? EditorTyp = Type.GetType("System.Windows.Documents.TextEditor, PresentationFramework");
    private static readonly Type? ContainerTyp = Type.GetType("System.Windows.Documents.ITextContainer, PresentationFramework");
    private static readonly bool Geht;
    private object? editor;

    static WaehlbarerText()
    {
        try
        {
            var reg = EditorTyp?.GetMethod("RegisterCommandHandlers", BindingFlags.Static | BindingFlags.NonPublic, null, new[] { typeof(Type), typeof(bool), typeof(bool), typeof(bool) }, null);
            if (reg == null || ContainerTyp == null) return;
            reg.Invoke(null, new object[] { typeof(WaehlbarerText), true, true, true });
            FocusableProperty.OverrideMetadata(typeof(WaehlbarerText), new FrameworkPropertyMetadata(true));
            FocusVisualStyleProperty.OverrideMetadata(typeof(WaehlbarerText), new FrameworkPropertyMetadata((object?)null));
            Geht = true;
        }
        catch { Geht = false; }
    }

    public WaehlbarerText()
    {
        if (!Geht) return;
        try
        {
            var container = typeof(TextBlock).GetProperty("TextContainer", BindingFlags.Instance | BindingFlags.NonPublic)!.GetValue(this);
            editor = Activator.CreateInstance(EditorTyp!, BindingFlags.NonPublic | BindingFlags.Instance | BindingFlags.CreateInstance, null, new[] { container, this, false }, null);
            EditorTyp!.GetProperty("IsReadOnly", BindingFlags.Instance | BindingFlags.NonPublic)!.SetValue(editor, true);
            EditorTyp.GetProperty("TextView", BindingFlags.Instance | BindingFlags.NonPublic)!.SetValue(editor, ContainerTyp!.GetProperty("TextView")!.GetValue(container));
        }
        catch { editor = null; }
    }
}

/// <summary>Einfache Markdown-Darstellung (Überschriften, Listen, Codeblöcke, Fett/Kursiv/Links inline). Beim Streamen wird nur der geänderte Schwanz neu gebaut.</summary>
public sealed class MarkdownText : Decorator
{
    private enum Art { Absatz, Ueberschrift, Punkt, Code, Linie }
    private sealed record Block(Art Art, string Text, int Stufe = 0, string Zeichen = "");
    private readonly Stapel stapel = new(false, 8);
    private readonly List<(Block B, UIElement E)> gebaut = new();
    private readonly double groesse;
    private string text = "";

    public MarkdownText(string text = "", double groesse = 14)
    {
        this.groesse = groesse;
        Child = stapel;
        Text = text;
    }

    public string Text
    {
        get => text;
        set
        {
            if (value == text && gebaut.Count > 0) return;
            text = value ?? "";
            var neu = Bloecke(text);
            var gleich = 0;
            while (gleich < neu.Count && gleich < gebaut.Count && neu[gleich] == gebaut[gleich].B) gleich++;
            // Der letzte Block wächst beim Streamen: Er kommt nicht in den Vorrat.
            for (var i = gebaut.Count - 1; i >= gleich; i--) { stapel.Children.RemoveAt(i); gebaut.RemoveAt(i); }
            for (var i = gleich; i < neu.Count; i++)
            {
                var e = Baue(neu[i], merken: i < neu.Count - 1);
                gebaut.Add((neu[i], e));
                stapel.Children.Add(e);
            }
        }
    }

    private static List<Block> Bloecke(string text)
    {
        var r = new List<Block>();
        List<string>? code = null;
        var absatz = new List<string>();
        void AbsatzFertig() { if (absatz.Count > 0) { r.Add(new Block(Art.Absatz, string.Join("\n", absatz))); absatz.Clear(); } }
        foreach (var zeileRoh in text.Split('\n'))
        {
            var zeile = zeileRoh.TrimEnd('\r');
            var z = zeile.Trim(' ', '\t');
            if (z.StartsWith("```", StringComparison.Ordinal))
            {
                if (code != null) { r.Add(new Block(Art.Code, string.Join("\n", code))); code = null; }
                else { AbsatzFertig(); code = new(); }
                continue;
            }
            if (code != null) { code.Add(zeile); continue; }
            if (z.StartsWith("<!--", StringComparison.Ordinal) && z.EndsWith("-->", StringComparison.Ordinal)) continue;
            if (z.Length == 0) { AbsatzFertig(); continue; }
            if (z[0] == '#')
            {
                AbsatzFertig();
                var stufe = z.TakeWhile(c => c == '#').Count();
                r.Add(new Block(Art.Ueberschrift, z[stufe..].Trim(' ', '\t'), stufe));
                continue;
            }
            if (z is "---" or "***") { AbsatzFertig(); r.Add(new Block(Art.Linie, "")); continue; }
            if (z.StartsWith("- ", StringComparison.Ordinal) || z.StartsWith("* ", StringComparison.Ordinal) || z.StartsWith("• ", StringComparison.Ordinal))
            {
                AbsatzFertig();
                r.Add(new Block(Art.Punkt, z[2..], 0, "•"));
                continue;
            }
            var p = z.IndexOf('.');
            if (p > 0 && z[..p].All(char.IsDigit) && p + 1 < z.Length && z[p + 1] == ' ')
            {
                AbsatzFertig();
                r.Add(new Block(Art.Punkt, z[(p + 2)..], 0, z[..(p + 1)]));
                continue;
            }
            absatz.Add(zeile);
        }
        if (code != null) r.Add(new Block(Art.Code, string.Join("\n", code)));
        AbsatzFertig();
        return r;
    }

    private UIElement Baue(Block b, bool merken)
    {
        switch (b.Art)
        {
            case Art.Absatz:
                {
                    var t = Inline(b.Text, groesse, FontWeights.Regular, merken);
                    t.LineStackingStrategy = LineStackingStrategy.BlockLineHeight;
                    t.LineHeight = groesse * 1.3 + 3;
                    return t;
                }
            case Art.Ueberschrift:
                {
                    var t = Inline(b.Text, b.Stufe <= 1 ? groesse + 6 : (b.Stufe == 2 ? groesse + 3 : groesse + 1), FontWeights.Bold, true);
                    t.Margin = new Thickness(0, 4, 0, 0);
                    return t;
                }
            case Art.Punkt:
                {
                    var marke = Ui.Tx(b.Zeichen, groesse, FontWeights.Bold, Farbe.Haupt);
                    return Ui.HOben(8, marke, Inline(b.Text, groesse, FontWeights.Regular, merken).Fuell());
                }
            case Art.Code:
                {
                    var t = new WaehlbarerText { Text = b.Text, FontFamily = Ui.Mono, FontSize = groesse - 1, Foreground = Farbe.Text, Margin = new Thickness(12) };
                    return new Border { Background = Farbe.Tiefe(0.35), CornerRadius = new CornerRadius(10), Child = new Scroller(t, waagerecht: true) };
                }
            default:
                return new Border { Height = 1, Background = Farbe.Linie };
        }
    }

    // --- Inline ---
    private sealed record Lauf(string Text, bool Fett, bool Kursiv, bool Code, bool Durch, string? Link);
    private static readonly Dictionary<string, List<Lauf>> Vorrat = new();
    private static long vorratGroesse;

    private static List<Lauf> Zerlegt(string s, bool merken)
    {
        if (Vorrat.TryGetValue(s, out var da)) return da;
        var neu = new List<Lauf>();
        Zerlege(s, false, false, false, null, neu);
        if (merken)
        {
            if (vorratGroesse > 8_000_000) { Vorrat.Clear(); vorratGroesse = 0; }
            Vorrat[s] = neu;
            vorratGroesse += s.Length;
        }
        return neu;
    }

    private static void Zerlege(string s, bool fett, bool kursiv, bool durch, string? link, List<Lauf> aus)
    {
        var puffer = new System.Text.StringBuilder();
        void Raus() { if (puffer.Length > 0) { aus.Add(new Lauf(puffer.ToString(), fett, kursiv, false, durch, link)); puffer.Clear(); } }
        var i = 0;
        while (i < s.Length)
        {
            var c = s[i];
            if (c == '\\' && i + 1 < s.Length && "\\`*_~[]()#!".Contains(s[i + 1])) { puffer.Append(s[i + 1]); i += 2; continue; }
            if (c == '`')
            {
                var e = s.IndexOf('`', i + 1);
                if (e > i) { Raus(); aus.Add(new Lauf(s[(i + 1)..e], fett, kursiv, true, durch, link)); i = e + 1; continue; }
            }
            if (c == '[')
            {
                var zu = s.IndexOf(']', i + 1);
                if (zu > i && zu + 1 < s.Length && s[zu + 1] == '(')
                {
                    var ende = s.IndexOf(')', zu + 2);
                    if (ende > zu)
                    {
                        Raus();
                        Zerlege(s[(i + 1)..zu], fett, kursiv, durch, s[(zu + 2)..ende].Trim(), aus);
                        i = ende + 1;
                        continue;
                    }
                }
            }
            if ((c == '*' || c == '_') && i + 1 < s.Length && s[i + 1] == c)
            {
                var marke = new string(c, 2);
                var e = s.IndexOf(marke, i + 2, StringComparison.Ordinal);
                if (e > i + 2 && !char.IsWhiteSpace(s[i + 2]) && (c == '*' || WortGrenze(s, i, e + 1)))
                {
                    Raus();
                    Zerlege(s[(i + 2)..e], true, kursiv, durch, link, aus);
                    i = e + 2;
                    continue;
                }
            }
            if (c == '*' || c == '_')
            {
                var e = i + 1 < s.Length ? s.IndexOf(c, i + 1) : -1;
                if (e > i + 1 && !char.IsWhiteSpace(s[i + 1]) && !char.IsWhiteSpace(s[e - 1]) && (c == '*' || WortGrenze(s, i, e)))
                {
                    Raus();
                    Zerlege(s[(i + 1)..e], fett, true, durch, link, aus);
                    i = e + 1;
                    continue;
                }
            }
            if (c == '~' && i + 1 < s.Length && s[i + 1] == '~')
            {
                var e = s.IndexOf("~~", i + 2, StringComparison.Ordinal);
                if (e > i + 2)
                {
                    Raus();
                    Zerlege(s[(i + 2)..e], fett, kursiv, true, link, aus);
                    i = e + 2;
                    continue;
                }
            }
            puffer.Append(c);
            i++;
        }
        Raus();
    }

    /// <summary>Unterstriche innerhalb von Wörtern (datei_name_alt) sind keine Betonung.</summary>
    private static bool WortGrenze(string s, int anfang, int ende) =>
        (anfang == 0 || !char.IsLetterOrDigit(s[anfang - 1])) && (ende + 1 >= s.Length || !char.IsLetterOrDigit(s[ende + 1]));

    private static TextBlock Inline(string s, double groesse, FontWeight grund, bool merken)
    {
        var t = new WaehlbarerText { FontFamily = Ui.Schrift, FontSize = groesse, FontWeight = grund, Foreground = Farbe.Text, TextWrapping = TextWrapping.Wrap };
        foreach (var l in Zerlegt(s, merken))
        {
            var r = new Run(l.Text);
            if (l.Fett) r.FontWeight = FontWeights.Bold;
            if (l.Kursiv) r.FontStyle = FontStyles.Italic;
            if (l.Code) { r.FontFamily = Ui.Mono; r.FontSize = groesse - 0.5; }
            if (l.Durch) r.TextDecorations = TextDecorations.Strikethrough;
            if (l.Link != null)
            {
                var ziel = l.Link;
                var h = new Hyperlink(r) { Foreground = Farbe.Haupt };
                h.PreviewMouseLeftButtonDown += (_, e) =>
                {
                    e.Handled = true;
                    if (ziel.StartsWith("http", StringComparison.OrdinalIgnoreCase) || ziel.StartsWith("mailto:", StringComparison.OrdinalIgnoreCase))
                        try { Process.Start(new ProcessStartInfo(ziel) { UseShellExecute = true }); } catch { }
                };
                t.Inlines.Add(h);
            }
            else t.Inlines.Add(r);
        }
        return t;
    }
}

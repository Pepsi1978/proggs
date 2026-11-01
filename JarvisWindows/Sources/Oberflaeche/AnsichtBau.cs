using System.Collections.Specialized;
using System.ComponentModel;
using System.Diagnostics;
using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using System.Windows.Threading;

namespace JarvisWindows;

/// <summary>Entspricht @Binding für Text: Lesen, Schreiben und eine Meldung, wenn sich der Wert von außen geändert hat.</summary>
public sealed class TextBindung
{
    private readonly Func<string> lies;
    private readonly Action<string> setze;
    public event Action? Geaendert;
    public TextBindung(Func<string> lies, Action<string> setze) { this.lies = lies; this.setze = setze; }
    public string Wert { get => lies(); set { if (lies() == value) return; setze(value); Geaendert?.Invoke(); } }
    public void Melde() => Geaendert?.Invoke();
}

/// <summary>Entspricht einer SwiftUI-Ansicht mit @State/@Observable: Baue() läuft neu, sobald sich eine beobachtete Quelle ändert (zusammengefasst je Takt).</summary>
public abstract class LebendeAnsicht : UserControl
{
    private readonly List<(object Quelle, string[] Namen)> quellen = new();
    private bool an, geplant;

    protected LebendeAnsicht()
    {
        Loaded += (_, _) => { if (an) return; an = true; foreach (var q in quellen) Haenge(q.Quelle, true); Content = Baue(); Erschienen(); };
        Unloaded += (_, _) => { if (!an) return; an = false; foreach (var q in quellen) Haenge(q.Quelle, false); Verschwunden(); };
    }

    protected virtual void Erschienen() { }
    protected virtual void Verschwunden() { }
    protected abstract UIElement Baue();

    /// <summary>Quelle kann INotifyPropertyChanged oder eine Sammlung sein. Ohne Namen löst jede Änderung aus.</summary>
    protected void Beobachte(object? quelle, params string[] namen) { if (quelle != null) quellen.Add((quelle, namen)); }

    private void Haenge(object q, bool dran)
    {
        if (q is INotifyPropertyChanged p) { if (dran) p.PropertyChanged += Geaendert; else p.PropertyChanged -= Geaendert; }
        if (q is INotifyCollectionChanged c) { if (dran) c.CollectionChanged += SammlungGeaendert; else c.CollectionChanged -= SammlungGeaendert; }
    }

    private void Geaendert(object? s, PropertyChangedEventArgs e)
    {
        foreach (var q in quellen)
            if (ReferenceEquals(q.Quelle, s) && (q.Namen.Length == 0 || e.PropertyName == null || q.Namen.Contains(e.PropertyName))) { Neu(); return; }
    }
    private void SammlungGeaendert(object? s, NotifyCollectionChangedEventArgs e) => Neu();

    public void Neu()
    {
        if (geplant) return;
        geplant = true;
        Dispatcher.BeginInvoke(DispatcherPriority.DataBind, () => { geplant = false; if (an) Content = Baue(); });
    }
}

/// <summary>Gleich breite Spalten ab einer Mindestbreite (GridItem(.adaptive(minimum:))).</summary>
public sealed class AdaptivGitter : Panel
{
    public double Minimum { get; set; } = 150;
    public double Abstand { get; set; } = 10;
    private (int Spalten, double Breite) Teilung(double w)
    {
        if (double.IsInfinity(w)) w = Minimum;
        var n = Math.Max(1, (int)Math.Floor((w + Abstand) / (Minimum + Abstand)));
        return (n, Math.Max(0, (w - Abstand * (n - 1)) / n));
    }
    protected override Size MeasureOverride(Size verfuegbar)
    {
        var (n, b) = Teilung(verfuegbar.Width);
        double hoehe = 0, zeile = 0; int i = 0;
        foreach (UIElement k in InternalChildren)
        {
            k.Measure(new Size(b, double.PositiveInfinity));
            zeile = Math.Max(zeile, k.DesiredSize.Height);
            if (++i % n == 0) { hoehe += zeile + Abstand; zeile = 0; }
        }
        if (i % n != 0) hoehe += zeile; else if (i > 0) hoehe -= Abstand;
        return new Size(double.IsInfinity(verfuegbar.Width) ? b : verfuegbar.Width, hoehe);
    }
    protected override Size ArrangeOverride(Size endgroesse)
    {
        var (n, b) = Teilung(endgroesse.Width);
        double y = 0; int i = 0;
        while (i < InternalChildren.Count)
        {
            double zeile = 0;
            for (var j = i; j < Math.Min(i + n, InternalChildren.Count); j++) zeile = Math.Max(zeile, InternalChildren[j].DesiredSize.Height);
            for (var j = i; j < Math.Min(i + n, InternalChildren.Count); j++) InternalChildren[j].Arrange(new Rect((j - i) * (b + Abstand), y, b, zeile));
            y += zeile + Abstand;
            i += n;
        }
        return endgroesse;
    }
}

/// <summary>Toggle(.switch) in den Farben des Designs.</summary>
public sealed class Schalter : Border
{
    private readonly Ellipse punkt = new() { Width = 18, Height = 18, Fill = Brushes.White, HorizontalAlignment = HorizontalAlignment.Left, VerticalAlignment = VerticalAlignment.Center };
    private readonly Func<bool> lies;
    private readonly Action<bool> setze;
    public Schalter(Func<bool> lies, Action<bool> setze)
    {
        this.lies = lies; this.setze = setze;
        Width = 40; Height = 22; CornerRadius = new CornerRadius(11); Cursor = Cursors.Hand; HorizontalAlignment = HorizontalAlignment.Left; VerticalAlignment = VerticalAlignment.Center;
        Child = punkt;
        Zeichne();
        MouseLeftButtonUp += (_, e) => { setze(!lies()); Zeichne(); e.Handled = true; };
    }
    private void Zeichne()
    {
        var w = lies();
        Background = w ? Bau.B(Farbe.Haupt) : Bau.B(Farbe.Glas(0.18));
        punkt.Margin = new Thickness(w ? 20 : 2, 0, 0, 0);
    }
}

public sealed record MenueZeile(string Titel, Action? Aktion = null, bool Linie = false, bool Haken = false)
{
    public static readonly MenueZeile Trenner = new("", null, true);
}

/// <summary>Bausteine der Oberfläche. Alle Zugriffe auf das Design-Kit laufen hier durch.</summary>
public static class Bau
{
    public static readonly FontFamily Schrift = new("Segoe UI Variable Text, Segoe UI");
    public static readonly FontFamily Mono = new("Cascadia Mono, Consolas");
    public static readonly FontFamily Symbole = new("Segoe Fluent Icons, Segoe MDL2 Assets");
    public static readonly CultureInfo Deutsch = CultureInfo.GetCultureInfo("de-DE");
    private const string FuellMarke = "fuell";

    public static Brush B(object? farbe) => farbe switch
    {
        Brush b => b,
        Color c => new SolidColorBrush(c),
        _ => Brushes.Transparent,
    };

    /// <summary>Text(...).font(.jarvis(g, w)).foregroundStyle(f). zeilen = 1 entspricht lineLimit(1).</summary>
    public static TextBlock T(string text, double groesse = 13, FontWeight? gewicht = null, object? farbe = null, int zeilen = 0, FontFamily? schrift = null)
    {
        var t = new TextBlock
        {
            Text = text, FontSize = groesse, FontFamily = schrift ?? Schrift, FontWeight = gewicht ?? FontWeights.Normal,
            Foreground = B(farbe ?? Farbe.Text), TextWrapping = zeilen == 1 ? TextWrapping.NoWrap : TextWrapping.Wrap,
            TextTrimming = zeilen > 0 ? TextTrimming.CharacterEllipsis : TextTrimming.None,
        };
        if (zeilen > 1) t.MaxHeight = groesse * 1.4 * zeilen;
        return t;
    }

    public static TextBlock Sy(string symbol, double groesse = 13, object? farbe = null, FontWeight? gewicht = null) => new()
    {
        Text = Sym.Zeichen(symbol), FontFamily = Symbole, FontSize = groesse, FontWeight = gewicht ?? FontWeights.Normal, Foreground = B(farbe ?? Farbe.Text),
        VerticalAlignment = VerticalAlignment.Center, HorizontalAlignment = HorizontalAlignment.Center, TextAlignment = TextAlignment.Center,
    };

    /// <summary>VStack(spacing:). null wird übersprungen.</summary>
    public static StackPanel V(double abstand, params UIElement?[] kinder)
    {
        var s = new StackPanel { Orientation = Orientation.Vertical };
        foreach (var k in kinder) VAnfuegen(s, k, abstand);
        return s;
    }
    public static void VAnfuegen(StackPanel s, UIElement? k, double abstand)
    {
        if (k == null) return;
        if (k is FrameworkElement f && s.Children.Count > 0) { var m = f.Margin; f.Margin = new Thickness(m.Left, m.Top + abstand, m.Right, m.Bottom); }
        s.Children.Add(k);
    }

    public static FrameworkElement Fuell(FrameworkElement e) { e.Tag = FuellMarke; return e; }
    public static FrameworkElement Spacer(double min = 0) => new Border { Tag = FuellMarke, MinWidth = min, Background = null };

    /// <summary>HStack(spacing:). Fuell(...) und Spacer() nehmen den freien Platz, alles andere seine eigene Breite.</summary>
    public static Grid H(double abstand, params UIElement?[] kinder) => HAusgerichtet(abstand, VerticalAlignment.Center, kinder);
    public static Grid HOben(double abstand, params UIElement?[] kinder) => HAusgerichtet(abstand, VerticalAlignment.Top, kinder);
    private static Grid HAusgerichtet(double abstand, VerticalAlignment ausrichtung, UIElement?[] kinder)
    {
        var g = new Grid();
        var i = 0;
        foreach (var k in kinder)
        {
            if (k == null) continue;
            var fuell = k is FrameworkElement m && m.Tag as string == FuellMarke;
            g.ColumnDefinitions.Add(new ColumnDefinition { Width = fuell ? new GridLength(1, GridUnitType.Star) : GridLength.Auto });
            if (k is FrameworkElement f)
            {
                if (i > 0) { var r = f.Margin; f.Margin = new Thickness(r.Left + abstand, r.Top, r.Right, r.Bottom); }
                if (f.ReadLocalValue(FrameworkElement.VerticalAlignmentProperty) == DependencyProperty.UnsetValue) f.VerticalAlignment = ausrichtung;
            }
            Grid.SetColumn(k, i++);
            g.Children.Add(k);
        }
        return g;
    }

    public static T1 Rand<T1>(T1 e, double l = 0, double o = 0, double r = 0, double u = 0) where T1 : FrameworkElement
    {
        var m = e.Margin;
        e.Margin = new Thickness(m.Left + l, m.Top + o, m.Right + r, m.Bottom + u);
        return e;
    }

    /// <summary>.padding().background(RoundedRectangle().fill()).</summary>
    public static Border Flaeche(UIElement? inhalt, object? farbe, double ecke = 0, double innenH = 0, double innenV = 0, object? rand = null, double randBreite = 1) => new()
    {
        Child = inhalt, Background = B(farbe), CornerRadius = new CornerRadius(ecke), Padding = new Thickness(innenH, innenV, innenH, innenV),
        BorderBrush = rand == null ? null : B(rand), BorderThickness = rand == null ? new Thickness(0) : new Thickness(randBreite),
    };

    public static Ellipse Kreis(object farbe, double d = 8) => new() { Width = d, Height = d, Fill = B(farbe), VerticalAlignment = VerticalAlignment.Center };

    private static readonly ControlTemplate KnopfVorlage = BaueVorlage();
    private static ControlTemplate BaueVorlage()
    {
        var v = new ControlTemplate(typeof(Button)) { VisualTree = new FrameworkElementFactory(typeof(ContentPresenter)) };
        v.Seal();
        return v;
    }

    /// <summary>Button mit .buttonStyle(.plain): ganze Fläche klickbar, kein eigenes Aussehen.</summary>
    public static Button Klick(UIElement inhalt, Action aktion, string? hilfe = null, bool hand = false)
    {
        var b = new Button { Template = KnopfVorlage, Focusable = false, Content = new Border { Background = Brushes.Transparent, Child = inhalt }, Cursor = hand ? Cursors.Hand : null };
        if (!string.IsNullOrEmpty(hilfe)) b.ToolTip = hilfe;
        b.Click += (_, _) => aktion();
        return b;
    }

    public static Button TextKnopf(string titel, double groesse, object farbe, Action aktion, FontWeight? gewicht = null) => Klick(T(titel, groesse, gewicht, farbe), aktion);

    /// <summary>Kachel, Zeile oder Eintrag mit Schwebe-Färbung.</summary>
    public static void Schweben(Border b, object? normal, object? schwebt)
    {
        b.Background = B(normal);
        b.MouseEnter += (_, _) => b.Background = B(schwebt);
        b.MouseLeave += (_, _) => b.Background = B(normal);
    }

    // MARK: Design-Kit und Bausteine der anderen Bereiche

    public static GlasKarte Karte(UIElement inhalt, double abstand = 18, double ecke = 22) => new GlasKarte(inhalt, ecke: ecke, abstand: abstand);
    public static UIElement Titel(string symbol, string titel, object? farbe = null) => new KartenTitel(symbol, titel, farbe == null ? Farbe.Haupt : farbe);
    public static UIElement Rund(string symbol, Action aktion, string hilfe = "", bool aktiv = false, object? farbe = null, double groesse = 34)
        => new RundKnopf(symbol, aktion, hilfe: hilfe, aktiv: aktiv, farbe: farbe == null ? Farbe.Haupt : farbe, groesse: groesse);
    public static UIElement Kapsel(string symbol, string titel, Action aktion, bool hervorgehoben = false) => new KapselKnopf(symbol, titel, aktion, hervorgehoben: hervorgehoben);
    public static UIElement Klein(string symbol, string hilfe, Action aktion) => new KleinKnopf(symbol, hilfe, aktion);
    public static UIElement Md(string text, double groesse = 14) => new MarkdownText(text, groesse);

    // MARK: Eingaben

    /// <summary>ProgressView().controlSize(.small).</summary>
    public static FrameworkElement Drehkreis(double d = 16)
    {
        var bogen = new Ellipse
        {
            Width = d, Height = d, Stroke = B(Farbe.Haupt), StrokeThickness = 2, StrokeDashArray = new DoubleCollection { d * 1.1 / 2, d * 1.1 / 2 }, StrokeDashCap = PenLineCap.Round,
            RenderTransformOrigin = new Point(0.5, 0.5), VerticalAlignment = VerticalAlignment.Center, HorizontalAlignment = HorizontalAlignment.Left,
        };
        var dreh = new RotateTransform();
        bogen.RenderTransform = dreh;
        dreh.BeginAnimation(RotateTransform.AngleProperty, new DoubleAnimation(0, 360, new Duration(TimeSpan.FromSeconds(1))) { RepeatBehavior = RepeatBehavior.Forever });
        return bogen;
    }

    public static TextBox Eingabe(string? anfang = null, double groesse = 13, FontFamily? schrift = null, bool mehrzeilig = false) => new()
    {
        Text = anfang ?? "", FontSize = groesse, FontFamily = schrift ?? Schrift, Foreground = B(Farbe.Text), CaretBrush = B(Farbe.Text),
        Background = mehrzeilig ? Brushes.Transparent : B(Farbe.Tiefe(0.25)), BorderBrush = mehrzeilig ? null : B(Farbe.Glas(0.14)), BorderThickness = mehrzeilig ? new Thickness(0) : new Thickness(1),
        Padding = mehrzeilig ? new Thickness(0) : new Thickness(6, 4, 6, 4), AcceptsReturn = mehrzeilig, AcceptsTab = false, TextWrapping = mehrzeilig ? TextWrapping.Wrap : TextWrapping.NoWrap,
        VerticalScrollBarVisibility = mehrzeilig ? ScrollBarVisibility.Auto : ScrollBarVisibility.Hidden, VerticalContentAlignment = mehrzeilig ? VerticalAlignment.Top : VerticalAlignment.Center,
        SelectionBrush = B(Farbe.Haupt),
    };

    /// <summary>TextField/TextEditor an einer Bindung, mit Platzhalter. Änderungen von außen ziehen nach, ohne die Einfügemarke zu verlieren, solange der Text gleich ist.</summary>
    public static FrameworkElement Feld(TextBindung text, string platzhalter = "", double groesse = 13, bool mehrzeilig = false, double breite = double.NaN, FontFamily? schrift = null, Action<TextBox>? fertig = null)
    {
        var box = Eingabe(text.Wert, groesse, schrift, mehrzeilig);
        box.TextChanged += (_, _) => { if (box.Text != text.Wert) text.Wert = box.Text; };
        Action nach = () => { var w = text.Wert; if (box.Text != w) { box.Text = w; box.CaretIndex = w.Length; } };
        box.Loaded += (_, _) => { text.Geaendert -= nach; text.Geaendert += nach; nach(); };
        box.Unloaded += (_, _) => text.Geaendert -= nach;
        fertig?.Invoke(box);
        if (!double.IsNaN(breite)) box.Width = breite;
        if (platzhalter.Length == 0) return box;
        var hinweis = T(platzhalter, groesse, null, Farbe.Text3, 1);
        hinweis.IsHitTestVisible = false;
        hinweis.Margin = mehrzeilig ? new Thickness(2, 0, 0, 0) : new Thickness(9, 0, 0, 0);
        hinweis.VerticalAlignment = mehrzeilig ? VerticalAlignment.Top : VerticalAlignment.Center;
        hinweis.Visibility = box.Text.Length == 0 ? Visibility.Visible : Visibility.Collapsed;
        box.TextChanged += (_, _) => hinweis.Visibility = box.Text.Length == 0 ? Visibility.Visible : Visibility.Collapsed;
        var g = new Grid();
        g.Children.Add(box);
        g.Children.Add(hinweis);
        return g;
    }

    /// <summary>Stepper mit Wert links.</summary>
    public static FrameworkElement Stepper(Func<int> lies, Action<int> setze, int min, int max, int schritt, double breite = 120)
    {
        var wert = T(lies().ToString(), 13, FontWeights.Medium);
        void Aendere(int d) { var n = Math.Clamp(lies() + d, min, max); setze(n); wert.Text = n.ToString(); }
        var r = H(6, Fuell(wert), Klein("minus", "", () => Aendere(-schritt)), Klein("plus", "", () => Aendere(schritt)));
        r.Width = breite;
        return r;
    }

    /// <summary>Picker: Knopf mit gewähltem Namen, Liste im Popup.</summary>
    public static FrameworkElement Auswahl<TW>(IEnumerable<(TW Wert, string Name)> werte, Func<TW> lies, Action<TW> setze, double maxBreite = 220)
    {
        var liste = werte.ToList();
        var name = T("", 13, FontWeights.Medium, null, 1);
        void Beschrifte() { var w = lies(); var i = liste.FindIndex(x => EqualityComparer<TW>.Default.Equals(x.Wert, w)); name.Text = i >= 0 ? liste[i].Name : w?.ToString() ?? ""; }
        Beschrifte();
        var pfeil = Sy("chevron.down", 10, Farbe.Text2);
        var kopf = Flaeche(H(8, Fuell(name), pfeil), Farbe.Glas(0.07), 8, 10, 6, Farbe.Glas(0.10));
        kopf.MaxWidth = maxBreite;
        kopf.MinWidth = 90;
        kopf.HorizontalAlignment = HorizontalAlignment.Right;
        var k = Klick(kopf, () => { });
        k.Click += (_, _) => OeffneMenue(k, liste.Select(x => new MenueZeile(x.Name, () => { setze(x.Wert); Beschrifte(); }, false, EqualityComparer<TW>.Default.Equals(x.Wert, lies()))).ToList());
        k.HorizontalAlignment = HorizontalAlignment.Right;
        return k;
    }

    /// <summary>Picker(.segmented).</summary>
    public static FrameworkElement Segment(IEnumerable<(string Wert, string Name)> werte, Func<string> lies, Action<string> setze, double maxBreite = 200)
    {
        var w = new StufenWahl(werte.ToList(), lies, setze);
        w.MaxWidth = maxBreite;
        w.MinWidth = Math.Min(maxBreite, 160);
        return w;
    }

    /// <summary>Menu { … }: Liste im Popup an einem Anker; null-Aktion ist reiner Text.</summary>
    public static void OeffneMenue(FrameworkElement anker, IReadOnlyList<MenueZeile> zeilen)
    {
        var p = new Popup { PlacementTarget = anker, Placement = PlacementMode.Bottom, StaysOpen = false, AllowsTransparency = true, PopupAnimation = PopupAnimation.None };
        var s = new StackPanel { MinWidth = Math.Max(anker.ActualWidth, 160) };
        foreach (var z in zeilen)
        {
            if (z.Linie) { s.Children.Add(new Border { Height = 1, Background = B(Farbe.Linie), Margin = new Thickness(6, 5, 6, 5) }); continue; }
            var r = H(10, Fuell(T(z.Titel, 13, null, z.Aktion == null ? Farbe.Text2 : Farbe.Text, 1)), z.Haken ? Sy("checkmark", 11, Farbe.Haupt) : null);
            var b = Flaeche(r, null, 6, 10, 6);
            if (z.Aktion == null) { s.Children.Add(b); continue; }
            Schweben(b, Brushes.Transparent, Farbe.Glas(0.09));
            var k = Klick(b, () => { p.IsOpen = false; z.Aktion(); });
            s.Children.Add(k);
        }
        var rand = Flaeche(new ScrollViewer { Content = s, MaxHeight = 420, VerticalScrollBarVisibility = ScrollBarVisibility.Auto }, Farbe.Grund2, 10, 4, 4, Farbe.Glas(0.14));
        p.Child = rand;
        p.IsOpen = true;
    }

    /// <summary>.popover: beliebiger Inhalt im Popup.</summary>
    public static void OeffnePopup(FrameworkElement anker, UIElement inhalt)
    {
        var p = new Popup { PlacementTarget = anker, Placement = PlacementMode.Bottom, StaysOpen = false, AllowsTransparency = true };
        p.Child = Flaeche(inhalt, Farbe.Grund2, 12, 0, 0, Farbe.Glas(0.14));
        p.IsOpen = true;
    }

    /// <summary>Capsule-Balken: Hintergrund Farbe.Glas(0.08), Anteil im Akzent oder in der übergebenen Farbe.</summary>
    public static FrameworkElement Balken(double anteil, object? farbe = null)
    {
        anteil = Math.Clamp(anteil, 0, 1);
        var g = new Grid { Height = 8 };
        g.Children.Add(new Border { CornerRadius = new CornerRadius(4), Background = B(Farbe.Glas(0.08)) });
        var inner = new Grid();
        inner.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(anteil, GridUnitType.Star) });
        inner.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1 - anteil, GridUnitType.Star) });
        inner.Children.Add(new Border { CornerRadius = new CornerRadius(4), Background = B(farbe ?? Farbe.Akzent) });
        g.Children.Add(inner);
        return g;
    }

    // MARK: Dialoge

    /// <summary>.confirmationDialog / .alert: true = Bestätigt. Liegt mittig über dem aktiven Fenster.</summary>
    public static bool Bestaetigen(string titel, string nachricht, string knopf, string abbrechen = "Abbrechen")
    {
        var fenster = new Window
        {
            WindowStyle = WindowStyle.None, ResizeMode = ResizeMode.NoResize, AllowsTransparency = true, Background = Brushes.Transparent, ShowInTaskbar = false,
            SizeToContent = SizeToContent.WidthAndHeight, Owner = AktivesFenster(), WindowStartupLocation = WindowStartupLocation.CenterOwner,
        };
        if (fenster.Owner == null) fenster.WindowStartupLocation = WindowStartupLocation.CenterScreen;
        var ja = false;
        var knoepfe = H(10, Spacer(), Kapsel("xmark", abbrechen, () => fenster.Close()), Kapsel("checkmark", knopf, () => { ja = true; fenster.Close(); }, hervorgehoben: true));
        var inhalt = V(10, T(titel, 15, FontWeights.SemiBold), nachricht.Length > 0 ? T(nachricht, 13, null, Farbe.Text2) : null, Rand(knoepfe, o: 8));
        var rand = Flaeche(inhalt, Farbe.Grund2, 16, 22, 20, Farbe.Glas(0.16));
        rand.Width = 420;
        fenster.Content = rand;
        fenster.KeyDown += (_, e) => { if (e.Key == Key.Escape) fenster.Close(); };
        fenster.ShowDialog();
        return ja;
    }

    public static Window? AktivesFenster()
    {
        var app = Application.Current;
        if (app == null) return null;
        return app.Windows.OfType<Window>().FirstOrDefault(w => w.IsActive) ?? app.MainWindow;
    }

    // MARK: Hilfen

    /// <summary>NSWorkspace.open.</summary>
    public static void Oeffne(object? ziel)
    {
        var s = ziel switch { Uri u => u.IsFile ? u.LocalPath : u.AbsoluteUri, null => "", _ => ziel.ToString() ?? "" };
        if (s.Length == 0) return;
        try { using var _ = Process.Start(new ProcessStartInfo(s) { UseShellExecute = true }); }
        catch (Exception ex) { Log("fehler", "Öffnen gescheitert: " + s, wo: "Oberfläche", fehler: ex.Message); }
    }

    public static void Kopiere(string text)
    {
        // Die Zwischenablage kann kurz von einem anderen Programm belegt sein.
        for (var i = 0; i < 5; i++)
        {
            try { Clipboard.SetText(text); return; } catch (System.Runtime.InteropServices.COMException) { Thread.Sleep(40); }
        }
    }

    /// <summary>Ein Datei-Ergebnis eines anderen Bereichs (Pfad, Uri oder FileInfo) als Pfad.</summary>
    public static string Pfad(object? datei) => datei switch { Uri u => u.LocalPath, FileInfo f => f.FullName, _ => datei?.ToString() ?? "" };
}

using System.ComponentModel;
using System.Collections.Specialized;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Shell;
using System.Windows.Threading;
using static JarvisWindows.Ui;

namespace JarvisWindows;

public sealed class HauptFenster : Window
{
    public static HauptFenster? Aktuell { get; private set; }
    public static HauptFenster Anlegen()
    {
        if (Aktuell != null) return Aktuell;
        Aktuell = new HauptFenster();
        // Sonst wäre die Sprachpille oder die Menüleiste das Hauptfenster, wenn sie zuerst da waren.
        Application.Current.MainWindow = Aktuell;
        return Aktuell;
    }

    private readonly Grid wurzel = new();
    private readonly Grid inhaltsEbene = new();
    private readonly Grid dialogEbene = new();
    private readonly Decorator bereichPlatz = new();
    private Decorator schwebePlatz = new();
    private (Bereich, Guid?)? gezeigt;
    private long? promptGezeigt;
    private Grid? promptEbene;
    private Border? groesseKnopf;
    /// <summary>War das Fenster vor dem Minimieren maximiert?</summary>
    public bool VorherGross { get; private set; }

    private HauptFenster()
    {
        Title = AppInfo.Name;
        Width = 1280; Height = 820;
        MinWidth = 980; MinHeight = 640;
        WindowStartupLocation = WindowStartupLocation.CenterScreen;
        WindowState = WindowState.Maximized;   // Franks Vorgabe: Jarvis startet immer über den ganzen Bildschirm.
        FontFamily = Ui.Schrift;
        Background = Brushes.Black;
        UseLayoutRounding = true;
        Icon = FensterSymbol();
        // Entspricht .hiddenTitleBar: Der Inhalt reicht bis oben, die Knöpfe liegen darüber.
        WindowChrome.SetWindowChrome(this, new WindowChrome { CaptionHeight = 30, ResizeBorderThickness = new Thickness(6), CornerRadius = new CornerRadius(0), GlassFrameThickness = new Thickness(1), UseAeroCaptionButtons = false });
        Content = wurzel;
        wurzel.Children.Add(inhaltsEbene);
        dialogEbene.Visibility = Visibility.Collapsed;
        wurzel.Children.Add(dialogEbene);
        Bauen();
        Activated += (_, _) => AppSteuerung.Shared.FensterZustandGeaendert();
        Deactivated += (_, _) => AppSteuerung.Shared.FensterZustandGeaendert();
        StateChanged += (_, _) =>
        {
            if (WindowState != WindowState.Minimized) VorherGross = WindowState == WindowState.Maximized;
            // Maximiert ragt das Fenster um den Rahmen über den Schirm hinaus.
            wurzel.Margin = WindowState == WindowState.Maximized ? new Thickness(7) : new Thickness(0);
            KnopfSymbol();
            AppSteuerung.Shared.FensterZustandGeaendert();
        };
        SourceInitialized += (_, _) => DwmAbgleich();
        Closing += (_, e) =>
        {
            // Wie am Mac: Schließen beendet die App nicht.
            e.Cancel = true;
            Hide();
            TresorSperre.Shared.Sperren();
            AppSteuerung.Shared.FensterZustandGeaendert();
        };
        PreviewKeyDown += TasteGedrueckt;
        Loaded += (_, _) =>
        {
            AppSteuerung.Shared.PropertyChanged += SteuerungGeaendert;
            DesignStand.Shared.PropertyChanged += DesignGeaendert;
        };
    }

    private static ImageSource FensterSymbol()
    {
        var pfad = Pfade.Ressource("AppIcon.ico");
        if (File.Exists(pfad)) { var d = BitmapFrame.Create(new Uri(pfad)); d.Freeze(); return d; }
        var v = new DrawingVisual();
        using (var c = v.RenderOpen())
        {
            var b = new RadialGradientBrush(Color.FromRgb(255, 236, 200), Color.FromRgb(255, 140, 26)) { GradientOrigin = new Point(0.42, 0.38), Center = new Point(0.42, 0.38), RadiusX = 0.6, RadiusY = 0.6 };
            c.DrawEllipse(b, null, new Point(32, 32), 30, 30);
        }
        var r = new RenderTargetBitmap(64, 64, 96, 96, PixelFormats.Pbgra32);
        r.Render(v);
        r.Freeze();
        return r;
    }

    private void DwmAbgleich()
    {
        var griff = new WindowInteropHelper(this).Handle;
        if (griff == IntPtr.Zero) return;
        var dunkel = DesignStand.Shared.Dunkel ? 1 : 0;
        Nativ.DwmSetWindowAttribute(griff, Nativ.DWMWA_USE_IMMERSIVE_DARK_MODE, ref dunkel, 4);
        var rund = 2;
        Nativ.DwmSetWindowAttribute(griff, Nativ.DWMWA_WINDOW_CORNER_PREFERENCE, ref rund, 4);
    }

    private void DesignGeaendert(object? s, PropertyChangedEventArgs e)
    {
        if (e.PropertyName != nameof(DesignStand.Dunkel)) return;
        DwmAbgleich();
        Bauen(); // Ein Wechsel Hell/Dunkel baut alles neu; der Zustand liegt in AppSteuerung und bleibt.
    }

    private void Bauen()
    {
        Foreground = Farbe.Text;
        gezeigt = null;
        promptGezeigt = null;
        inhaltsEbene.Children.Clear();
        inhaltsEbene.Children.Add(new JarvisHintergrund());
        var zeile = new Grid();
        zeile.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(272) });
        zeile.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1) });
        zeile.ColumnDefinitions.Add(new ColumnDefinition());
        zeile.Children.Add(new Seitenleiste());
        var linie = new Border { Background = Farbe.Linie };
        Grid.SetColumn(linie, 1);
        zeile.Children.Add(linie);
        var rechts = new Grid { ClipToBounds = true };
        Grid.SetColumn(rechts, 2);
        bereichPlatz.Child = null;
        if (bereichPlatz.Parent is Grid alt) alt.Children.Remove(bereichPlatz);
        rechts.Children.Add(bereichPlatz);
        // Wie am Handy frei über dem Inhalt, nur dort, wo die Blase steht: Sie kosten keinen Platz.
        schwebePlatz = new Decorator { HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Bottom, Child = new SchwebeKnoepfe() };
        rechts.Children.Add(schwebePlatz);
        zeile.Children.Add(rechts);
        inhaltsEbene.Children.Add(zeile);
        promptEbene = new Grid { Visibility = Visibility.Collapsed };
        inhaltsEbene.Children.Add(promptEbene);
        inhaltsEbene.Children.Add(FensterKnoepfe());
        BereichAbgleich();
        SchwebeAbgleich();
        PromptAbgleich();
    }

    /// <summary>Knöpfe statt der Ampel: Minimieren, Maximieren, Schließen. Stehen im Chrome, bleiben aber klickbar.</summary>
    private FrameworkElement FensterKnoepfe()
    {
        Border Knopf(string zeichen, Action a, bool rot = false)
        {
            var b = new Border { Width = 46, Height = 30, Child = new TextBlock { Text = zeichen, FontFamily = Sym.Schrift, FontSize = 10, Foreground = Farbe.Text2, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center } };
            b.SchwebeFlaeche(Farbe.Klar, rot ? Farbe.Rot : Farbe.Glas(0.10), 100);
            b.Klick(a);
            WindowChrome.SetIsHitTestVisibleInChrome(b, true);
            return b;
        }
        groesseKnopf = Knopf("", () => WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized);
        var reihe = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Top };
        reihe.Children.Add(Knopf("", () => WindowState = WindowState.Minimized));
        reihe.Children.Add(groesseKnopf);
        reihe.Children.Add(Knopf("", Close, rot: true));
        KnopfSymbol();
        return reihe;
    }

    private void KnopfSymbol()
    {
        if (groesseKnopf?.Child is TextBlock t) t.Text = WindowState == WindowState.Maximized ? "" : "";
    }

    private void SteuerungGeaendert(object? s, PropertyChangedEventArgs e)
    {
        switch (e.PropertyName)
        {
            case nameof(AppSteuerung.Bereich):
            case nameof(AppSteuerung.Gespraech):
                BereichAbgleich();
                SchwebeAbgleich();
                break;
            case nameof(AppSteuerung.BlaseHoehe):
                SchwebeAbgleich();
                break;
            case nameof(AppSteuerung.PromptBearbeiten):
                PromptAbgleich();
                break;
            case nameof(AppSteuerung.PromptLoeschenFrage):
                if (AppSteuerung.Shared.PromptLoeschenFrage is { } p)
                    // Hier und nicht in der Prompt-Liste: Das Senden verlässt die Ablage. Abbrechen gilt als Nein, der Prompt bleibt.
                    Dialog.Zeige("Soll der Prompt gelöscht werden?", "Der Text in der Eingabe bleibt erhalten.",
                        new DialogKnopf("Nein", () => AppSteuerung.Shared.PromptLoeschenFrage = null, Abbruch: true),
                        new DialogKnopf("Ja", () => { AppSteuerung.Shared.PromptLoeschenFrage = null; AppSteuerung.Shared.GesendetenPromptLoeschen(p); }, Destruktiv: true));
                break;
            case nameof(AppSteuerung.PromptLoeschenFehler):
                if (AppSteuerung.Shared.PromptLoeschenFehler is { } f)
                    Dialog.Zeige("Prompt nicht gelöscht", f, new DialogKnopf("OK", () => AppSteuerung.Shared.PromptLoeschenFehler = null, Abbruch: true));
                break;
        }
    }

    private void BereichAbgleich()
    {
        var s = AppSteuerung.Shared;
        (Bereich, Guid?) soll = (s.Bereich, s.Bereich == Bereich.Gespraech ? s.Gespraech?.Id : null);
        if (gezeigt == soll) return;
        gezeigt = soll;
        UIElement neu = s.Bereich switch
        {
            Bereich.Start => new StartAnsicht(),
            Bereich.Zentrale => new ZentraleAnsicht(),
            Bereich.Biomarker => new BiomarkerAnsicht(),
            Bereich.Ablage => new AblageAnsicht(),
            Bereich.Gespraech => s.Gespraech is { } g ? new ChatAnsicht(g) : new StartAnsicht(),
            _ => new EinstellungenAnsicht(),
        };
        bereichPlatz.Child = neu;
        neu.BeginAnimation(OpacityProperty, new DoubleAnimation(0, 1, new Duration(TimeSpan.FromMilliseconds(180))) { EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseInOut } });
    }

    private void SchwebeAbgleich()
    {
        var s = AppSteuerung.Shared;
        schwebePlatz.Visibility = s.Bereich is Bereich.Start or Bereich.Gespraech ? Visibility.Visible : Visibility.Collapsed;
        schwebePlatz.Margin = new Thickness(0, 0, 14, s.BlaseHoehe + 4);
    }

    /// <summary>Prompt bearbeiten deckt alles ab, auch die Seitenleiste.</summary>
    private void PromptAbgleich()
    {
        if (promptEbene == null) return;
        var p = AppSteuerung.Shared.PromptBearbeiten;
        if (p?.Id == promptGezeigt && (p != null) == (promptEbene.Children.Count > 0)) return;
        promptGezeigt = p?.Id;
        promptEbene.Children.Clear();
        promptEbene.Visibility = p != null ? Visibility.Visible : Visibility.Collapsed;
        if (p != null) promptEbene.Children.Add(new PromptEditorVollbild(p));
    }

    /// <summary>Fenster-Tastenkürzel: Strg+N und Strg+1 Start mit neuem Gespräch, Strg+L Live, Strg+D Sprechen, Strg+, Einstellungen, Strg+2 Zentrale, Strg+3 Ablage.</summary>
    private void TasteGedrueckt(object sender, KeyEventArgs e)
    {
        if (dialogEbene.Visibility == Visibility.Visible && e.Key == Key.Escape) { obersterAbbruch?.Invoke(); e.Handled = true; return; }
        if (Keyboard.Modifiers != ModifierKeys.Control) return;
        // Im Prompt-Editor würden sie den Bereich unsichtbar darunter wechseln.
        if (AppSteuerung.Shared.PromptBearbeiten != null) return;
        var s = AppSteuerung.Shared;
        Action? a = e.Key switch
        {
            Key.N or Key.D1 or Key.NumPad1 => () => s.StartOeffnen(),
            Key.L => s.Live,
            Key.D => s.Sprechen,
            Key.OemComma => () => s.Bereich = Bereich.Einstellungen,
            Key.D2 or Key.NumPad2 => () => s.Bereich = Bereich.Zentrale,
            Key.D3 or Key.NumPad3 => () => s.Bereich = Bereich.Ablage,
            _ => null,
        };
        if (a == null) return;
        e.Handled = true;
        a();
    }

    private Action? obersterAbbruch;

    /// <summary>Legt Inhalt mittig über alles und dunkelt den Rest ab (.alert, .sheet). Gibt die Funktion zum Schließen zurück.</summary>
    public Action Ueberlagerung(FrameworkElement inhalt, Action? abbruch = null)
    {
        var ebene = new Grid { Background = new SolidColorBrush(Color.FromArgb(110, 0, 0, 0)) };
        inhalt.HorizontalAlignment = HorizontalAlignment.Center;
        inhalt.VerticalAlignment = VerticalAlignment.Center;
        ebene.Children.Add(inhalt);
        dialogEbene.Children.Add(ebene);
        dialogEbene.Visibility = Visibility.Visible;
        ebene.BeginAnimation(OpacityProperty, new DoubleAnimation(0, 1, new Duration(TimeSpan.FromMilliseconds(120))));
        var vorher = obersterAbbruch;
        var zu = false;
        void Schliessen()
        {
            if (zu) return;
            zu = true;
            dialogEbene.Children.Remove(ebene);
            if (dialogEbene.Children.Count == 0) dialogEbene.Visibility = Visibility.Collapsed;
            obersterAbbruch = vorher;
        }
        obersterAbbruch = () => { Schliessen(); abbruch?.Invoke(); };
        return Schliessen;
    }
}

public sealed class Seitenleiste : Decorator
{
    private static string suche = "";
    private static int reiter; // 0 Gespräche, 1 Programmierung, 2 Projekte
    private static string neuesProjekt = "";
    private const int Seite = 60;

    private readonly AppSteuerung steuerung = AppSteuerung.Shared;
    private readonly GespraechsSpeicher speicher = GespraechsSpeicher.Shared;
    private readonly HashSet<Gespraech> beobachtet = new();
    private readonly Decorator reiterPlatz = new(), projektPlatz = new();
    private readonly StackPanel liste = new();
    private readonly Scroller listenScroller;
    private int gezeigtBis = Seite;
    private bool wartet, angemeldet;

    public Seitenleiste()
    {
        var kopf = H(12, new KugelLive(34), V(2,
                Gesperrt("JARVIS", 17, FontWeights.ExtraBold, 4),
                new Reaktiv(() => H(6, new LivePunkt(7), Tx(steuerung.HandyVerbunden ? "Mit dem Handy verbunden" : "Handy nicht erreichbar", 11, FontWeights.Medium, Farbe.Text2, zeilen: 1)), new[] { nameof(AppSteuerung.HandyVerbunden) }, steuerung)))
            .Rand(6, 34, 6, 0);

        var startKnopf = new Border
        {
            Background = Farbe.Akzent, CornerRadius = new CornerRadius(14), Padding = new Thickness(12, 10, 12, 10), Margin = new Thickness(0, 0, 0, 4),
            // „Start“ beginnt ein neues Gespräch und öffnet unten die Sprechblase.
            Child = H(12, Sym.Bild(Bereich.Start.Symbol, 13, Farbe.Schwarz, 20), Tx(Bereich.Start.Titel, 14, FontWeights.SemiBold, Farbe.Schwarz), Spacer(), Tx("Strg+N", 11, FontWeights.Medium, new SolidColorBrush(Color.FromArgb(140, 0, 0, 0)))),
        }.Klick(() => steuerung.StartOeffnen());

        var bereiche = new Reaktiv(() =>
        {
            var v = V(2, startKnopf.Parent == null ? startKnopf : Abgeloest(startKnopf));
            foreach (var b in new[] { Bereich.Zentrale, Bereich.Biomarker, Bereich.Ablage, Bereich.Einstellungen })
                v.Children.Add(new LeistenEintrag(b.Symbol, b.Titel, steuerung.Bereich == b, () => steuerung.Bereich = b));
            return v;
        }, new[] { nameof(AppSteuerung.Bereich) }, steuerung);

        var suchFeld = new TextFeld("Gespräche durchsuchen", 13, text: suche, schreiben: t => { suche = t; gezeigtBis = Seite; ListeVormerken(); });
        var sucheBox = Box(H(8, Sym.Bild("magnifyingglass", 13, Farbe.Text3), suchFeld.Fuell()), Farbe.Glas(0.05), 12, 12, 9);

        listenScroller = new Scroller(liste);
        listenScroller.Sv.ScrollChanged += (_, _) =>
        {
            if (listenScroller.Sv.ScrollableHeight - listenScroller.Sv.VerticalOffset < 200 && liste.Children.Count >= gezeigtBis) { gezeigtBis += Seite; ListeVormerken(); }
        };

        var fuss = H(10, new Reaktiv(() => new RundKnopf(DesignStand.Shared.Modus.Symbol, DesignStand.Shared.Weiter, hilfe: DesignStand.Shared.Hilfe, groesse: 30), new[] { nameof(DesignStand.Modus) }, DesignStand.Shared),
                V(1, Tx($"{AppInfo.Name} {AppInfo.Version}", 10, null, Farbe.Text3), Tx($"Build {AppInfo.Stand} Uhr", 10, null, Farbe.Text3)), Spacer())
            .Rand(2, 0, 0, 12);

        var spalte = new Grid { Background = Farbe.Tiefe(0.22) };
        var innen = new Grid { Margin = new Thickness(14, 0, 14, 0) };
        var teile = new UIElement[] { kopf, bereiche, sucheBox, reiterPlatz, projektPlatz, listenScroller, fuss };
        for (var i = 0; i < teile.Length; i++)
        {
            innen.RowDefinitions.Add(new RowDefinition { Height = teile[i] == listenScroller ? new GridLength(1, GridUnitType.Star) : GridLength.Auto });
            Grid.SetRow(teile[i], i);
            if (i > 0 && teile[i] is FrameworkElement f && f != projektPlatz) f.Margin = new Thickness(f.Margin.Left, f.Margin.Top + 14, f.Margin.Right, f.Margin.Bottom);
            innen.Children.Add(teile[i]);
        }
        spalte.Children.Add(innen);
        Child = spalte;
        ReiterBauen();
        ProjektBauen();
        ListeBauen();
        Loaded += (_, _) =>
        {
            if (angemeldet) return;
            angemeldet = true;
            speicher.Alle.CollectionChanged += ListeGeaendert;
            steuerung.PropertyChanged += SteuerungGeaendert;
            Projekte.Shared.Namen.CollectionChanged += ProjekteGeaendert;
            AlleBeobachten();
            ListeVormerken();
        };
        Unloaded += (_, _) =>
        {
            angemeldet = false;
            speicher.Alle.CollectionChanged -= ListeGeaendert;
            steuerung.PropertyChanged -= SteuerungGeaendert;
            Projekte.Shared.Namen.CollectionChanged -= ProjekteGeaendert;
            foreach (var g in beobachtet) g.PropertyChanged -= GespraechGeaendert;
            beobachtet.Clear();
        };
    }

    private static UIElement Abgeloest(FrameworkElement e)
    {
        if (e.Parent is Panel p) p.Children.Remove(e);
        return e;
    }

    private void ProjekteGeaendert(object? s, NotifyCollectionChangedEventArgs e) => ProjektBauen();
    private void ListeGeaendert(object? s, NotifyCollectionChangedEventArgs e) { AlleBeobachten(); ListeVormerken(); }
    private void AlleBeobachten()
    {
        var jetzt = speicher.Alle.ToHashSet();
        foreach (var g in beobachtet.Except(jetzt).ToList()) { g.PropertyChanged -= GespraechGeaendert; beobachtet.Remove(g); }
        foreach (var g in jetzt) if (beobachtet.Add(g)) g.PropertyChanged += GespraechGeaendert;
    }
    private void GespraechGeaendert(object? s, PropertyChangedEventArgs e)
    {
        if (e.PropertyName is nameof(Gespraech.Arbeitet) or nameof(Gespraech.Titel) or nameof(Gespraech.Geaendert) or nameof(Gespraech.Projekt) or nameof(Gespraech.IstProgrammierung) or null) ListeVormerken();
    }
    private void SteuerungGeaendert(object? s, PropertyChangedEventArgs e)
    {
        if (e.PropertyName is nameof(AppSteuerung.Bereich) or nameof(AppSteuerung.Gespraech)) ListeVormerken();
        if (e.PropertyName == nameof(AppSteuerung.Projekt)) { ProjektBauen(); ListeVormerken(); }
    }

    private void ListeVormerken()
    {
        if (wartet) return;
        wartet = true;
        Haupt.D?.BeginInvoke(() => { wartet = false; ListeBauen(); ReiterBauen(); }, DispatcherPriority.Background);
    }

    private List<Gespraech> Gefiltert()
    {
        IEnumerable<Gespraech> basis = reiter switch
        {
            1 => speicher.Alle.Where(g => g.IstProgrammierung),
            2 => steuerung.Projekt is { } p ? speicher.Alle.Where(g => g.Projekt == p) : Enumerable.Empty<Gespraech>(),
            _ => speicher.Alle.Where(g => g.Projekt == null && !g.IstProgrammierung),
        };
        var s = suche.Trim().ToLowerInvariant();
        if (s.Length == 0) return basis.ToList();
        return basis.Where(g => g.Titel.ToLowerInvariant().Contains(s) || g.Nachrichten.Any(n => n.Text.ToLowerInvariant().Contains(s))).ToList();
    }

    private void ListeBauen()
    {
        liste.Children.Clear();
        foreach (var g in Gefiltert().Take(gezeigtBis))
        {
            var e = new GespraechsEintrag(g, steuerung.Bereich == Bereich.Gespraech && steuerung.Gespraech?.Id == g.Id);
            e.Margin = new Thickness(0, 0, 0, 2);
            liste.Children.Add(e);
        }
    }

    /// <summary>Welche Reiter gerade arbeiten – nur über die (wenigen) arbeitenden Gespräche.</summary>
    private bool[] Arbeitet()
    {
        var aktiv = speicher.Alle.Where(g => g.Arbeitet).ToList();
        if (aktiv.Count == 0) return new[] { false, false, false };
        return new[] { aktiv.Any(g => g.Projekt == null && !g.IstProgrammierung), aktiv.Any(g => g.IstProgrammierung), aktiv.Any(g => g.Projekt != null) };
    }

    private void ReiterBauen()
    {
        var status = Arbeitet();
        var raster = new Grid();
        var namen = new[] { "Gespräche", "Programmierung", "Projekte" };
        for (var i = 0; i < 3; i++)
        {
            raster.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(i == 1 ? 1.45 : 1, GridUnitType.Star) });
            var nr = i;
            var titel = Tx(namen[i], 11, reiter == i ? FontWeights.Bold : FontWeights.Medium, reiter == i ? Farbe.Text : Farbe.Text3, zeilen: 1);
            UIElement inhalt = titel;
            if (status[i])
            {
                var punkt = new Border { Width = 6, Height = 6, CornerRadius = new CornerRadius(3), Background = Farbe.Warnung, Effect = new System.Windows.Media.Effects.DropShadowEffect { Color = Farbe.Wert(Farbe.Warnung), BlurRadius = 6, ShadowDepth = 0 } };
                inhalt = H(4, titel, punkt);
            }
            if (inhalt is FrameworkElement fe) fe.HorizontalAlignment = HorizontalAlignment.Center;
            var feld = new Border { Child = inhalt, Padding = new Thickness(4, 6, 4, 6), CornerRadius = new CornerRadius(8), Background = reiter == i ? Farbe.Glas(0.09) : Farbe.Klar, Margin = new Thickness(i > 0 ? 2 : 0, 0, i < 2 ? 2 : 0, 0) }
                .Klick(() => { reiter = nr; if (nr != 2) steuerung.Projekt = null; gezeigtBis = Seite; ReiterBauen(); ProjektBauen(); ListeBauen(); });
            Grid.SetColumn(feld, i);
            raster.Children.Add(feld);
        }
        reiterPlatz.Child = raster;
    }

    private void ProjektBauen()
    {
        if (reiter != 2) { projektPlatz.Child = null; projektPlatz.Margin = new Thickness(0); return; }
        projektPlatz.Margin = new Thickness(0, 14, 0, 0);
        if (steuerung.Projekt is { } p)
        {
            projektPlatz.Child = H(8,
                    Sym.Bild("chevron.left", 12, Farbe.Text).Breite(20).Hoehe(20),
                    Tx(p, 13, FontWeights.Bold, Farbe.Text, zeilen: 1).Fuell(),
                    Sym.Bild("plus.bubble", 13, Farbe.Text).Breite(20).Hoehe(20))
                .Rand(6, 0);
            var zeile = (Stapel)projektPlatz.Child;
            var zurueck = (FrameworkElement)zeile.Children[0];
            var neu = (FrameworkElement)zeile.Children[2];
            zurueck.IsHitTestVisible = true; neu.IsHitTestVisible = true;
            ((TextBlock)zurueck).Background = Farbe.Klar;
            ((TextBlock)neu).Background = Farbe.Klar;
            zurueck.Klick(() => steuerung.Projekt = null);
            neu.Klick(() => steuerung.NeuesGespraech(imProjekt: true)).Hilfe("Neues Gespräch im Projekt");
            return;
        }
        TextFeld? feld = null;
        void Anlegen() { Projekte.Shared.Neu(neuesProjekt); neuesProjekt = ""; if (feld != null) feld.Text = ""; }
        feld = new TextFeld("Neues Projekt", 12, text: neuesProjekt, absenden: Anlegen, schreiben: t => neuesProjekt = t);
        var anlegen = Sym.Bild("folder.badge.plus", 13, Farbe.Text);
        anlegen.IsHitTestVisible = true; anlegen.Background = Farbe.Klar;
        anlegen.Klick(Anlegen);
        var kopf = Box(H(8, feld.Fuell(), anlegen), Farbe.Glas(0.05), 10, 10, 7);
        var namen = new StackPanel();
        foreach (var n in Projekte.Shared.Namen)
            namen.Children.Add(new LeistenEintrag("folder.fill", n, false, () => steuerung.Projekt = n).Rand(0, 0, 0, 2));
        projektPlatz.Child = V(14, kopf, new Scroller(namen) { MaxHeight = 180 });
    }
}

/// <summary>Die schwebenden Knöpfe nach dem Stapel des Handys: Neue Session, Papierkorb, ein freier Platz, Nachtrag, Programmierung. Ohne eigene Fläche, Papierkorb und Nachtrag nur, wenn sie etwas tun können.</summary>
public sealed class SchwebeKnoepfe : Decorator
{
    private static bool sessionBlitz;
    private readonly AppSteuerung steuerung = AppSteuerung.Shared;

    public SchwebeKnoepfe()
    {
        var reaktiv = new Reaktiv(Bauen, new[] { nameof(AppSteuerung.Gespraech), nameof(AppSteuerung.GespraechInhalt), nameof(AppSteuerung.NachtragMoeglich), nameof(AppSteuerung.NachtragAktiv), nameof(AppSteuerung.ProgrammiererAktiv), nameof(AppSteuerung.ProgrammierVorwahlOffen), nameof(Blitz) }, steuerung, BlitzMelder);
        Child = reaktiv;
    }

    private const string Blitz = "Blitz";
    private static readonly BlitzQuelle BlitzMelder = new();
    private sealed class BlitzQuelle : Beobachtbar { public void Sag() => Melde(Blitz); }

    private UIElement Bauen()
    {
        var s = steuerung;
        var v = V(10);
        v.Children.Add(new SchwebeKnopf("plus.bubble.fill", "Neue Session", () =>
        {
            s.NeueSession();
            sessionBlitz = true;
            BlitzMelder.Sag();
            Starte(async () => { await Task.Delay(450); sessionBlitz = false; BlitzMelder.Sag(); });
        }, sessionBlitz));
        if (s.Gespraech is { } g && g.Nachrichten.Count > 0)
            v.Children.Add(new SchwebeKnopf("trash.fill", "Gespräch löschen", () =>
            {
                // Das Ziel steht seit dem Klick fest: Hat das Gespräch inzwischen gewechselt, bleibt die offene Ansicht stehen.
                Dialog.Zeige("Gespräch löschen?", "Möchtest du dieses Gespräch wirklich löschen? Es wird endgültig entfernt und erscheint nicht mehr unter Gespräche.",
                    new DialogKnopf("Abbrechen", null, Abbruch: true),
                    new DialogKnopf("Löschen", () =>
                    {
                        GespraechsSpeicher.Shared.Loeschen(g);
                        if (ReferenceEquals(s.Gespraech, g))
                        {
                            s.Gespraech = null;
                            if (s.Bereich == Bereich.Gespraech) s.Bereich = Bereich.Start;
                        }
                    }, Destruktiv: true));
            }));
        v.Children.Add(new Border { Width = 38, Height = 14 });
        if (s.NachtragMoeglich)
            v.Children.Add(new SchwebeKnopf("plus", "Nachtrag in die Programmier-Sitzung dieses Gesprächs", s.NachtragUmschalten, s.NachtragAktiv));
        var prog = new Grid();
        prog.Children.Add(new SchwebeKnopf("chevron.left.forwardslash.chevron.right", "Neuer Programmierauftrag", s.ProgrammiererUmschalten, s.ProgrammiererAktiv || s.ProgrammierVorwahlOffen));
        // Links neben dem Symbol, ohne den Stapel zu verschieben.
        if (s.ProgrammierVorwahlOffen)
        {
            var reihe = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, Margin = new Thickness(-1200, 0, 46, 0) };
            var i = 0;
            foreach (var m in ProgrammierVorspann.Modelle)
            {
                var erste = i == 0;
                var t = Tx(m.Label, 14, FontWeights.SemiBold, erste ? Farbe.Schwarz : Farbe.Weiss, zeilen: 1);
                t.VerticalAlignment = VerticalAlignment.Center;
                var knopf = new Border { Height = 38, Padding = new Thickness(14, 0, 14, 0), CornerRadius = new CornerRadius(19), Background = erste ? Farbe.Weiss : new SolidColorBrush(Color.FromRgb(51, 51, 51)), Child = t, Margin = new Thickness(erste ? 0 : 8, 0, 0, 0) };
                knopf.Klick(() => s.ProgrammierVorwahl(m));
                reihe.Children.Add(knopf);
                i++;
            }
            prog.Children.Add(reihe);
        }
        v.Children.Add(prog);
        return v;
    }
}

internal sealed class SchwebeKnopf : Border
{
    public SchwebeKnopf(string symbol, string hilfe, Action aktion, bool leuchtet = false)
    {
        Width = 38; Height = 38;
        CornerRadius = new CornerRadius(19);
        Child = Sym.Bild(symbol, 14, leuchtet ? Farbe.Schwarz : Farbe.Text2);
        if (leuchtet) Background = Farbe.Gruen; else this.SchwebeFlaeche(Farbe.Glas(0.05), Farbe.Glas(0.13), 100);
        this.Hilfe(hilfe);
        this.Klick(aktion);
    }
}

public sealed class LeistenEintrag : Border
{
    public LeistenEintrag(string symbol, string titel, bool aktiv, Action aktion)
    {
        CornerRadius = new CornerRadius(11);
        Padding = new Thickness(12, 9, 12, 9);
        Child = H(12, Sym.Bild(symbol, 13, aktiv ? Farbe.Akzent : Farbe.Text2, 20), Tx(titel, 14, aktiv ? FontWeights.SemiBold : FontWeights.Medium, aktiv ? Farbe.Text : Farbe.Text2, zeilen: 1).Fuell());
        this.SchwebeFlaeche(Farbe.Glas(aktiv ? 0.09 : 0), Farbe.Glas(aktiv ? 0.09 : 0.05), 80);
        this.Klick(aktion);
    }
}

public sealed class GespraechsEintrag : Border
{
    public GespraechsEintrag(Gespraech g, bool aktiv)
    {
        CornerRadius = new CornerRadius(10);
        Padding = new Thickness(10, 7, 10, 7);
        var loeschen = Sym.Bild("trash", 11, Farbe.Text3);
        loeschen.IsHitTestVisible = true;
        loeschen.Background = Farbe.Klar;
        loeschen.Width = 18; loeschen.Height = 18;
        loeschen.Visibility = Visibility.Collapsed;
        loeschen.Hilfe("Gespräch löschen");
        loeschen.Klick(() =>
        {
            GespraechsSpeicher.Shared.Loeschen(g);
            if (AppSteuerung.Shared.Gespraech?.Id == g.Id) AppSteuerung.Shared.Gespraech = null;
            if (aktiv) AppSteuerung.Shared.Bereich = Bereich.Start;
        });
        UIElement zeichen = g.Arbeitet
            ? new Spinner(10).Rand(2, 0)
            : Sym.Bild(g.Titel == "Sprachgespräch" ? "waveform" : "bubble.left.fill", 10, Farbe.Text3, 14);
        Child = H(10, zeichen,
            V(2, Tx(g.Titel, 13, aktiv ? FontWeights.SemiBold : FontWeights.Regular, aktiv ? Farbe.Text : Farbe.Text2, zeilen: 1), Tx(Relativ(g.Geaendert), 10, null, Farbe.Text3, zeilen: 1)).Fuell(),
            loeschen);
        this.SchwebeFlaeche(Farbe.Glas(aktiv ? 0.09 : 0), Farbe.Glas(aktiv ? 0.09 : 0.04), 80);
        this.Schwebe(an => loeschen.Visibility = an ? Visibility.Visible : Visibility.Collapsed);
        this.Klick(() => AppSteuerung.Shared.Oeffne(g));
    }
}

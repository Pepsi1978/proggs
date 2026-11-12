using System.Diagnostics;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Effects;
using System.Windows.Threading;

namespace JarvisWindows;

/// <summary>Die schwebende Sprachpille, wie die Sprachblase am Handy (SprachBlase.kt): „Hey Jarvis“ aus einem anderen Programm öffnet Jarvis nicht, das Gespräch
/// läuft im Hintergrund, und oben mittig zeigt eine kleine Pille ein Pegel-Spektrum in Orange. Sprechen Frank oder Jarvis, zittert es nach dem Pegel,
/// dazwischen eine ruhige Welle. Klick öffnet Jarvis, das X, langes Drücken, Rechtsklick oder Ziehen nach oben beendet das Gespräch; auch im Live-Modus.
/// Nimmt nie den Fokus, erscheint nicht auf Bildschirmfotos und zeichnet direkt: leicht, damit Spracherkennung und Ausgabe nichts davon merken.</summary>
public sealed class SprachPille
{
    private PillenFenster? fenster;
    private bool sichtbar;
    /// <summary>Laufende Aufnahmen: Werkzeuge können parallel laufen, erst die letzte holt die Pille zurück.</summary>
    private int aufnahmen;

    public void Setze(bool neu)
    {
        if (neu == sichtbar) return;
        sichtbar = neu;
        Anwenden();
    }

    public async Task FuerAufnahme(bool ausblenden)
    {
        aufnahmen = Math.Max(0, aufnahmen + (ausblenden ? 1 : -1));
        Anwenden();
        // Der Fensterserver braucht einen Moment, bis die Pille wirklich weg ist.
        if (ausblenden && sichtbar) await Task.Delay(80);
    }

    private void Anwenden()
    {
        if (!sichtbar || aufnahmen != 0)
        {
            fenster?.Ansicht.Laufen(false);
            fenster?.Hide();
            return;
        }
        fenster ??= new PillenFenster();
        fenster.Zeigen();
        fenster.Ansicht.Laufen(true);
    }
}

/// <summary>Fenster ohne Fokus: WS_EX_NOACTIVATE|WS_EX_TOOLWINDOW|WS_EX_TOPMOST, von Bildschirmaufnahmen ausgenommen.</summary>
internal sealed class PillenFenster : Window
{
    public const double Rand = 14;
    public PillenAnsicht Ansicht { get; } = new();

    public PillenFenster()
    {
        Title = "Jarvis-Sprachpille";
        WindowStyle = WindowStyle.None;
        ResizeMode = ResizeMode.NoResize;
        AllowsTransparency = true;
        Background = Brushes.Transparent;
        ShowInTaskbar = false;
        ShowActivated = false;
        Focusable = false;
        Topmost = true;
        Width = PillenAnsicht.Breite + 2 * Rand;
        Height = PillenAnsicht.Hoehe + 2 * Rand;
        var schatten = new System.Windows.Controls.Border
        {
            CornerRadius = new CornerRadius(PillenAnsicht.Hoehe / 2), Background = new SolidColorBrush(Color.FromRgb(0x0E, 0x0A, 0x06)), Margin = new Thickness(Rand), IsHitTestVisible = false,
            Effect = new DropShadowEffect { BlurRadius = 12, ShadowDepth = 2, Opacity = 0.45 }, CacheMode = new BitmapCache(),
        };
        var g = new System.Windows.Controls.Grid();
        g.Children.Add(schatten);
        Ansicht.Margin = new Thickness(Rand);
        g.Children.Add(Ansicht);
        Content = g;
        SourceInitialized += (_, _) =>
        {
            var griff = new WindowInteropHelper(this).Handle;
            var alt = Nativ.GetWindowLong(griff, Nativ.GWL_EXSTYLE).ToInt64();
            Nativ.SetWindowLong(griff, Nativ.GWL_EXSTYLE, new IntPtr(alt | Nativ.WS_EX_NOACTIVATE | Nativ.WS_EX_TOOLWINDOW | Nativ.WS_EX_TOPMOST));
            Nativ.SetWindowDisplayAffinity(griff, Nativ.WDA_EXCLUDEFROMCAPTURE);
        };
    }

    public void Zeigen()
    {
        if (!IsVisible) Show();
        Lage();
    }

    /// <summary>Oben mittig auf dem Bildschirm, auf dem Frank gerade arbeitet (NSScreen.main).</summary>
    private void Lage()
    {
        var griff = new WindowInteropHelper(this).Handle;
        var monitor = Nativ.MonitorFromWindow(Nativ.GetForegroundWindow(), 2);
        var info = new Nativ.MONITORINFO { cbSize = System.Runtime.InteropServices.Marshal.SizeOf<Nativ.MONITORINFO>() };
        if (!Nativ.GetMonitorInfo(monitor, ref info)) return;
        Nativ.GetDpiForMonitor(monitor, 0, out var dpi, out _);
        var f = (dpi == 0 ? 96 : dpi) / 96.0;
        var breite = (int)(Width * f);
        var x = (info.Arbeit.L + info.Arbeit.R - breite) / 2;
        var y = info.Arbeit.T + (int)((6 - Rand) * f);
        Nativ.SetWindowPos(griff, Nativ.HWND_TOPMOST, x, y, 0, 0, Nativ.SWP_NOSIZE | Nativ.SWP_NOACTIVATE);
    }
}

internal sealed class PillenAnsicht : FrameworkElement
{
    public const double Breite = 150, Hoehe = 44;
    private const int Balken = 13;
    /// <summary>Das X rechts: Abstand des Mittelpunkts vom rechten Rand und Radius.</summary>
    private const double KreuzAbstand = 22, KreuzRadius = 11;

    // Immer leuchtendes Orange (#FF6D00), wie am Handy; oben im Balken zu Gelb-Orange aufgehellt.
    private static readonly Color Orange = Color.FromRgb(255, 109, 0);
    private static readonly Pen RandStift = Eingefroren(new Pen(new SolidColorBrush(Color.FromArgb(0xD0, 255, 109, 0)), 1));
    private static readonly Brush Flaeche = Eingefroren(new LinearGradientBrush(Color.FromArgb(0xF2, 0x24, 0x1A, 0x10), Color.FromArgb(0xF2, 0x0E, 0x0A, 0x06), new Point(0, 0), new Point(0, 1)));
    private static readonly Brush Glanz = Eingefroren(new LinearGradientBrush(Color.FromArgb(0x22, 255, 255, 255), Color.FromArgb(0, 255, 255, 255), new Point(0, 0), new Point(0, 0.5)));
    private static readonly Brush KreuzFlaeche = Eingefroren(new SolidColorBrush(Color.FromArgb(51, 255, 109, 0)));
    private static readonly Pen KreuzStift = Eingefroren(new Pen(new SolidColorBrush(Orange), 2) { StartLineCap = PenLineCap.Round, EndLineCap = PenLineCap.Round });
    private static T Eingefroren<T>(T f) where T : Freezable { f.Freeze(); return f; }

    private readonly double[] werte = new double[Balken];
    private readonly Stopwatch uhr = Stopwatch.StartNew();
    private double zuletzt;
    private readonly DispatcherTimer takt = new(DispatcherPriority.Render) { Interval = TimeSpan.FromMilliseconds(33) };
    private Point? startPunkt;
    private CancellationTokenSource? langDruck;
    private bool kreuzGedrueckt;

    public PillenAnsicht()
    {
        Width = Breite; Height = Hoehe;
        ToolTip = "Klick öffnet Jarvis, das X oder lange drücken beendet das Gespräch.";
        takt.Tick += (_, _) => InvalidateVisual();
    }

    public void Laufen(bool an)
    {
        takt.Stop();
        if (!an) return;
        zuletzt = uhr.Elapsed.TotalSeconds;
        takt.Start();
    }

    // MARK: Bedienung

    protected override void OnMouseLeftButtonDown(MouseButtonEventArgs e)
    {
        e.Handled = true;
        CaptureMouse();
        if (ImKreuz(e)) { kreuzGedrueckt = true; return; }
        startPunkt = e.GetPosition(this);
        langDruck?.Cancel();
        var q = langDruck = new CancellationTokenSource();
        Ui.Starte(async () =>
        {
            await Task.Delay(500, q.Token);
            Beenden();
        });
    }

    protected override void OnMouseMove(MouseEventArgs e)
    {
        if (startPunkt is not { } s || e.LeftButton != MouseButtonState.Pressed) return;
        if (s.Y - e.GetPosition(this).Y > 12) Beenden();
    }

    protected override void OnMouseLeftButtonUp(MouseButtonEventArgs e)
    {
        e.Handled = true;
        ReleaseMouseCapture();
        if (kreuzGedrueckt)
        {
            kreuzGedrueckt = false;
            if (ImKreuz(e)) Beenden();
            return;
        }
        langDruck?.Cancel();
        if (startPunkt == null) return;
        startPunkt = null;
        AppSteuerung.Shared.PilleOeffnen();
    }

    protected override void OnMouseRightButtonDown(MouseButtonEventArgs e) { e.Handled = true; Beenden(); }

    /// <summary>Nur das X selbst plus wenige Punkte, damit ein Klick zum Öffnen nicht versehentlich das Gespräch beendet.</summary>
    private bool ImKreuz(MouseEventArgs e)
    {
        var p = e.GetPosition(this);
        var dx = p.X - (ActualWidth - KreuzAbstand);
        var dy = p.Y - ActualHeight / 2;
        return Math.Sqrt(dx * dx + dy * dy) <= KreuzRadius + 4;
    }

    private void Beenden()
    {
        langDruck?.Cancel();
        startPunkt = null;
        if (IsMouseCaptured) ReleaseMouseCapture();

        Log("sprache", "Sprachsitzung beendet", wo: "Sprachpille");
        SprachSchleife.Shared.Beenden();
    }

    // MARK: Zeichnen

    protected override void OnRender(DrawingContext dc)
    {
        var jetzt = uhr.Elapsed.TotalSeconds;
        var schritt = Math.Min(Math.Max(jetzt - zuletzt, 0.001), 0.1);
        zuletzt = jetzt;
        var t = jetzt;

        var frank = SprachSchleife.Shared.Zustand == SprachZustand.Hoert && Diktat.Shared.Aktiv && !Diktat.Shared.SchreibtMit;
        double pegel = frank ? Diktat.Shared.Pegel : (Stimme.Shared.Spricht ? Stimme.Shared.Pegel : 0);

        double w = Breite, h = Hoehe;
        var form = new RectangleGeometry(new Rect(0.5, 0.5, w - 1, h - 1), h / 2 - 0.5, h / 2 - 0.5);
        dc.DrawGeometry(Flaeche, null, form);
        dc.DrawGeometry(Glanz, null, form);
        dc.DrawGeometry(null, RandStift, form);

        const double abstand = 5.6, breite = 3.2;
        var start = (w - KreuzAbstand) / 2 - abstand * (Balken - 1) / 2;
        var maxHoehe = h * 0.62;
        var mitte = h / 2;
        var strich = new LinearGradientBrush(Color.FromRgb(255, 139, 0), Orange, new Point(0, mitte - maxHoehe / 2), new Point(0, mitte + maxHoehe / 2)) { MappingMode = BrushMappingMode.Absolute };
        strich.Freeze();
        var stift = new Pen(strich, breite) { StartLineCap = PenLineCap.Round, EndLineCap = PenLineCap.Round };
        stift.Freeze();
        for (var i = 0; i < Balken; i++)
        {
            double n = i;
            var lage = (n - (Balken - 1) / 2.0) / ((Balken - 1) / 2.0);
            var huelle = 1 - 0.55 * lage * lage;
            // Jeder Balken mit eigener Phase: Das Spektrum zittert nach dem Pegel, nicht alle gleich.
            var zittern = 0.55 + 0.45 * Math.Sin(t * (7 + n * 1.3) + n * 2.1);
            var ziel = pegel > 0.02 ? Math.Min(pegel * 1.25, 1) * huelle * zittern
                                    : 0.10 + 0.07 * Math.Sin(t * 2.4 - n * 0.55); // ruhige Welle: hört noch zu
            var tempo = ziel > werte[i] ? 22.0 : 7.0;
            werte[i] += (ziel - werte[i]) * Math.Min(1, schritt * tempo);
            var hb = Math.Max(breite, werte[i] * maxHoehe);
            var x = start + n * abstand;
            dc.DrawLine(stift, new Point(x, mitte - hb / 2 + breite / 2), new Point(x, mitte + hb / 2 - breite / 2));
        }

        var kreuz = new Point(w - KreuzAbstand, mitte);
        dc.DrawEllipse(KreuzFlaeche, null, kreuz, KreuzRadius, KreuzRadius);
        const double arm = 4.5;
        dc.DrawLine(KreuzStift, new Point(kreuz.X - arm, kreuz.Y - arm), new Point(kreuz.X + arm, kreuz.Y + arm));
        dc.DrawLine(KreuzStift, new Point(kreuz.X - arm, kreuz.Y + arm), new Point(kreuz.X + arm, kreuz.Y - arm));
    }
}

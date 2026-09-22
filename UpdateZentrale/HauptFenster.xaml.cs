using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using UpdateZentrale.Services;
using UpdateZentrale.ViewModels;

namespace UpdateZentrale;

public partial class HauptFenster : Window
{
    private const int DwmwaImmersiveDarkMode = 20;

    public HauptFenster()
    {
        InitializeComponent();

        SourceInitialized += (_, _) =>
        {
            AnBildschirmAnpassen();
            TitelleisteAnpassen();
        };
        Darstellung.Gewechselt += (_, _) => TitelleisteAnpassen();

        // The list is only useful once every card knows its state, so the first check runs by
        // itself right after the window is up.
        Loaded += async (_, _) =>
        {
            if (DataContext is HauptViewModel modell) await modell.ErstePruefungAsync();
        };
    }

    /// <summary>
    /// Die Wunschgröße aus der XAML passt auf einen großen Bildschirm, nicht auf jeden. Auf einem
    /// Laptop mit 1440x852 nutzbarer Fläche (2880x1800 bei 200% Skalierung) ist das Fenster höher
    /// als der Platz: WPF zentriert dann auf einen negativen oberen Rand, und die Kopfzeile steht
    /// über dem oberen Bildschirmrand -- unerreichbar. Deshalb wird die Größe auf die Arbeitsfläche
    /// gedeckelt und das Fenster darin selbst zentriert. Auf einem großen Bildschirm greift der
    /// Deckel nicht und es bleibt bei der Wunschgröße.
    /// </summary>
    private void AnBildschirmAnpassen()
    {
        // Der Bildschirm, auf dem das Fenster tatsächlich erscheint -- SystemParameters.WorkArea
        // kennt nur den Hauptbildschirm und hätte auf einem zweiten Monitor falsch gedeckelt.
        var flaeche = ArbeitsflaecheDesMonitors() ?? SystemParameters.WorkArea;
        if (flaeche.Width <= 0 || flaeche.Height <= 0) return;

        // Schicht gegen den schwarzen Rand: Ein gesetztes MaxWidth/MaxHeight gibt WPF als größte
        // Fenstergröße an Windows weiter, maximiert bleibt dann ein schwarzer Streifen rechts und
        // unten. Hier ausdrücklich aufgehoben, damit auch ein späterer Style es nicht einschleppt.
        MaxWidth = double.PositiveInfinity;
        MaxHeight = double.PositiveInfinity;

        // Etwas Luft zum Rand: ein randloses Fenster wirkt wie ein halb misslungenes Maximieren.
        var hoechstBreite = flaeche.Width * 0.96;
        var hoechstHoehe = flaeche.Height * 0.96;

        // Die Mindestgröße zuerst, sonst hält WPF ein zu großes MinHeight gegen den Deckel.
        MinWidth = Math.Min(MinWidth, hoechstBreite);
        MinHeight = Math.Min(MinHeight, hoechstHoehe);
        // Bewusst KEIN MaxWidth/MaxHeight: WPF reicht sie als größte Fenstergröße an Windows
        // weiter. Maximiert ist das Fenster aber um den Rahmen größer als die Arbeitsfläche, und
        // auf einem zweiten, größeren Bildschirm deutlich größer als die Fläche des Hauptbildschirms
        // -- der Rest blieb als schwarzer Streifen rechts und unten stehen.

        Width = Math.Min(Width, hoechstBreite);
        Height = Math.Min(Height, hoechstHoehe);

        Left = flaeche.Left + (flaeche.Width - Width) / 2;
        Top = flaeche.Top + (flaeche.Height - Height) / 2;
    }

    /// <summary>
    /// Zweite Schicht: Wer auch immer später eine Obergrenze setzt (Style, Code, Bindung) --
    /// beim Maximieren wird sie aufgehoben, bevor Windows die Größe übernimmt.
    /// </summary>
    protected override void OnStateChanged(EventArgs e)
    {
        if (WindowState == WindowState.Maximized
            && (!double.IsPositiveInfinity(MaxWidth) || !double.IsPositiveInfinity(MaxHeight)))
        {
            MaxWidth = double.PositiveInfinity;
            MaxHeight = double.PositiveInfinity;
        }
        base.OnStateChanged(e);
    }

    /// <summary>Arbeitsfläche des Monitors unter dem Fenster, in geräteunabhängigen Einheiten.</summary>
    private Rect? ArbeitsflaecheDesMonitors()
    {
        try
        {
            var griff = new WindowInteropHelper(this).Handle;
            if (griff == IntPtr.Zero) return null;

            var monitor = MonitorFromWindow(griff, MonitorDefaultToNearest);
            var info = new MonitorInfo { Groesse = Marshal.SizeOf<MonitorInfo>() };
            if (monitor == IntPtr.Zero || !GetMonitorInfo(monitor, ref info)) return null;

            // Physische Pixel -> DIPs mit genau der Transformation, mit der WPF Left/Top/Width/
            // Height dieses Fensters auslegt. Eine pauschale Division durch die Fenster-DPI
            // verschiebt bei gemischten DPI-Werten die absoluten Koordinaten.
            if (HwndSource.FromHwnd(griff)?.CompositionTarget is not { } ziel) return null;
            var a = info.Arbeit;
            return Rect.Transform(new Rect(a.Links, a.Oben, a.Rechts - a.Links, a.Unten - a.Oben),
                ziel.TransformFromDevice);
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "fenster", Schwere.Debug);
            return null;   // Fällt auf die Fläche des Hauptbildschirms zurück.
        }
    }

    private const uint MonitorDefaultToNearest = 2;

    [StructLayout(LayoutKind.Sequential)]
    private struct Rechteck { public int Links, Oben, Rechts, Unten; }

    [StructLayout(LayoutKind.Sequential)]
    private struct MonitorInfo { public int Groesse; public Rechteck Monitor; public Rechteck Arbeit; public int Flags; }

    [DllImport("user32.dll")]
    private static extern IntPtr MonitorFromWindow(IntPtr fenster, uint flags);

    [DllImport("user32.dll")]
    private static extern bool GetMonitorInfo(IntPtr monitor, ref MonitorInfo info);

    /// <summary>
    /// Without this the window keeps the light Windows caption bar above a dark surface -- and
    /// the other way round after switching to light mode.
    /// </summary>
    private void TitelleisteAnpassen()
    {
        try
        {
            var griff = new WindowInteropHelper(this).Handle;
            if (griff == IntPtr.Zero) return;

            var dunkel = Darstellung.IstHell ? 0 : 1;
            DwmSetWindowAttribute(griff, DwmwaImmersiveDarkMode, ref dunkel, sizeof(int));
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "fenster", Schwere.Debug);
            // Older Windows builds simply keep the light caption -- never worth an error.
        }
    }

    [DllImport("dwmapi.dll", PreserveSig = true)]
    private static extern int DwmSetWindowAttribute(IntPtr fenster, int attribut, ref int wert, int groesse);
}

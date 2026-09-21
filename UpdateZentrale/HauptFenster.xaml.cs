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
        var flaeche = SystemParameters.WorkArea;
        if (flaeche.Width <= 0 || flaeche.Height <= 0) return;

        // Etwas Luft zum Rand: ein randloses Fenster wirkt wie ein halb misslungenes Maximieren.
        var hoechstBreite = flaeche.Width * 0.96;
        var hoechstHoehe = flaeche.Height * 0.96;

        // Die Mindestgröße zuerst, sonst hält WPF ein zu großes MinHeight gegen den Deckel.
        MinWidth = Math.Min(MinWidth, hoechstBreite);
        MinHeight = Math.Min(MinHeight, hoechstHoehe);
        MaxWidth = flaeche.Width;
        MaxHeight = flaeche.Height;

        Width = Math.Min(Width, hoechstBreite);
        Height = Math.Min(Height, hoechstHoehe);

        Left = flaeche.Left + (flaeche.Width - Width) / 2;
        Top = flaeche.Top + (flaeche.Height - Height) / 2;
    }

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
        catch
        {
            // Older Windows builds simply keep the light caption -- never worth an error.
        }
    }

    [DllImport("dwmapi.dll", PreserveSig = true)]
    private static extern int DwmSetWindowAttribute(IntPtr fenster, int attribut, ref int wert, int groesse);
}

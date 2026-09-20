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

        SourceInitialized += (_, _) => TitelleisteAnpassen();
        Darstellung.Gewechselt += (_, _) => TitelleisteAnpassen();

        // The list is only useful once every card knows its state, so the first check runs by
        // itself right after the window is up.
        Loaded += async (_, _) =>
        {
            if (DataContext is HauptViewModel modell) await modell.ErstePruefungAsync();
        };
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

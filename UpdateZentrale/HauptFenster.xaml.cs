using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;

namespace UpdateZentrale;

public partial class HauptFenster : Window
{
    private const int DwmwaImmersiveDarkMode = 20;

    public HauptFenster()
    {
        InitializeComponent();
        SourceInitialized += (_, _) => TitelleisteDunkel();
    }

    /// <summary>
    /// Without this the window keeps the light Windows caption bar, which looks broken above a
    /// dark surface.
    /// </summary>
    private void TitelleisteDunkel()
    {
        try
        {
            var griff = new WindowInteropHelper(this).Handle;
            var an = 1;
            DwmSetWindowAttribute(griff, DwmwaImmersiveDarkMode, ref an, sizeof(int));
        }
        catch
        {
            // Older Windows builds simply keep the light caption -- never worth an error.
        }
    }

    [DllImport("dwmapi.dll", PreserveSig = true)]
    private static extern int DwmSetWindowAttribute(IntPtr fenster, int attribut, ref int wert, int groesse);
}

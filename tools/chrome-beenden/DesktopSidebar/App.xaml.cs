using System.Threading;
using System.Windows;

namespace DesktopSidebar;

public partial class App : Application
{
    // Mutex MUSS static gehalten werden, sonst sammelt der GC ihn ein (Almanach §6.4).
    private static Mutex? _instanceMutex;

    protected override void OnStartup(StartupEventArgs e)
    {
        // Single-Instance: verhindert mehrere parallele Sidebars (z.B. bei doppeltem Autostart).
        _instanceMutex = new Mutex(initiallyOwned: true, @"Global\ChromeShutdownSidebar_SingleInstance", out bool isNew);
        if (!isNew)
        {
            // Es laeuft bereits eine Instanz -> diese still beenden.
            Shutdown();
            return;
        }

        base.OnStartup(e);

        var window = new MainWindow();
        window.Show();
    }
}

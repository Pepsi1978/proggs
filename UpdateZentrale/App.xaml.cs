using System.Windows;
using UpdateZentrale.Services;

namespace UpdateZentrale;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        // Only one window: two instances would write the same settings and log files, and two
        // update runs could meet on the same installer.
        if (!Einzelstart.Beanspruchen())
        {
            Einzelstart.VorhandenesFensterZeigen();
            Shutdown();
            return;
        }

        Exit += (_, _) => Einzelstart.Freigeben();

        // A crash in a background update task must not take the window with it.
        DispatcherUnhandledException += (_, args) =>
        {
            MessageBox.Show(args.Exception.Message, "UpdateZentrale - unerwarteter Fehler",
                MessageBoxButton.OK, MessageBoxImage.Warning);
            args.Handled = true;
        };

        base.OnStartup(e);
    }
}

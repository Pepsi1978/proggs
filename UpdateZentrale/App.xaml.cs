using System.Windows;

namespace UpdateZentrale;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
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

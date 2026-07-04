using System.Windows;

namespace OpenCodeLauncher;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        Services.Logger.Instance.Info("App", "OnStartup", "OpenCode Launcher gestartet");
    }
}

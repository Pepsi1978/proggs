using System.Windows;
using OpenCodeLauncher.Services;

namespace OpenCodeLauncher;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        Services.Logger.Instance.Info("App", "OnStartup", "OpenCode Launcher gestartet");

        // Gespeichertes Design anwenden, bevor das Hauptfenster gerendert wird (kein Umschalt-Flackern).
        var layout = LayoutSettings.Load();
        var theme = string.Equals(layout.Theme, "Light", StringComparison.OrdinalIgnoreCase)
            ? ThemeManager.AppTheme.Light
            : ThemeManager.AppTheme.Dark;
        if (theme != ThemeManager.Current)
            ThemeManager.Apply(theme);
    }
}

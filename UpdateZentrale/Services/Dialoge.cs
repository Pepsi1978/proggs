using System.Windows;

namespace UpdateZentrale.Services;

/// <summary>
/// Thin wrapper so view models can ask a yes/no question without referencing a window. Marshals
/// onto the UI thread because update runs happen on background tasks.
/// </summary>
public static class Dialoge
{
    public static bool Fragen(string text, string titel)
    {
        var app = Application.Current;
        if (app is null) return false;

        return app.Dispatcher.Invoke(() => MessageBox.Show(
            app.MainWindow,
            text,
            titel,
            MessageBoxButton.YesNo,
            MessageBoxImage.Question,
            MessageBoxResult.No) == MessageBoxResult.Yes);
    }

    public static void Hinweis(string text, string titel = "UpdateZentrale")
    {
        var app = Application.Current;
        app?.Dispatcher.Invoke(() => MessageBox.Show(app.MainWindow, text, titel,
            MessageBoxButton.OK, MessageBoxImage.Information));
    }
}

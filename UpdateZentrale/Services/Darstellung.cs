using System.Windows;

namespace UpdateZentrale.Services;

/// <summary>
/// Swaps the colour dictionary at runtime. Only the first merged dictionary is exchanged, so the
/// style dictionary (Theme.xaml) stays untouched and every DynamicResource picks up the new brush.
/// </summary>
public static class Darstellung
{
    private const string Dunkel = "Themes/Dunkel.xaml";
    private const string Hell = "Themes/Hell.xaml";

    public static bool IstHell { get; private set; }

    /// <summary>Raised after a switch so converters that bake in the mode can re-evaluate.</summary>
    public static event EventHandler? Gewechselt;

    public static void Anwenden(bool hell)
    {
        var app = Application.Current;
        if (app is null) return;

        var neu = new ResourceDictionary
        {
            Source = new Uri(hell ? Hell : Dunkel, UriKind.Relative)
        };

        var buecher = app.Resources.MergedDictionaries;
        var alt = buecher.FirstOrDefault(d => d.Source is not null
            && (d.Source.OriginalString.EndsWith("Dunkel.xaml", StringComparison.OrdinalIgnoreCase)
                || d.Source.OriginalString.EndsWith("Hell.xaml", StringComparison.OrdinalIgnoreCase)));

        if (alt is not null)
        {
            var stelle = buecher.IndexOf(alt);
            buecher.Insert(stelle, neu);
            buecher.Remove(alt);
        }
        else
        {
            buecher.Insert(0, neu);
        }

        IstHell = hell;
        Gewechselt?.Invoke(null, EventArgs.Empty);
    }
}

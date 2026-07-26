using System;
using System.Linq;
using System.Windows;

namespace OpenLauncher.Services;

/// <summary>
/// Wechselt das Farb-ResourceDictionary (Themes/DarkTheme.xaml bzw. LightTheme.xaml)
/// zur Laufzeit aus. Alle Styles referenzieren die Farben per DynamicResource,
/// daher schaltet die komplette UI sofort um.
/// </summary>
public static class ThemeManager
{
    public enum AppTheme { Dark, Light }

    public static AppTheme Current { get; private set; } = AppTheme.Dark;

    /// <summary>Wird nach jedem Wechsel gefeuert (z.B. um die OS-Titelleiste anzupassen).</summary>
    public static event Action<AppTheme>? ThemeChanged;

    public static void Apply(AppTheme theme)
    {
        var dicts = Application.Current.Resources.MergedDictionaries;

        // vorhandenes Theme-Dictionary finden (per Pfad "Themes/...Theme.xaml")
        var existing = dicts.FirstOrDefault(d =>
            d.Source != null &&
            d.Source.OriginalString.IndexOf("Theme.xaml", StringComparison.OrdinalIgnoreCase) >= 0 &&
            d.Source.OriginalString.IndexOf("Themes/", StringComparison.OrdinalIgnoreCase) >= 0);

        var next = new ResourceDictionary
        {
            Source = new Uri($"Themes/{theme}Theme.xaml", UriKind.Relative)
        };

        if (existing != null)
            dicts[dicts.IndexOf(existing)] = next;
        else
            dicts.Add(next);

        Current = theme;
        Logger.Instance.Info("ThemeManager", "Apply", $"Design gewechselt: {theme}");
        ThemeChanged?.Invoke(theme);
    }

    public static void Toggle() =>
        Apply(Current == AppTheme.Dark ? AppTheme.Light : AppTheme.Dark);
}

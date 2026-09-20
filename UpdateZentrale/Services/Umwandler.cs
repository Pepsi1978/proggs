using System.Globalization;
using System.Windows;
using System.Windows.Data;
using System.Windows.Media;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>Turns a state into the pill colour that carries the meaning at a glance.</summary>
public sealed class ZustandZuPinselUmwandler : IValueConverter
{
    /// <summary>true = background (tinted), false = foreground (saturated).</summary>
    public bool Hintergrund { get; set; }

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        var (r, g, b) = (value as UpdateZustand?) switch
        {
            UpdateZustand.Aktuell => (34, 197, 94),
            UpdateZustand.UpdateVerfuegbar => (245, 158, 11),
            UpdateZustand.Fertig => (56, 189, 248),
            UpdateZustand.Fehler => (244, 63, 94),
            UpdateZustand.Abgebrochen => (148, 163, 184),
            UpdateZustand.NichtInstalliert => (148, 163, 184),
            UpdateZustand.Pruefe => (124, 92, 255),
            _ => (148, 163, 184)
        };

        if (Hintergrund)
        {
            return new SolidColorBrush(Color.FromArgb(Darstellung.IstHell ? (byte)30 : (byte)38,
                (byte)r, (byte)g, (byte)b));
        }

        // On a light surface the saturated tone is too pale to read, so it is darkened.
        if (Darstellung.IstHell)
        {
            r = (int)(r * 0.72);
            g = (int)(g * 0.72);
            b = (int)(b * 0.72);
        }

        return new SolidColorBrush(Color.FromRgb((byte)r, (byte)g, (byte)b));
    }

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => Binding.DoNothing;
}

public sealed class ZustandZuTextUmwandler : IValueConverter
{
    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
        => (value as UpdateZustand?) switch
        {
            UpdateZustand.Aktuell => "Aktuell",
            UpdateZustand.UpdateVerfuegbar => "Update verfügbar",
            UpdateZustand.Fertig => "Fertig",
            UpdateZustand.Fehler => "Fehler",
            UpdateZustand.Abgebrochen => "Abgebrochen",
            UpdateZustand.NichtInstalliert => "Nicht installiert",
            UpdateZustand.Pruefe => "Prüft …",
            _ => "Noch nicht geprüft"
        };

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => Binding.DoNothing;
}

/// <summary>Hex string from the catalog to a brush, so accents live in JSON instead of XAML.</summary>
public sealed class HexZuPinselUmwandler : IValueConverter
{
    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        try
        {
            if (value is string hex && !string.IsNullOrWhiteSpace(hex))
            {
                return new SolidColorBrush((Color)ColorConverter.ConvertFromString(hex));
            }
        }
        catch
        {
        }
        return new SolidColorBrush(Color.FromRgb(124, 92, 255));
    }

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => Binding.DoNothing;
}

public sealed class BoolZuSichtbarkeitUmwandler : IValueConverter
{
    public bool Umkehren { get; set; }

    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        var wahr = value is bool b && b;
        if (Umkehren) wahr = !wahr;
        return wahr ? Visibility.Visible : Visibility.Collapsed;
    }

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => Binding.DoNothing;
}

public sealed class TextZuSichtbarkeitUmwandler : IValueConverter
{
    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
        => string.IsNullOrWhiteSpace(value as string) ? Visibility.Collapsed : Visibility.Visible;

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => Binding.DoNothing;
}

public sealed class NichtUmwandler : IValueConverter
{
    public object Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
        => value is not bool b || !b;

    public object ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => value is bool b && !b;
}

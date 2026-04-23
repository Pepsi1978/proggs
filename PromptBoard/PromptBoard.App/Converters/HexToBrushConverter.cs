using System;
using System.Globalization;
using Microsoft.UI;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace PromptBoard.App.Converters;

/// <summary>
/// Converts "#RRGGBB" or "#AARRGGBB" strings to a SolidColorBrush.
/// Returns a neutral brush for invalid input so bindings never throw.
/// </summary>
public sealed class HexToBrushConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, string language)
    {
        if (value is string hex && TryParse(hex, out Color color))
        {
            return new SolidColorBrush(color);
        }
        return new SolidColorBrush(Colors.LightGray);
    }

    public object ConvertBack(object value, Type targetType, object parameter, string language)
        => throw new NotSupportedException();

    private static bool TryParse(string hex, out Color color)
    {
        color = Colors.LightGray;
        if (string.IsNullOrWhiteSpace(hex))
        {
            return false;
        }

        string s = hex.StartsWith('#') ? hex[1..] : hex;
        if (s.Length is not (6 or 8))
        {
            return false;
        }

        byte a = 0xFF, r, g, b;
        try
        {
            if (s.Length == 8)
            {
                a = byte.Parse(s.AsSpan(0, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture);
                r = byte.Parse(s.AsSpan(2, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture);
                g = byte.Parse(s.AsSpan(4, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture);
                b = byte.Parse(s.AsSpan(6, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture);
            }
            else
            {
                r = byte.Parse(s.AsSpan(0, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture);
                g = byte.Parse(s.AsSpan(2, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture);
                b = byte.Parse(s.AsSpan(4, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture);
            }
        }
        catch
        {
            return false;
        }

        color = Color.FromArgb(a, r, g, b);
        return true;
    }
}

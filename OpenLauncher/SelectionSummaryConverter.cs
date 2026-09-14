using System.Globalization;
using System.Windows;
using System.Windows.Data;

namespace OpenLauncher;

/// <summary>Fasst die aktuelle Auswahl zusammen, unabhängig von Betriebs- und Statusmeldungen.</summary>
public sealed class SelectionSummaryConverter : IMultiValueConverter
{
    public object Convert(object[] values, Type targetType, object parameter, CultureInfo culture)
    {
        string Text(int index) => index < values.Length && values[index] is string text ? text : string.Empty;
        var model = Text(0);
        if (string.IsNullOrWhiteSpace(model)) return "Modell wählen.";

        var parts = new List<string> { model };
        if (!string.IsNullOrWhiteSpace(Text(1))) parts.Add($"Profil {Text(1)}");
        if (!string.IsNullOrWhiteSpace(Text(2))) parts.Add($"Modus {Text(2)}");
        if (values.Length > 3 && values[3] is true && !string.IsNullOrWhiteSpace(Text(4)))
            parts.Add($"CLI {Text(4)}");
        if (!string.IsNullOrWhiteSpace(Text(5)))
            parts.Add($"{(string.Equals(Text(6), "EFFORT", StringComparison.OrdinalIgnoreCase) ? "Effort" : "Thinking")} {Text(5)}");
        return string.Join(" · ", parts);
    }

    public object[] ConvertBack(object value, Type[] targetTypes, object parameter, CultureInfo culture) =>
        targetTypes.Select(_ => DependencyProperty.UnsetValue).ToArray();
}

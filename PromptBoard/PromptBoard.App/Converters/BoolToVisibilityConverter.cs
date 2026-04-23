using System;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Data;

namespace PromptBoard.App.Converters;

/// <summary>True → Visible, false → Collapsed. Used to hide filtered-out
/// categories + prompts during search.</summary>
public sealed class BoolToVisibilityConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, string language)
    {
        bool flag = value is bool b && b;
        return flag ? Visibility.Visible : Visibility.Collapsed;
    }

    public object ConvertBack(object value, Type targetType, object parameter, string language)
        => throw new NotSupportedException();
}

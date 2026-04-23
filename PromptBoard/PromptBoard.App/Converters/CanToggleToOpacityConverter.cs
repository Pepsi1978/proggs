using System;
using Microsoft.UI.Xaml.Data;

namespace PromptBoard.App.Converters;

/// <summary>Visual hint for the version toggle: dimmed when no improved text exists yet.</summary>
public sealed class BoolToOpacityConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, string language)
    {
        bool flag = value is bool b && b;
        return flag ? 1.0 : 0.4;
    }

    public object ConvertBack(object value, Type targetType, object parameter, string language)
        => throw new NotSupportedException();
}

using System.Reflection;
using Microsoft.UI.Xaml.Controls;

namespace PromptBoard.App.Views;

public sealed partial class AboutDialog : ContentDialog
{
    public AboutDialog()
    {
        InitializeComponent();
        string? version = Assembly.GetExecutingAssembly().GetName().Version?.ToString();
        VersionText.Text = $"Version {version ?? "dev"}";
    }
}

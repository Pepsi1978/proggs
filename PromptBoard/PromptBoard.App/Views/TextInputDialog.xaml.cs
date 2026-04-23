using Microsoft.UI.Xaml.Controls;

namespace PromptBoard.App.Views;

public sealed partial class TextInputDialog : ContentDialog
{
    public TextInputDialog(string title, string initialValue)
    {
        InitializeComponent();
        Title = title;
        InputTextBox.Text = initialValue;
        Opened += (_, _) =>
        {
            InputTextBox.SelectAll();
            InputTextBox.Focus(Microsoft.UI.Xaml.FocusState.Programmatic);
        };
    }

    public string Value => InputTextBox.Text;
}

using System.Windows;

namespace ClaudeVoiceOverlay.Views;

/// <summary>Small modal for asking the user for a single line of text
/// (e.g. category name). Returns the trimmed string via Result or
/// null if cancelled.</summary>
public partial class TextInputDialog : Window
{
    public string? Result { get; private set; }

    public TextInputDialog(string title, string label, string initialValue = "")
    {
        InitializeComponent();
        TitleText.Text = title;
        LabelText.Text = label;
        InputBox.Text = initialValue;
        InputBox.Focus();
        InputBox.SelectAll();

        BtnCancel.Click += (_, _) => { Result = null; Close(); };
        BtnOk.Click += (_, _) =>
        {
            var v = InputBox.Text?.Trim() ?? string.Empty;
            if (string.IsNullOrEmpty(v)) return;
            Result = v;
            Close();
        };
    }

    public static string? Ask(Window owner, string title, string label, string initial = "")
    {
        var dlg = new TextInputDialog(title, label, initial) { Owner = owner };
        dlg.ShowDialog();
        return dlg.Result;
    }
}

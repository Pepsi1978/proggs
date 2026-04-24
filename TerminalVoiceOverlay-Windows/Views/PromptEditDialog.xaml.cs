using System.Windows;

namespace TerminalVoiceOverlay.Views;

public sealed record PromptEditResult(string ShortLabel, string OriginalText, bool IsAlwaysOn);

public partial class PromptEditDialog : Window
{
    public PromptEditResult? Result { get; private set; }

    public PromptEditDialog(
        string title,
        string initialLabel,
        string initialText,
        bool initialAlwaysOn)
    {
        InitializeComponent();
        TitleText.Text = title;
        ShortLabelBox.Text = initialLabel;
        OriginalTextBox.Text = initialText;
        AlwaysOnCheckbox.IsChecked = initialAlwaysOn;

        ShortLabelBox.Focus();
        ShortLabelBox.SelectAll();

        BtnCancel.Click += (_, _) => { Result = null; Close(); };
        BtnOk.Click += (_, _) =>
        {
            var label = ShortLabelBox.Text?.Trim() ?? string.Empty;
            var text = OriginalTextBox.Text ?? string.Empty;
            if (string.IsNullOrEmpty(label)) return;
            Result = new PromptEditResult(label, text, AlwaysOnCheckbox.IsChecked == true);
            Close();
        };
    }

    public static PromptEditResult? Ask(
        Window owner, string title, string label, string text, bool alwaysOn)
    {
        var dlg = new PromptEditDialog(title, label, text, alwaysOn) { Owner = owner };
        dlg.ShowDialog();
        return dlg.Result;
    }
}

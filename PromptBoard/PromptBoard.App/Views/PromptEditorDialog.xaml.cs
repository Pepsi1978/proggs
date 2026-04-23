using Microsoft.UI.Xaml.Controls;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Services;

namespace PromptBoard.App.Views;

public sealed partial class PromptEditorDialog : ContentDialog
{
    public PromptEditorDialog(PromptEditorRequest request)
    {
        InitializeComponent();

        ShortLabelTextBox.Text = request.ShortLabel;
        OriginalTextBox.Text = request.OriginalText;
        ImprovedTextBox.Text = request.ImprovedText ?? string.Empty;

        bool hasImproved = !string.IsNullOrWhiteSpace(request.ImprovedText);
        ImprovedHint.Visibility = hasImproved
            ? Microsoft.UI.Xaml.Visibility.Collapsed
            : Microsoft.UI.Xaml.Visibility.Visible;
        UseImprovedRadio.IsEnabled = hasImproved;

        if (request.ActiveVersion == PromptVersion.Improved && hasImproved)
        {
            UseImprovedRadio.IsChecked = true;
        }
        else
        {
            UseOriginalRadio.IsChecked = true;
        }

        Opened += (_, _) =>
        {
            ShortLabelTextBox.Focus(Microsoft.UI.Xaml.FocusState.Programmatic);
        };
    }

    public PromptEditorResult Result
    {
        get
        {
            string? improved = string.IsNullOrWhiteSpace(ImprovedTextBox.Text) ? null : ImprovedTextBox.Text;
            PromptVersion version = UseImprovedRadio.IsChecked == true && improved is not null
                ? PromptVersion.Improved
                : PromptVersion.Original;
            return new PromptEditorResult(
                ShortLabel: ShortLabelTextBox.Text?.Trim() ?? string.Empty,
                OriginalText: OriginalTextBox.Text ?? string.Empty,
                ImprovedText: improved,
                ActiveVersion: version);
        }
    }
}

using Microsoft.UI.Xaml.Controls;
using PromptBoard.Core.Enums;

namespace PromptBoard.App.Views;

public sealed partial class AddCategoryDialog : ContentDialog
{
    public AddCategoryDialog()
    {
        InitializeComponent();
        Opened += (_, _) =>
        {
            NameTextBox.Focus(Microsoft.UI.Xaml.FocusState.Programmatic);
        };
    }

    public string CategoryName => NameTextBox.Text?.Trim() ?? string.Empty;

    public CategoryType CategoryType
    {
        get
        {
            if (TypeComboBox.SelectedItem is ComboBoxItem item && item.Tag is string tag
                && System.Enum.TryParse(tag, out CategoryType parsed))
            {
                return parsed;
            }
            return CategoryType.Standard;
        }
    }
}

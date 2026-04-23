using System.Threading.Tasks;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PromptBoard.ViewModels;
using Serilog;

namespace PromptBoard.App.Views;

public sealed partial class SettingsDialog : ContentDialog
{
    public SettingsViewModel ViewModel { get; }

    public bool Saved { get; private set; }

    public SettingsDialog(SettingsViewModel viewModel)
    {
        InitializeComponent();
        ViewModel = viewModel;

        // Populate combos with the static lists.
        GroqModelBox.ItemsSource = SettingsViewModel.GroqModels;
        GeminiModelList.ItemsSource = ViewModel.GeminiModels;

        // The dialog's own PrimaryButtonClick is async; we guard save there.
        PrimaryButtonClick += OnSaveClick;

        Opened += async (_, _) => await InitializeFromViewModelAsync();
    }

    private async Task InitializeFromViewModelAsync()
    {
        try
        {
            await ViewModel.LoadAsync();
            GroqKeyBox.Password = ViewModel.GroqApiKey ?? string.Empty;
            GeminiKeyBox.Password = ViewModel.GeminiApiKey ?? string.Empty;
            BarHeightBox.Value = ViewModel.BarHeight;
            SeparatorBox.Text = ViewModel.SeparatorTemplate.Replace("\n", "\\n").Replace("\t", "\\t");
            AlwaysOnTopToggle.IsOn = ViewModel.AlwaysOnTop;

            // ComboBox selection: fall back to first item if current not in list.
            if (SettingsViewModel.GroqModels.Contains(ViewModel.GroqModel))
            {
                GroqModelBox.SelectedItem = ViewModel.GroqModel;
            }
            else
            {
                GroqModelBox.SelectedIndex = 0;
            }
        }
        catch (System.Exception ex)
        {
            Log.Error(ex, "SettingsDialog failed to load settings.");
        }
    }

    private async void OnSaveClick(ContentDialog sender, ContentDialogButtonClickEventArgs args)
    {
        var deferral = args.GetDeferral();
        try
        {
            // Unescape the separator ("\\n" → "\n") so users can type it visibly.
            ViewModel.GroqApiKey = GroqKeyBox.Password;
            ViewModel.GeminiApiKey = GeminiKeyBox.Password;
            ViewModel.BarHeight = BarHeightBox.Value;
            ViewModel.SeparatorTemplate = (SeparatorBox.Text ?? string.Empty)
                .Replace("\\n", "\n")
                .Replace("\\t", "\t");
            ViewModel.AlwaysOnTop = AlwaysOnTopToggle.IsOn;
            ViewModel.GroqModel = GroqModelBox.SelectedItem as string ?? SettingsViewModel.GroqModels[0];

            await ViewModel.SaveAsync();
            Saved = true;
        }
        catch (System.Exception ex)
        {
            Log.Error(ex, "SettingsDialog save failed.");
            args.Cancel = true;
        }
        finally
        {
            deferral.Complete();
        }
    }
}

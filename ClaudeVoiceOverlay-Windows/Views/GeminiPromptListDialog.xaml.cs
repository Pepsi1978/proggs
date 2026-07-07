using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using ClaudeVoiceOverlay.Services;

namespace ClaudeVoiceOverlay.Views;

/// <summary>
/// Liste der 10 Gemini-Korrektur-Profile. Pro Profil ein Knopf, der den
/// Editor (GeminiPromptEditDialog) fuer dieses Profil oeffnet. Erreichbar
/// ueber den Knopf "Gemini-Prompts bearbeiten" im Einstellungs-Dialog
/// (Frank-Wunsch 2026-06-22, Etappe 2b).
/// </summary>
public partial class GeminiPromptListDialog : Window
{
    private const int ProfileCount = 10;

    public GeminiPromptListDialog()
    {
        InitializeComponent();
        BuildProfileButtons();
        BtnClose.Click += (_, _) => Close();
    }

    private void BuildProfileButtons()
    {
        ProfileList.Children.Clear();
        for (int profile = 1; profile <= ProfileCount; profile++)
        {
            int captured = profile;
            var btn = new Button
            {
                Content = GeminiClient.ProfileLabel(profile),
                Foreground = Brushes.White,
                Background = new SolidColorBrush(Color.FromRgb(0x2D, 0x2D, 0x2D)),
                BorderBrush = new SolidColorBrush(Color.FromRgb(0x45, 0x45, 0x45)),
                BorderThickness = new Thickness(1),
                Padding = new Thickness(12, 10, 12, 10),
                Margin = new Thickness(0, 0, 0, 6),
                HorizontalContentAlignment = HorizontalAlignment.Left,
                Cursor = System.Windows.Input.Cursors.Hand,
                FontSize = 13,
            };
            btn.Click += (_, _) => GeminiPromptEditDialog.Ask(this, captured);
            ProfileList.Children.Add(btn);
        }
    }

    /// <summary>Oeffnet die Profil-Liste modal.</summary>
    public static void Show(Window owner)
    {
        var dlg = new GeminiPromptListDialog { Owner = owner };
        dlg.ShowDialog();
    }
}

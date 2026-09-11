using System;
using System.Windows;
using System.Windows.Input;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// Editor fuer EINEN Schnell-Prompt (Kachel 1-10). Oeffnet sich per
/// Rechtsklick auf die Zahl -> "Prompt bearbeiten" oder per Linksklick auf
/// eine noch leere Zahl.
/// </summary>
public partial class QuickPromptEditDialog : Window
{
    private readonly int _slot;
    public bool Saved { get; private set; }

    public QuickPromptEditDialog(int slot)
    {
        InitializeComponent();
        _slot = slot;
        HeaderText.Text = $"Prompt {slot} bearbeiten";
        PromptBox.Text = QuickPromptStore.Load(slot);

        MouseLeftButtonDown += (_, e) => { if (e.ButtonState == MouseButtonState.Pressed) DragMove(); };
        Loaded += (_, _) => { Activate(); PromptBox.Focus(); PromptBox.CaretIndex = PromptBox.Text.Length; };
        BtnCancel.Click += (_, _) => { Saved = false; Close(); };
        BtnSave.Click += (_, _) =>
        {
            try
            {
                QuickPromptStore.Save(_slot, PromptBox.Text);
                Saved = true;
                Close();
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Speichern fehlgeschlagen: {ex.Message}", "Fehler",
                    MessageBoxButton.OK, MessageBoxImage.Error);
            }
        };
    }

    /// <summary>Oeffnet den Editor modal. True, wenn gespeichert wurde.</summary>
    public static bool Ask(int slot)
    {
        var dlg = new QuickPromptEditDialog(slot);
        dlg.ShowDialog();
        return dlg.Saved;
    }
}

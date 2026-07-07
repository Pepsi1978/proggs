using System;
using System.Windows;
using ClaudeVoiceOverlay.Services;

namespace ClaudeVoiceOverlay.Views;

/// <summary>
/// Editor fuer EINE Gemini-Korrektur-Prompt-Vorlage (Profil 1-10). Laedt die
/// aktuell wirksame Vorlage (profilspezifische SK-Datei oder eingebauter
/// Default) und speichert die Aenderung zurueck in die SK-Datei. Die zwei
/// Knoepfe fuegen die Platzhalter {{TEXT}} und {{WOERTERBUCH}} an der
/// Cursor-Position ein (Frank-Wunsch 2026-06-22, Etappe 2b).
/// </summary>
public partial class GeminiPromptEditDialog : Window
{
    private const string TextMarker = "{{TEXT}}";
    private const string VocabMarker = "{{WOERTERBUCH}}";

    private readonly int _profile;
    public bool Saved { get; private set; }

    public GeminiPromptEditDialog(int profile)
    {
        InitializeComponent();
        _profile = profile;
        HeaderText.Text = $"{GeminiClient.ProfileLabel(profile)} — bearbeiten";
        PromptBox.Text = GeminiClient.EffectivePrompt(profile);

        BtnInsertText.Click += (_, _) => InsertAtCursor(TextMarker);
        BtnInsertVocab.Click += (_, _) => InsertAtCursor(VocabMarker);
        BtnCancel.Click += (_, _) => { Saved = false; Close(); };
        BtnSave.Click += (_, _) =>
        {
            try
            {
                GeminiClient.SaveProfilePrompt(_profile, PromptBox.Text);
                GeminiPromptDriveSync.TryUpload();   // sofort ins Google-Drive-Backup
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

    private void InsertAtCursor(string marker)
    {
        int idx = PromptBox.SelectionStart;
        PromptBox.Text = PromptBox.Text.Remove(idx, PromptBox.SelectionLength).Insert(idx, marker);
        PromptBox.SelectionStart = idx + marker.Length;
        PromptBox.SelectionLength = 0;
        PromptBox.Focus();
    }

    /// <summary>Oeffnet den Editor modal fuer ein Profil. True, wenn gespeichert wurde.</summary>
    public static bool Ask(Window owner, int profile)
    {
        var dlg = new GeminiPromptEditDialog(profile) { Owner = owner };
        dlg.ShowDialog();
        return dlg.Saved;
    }
}

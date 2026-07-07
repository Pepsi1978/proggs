using System;
using System.Globalization;
using System.Windows;
using System.Windows.Input;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// Modaler Editor fuer einen Historie-Eintrag. Wird per Rechtsklick auf
/// eine Zeile im PromptHistoryWindow geoeffnet. Der Benutzer kann den
/// gespeicherten Prompt-Text frei aendern und mit Speichern bestaetigen
/// oder mit Abbrechen verwerfen. Titel und Zeitstempel werden NICHT
/// veraendert — der Titel ist KI-generiert und der Zeitstempel ist der
/// Zeitpunkt des urspruenglichen Sendens (Sortier-Schluessel).
/// </summary>
public partial class PromptHistoryEditDialog : Window
{
    /// <summary>Der bearbeitete Text. Wird vom Aufrufer ausgelesen wenn ShowDialog true zurueckgibt.</summary>
    public string EditedText => EditBox.Text ?? string.Empty;

    public PromptHistoryEditDialog(string text, string title, DateTime timestampUtc)
    {
        InitializeComponent();

        // Vorhandenen Text laden — wir wollen den exakten Originaltext
        // (mit Zeilenumbruechen, Tabs etc.) im Editor sehen, nicht die
        // Vorschau-glatt-gezogene Variante aus der Historien-Liste.
        EditBox.Text = text ?? string.Empty;

        var local = timestampUtc.ToLocalTime();
        string when = local.ToString("dd.MM.yyyy · HH:mm", CultureInfo.GetCultureInfo("de-DE"));
        string safeTitle = string.IsNullOrWhiteSpace(title) ? "Ohne Titel" : title;
        MetaLabel.Text = $"{safeTitle}  ·  {when}";

        BtnSave.Click   += (_, _) => { DialogResult = true;  Close(); };
        BtnCancel.Click += (_, _) => { DialogResult = false; Close(); };

        // Tastatur-Komfort:
        //  - Esc bricht ab
        //  - Strg+Enter speichert (Enter alleine wuerde im Multi-Line-Feld neue Zeile bedeuten)
        PreviewKeyDown += (_, e) =>
        {
            if (e.Key == Key.Escape)
            {
                DialogResult = false;
                Close();
                e.Handled = true;
            }
            else if (e.Key == Key.Enter && (Keyboard.Modifiers & ModifierKeys.Control) == ModifierKeys.Control)
            {
                DialogResult = true;
                Close();
                e.Handled = true;
            }
        };

        // Auto-Fokus + Cursor ans Ende, damit der Benutzer sofort tippen kann.
        Loaded += (_, _) =>
        {
            EditBox.Focus();
            EditBox.CaretIndex = EditBox.Text.Length;
        };
    }
}

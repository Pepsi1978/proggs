using System;
using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// Frei schwebendes Historie-Fenster — gleiche Breite wie das
/// PromptInputWindow (760 px). Listet alle aktiven Eintraege aus dem
/// PromptHistoryService, neueste oben. Linksklick auf einen Eintrag
/// reicht den Text per <see cref="EntrySelected"/> nach aussen, damit das
/// PromptBoardPanel ihn ins Eingabefenster einfuegen kann (oeffnet das
/// Eingabefenster falls noetig). Rechtsklick auf den Hintergrund
/// verschiebt das Fenster — kein Andocken, der Benutzer waehlt seine
/// Position selbst.
/// </summary>
public partial class PromptHistoryWindow : Window
{
    /// <summary>
    /// Wird ausgeloest wenn der Benutzer einen Eintrag anklickt. Der
    /// uebergebene Text ist der gespeicherte Mittel-Anteil — Pre/Post
    /// werden beim spaeteren Submit aus dem Eingabefenster wieder
    /// angebaut, also genau wie wenn der Eintrag frisch getippt waere.
    /// </summary>
    public event Action<string>? EntrySelected;

    // ── Rechtsklick-Drag-State ──
    private bool _isDragging;
    private Point _dragStartCursor;
    private double _dragStartLeft;
    private double _dragStartTop;

    private static readonly Brush RowBackground = new SolidColorBrush(Color.FromRgb(0x25, 0x25, 0x25));
    private static readonly Brush RowHover      = new SolidColorBrush(Color.FromRgb(0x33, 0x33, 0x33));
    private static readonly Brush TitleColor    = new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00));
    private static readonly Brush MetaColor     = new SolidColorBrush(Color.FromRgb(0x9A, 0x9A, 0x9A));
    private static readonly Brush PreviewColor  = new SolidColorBrush(Color.FromRgb(0xCC, 0xCC, 0xCC));

    public PromptHistoryWindow()
    {
        InitializeComponent();
        PreviewMouseRightButtonDown += OnRightDragStart;
        PreviewMouseMove            += OnRightDragMove;
        PreviewMouseRightButtonUp   += OnRightDragEnd;
    }

    /// <summary>
    /// Setzt die Liste der angezeigten Eintraege. Wird aus dem Owner-
    /// Code (PromptBoardPanel) aufgerufen — der Owner laedt vorher die
    /// Daten via PromptHistoryService.LoadAsync, damit das Fenster keine
    /// eigenen Service-Abhaengigkeiten aufbauen muss.
    /// </summary>
    public void Render(System.Collections.Generic.IReadOnlyList<PromptHistoryEntry> entries)
    {
        HistoryList.Children.Clear();
        if (entries.Count == 0)
        {
            var empty = new TextBlock
            {
                Text = "Noch keine Historie. Sende einen Prompt — er erscheint hier.",
                Foreground = MetaColor,
                FontSize = 12,
                Margin = new Thickness(8, 12, 8, 12),
                TextWrapping = TextWrapping.Wrap,
            };
            HistoryList.Children.Add(empty);
            CountLabel.Text = "0";
            return;
        }

        for (int i = 0; i < entries.Count; i++)
        {
            HistoryList.Children.Add(BuildRow(entries[i]));
        }
        CountLabel.Text = entries.Count.ToString(CultureInfo.InvariantCulture);
    }

    private Border BuildRow(PromptHistoryEntry entry)
    {
        // Pro Eintrag: Border (Hintergrund) → StackPanel mit Titel + Meta + Preview
        var titleText = new TextBlock
        {
            Text = string.IsNullOrWhiteSpace(entry.Title) ? "Ohne Titel" : entry.Title,
            Foreground = TitleColor,
            FontWeight = FontWeights.SemiBold,
            FontSize = 13,
            TextTrimming = TextTrimming.CharacterEllipsis,
        };

        var local = entry.Timestamp.ToLocalTime();
        var metaText = new TextBlock
        {
            Text = local.ToString("dd.MM.yyyy · HH:mm", CultureInfo.GetCultureInfo("de-DE")),
            Foreground = MetaColor,
            FontSize = 10,
            Margin = new Thickness(0, 2, 0, 0),
        };

        // Vorschau: erste ~120 Zeichen ohne Zeilenumbrueche
        string preview = (entry.Text ?? string.Empty).Replace('\n', ' ').Replace('\r', ' ');
        if (preview.Length > 140) preview = preview[..140] + "…";
        var previewText = new TextBlock
        {
            Text = preview,
            Foreground = PreviewColor,
            FontSize = 11,
            Margin = new Thickness(0, 4, 0, 0),
            TextTrimming = TextTrimming.CharacterEllipsis,
        };

        var stack = new StackPanel { Orientation = Orientation.Vertical };
        stack.Children.Add(titleText);
        stack.Children.Add(metaText);
        if (preview.Length > 0) stack.Children.Add(previewText);

        var row = new Border
        {
            Background = RowBackground,
            CornerRadius = new CornerRadius(6),
            Padding = new Thickness(10, 8, 10, 8),
            Margin = new Thickness(0, 0, 0, 6),
            Cursor = Cursors.Hand,
            Child = stack,
            // Tag merkt sich den Text, damit der Click-Handler ihn liefern kann.
            Tag = entry.Text ?? string.Empty,
        };

        // Hover-Effekt: dezenter Helligkeitswechsel.
        row.MouseEnter += (_, _) => row.Background = RowHover;
        row.MouseLeave += (_, _) => row.Background = RowBackground;

        // Linksklick → Eintrag waehlen.
        row.MouseLeftButtonUp += (_, e) =>
        {
            if (row.Tag is string t && !string.IsNullOrEmpty(t))
            {
                EntrySelected?.Invoke(t);
            }
            e.Handled = true;
        };

        return row;
    }

    // ── Rechtsklick-Drag ──────────────────────────────────────────────────

    private void OnRightDragStart(object sender, MouseButtonEventArgs e)
    {
        // Drag nur starten wenn der Klick NICHT auf einer scrollbaren
        // Innenflaeche endet — sonst kann der Benutzer keinen Rechtsklick-
        // Kontextzielmechanismus nutzen. Hier gibt es keinen, also einfach
        // den Drag immer starten.
        _isDragging = true;
        _dragStartCursor = e.GetPosition(this);
        _dragStartLeft = Left;
        _dragStartTop = Top;
        CaptureMouse();
    }

    private void OnRightDragMove(object sender, MouseEventArgs e)
    {
        if (!_isDragging) return;
        var current = e.GetPosition(this);
        Left = _dragStartLeft + (current.X - _dragStartCursor.X);
        Top  = _dragStartTop  + (current.Y - _dragStartCursor.Y);
    }

    private void OnRightDragEnd(object sender, MouseButtonEventArgs e)
    {
        if (!_isDragging) return;
        _isDragging = false;
        ReleaseMouseCapture();
    }
}

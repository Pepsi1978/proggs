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

    /// <summary>
    /// Wird beim Rechtsklick auf einen Eintrag ausgeloest. Der Aufrufer
    /// (PromptBoardPanel) oeffnet daraufhin den Editor-Dialog, persistiert
    /// die Aenderung via PromptHistoryService.UpdateTextAsync und stoesst
    /// einen Cloud-Sync an. Rechtsklick auf den Hintergrund verschiebt
    /// stattdessen die ganze Fenster-Gruppe (siehe GroupDragDelta).
    /// </summary>
    public event Action<PromptHistoryEntry>? EntryEditRequested;

    /// <summary>
    /// Wird beim Rechtsklick-Drag fuer jeden Mausschritt ausgeloest. Das
    /// Promptboard verschiebt daraufhin die GANZE Gruppe (Pillar +
    /// Promptboard + Eingabe + Historie) um den gleichen Versatz. Wir
    /// bewegen uns NIE selbst — die Andockung bleibt dadurch starr.
    /// </summary>
    public event Action<double, double>? GroupDragDelta;

    // ── Rechtsklick-Drag-State ──
    private bool _isDragging;
    private Point _dragStartScreen;

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

        // Vorschau: IMMER 3 Zeilen Platz, danach mit … abgeschnitten. Wir
        // glaetten Tabs / Zeilenumbrueche zu Leerzeichen damit ein langer
        // mehrzeiliger Prompt nicht nur die ersten paar Worte seiner ersten
        // Zeile zeigt — die drei sichtbaren Zeilen sollen soviel Inhalt wie
        // moeglich preisgeben.
        string preview = (entry.Text ?? string.Empty)
            .Replace('\t', ' ').Replace('\n', ' ').Replace('\r', ' ');
        if (preview.Length > 420) preview = preview[..420] + "…";
        var previewText = new TextBlock
        {
            Text = preview,
            Foreground = PreviewColor,
            FontSize = 11,
            Margin = new Thickness(0, 4, 0, 0),
            TextWrapping = TextWrapping.Wrap,
            // 3 Zeilen reservieren: bei FontSize 11 ca. 14.6 px pro Zeile,
            // 48 px ergeben sicher drei volle Zeilen plus einen Hauch
            // Atemluft. Min == Max = harte Hoehe damit JEDE Zeile in der
            // Liste exakt gleich hoch ist (auch bei kurzen Prompts) — der
            // Benutzer wollte "immer drei Zeilen sichtbar". WPF kennt im
            // klassischen .NET Framework kein MaxLines — MinHeight +
            // MaxHeight + Wrap + Trim ist der etablierte Workaround dafuer.
            MinHeight = 48,
            MaxHeight = 48,
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
            // Tag merkt sich den vollstaendigen Eintrag, damit Linksklick
            // den Text liefern und Rechtsklick den Editor mit Titel +
            // Zeitstempel oeffnen kann.
            Tag = entry,
        };

        // Hover-Effekt: dezenter Helligkeitswechsel.
        row.MouseEnter += (_, _) => row.Background = RowHover;
        row.MouseLeave += (_, _) => row.Background = RowBackground;

        // Linksklick → Eintrag waehlen.
        row.MouseLeftButtonUp += (_, e) =>
        {
            if (row.Tag is PromptHistoryEntry ent && !string.IsNullOrEmpty(ent.Text))
            {
                EntrySelected?.Invoke(ent.Text);
            }
            e.Handled = true;
        };

        return row;
    }

    // ── Andocken am Promptboard (gleiches Muster wie PromptInputWindow) ──

    /// <summary>
    /// Dockt das Fenster an die linke Seite des uebergebenen Promptboards
    /// an — Hoehe wird angeglichen, x-Position 4 px links neben dem Board.
    /// Andockung ist absolut: kein "manuell positioniert"-Konzept mehr.
    /// </summary>
    public void DockTo(Window promptBoard, bool force = false)
    {
        if (promptBoard is null) return;

        Height = promptBoard.Height;
        Top    = promptBoard.Top;
        Left   = promptBoard.Left - Width - 4;
        ClampToWorkArea();
    }

    /// <summary>
    /// Folgt einer Drag-Bewegung des Promptboards. Wird vom PromptBoardPanel
    /// bei dessen PanelDragged-Aequivalent aufgerufen — ueberschreibt auch
    /// eine manuelle Position, weil ein Promtboard-Drag das gesamte
    /// Fenster-Set als Paar wandern soll.
    /// </summary>
    public void FollowPanelDrag(Window promptBoard)
    {
        if (promptBoard is null) return;
        // Hoehe NACHZIEHEN: das Promptboard kann seine Hoehe nachtraeglich
        // aendern (Pillar-Drag → OverlayWindow.PositionPromptPanel setzt
        // _promptPanel.Height = Pillar.Height). Ohne diesen Sync laeuft
        // die Historie vertikal aus dem Promtboard heraus.
        Height = promptBoard.Height;
        Top    = promptBoard.Top;
        Left   = promptBoard.Left - Width - 4;
        // KEIN ClampToWorkArea hier — beim Drag soll die Andockung 1:1
        // mitziehen, sonst springt die Historie wegen Bildschirm-Grenze
        // auf eine andere Position als das Promtboard.
    }

    private void ClampToWorkArea()
    {
        var area = SystemParameters.WorkArea;
        if (Left < area.Left)              Left = area.Left;
        if (Top  < area.Top)               Top  = area.Top;
        if (Left + Width > area.Right)     Left = area.Right - Width;
        if (Top  + Height > area.Bottom)   Top  = area.Bottom - Height;
    }

    // ── Rechtsklick-Drag ──────────────────────────────────────────────────

    private void OnRightDragStart(object sender, MouseButtonEventArgs e)
    {
        // Wenn der Klick eine Historie-Zeile getroffen hat, gehoert er dem
        // Eintrag — Editor oeffnen, NICHT die Fenster-Gruppe ziehen.
        if (FindRowEntry(e.OriginalSource) is PromptHistoryEntry entry)
        {
            EntryEditRequested?.Invoke(entry);
            e.Handled = true;
            return;
        }
        _isDragging = true;
        _dragStartScreen = PointToScreen(e.GetPosition(this));
        CaptureMouse();
    }

    /// <summary>
    /// Laeuft den Visual-Tree vom OriginalSource nach oben bis ein Border
    /// mit einem PromptHistoryEntry im Tag gefunden wird. So koennen wir
    /// auch Rechtsklicks auf die TextBlocks innerhalb der Zeile dem
    /// richtigen Eintrag zuordnen.
    /// </summary>
    private static PromptHistoryEntry? FindRowEntry(object originalSource)
    {
        DependencyObject? node = originalSource as DependencyObject;
        while (node is not null)
        {
            if (node is Border b && b.Tag is PromptHistoryEntry e) return e;
            node = VisualTreeHelper.GetParent(node) ?? LogicalTreeHelper.GetParent(node);
        }
        return null;
    }

    private void OnRightDragMove(object sender, MouseEventArgs e)
    {
        if (!_isDragging) return;
        // Wir bewegen UNS NICHT direkt — stattdessen melden wir den Mouse-
        // Delta an das Promptboard, das daraufhin die GANZE Gruppe
        // (Pillar + Promptboard + Eingabe + Historie) um den gleichen
        // Versatz verschiebt. Wir selbst werden vom Promptboard ueber
        // FollowPanelDrag zurueckgezogen — die Andockung bleibt damit
        // immer exakt bei 4 px Abstand.
        var cur = PointToScreen(e.GetPosition(this));
        double dx = cur.X - _dragStartScreen.X;
        double dy = cur.Y - _dragStartScreen.Y;
        if (dx == 0 && dy == 0) return;
        GroupDragDelta?.Invoke(dx, dy);
        _dragStartScreen = cur;
    }

    private void OnRightDragEnd(object sender, MouseButtonEventArgs e)
    {
        if (!_isDragging) return;
        _isDragging = false;
        ReleaseMouseCapture();
    }
}

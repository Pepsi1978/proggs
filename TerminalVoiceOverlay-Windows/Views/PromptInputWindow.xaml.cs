using System;
using System.Windows;
using System.Windows.Input;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// Tippbares Prompt-Eingabefenster, das links am PromptBoardPanel andockt.
/// Enter sendet (loest <see cref="SubmitRequested"/> aus), Shift+Enter macht
/// einen Zeilenumbruch. Rechtsklick auf den Hintergrund verschiebt das Fenster
/// frei — das setzt das Andock-Tracking aus, damit der Benutzer eine eigene
/// Position waehlen kann ohne dass das Promptboard sie wieder ueberschreibt.
/// </summary>
public partial class PromptInputWindow : Window
{
    /// <summary>
    /// Wird ausgeloest wenn der Benutzer Enter drueckt (ohne Shift). Der
    /// uebergebene Text ist der aktuelle Inhalt der Eingabe-Box; das Fenster
    /// selbst leert die Box danach.
    /// </summary>
    public event Action<string>? SubmitRequested;

    /// <summary>
    /// Wird beim Rechtsklick-Drag fuer jeden Mausschritt ausgeloest.
    /// Das Promptboard reagiert darauf und verschiebt die GANZE Gruppe
    /// (Pillar + Promptboard + Eingabe + Historie) um den gleichen Delta.
    /// Wir ueberschreiben hier NICHT mehr unsere eigene Position direkt —
    /// das Promptboard zieht uns als Andockpartner mit, sobald es selbst
    /// gewandert ist. So bleibt die Andock-Geometrie immer erhalten.
    /// </summary>
    public event Action<double, double>? GroupDragDelta;

    // ── Rechtsklick-Drag-State ──
    private bool _isDragging;
    private Point _dragStartScreen;

    public PromptInputWindow()
    {
        InitializeComponent();

        InputBox.PreviewKeyDown += InputBox_PreviewKeyDown;

        // Rechtsklick auf das Window selbst startet einen Drag.
        PreviewMouseRightButtonDown += OnRightDragStart;
        PreviewMouseMove            += OnRightDragMove;
        PreviewMouseRightButtonUp   += OnRightDragEnd;

        // Beim Anzeigen Fokus in die Box setzen, damit der Benutzer
        // direkt lostippen kann.
        Loaded += (_, _) => InputBox.Focus();
    }

    /// <summary>
    /// Setzt das Eingabefeld auf den Text und positioniert den Cursor ans
    /// Ende. Wird vom Voice-Overlay aufgerufen, wenn ein gesprochener Prompt
    /// in das Eingabefenster geroutet wird (statt direkt in die CLI).
    /// </summary>
    public void SetText(string text)
    {
        InputBox.Text = text ?? string.Empty;
        InputBox.CaretIndex = InputBox.Text.Length;
        InputBox.Focus();
    }

    /// <summary>
    /// Fuegt Text an die aktuelle Cursor-Position ein. Wird vom Voice-Overlay
    /// genutzt wenn das Eingabefeld bereits Inhalt hat und der Benutzer einen
    /// weiteren Voice-Schnipsel anhaengen will.
    /// </summary>
    public void AppendText(string text)
    {
        if (string.IsNullOrEmpty(text)) return;
        int caret = InputBox.CaretIndex;
        InputBox.Text = InputBox.Text.Insert(caret, text);
        InputBox.CaretIndex = caret + text.Length;
        InputBox.Focus();
    }

    /// <summary>
    /// Wird vom Voice-Overlay aufgerufen, wenn ein gesprochener Prompt ins
    /// Eingabefenster geroutet wird. Verhalten:
    /// <list type="bullet">
    /// <item>Box leer → Text wird gesetzt (frischer Start).</item>
    /// <item>Box nicht leer → bestehender Inhalt bleibt erhalten, der neue
    /// Voice-Schnipsel wird mit " ; " als Aufgaben-Trenner angehaengt. So
    /// kann der Benutzer mehrere Aufgaben hintereinander einsprechen ohne
    /// dass die vorhergehende ueberschrieben wird.</item>
    /// <item>Wenn <paramref name="autoSubmit"/> true ist (Auto-Enter-Toggle
    /// im Voice-Overlay aktiv), wird das Submit-Event direkt ausgeloest —
    /// als haette der Benutzer Enter gedrueckt.</item>
    /// </list>
    /// </summary>
    public void AppendVoiceText(string text, bool autoSubmit)
    {
        if (string.IsNullOrEmpty(text)) return;

        var existing = InputBox.Text ?? string.Empty;
        string combined = string.IsNullOrWhiteSpace(existing)
            ? text
            : existing.TrimEnd() + " ; " + text;

        InputBox.Text = combined;
        InputBox.CaretIndex = InputBox.Text.Length;
        InputBox.Focus();

        if (autoSubmit)
        {
            SubmitRequested?.Invoke(combined);
        }
    }

    /// <summary>Leert die Eingabe-Box und setzt den Fokus zurueck hinein.</summary>
    public void ClearInput()
    {
        InputBox.Text = string.Empty;
        InputBox.Focus();
    }

    /// <summary>Aktualisiert die kleine Pre/Post-Vorschau unter der Box.</summary>
    public void UpdatePreview(string preview)
    {
        PreviewLabel.Text = preview ?? string.Empty;
    }

    /// <summary>
    /// Dockt das Fenster an die linke Seite des uebergebenen Promptboards
    /// an. Wird beim Oeffnen aufgerufen und nach jedem Drag (egal an
    /// welchem Fenster der Benutzer angefasst hat) — die Andockung ist
    /// jetzt absolut, kein "manuell positioniert"-Konzept mehr.
    /// </summary>
    public void DockTo(Window promptBoard, bool force = false)
    {
        if (promptBoard is null) return;

        // Hoehe an Promptboard angleichen, damit die beiden Fenster
        // visuell als zusammengehoeriges Paar erscheinen.
        Height = promptBoard.Height;
        Top    = promptBoard.Top;
        // 4-Pixel-Naht zwischen Eingabefenster und Promptboard.
        Left   = promptBoard.Left - Width - 4;

        ClampToWorkArea();
    }

    /// <summary>
    /// Folgt einer Drag-Bewegung des Promptboards. Wird vom PromptBoardPanel
    /// aufgerufen wenn dessen <c>PanelDragged</c>-Event feuert. Ueberschreibt
    /// auch eine manuelle Position — wenn das Promptboard sich bewegt, soll
    /// das angedockte Eingabefenster mitwandern, sonst wirken die beiden
    /// Fenster nicht mehr als Paar.
    /// </summary>
    public void FollowPanelDrag(Window promptBoard)
    {
        if (promptBoard is null) return;
        // Hoehe NACHZIEHEN: das Promptboard kann seine Hoehe nachtraeglich
        // aendern (Pillar-Drag → OverlayWindow.PositionPromptPanel setzt
        // _promptPanel.Height = Pillar.Height). Wenn wir die Hoehe nicht
        // mitziehen, sieht das Eingabe-Fenster vertikal versetzt aus.
        Height = promptBoard.Height;
        Top    = promptBoard.Top;
        Left   = promptBoard.Left - Width - 4;
        // KEIN ClampToWorkArea hier — beim Drag soll die Andockung 1:1
        // mitziehen, sonst springt das Kind wegen Bildschirm-Grenze auf
        // eine andere Position als das Promtboard und reisst die Naht auf.
    }

    /// <summary>
    /// Verhindert, dass das Fenster ausserhalb des sichtbaren Bildschirms
    /// landet — kann passieren wenn das Promptboard sehr weit links sitzt
    /// und unser Eingabefenster (doppelte Promptboard-Breite) keinen Platz
    /// mehr hat.
    /// </summary>
    private void ClampToWorkArea()
    {
        var area = SystemParameters.WorkArea;
        if (Left < area.Left)              Left = area.Left;
        if (Top  < area.Top)               Top  = area.Top;
        if (Left + Width > area.Right)     Left = area.Right - Width;
        if (Top  + Height > area.Bottom)   Top  = area.Bottom - Height;
    }

    // ── Eingabe-Tasten ────────────────────────────────────────────────────

    private void InputBox_PreviewKeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key != Key.Return && e.Key != Key.Enter) return;

        bool shiftDown = (Keyboard.Modifiers & ModifierKeys.Shift) == ModifierKeys.Shift;
        if (shiftDown)
        {
            // Default-Verhalten von AcceptsReturn=True: einen Zeilenumbruch
            // an der Cursor-Position einfuegen — explizit nichts tun, damit
            // WPF das selbst erledigt.
            return;
        }

        // Enter ohne Shift: Submit ausloesen.
        e.Handled = true;
        var text = InputBox.Text ?? string.Empty;
        SubmitRequested?.Invoke(text);
    }

    // ── Rechtsklick-Drag ──────────────────────────────────────────────────

    private void OnRightDragStart(object sender, MouseButtonEventArgs e)
    {
        // Rechtsklick auf die TextBox darf NICHT den Drag starten — sonst
        // koennte der Benutzer kein Kontextmenue mehr nutzen. Wir starten
        // nur wenn das Klick-Ziel das Window oder der Border ist.
        if (e.OriginalSource is System.Windows.Controls.TextBox) return;

        _isDragging      = true;
        _dragStartScreen = PointToScreen(e.GetPosition(this));
        CaptureMouse();
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
        // Baseline auf die neue Cursor-Position setzen, sonst summieren
        // sich die Deltas und das Fenster rast davon.
        _dragStartScreen = cur;
    }

    private void OnRightDragEnd(object sender, MouseButtonEventArgs e)
    {
        if (!_isDragging) return;
        _isDragging = false;
        ReleaseMouseCapture();
    }
}

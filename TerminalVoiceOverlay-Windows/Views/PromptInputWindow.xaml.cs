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
    /// Hat der Benutzer das Fenster per Rechtsklick selbst verschoben? Wenn
    /// ja, folgen wir dem Promptboard nicht mehr automatisch. Solange das
    /// Promptboard das Eingabefenster bewegt (z.B. weil das ganze Pillar
    /// per Drag verschoben wird), bleibt der Wert false.
    /// </summary>
    private bool _manuallyPositioned;

    // ── Rechtsklick-Drag-State ──
    private bool _isDragging;
    private Point _dragStartCursor;
    private double _dragStartLeft;
    private double _dragStartTop;

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
    /// Dockt das Fenster an die linke Seite des uebergebenen Promptboards an.
    /// Respektiert <see cref="_manuallyPositioned"/> — wenn der Benutzer
    /// selbst verschoben hat, wird die Andockung nicht erzwungen.
    /// </summary>
    public void DockTo(Window promptBoard, bool force = false)
    {
        if (promptBoard is null) return;
        if (_manuallyPositioned && !force) return;

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
        Top  = promptBoard.Top;
        Left = promptBoard.Left - Width - 4;
        ClampToWorkArea();
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

        _isDragging         = true;
        _dragStartCursor    = e.GetPosition(this);
        _dragStartLeft      = Left;
        _dragStartTop       = Top;
        _manuallyPositioned = true;
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
        ClampToWorkArea();
    }
}

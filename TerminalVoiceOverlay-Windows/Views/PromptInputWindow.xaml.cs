using System;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using TerminalVoiceOverlay.Services;

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

    /// <summary>
    /// Feuert beim Klick auf den neuen Stern-Button in der Toolbar oben.
    /// Das Argument ist der NEUE gewuenschte Solo-Andock-Zustand:
    /// <c>true</c> = Promptboard ausblenden und dieses Fenster direkt ans
    /// Voice-Overlay andocken; <c>false</c> = zurueck in den Normalmodus
    /// (Promptboard sichtbar, dieses Fenster dockt links daran an).
    /// PromptBoardPanel reicht das Event ans OverlayWindow weiter, das den
    /// eigentlichen Layout-Wechsel umsetzt.
    /// </summary>
    public event Action<bool>? SoloDockToggleRequested;

    // ── Rechtsklick-Drag-State ──
    private bool _isDragging;
    private Point _dragStartScreen;

    // ── Gemini-Verbesserung ──
    // Verhindert dass der Benutzer waehrend einer laufenden Gemini-Anfrage
    // den Button erneut drueckt und parallele Calls ausloest.
    private bool _isImproving;

    // ── Solo-Andock-Modus ──
    // True wenn der Benutzer den Stern in dieser Toolbar geklickt hat,
    // sodass das Promptboard ausgeblendet ist und dieses Fenster direkt
    // am Voice-Overlay haengt. Wird vom OverlayWindow ueber
    // SetSoloDockState gesetzt nachdem der Layout-Wechsel passiert ist —
    // damit Visual und Realitaet synchron bleiben.
    private bool _isSoloDock;

    public PromptInputWindow()
    {
        InitializeComponent();

        InputBox.PreviewKeyDown += InputBox_PreviewKeyDown;

        // Rechtsklick auf das Window selbst startet einen Drag.
        PreviewMouseRightButtonDown += OnRightDragStart;
        PreviewMouseMove            += OnRightDragMove;
        PreviewMouseRightButtonUp   += OnRightDragEnd;

        // G-Button nur aktiv wenn Gemini ueberhaupt konfiguriert ist
        // (kein GEMINI_API_KEY in .env → grau).
        GeminiButton.IsEnabled = VoiceServiceProvider.GeminiAvailable;

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
    /// Voice-Schnipsel wird mit einem einfachen Leerzeichen angehaengt.
    /// Mehrere Voice-Inputs verschmelzen so zu EINEM Fliesstext — der
    /// Benutzer kann denselben Gedanken mehrfach einsprechen ohne dass
    /// automatisch ein Aufgaben-Trenner dazwischen geraet. Wer mehrere
    /// Aufgaben einsprechen will, fuegt den Trenner manuell ueber den
    /// ;-Button in der Toolbar ein.</item>
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
            : existing.TrimEnd() + " " + text.TrimStart();

        InputBox.Text = combined;
        InputBox.CaretIndex = InputBox.Text.Length;

        // Fokus-Garantie: Das Fenster wird mit ShowActivated="False" geoeffnet
        // (damit der Stern-Toggle den Terminal-Fokus nicht stiehlt). Nach dem
        // Voice-Insert MUSS das Fenster aber aktiv sein, sonst geht der
        // naechste Enter-Tastendruck ans Terminal statt an die InputBox und
        // der Benutzer kann den eingesprochenen Prompt nicht abschicken.
        Activate();
        InputBox.Focus();
        Keyboard.Focus(InputBox);

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

    /// <summary>
    /// Liefert den aktuellen Inhalt der Eingabe-Box. Wird vom PromptBoardPanel
    /// verwendet, um den Text VOR einem Stern-Toggle (das das Fenster zerstoert)
    /// zwischenzuspeichern, damit der Benutzer beim erneuten Einblenden seinen
    /// noch nicht abgeschickten Text wiederfindet.
    /// </summary>
    public string GetCurrentText() => InputBox.Text ?? string.Empty;

    /// <summary>
    /// Fuegt manuell einen Aufgaben-Trenner hinter dem aktuellen Text ein:
    /// Leerzeichen, Semikolon, Leerzeichen — also genau das gleiche Muster
    /// das frueher automatisch zwischen Voice-Inputs verwendet wurde
    /// (" ; "). So kann der Benutzer mehrere Aufgaben in einer Zeile
    /// trennen ohne dass das Eingabefeld vertikal aufgeblaeht wird.
    /// Bei leerer Box wird nichts eingefuegt (ein Trenner ohne Inhalt davor
    /// waere sinnlos und wuerde nur fuehrendes Leerzeichen erzeugen).
    /// </summary>
    public void InsertManualSeparator()
    {
        var existing = InputBox.Text ?? string.Empty;
        if (string.IsNullOrWhiteSpace(existing))
        {
            InputBox.Focus();
            return;
        }

        // existing.TrimEnd() entfernt evtl. bereits vorhandene Whitespace/Newlines
        // am Ende, damit wir nicht doppelt Leerzeichen produzieren wenn der
        // Benutzer den Button mehrmals hintereinander drueckt oder direkt
        // nach Shift+Enter.
        string trimmed = existing.TrimEnd();
        string separator = " ; ";
        InputBox.Text = trimmed + separator;
        InputBox.CaretIndex = InputBox.Text.Length;
        InputBox.ScrollToEnd();
        InputBox.Focus();
        Keyboard.Focus(InputBox);
    }

    /// <summary>
    /// Wird vom Voice-Overlay aufgerufen wenn der Benutzer auf den orangen
    /// Enter-Button klickt: Falls die Eingabe-Box Text enthaelt, wird das
    /// SubmitRequested-Event ausgeloest (so als haette der Benutzer Enter
    /// in der Box gedrueckt) und true zurueckgegeben. Bei leerer Box passiert
    /// nichts und es wird false zurueckgegeben — der Aufrufer fuehrt dann
    /// seine Standard-Logik aus (Toggle).
    /// </summary>
    public bool TrySubmitText()
    {
        var text = InputBox.Text ?? string.Empty;
        if (string.IsNullOrWhiteSpace(text)) return false;
        SubmitRequested?.Invoke(text);
        return true;
    }

    /// <summary>
    /// Click-Handler fuer den roten X-Button in der Toolbar oben rechts.
    /// Loescht ausschliesslich den Text in der Eingabe-Box dieses Fensters —
    /// die CLI-Zeile im Terminal bleibt unangetastet (dafuer gibt es das
    /// X im Voice-Overlay).
    /// </summary>
    private void BtnClearInput_Click(object sender, RoutedEventArgs e)
    {
        ClearInput();
    }

    /// <summary>
    /// Click-Handler fuer den neuen Semikolon-Button (links neben dem G-Button).
    /// Fuegt manuell einen Aufgaben-Trenner ans Ende des Eingabe-Textes ein
    /// (Leerzeile + Semikolon + Leerzeile). Damit ersetzt der Benutzer das
    /// frueher automatische " ; " zwischen Voice-Inputs durch eine bewusste
    /// manuelle Aktion.
    /// </summary>
    private void BtnInsertSeparator_Click(object sender, RoutedEventArgs e)
    {
        InsertManualSeparator();
        FlashSeparatorButton();
    }

    /// <summary>
    /// Laesst den Semikolon-Button kurz aufleuchten als visuelle Bestaetigung
    /// dass der Aufgaben-Trenner gesetzt wurde. Drei parallele Animationen
    /// ueber jeweils 400ms:
    /// 1. DropShadow-Opacity geht von 1.0 → 0.0 (Glow-Schein verschwindet)
    /// 2. DropShadow-BlurRadius geht von 18 → 0 (Glow-Radius schrumpft)
    /// 3. TextBlock-Foreground geht von Gold (#FFFFE680) → Hellblau (#FF7FD0FF)
    ///    (das Semikolon-Zeichen blitzt golden auf und verblasst zum Standard).
    /// Damit sieht der Benutzer einen klaren goldenen Blitz, der genau im Moment
    /// des Klicks erscheint und schnell wieder verschwindet — ohne den Workflow
    /// zu unterbrechen.
    /// </summary>
    private void FlashSeparatorButton()
    {
        var duration = new Duration(TimeSpan.FromMilliseconds(400));

        var opacityAnim = new DoubleAnimation
        {
            From = 1.0,
            To = 0.0,
            Duration = duration,
            FillBehavior = FillBehavior.Stop
        };
        var blurAnim = new DoubleAnimation
        {
            From = 18.0,
            To = 0.0,
            Duration = duration,
            FillBehavior = FillBehavior.Stop
        };
        var colorAnim = new ColorAnimation
        {
            From = Color.FromRgb(0xFF, 0xE6, 0x80),
            To   = Color.FromRgb(0x7F, 0xD0, 0xFF),
            Duration = duration,
            FillBehavior = FillBehavior.Stop
        };

        // Sicherstellen dass die Ruhezustaende nach der Animation wieder
        // gesetzt sind — FillBehavior.Stop laesst die Werte sonst auf
        // ihrem Endwert haengen, was hier zwar bereits dem Ruhewert
        // entspricht, aber explizit ist robuster gegen spaetere Aenderungen.
        SeparatorGlow.BeginAnimation(System.Windows.Media.Effects.DropShadowEffect.OpacityProperty, opacityAnim);
        SeparatorGlow.BeginAnimation(System.Windows.Media.Effects.DropShadowEffect.BlurRadiusProperty, blurAnim);

        // Foreground-Animation laeuft auf einem SolidColorBrush — der
        // statische Brush aus der XAML-Definition kann nicht direkt
        // animiert werden (frozen), also setzen wir vor der Animation
        // einen frischen, animierbaren Brush und animieren dessen
        // Color-Property.
        var animBrush = new SolidColorBrush(Color.FromRgb(0xFF, 0xE6, 0x80));
        SeparatorGlyph.Foreground = animBrush;
        animBrush.BeginAnimation(SolidColorBrush.ColorProperty, colorAnim);
    }

    /// <summary>
    /// Click-Handler fuer den Stern ganz links in der Toolbar. Schaltet den
    /// Solo-Andock-Modus um: aktiv = Promptboard verschwindet und dieses
    /// Fenster dockt direkt ans Voice-Overlay an, inaktiv = zurueck in den
    /// Normalmodus. Wir feuern nur das Event — den eigentlichen Layout-
    /// Wechsel macht das OverlayWindow (das die Geometrien aller Fenster
    /// kennt). Das Visual des Sterns wird vom OverlayWindow per
    /// <see cref="SetSoloDockState"/> nachgezogen, sobald der Wechsel
    /// erfolgreich war.
    /// </summary>
    private void BtnSoloDockStar_Click(object sender, RoutedEventArgs e)
    {
        bool newState = !_isSoloDock;
        SoloDockToggleRequested?.Invoke(newState);
    }

    /// <summary>
    /// Wird vom OverlayWindow aufgerufen nachdem der Solo-Andock-Layout-
    /// Wechsel umgesetzt wurde. Synchronisiert intern das Flag und
    /// aktualisiert das Stern-Visual (gefuellter Gold-Stern wenn aktiv,
    /// weisser Outline-Stern wenn inaktiv) — 1:1 wie der Stern in der
    /// Promptboard-Toolbar.
    /// </summary>
    public void SetSoloDockState(bool active)
    {
        _isSoloDock = active;
        UpdateSoloStarVisual();
    }

    private void UpdateSoloStarVisual()
    {
        // Segoe Fluent Icons — gleiche Glyphen und Farben wie der Stern
        // im Promptboard, damit beide Buttons visuell identisch sind:
        //   E735 = FavoriteStarFill  (gefuellt, gold)
        //   E734 = FavoriteStar      (Outline, weiss)
        if (_isSoloDock)
        {
            SoloDockStarButton.Content    = "";
            SoloDockStarButton.Foreground = new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00));
            SoloDockStarButton.ToolTip    = "Promptboard wieder einblenden (zurueck in den Normalmodus).";
        }
        else
        {
            SoloDockStarButton.Content    = "";
            SoloDockStarButton.Foreground = Brushes.White;
            SoloDockStarButton.ToolTip    = "Promptboard ausblenden und Eingabe direkt ans Voice-Overlay andocken (erneuter Klick blendet das Promptboard wieder ein).";
        }
    }

    /// <summary>
    /// Click-Handler fuer den gelben G-Button. Schickt den aktuellen Text der
    /// Eingabe-Box an Gemini und ersetzt ihn durch eine bereinigte Variante.
    /// Nutzt das gleiche Prompt-Engineer-Template wie der G-Button im
    /// PromptBoard-Edit-Dialog (<see cref="PromptEditDialog.RunGeminiAsync"/>):
    /// rohes Diktat → kopierfertiger Claude-Code-CLI-Prompt.
    /// Bei leerem Text oder fehlendem Gemini-Key passiert nichts ausser einer
    /// kurzen Status-Zeile in der Pre/Post-Vorschau unten.
    /// </summary>
    private async void BtnGemini_Click(object sender, RoutedEventArgs e)
    {
        await RunGeminiAsync();
    }

    private async Task RunGeminiAsync()
    {
        if (_isImproving) return;

        var gemini = VoiceServiceProvider.Gemini;
        if (gemini is null)
        {
            UpdatePreview("Gemini nicht verfuegbar (kein GEMINI_API_KEY in .env).");
            return;
        }

        var current = InputBox.Text?.Trim();
        if (string.IsNullOrEmpty(current))
        {
            UpdatePreview("Kein Text zum Verbessern.");
            return;
        }

        _isImproving = true;
        GeminiButton.IsEnabled = false;
        UpdatePreview("Verbessere mit Gemini…");
        try
        {
            var improved = await gemini.BuildClaudeCodePromptAsync(current);
            if (string.IsNullOrWhiteSpace(improved))
            {
                UpdatePreview("Gemini lieferte leere Antwort.");
                return;
            }
            InputBox.Text = improved;
            InputBox.CaretIndex = InputBox.Text.Length;
            InputBox.ScrollToEnd();
            UpdatePreview($"Verbessert ({improved.Length} Zeichen).");
            // Fokus zurueck in die Box, damit der Benutzer direkt mit Enter
            // senden kann ohne erst hineinklicken zu muessen.
            Activate();
            InputBox.Focus();
            Keyboard.Focus(InputBox);
        }
        catch (Exception ex)
        {
            UpdatePreview($"Gemini-Verbesserung fehlgeschlagen: {ex.Message}");
        }
        finally
        {
            _isImproving = false;
            GeminiButton.IsEnabled = VoiceServiceProvider.GeminiAvailable;
        }
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
    /// Dockt das Fenster im Solo-Modus direkt an die linke Kante des
    /// Voice-Overlays an. Wird vom OverlayWindow aufgerufen wenn der
    /// Benutzer den Stern in dieser Toolbar geklickt hat — das Promptboard
    /// ist dann ausgeblendet und dieses Fenster nimmt seinen Platz ein.
    /// Geometrie-Regeln identisch zu DockTo: Hoehe an Pillar angleichen,
    /// 4-Pixel-Naht, gleicher Top-Wert.
    /// </summary>
    public void DockToOverlay(Window overlay)
    {
        if (overlay is null) return;
        Height = overlay.Height;
        Top    = overlay.Top;
        Left   = overlay.Left - Width - 4;
        ClampToWorkArea();
    }

    /// <summary>
    /// Folgt einer Drag-Bewegung des Voice-Overlays im Solo-Modus.
    /// Identisch zu FollowPanelDrag, nur mit dem Pillar als Anker. Kein
    /// Clamp damit die Andock-Naht beim Drag nicht aufreisst.
    /// </summary>
    public void FollowOverlayDrag(Window overlay)
    {
        if (overlay is null) return;
        Height = overlay.Height;
        Top    = overlay.Top;
        Left   = overlay.Left - Width - 4;
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

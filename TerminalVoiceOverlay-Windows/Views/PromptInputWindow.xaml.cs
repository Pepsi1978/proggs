using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
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

        // Noch nicht abgeschickten Eingabe-Text vom letzten Mal wiederherstellen —
        // ueberlebt einen TVO-Neustart (Datenverlust-Schutz, siehe SaveDraft/RestoreDraft).
        RestoreDraft();

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

        // Prompt-Zwischenspeicher-Leiste (1…15) unten aufbauen.
        BuildSlotBar();

        // Sichtbare Versionsanzeige (Frank-Wunsch 2026-06-16): zeigt im Kopf der
        // Eingabe, welche EXE-Version live ist — damit nie unklar ist, ob ein
        // Update angekommen ist. Quelle: Assembly-Version (= csproj <Version>).
        try
        {
            var asmV = System.Reflection.Assembly.GetExecutingAssembly().GetName().Version;
            if (asmV is not null) TitleLabel.Text = $"Prompt-Eingabe \u00b7 v{asmV.Major}.{asmV.Minor}.{asmV.Build}";
        }
        catch { /* Versionsanzeige ist optional */ }
        DiagLog.Write("SlotPriority", "input window built", ("slotMenus", PromptSlotService.SlotCount.ToString()));
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

        // Sofortiges Feedback im PreviewLabel: Anzahl der erkannten Aufgaben
        // (nicht-leere Teile nach Split an " ; "). Der Benutzer sieht das
        // ohne erst nach unten scrollen zu muessen.
        int taskCount = InputBox.Text
            .Split([" ; "], StringSplitOptions.None)
            .Count(p => !string.IsNullOrWhiteSpace(p));
        UpdatePreview($"{taskCount} Aufgabe{(taskCount == 1 ? "" : "n")} erkannt");
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
    /// Kopiert den angezeigten Eingabe-Text in die System-Zwischenablage —
    /// danach ueberall mit Strg+V einfuegbar. Die Zwischenablage kann jederzeit
    /// von einem anderen Prozess gesperrt sein (CLIPBRD_E_CANT_OPEN), daher die
    /// eigene Retry-Schleife in TrySetClipboardTextAsync (Bug-Almanach C#/.NET §4.1).
    /// </summary>
    private async void BtnCopy_Click(object sender, RoutedEventArgs e)
    {
        var text = InputBox.Text ?? string.Empty;
        if (string.IsNullOrEmpty(text)) return;
        if (await TrySetClipboardTextAsync(text))
            UpdatePreview("Text kopiert — überall mit Strg+V einfügbar.");
        else
            UpdatePreview("Kopieren fehlgeschlagen — Zwischenablage gerade belegt.");
    }

    /// <summary>
    /// Setzt Text mit eigener Retry-Schleife in die Zwischenablage
    /// (CLIPBRD_E_CANT_OPEN, Bug-Almanach C#/.NET §4.1). WICHTIG: die
    /// 4-Argument-Overload SetDataObject(data, copy, retryTimes, retryDelay)
    /// gibt es NUR in System.Windows.Forms.Clipboard, NICHT im WPF-Clipboard
    /// (System.Windows.Clipboard) — darum hier die Schleife von Hand. Task.Delay
    /// statt Thread.Sleep, damit der UI-Thread nicht blockiert.
    /// </summary>
    private static async Task<bool> TrySetClipboardTextAsync(string text)
    {
        for (int attempt = 0; attempt < 10; attempt++)
        {
            try
            {
                Clipboard.SetDataObject(text, true);
                return true;
            }
            catch
            {
                await Task.Delay(100);
            }
        }
        return false;
    }

    /// <summary>
    /// Fuegt den Text aus der System-Zwischenablage an der Cursor-Position ins
    /// Eingabefeld ein (Gegenstueck zum Kopieren). Clipboard-Lesen in einer
    /// Retry-Schleife, da die Zwischenablage gesperrt sein kann (Almanach §4.1).
    /// </summary>
    private async void BtnPaste_Click(object sender, RoutedEventArgs e)
    {
        for (int attempt = 0; attempt < 10; attempt++)
        {
            try
            {
                if (!Clipboard.ContainsText()) return;
                var text = Clipboard.GetText();
                if (!string.IsNullOrEmpty(text)) AppendText(text);
                return;
            }
            catch
            {
                await Task.Delay(100);
            }
        }
        UpdatePreview("Einfügen fehlgeschlagen — Zwischenablage gerade belegt.");
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
    /// aktualisiert NUR den Tooltip-Text — das Stern-Visual bleibt auf
    /// Wunsch des Benutzers (Frank, 2026-05-09) IMMER goldig, unabhaengig
    /// vom Solo-Modus. Der gefuellte Gold-Stern (E735) ist im XAML fix
    /// verdrahtet (Foreground=#FFD700).
    /// </summary>
    public void SetSoloDockState(bool active)
    {
        _isSoloDock = active;
        UpdateSoloStarVisual();
    }

    private void UpdateSoloStarVisual()
    {
        // Visual bleibt fix (Glyph E735 + Gold #FFD700 aus XAML). Nur der
        // Tooltip aendert sich, damit der Benutzer trotzdem erkennt was der
        // naechste Klick MACHEN wird — auch ohne Farbwechsel.
        SoloDockStarButton.ToolTip = _isSoloDock
            ? "Promptboard wieder einblenden (zurueck in den Normalmodus)."
            : "Promptboard ausblenden und Eingabe direkt ans Voice-Overlay andocken (erneuter Klick blendet das Promptboard wieder ein).";
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

    // ── Fokus-Indikator ───────────────────────────────────────────────────

    /// <summary>
    /// Hebt die umgebende Border beim Tastatur-Fokus hervor: BorderBrush
    /// wechselt auf gedaempftes Blau (#FF6A9FD8) statt dem haesslichen
    /// gestrichelten WPF-Standard-FocusVisualStyle (der auf der TextBox
    /// per FocusVisualStyle="{x:Null}" deaktiviert wurde).
    /// </summary>
    private void InputBox_GotFocus(object sender, RoutedEventArgs e)
    {
        InputBorder.BorderBrush = new SolidColorBrush(Color.FromRgb(0x6A, 0x9F, 0xD8));
    }

    /// <summary>
    /// Setzt den Border-Rahmen zurueck auf den Ruhezustand (#FF3A3A3A)
    /// sobald die TextBox den Fokus verliert.
    /// </summary>
    private void InputBox_LostFocus(object sender, RoutedEventArgs e)
    {
        InputBorder.BorderBrush = new SolidColorBrush(Color.FromRgb(0x3A, 0x3A, 0x3A));
    }

    // ── Live-Vorschau (TextChanged) ───────────────────────────────────────

    /// <summary>
    /// Fuellt das PreviewLabel bei jeder Texteingabe mit einer kompakten
    /// Struktur-Vorschau. Das Format deutet an, dass Pre- und Post-Prompts
    /// aus dem PromptBoard beim Abschicken dazukommen, ohne deren genauen
    /// Inhalt hier zu kennen:
    ///   "[Pre] &lt;Anfang des Textes&gt;… [Post]"
    /// Bei leerem Text wird das Label geleert. Haelt sich an die gleiche
    /// Logik wie PromptChainBuilder.Build(): alwaysOn-Prompts kommen vor
    /// dem Benutzer-Text, Post-Prompts danach.
    /// </summary>
    private void InputBox_TextChanged(object sender, System.Windows.Controls.TextChangedEventArgs e)
    {
        var text = InputBox.Text ?? string.Empty;

        // Datenverlust-Schutz: jeden Stand sofort in den Draft spiegeln, damit ein
        // TVO-Neustart den noch nicht abgeschickten Text nicht verliert. Beim Leeren
        // (nach dem Abschicken) wird hier "" gespeichert -> Draft leert sich automatisch mit.
        SaveDraft(text);

        if (string.IsNullOrWhiteSpace(text))
        {
            // Kein Text — Vorschau leeren, damit kein veralteter Status stehen bleibt.
            // Nur leeren wenn kein Gemini-Status angezeigt wird (der beginnt nie mit "[").
            if (!PreviewLabel.Text.StartsWith("Verbessere", StringComparison.Ordinal) &&
                !PreviewLabel.Text.StartsWith("Gemini", StringComparison.Ordinal))
            {
                PreviewLabel.Text = string.Empty;
            }
            return;
        }

        // Kompakte Vorschau: ersten 60 Zeichen des Textes zeigen.
        const int maxLen = 60;
        string snippet = text.Length > maxLen ? text[..maxLen].TrimEnd() + "…" : text.Replace('\n', ' ');
        PreviewLabel.Text = $"[Pre] {snippet} [Post]";
    }

    // ── Draft-Persistenz (Datenverlust-Schutz beim TVO-Neustart, 2026-06-22) ──
    // Der noch nicht abgeschickte Eingabe-Text wird bei jeder Aenderung sofort in
    // eine kleine Datei gespiegelt und beim Fenster-Start wiederhergestellt. So
    // ueberlebt ein im Eingabefeld stehender Text einen Overlay-Neustart, auch wenn
    // er nicht abgeschickt wurde. Best-Effort: Fehler nie auf den UI-Thread werfen.
    private static string DraftPath =>
        System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "PromptBoard", "input-draft.txt");

    /// <summary>Existiert ein nicht-leerer Draft? (Vom OverlayWindow beim Start genutzt,
    /// um das Eingabefenster automatisch zu oeffnen und den Text sichtbar zu machen.)</summary>
    public static bool HasPendingDraft()
    {
        try
        {
            return System.IO.File.Exists(DraftPath)
                && !string.IsNullOrEmpty(System.IO.File.ReadAllText(DraftPath, System.Text.Encoding.UTF8));
        }
        catch { return false; }
    }

    private void SaveDraft(string text)
    {
        try
        {
            string dir = System.IO.Path.GetDirectoryName(DraftPath)!;
            System.IO.Directory.CreateDirectory(dir);
            // Atomar: temp schreiben, dann ersetzen — nie eine halbe Datei hinterlassen.
            string tmp = DraftPath + ".tmp";
            System.IO.File.WriteAllText(tmp, text ?? string.Empty, System.Text.Encoding.UTF8);
            System.IO.File.Move(tmp, DraftPath, overwrite: true);
        }
        catch { /* Best-Effort — Draft darf den UI-Thread nie blockieren/werfen */ }
    }

    private void RestoreDraft()
    {
        try
        {
            if (!System.IO.File.Exists(DraftPath)) return;
            string saved = System.IO.File.ReadAllText(DraftPath, System.Text.Encoding.UTF8);
            if (!string.IsNullOrEmpty(saved))
            {
                InputBox.Text = saved;
                InputBox.CaretIndex = InputBox.Text.Length;
            }
        }
        catch { /* ignore */ }
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

    /// <summary>Beim Rechtsklick-Down auf einem belegten Slot vorgemerkter Button —
    /// sein Prioritaets-Menue wird beim Loslassen (Up) EXPLIZIT geoeffnet. Der
    /// WPF-Automatismus (btn.ContextMenu oeffnet sich von selbst) feuert auf
    /// diesem Topmost-/Rechtsklick-Drag-Fenster NICHT zuverlaessig — bewiesen per
    /// diag.log: "input window built" da, aber nie "context menu opened".</summary>
    private Button? _pendingPriorityBtn;

    private void OnRightDragStart(object sender, MouseButtonEventArgs e)
    {
        // Rechtsklick auf die TextBox darf NICHT den Drag starten.
        if (e.OriginalSource is System.Windows.Controls.TextBox) return;

        // Rechtsklick auf einen Button (Slot/Diskette/X/Toolbar): KEIN Fenster-Drag.
        // Sitzt der Klick auf einem BELEGTEN Zahlen-Slot, Button vormerken und das
        // Prioritaets-Menue beim Loslassen (OnRightDragEnd) selbst oeffnen.
        if (e.OriginalSource is DependencyObject src && FindAncestorButton(src) is Button hitBtn)
        {
            int? slotNo = hitBtn.Tag as int?;
            bool occupied = slotNo is int sn
                && _slotContents.TryGetValue(sn, out var stxt) && !string.IsNullOrEmpty(stxt);
            _pendingPriorityBtn = occupied ? hitBtn : null;
            DiagLog.Write("SlotPriority", "right-click on button",
                ("slot", slotNo?.ToString() ?? "non-slot"), ("occupied", occupied.ToString()));
            e.Handled = true;   // verhindert Fenster-Drag UND das unzuverlaessige Auto-Menue
            return;
        }

        _pendingPriorityBtn = null;
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
        // Vorgemerkter belegter Slot -> Prioritaets-Menue JETZT explizit oeffnen
        // (Standard-Timing auf dem Up). cm.Opened schiebt das Popup auf
        // HWND_TOPMOST + loggt "context menu opened".
        if (_pendingPriorityBtn is Button pb)
        {
            _pendingPriorityBtn = null;
            if (pb.Tag is int n)
            {
                _selectedSlot = n;
                UpdateSlotVisuals();
            }
            if (pb.ContextMenu is ContextMenu cm)
            {
                cm.PlacementTarget = pb;
                cm.Placement = System.Windows.Controls.Primitives.PlacementMode.Bottom;
                cm.IsOpen = true;
            }
            e.Handled = true;
            return;
        }
        if (!_isDragging) return;
        _isDragging = false;
        ReleaseMouseCapture();
    }

    /// <summary>Laeuft vom getroffenen Element die Visual-Tree-Eltern hoch und
    /// liefert den ersten umschliessenden Button (oder null). Damit erkennt der
    /// Rechtsklick-Drag, ob er auf einem Button (Slot/Toolbar) sitzt — dann wird
    /// kein Fenster-Drag gestartet und das Kontextmenue kann sich oeffnen.</summary>
    private static ButtonBase? FindAncestorButton(DependencyObject? d)
    {
        while (d is not null)
        {
            if (d is ButtonBase b) return b;
            d = d is System.Windows.Media.Visual ? System.Windows.Media.VisualTreeHelper.GetParent(d) : null;
        }
        return null;
    }

    // ── Prompt-Zwischenspeicher-Slots (1…15) ─────────────────────────────

    /// <summary>Diskette geklickt: speichere <paramref name="text"/> im Slot
    /// <paramref name="number"/> (1…10). Das PromptBoardPanel persistiert
    /// und stoesst SOFORT den Cloud-Sync an.</summary>
    public event Action<int, string>? SlotSaveRequested;

    /// <summary>X geklickt: loesche Slot <paramref name="number"/> dauerhaft.</summary>
    public event Action<int>? SlotDeleteRequested;

    /// <summary>
    /// Eine frisch per Gemini erzeugte 6-8-Wort-Zusammenfassung steht bereit
    /// (number, fuer-diesen-Text, summary). Das PromptBoardPanel persistiert sie
    /// via <c>PromptSlotService.SetSummaryAsync</c> und stoesst den Cloud-Sync
    /// an, damit der Hover-Tooltip auch ins Drive-Backup wandert.
    /// </summary>
    public event Action<int, string, string>? SlotSummaryRequested;

    /// <summary>
    /// Per Drag&amp;Drop einen gespeicherten Prompt von Slot <c>From</c> auf Slot
    /// <c>To</c> gezogen (Frank-Wunsch 2026-06-08). Ziel leer → verschieben,
    /// Ziel belegt → tauschen. Das PromptBoardPanel ruft daraufhin
    /// <c>PromptSlotService.MoveAsync</c> und stoesst den Cloud-Sync an.
    /// </summary>
    public event Action<int, int>? SlotMoveRequested;

    /// <summary>
    /// Rechtsklick auf einen belegten Slot -> Prioritaet setzen
    /// (0=keine, 1=niedrig, 2=mittel, 3=hoch). Das PromptBoardPanel persistiert
    /// via <c>PromptSlotService.SetPriorityAsync</c> und stoesst SOFORT den
    /// Cloud-Sync an, damit die Prioritaet auch ins Google-Drive-Backup wandert.
    /// </summary>
    public event Action<int, int>? SlotPriorityRequested;

    private int? _selectedSlot;
    private readonly Dictionary<int, string> _slotContents = new();
    /// <summary>Speicher-Zeitstempel pro belegtem Slot — fuer die Anzeige neben dem X.</summary>
    private readonly Dictionary<int, DateTime> _slotTimestamps = new();
    /// <summary>KI-Zusammenfassung (6-8 Woerter) pro belegtem Slot — als Hover-Tooltip.</summary>
    private readonly Dictionary<int, string> _slotSummaries = new();
    /// <summary>Prioritaet (1=niedrig, 2=mittel, 3=hoch) pro belegtem Slot — Quelle fuer die
    /// farbige Hintergrund-Einfaerbung der Zahl. Fehlend/0 = keine Prioritaet.</summary>
    private readonly Dictionary<int, int> _slotPriorities = new();
    /// <summary>Slots, fuer die gerade eine Summary erzeugt wird — verhindert Doppel-Calls.</summary>
    private readonly HashSet<int> _summaryInFlight = new();
    /// <summary>True solange der Backfill bestehender Slots laeuft (sequentiell, gedrosselt).</summary>
    private bool _backfillRunning;
    private readonly Dictionary<int, Button> _slotButtons = new();
    private Button? _slotSaveButton;
    private Button? _slotDeleteButton;

    // ── Drag&Drop der Zahlen-Slots (belegten Prompt auf andere Zahl ziehen) ──
    /// <summary>Eigenes Clipboard-Format, damit nur unsere Slot-Drags akzeptiert werden.</summary>
    private const string SlotDragFormat = "TVO_PromptSlotNumber";
    /// <summary>Mausposition beim Druecken auf einen belegten Slot (Drag-Schwelle).</summary>
    private System.Windows.Point _slotDragStartPoint;
    /// <summary>Slot-Nummer, auf der ein moeglicher Drag „scharf" gemacht wurde (nur belegte).</summary>
    private int? _slotDragArmedNumber;
    /// <summary>True direkt nach einem echten Drag — unterdrueckt den darauf folgenden Klick.</summary>
    private bool _slotDragJustHappened;
    /// <summary>Cyan-Rahmen, der den moeglichen Ablege-Slot beim Drueberziehen markiert.</summary>
    private static readonly Brush SlotDropTarget = new SolidColorBrush(Color.FromRgb(0x33, 0xC4, 0xFF));
    /// <summary>Zeigt „wann gespeichert" fuer den gewaehlten belegten Slot, rechts neben dem X.</summary>
    private TextBlock? _slotTimeLabel;

    private static readonly Brush SlotGold  = new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00));
    private static readonly Brush SlotGrey  = new SolidColorBrush(Color.FromRgb(0x8C, 0x8C, 0x8C));
    private static readonly Brush SlotRed   = new SolidColorBrush(Color.FromRgb(0xFF, 0x44, 0x44));
    private static readonly Brush SlotClear = new SolidColorBrush(Colors.Transparent);
    // Prioritaets-Hintergruende (Rechtsklick auf belegten Slot -> Prioritaet):
    // Hoch=Rot, Mittel=Gelb, Niedrig=Gruen. Die Zahl darauf wird Near-Black
    // (SlotPrioText) — gut lesbar auf allen drei UND klar verschieden vom Grau
    // leerer Slots. SlotDefaultBg = Standard-Hintergrund wie im SlotButton-Style.
    private static readonly Brush SlotPrioHigh   = new SolidColorBrush(Color.FromRgb(0xE5, 0x39, 0x35));
    private static readonly Brush SlotPrioMedium = new SolidColorBrush(Color.FromRgb(0xFB, 0xC0, 0x2D));
    private static readonly Brush SlotPrioLow    = new SolidColorBrush(Color.FromRgb(0x43, 0xA0, 0x47));
    private static readonly Brush SlotPrioText   = new SolidColorBrush(Color.FromRgb(0x1A, 0x1A, 0x1A));
    private static readonly Brush SlotDefaultBg  = new SolidColorBrush(Color.FromRgb(0x2D, 0x2D, 0x2D));

    /// <summary>
    /// Wird vom PromptBoardPanel aufgerufen — uebergibt den aktuellen Stand
    /// der belegten Slots (Nummer → Text). Faerbt die Zahlen-Leiste neu ein
    /// und laesst die aktuelle Auswahl/Diskette unveraendert.
    /// </summary>
    public void SetSlotContents(Dictionary<int, string> map, Dictionary<int, DateTime>? timestamps = null,
        Dictionary<int, string>? summaries = null, Dictionary<int, int>? priorities = null)
    {
        _slotContents.Clear();
        foreach (var kv in map)
        {
            if (!string.IsNullOrEmpty(kv.Value)) _slotContents[kv.Key] = kv.Value;
        }
        _slotTimestamps.Clear();
        if (timestamps != null)
        {
            foreach (var kv in timestamps) _slotTimestamps[kv.Key] = kv.Value;
        }
        _slotSummaries.Clear();
        if (summaries != null)
        {
            foreach (var kv in summaries)
                if (!string.IsNullOrWhiteSpace(kv.Value)) _slotSummaries[kv.Key] = kv.Value;
        }
        _slotPriorities.Clear();
        if (priorities != null)
        {
            foreach (var kv in priorities)
                if (kv.Value != 0) _slotPriorities[kv.Key] = kv.Value;
        }
        UpdateSlotVisuals();
        // Bestehende belegte Slots ohne Zusammenfassung nachtraeglich auffuellen.
        MaybeBackfillSummaries();
    }

    /// <summary>
    /// Baut die untere Leiste: Zahlen 1-30 in zwei Reihen (1-15 oben in
    /// SlotBarRow1, 16-30 unten in SlotBarRow2). Diskette und X liegen EINMAL
    /// in der gemeinsamen Aktionsleiste (SlotActions) rechts daneben und gelten
    /// fuer den gewaehlten Slot egal in welcher Reihe. Sie sind anfangs
    /// versteckt — sie erscheinen erst nach Auswahl einer Zahl (Frank-Wunsch).
    /// </summary>
    /// <summary>Wieviele Slots je Reihe — 15 oben (1-15), 15 unten (16-30).</summary>
    private const int SlotsPerRow = 15;

    private void BuildSlotBar()
    {
        for (int n = 1; n <= PromptSlotService.SlotCount; n++)
        {
            var btn = CreateSlotButton(n.ToString(), SlotGrey,
                $"Slot {n} — Klick speichert/laedt. Belegten Slot auf eine andere Zahl ziehen: " +
                "auf leere Zahl = verschieben, auf belegte Zahl = tauschen.");
            btn.Tag = n;
            btn.Margin = new Thickness(0, 0, 7, 0);
            btn.Click += OnSlotNumberClick;
            // Drag&Drop: belegten Slot „greifen" und auf eine andere Zahl ziehen.
            btn.AllowDrop = true;
            btn.PreviewMouseLeftButtonDown += OnSlotPreviewMouseDown;
            btn.PreviewMouseMove += OnSlotPreviewMouseMove;
            btn.PreviewMouseLeftButtonUp += OnSlotPreviewMouseUp;
            btn.DragOver += OnSlotDragOver;
            btn.DragLeave += OnSlotDragLeave;
            btn.Drop += OnSlotDrop;
            // Rechtsklick auf einen belegten Slot -> Prioritaets-Kontextmenue
            // (Hoch/Mittel/Niedrig/Keine). Bei leerem Slot wird das Oeffnen
            // unterdrueckt (Prioritaet ohne Prompt ergibt keinen Sinn).
            btn.ContextMenu = BuildPriorityMenu(n);
            btn.ContextMenuOpening += (_, ev) =>
            {
                if (!(_slotContents.TryGetValue(n, out var cont) && !string.IsNullOrEmpty(cont)))
                    ev.Handled = true;   // leerer Slot -> kein Menue
            };
            _slotButtons[n] = btn;
            // 1-15 in die obere Reihe (SlotBarRow1), 16-30 in die untere (SlotBarRow2).
            var targetRow = n <= SlotsPerRow ? SlotBarRow1 : SlotBarRow2;
            targetRow.Children.Add(btn);
        }

        // Diskette (Segoe-Fluent-Icons "Save" E74E) — gold. In die gemeinsame
        // Aktionsleiste (SlotActions), die fuer beide Reihen gilt.
        _slotSaveButton = CreateSlotButton("", SlotGold,
            "Aktuellen Prompt im gewaehlten Slot dauerhaft speichern.");
        _slotSaveButton.FontFamily = new FontFamily("Segoe Fluent Icons, Segoe MDL2 Assets");
        _slotSaveButton.Margin = new Thickness(0, 0, 4, 0);
        _slotSaveButton.Visibility = Visibility.Collapsed;
        _slotSaveButton.Click += OnSlotSaveClick;
        SlotActions.Children.Add(_slotSaveButton);

        // X — rot.
        _slotDeleteButton = CreateSlotButton("✕", SlotRed,
            "Prompt im gewaehlten Slot dauerhaft loeschen.");
        _slotDeleteButton.FontWeight = FontWeights.Bold;
        _slotDeleteButton.Visibility = Visibility.Collapsed;
        _slotDeleteButton.Click += OnSlotDeleteClick;
        SlotActions.Children.Add(_slotDeleteButton);

        // Zeitstempel-Label rechts neben dem X — zeigt wann der Prompt im
        // gewaehlten Slot gespeichert wurde. Anfangs versteckt.
        _slotTimeLabel = new TextBlock
        {
            Foreground = SlotGrey,
            FontSize = 11,
            VerticalAlignment = VerticalAlignment.Center,
            Margin = new Thickness(4, 0, 0, 0),
            Visibility = Visibility.Collapsed,
        };
        SlotActions.Children.Add(_slotTimeLabel);

        UpdateSlotVisuals();
    }

    private Button CreateSlotButton(string content, Brush foreground, string tooltip)
    {
        return new Button
        {
            Content = content,
            Foreground = foreground,
            Style = (Style)FindResource("SlotButton"),
            ToolTip = tooltip,
        };
    }

    // ── Prioritaet (Rechtsklick-Kontextmenue) ───────────────────────────────

    /// <summary>Baut das Rechtsklick-Menue eines Slots: Hoch/Mittel/Niedrig + Keine.
    /// Jeder Eintrag traegt ein farbiges Quadrat. Beim Oeffnen wird das Popup auf
    /// HWND_TOPMOST gezwungen (sonst liegt es hinter dem Topmost-Overlay).</summary>
    private ContextMenu BuildPriorityMenu(int n)
    {
        var cm = new ContextMenu();
        cm.Items.Add(PriorityMenuItem(n, 3, "Hoch", SlotPrioHigh));
        cm.Items.Add(PriorityMenuItem(n, 2, "Mittel", SlotPrioMedium));
        cm.Items.Add(PriorityMenuItem(n, 1, "Niedrig", SlotPrioLow));
        cm.Items.Add(new Separator());
        cm.Items.Add(PriorityMenuItem(n, 0, "Keine", SlotGrey));
        cm.Opened += (_, _) =>
        {
            ForceToolTipTopmost(cm);
            DiagLog.Write("SlotPriority", "context menu opened", ("slot", n.ToString()));
        };
        return cm;
    }

    private MenuItem PriorityMenuItem(int n, int level, string label, Brush swatch)
    {
        var item = new MenuItem
        {
            Header = label,
            Icon = new System.Windows.Shapes.Rectangle
            {
                Width = 12,
                Height = 12,
                RadiusX = 3,
                RadiusY = 3,
                Fill = swatch,
            },
        };
        item.Click += (_, _) => OnSetPriority(n, level);
        return item;
    }

    /// <summary>Setzt die Prioritaet eines belegten Slots, faerbt sofort lokal um
    /// und meldet die Aenderung zur Persistenz + Cloud-/Drive-Sync.</summary>
    private void OnSetPriority(int n, int priority)
    {
        if (!(_slotContents.TryGetValue(n, out var t) && !string.IsNullOrEmpty(t)))
            return; // nur belegte Slots bekommen eine Prioritaet
        if (priority <= 0) { priority = 0; _slotPriorities.Remove(n); }
        else _slotPriorities[n] = priority;
        UpdateSlotVisuals();
        UpdatePreview(priority == 0
            ? $"Prioritaet von Slot {n} entfernt."
            : $"Slot {n}: Prioritaet {PriorityName(priority)}.");
        DiagLog.Write("SlotPriority", "priority set", ("slot", n.ToString()), ("priority", priority.ToString()));
        SlotPriorityRequested?.Invoke(n, priority);
    }

    private static string PriorityName(int p) => p switch
    {
        3 => "hoch",
        2 => "mittel",
        1 => "niedrig",
        _ => "keine",
    };

    private static Brush PriorityBrush(int p) => p switch
    {
        3 => SlotPrioHigh,
        2 => SlotPrioMedium,
        1 => SlotPrioLow,
        _ => SlotDefaultBg,
    };

    /// <summary>
    /// Faerbt die Zahlen-Leiste neu: belegte Zahlen gold, leere grau, die
    /// ausgewaehlte Zahl bekommt einen goldenen Rahmen. Diskette/X sind nur
    /// sichtbar wenn eine Zahl ausgewaehlt ist; X ist nur aktiv wenn der
    /// Slot wirklich Inhalt hat.
    /// </summary>
    private void UpdateSlotVisuals()
    {
        foreach (var kv in _slotButtons)
        {
            int n = kv.Key;
            Button btn = kv.Value;
            bool hasContent = _slotContents.TryGetValue(n, out var t) && !string.IsNullOrEmpty(t);
            int prio = hasContent && _slotPriorities.TryGetValue(n, out var p) ? p : 0;
            if (prio != 0)
            {
                // Belegt MIT Prioritaet: farbiger Hintergrund (rot/gelb/gruen) +
                // Near-Black-Zahl (auf allen drei lesbar, klar verschieden vom Grau).
                btn.Background = PriorityBrush(prio);
                btn.Foreground = SlotPrioText;
            }
            else
            {
                // Belegt ohne Prioritaet: wie bisher (Gold-Zahl auf dunkel);
                // leerer Slot: graue Zahl auf dunkel.
                btn.Background = SlotDefaultBg;
                btn.Foreground = hasContent ? SlotGold : SlotGrey;
            }
            bool selected = _selectedSlot == n;
            btn.BorderBrush = selected ? SlotGold : SlotClear;
            btn.BorderThickness = new Thickness(selected ? 2 : 0);
            // Hover-Tooltip: bei belegtem Slot die KI-Zusammenfassung (falls
            // vorhanden), sonst der Standard-Hinweis.
            btn.ToolTip = SlotTooltip(n, hasContent);
        }

        bool hasSelection = _selectedSlot.HasValue;
        if (_slotSaveButton is not null)
            _slotSaveButton.Visibility = hasSelection ? Visibility.Visible : Visibility.Collapsed;
        if (_slotDeleteButton is not null)
        {
            _slotDeleteButton.Visibility = hasSelection ? Visibility.Visible : Visibility.Collapsed;
            // X immer aktiv wenn eine Zahl gewaehlt ist (der Store ist die
            // Wahrheit, nicht der lokale Cache) — Loeschen+Sync laeuft immer.
            _slotDeleteButton.IsEnabled = hasSelection;
        }

        // Zeitstempel rechts neben dem X — nur wenn der gewaehlte Slot belegt
        // ist und ein Speicher-Zeitpunkt bekannt ist.
        if (_slotTimeLabel is not null)
        {
            if (_selectedSlot is int sel
                && _slotTimestamps.TryGetValue(sel, out var ts)
                && _slotContents.TryGetValue(sel, out var tx) && !string.IsNullOrEmpty(tx))
            {
                _slotTimeLabel.Text = "🕒 " + ts.ToLocalTime().ToString("dd.MM. HH:mm");
                _slotTimeLabel.Visibility = Visibility.Visible;
            }
            else
            {
                _slotTimeLabel.Text = string.Empty;
                _slotTimeLabel.Visibility = Visibility.Collapsed;
            }
        }
    }

    /// <summary>
    /// Liefert den Hover-Tooltip eines Slot-Buttons: bei belegtem Slot mit
    /// vorhandener KI-Zusammenfassung die 6-8-Wort-Summary, sonst den
    /// Standard-Hinweis (speichern/laden/ziehen).
    /// </summary>
    private string? SlotTooltip(int n, bool hasContent)
    {
        // Nur belegte Slots MIT vorhandener Zusammenfassung zeigen ein Tooltip-
        // Fenster. Leere Slots (und noch nicht zusammengefasste) zeigen KEINS
        // (Frank-Wunsch 2026-06-11) — null => WPF blendet gar nichts ein.
        if (hasContent && _slotSummaries.TryGetValue(n, out var s) && !string.IsNullOrWhiteSpace(s))
            return s;
        return null;
    }

    /// <summary>
    /// Opened-Handler des Tooltip-Styles (XAML EventSetter). Schiebt das native
    /// Popup-HWND des Tooltips auf HWND_TOPMOST. Hintergrund: dieses Fenster
    /// laeuft mit Topmost="True"/ShowActivated="False" — der Tooltip-Popup erbt
    /// das nicht zuverlaessig und erscheint sonst HINTER dem Overlay (Frank-Bug
    /// 2026-06-11: "Tooltip springt in den Hintergrund"). Gleiche Loesung wie
    /// ForcePopupTopmost bei den Kontextmenues im PromptBoardPanel.
    /// </summary>
    /// <summary>Aktuell offener Slot-Tooltip — fuer den koordinierten Topmost-Reassert.</summary>
    private ToolTip? _openToolTip;

    private void SlotToolTip_Opened(object sender, RoutedEventArgs e)
    {
        _openToolTip = sender as ToolTip;
        ForceToolTipTopmost(_openToolTip);
    }

    private void SlotToolTip_Closed(object sender, RoutedEventArgs e)
    {
        if (ReferenceEquals(sender, _openToolTip)) _openToolTip = null;
    }

    /// <summary>
    /// Schiebt einen aktuell offenen Slot-Tooltip erneut auf HWND_TOPMOST. Wird
    /// vom Topmost-Reassert des OverlayWindows aufgerufen, NACHDEM dieses das
    /// Eingabefenster nach ganz oben gekickt hat — sonst verschwindet der
    /// Tooltip nach ~1 Reassert-Tick wieder hinter den Buttons (Frank-Bug
    /// 2026-06-11: "nach einer Sekunde geht er in den Hintergrund").
    /// </summary>
    public void ReassertOpenToolTipTopmost()
    {
        if (_openToolTip is { IsOpen: true } tt) ForceToolTipTopmost(tt);
    }

    private static void ForceToolTipTopmost(System.Windows.Media.Visual? popupVisual)
    {
        if (popupVisual is null) return;
        if (PresentationSource.FromVisual(popupVisual) is not System.Windows.Interop.HwndSource hwndSource) return;
        if (hwndSource.Handle == IntPtr.Zero) return;
        NativeMethods.Win32.SetWindowPos(
            hwndSource.Handle,
            NativeMethods.Win32.HWND_TOPMOST,
            0, 0, 0, 0,
            NativeMethods.Win32.SWP_NOMOVE | NativeMethods.Win32.SWP_NOSIZE | NativeMethods.Win32.SWP_NOACTIVATE);
    }

    /// <summary>
    /// Klick auf eine Zahl: auswaehlen, Diskette/X einblenden. Hat der Slot
    /// bereits einen gespeicherten Prompt, wird er sofort ins Eingabefeld
    /// geladen (Zwischenspeicher abrufen — Frank-Wunsch). Leere Slots lassen
    /// den getippten Text stehen, damit der Benutzer ihn dort ablegen kann.
    /// </summary>
    private void OnSlotNumberClick(object sender, RoutedEventArgs e)
    {
        // Wurde gerade per Drag&Drop ein Slot verschoben, feuert WPF danach noch
        // den Klick auf den Quell-Button — den ignorieren, sonst wuerde der Slot
        // ausgewaehlt/geladen statt verschoben.
        if (_slotDragJustHappened) { _slotDragJustHappened = false; return; }
        if (sender is not Button btn || btn.Tag is not int n) return;
        if (n < 1 || n > PromptSlotService.SlotCount) return;
        _selectedSlot = n;
        if (_slotContents.TryGetValue(n, out var text) && !string.IsNullOrEmpty(text))
        {
            // Belegter Slot → gespeicherten Prompt abrufen (bewusste Lade-Aktion).
            SetText(text);
        }
        // Leerer Slot → das Eingabefeld NIE anfassen. Der frisch getippte Prompt
        // muss stehen bleiben, denn genau ihn will der Benutzer jetzt in diesem
        // Slot speichern. Frueher stand hier SetText(string.Empty) — das loeschte
        // den getippten Prompt im Moment des Zahl-Klicks, BEVOR die Diskette
        // gedrueckt werden konnte (Frank-Datenverlust 2026-06-06). NIEMALS leeren.
        UpdateSlotVisuals();
    }

    /// <summary>
    /// Diskette: speichert den aktuellen Eingabe-Text im gewaehlten Slot.
    /// Leerer Text wird nicht gespeichert (kurzer Hinweis in der Vorschau).
    /// </summary>
    private void OnSlotSaveClick(object sender, RoutedEventArgs e)
    {
        if (_selectedSlot is not int n) return;
        var text = InputBox.Text ?? string.Empty;
        if (string.IsNullOrWhiteSpace(text))
        {
            UpdatePreview("Kein Text zum Speichern im Slot.");
            return;
        }
        _slotContents[n] = text;
        _slotTimestamps[n] = DateTime.UtcNow;
        _slotSummaries.Remove(n);   // alte Summary passt nicht mehr zum neuen Text
        UpdateSlotVisuals();
        UpdatePreview($"In Slot {n} gespeichert.");
        SlotSaveRequested?.Invoke(n, text);
        // Frische 6-8-Wort-Zusammenfassung asynchron per Gemini holen
        // (best-effort, blockiert das Speichern nicht). Fire-and-forget mit
        // eigenem try/catch in der Methode — kein unbehandeltes async void.
        _ = RequestSlotSummaryAsync(n, text);
    }

    /// <summary>
    /// Holt asynchron eine 6-8-Wort-Zusammenfassung fuer den gerade
    /// gespeicherten Slot von Gemini, zeigt sie als Hover-Tooltip an und
    /// meldet sie via <see cref="SlotSummaryRequested"/> zur Persistenz +
    /// Cloud-Sync. Best-effort: kein Gemini-Key oder Fehler -> der Slot bleibt
    /// voll nutzbar, der Tooltip faellt auf den Standardtext zurueck. Wird nur
    /// angewandt, wenn der Slot noch exakt diesen Text haelt (der Benutzer
    /// koennte den Slot in der Zwischenzeit ueberschrieben haben — dann waere
    /// die Summary veraltet).
    /// </summary>
    private async Task RequestSlotSummaryAsync(int n, string text)
    {
        var gemini = VoiceServiceProvider.Gemini;
        if (gemini is null) return;
        try
        {
            var summary = await gemini.GenerateSlotSummaryAsync(text);
            if (string.IsNullOrWhiteSpace(summary)) return;
            if (!(_slotContents.TryGetValue(n, out var cur) && string.Equals(cur, text, StringComparison.Ordinal)))
                return; // Slot inzwischen geaendert — Summary verwerfen
            _slotSummaries[n] = summary;
            if (_slotButtons.TryGetValue(n, out var btn)) btn.ToolTip = summary;
            SlotSummaryRequested?.Invoke(n, text, summary);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Slot summary failed: {ex.Message}");
        }
    }

    /// <summary>
    /// Stoesst den Backfill an: erzeugt fuer alle belegten Slots OHNE
    /// Zusammenfassung nachtraeglich eine — auch fuer Prompts, die vor diesem
    /// Feature gespeichert wurden (Frank-Wunsch 2026-06-11). Laeuft nur einmal
    /// gleichzeitig; ohne Gemini-Key passiert nichts.
    /// </summary>
    private void MaybeBackfillSummaries()
    {
        if (_backfillRunning) return;
        if (VoiceServiceProvider.Gemini is null) return;
        var todo = _slotContents.Keys
            .Where(n => !_slotSummaries.ContainsKey(n) && !_summaryInFlight.Contains(n))
            .OrderBy(n => n)
            .ToList();
        if (todo.Count == 0) return;
        _ = BackfillSummariesAsync(todo);
    }

    /// <summary>
    /// Arbeitet die Backfill-Liste SEQUENTIELL ab (eine Summary nach der
    /// anderen, mit kleiner Pause) — sanfte Drosselung gegen Gemini-Rate-Limits.
    /// Nutzt denselben Pfad wie ein frisches Speichern (RequestSlotSummaryAsync):
    /// Summary erzeugen, Tooltip setzen, persistieren + Cloud-Sync.
    /// </summary>
    private async Task BackfillSummariesAsync(List<int> slots)
    {
        _backfillRunning = true;
        try
        {
            foreach (var n in slots)
            {
                if (!_slotContents.TryGetValue(n, out var text) || string.IsNullOrWhiteSpace(text)) continue;
                if (_slotSummaries.ContainsKey(n) || _summaryInFlight.Contains(n)) continue;
                _summaryInFlight.Add(n);
                try { await RequestSlotSummaryAsync(n, text); }
                finally { _summaryInFlight.Remove(n); }
                await Task.Delay(600); // Drosselung: nicht alle Slots gleichzeitig an Gemini
            }
        }
        finally { _backfillRunning = false; }
    }

    /// <summary>
    /// X: loescht den gewaehlten Slot dauerhaft. Laeuft IMMER durch wenn eine
    /// Zahl gewaehlt ist (nicht mehr vom lokalen Cache abhaengig — Frank-Bug
    /// 2026-06-05: X reagierte nicht, weil der Button bei vermeintlich leerem
    /// Slot deaktiviert war). Tombstone + Sofort-Sync verhindern ein Restore.
    /// </summary>
    private void OnSlotDeleteClick(object sender, RoutedEventArgs e)
    {
        if (_selectedSlot is not int n) return;
        _slotContents.Remove(n);
        _slotTimestamps.Remove(n);
        _slotSummaries.Remove(n);
        _slotPriorities.Remove(n);
        ClearInput();
        UpdateSlotVisuals();
        UpdatePreview($"Slot {n} geloescht.");
        SlotDeleteRequested?.Invoke(n);
    }

    // ── Drag&Drop der Zahlen-Slots ──────────────────────────────────────────

    /// <summary>Maus auf einem Slot gedrueckt: nur BELEGTE Slots „scharf" machen
    /// (ein leerer Slot hat nichts zu ziehen). Merkt den Startpunkt fuer die
    /// Drag-Schwelle. Verhindert den normalen Klick NICHT (kein e.Handled).</summary>
    private void OnSlotPreviewMouseDown(object sender, MouseButtonEventArgs e)
    {
        if (sender is not Button btn || btn.Tag is not int n) return;
        bool hasContent = _slotContents.TryGetValue(n, out var t) && !string.IsNullOrEmpty(t);
        if (!hasContent) { _slotDragArmedNumber = null; return; }
        _slotDragArmedNumber = n;
        _slotDragStartPoint = e.GetPosition(null);
    }

    /// <summary>Taste losgelassen ohne Drag: Scharfstellung verwerfen (normaler Klick folgt).</summary>
    private void OnSlotPreviewMouseUp(object sender, MouseButtonEventArgs e)
        => _slotDragArmedNumber = null;

    /// <summary>Maus mit gedrueckter Taste ueber die Schwelle bewegt → modaler Drag
    /// startet. DoDragDrop laeuft nur auf dem UI-Thread (Bug-Almanach §14.8); die
    /// eigentliche Persistenz passiert danach async ueber das Move-Event.</summary>
    private void OnSlotPreviewMouseMove(object sender, MouseEventArgs e)
    {
        if (_slotDragArmedNumber is not int from) return;
        if (e.LeftButton != MouseButtonState.Pressed) { _slotDragArmedNumber = null; return; }

        var pos = e.GetPosition(null);
        if (Math.Abs(pos.X - _slotDragStartPoint.X) < SystemParameters.MinimumHorizontalDragDistance &&
            Math.Abs(pos.Y - _slotDragStartPoint.Y) < SystemParameters.MinimumVerticalDragDistance)
            return;

        _slotDragArmedNumber = null;
        _slotDragJustHappened = true; // den nachlaufenden Klick auf den Quell-Button schlucken
        try
        {
            var data = new DataObject(SlotDragFormat, from);
            DragDrop.DoDragDrop((DependencyObject)sender, data, DragDropEffects.Move);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Slot drag failed: {ex.Message}");
        }
        finally
        {
            // Flag mit Background-Prioritaet zuruecksetzen: laeuft NACH einem evtl.
            // nachlaufenden Klick (Input-Prioritaet), aber auch dann, wenn gar kein
            // Klick kommt — so bleibt nie ein Folge-Klick faelschlich blockiert.
            Dispatcher.BeginInvoke(new Action(() => _slotDragJustHappened = false),
                System.Windows.Threading.DispatcherPriority.Background);
        }
    }

    /// <summary>Waehrend des Ziehens: markiert den moeglichen Ziel-Slot cyan und
    /// signalisiert, ob hier abgelegt werden darf.</summary>
    private void OnSlotDragOver(object sender, DragEventArgs e)
    {
        if (sender is not Button btn || btn.Tag is not int to ||
            !e.Data.GetDataPresent(SlotDragFormat))
        {
            e.Effects = DragDropEffects.None;
            e.Handled = true;
            return;
        }
        int from = (int)e.Data.GetData(SlotDragFormat)!;
        if (from == to)
        {
            e.Effects = DragDropEffects.None;
        }
        else
        {
            e.Effects = DragDropEffects.Move;
            btn.BorderBrush = SlotDropTarget;
            btn.BorderThickness = new Thickness(2);
        }
        e.Handled = true;
    }

    /// <summary>Cursor verlaesst den Slot: Drop-Markierung zuruecksetzen
    /// (UpdateSlotVisuals stellt Auswahl-/Belegt-Rahmen wieder her).</summary>
    private void OnSlotDragLeave(object sender, DragEventArgs e) => UpdateSlotVisuals();

    /// <summary>Auf einem Slot abgelegt: lokal verschieben (Ziel leer) bzw. tauschen
    /// (Ziel belegt), Anzeige auffrischen und das Move-Event feuern — das
    /// PromptBoardPanel persistiert via PromptSlotService.MoveAsync + Cloud-Sync.</summary>
    private void OnSlotDrop(object sender, DragEventArgs e)
    {
        UpdateSlotVisuals(); // Drop-Markierung entfernen
        if (sender is not Button btn || btn.Tag is not int to ||
            !e.Data.GetDataPresent(SlotDragFormat)) return;
        int from = (int)e.Data.GetData(SlotDragFormat)!;
        if (from == to) return;
        if (!(_slotContents.TryGetValue(from, out var fromText) && !string.IsNullOrEmpty(fromText)))
            return; // Quelle unerwartet leer

        bool targetOccupied = _slotContents.TryGetValue(to, out var toText) && !string.IsNullOrEmpty(toText);

        // Lokalen Cache spiegeln zu PromptSlotService.MoveAsync: Ziel bekommt den
        // gezogenen Text, Quelle den alten Ziel-Text (leer = verschieben, belegt = tauschen).
        // Summaries vor dem Ueberschreiben sichern, damit der Hover-Tooltip mit
        // dem Prompt mitwandert (deckungsgleich zu PromptSlotService.MoveAsync).
        _slotSummaries.TryGetValue(from, out var fromSummary);
        _slotSummaries.TryGetValue(to, out var toSummary);
        // Prioritaet (rot/gelb/gruen) gehoert — wie die Summary — zum PROMPT und
        // muss mit ihm wandern. Fehlt das, bleibt die farbige Einfaerbung am alten
        // Slot haengen (Bug 2026-06-16). 0 = keine Prioritaet (out-Default bei Miss).
        // Deckungsgleich zu PromptSlotService.MoveAsync (fromPriority/toPriority).
        _slotPriorities.TryGetValue(from, out var fromPriority);
        _slotPriorities.TryGetValue(to, out var toPriority);

        var now = DateTime.UtcNow;
        _slotContents[to] = fromText;
        _slotTimestamps[to] = now;
        if (!string.IsNullOrWhiteSpace(fromSummary)) _slotSummaries[to] = fromSummary; else _slotSummaries.Remove(to);
        if (fromPriority != 0) _slotPriorities[to] = fromPriority; else _slotPriorities.Remove(to);
        if (targetOccupied)
        {
            _slotContents[from] = toText!;
            _slotTimestamps[from] = now;
            if (!string.IsNullOrWhiteSpace(toSummary)) _slotSummaries[from] = toSummary; else _slotSummaries.Remove(from);
            if (toPriority != 0) _slotPriorities[from] = toPriority; else _slotPriorities.Remove(from);
        }
        else
        {
            _slotContents.Remove(from);
            _slotTimestamps.Remove(from);
            _slotSummaries.Remove(from);
            _slotPriorities.Remove(from);   // Quelle wird leer -> keine Prioritaet
        }

        _selectedSlot = to; // Auswahl auf das Ziel ziehen
        UpdateSlotVisuals();
        UpdatePreview(targetOccupied
            ? $"Slot {from} und {to} getauscht."
            : $"Prompt von Slot {from} nach {to} verschoben.");
        SlotMoveRequested?.Invoke(from, to);
    }
}

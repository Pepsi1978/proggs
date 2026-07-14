using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Win32;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using TerminalVoiceOverlay.NativeMethods;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// PromptBoard side panel: 1:1 port of the macOS TerminalVoiceOverlay PromptBoardPanel.swift.
/// Multi-category selection, per-category row tinting, clickable always-on checkbox,
/// whole-row click + right-click, auto-backup with debounce, sync badge.
/// </summary>
public partial class PromptBoardPanel : Window
{
    // ──────────────── Static frozen brushes (hot-path optimization) ────────────────
    // Frueher: jede BuildPromptRow / BuildAlwaysOnCheckbox / RenderEmptyState legte pro
    // Aufruf 1-5 neue, NICHT-eingefrorene SolidColorBrushes an — bei 50 Prompts und
    // einem Re-Render pro Drag/Toggle/Edit/Add/Delete ergaben sich 6×50 = 300 unfrozene
    // Brushes pro Render. Jeder unfrozen Brush bindet sich an WPFs Change-Notification-
    // Pipeline (Cross-Thread-Sicherheit kostet auch wenn sie nicht genutzt wird).
    // Jetzt: einmal beim App-Start eingefrorene statische Brushes; alle Rows teilen
    // sich denselben Brush. Optisch identisch (gleiche RGB-Werte), kein Layout-Effekt.
    private static SolidColorBrush FrozenBrush(byte r, byte g, byte b)
    {
        var brush = new SolidColorBrush(Color.FromRgb(r, g, b));
        brush.Freeze();
        return brush;
    }
    private static readonly SolidColorBrush BrushTimestampGray   = FrozenBrush(0xAA, 0xAA, 0xAA); // BuildPromptRow timestamp
    private static readonly SolidColorBrush BrushDarkFg          = FrozenBrush(0x1F, 0x1F, 0x1F); // Checkbox foreground / Filter active fg
    private static readonly SolidColorBrush BrushGold            = FrozenBrush(0xFF, 0xD7, 0x00); // Always-on / Filter-active / DropTarget border
    private static readonly SolidColorBrush BrushDarkBg          = FrozenBrush(0x2D, 0x2D, 0x2D); // Checkbox bg inactive / Cat tab inactive
    private static readonly SolidColorBrush BrushMutedBorder     = FrozenBrush(0x8C, 0x8C, 0x8C); // Checkbox border inactive
    private static readonly SolidColorBrush BrushEmptyStateFg    = FrozenBrush(0x9A, 0x9A, 0x9A); // RenderEmptyState text
    private static readonly SolidColorBrush BrushHistoryInactive = FrozenBrush(0xCC, 0xCC, 0xCC); // History toggle inactive
    private static readonly SolidColorBrush BrushSuccessGreen    = FrozenBrush(0x4C, 0xAF, 0x50); // ClearAllChecks
    private static readonly SolidColorBrush BrushFilterInactiveBg = FrozenBrush(0x38, 0x38, 0x38); // Filter inactive

    public event Action<string>? PromptInsertRequested;

    /// <summary>
    /// Wird vom Eingabefenster gefeuert wenn der Benutzer dort den Stern-
    /// Button geklickt hat. Argument: gewuenschter Solo-Andock-Zustand
    /// (true = Promptboard ausblenden, Eingabe ans Voice-Overlay andocken;
    /// false = zurueck in den Normalmodus). Das OverlayWindow abonniert
    /// dieses weitergereichte Event und setzt den Layout-Wechsel um.
    /// </summary>
    public event Action<bool>? SoloDockToggleRequested;

    /// <summary>
    /// Erlaubt dem OverlayWindow Zugriff auf das angedockte Eingabefenster,
    /// damit es im Solo-Modus dessen Geometrie direkt am Pillar ausrichten
    /// kann (Hide/Show des Promtboards reicht allein nicht — das Eingabe-
    /// fenster muss seinen neuen Andock-Partner kennen).
    /// </summary>
    public PromptInputWindow? InputWindow => _inputWindow;

    /// <summary>
    /// Fires while the user right-click drags the panel itself. The
    /// OverlayWindow uses this to slide the pillar by the same delta
    /// so both floating windows move together as a single unit.
    /// </summary>
    public event Action? PanelDragged;

    /// <summary>
    /// Wird ausgeloest, wenn der Benutzer im neuen PromptInputWindow Enter
    /// drueckt. Der uebergebene Text ist der reine Inhalt der Eingabe-Box —
    /// das Pre/Mitte/Post-Zusammenbauen passiert im OverlayWindow, das den
    /// AlwaysOnPrefixService bereits kennt.
    /// </summary>
    public event Func<string, Task<bool>>? InputSubmitRequested;
    private bool _inputSubmitInProgress;

    /// <summary>
    /// Das angedockte Prompt-Eingabefenster. Existiert nur solange der Stern
    /// aktiv ist — beim Schliessen wird das Window komplett zerstoert, damit
    /// der naechste Start einen sauberen Zustand bekommt (Inhalt ist nicht
    /// persistent, Position kehrt zur Standard-Andockposition zurueck).
    /// </summary>
    private PromptInputWindow? _inputWindow;

    /// <summary>
    /// Persistenter Eingabetext, der einen Stern-Toggle ueberlebt. Wird beim
    /// Schliessen des Eingabefensters gesetzt und beim erneuten Oeffnen wieder
    /// in die InputBox geschrieben. <c>static</c>, weil beim Stern-OFF die
    /// PromptBoardPanel-Instanz selbst zerstoert wird (OverlayWindow ruft
    /// HidePromptPanel→Close auf) und ein Instance-Field damit nicht
    /// ueberleben wuerde. Nach App-Neustart ist das Feld wieder leer — der
    /// Text persistiert nur fuer die Dauer der laufenden Session.
    /// </summary>
    private static string _persistedInputText = string.Empty;

    /// <summary>
    /// Erstauswahl-Modus: Beim Sichtbarwerden des Panels (Stern-Klick) wird dieser
    /// Flag aktiviert. So lange er gesetzt ist, werden ausschliesslich Prompts mit
    /// IsAlwaysOn=true (kategorienueberhgreifend) angezeigt — die zugehoerigen
    /// Kategorie-Tabs sind visuell markiert. Der erste Tab-Klick beendet den Modus
    /// und schaltet auf das normale Verhalten um (alle Prompts der angeklickten
    /// Kategorie sichtbar, auch ohne Haekchen).
    /// </summary>
    private bool _alwaysOnFilterMode;

    /// <summary>
    /// Stern-Zustand. Beim App-Neustart immer false (laut Spec). Wir spiegeln
    /// das hier, weil der Foreground-Wechsel des Buttons sonst aus dem
    /// Click-Handler-Code abgeleitet werden muesste.
    /// </summary>
    private bool _inputWindowVisible;

    /// <summary>Historie-Fenster + dessen Sichtbarkeitsstatus.</summary>
    private PromptHistoryWindow? _historyWindow;
    private bool _historyWindowVisible;

    /// <summary>
    /// Einzige PromptHistoryService-Instanz im Panel — teilt sich denselben
    /// Pfad UND denselben SemaphoreSlim-Lock wie das OverlayWindow ueber
    /// den process-weiten VoiceServiceProvider. Frueher hatten beide
    /// Klassen jeweils ein new() und damit getrennte Locks; bei sehr
    /// schnellem Submit + Re-Render konnten parallele File.Move-Aufrufe
    /// auf der prompt-history.tmp-Datei kollidieren. Mit einem einzigen
    /// Service ist die Schreib-Lese-Sequenz garantiert serialisiert.
    /// </summary>
    private readonly PromptHistoryService _historyService = VoiceServiceProvider.History;

    // ── Right-click panel drag state ──
    private bool _isDraggingPanel;
    // Cursor-Anker in absoluten Screen-Pixeln (Win32.GetCursorPos), NICHT
    // ueber PointToScreen(e.GetPosition(this)). Der WPF-Pfad ist relativ
    // zum Fenster-Origin — der Origin verschiebt sich aber waehrend des
    // Drags, sodass der Anker laufend zerrissen wuerde und das Fenster
    // dem Cursor mit wachsendem Versatz hinterherlaeuft. Mit absolutem
    // Cursor-Anker (Win32) bleibt der Drag stabil.
    private int _panelDragStartCursorX;
    private int _panelDragStartCursorY;
    private double _panelDragStartLeft;
    private double _panelDragStartTop;
    // DPI-Skalierungs-Faktoren beim Drag-Start. Win32.GetCursorPos liefert
    // raw Geraetepixel; WPFs Window.Left/Top sind in DIPs. Bei 150% oder
    // 200% Skalierung wuerde der Drag ohne Division zu schnell (Faktor 1.5
    // bzw. 2). Erfassung beim Drag-Start damit ein DPI-Wechsel mitten im
    // Drag (Cursor wandert ueber Monitor-Grenze) keine Sprungberechnung
    // verursacht — der Fenster-Origin folgt immer dem Anker-Monitor.
    private double _panelDragDpiX = 1.0;
    private double _panelDragDpiY = 1.0;

    private List<Category> _categories = new();
    /// <summary>
    /// Multiple categories can be active simultaneously. Prompts from every
    /// active category are merged and shown in one combined list, each row
    /// tinted with its category color.
    /// </summary>
    private readonly HashSet<Guid> _activeCategoryIds = new();
    private List<Prompt> _currentPrompts = new();

    /// <summary>Auto-backup debounce window — many quick edits collapse into one upload.</summary>
    private static readonly TimeSpan AutoBackupDelay = TimeSpan.FromSeconds(2);
    private DispatcherTimer? _autoBackupTimer;
    private readonly SemaphoreSlim _autoBackupGate = new(1, 1);

    // ── Drag-and-drop arming state ──
    private System.Windows.Point _dragArmStartPoint;
    private Guid? _dragArmedRowId;
    /// <summary>
    /// Set when DoDragDrop just ran for a given row; the subsequent
    /// MouseLeftButtonUp on the same row must NOT also insert the prompt.
    /// Cleared on the very next click.
    /// </summary>
    private Guid? _dragJustHappenedForRowId;

    /// <summary>
    /// Live drag preview floating under the cursor. Created on drag start,
    /// updated by Window.DragOver, removed on drag end.
    /// </summary>
    private DragGhostAdorner? _dragGhost;
    private AdornerLayer? _dragGhostLayer;
    private Border? _dragSourceRow;

    /// <summary>
    /// Persistent record of the last successful Drive backup. macOS uses
    /// UserDefaults; Windows uses a tiny text file alongside the SQLite
    /// database so it survives app restarts and stays out of the DB schema.
    /// </summary>
    private static readonly string LastSyncFilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "PromptBoard",
        "last-sync.txt");

    /// <summary>
    /// Fixed palette: distinct color per category by index. Deterministic so
    /// colors stay stable across renders and across app restarts.
    /// </summary>
    private static readonly Color[] CategoryPalette =
    {
        Color.FromRgb(0x4A, 0x8F, 0xFC), // blue
        Color.FromRgb(0xF2, 0x70, 0x42), // orange
        Color.FromRgb(0x66, 0xBA, 0x6B), // green
        Color.FromRgb(0xAB, 0x47, 0xBC), // purple
        Color.FromRgb(0xF2, 0xA6, 0x26), // amber
        Color.FromRgb(0x26, 0xB7, 0xD1), // cyan
        Color.FromRgb(0xED, 0x4D, 0x85), // pink
        Color.FromRgb(0x78, 0x8F, 0x9C), // blue-grey
    };

    /// <summary>Hex-Strings der Palette-Farben — dient als Quick-Lookup ob
    /// ein BackgroundColorHex bereits eine "echte" Palette-Farbe ist.</summary>
    private static readonly string[] CategoryPaletteHex = CategoryPalette
        .Select(c => $"#{c.R:X2}{c.G:X2}{c.B:X2}")
        .ToArray();

    public PromptBoardPanel()
    {
        InitializeComponent();
        BtnAddCategory.Click     += async (_, _) => await AddCategoryAsync();
        BtnAddPrompt.Click       += async (_, _) => await AddPromptAsync();
        BtnSettings.Click        += async (_, _) => await ShowSettingsAsync();
        BtnBackup.Click          += async (_, _) => await ShowBackupMenuAsync();
        BtnHistory.Click         += (_, _) => ToggleHistoryWindow();
        BtnClearAllChecks.Click  += async (_, _) => await ClearAllAlwaysOnAsync();
        BtnFilterActiveOnly.Click += async (_, _) => await ToggleAlwaysOnFilterAsync();
        UpdateFilterButtonVisual();
        RefreshSyncLabel();

        // Datenverlust-Schutz (2026-06-22): Steht aus der letzten Sitzung noch ein
        // nicht abgeschickter Eingabe-Text im Draft (z.B. weil das TVO neu gestartet
        // wurde, waehrend Text im Eingabefeld stand), oeffnen wir das Eingabefenster
        // beim Start automatisch — sein Konstruktor stellt den Text dann wieder her,
        // sodass er sichtbar bleibt. Greift nur bei vorhandenem Draft, sonst NoOp.
        Loaded += (_, _) =>
        {
            try
            {
                if (PromptInputWindow.HasPendingDraft() && !_inputWindowVisible)
                    OpenInputWindow();
            }
            catch { /* nie den Start blockieren */ }
        };

        // Bei jedem Sichtbarwerden des Panels (Stern-Klick im Voice-Overlay)
        // die aktive Kategorienauswahl resetten, damit der Benutzer immer
        // mit einer leeren Auswahl startet und bewusst eine Kategorie
        // anklickt. Das Panel wird beim Schliessen nur ausgeblendet (siehe
        // OverlayWindow.HidePromptPanel), die Instanz lebt also weiter und
        // wuerde sonst die letzte Auswahl wiederzeigen.
        IsVisibleChanged += (_, e) =>
        {
            if (e.NewValue is bool isVisible && isVisible)
            {
                // Erstauswahl-Modus aktivieren: Beim Sichtbarwerden des Panels
                // (Sternchen in der Eingabe-Toolbar oder direkter Show-Aufruf)
                // werden nur Always-On-Prompts angezeigt. Die zugehoerigen
                // Kategorien werden in RefreshAsync automatisch in
                // _activeCategoryIds eingetragen, damit die Tabs visuell
                // als aktiv markiert sind.
                _alwaysOnFilterMode = true;
                _activeCategoryIds.Clear();
                PromptList.Children.Clear();
                RenderEmptyState("Lade aktive Prompts...");
                RenderCategoryTabs();
                UpdateFilterButtonVisual();
                // RefreshAsync hier explizit anstossen. Frueher uebernahm das
                // OverlayWindow das nach Show(); im neuen Solo-Andock-Flow
                // ruft ApplySoloDockMode(false) jedoch nur _promptPanel.Show()
                // ohne Refresh auf — _activeCategoryIds bliebe sonst leer und
                // der Filter-Modus haette nichts anzuzeigen.
                _ = RefreshAsync();
            }
        };
        // Beim Schliessen das Eingabefenster mitnehmen, sonst bleibt ein
        // verwaistes Fenster ohne Trigger zurueck.
        Closed += (_, _) => { CloseInputWindow(); CloseHistoryWindow(); };

        // Window-wide drop tracking so the floating drag preview updates
        // its position over the entire panel area — not just over the
        // category tabs that accept the drop.
        AllowDrop = true;
        PreviewDragOver += (_, e) =>
        {
            if (_dragGhost is null) return;
            _dragGhost.UpdateLocation(e.GetPosition(this));
        };

        // Right-click drag on the panel background moves both this
        // panel AND the pillar (via the PanelDragged event handled in
        // OverlayWindow). The handlers are wired with PreviewMouseRight*
        // so they run before child controls — but child controls that
        // already do something with right-click (e.g. the existing
        // contextual edit on prompt rows) can still handle their event;
        // we only arm a drag if no child marked it Handled.
        PreviewMouseRightButtonDown += OnPanelRightDown;
        PreviewMouseMove            += OnPanelMouseMove;
        PreviewMouseRightButtonUp   += OnPanelRightUp;

        // Andock-Garantie: Egal WER das Promtboard bewegt oder seine Hoehe
        // aendert (eigenes Drag, OverlayWindow.PositionPromptPanel beim
        // Pillar-Drag, Star-Toggle, Tab-Wechsel) — die angedockten Kinder
        // (Eingabe + Historie) muessen IMMER 1:1 mitziehen. Die explizite
        // FollowPanelDrag in OnPanelMouseMove und OnChildGroupDrag bleibt
        // bestehen; diese Hooks sind die Sicherheitsschicht fuer alle
        // anderen Wege wie Position/Groesse veraendert werden.
        LocationChanged += (_, _) => RefollowChildren();
        SizeChanged     += (_, _) => RefollowChildren();
    }

    private void RefollowChildren()
    {
        _inputWindow?.FollowPanelDrag(this);
        _historyWindow?.FollowPanelDrag(this);
    }

    private void OnPanelRightDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        // Hit-test: if the click is on a prompt row's interactive
        // surface (the row itself opens the editor on right-click),
        // let that handler win.
        var hit = e.OriginalSource as DependencyObject;
        while (hit is not null && hit != this)
        {
            if (hit is Border b && b.Tag is Prompt) return;     // prompt row
            if (hit is System.Windows.Controls.Button) return;  // any tab/button
            hit = System.Windows.Media.VisualTreeHelper.GetParent(hit);
        }

        // Anker in absoluten Screen-Pixeln nehmen — siehe Kommentar bei
        // _panelDragStartCursorX. Wenn GetCursorPos fehlschlaegt (sehr
        // selten, z.B. UAC-Wechsel mid-click), brechen wir den Drag
        // sauber ab statt mit ungueltigen Werten weiterzumachen.
        if (!Win32.GetCursorPos(out var startPt)) return;

        _isDraggingPanel       = true;
        _panelDragStartCursorX = startPt.X;
        _panelDragStartCursorY = startPt.Y;
        _panelDragStartLeft    = Left;
        _panelDragStartTop     = Top;

        // DPI-Skalierung des aktuellen Fensters einmalig erfassen.
        // CompositionTarget kann theoretisch null sein bevor das Fenster
        // gerendert wurde — Defensiv-Default 1.0 verhindert NaN-Werte
        // im Drag. Dass sich der Faktor mid-Drag aendert (Cursor ueber
        // Monitor-Grenze) ist OK: das Window folgt dem Anker-Monitor.
        var src = PresentationSource.FromVisual(this);
        _panelDragDpiX = src?.CompositionTarget?.TransformToDevice.M11 ?? 1.0;
        _panelDragDpiY = src?.CompositionTarget?.TransformToDevice.M22 ?? 1.0;
        if (_panelDragDpiX == 0) _panelDragDpiX = 1.0;
        if (_panelDragDpiY == 0) _panelDragDpiY = 1.0;

        CaptureMouse();
        e.Handled = true;
    }

    private void OnPanelMouseMove(object sender, System.Windows.Input.MouseEventArgs e)
    {
        if (!_isDraggingPanel) return;
        if (!Win32.GetCursorPos(out var cur)) return;

        // Delta in Geraetepixeln durch DPI teilen — Result in DIPs, was
        // Window.Left/Top erwartet. Gleicher Algorithmus wie im
        // OverlayWindow.WndProc / WM_MOUSEMOVE.
        Left = _panelDragStartLeft + (cur.X - _panelDragStartCursorX) / _panelDragDpiX;
        Top  = _panelDragStartTop  + (cur.Y - _panelDragStartCursorY) / _panelDragDpiY;

        PanelDragged?.Invoke();
        _inputWindow?.FollowPanelDrag(this);
        _historyWindow?.FollowPanelDrag(this);
    }

    private void OnPanelRightUp(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        if (!_isDraggingPanel) return;
        _isDraggingPanel = false;
        ReleaseMouseCapture();
        PanelDragged?.Invoke();
        _inputWindow?.FollowPanelDrag(this);
        _historyWindow?.FollowPanelDrag(this);
        e.Handled = true;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Prompt-Eingabefenster (oeffnet via OverlayWindow.BtnUltrathink — der
    // frueher hier sitzende Stern-Toggle wurde als redundant entfernt)
    // ─────────────────────────────────────────────────────────────────────

    /// <summary>
    /// Stellt sicher dass das Prompt-Eingabefenster geoeffnet ist. Wird vom
    /// Voice-Overlay nach einem Stern-Klick aufgerufen, damit Promptboard
    /// und Eingabe-Fenster zusammen erscheinen statt einzeln.
    /// </summary>
    public void EnsureInputWindowOpen()
    {
        if (!_inputWindowVisible) OpenInputWindow();
    }

    /// <summary>
    /// Blendet das Eingabefenster aus, sodass im Solo-Andock-Modus NUR noch
    /// das Promtboard sichtbar ist (Frank-Wunsch 2026-06-06: der Stern togglet
    /// zwischen "nur Eingabe" und "nur Board", nie beide zugleich). Symmetrisches
    /// Gegenstueck zu <see cref="EnsureInputWindowOpen"/>. Der noch nicht
    /// abgeschickte Text wird ueber CloseInputWindow in den Session-Puffer
    /// (_persistedInputText) gesichert und vom naechsten EnsureInputWindowOpen
    /// wiederhergestellt — beim Toggle geht nichts verloren.
    /// </summary>
    public void HideInputWindow()
    {
        if (_inputWindowVisible) CloseInputWindow();
    }

    /// <summary>
    /// Klick-Handler fuer den Stern-Button in der Promtboard-Toolbar (neben
    /// dem "!"). Symmetrisches Pendant zum Stern im Eingabefenster: feuert
    /// <see cref="SoloDockToggleRequested"/>(true) — das OverlayWindow stellt
    /// sicher dass das Eingabefenster offen ist, blendet das Promtboard aus
    /// und dockt das Eingabefeld direkt an die linke Pillar-Kante an. Das
    /// Open des Eingabefensters macht jetzt das OverlayWindow (in
    /// ApplySoloDockMode), damit kein visueller Flash zwischen "Eingabe
    /// neben Board" und "Eingabe an Pillar" entsteht.
    /// Der Rueckweg (Eingabefeld zurueck zum Promtboard) laeuft ueber den
    /// Stern in der Eingabe-Toolbar, der genauso das Event feuert — nur mit
    /// active=false.
    /// </summary>
    private void BtnBoardStar_Click(object sender, RoutedEventArgs e)
    {
        SoloDockToggleRequested?.Invoke(true);
    }

    /// <summary>
    /// Klick-Handler fuer den "!"-Button neben dem Sync-Label. Entfernt das
    /// Always-On-Haekchen von ALLEN Prompts in ALLEN Kategorien — nuetzlich
    /// wenn der Benutzer einen frischen Start braucht und nur noch gezielt
    /// Haekchen setzen will. Visual-Feedback: der Button wird fuer 1 Sekunde
    /// gruen, dann wieder normal.
    /// </summary>
    private async Task ClearAllAlwaysOnAsync()
    {
        // Guard against an accidental click: this button clears ALL Always-On
        // checks across every category (except "Allgemein") and is not trivially
        // reversible. Require an explicit confirmation first.
        if (!ConfirmDialog.Ask(this, "Alle Haekchen entfernen?",
                "Alle Always-On-Haekchen in allen Kategorien werden entfernt. Die Kategorie \"Allgemein\" bleibt unberuehrt. Fortfahren?",
                "Entfernen"))
        {
            return;
        }

        // Visuelles Feedback SOFORT setzen, damit der Klick ankommt — auch
        // wenn die DB-Operation ein paar hundert Millisekunden dauert.
        var originalBackground = BtnClearAllChecks.Background;
        BtnClearAllChecks.Background = BrushSuccessGreen;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var categoryRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

            // Nur Name wird gelesen — Summary-Variante ohne Prompts-JOIN.
            var categories = await categoryRepo.GetAllSummaryAsync();
            int cleared = 0;
            foreach (var cat in categories)
            {
                // Kategorie "Allgemein" wird ausgenommen — die Haekchen
                // dort sollen vom Reset-Button NICHT angefasst werden.
                // Case-insensitiv und mit Trim, damit kleine Variationen
                // ("allgemein", " Allgemein ") trotzdem matchen.
                if (string.Equals(cat.Name?.Trim(), "Allgemein",
                        StringComparison.OrdinalIgnoreCase))
                {
                    continue;
                }

                var prompts = await promptRepo.GetByCategoryAsync(cat.Id);
                foreach (var p in prompts)
                {
                    if (p.IsAlwaysOn)
                    {
                        p.IsAlwaysOn = false;
                        await promptRepo.UpdateAsync(p);
                        cleared++;
                    }
                }
            }
            Console.WriteLine($"ClearAllAlwaysOn: {cleared} prompts entmarkiert (Allgemein uebersprungen).");
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"ClearAllAlwaysOn failed: {ex.Message}");
        }

        // RefreshAsync (nicht RenderPromptsAsync) — die DB-Schreibvorgaenge oben
        // haben _allPromptsCache veraltet, RenderPromptsAsync allein wuerde die
        // alten IsAlwaysOn-Flags weiter zeigen. Frank-Bug-Pattern 2026-05-09.
        await RefreshAsync();

        // Eine Sekunde gruen halten, dann zurueck auf den Original-Hintergrund.
        await Task.Delay(1000);
        BtnClearAllChecks.Background = originalBackground;
    }

    /// <summary>
    /// Click-Handler fuer den Filter-Button neben dem Sync-Bereich. Schaltet
    /// den Erstauswahl-/AlwaysOn-Filter manuell um:
    /// - Beim Aktivieren werden alle nicht-aktiven Prompts ausgeblendet, die
    ///   Tabs aller Kategorien mit aktiven Prompts werden in RefreshAsync
    ///   automatisch wieder eingetragen (visuelle Markierung).
    /// - Beim Deaktivieren wird der Standard-Zustand wiederhergestellt: alle
    ///   Kategorien werden wieder aktiv (Tabs alle gelb), so dass der
    ///   Benutzer nach einem Filter-Aus wieder den vollen Ueberblick hat.
    /// Identisch zum Tab-Klick-Pfad existiert fuer den Wechsel auch ein
    /// einzelner Tab-Klick als Filter-Aus-Weg — beide Pfade rufen
    /// <see cref="UpdateFilterButtonVisual"/> auf, sodass der Button-Look
    /// immer dem internen Modus entspricht.
    /// </summary>
    private async Task ToggleAlwaysOnFilterAsync()
    {
        if (_alwaysOnFilterMode)
        {
            // Filter aus: alle Kategorien aktiv, alle Prompts sichtbar.
            _alwaysOnFilterMode = false;
            _activeCategoryIds.Clear();
            foreach (var cat in _categories)
                _activeCategoryIds.Add(cat.Id);
        }
        else
        {
            // Filter an: RefreshAsync fuellt _activeCategoryIds neu mit
            // allen Kategorien die aktive Prompts haben.
            _alwaysOnFilterMode = true;
            _activeCategoryIds.Clear();
        }
        UpdateFilterButtonVisual();
        await RefreshAsync();
    }

    /// <summary>
    /// Aktualisiert das Aussehen des Filter-Buttons abhaengig vom aktuellen
    /// <see cref="_alwaysOnFilterMode"/>. Aktiv = goldener Hintergrund mit
    /// dunklem Glyph (matcht den Look der gelben Haekchen-Checkboxen in den
    /// Prompt-Reihen). Inaktiv = Standard-Toolbar-Look (dunkler Hintergrund,
    /// weisser Glyph). Tooltip wechselt entsprechend, damit der Benutzer
    /// auf einen Blick versteht ob ein Klick den Filter ein- oder ausschaltet.
    /// Wird ueberall aufgerufen wo <see cref="_alwaysOnFilterMode"/>
    /// veraendert wird (Toggle, IsVisibleChanged, Tab-Klick, Constructor).
    /// </summary>
    private void UpdateFilterButtonVisual()
    {
        if (_alwaysOnFilterMode)
        {
            BtnFilterActiveOnly.Background = BrushGold;
            BtnFilterActiveOnly.Foreground = BrushDarkFg;
            BtnFilterActiveOnly.ToolTip = "Filter aktiv — nur Prompts mit Haekchen sind sichtbar. Klick zeigt wieder alle.";
        }
        else
        {
            BtnFilterActiveOnly.Background = BrushFilterInactiveBg;
            BtnFilterActiveOnly.Foreground = System.Windows.Media.Brushes.White;
            BtnFilterActiveOnly.ToolTip = "Nur aktivierte Prompts anzeigen — blendet alle Prompts ohne Haekchen aus. Erneuter Klick zeigt wieder alle.";
        }
    }

    /// <summary>
    /// Versucht den Inhalt der Eingabe-Box als Submit auszuloesen. Liefert
    /// <c>true</c> wenn Text vorhanden war und gesendet wurde, sonst <c>false</c>.
    /// Wird vom Voice-Overlay verwendet, damit der orange Enter-Button unten
    /// in der Pillar-Leiste den Text aus der Prompt-Eingabe abschickt.
    /// </summary>
    public bool TrySubmitInputText()
    {
        return _inputWindow?.TrySubmitText() ?? false;
    }

    private void OpenInputWindow()
    {
        // Eingabe und Historie schliessen sich gegenseitig aus — beide
        // docken am gleichen Platz links neben dem Promtboard. Wenn die
        // Historie offen ist, machen wir sie zu BEVOR die Eingabe sich
        // einblendet, damit nie beide gleichzeitig dort liegen.
        if (_historyWindowVisible) CloseHistoryWindow();
        if (_inputWindow is null)
        {
            _inputWindow = new PromptInputWindow();
            _inputWindow.SubmitRequested += async text =>
            {
                if (_inputSubmitInProgress) return;
                _inputSubmitInProgress = true;
                var sourceWindow = _inputWindow;
                try
                {
                    var submit = InputSubmitRequested;
                    if (submit is null || !await submit(text)) return;

                    if (ReferenceEquals(_inputWindow, sourceWindow))
                        sourceWindow?.ClearInput();
                    _persistedInputText = string.Empty;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Input submit failed: {ex.Message}");
                }
                finally { _inputSubmitInProgress = false; }
            };
            // Rechtsklick-Drag im Eingabefenster verschiebt die GANZE Gruppe.
            _inputWindow.GroupDragDelta += OnChildGroupDrag;
            // Stern-Klick in der Eingabe-Toolbar: Event 1:1 ans OverlayWindow
            // weiterreichen — der entscheidet ob das Promtboard versteckt
            // wird und wo das Eingabefenster anschliessend andockt.
            _inputWindow.SoloDockToggleRequested += newState =>
                SoloDockToggleRequested?.Invoke(newState);
            // Prompt-Zwischenspeicher: Speichern/Loeschen direkt in den Store,
            // danach SOFORT Cloud-Sync anstossen (Frank-Wunsch). Async-Lambda
            // mit try/catch — kein unbehandeltes async void (Bug-Almanach §7.2).
            _inputWindow.SlotSaveRequested += async (number, text) =>
            {
                try
                {
                    await VoiceServiceProvider.Slots.SaveAsync(number, text);
                    SlotsSyncRequested?.Invoke();
                }
                catch (Exception ex) { Console.WriteLine($"Slot save failed: {ex.Message}"); }
            };
            _inputWindow.SlotDeleteRequested += async number =>
            {
                try
                {
                    await VoiceServiceProvider.Slots.DeleteAsync(number);
                    SlotsSyncRequested?.Invoke();
                }
                catch (Exception ex) { Console.WriteLine($"Slot delete failed: {ex.Message}"); }
            };
            // Drag&Drop: Prompt von einer Zahl auf eine andere ziehen → verschieben
            // (Ziel leer) bzw. tauschen (Ziel belegt). MoveAsync setzt frische
            // Zeitstempel, danach sofort Cloud-Sync → anderer PC zieht es nach.
            _inputWindow.SlotMoveRequested += async (from, to) =>
            {
                try
                {
                    await VoiceServiceProvider.Slots.MoveAsync(from, to);
                    SlotsSyncRequested?.Invoke();
                }
                catch (Exception ex) { Console.WriteLine($"Slot move failed: {ex.Message}"); }
            };
            // Frisch generierte KI-Zusammenfassung persistieren (nur wenn der Text
            // noch passt) und SOFORT Cloud-Sync anstossen, damit der Hover-Tooltip
            // auch ins Drive-Backup wandert und auf andere Geraete synct.
            _inputWindow.SlotSummaryRequested += async (number, text, summary) =>
            {
                try
                {
                    await VoiceServiceProvider.Slots.SetSummaryAsync(number, text, summary);
                    SlotsSyncRequested?.Invoke();
                }
                catch (Exception ex) { Console.WriteLine($"Slot summary save failed: {ex.Message}"); }
            };
            // Rechtsklick-Prioritaet (Hoch/Mittel/Niedrig/Keine) persistieren und
            // SOFORT Cloud-Sync anstossen, damit die farbige Einfaerbung auch ins
            // Google-Drive-Backup wandert und auf andere Geraete synct.
            _inputWindow.SlotPriorityRequested += async (number, priority) =>
            {
                try
                {
                    await VoiceServiceProvider.Slots.SetPriorityAsync(number, priority);
                    SlotsSyncRequested?.Invoke();
                }
                catch (Exception ex) { Console.WriteLine($"Slot priority save failed: {ex.Message}"); }
            };
            _inputWindow.Closed += (_, _) =>
            {
                _inputWindow = null;
                _inputWindowVisible = false;
            };
            // Frischen Window-Aufbau: noch nicht abgeschickten Text aus dem
            // letzten Stern-Zyklus zurueckschreiben, damit dem Benutzer beim
            // Toggle nichts verloren geht.
            if (!string.IsNullOrEmpty(_persistedInputText))
            {
                _inputWindow.SetText(_persistedInputText);
            }
        }
        _inputWindow.DockTo(this, force: true);
        _inputWindow.Show();
        _inputWindowVisible = true;
        // Belegte Slots in die Zahlen-Leiste laden — bei jedem Oeffnen, damit
        // ein zwischenzeitlicher Cloud-Merge sofort sichtbar wird.
        ReloadSlots();
    }

    private void CloseInputWindow()
    {
        if (_inputWindow is null)
        {
            _inputWindowVisible = false;
            return;
        }
        var win = _inputWindow;
        // Noch nicht abgeschickten Text in den Session-Puffer sichern, BEVOR
        // das Fenster zerstoert wird. Beim naechsten Oeffnen wird der Text
        // in die InputBox des frischen Fensters zurueckgeschrieben.
        _persistedInputText = win.GetCurrentText();
        _inputWindow = null;
        _inputWindowVisible = false;
        win.Close();
    }

    /// <summary>
    /// Reagiert auf einen Rechtsklick-Drag in einem der Kinder (Eingabe
    /// oder Historie). Verschiebt das Promptboard um den gleichen Versatz
    /// — durch das anschliessende PanelDragged-Event folgt der Voice-
    /// Pillar, durch FollowPanelDrag folgen die beiden Kinder. Damit
    /// bewegt sich die GANZE Gruppe als ein einziges starres Konstrukt,
    /// egal an welchem Fenster der Benutzer angefasst hat.
    /// </summary>
    private void OnChildGroupDrag(double dx, double dy)
    {
        Left += dx;
        Top  += dy;
        PanelDragged?.Invoke();                      // Voice-Pillar folgt
        _inputWindow?.FollowPanelDrag(this);         // Eingabe re-dockt
        _historyWindow?.FollowPanelDrag(this);       // Historie re-dockt
    }

    /// <summary>
    /// Setzt Text ins Eingabefeld bzw. oeffnet das Fenster zuerst, falls es
    /// noch nicht da ist. Wird vom Voice-Overlay genutzt, damit gesprochene
    /// Prompts in das Eingabefenster geroutet werden statt direkt in die
    /// CLI — so landen auch Voice-Prompts in der Historie (Phase 4).
    ///
    /// Wenn die Box bereits Text enthaelt, wird der neue Voice-Schnipsel
    /// mit " ; " als Aufgaben-Trenner angehaengt — Mehrfach-Diktat ohne
    /// Datenverlust. Wenn <paramref name="autoSubmit"/> true ist (Auto-
    /// Enter-Toggle aktiv), wird der zusammengebaute Text direkt
    /// abgeschickt — als haette der Benutzer Enter gedrueckt.
    /// </summary>
    public void RouteVoiceTextToInput(string text, bool autoSubmit)
    {
        if (string.IsNullOrEmpty(text)) return;
        if (!_inputWindowVisible) OpenInputWindow();
        _inputWindow?.AppendVoiceText(text, autoSubmit);
    }

    /// <summary>True wenn der Stern an ist und das Eingabefenster sichtbar.</summary>
    public bool IsInputWindowVisible => _inputWindowVisible;

    /// <summary>True wenn das Historie-Fenster gerade sichtbar ist. Das
    /// OverlayWindow nutzt das, um den Auto-Einklapp-Timer zu pausieren
    /// solange der Benutzer in der Historie scrollt/arbeitet.</summary>
    public bool IsHistoryWindowVisible => _historyWindowVisible;

    /// <summary>
    /// Versteckt Eingabe- und Historie-Fenster — wird vom OverlayWindow
    /// gerufen sobald der Benutzer aus dem Terminal in eine andere App
    /// wechselt. Wir nutzen <c>Hide()</c>, NICHT <c>Close()</c>: der
    /// Benutzer-Wunsch (Sichtbarkeits-Flags) bleibt erhalten, beim
    /// Zurueckwechseln ins Terminal bringen wir die Fenster automatisch
    /// wieder zurueck. So sind die Floating-Panels nie ueber Chrome,
    /// VS Code oder anderen App-Fenstern zu sehen.
    /// </summary>
    public void HideTransientChildren()
    {
        if (_inputWindow is not null && _inputWindow.IsVisible)
        {
            _inputWindow.Hide();
        }
        if (_historyWindow is not null && _historyWindow.IsVisible)
        {
            _historyWindow.Hide();
        }
    }

    /// <summary>
    /// Bringt Eingabe- und Historie-Fenster zurueck, falls der Benutzer
    /// sie vor dem App-Wechsel offen hatte. Wird nach
    /// <see cref="HideTransientChildren"/> gerufen sobald das Terminal
    /// wieder aktiv ist. Position wird neu angedockt — das Promptboard
    /// kann sich in der Zwischenzeit verschoben haben.
    /// </summary>
    public void ShowTransientChildrenIfNeeded()
    {
        if (_inputWindowVisible && _inputWindow is not null && !_inputWindow.IsVisible)
        {
            _inputWindow.DockTo(this);
            _inputWindow.Show();
        }
        if (_historyWindowVisible && _historyWindow is not null && !_historyWindow.IsVisible)
        {
            _historyWindow.DockTo(this);
            _historyWindow.Show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Historie-Fenster (Linksklick auf Eintrag → Text in Eingabefenster)
    // ─────────────────────────────────────────────────────────────────────

    private async void ToggleHistoryWindow()
    {
        // async void here is an event-handler wrapper (BtnHistory.Click). An
        // exception out of OpenHistoryWindowAsync would otherwise travel
        // unhandled into the WPF dispatcher loop and crash the process without
        // any visible error. Catch and log defensively (same pattern as the
        // other click handlers in this file).
        try
        {
            if (_historyWindowVisible)
            {
                CloseHistoryWindow();
            }
            else
            {
                await OpenHistoryWindowAsync();
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[PBPanel] ToggleHistoryWindow failed: {ex.Message}");
        }
    }

    private async Task OpenHistoryWindowAsync()
    {
        // Eingabe und Historie schliessen sich gegenseitig aus — beide
        // docken am gleichen Platz links neben dem Promtboard. Wenn die
        // Eingabe offen ist, machen wir sie zu BEVOR die Historie sich
        // einblendet, damit nie beide gleichzeitig dort liegen.
        if (_inputWindowVisible) CloseInputWindow();
        if (_historyWindow is null)
        {
            _historyWindow = new PromptHistoryWindow();
            _historyWindow.EntrySelected += text =>
            {
                // Eintrag in das Eingabefenster routen — und das Eingabe-
                // fenster oeffnen wenn es noch nicht offen ist (sonst
                // wuerde der Klick ins Leere gehen).
                if (!_inputWindowVisible) OpenInputWindow();
                _inputWindow?.SetText(text);
            };
            _historyWindow.EntryEditRequested += async entry =>
            {
                await EditHistoryEntryAsync(entry);
            };
            // Rechtsklick-Drag im Historie-Fenster verschiebt die GANZE Gruppe.
            _historyWindow.GroupDragDelta += OnChildGroupDrag;
            _historyWindow.Closed += (_, _) =>
            {
                _historyWindow = null;
                _historyWindowVisible = false;
                UpdateHistoryButtonVisual();
            };
        }

        // Andocken links am Promtboard — gleiche Position wie das
        // Eingabefenster. Wenn beide gleichzeitig offen sind, liegt die
        // Historie ueber dem Eingabefenster (Z-Order); der Benutzer kann
        // sie via Rechtsklick frei verschieben wenn er beide gleichzeitig
        // sehen will.
        _historyWindow.DockTo(this, force: true);

        await ReloadHistoryAsync();
        _historyWindow.Show();
        _historyWindowVisible = true;
        UpdateHistoryButtonVisual();
    }

    private void CloseHistoryWindow()
    {
        if (_historyWindow is null)
        {
            _historyWindowVisible = false;
            UpdateHistoryButtonVisual();
            return;
        }
        var win = _historyWindow;
        _historyWindow = null;
        _historyWindowVisible = false;
        UpdateHistoryButtonVisual();
        win.Close();
    }

    /// <summary>
    /// Liest die aktuelle Historie aus dem Service und uebergibt sie an
    /// das Fenster. Wird beim Oeffnen aufgerufen sowie public erreichbar
    /// damit das OverlayWindow nach jedem Submit ein Re-Render anstossen
    /// kann (sonst sieht der Benutzer seinen frischen Eintrag erst nach
    /// dem naechsten Oeffnen).
    /// </summary>
    public async Task ReloadHistoryAsync()
    {
        if (_historyWindow is null) return;
        try
        {
            var entries = await _historyService.LoadAsync();
            _historyWindow.Render(entries);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"ReloadHistoryAsync failed: {ex.Message}");
        }
    }

    /// <summary>
    /// Wird ausgeloest nachdem der Benutzer einen Historie-Eintrag im
    /// Editor-Dialog veraendert und gespeichert hat. Das OverlayWindow
    /// abonniert dieses Event und stoesst einen Cloud-Upload der
    /// kompletten Historie an, damit der bearbeitete Eintrag auch auf der
    /// Mac-Seite sichtbar wird.
    /// </summary>
    public event Action? HistorySyncRequested;

    /// <summary>
    /// Wird ausgeloest nachdem ein Prompt-Zwischenspeicher-Slot gespeichert
    /// oder geloescht wurde. Das OverlayWindow abonniert dieses Event und
    /// stoesst SOFORT einen Cloud-Upload der prompt-slots.json an, damit der
    /// Stand auch auf dem anderen Geraet ankommt.
    /// </summary>
    public event Action? SlotsSyncRequested;

    /// <summary>
    /// Wird ausgeloest nachdem der Settings-Dialog gespeichert wurde (das
    /// persoenliche Vokabular-Woerterbuch koennte geaendert worden sein). Das
    /// OverlayWindow abonniert dieses Event und stoesst einen Cloud-Upload der
    /// personal-vocabulary.txt an, damit der Stand auf alle Geraete kommt.
    /// </summary>
    public event Action? VocabularySyncRequested;

    /// <summary>
    /// Laedt die belegten Slots aus dem Store und faerbt die Zahlen-Leiste im
    /// offenen Eingabefenster nach. Marshallt auf den UI-Thread, weil der
    /// Store-Zugriff off-thread zurueckkommen kann. Wird beim Oeffnen der
    /// Eingabe und vom OverlayWindow nach dem Cloud-Merge aufgerufen.
    /// </summary>
    public void ReloadSlots() => _ = LoadSlotsIntoInputAsync();

    private async Task LoadSlotsIntoInputAsync()
    {
        try
        {
            var (map, times, summaries, priorities) = await VoiceServiceProvider.Slots.LoadMapTimesSummariesAsync().ConfigureAwait(false);
            await Dispatcher.InvokeAsync(() => _inputWindow?.SetSlotContents(map, times, summaries, priorities));
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Slot load failed: {ex.Message}");
        }
    }

    /// <summary>
    /// Oeffnet den modalen Editor fuer einen Historie-Eintrag, persistiert
    /// die Aenderung und meldet das History-Window neu zu rendern. Wird
    /// vom Rechtsklick auf eine Zeile ausgeloest.
    /// </summary>
    private async Task EditHistoryEntryAsync(PromptHistoryEntry entry)
    {
        if (entry is null || string.IsNullOrEmpty(entry.Id)) return;

        var dlg = new PromptHistoryEditDialog(entry.Text, entry.Title, entry.Timestamp)
        {
            Owner = this,
        };
        var result = dlg.ShowDialog();
        if (result != true) return;

        string newText = dlg.EditedText ?? string.Empty;
        if (newText == (entry.Text ?? string.Empty)) return;

        try
        {
            await _historyService.UpdateTextAsync(entry.Id, newText);
            await ReloadHistoryAsync();
            // Cloud-Sync anstossen damit die Aenderung auch auf dem
            // Mac ankommt — Upload laeuft via OverlayWindow weil dort der
            // PromptHistoryDriveSync mit Drive-Credentials lebt.
            HistorySyncRequested?.Invoke();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"EditHistoryEntryAsync failed: {ex.Message}");
        }
    }

    private void UpdateHistoryButtonVisual()
    {
        // Aktiv-Zustand: gelbe Fuellung wie der Stern. Inaktiv: gleiche
        // gedaempfte Farbe wie die anderen Toolbar-Symbole.
        if (_historyWindowVisible)
        {
            BtnHistory.Foreground = BrushGold;
            BtnHistory.ToolTip    = "Prompt-Historie schliessen";
        }
        else
        {
            BtnHistory.Foreground = BrushHistoryInactive;
            BtnHistory.ToolTip    = "Prompt-Historie";
        }
    }

    /// <summary>
    /// Wird aufgerufen wenn das Promtboard geschlossen wird (z.B. durch
    /// den zweiten Klick auf den Voice-Overlay-Stern). Schliesst die beiden
    /// abhaengigen Fenster (Eingabe + Historie) gleich mit, damit keine
    /// "Geister"-Fenster ohne Eltern-Board uebrig bleiben. Ohne diesen
    /// Override wuerde das Eingabefenster im Solo-Modus offen bleiben,
    /// obwohl der Benutzer alles dichtmachen wollte.
    /// </summary>
    protected override void OnClosed(EventArgs e)
    {
        try
        {
            _inputWindow?.Close();
            _historyWindow?.Close();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"PromptBoardPanel.OnClosed cleanup failed: {ex.Message}");
        }
        base.OnClosed(e);
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);
        var hwnd = new WindowInteropHelper(this).Handle;
        int exStyle = Win32.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
        Win32.SetWindowLong(hwnd, Win32.GWL_EXSTYLE, exStyle | Win32.WS_EX_NOACTIVATE | Win32.WS_EX_TOOLWINDOW);
    }

    /// <summary>
    /// Pro-Render-Cache fuer alle Prompts. Wird zu Beginn von RefreshAsync mit
    /// einem einzigen GetAllAsync()-Roundtrip befuellt und an RenderPromptsAsync
    /// + die HotkeyRegistry-Logik weitergereicht. Spart die alte N+1-Schleife
    /// (eine Query pro Kategorie, mehrfach).
    /// </summary>
    private List<Prompt> _allPromptsCache = new();

    /// <summary>
    /// True wenn der aktuelle Cache durch einen DB-Fehler leer ist. In dem
    /// Fall wird die HotkeyRegistry NICHT ueberschrieben — sie bleibt auf
    /// dem letzten erfolgreichen Stand ("better stale than broken").
    /// </summary>
    private bool _allPromptsCacheStaleDueToError;

    /// <summary>
    /// Lookup von Kategorie-Id auf Kategorie. Wird nach jedem RefreshAsync
    /// neu aufgebaut. Spart pro Row im Listen-Render einen
    /// FirstOrDefault-Linear-Scan ueber _categories
    /// (vorher O(N*K), jetzt O(N+K) gesamt).
    /// </summary>
    private Dictionary<Guid, Category> _categoryById = new();

    /// <summary>
    /// Cache fuer gefrorene SolidColorBrushes pro Kategorie. Frueher legte
    /// RowBackgroundFor pro Row einen neuen unfrozen Brush an (50 Prompts =
    /// 50 Brush-Allokationen pro Render). Mit Cache + Freeze() dispatched
    /// WPF die Brushes ohne Threading-Check und kann sie ueber alle Rows
    /// teilen.
    /// </summary>
    private readonly Dictionary<Guid, SolidColorBrush> _rowBrushCache = new();

    public async Task RefreshAsync()
    {
        _allPromptsCacheStaleDueToError = false;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var categoryRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            // Summary-Variante (ohne Prompts-Include) — RefreshAsync laedt
            // direkt darunter eh _allPromptsCache via promptRepo.GetAllAsync,
            // also waere der Categories-JOIN ueber Prompts doppelte Arbeit.
            _categories = (await categoryRepo.GetAllSummaryAsync())
                .OrderBy(c => c.SortOrder).ThenBy(c => c.Name)
                .ToList();
            // Einmalige Migration: Jede Kategorie ohne Palette-Hex bekommt
            // einen zugewiesen, sodass die Farben kuenftig stabil sind.
            await EnsureCategoryColorsPersistedAsync(categoryRepo);

            // Alle Prompts in EINEM Roundtrip laden — vorher wurden bis zu
            // 3*N Queries pro Render abgesetzt (AlwaysOn-Scan,
            // RenderPrompts pro Kategorie, HotkeyRegistry pro Kategorie).
            // Bei 8 Kategorien sparte das vorher bis zu 24 DB-Roundtrips
            // pro Tab-Klick.
            var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            _allPromptsCache = (await promptRepo.GetAllAsync()).ToList();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"PromptBoardPanel refresh failed: {ex.Message}");
            _categories = new List<Category>();
            _allPromptsCache = new List<Prompt>();
            _allPromptsCacheStaleDueToError = true;
        }

        // Lookup-Tabellen aktualisieren — einmal pro Refresh, statt pro Row
        // im spaeteren Render. Brush-Cache wird beim Refresh geleert, weil
        // eine Kategorie ihre Farbe (BackgroundColorHex) im Edit-Dialog
        // geaendert haben koennte und der gefrorene Brush sonst stale waere.
        // Bei N Kategorien sind das N Brush-Allokationen pro Refresh — viel
        // weniger als die alten N pro Row.
        _categoryById = _categories.ToDictionary(c => c.Id);
        _rowBrushCache.Clear();
        RefreshHotkeyRegistryFromCache();

        // Prune stale ids (a category deleted elsewhere) but keep every id the
        // user still has active.
        var known = _categories.Select(c => c.Id).ToHashSet();
        _activeCategoryIds.IntersectWith(known);

        // Erstauswahl-Modus: alle Kategorien mit mindestens einem Always-On-Prompt
        // werden in _activeCategoryIds eingetragen, damit ihre Tabs visuell als
        // aktiv markiert sind. In-memory Filter ueber den frisch geladenen
        // Cache — keine zusaetzliche DB-Roundtrip mehr.
        if (_alwaysOnFilterMode)
        {
            foreach (var cat in _categories)
            {
                bool hasAlwaysOn = false;
                foreach (var p in _allPromptsCache)
                {
                    if (p.CategoryId == cat.Id && p.IsAlwaysOn) { hasAlwaysOn = true; break; }
                }
                if (hasAlwaysOn) _activeCategoryIds.Add(cat.Id);
            }
        }

        RenderCategoryTabs();

        if (_categories.Count == 0)
        {
            RenderEmptyState("Noch keine Kategorien. Klick +");
            return;
        }

        await RenderPromptsAsync();
    }

    // ──────────────── Color helpers ────────────────

    private Color ColorForCategory(Guid id)
    {
        // Erst pruefen ob die Kategorie einen Palette-Hex aus unserer
        // CategoryPalette persistiert hat — dann ist die Farbe stabil
        // pro Kategorie, egal wie sie sortiert ist. Die einmalige Migration
        // in EnsureCategoryColorsPersistedAsync sorgt dafuer dass alle
        // Kategorien diesen Eintrag bekommen.
        // Lookup ueber das in RefreshAsync gepflegte Dictionary statt
        // FirstOrDefault — O(1) statt O(K) pro Aufruf, damit eine Render-
        // Schleife mit N Rows ueber K Kategorien nicht O(N*K) wird.
        if (_categoryById.TryGetValue(id, out var cat) && cat is not null)
        {
            int paletteIdx = Array.FindIndex(CategoryPaletteHex,
                h => string.Equals(h, cat.BackgroundColorHex, StringComparison.OrdinalIgnoreCase));
            if (paletteIdx >= 0) return CategoryPalette[paletteIdx];
        }
        // Fallback (sollte nach Migration nie greifen): Listenposition als
        // Index — gleiche Logik wie vor dem Bug, damit nichts crashed.
        int idx = _categories.FindIndex(c => c.Id == id);
        if (idx < 0) idx = 0;
        return CategoryPalette[idx % CategoryPalette.Length];
    }

    /// <summary>
    /// Migration: Sorgt dafuer dass jede Kategorie eine vivid Palette-Farbe
    /// in BackgroundColorHex stehen hat. Wird einmal pro Session nach dem
    /// Laden der Kategorien aufgerufen. Existierende Palette-Eintraege
    /// werden NICHT angefasst — nur Pastel-Defaults oder leere Felder
    /// bekommen einen Palette-Hex zugewiesen, basierend auf der aktuellen
    /// Listenposition (so erinnert die Migration an die alten Farben).
    /// Nach der Migration sind die Farben stabil ueber Reorder hinweg.
    /// </summary>
    private async Task EnsureCategoryColorsPersistedAsync(ICategoryRepository repo)
    {
        for (int i = 0; i < _categories.Count; i++)
        {
            var cat = _categories[i];
            bool alreadyPalette = Array.FindIndex(CategoryPaletteHex,
                h => string.Equals(h, cat.BackgroundColorHex, StringComparison.OrdinalIgnoreCase)) >= 0;
            if (alreadyPalette) continue;

            cat.BackgroundColorHex = CategoryPaletteHex[i % CategoryPaletteHex.Length];
            await repo.UpdateAsync(cat);
        }
    }

    /// <summary>
    /// Dim but clearly tinted row background — keeps the dark-panel aesthetic
    /// so white prompt text stays legible, but lets you see each row's
    /// category at a glance from the tinted bar.
    /// </summary>
    private SolidColorBrush RowBackgroundFor(Guid categoryId)
    {
        // Cache + Freeze — pro Kategorie wird der Mix-Brush einmal berechnet
        // und gefroren. Frozen Brushes umgehen WPFs Threading-Pruefungen und
        // koennen ueber alle Rows einer Kategorie geteilt werden statt pro
        // Row eine neue Instanz zu allokieren. Wenn die Kategorie-Farbe sich
        // aendert, faengt der naechste Vergleich der Hex-Quelle das auf —
        // dann wird der Cache-Eintrag erneuert.
        if (_rowBrushCache.TryGetValue(categoryId, out var cached) && cached is not null)
            return cached;

        var c = ColorForCategory(categoryId);
        // Blend ~30% category color over a dark base.
        byte r = (byte)((c.R * 0.30) + (0x25 * 0.70));
        byte g = (byte)((c.G * 0.30) + (0x25 * 0.70));
        byte b = (byte)((c.B * 0.30) + (0x25 * 0.70));
        var brush = new SolidColorBrush(Color.FromRgb(r, g, b));
        if (brush.CanFreeze) brush.Freeze();
        _rowBrushCache[categoryId] = brush;
        return brush;
    }

    // ──────────────── Rendering: categories ────────────────

    private void RenderCategoryTabs()
    {
        CategoryTabs.Children.Clear();
        foreach (var cat in _categories)
        {
            bool isActive = _activeCategoryIds.Contains(cat.Id);
            var catColor = ColorForCategory(cat.Id);

            var btn = new Button
            {
                Content = cat.Name,
                Tag = cat.Id,
                Style = (Style)FindResource("CategoryTab"),
            };
            // Override the static-style background per category. Active = full
            // category color, inactive = the static dark grey from the style.
            if (isActive)
            {
                btn.Background = new SolidColorBrush(catColor);
                btn.FontWeight = FontWeights.Bold;
            }

            btn.Click += async (_, _) =>
            {
                // Wenn gerade ein echter Kategorie-Drag ausgefuehrt wurde,
                // unterdrueck den nachgelagerten Click — sonst wuerde das
                // Drag-Loslassen die Kategorie ungewollt toggeln.
                if (_categoryDragJustHappenedId == cat.Id)
                {
                    _categoryDragJustHappenedId = null;
                    return;
                }
                // Erstauswahl-Modus beenden: Sobald der Benutzer bewusst auf
                // einen Tab klickt, wechseln wir aus dem Filter-Modus in den
                // normalen Modus. Die im Filter-Modus aktiv markierten Tabs
                // sind nur Visualisierung — sie werden hier vollstaendig
                // verworfen, sodass ausschliesslich der angeklickte Tab aktiv
                // wird (statt zusaetzlich zu den Filter-Tabs).
                if (_alwaysOnFilterMode)
                {
                    _alwaysOnFilterMode = false;
                    _activeCategoryIds.Clear();
                    _activeCategoryIds.Add(cat.Id);
                    UpdateFilterButtonVisual();
                }
                else
                {
                    // Toggle: clicking an active tab turns it off, clicking an
                    // inactive one adds it. Multiple can be active simultaneously.
                    if (_activeCategoryIds.Contains(cat.Id))
                        _activeCategoryIds.Remove(cat.Id);
                    else
                        _activeCategoryIds.Add(cat.Id);
                }
                RenderCategoryTabs();
                await RenderPromptsAsync();
            };
            btn.ContextMenu = BuildCategoryContextMenu(cat);

            // Drop target: a prompt dragged onto another category tab moves
            // the prompt into that category (CategoryId update + auto-backup).
            // DragOver fires continuously while the cursor is over the tab —
            // we MUST re-set e.Effects every time, otherwise WPF resets the
            // effect to "None" on the next event tick and the drop silently
            // becomes invalid.
            //
            // ZUSAETZLICH: Wenn ein anderer Kategorie-Tab auf diesen Tab
            // gezogen wird, sortieren wir die Kategorien neu — derselbe
            // Tab ist also Drop-Target fuer ZWEI verschiedene Drag-Typen.
            // Unterscheidung ueber das DataObject-Format.
            btn.AllowDrop = true;
            btn.DragOver  += (_, e) => HighlightDropTarget(btn, catColor, e, true);
            btn.DragLeave += (_, _) => HighlightDropTarget(btn, catColor, null, false);
            btn.Drop      += async (_, e) =>
            {
                if (e.Data.GetDataPresent(CategoryDragFormat))
                    await OnCategoryDroppedOnCategoryAsync(cat.Id, btn, e);
                else
                    await OnPromptDroppedOnCategoryAsync(cat.Id, btn, catColor, e);
            };

            // Drag source: PreviewMouseLeftButtonDown armiert, PreviewMouseMove
            // ueber 6-Pixel-Threshold loest DoDragDrop aus. Spiegelbildlich zur
            // Prompt-Row-Drag-Logik. Wir muessen Preview-Varianten nutzen, weil
            // der Button selbst Click-Events absorbiert — ohne Tunneling kaeme
            // der Mausevent nie hier an.
            var thisCatId = cat.Id;
            btn.PreviewMouseLeftButtonDown += (_, e) =>
            {
                _categoryDragArmStart = e.GetPosition(this);
                _categoryDragArmedId  = thisCatId;
            };
            btn.PreviewMouseMove += (s, e) =>
            {
                if (e.LeftButton != System.Windows.Input.MouseButtonState.Pressed) return;
                if (_categoryDragArmedId != thisCatId) return;
                var current = e.GetPosition(this);
                var dx = current.X - _categoryDragArmStart.X;
                var dy = current.Y - _categoryDragArmStart.Y;
                if (dx * dx + dy * dy < 36) return; // < 6 px = Klick, sonst Drag

                _categoryDragArmedId = null;
                _categoryDragJustHappenedId = thisCatId;
                try
                {
                    var data = new DataObject(CategoryDragFormat, thisCatId.ToString());
                    System.Windows.DragDrop.DoDragDrop((DependencyObject)s!, data,
                        System.Windows.DragDropEffects.Move);
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Category DoDragDrop failed: {ex.Message}");
                }
            };

            CategoryTabs.Children.Add(btn);
        }
    }

    /// <summary>
    /// Wird ausgeloest wenn ein Kategorie-Tab auf einen anderen Kategorie-Tab
    /// gezogen wird. Tauscht die SortOrder von Quelle und Ziel — also reines
    /// Swap-Verhalten ohne Insert-Shift. Beispiel: Letzte Kategorie auf erste
    /// gezogen → die ehemals erste wandert ans Ende, die ehemals letzte nach
    /// vorne. Eindeutig und ohne Cursor-Position-Interpretation.
    /// </summary>
    private async Task OnCategoryDroppedOnCategoryAsync(Guid targetCategoryId,
        System.Windows.Controls.Button btn, System.Windows.DragEventArgs e)
    {
        // Visuelle Hervorhebung zuruecksetzen — DragLeave triggert nicht
        // immer beim Drop, also explizit.
        var catColor = ColorForCategory(targetCategoryId);
        HighlightDropTarget(btn, catColor, null, false);

        if (!e.Data.GetDataPresent(CategoryDragFormat)) return;
        if (e.Data.GetData(CategoryDragFormat) is not string idStr ||
            !Guid.TryParse(idStr, out var sourceId)) return;
        if (sourceId == targetCategoryId) return; // Drop auf sich selbst

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();

            var source = _categories.FirstOrDefault(c => c.Id == sourceId);
            var target = _categories.FirstOrDefault(c => c.Id == targetCategoryId);
            if (source is null || target is null) return;

            // SWAP: SortOrder von Quelle und Ziel tauschen. Wenn beide
            // versehentlich denselben Wert haben (kann nach Imports passieren),
            // mit lueckenloser Renumberierung als Fallback aufraeumen.
            int sourceOrder = source.SortOrder;
            int targetOrder = target.SortOrder;
            if (sourceOrder == targetOrder)
            {
                var ordered = _categories
                    .OrderBy(c => c.SortOrder).ThenBy(c => c.Name)
                    .ToList();
                for (int i = 0; i < ordered.Count; i++)
                {
                    if (ordered[i].SortOrder != i)
                    {
                        ordered[i].SortOrder = i;
                        await repo.UpdateAsync(ordered[i]);
                    }
                }
                sourceOrder = source.SortOrder;
                targetOrder = target.SortOrder;
            }

            source.SortOrder = targetOrder;
            target.SortOrder = sourceOrder;
            await repo.UpdateAsync(source);
            await repo.UpdateAsync(target);

            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Kategorien tauschen fehlgeschlagen: {ex.Message}",
                "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await RefreshAsync();
    }

    /// <summary>
    /// Visual feedback while a prompt drag hovers over a category tab —
    /// brightens the tab regardless of active state so the user can tell
    /// where the drop will land. <paramref name="enter"/>=false restores
    /// the resting style.
    /// </summary>
    private void HighlightDropTarget(System.Windows.Controls.Button btn, Color catColor,
                                     System.Windows.DragEventArgs? e, bool enter)
    {
        if (e is not null)
        {
            // Only react if the drag actually carries a prompt OR a
            // category id — otherwise (e.g. a stray text drag) leave the
            // tab alone.
            bool acceptable = e.Data.GetDataPresent(PromptDragFormat)
                           || e.Data.GetDataPresent(CategoryDragFormat);
            e.Effects = acceptable
                ? System.Windows.DragDropEffects.Move
                : System.Windows.DragDropEffects.None;
            e.Handled = true;
        }
        if (enter)
        {
            btn.Background = new SolidColorBrush(Color.FromArgb(0xFF,
                (byte)Math.Min(255, catColor.R + 40),
                (byte)Math.Min(255, catColor.G + 40),
                (byte)Math.Min(255, catColor.B + 40)));
        }
        else
        {
            // Restore JUST this button. We must NOT re-render the whole tab
            // row here — that throws the live drag target out of the visual
            // tree mid-drag, which silently kills the Drop event and leaves
            // dragging looking like it does nothing.
            bool isActive = btn.Tag is Guid id && _activeCategoryIds.Contains(id);
            btn.Background = isActive
                ? new SolidColorBrush(catColor)
                : BrushDarkBg;
        }
    }

    private async Task OnPromptDroppedOnCategoryAsync(Guid targetCategoryId,
        System.Windows.Controls.Button btn, Color catColor, System.Windows.DragEventArgs e)
    {
        if (!e.Data.GetDataPresent(PromptDragFormat)) return;
        if (e.Data.GetData(PromptDragFormat) is not string idStr ||
            !Guid.TryParse(idStr, out var promptId)) return;

        var prompt = _currentPrompts.FirstOrDefault(p => p.Id == promptId);
        if (prompt is null) return;
        if (prompt.CategoryId == targetCategoryId)
        {
            // Same-category drop is a no-op. Just refresh visuals.
            RenderCategoryTabs();
            return;
        }

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            prompt.CategoryId = targetCategoryId;
            await repo.UpdateAsync(prompt);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Verschieben fehlgeschlagen: {ex.Message}",
                "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await RefreshAsync();
    }

    /// <summary>Custom DataObject format for a prompt drag — id as string.</summary>
    private const string PromptDragFormat = "TVO.PromptId";

    /// <summary>Custom DataObject format fuer einen Kategorie-Drag — Id als String.</summary>
    private const string CategoryDragFormat = "TVO.CategoryId";

    // ── Kategorie-Drag-Arming ──
    // Spiegelbildlich zum Prompt-Row-Drag: Mouse-Down armiert, MouseMove
    // ueber Threshold loest DoDragDrop aus. So bleibt der normale Klick
    // (Toggle aktiv/inaktiv) erhalten und nur eine echte Bewegung wird
    // als Reorder-Drag erkannt.
    private System.Windows.Point _categoryDragArmStart;
    private Guid? _categoryDragArmedId;
    private Guid? _categoryDragJustHappenedId;

    /// <summary>
    /// Splits the stored short label into title text + parenthesised
    /// timestamp suffix, e.g. "Refactor Login (25.04.2026, 16:02)" →
    /// ("Refactor Login", "(25.04.2026, 16:02)"). Returns the whole label
    /// as title with empty timestamp when there's no trailing parenthesis.
    /// </summary>
    private static (string title, string timestamp) SplitLabel(string label)
    {
        int parenIdx = label.LastIndexOf(" (", StringComparison.Ordinal);
        if (parenIdx > 0 && label.EndsWith(")", StringComparison.Ordinal))
        {
            return (label.Substring(0, parenIdx), label.Substring(parenIdx + 1));
        }
        return (label, string.Empty);
    }

    /// <summary>
    /// Erkennt ob der Prompt-Text mit dem Wrapper <c>Pre-Prompt: "..."</c>
    /// oder <c>Post-Prompt: "..."</c> anfaengt und liefert das passende
    /// Titel-Suffix (" - vor" / " - nach") zurueck — sonst leer.
    /// Toleriert dieselben Schreibvarianten wie der PromptEditDialog
    /// (mit/ohne Bindestrich, mit/ohne Leerzeichen, Gross/Klein, deutsche
    /// und ASCII-Anfuehrungszeichen).
    /// </summary>
    private static readonly Regex PrePromptDetectRegex = new(
        @"^\s*Pre[-\s]?Prompt\s*:\s*[""„“]",
        RegexOptions.IgnoreCase | RegexOptions.Compiled);

    private static readonly Regex PostPromptDetectRegex = new(
        @"^\s*Post[-\s]?Prompt\s*:\s*[""„“]",
        RegexOptions.IgnoreCase | RegexOptions.Compiled);

    private static string DetectPrePostSuffix(string text)
    {
        if (string.IsNullOrWhiteSpace(text)) return string.Empty;
        if (PrePromptDetectRegex.IsMatch(text)) return " - vor";
        if (PostPromptDetectRegex.IsMatch(text)) return " - nach";
        return string.Empty;
    }

    /// <summary>
    /// Captures a bitmap snapshot of the source row, dims the row in place,
    /// and attaches the floating <see cref="DragGhostAdorner"/> at the
    /// current cursor position. Called on the same UI thread that's about
    /// to enter <c>DoDragDrop</c>.
    /// </summary>
    private void BeginDragVisual(Border sourceRow, System.Windows.Point cursor)
    {
        try
        {
            // Render the row to a bitmap. We Freeze the bitmap so the WPF
            // render thread doesn't see a half-built image.
            int w = (int)Math.Ceiling(sourceRow.ActualWidth);
            int h = (int)Math.Ceiling(sourceRow.ActualHeight);
            if (w <= 0 || h <= 0) return;
            var bmp = new RenderTargetBitmap(w, h, 96, 96, PixelFormats.Pbgra32);
            bmp.Render(sourceRow);
            bmp.Freeze();

            // Adorner layer comes from the panel's content border — first
            // adornable element above this row.
            var rootElement = (UIElement?)Content;
            if (rootElement is null) return;
            _dragGhostLayer = AdornerLayer.GetAdornerLayer(rootElement);
            if (_dragGhostLayer is null) return;
            _dragGhost = new DragGhostAdorner(rootElement, bmp,
                sourceRow.ActualWidth, sourceRow.ActualHeight);
            _dragGhost.UpdateLocation(cursor);
            _dragGhostLayer.Add(_dragGhost);

            // Dim the source row so the user sees clearly that THIS row is
            // the one being dragged. Restored in EndDragVisual.
            _dragSourceRow = sourceRow;
            _dragSourceRow.Opacity = 0.35;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"BeginDragVisual failed: {ex.Message}");
        }
    }

    private void EndDragVisual()
    {
        try
        {
            if (_dragGhost is not null && _dragGhostLayer is not null)
                _dragGhostLayer.Remove(_dragGhost);
        }
        catch { /* best-effort cleanup */ }
        _dragGhost = null;
        _dragGhostLayer = null;
        if (_dragSourceRow is not null)
        {
            _dragSourceRow.Opacity = 1.0;
            _dragSourceRow = null;
        }
    }

    private ContextMenu BuildCategoryContextMenu(Category cat)
    {
        var menu = new ContextMenu();
        var rename = new MenuItem { Header = "Umbenennen" };
        rename.Click += async (_, _) => await RenameCategoryAsync(cat);
        var del = new MenuItem { Header = "Loeschen" };
        del.Click += async (_, _) => await DeleteCategoryAsync(cat);
        menu.Items.Add(rename);
        menu.Items.Add(del);
        return menu;
    }

    // ──────────────── Rendering: prompts ────────────────

    private async Task RenderPromptsAsync()
    {
        PromptList.Children.Clear();

        if (_activeCategoryIds.Count == 0)
        {
            _currentPrompts = new List<Prompt>();
            RenderEmptyState(
                _alwaysOnFilterMode
                    ? "Keine aktivierten Prompts. Setze ein Haekchen oder klick eine Kategorie."
                    : "Keine Kategorie aktiv. Klick oben auf einen Tab.");
            return;
        }

        // Collect prompts from every active category, carrying the category id
        // along so we can tint each row accordingly. Im Erstauswahl-Modus
        // werden zusaetzlich alle Prompts ohne Haekchen ausgefiltert, sodass
        // nur die Always-On-Prompts sichtbar sind.
        // Filter aus dem Pro-Render-Cache (siehe RefreshAsync) — eine einzige
        // Query reicht fuer alle Kategorien.
        var combined = new List<(Prompt prompt, Guid catId)>();
        foreach (var p in _allPromptsCache)
        {
            if (!_activeCategoryIds.Contains(p.CategoryId)) continue;
            if (_alwaysOnFilterMode && !p.IsAlwaysOn) continue;
            combined.Add((p, p.CategoryId));
        }

        // Reihenfolge der Anzeige: erst alle Prompts der ersten aktiven
        // Kategorie (in deren SortOrder), dann der zweiten, dann der dritten.
        // Sonst wuerden Prompts mit gleicher SortOrder aus verschiedenen
        // Kategorien gemischt erscheinen — verwirrt den Benutzer.
        var categoryOrder = _categories
            .Select((c, idx) => new { c.Id, Idx = idx })
            .ToDictionary(x => x.Id, x => x.Idx);

        var sorted = combined
            .OrderBy(t => categoryOrder.TryGetValue(t.catId, out var idx) ? idx : int.MaxValue)
            .ThenBy(t => t.prompt.SortOrder)
            .ThenBy(t => t.prompt.ShortLabel, StringComparer.CurrentCultureIgnoreCase)
            .ToList();
        _currentPrompts = sorted.Select(t => t.prompt).ToList();

        if (sorted.Count == 0)
        {
            RenderEmptyState("Keine Prompts in den aktiven Kategorien.");
            return;
        }

        foreach (var (prompt, catId) in sorted)
        {
            PromptList.Children.Add(BuildPromptRow(prompt, catId));
        }

    }

    // Refreshes all bindings after every successful cache load. This runs even
    // when no category or prompt is visible, so deleted bindings cannot survive.
    private void RefreshHotkeyRegistryFromCache()
    {
        if (!_allPromptsCacheStaleDueToError)
        {
            var hotkeyEntries = new List<KeyValuePair<int, HotkeyRegistry.Entry>>();
            var letterEntries = new List<KeyValuePair<char, HotkeyRegistry.Entry>>();
            foreach (var p in _allPromptsCache)
            {
                if (p.HotkeyNumber is int n && n >= 1 && n <= 9)
                {
                    hotkeyEntries.Add(new KeyValuePair<int, HotkeyRegistry.Entry>(
                        n, new HotkeyRegistry.Entry(p.Id, p.EffectiveText())));
                }
                if (p.HotkeyLetter is char l)
                {
                    char upper = char.ToUpperInvariant(l);
                    if (upper >= 'A' && upper <= 'Z')
                    {
                        letterEntries.Add(new KeyValuePair<char, HotkeyRegistry.Entry>(
                            upper, new HotkeyRegistry.Entry(p.Id, p.EffectiveText())));
                    }
                }
            }
            HotkeyRegistry.Replace(hotkeyEntries);
            HotkeyRegistry.ReplaceLetters(letterEntries);
        }
    }

    private Border BuildPromptRow(Prompt prompt, Guid categoryId)
    {
        var row = new Border
        {
            Style = (Style)FindResource("PromptRow"),
            Background = RowBackgroundFor(categoryId),
            Tag = prompt.Id,
            Cursor = Cursors.Hand,
        };

        // 6-column layout:
        //   [checkbox] [title (stretch)] [timestamp] [paste] [edit] [delete]
        // Klick auf Text/Zeile schaltet das gelbe Haekchen (Always-On) um —
        // einfuegen geschieht ausschliesslich ueber den Paste-Button neben
        // dem Stift. Damit ist Toggle vs. Insert eindeutig getrennt.
        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(22) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(24) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(24) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(24) });

        // ── Always-On checkbox (clickable; toggles persisted state) ──
        var checkbox = BuildAlwaysOnCheckbox(prompt);
        // The 3 action buttons (checkbox / ✎ / ✕) absorb their own right-click
        // events so the row-level right-click handler only fires for the label
        // area and the row background. Without this guard a right-click on
        // ✕ or ✎ would also pop the editor on top of the action.
        checkbox.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(checkbox, 0);
        grid.Children.Add(checkbox);

        // Split the stored short label into "Title" + "(Timestamp)" so we
        // can render them in their own cells with different sizes and
        // alignments. If there's no parenthesis at the end, the whole
        // label is treated as title.
        var (titleText, timestampText) = SplitLabel(prompt.ShortLabel);

        // Pre/Post-Prompt-Marker hinter dem Titel anzeigen — z.B.
        // "Branch ist feature/foo - vor" oder "Kurz halten - nach". Erkannt
        // wird das am Wrapper-Praefix im OriginalText, identisch zur Logik
        // im PromptEditDialog. Normale Prompts bleiben unveraendert.
        var prePostSuffix = DetectPrePostSuffix(prompt.OriginalText);
        if (prePostSuffix.Length > 0)
        {
            titleText += prePostSuffix;
        }

        // Hotkey-Hinweis. Wir haengen einen schlichten "(Strg+N)" hinter
        // den Titel — bewusst kein Glow / kein Highlight, damit das
        // Listing aufgeraeumt bleibt. Frank hatte explizit nach normaler
        // Schrift in Klammern gefragt.
        if (prompt.HotkeyNumber is int hk && hk >= 1 && hk <= 9)
        {
            titleText += $" (Strg+{hk})";
        }
        // Zusaetzlich der Win+Alt+Buchstabe-Hotkey wenn gesetzt. Beide
        // Klammern koennen gleichzeitig sichtbar sein, falls ein Prompt
        // sowohl eine Nummer als auch einen Buchstaben hat.
        if (prompt.HotkeyLetter is char hl && hl >= 'A' && hl <= 'Z')
        {
            titleText += $" (Win+Alt+{hl})";
        }

        // ── Title button (klick schaltet das gelbe Haekchen um) ──
        // Vorher: Klick auf den Titel hat den Prompt eingefuegt. Jetzt: Klick
        // auf den Titel toggelt den Always-On-Status — Einfuegen geht
        // ausschliesslich ueber den Paste-Button rechts neben dem Stift.
        var titleBtn = new Button
        {
            Content = titleText,
            Style = (Style)FindResource("PromptButton"),
            ToolTip = prompt.EffectiveText().Length > 500
                ? prompt.EffectiveText().Substring(0, 500) + "..."
                : prompt.EffectiveText(),
            FontSize = 13,
        };
        titleBtn.Click += async (_, _) => await ToggleAlwaysOnAsync(prompt);
        Grid.SetColumn(titleBtn, 1);
        grid.Children.Add(titleBtn);

        // ── Timestamp (small, dim, vertically centered next to icons) ──
        if (timestampText.Length > 0)
        {
            var tsLabel = new TextBlock
            {
                Text = timestampText,
                FontSize = 10,
                Foreground = BrushTimestampGray,
                VerticalAlignment = VerticalAlignment.Center,
                Margin = new Thickness(6, 0, 6, 0),
                IsHitTestVisible = false, // click should fall through to the row
            };
            Grid.SetColumn(tsLabel, 2);
            grid.Children.Add(tsLabel);
        }

        // ── Paste (Fluent E77F = Paste / Einfuegen) ──
        // Einziger Weg den Prompt in die CLI einzufuegen. Vom Stift getrennt
        // damit Toggle (Klick auf Text) und Insert (Klick auf Paste) sich
        // nicht in die Quere kommen.
        var pasteBtn = new Button
        {
            Content = "",
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "In Terminal einfuegen",
        };
        pasteBtn.Click += (_, _) => PromptInsertRequested?.Invoke(prompt.EffectiveText());
        pasteBtn.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(pasteBtn, 3);
        grid.Children.Add(pasteBtn);

        // ── Edit (Fluent E70F = Edit / Pencil) ──
        var editBtn = new Button
        {
            Content = "",
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Bearbeiten",
        };
        editBtn.Click += async (_, _) => await EditPromptAsync(prompt);
        editBtn.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(editBtn, 4);
        grid.Children.Add(editBtn);

        // ── Delete (Fluent E74D = Delete / Trash) ──
        var deleteBtn = new Button
        {
            Content = "",
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Loeschen",
            // Wider left gap than the other row icons (style default is 2px) so a
            // fast click can't accidentally hit delete instead of edit/insert.
            // Delete is destructive, so give it some breathing room.
            Margin = new Thickness(8, 0, 0, 0),
        };
        deleteBtn.Click += async (_, _) => await DeletePromptAsync(prompt);
        deleteBtn.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(deleteBtn, 5);
        grid.Children.Add(deleteBtn);

        row.Child = grid;

        // ── Whole-row click → toggle Always-On (gelbes Haekchen) ──
        // WPF Button.Click marks MouseLeftButtonUp as handled when the click
        // lands on a child Button, so the row-level handler only fires for
        // background clicks. No manual hit-test guard needed for left-click.
        // Klick auf den Hintergrund der Zeile schaltet das gelbe Haekchen um —
        // genauso wie ein Klick auf den Titel oder die Checkbox selbst.
        // Einfuegen geht ausschliesslich ueber den Paste-Button.
        row.MouseLeftButtonUp += async (_, e) =>
        {
            if (e.Handled) return;
            // If a drag was just kicked off, the LeftButtonUp comes after
            // DoDragDrop has already returned — _dragArmedRowId tracks that
            // case so we don't ALSO toggle on drop.
            if (_dragJustHappenedForRowId == prompt.Id)
            {
                _dragJustHappenedForRowId = null;
                return;
            }
            await ToggleAlwaysOnAsync(prompt);
        };

        // ── Drag source: PreviewMouseLeftButtonDown arms the drag, PreviewMouseMove
        // triggers DoDragDrop once the cursor moves past the threshold. We MUST
        // use the Preview (tunneling) variants here because a child WPF Button
        // (insert label, ✎, ✕, checkbox) absorbs MouseLeftButtonDown / MouseMove
        // by marking them Handled — so the bubbling-phase handler on the row
        // would never fire when the user grabs the row by its title text. ──
        row.PreviewMouseLeftButtonDown += (_, e) =>
        {
            _dragArmStartPoint = e.GetPosition(this);
            _dragArmedRowId    = prompt.Id;
        };
        row.PreviewMouseMove += (s, e) =>
        {
            if (e.LeftButton != System.Windows.Input.MouseButtonState.Pressed) return;
            if (_dragArmedRowId != prompt.Id) return;
            var current = e.GetPosition(this);
            // 6-pixel threshold (squared = 36): less than that is treated as
            // a click, more than that is a drag. Matches the WPF default
            // SystemParameters.MinimumHorizontalDragDistance ballpark.
            var dx = current.X - _dragArmStartPoint.X;
            var dy = current.Y - _dragArmStartPoint.Y;
            if (dx * dx + dy * dy < 36) return;

            _dragArmedRowId = null;
            _dragJustHappenedForRowId = prompt.Id;
            var sourceRow = (Border)s!;
            BeginDragVisual(sourceRow, current);
            try
            {
                var data = new DataObject(PromptDragFormat, prompt.Id.ToString());
                System.Windows.DragDrop.DoDragDrop(sourceRow, data, System.Windows.DragDropEffects.Move);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"DoDragDrop failed: {ex.Message}");
            }
            finally
            {
                EndDragVisual();
            }
        };

        // ── Whole-row right-click → context menu (Hotkey-Auswahl + Edit) ──
        // Frueher oeffnete der Rechtsklick direkt den Editor. Seit der
        // Win+Alt+<Buchstabe>-Erweiterung zeigt er stattdessen ein
        // Kontextmenue mit den Hotkey-Optionen UND der Edit-Option, damit
        // Frank die A-Z-Belegung schnell per Maus zuweisen kann ohne den
        // Edit-Dialog zu durchlaufen. WPF feuert das ContextMenu bei
        // Rechtsklick automatisch — wir brauchen daher keinen eigenen
        // MouseRightButtonUp-Handler mehr. Die 3 Action-Buttons absorbieren
        // ihre eigenen Rechtsklicks weiter oben (damit ein Rechtsklick auf
        // den Stift nicht zusaetzlich das ContextMenu oeffnet).
        // PreviewMouseRightButtonDown: PromptBoard zuerst nach vorne
        // holen, damit die Popups des ContextMenus nicht hinter anderen
        // Topmost-Fenstern landen.
        row.PreviewMouseRightButtonDown += (_, _) =>
        {
            try { Activate(); } catch { /* Activate kann fehlschlagen wenn die App keinen Input-Focus haben darf — egal, dann hilft ForcePopupTopmost. */ }
        };
        row.ContextMenu = BuildPromptContextMenu(prompt);

        // ── Drop-Target: Drag&Drop einer anderen Prompt-Zeile auf diese
        //    Zeile setzt die Reihenfolge neu. Position oben/unten am Cursor
        //    erkannt — Cursor in der oberen Haelfte = "darueber einfuegen",
        //    untere Haelfte = "darunter einfuegen". Funktioniert sowohl
        //    innerhalb derselben Kategorie (reine Sortierung) als auch
        //    kategorieuebergreifend (Move + Sortierung in einem Schritt).
        row.AllowDrop = true;
        row.DragOver  += (_, e) => OnPromptRowDragOver(row, prompt, e);
        row.DragLeave += (_, _) => HighlightRowDropTarget(row, false, false);
        row.Drop      += async (_, e) => await OnPromptDroppedOnRowAsync(prompt, row, e);

        return row;
    }

    /// <summary>
    /// DragOver-Handler fuer eine Prompt-Zeile. Setzt die Drop-Effekte und
    /// hebt die Zeile mit einer farbigen Linie oben oder unten hervor — je
    /// nachdem ob der Cursor in der oberen oder unteren Haelfte ist.
    /// Lehnt das Drop ab wenn Quell- und Ziel-Prompt aus verschiedenen
    /// Kategorien stammen — Reordering funktioniert nur INNERHALB einer
    /// Kategorie. Cross-Category-Move geht ueber den Kategorie-Tab.
    /// </summary>
    private void OnPromptRowDragOver(Border row, Prompt targetPrompt, System.Windows.DragEventArgs e)
    {
        if (!IsSameCategoryDrag(targetPrompt, e))
        {
            e.Effects = System.Windows.DragDropEffects.None;
            HighlightRowDropTarget(row, false, false);
            e.Handled = true;
            return;
        }
        e.Effects = System.Windows.DragDropEffects.Move;
        e.Handled = true;

        var pos = e.GetPosition(row);
        bool above = pos.Y < row.ActualHeight / 2.0;
        HighlightRowDropTarget(row, true, above);
    }

    /// <summary>
    /// Prueft ob ein laufender Drag von einer Prompt-Zeile auf das angegebene
    /// Ziel innerhalb derselben Kategorie stattfindet. Verlangt das richtige
    /// DataObject-Format UND eine in <see cref="_currentPrompts"/> auffindbare
    /// Quelle mit gleicher CategoryId wie das Ziel.
    /// </summary>
    private bool IsSameCategoryDrag(Prompt targetPrompt, System.Windows.DragEventArgs e)
    {
        if (!e.Data.GetDataPresent(PromptDragFormat)) return false;
        if (e.Data.GetData(PromptDragFormat) is not string idStr) return false;
        if (!Guid.TryParse(idStr, out var sourceId)) return false;
        var source = _currentPrompts.FirstOrDefault(p => p.Id == sourceId);
        if (source is null) return false;
        return source.CategoryId == targetPrompt.CategoryId;
    }

    /// <summary>
    /// Schaltet die Drop-Indikator-Linie an oder aus. Aktiv: goldene Linie
    /// oben (above=true) oder unten (above=false). Inaktiv: keine Linie.
    /// Nutzt BorderBrush+BorderThickness direkt am Row-Border — der
    /// PromptRow-Style setzt diese Properties NICHT, also kein Konflikt.
    /// </summary>
    private static void HighlightRowDropTarget(Border row, bool active, bool above)
    {
        if (!active)
        {
            row.BorderThickness = new Thickness(0);
            return;
        }
        row.BorderBrush = BrushGold;
        row.BorderThickness = above
            ? new Thickness(0, 2, 0, 0)
            : new Thickness(0, 0, 0, 2);
    }

    /// <summary>
    /// Drop-Handler fuer eine Prompt-Zeile. Bestimmt die Insert-Position
    /// (oben/unten zum Target), sortiert die SortOrder aller betroffenen
    /// Prompts in der Ziel-Kategorie neu und persistiert die Aenderungen.
    /// Bei kategorieuebergreifendem Drop wird zusaetzlich die CategoryId
    /// des Quell-Prompts auf die Ziel-Kategorie umgesetzt.
    /// </summary>
    private async Task OnPromptDroppedOnRowAsync(Prompt targetPrompt, Border row,
                                                 System.Windows.DragEventArgs e)
    {
        HighlightRowDropTarget(row, false, false);

        if (!e.Data.GetDataPresent(PromptDragFormat)) return;
        if (e.Data.GetData(PromptDragFormat) is not string idStr ||
            !Guid.TryParse(idStr, out var sourceId)) return;
        if (sourceId == targetPrompt.Id) return; // Drop auf sich selbst — No-Op

        // Quelle aus den aktuell sichtbaren Prompts holen — der Drag kann
        // nur von einer sichtbaren Zeile gestartet sein.
        var sourcePrompt = _currentPrompts.FirstOrDefault(p => p.Id == sourceId);
        if (sourcePrompt is null) return;

        // Cross-Kategorie-Drops sind hier verboten — Reordering nur INNERHALB
        // derselben Kategorie. Der Benutzer verschiebt Prompts zwischen
        // Kategorien indem er sie auf einen Kategorie-Tab zieht (anderer
        // Drop-Pfad: OnPromptDroppedOnCategoryAsync).
        if (sourcePrompt.CategoryId != targetPrompt.CategoryId) return;

        var pos = e.GetPosition(row);
        bool above = pos.Y < row.ActualHeight / 2.0;
        var targetCategoryId = targetPrompt.CategoryId;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

            // Alle Prompts der Ziel-Kategorie geordnet einsammeln.
            var prompts = (await repo.GetByCategoryAsync(targetCategoryId))
                .OrderBy(p => p.SortOrder)
                .ThenBy(p => p.ShortLabel, StringComparer.CurrentCultureIgnoreCase)
                .ToList();
            // Falls die Quelle schon in der Liste war (gleiche Kategorie),
            // entfernen — sie wird gleich an der neuen Position eingefuegt.
            prompts.RemoveAll(p => p.Id == sourceId);

            int targetIdx = prompts.FindIndex(p => p.Id == targetPrompt.Id);
            if (targetIdx < 0) targetIdx = prompts.Count;
            int insertIdx = above ? targetIdx : targetIdx + 1;
            if (insertIdx < 0) insertIdx = 0;
            if (insertIdx > prompts.Count) insertIdx = prompts.Count;

            // Quelle ist garantiert in derselben Kategorie (oben gecheckt) —
            // an der neuen Position einfuegen ohne CategoryId-Aenderung.
            prompts.Insert(insertIdx, sourcePrompt);

            // Lueckenlos neu nummerieren (0, 1, 2, ...) und alles schreiben.
            for (int i = 0; i < prompts.Count; i++)
            {
                if (prompts[i].SortOrder != i ||
                    (prompts[i].Id == sourceId && prompts[i].CategoryId == targetCategoryId))
                {
                    prompts[i].SortOrder = i;
                    await repo.UpdateAsync(prompts[i]);
                }
            }

            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Verschieben fehlgeschlagen: {ex.Message}",
                "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await RefreshAsync();
    }

    /// <summary>
    /// Builds a small clickable checkbox-style toggle. Yellow with a check
    /// when the prompt is always-on, dark when it isn't. Clicking persists
    /// the change and schedules an auto-backup.
    /// </summary>
    private Button BuildAlwaysOnCheckbox(Prompt prompt)
    {
        var checkbox = new Button
        {
            Width = 18,
            Height = 18,
            Content = prompt.IsAlwaysOn ? "✓" : "",
            Foreground = BrushDarkFg,
            FontSize = 12,
            FontWeight = FontWeights.Bold,
            Background = prompt.IsAlwaysOn ? BrushGold : BrushDarkBg,
            BorderBrush  = prompt.IsAlwaysOn ? BrushGold : BrushMutedBorder,
            BorderThickness = new Thickness(1.5),
            Padding = new Thickness(0),
            Cursor = Cursors.Hand,
            ToolTip = prompt.IsAlwaysOn
                ? "Immer aktiv — wird bei jedem Prompt dauerhaft eingefuegt. Klicken zum Deaktivieren."
                : "Anhaken, damit dieser Prompt bei jedem Insert dauerhaft mitgeschickt wird.",
            // Override the default WPF button chrome with a flat rectangle
            // template so the checkbox is small, square and reads as a checkbox.
            Template = (ControlTemplate)XamlReader_FlatTemplate(),
        };
        checkbox.Click += async (_, _) => await ToggleAlwaysOnAsync(prompt);
        return checkbox;
    }

    /// <summary>
    /// Reusable flat ControlTemplate for the always-on checkbox: just a
    /// rounded border with a centered content presenter — no WPF default
    /// chrome, hover highlight, or focus rectangle.
    /// </summary>
    private static object XamlReader_FlatTemplate()
    {
        const string xaml = @"
<ControlTemplate xmlns='http://schemas.microsoft.com/winfx/2006/xaml/presentation'
                 TargetType='Button'>
    <Border Background='{TemplateBinding Background}'
            BorderBrush='{TemplateBinding BorderBrush}'
            BorderThickness='{TemplateBinding BorderThickness}'
            CornerRadius='3'>
        <ContentPresenter HorizontalAlignment='Center' VerticalAlignment='Center'/>
    </Border>
</ControlTemplate>";
        using var sr = new System.IO.StringReader(xaml);
        using var xr = System.Xml.XmlReader.Create(sr);
        return System.Windows.Markup.XamlReader.Load(xr);
    }

    /// <summary>
    /// Walks up the visual tree from a click's OriginalSource to decide
    /// whether the click landed on (or inside) an interactive Button child
    /// of the row. Used to filter row-level right-clicks so the ✎/✕/checkbox
    /// keep their own behavior.
    /// </summary>
    private static bool IsOriginatedFromButton(object? originalSource)
    {
        if (originalSource is not DependencyObject node) return false;
        DependencyObject? d = node;
        while (d is not null)
        {
            if (d is Button) return true;
            d = VisualTreeHelper.GetParent(d) ?? (d is FrameworkElement fe ? fe.Parent : null);
        }
        return false;
    }

    private void RenderEmptyState(string message = "Noch keine Kategorien. Benutze + oben.")
    {
        PromptList.Children.Clear();
        PromptList.Children.Add(new TextBlock
        {
            Text = message,
            Foreground = BrushEmptyStateFg,
            FontSize = 12,
            TextWrapping = TextWrapping.Wrap,
            Margin = new Thickness(4, 8, 4, 0),
        });
    }

    // ──────────────── Editor actions: categories ────────────────

    private async Task AddCategoryAsync()
    {
        var name = TextInputDialog.Ask(this, "Neue Kategorie", "Name:");
        if (string.IsNullOrWhiteSpace(name)) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            var colorSvc = scope.ServiceProvider.GetRequiredService<IPastelColorGenerator>();
            int nextOrder = _categories.Count == 0 ? 0 : _categories.Max(c => c.SortOrder) + 1;

            var cat = new Category
            {
                Id = Guid.NewGuid(),
                Name = name,
                BackgroundColorHex = colorSvc.NextDistinctColor(_categories.Select(c => c.BackgroundColorHex)),
                SortOrder = nextOrder,
                Type = CategoryType.Standard,
            };
            await repo.AddAsync(cat);
            _activeCategoryIds.Add(cat.Id);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Kategorie konnte nicht angelegt werden: {ex.Message}",
                "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await RefreshAsync();
    }

    private async Task RenameCategoryAsync(Category cat)
    {
        var newName = TextInputDialog.Ask(this, "Kategorie umbenennen", "Neuer Name:", cat.Name);
        if (string.IsNullOrWhiteSpace(newName) || newName == cat.Name) return;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            cat.Name = newName;
            await repo.UpdateAsync(cat);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        await RefreshAsync();
    }

    private async Task DeleteCategoryAsync(Category cat)
    {
        if (!ConfirmDialog.Ask(this, "Kategorie loeschen?",
            $"Kategorie '{cat.Name}' wird mit allen enthaltenen Prompts geloescht.",
            "Loeschen")) return;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            await repo.DeleteAsync(cat.Id);
            _activeCategoryIds.Remove(cat.Id);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        await RefreshAsync();
    }

    // ──────────────── Editor actions: prompts ────────────────

    private async Task AddPromptAsync()
    {
        // New prompts land in the first active category. Fallback to the first
        // overall category if nothing is active, refuse if there is none yet.
        Guid? targetCatId = _activeCategoryIds.FirstOrDefault();
        if (targetCatId == Guid.Empty) targetCatId = _categories.FirstOrDefault()?.Id;
        if (targetCatId is null || targetCatId == Guid.Empty)
        {
            MessageBox.Show("Lege zuerst eine Kategorie an.", "PromptBoard",
                MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }

        var result = PromptEditDialog.Ask(
            this, "Neuer Prompt", string.Empty, string.Empty,
            alwaysOn: false, prePrompt: true, postPrompt: false, hotkey: null);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

            // Hotkey-Konflikt: Wenn die gewaehlte Strg+N schon einem anderen
            // Prompt gehoert, muss DESSEN Hotkey jetzt geloescht werden —
            // last-wins. Macht's der Benutzer absichtlich, sieht er beim
            // naechsten Editieren des alten Prompts die Vergabe wieder offen.
            if (result.HotkeyNumber is int newHk)
            {
                await StripHotkeyFromOthersAsync(repo, newHk, exceptId: Guid.Empty);
            }

            var prompt = new Prompt
            {
                Id = Guid.NewGuid(),
                CategoryId = targetCatId.Value,
                ShortLabel = result.ShortLabel,
                OriginalText = result.OriginalText,
                IsAlwaysOn = result.IsAlwaysOn,
                IsPrePrompt = result.IsPrePrompt,
                IsPostPrompt = result.IsPostPrompt,
                HotkeyNumber = result.HotkeyNumber,
                ActiveVersion = PromptVersion.Original,
                SortOrder = 0,
            };
            await repo.AddAsync(prompt);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        // RefreshAsync (nicht RenderPromptsAsync) — sonst zeigt der Render
        // den alten _allPromptsCache und der neue Prompt erscheint erst nach
        // dem naechsten Stern-Klick (Frank-Bug-Report 2026-05-09 23:35).
        await RefreshAsync();
    }

    private async Task EditPromptAsync(Prompt prompt)
    {
        var result = PromptEditDialog.Ask(
            this, "Prompt bearbeiten",
            prompt.ShortLabel, prompt.OriginalText, prompt.IsAlwaysOn,
            prompt.IsPrePrompt, prompt.IsPostPrompt, prompt.HotkeyNumber);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

            // Konfliktloesung — siehe AddPromptAsync. Eigenen Prompt ausnehmen
            // damit das wiederholte Speichern desselben Prompts mit demselben
            // Hotkey nicht erst sich selbst die Nummer wegnimmt.
            if (result.HotkeyNumber is int newHk && prompt.HotkeyNumber != newHk)
            {
                await StripHotkeyFromOthersAsync(repo, newHk, exceptId: prompt.Id);
            }

            prompt.ShortLabel = result.ShortLabel;
            prompt.OriginalText = result.OriginalText;
            prompt.IsAlwaysOn = result.IsAlwaysOn;
            prompt.IsPrePrompt = result.IsPrePrompt;
            prompt.IsPostPrompt = result.IsPostPrompt;
            prompt.HotkeyNumber = result.HotkeyNumber;
            await repo.UpdateAsync(prompt);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        // RefreshAsync — siehe AddPromptAsync. RenderPromptsAsync allein laesst
        // den Cache stehen und der bearbeitete Prompt zeigt nicht die neuen Werte.
        await RefreshAsync();
    }

    /// <summary>
    /// Implements the "last wins" rule for prompt hotkey assignments:
    /// any prompt other than <paramref name="exceptId"/> that currently
    /// holds <paramref name="hotkey"/> has its number cleared. Walks all
    /// categories because hotkeys are global (not per-category) and the
    /// repository has no direct "find by hotkey" query.
    ///
    /// Pass <see cref="Guid.Empty"/> as <paramref name="exceptId"/> when
    /// adding a brand-new prompt — there's no own row to skip yet.
    /// </summary>
    private static async Task StripHotkeyFromOthersAsync(
        IPromptRepository repo, int hotkey, Guid exceptId)
    {
        if (hotkey < 1 || hotkey > 9) return;

        // The set of prompts is small (tens, low hundreds), so loading
        // them all is cheaper than threading a custom query through the
        // repository interface that callers rarely need.
        var allPrompts = new List<Prompt>();
        // Reuse the same scope's repository — but we need every category,
        // not just the active ones. Pull the categories first.
        using (var scope = PromptBoardHost.Services.CreateScope())
        {
            var catRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            var cats = await catRepo.GetAllAsync();
            var localRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            foreach (var c in cats)
                allPrompts.AddRange(await localRepo.GetByCategoryAsync(c.Id));
        }

        foreach (var other in allPrompts)
        {
            if (other.HotkeyNumber != hotkey) continue;
            if (other.Id == exceptId) continue;
            other.HotkeyNumber = null;
            await repo.UpdateAsync(other);
        }
    }

    /// <summary>
    /// Analog zu <see cref="StripHotkeyFromOthersAsync"/> aber fuer den
    /// Buchstaben-Hotkey. Falls ein anderer Prompt bereits den gleichen
    /// Buchstaben hatte, wird er stillschweigend uebernommen (last-wins),
    /// damit die Eindeutigkeit erhalten bleibt.
    /// </summary>
    private static async Task StripHotkeyLetterFromOthersAsync(
        IPromptRepository repo, char letter, Guid exceptId)
    {
        char upper = char.ToUpperInvariant(letter);
        if (upper < 'A' || upper > 'Z') return;

        var allPrompts = new List<Prompt>();
        using (var scope = PromptBoardHost.Services.CreateScope())
        {
            var catRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            var cats = await catRepo.GetAllAsync();
            var localRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            foreach (var c in cats)
                allPrompts.AddRange(await localRepo.GetByCategoryAsync(c.Id));
        }

        foreach (var other in allPrompts)
        {
            if (other.HotkeyLetter is not char ol) continue;
            if (char.ToUpperInvariant(ol) != upper) continue;
            if (other.Id == exceptId) continue;
            other.HotkeyLetter = null;
            await repo.UpdateAsync(other);
        }
    }

    /// <summary>
    /// Weist dem Prompt einen Win+Alt+&lt;letter&gt;-Hotkey zu. Falls bereits
    /// ein anderer Prompt diesen Buchstaben hatte, wird dort die Bindung
    /// entfernt (last-wins, identisch zur Number-Variante).
    /// <paramref name="letter"/> = null entfernt die Bindung komplett.
    /// </summary>
    private async Task AssignHotkeyLetterAsync(Prompt prompt, char? letter)
    {
        try
        {
            char? upper = letter is char c ? char.ToUpperInvariant(c) : (char?)null;
            if (upper is char u && (u < 'A' || u > 'Z')) upper = null;

            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

            // Erst die anderen freistellen, dann unsere eigene Bindung setzen.
            // Reihenfolge wichtig: wenn der Benutzer sich selbst nochmal
            // den gleichen Buchstaben zuweist, soll exceptId verhindern dass
            // wir uns selber leer machen.
            if (upper is char up)
            {
                await StripHotkeyLetterFromOthersAsync(repo, up, prompt.Id);
            }

            prompt.HotkeyLetter = upper;
            await repo.UpdateAsync(prompt);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler beim Hotkey-Zuweisen",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await RefreshAsync();
    }

    /// <summary>
    /// Forciert das native Hosting-HWND eines Popups (ContextMenu oder
    /// Submenue) auf HWND_TOPMOST. Hintergrund: das PromptBoard-Fenster
    /// laeuft mit ShowActivated="False" und Topmost="True" — die
    /// Default-Popups erben das nicht zuverlaessig und landen hinter
    /// anderen Topmost-Fenstern (Voice-Overlay, andere Apps). Ohne diesen
    /// Workaround konnte Frank das A..Z-Untermenue nicht sehen weil es
    /// hinter dem Voice-Terminal-Overlay verschwand.
    /// </summary>
    private static void ForcePopupTopmost(System.Windows.Media.Visual? popupVisual)
    {
        if (popupVisual is null) return;
        if (PresentationSource.FromVisual(popupVisual) is not HwndSource hwndSource) return;
        if (hwndSource.Handle == IntPtr.Zero) return;
        NativeMethods.Win32.SetWindowPos(
            hwndSource.Handle,
            NativeMethods.Win32.HWND_TOPMOST,
            0, 0, 0, 0,
            NativeMethods.Win32.SWP_NOMOVE | NativeMethods.Win32.SWP_NOSIZE | NativeMethods.Win32.SWP_NOACTIVATE);
    }

    /// <summary>
    /// Erzeugt das ContextMenu fuer eine Prompt-Zeile (Rechtsklick).
    /// Inhalt: "Hotkey zuweisen" mit Untermenue A..Z, "Hotkey entfernen",
    /// Separator, "Bearbeiten", "Loeschen". Wird bei jedem Render neu
    /// aufgebaut damit "belegt durch Prompt X"-Marker aktuell bleiben.
    /// </summary>
    private ContextMenu BuildPromptContextMenu(Prompt prompt)
    {
        var menu = new ContextMenu();
        // Wenn das Top-Level-ContextMenu im Vordergrund poppt, schicken
        // wir sein natives Popup-HWND aktiv auf HWND_TOPMOST — siehe
        // ForcePopupTopmost-Kommentar.
        menu.Opened += (s, _) => ForcePopupTopmost(s as System.Windows.Media.Visual);

        // ── "Hotkey zuweisen" mit Untermenue A..Z ──
        var assignItem = new MenuItem { Header = "Hotkey zuweisen (Win+Alt+…)" };
        // Beim Aufklappen des A..Z-Untermenues liegt der Popup-Container
        // in einem EIGENEN HWND — also auch hier explizit TOPMOST setzen.
        // Wir greifen das Visual des ersten Sub-Items ab (es teilt sich
        // das Popup-HWND mit allen anderen Sub-Items) und schicken den
        // Befehl per Dispatcher.BeginInvoke an den naechsten Render-Tick,
        // weil das Popup im Moment des SubmenuOpened-Events noch nicht
        // gemessen / verbunden sein kann.
        assignItem.SubmenuOpened += (s, _) =>
        {
            if (s is not MenuItem mi || mi.Items.Count == 0) return;
            Dispatcher.BeginInvoke(new Action(() =>
            {
                var firstChild = mi.ItemContainerGenerator.ContainerFromIndex(0) as System.Windows.Media.Visual;
                ForcePopupTopmost(firstChild);
            }), System.Windows.Threading.DispatcherPriority.Loaded);
        };

        // Belegungs-Map vorberechnen: welcher Buchstabe gehoert welchem
        // Prompt? Wenn ein anderer Prompt den Buchstaben schon hat, zeigen
        // wir das im Submenue-Eintrag damit Frank sehen kann ob ein Klick
        // die Bindung "stiehlt". O(N) ueber den Cache, was bei ~hunderten
        // Prompts ohne Probleme passt.
        var byLetter = new Dictionary<char, Prompt>();
        foreach (var p in _allPromptsCache)
        {
            if (p.HotkeyLetter is char l)
            {
                char up = char.ToUpperInvariant(l);
                if (up >= 'A' && up <= 'Z' && !byLetter.ContainsKey(up))
                {
                    byLetter[up] = p;
                }
            }
        }

        char? currentLetter = prompt.HotkeyLetter is char cl
            ? char.ToUpperInvariant(cl)
            : (char?)null;

        for (char ch = 'A'; ch <= 'Z'; ch++)
        {
            bool ownedByOther = byLetter.TryGetValue(ch, out var ownerPrompt) && ownerPrompt.Id != prompt.Id;

            string header;
            if (ownedByOther)
            {
                // Belegt von einem anderen Prompt — Label kuerzen damit das
                // Submenue nicht ueberlaeuft. Eintrag wird unten disabled,
                // damit Frank sofort sieht "den kann ich nicht benutzen".
                string ownerLabel = ownerPrompt!.ShortLabel ?? "";
                if (ownerLabel.Length > 40) ownerLabel = ownerLabel.Substring(0, 40) + "…";
                header = $"Win+Alt+{ch}   (belegt von: {ownerLabel})";
            }
            else
            {
                header = $"Win+Alt+{ch}";
            }

            var item = new MenuItem
            {
                Header = header,
                IsCheckable = true,
                IsChecked = currentLetter == ch,
                // Ausgegraut sobald ein ANDERER Prompt den Buchstaben schon
                // belegt — verhindert versehentliches Stehlen. Der eigene
                // aktuell aktive Buchstabe bleibt klickbar, sonst koennte
                // man ihn nie wieder freigeben/wechseln. Freie Buchstaben
                // bleiben ebenfalls klickbar.
                IsEnabled = !ownedByOther,
            };
            // Lokale Kopie fuer den Closure — ohne diese Zeile wuerden alle
            // Klicks immer auf das letzte ch ('Z') zeigen.
            char captured = ch;
            item.Click += async (_, _) => await AssignHotkeyLetterAsync(prompt, captured);
            assignItem.Items.Add(item);
        }
        menu.Items.Add(assignItem);

        // ── "Hotkey entfernen" — nur enabled wenn aktuell einer gesetzt ist ──
        var removeItem = new MenuItem
        {
            Header = "Hotkey entfernen",
            IsEnabled = currentLetter is not null,
        };
        removeItem.Click += async (_, _) => await AssignHotkeyLetterAsync(prompt, null);
        menu.Items.Add(removeItem);

        menu.Items.Add(new Separator());

        // ── "Bearbeiten" — ersetzt das alte Direkt-Verhalten des Rechtsklicks ──
        var editItem = new MenuItem { Header = "Bearbeiten…" };
        editItem.Click += async (_, _) => await EditPromptAsync(prompt);
        menu.Items.Add(editItem);

        // ── "Loeschen" — analog zum Trash-Icon mit Bestaetigungsdialog ──
        var deleteItem = new MenuItem { Header = "Loeschen" };
        deleteItem.Click += async (_, _) => await DeletePromptAsync(prompt);
        menu.Items.Add(deleteItem);

        return menu;
    }

    private async Task DeletePromptAsync(Prompt prompt)
    {
        if (!ConfirmDialog.Ask(this, "Prompt loeschen?",
            $"Prompt '{prompt.ShortLabel}' wirklich loeschen?",
            "Loeschen")) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            await repo.DeleteAsync(prompt.Id);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        // RefreshAsync — siehe AddPromptAsync. Der geloeschte Prompt steht sonst
        // weiter im _allPromptsCache und ist erst nach Stern-Reopen weg.
        await RefreshAsync();
    }

    private async Task ToggleAlwaysOnAsync(Prompt prompt)
    {
        prompt.IsAlwaysOn = !prompt.IsAlwaysOn;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            await repo.UpdateAsync(prompt);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Toggle IsAlwaysOn failed: {ex.Message}");
        }
        // RefreshAsync — siehe AddPromptAsync. Toggle aendert IsAlwaysOn,
        // der Cache muss neu geladen werden damit die Filter-Logik den neuen
        // Wert sieht.
        await RefreshAsync();
    }

    // ──────────────── Settings ────────────────

    private async Task ShowSettingsAsync()
    {
        AppSettings current;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();
            current = await repo.GetAsync();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
            return;
        }

        var result = SettingsDialog.Ask(this, current);
        if (result is null) return;

        // Vokabular-Woerterbuch wurde im Dialog evtl. geaendert (in die SK-Datei
        // geschrieben) — Cloud-Upload anstossen, damit es auf alle Geraete kommt.
        VocabularySyncRequested?.Invoke();

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();
            // Google OAuth fields (ClientId, Secret, RefreshToken, AccountEmail)
            // are persisted by the SettingsDialog directly into the SK file,
            // so we only mirror the non-secret half back to the DB here.
            var latest = await repo.GetAsync();
            latest.GroqApiKey = result.GroqApiKey;
            latest.GeminiApiKey = result.GeminiApiKey;
            latest.SeparatorTemplate = result.SeparatorTemplate;
            latest.AutoHide = result.AutoHide;
            latest.Orientation = result.Orientation;
            await repo.UpdateAsync(latest);
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    // ──────────────── Auto-backup (debounced) ────────────────

    /// <summary>
    /// Schedules a Drive backup after a short debounce window. Many quick
    /// edits collapse into one upload. Does nothing if Drive isn't connected.
    /// Safe to call from any mutation path.
    /// </summary>
    private void ScheduleAutoBackup()
    {
        if (_autoBackupTimer is null)
        {
            _autoBackupTimer = new DispatcherTimer { Interval = AutoBackupDelay };
            _autoBackupTimer.Tick += async (_, _) =>
            {
                // Defensive: this is an async-void timer tick. Even though
                // RunAutoBackupIfConnectedAsync catches internally, Stop() or an
                // unexpected state (e.g. ObjectDisposedException while closing)
                // could still throw and take down the dispatcher.
                try
                {
                    _autoBackupTimer!.Stop();
                    await RunAutoBackupIfConnectedAsync();
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[PBPanel] auto-backup tick unhandled: {ex.Message}");
                }
            };
        }
        _autoBackupTimer.Stop();
        _autoBackupTimer.Start();
    }

    /// <summary>
    /// Silent upload — success and failure only land in the debug log,
    /// never in a dialog. Manual "G" upload from the backup menu still
    /// shows a confirmation message.
    /// </summary>
    private async Task RunAutoBackupIfConnectedAsync()
    {
        await _autoBackupGate.WaitAsync();
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();
            if (!await drive.IsAuthenticatedAsync())
            {
                Console.WriteLine("[PBPanel] auto-backup skipped (Drive not connected)");
                return;
            }
            var json = await BuildBackupJsonAsync();
            await drive.UploadAsync(json);
            Console.WriteLine("[PBPanel] auto-backup uploaded");
            RecordSuccessfulSync();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[PBPanel] auto-backup failed: {ex.Message}");
        }
        finally
        {
            _autoBackupGate.Release();
        }
    }

    // ──────────────── Sync badge persistence ────────────────

    /// <summary>
    /// Persists "now" as the last successful Drive backup time and refreshes
    /// the muted sync badge in the header.
    /// </summary>
    private void RecordSuccessfulSync()
    {
        var now = DateTime.UtcNow;
        _ = Task.Run(() =>
        {
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(LastSyncFilePath)!);
                File.WriteAllText(LastSyncFilePath,
                    now.ToString("o", CultureInfo.InvariantCulture));
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[PBPanel] write last-sync failed: {ex.Message}");
            }
        });
        RefreshSyncLabel(now);
    }

    /// <summary>
    /// Public wrapper fuer den Sync-Timestamp — wird vom OverlayWindow nach
    /// einem erfolgreichen Historie-Upload aufgerufen, damit der "· sync"-
    /// Badge im Header auch fuer Historie-Aktivitaet aktuell bleibt. Der
    /// Promtboard-Auto-Backup feuert nur bei Promtboard-Mutationen — ohne
    /// diesen Aufruf wuerde der Header-Timestamp Stunden alt bleiben,
    /// obwohl die Historie laufend gesynct wird.
    /// </summary>
    public void MarkSyncedNow()
    {
        if (Dispatcher.CheckAccess())
            RecordSuccessfulSync();
        else
            Dispatcher.BeginInvoke(new Action(RecordSuccessfulSync));
    }

    /// <summary>
    /// Reads the persisted last-sync timestamp and renders it as a short
    /// muted badge: "· sync 24.04. 22:39". Always shows date+time so freshness
    /// is obvious right after restart. Empty when no sync has happened yet.
    /// </summary>
    private void RefreshSyncLabel(DateTime? value = null)
    {
        var d = value ?? ReadLastSync();
        if (d is null) { SyncLabel.Text = ""; return; }
        var de = new CultureInfo("de-DE");
        SyncLabel.Text = "· sync " + d.Value.ToLocalTime().ToString("dd.MM. HH:mm", de);
    }

    private static DateTime? ReadLastSync()
    {
        try
        {
            if (!File.Exists(LastSyncFilePath)) return null;
            var text = File.ReadAllText(LastSyncFilePath).Trim();
            if (DateTime.TryParse(text, CultureInfo.InvariantCulture,
                                  DateTimeStyles.RoundtripKind, out var dt))
                return dt;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[PBPanel] read last-sync failed: {ex.Message}");
        }
        return null;
    }

    // ──────────────── Backup / Restore (manual) ────────────────

    private async Task ShowBackupMenuAsync()
    {
        var action = TextInputDialog.Ask(
            this,
            "Backup / Wiederherstellen",
            "E = Export Datei, I = Import Datei, G = Google Drive sichern, R = Google Drive laden:",
            "");
        if (string.IsNullOrEmpty(action)) return;

        switch (action.Trim().ToUpperInvariant())
        {
            case "E": await ExportAsync(); break;
            case "I": await ImportAsync(); break;
            case "G": await UploadToGoogleDriveAsync(); break;
            case "R": await RestoreFromGoogleDriveAsync(); break;
        }
    }

    private async Task UploadToGoogleDriveAsync()
    {
        try
        {
            string json = await BuildBackupJsonAsync();
            using var scope = PromptBoardHost.Services.CreateScope();
            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();

            if (!await drive.IsAuthenticatedAsync())
            {
                MessageBox.Show("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.",
                    "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            await drive.UploadAsync(json);
            RecordSuccessfulSync();
            var email = await drive.GetAccountEmailAsync();
            MessageBox.Show($"Backup bei Google Drive gespeichert ({email}).",
                "PromptBoard", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (GoogleDriveNotConfiguredException)
        {
            MessageBox.Show("Google Drive ist noch nicht eingerichtet. Bitte in den Einstellungen Client ID/Secret eintragen und 'Verbinden' klicken.",
                "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Google-Drive-Upload fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private async Task RestoreFromGoogleDriveAsync()
    {
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();

            if (!await drive.IsAuthenticatedAsync())
            {
                MessageBox.Show("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.",
                    "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            var json = await drive.DownloadLatestAsync();
            if (json is null)
            {
                MessageBox.Show("Kein Backup bei Google Drive gefunden.",
                    "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            if (!ConfirmDialog.Ask(this, "Google-Drive-Backup laden",
                "Lokale Eintraege mit gleicher ID werden ueberschrieben. Lokal vorhandene aber nicht im Backup enthaltene Eintraege werden geloescht.",
                "Einspielen")) return;

            await ApplyBackupJsonAsync(json);
            // Mark the remote ExportedAt as our local sync time so the launch
            // check doesn't immediately re-restore it next start.
            var remote = BackupExportedAtUtc(json);
            if (remote is not null) WriteLastSync(remote.Value);
            RefreshSyncLabel();
            MessageBox.Show("Google-Drive-Backup eingespielt.",
                "PromptBoard", MessageBoxButton.OK, MessageBoxImage.Information);
            await RefreshAsync();
        }
        catch (GoogleDriveNotConfiguredException)
        {
            MessageBox.Show("Google Drive ist noch nicht eingerichtet. Bitte in den Einstellungen Client ID/Secret eintragen und 'Verbinden' klicken.",
                "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Google-Drive-Restore fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    // ──────────────── Backup serialization ────────────────

    /// <summary>
    /// Returns a backup JSON identical in shape to the macOS one. Caller
    /// owns the lifecycle — we only serialize and return.
    /// </summary>
    public static async Task<string> BuildBackupJsonAsync()
    {
        using var scope = PromptBoardHost.Services.CreateScope();
        var catRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
        var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
        var settings = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();

        var cats = await catRepo.GetAllAsync();
        var allPrompts = new List<Prompt>();
        foreach (var c in cats)
            allPrompts.AddRange(await promptRepo.GetByCategoryAsync(c.Id));
        var appSettings = await settings.GetAsync();

        var backup = new BackupData
        {
            ExportedAt = DateTime.UtcNow,
            Categories = cats.Select(c => new BackupCategory
            {
                Id = c.Id, Name = c.Name, SortOrder = c.SortOrder,
                BackgroundColorHex = c.BackgroundColorHex, Type = (int)c.Type
            }).ToList(),
            Prompts = allPrompts.Select(p => new BackupPrompt
            {
                Id = p.Id, CategoryId = p.CategoryId,
                ShortLabel = p.ShortLabel, OriginalText = p.OriginalText,
                ImprovedText = p.ImprovedText, ActiveVersion = (int)p.ActiveVersion,
                IsAlwaysOn = p.IsAlwaysOn,
                IsPrePrompt = p.IsPrePrompt,
                IsPostPrompt = p.IsPostPrompt,
                SortOrder = p.SortOrder,
                HotkeyNumber = p.HotkeyNumber,
                HotkeyLetter = p.HotkeyLetter,
            }).ToList(),
            SeparatorTemplate = appSettings.SeparatorTemplate,
        };

        return JsonSerializer.Serialize(backup, new JsonSerializerOptions { WriteIndented = true });
    }

    /// <summary>
    /// Applies a backup JSON as the authoritative state of the local store.
    /// Upserts everything the backup contains AND deletes any local prompt or
    /// category whose id is NOT in the backup. Without the delete pass a row
    /// removed on another machine would silently re-appear after restore.
    /// Static so it can run at app launch before the panel is created.
    /// </summary>
    public static async Task ApplyBackupJsonAsync(string json)
    {
        var backup = JsonSerializer.Deserialize<BackupData>(json)
            ?? throw new InvalidOperationException("Backup-Datei konnte nicht gelesen werden.");

        using var scope = PromptBoardHost.Services.CreateScope();
        var catRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
        var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

        // Upsert categories from the backup.
        var existingCats = (await catRepo.GetAllAsync()).ToDictionary(c => c.Id);
        var remoteCategoryIds = new HashSet<Guid>();
        foreach (var c in backup.Categories)
        {
            remoteCategoryIds.Add(c.Id);
            var entity = new Category
            {
                Id = c.Id, Name = c.Name, SortOrder = c.SortOrder,
                BackgroundColorHex = c.BackgroundColorHex,
                Type = (CategoryType)c.Type,
            };
            if (existingCats.ContainsKey(c.Id))
                await catRepo.UpdateAsync(entity);
            else
                await catRepo.AddAsync(entity);
        }

        // Upsert prompts from the backup.
        var existingPromptIds = new Dictionary<Guid, Prompt>();
        foreach (var c in await catRepo.GetAllAsync())
        {
            var ps = await promptRepo.GetByCategoryAsync(c.Id);
            foreach (var p in ps) existingPromptIds[p.Id] = p;
        }
        var remotePromptIds = new HashSet<Guid>();
        foreach (var p in backup.Prompts)
        {
            remotePromptIds.Add(p.Id);
            var entity = new Prompt
            {
                Id = p.Id, CategoryId = p.CategoryId,
                ShortLabel = p.ShortLabel, OriginalText = p.OriginalText,
                ImprovedText = p.ImprovedText,
                ActiveVersion = (PromptVersion)p.ActiveVersion,
                IsAlwaysOn = p.IsAlwaysOn,
                // Older backups (pre-#1820) don't carry the Pre/Post
                // fields — BackupPrompt's defaults (Pre=true, Post=false)
                // give those rows the legacy "always-on means prefix"
                // behaviour automatically.
                IsPrePrompt = p.IsPrePrompt,
                IsPostPrompt = p.IsPostPrompt,
                SortOrder = p.SortOrder,
                HotkeyNumber = p.HotkeyNumber,
                HotkeyLetter = p.HotkeyLetter,
            };
            if (existingPromptIds.ContainsKey(p.Id))
                await promptRepo.UpdateAsync(entity);
            else
                await promptRepo.AddAsync(entity);
        }

        // Delete local rows that aren't in the authoritative backup. Prompts
        // first because they reference categories.
        foreach (var (id, _) in existingPromptIds)
        {
            if (!remotePromptIds.Contains(id))
            {
                try { await promptRepo.DeleteAsync(id); }
                catch (Exception ex) { Console.WriteLine($"[PBPanel] delete prompt {id} failed: {ex.Message}"); }
            }
        }
        foreach (var c in existingCats.Values)
        {
            if (!remoteCategoryIds.Contains(c.Id))
            {
                try { await catRepo.DeleteAsync(c.Id); }
                catch (Exception ex) { Console.WriteLine($"[PBPanel] delete category {c.Id} failed: {ex.Message}"); }
            }
        }
    }

    /// <summary>
    /// Returns the backup's <c>ExportedAt</c> field as UTC, or null if the
    /// JSON is missing it. Used by the launch-time auto-restore to decide
    /// whether the remote backup is newer than the local sync mark.
    /// </summary>
    public static DateTime? BackupExportedAtUtc(string json)
    {
        try
        {
            var d = JsonSerializer.Deserialize<BackupData>(json);
            if (d is null) return null;
            // ExportedAt is serialized as a UTC ISO string by JsonSerializer,
            // but if it round-tripped as Local we must CONVERT it (not just
            // re-label it): SpecifyKind keeps the clock value unchanged, so a
            // local timestamp tagged as UTC would shift the comparison and the
            // launch-time auto-restore could pick an OLDER remote backup as the
            // newer one. Convert local -> UTC, only label already-UTC/unspecified.
            return d.ExportedAt.Kind == DateTimeKind.Local
                ? d.ExportedAt.ToUniversalTime()
                : DateTime.SpecifyKind(d.ExportedAt, DateTimeKind.Utc);
        }
        catch { return null; }
    }

    public static void WriteLastSync(DateTime utc)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(LastSyncFilePath)!);
            File.WriteAllText(LastSyncFilePath,
                utc.ToUniversalTime().ToString("o", CultureInfo.InvariantCulture));
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[PBPanel] write last-sync failed: {ex.Message}");
        }
    }

    public static DateTime? ReadLastSyncUtc() => ReadLastSync()?.ToUniversalTime();

    private async Task ExportAsync()
    {
        var dlg = new SaveFileDialog
        {
            Title = "PromptBoard-Backup speichern",
            Filter = "JSON-Datei (*.json)|*.json",
            FileName = $"promptboard-backup-{DateTime.Now:yyyyMMdd-HHmm}.json",
        };
        if (dlg.ShowDialog(this) != true) return;

        try
        {
            string json = await BuildBackupJsonAsync();
            await File.WriteAllTextAsync(dlg.FileName, json);
            MessageBox.Show($"Backup gespeichert: {dlg.FileName}", "PromptBoard",
                MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Export fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private async Task ImportAsync()
    {
        var dlg = new OpenFileDialog
        {
            Title = "PromptBoard-Backup einlesen",
            Filter = "JSON-Datei (*.json)|*.json",
        };
        if (dlg.ShowDialog(this) != true) return;

        if (!ConfirmDialog.Ask(this, "Import bestaetigen",
            "Vorhandene Eintraege mit gleicher ID werden ueberschrieben. Lokal vorhandene aber nicht im Backup enthaltene Eintraege werden geloescht.",
            "Importieren")) return;

        try
        {
            string json = await File.ReadAllTextAsync(dlg.FileName);
            await ApplyBackupJsonAsync(json);
            MessageBox.Show("Import abgeschlossen.", "PromptBoard",
                MessageBoxButton.OK, MessageBoxImage.Information);
            await RefreshAsync();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Import fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    // ──────────────── Backup DTOs ────────────────

    private sealed class BackupData
    {
        public DateTime ExportedAt { get; set; }
        public List<BackupCategory> Categories { get; set; } = new();
        public List<BackupPrompt> Prompts { get; set; } = new();
        public string SeparatorTemplate { get; set; } = " ; ";
    }

    private sealed class BackupCategory
    {
        public Guid Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public int SortOrder { get; set; }
        public string BackgroundColorHex { get; set; } = "#DCEDEC";
        public int Type { get; set; }
    }

    private sealed class BackupPrompt
    {
        public Guid Id { get; set; }
        public Guid CategoryId { get; set; }
        public string ShortLabel { get; set; } = string.Empty;
        public string OriginalText { get; set; } = string.Empty;
        public string? ImprovedText { get; set; }
        public int ActiveVersion { get; set; }
        public bool IsAlwaysOn { get; set; }
        // Defaults match the macOS backup parser: a backup written by an
        // older client (pre-#1820) lacks these fields and JsonSerializer
        // leaves the property at its declared default — Pre=true matches
        // the legacy "always-on means prefix" behaviour, Post=false
        // disables the new suffix path.
        public bool IsPrePrompt { get; set; } = true;
        public bool IsPostPrompt { get; set; } = false;
        public int SortOrder { get; set; }
        // Strg+N hotkey (1-9), null when unbound. Older backups don't
        // carry this — JSON deserialization leaves the property at null,
        // exactly the same state as a freshly created prompt.
        public int? HotkeyNumber { get; set; }
        // Win+Alt+<letter> hotkey ('A'..'Z'), null when unbound. Same
        // backward-compatibility note as HotkeyNumber.
        public char? HotkeyLetter { get; set; }
    }
}

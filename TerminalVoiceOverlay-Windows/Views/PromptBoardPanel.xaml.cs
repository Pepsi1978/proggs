using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text.Json;
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
    public event Action<string>? PromptInsertRequested;

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
    public event Action<string>? InputSubmitRequested;

    /// <summary>
    /// Das angedockte Prompt-Eingabefenster. Existiert nur solange der Stern
    /// aktiv ist — beim Schliessen wird das Window komplett zerstoert, damit
    /// der naechste Start einen sauberen Zustand bekommt (Inhalt ist nicht
    /// persistent, Position kehrt zur Standard-Andockposition zurueck).
    /// </summary>
    private PromptInputWindow? _inputWindow;

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
    /// Pfad wie das OverlayWindow (LocalAppData\PromptBoard\history\), so
    /// dass jeder Append vom Submit-Pfad sofort durch ein Re-Render hier
    /// sichtbar wird.
    /// </summary>
    private readonly PromptHistoryService _historyService = new();

    // ── Right-click panel drag state ──
    private bool _isDraggingPanel;
    private System.Windows.Point _panelDragStartCursor;
    private double _panelDragStartLeft;
    private double _panelDragStartTop;

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

    public PromptBoardPanel()
    {
        InitializeComponent();
        BtnAddCategory.Click  += async (_, _) => await AddCategoryAsync();
        BtnAddPrompt.Click    += async (_, _) => await AddPromptAsync();
        BtnSettings.Click     += async (_, _) => await ShowSettingsAsync();
        BtnBackup.Click       += async (_, _) => await ShowBackupMenuAsync();
        BtnInputToggle.Click  += (_, _) => ToggleInputWindow();
        BtnHistory.Click      += (_, _) => ToggleHistoryWindow();
        RefreshSyncLabel();
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
        _isDraggingPanel        = true;
        _panelDragStartCursor   = PointToScreen(e.GetPosition(this));
        _panelDragStartLeft     = Left;
        _panelDragStartTop      = Top;
        CaptureMouse();
        e.Handled = true;
    }

    private void OnPanelMouseMove(object sender, System.Windows.Input.MouseEventArgs e)
    {
        if (!_isDraggingPanel) return;
        var cur = PointToScreen(e.GetPosition(this));
        Left = _panelDragStartLeft + (cur.X - _panelDragStartCursor.X);
        Top  = _panelDragStartTop  + (cur.Y - _panelDragStartCursor.Y);
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
    // Prompt-Eingabefenster (Stern-Toggle in der Toolbar)
    // ─────────────────────────────────────────────────────────────────────

    /// <summary>
    /// Toggle-Click auf den Stern in der Toolbar. Oeffnet das angedockte
    /// Eingabefenster bzw. schliesst es wieder. Der Stern wechselt Farbe:
    /// gold = aktiv, grau = inaktiv. Inhalt der Box wird beim Schliessen
    /// verworfen — das ist Absicht (laut Spec startet das Fenster bei
    /// jedem App-Neustart leer und der Stern ist aus).
    /// </summary>
    private void ToggleInputWindow()
    {
        if (_inputWindowVisible)
        {
            CloseInputWindow();
        }
        else
        {
            OpenInputWindow();
        }
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
            _inputWindow.SubmitRequested += text =>
            {
                // Submit-Logik kommt in Phase 3 — hier nur den Event nach
                // aussen reichen (das OverlayWindow baut Pre/Mitte/Post
                // zusammen und ruft den TerminalController auf).
                InputSubmitRequested?.Invoke(text);
                // Eingabefeld nach Senden leeren, Fokus bleibt drin.
                _inputWindow?.ClearInput();
            };
            // Rechtsklick-Drag im Eingabefenster verschiebt die GANZE Gruppe.
            _inputWindow.GroupDragDelta += OnChildGroupDrag;
            _inputWindow.Closed += (_, _) =>
            {
                _inputWindow = null;
                _inputWindowVisible = false;
                UpdateStarVisual();
            };
        }
        _inputWindow.DockTo(this, force: true);
        _inputWindow.Show();
        _inputWindowVisible = true;
        UpdateStarVisual();
    }

    private void CloseInputWindow()
    {
        if (_inputWindow is null)
        {
            _inputWindowVisible = false;
            UpdateStarVisual();
            return;
        }
        var win = _inputWindow;
        _inputWindow = null;
        _inputWindowVisible = false;
        UpdateStarVisual();
        win.Close();
    }

    private void UpdateStarVisual()
    {
        // Segoe Fluent Icons:  E735 = FavoriteStarFill,  E734 = FavoriteStar.
        // Beide haben dieselbe Glyph-Mitte → kein vertikaler Versatz beim
        // Wechsel zwischen aktivem und inaktivem Stern.
        if (_inputWindowVisible)
        {
            BtnInputToggle.Content    = ""; // FavoriteStarFill
            BtnInputToggle.Foreground = new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00));
            BtnInputToggle.ToolTip    = "Prompt-Eingabe ausblenden";
        }
        else
        {
            BtnInputToggle.Content    = ""; // FavoriteStar (Outline)
            // Weiss wie die anderen Toolbar-Symbole (Plus, Diskette,
            // Zahnrad, Schriftrolle) — im inaktiven Zustand soll der
            // Stern visuell gleichberechtigt zu den anderen sein.
            BtnInputToggle.Foreground = Brushes.White;
            BtnInputToggle.ToolTip    = "Prompt-Eingabe einblenden";
        }
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
        if (_historyWindowVisible)
        {
            CloseHistoryWindow();
        }
        else
        {
            await OpenHistoryWindowAsync();
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
            BtnHistory.Foreground = new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00));
            BtnHistory.ToolTip    = "Prompt-Historie schliessen";
        }
        else
        {
            BtnHistory.Foreground = new SolidColorBrush(Color.FromRgb(0xCC, 0xCC, 0xCC));
            BtnHistory.ToolTip    = "Prompt-Historie";
        }
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);
        var hwnd = new WindowInteropHelper(this).Handle;
        int exStyle = Win32.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
        Win32.SetWindowLong(hwnd, Win32.GWL_EXSTYLE, exStyle | Win32.WS_EX_NOACTIVATE | Win32.WS_EX_TOOLWINDOW);
    }

    public async Task RefreshAsync()
    {
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var categoryRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            _categories = (await categoryRepo.GetAllAsync())
                .OrderBy(c => c.SortOrder).ThenBy(c => c.Name)
                .ToList();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"PromptBoardPanel refresh failed: {ex.Message}");
            _categories = new List<Category>();
        }

        // Prune stale ids (a category deleted elsewhere) but keep every id the
        // user still has active.
        var known = _categories.Select(c => c.Id).ToHashSet();
        _activeCategoryIds.IntersectWith(known);
        // First-time / after-delete fallback: activate the first category so
        // the user isn't greeted with an empty list.
        if (_activeCategoryIds.Count == 0 && _categories.Count > 0)
        {
            _activeCategoryIds.Add(_categories[0].Id);
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
        int idx = _categories.FindIndex(c => c.Id == id);
        if (idx < 0) idx = 0;
        return CategoryPalette[idx % CategoryPalette.Length];
    }

    /// <summary>
    /// Dim but clearly tinted row background — keeps the dark-panel aesthetic
    /// so white prompt text stays legible, but lets you see each row's
    /// category at a glance from the tinted bar.
    /// </summary>
    private SolidColorBrush RowBackgroundFor(Guid categoryId)
    {
        var c = ColorForCategory(categoryId);
        // Blend ~30% category color over a dark base.
        byte r = (byte)((c.R * 0.30) + (0x25 * 0.70));
        byte g = (byte)((c.G * 0.30) + (0x25 * 0.70));
        byte b = (byte)((c.B * 0.30) + (0x25 * 0.70));
        return new SolidColorBrush(Color.FromRgb(r, g, b));
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
                // Toggle: clicking an active tab turns it off, clicking an
                // inactive one adds it. Multiple can be active simultaneously.
                if (_activeCategoryIds.Contains(cat.Id))
                    _activeCategoryIds.Remove(cat.Id);
                else
                    _activeCategoryIds.Add(cat.Id);
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
            btn.AllowDrop = true;
            btn.DragOver  += (_, e) => HighlightDropTarget(btn, catColor, e, true);
            btn.DragLeave += (_, _) => HighlightDropTarget(btn, catColor, null, false);
            btn.Drop      += async (_, e) => await OnPromptDroppedOnCategoryAsync(cat.Id, btn, catColor, e);

            CategoryTabs.Children.Add(btn);
        }
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
            // Only react if the drag actually carries a prompt id — otherwise
            // (e.g. a stray text drag) leave the tab alone.
            e.Effects = e.Data.GetDataPresent(PromptDragFormat)
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
                : new SolidColorBrush(Color.FromRgb(0x2D, 0x2D, 0x2D));
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
            RenderEmptyState("Keine Kategorie aktiv. Klick oben auf einen Tab.");
            return;
        }

        // Collect prompts from every active category, carrying the category id
        // along so we can tint each row accordingly.
        var combined = new List<(Prompt prompt, Guid catId)>();
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            foreach (var catId in _activeCategoryIds)
            {
                var prompts = await promptRepo.GetByCategoryAsync(catId);
                foreach (var p in prompts) combined.Add((p, catId));
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Prompt load failed: {ex.Message}");
            RenderEmptyState();
            return;
        }

        var sorted = combined
            .OrderBy(t => t.prompt.SortOrder)
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

    private Border BuildPromptRow(Prompt prompt, Guid categoryId)
    {
        var row = new Border
        {
            Style = (Style)FindResource("PromptRow"),
            Background = RowBackgroundFor(categoryId),
            Tag = prompt.Id,
            Cursor = Cursors.Hand,
        };

        // 5-column layout:
        //   [checkbox] [title (stretch)] [timestamp] [edit] [delete]
        // Title and timestamp live in separate cells so the timestamp
        // can sit right next to the action buttons (✎/✕) instead of
        // hugging the title — and so it gets a proper VerticalAlignment
        // that lines up with the icons regardless of font metrics.
        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(22) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
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

        // ── Insert label (clickable button — title only) ──
        var insertBtn = new Button
        {
            Content = titleText,
            Style = (Style)FindResource("PromptButton"),
            ToolTip = prompt.EffectiveText().Length > 500
                ? prompt.EffectiveText().Substring(0, 500) + "..."
                : prompt.EffectiveText(),
            FontSize = 13,
        };
        insertBtn.Click += (_, _) => PromptInsertRequested?.Invoke(prompt.EffectiveText());
        Grid.SetColumn(insertBtn, 1);
        grid.Children.Add(insertBtn);

        // ── Timestamp (small, dim, vertically centered next to icons) ──
        if (timestampText.Length > 0)
        {
            var tsLabel = new TextBlock
            {
                Text = timestampText,
                FontSize = 10,
                Foreground = new SolidColorBrush(Color.FromRgb(0xAA, 0xAA, 0xAA)),
                VerticalAlignment = VerticalAlignment.Center,
                Margin = new Thickness(6, 0, 6, 0),
                IsHitTestVisible = false, // click should fall through to the row
            };
            Grid.SetColumn(tsLabel, 2);
            grid.Children.Add(tsLabel);
        }

        // ── Edit (Fluent E70F = Edit / Pencil) ──
        var editBtn = new Button
        {
            Content = "",
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Bearbeiten",
        };
        editBtn.Click += async (_, _) => await EditPromptAsync(prompt);
        editBtn.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(editBtn, 3);
        grid.Children.Add(editBtn);

        // ── Delete (Fluent E74D = Delete / Trash) ──
        var deleteBtn = new Button
        {
            Content = "",
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Loeschen",
        };
        deleteBtn.Click += async (_, _) => await DeletePromptAsync(prompt);
        deleteBtn.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(deleteBtn, 4);
        grid.Children.Add(deleteBtn);

        row.Child = grid;

        // ── Whole-row click → insert (matching macOS row gesture) ──
        // WPF Button.Click marks MouseLeftButtonUp as handled when the click
        // lands on a child Button, so the row-level handler only fires for
        // background clicks. No manual hit-test guard needed for left-click.
        row.MouseLeftButtonUp += (_, e) =>
        {
            if (e.Handled) return;
            // If a drag was just kicked off, the LeftButtonUp comes after
            // DoDragDrop has already returned — _dragArmedRowId tracks that
            // case so we don't ALSO insert the prompt on drop.
            if (_dragJustHappenedForRowId == prompt.Id)
            {
                _dragJustHappenedForRowId = null;
                return;
            }
            PromptInsertRequested?.Invoke(prompt.EffectiveText());
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

        // ── Whole-row right-click → open editor (same effect as ✎) ──
        // Right-click is NOT consumed by WPF Buttons by default. Instead of
        // walking the visual tree (which would also exclude the insert label
        // button — wrong), the 3 action buttons absorb their own right-click
        // above. Anything that bubbles up here is a click on the label or
        // the row background, both of which should open the editor.
        row.MouseRightButtonUp += async (_, e) =>
        {
            if (e.Handled) return;
            e.Handled = true;
            await EditPromptAsync(prompt);
        };

        return row;
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
            Foreground = new SolidColorBrush(Color.FromRgb(0x1F, 0x1F, 0x1F)),
            FontSize = 12,
            FontWeight = FontWeights.Bold,
            Background = prompt.IsAlwaysOn
                ? new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00))
                : new SolidColorBrush(Color.FromRgb(0x2D, 0x2D, 0x2D)),
            BorderBrush = prompt.IsAlwaysOn
                ? new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00))
                : new SolidColorBrush(Color.FromRgb(0x8C, 0x8C, 0x8C)),
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
            Foreground = new SolidColorBrush(Color.FromRgb(0x9A, 0x9A, 0x9A)),
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
            alwaysOn: false, prePrompt: true, postPrompt: false);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            var prompt = new Prompt
            {
                Id = Guid.NewGuid(),
                CategoryId = targetCatId.Value,
                ShortLabel = result.ShortLabel,
                OriginalText = result.OriginalText,
                IsAlwaysOn = result.IsAlwaysOn,
                IsPrePrompt = result.IsPrePrompt,
                IsPostPrompt = result.IsPostPrompt,
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

        await RenderPromptsAsync();
    }

    private async Task EditPromptAsync(Prompt prompt)
    {
        var result = PromptEditDialog.Ask(
            this, "Prompt bearbeiten",
            prompt.ShortLabel, prompt.OriginalText, prompt.IsAlwaysOn,
            prompt.IsPrePrompt, prompt.IsPostPrompt);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            prompt.ShortLabel = result.ShortLabel;
            prompt.OriginalText = result.OriginalText;
            prompt.IsAlwaysOn = result.IsAlwaysOn;
            prompt.IsPrePrompt = result.IsPrePrompt;
            prompt.IsPostPrompt = result.IsPostPrompt;
            await repo.UpdateAsync(prompt);
            ScheduleAutoBackup();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await RenderPromptsAsync();
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

        await RenderPromptsAsync();
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
        await RenderPromptsAsync();
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
                _autoBackupTimer!.Stop();
                await RunAutoBackupIfConnectedAsync();
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
    }

    // ──────────────── Sync badge persistence ────────────────

    /// <summary>
    /// Persists "now" as the last successful Drive backup time and refreshes
    /// the muted sync badge in the header.
    /// </summary>
    private void RecordSuccessfulSync()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(LastSyncFilePath)!);
            File.WriteAllText(LastSyncFilePath,
                DateTime.UtcNow.ToString("o", CultureInfo.InvariantCulture));
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[PBPanel] write last-sync failed: {ex.Message}");
        }
        RefreshSyncLabel();
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
        Dispatcher.Invoke(RecordSuccessfulSync);
    }

    /// <summary>
    /// Reads the persisted last-sync timestamp and renders it as a short
    /// muted badge: "· sync 24.04. 22:39". Always shows date+time so freshness
    /// is obvious right after restart. Empty when no sync has happened yet.
    /// </summary>
    private void RefreshSyncLabel()
    {
        var d = ReadLastSync();
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
            // but if it round-tripped as Local we still treat it as UTC.
            return DateTime.SpecifyKind(d.ExportedAt, DateTimeKind.Utc);
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
    }
}

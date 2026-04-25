using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
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
        RefreshSyncLabel();
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
            btn.AllowDrop = true;
            btn.DragEnter += (_, e) => HighlightDropTarget(btn, catColor, e, true);
            btn.DragLeave += (_, _) => HighlightDropTarget(btn, catColor, null, false);
            btn.Drop += async (_, e) => await OnPromptDroppedOnCategoryAsync(cat.Id, btn, catColor, e);

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

        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(22) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
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

        // ── Insert label (clickable button — same as before) ──
        var insertBtn = new Button
        {
            Content = prompt.ShortLabel,
            Style = (Style)FindResource("PromptButton"),
            ToolTip = prompt.EffectiveText().Length > 500
                ? prompt.EffectiveText().Substring(0, 500) + "..."
                : prompt.EffectiveText(),
        };
        insertBtn.Click += (_, _) => PromptInsertRequested?.Invoke(prompt.EffectiveText());
        Grid.SetColumn(insertBtn, 1);
        grid.Children.Add(insertBtn);

        // ── Edit (✎) ──
        var editBtn = new Button
        {
            Content = "✎",
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Bearbeiten",
        };
        editBtn.Click += async (_, _) => await EditPromptAsync(prompt);
        editBtn.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(editBtn, 2);
        grid.Children.Add(editBtn);

        // ── Delete (✕) ──
        var deleteBtn = new Button
        {
            Content = "✕",
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Loeschen",
        };
        deleteBtn.Click += async (_, _) => await DeletePromptAsync(prompt);
        deleteBtn.MouseRightButtonUp += (_, e) => e.Handled = true;
        Grid.SetColumn(deleteBtn, 3);
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

        // ── Drag source: mousedown arms, threshold-move triggers DoDragDrop ──
        row.MouseLeftButtonDown += (_, e) =>
        {
            _dragArmStartPoint = e.GetPosition(this);
            _dragArmedRowId    = prompt.Id;
        };
        row.MouseMove += (s, e) =>
        {
            if (e.LeftButton != System.Windows.Input.MouseButtonState.Pressed) return;
            if (_dragArmedRowId != prompt.Id) return;
            var current = e.GetPosition(this);
            // Only start a drag once the user has moved far enough that they
            // clearly meant "drag", not "click to insert". 6 pixels matches
            // the WPF default SystemParameters.MinimumHorizontal/VerticalDragDistance
            // ballpark and feels right on a 380-pixel-wide panel.
            var dx = current.X - _dragArmStartPoint.X;
            var dy = current.Y - _dragArmStartPoint.Y;
            if (dx * dx + dy * dy < 36) return;

            _dragArmedRowId = null;
            _dragJustHappenedForRowId = prompt.Id;
            try
            {
                var data = new DataObject(PromptDragFormat, prompt.Id.ToString());
                System.Windows.DragDrop.DoDragDrop((Border)s!, data, System.Windows.DragDropEffects.Move);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"DoDragDrop failed: {ex.Message}");
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

        var result = PromptEditDialog.Ask(this, "Neuer Prompt", string.Empty, string.Empty, false);
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
            prompt.ShortLabel, prompt.OriginalText, prompt.IsAlwaysOn);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            prompt.ShortLabel = result.ShortLabel;
            prompt.OriginalText = result.OriginalText;
            prompt.IsAlwaysOn = result.IsAlwaysOn;
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
            // Re-fetch so we keep any refresh-token/email written during Connect.
            var latest = await repo.GetAsync();
            latest.GroqApiKey = result.GroqApiKey;
            latest.GeminiApiKey = result.GeminiApiKey;
            latest.SeparatorTemplate = result.SeparatorTemplate;
            latest.GoogleClientId = result.GoogleClientId;
            latest.GoogleClientSecret = result.GoogleClientSecret;
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
                IsAlwaysOn = p.IsAlwaysOn, SortOrder = p.SortOrder,
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
                IsAlwaysOn = p.IsAlwaysOn, SortOrder = p.SortOrder,
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
        public int SortOrder { get; set; }
    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using Microsoft.Extensions.DependencyInjection;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using TerminalVoiceOverlay.NativeMethods;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// Side panel that renders the PromptBoard categories and their prompts
/// as clickable buttons. A click on a prompt inserts it directly into
/// the focused terminal — identical pipeline to voice dictation, just
/// without the voice part. Window mirrors the pillar's WS_EX_NOACTIVATE
/// so focus stays in the terminal while the user browses.
/// </summary>
public partial class PromptBoardPanel : Window
{
    /// <summary>Invoked when the user clicks a prompt row. The host is
    /// responsible for injecting the effective text into the active
    /// terminal (same code path as voice dictation).</summary>
    public event Action<string>? PromptInsertRequested;

    private Guid? _activeCategoryId;
    private List<Category> _categories = new();

    public PromptBoardPanel()
    {
        InitializeComponent();
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);

        // Keep focus in the terminal when the user clicks inside the panel.
        var hwnd = new WindowInteropHelper(this).Handle;
        int exStyle = Win32.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
        Win32.SetWindowLong(hwnd, Win32.GWL_EXSTYLE, exStyle | Win32.WS_EX_NOACTIVATE | Win32.WS_EX_TOOLWINDOW);
    }

    /// <summary>Reload categories and prompts from the shared PromptBoard DB.</summary>
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

        RenderCategoryTabs();

        if (_categories.Count == 0)
        {
            _activeCategoryId = null;
            RenderEmptyState();
            return;
        }

        if (_activeCategoryId is null || !_categories.Any(c => c.Id == _activeCategoryId))
        {
            _activeCategoryId = _categories[0].Id;
        }

        await LoadPromptsForActiveCategoryAsync();
    }

    private void RenderCategoryTabs()
    {
        CategoryTabs.Children.Clear();
        foreach (var cat in _categories)
        {
            var btn = new Button
            {
                Content = cat.Name,
                Tag = cat.Id,
                Style = (Style)FindResource(
                    cat.Id == _activeCategoryId ? "CategoryTabActive" : "CategoryTab"),
            };
            btn.Click += async (_, _) =>
            {
                _activeCategoryId = (Guid)btn.Tag;
                RenderCategoryTabs();
                await LoadPromptsForActiveCategoryAsync();
            };
            CategoryTabs.Children.Add(btn);
        }
    }

    private async Task LoadPromptsForActiveCategoryAsync()
    {
        if (_activeCategoryId is null)
        {
            RenderEmptyState();
            return;
        }

        IReadOnlyList<Prompt> prompts;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            prompts = await promptRepo.GetByCategoryAsync(_activeCategoryId.Value);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Prompt load failed: {ex.Message}");
            RenderEmptyState();
            return;
        }

        PromptList.Children.Clear();

        if (prompts.Count == 0)
        {
            RenderEmptyState("Keine Prompts in dieser Kategorie.");
            return;
        }

        foreach (var p in prompts.OrderBy(x => x.SortOrder).ThenBy(x => x.ShortLabel))
        {
            PromptList.Children.Add(BuildPromptRow(p));
        }
    }

    private Border BuildPromptRow(Prompt prompt)
    {
        var row = new Border { Style = (Style)FindResource("PromptRow") };

        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(20) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

        var dot = PromptBoardPanelHelpers.BuildAOIndicator(prompt.IsAlwaysOn);
        Grid.SetColumn(dot, 0);
        grid.Children.Add(dot);

        var btn = new Button
        {
            Content = prompt.ShortLabel,
            Style = (Style)FindResource("PromptButton"),
            ToolTip = prompt.EffectiveText().Length > 500
                ? prompt.EffectiveText().Substring(0, 500) + "..."
                : prompt.EffectiveText(),
        };
        btn.Click += (_, _) => PromptInsertRequested?.Invoke(prompt.EffectiveText());
        Grid.SetColumn(btn, 1);
        grid.Children.Add(btn);

        row.Child = grid;
        return row;
    }

    private void RenderEmptyState(string message = "Noch keine Kategorien. Lege welche in PromptBoard an.")
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
}

internal static class PromptBoardPanelHelpers
{
    /// <summary>Small gold dot shown next to prompts that are marked IsAlwaysOn.</summary>
    public static System.Windows.Shapes.Ellipse BuildAOIndicator(bool isAlwaysOn) => new()
    {
        Width = 8,
        Height = 8,
        VerticalAlignment = VerticalAlignment.Center,
        HorizontalAlignment = HorizontalAlignment.Center,
        Fill = isAlwaysOn
            ? new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00))   // gold
            : new SolidColorBrush(Color.FromRgb(0x3A, 0x3A, 0x3A)),  // dark grey
    };
}

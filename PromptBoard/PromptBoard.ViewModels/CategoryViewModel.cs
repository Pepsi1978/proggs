using System;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.Linq;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;

namespace PromptBoard.ViewModels;

/// <summary>
/// ViewModel for one column in the bar (a category and its prompts).
/// Phase 3: IsProject/IsInline flags drive the two rendering paths
/// (inline tile vs. flyout-only header).
/// Phase 5: creating a prompt in an AiLibrary category auto-activates
/// it as the current Gemini meta-prompt (enforces "exactly one active").
/// </summary>
public partial class CategoryViewModel : ObservableObject
{
    private readonly ICategoryRepository _categories;
    private readonly IPromptRepository _prompts;
    private readonly IAiImprovementPromptRepository _aiImprovementRepo;
    private readonly IDialogService _dialogs;
    private readonly IInsertOrchestrator _insertOrchestrator;
    private readonly ILoggerFactory _loggerFactory;
    private readonly ILogger<CategoryViewModel> _logger;

    public Guid Id { get; }

    [ObservableProperty]
    private string _name;

    [ObservableProperty]
    private string _backgroundColorHex;

    [ObservableProperty]
    private CategoryType _type;

    [ObservableProperty]
    private int _sortOrder;

    public ObservableCollection<PromptViewModel> Prompts { get; } = [];

    /// <summary>Controls whether the whole category tile/flyout renders. Flipped by
    /// the search filter; default true.</summary>
    [ObservableProperty]
    private bool _isVisible = true;

    public Func<CategoryViewModel, Task>? OnDeleteRequested { get; set; }

    /// <summary>
    /// Invoked after a successful delete so the MainViewModel can publish
    /// an UndoAction to the snackbar. May be null in unit tests.
    /// </summary>
    public Action<UndoAction>? OnUndoRequested { get; set; }

    public CategoryViewModel(
        Category category,
        ICategoryRepository categories,
        IPromptRepository prompts,
        IAiImprovementPromptRepository aiImprovementRepo,
        IDialogService dialogs,
        IInsertOrchestrator insertOrchestrator,
        ILoggerFactory loggerFactory)
    {
        ArgumentNullException.ThrowIfNull(category);
        _categories = categories;
        _prompts = prompts;
        _aiImprovementRepo = aiImprovementRepo;
        _dialogs = dialogs;
        _insertOrchestrator = insertOrchestrator;
        _loggerFactory = loggerFactory;
        _logger = loggerFactory.CreateLogger<CategoryViewModel>();

        Id = category.Id;
        _name = category.Name;
        _backgroundColorHex = category.BackgroundColorHex;
        _type = category.Type;
        _sortOrder = category.SortOrder;

        foreach (Prompt p in category.Prompts.OrderBy(p => p.SortOrder))
        {
            AttachPrompt(p);
        }

        Prompts.CollectionChanged += OnPromptsCollectionChanged;
    }

    private async void OnPromptsCollectionChanged(object? sender, NotifyCollectionChangedEventArgs e)
    {
        if (e.Action != NotifyCollectionChangedAction.Move) return;
        try
        {
            for (int i = 0; i < Prompts.Count; i++)
            {
                if (Prompts[i].SortOrder != i)
                {
                    Prompts[i].SortOrder = i;
                    await Prompts[i].PersistAsync();
                }
            }
            _logger.LogInformation("Prompts reordered in category '{Name}'.", Name);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to persist prompt reorder in category '{Name}'.", Name);
        }
    }

    public bool IsProject => Type == CategoryType.Project;
    public bool IsInline => Type != CategoryType.Project;

    /// <summary>
    /// Applies a search query: shows the category if its name matches or any of
    /// its prompts match; individual prompts are also hidden if the category matches
    /// only via its name (so the user still sees the full category content in that case).
    /// </summary>
    public void ApplyFilter(string query)
    {
        if (string.IsNullOrWhiteSpace(query))
        {
            IsVisible = true;
            foreach (PromptViewModel p in Prompts)
            {
                p.IsVisible = true;
            }
            return;
        }

        bool nameHit = Name.Contains(query, StringComparison.OrdinalIgnoreCase);
        bool anyPromptHit = false;
        foreach (PromptViewModel p in Prompts)
        {
            bool promptHit = p.MatchesQuery(query);
            // When the category name matches, show ALL prompts so the user sees
            // the full category. Otherwise only show the matching prompts.
            p.IsVisible = nameHit || promptHit;
            anyPromptHit = anyPromptHit || promptHit;
        }
        IsVisible = nameHit || anyPromptHit;
    }

    [RelayCommand]
    private async Task RenameAsync()
    {
        string? updated = await _dialogs.ShowRenameAsync("Kategorie umbenennen", Name);
        if (string.IsNullOrWhiteSpace(updated) || updated == Name)
        {
            return;
        }

        Name = updated.Trim();
        await PersistAsync();
    }

    [RelayCommand]
    private async Task DeleteAsync()
    {
        bool confirmed = await _dialogs.ShowConfirmAsync(
            "Kategorie loeschen?",
            $"Soll die Kategorie \"{Name}\" mit allen enthaltenen Prompts wirklich geloescht werden?");
        if (!confirmed)
        {
            return;
        }

        // Snapshot BEFORE delete so Undo can rebuild the whole category.
        Category snapshot = ToEntity();
        var promptSnapshots = Prompts.Select(p => p.ToEntity()).ToList();

        await _categories.DeleteAsync(Id);
        if (OnDeleteRequested is not null)
        {
            await OnDeleteRequested(this);
        }

        OnUndoRequested?.Invoke(new UndoAction
        {
            Kind = UndoActionKind.DeletedCategory,
            Message = $"Kategorie \"{snapshot.Name}\" geloescht.",
            Category = snapshot,
            Prompts = promptSnapshots,
        });
    }

    [RelayCommand]
    private async Task AddPromptAsync()
    {
        bool isAiLibrary = Type == CategoryType.AiLibrary;

        var request = new PromptEditorRequest(
            ShortLabel: "Neuer Prompt",
            OriginalText: string.Empty,
            ImprovedText: null,
            ActiveVersion: PromptVersion.Original,
            IsAiImprovementPrompt: isAiLibrary);

        var result = await _dialogs.ShowPromptEditorAsync(request);
        if (result is null || string.IsNullOrWhiteSpace(result.ShortLabel))
        {
            return;
        }

        Prompt prompt = isAiLibrary
            ? new AiImprovementPrompt
            {
                CategoryId = Id,
                ShortLabel = result.ShortLabel.Trim(),
                OriginalText = result.OriginalText,
                ImprovedText = result.ImprovedText,
                ActiveVersion = result.ActiveVersion,
                SortOrder = Prompts.Count,
                GeminiModel = "gemini-2.5-flash",
                IsActiveForImprovement = false, // set below via repo to enforce exclusivity
            }
            : new Prompt
            {
                CategoryId = Id,
                ShortLabel = result.ShortLabel.Trim(),
                OriginalText = result.OriginalText,
                ImprovedText = result.ImprovedText,
                ActiveVersion = result.ActiveVersion,
                SortOrder = Prompts.Count,
            };

        await _prompts.AddAsync(prompt);

        if (isAiLibrary && prompt is AiImprovementPrompt)
        {
            // Auto-activate the newly-created meta-prompt; repo also
            // deactivates any previously active one in a single tx.
            await _aiImprovementRepo.SetActiveAsync(prompt.Id);
            if (prompt is AiImprovementPrompt ai)
            {
                ai.IsActiveForImprovement = true;
            }
        }

        AttachPrompt(prompt);
        _logger.LogInformation(
            "Prompt added: {Label} in {Category}{Active}",
            prompt.ShortLabel, Name, isAiLibrary ? " (auto-activated as meta-prompt)" : "");
    }

    private void AttachPrompt(Prompt prompt)
    {
        var vm = new PromptViewModel(
            prompt,
            _prompts,
            _dialogs,
            _insertOrchestrator,
            _loggerFactory.CreateLogger<PromptViewModel>())
        {
            OnDeleteRequested = RemovePromptAsync,
            OnUndoRequested = OnUndoRequested,
        };
        Prompts.Add(vm);
    }

    /// <summary>Called from MainViewModel.RestoreUndoAsync when a single prompt is brought back.</summary>
    public void AttachRestoredPrompt(Prompt prompt) => AttachPrompt(prompt);

    private Task RemovePromptAsync(PromptViewModel vm)
    {
        Prompts.Remove(vm);
        return Task.CompletedTask;
    }

    internal Task PersistAsync() => _categories.UpdateAsync(ToEntity());

    internal Category ToEntity() => new()
    {
        Id = Id,
        Name = Name,
        BackgroundColorHex = BackgroundColorHex,
        Type = Type,
        SortOrder = SortOrder,
    };
}

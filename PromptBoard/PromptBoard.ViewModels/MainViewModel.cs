using System;
using System.Collections.Generic;
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
/// Root ViewModel for the main bar. Holds all categories grouped into
/// three horizontal zones (Standard left, Projects middle, AiLibrary right)
/// plus the global "+ Kategorie" command.
/// </summary>
public partial class MainViewModel : ObservableObject
{
    private readonly ICategoryRepository _categories;
    private readonly IPromptRepository _prompts;
    private readonly IAiImprovementPromptRepository _aiImprovementRepo;
    private readonly IPastelColorGenerator _colors;
    private readonly IDialogService _dialogs;
    private readonly IInsertOrchestrator _insertOrchestrator;
    private readonly ILoggerFactory _loggerFactory;
    private readonly ILogger<MainViewModel> _logger;

    public ObservableCollection<CategoryViewModel> Categories { get; } = [];
    public ObservableCollection<CategoryViewModel> StandardCategories { get; } = [];
    public ObservableCollection<CategoryViewModel> ProjectCategories { get; } = [];
    public ObservableCollection<CategoryViewModel> AiLibraryCategories { get; } = [];

    [ObservableProperty]
    private bool _isLoading;

    /// <summary>
    /// The most recent undoable deletion. Set by the child VMs, watched by MainPage
    /// which displays an InfoBar with a restore button for 10 seconds.
    /// </summary>
    [ObservableProperty]
    private UndoAction? _pendingUndo;

    /// <summary>Current live search query. Empty → no filter.</summary>
    [ObservableProperty]
    private string _searchQuery = string.Empty;

    partial void OnSearchQueryChanged(string value)
    {
        foreach (CategoryViewModel c in Categories)
        {
            c.ApplyFilter(value);
        }
    }

    public MainViewModel(
        ICategoryRepository categories,
        IPromptRepository prompts,
        IAiImprovementPromptRepository aiImprovementRepo,
        IPastelColorGenerator colors,
        IDialogService dialogs,
        IInsertOrchestrator insertOrchestrator,
        ILoggerFactory loggerFactory)
    {
        _categories = categories;
        _prompts = prompts;
        _aiImprovementRepo = aiImprovementRepo;
        _colors = colors;
        _dialogs = dialogs;
        _insertOrchestrator = insertOrchestrator;
        _loggerFactory = loggerFactory;
        _logger = loggerFactory.CreateLogger<MainViewModel>();

        // Drag & drop: each zone's ListView fires CollectionChanged(Move) when the
        // user reorders items. We renumber the global SortOrder so the stored
        // order matches what the user sees across zones.
        StandardCategories.CollectionChanged += OnZoneCollectionChanged;
        ProjectCategories.CollectionChanged += OnZoneCollectionChanged;
        AiLibraryCategories.CollectionChanged += OnZoneCollectionChanged;
    }

    private bool _suppressReorderPersist;

    private async void OnZoneCollectionChanged(object? sender, NotifyCollectionChangedEventArgs e)
    {
        if (_suppressReorderPersist) return;
        if (e.Action != NotifyCollectionChangedAction.Move) return;
        try
        {
            int index = 0;
            foreach (CategoryViewModel c in StandardCategories) c.SortOrder = index++;
            foreach (CategoryViewModel c in ProjectCategories) c.SortOrder = index++;
            foreach (CategoryViewModel c in AiLibraryCategories) c.SortOrder = index++;

            foreach (CategoryViewModel c in Categories)
            {
                await c.PersistAsync();
            }
            _logger.LogInformation("Categories reordered via drag and drop.");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to persist category reorder.");
        }
    }

    public async Task InitializeAsync()
    {
        IsLoading = true;
        _suppressReorderPersist = true;
        try
        {
            Categories.Clear();
            StandardCategories.Clear();
            ProjectCategories.Clear();
            AiLibraryCategories.Clear();

            IReadOnlyList<Category> entities = await _categories.GetAllAsync();
            foreach (Category c in entities)
            {
                AttachCategory(c);
            }
            _logger.LogInformation(
                "Loaded {Total} categories ({Std} standard, {Prj} projects, {Ai} ai-library).",
                Categories.Count,
                StandardCategories.Count,
                ProjectCategories.Count,
                AiLibraryCategories.Count);
        }
        finally
        {
            _suppressReorderPersist = false;
            IsLoading = false;
        }
    }

    [RelayCommand]
    private async Task AddCategoryAsync()
    {
        var result = await _dialogs.ShowAddCategoryAsync();
        if (result is null || string.IsNullOrWhiteSpace(result.Name))
        {
            return;
        }

        IEnumerable<string> existing = Categories.Select(c => c.BackgroundColorHex);
        string color = _colors.NextDistinctColor(existing);

        Category entity = new()
        {
            Name = result.Name.Trim(),
            Type = result.Type,
            BackgroundColorHex = color,
            SortOrder = Categories.Count,
        };
        await _categories.AddAsync(entity);
        AttachCategory(entity);
        _logger.LogInformation(
            "Category created: {Name} ({Type}) color={Color}",
            entity.Name,
            entity.Type,
            entity.BackgroundColorHex);
    }

    private void AttachCategory(Category entity)
    {
        var vm = new CategoryViewModel(
            entity,
            _categories,
            _prompts,
            _aiImprovementRepo,
            _dialogs,
            _insertOrchestrator,
            _loggerFactory)
        {
            OnDeleteRequested = RemoveCategoryAsync,
            OnUndoRequested = PublishUndo,
        };
        Categories.Add(vm);
        GetBucket(vm.Type).Add(vm);
    }

    private Task RemoveCategoryAsync(CategoryViewModel vm)
    {
        Categories.Remove(vm);
        GetBucket(vm.Type).Remove(vm);
        return Task.CompletedTask;
    }

    /// <summary>Called by child VMs after a successful delete.</summary>
    public void PublishUndo(UndoAction action)
    {
        PendingUndo = action;
    }

    /// <summary>Drops the currently pending undo, used by the 10s-timer in the view.</summary>
    public void DismissUndo()
    {
        PendingUndo = null;
    }

    [RelayCommand]
    private async Task RestoreUndoAsync()
    {
        UndoAction? action = PendingUndo;
        if (action is null)
        {
            return;
        }
        PendingUndo = null;

        try
        {
            if (action.Kind == UndoActionKind.DeletedCategory && action.Category is not null)
            {
                await _categories.AddAsync(action.Category);
                foreach (Prompt p in action.Prompts)
                {
                    await _prompts.AddAsync(p);
                }
                // Re-attach so the VM + its prompts reappear in the bar.
                Category restored = action.Category;
                restored.Prompts = action.Prompts.ToList();
                AttachCategory(restored);
                _logger.LogInformation(
                    "Restored category '{Name}' with {Count} prompts.",
                    restored.Name, action.Prompts.Count);
            }
            else if (action.Kind == UndoActionKind.DeletedPrompt && action.Prompts.Count == 1)
            {
                Prompt prompt = action.Prompts[0];
                await _prompts.AddAsync(prompt);
                // Find the owning category VM and attach the prompt back.
                CategoryViewModel? owner = Categories.FirstOrDefault(c => c.Id == prompt.CategoryId);
                owner?.AttachRestoredPrompt(prompt);
                _logger.LogInformation("Restored prompt '{Label}'.", prompt.ShortLabel);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Restore from undo failed.");
        }
    }

    private ObservableCollection<CategoryViewModel> GetBucket(CategoryType type) => type switch
    {
        CategoryType.Project => ProjectCategories,
        CategoryType.AiLibrary => AiLibraryCategories,
        _ => StandardCategories,
    };
}

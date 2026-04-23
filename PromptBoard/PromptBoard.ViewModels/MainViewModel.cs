using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
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
    }

    public async Task InitializeAsync()
    {
        IsLoading = true;
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

    private ObservableCollection<CategoryViewModel> GetBucket(CategoryType type) => type switch
    {
        CategoryType.Project => ProjectCategories,
        CategoryType.AiLibrary => AiLibraryCategories,
        _ => StandardCategories,
    };
}

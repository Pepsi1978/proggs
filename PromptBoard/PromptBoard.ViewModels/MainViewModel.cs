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
/// Root ViewModel for the main bar. Holds all categories and the "+ Kategorie" command.
/// </summary>
public partial class MainViewModel : ObservableObject
{
    private readonly ICategoryRepository _categories;
    private readonly IPromptRepository _prompts;
    private readonly IPastelColorGenerator _colors;
    private readonly IDialogService _dialogs;
    private readonly ILoggerFactory _loggerFactory;
    private readonly ILogger<MainViewModel> _logger;

    public ObservableCollection<CategoryViewModel> Categories { get; } = [];

    [ObservableProperty]
    private bool _isLoading;

    public MainViewModel(
        ICategoryRepository categories,
        IPromptRepository prompts,
        IPastelColorGenerator colors,
        IDialogService dialogs,
        ILoggerFactory loggerFactory)
    {
        _categories = categories;
        _prompts = prompts;
        _colors = colors;
        _dialogs = dialogs;
        _loggerFactory = loggerFactory;
        _logger = loggerFactory.CreateLogger<MainViewModel>();
    }

    /// <summary>Load all categories and their prompts from the database.</summary>
    public async Task InitializeAsync()
    {
        IsLoading = true;
        try
        {
            Categories.Clear();
            IReadOnlyList<Category> entities = await _categories.GetAllAsync();
            foreach (Category c in entities)
            {
                AttachCategory(c);
            }
            _logger.LogInformation("Loaded {Count} categories.", Categories.Count);
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
        _logger.LogInformation("Category created: {Name} ({Type}) color={Color}", entity.Name, entity.Type, entity.BackgroundColorHex);
    }

    private void AttachCategory(Category entity)
    {
        var vm = new CategoryViewModel(entity, _categories, _prompts, _dialogs, _loggerFactory)
        {
            OnDeleteRequested = RemoveCategoryAsync,
        };
        Categories.Add(vm);
    }

    private Task RemoveCategoryAsync(CategoryViewModel vm)
    {
        Categories.Remove(vm);
        return Task.CompletedTask;
    }

    /// <summary>Grouped view: Standard categories first, then Projects, then AiLibrary.</summary>
    public IEnumerable<CategoryViewModel> StandardCategories => Categories.Where(c => c.Type == CategoryType.Standard);
    public IEnumerable<CategoryViewModel> ProjectCategories => Categories.Where(c => c.Type == CategoryType.Project);
    public IEnumerable<CategoryViewModel> AiLibraryCategories => Categories.Where(c => c.Type == CategoryType.AiLibrary);
}

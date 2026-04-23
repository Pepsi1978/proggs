using System;
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
/// ViewModel for one column in the bar (a category and its prompts).
/// Phase 3: IsProject/IsInline flags drive the two rendering paths
/// (inline tile vs. flyout-only header).
/// </summary>
public partial class CategoryViewModel : ObservableObject
{
    private readonly ICategoryRepository _categories;
    private readonly IPromptRepository _prompts;
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

    public Func<CategoryViewModel, Task>? OnDeleteRequested { get; set; }

    public CategoryViewModel(
        Category category,
        ICategoryRepository categories,
        IPromptRepository prompts,
        IDialogService dialogs,
        IInsertOrchestrator insertOrchestrator,
        ILoggerFactory loggerFactory)
    {
        ArgumentNullException.ThrowIfNull(category);
        _categories = categories;
        _prompts = prompts;
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
    }

    public bool IsProject => Type == CategoryType.Project;
    public bool IsInline => Type != CategoryType.Project;

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

        await _categories.DeleteAsync(Id);
        if (OnDeleteRequested is not null)
        {
            await OnDeleteRequested(this);
        }
    }

    [RelayCommand]
    private async Task AddPromptAsync()
    {
        var request = new PromptEditorRequest(
            ShortLabel: "Neuer Prompt",
            OriginalText: string.Empty,
            ImprovedText: null,
            ActiveVersion: PromptVersion.Original);

        var result = await _dialogs.ShowPromptEditorAsync(request);
        if (result is null || string.IsNullOrWhiteSpace(result.ShortLabel))
        {
            return;
        }

        Prompt prompt = Type == CategoryType.AiLibrary
            ? new AiImprovementPrompt
            {
                CategoryId = Id,
                ShortLabel = result.ShortLabel.Trim(),
                OriginalText = result.OriginalText,
                ImprovedText = result.ImprovedText,
                ActiveVersion = result.ActiveVersion,
                SortOrder = Prompts.Count,
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
        AttachPrompt(prompt);
        _logger.LogInformation("Prompt added: {Label} in {Category}", prompt.ShortLabel, Name);
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
        };
        Prompts.Add(vm);
    }

    private Task RemovePromptAsync(PromptViewModel vm)
    {
        Prompts.Remove(vm);
        return Task.CompletedTask;
    }

    internal Task PersistAsync()
    {
        Category entity = new()
        {
            Id = Id,
            Name = Name,
            BackgroundColorHex = BackgroundColorHex,
            Type = Type,
            SortOrder = SortOrder,
        };
        return _categories.UpdateAsync(entity);
    }
}

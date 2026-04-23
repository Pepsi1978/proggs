using System;
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
/// ViewModel wrapping a single <see cref="Prompt"/> row in the bar.
/// Phase 5: tracks whether the underlying entity is an
/// <see cref="AiImprovementPrompt"/> so ToEntity() preserves the TPH
/// subtype and the editor dialog hides its own "Improve" button.
/// </summary>
public partial class PromptViewModel : ObservableObject
{
    private readonly IPromptRepository _prompts;
    private readonly IDialogService _dialogs;
    private readonly IInsertOrchestrator _insertOrchestrator;
    private readonly ILogger<PromptViewModel> _logger;

    public Guid Id { get; }
    public Guid CategoryId { get; }
    public bool IsAiImprovementPrompt { get; }

    // Only meaningful when IsAiImprovementPrompt == true.
    public string GeminiModel { get; private set; }
    public bool IsActiveForImprovement { get; internal set; }

    [ObservableProperty]
    private string _shortLabel;

    [ObservableProperty]
    private string _originalText;

    [ObservableProperty]
    private string? _improvedText;

    [ObservableProperty]
    private PromptVersion _activeVersion;

    [ObservableProperty]
    private bool _isAlwaysOn;

    [ObservableProperty]
    private int _sortOrder;

    public Func<PromptViewModel, Task>? OnDeleteRequested { get; set; }

    /// <summary>Published after a successful delete so the snackbar can show.</summary>
    public Action<UndoAction>? OnUndoRequested { get; set; }

    public PromptViewModel(
        Prompt prompt,
        IPromptRepository prompts,
        IDialogService dialogs,
        IInsertOrchestrator insertOrchestrator,
        ILogger<PromptViewModel> logger)
    {
        ArgumentNullException.ThrowIfNull(prompt);
        _prompts = prompts;
        _dialogs = dialogs;
        _insertOrchestrator = insertOrchestrator;
        _logger = logger;

        Id = prompt.Id;
        CategoryId = prompt.CategoryId;
        _shortLabel = prompt.ShortLabel;
        _originalText = prompt.OriginalText;
        _improvedText = prompt.ImprovedText;
        _activeVersion = prompt.ActiveVersion;
        _isAlwaysOn = prompt.IsAlwaysOn;
        _sortOrder = prompt.SortOrder;

        if (prompt is AiImprovementPrompt ai)
        {
            IsAiImprovementPrompt = true;
            GeminiModel = ai.GeminiModel;
            IsActiveForImprovement = ai.IsActiveForImprovement;
        }
        else
        {
            GeminiModel = "gemini-2.5-flash";
        }
    }

    public bool CanToggleVersion => !string.IsNullOrEmpty(ImprovedText);

    public string EffectiveText => ActiveVersion == PromptVersion.Improved && !string.IsNullOrEmpty(ImprovedText)
        ? ImprovedText!
        : OriginalText;

    partial void OnImprovedTextChanged(string? value)
    {
        OnPropertyChanged(nameof(CanToggleVersion));
        OnPropertyChanged(nameof(EffectiveText));
    }

    partial void OnActiveVersionChanged(PromptVersion value)
    {
        OnPropertyChanged(nameof(EffectiveText));
        _ = PersistAsync();
    }

    partial void OnOriginalTextChanged(string value)
    {
        OnPropertyChanged(nameof(EffectiveText));
    }

    partial void OnIsAlwaysOnChanged(bool value)
    {
        _ = PersistAsync();
    }

    [RelayCommand]
    private async Task InsertAsync()
    {
        try
        {
            await _insertOrchestrator.InsertAsync(Id, EffectiveText);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Insert failed for prompt {Id} ({Label}).", Id, ShortLabel);
        }
    }

    [RelayCommand]
    private async Task EditAsync()
    {
        var request = new PromptEditorRequest(
            ShortLabel,
            OriginalText,
            ImprovedText,
            ActiveVersion,
            IsAiImprovementPrompt);

        var result = await _dialogs.ShowPromptEditorAsync(request);
        if (result is null)
        {
            return;
        }

        ShortLabel = result.ShortLabel;
        OriginalText = result.OriginalText;
        ImprovedText = result.ImprovedText;
        ActiveVersion = result.ActiveVersion;

        await PersistAsync();
    }

    [RelayCommand]
    private async Task DeleteAsync()
    {
        bool confirmed = await _dialogs.ShowConfirmAsync(
            "Prompt loeschen?",
            $"Soll der Prompt \"{ShortLabel}\" wirklich geloescht werden?");
        if (!confirmed)
        {
            return;
        }

        // Snapshot BEFORE delete so Undo gets the exact TPH subtype back.
        Prompt snapshot = ToEntity();

        await _prompts.DeleteAsync(Id);
        if (OnDeleteRequested is not null)
        {
            await OnDeleteRequested(this);
        }

        OnUndoRequested?.Invoke(new UndoAction
        {
            Kind = UndoActionKind.DeletedPrompt,
            Message = $"Prompt \"{snapshot.ShortLabel}\" geloescht.",
            Prompts = new[] { snapshot },
        });
    }

    [RelayCommand]
    private void ToggleAlwaysOn()
    {
        IsAlwaysOn = !IsAlwaysOn;
    }

    [RelayCommand]
    private void ToggleVersion()
    {
        if (!CanToggleVersion)
        {
            return;
        }
        ActiveVersion = ActiveVersion == PromptVersion.Original
            ? PromptVersion.Improved
            : PromptVersion.Original;
    }

    internal async Task PersistAsync()
    {
        Prompt entity = ToEntity();
        await _prompts.UpdateAsync(entity);
    }

    internal Prompt ToEntity()
    {
        if (IsAiImprovementPrompt)
        {
            return new AiImprovementPrompt
            {
                Id = Id,
                CategoryId = CategoryId,
                ShortLabel = ShortLabel,
                OriginalText = OriginalText,
                ImprovedText = ImprovedText,
                ActiveVersion = ActiveVersion,
                IsAlwaysOn = IsAlwaysOn,
                SortOrder = SortOrder,
                GeminiModel = GeminiModel,
                IsActiveForImprovement = IsActiveForImprovement,
            };
        }

        return new Prompt
        {
            Id = Id,
            CategoryId = CategoryId,
            ShortLabel = ShortLabel,
            OriginalText = OriginalText,
            ImprovedText = ImprovedText,
            ActiveVersion = ActiveVersion,
            IsAlwaysOn = IsAlwaysOn,
            SortOrder = SortOrder,
        };
    }
}

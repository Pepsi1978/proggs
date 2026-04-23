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
/// </summary>
public partial class PromptViewModel : ObservableObject
{
    private readonly IPromptRepository _prompts;
    private readonly IDialogService _dialogs;
    private readonly ILogger<PromptViewModel> _logger;

    public Guid Id { get; }
    public Guid CategoryId { get; }

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

    /// <summary>Set by parent CategoryViewModel so we can request our own removal.</summary>
    public Func<PromptViewModel, Task>? OnDeleteRequested { get; set; }

    public PromptViewModel(
        Prompt prompt,
        IPromptRepository prompts,
        IDialogService dialogs,
        ILogger<PromptViewModel> logger)
    {
        ArgumentNullException.ThrowIfNull(prompt);
        _prompts = prompts;
        _dialogs = dialogs;
        _logger = logger;

        Id = prompt.Id;
        CategoryId = prompt.CategoryId;
        _shortLabel = prompt.ShortLabel;
        _originalText = prompt.OriginalText;
        _improvedText = prompt.ImprovedText;
        _activeVersion = prompt.ActiveVersion;
        _isAlwaysOn = prompt.IsAlwaysOn;
        _sortOrder = prompt.SortOrder;
    }

    /// <summary>True when there is an AI-improved text and the toggle is meaningful.</summary>
    public bool CanToggleVersion => !string.IsNullOrEmpty(ImprovedText);

    /// <summary>The text that would be inserted into the CLI on click.</summary>
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

    /// <summary>
    /// Called by the UI when the user clicks the prompt's label. In Phase 1
    /// this is a stub; Phase 2 wires it to the CLI text injection.
    /// </summary>
    [RelayCommand]
    private Task InsertAsync()
    {
        _logger.LogInformation("Insert requested for prompt {Id} ({Label}). Will be wired in Phase 2.", Id, ShortLabel);
        return Task.CompletedTask;
    }

    [RelayCommand]
    private async Task EditAsync()
    {
        var result = await _dialogs.ShowPromptEditorAsync(new PromptEditorRequest(
            ShortLabel, OriginalText, ImprovedText, ActiveVersion));
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

        await _prompts.DeleteAsync(Id);
        if (OnDeleteRequested is not null)
        {
            await OnDeleteRequested(this);
        }
    }

    [RelayCommand]
    private void ToggleAlwaysOn()
    {
        IsAlwaysOn = !IsAlwaysOn; // partial OnChanged handler persists.
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
        // partial OnChanged handler persists.
    }

    internal async Task PersistAsync()
    {
        Prompt entity = ToEntity();
        await _prompts.UpdateAsync(entity);
    }

    internal Prompt ToEntity() => new()
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

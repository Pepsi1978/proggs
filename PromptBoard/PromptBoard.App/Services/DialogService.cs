using System;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PromptBoard.App.Views;
using PromptBoard.Core.Services;

namespace PromptBoard.App.Services;

/// <summary>
/// Concrete WinUI 3 implementation of <see cref="IDialogService"/>.
/// Dialogs require a <see cref="XamlRoot"/>; the MainPage sets it
/// once its content has loaded.
/// </summary>
public sealed class DialogService : IDialogService
{
    private readonly IServiceProvider _services;
    private readonly IAudioRecorder _recorder;
    private readonly IGroqTranscriptionService _transcription;
    private XamlRoot? _xamlRoot;

    public DialogService(
        IServiceProvider services,
        IAudioRecorder recorder,
        IGroqTranscriptionService transcription)
    {
        _services = services;
        _recorder = recorder;
        _transcription = transcription;
    }

    public void Initialize(XamlRoot xamlRoot) => _xamlRoot = xamlRoot;

    private XamlRoot EnsureRoot()
    {
        if (_xamlRoot is null)
        {
            throw new InvalidOperationException("DialogService is not initialized. Call Initialize from MainPage first.");
        }
        return _xamlRoot;
    }

    public async Task<NewCategoryResult?> ShowAddCategoryAsync()
    {
        var dialog = new AddCategoryDialog { XamlRoot = EnsureRoot() };
        ContentDialogResult result = await dialog.ShowAsync();
        if (result != ContentDialogResult.Primary || string.IsNullOrWhiteSpace(dialog.CategoryName))
        {
            return null;
        }
        return new NewCategoryResult(dialog.CategoryName, dialog.CategoryType);
    }

    public async Task<string?> ShowRenameAsync(string title, string currentValue)
    {
        var dialog = new TextInputDialog(title, currentValue) { XamlRoot = EnsureRoot() };
        ContentDialogResult result = await dialog.ShowAsync();
        return result == ContentDialogResult.Primary ? dialog.Value?.Trim() : null;
    }

    public async Task<bool> ShowConfirmAsync(string title, string message, string confirmText = "Loeschen", string cancelText = "Abbrechen")
    {
        var dialog = new ContentDialog
        {
            XamlRoot = EnsureRoot(),
            Title = title,
            Content = message,
            PrimaryButtonText = confirmText,
            CloseButtonText = cancelText,
            DefaultButton = ContentDialogButton.Close,
        };
        ContentDialogResult result = await dialog.ShowAsync();
        return result == ContentDialogResult.Primary;
    }

    public async Task<PromptEditorResult?> ShowPromptEditorAsync(PromptEditorRequest request)
    {
        // Resolve the improvement service fresh per dialog so its transient
        // dependencies (repository) get a clean DbContext.
        var improvement = _services.GetRequiredService<IPromptImprovementService>();

        var dialog = new PromptEditorDialog(request, _recorder, _transcription, improvement)
        {
            XamlRoot = EnsureRoot(),
        };
        ContentDialogResult result = await dialog.ShowAsync();
        return result == ContentDialogResult.Primary ? dialog.Result : null;
    }
}

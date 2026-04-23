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
/// Every ShowAsync call is wrapped so the window briefly accepts focus
/// (WS_EX_NOACTIVATE removed), otherwise the user can't type into the
/// dialog textboxes and input leaks to the underlying CLI.
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

    private async Task<T> WithActivationAsync<T>(Func<Task<T>> work)
    {
        IntPtr hwnd = MainWindow.CurrentHwnd;
        if (hwnd != IntPtr.Zero)
        {
            NativeMethods.AllowActivation(hwnd);
        }
        try
        {
            return await work();
        }
        finally
        {
            if (hwnd != IntPtr.Zero)
            {
                NativeMethods.ForbidActivation(hwnd);
            }
        }
    }

    public Task<NewCategoryResult?> ShowAddCategoryAsync() => WithActivationAsync(async () =>
    {
        var dialog = new AddCategoryDialog { XamlRoot = EnsureRoot() };
        ContentDialogResult result = await dialog.ShowAsync();
        if (result != ContentDialogResult.Primary || string.IsNullOrWhiteSpace(dialog.CategoryName))
        {
            return (NewCategoryResult?)null;
        }
        return new NewCategoryResult(dialog.CategoryName, dialog.CategoryType);
    });

    public Task<string?> ShowRenameAsync(string title, string currentValue) => WithActivationAsync(async () =>
    {
        var dialog = new TextInputDialog(title, currentValue) { XamlRoot = EnsureRoot() };
        ContentDialogResult result = await dialog.ShowAsync();
        return result == ContentDialogResult.Primary ? dialog.Value?.Trim() : null;
    });

    public Task<bool> ShowConfirmAsync(string title, string message, string confirmText = "Loeschen", string cancelText = "Abbrechen") => WithActivationAsync(async () =>
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
    });

    public Task<PromptEditorResult?> ShowPromptEditorAsync(PromptEditorRequest request) => WithActivationAsync(async () =>
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
    });

    public Task<bool> ShowBackupAsync() => WithActivationAsync(async () =>
    {
        // Fresh per dialog open so each run starts with a clean DbContext.
        var backup = _services.GetRequiredService<IBackupService>();
        var backupFiles = _services.GetRequiredService<IBackupFileService>();
        var drive = _services.GetRequiredService<IGoogleDriveBackupService>();

        var dialog = new BackupDialog(backup, backupFiles, drive, MainWindow.CurrentHwnd)
        {
            XamlRoot = EnsureRoot(),
        };
        await dialog.ShowAsync();
        return dialog.DidRestore;
    });

    public Task<bool> ShowSettingsAsync() => WithActivationAsync(async () =>
    {
        var vm = _services.GetRequiredService<PromptBoard.ViewModels.SettingsViewModel>();
        var dialog = new SettingsDialog(vm) { XamlRoot = EnsureRoot() };
        await dialog.ShowAsync();
        return dialog.Saved;
    });

    public async Task ShowAboutAsync()
    {
        await WithActivationAsync(async () =>
        {
            var dialog = new AboutDialog { XamlRoot = EnsureRoot() };
            await dialog.ShowAsync();
            return true;
        });
    }
}

using System;
using System.Threading.Tasks;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using PromptBoard.Core.Models;
using PromptBoard.Core.Services;
using Serilog;
using Windows.Storage;
using Windows.Storage.Pickers;

namespace PromptBoard.App.Views;

public sealed partial class BackupDialog : ContentDialog
{
    private readonly IBackupService _backup;
    private readonly IBackupFileService _backupFiles;
    private readonly IntPtr _ownerHwnd;

    /// <summary>
    /// True when a restore (Replace or Merge) was applied successfully,
    /// so the caller knows to reload the main view.
    /// </summary>
    public bool DidRestore { get; private set; }

    public BackupDialog(
        IBackupService backup,
        IBackupFileService backupFiles,
        IntPtr ownerHwnd)
    {
        InitializeComponent();
        _backup = backup;
        _backupFiles = backupFiles;
        _ownerHwnd = ownerHwnd;

        ExportButton.Click += async (_, _) => await HandleExportAsync();
        ImportReplaceButton.Click += async (_, _) => await HandleImportAsync(RestoreMode.Replace);
        ImportMergeButton.Click += async (_, _) => await HandleImportAsync(RestoreMode.Merge);
    }

    private async Task HandleExportAsync()
    {
        string? path = await PickSavePathAsync();
        if (string.IsNullOrEmpty(path))
        {
            return;
        }

        await RunWithStatusAsync("Schreibe Backup...", async () =>
        {
            await _backupFiles.ExportAsync(path);
            ShowSuccess($"Backup gespeichert: {path}");
        });
    }

    private async Task HandleImportAsync(RestoreMode mode)
    {
        string? path = await PickOpenPathAsync();
        if (string.IsNullOrEmpty(path))
        {
            return;
        }

        BackupDocument? document;
        try
        {
            document = await _backupFiles.ReadAsync(path);
        }
        catch (Exception ex)
        {
            ShowError($"Datei konnte nicht gelesen werden: {ex.Message}");
            return;
        }

        string modeText = mode == RestoreMode.Replace ? "ersetzt" : "zusammengefuehrt";
        bool confirmed = await AskInlineConfirmAsync(
            $"Backup enthaelt {document.Categories.Count} Kategorie(n) und {document.Prompts.Count} Prompt(s). Bestehende Daten werden {modeText}.");
        if (!confirmed)
        {
            return;
        }

        await RunWithStatusAsync($"Wende Backup an ({mode})...", async () =>
        {
            RestoreResult result = await _backup.ApplyAsync(document!, mode);
            DidRestore = true;
            ShowSuccess(
                $"Fertig. Kategorien: +{result.CategoriesAdded} / geaendert {result.CategoriesUpdated} / entfernt {result.CategoriesRemoved}. " +
                $"Prompts: +{result.PromptsAdded} / geaendert {result.PromptsUpdated} / entfernt {result.PromptsRemoved}.");
        });
    }

    private async Task<bool> AskInlineConfirmAsync(string question)
    {
        var dialog = new ContentDialog
        {
            XamlRoot = XamlRoot,
            Title = "Wiederherstellen bestaetigen",
            Content = question,
            PrimaryButtonText = "Fortfahren",
            CloseButtonText = "Abbrechen",
            DefaultButton = ContentDialogButton.Close,
        };
        ContentDialogResult r = await dialog.ShowAsync();
        return r == ContentDialogResult.Primary;
    }

    private async Task<string?> PickSavePathAsync()
    {
        try
        {
            var picker = new FileSavePicker
            {
                SuggestedFileName = $"promptboard-backup-{DateTime.Now:yyyyMMdd-HHmm}",
                SuggestedStartLocation = PickerLocationId.DocumentsLibrary,
            };
            picker.FileTypeChoices.Add("PromptBoard-Backup", new[] { ".json" });

            WinRT.Interop.InitializeWithWindow.Initialize(picker, _ownerHwnd);
            StorageFile? file = await picker.PickSaveFileAsync();
            return file?.Path;
        }
        catch (Exception ex)
        {
            ShowError($"Datei-Auswahl fehlgeschlagen: {ex.Message}");
            Log.Error(ex, "FileSavePicker failed.");
            return null;
        }
    }

    private async Task<string?> PickOpenPathAsync()
    {
        try
        {
            var picker = new FileOpenPicker
            {
                SuggestedStartLocation = PickerLocationId.DocumentsLibrary,
                ViewMode = PickerViewMode.List,
            };
            picker.FileTypeFilter.Add(".json");

            WinRT.Interop.InitializeWithWindow.Initialize(picker, _ownerHwnd);
            StorageFile? file = await picker.PickSingleFileAsync();
            return file?.Path;
        }
        catch (Exception ex)
        {
            ShowError($"Datei-Auswahl fehlgeschlagen: {ex.Message}");
            Log.Error(ex, "FileOpenPicker failed.");
            return null;
        }
    }

    private async Task RunWithStatusAsync(string running, Func<Task> action)
    {
        try
        {
            SetRunning(running);
            ToggleButtons(false);
            await action();
        }
        catch (Exception ex)
        {
            ShowError($"Fehler: {ex.Message}");
            Log.Error(ex, "Backup action failed.");
        }
        finally
        {
            StatusSpinner.IsActive = false;
            ToggleButtons(true);
        }
    }

    private void ToggleButtons(bool enabled)
    {
        ExportButton.IsEnabled = enabled;
        ImportReplaceButton.IsEnabled = enabled;
        ImportMergeButton.IsEnabled = enabled;
    }

    private void SetRunning(string message)
    {
        StatusBox.Visibility = Visibility.Visible;
        StatusSpinner.IsActive = true;
        StatusText.Text = message;
    }

    private void ShowSuccess(string message)
    {
        StatusBox.Visibility = Visibility.Visible;
        StatusSpinner.IsActive = false;
        StatusText.Text = message;
    }

    private void ShowError(string message)
    {
        StatusBox.Visibility = Visibility.Visible;
        StatusSpinner.IsActive = false;
        StatusText.Text = message;
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Win32;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using TerminalVoiceOverlay.NativeMethods;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views;

/// <summary>
/// PromptBoard side panel with full editor: add/edit/delete prompts and
/// categories, settings dialog for API keys, JSON backup/restore. Reads
/// and writes the shared SQLite database so changes are visible to every
/// consumer (VTO, standalone PromptBoard.App during the transition).
/// </summary>
public partial class PromptBoardPanel : Window
{
    public event Action<string>? PromptInsertRequested;

    private Guid? _activeCategoryId;
    private List<Category> _categories = new();

    public PromptBoardPanel()
    {
        InitializeComponent();
        BtnAddCategory.Click  += async (_, _) => await AddCategoryAsync();
        BtnAddPrompt.Click    += async (_, _) => await AddPromptAsync();
        BtnSettings.Click     += async (_, _) => await ShowSettingsAsync();
        BtnBackup.Click       += async (_, _) => await ShowBackupMenuAsync();
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);
        var hwnd = new WindowInteropHelper(this).Handle;
        int exStyle = Win32.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
        Win32.SetWindowLong(hwnd, Win32.GWL_EXSTYLE, exStyle | Win32.WS_EX_NOACTIVATE | Win32.WS_EX_TOOLWINDOW);
    }

    public async Task RefreshAsync()
    {
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var categoryRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            _categories = (await categoryRepo.GetAllAsync())
                .OrderBy(c => c.SortOrder).ThenBy(c => c.Name)
                .ToList();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"PromptBoardPanel refresh failed: {ex.Message}");
            _categories = new List<Category>();
        }

        RenderCategoryTabs();

        if (_categories.Count == 0)
        {
            _activeCategoryId = null;
            RenderEmptyState();
            return;
        }

        if (_activeCategoryId is null || !_categories.Any(c => c.Id == _activeCategoryId))
        {
            _activeCategoryId = _categories[0].Id;
        }

        await LoadPromptsForActiveCategoryAsync();
    }

    // ──────────────── Rendering ────────────────

    private void RenderCategoryTabs()
    {
        CategoryTabs.Children.Clear();
        foreach (var cat in _categories)
        {
            var btn = new Button
            {
                Content = cat.Name,
                Tag = cat.Id,
                Style = (Style)FindResource(
                    cat.Id == _activeCategoryId ? "CategoryTabActive" : "CategoryTab"),
            };
            btn.Click += async (_, _) =>
            {
                _activeCategoryId = (Guid)btn.Tag;
                RenderCategoryTabs();
                await LoadPromptsForActiveCategoryAsync();
            };
            btn.MouseRightButtonUp += async (_, _) =>
            {
                var target = _categories.FirstOrDefault(c => c.Id == (Guid)btn.Tag);
                if (target is not null) await ShowCategoryContextAsync(target);
            };
            CategoryTabs.Children.Add(btn);
        }
    }

    private async Task LoadPromptsForActiveCategoryAsync()
    {
        if (_activeCategoryId is null)
        {
            RenderEmptyState();
            return;
        }

        IReadOnlyList<Prompt> prompts;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            prompts = await promptRepo.GetByCategoryAsync(_activeCategoryId.Value);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Prompt load failed: {ex.Message}");
            RenderEmptyState();
            return;
        }

        PromptList.Children.Clear();

        if (prompts.Count == 0)
        {
            RenderEmptyState("Keine Prompts in dieser Kategorie. Benutze + unten.");
            return;
        }

        foreach (var p in prompts.OrderBy(x => x.SortOrder).ThenBy(x => x.ShortLabel))
        {
            PromptList.Children.Add(BuildPromptRow(p));
        }
    }

    private Border BuildPromptRow(Prompt prompt)
    {
        var row = new Border { Style = (Style)FindResource("PromptRow") };

        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(20) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(24) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(24) });

        var dot = PromptBoardPanelHelpers.BuildAOIndicator(prompt.IsAlwaysOn);
        Grid.SetColumn(dot, 0);
        grid.Children.Add(dot);

        var insertBtn = new Button
        {
            Content = prompt.ShortLabel,
            Style = (Style)FindResource("PromptButton"),
            ToolTip = prompt.EffectiveText().Length > 500
                ? prompt.EffectiveText().Substring(0, 500) + "..."
                : prompt.EffectiveText(),
        };
        insertBtn.Click += (_, _) => PromptInsertRequested?.Invoke(prompt.EffectiveText());
        Grid.SetColumn(insertBtn, 1);
        grid.Children.Add(insertBtn);

        var editBtn = new Button
        {
            Content = "✎", // pencil
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Bearbeiten",
        };
        editBtn.Click += async (_, _) => await EditPromptAsync(prompt);
        Grid.SetColumn(editBtn, 2);
        grid.Children.Add(editBtn);

        var deleteBtn = new Button
        {
            Content = "✕", // cross
            Style = (Style)FindResource("RowIconButton"),
            ToolTip = "Loeschen",
        };
        deleteBtn.Click += async (_, _) => await DeletePromptAsync(prompt);
        Grid.SetColumn(deleteBtn, 3);
        grid.Children.Add(deleteBtn);

        row.Child = grid;
        return row;
    }

    private void RenderEmptyState(string message = "Noch keine Kategorien. Benutze + oben.")
    {
        PromptList.Children.Clear();
        PromptList.Children.Add(new TextBlock
        {
            Text = message,
            Foreground = new SolidColorBrush(Color.FromRgb(0x9A, 0x9A, 0x9A)),
            FontSize = 12,
            TextWrapping = TextWrapping.Wrap,
            Margin = new Thickness(4, 8, 4, 0),
        });
    }

    // ──────────────── Editor actions ────────────────

    private async Task AddCategoryAsync()
    {
        var name = TextInputDialog.Ask(this, "Neue Kategorie", "Name:");
        if (string.IsNullOrWhiteSpace(name)) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
            var colorSvc = scope.ServiceProvider.GetRequiredService<IPastelColorGenerator>();
            int nextOrder = _categories.Count == 0 ? 0 : _categories.Max(c => c.SortOrder) + 1;

            var cat = new Category
            {
                Id = Guid.NewGuid(),
                Name = name,
                BackgroundColorHex = colorSvc.NextDistinctColor(_categories.Select(c => c.BackgroundColorHex)),
                SortOrder = nextOrder,
                Type = CategoryType.Standard,
            };
            await repo.AddAsync(cat);
            _activeCategoryId = cat.Id;
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Kategorie konnte nicht angelegt werden: {ex.Message}",
                "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await RefreshAsync();
    }

    private async Task ShowCategoryContextAsync(Category cat)
    {
        // Simple popup: rename or delete. Using a tiny menu since WPF
        // ContextMenu + WS_EX_NOACTIVATE can misbehave.
        var action = TextInputDialog.Ask(
            this,
            $"Kategorie '{cat.Name}'",
            "Tippe R zum Umbenennen, D zum Loeschen, Enter zum Abbrechen:",
            "");
        if (string.IsNullOrEmpty(action)) return;

        if (action.Equals("R", StringComparison.OrdinalIgnoreCase))
        {
            var newName = TextInputDialog.Ask(this, "Kategorie umbenennen", "Neuer Name:", cat.Name);
            if (string.IsNullOrWhiteSpace(newName) || newName == cat.Name) return;

            try
            {
                using var scope = PromptBoardHost.Services.CreateScope();
                var repo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
                cat.Name = newName;
                await repo.UpdateAsync(cat);
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            await RefreshAsync();
        }
        else if (action.Equals("D", StringComparison.OrdinalIgnoreCase))
        {
            if (!ConfirmDialog.Ask(this, "Kategorie loeschen?",
                $"Kategorie '{cat.Name}' wird mit allen enthaltenen Prompts geloescht.",
                "Loeschen")) return;

            try
            {
                using var scope = PromptBoardHost.Services.CreateScope();
                var repo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
                await repo.DeleteAsync(cat.Id);
                if (_activeCategoryId == cat.Id) _activeCategoryId = null;
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            await RefreshAsync();
        }
    }

    private async Task AddPromptAsync()
    {
        if (_activeCategoryId is null)
        {
            MessageBox.Show("Lege zuerst eine Kategorie an.", "PromptBoard",
                MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }

        var result = PromptEditDialog.Ask(this, "Neuer Prompt", string.Empty, string.Empty, false);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            var prompt = new Prompt
            {
                Id = Guid.NewGuid(),
                CategoryId = _activeCategoryId.Value,
                ShortLabel = result.ShortLabel,
                OriginalText = result.OriginalText,
                IsAlwaysOn = result.IsAlwaysOn,
                ActiveVersion = PromptVersion.Original,
                SortOrder = 0,
            };
            await repo.AddAsync(prompt);
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await LoadPromptsForActiveCategoryAsync();
    }

    private async Task EditPromptAsync(Prompt prompt)
    {
        var result = PromptEditDialog.Ask(
            this, "Prompt bearbeiten",
            prompt.ShortLabel, prompt.OriginalText, prompt.IsAlwaysOn);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            prompt.ShortLabel = result.ShortLabel;
            prompt.OriginalText = result.OriginalText;
            prompt.IsAlwaysOn = result.IsAlwaysOn;
            await repo.UpdateAsync(prompt);
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await LoadPromptsForActiveCategoryAsync();
    }

    private async Task DeletePromptAsync(Prompt prompt)
    {
        if (!ConfirmDialog.Ask(this, "Prompt loeschen?",
            $"Prompt '{prompt.ShortLabel}' wirklich loeschen?",
            "Loeschen")) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
            await repo.DeleteAsync(prompt.Id);
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }

        await LoadPromptsForActiveCategoryAsync();
    }

    // ──────────────── Settings ────────────────

    private async Task ShowSettingsAsync()
    {
        AppSettings current;
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();
            current = await repo.GetAsync();
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
            return;
        }

        var result = SettingsDialog.Ask(this, current);
        if (result is null) return;

        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var repo = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();
            // Re-fetch so we keep any refresh-token/email that was written
            // during the dialog (Connect flow).
            var latest = await repo.GetAsync();
            latest.GroqApiKey = result.GroqApiKey;
            latest.GeminiApiKey = result.GeminiApiKey;
            latest.SeparatorTemplate = result.SeparatorTemplate;
            latest.GoogleClientId = result.GoogleClientId;
            latest.GoogleClientSecret = result.GoogleClientSecret;
            await repo.UpdateAsync(latest);
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Fehler", MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    // ──────────────── Backup / Restore ────────────────

    private async Task ShowBackupMenuAsync()
    {
        var action = TextInputDialog.Ask(
            this,
            "Backup / Wiederherstellen",
            "E = Export Datei, I = Import Datei, G = Google Drive sichern, R = Google Drive laden:",
            "");
        if (string.IsNullOrEmpty(action)) return;

        switch (action.Trim().ToUpperInvariant())
        {
            case "E": await ExportAsync(); break;
            case "I": await ImportAsync(); break;
            case "G": await UploadToGoogleDriveAsync(); break;
            case "R": await RestoreFromGoogleDriveAsync(); break;
        }
    }

    private async Task UploadToGoogleDriveAsync()
    {
        try
        {
            string json = await BuildBackupJsonAsync();
            using var scope = PromptBoardHost.Services.CreateScope();
            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();

            if (!await drive.IsAuthenticatedAsync())
            {
                MessageBox.Show("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.",
                    "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            await drive.UploadAsync(json);
            var email = await drive.GetAccountEmailAsync();
            MessageBox.Show($"Backup bei Google Drive gespeichert ({email}).",
                "PromptBoard", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (GoogleDriveNotConfiguredException)
        {
            MessageBox.Show("Google Drive ist noch nicht eingerichtet. Bitte in den Einstellungen Client ID/Secret eintragen und 'Verbinden' klicken.",
                "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Google-Drive-Upload fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private async Task RestoreFromGoogleDriveAsync()
    {
        try
        {
            using var scope = PromptBoardHost.Services.CreateScope();
            var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();

            if (!await drive.IsAuthenticatedAsync())
            {
                MessageBox.Show("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.",
                    "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            var json = await drive.DownloadLatestAsync();
            if (json is null)
            {
                MessageBox.Show("Kein Backup bei Google Drive gefunden.",
                    "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            if (!ConfirmDialog.Ask(this, "Google-Drive-Backup laden",
                "Lokale Kategorien und Prompts mit gleicher ID werden ueberschrieben. Neue werden angelegt.",
                "Einspielen")) return;

            await ApplyBackupJsonAsync(json);
            MessageBox.Show("Google-Drive-Backup eingespielt.",
                "PromptBoard", MessageBoxButton.OK, MessageBoxImage.Information);
            await RefreshAsync();
        }
        catch (GoogleDriveNotConfiguredException)
        {
            MessageBox.Show("Google Drive ist noch nicht eingerichtet. Bitte in den Einstellungen Client ID/Secret eintragen und 'Verbinden' klicken.",
                "Google Drive", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Google-Drive-Restore fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private async Task<string> BuildBackupJsonAsync()
    {
        using var scope = PromptBoardHost.Services.CreateScope();
        var catRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
        var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
        var settings = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();

        var cats = await catRepo.GetAllAsync();
        var allPrompts = new List<Prompt>();
        foreach (var c in cats)
            allPrompts.AddRange(await promptRepo.GetByCategoryAsync(c.Id));
        var appSettings = await settings.GetAsync();

        var backup = new BackupData
        {
            ExportedAt = DateTime.UtcNow,
            Categories = cats.Select(c => new BackupCategory
            {
                Id = c.Id, Name = c.Name, SortOrder = c.SortOrder,
                BackgroundColorHex = c.BackgroundColorHex, Type = (int)c.Type
            }).ToList(),
            Prompts = allPrompts.Select(p => new BackupPrompt
            {
                Id = p.Id, CategoryId = p.CategoryId,
                ShortLabel = p.ShortLabel, OriginalText = p.OriginalText,
                ImprovedText = p.ImprovedText, ActiveVersion = (int)p.ActiveVersion,
                IsAlwaysOn = p.IsAlwaysOn, SortOrder = p.SortOrder,
            }).ToList(),
            SeparatorTemplate = appSettings.SeparatorTemplate,
        };

        return JsonSerializer.Serialize(backup, new JsonSerializerOptions { WriteIndented = true });
    }

    private async Task ApplyBackupJsonAsync(string json)
    {
        var backup = JsonSerializer.Deserialize<BackupData>(json)
            ?? throw new InvalidOperationException("Backup-Datei konnte nicht gelesen werden.");

        using var scope = PromptBoardHost.Services.CreateScope();
        var catRepo = scope.ServiceProvider.GetRequiredService<ICategoryRepository>();
        var promptRepo = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

        var existingCats = (await catRepo.GetAllAsync()).ToDictionary(c => c.Id);

        foreach (var c in backup.Categories)
        {
            var entity = new Category
            {
                Id = c.Id, Name = c.Name, SortOrder = c.SortOrder,
                BackgroundColorHex = c.BackgroundColorHex,
                Type = (CategoryType)c.Type,
            };
            if (existingCats.ContainsKey(c.Id))
                await catRepo.UpdateAsync(entity);
            else
                await catRepo.AddAsync(entity);
        }

        var existingPromptIds = new HashSet<Guid>();
        foreach (var c in backup.Categories)
        {
            var ps = await promptRepo.GetByCategoryAsync(c.Id);
            foreach (var p in ps) existingPromptIds.Add(p.Id);
        }

        foreach (var p in backup.Prompts)
        {
            var entity = new Prompt
            {
                Id = p.Id, CategoryId = p.CategoryId,
                ShortLabel = p.ShortLabel, OriginalText = p.OriginalText,
                ImprovedText = p.ImprovedText,
                ActiveVersion = (PromptVersion)p.ActiveVersion,
                IsAlwaysOn = p.IsAlwaysOn, SortOrder = p.SortOrder,
            };
            if (existingPromptIds.Contains(p.Id))
                await promptRepo.UpdateAsync(entity);
            else
                await promptRepo.AddAsync(entity);
        }
    }

    private async Task ExportAsync()
    {
        var dlg = new SaveFileDialog
        {
            Title = "PromptBoard-Backup speichern",
            Filter = "JSON-Datei (*.json)|*.json",
            FileName = $"promptboard-backup-{DateTime.Now:yyyyMMdd-HHmm}.json",
        };
        if (dlg.ShowDialog(this) != true) return;

        try
        {
            string json = await BuildBackupJsonAsync();
            await File.WriteAllTextAsync(dlg.FileName, json);
            MessageBox.Show($"Backup gespeichert: {dlg.FileName}", "PromptBoard",
                MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Export fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    private async Task ImportAsync()
    {
        var dlg = new OpenFileDialog
        {
            Title = "PromptBoard-Backup einlesen",
            Filter = "JSON-Datei (*.json)|*.json",
        };
        if (dlg.ShowDialog(this) != true) return;

        if (!ConfirmDialog.Ask(this, "Import bestaetigen",
            "Vorhandene Kategorien und Prompts mit gleicher ID werden ueberschrieben. Neue werden angelegt.",
            "Importieren")) return;

        try
        {
            string json = await File.ReadAllTextAsync(dlg.FileName);
            await ApplyBackupJsonAsync(json);
            MessageBox.Show("Import abgeschlossen.", "PromptBoard",
                MessageBoxButton.OK, MessageBoxImage.Information);
            await RefreshAsync();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"Import fehlgeschlagen: {ex.Message}", "Fehler",
                MessageBoxButton.OK, MessageBoxImage.Error);
        }
    }

    // ──────────────── Backup DTOs ────────────────

    private sealed class BackupData
    {
        public DateTime ExportedAt { get; set; }
        public List<BackupCategory> Categories { get; set; } = new();
        public List<BackupPrompt> Prompts { get; set; } = new();
        public string SeparatorTemplate { get; set; } = " ; ";
    }

    private sealed class BackupCategory
    {
        public Guid Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public int SortOrder { get; set; }
        public string BackgroundColorHex { get; set; } = "#DCEDEC";
        public int Type { get; set; }
    }

    private sealed class BackupPrompt
    {
        public Guid Id { get; set; }
        public Guid CategoryId { get; set; }
        public string ShortLabel { get; set; } = string.Empty;
        public string OriginalText { get; set; } = string.Empty;
        public string? ImprovedText { get; set; }
        public int ActiveVersion { get; set; }
        public bool IsAlwaysOn { get; set; }
        public int SortOrder { get; set; }
    }
}

internal static class PromptBoardPanelHelpers
{
    public static System.Windows.Shapes.Ellipse BuildAOIndicator(bool isAlwaysOn) => new()
    {
        Width = 8,
        Height = 8,
        VerticalAlignment = VerticalAlignment.Center,
        HorizontalAlignment = HorizontalAlignment.Center,
        Fill = isAlwaysOn
            ? new SolidColorBrush(Color.FromRgb(0xFF, 0xD7, 0x00))
            : new SolidColorBrush(Color.FromRgb(0x3A, 0x3A, 0x3A)),
    };
}

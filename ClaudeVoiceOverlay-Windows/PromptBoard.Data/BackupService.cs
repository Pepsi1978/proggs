using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Core.Services;

namespace PromptBoard.Data;

/// <summary>
/// Implements the backup/restore orchestration directly against the
/// DbContext — one round-trip for Create, one transaction for Apply.
/// </summary>
public sealed class BackupService : IBackupService
{
    private const int CurrentSchemaVersion = 1;

    private readonly PromptBoardDbContext _db;
    private readonly ILogger<BackupService> _logger;

    public BackupService(PromptBoardDbContext db, ILogger<BackupService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task<BackupDocument> CreateAsync(CancellationToken ct = default)
    {
        List<Category> categories = await _db.Categories
            .AsNoTracking()
            .OrderBy(c => c.SortOrder)
            .ToListAsync(ct);

        List<Prompt> prompts = await _db.Prompts
            .AsNoTracking()
            .OrderBy(p => p.SortOrder)
            .ToListAsync(ct);

        var categoryDtos = categories.Select(c => new CategoryDto(
            c.Id, c.Name, c.BackgroundColorHex, c.SortOrder, c.Type,
            c.CreatedAt, c.UpdatedAt)).ToList();

        var promptDtos = prompts.Select(ToDto).ToList();

        _logger.LogInformation(
            "Backup document built: {Cat} categories, {Prm} prompts.",
            categoryDtos.Count, promptDtos.Count);

        return new BackupDocument(
            SchemaVersion: CurrentSchemaVersion,
            CreatedAtUtc: DateTime.UtcNow,
            AppVersion: typeof(BackupService).Assembly.GetName().Version?.ToString(),
            Categories: categoryDtos,
            Prompts: promptDtos);
    }

    public async Task<RestoreResult> ApplyAsync(BackupDocument document, RestoreMode mode, CancellationToken ct = default)
    {
        ArgumentNullException.ThrowIfNull(document);

        if (document.SchemaVersion > CurrentSchemaVersion)
        {
            throw new InvalidOperationException(
                $"Backup-Schema {document.SchemaVersion} ist neuer als diese App-Version kennt ({CurrentSchemaVersion}).");
        }

        await using var tx = await _db.Database.BeginTransactionAsync(ct);

        RestoreResult result = mode switch
        {
            RestoreMode.Replace => await ApplyReplaceAsync(document, ct),
            RestoreMode.Merge => await ApplyMergeAsync(document, ct),
            _ => throw new ArgumentOutOfRangeException(nameof(mode)),
        };

        await tx.CommitAsync(ct);

        _logger.LogInformation(
            "Restore ({Mode}) done: cats +{CA}/~{CU}/-{CR}, prompts +{PA}/~{PU}/-{PR}.",
            mode,
            result.CategoriesAdded, result.CategoriesUpdated, result.CategoriesRemoved,
            result.PromptsAdded, result.PromptsUpdated, result.PromptsRemoved);

        return result;
    }

    private async Task<RestoreResult> ApplyReplaceAsync(BackupDocument document, CancellationToken ct)
    {
        int removedCategories = await _db.Categories.CountAsync(ct);
        int removedPrompts = await _db.Prompts.CountAsync(ct);

        // Cascade: deleting the category also deletes its prompts via FK.
        await _db.Prompts.ExecuteDeleteAsync(ct);
        await _db.Categories.ExecuteDeleteAsync(ct);

        foreach (CategoryDto c in document.Categories)
        {
            _db.Categories.Add(new Category
            {
                Id = c.Id,
                Name = c.Name,
                BackgroundColorHex = c.BackgroundColorHex,
                SortOrder = c.SortOrder,
                Type = c.Type,
                CreatedAt = c.CreatedAtUtc,
                UpdatedAt = c.UpdatedAtUtc,
            });
        }

        foreach (PromptDto p in document.Prompts)
        {
            _db.Prompts.Add(FromDto(p));
        }

        await _db.SaveChangesAsync(ct);

        return new RestoreResult(
            CategoriesAdded: document.Categories.Count,
            CategoriesUpdated: 0,
            CategoriesRemoved: removedCategories,
            PromptsAdded: document.Prompts.Count,
            PromptsUpdated: 0,
            PromptsRemoved: removedPrompts);
    }

    private async Task<RestoreResult> ApplyMergeAsync(BackupDocument document, CancellationToken ct)
    {
        int catsAdded = 0, catsUpdated = 0, promptsAdded = 0, promptsUpdated = 0;

        Dictionary<Guid, Category> existingCategories = await _db.Categories
            .ToDictionaryAsync(c => c.Id, ct);

        foreach (CategoryDto c in document.Categories)
        {
            if (existingCategories.TryGetValue(c.Id, out Category? existing))
            {
                existing.Name = c.Name;
                existing.BackgroundColorHex = c.BackgroundColorHex;
                existing.SortOrder = c.SortOrder;
                existing.Type = c.Type;
                existing.UpdatedAt = c.UpdatedAtUtc;
                catsUpdated++;
            }
            else
            {
                _db.Categories.Add(new Category
                {
                    Id = c.Id,
                    Name = c.Name,
                    BackgroundColorHex = c.BackgroundColorHex,
                    SortOrder = c.SortOrder,
                    Type = c.Type,
                    CreatedAt = c.CreatedAtUtc,
                    UpdatedAt = c.UpdatedAtUtc,
                });
                catsAdded++;
            }
        }

        Dictionary<Guid, Prompt> existingPrompts = await _db.Prompts
            .ToDictionaryAsync(p => p.Id, ct);

        foreach (PromptDto p in document.Prompts)
        {
            if (existingPrompts.TryGetValue(p.Id, out Prompt? existing))
            {
                existing.CategoryId = p.CategoryId;
                existing.ShortLabel = p.ShortLabel;
                existing.OriginalText = p.OriginalText;
                existing.ImprovedText = p.ImprovedText;
                existing.ActiveVersion = p.ActiveVersion;
                existing.IsAlwaysOn = p.IsAlwaysOn;
                existing.SortOrder = p.SortOrder;
                existing.ImprovedByAiPromptId = p.ImprovedByAiPromptId;
                existing.UpdatedAt = p.UpdatedAtUtc;

                if (existing is AiImprovementPrompt existingAi && p.IsAiImprovementPrompt)
                {
                    existingAi.GeminiModel = p.GeminiModel ?? "gemini-3.1-flash-lite";
                    existingAi.IsActiveForImprovement = p.IsActiveForImprovement;
                }

                promptsUpdated++;
            }
            else
            {
                _db.Prompts.Add(FromDto(p));
                promptsAdded++;
            }
        }

        await _db.SaveChangesAsync(ct);

        return new RestoreResult(
            CategoriesAdded: catsAdded,
            CategoriesUpdated: catsUpdated,
            CategoriesRemoved: 0,
            PromptsAdded: promptsAdded,
            PromptsUpdated: promptsUpdated,
            PromptsRemoved: 0);
    }

    private static PromptDto ToDto(Prompt p)
    {
        bool isAi = p is AiImprovementPrompt;
        string? model = (p as AiImprovementPrompt)?.GeminiModel;
        bool active = (p as AiImprovementPrompt)?.IsActiveForImprovement ?? false;

        return new PromptDto(
            Id: p.Id,
            CategoryId: p.CategoryId,
            ShortLabel: p.ShortLabel,
            OriginalText: p.OriginalText,
            ImprovedText: p.ImprovedText,
            ActiveVersion: p.ActiveVersion,
            IsAlwaysOn: p.IsAlwaysOn,
            SortOrder: p.SortOrder,
            ImprovedByAiPromptId: p.ImprovedByAiPromptId,
            IsAiImprovementPrompt: isAi,
            GeminiModel: model,
            IsActiveForImprovement: active,
            CreatedAtUtc: p.CreatedAt,
            UpdatedAtUtc: p.UpdatedAt,
            HotkeyNumber: p.HotkeyNumber,
            HotkeyLetter: p.HotkeyLetter);
    }

    private static Prompt FromDto(PromptDto p)
    {
        if (p.IsAiImprovementPrompt)
        {
            return new AiImprovementPrompt
            {
                Id = p.Id,
                CategoryId = p.CategoryId,
                ShortLabel = p.ShortLabel,
                OriginalText = p.OriginalText,
                ImprovedText = p.ImprovedText,
                ActiveVersion = p.ActiveVersion,
                IsAlwaysOn = p.IsAlwaysOn,
                SortOrder = p.SortOrder,
                ImprovedByAiPromptId = p.ImprovedByAiPromptId,
                GeminiModel = p.GeminiModel ?? "gemini-3.1-flash-lite",
                IsActiveForImprovement = p.IsActiveForImprovement,
                CreatedAt = p.CreatedAtUtc,
                UpdatedAt = p.UpdatedAtUtc,
            };
        }

        return new Prompt
        {
            Id = p.Id,
            CategoryId = p.CategoryId,
            ShortLabel = p.ShortLabel,
            OriginalText = p.OriginalText,
            ImprovedText = p.ImprovedText,
            ActiveVersion = p.ActiveVersion,
            IsAlwaysOn = p.IsAlwaysOn,
            SortOrder = p.SortOrder,
            ImprovedByAiPromptId = p.ImprovedByAiPromptId,
            HotkeyNumber = p.HotkeyNumber,
            HotkeyLetter = p.HotkeyLetter,
            CreatedAt = p.CreatedAtUtc,
            UpdatedAt = p.UpdatedAtUtc,
        };
    }
}

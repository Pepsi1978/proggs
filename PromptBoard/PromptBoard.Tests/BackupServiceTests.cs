using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Core.Services;
using PromptBoard.Data;

namespace PromptBoard.Tests;

public class BackupServiceTests
{
    [Fact]
    public async Task CreateAsync_ProducesDocumentWithAllEntities()
    {
        using var db = NewDb();
        Category cat = new() { Name = "Test", BackgroundColorHex = "#EEE", Type = CategoryType.Standard, SortOrder = 0 };
        db.Categories.Add(cat);
        db.SaveChanges();

        db.Prompts.Add(new Prompt
        {
            CategoryId = cat.Id,
            ShortLabel = "P1",
            OriginalText = "Hello",
            SortOrder = 0,
        });
        db.Prompts.Add(new AiImprovementPrompt
        {
            CategoryId = cat.Id,
            ShortLabel = "Meta",
            OriginalText = "rewrite",
            GeminiModel = "gemini-2.5-pro",
            IsActiveForImprovement = true,
            SortOrder = 1,
        });
        db.SaveChanges();

        var sut = new BackupService(db, NullLogger<BackupService>.Instance);
        BackupDocument doc = await sut.CreateAsync();

        Assert.Equal(1, doc.Categories.Count);
        Assert.Equal(2, doc.Prompts.Count);
        PromptDto ai = doc.Prompts.Single(p => p.IsAiImprovementPrompt);
        Assert.Equal("gemini-2.5-pro", ai.GeminiModel);
        Assert.True(ai.IsActiveForImprovement);
    }

    [Fact]
    public async Task ApplyAsync_Replace_ClearsAndRepopulates()
    {
        using var seedDb = NewDb();
        Category existing = new() { Name = "Bleibt nicht", BackgroundColorHex = "#EEE", Type = CategoryType.Standard };
        seedDb.Categories.Add(existing);
        seedDb.SaveChanges();
        seedDb.Prompts.Add(new Prompt { CategoryId = existing.Id, ShortLabel = "old", OriginalText = "x" });
        seedDb.SaveChanges();

        // Backup with different data.
        BackupDocument doc = new(
            SchemaVersion: 1,
            CreatedAtUtc: DateTime.UtcNow,
            AppVersion: "1.0",
            Categories: new[]
            {
                new CategoryDto(Guid.NewGuid(), "Neu", "#AAA", 0, CategoryType.Standard, DateTime.UtcNow, DateTime.UtcNow),
            },
            Prompts: Array.Empty<PromptDto>());

        var sut = new BackupService(seedDb, NullLogger<BackupService>.Instance);
        RestoreResult result = await sut.ApplyAsync(doc, RestoreMode.Replace);

        Assert.Equal(1, result.CategoriesAdded);
        Assert.Equal(1, result.CategoriesRemoved);
        Assert.Equal(1, result.PromptsRemoved);
        Assert.Equal(1, seedDb.Categories.Count());
        Assert.Equal("Neu", seedDb.Categories.Single().Name);
        Assert.Equal(0, seedDb.Prompts.Count());
    }

    [Fact]
    public async Task ApplyAsync_Merge_AddsNewAndUpdatesExisting_WithoutRemoving()
    {
        using var db = NewDb();
        Guid sharedCatId = Guid.NewGuid();
        db.Categories.Add(new Category
        {
            Id = sharedCatId,
            Name = "Alt",
            BackgroundColorHex = "#EEE",
            Type = CategoryType.Standard,
            SortOrder = 0,
        });
        db.SaveChanges();

        Guid sharedPromptId = Guid.NewGuid();
        db.Prompts.Add(new Prompt
        {
            Id = sharedPromptId,
            CategoryId = sharedCatId,
            ShortLabel = "old label",
            OriginalText = "old text",
            SortOrder = 0,
        });
        db.Prompts.Add(new Prompt
        {
            CategoryId = sharedCatId,
            ShortLabel = "stays",
            OriginalText = "keep me",
            SortOrder = 1,
        });
        db.SaveChanges();

        BackupDocument doc = new(
            SchemaVersion: 1,
            CreatedAtUtc: DateTime.UtcNow,
            AppVersion: "1.0",
            Categories: new[]
            {
                new CategoryDto(sharedCatId, "Neu umbenannt", "#EEE", 0, CategoryType.Standard, DateTime.UtcNow, DateTime.UtcNow),
                new CategoryDto(Guid.NewGuid(), "Zusaetzlich", "#FFF", 1, CategoryType.Project, DateTime.UtcNow, DateTime.UtcNow),
            },
            Prompts: new[]
            {
                new PromptDto(sharedPromptId, sharedCatId, "new label", "new text", null,
                    PromptVersion.Original, false, 0, null, false, null, false, DateTime.UtcNow, DateTime.UtcNow),
                new PromptDto(Guid.NewGuid(), sharedCatId, "brand new", "added", null,
                    PromptVersion.Original, false, 2, null, false, null, false, DateTime.UtcNow, DateTime.UtcNow),
            });

        var sut = new BackupService(db, NullLogger<BackupService>.Instance);
        RestoreResult result = await sut.ApplyAsync(doc, RestoreMode.Merge);

        Assert.Equal(1, result.CategoriesAdded);
        Assert.Equal(1, result.CategoriesUpdated);
        Assert.Equal(0, result.CategoriesRemoved);
        Assert.Equal(1, result.PromptsAdded);
        Assert.Equal(1, result.PromptsUpdated);
        Assert.Equal(0, result.PromptsRemoved);

        Assert.Equal(2, db.Categories.Count());
        Assert.Equal("Neu umbenannt", db.Categories.Single(c => c.Id == sharedCatId).Name);
        Assert.Equal(3, db.Prompts.Count()); // old stays, shared updated, new added
        Assert.Equal("new label", db.Prompts.Single(p => p.Id == sharedPromptId).ShortLabel);
    }

    [Fact]
    public async Task ApplyAsync_SchemaVersionFromFuture_Throws()
    {
        using var db = NewDb();
        var sut = new BackupService(db, NullLogger<BackupService>.Instance);

        var doc = new BackupDocument(
            SchemaVersion: 99,
            CreatedAtUtc: DateTime.UtcNow,
            AppVersion: "1.0",
            Categories: Array.Empty<CategoryDto>(),
            Prompts: Array.Empty<PromptDto>());

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => sut.ApplyAsync(doc, RestoreMode.Replace));
    }

    // -------- helpers --------

    private static PromptBoardDbContext NewDb()
    {
        var options = new DbContextOptionsBuilder<PromptBoardDbContext>()
            .UseSqlite($"Data Source=backup-{Guid.NewGuid():N}.db")
            .Options;
        var db = new PromptBoardDbContext(options);
        db.Database.EnsureCreated();
        return db;
    }
}

using System;
using Microsoft.EntityFrameworkCore;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Data;

namespace PromptBoard.Tests;

public class DbContextSmokeTests
{
    [Fact]
    public void EnsureCreated_RoundtripsCategoryAndPrompt()
    {
        var options = new DbContextOptionsBuilder<PromptBoardDbContext>()
            .UseSqlite($"Data Source=smoke-{Guid.NewGuid():N}.db")
            .Options;

        using var db = new PromptBoardDbContext(options);
        db.Database.EnsureCreated();

        var category = new Category { Name = "Test", BackgroundColorHex = "#EEFFEE", Type = CategoryType.Standard };
        db.Categories.Add(category);
        db.SaveChanges();

        db.Prompts.Add(new Prompt
        {
            CategoryId = category.Id,
            ShortLabel = "Smoke test prompt",
            OriginalText = "Hello world",
            ActiveVersion = PromptVersion.Original,
        });
        db.SaveChanges();

        Assert.Equal(1, db.Categories.Count());
        Assert.Equal(1, db.Prompts.Count());

        db.Database.EnsureDeleted();
    }

    [Fact]
    public void TphDiscriminator_DistinguishesPromptAndAiImprovementPrompt()
    {
        var options = new DbContextOptionsBuilder<PromptBoardDbContext>()
            .UseSqlite($"Data Source=tph-{Guid.NewGuid():N}.db")
            .Options;

        using var db = new PromptBoardDbContext(options);
        db.Database.EnsureCreated();

        var cat = new Category { Name = "AI", BackgroundColorHex = "#FFEEFF", Type = CategoryType.AiLibrary };
        db.Categories.Add(cat);
        db.SaveChanges();

        db.AiImprovementPrompts.Add(new AiImprovementPrompt
        {
            CategoryId = cat.Id,
            ShortLabel = "Make it better",
            OriginalText = "Rewrite the following as a production-grade prompt.",
            GeminiModel = "gemini-2.5-flash",
            IsActiveForImprovement = true,
        });
        db.SaveChanges();

        Assert.Equal(1, db.AiImprovementPrompts.Count());
        Assert.Equal(1, db.Prompts.Count()); // base query also sees it (TPH)

        db.Database.EnsureDeleted();
    }
}

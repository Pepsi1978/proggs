using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using PromptBoard.Core.Enums;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;

namespace PromptBoard.Tests;

public class PromptImprovementServiceTests
{
    [Fact]
    public async Task ImproveAsync_WithoutActiveMetaPrompt_Throws()
    {
        var metaRepo = new Mock<IAiImprovementPromptRepository>();
        metaRepo.Setup(r => r.GetActiveAsync(It.IsAny<CancellationToken>()))
                .ReturnsAsync((AiImprovementPrompt?)null);

        var gemini = new Mock<IGeminiService>(MockBehavior.Strict);

        var sut = new PromptImprovementService(
            metaRepo.Object,
            gemini.Object,
            NullLogger<PromptImprovementService>.Instance);

        await Assert.ThrowsAsync<NoActiveMetaPromptException>(
            () => sut.ImproveAsync("my prompt"));

        gemini.VerifyNoOtherCalls();
    }

    [Fact]
    public async Task ImproveAsync_UsesActiveMetaPromptTextAndModel()
    {
        var active = new AiImprovementPrompt
        {
            ShortLabel = "Meta",
            OriginalText = "Rewrite it clearer.",
            ActiveVersion = PromptVersion.Original,
            GeminiModel = "gemini-2.5-pro",
            IsActiveForImprovement = true,
        };

        var metaRepo = new Mock<IAiImprovementPromptRepository>();
        metaRepo.Setup(r => r.GetActiveAsync(It.IsAny<CancellationToken>()))
                .ReturnsAsync(active);

        var gemini = new Mock<IGeminiService>();
        gemini.Setup(g => g.ImproveAsync(
                    "user prompt",
                    "Rewrite it clearer.",
                    "gemini-2.5-pro",
                    It.IsAny<CancellationToken>()))
              .ReturnsAsync("improved!");

        var sut = new PromptImprovementService(
            metaRepo.Object,
            gemini.Object,
            NullLogger<PromptImprovementService>.Instance);

        string result = await sut.ImproveAsync("user prompt");
        Assert.Equal("improved!", result);
        gemini.Verify(
            g => g.ImproveAsync("user prompt", "Rewrite it clearer.", "gemini-2.5-pro", It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ImproveAsync_UsesImprovedMetaText_WhenActiveVersionIsImproved()
    {
        var active = new AiImprovementPrompt
        {
            ShortLabel = "Meta",
            OriginalText = "Original meta",
            ImprovedText = "Refined meta",
            ActiveVersion = PromptVersion.Improved,
            GeminiModel = "gemini-2.5-flash",
        };

        var metaRepo = new Mock<IAiImprovementPromptRepository>();
        metaRepo.Setup(r => r.GetActiveAsync(It.IsAny<CancellationToken>()))
                .ReturnsAsync(active);

        var gemini = new Mock<IGeminiService>();
        gemini.Setup(g => g.ImproveAsync(
                    It.IsAny<string>(),
                    "Refined meta",
                    It.IsAny<string>(),
                    It.IsAny<CancellationToken>()))
              .ReturnsAsync("ok");

        var sut = new PromptImprovementService(
            metaRepo.Object,
            gemini.Object,
            NullLogger<PromptImprovementService>.Instance);

        string result = await sut.ImproveAsync("anything");
        Assert.Equal("ok", result);
        gemini.Verify(
            g => g.ImproveAsync("anything", "Refined meta", "gemini-2.5-flash", It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task ImproveAsync_FallsBackToDefaultModel_WhenMetaModelEmpty()
    {
        var active = new AiImprovementPrompt
        {
            ShortLabel = "Meta",
            OriginalText = "Meta text",
            GeminiModel = string.Empty,
        };

        var metaRepo = new Mock<IAiImprovementPromptRepository>();
        metaRepo.Setup(r => r.GetActiveAsync(It.IsAny<CancellationToken>()))
                .ReturnsAsync(active);

        var gemini = new Mock<IGeminiService>();
        gemini.Setup(g => g.ImproveAsync(
                    It.IsAny<string>(),
                    It.IsAny<string>(),
                    "gemini-2.5-flash",
                    It.IsAny<CancellationToken>()))
              .ReturnsAsync("ok");

        var sut = new PromptImprovementService(
            metaRepo.Object,
            gemini.Object,
            NullLogger<PromptImprovementService>.Instance);

        await sut.ImproveAsync("user");
        gemini.Verify(
            g => g.ImproveAsync("user", "Meta text", "gemini-2.5-flash", It.IsAny<CancellationToken>()),
            Times.Once);
    }
}

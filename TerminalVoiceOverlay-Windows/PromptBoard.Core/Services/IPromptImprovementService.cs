using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.Core.Services;

/// <summary>
/// Facade over the "Mit KI verbessern" flow: looks up the currently
/// active AiImprovementPrompt and calls Gemini with its text as system
/// instruction. The UI only has to call ImproveAsync(userPrompt).
/// </summary>
public interface IPromptImprovementService
{
    Task<string> ImproveAsync(string userPrompt, CancellationToken ct = default);
}

public sealed class NoActiveMetaPromptException : Exception
{
    public NoActiveMetaPromptException()
        : base("Keine aktive KI-Verbesserungs-Regel. Lege in einer KI-Bibliotheks-Kategorie einen Meta-Prompt an.") { }
}

public sealed class PromptImprovementService : IPromptImprovementService
{
    private readonly IAiImprovementPromptRepository _metaRepo;
    private readonly IGeminiService _gemini;
    private readonly ILogger<PromptImprovementService> _logger;

    public PromptImprovementService(
        IAiImprovementPromptRepository metaRepo,
        IGeminiService gemini,
        ILogger<PromptImprovementService> logger)
    {
        _metaRepo = metaRepo;
        _gemini = gemini;
        _logger = logger;
    }

    public async Task<string> ImproveAsync(string userPrompt, CancellationToken ct = default)
    {
        AiImprovementPrompt? active = await _metaRepo.GetActiveAsync(ct);
        if (active is null)
        {
            throw new NoActiveMetaPromptException();
        }

        string metaText = active.EffectiveText();
        string model = string.IsNullOrWhiteSpace(active.GeminiModel) ? "gemini-3.1-flash-lite" : active.GeminiModel;

        _logger.LogInformation(
            "Invoking Gemini improvement with meta-prompt {MetaId} ({MetaLabel}) on model {Model}.",
            active.Id, active.ShortLabel, model);

        return await _gemini.ImproveAsync(userPrompt, metaText, model, ct);
    }
}

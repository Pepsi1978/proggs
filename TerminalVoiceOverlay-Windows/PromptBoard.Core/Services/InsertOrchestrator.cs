using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.Core.Services;

/// <summary>
/// Default orchestrator: reads the always-on list and separator, builds
/// the chain via <see cref="IPromptChainBuilder"/>, and hands it off to
/// <see cref="ITextInjectionService"/>. Lives in Core — no Win32 deps.
/// </summary>
public sealed class InsertOrchestrator : IInsertOrchestrator
{
    private readonly IPromptRepository _prompts;
    private readonly IAppSettingsRepository _settings;
    private readonly IPromptChainBuilder _builder;
    private readonly ITextInjectionService _injection;
    private readonly ILogger<InsertOrchestrator> _logger;

    public InsertOrchestrator(
        IPromptRepository prompts,
        IAppSettingsRepository settings,
        IPromptChainBuilder builder,
        ITextInjectionService injection,
        ILogger<InsertOrchestrator> logger)
    {
        _prompts = prompts;
        _settings = settings;
        _builder = builder;
        _injection = injection;
        _logger = logger;
    }

    public async Task InsertAsync(Guid clickedPromptId, string clickedEffectiveText, CancellationToken ct = default)
    {
        var settings = await _settings.GetAsync(ct);
        var alwaysOn = await _prompts.GetAllAlwaysOnAsync(ct);

        var items = alwaysOn
            .OrderBy(p => p.SortOrder)
            .Select(p => new PromptChainItem(p.Id, p.EffectiveText()));

        string chain = _builder.Build(
            items,
            new PromptChainItem(clickedPromptId, clickedEffectiveText ?? string.Empty),
            settings.SeparatorTemplate);

        if (string.IsNullOrEmpty(chain))
        {
            _logger.LogWarning("Chain builder returned empty text; skipping injection.");
            return;
        }

        try
        {
            await _injection.InjectAsync(chain, ct);
            _logger.LogInformation("Injected chain: {Chars} chars, {Parts} parts.",
                chain.Length, chain.Split(settings.SeparatorTemplate).Length);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to inject text into the active CLI.");
            throw;
        }
    }
}

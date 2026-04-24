using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;

namespace TerminalVoiceOverlay.Services;

/// <inheritdoc cref="IAlwaysOnPrefixService"/>
public sealed class AlwaysOnPrefixService : IAlwaysOnPrefixService
{
    private readonly IServiceProvider _root;
    private readonly IPromptChainBuilder _builder;
    private readonly ILogger<AlwaysOnPrefixService> _logger;

    public AlwaysOnPrefixService(
        IServiceProvider root,
        IPromptChainBuilder builder,
        ILogger<AlwaysOnPrefixService> logger)
    {
        _root = root;
        _builder = builder;
        _logger = logger;
    }

    public async Task<string> BuildAsync(CancellationToken ct = default)
    {
        // Transient DbContext per call — the shared SQLite file is the
        // coordination point between the VTO and the standalone app.
        using var scope = _root.CreateScope();
        var prompts = scope.ServiceProvider.GetRequiredService<IPromptRepository>();
        var settings = scope.ServiceProvider.GetRequiredService<IAppSettingsRepository>();

        var alwaysOn = await prompts.GetAllAlwaysOnAsync(ct);
        if (alwaysOn.Count == 0)
        {
            return string.Empty;
        }

        // Kompakter Multi-Task-Separator identisch zum VTO-Ende (" ; ").
        // Ignoriert AppSettings.SeparatorTemplate absichtlich, damit der
        // Prefix auf einer Zeile bleibt statt Leerzeilen zu erzeugen.
        const string inlineSeparator = " ; ";

        var items = alwaysOn
            .OrderBy(p => p.SortOrder)
            .Select(p => new PromptChainItem(p.Id, p.EffectiveText()));

        string prefix = _builder.Build(items, clicked: null, inlineSeparator);

        _logger.LogDebug("Always-on prefix built: {Count} prompts, {Chars} chars.",
            alwaysOn.Count, prefix.Length);

        return prefix;
    }
}

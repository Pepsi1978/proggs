using System;
using System.Collections.Generic;
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

    public Task<string> BuildAsync(CancellationToken ct = default) => BuildPreAsync(ct);

    public Task<string> BuildPreAsync(CancellationToken ct = default) =>
        // A prompt with neither flag set falls back to Pre so legacy /
        // hand-imported rows aren't silently dropped.
        BuildJoinedAsync(p => p.IsPrePrompt || (!p.IsPrePrompt && !p.IsPostPrompt), "Pre", ct);

    public Task<string> BuildPostAsync(CancellationToken ct = default) =>
        BuildJoinedAsync(p => p.IsPostPrompt, "Post", ct);

    /// <summary>
    /// Optimiertes BuildBothAsync: liest die AlwaysOn-Liste EINMAL aus der DB
    /// und filtert beide Sub-Listen (Pre + Post) in-memory. Frueher rief der
    /// Voice-Submit-Pfad in OverlayWindow.BuildAlwaysOnWrappersAsync zwei
    /// separate Methoden — beide oeffneten einen Scope, holten den Repository
    /// und feuerten einen eigenen GetAllAlwaysOnAsync-Query auf SQLite. Bei
    /// jeder Voice-Aufnahme also 2 DB-Roundtrips zwischen Whisper-Antwort und
    /// Paste-Aktion. Mit BuildBothAsync ist es jetzt ein einziger Roundtrip.
    /// </summary>
    public async Task<(string Pre, string Post)> BuildBothAsync(CancellationToken ct = default)
    {
        using var scope = _root.CreateScope();
        var prompts = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

        var alwaysOn = await prompts.GetAllAlwaysOnAsync(ct);
        if (alwaysOn.Count == 0) return (string.Empty, string.Empty);

        const string inlineSeparator = " ; ";

        // Pre: alles mit IsPrePrompt ODER ohne beide Flags (Legacy-Default).
        var preItems = alwaysOn
            .Where(p => p.IsPrePrompt || (!p.IsPrePrompt && !p.IsPostPrompt))
            .OrderBy(p => p.SortOrder)
            .Select(p => new PromptChainItem(p.Id, p.EffectiveText()))
            .ToList();

        var postItems = alwaysOn
            .Where(p => p.IsPostPrompt)
            .OrderBy(p => p.SortOrder)
            .Select(p => new PromptChainItem(p.Id, p.EffectiveText()))
            .ToList();

        string pre  = preItems.Count  == 0 ? string.Empty : _builder.Build(preItems,  clicked: null, inlineSeparator);
        string post = postItems.Count == 0 ? string.Empty : _builder.Build(postItems, clicked: null, inlineSeparator);

        _logger.LogDebug(
            "Always-on Both built: Pre={PreCount} prompts/{PreChars} chars, Post={PostCount}/{PostChars}.",
            preItems.Count, pre.Length, postItems.Count, post.Length);

        return (pre, post);
    }

    private async Task<string> BuildJoinedAsync(
        Func<Prompt, bool> filter, string label, CancellationToken ct)
    {
        // Transient DbContext per call — the shared SQLite file is the
        // coordination point between the VTO and the standalone app.
        using var scope = _root.CreateScope();
        var prompts = scope.ServiceProvider.GetRequiredService<IPromptRepository>();

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
            .Where(filter)
            .OrderBy(p => p.SortOrder)
            .Select(p => new PromptChainItem(p.Id, p.EffectiveText()))
            .ToList();

        if (items.Count == 0)
        {
            return string.Empty;
        }

        string prefix = _builder.Build(items, clicked: null, inlineSeparator);

        _logger.LogDebug(
            "Always-on {Side} built: {Count} prompts, {Chars} chars.",
            label, items.Count, prefix.Length);

        return prefix;
    }
}

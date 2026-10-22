using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.Data.Repositories;

/// <summary>
/// Targeted ("patch") updates for prompts and categories. Callers pass only the
/// change they want (e.g. <c>p =&gt; p.IsAlwaysOn = false</c>) instead of writing
/// back a cached object with <c>UpdateAsync</c>, which would overwrite every column
/// with possibly stale values (TVO and CVO share the same promptboard.db).
/// Lives here as extension methods because the repository interfaces belong to
/// PromptBoard.Core.
/// </summary>
public static class RepositoryPatchExtensions
{
    public static Task<int> PatchAsync(this IPromptRepository repo, Guid id,
        Action<Prompt> mutate, CancellationToken ct = default)
        => repo.PatchManyAsync(new[] { id }, mutate, ct);

    public static async Task<int> PatchManyAsync(this IPromptRepository repo, IEnumerable<Guid> ids,
        Action<Prompt> mutate, CancellationToken ct = default)
    {
        if (repo is PromptRepository concrete)
            return await concrete.PatchManyAsync(ids, mutate, ct);

        // Fallback for other implementations: reload fresh, mutate, write back.
        var idSet = ids.ToHashSet();
        int count = 0;
        foreach (var p in (await repo.GetAllAsync(ct)).Where(p => idSet.Contains(p.Id)))
        {
            mutate(p);
            await repo.UpdateAsync(p, ct);
            count++;
        }
        return count;
    }

    public static Task<int> PatchAsync(this ICategoryRepository repo, Guid id,
        Action<Category> mutate, CancellationToken ct = default)
        => repo.PatchManyAsync(new[] { id }, mutate, ct);

    public static async Task<int> PatchManyAsync(this ICategoryRepository repo, IEnumerable<Guid> ids,
        Action<Category> mutate, CancellationToken ct = default)
    {
        if (repo is CategoryRepository concrete)
            return await concrete.PatchManyAsync(ids, mutate, ct);

        var idSet = ids.ToHashSet();
        int count = 0;
        foreach (var c in (await repo.GetAllSummaryAsync(ct)).Where(c => idSet.Contains(c.Id)))
        {
            mutate(c);
            await repo.UpdateAsync(c, ct);
            count++;
        }
        return count;
    }
}

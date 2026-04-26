using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.Data.Repositories;

public sealed class AiImprovementPromptRepository : IAiImprovementPromptRepository
{
    private readonly PromptBoardDbContext _db;

    public AiImprovementPromptRepository(PromptBoardDbContext db)
    {
        _db = db;
    }

    public Task<AiImprovementPrompt?> GetActiveAsync(CancellationToken ct = default)
    {
        return _db.AiImprovementPrompts
            .AsNoTracking()
            .FirstOrDefaultAsync(p => p.IsActiveForImprovement, ct);
    }

    /// <summary>
    /// Enforces the single-active invariant in a single transaction:
    /// deactivate everyone, then activate exactly the target (if any).
    /// </summary>
    public async Task SetActiveAsync(Guid id, CancellationToken ct = default)
    {
        await using var tx = await _db.Database.BeginTransactionAsync(ct);

        await _db.AiImprovementPrompts
            .Where(p => p.IsActiveForImprovement)
            .ExecuteUpdateAsync(updates => updates.SetProperty(p => p.IsActiveForImprovement, false), ct);

        if (id != Guid.Empty)
        {
            await _db.AiImprovementPrompts
                .Where(p => p.Id == id)
                .ExecuteUpdateAsync(updates => updates.SetProperty(p => p.IsActiveForImprovement, true), ct);
        }

        await tx.CommitAsync(ct);
    }
}

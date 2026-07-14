using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.Data.Repositories;

public sealed class PromptRepository : IPromptRepository
{
    private readonly PromptBoardDbContext _db;

    public PromptRepository(PromptBoardDbContext db)
    {
        _db = db;
    }

    public async Task<IReadOnlyList<Prompt>> GetByCategoryAsync(Guid categoryId, CancellationToken ct = default)
    {
        return await _db.Prompts
            .AsNoTracking()
            .Where(p => p.CategoryId == categoryId)
            .OrderBy(p => p.SortOrder)
            .ThenBy(p => p.ShortLabel)
            .ToListAsync(ct);
    }

    public async Task<IReadOnlyList<Prompt>> GetAllAlwaysOnAsync(CancellationToken ct = default)
    {
        return await _db.Prompts
            .AsNoTracking()
            .Where(p => p.IsAlwaysOn)
            .OrderBy(p => p.Category.SortOrder)
            .ThenBy(p => p.Category.Name)
            .ThenBy(p => p.SortOrder)
            .ThenBy(p => p.Id)
            .ToListAsync(ct);
    }

    public async Task<IReadOnlyList<Prompt>> GetAllAsync(CancellationToken ct = default)
    {
        // Eine Roundtrip fuer alle Prompts. Sortierung uebernimmt der
        // Aufrufer in-memory weil die UI sowieso pro Kategorie gruppiert.
        return await _db.Prompts
            .AsNoTracking()
            .ToListAsync(ct);
    }

    public async Task AddAsync(Prompt prompt, CancellationToken ct = default)
    {
        _db.Prompts.Add(prompt);
        await _db.SaveChangesAsync(ct);
    }

    public async Task UpdateAsync(Prompt prompt, CancellationToken ct = default)
    {
        Prompt? existing = await _db.Prompts.FirstOrDefaultAsync(p => p.Id == prompt.Id, ct);
        if (existing is null) throw new InvalidOperationException($"Prompt {prompt.Id} existiert nicht.");
        if ((existing is AiImprovementPrompt) != (prompt is AiImprovementPrompt))
            throw new InvalidOperationException($"Prompt {prompt.Id} kann nicht ohne Restore den Typ wechseln.");
        _db.Entry(existing).CurrentValues.SetValues(prompt);
        await _db.SaveChangesAsync(ct);
    }

    public async Task DeleteAsync(Guid id, CancellationToken ct = default)
    {
        Prompt? existing = await _db.Prompts.FindAsync([id], ct);
        if (existing is null)
        {
            return;
        }
        _db.Prompts.Remove(existing);
        await _db.SaveChangesAsync(ct);
    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.Data.Repositories;

public sealed class CategoryRepository : ICategoryRepository
{
    private readonly PromptBoardDbContext _db;

    public CategoryRepository(PromptBoardDbContext db)
    {
        _db = db;
    }

    public async Task<IReadOnlyList<Category>> GetAllAsync(CancellationToken ct = default)
    {
        return await _db.Categories
            .AsNoTracking()
            .Include(c => c.Prompts)
            .OrderBy(c => c.SortOrder)
            .ThenBy(c => c.Name)
            .ToListAsync(ct);
    }

    public async Task<IReadOnlyList<Category>> GetAllSummaryAsync(CancellationToken ct = default)
    {
        // Ohne Include — Aufrufer die nur die Tab-Metadaten (Name, Farbe,
        // SortOrder) brauchen sparen pro Aufruf einen SQL-JOIN ueber alle
        // Prompts. Bei N Prompts und M Kategorien: vorher liefert SQLite
        // N+M Zeilen (JOIN-Auflistung), jetzt nur M.
        return await _db.Categories
            .AsNoTracking()
            .OrderBy(c => c.SortOrder)
            .ThenBy(c => c.Name)
            .ToListAsync(ct);
    }

    public Task<Category?> GetByIdAsync(Guid id, CancellationToken ct = default)
    {
        return _db.Categories
            .Include(c => c.Prompts)
            .FirstOrDefaultAsync(c => c.Id == id, ct);
    }

    public async Task AddAsync(Category category, CancellationToken ct = default)
    {
        _db.Categories.Add(category);
        await _db.SaveChangesAsync(ct);
    }

    public async Task UpdateAsync(Category category, CancellationToken ct = default)
    {
        _db.Categories.Update(category);
        await _db.SaveChangesAsync(ct);
    }

    public async Task DeleteAsync(Guid id, CancellationToken ct = default)
    {
        Category? existing = await _db.Categories.FindAsync([id], ct);
        if (existing is null)
        {
            return;
        }
        _db.Categories.Remove(existing);
        await _db.SaveChangesAsync(ct);
    }
}

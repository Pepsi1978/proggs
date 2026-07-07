using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;

namespace PromptBoard.Data.Repositories;

public sealed class AppSettingsRepository : IAppSettingsRepository
{
    private readonly PromptBoardDbContext _db;

    public AppSettingsRepository(PromptBoardDbContext db)
    {
        _db = db;
    }

    public async Task<AppSettings> GetAsync(CancellationToken ct = default)
    {
        AppSettings? existing = await _db.AppSettings.FirstOrDefaultAsync(ct);
        if (existing is not null)
        {
            return existing;
        }

        AppSettings fresh = new();
        _db.AppSettings.Add(fresh);
        await _db.SaveChangesAsync(ct);
        return fresh;
    }

    public async Task UpdateAsync(AppSettings settings, CancellationToken ct = default)
    {
        _db.AppSettings.Update(settings);
        await _db.SaveChangesAsync(ct);
    }
}

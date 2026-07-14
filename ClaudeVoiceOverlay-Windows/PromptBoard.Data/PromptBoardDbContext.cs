using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using PromptBoard.Core.Models;
using PromptBoard.Data.Configurations;

namespace PromptBoard.Data;

/// <summary>
/// Root database context for PromptBoard.
/// Prompt and AiImprovementPrompt share a single table (TPH).
/// </summary>
public class PromptBoardDbContext : DbContext
{
    private bool _preserveTimestamps;

    public PromptBoardDbContext(DbContextOptions<PromptBoardDbContext> options)
        : base(options)
    {
    }

    public DbSet<Category> Categories => Set<Category>();

    public DbSet<Prompt> Prompts => Set<Prompt>();

    public DbSet<AiImprovementPrompt> AiImprovementPrompts => Set<AiImprovementPrompt>();

    public DbSet<AppSettings> AppSettings => Set<AppSettings>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);
        modelBuilder.ApplyConfiguration(new CategoryConfiguration());
        modelBuilder.ApplyConfiguration(new PromptConfiguration());
        modelBuilder.ApplyConfiguration(new AiImprovementPromptConfiguration());
        modelBuilder.ApplyConfiguration(new AppSettingsConfiguration());
    }

    public override int SaveChanges()
    {
        StampTimestamps();
        return base.SaveChanges();
    }

    public override Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        StampTimestamps();
        return base.SaveChangesAsync(cancellationToken);
    }

    public async Task<int> SaveChangesPreservingTimestampsAsync(CancellationToken cancellationToken = default)
    {
        _preserveTimestamps = true;
        try
        {
            return await base.SaveChangesAsync(cancellationToken);
        }
        finally
        {
            _preserveTimestamps = false;
        }
    }

    private void StampTimestamps()
    {
        if (_preserveTimestamps) return;
        DateTime now = DateTime.UtcNow;
        foreach (var entry in ChangeTracker.Entries<BaseEntity>())
        {
            if (entry.State == EntityState.Added)
            {
                if (entry.Entity.CreatedAt == default)
                {
                    entry.Entity.CreatedAt = now;
                }
                entry.Entity.UpdatedAt = now;
            }
            else if (entry.State == EntityState.Modified)
            {
                entry.Entity.UpdatedAt = now;
            }
        }
    }
}

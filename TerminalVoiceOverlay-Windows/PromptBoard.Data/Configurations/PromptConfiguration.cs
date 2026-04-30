using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using PromptBoard.Core.Models;

namespace PromptBoard.Data.Configurations;

internal sealed class PromptConfiguration : IEntityTypeConfiguration<Prompt>
{
    public void Configure(EntityTypeBuilder<Prompt> b)
    {
        b.ToTable("Prompts");
        b.HasKey(p => p.Id);

        // Table-per-Hierarchy: Prompt and AiImprovementPrompt share one table.
        b.HasDiscriminator<string>("PromptKind")
            .HasValue<Prompt>("Prompt")
            .HasValue<AiImprovementPrompt>("AiImprovementPrompt");

        b.Property(p => p.ShortLabel).HasMaxLength(200).IsRequired();
        b.Property(p => p.OriginalText).IsRequired();
        b.Property(p => p.ImprovedText);
        b.Property(p => p.ActiveVersion).HasConversion<int>();
        b.Property(p => p.IsAlwaysOn);
        // Defaults match the macOS migration so legacy rows on either
        // platform render identically and Drive sync stays idempotent.
        b.Property(p => p.IsPrePrompt).HasDefaultValue(true);
        b.Property(p => p.IsPostPrompt).HasDefaultValue(false);
        b.Property(p => p.SortOrder);
        // Optional Strg+N hotkey (1-9). Indexed because the low-level
        // keyboard hook looks up "which prompt owns Strg+3?" on every
        // keydown — without the index that's a full table scan.
        b.Property(p => p.HotkeyNumber);

        b.HasIndex(p => p.CategoryId);
        b.HasIndex(p => p.IsAlwaysOn);
        b.HasIndex(p => p.HotkeyNumber);
    }
}

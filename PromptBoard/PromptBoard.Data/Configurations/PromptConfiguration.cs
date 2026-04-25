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

        b.HasIndex(p => p.CategoryId);
        b.HasIndex(p => p.IsAlwaysOn);
    }
}

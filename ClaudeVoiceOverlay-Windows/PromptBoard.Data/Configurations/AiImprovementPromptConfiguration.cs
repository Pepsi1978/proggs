using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using PromptBoard.Core.Models;

namespace PromptBoard.Data.Configurations;

internal sealed class AiImprovementPromptConfiguration : IEntityTypeConfiguration<AiImprovementPrompt>
{
    public void Configure(EntityTypeBuilder<AiImprovementPrompt> b)
    {
        // Table name is inherited (TPH). Only declare the extra columns here.
        b.Property(p => p.GeminiModel).HasMaxLength(80).IsRequired();
        b.Property(p => p.IsActiveForImprovement);
        b.HasIndex(p => p.IsActiveForImprovement)
            .IsUnique()
            .HasFilter("IsActiveForImprovement = 1");
    }
}

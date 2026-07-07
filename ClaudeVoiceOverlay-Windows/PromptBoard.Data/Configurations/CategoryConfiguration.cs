using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using PromptBoard.Core.Models;

namespace PromptBoard.Data.Configurations;

internal sealed class CategoryConfiguration : IEntityTypeConfiguration<Category>
{
    public void Configure(EntityTypeBuilder<Category> b)
    {
        b.ToTable("Categories");
        b.HasKey(c => c.Id);

        b.Property(c => c.Name).HasMaxLength(120).IsRequired();
        b.Property(c => c.BackgroundColorHex).HasMaxLength(9).IsRequired();
        b.Property(c => c.Type).HasConversion<int>();
        b.Property(c => c.SortOrder);

        b.HasIndex(c => c.SortOrder);

        b.HasMany(c => c.Prompts)
            .WithOne(p => p.Category)
            .HasForeignKey(p => p.CategoryId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}

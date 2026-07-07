using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using PromptBoard.Core.Models;

namespace PromptBoard.Data.Configurations;

internal sealed class AppSettingsConfiguration : IEntityTypeConfiguration<AppSettings>
{
    public void Configure(EntityTypeBuilder<AppSettings> b)
    {
        b.ToTable("AppSettings");
        b.HasKey(s => s.Id);

        b.Property(s => s.GroqApiKey).HasMaxLength(256);
        b.Property(s => s.GeminiApiKey).HasMaxLength(256);
        b.Property(s => s.GoogleOAuthRefreshToken).HasMaxLength(512);
        b.Property(s => s.GroqModel).HasMaxLength(80).IsRequired();
        b.Property(s => s.SeparatorTemplate).HasMaxLength(64).IsRequired();
    }
}

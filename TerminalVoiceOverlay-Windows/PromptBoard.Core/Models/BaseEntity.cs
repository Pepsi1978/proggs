using System;

namespace PromptBoard.Core.Models;

/// <summary>
/// Common fields for every persisted entity. Audit timestamps are
/// maintained by the DbContext on SaveChanges.
/// </summary>
public abstract class BaseEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

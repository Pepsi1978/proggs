using System;
using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;

namespace PromptBoard.Core.Repositories;

public interface IAiImprovementPromptRepository
{
    /// <summary>
    /// Returns the single active AI improvement prompt, or null if none is active.
    /// </summary>
    Task<AiImprovementPrompt?> GetActiveAsync(CancellationToken ct = default);

    /// <summary>
    /// Activates one prompt and deactivates all others in a single transaction.
    /// Pass <see cref="Guid.Empty"/> to deactivate all.
    /// </summary>
    Task SetActiveAsync(Guid id, CancellationToken ct = default);
}

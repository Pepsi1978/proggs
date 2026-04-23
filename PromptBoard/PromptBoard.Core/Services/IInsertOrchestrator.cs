using System;
using System.Threading;
using System.Threading.Tasks;

namespace PromptBoard.Core.Services;

/// <summary>
/// Single entry point used by the ViewModels when the user clicks a prompt.
/// Orchestrates: fetching the always-on set + separator, building the chain,
/// and delegating to <see cref="ITextInjectionService"/>.
/// </summary>
public interface IInsertOrchestrator
{
    /// <summary>
    /// Insert the chain produced for the given clicked prompt.
    /// </summary>
    /// <param name="clickedPromptId">Id of the prompt the user clicked.</param>
    /// <param name="clickedEffectiveText">Live effective text from the ViewModel
    /// (not re-read from the DB to avoid a race with the auto-persist handler).</param>
    Task InsertAsync(Guid clickedPromptId, string clickedEffectiveText, CancellationToken ct = default);
}

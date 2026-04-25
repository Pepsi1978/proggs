using System.Threading;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Assembles always-on prefix and suffix strings from every AlwaysOn
/// prompt in the shared PromptBoard database. Returns empty when no
/// prompts match. The Pre/Post split (#1820) means each prompt can be
/// front-only, back-only, or both.
/// </summary>
public interface IAlwaysOnPrefixService
{
    /// <summary>Back-compat alias for BuildPreAsync — same signature
    /// and semantics as before the Pre/Post split.</summary>
    Task<string> BuildAsync(CancellationToken ct = default);

    /// <summary>Prompts that should appear BEFORE the dictated text.</summary>
    Task<string> BuildPreAsync(CancellationToken ct = default);

    /// <summary>Prompts that should appear AFTER the dictated text.</summary>
    Task<string> BuildPostAsync(CancellationToken ct = default);
}

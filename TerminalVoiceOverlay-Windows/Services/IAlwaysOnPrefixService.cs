using System.Threading;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Assembles the always-on prefix string from every AlwaysOn prompt in
/// the shared PromptBoard database. Returns an empty string when no
/// always-on prompts exist.
/// </summary>
public interface IAlwaysOnPrefixService
{
    Task<string> BuildAsync(CancellationToken ct = default);
}

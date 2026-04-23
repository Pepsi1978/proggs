using System.Threading;
using System.Threading.Tasks;

namespace PromptBoard.Core.Services;

/// <summary>
/// Sends text into whatever window currently holds keyboard focus
/// (the CLI the user was last typing into). The standard implementation
/// copies the text into the clipboard and simulates Ctrl+V with SendInput.
/// </summary>
public interface ITextInjectionService
{
    Task InjectAsync(string text, CancellationToken ct = default);
}

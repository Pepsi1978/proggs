using System;
using System.Collections.Generic;

namespace PromptBoard.Core.Services;

/// <summary>
/// Pure function: produces the final text that gets inserted into the CLI
/// when the user clicks a prompt. Always-on prompts are concatenated in
/// stable order first; then the clicked prompt is appended — but only if
/// it is not already part of the always-on list (no duplication).
/// </summary>
public interface IPromptChainBuilder
{
    /// <summary>
    /// Build the chain.
    /// </summary>
    /// <param name="alwaysOn">Always-on prompts in the order they should appear.</param>
    /// <param name="clicked">The prompt the user clicked, or null for a preview.</param>
    /// <param name="separator">Template between items. Default recommended: "\n\n;\n\n".</param>
    string Build(IEnumerable<PromptChainItem> alwaysOn, PromptChainItem? clicked, string separator);
}

/// <summary>Input to the chain builder. Keeps it independent from Prompt entity.</summary>
public sealed record PromptChainItem(Guid Id, string Text);

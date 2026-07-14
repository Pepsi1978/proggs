using System;
using System.Collections.Generic;

namespace PromptBoard.Core.Services;

public sealed class PromptChainBuilder : IPromptChainBuilder
{
    public string Build(IEnumerable<PromptChainItem> alwaysOn, PromptChainItem? clicked, string separator)
    {
        ArgumentNullException.ThrowIfNull(alwaysOn);
        separator ??= string.Empty;

        List<string> parts = [];
        HashSet<Guid> seen = [];

        foreach (PromptChainItem item in alwaysOn)
        {
            if (item is null) continue;
            if (!string.IsNullOrWhiteSpace(item.Text) && seen.Add(item.Id))
            {
                parts.Add(item.Text);
            }
        }

        if (clicked is not null && !string.IsNullOrWhiteSpace(clicked.Text) && seen.Add(clicked.Id))
        {
            parts.Add(clicked.Text);
        }

        return string.Join(separator, parts);
    }
}

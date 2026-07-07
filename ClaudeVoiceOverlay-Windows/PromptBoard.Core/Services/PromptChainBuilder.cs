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
            if (seen.Add(item.Id))
            {
                parts.Add(item.Text ?? string.Empty);
            }
        }

        if (clicked is not null && seen.Add(clicked.Id))
        {
            parts.Add(clicked.Text ?? string.Empty);
        }

        return string.Join(separator, parts);
    }
}

using System.Collections.Generic;

namespace PromptBoard.Core.Services;

/// <summary>
/// Generates pastel background colors for category headers.
/// Same input → same output is not guaranteed; use the repository to
/// persist the chosen color, then never re-generate it.
/// </summary>
public interface IPastelColorGenerator
{
    /// <summary>
    /// Pick a fresh pastel color that visually differs from all values in
    /// <paramref name="existingHex"/>. Returns a "#RRGGBB" string.
    /// </summary>
    string NextDistinctColor(IEnumerable<string> existingHex);
}

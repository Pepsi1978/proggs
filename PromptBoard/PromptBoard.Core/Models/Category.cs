using System.Collections.Generic;
using PromptBoard.Core.Enums;

namespace PromptBoard.Core.Models;

/// <summary>
/// A vertical column in the bar. Holds a list of prompts.
/// </summary>
public class Category : BaseEntity
{
    public string Name { get; set; } = string.Empty;

    /// <summary>
    /// Persistent background color for the category header (hex, e.g. "#E8F2FF").
    /// Assigned once when the category is created and never regenerated.
    /// </summary>
    public string BackgroundColorHex { get; set; } = "#F0F0F0";

    /// <summary>Smaller value sorts leftward.</summary>
    public int SortOrder { get; set; }

    public CategoryType Type { get; set; } = CategoryType.Standard;

    public ICollection<Prompt> Prompts { get; set; } = new List<Prompt>();
}

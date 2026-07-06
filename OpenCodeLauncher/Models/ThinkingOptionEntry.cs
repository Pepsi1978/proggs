using System.Text.Json.Serialization;

namespace OpenCodeLauncher.Models;

public sealed class ThinkingOptionEntry
{
    public string Value { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;

    [JsonIgnore]
    public string CommandValue => Value;
}

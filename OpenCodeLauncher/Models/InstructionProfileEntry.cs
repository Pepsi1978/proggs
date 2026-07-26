namespace OpenLauncher.Models;

public sealed class InstructionProfileEntry
{
    public required string Id { get; init; }
    public required string DisplayName { get; init; }
    public required string Description { get; init; }
    public bool IsEnabled { get; init; }
}

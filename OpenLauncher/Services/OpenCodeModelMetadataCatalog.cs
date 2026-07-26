namespace OpenLauncher.Services;

public sealed record OpenCodeModelMetadata(string OpenRouterSlug, int ContextLength);

public static class OpenCodeModelMetadataCatalog
{
    private static readonly Dictionary<string, OpenCodeModelMetadata> ByProviderAndSlug = new(StringComparer.OrdinalIgnoreCase)
    {
        [Key("opencode", "gpt-5-nano")] = new("openai/gpt-5-nano", 400_000),
        [Key("opencode", "deepseek-v4-flash-free")] = new("deepseek/deepseek-v4-flash", 1_048_576),
        [Key("opencode", "mimo-v2.5-free")] = new("xiaomi/mimo-v2.5", 1_048_576),
        [Key("opencode", "nemotron-3-ultra-free")] = new("nvidia/nemotron-3-ultra-550b-a55b:free", 1_000_000),
        [Key("opencode", "north-mini-code-free")] = new("cohere/north-mini-code:free", 256_000),

        [Key("opencode-go", "deepseek-v4-flash")] = new("deepseek/deepseek-v4-flash", 1_048_576),
        [Key("opencode-go", "deepseek-v4-pro")] = new("deepseek/deepseek-v4-pro", 1_048_576),
        [Key("opencode-go", "glm-5.1")] = new("z-ai/glm-5.1", 202_752),
        [Key("opencode-go", "glm-5.2")] = new("z-ai/glm-5.2", 1_048_576),
        [Key("opencode-go", "kimi-k2.6")] = new("moonshotai/kimi-k2.6", 262_144),
        [Key("opencode-go", "kimi-k2.7-code")] = new("moonshotai/kimi-k2.7-code", 262_144),
        [Key("opencode-go", "mimo-v2.5")] = new("xiaomi/mimo-v2.5", 1_048_576),
        [Key("opencode-go", "mimo-v2.5-pro")] = new("xiaomi/mimo-v2.5-pro", 1_048_576),
        [Key("opencode-go", "minimax-m2.7")] = new("minimax/minimax-m2.7", 204_800),
        [Key("opencode-go", "minimax-m3")] = new("minimax/minimax-m3", 1_048_576),
        [Key("opencode-go", "qwen3.6-plus")] = new("qwen/qwen3.6-plus", 1_000_000),
        [Key("opencode-go", "qwen3.7-max")] = new("qwen/qwen3.7-max", 1_000_000),
        [Key("opencode-go", "qwen3.7-plus")] = new("qwen/qwen3.7-plus", 1_000_000),
    };

    public static OpenCodeModelMetadata? Find(string providerId, string slug) =>
        ByProviderAndSlug.TryGetValue(Key(providerId, slug), out var metadata) ? metadata : null;

    private static string Key(string providerId, string slug) => $"{providerId.Trim().ToLowerInvariant()}:{slug.Trim().ToLowerInvariant()}";
}

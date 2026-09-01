using OpenLauncher.Models;

namespace OpenLauncher.Services;

public static class OpenCodeVariantCatalog
{
    private static readonly string[] WidelySupported = ["low", "medium", "high"];
    private static readonly string[] OpenAiGeneric = ["none", "minimal", "low", "medium", "high", "xhigh"];
    private static readonly string[] Gpt52Plus = ["none", "low", "medium", "high", "xhigh"];
    private static readonly string[] Gpt5Codex3Plus = ["none", "low", "medium", "high", "xhigh"];

    public static IReadOnlyList<string> GetLauncherLevels(ModelEntry model)
    {
        var provider = model.ProviderId.ToLowerInvariant();
        var slug = Normalize(model.Slug);

        return provider switch
        {
            "openai" => GetOpenAiLevels(slug),
            "opencode" => GetOpenCodeZenLevels(slug),
            "opencode-go" => GetOpenCodeGoLevels(slug),
            "nvidia" => GetNvidiaLevels(slug),
            "anthropic" => GetAnthropicLevels(slug),
            "openrouter" => GetOpenRouterLevels(slug, KnownOpenRouterReasoning(slug)),
            _ => []
        };
    }

    private static IReadOnlyList<string> GetAnthropicLevels(string slug)
    {
        // "claude-opus-5[1m]" waehlt nur das 1M-Kontextfenster desselben Modells — die
        // Effort-Stufen sind identisch, deshalb das Suffix vor dem Vergleich abschneiden.
        if (slug.EndsWith("[1m]", StringComparison.Ordinal)) slug = slug[..^"[1m]".Length];

        if (slug is "claude-opus-5" or "claude-fable-5-1" or "claude-fable-5" or "claude-opus-4-8" or "claude-opus-4-7" or "claude-sonnet-5" or "claude-haiku-4-5")
            return ["low", "medium", "high", "xhigh", "max"];
        if (slug is "claude-opus-4-6" or "claude-sonnet-4-6") return ["low", "medium", "high", "max"];
        if (slug is "claude-opus-4-5") return ["low", "medium", "high"];
        return [];
    }

    public static IReadOnlyList<string> GetOpenRouterLevels(string slug, bool supportsReasoning, bool supportsReasoningEffort = false)
    {
        var id = Normalize(slug);
        if (!supportsReasoning && !supportsReasoningEffort) return [];

        if (IsGlm52(id)) return ["high", "xhigh"];
        if (id.Contains("grok-3-mini", StringComparison.Ordinal)) return ["low", "high"];
        if (id.StartsWith("openai/", StringComparison.Ordinal) || id.Contains("gpt", StringComparison.Ordinal))
            return GetOpenAiCompatibleLevels(id);
        if (supportsReasoningEffort) return WidelySupported;
        if (id.Contains("minimax", StringComparison.Ordinal)) return [];
        if (IsOpenCodeEarlyReturn(id)) return [];
        if (id.Contains("grok", StringComparison.Ordinal)) return [];

        return WidelySupported;
    }

    private static IReadOnlyList<string> GetOpenAiLevels(string slug)
    {
        if (slug.Contains("-mini", StringComparison.Ordinal) || slug.Contains("-nano", StringComparison.Ordinal)) return [];
        if (slug.Contains("-chat", StringComparison.Ordinal)) return ["medium"];
        if (slug.Contains("pro", StringComparison.Ordinal)) return slug.Contains("gpt-5.", StringComparison.Ordinal)
            ? ["medium", "high", "xhigh"]
            : ["high"];
        if (slug.Contains("codex", StringComparison.Ordinal)) return slug.Contains("gpt-5.3", StringComparison.Ordinal)
            || slug.Contains("gpt-5.4", StringComparison.Ordinal)
            || slug.Contains("gpt-5.5", StringComparison.Ordinal)
                ? Gpt5Codex3Plus
                : WidelySupported;
        if (slug.StartsWith("gpt-5.1", StringComparison.Ordinal)) return ["none", "low", "medium", "high"];
        if (slug.StartsWith("gpt-5.", StringComparison.Ordinal)) return Gpt52Plus;
        if (slug.StartsWith("gpt-5", StringComparison.Ordinal)) return ["minimal", "low", "medium", "high"];
        return [];
    }

    private static IReadOnlyList<string> GetOpenAiCompatibleLevels(string id)
    {
        var local = id.StartsWith("openai/", StringComparison.Ordinal) ? id["openai/".Length..] : id;
        if (local.Contains("-chat", StringComparison.Ordinal)) return ["medium"];
        if (local.Contains("pro", StringComparison.Ordinal)) return local.Contains("gpt-5.", StringComparison.Ordinal)
            ? ["medium", "high", "xhigh"]
            : ["high"];
        if (local.Contains("codex", StringComparison.Ordinal)) return local.Contains("gpt-5.3", StringComparison.Ordinal)
            || local.Contains("gpt-5.4", StringComparison.Ordinal)
            || local.Contains("gpt-5.5", StringComparison.Ordinal)
                ? Gpt5Codex3Plus
                : ["low", "medium", "high", "xhigh"];
        if (local.StartsWith("gpt-5.1", StringComparison.Ordinal)) return ["none", "low", "medium", "high"];
        if (local.StartsWith("gpt-5.", StringComparison.Ordinal)) return Gpt52Plus;
        return OpenAiGeneric;
    }

    private static IReadOnlyList<string> GetOpenCodeZenLevels(string slug)
    {
        if (slug == "gpt-5-nano") return [];
        if (slug == "deepseek-v4-flash-free") return ["low", "medium", "high", "max"];
        if (slug == "mimo-v2.5-free") return WidelySupported;
        if (slug == "nemotron-3-ultra-free") return WidelySupported;
        if (slug == "north-mini-code-free") return ["none", "high"];
        return [];
    }

    private static IReadOnlyList<string> GetOpenCodeGoLevels(string slug)
    {
        if (slug.StartsWith("deepseek-v4-", StringComparison.Ordinal)) return ["low", "medium", "high", "max"];
        if (slug == "glm-5.2") return ["high", "max"];
        if (slug.StartsWith("glm-", StringComparison.Ordinal)) return [];
        if (slug.StartsWith("kimi-", StringComparison.Ordinal)) return [];
        if (slug.StartsWith("mimo-v2.5", StringComparison.Ordinal)) return WidelySupported;
        if (slug == "minimax-m3") return ["none", "thinking"];
        if (slug.StartsWith("minimax-", StringComparison.Ordinal)) return [];
        if (slug.StartsWith("qwen", StringComparison.Ordinal)) return [];
        return [];
    }

    /// <summary>
    /// Stufen der kostenlosen NVIDIA-NIM-Modelle, 1:1 aus deren reasoning_options in models.dev:
    /// "effort" liefert die Stufenliste unveraendert, "toggle" kennt nur Denken an/aus (gleiche
    /// Abbildung wie minimax-m3 im OpenCode-Go-Katalog), ohne reasoning_options gibt es keine Wahl.
    /// Modelle ohne Reasoning fallen ueber den Default auf eine leere Liste.
    /// </summary>
    private static IReadOnlyList<string> GetNvidiaLevels(string slug) => slug switch
    {
        "stepfun-ai/step-3.7-flash" => ["minimal", "low", "medium", "high", "xhigh", "max"],
        "openai/gpt-oss-120b" or "openai/gpt-oss-20b" => WidelySupported,
        "mistralai/mistral-medium-3.5-128b" => ["none", "high"],
        "z-ai/glm-5.2"
            or "minimaxai/minimax-m3"
            or "google/gemma-4-31b-it"
            or "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning"
            or "nvidia/nemotron-3-nano-30b-a3b"
            or "nvidia/llama-3.1-nemotron-ultra-253b-v1"
            or "nvidia/llama-3.3-nemotron-super-49b-v1.5"
            or "nvidia/llama-3.3-nemotron-super-49b-v1"
            or "nvidia/nvidia-nemotron-nano-9b-v2"
            or "nvidia/llama-3.1-nemotron-nano-8b-v1" => ["none", "thinking"],
        _ => []
    };

    private static bool KnownOpenRouterReasoning(string id)
    {
        return IsGlm52(id)
            || id.StartsWith("openai/gpt-oss", StringComparison.Ordinal)
            || id.StartsWith("openai/gpt-5", StringComparison.Ordinal)
            || id.StartsWith("deepseek/deepseek-v4", StringComparison.Ordinal)
            || id.StartsWith("xiaomi/mimo-v2.5", StringComparison.Ordinal)
            || id.Contains("nemotron-3-ultra", StringComparison.Ordinal)
            || id.Contains("nemotron-3-nano-omni", StringComparison.Ordinal)
            || id.Contains("nemotron-3-super", StringComparison.Ordinal)
            || id.Contains("laguna-m", StringComparison.Ordinal)
            || id.Contains("lfm-2.5-1.2b-thinking", StringComparison.Ordinal)
            || id.Contains("gemma-4-31b", StringComparison.Ordinal);
    }

    private static bool IsOpenCodeEarlyReturn(string id)
    {
        return id.Contains("deepseek-chat", StringComparison.Ordinal)
            || id.Contains("deepseek-reasoner", StringComparison.Ordinal)
            || id.Contains("deepseek-r1", StringComparison.Ordinal)
            || id.Contains("deepseek-v3", StringComparison.Ordinal)
            || (id.Contains("glm", StringComparison.Ordinal) && !IsGlm52(id))
            || id.Contains("kimi", StringComparison.Ordinal)
            || id.Contains("k2p", StringComparison.Ordinal)
            || id.Contains("qwen", StringComparison.Ordinal)
            || id.Contains("big-pickle", StringComparison.Ordinal);
    }

    private static bool IsGlm52(string id) => id.Contains("glm-5.2", StringComparison.Ordinal)
        || id.Contains("glm-5-2", StringComparison.Ordinal)
        || id.Contains("glm-5p2", StringComparison.Ordinal);

    private static string Normalize(string value)
    {
        var result = value.Trim().ToLowerInvariant();
        return result.StartsWith("openrouter/", StringComparison.Ordinal) ? result["openrouter/".Length..] : result;
    }
}

import Foundation

/// Welche Thinking-/Effort-Stufen ein Modell anbietet. 1:1-Port von Services/OpenCodeVariantCatalog.cs
/// - reine Tabellenlogik, voellig plattformneutral.
enum OpenCodeVariantCatalog {
    private static let widelySupported = ["low", "medium", "high"]
    private static let openAiGeneric = ["none", "minimal", "low", "medium", "high", "xhigh"]
    private static let gpt52Plus = ["none", "low", "medium", "high", "xhigh"]
    private static let gpt5Codex3Plus = ["none", "low", "medium", "high", "xhigh"]

    /// nil means unknown, [] means explicitly no selectable effort levels.
    static func currentLevels(providerId: String, slug rawSlug: String, forceRefresh: Bool) async throws -> [String]? {
        var request = URLRequest(url: URL(string: "https://models.dev/api.json")!)
        request.timeoutInterval = 30
        request.cachePolicy = forceRefresh ? .reloadIgnoringLocalCacheData : .useProtocolCachePolicy
        if forceRefresh { request.setValue("no-cache", forHTTPHeaderField: "Cache-Control") }
        let (data, response) = try await URLSession.shared.data(for: request)
        try Task.checkCancellation()
        guard let response = response as? HTTPURLResponse, (200..<300).contains(response.statusCode) else {
            throw URLError(.badServerResponse)
        }
        var slug = rawSlug
        if providerId == "anthropic", slug.hasSuffix("[1m]") { slug = String(slug.dropLast(4)) }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let provider = root[providerId] as? [String: Any],
              let models = provider["models"] as? [String: Any],
              let item = models[slug] as? [String: Any] else { return nil }
        guard let options = item["reasoning_options"] as? [[String: Any]] else {
            return (item["reasoning"] as? Bool) == false ? [] : nil
        }
        var levels: [String] = []
        var toggle = false
        for option in options {
            if option["type"] as? String == "toggle" { toggle = true }
            guard option["type"] as? String == "effort", let values = option["values"] as? [String] else { continue }
            for value in values {
                let level = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                if !level.isEmpty, !levels.contains(level) { levels.append(level) }
            }
        }
        if !levels.isEmpty { return levels }
        if toggle { return ["none", "thinking"] }
        return options.isEmpty ? [] : nil
    }

    static func launcherLevels(for model: ModelEntry) -> [String] {
        let provider = model.providerId.lowercased()
        let slug = normalize(model.slug)

        switch provider {
        case "openai": return openAiLevels(slug)
        case "opencode": return openCodeZenLevels(slug)
        case "opencode-go": return openCodeGoLevels(slug)
        case "nvidia": return nvidiaLevels(slug)
        case "anthropic": return anthropicLevels(slug)
        case "openrouter": return openRouterLevels(slug, supportsReasoning: knownOpenRouterReasoning(slug))
        default: return []
        }
    }

    private static func anthropicLevels(_ rawSlug: String) -> [String] {
        // "claude-opus-5[1m]" waehlt nur das 1M-Kontextfenster desselben Modells - die Effort-Stufen
        // sind identisch, deshalb das Suffix vor dem Vergleich abschneiden.
        var slug = rawSlug
        if slug.hasSuffix("[1m]") { slug = String(slug.dropLast("[1m]".count)) }

        if ["claude-opus-5", "claude-fable-5-1", "claude-fable-5", "claude-opus-4-8", "claude-opus-4-7",
            "claude-sonnet-5", "claude-haiku-4-5"].contains(slug) {
            return ["low", "medium", "high", "xhigh", "max"]
        }
        if ["claude-opus-4-6", "claude-sonnet-4-6"].contains(slug) { return ["low", "medium", "high", "max"] }
        if slug == "claude-opus-4-5" { return ["low", "medium", "high"] }
        return []
    }

    static func openRouterLevels(_ slug: String, supportsReasoning: Bool, supportsReasoningEffort: Bool = false) -> [String] {
        let id = normalize(slug)
        if !supportsReasoning && !supportsReasoningEffort { return [] }

        if isGlm52(id) { return ["high", "xhigh"] }
        if id.contains("grok-3-mini") { return ["low", "high"] }
        if id.hasPrefix("openai/") || id.contains("gpt") { return openAiCompatibleLevels(id) }
        if supportsReasoningEffort { return widelySupported }
        if id.contains("minimax") { return [] }
        if isOpenCodeEarlyReturn(id) { return [] }
        if id.contains("grok") { return [] }

        return widelySupported
    }

    private static func openAiLevels(_ slug: String) -> [String] {
        if slug == "gpt-6-astra" { return ["low", "medium", "high", "xhigh", "max"] }
        if slug.contains("-mini") || slug.contains("-nano") { return [] }
        if slug.contains("-chat") { return ["medium"] }
        if slug.contains("pro") {
            return slug.contains("gpt-5.") ? ["medium", "high", "xhigh"] : ["high"]
        }
        if slug.contains("codex") {
            return (slug.contains("gpt-5.3") || slug.contains("gpt-5.4") || slug.contains("gpt-5.5"))
                ? gpt5Codex3Plus : widelySupported
        }
        if slug.hasPrefix("gpt-5.1") { return ["none", "low", "medium", "high"] }
        if slug.hasPrefix("gpt-5.") { return gpt52Plus }
        if slug.hasPrefix("gpt-5") { return ["minimal", "low", "medium", "high"] }
        return []
    }

    private static func openAiCompatibleLevels(_ id: String) -> [String] {
        let local = id.hasPrefix("openai/") ? String(id.dropFirst("openai/".count)) : id
        if local == "gpt-6-astra" { return ["low", "medium", "high", "xhigh", "max"] }
        if local.contains("-chat") { return ["medium"] }
        if local.contains("pro") {
            return local.contains("gpt-5.") ? ["medium", "high", "xhigh"] : ["high"]
        }
        if local.contains("codex") {
            return (local.contains("gpt-5.3") || local.contains("gpt-5.4") || local.contains("gpt-5.5"))
                ? gpt5Codex3Plus : ["low", "medium", "high", "xhigh"]
        }
        if local.hasPrefix("gpt-5.1") { return ["none", "low", "medium", "high"] }
        if local.hasPrefix("gpt-5.") { return gpt52Plus }
        return openAiGeneric
    }

    private static func openCodeZenLevels(_ slug: String) -> [String] {
        switch slug {
        case "gpt-5-nano": return []
        case "deepseek-v4-flash-free": return ["low", "medium", "high", "max"]
        case "mimo-v2.5-free": return widelySupported
        case "nemotron-3-ultra-free": return widelySupported
        case "north-mini-code-free": return ["none", "high"]
        default: return []
        }
    }

    private static func openCodeGoLevels(_ slug: String) -> [String] {
        if slug.hasPrefix("deepseek-v4-") { return ["low", "medium", "high", "max"] }
        if slug == "glm-5.2" { return ["high", "max"] }
        if slug.hasPrefix("glm-") { return [] }
        if slug.hasPrefix("kimi-") { return [] }
        if slug.hasPrefix("mimo-v2.5") { return widelySupported }
        if slug == "minimax-m3" { return ["none", "thinking"] }
        if slug.hasPrefix("minimax-") { return [] }
        if slug.hasPrefix("qwen") { return [] }
        return []
    }

    /// Stufen der kostenlosen NVIDIA-NIM-Modelle, 1:1 aus deren reasoning_options in models.dev:
    /// "effort" liefert die Stufenliste unveraendert, "toggle" kennt nur Denken an/aus (gleiche
    /// Abbildung wie minimax-m3 im OpenCode-Go-Katalog), ohne reasoning_options gibt es keine Wahl.
    private static func nvidiaLevels(_ slug: String) -> [String] {
        switch slug {
        case "stepfun-ai/step-3.7-flash":
            return ["minimal", "low", "medium", "high", "xhigh", "max"]
        case "openai/gpt-oss-120b", "openai/gpt-oss-20b":
            return widelySupported
        case "mistralai/mistral-medium-3.5-128b":
            return ["none", "high"]
        case "z-ai/glm-5.2",
             "minimaxai/minimax-m3",
             "google/gemma-4-31b-it",
             "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",
             "nvidia/nemotron-3-nano-30b-a3b",
             "nvidia/llama-3.1-nemotron-ultra-253b-v1",
             "nvidia/llama-3.3-nemotron-super-49b-v1.5",
             "nvidia/llama-3.3-nemotron-super-49b-v1",
             "nvidia/nvidia-nemotron-nano-9b-v2",
             "nvidia/llama-3.1-nemotron-nano-8b-v1":
            return ["none", "thinking"]
        default:
            return []
        }
    }

    private static func knownOpenRouterReasoning(_ id: String) -> Bool {
        isGlm52(id)
            || id.hasPrefix("openai/gpt-oss")
            || id.hasPrefix("openai/gpt-5")
            || id.hasPrefix("deepseek/deepseek-v4")
            || id.hasPrefix("xiaomi/mimo-v2.5")
            || id.contains("nemotron-3-ultra")
            || id.contains("nemotron-3-nano-omni")
            || id.contains("nemotron-3-super")
            || id.contains("laguna-m")
            || id.contains("lfm-2.5-1.2b-thinking")
            || id.contains("gemma-4-31b")
    }

    private static func isOpenCodeEarlyReturn(_ id: String) -> Bool {
        id.contains("deepseek-chat")
            || id.contains("deepseek-reasoner")
            || id.contains("deepseek-r1")
            || id.contains("deepseek-v3")
            || (id.contains("glm") && !isGlm52(id))
            || id.contains("kimi")
            || id.contains("k2p")
            || id.contains("qwen")
            || id.contains("big-pickle")
    }

    private static func isGlm52(_ id: String) -> Bool {
        id.contains("glm-5.2") || id.contains("glm-5-2") || id.contains("glm-5p2")
    }

    private static func normalize(_ value: String) -> String {
        let result = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return result.hasPrefix("openrouter/") ? String(result.dropFirst("openrouter/".count)) : result
    }
}

struct OpenCodeModelMetadata {
    let openRouterSlug: String
    let contextLength: Int
}

/// Zusatz-Metadaten fuer Direktmodelle (Kontextfenster + der OpenRouter-Slug, ueber den sich
/// Durchsatz-Werte nachschlagen lassen). 1:1-Port von Services/OpenCodeModelMetadataCatalog.cs.
enum OpenCodeModelMetadataCatalog {
    private static let byProviderAndSlug: [String: OpenCodeModelMetadata] = [
        key("opencode", "gpt-5-nano"): OpenCodeModelMetadata(openRouterSlug: "openai/gpt-5-nano", contextLength: 400_000),
        key("opencode", "deepseek-v4-flash-free"): OpenCodeModelMetadata(openRouterSlug: "deepseek/deepseek-v4-flash", contextLength: 1_048_576),
        key("opencode", "mimo-v2.5-free"): OpenCodeModelMetadata(openRouterSlug: "xiaomi/mimo-v2.5", contextLength: 1_048_576),
        key("opencode", "nemotron-3-ultra-free"): OpenCodeModelMetadata(openRouterSlug: "nvidia/nemotron-3-ultra-550b-a55b:free", contextLength: 1_000_000),
        key("opencode", "north-mini-code-free"): OpenCodeModelMetadata(openRouterSlug: "cohere/north-mini-code:free", contextLength: 256_000),

        key("opencode-go", "deepseek-v4-flash"): OpenCodeModelMetadata(openRouterSlug: "deepseek/deepseek-v4-flash", contextLength: 1_048_576),
        key("opencode-go", "deepseek-v4-pro"): OpenCodeModelMetadata(openRouterSlug: "deepseek/deepseek-v4-pro", contextLength: 1_048_576),
        key("opencode-go", "glm-5.1"): OpenCodeModelMetadata(openRouterSlug: "z-ai/glm-5.1", contextLength: 202_752),
        key("opencode-go", "glm-5.2"): OpenCodeModelMetadata(openRouterSlug: "z-ai/glm-5.2", contextLength: 1_048_576),
        key("opencode-go", "kimi-k2.6"): OpenCodeModelMetadata(openRouterSlug: "moonshotai/kimi-k2.6", contextLength: 262_144),
        key("opencode-go", "kimi-k2.7-code"): OpenCodeModelMetadata(openRouterSlug: "moonshotai/kimi-k2.7-code", contextLength: 262_144),
        key("opencode-go", "mimo-v2.5"): OpenCodeModelMetadata(openRouterSlug: "xiaomi/mimo-v2.5", contextLength: 1_048_576),
        key("opencode-go", "mimo-v2.5-pro"): OpenCodeModelMetadata(openRouterSlug: "xiaomi/mimo-v2.5-pro", contextLength: 1_048_576),
        key("opencode-go", "minimax-m2.7"): OpenCodeModelMetadata(openRouterSlug: "minimax/minimax-m2.7", contextLength: 204_800),
        key("opencode-go", "minimax-m3"): OpenCodeModelMetadata(openRouterSlug: "minimax/minimax-m3", contextLength: 1_048_576),
        key("opencode-go", "qwen3.6-plus"): OpenCodeModelMetadata(openRouterSlug: "qwen/qwen3.6-plus", contextLength: 1_000_000),
        key("opencode-go", "qwen3.7-max"): OpenCodeModelMetadata(openRouterSlug: "qwen/qwen3.7-max", contextLength: 1_000_000),
        key("opencode-go", "qwen3.7-plus"): OpenCodeModelMetadata(openRouterSlug: "qwen/qwen3.7-plus", contextLength: 1_000_000)
    ]

    static func find(providerId: String, slug: String) -> OpenCodeModelMetadata? {
        byProviderAndSlug[key(providerId, slug)]
    }

    private static func key(_ providerId: String, _ slug: String) -> String {
        "\(providerId.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()):\(slug.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }
}

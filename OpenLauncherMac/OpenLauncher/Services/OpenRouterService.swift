import Foundation

/// Ruft die Provider-Liste fuer ein OpenRouter-Modell ab.
/// Endpunkt: GET https://openrouter.ai/api/v1/models/{author}/{slug}/endpoints
/// Liefert data.endpoints[] mit provider_name, tag, pricing.prompt/completion (USD/Token),
/// context_length, throughput_last_30m, status, uptime_*.
/// Provider-Slug fuer die OpenCode-Config wird aus tag (vor "/") abgeleitet.
///
/// 1:1-Port von Services/OpenRouterService.cs inklusive der beiden TPS-Fallbacks
/// (Throughput-Chart der Website + Legacy-HTML) und des dauerhaften TPS-Caches.
final class OpenRouterService {
    private static let baseUrl = "https://openrouter.ai/api/v1"
    private static let frontendBaseUrl = "https://openrouter.ai/api/frontend"

    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        // Ohne Timeout blieben haengende Requests bis zum System-Default stehen. 30 s sind
        // grosszuegig fuer /models und den TPS-HTML-Fallback.
        config.timeoutIntervalForRequest = 30
        config.httpAdditionalHeaders = [
            "HTTP-Referer": "https://github.com/Pepsi1978/proggs",
            "X-Title": "OpenLauncher"
        ]
        return URLSession(configuration: config)
    }()

    // Die vollstaendige /models-Antwort ist gross und wird sowohl beim Start (Free-Modelle) als
    // auch bei jedem Modellwechsel (Thinking-Level) gebraucht. Ohne Cache laedt jeder Wechsel die
    // komplette Liste neu. Kurzlebiger Cache (TTL); der Actor ersetzt die SemaphoreSlim aus C# und
    // verhindert paralleles Doppel-Laden, ohne in einem async-Kontext zu sperren (NSLock waere dort
    // in Swift 6 ein Fehler).
    private static let modelsCacheTtl: TimeInterval = 300

    private actor ModelsCache {
        static let shared = ModelsCache()
        private var json: String?
        private var fetched = Date.distantPast

        /// Liefert den zwischengespeicherten Text oder laedt ihn genau einmal nach. Weitere
        /// Aufrufer warten am Actor, statt einen zweiten Request zu starten.
        func value(forceRefresh: Bool, loader: () async throws -> String) async throws -> String {
            if !forceRefresh, let json, Date().timeIntervalSince(fetched) < OpenRouterService.modelsCacheTtl {
                return json
            }
            let loaded = try await loader()
            json = loaded
            fetched = Date()
            return loaded
        }
    }

    private static let throughputCacheLock = NSLock()
    nonisolated(unsafe) private static var throughputCache: [String: Double]?
    private static var throughputCachePath: String {
        (Paths.appSupport as NSString).appendingPathComponent("provider-throughput.json")
    }

    struct CancelledError: Error {}

    // ===================== Oeffentliche API =====================

    /// Liefert den Modell-Anzeigenamen (data.name) und die Provider-Liste.
    func providers(slug: String, isCancelled: @escaping () -> Bool = { false }) async throws -> (displayName: String?, providers: [ProviderEntry]) {
        do {
            guard let url = URL(string: "\(Self.baseUrl)/models/\(slug)/endpoints") else { return (nil, []) }
            let (data, response) = try await Self.session.data(from: url)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw NSError(domain: "OpenRouter", code: (response as? HTTPURLResponse)?.statusCode ?? -1,
                              userInfo: [NSLocalizedDescriptionKey: "HTTP-Fehler beim Abruf der Provider-Liste"])
            }

            let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
            var displayName: String?
            var permaslug: String?
            var providers: [ProviderEntry] = []

            if let dataNode = root["data"] as? [String: Any] {
                displayName = dataNode["name"] as? String
                if let endpoints = dataNode["endpoints"] as? [[String: Any]] {
                    for endpoint in endpoints {
                        if permaslug == nil { permaslug = Self.readPermaslug(endpoint) }
                        if let entry = Self.parseEndpoint(endpoint) { providers.append(entry) }
                    }
                }
            }

            try await Self.enrichProviderMetricsFromWeb(slug: slug, permaslug: permaslug ?? slug,
                                                        providers: providers, isCancelled: isCancelled)
            providers.sort(by: Self.compareByPrice)
            Logger.shared.info("OpenRouterService", "providers", "slug=\(slug) -> \(providers.count) Provider")
            return (displayName, providers)
        } catch {
            Logger.shared.error("OpenRouterService", "providers", "slug=\(slug) fehlgeschlagen: \(error.localizedDescription)")
            throw error
        }
    }

    func freeModels() async throws -> [ModelEntry] {
        do {
            let json = try await Self.fetchModelsJson()
            guard let data = json.data(using: .utf8),
                  let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let items = root["data"] as? [[String: Any]] else { return [] }

            var models: [ModelEntry] = []
            for item in items {
                if let model = Self.parseFreeModel(item) { models.append(model) }
            }
            models.sort { $0.displayName.lowercased() < $1.displayName.lowercased() }
            Logger.shared.info("OpenRouterService", "freeModels", "\(models.count) kostenlose OpenRouter-Modelle geladen")
            return models
        } catch {
            Logger.shared.error("OpenRouterService", "freeModels", "Free-Modellliste fehlgeschlagen: \(error.localizedDescription)")
            throw error
        }
    }

    func thinkingLevels(slug: String, forceRefresh: Bool = false) async throws -> [String]? {
        do {
            let json = try await Self.fetchModelsJson(forceRefresh: forceRefresh)
            guard let data = json.data(using: .utf8),
                  let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let items = root["data"] as? [[String: Any]] else { throw URLError(.cannotParseResponse) }

            for item in items {
                guard let id = item["id"] as? String, id.caseInsensitiveCompare(slug) == .orderedSame else { continue }
                guard item["supported_parameters"] is [String] else { return nil }
                let levels = Self.parseThinkingLevels(item)
                Logger.shared.info("OpenRouterService", "thinkingLevels", "slug=\(slug) -> \(levels.count) Thinking-Level")
                return levels
            }
        } catch {
            Logger.shared.warn("OpenRouterService", "thinkingLevels", "Thinking-Level-Fallback für \(slug): \(error.localizedDescription)")
            throw error
        }
        return nil
    }

    // ===================== /models mit Cache =====================

    private static func fetchModelsJson(forceRefresh: Bool = false) async throws -> String {
        try await ModelsCache.shared.value(forceRefresh: forceRefresh) {
            guard let url = URL(string: "\(baseUrl)/models") else { return "" }
            let (data, response) = try await session.data(from: url)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw NSError(domain: "OpenRouter", code: (response as? HTTPURLResponse)?.statusCode ?? -1,
                              userInfo: [NSLocalizedDescriptionKey: "HTTP-Fehler beim Abruf der Modell-Liste"])
            }
            return String(data: data, encoding: .utf8) ?? ""
        }
    }

    // ===================== Parser =====================

    private static func parseFreeModel(_ item: [String: Any]) -> ModelEntry? {
        guard let id = item["id"] as? String, id.lowercased().hasSuffix(":free") else { return nil }
        guard let pricing = item["pricing"] as? [String: Any] else { return nil }
        let prompt = parseDouble(pricing, "prompt")
        let completion = parseDouble(pricing, "completion")
        if prompt != 0 || completion != 0 { return nil }

        let name = (item["name"] as? String) ?? id
        return ModelEntry(slug: id,
                          displayName: normalizeFreeModelName(name),
                          providerId: "openrouter",
                          providerName: "OpenRouter")
    }

    private static func parseThinkingLevels(_ item: [String: Any]) -> [String] {
        guard let id = item["id"] as? String else { return [] }
        return OpenCodeVariantCatalog.openRouterLevels(
            id,
            supportsReasoning: modelMentionsReasoning(item),
            supportsReasoningEffort: modelSupportsParameter(item, "reasoning_effort"))
    }

    private static func modelSupportsParameter(_ item: [String: Any], _ expected: String) -> Bool {
        guard let parameters = item["supported_parameters"] as? [Any] else { return false }
        return parameters.contains { ($0 as? String)?.caseInsensitiveCompare(expected) == .orderedSame }
    }

    private static func modelMentionsReasoning(_ item: [String: Any]) -> Bool {
        if let parameters = item["supported_parameters"] as? [Any] {
            for parameter in parameters {
                guard let value = parameter as? String else { continue }
                if value.lowercased().contains("reasoning") { return true }
            }
        }
        if let id = item["id"] as? String {
            let lower = id.lowercased()
            return lower.contains("gpt-5") || lower.contains("reasoning") || lower.contains("thinking")
        }
        return false
    }

    private static func normalizeFreeModelName(_ name: String) -> String {
        let pattern = #"\s*\(free\)\s*$"#
        let normalized = (try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]))
            .map { regex -> String in
                let range = NSRange(name.startIndex..., in: name)
                return regex.stringByReplacingMatches(in: name, range: range, withTemplate: " Free")
            } ?? name
        return normalized.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: ": ", with: " ")
    }

    private static func parseEndpoint(_ endpoint: [String: Any]) -> ProviderEntry? {
        let entry = ProviderEntry()
        entry.providerName = (endpoint["provider_name"] as? String) ?? ""
        entry.tag = (endpoint["tag"] as? String) ?? ""
        entry.contextLength = (endpoint["context_length"] as? NSNumber)?.intValue ?? 0
        entry.quantization = (endpoint["quantization"] as? String) ?? ""
        entry.status = (endpoint["status"] as? NSNumber)?.intValue ?? 0
        entry.maxCompletionTokens = (endpoint["max_completion_tokens"] as? NSNumber)?.intValue
        entry.endpointId = (endpoint["id"] as? String) ?? ""
        entry.throughputLast30m = readThroughput(endpoint)
        entry.uptimeLast5m = (endpoint["uptime_last_5m"] as? NSNumber)?.doubleValue

        if let pricing = endpoint["pricing"] as? [String: Any] {
            entry.promptPerToken = parseDouble(pricing, "prompt")
            entry.completionPerToken = parseDouble(pricing, "completion")
            entry.cacheReadPerToken = parseDouble(pricing, "input_cache_read")
            entry.discount = (pricing["discount"] as? NSNumber)?.doubleValue
        }

        // Provider-Slug aus tag (vor "/") ableiten - zuverlaessiger als provider_name-Normalisierung.
        if !entry.tag.isEmpty {
            entry.providerSlug = entry.tag.split(separator: "/").first.map { $0.lowercased() } ?? entry.tag.lowercased()
        } else {
            entry.providerSlug = entry.providerName.lowercased()
                .replacingOccurrences(of: " ", with: "")
                .replacingOccurrences(of: ".", with: "")
        }
        return entry
    }

    private static func readPermaslug(_ endpoint: [String: Any]) -> String? {
        guard let name = endpoint["name"] as? String,
              let separator = name.lastIndex(of: "|") else { return nil }
        var value = String(name[name.index(after: separator)...]).trimmingCharacters(in: .whitespacesAndNewlines)
        if value.lowercased().hasSuffix(":free") { value = String(value.dropLast(":free".count)) }
        return value.contains("/") ? value : nil
    }

    private static func readThroughput(_ endpoint: [String: Any]) -> Double? {
        guard let raw = endpoint["throughput_last_30m"] else { return nil }
        if let number = raw as? NSNumber, !(raw is [String: Any]) { return number.doubleValue }
        guard let object = raw as? [String: Any] else { return nil }
        return readJsonDouble(object, "p50") ?? readJsonDouble(object, "median") ?? readJsonDouble(object, "value")
    }

    // ===================== TPS-Anreicherung =====================

    private static func enrichProviderMetricsFromWeb(slug: String, permaslug: String,
                                                     providers: [ProviderEntry],
                                                     isCancelled: @escaping () -> Bool) async throws {
        if providers.isEmpty { return }
        if providers.allSatisfy({ $0.throughputLast30m != nil }) {
            storeThroughputCache(slug: slug, providers: providers)
            return
        }

        var html: String?
        do {
            if let url = URL(string: "https://openrouter.ai/\(slug)/providers") {
                let (data, _) = try await session.data(from: url)
                html = String(data: data, encoding: .utf8)
            }
        } catch {
            Logger.shared.warn("OpenRouterService", "enrichProviderMetricsFromWeb",
                               "TPS-Providerseite fehlgeschlagen: \(error.localizedDescription)", ["slug": slug])
        }

        if let html {
            await enrichFromThroughputChart(permaslug: permaslug, html: html, providers: providers, isCancelled: isCancelled)
            enrichFromLegacyHtml(html: html, providers: providers)
        }

        storeThroughputCache(slug: slug, providers: providers)
        applyThroughputCache(slug: slug, providers: providers)
    }

    private static func enrichFromThroughputChart(permaslug: String, html: String,
                                                  providers: [ProviderEntry],
                                                  isCancelled: @escaping () -> Bool) async {
        var endpointTags: [String: String] = [:]
        let pattern = #"id\\":\\"(?<id>[0-9a-fA-F-]{36})\\",\\"name\\":\\"[^\\"]+\\".{0,40000}?provider_slug\\":\\"(?<tag>[^\\"]+)\\""#
        if let regex = try? NSRegularExpression(pattern: pattern, options: [.dotMatchesLineSeparators]) {
            let range = NSRange(html.startIndex..., in: html)
            for match in regex.matches(in: html, range: range) {
                guard let id = match.group("id", in: html), let tag = match.group("tag", in: html) else { continue }
                if endpointTags[id] == nil { endpointTags[id] = tag }
            }
        }

        for provider in providers where !provider.endpointId.isEmpty {
            if endpointTags[provider.endpointId] == nil { endpointTags[provider.endpointId] = provider.tag }
        }
        if endpointTags.isEmpty { return }

        let encodedPermaslug = permaslug.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? permaslug
        var json: String?
        for urlString in ["\(frontendBaseUrl)/v1/stats/throughput-comparison?permaslug=\(encodedPermaslug)",
                          "\(frontendBaseUrl)/stats/throughput-comparison?permaslug=\(encodedPermaslug)"] {
            if isCancelled() { return }
            guard let url = URL(string: urlString) else { continue }
            do {
                let (data, response) = try await session.data(from: url)
                guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { continue }
                json = String(data: data, encoding: .utf8)
                break
            } catch {
                // OpenRouter hat diesen Website-Endpunkt bereits zwischen /stats und /v1/stats verschoben.
                continue
            }
        }
        guard let json, let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let series = root["data"] as? [[String: Any]] else { return }

        var latestByEndpoint: [String: Double] = [:]
        for entry in series.reversed() {
            guard let values = entry["y"] as? [String: Any] else { continue }
            for (name, value) in values {
                let endpointId = name.components(separatedBy: "::").first ?? name
                if latestByEndpoint[endpointId] == nil, let number = value as? NSNumber {
                    latestByEndpoint[endpointId] = number.doubleValue
                }
            }
        }

        var throughputByTag: [String: Double] = [:]
        for (endpointId, throughput) in latestByEndpoint {
            if let tag = endpointTags[endpointId] { throughputByTag[tag] = throughput }
        }
        for provider in providers where provider.throughputLast30m == nil {
            if let throughput = throughputByTag[provider.tag] { provider.throughputLast30m = throughput }
        }
    }

    private static func enrichFromLegacyHtml(html: String, providers: [ProviderEntry]) {
        var metrics: [String: Double] = [:]
        let pattern = #"provider_name\\":\\"(?<name>[^\\"]+)\\".*?routing_heuristics\\":\{(?<body>.*?)\}"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.dotMatchesLineSeparators]) else { return }

        let range = NSRange(html.startIndex..., in: html)
        for match in regex.matches(in: html, range: range) {
            guard let name = match.group("name", in: html), metrics[name] == nil,
                  let body = match.group("body", in: html) else { continue }
            if let throughput = readMetric(body, "p50_throughput_30_minutes") ?? readMetric(body, "p50_throughput") {
                metrics[name] = throughput
            }
        }

        for provider in providers where provider.throughputLast30m == nil {
            if let throughput = metrics[provider.providerName] { provider.throughputLast30m = throughput }
        }
    }

    // ===================== TPS-Cache =====================

    private static func storeThroughputCache(slug: String, providers: [ProviderEntry]) {
        throughputCacheLock.lock()
        defer { throughputCacheLock.unlock() }
        ensureThroughputCacheLoaded()

        var changed = false
        for provider in providers {
            guard let value = provider.throughputLast30m else { continue }
            let key = throughputCacheKey(slug: slug, provider: provider)
            if let existing = throughputCache?[key], abs(existing - value) <= 0.001 { continue }
            throughputCache?[key] = value
            changed = true
        }
        if !changed { return }

        guard let cache = throughputCache,
              let data = try? JSONSerialization.data(withJSONObject: cache, options: [.sortedKeys]),
              let json = String(data: data, encoding: .utf8) else { return }
        if !Paths.writeAtomic(json, to: throughputCachePath) {
            Logger.shared.warn("OpenRouterService", "storeThroughputCache", "TPS-Cache konnte nicht gespeichert werden")
        }
    }

    private static func applyThroughputCache(slug: String, providers: [ProviderEntry]) {
        throughputCacheLock.lock()
        defer { throughputCacheLock.unlock() }
        ensureThroughputCacheLoaded()
        for provider in providers where provider.throughputLast30m == nil {
            if let value = throughputCache?[throughputCacheKey(slug: slug, provider: provider)] {
                provider.throughputLast30m = value
            }
        }
    }

    private static func ensureThroughputCacheLoaded() {
        if throughputCache != nil { return }
        if Paths.fileExists(throughputCachePath),
           let data = FileManager.default.contents(atPath: throughputCachePath),
           let parsed = try? JSONSerialization.jsonObject(with: data) as? [String: Double] {
            throughputCache = parsed
        } else {
            throughputCache = [:]
        }
    }

    private static func throughputCacheKey(slug: String, provider: ProviderEntry) -> String {
        let endpoint = provider.tag.trimmingCharacters(in: .whitespaces).isEmpty ? provider.providerName : provider.tag
        return "\(slug.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())|\(endpoint.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())"
    }

    // ===================== Kleinkram =====================

    private static func readMetric(_ body: String, _ name: String) -> Double? {
        let escaped = NSRegularExpression.escapedPattern(for: name)
        guard let regex = try? NSRegularExpression(pattern: "\(escaped)\\\\\":(?<value>[0-9.]+)") else { return nil }
        let range = NSRange(body.startIndex..., in: body)
        guard let match = regex.firstMatch(in: body, range: range),
              let value = match.group("value", in: body) else { return nil }
        return Double(value)
    }

    private static func readJsonDouble(_ parent: [String: Any], _ name: String) -> Double? {
        guard let raw = parent[name] else { return nil }
        if let number = raw as? NSNumber { return number.doubleValue }
        if let text = raw as? String { return Double(text) }
        return nil
    }

    private static func parseDouble(_ parent: [String: Any], _ name: String) -> Double {
        guard let raw = parent[name] else { return 0 }
        if let text = raw as? String { return Double(text) ?? 0 }
        if let number = raw as? NSNumber { return number.doubleValue }
        return 0
    }

    /// Sortiert guenstigsten Input-Preis zuerst; bei Gleichstand Output-Preis, dann groesstes
    /// Kontextfenster - entspricht der OpenRouter-Web-Sortierung.
    private static func compareByPrice(_ a: ProviderEntry, _ b: ProviderEntry) -> Bool {
        if a.inputPerMillion != b.inputPerMillion { return a.inputPerMillion < b.inputPerMillion }
        if a.outputPerMillion != b.outputPerMillion { return a.outputPerMillion < b.outputPerMillion }
        return a.contextLength > b.contextLength
    }
}

extension NSTextCheckingResult {
    /// Benannte Gruppe als String - Ersatz fuer C#s `match.Groups["name"].Value`.
    func group(_ name: String, in text: String) -> String? {
        let range = self.range(withName: name)
        guard range.location != NSNotFound, let swiftRange = Range(range, in: text) else { return nil }
        return String(text[swiftRange])
    }
}

import Foundation

@MainActor
final class EffortRefreshService {
    private let router = OpenRouterService()

    func refresh(_ target: EffortTarget, force: Bool = false) async throws -> EffortSnapshot? {
        var current: EffortSnapshot?
        var problems: [String] = []
        var conflict = false
        if target.access == "codex", target.provider == "openai" {
            do {
                let models = try await CodexResearchService.shared.models()
                if let item = models.first(where: { $0.id == target.model }), !item.efforts.isEmpty {
                    current = EffortSnapshot(levels: item.efforts, source: "OpenAI-Codex-Kontokatalog", canRemove: true)
                }
            } catch {
                try Task.checkCancellation()
                problems.append("Codex-Kontokatalog nicht verfügbar")
            }
        } else {
            do {
                if let levels = try await OpenCodeVariantCatalog.currentLevels(providerId: target.provider, slug: target.model, forceRefresh: force) {
                    current = EffortSnapshot(levels: levels, source: "https://models.dev/api.json", canRemove: true)
                } else { problems.append("models.dev: keine eindeutigen Stufen") }
            } catch {
                try Task.checkCancellation()
                problems.append("models.dev: Abruf fehlgeschlagen")
            }
            if target.provider == "openrouter" {
                do {
                    if let levels = try await router.thinkingLevels(slug: target.model, forceRefresh: force) {
                        if let existing = current, Set(existing.levels) != Set(levels) {
                            conflict = true
                            problems.append("Widerspruch zwischen models.dev und OpenRouter-Parametern")
                        } else if current == nil {
                            current = EffortSnapshot(levels: levels, source: "https://openrouter.ai/api/v1/models (abgeleitete Stufen)")
                        }
                    } else { problems.append("OpenRouter: keine eindeutigen Fähigkeiten") }
                } catch {
                    try Task.checkCancellation()
                    problems.append("OpenRouter: Abruf fehlgeschlagen")
                }
            }
        }
        if let snapshot = current, !snapshot.levels.allSatisfy({ EffortStore.allowed.contains($0) }) {
            problems.append("Unbekannte Effort-Werte; keine Übernahme")
            current = nil
        }
        if conflict { current = nil }
        if current == nil || force {
            do {
                if var research = try await CodexResearchService.shared.research(target, manual: force) {
                    if let existing = current, Set(existing.levels) != Set(research.levels) {
                        problems.append("Webrecherche weicht vom Katalog ab; keine ungeprüfte Zusammenführung")
                    } else {
                        research.canRemove = current?.canRemove ?? false
                        current = research
                    }
                }
            }
            catch {
                try Task.checkCancellation()
                problems.append((error as? ResearchFailure)?.localizedDescription ?? "KI-Recherche fehlgeschlagen; keine Übernahme")
            }
        }
        try Task.checkCancellation()
        let status = current == nil ? "Bisheriger Stand bleibt erhalten; kein neuer Nachweis." : "Aktualisiert; bestehende Stufen geschützt."
        EffortStore.record(target, snapshot: current, status: ([status] + problems).joined(separator: " · "))
        return current ?? (target.access == "codex" ? Self.localCodex(target) : nil)
    }

    static func localCodex(_ target: EffortTarget) -> EffortSnapshot? {
        guard target.access == "codex", target.provider == "openai" else { return nil }
        let home = ProcessInfo.processInfo.environment["CODEX_HOME"] ?? (Paths.home as NSString).appendingPathComponent(".codex")
        let path = URL(fileURLWithPath: home).appendingPathComponent("models_cache.json")
        guard let data = try? Data(contentsOf: path),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let items = root["models"] as? [[String: Any]],
              let item = items.first(where: { $0["slug"] as? String == target.model }),
              let raw = item["supported_reasoning_levels"] as? [Any] else { return nil }
        let levels = raw.compactMap { ($0 as? String) ?? (($0 as? [String: Any])?["effort"] as? String) }
        guard !levels.isEmpty, levels.count == raw.count, levels.allSatisfy({ EffortStore.allowed.contains($0) }) else { return nil }
        let at = (root["fetched_at"] as? String).flatMap { ISO8601DateFormatter().date(from: $0) }
            ?? (try? path.resourceValues(forKeys: [.contentModificationDateKey]))?.contentModificationDate ?? .distantPast
        return EffortSnapshot(levels: levels, source: "Lokaler Codex-Kontokatalog", checkedAt: at)
    }
}

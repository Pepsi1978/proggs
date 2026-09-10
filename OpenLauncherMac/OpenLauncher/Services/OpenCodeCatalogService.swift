import Foundation

/// Liest den von OpenCode selbst verwendeten models.dev-Katalog und liefert die aktiven,
/// kostenlosen Modelle des Providers OpenCode Zen. 1:1-Port von Services/OpenCodeCatalogService.cs.
final class OpenCodeCatalogService {
    private static let catalogUrl = "https://models.dev/api.json"

    func freeZenModels() async throws -> [ModelEntry] {
        let data = try await PublicCatalogHttp.shared.get(Self.catalogUrl, force: true)
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let provider = root["opencode"] as? [String: Any],
              let items = provider["models"] as? [String: Any] else { return [] }

        var models: [ModelEntry] = []
        for (name, value) in items {
            guard let item = value as? [String: Any], Self.isVisible(item), Self.isFree(item) else { continue }
            var id = (item["id"] as? String) ?? name
            if id.lowercased().hasPrefix("opencode/") { id = String(id.dropFirst("opencode/".count)) }
            if id.trimmingCharacters(in: .whitespaces).isEmpty { continue }
            models.append(ModelEntry(slug: id, displayName: (item["name"] as? String) ?? id,
                                     providerId: "opencode", providerName: "OpenCode Zen"))
        }

        models.sort { $0.displayName.lowercased() < $1.displayName.lowercased() }
        Logger.shared.info("OpenCodeCatalogService", "freeZenModels", "\(models.count) kostenlose OpenCode-Zen-Modelle geladen")
        return models
    }

    private static func isVisible(_ item: [String: Any]) -> Bool {
        let status = (item["status"] as? String)?.lowercased()
        return status != "alpha" && status != "deprecated"
    }

    private static func isFree(_ item: [String: Any]) -> Bool {
        guard let cost = item["cost"] as? [String: Any] else { return false }
        return number(cost["input"]) == 0 && number(cost["output"]) == 0
    }

    private static func number(_ value: Any?) -> Double? {
        if let number = value as? NSNumber { return number.doubleValue }
        if let text = value as? String { return Double(text) }
        return nil
    }
}

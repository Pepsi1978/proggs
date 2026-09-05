import Foundation

enum ResearchMode: String, Codable, CaseIterable {
    case disabled, fallback, manual, periodic
    var title: String {
        switch self {
        case .disabled: return "Aus"
        case .fallback: return "Fallback bei Lücken oder Widersprüchen"
        case .manual: return "Nur manuell"
        case .periodic: return "Regelmäßig: benutzte Modelle"
        }
    }
}

struct ResearchSettings: Codable {
    var mode: ResearchMode = .fallback
    var model = ""
    var effort = ""
    var periodHours = 24
}

enum ResearchSettingsService {
    static func load() -> ResearchSettings {
        guard let data = UserDefaults.standard.data(forKey: "research.settings") else { return ResearchSettings() }
        guard let settings = try? JSONDecoder().decode(ResearchSettings.self, from: data),
              (1...720).contains(settings.periodHours) else {
            var disabled = ResearchSettings()
            disabled.mode = .disabled
            return disabled
        }
        return settings
    }

    static func save(_ settings: ResearchSettings) throws {
        guard (1...720).contains(settings.periodHours) else { throw URLError(.badURL) }
        UserDefaults.standard.set(try JSONEncoder().encode(settings), forKey: "research.settings")
    }
}

struct EffortTarget: Codable, Hashable, Sendable {
    let model: String
    let provider: String
    let access: String
    // Length-prefixed components avoid ambiguous slashes and provider aliases in modelString.
    var key: String { [provider, model, access].map { "\($0.utf8.count):\($0)" }.joined() }
    var label: String { "\(provider)/\(model) [\(access)]" }
}

struct EffortSnapshot: Codable, Sendable {
    var levels: [String]
    var source: String
    var checkedAt = Date()
    var canRemove = false
}

struct EffortReport: Codable {
    var target: EffortTarget
    var snapshot: EffortSnapshot?
    var status: String
    var attemptedAt = Date()
}

@MainActor
enum EffortStore {
    static let allowed: Set<String> = ["none", "minimal", "low", "medium", "high", "xhigh", "max", "ultra", "thinking"]
    static var reports: [String: EffortReport] = {
        guard let data = UserDefaults.standard.data(forKey: "effort.reports"),
              let reports = try? JSONDecoder().decode([String: EffortReport].self, from: data) else { return [:] }
        return reports
    }()

    static func record(_ target: EffortTarget, snapshot: EffortSnapshot?, status: String) {
        reports[target.key] = EffortReport(target: target, snapshot: snapshot ?? reports[target.key]?.snapshot, status: status)
        if let data = try? JSONEncoder().encode(reports) { UserDefaults.standard.set(data, forKey: "effort.reports") }
    }

    static func cached(_ target: EffortTarget) -> [String: Any]? {
        UserDefaults.standard.dictionary(forKey: "thinking-levels.v2." + target.key)
    }

    static func save(_ target: EffortTarget, levels: [String], selected: String?) {
        UserDefaults.standard.set(["levels": levels, "selected": selected ?? ""], forKey: "thinking-levels.v2." + target.key)
    }
}

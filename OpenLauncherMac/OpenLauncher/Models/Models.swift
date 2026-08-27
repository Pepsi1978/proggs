import Foundation

// 1:1-Port der Windows-Modelle (OpenLauncher/Models/*.cs).
// Alle Typen sind Referenztypen (class), weil die Oberflaeche - wie unter WPF - dieselben
// Instanzen ueber Listen, Auswahl und Drag&Drop hinweg vergleicht (ReferenceEquals).

/// Ein vom Nutzer gepflegter Modell-Eintrag in einer Launcher-Gruppe.
/// Slug = provider-lokale ID (z.B. "z-ai/glm-5.2" oder "gpt-5.5").
final class ModelEntry: Decodable {
    /// Provider, dessen Modell-IDs den Provider-Namen selbst als Hersteller-Praefix tragen
    /// ("nvidia/nemotron-3-nano-30b-a3b"). Dort darf die Doppel-Praefix-Abwehr NICHT greifen:
    /// OpenCode erwartet "nvidia/nvidia/nemotron-3-nano-30b-a3b" (Provider + vollstaendige ID).
    static let nvidiaProviderId = "nvidia"

    var slug: String = ""
    var displayName: String = ""

    /// Anzeigename wurde von Hand gesetzt (Dialog "Modell bearbeiten"). Dann darf ihn weder der
    /// OpenRouter-API-Name noch der OpenRouterFree-Sync noch die Default-Liste ueberschreiben.
    var hasCustomDisplayName: Bool = false

    /// Eintrag stammt vom Nutzer (hinzugefuegt oder bearbeitet) und darf vom OpenRouterFree-Sync
    /// nicht entfernt werden, nur weil der Slug in der Remote-Liste fehlt.
    var isUserDefined: Bool = false

    /// Ausgeblendete Modelle bleiben vollstaendig in der Registry erhalten und koennen ueber die
    /// Wiederherstellungsansicht jederzeit erneut eingeblendet werden.
    var isHidden: Bool = false

    var providerId: String = "openrouter"
    var providerName: String = "OpenRouter"

    /// Kontextlaenge, mit der dieses Modell GERADE in LM Studio geladen ist; 0 = nicht geladen.
    ///
    /// Bewusst weder dekodiert noch in `jsonNode` geschrieben: der Wert beschreibt den Zustand
    /// EINES Rechners zu EINEM Zeitpunkt, models.json liegt dagegen im Repo und wird von beiden
    /// Launchern geteilt. Er wird bei jedem Abgleich mit LM Studio neu gesetzt.
    var lmStudioLoadedContext: Int = 0

    var modelString: String {
        if providerId.caseInsensitiveCompare(ModelEntry.nvidiaProviderId) != .orderedSame,
           slug.lowercased().hasPrefix("\(providerId.lowercased())/") {
            return slug
        }
        return "\(providerId)/\(slug)"
    }

    init(slug: String = "",
         displayName: String = "",
         providerId: String = "openrouter",
         providerName: String = "OpenRouter",
         hasCustomDisplayName: Bool = false,
         isUserDefined: Bool = false,
         isHidden: Bool = false) {
        self.slug = slug
        self.displayName = displayName
        self.providerId = providerId
        self.providerName = providerName
        self.hasCustomDisplayName = hasCustomDisplayName
        self.isUserDefined = isUserDefined
        self.isHidden = isHidden
    }

    // Schluesselnamen bleiben in PascalCase, damit models.json zwischen Windows und macOS
    // identisch bleibt (dieselbe Datei liegt im Repo und wird von beiden Launchern gelesen).
    enum CodingKeys: String, CodingKey {
        case slug = "Slug"
        case displayName = "DisplayName"
        case hasCustomDisplayName = "HasCustomDisplayName"
        case isUserDefined = "IsUserDefined"
        case isHidden = "IsHidden"
        case providerId = "ProviderId"
        case providerName = "ProviderName"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        slug = (try? c.decode(String.self, forKey: .slug)) ?? ""
        displayName = (try? c.decode(String.self, forKey: .displayName)) ?? ""
        hasCustomDisplayName = (try? c.decode(Bool.self, forKey: .hasCustomDisplayName)) ?? false
        isUserDefined = (try? c.decode(Bool.self, forKey: .isUserDefined)) ?? false
        isHidden = (try? c.decode(Bool.self, forKey: .isHidden)) ?? false
        providerId = (try? c.decode(String.self, forKey: .providerId)) ?? "openrouter"
        providerName = (try? c.decode(String.self, forKey: .providerName)) ?? "OpenRouter"
    }

    /// Schreibt den Eintrag in GENAU der Schluesselreihenfolge, die der Windows-Launcher erzeugt.
    ///
    /// Warum nicht JSONEncoder? Dessen Ausgabe-Reihenfolge ist nicht garantiert - auf Darwin kommt
    /// sie alphabetisch sortiert und mit einem Leerzeichen vor dem Doppelpunkt heraus. models.json
    /// liegt aber im Repo und wird von BEIDEN Launchern geschrieben: jeder Mac-Start haette die
    /// Datei komplett umformatiert und bei jedem Abgleich mit Windows einen Konflikt erzeugt.
    var jsonNode: JSONNode {
        let node = JSONNode.object()
        node["HasCustomDisplayName"] = .bool(hasCustomDisplayName)
        node["IsUserDefined"] = .bool(isUserDefined)
        node["ProviderId"] = .string(providerId)
        node["ProviderName"] = .string(providerName)
        node["Slug"] = .string(slug)
        node["DisplayName"] = .string(displayName)
        node["IsHidden"] = .bool(isHidden)
        return node
    }
}

final class ModelGroupEntry: Decodable {
    var id: String = ""
    var title: String = ""
    var providerId: String = ""
    var providerName: String = ""
    var models: [ModelEntry] = []
    var hiddenModelSlugs: [String] = []
    var knownSyncedModelSlugs: [String] = []
    var isExpanded: Bool = true

    var visibleModelCount: Int { models.filter { !$0.isHidden }.count }
    var headerText: String { "\(title) (\(visibleModelCount))" }

    init(id: String = "",
         title: String = "",
         providerId: String = "",
         providerName: String = "",
         models: [ModelEntry] = [],
         hiddenModelSlugs: [String] = [],
         knownSyncedModelSlugs: [String] = [],
         isExpanded: Bool = true) {
        self.id = id
        self.title = title
        self.providerId = providerId
        self.providerName = providerName
        self.models = models
        self.hiddenModelSlugs = hiddenModelSlugs
        self.knownSyncedModelSlugs = knownSyncedModelSlugs
        self.isExpanded = isExpanded
    }

    enum CodingKeys: String, CodingKey {
        case id = "Id"
        case title = "Title"
        case providerId = "ProviderId"
        case providerName = "ProviderName"
        case models = "Models"
        case hiddenModelSlugs = "HiddenModelSlugs"
        case knownSyncedModelSlugs = "KnownSyncedModelSlugs"
        case isExpanded = "IsExpanded"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = (try? c.decode(String.self, forKey: .id)) ?? ""
        title = (try? c.decode(String.self, forKey: .title)) ?? ""
        providerId = (try? c.decode(String.self, forKey: .providerId)) ?? ""
        providerName = (try? c.decode(String.self, forKey: .providerName)) ?? ""
        models = (try? c.decode([ModelEntry].self, forKey: .models)) ?? []
        hiddenModelSlugs = (try? c.decode([String].self, forKey: .hiddenModelSlugs)) ?? []
        knownSyncedModelSlugs = (try? c.decode([String].self, forKey: .knownSyncedModelSlugs)) ?? []
        isExpanded = (try? c.decode(Bool.self, forKey: .isExpanded)) ?? true
    }

    /// Schluesselreihenfolge exakt wie beim Windows-Launcher (siehe ModelEntry.jsonNode).
    var jsonNode: JSONNode {
        let node = JSONNode.object()
        node["Id"] = .string(id)
        node["Title"] = .string(title)
        node["ProviderId"] = .string(providerId)
        node["ProviderName"] = .string(providerName)
        node["Models"] = .array(models.map(\.jsonNode))
        node["HiddenModelSlugs"] = .array(hiddenModelSlugs.map { JSONNode.string($0) })
        node["KnownSyncedModelSlugs"] = .array(knownSyncedModelSlugs.map { JSONNode.string($0) })
        node["IsExpanded"] = .bool(isExpanded)
        return node
    }
}

/// Ein Provider-Angebot fuer ein Modell (ein Eintrag aus
/// GET /api/v1/models/{author}/{slug}/endpoints -> data.endpoints[]).
/// Preise liegen als USD/Token vor und werden fuer die Anzeige in USD / 1M Tokens umgerechnet.
final class ProviderEntry {
    var providerName: String = ""
    var tag: String = ""
    /// OpenRouter-Provider-Slug (lowercase) fuer provider.order, abgeleitet aus tag.
    var providerSlug: String = ""

    var promptPerToken: Double = 0
    var completionPerToken: Double = 0
    var cacheReadPerToken: Double = 0
    var discount: Double?

    var contextLength: Int = 0
    var maxCompletionTokens: Int?
    var quantization: String = ""

    var endpointId: String = ""
    var throughputLast30m: Double?
    var uptimeLast5m: Double?
    var status: Int = 0

    var inputPerMillion: Double { promptPerToken * 1_000_000.0 }
    var outputPerMillion: Double { completionPerToken * 1_000_000.0 }
    var cacheReadPerMillion: Double { cacheReadPerToken * 1_000_000.0 }

    var cacheReadText: String {
        cacheReadPerToken > 0 ? Format.price(cacheReadPerMillion) : "--"
    }

    var throughputText: String {
        guard let value = throughputLast30m else { return "--" }
        return "\(Format.oneDecimal(value)) tps"
    }

    var statusText: String {
        switch status {
        case 0: return "ok"
        case -2: return "eingeschränkt"
        case -5: return "gestört"
        default: return "Status \(status)"
        }
    }

    var isUp: Bool {
        status >= 0 && !(uptimeLast5m.map { $0 < 80.0 } ?? false)
    }

    init() {}
}

final class ThinkingOptionEntry {
    var value: String = ""
    var displayName: String = ""
    var descriptionText: String = ""
    var commandValue: String { value }

    init(value: String, displayName: String, descriptionText: String) {
        self.value = value
        self.displayName = displayName
        self.descriptionText = descriptionText
    }
}

final class InstructionProfileEntry {
    let id: String
    let displayName: String
    let descriptionText: String
    let isEnabled: Bool

    init(id: String, displayName: String, descriptionText: String, isEnabled: Bool) {
        self.id = id
        self.displayName = displayName
        self.descriptionText = descriptionText
        self.isEnabled = isEnabled
    }
}

final class WorkModeEntry {
    let id: String
    let displayName: String
    let descriptionText: String

    init(id: String, displayName: String, descriptionText: String) {
        self.id = id
        self.displayName = displayName
        self.descriptionText = descriptionText
    }
}

/// Von Hand gespeicherte Startauswahl EINES Modells: Profil, Arbeitsmodus und Effort/Thinking.
/// Wird ueber den Schalter "Standard speichern" im Profil-Bereich angelegt und beim Modellwechsel
/// (und damit auch beim Start) wieder vorausgewaehlt.
final class ModelDefaultEntry {
    var profileId: String = ""
    var workModeId: String = ""
    /// Effort- bzw. Thinking-Stufe als Kleinbuchstaben-Wert ("high", "xhigh", ...). Leer, wenn das
    /// Modell beim Speichern keine Stufe angeboten hat.
    var thinkingValue: String = ""

    init(profileId: String = "", workModeId: String = "", thinkingValue: String = "") {
        self.profileId = profileId
        self.workModeId = workModeId
        self.thinkingValue = thinkingValue
    }

    /// Schluesselreihenfolge exakt wie beim Windows-Launcher (siehe ModelEntry.jsonNode).
    var jsonNode: JSONNode {
        let node = JSONNode.object()
        node["ProfileId"] = .string(profileId)
        node["WorkModeId"] = .string(workModeId)
        node["ThinkingValue"] = .string(thinkingValue)
        return node
    }
}

/// Zahlenformatierung wie in WPF (StringFormat={}{0:0.####} bzw. N0) - immer invariant,
/// damit die Tabelle unabhaengig von der Systemsprache identisch aussieht.
enum Format {
    static func price(_ value: Double) -> String {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.minimumFractionDigits = 0
        f.maximumFractionDigits = 4
        return f.string(from: NSNumber(value: value)) ?? "0"
    }

    static func oneDecimal(_ value: Double) -> String {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.minimumFractionDigits = 0
        f.maximumFractionDigits = 1
        return f.string(from: NSNumber(value: value)) ?? "0"
    }

    static func thousands(_ value: Int) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.locale = Locale(identifier: "de_DE")
        f.maximumFractionDigits = 0
        return f.string(from: NSNumber(value: value)) ?? "\(value)"
    }
}

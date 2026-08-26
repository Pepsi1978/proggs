import Foundation

/// Speichert je Modell eine von Hand festgelegte Startauswahl (Profil, Modus, Effort/Thinking).
/// 1:1-Port von Services/ModelDefaultsService.cs.
///
/// Der Ablageort ist bewusst derselbe wie bei der Modell-Liste: das Repo ~/proggs/OpenLauncher/.
/// Der Inhalt ist modellbezogene Konfiguration ohne Geheimnisse und gehoert damit zum
/// versionierten Bestand - nach Commit und Push steht derselbe Standard auf jedem Rechner bereit
/// (Windows wie macOS lesen dieselbe Datei).
///
/// Schluessel ist die provider-qualifizierte Modell-ID (ModelEntry.modelString), damit gleich
/// benannte Slugs verschiedener Provider nicht kollidieren.
final class ModelDefaultsService {
    private static var dir: String { Paths.repoRoot }
    private static var filePath: String {
        (dir as NSString).appendingPathComponent("model-defaults.json")
    }

    /// Schluessel case-insensitiv wie das .NET-Dictionary (StringComparer.OrdinalIgnoreCase):
    /// intern wird kleingeschrieben abgelegt, die Original-Schreibweise bleibt im Wert erhalten.
    private var defaults: [String: ModelDefaultEntry] = [:]
    private var originalKeys: [String: String] = [:]
    /// Reihenfolge, in der die Standards in der Datei stehen - ein Dictionary allein wuerde sie bei
    /// jedem Schreiben neu wuerfeln und einen unnoetigen Diff gegen die Windows-Fassung erzeugen.
    private var insertionOrder: [String] = []

    init() {
        guard Paths.fileExists(ModelDefaultsService.filePath) else { return }
        // Ueber JSONNode statt JSONDecoder gelesen: ein Swift-Dictionary verliert beim Dekodieren die
        // Reihenfolge, und die Datei wuerde beim naechsten Schreiben umsortiert - sie liegt aber im
        // Repo und wird mit Windows geteilt.
        guard let root = JSONNode.parse(Paths.readText(ModelDefaultsService.filePath)), root.isObject else {
            // Eine unlesbare Datei darf den Start nicht verhindern: dann gilt eben kein Standard.
            Logger.shared.warn("ModelDefaultsService", "load", "Modell-Standards ignoriert: Datei nicht lesbar",
                               ["filePath": ModelDefaultsService.filePath])
            return
        }

        for (key, value) in root.entries where !key.trimmingCharacters(in: .whitespaces).isEmpty {
            guard value.isObject else { continue }
            let entry = ModelDefaultEntry(profileId: value["ProfileId"]?.stringValue ?? "",
                                          workModeId: value["WorkModeId"]?.stringValue ?? "",
                                          thinkingValue: value["ThinkingValue"]?.stringValue ?? "")
            let lower = key.lowercased()
            if defaults[lower] == nil { insertionOrder.append(lower) }
            defaults[lower] = entry
            originalKeys[lower] = key
        }
        Logger.shared.info("ModelDefaultsService", "load", "\(defaults.count) Modell-Standard(s) geladen",
                           ["filePath": ModelDefaultsService.filePath])
    }

    /// Gespeicherter Standard des Modells oder nil, wenn keiner hinterlegt ist.
    func find(_ modelKey: String) -> ModelDefaultEntry? {
        let key = modelKey.trimmingCharacters(in: .whitespacesAndNewlines)
        if key.isEmpty { return nil }
        return defaults[key.lowercased()]
    }

    func save(_ modelKey: String, entry: ModelDefaultEntry) {
        let key = modelKey.trimmingCharacters(in: .whitespacesAndNewlines)
        if key.isEmpty { return }
        let lower = key.lowercased()
        if defaults[lower] == nil { insertionOrder.append(lower) }
        defaults[lower] = entry
        originalKeys[lower] = key
        persist()
    }

    @discardableResult
    func remove(_ modelKey: String) -> Bool {
        let key = modelKey.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard defaults.removeValue(forKey: key) != nil else { return false }
        originalKeys.removeValue(forKey: key)
        insertionOrder.removeAll { $0 == key }
        persist()
        return true
    }

    private func persist() {
        Paths.ensureDirectory(ModelDefaultsService.dir)
        // Wie bei models.json ueber JSONNode, damit die Datei auf Mac und Windows byte-gleich
        // aussieht. Die Reihenfolge der Modelle bleibt dabei die, in der sie angelegt wurden.
        let root = JSONNode.object()
        for key in insertionOrder {
            guard let entry = defaults[key] else { continue }
            root[originalKeys[key] ?? key] = entry.jsonNode
        }
        // Atomar schreiben (Temp + Move) wie bei models.json: ein Absturz mitten im Schreiben darf
        // die Datei nicht abgeschnitten zuruecklassen.
        if !Paths.writeAtomic(root.serialized(), to: ModelDefaultsService.filePath) {
            Logger.shared.error("ModelDefaultsService", "persist", "Modell-Standards nicht schreibbar")
        }
    }
}

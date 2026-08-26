import Foundation

/// Liest die lokal in LM Studio vorhandenen Modelle ueber den OpenAI-kompatiblen Server
/// (GET http://localhost:1234/v1/models) und startet den Server bei Bedarf ueber die
/// mitgelieferte lms-CLI. LM Studio laedt ein angefragtes Modell per JIT-Loading selbst,
/// deshalb reicht es, den Server laufen zu haben - OpenCode spricht ihn dann direkt an.
///
/// 1:1-Port von Services/LmStudioService.cs. Einziger Plattform-Unterschied: die CLI heisst auf
/// macOS `lms` (ohne .exe) und liegt unter ~/.lmstudio/bin/lms.
final class LmStudioService {
    static let providerId = "lmstudio"
    static let providerName = "LM Studio"
    static let baseUrl = "http://localhost:1234/v1"

    static var lmsPath: String {
        (Paths.home as NSString).appendingPathComponent(".lmstudio/bin/lms")
    }

    /// Ist die lms-CLI (und damit LM Studio) auf diesem Rechner installiert?
    static var isInstalled: Bool { FileManager.default.isExecutableFile(atPath: lmsPath) }

    /// Startet den lokalen LM-Studio-Server, falls er nicht schon laeuft. Idempotent -
    /// "lms server start" meldet bei laufendem Server nur Erfolg.
    @discardableResult
    static func ensureServerRunning() -> Bool {
        guard isInstalled else { return false }
        let result = Shell.run(lmsPath, ["server", "start"], timeout: 20)
        Logger.shared.info("LmStudioService", "ensureServerRunning", "lms server start beendet mit \(result.exitCode)")
        return result.exitCode == 0
    }

    /// Kontextlaenge, mit der das Modell in LM Studio gerade geladen ist - also exakt die
    /// Einstellung, die der Benutzer dort gewaehlt hat. 0 bedeutet: nicht geladen. OpenCode rechnet
    /// seine Auslastung gegen diesen Wert; steht in der Konfig eine kleinere Zahl, meldet die
    /// Oberflaeche viel zu frueh einen vollen Kontext und komprimiert endlos.
    static func loadedContextLength(modelId: String) -> Int {
        guard isInstalled, !modelId.trimmingCharacters(in: .whitespaces).isEmpty else { return 0 }
        let result = Shell.run(lmsPath, ["ps", "--json"], timeout: 30)
        guard !result.stdout.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let data = result.stdout.data(using: .utf8),
              let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else { return 0 }

        for item in array {
            let identifier = item["identifier"] as? String
            let modelKey = item["modelKey"] as? String
            let matches = identifier?.caseInsensitiveCompare(modelId) == .orderedSame
                || modelKey?.caseInsensitiveCompare(modelId) == .orderedSame
            if !matches { continue }
            return (item["contextLength"] as? Int) ?? 0
        }
        return 0
    }

    /// Ausgabe-Obergrenze. OpenCode reserviert diesen Wert im Kontextbudget und zeigt ihn als
    /// bereits verbraucht an - eine grosszuegige Obergrenze frisst also sichtbar Kontext, ohne dass
    /// ein einziges Token geschrieben waere. 8192 reicht fuer jede realistische Antwort inklusive
    /// Werkzeugaufrufen; bei kleinen Fenstern wird anteilig gedeckelt.
    static func outputLimit(for contextLength: Int) -> Int {
        Swift.min(Swift.max(Swift.min(8_192, contextLength / 8), 2_048), 8_192)
    }

    /// Holt die Modell-IDs vom laufenden Server. Embedding-Modelle werden aussortiert, sie taugen
    /// nicht als Chat-Modell fuer OpenCode. Leere Liste = Server aus oder keine Modelle.
    func localModels() async -> [ModelEntry] {
        var result: [ModelEntry] = []
        guard let url = URL(string: "\(LmStudioService.baseUrl)/models") else { return result }

        var request = URLRequest(url: url)
        request.timeoutInterval = 5

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { return result }
            guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let items = root["data"] as? [[String: Any]] else { return result }

            for item in items {
                guard let id = item["id"] as? String, !id.trimmingCharacters(in: .whitespaces).isEmpty else { continue }
                if id.lowercased().contains("embed") { continue }
                result.append(ModelEntry(slug: id,
                                         displayName: LmStudioService.buildDisplayName(id),
                                         providerId: LmStudioService.providerId,
                                         providerName: LmStudioService.providerName))
            }
        } catch {
            Logger.shared.warn("LmStudioService", "localModels", "LM-Studio-Modelle nicht lesbar: \(error.localizedDescription)")
        }
        return result
    }

    /// Startet bei Bedarf den Server und liest danach die Modelle. Der Serverstart passiert nur,
    /// wenn die erste Abfrage ins Leere laeuft - so bleibt der Programmstart schnell.
    func localModelsWithServer() async -> [ModelEntry] {
        let models = await localModels()
        if !models.isEmpty || !LmStudioService.isInstalled { return models }

        await Task.detached { _ = LmStudioService.ensureServerRunning() }.value
        return await localModels()
    }

    /// "mistralai/devstral-small-2-2512" -> "Devstral Small 2 2512 (lokal)".
    private static func buildDisplayName(_ id: String) -> String {
        let name = id.split(separator: "/").last.map(String.init) ?? id
        let pretty = name.split(separator: "-")
            .map { $0.prefix(1).uppercased() + $0.dropFirst() }
            .joined(separator: " ")
        return "\(pretty) (lokal)"
    }
}

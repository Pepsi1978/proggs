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

    /// Kleinster Kontext, mit dem OpenCode ueberhaupt arbeiten kann.
    ///
    /// Frueher 32768, begruendet mit "der Systemprompt braucht rund 22000 Tokens". Das galt, solange
    /// die rund 70 externen Skills mitgeschickt wurden (gemessen 21816 Token). Seit die
    /// Vorbereitung OPENCODE_DISABLE_EXTERNAL_SKILLS setzt, sind es gemessene 7461 Token; mit
    /// abgeschalteten Chrome-MCPs noch weniger. 16384 laesst also reichlich Luft fuer Prompt,
    /// Antwort und Verlauf.
    ///
    /// Der alte Wert war auf 24-GB-Rechnern schaedlich: der KV-Cache eines 27B-Modells kostet bei
    /// 32768 Tokens rund 8 GB, zusammen mit 15 GB Gewichten mehr als der Rechner hat - LM Studio
    /// brach mit "insufficient system resources" ab, obwohl das Modell selbst gepasst haette.
    ///
    /// Reine WARNSCHWELLE und Rueckfallwert beim Laden - kein Zwang: ein Modell, das in LM Studio
    /// bereits mit weniger geladen ist, wird nicht angetastet, sondern nur mit einem Hinweis
    /// uebernommen.
    static let minimumAgentContext = 16_384

    /// Wunschgroesse beim automatischen Laden. Wird auf den Maximalkontext des Modells gedeckelt.
    /// 32768 statt frueher 65536: der KV-Cache waechst linear mit, 65536 kostete bei einem
    /// 27B-Modell allein rund 16 GB und sprengte jeden Rechner mit 24 GB.
    static let preferredContext = 32_768

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

    /// Entlaedt ein Modell, damit es mit anderer Kontextlaenge neu geladen werden kann.
    @discardableResult
    static func unload(modelId: String) -> Bool {
        guard isInstalled, !modelId.trimmingCharacters(in: .whitespaces).isEmpty else { return false }
        let result = Shell.run(lmsPath, ["unload", modelId], timeout: 60)
        Logger.shared.info("LmStudioService", "unload", "lms unload \(modelId) beendet mit \(result.exitCode)")
        return result.exitCode == 0
    }

    /// Kontextlaenge, mit der das Modell in LM Studio gerade geladen ist - also exakt die
    /// Einstellung, die dort gilt. 0 bedeutet: nicht geladen. OpenCode rechnet seine Auslastung
    /// gegen diesen Wert; steht in der Konfig eine kleinere Zahl, meldet die Oberflaeche viel zu
    /// frueh einen vollen Kontext und komprimiert endlos. Steht dort eine groessere, lehnt LM Studio
    /// die erste Anfrage ab.
    ///
    /// Gemessen (2026-08-27, LM Studio mit `parallel: 4` und 16384 Tokens): ein Prompt mit 9123
    /// Tokens laeuft durch. Das geladene Fenster wird also NICHT auf die parallelen Slots
    /// aufgeteilt - der gemeldete Wert gilt jeder einzelnen Anfrage in voller Hoehe.
    static func loadedContextLength(modelId: String) -> Int {
        guard !modelId.trimmingCharacters(in: .whitespaces).isEmpty else { return 0 }
        return loadedContexts()[modelId.lowercased()] ?? 0
    }

    /// Ausgabe-Obergrenze. OpenCode reserviert diesen Wert im Kontextbudget und zeigt ihn als
    /// bereits verbraucht an - eine grosszuegige Obergrenze frisst also sichtbar Kontext, ohne dass
    /// ein einziges Token geschrieben waere. 8192 reicht fuer jede realistische Antwort inklusive
    /// Werkzeugaufrufen; bei kleinen Fenstern wird anteilig gedeckelt.
    static func outputLimit(for contextLength: Int) -> Int {
        Swift.min(Swift.max(Swift.min(8_192, contextLength / 8), 2_048), 8_192)
    }

    /// Ein Eintrag aus dem LM-Studio-Katalog (`lms ls --json`), soweit er fuer die Anzeige zaehlt.
    private struct CatalogEntry {
        var displayName: String
        var publisher: String
    }

    /// Liest den Katalog der heruntergeladenen Modelle. Daraus stammen der echte Anzeigename
    /// ("Qwen3.8 27B UD") und der Herausgeber - beides braucht die Oberflaeche, um zwei Varianten
    /// desselben Modells auseinanderzuhalten. Ohne lms-CLI bleibt die Zuordnung leer, dann greift
    /// der aus der ID abgeleitete Name.
    private static func catalog() -> [String: CatalogEntry] {
        guard isInstalled else { return [:] }
        let result = Shell.run(lmsPath, ["ls", "--json"], timeout: 30)
        guard let data = result.stdout.data(using: .utf8),
              let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else { return [:] }

        var map: [String: CatalogEntry] = [:]
        for item in array {
            let name = (item["displayName"] as? String) ?? ""
            let publisher = (item["publisher"] as? String) ?? ""
            for key in ["modelKey", "identifier", "indexedModelIdentifier", "path"] {
                if let value = item[key] as? String, !value.isEmpty {
                    map[value.lowercased()] = CatalogEntry(displayName: name, publisher: publisher)
                }
            }
        }
        return map
    }

    /// Kontextlaenge je GERADE geladenem Modell. Ein Aufruf fuer alle: `loadedContextLength` wuerde
    /// die CLI sonst einmal pro Modell starten und das Oeffnen der Liste zaeh machen.
    static func loadedContexts() -> [String: Int] {
        guard isInstalled else { return [:] }
        let result = Shell.run(lmsPath, ["ps", "--json"], timeout: 30)
        guard let data = result.stdout.data(using: .utf8),
              let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else { return [:] }

        var map: [String: Int] = [:]
        for item in array {
            let context = (item["contextLength"] as? Int) ?? 0
            guard context > 0 else { continue }
            for key in ["identifier", "modelKey", "indexedModelIdentifier", "path"] {
                if let value = item[key] as? String, !value.isEmpty { map[value.lowercased()] = context }
            }
        }
        return map
    }

    /// Holt die Modell-IDs vom laufenden Server. Embedding-Modelle werden aussortiert, sie taugen
    /// nicht als Chat-Modell fuer OpenCode. Leere Liste = Server aus oder keine Modelle.
    ///
    /// Jeder Eintrag bekommt den Herausgeber in den Namen. Ohne ihn heissen zwei Varianten
    /// desselben Modells gleich - "unsloth/qwen3.8-27b" und "qwen/qwen3.8-27b" wurden beide zu
    /// "Qwen3.8 27b (lokal)". Wer dann den falschen der beiden startet, sieht nur noch
    /// "insufficient system resources", obwohl in LM Studio sichtbar ein Modell geladen ist.
    /// Bereits geladene Modelle stehen zusaetzlich oben in der Liste.
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

            let catalog = LmStudioService.catalog()
            let loaded = LmStudioService.loadedContexts()

            for item in items {
                guard let id = item["id"] as? String, !id.trimmingCharacters(in: .whitespaces).isEmpty else { continue }
                if id.lowercased().contains("embed") { continue }
                let entry = ModelEntry(slug: id,
                                       displayName: LmStudioService.buildDisplayName(id, catalog: catalog),
                                       providerId: LmStudioService.providerId,
                                       providerName: LmStudioService.providerName)
                entry.lmStudioLoadedContext = loaded[id.lowercased()] ?? 0
                result.append(entry)
            }
        } catch {
            Logger.shared.warn("LmStudioService", "localModels", "LM-Studio-Modelle nicht lesbar: \(error.localizedDescription)")
        }

        // Geladene zuerst - sie sind sofort einsatzbereit, alles andere muesste erst laden.
        // Innerhalb der Gruppen bleibt die Reihenfolge des Servers erhalten (stabile Sortierung).
        return result.enumerated()
            .sorted { a, b in
                let aLoaded = a.element.lmStudioLoadedContext > 0
                let bLoaded = b.element.lmStudioLoadedContext > 0
                if aLoaded != bLoaded { return aLoaded }
                return a.offset < b.offset
            }
            .map { $0.element }
    }

    /// Startet bei Bedarf den Server und liest danach die Modelle. Der Serverstart passiert nur,
    /// wenn die erste Abfrage ins Leere laeuft - so bleibt der Programmstart schnell.
    func localModelsWithServer() async -> [ModelEntry] {
        let models = await localModels()
        if !models.isEmpty || !LmStudioService.isInstalled { return models }

        await Task.detached { _ = LmStudioService.ensureServerRunning() }.value

        // "lms server start" kehrt zurueck, bevor der HTTP-Endpunkt Anfragen annimmt. Ohne
        // Wiederholversuche liefert die naechste Abfrage deshalb eine leere Liste - der Reiter
        // "LM Studio" bleibt dann leer, obwohl LM Studio laeuft.
        for attempt in 0..<4 {
            let models = await localModels()
            if !models.isEmpty { return models }
            if attempt < 3 { try? await Task.sleep(nanoseconds: 1_500_000_000) }
        }
        Logger.shared.warn("LmStudioService", "localModelsWithServer",
                           "Server gestartet, liefert aber keine Modelle")
        return []
    }

    /// Baut den Anzeigenamen. Mit Katalogeintrag: "Qwen3.8 27B UD - unsloth (lokal)". Ohne ihn
    /// (kein lms, Modell nicht im Katalog) wie bisher aus der ID abgeleitet:
    /// "mistralai/devstral-small-2-2512" -> "Devstral Small 2 2512 (lokal)".
    private static func buildDisplayName(_ id: String, catalog: [String: CatalogEntry] = [:]) -> String {
        let known = catalog[id.lowercased()]
        let base: String
        if let known, !known.displayName.isEmpty {
            base = known.displayName
        } else {
            let name = id.split(separator: "/").last.map(String.init) ?? id
            base = name.split(separator: "-")
                .map { $0.prefix(1).uppercased() + $0.dropFirst() }
                .joined(separator: " ")
        }

        // Der Herausgeber macht zwei Varianten desselben Modells unterscheidbar. Faellt er mit dem
        // Namen zusammen ("openai" + "GPT-OSS 20B" waere noch eindeutig), bleibt er trotzdem
        // stehen - eine einheitliche Form ist leichter zu lesen als eine bedingte.
        let publisher = known?.publisher ?? String(id.split(separator: "/").dropLast().joined(separator: "/"))
        if publisher.isEmpty { return "\(base) (lokal)" }
        return "\(base) - \(publisher) (lokal)"
    }
}

import Foundation

/// Der allgemeine Kanal zwischen Jarvis Mac OS und Jarvis am Handy: zwei gleichberechtigte Agenten, die sich jederzeit schreiben.
/// Läuft über den Relay (Geheimnis der MCP-Adresse), nicht über die Handy-Werkzeuge: So kommt Post auch an, wenn die Gegenseite gerade
/// weg ist, und eine lange Begutachtung hängt an keiner Zeitgrenze. Eingang erst auf die Platte, dann quittieren, dann abarbeiten:
/// Ein Absturz verliert nichts. Fragen vom Handy beantwortet ein eigenes Gespräch, Franks Gespräche bleiben unberührt.
@MainActor
final class AgentenKanal {
    static let shared = AgentenKanal()
    /// Solange ein Lauf eine Nachricht vom Handy beantwortet: Rückfragen stehen in der Antwort, sonst warten beide aufeinander.
    @TaskLocal nonisolated static var imLauf = false

    private static let titel = "Jarvis am Handy (Agentenkanal)"
    private static let warteSekunden = 60.0
    private let eingangDatei = Pfade.unterstuetzung.appendingPathComponent("agentenkanal-eingang.json")
    private let verlaufDatei = Pfade.unterstuetzung.appendingPathComponent("agentenkanal-verlauf.json")

    private var eingang: [JSONObjekt] = []
    private var erledigt: [String] = []
    private var verlauf: [JSONObjekt] = []
    private var wartende: [String: CheckedContinuation<String?, Never>] = [:]
    private var arbeitet = false
    private(set) var handyDa = false

    private init() {
        if let d = try? Data(contentsOf: eingangDatei), let j = JSON.objekt(d) {
            eingang = j["eingang"] as? [JSONObjekt] ?? []
            erledigt = j["erledigt"] as? [String] ?? []
        }
        if let d = try? Data(contentsOf: verlaufDatei), let j = JSON.objekt(d) { verlauf = j["verlauf"] as? [JSONObjekt] ?? [] }
    }

    func starten() {
        Task { await abfragen() }
        abarbeiten()
    }

    private var basis: String? {
        let a = Einstellungen.shared.zugang.relay.trimmingCharacters(in: .whitespacesAndNewlines)
        guard a.hasSuffix("/mcp") else { return nil }
        return String(a.dropLast(4)) + "/kanal"
    }

    private func anfrage(_ adresse: String, rumpf: JSONObjekt? = nil, sekunden: Double) async throws -> (Int, Data) {
        guard let url = URL(string: adresse) else { throw URLError(.badURL) }
        var req = URLRequest(url: url, timeoutInterval: sekunden)
        if let rumpf {
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.httpBody = JSON.daten(rumpf)
        }
        req.setValue("JarvisMac/\(AppInfo.version)", forHTTPHeaderField: "User-Agent")
        let (daten, antwort) = try await URLSession.shared.data(for: req)
        return ((antwort as? HTTPURLResponse)?.statusCode ?? 0, daten)
    }

    private func abfragen() async {
        var pause = 2.0
        while true {
            guard let basis else { try? await Task.sleep(for: .seconds(30)); continue }
            do {
                let (code, daten) = try await anfrage(basis, sekunden: 40)
                guard code == 200, let j = JSON.objekt(daten) else { throw URLError(.badServerResponse) }
                pause = 2
                handyDa = j["handy"] as? Bool ?? false
                let neu = j["nachrichten"] as? [JSONObjekt] ?? []
                if !neu.isEmpty { await annehmen(neu, basis: basis) }
            } catch {
                try? await Task.sleep(for: .seconds(pause))
                pause = min(pause * 2, 15)
            }
        }
    }

    private func annehmen(_ neu: [JSONObjekt], basis: String) async {
        var ids: [String] = []
        for n in neu {
            guard let id = n["id"] as? String else { continue }
            ids.append(id)
            if erledigt.contains(id) || eingang.contains(where: { $0["id"] as? String == id }) { continue }
            let art = n["art"] as? String ?? "info"
            let text = n["text"] as? String ?? ""
            merke(von: "Handy", art: art, text: text)
            if art == "antwort" { antwortKam(auf: n["antwort_auf"] as? String ?? "", text: text); erledigt.append(id) }
            else { eingang.append(n) }
        }
        erledigt = Array(erledigt.suffix(300))
        sichere()
        _ = try? await anfrage(basis + "/quittung", rumpf: ["ids": ids], sekunden: 20)
        abarbeiten()
    }

    private func antwortKam(auf frage: String, text: String) {
        if let w = wartende.removeValue(forKey: frage) { w.resume(returning: text); return }
        // Niemand wartet mehr: Sie steht im Kanalgespräch und im Verlauf (handy_jarvis_lesen).
        zeigeImGespraech("Späte Antwort von Jarvis am Handy: \(text)")
        Task { _ = await Shell.osascript("display notification \"\(String(text.prefix(180)).replacingOccurrences(of: "\"", with: "'"))\" with title \"Jarvis am Handy\"") }
    }

    private func abarbeiten() {
        guard !arbeitet, let n = eingang.first else { return }
        arbeitet = true
        Task {
            defer { arbeitet = false; abarbeiten() }
            await bearbeite(n)
        }
    }

    private func bearbeite(_ n: JSONObjekt) async {
        let id = n["id"] as? String ?? ""
        let art = n["art"] as? String ?? "info"
        let text = n["text"] as? String ?? ""
        let g = kanalGespraech()
        if art == "info" {
            zeigeImGespraech("Mitteilung von Jarvis am Handy: \(text)")
        } else {
            log("gespraech", "Agentenkanal: \(art) vom Handy", wo: "Agentenkanal")
            let auftrag = Self.auftrag(art: art, text: text, verlauf: verlaufText(ohneLetzte: true))
            let antwort = await AgentenKanal.$imLauf.withValue(true) { await JarvisAgent.shared.frage(auftrag, in: g) }
            await sende(art: "antwort", text: antwort.isEmpty ? "(keine Antwort)" : antwort, antwortAuf: id)
        }
        eingang.removeAll { $0["id"] as? String == id }
        erledigt.append(id)
        sichere()
    }

    private static func auftrag(art: String, text: String, verlauf: String) -> String {
        let vorgabe: String
        switch art {
        case "stand":
            vorgabe = "Ermittle den aktuellen Stand der Programmieraufgaben auf diesem Mac, nicht aus der Erinnerung: laufende Sitzungen (`pgrep -fl claude`, Terminal-Fenster und ihr Inhalt per mac_applescript, ~/Library/Logs/JarvisPcBruecke), `git -C ~/proggs status -sb` und `git log`, laufende Bauvorgänge. Nenne je Aufgabe, was fertig ist, was gerade läuft und was hängt."
        case "begutachten":
            vorgabe = "Begutachte das gesamte Geschehen auf diesem Mac zu dieser Aufgabe: Bildschirm (mac_bildschirm), Programme, Terminal-Inhalte, Sitzungen, Logs, Git-Stand, Bau und Tests. Sag deine Einschätzung: Was läuft gut, was ist falsch oder riskant, was sollte als Nächstes geschehen."
        default:
            vorgabe = "Beantworte die Frage, prüfe dafür am Mac selbst nach, statt zu raten."
        }
        return """
        [AGENTENKANAL – Nachricht von deinem Schwester-Agenten Jarvis am Handy, nicht von Frank. Art: \(art)]
        \(text)

        \(vorgabe)
        Deine Antwort geht zurück an Jarvis am Handy: kurz, sachlich, für sich verständlich. Rückfragen und Vorschläge, wer was wann macht, schreibst du einfach in die Antwort.\(verlauf.isEmpty ? "" : "\n\nZuletzt auf dem Kanal:\n\(verlauf)")
        """
    }

    private func kanalGespraech() -> Gespraech {
        if let g = GespraechsSpeicher.shared.alle.first(where: { $0.titel == Self.titel }) { return g }
        let g = GespraechsSpeicher.shared.neu()
        g.titel = Self.titel
        GespraechsSpeicher.shared.aufnehmen(g)
        return g
    }

    private func zeigeImGespraech(_ text: String) {
        let g = kanalGespraech()
        g.nachrichten.append(Nachricht(rolle: .jarvis, text: text))
        g.speichern()
    }

    /// Schickt an das Handy und wiederholt bei Netzfehlern; dieselbe id legt der Relay nicht doppelt ab. false = nicht angekommen.
    @discardableResult
    func sende(art: String, text: String, antwortAuf: String? = nil, id: String = UUID().uuidString) async -> Bool {
        guard let basis else { return false }
        var rumpf: JSONObjekt = ["id": id, "art": art, "text": text]
        if let antwortAuf { rumpf["antwort_auf"] = antwortAuf }
        merke(von: "Mac", art: art, text: text)
        for versuch in 0..<6 {
            if let (code, _) = try? await anfrage(basis, rumpf: rumpf, sekunden: 20), code == 200 { return true }
            try? await Task.sleep(for: .seconds(2 << versuch))
        }
        log("fehler", "Agentenkanal: Nachricht ans Handy nicht angekommen", wo: "Agentenkanal")
        return false
    }

    /// Frage ans Handy: wartet höchstens eine Minute. Danach kommt die Antwort trotzdem an, sie steht dann im Kanalgespräch.
    func frageHandy(_ text: String) async -> WerkzeugErgebnis {
        let id = UUID().uuidString
        guard await sende(art: "frage", text: text, id: id) else {
            return WerkzeugErgebnis(text: "Die Nachricht ist nicht beim Relay angekommen (keine Verbindung oder keine Relay-Adresse).", fehler: true)
        }
        let antwort: String? = await withCheckedContinuation { w in
            wartende[id] = w
            Task {
                try? await Task.sleep(for: .seconds(Self.warteSekunden))
                wartende.removeValue(forKey: id)?.resume(returning: nil)
            }
        }
        if let antwort { return WerkzeugErgebnis(text: antwort) }
        return WerkzeugErgebnis(text: "Noch keine Antwort nach einer Minute\(handyDa ? "" : ", das Handy ist gerade nicht verbunden"). Die Frage bleibt zugestellt; die Antwort steht später in handy_jarvis_lesen.")
    }

    private func merke(von: String, art: String, text: String) {
        verlauf.append(["von": von, "art": art, "text": String(text.prefix(4000)), "zeit": Date().timeIntervalSince1970])
        verlauf = Array(verlauf.suffix(100))
        try? JSON.daten(["verlauf": verlauf]).write(to: verlaufDatei, options: .atomic)
    }

    private func sichere() {
        try? JSON.daten(["eingang": eingang, "erledigt": erledigt]).write(to: eingangDatei, options: .atomic)
    }

    func verlaufText(anzahl: Int = 8, ohneLetzte: Bool = false) -> String {
        let f = DateFormatter(); f.dateFormat = "dd.MM. HH:mm"
        let liste = ohneLetzte ? Array(verlauf.dropLast()) : verlauf
        return liste.suffix(anzahl).map { n in
            let zeit = f.string(from: Date(timeIntervalSince1970: n["zeit"] as? Double ?? 0))
            return "[\(zeit)] \(n["von"] as? String ?? "") (\(n["art"] as? String ?? "")): \(n["text"] as? String ?? "")"
        }.joined(separator: "\n")
    }

    nonisolated static let werkzeuge: [LokalesWerkzeug] = [
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "handy_jarvis_sprechen",
            beschreibung: "Jarvis am Mac: spricht über den Agentenkanal mit deinem Schwester-Agenten Jarvis am Handy, jederzeit und von dir aus. art=frage: Er arbeitet die Frage auf dem Handy ab (Apps, Sensoren, was dort läuft); du wartest bis zu einer Minute auf die Antwort. art=info: Mitteilung ohne Antwort, zum Beispiel wer was wann übernimmt. Während du selbst gerade eine Kanalnachricht beantwortest, geht nur art=info: Rückfragen schreibst du dann in deine Antwort.",
            schema: ["type": "object", "properties": [
                "art": ["type": "string", "enum": ["frage", "info"], "description": "frage oder info."] as JSONObjekt,
                "text": ["type": "string", "description": "Die Nachricht, vollständig und für sich verständlich."] as JSONObjekt,
            ] as JSONObjekt, "required": ["art", "text"]])) { a in
            let text = (a["text"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty else { return WerkzeugErgebnis(text: "Der Text ist leer.", fehler: true) }
            if (a["art"] as? String) == "info" {
                let ok = await AgentenKanal.shared.sende(art: "info", text: text)
                return WerkzeugErgebnis(text: ok ? "Mitteilung an Jarvis am Handy abgelegt." : "Die Mitteilung ist nicht beim Relay angekommen.", fehler: !ok)
            }
            if AgentenKanal.imLauf { return WerkzeugErgebnis(text: "Du beantwortest gerade eine Kanalnachricht: Schreib die Rückfrage in deine Antwort.", fehler: true) }
            return await AgentenKanal.shared.frageHandy(text)
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "handy_jarvis_lesen",
            beschreibung: "Jarvis am Mac: die letzten Nachrichten des Agentenkanals mit Jarvis am Handy, in beide Richtungen, auch späte Antworten.",
            schema: ["type": "object", "properties": [:] as JSONObjekt])) { _ in
            let t = await AgentenKanal.shared.verlaufText(anzahl: 20)
            return WerkzeugErgebnis(text: t.isEmpty ? "Auf dem Kanal steht noch nichts." : t)
        },
    ]
}

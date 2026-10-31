import Foundation

/// Der allgemeine Kanal zwischen Jarvis Mac OS und Jarvis am Handy: zwei gleichberechtigte Agenten, die sich jederzeit schreiben.
/// Läuft über den Relay (Geheimnis der MCP-Adresse), nicht über die Handy-Werkzeuge: So kommt Post auch an, wenn die Gegenseite gerade
/// weg ist, und eine lange Begutachtung hängt an keiner Zeitgrenze. Eingang erst auf die Platte, dann quittieren, dann abarbeiten;
/// die fertige Antwort bleibt im Eingang, bis sie beim Relay liegt: Ein Absturz oder Funkloch verliert nichts und rechnet nichts doppelt.
/// Fragen vom Handy beantwortet ein eigenes Gespräch: je Nachricht ein frisches Modellgespräch mit Auszug des Kanals, damit Live-Gedächtnis
/// und Kosten nicht mit dem Kanal altern und wachsen. Kein Lernen daraus und keine Werkzeuge, die für Frank sprechen: Dort redet nicht Frank.
@MainActor
final class AgentenKanal {
    static let shared = AgentenKanal()
    /// Solange ein Lauf eine Nachricht vom Handy beantwortet: Rückfragen stehen in der Antwort, sonst warten beide aufeinander.
    @TaskLocal nonisolated static var imLauf = false

    nonisolated static let titel = "Jarvis am Handy (Agentenkanal)"
    private static let warteSekunden = 60.0
    private static let zeitlimit = 600.0
    private static let hoechstalter = 6.0 * 3600
    private static let gesperrt: Set<String> = ["jarvis_merken", "jarvis_vergessen", "jarvis_langzeitkontext_ersetzen", "mail_senden",
                                                "programmieren", "programmier_nachtrag", "programmier_antwort", "programmierer_sprechen", "agent_starten", "mac_dialog"]
    private let eingangDatei = Pfade.unterstuetzung.appendingPathComponent("agentenkanal-eingang.json")
    private let verlaufDatei = Pfade.unterstuetzung.appendingPathComponent("agentenkanal-verlauf.json")

    private var eingang: [JSONObjekt] = []
    private var erledigt: [String] = []
    private var verlauf: [JSONObjekt] = []
    private var wartende: [String: CheckedContinuation<String?, Never>] = [:]
    private var arbeitet = false
    /// Hält App Nap fern: Verdeckt gestartet soll die App weiter abfragen und arbeiten.
    private var aktivitaet: NSObjectProtocol?

    private init() {
        if let d = try? Data(contentsOf: eingangDatei), let j = JSON.objekt(d) {
            eingang = j["eingang"] as? [JSONObjekt] ?? []
            erledigt = j["erledigt"] as? [String] ?? []
        }
        if let d = try? Data(contentsOf: verlaufDatei), let j = JSON.objekt(d) { verlauf = j["verlauf"] as? [JSONObjekt] ?? [] }
    }

    func starten() {
        aktivitaet = ProcessInfo.processInfo.beginActivity(options: .userInitiatedAllowingIdleSystemSleep, reason: "Agentenkanal mit Jarvis am Handy")
        Task {
            // Erst wenn die Gespräche geladen sind: Sonst legte die erste Nachricht ein zweites Kanalgespräch an.
            while !GespraechsSpeicher.shared.geladen { try? await Task.sleep(for: .milliseconds(100)) }
            abarbeiten()
            await abfragen()
        }
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
                let neu = j["nachrichten"] as? [JSONObjekt] ?? []
                // Ohne Quittung liefert der Relay sofort dasselbe noch einmal: darum wie jeder Fehler mit Pause.
                if !neu.isEmpty, !(await annehmen(neu, basis: basis)) { throw URLError(.badServerResponse) }
                pause = 2
            } catch {
                try? await Task.sleep(for: .seconds(pause))
                pause = min(pause * 2, 15)
            }
        }
    }

    private func annehmen(_ neu: [JSONObjekt], basis: String) async -> Bool {
        var ids: [String] = []
        for n in neu {
            guard let id = n["id"] as? String else { continue }
            ids.append(id)
            if erledigt.contains(id) || eingang.contains(where: { $0["id"] as? String == id }) { continue }
            let art = n["art"] as? String ?? "info"
            let text = n["text"] as? String ?? ""
            merke(id: id, von: "Handy", art: art, text: text)
            if art == "antwort" { antwortKam(auf: n["antwort_auf"] as? String ?? "", text: text); erledigt.append(id) }
            else { eingang.append(n) }
        }
        erledigt = Array(erledigt.suffix(300))
        sichere()
        abarbeiten()
        guard let (code, _) = try? await anfrage(basis + "/quittung", rumpf: ["ids": ids], sekunden: 20) else { return false }
        return code == 200
    }

    private func antwortKam(auf frage: String, text: String) {
        if let w = wartende.removeValue(forKey: frage) { w.resume(returning: text); return }
        // Niemand wartet mehr: Sie steht im Kanalgespräch und im Verlauf (handy_jarvis_lesen).
        zeigeImGespraech("Späte Antwort von Jarvis am Handy: \(text)")
        Task { _ = await Shell.mitteilung("Jarvis am Handy", String(text.prefix(180))) }
    }

    private func abarbeiten() {
        guard !arbeitet, let n = eingang.first else { return }
        arbeitet = true
        Task {
            // Antwort nicht zugestellt (Mac ohne Netz): Sie bleibt im Eingang, später erneut senden.
            if !(await bearbeite(n)) { try? await Task.sleep(for: .seconds(30)) }
            arbeitet = false
            abarbeiten()
        }
    }

    private func bearbeite(_ n: JSONObjekt) async -> Bool {
        let id = n["id"] as? String ?? ""
        let art = n["art"] as? String ?? "info"
        let text = n["text"] as? String ?? ""
        if art == "info" {
            zeigeImGespraech("Mitteilung von Jarvis am Handy: \(text)")
        } else {
            var antwort = n["fertige_antwort"] as? String
            if antwort == nil {
                let alter = Date().timeIntervalSince1970 - ((n["zeit"] as? NSNumber)?.doubleValue ?? 0)
                antwort = alter > Self.hoechstalter
                    ? "Deine Nachricht lag länger als sechs Stunden, ich habe sie nicht mehr bearbeitet. Frag neu, falls sie noch gilt."
                    : await beantworte(art: art, text: text, id: id)
                if let i = eingang.firstIndex(where: { $0["id"] as? String == id }) {
                    eingang[i]["fertige_antwort"] = antwort
                    sichere()
                }
            }
            // Dieselbe Kennung bei jedem Versuch: Der Relay legt die Antwort nicht doppelt ab.
            guard await sende(art: "antwort", text: antwort ?? "(keine Antwort)", antwortAuf: id, id: "antwort-\(id)") else { return false }
        }
        eingang.removeAll { $0["id"] as? String == id }
        erledigt.append(id)
        sichere()
        return true
    }

    private func beantworte(art: String, text: String, id: String) async -> String {
        log("gespraech", "Agentenkanal: \(art) vom Handy", wo: "Agentenkanal")
        let g = kanalGespraech()
        let auftrag = Self.auftrag(art: art, text: text, verlauf: verlaufText(ohne: id))
        var antwort = ""
        for versuch in 0..<3 {
            if versuch > 0 { try? await Task.sleep(for: .seconds(20)) }
            g.verlauf = []
            g.kontext = ""
            g.nachrichten = Array(g.nachrichten.suffix(200))
            let lauf = Task { @MainActor in
                await AgentenKanal.$imLauf.withValue(true) { await JarvisAgent.shared.frage(auftrag, in: g, ohneWerkzeuge: Self.gesperrt) }
            }
            guard await mitFrist(Self.zeitlimit, { _ = await lauf.value }) else {
                lauf.cancel()
                return "Jarvis Mac OS ist nach zehn Minuten nicht fertig geworden und hat abgebrochen. Frag enger gefasst noch einmal."
            }
            antwort = await lauf.value
            // Modellfehler: frage meldet sie als Text, und ein Aussetzer soll keine Antwort sein.
            if !(antwort.components(separatedBy: "\n\n").last ?? "").hasPrefix("⚠️") {
                return antwort.isEmpty ? "(keine Antwort)" : antwort.replacingOccurrences(of: "sag „weiter“, dann mache ich weiter.", with: "frag nach, wenn ich weitermachen soll.")
            }
        }
        return "Jarvis Mac OS konnte nicht antworten: \(antwort)"
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

    /// Legt beim Relay ab und wiederholt bei Netzfehlern; nil = nicht angekommen.
    private func post(art: String, text: String, antwortAuf: String?, id: String) async -> JSONObjekt? {
        guard let basis else { return nil }
        var rumpf: JSONObjekt = ["id": id, "art": art, "text": text]
        if let antwortAuf { rumpf["antwort_auf"] = antwortAuf }
        merke(id: id, von: "Mac", art: art, text: text)
        for versuch in 0..<4 {
            if versuch > 0 { try? await Task.sleep(for: .seconds(Double(1 << versuch))) }
            if let (code, daten) = try? await anfrage(basis, rumpf: rumpf, sekunden: 15), code == 200 { return JSON.objekt(daten) ?? [:] }
        }
        log("fehler", "Agentenkanal: Nachricht ans Handy nicht angekommen", wo: "Agentenkanal")
        return nil
    }

    func sende(art: String, text: String, antwortAuf: String? = nil, id: String = UUID().uuidString) async -> Bool {
        await post(art: art, text: text, antwortAuf: antwortAuf, id: id) != nil
    }

    private func loese(_ id: String, _ antwort: String?) { wartende.removeValue(forKey: id)?.resume(returning: antwort) }

    /// Frage ans Handy: wartet höchstens eine Minute. Danach kommt die Antwort trotzdem an, sie steht dann im Kanalgespräch.
    func frageHandy(_ text: String) async -> WerkzeugErgebnis {
        let id = UUID().uuidString
        var abgelegt: Bool?
        var handyDa = true
        // Erst anmelden, dann senden: Eine schnelle Antwort kann kommen, bevor der Relay zurück ist.
        let antwort: String? = await withTaskCancellationHandler {
            await withCheckedContinuation { w in
                wartende[id] = w
                Task {
                    try? await Task.sleep(for: .seconds(Self.warteSekunden))
                    loese(id, nil)
                }
                Task {
                    let stand = await post(art: "frage", text: text, antwortAuf: nil, id: id)
                    abgelegt = stand != nil
                    handyDa = stand?["zugestellt"] as? Bool ?? false
                    if !handyDa { loese(id, nil) }
                }
            }
        } onCancel: {
            Task { @MainActor in AgentenKanal.shared.loese(id, nil) }
        }
        if let antwort { return WerkzeugErgebnis(text: antwort) }
        if abgelegt == false {
            return WerkzeugErgebnis(text: "Die Nachricht ist nicht beim Relay angekommen (keine Verbindung oder keine Relay-Adresse).", fehler: true)
        }
        if !handyDa { return WerkzeugErgebnis(text: "Das Handy ist gerade nicht verbunden. Die Frage bleibt eingereiht; die Antwort steht später in handy_jarvis_lesen.") }
        return WerkzeugErgebnis(text: "Noch keine Antwort nach einer Minute. Die Frage ist zugestellt; die Antwort steht später in handy_jarvis_lesen.")
    }

    private func merke(id: String, von: String, art: String, text: String) {
        guard !verlauf.contains(where: { $0["id"] as? String == id }) else { return }
        verlauf.append(["id": id, "von": von, "art": art, "text": String(text.prefix(4000)), "zeit": Date().timeIntervalSince1970])
        verlauf = Array(verlauf.suffix(100))
        try? JSON.daten(["verlauf": verlauf]).write(to: verlaufDatei, options: .atomic)
    }

    private func sichere() {
        try? JSON.daten(["eingang": eingang, "erledigt": erledigt]).write(to: eingangDatei, options: .atomic)
    }

    func verlaufText(anzahl: Int = 8, ohne: String = "", breite: Int = 1500) -> String {
        let f = DateFormatter(); f.dateFormat = "dd.MM. HH:mm"
        return verlauf.filter { $0["id"] as? String != ohne }.suffix(anzahl).map { n in
            let zeit = f.string(from: Date(timeIntervalSince1970: n["zeit"] as? Double ?? 0))
            return "[\(zeit)] \(n["von"] as? String ?? "") (\(n["art"] as? String ?? "")): \((n["text"] as? String ?? "").prefix(breite))"
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
            let t = await AgentenKanal.shared.verlaufText(anzahl: 20, breite: 4000)
            return WerkzeugErgebnis(text: t.isEmpty ? "Auf dem Kanal steht noch nichts." : t)
        },
    ]
}

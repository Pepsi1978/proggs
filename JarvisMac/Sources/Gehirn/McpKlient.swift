import Foundation

/// Werkzeug, wie es das Modell sieht (Name, Beschreibung, JSON-Schema der Eingabe).
struct WerkzeugDefinition {
    let name: String
    let beschreibung: String
    let schema: JSONObjekt
}

/// Ergebnis eines Werkzeugaufrufs. `bildPNG` landet beim Modell als Bild (z. B. Bildschirmfoto).
struct WerkzeugErgebnis {
    var text: String
    var fehler = false
    var bildPNG: Data? = nil
}

/// Anbindung an das Handy-Jarvis über dessen MCP-Relay (dieselbe Adresse wie das ChatGPT-Plugin und Claude Code).
/// Darüber laufen alle Werkzeuge der Handy-App: Live-Gedächtnis, Merken, Aufgaben, Wecker, Kalender, Ablage,
/// Biodaten, Agenten, Programmieraufträge … – so ist der Mac immer auf demselben Stand wie das Handy.
actor McpKlient {
    static let shared = McpKlient()
    /// Technische Werkzeuge für die Ablage-Oberfläche; das Modell braucht sie nicht.
    /// Der Hinweis, den das Handy an viele Ergebnisse hängt; in der Oberfläche hat er nichts zu suchen.
    static func ohneKontextHinweis(_ t: String) -> String {
        t.replacingOccurrences(of: "\n(Falls in diesem Gespräch noch nicht geschehen: zuerst jarvis_kontext aufrufen.)", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static let nurFuerOberflaeche: Set<String> = ["ablage_struktur", "ablage_datei", "jarvis_mac_starten", "jarvis_mac_sprechen", "jarvis_mac_lesen"]
    static let privateWerkzeuge: Set<String> = ["jarvis_kontext", "jarvis_gemerktes_lesen", "jarvis_merken", "jarvis_langzeitkontext_lesen", "jarvis_langzeitkontext_ersetzen",
                                                "gedaechtnis_suchen", "mail_lesen", "mail_senden", "tagebuch_lesen", "tagesauswertung_lesen", "tagesdaten_lesen",
                                                "biomarker_auswertung", "biomarker_tag", "biomarker_verlauf", "ablage_lesen", "ablage_struktur", "ablage_schreiben",
                                                "prompts_lesen", "ideen_lesen", "protokoll_lesen", "logbuch_lesen", "kalender_lesen", "aufgaben_lesen"]

    private var werkzeugCache: [WerkzeugDefinition] = []
    private(set) var anweisungen = ""
    private(set) var serverVersion = ""
    private var naechsteId = 1
    private var verbundeneAdresse = ""
    private var letzterFehlschlag = Date.distantPast
    private var fehlschlaegeInFolge = 0
    private var werkzeugAbruf: Task<[WerkzeugDefinition], Never>?
    private var kontextVorrat = ""
    private var kontextZeit = Date.distantPast
    private var kontextAbruf: Task<Void, Never>?

    private var adresse: String { get async { await MainActor.run { Einstellungen.shared.zugang.relay } } }

    enum Fehler: LocalizedError {
        case keineAdresse, http(Int, String), antwort(String)
        var errorDescription: String? {
            switch self {
            case .keineAdresse: return "Keine Relay-Adresse für das Handy-Jarvis eingetragen (Einstellungen → Konten)."
            case .http(let c, let t): return "Handy-Jarvis antwortet mit HTTP \(c): \(t.prefix(200))"
            case .antwort(let t): return t
            }
        }
    }

    private func rpc(_ methode: String, _ params: JSONObjekt? = nil, sekunden: Double = 60) async throws -> JSONObjekt {
        let a = await adresse
        guard let url = URL(string: a), !a.isEmpty else { throw Fehler.keineAdresse }
        var req = URLRequest(url: url, timeoutInterval: sekunden)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json, text/event-stream", forHTTPHeaderField: "Accept")
        req.setValue("JarvisMac/\(AppInfo.version)", forHTTPHeaderField: "User-Agent")
        var rumpf: JSONObjekt = ["jsonrpc": "2.0", "id": naechsteId, "method": methode]
        naechsteId += 1
        if let params { rumpf["params"] = params }
        req.httpBody = JSON.daten(rumpf)
        let (daten, antwort) = try await URLSession.shared.data(for: req)
        let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
        let text = String(decoding: daten, as: UTF8.self)
        guard code == 200 else { throw Fehler.http(code, text) }
        // Antwort kann JSON oder ein einzelnes SSE-Ereignis sein.
        var json = JSON.objekt(daten)
        if json == nil {
            let zeilen = text.split(separator: "\n").filter { $0.hasPrefix("data:") }
            if let z = zeilen.last { json = JSON.objekt(String(z.dropFirst(5)).trimmingCharacters(in: .whitespaces)) }
        }
        guard let json else { throw Fehler.antwort("Unlesbare Antwort vom Handy-Jarvis") }
        if let f = json["error"] as? JSONObjekt { throw Fehler.antwort(f["message"] as? String ?? "MCP-Fehler") }
        return json["result"] as? JSONObjekt ?? [:]
    }

    func verbinden() async throws {
        let r = try await rpc("initialize", [
            "protocolVersion": "2025-06-18", "capabilities": [:] as JSONObjekt,
            "clientInfo": ["name": "JarvisMac", "version": AppInfo.version],
        ], sekunden: 20)
        anweisungen = r["instructions"] as? String ?? ""
        serverVersion = (r["serverInfo"] as? JSONObjekt)?["version"] as? String ?? ""
    }

    /// Werkzeugliste des Handys, 10 Minuten zwischengespeichert, nach Namen sortiert (stabil für den Prompt-Cache).
    func werkzeuge(frisch: Bool = false) async -> [WerkzeugDefinition] {
        let a = await adresse
        if a != verbundeneAdresse {   // Relay gewechselt
            werkzeugCache = []; anweisungen = ""; verbundeneAdresse = a
            letzterFehlschlag = .distantPast; fehlschlaegeInFolge = 0; kontextVorrat = ""; kontextZeit = .distantPast
        }
        // Die App erneuert die Liste selbst im Hintergrund: Keine Frage wartet darauf.
        if !frisch, !werkzeugCache.isEmpty { return werkzeugCache }
        // Eben nicht erreichbar: nicht bei jeder Frage erneut bis zur Zeitgrenze warten. Die App prüft alle 30 Sekunden selbst.
        if !frisch, Date().timeIntervalSince(letzterFehlschlag) < 45 { return [] }
        let abruf = werkzeugeHolen()
        // Stopp soll nicht auf den Abruf warten müssen.
        return await withTaskCancellationHandler { await abruf.value } onCancel: { abruf.cancel() }
    }

    private func werkzeugeHolen() -> Task<[WerkzeugDefinition], Never> {
        if let werkzeugAbruf { return werkzeugAbruf }
        let t = Task { () -> [WerkzeugDefinition] in
            defer { werkzeugAbruf = nil }
            return await werkzeugeAbrufen()
        }
        werkzeugAbruf = t
        return t
    }

    private func werkzeugeAbrufen() async -> [WerkzeugDefinition] {
        do {
            if anweisungen.isEmpty { try await verbinden() }
            let r = try await rpc("tools/list", sekunden: 30)
            let liste = (r["tools"] as? [JSONObjekt] ?? []).compactMap { t -> WerkzeugDefinition? in
                guard let n = t["name"] as? String else { return nil }
                var schema = t["inputSchema"] as? JSONObjekt ?? ["type": "object", "properties": [:] as JSONObjekt]
                if schema["type"] == nil { schema["type"] = "object" }
                return WerkzeugDefinition(name: n, beschreibung: t["description"] as? String ?? "", schema: schema)
            }.sorted { $0.name < $1.name }
            if !liste.isEmpty { werkzeugCache = liste; letzterFehlschlag = .distantPast; fehlschlaegeInFolge = 0 }
            log("system", "Handy-Jarvis verbunden: \(liste.count) Werkzeuge (\(serverVersion))")
        } catch {
            if Task.isCancelled { return werkzeugCache }
            // Ein einzelner Aussetzer lässt die Liste stehen. Erst beim zweiten Fehlschlag in Folge gilt das Handy als weg:
            // keinen alten Stand als „verbunden“ melden.
            fehlschlaegeInFolge += 1
            if fehlschlaegeInFolge >= 2 { werkzeugCache = [] }
            letzterFehlschlag = Date()
            log("fehler", "Handy-Jarvis nicht erreichbar: \(error.localizedDescription)")
        }
        return werkzeugCache
    }

    var verbunden: Bool { !werkzeugCache.isEmpty }
    /// Der letzte Abruf ging schief: Die App prüft dann bald erneut statt erst in fünf Minuten.
    var unsicher: Bool { fehlschlaegeInFolge > 0 }

    /// Holt das Live-Gedächtnis schon, während Frank spricht (Weckwort, neues Gespräch), damit die erste Antwort nicht darauf wartet.
    func kontextVorladen() {
        guard kontextAbruf == nil, Date().timeIntervalSince(kontextZeit) > 60 else { return }
        kontextAbruf = Task {
            let r = await rufe("jarvis_kontext", [:], sekunden: 30)
            if !r.fehler { kontextVorrat = r.text; kontextZeit = Date() }
            kontextAbruf = nil
        }
    }

    /// Das Live-Gedächtnis für ein neues Gespräch: der höchstens 5 Minuten alte Vorrat, sonst höchstens 8 Sekunden Warten.
    /// Kommt nichts, bleibt es leer: Das Modell erfährt das und die nächste Frage holt es nach. Ein alter Stand gilt nie.
    func kontextFuerGespraech() async -> String {
        func frisch() -> String { Date().timeIntervalSince(kontextZeit) < 300 ? kontextVorrat : "" }
        if !frisch().isEmpty { return frisch() }
        if werkzeugCache.isEmpty, Date().timeIntervalSince(letzterFehlschlag) < 45 { return "" }
        kontextVorladen()
        if let t = kontextAbruf { _ = await mitFrist(8) { await t.value } }
        return frisch()
    }

    /// Was nur liest, ändert das Gedächtnis nicht. Alles andere kann es ändern, etwa jarvis_merken: Dann gilt der Vorrat nicht mehr.
    private static func nurLesend(_ name: String) -> Bool {
        name.hasSuffix("_lesen") || ["jarvis_kontext", "gedaechtnis_suchen", "web_suche", "ablage_datei", "ablage_struktur", "vorgaenge"].contains(name)
    }

    /// Diese Werkzeuge erzeugen am Handy einen Ablage-Eintrag: Er soll trotzdem als vom Mac gelten.
    private static let legtInAblage: Set<String> = ["ablage_schreiben", "ablage_datei_speichern", "bild_erzeugen", "in_ablage_uebernehmen"]

    func rufe(_ name: String, _ argumente: JSONObjekt, sekunden: Double = 150) async -> WerkzeugErgebnis {
        let start = Date()
        // Ablage und Prompts liegen auf dem Server: Der Mac schreibt selbst dorthin, auch wenn das Handy aus ist.
        if let lokal = await AblageAbgleich.shared.werkzeug(name, argumente) {
            if !Self.nurLesend(name) { kontextVorrat = ""; kontextZeit = .distantPast }
            log("werkzeug", "Ablage-Werkzeug \(name): \(lokal.fehler ? "Fehler" : "ok")", wo: "Server", werkzeug: name, fehler: lokal.fehler ? lokal.text : nil, dauerMs: Int(Date().timeIntervalSince(start) * 1000))
            return lokal
        }
        var argumente = argumente
        if Self.legtInAblage.contains(name) { argumente["_quelle"] = AblageAbgleich.quelle }
        let e = await rufeOhneLog(name, argumente, sekunden: sekunden)
        if !Self.nurLesend(name) { kontextVorrat = ""; kontextZeit = .distantPast }
        let ms = Int(Date().timeIntervalSince(start) * 1000)
        // Das Logbuch soll die App verbessern, nicht das Leben mitschreiben: Private Inhalte nur als Länge, Dateiabrufe nur bei Fehlern.
        if name != "ablage_datei" || e.fehler {
            let privat = Self.privateWerkzeuge.contains(name)
            log("werkzeug", "Handy-Werkzeug \(name): \(e.fehler ? "Fehler" : "ok")", wo: "Handy über MCP", werkzeug: name,
                eingabe: privat ? nil : JSON.text(argumente), ergebnis: e.fehler ? nil : (privat ? "(\(e.text.count) Zeichen)" : e.text),
                fehler: e.fehler ? e.text : nil, dauerMs: ms)
        }
        return e
    }

    private func rufeOhneLog(_ name: String, _ argumente: JSONObjekt, sekunden: Double) async -> WerkzeugErgebnis {
        do {
            let r = try await rpc("tools/call", ["name": name, "arguments": argumente], sekunden: sekunden)
            let inhalt = r["content"] as? [JSONObjekt] ?? []
            var text = inhalt.compactMap { $0["text"] as? String }.joined(separator: "\n")
            var bild: Data? = nil
            if let b = inhalt.first(where: { ($0["type"] as? String) == "image" }), let s = b["data"] as? String {
                bild = Data(base64Encoded: s)
            }
            if text.isEmpty, let sc = r["structuredContent"] { text = JSON.text(sc) }
            return WerkzeugErgebnis(text: text.isEmpty ? "(leer)" : text, fehler: r["isError"] as? Bool ?? false, bildPNG: bild)
        } catch {
            return WerkzeugErgebnis(text: "Handy-Jarvis nicht erreichbar: \(error.localizedDescription)", fehler: true)
        }
    }

    /// Bequemer Textaufruf für die Oberfläche (Tagesauswertung, Aufgaben, Kontext …).
    func text(_ name: String, _ argumente: JSONObjekt = [:], sekunden: Double = 60) async -> String? {
        let e = await rufe(name, argumente, sekunden: sekunden)
        return e.fehler ? nil : e.text
    }
}

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

    private var werkzeugCache: [WerkzeugDefinition] = []
    private var cacheZeit = Date.distantPast
    private(set) var anweisungen = ""
    private(set) var serverVersion = ""
    private var naechsteId = 1
    private var verbundeneAdresse = ""

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
        if a != verbundeneAdresse { werkzeugCache = []; anweisungen = ""; verbundeneAdresse = a }   // Relay gewechselt
        if !frisch, !werkzeugCache.isEmpty, Date().timeIntervalSince(cacheZeit) < 600 { return werkzeugCache }
        do {
            if anweisungen.isEmpty { try await verbinden() }
            let r = try await rpc("tools/list", sekunden: 30)
            let liste = (r["tools"] as? [JSONObjekt] ?? []).compactMap { t -> WerkzeugDefinition? in
                guard let n = t["name"] as? String else { return nil }
                var schema = t["inputSchema"] as? JSONObjekt ?? ["type": "object", "properties": [:] as JSONObjekt]
                if schema["type"] == nil { schema["type"] = "object" }
                return WerkzeugDefinition(name: n, beschreibung: t["description"] as? String ?? "", schema: schema)
            }.sorted { $0.name < $1.name }
            if !liste.isEmpty { werkzeugCache = liste; cacheZeit = Date() }
            log("system", "Handy-Jarvis verbunden: \(liste.count) Werkzeuge (\(serverVersion))")
        } catch {
            // Nicht mehr erreichbar: keinen alten Stand als „verbunden“ melden.
            werkzeugCache = []
            log("fehler", "Handy-Jarvis nicht erreichbar: \(error.localizedDescription)")
        }
        return werkzeugCache
    }

    var verbunden: Bool { !werkzeugCache.isEmpty }

    func rufe(_ name: String, _ argumente: JSONObjekt, sekunden: Double = 150) async -> WerkzeugErgebnis {
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

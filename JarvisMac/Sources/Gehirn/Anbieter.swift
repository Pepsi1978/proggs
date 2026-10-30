import Foundation

/// Antwort eines Modellschritts – immer im Format der Claude-Messages-API (kanonisches Verlaufsformat von JarvisMac).
struct ModellAntwort {
    var inhalt: [JSONObjekt]      // Inhaltsblöcke (text, tool_use, thinking, server_tool_use …), unverändert zurückzugeben
    var stopGrund: String         // end_turn, tool_use, pause_turn, max_tokens, refusal
    var eingabeTokens = 0
    var ausgabeTokens = 0
    var cacheTokens = 0
    var cacheSchreibTokens = 0
}

/// Listenpreise der Claude-API in Dollar je Million Tokens (Stand 06.10.2026). Cache schreiben = 1,25 × Eingabe. Websuche ist nicht enthalten.
enum ClaudePreis {
    private static let tabelle: [String: (ein: Double, aus: Double, cacheLesen: Double)] = [
        "claude-opus-5-5": (4, 20, 0.20),
        "claude-sonnet-5-5": (2, 10, 0.20),
        "claude-fable-5-1": (10, 50, 0.25),
        "claude-haiku-5-5": (0.10, 0.50, 0.01),
    ]
    static func dollar(modell: String, _ a: ModellAntwort) -> Double {
        guard var p = tabelle[modell] else { return 0 }
        // Haiku kostet oberhalb von 100 000 Tokens Eingabe das Fünffache.
        if modell == "claude-haiku-5-5", a.eingabeTokens + a.cacheTokens + a.cacheSchreibTokens > 100_000 { p = (0.50, 2.50, 0.05) }
        let ein = Double(a.eingabeTokens) * p.ein + Double(a.cacheSchreibTokens) * p.ein * 1.25 + Double(a.cacheTokens) * p.cacheLesen
        return (ein + Double(a.ausgabeTokens) * p.aus) / 1_000_000
    }
}

protocol ModellAnbieter {
    /// Ein Schritt: Verlauf (Messages-Format) rein, Inhaltsblöcke raus. `onText` bekommt Textstücke beim Streamen.
    func antworte(system: [String], werkzeuge: [WerkzeugDefinition], websuche: Bool, verlauf: [JSONObjekt],
                  modell: String, effort: String, onText: @escaping @Sendable (String) -> Void) async throws -> ModellAntwort
}

enum AnbieterFehler: LocalizedError {
    case keinSchluessel(String), http(Int, String), abgelehnt(String), netz(String)
    var errorDescription: String? {
        switch self {
        case .keinSchluessel(let a): return "Kein API-Schlüssel für \(a) eingetragen (Einstellungen → Konten)."
        case .http(let c, let t): return "Modell-Fehler \(c): \(t.prefix(400))"
        case .abgelehnt(let t): return "Das Modell hat die Anfrage abgelehnt. \(t)"
        case .netz(let t): return "Netzwerkfehler: \(t)"
        }
    }
}

// MARK: - Claude (Messages API, Streaming, Prompt-Caching)

struct AnthropicKlient: ModellAnbieter {
    let schluessel: String

    func antworte(system: [String], werkzeuge: [WerkzeugDefinition], websuche: Bool, verlauf: [JSONObjekt],
                  modell: String, effort: String, onText: @escaping @Sendable (String) -> Void) async throws -> ModellAntwort {
        guard !schluessel.isEmpty else { throw AnbieterFehler.keinSchluessel("Anthropic") }
        var req = URLRequest(url: URL(string: "https://api.anthropic.com/v1/messages")!, timeoutInterval: 600)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "content-type")
        req.setValue(schluessel, forHTTPHeaderField: "x-api-key")
        req.setValue("2026-10-21", forHTTPHeaderField: "anthropic-version")

        let mitFallback = ["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1"].contains(modell)
        if mitFallback { req.setValue("server-side-fallback-2026-07-01", forHTTPHeaderField: "anthropic-beta") }

        // Prompt-Cache: tools → system → messages. Stabiles zuerst, Haltepunkte am letzten Werkzeug und an den Systemteilen.
        var tools: [JSONObjekt] = werkzeuge.map { ["name": $0.name, "description": String($0.beschreibung.prefix(4000)), "input_schema": $0.schema] }
        if websuche {
            let neu = ["claude-opus-5-5", "claude-sonnet-5-5"].contains(modell)
            tools.append(["type": neu ? "web_search_20260209" : "web_search_20250305", "name": "web_search", "max_uses": 3])
        }
        if !tools.isEmpty { tools[tools.count - 1]["cache_control"] = ["type": "ephemeral"] }
        let systemBloecke: [JSONObjekt] = system.filter { !$0.isEmpty }.enumerated().map { i, t in
            var b: JSONObjekt = ["type": "text", "text": t]
            if i < 2 { b["cache_control"] = ["type": "ephemeral"] }
            return b
        }
        var verlaufMitCache = verlauf
        if var letzte = verlaufMitCache.last, var inhalt = letzte["content"] as? [JSONObjekt], !inhalt.isEmpty {
            inhalt[inhalt.count - 1]["cache_control"] = ["type": "ephemeral"]
            letzte["content"] = inhalt
            verlaufMitCache[verlaufMitCache.count - 1] = letzte
        }
        var rumpf: JSONObjekt = [
            "model": modell, "max_tokens": 32000, "stream": true,
            "system": systemBloecke, "messages": verlaufMitCache,
            "output_config": ["effort": effort],
        ]
        if !tools.isEmpty { rumpf["tools"] = tools }
        if mitFallback { rumpf["fallbacks"] = "default" }
        req.httpBody = JSON.daten(rumpf)

        let (bytes, antwort): (URLSession.AsyncBytes, URLResponse)
        do { (bytes, antwort) = try await URLSession.shared.bytes(for: req) } catch { throw AnbieterFehler.netz(error.localizedDescription) }
        let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
        if code != 200 {
            var t = ""
            for try await z in bytes.lines { t += z; if t.count > 4000 { break } }
            throw AnbieterFehler.http(code, t)
        }

        var bloecke: [Int: JSONObjekt] = [:]
        var teilJSON: [Int: String] = [:]
        var ergebnis = ModellAntwort(inhalt: [], stopGrund: "end_turn")
        for try await zeile in bytes.lines {
            guard zeile.hasPrefix("data:") else { continue }
            guard let e = JSON.objekt(String(zeile.dropFirst(5)).trimmingCharacters(in: .whitespaces)) else { continue }
            switch e["type"] as? String ?? "" {
            case "message_start":
                if let u = (e["message"] as? JSONObjekt)?["usage"] as? JSONObjekt {
                    ergebnis.eingabeTokens = u["input_tokens"] as? Int ?? 0
                    ergebnis.cacheTokens = u["cache_read_input_tokens"] as? Int ?? 0
                    ergebnis.cacheSchreibTokens = u["cache_creation_input_tokens"] as? Int ?? 0
                }
            case "content_block_start":
                let i = e["index"] as? Int ?? 0
                bloecke[i] = e["content_block"] as? JSONObjekt ?? [:]
            case "content_block_delta":
                let i = e["index"] as? Int ?? 0
                guard let d = e["delta"] as? JSONObjekt, var b = bloecke[i] else { continue }
                switch d["type"] as? String ?? "" {
                case "text_delta":
                    let s = d["text"] as? String ?? ""
                    b["text"] = (b["text"] as? String ?? "") + s
                    onText(s)
                case "thinking_delta": b["thinking"] = (b["thinking"] as? String ?? "") + (d["thinking"] as? String ?? "")
                case "signature_delta": b["signature"] = d["signature"] as? String ?? ""
                case "input_json_delta": teilJSON[i, default: ""] += d["partial_json"] as? String ?? ""
                case "citations_delta":
                    var c = b["citations"] as? [Any] ?? []
                    if let x = d["citation"] { c.append(x) }
                    b["citations"] = c
                default: break
                }
                bloecke[i] = b
            case "content_block_stop":
                let i = e["index"] as? Int ?? 0
                if let roh = teilJSON[i], var b = bloecke[i] {
                    b["input"] = roh.isEmpty ? JSONObjekt() : (JSON.objekt(roh) ?? ["_unlesbar": roh])
                    bloecke[i] = b
                }
            case "message_delta":
                if let d = e["delta"] as? JSONObjekt, let s = d["stop_reason"] as? String { ergebnis.stopGrund = s }
                if let u = e["usage"] as? JSONObjekt { ergebnis.ausgabeTokens = u["output_tokens"] as? Int ?? 0 }
            case "error":
                let m = ((e["error"] as? JSONObjekt)?["message"] as? String) ?? "Unbekannter Fehler"
                throw AnbieterFehler.http(500, m)
            default: break
            }
        }
        ergebnis.inhalt = bloecke.keys.sorted().compactMap { bloecke[$0] }
        if ergebnis.stopGrund == "refusal" && ergebnis.inhalt.isEmpty { throw AnbieterFehler.abgelehnt("") }
        return ergebnis
    }
}

/// Einfacher Einzelaufruf ohne Werkzeuge (Titel, Lernen, Umformulieren).
enum Kurzauftrag {
    @MainActor static func frage(_ prompt: String, system: String = "", modell: String = "claude-haiku-5-5") async -> String? {
        let z = Einstellungen.shared.zugang
        // Ohne Claude-Schlüssel über das ChatGPT-Konto (GPT-6 Luna) – Titel und Lernen gehen dann trotzdem.
        let anbieter: ModellAnbieter = z.anthropic.isEmpty ? OpenAIKlient(schluessel: z.openai) : AnthropicKlient(schluessel: z.anthropic)
        let m = z.anthropic.isEmpty ? "gpt-6-luna" : modell
        let verlauf: [JSONObjekt] = [["role": "user", "content": [["type": "text", "text": prompt]]]]
        let a = try? await anbieter.antworte(system: [system], werkzeuge: [], websuche: false, verlauf: verlauf,
                                             modell: m, effort: "low", onText: { _ in })
        let t = a?.inhalt.compactMap { ($0["type"] as? String) == "text" ? $0["text"] as? String : nil }.joined()
        return t?.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

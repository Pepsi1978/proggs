import Foundation

/// OpenAI Responses API. Zwei Wege:
/// 1. Mit ChatGPT-Anmeldung (CodexAnmeldung, eigener Login am Mac) über chatgpt.com/backend-api/codex/responses –
///    wie die Handy-App, läuft über Franks ChatGPT-Abo. Hat Vorrang, sobald angemeldet.
/// 2. Sonst mit eigenem API-Schlüssel über api.openai.com/v1/responses.
/// Der kanonische Verlauf (Claude-Messages-Format) wird pro Aufruf in Responses-`input` übersetzt und die Antwort
/// zurück in Claude-Blöcke (text, tool_use).
struct OpenAIKlient: ModellAnbieter {
    let schluessel: String

    static let codexURL = URL(string: "https://chatgpt.com/backend-api/codex/responses")!
    static let apiURL = URL(string: "https://api.openai.com/v1/responses")!
    /// Neue Modelle (z. B. GPT-6.1 Sol) schaltet das Codex-Backend erst ab dieser Client-Kennung frei, sonst HTTP 400.
    static let codexVersion = "0.159.0"

    func antworte(system: [String], werkzeuge: [WerkzeugDefinition], websuche: Bool, verlauf: [JSONObjekt],
                  modell: String, effort: String, onText: @escaping @Sendable (String) -> Void) async throws -> ModellAntwort {
        let codex = await CodexAnmeldung.shared.angemeldet
        if !codex && schluessel.isEmpty { throw AnbieterFehler.keinSchluessel("OpenAI (API-Schlüssel oder ChatGPT-Anmeldung)") }

        var tools: [JSONObjekt] = werkzeuge.map {
            ["type": "function", "name": $0.name, "description": String($0.beschreibung.prefix(4000)),
             "parameters": $0.schema, "strict": false]
        }
        if websuche { tools.append(["type": "web_search"]) }

        var rumpf: JSONObjekt = [
            "model": modell,
            "stream": true,
            "store": false,
            "instructions": system.filter { !$0.isEmpty }.joined(separator: "\n\n"),
            "input": Self.eingabe(aus: verlauf),
            "reasoning": ["effort": Self.effort(effort, modell: modell, codex: codex)],
        ]
        if !tools.isEmpty {
            rumpf["tools"] = tools
            rumpf["tool_choice"] = "auto"
            rumpf["parallel_tool_calls"] = true
        }

        var versuch = 0
        while true {
            var req = URLRequest(url: codex ? Self.codexURL : Self.apiURL, timeoutInterval: 900)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("text/event-stream", forHTTPHeaderField: "Accept")
            var r = rumpf
            if codex {
                let (token, konto) = try await CodexAnmeldung.shared.gueltigerToken()
                req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                req.setValue("codex_cli_rs", forHTTPHeaderField: "originator")
                req.setValue(Self.codexVersion, forHTTPHeaderField: "version")
                req.setValue("codex_cli_rs/\(Self.codexVersion) (JarvisMac)", forHTTPHeaderField: "User-Agent")
                req.setValue(konto, forHTTPHeaderField: "ChatGPT-Account-ID")
                // Erster Versuch über den Priority-Pool wie am Handy; bei Serverfehlern ohne.
                if versuch == 0 { r["service_tier"] = "priority" }
            } else {
                req.setValue("Bearer \(schluessel)", forHTTPHeaderField: "Authorization")
            }
            req.httpBody = JSON.daten(r)
            do {
                return try await Self.stream(req, onText: onText)
            } catch AnbieterFehler.http(let code, let t) where (code == 408 || code >= 500) && versuch < 2 {
                versuch += 1
                log("fehler", "OpenAI \(code), neuer Versuch \(versuch): \(t.prefix(120))")
                try? await Task.sleep(for: .seconds(versuch == 1 ? 1 : 3))
            }
        }
    }

    // MARK: - Effort

    static func effort(_ e: String, modell: String, codex: Bool) -> String {
        let s = e.lowercased()
        if codex {
            // Codex-Backend: Astra, Sol, Terra bis ultra; Luna bis max.
            let erlaubt = modell.contains("luna") ? ["low", "medium", "high", "xhigh", "max"]
                                                  : ["low", "medium", "high", "xhigh", "max", "ultra"]
            return erlaubt.contains(s) ? s : "medium"
        }
        switch s {
        case "low", "medium", "high": return s
        case "xhigh", "max", "ultra": return "high"
        default: return "medium"
        }
    }

    // MARK: - Verlauf (Claude-Format) → Responses-input

    static func eingabe(aus verlauf: [JSONObjekt]) -> [JSONObjekt] {
        var input: [JSONObjekt] = []
        for nachricht in verlauf {
            let rolle = nachricht["role"] as? String ?? "user"
            let bloecke: [JSONObjekt]
            if let s = nachricht["content"] as? String { bloecke = [["type": "text", "text": s]] }
            else { bloecke = nachricht["content"] as? [JSONObjekt] ?? [] }

            if rolle == "assistant" {
                var texte: [String] = []
                func textAbschliessen() {
                    let t = texte.joined()
                    if !t.isEmpty {
                        input.append(["type": "message", "role": "assistant",
                                      "content": [["type": "output_text", "text": t]]])
                    }
                    texte = []
                }
                for b in bloecke {
                    switch b["type"] as? String ?? "" {
                    case "text": texte.append(b["text"] as? String ?? "")
                    case "tool_use":
                        textAbschliessen()
                        let args = b["input"].map { JSON.text($0) } ?? "{}"
                        input.append(["type": "function_call", "call_id": b["id"] as? String ?? "",
                                      "name": b["name"] as? String ?? "", "arguments": args])
                    default: break   // thinking, server_tool_use, web_search_tool_result … überspringen
                    }
                }
                textAbschliessen()
            } else {
                var teile: [JSONObjekt] = []
                var nachBildern: [JSONObjekt] = []
                func teileAbschliessen() {
                    if !teile.isEmpty { input.append(["type": "message", "role": "user", "content": teile]); teile = [] }
                }
                for b in bloecke {
                    switch b["type"] as? String ?? "" {
                    case "text": teile.append(["type": "input_text", "text": b["text"] as? String ?? ""])
                    case "image": if let u = bildURL(b) { teile.append(["type": "input_image", "image_url": u]) }
                    case "tool_result":
                        teileAbschliessen()
                        var text = ""
                        if let s = b["content"] as? String { text = s }
                        else {
                            for c in b["content"] as? [JSONObjekt] ?? [] {
                                if (c["type"] as? String) == "text" { text += (text.isEmpty ? "" : "\n") + (c["text"] as? String ?? "") }
                                else if (c["type"] as? String) == "image", let u = bildURL(c) {
                                    nachBildern.append(["type": "input_image", "image_url": u])
                                }
                            }
                        }
                        if b["is_error"] as? Bool == true { text = "FEHLER: " + text }
                        input.append(["type": "function_call_output", "call_id": b["tool_use_id"] as? String ?? "",
                                      "output": text.isEmpty ? "(leer)" : text])
                    default: break
                    }
                }
                teileAbschliessen()
                // Bilder aus Werkzeugergebnissen (z. B. Bildschirmfoto) als eigene Nutzernachricht nach den Ausgaben.
                if !nachBildern.isEmpty {
                    input.append(["type": "message", "role": "user",
                                  "content": [["type": "input_text", "text": "Bild aus dem Werkzeugergebnis:"]] + nachBildern])
                }
            }
        }
        return input
    }

    private static func bildURL(_ b: JSONObjekt) -> String? {
        guard let q = b["source"] as? JSONObjekt else { return nil }
        if let d = q["data"] as? String { return "data:\(q["media_type"] as? String ?? "image/png");base64,\(d)" }
        return q["url"] as? String
    }

    // MARK: - Streaming

    private static func stream(_ req: URLRequest, onText: @escaping @Sendable (String) -> Void) async throws -> ModellAntwort {
        let (bytes, antwort): (URLSession.AsyncBytes, URLResponse)
        do { (bytes, antwort) = try await URLSession.shared.bytes(for: req) } catch { throw AnbieterFehler.netz(error.localizedDescription) }
        let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
        if code != 200 {
            var t = ""
            for try await z in bytes.lines { t += z; if t.count > 4000 { break } }
            if code == 401 || code == 403 { await CodexAnmeldung.shared.ungueltigMelden(code) }
            throw AnbieterFehler.http(code, t)
        }

        var deltas = ""
        var items: [JSONObjekt] = []
        var gesehen = Set<String>()
        var ergebnis = ModellAntwort(inhalt: [], stopGrund: "end_turn")
        var fertig = false
        func sammle(_ item: JSONObjekt) {
            let schluessel = item["id"] as? String ?? item["call_id"] as? String ?? JSON.text(item)
            guard gesehen.insert(schluessel).inserted else { return }
            items.append(item)
        }
        for try await zeile in bytes.lines {
            guard zeile.hasPrefix("data:") else { continue }
            let daten = String(zeile.dropFirst(5)).trimmingCharacters(in: .whitespaces)
            if daten == "[DONE]" { break }
            guard let e = JSON.objekt(daten) else { continue }
            switch e["type"] as? String ?? "" {
            case "response.output_text.delta":
                if let s = e["delta"] as? String, !s.isEmpty { deltas += s; onText(s) }
            case "response.output_item.done":
                if let i = e["item"] as? JSONObjekt { sammle(i) }
            case "response.completed", "response.incomplete":
                let r = e["response"] as? JSONObjekt ?? [:]
                for i in r["output"] as? [JSONObjekt] ?? [] { sammle(i) }
                if let u = r["usage"] as? JSONObjekt {
                    ergebnis.eingabeTokens = u["input_tokens"] as? Int ?? 0
                    ergebnis.ausgabeTokens = u["output_tokens"] as? Int ?? 0
                    ergebnis.cacheTokens = (u["input_tokens_details"] as? JSONObjekt)?["cached_tokens"] as? Int ?? 0
                }
                if (e["type"] as? String) == "response.incomplete" { ergebnis.stopGrund = "max_tokens" }
                fertig = true
            case "response.failed", "error":
                let f = (e["response"] as? JSONObjekt)?["error"] as? JSONObjekt ?? e["error"] as? JSONObjekt ?? e
                throw AnbieterFehler.http(500, f["message"] as? String ?? JSON.text(f))
            default: break
            }
            if fertig { break }
        }

        var inhalt: [JSONObjekt] = []
        var textAusItems = ""
        var aufrufe: [JSONObjekt] = []
        for i in items {
            switch i["type"] as? String ?? "" {
            case "message":
                for c in i["content"] as? [JSONObjekt] ?? [] where (c["type"] as? String) == "output_text" {
                    textAusItems += c["text"] as? String ?? ""
                }
            case "function_call":
                let args = i["arguments"] as? String ?? ""
                let eingabe: JSONObjekt = args.isEmpty ? [:] : (JSON.objekt(args) ?? ["_unlesbar": args])
                aufrufe.append(["type": "tool_use", "id": i["call_id"] as? String ?? i["id"] as? String ?? UUID().uuidString,
                                "name": i["name"] as? String ?? "", "input": eingabe])
            default: break
            }
        }
        let text = textAusItems.isEmpty ? deltas : textAusItems
        if !text.isEmpty { inhalt.append(["type": "text", "text": text]) }
        inhalt += aufrufe
        if !aufrufe.isEmpty { ergebnis.stopGrund = "tool_use" }
        ergebnis.inhalt = inhalt
        return ergebnis
    }
}

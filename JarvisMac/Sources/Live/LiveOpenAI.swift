import Foundation

/// OpenAI: GPT-Live (`gpt-live-1`, wss://api.openai.com/v1/live/sessions, Client-Delegation: Jarvis denkt) und
/// Realtime (`gpt-realtime-*`, wss://api.openai.com/v1/realtime?model=…, Funktion jarvis_fragen). Protokoll wie
/// Jarvis/live/LiveGespraech.kt und best-practices/apis/openai-live-realtime.md §3–§7.
final class LiveOpenAI: LiveSitzung, @unchecked Sendable {
    private let schluessel: String
    private let modell: String
    private let stimme: String
    private var socket: LiveSocket?
    private let lock = NSLock()
    private var live: Bool { modell.hasPrefix("gpt-live") }

    // Mitschrift-Puffer (Fragmente in Lieferreihenfolge, Turns bildet die App selbst, §6)
    private var nutzerTeile = ""
    private var jarvisTeile = ""
    private var seitAuftrag = ""
    private var letzteNutzerMs: Int64 = 0
    private var letzteJarvisMs: Int64 = 0
    private var kennungArt: [String: String] = [:]
    // Unterbrechen per Antippen (GPT-Live hat kein Cancel): Rest der laufenden Antwort verwerfen.
    private var verwerfenSeitMs: Int64 = 0
    private var letzteVerworfenMs: Int64 = 0
    // Realtime: Kürzen beim Dazwischenreden
    private var antwortItem = ""
    private var verworfenItem = ""
    private var empfangenFrames = 0
    private var schliessenWartet = false

    init(ton: LiveTon, schluessel: String, modell: String, stimme: String) {
        self.schluessel = schluessel; self.modell = modell; self.stimme = stimme
        super.init(ton: ton)
    }

    private func sync<T>(_ f: () -> T) -> T { lock.lock(); defer { lock.unlock() }; return f() }

    override func starten() {
        let adresse = live ? "wss://api.openai.com/v1/live/sessions" : "wss://api.openai.com/v1/realtime?model=\(modell)"
        var anfrage = URLRequest(url: URL(string: adresse)!)
        anfrage.setValue("Bearer \(schluessel)", forHTTPHeaderField: "Authorization")
        socket = LiveSocket(anfrage: anfrage, beiOffen: { [weak self] in self?.geoeffnet() },
                            beiText: { [weak self] t in self?.ereignis(t) },
                            beiZu: { [weak self] code, grund in self?.geschlossen(code, grund) })
    }

    private func geoeffnet() {
        guard let s = socket else { return }
        if live {
            s.sende(liveStart())
        } else {
            s.sende(realtimeSitzung())
            // Getrennt: Lehnt der Dienst die Mitschrift ab, scheitert nur sie.
            s.sende(["type": "session.update", "session": ["type": "realtime", "audio": ["input": ["transcription": ["model": "gpt-transcribe", "language": "de"]]]]])
            s.sende(["type": "response.create", "response": ["instructions": LiveOptionen.begruessung]])
            bereit = true
            delegate?.liveBereit()
        }
    }

    private func geschlossen(_ code: Int, _ grund: String) {
        let warteteAufEnde = sync { schliessenWartet }
        if warteteAufEnde { return }
        let text: String
        switch code {
        case 401: text = "Der OpenAI-Schlüssel wurde abgelehnt."
        case 403: text = "Der Schlüssel darf dieses Live-Modell nicht nutzen."
        case 404: text = "Das Live-Modell „\(modell)“ gibt es nicht (mehr)."
        case 429: text = "Kein Guthaben oder zu viele Anfragen bei OpenAI."
        case 1000: text = ""
        default: text = "Live-Verbindung getrennt (\(code)): \(grund.prefix(200))"
        }
        if !text.isEmpty { log("live", "OpenAI schließt: \(code) \(grund.prefix(300))"); delegate?.liveFehler(text) }
        delegate?.liveEnde(text.isEmpty ? nil : text)
    }

    override func beenden() {
        guard let s = socket else { return }
        if live && s.offen {
            // Sauber beenden: session.close, kurz auf session.closed warten (Kosten laufen sonst weiter, §6).
            sync { schliessenWartet = true }
            s.sende(["type": "session.close"])
            DispatchQueue.global().asyncAfter(deadline: .now() + 3) { s.trennen() }
        } else {
            sync { schliessenWartet = true }
            s.trennen()
        }
    }

    override func mikro(_ pcm: Data) {
        guard bereit, let s = socket else { return }
        s.sende(["type": live ? "session.input_audio.append" : "input_audio_buffer.append", "audio": pcm.base64EncodedString()])
    }

    override func angetippt() {
        ton.leeren()
        let jetzt = LiveTon.jetztMs()
        sync { if live { verwerfenSeitMs = jetzt; letzteVerworfenMs = jetzt } else { verworfenItem = antwortItem } }
        if !live, !antwortItem.isEmpty { socket?.sende(["type": "response.cancel"]) }
    }

    // MARK: Sitzungsaufbau

    private func jarvisWerkzeug() -> JSONObjekt {
        ["type": "function", "name": "jarvis_fragen", "description": LiveOptionen.jarvisWerkzeugBeschreibung(),
         "parameters": ["type": "object",
                        "properties": ["auftrag": ["type": "string", "description": "Die vollständige Frage oder der Auftrag"]],
                        "required": ["auftrag"]]]
    }

    private func liveStart() -> JSONObjekt {
        let sitzung: JSONObjekt = [
            "model": modell,
            "instructions": LiveOptionen.anweisung(gptLive: true, nurStimme: false),
            "audio": ["format": ["type": "audio/pcm", "rate": 24000], "output": ["voice": stimme]],
            "store": false,
            "input": [["type": "message", "role": "developer", "content": [["type": "input_text", "text": LiveOptionen.begruessung]]]],
            "delegation": ["type": "client"],
        ]
        return ["type": "session.start", "event_id": "start-1", "session": sitzung]
    }

    private func realtimeSitzung() -> JSONObjekt {
        let format: JSONObjekt = ["type": "audio/pcm", "rate": 24000]
        let erkennung: JSONObjekt = ["type": "semantic_vad", "eagerness": "auto", "create_response": true, "interrupt_response": true]
        var s: JSONObjekt = [
            "type": "realtime", "model": modell, "output_modalities": ["audio"],
            "instructions": LiveOptionen.anweisung(gptLive: false, nurStimme: false),
            "audio": ["input": ["format": format, "turn_detection": erkennung, "noise_reduction": ["type": "far_field"]],
                      "output": ["format": format, "voice": stimme]],
            "tools": [jarvisWerkzeug()], "tool_choice": "auto",
            "truncation": ["type": "retention_ratio", "retention_ratio": 0.8],
        ]
        if ["gpt-realtime-2.1", "gpt-realtime-2.1-mini", "gpt-realtime-2"].contains(modell) { s["reasoning"] = ["effort": "low"] }
        return ["type": "session.update", "session": s]
    }

    // MARK: Ereignisse

    private func ereignis(_ text: String) {
        guard let e = JSON.objekt(text) else { return }
        if live { liveEreignis(e) } else { realtimeEreignis(e) }
    }

    private func nutzerFertig() {
        let t = sync { () -> String in let x = nutzerTeile; nutzerTeile = ""; return x }.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { delegate?.liveNutzerText(t) }
    }
    private func jarvisFertig() {
        let t = sync { () -> String in let x = jarvisTeile; jarvisTeile = ""; return x }.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { delegate?.liveJarvisText(t) }
    }

    private func liveEreignis(_ e: JSONObjekt) {
        let jetzt = LiveTon.jetztMs()
        switch e["type"] as? String ?? "" {
        case "session.started":
            bereit = true
            delegate?.liveBereit()
        case "session.output_audio.delta":
            guard let b = e["delta"] as? String, let daten = Data(base64Encoded: b) else { return }
            let verwerfen = sync { () -> Bool in
                guard verwerfenSeitMs > 0 else { return false }
                let hoerbar = Self.hoerbar(daten)
                // Rest der abgebrochenen Antwort: bis 1,5 s ohne hörbaren Ton, höchstens 10 s.
                if jetzt - verwerfenSeitMs < 10_000 && (!hoerbar || jetzt - letzteVerworfenMs < 1_500) {
                    if hoerbar { letzteVerworfenMs = jetzt }
                    return true
                }
                verwerfenSeitMs = 0
                return false
            }
            if !verwerfen { ton.spiele(daten, kappenErlaubt: true) }
        case "session.input_transcript.delta":
            // Mit echter Echounterdrückung am Mac: Kommt Franks Mitschrift, während Jarvis spricht, ist das Dazwischenreden.
            if ton.sprichtHoerbar { ton.leeren() }
            jarvisFertig()
            let d = e["delta"] as? String ?? ""
            sync { nutzerTeile += d; seitAuftrag += d; letzteNutzerMs = jetzt; verwerfenSeitMs = 0 }
        case "session.output_transcript.delta":
            nutzerFertig()
            let d = e["delta"] as? String ?? ""
            sync { jarvisTeile += d; letzteJarvisMs = jetzt }
        case "session.delegation.created":
            guard let d = e["delegation"] as? JSONObjekt, let id = d["id"] as? String else { return }
            jarvisFertig()
            if (d["target"] as? String ?? "client") == "client" {
                // Der Auftragstext fehlt im Ereignis (§5.2): aus Franks Mitschrift seit dem letzten Auftrag. Läuft sie dem
                // Ereignis hinterher, kurz auf sie warten.
                let leer = sync { seitAuftrag.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
                DispatchQueue.global(qos: .userInitiated).asyncAfter(deadline: .now() + (leer ? 1.5 : 0)) { [weak self] in
                    guard let self else { return }
                    self.nutzerFertig()
                    let auftrag = self.sync { () -> String in let x = self.seitAuftrag; self.seitAuftrag = ""; self.kennungArt[id] = "auftrag"; return x }
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    self.delegate?.liveWerkzeug(kennung: id, auftrag: auftrag, hinweis: "")
                }
            }
        case "session.closed":
            nutzerFertig(); jarvisFertig()
            let grund = e["reason"] as? String ?? ""
            if grund == "expired" { delegate?.liveFehler("Das Live-Gespräch hat die Höchstdauer erreicht.") }
            sync { schliessenWartet = true }
            socket?.trennen()
            delegate?.liveEnde(nil)
        case "error":
            let f = e["error"] as? JSONObjekt
            let t = [f?["message"] as? String, (f?["code"] as? String).map { "Code \($0)" }, (f?["param"] as? String).map { "Feld \($0)" }]
                .compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
            log("live", "GPT-Live-Fehler: \(t)")
            delegate?.liveFehler("GPT-Live: " + (t.isEmpty ? "unbekannter Fehler" : t))
            if !bereit { beenden(); delegate?.liveEnde(t) }
        default: break
        }
    }

    private func realtimeEreignis(_ e: JSONObjekt) {
        switch e["type"] as? String ?? "" {
        case "response.output_audio.delta", "response.audio.delta":
            let item = e["item_id"] as? String ?? ""
            guard let b = e["delta"] as? String, let daten = Data(base64Encoded: b) else { return }
            let spielen = sync { () -> Bool in
                if !item.isEmpty && item == verworfenItem { return false }
                if item != antwortItem { antwortItem = item; empfangenFrames = 0 }
                empfangenFrames += daten.count / 2
                return true
            }
            // Realtime grenzt Antworten selbst ab und liefert schubweise: nichts kappen.
            if spielen { ton.spiele(daten, kappenErlaubt: false) }
        case "input_audio_buffer.speech_started":
            let (item, ms) = sync { () -> (String, Int) in
                let gehoert = max(0, empfangenFrames - ton.gepuffert) * 1000 / Int(LiveTon.ausgabeRate)
                return (antwortItem, gehoert)
            }
            if !item.isEmpty && ton.sprichtHoerbar {
                // Auf die gehörte Länge kürzen, sonst glaubt das Modell, Frank habe alles gehört (§7).
                socket?.sende(["type": "conversation.item.truncate", "item_id": item, "content_index": 0, "audio_end_ms": ms])
            }
            ton.leeren()
        case "conversation.item.input_audio_transcription.completed":
            let t = (e["transcript"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !t.isEmpty { delegate?.liveNutzerText(t) }
        case "response.output_audio_transcript.done", "response.audio_transcript.done":
            let t = (e["transcript"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !t.isEmpty { delegate?.liveJarvisText(t) }
        case "response.function_call_arguments.done":
            let name = e["name"] as? String ?? ""
            if !name.isEmpty && name != "jarvis_fragen" { return }
            let auftrag = (JSON.objekt(e["arguments"] as? String ?? "")?["auftrag"] as? String) ?? ""
            let callId = e["call_id"] as? String ?? ""
            sync { kennungArt[callId] = "realtime" }
            delegate?.liveWerkzeug(kennung: callId, auftrag: auftrag, hinweis: "")
        case "error":
            let f = e["error"] as? JSONObjekt
            let code = f?["code"] as? String ?? ""
            if code == "response_cancel_not_active" || code == "conversation_already_has_active_response" { return }
            let m = f?["message"] as? String ?? ""
            if m.lowercased().contains("transcription") { return }
            log("live", "Realtime-Fehler: \(m)")
            delegate?.liveFehler("Live: " + (m.isEmpty ? "unbekannter Fehler" : m))
        default: break
        }
    }

    // MARK: Ergebnisse

    override func ergebnis(kennung: String, text: String) -> Bool {
        guard let s = socket, s.offen else { return false }
        let art = sync { kennungArt.removeValue(forKey: kennung) }
        if art == "auftrag" {
            // Höchstens 500 Tokens je Kommentar.
            sync { kennungArt[kennung] = "auftrag_beantwortet" }
            return s.sende(["type": "session.commentary.append", "delegation_id": kennung, "content": String(text.prefix(1500))])
        }
        s.sende(["type": "conversation.item.create", "item": ["type": "function_call_output", "call_id": kennung, "output": text]])
        return s.sende(["type": "response.create"])
    }

    override func nachreichen(kennung: String, text: String) -> Bool {
        guard let s = socket, s.offen else { return false }
        if live {
            // Mehrere Kommentare mit derselben delegation_id setzen fort (§5.2).
            return s.sende(["type": "session.commentary.append", "delegation_id": kennung, "content": String(text.prefix(1500))])
        }
        return s.sende(["type": "response.create", "response": ["instructions":
            "Sag Frank jetzt auf Deutsch in natürlichen Worten das Ergebnis von Jarvis zu seiner Anfrage von eben, ohne Fakten zu verändern: \(text.prefix(3000))"]])
    }

    override func takt() {
        // GPT-Live meldet keine Turn-Grenzen: nach 2 s ohne neue Fragmente gilt der Beitrag als fertig (§6).
        guard live else { return }
        let jetzt = LiveTon.jetztMs()
        let (n, j) = sync { (!nutzerTeile.isEmpty && jetzt - letzteNutzerMs > 2_000, !jarvisTeile.isEmpty && jetzt - letzteJarvisMs > 2_000) }
        if n { nutzerFertig() }
        if j { jarvisFertig() }
    }

    static func hoerbar(_ d: Data) -> Bool {
        let n = d.count / 2
        guard n > 0 else { return false }
        var summe: Double = 0
        d.withUnsafeBytes { r in let s = r.bindMemory(to: Int16.self); for i in 0..<n { let v = Double(s[i]); summe += v * v } }
        return sqrt(summe / Double(n)) >= LiveTon.hoerbarEffektiv
    }
}

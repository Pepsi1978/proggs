import Foundation

/// Gemini Live (BidiGenerateContent per WebSocket) mit „Jarvis direkt“ wie auf dem Handy: Die Weiche gibt jeden
/// abgeschlossenen Beitrag mit Franks erkannten Worten an Jarvis; Gemini spricht nur Jarvis' Antworten und kurze neutrale
/// Signale. Verbindungen enden nach etwa 10 Minuten (goAway) und werden mit der Fortsetzungskennung nahtlos erneuert;
/// bei Abbruch nach 2 s neu, lehnt Google ab („already connected“), noch nach 3 und 6 s, danach als neue Sitzung mit dem
/// bisher Gesagten (letzte 12 Beiträge).
final class LiveGemini: LiveSitzung, @unchecked Sendable {
    private let schluessel: String
    private let modell: String
    private let stimme: String
    private let weiche: JarvisWeiche
    private let lock = NSLock()
    private var socket: LiveSocket?
    private var abgeloest: LiveSocket?
    private var apiVersion = "v1beta"
    private var fortsetzKennung = ""
    private var begruesst = false
    private var beendet = false
    private var wartetAufNeu = false
    private var neuVersuche = 0
    private var nutzerTeile = ""
    private var antwortLaeuft = false
    private var aufrufVerbindung: [String: LiveSocket] = [:]
    private var diesesGespraech: [(Bool, String)] = []
    private var jarvisTeile = ""
    override var istGemini: Bool { true }

    /// Werkzeugaufrufe: Extended Thinking nebenbei (NON_BLOCKING), 3.8 Live wartend (BLOCKING), 3.1 Flash ohne Angabe.
    private var nebenbei: Bool { modell == "gemini-3.8-live-extended-thinking" }

    init(ton: LiveTon, schluessel: String, modell: String, stimme: String) {
        self.schluessel = schluessel; self.modell = modell; self.stimme = stimme
        // Ruhezeit wie am Handy: Pause bis zur Antwort (800 ms) + 1200 ms.
        weiche = JarvisWeiche(ruheMs: 800 + 1_200)
        super.init(ton: ton)
    }

    private func sync<T>(_ f: () -> T) -> T { lock.lock(); defer { lock.unlock() }; return f() }

    override func starten() { verbinden() }

    private func verbinden() {
        let enc = schluessel.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? schluessel
        let adresse = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.\(apiVersion).GenerativeService.BidiGenerateContent?key=\(enc)"
        bereit = false
        let alt = sync { socket }
        var neu: LiveSocket?
        neu = LiveSocket(anfrage: URLRequest(url: URL(string: adresse)!),
                         beiOffen: { [weak self] in guard let self, let n = neu else { return }; n.sende(self.setup()) },
                         beiText: { [weak self] t in guard let self, let n = neu else { return }; self.ereignis(n, t) },
                         beiZu: { [weak self] code, grund in guard let self, let n = neu else { return }; self.geschlossen(n, code, grund) })
        sync {
            if let a = alt { abgeloest?.trennen(); abgeloest = a }
            socket = neu
            wartetAufNeu = false
        }
    }

    private func setup() -> JSONObjekt {
        var gen: JSONObjekt = ["responseModalities": ["AUDIO"],
                               "speechConfig": ["voiceConfig": ["prebuiltVoiceConfig": ["voiceName": stimme]]]]
        if let stufe = LiveOptionen.geminiDenkstufe(modell) { gen["thinkingConfig"] = ["thinkingLevel": stufe] }
        // Neue Sitzung mitten im Gespräch ohne Fortsetzung: das bisher Gesagte dazu.
        let dieses = sync { begruesst && fortsetzKennung.isEmpty ? diesesGespraech : [] }
        let bisher = dieses.isEmpty ? "" : "\n# Bisheriges Gespräch (zum Anknüpfen)\n" +
            dieses.map { ($0.0 ? "Frank: " : "Jarvis: ") + String($0.1.prefix(1200)) }.joined(separator: "\n")
        var funktion: JSONObjekt = [
            "name": "jarvis_fragen", "description": LiveOptionen.jarvisWerkzeugBeschreibung(),
            "parameters": ["type": "OBJECT", "properties": ["auftrag": ["type": "STRING", "description": "Die vollständige Frage oder der Auftrag"]], "required": ["auftrag"]],
        ]
        if nebenbei { funktion["behavior"] = "NON_BLOCKING" } else if modell == "gemini-3.8-live" { funktion["behavior"] = "BLOCKING" }
        let erkennung: JSONObjekt = ["startOfSpeechSensitivity": "START_SENSITIVITY_LOW", "endOfSpeechSensitivity": "END_SENSITIVITY_LOW",
                                     "prefixPaddingMs": 20, "silenceDurationMs": 800]
        var fortsetzung: JSONObjekt = [:]
        let kennung = sync { fortsetzKennung }
        if !kennung.isEmpty { fortsetzung["handle"] = kennung }
        let s: JSONObjekt = [
            "model": "models/\(modell)",
            "generationConfig": gen,
            "systemInstruction": ["parts": [["text": LiveOptionen.anweisung(gptLive: false, nurStimme: true) + bisher +
                "\n# Language\nRESPOND IN GERMAN. YOU MUST RESPOND UNMISTAKABLY IN GERMAN."]]],
            "tools": [["functionDeclarations": [funktion]]],
            "realtimeInputConfig": ["automaticActivityDetection": erkennung, "activityHandling": "START_OF_ACTIVITY_INTERRUPTS"],
            "outputAudioTranscription": [:] as JSONObjekt,
            "inputAudioTranscription": [:] as JSONObjekt,
            "contextWindowCompression": ["slidingWindow": [:] as JSONObjekt, "triggerTokens": 16000],
            "sessionResumption": fortsetzung,
        ]
        return ["setup": s]
    }

    private func geschlossen(_ w: LiveSocket, _ code: Int, _ grund: String) {
        let (aktuell, ende, wartet) = sync { (w === socket, beendet, wartetAufNeu) }
        guard aktuell, !ende, !wartet else { return }
        log("live", "Gemini schließt: \(code) \(grund.prefix(300))")
        let g = grund.lowercased()
        if code == 401 || code == 403 || g.contains("api key") || g.contains("permission_denied") || g.contains("unauthenticated") {
            delegate?.liveFehler("Der Gemini-Schlüssel wurde abgelehnt."); delegate?.liveEnde("Schlüssel"); return
        }
        if g.contains("quota") || g.contains("resource_exhausted") || code == 429 {
            delegate?.liveFehler("Gemini-Kontingent erschöpft oder zu viele Anfragen."); delegate?.liveEnde("Kontingent"); return
        }
        let modellFehler = (code == 1007 || code == 1008 || g.contains("not found") || g.contains("not supported") || g.contains("invalid argument"))
            && !g.contains("already connected") && !g.contains("existing client")
        if modellFehler {
            if !bereit && apiVersion == "v1beta" && modell.contains("extended") { apiVersion = "v1alpha"; verbinden(); return }
            delegate?.liveFehler("Gemini hat den Verbindungsaufbau abgelehnt: \(grund.prefix(160))"); delegate?.liveEnde("Modell"); return
        }
        // Netz weg, Serverfehler oder abgelehnte Fortsetzung: neu verbinden.
        let versuch = sync { () -> Int in neuVersuche += 1; wartetAufNeu = true; return neuVersuche }
        if versuch > 5 {
            delegate?.liveFehler("Gemini-Verbindung mehrfach abgebrochen. Bitte neu starten."); delegate?.liveEnde("Netz"); return
        }
        let warte: [Double] = [2, 3, 6, 2, 2]
        if versuch > 3 { sync { fortsetzKennung = "" } }   // danach als neue Sitzung mit dem bisher Gesagten
        log("live", "Gemini: Neuverbindung \(versuch) in \(warte[versuch - 1]) s")
        weiche.verbindungNeu()
        DispatchQueue.global().asyncAfter(deadline: .now() + warte[versuch - 1]) { [weak self] in
            guard let self, !self.sync({ self.beendet }) else { return }
            self.verbinden()
        }
    }

    override func beenden() {
        let (s, a) = sync { () -> (LiveSocket?, LiveSocket?) in beendet = true; return (socket, abgeloest) }
        s?.trennen(); a?.trennen()
    }

    override func mikro(_ pcm: Data) {
        guard bereit, let s = sync({ wartetAufNeu ? nil : socket }) else { return }
        s.sende(["realtimeInput": ["audio": ["data": pcm.base64EncodedString(), "mimeType": "audio/pcm;rate=16000"]]])
    }

    override func angetippt() {
        weiche.angetippt()
        ton.leeren()
    }

    private func merken(_ vonFrank: Bool, _ text: String) {
        sync { diesesGespraech.append((vonFrank, text)); if diesesGespraech.count > 12 { diesesGespraech.removeFirst(diesesGespraech.count - 12) } }
    }

    private func nutzerFertig() {
        let t = sync { () -> String in let x = nutzerTeile; nutzerTeile = ""; return x }.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { merken(true, t); delegate?.liveNutzerText(t) }
    }
    private func jarvisFertig() {
        let t = sync { () -> String in let x = jarvisTeile; jarvisTeile = ""; return x }.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { merken(false, t); delegate?.liveJarvisText(t) }
    }

    private func ereignis(_ w: LiveSocket, _ text: String) {
        guard let e = JSON.objekt(text) else { return }
        let (istAktuell, istAlt) = sync { (w === socket, w === abgeloest) }
        guard istAktuell || istAlt else { return }
        // Von der abgelösten Verbindung nur noch Ton, Mitschrift und Werkzeugaufrufe.
        if !istAktuell && (e["setupComplete"] != nil || e["goAway"] != nil || e["sessionResumptionUpdate"] != nil) { return }
        let jetzt = LiveTon.jetztMs()

        if e["setupComplete"] != nil {
            bereit = true
            let (alt, warBegruesst) = sync { () -> (LiveSocket?, Bool) in
                neuVersuche = 0
                let a = abgeloest; abgeloest = nil
                let b = begruesst; begruesst = true
                return (a, b)
            }
            if let alt { DispatchQueue.global().asyncAfter(deadline: .now() + 1.5) { alt.trennen() } }
            if warBegruesst { weiche.verbindungNeu() } else { w.sende(["realtimeInput": ["text": LiveOptionen.begruessung]]) }
            delegate?.liveBereit()
            return
        }
        if let c = e["serverContent"] as? JSONObjekt {
            if let vorl = (c["interimInputTranscription"] as? JSONObjekt)?["text"] as? String, !vorl.isEmpty {
                ausfuehren(weiche.nutzerSpricht(jetzt))
            }
            if let d = (c["inputTranscription"] as? JSONObjekt)?["text"] as? String, !d.isEmpty {
                sync { nutzerTeile += d }
                ausfuehren(weiche.nutzerText(d, jetzt))
            }
            let frei = weiche.tonFrei
            if c["interrupted"] as? Bool == true {
                sync { antwortLaeuft = false }
                ton.leeren()
                jarvisFertig()
                ausfuehren(weiche.unterbrochen(jetzt))
            }
            if let d = (c["outputTranscription"] as? JSONObjekt)?["text"] as? String, !d.isEmpty {
                if frei { sync { jarvisTeile += d } } else { weiche.eigeneMitschrift(d, jetzt) }
            }
            let teile = ((c["modelTurn"] as? JSONObjekt)?["parts"] as? [JSONObjekt]) ?? []
            var toene: [Data] = []
            for p in teile where p["thought"] as? Bool != true {
                if let s = (p["inlineData"] as? JSONObjekt)?["data"] as? String, let d = Data(base64Encoded: s) { toene.append(d) }
            }
            if !toene.isEmpty || c["outputTranscription"] != nil { ausfuehren(weiche.modellAusgabe(jetzt, ton: !toene.isEmpty)) }
            if frei {
                if !toene.isEmpty { sync { antwortLaeuft = true } }
                toene.forEach { ton.spiele($0, kappenErlaubt: false) }
            } else {
                toene.forEach { weiche.eigenerTon($0, jetzt) }
            }
            if c["turnComplete"] as? Bool == true {
                sync { antwortLaeuft = false }
                jarvisFertig()
                ausfuehren(weiche.zugEnde(jetzt))
            }
            return
        }
        if let tc = e["toolCall"] as? JSONObjekt, let aufrufe = tc["functionCalls"] as? [JSONObjekt] {
            for f in aufrufe where (f["name"] as? String) == "jarvis_fragen" {
                let id = f["id"] as? String ?? UUID().uuidString
                sync { aufrufVerbindung[id] = w }
                let auftrag = (f["args"] as? JSONObjekt)?["auftrag"] as? String ?? ""
                ausfuehren(weiche.werkzeugAufruf(id, auftrag, jetzt))
            }
            return
        }
        if let tc = e["toolCallCancellation"] as? JSONObjekt, let ids = tc["ids"] as? [String] {
            log("live", "Gemini bricht Werkzeugaufruf ab: \(ids)")
            sync { ids.forEach { aufrufVerbindung.removeValue(forKey: $0) } }
            weiche.werkzeugAbgebrochen(ids)
            return
        }
        if let u = e["sessionResumptionUpdate"] as? JSONObjekt {
            if u["resumable"] as? Bool == true, let h = u["newHandle"] as? String, !h.isEmpty { sync { fortsetzKennung = h } }
            return
        }
        if e["goAway"] != nil {
            let kann = sync { !fortsetzKennung.isEmpty && !beendet }
            log("live", "Gemini goAway, Fortsetzung \(kann ? "folgt" : "nicht möglich")")
            if kann { verbinden() }
        }
    }

    /// Führt die Aktionen der Weiche aus.
    private func ausfuehren(_ aktionen: [JarvisWeiche.Aktion]) {
        for a in aktionen {
            switch a {
            case .nutzerFertig:
                nutzerFertig()
            case .anJarvis(let kennung, let auftrag, let hinweis):
                jarvisFertig()
                delegate?.liveWerkzeug(kennung: kennung, auftrag: auftrag, hinweis: hinweis)
            case .werkzeugantwort(let ids, let text):
                guard let w = sync({ socket }) else { continue }
                for (i, id) in ids.enumerated() {
                    var r: JSONObjekt = ["result": i == 0 ? text : "Die Antwort von Jarvis steht im ersten Ergebnis."]
                    if nebenbei { r["scheduling"] = "INTERRUPT" }
                    let ziel = sync { () -> LiveSocket in
                        let z = aufrufVerbindung.removeValue(forKey: id)
                        return (z != nil && (z === socket || z === abgeloest)) ? z! : w
                    }
                    ziel.sende(["toolResponse": ["functionResponses": [["id": id, "name": "jarvis_fragen", "response": r]]]])
                }
            case .einspielen(let text):
                guard let w = sync({ socket }) else { continue }
                // 3.8: clientContent ist während der ganzen Sitzung erlaubt und kommt in Reihenfolge an; 3.1 nur realtimeInput.
                if modell.hasPrefix("gemini-3.8") {
                    w.sende(["clientContent": ["turns": [["role": "user", "parts": [["text": text]]]], "turnComplete": true]])
                } else {
                    w.sende(["realtimeInput": ["text": text]])
                }
            case .anzeigen(let text):
                // Jarvis' Antwort steht über den Agenten schon im Gespräch; nur ins Live-Protokoll.
                log("live", "Gemini hat Jarvis' Antwort nicht gesprochen: \(text.prefix(120))")
            case .signal(let toene, _):
                toene.forEach { ton.spiele($0, kappenErlaubt: false) }
            case .fehler(let t):
                log("live", "Weiche: \(t)")
                delegate?.liveFehler(t)
            case .protokoll(let t):
                log("live", t)
            }
        }
    }

    override func ergebnis(kennung: String, text: String) -> Bool {
        ausfuehren(weiche.jarvisAntwort(kennung, text, LiveTon.jetztMs()))
        return sync { socket?.offen ?? false }
    }

    override func nachreichen(kennung: String, text: String) -> Bool {
        guard sync({ socket?.offen ?? false }) else { return false }
        ausfuehren(weiche.nachreichen(text, LiveTon.jetztMs()))
        return true
    }

    override func takt() {
        guard bereit else { return }
        ausfuehren(weiche.takt(LiveTon.jetztMs()))
    }

    /// Jarvis arbeitet laut Weiche (für die Statusanzeige).
    var weicheArbeitet: Bool { weiche.arbeitet }
}

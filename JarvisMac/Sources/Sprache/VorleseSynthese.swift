import AVFoundation
import CryptoKit
import Foundation

/// Fertig erzeugter Ton eines Absatzes (mp3 oder wav), noch nicht abgespielt.
struct VorleseTon: Sendable {
    var daten: Data
    var endung: String
}

enum VorleseFehler: LocalizedError {
    /// Betrifft die ganze Stimme (Schlüssel fehlt/abgelehnt, Kontingent) – weiter zur nächsten Rückfallstufe.
    case abbruch(String)
    /// Fehler an genau diesem Absatz.
    case fehler(String)
    /// Die Stimme hat nicht rechtzeitig geantwortet.
    case zeitlimit(String)
    var errorDescription: String? {
        switch self { case .abbruch(let t), .fehler(let t), .zeitlimit(let t): return t }
    }
}

/// Gemini-Stimmen: nacheinander statt parallel (gemeinsames Minutenlimit), kostenloser Schlüssel nach einem Fehler
/// 10 Minuten übersprungen, wenn es einen Ausweichweg gibt (wie Synthese.kt).
actor GeminiTor {
    static let shared = GeminiTor()
    private var belegt = false
    private var wartende: [CheckedContinuation<Void, Never>] = []
    private var freiGesperrtBis: [String: Date] = [:]

    func betreten() async {
        if !belegt { belegt = true; return }
        await withCheckedContinuation { wartende.append($0) }
    }
    func verlassen() {
        if wartende.isEmpty { belegt = false } else { wartende.removeFirst().resume() }
    }
    func freiGesperrt(_ modell: String) -> Bool { (freiGesperrtBis[modell] ?? .distantPast) > Date() }
    func sperreFrei(_ modell: String) { freiGesperrtBis[modell] = Date().addingTimeInterval(600) }
}

/// Erzeugt Vorleseton mit allen Anbietern der Android-App – Endpunkte, Modelle und Anfrageformate aus Synthese.kt,
/// EdgeTtsPlayer.kt und GoogleCloudTtsPlayer.kt. Scheitert die Hauptstimme, geht es die Rückfallkette durch, danach Edge.
enum VorleseSynthese {
    static let googleURL = "https://texttospeech.googleapis.com/v1/text:synthesize"
    static let geminiURL = "https://generativelanguage.googleapis.com/v1beta/models/"
    static let qwenURL = "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"

    /// Ton für einen Absatz samt dem Anbieter, der ihn tatsächlich erzeugt hat (für das Tempo-Strecken bei Gemini).
    static func synthetisiere(_ text: String, _ s: VorleseStand) async throws -> (VorleseTon, VorleseAnbieter) {
        let haupt = s.anbieter
        let hauptFehler: Error
        do {
            return (try await erzeuge(haupt, text, s, schluesselStufe: nil, mitAusweg: !s.kette.isEmpty), haupt)
        } catch {
            if error is CancellationError { throw error }
            hauptFehler = error
        }
        var versucht: Set<VorleseAnbieter> = [haupt]
        for (i, stufe) in s.kette.enumerated() {
            if Task.isCancelled { throw CancellationError() }
            do {
                let ton = try await erzeuge(stufe.anbieter, text, s, schluesselStufe: stufe.schluessel, mitAusweg: i < s.kette.count - 1)
                log("sprache", "Vorlese-Rückfall: \(stufe.titel) statt \(haupt.titel) (\(hauptFehler.localizedDescription.prefix(80)))")
                return (ton, stufe.anbieter)
            } catch {
                versucht.insert(stufe.anbieter)
                log("fehler", "Vorlese-Rückfall \(stufe.titel) gescheitert: \(error.localizedDescription.prefix(120))")
            }
        }
        // Edge braucht keinen Schlüssel und ist der garantierte Rückfall.
        if !versucht.contains(.edge) {
            if let ton = try? await erzeuge(.edge, text, s, schluesselStufe: nil, mitAusweg: false) {
                log("sprache", "Vorlese-Rückfall: Microsoft Edge statt \(haupt.titel)")
                return (ton, .edge)
            }
        }
        throw hauptFehler
    }

    static func erzeuge(_ a: VorleseAnbieter, _ text: String, _ s: VorleseStand, schluesselStufe: String?, mitAusweg: Bool) async throws -> VorleseTon {
        switch a {
        case .google: return try await google(text, stimme: s.stimme(.google), s)
        case .geminiFlash: return try await gemini(text, modell: VorleseKatalog.geminiFlashModell, stimme: s.stimme(.geminiFlash), s, nur: schluesselStufe, mitAusweg: mitAusweg)
        case .geminiLite: return try await gemini(text, modell: VorleseKatalog.geminiLiteModell, stimme: s.stimme(.geminiLite), s, nur: schluesselStufe, mitAusweg: mitAusweg)
        case .edge: return try await edge(text, stimme: s.stimme(.edge), tempo: s.tempo(.edge), deutsch: s.immerDeutsch)
        case .azure: return try await azure(text, stimme: s.stimme(.azure), s)
        case .qwen: return try await qwen(text, geklont: false, s)
        case .qwenKlon: return try await qwen(text, geklont: true, s)
        case .system: throw VorleseFehler.abbruch("Systemstimme wird direkt gesprochen.")
        }
    }

    // MARK: HTTP mit Wiederholung

    private static let wartezeiten: [Double] = [1, 3, 8]

    /// Führt die Anfrage aus. 5xx und Netzfehler werden wiederholt; 429 wartet nur mit `warteBei429` (retryDelay bis 65 s).
    static func fuehreAus(_ req: URLRequest, warteBei429: Bool = false, name: String) async throws -> Data {
        var versuch = 0
        while true {
            try Task.checkCancellation()
            do {
                let (daten, antwort) = try await URLSession.shared.data(for: req)
                let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
                let text = String(decoding: daten.prefix(400), as: UTF8.self)
                switch code {
                case 200..<300: return daten
                case 401, 403: throw VorleseFehler.abbruch("\(name) hat den Schlüssel abgelehnt (Fehler \(code)).")
                case 429:
                    var warte: Double? = nil
                    if let r = text.range(of: #""retryDelay"\s*:\s*"(\d+(?:\.\d+)?)s""#, options: .regularExpression) {
                        warte = Double(text[r].filter { $0.isNumber || $0 == "." })
                    }
                    if warteBei429, versuch < 3, (warte ?? 5) <= 65 {
                        try await Task.sleep(for: .seconds(warte ?? 5)); versuch += 1; continue
                    }
                    throw VorleseFehler.abbruch("\(name): Kontingent oder Ratenlimit erschöpft (Fehler 429).")
                case 500...:
                    if versuch < wartezeiten.count { try await Task.sleep(for: .seconds(wartezeiten[versuch])); versuch += 1; continue }
                    throw VorleseFehler.fehler("\(name) antwortete mit Fehler \(code).")
                default:
                    throw VorleseFehler.fehler("\(name) antwortete mit Fehler \(code): \(text.prefix(200))")
                }
            } catch let f as VorleseFehler {
                throw f
            } catch is CancellationError {
                throw CancellationError()
            } catch {
                if versuch < wartezeiten.count - 1 { try await Task.sleep(for: .seconds(wartezeiten[versuch])); versuch += 1; continue }
                throw VorleseFehler.fehler("\(name) nicht erreichbar: \(error.localizedDescription)")
            }
        }
    }

    static func ssmlEscape(_ t: String) -> String {
        t.replacingOccurrences(of: "&", with: "&amp;").replacingOccurrences(of: "<", with: "&lt;").replacingOccurrences(of: ">", with: "&gt;")
    }

    static func ohneSteuerzeichen(_ t: String) -> String {
        String(t.unicodeScalars.map { ($0.value < 0x20 && $0 != "\n" && $0 != "\r" && $0 != "\t") ? " " : Character($0) })
    }

    // MARK: Google Chirp 3 HD

    /// Feste deutsche Aussprachen (IPA) für Wörter, die Chirp sonst englisch spricht (DeutscheAussprache.kt).
    private static let aussprachen: [(String, String)] = [("Frank", "fʁaŋk"), ("Franks", "fʁaŋks")]

    static func google(_ text: String, stimme: String, _ s: VorleseStand) async throws -> VorleseTon {
        let schluessel = s.zugang.google.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !schluessel.isEmpty else { throw VorleseFehler.abbruch("Es ist kein Google-Schlüssel hinterlegt.") }
        let liste = aussprachen.filter { text.range(of: "\\b\($0.0)\\b", options: .regularExpression) != nil }
            .map { ["phrase": $0.0, "phoneticEncoding": "PHONETIC_ENCODING_IPA", "pronunciation": $0.1] }
        // Chirp versteht kein SSML: Aussprachen als customPronunciations; lehnt Google sie ab (400), ohne sie erneut.
        if !liste.isEmpty {
            do { return try await googleAnfrage(text, stimme: stimme, schluessel: schluessel, aussprachen: liste, tempo: s.tempo(.google)) }
            catch VorleseFehler.fehler(let t) where t.contains("Fehler 400") {}
        }
        return try await googleAnfrage(text, stimme: stimme, schluessel: schluessel, aussprachen: nil, tempo: s.tempo(.google))
    }

    private static func googleAnfrage(_ text: String, stimme: String, schluessel: String, aussprachen: [[String: String]]?, tempo: Double) async throws -> VorleseTon {
        var komp = URLComponents(string: googleURL)!
        komp.queryItems = [URLQueryItem(name: "key", value: schluessel)]
        var req = URLRequest(url: komp.url!, timeoutInterval: 60)
        req.httpMethod = "POST"
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        var eingabe: JSONObjekt = ["text": text]
        if let aussprachen { eingabe["customPronunciations"] = ["pronunciations": aussprachen] }
        req.httpBody = JSON.daten([
            "input": eingabe,
            "voice": ["languageCode": "de-DE", "name": stimme],
            // Chirp 3 HD kennt keinen pitch-Parameter – er wird gar nicht erst gesendet (sonst 400).
            "audioConfig": ["audioEncoding": "MP3", "speakingRate": min(max(tempo, 0.25), 4)],
        ] as JSONObjekt)
        let daten = try await fuehreAus(req, name: "Google")
        guard let b64 = JSON.objekt(daten)?["audioContent"] as? String, let mp3 = Data(base64Encoded: b64), !mp3.isEmpty else {
            throw VorleseFehler.fehler("Google hat kein Audio geliefert.")
        }
        return VorleseTon(daten: mp3, endung: "mp3")
    }

    // MARK: Gemini 3.8 Flash / Flash-Lite TTS

    static func gemini(_ text: String, modell: String, stimme: String, _ s: VorleseStand, nur: String?, mitAusweg: Bool) async throws -> VorleseTon {
        let wahl: String
        switch nur {
        case RueckfallStufe.frei: wahl = VorleseEinstellungen.geminiNurFrei
        case RueckfallStufe.bezahlt: wahl = VorleseEinstellungen.geminiNurBezahlt
        default: wahl = s.geminiWahl
        }
        let frei = s.zugang.geminiFrei.filter { !$0.isWhitespace }
        let bezahlt = s.zugang.geminiBezahlt.filter { !$0.isWhitespace }
        var fehlerFrei: Error?
        if wahl != VorleseEinstellungen.geminiNurBezahlt {
            let gesperrt = await GeminiTor.shared.freiGesperrt(modell)
            // Mit Ausweichweg nicht erst bei 429 warten – der nächste Weg ist schneller als jede Wartezeit.
            let ausweg = mitAusweg || (wahl == VorleseEinstellungen.geminiFreiDannBezahlt && !bezahlt.isEmpty)
            if !frei.isEmpty && !gesperrt {
                do {
                    return try await geminiAnfrage(text, modell: modell, stimme: stimme, schluessel: frei, warteBei429: !ausweg)
                } catch let f as VorleseFehler {
                    fehlerFrei = f
                    if ausweg { await GeminiTor.shared.sperreFrei(modell) }
                    log("fehler", "Gemini-Stimme: kostenloser Schlüssel gescheitert (\(modell)): \(f.localizedDescription.prefix(120))")
                }
            } else if !frei.isEmpty && wahl == VorleseEinstellungen.geminiNurFrei {
                fehlerFrei = VorleseFehler.abbruch("Der kostenlose Gemini-Schlüssel pausiert nach einem Fehler.")
            }
            if wahl == VorleseEinstellungen.geminiNurFrei {
                throw fehlerFrei ?? VorleseFehler.abbruch("Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt.")
            }
        }
        guard !bezahlt.isEmpty else {
            throw fehlerFrei ?? VorleseFehler.abbruch("Es ist kein Gemini-Schlüssel für die Stimme hinterlegt.")
        }
        return try await geminiAnfrage(text, modell: modell, stimme: stimme, schluessel: bezahlt, warteBei429: true)
    }

    private static func geminiAnfrage(_ text: String, modell: String, stimme: String, schluessel: String, warteBei429: Bool) async throws -> VorleseTon {
        var req = URLRequest(url: URL(string: "\(geminiURL)\(modell):generateContent")!, timeoutInterval: 90)
        req.httpMethod = "POST"
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        req.setValue(schluessel, forHTTPHeaderField: "x-goog-api-key")
        // Gemini liest den Text wörtlich vor; eine Anweisung im Text würde mitgesprochen.
        req.httpBody = JSON.daten([
            "contents": [["parts": [["text": text]]]],
            "generationConfig": [
                "responseModalities": ["AUDIO"],
                "speechConfig": ["voiceConfig": ["prebuiltVoiceConfig": ["voiceName": VorleseKatalog.geminiName(stimme)]]],
            ],
        ] as JSONObjekt)
        await GeminiTor.shared.betreten()
        let daten: Data
        do { daten = try await fuehreAus(req, warteBei429: warteBei429, name: "Gemini") } catch {
            await GeminiTor.shared.verlassen(); throw error
        }
        await GeminiTor.shared.verlassen()
        let kandidat = (JSON.objekt(daten)?["candidates"] as? [JSONObjekt])?.first
        let ende = kandidat?["finishReason"] as? String ?? ""
        let teil = (((kandidat?["content"] as? JSONObjekt)?["parts"] as? [JSONObjekt])?.first)?["inlineData"] as? JSONObjekt
        guard let b64 = teil?["data"] as? String, let roh = Data(base64Encoded: b64), !roh.isEmpty else {
            throw VorleseFehler.fehler("Gemini hat kein Audio geliefert (\(ende)).")
        }
        if !ende.isEmpty && ende != "STOP" { throw VorleseFehler.fehler("Gemini hat die Stimme abgebrochen (\(ende)).") }
        let mime = teil?["mimeType"] as? String ?? ""
        var rate = 24_000
        if let r = mime.range(of: #"rate=(\d+)"#, options: .regularExpression) { rate = Int(mime[r].filter(\.isNumber)) ?? 24_000 }
        let istWav = roh.count >= 12 && roh.prefix(4) == Data("RIFF".utf8)
        let wav = istWav ? roh : WavKopf.mit(pcm: roh, rate: rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        if !istWav {
            let sekunden = Double(roh.count) / Double(rate * 2)
            if text.count >= 60 && sekunden < Double(text.count) / 25 {
                throw VorleseFehler.fehler(String(format: "Gemini-Audio zu kurz (%.1f s für %d Zeichen).", sekunden, text.count))
            }
        }
        return VorleseTon(daten: wav, endung: "wav")
    }

    // MARK: Azure Neural

    static func azure(_ text: String, stimme stimmeId: String, _ s: VorleseStand) async throws -> VorleseTon {
        let schluessel = s.zugang.azure.filter { !$0.isWhitespace }
        guard !schluessel.isEmpty else { throw VorleseFehler.abbruch("Es ist kein Azure-Schlüssel hinterlegt.") }
        guard let region = VorleseKatalog.azureRegion(s.zugang.azureRegion) else {
            throw VorleseFehler.abbruch("Die Azure-Region „\(s.zugang.azureRegion.prefix(40))“ verstehe ich nicht (etwa westeurope).")
        }
        let stimme = VorleseKatalog.azureName(stimmeId)
        let hd = VorleseKatalog.istAzureHd(stimmeId)
        let escaped = ssmlEscape(ohneSteuerzeichen(text))
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen.
        let inhalt = s.immerDeutsch && stimme.contains("Multilingual") ? "<lang xml:lang='de-DE'>\(escaped)</lang>" : escaped
        let prozent = Int(((min(max(s.tempo(.azure), 0.7), 1.3) - 1) * 100).rounded())
        // HD-Stimmen bestimmen Betonung und Tempo selbst.
        let mitTempo = (hd || prozent == 0) ? inhalt : "<prosody rate='\(prozent > 0 ? "+" : "")\(prozent)%'>\(inhalt)</prosody>"
        let ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'><voice name='\(stimme)'>\(mitTempo)</voice></speak>"
        var req = URLRequest(url: URL(string: "https://\(region).tts.speech.microsoft.com/cognitiveservices/v1")!, timeoutInterval: 60)
        req.httpMethod = "POST"
        req.setValue(schluessel, forHTTPHeaderField: "Ocp-Apim-Subscription-Key")
        req.setValue("audio-24khz-96kbitrate-mono-mp3", forHTTPHeaderField: "X-Microsoft-OutputFormat")
        req.setValue("Jarvis", forHTTPHeaderField: "User-Agent")
        req.setValue("application/ssml+xml", forHTTPHeaderField: "Content-Type")
        req.httpBody = Data(ssml.utf8)
        // F0 erlaubt rund 20 Anfragen je Minute: Ein 429 ist meist das Ratenlimit – nach Retry-After warten.
        let wartezeiten: [Double] = [3, 10, 20, 30]
        var versuch = 0
        while true {
            try Task.checkCancellation()
            let (daten, antwort): (Data, URLResponse)
            do { (daten, antwort) = try await URLSession.shared.data(for: req) } catch {
                throw VorleseFehler.fehler("Azure nicht erreichbar: \(error.localizedDescription)")
            }
            let http = antwort as? HTTPURLResponse
            let code = http?.statusCode ?? 0
            switch code {
            case 200..<300:
                guard !daten.isEmpty else { throw VorleseFehler.fehler("Azure hat kein Audio geliefert.") }
                return VorleseTon(daten: daten, endung: "mp3")
            case 429 where versuch < wartezeiten.count:
                let sek = (http?.value(forHTTPHeaderField: "Retry-After")).flatMap(Double.init).map { min(max($0, 1), 60) } ?? wartezeiten[versuch]
                try await Task.sleep(for: .seconds(sek)); versuch += 1
            case 401, 403: throw VorleseFehler.abbruch("Azure hat Schlüssel oder Region abgelehnt (Fehler \(code)).")
            case 429: throw VorleseFehler.abbruch("Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.")
            case 400 where hd: throw VorleseFehler.abbruch("Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit Stufe S0 und nicht in jeder Region.")
            default: throw VorleseFehler.fehler("Azure antwortete mit Fehler \(code): \(String(decoding: daten.prefix(200), as: UTF8.self))")
            }
        }
    }

    // MARK: Alibaba Qwen (Standardstimmen und geklonte Stimme)

    static func qwen(_ text: String, geklont: Bool, _ s: VorleseStand) async throws -> VorleseTon {
        let schluessel = s.zugang.qwen.filter { !$0.isWhitespace }
        let stimme = (geklont ? s.stimme(.qwenKlon) : s.stimme(.qwen)).filter { !$0.isWhitespace }
        guard !schluessel.isEmpty else { throw VorleseFehler.abbruch("Der Alibaba-Schlüssel fehlt.") }
        guard !stimme.isEmpty else { throw VorleseFehler.abbruch("Die eigene Stimme ist nicht eingerichtet (Stimmen aus dem Alibaba-Konto laden).") }
        var req = URLRequest(url: URL(string: qwenURL)!, timeoutInterval: 90)
        req.httpMethod = "POST"
        req.setValue("Bearer \(schluessel)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        req.httpBody = JSON.daten([
            "model": geklont ? VorleseKatalog.qwenKlonModell : VorleseKatalog.qwenModell,
            "input": ["text": text, "voice": stimme, "language_type": "German"],
        ] as JSONObjekt)
        let daten = try await fuehreAus(req, name: "Alibaba")
        guard var adresse = ((JSON.objekt(daten)?["output"] as? JSONObjekt)?["audio"] as? JSONObjekt)?["url"] as? String, !adresse.isEmpty else {
            throw VorleseFehler.fehler("Alibaba hat kein Audio geliefert.")
        }
        if adresse.hasPrefix("http://") { adresse = "https://" + adresse.dropFirst(7) }
        guard let url = URL(string: adresse) else { throw VorleseFehler.fehler("Alibaba lieferte eine unlesbare Adresse.") }
        let ton = try await fuehreAus(URLRequest(url: url, timeoutInterval: 60), name: "Alibaba-Download")
        return VorleseTon(daten: ton, endung: "wav")
    }

    // MARK: Microsoft Edge (WebSocket wie EdgeTtsPlayer.kt)

    static let edgeToken = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"  // öffentlicher Edge-Vorlese-Token (wie edge-tts), kein Geheimnis gitleaks:allow
    static let chromiumVoll = "143.0.3650.75"
    static let chromiumHaupt = "143"
    static let edgeURL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
    static let edgeKonfig = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
        #"{"context":{"synthesis":{"audio":{"metadataOptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"#

    static func secMsGec() -> String {
        let jetzt = Int64(Date().timeIntervalSince1970)
        let gerundet = jetzt - (jetzt % 300)
        let ticks = (gerundet + 11_644_473_600) * 10_000_000
        let digest = SHA256.hash(data: Data("\(ticks)\(edgeToken)".utf8))
        return digest.map { String(format: "%02X", $0) }.joined()
    }

    static func edgeSsml(_ text: String, stimme: String, tempo: Double, deutsch: Bool) -> String {
        let anfrageId = UUID().uuidString.replacingOccurrences(of: "-", with: "")
        let escaped = ssmlEscape(text)
        let prozent = Int(((min(max(tempo, 0.7), 1.3) - 1) * 100).rounded())
        let rate = prozent >= 0 ? "+\(prozent)%" : "\(prozent)%"
        // Der <lang>-Rahmen hält mehrsprachige Stimmen bei Deutsch.
        let inhalt = deutsch ? "<lang xml:lang='de-DE'>\(escaped)</lang>" : escaped
        return "X-RequestId:\(anfrageId)\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
            "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'><voice name='\(stimme)'>" +
            "<prosody rate='\(rate)' pitch='+0%'>\(inhalt)</prosody></voice></speak>"
    }

    private final class EdgeZustand: @unchecked Sendable {
        let lock = NSLock()
        var beginn = Date()
        var letzterRahmen: Date?
        var abgelaufen = false
        var daten = Data()
    }

    static func edge(_ text: String, stimme: String, tempo: Double, deutsch: Bool) async throws -> VorleseTon {
        let verbindung = UUID().uuidString.replacingOccurrences(of: "-", with: "")
        let adresse = "\(edgeURL)?TrustedClientToken=\(edgeToken)&Sec-MS-GEC=\(secMsGec())&Sec-MS-GEC-Version=1-\(chromiumVoll)&ConnectionId=\(verbindung)"
        var req = URLRequest(url: URL(string: adresse)!)
        req.setValue("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/\(chromiumHaupt).0.0.0 Safari/537.36 Edg/\(chromiumHaupt).0.0.0",
                     forHTTPHeaderField: "User-Agent")
        req.setValue("chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold", forHTTPHeaderField: "Origin")
        req.setValue("no-cache", forHTTPHeaderField: "Pragma")
        req.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        req.setValue("muid=\(UUID().uuidString.replacingOccurrences(of: "-", with: "").uppercased());", forHTTPHeaderField: "Cookie")
        let socket = URLSession.shared.webSocketTask(with: req)
        let z = EdgeZustand()
        socket.resume()
        // Fristen wie Synthese.kt: erste Antwort, Leerlauf zwischen Rahmen, insgesamt.
        let waechter = Task.detached {
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(500))
                let ab: Bool = z.lock.withLock {
                    let jetzt = Date()
                    let leer = z.letzterRahmen.map { jetzt.timeIntervalSince($0) > 15 } ?? (jetzt.timeIntervalSince(z.beginn) > 20)
                    if leer || jetzt.timeIntervalSince(z.beginn) > 60 { z.abgelaufen = true }
                    return z.abgelaufen
                }
                if ab { socket.cancel(with: .goingAway, reason: nil); return }
            }
        }
        defer { waechter.cancel(); socket.cancel(with: .normalClosure, reason: nil) }
        do {
            try await socket.send(.string(edgeKonfig))
            try await socket.send(.string(edgeSsml(text, stimme: stimme, tempo: tempo, deutsch: deutsch)))
            while true {
                try Task.checkCancellation()
                let nachricht = try await socket.receive()
                switch nachricht {
                case .data(let d):
                    guard d.count > 2 else { continue }
                    let kopf = (Int(d[d.startIndex]) << 8) | Int(d[d.startIndex + 1])
                    let start = kopf + 2
                    guard start < d.count else { continue }
                    z.lock.withLock { z.daten.append(d.subdata(in: (d.startIndex + start)..<d.endIndex)); z.letzterRahmen = Date() }
                case .string(let t):
                    if t.contains("Path:turn.end") {
                        let fertig = z.lock.withLock { z.daten }
                        guard !fertig.isEmpty else { throw VorleseFehler.fehler("Edge hat kein Audio geliefert.") }
                        return VorleseTon(daten: fertig, endung: "mp3")
                    }
                @unknown default: break
                }
            }
        } catch let f as VorleseFehler {
            throw f
        } catch {
            if error is CancellationError { throw error }
            let (ab, d) = z.lock.withLock { (z.abgelaufen, z.daten) }
            // Kam nach Ton ein Zeitlimit, gilt der Absatz als fertig (wie der Edge-Wächter am Handy).
            if ab && !d.isEmpty { return VorleseTon(daten: d, endung: "mp3") }
            if ab { throw VorleseFehler.zeitlimit("Edge hat nicht rechtzeitig geantwortet.") }
            throw VorleseFehler.fehler("Edge-Verbindung fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    // MARK: Ton → abspielbarer Puffer

    /// Dekodiert mp3/wav in einen Puffer im Zielformat (24 kHz mono float); `strecken` ≠ 1 ändert das Tempo
    /// bei gleicher Tonhöhe (für Gemini, das kein Sprechtempo kennt – am Handy per WSOLA).
    static func puffer(_ ton: VorleseTon, format ziel: AVAudioFormat, strecken: Double) -> AVAudioPCMBuffer? {
        let datei = FileManager.default.temporaryDirectory.appendingPathComponent("jarvis-vorlesen-\(UUID().uuidString).\(ton.endung)")
        defer { try? FileManager.default.removeItem(at: datei) }
        guard (try? ton.daten.write(to: datei)) != nil,
              let f = try? AVAudioFile(forReading: datei),
              let quelle = AVAudioPCMBuffer(pcmFormat: f.processingFormat, frameCapacity: AVAudioFrameCount(f.length)),
              (try? f.read(into: quelle)) != nil, quelle.frameLength > 0 else { return nil }
        guard var aus = wandle(quelle, nach: ziel) else { return nil }
        if abs(strecken - 1) >= 0.02, let gestreckt = strecke(aus, rate: strecken) { aus = gestreckt }
        return aus
    }

    private static func wandle(_ q: AVAudioPCMBuffer, nach ziel: AVAudioFormat) -> AVAudioPCMBuffer? {
        if q.format == ziel { return q }
        guard let k = AVAudioConverter(from: q.format, to: ziel) else { return nil }
        let kap = AVAudioFrameCount(Double(q.frameLength) * ziel.sampleRate / q.format.sampleRate) + 4096
        guard let aus = AVAudioPCMBuffer(pcmFormat: ziel, frameCapacity: kap) else { return nil }
        var geliefert = false
        var fehler: NSError?
        k.convert(to: aus, error: &fehler) { _, status in
            if geliefert { status.pointee = .endOfStream; return nil }
            geliefert = true; status.pointee = .haveData; return q
        }
        return fehler == nil && aus.frameLength > 0 ? aus : nil
    }

    /// Tempo ändern bei gleicher Tonhöhe: AVAudioUnitTimePitch im Offline-Rendering (kein Audiogerät nötig).
    private static func strecke(_ ein: AVAudioPCMBuffer, rate: Double) -> AVAudioPCMBuffer? {
        let f = ein.format
        let e = AVAudioEngine()
        let p = AVAudioPlayerNode()
        let tp = AVAudioUnitTimePitch()
        tp.rate = Float(min(max(rate, 0.5), 2))
        e.attach(p); e.attach(tp)
        e.connect(p, to: tp, format: f)
        e.connect(tp, to: e.mainMixerNode, format: f)
        let block: AVAudioFrameCount = 4096
        do { try e.enableManualRenderingMode(.offline, format: f, maximumFrameCount: block); try e.start() } catch { return nil }
        defer { p.stop(); e.stop() }
        p.scheduleBuffer(ein, completionHandler: nil)
        p.play()
        let gesamt = AVAudioFrameCount(Double(ein.frameLength) / Double(tp.rate)) + block
        guard let aus = AVAudioPCMBuffer(pcmFormat: f, frameCapacity: gesamt),
              let stueck = AVAudioPCMBuffer(pcmFormat: e.manualRenderingFormat, frameCapacity: block) else { return nil }
        while aus.frameLength < gesamt {
            let n = min(block, gesamt - aus.frameLength)
            guard let status = try? e.renderOffline(n, to: stueck), status == .success else { break }
            let anzahl = Int(stueck.frameLength)
            guard let q = stueck.floatChannelData?[0], let z = aus.floatChannelData?[0] else { break }
            (z + Int(aus.frameLength)).update(from: q, count: anzahl)
            aus.frameLength += stueck.frameLength
        }
        return aus.frameLength > 0 ? aus : nil
    }
}

enum WavKopf {
    /// Rohes PCM 16 Bit mono mit WAV-Kopf versehen.
    static func mit(pcm: Data, rate: Int) -> Data {
        var k = Data()
        func s(_ t: String) { k.append(contentsOf: Array(t.utf8)) }
        func u32(_ v: Int) { var x = UInt32(v).littleEndian; k.append(Data(bytes: &x, count: 4)) }
        func u16(_ v: Int) { var x = UInt16(v).littleEndian; k.append(Data(bytes: &x, count: 2)) }
        s("RIFF"); u32(36 + pcm.count); s("WAVE"); s("fmt "); u32(16); u16(1); u16(1); u32(rate); u32(rate * 2); u16(2); u16(16)
        s("data"); u32(pcm.count)
        return k + pcm
    }
}

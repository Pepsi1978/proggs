import Foundation

final class GroqWhisperClient {
    private let apiKey: String
    private let endpoint = URL(string: "https://api.groq.com/openai/v1/audio/transcriptions")!
    private let retryableStatusCodes: Set<Int> = [429, 500, 503]
    private let maxRetries = 3
    private let delays: [TimeInterval] = [2, 4, 8]

    // ----- Abwehr gegen Whisper-Stille-Halluzination (Almanach bugs/desktop/groq-transkription.md) -----
    // Schicht 1 (Vorfilter): reine Stille gar nicht erst senden — Whisper halluziniert sonst Floskeln
    // ("Vielen Dank") MIT hoher Confidence, die das Confidence-Gate nicht faengt; ultrakurze Clips
    // liefern zudem oft keine Segmente. Nur ABSOLUTE laute Zeit (keine Ratio), damit echte Aufnahmen
    // mit langen Denkpausen erhalten bleiben.
    private static let speechRmsThreshold = 0.015
    private static let minSpeechMs: Double = 150
    // Schicht 2 (Confidence-Gate). Stille-Regel UND-verknuepft (schuetzt echte leise Sprache).
    private static let noSpeechProbMax = 0.6
    private static let avgLogProbMin = -1.0
    private static let compressionRatioMax = 2.4
    private static let miniNoiseDurSec = 0.4
    // Schicht 3 (Segment-Audio-Abgleich, Almanach §2.3 "zweite Luecke"): Trailing-/Pausen-Halluzination
    // ("Ja" am Ende nach echtem Satz) kommt mit HOHER Confidence -> Schicht 2 faengt sie nicht. Jedes
    // Segment gegen die echte Audio-Lautstaerke pruefen — stilles Zeitfenster = Halluzination.
    private static let frameMs = 20
    private static let segVoicedRatio = 0.10
    // Schicht 4 (Floskel-Blocklist, letzter Filter, Almanach §2.4): bei kurzem Knopfdruck ("nichts gesagt")
    // halluziniert Whisper "Vielen Dank" MIT hoher Confidence (Schicht 2 greift nicht), der Klick liegt oft
    // IM Segment-Fenster oder die Drift-Sicherung haelt das Segment (Schicht 3 greift nicht). GOLDENE REGEL:
    // Floskel NIE allein wegen des Wortlauts verwerfen — nur bei (1) kurz UND (2) exaktem normalisierten
    // Match UND (3) Stille-Kontext (Clip sprach-arm). Bewusst gesprochenes "Vielen Dank" bleibt erhalten.
    private static let floskelMaxWords = 6
    private static let floskelMaxChars = 64
    private static let silenceContextMaxVoicedMs = 600.0
    private static let floskelBlocklist: Set<String> = [
        "vielen dank",
        "vielen dank fürs zuschauen",
        "vielen dank fuers zuschauen",
        "vielen dank für eure aufmerksamkeit",
        "vielen dank für ihre aufmerksamkeit",
        "vielen dank für die aufmerksamkeit",
        "bis zum nächsten mal",
        "bis zum nächsten video",
        "untertitel",
        "untertitel des zdf",
        "untertitelung des zdf für funk",
        "untertitel im auftrag des zdf für funk",
        "untertitel von stephanie geiges",
        "untertitel der amara org community",
        "der text ist nicht auf deutsch",
        "thank you",
        "thank you for watching",
        "thanks for watching",
        "please subscribe",
        "subtitles by the amara org community",
    ]

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    func transcribe(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.sendRequest(fileURL: fileURL, attempt: 0, completion: completion)
        }
    }

    private func sendRequest(fileURL: URL, attempt: Int, completion: @escaping (Result<String, Error>) -> Void) {
        guard let audioData = try? Data(contentsOf: fileURL) else {
            completion(.failure(APIError.fileReadError))
            return
        }

        // Schicht 1: Aufnahme ohne erkennbaren Sprachinhalt gar nicht senden (faengt "Aufnahme
        // gestartet, nichts gesagt"). WICHTIG: .success("") statt .failure — ein .failure wuerde
        // ueber pasteError eine Fehlermeldung ins Terminal schreiben. Leerer Erfolg -> der
        // .success-Leer-Guard im AppDelegate fuegt nichts ein. Nur beim ersten Versuch pruefen.
        if attempt == 0 && !GroqWhisperClient.hasSpeechContent(audioData) {
            completion(.success(""))
            return
        }

        let boundary = UUID().uuidString
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 180

        var body = Data()
        // verbose_json statt text: liefert die Confidence-Felder fuers Halluzinations-Gate,
        // bei Groq ohne Mehrlatenz/-kosten (nur word-Timestamps kosten extra — nicht angefordert).
        let fields: [(String, String)] = [
            ("model", "whisper-large-v3-turbo"),
            ("language", "de"),
            ("response_format", "verbose_json"),
            ("temperature", "0")
        ]
        for (key, value) in fields {
            body.append(Data("--\(boundary)\r\n".utf8))
            body.append(Data("Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n".utf8))
            body.append(Data("\(value)\r\n".utf8))
        }
        // File part
        body.append(Data("--\(boundary)\r\n".utf8))
        body.append(Data("Content-Disposition: form-data; name=\"file\"; filename=\"recording.wav\"\r\n".utf8))
        body.append(Data("Content-Type: audio/wav\r\n\r\n".utf8))
        body.append(audioData)
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))

        request.httpBody = body

        let task = URLSession.shared.dataTask(with: request) { [self] data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0

            if (200...299).contains(statusCode), let data = data {
                let raw = String(data: data, encoding: .utf8) ?? ""
                // Schicht 3: Voiced-Timeline aus dem aufgenommenen PCM bauen (Segment-Audio-Abgleich).
                let voiced = GroqWhisperClient.buildVoicedTimeline(audioData)
                // Schicht 2 (Confidence-Gate) + Schicht 3 (Audio-Abgleich). Bleibt Text uebrig ->
                // .success(text). Alles Stille/halluziniert -> leer -> .success("") (Leer-Guard fuegt nichts ein).
                let filtered = GroqWhisperClient.filterTranscription(raw, voiced)
                completion(.success(filtered))
                return
            }

            if self.retryableStatusCodes.contains(statusCode) && attempt < self.maxRetries {
                let delay = self.delays[attempt]
                NSLog("Groq %d - retry %d/%d, waiting %.0fs...", statusCode, attempt + 1, self.maxRetries, delay)
                DispatchQueue.global(qos: .userInitiated).asyncAfter(deadline: .now() + delay) {
                    self.sendRequest(fileURL: fileURL, attempt: attempt + 1, completion: completion)
                }
                return
            }

            let responseText = data.flatMap { String(data: $0, encoding: .utf8) } ?? "no response"
            completion(.failure(APIError.httpError(statusCode, responseText)))
        }
        task.resume()
    }

    // MARK: - Halluzinations-Abwehr (Almanach bugs/desktop/groq-transkription.md §2.1 / §2.3)

    /// Schicht 2: wendet das Confidence-Gate auf die verbose_json-Antwort an und gibt die
    /// verbleibenden Segment-Texte zusammengefuegt zurueck. Funktionserhaltend: ohne Segment-
    /// Metadaten wird der top-level Text durchgelassen; bei nicht-parsebarem JSON top-level "text".
    private static func filterTranscription(_ json: String, _ voiced: [Bool]?) -> String {
        guard let data = json.data(using: .utf8) else { return "" }
        guard let parsed = try? JSONDecoder().decode(VerboseResponse.self, from: data) else {
            return extractFallbackText(data)
        }
        guard let segments = parsed.segments, !segments.isEmpty else {
            return (parsed.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        }
        // Schicht 2: Confidence-Gate. Halluzinationen mit hoher "keine Sprache"-Wahrscheinlichkeit raus.
        var afterConfidence: [Segment] = []
        var droppedConfidence = 0
        for seg in segments {
            if isHallucination(seg) { droppedConfidence += 1; continue }
            afterConfidence.append(seg)
        }
        // Schicht 3: Audio-Abgleich. Trailing-/Pausen-Halluzination mit HOHER Confidence (die Schicht 2
        // ueberlebt) verwerfen, wenn ihr Zeitfenster im echten Audio still war.
        var kept: [Segment] = []
        var droppedAudio = 0
        if let voiced = voiced {
            for seg in afterConfidence {
                if !segmentHasSpeech(seg, voiced) { droppedAudio += 1; continue }
                kept.append(seg)
            }
        } else {
            kept = afterConfidence
        }
        // Drift-Sicherung (funktionserhaltend): verwirft der Audio-Abgleich ALLE Segmente, obwohl das
        // Confidence-Gate noch welche liess, ist vermutlich das Zeitstempel-Alignment verschoben ->
        // die Confidence-gefilterten behalten statt den Satz zu verlieren.
        var finalSegs = kept
        if kept.isEmpty && !afterConfidence.isEmpty && voiced != nil {
            NSLog("Groq: Audio-Abgleich (Schicht 3) verwarf alle Segmente -> Fallback auf Confidence-gefilterte (Zeitstempel-Drift?).")
            finalSegs = afterConfidence
        }
        var parts: [String] = []
        for seg in finalSegs {
            let t = (seg.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !t.isEmpty { parts.append(t) }
        }
        if droppedConfidence > 0 || droppedAudio > 0 {
            NSLog("Groq: %d Confidence- + %d Audio-Segment(e) verworfen (Stille-Schutz).", droppedConfidence, droppedAudio)
        }
        let result = parts.joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines)
        // Schicht 4: Floskel-Blocklist (letzter Filter). Faengt "Vielen Dank" bei kurzem Knopfdruck,
        // das Schicht 2+3 ueberlebt. Funktionserhaltend: nur bei kurz + exaktem Match + Stille-Kontext.
        if isBlocklistedFloskel(result, voiced) {
            NSLog("Groq: Floskel-Blocklist (Schicht 4) verwarf \"%@\" (kurz + exakter Match + Stille-Kontext).", result)
            return ""
        }
        return result
    }

    /// Schicht 4 (Almanach §2.4): true, wenn der Gesamttext eine Whisper-Outro-Floskel ist. Verwirft NUR bei
    /// drei gleichzeitigen Signalen — (1) kurz, (2) normalisierter EXAKTER Blocklist-Match, (3) Stille-Kontext
    /// (gesamte laute Zeit im Clip < Schwelle). Ohne Voiced-Timeline (voiced==nil) wird NICHT verworfen.
    private static func isBlocklistedFloskel(_ text: String, _ voiced: [Bool]?) -> Bool {
        if text.isEmpty || text.count > floskelMaxChars { return false }
        guard let voiced = voiced else { return false }  // Stille-Kontext nicht messbar -> echte Sprache nie verlieren
        let norm = normalizeFloskel(text)
        if norm.isEmpty { return false }
        if norm.split(separator: " ").count > floskelMaxWords { return false }
        if !floskelBlocklist.contains(norm) { return false }   // (2) exakter Match (== nicht contains)
        // (3) Stille-Kontext: war der ganze Clip sprach-arm? Bewusst gesprochene Floskel hat mehr laute Zeit.
        let voicedMs = Double(voiced.filter { $0 }.count) * Double(frameMs)
        return voicedMs < silenceContextMaxVoicedMs
    }

    /// lowercase, Satzzeichen/Ziffern entfernt (Umlaute bleiben), Whitespace kollabiert.
    private static func normalizeFloskel(_ s: String) -> String {
        let lowered = s.lowercased()
        var chars = ""
        for c in lowered {
            if c.isLetter { chars.append(c) }
            else if c.isWhitespace { chars.append(" ") }
            // Satzzeichen, Ziffern, Symbole weglassen
        }
        return chars.split(separator: " ").joined(separator: " ")
    }

    /// Schicht 3: Voiced-Timeline aus den WAV-Bytes (16-bit mono PCM) — pro 20-ms-Frame true, wenn der
    /// RMS-Pegel ueber der Stille-Schwelle liegt. Liest die Sample-Rate aus dem Header (Bytes 24-27).
    /// Bei jedem Problem nil -> dann kein Audio-Abgleich (funktionserhaltend).
    private static func buildVoicedTimeline(_ wav: Data) -> [Bool]? {
        let bytes = [UInt8](wav)
        let headerSize = 44
        guard bytes.count > headerSize + 4 else { return nil }
        let sampleRate = Int(bytes[24]) | (Int(bytes[25]) << 8) | (Int(bytes[26]) << 16) | (Int(bytes[27]) << 24)
        let rate = sampleRate > 0 ? sampleRate : 16000
        let frameSamples = max(1, rate * frameMs / 1000)
        let frameBytes = frameSamples * 2
        let frameCount = (bytes.count - headerSize) / frameBytes
        guard frameCount > 0 else { return nil }
        var voiced = [Bool](repeating: false, count: frameCount)
        for f in 0..<frameCount {
            let baseB = headerSize + f * frameBytes
            var sumSq = 0.0
            for s in 0..<frameSamples {
                let idx = baseB + s * 2
                let sample = Int16(bitPattern: UInt16(bytes[idx]) | (UInt16(bytes[idx + 1]) << 8))
                let v = Double(sample) / 32768.0
                sumSq += v * v
            }
            voiced[f] = (sumSq / Double(frameSamples)).squareRoot() > speechRmsThreshold
        }
        return voiced
    }

    /// True, wenn das Zeitfenster [start,end] eines Segments genug laute Frames hat (>= segVoicedRatio).
    /// Ungueltige/leere Fenster werden NICHT verworfen (funktionserhaltend).
    private static func segmentHasSpeech(_ seg: Segment, _ voiced: [Bool]) -> Bool {
        let start = seg.start ?? 0
        let end = seg.end ?? 0
        guard end > start else { return true }
        let startF = max(0, Int(start * 1000.0 / Double(frameMs)))
        let endF = min(voiced.count, Int((end * 1000.0 / Double(frameMs)).rounded(.up)))
        guard endF > startF else { return true }
        var v = 0
        for i in startF..<endF where voiced[i] { v += 1 }
        return Double(v) / Double(endF - startF) >= segVoicedRatio
    }

    /// Halluzinations-Heuristik fuer EIN Segment. Stille-Regel UND-verknuepft (schuetzt leise Sprache).
    private static func isHallucination(_ seg: Segment) -> Bool {
        let nsp = seg.noSpeechProb ?? 0
        let alp = seg.avgLogprob ?? 0
        let cr = seg.compressionRatio ?? 0
        if nsp > noSpeechProbMax && alp < avgLogProbMin { return true }   // Stille (UND!)
        if cr > compressionRatioMax { return true }                       // Wiederholungs-Halluzination
        let dur = (seg.end ?? 0) - (seg.start ?? 0)
        if dur > 0 && dur < miniNoiseDurSec && nsp > noSpeechProbMax { return true }   // Mini-Noise
        return false
    }

    /// Fallback bei unerwartetem Format: top-level "text" aus dem JSON ziehen (nie das ganze JSON).
    private static func extractFallbackText(_ data: Data) -> String {
        if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let t = obj["text"] as? String {
            return t.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return ""
    }

    /// Schicht 1: prueft, ob eine 16-bit-PCM-mono-WAV genug echten Sprachinhalt enthaelt. Misst die
    /// aufsummierte LAUTE Zeit in 20-ms-Frames (RMS > Schwelle); reine Stille bleibt darunter. Liest die
    /// Sample-Rate aus dem WAV-Header (Bytes 24-27); nimmt mono 16-bit an (so nimmt der AudioRecorder auf).
    private static func hasSpeechContent(_ wav: Data) -> Bool {
        let bytes = [UInt8](wav)
        let headerSize = 44
        guard bytes.count > headerSize + 4 else { return false }
        let sampleRate = Int(bytes[24]) | (Int(bytes[25]) << 8) | (Int(bytes[26]) << 16) | (Int(bytes[27]) << 24)
        let rate = sampleRate > 0 ? sampleRate : 16000
        let frameSamples = max(1, rate * 20 / 1000)
        let frameBytes = frameSamples * 2
        var voicedMs = 0.0
        var i = headerSize
        while i + frameBytes <= bytes.count {
            var sumSq = 0.0
            for s in 0..<frameSamples {
                let idx = i + s * 2
                let sample = Int16(bitPattern: UInt16(bytes[idx]) | (UInt16(bytes[idx + 1]) << 8))
                let f = Double(sample) / 32768.0
                sumSq += f * f
            }
            let rms = (sumSq / Double(frameSamples)).squareRoot()
            if rms > speechRmsThreshold { voicedMs += 20 }
            i += frameBytes
        }
        NSLog("Groq-Vorfilter: laute Zeit %.0f ms (Schwelle %.0f ms) -> %@",
              voicedMs, minSpeechMs, voicedMs >= minSpeechMs ? "senden" : "verworfen")
        return voicedMs >= minSpeechMs
    }

    // MARK: - verbose_json-DTOs

    private struct VerboseResponse: Decodable {
        let text: String?
        let segments: [Segment]?
    }

    private struct Segment: Decodable {
        let text: String?
        let start: Double?
        let end: Double?
        let noSpeechProb: Double?
        let avgLogprob: Double?
        let compressionRatio: Double?

        enum CodingKeys: String, CodingKey {
            case text, start, end
            case noSpeechProb = "no_speech_prob"
            case avgLogprob = "avg_logprob"
            case compressionRatio = "compression_ratio"
        }
    }

    enum APIError: Error, LocalizedError {
        case fileReadError
        case httpError(Int, String)

        var errorDescription: String? {
            switch self {
            case .fileReadError: return "Audio-Datei konnte nicht gelesen werden"
            case .httpError(let code, let msg): return "Groq API Fehler \(code): \(msg)"
            }
        }
    }

}

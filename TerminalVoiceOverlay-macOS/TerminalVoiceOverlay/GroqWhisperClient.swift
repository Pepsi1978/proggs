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
                // Schicht 2: Confidence-Gate. Bleibt Text uebrig -> .success(text). Alles Stille/
                // halluziniert -> leer -> .success("") (der AppDelegate-Leer-Guard fuegt nichts ein).
                let filtered = GroqWhisperClient.filterTranscription(raw)
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
    private static func filterTranscription(_ json: String) -> String {
        guard let data = json.data(using: .utf8) else { return "" }
        guard let parsed = try? JSONDecoder().decode(VerboseResponse.self, from: data) else {
            return extractFallbackText(data)
        }
        guard let segments = parsed.segments, !segments.isEmpty else {
            return (parsed.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        }
        var parts: [String] = []
        var dropped = 0
        for seg in segments {
            if isHallucination(seg) { dropped += 1; continue }
            let t = (seg.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !t.isEmpty { parts.append(t) }
        }
        if dropped > 0 {
            NSLog("Groq: %d Halluzinations-Segment(e) verworfen (Stille-Schutz).", dropped)
        }
        return parts.joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines)
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

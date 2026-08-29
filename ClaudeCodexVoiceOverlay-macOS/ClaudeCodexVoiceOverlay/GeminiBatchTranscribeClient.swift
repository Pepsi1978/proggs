import Foundation

/// Sprache-zu-Text ueber Gemini 3.5 Transcribe (Modell `gemini-3.5-transcribe`)
/// — der von Google fuer FERTIGE Aufnahmen vorgesehene Weg.
///
/// WARUM DIESER WEG (recherchiert und unter Windows nachgemessen):
///   - Google verweist im Live-Guide ausdruecklich hierher: "Read the Gemini
///     Transcribe documentation for non-streaming audio files." Das Overlay hat
///     beim Transkribieren immer eine fertige Datei.
///   - Genauer: 2,6 % Wortfehlerrate gegenueber 4,0 % bei der Live-Variante
///     (und 4,6 % bei Groq Whisper large-v3-turbo).
///   - Schneller: an derselben 64,5-s-Aufnahme gemessen 4,4 s statt 15,1 s.
///   - Einfacher: ein HTTPS-Aufruf statt WebSocket — damit entfallen
///     Sprechpausen-Erkennung, Aktivitaets-Markierungen und das Zusammensetzen
///     von Teilstuecken, also genau die Stellen, an denen die Live-Variante
///     Text verschluckt hat.
///
/// AUDIO GEHT INLINE, NICHT UEBER DIE FILES-API: beides funktioniert, aber
/// inline spart den zweiten Roundtrip (gemessen 4,4 s statt 5,7 s) und vor
/// allem landen die Diktate so NICHT als Dateien auf Google-Servern, wo sie
/// sonst 48 h liegen blieben.
///
/// VERBATIM ODER SMART: umschaltbar in den Einstellungen, siehe
/// `TranscriptionModeSetting`. Voreinstellung ist verbatim, weil smart zwar
/// Fuellwoerter raeumt und Absaetze setzt, dabei aber umformuliert und WOERTER
/// WEGLAESST — in jedem Messdurchgang wurde aus "Ich frage mich" ein "Frage
/// mich". Schneller ist smart nicht (Median 4,6 s gegen 6,1 s, im Rauschen der
/// Serverlast). Fuellwoerter raeumt ohnehin die Gemini-Textkorrektur weg.
///
/// BEWUSST OHNE HALLUZINATIONS-GATE: die dreischichtige Abwehr gegen
/// Whisper-Stille-Halluzinationen im GroqWhisperClient haengt an Whispers
/// verbose_json-Feldern, die Gemini nicht liefert. Bleibt die Antwort leer,
/// wird `noSpeech` gemeldet, damit nichts getippt wird.
///
/// Portierung von TerminalVoiceOverlay-Windows/Services/GeminiBatchTranscribeClient.cs.
final class GeminiBatchTranscribeClient {

    enum TranscribeError: LocalizedError {
        /// Aufnahme ohne erkennbaren Sprachinhalt. Eigener Fall, damit der
        /// Router NICHT auf Groq ausweicht — sonst wuerde eine stille Aufnahme
        /// zweimal verschickt und am Ende doch eine Floskel getippt.
        case noSpeech
        case quotaExceeded
        case http(status: Int, body: String)

        var errorDescription: String? {
            switch self {
            case .noSpeech:
                return "Aufnahme ohne erkennbaren Sprachinhalt (Gemini gab keinen Text zurueck)"
            case .quotaExceeded:
                return "Gemini-Kontingent erschoepft (429) — das Overlay weicht auf Groq Whisper aus."
            case .http(let status, let body):
                return "Gemini-Transkription Fehler \(status): \(body)"
            }
        }
    }

    private let apiKey: String
    private let model: String
    private let endpoint = URL(string: "https://generativelanguage.googleapis.com/v1beta/interactions")!

    private let session: URLSession = {
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 120
        return URLSession(configuration: cfg)
    }()

    init(apiKey: String, model: String = "gemini-3.5-transcribe") {
        self.apiKey = apiKey
        self.model = model.isEmpty ? "gemini-3.5-transcribe" : model
    }

    func transcribe(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        let started = Date()
        let wav: Data
        do {
            wav = try Data(contentsOf: fileURL)
        } catch {
            completion(.failure(error))
            return
        }

        let vocabulary = PersonalVocabulary.load()
        let mode = TranscriptionModeSetting.current
        DiagLog.write("GeminiBatch", "start",
                      [("model", model), ("wavBytes", wav.count),
                       ("vocabWords", vocabulary.count), ("mode", mode)])

        var transcriptionConfig: [String: Any] = [
            // Verbatim (Standard) = wortgetreu; smart = aufgeraeumt, aber
            // laesst Woerter weg. Umschaltbar in den Einstellungen.
            "mode": ["type": mode]
        ]
        if !vocabulary.isEmpty {
            transcriptionConfig["custom_vocabulary"] = vocabulary
        }

        let payload: [String: Any] = [
            "model": model,
            "input": [[
                "type": "audio",
                "mime_type": "audio/wav",
                "data": wav.base64EncodedString()
            ]],
            "generation_config": ["transcription_config": transcriptionConfig]
        ]

        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "x-goog-api-key")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        } catch {
            completion(.failure(error))
            return
        }

        session.dataTask(with: request) { data, response, error in
            if let error = error {
                DiagLog.warn("GeminiBatch", "transport_error", [("error", error.localizedDescription)])
                completion(.failure(error))
                return
            }
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            let body = data.flatMap { String(data: $0, encoding: .utf8) } ?? ""
            let ms = Int(Date().timeIntervalSince(started) * 1000)
            DiagLog.write("GeminiBatch", "http_response", [("status", status), ("ms", ms)])

            guard status == 200 else {
                DiagLog.warn("GeminiBatch", "http_error",
                             [("status", status), ("body", String(body.prefix(500)))])
                completion(.failure(status == 429
                                    ? TranscribeError.quotaExceeded
                                    : TranscribeError.http(status: status, body: body)))
                return
            }

            let text = Self.extractText(body)
            guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                DiagLog.warn("GeminiBatch", "empty_result")
                completion(.failure(TranscribeError.noSpeech))
                return
            }
            DiagLog.write("GeminiBatch", "done",
                          [("chars", text.count),
                           ("ms", Int(Date().timeIntervalSince(started) * 1000))])
            completion(.success(text))
        }.resume()
    }

    /// Der Text steht in `steps[].content[].text`. Ein `output_text`-Feld gibt
    /// es nur in den SDKs, nicht in der REST-Antwort — dort muss der Baum
    /// selbst durchlaufen werden.
    private static func extractText(_ json: String) -> String {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let steps = root["steps"] as? [[String: Any]] else {
            return ""
        }
        var parts: [String] = []
        for step in steps {
            guard let content = step["content"] as? [[String: Any]] else { continue }
            for part in content {
                if let type = part["type"] as? String, type != "text" { continue }
                if let text = part["text"] as? String { parts.append(text) }
            }
        }
        return parts.joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

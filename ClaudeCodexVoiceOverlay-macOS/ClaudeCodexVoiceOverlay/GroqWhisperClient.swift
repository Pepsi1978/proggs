import Foundation

final class GroqWhisperClient {
    private let apiKey: String
    private let endpoint = URL(string: "https://api.groq.com/openai/v1/audio/transcriptions")!
    private let retryableStatusCodes: Set<Int> = [429, 500, 503]
    private let maxRetries = 3
    private let delays: [TimeInterval] = [2, 4, 8]

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    func transcribe(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.sendRequest(fileURL: fileURL, attempt: 0, sendPrompt: true, completion: completion)
        }
    }

    private func sendRequest(fileURL: URL, attempt: Int, sendPrompt: Bool, completion: @escaping (Result<String, Error>) -> Void) {
        guard let audioData = try? Data(contentsOf: fileURL) else {
            completion(.failure(APIError.fileReadError))
            return
        }

        let boundary = UUID().uuidString
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 180

        var body = Data()
        var fields: [(String, String)] = [
            ("model", "whisper-large-v3"),
            ("language", "de"),
            ("response_format", "text"),
            ("temperature", "0"),
        ]
        // Whisper-Prompt: persoenlicher Vokabular-Hint, hilft Whisper bei
        // englischen Fachbegriffen die sonst eingedeutscht wuerden
        // (commit→Kommit, push→Pusch, branch→Braensch, etc.). Datei wird
        // bei JEDEM Aufruf neu gelesen — Aenderungen wirken sofort.
        // sendPrompt=false beim Fallback-Retry nach Groq-500-Bug, siehe unten.
        var promptWasSent = false
        if sendPrompt, let vocabPrompt = Self.loadVocabularyPrompt() {
            fields.append(("prompt", vocabPrompt))
            promptWasSent = true
        }
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

            if (200...299).contains(statusCode), let data = data,
               let text = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines),
               !text.isEmpty {
                completion(.success(text))
                return
            }

            // Bekannter Groq-Bug: bei manchen Audio-Files loest der prompt-Parameter
            // einen 500-Fehler aus. Workaround: einmal sofort ohne prompt retryen,
            // bevor die normale Retry-Eskalation greift. Quelle:
            // https://community.groq.com/t/500-errors-tied-to-prompt-parameter-on-audio-transcriptions/858
            if statusCode == 500 && promptWasSent {
                NSLog("Groq 500 mit prompt — fallback ohne prompt (vocab-bug Workaround)")
                self.sendRequest(fileURL: fileURL, attempt: attempt, sendPrompt: false, completion: completion)
                return
            }

            if self.retryableStatusCodes.contains(statusCode) && attempt < self.maxRetries {
                let delay = self.delays[attempt]
                NSLog("Groq %d - retry %d/%d, waiting %.0fs...", statusCode, attempt + 1, self.maxRetries, delay)
                DispatchQueue.global(qos: .userInitiated).asyncAfter(deadline: .now() + delay) {
                    self.sendRequest(fileURL: fileURL, attempt: attempt + 1, sendPrompt: sendPrompt, completion: completion)
                }
                return
            }

            let responseText = data.flatMap { String(data: $0, encoding: .utf8) } ?? "no response"
            completion(.failure(APIError.httpError(statusCode, responseText)))
        }
        task.resume()
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

    /// Pfad zur persoenlichen Whisper-Vocabulary-Datei. Wird bei JEDEM
    /// Transkriptions-Aufruf neu gelesen — Aenderungen wirken sofort
    /// ohne App-Neustart. Fehlt die Datei: prompt-Field wird einfach
    /// weggelassen (kein Fehler).
    private static var vocabularyFilePath: URL {
        let home = FileManager.default.homeDirectoryForCurrentUser
        return home.appendingPathComponent("SK/VoiceOverlays/voice-prompt.txt")
    }

    private static func loadVocabularyPrompt() -> String? {
        let url = vocabularyFilePath
        guard FileManager.default.fileExists(atPath: url.path) else { return nil }
        guard let text = try? String(contentsOf: url, encoding: .utf8) else { return nil }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

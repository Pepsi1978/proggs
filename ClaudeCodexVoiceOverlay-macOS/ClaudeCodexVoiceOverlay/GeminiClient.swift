import Foundation

final class GeminiClient {
    private let apiKey: String
    private let model = "gemini-3.1-flash-lite-preview"
    private let retryableStatusCodes: Set<Int> = [429, 500, 503]
    private let maxRetries = 5
    private let delays: [TimeInterval] = [2, 4, 8, 16, 32]

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    /// Pfad zur externen Gemini-Korrektur-Prompt-Datei. Wird bei JEDEM
    /// correctText-Aufruf neu gelesen — Aenderungen wirken sofort ohne
    /// App-Neustart. Fehlt die Datei: Fallback auf eingebauten Default-Prompt.
    private static var geminiPromptFilePath: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("SK/VoiceOverlays/gemini-correction-prompt.txt")
    }

    private static func loadCorrectionPrompt() -> String? {
        let url = geminiPromptFilePath
        guard FileManager.default.fileExists(atPath: url.path) else { return nil }
        guard let text = try? String(contentsOf: url, encoding: .utf8) else { return nil }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    func correctText(_ text: String, completion: @escaping (Result<String, Error>) -> Void) {
        // Externe Korrektur-Prompt-Datei hat Vorrang — Frank kann sie
        // jederzeit pflegen ohne Rebuild. Fallback: eingebauter Default.
        let template = Self.loadCorrectionPrompt() ?? GeminiClient.defaultCorrectionTemplate
        let prompt = template + "\n" + text + "\nTEXT_END"

        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.sendRequest(prompt: prompt, attempt: 0, completion: completion)
        }
    }

    private static let defaultCorrectionTemplate = """
    ROLLE:
    Du bist ein technischer Redakteur, spezialisiert auf die Aufbereitung von Spracheingaben für KI-Coding-Tools. Du verstehst Programmierkonzepte und bewahrst technische Präzision, während du gesprochene Sprache in klare schriftliche Anweisungen umwandelst.

    AUFGABE:
    Du erhältst einen diktierten Text (Speech-to-Text). Der Sprecher spricht Deutsch, verwendet aber regelmäßig englische Fachbegriffe aus der Programmierung. Die Spracherkennung kann diese englischen Begriffe falsch transkribieren – erkenne und korrigiere solche Fehler anhand des technischen Kontexts.

    VORGEHEN:
    1) Entferne Diktat-Artefakte: Fülllaute, Stotterer, Wortwiederholungen.
    2) Erkenne und korrigiere falsch transkribierte englische Fachbegriffe.
    3) Erkenne die Absicht und formuliere den Text als klare Anweisung um.
    4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.
    5) Bewahre technische Begriffe EXAKT.

    AUSGABEFORMAT:
    Ausschließlich den überarbeiteten Text. Keine Kommentare.

    TEXT_START
    """

    private func sendRequest(prompt: String, attempt: Int, completion: @escaping (Result<String, Error>) -> Void) {
        var urlComponents = URLComponents(string: "https://generativelanguage.googleapis.com/v1beta/models/\(model):generateContent")!
        urlComponents.queryItems = [URLQueryItem(name: "key", value: apiKey)]

        var request = URLRequest(url: urlComponents.url!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 120

        let payload: [String: Any] = [
            "contents": [
                ["role": "user", "parts": [["text": prompt]]]
            ],
            "generationConfig": [
                "maxOutputTokens": 8192,
                "thinkingConfig": ["thinkingLevel": "MEDIUM"]
            ] as [String: Any]
        ]

        request.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        let task = URLSession.shared.dataTask(with: request) { [self] data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0

            if !(200...299).contains(statusCode) {
                if self.retryableStatusCodes.contains(statusCode) && attempt < self.maxRetries {
                    let delay = self.delays[attempt]
                    NSLog("Gemini %d - retry %d/%d, waiting %.0fs...", statusCode, attempt + 1, self.maxRetries, delay)
                    DispatchQueue.global(qos: .userInitiated).asyncAfter(deadline: .now() + delay) {
                        self.sendRequest(prompt: prompt, attempt: attempt + 1, completion: completion)
                    }
                    return
                }
                let responseText = data.flatMap { String(data: $0, encoding: .utf8) } ?? ""
                completion(.failure(APIError.httpError(statusCode, responseText)))
                return
            }

            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }

            do {
                let text = try self.extractText(from: data)
                completion(.success(text))
            } catch {
                completion(.failure(error))
            }
        }
        task.resume()
    }

    private func extractText(from data: Data) throws -> String {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let candidates = json["candidates"] as? [[String: Any]],
              let content = candidates.first?["content"] as? [String: Any],
              let parts = content["parts"] as? [[String: Any]] else {
            throw APIError.unexpectedResponse
        }

        // Skip thinking parts (thought: true), use only text parts
        for part in parts {
            if let isThought = part["thought"] as? Bool, isThought { continue }
            if let text = part["text"] as? String, !text.isEmpty {
                return text.trimmingCharacters(in: .whitespacesAndNewlines)
            }
        }
        throw APIError.noTextInResponse
    }

    enum APIError: Error, LocalizedError {
        case httpError(Int, String)
        case noData
        case unexpectedResponse
        case noTextInResponse

        var errorDescription: String? {
            switch self {
            case .httpError(let code, let msg): return "Gemini API Fehler \(code): \(msg)"
            case .noData: return "Keine Daten von Gemini erhalten"
            case .unexpectedResponse: return "Unerwartete Gemini-Antwort"
            case .noTextInResponse: return "Kein Text in Gemini-Antwort"
            }
        }
    }
}

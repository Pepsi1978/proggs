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

    func correctText(_ text: String, completion: @escaping (Result<String, Error>) -> Void) {
        let prompt = """
        Du bist ein deutscher Textredakteur für diktierte Spracheingaben.

        AUFGABE:
        Du erhältst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, präzise und sprachlich hochwertig** zum Ausdruck kommt.

        VORGEHEN (in dieser Reihenfolge):
        1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdrücken?
        2) Entferne Diktat-Artefakte: Fülllaute ("äh", "ähm", "öhm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
        3) Formuliere Sätze so um, dass die erkannte Intention **klar und gut lesbar** wird.
           - Sätze dürfen umstrukturiert werden.
           - Wortwahl darf verbessert werden.
           - Satzgrenzen dürfen neu gesetzt werden.
        4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.

        GRENZEN (strikt):
        - Keine neuen Informationen, Fakten oder Inhalte hinzufügen.
        - Keine Vermutungen über nicht Gesagtes.
        - Die Intention des Originals muss vollständig erhalten bleiben.
        - Sprache: Deutsch.

        REGEL:
        Gib AUSSCHLIESSLICH den überarbeiteten Text zurück.
        Keine Kommentare. Keine Erklärungen. Kein Präfix.

        TEXT:
        \(text)
        """

        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.sendRequest(prompt: prompt, attempt: 0, completion: completion)
        }
    }

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
                    NSLog("Gemini %d - Versuch %d/%d, warte %.0fs...", statusCode, attempt + 1, self.maxRetries, delay)
                    Thread.sleep(forTimeInterval: delay)
                    self.sendRequest(prompt: prompt, attempt: attempt + 1, completion: completion)
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

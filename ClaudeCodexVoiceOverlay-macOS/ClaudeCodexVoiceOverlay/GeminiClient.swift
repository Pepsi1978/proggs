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
        ROLLE:
        Du bist ein technischer Redakteur, spezialisiert auf die Aufbereitung von Spracheingaben für KI-Coding-Tools. Du verstehst Programmierkonzepte und bewahrst technische Präzision, während du gesprochene Sprache in klare schriftliche Anweisungen umwandelst.

        AUFGABE:
        Du erhältst einen diktierten Text (Speech-to-Text). Der Sprecher spricht Deutsch, verwendet aber regelmäßig englische Fachbegriffe aus der Programmierung (Funktionsnamen, Frameworks, CLI-Befehle etc.). Die Spracherkennung kann diese englischen Begriffe falsch transkribieren – erkenne und korrigiere solche Fehler anhand des technischen Kontexts. Der Sprecher gibt Programmier-Anweisungen an ein KI-Coding-Tool (z.B. Claude Code). Bereite den Text so auf, dass er als klare, präzise Eingabe funktioniert.

        VORGEHEN:
        1) Entferne Diktat-Artefakte: Fülllaute ("äh", "ähm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
        2) Erkenne und korrigiere falsch transkribierte englische Fachbegriffe (z.B. "use Tate" → "useState", "Fötch" → "fetch").
        3) Erkenne die Absicht und formuliere den Text als klare Anweisung um. Sätze dürfen umstrukturiert, Wortwahl verbessert und Satzgrenzen neu gesetzt werden. Zusammengehörige Anweisungen als einen Befehl belassen.
        4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.
        5) Bewahre technische Begriffe EXAKT: Dateinamen, Funktionsnamen, Variablen, Programmiersprachen, Frameworks, CLI-Befehle, API-Namen – NICHT übersetzen oder verändern.

        GRENZEN:
        - Keine neuen Informationen oder Vermutungen hinzufügen.
        - Intention des Originals vollständig erhalten.
        - Englische Fachbegriffe und Code-Referenzen NIEMALS eindeutschen.
        - Sprache: Deutsch (außer technische Begriffe).

        AUSGABEFORMAT:
        - Ausschließlich den überarbeiteten Text. Keine Kommentare, keine Erklärungen, kein Präfix.
        - Ausführliche, vollständige Sätze, sodass jede Intention des Sprechers klar und unmissverständlich beim Leser ankommt.
        - Natürlicher, verständlicher Ton – so wie man einem Kollegen etwas erklärt. Kein Behördendeutsch, keine Geschäftsbrief-Floskeln, keine gestelzte Sprache.

        TEXT_START
        \(text)
        TEXT_END
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

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

    /// Wandelt einen roh-transkribierten Whisper-Text in einen
    /// kopierfertigen Claude-Code-CLI-Prompt um. Wird vom PromptBoard-
    /// Edit-Dialog beim Klick auf "G" verwendet — getrennt vom Overlay-
    /// Diktat-Cleanup, damit beide Pfade unabhaengig optimierbar bleiben.
    func buildClaudeCodePrompt(_ rawWhisperText: String, completion: @escaping (Result<String, Error>) -> Void) {
        let prompt = """
        Du bist ein international anerkannter Prompt-Engineer mit Spezialisierung auf Programmier-Prompts für Claude Code in der CLI-Umgebung. Deine einzige Aufgabe ist es, einen per Whisper eingesprochenen, roh transkribierten Text in einen präzisen, sofort im Claude-Code-CLI-Fenster einsetzbaren Prompt umzuwandeln.

        Vorgehen:

        1. Erfasse die programmiertechnische Absicht des Textes: Was soll gebaut, geändert, debuggt oder refaktoriert werden? Welche Dateien, Sprachen, Frameworks, Pfade, Tools sind gemeint?

        2. Korrigiere Whisper-Transkriptionsfehler nicht nur nach allgemeiner Sprachlogik, sondern explizit nach Programmier-Logik. Wenn ein Wort im Code-Kontext sinnlos wirkt, prüfe phonetisch, ob ein Programmier-Begriff gemeint ist. Typische Whisper-Verwechslungen, an denen du dich orientierst:
           - "Brunch" → branch
           - "Mörsch" / "März" → merge
           - "Komitee" → commit
           - "Reposi-Tory" / "Repo-Story" → repository / repo
           - "Busch" → push
           - "Pull-Anforderung" / "Pullrikwest" → pull request
           - "A-Sync" / "Async-hron" → async
           - "Funkschon" → function
           - "Wert" / "Lett" → var / let / const
           - "Klosure" → closure
           - "Daiwa" / "Daiver" → diff
           - "Cäsch" → cache
           - "Kju" → queue
           - "Schell" → shell
           - "Endpoint" / "End-Punkt" → endpoint
           - Tool-/Library-Namen wie npm, pnpm, Vite, Docker, Tampermonkey, ESLint, Prettier, Jest, Vitest, FastAPI, Tailwind etc. phonetisch erkennen.
           Bei mehrdeutigen Stellen wähle die im Kontext plausibelste Programmier-Bedeutung.

        3. Strukturiere den fertigen Prompt programmierfreundlich für Claude Code:
           - Klare Aufgabe im Imperativ ("Implementiere…", "Refaktoriere…", "Debugge…", "Erstelle…")
           - Sofern erkennbar: Dateipfade, Funktions-/Modulnamen, Sprache, Framework, Versionen
           - Akzeptanzkriterien: Was muss am Ende funktionieren oder getestet sein?
           - Wenn sinnvoll: Hinweis auf bestehende Konventionen / Code-Stil beibehalten
           - Wenn die Aufgabe komplex ist: explizit "Erstelle erst einen Plan, dann implementiere" oder "Schreibe Tests" ergänzen
           - Edge-Cases nennen, wenn sie aus dem Original ableitbar sind

        4. Sprache des Output-Prompts: Deutsch. Technische Fachbegriffe (function, branch, commit, async, hook, endpoint, …) bleiben Englisch.

        5. Länge: so kurz wie möglich, so lang wie nötig. Keine Floskeln, kein Vorgeplänkel, keine Meta-Kommentare über deine Arbeit.

        Output-Format (exakt so):

        [Der fertige, kopierfertige Claude-Code-CLI-Prompt – direkt loslegen, kein "Hier ist…"]

        Annahmen (nur falls vorhanden, sonst diesen Block komplett weglassen):
        - [phonetisch oder inhaltlich getroffene Annahme 1]
        - [phonetisch oder inhaltlich getroffene Annahme 2]

        Gib ausschließlich den umgewandelten Prompt (plus optional den Annahmen-Block) zurück. Keine Einleitung, keine Bestätigung, keine Erklärung deiner Arbeit.

        Der zu verarbeitende Whisper-Text folgt nun:
        \(rawWhisperText)
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
        let bodySize = request.httpBody?.count ?? 0
        let attemptStart = Date()
        tvoDebug("[Gemini] POST attempt=\(attempt) model=\(self.model) prompt=\(prompt.count)c body=\(bodySize)b")

        let task = URLSession.shared.dataTask(with: request) { [self] data, response, error in
            let elapsed = Date().timeIntervalSince(attemptStart)
            if let error = error {
                tvoDebug("[Gemini] network error after \(String(format: "%.1f", elapsed))s: \(error.localizedDescription)")
                completion(.failure(error))
                return
            }

            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
            let bytes = data?.count ?? 0
            tvoDebug("[Gemini] response status=\(statusCode) bytes=\(bytes) after \(String(format: "%.1f", elapsed))s")

            if !(200...299).contains(statusCode) {
                if self.retryableStatusCodes.contains(statusCode) && attempt < self.maxRetries {
                    let delay = self.delays[attempt]
                    NSLog("Gemini %d - retry %d/%d, waiting %.0fs...", statusCode, attempt + 1, self.maxRetries, delay)
                    tvoDebug("[Gemini] retry \(attempt+1)/\(self.maxRetries) in \(delay)s")
                    DispatchQueue.global(qos: .userInitiated).asyncAfter(deadline: .now() + delay) {
                        self.sendRequest(prompt: prompt, attempt: attempt + 1, completion: completion)
                    }
                    return
                }
                let responseText = data.flatMap { String(data: $0, encoding: .utf8) } ?? ""
                tvoDebug("[Gemini] HTTP \(statusCode) — body[0..400]=\(responseText.prefix(400))")
                completion(.failure(APIError.httpError(statusCode, responseText)))
                return
            }

            guard let data = data else {
                tvoDebug("[Gemini] success status but no data")
                completion(.failure(APIError.noData))
                return
            }

            do {
                let text = try self.extractText(from: data)
                tvoDebug("[Gemini] extracted \(text.count) chars of usable text")
                completion(.success(text))
            } catch {
                let preview = String(data: data, encoding: .utf8)?.prefix(600) ?? ""
                tvoDebug("[Gemini] extract failed: \(error.localizedDescription) — body[0..600]=\(preview)")
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

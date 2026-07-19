import Foundation

final class GeminiClient {
    private let apiKey: String
    private let model = "gemini-3.1-flash-lite"
    private let retryableStatusCodes: Set<Int> = [429, 500, 503]
    private let maxRetries = 5
    private let delays: [TimeInterval] = [2, 4, 8, 16, 32]

    init(apiKey: String) {
        self.apiKey = apiKey
    }

    /// Basis-Verzeichnis fuer alle Gemini-Korrektur-Prompts. Wird bei JEDEM
    /// correctText-Aufruf neu gelesen — Aenderungen wirken sofort ohne
    /// App-Neustart.
    private static var geminiPromptDir: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("SK/VoiceOverlays")
    }

    /// Mappt eine Profil-Nummer auf den Dateinamen der zugehoerigen
    /// Prompt-Datei. Profile 1-3 haben semantische Namen (Standard,
    /// Programmierung, Meta), Profile 4-10 haben numerische Slots, die
    /// der Benutzer spaeter mit eigenen Prompt-Dateien fuellen kann.
    /// Falls die profilspezifische Datei fehlt, faellt der Aufrufer auf
    /// die alte Sammeldatei zurueck (Backward Compat) und am Ende auf
    /// den eingebauten defaultCorrectionTemplate.
    private static func profilePromptFileName(_ profile: Int) -> String {
        switch profile {
        case 1: return "gemini-correction-prompt-standard.txt"
        case 2: return "gemini-correction-prompt-programmierung.txt"
        case 3: return "gemini-correction-prompt-meta.txt"
        case 4...10: return String(format: "gemini-correction-prompt-%02d.txt", profile)
        default: return "gemini-correction-prompt-standard.txt"
        }
    }

    private static func loadCorrectionPrompt(profile: Int) -> String? {
        // 1) Profil-spezifische Datei
        let profileURL = geminiPromptDir.appendingPathComponent(profilePromptFileName(profile))
        if FileManager.default.fileExists(atPath: profileURL.path),
           let text = try? String(contentsOf: profileURL, encoding: .utf8) {
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                return isLegacyMinimalInterventionPrompt(profile: profile, text: trimmed)
                    ? defaultCorrectionPrompt(profile: profile)
                    : trimmed
            }
        }

        // 2) Legacy-Fallback: alte Sammeldatei (vor Profil-Trennung)
        let legacyURL = geminiPromptDir.appendingPathComponent("gemini-correction-prompt.txt")
        if FileManager.default.fileExists(atPath: legacyURL.path),
           let text = try? String(contentsOf: legacyURL, encoding: .utf8) {
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { return trimmed }
        }

        return nil
    }

    // ── Public API fuer den Prompt-Editor (Etappe 2c, Frank-Wunsch 2026-06-22) ──

    /// Anzeige-Name eines Profils fuer die Editor-Liste.
    static func profileLabel(_ profile: Int) -> String {
        switch profile {
        case 1: return "Profil 1 — Standard (alltaegliche Texte)"
        case 2: return "Profil 2 — Programmierung (Code, CLI, Frameworks)"
        case 3: return "Profil 3 — Meta-Intelligenz (strukturiertes Denken)"
        default: return "Profil \(profile) — Slot \(profile)"
        }
    }

    /// Voller Pfad der Prompt-Datei eines Profils (zum Speichern aus dem Editor).
    static func profilePromptURL(_ profile: Int) -> URL {
        geminiPromptDir.appendingPathComponent(profilePromptFileName(profile))
    }

    /// Die aktuell WIRKSAME Vorlage eines Profils: profilspezifische Datei,
    /// sonst Legacy-Sammeldatei, sonst eingebauter Default. So sieht der Editor
    /// immer genau das, was Gemini fuer dieses Profil gerade bekommt.
    static func effectivePrompt(profile: Int) -> String {
        loadCorrectionPrompt(profile: profile) ?? defaultCorrectionPrompt(profile: profile)
    }

    /// Speichert die bearbeitete Vorlage eines Profils in seine SK-Datei.
    /// Aenderung wirkt sofort (correctText liest die Datei neu).
    static func saveProfilePrompt(profile: Int, text: String) {
        let url = profilePromptURL(profile)
        try? FileManager.default.createDirectory(at: geminiPromptDir, withIntermediateDirectories: true)
        try? text.write(to: url, atomically: true, encoding: .utf8)
    }

    static func upgradeLegacyMinimalInterventionPrompts() -> Bool {
        var changed = false
        for profile in 1...2 {
            let url = profilePromptURL(profile)
            guard let text = try? String(contentsOf: url, encoding: .utf8),
                  isLegacyMinimalInterventionPrompt(profile: profile, text: text) else { continue }
            saveProfilePrompt(profile: profile, text: defaultCorrectionPrompt(profile: profile))
            changed = true
        }
        return changed
    }

    /// Dateiname der persoenlichen Vokabular-Liste. Liegt im selben SK-Ordner
    /// wie die Korrektur-Prompts, sodass sich auf EINEM Rechner beide Overlays
    /// automatisch dieselbe Liste teilen. Wird bei jedem correctText neu
    /// gelesen — Aenderungen wirken sofort ohne App-Neustart.
    private static let personalVocabularyFileName = "personal-vocabulary.txt"

    /// Woerterbuch-Schalter (Frank-Wunsch 2026-06-22): geteilte SK-Datei.
    /// Inhalt "true" = Woerterbuch wird mitgeschickt; sonst (auch fehlend) aus.
    private static let vocabularyEnabledFileName = "vocabulary-enabled.txt"

    /// Editierbarer Einleitungstext fuer den Woerterbuch-Block (Frank-Wunsch
    /// 2026-06-22): Datei vocabulary-preamble.txt, sonst Default. Wird vom
    /// Backup-Sync mitgenommen; auf Windows editierbar.
    private static let vocabularyPreambleFileName = "vocabulary-preamble.txt"
    static let defaultVocabularyPreamble =
        "PERSÖNLICHES VOKABULAR (Begriffe, die die Spracherkennung oft falsch schreibt):\n"
        + "Die folgenden Wörter/Eigennamen benutzt der Sprecher regelmäßig. Die Groq/Whisper-"
        + "Spracherkennung transkribiert sie oft falsch, weil sie nur ÄHNLICH KLINGEN. Wenn ein "
        + "transkribiertes Wort phonetisch einem dieser Begriffe ähnelt UND es im Satzkontext "
        + "Sinn ergibt, ersetze es durch die hier angegebene korrekte Schreibweise. Erzwinge die "
        + "Ersetzung NICHT, wenn der Kontext eindeutig eine andere, normale Bedeutung hat "
        + "(z.B. bleibt \"Backen\" beim Thema Kuchen \"Backen\", wird aber im Programmier-"
        + "Kontext zu \"Backend\")."

    static func effectiveVocabularyPreamble() -> String {
        let url = geminiPromptDir.appendingPathComponent(vocabularyPreambleFileName)
        if let text = try? String(contentsOf: url, encoding: .utf8) {
            let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
            if !t.isEmpty { return t }
        }
        return defaultVocabularyPreamble
    }

    /// Speichert den editierbaren Woerterbuch-Einleitungstext (Praeambel) in
    /// vocabulary-preamble.txt (wirkt sofort, wird ueber Drive gesynct). Spiegelt
    /// Windows SaveVocabularyPreamble; effectiveVocabularyPreamble() faellt bei
    /// leerem/fehlendem Inhalt auf defaultVocabularyPreamble zurueck.
    static func saveVocabularyPreamble(_ text: String) {
        let url = geminiPromptDir.appendingPathComponent(vocabularyPreambleFileName)
        do {
            try FileManager.default.createDirectory(at: geminiPromptDir, withIntermediateDirectories: true)
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            try (trimmed + "\n").write(to: url, atomically: true, encoding: .utf8)
        } catch {
            NSLog("Vokabular-Praeambel konnte nicht gespeichert werden: \(error.localizedDescription)")
        }
    }

    /// Laedt die persoenliche Vokabular-Liste (haeufige Begriffe/Eigennamen des
    /// Sprechers) und baut daraus einen Praeambel-Block, der VOR den Korrektur-
    /// Prompt gesetzt wird. Bewusst kontextsensitiv: KEIN stures Suchen-und-
    /// Ersetzen — Gemini bringt einen Begriff nur dann in die korrekte
    /// Schreibweise, wenn er phonetisch passt UND der Satzkontext es hergibt
    /// ("Backen" bleibt "Backen", wird nur im Code-Kontext zu "Backend").
    /// Leere/fehlende Datei → leerer String (= bisheriges Verhalten).
    private static func loadPersonalVocabularyBlock() -> String {
        // Woerterbuch-Schalter zuerst: nur laden wenn aktiviert ("true").
        // Fehlende Datei / anderer Inhalt = aus (Frank-Wunsch 2026-06-22, Default aus).
        let toggleURL = geminiPromptDir.appendingPathComponent(vocabularyEnabledFileName)
        let toggle = (try? String(contentsOf: toggleURL, encoding: .utf8))?
            .trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard toggle == "true" else { return "" }

        let url = geminiPromptDir.appendingPathComponent(personalVocabularyFileName)
        guard FileManager.default.fileExists(atPath: url.path),
              let text = try? String(contentsOf: url, encoding: .utf8) else {
            return ""
        }
        let vocab = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if vocab.isEmpty { return "" }
        // Einleitungstext (Praeambel) aus der editierbaren Datei, sonst Default.
        return effectiveVocabularyPreamble() + "\n" + vocab + "\n\n"
    }

    func correctText(_ text: String, profile: Int = 1,
                     completion: @escaping (Result<String, Error>) -> Void) {
        // Profil-spezifische Korrektur-Prompt-Datei hat Vorrang — Frank kann
        // sie jederzeit pflegen ohne Rebuild. Fallbacks: Legacy-Sammeldatei,
        // dann eingebauter Default-Template.
        let template = Self.loadCorrectionPrompt(profile: profile)
            ?? GeminiClient.defaultCorrectionPrompt(profile: profile)
        // Persoenliches Vokabular als Praeambel VORANSTELLEN (Gemini-BP §7:
        // wiederkehrende Inhalte an den Prompt-Anfang → bessere Cache-Trefferquote).
        let vocab = Self.loadPersonalVocabularyBlock()
        let prompt = Self.buildPrompt(template: template, vocab: vocab, text: text) + "\nTEXT_END"

        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.sendRequest(prompt: prompt, attempt: 0, completion: completion)
        }
    }

    /// Baut den finalen Prompt aus Vorlage + Woerterbuch-Block + gesprochenem Text.
    /// {{TEXT}} und {{WOERTERBUCH}} frei positionierbar (Frank-Wunsch 2026-06-22).
    /// Abwaertskompatibel: fehlt {{WOERTERBUCH}} -> Block an den Anfang (wie bisher);
    /// fehlt {{TEXT}} -> Text ans Ende mit fuehrendem \n (wie bisher). vocab ist
    /// bereits leer, wenn der Woerterbuch-Schalter aus ist.
    private static func buildPrompt(template: String, vocab: String, text: String) -> String {
        let vocabMarker = "{{WOERTERBUCH}}"
        let textMarker = "{{TEXT}}"
        var result = template
        result = result.contains(vocabMarker)
            ? result.replacingOccurrences(of: vocabMarker, with: vocab)
            : vocab + result
        result = result.contains(textMarker)
            ? result.replacingOccurrences(of: textMarker, with: text)
            : result + "\n" + text
        return result
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

    private static let standardImprovementPrompt = """
    ROLLE:
    Du bist ein professioneller deutscher Textredakteur für Spracheingaben.

    AUFGABE:
    Überarbeite das Whisper-Transkript zu einem klaren, flüssigen und präzisen Text. Die Bedeutung und alle Informationen des Originals müssen vollständig erhalten bleiben.

    VERBINDLICHES VORGEHEN:
    1) Entferne Fülllaute, Stotterer, Wortwiederholungen und abgebrochene Satzanfänge.
    2) Korrigiere erkennbare Transkriptionsfehler, Grammatik, Zeichensetzung sowie Groß- und Kleinschreibung.
    3) Formuliere umständliche oder unklare gesprochene Sätze als natürliches, gut verständliches Schriftdeutsch.
    4) Ordne zusammengehörige Aussagen sinnvoll, ohne neue Inhalte, Vermutungen oder Anforderungen hinzuzufügen.
    5) Bewahre Namen, Zahlen, Fachbegriffe und die Absicht des Sprechers exakt.

    AUSGABE:
    Nur den vollständig überarbeiteten Text ausgeben. Keine Erklärung, kein Präfix und keine Anführungszeichen.

    TEXT_START
    """

    private static let programmingImprovementPrompt = """
    ROLLE:
    Du bist ein technischer Redakteur für deutschsprachige Programmier-Diktate und KI-Coding-Prompts.

    AUFGABE:
    Überarbeite das Whisper-Transkript zu einer klaren, präzisen und direkt ausführbaren Programmier-Anweisung. Die technische Absicht und alle genannten Details müssen vollständig erhalten bleiben.

    VERBINDLICHES VORGEHEN:
    1) Entferne Fülllaute, Stotterer, Wiederholungen und irrelevante Diktat-Artefakte.
    2) Korrigiere phonetisch falsch erkannte englische Fachbegriffe, Tool-Namen, APIs, Dateinamen und CLI-Befehle anhand des Kontexts.
    3) Korrigiere Grammatik und Zeichensetzung und strukturiere gesprochene Satzketten zu klaren Arbeitsaufträgen.
    4) Formuliere Ziel, erwartetes Verhalten und erkennbare Akzeptanzkriterien präzise, ohne neue Anforderungen zu erfinden.
    5) Bewahre Code, Pfade, Variablen, Versionsnummern und technische Eigennamen exakt und übersetze sie nicht.

    AUSGABE:
    Nur den vollständig überarbeiteten Text ausgeben. Keine Erklärung, kein Präfix und keine Anführungszeichen.

    TEXT_START
    """

    private static func defaultCorrectionPrompt(profile: Int) -> String {
        switch profile {
        case 1: return standardImprovementPrompt
        case 2: return programmingImprovementPrompt
        default: return defaultCorrectionTemplate
        }
    }

    private static func isLegacyMinimalInterventionPrompt(profile: Int, text: String) -> Bool {
        guard profile == 1 || profile == 2,
              text.contains("WICHTIGSTE REGEL — MINIMAL-INTERVENTION") else { return false }
        return text.contains(profile == 1
            ? "Spracherkennungs-Korrektor für alltägliche deutsche Diktate"
            : "Spracherkennungs-Korrektor für Programmier-Diktate")
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

    /// Erzeugt einen kompakten Titel von maximal 4 Woertern auf Deutsch
    /// fuer einen gegebenen Prompt. Fuer die Prompt-Historie — der Benutzer
    /// soll Eintraege auf einen Blick wiedererkennen. Fallback auf die
    /// ersten 4 Woerter des Texts wenn Gemini fehlschlaegt; die Historie
    /// blockiert NIE wegen einer fehlgeschlagenen Titel-Generierung.
    func generateTitle(_ text: String, completion: @escaping (String) -> Void) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { completion(""); return }

        let prompt = """
        Erstelle einen sehr kompakten Titel auf Deutsch fuer den folgenden \
        Prompt. STRENGE REGELN: Maximal 4 Woerter. Keine Anfuehrungszeichen. \
        Kein Punkt am Ende. Keine Aufzaehlungen, kein Praefix wie 'Titel:'. \
        Nur die nackten 1-4 Woerter zurueckgeben.

        PROMPT:
        \(trimmed)
        """

        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.sendRequest(prompt: prompt, attempt: 0) { result in
                switch result {
                case .success(let raw):
                    completion(GeminiClient.sanitizeTitle(raw, fallbackSource: trimmed))
                case .failure:
                    completion(GeminiClient.fallbackTitle(from: trimmed))
                }
            }
        }
    }

    static func sanitizeTitle(_ raw: String, fallbackSource: String) -> String {
        var s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let strip: Set<Character> = ["\"", "'", "“", "”", "‚", "‘"]
        s = String(s.filter { !strip.contains($0) })
        if s.hasSuffix(".") { s.removeLast() }
        s = s.trimmingCharacters(in: .whitespaces)
        let words = s.split(whereSeparator: { $0.isWhitespace }).map(String.init)
        guard !words.isEmpty else { return fallbackTitle(from: fallbackSource) }
        let limited = Array(words.prefix(4))
        return limited.joined(separator: " ")
    }

    static func fallbackTitle(from text: String) -> String {
        let words = text.split(whereSeparator: { $0.isWhitespace }).map(String.init)
        guard !words.isEmpty else { return "Ohne Titel" }
        let limited = Array(words.prefix(4))
        return limited.joined(separator: " ")
    }

    /// Erzeugt eine kompakte Zusammenfassung von 6-8 deutschen Woertern, die
    /// beschreibt WOFUER ein gespeicherter Prompt da ist bzw. was er bewirkt.
    /// Wird als Hover-Tooltip ueber dem belegten Slot angezeigt. Leerer
    /// Rueckgabewert bei Fehler oder leerem Input — der Tooltip faellt dann auf
    /// den Standardtext zurueck, die Slot-Funktion bleibt unberuehrt (best-effort).
    func generateSlotSummary(_ text: String, completion: @escaping (String) -> Void) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { completion(""); return }

        let prompt = """
        Fasse in 6 bis 8 deutschen Woertern zusammen, WOFUER der folgende \
        Prompt da ist bzw. was er bewirkt. STRENGE REGELN: 6 bis 8 Woerter. \
        Keine Anfuehrungszeichen. Kein Punkt am Ende. Kein Praefix wie \
        'Zusammenfassung:'. Nur die nackte Wortgruppe zurueckgeben.

        PROMPT:
        \(trimmed)
        """

        DispatchQueue.global(qos: .userInitiated).async { [self] in
            self.sendRequest(prompt: prompt, attempt: 0) { result in
                switch result {
                case .success(let raw): completion(GeminiClient.sanitizeSummary(raw))
                case .failure: completion("")
                }
            }
        }
    }

    static func sanitizeSummary(_ raw: String) -> String {
        var s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        let strip: Set<Character> = ["\"", "'", "“", "”", "‚", "‘"]
        s = String(s.filter { !strip.contains($0) })
        if s.hasSuffix(".") { s.removeLast() }
        s = s.trimmingCharacters(in: .whitespaces)
        let words = s.split(whereSeparator: { $0.isWhitespace }).map(String.init)
        guard !words.isEmpty else { return "" }
        return Array(words.prefix(8)).joined(separator: " ")
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

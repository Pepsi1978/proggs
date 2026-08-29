import Foundation

final class GroqWhisperClient {
    private let apiKey: String
    /// Endpunkt, Modell und Sprache kommen aus der .env (Windows: WHISPER_URL /
    /// WHISPER_MODEL / WHISPER_LANG). Vorher standen alle drei fest im Code.
    private let endpoint: URL
    private let model: String
    private let language: String
    private let retryableStatusCodes: Set<Int> = [429, 500, 503]
    /// 1:1 Windows (GroqWhisperClient.cs: MaxRetries = 0). Retries sind dort
    /// bewusst abgeschaltet: Groq ist zeitweise ohnehin 40-50 s langsam, und
    /// drei Wiederholungen mit 2/4/8 s Pause haengen im Fehlerfall bis zu
    /// 14 Sekunden Wartezeit an, ohne dass ein Text ankommt. Der Benutzer
    /// drueckt schneller selbst noch einmal.
    private let maxRetries = 0
    private let delays: [TimeInterval] = [2, 4, 8]

    // ----- Upload-Groessenlimit / Chunking (Almanach bugs/apis/groq-api.md Nr. 11,
    // bugs/desktop/groq-transkription.md 4.3) -----
    // Groq weist zu grosse Uploads mit HTTP 413 ("Request Entity Too Large") ab: 25 MB im
    // Free-Plan, im Dev-Plan in der Praxis schon ab ~37 MB trotz dokumentierter 100 MB.
    // Bei 16 kHz mono 16-bit sind das 32 kB/s -> das Limit faellt nach rund 13 Minuten.
    // Vorfall 29.08.2026: 15,4 Minuten am Stueck diktiert -> 29,5 MB -> 413 -> der gesamte
    // gesprochene Text war verloren. Ein 413 ist NICHT retrybar (kleiner wird die Datei ja
    // nicht), deshalb wird lange Audio VOR dem Senden in Teile geschnitten.
    private static let wavHeaderSize = 44
    private static let maxUploadBytes = 20 * 1024 * 1024
    /// Ziel-Groesse eines Teilstuecks (~8,7 Min bei 16 kHz mono). Bewusst unter maxUploadBytes,
    /// damit die Pausensuche den Schnitt nach vorne verschieben darf, ohne das Limit zu reissen.
    private static let chunkTargetBytes = 16 * 1024 * 1024
    /// Suchfenster fuer den Schnitt an einer Sprechpause: die letzten 45 s vor dem Ziel-Ende.
    private static let cutSearchWindowMs = 45_000
    /// Ab so vielen zusammenhaengenden stillen 20-ms-Frames gilt eine Stelle als Sprechpause.
    private static let minPauseFrames = 4

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

    init(apiKey: String,
         model: String? = nil,
         language: String? = nil,
         url: String? = nil) {
        let cfg = Config.current
        self.apiKey = apiKey
        self.model = model ?? cfg?.whisperModel ?? "whisper-large-v3-turbo"
        self.language = language ?? cfg?.whisperLang ?? "de"
        let raw = url ?? cfg?.whisperUrl ?? "https://api.groq.com/openai/v1/audio/transcriptions"
        self.endpoint = URL(string: raw)
            ?? URL(string: "https://api.groq.com/openai/v1/audio/transcriptions")!
    }

    func transcribe(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [self] in
            // Lange Aufnahmen wuerden am Groq-Upload-Limit mit 413 scheitern (siehe
            // maxUploadBytes) — sie werden in Teile geschnitten und einzeln gesendet.
            let attrs = try? FileManager.default.attributesOfItem(atPath: fileURL.path)
            let size = (attrs?[.size] as? NSNumber)?.intValue ?? 0
            if size > Self.maxUploadBytes {
                self.transcribeInChunks(fileURL: fileURL, completion: completion)
            } else {
                self.sendRequest(fileURL: fileURL, attempt: 0, completion: completion)
            }
        }
    }

    // MARK: - Chunking langer Aufnahmen

    /// Kopfdaten einer 16-bit-PCM-WAV (fuer das Chunking langer Aufnahmen).
    private struct WavFormat {
        let sampleRate: Int
        let channels: Int
        let bitsPerSample: Int
        let blockAlign: Int
        let byteRate: Int
        let dataOffset: Int
        let dataLength: Int
    }

    /// Transkribiert eine Aufnahme, die ueber dem Groq-Upload-Limit liegt, in mehreren Teilen.
    /// Geschnitten wird bevorzugt in einer Sprechpause kurz vor der Ziel-Groesse, damit kein Wort
    /// zerrissen wird; findet sich keine Pause, wird hart geschnitten (funktionserhaltend: lieber
    /// eine Wortgrenze riskieren als die ganze Aufnahme verlieren). Faellt ein einzelner Teil aus,
    /// gehen NUR dessen Sekunden verloren — alle uebrigen Teile kommen trotzdem an.
    private func transcribeInChunks(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        guard let wav = try? Data(contentsOf: fileURL) else {
            completion(.failure(APIError.fileReadError))
            return
        }
        guard let fmt = Self.readWavFormat(wav) else {
            // Unlesbarer Header -> nicht schneiden, sondern wie bisher senden. Dann entscheidet
            // Groq (evtl. 413) — aber die Aufnahme wird nicht durch falsches Schneiden zerstoert.
            DiagLog.warn("Groq", "chunk_header_unreadable", [("bytes", wav.count)])
            sendRequest(fileURL: fileURL, attempt: 0, completion: completion)
            return
        }

        let ranges = Self.planChunks(wav, fmt)
        DiagLog.write("Groq", "chunk_plan",
                      [("bytes", wav.count), ("chunks", ranges.count),
                       ("limitBytes", Self.maxUploadBytes)])

        // Teilstuecke als temporaere Dateien ablegen — sendRequest arbeitet dateibasiert.
        // Das sind abgeleitete Daten; die Original-Aufnahme selbst bleibt unangetastet.
        var chunkURLs: [URL] = []
        let tmpDir = FileManager.default.temporaryDirectory
        for (i, range) in ranges.enumerated() {
            let chunk = Self.buildChunkWav(wav, fmt, range.start, range.length)
            let url = tmpDir.appendingPathComponent("tvo_chunk_\(UUID().uuidString)_\(i).wav")
            do {
                try chunk.write(to: url)
                chunkURLs.append(url)
            } catch {
                DiagLog.warn("Groq", "chunk_write_failed", [("index", i + 1), ("err", error.localizedDescription)])
            }
        }

        guard !chunkURLs.isEmpty else {
            completion(.failure(APIError.fileReadError))
            return
        }

        sendChunk(chunkURLs, index: 0, parts: [], failed: 0, completion: completion)
    }

    /// Schickt die Teilstuecke NACHEINANDER (Groq-Rate-Limits) und setzt die Texte zusammen.
    private func sendChunk(_ urls: [URL],
                           index: Int,
                           parts: [String],
                           failed: Int,
                           completion: @escaping (Result<String, Error>) -> Void) {
        guard index < urls.count else {
            for url in urls { try? FileManager.default.removeItem(at: url) }
            DiagLog.write("Groq", "chunk_merged",
                          [("chunks", urls.count), ("ok", parts.count), ("failed", failed)])
            if !parts.isEmpty {
                completion(.success(parts.joined(separator: " ")))
            } else if failed > 0 {
                // Wirklich alles gescheitert -> Fehler melden.
                completion(.failure(APIError.httpError(0, "Alle \(urls.count) Teilstuecke der langen Aufnahme sind fehlgeschlagen")))
            } else {
                // Kein Fehler, nur kein Sprachinhalt -> leerer Erfolg (fuegt nichts ein).
                completion(.success(""))
            }
            return
        }

        sendRequest(fileURL: urls[index], attempt: 0) { [weak self] result in
            guard let self = self else { return }
            var next = parts
            var failedNext = failed
            switch result {
            case .success(let text):
                let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
                DiagLog.write("Groq", "chunk_done",
                              [("index", index + 1), ("of", urls.count), ("chars", trimmed.count)])
                if !trimmed.isEmpty { next.append(trimmed) }
            case .failure(let error):
                // Funktionserhaltend: ein kaputter Teil darf die uebrigen Teile nicht mitreissen.
                failedNext += 1
                DiagLog.warn("Groq", "chunk_failed",
                             [("index", index + 1), ("of", urls.count), ("err", error.localizedDescription)])
            }
            self.sendChunk(urls, index: index + 1, parts: next, failed: failedNext, completion: completion)
        }
    }

    /// Legt die Byte-Bereiche der Teilstuecke fest. Jedes Teilstueck bleibt unter `maxUploadBytes`;
    /// der Schnitt wird, wenn moeglich, in die laengste Sprechpause im letzten
    /// `cutSearchWindowMs`-Fenster vor der Ziel-Groesse gelegt.
    private static func planChunks(_ wav: Data, _ fmt: WavFormat) -> [(start: Int, length: Int)] {
        var ranges: [(start: Int, length: Int)] = []
        let voiced = buildVoicedTimeline(wav, fmt)
        let frameBytes = max(fmt.blockAlign, fmt.sampleRate * frameMs / 1000 * fmt.blockAlign)
        let dataEnd = fmt.dataOffset + fmt.dataLength
        let payloadPerChunk = chunkTargetBytes - wavHeaderSize

        var pos = fmt.dataOffset
        while pos < dataEnd {
            let remaining = dataEnd - pos
            if remaining + wavHeaderSize <= maxUploadBytes {
                ranges.append((start: pos, length: remaining))
                break
            }

            let hardEnd = pos + payloadPerChunk
            var cut = findPauseCut(voiced, fmt, frameBytes, pos, hardEnd) ?? hardEnd
            // Nie mitten in ein Sample schneiden — sonst knackt der naechste Teil am Anfang.
            cut -= (cut - fmt.dataOffset) % fmt.blockAlign
            if cut <= pos { cut = hardEnd }              // Sicherheitsnetz gegen Endlosschleife
            if cut > dataEnd { cut = dataEnd }
            ranges.append((start: pos, length: cut - pos))
            pos = cut
        }
        return ranges
    }

    /// Sucht im Fenster [hardEnd - cutSearchWindowMs, hardEnd] die laengste zusammenhaengende
    /// Stille und liefert deren Mitte als Schnitt-Byte. nil, wenn keine ausreichende Pause da ist.
    private static func findPauseCut(_ voiced: [Bool]?,
                                     _ fmt: WavFormat,
                                     _ frameBytes: Int,
                                     _ rangeStart: Int,
                                     _ hardEnd: Int) -> Int? {
        guard let voiced = voiced, frameBytes > 0 else { return nil }
        let windowBytes = min(cutSearchWindowMs * fmt.byteRate / 1000, hardEnd - rangeStart)
        guard windowBytes > 0 else { return nil }

        let fromFrame = max(0, (hardEnd - windowBytes - fmt.dataOffset) / frameBytes)
        let toFrame = min(voiced.count, (hardEnd - fmt.dataOffset) / frameBytes)
        guard toFrame - fromFrame >= minPauseFrames else { return nil }

        var bestStart = -1, bestLen = 0, runStart = -1
        for f in fromFrame..<toFrame {
            if !voiced[f] {
                if runStart < 0 { runStart = f }
                let len = f - runStart + 1
                if len > bestLen { bestLen = len; bestStart = runStart }
            } else {
                runStart = -1
            }
        }
        guard bestLen >= minPauseFrames else { return nil }

        let midFrame = bestStart + bestLen / 2
        return fmt.dataOffset + midFrame * frameBytes
    }

    /// Baut aus einem Byte-Bereich der Quelldatei eine eigenstaendige WAV mit frischem
    /// kanonischem 44-Byte-Header (RIFF- und data-Groesse passend zum Teilstueck).
    private static func buildChunkWav(_ source: Data, _ fmt: WavFormat, _ dataStart: Int, _ dataLength: Int) -> Data {
        var header = Data(capacity: wavHeaderSize)
        func ascii(_ s: String) { header.append(contentsOf: Array(s.utf8)) }
        func i32(_ v: Int) {
            let u = UInt32(truncatingIfNeeded: v)
            header.append(contentsOf: [UInt8(u & 0xFF), UInt8((u >> 8) & 0xFF),
                                       UInt8((u >> 16) & 0xFF), UInt8((u >> 24) & 0xFF)])
        }
        func i16(_ v: Int) {
            let u = UInt16(truncatingIfNeeded: v)
            header.append(contentsOf: [UInt8(u & 0xFF), UInt8((u >> 8) & 0xFF)])
        }

        ascii("RIFF"); i32(wavHeaderSize + dataLength - 8); ascii("WAVE")
        ascii("fmt "); i32(16); i16(1)
        i16(fmt.channels); i32(fmt.sampleRate); i32(fmt.byteRate)
        i16(fmt.blockAlign); i16(fmt.bitsPerSample)
        ascii("data"); i32(dataLength)

        var out = header
        out.append(source.subdata(in: dataStart..<(dataStart + dataLength)))
        return out
    }

    /// Liest fmt- und data-Chunk einer RIFF/WAVE-Datei. Laeuft die Chunk-Kette echt durch, statt
    /// den 44-Byte-Standardheader anzunehmen — Aufnahmen mit LIST/fact-Chunk werden sonst falsch
    /// geschnitten. nil bei allem, was nicht als 16-bit-PCM lesbar ist.
    private static func readWavFormat(_ wav: Data) -> WavFormat? {
        let b = [UInt8](wav)
        guard b.count >= wavHeaderSize else { return nil }
        func tag(_ at: Int) -> String {
            String(bytes: b[at..<(at + 4)], encoding: .ascii) ?? ""
        }
        func le32(_ at: Int) -> Int {
            Int(b[at]) | (Int(b[at + 1]) << 8) | (Int(b[at + 2]) << 16) | (Int(b[at + 3]) << 24)
        }
        func le16(_ at: Int) -> Int { Int(b[at]) | (Int(b[at + 1]) << 8) }

        guard tag(0) == "RIFF", tag(8) == "WAVE" else { return nil }

        var channels = 0, sampleRate = 0, byteRate = 0, blockAlign = 0, bits = 0
        var dataOffset = -1, dataLength = 0
        var pos = 12
        while pos + 8 <= b.count {
            let id = tag(pos)
            let size = le32(pos + 4)
            if size < 0 { return nil }
            let body = pos + 8
            if id == "fmt ", size >= 16, body + 16 <= b.count {
                channels = le16(body + 2)
                sampleRate = le32(body + 4)
                byteRate = le32(body + 8)
                blockAlign = le16(body + 12)
                bits = le16(body + 14)
            } else if id == "data" {
                dataOffset = body
                // Groesse aus dem Header kann bei abgebrochener Aufnahme zu gross sein -> kappen.
                dataLength = min(size, b.count - body)
                break
            }
            pos = body + size + (size % 2)   // RIFF-Chunks sind auf gerade Byte-Grenzen gepaddet
        }

        guard dataOffset >= 0, dataLength > 0, bits == 16, channels > 0, sampleRate > 0 else { return nil }
        let align = blockAlign > 0 ? blockAlign : channels * bits / 8
        let rate = byteRate > 0 ? byteRate : sampleRate * align
        return WavFormat(sampleRate: sampleRate, channels: channels, bitsPerSample: bits,
                         blockAlign: align, byteRate: rate,
                         dataOffset: dataOffset, dataLength: dataLength)
    }

    /// Voiced-Timeline ueber den echten data-Bereich (Variante fuer den Chunking-Pfad, der den
    /// Header nicht als fix 44 Byte annehmen darf).
    private static func buildVoicedTimeline(_ wav: Data, _ fmt: WavFormat) -> [Bool]? {
        let bytes = [UInt8](wav)
        let frameSamples = max(1, fmt.sampleRate * frameMs / 1000)
        let frameBytes = frameSamples * fmt.blockAlign
        let frameCount = fmt.dataLength / frameBytes
        guard frameCount > 0 else { return nil }
        var voiced = [Bool](repeating: false, count: frameCount)
        for f in 0..<frameCount {
            let baseB = fmt.dataOffset + f * frameBytes
            var sumSq = 0.0
            for s in 0..<frameSamples {
                let idx = baseB + s * fmt.blockAlign      // bei Stereo nur der linke Kanal
                let sample = Int16(bitPattern: UInt16(bytes[idx]) | (UInt16(bytes[idx + 1]) << 8))
                let v = Double(sample) / 32768.0
                sumSq += v * v
            }
            voiced[f] = (sumSq / Double(frameSamples)).squareRoot() > speechRmsThreshold
        }
        return voiced
    }

    private func sendRequest(fileURL: URL, attempt: Int, completion: @escaping (Result<String, Error>) -> Void) {
        let totalStart = DiagLog.now()
        DiagLog.write("Groq", "transcribe_start", [("file", fileURL.lastPathComponent), ("attempt", attempt)])
        guard let audioData = try? Data(contentsOf: fileURL) else {
            DiagLog.warn("Groq", "read_wav_failed", [("file", fileURL.lastPathComponent)])
            completion(.failure(APIError.fileReadError))
            return
        }

        // Schicht 1: Aufnahme ohne erkennbaren Sprachinhalt gar nicht senden (faengt "Aufnahme
        // gestartet, nichts gesagt"). WICHTIG: .success("") statt .failure — ein .failure wuerde
        // ueber pasteError eine Fehlermeldung ins Terminal schreiben. Leerer Erfolg -> der
        // .success-Leer-Guard im AppDelegate fuegt nichts ein. Nur beim ersten Versuch pruefen.
        if attempt == 0 && !GroqWhisperClient.hasSpeechContent(audioData) {
            DiagLog.warn("Groq", "prefilter_rejected", [("bytes", audioData.count)])
            completion(.success(""))
            return
        }

        let boundary = UUID().uuidString
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        // 1:1 Windows (TransportTimeoutSeconds = 75). 180 s liessen die App bei
        // einem haengenden Groq-Aufruf drei Minuten scheinbar "verarbeiten".
        request.timeoutInterval = 75

        var body = Data()
        // verbose_json statt text: liefert die Confidence-Felder fuers Halluzinations-Gate,
        // bei Groq ohne Mehrlatenz/-kosten (nur word-Timestamps kosten extra — nicht angefordert).
        let fields: [(String, String)] = [
            ("model", model),
            ("language", language),
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

        let httpStart = DiagLog.now()
        DiagLog.write("Groq", "http_start", [("attempt", attempt), ("bytes", audioData.count)])
        let task = URLSession.shared.dataTask(with: request) { [self] data, response, error in
            if let error = error {
                DiagLog.error("Groq", "http_failed", error, [("attempt", attempt)])
                completion(.failure(error))
                return
            }

            let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
            DiagLog.perf("Groq", "http_response", since: httpStart,
                         [("attempt", attempt), ("status", statusCode)])

            if (200...299).contains(statusCode), let data = data {
                let raw = String(data: data, encoding: .utf8) ?? ""
                // Schicht 3: Voiced-Timeline aus dem aufgenommenen PCM bauen (Segment-Audio-Abgleich).
                let voiced = GroqWhisperClient.buildVoicedTimeline(audioData)
                // Schicht 2 (Confidence-Gate) + Schicht 3 (Audio-Abgleich). Bleibt Text uebrig ->
                // .success(text). Alles Stille/halluziniert -> leer -> .success("") (Leer-Guard fuegt nichts ein).
                let filtered = GroqWhisperClient.filterTranscription(raw, voiced)
                DiagLog.perf("Groq", "transcribe_total", since: totalStart,
                             [("jsonChars", raw.count), ("textChars", filtered.count)])
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
            DiagLog.warn("Groq", "http_error", [("status", statusCode),
                                                ("body", String(responseText.prefix(500)))])
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
            // Schicht 4 trotzdem anwenden (auch hier kann eine reine Floskel stehen).
            return blockIfFloskel(extractFallbackText(data), voiced)
        }
        guard let segments = parsed.segments, !segments.isEmpty else {
            // Ultrakurze Clips beim schnellen Druecken liefern oft NUR top-level Text, KEINE Segmente
            // (Almanach §2.3). Funktionserhalt: durchlassen, aber ZUERST durch die Floskel-Blocklist —
            // sonst umgeht "Vielen Dank" bei Kurzclips den ganzen Stille-Schutz.
            return blockIfFloskel((parsed.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines), voiced)
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
            DiagLog.warn("Groq", "audio_filter_drift_fallback", [("segments", afterConfidence.count)])
            finalSegs = afterConfidence
        }
        var parts: [String] = []
        for seg in finalSegs {
            let t = (seg.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !t.isEmpty { parts.append(t) }
        }
        if droppedConfidence > 0 || droppedAudio > 0 {
            NSLog("Groq: %d Confidence- + %d Audio-Segment(e) verworfen (Stille-Schutz).", droppedConfidence, droppedAudio)
            DiagLog.write("Groq", "segments_filtered", [("confidence", droppedConfidence),
                                                        ("audio", droppedAudio),
                                                        ("kept", finalSegs.count)])
        }
        // Schicht 4: Floskel-Blocklist (letzter Filter, an ALLEN Ausgaengen — siehe oben). Faengt
        // "Vielen Dank", das Schicht 2+3 ueberlebt. Funktionserhaltend (kurz + exakt + Stille-Kontext).
        return blockIfFloskel(parts.joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines), voiced)
    }

    /// Schicht 4 zentral: gibt den Text zurueck — oder leer, wenn er eine Stille-Floskel ist. An JEDEM
    /// Rueckgabepfad von filterTranscription aufgerufen, damit auch der "keine Segmente"-Kurzclip-Pfad
    /// (Almanach §2.3) gefiltert wird.
    private static func blockIfFloskel(_ text: String, _ voiced: [Bool]?) -> String {
        if isBlocklistedFloskel(text, voiced) {
            NSLog("Groq: Floskel-Blocklist (Schicht 4) verwarf \"%@\" (kurz + exakter Match + Stille-Kontext).", text)
            DiagLog.warn("Groq", "floskel_blocked", [("text", text)])
            return ""
        }
        return text
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
        DiagLog.write("Groq", "prefilter_measure", [("voicedMs", Int(voicedMs)),
                                                    ("thresholdMs", Int(minSpeechMs)),
                                                    ("ok", voicedMs >= minSpeechMs)])
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

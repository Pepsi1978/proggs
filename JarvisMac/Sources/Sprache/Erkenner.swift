import AppKit
import AVFoundation
import Foundation
import Speech

// MARK: - Rechte

enum SprachRechte {
    /// Mikrofonrecht just-in-time anfragen (Almanach: nie alle Rechte beim Start).
    static func mikrofon() async -> Bool {
        switch AVCaptureDevice.authorizationStatus(for: .audio) {
        case .authorized: return true
        case .notDetermined: return await AVCaptureDevice.requestAccess(for: .audio)
        default: return false
        }
    }

    static func spracherkennung() async -> Bool {
        switch SFSpeechRecognizer.authorizationStatus() {
        case .authorized: return true
        case .notDetermined:
            return await withCheckedContinuation { (k: CheckedContinuation<Bool, Never>) in
                SFSpeechRecognizer.requestAuthorization { k.resume(returning: $0 == .authorized) }
            }
        default: return false
        }
    }
}

// MARK: - Mikrofon

/// Eine Aufnahme über eine eigene AVAudioEngine. Tap-Format immer vom inputNode (Almanach §E4),
/// Neustart nach Gerätewechsel über AVAudioEngineConfigurationChange (§E5).
final class Mikrofon: @unchecked Sendable {
    private var engine: AVAudioEngine?
    private var beobachter: NSObjectProtocol?
    private let lock = NSLock()
    /// Läuft auf dem Audio-Thread.
    var beiPuffer: ((AVAudioPCMBuffer) -> Void)?
    /// Wird nach einem Gerätewechsel aufgerufen (Hauptthread), nachdem die Aufnahme neu gestartet wurde oder scheiterte.
    var beiGeraetewechsel: (() -> Void)?

    var laeuft: Bool { lock.lock(); defer { lock.unlock() }; return engine?.isRunning ?? false }

    func starten() throws {
        stoppen()
        let e = AVAudioEngine()
        let eingang = e.inputNode
        let format = eingang.outputFormat(forBus: 0)
        guard format.sampleRate > 0, format.channelCount > 0 else {
            throw NSError(domain: "Jarvis", code: 1, userInfo: [NSLocalizedDescriptionKey: "Kein Mikrofon verfügbar"])
        }
        eingang.installTap(onBus: 0, bufferSize: 2048, format: format) { [weak self] puffer, _ in
            self?.beiPuffer?(puffer)
        }
        e.prepare()
        try e.start()
        lock.lock(); engine = e; lock.unlock()
        beobachter = NotificationCenter.default.addObserver(forName: .AVAudioEngineConfigurationChange, object: e, queue: .main) { [weak self] _ in
            guard let self else { return }
            log("sprache", "Audiogerät gewechselt – Aufnahme startet neu")
            try? self.starten()
            self.beiGeraetewechsel?()
        }
    }

    func stoppen() {
        if let b = beobachter { NotificationCenter.default.removeObserver(b); beobachter = nil }
        lock.lock(); let e = engine; engine = nil; lock.unlock()
        guard let e else { return }
        e.inputNode.removeTap(onBus: 0)
        e.stop()
    }

    static func pegel(_ p: AVAudioPCMBuffer) -> Float {
        guard let daten = p.floatChannelData, p.frameLength > 0 else { return 0 }
        let n = Int(p.frameLength)
        var summe: Float = 0
        let k = daten[0]
        for i in 0..<n { summe += k[i] * k[i] }
        return sqrt(summe / Float(n))
    }
}

/// Wandelt Mikrofonpuffer in ein Zielformat (z. B. 16 kHz mono) um.
final class Umwandler: @unchecked Sendable {
    let ziel: AVAudioFormat
    private var konverter: AVAudioConverter?
    private var quelle: AVAudioFormat?

    init(ziel: AVAudioFormat) { self.ziel = ziel }

    func wandle(_ p: AVAudioPCMBuffer) -> AVAudioPCMBuffer? {
        if quelle == nil || quelle! != p.format {
            quelle = p.format
            konverter = AVAudioConverter(from: p.format, to: ziel)
        }
        guard let konverter else { return nil }
        let kapazitaet = AVAudioFrameCount(Double(p.frameLength) * ziel.sampleRate / p.format.sampleRate) + 64
        guard let aus = AVAudioPCMBuffer(pcmFormat: ziel, frameCapacity: kapazitaet) else { return nil }
        var geliefert = false
        var fehler: NSError?
        konverter.convert(to: aus, error: &fehler) { _, status in
            if geliefert { status.pointee = .noDataNow; return nil }
            geliefert = true
            status.pointee = .haveData
            return p
        }
        return fehler == nil && aus.frameLength > 0 ? aus : nil
    }
}

// MARK: - Apple-Spracherkennung (On-Device, Deutsch)

/// Streaming-Erkennung: bevorzugt SpeechAnalyzer/SpeechTranscriber (macOS 26), sonst SFSpeechRecognizer on-device.
/// `beiText(gesamt, final)` liefert den bisherigen Gesamttext (fertige Teile + vorläufiger Rest).
final class ApfelErkenner: @unchecked Sendable {
    var beiText: ((String, Bool) -> Void)?
    /// Nur für das Weckwort: fertige Teile nicht aufsammeln (Dauerbetrieb).
    let dauerbetrieb: Bool

    private let lock = NSLock()
    private var fertig = ""
    private var vorlaeufig = ""

    // SpeechAnalyzer
    private var analysator: SpeechAnalyzer?
    private var fortsetzung: AsyncStream<AnalyzerInput>.Continuation?
    private var umwandler: Umwandler?
    private var ergebnisAufgabe: Task<Void, Never>?
    // SFSpeechRecognizer
    private var sfErkenner: SFSpeechRecognizer?
    private var sfAnfrage: SFSpeechAudioBufferRecognitionRequest?
    private var sfAufgabe: SFSpeechRecognitionTask?
    private var sfFertig: CheckedContinuation<Void, Never>?

    private(set) var nutztAnalyzer = false

    init(dauerbetrieb: Bool) { self.dauerbetrieb = dauerbetrieb }

    var text: String {
        lock.lock(); defer { lock.unlock() }
        return (fertig + " " + vorlaeufig).trimmingCharacters(in: .whitespaces)
    }

    private static let gebietsschema = Locale(identifier: "de-DE")

    func starten() async throws {
        if SpeechTranscriber.isAvailable, let ort = await SpeechTranscriber.supportedLocale(equivalentTo: Self.gebietsschema) {
            do { try await starteAnalyzer(ort); return } catch {
                log("sprache", "SpeechAnalyzer nicht nutzbar (\(error.localizedDescription)) – Rückfall auf SFSpeechRecognizer")
            }
        }
        try await starteSF()
    }

    private func starteAnalyzer(_ ort: Locale) async throws {
        let t = SpeechTranscriber(locale: ort, transcriptionOptions: [], reportingOptions: [.volatileResults, .fastResults], attributeOptions: [])
        if let anfrage = try await AssetInventory.assetInstallationRequest(supporting: [t]) {
            log("sprache", "Lade deutsches Sprachmodell für die Erkennung …")
            try await anfrage.downloadAndInstall()
        }
        guard let format = await SpeechAnalyzer.bestAvailableAudioFormat(compatibleWith: [t]) else {
            throw NSError(domain: "Jarvis", code: 2, userInfo: [NSLocalizedDescriptionKey: "Kein Audioformat für die Erkennung"])
        }
        let (strom, fort) = AsyncStream<AnalyzerInput>.makeStream()
        let a = SpeechAnalyzer(modules: [t])
        try await a.prepareToAnalyze(in: format)
        umwandler = Umwandler(ziel: format)
        fortsetzung = fort
        analysator = a
        nutztAnalyzer = true
        ergebnisAufgabe = Task { [weak self] in
            do {
                for try await r in t.results {
                    guard let self else { return }
                    let s = String(r.text.characters).trimmingCharacters(in: .whitespacesAndNewlines)
                    let gesamt = self.lock.withLock { () -> String in
                        if r.isFinal {
                            if self.dauerbetrieb { self.fertig = s } else if !s.isEmpty { self.fertig += (self.fertig.isEmpty ? "" : " ") + s }
                            self.vorlaeufig = ""
                        } else {
                            self.vorlaeufig = s
                        }
                        return (self.fertig + " " + self.vorlaeufig).trimmingCharacters(in: .whitespaces)
                    }
                    self.beiText?(gesamt, r.isFinal)
                }
            } catch {
                log("sprache", "Erkennung beendet: \(error.localizedDescription)")
            }
        }
        try await a.start(inputSequence: strom)
    }

    private func starteSF() async throws {
        guard await SprachRechte.spracherkennung() else {
            throw NSError(domain: "Jarvis", code: 3, userInfo: [NSLocalizedDescriptionKey: "Spracherkennung nicht erlaubt (Systemeinstellungen → Datenschutz)"])
        }
        guard let e = SFSpeechRecognizer(locale: Self.gebietsschema), e.isAvailable else {
            throw NSError(domain: "Jarvis", code: 4, userInfo: [NSLocalizedDescriptionKey: "Deutsche Spracherkennung nicht verfügbar"])
        }
        let anfrage = SFSpeechAudioBufferRecognitionRequest()
        anfrage.shouldReportPartialResults = true
        if e.supportsOnDeviceRecognition { anfrage.requiresOnDeviceRecognition = true }
        anfrage.addsPunctuation = true
        sfErkenner = e
        sfAnfrage = anfrage
        nutztAnalyzer = false
        sfAufgabe = e.recognitionTask(with: anfrage) { [weak self] ergebnis, fehler in
            guard let self else { return }
            if let ergebnis {
                let s = ergebnis.bestTranscription.formattedString
                self.lock.lock(); self.vorlaeufig = s; if ergebnis.isFinal { self.fertig = s; self.vorlaeufig = "" }; self.lock.unlock()
                self.beiText?(s, ergebnis.isFinal)
            }
            if fehler != nil || (ergebnis?.isFinal ?? false) {
                self.lock.lock(); let k = self.sfFertig; self.sfFertig = nil; self.lock.unlock()
                k?.resume()
            }
        }
    }

    /// Audio-Thread.
    func fuettere(_ p: AVAudioPCMBuffer) {
        if let fortsetzung, let umwandler {
            if let w = umwandler.wandle(p) { fortsetzung.yield(AnalyzerInput(buffer: w)) }
        } else {
            sfAnfrage?.append(p)
        }
    }

    /// Beendet die Erkennung. `abschliessen` = auf das Endergebnis warten (Diktat), sonst sofort abbrechen.
    @discardableResult
    func beenden(abschliessen: Bool) async -> String {
        if let a = analysator {
            fortsetzung?.finish()
            fortsetzung = nil
            if abschliessen {
                try? await a.finalizeAndFinishThroughEndOfInput()
                let aufgabe = ergebnisAufgabe
                _ = await mitFrist(3) { await aufgabe?.value }
            } else {
                await a.cancelAndFinishNow()
                ergebnisAufgabe?.cancel()
            }
            analysator = nil
        } else if let anfrage = sfAnfrage {
            if abschliessen {
                anfrage.endAudio()
                _ = await mitFrist(3) {
                    await withCheckedContinuation { (k: CheckedContinuation<Void, Never>) in
                        self.lock.withLock { self.sfFertig = k }
                    }
                }
                let k = self.lock.withLock { () -> CheckedContinuation<Void, Never>? in let k = self.sfFertig; self.sfFertig = nil; return k }
                k?.resume()
            }
            sfAufgabe?.cancel()
            sfAufgabe = nil; sfAnfrage = nil
        }
        return text
    }
}

/// Wartet höchstens `sekunden` auf eine Arbeit.
func mitFrist(_ sekunden: Double, _ arbeit: @escaping @Sendable () async -> Void) async -> Bool {
    await withTaskGroup(of: Bool.self) { g in
        g.addTask { await arbeit(); return true }
        g.addTask { try? await Task.sleep(for: .seconds(sekunden)); return false }
        let erstes = await g.next() ?? false
        g.cancelAll()
        return erstes
    }
}

// MARK: - Groq Whisper

enum GroqWhisper {
    private static let floskeln: Set<String> = [
        "vielen dank", "danke", "danke schön", "vielen dank fürs zuschauen", "vielen dank für ihre aufmerksamkeit",
        "untertitel im auftrag des zdf", "untertitel im auftrag des zdf für funk", "untertitelung des zdf",
        "untertitel von stephanie geiges", "copyright wdr", "bis zum nächsten mal", "tschüss", "amen",
        "untertitel der amaraorg community", "swr", "musik",
    ]

    static func normal(_ s: String) -> String {
        var c = ""
        for z in s.lowercased() { if z.isLetter { c.append(z) } else if z.isWhitespace { c.append(" ") } }
        return c.split(separator: " ").joined(separator: " ")
    }

    /// Transkribiert 16-kHz-Mono-Samples. `sprachSekunden` = gemessene laute Zeit (für den Floskelfilter).
    static func transkribiere(_ samples: [Float], sprachSekunden: Double, schluessel: String) async throws -> String {
        guard !schluessel.isEmpty else {
            throw NSError(domain: "Jarvis", code: 5, userInfo: [NSLocalizedDescriptionKey: "Kein Groq-Schlüssel eingetragen"])
        }
        let wav = Wav.kodiere(samples, rate: 16000)
        let grenze = UUID().uuidString
        var req = URLRequest(url: URL(string: "https://api.groq.com/openai/v1/audio/transcriptions")!, timeoutInterval: 60)
        req.httpMethod = "POST"
        req.setValue("Bearer \(schluessel)", forHTTPHeaderField: "Authorization")
        req.setValue("multipart/form-data; boundary=\(grenze)", forHTTPHeaderField: "Content-Type")
        var rumpf = Data()
        for (k, v) in [("model", "whisper-large-v3-turbo"), ("language", "de"), ("response_format", "verbose_json"), ("temperature", "0")] {
            rumpf.append(Data("--\(grenze)\r\nContent-Disposition: form-data; name=\"\(k)\"\r\n\r\n\(v)\r\n".utf8))
        }
        rumpf.append(Data("--\(grenze)\r\nContent-Disposition: form-data; name=\"file\"; filename=\"aufnahme.wav\"\r\nContent-Type: audio/wav\r\n\r\n".utf8))
        rumpf.append(wav)
        rumpf.append(Data("\r\n--\(grenze)--\r\n".utf8))
        req.httpBody = rumpf
        let (daten, antwort) = try await URLSession.shared.data(for: req)
        let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
        guard code == 200, let o = JSON.objekt(daten) else {
            throw NSError(domain: "Jarvis", code: code, userInfo: [NSLocalizedDescriptionKey: "Groq \(code): \(String(decoding: daten.prefix(300), as: UTF8.self))"])
        }
        // Halluzinationsfilter: Segmente mit hoher Stille-Wahrscheinlichkeit verwerfen.
        var text: String
        if let segmente = o["segments"] as? [JSONObjekt], !segmente.isEmpty {
            text = segmente.filter { s in
                let stille = s["no_speech_prob"] as? Double ?? 0
                let logp = s["avg_logprob"] as? Double ?? 0
                return !(stille > 0.6 && logp < -0.7)
            }.compactMap { $0["text"] as? String }.joined(separator: " ")
        } else {
            text = o["text"] as? String ?? ""
        }
        text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let n = normal(text)
        if floskeln.contains(n) && sprachSekunden < 1.5 { return "" }
        return text
    }
}

enum Wav {
    static func kodiere(_ samples: [Float], rate: Int) -> Data {
        var d = Data()
        func u32(_ v: UInt32) { var x = v.littleEndian; d.append(Data(bytes: &x, count: 4)) }
        func u16(_ v: UInt16) { var x = v.littleEndian; d.append(Data(bytes: &x, count: 2)) }
        let bytes = UInt32(samples.count * 2)
        d.append(Data("RIFF".utf8)); u32(36 + bytes); d.append(Data("WAVE".utf8))
        d.append(Data("fmt ".utf8)); u32(16); u16(1); u16(1); u32(UInt32(rate)); u32(UInt32(rate * 2)); u16(2); u16(16)
        d.append(Data("data".utf8)); u32(bytes)
        var pcm = [Int16](repeating: 0, count: samples.count)
        for i in 0..<samples.count { pcm[i] = Int16(max(-1, min(1, samples[i])) * 32767) }
        pcm.withUnsafeBufferPointer { d.append(Data(buffer: $0)) }
        return d
    }

    /// Liest PCM16-Mono aus einer WAV-Datei (sucht den data-Chunk). Gibt Samples und Rate zurück.
    static func lies(_ d: Data) -> (samples: [Float], rate: Double)? {
        let b = [UInt8](d)
        guard b.count > 44, String(decoding: b[0..<4], as: UTF8.self) == "RIFF" else { return nil }
        func le32(_ i: Int) -> Int { Int(b[i]) | Int(b[i + 1]) << 8 | Int(b[i + 2]) << 16 | Int(b[i + 3]) << 24 }
        var rate = 24000.0
        var i = 12
        while i + 8 <= b.count {
            let id = String(decoding: b[i..<i + 4], as: UTF8.self)
            let len = le32(i + 4)
            if id == "fmt " { rate = Double(le32(i + 12)) }
            if id == "data" {
                let start = i + 8
                let ende = min(b.count, start + len)
                var s = [Float](); s.reserveCapacity((ende - start) / 2)
                var j = start
                while j + 1 < ende {
                    let v = Int16(bitPattern: UInt16(b[j]) | UInt16(b[j + 1]) << 8)
                    s.append(Float(v) / 32768)
                    j += 2
                }
                return (s, rate)
            }
            i += 8 + len + (len % 2)
        }
        return nil
    }
}

// MARK: - Töne

enum Toene {
    @MainActor static func start() { NSSound(named: "Tink")?.play() }
    @MainActor static func ende() { NSSound(named: "Pop")?.play() }
    @MainActor static func weckwort() { NSSound(named: "Morse")?.play() }
}

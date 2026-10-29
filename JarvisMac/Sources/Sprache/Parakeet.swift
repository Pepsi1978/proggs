import AVFoundation
import Foundation

/// Offline-Transkription mit parakeet-primeline: deutscher Fine-Tune von nvidia/parakeet-tdt-0.6b-v3,
/// int8-ONNX ueber sherpa-onnx (C-API, libsherpa-onnx-c-api.dylib im App-Bundle), nur CPU mit 4 Threads.
/// Genau das Modell aus github.com/winidi/dictate: Hugging Face flozen1981/parakeet-primeline-onnx in der
/// dort festgelegten Revision. Die Dateien (rund 640 MB) liegen einmal pro Mac unter
/// ~/Library/Application Support/VoiceOverlays/ und werden von TVO und CVO geteilt.
///
/// Nach dem einmaligen Download kein Netz, kein Schluessel. Fehlt das Modell oder scheitert es, gibt es
/// einen Fehler, nie einen Rueckfall auf Groq oder Gemini.
/// Lizenz CC-BY-4.0: primeline (Fine-Tune), NVIDIA (Basismodell), k2-fsa/sherpa-onnx (Laufzeit).
///
/// Kopie aus TerminalVoiceOverlay-macOS (dort auch in ClaudeCodexVoiceOverlay-macOS); JarvisMac nutzt dasselbe geteilte Modell.
/// Ergänzt: Mitschrift direkt aus Samples (async) und Messwerte für die Einstellungen wie am Handy.
final class ParakeetLocalClient: @unchecked Sendable {

    enum ParakeetError: LocalizedError {
        case notInstalled
        case loadFailed
        case audioUnreadable
        case download(String)

        var errorDescription: String? {
            switch self {
            case .notInstalled: return "Offline-Modell fehlt — in den Einstellungen \"Offline-Modell laden\" klicken"
            case .loadFailed: return "Offline-Modell ließ sich nicht laden"
            case .audioUnreadable: return "Aufnahme für die Offline-Transkription nicht lesbar"
            case .download(let s): return "Download des Offline-Modells fehlgeschlagen: \(s)"
            }
        }
    }

    static let shared = ParakeetLocalClient()

    static let repo = "flozen1981/parakeet-primeline-onnx"
    static let revision = "d548e25b9bfe559aa274f361892dc4ed5d64743a"
    /// Dateien mit ihrer Groesse in genau dieser Revision; die Groesse dient als Vollstaendigkeitspruefung.
    static let files: [(name: String, size: Int64)] = [
        ("tokens.txt", 102_132),
        ("decoder.int8.onnx", 11_845_275),
        ("joiner.int8.onnx", 6_355_277),
        ("encoder.int8.onnx", 1_548_009),
        // Die Gewichte des Encoders; sie muessen neben encoder.int8.onnx liegen.
        ("encoder.int8.onnx.data", 650_776_320),
    ]
    static var totalBytes: Int64 { files.reduce(0) { $0 + $1.size } }

    static var modelDir: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("Library/Application Support/VoiceOverlays/parakeet-primeline-onnx")
    }

    static var isInstalled: Bool {
        files.allSatisfy { f in
            let p = modelDir.appendingPathComponent(f.name).path
            let size = (try? FileManager.default.attributesOfItem(atPath: p)[.size] as? NSNumber)??.int64Value
            return size == f.size
        }
    }

    private static let sampleRate = 16_000
    private static let threads: Int32 = 4
    /// Der Encoder scheitert ueber 400 s und braucht davor quadratisch mehr Speicher: laengere Diktate in Stuecken.
    private static let maxChunkS = 90
    private static let retryChunkS = 20
    /// Leise Aufnahmen kommen sonst leer zurueck: auf diese Spitze anheben, hoechstens um maxGain.
    private static let targetPeak: Float = 0.5
    private static let maxGain: Float = 30
    /// Nach so langer Ruhe gibt das Overlay den Arbeitsspeicher wieder frei (TVO und CVO laufen gleichzeitig).
    private static let idleUnloadS: Double = 15 * 60

    private let queue = DispatchQueue(label: "parakeet.local", qos: .userInitiated)
    private var recognizer: OpaquePointer?
    private var idleWork: DispatchWorkItem?

    /// Laedt das Modell im Hintergrund vor (beim Aufnahmestart), damit das Diktat nicht wartet.
    func preload() {
        guard Self.isInstalled else { return }
        queue.async { _ = try? self.loadIfNeeded(); self.scheduleUnload() }
    }

    /// Mitschrift einer fertigen Aufnahme, ganz auf diesem Mac.
    func transcribe(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        queue.async {
            do {
                let r = try self.loadIfNeeded()
                let samples = try Self.readSamples(fileURL)
                let start = CFAbsoluteTimeGetCurrent()
                var parts: [String] = []
                for chunk in Self.split(samples, maxS: Self.maxChunkS) {
                    var text = Self.decode(r, chunk)
                    // Ein langes Stueck kommt gelegentlich leer zurueck; kurze Teile derselben Aufnahme klappen.
                    if text.isEmpty && chunk.count > Self.retryChunkS * Self.sampleRate {
                        text = Self.split(chunk, maxS: Self.retryChunkS).map { Self.decode(r, $0) }
                            .filter { !$0.isEmpty }.joined(separator: " ")
                    }
                    if !text.isEmpty { parts.append(text) }
                }
                let ms = Int((CFAbsoluteTimeGetCurrent() - start) * 1000)
                log("sprache", "Parakeet: \(samples.count * 1000 / Self.sampleRate) ms Aufnahme in \(ms) ms gerechnet")
                self.setzeMessung(audioMs: samples.count * 1000 / Self.sampleRate, rechenMs: ms)
                self.scheduleUnload()
                completion(.success(parts.joined(separator: " ")))
            } catch {
                log("fehler", "Parakeet: \(error.localizedDescription)")
                completion(.failure(error))
            }
        }
    }

    /// Mitschrift aus 16-kHz-Mono-Samples (vom Diktat), ganz auf diesem Mac.
    func transkribiere(_ samples: [Float]) async throws -> String {
        try await withCheckedThrowingContinuation { (k: CheckedContinuation<String, Error>) in
            queue.async {
                do {
                    let r = try self.loadIfNeeded()
                    let start = CFAbsoluteTimeGetCurrent()
                    var parts: [String] = []
                    for chunk in Self.split(samples, maxS: Self.maxChunkS) {
                        var text = Self.decode(r, chunk)
                        if text.isEmpty && chunk.count > Self.retryChunkS * Self.sampleRate {
                            text = Self.split(chunk, maxS: Self.retryChunkS).map { Self.decode(r, $0) }
                                .filter { !$0.isEmpty }.joined(separator: " ")
                        }
                        if !text.isEmpty { parts.append(text) }
                    }
                    let ms = Int((CFAbsoluteTimeGetCurrent() - start) * 1000)
                    self.setzeMessung(audioMs: samples.count * 1000 / Self.sampleRate, rechenMs: ms)
                    log("sprache", "Parakeet: \(samples.count * 1000 / Self.sampleRate) ms Aufnahme in \(ms) ms gerechnet")
                    self.scheduleUnload()
                    k.resume(returning: parts.joined(separator: " "))
                } catch {
                    log("fehler", "Parakeet: \(error.localizedDescription)")
                    k.resume(throwing: error)
                }
            }
        }
    }

    /// Gemessene Werte wie in den Handy-Einstellungen (Laden/Aufwärmen, Arbeitsspeicher, letzte Mitschrift).
    struct Messung { var ladeMs = 0; var ramMb = 0; var audioMs = 0; var rechenMs = 0 }
    private let messLock = NSLock()
    private var _messung: Messung?
    var messung: Messung? { messLock.withLock { _messung } }

    private func setzeMessung(ladeMs: Int? = nil, audioMs: Int? = nil, rechenMs: Int? = nil) {
        messLock.withLock {
            var m = _messung ?? Messung()
            if let ladeMs { m.ladeMs = ladeMs; m.ramMb = Self.arbeitsspeicherMb() }
            if let audioMs { m.audioMs = audioMs }
            if let rechenMs { m.rechenMs = rechenMs }
            _messung = m
        }
    }

    private static func arbeitsspeicherMb() -> Int {
        var info = mach_task_basic_info()
        var anzahl = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size / MemoryLayout<natural_t>.size)
        let r = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(anzahl)) { task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &anzahl) }
        }
        return r == KERN_SUCCESS ? Int(info.resident_size / 1_048_576) : 0
    }

    /// Belegter Platz der Modelldateien.
    static var belegteBytes: Int64 {
        files.reduce(0) { s, f in
            s + (((try? FileManager.default.attributesOfItem(atPath: modelDir.appendingPathComponent(f.name).path)[.size] as? NSNumber)??.int64Value) ?? 0)
        }
    }

    // MARK: - Laden

    private func loadIfNeeded() throws -> OpaquePointer {
        if let r = recognizer { return r }
        guard Self.isInstalled else { throw ParakeetError.notInstalled }
        let dir = Self.modelDir
        let start = CFAbsoluteTimeGetCurrent()
        var owned: [UnsafeMutablePointer<CChar>] = []
        func c(_ s: String) -> UnsafePointer<CChar> {
            let p = strdup(s)!
            owned.append(p)
            return UnsafePointer(p)
        }
        defer { owned.forEach { free($0) } }

        var cfg = SherpaOnnxOfflineRecognizerConfig()
        cfg.feat_config.sample_rate = Int32(Self.sampleRate)
        cfg.feat_config.feature_dim = 128
        cfg.model_config.transducer.encoder = c(dir.appendingPathComponent("encoder.int8.onnx").path)
        cfg.model_config.transducer.decoder = c(dir.appendingPathComponent("decoder.int8.onnx").path)
        cfg.model_config.transducer.joiner = c(dir.appendingPathComponent("joiner.int8.onnx").path)
        cfg.model_config.tokens = c(dir.appendingPathComponent("tokens.txt").path)
        cfg.model_config.num_threads = Self.threads
        cfg.model_config.provider = c("cpu")
        cfg.model_config.model_type = c("nemo_transducer")
        cfg.decoding_method = c("greedy_search")

        guard let r = SherpaOnnxCreateOfflineRecognizer(&cfg) else { throw ParakeetError.loadFailed }
        // Aufwaermen: Die erste echte Mitschrift waere sonst etwa doppelt so langsam.
        for _ in 0..<2 { _ = Self.decode(r, [Float](repeating: 0, count: Self.sampleRate)) }
        recognizer = r
        let ladeMs = Int((CFAbsoluteTimeGetCurrent() - start) * 1000)
        setzeMessung(ladeMs: ladeMs)
        log("sprache", "Parakeet geladen in \(ladeMs) ms (\(Self.threads) Threads)")
        return r
    }

    private func scheduleUnload() {
        idleWork?.cancel()
        let work = DispatchWorkItem { [weak self] in
            guard let self = self, let r = self.recognizer else { return }
            SherpaOnnxDestroyOfflineRecognizer(r)
            self.recognizer = nil
            log("sprache", "Parakeet entladen (Ruhe)")
        }
        idleWork = work
        queue.asyncAfter(deadline: .now() + Self.idleUnloadS, execute: work)
    }

    // MARK: - Rechnen

    private static func decode(_ r: OpaquePointer, _ chunk: [Float]) -> String {
        var peak: Float = 0
        for v in chunk { peak = max(peak, abs(v)) }
        var input = chunk
        if peak > 0 && peak < targetPeak {
            let gain = min(targetPeak / peak, maxGain)
            input = chunk.map { $0 * gain }
        }
        guard let stream = SherpaOnnxCreateOfflineStream(r) else { return "" }
        defer { SherpaOnnxDestroyOfflineStream(stream) }
        input.withUnsafeBufferPointer { buf in
            SherpaOnnxAcceptWaveformOffline(stream, Int32(sampleRate), buf.baseAddress, Int32(buf.count))
        }
        SherpaOnnxDecodeOfflineStream(r, stream)
        guard let result = SherpaOnnxGetOfflineStreamResult(stream) else { return "" }
        defer { SherpaOnnxDestroyOfflineRecognizerResult(result) }
        guard let text = result.pointee.text else { return "" }
        return String(cString: text).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// WAV lesen, auf mono 16 kHz Float bringen.
    private static func readSamples(_ url: URL) throws -> [Float] {
        guard let file = try? AVAudioFile(forReading: url),
              let target = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: Double(sampleRate),
                                         channels: 1, interleaved: false),
              let inBuf = AVAudioPCMBuffer(pcmFormat: file.processingFormat,
                                           frameCapacity: AVAudioFrameCount(file.length)) else {
            throw ParakeetError.audioUnreadable
        }
        try file.read(into: inBuf)
        let src = file.processingFormat
        if src.sampleRate == target.sampleRate && src.channelCount == 1, let d = inBuf.floatChannelData {
            return Array(UnsafeBufferPointer(start: d[0], count: Int(inBuf.frameLength)))
        }
        guard let conv = AVAudioConverter(from: src, to: target),
              let outBuf = AVAudioPCMBuffer(pcmFormat: target, frameCapacity:
                AVAudioFrameCount(Double(inBuf.frameLength) * target.sampleRate / src.sampleRate) + 1024) else {
            throw ParakeetError.audioUnreadable
        }
        var given = false
        var err: NSError?
        conv.convert(to: outBuf, error: &err) { _, status in
            if given { status.pointee = .endOfStream; return nil }
            given = true
            status.pointee = .haveData
            return inBuf
        }
        guard err == nil, let d = outBuf.floatChannelData else { throw ParakeetError.audioUnreadable }
        return Array(UnsafeBufferPointer(start: d[0], count: Int(outBuf.frameLength)))
    }

    /// Stuecke von hoechstens maxS Sekunden, geschnitten an der leisesten Stelle kurz vor dem Ende.
    private static func split(_ samples: [Float], maxS: Int) -> [[Float]] {
        let maxN = maxS * sampleRate
        let searchN = min(15, maxS / 2) * sampleRate
        let winN = sampleRate * 2 / 5
        var parts: [[Float]] = []
        var begin = 0
        while samples.count - begin > maxN {
            var best = begin + maxN - searchN
            var bestEnergy = Float.greatestFiniteMagnitude
            var p = best
            while p + winN <= begin + maxN {
                var e: Float = 0
                for i in p..<(p + winN) { e += samples[i] * samples[i] }
                if e < bestEnergy { bestEnergy = e; best = p }
                p += winN
            }
            let cut = best + winN / 2
            parts.append(Array(samples[begin..<cut]))
            begin = cut
        }
        parts.append(Array(samples[begin...]))
        return parts
    }

    // MARK: - Download

    /// Laedt die Modelldateien der festgelegten Revision (einmalig, rund 640 MB). Fertige Dateien werden
    /// uebersprungen. progress(geladen, gesamt) und completion kommen auf dem Main-Thread, im Modus .common:
    /// Das Einstellungsfenster laeuft modal, ein schlichtes DispatchQueue.main.async kaeme erst nach dem Schliessen an.
    static func download(progress: @escaping (Int64, Int64) -> Void,
                         completion: @escaping (Error?) -> Void) {
        Task.detached {
            do {
                try FileManager.default.createDirectory(at: modelDir, withIntermediateDirectories: true)
                var done: Int64 = 0
                for f in files {
                    let target = modelDir.appendingPathComponent(f.name)
                    let have = (try? FileManager.default.attributesOfItem(atPath: target.path)[.size] as? NSNumber)??.int64Value
                    if have == f.size { done += f.size; continue }
                    let url = URL(string: "https://huggingface.co/\(repo)/resolve/\(revision)/\(f.name)")!
                    let base = done
                    let tmp = try await DownloadWithProgress().run(url) { n in
                        RunLoop.main.perform(inModes: [.common]) { progress(base + n, totalBytes) }
                    }
                    let size = (try FileManager.default.attributesOfItem(atPath: tmp.path)[.size] as? NSNumber)?.int64Value
                    guard size == f.size else {
                        try? FileManager.default.removeItem(at: tmp)
                        throw ParakeetError.download("\(f.name) unvollständig")
                    }
                    try? FileManager.default.removeItem(at: target)
                    try FileManager.default.moveItem(at: tmp, to: target)
                    done += f.size
                }
                log("sprache", "Parakeet-Modell geladen (\(totalBytes / 1_048_576) MB)")
                RunLoop.main.perform(inModes: [.common]) { completion(nil) }
            } catch {
                RunLoop.main.perform(inModes: [.common]) { completion(error) }
            }
        }
    }
}

/// Ein Download mit Fortschritt; die Datei landet erst als Ganzes im Zielordner.
private final class DownloadWithProgress: NSObject, URLSessionDownloadDelegate {
    private var onProgress: ((Int64) -> Void)?
    private var continuation: CheckedContinuation<URL, Error>?

    func run(_ url: URL, onProgress: @escaping (Int64) -> Void) async throws -> URL {
        self.onProgress = onProgress
        let session = URLSession(configuration: .default, delegate: self, delegateQueue: nil)
        defer { session.finishTasksAndInvalidate() }
        return try await withCheckedThrowingContinuation { cont in
            continuation = cont
            session.downloadTask(with: url).resume()
        }
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didWriteData bytesWritten: Int64,
                    totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        onProgress?(totalBytesWritten)
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        if let http = downloadTask.response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            continuation?.resume(throwing: ParakeetLocalClient.ParakeetError.download("HTTP \(http.statusCode)"))
            continuation = nil
            return
        }
        // Die Systemdatei verschwindet nach diesem Aufruf: sofort in den Zielordner unter eigenem Namen.
        let keep = ParakeetLocalClient.modelDir.appendingPathComponent("dl-\(UUID().uuidString).part")
        do {
            try FileManager.default.moveItem(at: location, to: keep)
            continuation?.resume(returning: keep)
        } catch {
            continuation?.resume(throwing: error)
        }
        continuation = nil
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            continuation?.resume(throwing: error)
            continuation = nil
        }
    }
}

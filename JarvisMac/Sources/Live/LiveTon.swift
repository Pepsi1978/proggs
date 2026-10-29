import AVFoundation
import Foundation

/// Ton des Live-Gesprächs am Mac: Mikrofon mit echter Echounterdrückung (Voice Processing von Apple) und lückenlose
/// Wiedergabe der 24-kHz-Antworten. Werte für Polster, Kappen und Hörbar-Schwellen aus der Handy-App
/// (best-practices/apis/openai-live-realtime.md §14.3): Polster 250 ms nur hinter Stille, Kappen überschüssiger Stille
/// erst in Pausen ab 600 ms, hörbar ab Effektivwert 120, „ganz still“ unter Spitze 200.
final class LiveTon: @unchecked Sendable {
    static let ausgabeRate: Double = 24_000
    static let startPolsterMs = 250
    static let neuerSatzMs: Int64 = 300
    static let kappenPauseMs: Int64 = 600
    static let kappenUeberMs = 30
    static let stilleSpitze = 200
    static let hoerbarEffektiv: Double = 120
    static let blockMs = 100

    let eingabeRate: Double
    /// PCM16 mono in `eingabeRate`, Blöcke zu 100 ms. Läuft im Audio-Faden.
    var beiMikro: ((Data) -> Void)?
    /// Mikrofon stumm (Stille statt Mikrofon an den Dienst), z. B. während eines späten Vorlesens.
    var mikroStumm = false

    private var engine = AVAudioEngine()
    private var player = AVAudioPlayerNode()
    /// Ohne Apples Echounterdrückung (Rückfall, wenn Voice Processing auf diesem Gerät nicht startet):
    /// dann ist das Mikrofon stumm, solange Jarvis hörbar spricht (Echosperre wie am Handy).
    private(set) var ohneEchoUnterdrueckung = false
    private let ausFormat = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: LiveTon.ausgabeRate, channels: 1, interleaved: false)!
    private var zielFormat: AVAudioFormat
    private var wandler: AVAudioConverter?
    private var sammel = Data()
    private let lock = NSLock()
    private var gepuffertFrames = 0
    private var generation = 0
    private var letzteHoerbarMs: Int64 = 0
    private var hoerbarBisMs: Int64 = 0
    private var _pegelEin: Float = 0
    private var _pegelAus: Float = 0
    private var laeuft = false
    private var beobachter: NSObjectProtocol?
    private let tonFaden = DispatchQueue(label: "jarvis.live.ton", qos: .userInteractive)

    init(eingabeRate: Double) {
        self.eingabeRate = eingabeRate
        zielFormat = AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: eingabeRate, channels: 1, interleaved: true)!
    }

    static func jetztMs() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    var pegelEin: Float { lock.lock(); defer { lock.unlock() }; return _pegelEin }
    var pegelAus: Float {
        lock.lock(); defer { lock.unlock() }
        return gepuffertFrames == 0 ? 0 : _pegelAus
    }
    /// Hörbarer Ton liegt noch im Puffer oder wird gerade gespielt.
    var sprichtHoerbar: Bool { lock.lock(); defer { lock.unlock() }; return Self.jetztMs() < hoerbarBisMs }
    /// Noch nicht abgespielte Frames (24 kHz).
    var gepuffert: Int { lock.lock(); defer { lock.unlock() }; return gepuffertFrames }

    // MARK: Start / Stopp

    /// Startet den Ton. Voice Processing scheitert am Mac gelegentlich mit -10875 (kAudioUnitErr_FailedInitialization),
    /// etwa wenn das Weckwort das Mikrofon gerade erst freigibt oder Ein- und Ausgabegerät nicht zusammenpassen
    /// (Bluetooth, externer Monitor). Deshalb: zweiter Versuch mit frischer Engine, danach ohne Voice Processing mit Echosperre.
    func starten() async throws {
        var letzterFehler: Error?
        for versuch in 0..<3 {
            if versuch > 0 {
                engine.inputNode.removeTap(onBus: 0)
                engine.stop()
                engine = AVAudioEngine()
                player = AVAudioPlayerNode()
                try? await Task.sleep(for: .milliseconds(400))   // nicht den Hauptfaden blockieren
            }
            do {
                try startenIntern(sprachverarbeitung: versuch < 2)
                ohneEchoUnterdrueckung = versuch == 2
                if versuch > 0 { log("live", "Ton im \(versuch + 1). Versuch gestartet\(versuch == 2 ? " – ohne Echounterdrückung, Echosperre aktiv" : "")") }
                return
            } catch {
                letzterFehler = error
                log("fehler", "Live: Tonstart Versuch \(versuch + 1) fehlgeschlagen: \(error.localizedDescription)")
            }
        }
        throw letzterFehler ?? NSError(domain: "JarvisLive", code: 2)
    }

    private func startenIntern(sprachverarbeitung: Bool) throws {
        let eingang = engine.inputNode
        if sprachverarbeitung {
            try eingang.setVoiceProcessingEnabled(true)
            if #available(macOS 14.0, *) {
                eingang.voiceProcessingOtherAudioDuckingConfiguration = .init(enableAdvancedDucking: false, duckingLevel: .min)
            }
        }
        _ = engine.outputNode   // Voice Processing braucht Ein- und Ausgabe in derselben Einheit
        engine.attach(player)
        engine.connect(player, to: engine.mainMixerNode, format: ausFormat)
        try abgriffSetzen()
        engine.prepare()
        try engine.start()
        player.play()
        laeuft = true
        // 250 ms Stille vorlegen, damit der Ausgang nicht leer startet (§14.2 Nr. 7).
        planeStille(ms: Self.startPolsterMs)
        beobachter = NotificationCenter.default.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: nil) { [weak self] _ in
            self?.tonFaden.async { self?.neuStarten() }
        }
    }

    private func abgriffSetzen() throws {
        let eingang = engine.inputNode
        eingang.removeTap(onBus: 0)
        let format = eingang.outputFormat(forBus: 0)
        guard format.sampleRate > 0, format.channelCount > 0 else {
            throw NSError(domain: "JarvisLive", code: 1, userInfo: [NSLocalizedDescriptionKey: "Kein Mikrofon verfügbar."])
        }
        let w = AVAudioConverter(from: format, to: zielFormat)
        w?.downmix = true
        wandler = w
        let blockBytes = Int(eingabeRate) * Self.blockMs / 1000 * 2
        eingang.installTap(onBus: 0, bufferSize: AVAudioFrameCount(format.sampleRate / 20), format: format) { [weak self] puffer, _ in
            guard let self, let w = self.wandler else { return }
            let kapazitaet = AVAudioFrameCount(Double(puffer.frameLength) * self.eingabeRate / format.sampleRate) + 32
            guard let aus = AVAudioPCMBuffer(pcmFormat: self.zielFormat, frameCapacity: kapazitaet) else { return }
            var geliefert = false
            var fehler: NSError?
            w.convert(to: aus, error: &fehler) { _, status in
                if geliefert { status.pointee = .noDataNow; return nil }
                geliefert = true
                status.pointee = .haveData
                return puffer
            }
            guard fehler == nil, aus.frameLength > 0, let p = aus.int16ChannelData?[0] else { return }
            let n = Int(aus.frameLength)
            var summe: Double = 0
            for i in 0..<n { let s = Double(p[i]); summe += s * s }
            let rms = sqrt(summe / Double(n))
            self.lock.lock(); self._pegelEin = Float(min(1, rms / 6000)); self.lock.unlock()
            var daten = Data(bytes: p, count: n * 2)
            if self.mikroStumm || (self.ohneEchoUnterdrueckung && self.sprichtHoerbar) { daten = Data(count: daten.count) }
            self.sammel.append(daten)
            while self.sammel.count >= blockBytes {
                let block = self.sammel.prefix(blockBytes)
                self.sammel.removeFirst(blockBytes)
                self.beiMikro?(Data(block))
            }
        }
    }

    private func neuStarten() {
        guard laeuft else { return }
        log("live", "Audiogerät gewechselt, Ton wird neu verbunden")
        engine.stop()
        do {
            try abgriffSetzen()
            engine.prepare()
            try engine.start()
            player.play()
        } catch {
            log("fehler", "Live: Ton nach Gerätewechsel nicht wieder gestartet: \(error.localizedDescription)")
        }
    }

    func stoppen() {
        laeuft = false
        if let b = beobachter { NotificationCenter.default.removeObserver(b); beobachter = nil }
        beiMikro = nil
        engine.inputNode.removeTap(onBus: 0)
        lock.lock(); generation += 1; gepuffertFrames = 0; hoerbarBisMs = 0; lock.unlock()
        player.stop()
        engine.stop()
        try? engine.inputNode.setVoiceProcessingEnabled(false)
    }

    // MARK: Wiedergabe

    /// Ein Stück Antwort (PCM16 mono 24 kHz). [kappenErlaubt]: GPT-Live liefert auch in Pausen Stille in Echtzeit; die darf
    /// gekappt werden, wenn sich ein Polster aufgestaut hat.
    func spiele(_ pcm: Data, kappenErlaubt: Bool = true) {
        guard laeuft, pcm.count >= 2 else { return }
        let frames = pcm.count / 2
        var summe: Double = 0
        var spitze = 0
        pcm.withUnsafeBytes { roh in
            let s = roh.bindMemory(to: Int16.self)
            for i in 0..<frames {
                let v = Int(s[i]); let a = abs(v)
                if a > spitze { spitze = a }
                summe += Double(v * v)
            }
        }
        let rms = sqrt(summe / Double(frames))
        let hoerbar = rms >= Self.hoerbarEffektiv
        let still = spitze < Self.stilleSpitze
        let jetzt = Self.jetztMs()

        lock.lock()
        let gepuffertMs = gepuffertFrames * 1000 / Int(Self.ausgabeRate)
        if kappenErlaubt && !hoerbar && still && gepuffertMs > Self.startPolsterMs + Self.kappenUeberMs && jetzt - letzteHoerbarMs > Self.kappenPauseMs {
            lock.unlock()
            return
        }
        let polster = hoerbar && jetzt - letzteHoerbarMs >= Self.neuerSatzMs && gepuffertMs < Self.startPolsterMs - 40
            ? Self.startPolsterMs - gepuffertMs : 0
        lock.unlock()
        if polster > 0 { planeStille(ms: polster) }
        plane(pcm, frames: frames, pegel: Float(min(1, rms / 5000)))
        if hoerbar {
            lock.lock()
            letzteHoerbarMs = jetzt
            hoerbarBisMs = jetzt + Int64(gepuffertFrames * 1000 / Int(Self.ausgabeRate)) + 150
            lock.unlock()
        }
    }

    private func planeStille(ms: Int) {
        let frames = Int(Self.ausgabeRate) * ms / 1000
        plane(Data(count: frames * 2), frames: frames, pegel: 0)
    }

    private func plane(_ pcm: Data, frames: Int, pegel: Float) {
        guard let puffer = AVAudioPCMBuffer(pcmFormat: ausFormat, frameCapacity: AVAudioFrameCount(frames)), let ziel = puffer.floatChannelData?[0] else { return }
        puffer.frameLength = AVAudioFrameCount(frames)
        pcm.withUnsafeBytes { roh in
            let s = roh.bindMemory(to: Int16.self)
            for i in 0..<frames { ziel[i] = Float(s[i]) / 32768 }
        }
        lock.lock()
        let gen = generation
        gepuffertFrames += frames
        lock.unlock()
        player.scheduleBuffer(puffer, completionCallbackType: .dataPlayedBack) { [weak self] _ in
            guard let self else { return }
            self.lock.lock()
            if gen == self.generation {
                self.gepuffertFrames = max(0, self.gepuffertFrames - frames)
                self._pegelAus = pegel
            }
            self.lock.unlock()
        }
    }

    /// Wiedergabe sofort leeren (Frank unterbricht).
    func leeren() {
        lock.lock()
        generation += 1
        gepuffertFrames = 0
        hoerbarBisMs = 0
        _pegelAus = 0
        lock.unlock()
        player.stop()
        if laeuft { player.play() }
    }
}

import AVFoundation
import AppKit

/// Aufnahme-Signaltoene — 1:1-Portierung von
/// `TerminalVoiceOverlay-Windows/Services/RecordingCuePlayer.cs`.
///
/// macOS spielte bisher nur `NSSound.beep()` (Start: ein Systempiep, Stop:
/// zwei). Das klang anders als unter Windows, war lautstaerkeabhaengig vom
/// System-Alertton und liess sich nicht vom „echten" Fehler-Piep unterscheiden.
/// Jetzt werden dieselben Toene erzeugt wie unter Windows:
///
/// * **Start** — 880 Hz, 150 ms
/// * **Stop**  — 660 Hz, 120 ms, danach 440 Hz, 120 ms
///
/// Gleiche Amplitude (0,22) und gleiche kurze Ein-/Ausblendung wie im
/// C#-Original, damit es an beiden Rechnern identisch klingt. Die Blende
/// verhindert das Knacken, das ein hart abgeschnittener Sinus erzeugt.
///
/// Robustheit (Bug-Almanach `bugs/desktop/swift-appkit.md` §E4/§E5): die
/// Puffer werden immer in der LIVE-Hardware-Abtastrate gebaut, nie in einer
/// fest verdrahteten. Wechselt das Ausgabegeraet (AirPods rein/raus), meldet
/// AVAudioEngine `configurationChange` — dann wird die Engine samt Puffern neu
/// aufgebaut. Schlaegt irgendetwas fehl, faellt der Ton auf `NSSound.beep()`
/// zurueck; ein Signalton darf nie die Aufnahme verhindern.
final class RecordingCuePlayer {

    /// Amplitude wie im Windows-Original (0,22 von Vollaussteuerung) — laut
    /// genug zum Hoeren, leise genug um nicht zu erschrecken.
    private static let amplitude: Float = 0.22

    /// Toene je Signal: (Frequenz in Hz, Dauer in ms). Exakt die Werte aus
    /// `RecordingCuePlayer.cs`.
    private static let startTones: [(Double, Int)] = [(880, 150)]
    private static let stopTones:  [(Double, Int)] = [(660, 120), (440, 120)]

    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()

    /// Serialisiert Aufbau und Abspielen — `playStart`/`playStop` koennen aus
    /// verschiedenen Threads kommen (Hotkey-Monitor, Overlay-Klick).
    private let lock = NSLock()

    private var startBuffer: AVAudioPCMBuffer?
    private var stopBuffer: AVAudioPCMBuffer?
    /// Abtastrate, fuer die die Puffer gebaut wurden — aendert sie sich, muss
    /// alles neu erzeugt werden.
    private var preparedSampleRate: Double = 0
    private var ready = false

    init() {
        // Geraetewechsel (AirPods, USB-Interface, Monitor-Lautsprecher) macht
        // die vorbereitete Engine ungueltig. Beim naechsten Ton neu aufbauen.
        NotificationCenter.default.addObserver(
            forName: .AVAudioEngineConfigurationChange,
            object: engine, queue: nil) { [weak self] _ in
                guard let self = self else { return }
                self.lock.lock()
                self.ready = false
                self.lock.unlock()
            }
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
        if engine.isRunning { engine.stop() }
    }

    /// Kurzer hoher Ton beim Start der Aufnahme.
    func playStart() { play(startBuffer: true) }

    /// Absteigendes Zwei-Ton-Signal beim Stopp der Aufnahme.
    func playStop() { play(startBuffer: false) }

    // MARK: - Intern

    private func play(startBuffer wantStart: Bool) {
        lock.lock()
        defer { lock.unlock() }
        do {
            try prepareIfNeeded()
            guard let buffer = wantStart ? self.startBuffer : self.stopBuffer else {
                throw CueError.bufferMissing
            }
            // `.interrupts` statt anhaengen: druecke ich zweimal schnell
            // hintereinander, soll der neue Ton den alten ersetzen und nicht
            // dahinter in einer Warteschlange auflaufen.
            player.scheduleBuffer(buffer, at: nil, options: [.interrupts], completionHandler: nil)
            if !player.isPlaying { player.play() }
        } catch {
            NSLog("RecordingCuePlayer: Ton fehlgeschlagen (%@) — Fallback auf Systempiep.",
                  String(describing: error))
            // Beim naechsten Mal komplett neu aufbauen, statt dauerhaft kaputt
            // zu bleiben (selbstheilend wie die Windows-Fassung).
            ready = false
            NSSound.beep()
        }
    }

    private enum CueError: Error {
        case bufferMissing
        case formatUnavailable
    }

    /// Baut Engine und Puffer auf, falls noch nicht geschehen oder falls sich
    /// die Hardware-Abtastrate geaendert hat.
    private func prepareIfNeeded() throws {
        let outputFormat = engine.outputNode.outputFormat(forBus: 0)
        let sampleRate = outputFormat.sampleRate
        guard sampleRate > 0 else { throw CueError.formatUnavailable }

        if ready && engine.isRunning && sampleRate == preparedSampleRate { return }

        if engine.isRunning { engine.stop() }
        if player.engine == nil { engine.attach(player) }

        // Mono-Float-Format in der Rate der Hardware. AVAudioEngine mischt
        // selbst auf die Kanalzahl des Ausgabegeraets hoch.
        guard let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1) else {
            throw CueError.formatUnavailable
        }
        engine.connect(player, to: engine.mainMixerNode, format: format)

        startBuffer = Self.makeBuffer(tones: Self.startTones, format: format)
        stopBuffer  = Self.makeBuffer(tones: Self.stopTones,  format: format)

        engine.prepare()
        try engine.start()
        preparedSampleRate = sampleRate
        ready = true
    }

    /// Erzeugt einen PCM-Puffer aus einer Tonfolge — Swift-Pendant zu
    /// `CreatePcm` in der C#-Fassung, inklusive der 5-ms-Ein-/Ausblendung.
    private static func makeBuffer(tones: [(Double, Int)],
                                   format: AVAudioFormat) -> AVAudioPCMBuffer? {
        let sampleRate = format.sampleRate
        let total = tones.reduce(0) { $0 + Int(sampleRate * Double($1.1) / 1000.0) }
        guard total > 0,
              let buffer = AVAudioPCMBuffer(pcmFormat: format,
                                            frameCapacity: AVAudioFrameCount(total)),
              let channel = buffer.floatChannelData?[0] else { return nil }

        var offset = 0
        for (frequency, durationMs) in tones {
            let toneSamples = Int(sampleRate * Double(durationMs) / 1000.0)
            // Windows: fadeSamples = min(SampleRate/200, toneSamples/2) — also
            // 5 ms Blende, bei sehr kurzen Toenen hoechstens die halbe Laenge.
            let fadeSamples = max(1, min(Int(sampleRate / 200), toneSamples / 2))
            for i in 0..<toneSamples {
                let edge = Double(min(i + 1, toneSamples - i))
                let fade = min(1.0, edge / Double(fadeSamples))
                let value = sin(2 * Double.pi * frequency * Double(i) / sampleRate)
                channel[offset] = Float(value * Double(amplitude) * fade)
                offset += 1
            }
        }
        buffer.frameLength = AVAudioFrameCount(offset)
        return buffer
    }
}

import AVFoundation
import CoreAudio
import Foundation

final class AudioRecorder {
    private var audioEngine: AVAudioEngine?
    private var outputFile: AVAudioFile?
    private var tempURL: URL?
    private let lock = NSLock()
    private var _isRecording = false

    /// Kleinste brauchbare WAV-Groesse: 44 Byte Header + 320 Byte PCM (= 10 ms bei
    /// 16 kHz mono). 1:1 Windows (`MinUsableWavBytes = 44 + 320`). Alles darunter ist
    /// kein Ton, sondern ein Fehlstart — Groq bekaeme daraus nur Halluzinationen.
    private static let minUsableWavBytes = 44 + 320

    /// Zeitpunkt des letzten Pegel-Callbacks. Der Watchdog erkennt daran ein
    /// Mikrofon, das zwar "laeuft", aber keine Buffer mehr liefert.
    private var lastLevelTime: CFAbsoluteTime = 0
    private var levelWatchdog: Timer?
    private var stallLogged = false
    /// Ab dieser Stille im Buffer-Strom gilt die Aufnahme als haengend
    /// (Windows: `sinceMs > 2500`).
    private static let levelStallSeconds: CFAbsoluteTime = 2.5

    /// Wird gerufen, wenn waehrend einer laufenden Aufnahme ueber Sekunden kein
    /// einziger Audio-Buffer mehr ankommt. Der AppDelegate kann den Benutzer damit
    /// warnen, statt ihn ins Leere sprechen zu lassen.
    var onCaptureStalled: (() -> Void)?

    /// Wird pro Audio-Buffer (~100 ms) mit dem Peak-Level 0..1 aufgerufen.
    /// Pendant zu Windows `LevelChanged` (NAudio). Aktiviert die Waveform-
    /// Animation in OverlayPanel.
    var onLevel: ((Float) -> Void)?

    var isRecording: Bool {
        lock.lock()
        defer { lock.unlock() }
        return _isRecording
    }

    /// Startet die Aufnahme. Schlaegt der Start fehl, wird er bis zu dreimal wiederholt und ab
    /// dem zweiten Anlauf das Eingabegeraet ausdruecklich neu an die Engine gebunden.
    ///
    /// Der Grund: `AVAudioEngine.start()` scheitert reproduzierbar mit einem rohen CoreAudio-Code
    /// (etwa 2003329396), wenn der Ton-Stack sich gerade neu sortiert - Geraetewechsel, ein
    /// zweites Programm, das im selben Moment das Mikrofon greift, oder ein Standardgeraet, das
    /// CoreAudio im Prozess noch zwischengespeichert hat. Bisher gab der Recorder in genau diesem
    /// Fall sofort auf und meldete "Mikrofon nicht verfuegbar", obwohl das Geraet da war und der
    /// naechste Anlauf gereicht haette. Ein Anlauf allein ist deshalb zu wenig.
    func start() throws {
        // Doppelstart verhindern: der Recorder ist prozessweit geteilt. Ohne
        // diese Pruefung baut ein zweiter Aufrufer eine NEUE Engine auf,
        // ueberschreibt outputFile/tempURL und die bereits laufende Aufnahme
        // schreibt ins Leere — der bis dahin gesprochene Text ist weg.
        lock.lock()
        let busy = _isRecording
        lock.unlock()
        if busy { throw RecorderError.alreadyRecording }

        // Ohne erteilte Berechtigung scheitert jeder Anlauf gleich - dann lieber sofort sagen,
        // was zu tun ist, statt dreimal vergeblich zu starten und eine CoreAudio-Nummer zu zeigen.
        switch AVCaptureDevice.authorizationStatus(for: .audio) {
        case .denied, .restricted:
            throw RecorderError.permissionDenied
        case .notDetermined:
            // Noch nie gefragt (oder der Eintrag wurde zurueckgesetzt). Hier AKTIV nachfragen und
            // auf die Antwort warten. Ohne diesen Schritt laeuft der Start einfach weiter und
            // `AVAudioEngine.start()` scheitert mit dem nichtssagenden CoreAudio-Code 'what' -
            // ein Dialog erscheint dabei NICHT von allein, AVAudioEngine loest ihn nicht aus.
            DiagLog.write("Audio", "permission_request")
            let semaphore = DispatchSemaphore(value: 0)
            var granted = false
            AVCaptureDevice.requestAccess(for: .audio) { ok in
                granted = ok
                semaphore.signal()
            }
            // Grosszuegiges Zeitfenster: der Benutzer muss den Systemdialog erst sehen und klicken.
            _ = semaphore.wait(timeout: .now() + 60)
            DiagLog.write("Audio", "permission_result", [("granted", granted)])
            if !granted { throw RecorderError.permissionDenied }
        default:
            break
        }

        var lastError: Error = RecorderError.noInputDevice
        for attempt in 0..<3 {
            do {
                DiagLog.write("Audio", "start_attempt", [("attempt", attempt)])
                // Erst im DRITTEN Anlauf das Geraet erzwingen: die ersten beiden versuchen es mit
                // einer frischen Engine, was den haeufigsten Fall (Ton-Stack sortiert sich gerade
                // neu) ohne den riskanten AudioUnit-Eingriff loest.
                try startEngineOnce(forceRebind: attempt >= 2)
                if attempt > 0 {
                    NSLog("AudioRecorder: Aufnahme im %d. Anlauf gestartet", attempt + 1)
                }
                return
            } catch RecorderError.alreadyRecording {
                // Kein Wiederholungsfall: hier laeuft bereits eine Aufnahme.
                throw RecorderError.alreadyRecording
            } catch {
                lastError = error
                NSLog("AudioRecorder: Start-Versuch %d fehlgeschlagen: %@",
                      attempt + 1, error.localizedDescription)
                DiagLog.warn("Audio", "start_attempt_failed",
                             [("attempt", attempt), ("err", error.localizedDescription),
                              ("code", (error as NSError).code)])
                // Kurz warten: bei einem Geraetewechsel braucht CoreAudio einen Moment, bis das
                // neue Standardgeraet bereitsteht. Ohne Pause scheitern alle drei Anlaeufe gleich.
                if attempt < 2 { Thread.sleep(forTimeInterval: 0.25) }
            }
        }
        throw lastError
    }

    /// Ein einzelner Startversuch: Engine bauen, Tap setzen, starten.
    /// - Parameter forceRebind: Eingabegeraet ausdruecklich an die Engine binden, auch wenn ihr
    ///   Format plausibel aussieht. Genau dieser Fall rettet den zweiten Anlauf: CoreAudio kann ein
    ///   verschwundenes Geraet mit gueltig wirkendem Format zwischenspeichern.
    private func startEngineOnce(forceRebind: Bool) throws {
        // Gibt es ueberhaupt ein Eingabegeraet? Ohne diese Pruefung meldet der Recorder bei
        // abgezogenem Mikrofon nur "Audio-Converter konnte nicht erstellt werden" - eine
        // Meldung, aus der niemand auf ein fehlendes Geraet schliesst.
        guard let defaultInput = Self.defaultInputDeviceID() else {
            throw RecorderError.noInputDevice
        }

        let engine = AVAudioEngine()
        let inputNode = engine.inputNode

        // Wechselt das Standard-Eingabegeraet waehrend der Laufzeit (Bluetooth-Kopfhoerer weg,
        // Dock abgezogen, Ton-Neustart), liefert eine frisch erzeugte Engine trotzdem ein
        // leeres Format - CoreAudio haelt in dem Prozess noch das alte, verschwundene Geraet.
        // Ohne die folgende Neuzuweisung bleibt die Aufnahme bis zum Neustart der App tot:
        // genau das Symptom "kein Mikrofon vorhanden", obwohl das Geraet im System da ist.
        var recordingFormat = inputNode.outputFormat(forBus: 0)
        let formatInvalid = recordingFormat.sampleRate <= 0 || recordingFormat.channelCount == 0
        // Das Geraet NUR dann ausdruecklich binden, wenn das Format tatsaechlich unbrauchbar ist
        // (oder ein vorheriger Anlauf gescheitert ist). `AudioUnitSetProperty` auf einer bereits
        // benutzten AudioUnit ist selbst ein Ausloeser fuer 'what' — beim Wiederholversuch reicht
        // in aller Regel schon die frisch gebaute Engine.
        if formatInvalid || forceRebind {
            DiagLog.write("Audio", "rebind_input_device",
                          [("reason", formatInvalid ? "format_invalid" : "retry"),
                           ("device", defaultInput)])
            Self.bindInputDevice(defaultInput, to: inputNode)
            recordingFormat = inputNode.outputFormat(forBus: 0)
        }
        guard recordingFormat.sampleRate > 0, recordingFormat.channelCount > 0 else {
            throw RecorderError.noInputDevice
        }

        // Ziel-Format der WAV. Abtastrate und Kanalzahl kommen aus der .env
        // (Windows: AUDIO_SAMPLE_RATE / AUDIO_CHANNELS), Vorgabe 16 kHz mono.
        let targetRate = Double(Config.current?.audioSampleRate ?? 16000)
        let targetChannels = AVAudioChannelCount(Config.current?.audioChannels ?? 1)
        guard let wavFormat = AVAudioFormat(commonFormat: .pcmFormatFloat32,
                                            sampleRate: targetRate,
                                            channels: targetChannels,
                                            interleaved: false) else {
            throw RecorderError.formatError
        }

        let tempDir = FileManager.default.temporaryDirectory
        let url = tempDir.appendingPathComponent("recording_\(UUID().uuidString).wav")

        let file = try AVAudioFile(forWriting: url, settings: wavFormat.settings)

        guard let converter = AVAudioConverter(from: recordingFormat, to: wavFormat) else {
            throw RecorderError.converterError
        }

        lock.lock()
        self.tempURL = url
        self.outputFile = file
        lock.unlock()

        inputNode.installTap(onBus: 0, bufferSize: 4096, format: recordingFormat) { [weak self] buffer, _ in
            guard let self = self else { return }

            self.lock.lock()
            let recording = self._isRecording
            let currentFile = self.outputFile
            self.lock.unlock()

            guard recording, let file = currentFile else { return }

            // Watchdog fuettern: solange Buffer ankommen, laeuft die Aufnahme.
            self.lock.lock()
            self.lastLevelTime = CFAbsoluteTimeGetCurrent()
            self.lock.unlock()

            let frameCount = AVAudioFrameCount(
                Double(buffer.frameLength) * wavFormat.sampleRate / recordingFormat.sampleRate
            )
            guard frameCount > 0,
                  let convertedBuffer = AVAudioPCMBuffer(pcmFormat: wavFormat, frameCapacity: frameCount) else { return }

            var error: NSError?
            let status = converter.convert(to: convertedBuffer, error: &error) { _, outStatus in
                outStatus.pointee = .haveData
                return buffer
            }

            if status == .haveData {
                try? file.write(from: convertedBuffer)
            }

            // Peak-Level berechnen fuer die Waveform-Animation (Windows
            // LevelChanged-Pendant). Wir nehmen das ORIGINAL-Buffer (vor
            // der Sample-Rate-Konvertierung), weil das die rohen Mic-Daten
            // sind und genuegend Frames hat.
            if let onLevel = self.onLevel,
               let channelData = buffer.floatChannelData {
                let frames = Int(buffer.frameLength)
                let samples = channelData[0]
                var peak: Float = 0
                for i in 0..<frames {
                    let s = abs(samples[i])
                    if s > peak { peak = s }
                }
                DispatchQueue.main.async {
                    onLevel(min(peak, 1.0))
                }
            }
        }

        // _isRecording wird ERST nach dem erfolgreichen Start gesetzt. Frueher stand das Flag
        // schon vor engine.start(); schlug der Start fehl, blieb es auf true haengen und jeder
        // weitere Versuch scheiterte mit "Es laeuft bereits eine Aufnahme" - der Recorder war
        // fuer den Rest der Sitzung tot, ohne dass je eine Aufnahme lief.
        do {
            // `prepare()` reserviert die Ressourcen des Graphen und bringt die AudioUnits in den
            // startbereiten Zustand. Ohne diesen Schritt scheitert `start()` reproduzierbar mit
            // kAudioUnitErr_CannotDoInCurrentContext (FourCC 'what', 2003329396) — die Engine ist
            // dann schlicht noch nicht so weit. Der Aufruf fehlte hier bisher ganz.
            engine.prepare()
            try engine.start()
            DiagLog.write("Audio", "engine_started", [("rate", recordingFormat.sampleRate),
                                                      ("channels", recordingFormat.channelCount)])
        } catch {
            inputNode.removeTap(onBus: 0)
            lock.lock()
            self.outputFile = nil
            self.tempURL = nil
            self._isRecording = false
            lock.unlock()
            try? FileManager.default.removeItem(at: url)
            let ns = error as NSError
            DiagLog.error("Audio", "engine_start_failed", error,
                          [("domain", ns.domain), ("code", ns.code),
                           ("rate", recordingFormat.sampleRate),
                           ("channels", recordingFormat.channelCount)])
            throw error
        }

        lock.lock()
        self._isRecording = true
        self.lastLevelTime = CFAbsoluteTimeGetCurrent()
        self.stallLogged = false
        lock.unlock()
        self.audioEngine = engine
        startLevelWatchdog()
    }

    // MARK: - Watchdog

    /// Windows-Pendant `WatchdogTick`: prueft im Sekundentakt, ob noch Audio-Buffer
    /// ankommen. Bleiben sie laenger als `levelStallSeconds` aus, obwohl die Aufnahme
    /// laeuft, hat das Eingabegeraet aufgehoert zu liefern (Geraetewechsel, entzogene
    /// Berechtigung, eingeschlafener Ton-Stack). Das wird EINMAL pro Aufnahme gemeldet.
    private func startLevelWatchdog() {
        stopLevelWatchdog()
        let timer = Timer(timeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            self.lock.lock()
            let recording = self._isRecording
            let since = CFAbsoluteTimeGetCurrent() - self.lastLevelTime
            let alreadyLogged = self.stallLogged
            if recording && since > AudioRecorder.levelStallSeconds && !alreadyLogged {
                self.stallLogged = true
            }
            self.lock.unlock()

            guard recording else { return }
            if since > AudioRecorder.levelStallSeconds && !alreadyLogged {
                NSLog("AudioRecorder: seit %.1f s kein Audio-Buffer mehr - Aufnahme haengt.", since)
                DiagLog.warn("Audio", "capture_level_stalled", [("silentMs", Int(since * 1000))])
                DispatchQueue.main.async { self.onCaptureStalled?() }
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        levelWatchdog = timer
    }

    private func stopLevelWatchdog() {
        levelWatchdog?.invalidate()
        levelWatchdog = nil
    }

    // MARK: - Eingabegeraet

    /// ID des aktuellen Standard-Eingabegeraets, oder nil wenn das System keines meldet.
    private static func defaultInputDeviceID() -> AudioDeviceID? {
        var address = AudioObjectPropertyAddress(
            mSelector: kAudioHardwarePropertyDefaultInputDevice,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain)
        var deviceID = AudioDeviceID(0)
        var size = UInt32(MemoryLayout<AudioDeviceID>.size)
        let status = AudioObjectGetPropertyData(AudioObjectID(kAudioObjectSystemObject),
                                                &address, 0, nil, &size, &deviceID)
        guard status == noErr, deviceID != kAudioObjectUnknown else { return nil }
        return deviceID
    }

    /// Zwingt die AudioUnit des Eingangsknotens auf ein bestimmtes Geraet. Das ist der einzige
    /// Weg, eine Engine nach einem Geraetewechsel wieder an das echte Mikrofon zu binden.
    private static func bindInputDevice(_ deviceID: AudioDeviceID, to inputNode: AVAudioInputNode) {
        // audioUnit ist optional und im Fehlerfall (Ton-Stack neu gestartet) tatsaechlich nil.
        // Ein Force-Unwrap wuerde die App genau dann abschiessen, wenn sie sich erholen soll.
        guard let unit = inputNode.audioUnit else {
            NSLog("AudioRecorder: Eingangsknoten hat keine AudioUnit - Geraet nicht bindbar")
            return
        }
        var device = deviceID
        let status = AudioUnitSetProperty(unit,
                                          kAudioOutputUnitProperty_CurrentDevice,
                                          kAudioUnitScope_Global,
                                          0,
                                          &device,
                                          UInt32(MemoryLayout<AudioDeviceID>.size))
        if status != noErr {
            NSLog("AudioRecorder: Eingabegeraet konnte nicht gesetzt werden (Status %d)", status)
        }
    }

    func stop() -> URL? {
        stopLevelWatchdog()
        // Signal recording to stop first — the tap closure checks this flag
        lock.lock()
        _isRecording = false
        let engine = self.audioEngine
        let url = self.tempURL
        lock.unlock()

        // Remove the tap before stopping the engine to prevent
        // the tap closure from accessing freed resources
        engine?.inputNode.removeTap(onBus: 0)

        // Small delay to let any in-flight audio callbacks finish
        // before we deallocate the engine and file
        Thread.sleep(forTimeInterval: 0.05)

        engine?.stop()

        lock.lock()
        self.audioEngine = nil
        self.outputFile = nil
        lock.unlock()

        // Zu kurze Datei = Fehlstart (Windows: MinUsableWavBytes). Sie jetzt zu
        // verwerfen ist wichtiger, als sie zu senden: Whisper macht aus 10 ms
        // Rauschen zuverlaessig eine erfundene Floskel.
        guard let url = url else { return nil }
        let attrs = try? FileManager.default.attributesOfItem(atPath: url.path)
        let size = (attrs?[.size] as? Int) ?? 0
        if size < AudioRecorder.minUsableWavBytes {
            NSLog("AudioRecorder: Aufnahme zu kurz (%d Byte) - verworfen.", size)
            DiagLog.warn("Audio", "wav_too_small", [("bytes", size),
                                                    ("minBytes", AudioRecorder.minUsableWavBytes)])
            try? FileManager.default.removeItem(at: url)
            return nil
        }
        DiagLog.write("Audio", "stop_ok", [("bytes", size)])
        return url
    }

    enum RecorderError: Error, LocalizedError {
        case formatError
        case converterError
        /// Es laeuft bereits eine Aufnahme. Der Recorder ist prozessweit
        /// geteilt (Overlay + Prompt-Editor greifen auf DIESELBE Instanz zu) —
        /// ein zweiter Start wuerde die erste Aufnahme verwaisen lassen und
        /// ihren Ton verlieren. Windows-Pendant: `Start()` liefert `false`.
        case alreadyRecording
        /// Das System meldet kein Eingabegeraet - oder CoreAudio haelt noch ein
        /// verschwundenes fest. Eigener Fall, damit die Meldung im Terminal sagt, was
        /// wirklich fehlt, statt auf einen Converter-Fehler auszuweichen.
        case noInputDevice
        /// Die Mikrofon-Berechtigung ist entzogen oder durch eine Richtlinie gesperrt. Ohne sie
        /// scheitert jeder Startversuch mit einer nichtssagenden CoreAudio-Nummer.
        case permissionDenied

        var errorDescription: String? {
            switch self {
            case .formatError: return "Audio-Format konnte nicht erstellt werden"
            case .converterError: return "Audio-Converter konnte nicht erstellt werden"
            case .alreadyRecording: return "Es laeuft bereits eine Aufnahme"
            case .noInputDevice: return "kein Eingabegeraet gefunden (Systemeinstellungen > Ton > Eingabe pruefen)"
            case .permissionDenied: return "keine Mikrofon-Berechtigung (Systemeinstellungen > Datenschutz & Sicherheit > Mikrofon)"
            }
        }
    }
}

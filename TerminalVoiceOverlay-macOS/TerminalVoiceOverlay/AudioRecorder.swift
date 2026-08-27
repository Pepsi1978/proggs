import AVFoundation
import CoreAudio
import Foundation

final class AudioRecorder {
    private var audioEngine: AVAudioEngine?
    private var outputFile: AVAudioFile?
    private var tempURL: URL?
    private let lock = NSLock()
    private var _isRecording = false

    /// Wird pro Audio-Buffer (~100 ms) mit dem Peak-Level 0..1 aufgerufen.
    /// Pendant zu Windows `LevelChanged` (NAudio). Aktiviert die Waveform-
    /// Animation in OverlayPanel.
    var onLevel: ((Float) -> Void)?

    var isRecording: Bool {
        lock.lock()
        defer { lock.unlock() }
        return _isRecording
    }

    func start() throws {
        // Doppelstart verhindern: der Recorder ist prozessweit geteilt. Ohne
        // diese Pruefung baut ein zweiter Aufrufer eine NEUE Engine auf,
        // ueberschreibt outputFile/tempURL und die bereits laufende Aufnahme
        // schreibt ins Leere — der bis dahin gesprochene Text ist weg.
        lock.lock()
        let busy = _isRecording
        lock.unlock()
        if busy { throw RecorderError.alreadyRecording }

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
        if recordingFormat.sampleRate <= 0 || recordingFormat.channelCount == 0 {
            Self.bindInputDevice(defaultInput, to: inputNode)
            recordingFormat = inputNode.outputFormat(forBus: 0)
        }
        guard recordingFormat.sampleRate > 0, recordingFormat.channelCount > 0 else {
            throw RecorderError.noInputDevice
        }

        // Create WAV format: 16kHz mono
        guard let wavFormat = AVAudioFormat(commonFormat: .pcmFormatFloat32,
                                            sampleRate: 16000,
                                            channels: 1,
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
            try engine.start()
        } catch {
            inputNode.removeTap(onBus: 0)
            lock.lock()
            self.outputFile = nil
            self.tempURL = nil
            self._isRecording = false
            lock.unlock()
            try? FileManager.default.removeItem(at: url)
            throw error
        }

        lock.lock()
        self._isRecording = true
        lock.unlock()
        self.audioEngine = engine
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
        var device = deviceID
        let status = AudioUnitSetProperty(inputNode.audioUnit!,
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

        var errorDescription: String? {
            switch self {
            case .formatError: return "Audio-Format konnte nicht erstellt werden"
            case .converterError: return "Audio-Converter konnte nicht erstellt werden"
            case .alreadyRecording: return "Es laeuft bereits eine Aufnahme"
            case .noInputDevice: return "kein Eingabegeraet gefunden (Systemeinstellungen > Ton > Eingabe pruefen)"
            }
        }
    }
}

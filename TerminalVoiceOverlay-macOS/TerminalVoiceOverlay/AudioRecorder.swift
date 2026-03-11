import AVFoundation
import Foundation

final class AudioRecorder {
    private var audioEngine: AVAudioEngine?
    private var outputFile: AVAudioFile?
    private var tempURL: URL?
    private let lock = NSLock()
    private var _isRecording = false

    var isRecording: Bool {
        lock.lock()
        defer { lock.unlock() }
        return _isRecording
    }

    func start() throws {
        let engine = AVAudioEngine()
        let inputNode = engine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)

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
        self._isRecording = true
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
        }

        try engine.start()
        self.audioEngine = engine
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

        var errorDescription: String? {
            switch self {
            case .formatError: return "Audio-Format konnte nicht erstellt werden"
            case .converterError: return "Audio-Converter konnte nicht erstellt werden"
            }
        }
    }
}

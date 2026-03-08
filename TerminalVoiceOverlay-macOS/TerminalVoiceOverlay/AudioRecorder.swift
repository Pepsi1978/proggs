import AVFoundation
import Foundation

final class AudioRecorder {
    private var audioEngine: AVAudioEngine?
    private var outputFile: AVAudioFile?
    private var tempURL: URL?
    private(set) var isRecording = false

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
        self.tempURL = url

        let file = try AVAudioFile(forWriting: url, settings: wavFormat.settings)
        self.outputFile = file

        guard let converter = AVAudioConverter(from: recordingFormat, to: wavFormat) else {
            throw RecorderError.converterError
        }

        inputNode.installTap(onBus: 0, bufferSize: 4096, format: recordingFormat) { [weak self] buffer, _ in
            guard let self = self, self.isRecording else { return }

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
        self.isRecording = true
    }

    func stop() -> URL? {
        isRecording = false
        audioEngine?.inputNode.removeTap(onBus: 0)
        audioEngine?.stop()
        audioEngine = nil
        outputFile = nil
        return tempURL
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

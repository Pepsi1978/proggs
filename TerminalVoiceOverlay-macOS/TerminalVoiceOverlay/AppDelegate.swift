import AppKit
import AVFoundation

final class AppDelegate: NSObject, NSApplicationDelegate {
    private var panel: OverlayPanel!
    private var appWatcher: AppWatcher!
    private var audioRecorder: AudioRecorder!
    private var groqClient: GroqWhisperClient!
    private var geminiClient: GeminiClient?
    private var config: Config!
    private var statusItem: NSStatusItem!

    // All state flags are only read/written on the main thread (Fix 4)
    private var isRecording = false
    private var isProcessing = false
    private var geminiEnabled = false
    private var autoEnterEnabled = true
    private var isBtwRecording = false
    private var hasPastedText = false
    private var lastRawTranscript: String?
    private var resetTimer: Timer?

    func applicationDidFinishLaunching(_ notification: Notification) {
        do {
            config = try Config.load()
        } catch {
            NSLog("Config error: %@", error.localizedDescription)
            let alert = NSAlert()
            alert.messageText = "Konfigurationsfehler"
            alert.informativeText = error.localizedDescription
            alert.alertStyle = .critical
            alert.addButton(withTitle: "Beenden")
            alert.runModal()
            NSApp.terminate(nil)
            return
        }
        NSLog("TerminalVoiceOverlay started")

        if !TerminalController.checkAccessibility() {
            NSLog("Accessibility permission missing")
        }

        AVCaptureDevice.requestAccess(for: .audio) { granted in
            if !granted { NSLog("Microphone permission denied") }
        }

        // Init clients
        audioRecorder = AudioRecorder()
        groqClient = GroqWhisperClient(apiKey: config.groqApiKey)
        if let geminiKey = config.geminiApiKey, !geminiKey.isEmpty {
            geminiClient = GeminiClient(apiKey: geminiKey)
        } else {
            geminiEnabled = false
        }

        // Create overlay panel
        panel = OverlayPanel()
        panel.setGeminiEnabled(geminiEnabled)
        panel.setAutoEnterEnabled(autoEnterEnabled)

        panel.onXClicked = { [weak self] in
            guard let self = self else { return }
            if self.panel.flashXButton() {
                self.clearLine()
            }
        }
        panel.onMicClicked = { [weak self] in self?.toggleRecording() }
        panel.onWClicked = { [weak self] in self?.whisperUndo() }
        panel.onGClicked = { [weak self] in self?.toggleGemini() }
        panel.onBtwClicked = { [weak self] in self?.toggleBtwRecording() }
        panel.onEnterClicked = { [weak self] in self?.handleEnterClick() }

        setupStatusItem()

        // Setup app watcher
        appWatcher = AppWatcher()
        appWatcher.onTerminalActivated = { [weak self] in
            DispatchQueue.main.async {
                self?.panel.orderFront(nil)
            }
        }
        appWatcher.onTerminalDeactivated = { [weak self] in
            DispatchQueue.main.async {
                guard let self = self else { return }
                if !self.isRecording && !self.isProcessing {
                    self.panel.orderOut(nil)
                }
            }
        }
        appWatcher.start()
    }

    // MARK: - Status Bar

    private func setupStatusItem() {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        if let button = statusItem.button {
            button.image = NSImage(systemSymbolName: "mic.circle", accessibilityDescription: "Voice Overlay")
        }

        let menu = NSMenu()
        menu.addItem(NSMenuItem(title: "Overlay zeigen", action: #selector(showOverlay), keyEquivalent: ""))
        menu.addItem(NSMenuItem.separator())
        menu.addItem(NSMenuItem(title: "Beenden", action: #selector(quitApp), keyEquivalent: "q"))
        statusItem.menu = menu
    }

    @objc private func showOverlay() {
        panel.orderFront(nil)
    }

    @objc private func quitApp() {
        if audioRecorder.isRecording {
            _ = audioRecorder.stop()
        }
        NSApp.terminate(nil)
    }

    // MARK: - Regular Mic Recording

    private func toggleRecording() {
        if isProcessing { return }
        if isRecording {
            stopRecording()
        } else {
            startRecording(btw: false)
        }
    }

    // MARK: - BTW Mic Recording

    private func toggleBtwRecording() {
        if isProcessing { return }
        if isRecording && isBtwRecording {
            stopRecording()
        } else if isRecording && !isBtwRecording {
            return
        } else {
            startRecording(btw: true)
        }
    }

    private func startRecording(btw: Bool) {
        isBtwRecording = btw
        do {
            try audioRecorder.start()
            isRecording = true
            if btw {
                panel.setBtwMicState(.recording)
            } else {
                panel.setMicState(.recording)
            }
            NSSound.beep()
        } catch {
            NSLog("Microphone error: %@", error.localizedDescription)
            pasteError("Mikrofon nicht verfuegbar — \(error.localizedDescription)")
        }
    }

    private func stopRecording() {
        isRecording = false
        isProcessing = true
        let wasBtw = isBtwRecording

        if wasBtw {
            panel.setBtwMicState(.processing)
        } else {
            panel.setMicState(.processing)
        }

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            guard let fileURL = self.audioRecorder.stop() else {
                DispatchQueue.main.async {
                    self.isProcessing = false
                    if wasBtw {
                        self.panel.setBtwMicState(.idle)
                    } else {
                        self.panel.setMicState(.idle)
                    }
                }
                return
            }

            // Audio feedback: double beep on stop
            DispatchQueue.main.async { NSSound.beep() }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { NSSound.beep() }

            self.groqClient.transcribe(fileURL: fileURL) { [weak self] result in
                try? FileManager.default.removeItem(at: fileURL)

                DispatchQueue.main.async {
                    guard let self = self else { return }
                    switch result {
                    case .success(let transcript):
                        #if DEBUG
                        NSLog("Transcript: %@", transcript)
                        #endif
                        self.lastRawTranscript = transcript
                        self.handleTranscript(transcript, wasBtw: wasBtw)
                    case .failure(let error):
                        NSLog("Transcription error: %@", error.localizedDescription)
                        let msg = ErrorDescriptions.describeTranscriptionError(error)
                        self.pasteError(msg, wasBtw: wasBtw)
                    }
                }
            }
        }
    }

    private func handleTranscript(_ transcript: String, wasBtw: Bool) {
        if geminiEnabled, let geminiClient = geminiClient {
            geminiClient.correctText(transcript) { [weak self] result in
                DispatchQueue.main.async {
                    guard let self = self else { return }
                    switch result {
                    case .success(let corrected):
                        self.insertText(corrected, wasBtw: wasBtw)
                    case .failure(let error):
                        NSLog("Gemini error: %@", error.localizedDescription)
                        let hint = ErrorDescriptions.describeGeminiError(error)
                        self.insertText("\(transcript) # [VoiceOverlay] \(hint)", wasBtw: wasBtw)
                    }
                }
            }
        } else {
            insertText(transcript, wasBtw: wasBtw)
        }
    }

    private func insertText(_ text: String, wasBtw: Bool) {
        var finalText = text
        if wasBtw {
            finalText = "/btw " + finalText
            isBtwRecording = false
        } else if hasPastedText {
            finalText = " " + finalText
        }

        TerminalController.pasteText(finalText, autoEnter: autoEnterEnabled)
        isProcessing = false

        if wasBtw {
            panel.setBtwMicState(.idle)
        } else {
            panel.setMicState(.idle)
        }

        hasPastedText = !autoEnterEnabled
    }

    // MARK: - Error Feedback

    private func pasteError(_ message: String, wasBtw: Bool = false) {
        if wasBtw { isBtwRecording = false }
        let errorText = "# [VoiceOverlay] FEHLER: \(message)"
        DispatchQueue.global(qos: .userInitiated).async {
            TerminalController.clearLine()
            usleep(100_000)
            TerminalController.pasteText(errorText)
        }
        hasPastedText = false
        isProcessing = false
        if wasBtw {
            panel.setBtwMicState(.error)
        } else {
            panel.setMicState(.error)
        }
        scheduleReset(wasBtw: wasBtw)
    }

    // MARK: - Clear Line

    private func clearLine() {
        DispatchQueue.global(qos: .userInitiated).async {
            TerminalController.clearLine()
        }
        hasPastedText = false
    }

    // MARK: - Whisper Undo

    private func whisperUndo() {
        guard let rawTranscript = lastRawTranscript else { return }

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            TerminalController.clearLine()
            usleep(100_000)
            TerminalController.pasteText(rawTranscript)
            DispatchQueue.main.async {
                self?.hasPastedText = true
            }
        }
    }

    // MARK: - Gemini Toggle

    private func toggleGemini() {
        guard config.geminiAvailable else { return }
        geminiEnabled.toggle()
        panel.setGeminiEnabled(geminiEnabled)
    }

    // MARK: - Auto-Enter Toggle & Manual Enter

    private func handleEnterClick() {
        if autoEnterEnabled {
            autoEnterEnabled = false
            panel.setAutoEnterEnabled(autoEnterEnabled)
        } else {
            autoEnterEnabled = true
            panel.setAutoEnterEnabled(autoEnterEnabled)
            DispatchQueue.global(qos: .userInitiated).async {
                TerminalController.activateTerminal()
                usleep(150_000)
                TerminalController.sendKeyCombo(keyCode: 0x24, flags: []) // Return
            }
        }
    }

    // MARK: - Reset Timer

    private func scheduleReset(wasBtw: Bool = false) {
        resetTimer?.invalidate()
        resetTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: false) { [weak self] _ in
            if wasBtw {
                self?.panel.setBtwMicState(.idle)
            } else {
                self?.panel.setMicState(.idle)
            }
        }
    }
}

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

    private var isRecording = false
    private var isProcessing = false
    private var geminiEnabled = false
    private var autoEnterEnabled = false
    private var hasPastedText = false
    private var lastRawTranscript: String?
    private var resetTimer: Timer?

    func applicationDidFinishLaunching(_ notification: Notification) {
        do {
            config = try Config.load()
        } catch {
            NSLog("Konfigurationsfehler: %@", error.localizedDescription)
            let alert = NSAlert()
            alert.messageText = "Konfigurationsfehler"
            alert.informativeText = error.localizedDescription
            alert.alertStyle = .critical
            alert.addButton(withTitle: "Beenden")
            alert.runModal()
            NSApp.terminate(nil)
            return
        }
        NSLog("TerminalVoiceOverlay gestartet")

        // Check accessibility
        if !TerminalController.checkAccessibility() {
            NSLog("Accessibility-Berechtigung fehlt - CGEvent funktioniert nicht")
        }

        // Request microphone permission
        AVCaptureDevice.requestAccess(for: .audio) { granted in
            if !granted {
                NSLog("Mikrofon-Berechtigung verweigert")
            }
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

        panel.onXClicked = { [weak self] in self?.clearLine() }
        panel.onMicClicked = { [weak self] in self?.toggleRecording() }
        panel.onWClicked = { [weak self] in self?.whisperUndo() }
        panel.onGClicked = { [weak self] in self?.toggleGemini() }
        panel.onEnterClicked = { [weak self] in self?.handleEnterClick() }

        // Setup menu bar icon
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

    // MARK: - Recording

    private func toggleRecording() {
        if isProcessing { return }
        if isRecording {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private func startRecording() {
        do {
            try audioRecorder.start()
            isRecording = true
            panel.setMicState(.recording)
            // Audio feedback: beep on start
            NSSound.beep()
            NSLog("Aufnahme gestartet")
        } catch {
            NSLog("Mikrofon-Fehler: %@", error.localizedDescription)
            panel.setMicState(.error)
            scheduleReset()
        }
    }

    private func stopRecording() {
        isRecording = false
        isProcessing = true
        panel.setMicState(.processing)

        // Stop recording on background thread to avoid blocking UI
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            guard let fileURL = self.audioRecorder.stop() else {
                DispatchQueue.main.async {
                    self.isProcessing = false
                    self.panel.setMicState(.idle)
                }
                return
            }

            // Audio feedback: double beep on stop (on main thread)
            DispatchQueue.main.async {
                NSSound.beep()
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                NSSound.beep()
            }
            NSLog("Aufnahme gestoppt, transkribiere...")

            self.groqClient.transcribe(fileURL: fileURL) { [weak self] result in
                // Clean up temp file
                try? FileManager.default.removeItem(at: fileURL)

                switch result {
                case .success(let transcript):
                    NSLog("Transkription: %@", transcript)
                    self?.lastRawTranscript = transcript
                    self?.handleTranscript(transcript)
                case .failure(let error):
                    NSLog("Transkriptionsfehler: %@", error.localizedDescription)
                    DispatchQueue.main.async {
                        self?.isProcessing = false
                        self?.panel.setMicState(.error)
                        self?.scheduleReset()
                    }
                }
            }
        }
    }

    private func handleTranscript(_ transcript: String) {
        if geminiEnabled, let geminiClient = geminiClient {
            NSLog("Gemini-Korrektur...")
            geminiClient.correctText(transcript) { [weak self] result in
                DispatchQueue.main.async {
                    switch result {
                    case .success(let corrected):
                        NSLog("Korrigiert: %@", corrected)
                        self?.insertText(corrected)
                    case .failure(let error):
                        NSLog("Gemini-Fehler: %@, verwende Rohtext", error.localizedDescription)
                        // Fallback to raw transcript
                        self?.insertText(transcript)
                    }
                }
            }
        } else {
            DispatchQueue.main.async { [weak self] in
                self?.insertText(transcript)
            }
        }
    }

    private func insertText(_ text: String) {
        // Prepend space if text was already pasted on this line
        var finalText = text
        if hasPastedText {
            finalText = " " + finalText
        }

        TerminalController.pasteText(finalText, autoEnter: autoEnterEnabled)
        isProcessing = false
        panel.setMicState(.success)
        NSLog("Text eingefuegt")

        // Track paste state: reset after Enter, keep for next paste
        if autoEnterEnabled {
            hasPastedText = false
        } else {
            hasPastedText = true
        }

        scheduleReset()
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

        // Clear the current line, then paste raw transcript after short delay
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            TerminalController.clearLine()
            usleep(100_000) // 100ms for terminal to process Ctrl+C
            TerminalController.pasteText(rawTranscript)
            DispatchQueue.main.async {
                self?.hasPastedText = true
            }
            NSLog("Whisper-Rohtext eingefuegt: %@", rawTranscript)
        }
    }

    // MARK: - Gemini Toggle

    private func toggleGemini() {
        guard config.geminiAvailable else { return }
        geminiEnabled.toggle()
        panel.setGeminiEnabled(geminiEnabled)
        NSLog("Gemini %@", geminiEnabled ? "AN" : "AUS")
    }

    // MARK: - Auto-Enter Toggle & Manual Enter

    private func handleEnterClick() {
        if autoEnterEnabled {
            // AN → ausschalten
            autoEnterEnabled = false
            panel.setAutoEnterEnabled(autoEnterEnabled)
            NSLog("Auto-Enter AUS")
        } else {
            // AUS → einschalten UND sofort Enter senden
            autoEnterEnabled = true
            panel.setAutoEnterEnabled(autoEnterEnabled)
            NSLog("Auto-Enter AN + Enter gesendet")
            DispatchQueue.global(qos: .userInitiated).async {
                TerminalController.activateTerminal()
                usleep(150_000)
                TerminalController.sendKeyCombo(keyCode: 0x24, flags: []) // Return
            }
        }
    }

    // MARK: - Reset Timer

    private func scheduleReset() {
        resetTimer?.invalidate()
        resetTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: false) { [weak self] _ in
            self?.panel.setMicState(.idle)
        }
    }
}

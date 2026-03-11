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
    private var autoEnterEnabled = true
    private var isBtwRecording = false
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
        NSLog("ClaudeCodexVoiceOverlay gestartet")

        // Check accessibility
        if !InputController.checkAccessibility() {
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

        // Setup menu bar icon
        setupStatusItem()

        // Setup app watcher
        appWatcher = AppWatcher()
        appWatcher.onTargetAppActivated = { [weak self] in
            DispatchQueue.main.async {
                self?.panel.orderFront(nil)
            }
        }
        appWatcher.onTargetAppDeactivated = { [weak self] in
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
            // Regular recording in progress — ignore BTW click
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
            NSLog("Aufnahme gestartet (BTW: %@)", btw ? "JA" : "NEIN")
        } catch {
            NSLog("Mikrofon-Fehler: %@", error.localizedDescription)
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
            DispatchQueue.main.async {
                NSSound.beep()
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                NSSound.beep()
            }
            NSLog("Aufnahme gestoppt, transkribiere...")

            self.groqClient.transcribe(fileURL: fileURL) { [weak self] result in
                try? FileManager.default.removeItem(at: fileURL)

                switch result {
                case .success(let transcript):
                    NSLog("Transkription: %@", transcript)
                    self?.lastRawTranscript = transcript
                    self?.handleTranscript(transcript, wasBtw: wasBtw)
                case .failure(let error):
                    NSLog("Transkriptionsfehler: %@", error.localizedDescription)
                    let msg = Self.describeTranscriptionError(error)
                    DispatchQueue.main.async {
                        self?.pasteError(msg, wasBtw: wasBtw)
                    }
                }
            }
        }
    }

    private func handleTranscript(_ transcript: String, wasBtw: Bool) {
        if geminiEnabled, let geminiClient = geminiClient {
            NSLog("Gemini-Korrektur...")
            geminiClient.correctText(transcript) { [weak self] result in
                DispatchQueue.main.async {
                    switch result {
                    case .success(let corrected):
                        NSLog("Korrigiert: %@", corrected)
                        self?.insertText(corrected, wasBtw: wasBtw)
                    case .failure(let error):
                        NSLog("Gemini-Fehler: %@, verwende Rohtext", error.localizedDescription)
                        let hint = Self.describeGeminiError(error)
                        self?.insertText("\(transcript) # [VoiceOverlay] \(hint)", wasBtw: wasBtw)
                    }
                }
            }
        } else {
            DispatchQueue.main.async { [weak self] in
                self?.insertText(transcript, wasBtw: wasBtw)
            }
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

        InputController.pasteText(finalText, autoEnter: autoEnterEnabled)
        isProcessing = false

        if wasBtw {
            panel.setBtwMicState(.success)
        } else {
            panel.setMicState(.success)
        }
        NSLog("Text eingefuegt")

        if autoEnterEnabled {
            hasPastedText = false
        } else {
            hasPastedText = true
        }

        scheduleReset(wasBtw: wasBtw)
    }

    // MARK: - Error Feedback

    private static func describeGeminiError(_ error: Error) -> String {
        let nsError = error as NSError

        if nsError.domain == NSURLErrorDomain {
            switch nsError.code {
            case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost:
                return "Gemini offline (kein Internet) — Rohtext verwendet"
            case NSURLErrorTimedOut:
                return "Gemini Timeout — Rohtext verwendet"
            case NSURLErrorCannotFindHost, NSURLErrorCannotConnectToHost, NSURLErrorDNSLookupFailed:
                return "Gemini nicht erreichbar — Rohtext verwendet"
            default:
                return "Gemini Netzwerkfehler — Rohtext verwendet"
            }
        }

        if let apiError = error as? GeminiClient.APIError {
            switch apiError {
            case .httpError(let code, _):
                if code == 429 {
                    return "Gemini Rate-Limit — Rohtext verwendet"
                } else if (500...599).contains(code) {
                    return "Gemini Server-Fehler (\(code)) — Rohtext verwendet"
                } else {
                    return "Gemini Fehler \(code) — Rohtext verwendet"
                }
            case .noData, .unexpectedResponse, .noTextInResponse:
                return "Gemini lieferte keine Antwort — Rohtext verwendet"
            }
        }

        return "Gemini-Korrektur fehlgeschlagen — Rohtext verwendet"
    }

    private static func describeTranscriptionError(_ error: Error) -> String {
        let nsError = error as NSError

        if nsError.domain == NSURLErrorDomain {
            switch nsError.code {
            case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost:
                return "Kein Internet — Spracheingabe konnte nicht transkribiert werden"
            case NSURLErrorTimedOut:
                return "Timeout — Groq API antwortet nicht (Internet-Problem?)"
            case NSURLErrorCannotFindHost, NSURLErrorCannotConnectToHost, NSURLErrorDNSLookupFailed:
                return "Groq API nicht erreichbar — DNS oder Server-Problem"
            case NSURLErrorSecureConnectionFailed:
                return "SSL-Fehler — sichere Verbindung zu Groq fehlgeschlagen"
            default:
                return "Netzwerkfehler — \(error.localizedDescription)"
            }
        }

        if let apiError = error as? GroqWhisperClient.APIError {
            switch apiError {
            case .fileReadError:
                return "Audio-Datei konnte nicht gelesen werden"
            case .httpError(let code, _):
                if code == 429 {
                    return "Groq API Rate-Limit erreicht — zu viele Anfragen, kurz warten"
                } else if (500...599).contains(code) {
                    return "Groq API Server-Fehler (\(code)) — spaeter nochmal versuchen"
                } else {
                    return "Groq API Fehler \(code) — \(error.localizedDescription)"
                }
            }
        }

        return "Transkription fehlgeschlagen — \(error.localizedDescription)"
    }

    private func pasteError(_ message: String, wasBtw: Bool = false) {
        if wasBtw {
            isBtwRecording = false
        }
        let errorText = "# [VoiceOverlay] FEHLER: \(message)"
        NSLog("Fehler ins Terminal eingefuegt: %@", errorText)
        DispatchQueue.global(qos: .userInitiated).async {
            InputController.clearLine()
            usleep(100_000)
            InputController.pasteText(errorText)
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
            InputController.clearLine()
        }
        hasPastedText = false
    }

    // MARK: - Whisper Undo

    private func whisperUndo() {
        guard let rawTranscript = lastRawTranscript else { return }

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            InputController.clearLine()
            usleep(100_000)
            InputController.pasteText(rawTranscript)
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
            autoEnterEnabled = false
            panel.setAutoEnterEnabled(autoEnterEnabled)
            NSLog("Auto-Enter AUS")
        } else {
            autoEnterEnabled = true
            panel.setAutoEnterEnabled(autoEnterEnabled)
            NSLog("Auto-Enter AN + Enter gesendet")
            DispatchQueue.global(qos: .userInitiated).async {
                InputController.activateTargetApp()
                usleep(150_000)
                InputController.sendKeyCombo(keyCode: 0x24, flags: []) // Return
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

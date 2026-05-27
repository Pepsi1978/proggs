import AppKit
import AVFoundation

/// Append a debug line to a stable file so we can diagnose without
/// os_log privacy masking eating our strings. Safe to call from any
/// thread — uses a serial queue so lines don't interleave.
private let tvoDebugQueue = DispatchQueue(label: "tvo.debug.log")
func tvoDebug(_ message: @autoclosure @escaping () -> String,
              file: String = #file, line: Int = #line) {
    let msg = message()
    tvoDebugQueue.async {
        let stamp = ISO8601DateFormatter().string(from: Date())
        let src = (file as NSString).lastPathComponent
        let entry = "[\(stamp)] \(src):\(line) \(msg)\n"
        let url = URL(fileURLWithPath: "/tmp/tvo-debug.log")
        if let h = try? FileHandle(forWritingTo: url) {
            h.seekToEndOfFile()
            if let data = entry.data(using: .utf8) { h.write(data) }
            try? h.close()
        } else {
            try? entry.write(to: url, atomically: false, encoding: .utf8)
        }
        // Also mirror to stderr so `log stream` or console can see it
        FileHandle.standardError.write((entry.data(using: .utf8)) ?? Data())
    }
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    /// Safety net: whenever the app becomes active, make sure our overlay /
    /// prompt-board panels still have the correct `.floating` level. If a
    /// previous modal dialog or a crash-path ever left them demoted to
    /// `.normal` they would slip under other windows and clicks would only
    /// produce a system beep. This restores them automatically.
    func applicationDidBecomeActive(_ notification: Notification) {
        let pl = self.panel?.level.rawValue ?? -99
        let pbl = self.promptBoardPanel?.level.rawValue ?? -99
        tvoDebug("[App] applicationDidBecomeActive panelLevel=\(pl) pbLevel=\(pbl)")
        if let p = self.panel, p.level != .floating { p.level = .floating }
        if let pb = self.promptBoardPanel, pb.level != .floating { pb.level = .floating }
    }

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
    private var geminiEnabled = false // Default = Whisper-Mode (W aktiv, G dunkel). Erstes Profil-Klick schaltet Gemini automatisch ein.
    private var autoEnterEnabled = true
    private let autoEnterServer = AutoEnterStatusServer()
    private var autoHide: AutoHideController?
    private var alwaysOnActive = false
    private var promptBoardPanel: PromptBoardPanel?

    // Path of the last screenshot captured by the ScreenshotButton.
    // The InsertScreenshotButton paste this path into the active terminal.
    // Stays UNCHANGED on a failed capture so the user keeps the last
    // good path memorised — matches Windows _lastScreenshotPath behaviour.
    private var lastScreenshotPath: String?

    /// Timer fuer X-Button Press-and-Hold: solange linke Maustaste gedrueckt,
    /// wird alle 10 ms eine Zeile geloescht. Bei mouseUp wird der Timer invalidiert.
    private var xRepeatTimer: Timer?

    // 5-second hide-delay timer (matches Windows _hideDelayTimer from
    // commit #1913). When the user switches away from the terminal we
    // delay the panel hide by 5 s so they can still grab a screenshot
    // or use the pillar from another app. If the terminal becomes
    // active again within those 5 s, the timer is cancelled.
    private var hideDelayTimer: Timer?
    private var isBtwRecording = false
    private var hasPastedText = false
    private var lastRawTranscript: String?
    private var resetTimer: Timer?

    // ── Profile-Korrektur (Windows-Port #1957..#1964) ──
    // 1 = Standard, 2 = Programmierung, 3 = Meta-Intelligenz,
    // 4-10 = numerische Slots (frei belegbar). Default = 1.
    private var activeProfile: Int = 1
    // Letzte Whisper-Roh-Transkription. Bleibt unbegrenzt im Cache,
    // wird nur durch eine NEUE Aufnahme ueberschrieben — kein Zeitlimit.
    // Frank steuert das Verhalten ueber Maustaste:
    //  • Linksklick auf Profil-Tile → Re-Correct (Cache durch Profil
    //    schicken, Eingabe ersetzen, ggf. Auto-Submit)
    //  • Rechtsklick auf Profil-Tile → nur Profil wechseln, Cache bleibt
    //    unangetastet, naechste Aufnahme nutzt das neue Profil
    private var lastCorrectableRaw: String?

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

        // Bring up the shared PromptBoard database before the overlay so
        // the star-button panel can pull categories/prompts on demand.
        // Failures are non-fatal: the overlay still works, the star just
        // opens an empty panel in that case.
        do {
            try PromptBoardStore.shared.open()
            NSLog("PromptBoard DB: %@", PromptBoardStore.shared.dbPath)
        } catch {
            NSLog("PromptBoardStore open failed: %@", error.localizedDescription)
        }

        // Async check: if the Drive backup is newer than our local state,
        // pull it down and apply. Runs once per launch, silent on all error
        // paths — the user's workflow is never blocked by this.
        checkForRemoteBackupOnLaunch()

        // Cloud-Merge der Prompt-Historie: einmal beim App-Start
        // versuchen, neue Eintraege vom anderen Geraet abzuholen.
        // Fire-and-forget — wenn Drive nicht verbunden ist, schluckt
        // der Helper still und es wird einfach nichts gemergt.
        mergeHistoryFromCloudOnLaunch()

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

        // Share the audio/STT/Gemini stack with secondary surfaces
        // (e.g. PBPromptEditDialog's mic + G buttons). Single AudioRecorder
        // instance is critical — only one process can hold the microphone.
        VoiceServiceProvider.initialize(
            recorder: audioRecorder,
            groq: groqClient,
            gemini: geminiClient)

        // Create overlay panel
        panel = OverlayPanel()
        panel.setGeminiEnabled(geminiEnabled)
        panel.setAutoEnterEnabled(autoEnterEnabled)
        panel.setActiveProfile(activeProfile)

        // HTTP-Server fuer Stream-Deck-XL-Polling starten (Port 5723).
        autoEnterServer.statusProvider = { [weak self] in
            return self?.autoEnterEnabled ?? false
        }
        autoEnterServer.toggleHandler = { [weak self] in
            guard let self = self else { return false }
            self.autoEnterEnabled.toggle()
            DispatchQueue.main.async {
                self.panel?.setAutoEnterEnabled(self.autoEnterEnabled)
            }
            return self.autoEnterEnabled
        }
        autoEnterServer.start()

        // X-Button: kurzer Klick loescht eine Zeile, gedrueckt halten loescht alle Zeilen
        // hintereinander im 100ms-Takt — gut kontrollierbare Geschwindigkeit
        // beim Halten (~10 Loeschungen pro Sekunde).
        //
        // KRITISCH — zwei Stoerquellen, die zusammen das "Loop-laeuft-ewig"-Problem
        // verursacht haben:
        //
        // 1. Mouse-Release-Detection: onXMouseUp kommt NICHT zuverlaessig an.
        //    Wenn clearLine() per activateTerminal() die Terminal-App in den
        //    Vordergrund holt, verliert unser nonactivatingPanel den
        //    Mouse-Tracking-Stream. Loesung: pro Timer-Tick mit
        //    NSEvent.pressedMouseButtons pollen — funktioniert systemweit
        //    ohne spezielle Permission.
        //
        // 2. sendQueue-Backlog: Der vorherige Code hat im 10-ms-Takt
        //    TerminalController.clearLine() aufgerufen. Das hat jeden Auftrag
        //    in die SERIELLE sendQueue geschoben (mit 150 ms usleep pro Item).
        //    Bei 100 Submits/sec aber nur 6-7 Verarbeitungen/sec stauen sich
        //    massive Backlogs auf — die werden NACH dem Loslassen noch
        //    SEKUNDEN lang abgearbeitet und loeschen jede neue Eingabe.
        //    Loesung: Im Timer-Tick direkt sendKeyCombo aufrufen, OHNE den
        //    sendQueue-Umweg. Die Initial-Aktivierung uebernimmt einmalig
        //    der erste clearLine()-Aufruf vor dem Timer-Start.
        panel.onXMouseDown = { [weak self] in
            guard let self = self else { return }
            tvoDebug("[App] onXMouseDown — Press-and-Hold-Loop start")
            self.panel.flashXButton()
            // Erstes Loeschen mit voller Maschinerie: aktiviert Terminal +
            // wartet 150 ms + sendet Ctrl+U. Danach ist Terminal aktiv und
            // wir koennen direkt nachfeuern.
            self.clearLine()
            // Repeat-Timer: alle 100 ms direkt Ctrl+U senden, OHNE Queue-Umweg.
            // Vor jedem Tick pruefen wir ob die linke Maustaste noch gedrueckt
            // ist — wenn nicht, sofort stoppen. Kein Backlog moeglich weil
            // sendKeyCombo synchron innerhalb von Mikrosekunden zurueckkehrt.
            self.xRepeatTimer?.invalidate()
            self.xRepeatTimer = Timer.scheduledTimer(withTimeInterval: 0.10, repeats: true) { [weak self] _ in
                guard let self = self else { return }
                let leftButtonPressed = (NSEvent.pressedMouseButtons & 1) != 0
                if !leftButtonPressed {
                    tvoDebug("[App] xRepeatTimer — linke Maustaste losgelassen, Loop stop")
                    self.xRepeatTimer?.invalidate()
                    self.xRepeatTimer = nil
                    return
                }
                // Direkter Aufruf ohne sendQueue, ohne usleep, ohne erneute
                // Activation. 0x20 = 'u', .maskControl = Ctrl-Modifier.
                TerminalController.sendKeyCombo(keyCode: 0x20, flags: .maskControl)
            }
        }
        panel.onXMouseUp = { [weak self] in
            // Backup-Pfad fuer den Fall dass der native mouseUp doch ankommt
            // (z.B. Click-und-sofort-Loslassen bevor das Terminal aktiviert ist).
            tvoDebug("[App] onXMouseUp — native mouseUp, Loop stop")
            self?.xRepeatTimer?.invalidate()
            self?.xRepeatTimer = nil
        }
        panel.onUltrathinkClicked = { [weak self] in self?.toggleUltrathink() }
        panel.onOrientationToggleClicked = { [weak self] in self?.toggleOverlayOrientation() }
        panel.onSaveClicked = { [weak self] in self?.savePositionForCurrentOrientation() }
        // Lazy-init der Extra-Buttons (OrientationToggle + Save) + Anordnung
        // im vertikalen Stack-Layout (Stern oben/⇄ unten, Enter oben/Save unten).
        // Sonst wuerden die Buttons erst nach dem ersten Cmd+Shift+O erscheinen.
        panel.positionExtraButtonsVertical()

        // Auto-Hide-Controller starten (5s Inaktivitaet → Collapsed-Pille).
        autoHide = AutoHideController(panel: panel)
        autoHide?.busyProvider = { [weak self] in
            guard let self = self else { return false }
            return self.isRecording || self.isProcessing
        }
        panel.onPillarMoved = { [weak self] in
            // Keep the PromptBoard side panel docked to the pillar's
            // left edge as the user right-click drags it around.
            guard let self = self else { return }
            self.promptBoardPanel?.dock(rightOf: self.panel)
        }
        panel.onMicClicked = { [weak self] in self?.toggleRecording() }
        panel.onWClicked = { [weak self] in self?.whisperUndo() }
        panel.onGClicked = { [weak self] in self?.toggleGemini() }
        panel.onBtwClicked = { [weak self] in self?.toggleBtwRecording() }
        panel.onEnterClicked = { [weak self] in self?.handleEnterClick() }

        panel.onCopyClicked = { [weak self] in
            TerminalController.copySelection()
            self?.panel.flashCopyButton()
        }

        panel.onPasteClicked = { [weak self] in
            guard let self = self else { return }
            TerminalController.pasteClipboard()
            self.panel.flashPasteButton()
            self.hasPastedText = true
            if self.autoEnterEnabled {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                    guard let self = self else { return }
                    TerminalController.pressReturn()
                    self.hasPastedText = false
                }
            }
        }

        panel.onScreenshotClicked = { [weak self] in self?.takeScreenshot() }
        panel.onInsertScreenshotClicked = { [weak self] in self?.insertLastScreenshot() }
        panel.onProfileClicked = { [weak self] profile in self?.switchProfile(profile) }
        panel.onProfileRightClicked = { [weak self] profile in self?.switchProfileWithoutReCorrect(profile) }

        setupStatusItem()
        setupGlobalHotkeys()

        // Debug: log every mouseDown anywhere in the system so we can see
        // whether clicks are even reaching our process. If a click lands on
        // a coordinate inside one of our panels but no button-handler fires,
        // we have a hit-testing / event-routing bug.
        NSEvent.addGlobalMonitorForEvents(matching: [.leftMouseDown, .rightMouseDown]) { [weak self] event in
            guard let self = self else { return }
            let loc = NSEvent.mouseLocation
            let inPanel = self.panel?.frame.contains(loc) ?? false
            let inPB = self.promptBoardPanel?.frame.contains(loc) ?? false
            let which = event.type == .rightMouseDown ? "RIGHT" : "LEFT"
            tvoDebug("[App] GLOBAL \(which)-mouseDown loc=(\(Int(loc.x)),\(Int(loc.y))) inPillar=\(inPanel) inPromptBoard=\(inPB) panelLevel=\(self.panel?.level.rawValue ?? -99) pbLevel=\(self.promptBoardPanel?.level.rawValue ?? -99) active=\(NSApp.isActive)")
        }
        NSEvent.addLocalMonitorForEvents(matching: [.leftMouseDown, .rightMouseDown]) { [weak self] event in
            guard let self = self else { return event }
            let loc = NSEvent.mouseLocation
            let inPanel = self.panel?.frame.contains(loc) ?? false
            let inPB = self.promptBoardPanel?.frame.contains(loc) ?? false
            let modalWinTitle = NSApp.modalWindow?.title ?? "<none>"
            let keyWinTitle = NSApp.keyWindow?.title ?? "<none>"
            let mainWinTitle = NSApp.mainWindow?.title ?? "<none>"
            let windowAtPoint = event.window?.title ?? "<nil>"
            let which = event.type == .rightMouseDown ? "RIGHT" : "LEFT"
            tvoDebug("[App] LOCAL \(which)-mouseDown loc=(\(Int(loc.x)),\(Int(loc.y))) inPillar=\(inPanel) inPB=\(inPB) active=\(NSApp.isActive) modal=\(modalWinTitle) key=\(keyWinTitle) main=\(mainWinTitle) evtWin=\(windowAtPoint)")
            return event
        }

        // Setup app watcher
        appWatcher = AppWatcher()
        appWatcher.onTerminalActivated = { [weak self] in
            DispatchQueue.main.async {
                guard let self = self else { return }
                tvoDebug("[App] onTerminalActivated — panelLevel=\(self.panel.level.rawValue) pbLevel=\(self.promptBoardPanel?.level.rawValue ?? -1) active=\(NSApp.isActive)")
                // Cancel any pending hide — terminal is back, so the
                // 5-s grace period is over and the panel stays up.
                self.hideDelayTimer?.invalidate()
                self.hideDelayTimer = nil
                // CRITICAL: use orderFrontRegardless instead of orderFront(nil).
                // When the terminal becomes active, our own app is *not* active.
                // AppKit logs in that case: "ordered front from a non-active
                // application and may order beneath the active application's
                // windows" — and the panel visibly slips below the terminal.
                // After that, clicks miss our buttons (they hit the terminal)
                // and the user only hears system beeps.
                self.panel.orderFrontRegardless()
                // Keep the pillar pinned to the floating level. Earlier bugs
                // left it demoted to .normal, which reproduces the same
                // clicks-fall-through-to-terminal symptom.
                self.panel.level = .floating
                if self.alwaysOnActive, let p = self.promptBoardPanel {
                    p.dock(rightOf: self.panel)
                    p.level = .floating
                    p.orderFrontRegardless()
                    // Floating Eingabe/Historie auch zurueckholen — der
                    // Benutzer hatte sie evtl. offen als das Terminal die
                    // Aktivitaet verlor. So sind die Panels nie ueber
                    // Chrome o.ae. zu sehen, sondern nur ueber dem Terminal.
                    p.showTransientChildrenIfNeeded()
                }
            }
        }
        appWatcher.onTerminalDeactivated = { [weak self] in
            DispatchQueue.main.async {
                guard let self = self else { return }
                tvoDebug("[App] onTerminalDeactivated isRec=\(self.isRecording) isProc=\(self.isProcessing)")
                if !self.isRecording && !self.isProcessing {
                    // 5-second hide-delay (matches Windows #1913). Lets
                    // the user grab a screenshot of another app or use
                    // the pillar from a browser without the panel
                    // disappearing the moment they leave the terminal.
                    self.hideDelayTimer?.invalidate()
                    self.hideDelayTimer = Timer.scheduledTimer(
                        withTimeInterval: 5.0, repeats: false
                    ) { [weak self] _ in
                        guard let self = self else { return }
                        self.hideDelayTimer = nil
                        // Re-check state — recording may have started
                        // during the grace window.
                        guard !self.isRecording && !self.isProcessing else { return }
                        self.panel.orderOut(nil)
                        self.promptBoardPanel?.hideTransientChildren()
                        self.promptBoardPanel?.orderOut(nil)
                    }
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
        // Status-bar item action: our app is inactive while the user clicks
        // the menu bar, so use orderFrontRegardless to avoid the panel
        // landing behind other apps' windows.
        panel.orderFrontRegardless()
        panel.level = .floating
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
        // KRITISCH: Reset-Timer aus der vorherigen Aufnahme stoppen, sonst
        // feuert er ggf. mitten in der NEUEN Aufnahme und setzt den Mic-State
        // auf Idle zurueck — UI sieht aus als waere die Aufnahme aus, der
        // AudioRecorder laeuft aber weiter (State-Drift bei schnellen
        // aufeinanderfolgenden Aufnahmen).
        resetTimer?.invalidate()
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
                        // Re-Correct-Cache fuer Profile-Wechsel: Roh-Whisper-
                        // Text merken — bleibt im Cache bis eine neue
                        // Aufnahme ihn ueberschreibt. Linksklick auf ein
                        // Profil-Tile schickt diesen Text durch das gewaehlte
                        // Profil; Rechtsklick laesst den Cache unangetastet.
                        self.lastCorrectableRaw = transcript
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
            geminiClient.correctText(transcript, profile: activeProfile) { [weak self] result in
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
        // Voice-Routing: Wenn das neue Prompt-Eingabefenster offen ist
        // (Stern an im Promptboard) und es sich nicht um eine BTW-Diktatur
        // handelt, wandert das Voice-Transkript ins Eingabefeld statt direkt
        // in die CLI. Der Benutzer kann es dann editieren und mit Enter
        // absenden — der Submit-Pfad baut Pre/Mitte/Post zusammen UND legt
        // den Eintrag in der Historie ab. So landen auch eingesprochene
        // Prompts in der Historie.
        if !wasBtw, let board = promptBoardPanel, board.isInputPanelVisible {
            board.routeVoiceTextToInput(text)
            isProcessing = false
            panel.setMicState(.idle)
            tvoDebug("[App] voice text routed to input panel (\(text.count) chars)")
            return
        }

        var finalText = text
        if wasBtw {
            // BTW dictations stay simple — no always-on prefix here,
            // these are short asides, not full prompts.
            finalText = "/btw " + finalText
            isBtwRecording = false
        } else {
            // Wrap the dictated text with PromptBoard always-on prompts
            // when the star toggle is active. Pre-prompts go in front,
            // post-prompts go after — both sets are independent and a
            // single prompt can be on both sides if its both flags are
            // set. Only on the first paste per line; follow-up dictations
            // are appended without wrapping.
            if alwaysOnActive && !hasPastedText {
                let pre = AlwaysOnPrefixService.buildPre()
                let post = AlwaysOnPrefixService.buildPost()
                if !pre.isEmpty { finalText = pre + finalText }
                if !post.isEmpty { finalText = finalText + post }
            }
            // Always append " ; " — compact inline task separator.
            finalText = finalText + " ; "
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

    // MARK: - Prompt Input Submit (tippbares Eingabefenster)

    /// Wird aus dem Prompt-Eingabefenster ausgeloest wenn der Benutzer Enter
    /// drueckt. Der `middleText` ist die reine Mitte (was der Benutzer
    /// getippt oder per Voice eingespielt hat). Wir bauen Pre + Mitte + Post
    /// mit ` ; ` als Trenner zusammen, fuegen alles in die CLI ein und
    /// respektieren den Auto-Enter-Toggle des Voice-Overlays — so geht der
    /// Prompt direkt an die KI ab, wenn Auto-Enter an ist.
    /// Phase 4 wird hier zusaetzlich den Eintrag in die Historie schreiben.
    private func handleInputSubmit(_ middleText: String) {
        let mid = middleText.trimmingCharacters(in: .whitespacesAndNewlines)

        // AlwaysOnPrefixService nur aufrufen wenn der OverlayWindow-Stern
        // aktiv ist — sonst wuerde er einen leeren String zurueckgeben.
        let pre = alwaysOnActive ? AlwaysOnPrefixService.buildPre() : ""
        let post = alwaysOnActive ? AlwaysOnPrefixService.buildPost() : ""

        // Die buildPre/buildPost geben Pre/Post bereits joined zurueck (mit
        // " ; " als Trenner). Wir verbinden Pre/Mitte/Post mit demselben
        // Trenner — leere Bloecke werden uebersprungen.
        var parts: [String] = []
        if !pre.isEmpty  { parts.append(pre) }
        if !mid.isEmpty  { parts.append(mid) }
        if !post.isEmpty { parts.append(post) }

        guard !parts.isEmpty else {
            tvoDebug("[App] handleInputSubmit: nothing to insert (empty)")
            return
        }

        let final = parts.joined(separator: " ; ")
        TerminalController.pasteText(final, autoEnter: autoEnterEnabled)
        tvoDebug("[App] input submit: \(final.count) chars (autoEnter=\(self.autoEnterEnabled))")
        hasPastedText = !autoEnterEnabled

        // Historie-Eintrag: Erst sofort mit Fallback-Titel speichern (damit
        // der Benutzer den Eintrag direkt sieht), dann Gemini-Titel im
        // Hintergrund nachziehen. Submit darf NICHT auf das KI-Ergebnis
        // warten — der Tipp-Flow soll fluessig bleiben.
        writeHistory(middleText: mid)
    }

    // MARK: - Cloud-Sync der Historie (Phase 5)

    /// Holt die Cloud-Historie und mergt sie mit dem lokalen Stand.
    /// Wird einmal beim App-Start aufgerufen. Neue Cloud-Eintraege
    /// wandern oben in die Liste, lokale Eintraege bleiben erhalten —
    /// bei doppelten IDs gewinnt der lokale Stand (kann frischeren
    /// KI-Titel haben). Fehler werden nur in den Debug-Log geschrieben,
    /// niemals dem Benutzer angezeigt — Sync ist Komfort, kein Pflichtkanal.
    private func mergeHistoryFromCloudOnLaunch() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[App] history cloud merge skipped: drive not connected")
            return
        }
        GoogleDriveBackupService.shared.downloadHistory { [weak self] result in
            switch result {
            case .failure(let e):
                tvoDebug("[App] history cloud download failed: \(e.localizedDescription)")
            case .success(let cloud):
                guard let cloudJson = cloud, !cloudJson.isEmpty else {
                    tvoDebug("[App] no cloud history yet — nothing to merge")
                    return
                }
                PromptHistoryStore.shared.load { local in
                    let merged = PromptHistoryStore.merge(local: local, cloudJson: cloudJson)
                    if merged.count == local.count {
                        tvoDebug("[App] cloud history merge: no new entries")
                        return
                    }
                    PromptHistoryStore.shared.replaceAll(entries: merged) {
                        tvoDebug("[App] cloud history merged: +\(merged.count - local.count) entries")
                        // Offene Historie-Ansicht direkt aktualisieren.
                        self?.promptBoardPanel?.reloadHistory()
                    }
                }
            }
        }
    }

    /// Laedt die lokale prompt-history.json zu Drive hoch. Wird nach
    /// jedem Submit aufgerufen (fire-and-forget). Wenn Drive nicht
    /// verbunden ist, ist das kein Problem fuer den Tipp-Flow.
    private func uploadHistoryToCloud() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[App] history upload skipped: drive not connected")
            return
        }
        let json = PromptHistoryStore.shared.rawJsonFromDisk()
        GoogleDriveBackupService.shared.uploadHistory(json: json) { [weak self] result in
            switch result {
            case .success:
                tvoDebug("[App] history uploaded to cloud")
                // Sync-Badge im Promtboard-Header auch fuer Historie-
                // Uploads aktualisieren — sonst zeigt das Label nur den
                // letzten Promtboard-Backup, obwohl die Historie laufend
                // gesynct wird.
                self?.promptBoardPanel?.markSyncedNow()
            case .failure(let e):
                tvoDebug("[App] history upload failed: \(e.localizedDescription)")
            }
        }
    }

    private func writeHistory(middleText: String) {
        let mid = middleText
        let fallbackTitle = GeminiClient.fallbackTitle(from: mid)
        PromptHistoryStore.shared.append(text: mid, title: fallbackTitle) { [weak self] entry in
            // Sofortiges Re-Render: das offene Historie-Fenster zeigt den
            // neuen Eintrag direkt, ohne Zu-/Aufklappen.
            self?.promptBoardPanel?.reloadHistory()

            // Cloud-Push der lokalen Historie. Bewusst NACH dem Render —
            // der Benutzer sieht seinen Eintrag sofort, der Upload ist
            // Hintergrund-Arbeit.
            self?.uploadHistoryToCloud()

            // KI-Titel-Generierung nutzt den Gemini-Key aus dem Promptboard-
            // Store (gleiche Quelle wie der Edit-Dialog "G"-Button und die
            // AI-Improvement-Pipeline). So pflegt der Benutzer genau EINEN
            // Schluessel im Promptboard-Settings-Dialog, und alle drei
            // Pfade ziehen am selben Strang. Faellt der .env-Key (Voice-
            // Overlay) aus, ist die Historie davon nicht betroffen.
            guard let self = self else { return }
            guard let pbKey = (try? PromptBoardStore.shared.settings().geminiApiKey)
                                ?? nil,
                  !pbKey.isEmpty else {
                tvoDebug("[App] history title: no Gemini key in PromptBoardStore")
                return
            }
            let titleClient = GeminiClient(apiKey: pbKey)
            titleClient.generateTitle(mid) { [weak self] aiTitle in
                let trimmed = aiTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !trimmed.isEmpty, trimmed != fallbackTitle else { return }
                PromptHistoryStore.shared.updateTitle(entryId: entry.id, newTitle: trimmed)
                // Nochmal rendern damit der KI-Titel sichtbar wird —
                // und Cloud nochmal hochladen damit das andere Geraet
                // beim naechsten Start den feineren Titel sieht.
                DispatchQueue.main.async {
                    self?.promptBoardPanel?.reloadHistory()
                    self?.uploadHistoryToCloud()
                }
            }
        }
    }

    // MARK: - Star Button: PromptBoard toggle (panel + always-on prefix)

    /// Pillar-Stern-Klick (Windows-Pendant: BtnUltrathink_Click).
    /// Default seit 2026-05-09: erster Klick zeigt das PromptBoard mit
    /// allen Kategorien, das Eingabefeld bleibt zu. Der Benutzer schaltet
    /// ueber den NEUEN Stern in der Promtboard-Toolbar in den Solo-Modus
    /// (Board zu, Eingabefeld am Pillar). Der Stern in der Eingabe-Toolbar
    /// holt das Promtboard wieder zurueck. Zweiter Klick auf diesen Pillar-
    /// Stern schliesst alles.
    private func toggleUltrathink() {
        alwaysOnActive.toggle()
        panel.setUltrathinkEnabled(alwaysOnActive)
        if alwaysOnActive {
            showPromptPanel()
        } else {
            hidePromptStack()
        }
        NSLog("PromptBoard panel %@", alwaysOnActive ? "OPEN" : "CLOSED")
    }

    /// Klassischer Show-Pfad: das Promtboard erscheint rechts neben dem
    /// Pillar mit allen Kategorien. Eingabefeld bleibt zu — der Benutzer
    /// kann es ueber den Stern in der Promtboard-Toolbar dazuschalten
    /// (= applySoloDockMode(true)). Pendant zu Windows ShowPromptPanel.
    private func showPromptPanel() {
        ensurePromptBoardInstance()
        guard let board = promptBoardPanel else { return }
        inputSoloDock = false
        board.refresh()
        board.dock(rightOf: self.panel)
        board.orderFrontRegardless()
    }

    /// True wenn die Prompt-Eingabe direkt am Pillar haengt (PromptBoard
    /// ist dann versteckt). Default seit 2026-05-09 ist false — der Pillar-
    /// Stern oeffnet jetzt zuerst das Promtboard, der Solo-Dock-Stern
    /// (in der Promtboard-Toolbar oder der Eingabe-Toolbar) schaltet um.
    private var inputSoloDock: Bool = false

    /// Startup-Default: Pillar-Stern oeffnet die Eingabe direkt am Pillar.
    /// Das PromptBoard wird intern instanziiert (fuer Daten/Logik), aber
    /// nicht sichtbar. Der Solo-Dock-Stern in der Eingabe-Toolbar holt es
    /// bei Bedarf nach vorne.
    private func showPromptInputDockedToOverlay() {
        ensurePromptBoardInstance()
        guard let board = promptBoardPanel else { return }
        // Daten frisch laden, damit beim Einblenden des Boards via
        // Solo-Dock-Stern keine leere Liste zu sehen ist.
        board.refresh()
        // Eingabe oeffnen — das Board ruft openInputPanelExternally(),
        // welche das InputPanel via interner dock(leftOf:) ans Board
        // andockt. Wir ueberschreiben das gleich auf Pillar-Andock.
        board.openInputPanelExternally()
        // Solo-Dock-Toggle und Gemini-Callback verdrahten BEVOR wir die
        // Andockung anpassen — sonst koennten Klicks ins Leere laufen.
        wireInputPanelCallbacks()
        guard let input = board.currentInputPanel else { return }
        inputSoloDock = true
        input.dockToOverlay(self.panel)
        input.setSoloDockState(true)
        // Fokus ins Eingabefeld damit der Benutzer SOFORT tippen kann
        // ohne erst klicken zu muessen — fixt den Beep-Bug, weil das
        // Eingabe-Fenster jetzt firstResponder ist.
        input.makeKeyAndOrderFront(nil)
    }

    /// Schliesst sowohl Eingabe als auch Board (z.B. wenn der Pillar-Stern
    /// erneut geklickt wird = Ultrathink off).
    private func hidePromptStack() {
        promptBoardPanel?.closeInputPanelExternally()
        promptBoardPanel?.orderOut(nil)
    }

    /// Solo-Dock-Stern wurde geklickt. Wird sowohl vom Stern in der
    /// Promtboard-Toolbar (active=true, Wechsel Board → Solo) als auch vom
    /// Stern in der PromptInput-Toolbar (active=false, Rueckweg Solo → Board)
    /// aufgerufen.
    /// `active=true`: PromptBoard ausblenden, Eingabe ans Pillar andocken.
    /// `active=false`: PromptBoard rechts neben Pillar zeigen, Eingabe rueckt
    /// an dessen linken Rand (Standard-Zweier-Layout).
    private func applySoloDockMode(_ active: Bool) {
        guard let board = promptBoardPanel else { return }

        if active {
            // Wechsel Board → Solo: das Eingabefeld muss EXISTIEREN bevor
            // wir es positionieren koennen. Klick aus der Board-Toolbar kann
            // bedeuten dass es noch nie offen war — also bei Bedarf erzeugen
            // und Callbacks (Solo-Toggle, Gemini) verdrahten. Pendant zu
            // Windows OverlayWindow.ApplySoloDockMode #1742-1750.
            if board.currentInputPanel == nil {
                board.openInputPanelExternally()
                wireInputPanelCallbacks()
            }
            guard let input = board.currentInputPanel else { return }
            inputSoloDock = true
            board.orderOut(nil)
            input.dockToOverlay(self.panel)
            input.setSoloDockState(true)
            input.makeKeyAndOrderFront(nil)
        } else {
            guard let input = board.currentInputPanel else { return }
            inputSoloDock = false
            board.dock(rightOf: self.panel)
            board.orderFrontRegardless()
            board.refresh()
            // Eingabe rueckt an den linken Rand des Boards via interner
            // dock(leftOf:) — diese Methode existiert schon im PromptInputPanel.
            input.dock(leftOf: board, force: true)
            input.setSoloDockState(false)
            input.makeKeyAndOrderFront(nil)
        }
    }

    /// Erstellt die PromptBoardPanel-Instanz inkl. aller Callbacks, macht es
    /// aber NICHT sichtbar. Wird sowohl vom Solo-Dock-Einstieg (nur Eingabe)
    /// als auch von applySoloDockMode(false) genutzt — alle Subscriptions
    /// liegen damit an einer Stelle. Pendant zu Windows EnsurePromptPanelInstance.
    private func ensurePromptBoardInstance() {
        if promptBoardPanel != nil { return }
        let p = PromptBoardPanel()
        p.onInsertText = { [weak self] text in
            guard let self = self, !text.isEmpty else { return }
            tvoDebug("[App] onInsertText textLen=\(text.count) autoEnter=\(self.autoEnterEnabled)")
            TerminalController.pasteText(text, autoEnter: self.autoEnterEnabled)
        }
        p.onInputSubmit = { [weak self] middleText in
            self?.handleInputSubmit(middleText)
        }
        p.onHistorySyncRequested = { [weak self] in
            self?.uploadHistoryToCloud()
        }
        // Rechtsklick-Drag aufs Board verschiebt die ganze Gruppe — wenn
        // das Board sichtbar ist, zieht der Pillar mit; im Solo-Dock-Modus
        // ist das Board orderOut'd, also kommt dieser Pfad gar nicht zum
        // Tragen. Logik bleibt unveraendert.
        p.onPanelDragged = { [weak self] panelOrigin in
            guard let self = self else { return }
            let pillarSize = self.panel.frame.size
            let pillarOrigin = NSPoint(
                x: panelOrigin.x + p.frame.size.width + 4,
                y: panelOrigin.y)
            self.panel.setFrame(NSRect(origin: pillarOrigin, size: pillarSize),
                                display: true)
            self.panel.savePillarPosition()
        }
        // Stern in der Promtboard-Toolbar — symmetrisches Pendant zum Stern
        // im Eingabefeld. Klick blendet das Board aus und dockt das
        // Eingabefeld direkt an den Pillar an. applySoloDockMode(true) macht
        // bei Bedarf gleich noch ein openInputPanelExternally() falls das
        // Eingabefeld noch nicht existiert.
        p.onBoardStarToggle = { [weak self] in
            self?.applySoloDockMode(true)
        }
        promptBoardPanel = p
    }

    /// Wird aufgerufen NACHDEM die InputPanel-Instanz im Board erzeugt
    /// wurde — wir verdrahten Solo-Dock-Toggle und Gemini-Verbesserung.
    /// Diese Verdrahtung muss bei jedem Re-Open passieren weil das Board
    /// das InputPanel intern verwirft (closeInputPanel() setzt es auf nil).
    private func wireInputPanelCallbacks() {
        guard let input = promptBoardPanel?.currentInputPanel else { return }
        input.onSoloDockToggle = { [weak self] newState in
            self?.applySoloDockMode(newState)
        }
        input.onGeminiImprove = { [weak self] currentText, completion in
            self?.geminiImproveText(currentText, completion: completion)
        }
    }

    /// Fuer den G-Button in der Eingabe-Toolbar: ruft Gemini mit dem
    /// aktuellen Text auf und liefert die korrigierte Variante zurueck.
    /// Bei Fehlern liefern wir nil — die Eingabe bleibt dann unveraendert.
    private func geminiImproveText(_ text: String,
                                   completion: @escaping (String?) -> Void) {
        guard let gemini = geminiClient else {
            completion(nil)
            return
        }
        gemini.correctText(text, profile: activeProfile) { result in
            switch result {
            case .success(let corrected):
                completion(corrected)
            case .failure(let err):
                tvoDebug("[App] Gemini improve failed: \(err.localizedDescription)")
                completion(nil)
            }
        }
    }

    // MARK: - Gemini Toggle

    private func toggleGemini() {
        guard config.geminiAvailable else { return }
        geminiEnabled.toggle()
        panel.setGeminiEnabled(geminiEnabled)
        // setGeminiEnabled triggert intern auch refreshProfileTiles —
        // bei G=off werden alle Profile-Kästchen dunkel, bei G=on leuchtet
        // das gespeicherte aktive Profil wieder goldgelb.
    }

    /// Wechselt das aktive Korrektur-Profil. Schaltet Gemini auto-ein wenn
    /// es gerade aus war (klare Absicht durch Profil-Klick). Wenn der
    /// zuletzt gediktierte Whisper-Text noch frisch im Cache liegt
    /// (max. 2 Minuten), wird er per Re-Correct durch das neue Profil
    /// geschickt: alte Eingabezeile via clearAllInput leeren, neue
    /// Korrektur einfuegen.
    ///
    /// Wichtige Regel: NICHT aktiv wenn gerade aufgenommen wird — das wuerde
    /// die Aufnahme-Anzeige stoeren. Der Mic-State wird waehrend des Re-
    /// Correct NICHT veraendert; stattdessen wird das geklickte Tile kurz
    /// orange als visueller Indikator (analog Windows #1956).
    private func switchProfile(_ newProfile: Int) {
        let oldProfile = activeProfile

        // Auto-Aktivierung: Klick auf ein Profil-Tile zeigt klare Absicht,
        // Gemini-Korrektur zu wollen. Falls G aus war, schalten wir es ein.
        var didAutoEnableGemini = false
        if !geminiEnabled, geminiClient != nil {
            geminiEnabled = true
            didAutoEnableGemini = true
            panel.setGeminiEnabled(geminiEnabled)
            tvoDebug("[App] Gemini auto-eingeschaltet durch Profil-Klick")
        }

        activeProfile = newProfile
        panel.setActiveProfile(newProfile)

        // Wenn gerade aufgenommen wird: nur Profil setzen, sonst nichts.
        if isRecording { return }
        // Gleiches Profil = no-op — AUSSER Gemini wurde gerade auto-aktiviert.
        if !didAutoEnableGemini && newProfile == oldProfile { return }
        guard geminiEnabled, let gemini = geminiClient else { return }
        guard let rawText = lastCorrectableRaw, !rawText.isEmpty else { return }

        // Visueller Indikator: das geklickte Tile waehrend des Re-Correct
        // orange faerben.
        panel.flashProfileTile(newProfile, processing: true)

        gemini.correctText(rawText, profile: newProfile) { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }
                switch result {
                case .success(let corrected):
                    var finalText = corrected
                    // Always-On-Wrappers wieder anwenden falls aktiv.
                    if self.alwaysOnActive {
                        let pre = AlwaysOnPrefixService.buildPre()
                        let post = AlwaysOnPrefixService.buildPost()
                        if !pre.isEmpty { finalText = pre + finalText }
                        if !post.isEmpty { finalText = finalText + post }
                    }
                    finalText += " ; "

                    // Eingabezeile vollstaendig loeschen, neuen Text paten.
                    // AutoEnter wird respektiert: ist der Enter-Toggle aktiv,
                    // wird die Frage direkt abgeschickt — sonst nur in die
                    // Befehlszeile kopiert.
                    let shouldAutoEnter = self.autoEnterEnabled
                    DispatchQueue.global(qos: .userInitiated).async {
                        TerminalController.clearAllInput()
                        usleep(120_000)
                        TerminalController.pasteText(finalText, autoEnter: shouldAutoEnter)
                    }
                    self.hasPastedText = !shouldAutoEnter
                    tvoDebug("[App] Re-Correct ok (\(finalText.count) chars, profile \(newProfile))")
                case .failure(let error):
                    tvoDebug("[App] Re-Correct error: \(error.localizedDescription)")
                }
                // Tile zurueck auf Standard-Look.
                self.panel.flashProfileTile(newProfile, processing: false)
            }
        }
    }

    /// Rechtsklick auf ein Profil-Tile: aktiviert Gemini falls aus, setzt
    /// das aktive Profil — fuehrt aber KEINEN Re-Correct durch. Der Whisper-
    /// Cache bleibt unveraendert und kann spaeter per Linksklick auf irgend-
    /// ein Profil-Tile noch durchgeschickt werden.
    private func switchProfileWithoutReCorrect(_ newProfile: Int) {
        if !geminiEnabled, geminiClient != nil {
            geminiEnabled = true
            panel.setGeminiEnabled(geminiEnabled)
            tvoDebug("[App] Gemini auto-eingeschaltet durch Profil-Rechtsklick")
        }
        activeProfile = newProfile
        panel.setActiveProfile(newProfile)
        tvoDebug("[App] Profil \(newProfile) aktiv (Rechtsklick — kein Re-Correct)")
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

    // MARK: - Launch-time Drive restore

    /// On launch, pull the latest backup from Google Drive and compare its
    /// `ExportedAt` timestamp against our local "last sync" mark. If the
    /// remote is newer (typical case: the user edited prompts on another
    /// machine), apply it as the authoritative state. Silent on every
    /// failure path — Drive not connected, network off, JSON malformed,
    /// etc. — because a startup check must never block the overlay UI.
    private func checkForRemoteBackupOnLaunch() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[App] launch-restore skipped (Drive not connected)")
            return
        }
        GoogleDriveBackupService.shared.downloadLatest { [weak self] result in
            switch result {
            case .failure(let e):
                tvoDebug("[App] launch-restore download failed: \(e.localizedDescription)")
            case .success(let json):
                guard let json = json else {
                    tvoDebug("[App] launch-restore: no backup on Drive yet")
                    return
                }
                guard let remoteDate = PromptBoardPanel.backupExportedAt(from: json) else {
                    tvoDebug("[App] launch-restore: no ExportedAt in backup")
                    return
                }
                let localDate = UserDefaults.standard.object(forKey: "pbLastBackupDate") as? Date
                    ?? .distantPast
                // Give a small grace window so a backup we uploaded ourselves
                // moments earlier doesn't trigger a self-restore race.
                if remoteDate > localDate.addingTimeInterval(2) {
                    DispatchQueue.main.async {
                        do {
                            try PromptBoardPanel.applyBackupJson(json)
                            UserDefaults.standard.set(remoteDate, forKey: "pbLastBackupDate")
                            tvoDebug("[App] launch-restore applied remote backup from \(remoteDate)")
                            // If the PromptBoard panel has already been created
                            // (lazy — only after ★ click), refresh it so the
                            // new data is visible immediately.
                            self?.promptBoardPanel?.refresh()
                        } catch {
                            tvoDebug("[App] launch-restore apply failed: \(error.localizedDescription)")
                        }
                    }
                } else {
                    tvoDebug("[App] launch-restore: local is up-to-date (remote=\(remoteDate), local=\(localDate))")
                }
            }
        }
    }

    // MARK: - Reset Timer

    private func scheduleReset(wasBtw: Bool = false) {
        resetTimer?.invalidate()
        resetTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: false) { [weak self] _ in
            guard let self = self else { return }
            // Defensiver Check gegen Race Condition (Windows #1956): wenn
            // gerade neu aufgenommen wird, Mic-Anzeige NICHT auf Idle
            // ueberschreiben. Sonst sieht der Benutzer mitten in der Aufnahme
            // ploetzlich das gelbe Idle-Mikrofon und denkt, er habe vergessen
            // zu druecken.
            if self.isRecording { return }
            if wasBtw {
                self.panel.setBtwMicState(.idle)
            } else {
                self.panel.setMicState(.idle)
            }
        }
    }

    // MARK: - Screenshot capture / paste (matches Windows #1912 + #1917)

    /// Capture the entire screen via /usr/sbin/screencapture and save the
    /// resulting PNG to ~/Pictures/Screenshots/. The path is remembered
    /// in `lastScreenshotPath` for the InsertScreenshotButton to paste.
    /// `-x` suppresses the shutter sound; the user just hears nothing
    /// and sees the green flash on success.
    private func takeScreenshot() {
        let homeDir = FileManager.default.homeDirectoryForCurrentUser
        let shotsDir = homeDir.appendingPathComponent("Pictures/Screenshots")
        do {
            try FileManager.default.createDirectory(at: shotsDir, withIntermediateDirectories: true)
        } catch {
            NSLog("[Screenshot] could not create %@: %@", shotsDir.path, error.localizedDescription)
            self.panel.flashScreenshotButton(success: false)
            return
        }

        // Filename with millisecond precision so rapid clicks produce
        // unique names (yyyy-MM-dd_HH-mm-ss-SSS). Defensive UUID suffix
        // if the same millisecond ever collides.
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd_HH-mm-ss-SSS"
        var filename = "screenshot_\(formatter.string(from: Date())).png"
        var fullURL = shotsDir.appendingPathComponent(filename)
        var collisionGuard = 0
        while FileManager.default.fileExists(atPath: fullURL.path), collisionGuard < 10 {
            collisionGuard += 1
            let suffix = String(UUID().uuidString.prefix(6))
            filename = "screenshot_\(formatter.string(from: Date()))_\(suffix).png"
            fullURL = shotsDir.appendingPathComponent(filename)
        }

        let task = Process()
        task.executableURL = URL(fileURLWithPath: "/usr/sbin/screencapture")
        // -x: no shutter sound. No -i, no -c — capture entire screen
        // straight to the file path. -t png is implicit by extension.
        task.arguments = ["-x", fullURL.path]

        do {
            try task.run()
            task.waitUntilExit()
        } catch {
            NSLog("[Screenshot] launch failed: %@", error.localizedDescription)
            self.panel.flashScreenshotButton(success: false)
            return
        }

        // Verify the file is on disk and non-empty. screencapture exit
        // code 0 doesn't always guarantee a successful write — a denied
        // screen-recording permission can produce a 0-byte file.
        let attrs = try? FileManager.default.attributesOfItem(atPath: fullURL.path)
        let size = (attrs?[.size] as? NSNumber)?.int64Value ?? 0
        if task.terminationStatus == 0,
           FileManager.default.fileExists(atPath: fullURL.path),
           size > 0 {
            self.lastScreenshotPath = fullURL.path
            NSLog("[Screenshot] saved %@ (%lld bytes)", fullURL.path, size)
            self.panel.flashScreenshotButton(success: true)
        } else {
            NSLog("[Screenshot] failed: status=%d, exists=%@, size=%lld",
                  task.terminationStatus,
                  FileManager.default.fileExists(atPath: fullURL.path) ? "yes" : "no",
                  size)
            // lastScreenshotPath stays UNCHANGED so the previous good
            // path is still available for Insert. Same as Windows.
            self.panel.flashScreenshotButton(success: false)
        }
    }

    /// Paste the absolute path of the last successful screenshot into
    /// the active terminal. Quotes the path if it contains spaces so
    /// shells (zsh, bash, fish) read it as a single argument.
    private func insertLastScreenshot() {
        guard let path = self.lastScreenshotPath else {
            NSLog("[InsertScreenshot] no screenshot taken yet")
            self.panel.flashInsertScreenshotButton(success: false)
            return
        }
        guard FileManager.default.fileExists(atPath: path) else {
            NSLog("[InsertScreenshot] file gone: %@", path)
            self.panel.flashInsertScreenshotButton(success: false)
            return
        }

        let toPaste = path.contains(" ") ? "\"\(path)\"" : path
        TerminalController.pasteText(toPaste, autoEnter: false)
        self.panel.flashInsertScreenshotButton(success: true)
    }

    // MARK: - Global Hotkeys (Carbon API)

    /// Registriert alle globalen Tastatur-Hotkeys. Wird einmal in
    /// `applicationDidFinishLaunching` aufgerufen, NACHDEM Panel und
    /// Services initialisiert sind. Carbon-API braucht keine Accessibility-
    /// Permission — die Hotkeys feuern auch wenn die App im Hintergrund ist.
    /// Bei Kollision mit System-Hotkeys (selten bei Cmd+Shift+R/S/I/E) gibt
    /// `RegisterEventHotKey` einen Fehlerstatus zurueck den `HotkeyRegistry`
    /// loggt — die App startet trotzdem.
    private func setupGlobalHotkeys() {
        let reg = HotkeyRegistry.shared

        // Voice-Toggle (Cmd+Shift+R) — Pendant zu Windows Alt+F12
        reg.register(keyCode: TVOHotkey.voiceToggle.keyCode,
                     modifiers: TVOHotkey.voiceToggle.modifiers) { [weak self] in
            self?.toggleRecording()
        }

        // Screenshot (Cmd+Shift+S) — Pendant zu Windows Strg+Alt+P
        reg.register(keyCode: TVOHotkey.screenshot.keyCode,
                     modifiers: TVOHotkey.screenshot.modifiers) { [weak self] in
            self?.takeScreenshot()
        }

        // Insert-Screenshot (Cmd+Shift+I) — Pendant zu Windows Strg+Alt+I
        reg.register(keyCode: TVOHotkey.insertScreenshot.keyCode,
                     modifiers: TVOHotkey.insertScreenshot.modifiers) { [weak self] in
            self?.insertLastScreenshot()
        }

        // Finder zum Release-Bundle (Cmd+Shift+E) — Pendant zu Windows Alt+F11
        reg.register(keyCode: TVOHotkey.openReleaseBundle.keyCode,
                     modifiers: TVOHotkey.openReleaseBundle.modifiers) { [weak self] in
            self?.openReleaseBundleFolder()
        }

        // Orientation umschalten (Cmd+Shift+O) — Dev-Hotkey solange der
        // OrientationToggleButton in der UI noch fehlt. Pendant zu Windows
        // OrientationToggleButton-Click.
        reg.register(keyCode: TVOHotkey.orientationToggle.keyCode,
                     modifiers: TVOHotkey.orientationToggle.modifiers) { [weak self] in
            self?.toggleOverlayOrientation()
        }

        // Collapsed-Pille togglen (Cmd+Shift+C) — Dev-Hotkey.
        reg.register(keyCode: TVOHotkey.collapsedToggle.keyCode,
                     modifiers: TVOHotkey.collapsedToggle.modifiers) { [weak self] in
            DispatchQueue.main.async {
                guard let panel = self?.panel else { return }
                tvoDebug("[App] toggleCollapsed isCollapsed=\(panel.isCollapsed)")
                panel.toggleCollapsed()
            }
        }

        // Settings-Dialog oeffnen (Cmd+Shift+,)
        reg.register(keyCode: TVOHotkey.openSettings.keyCode,
                     modifiers: TVOHotkey.openSettings.modifiers) { [weak self] in
            DispatchQueue.main.async { self?.openSettingsDialog() }
        }

        // Prompt-Hotkeys Cmd+1..9 — Pendant zu Windows Strg+1..9
        for digit in 1...9 {
            guard let combo = TVOHotkey.promptDigit(digit) else { continue }
            reg.register(keyCode: combo.keyCode, modifiers: combo.modifiers) { [weak self] in
                self?.pastePromptByHotkey(digit)
            }
        }

        // Prompt-Hotkeys Cmd+Opt+A..Z — Pendant zu Windows Win+Alt+A..Z
        for letter in "ABCDEFGHIJKLMNOPQRSTUVWXYZ" {
            guard let combo = TVOHotkey.promptLetter(letter) else { continue }
            reg.register(keyCode: combo.keyCode, modifiers: combo.modifiers) { [weak self] in
                self?.pastePromptByLetter(letter)
            }
        }

        NSLog("[App] Global hotkeys registered (Cmd+Shift+R/S/I/E/O/C + Cmd+1..9 + Cmd+Opt+A..Z)")
    }

    /// Oeffnet den Settings-Dialog (Cmd+Shift+,).
    /// Speichert die Felder in UserDefaults, ruft danach optional Drive-
    /// Connect/Disconnect-Aktionen aus.
    private func openSettingsDialog() {
        let dialog = SettingsDialog()
        dialog.loadFromDefaults()
        // Status der Drive-Verbindung anzeigen.
        let connected = GoogleDriveBackupService.shared.isAuthenticated()
        dialog.setGoogleStatus(connected ? "Verbunden" : "Nicht verbunden",
                               connected: connected)
        dialog.onSave = { values in
            tvoDebug("[App] Settings saved: groqLen=\(values.groqApiKey.count) " +
                     "geminiLen=\(values.geminiApiKey.count) " +
                     "autoHide=\(values.autoHide) horizontal=\(values.horizontalOrientation) " +
                     "persistPos=\(values.persistOverlayPosition)")
        }
        dialog.onGoogleConnect = { [weak dialog] in
            let d = UserDefaults.standard
            let cid = d.string(forKey: "GOOGLE_CLIENT_ID") ?? ""
            let cs  = d.string(forKey: "GOOGLE_CLIENT_SECRET") ?? ""
            if cid.isEmpty || cs.isEmpty {
                dialog?.setGoogleStatus("Bitte Client-ID + Secret eintragen + Speichern", connected: false)
                return
            }
            dialog?.setGoogleStatus("Browser oeffnet ... bitte autorisieren", connected: false)
            GoogleDriveBackupService.shared.connect(clientId: cid, clientSecret: cs) { result in
                DispatchQueue.main.async {
                    switch result {
                    case .success(let info):
                        dialog?.setGoogleStatus("Verbunden: \(info.email)", connected: true)
                        tvoDebug("[App] Drive-Connect OK email=\(info.email)")
                    case .failure(let err):
                        dialog?.setGoogleStatus("Fehler: \(err.localizedDescription)", connected: false)
                        tvoDebug("[App] Drive-Connect Fehler: \(err.localizedDescription)")
                    }
                }
            }
        }
        dialog.onGoogleDisconnect = { [weak dialog] in
            GoogleDriveBackupService.shared.signOut()
            dialog?.setGoogleStatus("Nicht verbunden", connected: false)
            tvoDebug("[App] Drive-Disconnect ausgefuehrt")
        }
        dialog.center()
        dialog.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }

    /// SaveButton-Click: aktuelle Position fuer die aktuelle Orientation
    /// persistent merken (Disketten-Symbol). Pendant zu Windows-Diskette.
    private func savePositionForCurrentOrientation() {
        guard let panel = self.panel else { return }
        let origin = panel.frame.origin
        switch panel.currentOrientation {
        case .vertical:
            panel.savedVerticalPosition = origin
            tvoDebug("[App] saved vertical position: \(origin)")
        case .horizontal:
            panel.savedHorizontalPosition = origin
            tvoDebug("[App] saved horizontal position: \(origin)")
        }
    }

    /// Dev-Helfer: Cmd+Shift+O togglet die Overlay-Orientation. Wird in
    /// Etappe 3+ durch den richtigen OrientationToggleButton in der UI
    /// abgeloest. Beam-Crossfade laeuft ueber OverlayPanel.beamToOrientation.
    private func toggleOverlayOrientation() {
        DispatchQueue.main.async { [weak self] in
            guard let panel = self?.panel else { return }
            let next: OverlayOrientation =
                (panel.currentOrientation == .vertical) ? .horizontal : .vertical
            tvoDebug("[App] toggleOrientation \(panel.currentOrientation.rawValue) -> \(next.rawValue)")
            panel.beamToOrientation(next)
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        HotkeyRegistry.shared.unregisterAll()
    }

    /// Holt den Prompt mit `hotkeyNumber == digit` aus der DB und fuegt
    /// dessen `effectiveText` in das aktive Terminal ein. Wenn kein Prompt
    /// dem Digit zugewiesen ist, passiert nichts (still — kein Beep, kein
    /// Alert, weil die Cmd+N-Kombi auch in vielen Apps eigene Bedeutungen
    /// hat und der User vielleicht gar nicht uns gemeint hat).
    /// Stub fuer Cmd+Opt+A..Z (Letter-Hotkeys). Das DB-Schema hat aktuell
    /// nur HotkeyNumber, kein HotkeyLetter — vollstaendige Implementation
    /// braucht eine DB-Migration in einer spaeteren Etappe. Bis dahin nur
    /// Log-Ausgabe damit der Hotkey registriert ist und das System lernt
    /// wo der User welche Buchstaben drueckt.
    private func pastePromptByLetter(_ letter: Character) {
        tvoDebug("[PromptHotkey] Cmd+Opt+\(letter) — letter-hotkey not yet wired to DB (HotkeyLetter-Spalte fehlt)")
    }

    private func pastePromptByHotkey(_ digit: Int) {
        do {
            guard let prompt = try PromptBoardStore.shared.promptByHotkey(digit) else {
                NSLog("[PromptHotkey] Cmd+%d — no prompt assigned", digit)
                return
            }
            let text = prompt.effectiveText
            NSLog("[PromptHotkey] Cmd+%d -> '%@' (len=%d)", digit, prompt.shortLabel, text.count)
            TerminalController.pasteText(text, autoEnter: autoEnterEnabled)
        } catch {
            NSLog("[PromptHotkey] DB error: %@", error.localizedDescription)
        }
    }

    /// Oeffnet den Release-Bundle-Ordner im Finder. Falls der Ordner noch
    /// nicht existiert (kein Release-Build gemacht), klettern wir den
    /// Pfad-Baum hoch bis zum naechsten existierenden Eltern-Verzeichnis.
    /// Pendant zur Windows-Methode `OpenReleaseBundleFolder`.
    private func openReleaseBundleFolder() {
        let home = FileManager.default.homeDirectoryForCurrentUser.path
        var folder = "\(home)/proggs/BestJournalAndroid/app/build/outputs/bundle/release"

        var checked = folder
        while !checked.isEmpty && !FileManager.default.fileExists(atPath: checked) {
            let parent = (checked as NSString).deletingLastPathComponent
            if parent == checked || parent.isEmpty { break }
            checked = parent
        }

        guard FileManager.default.fileExists(atPath: checked) else {
            NSLog("[OpenReleaseBundle] path not found: %@", folder)
            return
        }
        folder = checked

        let url = URL(fileURLWithPath: folder)
        NSWorkspace.shared.open(url)
        NSLog("[OpenReleaseBundle] opened: %@", folder)
    }
}

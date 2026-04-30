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
    private var geminiEnabled = false
    private var autoEnterEnabled = true
    private var alwaysOnActive = false
    private var promptBoardPanel: PromptBoardPanel?

    // Path of the last screenshot captured by the ScreenshotButton.
    // The InsertScreenshotButton paste this path into the active terminal.
    // Stays UNCHANGED on a failed capture so the user keeps the last
    // good path memorised — matches Windows _lastScreenshotPath behaviour.
    private var lastScreenshotPath: String?

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

        panel.onXClicked = { [weak self] in
            guard let self = self else { return }
            tvoDebug("[App] onXClicked panelLevel=\(self.panel.level.rawValue) active=\(NSApp.isActive)")
            // No cooldown — rapid ✕-ing fires ClearLine every time,
            // matching the Windows Voice Overlay behavior.
            self.panel.flashXButton()
            self.clearLine()
        }
        panel.onUltrathinkClicked = { [weak self] in self?.toggleUltrathink() }
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

        setupStatusItem()

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

    private func toggleUltrathink() {
        alwaysOnActive.toggle()
        panel.setUltrathinkEnabled(alwaysOnActive)
        if alwaysOnActive {
            showPromptBoardPanel()
        } else {
            hidePromptBoardPanel()
        }
        NSLog("PromptBoard panel %@", alwaysOnActive ? "OPEN" : "CLOSED")
    }

    private func showPromptBoardPanel() {
        if promptBoardPanel == nil {
            let p = PromptBoardPanel()
            p.onInsertText = { [weak self] text in
                guard let self = self, !text.isEmpty else { return }
                tvoDebug("[App] onInsertText textLen=\(text.count) autoEnter=\(self.autoEnterEnabled)")
                TerminalController.pasteText(text, autoEnter: self.autoEnterEnabled)
            }
            p.onInputSubmit = { [weak self] middleText in
                self?.handleInputSubmit(middleText)
            }
            // Wird gefeuert nachdem der Benutzer einen Historie-Eintrag im
            // Editor-Sheet gespeichert hat — Cloud-Upload anstossen, damit
            // die Aenderung auch auf der Windows-Seite sichtbar wird.
            p.onHistorySyncRequested = { [weak self] in
                self?.uploadHistoryToCloud()
            }
            // Right-click drag on the panel itself moves both floating
            // windows together: panel slides under the cursor (already
            // done inside the panel), and we slide the pillar by the
            // same delta so they stay glued. The pillar saves its own
            // position so the new spot is restored on next launch.
            p.onPanelDragged = { [weak self] panelOrigin in
                guard let self = self else { return }
                let pillarSize = self.panel.frame.size
                // Pillar sits to the right of the panel with a 4 px seam
                // (matches PromptBoardPanel.dock(rightOf:)).
                let pillarOrigin = NSPoint(
                    x: panelOrigin.x + p.frame.size.width + 4,
                    y: panelOrigin.y)
                self.panel.setFrame(NSRect(origin: pillarOrigin, size: pillarSize),
                                    display: true)
                // Persist so the manual position survives an app restart.
                self.panel.savePillarPosition()
            }
            promptBoardPanel = p
        }
        guard let p = promptBoardPanel else { return }
        p.dock(rightOf: panel)
        p.orderFrontRegardless()
        p.refresh()
    }

    private func hidePromptBoardPanel() {
        promptBoardPanel?.orderOut(nil)
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
            if wasBtw {
                self?.panel.setBtwMicState(.idle)
            } else {
                self?.panel.setMicState(.idle)
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
}

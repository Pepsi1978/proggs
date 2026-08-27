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
    /// Selbstheilender Sichtbarkeits-Poll (0,7 s) — holt das Overlay zurueck,
    /// wenn die Ziel-App real im Vordergrund steht, es aber faelschlich
    /// versteckt wurde. Windows-Pendant: `_foregroundReclaimTimer`.
    private var foregroundReclaimTimer: Timer?
    private var audioRecorder: AudioRecorder!
    private var groqClient: GroqWhisperClient!
    private var geminiClient: GeminiClient?
    private var config: Config!
    private var statusItem: NSStatusItem!

    // All state flags are only read/written on the main thread (Fix 4)
    private var isRecording = false
    private var isProcessing = false
    private var deploymentPending = false
    private var geminiEnabled = false // Default = Gemini-Korrektur AUS (Whisper-roh), KEIN Profil aktiv (Frank-Wunsch 2026-06-22: beim Start kein Profil voreingestellt). Profil-Klick oder G-Button schaltet Gemini ein. Ohne Gemini-API-Key bleibt es ohnehin false.
    private var autoEnterEnabled = true
    private let autoEnterServer = AutoEnterStatusServer()
    private var autoHide: AutoHideController?
    private var ptt: PushToTalkController?
    /// Aufnahme-Signaltoene wie unter Windows (880 Hz Start, 660+440 Hz Stop)
    /// statt der bisherigen System-Pieptoene.
    private let recordingCuePlayer = RecordingCuePlayer()
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

    // Kurzer hide-delay timer (Frank-Wunsch 2026-06-01: 0.4s, frueher 5s wie
    // Windows _hideDelayTimer #1913). Beim Wechsel weg vom Terminal wird das
    // Panel nach 0.4s ausgeblendet — das VTO soll nur ueber dem Terminal/CLI
    // haengen, nicht im Browser nachhaengen. Wird das Terminal innerhalb der
    // 0.4s wieder aktiv, wird der Timer gecancelt (kein Flackern).
    private var hideDelayTimer: Timer?
    private var isBtwRecording = false
    private var hasPastedText = false
    private var lastRawTranscript: String?
    private var resetTimer: Timer?

    // ── Profile-Korrektur (Windows-Port #1957..#1964) ──
    // 1 = Standard, 2 = Programmierung, 3 = Meta-Intelligenz,
    // 4-10 = numerische Slots (frei belegbar). Default = 0 = KEIN Profil
    // voreingestellt (Frank-Wunsch 2026-06-22): beim Start kein Tile aktiv.
    private var activeProfile: Int = 0
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

        // Cloud-Merge der Prompt-Zwischenspeicher-Slots: gleiche Idee wie
        // bei der Historie — einmal beim Start den Stand vom anderen Geraet
        // abholen. Fire-and-forget.
        mergeSlotsFromCloudOnLaunch()

        // Persoenliches Vokabular-Woerterbuch ebenfalls beim Start vom anderen
        // Geraet abholen (non-destruktive Vereinigung — kein Wort geht verloren).
        mergeVocabularyFromCloudOnLaunch()

        // Gemini-Prompts + Woerterbuch-Schalter/Praeambel beim Start vom Backup
        // holen (LWW per Timestamp). Fire-and-forget.
        GeminiPromptSync.trySyncFromCloud()

        if !TerminalController.checkAccessibility() {
            NSLog("Accessibility permission missing")
        }

        AVCaptureDevice.requestAccess(for: .audio) { granted in
            if !granted { NSLog("Microphone permission denied") }
        }

        // Init clients
        audioRecorder = AudioRecorder()
        // Waveform-Animation waehrend Recording (1:1 Windows LevelChanged).
        audioRecorder.onLevel = { [weak self] level in
            self?.panel?.pushWaveformLevel(level)
        }
        groqClient = GroqWhisperClient(apiKey: config.groqApiKey)
        if let geminiKey = config.geminiApiKey, !geminiKey.isEmpty {
            geminiClient = GeminiClient(apiKey: geminiKey)
        } else {
            geminiEnabled = false
        }
        if GeminiClient.upgradeLegacyMinimalInterventionPrompts() {
            GeminiPromptSync.tryUpload()
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
        autoEnterServer.busyProvider = { [weak self] in
            DispatchQueue.main.sync {
                guard let self = self else { return false }
                return self.isRecording || self.isProcessing
            }
        }
        autoEnterServer.deploymentPrepareHandler = { [weak self] in
            DispatchQueue.main.sync {
                guard let self = self, !self.isRecording, !self.isProcessing else { return false }
                self.deploymentPending = true
                return true
            }
        }
        autoEnterServer.deploymentReleaseHandler = { [weak self] in
            DispatchQueue.main.async { self?.deploymentPending = false }
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

        // Beim App-Start die zuletzt verwendete Orientation aus AppSettings
        // laden — 1:1 wie Windows (`startupHorizontal` aus _settings.Orientation).
        if let stored = try? PromptBoardStore.shared.settings() {
            // Persistierte Positionen laden, falls PersistOverlayPosition=true.
            if stored.persistOverlayPosition {
                if let x = stored.overlayVerticalLeft, let y = stored.overlayVerticalTop {
                    panel.savedVerticalPosition = NSPoint(x: x, y: y)
                }
                if let x = stored.overlayHorizontalLeft, let y = stored.overlayHorizontalTop {
                    panel.savedHorizontalPosition = NSPoint(x: x, y: y)
                }
            }
            if stored.orientation == "horizontal" {
                DispatchQueue.main.async { [weak self] in
                    self?.panel?.beamToOrientation(.horizontal)
                }
            }
        }

        // Auto-Hide-Controller starten (5s Inaktivitaet → Collapsed-Pille).
        // PTT: NSEvent-Monitor fuer KeyUp-Detection (Hold ≥500ms = PTT-Release).
        ptt = PushToTalkController()
        ptt?.onPTTRelease = { [weak self] in
            if self?.isRecording == true { self?.stopRecording() }
        }

        autoHide = AutoHideController(panel: panel)
        // 1:1 Windows (ScheduleCollapse): NUR eine laufende Aufnahme haelt das
        // Overlay offen. Waehrend der Transkription (Processing) klappt Windows
        // bereits ein — sie laeuft im Hintergrund weiter. Solange hier auch
        // `isProcessing` blockte, blieb das macOS-Overlay bis zum Eintreffen des
        // Whisper-Texts stehen und wirkte spuerbar traeger.
        autoHide?.busyProvider = { [weak self] in
            guard let self = self else { return false }
            return self.isRecording
        }
        // Kein Auto-Collapse solange der Benutzer im Prompt-System arbeitet
        // (Board, Eingabe oder Historie sichtbar) — vertikal wie horizontal.
        autoHide?.promptSurfaceVisibleProvider = { [weak self] in
            self?.isPromptSurfaceVisible() ?? false
        }
        // AutoHide-Enabled-State aus AppSettings laden (1:1 Windows-AutoHide).
        if let stored = try? PromptBoardStore.shared.settings() {
            autoHide?.enabled = stored.autoHide
        }
        panel.onPillarMoved = { [weak self] in
            // Beim Verschieben des VTO das angedockte Prompt-Fenster mitziehen,
            // damit es IMMER am VTO klebt (Frank-Wunsch 2026-05-28).
            guard let self = self else { return }
            if self.inputSoloDock,
               let input = self.promptBoardPanel?.currentInputPanel {
                // Solo-Dock: nur die Eingabe haengt am VTO — sie nachziehen.
                input.dockToOverlay(self.panel)
            } else {
                // Board-Modus: das Board nachziehen.
                self.promptBoardPanel?.dock(rightOf: self.panel)
            }
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
                    if self.inputSoloDock {
                        // Solo-Dock: nur die Eingabe haengt am Pillar, das
                        // Board ist bewusst unsichtbar. Bei Rueckkehr ins
                        // Terminal NUR die Eingabe zurueckholen — das Board
                        // NICHT sichtbar machen, sonst taucht es ungewollt auf
                        // (Regression-Fix #1170). Das Board selbst bleibt
                        // orderOut'd.
                        if let input = p.currentInputPanel {
                            input.dockToOverlay(self.panel)
                            input.orderFrontRegardless()
                        }
                    } else {
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
        }
        appWatcher.onTerminalDeactivated = { [weak self] in
            DispatchQueue.main.async {
                guard let self = self else { return }
                tvoDebug("[App] onTerminalDeactivated isRec=\(self.isRecording) isProc=\(self.isProcessing)")
                if !self.isRecording && !self.isProcessing {
                    // Kurze hide-delay (Frank-Wunsch 2026-06-01: 0.4s statt 5s).
                    // Das VTO soll WIRKLICH nur ueber dem Terminal/CLI haengen —
                    // beim Wechsel in den Browser o.ae. quasi sofort verschwinden,
                    // nicht 5s nachhaengen. Die 0.4s sind nur ein Flacker-Schutz
                    // gegen sehr kurze Fokus-Glitches (z.B. kurzes Aufblitzen eines
                    // Hilfsfensters); 0s wuerde bei jedem Mini-Fokuswechsel flackern.
                    // Recording/Processing pausiert das Verstecken weiterhin (s.o.).
                    self.hideDelayTimer?.invalidate()
                    self.hideDelayTimer = Timer.scheduledTimer(
                        withTimeInterval: 0.4, repeats: false
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

        // ── Foreground-Reclaim-Poll (Portierung von Windows) ──
        // Das Einblenden haengt sonst allein an den Aktivierungs-Notifications.
        // Blitzt kurz ein Fremdfenster auf (Mitteilung, Spotlight, ein
        // Berechtigungs-Dialog) ODER geht eine Notification verloren, laeuft
        // `onTerminalDeactivated` — und das Overlay bleibt weg, obwohl das CLI
        // weiter vorne steht. Fuer Sprach-/Klick-Bedienung muss es aber sichtbar
        // sein, solange das CLI zu sehen ist. Der Poll prueft daher die reale
        // vorderste App: ist es eine Ziel-App und das Overlay unsichtbar, kommt
        // es sofort zurueck. Steht eine echte Fremd-App vorne, passiert nichts —
        // kein Widerspruch zum schnellen Verstecken (0,4 s). 0,7 s sind schnell
        // genug, dass die Luecke kaum auffaellt, und der Poll ist billig
        // (ein NSWorkspace-Zugriff, kein Fenster-Scan).
        foregroundReclaimTimer?.invalidate()
        foregroundReclaimTimer = Timer.scheduledTimer(withTimeInterval: 0.7, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            guard !self.panel.isVisible else { return }
            guard self.appWatcher.isTargetAppFrontmost() else { return }
            tvoDebug("[App] Foreground-Reclaim: Ziel-App ist vorne, Overlay war weg -> wieder einblenden")
            self.hideDelayTimer?.invalidate()
            self.hideDelayTimer = nil
            self.appWatcher.onTerminalActivated?()
        }
    }

    // MARK: - Status Bar

    private func setupStatusItem() {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        if let button = statusItem.button {
            button.image = NSImage(systemSymbolName: "mic.circle", accessibilityDescription: "Voice Overlay")
        }

        let menu = NSMenu()
        // Version + Bau-Zeitpunkt dauerhaft sichtbar (Regel version-bump-visible-always):
        // beides direkt aus der Info.plist abgeleitet, damit die Anzeige nie von der
        // Quelle abweichen kann. Der Eintrag ist bewusst nicht anklickbar.
        let info = Bundle.main.infoDictionary ?? [:]
        let versionText = "Version \(info["CFBundleShortVersionString"] as? String ?? "?")"
            + " (\(info["BuildTimestamp"] as? String ?? "unbekannt"))"
        let versionItem = NSMenuItem(title: versionText, action: nil, keyEquivalent: "")
        versionItem.isEnabled = false
        menu.addItem(versionItem)
        menu.addItem(NSMenuItem.separator())
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
        guard !deploymentPending else {
            tvoDebug("[App] recording blocked: overlay deployment is reserved")
            NSSound.beep()
            return
        }
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
            // Windows: waehrend einer Aufnahme ist das Overlay IMMER ausgeklappt
            // (die Welle soll sichtbar sein). Der Idle-Timer pausiert solange.
            autoHide?.expandForRecording()
            autoHide?.suspend()
            recordingCuePlayer.playStart()
        } catch AudioRecorder.RecorderError.alreadyRecording {
            // Es laeuft schon eine Aufnahme (z.B. aus dem Prompt-Editor). Die
            // NICHT abwuergen und auch keinen Fehlertext ins Terminal schreiben
            // — nur den eigenen Start verwerfen und die Anzeige zuruecksetzen.
            tvoDebug("[App] Start verworfen: es laeuft bereits eine Aufnahme")
            isBtwRecording = false
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

        // Windows ruft ScheduleCollapse() genau hier — beim Uebergang von
        // Recording auf Processing. Das Overlay verschwindet also direkt nach
        // dem Sprechen, waehrend die Transkription noch laeuft.
        autoHide?.scheduleCollapseSoon()

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

            // Stopp-Signal: absteigendes Zwei-Ton-Signal (660 Hz -> 440 Hz),
            // exakt wie unter Windows. Der Player spielt beide Toene in EINEM
            // Puffer — kein zweiter Timer noetig, kein Auseinanderdriften.
            DispatchQueue.main.async { self.recordingCuePlayer.playStop() }

            self.groqClient.transcribe(fileURL: fileURL) { [weak self] result in
                try? FileManager.default.removeItem(at: fileURL)

                DispatchQueue.main.async {
                    guard let self = self else { return }
                    switch result {
                    case .success(let transcript):
                        #if DEBUG
                        NSLog("Transcript: %@", transcript)
                        #endif
                        // Stille-Schutz (Schicht 1/2): kein Sprachinhalt / alles als Halluzination
                        // gefiltert -> NICHTS einfuegen (kein einsames " ; "), Cache nicht mit
                        // Leerstring ueberschreiben, Aufnahme still als erledigt abschliessen (.idle).
                        if transcript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            self.isProcessing = false
                            if wasBtw { self.panel.setBtwMicState(.idle) } else { self.panel.setMicState(.idle) }
                            return
                        }
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
            board.routeVoiceTextToInput(text, autoSubmit: autoEnterEnabled)
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
        geminiEnabled = false
        panel.setGeminiEnabled(false)
        tvoDebug("[App] Gemini aus, Whisper-Rohmodus durch W-Button aktiv")
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
                PromptHistoryStore.shared.loadAll { local in
                    let merged = PromptHistoryStore.merge(local: local, cloudJson: cloudJson)
                    if merged == local {
                        tvoDebug("[App] cloud history merge: no changes")
                        return
                    }
                    PromptHistoryStore.shared.replaceAll(entries: merged) {
                        tvoDebug("[App] cloud history merged: +\(merged.count - local.count) entries")
                        self?.uploadHistoryToCloud()
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
            notifyDriveDisconnectedOnce()
            return
        }
        let json = PromptHistoryStore.shared.rawJsonFromDisk()
        GoogleDriveBackupService.shared.uploadHistory(json: json) { [weak self] result in
            switch result {
            case .success:
                tvoDebug("[App] history uploaded to cloud")
                self?.driveDisconnectedNotified = false
                PromptBoardPanel.recordSyncNow()
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

    /// Holt die Cloud-Slots beim App-Start und mergt sie mit dem lokalen
    /// Stand (pro Nummer gewinnt der juengste `updatedAt` — auch Tombstones).
    /// Fire-and-forget, Fehler nur in den Debug-Log.
    private func mergeSlotsFromCloudOnLaunch() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[App] slot cloud merge skipped: drive not connected")
            return
        }
        GoogleDriveBackupService.shared.downloadSlots { [weak self] result in
            switch result {
            case .failure(let e):
                tvoDebug("[App] slot cloud download failed: \(e.localizedDescription)")
            case .success(let cloud):
                guard let cloudJson = cloud, !cloudJson.isEmpty else {
                    tvoDebug("[App] no cloud slots yet — nothing to merge")
                    return
                }
                PromptSlotStore.shared.loadEntries { local in
                    let merged = PromptSlotStore.merge(local: local, cloudJson: cloudJson)
                    PromptSlotStore.shared.replaceAll(entries: merged) {
                        tvoDebug("[App] cloud slots merged")
                        self?.promptBoardPanel?.reloadSlots()
                    }
                }
            }
        }
    }

    /// Laedt die lokale prompt-slots.json SOFORT zu Drive hoch. Wird nach
    /// jedem Speichern UND jedem Loeschen eines Slots aufgerufen (Frank-Wunsch:
    /// direkt nach Speichern und Loeschen syncen). Fire-and-forget.
    /// Einmal pro Session: warnt der Benutzer sichtbar, dass Drive nicht
    /// verbunden ist und deshalb NICHTS gesichert wird. Frank-Vorfall
    /// 2026-06-05: der Token war seit 01.06 weg (OAuth-Testing-Modus laeuft
    /// nach 7 Tagen ab), der Sync uebersprang aber still — tagelang unbemerkt.
    private var driveDisconnectedNotified = false
    private func notifyDriveDisconnectedOnce() {
        guard !driveDisconnectedNotified else { return }
        driveDisconnectedNotified = true
        let n = NSUserNotification()
        n.title = "Drive-Sync pausiert"
        n.informativeText = "Google Drive ist nicht verbunden — Prompts und Slots werden NICHT gesichert. Bitte im Promptboard (Stern → Einstellungen) neu verbinden."
        NSUserNotificationCenter.default.deliver(n)
    }

    private func uploadSlotsToCloud() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[App] slot upload skipped: drive not connected")
            notifyDriveDisconnectedOnce()
            return
        }
        let json = PromptSlotStore.shared.rawJsonFromDisk()
        GoogleDriveBackupService.shared.uploadSlots(json: json) { [weak self] result in
            switch result {
            case .success:
                tvoDebug("[App] slots uploaded to cloud")
                self?.driveDisconnectedNotified = false
                // Sync-Zeitstempel GARANTIERT persistieren (auch wenn das Board
                // gerade nicht offen/instanziiert ist) + UI refreshen wenn offen.
                PromptBoardPanel.recordSyncNow()
                self?.promptBoardPanel?.markSyncedNow()
            case .failure(let e):
                tvoDebug("[App] slot upload failed: \(e.localizedDescription)")
            }
        }
    }

    // ── Persoenliches Vokabular-Woerterbuch (Drive-Sync, non-destruktiv) ──
    // SK-Datei (gleicher Ordner wie die Korrektur-Prompts); auf einem Geraet
    // teilen sich beide Overlays dieselbe Datei.
    private static var personalVocabularyURL: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("SK/VoiceOverlays/personal-vocabulary.txt")
    }

    /// Reine VEREINIGUNG zweier Vokabular-Texte (kein Wort geht je verloren).
    /// Dedupliziert case-insensitiv, lokale Reihenfolge zuerst.
    private static func mergeVocabularies(_ localText: String, _ cloudText: String) -> String {
        var seen = Set<String>()
        var out: [String] = []
        for text in [localText, cloudText] {
            let normalized = text.replacingOccurrences(of: "\r\n", with: "\n")
            for rawLine in normalized.components(separatedBy: "\n") {
                let line = rawLine.trimmingCharacters(in: .whitespaces)
                if line.isEmpty { continue }
                if seen.insert(line.lowercased()).inserted { out.append(line) }
            }
        }
        return out.joined(separator: "\n")
    }

    /// Holt das Cloud-Vokabular beim App-Start und VEREINIGT es mit dem lokalen
    /// Stand. Fire-and-forget. Erstes Geraet ohne Cloud-Stand saet den lokalen.
    private func mergeVocabularyFromCloudOnLaunch() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[App] vocab cloud merge skipped: drive not connected")
            return
        }
        GoogleDriveBackupService.shared.downloadVocabulary { [weak self] result in
            switch result {
            case .failure(let e):
                tvoDebug("[App] vocab cloud download failed: \(e.localizedDescription)")
            case .success(let cloud):
                let url = AppDelegate.personalVocabularyURL
                let local = (try? String(contentsOf: url, encoding: .utf8)) ?? ""
                guard let cloud = cloud else {
                    // Noch kein Cloud-Stand: lokalen Stand als Saat hochladen.
                    if !local.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        self?.uploadVocabularyToCloud()
                    }
                    return
                }
                let merged = AppDelegate.mergeVocabularies(local, cloud)
                if merged.trimmingCharacters(in: .whitespacesAndNewlines)
                    != local.trimmingCharacters(in: .whitespacesAndNewlines) {
                    do {
                        try FileManager.default.createDirectory(
                            at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
                        try (merged + "\n").write(to: url, atomically: true, encoding: .utf8)
                        tvoDebug("[App] cloud vocabulary merged")
                        // Vereinten Stand zurueck in die Cloud, damit alle Geraete konvergieren.
                        self?.uploadVocabularyToCloud()
                    } catch {
                        tvoDebug("[App] vocab merge write failed: \(error.localizedDescription)")
                    }
                }
            }
        }
    }

    /// Laedt das lokale Vokabular-Woerterbuch SOFORT zu Drive hoch. Wird nach dem
    /// Speichern im Settings-Dialog aufgerufen. Aktualisiert den sichtbaren
    /// Sync-Zeitstempel (wie die Slots) — Frank-Wunsch.
    private func uploadVocabularyToCloud() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[App] vocab upload skipped: drive not connected")
            return
        }
        let url = AppDelegate.personalVocabularyURL
        guard let text = try? String(contentsOf: url, encoding: .utf8), !text.isEmpty else { return }
        GoogleDriveBackupService.shared.uploadVocabulary(text: text) { [weak self] result in
            switch result {
            case .success:
                tvoDebug("[App] vocabulary uploaded to cloud")
                PromptBoardPanel.recordSyncNow()
                self?.promptBoardPanel?.markSyncedNow()
            case .failure(let e):
                tvoDebug("[App] vocab upload failed: \(e.localizedDescription)")
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
    /// Seit 2026-05-28 (Frank-Wunsch): erster Klick oeffnet ZUERST die
    /// Prompt-Eingabe direkt am Pillar (Solo-Dock). Das PromptBoard wird
    /// intern instanziiert, bleibt aber unsichtbar — der Solo-Dock-Stern in
    /// der Eingabe-Toolbar holt es bei Bedarf nach vorne. Zweiter Klick auf
    /// diesen Pillar-Stern schliesst alles (Eingabe + Board).
    private func toggleUltrathink() {
        alwaysOnActive.toggle()
        panel.setUltrathinkEnabled(alwaysOnActive)
        if alwaysOnActive {
            showPromptInputDockedToOverlay()
        } else {
            hidePromptStack()
        }
        NSLog("PromptBoard panel %@", alwaysOnActive ? "OPEN" : "CLOSED")
    }

    /// True solange irgendein Teil des Prompt-Systems sichtbar ist (Board,
    /// Eingabe oder Historie). Der AutoHideController nutzt das, um das VTO
    /// NICHT einzuklappen waehrend der Benutzer im Prompt-Board / in der
    /// Eingabe (inkl. Menues/Untermenues) arbeitet — vertikal wie horizontal.
    private func isPromptSurfaceVisible() -> Bool {
        guard let board = promptBoardPanel else { return false }
        return board.isAnyPromptSurfaceVisible
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
        // KRITISCH (Frank-Bug 2026-06-05 "soll immer angedockt sein"): Wenn das
        // Overlay eingeklappt ist (Mic-Pille 84x84, z.B. nach langer Nicht-
        // Nutzung), ZUERST vollstaendig ausklappen und ERST DANN andocken.
        // Sonst dockt die Eingabe an die collapsed-Geometrie, die sich direkt
        // danach (beamToExpanded) wieder aendert → die Eingabe landet weit
        // links statt sauber 4px am Pillar.
        if panel.isCollapsed {
            tvoDebug("[SoloDock] Pillar collapsed → erst ausklappen, dann andocken")
            panel.beamToExpanded { [weak self] in
                self?.dockPromptInputToPillar()
            }
        } else {
            dockPromptInputToPillar()
        }
    }

    private func dockPromptInputToPillar() {
        ensurePromptBoardInstance()
        guard let board = promptBoardPanel else { return }
        // Board im Solo-Dock-Modus garantiert unsichtbar halten UND ihm eine
        // valide Position relativ zum Pillar geben, BEVOR openInputPanel-
        // Externally() das Eingabefeld intern via dock(leftOf:board)
        // positioniert. Sonst stuende das Board noch bei (0,0) und die Eingabe
        // wuerde kurz in der Bildschirmecke aufflackern (Fix #1170).
        board.orderOut(nil)
        board.dock(rightOf: self.panel)
        board.refresh()
        board.openInputPanelExternally()
        wireInputPanelCallbacks()
        guard let input = board.currentInputPanel else { return }
        inputSoloDock = true
        input.dockToOverlay(self.panel)
        input.setSoloDockState(true)
        installSoloDockDragHandler(on: board)
        tvoDebug("[SoloDock] pillar=\(self.panel.frame) input=\(input.frame) collapsed=\(self.panel.isCollapsed)")
        input.makeKeyAndOrderFront(nil)
        // Sicherheitsnetz: kurz nach allen Layout-/Beam-Animationen nochmal
        // exakt an den Pillar andocken — falls sich die Pillar-Geometrie direkt
        // nach dem Oeffnen noch geaendert hat (Race mit beamFadeIn).
        redockInputToPillarAfterSettle()
    }

    /// Dockt die Eingabe nach ~0.35s nochmal exakt an den Pillar — Sicherheits-
    /// netz gegen Geometrie-Aenderungen direkt nach dem Oeffnen (Aufklapp-
    /// Animation, Layout-Settle). Greift nur wenn der Solo-Dock-Modus noch aktiv
    /// ist.
    private func redockInputToPillarAfterSettle() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { [weak self] in
            guard let self = self, self.inputSoloDock,
                  let input = self.promptBoardPanel?.currentInputPanel else { return }
            input.dockToOverlay(self.panel)
            tvoDebug("[SoloDock] re-dock settle: input=\(input.frame)")
        }
    }

    /// Installiert den Solo-Dock-Drag-Handler: beim Ziehen der Eingabe wird der
    /// Pillar DIREKT verschoben (auf den sichtbaren Bildschirm geklemmt) und die
    /// Eingabe nachgezogen — NICHT ueber die versteckte, veraltete Board-Position.
    /// Behebt Frank-Bug 2026-06-05 (Pillar sprang beim Ziehen der Eingabe rechts
    /// aus dem Monitor). Wird an beiden Solo-Dock-Einstiegen aufgerufen.
    private func installSoloDockDragHandler(on board: PromptBoardPanel) {
        board.soloDockDragHandler = { [weak self] dx, dy in
            guard let self = self,
                  let input = self.promptBoardPanel?.currentInputPanel else { return }
            var origin = self.panel.frame.origin
            origin.x += dx
            origin.y += dy
            let clamped = OverlayPanel.clampFrameToVisibleScreen(
                NSRect(origin: origin, size: self.panel.frame.size))
            self.panel.setFrame(clamped, display: true)
            self.panel.savePillarPosition()
            input.dockToOverlay(self.panel)
        }
    }

    /// Schliesst sowohl Eingabe als auch Board (z.B. wenn der Pillar-Stern
    /// erneut geklickt wird = Ultrathink off).
    private func hidePromptStack() {
        promptBoardPanel?.closeInputPanelExternally()
        promptBoardPanel?.orderOut(nil)
        promptBoardPanel?.soloDockDragHandler = nil
        // Solo-Dock-Zustand zuruecksetzen, damit er nach dem Schliessen immer
        // konsistent ist (Fix #1170).
        inputSoloDock = false
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
            installSoloDockDragHandler(on: board)
            redockInputToPillarAfterSettle()
        } else {
            // Eingabe-Stern (Frank-Wunsch 2026-05-28): die Prompt-Eingabe
            // SCHLIESSEN und nur das Board zeigen. Vorher rueckte die Eingabe
            // an den linken Board-Rand und blieb offen — der Benutzer will sie
            // zu. Der Text bleibt erhalten (closeInputPanel macht nur orderOut,
            // kein Verwerfen); der Board-Stern holt die Eingabe bei Bedarf
            // wieder zurueck via applySoloDockMode(true).
            inputSoloDock = false
            board.soloDockDragHandler = nil
            board.closeInputPanelExternally()
            board.dock(rightOf: self.panel)
            board.orderFrontRegardless()
            board.refresh()
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
        p.onSlotsSyncRequested = { [weak self] in
            self?.uploadSlotsToCloud()
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
        // Slot-Summary: 6-8-Wort-Zusammenfassung fuer den Hover-Tooltip.
        // Best-effort — kein Gemini-Key -> leerer String -> Tooltip-Fallback.
        input.onGenerateSlotSummary = { [weak self] text, completion in
            guard let self = self, let gemini = self.geminiClient else {
                completion("")
                return
            }
            gemini.generateSlotSummary(text) { summary in completion(summary) }
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
        geminiEnabled = true
        if activeProfile == 0 { activeProfile = 1 }
        panel.setGeminiEnabled(true)
        panel.setActiveProfile(activeProfile)
        tvoDebug("[App] Gemini durch G-Button aktiv, Profil \(self.activeProfile)")
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
        // Waehrend einer laufenden Aufnahme/Verarbeitung schaltet der Enter-
        // Button NUR den Auto-Enter-Toggle um — KEIN Return ans Terminal, kein
        // Aufnahme-Stopp. So kann der Benutzer mitten beim Sprechen entscheiden,
        // ob der fertige Text gesendet (orange) oder nur ins Eingabefeld
        // eingefuegt (dunkel) werden soll, ohne dass etwas Sofortiges passiert.
        // (Frank-Wunsch 2026-06-20.)
        if isRecording || isProcessing || isBtwRecording {
            autoEnterEnabled.toggle()
            panel.setAutoEnterEnabled(autoEnterEnabled)
            return
        }
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
        // Datenverlust-Schutz (Portierung von Windows "Protect launch restore
        // from local changes"): weicht der lokale Stand vom zuletzt
        // synchronisierten ab, gibt es hier ungesicherte Aenderungen. Ein
        // Restore wuerde sie ueberschreiben — also gar nicht erst anfangen.
        // Beim allerersten Start (noch kein Fingerabdruck) darf er laufen.
        let localBeforeDownload = PromptBoardPanel.currentBackupFingerprint()
        if let lastSynced = PromptBoardPanel.readLastSyncFingerprint(),
           let localNow = localBeforeDownload,
           localNow != lastSynced {
            tvoDebug("[App] launch-restore skipped (lokale PromptBoard-Aenderungen sind noch nicht gesichert)")
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
                        // Zweite Sicherung: waehrend des Downloads kann der
                        // Benutzer weitergearbeitet haben. Hat sich der lokale
                        // Stand seit der Pruefung oben geaendert, wird NICHT
                        // ueberschrieben (Windows-Pendant: localAfterDownload).
                        let localAfterDownload = PromptBoardPanel.currentBackupFingerprint()
                        guard localAfterDownload == localBeforeDownload else {
                            tvoDebug("[App] launch-restore skipped (PromptBoard hat sich waehrend des Downloads geaendert)")
                            return
                        }
                        do {
                            let result = try PromptBoardPanel.applyBackupJson(json)
                            // Der eingespielte Stand ist ab jetzt der
                            // synchronisierte — Fingerabdruck mitziehen, sonst
                            // gilt er beim naechsten Start als "lokal veraendert".
                            // Bewusst aus dem ERGEBNIS-Zustand berechnet, nicht
                            // aus dem heruntergeladenen JSON: ein von Windows
                            // geschriebenes Backup hat eine leicht andere
                            // Feld-/Reihenfolge-Form, sein Abdruck wuerde nie
                            // zum lokal gebauten passen und der Restore waere
                            // dauerhaft blockiert.
                            PromptBoardPanel.writeLastSyncFingerprintFromCurrentState()
                            tvoDebug("[App] launch-restore applied remote backup from \(remoteDate): \(result.newPrompts) neue Prompts, \(result.newCategories) neue Kategorien")
                            // PromptBoard ueber den Auto-Sync informieren —
                            // setzt Timestamp + "+N neu"-Badge im Header.
                            // Wenn das Panel noch nicht existiert (lazy nach
                            // Stern-Klick), bleiben die Werte in UserDefaults
                            // und werden beim ersten Anzeigen sichtbar.
                            if let panel = self?.promptBoardPanel {
                                panel.recordLaunchAutoSync(date: remoteDate, newItems: result.total)
                                panel.refresh()
                            } else {
                                UserDefaults.standard.set(remoteDate, forKey: "pbLastBackupDate")
                                UserDefaults.standard.set(result.total, forKey: PromptBoardPanel.lastSyncNewItemsKey)
                            }
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
            // Auto-Hide-Timer neu starten nach Recording-Ende — sonst bleibt
            // das Panel sichtbar bis zur naechsten Mausbewegung ueber dem
            // Overlay.
            self.autoHide?.resume()
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
            // PTT: Press-Down-Zeit merken — bei KeyUp (NSEvent-Monitor)
            // wird elapsed verglichen. Tap startet Aufnahme normal,
            // Hold ≥500ms stoppt sie beim Loslassen.
            self?.ptt?.notePressDown()
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

    /// Oeffnet den existierenden PromptBoard-Settings-Dialog (1:1 Windows).
    /// Cmd+Shift+, ist mein Dev-Hotkey — derselbe Dialog kann auch ueber
    /// den Settings-Knopf in der PromptBoardPanel-Toolbar geoeffnet werden.
    private func openSettingsDialog() {
        guard let settings = try? PromptBoardStore.shared.settings() else { return }
        // Anchor-Window fuer .modalPanel: das aktuelle Schluessel-Fenster
        // oder das overlay panel.
        let anchor = NSApp.keyWindow ?? self.panel
        if let result = PBSettingsDialog.ask(parent: anchor, settings: settings) {
            var latest = (try? PromptBoardStore.shared.settings()) ?? settings
            latest.groqApiKey = result.groqApiKey
            latest.geminiApiKey = result.geminiApiKey
            latest.separatorTemplate = result.separatorTemplate
            latest.googleClientId = result.googleClientId
            latest.googleClientSecret = result.googleClientSecret
            latest.autoHide = result.autoHide
            latest.orientation = result.orientation
            latest.persistOverlayPosition = result.persistOverlayPosition
            try? PromptBoardStore.shared.updateSettings(latest)
            // Sofort wirksam: AutoHide-Controller umschalten + ggf. Orientation switchen.
            autoHide?.enabled = result.autoHide
            if let panel = self.panel,
               result.orientation == "horizontal" && panel.currentOrientation == .vertical {
                panel.beamToOrientation(.horizontal)
            } else if let panel = self.panel,
                      result.orientation == "vertical" && panel.currentOrientation == .horizontal {
                panel.beamToOrientation(.vertical)
            }
            // Persoenliches Vokabular wurde evtl. geaendert (in die SK-Datei
            // geschrieben) — zu Drive hochladen + Sync-Zeitstempel aktualisieren.
            uploadVocabularyToCloud()
        }
    }

    /// SaveButton-Click: toggelt die gespeicherte Position fuer die aktuelle
    /// Orientation.
    /// - Diskette weiss (keine Position gespeichert): aktuelle Position
    ///   speichern, Diskette wird gruen.
    /// - Diskette gruen (Position gespeichert): Position loeschen, Overlay
    ///   glide-t zurueck zur Standard-Position (canonicalHorizontalOrigin
    ///   bzw. canonicalVerticalOrigin), Diskette wird wieder weiss.
    /// Vertikal und horizontal werden getrennt verwaltet.
    private func savePositionForCurrentOrientation() {
        guard let panel = self.panel else { return }

        let alreadySaved: Bool
        switch panel.currentOrientation {
        case .vertical:   alreadySaved = (panel.savedVerticalPosition   != nil)
        case .horizontal: alreadySaved = (panel.savedHorizontalPosition != nil)
        }

        if alreadySaved {
            clearSavedPositionAndReturnToCanonical()
            return
        }

        let origin = panel.frame.origin
        switch panel.currentOrientation {
        case .vertical:
            panel.savedVerticalPosition = origin
            tvoDebug("[App] saved vertical position: \(origin)")
        case .horizontal:
            panel.savedHorizontalPosition = origin
            tvoDebug("[App] saved horizontal position: \(origin)")
        }
        // Wenn PersistOverlayPosition aktiv: zusaetzlich in DB-Settings.
        if var settings = try? PromptBoardStore.shared.settings(),
           settings.persistOverlayPosition {
            switch panel.currentOrientation {
            case .vertical:
                settings.overlayVerticalLeft = Double(origin.x)
                settings.overlayVerticalTop  = Double(origin.y)
            case .horizontal:
                settings.overlayHorizontalLeft = Double(origin.x)
                settings.overlayHorizontalTop  = Double(origin.y)
            }
            try? PromptBoardStore.shared.updateSettings(settings)
        }
    }

    /// Loescht die gespeicherte Position fuer die aktive Orientation und
    /// glide-t das Overlay zurueck zur kanonischen Standard-Position.
    /// Wird vom Disketten-Toggle-Off und kann auch von anderen Stellen
    /// genutzt werden (Reset-Hotkeys o.ae.).
    private func clearSavedPositionAndReturnToCanonical() {
        guard let panel = self.panel else { return }
        let canonical: NSPoint
        switch panel.currentOrientation {
        case .vertical:
            panel.savedVerticalPosition = nil
            let panelHeight: CGFloat = 612
            canonical = panel.canonicalVerticalOrigin(panelHeight: panelHeight)
            tvoDebug("[App] cleared vertical position, gliding to canonical: \(canonical)")
        case .horizontal:
            panel.savedHorizontalPosition = nil
            canonical = panel.canonicalHorizontalOrigin(panelWidth: panel.frame.width)
            tvoDebug("[App] cleared horizontal position, gliding to canonical: \(canonical)")
        }
        panel.glideWindow(to: canonical, completion: nil)

        // Wenn PersistOverlayPosition aktiv: gespeicherte Werte auch
        // aus DB-Settings entfernen (auf 0/0 zuruecksetzen, damit ein
        // App-Neustart wieder zur kanonischen Position startet).
        if var settings = try? PromptBoardStore.shared.settings(),
           settings.persistOverlayPosition {
            switch panel.currentOrientation {
            case .vertical:
                settings.overlayVerticalLeft = 0
                settings.overlayVerticalTop  = 0
            case .horizontal:
                settings.overlayHorizontalLeft = 0
                settings.overlayHorizontalTop  = 0
            }
            try? PromptBoardStore.shared.updateSettings(settings)
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
            // Orientation in AppSettings persistieren (Windows-Pendant:
            // PersistOrientation in OverlayWindow.xaml.cs).
            if var settings = try? PromptBoardStore.shared.settings() {
                settings.orientation = next.rawValue
                try? PromptBoardStore.shared.updateSettings(settings)
            }
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
    /// Cmd+Opt+A..Z: laedt den Prompt mit HotkeyLetter=letter aus der DB
    /// und fuegt seinen effectiveText ins aktive Terminal ein.
    /// Pendant zu Windows Win+Alt+A..Z.
    private func pastePromptByLetter(_ letter: Character) {
        do {
            guard let prompt = try PromptBoardStore.shared.promptByLetter(letter) else {
                NSLog("[PromptHotkey] Cmd+Opt+%@ — no prompt assigned", String(letter))
                return
            }
            let text = prompt.effectiveText
            NSLog("[PromptHotkey] Cmd+Opt+%@ -> '%@' (len=%d)",
                  String(letter), prompt.shortLabel, text.count)
            TerminalController.pasteText(text, autoEnter: autoEnterEnabled)
        } catch {
            NSLog("[PromptHotkey] DB error: %@", error.localizedDescription)
        }
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

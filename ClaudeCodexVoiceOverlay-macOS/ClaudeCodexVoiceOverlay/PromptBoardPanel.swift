import AppKit
import CryptoKit

/// Anzahl Items, die ein applyBackupJson()-Aufruf NEU lokal hinzugefuegt
/// hat (id war lokal vorher nicht vorhanden). Updates an bestehenden
/// Items zaehlen NICHT mit — nur "neu" ist relevant fuer "+N neu" im
/// PromptBoard-Header.
struct BackupApplyResult {
    let newPrompts: Int
    let newCategories: Int
    var total: Int { newPrompts + newCategories }
}

/// Side panel that mirrors the Windows VTO PromptBoardPanel.xaml:
/// - horizontal category tabs at the top
/// - vertical scrollable prompt list
/// - "+" for category, backup/settings icons in the header
/// - per-row edit and delete icons
/// - "+ Neuer Prompt" at the bottom
/// Click on a prompt -> dictation-less insert via callback.
final class PromptBoardPanel: NSPanel, NSGestureRecognizerDelegate {

    var onInsertText: ((String) -> Void)?

    /// Wird ausgeloest wenn der Benutzer im neuen PromptInputPanel Enter
    /// drueckt. Der Text ist der reine Inhalt der Eingabe-Box — das
    /// Pre/Mitte/Post-Zusammenbauen passiert weiter oben (AppDelegate
    /// kennt schon den AlwaysOnPrefixService und den TerminalController).
    var onInputSubmit: ((String) -> Void)?

    /// Das angedockte tippbare Eingabefenster. Wird beim Stern-Klick
    /// erzeugt und beim erneuten Klick (oder beim Schliessen des
    /// Promptboards) zerstoert. Inhalt ist absichtlich nicht persistent.
    private var inputPanel: PromptInputPanel?

    /// Stern-Zustand. Beim App-Neustart immer false (laut Spec).
    private var inputPanelVisible = false

    private var inputToggleButton: NSButton?
    private var historyToggleButton: NSButton?
    private var filterActiveButton: NSButton?
    private var clearAllChecksButton: NSButton?

    /// Wenn true, werden in renderPrompts() nur Prompts mit isAlwaysOn=true
    /// angezeigt (Windows-Pendant: _filterActiveOnly).
    private var filterActiveOnly: Bool = false

    /// Historie-Fenster + Sichtbarkeitsstatus.
    private var historyPanel: PromptHistoryPanel?
    private var historyPanelVisible = false

    private let categoryStack = NSStackView()
    private let promptStack = PBFlippedStackView()
    private let addPromptButton = NSButton(title: "+ Neuer Prompt", target: nil, action: nil)
    private let titleLabel = NSTextField(labelWithString: "PromptBoard")
    /// Small muted label next to the title that shows when the Google Drive
    /// backup last completed. Updated after every successful auto-backup
    /// and persisted in UserDefaults so it survives app restarts.
    private let syncLabel = NSTextField(labelWithString: "")
    private static let lastSyncKey = "pbLastBackupDate"
    /// Anzahl neuer Items aus dem letzten Auto-Sync (Launch-Restore).
    /// Wird beim naechsten manuellen Sync auf 0 zurueckgesetzt, weil dann
    /// nichts mehr "neu vom letzten Mal" ist.
    static let lastSyncNewItemsKey = "pbLastBackupNewItemCount"

    private var categories: [PBCategory] = []
    /// Multiple categories can be active at the same time — prompts from every
    /// active category show up in the list, each tinted with that category's
    /// color. Clicking an active tab toggles it off.
    private var activeCategoryIds: Set<UUID> = []
    private var currentPrompts: [PBPrompt] = []

    /// Maps each rendered row's NSView → prompt id, so the right-mouse
    /// event monitor can quickly resolve "which prompt was right-clicked"
    /// by testing window coordinates without relying on gesture recognizers
    /// (which don't fire reliably on .nonactivatingPanel floating windows).
    private var rowViewsByPromptId: [UUID: NSView] = [:]

    /// Local event monitor for right-mouse-down events. Needed as a fallback
    /// because NSClickGestureRecognizer with buttonMask=.secondary sometimes
    /// refuses to fire inside a borderless, canBecomeKey=false NSPanel.
    private var rightClickMonitor: Any?
    /// Global event monitor for right-mouse drag/up — needed so the drag
    /// keeps tracking even when the cursor leaves our panel's frame.
    private var globalRightDragMonitor: Any?

    // ── Right-click drag state (drag the panel to move pillar+panel) ──
    private var isDraggingPanel = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var dragStartPanelOrigin: NSPoint = .zero
    /// Fires on every drag step + drag-end. The owner (AppDelegate)
    /// uses this to slide the Voice Overlay pillar by the same delta
    /// so both windows move together as a single unit.
    var onPanelDragged: ((NSPoint) -> Void)?

    /// Fires when the user clicks the new star button in the toolbar
    /// (next to the "!" reset button). Symmetrisches Pendant zum Stern in
    /// der PromptInput-Toolbar: blendet das Promtboard aus und dockt das
    /// Eingabefeld direkt an den Voice-Overlay-Pillar an. AppDelegate
    /// haengt sich hier ein und ruft applySoloDockMode(true) auf — die
    /// Logik ist 1:1 dieselbe wie beim Solo-Stern im Eingabefeld, deshalb
    /// reicht ein parameterloser Callback.
    var onBoardStarToggle: (() -> Void)?

    /// Debounce timer for auto-backup to Google Drive. Every mutation
    /// (add / edit / delete / toggle) reschedules the timer; the actual
    /// upload only fires once the user has stopped clicking for the delay
    /// window below. This prevents a rapid sequence of edits from
    /// generating ten separate Drive round-trips.
    private var autoBackupTimer: Timer?
    private static let autoBackupDelay: TimeInterval = 2.0

    /// Fixed palette used to assign a distinct color per category by its
    /// position in the categories array. Kept deterministic so the colors
    /// stay stable across renders.
    private static let categoryPalette: [NSColor] = [
        NSColor(calibratedRed: 0.29, green: 0.56, blue: 0.99, alpha: 1), // blue
        NSColor(calibratedRed: 0.95, green: 0.44, blue: 0.26, alpha: 1), // orange
        NSColor(calibratedRed: 0.40, green: 0.73, blue: 0.42, alpha: 1), // green
        NSColor(calibratedRed: 0.67, green: 0.28, blue: 0.74, alpha: 1), // purple
        NSColor(calibratedRed: 0.95, green: 0.65, blue: 0.15, alpha: 1), // amber
        NSColor(calibratedRed: 0.15, green: 0.72, blue: 0.82, alpha: 1), // cyan
        NSColor(calibratedRed: 0.93, green: 0.30, blue: 0.52, alpha: 1), // pink
        NSColor(calibratedRed: 0.47, green: 0.56, blue: 0.61, alpha: 1), // blue-grey
    ]

    private func color(for categoryId: UUID) -> NSColor {
        if let idx = categories.firstIndex(where: { $0.id == categoryId }) {
            return Self.categoryPalette[idx % Self.categoryPalette.count]
        }
        return Self.categoryPalette[0]
    }

    /// A dim but clearly tinted row background: keeps the dark-panel aesthetic
    /// so the white prompt text stays legible, but you can instantly tell
    /// which category each prompt belongs to by the tinted bar.
    private func rowBackground(for categoryId: UUID) -> NSColor {
        let base = color(for: categoryId)
        guard let rgb = base.usingColorSpace(.deviceRGB) else { return base }
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        rgb.getHue(&h, saturation: &s, brightness: &b, alpha: &a)
        return NSColor(deviceHue: h, saturation: min(s, 0.55), brightness: 0.30, alpha: 1)
    }

    init() {
        // Initial height matches the Voice Overlay pillar (480 pt).
        // dock(rightOf:) re-syncs it on demand so the two windows always
        // share the same vertical extent — change the pillar height in
        // OverlayPanel.swift and the panel automatically follows.
        // Breite: 532 px (1:1 mit Windows-PromptBoardPanel.xaml — Commit #1868
        // hat das Panel von 380 auf 532 verbreitert fuer mehr Platz pro Reihe).
        let contentRect = NSRect(x: 0, y: 0, width: 532, height: 480)
        super.init(
            contentRect: contentRect,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered, defer: false)
        isFloatingPanel = true
        level = .floating
        hidesOnDeactivate = false
        isMovableByWindowBackground = false
        backgroundColor = .clear
        isOpaque = false
        hasShadow = true
        titleVisibility = .hidden
        titlebarAppearsTransparent = true
        becomesKeyOnlyIfNeeded = true

        buildUI()
        installRightClickMonitor()
        installFrameObservers()
    }

    deinit {
        if let m = rightClickMonitor { NSEvent.removeMonitor(m) }
        if let m = globalRightDragMonitor { NSEvent.removeMonitor(m) }
        autoBackupTimer?.invalidate()
        NotificationCenter.default.removeObserver(self)
    }

    /// Andock-Garantie: Egal WER das Promtboard bewegt oder seine Groesse
    /// aendert (eigenes Drag, AppDelegate.dock(rightOf:) beim Pillar-Drag,
    /// Star-Toggle, Tab-Wechsel) — die angedockten Kinder (Eingabe +
    /// Historie) muessen IMMER 1:1 mitziehen. Die expliziten
    /// followBoardDrag-Aufrufe in mouseDragged etc. bleiben bestehen;
    /// diese Hooks sind die Sicherheitsschicht fuer alle anderen Wege wie
    /// Position/Groesse veraendert werden.
    private func installFrameObservers() {
        let nc = NotificationCenter.default
        nc.addObserver(self, selector: #selector(onFrameChanged),
                       name: NSWindow.didMoveNotification, object: self)
        nc.addObserver(self, selector: #selector(onFrameChanged),
                       name: NSWindow.didResizeNotification, object: self)
    }

    @objc private func onFrameChanged() {
        inputPanel?.followBoardDrag(self)
        historyPanel?.followBoardDrag(self)
    }

    // MARK: - Auto-backup

    /// Schedules a Google Drive backup after a short debounce window. Safe
    /// to call from any mutation path — many rapid calls collapse into
    /// one upload. Does nothing if Drive isn't connected yet.
    private func scheduleAutoBackup() {
        autoBackupTimer?.invalidate()
        autoBackupTimer = Timer.scheduledTimer(
            withTimeInterval: Self.autoBackupDelay,
            repeats: false) { [weak self] _ in
            self?.runAutoBackupIfConnected()
        }
    }

    /// Runs the actual upload silently. Success / failure is only written
    /// to the debug log — never a dialog, because auto-backup must not
    /// interrupt the user's flow. Manual "upload now" (in the ⇪ menu)
    /// still shows a confirmation dialog as before.
    private func runAutoBackupIfConnected() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[PBPanel] auto-backup skipped (Drive not connected)")
            return
        }
        do {
            let json = try buildBackupJson()
            GoogleDriveBackupService.shared.upload(json: json) { [weak self] result in
                DispatchQueue.main.async {
                    switch result {
                    case .success:
                        tvoDebug("[PBPanel] auto-backup uploaded")
                        // Der gerade hochgeladene Stand ist ab jetzt der
                        // synchronisierte — Fingerabdruck mitziehen, sonst
                        // haelt der Start-Restore ihn faelschlich fuer
                        // "lokal veraendert" und wird nie ausgefuehrt.
                        Self.writeLastSyncFingerprint(backupJson: json)
                        self?.recordSuccessfulSync()
                    case .failure(let e):
                        tvoDebug("[PBPanel] auto-backup failed: \(e.localizedDescription)")
                    }
                }
            }
        } catch {
            tvoDebug("[PBPanel] auto-backup build failed: \(error.localizedDescription)")
        }
    }

    /// Persists "now" as the last successful sync time and repaints the
    /// header badge next to the title. Bei manuellem Sync (Upload/manuelles
    /// Restore) wird der "+N neu"-Counter auf 0 zurueckgesetzt, weil ab
    /// jetzt nichts mehr "neu vom letzten Auto-Sync" ist.
    private func recordSuccessfulSync() {
        Self.recordSyncNow()
        refreshSyncLabel()
    }

    /// Persistiert "jetzt" als letzten Sync-Zeitpunkt — STATIC, damit der
    /// AppDelegate den Zeitstempel garantiert sichern kann, auch wenn das
    /// Board-Panel gerade nicht existiert oder nicht geoeffnet ist. Frank-Wunsch
    /// 2026-06-05: nach jedem Slot-Speichern/-Loeschen (= Sync) muss der
    /// Promtboard-Sync-Zeitstempel stimmen, egal ob das Board offen ist.
    static func recordSyncNow() {
        UserDefaults.standard.set(Date(), forKey: lastSyncKey)
        UserDefaults.standard.set(0, forKey: lastSyncNewItemsKey)
    }

    /// Public Hook fuer den AppDelegate: nach einem erfolgreichen Auto-Sync
    /// beim App-Launch wird hier der Timestamp + die Anzahl neuer Items
    /// gespeichert und das Header-Badge live aktualisiert. Wenn das Panel
    /// noch nicht sichtbar ist (lazy nach Stern-Klick), bleiben die Werte
    /// in UserDefaults und werden beim ersten Refresh angezeigt.
    func recordLaunchAutoSync(date: Date, newItems: Int) {
        UserDefaults.standard.set(date, forKey: Self.lastSyncKey)
        UserDefaults.standard.set(newItems, forKey: Self.lastSyncNewItemsKey)
        refreshSyncLabel()
    }

    /// Public wrapper fuer den Sync-Timestamp — wird vom AppDelegate nach
    /// einem erfolgreichen Historie-Upload aufgerufen, damit der "· sync"-
    /// Badge auch fuer Historie-Aktivitaet aktualisiert wird (nicht nur
    /// bei Promtboard-Backups).
    func markSyncedNow() {
        DispatchQueue.main.async { [weak self] in
            self?.recordSuccessfulSync()
        }
    }

    /// Reads the persisted last-sync timestamp and renders it as a short
    /// muted badge like "· sync 24.04. 22:39". Wenn beim letzten Auto-Sync
    /// neue Items geladen wurden, wird zusaetzlich "(+N neu)" angehaengt.
    /// Always shows date + time so you can tell at a glance how fresh the
    /// last sync is, even right after restarting the app. Empty when no
    /// sync has happened yet.
    private func refreshSyncLabel() {
        guard let last = UserDefaults.standard.object(forKey: Self.lastSyncKey) as? Date else {
            syncLabel.stringValue = ""
            return
        }
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "de_DE")
        fmt.dateFormat = "dd.MM. HH:mm"
        let newItems = UserDefaults.standard.integer(forKey: Self.lastSyncNewItemsKey)
        if newItems > 0 {
            let suffix = newItems == 1 ? "1 neu" : "\(newItems) neu"
            syncLabel.stringValue = "· sync \(fmt.string(from: last)) (+\(suffix))"
        } else {
            syncLabel.stringValue = "· sync \(fmt.string(from: last))"
        }
    }

    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }

    /// Installs a local event monitor for right-mouse-down. When the click
    /// lands inside one of our rendered prompt rows, open the editor —
    /// same effect as clicking the ✎ pencil. Using a direct event monitor
    /// instead of NSClickGestureRecognizer because the gesture variant is
    /// flaky on floating nonactivating panels.
    private func installRightClickMonitor() {
        rightClickMonitor = NSEvent.addLocalMonitorForEvents(
            matching: [.rightMouseDown, .rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            guard let self = self else { return event }
            // Only handle events meant for this panel.
            guard event.window === self else { return event }

            // Drag tracking takes priority once a drag is armed — pass
            // through to the dedicated handler.
            if event.type == .rightMouseDragged || event.type == .rightMouseUp {
                self.handlePanelDragEvent(event)
                return self.isDraggingPanel || event.type == .rightMouseUp ? nil : event
            }

            // .rightMouseDown — first check if it's on a prompt row.
            let windowPoint = event.locationInWindow
            for (id, row) in self.rowViewsByPromptId {
                guard row.window === self else { continue }
                let rowPoint = row.convert(windowPoint, from: nil)
                if row.bounds.contains(rowPoint) {
                    // Skip the interactive subviews (checkbox / ✎ / ✕).
                    if let hit = row.hitTest(row.superview?.convert(windowPoint, from: nil) ?? .zero),
                       hit is NSButton {
                        return event
                    }
                    tvoDebug("[PBPanel] rightClickMonitor → open editor id=\(id.uuidString)")
                    DispatchQueue.main.async { [weak self] in
                        self?.openEditorForPrompt(id: id)
                    }
                    return nil  // consume the event
                }
            }

            // Not on a row → arm a panel drag. The user can grab the
            // panel anywhere outside a prompt row (header strip, empty
            // list area, between tabs) to move the whole overlay.
            self.isDraggingPanel = true
            self.dragStartMouseLocation = NSEvent.mouseLocation
            self.dragStartPanelOrigin = self.frame.origin
            tvoDebug("[PBPanel] panel-drag armed at origin=\(self.dragStartPanelOrigin)")
            return nil  // consume so no contextual menu pops
        }

        // Global monitor: keeps the drag tracking even when the cursor
        // leaves the panel's frame mid-drag. Without this the panel
        // would freeze the moment the cursor crossed its own boundary.
        globalRightDragMonitor = NSEvent.addGlobalMonitorForEvents(
            matching: [.rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            self?.handlePanelDragEvent(event)
        }
    }

    private func handlePanelDragEvent(_ event: NSEvent) {
        guard isDraggingPanel else { return }
        let mouseLocation = NSEvent.mouseLocation
        switch event.type {
        case .rightMouseDragged:
            let dx = mouseLocation.x - dragStartMouseLocation.x
            let dy = mouseLocation.y - dragStartMouseLocation.y
            let newOrigin = NSPoint(x: dragStartPanelOrigin.x + dx,
                                    y: dragStartPanelOrigin.y + dy)
            setFrameOrigin(newOrigin)
            // Tell the AppDelegate to drag the pillar by the same delta
            // so the two floating windows move as one.
            onPanelDragged?(newOrigin)
            // Andockpartner mitnehmen — wenn das Eingabefenster oder die
            // Historie offen sind, sollen sie mit dem Promptboard
            // verschoben werden.
            inputPanel?.followBoardDrag(self)
            historyPanel?.followBoardDrag(self)
        case .rightMouseUp:
            isDraggingPanel = false
            onPanelDragged?(frame.origin)  // final position
            inputPanel?.followBoardDrag(self)
            historyPanel?.followBoardDrag(self)
            tvoDebug("[PBPanel] panel-drag end origin=\(self.frame.origin)")
        default:
            break
        }
    }

    override func close() {
        // Eingabefenster und Historie mitnehmen, sonst bleiben verwaiste
        // Panels ohne Trigger zurueck.
        closeInputPanel()
        closeHistoryPanel()
        super.close()
    }

    private func buildUI() {
        let root = NSView(frame: contentView!.bounds)
        root.autoresizingMask = [.width, .height]
        root.wantsLayer = true
        // Slightly translucent so the underlying terminal/app text shows
        // through the panel — alpha 0.78 keeps the dark theme legible
        // for the prompt rows while letting the user see what's behind.
        // Match the Windows counterpart's #C71E1E1E.
        root.layer?.backgroundColor = NSColor(calibratedWhite: 0.11, alpha: 0.78).cgColor
        root.layer?.cornerRadius = 16
        root.layer?.borderColor = NSColor(calibratedWhite: 0.28, alpha: 1).cgColor
        root.layer?.borderWidth = 1
        contentView?.addSubview(root)

        titleLabel.textColor = NSColor(calibratedWhite: 0.8, alpha: 1)
        titleLabel.font = NSFont.boldSystemFont(ofSize: 13)

        syncLabel.textColor = NSColor(calibratedWhite: 0.50, alpha: 1)
        syncLabel.font = NSFont.systemFont(ofSize: 10)
        refreshSyncLabel()   // show the last known date from UserDefaults

        // Filter-Button "nur aktivierte Prompts" — sitzt direkt rechts neben
        // dem SyncLabel. Klick blendet alle Prompts ohne Always-On-Haekchen
        // aus, erneuter Klick zeigt wieder alle. Im aktiven Filter-Zustand
        // wird der Button golden — passend zum gelb-goldenen Haekchen der
        // Always-On-Checkboxen (Windows-Pendant: BtnFilterActiveOnly #1880).
        let filterBtn = makeIconButton(symbol: "✓", tooltip: "Nur aktivierte Prompts anzeigen — blendet alle Prompts ohne Haekchen aus. Erneuter Klick zeigt wieder alle.", action: #selector(onToggleFilterActive))
        self.filterActiveButton = filterBtn

        // !-Button "Alle Haekchen entfernen" — entfernt Always-On in allen
        // Kategorien (ausser "Allgemein", siehe Windows #1868). Klick zeigt
        // gruenes Aufblitzen als visuelle Bestaetigung (Windows #1867).
        let clearChecksBtn = makeIconButton(symbol: "!", tooltip: "Alle Haekchen entfernen (Always-On in allen Kategorien)", action: #selector(onClearAllChecks))
        self.clearAllChecksButton = clearChecksBtn

        // Stern-Button — Promtboard ausblenden und Eingabefeld direkt am
        // Pillar andocken (symmetrisches Pendant zum Stern in der PromptInput-
        // Toolbar). Auf Wunsch des Benutzers (Frank, 2026-05-09) IMMER
        // goldig (#FFD700) — unabhaengig vom Solo-Modus. Position direkt
        // neben dem Filter-Button im SyncBlock, NICHT mehr in der rechten
        // Action-Gruppe.
        let boardStarBtn = makeIconButton(symbol: "★", tooltip: "Promptboard schliessen und Eingabefeld direkt am Voice-Overlay andocken (Stern im Eingabefeld holt das Promtboard wieder zurueck).", action: #selector(onBoardStarTapped))
        boardStarBtn.contentTintColor = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1.0)
        if let cell = boardStarBtn.cell as? NSButtonCell {
            let goldAttrs: [NSAttributedString.Key: Any] = [
                .foregroundColor: NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1.0),
                .font: cell.font ?? NSFont.boldSystemFont(ofSize: 14)
            ]
            boardStarBtn.attributedTitle = NSAttributedString(string: "★", attributes: goldAttrs)
        }

        let historyBtn = makeIconButton(symbol: "📜", tooltip: "Prompt-Historie", action: #selector(onToggleHistory))
        let addCatBtn = makeIconButton(symbol: "+", tooltip: "Neue Kategorie", action: #selector(onAddCategory))
        let backupBtn = makeIconButton(symbol: "⇪", tooltip: "Backup / Wiederherstellen", action: #selector(onBackup))
        let settingsBtn = makeIconButton(symbol: "⚙︎", tooltip: "Einstellungen", action: #selector(onSettings), fontSize: 18)
        self.historyToggleButton = historyBtn

        // Reihenfolge (Frank-Wunsch 2026-05-09: Stern direkt neben dem
        // Filter-Button im SyncBlock, immer goldig). Layout:
        // [Title] [Sync + Filter + Stern] <flex spacer> [!] [Historie] [+] [Backup] [Settings]
        let syncBlock = NSStackView(views: [syncLabel, filterBtn, boardStarBtn])
        syncBlock.orientation = .horizontal
        syncBlock.spacing = 6
        syncBlock.alignment = .centerY

        let header = NSStackView(views: [titleLabel, syncBlock, NSView(), clearChecksBtn, historyBtn, addCatBtn, backupBtn, settingsBtn])
        header.orientation = .horizontal
        header.alignment = .centerY
        header.spacing = 6
        header.distribution = .fill
        // Index 2 is the stretchable spacer NSView() between the sync block
        // and the header's right-side icon buttons.
        (header.arrangedSubviews[2]).setContentHuggingPriority(.defaultLow, for: .horizontal)

        // Vertical container that holds one horizontal row-stack per line of tabs.
        // Tabs wrap to the next row automatically when they don't fit horizontally.
        categoryStack.orientation = .vertical
        categoryStack.alignment = .leading
        categoryStack.spacing = 4
        categoryStack.translatesAutoresizingMaskIntoConstraints = false

        promptStack.orientation = .vertical
        promptStack.alignment = .leading
        promptStack.spacing = 6

        let promptScroll = NSScrollView()
        promptScroll.hasVerticalScroller = true
        promptScroll.scrollerStyle = .overlay
        promptScroll.autohidesScrollers = true
        promptScroll.drawsBackground = false
        promptScroll.borderType = .noBorder
        promptScroll.documentView = promptStack
        promptScroll.translatesAutoresizingMaskIntoConstraints = false
        // Let the scroll view expand to fill the remaining vertical space in
        // the outer stack; if there are more prompts than fit, the overflow
        // scrolls instead of pushing the "+ Neuer Prompt" button off-screen.
        promptScroll.setContentHuggingPriority(.defaultLow, for: .vertical)
        promptScroll.setContentCompressionResistancePriority(.defaultLow, for: .vertical)
        promptStack.translatesAutoresizingMaskIntoConstraints = false

        addPromptButton.target = self
        addPromptButton.action = #selector(onAddPrompt)
        addPromptButton.isBordered = false
        addPromptButton.wantsLayer = true
        addPromptButton.layer?.backgroundColor = NSColor(calibratedWhite: 0.24, alpha: 1).cgColor
        addPromptButton.layer?.borderColor = NSColor(calibratedWhite: 0.45, alpha: 1).cgColor
        addPromptButton.layer?.borderWidth = 1
        addPromptButton.layer?.cornerRadius = 8
        addPromptButton.contentTintColor = .white
        addPromptButton.attributedTitle = NSAttributedString(
            string: "+ Neuer Prompt",
            attributes: [
                .foregroundColor: NSColor.white,
                .font: NSFont.systemFont(ofSize: 13, weight: .semibold)
            ]
        )
        addPromptButton.heightAnchor.constraint(equalToConstant: 30).isActive = true

        let stack = NSStackView(views: [header, categoryStack, promptScroll, addPromptButton])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        root.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: root.leadingAnchor, constant: 10),
            stack.trailingAnchor.constraint(equalTo: root.trailingAnchor, constant: -10),
            stack.topAnchor.constraint(equalTo: root.topAnchor, constant: 10),
            stack.bottomAnchor.constraint(equalTo: root.bottomAnchor, constant: -10),
            header.widthAnchor.constraint(equalTo: stack.widthAnchor),
            categoryStack.widthAnchor.constraint(equalTo: stack.widthAnchor),
            promptScroll.widthAnchor.constraint(equalTo: stack.widthAnchor),
            promptScroll.heightAnchor.constraint(greaterThanOrEqualToConstant: 300),
            addPromptButton.widthAnchor.constraint(equalTo: stack.widthAnchor),
            promptStack.widthAnchor.constraint(equalTo: promptScroll.widthAnchor),
        ])
    }

    private func makeIconButton(symbol: String, tooltip: String, action: Selector, fontSize: CGFloat = 14) -> NSButton {
        let btn = NSButton(title: "", target: self, action: action)
        btn.isBordered = false
        btn.toolTip = tooltip
        btn.setButtonType(.momentaryChange)
        btn.wantsLayer = true
        btn.layer?.backgroundColor = NSColor(calibratedWhite: 0.22, alpha: 1).cgColor
        btn.layer?.borderColor = NSColor(calibratedWhite: 0.42, alpha: 1).cgColor
        btn.layer?.borderWidth = 1
        btn.layer?.cornerRadius = 13
        btn.attributedTitle = NSAttributedString(
            string: symbol,
            attributes: [
                .foregroundColor: NSColor.white,
                .font: NSFont.systemFont(ofSize: fontSize, weight: .semibold)
            ]
        )
        btn.widthAnchor.constraint(equalToConstant: 28).isActive = true
        btn.heightAnchor.constraint(equalToConstant: 26).isActive = true
        return btn
    }

    // MARK: - Public

    func dock(rightOf vto: NSWindow) {
        let pillarFrame = vto.frame
        let isHorizontal = (vto as? OverlayPanel)?.currentOrientation == .horizontal
        if isHorizontal {
            // Horizontaler VTO-Modus: die Leiste ist breit und nur ~92px hoch.
            // Das Board dockt OBEN an (Unterkante an der VTO-Oberkante),
            // linksbuendig mit der Leiste (Frank-Wunsch 2026-05-28). Hoehe =
            // 3/4 der vertikalen Saeulenhoehe — die Leistenhoehe selbst waere
            // viel zu klein.
            // UNTERKANTE fix BUENDIG an der VTO-Oberkante (4px Anschluss). Die
            // Unterkante bleibt fix — passt die 3/4-Hoehe oben nicht in den
            // sichtbaren Bereich, wird die HOEHE gekuerzt statt das Fenster nach
            // unten zu verschieben (Letzteres machte das Board "zu tief", weil
            // promptScroll min 300 das Board hoeher zwingt als 3/4). Frank-
            // Wunsch 2026-05-28. Rechtsbuendig zur VTO-Aussenkante.
            let bottomY = pillarFrame.origin.y + pillarFrame.size.height + 4
            var boardHeight = (OverlayPanel.verticalPanelHeight * 0.75).rounded()
            if let vf = NSScreen.main?.visibleFrame, bottomY + boardHeight > vf.maxY {
                boardHeight = vf.maxY - bottomY
            }
            let newSize = NSSize(width: frame.size.width, height: boardHeight)
            var newX = pillarFrame.origin.x + pillarFrame.size.width - newSize.width
            if let vf = NSScreen.main?.visibleFrame {
                if newX + newSize.width > vf.maxX { newX = vf.maxX - newSize.width }
                if newX < vf.minX { newX = vf.minX }
            }
            setFrame(NSRect(origin: NSPoint(x: newX, y: bottomY), size: newSize), display: true)
        } else {
            // Vertikaler Modus: Board dockt LINKS an den Pillar.
            // Hoehe: 3/4 der Pillar-Hoehe (Frank-Wunsch 2026-05-28 — das Board
            // wirkte zu hoch). Oben buendig mit dem Pillar ausgerichtet, damit
            // der obere Rand der beiden Floating-Fenster auf einer Linie liegt.
            // Width bleibt unser eigener Wert — der Pillar ist viel schmaler.
            let boardHeight = (pillarFrame.size.height * 0.75).rounded()
            let newSize = NSSize(width: frame.size.width, height: boardHeight)
            let newOrigin = NSPoint(
                x: pillarFrame.origin.x - newSize.width - 4,
                y: pillarFrame.origin.y + pillarFrame.size.height - boardHeight)
            setFrame(NSRect(origin: newOrigin, size: newSize), display: true)
        }
    }

    func refresh() {
        do {
            categories = try PromptBoardStore.shared.allCategories()
        } catch {
            NSLog("refresh failed: \(error.localizedDescription)")
            categories = []
        }
        // Prune stale ids (category deleted elsewhere) but keep every id the
        // user still has active.
        let known = Set(categories.map { $0.id })
        activeCategoryIds = activeCategoryIds.intersection(known)
        // First-time / after-delete fallback: activate the first category so
        // the user isn't greeted with an empty list.
        if activeCategoryIds.isEmpty, let first = categories.first {
            activeCategoryIds.insert(first.id)
        }
        renderCategoryTabs()
        if categories.isEmpty {
            renderEmptyState("Noch keine Kategorien. Klick +")
            return
        }
        renderPrompts()
    }

    // MARK: - Rendering

    private func renderCategoryTabs() {
        categoryStack.arrangedSubviews.forEach { $0.removeFromSuperview() }

        // Build the tab buttons first so we can measure their fitting widths.
        let tabButtons: [NSButton] = categories.map { cat in
            let isActive = activeCategoryIds.contains(cat.id)
            let catColor = color(for: cat.id)

            let btn = PBCategoryTabButton(title: "", target: self, action: #selector(onSelectCategory(_:)))
            btn.categoryId = cat.id
            btn.owner = self
            btn.tag = categories.firstIndex(where: { $0.id == cat.id }) ?? 0
            btn.isBordered = false
            btn.wantsLayer = true
            btn.layer?.cornerRadius = 11
            btn.layer?.backgroundColor = isActive
                ? catColor.cgColor
                : NSColor(calibratedWhite: 0.22, alpha: 1).cgColor
            btn.layer?.borderWidth = 1
            btn.layer?.borderColor = isActive
                ? catColor.cgColor
                : NSColor(calibratedWhite: 0.35, alpha: 1).cgColor

            let textColor: NSColor = isActive
                ? .white
                : NSColor(calibratedWhite: 0.90, alpha: 1)
            btn.attributedTitle = NSAttributedString(string: "  \(cat.name)  ", attributes: [
                .foregroundColor: textColor,
                .font: isActive
                    ? NSFont.boldSystemFont(ofSize: 12)
                    : NSFont.systemFont(ofSize: 12, weight: .medium)
            ])

            btn.heightAnchor.constraint(equalToConstant: 22).isActive = true

            // Right-click for context menu
            let menu = NSMenu()
            let rename = NSMenuItem(title: "Umbenennen", action: #selector(onRenameCategory(_:)), keyEquivalent: "")
            rename.target = self
            rename.representedObject = cat.id.uuidString
            let del = NSMenuItem(title: "Loeschen", action: #selector(onDeleteCategory(_:)), keyEquivalent: "")
            del.target = self
            del.representedObject = cat.id.uuidString
            menu.addItem(rename)
            menu.addItem(del)
            btn.menu = menu
            return btn
        }

        // Flow-layout: pack buttons into horizontal rows, wrap when a row would overflow.
        // Panel is 380 wide with 10pt padding on each side → effective row width ≈ 360pt.
        let rowSpacing: CGFloat = 4
        let maxRowWidth: CGFloat = 360

        var currentRow = makeRowStack(spacing: rowSpacing)
        var currentWidth: CGFloat = 0
        categoryStack.addArrangedSubview(currentRow)

        for btn in tabButtons {
            let btnWidth = ceil(btn.fittingSize.width)
            let needsBreak = !currentRow.arrangedSubviews.isEmpty
                && currentWidth + rowSpacing + btnWidth > maxRowWidth
            if needsBreak {
                currentRow = makeRowStack(spacing: rowSpacing)
                categoryStack.addArrangedSubview(currentRow)
                currentWidth = 0
            }
            currentRow.addArrangedSubview(btn)
            currentWidth += (currentWidth == 0 ? btnWidth : btnWidth + rowSpacing)
        }
    }

    private func makeRowStack(spacing: CGFloat) -> NSStackView {
        let row = NSStackView()
        row.orientation = .horizontal
        row.alignment = .centerY
        row.spacing = spacing
        row.translatesAutoresizingMaskIntoConstraints = false
        return row
    }

    private func renderPrompts() {
        promptStack.arrangedSubviews.forEach { $0.removeFromSuperview() }

        if activeCategoryIds.isEmpty {
            currentPrompts = []
            rowViewsByPromptId.removeAll()
            renderEmptyState("Keine Kategorie aktiv. Klick oben auf einen Tab.")
            return
        }

        // Collect prompts from every active category, carrying the category id
        // along so we can tint each row accordingly.
        var combined: [(PBPrompt, UUID)] = []
        for catId in activeCategoryIds {
            do {
                let prompts = try PromptBoardStore.shared.prompts(in: catId)
                combined.append(contentsOf: prompts.map { ($0, catId) })
            } catch {
                NSLog("load prompts for \(catId): \(error.localizedDescription)")
            }
        }

        let sortedAll = combined.sorted(by: { lhs, rhs in
            if lhs.0.sortOrder != rhs.0.sortOrder { return lhs.0.sortOrder < rhs.0.sortOrder }
            return lhs.0.shortLabel.localizedCaseInsensitiveCompare(rhs.0.shortLabel) == .orderedAscending
        })
        // Filter "Nur aktivierte Prompts": wenn eingeschaltet, nur Eintraege
        // mit isAlwaysOn=true durchlassen. Sonst alle (Windows-Pendant: BtnFilterActiveOnly).
        let sorted = filterActiveOnly ? sortedAll.filter { $0.0.isAlwaysOn } : sortedAll
        currentPrompts = sorted.map { $0.0 }

        if sorted.isEmpty {
            rowViewsByPromptId.removeAll()
            renderEmptyState("Keine Prompts in den aktiven Kategorien.")
            return
        }

        // Reset row map before we rebuild the list so stale views can't
        // intercept right-click lookups after a re-render.
        rowViewsByPromptId.removeAll()
        for (prompt, catId) in sorted {
            let row = buildRow(for: prompt, categoryId: catId)
            rowViewsByPromptId[prompt.id] = row
            promptStack.addArrangedSubview(row)
            // Width constraint must be activated AFTER adding to superview hierarchy,
            // otherwise NSISEngine throws on restore-triggered re-render (views live).
            // Full width — matches the "+ Neuer Prompt" button exactly. The delete ✕
            // gets its breathing room from the rowStack's trailing padding.
            row.widthAnchor.constraint(equalTo: promptStack.widthAnchor).isActive = true
        }
    }

    private func buildRow(for prompt: PBPrompt, categoryId: UUID) -> NSView {
        let row = PBPromptRowView()
        row.promptId = prompt.id
        row.owner = self
        row.wantsLayer = true
        row.layer?.backgroundColor = rowBackground(for: categoryId).cgColor
        row.layer?.cornerRadius = 8
        row.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        // Whole-row click inserts the prompt. The gesture recognizer has a
        // delegate that blocks the recognition when the click lands on an
        // NSButton child (checkbox, edit ✎, delete ✕) — so those buttons run
        // only their own action and never double-trigger an insert.
        let rowClick = NSClickGestureRecognizer(target: self, action: #selector(onRowClick(_:)))
        rowClick.delegate = self
        row.addGestureRecognizer(rowClick)

        // Right-click anywhere on the row opens the prompt editor — same as
        // clicking the ✎ pencil, but with the whole bar as hit area. Uses the
        // same delegate guard so right-clicks on the three interactive
        // buttons still run their own action.
        let rowRightClick = NSClickGestureRecognizer(target: self, action: #selector(onRowRightClick(_:)))
        rowRightClick.buttonMask = 0x2   // secondary (right) mouse button
        rowRightClick.delegate = self
        row.addGestureRecognizer(rowRightClick)

        let alwaysOnToggle = NSButton(title: "", target: self, action: #selector(onToggleAlwaysOn(_:)))
        alwaysOnToggle.isBordered = false
        alwaysOnToggle.setButtonType(.momentaryChange)
        alwaysOnToggle.wantsLayer = true
        alwaysOnToggle.layer?.cornerRadius = 4
        alwaysOnToggle.layer?.borderWidth = 1.5
        alwaysOnToggle.layer?.backgroundColor = (prompt.isAlwaysOn
            ? NSColor(red: 1, green: 0xD7/255.0, blue: 0, alpha: 1)
            : NSColor(calibratedWhite: 0.18, alpha: 1)).cgColor
        alwaysOnToggle.layer?.borderColor = (prompt.isAlwaysOn
            ? NSColor(red: 1, green: 0xD7/255.0, blue: 0, alpha: 1)
            : NSColor(calibratedWhite: 0.55, alpha: 1)).cgColor
        alwaysOnToggle.attributedTitle = prompt.isAlwaysOn
            ? NSAttributedString(string: "✓", attributes: [
                .foregroundColor: NSColor(calibratedWhite: 0.12, alpha: 1),
                .font: NSFont.boldSystemFont(ofSize: 12)
            ])
            : NSAttributedString()
        alwaysOnToggle.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        alwaysOnToggle.toolTip = prompt.isAlwaysOn
            ? "Immer aktiv — wird bei jedem Prompt dauerhaft eingefuegt. Klicken zum Deaktivieren."
            : "Anhaken, damit dieser Prompt bei jedem Insert dauerhaft mitgeschickt wird."

        // Split the stored short label into "Title" + "(Timestamp)" so each
        // gets its own view with a different size and alignment. Timestamp
        // sits as a small dim label between the spacer and the icons,
        // matching the Windows-side 5-column layout.
        let (titleText, timestampText) = Self.splitLabel(prompt.shortLabel)

        // Title is rendered as a non-interactive NSTextField (not NSButton)
        // so mouseDown/mouseDragged events propagate to the parent
        // PBPromptRowView. With an NSButton the mouse event was eaten by
        // the button — so dragging from the title text never armed the
        // drag session, only dragging from the empty area between title
        // and timestamp worked. The row-level NSClickGestureRecognizer
        // already handles the "click → insert" path, so we don't lose
        // the insert behavior. Hit-test forwarding (see PBPromptRowView)
        // ensures the title still doesn't swallow drags.
        // Hotkey-Suffix " ⌘N" wird vor den Titel gesetzt wenn ein Hotkey
        // gebunden ist — kleines visuelles Badge das dem Windows-Pendant
        // entspricht (dort steht z.B. "1 — Title"). Macht beim Scannen der
        // Prompt-Liste sofort sichtbar welche Cmd+N-Taste was triggert.
        // Pre/Post-Suffix " - vor"/" - nach" wird an den Titel angehaengt damit
        // beim Scannen der Liste sofort sichtbar ist welche Prompts vor und
        // welche nach dem eigentlichen Eingabe-Text eingefuegt werden
        // (Windows-Pendant #1867).
        var titleWithFlags = titleText
        if prompt.isPrePrompt { titleWithFlags += " - vor" }
        if prompt.isPostPrompt { titleWithFlags += " - nach" }

        let titleWithHotkey: String
        if let hk = prompt.hotkeyNumber, hk >= 1, hk <= 9 {
            titleWithHotkey = "⌘\(hk)  \(titleWithFlags)"
        } else {
            titleWithHotkey = titleWithFlags
        }
        let insertLabel = NSTextField(labelWithString: titleWithHotkey)
        insertLabel.textColor = .white
        insertLabel.font = NSFont.systemFont(ofSize: 13)
        insertLabel.lineBreakMode = .byTruncatingTail
        insertLabel.maximumNumberOfLines = 1
        insertLabel.drawsBackground = false
        insertLabel.isBordered = false
        insertLabel.isEditable = false
        insertLabel.isSelectable = false
        insertLabel.toolTip = prompt.effectiveText.prefix(500).description
        insertLabel.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        insertLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        insertLabel.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)

        // Timestamp label — small, dim, vertically centered next to icons.
        // hidden when the label has no timestamp suffix yet (legacy data).
        let timestampLabel = NSTextField(labelWithString: timestampText)
        timestampLabel.font = NSFont.systemFont(ofSize: 10)
        timestampLabel.textColor = NSColor(calibratedWhite: 0.66, alpha: 1)
        timestampLabel.drawsBackground = false
        timestampLabel.isBordered = false
        timestampLabel.isHidden = timestampText.isEmpty

        let editBtn = NSButton(title: "✎", target: self, action: #selector(onEditPrompt(_:)))
        editBtn.bezelStyle = .recessed
        editBtn.isBordered = false
        editBtn.contentTintColor = NSColor(calibratedWhite: 0.75, alpha: 1)
        editBtn.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        editBtn.toolTip = "Bearbeiten"

        let deleteBtn = NSButton(title: "✕", target: self, action: #selector(onDeletePrompt(_:)))
        deleteBtn.bezelStyle = .recessed
        deleteBtn.isBordered = false
        deleteBtn.contentTintColor = NSColor(calibratedWhite: 0.75, alpha: 1)
        deleteBtn.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        deleteBtn.toolTip = "Loeschen"

        // Insert-Button (▶) — Windows-Pendant #1866 (E77F). Klick fuegt den
        // Prompt direkt ins Terminal ein. Vorher hatte die ganze Reihe diese
        // Aufgabe; seit #1866 toggelt der Row-Click Always-On und es gibt
        // einen dedizierten Insert-Button.
        let insertBtn = NSButton(title: "▶", target: self, action: #selector(onInsertPromptClick(_:)))
        insertBtn.bezelStyle = .recessed
        insertBtn.isBordered = false
        insertBtn.contentTintColor = NSColor(calibratedRed: 0.50, green: 0.81, blue: 1.0, alpha: 1)
        insertBtn.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        insertBtn.toolTip = "In Claude einfuegen"

        // Layout: [checkbox][insertLabel][spacer][timestamp][insert][edit][delete]
        let rowStack = NSStackView(views: [alwaysOnToggle, insertLabel, NSView(), timestampLabel, insertBtn, editBtn, deleteBtn])
        rowStack.orientation = .horizontal
        rowStack.alignment = .centerY
        rowStack.spacing = 6
        rowStack.translatesAutoresizingMaskIntoConstraints = false
        rowStack.setContentHuggingPriority(.defaultLow, for: .horizontal)
        row.addSubview(rowStack)

        NSLayoutConstraint.activate([
            rowStack.leadingAnchor.constraint(equalTo: row.leadingAnchor, constant: 12),
            rowStack.trailingAnchor.constraint(equalTo: row.trailingAnchor, constant: -12),
            rowStack.topAnchor.constraint(equalTo: row.topAnchor, constant: 6),
            rowStack.bottomAnchor.constraint(equalTo: row.bottomAnchor, constant: -6),
            row.heightAnchor.constraint(greaterThanOrEqualToConstant: 32),
            alwaysOnToggle.widthAnchor.constraint(equalToConstant: 18),
            alwaysOnToggle.heightAnchor.constraint(equalToConstant: 18),
            insertBtn.widthAnchor.constraint(equalToConstant: 22),
            editBtn.widthAnchor.constraint(equalToConstant: 22),
            deleteBtn.widthAnchor.constraint(equalToConstant: 22),
        ])
        return row
    }

    /// Splits the stored short label into title text + parenthesised
    /// timestamp suffix, e.g. "Refactor Login (25.04.2026, 16:02)" →
    /// ("Refactor Login", "(25.04.2026, 16:02)"). Returns the whole label
    /// as title with empty timestamp when there's no trailing parenthesis.
    private static func splitLabel(_ label: String) -> (title: String, timestamp: String) {
        // Find the last " (" that opens a parenthesised suffix and check
        // the label closes with ")".
        if label.hasSuffix(")"),
           let openRange = label.range(of: " (", options: .backwards) {
            let title = String(label[..<openRange.lowerBound])
            // Skip the leading space, keep the parens.
            let timestamp = String(label[label.index(after: openRange.lowerBound)...])
            return (title, timestamp)
        }
        return (label, "")
    }

    private func renderEmptyState(_ message: String) {
        promptStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        let label = NSTextField(labelWithString: message)
        label.textColor = NSColor(calibratedWhite: 0.6, alpha: 1)
        label.maximumNumberOfLines = 3
        label.lineBreakMode = .byWordWrapping
        promptStack.addArrangedSubview(label)
    }

    // MARK: - Actions

    @objc private func onSelectCategory(_ sender: NSButton) {
        tvoDebug("[PBPanel] onSelectCategory tag=\(sender.tag) panelLevel=\(self.level.rawValue)")
        guard sender.tag >= 0, sender.tag < categories.count else { return }
        let id = categories[sender.tag].id
        // Toggle: clicking an already-active tab turns it off, clicking an
        // inactive one adds it. Multiple can be active at once.
        if activeCategoryIds.contains(id) {
            activeCategoryIds.remove(id)
        } else {
            activeCategoryIds.insert(id)
        }
        renderCategoryTabs()
        renderPrompts()
    }

    // MARK: - Prompt-Eingabefenster (Stern-Toggle)

    @objc private func onToggleInput() {
        if inputPanelVisible {
            closeInputPanel()
        } else {
            openInputPanel()
        }
    }

    /// Liefert das aktuelle InputPanel falls offen — AppDelegate braucht das
    /// fuer Solo-Dock-Logik (am Pillar andocken statt am Board).
    var currentInputPanel: PromptInputPanel? { inputPanel }

    /// Public Wrapper damit AppDelegate das Eingabefenster oeffnen kann
    /// ohne dass der Benutzer auf den Stern im Header klicken muss
    /// (Stern wurde mit Windows-Parity #1877 aus dem Header entfernt).
    func openInputPanelExternally() { openInputPanel() }
    func closeInputPanelExternally() { closeInputPanel() }

    private func openInputPanel() {
        // Eingabe und Historie schliessen sich gegenseitig aus — beide
        // docken am gleichen Platz links neben dem Promtboard. Wenn die
        // Historie offen ist, machen wir sie zu BEVOR die Eingabe sich
        // einblendet, damit nie beide gleichzeitig dort liegen.
        if historyPanelVisible { closeHistoryPanel() }
        if inputPanel == nil {
            let panel = PromptInputPanel()
            panel.onSubmit = { [weak self] text in
                guard let self = self else { return }
                // KRITISCH: Floating Panels behalten Key-Status auch wenn
                // eine andere App active wird. Wenn wir hier nicht resignKey
                // aufrufen, geht das gleich folgende Cmd+V (aus pasteText)
                // ans Key-Window = unser PromptInputPanel statt ans Terminal —
                // ergibt einen System-Beep weil ein gerade geleertes TextView
                // mit Cmd+V nicht sinnvoll umgehen kann.
                self.inputPanel?.resignKey()
                // Pre/Mitte/Post-Bauen passiert weiter oben — hier nur den
                // reinen Inhalt nach aussen reichen.
                self.onInputSubmit?(text)
                // Eingabe leeren (clearInput zieht den Fokus NICHT mehr
                // automatisch zurueck ins Panel — das wuerde mit pasteText racen).
                self.inputPanel?.clearInput()
            }
            // Rechtsklick-Drag im Eingabefenster verschiebt die GANZE Gruppe.
            panel.onGroupDragDelta = { [weak self] dx, dy in
                self?.translateGroup(dx: dx, dy: dy)
            }
            // Prompt-Zwischenspeicher: Speichern/Loeschen direkt in den Store,
            // danach SOFORT Cloud-Sync anstossen (Frank-Wunsch). Gleiches
            // Muster wie die Historie (onHistorySyncRequested).
            panel.onSlotSave = { [weak self] number, text in
                PromptSlotStore.shared.save(number: number, text: text) {
                    self?.onSlotsSyncRequested?()
                }
            }
            panel.onSlotDelete = { [weak self] number in
                PromptSlotStore.shared.delete(number: number) {
                    self?.onSlotsSyncRequested?()
                }
            }
            // Frische KI-Zusammenfassung persistieren (nur wenn der Text noch
            // passt) und SOFORT Cloud-Sync — damit der Hover-Tooltip ins
            // Drive-Backup wandert und auf andere Geraete synct.
            panel.onSlotSummary = { [weak self] number, text, summary in
                PromptSlotStore.shared.setSummary(number: number, forText: text, summary: summary) {
                    self?.onSlotsSyncRequested?()
                }
            }
            // Rechtsklick-Prioritaet (Hoch/Mittel/Niedrig/Keine) persistieren und
            // SOFORT Cloud-Sync — damit die farbige Einfaerbung ins Drive-Backup
            // wandert und auf andere Geraete synct.
            panel.onSlotPriority = { [weak self] number, priority in
                PromptSlotStore.shared.setPriority(number: number, priority: priority) {
                    self?.onSlotsSyncRequested?()
                }
            }
            // Slot per Drag & Drop verschoben/getauscht: im Store spiegeln und
            // SOFORT synchronisieren — sonst holt der naechste Cloud-Merge den
            // alten Stand zurueck und die Verschiebung waere wieder weg.
            panel.onSlotMove = { [weak self] from, to in
                PromptSlotStore.shared.move(from: from, to: to) {
                    self?.onSlotsSyncRequested?()
                }
            }
            inputPanel = panel
        }
        inputPanel?.dock(leftOf: self, force: true)
        inputPanel?.orderFront(nil)
        inputPanel?.makeKeyAndOrderFront(nil)
        // Belegte Slots in die Zahlen-Leiste laden — bei jedem Oeffnen, damit
        // ein zwischenzeitlicher Cloud-Merge sofort sichtbar wird.
        PromptSlotStore.shared.loadMapTimesSummaries { [weak self] map, times, summaries, priorities in
            self?.inputPanel?.setSlotContents(map, timestamps: times, summaries: summaries, priorities: priorities)
        }
        inputPanelVisible = true
        updateStarVisual()
    }

    private func closeInputPanel() {
        // Nur ausblenden, NICHT zerstoeren — sonst geht der eingetippte Text
        // verloren wenn der Pillar-Stern erneut geklickt wird. Das Panel-Objekt
        // bleibt im Speicher, beim Wieder-Oeffnen ist der Text wieder da.
        inputPanel?.orderOut(nil)
        inputPanelVisible = false
        updateStarVisual()
    }

    private func updateStarVisual() {
        guard let btn = inputToggleButton else { return }
        let symbol = inputPanelVisible ? "★" : "☆"
        // Weiss im inaktiven Zustand wie die anderen Toolbar-Symbole
        // (Plus, Diskette, Zahnrad, Schriftrolle) — der Stern soll sich
        // im Ruhezustand visuell nicht von den uebrigen Buttons abheben.
        let color: NSColor = inputPanelVisible
            ? NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
            : NSColor.white
        btn.attributedTitle = NSAttributedString(
            string: symbol,
            attributes: [
                .foregroundColor: color,
                .font: NSFont.systemFont(ofSize: 14, weight: .semibold)
            ]
        )
        btn.toolTip = inputPanelVisible ? "Prompt-Eingabe ausblenden" : "Prompt-Eingabe einblenden"
    }

    /// Setzt Text ins Eingabefeld bzw. oeffnet das Fenster zuerst, falls
    /// noetig. Wird vom Voice-Overlay genutzt, damit gesprochene Prompts in
    /// das Eingabefenster geroutet werden statt direkt in die CLI.
    func routeVoiceTextToInput(_ text: String) {
        guard !text.isEmpty else { return }
        if !inputPanelVisible { openInputPanel() }
        inputPanel?.setText(text)
    }

    /// True wenn der Stern an ist und das Eingabefenster sichtbar.
    var isInputPanelVisible: Bool { inputPanelVisible }

    /// True solange irgendeine Flaeche des Prompt-Systems sichtbar ist: das
    /// Board selbst, die Prompt-Eingabe (auch im Solo-Dock, wenn das Board
    /// orderOut'd ist) oder die Historie. Wird vom AutoHideController (via
    /// AppDelegate.promptSurfaceVisibleProvider) genutzt, um das VTO NICHT
    /// einzuklappen waehrend der Benutzer hier arbeitet — auch wenn die Maus
    /// gerade nicht ueber dem Pillar liegt. Modale Untermenues (NSApp.runModal)
    /// sind zusaetzlich schon durch den RunLoop-Modus geschuetzt, weil der
    /// .default-Timer waehrend eines modalen Loops nicht feuert.
    var isAnyPromptSurfaceVisible: Bool {
        if self.isVisible { return true }
        if inputPanelVisible { return true }
        if let ip = inputPanel, ip.isVisible { return true }
        if historyPanelVisible { return true }
        if let hp = historyPanel, hp.isVisible { return true }
        return false
    }

    /// Verschiebt die GANZE Fenster-Gruppe (Promptboard, Eingabe, Historie
    /// und durch das onPanelDragged-Callback auch den Voice-Pillar) um den
    /// gleichen Versatz. Wird aus den Rechtsklick-Drags der Eingabe und
    /// Historie aufgerufen — egal an welchem Fenster der Benutzer anfasst,
    /// alle bewegen sich als ein starres Konstrukt.
    /// Wird vom AppDelegate gesetzt waehrend der Solo-Dock-Modus aktiv ist
    /// (Promptboard versteckt, Eingabe haengt direkt am Pillar). Dann darf der
    /// Drag NICHT die veraltete Board-Position als Anker nehmen — der
    /// AppDelegate verschiebt stattdessen Pillar + Eingabe direkt.
    /// Frank-Bug 2026-06-05: ohne das sprang der Pillar beim Ziehen der
    /// Eingabe nach rechts aus dem Bildschirm.
    var soloDockDragHandler: ((CGFloat, CGFloat) -> Void)?

    func translateGroup(dx: CGFloat, dy: CGFloat) {
        // Solo-Dock: Board ist versteckt → eigener Pfad (Pillar direkt).
        if let handler = soloDockDragHandler {
            handler(dx, dy)
            return
        }
        var origin = self.frame.origin
        origin.x += dx
        origin.y += dy
        self.setFrameOrigin(origin)
        // Voice-Pillar folgt ueber den existierenden onPanelDragged-Pfad.
        onPanelDragged?(origin)
        // Andockpartner re-positionieren — sie kennen die neue Boardposition
        // und docken sich exakt 4 pt links davor.
        inputPanel?.followBoardDrag(self)
        historyPanel?.followBoardDrag(self)
    }

    /// Versteckt Eingabe- und Historie-Panel — wird vom AppDelegate
    /// gerufen sobald der Benutzer aus dem Terminal in eine andere App
    /// wechselt. Wir nutzen `orderOut(nil)` (NICHT close), damit der
    /// Sichtbarkeitswunsch erhalten bleibt — beim Zurueckwechseln ins
    /// Terminal kommen die Panels automatisch wieder zurueck. So sind
    /// die Floating-Panels nie ueber Chrome o.ae. App-Fenstern zu sehen.
    func hideTransientChildren() {
        if inputPanelVisible { inputPanel?.orderOut(nil) }
        if historyPanelVisible { historyPanel?.orderOut(nil) }
    }

    /// Bringt die Eingabe-/Historie-Panels zurueck, falls der Benutzer
    /// sie vor dem App-Wechsel offen hatte. Wird nach
    /// `hideTransientChildren()` gerufen wenn das Terminal wieder aktiv
    /// ist. Position wird neu angedockt — das Promtboard kann sich in
    /// der Zwischenzeit verschoben haben.
    func showTransientChildrenIfNeeded() {
        if inputPanelVisible, let p = inputPanel, !p.isVisible {
            p.dock(leftOf: self)
            p.orderFrontRegardless()
        }
        if historyPanelVisible, let p = historyPanel, !p.isVisible {
            p.dock(leftOf: self)
            p.orderFrontRegardless()
        }
    }

    // MARK: - Historie-Fenster (Linksklick auf Eintrag → Text in Eingabe)

    @objc private func onToggleHistory() {
        if historyPanelVisible {
            closeHistoryPanel()
        } else {
            openHistoryPanel()
        }
    }

    private func openHistoryPanel() {
        // Eingabe und Historie schliessen sich gegenseitig aus — beide
        // docken am gleichen Platz links neben dem Promtboard. Wenn die
        // Eingabe offen ist, machen wir sie zu BEVOR die Historie sich
        // einblendet, damit nie beide gleichzeitig dort liegen.
        if inputPanelVisible { closeInputPanel() }
        if historyPanel == nil {
            let p = PromptHistoryPanel()
            p.onEntrySelected = { [weak self] text in
                guard let self = self else { return }
                // Eintrag in das Eingabefenster routen — und das Eingabe-
                // fenster oeffnen wenn es noch nicht offen ist (sonst
                // wuerde der Klick ins Leere gehen).
                if !self.inputPanelVisible { self.openInputPanel() }
                self.inputPanel?.setText(text)
            }
            p.onEntryEditRequested = { [weak self] entry in
                self?.editHistoryEntry(entry)
            }
            // Rechtsklick-Drag in der Historie verschiebt die GANZE Gruppe.
            p.onGroupDragDelta = { [weak self] dx, dy in
                self?.translateGroup(dx: dx, dy: dy)
            }
            historyPanel = p
        }
        // Andocken links am Promtboard — gleiche Position wie das
        // Eingabefenster. Wenn beide gleichzeitig offen sind, liegt die
        // Historie ueber dem Eingabefenster (Z-Order).
        historyPanel?.dock(leftOf: self, force: true)
        reloadHistory()
        historyPanel?.orderFront(nil)
        historyPanel?.makeKeyAndOrderFront(nil)
        historyPanelVisible = true
        updateHistoryButtonVisual()
    }

    private func closeHistoryPanel() {
        historyPanel?.close()
        historyPanel = nil
        historyPanelVisible = false
        updateHistoryButtonVisual()
    }

    /// Liest die aktuelle Historie aus dem Store und uebergibt sie ans
    /// Fenster. Wird beim Oeffnen aufgerufen — und vom AppDelegate nach
    /// jedem Submit, damit der Benutzer seinen frischen Eintrag sofort
    /// sieht ohne das Fenster zu/auf-klappen zu muessen.
    func reloadHistory() {
        guard let panel = historyPanel else { return }
        PromptHistoryStore.shared.load { entries in
            panel.render(entries)
        }
    }

    /// Wird vom AppDelegate gesetzt — stoesst nach Speichern/Loeschen eines
    /// Prompt-Zwischenspeicher-Slots SOFORT den Cloud-Upload an, damit das
    /// andere Geraet den neuen Stand erhaelt.
    var onSlotsSyncRequested: (() -> Void)?

    /// Liest die belegten Slots neu aus dem Store und faerbt die Zahlen-Leiste
    /// nach (falls die Eingabe offen ist). Wird vom AppDelegate nach dem
    /// Cloud-Merge beim Start aufgerufen.
    func reloadSlots() {
        PromptSlotStore.shared.loadMapTimesSummaries { [weak self] map, times, summaries, priorities in
            self?.inputPanel?.setSlotContents(map, timestamps: times, summaries: summaries, priorities: priorities)
        }
    }

    /// Wird gerufen nachdem ein Historie-Eintrag bearbeitet und gespeichert
    /// wurde. Der AppDelegate setzt diese Closure und stoesst dort den
    /// Cloud-Upload an, damit der bearbeitete Eintrag auch auf der
    /// Windows-Seite sichtbar wird.
    var onHistorySyncRequested: (() -> Void)?

    /// Halt fuer den modalen Editor — sonst wird er sofort wieder
    /// freigegeben und das Fenster schliesst sich von alleine.
    private var historyEditController: PromptHistoryEditController?

    /// Oeffnet den modalen Editor fuer einen Historie-Eintrag, persistiert
    /// die Aenderung und meldet das History-Panel neu zu rendern.
    fileprivate func editHistoryEntry(_ entry: PBHistoryEntry) {
        guard !entry.id.isEmpty else { return }
        let controller = PromptHistoryEditController(entry: entry)
        historyEditController = controller
        controller.onResult = { [weak self] newText in
            guard let self = self else { return }
            defer { self.historyEditController = nil }
            guard let updated = newText, updated != entry.text else { return }
            PromptHistoryStore.shared.updateText(entryId: entry.id, newText: updated) {
                self.reloadHistory()
                self.onHistorySyncRequested?()
            }
        }
        controller.showWindow(nil)
        controller.window?.center()
        controller.window?.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }

    private func updateHistoryButtonVisual() {
        guard let btn = historyToggleButton else { return }
        let color: NSColor = historyPanelVisible
            ? NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
            : NSColor(calibratedWhite: 0.80, alpha: 1)
        btn.attributedTitle = NSAttributedString(
            string: "📜",
            attributes: [
                .foregroundColor: color,
                .font: NSFont.systemFont(ofSize: 14, weight: .semibold)
            ]
        )
        btn.toolTip = historyPanelVisible ? "Prompt-Historie schliessen" : "Prompt-Historie"
    }

    // MARK: - Stern-Button (Solo-Andocken Pendant)

    /// Action des neuen Stern-Buttons in der Toolbar. Reicht den Klick 1:1
    /// nach aussen weiter — AppDelegate ruft daraufhin applySoloDockMode(true)
    /// auf und blendet das Promtboard zugunsten des am Pillar angedockten
    /// Eingabefelds aus.
    @objc private func onBoardStarTapped() {
        onBoardStarToggle?()
    }

    // MARK: - Filter "Nur aktivierte Prompts" + "Alle Haekchen entfernen"

    /// Toggle des Filter-Modus: Nur Prompts mit Always-On=true sichtbar.
    /// Aktiver Filter -> goldener Button, sonst Standard-Hintergrund. Beim
    /// Klick auf einen Tab wird der Filter automatisch wieder aufgehoben
    /// damit die Anzeige nicht "hängt" wenn der Benutzer Kategorien wechselt.
    @objc private func onToggleFilterActive() {
        filterActiveOnly.toggle()
        updateFilterButtonVisual()
        renderPrompts()
    }

    private func updateFilterButtonVisual() {
        guard let btn = filterActiveButton else { return }
        if filterActiveOnly {
            btn.layer?.backgroundColor = NSColor(calibratedRed: 0.72, green: 0.53, blue: 0.04, alpha: 1).cgColor
        } else {
            btn.layer?.backgroundColor = NSColor(calibratedWhite: 0.22, alpha: 1).cgColor
        }
    }

    /// Entfernt Always-On in allen Prompts ausser denen aus der "Allgemein"-
    /// Kategorie (Windows #1868). Visuell: gruener Flash auf dem Button als
    /// Bestaetigung. Loest danach refresh aus.
    @objc private func onClearAllChecks() {
        do {
            for cat in categories {
                if cat.name.lowercased() == "allgemein" { continue }
                let prompts = try PromptBoardStore.shared.prompts(in: cat.id)
                for var p in prompts where p.isAlwaysOn {
                    p.isAlwaysOn = false
                    p.updatedAt = Date()
                    try PromptBoardStore.shared.updatePrompt(p)
                }
            }
            flashClearButton()
            scheduleAutoBackup()
            refresh()
        } catch {
            NSLog("clearAllChecks failed: \(error.localizedDescription)")
        }
    }

    private func flashClearButton() {
        guard let btn = clearAllChecksButton else { return }
        let flash = CABasicAnimation(keyPath: "backgroundColor")
        flash.fromValue = NSColor(calibratedRed: 0.20, green: 0.80, blue: 0.20, alpha: 1).cgColor
        flash.toValue = NSColor(calibratedWhite: 0.22, alpha: 1).cgColor
        flash.duration = 0.5
        flash.timingFunction = CAMediaTimingFunction(name: .easeOut)
        btn.layer?.add(flash, forKey: "flash")
    }

    // MARK: - Originale Aktionen

    @objc private func onAddCategory() {
        guard let name = PBTextInput.ask(title: "Neue Kategorie",
                                         label: "Name:", parent: self) else { return }
        let palette = ["#DCEDEC","#FCE4EC","#FFF3E0","#E8F5E9","#EDE7F6","#F3E5F5","#E3F2FD","#FFFDE7"]
        let taken = Set(categories.map { $0.backgroundColorHex })
        let color = palette.first { !taken.contains($0) } ?? "#DCEDEC"
        let nextOrder = (categories.map { $0.sortOrder }.max() ?? -1) + 1

        let now = Date()
        let cat = PBCategory(id: UUID(), name: name, sortOrder: nextOrder,
                             backgroundColorHex: color, type: 0,
                             createdAt: now, updatedAt: now)
        do {
            try PromptBoardStore.shared.addCategory(cat)
            activeCategoryIds.insert(cat.id)
            scheduleAutoBackup()
        } catch {
            NSAlert.warn("Kategorie konnte nicht angelegt werden: \(error.localizedDescription)")
        }
        refresh()
    }

    @objc private func onRenameCategory(_ sender: NSMenuItem) {
        guard let idStr = sender.representedObject as? String,
              let id = UUID(uuidString: idStr),
              var cat = categories.first(where: { $0.id == id }) else { return }
        guard let newName = PBTextInput.ask(title: "Kategorie umbenennen",
                                            label: "Neuer Name:",
                                            initialValue: cat.name, parent: self) else { return }
        cat.name = newName
        do {
            try PromptBoardStore.shared.updateCategory(cat)
            scheduleAutoBackup()
        } catch {
            NSAlert.warn(error.localizedDescription)
        }
        refresh()
    }

    @objc private func onDeleteCategory(_ sender: NSMenuItem) {
        guard let idStr = sender.representedObject as? String,
              let id = UUID(uuidString: idStr),
              let cat = categories.first(where: { $0.id == id }) else { return }
        guard PBConfirm.ask(title: "Kategorie loeschen?",
                            message: "Kategorie '\(cat.name)' wird mit allen Prompts geloescht.",
                            parent: self) else { return }
        do {
            try PromptBoardStore.shared.deleteCategory(id)
            activeCategoryIds.remove(id)
            scheduleAutoBackup()
        } catch { NSAlert.warn(error.localizedDescription) }
        refresh()
    }

    @objc private func onAddPrompt() {
        // New prompts land in the first active category. If none are active,
        // fall back to the first category overall (or refuse if truly empty).
        guard let catId = activeCategoryIds.first ?? categories.first?.id else {
            NSAlert.warn("Lege zuerst eine Kategorie an.")
            return
        }
        guard let result = PBPromptEditDialog.ask(parent: self,
                                                  title: "Neuer Prompt",
                                                  label: "", text: "", alwaysOn: false,
                                                  prePrompt: true, postPrompt: false,
                                                  hotkey: nil) else { return }
        let now = Date()
        let newPromptId = UUID()
        let prompt = PBPrompt(id: newPromptId, categoryId: catId,
                              shortLabel: result.shortLabel,
                              originalText: result.originalText,
                              improvedText: nil, activeVersion: 0,
                              isAlwaysOn: result.isAlwaysOn,
                              isPrePrompt: result.isPrePrompt,
                              isPostPrompt: result.isPostPrompt,
                              sortOrder: 0, promptKind: "Prompt",
                              geminiModel: nil, isActiveForImprovement: false,
                              improvedByAiPromptId: nil,
                              hotkeyNumber: result.hotkeyNumber,
                              hotkeyLetter: result.hotkeyLetter,
                              createdAt: now, updatedAt: now)
        do {
            // "Last wins" fuer beide Hotkeys: Number und Letter global eindeutig.
            if let hk = result.hotkeyNumber {
                try PromptBoardStore.shared.stripHotkeyFromOthers(hotkey: hk, exceptId: newPromptId)
            }
            if let hl = result.hotkeyLetter {
                try PromptBoardStore.shared.stripLetterFromOthers(letter: hl, exceptId: newPromptId)
            }
            try PromptBoardStore.shared.addPrompt(prompt)
            scheduleAutoBackup()
        } catch {
            NSAlert.warn(error.localizedDescription)
        }
        renderPrompts()
    }

    @objc private func onInsertPrompt(_ sender: NSButton) {
        tvoDebug("[PBPanel] onInsertPrompt (label-button) id=\(sender.identifier?.rawValue ?? "nil")")
        insertPromptByIdString(sender.identifier?.rawValue)
    }

    /// Fired when the user clicks the row background (outside checkbox / edit /
    /// delete). Inserts the prompt exactly as the label-button would.
    /// Row-Click toggelt jetzt Always-On (Windows-Pendant #1866). Vorher
    /// fuegte er den Prompt direkt ein — diese Aufgabe hat jetzt der
    /// dedizierte ▶-Insert-Button. Vorteil: Always-On laesst sich mit
    /// einem Klick auf die ganze Reihe schalten, ohne die kleine
    /// 18x18-Checkbox punktgenau treffen zu muessen.
    @objc private func onRowClick(_ sender: NSClickGestureRecognizer) {
        tvoDebug("[PBPanel] onRowClick id=\(sender.view?.identifier?.rawValue ?? "nil") panelLevel=\(self.level.rawValue)")
        toggleAlwaysOnByIdString(sender.view?.identifier?.rawValue)
    }

    /// Click-Handler fuer den dedizierten ▶-Insert-Button (Windows #1866).
    @objc private func onInsertPromptClick(_ sender: NSButton) {
        tvoDebug("[PBPanel] onInsertPromptClick id=\(sender.identifier?.rawValue ?? "nil")")
        insertPromptByIdString(sender.identifier?.rawValue)
    }

    /// Hilfsmethode: Toggelt den Always-On-Status eines Prompts anhand
    /// seiner UUID-String-Identitaet (kommt aus dem Row-View-Identifier).
    private func toggleAlwaysOnByIdString(_ idStr: String?) {
        guard let idStr = idStr,
              let id = UUID(uuidString: idStr),
              var prompt = currentPrompts.first(where: { $0.id == id }) else { return }
        prompt.isAlwaysOn.toggle()
        prompt.updatedAt = Date()
        do {
            try PromptBoardStore.shared.updatePrompt(prompt)
            scheduleAutoBackup()
            renderPrompts()
        } catch {
            NSLog("toggleAlwaysOnByIdString failed: \(error.localizedDescription)")
        }
    }

    /// Fired when the user RIGHT-clicks the row. Opens the prompt editor —
    /// same result as clicking the ✎ pencil, but with the whole bar as a
    /// larger hit target. Handy when the pencil is too small to hit
    /// reliably on Retina displays.
    @objc private func onRowRightClick(_ sender: NSClickGestureRecognizer) {
        tvoDebug("[PBPanel] onRowRightClick id=\(sender.view?.identifier?.rawValue ?? "nil")")
        guard let idStr = sender.view?.identifier?.rawValue,
              let id = UUID(uuidString: idStr) else { return }
        openEditorForPrompt(id: id)
    }

    // MARK: - NSGestureRecognizerDelegate

    /// Block the row-level click when the mouse is over an NSButton subview
    /// (checkbox, ✎, ✕). Those buttons drive their own actions; without this
    /// guard, clicking ✎ would both open the editor AND insert the prompt.
    func gestureRecognizer(_ gestureRecognizer: NSGestureRecognizer,
                           shouldAttemptToRecognizeWith event: NSEvent) -> Bool {
        guard let row = gestureRecognizer.view,
              let superview = row.superview else { return true }
        let pointInSuper = superview.convert(event.locationInWindow, from: nil)
        let hit = row.hitTest(pointInSuper)
        return !(hit is NSButton)
    }

    private func insertPromptByIdString(_ idStr: String?) {
        guard let idStr = idStr,
              let id = UUID(uuidString: idStr),
              let prompt = currentPrompts.first(where: { $0.id == id }) else {
            let n = self.currentPrompts.count
            tvoDebug("[PBPanel] insertPromptByIdString MISS idStr=\(idStr ?? "nil") currentPrompts.count=\(n)")
            return
        }
        tvoDebug("[PBPanel] insertPromptByIdString HIT label=\(prompt.shortLabel) textLen=\(prompt.effectiveText.count)")
        onInsertText?(prompt.effectiveText)
    }

    @objc private func onEditPrompt(_ sender: NSButton) {
        guard let idStr = sender.identifier?.rawValue,
              let id = UUID(uuidString: idStr) else { return }
        openEditorForPrompt(id: id)
    }

    /// Shared entry point for opening the prompt editor — reused by both
    /// the ✎ button and the whole-row right-click gesture.
    private func openEditorForPrompt(id: UUID) {
        guard var prompt = currentPrompts.first(where: { $0.id == id }) else { return }
        guard let result = PBPromptEditDialog.ask(parent: self,
                                                  title: "Prompt bearbeiten",
                                                  label: prompt.shortLabel,
                                                  text: prompt.originalText,
                                                  alwaysOn: prompt.isAlwaysOn,
                                                  prePrompt: prompt.isPrePrompt,
                                                  postPrompt: prompt.isPostPrompt,
                                                  hotkey: prompt.hotkeyNumber,
                                                  hotkeyLetter: prompt.hotkeyLetter) else { return }
        prompt.shortLabel = result.shortLabel
        prompt.originalText = result.originalText
        prompt.isAlwaysOn = result.isAlwaysOn
        prompt.isPrePrompt = result.isPrePrompt
        prompt.isPostPrompt = result.isPostPrompt
        prompt.hotkeyNumber = result.hotkeyNumber
        prompt.hotkeyLetter = result.hotkeyLetter
        do {
            // "Last wins" fuer beide Hotkeys: Number und Letter global eindeutig.
            if let hk = result.hotkeyNumber {
                try PromptBoardStore.shared.stripHotkeyFromOthers(hotkey: hk, exceptId: prompt.id)
            }
            if let hl = result.hotkeyLetter {
                try PromptBoardStore.shared.stripLetterFromOthers(letter: hl, exceptId: prompt.id)
            }
            try PromptBoardStore.shared.updatePrompt(prompt)
            scheduleAutoBackup()
        } catch {
            NSAlert.warn(error.localizedDescription)
        }
        renderPrompts()
    }

    @objc private func onDeletePrompt(_ sender: NSButton) {
        guard let idStr = sender.identifier?.rawValue,
              let id = UUID(uuidString: idStr),
              let prompt = currentPrompts.first(where: { $0.id == id }) else { return }
        guard PBConfirm.ask(title: "Prompt loeschen?",
                            message: "Prompt '\(prompt.shortLabel)' wirklich loeschen?",
                            parent: self) else { return }
        do {
            try PromptBoardStore.shared.deletePrompt(id)
            scheduleAutoBackup()
        } catch {
            NSAlert.warn(error.localizedDescription)
        }
        renderPrompts()
    }

    @objc private func onSettings() {
        guard let settings = try? PromptBoardStore.shared.settings() else { return }
        // The settings window itself sits at .modalPanel level, so it naturally
        // appears above our .floating pillar — no level juggling needed.
        guard let result = PBSettingsDialog.ask(parent: self, settings: settings) else { return }
        var latest = (try? PromptBoardStore.shared.settings()) ?? settings
        latest.groqApiKey = result.groqApiKey
        latest.geminiApiKey = result.geminiApiKey
        latest.separatorTemplate = result.separatorTemplate
        latest.googleClientId = result.googleClientId
        latest.googleClientSecret = result.googleClientSecret
        // Windows-1:1-Felder.
        latest.autoHide = result.autoHide
        latest.orientation = result.orientation
        latest.persistOverlayPosition = result.persistOverlayPosition
        try? PromptBoardStore.shared.updateSettings(latest)
    }

    @objc private func onToggleAlwaysOn(_ sender: NSButton) {
        guard let idStr = sender.identifier?.rawValue,
              let id = UUID(uuidString: idStr),
              var prompt = currentPrompts.first(where: { $0.id == id }) else { return }
        prompt.isAlwaysOn.toggle()
        prompt.updatedAt = Date()
        do {
            try PromptBoardStore.shared.updatePrompt(prompt)
            scheduleAutoBackup()
            renderPrompts()
        } catch {
            NSLog("toggle isAlwaysOn failed: \(error.localizedDescription)")
        }
    }

    @objc private func onBackup() {
        guard let action = PBTextInput.ask(
            title: "Backup / Wiederherstellen",
            label: "E = Export Datei, I = Import Datei, G = Google Drive sichern, R = Google Drive laden:",
            parent: self) else { return }
        switch action.trimmingCharacters(in: .whitespaces).uppercased() {
        case "E": exportToFile()
        case "I": importFromFile()
        case "G": uploadToDrive()
        case "R": restoreFromDrive()
        default: break
        }
    }

    // MARK: - Backup helpers

    private func buildBackupJson() throws -> String { try Self.buildBackupJson() }

    /// STATIC-Variante — der AppDelegate braucht sie beim Start, wenn es noch
    /// gar kein Board-Panel gibt (Fingerprint-Vergleich vor dem Launch-Restore).
    static func buildBackupJson() throws -> String {
        let cats = try PromptBoardStore.shared.allCategories()
        var allPrompts: [PBPrompt] = []
        for c in cats { allPrompts.append(contentsOf: try PromptBoardStore.shared.prompts(in: c.id)) }
        let settings = try PromptBoardStore.shared.settings()

        let payload: [String: Any] = [
            "ExportedAt": ISO8601DateFormatter().string(from: Date()),
            "SeparatorTemplate": settings.separatorTemplate,
            "Categories": cats.map { c -> [String: Any] in
                [
                    "Id": c.id.uuidString,
                    "Name": c.name,
                    "SortOrder": c.sortOrder,
                    "BackgroundColorHex": c.backgroundColorHex,
                    "Type": c.type,
                ]
            },
            "Prompts": allPrompts.map { p -> [String: Any] in
                // Vollstaendige Feld-Liste — frueher fehlten promptKind,
                // geminiModel, isActiveForImprovement, improvedByAiPromptId
                // und hotkeyLetter (die wurden beim Restore auf Defaults
                // zurueckgesetzt → "unvollstaendig"-Symptom).
                var dict: [String: Any] = [
                    "Id": p.id.uuidString,
                    "CategoryId": p.categoryId.uuidString,
                    "ShortLabel": p.shortLabel,
                    "OriginalText": p.originalText,
                    "ActiveVersion": p.activeVersion,
                    "IsAlwaysOn": p.isAlwaysOn,
                    "IsPrePrompt": p.isPrePrompt,
                    "IsPostPrompt": p.isPostPrompt,
                    "SortOrder": p.sortOrder,
                    "PromptKind": p.promptKind,
                    "IsActiveForImprovement": p.isActiveForImprovement,
                ]
                if let it = p.improvedText                { dict["ImprovedText"]            = it }
                if let hk = p.hotkeyNumber                { dict["HotkeyNumber"]            = hk }
                if let hl = p.hotkeyLetter                { dict["HotkeyLetter"]            = hl }
                if let gm = p.geminiModel                 { dict["GeminiModel"]             = gm }
                if let ai = p.improvedByAiPromptId        { dict["ImprovedByAiPromptId"]    = ai.uuidString }
                return dict
            },
        ]
        let data = try JSONSerialization.data(withJSONObject: payload, options: [.prettyPrinted])
        return String(data: data, encoding: .utf8) ?? ""
    }

    @discardableResult
    private func applyBackupJson(_ json: String) throws -> BackupApplyResult {
        try Self.applyBackupJson(json)
    }

    /// Applies a backup JSON as the authoritative state of the local store.
    /// Upserts everything the backup contains AND deletes any local prompt
    /// or category whose id is NOT in the backup — otherwise a prompt
    /// deleted on another machine would re-appear locally on restore.
    /// Static so it can run at app launch before the PromptBoard panel
    /// is lazily created.
    ///
    /// Returnt die Anzahl an Items, die NEU lokal hinzukommen (Prompt/Category
    /// existiert lokal noch nicht). Updates zaehlen nicht mit — nur "neu".
    /// Wird vom Launch-Auto-Sync genutzt um "+N neu" im PromptBoard anzuzeigen.
    @discardableResult
    static func applyBackupJson(_ json: String) throws -> BackupApplyResult {
        guard let data = json.data(using: .utf8),
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw NSError(domain: "PromptBoardPanel", code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Ungueltiges Backup."])
        }
        let cats = root["Categories"] as? [[String: Any]] ?? []
        let prompts = root["Prompts"] as? [[String: Any]] ?? []

        // Lokale IDs einsammeln BEVOR wir upserten — sonst koennen wir
        // "neu" nicht von "Update" unterscheiden.
        let localCategoryIdsBefore: Set<UUID> = {
            guard let locals = try? PromptBoardStore.shared.allCategories() else { return [] }
            return Set(locals.map { $0.id })
        }()
        let localPromptIdsBefore: Set<UUID> = {
            guard let locals = try? PromptBoardStore.shared.allCategories() else { return [] }
            var ids = Set<UUID>()
            for c in locals {
                if let ps = try? PromptBoardStore.shared.prompts(in: c.id) {
                    for p in ps { ids.insert(p.id) }
                }
            }
            return ids
        }()

        // Collect the ids the backup considers authoritative.
        var remoteCategoryIds = Set<UUID>()
        var remotePromptIds = Set<UUID>()

        for c in cats {
            guard let idStr = c["Id"] as? String, let id = UUID(uuidString: idStr) else { continue }
            remoteCategoryIds.insert(id)
            let now = Date()
            let cat = PBCategory(
                id: id,
                name: (c["Name"] as? String) ?? "",
                sortOrder: (c["SortOrder"] as? Int) ?? 0,
                backgroundColorHex: (c["BackgroundColorHex"] as? String) ?? "#DCEDEC",
                type: (c["Type"] as? Int) ?? 0,
                createdAt: now, updatedAt: now)
            try PromptBoardStore.shared.upsertCategory(cat)
        }
        for p in prompts {
            guard let idStr = p["Id"] as? String, let id = UUID(uuidString: idStr),
                  let catStr = p["CategoryId"] as? String, let catId = UUID(uuidString: catStr) else { continue }
            remotePromptIds.insert(id)
            let now = Date()
            let prompt = PBPrompt(
                id: id, categoryId: catId,
                shortLabel: (p["ShortLabel"] as? String) ?? "",
                originalText: (p["OriginalText"] as? String) ?? "",
                improvedText: p["ImprovedText"] as? String,
                activeVersion: (p["ActiveVersion"] as? Int) ?? 0,
                isAlwaysOn: (p["IsAlwaysOn"] as? Bool) ?? false,
                // Default for older backups that pre-date the Pre/Post split:
                // missing → IsPrePrompt = true (= legacy "always-on means
                // prefix" behaviour), IsPostPrompt = false.
                isPrePrompt: (p["IsPrePrompt"] as? Bool) ?? true,
                isPostPrompt: (p["IsPostPrompt"] as? Bool) ?? false,
                sortOrder: (p["SortOrder"] as? Int) ?? 0,
                promptKind: (p["PromptKind"] as? String) ?? "Prompt",
                geminiModel: p["GeminiModel"] as? String,
                isActiveForImprovement: (p["IsActiveForImprovement"] as? Bool) ?? false,
                improvedByAiPromptId: (p["ImprovedByAiPromptId"] as? String).flatMap(UUID.init(uuidString:)),
                hotkeyNumber: p["HotkeyNumber"] as? Int,
                hotkeyLetter: p["HotkeyLetter"] as? String,
                createdAt: now, updatedAt: now)
            try PromptBoardStore.shared.upsertPrompt(prompt)
        }

        // Delete local rows that aren't in the authoritative backup.
        // Prompts first (they reference categories), categories after.
        if let localCats = try? PromptBoardStore.shared.allCategories() {
            for cat in localCats where !remoteCategoryIds.contains(cat.id) {
                if let localPrompts = try? PromptBoardStore.shared.prompts(in: cat.id) {
                    for p in localPrompts {
                        try? PromptBoardStore.shared.deletePrompt(p.id)
                    }
                }
                try? PromptBoardStore.shared.deleteCategory(cat.id)
            }
            // Also drop stale prompts whose category still exists but whose
            // id vanished from the backup.
            for cat in localCats where remoteCategoryIds.contains(cat.id) {
                if let localPrompts = try? PromptBoardStore.shared.prompts(in: cat.id) {
                    for p in localPrompts where !remotePromptIds.contains(p.id) {
                        try? PromptBoardStore.shared.deletePrompt(p.id)
                    }
                }
            }
        }

        // Neu = im Backup vorhanden, lokal vor dem Apply NICHT vorhanden.
        let newCategories = remoteCategoryIds.subtracting(localCategoryIdsBefore).count
        let newPrompts    = remotePromptIds.subtracting(localPromptIdsBefore).count
        return BackupApplyResult(newPrompts: newPrompts, newCategories: newCategories)
    }

    // MARK: - Backup-Fingerabdruck (Schutz des Start-Restores)
    // Portierung von Windows `BuildBackupFingerprintAsync` / `BackupFingerprint` /
    // `ReadLastSyncFingerprint` (Commit "Protect launch restore from local changes").
    //
    // Ohne diesen Schutz konnte der Start-Restore lokale Aenderungen wegwerfen:
    // wer auf diesem Rechner Prompts anlegt, waehrend Drive gerade ein AELTERES
    // Backup vorhaelt, bekam beim naechsten Start das alte Backup aufgespielt.
    // Der Fingerabdruck erkennt "lokal hat sich seit dem letzten Sync etwas
    // getaendert" und laesst den Restore dann bewusst aus.

    private static let lastSyncFingerprintKey = "pbLastSyncFingerprint"

    /// SHA-256 ueber den Backup-Inhalt OHNE `ExportedAt` — der Zeitstempel
    /// aendert sich bei jedem Aufbau und wuerde sonst jeden Vergleich zerstoeren.
    /// Die Schluessel werden sortiert serialisiert, damit derselbe Datenstand
    /// immer denselben Abdruck ergibt.
    static func backupFingerprint(_ json: String) -> String? {
        guard let data = json.data(using: .utf8),
              var root = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return nil }
        root.removeValue(forKey: "ExportedAt")
        guard let normalized = try? JSONSerialization.data(withJSONObject: root,
                                                          options: [.sortedKeys])
        else { return nil }
        return SHA256.hash(data: normalized).map { String(format: "%02X", $0) }.joined()
    }

    /// Fingerabdruck des AKTUELLEN lokalen Stands.
    static func currentBackupFingerprint() -> String? {
        guard let json = try? buildBackupJson() else { return nil }
        return backupFingerprint(json)
    }

    /// Fingerabdruck des Stands, der zuletzt erfolgreich synchronisiert wurde
    /// (nil = noch nie synchronisiert; dann darf der Restore laufen).
    static func readLastSyncFingerprint() -> String? {
        UserDefaults.standard.string(forKey: lastSyncFingerprintKey)
    }

    /// Merkt sich den Fingerabdruck des gerade synchronisierten Stands.
    /// `json` ist der Backup-Inhalt, der hoch- bzw. eingespielt wurde.
    static func writeLastSyncFingerprint(backupJson: String) {
        guard let fp = backupFingerprint(backupJson) else { return }
        UserDefaults.standard.set(fp, forKey: lastSyncFingerprintKey)
    }

    /// Merkt sich den Fingerabdruck des aktuellen lokalen Stands — nach einem
    /// erfolgreichen Upload ist genau der der synchronisierte Stand.
    static func writeLastSyncFingerprintFromCurrentState() {
        guard let fp = currentBackupFingerprint() else { return }
        UserDefaults.standard.set(fp, forKey: lastSyncFingerprintKey)
    }

    /// Returns the `ExportedAt` timestamp from a backup JSON, or nil if
    /// the field is missing / unparseable. Used to decide whether the
    /// remote backup is newer than the local state.
    ///
    /// Cross-platform note: macOS writes ISO-8601 without fractional
    /// seconds (`2026-04-25T16:23:21Z`), Windows' C# JsonSerializer writes
    /// it WITH fractional seconds (`2026-04-25T16:23:21.1234567Z`). A
    /// single `ISO8601DateFormatter` only accepts one of the two forms,
    /// so we try both. Without this, a Windows-uploaded backup would
    /// silently fail to restore on macOS — see launch-restore log line
    /// "no ExportedAt in backup" from 2026-04-25.
    static func backupExportedAt(from json: String) -> Date? {
        guard let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let iso = root["ExportedAt"] as? String else { return nil }
        let plain = ISO8601DateFormatter()
        if let d = plain.date(from: iso) { return d }
        let frac = ISO8601DateFormatter()
        frac.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = frac.date(from: iso) { return d }
        // Last-ditch: trim fractional seconds and retry the plain parser.
        // Catches odd shapes like trailing "+00:00" or single-digit fractions
        // that neither preset accepts.
        if let dot = iso.firstIndex(of: ".") {
            let zIdx = iso[dot...].firstIndex(where: { $0 == "Z" || $0 == "+" || $0 == "-" })
                ?? iso.endIndex
            let trimmed = String(iso[..<dot]) + String(iso[zIdx...])
            if let d = plain.date(from: trimmed) { return d }
        }
        return nil
    }

    private func exportToFile() {
        let save = NSSavePanel()
        save.nameFieldStringValue = "promptboard-backup-\(Int(Date().timeIntervalSince1970)).json"
        save.allowedContentTypes = [.json]
        // Use the presenter so the Save panel is guaranteed visible (it would
        // otherwise open behind our floating pillar → user doesn't see it,
        // clicks fall into the modal trap → beep beep).
        guard PBModalPresenter.runModal(on: save) == .OK, let url = save.url else { return }
        do {
            let json = try buildBackupJson()
            try json.write(to: url, atomically: true, encoding: .utf8)
            NSAlert.warn("Backup gespeichert: \(url.path)")
        } catch {
            NSAlert.warn("Export fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    private func importFromFile() {
        let open = NSOpenPanel()
        open.allowedContentTypes = [.json]
        open.allowsMultipleSelection = false
        // NSOpenPanel is a subclass of NSSavePanel — same presenter works.
        guard PBModalPresenter.runModal(on: open) == .OK, let url = open.url else { return }
        guard PBConfirm.ask(title: "Import bestaetigen",
                            message: "Vorhandene Eintraege mit gleicher ID werden ueberschrieben.",
                            confirmLabel: "Importieren", parent: self) else { return }
        do {
            let json = try String(contentsOf: url)
            try applyBackupJson(json)
            NSAlert.warn("Import abgeschlossen.")
            refresh()
        } catch {
            NSAlert.warn("Import fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    private func uploadToDrive() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            NSAlert.warn("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.")
            return
        }
        do {
            let json = try buildBackupJson()
            GoogleDriveBackupService.shared.upload(json: json) { [weak self] result in
                DispatchQueue.main.async {
                    switch result {
                    case .success:
                        Self.writeLastSyncFingerprint(backupJson: json)
                        self?.recordSuccessfulSync()
                        NSAlert.warn("Backup bei Google Drive gespeichert.")
                    case .failure(let e):
                        NSAlert.warn("Upload fehlgeschlagen: \(e.localizedDescription)")
                    }
                }
            }
        } catch {
            NSAlert.warn("Upload fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    private func restoreFromDrive() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            NSAlert.warn("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.")
            return
        }
        GoogleDriveBackupService.shared.downloadLatest { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }
                switch result {
                case .failure(let e):
                    NSAlert.warn("Download fehlgeschlagen: \(e.localizedDescription)")
                case .success(nil):
                    NSAlert.warn("Kein Backup bei Google Drive gefunden.")
                case .success(let json?):
                    guard PBConfirm.ask(title: "Google-Drive-Backup laden",
                                        message: "Lokale Eintraege mit gleicher ID werden ueberschrieben.",
                                        confirmLabel: "Einspielen", parent: self) else { return }
                    do {
                        try self.applyBackupJson(json)
                        // Eingespielter Stand = synchronisierter Stand. Ohne das
                        // haelt der Start-Restore ihn beim naechsten Start fuer
                        // "lokal veraendert" und laeuft nie wieder.
                        Self.writeLastSyncFingerprintFromCurrentState()
                        self.recordSuccessfulSync()
                        NSAlert.warn("Google-Drive-Backup eingespielt.")
                        self.refresh()
                    } catch {
                        NSAlert.warn("Einspielen fehlgeschlagen: \(error.localizedDescription)")
                    }
                }
            }
        }
    }

    // MARK: - Drag & Drop between categories

    /// Handler invoked by PBCategoryTabButton when a prompt is dropped on it.
    /// Updates the prompt's categoryId, schedules an auto-backup and
    /// re-renders the panel so the prompt shows up under its new color.
    fileprivate func handlePromptDrop(promptId: UUID, onCategory targetCategoryId: UUID) {
        guard var prompt = currentPrompts.first(where: { $0.id == promptId }) else { return }
        if prompt.categoryId == targetCategoryId { return }
        prompt.categoryId = targetCategoryId
        prompt.updatedAt = Date()
        do {
            try PromptBoardStore.shared.updatePrompt(prompt)
            scheduleAutoBackup()
        } catch {
            NSAlert.warn("Verschieben fehlgeschlagen: \(error.localizedDescription)")
            return
        }
        refresh()
    }

    // MARK: - Reihenfolge per Drag & Drop (Portierung von Windows)
    // Windows kann zwei Dinge, die macOS bisher fehlten:
    //   * einen Prompt auf eine ANDERE ZEILE ziehen -> Reihenfolge innerhalb
    //     der Kategorie aendern (OnPromptDroppedOnRowAsync)
    //   * einen Kategorie-Tab auf einen anderen ziehen -> Kategorien tauschen
    //     (OnCategoryDroppedOnCategoryAsync)
    // Der bereits vorhandene Pfad "Prompt auf Kategorie-Tab" (= Kategorie
    // wechseln) bleibt unveraendert daneben bestehen.

    /// Setzt den Quell-Prompt vor (`above`) bzw. hinter den Ziel-Prompt und
    /// nummeriert die ganze Kategorie lueckenlos neu. Nur INNERHALB derselben
    /// Kategorie — der Wechsel zwischen Kategorien laeuft weiter ueber den
    /// Kategorie-Tab (genau wie unter Windows).
    /// Darf hier ueberhaupt umsortiert werden? Nur wenn beide Prompts in
    /// DERSELBEN Kategorie liegen — sonst zeigt die Zeile gar keine Drop-Linie
    /// und der Drag landet stattdessen (wie bisher) auf einem Kategorie-Tab.
    fileprivate func canReorder(sourceId: UUID, targetId: UUID?) -> Bool {
        guard let targetId = targetId, sourceId != targetId,
              let s = currentPrompts.first(where: { $0.id == sourceId }),
              let t = currentPrompts.first(where: { $0.id == targetId }) else { return false }
        return s.categoryId == t.categoryId
    }

    fileprivate func handlePromptReorder(sourceId: UUID, targetId: UUID, above: Bool) {
        guard sourceId != targetId,
              let source = currentPrompts.first(where: { $0.id == sourceId }),
              let target = currentPrompts.first(where: { $0.id == targetId }),
              source.categoryId == target.categoryId else { return }

        do {
            var prompts = try PromptBoardStore.shared.prompts(in: target.categoryId)
                .sorted {
                    if $0.sortOrder != $1.sortOrder { return $0.sortOrder < $1.sortOrder }
                    return $0.shortLabel.localizedCaseInsensitiveCompare($1.shortLabel) == .orderedAscending
                }
            guard let moving = prompts.first(where: { $0.id == sourceId }) else { return }
            prompts.removeAll { $0.id == sourceId }

            var insertIdx = prompts.firstIndex(where: { $0.id == targetId }) ?? prompts.count
            if !above { insertIdx += 1 }
            insertIdx = max(0, min(insertIdx, prompts.count))
            prompts.insert(moving, at: insertIdx)

            // Lueckenlos neu nummerieren (0, 1, 2, …) und nur schreiben, was
            // sich wirklich geaendert hat.
            for (i, var p) in prompts.enumerated() where p.sortOrder != i {
                p.sortOrder = i
                p.updatedAt = Date()
                try PromptBoardStore.shared.updatePrompt(p)
            }
            scheduleAutoBackup()
        } catch {
            NSAlert.warn("Reihenfolge aendern fehlgeschlagen: \(error.localizedDescription)")
            return
        }
        refresh()
    }

    /// Tauscht die Reihenfolge zweier Kategorien. Haben beide (nach einem
    /// Import) denselben `sortOrder`, wird zuerst lueckenlos neu nummeriert —
    /// sonst waere der Tausch wirkungslos. 1:1 zum Windows-Verhalten.
    fileprivate func handleCategoryReorder(sourceId: UUID, targetId: UUID) {
        guard sourceId != targetId else { return }
        do {
            var all = try PromptBoardStore.shared.allCategories()
            guard let sIdx = all.firstIndex(where: { $0.id == sourceId }),
                  let tIdx = all.firstIndex(where: { $0.id == targetId }) else { return }

            if all[sIdx].sortOrder == all[tIdx].sortOrder {
                var ordered = all.sorted {
                    if $0.sortOrder != $1.sortOrder { return $0.sortOrder < $1.sortOrder }
                    return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
                }
                for i in ordered.indices where ordered[i].sortOrder != i {
                    ordered[i].sortOrder = i
                    ordered[i].updatedAt = Date()
                    try PromptBoardStore.shared.updateCategory(ordered[i])
                }
                all = try PromptBoardStore.shared.allCategories()
            }

            guard var source = all.first(where: { $0.id == sourceId }),
                  var target = all.first(where: { $0.id == targetId }) else { return }
            let sourceOrder = source.sortOrder
            source.sortOrder = target.sortOrder
            target.sortOrder = sourceOrder
            source.updatedAt = Date()
            target.updatedAt = Date()
            try PromptBoardStore.shared.updateCategory(source)
            try PromptBoardStore.shared.updateCategory(target)
            scheduleAutoBackup()
        } catch {
            NSAlert.warn("Kategorien tauschen fehlgeschlagen: \(error.localizedDescription)")
            return
        }
        refresh()
    }
}

// MARK: - Drag-source row view

/// NSView subclass for prompt rows that can act as a drag source. The
/// row's mouseDown stores the start point + arms the drag; mouseDragged
/// triggers a real drag session once the cursor has moved past a small
/// threshold (so a quick click still inserts the prompt as before).
fileprivate final class PBPromptRowView: NSView, NSDraggingSource {
    var promptId: UUID?
    weak var owner: PromptBoardPanel?
    private var mouseDownAt: NSPoint = .zero
    private var dragArmed = false

    /// Pasteboard type used to identify our prompt drags. macOS would
    /// otherwise also accept stray strings (e.g. selected text) as drops.
    static let pasteboardType = NSPasteboard.PasteboardType("com.tvo.PromptId")

    func draggingSession(_ session: NSDraggingSession,
                         sourceOperationMaskFor context: NSDraggingContext) -> NSDragOperation {
        return .move
    }

    override func mouseDown(with event: NSEvent) {
        mouseDownAt = self.convert(event.locationInWindow, from: nil)
        dragArmed = true
        // Don't call super — the gesture recognizers attached by buildRow
        // already drive insert/edit. Calling super.mouseDown on a plain
        // NSView is a no-op anyway.
    }

    override func mouseDragged(with event: NSEvent) {
        guard dragArmed, let promptId = promptId else { return }
        let cur = self.convert(event.locationInWindow, from: nil)
        let dx = cur.x - mouseDownAt.x
        let dy = cur.y - mouseDownAt.y
        // 6-pixel threshold (squared = 36) — matches the Windows side.
        if dx * dx + dy * dy < 36 { return }
        dragArmed = false

        let pb = NSPasteboardItem()
        pb.setString(promptId.uuidString, forType: PBPromptRowView.pasteboardType)
        let item = NSDraggingItem(pasteboardWriter: pb)

        // Snapshot of self for the drag image — AppKit would render a
        // generic icon otherwise. cacheDisplay produces an exact pixel
        // copy of the row including category tint and timestamp.
        if let bmp = self.bitmapImageRepForCachingDisplay(in: self.bounds) {
            self.cacheDisplay(in: self.bounds, to: bmp)
            let img = NSImage(size: self.bounds.size)
            img.addRepresentation(bmp)
            item.setDraggingFrame(self.bounds, contents: img)
        } else {
            item.draggingFrame = self.bounds
        }

        beginDraggingSession(with: [item], event: event, source: self)
    }

    // MARK: - Zeile als Drop-Ziel (Reihenfolge aendern)
    // Portierung von Windows `OnPromptRowDragOver` / `OnPromptDroppedOnRowAsync`:
    // ein Prompt laesst sich auf eine andere ZEILE ziehen und landet dort davor
    // oder dahinter — je nachdem, ob die obere oder untere Haelfte getroffen
    // wird. Eine goldene Linie oben bzw. unten zeigt an, wo er einrastet.

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        registerForDraggedTypes([PBPromptRowView.pasteboardType])
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht benutzt") }

    private var dropAbove: Bool?

    private func sourcePromptId(_ sender: NSDraggingInfo) -> UUID? {
        guard let str = sender.draggingPasteboard.string(forType: PBPromptRowView.pasteboardType)
        else { return nil }
        return UUID(uuidString: str)
    }

    /// Obere oder untere Haelfte der Zeile getroffen? Bestimmt, ob der Prompt
    /// VOR oder HINTER dem Ziel einsortiert wird.
    private func isAbove(_ sender: NSDraggingInfo) -> Bool {
        let p = convert(sender.draggingLocation, from: nil)
        // Die Zeilen liegen in einer geflippten Stack-View: Y waechst nach
        // unten. „Oben" ist dann die kleinere Y-Haelfte.
        return isFlipped ? p.y < bounds.height / 2 : p.y > bounds.height / 2
    }

    private func showDropLine(_ above: Bool?) {
        dropAbove = above
        guard let above = above else {
            layer?.borderWidth = 0
            return
        }
        layer?.borderWidth = 0
        // Nur EINE Kante faerben: AppKit-Layer koennen keine einseitigen
        // Rahmen, deshalb eine duenne Hilfsschicht als Linie.
        dropLine.removeFromSuperlayer()
        dropLine.frame = above
            ? CGRect(x: 0, y: isFlipped ? 0 : bounds.height - 2, width: bounds.width, height: 2)
            : CGRect(x: 0, y: isFlipped ? bounds.height - 2 : 0, width: bounds.width, height: 2)
        dropLine.backgroundColor = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1).cgColor
        layer?.addSublayer(dropLine)
    }

    private let dropLine = CALayer()

    override func draggingEntered(_ sender: NSDraggingInfo) -> NSDragOperation {
        guard let from = sourcePromptId(sender), from != promptId,
              owner?.canReorder(sourceId: from, targetId: promptId) == true else { return [] }
        showDropLine(isAbove(sender))
        return .move
    }

    override func draggingUpdated(_ sender: NSDraggingInfo) -> NSDragOperation {
        guard let from = sourcePromptId(sender), from != promptId,
              owner?.canReorder(sourceId: from, targetId: promptId) == true else { return [] }
        let above = isAbove(sender)
        if above != dropAbove { showDropLine(above) }
        return .move
    }

    override func draggingExited(_ sender: NSDraggingInfo?) {
        showDropLine(nil)
        dropLine.removeFromSuperlayer()
    }

    override func performDragOperation(_ sender: NSDraggingInfo) -> Bool {
        let above = dropAbove ?? isAbove(sender)
        showDropLine(nil)
        dropLine.removeFromSuperlayer()
        guard let from = sourcePromptId(sender), let target = promptId, from != target else { return false }
        owner?.handlePromptReorder(sourceId: from, targetId: target, above: above)
        return true
    }
}

// MARK: - Flipped vertical stack

/// NSStackView subclass that reports `isFlipped = true` so Y-origin sits at
/// the TOP of the view, not the bottom. Without this the prompt rows pile
/// up at the bottom of the scroll view's document area when there are
/// fewer prompts than fit on screen — because plain AppKit views use the
/// mathematical convention (Y grows upward). Flipping aligns the layout
/// with how a list naturally reads from top to bottom and matches the
/// Windows panel's WPF default. The change has no effect when the stack
/// already overflows the scroll view (scrolling logic is unaffected).
fileprivate final class PBFlippedStackView: NSStackView {
    override var isFlipped: Bool { true }
}

// MARK: - Drop-target category tab

/// NSButton subclass that accepts prompt drags and forwards them to
/// PromptBoardPanel.handlePromptDrop. Tab is highlighted while a drag
/// hovers over it — restored to its normal style on exit.
fileprivate final class PBCategoryTabButton: NSButton, NSDraggingSource {
    var categoryId: UUID?
    weak var owner: PromptBoardPanel?
    private var savedBackground: CGColor?

    /// Eigener Pasteboard-Typ fuer KATEGORIE-Drags — ein Tab, der auf einen
    /// anderen Tab gezogen wird, tauscht die Reihenfolge (Windows-Pendant:
    /// `CategoryDragFormat` / `OnCategoryDroppedOnCategoryAsync`). Bewusst
    /// getrennt vom Prompt-Typ, damit ein Prompt-Drag weiterhin "Kategorie
    /// wechseln" bedeutet und nicht versehentlich sortiert.
    static let categoryPasteboardType = NSPasteboard.PasteboardType("com.tvo.CategoryId")

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        registerForDraggedTypes([PBPromptRowView.pasteboardType, Self.categoryPasteboardType])
    }

    convenience init(title: String, target: AnyObject?, action: Selector) {
        self.init(frame: .zero)
        self.title = title
        self.target = target
        self.action = action
    }

    required init?(coder: NSCoder) { fatalError() }

    // ── Drag-Quelle: Tab auf Tab ziehen (Reihenfolge tauschen) ──

    func draggingSession(_ session: NSDraggingSession,
                         sourceOperationMaskFor context: NSDraggingContext) -> NSDragOperation {
        return .move
    }

    /// Eigene Maus-Verfolgung, weil `NSButton.mouseDown` einen eigenen
    /// Tracking-Loop faehrt — `mouseDragged` kommt darin nie an. Unter der
    /// 4-px-Schwelle bleibt es ein normaler Klick (Kategorie an/aus), darueber
    /// startet der Drag. `super.mouseDown` wird bewusst nicht aufgerufen: es
    /// wuerde auf ein MouseUp warten, das wir schon verbraucht haben.
    override func mouseDown(with event: NSEvent) {
        guard categoryId != nil, let win = window else {
            super.mouseDown(with: event)
            return
        }
        let start = event.locationInWindow
        while let next = win.nextEvent(matching: [.leftMouseDragged, .leftMouseUp]) {
            if next.type == .leftMouseUp {
                if bounds.contains(convert(next.locationInWindow, from: nil)) {
                    sendAction(action, to: target)
                }
                return
            }
            if abs(next.locationInWindow.x - start.x) >= 4 || abs(next.locationInWindow.y - start.y) >= 4 {
                beginCategoryDrag(with: next)
                return
            }
        }
    }

    private func beginCategoryDrag(with event: NSEvent) {
        guard let categoryId = categoryId else { return }
        let pbItem = NSPasteboardItem()
        pbItem.setString(categoryId.uuidString, forType: Self.categoryPasteboardType)
        let item = NSDraggingItem(pasteboardWriter: pbItem)
        if let bmp = bitmapImageRepForCachingDisplay(in: bounds) {
            cacheDisplay(in: bounds, to: bmp)
            let img = NSImage(size: bounds.size)
            img.addRepresentation(bmp)
            item.setDraggingFrame(bounds, contents: img)
        } else {
            item.draggingFrame = bounds
        }
        beginDraggingSession(with: [item], event: event, source: self)
    }

    // ── Drop-Ziel: Prompt (Kategorie wechseln) ODER Kategorie (tauschen) ──

    /// Name bewusst NICHT `highlight` — das kollidiert mit NSButton.highlight(_:).
    private func setDropHighlight(_ on: Bool) {
        if on {
            if savedBackground == nil { savedBackground = layer?.backgroundColor }
            if let bg = savedBackground, let nsBg = NSColor(cgColor: bg) {
                let brighter = nsBg.blended(withFraction: 0.25, of: .white) ?? nsBg
                layer?.backgroundColor = brighter.cgColor
            }
        } else {
            if let saved = savedBackground { layer?.backgroundColor = saved }
            savedBackground = nil
        }
    }

    override func draggingEntered(_ sender: NSDraggingInfo) -> NSDragOperation {
        let types = sender.draggingPasteboard.types ?? []
        if types.contains(Self.categoryPasteboardType) {
            // Ein Tab auf sich selbst zu ziehen ergibt nichts.
            if let str = sender.draggingPasteboard.string(forType: Self.categoryPasteboardType),
               UUID(uuidString: str) == categoryId { return [] }
            setDropHighlight(true)
            return .move
        }
        guard types.contains(PBPromptRowView.pasteboardType) else { return [] }
        setDropHighlight(true)
        return .move
    }

    override func draggingExited(_ sender: NSDraggingInfo?) {
        setDropHighlight(false)
    }

    override func performDragOperation(_ sender: NSDraggingInfo) -> Bool {
        defer { setDropHighlight(false) }
        guard let categoryId = self.categoryId, let owner = self.owner else { return false }

        // Kategorie-Drag zuerst pruefen: er ist der spezifischere Fall.
        if let str = sender.draggingPasteboard.string(forType: Self.categoryPasteboardType),
           let sourceId = UUID(uuidString: str) {
            owner.handleCategoryReorder(sourceId: sourceId, targetId: categoryId)
            return true
        }
        guard let str = sender.draggingPasteboard.string(forType: PBPromptRowView.pasteboardType),
              let promptId = UUID(uuidString: str) else { return false }
        owner.handlePromptDrop(promptId: promptId, onCategory: categoryId)
        return true
    }
}

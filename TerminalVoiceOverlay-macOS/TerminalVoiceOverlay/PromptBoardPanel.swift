import AppKit

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
        let contentRect = NSRect(x: 0, y: 0, width: 380, height: 480)
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
    }

    deinit {
        if let m = rightClickMonitor { NSEvent.removeMonitor(m) }
        if let m = globalRightDragMonitor { NSEvent.removeMonitor(m) }
        autoBackupTimer?.invalidate()
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
    /// header badge next to the title.
    private func recordSuccessfulSync() {
        UserDefaults.standard.set(Date(), forKey: Self.lastSyncKey)
        refreshSyncLabel()
    }

    /// Reads the persisted last-sync timestamp and renders it as a short
    /// muted badge like "· sync 24.04. 22:39". Always shows date + time so
    /// you can tell at a glance how fresh the last sync is, even right
    /// after restarting the app. Empty when no sync has happened yet.
    private func refreshSyncLabel() {
        guard let last = UserDefaults.standard.object(forKey: Self.lastSyncKey) as? Date else {
            syncLabel.stringValue = ""
            return
        }
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "de_DE")
        fmt.dateFormat = "dd.MM. HH:mm"
        syncLabel.stringValue = "· sync \(fmt.string(from: last))"
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

        let historyBtn = makeIconButton(symbol: "📜", tooltip: "Prompt-Historie", action: #selector(onToggleHistory))
        let inputBtn = makeIconButton(symbol: "☆", tooltip: "Prompt-Eingabe einblenden", action: #selector(onToggleInput))
        let addCatBtn = makeIconButton(symbol: "+", tooltip: "Neue Kategorie", action: #selector(onAddCategory))
        let backupBtn = makeIconButton(symbol: "⇪", tooltip: "Backup / Wiederherstellen", action: #selector(onBackup))
        let settingsBtn = makeIconButton(symbol: "⚙︎", tooltip: "Einstellungen", action: #selector(onSettings), fontSize: 18)
        self.historyToggleButton = historyBtn
        self.inputToggleButton = inputBtn

        // Reihenfolge: [Historie] [Stern] [+] [Backup] [Settings]
        let header = NSStackView(views: [titleLabel, syncLabel, NSView(), historyBtn, inputBtn, addCatBtn, backupBtn, settingsBtn])
        header.orientation = .horizontal
        header.alignment = .centerY
        header.spacing = 6
        header.distribution = .fill
        // Index 2 is the stretchable spacer NSView() between the sync label
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
        // Dock to the LEFT of the VTO pillar so the panel stays on-screen
        // when the user pins the pillar to the right edge. Match the
        // pillar's HEIGHT exactly so the two floating windows visually
        // line up — the user asked for a uniform vertical extent so the
        // panel doesn't look stubby next to the bar (or vice-versa).
        // Width stays at our own value (380) — the pillar is much
        // narrower so we don't want to inherit that.
        let pillarFrame = vto.frame
        let newSize = NSSize(width: frame.size.width, height: pillarFrame.size.height)
        let newOrigin = NSPoint(x: pillarFrame.origin.x - newSize.width - 4,
                                y: pillarFrame.origin.y)
        setFrame(NSRect(origin: newOrigin, size: newSize), display: true)
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

        let sorted = combined.sorted(by: { lhs, rhs in
            if lhs.0.sortOrder != rhs.0.sortOrder { return lhs.0.sortOrder < rhs.0.sortOrder }
            return lhs.0.shortLabel.localizedCaseInsensitiveCompare(rhs.0.shortLabel) == .orderedAscending
        })
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
        let insertLabel = NSTextField(labelWithString: titleText)
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

        // Layout: [checkbox][insertLabel (huggable)][stretchable spacer][timestamp][edit][delete]
        let rowStack = NSStackView(views: [alwaysOnToggle, insertLabel, NSView(), timestampLabel, editBtn, deleteBtn])
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

    private func openInputPanel() {
        if inputPanel == nil {
            let panel = PromptInputPanel()
            panel.onSubmit = { [weak self] text in
                guard let self = self else { return }
                // Pre/Mitte/Post-Bauen passiert weiter oben — hier nur den
                // reinen Inhalt nach aussen reichen.
                self.onInputSubmit?(text)
                // Eingabe nach Senden leeren, Fokus bleibt drin.
                self.inputPanel?.clearInput()
            }
            inputPanel = panel
        }
        inputPanel?.dock(leftOf: self, force: true)
        inputPanel?.orderFront(nil)
        inputPanel?.makeKeyAndOrderFront(nil)
        inputPanelVisible = true
        updateStarVisual()
    }

    private func closeInputPanel() {
        inputPanel?.close()
        inputPanel = nil
        inputPanelVisible = false
        updateStarVisual()
    }

    private func updateStarVisual() {
        guard let btn = inputToggleButton else { return }
        let symbol = inputPanelVisible ? "★" : "☆"
        let color: NSColor = inputPanelVisible
            ? NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
            : NSColor(calibratedWhite: 0.50, alpha: 1)
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

    // MARK: - Historie-Fenster (Linksklick auf Eintrag → Text in Eingabe)

    @objc private func onToggleHistory() {
        if historyPanelVisible {
            closeHistoryPanel()
        } else {
            openHistoryPanel()
        }
    }

    private func openHistoryPanel() {
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
            historyPanel = p
        }
        // Andocken links am Promtboard — gleiche Position wie das
        // Eingabefenster. Wenn beide gleichzeitig offen sind, liegt die
        // Historie ueber dem Eingabefenster (Z-Order); der Benutzer kann
        // sie via Rechtsklick frei verschieben wenn er beide gleichzeitig
        // sehen will.
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
                                                  prePrompt: true, postPrompt: false) else { return }
        let now = Date()
        let prompt = PBPrompt(id: UUID(), categoryId: catId,
                              shortLabel: result.shortLabel,
                              originalText: result.originalText,
                              improvedText: nil, activeVersion: 0,
                              isAlwaysOn: result.isAlwaysOn,
                              isPrePrompt: result.isPrePrompt,
                              isPostPrompt: result.isPostPrompt,
                              sortOrder: 0, promptKind: "Prompt",
                              geminiModel: nil, isActiveForImprovement: false,
                              improvedByAiPromptId: nil,
                              createdAt: now, updatedAt: now)
        do {
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
    @objc private func onRowClick(_ sender: NSClickGestureRecognizer) {
        tvoDebug("[PBPanel] onRowClick id=\(sender.view?.identifier?.rawValue ?? "nil") panelLevel=\(self.level.rawValue)")
        insertPromptByIdString(sender.view?.identifier?.rawValue)
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
                                                  postPrompt: prompt.isPostPrompt) else { return }
        prompt.shortLabel = result.shortLabel
        prompt.originalText = result.originalText
        prompt.isAlwaysOn = result.isAlwaysOn
        prompt.isPrePrompt = result.isPrePrompt
        prompt.isPostPrompt = result.isPostPrompt
        do {
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

    private func buildBackupJson() throws -> String {
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
                ]
                if let it = p.improvedText { dict["ImprovedText"] = it }
                return dict
            },
        ]
        let data = try JSONSerialization.data(withJSONObject: payload, options: [.prettyPrinted])
        return String(data: data, encoding: .utf8) ?? ""
    }

    private func applyBackupJson(_ json: String) throws {
        try Self.applyBackupJson(json)
    }

    /// Applies a backup JSON as the authoritative state of the local store.
    /// Upserts everything the backup contains AND deletes any local prompt
    /// or category whose id is NOT in the backup — otherwise a prompt
    /// deleted on another machine would re-appear locally on restore.
    /// Static so it can run at app launch before the PromptBoard panel
    /// is lazily created.
    static func applyBackupJson(_ json: String) throws {
        guard let data = json.data(using: .utf8),
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw NSError(domain: "PromptBoardPanel", code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Ungueltiges Backup."])
        }
        let cats = root["Categories"] as? [[String: Any]] ?? []
        let prompts = root["Prompts"] as? [[String: Any]] ?? []

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
fileprivate final class PBCategoryTabButton: NSButton {
    var categoryId: UUID?
    weak var owner: PromptBoardPanel?
    private var savedBackground: CGColor?

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        registerForDraggedTypes([PBPromptRowView.pasteboardType])
    }

    convenience init(title: String, target: AnyObject?, action: Selector) {
        self.init(frame: .zero)
        self.title = title
        self.target = target
        self.action = action
    }

    required init?(coder: NSCoder) { fatalError() }

    override func draggingEntered(_ sender: NSDraggingInfo) -> NSDragOperation {
        guard sender.draggingPasteboard.types?.contains(PBPromptRowView.pasteboardType) == true
        else { return [] }
        // Brighten the tab to signal it's a valid drop target.
        if savedBackground == nil { savedBackground = layer?.backgroundColor }
        if let bg = savedBackground, let nsBg = NSColor(cgColor: bg) {
            let brighter = nsBg.blended(withFraction: 0.25, of: .white) ?? nsBg
            layer?.backgroundColor = brighter.cgColor
        }
        return .move
    }

    override func draggingExited(_ sender: NSDraggingInfo?) {
        if let saved = savedBackground { layer?.backgroundColor = saved }
        savedBackground = nil
    }

    override func performDragOperation(_ sender: NSDraggingInfo) -> Bool {
        defer {
            if let saved = savedBackground { layer?.backgroundColor = saved }
            savedBackground = nil
        }
        guard let str = sender.draggingPasteboard.string(forType: PBPromptRowView.pasteboardType),
              let promptId = UUID(uuidString: str),
              let categoryId = self.categoryId,
              let owner = self.owner else { return false }
        owner.handlePromptDrop(promptId: promptId, onCategory: categoryId)
        return true
    }
}

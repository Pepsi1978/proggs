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

    private let categoryStack = NSStackView()
    private let promptStack = NSStackView()
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
        let contentRect = NSRect(x: 0, y: 0, width: 380, height: 528)
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
    /// muted badge like "· sync 22:39" (same day) or "· sync 24.04. 22:39"
    /// (older day). Empty string when no sync has happened yet.
    private func refreshSyncLabel() {
        guard let last = UserDefaults.standard.object(forKey: Self.lastSyncKey) as? Date else {
            syncLabel.stringValue = ""
            return
        }
        let cal = Calendar.current
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "de_DE")
        if cal.isDateInToday(last) {
            fmt.dateFormat = "HH:mm"
            syncLabel.stringValue = "· sync \(fmt.string(from: last))"
        } else {
            fmt.dateFormat = "dd.MM. HH:mm"
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
        rightClickMonitor = NSEvent.addLocalMonitorForEvents(matching: [.rightMouseDown]) { [weak self] event in
            guard let self = self else { return event }
            // Only handle events meant for this panel.
            guard event.window === self else { return event }
            // Translate click point into each known row's coordinate space.
            // The event window-coord → root view → row works reliably even
            // through nested NSStackView / NSScrollView containers.
            let windowPoint = event.locationInWindow
            for (id, row) in self.rowViewsByPromptId {
                // Skip detached rows (can happen during a re-render that
                // crosses an event delivery).
                guard row.window === self else { continue }
                let rowPoint = row.convert(windowPoint, from: nil)
                if row.bounds.contains(rowPoint) {
                    // Don't fire when the right-click is on one of the
                    // interactive subviews (checkbox / ✎ / ✕) — pick any
                    // descendant button at that spot; if one is hit, we
                    // bail and let its own handling (or nothing) run.
                    if let hit = row.hitTest(row.superview?.convert(windowPoint, from: nil) ?? .zero),
                       hit is NSButton {
                        return event
                    }
                    tvoDebug("[PBPanel] rightClickMonitor → open editor id=\(id.uuidString)")
                    DispatchQueue.main.async { [weak self] in
                        self?.openEditorForPrompt(id: id)
                    }
                    return nil  // consume the event — no menu, no beep
                }
            }
            return event
        }
    }

    private func buildUI() {
        let root = NSView(frame: contentView!.bounds)
        root.autoresizingMask = [.width, .height]
        root.wantsLayer = true
        root.layer?.backgroundColor = NSColor(calibratedWhite: 0.11, alpha: 0.97).cgColor
        root.layer?.cornerRadius = 16
        root.layer?.borderColor = NSColor(calibratedWhite: 0.28, alpha: 1).cgColor
        root.layer?.borderWidth = 1
        contentView?.addSubview(root)

        titleLabel.textColor = NSColor(calibratedWhite: 0.8, alpha: 1)
        titleLabel.font = NSFont.boldSystemFont(ofSize: 13)

        syncLabel.textColor = NSColor(calibratedWhite: 0.50, alpha: 1)
        syncLabel.font = NSFont.systemFont(ofSize: 10)
        refreshSyncLabel()   // show the last known date from UserDefaults

        let addCatBtn = makeIconButton(symbol: "+", tooltip: "Neue Kategorie", action: #selector(onAddCategory))
        let backupBtn = makeIconButton(symbol: "⇪", tooltip: "Backup / Wiederherstellen", action: #selector(onBackup))
        let settingsBtn = makeIconButton(symbol: "⚙︎", tooltip: "Einstellungen", action: #selector(onSettings), fontSize: 18)

        let header = NSStackView(views: [titleLabel, syncLabel, NSView(), addCatBtn, backupBtn, settingsBtn])
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
        // when the user pins the pillar to the right edge.
        let origin = vto.frame.origin
        let panelSize = frame.size
        let newOrigin = NSPoint(x: origin.x - panelSize.width - 4, y: origin.y)
        setFrame(NSRect(origin: newOrigin, size: panelSize), display: true)
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

            let btn = NSButton(title: "", target: self, action: #selector(onSelectCategory(_:)))
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
        let row = NSView()
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

        let insertBtn = NSButton(title: prompt.shortLabel, target: self, action: #selector(onInsertPrompt(_:)))
        insertBtn.bezelStyle = .recessed
        insertBtn.setButtonType(.momentaryLight)
        insertBtn.isBordered = false
        insertBtn.contentTintColor = .white
        insertBtn.alignment = .left
        insertBtn.toolTip = prompt.effectiveText.prefix(500).description
        insertBtn.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)

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

        let rowStack = NSStackView(views: [alwaysOnToggle, insertBtn, NSView(), editBtn, deleteBtn])
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
                                                  label: "", text: "", alwaysOn: false) else { return }
        let now = Date()
        let prompt = PBPrompt(id: UUID(), categoryId: catId,
                              shortLabel: result.shortLabel,
                              originalText: result.originalText,
                              improvedText: nil, activeVersion: 0,
                              isAlwaysOn: result.isAlwaysOn,
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
                                                  alwaysOn: prompt.isAlwaysOn) else { return }
        prompt.shortLabel = result.shortLabel
        prompt.originalText = result.originalText
        prompt.isAlwaysOn = result.isAlwaysOn
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
        guard let data = json.data(using: .utf8),
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw NSError(domain: "PromptBoardPanel", code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Ungueltiges Backup."])
        }
        let cats = root["Categories"] as? [[String: Any]] ?? []
        let prompts = root["Prompts"] as? [[String: Any]] ?? []

        for c in cats {
            guard let idStr = c["Id"] as? String, let id = UUID(uuidString: idStr) else { continue }
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
            let now = Date()
            let prompt = PBPrompt(
                id: id, categoryId: catId,
                shortLabel: (p["ShortLabel"] as? String) ?? "",
                originalText: (p["OriginalText"] as? String) ?? "",
                improvedText: p["ImprovedText"] as? String,
                activeVersion: (p["ActiveVersion"] as? Int) ?? 0,
                isAlwaysOn: (p["IsAlwaysOn"] as? Bool) ?? false,
                sortOrder: (p["SortOrder"] as? Int) ?? 0,
                promptKind: (p["PromptKind"] as? String) ?? "Prompt",
                geminiModel: p["GeminiModel"] as? String,
                isActiveForImprovement: (p["IsActiveForImprovement"] as? Bool) ?? false,
                improvedByAiPromptId: (p["ImprovedByAiPromptId"] as? String).flatMap(UUID.init(uuidString:)),
                createdAt: now, updatedAt: now)
            try PromptBoardStore.shared.upsertPrompt(prompt)
        }
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
}

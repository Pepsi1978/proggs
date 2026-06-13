import AppKit

/// Frei schwebendes Historie-Fenster — gleiche Breite wie das
/// PromptInputPanel (760 pt). Listet alle aktiven Eintraege aus dem
/// PromptHistoryStore, neueste oben. Linksklick auf einen Eintrag reicht
/// den Text per `onEntrySelected` nach aussen, damit das PromptBoardPanel
/// ihn ins Eingabefenster einfuegen kann (oeffnet das Eingabefenster
/// falls noetig). Rechtsklick auf den Hintergrund verschiebt das Fenster
/// — kein Andocken, der Benutzer waehlt seine Position selbst.
final class PromptHistoryPanel: NSPanel {

    /// Wird ausgeloest wenn der Benutzer einen Eintrag anklickt. Der
    /// Text ist der gespeicherte Mittel-Anteil — Pre/Post werden beim
    /// spaeteren Submit aus dem Eingabefenster wieder angebaut, also
    /// genau wie wenn der Eintrag frisch getippt waere.
    var onEntrySelected: ((String) -> Void)?

    /// Wird beim Rechtsklick auf einen Eintrag ausgeloest. Das
    /// PromptBoardPanel oeffnet daraufhin den Editor-Sheet, persistiert
    /// die Aenderung via PromptHistoryStore.updateText und stoesst den
    /// Cloud-Sync an. Rechtsklick auf den Hintergrund verschiebt
    /// stattdessen die ganze Fenster-Gruppe (siehe onGroupDragDelta).
    var onEntryEditRequested: ((PBHistoryEntry) -> Void)?

    /// Wird beim Rechtsklick-Drag fuer jeden Mausschritt ausgeloest. Das
    /// Promptboard verschiebt daraufhin die GANZE Gruppe um den gleichen
    /// Versatz. Wir bewegen uns NIE selbst — die Andockung bleibt starr.
    var onGroupDragDelta: ((CGFloat, CGFloat) -> Void)?

    private let titleLabel = NSTextField(labelWithString: "Prompt-Historie")
    private let hintLabel = NSTextField(labelWithString:
        "Klick = in Eingabe einfuegen · Rechtsklick = Fenster verschieben")
    private let countLabel = NSTextField(labelWithString: "")
    private let stack = NSStackView()
    private let scrollView = NSScrollView()

    // ── Rechtsklick-Drag-State ──
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var rightDragMonitor: Any?

    private let displayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "dd.MM.yyyy · HH:mm"
        return f
    }()

    init() {
        let contentRect = NSRect(x: 0, y: 0, width: 760, height: 480)
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
        installRightClickDragMonitor()
        // Initiale Position legt der Caller via dock(leftOf:) fest. Wenn
        // er das nicht tut, bleibt das Panel an seiner Default-(0,0)-
        // Position bis zur ersten Andockung.
    }

    // MARK: - Andocken am Promptboard

    /// Dockt das Fenster an die linke Seite des uebergebenen Promptboards
    /// an — Hoehe wird angeglichen, x-Position 4 pt links neben dem Board.
    /// Andockung ist absolut: kein "manuell positioniert"-Konzept mehr.
    func dock(leftOf board: NSWindow, force: Bool = false) {
        var frame = self.frame
        frame.size.height = board.frame.height
        frame.origin.x = board.frame.origin.x - frame.size.width - 4
        frame.origin.y = board.frame.origin.y
        setFrame(frame, display: true)
        clampToScreen()
    }

    /// Folgt einer Drag-Bewegung des Promptboards. Im Gegensatz zu `dock`
    /// ueberschreibt das auch eine manuelle Position — wenn das Promptboard
    /// sich bewegt oder seine Hoehe aendert, soll das angedockte Fenster
    /// 1:1 mitwandern. Hoehe wird IMMER nachgezogen damit das Paar nicht
    /// vertikal auseinanderlaeuft. Kein clampToScreen hier — beim Drag
    /// soll die Andockung 1:1 bleiben, nicht von Bildschirm-Klamp gestoert.
    func followBoardDrag(_ board: NSWindow) {
        var frame = self.frame
        frame.size.height = board.frame.height
        frame.origin.x = board.frame.origin.x - frame.size.width - 4
        frame.origin.y = board.frame.origin.y
        setFrame(frame, display: true)
    }

    private func clampToScreen() {
        guard let screen = NSScreen.main else { return }
        let v = screen.visibleFrame
        var f = self.frame
        if f.origin.x < v.origin.x { f.origin.x = v.origin.x }
        if f.origin.y < v.origin.y { f.origin.y = v.origin.y }
        if f.maxX > v.maxX { f.origin.x = v.maxX - f.size.width }
        if f.maxY > v.maxY { f.origin.y = v.maxY - f.size.height }
        if f != self.frame { setFrame(f, display: true) }
    }

    deinit {
        if let m = rightDragMonitor { NSEvent.removeMonitor(m) }
    }

    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { false }

    // MARK: - Public

    /// Setzt die Liste der angezeigten Eintraege. Wird aus dem Owner-Code
    /// (PromptBoardPanel) aufgerufen — der Owner laedt vorher die Daten
    /// via PromptHistoryStore.shared.load, damit das Fenster keine eigenen
    /// Service-Abhaengigkeiten aufbauen muss.
    func render(_ entries: [PBHistoryEntry]) {
        // Alle alten Reihen entfernen.
        for v in stack.arrangedSubviews { stack.removeArrangedSubview(v); v.removeFromSuperview() }

        if entries.isEmpty {
            let empty = NSTextField(labelWithString:
                "Noch keine Historie. Sende einen Prompt — er erscheint hier.")
            empty.textColor = NSColor(calibratedWhite: 0.60, alpha: 1)
            empty.font = NSFont.systemFont(ofSize: 12)
            stack.addArrangedSubview(empty)
            countLabel.stringValue = "0"
            return
        }

        for entry in entries {
            stack.addArrangedSubview(buildRow(entry))
        }
        countLabel.stringValue = "\(entries.count)"
    }

    // MARK: - UI

    private func buildUI() {
        let root = NSView(frame: contentView!.bounds)
        root.autoresizingMask = [.width, .height]
        root.wantsLayer = true
        root.layer?.backgroundColor = NSColor(calibratedWhite: 0.11, alpha: 0.78).cgColor
        root.layer?.cornerRadius = 16
        root.layer?.borderColor = NSColor(calibratedWhite: 0.28, alpha: 1).cgColor
        root.layer?.borderWidth = 1
        contentView?.addSubview(root)

        titleLabel.textColor = NSColor(calibratedWhite: 0.8, alpha: 1)
        titleLabel.font = NSFont.boldSystemFont(ofSize: 13)

        hintLabel.textColor = NSColor(calibratedWhite: 0.50, alpha: 1)
        hintLabel.font = NSFont.systemFont(ofSize: 10)
        hintLabel.lineBreakMode = .byTruncatingTail

        countLabel.textColor = NSColor(calibratedWhite: 0.55, alpha: 1)
        countLabel.font = NSFont.systemFont(ofSize: 10)
        countLabel.alignment = .right

        let header = NSStackView(views: [titleLabel, hintLabel, NSView(), countLabel])
        header.orientation = .horizontal
        header.alignment = .centerY
        header.spacing = 10
        header.distribution = .fill
        header.translatesAutoresizingMaskIntoConstraints = false

        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 6
        stack.edgeInsets = NSEdgeInsets(top: 6, left: 6, bottom: 6, right: 6)
        stack.translatesAutoresizingMaskIntoConstraints = false

        scrollView.hasVerticalScroller = true
        scrollView.scrollerStyle = .overlay
        scrollView.autohidesScrollers = true
        scrollView.drawsBackground = false
        scrollView.borderType = .noBorder
        scrollView.documentView = stack
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.wantsLayer = true
        // Kein eigener Background — Panel-Huelle (alpha 0.78) scheint durch,
        // damit die Historie genauso transparent wirkt wie Promtboard und
        // Eingabefenster. Nur der schmale Border bleibt zur visuellen
        // Abgrenzung der Liste.
        scrollView.layer?.backgroundColor = NSColor.clear.cgColor
        scrollView.layer?.cornerRadius = 8
        scrollView.layer?.borderColor = NSColor(calibratedWhite: 0.23, alpha: 1).cgColor
        scrollView.layer?.borderWidth = 1

        let outer = NSStackView(views: [header, scrollView])
        outer.orientation = .vertical
        outer.alignment = .leading
        outer.spacing = 8
        outer.translatesAutoresizingMaskIntoConstraints = false
        root.addSubview(outer)

        NSLayoutConstraint.activate([
            outer.leadingAnchor.constraint(equalTo: root.leadingAnchor, constant: 10),
            outer.trailingAnchor.constraint(equalTo: root.trailingAnchor, constant: -10),
            outer.topAnchor.constraint(equalTo: root.topAnchor, constant: 10),
            outer.bottomAnchor.constraint(equalTo: root.bottomAnchor, constant: -10),
            header.widthAnchor.constraint(equalTo: outer.widthAnchor),
            scrollView.widthAnchor.constraint(equalTo: outer.widthAnchor),
            scrollView.heightAnchor.constraint(greaterThanOrEqualToConstant: 360),
            stack.widthAnchor.constraint(equalTo: scrollView.widthAnchor, constant: -12),
        ])
    }

    private func buildRow(_ entry: PBHistoryEntry) -> NSView {
        // Hintergrund-Container (NSButton-like aber komplett selbstgebaut,
        // damit Hover-Highlight + Linksklick als simple Aktion funktioniert).
        let container = ClickableHistoryRow(entry: entry)
        container.translatesAutoresizingMaskIntoConstraints = false
        container.wantsLayer = true
        container.layer?.cornerRadius = 6
        container.layer?.backgroundColor = NSColor(calibratedWhite: 0.14, alpha: 1).cgColor
        container.onClick = { [weak self] text in
            self?.onEntrySelected?(text)
        }
        container.onEditRequested = { [weak self] e in
            self?.onEntryEditRequested?(e)
        }

        let title = NSTextField(labelWithString:
            entry.title.isEmpty ? "Ohne Titel" : entry.title)
        title.textColor = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
        title.font = NSFont.boldSystemFont(ofSize: 13)
        title.lineBreakMode = .byTruncatingTail

        let meta = NSTextField(labelWithString:
            displayFormatter.string(from: entry.timestamp))
        meta.textColor = NSColor(calibratedWhite: 0.60, alpha: 1)
        meta.font = NSFont.systemFont(ofSize: 10)

        // Vorschau: bis zu ~280 Zeichen ohne Zeilenumbrueche, damit beide
        // sichtbaren Zeilen mit Inhalt gefuellt werden (1:1 mit Windows).
        var preview = entry.text
            .replacingOccurrences(of: "\n", with: " ")
            .replacingOccurrences(of: "\r", with: " ")
            .replacingOccurrences(of: "\t", with: " ")
        if preview.count > 280 {
            let cutIdx = preview.index(preview.startIndex, offsetBy: 280)
            preview = String(preview[..<cutIdx]) + "…"
        }
        let prev = NSTextField(labelWithString: preview)
        prev.textColor = NSColor(calibratedWhite: 0.80, alpha: 1)
        prev.font = NSFont.systemFont(ofSize: 11)
        prev.lineBreakMode = .byTruncatingTail
        prev.maximumNumberOfLines = 2
        prev.cell?.wraps = true
        prev.usesSingleLineMode = false

        let inner = NSStackView(views: [title, meta, prev])
        inner.orientation = .vertical
        inner.alignment = .leading
        inner.spacing = 2
        inner.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(inner)
        NSLayoutConstraint.activate([
            inner.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 10),
            inner.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -10),
            inner.topAnchor.constraint(equalTo: container.topAnchor, constant: 8),
            inner.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -8),
        ])
        container.heightAnchor.constraint(greaterThanOrEqualToConstant: 56).isActive = true
        return container
    }

    // MARK: - Rechtsklick-Drag

    /// Sucht entlang der View-Hierarchie an der Klick-Position nach einer
    /// ClickableHistoryRow. Wir konvertieren die globale Mausposition in
    /// die View-Koordinaten des contentView und nutzen hitTest — so finden
    /// wir die Zeile auch dann wenn das Event auf einem inneren Label
    /// landet.
    fileprivate func findRow(at event: NSEvent) -> ClickableHistoryRow? {
        guard let cv = self.contentView else { return nil }
        let pointInWindow = event.locationInWindow
        let pointInContent = cv.convert(pointInWindow, from: nil)
        var hit: NSView? = cv.hitTest(pointInContent)
        while let v = hit {
            if let row = v as? ClickableHistoryRow { return row }
            hit = v.superview
        }
        return nil
    }

    private func installRightClickDragMonitor() {
        rightDragMonitor = NSEvent.addLocalMonitorForEvents(
            matching: [.rightMouseDown, .rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            guard let self = self, event.window === self else { return event }
            switch event.type {
            case .rightMouseDown:
                // Wenn der Klick eine Historie-Zeile getroffen hat, gehoert er
                // dem Eintrag — Editor-Sheet oeffnen, NICHT die Gruppe ziehen.
                if let row = self.findRow(at: event) {
                    self.onEntryEditRequested?(row.entry)
                    return nil
                }
                self.isDragging = true
                self.dragStartMouseLocation = NSEvent.mouseLocation
                return nil
            case .rightMouseDragged:
                if self.isDragging {
                    // Wir bewegen UNS NICHT direkt — Promptboard
                    // verschiebt die GANZE Gruppe um den gleichen Versatz,
                    // wir werden ueber followBoardDrag zurueckgezogen.
                    let cur = NSEvent.mouseLocation
                    let dx = cur.x - self.dragStartMouseLocation.x
                    let dy = cur.y - self.dragStartMouseLocation.y
                    if dx != 0 || dy != 0 {
                        self.onGroupDragDelta?(dx, dy)
                        self.dragStartMouseLocation = cur
                    }
                    return nil
                }
                return event
            case .rightMouseUp:
                if self.isDragging { self.isDragging = false; return nil }
                return event
            default:
                return event
            }
        }
    }
}

/// Modaler Editor fuer einen Historie-Eintrag. Wird per Rechtsklick auf
/// eine Zeile im PromptHistoryPanel geoeffnet. Der Benutzer kann den
/// gespeicherten Prompt-Text frei aendern und mit Speichern bestaetigen
/// oder mit Abbrechen verwerfen. Titel und Zeitstempel bleiben unveraendert.
final class PromptHistoryEditController: NSWindowController {

    /// Wird mit dem neuen Text aufgerufen wenn der Benutzer Speichern
    /// klickt. Nil-Aufruf bei Abbrechen.
    var onResult: ((String?) -> Void)?

    private let textView = NSTextView()
    private let metaLabel = NSTextField(labelWithString: "")

    init(entry: PBHistoryEntry) {
        let win = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 720, height: 520),
            styleMask: [.titled, .closable, .resizable],
            backing: .buffered, defer: false)
        win.title = "Historie-Eintrag bearbeiten"
        win.isReleasedWhenClosed = false
        super.init(window: win)
        buildUI(in: win, entry: entry)
    }

    required init?(coder: NSCoder) { fatalError() }

    private func buildUI(in win: NSWindow, entry: PBHistoryEntry) {
        let root = NSView(frame: win.contentView!.bounds)
        root.autoresizingMask = [.width, .height]
        win.contentView = root

        let title = NSTextField(labelWithString: "Historie-Eintrag bearbeiten")
        title.font = NSFont.boldSystemFont(ofSize: 14)
        title.textColor = NSColor(calibratedWhite: 0.85, alpha: 1)

        let displayFmt = DateFormatter()
        displayFmt.locale = Locale(identifier: "de_DE")
        displayFmt.dateFormat = "dd.MM.yyyy · HH:mm"
        let safeTitle = entry.title.isEmpty ? "Ohne Titel" : entry.title
        metaLabel.stringValue = "\(safeTitle)  ·  \(displayFmt.string(from: entry.timestamp))"
        metaLabel.font = NSFont.systemFont(ofSize: 11)
        metaLabel.textColor = NSColor(calibratedWhite: 0.55, alpha: 1)
        metaLabel.lineBreakMode = .byTruncatingTail

        let scroll = NSScrollView()
        scroll.hasVerticalScroller = true
        scroll.borderType = .bezelBorder
        scroll.translatesAutoresizingMaskIntoConstraints = false

        textView.string = entry.text
        textView.isRichText = false
        textView.isAutomaticQuoteSubstitutionEnabled = false
        textView.isAutomaticDashSubstitutionEnabled = false
        textView.isAutomaticTextReplacementEnabled = false
        textView.font = NSFont.systemFont(ofSize: 14)
        textView.textColor = NSColor.textColor
        // Goldener Cursor (Windows-XAML CaretBrush=#FFD700) — gleich wie
        // PromptInputPanel.
        textView.insertionPointColor = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
        textView.isVerticallyResizable = true
        textView.isHorizontallyResizable = false
        textView.autoresizingMask = [.width]
        textView.textContainer?.widthTracksTextView = true
        scroll.documentView = textView

        let btnCancel = NSButton(title: "Abbrechen", target: self,
                                  action: #selector(onCancel))
        btnCancel.bezelStyle = .rounded
        btnCancel.keyEquivalent = "\u{1b}" // Esc

        let btnSave = NSButton(title: "Speichern", target: self,
                                action: #selector(onSave))
        btnSave.bezelStyle = .rounded
        btnSave.keyEquivalent = "\r" // Enter (Plain — Multi-Line braucht Cmd+Enter via shortcut)

        let buttonRow = NSStackView(views: [NSView(), btnCancel, btnSave])
        buttonRow.orientation = .horizontal
        buttonRow.alignment = .centerY
        buttonRow.spacing = 8
        buttonRow.translatesAutoresizingMaskIntoConstraints = false

        let outer = NSStackView(views: [title, metaLabel, scroll, buttonRow])
        outer.orientation = .vertical
        outer.alignment = .leading
        outer.spacing = 8
        outer.translatesAutoresizingMaskIntoConstraints = false
        root.addSubview(outer)

        NSLayoutConstraint.activate([
            outer.leadingAnchor.constraint(equalTo: root.leadingAnchor, constant: 16),
            outer.trailingAnchor.constraint(equalTo: root.trailingAnchor, constant: -16),
            outer.topAnchor.constraint(equalTo: root.topAnchor, constant: 16),
            outer.bottomAnchor.constraint(equalTo: root.bottomAnchor, constant: -16),
            scroll.widthAnchor.constraint(equalTo: outer.widthAnchor),
            scroll.heightAnchor.constraint(greaterThanOrEqualToConstant: 360),
            buttonRow.widthAnchor.constraint(equalTo: outer.widthAnchor),
        ])
    }

    @objc private func onCancel() {
        onResult?(nil)
        close()
    }

    @objc private func onSave() {
        onResult?(textView.string)
        close()
    }
}

/// Klickbare Reihe mit Hover-Highlight. Wir bauen das selbst statt NSButton,
/// damit der gesamte Container reagiert und der Hover-Effekt nicht von einem
/// Default-Button-Look ueberschrieben wird. Haelt den ganzen `entry` damit
/// Linksklick den Text liefern UND Rechtsklick den Editor mit Titel +
/// Zeitstempel oeffnen kann.
final class ClickableHistoryRow: NSView {
    var onClick: ((String) -> Void)?
    var onEditRequested: ((PBHistoryEntry) -> Void)?
    let entry: PBHistoryEntry
    private var trackingArea: NSTrackingArea?

    init(entry: PBHistoryEntry) {
        self.entry = entry
        super.init(frame: .zero)
    }

    required init?(coder: NSCoder) { fatalError() }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let t = trackingArea { removeTrackingArea(t) }
        let area = NSTrackingArea(
            rect: bounds,
            options: [.mouseEnteredAndExited, .activeAlways, .inVisibleRect],
            owner: self, userInfo: nil)
        addTrackingArea(area)
        trackingArea = area
    }

    override func mouseEntered(with event: NSEvent) {
        layer?.backgroundColor = NSColor(calibratedWhite: 0.20, alpha: 1).cgColor
    }

    override func mouseExited(with event: NSEvent) {
        layer?.backgroundColor = NSColor(calibratedWhite: 0.14, alpha: 1).cgColor
    }

    override func mouseUp(with event: NSEvent) {
        // Linksklick: Eintrag waehlen.
        if event.type == .leftMouseUp {
            onClick?(entry.text)
        }
    }
}

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

    private let titleLabel = NSTextField(labelWithString: "Prompt-Historie")
    private let hintLabel = NSTextField(labelWithString:
        "Klick = in Eingabe einfuegen · Rechtsklick = Fenster verschieben")
    private let countLabel = NSTextField(labelWithString: "")
    private let stack = NSStackView()
    private let scrollView = NSScrollView()

    // ── Rechtsklick-Drag-State ──
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var dragStartOrigin: NSPoint = .zero
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

        // Erste Position: Bildschirmmitte — der Benutzer kann via
        // Rechtsklick-Drag selbst woandershin.
        if let screen = NSScreen.main {
            let v = screen.visibleFrame
            var f = self.frame
            f.origin.x = v.origin.x + (v.size.width - f.size.width) / 2
            f.origin.y = v.origin.y + (v.size.height - f.size.height) / 2
            setFrame(f, display: true)
        }
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
        scrollView.layer?.backgroundColor = NSColor(calibratedWhite: 0.10, alpha: 1).cgColor
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
        let container = ClickableHistoryRow(text: entry.text)
        container.translatesAutoresizingMaskIntoConstraints = false
        container.wantsLayer = true
        container.layer?.cornerRadius = 6
        container.layer?.backgroundColor = NSColor(calibratedWhite: 0.14, alpha: 1).cgColor
        container.onClick = { [weak self] text in
            self?.onEntrySelected?(text)
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

        // Vorschau: erste ~140 Zeichen ohne Zeilenumbrueche
        var preview = entry.text
            .replacingOccurrences(of: "\n", with: " ")
            .replacingOccurrences(of: "\r", with: " ")
        if preview.count > 140 {
            let cutIdx = preview.index(preview.startIndex, offsetBy: 140)
            preview = String(preview[..<cutIdx]) + "…"
        }
        let prev = NSTextField(labelWithString: preview)
        prev.textColor = NSColor(calibratedWhite: 0.80, alpha: 1)
        prev.font = NSFont.systemFont(ofSize: 11)
        prev.lineBreakMode = .byTruncatingTail
        prev.maximumNumberOfLines = 2

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

    private func installRightClickDragMonitor() {
        rightDragMonitor = NSEvent.addLocalMonitorForEvents(
            matching: [.rightMouseDown, .rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            guard let self = self, event.window === self else { return event }
            switch event.type {
            case .rightMouseDown:
                self.isDragging = true
                self.dragStartMouseLocation = NSEvent.mouseLocation
                self.dragStartOrigin = self.frame.origin
                return nil
            case .rightMouseDragged:
                if self.isDragging {
                    let cur = NSEvent.mouseLocation
                    var origin = self.dragStartOrigin
                    origin.x += cur.x - self.dragStartMouseLocation.x
                    origin.y += cur.y - self.dragStartMouseLocation.y
                    self.setFrameOrigin(origin)
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

/// Klickbare Reihe mit Hover-Highlight. Wir bauen das selbst statt NSButton,
/// damit der gesamte Container reagiert und der Hover-Effekt nicht von einem
/// Default-Button-Look ueberschrieben wird.
final class ClickableHistoryRow: NSView {
    var onClick: ((String) -> Void)?
    private let text: String
    private var trackingArea: NSTrackingArea?

    init(text: String) {
        self.text = text
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
            onClick?(text)
        }
    }
}

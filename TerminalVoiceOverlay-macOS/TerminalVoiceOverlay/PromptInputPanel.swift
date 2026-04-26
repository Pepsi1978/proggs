import AppKit

/// Tippbares Prompt-Eingabefenster, das links am PromptBoardPanel andockt —
/// 1:1-Pendant zum Windows PromptInputWindow.xaml. Enter sendet (loest
/// `onSubmit` aus), Shift+Enter macht einen Zeilenumbruch. Rechtsklick auf
/// den Hintergrund verschiebt das Fenster frei und setzt das Andock-Tracking
/// aus, damit der Benutzer eine eigene Position waehlen kann.
final class PromptInputPanel: NSPanel {

    /// Wird ausgeloest wenn der Benutzer Enter drueckt (ohne Shift). Der
    /// Text ist der reine Inhalt des Eingabefelds — Pre/Mitte/Post-Bauen
    /// passiert weiter oben (PromptBoardPanel reicht ihn weiter, AppDelegate
    /// baut zusammen wie beim Voice-Diktat).
    var onSubmit: ((String) -> Void)?

    private let textView = SubmitTextView()
    private let scrollView = NSScrollView()
    private let titleLabel = NSTextField(labelWithString: "Prompt-Eingabe")
    private let hintLabel = NSTextField(labelWithString:
        "↩ sendet · ⇧↩ neue Zeile · Rechtsklick zum Verschieben")
    private let previewLabel = NSTextField(labelWithString: "")

    /// Hat der Benutzer das Fenster per Rechtsklick selbst verschoben? Wenn
    /// ja, ueberschreibt `dock(leftOf:)` die Position nicht mehr — nur ein
    /// Drag des Promptboards selbst zieht das Eingabefenster mit.
    private var manuallyPositioned = false

    // ── Rechtsklick-Drag-State ──
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var dragStartOrigin: NSPoint = .zero
    private var rightDragMonitor: Any?

    init() {
        // Doppelte Promptboard-Breite (Promptboard ist 380 pt → wir 760).
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
    }

    deinit {
        if let m = rightDragMonitor { NSEvent.removeMonitor(m) }
    }

    // Damit das Eingabefeld den Fokus bekommen kann (statt das Promptboard).
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { false }

    // MARK: - Public API

    /// Setzt den Inhalt des Eingabefelds und positioniert den Cursor ans
    /// Ende. Wird vom Voice-Overlay genutzt, wenn ein gesprochener Prompt
    /// ins Eingabefenster geroutet wird statt direkt in die CLI.
    func setText(_ text: String) {
        textView.string = text
        let endRange = NSRange(location: (text as NSString).length, length: 0)
        textView.setSelectedRange(endRange)
        makeKeyAndFocusInput()
    }

    /// Fuegt Text an die aktuelle Cursor-Position ein. Wird vom Voice-Overlay
    /// genutzt, wenn das Eingabefeld bereits Inhalt hat und der Benutzer
    /// einen weiteren Voice-Schnipsel anhaengen moechte.
    func appendText(_ text: String) {
        guard !text.isEmpty else { return }
        let sel = textView.selectedRange()
        let nsText = textView.string as NSString
        let merged = nsText.replacingCharacters(in: sel, with: text)
        textView.string = merged
        let newCaret = sel.location + (text as NSString).length
        textView.setSelectedRange(NSRange(location: newCaret, length: 0))
        makeKeyAndFocusInput()
    }

    /// Leert das Eingabefeld und setzt den Fokus zurueck hinein — wird nach
    /// jedem Senden aufgerufen, damit der naechste Prompt direkt getippt
    /// werden kann ohne dass das Fenster sich schliesst.
    func clearInput() {
        textView.string = ""
        makeKeyAndFocusInput()
    }

    /// Aktualisiert die kleine Pre/Post-Vorschau unter dem Eingabefeld.
    func updatePreview(_ preview: String) {
        previewLabel.stringValue = preview
    }

    /// Dockt das Fenster an die linke Seite des uebergebenen Promptboards
    /// an. Respektiert `manuallyPositioned` — wenn der Benutzer selbst
    /// verschoben hat, bleibt diese Position erhalten (es sei denn `force`).
    func dock(leftOf board: NSWindow, force: Bool = false) {
        if manuallyPositioned && !force { return }
        // Hoehe an Promptboard angleichen, damit beide Fenster als Paar
        // wahrgenommen werden.
        var frame = self.frame
        frame.size.height = board.frame.height
        // 4-Punkt-Naht zwischen Eingabefenster und Promptboard.
        frame.origin.x = board.frame.origin.x - frame.size.width - 4
        frame.origin.y = board.frame.origin.y
        setFrame(frame, display: true)
        clampToScreen()
    }

    /// Folgt einer Drag-Bewegung des Promptboards. Im Gegensatz zu `dock`
    /// ueberschreibt das auch eine manuelle Position — wenn das Promptboard
    /// sich bewegt, soll das angedockte Eingabefenster mitwandern.
    func followBoardDrag(_ board: NSWindow) {
        var frame = self.frame
        frame.origin.x = board.frame.origin.x - frame.size.width - 4
        frame.origin.y = board.frame.origin.y
        setFrame(frame, display: true)
        clampToScreen()
    }

    // MARK: - UI

    private func buildUI() {
        let root = NSView(frame: contentView!.bounds)
        root.autoresizingMask = [.width, .height]
        root.wantsLayer = true
        // Gleiche Optik wie PromptBoardPanel: dunkler Hintergrund mit 78%
        // Alpha damit der Terminal-Text durchschimmert.
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

        let header = NSStackView(views: [titleLabel, hintLabel, NSView()])
        header.orientation = .horizontal
        header.alignment = .centerY
        header.spacing = 10
        header.distribution = .fill
        header.translatesAutoresizingMaskIntoConstraints = false

        // Das eigentliche Eingabefeld.
        textView.isRichText = false
        textView.allowsUndo = true
        textView.font = NSFont.systemFont(ofSize: 13)
        textView.textColor = .white
        textView.backgroundColor = NSColor(calibratedWhite: 0.10, alpha: 1)
        textView.insertionPointColor = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
        textView.textContainerInset = NSSize(width: 8, height: 8)
        textView.isAutomaticQuoteSubstitutionEnabled = false
        textView.isAutomaticDashSubstitutionEnabled = false
        textView.isAutomaticTextReplacementEnabled = false
        textView.onSubmit = { [weak self] text in
            self?.onSubmit?(text)
        }

        scrollView.hasVerticalScroller = true
        scrollView.scrollerStyle = .overlay
        scrollView.autohidesScrollers = true
        scrollView.drawsBackground = false
        scrollView.borderType = .noBorder
        scrollView.documentView = textView
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.wantsLayer = true
        scrollView.layer?.backgroundColor = NSColor(calibratedWhite: 0.10, alpha: 1).cgColor
        scrollView.layer?.cornerRadius = 8
        scrollView.layer?.borderColor = NSColor(calibratedWhite: 0.23, alpha: 1).cgColor
        scrollView.layer?.borderWidth = 1

        previewLabel.textColor = NSColor(calibratedWhite: 0.55, alpha: 1)
        previewLabel.font = NSFont.systemFont(ofSize: 10)
        previewLabel.lineBreakMode = .byTruncatingTail
        previewLabel.translatesAutoresizingMaskIntoConstraints = false

        let stack = NSStackView(views: [header, scrollView, previewLabel])
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
            scrollView.widthAnchor.constraint(equalTo: stack.widthAnchor),
            scrollView.heightAnchor.constraint(greaterThanOrEqualToConstant: 240),
            previewLabel.widthAnchor.constraint(equalTo: stack.widthAnchor),
        ])
    }

    private func makeKeyAndFocusInput() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.makeKeyAndOrderFront(nil)
            self.makeFirstResponder(self.textView)
        }
    }

    // MARK: - Rechtsklick-Drag

    /// Ein lokaler Event-Monitor, weil ein normaler `mouseDown`-Override auf
    /// einer `.nonactivatingPanel` mit `canBecomeKey=false` nicht zuverlaessig
    /// feuert — siehe gleiche Loesung im PromptBoardPanel.
    private func installRightClickDragMonitor() {
        rightDragMonitor = NSEvent.addLocalMonitorForEvents(
            matching: [.rightMouseDown, .rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            guard let self = self else { return event }
            // Nur Events fuer dieses Fenster behandeln.
            guard event.window === self else { return event }
            switch event.type {
            case .rightMouseDown:
                // Wenn der Klick auf die TextView trifft, lassen wir das
                // Standard-Kontextmenue zu — kein Drag.
                if let hit = self.contentView?.hitTest(event.locationInWindow),
                   hit is NSTextView || hit.isDescendant(of: self.scrollView) {
                    return event
                }
                self.isDragging = true
                self.manuallyPositioned = true
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
                if self.isDragging {
                    self.isDragging = false
                    self.clampToScreen()
                    return nil
                }
                return event
            default:
                return event
            }
        }
    }

    private func clampToScreen() {
        guard let screen = NSScreen.main else { return }
        let visible = screen.visibleFrame
        var f = self.frame
        if f.origin.x < visible.origin.x { f.origin.x = visible.origin.x }
        if f.origin.y < visible.origin.y { f.origin.y = visible.origin.y }
        if f.maxX > visible.maxX { f.origin.x = visible.maxX - f.size.width }
        if f.maxY > visible.maxY { f.origin.y = visible.maxY - f.size.height }
        if f != self.frame { setFrame(f, display: true) }
    }
}

/// NSTextView-Subklasse, die Enter (ohne Shift) als Submit interpretiert
/// und an einen Callback weiterreicht. Shift+Enter und Option+Enter machen
/// weiterhin einen Zeilenumbruch — wir fangen NUR die nackte Return-Taste ab.
final class SubmitTextView: NSTextView {

    var onSubmit: ((String) -> Void)?

    override func keyDown(with event: NSEvent) {
        let keyCode = event.keyCode
        // 36 = Return, 76 = Numpad Enter
        if keyCode == 36 || keyCode == 76 {
            let mods = event.modifierFlags.intersection(.deviceIndependentFlagsMask)
            if !mods.contains(.shift) && !mods.contains(.option) && !mods.contains(.control) {
                onSubmit?(self.string)
                return
            }
        }
        super.keyDown(with: event)
    }
}

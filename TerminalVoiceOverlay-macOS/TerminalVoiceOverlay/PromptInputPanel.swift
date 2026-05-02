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

    /// Wird ausgeloest wenn der Benutzer den Solo-Dock-Stern in der Toolbar
    /// klickt. `newState=true` heisst: Promptboard ausblenden, Eingabe ans
    /// Pillar andocken. `newState=false` heisst: Promptboard wieder zeigen,
    /// Eingabe rueckt an seinen linken Rand. Wird vom AppDelegate behandelt.
    var onSoloDockToggle: ((Bool) -> Void)?

    /// Wird beim Klick auf den G-Button ausgeloest. AppDelegate ruft Gemini
    /// mit dem aktuellen Text auf und ersetzt ihn durch die Korrektur.
    var onGeminiImprove: ((String, @escaping (String?) -> Void) -> Void)?

    private let textView = SubmitTextView()
    private let scrollView = NSScrollView()
    private let titleLabel = NSTextField(labelWithString: "Prompt-Eingabe")
    private let hintLabel = NSTextField(labelWithString:
        "↩ sendet · ⇧↩ neue Zeile · Rechtsklick zum Verschieben")
    private let previewLabel = NSTextField(labelWithString: "")

    // Toolbar-Buttons (rechts oben in der Header-Zeile)
    private let soloDockButton = NSButton()
    private let separatorButton = NSButton()
    private let geminiButton = NSButton()
    private let clearButton = NSButton()
    private var separatorGlowLayer: CALayer?

    /// True, wenn das PromptBoard ausgeblendet ist und die Eingabe direkt
    /// am Pillar haengt. False, wenn das PromptBoard sichtbar ist und die
    /// Eingabe an dessen linken Rand rueckt.
    private(set) var isSoloDocked: Bool = false

    /// Wird beim Rechtsklick-Drag fuer jeden Mausschritt ausgeloest. Das
    /// Promptboard verschiebt daraufhin die GANZE Gruppe (Pillar +
    /// Promptboard + Eingabe + Historie) um den gleichen Versatz. Wir
    /// bewegen uns NIE selbst — die Andockung bleibt dadurch starr.
    var onGroupDragDelta: ((CGFloat, CGFloat) -> Void)?

    // ── Rechtsklick-Drag-State ──
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
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
    /// an. Andockung ist absolut: kein "manuell positioniert"-Konzept mehr.
    func dock(leftOf board: NSWindow, force: Bool = false) {
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

        // Toolbar-Buttons rechts oben — 1:1 Pendant zur Windows-Toolbar:
        // [Solo-Dock-Stern] [;] [G] [X]
        configureToolbarButton(soloDockButton,
            symbol: "☆", color: .white, fontSize: 14,
            tooltip: "Promptboard ausblenden und Eingabe direkt ans Voice-Overlay andocken (erneuter Klick blendet das Promptboard wieder ein).",
            action: #selector(onSoloDockClick))
        configureToolbarButton(separatorButton,
            symbol: ";", color: NSColor(calibratedRed: 0.50, green: 0.81, blue: 1.0, alpha: 1),
            fontSize: 16, bold: true,
            tooltip: "Aufgaben-Trenner einfuegen — haengt eine Leerzeile, ein Semikolon und noch eine Leerzeile ans Ende des Textes an.",
            action: #selector(onInsertSeparatorClick))
        configureToolbarButton(geminiButton,
            symbol: "G", color: NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1),
            fontSize: 14, bold: true,
            tooltip: "Mit Gemini verbessern — der aktuelle Eingabe-Text wird durch eine bereinigte Variante ersetzt.",
            action: #selector(onGeminiClick))
        configureToolbarButton(clearButton,
            symbol: "X", color: NSColor(calibratedRed: 1.0, green: 0.27, blue: 0.27, alpha: 1),
            fontSize: 14, bold: true,
            tooltip: "Eingabe-Text loeschen",
            action: #selector(onClearClick))

        let toolbar = NSStackView(views: [soloDockButton, separatorButton, geminiButton, clearButton])
        toolbar.orientation = .horizontal
        toolbar.spacing = 4
        toolbar.alignment = .centerY

        // hintLabel: greedy spacer in der Mitte, damit toolbar rechts klebt.
        hintLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        let header = NSStackView(views: [titleLabel, hintLabel, toolbar])
        header.orientation = .horizontal
        header.alignment = .centerY
        header.spacing = 10
        header.distribution = .fill
        header.translatesAutoresizingMaskIntoConstraints = false

        // Das eigentliche Eingabefeld. Hintergrund ist transparent — die
        // aeussere Panel-Hülle (calibratedWhite 0.11, alpha 0.78) scheint
        // durch, so dass der Eingabebereich exakt so transparent wirkt wie
        // das Promtboard und die Historie.
        textView.isRichText = false
        textView.allowsUndo = true
        // Doppelte Standard-Eingabe-Schriftgroesse (13 → 26) — der
        // Eingabe-Text soll deutlich groesser sein als die UI-Labels rundherum.
        textView.font = NSFont.systemFont(ofSize: 26)
        textView.textColor = .white
        textView.backgroundColor = .clear
        textView.drawsBackground = false
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
        // Kein eigener Background — Panel-Huelle scheint durch.
        scrollView.layer?.backgroundColor = NSColor.clear.cgColor
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

    // MARK: - Toolbar Buttons

    /// Style-Helper fuer die 4 Toolbar-Buttons. Kompakte runde Buttons im
    /// Stil der Windows-ToolbarButton-Style — 28x28 mit 14pt CornerRadius,
    /// Hover-Zustand etwas heller. Kein Border, damit die Buttons sauber
    /// nebeneinanderliegen ohne sichtbare Lueckenkanten.
    private func configureToolbarButton(_ btn: NSButton,
                                         symbol: String,
                                         color: NSColor,
                                         fontSize: CGFloat,
                                         bold: Bool = false,
                                         tooltip: String,
                                         action: Selector) {
        btn.title = ""
        btn.bezelStyle = .regularSquare
        btn.isBordered = false
        btn.target = self
        btn.action = action
        btn.toolTip = tooltip
        btn.translatesAutoresizingMaskIntoConstraints = false
        btn.wantsLayer = true
        btn.layer?.backgroundColor = NSColor(calibratedWhite: 0.18, alpha: 1).cgColor
        btn.layer?.cornerRadius = 14

        let label = NSTextField(labelWithString: symbol)
        label.textColor = color
        label.font = bold
            ? NSFont.boldSystemFont(ofSize: fontSize)
            : NSFont.systemFont(ofSize: fontSize, weight: .semibold)
        label.alignment = .center
        label.isBezeled = false
        label.drawsBackground = false
        label.isEditable = false
        label.isSelectable = false
        label.translatesAutoresizingMaskIntoConstraints = false
        btn.addSubview(label)

        NSLayoutConstraint.activate([
            btn.widthAnchor.constraint(equalToConstant: 28),
            btn.heightAnchor.constraint(equalToConstant: 28),
            label.centerXAnchor.constraint(equalTo: btn.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: btn.centerYAnchor),
        ])

        btn.identifier = NSUserInterfaceItemIdentifier(symbol + "-label")
    }

    /// Holt das innere NSTextField (Symbol-Label) aus einem Toolbar-Button —
    /// brauchen wir um Farbe und Symbol des Solo-Dock-Sterns nachzuziehen.
    private func toolbarLabel(of btn: NSButton) -> NSTextField? {
        return btn.subviews.compactMap { $0 as? NSTextField }.first
    }

    @objc private func onSoloDockClick() {
        let newState = !isSoloDocked
        onSoloDockToggle?(newState)
    }

    /// Visual nachziehen — wird vom AppDelegate aufgerufen NACHDEM der
    /// Layout-Wechsel passiert ist. So bleibt das Stern-Icon immer in
    /// Sync mit dem tatsaechlichen Promptboard-Sichtbarkeitszustand.
    func setSoloDockState(_ active: Bool) {
        isSoloDocked = active
        guard let label = toolbarLabel(of: soloDockButton) else { return }
        if active {
            label.stringValue = "★"
            label.textColor = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
            soloDockButton.toolTip = "Promptboard wieder einblenden (zurueck in den Normalmodus)."
        } else {
            label.stringValue = "☆"
            label.textColor = .white
            soloDockButton.toolTip = "Promptboard ausblenden und Eingabe direkt ans Voice-Overlay andocken (erneuter Klick blendet das Promptboard wieder ein)."
        }
    }

    @objc private func onInsertSeparatorClick() {
        // Trenner anhaengen: 2 Newlines + ; + 2 Newlines (Pendant zu Windows
        // " ; " Trenner — hier mit Newline-Wrapping fuer bessere Sichtbarkeit
        // im Multi-Line-Eingabefeld).
        let suffix = "\n\n ; \n\n"
        let nsText = textView.string as NSString
        let merged = nsText.appending(suffix)
        textView.string = merged
        let endRange = NSRange(location: (merged as NSString).length, length: 0)
        textView.setSelectedRange(endRange)
        textView.scrollRangeToVisible(endRange)
        flashSeparatorButton()
        makeKeyAndFocusInput()
    }

    /// Goldener Aufleucht-Effekt fuer ~400ms nach Klick — visuelle Bestaetigung
    /// dass der Trenner gesetzt wurde, ohne dass der Benutzer ans Textende
    /// scrollen muss.
    private func flashSeparatorButton() {
        let glow = CABasicAnimation(keyPath: "backgroundColor")
        glow.fromValue = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1).cgColor
        glow.toValue = NSColor(calibratedWhite: 0.18, alpha: 1).cgColor
        glow.duration = 0.4
        glow.timingFunction = CAMediaTimingFunction(name: .easeOut)
        separatorButton.layer?.add(glow, forKey: "flash")
    }

    @objc private func onGeminiClick() {
        let current = textView.string
        guard !current.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let handler = onGeminiImprove else { return }
        // Visuelles Loading-Feedback: Button kurz dimmen.
        geminiButton.alphaValue = 0.5
        handler(current) { [weak self] improved in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.geminiButton.alphaValue = 1.0
                guard let improved = improved, !improved.isEmpty else { return }
                self.textView.string = improved
                let endRange = NSRange(location: (improved as NSString).length, length: 0)
                self.textView.setSelectedRange(endRange)
                self.makeKeyAndFocusInput()
            }
        }
    }

    @objc private func onClearClick() {
        clearInput()
    }

    // MARK: - Andocken am Pillar (Solo-Dock-Modus)

    /// Dockt die Eingabe direkt LINKS an den Pillar (Voice-Overlay) — ohne
    /// PromptBoard dazwischen. Wird vom AppDelegate aufgerufen wenn der
    /// Solo-Dock-Modus aktiv ist (entweder durch Pillar-Stern-Klick oder
    /// durch Solo-Dock-Stern in der eigenen Toolbar). Hoehe wird an den
    /// Pillar angeglichen damit beide Fenster als Paar wirken.
    func dockToOverlay(_ pillar: NSWindow) {
        var frame = self.frame
        frame.size.height = pillar.frame.height
        frame.origin.x = pillar.frame.origin.x - frame.size.width - 4
        frame.origin.y = pillar.frame.origin.y
        setFrame(frame, display: true)
        clampToScreen()
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
                self.dragStartMouseLocation = NSEvent.mouseLocation
                return nil
            case .rightMouseDragged:
                if self.isDragging {
                    // Wir bewegen UNS NICHT direkt — stattdessen melden wir
                    // den Mouse-Delta an das Promptboard, das daraufhin die
                    // GANZE Gruppe um den gleichen Versatz verschiebt. Wir
                    // selbst werden vom Promptboard ueber followBoardDrag
                    // zurueckgezogen — die Andockung bleibt damit immer
                    // exakt bei 4 px Abstand.
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
                if self.isDragging {
                    self.isDragging = false
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

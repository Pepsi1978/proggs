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
    // Ganz aussen rechts: Text kopieren / Zwischenablage einfuegen.
    private let copyButton = NSButton()
    private let pasteButton = NSButton()
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

    // ── Prompt-Zwischenspeicher-Slots (1…30) ──
    /// Wird ausgeloest wenn der Benutzer auf die Diskette klickt: speichere
    /// `text` dauerhaft im Slot `number` (1…30). Der AppDelegate persistiert
    /// und stoesst SOFORT den Cloud-Sync an.
    var onSlotSave: ((Int, String) -> Void)?
    /// Wird ausgeloest wenn der Benutzer auf das X klickt: loesche Slot
    /// `number` dauerhaft. AppDelegate persistiert + Sofort-Sync.
    var onSlotDelete: ((Int) -> Void)?

    /// Wird beim Speichern eines Slots ausgeloest, um asynchron eine 6-8-Wort-
    /// Zusammenfassung zu holen. Der Handler (AppDelegate) ruft Gemini und gibt
    /// das Ergebnis via Completion zurueck. Best-effort: leerer String -> kein
    /// Tooltip. Spiegelt das Muster von `onGeminiImprove`.
    var onGenerateSlotSummary: ((String, @escaping (String) -> Void) -> Void)?
    /// Eine frische Zusammenfassung steht bereit und soll persistiert werden
    /// (number, fuer-diesen-Text, summary). Der Handler (PromptBoardPanel)
    /// schreibt sie via `PromptSlotStore.setSummary` + Sofort-Sync.
    var onSlotSummary: ((Int, String, String) -> Void)?

    /// Rechtsklick auf einen belegten Slot -> Prioritaet (0=keine, 1=niedrig,
    /// 2=mittel, 3=hoch). Das PromptBoardPanel persistiert via
    /// `PromptSlotStore.setPriority` und stoesst SOFORT den Cloud-/Drive-Sync an.
    var onSlotPriority: ((Int, Int) -> Void)?

    /// Aktuell ausgewaehlte Slot-Nummer (1…30) oder nil. Bestimmt ob
    /// Diskette/X sichtbar sind und welcher Slot gespeichert/geloescht wird.
    private var selectedSlot: Int?
    /// Lokale Kopie der belegten Slots (Nummer → Text). Quelle fuer die
    /// Einfaerbung der Zahlen und fuer das Laden beim Klick. Wird vom
    /// AppDelegate via `setSlotContents` gefuellt (lokaler Stand + Cloud-Merge).
    private var slotContents: [Int: String] = [:]
    /// Speicher-Zeitstempel pro belegtem Slot — fuer die Anzeige neben dem X.
    private var slotTimestamps: [Int: Date] = [:]
    /// KI-Zusammenfassung (6-8 Woerter) pro belegtem Slot — als Hover-Tooltip.
    private var slotSummaries: [Int: String] = [:]
    /// Prioritaet (1=niedrig, 2=mittel, 3=hoch) pro belegtem Slot — Quelle fuer
    /// die farbige Hintergrund-Einfaerbung. Fehlend/0 = keine Prioritaet.
    private var slotPriorities: [Int: Int] = [:]
    /// Slots, fuer die gerade eine Summary erzeugt wird — verhindert Doppel-Calls.
    private var summaryInFlight: Set<Int> = []
    /// True solange der Backfill bestehender Slots laeuft (sequentiell, gedrosselt).
    private var backfillRunning = false
    private var slotButtons: [Int: NSButton] = [:]
    /// Eigenes gestyltes Tooltip-Fenster (weiss, abgerundet, Schatten, im
    /// Vordergrund) — Ersatz fuer den nicht gestaltbaren NSToolTip, 1:1 zur
    /// Windows-Optik (Frank-Wunsch 2026-06-11).
    private lazy var tooltipPanel = SlotTooltipPanel()
    /// Verzoegerungs-Timer fuer die Einblendung (~130ms wie unter Windows).
    private var hoverTimer: Timer?
    /// Aktuell mit der Maus ueberfahrener Slot (oder nil).
    private var hoveredSlot: Int?
    private let slotSaveButton = NSButton()
    private let slotDeleteButton = NSButton()
    /// Zeigt „wann gespeichert" fuer den gewaehlten belegten Slot, rechts neben dem X.
    private let slotTimeLabel = NSTextField(labelWithString: "")

    private static let slotGold = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
    private static let slotGrey = NSColor(calibratedWhite: 0.55, alpha: 1)
    private static let slotRed = NSColor(calibratedRed: 1.0, green: 0.27, blue: 0.27, alpha: 1)
    // Prioritaets-Hintergruende (Rechtsklick -> Prioritaet): Hoch=Rot, Mittel=Gelb,
    // Niedrig=Gruen. Zahl darauf = Near-Black (slotPrioText), auf allen drei lesbar.
    private static let slotPrioHigh = NSColor(calibratedRed: 0.898, green: 0.224, blue: 0.208, alpha: 1)
    private static let slotPrioMedium = NSColor(calibratedRed: 0.984, green: 0.753, blue: 0.176, alpha: 1)
    private static let slotPrioLow = NSColor(calibratedRed: 0.263, green: 0.627, blue: 0.278, alpha: 1)
    private static let slotPrioText = NSColor(calibratedWhite: 0.102, alpha: 1)
    /// Wieviele Slots je Reihe — 15 oben (1-15), 15 unten (16-30).
    private static let slotsPerRow = 15

    private static let slotTimeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "dd.MM. HH:mm"
        return f
    }()

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

    // Wird das Eingabe-Panel ausgeblendet, MUSS ein evtl. offener Slot-Tooltip
    // mit verschwinden — sonst bliebe das kleine Fenster sichtbar haengen.
    override func orderOut(_ sender: Any?) {
        super.orderOut(sender)
        hoverTimer?.invalidate(); hoverTimer = nil
        hoveredSlot = nil
        tooltipPanel.orderOut(nil)
    }

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

    /// Leert das Eingabefeld nach dem Senden. Setzt KEINEN Fokus mehr — das
    /// wuerde unser Floating-Panel direkt nach onSubmit wieder nach vorne
    /// holen und mit pasteText racen (Voice-Overlay klaut sich zurueck zum
    /// Front-Window, Terminal verliert seinen frischen Activate-Status, und
    /// das Cmd+V landet dann u.U. in unserem Panel statt im Terminal — was
    /// einen System-Beep ausloest weil ein leeres TextView Cmd+V nicht
    /// sinnvoll handhabt).
    func clearInput() {
        textView.string = ""
        // Bewusst KEIN makeKeyAndFocusInput() hier. Wenn der Benutzer den
        // naechsten Prompt tippen will, klickt er einmal ins Eingabefeld.
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
        // Stern bleibt auf Wunsch des Benutzers (Frank, 2026-05-09) IMMER
        // goldig (gefuellter Stern ★ in #FFD700), unabhaengig vom Solo-Modus.
        configureToolbarButton(soloDockButton,
            symbol: "★",
            color: NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1),
            fontSize: 14,
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
        // Ganz aussen rechts das Kopieren/Einfuegen-Paar — SF-Symbole, damit es
        // wie gekaufte Software aussieht. Kopieren legt den Feld-Text in die
        // System-Zwischenablage (ueberall mit ⌘V einfuegbar), Einfuegen holt den
        // Zwischenablage-Text an die Cursor-Position ins Feld.
        configureToolbarButton(copyButton,
            symbol: "", color: NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1),
            fontSize: 14,
            sfSymbol: "doc.on.doc",
            tooltip: "Angezeigten Text kopieren — danach ueberall mit ⌘V einfuegbar.",
            action: #selector(onCopyClick))
        configureToolbarButton(pasteButton,
            symbol: "", color: NSColor(calibratedRed: 0.30, green: 0.85, blue: 0.39, alpha: 1),
            fontSize: 14,
            sfSymbol: "doc.on.clipboard",
            tooltip: "Text aus der Zwischenablage ins Eingabefeld einfuegen.",
            action: #selector(onPasteClick))

        // Standard-Symbole als eine Gruppe (Stern, Trenner, Gemini, Loeschen).
        let mainToolbar = NSStackView(views: [soloDockButton, separatorButton, geminiButton, clearButton])
        mainToolbar.orientation = .horizontal
        mainToolbar.spacing = 4
        mainToolbar.alignment = .centerY
        mainToolbar.setContentHuggingPriority(.required, for: .horizontal)

        // Kopieren/Einfuegen als eigene Gruppe, die GANZ AUSSEN am rechten Rand
        // klebt — bewusst abgesetzt von den Standard-Symbolen, NICHT direkt neben
        // dem X (Frank-Wunsch 2026-06-06).
        let clipboardToolbar = NSStackView(views: [copyButton, pasteButton])
        clipboardToolbar.orientation = .horizontal
        clipboardToolbar.spacing = 4
        clipboardToolbar.alignment = .centerY
        clipboardToolbar.setContentHuggingPriority(.required, for: .horizontal)

        // Flexible Luecke zwischen Standard-Gruppe und Kopieren/Einfuegen-Paar,
        // damit das Paar nach ganz rechts geschoben wird (min. 24 pt Abstand).
        let midSpacer = NSView()
        midSpacer.translatesAutoresizingMaskIntoConstraints = false
        midSpacer.widthAnchor.constraint(greaterThanOrEqualToConstant: 24).isActive = true
        midSpacer.setContentHuggingPriority(.init(1), for: .horizontal)

        // hintLabel: greedy spacer direkt nach dem Titel.
        hintLabel.setContentHuggingPriority(.init(1), for: .horizontal)
        let header = NSStackView(views: [titleLabel, hintLabel, mainToolbar, midSpacer, clipboardToolbar])
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

        // Trennlinie OBEN: grenzt das Eingabefeld nach oben gegen die Kopfzeile
        // (Titel + Toolbar) ab — Gegenstueck zur Linie ueber der Slot-Leiste
        // (Frank-Wunsch 2026-06-06). Gleiche Optik wie slotSeparator.
        let topSeparator = NSView()
        topSeparator.wantsLayer = true
        topSeparator.layer?.backgroundColor = NSColor(calibratedWhite: 0.32, alpha: 1).cgColor
        topSeparator.translatesAutoresizingMaskIntoConstraints = false
        topSeparator.heightAnchor.constraint(equalToConstant: 1).isActive = true

        // Trennlinie + Zahlen-Leiste (Prompt-Zwischenspeicher 1…15) ganz unten.
        // Die Linie grenzt die Leiste sichtbar vom Eingabefeld ab — die Slots
        // sind unabhaengig von der Eingabe (Frank-Wunsch 2026-06-05).
        let slotSeparator = NSView()
        slotSeparator.wantsLayer = true
        slotSeparator.layer?.backgroundColor = NSColor(calibratedWhite: 0.32, alpha: 1).cgColor
        slotSeparator.translatesAutoresizingMaskIntoConstraints = false
        slotSeparator.heightAnchor.constraint(equalToConstant: 1).isActive = true

        let slotBar = buildSlotBar()

        let stack = NSStackView(views: [header, topSeparator, scrollView, previewLabel, slotSeparator, slotBar])
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
            topSeparator.widthAnchor.constraint(equalTo: stack.widthAnchor),
            scrollView.widthAnchor.constraint(equalTo: stack.widthAnchor),
            scrollView.heightAnchor.constraint(greaterThanOrEqualToConstant: 240),
            previewLabel.widthAnchor.constraint(equalTo: stack.widthAnchor),
            slotSeparator.widthAnchor.constraint(equalTo: stack.widthAnchor),
        ])

        updateSlotVisuals()
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
                                         sfSymbol: String? = nil,
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

        // SF-Symbol-Variante (fuer Kopieren/Einfuegen): monochrome Template-
        // Vorlage in einem NSImageView, ueber contentTintColor eingefaerbt —
        // gleiche 28x28-Basis wie die Text-Symbol-Buttons.
        if let sfSymbol = sfSymbol,
           let base = NSImage(systemSymbolName: sfSymbol, accessibilityDescription: nil) {
            let cfg = NSImage.SymbolConfiguration(pointSize: fontSize, weight: .semibold)
            let iv = NSImageView(image: base.withSymbolConfiguration(cfg) ?? base)
            iv.contentTintColor = color
            iv.translatesAutoresizingMaskIntoConstraints = false
            btn.addSubview(iv)
            NSLayoutConstraint.activate([
                btn.widthAnchor.constraint(equalToConstant: 28),
                btn.heightAnchor.constraint(equalToConstant: 28),
                iv.centerXAnchor.constraint(equalTo: btn.centerXAnchor),
                iv.centerYAnchor.constraint(equalTo: btn.centerYAnchor),
            ])
            btn.identifier = NSUserInterfaceItemIdentifier(sfSymbol + "-icon")
            return
        }

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

    /// Aktualisiert NUR den Tooltip — auf Wunsch des Benutzers (Frank,
    /// 2026-05-09) bleibt das Stern-Icon IMMER goldig (gefuellter Stern,
    /// #FFD700), unabhaengig vom Solo-Modus. Das hilft dem Benutzer den
    /// Stern als visuellen Anker wiederzuerkennen — gleicher Look wie der
    /// Stern in der Promtboard-Toolbar.
    func setSoloDockState(_ active: Bool) {
        isSoloDocked = active
        guard let label = toolbarLabel(of: soloDockButton) else { return }
        // Visual fix gold — bei jedem Aufruf neu setzen, damit auch ein
        // fruehes Init-Update die Defaultfarbe ueberschreibt.
        label.stringValue = "★"
        label.textColor = NSColor(calibratedRed: 1.0, green: 0.84, blue: 0.0, alpha: 1)
        soloDockButton.toolTip = active
            ? "Promptboard wieder einblenden (zurueck in den Normalmodus)."
            : "Promptboard ausblenden und Eingabe direkt ans Voice-Overlay andocken (erneuter Klick blendet das Promptboard wieder ein)."
    }

    @objc private func onInsertSeparatorClick() {
        // Trenner anhaengen: exakt 1 Space + Semikolon + 1 Space — 1:1 wie
        // im Windows-Voice-Overlay. KEINE Newlines drumherum (vorherige
        // Variante "\n\n ; \n\n" hat Leerzeilen erzeugt).
        let suffix = " ; "
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

    /// Kopiert den aktuell angezeigten Eingabe-Text in die System-Zwischenablage.
    /// Danach laesst er sich ueberall (Browser, Editor, …) mit ⌘V einfuegen. Kurzes
    /// gruenes Aufleuchten des Buttons bestaetigt den Kopiervorgang.
    @objc private func onCopyClick() {
        let text = textView.string
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let pb = NSPasteboard.general
        pb.clearContents()
        pb.setString(text, forType: .string)
        // Aufleuchten in hellem Gelb — passend zum jetzt gelben Kopieren-Button
        // (Gruen ist die Farbe des Einfuegen-Buttons, daher hier bewusst Gelb).
        flashSlotButton(copyButton,
            color: NSColor(calibratedRed: 1.0, green: 0.90, blue: 0.40, alpha: 1))
    }

    /// Fuegt den Text aus der System-Zwischenablage an der Cursor-Position ins
    /// Eingabefeld ein (Gegenstueck zum Kopieren). Nutzt appendText, damit eine
    /// bestehende Auswahl ersetzt und der Fokus zurueck ins Feld gesetzt wird.
    @objc private func onPasteClick() {
        guard let text = NSPasteboard.general.string(forType: .string),
              !text.isEmpty else { return }
        appendText(text)
    }

    // MARK: - Prompt-Zwischenspeicher-Slots (1…30)

    /// Wird vom AppDelegate aufgerufen — uebergibt den aktuellen Stand der
    /// belegten Slots (Nummer → Text). Faerbt die Zahlen-Leiste neu ein und
    /// laesst die aktuelle Auswahl/Diskette unveraendert.
    func setSlotContents(_ map: [Int: String], timestamps: [Int: Date] = [:],
                         summaries: [Int: String] = [:], priorities: [Int: Int] = [:]) {
        slotContents = map.filter { !$0.value.isEmpty }
        slotTimestamps = timestamps
        slotSummaries = summaries.filter { !$0.value.isEmpty }
        slotPriorities = priorities.filter { $0.value != 0 }
        updateSlotVisuals()
        // Bestehende belegte Slots ohne Zusammenfassung nachtraeglich auffuellen.
        maybeBackfillSummaries()
    }

    /// Baut die untere Leiste: Zahlen 1-30 in zwei Reihen (1-15 oben,
    /// 16-30 unten). Diskette und X liegen EINMAL rechts daneben (vertikal
    /// zentriert ueber beide Reihen) und gelten fuer den gewaehlten Slot egal
    /// in welcher Reihe. Sie sind anfangs versteckt — sie erscheinen erst nach
    /// Auswahl einer Zahl (Frank-Wunsch).
    private func buildSlotBar() -> NSView {
        var row1Views: [NSView] = []
        var row2Views: [NSView] = []
        for n in 1...PromptSlotStore.slotCount {
            let btn = HoverButton()
            // Kein System-Tooltip (leerer String) — die Anzeige uebernimmt der
            // eigene gestylte Tooltip ueber die Hover-Erkennung (handleSlotHover).
            configureSlotButton(btn, title: "\(n)", textColor: Self.slotGrey,
                tooltip: "",
                action: #selector(onSlotNumberClick(_:)))
            btn.tag = n
            btn.onHover = { [weak self] inside in self?.handleSlotHover(n, inside: inside) }
            // Rechtsklick auf einen belegten Slot -> Prioritaets-Menue. Bei leerem
            // Slot wird das Menue im Right-Drag-Monitor unterdrueckt (kein Drag,
            // kein Menue).
            btn.menu = buildPriorityMenu(for: n)
            slotButtons[n] = btn
            // 1-15 in die obere Reihe, 16-30 in die untere.
            if n <= Self.slotsPerRow { row1Views.append(btn) } else { row2Views.append(btn) }
        }
        configureSlotButton(slotSaveButton, title: "💾", textColor: Self.slotGold,
            tooltip: "Aktuellen Prompt im gewaehlten Slot dauerhaft speichern.",
            action: #selector(onSlotSaveClick))
        configureSlotButton(slotDeleteButton, title: "✕", textColor: Self.slotRed,
            tooltip: "Prompt im gewaehlten Slot dauerhaft loeschen.",
            action: #selector(onSlotDeleteClick))
        slotSaveButton.isHidden = true
        slotDeleteButton.isHidden = true

        // Zeitstempel-Label rechts neben dem X — zeigt wann der Prompt im
        // gewaehlten Slot gespeichert wurde. Anfangs leer/versteckt.
        slotTimeLabel.textColor = Self.slotGrey
        slotTimeLabel.font = NSFont.systemFont(ofSize: 11)
        slotTimeLabel.isHidden = true

        // Kleiner Abstand zwischen Zahlen und den Aktions-Buttons.
        let spacer = NSView()
        spacer.translatesAutoresizingMaskIntoConstraints = false
        spacer.widthAnchor.constraint(equalToConstant: 8).isActive = true

        // Zwei Zahlen-Reihen untereinander: 1-15 oben, 16-30 unten.
        let row1 = NSStackView(views: row1Views)
        row1.orientation = .horizontal
        row1.spacing = 7
        row1.alignment = .centerY
        let row2 = NSStackView(views: row2Views)
        row2.orientation = .horizontal
        row2.spacing = 7
        row2.alignment = .centerY
        let rows = NSStackView(views: [row1, row2])
        rows.orientation = .vertical
        rows.spacing = 6
        rows.alignment = .leading

        // Gemeinsame Aktions-Buttons (Diskette/X/Zeit) EINMAL, vertikal
        // zentriert ueber beide Reihen — gelten fuer den gewaehlten Slot
        // egal in welcher Reihe.
        let actions = NSStackView(views: [slotSaveButton, slotDeleteButton, slotTimeLabel])
        actions.orientation = .horizontal
        actions.spacing = 7
        actions.alignment = .centerY

        let bar = NSStackView(views: [rows, spacer, actions])
        bar.orientation = .horizontal
        bar.spacing = 0
        bar.alignment = .centerY
        return bar
    }

    /// Style-Helper fuer einen Slot-Button (Zahl oder Aktion). 30x26, gleiche
    /// dunkle Optik wie die Toolbar-Buttons, aber rechteckiger (CornerRadius 6).
    private func configureSlotButton(_ btn: NSButton, title: String,
                                     textColor: NSColor, tooltip: String,
                                     action: Selector) {
        btn.title = ""
        btn.bezelStyle = .regularSquare
        btn.isBordered = false
        btn.target = self
        btn.action = action
        btn.toolTip = tooltip.isEmpty ? nil : tooltip
        btn.translatesAutoresizingMaskIntoConstraints = false
        btn.wantsLayer = true
        btn.layer?.backgroundColor = NSColor(calibratedWhite: 0.18, alpha: 1).cgColor
        btn.layer?.cornerRadius = 6

        let label = NSTextField(labelWithString: title)
        label.textColor = textColor
        label.font = NSFont.systemFont(ofSize: 13, weight: .semibold)
        label.alignment = .center
        label.isBezeled = false
        label.drawsBackground = false
        label.isEditable = false
        label.isSelectable = false
        label.translatesAutoresizingMaskIntoConstraints = false
        btn.addSubview(label)

        NSLayoutConstraint.activate([
            btn.widthAnchor.constraint(equalToConstant: 30),
            btn.heightAnchor.constraint(equalToConstant: 26),
            label.centerXAnchor.constraint(equalTo: btn.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: btn.centerYAnchor),
        ])
    }

    // ── Prioritaet (Rechtsklick-Menue) ──────────────────────────────────────

    /// Baut das Rechtsklick-Menue eines Slots: Hoch/Mittel/Niedrig + Keine, je
    /// mit farbigem Quadrat. `representedObject` traegt [slot, level].
    private func buildPriorityMenu(for n: Int) -> NSMenu {
        let menu = NSMenu()
        menu.autoenablesItems = false
        func add(_ level: Int, _ label: String, _ color: NSColor) {
            let item = NSMenuItem(title: label, action: #selector(onSetPriority(_:)), keyEquivalent: "")
            item.target = self
            item.image = Self.prioritySwatch(color)
            item.representedObject = [n, level]
            menu.addItem(item)
        }
        add(3, "Hoch", Self.slotPrioHigh)
        add(2, "Mittel", Self.slotPrioMedium)
        add(1, "Niedrig", Self.slotPrioLow)
        menu.addItem(.separator())
        add(0, "Keine", Self.slotGrey)
        return menu
    }

    @objc private func onSetPriority(_ sender: NSMenuItem) {
        guard let arr = sender.representedObject as? [Int], arr.count == 2 else { return }
        let n = arr[0]
        let priority = arr[1]
        guard !(slotContents[n]?.isEmpty ?? true) else { return } // nur belegte Slots
        if priority <= 0 { slotPriorities.removeValue(forKey: n) }
        else { slotPriorities[n] = priority }
        updateSlotVisuals()
        onSlotPriority?(n, max(0, priority))
    }

    /// 12x12-Farbquadrat fuer die Menue-Eintraege.
    private static func prioritySwatch(_ color: NSColor) -> NSImage {
        let size = NSSize(width: 12, height: 12)
        let img = NSImage(size: size)
        img.lockFocus()
        let path = NSBezierPath(roundedRect: NSRect(origin: .zero, size: size), xRadius: 3, yRadius: 3)
        color.setFill()
        path.fill()
        img.unlockFocus()
        return img
    }

    private static func priorityColor(_ p: Int) -> NSColor {
        switch p {
        case 3: return slotPrioHigh
        case 2: return slotPrioMedium
        case 1: return slotPrioLow
        default: return NSColor(calibratedWhite: 0.18, alpha: 1)
        }
    }

    /// Faerbt die Zahlen-Leiste neu: belegte Zahlen gold, leere grau, die
    /// ausgewaehlte Zahl bekommt einen goldenen Rahmen. Diskette/X sind nur
    /// sichtbar wenn eine Zahl ausgewaehlt ist; X ist nur aktiv wenn der Slot
    /// wirklich Inhalt hat.
    private func updateSlotVisuals() {
        for (n, btn) in slotButtons {
            let hasContent = !(slotContents[n]?.isEmpty ?? true)
            let prio = hasContent ? (slotPriorities[n] ?? 0) : 0
            let isSelected = (selectedSlot == n)
            if prio != 0 {
                // Belegt MIT Prioritaet: farbiger Hintergrund + Near-Black-Zahl.
                slotLabel(of: btn)?.textColor = Self.slotPrioText
                btn.layer?.backgroundColor = Self.priorityColor(prio).cgColor
            } else {
                // Belegt ohne Prioritaet: Gold-Zahl; leer: graue Zahl. Auswahl
                // hellt den Standard-Hintergrund leicht auf.
                slotLabel(of: btn)?.textColor = hasContent ? Self.slotGold : Self.slotGrey
                btn.layer?.backgroundColor = isSelected
                    ? NSColor(calibratedWhite: 0.26, alpha: 1).cgColor
                    : NSColor(calibratedWhite: 0.18, alpha: 1).cgColor
            }
            btn.layer?.borderWidth = isSelected ? 2 : 0
            btn.layer?.borderColor = isSelected ? Self.slotGold.cgColor : NSColor.clear.cgColor
            // Kein System-Tooltip mehr — die Anzeige uebernimmt das eigene
            // gestylte Tooltip-Fenster ueber die Hover-Erkennung (handleSlotHover).
        }
        let hasSelection = (selectedSlot != nil)
        slotSaveButton.isHidden = !hasSelection
        slotDeleteButton.isHidden = !hasSelection
        // X ist immer aktiv wenn eine Zahl gewaehlt ist — Loeschen+Sync soll
        // immer durchlaufen (der Store ist die Wahrheit, nicht der lokale Cache).
        slotDeleteButton.isEnabled = hasSelection
        slotDeleteButton.alphaValue = 1.0

        // Zeitstempel rechts neben dem X — nur wenn der gewaehlte Slot belegt
        // ist und ein Speicher-Zeitpunkt bekannt ist.
        if let s = selectedSlot, let t = slotTimestamps[s],
           !(slotContents[s]?.isEmpty ?? true) {
            slotTimeLabel.stringValue = "🕒 \(Self.slotTimeFormatter.string(from: t))"
            slotTimeLabel.isHidden = false
        } else {
            slotTimeLabel.stringValue = ""
            slotTimeLabel.isHidden = true
        }
    }

    private func slotLabel(of btn: NSButton) -> NSTextField? {
        return btn.subviews.compactMap { $0 as? NSTextField }.first
    }

    /// Reaktion auf Maus-Hover ueber einen Slot. Nur belegte Slots MIT
    /// Zusammenfassung zeigen nach kurzer Verzoegerung (~130ms wie Windows) das
    /// eigene gestylte Tooltip-Fenster; leere oder noch nicht zusammengefasste
    /// Slots zeigen NICHTS (Frank-Wunsch). Verlassen blendet sofort aus.
    private func handleSlotHover(_ n: Int, inside: Bool) {
        if !inside {
            hoverTimer?.invalidate(); hoverTimer = nil
            if hoveredSlot == n {
                hoveredSlot = nil
                tooltipPanel.orderOut(nil)
            }
            return
        }
        hoveredSlot = n
        hoverTimer?.invalidate()
        guard let summary = slotSummaries[n], !summary.isEmpty,
              !(slotContents[n]?.isEmpty ?? true) else {
            tooltipPanel.orderOut(nil)
            return
        }
        hoverTimer = Timer.scheduledTimer(withTimeInterval: 0.13, repeats: false) { [weak self] _ in
            guard let self = self, self.hoveredSlot == n,
                  let btn = self.slotButtons[n], let win = btn.window else { return }
            let screenRect = win.convertToScreen(btn.convert(btn.bounds, to: nil))
            self.tooltipPanel.show(text: summary, above: screenRect)
        }
    }

    /// Aktualisiert das offene Tooltip-Fenster live, falls die Maus gerade ueber
    /// genau diesem Slot steht und seine Summary frisch erzeugt wurde (Backfill
    /// oder direkt nach dem Speichern).
    private func applySlotTooltip(_ n: Int) {
        guard hoveredSlot == n, let summary = slotSummaries[n], !summary.isEmpty,
              let btn = slotButtons[n], let win = btn.window else { return }
        let screenRect = win.convertToScreen(btn.convert(btn.bounds, to: nil))
        tooltipPanel.show(text: summary, above: screenRect)
    }

    /// Stoesst den Backfill an: erzeugt fuer alle belegten Slots OHNE
    /// Zusammenfassung nachtraeglich eine — auch fuer Prompts aus der Zeit vor
    /// diesem Feature (Frank-Wunsch 2026-06-11). Laeuft nur einmal gleichzeitig;
    /// ohne Gemini-Handler passiert nichts.
    private func maybeBackfillSummaries() {
        guard !backfillRunning, onGenerateSlotSummary != nil else { return }
        let todo = slotContents.keys
            .filter { slotSummaries[$0] == nil && !summaryInFlight.contains($0) }
            .sorted()
        guard !todo.isEmpty else { return }
        backfillRunning = true
        backfillNext(todo)
    }

    /// Arbeitet die Backfill-Liste SEQUENTIELL ab (eine Summary nach der
    /// anderen, mit 0,6s Pause) — sanfte Drosselung gegen Gemini-Rate-Limits.
    /// Nutzt denselben Pfad wie ein frisches Speichern.
    private func backfillNext(_ remaining: [Int]) {
        var rest = remaining
        guard !rest.isEmpty else { backfillRunning = false; return }
        let n = rest.removeFirst()
        guard let text = slotContents[n], !text.isEmpty, slotSummaries[n] == nil,
              !summaryInFlight.contains(n), let gen = onGenerateSlotSummary else {
            backfillNext(rest)
            return
        }
        summaryInFlight.insert(n)
        gen(text) { [weak self] summary in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.summaryInFlight.remove(n)
                if !summary.isEmpty, self.slotContents[n] == text {
                    self.slotSummaries[n] = summary
                    self.applySlotTooltip(n)
                    self.onSlotSummary?(n, text, summary)
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    self.backfillNext(rest)
                }
            }
        }
    }

    /// Klick auf eine Zahl: auswaehlen, Diskette/X einblenden. Hat der Slot
    /// bereits einen gespeicherten Prompt, wird er sofort ins Eingabefeld
    /// geladen (Zwischenspeicher abrufen — Frank-Wunsch). Leere Slots lassen
    /// den getippten Text stehen, damit der Benutzer ihn dort ablegen kann.
    @objc private func onSlotNumberClick(_ sender: NSButton) {
        let n = sender.tag
        guard (1...PromptSlotStore.slotCount).contains(n) else { return }
        selectedSlot = n
        if let text = slotContents[n], !text.isEmpty {
            // Belegter Slot → gespeicherten Prompt abrufen (bewusste Lade-Aktion).
            setText(text)
        }
        // Leerer Slot → das Eingabefeld NIE anfassen. Der frisch getippte Prompt
        // muss stehen bleiben, denn genau ihn will der Benutzer jetzt in diesem
        // Slot speichern. Frueher stand hier setText("") — das loeschte den
        // getippten Prompt im Moment des Zahl-Klicks, BEVOR die Diskette gedrueckt
        // werden konnte (Frank-Datenverlust 2026-06-06). NIEMALS wieder leeren.
        updateSlotVisuals()
    }

    /// Diskette: speichert den aktuellen Eingabe-Text im gewaehlten Slot.
    /// Leerer Text wird nicht gespeichert (kurzer roter Flash als Hinweis).
    @objc private func onSlotSaveClick() {
        guard let n = selectedSlot else { return }
        let text = textView.string
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            flashSlotButton(slotSaveButton, color: Self.slotRed)
            return
        }
        slotContents[n] = text
        slotTimestamps[n] = Date()
        slotSummaries.removeValue(forKey: n)   // alte Summary passt nicht mehr
        updateSlotVisuals()
        flashSlotButton(slotSaveButton, color: Self.slotGold)
        onSlotSave?(n, text)

        // Frische 6-8-Wort-Zusammenfassung asynchron per Gemini holen
        // (best-effort, blockiert das Speichern nicht). Nur anwenden, wenn der
        // Slot noch exakt diesen Text haelt.
        let savedText = text
        onGenerateSlotSummary?(text) { [weak self] summary in
            DispatchQueue.main.async {
                guard let self = self, !summary.isEmpty else { return }
                guard self.slotContents[n] == savedText else { return }
                self.slotSummaries[n] = summary
                self.applySlotTooltip(n)
                self.onSlotSummary?(n, savedText, summary)
            }
        }
    }

    /// X: loescht den gewaehlten Slot dauerhaft. Laeuft IMMER durch wenn eine
    /// Zahl gewaehlt ist (nicht mehr vom lokalen Cache abhaengig — Frank-Bug
    /// 2026-06-05: X reagierte nicht, weil der Button bei vermeintlich leerem
    /// Slot deaktiviert war). Tombstone im Store + SOFORT-Sync verhindern, dass
    /// der Prompt beim naechsten Merge wieder auftaucht.
    @objc private func onSlotDeleteClick() {
        guard let n = selectedSlot else { return }
        slotContents.removeValue(forKey: n)
        slotTimestamps.removeValue(forKey: n)
        slotSummaries.removeValue(forKey: n)
        // Geloeschten Prompt auch aus dem Eingabefeld entfernen.
        clearInput()
        updateSlotVisuals()
        flashSlotButton(slotDeleteButton, color: Self.slotRed)
        onSlotDelete?(n)
    }

    /// Kurzer Aufleucht-Effekt (~400ms) als visuelle Bestaetigung.
    private func flashSlotButton(_ btn: NSButton, color: NSColor) {
        let glow = CABasicAnimation(keyPath: "backgroundColor")
        glow.fromValue = color.cgColor
        glow.toValue = NSColor(calibratedWhite: 0.18, alpha: 1).cgColor
        glow.duration = 0.4
        glow.timingFunction = CAMediaTimingFunction(name: .easeOut)
        btn.layer?.add(glow, forKey: "slotFlash")
    }

    // MARK: - Andocken am Pillar (Solo-Dock-Modus)

    /// Dockt die Eingabe direkt LINKS an den Pillar (Voice-Overlay) — ohne
    /// PromptBoard dazwischen. Wird vom AppDelegate aufgerufen wenn der
    /// Solo-Dock-Modus aktiv ist (entweder durch Pillar-Stern-Klick oder
    /// durch Solo-Dock-Stern in der eigenen Toolbar). Hoehe wird an den
    /// Pillar angeglichen damit beide Fenster als Paar wirken.
    func dockToOverlay(_ pillar: NSWindow) {
        var frame = self.frame
        let isHorizontal = (pillar as? OverlayPanel)?.currentOrientation == .horizontal
        if isHorizontal {
            // Horizontaler VTO-Modus: Eingabe dockt OBEN an (Unterkante an der
            // VTO-Oberkante), linksbuendig mit der Leiste (Frank-Wunsch
            // 2026-05-28). Hoehe = 3/4 der vertikalen Saeulenhoehe — die Leiste
            // selbst ist nur ~92px hoch.
            // UNTERKANTE fix BUENDIG an der VTO-Oberkante (4px Anschluss). Hoehe
            // kuerzen statt verschieben, falls oben kein Platz ist — sonst wuerde
            // clampToScreen das Fenster nach unten druecken ("zu tief").
            // Frank-Wunsch 2026-05-28. Rechtsbuendig zur VTO-Aussenkante.
            let bottomY = pillar.frame.origin.y + pillar.frame.height + 4
            var h = (OverlayPanel.verticalPanelHeight * 0.75).rounded()
            if let vf = NSScreen.main?.visibleFrame, bottomY + h > vf.maxY {
                h = vf.maxY - bottomY
            }
            frame.size.height = h
            frame.origin.y = bottomY
            frame.origin.x = pillar.frame.origin.x + pillar.frame.width - frame.size.width
        } else {
            // Vertikaler Modus: Eingabe dockt LINKS an, oben buendig.
            // Hoehe: 3/4 der Pillar-Hoehe (#1174).
            let h = (pillar.frame.height * 0.75).rounded()
            frame.size.height = h
            frame.origin.x = pillar.frame.origin.x - frame.size.width - 4
            frame.origin.y = pillar.frame.origin.y + pillar.frame.height - h
        }
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
                // Rechtsklick auf einen Slot-Button: belegt -> NSMenu (Prioritaet)
                // zulassen (kein Drag), leer -> nichts (kein Drag, kein Menue).
                if let hit = self.contentView?.hitTest(event.locationInWindow),
                   let slotN = self.slotNumber(forHit: hit) {
                    let occupied = !(self.slotContents[slotN]?.isEmpty ?? true)
                    return occupied ? event : nil
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

    /// Laeuft vom getroffenen View die Eltern hoch und liefert die Slot-Nummer,
    /// falls der Klick auf einem Zahlen-Slot-Button (HoverButton mit tag 1...N)
    /// sitzt — sonst nil. Damit erkennt der Right-Drag-Monitor Slot-Klicks.
    private func slotNumber(forHit view: NSView) -> Int? {
        var v: NSView? = view
        while let cur = v {
            if let btn = cur as? HoverButton,
               (1...PromptSlotStore.slotCount).contains(btn.tag),
               slotButtons[btn.tag] === btn {
                return btn.tag
            }
            v = cur.superview
        }
        return nil
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
///
/// Implementiert ueber doCommand(by:) statt keyDown(with:): NSTextView
/// uebersetzt Tastenanschlaege intern via interpretKeyEvents in Selectoren
/// (Enter -> insertNewline:, Shift+Enter -> insertNewlineIgnoringFieldEditor:,
/// Tab -> insertTab: usw.). Eine keyDown-Override ist fragil, weil
/// NSTextView seinen Default-Pfad trotzdem startet und der Event nicht sauber
/// "consumed" wird — Folge: Beep und kein Submit. doCommand(by:) ist der
/// kanonische Hook fuer genau diesen Fall.
/// weiterhin einen Zeilenumbruch — wir fangen NUR die nackte Return-Taste ab.
final class SubmitTextView: NSTextView {

    var onSubmit: ((String) -> Void)?

    override func doCommand(by selector: Selector) {
        // insertNewline: → nacktes Return/Enter ohne Modifier
        // insertNewlineIgnoringFieldEditor: → Shift+Enter / Option+Enter
        //   (= echter Zeilenumbruch, NICHT abfangen)
        if selector == #selector(NSResponder.insertNewline(_:)) {
            onSubmit?(self.string)
            return
        }
        super.doCommand(by: selector)
    }
}

/// NSButton, der Maus-Eintritt/-Austritt ueber eine NSTrackingArea meldet —
/// Grundlage fuer das eigene Tooltip-Fenster (der System-NSToolTip laesst sich
/// nicht im App-Stil gestalten).
final class HoverButton: NSButton {
    var onHover: ((Bool) -> Void)?
    private var hoverTrackingArea: NSTrackingArea?

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let ta = hoverTrackingArea { removeTrackingArea(ta) }
        let ta = NSTrackingArea(
            rect: bounds,
            options: [.mouseEnteredAndExited, .activeAlways, .inVisibleRect],
            owner: self, userInfo: nil)
        addTrackingArea(ta)
        hoverTrackingArea = ta
    }

    override func mouseEntered(with event: NSEvent) { onHover?(true) }
    override func mouseExited(with event: NSEvent) { onHover?(false) }
}

/// Kleines, selbst gezeichnetes Tooltip-Fenster im App-Stil — 1:1 Pendant zum
/// gestylten WPF-Tooltip unter Windows (weiss, abgerundete Ecken, Schatten,
/// dunkle Schrift). Borderless, nonactivating, hohes Fenster-Level → schwebt im
/// Vordergrund ueber dem Overlay und klaut keinen Fokus.
final class SlotTooltipPanel: NSPanel {
    private let container = NSView()
    private let label = NSTextField(wrappingLabelWithString: "")

    init() {
        super.init(contentRect: NSRect(x: 0, y: 0, width: 200, height: 40),
                   styleMask: [.borderless, .nonactivatingPanel],
                   backing: .buffered, defer: false)
        isFloatingPanel = true
        level = .popUpMenu          // ueber dem Overlay (.floating)
        backgroundColor = .clear
        isOpaque = false
        hasShadow = true            // Fenster-Schatten folgt der runden, opaken Flaeche
        ignoresMouseEvents = true   // Tooltip faengt keine Klicks/Hover ab
        hidesOnDeactivate = false
        collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary, .ignoresCycle]

        container.wantsLayer = true
        container.layer?.backgroundColor = NSColor.white.cgColor
        container.layer?.cornerRadius = 8
        container.layer?.borderColor = NSColor(calibratedWhite: 0.82, alpha: 1).cgColor
        container.layer?.borderWidth = 1

        label.font = NSFont.systemFont(ofSize: 13)
        label.textColor = NSColor(calibratedWhite: 0.12, alpha: 1)
        label.isBezeled = false
        label.drawsBackground = false
        label.isEditable = false
        label.isSelectable = false
        label.maximumNumberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false

        contentView = container
        container.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 11),
            label.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -11),
            label.topAnchor.constraint(equalTo: container.topAnchor, constant: 7),
            label.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -7),
        ])
    }

    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }

    /// Zeigt den Tooltip mit `text`, ueber `screenRect` (Button-Rahmen in
    /// Bildschirmkoordinaten) zentriert. Klemmt in den sichtbaren Bildschirm und
    /// weicht nach unten aus, wenn oben kein Platz ist.
    func show(text: String, above screenRect: NSRect) {
        label.stringValue = text
        label.preferredMaxLayoutWidth = 360
        container.layoutSubtreeIfNeeded()
        let fit = container.fittingSize
        let w = min(max(fit.width, 70), 384)
        let h = max(fit.height, 26)
        var x = screenRect.midX - w / 2
        var y = screenRect.maxY + 6   // y waechst nach oben -> oberhalb des Buttons
        let screen = NSScreen.screens.first {
            $0.frame.contains(NSPoint(x: screenRect.midX, y: screenRect.midY))
        } ?? NSScreen.main
        if let vf = screen?.visibleFrame {
            x = Swift.max(vf.minX + 4, Swift.min(x, vf.maxX - w - 4))
            if y + h > vf.maxY { y = screenRect.minY - h - 6 }  // sonst unterhalb
            y = Swift.max(vf.minY + 4, y)
        }
        setFrame(NSRect(x: x, y: y, width: w, height: h), display: true)
        orderFront(nil)
    }
}

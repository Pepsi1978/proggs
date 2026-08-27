import AppKit

// MARK: - Colors
// Tuned to match the Windows TerminalVoiceOverlay 1:1 after commits
// #1953..#1964: Material 700/800 dark-theme tones, yellow Mic family,
// teal Screenshot/Insert pair, blue Copy/Paste pair, profile tiles in
// goldenrod (active) / dark (inactive), and the 7-section dark panel
// with black dividers and a black 2px outer ring.
// Sichtbarkeit: bewusst INTERN (nicht fileprivate). Die Palette wird auch von
// OverlayExtraButtons.swift/OverlayHorizontalLayout.swift gebraucht; ein
// `private` hier hat den Build brechen lassen (Fix 2026-08-27).
extension NSColor {
    // Idle/base
    static let btnIdle       = NSColor(hex: "#2D2D2D")
    static let btnRecording  = NSColor(hex: "#C62828")
    static let btnProcessing = NSColor(hex: "#EF6C00")
    static let btnSuccess    = NSColor(hex: "#2E7D32")
    // Toggles
    static let toggleOn       = NSColor(hex: "#2E7D32")
    static let toggleOnBright = NSColor(hex: "#43A047")
    static let toggleOff      = NSColor(hex: "#2D2D2D")
    // Mic + BTW — Yellow family
    static let btnBtwIdle      = NSColor(hex: "#FBC02D")
    static let btnBtwRecording = NSColor(hex: "#F57F17")
    static let btnBtwPulse     = NSColor(hex: "#FFEB3B")
    static let btnMicIdle      = NSColor(hex: "#F9A825")
    // Special
    static let btnX        = NSColor(hex: "#C62828")
    static let btnXPressed = NSColor(hex: "#E53935")
    // Copy/Paste
    static let btnCopy  = NSColor(hex: "#0288D1")
    static let btnPaste = NSColor(hex: "#0277BD")
    // Screenshot/Insert
    static let btnScreenshot       = NSColor(hex: "#00796B")
    static let btnInsertScreenshot = NSColor(hex: "#00897B")
    // Star
    static let ultrathinkOn = NSColor(hex: "#9E7B0E")
    static let starGold     = NSColor(hex: "#DAA520")
    static let starMuted    = NSColor(hex: "#8B7355")
    // Pulse for main mic recording
    static let btnRecordingBright = NSColor(hex: "#FF5252")
    // Tooltip
    static let tooltipBackground = NSColor(hex: "#1A1A1A")
    static let tooltipBorder     = NSColor(hex: "#3A3A3A")

    // Dark label / icon tint for the yellow Mic/BTW + active profile tiles.
    static let darkLabel = NSColor(hex: "#1A1A1A")

    convenience init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        var rgb: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&rgb)
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1.0
        )
    }
}

// MARK: - RoundButton
class RoundButton: NSView {
    var buttonWidth: CGFloat = 40
    var buttonHeight: CGFloat = 40
    var cornerRadius: CGFloat = 0          // 0 = round (oval), >0 = squarish with rounding
    var buttonColor: NSColor = .btnIdle { didSet { needsDisplay = true } }
    var label: String = "" { didSet { needsDisplay = true } }
    var labelFont: NSFont = .systemFont(ofSize: 16, weight: .bold)
    var labelColor: NSColor = .white { didSet { needsDisplay = true } }
    var symbolImage: NSImage?
    /// Blendet das Symbol aus, ohne es zu verlieren. Windows blendet waehrend
    /// der Aufnahme das Mic-Icon komplett aus (MicIcon.Visibility = Collapsed)
    /// und zeigt NUR die Wellenanzeige — sonst liegen Icon und Welle
    /// uebereinander und das Bild wird unruhig.
    var symbolHidden: Bool = false { didSet { needsDisplay = true } }
    /// Skalierungsfaktor des symbolImage relativ zu bounds (Default 0.5).
    /// Windows-XAML hat z.B. Mic-Viewbox 22 in 52px-Button → 22/52 ≈ 0.42.
    var symbolScaleFactor: CGFloat = 0.5
    var useSquareShape: Bool = false       // legacy flag — retained for compat
    var onClick: (() -> Void)?
    var onMouseDown: (() -> Void)?
    var onMouseUp: (() -> Void)?
    /// Optional callbacks, used by the TooltipManager. Kept distinct from
    /// the existing hover-scale animation so tooltips can be wired in
    /// without breaking the animation behaviour.
    var onMouseEnteredHook: (() -> Void)?
    var onMouseExitedHook: (() -> Void)?

    init(label: String, color: NSColor, width: CGFloat = 40, height: CGFloat = 40) {
        self.label = label
        self.buttonColor = color
        self.buttonWidth = width
        self.buttonHeight = height
        super.init(frame: NSRect(x: 0, y: 0, width: width, height: height))
        wantsLayer = true
    }

    required init?(coder: NSCoder) { fatalError() }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        for area in trackingAreas { removeTrackingArea(area) }
        addTrackingArea(NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeAlways], owner: self))
    }

    override func draw(_ dirtyRect: NSRect) {
        let inset = bounds.insetBy(dx: 1, dy: 1)
        let path: NSBezierPath
        if cornerRadius > 0 {
            path = NSBezierPath(roundedRect: inset, xRadius: cornerRadius, yRadius: cornerRadius)
        } else if useSquareShape {
            path = NSBezierPath(roundedRect: inset, xRadius: 6, yRadius: 6)
        } else {
            path = NSBezierPath(ovalIn: inset)
        }
        buttonColor.setFill()
        path.fill()

        if let img = symbolImage, !symbolHidden {
            let imgSize = CGSize(width: bounds.width * symbolScaleFactor,
                                 height: bounds.height * symbolScaleFactor)
            let imgRect = NSRect(
                x: (bounds.width - imgSize.width) / 2,
                y: (bounds.height - imgSize.height) / 2,
                width: imgSize.width, height: imgSize.height
            )
            let tinted = img.copy() as! NSImage
            tinted.lockFocus()
            labelColor.set()
            NSRect(origin: .zero, size: tinted.size).fill(using: .sourceAtop)
            tinted.unlockFocus()
            tinted.draw(in: imgRect, from: .zero, operation: .sourceOver, fraction: 1.0)
        } else if !symbolHidden {
            let attrs: [NSAttributedString.Key: Any] = [
                .foregroundColor: labelColor,
                .font: labelFont
            ]
            let str = NSAttributedString(string: label, attributes: attrs)
            let size = str.size()
            let point = NSPoint(x: (bounds.width - size.width) / 2, y: (bounds.height - size.height) / 2)
            str.draw(at: point)
        }
    }

    override func mouseDown(with event: NSEvent) {
        if onMouseDown != nil {
            onMouseDown?()
        } else {
            onClick?()
        }
    }

    override func mouseUp(with event: NSEvent) {
        onMouseUp?()
    }

    override func mouseEntered(with event: NSEvent) {
        animateScale(1.15)
        onMouseEnteredHook?()
    }

    override func mouseExited(with event: NSEvent) {
        animateScale(1.0)
        onMouseExitedHook?()
    }

    private func animateScale(_ scale: CGFloat) {
        let mid = bounds.width / 2
        var t = CATransform3DIdentity
        t = CATransform3DTranslate(t, mid, mid, 0)
        t = CATransform3DScale(t, scale, scale, 1)
        t = CATransform3DTranslate(t, -mid, -mid, 0)

        let anim = CABasicAnimation(keyPath: "transform")
        anim.toValue = NSValue(caTransform3D: t)
        anim.duration = 0.15
        anim.fillMode = .forwards
        anim.isRemovedOnCompletion = false
        // Windows-XAML: QuadraticEase EaseOut, 150ms — langsam am Ende.
        // easeOut entspricht 1:1 dem Windows-QuadraticEase mit EasingMode=EaseOut.
        anim.timingFunction = CAMediaTimingFunction(name: .easeOut)
        layer?.add(anim, forKey: "hoverScale")
    }
}

// MARK: - TooltipPanel
/// Eigenes Tooltip-Fenster. NSView.toolTip waere nicht ausreichend — es
/// erlaubt keine freie Position oder eigenes Styling. Stattdessen ein
/// borderless NSPanel mit dunklem Hintergrund, abgerundeten Ecken und
/// weisser Schrift, das per TooltipManager an der korrekten Stelle
/// (links vom Pillar, mittig zum Button) eingeblendet wird.
final class TooltipPanel: NSPanel {
    private let label: NSTextField
    private static let horizontalPadding: CGFloat = 10
    private static let verticalPadding: CGFloat   = 6

    init() {
        let lbl = NSTextField(labelWithString: "")
        lbl.font = .systemFont(ofSize: 12, weight: .regular)
        lbl.textColor = .white
        lbl.backgroundColor = .clear
        lbl.isBordered = false
        lbl.isEditable = false
        lbl.isSelectable = false
        lbl.alignment = .center
        self.label = lbl

        super.init(
            contentRect: NSRect(x: 0, y: 0, width: 100, height: 28),
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )

        self.level = .statusBar
        self.isFloatingPanel = true
        self.becomesKeyOnlyIfNeeded = true
        self.hidesOnDeactivate = false
        self.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .ignoresCycle]
        self.isOpaque = false
        self.backgroundColor = .clear
        self.hasShadow = true
        self.ignoresMouseEvents = true

        let cv = NSView()
        cv.wantsLayer = true
        cv.layer?.backgroundColor = NSColor.tooltipBackground.cgColor
        cv.layer?.cornerRadius = 8
        cv.layer?.borderWidth = 1
        cv.layer?.borderColor = NSColor.tooltipBorder.cgColor
        cv.layer?.masksToBounds = true
        self.contentView = cv

        cv.addSubview(label)
    }

    /// Setzt den Text und positioniert das Panel so, dass die rechte Kante
    /// 8 Pixel links vom uebergebenen overlayFrame.minX sitzt und die
    /// vertikale Mitte mit centeredOnY zusammenfaellt. Wird von TooltipManager
    /// aufgerufen wenn ein Hover-Timer abgelaufen ist.
    func showText(_ text: String, leftOf overlayFrame: NSRect, centeredOnY: CGFloat,
                  margin: CGFloat = 8) {
        label.stringValue = text

        // Zielgroesse aus Text + Padding berechnen
        let textSize = label.fittingSize
        let panelWidth  = ceil(textSize.width  + Self.horizontalPadding * 2)
        let panelHeight = ceil(textSize.height + Self.verticalPadding   * 2)

        let x = overlayFrame.minX - margin - panelWidth
        let y = centeredOnY - panelHeight / 2

        self.setFrame(NSRect(x: x, y: y, width: panelWidth, height: panelHeight),
                      display: false)
        // Label im Content zentrieren
        label.frame = NSRect(
            x: Self.horizontalPadding,
            y: Self.verticalPadding,
            width: panelWidth - Self.horizontalPadding * 2,
            height: panelHeight - Self.verticalPadding * 2
        )

        if !self.isVisible { self.orderFrontRegardless() }
    }

    func hide() {
        if self.isVisible { self.orderOut(nil) }
    }

    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }
}

// MARK: - TooltipManager
/// Verwaltet einen einzigen TooltipPanel fuer das gesamte Overlay. Pro
/// Button werden mouseEntered/mouseExited-Hooks registriert, ein 100ms
/// Hover-Timer entscheidet ueber das Anzeigen. So bleibt der Code zentral
/// und die Latenz deterministisch — anders als WPF wartet AppKit nicht
/// auf Maus-Stillstand, der Timer feuert auch bei Mausbewegung.
final class TooltipManager {
    static let hoverDelay: TimeInterval = 0.100   // 100 ms wie Windows

    private weak var ownerPanel: NSPanel?
    private let tooltipPanel = TooltipPanel()
    private var entries: [(view: RoundButton, text: String)] = []
    private var pendingTimer: Timer?
    private var pendingButton: RoundButton?
    private var pendingText: String?

    init(ownerPanel: NSPanel) {
        self.ownerPanel = ownerPanel
    }

    /// Registriert einen Tooltip fuer einen Button. mouseEntered/mouseExited-
    /// Hooks werden gesetzt, alte Hooks werden ueberschrieben.
    func register(_ button: RoundButton, text: String) {
        entries.append((button, text))
        button.onMouseEnteredHook = { [weak self, weak button] in
            guard let self = self, let button = button else { return }
            self.startHover(on: button, text: text)
        }
        button.onMouseExitedHook = { [weak self] in
            self?.cancelHover()
        }
    }

    private func startHover(on button: RoundButton, text: String) {
        cancelHover()
        pendingButton = button
        pendingText = text
        pendingTimer = Timer.scheduledTimer(withTimeInterval: TooltipManager.hoverDelay,
                                            repeats: false) { [weak self] _ in
            self?.fire()
        }
    }

    private func cancelHover() {
        pendingTimer?.invalidate()
        pendingTimer = nil
        tooltipPanel.hide()
        pendingButton = nil
        pendingText = nil
    }

    private func fire() {
        guard let button = pendingButton,
              let text = pendingText,
              let owner = ownerPanel else { return }

        // Button-Frame relativ zum Screen ermitteln
        let buttonFrameInPanel = button.convert(button.bounds, to: nil)  // panel coords
        // Panel-coords → screen-coords
        let panelOrigin = owner.frame.origin
        let buttonScreenY = panelOrigin.y + buttonFrameInPanel.midY

        tooltipPanel.showText(text,
                              leftOf: owner.frame,
                              centeredOnY: buttonScreenY)
    }
}

// MARK: - OverlayPanel
final class OverlayPanel: NSPanel {
    /// Hoehe der vertikalen Saeule (siehe init / applyVerticalLayout).
    /// Zentrale Konstante, damit die andockenden Panels (PromptBoard/PromptInput)
    /// im Horizontalmodus eine sinnvolle Hoehe ableiten koennen — die horizontale
    /// Leiste selbst ist nur 92px hoch.
    static let verticalPanelHeight: CGFloat = 612

    let ultrathinkButton: RoundButton
    let xButton: RoundButton
    let btwButton: RoundButton
    let micButton: RoundButton
    let wButton: RoundButton
    let gButton: RoundButton
    let enterButton: RoundButton
    let copyButton: RoundButton
    let pasteButton: RoundButton
    let screenshotButton: RoundButton
    let insertScreenshotButton: RoundButton

    /// Profile tiles 1-10. Index 0 = Profil 1.
    let profileButtons: [RoundButton]

    private var pulseTimer: Timer?
    private var btwPulseTimer: Timer?
    private var tooltipManager: TooltipManager?

    /// Internal record of which profile is "active". Becomes visible
    /// (goldenrod) only when geminiOn=true. When geminiOn=false all tiles
    /// are dim — just like Windows commit #1961.
    private var activeProfile: Int = 0
    private var geminiOn: Bool = false

    // Right-click drag state
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var dragStartPanelOrigin: NSPoint = .zero
    private var globalRightMouseMonitor: Any?
    private var localRightMouseMonitor: Any?
    private static let positionKey = "terminalOverlayPanelPosition"

    var onUltrathinkClicked: (() -> Void)?
    var onXClicked: (() -> Void)?
    /// Press-and-hold-Callbacks fuer den X-Button: bei Maus-Druck startet onXMouseDown,
    /// bei Loslassen onXMouseUp. Wird von AppDelegate genutzt um in 10ms-Intervallen
    /// Zeilen zu loeschen solange die linke Maustaste gedrueckt ist.
    var onXMouseDown: (() -> Void)?
    var onXMouseUp: (() -> Void)?
    var onBtwClicked: (() -> Void)?
    var onMicClicked: (() -> Void)?
    var onWClicked: (() -> Void)?
    var onGClicked: (() -> Void)?
    var onEnterClicked: (() -> Void)?
    var onCopyClicked: (() -> Void)?
    var onPasteClicked: (() -> Void)?
    var onScreenshotClicked: (() -> Void)?
    var onInsertScreenshotClicked: (() -> Void)?
    /// Linksklick auf eines der zehn Profil-Tiles. Index 1...10.
    /// Der AppDelegate fuehrt damit den Re-Correct mit der letzten
    /// Whisper-Nachricht durch.
    var onProfileClicked: ((Int) -> Void)?
    /// Rechtsklick auf eines der zehn Profil-Tiles. Index 1...10.
    /// Setzt nur das Profil ohne Re-Correct — der Whisper-Cache bleibt
    /// unangetastet, kann spaeter per Linksklick noch genutzt werden.
    /// Hit-Test laeuft im rightMouseDown-Handler unten, BEVOR die Drag-
    /// Logik greift, damit Right-Clicks auf Tiles nicht versehentlich
    /// das Pillar verschieben.
    var onProfileRightClicked: ((Int) -> Void)?
    var onPillarMoved: (() -> Void)?

    init() {
        let btnSize: CGFloat = 40
        let micSize: CGFloat = 52
        let profileWidth: CGFloat = 24
        let profileHeight: CGFloat = 32
        let panelWidth: CGFloat = 96       // war 72 — jetzt mit Profile-Spalte rechts
        // Total panel height bleibt 612 px:
        // S1 (63) + 1 + S2 (124) + 1 + S3 (100) + 1 + S4 (52) + 1
        // + S5 (100) + 1 + S6 (100) + 1 + S7 (63) = 608 + 4 (border) = 612
        let panelHeight = Self.verticalPanelHeight

        // x-Achse Layout (alles in panel-coords, origin links):
        //   Mic-Spalte: x=7..59  (52 px)  → Mic-Mitte = 33
        //   Spacer:     x=59..65 (6 px)
        //   Profile:    x=65..89 (24 px)  → Profile-Mitte = 77
        //   Aussenrand: 7 px links + 7 px rechts
        // Action-Buttons (40 px) zentriert in der Mic-Spalte → x=13..53,
        // Mitte = 33 (gleiche Achse wie Mic).
        let micColumnX: CGFloat = 7
        let micColumnW: CGFloat = 52
        let actionButtonX: CGFloat = micColumnX + (micColumnW - btnSize) / 2  // 13
        // Windows stellt in S1 und S7 je zwei 34er-Buttons nebeneinander:
        // Gesamtbreite 34 + 4 + 34 = 72, zentriert im 92 px breiten Innenraum
        // → erster bei x=12, zweiter bei x=50.
        let sideButtonSize: CGFloat = 34
        let sideButtonX: CGFloat = 12
        let profileX: CGFloat = micColumnX + micColumnW + 6                   // 65

        // Buttons erstellen
        ultrathinkButton = RoundButton(label: "★", color: .toggleOff)
        ultrathinkButton.labelFont = .systemFont(ofSize: 20, weight: .bold)
        ultrathinkButton.labelColor = .starMuted

        xButton = RoundButton(label: "X", color: .btnX)

        btwButton = RoundButton(label: "BTW", color: .btnBtwIdle, width: micSize, height: micSize)
        btwButton.labelFont = .boldSystemFont(ofSize: 11)
        btwButton.labelColor = .darkLabel
        btwButton.useSquareShape = true
        // Windows MicButton-Style: CornerRadius=10 (1:1).
        btwButton.cornerRadius = 10

        micButton = RoundButton(label: "", color: .btnMicIdle, width: micSize, height: micSize)
        // 1:1 Windows-XAML: Mic-Path 22x22 mit Fill #FF1A1A1A
        micButton.symbolImage = IconPaths.renderImage(
            path: IconPaths.mic(), size: NSSize(width: 22, height: 22), fill: NSColor.darkLabel)
        // Windows-Viewbox 22 in 52px → Scale 22/52 ≈ 0.423.
        micButton.symbolScaleFactor = 22.0 / 52.0
        micButton.labelColor = .darkLabel
        micButton.useSquareShape = true
        // Windows MicButton-Style: CornerRadius=10 (1:1).
        micButton.cornerRadius = 10

        wButton = RoundButton(label: "W", color: .btnIdle)
        gButton = RoundButton(label: "G", color: .toggleOff)
        enterButton = RoundButton(label: "\u{23CE}", color: .toggleOff)

        // Windows-XAML CircleButton-Viewbox: 18 in 40 = 0.45
        let actionSymbolScale: CGFloat = 18.0 / 40.0

        copyButton = RoundButton(label: "", color: .btnCopy)
        copyButton.symbolImage = IconPaths.renderImage(
            path: IconPaths.copy(), size: NSSize(width: 18, height: 18), fill: .white)
        copyButton.symbolScaleFactor = actionSymbolScale

        pasteButton = RoundButton(label: "", color: .btnPaste)
        pasteButton.symbolImage = IconPaths.renderImage(
            path: IconPaths.paste(), size: NSSize(width: 18, height: 18), fill: .white)
        pasteButton.symbolScaleFactor = actionSymbolScale

        screenshotButton = RoundButton(label: "", color: .btnScreenshot)
        screenshotButton.symbolImage = IconPaths.renderImage(
            path: IconPaths.screenshot(), size: NSSize(width: 18, height: 18), fill: .white)
        screenshotButton.symbolScaleFactor = actionSymbolScale

        insertScreenshotButton = RoundButton(label: "", color: .btnInsertScreenshot)
        // 1:1 Windows-XAML Segoe MDL2 E723 (Attach/Paperclip) — Material attach_file als NSBezierPath.
        insertScreenshotButton.symbolImage = IconPaths.renderImage(
            path: IconPaths.attach(), size: NSSize(width: 18, height: 18), fill: .white)
        insertScreenshotButton.symbolScaleFactor = actionSymbolScale

        // Profile-Tiles 1...10. Alle 24x32 mit cornerRadius 6.
        var tiles: [RoundButton] = []
        for n in 1...10 {
            let label = "\(n)"
            let tile = RoundButton(label: label, color: .toggleOff,
                                   width: profileWidth, height: profileHeight)
            tile.labelFont = .boldSystemFont(ofSize: n == 10 ? 11 : 13)
            tile.labelColor = .white
            tile.cornerRadius = 6
            tiles.append(tile)
        }
        self.profileButtons = tiles

        // Position auf dem Bildschirm (rechte Kante, nahe oben)
        let screenFrame = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1920, height: 1080)
        var x = screenFrame.maxX - panelWidth - 22
        var y = screenFrame.maxY - panelHeight - 40

        if let savedPosition = UserDefaults.standard.dictionary(forKey: OverlayPanel.positionKey),
           let savedX = savedPosition["x"] as? Double,
           let savedY = savedPosition["y"] as? Double {
            x = CGFloat(savedX)
            y = CGFloat(savedY)
        }

        let contentRect = OverlayPanel.clampFrameToVisibleScreen(
            NSRect(x: x, y: y, width: panelWidth, height: panelHeight))

        super.init(
            contentRect: contentRect,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )

        self.level = .floating
        self.isFloatingPanel = true
        self.becomesKeyOnlyIfNeeded = true
        self.hidesOnDeactivate = false
        self.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        self.isOpaque = false
        self.backgroundColor = .clear
        self.hasShadow = true

        // Outer pill: rounded mask + 2 px black ring (matches Windows #1957).
        // CornerRadius=36 wie Windows (panelWidth/2 ist 48; Windows benutzt
        // 36 als visuelle Konstante — wir ziehen das ebenfalls fest).
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.cornerRadius = 36
        self.contentView?.layer?.masksToBounds = true
        self.contentView?.layer?.backgroundColor = NSColor.clear.cgColor
        // 2 px schwarzer Border-Ring (Windows: BorderBrush #FF000000 BorderThickness 2)
        self.contentView?.layer?.borderColor = NSColor.black.cgColor
        self.contentView?.layer?.borderWidth = 2
        self.contentView?.layer?.borderColor = NSColor.black.cgColor
        self.contentView?.layer?.borderWidth = 2

        // ── 7 colour sections + 6 black 1 px dividers ──
        // Reihenfolge 1:1 wie Windows-XAML (OverlayWindow.xaml), von OBEN gelesen:
        //   S1 Enter + Orientierung, S2 Mic+BTW, S3 W+G, S4 X, S5 Copy+Paste,
        //   S6 Screenshot+Insert, S7 Stern + Diskette.
        // AppKit rechnet von UNTEN, die Liste hier laeuft also rueckwaerts.
        // Frueher lagen Enter und Stern vertauscht — der Auto-Enter sass unten
        // statt ueber dem Mikrofon (Frank 2026-08-27).
        addSection(hex: "#1F1B15", y: 2,   height: 63, width: panelWidth)  // S7 Stern + Diskette
        addDivider(y: 65, width: panelWidth)
        addSection(hex: "#151B15", y: 66,  height: 100, width: panelWidth) // S6 Screenshot+Insert
        addDivider(y: 166, width: panelWidth)
        addSection(hex: "#151B1D", y: 167, height: 100, width: panelWidth) // S5 Copy+Paste
        addDivider(y: 267, width: panelWidth)
        addSection(hex: "#1F1515", y: 268, height: 52,  width: panelWidth) // S4 X
        addDivider(y: 320, width: panelWidth)
        addSection(hex: "#19151F", y: 321, height: 100, width: panelWidth) // S3 W+G
        addDivider(y: 421, width: panelWidth)
        addSection(hex: "#1F1C15", y: 422, height: 124, width: panelWidth) // S2 Mic+BTW
        addDivider(y: 546, width: panelWidth)
        addSection(hex: "#1A1A1A", y: 547, height: 63,  width: panelWidth) // S1 Enter + Orientierung

        // ── Position der Action-Buttons (Mic-Achse links) ──
        // Stern (S1) und Enter (S7) bleiben zentriert, alle anderen rutschen
        // auf die Mic-Achse links.
        // Windows: Enter/Orientierung (S1 oben) und Stern/Diskette (S7 unten) sind
        // 34x34, nicht 40x40 wie die Aktions-Buttons. Padding wie im XAML:
        // S1 = 20 oben / 9 unten, S7 = 8 oben / 21 unten.
        ultrathinkButton.frame       = NSRect(x: sideButtonX, y: 23,  width: sideButtonSize, height: sideButtonSize)
        micButton.frame              = NSRect(x: micColumnX,      y: 488, width: micSize, height: micSize)
        btwButton.frame              = NSRect(x: micColumnX,      y: 428, width: micSize, height: micSize)
        wButton.frame                = NSRect(x: actionButtonX,   y: 375, width: btnSize, height: btnSize)
        gButton.frame                = NSRect(x: actionButtonX,   y: 327, width: btnSize, height: btnSize)
        xButton.frame                = NSRect(x: actionButtonX,   y: 274, width: btnSize, height: btnSize)
        copyButton.frame             = NSRect(x: actionButtonX,   y: 221, width: btnSize, height: btnSize)
        pasteButton.frame            = NSRect(x: actionButtonX,   y: 173, width: btnSize, height: btnSize)
        screenshotButton.frame       = NSRect(x: actionButtonX,   y: 120, width: btnSize, height: btnSize)
        insertScreenshotButton.frame = NSRect(x: actionButtonX,   y: 72,  width: btnSize, height: btnSize)
        enterButton.frame            = NSRect(x: sideButtonX, y: 556, width: sideButtonSize, height: sideButtonSize)

        // ── Position der Profile-Tiles (rechte Spalte) ──
        // Verteilung passt 1:1 zur Windows-Variante (#1957):
        //   S2 (Mic+BTW): Profil 1, 2, 3
        //   S3 (W+G):     Profil 4, 5
        //   S4 (X):       Profil 6
        //   S5 (Copy+P):  Profil 7, 8
        //   S6 (Shot+In): Profil 9, 10
        // Tile-Hoehe 32 px, Spacing 8 px, ergeben Stack-Hoehen die gut
        // in die jeweilige Sektion passen und vertikal mittig zentriert
        // sind (gleiche Optik wie Windows).
        let placements: [(profileIndex: Int, y: CGFloat)] = computeProfilePositions()
        for (idx, y) in placements {
            let tile = profileButtons[idx - 1]
            tile.frame = NSRect(x: profileX, y: y, width: profileWidth, height: profileHeight)
        }

        // Buttons als Subviews hinzufuegen (Z-Order = Insertion-Order)
        let allButtons: [NSView] = [
            ultrathinkButton, micButton, btwButton,
            wButton, gButton, xButton,
            copyButton, pasteButton,
            screenshotButton, insertScreenshotButton,
            enterButton
        ]
        for btn in allButtons { self.contentView?.addSubview(btn) }
        for tile in profileButtons { self.contentView?.addSubview(tile) }

        ultrathinkButton.onClick       = { [weak self] in self?.onUltrathinkClicked?() }
        // Press-and-hold-Logik: onMouseDown startet die Loesch-Schleife, onMouseUp stoppt sie.
        // Wenn onMouseDown gesetzt ist, ruft RoundButton intern KEIN onClick mehr auf — die
        // ganze Logik laeuft ueber onXMouseDown/onXMouseUp im AppDelegate. onXClicked bleibt
        // als Property fuer Backward-Compat erhalten, wird aber nicht mehr getriggert.
        xButton.onMouseDown            = { [weak self] in self?.onXMouseDown?() }
        xButton.onMouseUp              = { [weak self] in self?.onXMouseUp?() }
        btwButton.onClick              = { [weak self] in self?.onBtwClicked?() }
        micButton.onClick              = { [weak self] in self?.onMicClicked?() }
        wButton.onClick                = { [weak self] in self?.onWClicked?() }
        gButton.onClick                = { [weak self] in self?.onGClicked?() }
        enterButton.onClick            = { [weak self] in self?.onEnterClicked?() }
        copyButton.onClick             = { [weak self] in self?.onCopyClicked?() }
        pasteButton.onClick            = { [weak self] in self?.onPasteClicked?() }
        screenshotButton.onClick       = { [weak self] in self?.onScreenshotClicked?() }
        insertScreenshotButton.onClick = { [weak self] in self?.onInsertScreenshotClicked?() }
        for n in 1...10 {
            profileButtons[n - 1].onClick = { [weak self] in self?.onProfileClicked?(n) }
        }

        setupTooltips()
        setupDragMonitors()
    }

    // Berechnet die y-Koordinaten der 10 Profile-Tiles in panel-coords.
    private func computeProfilePositions() -> [(profileIndex: Int, y: CGFloat)] {
        // Sektion Heights / y-Origin (panel-coords, bottom-up):
        // S2 (Mic+BTW): y=422, h=124  → Tile-Stack 32+8+32+8+32 = 112 → centered: padding 6 oben+unten → y=428,468,508
        // S3 (W+G):     y=321, h=100  → Stack 32+8+32 = 72 → centered: padding 14 → y=335,375
        // S4 (X):       y=268, h=52   → Stack 32 → centered: padding 10 → y=278
        // S5 (Copy+P):  y=167, h=100  → Stack 72 → y=181,221
        // S6 (Shot+In): y=66,  h=100  → Stack 72 → y=80,120
        return [
            (1, 508), (2, 468), (3, 428),    // S2 Mic+BTW (von oben nach unten)
            (4, 375), (5, 335),               // S3 W+G
            (6, 278),                          // S4 X
            (7, 221), (8, 181),                // S5 Copy+P
            (9, 120), (10, 80)                 // S6 Shot+Insert
        ]
    }

    // MARK: - Section helpers

    private func addSection(hex: String, y: CGFloat, height: CGFloat, width: CGFloat) {
        let v = NSView(frame: NSRect(x: 0, y: y, width: width, height: height))
        v.wantsLayer = true
        v.layer?.backgroundColor = NSColor(hex: hex).cgColor
        self.contentView?.addSubview(v)
    }

    private func addDivider(y: CGFloat, width: CGFloat) {
        let v = NSView(frame: NSRect(x: 0, y: y, width: width, height: 1))
        v.wantsLayer = true
        v.layer?.backgroundColor = NSColor.black.cgColor
        self.contentView?.addSubview(v)
    }

    // MARK: - Tooltips

    /// Registriert alle Tooltip-Texte (gleiche kurze Texte wie Windows
    /// #1960, #1962, #1964). Profile 1-3 haben semantische Namen, 4-10
    /// "frei belegbar" bis Frank sie inhaltlich fuellt.
    private func setupTooltips() {
        let mgr = TooltipManager(ownerPanel: self)
        self.tooltipManager = mgr

        mgr.register(ultrathinkButton, text: "Promptboard")
        mgr.register(micButton,        text: "Aufnahme")
        mgr.register(btwButton,        text: "Zwischenfrage")
        mgr.register(wButton,          text: "Whisper")
        mgr.register(gButton,          text: "Gemini")
        mgr.register(xButton,          text: "Zeile löschen")
        mgr.register(copyButton,       text: "Text kopieren")
        mgr.register(pasteButton,      text: "Text einfügen")
        mgr.register(screenshotButton, text: "Screenshot")
        mgr.register(insertScreenshotButton, text: "Screenshots einfügen")
        mgr.register(enterButton,      text: "Auto-Enter")

        mgr.register(profileButtons[0], text: "Standard")
        mgr.register(profileButtons[1], text: "Programmierung")
        mgr.register(profileButtons[2], text: "Meta-Intelligenz")
        for n in 4...10 {
            mgr.register(profileButtons[n - 1], text: "Profil \(n) (frei belegbar)")
        }
    }

    // MARK: - State Updates

    /// Lazy WaveformView, ueber dem Mic-Button gezeichnet (sichtbar nur
    /// waehrend Recording). Wird in setMicState/.recording sichtbar gemacht.
    private var waveformView: WaveformView? {
        get { objc_getAssociatedObject(self, &waveformKey) as? WaveformView }
        set { objc_setAssociatedObject(self, &waveformKey, newValue,
                                       .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }

    /// Wird vom AudioRecorder ueber AppDelegate aufgerufen — schiebt einen
    /// neuen Peak-Pegelwert in die 14-stellige FIFO-Reihe.
    func pushWaveformLevel(_ value: Float) {
        DispatchQueue.main.async { [weak self] in
            self?.ensureWaveformView()
            self?.waveformView?.pushLevel(value)
        }
    }

    private func ensureWaveformView() {
        if waveformView != nil { return }
        let w = WaveformView(frame: micButton.bounds)
        w.autoresizingMask = [.width, .height]
        micButton.addSubview(w)
        waveformView = w
    }

    func showWaveform(_ show: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.ensureWaveformView()
            // Windows SetWaveformVisible: beim Einschalten wird der Buffer
            // geleert, damit nicht die Welle des letzten Diktats stehenbleibt.
            if show { self.waveformView?.clear() }
            self.waveformView?.isHidden = !show
            if !show { self.waveformView?.clear() }
            // Mic-Icon und Welle schliessen sich gegenseitig aus (1:1 Windows).
            self.micButton.symbolHidden = show
        }
    }

    func setMicState(_ state: MicState) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            switch state {
            case .idle:
                self.stopPulse()
                self.micButton.buttonColor = .btnMicIdle
                self.micButton.labelColor = .darkLabel
                self.showWaveform(false)
            case .recording:
                self.micButton.buttonColor = .btnRecording
                self.micButton.labelColor = .white
                self.startPulse()
                self.showWaveform(true)
            case .processing:
                self.stopPulse()
                self.micButton.buttonColor = .btnProcessing
                self.micButton.labelColor = .white
                self.showWaveform(false)
            case .success:
                self.stopPulse()
                self.micButton.buttonColor = .btnSuccess
                self.micButton.labelColor = .white
                self.showWaveform(false)
            case .error:
                self.stopPulse()
                self.micButton.buttonColor = .btnRecording
                self.micButton.labelColor = .white
                self.showWaveform(false)
            }
        }
    }

    func setBtwMicState(_ state: MicState) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            switch state {
            case .idle:
                self.stopBtwPulse()
                self.btwButton.buttonColor = .btnBtwIdle
                self.btwButton.labelColor = .darkLabel
            case .recording:
                // BTW Recording = #F57F17 (dunkles Gelb-Orange) wie Windows,
                // NICHT btnRecording (#C62828 Rot — das ist die Mic-Farbe).
                self.btwButton.buttonColor = .btnBtwRecording
                self.btwButton.labelColor = .darkLabel
                self.startBtwPulse()
            case .processing:
                self.stopBtwPulse()
                self.btwButton.buttonColor = .btnProcessing
                self.btwButton.labelColor = .white
            case .success:
                self.stopBtwPulse()
                self.btwButton.buttonColor = .btnSuccess
                self.btwButton.labelColor = .white
            case .error:
                self.stopBtwPulse()
                self.btwButton.buttonColor = .btnRecording
                self.btwButton.labelColor = .white
            }
        }
    }

    func setGeminiEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.gButton.buttonColor = enabled ? .toggleOn : .toggleOff
            self.wButton.buttonColor = enabled ? .toggleOff : .toggleOn
            self.geminiOn = enabled
            self.refreshProfileTiles()
        }
    }

    /// Setzt das aktive Gemini-Korrektur-Profil (1...10) und aktualisiert
    /// die Profile-Tiles. Wenn Gemini aus ist, bleiben ALLE Tiles dunkel —
    /// erst beim Wieder-Aktivieren leuchtet das gespeicherte Profil.
    func setActiveProfile(_ profile: Int) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.activeProfile = profile
            self.refreshProfileTiles()
        }
    }

    private func refreshProfileTiles() {
        let showActive = geminiOn
        for (idx, tile) in profileButtons.enumerated() {
            let isActive = showActive && (idx + 1) == activeProfile
            // Aktives Profile = goldenrod (Windows starGold #DAA520), inaktiv
            // = dunkelgrau (#2D2D2D). Label-Farbe dunkel auf gold, weiss auf grau.
            tile.buttonColor = isActive ? .starGold : .toggleOff
            tile.labelColor = isActive ? .darkLabel : .white
        }
    }

    /// Matches the Windows Voice Overlay behavior 1:1: every click fires
    /// ClearLine immediately. The button flashes gray for 2s but further
    /// clicks are NOT blocked during that window.
    func flashXButton() {
        DispatchQueue.main.async { [weak self] in
            self?.xButton.buttonColor = .btnIdle
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
                self?.xButton.buttonColor = .btnX
            }
        }
    }

    func flashCopyButton() {
        copyButton.buttonColor = .btnIdle
        copyButton.needsDisplay = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self else { return }
            self.copyButton.buttonColor = .btnCopy
            self.copyButton.needsDisplay = true
        }
    }

    func flashPasteButton() {
        pasteButton.buttonColor = .btnIdle
        pasteButton.needsDisplay = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self else { return }
            self.pasteButton.buttonColor = .btnPaste
            self.pasteButton.needsDisplay = true
        }
    }

    func flashScreenshotButton(success: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.screenshotButton.buttonColor = success ? .btnSuccess : .btnX
            self.screenshotButton.needsDisplay = true
            let delay: TimeInterval = success ? 1.5 : 3.0
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
                guard let self = self else { return }
                self.screenshotButton.buttonColor = .btnScreenshot
                self.screenshotButton.needsDisplay = true
            }
        }
    }

    func flashInsertScreenshotButton(success: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.insertScreenshotButton.buttonColor = success ? .btnSuccess : .btnX
            self.insertScreenshotButton.needsDisplay = true
            let delay: TimeInterval = success ? 1.5 : 3.0
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
                guard let self = self else { return }
                self.insertScreenshotButton.buttonColor = .btnInsertScreenshot
                self.insertScreenshotButton.needsDisplay = true
            }
        }
    }

    /// Visueller Indikator waehrend Re-Correct: das geklickte Tile wird
    /// kurz orange (Processing). Danach wieder zurueck auf goldgelb/dunkel.
    func flashProfileTile(_ profile: Int, processing: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self,
                  profile >= 1, profile <= self.profileButtons.count else { return }
            let tile = self.profileButtons[profile - 1]
            if processing {
                tile.buttonColor = .btnProcessing
            } else {
                self.refreshProfileTiles()
            }
        }
    }

    func setAutoEnterEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.enterButton.buttonColor = enabled ? .btnProcessing : .toggleOff
        }
    }

    func setUltrathinkEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.ultrathinkButton.buttonColor = enabled ? .ultrathinkOn : .toggleOff
            self.ultrathinkButton.labelColor = enabled ? .starGold : .starMuted
        }
    }

    // MARK: - Mic Pulse Animation

    private func startPulse() {
        stopPulse()
        var bright = false
        pulseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            bright.toggle()
            self.micButton.buttonColor = bright ? .btnRecordingBright : .btnRecording
        }
    }

    private func stopPulse() {
        pulseTimer?.invalidate()
        pulseTimer = nil
    }

    private func startBtwPulse() {
        stopBtwPulse()
        var bright = false
        btwPulseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            bright.toggle()
            // 1:1 Windows (_btwPulseTimer): der BTW-Knopf pulsiert GELB
            // (#FFEB3B <-> #F57F17), nicht rot. Rot ist die Farbe des grossen
            // Mikrofons — mit der roten Variante sahen beide Aufnahmearten
            // gleich aus, obwohl Windows sie klar unterscheidet.
            self.btwButton.buttonColor = bright ? .btnBtwPulse : .btnBtwRecording
            self.btwButton.labelColor = .darkLabel
        }
    }

    private func stopBtwPulse() {
        btwPulseTimer?.invalidate()
        btwPulseTimer = nil
    }

    // MARK: - Right-click drag to reposition

    private func setupDragMonitors() {
        globalRightMouseMonitor = NSEvent.addGlobalMonitorForEvents(
            matching: [.rightMouseDown, .rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            self?.handleDragEvent(event)
        }

        localRightMouseMonitor = NSEvent.addLocalMonitorForEvents(
            matching: [.rightMouseDown, .rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            guard let self = self else { return event }
            let consumed = self.handleDragEvent(event)
            return consumed ? nil : event
        }
    }

    @discardableResult
    private func handleDragEvent(_ event: NSEvent) -> Bool {
        let mouseLocation = NSEvent.mouseLocation

        switch event.type {
        case .rightMouseDown:
            let clickedWindowNumber = NSWindow.windowNumber(at: mouseLocation, belowWindowWithWindowNumber: 0)
            if frame.contains(mouseLocation) && clickedWindowNumber == self.windowNumber {
                // Hit-Test auf Profil-Tiles: liegt der Klick auf einem Tile,
                // wird er als Right-Click auf das Profil interpretiert (nur
                // Profil-Wechsel ohne Re-Correct) — NICHT als Drag-Start.
                let panelPoint = NSPoint(
                    x: mouseLocation.x - frame.origin.x,
                    y: mouseLocation.y - frame.origin.y
                )
                for (idx, tile) in profileButtons.enumerated() where tile.frame.contains(panelPoint) {
                    onProfileRightClicked?(idx + 1)
                    return true
                }
                isDragging = true
                dragStartMouseLocation = mouseLocation
                dragStartPanelOrigin = frame.origin
                return true
            }
        case .rightMouseDragged:
            guard isDragging else { return false }
            let dx = mouseLocation.x - dragStartMouseLocation.x
            let dy = mouseLocation.y - dragStartMouseLocation.y
            // Drag-Threshold (Pendant zu Windows DragThresholdPx = 4):
            // Erst ab 4 px Strecke wird tatsaechlich verschoben. Verhindert
            // dass ein kurzer Right-Click versehentlich das Pillar verschiebt.
            let distSq = dx * dx + dy * dy
            if distSq < 16 { return true }
            setFrameOrigin(NSPoint(x: dragStartPanelOrigin.x + dx, y: dragStartPanelOrigin.y + dy))
            onPillarMoved?()
            return true
        case .rightMouseUp:
            if isDragging {
                isDragging = false
                savePosition()
                onPillarMoved?()
                return true
            }
        default:
            break
        }
        return false
    }

    private func savePosition() {
        savePillarPosition()
    }

    func savePillarPosition() {
        let position: [String: Double] = ["x": Double(frame.origin.x), "y": Double(frame.origin.y)]
        UserDefaults.standard.set(position, forKey: OverlayPanel.positionKey)
    }

    /// Klemmt ein Frame so, dass es VOLLSTAENDIG im sichtbaren Bereich liegt.
    /// Verhindert dass das Overlay rechts/unten aus dem Monitor wandert —
    /// Root Cause des "verschwindet rechts vom Monitor"-Bugs (2026-06-05):
    /// eine im schmalen Vertikal-Modus (96pt) gespeicherte Position klebte am
    /// rechten Rand; im breiten Horizontal-Modus ragte das Overlay dann zu 2/3
    /// raus und die eingeklappte Mic-Pille verschwand ganz. Funktionserhaltend:
    /// es wird NUR die Position verschoben, NIE die Groesse geaendert (Layout
    /// bleibt intakt). Waehlt den Bildschirm mit dem groessten Ueberlapp,
    /// faellt auf den Hauptbildschirm zurueck. Static, damit auch der Init
    /// (vor super.init) ihn nutzen kann.
    static func clampFrameToVisibleScreen(_ frame: NSRect) -> NSRect {
        let screen: NSScreen? = NSScreen.screens.max(by: { a, b in
            let ia = a.frame.intersection(frame)
            let ib = b.frame.intersection(frame)
            return (ia.width * ia.height) < (ib.width * ib.height)
        }) ?? NSScreen.main
        guard let visible = screen?.visibleFrame else { return frame }
        var f = frame
        if f.maxX > visible.maxX { f.origin.x = visible.maxX - f.width }
        if f.maxY > visible.maxY { f.origin.y = visible.maxY - f.height }
        if f.origin.x < visible.minX { f.origin.x = visible.minX }
        if f.origin.y < visible.minY { f.origin.y = visible.minY }
        return f
    }

    deinit {
        if let monitor = globalRightMouseMonitor { NSEvent.removeMonitor(monitor) }
        if let monitor = localRightMouseMonitor { NSEvent.removeMonitor(monitor) }
    }

    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }

    enum MicState {
        case idle, recording, processing, success, error
    }
}

/// Associated-Object-Key fuer die Lazy-Initialisierte WaveformView, die im
/// Mic-Button ueberlagert wird waehrend Recording.
private var waveformKey: UInt8 = 0

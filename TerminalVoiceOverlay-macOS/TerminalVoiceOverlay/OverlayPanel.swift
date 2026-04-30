import AppKit

// MARK: - Colors
// Tuned to match the Windows TerminalVoiceOverlay 1:1 after commits
// #1914..#1919: Material 700/800 dark-theme tones, yellow Mic family,
// teal Screenshot/Insert pair, blue Copy/Paste pair, and the 7-section
// dark panel with black dividers and a black 2px outer ring.
private extension NSColor {
    // Idle/base
    static let btnIdle       = NSColor(hex: "#2D2D2D")
    static let btnRecording  = NSColor(hex: "#C62828")  // Red 700  (was #E53935)
    static let btnProcessing = NSColor(hex: "#EF6C00")  // Orange 800 (was #FF9800)
    static let btnSuccess    = NSColor(hex: "#2E7D32")  // Green 700  (was #43A047)
    // Toggles
    static let toggleOn       = NSColor(hex: "#2E7D32") // Green 700  (was #16a34a)
    static let toggleOnBright = NSColor(hex: "#43A047") // brighter green for transient flashes
    static let toggleOff      = NSColor(hex: "#2D2D2D")
    // Mic + BTW — Yellow family (warm cluster joining the gold star).
    // Requires dark labelColor for legibility on yellow surface.
    static let btnBtwIdle      = NSColor(hex: "#FBC02D") // Yellow 700
    static let btnBtwRecording = NSColor(hex: "#F57F17") // Yellow 900
    static let btnBtwPulse     = NSColor(hex: "#FFEB3B") // Yellow 500
    static let btnMicIdle      = NSColor(hex: "#F9A825") // Yellow 800
    // Special
    static let btnX        = NSColor(hex: "#C62828")     // Red 700
    static let btnXPressed = NSColor(hex: "#E53935")
    // Copy/Paste — pair in Light Blue family
    static let btnCopy  = NSColor(hex: "#0288D1")        // Light Blue 700
    static let btnPaste = NSColor(hex: "#0277BD")        // Light Blue 800
    // Screenshot/Insert — pair in Teal family (NEW — ports the Windows
    // ScreenshotButton + InsertScreenshotButton from #1912..#1917).
    static let btnScreenshot       = NSColor(hex: "#00796B") // Teal 700
    static let btnInsertScreenshot = NSColor(hex: "#00897B") // Teal 600
    // Star (always-on prefix)
    static let ultrathinkOn = NSColor(hex: "#9E7B0E")    // antique gold (was #B8860B)
    static let starGold     = NSColor(hex: "#DAA520")    // goldenrod    (was #FFD700)
    static let starMuted    = NSColor(hex: "#8B7355")
    // Pulse for main mic recording
    static let btnRecordingBright = NSColor(hex: "#FF5252") // was #FF6666

    // Dark label / icon tint for the yellow Mic/BTW buttons. White on
    // yellow has ~1.7:1 contrast (illegible); dark gives ~12:1.
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
    let diameter: CGFloat = 40
    var buttonColor: NSColor = .btnIdle { didSet { needsDisplay = true } }
    var label: String = "" { didSet { needsDisplay = true } }
    var labelFont: NSFont = .systemFont(ofSize: 16, weight: .bold)
    var labelColor: NSColor = .white { didSet { needsDisplay = true } }
    var symbolImage: NSImage?
    var useSquareShape: Bool = false
    var onClick: (() -> Void)?
    var onMouseDown: (() -> Void)?
    var onMouseUp: (() -> Void)?

    init(label: String, color: NSColor) {
        self.label = label
        self.buttonColor = color
        super.init(frame: NSRect(x: 0, y: 0, width: diameter, height: diameter))
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
        let path = useSquareShape
            ? NSBezierPath(roundedRect: inset, xRadius: 6, yRadius: 6)
            : NSBezierPath(ovalIn: inset)
        buttonColor.setFill()
        path.fill()

        if let img = symbolImage {
            let imgSize = CGSize(width: bounds.width * 0.5, height: bounds.height * 0.5)
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
        } else {
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
    }

    override func mouseExited(with event: NSEvent) {
        animateScale(1.0)
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
        anim.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
        layer?.add(anim, forKey: "hoverScale")
    }
}

// MARK: - OverlayPanel
final class OverlayPanel: NSPanel {
    let ultrathinkButton: RoundButton
    let xButton: RoundButton
    let btwButton: RoundButton
    let micButton: RoundButton
    let wButton: RoundButton
    let gButton: RoundButton
    let enterButton: RoundButton
    let copyButton: RoundButton
    let pasteButton: RoundButton
    /// Capture full screen via /usr/sbin/screencapture, save to
    /// ~/Pictures/Screenshots/. Wired up in AppDelegate.
    let screenshotButton: RoundButton
    /// Paste path of the last captured screenshot into the active terminal.
    let insertScreenshotButton: RoundButton

    private var pulseTimer: Timer?
    private var btwPulseTimer: Timer?

    // Right-click drag state
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var dragStartPanelOrigin: NSPoint = .zero
    private var globalRightMouseMonitor: Any?
    private var localRightMouseMonitor: Any?
    private static let positionKey = "terminalOverlayPanelPosition"

    var onUltrathinkClicked: (() -> Void)?
    var onXClicked: (() -> Void)?
    var onBtwClicked: (() -> Void)?
    var onMicClicked: (() -> Void)?
    var onWClicked: (() -> Void)?
    var onGClicked: (() -> Void)?
    var onEnterClicked: (() -> Void)?
    var onCopyClicked: (() -> Void)?
    var onPasteClicked: (() -> Void)?
    var onScreenshotClicked: (() -> Void)?
    var onInsertScreenshotClicked: (() -> Void)?
    /// Fired on every right-click drag step so anything docked to this
    /// panel (the PromptBoard side panel) can keep up. Also fired on
    /// drag-end so the final position is in sync.
    var onPillarMoved: (() -> Void)?

    init() {
        let btnSize: CGFloat = 40
        let micSize: CGFloat = 52
        let panelWidth: CGFloat = 72
        // Total panel height: 7 sections + 6 dividers + 2*2 outer border.
        // S1 (Star, 63) + 1 + S2 (Mic+BTW, 124) + 1 + S3 (W+G, 100) + 1
        // + S4 (X, 52) + 1 + S5 (Copy+Paste, 100) + 1 + S6 (Screenshot+
        // Insert, 100) + 1 + S7 (Enter, 63) + 4 (border) = 612.
        let panelHeight: CGFloat = 612

        // Create buttons
        ultrathinkButton = RoundButton(label: "★", color: .toggleOff)
        ultrathinkButton.labelFont = .systemFont(ofSize: 20, weight: .bold)
        ultrathinkButton.labelColor = .starMuted

        xButton = RoundButton(label: "X", color: .btnX)

        btwButton = RoundButton(label: "BTW", color: .btnBtwIdle)
        btwButton.labelFont = .boldSystemFont(ofSize: 11)
        btwButton.labelColor = .darkLabel       // dark text on yellow
        btwButton.useSquareShape = true

        micButton = RoundButton(label: "", color: .btnMicIdle)
        micButton.symbolImage = NSImage(systemSymbolName: "mic.fill", accessibilityDescription: "Microphone")
        micButton.labelColor = .darkLabel       // dark icon tint on yellow
        micButton.useSquareShape = true

        wButton = RoundButton(label: "W", color: .btnIdle)
        gButton = RoundButton(label: "G", color: .toggleOff)
        enterButton = RoundButton(label: "\u{23CE}", color: .toggleOff)

        copyButton = RoundButton(label: "", color: .btnCopy)
        copyButton.symbolImage = NSImage(systemSymbolName: "doc.on.doc", accessibilityDescription: "Copy")

        pasteButton = RoundButton(label: "", color: .btnPaste)
        pasteButton.symbolImage = NSImage(systemSymbolName: "doc.on.clipboard", accessibilityDescription: "Paste")

        screenshotButton = RoundButton(label: "", color: .btnScreenshot)
        screenshotButton.symbolImage = NSImage(systemSymbolName: "camera.fill", accessibilityDescription: "Screenshot")

        insertScreenshotButton = RoundButton(label: "", color: .btnInsertScreenshot)
        insertScreenshotButton.symbolImage = NSImage(systemSymbolName: "paperclip", accessibilityDescription: "Insert screenshot path")

        // Calculate screen position (right edge, vertically near top).
        // X offset 22 px from right edge.
        // Y offset 40 px from top.
        // Saved position via dragging still wins below; this is just
        // the first-launch fallback.
        let screenFrame = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1920, height: 1080)
        var x = screenFrame.maxX - panelWidth - 22
        var y = screenFrame.maxY - panelHeight - 40

        // Restore saved position if available
        if let savedPosition = UserDefaults.standard.dictionary(forKey: OverlayPanel.positionKey),
           let savedX = savedPosition["x"] as? Double,
           let savedY = savedPosition["y"] as? Double {
            x = CGFloat(savedX)
            y = CGFloat(savedY)
        }

        let contentRect = NSRect(x: x, y: y, width: panelWidth, height: panelHeight)

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

        // Outer pill: rounded mask + 2 px black ring (matches Windows
        // #1915). The contentView itself is transparent — the section
        // sub-views below paint their own opaque backgrounds, so the
        // panel reads as a coherent dark surface with a clean black
        // frame instead of a translucent grey pill.
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.cornerRadius = panelWidth / 2  // 36
        self.contentView?.layer?.masksToBounds = true
        self.contentView?.layer?.backgroundColor = NSColor.clear.cgColor
        self.contentView?.layer?.borderColor = NSColor.black.cgColor
        self.contentView?.layer?.borderWidth = 2

        // ── 7 colour sections + 6 black 1 px dividers ──
        // y-coordinates use AppKit (origin at panel bottom). The section
        // identity colours are very dim near-black tints (channel range
        // 0x14..0x1F) so the panel reads as one cohesive dark surface
        // and the colour signal lives on the buttons.
        // Layout — bottom-up so the visual order is, top→bottom:
        //   ★ → Mic → BTW → W → G → X → Copy → Paste → Screenshot → Insert → Enter
        addSection(hex: "#1A1A1A", y: 2,   height: 63, width: panelWidth)  // S7 Enter (neutral)
        addDivider(y: 65, width: panelWidth)
        addSection(hex: "#151B15", y: 66,  height: 100, width: panelWidth) // S6 Screenshot+Insert (cool green)
        addDivider(y: 166, width: panelWidth)
        addSection(hex: "#151B1D", y: 167, height: 100, width: panelWidth) // S5 Copy+Paste (cool teal)
        addDivider(y: 267, width: panelWidth)
        addSection(hex: "#1F1515", y: 268, height: 52,  width: panelWidth) // S4 X (warm red)
        addDivider(y: 320, width: panelWidth)
        addSection(hex: "#19151F", y: 321, height: 100, width: panelWidth) // S3 W+G (cool violet)
        addDivider(y: 421, width: panelWidth)
        addSection(hex: "#1F1C15", y: 422, height: 124, width: panelWidth) // S2 Mic+BTW (warm yellow)
        addDivider(y: 546, width: panelWidth)
        addSection(hex: "#1F1B15", y: 547, height: 63,  width: panelWidth) // S1 Star (warm amber)

        // ── Position buttons ──
        // y-coordinates derived from each section's y + WPF-bottom-padding.
        // Section S1 (y=547) padding "0,17,0,6" -> button y in section 6   -> panel y 553
        // Section S2 (y=422) padding "0,6,0,6"  -> BTW y 6, Mic y 66       -> panel BTW 428, Mic 488
        // Section S3 (y=321)                    -> G y 6, W y 54           -> panel G 327,  W 375
        // Section S4 (y=268)                    -> X y 6                   -> panel X 274
        // Section S5 (y=167)                    -> Paste y 6, Copy y 54    -> panel Paste 173, Copy 221
        // Section S6 (y=66)                     -> Insert y 6, Shot y 54   -> panel Insert 72, Screenshot 120
        // Section S7 (y=2)   padding "0,6,0,17" -> button y in section 17  -> panel y 19
        let smallInset = (panelWidth - btnSize) / 2  // 16
        let micInset   = (panelWidth - micSize) / 2  // 10

        ultrathinkButton.frame       = NSRect(x: smallInset, y: 553, width: btnSize, height: btnSize)
        micButton.frame              = NSRect(x: micInset,   y: 488, width: micSize, height: micSize)
        btwButton.frame              = NSRect(x: micInset,   y: 428, width: micSize, height: micSize)
        wButton.frame                = NSRect(x: smallInset, y: 375, width: btnSize, height: btnSize)
        gButton.frame                = NSRect(x: smallInset, y: 327, width: btnSize, height: btnSize)
        xButton.frame                = NSRect(x: smallInset, y: 274, width: btnSize, height: btnSize)
        copyButton.frame             = NSRect(x: smallInset, y: 221, width: btnSize, height: btnSize)
        pasteButton.frame            = NSRect(x: smallInset, y: 173, width: btnSize, height: btnSize)
        screenshotButton.frame       = NSRect(x: smallInset, y: 120, width: btnSize, height: btnSize)
        insertScreenshotButton.frame = NSRect(x: smallInset, y: 72,  width: btnSize, height: btnSize)
        enterButton.frame            = NSRect(x: smallInset, y: 19,  width: btnSize, height: btnSize)

        // Add buttons after sections so they layer ON TOP of the
        // section backgrounds (subview Z-order = insertion order).
        self.contentView?.addSubview(ultrathinkButton)
        self.contentView?.addSubview(micButton)
        self.contentView?.addSubview(btwButton)
        self.contentView?.addSubview(wButton)
        self.contentView?.addSubview(gButton)
        self.contentView?.addSubview(xButton)
        self.contentView?.addSubview(copyButton)
        self.contentView?.addSubview(pasteButton)
        self.contentView?.addSubview(screenshotButton)
        self.contentView?.addSubview(insertScreenshotButton)
        self.contentView?.addSubview(enterButton)

        ultrathinkButton.onClick       = { [weak self] in self?.onUltrathinkClicked?() }
        xButton.onClick                = { [weak self] in self?.onXClicked?() }
        btwButton.onClick              = { [weak self] in self?.onBtwClicked?() }
        micButton.onClick              = { [weak self] in self?.onMicClicked?() }
        wButton.onClick                = { [weak self] in self?.onWClicked?() }
        gButton.onClick                = { [weak self] in self?.onGClicked?() }
        enterButton.onClick            = { [weak self] in self?.onEnterClicked?() }
        copyButton.onClick             = { [weak self] in self?.onCopyClicked?() }
        pasteButton.onClick            = { [weak self] in self?.onPasteClicked?() }
        screenshotButton.onClick       = { [weak self] in self?.onScreenshotClicked?() }
        insertScreenshotButton.onClick = { [weak self] in self?.onInsertScreenshotClicked?() }

        setupDragMonitors()
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

    // MARK: - State Updates

    func setMicState(_ state: MicState) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            switch state {
            case .idle:
                self.stopPulse()
                self.micButton.buttonColor = .btnMicIdle
                self.micButton.labelColor = .darkLabel   // dark on yellow
            case .recording:
                self.micButton.buttonColor = .btnRecording
                self.micButton.labelColor = .white       // white on red
                self.startPulse()
            case .processing:
                self.stopPulse()
                self.micButton.buttonColor = .btnProcessing
                self.micButton.labelColor = .white
            case .success:
                self.stopPulse()
                self.micButton.buttonColor = .btnSuccess
                self.micButton.labelColor = .white
            case .error:
                self.stopPulse()
                self.micButton.buttonColor = .btnRecording
                self.micButton.labelColor = .white
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
                self.btwButton.labelColor = .darkLabel   // dark on yellow
            case .recording:
                self.btwButton.buttonColor = .btnRecording
                self.btwButton.labelColor = .white       // white on red
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
            self?.gButton.buttonColor = enabled ? .toggleOn : .toggleOff
            self?.wButton.buttonColor = enabled ? .toggleOff : .toggleOn
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

    /// Flash the Screenshot button green on success / red on failure,
    /// then fade back to teal. Mirrors the Windows BtnScreenshot_Click
    /// success/error flash pattern (1.5 s success, 3 s failure).
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

    /// Flash the Insert button green on success / red on failure (e.g.
    /// no screenshot taken yet, or the file went missing). Mirrors the
    /// Windows BtnInsertScreenshot_Click flash pattern.
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

    // MARK: - BTW Pulse Animation

    private func startBtwPulse() {
        stopBtwPulse()
        var bright = false
        btwPulseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            bright.toggle()
            self.btwButton.buttonColor = bright ? .btnRecordingBright : .btnRecording
        }
    }

    private func stopBtwPulse() {
        btwPulseTimer?.invalidate()
        btwPulseTimer = nil
    }

    // MARK: - Right-click drag to reposition

    private func setupDragMonitors() {
        // Global monitor: catches right-mouse events even when another app is frontmost
        globalRightMouseMonitor = NSEvent.addGlobalMonitorForEvents(
            matching: [.rightMouseDown, .rightMouseDragged, .rightMouseUp]
        ) { [weak self] event in
            self?.handleDragEvent(event)
        }

        // Local monitor: catches right-mouse events when our app is frontmost
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
                isDragging = true
                dragStartMouseLocation = mouseLocation
                dragStartPanelOrigin = frame.origin
                return true
            }
        case .rightMouseDragged:
            guard isDragging else { return false }
            let dx = mouseLocation.x - dragStartMouseLocation.x
            let dy = mouseLocation.y - dragStartMouseLocation.y
            setFrameOrigin(NSPoint(x: dragStartPanelOrigin.x + dx, y: dragStartPanelOrigin.y + dy))
            // Re-dock the PromptBoard live during drag so it stays
            // glued to the pillar instead of being left behind at its
            // initial position.
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

    /// Public so AppDelegate can call it after dragging the pillar via
    /// the PromptBoard panel (right-click drag inside the panel moves
    /// both windows together; the pillar's own drag handler isn't the
    /// one driving that motion, so we expose the save explicitly).
    func savePillarPosition() {
        let position: [String: Double] = ["x": Double(frame.origin.x), "y": Double(frame.origin.y)]
        UserDefaults.standard.set(position, forKey: OverlayPanel.positionKey)
    }

    deinit {
        if let monitor = globalRightMouseMonitor { NSEvent.removeMonitor(monitor) }
        if let monitor = localRightMouseMonitor { NSEvent.removeMonitor(monitor) }
    }

    // MARK: - Prevent panel from becoming key
    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }

    enum MicState {
        case idle, recording, processing, success, error
    }
}

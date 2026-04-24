import AppKit

// MARK: - Colors
private extension NSColor {
    static let btnIdle = NSColor(hex: "#2D2D2D")
    static let btnRecording = NSColor(hex: "#E53935")
    static let btnProcessing = NSColor(hex: "#FF9800")
    static let btnSuccess = NSColor(hex: "#43A047")
    static let toggleOn = NSColor(hex: "#16a34a")
    static let toggleOnBright = NSColor(hex: "#22c55e")
    static let toggleOff = NSColor(hex: "#2D2D2D")
    static let btnBtwIdle = NSColor(hex: "#64B5F6")
    static let btnBtwRecording = NSColor(hex: "#1E88E5")
    static let btnBtwPulse = NSColor(hex: "#90CAF9")
    static let btnX = NSColor(hex: "#E53935")
    static let btnXPressed = NSColor(hex: "#FF6666")
    static let btnMicIdle = NSColor(hex: "#2A5DA8")
    static let btnCopy = NSColor(red: 0x29/255.0, green: 0xB6/255.0, blue: 0xF6/255.0, alpha: 1) // #29B6F6 sky blue
    static let btnPaste = NSColor(red: 0xAB/255.0, green: 0x47/255.0, blue: 0xBC/255.0, alpha: 1) // #AB47BC purple
    static let ultrathinkOn = NSColor(hex: "#B8860B")     // dark gold background
    static let starGold = NSColor(hex: "#FFD700")          // bright gold star
    static let starMuted = NSColor(hex: "#8B7355")         // muted brown star
    static let i18nOn = NSColor(hex: "#26A69A")            // teal for i18n toggle

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
    let i18nButton: RoundButton
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
    var onI18nClicked: (() -> Void)?

    init() {
        let btnSize: CGFloat = 40
        let micSize: CGFloat = 52
        let gap: CGFloat = 8
        let panelWidth: CGFloat = micSize + 20
        // Height: 8 small buttons + 2 large mic buttons + 9 gaps + padding (16 top + 16 bottom)
        // = 40*8 + 52*2 + 8*9 + 32 = 320 + 104 + 72 + 32 = 528
        let panelHeight: CGFloat = btnSize * 8 + micSize * 2 + gap * 9 + 32

        // Create buttons
        ultrathinkButton = RoundButton(label: "★", color: .toggleOff)
        ultrathinkButton.labelFont = .systemFont(ofSize: 20, weight: .bold)
        ultrathinkButton.labelColor = .starMuted
        xButton = RoundButton(label: "X", color: .btnX)
        btwButton = RoundButton(label: "BTW", color: .btnBtwIdle)
        btwButton.labelFont = .boldSystemFont(ofSize: 11)
        btwButton.useSquareShape = true
        micButton = RoundButton(label: "", color: .btnMicIdle)
        micButton.symbolImage = NSImage(systemSymbolName: "mic.fill", accessibilityDescription: "Microphone")
        micButton.useSquareShape = true
        wButton = RoundButton(label: "W", color: .btnIdle)
        gButton = RoundButton(label: "G", color: .toggleOff)
        enterButton = RoundButton(label: "\u{23CE}", color: .toggleOff)
        copyButton = RoundButton(label: "", color: .btnCopy)
        copyButton.symbolImage = NSImage(systemSymbolName: "doc.on.doc", accessibilityDescription: "Copy")
        pasteButton = RoundButton(label: "", color: .btnPaste)
        pasteButton.symbolImage = NSImage(systemSymbolName: "doc.on.clipboard", accessibilityDescription: "Paste")
        i18nButton = RoundButton(label: "\u{00FC}", color: .toggleOff)  // ü

        // Calculate screen position (right edge, vertically centered)
        let screenFrame = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1920, height: 1080)
        var x = screenFrame.maxX - panelWidth - 30
        var y = screenFrame.maxY - panelHeight - 78

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

        // Round corners – use content view layer for rounded background
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.cornerRadius = panelWidth / 2
        self.contentView?.layer?.masksToBounds = true
        self.contentView?.layer?.backgroundColor = NSColor(white: 0.12, alpha: 0.9).cgColor

        // Layout buttons vertically (in AppKit, y=0 is bottom)
        // Visual order top→bottom: ★(ultrathink) → Mic(big) → BTW(big) → W → G → X → Copy → Paste → Enter
        // AppKit order bottom→top: Enter → Paste → Copy → X → G → W → BTW(big) → Mic(big) → ★
        let smallInset = (panelWidth - btnSize) / 2
        let micInset = (panelWidth - micSize) / 2
        var yPos: CGFloat = 16
        enterButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        i18nButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        pasteButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        copyButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        xButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        gButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        wButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        btwButton.frame = NSRect(x: micInset, y: yPos, width: micSize, height: micSize)
        yPos += micSize + gap
        micButton.frame = NSRect(x: micInset, y: yPos, width: micSize, height: micSize)
        yPos += micSize + gap
        ultrathinkButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)

        self.contentView?.addSubview(ultrathinkButton)
        self.contentView?.addSubview(micButton)
        self.contentView?.addSubview(btwButton)
        self.contentView?.addSubview(wButton)
        self.contentView?.addSubview(gButton)
        self.contentView?.addSubview(xButton)
        self.contentView?.addSubview(copyButton)
        self.contentView?.addSubview(pasteButton)
        self.contentView?.addSubview(i18nButton)
        self.contentView?.addSubview(enterButton)

        ultrathinkButton.onClick = { [weak self] in self?.onUltrathinkClicked?() }
        xButton.onClick = { [weak self] in self?.onXClicked?() }
        btwButton.onClick = { [weak self] in self?.onBtwClicked?() }
        micButton.onClick = { [weak self] in self?.onMicClicked?() }
        wButton.onClick = { [weak self] in self?.onWClicked?() }
        gButton.onClick = { [weak self] in self?.onGClicked?() }
        enterButton.onClick = { [weak self] in self?.onEnterClicked?() }
        copyButton.onClick = { [weak self] in self?.onCopyClicked?() }
        pasteButton.onClick = { [weak self] in self?.onPasteClicked?() }
        i18nButton.onClick = { [weak self] in self?.onI18nClicked?() }

        setupDragMonitors()
    }

    // MARK: - State Updates

    func setMicState(_ state: MicState) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            switch state {
            case .idle:
                self.stopPulse()
                self.micButton.buttonColor = .btnMicIdle
            case .recording:
                self.micButton.buttonColor = .btnRecording
                self.startPulse()
            case .processing:
                self.stopPulse()
                self.micButton.buttonColor = .btnProcessing
            case .success:
                self.stopPulse()
                self.micButton.buttonColor = .btnSuccess
            case .error:
                self.stopPulse()
                self.micButton.buttonColor = .btnRecording
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
            case .recording:
                self.btwButton.buttonColor = .btnRecording
                self.startBtwPulse()
            case .processing:
                self.stopBtwPulse()
                self.btwButton.buttonColor = .btnProcessing
            case .success:
                self.stopBtwPulse()
                self.btwButton.buttonColor = .btnSuccess
            case .error:
                self.stopBtwPulse()
                self.btwButton.buttonColor = .btnRecording
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
    /// clicks are NOT blocked during that window — rapid ✕-ing (wie bei
    /// Windows) funktioniert so ohne Cooldown.
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

    func setI18nEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.i18nButton.buttonColor = enabled ? .i18nOn : .toggleOff
        }
    }

    // MARK: - Mic Pulse Animation

    private func startPulse() {
        stopPulse()
        var bright = false
        pulseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            bright.toggle()
            self.micButton.buttonColor = bright ? NSColor(hex: "#FF6666") : .btnRecording
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
            self.btwButton.buttonColor = bright ? NSColor(hex: "#FF6666") : .btnRecording
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
            return true
        case .rightMouseUp:
            if isDragging {
                isDragging = false
                savePosition()
                return true
            }
        default:
            break
        }
        return false
    }

    private func savePosition() {
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

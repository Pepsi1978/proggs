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

// MARK: - RoundButton (3D)
class RoundButton: NSView {
    let diameter: CGFloat = 40
    var buttonColor: NSColor = .btnIdle { didSet { needsDisplay = true } }
    var label: String = "" { didSet { needsDisplay = true } }
    var labelFont: NSFont = .systemFont(ofSize: 16, weight: .bold)
    var symbolImage: NSImage?
    var useSquareShape: Bool = false
    var onClick: (() -> Void)?
    var onMouseDown: (() -> Void)?
    var onMouseUp: (() -> Void)?
    private var isPressed: Bool = false

    init(label: String, color: NSColor) {
        self.label = label
        self.buttonColor = color
        super.init(frame: NSRect(x: 0, y: 0, width: diameter, height: diameter))
        wantsLayer = true
        layer?.shadowColor = NSColor.black.cgColor
        layer?.shadowOpacity = 0.5
        layer?.shadowOffset = CGSize(width: 0, height: -2)
        layer?.shadowRadius = 3
    }

    required init?(coder: NSCoder) { fatalError() }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        for area in trackingAreas { removeTrackingArea(area) }
        addTrackingArea(NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeAlways], owner: self))
    }

    override func draw(_ dirtyRect: NSRect) {
        guard let ctx = NSGraphicsContext.current?.cgContext else { return }
        let inset = bounds.insetBy(dx: 2, dy: 2)

        // Build clipping path (circle or rounded rect)
        let cgPath: CGPath
        if useSquareShape {
            cgPath = CGPath(roundedRect: inset, cornerWidth: 8, cornerHeight: 8, transform: nil)
        } else {
            cgPath = CGPath(ellipseIn: inset, transform: nil)
        }

        ctx.saveGState()
        ctx.addPath(cgPath)
        ctx.clip()

        // Extract RGB components for gradient
        let base = buttonColor.usingColorSpace(.sRGB) ?? buttonColor
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        base.getRed(&r, green: &g, blue: &b, alpha: &a)

        // 3D gradient: light top → dark bottom (inverted when pressed)
        let lighten: CGFloat = isPressed ? -0.08 : 0.25
        let darken: CGFloat = isPressed ? 0.15 : -0.15
        let topR = min(1, max(0, r + lighten))
        let topG = min(1, max(0, g + lighten))
        let topB = min(1, max(0, b + lighten))
        let botR = min(1, max(0, r + darken))
        let botG = min(1, max(0, g + darken))
        let botB = min(1, max(0, b + darken))

        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let colors = [
            CGColor(colorSpace: colorSpace, components: [topR, topG, topB, 1.0])!,
            CGColor(colorSpace: colorSpace, components: [r, g, b, 1.0])!,
            CGColor(colorSpace: colorSpace, components: [botR, botG, botB, 1.0])!
        ] as CFArray
        let locations: [CGFloat] = [0.0, 0.5, 1.0]

        if let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: locations) {
            let startPt = CGPoint(x: inset.midX, y: inset.maxY)
            let endPt = CGPoint(x: inset.midX, y: inset.minY)
            ctx.drawLinearGradient(gradient, start: startPt, end: endPt, options: [])
        }

        // Specular highlight — translucent white arc at top
        if !isPressed {
            let highlightRect = NSRect(
                x: inset.origin.x + inset.width * 0.15,
                y: inset.origin.y + inset.height * 0.52,
                width: inset.width * 0.7,
                height: inset.height * 0.44
            )
            let highlightPath = useSquareShape
                ? CGPath(roundedRect: highlightRect, cornerWidth: 6, cornerHeight: 6, transform: nil)
                : CGPath(ellipseIn: highlightRect, transform: nil)
            ctx.addPath(highlightPath)
            ctx.setFillColor(CGColor(colorSpace: colorSpace, components: [1, 1, 1, 0.18])!)
            ctx.fillPath()
        }

        // Subtle inner rim — light edge at top, dark edge at bottom
        let rimPath: CGPath
        if useSquareShape {
            rimPath = CGPath(roundedRect: inset.insetBy(dx: 0.5, dy: 0.5), cornerWidth: 7.5, cornerHeight: 7.5, transform: nil)
        } else {
            rimPath = CGPath(ellipseIn: inset.insetBy(dx: 0.5, dy: 0.5), transform: nil)
        }
        ctx.addPath(rimPath)
        ctx.setStrokeColor(CGColor(colorSpace: colorSpace, components: [1, 1, 1, isPressed ? 0.05 : 0.2])!)
        ctx.setLineWidth(1.0)
        ctx.strokePath()

        ctx.restoreGState()

        // Draw icon or label on top
        if let img = symbolImage {
            let imgSize = CGSize(width: bounds.width * 0.5, height: bounds.height * 0.5)
            let yOff: CGFloat = isPressed ? -0.5 : 0.5
            let imgRect = NSRect(
                x: (bounds.width - imgSize.width) / 2,
                y: (bounds.height - imgSize.height) / 2 + yOff,
                width: imgSize.width, height: imgSize.height
            )
            let tinted = img.copy() as! NSImage
            tinted.lockFocus()
            NSColor.white.set()
            NSRect(origin: .zero, size: tinted.size).fill(using: .sourceAtop)
            tinted.unlockFocus()
            tinted.draw(in: imgRect, from: .zero, operation: .sourceOver, fraction: 1.0)
        } else {
            // Drop shadow on text for depth
            let shadow = NSShadow()
            shadow.shadowColor = NSColor(white: 0, alpha: 0.5)
            shadow.shadowOffset = NSSize(width: 0, height: -1)
            shadow.shadowBlurRadius = 1
            let yOff: CGFloat = isPressed ? -0.5 : 0.5
            let attrs: [NSAttributedString.Key: Any] = [
                .foregroundColor: NSColor.white,
                .font: labelFont,
                .shadow: shadow
            ]
            let str = NSAttributedString(string: label, attributes: attrs)
            let size = str.size()
            let point = NSPoint(x: (bounds.width - size.width) / 2, y: (bounds.height - size.height) / 2 + yOff)
            str.draw(at: point)
        }
    }

    override func mouseDown(with event: NSEvent) {
        isPressed = true
        layer?.shadowOffset = CGSize(width: 0, height: -1)
        layer?.shadowOpacity = 0.3
        needsDisplay = true
        if onMouseDown != nil {
            onMouseDown?()
        } else {
            onClick?()
        }
    }

    override func mouseUp(with event: NSEvent) {
        isPressed = false
        layer?.shadowOffset = CGSize(width: 0, height: -2)
        layer?.shadowOpacity = 0.5
        needsDisplay = true
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
    let xButton: RoundButton
    let btwButton: RoundButton
    let micButton: RoundButton
    let wButton: RoundButton
    let gButton: RoundButton
    let enterButton: RoundButton
    private var pulseTimer: Timer?
    private var btwPulseTimer: Timer?
    private var xButtonCooldown = false

    // Right-click drag state
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var dragStartPanelOrigin: NSPoint = .zero
    private var globalRightMouseMonitor: Any?
    private var localRightMouseMonitor: Any?
    private static let positionKey = "terminalOverlayPanelPosition"

    var onXClicked: (() -> Void)?
    var onBtwClicked: (() -> Void)?
    var onMicClicked: (() -> Void)?
    var onWClicked: (() -> Void)?
    var onGClicked: (() -> Void)?
    var onEnterClicked: (() -> Void)?

    init() {
        let btnSize: CGFloat = 40
        let micSize: CGFloat = 52
        let gap: CGFloat = 8
        let panelWidth: CGFloat = micSize + 20
        // Height: 4 small buttons + 2 large mic buttons + 5 gaps + padding (16 top + 16 bottom)
        let panelHeight: CGFloat = btnSize * 4 + micSize * 2 + gap * 5 + 32

        // Create buttons
        xButton = RoundButton(label: "X", color: .btnX)
        btwButton = RoundButton(label: "", color: .btnBtwIdle)
        btwButton.symbolImage = NSImage(systemSymbolName: "mic.fill", accessibilityDescription: "BTW Microphone")
        btwButton.useSquareShape = true
        micButton = RoundButton(label: "", color: .btnMicIdle)
        micButton.symbolImage = NSImage(systemSymbolName: "mic.fill", accessibilityDescription: "Microphone")
        micButton.useSquareShape = true
        wButton = RoundButton(label: "W", color: .btnIdle)
        gButton = RoundButton(label: "G", color: .toggleOff)
        enterButton = RoundButton(label: "\u{23CE}", color: .toggleOff)

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
        // Bottom to top: Enter, G, Mic(big), BTW-Mic(big), W, X
        let smallInset = (panelWidth - btnSize) / 2
        let micInset = (panelWidth - micSize) / 2
        var yPos: CGFloat = 16
        enterButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        gButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        micButton.frame = NSRect(x: micInset, y: yPos, width: micSize, height: micSize)
        yPos += micSize + gap
        btwButton.frame = NSRect(x: micInset, y: yPos, width: micSize, height: micSize)
        yPos += micSize + gap
        wButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)
        yPos += btnSize + gap
        xButton.frame = NSRect(x: smallInset, y: yPos, width: btnSize, height: btnSize)

        self.contentView?.addSubview(xButton)
        self.contentView?.addSubview(btwButton)
        self.contentView?.addSubview(micButton)
        self.contentView?.addSubview(wButton)
        self.contentView?.addSubview(gButton)
        self.contentView?.addSubview(enterButton)

        xButton.onClick = { [weak self] in self?.onXClicked?() }
        btwButton.onClick = { [weak self] in self?.onBtwClicked?() }
        micButton.onClick = { [weak self] in self?.onMicClicked?() }
        wButton.onClick = { [weak self] in self?.onWClicked?() }
        gButton.onClick = { [weak self] in self?.onGClicked?() }
        enterButton.onClick = { [weak self] in self?.onEnterClicked?() }

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

    func flashXButton() -> Bool {
        if xButtonCooldown { return false }
        xButtonCooldown = true
        DispatchQueue.main.async { [weak self] in
            self?.xButton.buttonColor = .btnIdle
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
                self?.xButton.buttonColor = .btnX
                self?.xButtonCooldown = false
            }
        }
        return true
    }

    func setAutoEnterEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.enterButton.buttonColor = enabled ? .btnProcessing : .toggleOff
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

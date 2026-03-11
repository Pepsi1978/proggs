import AppKit

// MARK: - Colors
private extension NSColor {
    static let btnIdle = NSColor(hex: "#2D2D2D")
    static let btnRecording = NSColor(hex: "#E53935")
    static let btnProcessing = NSColor(hex: "#FF9800")
    static let btnSuccess = NSColor(hex: "#43A047")
    static let toggleOn = NSColor(hex: "#22c55e")
    static let toggleOff = NSColor(hex: "#2D2D2D")
    static let toggleBtw = NSColor(hex: "#2196F3")
    static let btnBtwRecording = NSColor(hex: "#1E88E5")
    static let btnBtwPulse = NSColor(hex: "#64B5F6")
    static let btnX = NSColor(hex: "#E53935")
    static let btnXPressed = NSColor(hex: "#FF6666")

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
    var onClick: (() -> Void)?
    var onMouseDown: (() -> Void)?
    var onMouseUp: (() -> Void)?

    init(label: String, color: NSColor) {
        self.label = label
        self.buttonColor = color
        super.init(frame: NSRect(x: 0, y: 0, width: diameter, height: diameter))
        let trackingArea = NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeAlways], owner: self)
        addTrackingArea(trackingArea)
    }

    required init?(coder: NSCoder) { fatalError() }

    override func draw(_ dirtyRect: NSRect) {
        let path = NSBezierPath(ovalIn: bounds.insetBy(dx: 1, dy: 1))
        buttonColor.setFill()
        path.fill()

        let attrs: [NSAttributedString.Key: Any] = [
            .foregroundColor: NSColor.white,
            .font: labelFont
        ]
        let str = NSAttributedString(string: label, attributes: attrs)
        let size = str.size()
        let point = NSPoint(x: (bounds.width - size.width) / 2, y: (bounds.height - size.height) / 2)
        str.draw(at: point)
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

    // Right-click drag state
    private var isDragging = false
    private var dragStartMouseLocation: NSPoint = .zero
    private var dragStartPanelOrigin: NSPoint = .zero
    private var globalRightMouseMonitor: Any?
    private var localRightMouseMonitor: Any?
    private static let positionKey = "terminalOverlayPanelPosition"

    var isBtwMode: Bool = false

    var onXClicked: (() -> Void)?
    var onBtwClicked: (() -> Void)?
    var onMicClicked: (() -> Void)?
    var onWClicked: (() -> Void)?
    var onGClicked: (() -> Void)?
    var onEnterClicked: (() -> Void)?

    init() {
        let btnSize: CGFloat = 40
        let gap: CGFloat = 8
        let panelWidth: CGFloat = btnSize + 16
        let panelHeight: CGFloat = btnSize * 6 + gap * 5 + 16

        // Create buttons
        xButton = RoundButton(label: "X", color: .btnX)
        btwButton = RoundButton(label: "?", color: .toggleOff)
        micButton = RoundButton(label: "\u{1F3A4}", color: .btnIdle)
        micButton.labelFont = .systemFont(ofSize: 14)
        wButton = RoundButton(label: "W", color: .btnIdle)
        gButton = RoundButton(label: "G", color: .toggleOff)
        enterButton = RoundButton(label: "\u{23CE}", color: .toggleOff)

        // Calculate screen position (right edge, vertically centered)
        let screenFrame = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1920, height: 1080)
        var x = screenFrame.maxX - panelWidth - 23
        var y = screenFrame.maxY - panelHeight - 37

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
        // Bottom to top: Enter, G, W, Mic, X
        let inset = (panelWidth - btnSize) / 2
        enterButton.frame = NSRect(x: inset, y: 8, width: btnSize, height: btnSize)
        gButton.frame = NSRect(x: inset, y: 8 + (btnSize + gap), width: btnSize, height: btnSize)
        wButton.frame = NSRect(x: inset, y: 8 + (btnSize + gap) * 2, width: btnSize, height: btnSize)
        micButton.frame = NSRect(x: inset, y: 8 + (btnSize + gap) * 3, width: btnSize, height: btnSize)
        btwButton.frame = NSRect(x: inset, y: 8 + (btnSize + gap) * 4, width: btnSize, height: btnSize)
        xButton.frame = NSRect(x: inset, y: 8 + (btnSize + gap) * 5, width: btnSize, height: btnSize)

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
                self.micButton.buttonColor = .btnIdle
            case .recording:
                self.micButton.buttonColor = self.isBtwMode ? .btnBtwRecording : .btnRecording
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

    func setGeminiEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.gButton.buttonColor = enabled ? .toggleOn : .toggleOff
            self?.wButton.buttonColor = enabled ? .toggleOff : .toggleOn
        }
    }

    func flashXButton() {
        DispatchQueue.main.async { [weak self] in
            self?.xButton.buttonColor = .btnXPressed
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
                self?.xButton.buttonColor = .btnX
            }
        }
    }

    func setAutoEnterEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.enterButton.buttonColor = enabled ? .toggleOn : .toggleOff
        }
    }

    func setBtwEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.isBtwMode = enabled
            self?.btwButton.buttonColor = enabled ? .toggleBtw : .toggleOff
        }
    }

    // MARK: - Pulse Animation

    private func startPulse() {
        stopPulse()
        var bright = false
        pulseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            bright.toggle()
            if self.isBtwMode {
                self.micButton.buttonColor = bright ? .btnBtwPulse : .btnBtwRecording
            } else {
                self.micButton.buttonColor = bright ? NSColor(hex: "#FF6666") : .btnRecording
            }
        }
    }

    private func stopPulse() {
        pulseTimer?.invalidate()
        pulseTimer = nil
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

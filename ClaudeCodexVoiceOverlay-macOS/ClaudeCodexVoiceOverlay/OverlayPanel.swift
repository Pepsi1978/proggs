import AppKit

// MARK: - Colors
private extension NSColor {
    static let btnIdle = NSColor(hex: "#2D2D2D")
    static let btnRecording = NSColor(hex: "#E53935")
    static let btnProcessing = NSColor(hex: "#FF9800")
    static let btnSuccess = NSColor(hex: "#43A047")
    static let toggleOn = NSColor(hex: "#22c55e")
    static let toggleOff = NSColor(hex: "#2D2D2D")

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
        onClick?()
    }
}

// MARK: - OverlayPanel
final class OverlayPanel: NSPanel {
    let xButton: RoundButton
    let micButton: RoundButton
    let wButton: RoundButton
    let gButton: RoundButton
    let enterButton: RoundButton
    private var pulseTimer: Timer?

    var onXClicked: (() -> Void)?
    var onMicClicked: (() -> Void)?
    var onWClicked: (() -> Void)?
    var onGClicked: (() -> Void)?
    var onEnterClicked: (() -> Void)?

    init() {
        let btnSize: CGFloat = 40
        let gap: CGFloat = 8
        let panelWidth: CGFloat = btnSize + 16
        let panelHeight: CGFloat = btnSize * 5 + gap * 4 + 16

        // Create buttons
        xButton = RoundButton(label: "X", color: .btnIdle)
        micButton = RoundButton(label: "\u{1F3A4}", color: .btnIdle)
        micButton.labelFont = .systemFont(ofSize: 14)
        wButton = RoundButton(label: "W", color: .btnIdle)
        gButton = RoundButton(label: "G", color: .toggleOff)
        enterButton = RoundButton(label: "\u{23CE}", color: .toggleOff)

        // Calculate screen position (right edge, vertically centered)
        let screenFrame = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1920, height: 1080)
        let x = screenFrame.maxX - panelWidth - 23
        let y = screenFrame.midY - panelHeight / 2

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
        xButton.frame = NSRect(x: inset, y: 8 + (btnSize + gap) * 4, width: btnSize, height: btnSize)

        self.contentView?.addSubview(xButton)
        self.contentView?.addSubview(micButton)
        self.contentView?.addSubview(wButton)
        self.contentView?.addSubview(gButton)
        self.contentView?.addSubview(enterButton)

        xButton.onClick = { [weak self] in self?.onXClicked?() }
        micButton.onClick = { [weak self] in self?.onMicClicked?() }
        wButton.onClick = { [weak self] in self?.onWClicked?() }
        gButton.onClick = { [weak self] in self?.onGClicked?() }
        enterButton.onClick = { [weak self] in self?.onEnterClicked?() }
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

    func setGeminiEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.gButton.buttonColor = enabled ? .toggleOn : .toggleOff
        }
    }

    func setAutoEnterEnabled(_ enabled: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.enterButton.buttonColor = enabled ? .toggleOn : .toggleOff
        }
    }

    // MARK: - Pulse Animation

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

    // MARK: - Prevent panel from becoming key
    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }

    enum MicState {
        case idle, recording, processing, success, error
    }
}

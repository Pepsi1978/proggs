import AppKit

/// Nachbau der WPF-Styles aus MainWindow.xaml als AppKit-Bausteine.
/// WPF beschreibt Aussehen deklarativ (ControlTemplate + DynamicResource); AppKit braucht dafuer
/// eigene Views mit CALayer-Eigenschaften. Jede Klasse hier entspricht genau einem XAML-Style.

// MARK: - Karte (Style "Card")

/// `<Style x:Key="Card" TargetType="Border">`: abgerundeter Panel-Hintergrund mit feinem Rand und
/// weichem Schlagschatten.
final class CardView: NSView {
    var cornerRadius: CGFloat = 16 { didSet { needsLayout = true } }

    init() {
        super.init(frame: .zero)
        wantsLayer = true
        // KEIN Schlagschatten mehr. Die Karten sitzen im NSSplitView und werden dort auf ihren
        // eigenen Rahmen geclippt: nach AUSSEN war vom Schatten also nie etwas zu sehen, nach
        // INNEN dagegen schon - genau in den vier ausgesparten Rundungsecken, wo die Karte
        // durchsichtig ist. Dort stand der volle graue Schattenkern und wirkte wie eine
        // aufgesetzte eckige graue Ecke. Ohne Schatten bleiben nur die sauberen Rundungen.
        layer?.masksToBounds = true
        layer?.shadowOpacity = 0
        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    deinit { NotificationCenter.default.removeObserver(self) }

    @objc func applyTheme() {
        let palette = ThemeManager.palette
        layer?.cornerRadius = cornerRadius
        layer?.borderWidth = 1
        layer?.backgroundColor = palette.panelBg.flattened(over: ThemeManager.flattenBase).cgColor
        layer?.borderColor = palette.glassBorder.cgColor
        needsLayout = true
    }
}

// MARK: - Flaeche (Style "SurfaceBg"-Border)

/// Leichter, abgerundeter Untergrund - in XAML die `Border` mit `SurfaceBg` + `BorderBrushSoft`
/// (Modellgruppen, Thinking-Rahmen).
class SurfaceView: NSView {
    var cornerRadius: CGFloat = 13 { didSet { applyTheme() } }
    var showsBorder = true { didSet { applyTheme() } }

    init(cornerRadius: CGFloat = 13) {
        self.cornerRadius = cornerRadius
        super.init(frame: .zero)
        wantsLayer = true
        // Inhalte an der Rundung abschneiden: sonst schiebt sich ein Kind mit eigener Flaeche
        // (Listen, Kopfzeilen) bis in die ausgesparte Ecke und macht aus der Rundung wieder ein
        // Viereck.
        layer?.masksToBounds = true
        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    @objc func applyTheme() {
        let palette = ThemeManager.palette
        layer?.cornerRadius = cornerRadius
        layer?.backgroundColor = palette.surfaceBg.flattened(over: ThemeManager.flattenBase).cgColor
        layer?.borderWidth = showsBorder ? 1 : 0
        layer?.borderColor = palette.borderSoft.cgColor
    }
}

// MARK: - Buttons

/// Gemeinsame Basis: eigenes Zeichnen statt NSButton-Bezel, damit Ecken, Farben und Hover exakt
/// den WPF-ControlTemplates entsprechen. Barrierefreiheit bleibt erhalten (Rolle, Label,
/// accessibilityPerformPress) - Best-Practice §C.
class StyledButton: NSControl {
    enum Style {
        case accent   // `AccentBtn`
        case ghost    // `GhostBtn`
        case window   // `WindowBtn`
        case close    // `CloseBtn`
        case theme    // `ThemeBtn`
    }

    private let label = NSTextField(labelWithString: "")
    private let symbolView = NSImageView()
    private var trackingAreaRef: NSTrackingArea?
    private var isHovered = false { didSet { applyTheme() } }
    private var isPressed = false { didSet { applyTheme() } }

    let style: Style
    var horizontalPadding: CGFloat = 12
    var verticalPadding: CGFloat = 7

    var title: String {
        get { label.stringValue }
        set {
            label.stringValue = newValue
            setAccessibilityLabel(newValue)
            invalidateIntrinsicContentSize()
        }
    }

    /// Optionales SF-Symbol statt Text (fuer Knoepfe, die unter Windows ein Segoe-Icon tragen).
    var symbolName: String? {
        didSet {
            guard let symbolName,
                  let image = NSImage(systemSymbolName: symbolName, accessibilityDescription: label.stringValue) else {
                symbolView.isHidden = true
                label.isHidden = false
                return
            }
            symbolView.image = image
            symbolView.isHidden = false
            label.isHidden = true
            invalidateIntrinsicContentSize()
        }
    }

    var fontSize: CGFloat = 13 {
        didSet {
            label.font = .systemFont(ofSize: fontSize, weight: style == .accent ? .semibold : .semibold)
            invalidateIntrinsicContentSize()
        }
    }

    override var isEnabled: Bool {
        didSet { applyTheme() }
    }

    init(style: Style, title: String) {
        self.style = style
        super.init(frame: .zero)
        wantsLayer = true
        layer?.masksToBounds = false
        translatesAutoresizingMaskIntoConstraints = false

        label.translatesAutoresizingMaskIntoConstraints = false
        label.alignment = .center
        label.lineBreakMode = .byTruncatingTail
        label.font = .systemFont(ofSize: fontSize, weight: .semibold)
        label.isSelectable = false
        addSubview(label)

        symbolView.translatesAutoresizingMaskIntoConstraints = false
        symbolView.isHidden = true
        symbolView.imageScaling = .scaleProportionallyDown
        addSubview(symbolView)

        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: centerXAnchor),
            label.centerYAnchor.constraint(equalTo: centerYAnchor),
            label.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: 4),
            label.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -4),
            symbolView.centerXAnchor.constraint(equalTo: centerXAnchor),
            symbolView.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])

        setAccessibilityRole(.button)
        self.title = title
        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    override var intrinsicContentSize: NSSize {
        let size = label.intrinsicContentSize
        return NSSize(width: size.width + horizontalPadding * 2, height: size.height + verticalPadding * 2)
    }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let trackingAreaRef { removeTrackingArea(trackingAreaRef) }
        let area = NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeInActiveApp], owner: self)
        addTrackingArea(area)
        trackingAreaRef = area
    }

    override func layout() {
        super.layout()
        // Groesse geaendert -> Schattenumriss neu setzen.
        if style == .accent, layer?.shadowOpacity ?? 0 > 0 {
            layer?.shadowPath = CGPath(roundedRect: bounds, cornerWidth: 12,
                                       cornerHeight: 12, transform: nil)
        }
    }

    override func mouseEntered(with event: NSEvent) { if isEnabled { isHovered = true } }
    override func mouseExited(with event: NSEvent) { isHovered = false; isPressed = false }

    override func mouseDown(with event: NSEvent) {
        guard isEnabled else { return }
        isPressed = true
    }

    override func mouseUp(with event: NSEvent) {
        let wasPressed = isPressed
        isPressed = false
        guard isEnabled, wasPressed, bounds.contains(convert(event.locationInWindow, from: nil)) else { return }
        performClick()
    }

    private func performClick() {
        if let action, let target { NSApp.sendAction(action, to: target, from: self) }
    }

    override func accessibilityPerformPress() -> Bool {
        guard isEnabled else { return false }
        performClick()
        return true
    }

    override func resetCursorRects() {
        addCursorRect(bounds, cursor: isEnabled ? .pointingHand : .arrow)
    }

    @objc func applyTheme() {
        let palette = ThemeManager.palette
        let base = ThemeManager.flattenBase
        layer?.borderWidth = 0
        layer?.shadowOpacity = 0

        switch style {
        case .accent:
            layer?.cornerRadius = 12
            layer?.borderWidth = 1
            layer?.borderColor = palette.accentLine.cgColor
            if !isEnabled {
                layer?.backgroundColor = palette.surfaceBg.flattened(over: base).cgColor
                label.textColor = palette.dim
            } else {
                let fill: NSColor
                if isPressed { fill = palette.accentPressed }
                else if isHovered { fill = palette.accentHover }
                else { fill = palette.accentGradientBottom }
                layer?.backgroundColor = fill.cgColor
                label.textColor = .white
                // Schatten auf der Ebene mit rundem Umriss (siehe CardView) - NSView.shadow
                // zeichnete hier einen eckigen Schein um den runden Knopf.
                layer?.shadowColor = NSColor.wpf("#7C6CF5").cgColor
                layer?.shadowOpacity = 0.58
                layer?.shadowRadius = 13
                layer?.shadowOffset = CGSize(width: 0, height: -4)
                layer?.shadowPath = CGPath(roundedRect: bounds, cornerWidth: 12,
                                           cornerHeight: 12, transform: nil)
            }
        case .ghost:
            layer?.cornerRadius = 9
            layer?.borderWidth = 1
            layer?.borderColor = palette.glassBorder.cgColor
            let fill = isHovered && isEnabled ? palette.hoverBg : palette.surfaceBg
            layer?.backgroundColor = fill.flattened(over: base).cgColor
            label.textColor = isEnabled ? palette.text : palette.dim
        case .window:
            layer?.cornerRadius = 7
            layer?.backgroundColor = (isHovered ? palette.hoverBg.flattened(over: base) : .clear).cgColor
            label.textColor = isEnabled ? palette.muted : palette.dim
        case .close:
            layer?.cornerRadius = 7
            layer?.backgroundColor = (isHovered ? palette.closeHoverBg : .clear).cgColor
            label.textColor = isHovered ? .white : palette.muted
        case .theme:
            layer?.cornerRadius = 7
            layer?.backgroundColor = (isHovered ? palette.hoverBg.flattened(over: base) : palette.accentSoftBg.flattened(over: base)).cgColor
            label.textColor = palette.accent
        }
        symbolView.contentTintColor = label.textColor
        window?.invalidateCursorRects(for: self)
    }
}

// MARK: - Schlanker Scrollbalken

/// Eigener Scrollbalken ohne Rinne. macOS zeichnet - je nach Systemeinstellung "Scrollbalken immer
/// einblenden" - hinter dem Griff eine helle Rinne mit Trennlinie. Auf den hellen Karten sah das wie
/// ein langer grauer Strich aus. Hier bleibt nur ein schmaler, abgerundeter Griff uebrig.
final class SlimScroller: NSScroller {
    override class var isCompatibleWithOverlayScrollers: Bool { true }

    override class func scrollerWidth(for controlSize: NSControl.ControlSize,
                                      scrollerStyle: NSScroller.Style) -> CGFloat { 11 }

    /// Rinne (samt Trennlinie zum Inhalt) gar nicht erst zeichnen.
    override func drawKnobSlot(in slotRect: NSRect, highlight flag: Bool) { }

    override func drawKnob() {
        let knob = rect(for: .knob)
        guard knob.width > 0, knob.height > 0 else { return }
        let bar = knob.insetBy(dx: 3, dy: 2)
        let radius = bar.width / 2
        ThemeManager.palette.dim.withAlphaComponent(0.45).setFill()
        NSBezierPath(roundedRect: bar, xRadius: radius, yRadius: radius).fill()
    }
}

/// Einheitliche Einstellung fuer alle Listen der App.
enum ScrollStyling {
    static func apply(to scrollView: NSScrollView) {
        scrollView.hasVerticalScroller = true
        scrollView.autohidesScrollers = true
        scrollView.drawsBackground = false
        scrollView.scrollerStyle = .overlay
        scrollView.verticalScroller = SlimScroller()
        scrollView.scrollerInsets = NSEdgeInsets(top: 0, left: 0, bottom: 0, right: 0)
        scrollView.contentView.drawsBackground = false
        scrollView.backgroundColor = .clear
    }
}

// MARK: - Textfeld (Style "RoundedTextBox")

final class RoundedTextField: NSTextField {
    init() {
        super.init(frame: .zero)
        isBordered = false
        drawsBackground = false
        focusRingType = .none
        wantsLayer = true
        font = .systemFont(ofSize: 13)
        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    /// Innenabstand wie `Padding="11,8"` in WPF.
    override var intrinsicContentSize: NSSize {
        var size = super.intrinsicContentSize
        size.height += 16
        return size
    }

    override func drawFocusRingMask() { /* kein Fokusring - der Rahmen zeigt den Fokus */ }

    private final class PaddedCell: NSTextFieldCell {
        private let inset = NSSize(width: 11, height: 8)

        override func drawingRect(forBounds rect: NSRect) -> NSRect {
            super.drawingRect(forBounds: rect.insetBy(dx: inset.width, dy: inset.height))
        }

        override func select(withFrame rect: NSRect, in controlView: NSView, editor: NSText,
                             delegate: Any?, start: Int, length: Int) {
            super.select(withFrame: drawingRect(forBounds: rect), in: controlView, editor: editor,
                         delegate: delegate, start: start, length: length)
        }

        override func edit(withFrame rect: NSRect, in controlView: NSView, editor: NSText,
                           delegate: Any?, event: NSEvent?) {
            super.edit(withFrame: drawingRect(forBounds: rect), in: controlView, editor: editor,
                       delegate: delegate, event: event)
        }
    }

    override class var cellClass: AnyClass? {
        get { PaddedCell.self }
        set { super.cellClass = newValue }
    }

    @objc func applyTheme() {
        let palette = ThemeManager.palette
        layer?.cornerRadius = 9
        layer?.borderWidth = 1
        layer?.borderColor = palette.borderStrong.cgColor
        layer?.backgroundColor = palette.surfaceBg.flattened(over: ThemeManager.flattenBase).cgColor
        textColor = palette.text
    }
}

// MARK: - Auswahlzeile (Style "ModelItem")

/// Eine anklickbare, auswaehlbare Zeile - Grundlage fuer Modelle, Profile, Modi und Thinking-Stufen.
/// Entspricht `<Style x:Key="ModelItem" TargetType="ListBoxItem">`.
class SelectableRowView: NSView {
    private var trackingAreaRef: NSTrackingArea?
    private var isHovered = false { didSet { applyTheme() } }

    var isSelected = false { didSet { applyTheme() } }
    var isDisabled = false {
        didSet {
            alphaValue = isDisabled ? 0.42 : 1.0
            applyTheme()
        }
    }
    var onClick: (() -> Void)?

    init() {
        super.init(frame: .zero)
        wantsLayer = true
        setAccessibilityRole(.button)
        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let trackingAreaRef { removeTrackingArea(trackingAreaRef) }
        let area = NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeInActiveApp], owner: self)
        addTrackingArea(area)
        trackingAreaRef = area
    }

    override func mouseEntered(with event: NSEvent) { if !isDisabled { isHovered = true } }
    override func mouseExited(with event: NSEvent) { isHovered = false }

    override func mouseDown(with event: NSEvent) {
        guard !isDisabled else { return }
        onClick?()
    }

    override func accessibilityPerformPress() -> Bool {
        guard !isDisabled else { return false }
        onClick?()
        return true
    }

    override func resetCursorRects() {
        addCursorRect(bounds, cursor: isDisabled ? .arrow : .pointingHand)
    }

    @objc func applyTheme() {
        let palette = ThemeManager.palette
        let base = ThemeManager.flattenBase
        layer?.cornerRadius = 10
        layer?.borderWidth = 1
        if isSelected {
            layer?.backgroundColor = palette.selectedBg.flattened(over: base).cgColor
            layer?.borderColor = palette.selectedBorder.cgColor
        } else {
            layer?.backgroundColor = (isHovered ? palette.hoverBg : palette.surfaceBg).flattened(over: base).cgColor
            layer?.borderColor = palette.borderSoft.cgColor
        }
        setAccessibilityValue(isSelected ? "ausgewählt" : "nicht ausgewählt")
        window?.invalidateCursorRects(for: self)
    }
}

// MARK: - Hilfsfunktionen

enum UI {
    /// Beschriftung mit Theme-Farbe. `role` waehlt die Farbe wie die DynamicResources in XAML.
    enum TextRole { case normal, muted, dim, accent }

    static func label(_ text: String, size: CGFloat = 13, weight: NSFont.Weight = .regular,
                      role: TextRole = .normal, monospaced: Bool = false) -> ThemedLabel {
        let label = ThemedLabel(labelWithString: text)
        label.font = monospaced
            ? .monospacedSystemFont(ofSize: size, weight: weight)
            : .systemFont(ofSize: size, weight: weight)
        label.role = role
        label.translatesAutoresizingMaskIntoConstraints = false
        label.applyTheme()
        return label
    }

    /// Waagerechte Trennung/Abstand als unsichtbarer Platzhalter.
    static func spacer() -> NSView {
        let view = NSView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }
}

/// Eigene Titelleiste. Sie liegt ueber der (transparenten) Systemtitelleiste und wuerde deren
/// Verhalten sonst schlucken: Doppelklick zum Maximieren und Ziehen zum Verschieben muessen deshalb
/// hier selbst behandelt werden.
final class TitleBarView: NSView {
    var onDoubleClick: (() -> Void)?

    override func mouseDown(with event: NSEvent) {
        if event.clickCount >= 2 {
            onDoubleClick?()
            return
        }
        // Einfacher Klick: Fenster am Hintergrund ziehen - wie an einer echten Titelleiste.
        window?.performDrag(with: event)
    }
}

/// Beschriftung, die ihre Schriftgroesse so weit verkleinert, bis der Text in die verfuegbare
/// Breite passt (bis zu einer Untergrenze).
///
/// Hintergrund: Die vier Modus-Kacheln stehen nebeneinander und tragen lange deutsche Komposita
/// ("Gründlichkeitsmodus"). Passt ein solches Wort nicht in die Kachel, bricht AppKit es MITTEN IM
/// WORT um - "Gründlichke / itsmodus". Ein fester Schriftgrad kann das nicht loesen, weil die
/// Kachelbreite vom Splitter abhaengt. Deshalb wird die Groesse hier zur Laufzeit passend gewaehlt.
final class AutoShrinkLabel: ThemedLabel {
    /// Groesse, mit der begonnen wird (und die bei genug Platz erhalten bleibt).
    var maximumFontSize: CGFloat = 12
    /// Untergrenze - darunter wird nicht verkleinert, lieber umgebrochen.
    var minimumFontSize: CGFloat = 9.5
    var fontWeight: NSFont.Weight = .semibold

    private var lastFittedWidth: CGFloat = -1

    override func layout() {
        super.layout()
        fitFontToWidth()
    }

    override var stringValue: String {
        didSet {
            lastFittedWidth = -1
            fitFontToWidth()
        }
    }

    private func fitFontToWidth() {
        let available = bounds.width
        guard available > 1, available != lastFittedWidth else { return }
        lastFittedWidth = available

        // Das laengste einzelne Wort entscheidet: nur daran kann AppKit mitten im Wort umbrechen.
        let longestWord = stringValue.split(separator: " ").map(String.init)
            .max(by: { $0.count < $1.count }) ?? stringValue
        guard !longestWord.isEmpty else { return }

        var size = maximumFontSize
        while size > minimumFontSize {
            let candidate = NSFont.systemFont(ofSize: size, weight: fontWeight)
            let width = (longestWord as NSString).size(withAttributes: [.font: candidate]).width
            if width <= available { break }
            size -= 0.5
        }
        font = NSFont.systemFont(ofSize: size, weight: fontWeight)
    }
}

/// Dokument-Ansicht fuer NSScrollView. AppKit rechnet standardmaessig von UNTEN nach oben - der
/// Inhalt einer Liste begaenne dadurch am unteren Rand und die Liste waere beim Start ans Ende
/// gescrollt. `isFlipped = true` dreht die Achse auf die von WPF gewohnte Richtung.
final class FlippedView: NSView {
    override var isFlipped: Bool { true }
}

/// NSTextField, das seine Farbe beim Design-Wechsel selbst nachzieht.
class ThemedLabel: NSTextField {
    var role: UI.TextRole = .normal { didSet { applyTheme() } }

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        commonSetup()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    private func commonSetup() {
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    deinit { NotificationCenter.default.removeObserver(self) }

    @objc func applyTheme() {
        let palette = ThemeManager.palette
        switch role {
        case .normal: textColor = palette.text
        case .muted: textColor = palette.muted
        case .dim: textColor = palette.dim
        case .accent: textColor = palette.accent
        }
    }
}

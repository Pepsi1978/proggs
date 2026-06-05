import AppKit

// MARK: - Collapsed Mic-Pille (Auto-Hide)
// Portierung von TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs
// CollapsedView (84x84, CornerRadius 42, nur Mic sichtbar). BeamFade
// Crossfade, Mic-Position bleibt konstant (CollapseTopOffset = 50).
//
// Funktioniert in beiden Orientierungen — der ausgeklappte Zustand
// merkt sich seine vorherige Form (vertikal oder horizontal) damit
// das Ausklappen die richtige Form wiederherstellt.

enum CollapsedLayout {
    static let panelSize: CGFloat = 84
    static let cornerRadius: CGFloat = 42
    /// Verschiebung beim Einklappen, damit die Mic-Mitte BildschirmPos
    /// behaelt. Windows: CollapseTopOffset = 50. macOS Y ist umgekehrt:
    /// beim Einklappen 84px hoch, vorher 612 hoch → Differenz 528.
    /// Mic war vertikal auf y=488 (Mitte y=514). Nach Einklappen soll
    /// Mic-Mitte bei (collapsedPanelOriginY + 42) liegen.
}

/// Zustand vor dem Einklappen — wird beim Aufklappen wieder hergestellt.
enum CollapsedSource: String {
    case vertical
    case horizontal
}

extension OverlayPanel {

    /// True, wenn das Panel aktuell im Collapsed-Modus ist (84x84-Pille).
    var isCollapsed: Bool {
        get {
            (objc_getAssociatedObject(self, &isCollapsedKey) as? Bool) ?? false
        }
        set {
            objc_setAssociatedObject(self, &isCollapsedKey, newValue,
                                     .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    /// Welche Form war aktiv VOR dem Einklappen — beim Aufklappen
    /// stellen wir genau diese wieder her.
    var collapsedSource: CollapsedSource {
        get {
            let raw = (objc_getAssociatedObject(self, &collapsedSourceKey) as? String) ?? "vertical"
            return CollapsedSource(rawValue: raw) ?? .vertical
        }
        set {
            objc_setAssociatedObject(self, &collapsedSourceKey, newValue.rawValue,
                                     .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    /// Gemerkte expandierte Position (Bildschirm-Origin der vollen Form
    /// vor dem Einklappen). Beim Aufklappen wird genau dahin zurueckgesetzt.
    var savedExpandedPosition: NSPoint? {
        get {
            objc_getAssociatedObject(self, &savedExpandedPositionKey) as? NSPoint
        }
        set {
            if let p = newValue {
                objc_setAssociatedObject(self, &savedExpandedPositionKey, p,
                                         .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
            } else {
                objc_setAssociatedObject(self, &savedExpandedPositionKey, nil,
                                         .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
            }
        }
    }

    /// Baut das Collapsed-Layout auf (84x84, nur micButton in der Mitte).
    /// Andere Buttons werden auf alpha=0 gesetzt + ausserhalb des
    /// sichtbaren Bereichs verschoben, damit sie nicht im Z-Order stoeren.
    func applyCollapsedLayout(at origin: NSPoint) {
        removeOrnamentSubviewsHelper()

        let size = CollapsedLayout.panelSize
        // Auf den sichtbaren Bildschirm klemmen — sonst landet die eingeklappte
        // Mic-Pille ausserhalb, wenn das expandierte Overlay schon am Rand klebte
        // (2026-06-05-Bug: "kurz zu sehen, dann ganz weg").
        let frame = OverlayPanel.clampFrameToVisibleScreen(
            NSRect(x: origin.x, y: origin.y, width: size, height: size))
        self.setFrame(frame, display: false)
        self.contentView?.layer?.cornerRadius = CollapsedLayout.cornerRadius
        // 2 px schwarzer Border-Ring (Windows: BorderBrush #FF000000 BorderThickness 2).
        self.contentView?.layer?.borderColor = NSColor.black.cgColor
        self.contentView?.layer?.borderWidth = 2

        // Hintergrund — Windows-XAML CollapsedView nutzt #B31A1A1A (NICHT #B31F1C15
        // wie ich vorher faelschlich angenommen hatte — das war S2 Mic-Sektor).
        let bg = NSView(frame: NSRect(x: 0, y: 0, width: size, height: size))
        bg.wantsLayer = true
        bg.layer?.backgroundColor = NSColor(hexAlphaCollapsed: "#B31A1A1A").cgColor
        bg.layer?.cornerRadius = CollapsedLayout.cornerRadius
        bg.layer?.masksToBounds = true
        self.contentView?.addSubview(bg, positioned: .below, relativeTo: nil)
        self.hbarOrnamentViewsHelper = [bg]

        // Mic zentriert im 84x84-Pillar (Mic-Groesse 52x52 → Origin 16,16)
        let micSize: CGFloat = 52
        let micX = (size - micSize) / 2
        let micY = (size - micSize) / 2
        self.micButton.frame = NSRect(x: micX, y: micY, width: micSize, height: micSize)

        // Alle anderen Buttons aus dem sichtbaren Bereich schieben.
        // Wir nutzen einen Off-Screen-Punkt — auf macOS reicht negative
        // Koordinate weil ContentView die Subviews clipped.
        let offscreen = NSRect(x: -1000, y: -1000, width: 1, height: 1)
        let otherButtons: [RoundButton] = [
            ultrathinkButton, btwButton,
            wButton, gButton, xButton,
            copyButton, pasteButton,
            screenshotButton, insertScreenshotButton,
            enterButton
        ]
        for b in otherButtons { b.frame = offscreen }
        for tile in profileButtons { tile.frame = offscreen }

        // Extra-Buttons komplett verstecken im Collapsed-Modus.
        hideExtraButtons()
    }

    /// Beam-Wechsel von voll → eingeklappt.
    /// Logik analog Windows: fadeOut → applyCollapsed → fadeIn.
    func beamToCollapsed(completion: (() -> Void)? = nil) {
        guard !isCollapsed else { completion?(); return }
        guard let cv = self.contentView else { completion?(); return }

        // Quelle merken (welche Form war aktiv?)
        self.collapsedSource = (self.currentOrientation == .horizontal) ? .horizontal : .vertical
        // Aktuelle Position als expandierte Position absolut merken.
        self.savedExpandedPosition = self.frame.origin

        // Mic-Mittelpunkt VOR dem Einklappen merken (Bildschirmkoords),
        // damit wir die Pille so positionieren dass der Mic an derselben
        // Stelle bleibt (analog Windows CollapseTopOffset).
        let micFrameInPanel = self.micButton.frame
        let micCenterScreen = NSPoint(
            x: self.frame.origin.x + micFrameInPanel.midX,
            y: self.frame.origin.y + micFrameInPanel.midY
        )

        cv.beamFadeOut { [weak self] in
            guard let self = self else { return }
            // Pille so positionieren dass der zentrierte Mic an der
            // vorherigen Position liegt.
            let pilleOrigin = NSPoint(
                x: micCenterScreen.x - CollapsedLayout.panelSize / 2,
                y: micCenterScreen.y - CollapsedLayout.panelSize / 2
            )
            self.applyCollapsedLayout(at: pilleOrigin)
            self.isCollapsed = true
            cv.beamFadeIn { completion?() }
        }
    }

    /// Beam-Wechsel von eingeklappt → voll.
    /// Stellt die ZUVOR aktive Form wieder her (vertical oder horizontal)
    /// an der gemerkten Position.
    func beamToExpanded(completion: (() -> Void)? = nil) {
        guard isCollapsed else { completion?(); return }
        guard let cv = self.contentView else { completion?(); return }

        cv.beamFadeOut { [weak self] in
            guard let self = self else { return }
            switch self.collapsedSource {
            case .horizontal:
                self.applyHorizontalLayout(at: self.savedExpandedPosition ?? self.savedHorizontalPosition)
            case .vertical:
                self.applyVerticalLayout(at: self.savedExpandedPosition ?? self.savedVerticalPosition)
            }
            self.isCollapsed = false
            cv.beamFadeIn { completion?() }
        }
    }

    /// Toggle-Convenience fuer Dev-Hotkey.
    func toggleCollapsed(completion: (() -> Void)? = nil) {
        if isCollapsed { beamToExpanded(completion: completion) }
        else { beamToCollapsed(completion: completion) }
    }
}

// MARK: - Helper-Bruecke zu OverlayHorizontalLayout-internen Properties
//
// `hbarOrnamentViews` ist fileprivate in OverlayHorizontalLayout.swift, daher
// fuer diese Datei nicht direkt zugreifbar. Wir spiegeln den Mechanismus mit
// eigenem Helper-Key fuer das Collapsed-Layout. Beim Aufklappen wird er
// von applyHorizontalLayout/applyVerticalLayout sowieso geleert (die rufen
// `removeOrnamentSubviews()` auf, das die Heuristik nutzt).

private var hbarOrnamentViewsHelperKey: UInt8 = 0
extension OverlayPanel {
    fileprivate var hbarOrnamentViewsHelper: [NSView] {
        get { (objc_getAssociatedObject(self, &hbarOrnamentViewsHelperKey) as? [NSView]) ?? [] }
        set { objc_setAssociatedObject(self, &hbarOrnamentViewsHelperKey, newValue,
                                       .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }

    fileprivate func removeOrnamentSubviewsHelper() {
        // Heuristisches Entfernen — schaut alle Subviews durch und loescht
        // jene die NICHT RoundButton sind und backgroundColor gesetzt haben.
        // Selbe Logik wie removeOrnamentSubviews() in OverlayHorizontalLayout,
        // hier dupliziert weil das Original fileprivate ist.
        guard let subviews = self.contentView?.subviews else { return }
        for v in subviews {
            if v is RoundButton { continue }
            if v.layer?.backgroundColor != nil &&
               v.layer?.backgroundColor != NSColor.clear.cgColor {
                v.removeFromSuperview()
            }
        }
        self.hbarOrnamentViewsHelper = []
    }
}

// MARK: - Storage Keys
private var isCollapsedKey: UInt8 = 0
private var collapsedSourceKey: UInt8 = 0
private var savedExpandedPositionKey: UInt8 = 0

// MARK: - Farb-Hilfen
private extension NSColor {
    convenience init(hexAlphaCollapsed hex: String) {
        let raw = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        var v: UInt64 = 0
        Scanner(string: raw).scanHexInt64(&v)
        let a, r, g, b: CGFloat
        if raw.count == 8 {
            a = CGFloat((v >> 24) & 0xFF) / 255.0
            r = CGFloat((v >> 16) & 0xFF) / 255.0
            g = CGFloat((v >> 8)  & 0xFF) / 255.0
            b = CGFloat(v & 0xFF) / 255.0
        } else {
            a = 1.0
            r = CGFloat((v >> 16) & 0xFF) / 255.0
            g = CGFloat((v >> 8)  & 0xFF) / 255.0
            b = CGFloat(v & 0xFF) / 255.0
        }
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}

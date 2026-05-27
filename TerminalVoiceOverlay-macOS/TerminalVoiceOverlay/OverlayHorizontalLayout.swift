import AppKit

// MARK: - Horizontales Layout (HBar)
// Portierung von TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs
// BuildHorizontalLayout (Z. 1182+). Anordnung: rechts→links wie vertikal
// oben→unten — Enter ganz links (S7), Stern ganz rechts (S1).
//
// Etappe 1b: Berechnung + Anwendung des horizontalen Layouts.
// Tatsaechliches Umschalten (applyOrientation) folgt in Etappe 2.

enum HBarLayout {
    /// Panel-Hoehe der flachen Leiste. Zwei Reihen: obere = Symbol-Buttons (52px),
    /// untere = Profile-Kacheln (32px). Plus Padding.
    static let panelHeight: CGFloat = 96

    /// Innenabstand oben/unten zwischen Panel-Rand und Buttons.
    static let verticalPadding: CGFloat = 4

    /// Innenabstand zwischen den Gruppen (Sektionen).
    static let groupSpacing: CGFloat = 0

    /// Breite des schwarzen vertikalen Trenners zwischen Sektionen.
    static let dividerWidth: CGFloat = 1

    /// Horizontaler Padding innerhalb jeder Sektion.
    static let sectionInnerPadX: CGFloat = 6

    /// Horizontaler Abstand zwischen zwei Buttons in derselben Reihe.
    static let buttonSpacing: CGFloat = 6

    /// Y-Position der oberen Reihe (Symbol-Buttons), Panel-Koordinaten.
    /// macOS hat Y-Ursprung unten — obere Reihe liegt also bei hoeherem Y.
    static let upperRowY: CGFloat = 38

    /// Y-Position der unteren Reihe (Profile-Kacheln/Diskette).
    static let lowerRowY: CGFloat = 4

    /// Hoehe einer typischen Profile-Kachel (Windows: 30x22 fuer Diskette,
    /// 24x32 fuer Profile-Tiles — wir nutzen 24x30 als Mittelweg).
    static let profileTileSize = NSSize(width: 24, height: 30)

    /// Hoehe der typischen Symbol-Buttons in der oberen Reihe.
    static let actionButtonSize = NSSize(width: 40, height: 40)
    static let micButtonSize    = NSSize(width: 52, height: 52)
    static let smallButtonSize  = NSSize(width: 34, height: 34)

    /// CornerRadius der HBar — Windows nutzt 34 (rund, weil Hoehe 96).
    static let cornerRadius: CGFloat = 34
}

// MARK: - Sektions-Definition fuer das horizontale Layout
// Reihenfolge im Code = links→rechts auf dem Bildschirm.
// Inhalts-Reihenfolge innerhalb jeder Sektion ist gespiegelt
// (das obere Symbol vertikal wird das RECHTE Symbol horizontal).

private struct HBarSection {
    let backgroundHex: String
    /// Hoechste Breite die in dieser Sektion stehen muss (Symbol-Reihe).
    let width: CGFloat
    /// Buttons in der oberen Reihe (Symbol-Buttons), rechts → links.
    let upperButtons: [HBarButtonSlot]
    /// Buttons/Kacheln in der unteren Reihe (Profile/Diskette), rechts → links.
    let lowerButtons: [HBarButtonSlot]
    /// CornerRadius-Modus fuer diese Sektion (oben/unten gerundet).
    let cornerMode: HBarCornerMode
}

private enum HBarCornerMode {
    case leftRounded      // S7 ganz links — links rund
    case middle           // S2..S6 — keine Rundung
    case rightRounded     // S1 ganz rechts — rechts rund
}

private enum HBarButtonSlot {
    case action(KeyPath<OverlayPanel, RoundButton>, NSSize)
    case profile(Int, NSSize)
    case empty(NSSize)  // Platzhalter fuer noch fehlende Buttons (Save, OrientationToggle)
}

// MARK: - Anwendungs-Funktion
extension OverlayPanel {

    /// Wendet das horizontale Layout an: alle Buttons umpositionieren,
    /// alte Sektions-/Divider-Views entfernen, neue Sektionen zeichnen,
    /// Panel-Frame auf HBar-Dimension umstellen.
    ///
    /// `windowOrigin` bestimmt die linke untere Ecke des Panels in
    /// Bildschirmkoordinaten. `nil` -> wird per `savedHorizontalPosition`
    /// oder kanonischer HBar-Position berechnet.
    func applyHorizontalLayout(at windowOrigin: NSPoint? = nil) {
        removeOrnamentSubviews()

        let sections = makeHBarSections()
        var totalWidth: CGFloat = HBarLayout.sectionInnerPadX  // linkes Polster
        for (i, s) in sections.enumerated() {
            totalWidth += s.width
            if i < sections.count - 1 { totalWidth += HBarLayout.dividerWidth }
        }
        totalWidth += HBarLayout.sectionInnerPadX  // rechtes Polster

        // Panel-Frame setzen (links unten = origin).
        let origin: NSPoint = windowOrigin
            ?? savedHorizontalPosition
            ?? canonicalHorizontalOrigin(panelWidth: totalWidth)
        let newFrame = NSRect(x: origin.x, y: origin.y,
                              width: totalWidth, height: HBarLayout.panelHeight)
        self.setFrame(newFrame, display: false)

        // CornerRadius bleibt 34 (passt gut zu Hoehe 96).
        self.contentView?.layer?.cornerRadius = HBarLayout.cornerRadius

        // Sektions-Hintergruende + Trenner einsetzen.
        var x: CGFloat = HBarLayout.sectionInnerPadX
        var trackedSectionViews: [NSView] = []
        var trackedDividerViews: [NSView] = []
        for (i, s) in sections.enumerated() {
            let sectionRect = NSRect(x: x, y: 0,
                                     width: s.width,
                                     height: HBarLayout.panelHeight)
            let sectionView = makeSectionView(rect: sectionRect,
                                              hex: s.backgroundHex,
                                              cornerMode: s.cornerMode,
                                              totalRect: newFrame)
            self.contentView?.addSubview(sectionView, positioned: .below, relativeTo: nil)
            trackedSectionViews.append(sectionView)

            // Buttons in dieser Sektion positionieren — rechts -> links.
            positionButtonsInSection(buttons: s.upperButtons,
                                     rowY: HBarLayout.upperRowY,
                                     sectionX: x,
                                     sectionWidth: s.width)
            positionButtonsInSection(buttons: s.lowerButtons,
                                     rowY: HBarLayout.lowerRowY,
                                     sectionX: x,
                                     sectionWidth: s.width)

            x += s.width

            // Vertikaler Trenner nach dieser Sektion (ausser letzter).
            if i < sections.count - 1 {
                let dividerRect = NSRect(x: x, y: 4,
                                         width: HBarLayout.dividerWidth,
                                         height: HBarLayout.panelHeight - 8)
                let divider = NSView(frame: dividerRect)
                divider.wantsLayer = true
                divider.layer?.backgroundColor = NSColor.black.cgColor
                self.contentView?.addSubview(divider, positioned: .below, relativeTo: nil)
                trackedDividerViews.append(divider)
                x += HBarLayout.dividerWidth
            }
        }

        // Diese Views in einer assoziierten Property merken, damit ein
        // spaeteres applyVerticalLayout() sie wieder entfernen kann.
        self.hbarOrnamentViews = trackedSectionViews + trackedDividerViews
        self.currentOrientation = .horizontal
    }

    /// Stellt das vertikale Layout wieder her. Aktuell vereinfacht: entfernt
    /// die HBar-Ornamente und setzt das Panel-Frame zurueck auf die vertikale
    /// Form. Die existierende `init()`-Layout-Logik baut den vertikalen Modus
    /// auf — eine vollstaendige Rekonstruktion ohne kompletten Re-Init wird
    /// in Etappe 2 (applyOrientation) integriert.
    func resetForVerticalLayout(canonical: NSPoint? = nil) {
        removeOrnamentSubviews()
        self.hbarOrnamentViews = []
        // Panel-Frame und CornerRadius sind plattform-abhaengig und werden
        // in Etappe 2 sauber gesetzt — fuer jetzt setzen wir nur den
        // CornerRadius zurueck.
        self.contentView?.layer?.cornerRadius = 36
        self.currentOrientation = .vertical
    }

    // MARK: - Private Hilfen

    private func makeHBarSections() -> [HBarSection] {
        // Reihenfolge LINKS→RECHTS:
        // S7 (Enter+Diskette), S6, S5, S4, S3, S2, S1 (Stern+Orientation)
        // Inhalts-Reihenfolge innerhalb jeder Sektion = rechts→links wie
        // vertikal oben→unten (Windows Z. 1187).

        let s7 = HBarSection(
            backgroundHex: "#1A1A1A",  // S7-Farbe (Enter)
            width: 56,
            upperButtons: [.action(\.enterButton, HBarLayout.actionButtonSize)],
            lowerButtons: [.empty(NSSize(width: 30, height: 22))],  // Save (fehlt noch)
            cornerMode: .leftRounded)

        let s6 = HBarSection(
            backgroundHex: "#151B15",  // S6 (Shot+Insert)
            width: 92,
            upperButtons: [
                .action(\.insertScreenshotButton, HBarLayout.actionButtonSize),
                .action(\.screenshotButton,       HBarLayout.actionButtonSize),
            ],
            lowerButtons: [
                .profile(10, HBarLayout.profileTileSize),
                .profile(9,  HBarLayout.profileTileSize),
            ],
            cornerMode: .middle)

        let s5 = HBarSection(
            backgroundHex: "#151B1D",  // S5 (Copy+Paste)
            width: 92,
            upperButtons: [
                .action(\.pasteButton, HBarLayout.actionButtonSize),
                .action(\.copyButton,  HBarLayout.actionButtonSize),
            ],
            lowerButtons: [
                .profile(8, HBarLayout.profileTileSize),
                .profile(7, HBarLayout.profileTileSize),
            ],
            cornerMode: .middle)

        let s4 = HBarSection(
            backgroundHex: "#1F1515",  // S4 (X)
            width: 52,
            upperButtons: [.action(\.xButton, HBarLayout.actionButtonSize)],
            lowerButtons: [.profile(6, HBarLayout.profileTileSize)],
            cornerMode: .middle)

        let s3 = HBarSection(
            backgroundHex: "#19151F",  // S3 (W+G)
            width: 92,
            upperButtons: [
                .action(\.gButton, HBarLayout.actionButtonSize),
                .action(\.wButton, HBarLayout.actionButtonSize),
            ],
            lowerButtons: [
                .profile(5, HBarLayout.profileTileSize),
                .profile(4, HBarLayout.profileTileSize),
            ],
            cornerMode: .middle)

        let s2 = HBarSection(
            backgroundHex: "#1F1C15",  // S2 (Mic+BTW)
            width: 124,
            upperButtons: [
                .action(\.btwButton, HBarLayout.micButtonSize),
                .action(\.micButton, HBarLayout.micButtonSize),
            ],
            lowerButtons: [
                .profile(3, HBarLayout.profileTileSize),
                .profile(2, HBarLayout.profileTileSize),
                .profile(1, HBarLayout.profileTileSize),
            ],
            cornerMode: .middle)

        let s1 = HBarSection(
            backgroundHex: "#1F1B15",  // S1 (Stern)
            width: 80,
            upperButtons: [
                .empty(HBarLayout.smallButtonSize),  // OrientationToggle (fehlt noch)
                .action(\.ultrathinkButton, HBarLayout.smallButtonSize),
            ],
            lowerButtons: [],
            cornerMode: .rightRounded)

        return [s7, s6, s5, s4, s3, s2, s1]
    }

    private func positionButtonsInSection(buttons: [HBarButtonSlot],
                                          rowY: CGFloat,
                                          sectionX: CGFloat,
                                          sectionWidth: CGFloat) {
        // buttons[] ist rechts→links angeordnet. In der Sektion werden sie
        // von rechts (sectionX + sectionWidth) nach links platziert.
        var cursor = sectionX + sectionWidth - HBarLayout.sectionInnerPadX
        for slot in buttons {
            switch slot {
            case .action(let keyPath, let size):
                let btn = self[keyPath: keyPath]
                let bx = cursor - size.width
                let by = rowY + (rowYExtra(forRow: rowY, slotHeight: size.height))
                btn.frame = NSRect(x: bx, y: by, width: size.width, height: size.height)
                cursor = bx - HBarLayout.buttonSpacing
            case .profile(let index, let size):
                guard index >= 1, index <= profileButtons.count else { continue }
                let tile = profileButtons[index - 1]
                let bx = cursor - size.width
                let by = rowY + (rowYExtra(forRow: rowY, slotHeight: size.height))
                tile.frame = NSRect(x: bx, y: by, width: size.width, height: size.height)
                cursor = bx - HBarLayout.buttonSpacing
            case .empty(let size):
                // Platzhalter — Cursor vorruecken, aber nichts zeichnen.
                cursor = cursor - size.width - HBarLayout.buttonSpacing
            }
        }
    }

    /// Vertikales Zentrieren in der jeweiligen Reihe (52px obere, 32px untere).
    /// `rowY` ist die Basis-Y (untere Kante der Reihe), `slotHeight` die
    /// Hoehe des Buttons. Verschiebt nach oben damit der Button zentriert
    /// in der Reihen-Hoehe steht.
    private func rowYExtra(forRow rowY: CGFloat, slotHeight: CGFloat) -> CGFloat {
        // Obere Reihe nominal 52px hoch (52 = micButton), untere 32px (Profile).
        let rowHeight: CGFloat = (rowY == HBarLayout.upperRowY) ? 52 : 32
        return max(0, (rowHeight - slotHeight) / 2)
    }

    private func makeSectionView(rect: NSRect,
                                 hex: String,
                                 cornerMode: HBarCornerMode,
                                 totalRect: NSRect) -> NSView {
        let v = NSView(frame: rect)
        v.wantsLayer = true
        v.layer?.backgroundColor = NSColor(hexAlpha: hex).cgColor
        // CornerRadius an Panel-Raendern (links/rechts) per Maske.
        switch cornerMode {
        case .leftRounded, .rightRounded:
            // macOS-CALayer hat `maskedCorners`. CornerRadius wird vom
            // contentView geerbt (cornerRadius=34), aber wir setzen ihn
            // auch hier damit der Hintergrund mitgerundet wird.
            v.layer?.cornerRadius = HBarLayout.cornerRadius
            v.layer?.masksToBounds = true
            v.layer?.maskedCorners = (cornerMode == .leftRounded)
                ? [.layerMinXMinYCorner, .layerMinXMaxYCorner]
                : [.layerMaxXMinYCorner, .layerMaxXMaxYCorner]
        case .middle:
            v.layer?.cornerRadius = 0
        }
        return v
    }

    /// Loescht alle aktuell sichtbaren Sektions-/Divider-Subviews. Greift
    /// auf das Tracking via `hbarOrnamentViews` zurueck; faellt — falls
    /// das leer ist (Initial-Aufbau via OverlayPanel.init) — auf eine
    /// heuristische Suche zurueck: NSView ohne RoundButton-Subview und
    /// mit gesetzter backgroundColor.
    private func removeOrnamentSubviews() {
        if !hbarOrnamentViews.isEmpty {
            hbarOrnamentViews.forEach { $0.removeFromSuperview() }
            return
        }
        guard let subviews = self.contentView?.subviews else { return }
        for v in subviews {
            if v is RoundButton { continue }
            if v.layer?.backgroundColor != nil &&
               v.layer?.backgroundColor != NSColor.clear.cgColor {
                v.removeFromSuperview()
            }
        }
    }

    /// Kanonische HBar-Position: unten zentriert im sichtbaren Bildschirm
    /// (Windows: "horizontale Leiste sitzt unten am Arbeitsbereich").
    func canonicalHorizontalOrigin(panelWidth: CGFloat) -> NSPoint {
        let screen = NSScreen.main?.visibleFrame ??
                     NSRect(x: 0, y: 0, width: 1920, height: 1080)
        let x = screen.minX + (screen.width - panelWidth) / 2
        let y = screen.minY + 40
        return NSPoint(x: x, y: y)
    }
}

// MARK: - Property-Storage via Associated Object
private var hbarOrnamentViewsKey: UInt8 = 0
extension OverlayPanel {
    /// Trackt die Sektions-/Divider-Views des horizontalen Layouts, damit
    /// sie beim Umschalten zurueck zu vertikal sauber entfernt werden.
    fileprivate var hbarOrnamentViews: [NSView] {
        get {
            (objc_getAssociatedObject(self, &hbarOrnamentViewsKey) as? [NSView]) ?? []
        }
        set {
            objc_setAssociatedObject(self, &hbarOrnamentViewsKey, newValue,
                                    .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }
}

// MARK: - Farbe mit Alpha aus Hex-String
private extension NSColor {
    /// Akzeptiert "#RRGGBB" oder "#AARRGGBB" (Windows-Format mit Alpha vorne).
    convenience init(hexAlpha: String) {
        let raw = hexAlpha.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        var value: UInt64 = 0
        Scanner(string: raw).scanHexInt64(&value)
        let a, r, g, b: CGFloat
        if raw.count == 8 {
            a = CGFloat((value >> 24) & 0xFF) / 255.0
            r = CGFloat((value >> 16) & 0xFF) / 255.0
            g = CGFloat((value >> 8)  & 0xFF) / 255.0
            b = CGFloat(value & 0xFF) / 255.0
        } else {
            a = 1.0
            r = CGFloat((value >> 16) & 0xFF) / 255.0
            g = CGFloat((value >> 8)  & 0xFF) / 255.0
            b = CGFloat(value & 0xFF) / 255.0
        }
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}

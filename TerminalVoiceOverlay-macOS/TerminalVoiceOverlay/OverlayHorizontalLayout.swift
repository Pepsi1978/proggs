import AppKit

// MARK: - Horizontales Layout (HBar)
// Portierung von TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs
// BuildHorizontalLayout (Z. 1182+). Anordnung: rechts→links wie vertikal
// oben→unten — Enter ganz links (S7), Stern ganz rechts (S1).
//
// Etappe 1b: Berechnung + Anwendung des horizontalen Layouts.
// Tatsaechliches Umschalten (applyOrientation) folgt in Etappe 2.

enum HBarLayout {
    /// Panel-Hoehe der flachen Leiste — exakt wie Windows MakeHGroup:
    /// top-Reihe 52 + margin 6 + bot-Reihe 22 + padding (6+6) = 92.
    static let panelHeight: CGFloat = 92

    static let verticalPadding: CGFloat = 6   // Windows: Padding 8,6,8,6 → vertikal 6
    static let groupSpacing: CGFloat = 0
    static let dividerWidth: CGFloat = 1
    static let sectionInnerPadX: CGFloat = 8  // Windows: Padding 8 (links/rechts)
    static let buttonSpacing: CGFloat = 6     // Windows: Margin 3 pro Seite = 6 gesamt

    /// Y-Position der oberen Reihe (Symbol-Buttons), Panel-Koordinaten.
    /// macOS hat Y-Ursprung unten. Sektions-Border-Padding 6 oben/unten,
    /// dann Reihe 22 (bot), Reihe-Margin 6, dann Reihe 52 (top).
    /// Y-Wert der unteren Reihe (Unterkante): 6 (padding)
    /// Y-Wert der oberen Reihe (Unterkante): 6 + 22 + 6 = 34
    static let upperRowY: CGFloat = 34
    static let lowerRowY: CGFloat = 6

    /// Profile-Tiles werden im horizontalen Modus auf 30×22 gestaucht
    /// (Windows-Code Z. 1246: `t.Width = 30; t.Height = 22`).
    static let profileTileSize = NSSize(width: 30, height: 22)

    /// Standard-Button-Groessen wie in init().
    static let actionButtonSize = NSSize(width: 40, height: 40)
    static let micButtonSize    = NSSize(width: 52, height: 52)
    /// Stern + OrientationToggle in S1: 34×34 (Windows-Code Z. 1279).
    static let smallButtonSize  = NSSize(width: 34, height: 34)
    /// Save-Diskette in S7: 30×30 (QUADRATISCH). Frueher 36×26 — das
    /// nicht-quadratische Frame hat das quadratische Disketten-Symbol via
    /// RoundButton.draw (Symbolgroesse = bounds.width×scale × bounds.height×scale)
    /// horizontal gestaucht ("gequetscht": 18×13 statt 18×18). Quadratisch +
    /// hoeherer symbolScaleFactor (0.62, gesetzt in positionExtraButtonsHorizontal)
    /// ergibt wieder ein sauberes ~18×18-Symbol. Passt unter die 40×40 Enter-Reihe.
    static let saveButtonHSize  = NSSize(width: 30, height: 30)

    /// CornerRadius der HBar — Hoehe/2 = 46 fuer perfekt runde Enden.
    static let cornerRadius: CGFloat = 46
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
    case saveExtra(NSSize)              // Diskette (lazy via Associated Object)
    case orientationToggleExtra(NSSize) // Pfeil (lazy via Associated Object)
    case empty(NSSize)                  // Platzhalter
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
        // Auf den sichtbaren Bildschirm klemmen — sonst ragt das breite
        // Horizontal-Overlay bei einer am rechten Rand gespeicherten Position
        // aus dem Monitor (2026-06-05-Bug).
        let newFrame = OverlayPanel.clampFrameToVisibleScreen(
            NSRect(x: origin.x, y: origin.y,
                   width: totalWidth, height: HBarLayout.panelHeight))
        self.setFrame(newFrame, display: false)

        // CornerRadius + 2px schwarzer Border-Ring (Windows: BorderBrush #FF000000
        // BorderThickness 2 auf der HorizontalView).
        self.contentView?.layer?.cornerRadius = HBarLayout.cornerRadius
        self.contentView?.layer?.borderColor  = NSColor.black.cgColor
        self.contentView?.layer?.borderWidth  = 2

        // Sektions-Hintergruende + Trenner einsetzen.
        var x: CGFloat = HBarLayout.sectionInnerPadX
        var trackedSectionViews: [NSView] = []
        var trackedDividerViews: [NSView] = []
        var s1Origin: NSPoint = .zero
        var s1Width: CGFloat = 0
        for (i, s) in sections.enumerated() {
            // Hintergrund-Rect der Sektion. Erste Sektion (S7, links) deckt
            // zusaetzlich das linke Innen-Polster ab, letzte Sektion (S1,
            // rechts) das rechte Innen-Polster — sonst zeigt der transparente
            // contentView dort durch und die Pillenenden wirken "abgeschnitten".
            // Die masksToBounds=true + cornerRadius=46 am contentView clippt
            // die Erweiterung sauber auf die runden Pillenenden.
            var bgRect = NSRect(x: x, y: 0,
                                width: s.width,
                                height: HBarLayout.panelHeight)
            if i == 0 {
                bgRect.origin.x = 0
                bgRect.size.width = s.width + HBarLayout.sectionInnerPadX
            }
            if i == sections.count - 1 {
                bgRect.size.width = s.width + HBarLayout.sectionInnerPadX
            }
            // Tracking-Origin/Width fuer S1-Extra-Buttons bleibt die
            // logische Sektion (ohne Polster-Erweiterung).
            if i == sections.count - 1 {  // letzte Sektion = S1
                s1Origin = NSPoint(x: x, y: 0)
                s1Width  = s.width
            }
            let sectionView = makeSectionView(rect: bgRect,
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

            // Vertikaler Trenner nach dieser Sektion (ausser letzter) — geht
            // ueber die VOLLE Hoehe der HBar (Windows: VerticalAlignment Stretch).
            if i < sections.count - 1 {
                let dividerRect = NSRect(x: x, y: 0,
                                         width: HBarLayout.dividerWidth,
                                         height: HBarLayout.panelHeight)
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

        // Extra-Buttons (OrientationToggle, Save) positionieren.
        positionExtraButtonsHorizontal(s1Origin: s1Origin, s1Width: s1Width)

        // HBar-Sektion-Farben enthalten bereits B3-Alpha-Praefix —
        // kein zusaetzlicher alphaValue-Multiplikator (sonst doppelte
        // Transparenz).
    }

    /// Vertikales Layout (96x612 Saeule) komplett neu aufbauen. Wird beim
    /// Zurueckschalten von horizontal -> vertikal benoetigt; die existierende
    /// init()-Logik wird nicht erneut durchlaufen, daher Sektions-Views +
    /// Button-Frames hier 1:1 nachgebaut (gleiche Konstanten wie init()).
    func applyVerticalLayout(at windowOrigin: NSPoint? = nil) {
        removeOrnamentSubviews()

        let panelWidth: CGFloat = 96
        let panelHeight: CGFloat = 612

        let origin: NSPoint = windowOrigin
            ?? savedVerticalPosition
            ?? canonicalVerticalOrigin(panelHeight: panelHeight)
        // Auf den sichtbaren Bildschirm klemmen (Konsistenz mit Horizontal/Collapsed).
        let newFrame = OverlayPanel.clampFrameToVisibleScreen(
            NSRect(x: origin.x, y: origin.y,
                   width: panelWidth, height: panelHeight))
        self.setFrame(newFrame, display: false)
        self.contentView?.layer?.cornerRadius = 36
        self.contentView?.layer?.borderColor  = NSColor.black.cgColor
        self.contentView?.layer?.borderWidth  = 2

        var trackedViews: [NSView] = []

        // 7 Sektionen + 6 Trenner — gleiche Werte wie in OverlayPanel.init().
        // Erste Sektion (S7 unten) und letzte (S1 oben) wurden um 2 px zu den
        // Pillenenden hin verlaengert, damit die Hintergrundfarbe bis zu den
        // abgerundeten Enden durchgeht (sonst zeigt der transparente
        // contentView dort durch — analog zur HBar-Korrektur).
        let sections: [(hex: String, y: CGFloat, h: CGFloat)] = [
            ("#B31A1A1A", 0,   65),   // S7 Enter (war y=2 h=63)
            ("#B3151B15", 66,  100),  // S6 Screenshot+Insert
            ("#B3151B1D", 167, 100),  // S5 Copy+Paste
            ("#B31F1515", 268, 52),   // S4 X
            ("#B319151F", 321, 100),  // S3 W+G
            ("#B31F1C15", 422, 124),  // S2 Mic+BTW
            ("#B31F1B15", 547, 65),   // S1 Stern (war h=63 → 65 bis zur Oberkante 612)
        ]
        for s in sections {
            let v = NSView(frame: NSRect(x: 0, y: s.y, width: panelWidth, height: s.h))
            v.wantsLayer = true
            v.layer?.backgroundColor = NSColor(hexAlpha: s.hex).cgColor
            self.contentView?.addSubview(v, positioned: .below, relativeTo: nil)
            trackedViews.append(v)
        }
        let dividerYs: [CGFloat] = [65, 166, 267, 320, 421, 546]
        for dy in dividerYs {
            let d = NSView(frame: NSRect(x: 0, y: dy, width: panelWidth, height: 1))
            d.wantsLayer = true
            d.layer?.backgroundColor = NSColor.black.cgColor
            self.contentView?.addSubview(d, positioned: .below, relativeTo: nil)
            trackedViews.append(d)
        }

        // Button-Frames — identisch mit init() Z. 523-533.
        let btnSize: CGFloat = 40
        let micSize: CGFloat = 52
        let micColumnX: CGFloat = 7
        let actionButtonX: CGFloat = 13     // 7 + (52-40)/2
        let centeredButtonX: CGFloat = 28   // (96-40)/2

        ultrathinkButton.frame       = NSRect(x: centeredButtonX, y: 553, width: btnSize, height: btnSize)
        micButton.frame              = NSRect(x: micColumnX,      y: 488, width: micSize, height: micSize)
        btwButton.frame              = NSRect(x: micColumnX,      y: 428, width: micSize, height: micSize)
        wButton.frame                = NSRect(x: actionButtonX,   y: 375, width: btnSize, height: btnSize)
        gButton.frame                = NSRect(x: actionButtonX,   y: 327, width: btnSize, height: btnSize)
        xButton.frame                = NSRect(x: actionButtonX,   y: 274, width: btnSize, height: btnSize)
        copyButton.frame             = NSRect(x: actionButtonX,   y: 221, width: btnSize, height: btnSize)
        pasteButton.frame            = NSRect(x: actionButtonX,   y: 173, width: btnSize, height: btnSize)
        screenshotButton.frame       = NSRect(x: actionButtonX,   y: 120, width: btnSize, height: btnSize)
        insertScreenshotButton.frame = NSRect(x: actionButtonX,   y: 72,  width: btnSize, height: btnSize)
        enterButton.frame            = NSRect(x: centeredButtonX, y: 19,  width: btnSize, height: btnSize)

        // Profile-Tiles — gleiche Positionen wie computeProfilePositions().
        let profileX: CGFloat = 65
        let tileSize = NSSize(width: 24, height: 32)
        let profilePositions: [(idx: Int, y: CGFloat)] = [
            (1, 508), (2, 468), (3, 428),    // S2
            (4, 375), (5, 335),              // S3
            (6, 278),                         // S4
            (7, 221), (8, 181),              // S5
            (9, 120), (10, 80),              // S6
        ]
        for p in profilePositions where p.idx >= 1 && p.idx <= profileButtons.count {
            profileButtons[p.idx - 1].frame =
                NSRect(x: profileX, y: p.y, width: tileSize.width, height: tileSize.height)
        }

        self.hbarOrnamentViews = trackedViews
        self.currentOrientation = .vertical

        // Extra-Buttons (OrientationToggle, Save) positionieren.
        positionExtraButtonsVertical()
    }

    /// Vollstaendiger Beam-Switch zwischen vertikaler Saeule und horizontaler
    /// Leiste. Pseudocode (analog Windows BeamToOrientation Z. 1378):
    ///   1) beamFadeOut(contentView, 240ms)
    ///   2) im unsichtbaren Zustand neue Layout-Geometrie anwenden
    ///   3) beamFadeIn(contentView, 380ms)
    ///
    /// Ein erneuter Aufruf waehrend einer laufenden Animation ist via
    /// Generations-Counter abgesichert (analog `_collapseBeamGen`).
    func beamToOrientation(_ target: OverlayOrientation,
                           completion: (() -> Void)? = nil) {
        guard target != currentOrientation else {
            completion?()
            return
        }
        guard let cv = self.contentView else {
            completion?()
            return
        }

        let myGen = nextBeamGen()
        switch target {
        case .horizontal:
            // ── Vertikal → Horizontal ──
            // 1) Saeule ausblenden
            // 2) Layout-Switch — Leiste landet zunaechst an FINALER X-Position
            //    (rechts-unten), aber auf Saeulen-Oberkante-Y. So glidet sie
            //    NUR vertikal nach unten, nicht schraeg.
            // 3) Leiste einblenden
            // 4) Y-Glide nach unten zur finalen Y-Position
            cv.beamFadeOut { [weak self] in
                guard let self = self,
                      self.beamGenIsCurrent(myGen) else { return }

                let columnTopY = self.frame.maxY  // Saeulen-Oberkante
                if self.savedVerticalPosition == nil {
                    self.savedVerticalPosition = self.frame.origin
                }
                // Layout-Wechsel an FINALER Position aufbauen (= rechts-unten).
                self.applyHorizontalLayout(at: self.savedHorizontalPosition)
                // Finale X/Y nach dem Aufbau auslesen.
                let finalFrame = self.frame
                // Y-Sprung: HBar an die Saeulen-Oberkante hochsetzen.
                self.setFrameOrigin(NSPoint(
                    x: finalFrame.origin.x,
                    y: columnTopY - HBarLayout.panelHeight
                ))

                cv.beamFadeIn { [weak self] in
                    guard let self = self,
                          self.beamGenIsCurrent(myGen) else { return }
                    // GERADER Glide nach UNTEN — X bleibt, nur Y wechselt.
                    self.glideWindow(to: finalFrame.origin) { completion?() }
                }
            }

        case .vertical:
            // ── Horizontal → Vertikal ──
            // 1) Leiste glatt NACH OBEN gleiten lassen bis zur Saeulen-Oberkante
            //    (Gegenrichtung zur Erscheinen-Glide).
            // 2) BeamFadeOut der Leiste.
            // 3) Form wechseln unsichtbar — Saeule an gemerkter/kanonischer
            //    Position einsetzen.
            // 4) BeamFadeIn der Saeule.
            if self.savedHorizontalPosition == nil {
                self.savedHorizontalPosition = self.frame.origin
            }
            // Saeulen-Oberkante berechnen (Y-Position fuer die Glide-Endposition).
            let panelHeight: CGFloat = 612
            let canonicalCol = self.canonicalVerticalOrigin(panelHeight: panelHeight)
            let columnTopY = (self.savedVerticalPosition?.y ?? canonicalCol.y) + panelHeight
            let glideEnd = NSPoint(x: self.frame.origin.x,
                                   y: columnTopY - HBarLayout.panelHeight)

            self.glideWindow(to: glideEnd) { [weak self] in
                guard let self = self,
                      self.beamGenIsCurrent(myGen) else { return }
                cv.beamFadeOut { [weak self] in
                    guard let self = self,
                          self.beamGenIsCurrent(myGen) else { return }
                    self.applyVerticalLayout(at: self.savedVerticalPosition)
                    cv.beamFadeIn { completion?() }
                }
            }
        }
    }

    // MARK: - Generations-Counter (Re-Entrancy-Guard)

    private func nextBeamGen() -> Int {
        let next = beamGen + 1
        beamGen = next
        return next
    }

    private func beamGenIsCurrent(_ gen: Int) -> Bool {
        return gen == beamGen
    }

    // MARK: - Private Hilfen

    private func makeHBarSections() -> [HBarSection] {
        // Reihenfolge LINKS→RECHTS:
        // S7 (Enter+Umschalter), S6, S5, S4, S3, S2, S1 (Stern+Diskette)
        // Inhalts-Reihenfolge innerhalb jeder Sektion = rechts→links wie
        // vertikal oben→unten (Windows Z. 1187).

        let s7 = HBarSection(
            backgroundHex: "#B31A1A1A",  // S7 = Enter oben + Umschalter unten (MakeHGroup)
            width: 56,
            upperButtons: [.action(\.enterButton, HBarLayout.actionButtonSize)],
            lowerButtons: [.orientationToggleExtra(HBarLayout.saveButtonHSize)],  // 30×22
            cornerMode: .leftRounded)

        // Sektion-Widths: Mindestbreite = Content + 2×sectionInnerPadX (=16),
        // sonst landet das berechnete Polster (max(8, (width-content)/2))
        // hinter der Sektion-Grenze und die Buttons ragen in die Nachbar-
        // Sektion. Vorher: 92 fuer 86-Content → nur 3 px Polster, Buttons
        // 5 px ueber die Trennlinie. Jetzt 104 → echtes 9 px Polster.
        let s6 = HBarSection(
            backgroundHex: "#B3151B15",  // S6 (Shot+Insert)
            width: 104,
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
            backgroundHex: "#B3151B1D",  // S5 (Copy+Paste)
            width: 104,
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
            backgroundHex: "#B31F1515",  // S4 (X)
            width: 60,
            upperButtons: [.action(\.xButton, HBarLayout.actionButtonSize)],
            lowerButtons: [.profile(6, HBarLayout.profileTileSize)],
            cornerMode: .middle)

        let s3 = HBarSection(
            backgroundHex: "#B319151F",  // S3 (W+G)
            width: 104,
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
            backgroundHex: "#B31F1C15",  // S2 (Mic+BTW)
            width: 132,
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

        // S1 ist die Spezial-Sektion (MakeHStackGroup in Windows):
        // Stern 34×34 OBEN + Diskette 34×34 UNTEN, 4 px-Abstand. Beide Buttons sind
        // zu hoch fuer die 22-px-lower-Reihe, deshalb positioniert
        // positionExtraButtonsHorizontal() sie direkt anhand der gemerkten
        // s1-Sektion-Position. Slots leer halten.
        let s1 = HBarSection(
            backgroundHex: "#B31F1B15",
            width: 50,  // 34 + 16 padding
            upperButtons: [],
            lowerButtons: [],
            cornerMode: .rightRounded)

        return [s7, s6, s5, s4, s3, s2, s1]
    }

    private func positionButtonsInSection(buttons: [HBarButtonSlot],
                                          rowY: CGFloat,
                                          sectionX: CGFloat,
                                          sectionWidth: CGFloat) {
        // Windows StackPanel laeuft LINKS -> RECHTS, also auch hier:
        // buttons[0] landet links, buttons[N] rechts. Die Gesamtbreite der
        // gefuellten Slots wird in der Sektion ZENTRIERT damit kleinere
        // Inhalte (z.B. 1 Button + 1 Tile) nicht klebrig links sitzen.
        let totalContent: CGFloat = buttons.reduce(0) { acc, slot in
            switch slot {
            case .action(_, let s):                return acc + s.width + HBarLayout.buttonSpacing
            case .profile(_, let s):               return acc + s.width + HBarLayout.buttonSpacing
            case .saveExtra(let s):                return acc + s.width + HBarLayout.buttonSpacing
            case .orientationToggleExtra(let s):   return acc + s.width + HBarLayout.buttonSpacing
            case .empty(let s):                    return acc + s.width + HBarLayout.buttonSpacing
            }
        } - (buttons.isEmpty ? 0 : HBarLayout.buttonSpacing)
        let startX = sectionX + max(HBarLayout.sectionInnerPadX,
                                    (sectionWidth - totalContent) / 2)
        var cursor = startX
        for slot in buttons {
            switch slot {
            case .action(let keyPath, let size):
                let btn = self[keyPath: keyPath]
                let by = rowY + rowYExtra(forRow: rowY, slotHeight: size.height)
                btn.frame = NSRect(x: cursor, y: by, width: size.width, height: size.height)
                cursor += size.width + HBarLayout.buttonSpacing
            case .profile(let index, let size):
                guard index >= 1, index <= profileButtons.count else { continue }
                let tile = profileButtons[index - 1]
                let by = rowY + rowYExtra(forRow: rowY, slotHeight: size.height)
                tile.frame = NSRect(x: cursor, y: by, width: size.width, height: size.height)
                cursor += size.width + HBarLayout.buttonSpacing
            case .saveExtra(let size):
                let by = rowY + rowYExtra(forRow: rowY, slotHeight: size.height)
                self.saveButton.frame = NSRect(x: cursor, y: by,
                                                width: size.width, height: size.height)
                cursor += size.width + HBarLayout.buttonSpacing
            case .orientationToggleExtra(let size):
                let by = rowY + rowYExtra(forRow: rowY, slotHeight: size.height)
                self.orientationToggleButton.frame = NSRect(x: cursor, y: by,
                                                width: size.width, height: size.height)
                cursor += size.width + HBarLayout.buttonSpacing
            case .empty(let size):
                cursor += size.width + HBarLayout.buttonSpacing
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
        // Kein eigener CornerRadius — das contentView des Panels clippt
        // bereits durch seine masksToBounds=true + cornerRadius=46. Eigene
        // maskedCorners auf der Sektion fuehrten dazu dass S1 (50 breit,
        // cornerRadius 46) den Hintergrund komplett weg-clippte.
        v.layer?.cornerRadius = 0
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

    /// Kanonische HBar-Position: RECHTS-UNTEN im Arbeitsbereich (Windows:
    /// `finalLeft = _waX + _waW - ActualWidth - 27`,
    /// `finalTop = _waY + _waH - ActualHeight - HBarBottomLift`).
    /// HBarBottomLift ~ 40 (kleiner Lift vom unteren Rand).
    func canonicalHorizontalOrigin(panelWidth: CGFloat) -> NSPoint {
        let screen = NSScreen.main?.visibleFrame ??
                     NSRect(x: 0, y: 0, width: 1920, height: 1080)
        let x = screen.maxX - panelWidth - 27    // 27 px rechts
        let y = screen.minY + 40                  // 40 px ueber unterer Kante
        return NSPoint(x: x, y: y)
    }
}

// MARK: - Property-Storage via Associated Object
private var hbarOrnamentViewsKey: UInt8 = 0
private var beamGenKey: UInt8 = 0
extension OverlayPanel {
    /// Trackt die Sektions-/Divider-Views des aktuellen Layouts, damit
    /// sie beim Umschalten der Orientation sauber entfernt werden.
    fileprivate var hbarOrnamentViews: [NSView] {
        get {
            (objc_getAssociatedObject(self, &hbarOrnamentViewsKey) as? [NSView]) ?? []
        }
        set {
            objc_setAssociatedObject(self, &hbarOrnamentViewsKey, newValue,
                                    .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    }

    /// Generations-Counter fuer Beam-Switches (Re-Entrancy-Schutz wenn
    /// der Benutzer den Toggle mehrfach in schneller Folge ausloest).
    fileprivate var beamGen: Int {
        get {
            (objc_getAssociatedObject(self, &beamGenKey) as? Int) ?? 0
        }
        set {
            objc_setAssociatedObject(self, &beamGenKey, newValue,
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

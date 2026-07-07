import AppKit

// MARK: - OrientationToggleButton + SaveButton (Diskette)
// Portierung der zwei Buttons, die in der bestehenden OverlayPanel.swift
// fehlen. Implementiert als Associated-Object Lazy-Properties damit
// die bestehende init()-Logik unveraendert bleibt.
//
// OrientationToggleButton (34x34, Symbol ⇄): toggelt vertikal <-> horizontal
//   - S1 vertikal: neben dem Stern (positioniert in applyVerticalLayout)
//   - S1 horizontal: gleiche Sektion, neben dem Stern
//
// SaveButton (28x28, Symbol 💾): merkt die aktuelle Position pro Orientation
//   - S7 vertikal: neben dem Enter
//   - S7 horizontal: gleiche Sektion, neben dem Enter

extension OverlayPanel {

    /// Toggle-Button (⇄) — gleich gross wie Stern/Enter (40×40), rund.
    var orientationToggleButton: RoundButton {
        if let b = objc_getAssociatedObject(self, &orientationToggleButtonKey)
            as? RoundButton {
            return b
        }
        let b = RoundButton(label: "",
                            color: NSColor(red: 0.290, green: 0.290, blue: 0.290, alpha: 1),
                            width: 40, height: 40)
        b.symbolImage = NSImage(systemSymbolName: "arrow.left.arrow.right",
                                accessibilityDescription: "Orientation toggle")
        b.labelColor = NSColor.white
        b.onClick = { [weak self] in self?.onOrientationToggleClicked?() }
        // Windows-Tooltip: "Layout drehen (vertikal ↔ horizontal)"
        b.toolTip = "Layout drehen (vertikal ↔ horizontal)"
        self.contentView?.addSubview(b)
        objc_setAssociatedObject(self, &orientationToggleButtonKey, b,
                                 .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return b
    }

    /// Save-Button (Diskette) — TRANSPARENT (kein grauer Kasten), nur das
    /// Disketten-Symbol ist sichtbar. Vergroessert von 28×28 auf 36×36 fuer
    /// bessere Sichtbarkeit (Windows-Pendant zieht auf 36 mit).
    /// CornerRadius 10 (proportional skaliert).
    /// Disketten-Symbol als NSBezierPath (Material 'save'-Glyph approximiert
    /// das Segoe MDL2 E74E-Glyph aus Windows-XAML).
    /// Beim Klick blitzt das Symbol kurz gruen auf als Feedback.
    /// Wenn eine Position gespeichert ist (savedHorizontalPosition oder
    /// savedVerticalPosition != nil) wird das Symbol dauerhaft gruen
    /// angezeigt — `refreshSaveButtonState()` synchronisiert das.
    var saveButton: RoundButton {
        if let b = objc_getAssociatedObject(self, &saveButtonKey)
            as? RoundButton {
            return b
        }
        let b = RoundButton(label: "",
                            color: NSColor.clear,
                            width: 36, height: 36)
        b.symbolImage = IconPaths.renderImage(
            path: IconPaths.save(), size: NSSize(width: 22, height: 22), fill: .white)
        b.labelColor = NSColor.white
        b.cornerRadius = 10
        b.onClick = { [weak self] in
            self?.onSaveClicked?()
            self?.flashSaveButtonGreen()
            // Nach dem Speicher-Klick wird die Save-Position gesetzt — kurz
            // warten bis der Caller das tatsaechlich gemacht hat, dann den
            // Zustand neu auswerten (gruen bleibt an).
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) { [weak self] in
                self?.refreshSaveButtonState()
            }
        }
        // Windows-Tooltip: "Position dieser Ausrichtung merken (gilt bis zum Neustart)"
        b.toolTip = "Position dieser Ausrichtung merken"
        self.contentView?.addSubview(b)
        objc_setAssociatedObject(self, &saveButtonKey, b,
                                 .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return b
    }

    /// Gemeinsame Gruen-Farbe fuer Save-Feedback und Dauer-Indikator.
    static var saveButtonGreen: NSColor {
        NSColor(red: 0.18, green: 0.71, blue: 0.20, alpha: 1)
    }

    /// Disketten-Klick: das Disketten-Symbol blitzt kurz gruen auf
    /// (1:1 Windows: gruenes Aufblitzen der Foreground-Farbe).
    /// Nach dem Aufblitzen wird der dauerhafte Zustand (gruen wenn
    /// Position gespeichert ist, sonst weiss) wiederhergestellt.
    func flashSaveButtonGreen() {
        saveButton.labelColor = OverlayPanel.saveButtonGreen
        saveButton.needsDisplay = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            self?.refreshSaveButtonState()
        }
    }

    /// Setzt die Disketten-Farbe abhaengig davon ob eine Position fuer die
    /// AKTUELLE Orientierung gespeichert ist:
    /// - horizontal aktiv → savedHorizontalPosition != nil → gruen
    /// - vertikal   aktiv → savedVerticalPosition   != nil → gruen
    /// - sonst weiss.
    /// Muss nach jeder applyHorizontalLayout / applyVerticalLayout sowie
    /// nach jedem Setzen/Loeschen einer gespeicherten Position aufgerufen
    /// werden.
    func refreshSaveButtonState() {
        let hasSavedPosition: Bool
        switch currentOrientation {
        case .horizontal: hasSavedPosition = (savedHorizontalPosition != nil)
        case .vertical:   hasSavedPosition = (savedVerticalPosition   != nil)
        }
        saveButton.labelColor = hasSavedPosition
            ? OverlayPanel.saveButtonGreen
            : NSColor.white
        saveButton.needsDisplay = true
    }

    /// Klick-Callback fuer den OrientationToggleButton.
    var onOrientationToggleClicked: (() -> Void)? {
        get { objc_getAssociatedObject(self, &onOrientationToggleClickedKey)
              as? (() -> Void) }
        set { objc_setAssociatedObject(self, &onOrientationToggleClickedKey,
                                       newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }

    /// Klick-Callback fuer den SaveButton (Diskette).
    var onSaveClicked: (() -> Void)? {
        get { objc_getAssociatedObject(self, &onSaveClickedKey)
              as? (() -> Void) }
        set { objc_setAssociatedObject(self, &onSaveClickedKey,
                                       newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }

    /// Positioniert die Buttons in der vertikalen Saeule EXAKT wie das
    /// Windows-XAML OverlayWindow.xaml:
    ///
    /// **S1** (Section1Panel, Padding 0,21,0,8): StackPanel Horizontal,
    /// HorizontalAlignment=Center. Stern 34×34, ⇄ 34×34 mit Margin 5,0,0,0.
    /// Total breite 34+5+34 = 73, zentriert in 96px Panel → x_origin = 11.
    ///
    /// **S7** (Padding 0,6,0,17): Grid mit Spalten 52 + Auto, HorizontalAlignment
    /// Center. Enter 40×40 zentriert in 52-Spalte, Save 28×28 in Auto-Spalte
    /// mit Margin 2,0,0,0. Grid-Breite = 52+2+28 = 82, zentriert → x_origin = 7.
    func positionExtraButtonsVertical() {
        // S1: y=547..610 (h=63). WPF padding top 21 + bot 8.
        // macOS-Y (unten=0): Buttons-Unterkante = 547 + 8 = 555.
        ultrathinkButton.buttonWidth  = 34
        ultrathinkButton.buttonHeight = 34
        ultrathinkButton.frame        = NSRect(x: 11, y: 555, width: 34, height: 34)
        orientationToggleButton.frame = NSRect(x: 50, y: 555, width: 34, height: 34)
        // Stern als exakter XAML-Pentagram-Path (1:1 Windows-XAML Z. 202-204).
        // Wird mit labelColor (starMuted/starGold) eingefaerbt via RoundButton.draw.
        if ultrathinkButton.symbolImage == nil {
            ultrathinkButton.label = ""
            ultrathinkButton.symbolImage = IconPaths.renderImage(
                path: IconPaths.star(),
                size: NSSize(width: 17, height: 17),
                fill: .white  // Color wird zur Render-Zeit durch labelColor.set() ersetzt
            )
        }
        ultrathinkButton.needsDisplay = true

        // S7: y=0..65 (nach Pillenenden-Fix). WPF padding top 6 + bot 17.
        // macOS-Y: Enter-Unterkante = 2 + 17 = 19. Save 36×36 vertikal zentriert
        // zu Enter (40): Save-Unterkante = 19 + (40-36)/2 = 21.
        // X: Enter endet bei 13+40=53, Save mit kleinem Margin 2 → x=55.
        // Save endet bei 55+36=91 → 5 px rechtes Polster im 96er Panel.
        enterButton.frame = NSRect(x: 13, y: 19, width: 40, height: 40)
        saveButton.frame  = NSRect(x: 55, y: 21, width: 36, height: 36)

        orientationToggleButton.alphaValue = 1.0
        saveButton.alphaValue = 1.0

        // Vertikal ist die Diskette 36×36 — Standard-Scale (36×0.5 = 18).
        // Explizit zuruecksetzen, falls vorher der horizontale 0.62-Wert aktiv
        // war (Orientation-Wechsel).
        saveButton.symbolScaleFactor = 0.5
        saveButton.needsDisplay = true

        // 30% Durchsichtigkeit auf den Sektions-Hintergruenden (B3-Alpha in Windows).
        applySectionTransparency()

        // Save-Diskette einfaerben: gruen wenn eine vertikale Position
        // gespeichert ist, sonst weiss.
        refreshSaveButtonState()
    }

    /// Setzt 70% Opacity auf alle OPAKEN Sektions-Subviews der contentView.
    /// init() (OverlayPanel.swift) erzeugt Sektionen mit opaken Hex-Werten —
    /// die werden hier nachtraeglich auf 70% gesetzt. Sektionen die mit
    /// B3-Alpha-Hex (z.B. applyVerticalLayout/applyHorizontalLayout) erzeugt
    /// wurden, haben bereits Alpha < 1 und werden uebersprungen, um doppelte
    /// Transparenz (0.7 × 0.7 = 0.49) zu vermeiden.
    func applySectionTransparency() {
        guard let subviews = self.contentView?.subviews else { return }
        for v in subviews {
            if v is RoundButton { continue }
            guard let bg = v.layer?.backgroundColor else { continue }
            // Trenner (1px hoch) lassen wir voll opak.
            if v.frame.height <= 2 { continue }
            // Bereits transparenter Hintergrund: skip.
            if bg.alpha < 0.99 { continue }
            v.alphaValue = 0.70
        }
    }

    /// Positioniert die Extra-Buttons in der horizontalen Leiste.
    /// Save wird automatisch in S7 ueber die Slot-Mechanik platziert
    /// (.saveExtra in der lower-Reihe). Stern + ⇄ in S1 brauchen
    /// Spezial-Behandlung: beide 34×34 ÜBEREINANDER (MakeHStackGroup auf
    /// Windows), Slot-Mechanik kann das nicht (lower-Row ist nur 22 hoch).
    /// `s1Origin` + `s1Width` werden von applyHorizontalLayout uebergeben.
    func positionExtraButtonsHorizontal(s1Origin: NSPoint, s1Width: CGFloat) {
        // Stern auf 34×34 verkleinern (war 40×40 im vertikalen Layout).
        ultrathinkButton.buttonWidth  = 34
        ultrathinkButton.buttonHeight = 34

        // S1-Sektion: 50 breit, 92 hoch. Padding 6 oben/unten.
        // Stack: Stern oben + 4 px Margin + ⇄ unten. Total 34+4+34 = 72.
        // Free space innerhalb 92 - 12 padding = 80. (80-72)/2 = 4 zusaetzlich.
        // macOS-Y (unten=0):
        //   ⇄    untere Kante = padding 6 + 4 = 10
        //   Stern untere Kante = 10 + 34 + 4 (margin) = 48
        let stackX = s1Origin.x + (s1Width - 34) / 2
        ultrathinkButton.frame        = NSRect(x: stackX, y: 48, width: 34, height: 34)
        orientationToggleButton.frame = NSRect(x: stackX, y: 10, width: 34, height: 34)
        ultrathinkButton.needsDisplay = true

        orientationToggleButton.alphaValue = 1.0
        saveButton.alphaValue = 1.0

        // Diskette ist horizontal quadratisch (30×30). RoundButton skaliert das
        // Symbol relativ zu bounds — mit 0.62 wird das Symbol ~18×18 (so gross
        // wie im vertikalen 36×36-Layout) und nicht mehr gequetscht.
        saveButton.symbolScaleFactor = 0.62
        saveButton.needsDisplay = true

        // Save-Diskette einfaerben: gruen wenn eine horizontale Position
        // gespeichert ist, sonst weiss.
        refreshSaveButtonState()
    }

    /// Versteckt die Extra-Buttons (Collapsed-Mode).
    func hideExtraButtons() {
        orientationToggleButton.alphaValue = 0.0
        saveButton.alphaValue = 0.0
    }
}

// MARK: - Storage Keys
private var orientationToggleButtonKey: UInt8 = 0
private var saveButtonKey: UInt8 = 0
private var onOrientationToggleClickedKey: UInt8 = 0
private var onSaveClickedKey: UInt8 = 0

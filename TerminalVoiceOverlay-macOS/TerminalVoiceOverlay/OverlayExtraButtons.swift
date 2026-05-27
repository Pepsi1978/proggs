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
        self.contentView?.addSubview(b)
        objc_setAssociatedObject(self, &orientationToggleButtonKey, b,
                                 .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return b
    }

    /// Save-Button (Diskette) — TRANSPARENT (kein grauer Kasten), nur das
    /// Disketten-Symbol ist sichtbar. Exakt wie Windows SaveButton-Style:
    /// Width 28, Height 28, CornerRadius 8, Background transparent.
    /// Beim Klick blitzt das Symbol kurz gruen auf als Feedback.
    var saveButton: RoundButton {
        if let b = objc_getAssociatedObject(self, &saveButtonKey)
            as? RoundButton {
            return b
        }
        let b = RoundButton(label: "\u{1F4BE}",  // 💾
                            color: NSColor.clear,
                            width: 28, height: 28)
        b.labelFont = .systemFont(ofSize: 18)
        b.labelColor = NSColor.white
        b.cornerRadius = 8
        b.onClick = { [weak self] in
            self?.onSaveClicked?()
            self?.flashSaveButtonGreen()
        }
        self.contentView?.addSubview(b)
        objc_setAssociatedObject(self, &saveButtonKey, b,
                                 .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return b
    }

    /// Disketten-Klick: das Disketten-Symbol blitzt kurz gruen auf als
    /// Feedback "Position gespeichert" (Windows: gruenes Aufblitzen der
    /// Foreground-Farbe). Hintergrund bleibt transparent.
    func flashSaveButtonGreen() {
        let normalColor = NSColor.white
        let greenColor  = NSColor(red: 0.18, green: 0.71, blue: 0.20, alpha: 1)
        saveButton.labelColor = greenColor
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            self?.saveButton.labelColor = normalColor
        }
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
        ultrathinkButton.needsDisplay = true

        // S7: y=2..65. WPF padding top 6 + bot 17.
        // macOS-Y: Enter-Unterkante = 2 + 17 = 19. Save 28×28 vertikal zentriert
        // zu Enter (40): Save-Unterkante = 19 + (40-28)/2 = 25.
        enterButton.frame = NSRect(x: 13, y: 19, width: 40, height: 40)
        saveButton.frame  = NSRect(x: 61, y: 25, width: 28, height: 28)

        orientationToggleButton.alphaValue = 1.0
        saveButton.alphaValue = 1.0

        // 30% Durchsichtigkeit auf den Sektions-Hintergruenden (B3-Alpha in Windows).
        applySectionTransparency()
    }

    /// Setzt 70% Opacity auf alle Sektions-Subviews der contentView —
    /// passt das macOS-Aussehen an das Windows-Terminal-Vorbild an.
    /// Wirkt auf init()'s Original-Sektionen UND auf von applyVerticalLayout
    /// neu erstellte Sektions-Views.
    func applySectionTransparency() {
        guard let subviews = self.contentView?.subviews else { return }
        for v in subviews {
            if v is RoundButton { continue }
            guard v.layer?.backgroundColor != nil else { continue }
            // Schwarze 1px-Trenner (height ~1) lassen wir voll opak.
            if v.frame.height <= 2 { continue }
            v.alphaValue = 0.70
        }
    }

    /// In der horizontalen Leiste werden Save + OrientationToggle bereits
    /// von den HBar-Slots (saveExtra / orientationToggleExtra) positioniert.
    /// Wir muessen hier nur die Sichtbarkeit aktivieren (alphaValue=1).
    func positionExtraButtonsHorizontal() {
        // Erzwingt das Lazy-Init falls die Buttons noch nicht existieren —
        // die Slot-Positionierung passiert in positionButtonsInSection().
        _ = self.orientationToggleButton
        _ = self.saveButton
        orientationToggleButton.alphaValue = 1.0
        saveButton.alphaValue = 1.0
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

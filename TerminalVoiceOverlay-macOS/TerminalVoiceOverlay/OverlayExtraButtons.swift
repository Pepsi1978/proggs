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

    /// Save-Button (Diskette) — gleich gross wie Enter (40×40), rund.
    var saveButton: RoundButton {
        if let b = objc_getAssociatedObject(self, &saveButtonKey)
            as? RoundButton {
            return b
        }
        let b = RoundButton(label: "",
                            color: NSColor(red: 0.290, green: 0.290, blue: 0.290, alpha: 1),
                            width: 40, height: 40)
        b.symbolImage = NSImage(systemSymbolName: "externaldrive.fill",
                                accessibilityDescription: "Save position")
        b.labelColor = NSColor.white
        b.onClick = { [weak self] in self?.onSaveClicked?() }
        self.contentView?.addSubview(b)
        objc_setAssociatedObject(self, &saveButtonKey, b,
                                 .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return b
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

    /// Positioniert OrientationToggle + Save fuer die vertikale Saeule.
    /// Anordnung wie Windows: in S1 Stern OBEN + ⇄ UNTEN, in S7 Enter
    /// OBEN + Diskette UNTEN. Wir muessen dafuer auch ultrathinkButton
    /// und enterButton leicht verschieben, damit beide Buttons der Stack-
    /// Gruppe in die jeweilige Sektion passen.
    func positionExtraButtonsVertical() {
        // S1 von y=547 bis y=610 (63 hoch). Stack:
        //   Stern oben bei y=580 (40 hoch, Mitte y=600)
        //   ⇄    unten bei y=550 (24 hoch)
        ultrathinkButton.frame = NSRect(x: 28, y: 575, width: 40, height: 40)
        orientationToggleButton.frame = NSRect(x: 33, y: 549,
                                                width: 30, height: 24)

        // S7 von y=2 bis y=65 (63 hoch). Stack:
        //   Enter oben bei y=27 (40 hoch, Mitte y=47)
        //   Save  unten bei y=4 (22 hoch)
        enterButton.frame = NSRect(x: 28, y: 27, width: 40, height: 40)
        saveButton.frame = NSRect(x: 33, y: 4, width: 30, height: 22)

        orientationToggleButton.alphaValue = 1.0
        saveButton.alphaValue = 1.0

        // 30% Durchsichtigkeit (70% Opacity) auf den Sektions-Hintergruenden,
        // wie auf Windows (Alpha-Praefix B3 in den Hex-Werten). Wirkt auf
        // alle existierenden Sektions-NSViews (die kein RoundButton sind).
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

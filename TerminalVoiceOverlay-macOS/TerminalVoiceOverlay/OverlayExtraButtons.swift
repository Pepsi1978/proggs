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

    /// Toggle-Button (⇄) zwischen vertikaler Saeule und horizontaler Leiste.
    /// Lazy: wird beim ersten Zugriff angelegt und ins contentView gepackt.
    var orientationToggleButton: RoundButton {
        if let b = objc_getAssociatedObject(self, &orientationToggleButtonKey)
            as? RoundButton {
            return b
        }
        let b = RoundButton(label: "\u{21C4}",
                            color: NSColor(red: 0.176, green: 0.176, blue: 0.176, alpha: 1),
                            width: 34, height: 34)
        b.labelFont = .systemFont(ofSize: 16, weight: .bold)
        b.labelColor = NSColor.white
        b.onClick = { [weak self] in self?.onOrientationToggleClicked?() }
        self.contentView?.addSubview(b)
        objc_setAssociatedObject(self, &orientationToggleButtonKey, b,
                                 .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return b
    }

    /// Save-Button (Diskette) — merkt die aktuelle Position pro Orientation.
    var saveButton: RoundButton {
        if let b = objc_getAssociatedObject(self, &saveButtonKey)
            as? RoundButton {
            return b
        }
        let b = RoundButton(label: "",
                            color: NSColor(red: 0.176, green: 0.176, blue: 0.176, alpha: 1),
                            width: 28, height: 28)
        b.symbolImage = NSImage(systemSymbolName: "externaldrive.fill",
                                accessibilityDescription: "Save position")
        b.labelColor = NSColor.white
        b.cornerRadius = 6
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
    /// Wird von applyVerticalLayout() aufgerufen, nachdem die Sektionen
    /// und die Standard-Buttons gesetzt sind.
    func positionExtraButtonsVertical() {
        // S1 (y=547, h=63): Stern bei (28, 553, 40, 40). OrientationToggle
        // rechts neben dem Stern, kleiner (34x34) auf gleicher Mittenhoehe.
        // Panel-Breite 96, Stern endet bei x=68. OrientationToggle bei (66, 555).
        // Aber das wird zu eng — wir setzen ihn unter dem Stern bei (28, 549, 22, 22)?
        // Stattdessen: Stern bleibt zentriert, OrientationToggle daneben links.
        orientationToggleButton.frame = NSRect(x: 4, y: 555,
                                                width: 22, height: 22)

        // S7 (y=2, h=63): Enter bei (28, 19, 40, 40). Save klein daneben links.
        saveButton.frame = NSRect(x: 4, y: 21, width: 22, height: 22)

        orientationToggleButton.alphaValue = 1.0
        saveButton.alphaValue = 1.0
    }

    /// Positioniert OrientationToggle + Save fuer die horizontale Leiste.
    func positionExtraButtonsHorizontal() {
        // Position wird aus den HBarSection-Slots berechnet — der
        // applyHorizontalLayout() ueberlaesst die Anordnung an die
        // Sektions-Slots .empty(...) die wir hier ersetzen. Fuer jetzt
        // setzen wir die Buttons absolut sichtbar in S1 und S7 — die
        // genauen Positionen kommen aus der finalen Sektions-Breite.
        // Pragma: aktuell HBar baut Sektionen rechts→links, S7 ganz links,
        // S1 ganz rechts. Konkret messen wir die Frames der Anker-Buttons
        // (enterButton, ultrathinkButton) und positionieren daneben.

        // Save neben Enter (Enter ist in S7 ganz links, in der oberen Reihe).
        let enterFrame = enterButton.frame
        saveButton.frame = NSRect(x: enterFrame.maxX + 4,
                                   y: enterFrame.minY + 8,
                                   width: 22, height: 22)

        // OrientationToggle neben Stern (S1 ganz rechts, obere Reihe).
        let starFrame = ultrathinkButton.frame
        orientationToggleButton.frame = NSRect(x: starFrame.minX - 26,
                                                y: starFrame.minY,
                                                width: 22, height: 22)

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

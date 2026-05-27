import AppKit

// MARK: - Horizontales Layout (Schwester-App vereinfacht)
// ClaudeCodexVoiceOverlay hat 9 Buttons in einer Pille — kein PromptBoard,
// keine Profile-Tiles, keine 7 Sektionen. Im Horizontal-Modus reihen wir
// alle 9 Buttons nebeneinander in einer flachen Leiste.
//
// Reihenfolge (links → rechts):
//   Enter → Paste → Copy → X → G → W → BTW → Mic → Stern
// (entspricht vertikaler Reihenfolge unten → oben, gespiegelt zur Leiste)

enum HBarLayoutCCX {
    static let panelHeight: CGFloat = 72
    static let verticalPadding: CGFloat = 8
    static let horizontalPadding: CGFloat = 10
    static let buttonSpacing: CGFloat = 6
    static let cornerRadius: CGFloat = 36
    static let actionButtonSize: CGFloat = 40
    static let micButtonSize: CGFloat    = 52
}

extension OverlayPanel {

    /// Wendet das horizontale Pillen-Layout an: alle 9 Buttons reihen sich
    /// in einer flachen Leiste. CornerRadius = panelHeight/2 → komplett
    /// runde Enden.
    func applyHorizontalLayout(at windowOrigin: NSPoint? = nil) {
        // Total-Breite: 7 actionButtons (40) + 2 micButtons (52) + 8 spacing
        let totalContent = 7 * HBarLayoutCCX.actionButtonSize
                         + 2 * HBarLayoutCCX.micButtonSize
                         + 8 * HBarLayoutCCX.buttonSpacing
        let totalWidth = totalContent + 2 * HBarLayoutCCX.horizontalPadding

        let origin: NSPoint = windowOrigin
            ?? savedHorizontalPosition
            ?? canonicalHorizontalOrigin(panelWidth: totalWidth)
        let newFrame = NSRect(x: origin.x, y: origin.y,
                              width: totalWidth, height: HBarLayoutCCX.panelHeight)
        self.setFrame(newFrame, display: false)
        self.contentView?.layer?.cornerRadius = HBarLayoutCCX.cornerRadius

        // Buttons reihen — links beginnt mit Enter.
        var cursor = HBarLayoutCCX.horizontalPadding
        let midY = HBarLayoutCCX.panelHeight / 2

        func place(_ button: RoundButton, size: CGFloat) {
            let y = midY - size / 2
            button.frame = NSRect(x: cursor, y: y, width: size, height: size)
            cursor += size + HBarLayoutCCX.buttonSpacing
        }

        place(enterButton, size: HBarLayoutCCX.actionButtonSize)
        place(pasteButton, size: HBarLayoutCCX.actionButtonSize)
        place(copyButton,  size: HBarLayoutCCX.actionButtonSize)
        place(xButton,     size: HBarLayoutCCX.actionButtonSize)
        place(gButton,     size: HBarLayoutCCX.actionButtonSize)
        place(wButton,     size: HBarLayoutCCX.actionButtonSize)
        place(btwButton,   size: HBarLayoutCCX.micButtonSize)
        place(micButton,   size: HBarLayoutCCX.micButtonSize)
        place(ultrathinkButton, size: HBarLayoutCCX.actionButtonSize)

        self.currentOrientation = .horizontal
    }

    /// Stellt das vertikale Pillen-Layout wieder her.
    /// (Vereinfacht — fuer die Schwester-App ist init() der Quell-Aufbau.)
    func applyVerticalLayout(at windowOrigin: NSPoint? = nil) {
        let btnSize: CGFloat = 40
        let micSize: CGFloat = 52
        let gap: CGFloat = 8
        let panelWidth: CGFloat = micSize + 20
        let panelHeight: CGFloat = btnSize * 7 + micSize * 2 + gap * 8 + 32

        let origin: NSPoint = windowOrigin
            ?? savedVerticalPosition
            ?? canonicalVerticalOrigin(panelHeight: panelHeight)
        self.setFrame(NSRect(x: origin.x, y: origin.y,
                             width: panelWidth, height: panelHeight),
                      display: false)
        self.contentView?.layer?.cornerRadius = panelWidth / 2

        // Buttons im Stack neu positionieren (bottom-up, gleich wie init()).
        let smallInset = (panelWidth - btnSize) / 2
        let micInset = (panelWidth - micSize) / 2
        var y: CGFloat = 16  // padding bottom
        func place(_ button: RoundButton, size: CGFloat, inset: CGFloat) {
            button.frame = NSRect(x: inset, y: y, width: size, height: size)
            y += size + gap
        }
        place(enterButton,      size: btnSize, inset: smallInset)
        place(pasteButton,      size: btnSize, inset: smallInset)
        place(copyButton,       size: btnSize, inset: smallInset)
        place(xButton,          size: btnSize, inset: smallInset)
        place(gButton,          size: btnSize, inset: smallInset)
        place(wButton,          size: btnSize, inset: smallInset)
        place(btwButton,        size: micSize, inset: micInset)
        place(micButton,        size: micSize, inset: micInset)
        place(ultrathinkButton, size: btnSize, inset: smallInset)

        self.currentOrientation = .vertical
    }

    /// Beam-Crossfade zwischen den beiden Layouts (gleiche Mechanik wie TVO).
    func beamToOrientation(_ target: OverlayOrientation,
                           completion: (() -> Void)? = nil) {
        guard target != currentOrientation else { completion?(); return }
        guard let cv = self.contentView else { completion?(); return }
        cv.beamFadeOut { [weak self] in
            guard let self = self else { return }
            switch target {
            case .horizontal:
                self.applyHorizontalLayout(at: self.savedHorizontalPosition)
            case .vertical:
                self.applyVerticalLayout(at: self.savedVerticalPosition)
            }
            cv.beamFadeIn { completion?() }
        }
    }

    /// Kanonische HBar-Position rechts-unten.
    func canonicalHorizontalOrigin(panelWidth: CGFloat) -> NSPoint {
        let screen = NSScreen.main?.visibleFrame ??
                     NSRect(x: 0, y: 0, width: 1920, height: 1080)
        return NSPoint(x: screen.maxX - panelWidth - 34,
                       y: screen.minY + 24)
    }
}

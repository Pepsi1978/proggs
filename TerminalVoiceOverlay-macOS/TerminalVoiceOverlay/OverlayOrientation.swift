import AppKit
import QuartzCore

// MARK: - Orientation State
// Portierung von TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs
// Commits #1063-#1088 (Stand 2026-05-25). Vergleiche OVERLAY-ANIMATION-SPEC.md.
//
// Etappe 1a: Beam-Fade-Helper + Orientation-State. Layout-Switch + Glide
// folgen in Etappe 1b/2/3.

enum OverlayOrientation: String {
    case vertical = "vertical"
    case horizontal = "horizontal"
}

/// Konstanten passend zur Windows-Version (OverlayWindow.xaml.cs Z. 656/1097/1110).
enum OverlayBeam {
    /// Zeit fuer das vollstaendige Ausblenden (1 -> 0).
    static let fadeOutDuration: CFTimeInterval = 0.240
    /// Zeit fuer das vollstaendige Einblenden (0 -> 1). Bewusst laenger als
    /// fadeOutDuration — der "Reinbeam" soll ruhig wirken.
    static let fadeInDuration: CFTimeInterval = 0.380
    /// Pixel-Abstand der Saeulen-Oberkante zur Arbeitsbereichs-Oberkante.
    static let verticalTopOffset: CGFloat = 57
}

// MARK: - UserDefaults Keys (per-Orientation gemerkte Positionen)
enum OverlayPositionKeys {
    static let orientation       = "overlayOrientation"
    static let verticalLeft      = "overlayVerticalLeft"
    static let verticalTop       = "overlayVerticalTop"
    static let horizontalLeft    = "overlayHorizontalLeft"
    static let horizontalTop     = "overlayHorizontalTop"
}

// MARK: - Beam-Fade-Helpers
// Aequivalent zu Windows' BeamFadeOut / BeamFadeIn. Nutzt
// NSAnimationContext + CAMediaTimingFunction(.easeInEaseOut) — selbe Kurve
// wie WPF CubicEase EaseInOut.
//
// WICHTIG (analog Windows Z. 1099): Vor dem Start jede bisherige Animation
// auf alphaValue stoppen. CALayer-Animationen koennen den effektiven Wert
// "halten" — wir setzen den Startwert daher explizit.

extension NSView {
    /// Vollstaendiges Ausblenden (alpha -> 0) ueber `OverlayBeam.fadeOutDuration`.
    /// `completion` feuert garantiert (auch bei Abbruch durch eine neue
    /// Animation auf denselben View).
    func beamFadeOut(completion: (() -> Void)? = nil) {
        layer?.removeAnimation(forKey: "opacity")
        alphaValue = 1.0
        NSAnimationContext.runAnimationGroup({ ctx in
            ctx.duration = OverlayBeam.fadeOutDuration
            ctx.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
            ctx.allowsImplicitAnimation = true
            animator().alphaValue = 0.0
        }, completionHandler: {
            completion?()
        })
    }

    /// Vollstaendiges Einblenden (alpha -> 1) ueber `OverlayBeam.fadeInDuration`.
    /// Setzt vor Start explizit auf 0 — eine bestehende Animation hat den
    /// effektiven Wert sonst evtl. nicht aktualisiert.
    func beamFadeIn(completion: (() -> Void)? = nil) {
        layer?.removeAnimation(forKey: "opacity")
        alphaValue = 0.0
        NSAnimationContext.runAnimationGroup({ ctx in
            ctx.duration = OverlayBeam.fadeInDuration
            ctx.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
            ctx.allowsImplicitAnimation = true
            animator().alphaValue = 1.0
        }, completionHandler: {
            completion?()
        })
    }
}

// MARK: - OverlayPanel Orientation-Stub
// Stellt den State + die kanonischen Positionen bereit. Das tatsaechliche
// Umschalten und Layout-Building kommt in Etappe 1b.
extension OverlayPanel {
    private struct OrientationKeys {
        static var currentOrientation = "tvo_currentOrientation"
        static var savedVerticalPos   = "tvo_savedVerticalPos"
        static var savedHorizontalPos = "tvo_savedHorizontalPos"
    }

    /// Aktueller Orientation-State (per UserDefaults persistiert).
    var currentOrientation: OverlayOrientation {
        get {
            let raw = UserDefaults.standard.string(forKey: OverlayPositionKeys.orientation) ?? "vertical"
            return OverlayOrientation(rawValue: raw) ?? .vertical
        }
        set {
            UserDefaults.standard.set(newValue.rawValue, forKey: OverlayPositionKeys.orientation)
        }
    }

    /// Per-Orientation gemerkte Position (Windows: _savedVerticalPos).
    /// `nil` = noch nie manuell positioniert -> kanonische Position nutzen.
    var savedVerticalPosition: NSPoint? {
        get {
            let d = UserDefaults.standard
            guard d.object(forKey: OverlayPositionKeys.verticalLeft) != nil,
                  d.object(forKey: OverlayPositionKeys.verticalTop)  != nil else { return nil }
            return NSPoint(x: d.double(forKey: OverlayPositionKeys.verticalLeft),
                           y: d.double(forKey: OverlayPositionKeys.verticalTop))
        }
        set {
            let d = UserDefaults.standard
            if let p = newValue {
                d.set(Double(p.x), forKey: OverlayPositionKeys.verticalLeft)
                d.set(Double(p.y), forKey: OverlayPositionKeys.verticalTop)
            } else {
                d.removeObject(forKey: OverlayPositionKeys.verticalLeft)
                d.removeObject(forKey: OverlayPositionKeys.verticalTop)
            }
        }
    }

    var savedHorizontalPosition: NSPoint? {
        get {
            let d = UserDefaults.standard
            guard d.object(forKey: OverlayPositionKeys.horizontalLeft) != nil,
                  d.object(forKey: OverlayPositionKeys.horizontalTop)  != nil else { return nil }
            return NSPoint(x: d.double(forKey: OverlayPositionKeys.horizontalLeft),
                           y: d.double(forKey: OverlayPositionKeys.horizontalTop))
        }
        set {
            let d = UserDefaults.standard
            if let p = newValue {
                d.set(Double(p.x), forKey: OverlayPositionKeys.horizontalLeft)
                d.set(Double(p.y), forKey: OverlayPositionKeys.horizontalTop)
            } else {
                d.removeObject(forKey: OverlayPositionKeys.horizontalLeft)
                d.removeObject(forKey: OverlayPositionKeys.horizontalTop)
            }
        }
    }

    /// Kanonische Position der vertikalen Saeule (Windows: ColumnTop / ColumnLeft).
    /// macOS hat Y-Ursprung unten — `visibleFrame.maxY` ist die Oberkante,
    /// also Saeulen-Oberkante = `visibleFrame.maxY - verticalTopOffset`.
    /// `panelHeight` muss vom Aufrufer mitgegeben werden (Konstante in init()).
    func canonicalVerticalOrigin(panelHeight: CGFloat) -> NSPoint {
        let screen = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1920, height: 1080)
        // Windows: ColumnLeft = waX + waW - 96 - 27. HARTCODIERTE Saeulenbreite 96 —
        // NICHT `frame.width` nutzen, weil das im HBar-Modus ~580 ist und dann
        // die Saeule beim Switch zurueck weit links vom Bildschirm landen wuerde.
        let x = screen.maxX - 96 - 27
        let topY = screen.maxY - OverlayBeam.verticalTopOffset
        let y = topY - panelHeight
        return NSPoint(x: x, y: y)
    }
}

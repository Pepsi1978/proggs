import AppKit

// MARK: - IconPaths
// Portierung der Windows-XAML SVG-Pfade (1:1 aus OverlayWindow.xaml) als
// NSBezierPath-NSImages. Ersetzt SF-Symbols wo das Windows-Original einen
// spezifischen Path hat — exakte visuelle Uebereinstimmung statt ~95%
// Approximation.
//
// Alle Pfade sind in einem 24x24-Koordinatensystem (Material Icons-Standard)
// definiert und werden via Viewbox auf die Ziel-Symbolgroesse skaliert
// (Windows: 18x18 oder 22x22 je nach Button).

enum IconPaths {

    /// Mic — Material "mic" Icon. XAML Path:
    /// `M12,14 C13.66,14 15,12.66 15,11 L15,5 C15,3.34 ... Z`
    static func mic() -> NSBezierPath {
        let p = NSBezierPath()
        // Kapsel oben (Mikrofon-Korpus)
        p.move(to: NSPoint(x: 12, y: 14))
        p.curve(to: NSPoint(x: 15, y: 11), controlPoint1: NSPoint(x: 13.66, y: 14), controlPoint2: NSPoint(x: 15, y: 12.66))
        p.line(to: NSPoint(x: 15, y: 5))
        p.curve(to: NSPoint(x: 12, y: 2), controlPoint1: NSPoint(x: 15, y: 3.34), controlPoint2: NSPoint(x: 13.66, y: 2))
        p.curve(to: NSPoint(x: 9, y: 5), controlPoint1: NSPoint(x: 10.34, y: 2), controlPoint2: NSPoint(x: 9, y: 3.34))
        p.line(to: NSPoint(x: 9, y: 11))
        p.curve(to: NSPoint(x: 12, y: 14), controlPoint1: NSPoint(x: 9, y: 12.66), controlPoint2: NSPoint(x: 10.34, y: 14))
        p.close()
        // Unterteil (Staender + Bogen)
        p.move(to: NSPoint(x: 17.3, y: 11))
        p.curve(to: NSPoint(x: 12, y: 16.1), controlPoint1: NSPoint(x: 17.3, y: 14), controlPoint2: NSPoint(x: 14.76, y: 16.1))
        p.curve(to: NSPoint(x: 6.7, y: 11), controlPoint1: NSPoint(x: 9.24, y: 16.1), controlPoint2: NSPoint(x: 6.7, y: 14))
        p.line(to: NSPoint(x: 5, y: 11))
        p.curve(to: NSPoint(x: 11, y: 17.72), controlPoint1: NSPoint(x: 5, y: 14.41), controlPoint2: NSPoint(x: 7.72, y: 17.23))
        p.line(to: NSPoint(x: 11, y: 21))
        p.line(to: NSPoint(x: 13, y: 21))
        p.line(to: NSPoint(x: 13, y: 17.72))
        p.curve(to: NSPoint(x: 19, y: 11), controlPoint1: NSPoint(x: 16.28, y: 17.23), controlPoint2: NSPoint(x: 19, y: 14.41))
        p.line(to: NSPoint(x: 17.3, y: 11))
        p.close()
        return p
    }

    /// Copy — Material "content_copy" Icon. XAML Path (CopyButton).
    static func copy() -> NSBezierPath {
        let p = NSBezierPath()
        // Hinteres Blatt (kleineres)
        p.move(to: NSPoint(x: 16, y: 1))
        p.line(to: NSPoint(x: 4, y: 1))
        p.curve(to: NSPoint(x: 2, y: 3), controlPoint1: NSPoint(x: 2.9, y: 1), controlPoint2: NSPoint(x: 2, y: 1.9))
        p.line(to: NSPoint(x: 2, y: 17))
        p.line(to: NSPoint(x: 4, y: 17))
        p.line(to: NSPoint(x: 4, y: 3))
        p.line(to: NSPoint(x: 16, y: 3))
        p.line(to: NSPoint(x: 16, y: 1))
        p.close()
        // Vorderes Blatt (groesseres)
        p.move(to: NSPoint(x: 19, y: 5))
        p.line(to: NSPoint(x: 8, y: 5))
        p.curve(to: NSPoint(x: 6, y: 7), controlPoint1: NSPoint(x: 6.9, y: 5), controlPoint2: NSPoint(x: 6, y: 5.9))
        p.line(to: NSPoint(x: 6, y: 21))
        p.curve(to: NSPoint(x: 8, y: 23), controlPoint1: NSPoint(x: 6, y: 22.1), controlPoint2: NSPoint(x: 6.9, y: 23))
        p.line(to: NSPoint(x: 19, y: 23))
        p.curve(to: NSPoint(x: 21, y: 21), controlPoint1: NSPoint(x: 20.1, y: 23), controlPoint2: NSPoint(x: 21, y: 22.1))
        p.line(to: NSPoint(x: 21, y: 7))
        p.curve(to: NSPoint(x: 19, y: 5), controlPoint1: NSPoint(x: 21, y: 5.9), controlPoint2: NSPoint(x: 20.1, y: 5))
        p.close()
        // Innenflaeche (transparent — durch even-odd)
        p.move(to: NSPoint(x: 19, y: 21))
        p.line(to: NSPoint(x: 8, y: 21))
        p.line(to: NSPoint(x: 8, y: 7))
        p.line(to: NSPoint(x: 19, y: 7))
        p.line(to: NSPoint(x: 19, y: 21))
        p.close()
        p.windingRule = .evenOdd
        return p
    }

    /// Paste — Material "content_paste" Icon. XAML Path.
    static func paste() -> NSBezierPath {
        let p = NSBezierPath()
        p.move(to: NSPoint(x: 19, y: 2))
        p.line(to: NSPoint(x: 14.82, y: 2))
        p.curve(to: NSPoint(x: 12, y: 0), controlPoint1: NSPoint(x: 14.4, y: 0.84), controlPoint2: NSPoint(x: 13.3, y: 0))
        p.curve(to: NSPoint(x: 9.18, y: 2), controlPoint1: NSPoint(x: 10.7, y: 0), controlPoint2: NSPoint(x: 9.6, y: 0.84))
        p.line(to: NSPoint(x: 5, y: 2))
        p.curve(to: NSPoint(x: 3, y: 4), controlPoint1: NSPoint(x: 3.9, y: 2), controlPoint2: NSPoint(x: 3, y: 2.9))
        p.line(to: NSPoint(x: 3, y: 20))
        p.curve(to: NSPoint(x: 5, y: 22), controlPoint1: NSPoint(x: 3, y: 21.1), controlPoint2: NSPoint(x: 3.9, y: 22))
        p.line(to: NSPoint(x: 19, y: 22))
        p.curve(to: NSPoint(x: 21, y: 20), controlPoint1: NSPoint(x: 20.1, y: 22), controlPoint2: NSPoint(x: 21, y: 21.1))
        p.line(to: NSPoint(x: 21, y: 4))
        p.curve(to: NSPoint(x: 19, y: 2), controlPoint1: NSPoint(x: 21, y: 2.9), controlPoint2: NSPoint(x: 20.1, y: 2))
        p.close()
        // Pin oben
        p.move(to: NSPoint(x: 12, y: 2))
        p.curve(to: NSPoint(x: 13, y: 3), controlPoint1: NSPoint(x: 12.55, y: 2), controlPoint2: NSPoint(x: 13, y: 2.45))
        p.curve(to: NSPoint(x: 12, y: 4), controlPoint1: NSPoint(x: 13, y: 3.55), controlPoint2: NSPoint(x: 12.55, y: 4))
        p.curve(to: NSPoint(x: 11, y: 3), controlPoint1: NSPoint(x: 11.45, y: 4), controlPoint2: NSPoint(x: 11, y: 3.55))
        p.curve(to: NSPoint(x: 12, y: 2), controlPoint1: NSPoint(x: 11, y: 2.45), controlPoint2: NSPoint(x: 11.45, y: 2))
        p.close()
        // Innenflaeche
        p.move(to: NSPoint(x: 19, y: 20))
        p.line(to: NSPoint(x: 5, y: 20))
        p.line(to: NSPoint(x: 5, y: 4))
        p.line(to: NSPoint(x: 7, y: 4))
        p.line(to: NSPoint(x: 7, y: 7))
        p.line(to: NSPoint(x: 17, y: 7))
        p.line(to: NSPoint(x: 17, y: 4))
        p.line(to: NSPoint(x: 19, y: 4))
        p.line(to: NSPoint(x: 19, y: 20))
        p.close()
        p.windingRule = .evenOdd
        return p
    }

    /// Screenshot — Material "camera" Icon. XAML Path.
    static func screenshot() -> NSBezierPath {
        let p = NSBezierPath()
        // Aussenkontur (Kamera-Korpus)
        p.move(to: NSPoint(x: 9, y: 2))
        p.line(to: NSPoint(x: 7.17, y: 4))
        p.line(to: NSPoint(x: 4, y: 4))
        p.curve(to: NSPoint(x: 2, y: 6), controlPoint1: NSPoint(x: 2.9, y: 4), controlPoint2: NSPoint(x: 2, y: 4.9))
        p.line(to: NSPoint(x: 2, y: 18))
        p.curve(to: NSPoint(x: 4, y: 20), controlPoint1: NSPoint(x: 2, y: 19.1), controlPoint2: NSPoint(x: 2.9, y: 20))
        p.line(to: NSPoint(x: 20, y: 20))
        p.curve(to: NSPoint(x: 22, y: 18), controlPoint1: NSPoint(x: 21.1, y: 20), controlPoint2: NSPoint(x: 22, y: 19.1))
        p.line(to: NSPoint(x: 22, y: 6))
        p.curve(to: NSPoint(x: 20, y: 4), controlPoint1: NSPoint(x: 22, y: 4.9), controlPoint2: NSPoint(x: 21.1, y: 4))
        p.line(to: NSPoint(x: 16.83, y: 4))
        p.line(to: NSPoint(x: 15, y: 2))
        p.close()
        // Aeusserer Linsenring
        p.move(to: NSPoint(x: 12, y: 17))
        p.curve(to: NSPoint(x: 7, y: 12), controlPoint1: NSPoint(x: 9.24, y: 17), controlPoint2: NSPoint(x: 7, y: 14.76))
        p.curve(to: NSPoint(x: 12, y: 7), controlPoint1: NSPoint(x: 7, y: 9.24), controlPoint2: NSPoint(x: 9.24, y: 7))
        p.curve(to: NSPoint(x: 17, y: 12), controlPoint1: NSPoint(x: 14.76, y: 7), controlPoint2: NSPoint(x: 17, y: 9.24))
        p.curve(to: NSPoint(x: 12, y: 17), controlPoint1: NSPoint(x: 17, y: 14.76), controlPoint2: NSPoint(x: 14.76, y: 17))
        p.close()
        // Innere Linsenscheibe (Ausgespart per even-odd)
        p.move(to: NSPoint(x: 12, y: 9))
        p.curve(to: NSPoint(x: 9, y: 12), controlPoint1: NSPoint(x: 10.34, y: 9), controlPoint2: NSPoint(x: 9, y: 10.34))
        p.curve(to: NSPoint(x: 12, y: 15), controlPoint1: NSPoint(x: 9, y: 13.66), controlPoint2: NSPoint(x: 10.34, y: 15))
        p.curve(to: NSPoint(x: 15, y: 12), controlPoint1: NSPoint(x: 13.66, y: 15), controlPoint2: NSPoint(x: 15, y: 13.66))
        p.curve(to: NSPoint(x: 12, y: 9), controlPoint1: NSPoint(x: 15, y: 10.34), controlPoint2: NSPoint(x: 13.66, y: 9))
        p.close()
        p.windingRule = .evenOdd
        return p
    }

    /// Rendert einen Pfad (Koordinatensystem 24x24) als NSImage mit
    /// der angegebenen Zielgroesse + Fuell-Farbe.
    static func renderImage(path: NSBezierPath, size: NSSize, fill: NSColor) -> NSImage {
        let img = NSImage(size: size)
        img.lockFocus()
        // Y-Achse umkehren (XAML-Path ist Y-down, NSBezierPath ist Y-up
        // wenn wir Default-Flip nutzen — wir rotieren in eine 24x24-Box,
        // skalieren auf size, und kompensieren die Y-Flip).
        let ctx = NSGraphicsContext.current
        ctx?.cgContext.translateBy(x: 0, y: size.height)
        ctx?.cgContext.scaleBy(x: size.width / 24.0, y: -(size.height / 24.0))
        fill.set()
        path.fill()
        img.unlockFocus()
        return img
    }
}

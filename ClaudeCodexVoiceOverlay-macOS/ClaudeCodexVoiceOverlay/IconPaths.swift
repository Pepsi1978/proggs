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

    /// Stern (Pentagram) — XAML Path:
    /// `M12,1 L15.09,7.26 L22,8.27 L17,13.14 L18.18,20.02 L12,16.77 L5.82,20.02 L7,13.14 L2,8.27 L8.91,7.26 Z`
    static func star() -> NSBezierPath {
        let p = NSBezierPath()
        p.move(to: NSPoint(x: 12, y: 1))
        p.line(to: NSPoint(x: 15.09, y: 7.26))
        p.line(to: NSPoint(x: 22, y: 8.27))
        p.line(to: NSPoint(x: 17, y: 13.14))
        p.line(to: NSPoint(x: 18.18, y: 20.02))
        p.line(to: NSPoint(x: 12, y: 16.77))
        p.line(to: NSPoint(x: 5.82, y: 20.02))
        p.line(to: NSPoint(x: 7, y: 13.14))
        p.line(to: NSPoint(x: 2, y: 8.27))
        p.line(to: NSPoint(x: 8.91, y: 7.26))
        p.close()
        return p
    }

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

    /// Attach (Paperclip) — Material "attach_file" Icon. Approximiert das
    /// Segoe MDL2 E723-Glyph aus dem Windows-XAML (InsertScreenshotButton).
    static func attach() -> NSBezierPath {
        let p = NSBezierPath()
        // Buegel aussen
        p.move(to: NSPoint(x: 16.5, y: 6))
        p.line(to: NSPoint(x: 16.5, y: 17.5))
        p.curve(to: NSPoint(x: 12.5, y: 21.5),
                controlPoint1: NSPoint(x: 16.5, y: 19.71),
                controlPoint2: NSPoint(x: 14.71, y: 21.5))
        p.curve(to: NSPoint(x: 8.5, y: 17.5),
                controlPoint1: NSPoint(x: 10.29, y: 21.5),
                controlPoint2: NSPoint(x: 8.5, y: 19.71))
        p.line(to: NSPoint(x: 8.5, y: 5))
        p.curve(to: NSPoint(x: 11, y: 2.5),
                controlPoint1: NSPoint(x: 8.5, y: 3.62),
                controlPoint2: NSPoint(x: 9.62, y: 2.5))
        p.curve(to: NSPoint(x: 13.5, y: 5),
                controlPoint1: NSPoint(x: 12.38, y: 2.5),
                controlPoint2: NSPoint(x: 13.5, y: 3.62))
        p.line(to: NSPoint(x: 13.5, y: 15.5))
        p.curve(to: NSPoint(x: 12, y: 17),
                controlPoint1: NSPoint(x: 13.5, y: 16.33),
                controlPoint2: NSPoint(x: 12.83, y: 17))
        p.curve(to: NSPoint(x: 10.5, y: 15.5),
                controlPoint1: NSPoint(x: 11.17, y: 17),
                controlPoint2: NSPoint(x: 10.5, y: 16.33))
        p.line(to: NSPoint(x: 10.5, y: 6))
        p.line(to: NSPoint(x: 9, y: 6))
        p.line(to: NSPoint(x: 9, y: 15.5))
        p.curve(to: NSPoint(x: 12, y: 18.5),
                controlPoint1: NSPoint(x: 9, y: 17.16),
                controlPoint2: NSPoint(x: 10.34, y: 18.5))
        p.curve(to: NSPoint(x: 15, y: 15.5),
                controlPoint1: NSPoint(x: 13.66, y: 18.5),
                controlPoint2: NSPoint(x: 15, y: 17.16))
        p.line(to: NSPoint(x: 15, y: 5))
        p.curve(to: NSPoint(x: 11, y: 1),
                controlPoint1: NSPoint(x: 15, y: 2.79),
                controlPoint2: NSPoint(x: 13.21, y: 1))
        p.curve(to: NSPoint(x: 7, y: 5),
                controlPoint1: NSPoint(x: 8.79, y: 1),
                controlPoint2: NSPoint(x: 7, y: 2.79))
        p.line(to: NSPoint(x: 7, y: 17.5))
        p.curve(to: NSPoint(x: 12.5, y: 23),
                controlPoint1: NSPoint(x: 7, y: 20.54),
                controlPoint2: NSPoint(x: 9.46, y: 23))
        p.curve(to: NSPoint(x: 18, y: 17.5),
                controlPoint1: NSPoint(x: 15.54, y: 23),
                controlPoint2: NSPoint(x: 18, y: 20.54))
        p.line(to: NSPoint(x: 18, y: 6))
        p.close()
        return p
    }

    /// Diskette (Save) — Material "save" Icon. Approximiert das Segoe MDL2
    /// E74E-Glyph aus dem Windows-XAML (SaveButton).
    static func save() -> NSBezierPath {
        let p = NSBezierPath()
        // Aussenkontur
        p.move(to: NSPoint(x: 17, y: 3))
        p.line(to: NSPoint(x: 5, y: 3))
        p.curve(to: NSPoint(x: 3, y: 5),
                controlPoint1: NSPoint(x: 3.89, y: 3),
                controlPoint2: NSPoint(x: 3, y: 3.9))
        p.line(to: NSPoint(x: 3, y: 19))
        p.curve(to: NSPoint(x: 5, y: 21),
                controlPoint1: NSPoint(x: 3, y: 20.1),
                controlPoint2: NSPoint(x: 3.89, y: 21))
        p.line(to: NSPoint(x: 19, y: 21))
        p.curve(to: NSPoint(x: 21, y: 19),
                controlPoint1: NSPoint(x: 20.1, y: 21),
                controlPoint2: NSPoint(x: 21, y: 20.1))
        p.line(to: NSPoint(x: 21, y: 7))
        p.line(to: NSPoint(x: 17, y: 3))
        p.close()
        // Inneres Label (oben)
        p.move(to: NSPoint(x: 12, y: 19))
        p.curve(to: NSPoint(x: 9, y: 16),
                controlPoint1: NSPoint(x: 10.34, y: 19),
                controlPoint2: NSPoint(x: 9, y: 17.66))
        p.curve(to: NSPoint(x: 12, y: 13),
                controlPoint1: NSPoint(x: 9, y: 14.34),
                controlPoint2: NSPoint(x: 10.34, y: 13))
        p.curve(to: NSPoint(x: 15, y: 16),
                controlPoint1: NSPoint(x: 13.66, y: 13),
                controlPoint2: NSPoint(x: 15, y: 14.34))
        p.curve(to: NSPoint(x: 12, y: 19),
                controlPoint1: NSPoint(x: 15, y: 17.66),
                controlPoint2: NSPoint(x: 13.66, y: 19))
        p.close()
        // Schieber (oben)
        p.move(to: NSPoint(x: 15, y: 9))
        p.line(to: NSPoint(x: 5, y: 9))
        p.line(to: NSPoint(x: 5, y: 5))
        p.line(to: NSPoint(x: 15, y: 5))
        p.line(to: NSPoint(x: 15, y: 9))
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

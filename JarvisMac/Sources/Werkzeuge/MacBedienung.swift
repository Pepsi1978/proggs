import AppKit
import CoreGraphics
import Foundation

/// Computer Use: Maus am Mac (Tastatur macht mac_tastatur). Koordinaten sind Pixel im letzten Bild aus mac_bildschirm.
enum MacBedienung {
    /// Größe des letzten Bildes; nur über [merkeBild] und [punkt] benutzen.
    private static let sperre = NSLock()
    nonisolated(unsafe) private static var bild = CGSize.zero

    static func merkeBild(_ daten: Data) -> CGSize {
        let g = NSBitmapImageRep(data: daten).map { CGSize(width: $0.pixelsWide, height: $0.pixelsHigh) } ?? .zero
        sperre.withLock { bild = g }
        return g
    }

    /// Bild-Pixel auf Bildschirm-Punkte (mac_bildschirm zeigt den Hauptbildschirm); nil, solange es kein Bild gibt.
    private static func punkt(_ x: Double, _ y: Double) -> CGPoint? {
        let g = sperre.withLock { bild }
        guard g.width > 0, g.height > 0 else { return nil }
        let r = CGDisplayBounds(CGMainDisplayID())
        return CGPoint(x: r.minX + min(max(x / g.width, 0), 0.9999) * r.width, y: r.minY + min(max(y / g.height, 0), 0.9999) * r.height)
    }

    private static func maus(_ typ: CGEventType, _ p: CGPoint, _ knopf: CGMouseButton = .left, klicks: Int64 = 1) {
        let e = CGEvent(mouseEventSource: CGEventSource(stateID: .hidSystemState), mouseType: typ, mouseCursorPosition: p, mouseButton: knopf)
        e?.setIntegerValueField(.mouseEventClickState, value: klicks)
        e?.post(tap: .cghidEventTap)
    }

    /// Führt die Aktion aus; Rückgabe ist die Fehlermeldung oder nil.
    static func ausfuehren(_ a: JSONObjekt) async -> String? {
        guard AXIsProcessTrusted() else { return "Jarvis fehlt das Bedienungshilfen-Recht (Einstellungen → Mac & Rechte)." }
        func zahl(_ k: String) -> Double? { (a[k] as? Double) ?? (a[k] as? Int).map(Double.init) }
        let aktion = a["aktion"] as? String ?? ""
        if aktion == "scrollen", zahl("x") == nil {
            scrollen(a, zahl); return nil
        }
        guard let x = zahl("x"), let y = zahl("y") else { return "\(aktion) braucht x und y." }
        guard let p = punkt(x, y) else { return "Hol zuerst mit mac_bildschirm ein Bild; Koordinaten beziehen sich darauf." }
        maus(.mouseMoved, p)
        switch aktion {
        case "bewegen": break
        case "klick", "doppelklick":
            for k in 1...(aktion == "klick" ? 1 : 2) {
                maus(.leftMouseDown, p, klicks: Int64(k)); maus(.leftMouseUp, p, klicks: Int64(k))
            }
        case "rechtsklick":
            maus(.rightMouseDown, p, .right); maus(.rightMouseUp, p, .right)
        case "ziehen":
            guard let zx = zahl("bis_x"), let zy = zahl("bis_y") else { return "ziehen braucht bis_x und bis_y." }
            maus(.leftMouseDown, p)
            for i in 1...12 {
                if let q = punkt(x + (zx - x) * Double(i) / 12, y + (zy - y) * Double(i) / 12) { maus(.leftMouseDragged, q) }
                try? await Task.sleep(for: .milliseconds(15))
            }
            if let q = punkt(zx, zy) { maus(.leftMouseUp, q) }
        case "scrollen": scrollen(a, zahl)
        default: return "Unbekannte Aktion \(aktion)."
        }
        return nil
    }

    private static func scrollen(_ a: JSONObjekt, _ zahl: (String) -> Double?) {
        CGEvent(scrollWheelEvent2Source: CGEventSource(stateID: .hidSystemState), units: .pixel, wheelCount: 2,
                wheel1: Int32(-(zahl("scroll_y") ?? 0)), wheel2: Int32(-(zahl("scroll_x") ?? 0)), wheel3: 0)?.post(tap: .cghidEventTap)
    }
}

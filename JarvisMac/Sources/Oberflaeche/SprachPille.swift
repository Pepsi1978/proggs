import AppKit
import QuartzCore

/// Die schwebende Sprachpille, wie die Sprachblase am Handy (SprachBlase.kt): „Hey Jarvis“ aus einem anderen Programm öffnet Jarvis nicht, das Gespräch
/// läuft im Hintergrund, und oben mittig zeigt eine kleine Pille ein Pegel-Spektrum in Orange. Sprechen Frank oder Jarvis, zittert es nach dem Pegel,
/// dazwischen eine ruhige Welle. Klick öffnet Jarvis, das X, langes Drücken, Rechtsklick oder Ziehen nach oben beendet das Gespräch; auch während Live.
/// Nimmt nie den Fokus und zeichnet direkt statt über SwiftUI: leicht, damit Spracherkennung und Ausgabe nichts davon merken.
@MainActor
final class SprachPille {
    private final class Panel: NSPanel {
        override var canBecomeKey: Bool { false }
        override var canBecomeMain: Bool { false }
    }

    private var panel: NSPanel?
    private var ansicht: PillenAnsicht?
    private var sichtbar = false
    /// Laufende Aufnahmen: Werkzeuge können parallel laufen, erst die letzte holt die Pille zurück.
    private var aufnahmen = 0

    func setze(sichtbar neu: Bool) {
        guard neu != sichtbar else { return }
        sichtbar = neu
        anwenden()
    }

    func fuerAufnahme(ausblenden: Bool) async {
        aufnahmen = max(0, aufnahmen + (ausblenden ? 1 : -1))
        anwenden()
        // Der Fensterserver braucht einen Moment, bis die Pille wirklich weg ist.
        if ausblenden && sichtbar { try? await Task.sleep(for: .milliseconds(80)) }
    }

    private func anwenden() {
        guard sichtbar && aufnahmen == 0 else {
            ansicht?.laufen(false)
            panel?.orderOut(nil)
            return
        }
        let p = panel ?? anlegen()
        if let schirm = (NSScreen.main ?? NSScreen.screens.first)?.visibleFrame {
            p.setFrameOrigin(NSPoint(x: schirm.midX - PillenAnsicht.breite / 2, y: schirm.maxY - PillenAnsicht.hoehe - 6))
        }
        p.orderFrontRegardless()
        ansicht?.laufen(true)
    }

    private func anlegen() -> NSPanel {
        let p = Panel(contentRect: NSRect(x: 0, y: 0, width: PillenAnsicht.breite, height: PillenAnsicht.hoehe),
                      styleMask: [.nonactivatingPanel, .borderless], backing: .buffered, defer: false)
        p.isFloatingPanel = true
        p.level = .statusBar
        p.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary, .ignoresCycle]
        p.backgroundColor = .clear
        p.isOpaque = false
        p.hasShadow = true
        p.hidesOnDeactivate = false
        p.isMovable = false
        p.sharingType = .none
        p.title = "Jarvis-Sprachpille"
        let a = PillenAnsicht(frame: NSRect(x: 0, y: 0, width: PillenAnsicht.breite, height: PillenAnsicht.hoehe))
        p.contentView = a
        panel = p
        ansicht = a
        return p
    }
}

private final class PillenAnsicht: NSView {
    static let breite: CGFloat = 150
    static let hoehe: CGFloat = 44
    private static let balken = 13
    /// Das X rechts, Mittelpunkt und Radius; geklickt werden darf der ganze rechte Rand.
    private static let kreuzAbstand: CGFloat = 22
    private static let kreuzRadius: CGFloat = 11

    // Immer leuchtendes Orange (#FF6D00), wie am Handy; oben im Balken zu Gelb-Orange aufgehellt.
    private static let farbraum = CGColorSpaceCreateDeviceRGB()
    private static let orange = CGColor(srgbRed: 1, green: 109 / 255, blue: 0, alpha: 1)
    private static let randFarbe = CGColor(srgbRed: 1, green: 109 / 255, blue: 0, alpha: 0xD0 / 255)
    private static let flaeche = CGGradient(colorsSpace: farbraum, colors: [
        CGColor(srgbRed: 0x24 / 255, green: 0x1A / 255, blue: 0x10 / 255, alpha: 0xF2 / 255),
        CGColor(srgbRed: 0x0E / 255, green: 0x0A / 255, blue: 0x06 / 255, alpha: 0xF2 / 255),
    ] as CFArray, locations: [0, 1])!
    private static let glanz = CGGradient(colorsSpace: farbraum, colors: [
        CGColor(srgbRed: 1, green: 1, blue: 1, alpha: 0x22 / 255), CGColor(srgbRed: 1, green: 1, blue: 1, alpha: 0),
    ] as CFArray, locations: [0, 1])!
    private static let strich = CGGradient(colorsSpace: farbraum, colors: [
        CGColor(srgbRed: 1, green: 139 / 255, blue: 0, alpha: 1), orange,
    ] as CFArray, locations: [0, 1])!

    private var werte = [CGFloat](repeating: 0, count: balken)
    private var zuletzt = CACurrentMediaTime()
    private var uhr: CADisplayLink?
    private var startPunkt: NSPoint?
    private var langDruck: Task<Void, Never>?

    override init(frame: NSRect) {
        super.init(frame: frame)
        toolTip = "Jarvis hört zu. Klick öffnet Jarvis, das X oder lange drücken beendet das Gespräch."
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }

    override var isOpaque: Bool { false }
    override func acceptsFirstMouse(for event: NSEvent?) -> Bool { true }
    override var mouseDownCanMoveWindow: Bool { false }

    func laufen(_ an: Bool) {
        uhr?.invalidate()
        uhr = nil
        guard an else { return }
        zuletzt = CACurrentMediaTime()
        let u = displayLink(target: self, selector: #selector(takt))
        u.preferredFrameRateRange = CAFrameRateRange(minimum: 20, maximum: 30, preferred: 30)
        u.add(to: .main, forMode: .common)
        uhr = u
    }

    @objc private func takt() { needsDisplay = true }

    // MARK: Bedienung

    override func mouseDown(with event: NSEvent) {
        if convert(event.locationInWindow, from: nil).x >= bounds.width - Self.kreuzAbstand * 2 { beenden(); return }
        startPunkt = event.locationInWindow
        langDruck = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(500))
            guard !Task.isCancelled else { return }
            self?.beenden()
        }
    }

    override func mouseDragged(with event: NSEvent) {
        guard let s = startPunkt, event.locationInWindow.y - s.y > 12 else { return }
        beenden()
    }

    override func mouseUp(with event: NSEvent) {
        langDruck?.cancel()
        guard startPunkt != nil else { return }
        startPunkt = nil
        AppSteuerung.shared.pilleOeffnen()
    }

    override func rightMouseDown(with event: NSEvent) { beenden() }

    private func beenden() {
        langDruck?.cancel()
        startPunkt = nil
        NSHapticFeedbackManager.defaultPerformer.perform(.generic, performanceTime: .now)
        if LiveGespraech.shared.aktiv {
            log("live", "Live-Gespräch beendet", wo: "Sprachpille")
            LiveGespraech.shared.beenden()
            return
        }
        log("sprache", "Sprachsitzung beendet", wo: "Sprachpille")
        SprachSchleife.shared.beenden()
    }

    // MARK: Zeichnen

    override func draw(_ dirtyRect: NSRect) {
        guard let ctx = NSGraphicsContext.current?.cgContext else { return }
        let jetzt = CACurrentMediaTime()
        let schritt = CGFloat(min(max(jetzt - zuletzt, 0.001), 0.1))
        zuletzt = jetzt
        let t = CGFloat(jetzt)

        let live = LiveGespraech.shared
        let frank = SprachSchleife.shared.zustand == .hoert && Diktat.shared.aktiv && !Diktat.shared.schreibtMit
        let pegel = live.aktiv ? CGFloat(live.zustand == .spricht ? live.pegelAus : live.pegelEin)
                               : CGFloat(frank ? Diktat.shared.pegel : (Stimme.shared.spricht ? Stimme.shared.pegel : 0))

        let w = bounds.width, h = bounds.height
        let form = CGPath(roundedRect: bounds.insetBy(dx: 0.5, dy: 0.5), cornerWidth: h / 2 - 0.5, cornerHeight: h / 2 - 0.5, transform: nil)
        ctx.saveGState()
        ctx.addPath(form)
        ctx.clip()
        ctx.drawLinearGradient(Self.flaeche, start: CGPoint(x: 0, y: h), end: .zero, options: [])
        ctx.drawLinearGradient(Self.glanz, start: CGPoint(x: 0, y: h), end: CGPoint(x: 0, y: h / 2), options: [])
        ctx.restoreGState()
        ctx.addPath(form)
        ctx.setStrokeColor(Self.randFarbe)
        ctx.setLineWidth(1)
        ctx.strokePath()

        let abstand: CGFloat = 5.6
        let start = (w - Self.kreuzAbstand) / 2 - abstand * CGFloat(Self.balken - 1) / 2
        let maxHoehe = h * 0.62
        let mitte = h / 2
        let breite: CGFloat = 3.2
        let linien = CGMutablePath()
        for i in 0..<Self.balken {
            let n = CGFloat(i)
            let lage = (n - CGFloat(Self.balken - 1) / 2) / (CGFloat(Self.balken - 1) / 2)
            let huelle = 1 - 0.55 * lage * lage
            // Jeder Balken mit eigener Phase: Das Spektrum zittert nach dem Pegel, nicht alle gleich.
            let zittern = 0.55 + 0.45 * sin(t * (7 + n * 1.3) + n * 2.1)
            let ziel = pegel > 0.02 ? min(pegel * 1.25, 1) * huelle * zittern
                                    : 0.10 + 0.07 * sin(t * 2.4 - n * 0.55)   // ruhige Welle: hört noch zu
            let tempo: CGFloat = ziel > werte[i] ? 22 : 7
            werte[i] += (ziel - werte[i]) * min(1, schritt * tempo)
            let hb = max(breite, werte[i] * maxHoehe)
            let x = start + n * abstand
            linien.move(to: CGPoint(x: x, y: mitte - hb / 2 + breite / 2))
            linien.addLine(to: CGPoint(x: x, y: mitte + hb / 2 - breite / 2))
        }
        ctx.saveGState()
        ctx.addPath(linien.copy(strokingWithWidth: breite, lineCap: .round, lineJoin: .round, miterLimit: 1))
        ctx.clip()
        ctx.drawLinearGradient(Self.strich, start: CGPoint(x: 0, y: mitte + maxHoehe / 2), end: CGPoint(x: 0, y: mitte - maxHoehe / 2),
                               options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
        ctx.restoreGState()

        let kreuz = CGPoint(x: w - Self.kreuzAbstand, y: mitte)
        let r = Self.kreuzRadius
        ctx.setFillColor(Self.orange.copy(alpha: 0.2)!)
        ctx.fillEllipse(in: CGRect(x: kreuz.x - r, y: kreuz.y - r, width: r * 2, height: r * 2))
        ctx.setStrokeColor(Self.orange)
        ctx.setLineWidth(2)
        ctx.setLineCap(.round)
        let arm: CGFloat = 4.5
        ctx.move(to: CGPoint(x: kreuz.x - arm, y: kreuz.y - arm)); ctx.addLine(to: CGPoint(x: kreuz.x + arm, y: kreuz.y + arm))
        ctx.move(to: CGPoint(x: kreuz.x - arm, y: kreuz.y + arm)); ctx.addLine(to: CGPoint(x: kreuz.x + arm, y: kreuz.y - arm))
        ctx.strokePath()
    }
}

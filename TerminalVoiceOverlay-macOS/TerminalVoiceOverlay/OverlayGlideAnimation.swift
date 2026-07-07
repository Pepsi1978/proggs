import AppKit
import QuartzCore

// MARK: - Glide-Animation (CVDisplayLink + Smootherstep)
// Portierung von TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs
// Z. 1698+ (flache horizontale Leiste: kleines Fenster pro Frame
// verschieben mit Smootherstep). Auf macOS braucht es keinen DwmFlush —
// der WindowServer komponiert Fenster-Moves bereits gegen vsync. Wir
// nutzen einen vsync-getakteten CVDisplayLink fuer perfekt gleichmaessige
// Frames.
//
// WICHTIG (analog Windows Spec): NUR die flache Leiste rutschen — die
// hohe Saeule wird nie als Window bewegt. Diese Datei stellt nur den
// Mechanismus bereit, der Aufrufer entscheidet welche Form rutscht.

enum OverlayGlide {
    /// Minimale Dauer einer Glide-Animation (kurze Strecken).
    static let minDuration: CFTimeInterval = 0.340
    /// Maximale Dauer (lange Strecken).
    static let maxDuration: CFTimeInterval = 0.600
    /// Distanz die zur maxDuration fuehrt — laengere Strecken werden geclamped.
    static let maxDistance: CGFloat = 600

    /// Smootherstep-Easing aus Windows-Code (Z. 1716):
    ///   `e = t * t * t * (t * (t * 6 - 15) + 10)`
    /// Gibt einen weichen S-Verlauf mit Anfangs- UND Endbeschleunigung 0.
    static func smootherstep(_ t: CGFloat) -> CGFloat {
        let tt = max(0, min(1, t))
        return tt * tt * tt * (tt * (tt * 6 - 15) + 10)
    }

    /// Dauer abhaengig von der Strecke (kurze: minDuration, lange: maxDuration).
    static func durationFor(distance: CGFloat) -> CFTimeInterval {
        let clamped = min(max(abs(distance), 0), maxDistance)
        let ratio = Double(clamped / maxDistance)
        return minDuration + (maxDuration - minDuration) * ratio
    }
}

// MARK: - Glide-Engine
extension OverlayPanel {

    /// Verschiebt das Panel sanft per Smootherstep zu `targetOrigin`. Die
    /// Animation ist vsync-gebunden via CVDisplayLink — pro Tick wird der
    /// Smootherstep-Wert berechnet und das Fenster verschoben.
    ///
    /// Wenn waehrend einer laufenden Glide ein neuer Aufruf erfolgt, wird
    /// die alte abgebrochen und sofort vom aktuellen Frame neu gestartet
    /// (kein Sprung, kein Stop).
    func glideWindow(to targetOrigin: NSPoint,
                     completion: (() -> Void)? = nil) {
        // Bestehende Glide stoppen — ohne Stop greift die alte CVDisplayLink-
        // Animation noch ein paar Ticks weiter und schiebt unter uns.
        stopActiveGlide()

        let startOrigin = self.frame.origin
        let dx = targetOrigin.x - startOrigin.x
        let dy = targetOrigin.y - startOrigin.y
        let distance = sqrt(dx * dx + dy * dy)

        // Bei winziger Strecke: kein Glide, direkt setzen.
        guard distance > 1.0 else {
            self.setFrameOrigin(targetOrigin)
            completion?()
            return
        }

        let duration = OverlayGlide.durationFor(distance: distance)
        let startTime = CACurrentMediaTime()

        // CVDisplayLink fuer vsync-getaktete Updates. Auf macOS ist das
        // die richtige API fuer "ruckelfreie pro-Frame-Updates". Alternative
        // waere NSAnimationContext + window.animator() — funktioniert auch,
        // erlaubt aber keinen eigenen Smootherstep.
        var displayLink: CVDisplayLink?
        guard CVDisplayLinkCreateWithActiveCGDisplays(&displayLink) == kCVReturnSuccess,
              let link = displayLink else {
            // Fallback ohne CVDisplayLink: einmal direkt setzen.
            self.setFrameOrigin(targetOrigin)
            completion?()
            return
        }

        let state = GlideState(panel: self,
                               startOrigin: startOrigin,
                               targetOrigin: targetOrigin,
                               startTime: startTime,
                               duration: duration,
                               completion: completion,
                               displayLink: link)
        self.activeGlideState = state

        CVDisplayLinkSetOutputHandler(link) { _, _, _, _, _ in
            // Achtung: dieser Callback laeuft auf einem Display-Thread,
            // nicht auf der Main-Queue. NSWindow-Mutationen MUESSEN auf
            // der Main-Queue passieren.
            DispatchQueue.main.async {
                state.tick()
            }
            return kCVReturnSuccess
        }
        CVDisplayLinkStart(link)
    }

    /// Bricht eine laufende Glide ab (Callback wird NICHT ausgefuehrt).
    /// Aufrufer der "Glide jetzt durch und mit Completion" wollen, sollten
    /// `glideWindow` mit `completion` aufrufen — der Mechanismus dort
    /// stoppt automatisch vor dem Neustart.
    func stopActiveGlide() {
        guard let state = self.activeGlideState else { return }
        state.cancel()
        self.activeGlideState = nil
    }
}

// MARK: - Interner Glide-State
private final class GlideState {
    weak var panel: OverlayPanel?
    let startOrigin: NSPoint
    let targetOrigin: NSPoint
    let startTime: CFTimeInterval
    let duration: CFTimeInterval
    var completion: (() -> Void)?
    let displayLink: CVDisplayLink
    private var cancelled = false

    init(panel: OverlayPanel,
         startOrigin: NSPoint,
         targetOrigin: NSPoint,
         startTime: CFTimeInterval,
         duration: CFTimeInterval,
         completion: (() -> Void)?,
         displayLink: CVDisplayLink) {
        self.panel = panel
        self.startOrigin = startOrigin
        self.targetOrigin = targetOrigin
        self.startTime = startTime
        self.duration = duration
        self.completion = completion
        self.displayLink = displayLink
    }

    func tick() {
        if cancelled { return }
        guard let panel = panel else {
            stop()
            return
        }
        let now = CACurrentMediaTime()
        let t = CGFloat(min(1.0, (now - startTime) / duration))
        let eased = OverlayGlide.smootherstep(t)
        let x = startOrigin.x + (targetOrigin.x - startOrigin.x) * eased
        let y = startOrigin.y + (targetOrigin.y - startOrigin.y) * eased
        panel.setFrameOrigin(NSPoint(x: x, y: y))
        if t >= 1.0 {
            let done = completion
            completion = nil
            stop()
            done?()
        }
    }

    func cancel() {
        cancelled = true
        completion = nil  // verworfen, wir wurden gestoppt
        stop()
    }

    private func stop() {
        if CVDisplayLinkIsRunning(displayLink) {
            CVDisplayLinkStop(displayLink)
        }
        if panel?.activeGlideState === self {
            panel?.activeGlideState = nil
        }
    }
}

// MARK: - Active-State via Associated Object
private var activeGlideStateKey: UInt8 = 0
extension OverlayPanel {
    fileprivate var activeGlideState: GlideState? {
        get { objc_getAssociatedObject(self, &activeGlideStateKey) as? GlideState }
        set { objc_setAssociatedObject(self, &activeGlideStateKey, newValue,
                                       .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }
}

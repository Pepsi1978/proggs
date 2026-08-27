import AppKit

// MARK: - WaveformView
// Portierung von TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs
// Waveform-Animation: 14 Rechteck-Striche, 2 px breit, 1 px Spacing,
// Hoehe 3..40 px, animiert ueber `LevelChanged` (~100 ms-Buffer).
//
// Wird im MicButton ueberlagert sichtbar wenn `setLevel()` aufgerufen wird.
// `clear()` setzt alle Balken auf Minimum.

final class WaveformView: NSView {

    /// Anzahl der Balken — Windows hat 14.
    static let barCount: Int = 14
    /// Breite eines Balkens.
    static let barWidth: CGFloat = 2
    /// Abstand zwischen zwei Balken.
    static let barSpacing: CGFloat = 1
    /// Minimum-Hoehe eines Balkens (auch bei 0 Pegel).
    static let minBarHeight: CGFloat = 3
    /// Maximum-Hoehe eines Balkens (bei Pegel 1.0).
    static let maxBarHeight: CGFloat = 40

    /// Aktuelle Pegel pro Balken (0..1). Wird ueber `pushLevel(_:)` rotiert —
    /// neue Werte rechts, alte Werte verschieben sich nach links.
    private var levels: [Float] = Array(repeating: 0, count: WaveformView.barCount)

    override init(frame: NSRect) {
        super.init(frame: frame)
        wantsLayer = true
        layer?.backgroundColor = NSColor.clear.cgColor
    }

    required init?(coder: NSCoder) { fatalError() }

    /// Schiebt einen neuen Pegelwert ans rechte Ende (FIFO 14 Plaetze).
    /// Der Aufrufer (AppDelegate) verbindet das mit `AudioRecorder.onLevel`.
    ///
    /// Der Pegel wird EXAKT wie in Windows verstaerkt (OverlayWindow.xaml.cs,
    /// OnAudioLevelChanged): Wurzel macht leise Toene sichtbar, Faktor 1.6 hebt
    /// das Ergebnis an, Deckel bei 1.0. Ohne diese Verstaerkung bleiben normale
    /// Sprechlautstaerken bei 0.05..0.2 haengen und die Welle wirkt tot.
    func pushLevel(_ value: Float) {
        let clamped = min(max(value, 0), 1)
        let boosted = min(1.0, clamped.squareRoot() * 1.6)
        levels.removeFirst()
        levels.append(boosted)
        needsDisplay = true
    }

    /// Loescht alle Balken (z.B. wenn Recording stoppt).
    func clear() {
        levels = Array(repeating: 0, count: WaveformView.barCount)
        needsDisplay = true
    }

    override func draw(_ dirtyRect: NSRect) {
        let totalW = CGFloat(WaveformView.barCount) * WaveformView.barWidth
                   + CGFloat(WaveformView.barCount - 1) * WaveformView.barSpacing
        let startX = (bounds.width - totalW) / 2
        let midY = bounds.height / 2

        // Balken-Farbe: dunkel und VOLL deckend wie in Windows (shared frozen
        // brush #1A1A1A). Frueher 85% Alpha — zusammen mit dem darunter weiter
        // sichtbaren Mic-Icon sah die Welle dadurch matschig aus. In Windows
        // verschwindet das Icon waehrend der Aufnahme komplett.
        NSColor(red: 0.102, green: 0.102, blue: 0.102, alpha: 1.0).set()

        for (i, level) in levels.enumerated() {
            let lvl = CGFloat(level)
            // Windows-Formel 1:1: h = min + level * (max - min). Der Pegel ist
            // in pushLevel bereits verstaerkt; eine zweite Kurve hier wuerde
            // die Welle gegenueber Windows verflachen.
            let h = WaveformView.minBarHeight
                  + lvl * (WaveformView.maxBarHeight - WaveformView.minBarHeight)
            let x = startX + CGFloat(i) * (WaveformView.barWidth + WaveformView.barSpacing)
            let y = midY - h / 2
            let rect = NSRect(x: x, y: y, width: WaveformView.barWidth, height: h)
            let path = NSBezierPath(roundedRect: rect, xRadius: 1, yRadius: 1)
            path.fill()
        }
    }
}

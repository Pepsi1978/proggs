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
    func pushLevel(_ value: Float) {
        levels.removeFirst()
        levels.append(min(max(value, 0), 1))
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

        // Balken-Farbe: dunkel auf gelbem Mic (Windows MicIcon hat
        // Fill #FF1A1A1A). Mit leichter Transparenz, damit das Mic-Icon
        // im Hintergrund noch durchschimmert.
        NSColor(red: 0.102, green: 0.102, blue: 0.102, alpha: 0.85).set()

        for (i, level) in levels.enumerated() {
            let lvl = CGFloat(level)
            let h = max(WaveformView.minBarHeight,
                        WaveformView.maxBarHeight * pow(lvl, 0.6))
            let x = startX + CGFloat(i) * (WaveformView.barWidth + WaveformView.barSpacing)
            let y = midY - h / 2
            let rect = NSRect(x: x, y: y, width: WaveformView.barWidth, height: h)
            let path = NSBezierPath(roundedRect: rect, xRadius: 1, yRadius: 1)
            path.fill()
        }
    }
}

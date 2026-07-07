import AppKit
import Carbon

// MARK: - PushToTalkController
// Portierung der Windows-PTT-Logik (Strg+Alt+Leertaste Hold ≥500ms = PTT,
// Tap <500ms = Toggle) fuer macOS.
//
// Carbon RegisterEventHotKey liefert nur KeyDown — kein Hold/KeyUp.
// Wir koppeln Carbon (start recording bei KeyDown via TVOHotkey.voiceToggle)
// mit NSEvent.addGlobalMonitorForEvents matching .keyUp, um die Release-Zeit
// zu sehen. Accessibility-Permission ist vorhanden (App nutzt schon CGEvent.post
// in TerminalController).

final class PushToTalkController {

    /// Schwellwert in Sekunden zwischen KeyDown und KeyUp:
    /// >= holdThreshold → Hold-Modus (PTT-Release stoppt Aufnahme)
    /// < holdThreshold  → Tap-Modus (Aufnahme bleibt, naechster Tap stoppt)
    var holdThreshold: TimeInterval = 0.5

    /// Wird vom AppDelegate gesetzt — stoppt die Aufnahme.
    var onPTTRelease: (() -> Void)?

    /// Wird gesetzt wenn Carbon den KeyDown ausgeloest hat (= Aufnahme startet
    /// gerade). Beim KeyUp wird die Differenz geprueft.
    private var pressStartTime: TimeInterval?
    private var keyUpMonitor: Any?

    /// Carbon-Hotkey-KeyCode (TVOHotkey.voiceToggle = kVK_ANSI_R).
    private let targetKeyCode: CGKeyCode = CGKeyCode(kVK_ANSI_R)

    init() {
        startMonitor()
    }

    deinit {
        if let m = keyUpMonitor { NSEvent.removeMonitor(m) }
    }

    /// Vom AppDelegate gerufen wenn Carbon den KeyDown gefeuert hat —
    /// markiert den Start-Zeitpunkt fuer die Hold-Schwelle.
    func notePressDown() {
        pressStartTime = CACurrentMediaTime()
    }

    private func startMonitor() {
        keyUpMonitor = NSEvent.addGlobalMonitorForEvents(matching: [.keyUp]) { [weak self] event in
            guard let self = self else { return }
            guard event.keyCode == self.targetKeyCode else { return }
            guard let start = self.pressStartTime else { return }
            let elapsed = CACurrentMediaTime() - start
            self.pressStartTime = nil
            if elapsed >= self.holdThreshold {
                tvoDebug("[PTT] hold release after \(String(format: "%.2f", elapsed))s → stop recording")
                DispatchQueue.main.async { self.onPTTRelease?() }
            } else {
                tvoDebug("[PTT] tap (\(String(format: "%.2f", elapsed))s) — recording bleibt aktiv")
            }
        }
    }
}

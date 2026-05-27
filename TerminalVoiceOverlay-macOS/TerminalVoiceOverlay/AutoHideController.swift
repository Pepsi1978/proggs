import AppKit

// MARK: - Auto-Hide-Controller
// Portierung von TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs
// AutoHide-Mechanik (CollapseTimer 2s/5s, _collapseBeamGen).
//
// Verhalten wie Windows:
//   - Nach `idleTimeout` Sekunden ohne Interaktion: Panel schrumpft auf
//     die 84x84 Mic-Pille.
//   - Bei Aufnahme / Verarbeitung: kein Auto-Hide.
//   - Bei Mausbewegung ins Panel: Timer reset.
//   - Klick auf Collapsed-Mic: ausklappen + Timer reset.

final class AutoHideController {

    /// Sekunden Inaktivitaet bevor das Panel einklappt (Frank-Wunsch: 3s).
    var idleTimeout: TimeInterval = 3.0

    /// Wird gesetzt vom AppDelegate. Soll true sein wahrend Recording/
    /// Processing — dann kein Auto-Collapse.
    var busyProvider: (() -> Bool)?

    private weak var panel: OverlayPanel?
    private var timer: Timer?
    private var globalMouseMonitor: Any?

    init(panel: OverlayPanel) {
        self.panel = panel
        startGlobalMouseMonitor()
        resetTimer()
    }

    deinit {
        timer?.invalidate()
        if let m = globalMouseMonitor {
            NSEvent.removeMonitor(m)
        }
    }

    /// Manueller Reset, z.B. nach Click auf Mic/Btw/anderer Button.
    func resetTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: idleTimeout,
                                     repeats: false) { [weak self] _ in
            self?.collapseIfIdle()
        }
    }

    /// Wird vom AppDelegate gerufen wenn die App "busy" ist (Recording
    /// laeuft). Stoppt den Timer.
    func suspend() {
        timer?.invalidate()
        timer = nil
    }

    /// Resume nach Recording/Processing: Timer neu starten.
    func resume() {
        resetTimer()
    }

    // MARK: - Private

    private func collapseIfIdle() {
        guard let panel = panel else { return }
        if busyProvider?() == true { return }
        if panel.isCollapsed { return }
        panel.beamToCollapsed { [weak self] in
            // Im Collapsed-State KEIN Timer mehr — der User muss aktiv
            // expandieren (Klick auf Mic-Pille).
            self?.timer?.invalidate()
            self?.timer = nil
        }
    }

    /// Globaler Maus-Monitor: bei Bewegung INS Panel hinein wird der
    /// Timer resetted; bei Hover ueber dem Mic-Pille im Collapsed-Modus
    /// expandiert es automatisch (Windows-Verhalten).
    private func startGlobalMouseMonitor() {
        globalMouseMonitor = NSEvent.addGlobalMonitorForEvents(
            matching: [.mouseMoved, .leftMouseDown, .rightMouseDown]
        ) { [weak self] event in
            guard let self = self, let panel = self.panel else { return }
            let mouse = NSEvent.mouseLocation
            let inside = panel.frame.contains(mouse)
            if inside {
                if panel.isCollapsed && event.type == .mouseMoved {
                    // Hover auf Collapsed-Pille → auto-expand.
                    panel.beamToExpanded { [weak self] in
                        self?.resetTimer()
                    }
                } else if !panel.isCollapsed {
                    // Im Voll-Modus: Maus bewegt sich uebers Panel →
                    // Idle-Timer zuruecksetzen.
                    self.resetTimer()
                }
            }
        }
    }
}

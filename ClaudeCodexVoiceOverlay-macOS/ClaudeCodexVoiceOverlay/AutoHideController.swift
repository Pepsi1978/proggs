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

    /// Sekunden Inaktivitaet bevor das Panel einklappt. 1:1 Windows
    /// (OverlayWindow.xaml.cs: CollapseAfterUseMs = CollapseAfterPeekMs = 350).
    /// Die 350 ms liegen sicher hinter dem 240-ms-Beam-Fade, ohne die frueher
    /// volle Sekunde Wartezeit wieder einzufuehren — genau deshalb wirkte das
    /// macOS-Overlay nach einer Aufnahme traeger als das unter Windows.
    /// Gilt orientierungs-agnostisch fuer Vertikal- UND Horizontalmodus
    /// (collapseIfIdle ruft beamToCollapsed, das beide Layouts auf die Mic-Pille schrumpft).
    var idleTimeout: TimeInterval = 0.35

    /// Wird gesetzt vom AppDelegate. Soll true sein wahrend Recording/
    /// Processing — dann kein Auto-Collapse.
    var busyProvider: (() -> Bool)?

    /// Wird gesetzt vom AppDelegate. Soll true sein solange das Prompt-Board,
    /// die Prompt-Eingabe oder die Historie sichtbar ist — dann kein
    /// Auto-Collapse, unabhaengig davon ob die Maus gerade ueber dem Pillar
    /// liegt. Verhindert dass das VTO einklappt waehrend der Benutzer im
    /// Prompt-System (inkl. Menues/Untermenues) arbeitet. Gilt fuer den
    /// vertikalen UND den horizontalen Modus (collapseIfIdle ist
    /// orientierungs-agnostisch).
    var promptSurfaceVisibleProvider: (() -> Bool)?

    /// Wenn false: Auto-Hide-Timer wird nie gestartet (User hat Auto-Hide
    /// in den Einstellungen deaktiviert).
    var enabled: Bool = true {
        didSet {
            if enabled { resetTimer() } else { suspend() }
        }
    }

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
        guard enabled else { return }
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

    /// Windows-Pendant zu `ScheduleCollapse()`: wird EREIGNISGESTEUERT gerufen,
    /// sobald eine Aufnahme endet (Recording -> Processing/Idle/Success/Error).
    /// Windows klappt schon beim Uebergang auf Processing ein — die
    /// Transkription laeuft im Hintergrund weiter. Vorher lief auf macOS nur
    /// ein reiner Idle-Timer, der bei jeder Mausbewegung neu startete; das
    /// Overlay blieb nach dem Sprechen sichtbar stehen.
    func scheduleCollapseSoon() {
        guard enabled else { return }
        guard let panel = panel, !panel.isCollapsed else { return }
        // Liegt die Maus ueber dem Overlay, bleibt es offen (Windows:
        // `if (_mouseOverOverlay) return;`).
        if panel.frame.contains(NSEvent.mouseLocation) { return }
        resetTimer()
    }

    /// Windows-Pendant zum Expand beim Aufnahmestart: laeuft eine Aufnahme,
    /// muss die Welle sichtbar sein — also aufklappen, falls eingeklappt.
    func expandForRecording() {
        guard let panel = panel, panel.isCollapsed else { return }
        panel.beamToExpanded { [weak self] in self?.suspend() }
    }

    // MARK: - Private

    private func collapseIfIdle() {
        guard let panel = panel else { return }
        if busyProvider?() == true { return }
        // Prompt-Board / Eingabe / Historie offen → NICHT einklappen, aber den
        // Timer weiterlaufen lassen (Polling alle `idleTimeout` Sekunden),
        // damit nach dem Schliessen des Prompt-Systems wieder normal
        // eingeklappt wird. Ohne resetTimer() wuerde der non-repeating Timer
        // hier auslaufen und nie wieder feuern.
        if promptSurfaceVisibleProvider?() == true {
            resetTimer()
            return
        }
        // Maus aktuell ueber dem Overlay? → NICHT einklappen, Timer neu starten
        // (Polling alle idleTimeout Sekunden). Behebt Frank-Bug 2026-06-01:
        // Wenn der Zeiger STILL ueber dem Overlay liegt (keine Bewegung), feuert
        // der globalMouseMonitor kein .mouseMoved-Event, der Idle-Timer lief
        // trotzdem ab und klappte ein. Der Monitor resettet nur bei BEWEGUNG —
        // ein ruhender Zeiger wurde nicht erkannt. NSEvent.mouseLocation liefert
        // die Position ereignis-unabhaengig; panel.frame ist in denselben
        // Screen-Koordinaten (exakt die "ueber dem Overlay"-Definition aus dem
        // Monitor). Deckt den GESAMTEN Overlay-Bereich ab, Vertikal- wie
        // Horizontalmodus. Erst wenn der Zeiger ausserhalb ist, greift die
        // 1-Sekunden-Regel.
        if panel.frame.contains(NSEvent.mouseLocation) {
            resetTimer()
            return
        }
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

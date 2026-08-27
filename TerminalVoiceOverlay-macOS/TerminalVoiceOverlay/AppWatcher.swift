import AppKit

final class AppWatcher {
    /// Liste aller Terminal-Apps die wir als "Ziel" fuer Voice/Prompt-Input
    /// erkennen. Wenn die front-most App KEINE davon ist, wird beim
    /// activateTerminal-Fallback eine willkuerliche aus dem Hintergrund
    /// nach vorne geholt — was zu Beeps fuehrt weil Cmd+V dann an der
    /// falschen Stelle landet (oft am Voice-Overlay selbst, das kein
    /// Cmd+V-Handler hat → NSBeep).
    /// Erweiterung 2026-05-02: Warp + andere moderne Terminals + Editoren
    /// mit eingebautem Terminal (VS Code, Cursor) hinzugefuegt.
    static let targetBundleIDs: Set<String> = [
        "com.apple.Terminal",
        "com.googlecode.iterm2",
        "dev.warp.Warp-Stable",
        "co.zeit.hyper",
        "io.alacritty",
        "net.kovidgoyal.kitty",
        "com.github.wez.wezterm",
        "com.mitchellh.ghostty",
        "com.tabby",
        "com.microsoft.VSCode",
        "com.todesktop.230313mzl4w4u92",     // Cursor
        "com.googlecode.iterm2-nightly",
    ]

    private(set) var lastActiveTerminalBundleID: String?

    var onTerminalActivated: (() -> Void)?
    var onTerminalDeactivated: (() -> Void)?

    deinit {
        NSWorkspace.shared.notificationCenter.removeObserver(self)
    }

    static func isTargetApp(_ bundleID: String?) -> Bool {
        guard let id = bundleID else { return false }
        return targetBundleIDs.contains(id)
    }

    /// Poll-Variante zu den Aktivierungs-Notifications: prueft, ob die AKTUELL
    /// vorderste App eine unserer Ziel-Apps ist. Grundlage fuer den
    /// selbstheilenden Sichtbarkeits-Poll im AppDelegate — 1:1 zum Windows-
    /// `GetForegroundTerminalHwnd` (Commit "Foreground-Reclaim-Poll").
    ///
    /// Warum: das Einblenden haengt allein an
    /// `didActivateApplicationNotification`. Blitzt kurz ein Fremdfenster auf
    /// (Mitteilung, Berechtigungs-Dialog) oder geht eine Notification verloren,
    /// bleibt das Overlay versteckt, obwohl das CLI weiter vorne steht — und
    /// kaeme erst bei einem echten Fokuswechsel zurueck. Steht wirklich eine
    /// fremde App vorne, liefert das hier `false` und das Overlay bleibt aus.
    func isTargetAppFrontmost() -> Bool {
        guard let front = NSWorkspace.shared.frontmostApplication else { return false }
        // Die eigene App zaehlt NICHT als Ziel — sonst wuerde jeder Klick auf
        // ein eigenes Panel das Overlay auch dann zurueckholen, wenn es
        // bewusst versteckt wurde.
        if front.bundleIdentifier == Bundle.main.bundleIdentifier { return false }
        guard Self.isTargetApp(front.bundleIdentifier) else { return false }
        lastActiveTerminalBundleID = front.bundleIdentifier
        TerminalController.lastActiveTerminalBundleID = front.bundleIdentifier
        return true
    }

    func start() {
        let nc = NSWorkspace.shared.notificationCenter
        nc.addObserver(self, selector: #selector(appActivated(_:)),
                       name: NSWorkspace.didActivateApplicationNotification, object: nil)
        nc.addObserver(self, selector: #selector(appTerminated(_:)),
                       name: NSWorkspace.didTerminateApplicationNotification, object: nil)

        // Check initial state
        if let frontApp = NSWorkspace.shared.frontmostApplication,
           Self.isTargetApp(frontApp.bundleIdentifier) {
            lastActiveTerminalBundleID = frontApp.bundleIdentifier
            TerminalController.lastActiveTerminalBundleID = frontApp.bundleIdentifier
            onTerminalActivated?()
        }
    }

    @objc private func appActivated(_ notification: Notification) {
        guard let app = notification.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication else { return }
        // Ignore our own process becoming active — otherwise every time we
        // call NSApp.activate() (e.g. to force a modal dialog forward) we'd
        // also fire onTerminalDeactivated and order our own panels out
        // while the modal session is still opening. That race produced
        // invisible-dialog/beep-loops in past builds.
        if app.bundleIdentifier == Bundle.main.bundleIdentifier {
            return
        }
        if Self.isTargetApp(app.bundleIdentifier) {
            lastActiveTerminalBundleID = app.bundleIdentifier
            TerminalController.lastActiveTerminalBundleID = app.bundleIdentifier
            onTerminalActivated?()
        } else {
            onTerminalDeactivated?()
        }
    }

    @objc private func appTerminated(_ notification: Notification) {
        guard let app = notification.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication else { return }
        if Self.isTargetApp(app.bundleIdentifier) {
            onTerminalDeactivated?()
        }
    }
}

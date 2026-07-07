import AppKit

final class AppWatcher {
    /// Liste aller Electron-Ziel-Apps die wir als "Ziel" fuer Voice/Prompt-Input
    /// erkennen (Claude Desktop, Codex). Wenn die front-most App KEINE davon ist,
    /// wird beim activateTerminal-Fallback eine willkuerliche aus dem Hintergrund
    /// nach vorne geholt — was zu Beeps fuehrt weil Cmd+V dann an der falschen
    /// Stelle landet (oft am Voice-Overlay selbst, das kein Cmd+V-Handler hat →
    /// NSBeep).
    static let targetBundleIDs: Set<String> = [
        "com.anthropic.claudefordesktop",    // Claude Desktop
        "com.openai.codex",                  // Codex
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

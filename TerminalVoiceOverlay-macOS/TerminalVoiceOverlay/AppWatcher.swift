import AppKit

final class AppWatcher {
    static let targetBundleIDs: Set<String> = [
        "com.apple.Terminal",
        "com.googlecode.iterm2",
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

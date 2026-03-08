import AppKit

final class AppWatcher {
    private let targetBundleID = "com.apple.Terminal"
    var onTerminalActivated: (() -> Void)?
    var onTerminalDeactivated: (() -> Void)?

    func start() {
        let nc = NSWorkspace.shared.notificationCenter
        nc.addObserver(self, selector: #selector(appActivated(_:)),
                       name: NSWorkspace.didActivateApplicationNotification, object: nil)
        nc.addObserver(self, selector: #selector(appTerminated(_:)),
                       name: NSWorkspace.didTerminateApplicationNotification, object: nil)

        // Check initial state
        if let frontApp = NSWorkspace.shared.frontmostApplication,
           frontApp.bundleIdentifier == targetBundleID {
            onTerminalActivated?()
        }
    }

    @objc private func appActivated(_ notification: Notification) {
        guard let app = notification.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication else { return }
        if app.bundleIdentifier == targetBundleID {
            onTerminalActivated?()
        } else {
            onTerminalDeactivated?()
        }
    }

    @objc private func appTerminated(_ notification: Notification) {
        guard let app = notification.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication else { return }
        if app.bundleIdentifier == targetBundleID {
            onTerminalDeactivated?()
        }
    }
}

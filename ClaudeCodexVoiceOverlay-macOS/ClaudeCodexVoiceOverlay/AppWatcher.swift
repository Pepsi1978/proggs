import AppKit

final class AppWatcher {
    private let targetBundleIDs: Set<String> = ["com.anthropic.claudefordesktop", "com.openai.codex"]
    var onTargetAppActivated: (() -> Void)?
    var onTargetAppDeactivated: (() -> Void)?

    func start() {
        let nc = NSWorkspace.shared.notificationCenter
        nc.addObserver(self, selector: #selector(appActivated(_:)),
                       name: NSWorkspace.didActivateApplicationNotification, object: nil)
        nc.addObserver(self, selector: #selector(appTerminated(_:)),
                       name: NSWorkspace.didTerminateApplicationNotification, object: nil)

        // Check initial state
        if let frontApp = NSWorkspace.shared.frontmostApplication,
           let bundleID = frontApp.bundleIdentifier,
           targetBundleIDs.contains(bundleID) {
            onTargetAppActivated?()
        }
    }

    @objc private func appActivated(_ notification: Notification) {
        guard let app = notification.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication else { return }
        if let bundleID = app.bundleIdentifier, targetBundleIDs.contains(bundleID) {
            onTargetAppActivated?()
        } else {
            onTargetAppDeactivated?()
        }
    }

    @objc private func appTerminated(_ notification: Notification) {
        guard let app = notification.userInfo?[NSWorkspace.applicationUserInfoKey] as? NSRunningApplication else { return }
        if let bundleID = app.bundleIdentifier, targetBundleIDs.contains(bundleID) {
            onTargetAppDeactivated?()
        }
    }
}

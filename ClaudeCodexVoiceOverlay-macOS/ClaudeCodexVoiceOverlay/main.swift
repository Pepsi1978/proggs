import AppKit

// Single-instance guard: LaunchAgent + macOS "reopen on login" can start two copies,
// producing duplicate menu-bar icons. If another copy with our bundle ID is already
// running, exit silently. Paired with KeepAlive={SuccessfulExit:false} in the plist
// so launchd does not restart on this clean exit.
if let bundleID = Bundle.main.bundleIdentifier {
    let myPID = ProcessInfo.processInfo.processIdentifier
    let others = NSRunningApplication.runningApplications(withBundleIdentifier: bundleID)
        .filter { $0.processIdentifier != myPID }
    if !others.isEmpty {
        exit(0)
    }
}

let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate

// LSUIElement behavior (no dock icon, no cmd+tab)
// This is set in Info.plist, but also enforce programmatically
app.setActivationPolicy(.accessory)

app.run()

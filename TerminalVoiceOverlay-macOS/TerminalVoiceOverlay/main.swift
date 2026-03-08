import AppKit

let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate

// LSUIElement behavior (no dock icon, no cmd+tab)
// This is set in Info.plist, but also enforce programmatically
app.setActivationPolicy(.accessory)

app.run()

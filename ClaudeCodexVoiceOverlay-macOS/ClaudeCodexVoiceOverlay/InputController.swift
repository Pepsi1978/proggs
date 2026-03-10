import AppKit
import Foundation

final class InputController {

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeRetainedValue(): true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    /// Sends Cmd+A + Backspace to clear the current input field (works in Electron apps)
    static func clearLine() {
        activateTargetApp()
        usleep(150_000) // 150ms to ensure app is frontmost
        // Cmd+A to select all text in the input field
        sendKeyCombo(keyCode: 0x00, flags: .maskCommand) // 'a' = 0x00
        usleep(50_000) // 50ms to ensure selection is applied
        // Backspace to delete selected text
        sendKeyCombo(keyCode: 0x33, flags: []) // Backspace = 0x33
    }

    /// Pastes text via clipboard + Cmd+V, optionally sends Enter afterwards
    static func pasteText(_ text: String, autoEnter: Bool = false) {
        NSLog("=== pasteText aufgerufen: '%@' ===", text)
        NSLog("AXIsProcessTrusted: %@", AXIsProcessTrusted() ? "JA" : "NEIN")

        if let frontApp = NSWorkspace.shared.frontmostApplication {
            NSLog("Frontmost App VOR activate: %@ (BundleID: %@, PID: %d)",
                  frontApp.localizedName ?? "?", frontApp.bundleIdentifier ?? "?", frontApp.processIdentifier)
        }

        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(text, forType: .string)
        NSLog("Clipboard gesetzt")

        activateTargetApp()
        usleep(150_000) // 150ms to ensure app is frontmost

        if let frontApp2 = NSWorkspace.shared.frontmostApplication {
            NSLog("Frontmost App NACH activate: %@ (BundleID: %@, PID: %d)",
                  frontApp2.localizedName ?? "?", frontApp2.bundleIdentifier ?? "?", frontApp2.processIdentifier)
        }

        NSLog("Sende CGEvent Cmd+V...")
        sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // 'v' = 0x09

        if autoEnter {
            usleep(500_000) // 500ms
            sendKeyCombo(keyCode: 0x24, flags: []) // Return = 0x24
        }
    }

    /// Tracks the last active target app so we activate the correct one
    static var lastActiveTargetBundleID: String?

    /// Brings the last active target app (Claude Desktop or Codex) to the front so CGEvent reaches it
    private static func activateTargetApp() {
        guard let bundleID = lastActiveTargetBundleID,
              let app = NSWorkspace.shared.runningApplications
                .first(where: { $0.bundleIdentifier == bundleID }) else { return }
        app.activate(options: [])
    }

    private static func sendKeyCombo(keyCode: CGKeyCode, flags: CGEventFlags) {
        let source = CGEventSource(stateID: .hidSystemState)

        guard let keyDown = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: true),
              let keyUp = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: false) else {
            NSLog("CGEvent konnte nicht erstellt werden")
            return
        }

        keyDown.flags = flags
        keyUp.flags = flags

        keyDown.post(tap: .cghidEventTap)
        keyUp.post(tap: .cghidEventTap)
    }
}

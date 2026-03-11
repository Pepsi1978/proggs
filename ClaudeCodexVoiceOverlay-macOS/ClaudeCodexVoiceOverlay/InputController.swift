import AppKit
import Foundation

final class InputController {

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeUnretainedValue(): true] as CFDictionary
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
        #if DEBUG
        NSLog("pasteText: '%@'", text)
        #endif

        let pasteboard = NSPasteboard.general
        let previousContents = pasteboard.string(forType: .string)
        pasteboard.clearContents()
        pasteboard.setString(text, forType: .string)

        activateTargetApp()
        usleep(150_000)

        sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V

        if autoEnter {
            usleep(300_000)
            sendKeyCombo(keyCode: 0x24, flags: []) // Return
        }

        // Restore previous clipboard after paste completes
        DispatchQueue.global(qos: .utility).asyncAfter(deadline: .now() + 0.5) {
            if let previous = previousContents {
                DispatchQueue.main.async {
                    let pb = NSPasteboard.general
                    pb.clearContents()
                    pb.setString(previous, forType: .string)
                }
            }
        }
    }

    /// Tracks the last active target app so we activate the correct one
    static var lastActiveTargetBundleID: String?

    /// Brings the last active target app (Claude Desktop or Codex) to the front so CGEvent reaches it
    static func activateTargetApp() {
        guard let bundleID = lastActiveTargetBundleID,
              let app = NSWorkspace.shared.runningApplications
                .first(where: { $0.bundleIdentifier == bundleID }) else { return }
        app.activate(options: [])
    }

    static func sendKeyCombo(keyCode: CGKeyCode, flags: CGEventFlags) {
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

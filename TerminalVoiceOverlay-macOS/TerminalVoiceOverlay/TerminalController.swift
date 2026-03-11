import AppKit
import Foundation

final class TerminalController {

    /// Bundle ID of the last active terminal, set by AppWatcher
    static var lastActiveTerminalBundleID: String?

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeUnretainedValue(): true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    /// Sends Ctrl+C to discard the entire input (works for multi-line)
    static func clearLine() {
        activateTerminal()
        usleep(150_000)
        sendKeyCombo(keyCode: 0x08, flags: .maskControl) // 'c' = 0x08
    }

    /// Pastes text via clipboard + Cmd+V, optionally sends Enter afterwards.
    /// Saves and restores the previous clipboard content.
    static func pasteText(_ text: String, autoEnter: Bool = false) {
        #if DEBUG
        NSLog("pasteText: '%@'", text)
        #endif

        // Save current clipboard content
        let pasteboard = NSPasteboard.general
        let previousContents = pasteboard.string(forType: .string)

        pasteboard.clearContents()
        pasteboard.setString(text, forType: .string)

        activateTerminal()
        usleep(150_000)

        sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V

        if autoEnter {
            usleep(300_000)
            sendKeyCombo(keyCode: 0x24, flags: []) // Return = 0x24
        }

        // Restore previous clipboard content after a short delay
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

    /// Brings the last active terminal app to the front so CGEvent reaches it
    static func activateTerminal() {
        // Prefer the terminal that was last in focus
        let targetBundleID = lastActiveTerminalBundleID
        if let bundleID = targetBundleID,
           let app = NSWorkspace.shared.runningApplications
            .first(where: { $0.bundleIdentifier == bundleID }) {
            app.activate()
            return
        }
        // Fallback: pick any running target app
        if let app = NSWorkspace.shared.runningApplications
            .first(where: { AppWatcher.isTargetApp($0.bundleIdentifier) }) {
            app.activate()
        }
    }

    static func sendKeyCombo(keyCode: CGKeyCode, flags: CGEventFlags) {
        let source = CGEventSource(stateID: .hidSystemState)

        guard let keyDown = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: true),
              let keyUp = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: false) else {
            NSLog("CGEvent creation failed")
            return
        }

        keyDown.flags = flags
        keyUp.flags = flags

        keyDown.post(tap: .cghidEventTap)
        keyUp.post(tap: .cghidEventTap)
    }
}

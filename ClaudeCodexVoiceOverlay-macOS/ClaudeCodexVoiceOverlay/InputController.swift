import AppKit
import Foundation

final class InputController {

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeRetainedValue(): true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    /// Sends Cmd+A + Backspace to clear the current input field (works in Electron apps)
    static func clearLine() {
        // Cmd+A to select all text in the input field
        sendKeyCombo(keyCode: 0x00, flags: .maskCommand) // 'a' = 0x00
        // Small delay to ensure selection is applied
        usleep(50_000) // 50ms
        // Backspace to delete selected text
        sendKeyCombo(keyCode: 0x33, flags: []) // Backspace = 0x33
    }

    /// Pastes text via clipboard + Cmd+V, optionally sends Enter afterwards
    static func pasteText(_ text: String, autoEnter: Bool = false) {
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(text, forType: .string)

        // Small delay to ensure clipboard is set
        usleep(50_000) // 50ms

        sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // 'v' = 0x09

        if autoEnter {
            usleep(500_000) // 500ms
            sendKeyCombo(keyCode: 0x24, flags: []) // Return = 0x24
        }
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

import AppKit
import Foundation

final class TerminalController {

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeRetainedValue(): true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    /// Sends Ctrl+U to clear the current line in bash/zsh
    static func clearLine() {
        sendKeyCombo(keyCode: 0x20, flags: .maskControl) // 'u' = 0x20
    }

    /// Pastes text via clipboard + Cmd+V
    static func pasteText(_ text: String) {
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(text, forType: .string)

        // Small delay to ensure clipboard is set
        usleep(50_000) // 50ms

        sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // 'v' = 0x09
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

import AppKit
import Foundation

final class TerminalController {

    /// Bundle ID of the last active terminal, set by AppWatcher
    static var lastActiveTerminalBundleID: String?

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeUnretainedValue(): true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    /// Clears the current terminal input line via Ctrl+U (kill-line-to-start).
    /// Ctrl+U is the standard readline/bash/zsh shortcut for clearing the input
    /// buffer WITHOUT sending SIGINT. Critical for Claude Code CLI and similar
    /// TUIs: Ctrl+C would interrupt a running task ("Interrupted"), but Ctrl+U
    /// only clears the text in the input line. If no input is present (task is
    /// running), Ctrl+U is a safe no-op.
    /// Matches the pattern documented in this project's CLAUDE.md:
    ///   "Line clearing: Ctrl+U (terminal-specific)"
    static func clearLine() {
        activateTerminal()
        usleep(150_000)
        // 'u' = 0x20 on macOS CGKeyCode; Ctrl+U = kill line to start of input
        sendKeyCombo(keyCode: 0x20, flags: .maskControl)
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

    /// Copies the current selection in the terminal via Cmd+C
    static func copySelection() {
        activateTerminal()
        usleep(50_000) // 50ms
        sendKeyCombo(keyCode: 0x08, flags: .maskCommand) // Cmd+C
    }

    /// Pastes clipboard content into the terminal via Cmd+V
    static func pasteClipboard() {
        activateTerminal()
        usleep(50_000) // 50ms
        sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V
    }

    /// Sends Return (Enter) to the terminal
    static func pressReturn() {
        activateTerminal()
        usleep(100_000) // 100ms
        sendKeyCombo(keyCode: 0x24, flags: []) // Return
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

import AppKit
import Foundation

final class TerminalController {

    /// Bundle ID of the last active terminal, set by AppWatcher
    static var lastActiveTerminalBundleID: String?

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeUnretainedValue(): true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    /// Serial queue used so multiple rapid calls keep their order (clipboard
     /// save/restore + keystrokes must not interleave), while the caller's
     /// thread (typically main/UI) is never blocked by the usleep() waits.
     private static let sendQueue = DispatchQueue(label: "tvo.terminal.send")

    /// Clears the current input field via Cmd+A + Backspace (select all + delete).
    /// This is the Electron-app equivalent of the terminal's Ctrl+U: Cmd+A selects
    /// the entire input field (Claude Desktop / Codex composer), Backspace deletes
    /// the selection. Cmd+A is safe — it never sends a control character to a
    /// running task, it only selects text in the focused input field.
    static func clearLine() {
        sendQueue.async {
            activateTerminal()
            usleep(150_000)
            // Cmd+A (0x00) selects all text in the input field …
            sendKeyCombo(keyCode: 0x00, flags: .maskCommand)
            usleep(50_000)
            // … Backspace (0x33) deletes the selection.
            sendKeyCombo(keyCode: 0x33, flags: [])
        }
    }

    /// Loescht den GESAMTEN Eingabe-Buffer — auch bei mehrzeiligem Input
    /// (z.B. Claude-Desktop-Composer mit Shift+Enter-Zeilen). In Electron-Apps
    /// selektiert Cmd+A das KOMPLETTE Eingabefeld (alle Zeilen), Backspace
    /// loescht es. Anders als Ctrl+U im Terminal genuegt EIN Select-All+Delete;
    /// zur Sicherheit gegen Fokus-Verzoegerung wird es zweimal ausgefuehrt.
    ///
    /// Wird beim Profil-Wechsel im Voice-Overlay verwendet, damit der zuletzt
    /// eingefuegte Prompt restlos verschwindet, bevor die neue Korrektur
    /// reingepastet wird.
    static func clearAllInput() {
        sendQueue.async {
            activateTerminal()
            usleep(150_000)
            for _ in 0..<2 {
                sendKeyCombo(keyCode: 0x00, flags: .maskCommand) // Cmd+A
                usleep(50_000)
                sendKeyCombo(keyCode: 0x33, flags: [])           // Backspace
                usleep(50_000)
            }
        }
    }

    /// Pastes text via clipboard + Cmd+V, optionally sends Enter afterwards.
    /// Saves and restores the previous clipboard content.
    /// All blocking work (activateTerminal, usleep, CGEvent.post) runs on the
    /// serial send queue so the caller's thread (usually main) returns instantly.
    static func pasteText(_ text: String, autoEnter: Bool = false) {
        tvoDebug("[Term] pasteText start textLen=\(text.count) autoEnter=\(autoEnter) lastActiveTerminal=\(lastActiveTerminalBundleID ?? "<nil>")")
        sendQueue.async {
            let pasteboard = NSPasteboard.general
            let previousContents = pasteboard.string(forType: .string)

            pasteboard.clearContents()
            pasteboard.setString(text, forType: .string)
            tvoDebug("[Term] pasteText pasteboard set, calling activateTerminal")

            activateTerminal()
            usleep(150_000)
            let frontApp = NSWorkspace.shared.frontmostApplication?.bundleIdentifier ?? "<unknown>"
            tvoDebug("[Term] pasteText post-activate frontApp=\(frontApp), sending Cmd+V")
            sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V
            if autoEnter {
                usleep(300_000)
                sendKeyCombo(keyCode: 0x24, flags: []) // Return
            }
            // Restore the previous clipboard content after the paste settled.
            // Stays on the send queue so it runs after any subsequent send,
            // never in the middle of a pending paste.
            sendQueue.asyncAfter(deadline: .now() + 0.5) {
                if let previous = previousContents {
                    let pb = NSPasteboard.general
                    pb.clearContents()
                    pb.setString(previous, forType: .string)
                }
            }
        }
    }

    /// Copies the current selection in the terminal via Cmd+C
    static func copySelection() {
        sendQueue.async {
            activateTerminal()
            usleep(50_000)
            sendKeyCombo(keyCode: 0x08, flags: .maskCommand) // Cmd+C
        }
    }

    /// Pastes clipboard content into the terminal via Cmd+V
    static func pasteClipboard() {
        sendQueue.async {
            activateTerminal()
            usleep(50_000)
            sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V
        }
    }

    /// Sends Return (Enter) to the terminal
    static func pressReturn() {
        sendQueue.async {
            activateTerminal()
            usleep(100_000)
            sendKeyCombo(keyCode: 0x24, flags: []) // Return
        }
    }

    /// Brings the last active terminal app to the front so CGEvent reaches it.
    /// Wenn keine Terminal-App in der Liste gefunden wird, faellt es auf die
    /// front-most non-self App zurueck — das verhindert dass Cmd+V an unsere
    /// eigene App geht (was zu System-Beeps fuehrt weil wir kein Cmd+V handhaben).
    static func activateTerminal() {
        // Prefer the terminal that was last in focus
        let targetBundleID = lastActiveTerminalBundleID
        if let bundleID = targetBundleID,
           let app = NSWorkspace.shared.runningApplications
            .first(where: { $0.bundleIdentifier == bundleID }) {
            tvoDebug("[Term] activateTerminal -> last active: \(bundleID)")
            app.activate()
            return
        }
        // Fallback 1: pick any running target app from the known list
        if let app = NSWorkspace.shared.runningApplications
            .first(where: { AppWatcher.isTargetApp($0.bundleIdentifier) }) {
            tvoDebug("[Term] activateTerminal -> known-list fallback: \(app.bundleIdentifier ?? "?")")
            app.activate()
            return
        }
        // Fallback 2: front-most app, sofern nicht wir selbst sind. Besser
        // als gar nichts zu tun (sonst landet Cmd+V im Voice-Overlay → Beep).
        let myBundleID = Bundle.main.bundleIdentifier
        if let frontApp = NSWorkspace.shared.frontmostApplication,
           frontApp.bundleIdentifier != myBundleID {
            tvoDebug("[Term] activateTerminal -> frontmost-fallback: \(frontApp.bundleIdentifier ?? "?")")
            frontApp.activate()
            return
        }
        tvoDebug("[Term] activateTerminal -> KEIN Ziel gefunden! Cmd+V wuerde an uns selbst gehen → Beep")
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

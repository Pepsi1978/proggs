import AppKit
import Foundation

/// Tastatureingabe-Controller fuer Electron-Ziel-Apps (Claude Desktop, Codex).
///
/// Pendant zu `TerminalController` im Schwester-Projekt
/// `TerminalVoiceOverlay-macOS`. Identische API-Oberflaeche (pasteText,
/// clearLine, clearAllInput, copySelection, pasteClipboard, pressReturn,
/// activateTargetApp, sendKeyCombo) — der EINZIGE Unterschied ist die
/// Lösch-Tastenkombination:
///   • Terminal: Ctrl+U (kill-line-to-start)
///   • Electron: Cmd+A + Backspace (alles im Eingabefeld markieren + löschen)
///
/// So koennen alle restlichen portierten Bestandteile (AppDelegate, Panels,
/// Dialoge) den Controller exakt wie das Terminal-Overlay aufrufen.
final class InputController {

    /// Bundle-ID der zuletzt aktiven Ziel-App (Claude Desktop / Codex),
    /// gesetzt vom AppWatcher. Pendant zu
    /// `TerminalController.lastActiveTerminalBundleID`.
    static var lastActiveTargetBundleID: String?

    static func checkAccessibility() -> Bool {
        let options = [kAXTrustedCheckOptionPrompt.takeUnretainedValue(): true] as CFDictionary
        return AXIsProcessTrustedWithOptions(options)
    }

    /// Serielle Queue, damit mehrere schnelle Aufrufe ihre Reihenfolge behalten
    /// (Clipboard-Save/Restore + Tastenanschlaege duerfen nicht verschachteln),
    /// waehrend der Aufrufer-Thread (typisch Main/UI) NIE durch die usleep()-
    /// Wartezeiten blockiert wird. 1:1 wie TerminalController.sendQueue.
    private static let sendQueue = DispatchQueue(label: "ccvo.input.send")

    /// Leert das aktuelle Eingabefeld der Electron-App via Cmd+A + Backspace.
    /// Electron-Apps (Claude Desktop, Codex) haben ein einzelnes Texteingabe-
    /// feld — Cmd+A markiert den gesamten Inhalt, Backspace loescht ihn.
    /// Ist das Feld leer, ist die Kombi ein harmloser No-Op.
    static func clearLine() {
        sendQueue.async {
            activateTargetApp()
            usleep(150_000)
            sendKeyCombo(keyCode: 0x00, flags: .maskCommand) // Cmd+A (select all)
            usleep(50_000)
            sendKeyCombo(keyCode: 0x33, flags: [])           // Backspace (delete)
        }
    }

    /// Loescht den GESAMTEN Eingabe-Inhalt — bei Electron-Apps genuegt Cmd+A +
    /// Backspace, da das gesamte Feld (auch mehrzeilig) auf einen Schlag
    /// markiert wird. Zur Sicherheit zweimal hintereinander, falls ein
    /// Auto-Suggest-Popup den ersten Anschlag abfaengt. Pendant zu
    /// `TerminalController.clearAllInput()` (das 5× Ctrl+U sendet).
    static func clearAllInput() {
        sendQueue.async {
            activateTargetApp()
            usleep(150_000)
            for _ in 0..<2 {
                sendKeyCombo(keyCode: 0x00, flags: .maskCommand) // Cmd+A
                usleep(40_000)
                sendKeyCombo(keyCode: 0x33, flags: [])           // Backspace
                usleep(40_000)
            }
        }
    }

    /// Fuegt Text via Clipboard + Cmd+V ein, optional gefolgt von Enter.
    /// Sichert und stellt den vorherigen Clipboard-Inhalt wieder her.
    /// Alle blockierenden Arbeiten laufen auf der seriellen Send-Queue, der
    /// Aufrufer-Thread (meist Main) kehrt sofort zurueck.
    static func pasteText(_ text: String, autoEnter: Bool = false) {
        tvoDebug("[Input] pasteText start textLen=\(text.count) autoEnter=\(autoEnter) lastActiveTarget=\(lastActiveTargetBundleID ?? "<nil>")")
        sendQueue.async {
            let pasteboard = NSPasteboard.general
            let previousContents = pasteboard.string(forType: .string)

            pasteboard.clearContents()
            pasteboard.setString(text, forType: .string)
            tvoDebug("[Input] pasteText pasteboard set, calling activateTargetApp")

            activateTargetApp()
            usleep(150_000)
            let frontApp = NSWorkspace.shared.frontmostApplication?.bundleIdentifier ?? "<unknown>"
            tvoDebug("[Input] pasteText post-activate frontApp=\(frontApp), sending Cmd+V")
            sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V
            if autoEnter {
                usleep(300_000)
                sendKeyCombo(keyCode: 0x24, flags: []) // Return
            }
            // Vorherigen Clipboard-Inhalt nach dem Einfuegen wiederherstellen.
            // Bleibt auf der Send-Queue, laeuft also nach einem evtl.
            // nachfolgenden Send — nie mitten in einem haengenden Paste.
            sendQueue.asyncAfter(deadline: .now() + 0.5) {
                if let previous = previousContents {
                    let pb = NSPasteboard.general
                    pb.clearContents()
                    pb.setString(previous, forType: .string)
                }
            }
        }
    }

    /// Kopiert die aktuelle Auswahl in der Ziel-App via Cmd+C
    static func copySelection() {
        sendQueue.async {
            activateTargetApp()
            usleep(50_000)
            sendKeyCombo(keyCode: 0x08, flags: .maskCommand) // Cmd+C
        }
    }

    /// Fuegt den Clipboard-Inhalt in die Ziel-App ein via Cmd+V
    static func pasteClipboard() {
        sendQueue.async {
            activateTargetApp()
            usleep(50_000)
            sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V
        }
    }

    /// Sendet Return (Enter) an die Ziel-App
    static func pressReturn() {
        sendQueue.async {
            activateTargetApp()
            usleep(100_000)
            sendKeyCombo(keyCode: 0x24, flags: []) // Return
        }
    }

    /// Holt die zuletzt aktive Ziel-App (Claude Desktop / Codex) nach vorne,
    /// damit CGEvent dort ankommt. Faellt — wenn keine bekannte Ziel-App in der
    /// Liste ist — auf die front-most Nicht-Selbst-App zurueck, damit Cmd+V
    /// nicht an unser eigenes Overlay geht (was System-Beeps ausloest).
    /// 1:1 Logik wie `TerminalController.activateTerminal()`.
    static func activateTargetApp() {
        // Bevorzugt die zuletzt fokussierte Ziel-App
        if let bundleID = lastActiveTargetBundleID,
           let app = NSWorkspace.shared.runningApplications
            .first(where: { $0.bundleIdentifier == bundleID }) {
            tvoDebug("[Input] activateTargetApp -> last active: \(bundleID)")
            app.activate()
            return
        }
        // Fallback 1: irgendeine laufende bekannte Ziel-App
        if let app = NSWorkspace.shared.runningApplications
            .first(where: { AppWatcher.isTargetApp($0.bundleIdentifier) }) {
            tvoDebug("[Input] activateTargetApp -> known-list fallback: \(app.bundleIdentifier ?? "?")")
            app.activate()
            return
        }
        // Fallback 2: front-most App, sofern nicht wir selbst sind.
        let myBundleID = Bundle.main.bundleIdentifier
        if let frontApp = NSWorkspace.shared.frontmostApplication,
           frontApp.bundleIdentifier != myBundleID {
            tvoDebug("[Input] activateTargetApp -> frontmost-fallback: \(frontApp.bundleIdentifier ?? "?")")
            frontApp.activate()
            return
        }
        tvoDebug("[Input] activateTargetApp -> KEIN Ziel gefunden! Cmd+V wuerde an uns selbst gehen → Beep")
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

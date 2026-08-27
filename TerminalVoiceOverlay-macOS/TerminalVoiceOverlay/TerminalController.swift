import AppKit
import Foundation

/// Tastatur- und Zwischenablage-Steuerung des Ziel-Terminals.
///
/// Vollstaendige Portierung von TerminalVoiceOverlay-Windows/Services/TerminalController.cs.
/// Uebernommen wurden dabei die sechs Sicherungen, die auf macOS bisher fehlten:
///
///  1. **Erfolgs-Rueckmeldung** — jede Aktion liefert einen Bool. Vorher war alles
///     fire-and-forget, ein fehlgeschlagenes Einfuegen blieb unbemerkt.
///  2. **Zwischenablage-Sequenznummer** — vor dem Wiederherstellen wird geprueft, ob
///     die Ablage zwischenzeitlich von aussen geaendert wurde. Windows nutzt dafuer
///     `GetClipboardSequenceNumber()`, macOS `NSPasteboard.changeCount`. Ohne diese
///     Pruefung ueberschrieb das Overlay 0,5 s nach jedem Einfuegen blind das, was der
///     Benutzer inzwischen kopiert hatte.
///  3. **Alle Ablage-Formate** — Windows sichert per `GetDataObject()` jedes Format,
///     nicht nur Text. Hier uebernehmen `NSPasteboardItem`-Kopien dieselbe Rolle; ein
///     kopiertes Bild ueberlebt das Einfuegen jetzt.
///  4. **Wiederholversuche bei belegter Ablage** — 5 Versuche mit Backoff 30/60/120/240 ms.
///  5. **Vordergrund-Verifikation** — nach dem Aktivieren wird geprueft, ob die Ziel-App
///     wirklich vorn ist. Schlaegt das fehl, wird Cmd+V NICHT gesendet (sonst landet der
///     Text in einem fremden Fenster). Windows macht das mit `IsSameOrDescendant`.
///  6. **Zweite Fokuspruefung vor Auto-Enter** — Windows holt das Fenster vor dem Return
///     erneut nach vorn; ohne das kann das Return in einer anderen App landen.
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
    /// Windows-Pendant: `lock (InputSequenceGate)`.
    private static let sendQueue = DispatchQueue(label: "tvo.terminal.send")

    // MARK: - Konstanten (1:1 Windows)

    /// Wartezeit zwischen Einfuegen und Auto-Enter (Windows: Thread.Sleep(300)).
    private static let autoEnterDelayUs: UInt32 = 300_000
    /// Wartezeit vor dem Wiederherstellen der Ablage (Windows: Thread.Sleep(500)).
    private static let clipboardRestoreDelay: TimeInterval = 0.5
    /// Versuche fuer eine belegte Zwischenablage (Windows: maxAttempts = 5).
    private static let clipboardMaxAttempts = 5
    /// Maximale Wartezeit auf den Fokuswechsel, bevor aufgegeben wird.
    private static let foregroundTimeoutUs: UInt32 = 600_000
    /// Pollintervall der Fokuspruefung.
    private static let foregroundPollUs: UInt32 = 30_000

    // MARK: - Oeffentliche Aktionen

    /// Clears the current terminal input line via Ctrl+U (kill-line-to-start).
    /// Ctrl+U is the standard readline/bash/zsh shortcut for clearing the input
    /// buffer WITHOUT sending SIGINT. Critical for Claude Code CLI and similar
    /// TUIs: Ctrl+C would interrupt a running task ("Interrupted"), but Ctrl+U
    /// only clears the text in the input line. If no input is present (task is
    /// running), Ctrl+U is a safe no-op.
    static func clearLine(completion: ((Bool) -> Void)? = nil) {
        sendQueue.async {
            guard bringToForeground() else { report(completion, false, "clearLine: Ziel nicht im Vordergrund"); return }
            // 'u' = 0x20 on macOS CGKeyCode; Ctrl+U = kill line to start of input
            let ok = sendKeyCombo(keyCode: 0x20, flags: .maskControl)
            report(completion, ok, nil)
        }
    }

    /// Loescht den GESAMTEN Eingabe-Buffer — auch bei mehrzeiligem Input
    /// (z.B. Claude Code CLI mit Shift+Enter-Zeilen). Drueckt Ctrl+U fuenfmal
    /// hintereinander mit 50 ms Pause, bis garantiert nichts mehr in der
    /// Eingabezeile steht. Ctrl+U ist ein harmloser No-Op wenn der Buffer
    /// leer ist, also kein Risiko bei zu vielen Wiederholungen.
    ///
    /// Wird beim Profil-Wechsel im Voice-Overlay verwendet, damit der zuletzt
    /// eingefuegte Prompt restlos verschwindet, bevor die neue Korrektur
    /// reingepastet wird.
    static func clearAllInput(completion: ((Bool) -> Void)? = nil) {
        sendQueue.async {
            guard bringToForeground() else { report(completion, false, "clearAllInput: Ziel nicht im Vordergrund"); return }
            for _ in 0..<5 {
                if !sendKeyCombo(keyCode: 0x20, flags: .maskControl) {
                    report(completion, false, "clearAllInput: Tastenereignis fehlgeschlagen")
                    return
                }
                usleep(50_000)
            }
            report(completion, true, nil)
        }
    }

    /// Pastes text via clipboard + Cmd+V, optionally sends Enter afterwards.
    /// Sichert und stellt den vorherigen Ablage-Inhalt wieder her — inklusive
    /// aller Formate und nur dann, wenn die Ablage in der Zwischenzeit nicht
    /// von aussen geaendert wurde.
    /// All blocking work (activateTerminal, usleep, CGEvent.post) runs on the
    /// serial send queue so the caller's thread (usually main) returns instantly.
    static func pasteText(_ text: String, autoEnter: Bool = false,
                          completion: ((Bool) -> Void)? = nil) {
        tvoDebug("[Term] pasteText start textLen=\(text.count) autoEnter=\(autoEnter) lastActiveTerminal=\(lastActiveTerminalBundleID ?? "<nil>")")
        let totalStart = DiagLog.now()
        DiagLog.write("Paste", "start", [("chars", text.count), ("autoEnter", autoEnter),
                                         ("target", lastActiveTerminalBundleID ?? "nil")])
        sendQueue.async {
            // Vorherigen Inhalt in ALLEN Formaten sichern (Windows: GetDataObject).
            let previousItems = snapshotPasteboard()
            var ownedChangeCount = 0
            var pasteSent = false
            var succeeded = false

            // Schreiben mit Wiederholversuchen — die Ablage kann kurzzeitig von
            // einem anderen Prozess belegt sein (Windows: CLIPBRD_E_CANT_OPEN).
            let clipboardSet = withClipboardRetry {
                let pb = NSPasteboard.general
                pb.clearContents()
                guard pb.setString(text, forType: .string) else { return false }
                ownedChangeCount = pb.changeCount
                return true
            }

            defer {
                DiagLog.perf("Paste", "total", since: totalStart,
                             [("chars", text.count), ("autoEnter", autoEnter), ("ok", succeeded)])
                if clipboardSet {
                    // Erst nach dem Einfuegen zuruecksetzen — sonst holt sich das
                    // Terminal den alten Inhalt (Windows: Thread.Sleep(500)).
                    if pasteSent { Thread.sleep(forTimeInterval: clipboardRestoreDelay) }
                    restorePasteboard(previousItems, ownedChangeCount: ownedChangeCount)
                }
                report(completion, succeeded, nil)
            }

            guard clipboardSet else {
                NSLog("pasteText: Zwischenablage nicht beschreibbar — Einfuegen uebersprungen.")
                DiagLog.warn("Paste", "clipboard_set_failed", [("chars", text.count)])
                return
            }

            tvoDebug("[Term] pasteText pasteboard set, calling activateTerminal")
            guard bringToForeground() else {
                tvoDebug("[Term] pasteText: Ziel kam nicht in den Vordergrund — Cmd+V wird NICHT gesendet")
                DiagLog.warn("Paste", "foreground_failed_skip_cmd_v")
                return
            }

            let frontApp = NSWorkspace.shared.frontmostApplication?.bundleIdentifier ?? "<unknown>"
            tvoDebug("[Term] pasteText post-activate frontApp=\(frontApp), sending Cmd+V")
            pasteSent = sendKeyCombo(keyCode: 0x09, flags: .maskCommand) // Cmd+V
            succeeded = pasteSent
            DiagLog.write("Paste", "cmd_v", [("ok", pasteSent)])

            if pasteSent && autoEnter {
                usleep(autoEnterDelayUs)
                // Windows holt das Fenster vor dem Return ERNEUT nach vorn: in den
                // 300 ms kann der Fokus gewandert sein, und ein Return in einer
                // fremden App loest dort irgendetwas aus.
                guard bringToForeground() else {
                    tvoDebug("[Term] pasteText: Fokus vor Auto-Enter verloren — Return wird NICHT gesendet")
                    DiagLog.warn("Paste", "foreground_failed_skip_auto_enter")
                    succeeded = false
                    return
                }
                if !sendKeyCombo(keyCode: 0x24, flags: []) { // Return
                    succeeded = false
                }
            }
        }
    }

    /// Copies the current selection in the terminal via Cmd+C
    static func copySelection(completion: ((Bool) -> Void)? = nil) {
        sendQueue.async {
            guard bringToForeground() else { report(completion, false, "copySelection: Ziel nicht im Vordergrund"); return }
            report(completion, sendKeyCombo(keyCode: 0x08, flags: .maskCommand), nil) // Cmd+C
        }
    }

    /// Pastes clipboard content into the terminal via Cmd+V.
    /// Aendert die Ablage NICHT — fuegt ein, was ohnehin drin steht.
    static func pasteClipboard(completion: ((Bool) -> Void)? = nil) {
        sendQueue.async {
            guard bringToForeground() else { report(completion, false, "pasteClipboard: Ziel nicht im Vordergrund"); return }
            report(completion, sendKeyCombo(keyCode: 0x09, flags: .maskCommand), nil) // Cmd+V
        }
    }

    /// Sends Return (Enter) to the terminal
    static func pressReturn(completion: ((Bool) -> Void)? = nil) {
        sendQueue.async {
            guard bringToForeground() else { report(completion, false, "pressReturn: Ziel nicht im Vordergrund"); return }
            report(completion, sendKeyCombo(keyCode: 0x24, flags: []), nil) // Return
        }
    }

    /// Sendet Return OHNE vorherigen Fokuswechsel (Windows: SendReturn).
    /// Genutzt vom Enter-Knopf, wenn Auto-Enter eingeschaltet wird und sofort
    /// ein Return ausgeloest werden soll, waehrend das Terminal schon vorn ist.
    @discardableResult
    static func sendReturn() -> Bool {
        var ok = false
        sendQueue.sync { ok = sendKeyCombo(keyCode: 0x24, flags: []) }
        return ok
    }

    // MARK: - Vordergrund

    /// Holt das Ziel-Terminal nach vorn und PRUEFT das Ergebnis.
    ///
    /// Windows-Pendant `BringToForeground`: dort wird nach `SetForegroundWindow`
    /// mit `GetForegroundWindow()` + `IsSameOrDescendant` verifiziert und bei
    /// Misserfolg das anschliessende Ctrl+V uebersprungen. Auf macOS ist das
    /// Gegenstueck `NSRunningApplication.isActive`, das nach `activate()`
    /// gepollt wird. Ohne diese Pruefung ging ein Cmd+V bei fehlgeschlagenem
    /// Fokuswechsel an die falsche App.
    @discardableResult
    private static func bringToForeground() -> Bool {
        guard let app = resolveTargetApp() else {
            tvoDebug("[Term] bringToForeground -> KEIN Ziel gefunden! Cmd+V wuerde an uns selbst gehen → Beep")
            return false
        }

        // Schon vorn? Dann nur kurz durchatmen (Windows: Thread.Sleep(30)).
        if app.isActive {
            usleep(30_000)
            return true
        }

        app.activate()

        // Auf den Fokuswechsel warten statt blind zu schlafen. Der frueher feste
        // usleep(150_000) war je nach Systemlast mal zu kurz (Paste ging daneben)
        // und mal unnoetig lang.
        var waited: UInt32 = 0
        while waited < foregroundTimeoutUs {
            usleep(foregroundPollUs)
            waited += foregroundPollUs
            if app.isActive { break }
        }

        let ok = app.isActive
        tvoDebug("[Term] bringToForeground app=\(app.bundleIdentifier ?? "?") ok=\(ok) waitedMs=\(waited / 1000)")
        if !ok {
            NSLog("TerminalController: Fokuswechsel zu %@ fehlgeschlagen — Tastenereignis wird nicht gesendet.",
                  app.bundleIdentifier ?? "?")
            DiagLog.warn("Paste", "foreground_verify_failed",
                         [("app", app.bundleIdentifier ?? "?"), ("waitedMs", waited / 1000)])
        }
        return ok
    }

    /// Ermittelt die Ziel-App: zuletzt aktives Terminal, sonst eine bekannte
    /// Terminal-App, sonst die vorderste fremde App. Reihenfolge wie bisher.
    private static func resolveTargetApp() -> NSRunningApplication? {
        if let bundleID = lastActiveTerminalBundleID,
           let app = NSWorkspace.shared.runningApplications
            .first(where: { $0.bundleIdentifier == bundleID }) {
            tvoDebug("[Term] target -> last active: \(bundleID)")
            return app
        }
        if let app = NSWorkspace.shared.runningApplications
            .first(where: { AppWatcher.isTargetApp($0.bundleIdentifier) }) {
            tvoDebug("[Term] target -> known-list fallback: \(app.bundleIdentifier ?? "?")")
            return app
        }
        // Fallback: vorderste App, sofern nicht wir selbst. Besser als nichts zu
        // tun (sonst landet Cmd+V im Voice-Overlay → Beep).
        let myBundleID = Bundle.main.bundleIdentifier
        if let frontApp = NSWorkspace.shared.frontmostApplication,
           frontApp.bundleIdentifier != myBundleID {
            tvoDebug("[Term] target -> frontmost-fallback: \(frontApp.bundleIdentifier ?? "?")")
            return frontApp
        }
        return nil
    }

    /// Bringt das Ziel nach vorn, ohne das Ergebnis auszuwerten. Bleibt fuer
    /// bestehende Aufrufer erhalten, die nur "Fokus hin" wollen.
    static func activateTerminal() {
        resolveTargetApp()?.activate()
    }

    // MARK: - Zwischenablage

    /// Kopiert den kompletten Ablage-Inhalt (ALLE Formate) in freistehende Items.
    /// `NSPasteboardItem`-Objekte aus `pasteboard.pasteboardItems` werden beim
    /// naechsten `clearContents()` entwertet — deshalb wird jedes Item mit seinen
    /// Daten in ein neues Item umkopiert.
    private static func snapshotPasteboard() -> [NSPasteboardItem] {
        guard let items = NSPasteboard.general.pasteboardItems else { return [] }
        var copies: [NSPasteboardItem] = []
        for item in items {
            let copy = NSPasteboardItem()
            var hasData = false
            for type in item.types {
                if let data = item.data(forType: type) {
                    copy.setData(data, forType: type)
                    hasData = true
                }
            }
            if hasData { copies.append(copy) }
        }
        return copies
    }

    /// Stellt den gesicherten Inhalt wieder her — aber NUR, wenn die Ablage seit
    /// dem eigenen Schreiben unveraendert ist.
    ///
    /// Windows vergleicht dafuer `GetClipboardSequenceNumber()`; hat sich die Zahl
    /// geaendert, hat jemand anders geschrieben und die Wiederherstellung wird
    /// uebersprungen. Genau das fehlte hier: das Overlay schrieb 0,5 s nach jedem
    /// Einfuegen blind den alten Inhalt zurueck und zerstoerte damit alles, was der
    /// Benutzer in der Zwischenzeit kopiert hatte.
    private static func restorePasteboard(_ items: [NSPasteboardItem], ownedChangeCount: Int) {
        let pb = NSPasteboard.general
        guard pb.changeCount == ownedChangeCount else {
            NSLog("Clipboard restore skipped — clipboard changed externally.")
            DiagLog.write("Paste", "clipboard_restore_skipped_external_change")
            tvoDebug("[Term] Ablage extern geaendert (\(pb.changeCount) != \(ownedChangeCount)) — nicht wiederhergestellt")
            return
        }
        _ = withClipboardRetry {
            let pb = NSPasteboard.general
            pb.clearContents()
            if items.isEmpty { return true }
            return pb.writeObjects(items)
        }
    }

    /// Fuehrt eine Ablage-Operation mit bis zu `clipboardMaxAttempts` Versuchen aus.
    /// Backoff 30/60/120/240 ms wie in Windows — typische Sperren (ein anderes
    /// Programm haelt die Ablage gerade offen) sind binnen ~100 ms wieder weg.
    private static func withClipboardRetry(_ operation: () -> Bool) -> Bool {
        for attempt in 1...clipboardMaxAttempts {
            if operation() { return true }
            if attempt < clipboardMaxAttempts {
                let backoffMs = 30 * (1 << (attempt - 1))
                NSLog("Clipboard busy (attempt %d/%d) — retry in %d ms", attempt, clipboardMaxAttempts, backoffMs)
                usleep(UInt32(backoffMs) * 1000)
            }
        }
        NSLog("Clipboard operation failed after %d attempts", clipboardMaxAttempts)
        return false
    }

    // MARK: - Tastatur

    /// Sendet eine Tastenkombination und meldet, ob beide Ereignisse erzeugt
    /// werden konnten. Die Flags werden explizit gesetzt und ueberschreiben damit
    /// physisch gehaltene Modifier — das ist das macOS-Gegenstueck zu Windows'
    /// `ReleaseNonCtrlModifiers()`: ein per Hotkey ausgeloestes Einfuegen darf beim
    /// Ziel nicht als "Cmd+Alt+Shift+V" ankommen, nur weil der Benutzer die
    /// Hotkey-Modifier noch haelt.
    @discardableResult
    static func sendKeyCombo(keyCode: CGKeyCode, flags: CGEventFlags) -> Bool {
        let source = CGEventSource(stateID: .hidSystemState)

        guard let keyDown = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: true),
              let keyUp = CGEvent(keyboardEventSource: source, virtualKey: keyCode, keyDown: false) else {
            NSLog("CGEvent creation failed")
            return false
        }

        keyDown.flags = flags
        keyUp.flags = flags

        keyDown.post(tap: .cghidEventTap)
        keyUp.post(tap: .cghidEventTap)
        return true
    }

    // MARK: - Hilfen

    private static func report(_ completion: ((Bool) -> Void)?, _ ok: Bool, _ warning: String?) {
        if let warning = warning, !ok { tvoDebug("[Term] \(warning)") }
        guard let completion = completion else { return }
        DispatchQueue.main.async { completion(ok) }
    }
}

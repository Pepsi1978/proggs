import Foundation
import Carbon
import AppKit

/// Carbon-API-Wrapper fuer globale Tastatur-Hotkeys auf macOS.
///
/// Mirrors the Windows-side `HotkeyRegistry.cs` and `Win32` low-level
/// keyboard hooks: hier registrieren wir mit `RegisterEventHotKey` aus dem
/// Carbon-Framework, das ist die einzige API auf macOS die OHNE Accessibility-
/// Permissions funktioniert (Cocoa's `NSEvent.addGlobalMonitorForEvents`
/// braucht zusaetzlich Input-Monitoring, was wir vermeiden wollen).
///
/// Standard-Pattern: einmal `installEventHandlerOnce()` aufrufen, dann beliebig
/// oft `register(keyCode:modifiers:handler:)`. Beim App-Beenden
/// `unregisterAll()` damit das System sauber aufraeumt.
final class HotkeyRegistry {

    static let shared = HotkeyRegistry()

    /// Logischer Name -> Closure die ausgefuehrt wird wenn der Hotkey feuert.
    /// Wird via `register(...)` befuellt und ueber die Carbon-Event-ID
    /// dispatched.
    private var handlers: [UInt32: () -> Void] = [:]
    private var hotKeyRefs: [EventHotKeyRef] = []
    private var nextID: UInt32 = 1
    private var eventHandlerInstalled = false

    private init() {}

    /// Registriert einen globalen Hotkey. KeyCode = Carbon `kVK_*` Konstanten.
    /// Modifier = Carbon Modifier-Flags (cmdKey, shiftKey, optionKey, controlKey).
    /// Handler wird auf dem Main-Thread aufgerufen.
    @discardableResult
    func register(keyCode: UInt32, modifiers: UInt32, handler: @escaping () -> Void) -> Bool {
        installEventHandlerOnce()

        let id = nextID
        nextID += 1

        var hotKeyID = EventHotKeyID()
        hotKeyID.signature = OSType(0x544F564F) // 'TOVO' = TerminalOverlayVoice
        hotKeyID.id = id

        var hotKeyRef: EventHotKeyRef?
        let status = RegisterEventHotKey(
            keyCode, modifiers, hotKeyID,
            GetApplicationEventTarget(), 0, &hotKeyRef)

        if status == noErr, let ref = hotKeyRef {
            hotKeyRefs.append(ref)
            handlers[id] = handler
            NSLog("[HotkeyRegistry] registered keyCode=%u modifiers=0x%X id=%u", keyCode, modifiers, id)
            return true
        } else {
            NSLog("[HotkeyRegistry] FAILED keyCode=%u modifiers=0x%X status=%d", keyCode, modifiers, status)
            return false
        }
    }

    /// Loest registrierte Handler zur entsprechenden Hotkey-ID aus.
    /// Wird aus dem Carbon-Event-Handler heraus auf den Main-Thread gepostet.
    fileprivate func fireHandler(id: UInt32) {
        handlers[id]?()
    }

    /// Cleanup beim App-Beenden — wir geben alle Carbon-Hotkey-Refs frei
    /// damit das System keinen "verwaisten" Hotkey behaelt nach App-Restart.
    func unregisterAll() {
        for ref in hotKeyRefs {
            UnregisterEventHotKey(ref)
        }
        hotKeyRefs.removeAll()
        handlers.removeAll()
    }

    private func installEventHandlerOnce() {
        if eventHandlerInstalled { return }
        eventHandlerInstalled = true

        var eventType = EventTypeSpec()
        eventType.eventClass = OSType(kEventClassKeyboard)
        eventType.eventKind = UInt32(kEventHotKeyPressed)

        InstallEventHandler(
            GetApplicationEventTarget(),
            { _, eventRef, _ -> OSStatus in
                guard let eventRef = eventRef else { return noErr }
                var hotKeyID = EventHotKeyID()
                let err = GetEventParameter(
                    eventRef,
                    EventParamName(kEventParamDirectObject),
                    EventParamType(typeEventHotKeyID),
                    nil,
                    MemoryLayout<EventHotKeyID>.size,
                    nil,
                    &hotKeyID)
                guard err == noErr else { return noErr }
                let id = hotKeyID.id
                DispatchQueue.main.async {
                    HotkeyRegistry.shared.fireHandler(id: id)
                }
                return noErr
            },
            1, &eventType, nil, nil)
    }
}

/// Symbolische Konstanten fuer die TerminalVoiceOverlay-Hotkey-Belegung.
/// Keine Magic-Numbers in AppDelegate. Werte aus `<Carbon/HIToolbox/Events.h>`.
enum TVOHotkey {
    /// Cmd+Shift+R — Voice-Toggle (start/stop recording). R = "Record".
    static let voiceToggle = (keyCode: UInt32(kVK_ANSI_R), modifiers: UInt32(cmdKey | shiftKey))

    /// Cmd+Shift+S — Screenshot machen.
    static let screenshot = (keyCode: UInt32(kVK_ANSI_S), modifiers: UInt32(cmdKey | shiftKey))

    /// Cmd+Shift+I — Letzten Screenshot-Pfad einfuegen.
    static let insertScreenshot = (keyCode: UInt32(kVK_ANSI_I), modifiers: UInt32(cmdKey | shiftKey))

    /// Cmd+Shift+E — Finder am Release-Bundle-Pfad oeffnen.
    /// Pendant zu Windows Alt+F11.
    static let openReleaseBundle = (keyCode: UInt32(kVK_ANSI_E), modifiers: UInt32(cmdKey | shiftKey))

    /// Cmd+Shift+O — Orientation umschalten (vertikal <-> horizontal).
    /// Dev-Hotkey solange der OrientationToggleButton in der UI noch fehlt.
    /// Pendant zu Windows OrientationToggleButton-Click.
    static let orientationToggle = (keyCode: UInt32(kVK_ANSI_O), modifiers: UInt32(cmdKey | shiftKey))

    /// Cmd+1..9 — Prompt-Hotkeys (mappt auf gespeicherte HotkeyNumber).
    /// `digit` muss 1..9 sein, andere Werte werden ignoriert.
    static func promptDigit(_ digit: Int) -> (keyCode: UInt32, modifiers: UInt32)? {
        let codes: [Int: UInt32] = [
            1: UInt32(kVK_ANSI_1),
            2: UInt32(kVK_ANSI_2),
            3: UInt32(kVK_ANSI_3),
            4: UInt32(kVK_ANSI_4),
            5: UInt32(kVK_ANSI_5),
            6: UInt32(kVK_ANSI_6),
            7: UInt32(kVK_ANSI_7),
            8: UInt32(kVK_ANSI_8),
            9: UInt32(kVK_ANSI_9),
        ]
        guard let code = codes[digit] else { return nil }
        return (keyCode: code, modifiers: UInt32(cmdKey))
    }
}

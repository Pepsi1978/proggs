# macOS-Overlay-Fenster (Swift/AppKit) — Best Practices (Stand 2026-06-14, Swift 6 / Xcode 26, macOS 13–26 Tahoe)

> Für Franks Voice-Overlays **ClaudeCodexVoiceOverlay-macOS** und **TerminalVoiceOverlay-macOS**:
> schwebende Overlays mit globalem Hotkey und Mikrofon-Zugriff, die NICHT den Fokus klauen.
> Versions-Anker (live ermittelt): Overlay-Projekte mit deployment target **macOS 13/14**, swift-tools
> **6.1/6.2** (Swift 6), Xcode 26.x; aktuelles macOS **26 Tahoe**. Apple Developer zuerst, Community als
> `extern` markiert. Ergänzt die allgemeine `best-practices-swift-appkit.md` um den Overlay-Spezialfall.
> Zweite Seite (was schiefgeht): [`bugs/desktop/macos-overlay.md`](../../bugs/desktop/macos-overlay.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Overlay soll schweben, ohne Fokus zu klauen | `NSPanel`-Subklasse + `.nonactivatingPanel`; `canBecomeMain=false` | §2 |
| 2 | Overlay zeigen | `orderFrontRegardless()`, NIE `activate(ignoringOtherApps:)` (deprecated) | §2 |
| 3 | Start ohne Dock-Icon/Fokus-Klau | `LSUIElement` + Start-Sequenz `.prohibited` → `.accessory` | §2 |
| 4 | Auf allen Spaces + über Fullscreen | `collectionBehavior=[.canJoinAllSpaces, .fullScreenAuxiliary, .stationary]` | §3 |
| 5 | Durchklickbares Overlay | `ignoresMouseEvents=true` (ganz) oder `hitTest`-Override (teilweise) | §4 |
| 6 | Transparenz | `isOpaque=false` + `backgroundColor=.clear`, Transluzenz im View | §4 |
| 7 | Globaler Toggle-Hotkey | Carbon `RegisterEventHotKey` (keine Permission) — Kombi MUSS ⌘/⌃ enthalten | §5 |
| 8 | Push-to-Talk / Taste abfangen | `CGEvent.tapCreate` + Input Monitoring; Tap re-enablen + Wake-Handling | §5 |
| 9 | Welche Permission? | Nur mitlesen → **Input Monitoring**; abfangen/injizieren → **+ Accessibility** | §6 |
| 10 | Mikrofon | `NSMicrophoneUsageDescription` (Pflicht) + `AVCaptureDevice`; Live-Hardware-Format | §7 |
| 11 | Autostart | `SMAppService.mainApp`; Status live lesen; opt-in (Default aus) | §8 |
| 12 | Verteilen | Developer ID + Hardened Runtime (KEINE Sandbox) + notarisieren + stapeln | §9 |
| 13 | Permission weg nach Update | Stabile Developer-ID-Identität — TCC bindet an die Signatur | §6, §9 |

---

## §1 Architektur & Grundsatz

Ein Voice-Overlay ist ein **schwebendes, nonactivating NSPanel** in einer **Accessory-App** (kein Dock-Icon,
nur Menüleisten-Icon). Leitprinzip: Das Overlay drängt sich **nie** in den Vordergrund — der Nutzer bleibt in
seiner App, das Overlay zeigt nur Status/Pegel und reagiert auf Hotkey/Sprache. Drei harte Lehren aus der
Recherche prägen alles: (1) Seit macOS 14 ist `activate(ignoringOtherApps:)` deprecated — Aktivierung ist eine
*ablehnbare Bitte* (cooperative activation), also gar nicht erst aktivieren. (2) TCC-Permissions (Accessibility,
Input Monitoring, Mikrofon) binden an die **Code-Signatur** — eine stabile Developer-ID-Identität ist Pflicht,
sonst ist die Erlaubnis nach jedem Rebuild/Update weg. (3) macOS 15/26 brachten reale Regressionen bei
Overlay-/Tap-/Audio-Verhalten — auf der Zielversion testen.

---

## §2 NSPanel: schwebend & nonactivating (ohne Fokus-Klau)

`NSPanel` (nicht nacktes `NSWindow`) ist Pflicht: `.nonactivatingPanel` gilt kanonisch nur für NSPanel und
sorgt dafür, dass ein Klick die App NICHT aktiviert (kein Menüleisten-Übernahme, kein Fokus-Klau), das Panel
aber dennoch Tastatur/Text annehmen kann.

```swift
final class VoiceOverlayPanel: NSPanel {
    init(contentRect: NSRect) {
        super.init(contentRect: contentRect,
                   styleMask: [.nonactivatingPanel, .titled, .fullSizeContentView],
                   backing: .buffered, defer: false)
        self.level = .floating              // Apple-empfohlen für Overlays (NICHT Maximum-Level!)
        self.isFloatingPanel = true
        self.becomesKeyOnlyIfNeeded = true   // nur Key, wenn z.B. ein TextField es braucht
        self.hidesOnDeactivate = false
        self.isReleasedWhenClosed = false
        self.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]  // §3
        self.isOpaque = false; self.backgroundColor = .clear                  // §4
        self.titleVisibility = .hidden; self.titlebarAppearsTransparent = true
    }
    override var canBecomeKey: Bool { true }   // damit Texteingabe funktioniert
    override var canBecomeMain: Bool { false }  // verhindert App-Aktivierung / Fokus-Klau
}
```

**App-Setup (kein Dock-Icon, kein Fokus-Klau beim Start):** `LSUIElement = YES` in Info.plist. Wichtig:
Selbst mit `LSUIElement` klaut die App beim Launch den Fokus — die Lösung (Multi.app-Pattern) ist die
Start-Sequenz **`.prohibited` → `.accessory`**:

```swift
func applicationWillFinishLaunching(_ n: Notification) { NSApp.setActivationPolicy(.prohibited) }
func applicationDidFinishLaunching(_ n: Notification) {
    NSApp.setActivationPolicy(.accessory)          // kein Dock-Icon, Fenster nutzbar
    panel.orderFrontRegardless()                    // zeigen OHNE zu aktivieren
}
```

**Sichtbar machen ausschließlich mit `orderFrontRegardless()`** — niemals mit `makeKeyAndOrderFront()` (holt
Key-Status) oder dem deprecated `activate(ignoringOtherApps:)`. **StyleMask nicht zur Laufzeit umschalten**
(bricht die nonactivating-Eingabe, siehe Bug N1) — lieber zwei Panel-Instanzen.

Quellen: [NSPanel](https://developer.apple.com/documentation/appkit/nspanel) · [nonactivatingPanel](https://developer.apple.com/documentation/appkit/nswindow/stylemask-swift.struct/nonactivatingpanel) · [Cooperative activation (WWDC23 10054)](https://developer.apple.com/videos/play/wwdc2023/10054/) · offiziell. `extern`: [Multi.app](https://multi.app/blog/nailing-the-activation-behavior-of-a-spotlight-raycast-like-command-palette), [philz.blog](https://philz.blog/nspanel-nonactivating-style-mask-flag/).

---

## §3 Multi-Space & über Fullscreen-Apps

`collectionBehavior` hat **drei sich gegenseitig ausschließende Gruppen** (pro Gruppe max. eine Option,
per ODER kombiniert): **Spaces** (`.canJoinAllSpaces` / `.moveToActiveSpace`), **Exposé** (`.managed` /
`.transient` / `.stationary`), **Cycling** (`.participatesInCycle` / `.ignoresCycle`).

Für ein dauer-sichtbares Overlay über allem:

```swift
panel.collectionBehavior = [.canJoinAllSpaces,   // auf ALLEN Spaces (wie die Menüleiste)
                            .fullScreenAuxiliary,  // auch ÜBER Fullscreen-Apps
                            .stationary,           // von Exposé/Mission Control unberührt
                            .ignoresCycle]         // nicht in Cmd-` Fensterrotation
```

**Kritische Asymmetrie:** `.canJoinAllSpaces` allein zeigt das Overlay auf allen *normalen* Spaces, aber es
**verschwindet** beim Wechsel in eine Fullscreen-App — dafür ist `.fullScreenAuxiliary` zwingend zusätzlich
nötig. Über dem Fullscreen-Fenster zu *liegen* erfordert außerdem ein hohes Level (`.statusBar`/`.floating`,
NICHT `.maximumWindow`). **Mehrere Monitore:** bei „Displays have separate Spaces" ein Panel **pro `NSScreen`**
erzeugen; auf `didChangeScreenParametersNotification` reagieren. `NSScreen.main` für die Platzierung meiden
(zeigt bei Accessory-Apps auf den Menüleisten-Screen) — über `NSScreen.screens` / Mausposition wählen.

Quellen: [collectionBehavior](https://developer.apple.com/documentation/appkit/nswindow/collectionbehavior-swift.struct) · [Window Collection Behavior Guide](https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/WinPanel/Articles/SettingWindowCollectionBehavior.html) · offiziell. `extern`: [Apple Forum 26677 (Ken Thomases)](https://developer.apple.com/forums/thread/26677).

---

## §4 Click-through & Transparenz

**Voll durchklickbar** (reines Anzeige-HUD): `ignoresMouseEvents = true` lässt alle Klicks ans Fenster
darunter durch. Maus trotzdem mitlesen geht parallel über `NSEvent.addGlobalMonitorForEvents` (nur Lesen).

**Teilweise interaktiv** (Overlay hat einen Button, Rest durchlässig): NICHT global `ignoresMouseEvents`,
sondern `hitTest` im Host-View überschreiben — `nil` für nicht-interaktive/transparente Bereiche:

```swift
final class HitThroughHostingView<C: View>: NSHostingView<C> {
    override func hitTest(_ p: NSPoint) -> NSView? {
        let hit = super.hitTest(p)
        return (hit is NSControl) ? hit : nil   // nur echte Controls fangen, Rest durchlassen
    }
}
```

**Transparenz** am Fenster über `isOpaque = false` + `backgroundColor = .clear`; die sichtbare Transluzenz/
abgerundeten Ecken im SwiftUI-/View-Layer (z.B. `RoundedRectangle().fill(.black.opacity(0.55))`), `hasShadow`
bei runden Ecken oft `false`. Auf **Tahoe 26.2/26.3** unbedingt testen — dort gibt es aktive AppKit-Regressionen
bei Maus-Events von Overlay-/Custom-styleMask-Fenstern (Bugs S4/S5). Bonus: `sharingType = .none` blendet das
Overlay aus Screen-Recording (Zoom/Teams) aus.

Quellen: [ignoresMouseEvents](https://developer.apple.com/documentation/appkit/nswindow/ignoresmouseevents) · offiziell. `extern`: [Gaitatzis (Translucent Overlay)](https://gaitatzis.medium.com/create-a-translucent-overlay-window-on-macos-in-swift-67d5e000ce90).

---

## §5 Globale Hotkeys

**Entscheidung:** Reiner Toggle-Hotkey → **Carbon `RegisterEventHotKey`** (braucht KEINE Permission, übersteht
Sleep/Wake robust). Push-to-Talk / reine ⌥- oder F-Taste / Taste abfangen → **`CGEvent.tapCreate`** (braucht
Input Monitoring).

**Sequoia-Regel (macOS 15+):** `RegisterEventHotKey` lehnt jede Kombi ab, die **nur Shift und/oder Option**
nutzt (Fehler `-9868`, Anti-Keylogger) — jeder Hotkey braucht mindestens **⌘ oder ⌃**. Eine reine ⌥-Taste als
PTT geht damit nur über CGEventTap.

```swift
// Carbon Toggle-Hotkey: Kombi MUSS cmdKey oder controlKey enthalten (Sequoia+)
RegisterEventHotKey(keyCode, cmdKey | shiftKey, hotKeyID, GetEventDispatcherTarget(), 0, &ref)
```

**CGEventTap-Pflichtregeln (sonst stirbt der Tap):** `kCGEventTapDisabledByTimeout`/`…ByUserInput` in die
eventMask aufnehmen und im Callback sofort `CGEvent.tapEnable(tap:enable:true)`; Callback **schlank** halten
(schwere Arbeit async); auf **`NSWorkspace.didWakeNotification`/`sessionDidBecomeActiveNotification`** den Tap
neu scharf machen (Tahoe-Regression: Tap stirbt nach Sleep/Lock). Ein non-nil Tap ist NICHT automatisch ein
gesunder Tap → Health-Watchdog-Timer (5 s) der `tapIsEnabled` prüft.

`extern`-Lib-Empfehlung: **KeyboardShortcuts** (sindresorhus) für den nutzer-konfigurierbaren Toggle (kein
Prompt, Recorder-UI inklusive, nutzt intern Carbon) + eigener CGEventTap nur für den PTT-Modus.

Quellen: [Apple Forum 707680 (Quinn, CGEventTap)](https://developer.apple.com/forums/thread/707680) · [Forum 735223](https://developer.apple.com/forums/thread/735223) · [tapDisabledByTimeout](https://developer.apple.com/documentation/coregraphics/cgeventtype/tapdisabledbytimeout) · offiziell. `extern`: [Forum 763878 (Sequoia shift/option)](https://developer.apple.com/forums/thread/763878), [KeyboardShortcuts](https://github.com/sindresorhus/KeyboardShortcuts).

---

## §6 TCC: Accessibility vs. Input Monitoring

**Exakte Abgrenzung** (zwei getrennte, NICHT austauschbare TCC-Services):

| | **Input Monitoring** (`kTCCServiceListenEvent`) | **Accessibility** (`kTCCServiceAccessibility`) |
|---|---|---|
| Erlaubt | Events *mitlesen* (`.listenOnly`-Tap) | Events *abfangen/injizieren* + UI-Automation |
| Status | `CGPreflightListenEventAccess()` | `AXIsProcessTrusted()` |
| Prompt | `CGRequestListenEventAccess()` | `AXIsProcessTrustedWithOptions([prompt:true])` |
| Settings-Pane | `…?Privacy_ListenEvent` | `…?Privacy_Accessibility` |

**Regel fürs Voice-Overlay:** Nur Hotkeys *mitlesen* → **Input Monitoring reicht** (`.listenOnly`-Tap). Erst
wenn die App eine Taste *verschluckt* oder Input *injiziert* (`.defaultTap`) → **zusätzlich Accessibility**. Beide
Grants sind unabhängig (Input Monitoring impliziert nicht Accessibility und umgekehrt). Wer gar keinen freien
Input braucht, sondern nur eine feste Kombi → Carbon `RegisterEventHotKey` = **null TCC**.

**Permission-Flow:** Status preflighten (kein Prompt) → fehlt? `CGRequest…` (Prompt kommt **nur einmal** pro
Code-Identität) → Nutzer per `x-apple.systempreferences:…?Privacy_ListenEvent` in den Pane führen (Toggle muss
er selbst setzen). Diagnose/Reset: `tccutil reset Accessibility|ListenEvent <bundleid>`. **Sandbox:** im
Sandbox liefert `AXIsProcessTrusted` immer false — Overlay außerhalb des App Store ohne Sandbox bauen.

> **TCC bindet an die Code-Signatur** — siehe §9: ohne stabile Developer-ID ist die Erlaubnis nach jedem
> Rebuild/Re-Signing weg.

Quellen: [Apple Forum 758554](https://developer.apple.com/forums/thread/758554) · [Support: Input Monitoring](https://support.apple.com/guide/mac-help/mchl4cedafb6/mac) · offiziell. `extern`: [Daniel's Journal (Silent Disable Race)](https://danielraffel.me/til/2026/02/19/cgevent-taps-and-code-signing-the-silent-disable-race/), [jano.dev](https://jano.dev/apple/macos/swift/2025/01/08/Accessibility-Permission.html).

---

## §7 Mikrofon

**Pflicht-Bausteine** (sonst Crash/stilles Scheitern): `NSMicrophoneUsageDescription` in Info.plist (fehlt der
Key → **harter App-Exit** `__TCC_CRASHING…` beim ersten Zugriff), Entitlement
`com.apple.security.device.audio-input` (Hardened Runtime), korrektes Re-Signing.

```swift
switch AVCaptureDevice.authorizationStatus(for: .audio) {           // versionsübergreifend robust
case .authorized: break
case .notDetermined:
    await MainActor.run { NSApp.activate(ignoringOtherApps: true) }  // Prompt nur mit aktiver UI
    _ = await AVCaptureDevice.requestAccess(for: .audio)
case .denied, .restricted:
    NSWorkspace.shared.open(URL(string:"x-apple.systempreferences:com.apple.preference.security?Privacy_Microphone")!)
default: break
}
// Tap IMMER mit Live-Hardware-Format (sonst Sample-Rate-Crash):
let hw = engine.inputNode.outputFormat(forBus: 0)
engine.inputNode.installTap(onBus: 0, bufferSize: 4096, format: hw) { buf, _ in /* … */ }
```

**Pflichten:** nie ein festes Format an `installTap` geben (immer `outputFormat(forBus:)` live), auf
`AVAudioEngineConfigurationChange` reagieren (Geräte-Wechsel AirPods/USB bricht den Tap → Engine/Tap neu
aufbauen, NIE im Handler deallokieren), bei `.denied` nicht erneut prompten sondern in die Settings führen.
STT: `SpeechAnalyzer`/`SpeechTranscriber` (macOS 26+) mit `SFSpeechRecognizer`-Fallback. **Audio-Bugfixes
erst in macOS 26.1** — als Mindestversion für Audio empfehlen.

Quellen: [NSMicrophoneUsageDescription](https://developer.apple.com/documentation/BundleResources/Information-Property-List/NSMicrophoneUsageDescription) · [audio-input Entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.security.device.audio-input) · [AVAudioEngineConfigurationChange](https://developer.apple.com/documentation/foundation/nsnotification/name-swift.struct/avaudioengineconfigurationchange) · offiziell. `extern`: [Rogue Amoeba (26.1 Audio-Fixes)](https://weblog.rogueamoeba.com/2025/11/04/macos-26-tahoe-includes-important-audio-related-bug-fixes/).

---

## §8 Login-Item (Autostart via SMAppService)

**`SMAppService.mainApp`** (macOS 13+) ist der richtige Weg für ein Menüleisten-Overlay — kein Helfer, kein
plist. Status **immer live aus `SMAppService` lesen** (nie in UserDefaults cachen — der Nutzer kann extern
abschalten); Toggle defaultet auf **`false`** (App-Review 2.4.5(iii): kein Auto-Launch ohne Zustimmung); **nie
blind im `didFinishLaunching` registrieren** (überschreibt die Nutzer-Wahl).

```swift
var isEnabled: Bool { SMAppService.mainApp.status == .enabled }   // live lesen
func setEnabled(_ on: Bool) throws {
    if on { if SMAppService.mainApp.status != .enabled { try SMAppService.mainApp.register() } }
    else  { try SMAppService.mainApp.unregister() }
    if SMAppService.mainApp.status == .requiresApproval { SMAppService.openSystemSettingsLoginItems() }
}
```

`.status` deckt `.enabled`/`.notRegistered`/`.requiresApproval`/`.notFound` ab — alle vier im UI behandeln,
`!= .enabled` als „aus". **Still starten** (Sequoia entfernte die „Hide"-Option): `LSUIElement` + kein Fenster
in `didFinishLaunching` (nur `NSStatusItem`/`MenuBarExtra`, Overlay erst auf Hotkey). Recovery bei kaputter
BTM-DB: `sudo sfltool resetbtm`. Bundle muss signiert sein.

Quellen: [SMAppService](https://developer.apple.com/documentation/servicemanagement/smappservice) · offiziell. `extern`: [theevilbit](https://theevilbit.github.io/posts/smappservice/), [nilcoalescing](https://nilcoalescing.com/blog/LaunchAtLoginSetting/), [Apple Forum 707482](https://developer.apple.com/forums/thread/707482).

---

## §9 Code-Signing & Notarisierung

Für ein außerhalb des App Store verteiltes Overlay: **Developer ID Application + Hardened Runtime, KEINE
Sandbox** (Sandbox würde Accessibility/Input-Monitoring abwürgen). Entitlements: nur
`com.apple.security.device.audio-input` (Mikro), `com.apple.security.automation.apple-events` **nur** falls
AppleEvents an fremde Apps. Accessibility/Input-Monitoring brauchen **keine** Entitlement (laufen über TCC).

```bash
xattr -cr Overlay.app                                  # geerbte xattrs weg (sonst Signatur ungültig)
# bottom-up signieren (Helfer/Frameworks ZUERST, Bundle ZULETZT), kein --deep:
codesign -f -s "$ID" -o runtime --timestamp Overlay.app/Contents/Frameworks/*.framework
codesign -f -s "$ID" -o runtime --timestamp --entitlements overlay.entitlements Overlay.app/Contents/MacOS/Overlay
codesign -f -s "$ID" -o runtime --timestamp --entitlements overlay.entitlements Overlay.app
ditto -c -k --keepParent Overlay.app Overlay.zip       # NUR ditto, nicht zip
xcrun notarytool submit Overlay.zip --keychain-profile p --wait
xcrun stapler staple Overlay.app
```

**Der wichtigste Punkt fürs Overlay:** Die **stabile Developer-ID-Identität ist gleichzeitig die Lösung fürs
TCC-Problem** — TCC bindet die Accessibility-/Mikro-Erlaubnis an das Designated Requirement der Signatur.
Ad-hoc (`-`) hat keinen stabilen DR → Permission nach jedem Rebuild weg. Über alle Releases mit derselben
Identität signieren. `--timestamp` ist Pflicht (sonst „signature invalid"), `--deep` NICHT zum Signieren.
Seit **macOS 15** ist der Ctrl-Click-Bypass weg → eine unsignierte/nicht-notarisierte App ist für Endnutzer
praktisch unbenutzbar.

Quellen: [Notarizing macOS software](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution) · [Hardened Runtime](https://developer.apple.com/documentation/security/hardened-runtime) · [Apple Forum 795739 (Quinn, ad-hoc TCC)](https://developer.apple.com/forums/thread/795739) · offiziell. `extern`: [rsms Distribution-Gist](https://gist.github.com/rsms/929c9c2fec231f0cf843a1a746a416f5), [lapcatsoftware (Hardened Runtime ≠ Sandbox)](https://lapcatsoftware.com/articles/hardened-runtime-sandboxing.html).

---

## 🔗 Bezug zum Bug-Almanach (Kopplung)

| Best-Practice-Abschnitt | Bug-Almanach-Abschnitt (`bugs/desktop/macos-overlay.md`) |
|-------------------------|-----------------------------------------------------------|
| §2 (NSPanel/nonactivating) | N1–N5 (StyleMask-Switch/Fokus-Klau/activate-deprecated/Sequoia-TextField/Tahoe-Menüleiste) |
| §3 (Multi-Space), §4 (Click-through/Transparenz) | S1–S8 (fullScreenAuxiliary/Level/Sonoma-Click-through/Tahoe-26.2-26.3-Regressionen/Mission-Control/Multi-Monitor) |
| §5 (Globale Hotkeys) | H1–H6 (Sequoia-shift/option/Tap-Timeout/Sleep-Wake/Secure-Input/Permission-Verwechslung/Memory) |
| §6 (TCC Accessibility/Input-Monitoring) | T1–T6 (Silent-Disable-Race/Re-Sign/Prompt-once/Xcode-Debug/tapCreate-nil/IOHIDManager) |
| §7 (Mikrofon) | M1–M7 (Usage-String-Crash/Sample-Rate/Geräte-Wechsel/Background-Prompt/Status-Cache/Entitlement) |
| §8 (Login-Item) | L1–L7 (Auto-Register/notFound/unregister-113/register-1/Agent-BundleProgram/Ventura-13.6/Login-Fenster) |
| §9 (Signing/Notarisierung) | C1–C11 (Permission-nach-Resign/Timestamp/Zertifikat/--deep/ditto/xattr/damaged/exportArchive/Keychain) |

> **Checkpoint:** Vollständig recherchiert (7 Researcher parallel, Apple Developer zuerst, Stand 2026-06-14,
> Swift 6 / Xcode 26, macOS 13–26 Tahoe). Kern für ClaudeCodexVoiceOverlay/TerminalVoiceOverlay: `NSPanel` mit
> `.nonactivatingPanel` + `orderFrontRegardless()` (nie `activate(ignoringOtherApps:)`), Accessory-App mit
> `.prohibited→.accessory`-Start, `collectionBehavior` für alle Spaces + Fullscreen, Carbon-Hotkey (⌘/⌃) oder
> CGEventTap (mit Re-Enable + Wake-Handling), Input Monitoring statt Accessibility wo möglich, Mikro mit
> Live-Hardware-Format, `SMAppService.mainApp`, und **stabile Developer-ID-Signatur** als Schlüssel gegen
> TCC-Permission-Verlust.

# Swift / AppKit — Best Practices (Stand 2026-06-02, Swift 6.3.2 / Xcode 26.5, macOS 13+)

> **Die "richtige Seite der Medaille" zum Bug-Almanach `~/proggs/bugs/desktop/swift-appkit.md`.**
> Dort steht *was schiefgeht und wie man es umgeht* — hier steht *wie man es von vornherein
> richtig macht, damit der Bug gar nicht erst entsteht*. Die wechselseitige Abschnitts-
> Bezugstabelle steht unten ("Kopplung zum Bug-Almanach").
>
> **Anker:** Swift **6.3.2** / Xcode **26.5** Command Line Tools (Mai/Juni 2026), reines
> **AppKit** (KEIN SwiftUI), Deployment-Target **macOS 13.0+**, **unsandboxed**, Build per
> `swiftc`/`build.sh` als `.app`-Bundle (KEIN Xcode-Projekt, KEIN SwiftPM), Direktverteilung,
> Apple Silicon (arm64). Genutzte Frameworks: AppKit, AVFoundation, CoreGraphics, **Carbon**
> (RegisterEventHotKey), Network.
>
> **Projekt-Anker:** `~/proggs/ClaudeCodexVoiceOverlay-macOS`, `~/proggs/TerminalVoiceOverlay-macOS`
> — schwebende Voice-Overlay-Panels (nonactivating, immer sichtbar, custom-gezeichnete Buttons,
> Mikrofon-Diktat → Whisper/Gemini, globale Carbon-Hotkeys, CGEvent-Tastatur-Injection,
> Accessory-App/LSUIElement).
>
> **Quellen-Regel:** Offizielle Apple-Quellen (developer.apple.com, WWDC, swift.org, HIG) =
> Grundwahrheit. Externe/Community klar als `extern` markiert — ueberstimmt nie das Offizielle.
> Jeder Eintrag traegt Quelle + Datum + `offiziell`/`extern`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Overlay-Panel bauen | `.nonactivatingPanel` nur im Init; `hidesOnDeactivate=false` | §A |
| 2 | Overlay anzeigen | `orderFrontRegardless()`, NIE `NSApp.activate(...)` | §A |
| 3 | Über allen Spaces/Fullscreen schweben | `[.canJoinAllSpaces,.fullScreenAuxiliary]`, `level=.floating` | §A |
| 4 | Borderless mit Textfeld-Fokus | `canBecomeKey` überschreiben (sonst nie key-fähig) | §A |
| 5 | Position über Monitore robust halten | Gegen `visibleFrame` prüfen, auf Hauptschirm zurückfallen | §A |
| 6 | Concurrency aufsetzen | `-default-isolation MainActor`, single-threaded first | §B |
| 7 | Off-Main-Callback (Audio-Tap) brücken | `AsyncStream`/`Task{@MainActor}`, NIE `assumeIsolated` | §B |
| 8 | On-Main C-Callback (Carbon-Hotkey) | `MainActor.assumeIsolated` hier korrekt | §B |
| 9 | Custom-gezeichneter Button | Label + Rolle + `accessibilityPerformPress()` setzen | §C |
| 10 | Status visuell ändern (Aufnahme) | `accessibilityValue` + `NSAccessibility.post` | §C |
| 11 | Permissions anfragen | Just-in-time, nie alle beim Launch, bei Denial nicht crashen | §D |
| 12 | Accessibility nach Erteilen | Trust-Status pollen (verzögertes Live-Update) | §D |
| 13 | App-Struktur | AppDelegate=Composition-Root, Services per Protokoll-DI | §E |
| 14 | State halten | EIN `@Observable` ViewModel, nicht in der View | §E |
| 15 | Audio-Tap | Format vom Node; `AVAudioEngineConfigurationChange` behandeln | §F |
| 16 | Aufnahme stoppen | `removeTap`+Continuation statt `Thread.sleep` | §F |
| 17 | Build/Signing | echtes Zertifikat, codesign zuletzt, KEIN `--deep`, `ditto` | §G |

---

## A — Overlay-Fenster (NSPanel/NSWindow) richtig konfigurieren

Zielbild: ein borderless, nonactivating `NSPanel`, das immer sichtbar ueber allen Apps und Spaces (inkl. Fullscreen) schwebt, beim Anzeigen die fremde App NICHT deaktiviert, aber bei Bedarf Tastatur-Fokus fuer ein Textfeld bekommen kann. Der aktuelle Code im Projekt liegt damit bereits sehr nah an der idiomatischen Rezeptur.

### 1. Die nonactivating-Floating-Panel-Rezeptur

- **DO** das `.nonactivatingPanel`-Flag direkt im `init`-`styleMask` setzen, NICHT nachtraeglich umschalten. Das nonactivating-Flag wird beim Erstellen in einen WindowServer-Window-Tag uebersetzt; aenderst du es spaeter, geraten AppKit-State und WindowServer-State auseinander und das Verhalten wird inkonsistent. (`extern`, philz.blog/nspanel-nonactivating-style-mask-flag, 2026-06-02 — deckt sich mit Bug-Almanach A2)
- **DO** die drei NSPanel-Properties bewusst kombinieren:
  - `isFloatingPanel = true` → Panel schwebt ueber Standardfenstern/anderen Panels. (`offiziell`, developer.apple.com/documentation/appkit/nspanel/1531901-isfloatingpanel, 2026-06-02)
  - `becomesKeyOnlyIfNeeded = true` → Panel wird NUR Key-Window, wenn der Nutzer in ein Control klickt, das Tastatureingaben braucht. (`offiziell`, developer.apple.com/documentation/appkit/nspanel/becomeskeyonlyifneeded, 2026-06-02)
  - `hidesOnDeactivate` → **Default-Falle**: bei `NSPanel` ist der Default `true` (anders als `NSWindow`). Fuer ein **dauerhaft sichtbares** Overlay MUSS man explizit `hidesOnDeactivate = false` setzen, sonst verschwindet das Panel, sobald die eigene Accessory-App den Fokus verliert. (`offiziell`, developer.apple.com/library/archive/documentation/Cocoa/Conceptual/WinPanel/Concepts/UsingPanels.html, 2026-06-02) — der aktuelle Code macht das korrekt.

```swift
class OverlayPanel: NSPanel {
    init() {
        super.init(
            contentRect: NSRect(x: 0, y: 0, width: 220, height: 64),
            styleMask: [.borderless, .nonactivatingPanel], // nonactivating NUR hier setzen
            backing: .buffered,
            defer: false
        )
        self.isFloatingPanel = true
        self.becomesKeyOnlyIfNeeded = true
        self.hidesOnDeactivate = false // PFLICHT fuer permanentes Overlay (NSPanel-Default ist true!)
    }
}
```

- **DON'T** fuer ein Always-on-Overlay `.transient`/`hidesOnDeactivate=true` setzen. Die oft zitierte „Overlay"-Kombination mit `.transient` ist fuer temporaere Inspector-Panels gedacht, NICHT fuer ein permanentes Voice-Overlay. (`offiziell`, developer.apple.com/documentation/appkit/nswindow/collectionbehavior-swift.struct, 2026-06-02)

### 2. Window-Level bewusst waehlen

- **DO** fuer „ueber allem, inkl. Fullscreen" einen hohen Level nutzen. `.floating` ist der Standard fuer Hilfs-Panels und reicht, solange `fullScreenAuxiliary` im collectionBehavior steht (siehe 3). Eskalationsstufen: `.statusBar` → `.screenSaver`. (`offiziell`, developer.apple.com/documentation/appkit/nswindow/level-swift.struct, 2026-06-02)
- **DON'T** reflexartig `.screenSaver` nehmen. Faustregel: den **niedrigsten** Level waehlen, der das Ziel erfuellt — fuer ein Voice-Panel ist `.floating` + `fullScreenAuxiliary` der idiomatische Startpunkt. (`extern`, abgeleitet aus Apple-Level-Doku + developer.apple.com/forums/thread/26677, 2026-06-02)

```swift
panel.level = .floating // niedrigster ausreichender Level; .statusBar/.screenSaver nur bei Bedarf
```

### 3. collectionBehavior idiomatisch kombinieren

| Flag | Wirkung |
|------|---------|
| `canJoinAllSpaces` | Panel erscheint gleichzeitig auf allen Spaces |
| `moveToActiveSpace` | Panel wandert beim Aktivieren auf den aktiven Space (Alternative — nicht mit canJoinAllSpaces mischen) |
| `stationary` | Panel folgt NICHT dem App-Wechsel, bleibt ortsfest (Mission Control/Expose) |
| `fullScreenAuxiliary` | Panel erscheint ueber Fullscreen-Apps, ohne selbst Fullscreen zu werden |
| `ignoresCycle` | Panel wird bei Cmd+` uebersprungen |

(`offiziell`, developer.apple.com/documentation/appkit/nswindow/collectionbehavior-swift.struct, 2026-06-02)

- **DO** fuer „auf allen Spaces + ueber Fullscreen": `[.canJoinAllSpaces, .fullScreenAuxiliary]`. Genau das nutzt der aktuelle Code — die empfohlene Minimalkombination.
- **DO optional** `ignoresCycle` ergaenzen, damit das Overlay nicht im Cmd+`-Fenster-Zyklus auftaucht.
- **DON'T** `canJoinAllSpaces` und `moveToActiveSpace` mischen (gegensaetzliche Space-Strategien).

```swift
panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .ignoresCycle]
```

### 4. canBecomeKey/canBecomeMain und das richtige Anzeige-API

- **DO** zum Anzeigen `orderFrontRegardless()` benutzen — zeigt das Panel, OHNE die eigene App zu aktivieren. (`extern`, fazm.ai/blog/swiftui-floating-panel, 2026-06-02)
- **DON'T** `NSApp.activate(ignoringOtherApps: true)` aufrufen, wenn das Overlay erscheint — das aktiviert die ganze App und reisst dem Nutzer den Fokus aus seiner aktuellen App (unterlaeuft den Sinn eines nonactivating-Panels; zudem ab macOS 14 deprecated, siehe Bug-Almanach A4).
- **DO `canBecomeKey` ueberschreiben, wenn das Panel borderless ist UND ein Textfeld Fokus braucht.** Ein borderless Fenster gibt per Default `canBecomeKey == false` zurueck; ohne Override kann ein Textfeld im Overlay nie getippt werden:

```swift
class OverlayPanel: NSPanel {
    override var canBecomeKey: Bool { true }      // borderless waere sonst nie key-faehig
    override var canBecomeMain: Bool { false }    // Overlay soll nie Haupt-Window werden
}
```

  (`extern`, soff.es/blog/cancel-borderless-window; `offiziell` developer.apple.com/documentation/appkit/nswindow/canbecomekey, 2026-06-02)
- **Hinweis:** Reine Tap-Overlays ohne Textfeld (wie das aktuelle Pillar-Panel) duerfen `canBecomeKey=false` lassen — der aktuelle Code macht das bewusst. `canBecomeKey=true` nur, wenn ein Textfeld Fokus braucht (PromptInput-Panel).
- **DON'T** `makeKeyAndOrderFront` im nonactivating-Kontext nutzen — kann unnoetig aktivierend wirken; bei Bedarf `orderFrontRegardless()` + gezielt `makeKey()`.

### 5. Multi-Display & Positionierung robust

- **DO** Positionen immer gegen `NSScreen.visibleFrame` (ohne Menubar/Dock) pruefen, nicht gegen `frame`. (`offiziell`, developer.apple.com/documentation/appkit/nsscreen/visibleframe, 2026-06-02)
- **DO** beim Wiederherstellen einer gespeicherten Position pruefen, ob das Rect noch auf einem aktuell angeschlossenen Screen liegt — sonst auf den Hauptbildschirm zuruckfallen. So ueberlebt die Position das Abstecken eines externen Monitors:

```swift
func restore(_ saved: NSRect) {
    let onScreen = NSScreen.screens.contains { $0.visibleFrame.intersects(saved) }
    let target = onScreen ? saved : (NSScreen.main?.visibleFrame ?? .zero)
    panel.setFrame(target, display: true)
}
```

- **DO erwaegen** statt manueller UserDefaults-Speicherung die eingebaute `saveFrame(usingName:)` / `setFrameUsingName(_:)`-Persistenz (behandelt Display-Wechsel automatisch). Fuer ein per-Rechtsklick verschiebbares Custom-Overlay ist die manuelle Variante legitim, solange die Screen-Pruefung oben gemacht wird. (`offiziell`, developer.apple.com/documentation/appkit/nswindow, 2026-06-02)

### 6. borderless vs. titled mit versteckter Titlebar

- **DO** fuer ein komplett custom gezeichnetes Overlay den `.borderless`-Weg (wie im aktuellen Code) — einfachster Weg fuer „kein System-Chrome".
- **DO als Alternative** (Standard-Window-Mechanik behalten, nur Titlebar verstecken): `.titled + .fullSizeContentView` mit `titleVisibility = .hidden` + `titlebarAppearsTransparent = true`:

```swift
panel.styleMask = [.titled, .fullSizeContentView, .nonactivatingPanel]
panel.titleVisibility = .hidden
panel.titlebarAppearsTransparent = true
```

  (`extern`, cocoadev.github.io/BorderlessWindow, 2026-06-02)
- **DON'T** den borderless-Weg waehlen und dann `canBecomeKey`-Override vergessen — borderless ist ohne Override nicht key-faehig (siehe 4). `.titled` kann ohne Override Key werden, `.borderless` nicht.

---

## B — Moderne Swift-Concurrency in AppKit

Apples Kern-Philosophie seit WWDC25: **"single-threaded first"** — erst alles auf dem Main-Actor laufen lassen, Nebenlaeufigkeit nur dort einfuehren, wo der Profiler einen echten Bottleneck zeigt. Beim `swiftc`-Build ohne Package.swift haengt der Language-Mode an den `-swift-version`-/`-enable-upcoming-feature`-Flags (der aktuelle `build.sh` setzt KEIN `-swift-version 6` → noch kein Mode-6).

### Grundhaltung (Do)

**Do: "Approachable Concurrency" aktivieren und den Main-Actor zum Default machen.** Statt ueberall `DispatchQueue.main.async {}` wird die ganze App per Compiler-Flag implizit `@MainActor`:

```bash
swiftc -swift-version 6 \
  -default-isolation MainActor \
  -enable-upcoming-feature NonisolatedNonsendingByDefault \
  ...weitere Quelldateien...
```

- `-default-isolation MainActor`: Alle Deklarationen ohne explizite Isolation sind automatisch Main-Actor-isoliert. UI-Code, `AppDelegate`, View-Controller, Model-Klassen brauchen kein `@MainActor`-Attribut und keine `DispatchQueue.main.async`-Huelle.
- `NonisolatedNonsendingByDefault`: `nonisolated async`-Funktionen laufen im Aufrufer-Kontext (kein erzwungenes Executor-Hopping) → viele Sendable-Fehler verschwinden.

(`offiziell`, [swift.org — Swift 6.2 released](https://www.swift.org/blog/swift-6.2-released/), 2025-09; [SE-0478](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0478-default-isolation-typealias.md))

> Zitat (offiziell, WWDC25): *"Your apps should start by running all of their code on the main thread ... it's fine to keep everything on the main thread!"* — [Embracing Swift concurrency, WWDC25 Session 268](https://developer.apple.com/videos/play/wwdc2025/268/), 2025-06.

**Do: async/await fuer Latenz, nicht fuer Parallelitaet.** Netzwerk/IO wird `async` — vermeidet UI-Hangs OHNE echte Nebenlaeufigkeit. Der Main-Actor gibt waehrend `await` frei.

```swift
func transcribe(url: URL) async throws {
    let (data, _) = try await URLSession.shared.data(from: url)  // gibt Main-Actor frei
    statusLabel.stringValue = decode(data)                       // schon wieder auf Main
}
```

**Do: `@concurrent` NUR fuer echte CPU-Arbeit, nachgewiesen per Instruments.** Erst wenn eine Funktion den Main-Thread blockiert (grosse Audio-/Bilddekodierung):

```swift
@concurrent
func resample(_ buffer: [Float]) async -> [Float] { /* laeuft off-main */ }
```

**Do: `Task.sleep` statt `asyncAfter` fuer Retries** (strukturiert, abbrechbar):

```swift
// vorher: DispatchQueue.main.asyncAfter(deadline: .now() + 2) { retry() }
Task { @MainActor in
    try await Task.sleep(for: .seconds(2))
    retry()
}
```

### Actors & Thread-Safety

**Do: `NSLock` durch einen `actor` ersetzen — aber nur fuer ein unabhaengiges Subsystem.** Der `AudioRecorder` mit manuellem `NSLock` um geteilten Mutable-State ist der Lehrbuch-Fall:

```swift
actor AudioRecorder {
    private var buffers: [AVAudioPCMBuffer] = []
    func append(_ b: AVAudioPCMBuffer) { buffers.append(b) }  // serialisiert
}
```

(`offiziell`, WWDC25-268; [Protect mutable state with Swift actors, WWDC21](https://developer.apple.com/videos/play/wwdc2021/10133/))

**Don't: UI- und Model-Klassen NICHT zu Actors machen.** UI-naher Code bleibt auf dem Main-Actor; Model-Klassen bleiben Main-Actor-isoliert oder werden `nonisolated`, aber **nicht** `Sendable`. (`offiziell`, WWDC25-268)

**Don't: Actors ueber-adoptieren.** Jeder Actor = neue Isolations-Domaene = mehr `await`, mehr Sendable-Zwang. Fuer wiederverwendbare Helfer (Decoder) lieber `nonisolated` als eigenen Actor — der Aufrufer entscheidet ueber den Kontext. (`offiziell`, WWDC25-268: *"For libraries, it's best to provide a nonisolated API and let clients decide."*)

### Bruecken: C-/Carbon-Callbacks und AVAudioEngine-Tap

**Do: On-Main Carbon-Hotkey-Callback mit `MainActor.assumeIsolated` bruecken — synchron, ohne async-Hop.** Der Carbon-`EventHandler` ruft garantiert auf dem Main-RunLoop zurueck; `assumeIsolated` betritt den Main-Actor synchron und crasht nur, wenn man sich irrt (Fail-Fast):

```swift
let cb: EventHandlerProcPtr = { _, _, _ in
    MainActor.assumeIsolated { AppController.shared.toggleRecording() }  // sync, kein Hop
    return noErr
}
```

(`extern`, [fatbobman — MainActor.assumeIsolated](https://fatbobman.com/en/posts/mainactor-assumeisolated/), 2025; [Michael Tsai](https://mjtsai.com/blog/2025/11/07/mainactor-assumeisolated-preconcurrency-and-isolated-conformances/), 2025-11)

**Do: Off-Main-Callback (AVAudioEngine-Tap laeuft auf internem Audio-Thread) mit `AsyncStream` modellieren** — `assumeIsolated` waere hier FALSCH (Crash, weil nicht auf Main). Die nicht-isolierte Tap-Closure schiebt nur in die Continuation, der Consumer iteriert auf dem Main-Actor:

```swift
let (stream, continuation) = AsyncStream<AVAudioPCMBuffer>.makeStream()

node.installTap(onBus: 0, bufferSize: 4096, format: nil) { buffer, _ in
    continuation.yield(buffer)        // off-main, nur yield (Sendable-sicher)
}

Task { @MainActor in
    for await buffer in stream {       // verarbeitet auf Main-Actor
        await recorder.append(buffer)
        updateWaveform(buffer)
    }
}
```

Wichtig: `AsyncStream.makeStream()` verwenden, nicht den Closure-Init (der erzeugt Isolation-Probleme bei der Continuation). (`extern`, Pattern deckt sich mit WWDC25-Empfehlung)

**Do: Einmalige Callbacks per `withCheckedContinuation` in `async` umwandeln** (z.B. Permission-Prompts):

```swift
func requestMicAccess() async -> Bool {
    await withCheckedContinuation { c in
        AVCaptureDevice.requestAccess(for: .audio) { ok in c.resume(returning: ok) }
    }
}
```

### Netzwerk-Clients Groq/Gemini

**Do: `URLSession`-async/await statt Completion-Handler.** Der gesamte Client wird ein paar `async`-Funktionen; Fehler ueber `throws`, kein manuelles Zurueck-auf-Main:

```swift
struct GroqClient {
    func complete(_ prompt: String) async throws -> String {
        var req = URLRequest(url: endpoint); req.httpMethod = "POST"; req.httpBody = body(prompt)
        let (data, resp) = try await URLSession.shared.data(for: req)
        guard (resp as? HTTPURLResponse)?.statusCode == 200 else { throw ClientError.http }
        return try parse(data)
    }
}
```

### Don'ts (Zusammenfassung)

- **Don't:** `DispatchQueue.main.async {}` als Reflex behalten, wenn `-default-isolation MainActor` aktiv ist — der Code IST schon auf Main; die Huelle ist toter Boilerplate und verzoegert UI-Updates um einen RunLoop-Tick.
- **Don't:** `@preconcurrency import AppKit` dauerhaft einsetzen — nur als temporaere Bruecke fuer einzelne noch nicht auditierte APIs. (`offiziell`, [Adopting strict concurrency, Apple](https://developer.apple.com/documentation/swift/adoptingswift6))
- **Don't:** Mutable Model-Klassen `Sendable` machen, um Compiler-Fehler „wegzudruecken" — Data-Race-Falle.
- **Don't:** GCD blind komplett verbannen. Bewusst behalten ist OK fuer reines C-Interop/Timer oder wenn eine Drittbibliothek eine `DispatchQueue` erwartet. Nur den UI-Marshalling-Stil ersetzen.

**Quellen:** [WWDC25-268](https://developer.apple.com/videos/play/wwdc2025/268/) (offiziell, 2025-06) · [Swift 6.2 Released](https://www.swift.org/blog/swift-6.2-released/) (offiziell, 2025-09) · [SE-0478](https://github.com/swiftlang/swift-evolution/blob/main/proposals/0478-default-isolation-typealias.md) (offiziell) · [Adopting strict concurrency](https://developer.apple.com/documentation/swift/adoptingswift6) (offiziell) · [WWDC21-10133](https://developer.apple.com/videos/play/wwdc2021/10133/) (offiziell) · [fatbobman](https://fatbobman.com/en/posts/mainactor-assumeisolated/) (extern, 2025) · [Michael Tsai](https://mjtsai.com/blog/2025/11/07/mainactor-assumeisolated-preconcurrency-and-isolated-conformances/) (extern, 2025-11)

---

## C — Accessibility (VoiceOver) fuer custom AppKit-UI

**Kernproblem:** Eine selbst gezeichnete `NSView` ist fuer VoiceOver standardmaessig UNSICHTBAR. AppKit leitet Accessibility-Infos nur fuer Standard-Controls (`NSButton`, `NSControl`) automatisch ab. Ein `draw(_:)`-Kreis mit Icon (wie die `RoundButton`-Klasse) hat keine Semantik — VoiceOver kann ihn nicht ansagen, nicht fokussieren, nicht ausloesen. Apple woertlich: *"If you implement a control using custom drawing code and forget to supply the accessibility label, VoiceOver will have no idea how to describe it"* (`offiziell`). Aktuell ist in beiden Projekten NICHTS fuer VoiceOver annotiert.

### Do's

**1. Custom-Button per `NSAccessibilityButton`-Protokoll zugaenglich machen (empfohlener Weg 2026).** Setzt `isAccessibilityElement` automatisch auf `true` und impliziert Rolle `.button` — man liefert nur Label + Press-Handler:

```swift
final class RoundButton: NSView, NSAccessibilityButton {
    var onTap: (() -> Void)?
    override func accessibilityLabel() -> String? { "Aufnahme" }      // OHNE Rolle ("Aufnahme", nicht "Aufnahme-Button")
    override func accessibilityPerformPress() -> Bool { onTap?(); return true }  // VO-Space/Doppeltipp
}
```

Apple: *"If your NSView subclass adopts one of the role-specific accessibility protocols, the system automatically changes the accessibilityElement property's value to true."*

**2. Alternative ohne Protokoll: Rolle + Element-Flag explizit setzen** (beide MUESSEN gesetzt sein, sonst unsichtbar):

```swift
override var isAccessibilityElement: Bool { true }
override func accessibilityRole() -> NSAccessibility.Role? { .button }
override func accessibilityLabel() -> String? { "Kopieren" }
override func accessibilityPerformPress() -> Bool { onTap?(); return true }
```

**3. Setter-Variante (gut fuer statische Annotation im `init`):**

```swift
init() {
    super.init(frame: .zero)
    setAccessibilityElement(true)
    setAccessibilityRole(.button)
    setAccessibilityLabel("Einfuegen")
}
```

Faustregel (alle drei offiziell + gleichwertig): **Setter** fuer statisch, **Override** fuer dynamische Zustaende, **Protokoll** als semantisch saubersten Weg.

**4. Status/Wert kommunizieren (Mic „Aufnahme laeuft").** `accessibilityValue` UND bei Aenderung Notification posten (sonst bleibt der Zustandswechsel still):

```swift
override func accessibilityValue() -> Any? { isRecording ? "laeuft" : "gestoppt" }
func setRecording(_ on: Bool) {
    isRecording = on
    NSAccessibility.post(element: self, notification: .valueChanged)
}
```

Fuer freie Ansagen (z.B. „Profil 3 ausgewaehlt"): `.announcement` am Fenster mit Prioritaet (`NSAccessibilityPriorityLevel.high`).

**5. Tooltips als `accessibilityHelp` spiegeln** — der eigene Tooltip-Panel-Text ist rein visuell, VoiceOver sieht ihn nicht:

```swift
override func accessibilityHelp() -> String? { "Startet oder stoppt die Sprachaufnahme" }
```

**6. Profile-Tiles 1–10 als einzelne fokussierbare Elemente** mit eindeutigem Label ("Profil 1" … "Profil 10"), bei Auswahl `accessibilityValue("ausgewaehlt")` + `.selectedChildrenChanged`/`.valueChanged`. Optional in einer Gruppen-View mit `accessibilityRole(.group)` + Label "Profile" buendeln.

**7. Panel-Erreichbarkeit pruefen (.accessory-App + nonactivating).** VoiceOver erreicht Elemente unabhaengig vom Key-Status; Empfehlung: dem Panel `accessibilityLabel` (Fenstertitel-Ersatz) geben und mit echtem VoiceOver gegentesten, ob das Overlay im VO-Rotor auftaucht.

**8. Testen — zweistufig (Pflicht vor „fertig"):**
- **Accessibility Inspector** (Xcode → Open Developer Tool): "Run Audit" listet fehlende Labels/falsche Rollen/Kontrast automatisch.
- **Echtes VoiceOver** (Cmd+F5): durch alle Buttons navigieren — wird jedes Element angesagt, loest VO-Space die Aktion aus, wird der Status-Wechsel hoerbar.
- Optional: `performAccessibilityAudit()` im UI-Test (laeuft bei jedem Build mit).

### Don'ts

- **Icon-only Button ohne `accessibilityLabel`** — haeufigster Fehler; fuer VoiceOver existiert der Button nicht.
- **Rolle in den Label packen** ("Aufnahme-Button") — die Rollenbeschreibung kommt automatisch dazu, sonst sagt VO "Aufnahme-Button Button". Label = nur der Name.
- **`accessibilityPerformPress` weglassen** — VoiceOver loest NICHT ueber `mouseDown` aus.
- **Status nur visuell** (roter Kreis = Aufnahme) ohne `accessibilityValue` + Notification.
- **`true` aus `accessibilityPerformPress` zurueckgeben, obwohl die Aktion fehlschlug** — bei Misserfolg `false`.
- **Eigenen Tooltip-Text als einzige Hilfe** — immer als `accessibilityHelp` spiegeln.

**Quellen:** [Enhancing Accessibility of Standard AppKit Controls](https://developer.apple.com/library/archive/documentation/Accessibility/Conceptual/AccessibilityMacOSX/EnhancingtheAccessibilityofStandardAppKitControls.html) (offiziell, Archiv) · [NSAccessibilityProtocol](https://developer.apple.com/documentation/appkit/nsaccessibilityprotocol) (offiziell) · [accessibilityPerformPress()](https://developer.apple.com/documentation/appkit/nsaccessibilitybutton/1525542-accessibilityperformpress) (offiziell) · [Integrating accessibility into your app](https://developer.apple.com/documentation/accessibility/integrating_accessibility_into_your_app) (offiziell) · [Performing accessibility audits](https://developer.apple.com/documentation/accessibility/performing-accessibility-audits-for-your-app) (offiziell) · [WWDC23-10035](https://developer.apple.com/videos/play/wwdc2023/10035/) (offiziell)

---

## D — Permission-Handling (TCC) richtig & nutzerfreundlich

> Globale Hotkeys laufen ueber Carbon (`RegisterEventHotKey`) und brauchen **keine** TCC-Permission — das ist die richtige Wahl und sollte so bleiben.

### Grundprinzip: Just-in-time statt at-launch

Apple HIG (Privacy): Permissions **nur dann** anfragen, wenn der Nutzer eine Funktion startet, die sie braucht — nicht alle beim App-Start. Vor dem System-Dialog die eigene UI nutzen, um den **Grund** zu erklaeren (Pre-Permission-Screen). Der System-Prompt erscheint pro Permission nur **einmal** (danach `.denied`, nur ueber Settings aenderbar) — nie ungefragt im falschen Moment „verbrennen".

**Empfohlene Reihenfolge** (nach Bedarf, nicht gebuendelt): 1. **Accessibility** beim ersten Text-Injection-Versuch, 2. **Mikrofon** beim ersten Aufnahme-Start, 3. **Input Monitoring** nur falls je ein `CGEventTap` (Listen) eingefuehrt wird (aktuell nicht noetig).

- **Do:** Jede Permission an die konkrete Nutzer-Aktion koppeln.
- **Don't:** Alle drei Dialoge beim Launch nacheinander feuern (HIG-Verstoss, verbrennt Prompts).

### 1. Mikrofon (AVAudioEngine)

Vier Stati pruefen, nur bei `.notDetermined` anfragen, bei `.denied`/`.restricted` freundlich zu den Settings — **nie crashen**:

```swift
func ensureMicAccess(_ completion: @escaping (Bool) -> Void) {
    switch AVCaptureDevice.authorizationStatus(for: .audio) {
    case .authorized: completion(true)
    case .notDetermined:
        AVCaptureDevice.requestAccess(for: .audio) { granted in
            DispatchQueue.main.async { completion(granted) }   // Main-Thread vor UI
        }
    case .denied, .restricted: openSettings(.microphone); completion(false)
    @unknown default: completion(false)
    }
}
```

**Pflicht in Info.plist** — `NSMicrophoneUsageDescription` konkret + ehrlich (sonst Crash beim Request):

```xml
<key>NSMicrophoneUsageDescription</key>
<string>Ermoeglicht die Sprachaufnahme fuer das Voice-Overlay, damit dein Diktat in Text umgewandelt werden kann.</string>
```

- **Don't:** Den String generisch halten ("App braucht Mikrofon") — Nutzer misstrauen vagen Strings.

### 2. Accessibility (CGEvent-Tastatur-Simulation)

`AXIsProcessTrusted()` zum Pruefen, `AXIsProcessTrustedWithOptions` mit `kAXTrustedCheckOptionPrompt: true` zum einmaligen Anstossen. Es gibt **keinen** Weg um den manuellen Nutzer-Klick herum.

```swift
func isAccessibilityTrusted(prompt: Bool) -> Bool {
    let opts = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: prompt]
    return AXIsProcessTrustedWithOptions(opts as CFDictionary)
}
```

**Re-Check-/Polling-Pattern (kritisch):** Nach dem Erteilen aktualisiert macOS den Trust-Status fuer einen **laufenden** Prozess oft erst verzoegert oder erst nach Neustart. Deshalb pollen statt blockieren:

```swift
func waitForAccessibility(onGranted: @escaping () -> Void) {
    guard !AXIsProcessTrusted() else { onGranted(); return }
    _ = isAccessibilityTrusted(prompt: true)   // einmal anstossen
    openSettings(.accessibility)               // direkt ins richtige Pane
    axTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { t in
        if AXIsProcessTrusted() { t.invalidate(); DispatchQueue.main.async { onGranted() } }
    }
}
```

- **Do:** Bei Verweigerung im Basismodus weiterlaufen + klaren "Accessibility aktivieren"-Button anbieten.
- **Don't:** Bei `false` `fatalError()`/`exit()` oder Feature hart entfernen — absichern, nicht streichen.

### 3. Input Monitoring (nur falls je CGEventTap im Listen-Modus)

Aktuell nicht noetig (Carbon-Hotkeys reichen). Falls doch ein **Listen-only** `CGEventTap`:

```swift
if !CGPreflightListenEventAccess() {      // Status pruefen (kein Dialog)
    _ = CGRequestListenEventAccess()      // einmalig anfragen -> Settings, dann pollen
}
```

- **Wichtig:** Reines Event-**Posten** (Tastatur-Simulation senden, wie `TerminalController`/`InputController` es tun) braucht **nur Accessibility, KEIN Input Monitoring**. Input Monitoring (`Privacy_ListenEvent`) ist nur fuer Listen-Taps noetig — nicht verwechseln, sonst nervt man Nutzer mit einem ueberfluessigen Permission-Schritt.

### 4. Nutzer ins richtige Settings-Pane fuehren

```swift
enum PrivacyPane { case accessibility, microphone, inputMonitoring
    var anchor: String { switch self {
        case .accessibility:   return "Privacy_Accessibility"
        case .microphone:      return "Privacy_Microphone"
        case .inputMonitoring: return "Privacy_ListenEvent" } } }

func openSettings(_ pane: PrivacyPane) {
    let s = "x-apple.systempreferences:com.apple.preference.security?\(pane.anchor)"
    if let url = URL(string: s) { NSWorkspace.shared.open(url) }
}
```

- **Do:** Statt nur Text einen **Button** anbieten, der direkt das passende Pane oeffnet.

### 5. Sequoia 15: defensiv mit Re-Authorisierung umgehen

Die periodischen Re-Prompts (final ~monatlich + bei Reboot) betreffen **primaer Screen Recording/Capture** — Mikrofon und Accessibility laeuten **nicht** im gleichen 30-Tage-Rhythmus. Diese App nutzt kein Screen Recording, ist also vom schlimmsten Aerger nicht betroffen. Trotzdem defensiv bauen (ein Status kann jederzeit wegfallen: Update, Nutzer-Aenderung, Rebuild mit anderer Identitaet):

- Beim App-Start und bei jedem `didBecomeActiveNotification` den Status aller benoetigten Permissions neu pruefen.
- Bei Wegfall: nicht still scheitern, sondern freundliche, nicht-modale Hinweis-UI + Direkt-Button.
- Stabile Code-Identitaet (Developer-ID-Signatur) reduziert ungewollte Re-Authorisierung (→ G).

- **Do:** Status als veraenderlich behandeln, periodisch/event-getrieben re-checken.
- **Don't:** Den Status einmal beim Launch cachen und fuer den Rest der Laufzeit als gegeben annehmen.

**Quellen:** [Requesting Authorization to Capture Media](https://developer.apple.com/documentation/avfoundation/requesting-authorization-to-capture-and-save-media) (offiziell, 2026-06-02) · [AXIsProcessTrustedWithOptions](https://developer.apple.com/documentation/applicationservices/1459186-axisprocesstrustedwithoptions) (offiziell) · [CGRequestListenEventAccess](https://developer.apple.com/documentation/coregraphics/cgrequestlisteneventaccess()) (offiziell) · [HIG: Privacy](https://developer.apple.com/design/human-interface-guidelines/privacy) (offiziell) · [jano.dev — Accessibility Permission](https://jano.dev/apple/macos/swift/2025/01/08/Accessibility-Permission.html) (extern, 2025-01) · [System Preferences URL Schemes](https://gist.github.com/rmcdongit/f66ff91e0dad78d4d6346a75ded4b751) (extern)

---

## E — App-Architektur (MVVM & saubere Struktur) in AppKit

Reines AppKit (kein SwiftUI, kein Storyboard, `@main`/manuelles `NSApplication`-Setup). Ziel: eine Menubar/Overlay-Utility-App so strukturieren, dass `AppDelegate` NICHT mehr alles orchestriert (aktuell ein „Massive-AppDelegate", der Hotkeys, Audio, Netzwerk, Overlay-State, Profile haelt).

### E.1 — MVVM in AppKit: Wie das ViewModel an die View bindet

**Do — moderner Weg (`@Observable`, automatisches Tracking, aber erst ab macOS 15):** Seit macOS 15 trackt AppKit `@Observable`-Properties automatisch, wenn man sie in den Layout-/Draw-Methoden liest.

```swift
import Observation

@Observable final class OverlayViewModel { var isRecording = false; var statusText = "" }

final class OverlayViewController: NSViewController {
    let viewModel: OverlayViewModel
    override func viewWillLayout() {
        super.viewWillLayout()
        statusLabel.stringValue = viewModel.statusText                 // Zugriff hier wird ab macOS 15 getrackt
        view.layer?.backgroundColor = viewModel.isRecording ? .red : .clear
    }
}
```

Teilnehmende Methoden: `NSView.layout()/draw(_:)/updateLayer()/updateConstraints()`, `NSViewController.viewWillLayout()/viewDidLayout()/updateViewConstraints()`. Aktivierung auf macOS 15 per `Info.plist`-Key `NSObservationTrackingEnabled = YES`; ab macOS 26 Default (Key ignoriert). (`offiziell` Observation-Doku; `extern` steipete.me 2025)

> **WICHTIG fuer dieses Projekt:** Auto-Tracking gibt es **erst ab macOS 15**. Bei Deployment-Target **macOS 13.0** steht es NICHT zur Verfuegung — entweder Target anheben oder Fallback nutzen.

**Do — Fallback A (`withObservationTracking`, auch unter macOS 13):** feuert genau einmal beim *naechsten* Schreibzugriff (will-set) und muss danach erneut aufgesetzt werden:

```swift
private func observe() {
    withObservationTracking { _ = viewModel.statusText }
    onChange: { [weak self] in
        DispatchQueue.main.async { self?.render(); self?.observe() }   // onChange feuert VOR der Aenderung → auf Main + re-arm
    }
}
```

**Do — Fallback B (`Observations`-AsyncSequence, ab Swift 6.2 / macOS 26):** transaktionale did-set-Semantik; im `Task` mit `weak self`/`weak model` halten (sonst Retain Cycle).

**Do — klassisch (Cocoa Bindings / KVO):** auf macOS verfuegbar, echter Vorteil ggue. iOS, aber viel Boilerplate; nur fuer einfache Faelle/bestehenden Nib-Code. (`extern` objc.io)

### E.2 — AppDelegate als Composition-Root statt Gott-Objekt

```swift
protocol HasAudioRecorder { var audioRecorder: AudioRecording { get } }
protocol HasTranscriber   { var transcriber: Transcribing { get } }
protocol HasHotkeys       { var hotkeys: HotkeyRegistering { get } }

struct AppDependencies: HasAudioRecorder, HasTranscriber, HasHotkeys {
    let audioRecorder: AudioRecording; let transcriber: Transcribing; let hotkeys: HotkeyRegistering
}

final class AppCoordinator {
    typealias Dependencies = HasAudioRecorder & HasTranscriber & HasHotkeys
    private let deps: Dependencies
    init(dependencies: Dependencies) { self.deps = dependencies }
    func start() { /* Hotkeys registrieren, Menubar-Item bauen, Overlay vorbereiten */ }
}

@main final class AppDelegate: NSObject, NSApplicationDelegate {
    private var coordinator: AppCoordinator?
    func applicationDidFinishLaunching(_ note: Notification) {
        let deps = AppDependencies(audioRecorder: AudioRecorder(),
                                   transcriber: GroqWhisperClient(),
                                   hotkeys: HotkeyRegistry())          // injiziert, NICHT .shared
        coordinator = AppCoordinator(dependencies: deps); coordinator?.start()
    }
}
```

### E.3 — State-Management (Single Source of Truth)

- **Do:** State (Mic-State, aktives Profil, Toggles) lebt in **einem** `@Observable` AppState/ViewModel — die View liest nur, haelt keinen eigenen kopierten Zustand. Profile als Wertobjekte (`struct`).
- **Don't:** `isRecording`, Timer-Stand oder Profil-Auswahl als Stored Properties IN der `NSPanel`/`NSView` halten — zwei Stellen, die denselben Zustand spiegeln, driften auseinander. (Genau das macht das aktuelle `OverlayPanel`.)

### E.4 — Testbarkeit: Services hinter Protokollen, Singletons sind ein Smell

- **Do:** Jeder Service hinter ein Protokoll (`AudioRecording`, `Transcribing`, `HotkeyRegistering`); im Test ein Mock injizieren.
- **Don't:** `HotkeyRegistry.shared`. Singletons = global veraenderlicher Zustand, koppeln hart, im Test nicht ersetzbar. Stattdessen die EINE Instanz im Composition-Root erzeugen und durchreichen.

### E.5 — `NSViewController` vs. nackte `NSView`/`NSPanel`

- **Do — Controller einziehen, sobald** die View einen Lifecycle (`viewWillAppear`/`viewDidLayout`), eigenen State oder mehrere Actions buendelt. Das aktuelle `OverlayPanel` mit eingebettetem State + Timern gehoert hinter einen `OverlayPanelController`, der das ViewModel beobachtet und die Timer besitzt. (`offiziell` NSViewController-Doku)
- **OK ohne Controller:** rein dekorative, zustandslose Custom-Views.

### E.6 — Don'ts (Zusammenfassung)

- **Massive-AppDelegate** — nur Composition-Root + `start()`.
- **Massive-View / State in der View** — State ins ViewModel.
- **Singleton-Kopplung** (`.shared`) — per DI durchreichen.
- **Auto-`@Observable`-Tracking unter macOS 13 erwarten** — gibt es erst ab macOS 15.

**Quellen:** [Observation](https://developer.apple.com/documentation/Observation) (offiziell) · [NSViewController](https://developer.apple.com/documentation/appkit/nsviewcontroller) (offiziell) · [steipete.me — Automatic Observation Tracking in UIKit/AppKit](https://steipete.me/posts/2025/automatic-observation-tracking-uikit-appkit) (extern, 2025) · [mjtsai — Swift 6.2 Observations](https://mjtsai.com/blog/2025/10/31/swift-6-2-observations/) (extern, 2025-10) · [Swift with Majid — DI with Protocols](https://swiftwithmajid.com/2019/03/06/dependency-injection-in-swift-with-protocols/) (extern) · [objc.io — MVVM](https://www.objc.io/issues/13-architecture/mvvm/) (extern)

---

## F — AVFoundation-Audioaufnahme richtig machen

`AVAudioEngine` bleibt 2026 die richtige Wahl fuer Voice-to-Text — `AVCaptureSession` ist fuer Video/Spatial-Audio gedacht, `AVAudioRecorder` schreibt nur fertige Dateien ohne Live-Buffer-Zugriff (kein Waveform-Peak, keine eigene Konvertierung). `AVAudioSession`-APIs sind iOS-only — auf macOS nicht verfuegbar.

### Do's

**1. Tap-Format vom Node nehmen, nicht erzwingen.** Ein abweichendes Format an `installTap` wird ignoriert oder crasht. `nil` uebergeben oder `inputNode.outputFormat(forBus: 0)` lesen (bereits korrekt im Code):

```swift
let format = inputNode.outputFormat(forBus: 0)   // niemals ein Wunsch-Format
inputNode.installTap(onBus: 0, bufferSize: 4096, format: format) { buffer, time in
    // laeuft auf dem Audio-Thread — leichtgewichtig halten
}
```

**2. Buffer-Size moderat (4096), Tap-Closure leichtgewichtig** — nur puffern/konvertieren + Peak. Keine Datei-IO, kein Logging, keine UI im Callback.

**3. Sample-Rate per `AVAudioConverter` konvertieren (16 kHz mono).** Pflicht ist die Variante `convert(to:error:withInputFrom:)` mit Input-Block — die einfache `convert(to:from:)`-Methode kann KEINE Sample-Rate aendern. Dieselbe Converter-Instanz ueber die ganze Aufnahme weiterverwenden (haelt Resampler-Zustand, vermeidet Knackser):

```swift
let outFormat = AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: 16_000, channels: 1, interleaved: true)!
let converter = AVAudioConverter(from: inputFormat, to: outFormat)!
var provided = false; var error: NSError?
converter.convert(to: outBuffer, error: &error) { _, status in
    guard !provided else { status.pointee = .noDataNow; return nil }
    provided = true; status.pointee = .haveData; return inBuffer
}
```

**4. Geraetewechsel behandeln (`AVAudioEngineConfigurationChange`) — PFLICHT.** Beim Wechsel auf AirPods/USB/Bluetooth stoppt CoreAudio die Engine; der Tap feuert sonst gar nicht mehr. **Das fehlt aktuell im Code und ist der wichtigste Mangel** (= Bug-Almanach E5):

```swift
NotificationCenter.default.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: nil) { [weak self] _ in
    guard let self, self.isRecording else { return }
    self.engine.inputNode.removeTap(onBus: 0)
    let newFormat = self.engine.inputNode.outputFormat(forBus: 0)   // Format/Converter NEU lesen!
    self.converter = AVAudioConverter(from: newFormat, to: self.outFormat)
    self.installTap(format: newFormat)
    try? self.engine.start()
}
```

**5. `Thread.sleep` durch async/await + Continuation ersetzen.** Statt blind 50 ms zu schlafen (wie aktuell in `stop()`): Tap entfernen (danach kommt kein neuer Callback), letzten Stand finalisieren, ueber Continuation zurueckgeben:

```swift
func stop() async -> URL {
    engine.inputNode.removeTap(onBus: 0)   // nach removeTap kommen keine neuen Calls
    engine.stop()
    return await withCheckedContinuation { cont in
        finalizationQueue.async { [self] in cont.resume(returning: self.writeWavFile()) }
    }
}
```

Tap-Closure laeuft off-main/`@Sendable` — geteilten Zustand ueber Serial-Dispatch-Queue oder Actor schuetzen, nicht ueber ad-hoc `NSLock`.

**6. UI/Waveform nur ueber Main-Marshalling** — Peak im Tap berechnen, Update an die View IMMER per `await MainActor.run {}` / `DispatchQueue.main.async`.

**7. Mikrofon-Berechtigung modern abfragen** — `AVAudioApplication.shared.recordPermission` / `requestRecordPermission(completionHandler:)` (loest die alten `AVAudioSession`-Varianten ab). `NSMicrophoneUsageDescription` Pflicht (sonst TCC-Crash, auch unsandboxed).

### Don'ts

- **Kein Wunsch-Format an `installTap`** — Konvertierung gehoert hinter den Tap (`AVAudioConverter`).
- **Engine NICHT ohne `AVAudioEngineConfigurationChange`-Handling** — haeufigste Ursache fuer „Aufnahme tot nach AirPods-Wechsel".
- **Tap-Callback nicht ueberladen** (keine Datei-IO/Logging im Callback).
- **Kein `Thread.sleep` als Synchronisations-Ersatz** — Race-anfaellig; `removeTap` + Continuation ist deterministisch.
- **AppKit nie direkt aus dem Tap-Thread** — immer auf Main marshallen.

**Quellen:** [TN3136: AVAudioConverter Sample-Rate-Conversion](https://developer.apple.com/documentation/technotes/tn3136-avaudioconverter-performing-sample-rate-conversions) (offiziell, 2026-06-02) · [installTap(onBus:bufferSize:format:block:)](https://developer.apple.com/documentation/avfaudio/avaudionode/installtap(onbus:buffersize:format:block:)) (offiziell) · [AVAudioEngineConfigurationChange](https://developer.apple.com/documentation/avfaudio/avaudioengineconfigurationchangenotification) (offiziell) · [AVAudioApplication](https://developer.apple.com/documentation/avfaudio/avaudioapplication) (offiziell) · [WWDC25-251 Enhance audio recording](https://developer.apple.com/videos/play/wwdc2025/251/) (offiziell) · [snakamura — Tips about AVAudioEngine](https://snakamura.github.io/log/2024/11/audio_engine.html) (extern, 2024-11)

---

## G — Build, Code-Signing & Distribution (swiftc-CLI-.app) richtig

> Leitsatz: **Die Code-Identitaet ist heilig.** TCC-Grants (Mikrofon, Accessibility) haengen an `client` (= stabile `CFBundleIdentifier`) **und** `csreq` (= stabile Signatur-Identitaet). Aendert sich eine davon, sind alle Grants weg.

### 1. .app-Bundle-Struktur aus der CLI

```
MyApp.app/Contents/
├── Info.plist          ← Pflicht, separate Datei (NICHT ins Binary einbetten bei einer GUI-.app)
├── MacOS/MyApp         ← Mach-O Executable (= CFBundleExecutable)
├── Resources/          ← Assets, .icns
└── _CodeSignature/     ← von codesign erzeugt
```

- **Do:** Bei einer GUI-.app die `Info.plist` als **echte Datei** unter `Contents/Info.plist`. Das `-sectcreate __TEXT __info_plist`-Einbetten ist nur fuer single-file CLI-Tools ohne Bundle gedacht.

**Pflicht-Keys:** `CFBundleIdentifier` (STABIL!), `CFBundleExecutable`, `CFBundleName`, `CFBundlePackageType=APPL`, `LSMinimumSystemVersion=13.0`, `LSUIElement=true` (Overlay ohne Dock-Icon), `NSMicrophoneUsageDescription` (ohne diesen Key kein Mic-Prompt). — Beide Projekt-Info.plists erfuellen das bereits.

### 2. Stabile Code-Identitaet fuer TCC (das Kernstueck)

- **Do:** Mit **echtem Zertifikat** signieren (`Apple Development` lokal, `Developer ID Application` fuer Verteilung), nicht ad-hoc. Bei echtem Zertifikat erkennt das System die App ueber Rebuilds als dieselbe (konstante `csreq`). Genau deshalb haelt `build.sh` mit "Frank Local Dev" die Grants. (`extern` jano.dev; `offiziell` codesign(1))
- **Do:** Reihenfolge — codesign ist immer der **LETZTE** Schritt: 1. kompilieren, 2. Info.plist FINAL hineinlegen (stabile Bundle-ID), 3. Resources, 4. ERST DANN signieren.
- **Don't:** Ad-hoc signieren (`codesign --sign -`) wenn TCC-Grants stabil bleiben sollen.
- **Don't:** Die `CFBundleIdentifier` aendern (auch nicht „nur fuer einen Testbuild").

### 3. Signier-Befehl (Hardened Runtime + Entitlements)

```bash
codesign --force \
  --sign "Developer ID Application: Frank ... (TEAMID)" \
  --options runtime \
  --timestamp \
  --entitlements MyApp.entitlements \
  --identifier dev.frank.myapp \
  "MyApp.app"
```

- `--options runtime` → Hardened Runtime (Pflicht fuer Notarization). `--timestamp` → vertrauenswuerdiger Zeitstempel (schlaegt fehl, wenn der Timestamp-Server nicht erreichbar ist → im build.sh abfangen, nicht ignorieren).
- **Entitlements ohne XML-Kommentare** (`<!-- ... -->` koennen codesign/notarytool scheitern lassen — Bug-Almanach E3). Die Projekt-Entitlements sind sauber.

### 4. Notarization-Workflow (Direktverteilung 2026)

```bash
ditto -c -k --keepParent "MyApp.app" "MyApp.zip"          # IMMER ditto, NIE zip
xcrun notarytool submit "MyApp.zip" --keychain-profile "FRANK_NOTARY" --wait
xcrun stapler staple "MyApp.app"                          # Ticket anheften (offline-Start)
```

**Verifizieren (Pflicht-Endcheck):**
```bash
codesign --verify --strict --verbose=2 "MyApp.app"
codesign -d --entitlements - "MyApp.app"
spctl -a -vvv -t exec "MyApp.app"        # erwartet: "accepted, source=Notarized Developer ID"
```

### 5. Empfohlene build.sh-Struktur (idempotent + verifizierend)

```bash
set -euo pipefail
rm -rf "$APP"                                            # 1. sauberer Start (idempotent)
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
swiftc -O -target arm64-apple-macos13.0 Sources/*.swift -o "$APP/Contents/MacOS/MyApp"   # 2.
cp Info.plist "$APP/Contents/Info.plist"                # 3. Info.plist FINAL (stabile Bundle-ID)
cp -R Resources/ "$APP/Contents/Resources/"            # 4.
codesign --force --sign "$ID" --options runtime \       # 5. signieren (LETZTER Schritt, OHNE --deep)
  --timestamp --entitlements MyApp.entitlements --identifier "$BUNDLE_ID" "$APP"
codesign --verify --strict --verbose=2 "$APP"           # 6. verifizieren → bricht bei Fehler ab
```

### 6. Don'ts (kompakt)

| Don't | Warum |
|-------|-------|
| Ad-hoc signieren fuer Verteilung/stabile TCC | Keine stabile Identitaet → Grants brechen, kein Gatekeeper-Pass |
| **`codesign --deep` zum Signieren** | **Seit macOS 13.0 deprecated** (codesign(1)); wendet Optionen blind auf nested Inhalte an. Nur zum *Verifizieren* ok. (Beide Projekt-`build.sh` nutzen aktuell `--deep` → umstellen) |
| `Info.plist` nach dem Signieren patchen | Invalidiert die Signatur → TCC denied danach still (Bug-Almanach H4) |
| `zip` fuer das Notarization-Paket | Zerstoert Symlinks/Struktur; `ditto -c -k --keepParent` nutzen (Bug-Almanach G7) |
| `CFBundleIdentifier` aendern | TCC sieht eine fremde App → alle Grants weg (Bug-Almanach H1) |
| `--timestamp`-Fehler ignorieren | Signatur ohne Zeitstempel → Notarization-Probleme |

**Quellen:** [codesign(1) Manpage](https://keith.github.io/xcode-man-pages/codesign.1.html) (offiziell-Spiegel; `--deep` deprecated ab macOS 13.0) · [notarytool(1)](https://keith.github.io/xcode-man-pages/notarytool.1.html) (offiziell-Spiegel) · [Apple Forums — Notarization](https://developer.apple.com/forums/thread/130379) (offiziell) · [Apple — Resolving notarization issues](https://developer.apple.com/documentation/security/resolving-common-notarization-issues) (offiziell) · [jano.dev — Accessibility Permission](https://jano.dev/apple/macos/swift/2025/01/08/Accessibility-Permission.html) (extern, 2025-01) · [scriptingosx — Notarize with notarytool](https://scriptingosx.com/2021/07/notarize-a-command-line-tool-with-notarytool/) (extern, 2021-07)

---

## 🔗 Kopplung zum Bug-Almanach (wechselseitige Bezugstabelle)

Best-Practices (diese Datei) ↔ Bug-Almanach `~/proggs/bugs/desktop/swift-appkit.md`. Die identische Tabelle steht
auch dort — so bleibt jede „so macht man es richtig"-Regel mit ihrer konkreten Bug-Loesung verlinkt.

| Best-Practice-Abschnitt (hier) | Zugehoeriger Bug-Almanach-Abschnitt (`bugs/desktop/swift-appkit.md`) |
|--------------------------------|-------------------------------------------------------------|
| **A** Overlay-Fenster | **A** NSPanel Fokus/Aktivierung (A1/A2/A4), **B** Window-Level/Spaces/Fullscreen (B1–B3), **K2** Overlay-Fokus |
| **B** Moderne Concurrency | **J** Timer/RunLoop & Swift-6-Concurrency (J2, **J4** assumeIsolated, **J5** @Observable), **I4** Off-Main-UI |
| **C** Accessibility (VoiceOver) | (eigene UI fuer VoiceOver — kein direkter Bug; nicht verwechseln mit AX-**Permission** in C/D) |
| **D** Permission-Handling | **C** Accessibility-Permission (C1–C3), **D5/D8** TCC-Kategorien, **E1/E2/E6** Mic-Permission, **H** TCC-Identitaet, **J3** Sequoia-Reauth |
| **E** App-Architektur (MVVM) | **J5** @Observable-Auto-Tracking-Plist-Key |
| **F** Audio | **E** Mikrofon/AVFoundation (**E4** Tap-Format, **E5** Route-Change, **E7** Thread.sleep) |
| **G** Build/Signing | **G** Code-Signing/Notarization (G4–G7, **G6** `--deep`), **H** TCC-Identitaet (H1/H4, **H6** Cert-Ablauf), **K1** Bundle-Setup |

---

## Umgebungs-Gegencheck (Stand 2026-06-02, gegen den echten Projekt-Code)

Geprueft gegen `~/proggs/TerminalVoiceOverlay-macOS` + `~/proggs/ClaudeCodexVoiceOverlay-macOS`.

**Schon richtig gemacht (bestaetigt):**
- Overlay-Panel: `.nonactivatingPanel` im Init, `isFloatingPanel`/`becomesKeyOnlyIfNeeded`/`hidesOnDeactivate=false`, `[.canJoinAllSpaces,.fullScreenAuxiliary]`, `.floating`, `canBecomeKey/Main` ueberschrieben — entspricht Abschnitt A.
- Carbon-Hotkeys mit cmd+shift-Defaults (vermeidet die Sequoia-⌥/⌥⇧-Sperre, Bug D1), `kEventHotKeyPressed`, sauberes `unregisterAll()` — entspricht der Empfehlung „Carbon braucht keine Permission".
- AudioRecorder: Tap mit `inputNode.outputFormat(forBus:0)` (Abschnitt F.1), `AVAudioConverter`, Info.plist mit `NSMicrophoneUsageDescription`/`LSUIElement`/stabiler Bundle-ID, Entitlements ohne XML-Kommentare.
- build.sh: signiert mit echtem Zertifikat ("Frank Local Dev") statt ad-hoc, Info.plist vor dem Signieren kopiert — entspricht Abschnitt G.2.

**Verbesserbar (konkret, belegt):**
1. **`codesign --deep` zum Signieren** (`build.sh` Z. 92/97 in beiden Projekten) → `--deep` ist seit macOS 13.0 zum Signieren deprecated (Abschnitt G.6). Umstellen auf Signieren ohne `--deep`.
2. **`AVAudioEngineConfigurationChange` fehlt** im AudioRecorder → Aufnahme bricht bei Geraetewechsel (AirPods/USB) still ab (Abschnitt F.4, Bug-Almanach E5).
3. **`Thread.sleep(0.05)` in `AudioRecorder.stop()`** → durch `removeTap` + Continuation ersetzen (Abschnitt F.5).
4. **Optional/groesser:** Concurrency-Modernisierung (Abschnitt B) und Architektur-Entkopplung (Abschnitt E, `HotkeyRegistry.shared` → DI, State raus aus dem Panel) — bewusst groessere Umbauten, nur auf Wunsch.

---

## Pflicht-Checkliste vor Arbeit an Swift/AppKit-Overlay-Code

- [ ] **Overlay-Panel:** `.nonactivatingPanel` nur im Init; `hidesOnDeactivate=false`; bewusster Level + `collectionBehavior`; `orderFrontRegardless()` statt `activate()` (A)
- [ ] **Concurrency:** Off-Main-Callbacks per AsyncStream/`Task{@MainActor}`, NIE `assumeIsolated` off-main; On-Main-C-Callbacks `assumeIsolated` ok (B)
- [ ] **Accessibility:** custom Buttons mit Label + Rolle + `accessibilityPerformPress()`; Status per `accessibilityValue`+`post` (C)
- [ ] **Permissions:** just-in-time, nie alle beim Launch, Graceful Degradation, Accessibility pollen, Direkt-Button ins Settings-Pane (D)
- [ ] **Architektur:** AppDelegate = Composition-Root, Services per Protokoll-DI (kein `.shared`), State im ViewModel (E)
- [ ] **Audio:** Tap-Format vom Node, `AVAudioEngineConfigurationChange` behandeln, kein `Thread.sleep`, UI auf Main (F)
- [ ] **Build:** echtes Zertifikat, codesign zuletzt, KEIN `--deep` zum Signieren, `ditto` statt `zip`, stabile Bundle-ID (G)
- [ ] Bei jedem neu erlebten Bug → Eintrag in `bugs/desktop/swift-appkit.md` + ggf. Best-Practice hier ergaenzen, Bezugstabelle synchron halten.

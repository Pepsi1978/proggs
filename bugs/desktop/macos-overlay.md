# Bekannte Bugs/Fallen: macOS-Overlay-Fenster (Swift/AppKit)

> PFLICHT-LESEN vor Arbeit an Franks Voice-Overlays **ClaudeCodexVoiceOverlay-macOS** und
> **TerminalVoiceOverlay-macOS** (schwebend, globaler Hotkey, Mikrofon).
> Stand: recherchiert am 2026-06-14 (7 Researcher parallel, Apple Developer zuerst).
> Versions-Anker: Swift 6 / Xcode 26, deployment target **macOS 13/14**, aktuelles macOS **26 Tahoe**.
> Schwerpunkt: cooperative activation (macOS 14+), TCC↔Code-Signatur, reale Regressionen in macOS 15/26.
> Diese Fallen sind beim **Best-Practices-Lauf** mitgefunden worden; eine noch tiefere, dedizierte
> Bug-Recherche (Issue-Tracker-Fokus + Fix-Status) kann später per `bug-almanach-recherche` ergänzt werden.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/desktop/macos-overlay.md`](../../best-practices/desktop/macos-overlay.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Sektionen: **N** NSPanel/Window/Activation · **S** Spaces/Fullscreen/Click-through · **H** Hotkeys ·
> **T** TCC Accessibility/Input-Monitoring · **M** Mikrofon · **L** Login-Item/SMAppService ·
> **C** Code-Signing/Notarisierung.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | App klaut beim Start den Fokus (trotz LSUIElement) | Start-Sequenz `.prohibited` → `.accessory` | N2 |
| 2 | `activate(ignoringOtherApps:)` wirkt nicht mehr | Deprecated seit macOS 14; `orderFrontRegardless()` | N3 |
| 3 | Eingabe ins TextField geht ins falsche Fenster | Sequoia-Regression; `statusItem` als `var`, spät zuweisen | N4 |
| 4 | Overlay verschwindet über Fullscreen-App | `collectionBehavior` += `.fullScreenAuxiliary` | S1 |
| 5 | Overlay liegt nicht ÜBER Fullscreen | `level = .statusBar` (nicht `.maximumWindow`) | S2 |
| 6 | Klicks gehen nicht durch (Sonoma+) | `ignoresMouseEvents=true` oder `hitTest`→nil | S3 |
| 7 | Overlay blockiert System / unresponsive (Tahoe 26.3 RC) | Ziel-styleMask gleich erzeugen; `ignoresMouseEvents` | S4 |
| 8 | Hotkey mit nur ⇧/⌥ feuert nie (-9868) | Sequoia: Kombi MUSS ⌘ oder ⌃ enthalten | H1 |
| 9 | CGEventTap stirbt (Timeout/Sleep) | `tapDisabledByTimeout` re-enablen + Wake-Notification | H2, H3 |
| 10 | Hotkey schweigt bei Passwortfeld | Secure Input aktiv — erkennen, nicht umgehen | H4 |
| 11 | Tap liefert keine Events trotz non-nil | Health-Watchdog; stabile Signatur (Silent-Disable-Race) | T1 |
| 12 | Permission weg nach Update/Rebuild | Stabile Developer-ID; ad-hoc hat keinen stabilen DR | T2, C1 |
| 13 | Prompt kommt nur einmal, Toggle aus | Nicht erneut prompten; Nutzer in Settings-Pane führen | T3 |
| 14 | App-Crash beim ersten Mikrofon-Zugriff | `NSMicrophoneUsageDescription` fehlt in Info.plist | M1 |
| 15 | `format.sampleRate`-Crash beim Tap | Live-Hardware-Format (`outputFormat(forBus:)`) | M2 |
| 16 | Login-Item reaktiviert sich selbst | Nie blind `register()`; Nutzer-Wahl respektieren | L1 |
| 17 | „App is damaged / cannot be opened" | Developer ID + notarisieren + stapeln; xattr putzen | C7 |
| 18 | Notarisierung „signature invalid" | `--timestamp` + `ditto`-ZIP + `xattr -cr`, kein `--deep` | C2, C4, C5 |

---

## N) NSPanel / Window / Activation

### N1. Nonactivating-StyleMask zur Laufzeit umschalten bricht Tastatureingabe
- **Symptom:** Nach `panel.styleMask = …` (Entfernen/Setzen von `.nonactivatingPanel`) zeichnet das Panel als Key (Focus-Ring), aber Tippen ins TextField funktioniert nicht.
- **Ursache:** `-setStyleMask:` ruft `-_setPreventsActivation:` nicht erneut auf; das WindowServer-Tag `kCGSPreventsActivationTagBit` bleibt aus dem Init bestehen → Mismatch.
- **Versionen:** alle (Apple Feedback FB16484811, offen).
- **FIX:** StyleMask nicht zur Laufzeit umschalten — zwei Panel-Instanzen verwenden. Falls zwingend: nach jeder Änderung `perform(Selector(("_setPreventsActivation:")), with: NSNumber(value: styleMask.contains(.nonactivatingPanel)))` (SPI).
- **Quelle:** https://philz.blog/nspanel-nonactivating-style-mask-flag/ (extern)

### N2. App klaut beim Start den Fokus trotz `LSUIElement` ⭐ HAEUFIG
- **Symptom:** Background-/Agent-App ohne Dock-Icon deaktiviert beim Launch die Vordergrund-App (Finder/Editor verliert Fokus).
- **Ursache:** Die Default-Aktivierungs-Policy aktiviert die App kurz beim Start; `LSUIElement` allein verhindert das nicht.
- **Versionen:** alle.
- **FIX:** `applicationWillFinishLaunching` → `setActivationPolicy(.prohibited)`, dann `applicationDidFinishLaunching` → `setActivationPolicy(.accessory)`. Startet ohne Fokus-Klau.
- **Quelle:** https://multi.app/blog/nailing-the-activation-behavior-of-a-spotlight-raycast-like-command-palette (extern)

### N3. `activate(ignoringOtherApps: true)` funktioniert nicht mehr (Sonoma+) ⭐ HAEUFIG
- **Symptom:** Fenster soll nach vorn + aktiviert werden, wird unter Sonoma/Sequoia/Tahoe nur gezeigt, nicht aktiviert; oder aktiviert verspätet und klaut dann den Fokus.
- **Ursache:** `activate(ignoringOtherApps:)` ist seit macOS 14 deprecated; cooperative activation behandelt Aktivierung als ablehnbare Bitte.
- **Versionen:** macOS 14+.
- **FIX:** Fürs Overlay ist Nicht-Aktivieren gewünscht → `orderFrontRegardless()` + `.nonactivatingPanel`, gar nicht aktivieren. Muss man doch: parameterloses `NSApp.activate()`.
- **Quelle:** https://developer.apple.com/documentation/appkit/nsapplication/activate(ignoringotherapps:)

### N4. NSTextField in Status-Bar-/Background-App bekommt keine Key-Events (Sequoia-Regression)
- **Symptom:** Ab macOS 15 zeigt das TextField den Focus-Ring, aber Tastatureingaben landen im zuvor aktiven Fenster (Xcode/Safari). Sonoma fehlerfrei.
- **Ursache:** Sequoia-Responder-/Fokus-Regression bei Status-Item-getriebenen Views; im konkreten Fall ein Lebenszyklus-Problem des `statusItem`.
- **Versionen:** macOS 15+.
- **FIX:** `statusItem` NICHT als sofort initialisierte `let`-Konstante, sondern als `var statusItem: NSStatusItem?` und erst in `applicationDidFinishLaunching` zuweisen.
- **Quelle:** https://developer.apple.com/forums/thread/766783 (extern)

### N5. Tahoe: Overlay/Hintergrund flackert unter transparenter Menüleiste
- **Symptom:** In macOS 26 zeigt der Bereich hinter der transparenten (Liquid-Glass-)Menüleiste nur eine dünne Linie statt Glas; Overlay-Hintergrund wirkt inkonsistent.
- **Ursache:** Liquid-Glass-Compositing der transparenten Menüleiste bei Fenstern direkt dahinter.
- **Versionen:** macOS 26 Tahoe.
- **FIX:** Eigenen definierten Hintergrund setzen (`NSVisualEffectView`/Vibrancy statt nacktem `.clear`); Overlay nicht bündig unter die Menüleiste legen.
- **Quelle:** https://discussions.apple.com/thread/256145254 (extern)

---

## S) Spaces / Fullscreen / Click-through

### S1. Overlay verschwindet beim Wechsel in eine Fullscreen-App ⭐ HAEUFIG
- **Symptom:** Auf normalen Spaces sichtbar, aber sobald eine App in den Vollbild-Space geht, ist das Overlay weg.
- **Ursache:** Nur `.canJoinAllSpaces` gesetzt, **`.fullScreenAuxiliary` fehlt** — ohne dieses Flag darf das Fenster nicht auf Fullscreen-Spaces erscheinen.
- **Versionen:** alle.
- **FIX:** `collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]`.
- **Quelle:** https://developer.apple.com/forums/thread/26677 (extern, Ken Thomases)

### S2. Overlay auf allen Spaces, aber NICHT über der Fullscreen-App selbst
- **Symptom:** `.canJoinAllSpaces + .fullScreenAuxiliary` gesetzt, Fenster auf allen Spaces, liegt aber nicht über dem Fullscreen-Fenster.
- **Ursache:** Window-Level zu niedrig (`.normal`/`.floating` kann unter dem Fullscreen-Fenster liegen).
- **Versionen:** alle.
- **FIX:** `level = .statusBar` (`OverlayWindowLevelKey`) oder höher. NICHT `.maximumWindow` (instabil, Apple rät ab).
- **Quelle:** https://developer.apple.com/forums/thread/26677 (extern)

### S3. Sonoma+: transparentes Fenster lässt nach mehreren `setNeedsDisplay` keine Klicks mehr durch
- **Symptom:** Voll-transparentes Borderless-Window lässt anfangs Klicks durch; nach mehrfachem Neuzeichnen fängt das Overlay plötzlich alle Klicks. macOS 13 korrekt.
- **Ursache:** Regression im Hit-Testing transparenter `drawRect`-Bereiche; neu gezeichnete clear-Regionen gelten nicht mehr als durchlässig.
- **Versionen:** macOS 14+ (Sonoma).
- **FIX:** Ganz durchlässig → `ignoresMouseEvents = true` (robust). Teilbereiche → `hitTest`-Override, der für transparente Bereiche `nil` zurückgibt, statt auf Compositing-Transparenz zu vertrauen.
- **Quelle:** https://developer.apple.com/forums/thread/737584 (extern)

### S4. Tahoe 26.3 RC: Custom-`styleMask`-Fenster werden unresponsive / Overlay blockiert System ⭐ HAEUFIG
- **Symptom:** (1) Custom-`styleMask`-NSWindows werden „totally unresponsive"/mausdurchlässig (Bruch ggü. 26.3 beta3). (2) Fullscreen-Overlays fangen Maus-Events vom GESAMTEN transparenten Fenster → blockieren System-Interaktion, solange sichtbar.
- **Ursache:** Regression im AppKit-Hit-Testing/`styleMask`-Handling in der 26.3 **RC**.
- **Versionen:** macOS 26.3 RC (nicht beta3).
- **FIX:** Fenster gleich mit Ziel-styleMask erzeugen (nicht nachträglich `.titled` entfernen); für reine Anzeige `ignoresMouseEvents = true`; auf finalem Tahoe-Stand testen, ggf. Feedback einreichen.
- **Quelle:** https://developer.apple.com/forums/thread/814798 (extern)

### S5. Tahoe 26.2: NSHostingView fängt keine Maus-Events, wenn über anderem NSHostingView
- **Symptom:** Ein NSHostingView über einem zweiten NSHostingView empfängt keine Maus-Events mehr.
- **Ursache:** Regression im NSHostingView-Layering auf Tahoe 26.2.
- **Versionen:** macOS 26.2.
- **FIX:** Geschachtelte NSHostingViews vermeiden, ein einziges Hosting-Root; falls Schichtung nötig, AppKit-`NSView`-Zwischenschicht mit explizitem `hitTest`.
- **Quelle:** https://developer.apple.com/forums/thread/812113 (extern)

### S6. Overlay wird in Mission Control / Exposé weggeschoben oder ausgeblendet
- **Symptom:** Beim Öffnen von Mission Control rutscht/verschwindet das Overlay.
- **Ursache:** Bei Window-Level über `.normal` ist der Exposé-Default automatisch `.transient` (in Exposé versteckt).
- **Versionen:** alle.
- **FIX:** Explizit `.stationary` in `collectionBehavior` → bleibt sichtbar und unbewegt wie das Desktop-Fenster.
- **Quelle:** https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/WinPanel/Articles/SettingWindowCollectionBehavior.html

### S7. Fenster nicht auf neu erstellten Spaces sichtbar
- **Symptom:** `.canJoinAllSpaces` gesetzt, aber auf nach dem Erzeugen neu angelegten Spaces fehlt das Fenster.
- **Ursache:** Historischer Bug; `collectionBehavior` nach Space-Änderungen nicht neu angewandt.
- **Versionen:** ältere macOS, teils gelöst.
- **FIX:** Auf `NSWorkspace.activeSpaceDidChangeNotification` reagieren und `collectionBehavior`/`orderFrontRegardless()` erneut anwenden, falls das Overlay auf einem neuen Space fehlt.
- **Quelle:** https://developer.apple.com/forums/thread/671674 (extern)

### S8. Multi-Monitor: `NSScreen.main` liefert falschen Bildschirm bei Accessory-App
- **Symptom:** Overlay landet auf dem falschen Monitor.
- **Ursache:** `NSScreen.main`-Semantik bei Hintergrund-(Accessory-)Apps zeigt auf den Menüleisten-Screen; verschärft, wenn „Displays have separate Spaces" aus ist.
- **Versionen:** alle (FB11506568).
- **FIX:** Nicht `NSScreen.main` für die Platzierung; über `NSScreen.screens` iterieren bzw. Screen über die Mausposition bestimmen (`NSScreen.screens.first { $0.frame.contains(NSEvent.mouseLocation) }`); ein Panel pro Screen.
- **Quelle:** https://github.com/feedback-assistant/reports/issues/355 (extern)

---

## H) Hotkeys

### H1. Hotkey mit nur Shift/Option scheitert (`-9868`, Sequoia+) ⭐ HAEUFIG
- **Symptom:** `RegisterEventHotKey` mit z.B. ⌥⇧2 schlägt fehl, Hotkey reagiert nie. Trifft alle Carbon-Libs.
- **Ursache:** Absichtliche Sequoia+-Sperre (Anti-Keylogger); Shift/Option-only verboten.
- **Versionen:** macOS 15+ (von Apple-Engineer bestätigt, „no workaround").
- **FIX:** Jede Kombi mind. ⌘ oder ⌃. In der Recorder-UI Shift/Option-only ablehnen. Reine ⌥-Taste als PTT → CGEventTap statt Carbon.
- **Quelle:** https://developer.apple.com/forums/thread/763878 (extern)

### H2. CGEventTap „stirbt" und kommt nie zurück (Timeout) ⭐ HAEUFIG
- **Symptom:** Hotkey funktioniert anfangs, hört nach Last/Zeit auf; manchmal friert die System-Tastatur kurz ein.
- **Ursache:** Callback zu langsam → macOS deaktiviert den Tap via `kCGEventTapDisabledByTimeout`. Ohne Recovery bleibt er tot.
- **Versionen:** alle.
- **FIX:** `tapDisabledByTimeout`/`…ByUserInput` in die eventMask aufnehmen und im Callback sofort `CGEvent.tapEnable(tap:enable:true)`; Callback schlank halten, schwere Arbeit async.
- **Quelle:** https://developer.apple.com/documentation/coregraphics/cgeventtype/tapdisabledbytimeout

### H3. Globaler Hotkey/Tap tot nach Sleep/Wake oder Lock/Unlock (Tahoe-Regression)
- **Symptom:** Auf macOS 26 (bestätigt 26.3.1) funktioniert der globale Hotkey nicht mehr nach Schlaf/Sperre; nur App-Neustart hilft.
- **Ursache:** Der CGEventTap wird beim Session-/Power-Übergang ungültig und nicht neu registriert.
- **Versionen:** macOS 26 Tahoe.
- **FIX:** Auf `NSWorkspace.didWakeNotification`/`sessionDidBecomeActiveNotification` hören und Tap re-enablen/neu erstellen. Robuste Alternative für reinen Toggle: Carbon `RegisterEventHotKey` (übersteht Sleep/Wake zuverlässiger).
- **Quelle:** https://github.com/ghostty-org/ghostty/discussions/11819 (extern)

### H4. Hotkey schweigt, während ein Passwortfeld aktiv ist (Secure Input)
- **Symptom:** Globaler Hotkey/PTT reagiert nicht, solange ein Passwortfeld fokussiert ist oder eine App Secure Input „kleben" lässt.
- **Ursache:** `EnableSecureEventInput` (NSSecureTextField, Terminal-Secure-Keyboard-Entry, Passwort-Manager) blockiert CGEventTap UND NSEvent-Monitor vom Tastatur-Stream.
- **Versionen:** alle (erwartetes OS-Verhalten).
- **FIX:** Nicht umgehbar — handhaben: mit `IsSecureEventInputEnabled()` prüfen und dem Nutzer signalisieren („Hotkey pausiert"). Eigenes `EnableSecureEventInput` sparsam + immer mit `DisableSecureEventInput` paaren.
- **Quelle:** https://developer.apple.com/library/archive/technotes/tn2150/_index.html

### H5. Falsche Berechtigung erwartet (Accessibility vs. Input Monitoring)
- **Symptom:** App listet sich im falschen Privacy-Pane, Prompt erscheint nicht, Sandbox-/App-Store-Build abgelehnt.
- **Ursache:** NSEvent-Global-Monitor braucht **Accessibility**; CGEventTap braucht **Input Monitoring** (im Sandbox-Kontext sauberer zu bekommen).
- **Versionen:** alle.
- **FIX:** Für Sandbox/Store CGEventTap + `CGPreflightListenEventAccess()`/`CGRequestListenEventAccess()`. Carbon `RegisterEventHotKey` braucht gar keine Permission.
- **Quelle:** https://developer.apple.com/forums/thread/707680 (extern)

### H6. Memory-Mismatch im CGEventTap-Lifecycle (Swift)
- **Symptom:** Crash beim Stoppen des Taps oder Use-after-free/Leak.
- **Ursache:** `userInfo`-Pointer-Handling mit `Unmanaged` passt nicht zur Retain-Strategie.
- **Versionen:** alle.
- **FIX:** Quinns Muster exakt spiegeln: bei `tapCreate`-Fehlschlag `release()`, beim `stop()` `CFMachPortInvalidate` + genau ein passendes `release`; bei `passUnretained` die Tap-Instanz stark halten.
- **Quelle:** https://developer.apple.com/forums/thread/707680 (extern)

---

## T) TCC: Accessibility / Input Monitoring

### T1. „Silent Disable Race" nach Re-Signing — Tap liefert keine Events ⭐ HAEUFIG
- **Symptom:** `tapCreate()` liefert non-nil, `tapIsEnabled` ist true — aber kein einziger Callback feuert. Kein Crash, kein Error. Nach `codesign --force` + Start via Finder/Dock.
- **Ursache:** TCC-Entscheidungen hängen an der Code-Identität; Re-Signing erzeugt faktisch eine neue Identität, Launch Services triggert eine Trust-Re-Evaluierung, die den Tap inert macht.
- **Versionen:** alle.
- **FIX:** Health-Watchdog (5-s-Timer), der `tapIsEnabled` prüft und Tap re-enabled/neu installiert; in Dev Binary direkt starten; Release mit stabiler Developer-ID signieren. „Ein non-nil Tap ist kein gesunder Tap."
- **Quelle:** https://danielraffel.me/til/2026/02/19/cgevent-taps-and-code-signing-the-silent-disable-race/ (extern)

### T2. Permission überlebt App-Update/Re-Signing NICHT ⭐ HAEUFIG
- **Symptom:** App steht weiter in der Liste, Toggle sieht „an" aus — funktioniert nach Update nicht mehr.
- **Ursache:** TCC verknüpft den Grant mit dem Designated Requirement der Signatur; Update/Re-Signing mit anderer Identität invalidiert den Eintrag. (Signing-Sicht: siehe C1.)
- **Versionen:** alle.
- **FIX:** Über alle Releases mit derselben Developer-ID-Identität signieren. Bei Verlust: Eintrag entfernen + neu hinzufügen oder `tccutil reset Accessibility/ListenEvent <bundleid>` → Re-Prompt. Toggle aus/ein reicht NICHT.
- **Quelle:** https://jano.dev/apple/macos/swift/2025/01/08/Accessibility-Permission.html (extern)

### T3. Prompt kommt nur einmal, App in Liste aber Toggle aus
- **Symptom:** `AXIsProcessTrustedWithOptions([prompt:true])` zeigt beim zweiten Aufruf keinen Dialog mehr; App in der Liste, Toggle aus → keine Funktion.
- **Ursache:** TCC merkt sich pro Code-Identität, dass schon gefragt wurde; der Prompt ist eine Einmal-Einladung.
- **Versionen:** alle.
- **FIX:** Nicht auf wiederholten Prompt verlassen — Status pollen (`AXIsProcessTrusted()`/`CGPreflightListenEventAccess()`) + Nutzer per `x-apple.systempreferences:…?Privacy_ListenEvent` in den Pane führen, mit Anleitung den Toggle selbst setzen lassen.
- **Quelle:** https://developer.apple.com/forums/thread/24288 (extern)

### T4. Xcode-Debug: falsche App in der Permission-Liste
- **Symptom:** Im Debug wird die Permission nie wirksam, obwohl gesetzt.
- **Ursache:** Beim Debuggen aus Xcode braucht oft **Xcode selbst** den Grant (Parent-Prozess vererbt an Kind); gleiches für Terminal-gestartete Prozesse.
- **Versionen:** alle.
- **FIX:** App bauen → in der Liste entfernen + neu hinzufügen → via *Product > Perform Action > Run Without Building* starten; oder Xcode/Terminal selbst in den Pane aufnehmen.
- **Quelle:** https://gertrude.app/blog/macos-request-accessibility-control (extern)

### T5. `CGEventTapCreate` liefert nil
- **Symptom:** Tap-Erstellung gibt nil zurück.
- **Ursache:** Keine Permission für diesen Tap-Typ, oder Erstellung fehlgeschlagen; sieht oft identisch aus wie „Tap installiert, aber keine Events".
- **Versionen:** alle.
- **FIX:** Vor `tapCreate` zwingend `CGPreflightListenEventAccess()`, sonst `CGRequestListenEventAccess()`; bei nil → Nutzer in den Input-Monitoring-Pane; bei `.defaultTap` zusätzlich `AXIsProcessTrusted()`.
- **Quelle:** https://developer.apple.com/forums/thread/758554 (extern)

### T6. `IOHIDManager`-Pfad auf macOS 15 tot
- **Symptom:** HID-basiertes Keyboard-Monitoring funktioniert auf Sonoma, schweigt auf Sequoia.
- **Ursache:** Von Apple DTS bestätigte Regression (ab macOS 14/15) bei `IOHIDManager` für Keyboard-Events.
- **Versionen:** macOS 14/15.
- **FIX:** Auf `CGEventTap` umstellen (lief im selben Apple-Test weiter).
- **Quelle:** https://developer.apple.com/forums/thread/779582 (extern)

---

## M) Mikrofon

### M1. Sofortiger App-Exit beim ersten Mikrofon-Zugriff ⭐ HAEUFIG
- **Symptom:** App stürzt ohne Prompt ab; Crashlog `__TCC_CRASHING_DUE_TO_PRIVACY_VIOLATION__`.
- **Ursache:** `NSMicrophoneUsageDescription` fehlt in der Info.plist des gebauten Bundles.
- **Versionen:** alle.
- **FIX:** Key mit Begründungstext einfügen; prüfen, dass er im finalen `.app/Contents/Info.plist` landet. Aus Finder starten zum Verifizieren (Terminal-Start erbt fälschlich Terminal-Permission).
- **Quelle:** https://developer.apple.com/forums/thread/728814

### M2. `format.sampleRate == hwFormat.sampleRate`-Crash beim Tap ⭐ HAEUFIG
- **Symptom:** Fataler Crash in `AVAudioIONodeImpl::SetOutputFormat` beim Tap-Installieren oder Zugriff auf `inputNode`.
- **Ursache:** Hardware-Format änderte sich (Geräte-Wechsel, Aggregat-Device, ungewöhnliche Rate); ein fest vorgegebenes Format passt nicht mehr.
- **Versionen:** alle.
- **FIX:** Format IMMER live via `inputNode.outputFormat(forBus: 0)` holen und genau dieses an `installTap` geben; vor Start `sampleRate > 0 && channelCount > 0` prüfen; auf `AVAudioEngineConfigurationChange` reagieren.
- **Quelle:** https://developer.apple.com/forums/thread/711583 (extern)

### M3. Tap bricht / liefert keine Buffer nach Geräte-Wechsel (AirPods, USB)
- **Symptom:** Nach Umschalten der Eingabe (Bluetooth A2DP→HFP, USB-Interface) kommen keine/falsche Buffer.
- **Ursache:** `inputNode` aktualisiert sich nicht zuverlässig in-place; reset/reconnect helfen oft nicht.
- **Versionen:** alle.
- **FIX:** Bei `AVAudioEngineConfigurationChange` Tap entfernen, Engine stoppen und Engine/Tap **neu aufbauen** (im Härtefall neue `AVAudioEngine`-Instanz). Engine NIE im Notification-Handler deallokieren.
- **Quelle:** https://developer.apple.com/forums/thread/705706 (extern)

### M3a. `engine.start()` scheitert mit CoreAudio-Code 2003329396 ('what') ⭐ HAEUFIG
- **Symptom:** "Mikrofon nicht verfuegbar — com.apple.coreaudio.avfaudio error 2003329396", obwohl das Geraet im System da ist und die Berechtigung steht. Oft direkt nach einem Geraetewechsel oder wenn ein zweites Programm im selben Moment aufnimmt.
- **Ursache:** `AVAudioEngine.start()` bekommt eine AudioUnit, die im aktuellen Zustand nicht starten kann. Ein einzelner Startversuch gibt sofort auf. Verschaerfend: wird das Eingabegeraet nur bei LEEREM Format (`sampleRate <= 0`) neu gebunden, rettet das nichts — CoreAudio haelt ein verschwundenes Geraet mit gueltig wirkendem Format im Prozess-Cache.
- **Versionen:** alle (bestaetigt macOS 15/26, TerminalVoiceOverlay 1.36 / ClaudeCodexVoiceOverlay 1.34).
- **FIX:** Start bis zu 3x wiederholen mit ~0,25 s Pause; ab dem ZWEITEN Anlauf `kAudioOutputUnitProperty_CurrentDevice` ausdruecklich auf das Standard-Eingabegeraet setzen (`forceRebind`), statt nur bei leerem Format. Vorher `AVCaptureDevice.authorizationStatus(for: .audio)` pruefen — bei `.denied`/`.restricted` sofort mit Klartext abbrechen statt dreimal vergeblich zu starten. `inputNode.audioUnit` NIE force-unwrappen (ist nach einem Ton-Stack-Neustart wirklich nil).
- **Zusatz:** Rohe CoreAudio-Codes nie an den Benutzer durchreichen — 'what' (2003329396), 'nope' (560557673), '!pri' (561015905) in Klartext uebersetzen.
- **Quelle:** eigener Bug-Case 2026-08-27 (`.claude/agent-memory/shared/bug-cases.jsonl`)

### M4. Prompt erscheint nie beim Hintergrund-/Login-Item-Start
- **Symptom:** Overlay startet als Agent/Login-Item, `requestAccess` ruft keinen Dialog auf; App fehlt teils in der Settings-Liste.
- **Ursache:** TCC zeigt den Mikro-Prompt zuverlässig nur bei aktiver UI / Vordergrund-App.
- **Versionen:** alle.
- **FIX:** Permission-Request an einen sichtbaren Nutzer-Trigger binden; vor `requestAccess` per `NSApp.activate(ignoringOtherApps: true)` aktiv nach vorne holen; Erst-Prompt im UI-Kontext auslösen.
- **Quelle:** https://developer.apple.com/forums/thread/807323 (extern)

### M5. `authorizationStatus` bleibt `.notDetermined` trotz Freischaltung in Settings
- **Symptom:** Nutzer aktiviert Mikrofon in den Systemeinstellungen, App liest weiter `.notDetermined`.
- **Ursache:** Status-Cache wird ohne `requestAccess` nicht aktualisiert.
- **Versionen:** alle.
- **FIX:** Nach Rückkehr aus den Settings (App `didBecomeActive`) Status neu abfragen bzw. `requestAccess` aufrufen (gibt jetzt direkt `true` ohne erneuten Dialog).
- **Quelle:** https://developer.apple.com/forums/thread/738986 (extern)

### M6. Notarisierte App crasht beim Mikrofon-Zugriff trotz Info.plist-Key
- **Symptom:** Lokal okay, nach Notarisierung/Distribution Crash beim Aufnehmen.
- **Ursache:** Fehlendes `com.apple.security.device.audio-input`-Entitlement unter Hardened Runtime; tccd verweigert.
- **Versionen:** alle (Hardened Runtime).
- **FIX:** Entitlement in der `.entitlements`-Datei setzen, Hardened-Runtime-Build korrekt signieren, dann notarisieren.
- **Quelle:** https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.security.device.audio-input

### M7. Capture schlägt fehl bei abweichenden Sample-Rates (macOS 15 spät / 26.0)
- **Symptom:** Stille/fehlgeschlagene Aufnahme bei unterschiedlichen Geräte-Sample-Rates oder niedrigen Raten.
- **Ursache:** OS-Regression in späten Sequoia-Versionen und Tahoe 26.0.
- **Versionen:** macOS 15 spät / 26.0; **gefixt in 26.1**.
- **FIX:** Nutzer auf macOS 26.1+ anheben; app-seitig defensiv mit `AVAudioConverter` auf ein festes STT-Zielformat (16 kHz mono) wandeln.
- **Quelle:** https://weblog.rogueamoeba.com/2025/11/04/macos-26-tahoe-includes-important-audio-related-bug-fixes/ (extern)

---

## L) Login-Item / SMAppService

### L1. Auto-Register überschreibt Nutzer-Wahl ⭐ HAEUFIG
- **Symptom:** Nutzer deaktiviert das Login-Item, App registriert sich beim nächsten Start (z.B. in `didFinishLaunching`) wieder selbst.
- **Ursache:** Blindes `register()` beim App-Start ignoriert die Nutzer-Entscheidung; verstößt gegen App-Review 2.4.5(iii).
- **Versionen:** macOS 13+.
- **FIX:** Nie automatisch registrieren. Nur per Settings-Toggle (Default `false`), vorher `status` prüfen und `.enabled`/extern deaktivierte Zustände respektieren.
- **Quelle:** https://developer.apple.com/forums/thread/760186 (extern)

### L2. `.mainApp` meldet `.notFound` nach externer Deaktivierung
- **Symptom:** `SMAppService.mainApp.status` liefert `.notFound` (statt erwartet `.notRegistered`), UI desynchronisiert.
- **Ursache:** Externes Entfernen/Deaktivieren in den Systemeinstellungen kann `.notFound` zurückgeben; Zustand in der BTM-DB persistiert.
- **Versionen:** macOS 13+.
- **FIX:** Status immer live lesen, alle vier Fälle behandeln, `!= .enabled` als „aus"; bei Fenster-Reaktivierung neu syncen.
- **Quelle:** https://developer.apple.com/forums/thread/707482 (extern)

### L3. `unregister()` → Fehler 113 „Could not find the specified service" / Geister-Eintrag
- **Symptom:** `unregister()` schlägt mit `SMAppServiceErrorDomain Code 113` fehl; das Item bleibt sichtbar, persistiert über Reboots/Deinstallation.
- **Ursache:** Inkonsistenter BTM-Registrierungszustand; BTM persistiert „user intent" bewusst über die App-Lebensdauer hinaus.
- **Versionen:** macOS 13+.
- **FIX (Recovery):** `sudo sfltool resetbtm` ausführen, authentifizieren, neu booten (setzt die BTM-DB zurück).
- **Quelle:** https://developer.apple.com/forums/thread/707482 (extern)

### L4. `register()` → `Code 1 "Operation not permitted"`
- **Symptom:** `register()` wirft sofort „Operation not permitted".
- **Ursache:** Ein plist mit demselben Label ist bereits von launchd geladen (Alt-Kopie unter `/Library/Launch*`), oder App nicht/falsch signiert.
- **Versionen:** macOS 13+.
- **FIX:** Alte/duplizierte launchd-plists entfernen, korrekt signieren; debuggen via `log stream` auf `smd`/`backgroundtaskmanagementd`.
- **Quelle:** https://developer.apple.com/forums/thread/707482 (extern)

### L5. Agent startet nicht — `error 0xd – Permission denied` (BundleProgram falsch)
- **Symptom:** `.agent`-Registrierung wirft keinen Fehler, aber der Agent spawnt nicht; launchd-Log `Permission denied`/`exit(78)`.
- **Ursache:** `BundleProgram` zeigt auf das `.app`-Bundle statt auf die eigentliche Mach-O-Executable.
- **Versionen:** macOS 13+.
- **FIX:** `BundleProgram` immer auf die Executable (`…/Contents/MacOS/Agent`) zeigen; der Agent braucht meist gar kein eigenes `.app`-Bundle.
- **Quelle:** https://developer.apple.com/forums/thread/750528 (extern)

### L6. Ventura 13.6: launchd-Job wird beim Deaktivieren nicht beendet
- **Symptom:** Nutzer deaktiviert den Dienst, der launchd-Job läuft weiter (`enabled` + `disallowed`).
- **Ursache:** Bestätigter macOS-Bug in 13.6 (FB13206906).
- **Versionen:** macOS 13.6 (in neueren behoben).
- **FIX:** Clientseitig keiner; defensiv nicht darauf verlassen, dass „disallowed" sofort killt — eigene Laufzeitprüfung der `status`.
- **Quelle:** https://theevilbit.github.io/posts/smappservice/ (extern)

### L7. App öffnet beim Login ein Fenster / klaut Fokus
- **Symptom:** Overlay erscheint beim Login mit Fenster oder im Dock, weil `.mainApp` den vollen GUI-Prozess startet.
- **Ursache:** Fehlendes `LSUIElement`/Accessory-Policy; SwiftUI öffnet automatisch ein `WindowGroup`; Sequoia entfernte die „Hide"-Option.
- **Versionen:** macOS 13+ (Sequoia verschärft).
- **FIX:** `LSUIElement = true` (oder `.accessory`), kein Fenster in `didFinishLaunching` (nur `NSStatusItem`/`MenuBarExtra`), `NSApp.activate` beim Start weglassen.
- **Quelle:** https://developer.apple.com/forums/thread/758393 (extern)

---

## C) Code-Signing / Notarisierung

### C1. Accessibility/Mikrofon-Permission nach jedem Rebuild/Update weg (ad-hoc) ⭐ HAEUFIG
- **Symptom:** App fragt nach Update/Rebuild erneut nach Accessibility/Mikro, obwohl schon erteilt.
- **Ursache:** TCC verknüpft die Erlaubnis mit dem Designated Requirement der Signatur; **ad-hoc (`-`) hat keinen stabilen DR** → macOS erkennt Version N+1 nicht als dieselbe App. Auch Info.plist-Änderung ohne Re-Signing invalidiert. (TCC-Sicht: T2.)
- **Versionen:** alle.
- **FIX:** Mit stabiler, von Apple ausgestellter Identität signieren (Apple Development in Dev, Developer ID Application im Vertrieb) → DR bleibt stabil, Permission erhalten. Festgefahren: `sudo tccutil reset Accessibility|Microphone <bundleid>`.
- **Quelle:** https://developer.apple.com/forums/thread/795739 (extern)

### C2. Lokal valide Signatur, Apple sagt „The signature of the binary is invalid"
- **Symptom:** `codesign` ok, Gatekeeper schweigt, Notarisierung schlägt fehl.
- **Ursache:** fehlendes `--timestamp` (Apples Server-Zeitstempel).
- **Versionen:** alle.
- **FIX:** Immer `--timestamp` mitsignieren.
- **Quelle:** https://www.frr.dev/posts/macos-notarization-guide-linter/ (extern)

### C3. Notarisierung lehnt ab trotz gültiger Signatur (falsches Zertifikat / kein Hardened Runtime)
- **Symptom:** „Invalid" ohne klare Begründung.
- **Ursache:** mit Apple Development statt Developer ID Application signiert, oder `-o runtime` vergessen.
- **Versionen:** alle.
- **FIX:** Developer ID Application + `--options runtime`; `notarytool log <ID>` für Details ziehen.
- **Quelle:** https://www.frr.dev/posts/macos-notarization-guide-linter/ (extern)

### C4. `--deep` Fallstrick beim Signieren
- **Symptom:** Schwer debuggbare Signaturfehler bei Bundles mit Frameworks/Helpern.
- **Ursache:** `--deep` signiert verschachtelten Code in einem Rutsch, „funktioniert in der Praxis nicht gut" und ist für saubere Bundle-Signatur ungeeignet (deprecated).
- **Versionen:** alle.
- **FIX:** `--deep` NICHT zum Signieren; bottom-up einzeln signieren (Helfer/Frameworks zuerst, Bundle zuletzt). `--deep` allenfalls zum Verifizieren.
- **Quelle:** https://gist.github.com/rsms/929c9c2fec231f0cf843a1a746a416f5 (extern)

### C5. ZIP-Format bricht Notarisierung
- **Symptom:** Notarisierung schlägt mit „signature invalid" fehl, obwohl App korrekt signiert.
- **Ursache:** `zip -qr` zerstört Bundle-Symlinks/Struktur.
- **Versionen:** alle.
- **FIX:** ZIP nur mit `ditto -c -k --keepParent App App.zip`.
- **Quelle:** https://gist.github.com/rsms/929c9c2fec231f0cf843a1a746a416f5 (extern)

### C6. Geerbte extended attributes (xattr) brechen die Signatur
- **Symptom:** Signatur wird ungültig nach Datei-Kopie in der Pipeline.
- **Ursache:** `com.apple.quarantine` & Co. werden mitkopiert.
- **Versionen:** alle.
- **FIX:** Vor dem Signieren `xattr -cr App.app`.
- **Quelle:** https://www.frr.dev/posts/macos-notarization-guide-linter/ (extern)

### C7. „App is damaged / cannot be opened" bei unsigniertem/quarantäniertem Overlay ⭐ HAEUFIG
- **Symptom:** Doppelklick → „cannot be opened, developer cannot be verified"; Rechtsklick-Öffnen meldet „damaged".
- **Ursache:** ad-hoc/unsigniert + `com.apple.quarantine`-xattr; ab macOS 15 ist der Ctrl-Click-Bypass weg.
- **Versionen:** alle (verschärft macOS 15+).
- **FIX:** Properly mit Developer ID signieren + notarisieren + stapeln. Notlösung lokal (nur eigene Test-Builds): `xattr -dr com.apple.quarantine App.app`.
- **Quelle:** https://www.idownloadblog.com/2024/08/07/apple-macos-sequoia-gatekeeper-change-install-unsigned-apps-mac/ (extern)

### C8. `xcodebuild -exportArchive` hängt endlos (Developer ID)
- **Symptom:** Kein Output, keine Fehlermeldung, hängt unendlich.
- **Ursache:** exportArchive wartet auf interaktive Apple-ID-Auth, die im Skript-/CI-Kontext nie kommt.
- **Versionen:** alle.
- **FIX:** exportArchive überspringen: `.app` aus dem `.xcarchive` kopieren und manuell mit `codesign` signieren.
- **Quelle:** https://www.frr.dev/posts/macos-notarization-guide-linter/ (extern)

### C9. `codesign` hängt still an unsichtbarem Keychain-Popup
- **Symptom:** Erster Signiervorgang mit neuem Zertifikat aus Skript/IDE hängt; Popup hinter Fenstern.
- **Ursache:** Zugriff auf privaten Schlüssel braucht Keychain-Bestätigung.
- **Versionen:** alle.
- **FIX:** Ersten `codesign` einmal direkt im Terminal ausführen, „Immer erlauben" klicken — danach laufen Skripte durch.
- **Quelle:** https://www.frr.dev/posts/macos-notarization-guide-linter/ (extern)

### C10. Mikro-/AppleEvents schlägt still fehl trotz korrekter Entitlement
- **Symptom:** Kein TCC-Dialog, Zugriff einfach verweigert.
- **Ursache:** Fehlender Usage-String in Info.plist (`NSMicrophoneUsageDescription`/`NSAppleEventsUsageDescription`) — ohne String kein Dialog, keine Erlaubnis.
- **Versionen:** alle.
- **FIX:** Passende Usage-Strings setzen; AppleEvents zusätzlich nur mit `com.apple.security.automation.apple-events`.
- **Quelle:** https://lapcatsoftware.com/articles/hardened-runtime-sandboxing.html (extern)

### C11. `notarytool` exit-code lügt
- **Symptom:** Skript meldet Erfolg, Notarisierung war aber „Invalid".
- **Ursache:** `notarytool` kann mit Status 0 trotz Fehler enden und schreibt Infos auf stderr.
- **Versionen:** alle.
- **FIX:** Output parsen (`status: Accepted` prüfen), bei Zweifel `notarytool log` ziehen, `2>&1` umleiten.
- **Quelle:** https://gist.github.com/rsms/929c9c2fec231f0cf843a1a746a416f5 (extern)

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-macos-overlay.md`) |
|---------------|-------------------------------------------------------------|
| N1–N5 (NSPanel/Window/Activation) | §2 (NSPanel/nonactivating) |
| S1–S8 (Spaces/Fullscreen/Click-through) | §3 (Multi-Space), §4 (Click-through/Transparenz) |
| H1–H6 (Hotkeys) | §5 (Globale Hotkeys) |
| T1–T6 (TCC Accessibility/Input-Monitoring) | §6 (TCC) |
| M1–M7 (Mikrofon) | §7 (Mikrofon) |
| L1–L7 (Login-Item/SMAppService) | §8 (Login-Item) |
| C1–C11 (Code-Signing/Notarisierung) | §9 (Signing/Notarisierung) |

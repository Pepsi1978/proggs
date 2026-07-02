# Bekannte Bugs: macOS-Desktop — Swift / AppKit (Overlay-Apps)

> **PFLICHT-LESEN vor Arbeit an Swift/AppKit-Code (`*.swift`, `Info.plist`, `*.entitlements`, `build.sh`).**
> Dieser Almanach sammelt die oeffentlich bekannten Bugs/Fallen **und ihre funktionserhaltenden
> Loesungen** fuer macOS-Overlay-Apps, die mit Swift + AppKit gebaut werden.
>
> **Stand:** zuletzt recherchiert am **2026-06-02**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax).
> **Anker:** swift=6.3.2  <!-- maschinenlesbar fuer check-version-anchor.py -->
> **Toolchain-Anker:** Xcode **26.5** / Swift **6.3.2** (Mai 2026; Swift 6.2 brachte main-actor-by-default) — per
> Re-Recherche 2026-07-02 **weiterhin aktuell**. **Swift 6.4** wurde auf der WWDC26 (08.06.2026) angekuendigt
> (`~Sendable`-Syntax, `weak let`, Modul-Selektoren `::`, Subprocess 1.0) — Release-Datum noch offen; Xcode 26.6 nicht bestaetigt.
> **Projekt-Anker (Franks Overlay-Apps):** Build per `swiftc` von der Kommandozeile (KEIN Xcode-Projekt,
> KEIN SwiftPM), Build-Target `arm64-apple-macos13.0` (macOS 13+), **unsandboxed**, Direktverteilung
> (kein App Store). Frameworks: AppKit, AVFoundation, CoreGraphics, **Carbon** (RegisterEventHotKey), Network.
> Genutzte heikle APIs: NSPanel `.nonactivatingPanel`, NSWindow.Level, `AXIsProcessTrusted`,
> CGEvent/`NSEvent.addGlobalMonitorForEvents`, Carbon-HotKey, `AVCaptureDevice.requestAccess`,
> `setActivationPolicy(.accessory)`/LSUIElement, `window.animator()`/NSAnimationContext.
> Relevante Systemversionen: Ventura 13, Sonoma 14, **Sequoia 15**, **Tahoe 26** (Liquid-Glass-Design; siehe B7).
>
> Betroffene Projekte: `~/proggs/ClaudeCodexVoiceOverlay-macOS`, `~/proggs/TerminalVoiceOverlay-macOS`.
>
> **Zweite Seite der Medaille (Best Practices):** `~/proggs/best-practices/desktop/swift-appkit.md`
> — *wie man es von vornherein richtig macht*. Wechselseitige Abschnitts-Bezugstabelle unten
> ("Best-Practices-Kopplung"). Stand der Kopplung: 2026-06-02.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Permission weg nach jedem Rebuild (Mic/AX) | Stabile Bundle-ID + echtes Zertifikat, fester Pfad | §H1 |
| 2 | App crasht beim ersten Mic-Zugriff | `NSMicrophoneUsageDescription` in Info.plist setzen | §E1 |
| 3 | `swiftc`-Binary zeigt nie Mic-Prompt | Info.plist mit Bundle-ID einbetten; `.app` per `open` starten | §E2 |
| 4 | Textfeld im Overlay nicht fokussierbar | `canBecomeKey` UND `canBecomeMain` in NSPanel `true` | §A1 |
| 5 | Panel zeigt Fokus, tippt aber nicht | `.nonactivatingPanel` im Init setzen, nie per `setStyleMask` | §A2 |
| 6 | App kommt nicht in den Vordergrund | Kein `activate(ignoringOtherApps:)` — parameterlos `activate()` | §A4 |
| 7 | Hotkeys/Tap sterben nach Stunden/Sleep | Im Callback `tapDisabledByTimeout` abfangen + `tapEnable` | §D4 |
| 8 | CGEventTap-Callbacks feuern nie | Input Monitoring (nicht Accessibility) anfordern | §D5 |
| 9 | ⌥/⌥⇧-only Hotkey scheitert (Fehler -9868) | Auf 15.0/15.1 ⌘ oder ⌃ dazunehmen (gefixt ab 15.2) | §D1 |
| 10 | Overlay verschwindet bei Space/Fullscreen | Hohes Level + `[.canJoinAllSpaces, .stationary]` | §B2 |
| 11 | Overlay ragt aus Monitor / springt raus | Position immer auf `visibleFrame` klemmen (nicht löschen) | §B5 |
| 12 | TCC verweigert nach Info.plist-Edit still | Reihenfolge: bauen → plist final → DANN signieren | §H4 |
| 13 | AVAudioEngine-Tap crasht (Sample-Rate) | Tap-Format `inputNode.outputFormat(forBus:0)` nehmen | §E4 |
| 14 | Aufnahme tot nach AirPods/USB-Wechsel | `AVAudioEngineConfigurationChange` behandeln, Tap neu | §E5 |
| 15 | `codesign --deep` zum Signieren | Top-Bundle ohne `--deep`, Nested einzeln innen→außen | §G6 |

---

## A — NSPanel / NSWindow: Fokus & Aktivierung

### A1. NSPanel wird nie Key-Window → Textfelder im Overlay nicht fokussierbar   [⭐ HAEUFIG]
**Symptom:** Klick in ein Textfeld des Overlays fokussiert nicht, Tastatureingaben kommen nicht an; Caret blinkt nicht.
**Ursache:** Standard-`NSPanel` (erst recht mit `.nonactivatingPanel`) liefert fuer `canBecomeKey`/`canBecomeMain` `false` → das Fenster kann nie key/main werden, kein Responder bekommt Fokus. Per Design.
**Versionen:** per Design, alle macOS-Versionen.
**FIX (funktionserhaltend):** NSPanel subclassen und `canBecomeKey` **und** `canBecomeMain` auf `true` ueberschreiben (beide noetig). Zusaetzlich `isFloatingPanel = true` und `becomesKeyOnlyIfNeeded = true`, damit das Panel nur key wird, wenn ein Textfeld es braucht (behaelt den nonactivating-Charakter). StyleMask `.titled` beibehalten (Titlebar via `titleVisibility = .hidden` + `titlebarAppearsTransparent = true` verstecken), nicht durch `.borderless` ersetzen — Feature bleibt erhalten.
**Quelle:** [Cindori — Floating Panel](https://cindori.com/developer/floating-panel) · [FloatingPanel Gist](https://gist.github.com/jordibruin/8ae7b79a1c0ce2c355139f29990d5702)

### A2. `.nonactivatingPanel` nachtraeglich per `setStyleMask` gesetzt → Panel zeigt Fokus, tippt aber nicht (FB16484811)
**Symptom:** Panel wird visuell als key gezeichnet (Caret blinkt), aber Tippen ins Textfeld funktioniert nicht; inkonsistente Aktivierung.
**Ursache:** NSPanel ruft beim **Init** intern `-_setPreventsActivation:` auf und setzt das CoreGraphics-Window-Tag `kCGSPreventsActivationTagBit`, wenn das nonactivating-Bit gesetzt ist. Wird die StyleMask SPAETER per `setStyleMask:` geaendert, wird `_setPreventsActivation:` NICHT erneut aufgerufen → Tag bleibt veraltet/inkonsistent.
**Versionen:** offener AppKit-Defekt, gemeldet 2026 als FB16484811, nicht gefixt (per Design/SPI-abhaengig).
**FIX (funktionserhaltend):** StyleMask **schon bei der Panel-Erstellung final setzen**, nie zur Laufzeit umschalten. Muss dynamisch umgeschaltet werden: Panel mit korrekter StyleMask **neu erzeugen** statt umzuschalten (sauberster Weg, keine SPI). Notfalls `_setPreventsActivation:` manuell nachziehen (privates SPI — Risiko).
**Quelle:** [philz.blog — NSPanel Nonactivating Style Mask Flag](https://philz.blog/nspanel-nonactivating-style-mask-flag/) · [NonactivatingPanelBug Repo](https://github.com/philptr/NonactivatingPanelBug)

### A3. `.accessory`/LSUIElement-App: Fenster erscheint HINTER anderen Apps
**Symptom:** App mit `setActivationPolicy(.accessory)` bzw. `LSUIElement=YES`; Overlay/Settings-Fenster oeffnet hinter der aktiven App, taucht nicht im App-Switcher auf, bekommt keine Key-Events.
**Ursache:** Per Design — NSApplication behandelt Accessory/Menubar-Apps als Hintergrund-Utilities; das beeinflusst Window-Ordering und Event-Routing. Ohne Dock-Icon honoriert macOS `makeKeyAndOrderFront` nicht zuverlaessig.
**Versionen:** per Design; verschaerft macOS 14+ (Sonoma), besonders 15.x.
**FIX (funktionserhaltend):** Vor dem Anzeigen kurz `setActivationPolicy(.regular)` → `NSApp.activate()` → Fenster zeigen; nach Schliessen optional zurueck auf `.accessory`. Fuer bewusst nonactivating bleibende Overlays stattdessen den Panel-Ansatz (A1) nutzen. Policy-Wechsel wirkt nicht immer sofort — ggf. kleinen Delay einplanen.
**Quelle:** [Apple — setActivationPolicy](https://developer.apple.com/documentation/appkit/nsapplication/setactivationpolicy(_:)) · [steipete.me — Settings from Menu Bar Items](https://steipete.me/posts/2025/showing-settings-from-macos-menu-bar-items) · [Apple Forums 650270](https://developer.apple.com/forums/thread/650270)

### A4. `NSApp.activate(ignoringOtherApps:)` deprecated ab macOS 14 (Parameter wirkungslos)   [⭐ HAEUFIG]
**Symptom:** `NSApp.activate(ignoringOtherApps: true)` aktiviert die App (besonders Accessory-Apps) nicht mehr zuverlaessig; Fenster wird sichtbar, App kommt aber nicht in den Vordergrund — Nutzer muss erst ins Fenster klicken.
**Ursache:** Der Parameter `ignoringOtherApps` ist **ab macOS 14 (Sonoma) deprecated und wirkungslos**. Apple hat auf „cooperative activation" umgestellt — Aktivierung ist nur noch eine *Anfrage*, die aktive App muss kooperieren.
**Versionen:** deprecated ab macOS 14 (SDK 14); davor funktional. Bestaetigt bis Sequoia 15.x. (SwiftLint-Regel-Request `realm/SwiftLint#2643` OPEN — bestaetigt die Deprecation.)
**FIX (funktionserhaltend):** Parameterloses `activate()` verwenden. Fuer Aktivierungs-Uebergabe von einer anderen App das Cooperative-Activation-Pattern nutzen (`yieldActivation` von der aufrufenden App via `NSRunningApplication`). Pragmatischer Workaround fuer Accessory-Apps: kurz `setActivationPolicy(.regular)` → Delay → `activate()` + Fenster zeigen (siehe A3).
**Quelle:** [WWDC23 — What's new in AppKit](https://developer.apple.com/videos/play/wwdc2023/10054/) · [Apple Forums 739075](https://developer.apple.com/forums/thread/739075) · [Apple — activate(ignoringOtherApps:)](https://developer.apple.com/documentation/appkit/nsapplication/activate(ignoringotherapps:))

### A5. `setStyleMask` nach Init aktualisiert Aktivierungsverhalten generell nicht
**Symptom:** Jede nachtraegliche StyleMask-Aenderung an einem Panel fuehrt zu schwer diagnostizierbarer, inkonsistenter Aktivierung (Spezialfall siehe A2).
**Ursache:** Wie A2 — interne Aktivierungs-Tags werden nur im Init gesetzt.
**Versionen:** per Design.
**FIX (funktionserhaltend):** StyleMask immutabel behandeln; bei Bedarf Panel neu erzeugen.
**Quelle:** [philz.blog](https://philz.blog/nspanel-nonactivating-style-mask-flag/)

---

## B — Window-Level, Spaces & Fullscreen (Overlay verschwindet)

### B1. Falsche NSWindow.Level-Konstante → Overlay zu niedrig (verschwindet) oder zu hoch (deckt Menue ab)
**Symptom:** Overlay liegt hinter Statusleiste/Benachrichtigungen ODER deckt umgekehrt das Hauptmenue ab.
**Ursache:** Verwechslung der Level-Werte. Numerisch: `mainMenu` = 24, `statusBar` = 25, `screenSaver` = 101, `CGWindowLevelForKey(.overlayWindow)` = 102. Hoeheres Level liegt immer vorne und ueberschreibt die User-Fenster-Ordnung.
**Versionen:** per Design, alle Versionen.
**FIX (funktionserhaltend):** Level bewusst waehlen — „ueber normalen Fenstern, aber unter System-Overlays" = `.floating`/`.statusBar`; „ueber Screensaver/Fullscreen" = Richtung 101/102. Nicht blind `.screenSaver` nehmen, wenn man nur ueber normale Fenster will.
**Quelle:** [Jim Fisher — Order of NSWindow levels](https://jameshfisher.com/2020/08/03/what-is-the-order-of-nswindow-levels/) · [Apple — NSWindow.Level](https://developer.apple.com/documentation/appkit/nswindow/level)

### B2. `.fullScreenAuxiliary` legt das Overlay NICHT ueber Fullscreen-Apps
**Symptom:** Overlay verschwindet beim Wechsel in eine Fullscreen-App / liegt nicht ueber dem Fullscreen-Fenster, obwohl `collectionBehavior` gesetzt ist.
**Ursache:** `.fullScreenAuxiliary` bedeutet „zeige auf demselben Space wie das Fullscreen-Fenster" — explizit NICHT „liege drueber". `canJoinAllSpaces + fullScreenAuxiliary` zeigt auf allen Spaces, aber nicht ueber der Fullscreen-App.
**Versionen:** per Design; in Foren bestaetigt.
**FIX (funktionserhaltend):** Fuer „ueber Fullscreen schweben" hohes Window-Level (`.screenSaver`-Bereich, ~101) + `collectionBehavior = [.canJoinAllSpaces, .stationary]`. *Exakte beste Kombination ist geraeteabhaengig — empirisch verifizieren.* collectionBehavior allein garantiert die Z-Order nicht; Level entscheidet.
**Quelle:** [Apple — fullScreenAuxiliary](https://developer.apple.com/documentation/appkit/nswindow/collectionbehavior-swift.struct/fullscreenauxiliary) · [Apple Forums 26677](https://developer.apple.com/forums/thread/26677)

### B3. Overlay nur auf einem Space sichtbar / verschwindet bei Space-Wechsel
**Symptom:** Overlay ist nur auf dem Space sichtbar, auf dem es erstellt wurde; weg nach Space-Wechsel oder bewegt sich bei Mission Control mit.
**Ursache:** Fehlendes/falsches `collectionBehavior`. Ein Standard-Fenster folgt nicht ueber Spaces.
**Versionen:** per Design, alle Versionen.
**FIX (funktionserhaltend):** `window.collectionBehavior = [.canJoinAllSpaces, .stationary]` (`.canJoinAllSpaces` = ueberall sichtbar, `.stationary` = bleibt bei Mission Control/Expose ortsfest). Soll es nur dem aktiven Space folgen: `.moveToActiveSpace` statt `.canJoinAllSpaces`.
**Quelle:** [Apple — NSWindow.CollectionBehavior](https://developer.apple.com/documentation/appkit/nswindow/collectionbehavior-swift.struct) · [Apple Forums 26677](https://developer.apple.com/forums/thread/26677)

### B4. Stage-Manager-/neuere-macOS-Regressionen bei Floating-Panels — UNVERIFIZIERT
**Symptom (Hypothese):** Floating-Panel verhaelt sich bei aktivem Stage Manager unter Sequoia 15 anders/verschwindet.
**Status:** **Keine belastbare Quelle gefunden — ehrlich als unverifiziert markiert.** Nicht als Fakt behandeln. Bei Stage-Manager-Problemen zuerst `collectionBehavior` (B2/B3) und Level (B1) pruefen.
**Quelle:** —

### B5. Gespeicherte Position klemmt nicht → Overlay ragt im Horizontal-Modus aus dem Monitor (verschwindet)   [⭐ erlebt 2026-06-05]
**Symptom:** Overlay kurz sichtbar, dann „verschwindet rechts vom Monitor" — im Horizontal-Modus nur noch zu einem Drittel am rechten Rand sichtbar, nach dem Einklappen (Auto-Hide) ganz weg. Prozess laeuft weiter (kein Crash).
**Ursache:** Eine im schmalen Vertikal-Modus (z.B. 96pt) gespeicherte X-Position klebt am rechten Rand. Beim Wechsel in den breiten Horizontal-Modus (mehrere hundert pt breit) wird dieselbe Origin-X uebernommen — das Overlay ragt rechts raus. Die Positionierung (`applyHorizontalLayout`/`applyVerticalLayout`/`applyCollapsedLayout` + Init) rief `setFrame` OHNE Klemmung auf den `visibleFrame`. Verschaerft auf Retina: logische Breite (z.B. 2560px → 1280pt) macht eine „1159"-Position fast randbuendig. Die eingeklappte Mic-Pille wird auf die vorherige Mic-Mitte zentriert → liegt dann komplett ausserhalb.
**Versionen:** eigener Code, alle macOS-Versionen (Geometrie-Logik).
**FIX (funktionserhaltend):** Einen Helper `clampFrameToVisibleScreen(_:)` (waehlt Bildschirm mit groesstem Ueberlapp, verschiebt das Frame zurueck in den `visibleFrame` — NUR Position, NIE Groesse) an JEDER Positionierungs-Stelle anwenden: Init, vertikal, horizontal, collapsed (Defense in Depth). Layout und alle Funktionen bleiben unveraendert, das Overlay bleibt nur garantiert sichtbar. NICHT die gespeicherte Position blind loeschen (das verliert die Nutzer-Praeferenz) — klemmen genuegt.
**Quelle:** eigener Vorfall (TerminalVoiceOverlay-macOS, Commit #41573).

### B6. Solo-Dock-Drag berechnet Pillar-Position aus versteckter Board-Position → Pillar springt raus   [erlebt 2026-06-05]
**Symptom:** Im Solo-Dock-Modus (Promptboard versteckt, Eingabe haengt direkt am Pillar) verschiebt das Ziehen des Eingabe-Fensters den ganzen Pillar nach rechts aus dem Bildschirm. Erst nach Aus-/Wieder-Einblenden geht es korrekt.
**Ursache:** Der Eingabe-Drag ruft `translateGroup` des Promptboards, das die Pillar-Position aus der **Board-Position** ableitet (`pillar.x = board.x + board.width + 4`). Im Solo-Dock ist das Board `orderOut` und steht an veralteter/falscher Position → der Pillar wird falsch gerechnet und springt weg. Zusaetzlich ist die Drag-Geometrie inkonsistent (Board 380pt vs. Eingabe 760pt breit).
**Versionen:** eigener Code, alle macOS-Versionen.
**FIX (funktionserhaltend):** Im Solo-Dock eigener Drag-Pfad: `PromptBoardPanel.soloDockDragHandler` (vom AppDelegate gesetzt) verschiebt den **Pillar DIREKT** um das Delta, klemmt ihn mit `clampFrameToVisibleScreen` (B5) und zieht die Eingabe per `dockToOverlay` nach. An BEIDEN Solo-Dock-Einstiegen per gemeinsamem Helper installieren; beim Verlassen/Schliessen wieder nil. NICHT die Board-Position als Anker nehmen, solange das Board versteckt ist.
**Quelle:** eigener Vorfall (TerminalVoiceOverlay-macOS, Commit #41577).

### B7. macOS 26 „Tahoe" + Liquid Glass — was fuer die Overlays zu beachten ist (Re-Recherche 2026-07-02)
**Kontext:** macOS **26 „Tahoe"** ist erschienen (neues **Liquid-Glass**-Design: transluzentes Material, dynamische
Anpassung an Hell/Dunkel, **vollstaendig transparente Menueleiste**). Betrifft primaer Standard-Fenster/Toolbars/Sidebars
(neue APIs `NSToolbarItem.isBordered/.style(.prominent)/.backgroundTintColor`, `NSItemBadge`, `NSSplitViewController`
Sidebar/Inspector), NICHT direkt borderless `.nonactivatingPanel`-Overlays.
**Fuer Franks Overlays (funktionserhaltend):** Die bewaehrte NSPanel-Config (`.nonactivatingPanel` + `.canJoinAllSpaces`,
§A/§B) gilt **unveraendert** weiter. Zwei Awareness-Punkte:
- **Transparente Menueleiste (Tahoe):** ein Top-of-Screen-Overlay kann optisch anders mit der Menueleiste
  interagieren — auf Tahoe visuell testen (Position/Level), kein Code-Zwang.
- **Beta-Beobachtung (macOS 26 Beta 4, Juli 2025, unbestaetigt fuer Final):** Ein SwiftUI-`NSWindow`-Overlay zeigte
  `close`-Crash, wirkungsloses `setFrameOrigin`/`orderOut`, und `canBecomeKey/Main = true` → Crash ~3 s nach Anzeige.
  **Beim ersten Tahoe-Test gezielt pruefen**; falls reproduzierbar auf Final → hier als echter Bug nachtragen.
- **App-Icon:** Tahoe bringt das neue **`.icon`-Format** (Icon Composer, `CFBundleIconName`) statt `.icns` —
  Details im Almanach `assets/icon-building.md`; `.icns` fuer Abwaertskompatibilitaet behalten.
**Versionen:** macOS 26 (Tahoe). **Quelle:** Apple Newsroom „new software design" (Liquid Glass), WWDC25 Session 310
„Build an AppKit app with the new design", Medium/Itsuki (Beta-4-Overlay-Bugs, 2025-07-29).

---

## C — Accessibility-Permission (AX-API / TCC)

> Hinweis: Der „Permission verschwindet nach Rebuild"-Kern-Bug ist in **Sektion H** konsolidiert
> (er betrifft Accessibility, Mikrofon und alle TCC-Dienste gleichzeitig).

### C1. `AXIsProcessTrusted()` bleibt `false` nach Aktivierung (kein Live-Update)   [⭐ HAEUFIG]
**Symptom:** Nutzer aktiviert die App in Systemeinstellungen → Bedienungshilfen, aber `AXIsProcessTrusted()` liefert im laufenden Prozess weiter `false`; globale Monitore starten nicht.
**Ursache:** Der TCC-Trust wird beim **Prozessstart** ermittelt, nicht live aktualisiert. Lehnt der Nutzer „Beenden & neu oeffnen" ab, behaelt der Prozess das alte (negative) Ergebnis.
**Versionen:** per Design, alle (macOS 13–15+).
**FIX (funktionserhaltend):** App nach dem Erteilen **neu starten** lassen ODER `AXIsProcessTrusted()` per Timer **pollen** und beim Wechsel auf `true` die Monitore (nach)starten. Zusaetzlich `CGEventTap`-Erstellung als realeren Trust-Check nutzen (gibt `nil` zurueck, wenn nicht wirklich vertraut).
**Quelle:** [jano.dev — Accessibility Permission](https://jano.dev/apple/macos/swift/2025/01/08/Accessibility-Permission.html) · [Apple Forums 727984](https://developer.apple.com/forums/thread/727984)

### C2. `kAXTrustedCheckOptionPrompt`-Dialog erscheint nicht
**Symptom:** `AXIsProcessTrustedWithOptions` mit `kAXTrustedCheckOptionPrompt: true` zeigt keinen System-Dialog.
**Ursache:** Zwei Faelle: (a) **App-Sandbox aktiv** → Accessibility-Prompt grundsaetzlich unterdrueckt (Accessibility ist mit Sandbox inkompatibel, siehe F1). (b) macOS hat die Entscheidung schon einmal „gemerkt" → Prompt erscheint nur einmal.
**Versionen:** per Design, alle.
**FIX (funktionserhaltend):** App-Sandbox deaktiviert lassen (passt zum unsandboxed Projektkontext). Korrekt:
```swift
let opts: NSDictionary = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: true]
let trusted = AXIsProcessTrustedWithOptions(opts)
```
Wenn der Prompt schon „verbraucht" ist: Nutzer per URL direkt ins Settings-Pane leiten (C3) statt auf den Prompt zu warten; bei festsitzendem Zustand `tccutil reset Accessibility <bundle-id>`.
**Quelle:** [Apple — AXIsProcessTrustedWithOptions](https://developer.apple.com/documentation/applicationservices/1459186-axisprocesstrustedwithoptions) · [Gertrude](https://gertrude.app/blog/macos-request-accessibility-control)

### C3. Settings-Pane-URL hat sich mit Ventura geaendert (oeffnet falsches/kein Pane)
**Symptom:** Code zum Oeffnen des Accessibility-Panes oeffnet das falsche Fenster oder nichts; schnelles Toggeln liefert zufaellig falsche Trust-Werte.
**Ursache:** Ventura (13) hat die Systemeinstellungen komplett umgebaut; altes URL-Schema und Toggle-Verhalten aenderten sich.
**Versionen:** ab Ventura 13.
**FIX (funktionserhaltend):** Ventura+-URL nutzen: `x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility`. Nicht auf schnelle Toggle-Ergebnisse verlassen — nach Aenderung App-Restart/Polling (C1).
**Quelle:** [Apple Forums 727984](https://developer.apple.com/forums/thread/727984) · [alanwsmith.com](https://www.alanwsmith.com/en/2p/5a/aw/ok/)

### C4. Korrupte TCC.db: Checkbox sitzt, ist aber wirkungslos
**Symptom:** Checkbox in Bedienungshilfen ist gesetzt, App funktioniert trotzdem nicht; An-/Abhaken aendert nichts.
**Ursache:** Korrupte TCC-Privacy-Datenbank — UI-Zustand und tatsaechlicher Grant divergieren.
**Versionen:** Big Sur+, alle relevanten.
**FIX (funktionserhaltend):** Terminal → `sudo tccutil reset Accessibility <bundle-id>` (ohne ID = alle) → **Reboot** → Eintrag neu setzen. Reboot allein behebt viele Faelle.
**Quelle:** [Macworld — Fix Accessibility permission](https://www.macworld.com/article/347452/how-to-fix-macos-accessibility-permission-when-an-app-cant-be-enabled.html) · [Michael Tsai — Resetting TCC](https://mjtsai.com/blog/2023/02/09/resetting-tcc/)

---

## D — Globale Hotkeys, Event-Taps & Secure Input

### D1. Sequoia 15.0/15.1 blockiert ⌥-only und ⌥⇧-only Hotkeys (RegisterEventHotKey)
**Symptom:** `RegisterEventHotKey` mit *nur* Option (⌥) oder Option+Shift (⌥⇧) als Modifier scheitert mit Fehler **-9868 (`eventInternalErr`)** — egal welche Taste; Hotkey feuert nie.
**Ursache:** Bewusste Aenderung in macOS Sequoia gegen Keylogger (⌥⇧ erzeugt Sonderzeichen in Passwoertern). Apple-Engineer: „macOS Sequoia now requires that a hotkey registration use at least one modifier that is not shift or option."
**Versionen:** eingefuehrt **macOS 15.0/15.1**, **gefixt ab 15.2 Beta 2** (Build 24C5073e) — dort wieder erlaubt. (Apple FB15168205 / `feedback-assistant/reports#552` **CLOSED/COMPLETED** 2024-12-13.)
**FIX (funktionserhaltend):** Mindestens einen Modifier verlangen, der NICHT Shift/Option ist (⌘ oder ⌃ dazunehmen) — funktioniert auf allen 15.x. Hotkey NICHT entfernen, nur die Modifier-Kombi anpassen. Auf 15.2+ ist auch ⌥-only wieder ok.
**Quelle:** [feedback-assistant/reports#552](https://github.com/feedback-assistant/reports/issues/552) · [Apple Forums 763878](https://developer.apple.com/forums/thread/763878)

### D2. Carbon `RegisterEventHotKey` ist deprecated, funktioniert aber weiter
**Symptom:** Deprecation-Warnungen; Sorge, ob es auf Apple Silicon / neuem macOS noch laeuft.
**Ursache:** Seit macOS 10.8 deprecated, aber nie entfernt.
**Versionen:** deprecated, weiter nutzbar; stabil auf Apple Silicon, Sonoma, Sequoia (mit D1-Einschraenkung).
**FIX (funktionserhaltend):** Weiter nutzen — grosser Vorteil: **braucht keine TCC-Permission** (anders als NSEvent/CGEventTap). Auf `kEventHotKeyPressed` lauschen (nicht `Released`, sonst feuert es beim Loslassen). Keine Migration noetig.
**Quelle:** [Apple Forums 735223](https://developer.apple.com/forums/thread/735223) · [CocoaDev RegisterEventHotKey](https://cocoadev.github.io/RegisterEventHotKey/)

### D3. Hotkey schon vom System/anderer App belegt → feuert nie
**Symptom:** Registrierung scheint OK, aber Hotkey feuert nie (oder erst, wenn die andere App beendet ist).
**Ursache:** System-Shortcuts und andere `RegisterEventHotKey`-Clients haben Vorrang; bei Konflikt gewinnt der erste/das System.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Konfliktarme Kombis waehlen; bei Nicht-Feuern System Settings → Keyboard → Shortcuts auf Konflikt pruefen. Funktion bleibt — nur Tastenkombi tauschen.
**Quelle:** [CocoaDev RegisterEventHotKey](https://cocoadev.github.io/RegisterEventHotKey/)

### D4. CGEventTap wird vom System still deaktiviert (`kCGEventTapDisabledByTimeout`)   [⭐ HAEUFIG]
**Symptom:** Hotkey/Tastaturueberwachung funktioniert nach Start, hoert nach Stunden / nach Sleep-Wake **dauerhaft** auf zu feuern. App-Neustart hilft temporaer.
**Ursache:** macOS deaktiviert den Tap, wenn der Callback zu langsam ist (`kCGEventTapDisabledByTimeout`) oder bei zu viel User-Input (`kCGEventTapDisabledByUserInput`), und sendet ein Notification-Event in den Callback. Wird das nicht behandelt, bleibt der Tap tot.
**Versionen:** per Design, alle Versionen. (`ghostty#11883` „global keybind stops working" wurde **CLOSED/NOT_PLANNED** — der macOS-Mechanismus bleibt, der Workaround ist Pflicht.)
**FIX (funktionserhaltend):** Im Callback abfangen und neu aktivieren:
```swift
if type == .tapDisabledByTimeout || type == .tapDisabledByUserInput {
    CGEvent.tapEnable(tap: tap, enable: true); return nil
}
```
Zusaetzlich Sicherheitsnetz: Timer (~alle 5 s) prueft `tapIsEnabled` und re-enabled / installiert den Tap neu. Callback schlank halten (Timeout vermeiden).
**Quelle:** [Apple — tapDisabledByTimeout](https://developer.apple.com/documentation/coregraphics/cgeventtype/tapdisabledbytimeout) · [Ghostty #11883](https://github.com/ghostty-org/ghostty/issues/11883)

### D5. CGEventTap braucht „Input Monitoring", NICHT „Accessibility" (falsche Kategorie freigegeben)
**Symptom:** Tap „installiert", `tapIsEnabled` anfangs true, aber Callbacks feuern nie — sieht aus wie ein Code-Signing-Problem.
**Ursache:** Zwei getrennte TCC-Kategorien. Faustregel: **`NSEvent`-Global-Monitor → Accessibility**; **`CGEventTap` / IOHIDManager → Input Monitoring**. Carbon `RegisterEventHotKey` → keine Permission.
**Versionen:** Input-Monitoring-Kategorie ab macOS 10.15.
**FIX (funktionserhaltend):** Vor `tapCreate`: `if !CGPreflightListenEventAccess() { CGRequestListenEventAccess() }` (offizielle Pruef-/Anfrage-API fuer Input Monitoring). Passende Kategorie zur genutzten API anfordern — oder beide, wenn beide APIs verwendet werden.
**Quelle:** [AeroSpace #1012](https://github.com/nikitabobko/AeroSpace/issues/1012) (OPEN) · ["All about macOS event observation"](https://docs.google.com/presentation/d/1nEaiPUduh1vjks0rDVRTcJaEULbSWWh1tVdG2HF_XSU/htmlpresent)

### D6. Code-Signing-Race: Event-Tap erstellt, Callbacks feuern nie (Launch via `open`/Finder)
**Symptom:** `CGEvent.tapCreate()` gibt non-nil, `tapIsEnabled()` true — aber Callbacks feuern nie. Direkte Binary-Ausfuehrung funktioniert, Start via `open`/Finder/Dock nicht.
**Ursache:** Code-Identity-Wechsel durch Re-Signing loest eine TCC-Reevaluierung aus, die bei Launch via Launch Services unzuverlaessig greift — besonders bei `swiftc`-gebauten, ad-hoc/unsignierten Binaries.
**Versionen:** nicht praezise datiert (Quelle 2026-02) — als plausibel, nicht hart verifiziert markiert.
**FIX (funktionserhaltend):** Stabile Signatur/Bundle-ID; Health-Timer (`tapIsEnabled()` alle 5 s) + Tap bei Bedarf neu installieren (siehe D4). Waehrend Entwicklung Binary direkt starten.
**Quelle:** [danielraffel.me — CGEvent Taps and Code Signing](https://danielraffel.me/til/2026/02/19/cgevent-taps-and-code-signing-the-silent-disable-race/)

### D7. Secure Keyboard Entry (`EnableSecureEventInput`) blockiert ALLE globalen Observer
**Symptom:** Hotkeys/Tastaturueberwachung funktionieren normal, hoeren aber abrupt auf, sobald ein Passwortfeld (Login, `sudo` im Terminal, manche Apps) aktiv ist — und kommen manchmal nicht zurueck, wenn eine App Secure Input nicht sauber ausschaltet.
**Ursache:** Secure Keyboard Entry: weder `CGEventTap` noch `NSEvent.addGlobalMonitorForEvents` erhalten dann Events (Anti-Keylogger-Feature).
**Versionen:** per Design, alle Versionen.
**FIX (funktionserhaltend):** Nicht umgehbar (ohne Root/Kext). App sollte erkennen, dass Secure Input aktiv ist, und dem Nutzer eine Anzeige zeigen („Hotkeys derzeit durch Secure Input blockiert"). Bei haengendem Zustand die verursachende App identifizieren/beenden. Keine Funktion entfernen — nur informieren.
**Quelle:** [AeroSpace #1486](https://github.com/nikitabobko/AeroSpace/issues/1486) (CLOSED/COMPLETED — UI-Indikator ergaenzt) · ["All about macOS event observation"](https://docs.google.com/presentation/d/1nEaiPUduh1vjks0rDVRTcJaEULbSWWh1tVdG2HF_XSU/htmlpresent)

### D8. `NSEvent.addGlobalMonitorForEvents` liefert keine Key-Events ohne Accessibility
**Symptom:** Global Monitor mit `.keyDown` feuert nie.
**Ursache:** Der Global Monitor darf Key-Events nur beobachten, wenn die App fuer Accessibility freigegeben/trusted ist.
**Versionen:** per Design.
**FIX (funktionserhaltend):** `AXIsProcessTrustedWithOptions` mit `kAXTrustedCheckOptionPrompt` aufrufen; erst nach Trust den Monitor installieren. Nach Permission-Aenderung App neu starten (C1).
**Quelle:** [Apple — Monitoring Events](https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/EventOverview/MonitoringEvents/MonitoringEvents.html) · [Gertrude](https://gertrude.app/blog/macos-request-accessibility-control)

### D9. Global Monitor sieht NIE Events der eigenen App (+ kann nicht modifizieren)
**Symptom:** Hotkey feuert in anderen Apps, aber nicht, wenn die eigene App im Vordergrund ist.
**Ursache:** `addGlobalMonitorForEvents` beobachtet nur Events *anderer* Prozesse; eigene Events kommen nicht durch. Der Global Monitor kann Events zudem nur beobachten, nicht *modifizieren/schlucken*.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Zusaetzlich `addLocalMonitorForEvents` fuer die eigene App registrieren (beide parallel). Der Local Monitor kann Events auch schlucken (nil zurueckgeben).
**Quelle:** ["All about macOS event observation"](https://docs.google.com/presentation/d/1nEaiPUduh1vjks0rDVRTcJaEULbSWWh1tVdG2HF_XSU/htmlpresent)

---

## E — Mikrofon / AVFoundation

### E1. Sofort-Crash wegen fehlender `NSMicrophoneUsageDescription`   [⭐ HAEUFIG]
**Symptom:** App stuerzt beim ersten Mikrofonzugriff sofort ab. Konsole: *„The app's Info.plist must contain an NSMicrophoneUsageDescription key."*
**Ursache:** Kein `NSMicrophoneUsageDescription`-Key in der Info.plist — besonders bei `swiftc`-Bundles, die oft keine korrekte Info.plist haben.
**Versionen:** per Design seit macOS 10.14, bis heute (15+) gueltig.
**FIX (funktionserhaltend):** `NSMicrophoneUsageDescription` mit echtem Begruendungstext in die Info.plist (bzw. ins eingebettete `__info_plist`-Segment, siehe E2). Fuer Accessibility/Input-Monitoring gibt es keinen Plist-Key — der Nutzer gibt das in den Systemeinstellungen frei.
**Quelle:** [Apple — NSMicrophoneUsageDescription](https://developer.apple.com/documentation/BundleResources/Information-Property-List/NSMicrophoneUsageDescription) · [claude-code #33023](https://github.com/anthropics/claude-code/issues/33023) (CLOSED/COMPLETED)

### E2. `swiftc`-Binary ohne eingebettete Info.plist → kein Prompt, `.notDetermined` haengt
**Symptom:** `AVCaptureDevice.requestAccess(for: .audio)` loest keinen Dialog aus; Status bleibt dauerhaft `.notDetermined`. Kein Crash, aber nie Zugriff.
**Ursache:** Eine reine `swiftc`-Binary ohne sichtbare Info.plist und ohne Bundle-Identifier kann TCC nicht identifizieren → kein Prompt.
**Versionen:** per Design (TCC-Identifikation ueber Bundle-ID); macOS 13+.
**FIX (funktionserhaltend):** Info.plist mit `CFBundleIdentifier`, `CFBundleName` **und** `NSMicrophoneUsageDescription` ins Binary einbetten:
`swiftc … -Xlinker -sectcreate -Xlinker __TEXT -Xlinker __info_plist -Xlinker Info.plist`
Alternativ eine korrekte `.app`-Bundle-Struktur erstellen (K1). Die `build.sh`-Projekte bauen ohnehin ein Bundle — sicherstellen, dass die Info.plist die Keys enthaelt.
**Quelle:** [Tewha — Info.plist for command line tools](https://tewha.net/2015/03/info-plist-for-command-line-tools/) · [Apple Forums 111100](https://developer.apple.com/forums/thread/111100)

### E3. Hardened-Runtime-Signing: Mic-Prompt verschwindet wegen XML-Kommentaren in den Entitlements
**Symptom:** Unsigned erscheint der Mic-Prompt korrekt; nach Signing mit Hardened Runtime erscheint kein Prompt mehr.
**Ursache:** XML-Kommentare (`<!-- … -->`) in der `.entitlements`-Datei verhindern, dass `codesign` die Entitlements korrekt einbettet — `com.apple.security.device.microphone`/`…audio-input` wird verschluckt.
**Versionen:** durch Apple DTS bestaetigt; genaue macOS-Version unklar (*unsicher*), betrifft generell Hardened-Runtime-Signing.
**FIX (funktionserhaltend):** Alle Kommentare aus den Entitlements entfernen; Mic-Entitlement setzen; nach dem Signing pruefen: `codesign -d --entitlements - /path/App.app`.
**Quelle:** [Apple Forums 741303](https://developer.apple.com/forums/thread/741303)

### E4. AVAudioEngine-Tap-Crash bei Sample-Rate-Mismatch
**Symptom:** Crash beim `installTap` auf `inputNode`: *„required condition is false: format.sampleRate == inputHWFormat.sampleRate"*.
**Ursache:** Eigenes Format (z.B. 44.1 kHz) erzwungen, waehrend neuere Macs intern 48 kHz Default-Hardware-Rate haben.
**Versionen:** hardware-abhaengig/per Design; bekannt seit AVAudioEngine-Einfuehrung, weiterhin aktuell. (`AudioKit#1851` CLOSED/COMPLETED — in der Lib umgangen; eigener Code muss es selbst beachten.)
**FIX (funktionserhaltend):** Beim Tap **immer** das tatsaechliche Node-Format verwenden:
```swift
let fmt = inputNode.outputFormat(forBus: 0)
inputNode.installTap(onBus: 0, bufferSize: 4096, format: fmt) { … }
```
Fuer eine Ziel-Sample-Rate `AVAudioConverter` nutzen, statt das Tap-Format zu erzwingen.
**Quelle:** [Apple Forums 680785](https://developer.apple.com/forums/thread/680785) · [AudioKit #1851](https://github.com/AudioKit/AudioKit/issues/1851)

### E5. Geraetewechsel (USB/Bluetooth/AirPods) → stummer Stream / Engine stoppt
**Symptom:** Aufnahme laeuft an, aber kein Ton; oder die Engine stoppt, wenn das Default-Input-Geraet wechselt (AirPods verbinden, USB-Interface ein-/ausstecken).
**Ursache:** AVAudioEngine ist auf macOS ans **System-Default-Input** gebunden. Beim Route-Change aendert sich das Input-Format → bestehender Tap/Engine wird ungueltig.
**Versionen:** per Design (single-default-device-Limit); durchgaengig. (`AudioKit#2130` CLOSED/COMPLETED — Lib-Workaround.)
**FIX (funktionserhaltend):** `AVAudioEngineConfigurationChange`-Notification abonnieren → Engine stoppen, Tap entfernen, Format neu lesen (`outputFormat(forBus:0)`), Tap neu installieren, Engine neu starten. Fuer feste Geraetewahl `AudioUnitSetProperty(kAudioOutputUnitProperty_CurrentDevice)` auf `inputNode.audioUnit`.
**Quelle:** [Apple Forums 71008](https://developer.apple.com/forums/thread/71008) · [AudioKit #2130](https://github.com/AudioKit/AudioKit/issues/2130)

### E6. LSUIElement/`.accessory`-App ohne Fenster bekommt trotzdem Mic-Zugriff (kein Bug — Klarstellung)
**Symptom:** Unsicherheit, ob eine Agent-App ohne sichtbares Fenster ueberhaupt einen Mic-Prompt zeigen kann.
**Ursache:** Kein Bug — der Prompt haengt nur davon ab, ob `AVCaptureDevice.requestAccess(for: .audio)` aufgerufen wird, nicht ob ein Fenster existiert.
**Versionen:** per Design, macOS 13+.
**FIX:** `authorizationStatus(for: .audio)` pruefen, bei `.notDetermined` `requestAccess` aufrufen — der Dialog erscheint auch ohne Fenster. Voraussetzung: korrekte Info.plist (E1/E2).
**Quelle:** [Apple — requestAccess](https://developer.apple.com/documentation/avfoundation/avcapturedevice/1624584-requestaccess) · [Apple Forums 738986](https://developer.apple.com/forums/thread/738986)

### E7. `Thread.sleep` in `stop()` als Tap-Callback-Barriere (unsicher + unnoetig)
**Symptom:** Beim Stoppen der Aufnahme ein fixes `Thread.sleep(forTimeInterval: 0.05)`, um in-flight AVAudioEngine-Tap-Callbacks „abzuwarten". Race-anfaellig (50 ms sind nicht garantiert ausreichend) und blockiert den aufrufenden Thread.
**Ursache:** Annahme, der Tap koenne nach `engine.stop()` noch feuern. Tatsaechlich kommen nach `removeTap(onBus:)` keine neuen Callbacks mehr — der Sleep ist sowohl unsicher als auch ueberfluessig.
**Versionen:** Code-Smell / per Design, alle Versionen. (Im aktuellen `AudioRecorder.stop()` beider Projekte vorhanden.)
**FIX (funktionserhaltend):** Erst `removeTap(onBus: 0)` (danach feuert kein Callback mehr), dann `engine.stop()`, dann den letzten gepufferten Stand ueber eine Serial-Queue/`withCheckedContinuation` finalisieren statt zu schlafen. Geteilten Tap-Zustand ueber Serial-Dispatch-Queue oder Actor schuetzen, nicht ad-hoc `NSLock`. Siehe Best-Practices Abschnitt F.5.
**Quelle:** [Apple — removeTap](https://developer.apple.com/documentation/avfaudio/avaudionode/removetap(onbus:)) · best-practices/desktop/swift-appkit.md (F.5)

---

## F — App-Sandbox (falls je aktiviert)

> Die Projekte sind aktuell **unsandboxed** (Direktverteilung, kein App Store). Diese Bugs greifen nur,
> falls jemand die Sandbox aktiviert — dann aber sofort und hart. Faustregel: Fuer eine Overlay-App mit
> globalen Monitoren/Hotkeys **Sandbox NICHT aktivieren** (sonst Funktionsverlust durch F1).

### F1. Accessibility ist in der Sandbox UNMOEGLICH
**Symptom:** Permission-Prompt erscheint nie, App nicht manuell hinzufuegbar, `AXIsProcessTrusted()` immer `false`.
**Ursache:** Die App-Sandbox blockt die Accessibility-APIs grundsaetzlich — es gibt kein Entitlement, das das aufhebt.
**Versionen:** per Design, alle.
**FIX (funktionserhaltend):** Sandbox nicht aktivieren (Direktverteilung erlaubt das). Wenn Sandbox zwingend: globale Tastatur ueber `CGEventTap` mit `listenOnly` + **Input Monitoring** (`CGPreflightListenEventAccess`) statt `NSEvent`-Global-Monitor — das ist sandbox-/MAS-kompatibel. Bei Input-Monitoring bleibt `AXIsProcessTrusted` `false` — nicht als Pruefung verwenden.
**Quelle:** [Apple Forums 789896](https://developer.apple.com/forums/thread/789896) · [AeroSpace #1012](https://github.com/nikitabobko/AeroSpace/issues/1012)

### F2. Mikrofon in Sandbox: falsches/fehlendes Entitlement → TCC-Denial
**Symptom:** Log: `kTCCServiceMicrophone requires entitlement com.apple.security.device.audio-input but it is missing`.
**Ursache:** In der Sandbox muss Mic explizit freigegeben sein. Nicht-MAS/notarisiert: `com.apple.security.device.audio-input`; MAS: `com.apple.security.device.microphone`. Plus `NSMicrophoneUsageDescription`.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Bei Direktverteilung `com.apple.security.device.audio-input = true` + Usage-Description. Im Zweifel beide Mic-Entitlements setzen.
**Quelle:** [Apple — audio-input](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.security.device.audio-input) · [Apple — microphone](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.security.device.microphone)

### F3. Netzwerk-Client in Sandbox stumm blockiert
**Symptom:** Alle ausgehenden Verbindungen schlagen fehl, sobald Sandbox aktiv ist.
**Ursache:** Sandbox blockt Netzwerk ohne `com.apple.security.network.client`.
**Versionen:** per Design.
**FIX (funktionserhaltend):** `com.apple.security.network.client = true` (bzw. `.server`) setzen. (Relevant, da die Apps Groq/Gemini per Network ansprechen.)
**Quelle:** [Apple — Enabling App Sandbox](https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/EntitlementKeyReference/Chapters/EnablingAppSandbox.html)

### F4. Apple Events / AppleScript an Fremd-Apps in Sandbox blockiert
**Symptom:** `errAEEventNotPermitted` (-1743) beim Senden von Apple Events (z.B. an Terminal/Editor).
**Ursache:** Sandbox erlaubt keine Apple Events ohne Berechtigung; die API kann den Auth-Status nicht abfragen.
**Versionen:** ab Mojave 10.14, bis heute.
**FIX (funktionserhaltend):** `com.apple.security.automation.apple-events` + `NSAppleEventsUsageDescription`; fuer bestimmte Ziele `com.apple.security.temporary-exception.apple-events`. Status nicht abfragbar → einmal senden, Fehler abfangen.
**Quelle:** [Felix Schwarz — Apple Event Sandboxing](https://www.felix-schwarz.org/blog/2018/06/apple-event-sandboxing-in-macos-mojave)

### F5. Dateizugriff ausserhalb des Containers blockiert
**Symptom:** Lesen/Schreiben ausserhalb des App-Containers schlaegt fehl.
**Ursache:** Sandbox beschraenkt FS-Zugriff auf den Container.
**Versionen:** per Design.
**FIX (funktionserhaltend):** `com.apple.security.files.user-selected.read-write` + Security-Scoped Bookmarks fuer persistenten Zugriff. Hardcodierte Pfade sind in der Sandbox unmoeglich.
**Quelle:** [Apple — Enabling App Sandbox](https://developer.apple.com/library/archive/documentation/Miscellaneous/Reference/EntitlementKeyReference/Chapters/EnablingAppSandbox.html)

---

## G — Code-Signing / Gatekeeper / Notarization (CLI-`.app`)

### G1. Ad-hoc-Signatur (`codesign -s -`) laeuft nur auf der Build-Maschine
**Symptom:** Auf dem Build-Mac OK; auf anderem Mac „kann nicht geoeffnet werden, Entwickler nicht verifiziert" / App wird gekillt.
**Ursache:** Ad-hoc-Signatur ist nur eine Checksumme ohne Identitaet; Gatekeeper akzeptiert sie nur lokal. (`swiftc`/clang signiert beim macOS-Build automatisch ad-hoc.)
**Versionen:** per Design, alle.
**FIX (funktionserhaltend):** Fuer Verteilung mit „Developer ID Application"-Zertifikat signieren + notarisieren (G4/G5). Fuer Selbsttests: Empfaenger entfernt die Quarantaene (G2).
**Quelle:** [rsms — macOS distribution gist](https://gist.github.com/rsms/929c9c2fec231f0cf843a1a746a416f5)

### G2. `com.apple.quarantine`-xattr → Gatekeeper-Interstitial / „beschaedigt"
**Symptom:** Heruntergeladene/kopierte App bringt „aus dem Internet geladen"-Dialog oder wird blockiert.
**Ursache:** Quarantine-xattr wird beim Download/Transfer gesetzt; Gatekeeper prueft daraufhin Signatur/Notarisierung.
**Versionen:** alle.
**FIX (funktionserhaltend):** `xattr -dr com.apple.quarantine /Pfad/App.app`; pruefen mit `spctl -a -vvv App.app`. Fuer Endnutzer ohne Terminal siehe G3.
**Quelle:** [HackTricks — Gatekeeper](https://hacktricks.wiki/en/macos-hardening/macos-security-and-privilege-escalation/macos-security-protections/macos-gatekeeper.html) · [rsms gist](https://gist.github.com/rsms/929c9c2fec231f0cf843a1a746a416f5)

### G3. Sequoia (15) entfernt den Control-Click-„Oeffnen"-Bypass
**Symptom:** Rechtsklick/Control-Klick → „Oeffnen" funktioniert ab macOS 15 NICHT mehr zum Umgehen.
**Ursache:** Apple hat den Finder-Contextmenu-Override in Sequoia entfernt.
**Versionen:** ab macOS 15; vorher (≤14) ging Control-Click.
**FIX (funktionserhaltend, Endnutzer):** System Settings → Privacy & Security → ganz unten „'App' wurde blockiert…" → **„Open Anyway"** (einmalig pro App). Besser: notarisieren, dann entfaellt es.
**Quelle:** [Michael Tsai](https://mjtsai.com/blog/2024/07/05/sequoia-removes-gatekeeper-contextual-menu-override/) · [Apple Support 102445](https://support.apple.com/en-us/102445)

### G4. „Hardened Runtime not enabled" beim Signieren ausserhalb Xcode
**Symptom:** notarytool-Ablehnung „The executable does not have the hardened runtime enabled."
**Ursache:** `codesign` ohne `--options runtime` aktiviert Hardened Runtime nicht; ausserhalb Xcode leicht vergessen.
**Versionen:** per Design (alle Notarization). (`godot#83469` OPEN — gleiche Falle bei Nicht-Xcode-codesign.)
**FIX (funktionserhaltend):** `codesign --force --options runtime --timestamp -s "Developer ID Application: …" --entitlements ent.plist App.app`. Hardened Runtime ist Pflicht fuer Notarization.
**Quelle:** [godot #83469](https://github.com/godotengine/godot/issues/83469) · [Apple — Resolving notarization issues](https://developer.apple.com/documentation/security/resolving-common-notarization-issues)

### G5. Notarization-Ablehnungen: fehlender Timestamp / `get-task-allow` / unsignierte Nested-Binaries
**Symptom:** Ablehnung wegen fehlendem Secure Timestamp, vorhandenem `com.apple.security.get-task-allow`, unsignierten eingebetteten dylibs/Frameworks oder fehlender Developer-ID-Signatur.
**Ursache:** Debug-Builds tragen `get-task-allow`; CLI-Signaturen vergessen oft `--timestamp`; Nested-Code muss separat signiert sein.
**Versionen:** per Design.
**FIX (funktionserhaltend):** `--timestamp` immer setzen; `get-task-allow` aus Release-Entitlements entfernen; mit „Developer ID Application" signieren; alle eingebetteten Binaries **inside-out** zuerst signieren, dann die App.
**Quelle:** [Apple — Resolving notarization issues](https://developer.apple.com/documentation/security/resolving-common-notarization-issues) · [frr.dev — Notarization-Guide](https://www.frr.dev/posts/macos-notarization-guide-linter/)

### G6. `codesign --deep` ist eine Falle — und seit macOS 13.0 zum Signieren deprecated   [⭐ HAEUFIG]
**Symptom:** Notarisiert, aber zur Laufzeit Permissions falsch / Signatur ungueltig; Nested-Code bekommt App-Entitlements. Bei neueren Toolchains zusaetzlich Deprecation-Hinweis.
**Ursache:** `--deep` signiert verschachtelte Binaries mit denselben Flags/Entitlements wie die App statt individuell — von Apple ausdruecklich NICHT fuer korrektes Signing empfohlen. **Zudem ist `--deep` zum SIGNIEREN seit macOS 13.0 offiziell deprecated** (codesign(1)); nur noch zum reinen *Verifizieren* unproblematisch.
**Versionen:** Deprecation ab macOS 13.0 (codesign(1)); die Entitlements-Falle per Design alle Versionen. **Beide Projekt-`build.sh` nutzen aktuell `codesign … --deep` (Z. 92/97) → umstellen.**
**FIX (funktionserhaltend):** Top-Bundle ohne `--deep` signieren; eventuellen Nested-Code (dylibs/Frameworks) einzeln von innen nach aussen signieren, dann die App. `--deep` nur fuer schnelle ad-hoc-Tests oder zum Verifizieren (`codesign --verify --deep`).
**Quelle:** [Apple TN2206 — Code Signing In Depth](https://developer.apple.com/library/archive/technotes/tn2206/_index.html) · [codesign(1) Manpage](https://keith.github.io/xcode-man-pages/codesign.1.html)

### G7. `zip` statt `ditto` zerstoert die Notarization / „App is damaged"
**Symptom:** Notarization scheitert („signature of the binary is invalid"); ODER beim Endnutzer „App is damaged and can't be opened".
**Ursache:** `zip` strippt Resource-Forks/xattrs und packt ohne `.app`-Wurzel → Ticket enthaelt nur den cdhash des Hauptbinaries. Sequoia hat zudem einen bekannten Gatekeeper-Bug, der selbst korrekt notarisierte Apps faelschlich als „damaged" meldet.
**Versionen:** ZIP-Problem per Design (alle); „damaged"-False-Positive bekannt unter macOS 15.
**FIX (funktionserhaltend):** Fuer Upload/Verteilung `ditto -c -k --keepParent App.app App.zip` (erhaelt Struktur + xattrs). Nach Notarization `xcrun stapler staple App.app` (offline verifizierbar). Endnutzer-Workaround bei false-positive: `xattr -dr com.apple.quarantine App.app`.
**Quelle:** [rsms gist](https://gist.github.com/rsms/929c9c2fec231f0cf843a1a746a416f5) · [Apple Community 255759797](https://discussions.apple.com/thread/255759797)

---

## H — TCC bindet Permissions an Code-Identitaet (Rebuild/Pfad) [DER Kern-Bug ⭐]

> Von 4 unabhaengigen Recherche-Strangen bestaetigt (Accessibility, Mikrofon, Event-Taps, Signing).
> Das ist der wichtigste Bug fuer Franks `swiftc`-Workflow.

### H1. Permission nach JEDEM Rebuild weg (ad-hoc/unsigned Binary)   [⭐ HAEUFIG]
**Symptom:** Nach jedem `swiftc`-Rebuild ist die erteilte Mikrofon-/Accessibility-/Input-Monitoring-Berechtigung verschwunden; Prompt kommt nicht erneut; Aufnahme/Hotkeys schlagen still fehl.
**Ursache:** TCC bindet einen Grant an **Code-Signing-Identitaet + Bundle-ID** (bei fehlender Signatur an inode/Pfad). Ad-hoc-Signaturen (`--sign -`) erzeugen bei JEDEM Build eine neue Identitaet → macOS behandelt es als neue App; geaenderte Bundle-ID hat denselben Effekt.
**Versionen:** per Design, alle (verschaerft auf Sequoia, siehe J3).
**FIX (funktionserhaltend):** (1) **Stabile `CFBundleIdentifier`** in der Info.plist festschreiben, nie aendern. (2) Mit **echtem Zertifikat** signieren (Apple Development fuer den Alltag, Developer ID fuers Release) statt ad-hoc — Signatur bleibt ueber Rebuilds stabil, Grant ueberlebt. (3) Wenn unsigniert bleiben muss: nach jedem Build `tccutil reset Microphone <bundle-id>` / `… Accessibility …` und Eintrag neu setzen.
**Quelle:** [jano.dev](https://jano.dev/apple/macos/swift/2025/01/08/Accessibility-Permission.html) · [Apple Forums 730043](https://developer.apple.com/forums/thread/730043)

### H2. Pfad-Wechsel der `.app` invalidiert die Berechtigung (unsigned)
**Symptom:** Nach Verschieben der App (z.B. neuer Build-Output-Pfad, `~/Downloads` → `/Applications`) erscheint sie nie im Berechtigungsdialog bzw. funktioniert nicht mehr.
**Ursache:** Bei unsigniertem Binary speichert macOS den Grant mit **inode + Pfad**. Neuer Pfad = unbekannte App. (`input-leap#2224` OPEN — binary identity mismatch.)
**Versionen:** per Design (verschaerft bei unsigniert), alle.
**FIX (funktionserhaltend):** App an **festem Pfad** ausfuehren; bei Updates denselben Pfad behalten. Idealfall: signieren (dann zaehlt Identitaet statt Pfad).
**Quelle:** [input-leap #2224](https://github.com/input-leap/input-leap/issues/2224) · [claude-code #46859](https://github.com/anthropics/claude-code/issues/46859)

### H3. Geist-/Duplikat-Eintraege → Prompt verschwindet ganz
**Symptom:** In Privacy & Security wachsen mehrere Eintraege derselben App (alte Kopien); irgendwann erscheint **kein Prompt mehr** und neue Builds koennen nichts mehr anfordern.
**Ursache:** Jede ad-hoc-Identitaet/jeder Pfad legt einen eigenen TCC-Eintrag an; stale Eintraege blockieren die Neuanfrage. Kein sauberer UI-Aufraeumweg.
**Versionen:** Big Sur+, nicht gefixt.
**FIX (funktionserhaltend):** Alle alten Eintraege aus der Liste entfernen (−), dann `sudo tccutil reset Accessibility <bundle-id>` (bzw. Microphone/ListenEvent), **jede Kopie** der App mit gleicher Bundle-ID loeschen, ggf. Reboot. Danach genau eine Kopie neu autorisieren.
**Quelle:** [input-leap #2224](https://github.com/input-leap/input-leap/issues/2224) · [Macworld](https://www.macworld.com/article/347452/how-to-fix-macos-accessibility-permission-when-an-app-cant-be-enabled.html)

### H4. Info.plist-Patch NACH dem Signieren invalidiert die Signatur → TCC verweigert still
**Symptom:** Nach Bearbeiten der Info.plist im fertigen Bundle verweigert TCC alle Prompts stillschweigend.
**Ursache:** Jede Bundle-Aenderung nach dem Signieren invalidiert die Code-Signatur; TCC verweigert dann ohne Prompt.
**Versionen:** per Design, aktuell.
**FIX (funktionserhaltend):** Reihenfolge in `build.sh`: Bundle bauen → Info.plist **final** setzen → DANN signieren (`codesign --force --options runtime --sign … App.app`). Nach jedem Plist-Edit neu signieren.
**Quelle:** [claude-code #33023](https://github.com/anthropics/claude-code/issues/33023)

### H5. Aus dem Terminal gestartete App kann keine TCC-Permission anfordern
**Symptom:** App per `./App.app/Contents/MacOS/App` oder aus integriertem Terminal gestartet → kein Mic/Accessibility-Prompt; ggf. dem Terminal statt der App zugeordnet.
**Ursache:** Ohne korrekte Bundle-Attribution kann TCC den Grant nicht der App zuordnen; das Parent-Terminal „erbt" die Anfrage.
**Versionen:** per Design, aktuell. (`t3code#728` OPEN.)
**FIX (funktionserhaltend):** Immer die `.app` als Bundle starten (`open App.app`), nicht das nackte Executable. Bundle (mindestens ad-hoc) signiert + stabile Bundle-ID.
**Quelle:** [t3code #728](https://github.com/pingdotgg/t3code/issues/728)

### H6. Ablauf/Neuerzeugung des (Dev-)Signing-Zertifikats → stiller TCC-Reset
**Symptom:** TCC-Grants (Mic/Accessibility) verschwinden ohne Code-Aenderung und ohne Bundle-ID-Aenderung — nur weil das lokale Signing-Zertifikat (z.B. „Frank Local Dev") abgelaufen und neu erzeugt wurde.
**Ursache:** TCC bindet den Grant an die `csreq` (Code Signing Requirement), die aus der Zertifikats-Identitaet abgeleitet wird. Ein neues Zertifikat = neue `csreq` = fuer TCC eine fremde App, ohne Fehlermeldung.
**Versionen:** per Design, alle. Verschaerft bei kurzlebigen selbst-erzeugten Dev-Zertifikaten.
**FIX (funktionserhaltend):** Fuer die Verteilung ein laenger gueltiges „Developer ID Application"-Zertifikat nutzen (robuster als ein lokales Dev-Zertifikat). Beim bewussten Zertifikatswechsel Grants einmalig neu erteilen (ggf. `tccutil reset … <bundle-id>`). Stabile Bundle-ID beibehalten (H1).
**Quelle:** [jano.dev — Accessibility Permission](https://jano.dev/apple/macos/swift/2025/01/08/Accessibility-Permission.html) · best-practices/desktop/swift-appkit.md (G.2)

---

## I — Fenster-Animation (Ein-/Ausgleiten)

### I1. `setFrame(_:display:animate:)` ruckelt und blockiert
**Symptom:** Ein-/Ausgleiten des Panels ist haklig, andere UI friert kurz.
**Ursache:** AppKit treibt die Animation auf dem Main-Thread durch wiederholtes `-setFrame:`; `animate: true` ist synchron-blockierend bis zum Ende.
**Versionen:** per Design, alle.
**FIX (funktionserhaltend):** Frame-Slides via `NSAnimationContext.runAnimationGroup { $0.duration = …; window.animator().setFrame(target, display: true) }` (asynchron, nicht-blockierend). Fuer fluessigstes Stepping eigener DisplayLink + `layer.transform` (I5).
**Quelle:** [jwilling — osx-animations](https://jwilling.com/blog/osx-animations/) · [Apple — setFrame](https://developer.apple.com/documentation/appkit/nswindow/1419519-setframe)

### I2. Layer-backed View animiert trotz `wantsLayer` schlecht (falsche Redraw-Policy)
**Symptom:** `wantsLayer = true` gesetzt, Animation bleibt trotzdem ruckelig.
**Ursache:** Default `layerContentsRedrawPolicy` laesst AppKit weiter `-setFrame:` aufrufen → Core-Animation-Vorteil geht verloren.
**Versionen:** per Design.
**FIX (funktionserhaltend):** `view.layerContentsRedrawPolicy = .onSetNeedsDisplay` (+ `wantsLayer = true`). Dann laeuft die Animation auf Core Animations eigenem Thread → fluessig.
**Quelle:** [jwilling — osx-animations](https://jwilling.com/blog/osx-animations/)

### I3. `CVDisplayLink` ist ab macOS 15 deprecated (vsync-Stepping bricht weg)
**Symptom:** Deprecation-Warnung / Build-Bruch nach SDK-Update; manuelle vsync-Frame-Animation kuenftig unzuverlaessig.
**Ursache:** Die komplette `CVDisplayLink`-Familie ist ab macOS 15.0 deprecated; Ersatz ab macOS 14 auf `NSView`/`NSWindow`/`NSScreen`.
**Versionen:** deprecated ab macOS 15.0; Ersatz ab 14.0.
**FIX (funktionserhaltend):** `view.displayLink(target:selector:)` (liefert ein `CADisplayLink`). Bei Deployment-Target 13 weiter `CVDisplayLink` (per `if #available(macOS 14, *)` migrieren). **Achtung Threading:** Callback kann off-main kommen — UI-Updates strikt `@MainActor`/Main-Thread (I4). *Ob der Callback auf Main laeuft, ist in Quellen widerspruechlich — per `Thread.isMainThread`-Log selbst pruefen.*
**Quelle:** [yabai #2382](https://github.com/koekeishiya/yabai/issues/2382) · [Apple — CVDisplayLink](https://developer.apple.com/documentation/corevideo/cvdisplaylink-k0k)

### I4. Animation/UI vom Background-Thread = kaputt (AppKit ist main-thread-only)
**Symptom:** sporadische Crashes, „nichts passiert", Glitches, wenn Frame/alpha aus einem Hintergrund-Thread gesetzt wird.
**Ursache:** AppKit (NSWindow/NSView/NSAnimationContext) darf nur vom Main-Thread aus beruehrt werden; DisplayLink-Callbacks/async-Arbeit laufen oft off-main.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Lange Arbeit off-main, aber JEDER UI-/Animations-Aufruf zurueck auf Main: `DispatchQueue.main.async {}` bzw. `@MainActor`. DisplayLink-Callback nur Werte berechnen, UI-Mutation auf Main marshallen.
**Quelle:** [jwilling — osx-animations](https://jwilling.com/blog/osx-animations/)

### I5. Eigene Frame-Animation per `setFrame` pro Frame bleibt hakelig
**Symptom:** Selbst mit DisplayLink + `setFrame` pro Frame sichtbarer Jank.
**Ursache:** `-setFrame:` hat pro Aufruf viele Seiteneffekte (Layout, Geometrie-Neuberechnung).
**Versionen:** per Design.
**FIX (funktionserhaltend):** Statt `setFrame` pro Frame direkt `layer.transform` (Translation) animieren; den echten `frame` nur einmal am Ende setzen (fuer Hit-Testing, I6).
**Quelle:** [jwilling — osx-animations](https://jwilling.com/blog/osx-animations/)

### I6. `layer.transform` aendert kein Hit-Testing / Event-Handling
**Symptom:** Per Transform verschobene View empfaengt Klicks/Hover noch an der alten Position.
**Ursache:** AppKit ignoriert den Layer-Transform bei Hit-Test-Berechnungen — Transform ist rein visuell.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Fuer interaktive Endzustaende echten `frame`/Position setzen (nach der Transform-Animation). Reine Visual-Slides duerfen Transform behalten.
**Quelle:** [jwilling — osx-animations](https://jwilling.com/blog/osx-animations/)

### I7. `NSAnimationContext.completionHandler` feuert bei Interruption (wirkt „doppelt"/„zu frueh")
**Symptom:** Beim schnellen Hin-/Her-Toggeln (Ausgleiten startet, waehrend Eingleiten laeuft) feuert der Completion-Block der abgebrochenen Animation → Folge-Logik laeuft im falschen Zustand.
**Ursache:** Der Block ist garantiert „when the animations in the grouping are completed" — auch eine ersetzte/abgebrochene Animation gilt als beendet und ruft den Handler.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Generation-/Token-Guard: `let token = ++currentAnim; … completion { guard token == currentAnim else { return } }`. Vor neuer Animation Zielzustand explizit setzen.
**Quelle:** [Apple — completionHandler](https://developer.apple.com/documentation/appkit/nsanimationcontext/completionhandler) · [Apple Forums 14743](https://developer.apple.com/forums/thread/14743)

### I8. `allowsImplicitAnimation` greift nur im Animator-Kontext
**Symptom:** `view.animator().alphaValue = 0` animiert, andere Properties tun nichts.
**Ursache:** Implizite Animationen brauchen `NSAnimationContext.current.allowsImplicitAnimation = true` UND einen laufenden Context.
**Versionen:** per Design.
**FIX (funktionserhaltend):** In `runAnimationGroup` `context.allowsImplicitAnimation = true` setzen und Properties direkt aendern, oder konsistent ueber `.animator()` gehen.
**Quelle:** [advancedswift — NSView Animations Guide](https://www.advancedswift.com/nsview-animations-guide/)

---

## J — Timer / RunLoop & Swift-6-Concurrency

### J1. `scheduledTimer` pausiert bei Menue-Tracking / Fenster-Resize
**Symptom:** Repeating-Timer (Animations-Tick, Polling) liefert keine Events, solange ein Menue offen ist oder das Fenster grossgezogen wird.
**Ursache:** `Timer.scheduledTimer` registriert im `.default` RunLoop-Mode; Menue-/Resize-Tracking spinnt eine eigene RunLoop im `.eventTracking`-Mode.
**Versionen:** per Design.
**FIX (funktionserhaltend):** Timer manuell registrieren: `RunLoop.current.add(timer, forMode: .common)` (`.common` schliesst `.eventTracking` ein). Fuer Animationen besser DisplayLink (I3).
**Quelle:** [mattrajca — Run Loops & Smooth Scrolling](https://www.mattrajca.com/2016/09/15/on-run-loops-modal-ui-and-buttery-smooth-scrolling.html)

### J2. Swift 6: „Call to main actor-isolated … in a synchronous nonisolated context"
**Symptom:** Code, der unter Swift 5 nur warnte, ist im Swift-6-Language-Mode ein **harter Compilerfehler** — betrifft AppKit-Aufrufe aus nonisolated-Kontext (DisplayLink-/C-Callbacks, Delegate-Methoden ohne Isolation), selbst wenn faktisch auf Main laufend.
**Ursache:** AppKit-View-Klassen sind in neuen SDKs `@MainActor`-isoliert; Aufrufe aus nicht-isoliertem Kontext sind Data-Race-Verstoesse. Seit Swift 6 ist Data-Race-Safety Pflicht.
**Versionen:** Fehler ab Swift-6-Language-Mode (`-swift-version 6`); unter Swift 5 nur Warnung. (Swift 6.2 brachte main-actor-by-default als Opt-in.)
**FIX (funktionserhaltend):** Aufrufer `@MainActor` markieren; in einem Callback, von dem man WEISS, dass er auf Main laeuft, `MainActor.assumeIsolated { … }`; oder `Task { @MainActor in … }`. `@preconcurrency`-Import nur als Brueckenschritt. Migration schrittweise (`-strict-concurrency=targeted` → `complete` → Swift-6-Mode). **Hinweis:** Reiner `swiftc arm64-apple-macos13.0` ohne Package.swift erzwingt KEINEN Mode 6 — der Mode haengt an den `-swift-version`-Flags.
**Quelle:** [swift.org — Concurrency Migration: Common Problems](https://www.swift.org/migration/documentation/swift-6-concurrency-migration-guide/commonproblems/) · [jano.dev — Swift 6 Migration Errors](https://jano.dev/apple/macos/swift/2025/03/09/Swift-6-Migration-Errors.html)

### J3. macOS Sequoia 15: periodische Reauthorisierung (primaer Screen Recording, NICHT Mic/Accessibility im gleichen Takt)
**Symptom:** Laufende App bekommt periodisch (berichtet: monatlich + nach Reboot) erneut System-Permission-Dialoge — auch ohne Code-Aenderung.
**Ursache:** Bewusste TCC-Verschaerfung in Sequoia (urspruenglich woechentlich in Betas geplant, final monatlich). **Praezisierung 2026-06-02:** der monatliche Re-Prompt-Takt betrifft **primaer Screen Recording / Screen Capture** — **Mikrofon und Accessibility laeuten NICHT im selben 30-Tage-Rhythmus**. Eine App ohne Screen Recording (wie diese Overlays) ist vom schlimmsten Aerger nicht betroffen. Kein App-Bug.
**Versionen:** neu ab macOS 15; nicht in 13/14. (*Mic/Accessibility koennen weiterhin durch Update/Nutzer/Identitaetswechsel wegfallen — nur eben nicht im festen Monatstakt.*)
**FIX (funktionserhaltend):** Nicht im Code „fixbar". App beim Start UND bei `didBecomeActiveNotification` defensiv `AXIsProcessTrusted()` / `authorizationStatus(for:.audio)` pruefen, bei Entzug freundlich zur Re-Autorisierung fuehren (Settings-Pane oeffnen). Bei erneutem `.notDetermined`/Denial nicht crashen. NICHT praeventiv Mic neu anfragen „wegen Sequoia" — das nervt unnoetig.
**Quelle:** [TidBITS — Sequoia Permission Prompts](https://tidbits.com/2024/08/12/macos-15-sequoias-excessive-permissions-prompts-will-hurt-security/) · [Daring Fireball](https://daringfireball.net/linked/2024/08/07/macos-15-sequoia-weekly-permission-prompts)

### J4. `MainActor.assumeIsolated` in einem Off-Main-Callback (z.B. AVAudioEngine-Tap) = harter Crash
**Symptom:** App crasht sofort beim ersten Audio-Buffer mit einer `assumeIsolated`-precondition-failure, wenn man das „On-Main"-Bruecken-Pattern aus einem Carbon-Hotkey-Callback versehentlich in die AVAudioEngine-Tap-Closure kopiert.
**Ursache:** `MainActor.assumeIsolated { … }` betritt den Main-Actor NUR korrekt, wenn der Aufrufer wirklich auf dem Main-Thread laeuft (Fail-Fast). Die Tap-Closure laeuft aber auf einem internen Audio-Thread (off-main) → precondition schlaegt fehl.
**Versionen:** Swift 6 Concurrency, alle (relevant ab Concurrency-Migration).
**FIX (funktionserhaltend):** Off-Main-Callbacks per `AsyncStream`/`Task { @MainActor in … }` bruecken, NIE `assumeIsolated`. `assumeIsolated` NUR fuer Callbacks, die garantiert auf Main laufen (Carbon-`EventHandler`, viele AppKit-Delegate-Methoden). Siehe Best-Practices Abschnitt B („Bruecken").
**Quelle:** [fatbobman — MainActor.assumeIsolated](https://fatbobman.com/en/posts/mainactor-assumeisolated/) · [Embracing Swift concurrency, WWDC25-268](https://developer.apple.com/videos/play/wwdc2025/268/)

### J5. `@Observable`-Auto-Tracking in AppKit wirkt auf macOS 15 nur mit `NSObservationTrackingEnabled`-Plist-Key
**Symptom:** Bei MVVM mit `@Observable`-ViewModel aktualisiert sich die AppKit-UI nicht, obwohl Properties in `layout()`/`draw(_:)`/`viewWillLayout()` gelesen werden — kein Crash, die UI bleibt nur stehen.
**Ursache:** Das automatische Observation-Tracking in AppKit ist auf macOS 15 standardmaessig AUS und muss per Info.plist-Key `NSObservationTrackingEnabled = YES` aktiviert werden. Ab macOS 26 ist es Default (Key ignoriert). Zweite Falle: Properties, die NUR in `init`/eigenen Methoden (statt in den getrackten Layout-/Draw-Methoden) gelesen werden, etablieren keine Abhaengigkeit.
**Versionen:** Auto-Tracking ab macOS 15 (mit Plist-Key); Default ab macOS 26. Bei Deployment-Target macOS 13/14 gar nicht verfuegbar → `withObservationTracking`-Fallback (re-arm!) nutzen.
**FIX (funktionserhaltend):** Auf macOS 15 den Plist-Key setzen; Properties in einer getrackten Methode lesen; bei Target < 15 manuell `withObservationTracking` mit erneutem Aufsetzen im `onChange` (auf Main marshallen). Siehe Best-Practices Abschnitt E.1.
**Quelle:** [Apple — Observation](https://developer.apple.com/documentation/Observation) · [steipete.me — Automatic Observation Tracking](https://steipete.me/posts/2025/automatic-observation-tracking-uikit-appkit)

---

## K — `swiftc`-CLI-`.app`-Bundle-Setup

### K1. Ungueltige Bundle-Struktur/Info.plist → App startet nicht / nicht als App erkannt
**Symptom:** `swiftc`-gebautes Binary startet nicht als App / Finder erkennt es nicht; keine Permission-Prompts.
**Ursache:** Fehlende/unvollstaendige Bundle-Struktur und Info.plist.
**Versionen:** per Design, alle.
**FIX (funktionserhaltend):** Struktur `App.app/Contents/{MacOS/App, Info.plist, Resources/}`. Info.plist braucht mindestens `CFBundleExecutable`, `CFBundleIdentifier` (stabil!), `CFBundleName`, `CFBundlePackageType=APPL`, `LSMinimumSystemVersion=13.0`, ggf. `LSUIElement`, `NSMicrophoneUsageDescription`. Danach `codesign --force --options runtime --sign … App.app` (H4). (Franks `build.sh` baut bereits ein Bundle — Keys nur verifizieren.)
**Quelle:** [Vojta Stavik — Building without Xcode](https://medium.com/@vojtastavik/building-an-ios-app-without-xcodes-build-system-d3e5ca86d30d) · [smittytone — Swift Fighting Man](https://blog.smittytone.net/2025/08/21/swift-fighting-man-how-to-duke-it-out-with-the-xcode-build-process-and-win/)

### K2. `swiftc`-App ohne Activation-Policy/Setup → Overlay bekommt keinen Fokus
**Symptom:** Per `swiftc` ohne Storyboard gebaute App: Fenster erscheint nicht vorne / kann nicht Key werden.
**Ursache:** Ohne App-Bundle/Activation-Policy behandelt `NSApplication` die App als Background-Utility (`.accessory`) — solche Fenster bekommen keinen Key-Status.
**Versionen:** per Design.
**FIX (funktionserhaltend):** `@main` + manuelles `NSApplication.shared`/Delegate-Setup (kein Storyboard). `setActivationPolicy(.regular)` (oder bewusst `.accessory` fuer reine Overlays, dann Key-Verzicht akzeptieren); fuer ein Overlay-Panel `NSPanel` mit `.nonactivatingPanel` (A1) + `orderFrontRegardless()`. Policy im `applicationDidFinishLaunching` setzen; Wechsel wirkt evtl. erst nach kurzem Delay.
**Quelle:** [polpiella — Menu bar only AppKit app](https://www.polpiella.dev/a-menu-bar-only-macos-app-using-appkit/) · [Apple — nonactivatingPanel](https://developer.apple.com/documentation/appkit/nswindow/stylemask-swift.struct/nonactivatingpanel)

---

## Fix-Status (was ist bereits gefixt? — ehrlich getrennt)

**Methodik:** GitHub-Issues hart per `gh issue view` geprueft (2026-06-02). Apple-System-Verhalten ist
meist „per Design" (kein OSS-Fix erwartet — der Workaround bleibt dauerhaft aktiv); App-Issues zeigen
ueber ihren OPEN/CLOSED-Status, ob der zugrundeliegende Mechanismus noch lebt.

| Frueherer Bug | Status | Beleg |
|---------------|--------|-------|
| **D1** — Sequoia ⌥/⌥⇧-only Hotkeys blockiert | **GEFIXT ab macOS 15.2** | Apple FB15168205 / `feedback-assistant/reports#552` CLOSED/COMPLETED. Auf 15.0/15.1 Workaround (⌘/⌃ dazunehmen) noetig. |
| **E1/H4** — fehlendes audio-input-Entitlement / Info.plist | in der jeweiligen App gefixt | `claude-code#33023` CLOSED/COMPLETED — Lehrsatz bleibt fuer JEDE neue App gueltig. |
| **E4** — AVAudioEngine Sample-Rate-Crash | in AudioKit umgangen | `AudioKit#1851` CLOSED/COMPLETED (2019). Eigener Code muss `outputFormat(forBus:)` selbst nutzen — Mechanik per Design. |
| **E5** — AVAudioEngine Geraetewechsel | in AudioKit umgangen | `AudioKit#2130` CLOSED/COMPLETED (2020). Single-default-device-Limit bleibt per Design. |
| **D7** — Secure-Input-Indikation | UI-Indikator nachgeruestet (App) | `AeroSpace#1486` CLOSED/COMPLETED — der Secure-Input-Block selbst bleibt per Design. |

**Noch NICHT gefixt (Workaround bleibt aktiv):**
- **D4** CGEventTap `tapDisabledByTimeout` — `ghostty#11883` **CLOSED/NOT_PLANNED** → macOS-Mechanismus bleibt, Re-Enable-Workaround Pflicht.
- **D5** CGEventTap-Input-Monitoring-Untersuchung — `AeroSpace#1012` **OPEN**.
- **A4** `activate(ignoringOtherApps:)` deprecation — `SwiftLint#2643` **OPEN** (bestaetigt die Deprecation).
- **H2/H3** TCC-Identity-/Pfad-Mismatch — `input-leap#2224` **OPEN**, `claude-code#46859` (Pfad).
- **H5** Terminal-Launch kann TCC nicht anfordern — `t3code#728` **OPEN**.
- **G4** Hardened-Runtime ausserhalb Xcode — `godot#83469` **OPEN**.
- **A2** `.nonactivatingPanel`-setStyleMask (FB16484811), **C1–C4, D2–D3, D8–D9, E2–E3, E6, F1–F5, G1–G3, G5–G7, H1, I1–I8, J1–J3, K1–K2** — **per Design / Apple-Systemverhalten**, kein Fix erwartet, Workaround bleibt dauerhaft.

**Ehrlichkeits-Hinweise:**
- **B4** (Stage-Manager-Regression) ist **unverifiziert** — keine belastbare Quelle.
- **D6** (Code-Signing-Race bei Event-Taps) ist plausibel, aber nicht hart datiert (Quelle 2026-02).
- **E3** (Entitlement-XML-Kommentar) — genaue macOS-Version nicht belegt.
- **J3** — exakte Mic-Reauth-Frequenz auf Sequoia uneinheitlich dokumentiert.
- Apple-Doku-Seiten sind teils JS-gerendert und liessen sich nicht per Fetch im Volltext lesen; betroffene Punkte sind durch Sekundaerquellen quer-belegt.

---

## 🔗 Best-Practices-Kopplung (wechselseitige Bezugstabelle)

Bug-Almanach (diese Datei) ↔ Best-Practices `~/proggs/best-practices/desktop/swift-appkit.md`.
Die identische Tabelle steht auch dort — so bleibt jeder Bug mit seiner „so macht man es von vornherein
richtig"-Regel verlinkt.

| Bug-Almanach-Abschnitt (hier) | Zugehoeriger Best-Practice-Abschnitt (`best-practices/.../swift-appkit`) |
|-------------------------------|--------------------------------------------------------------------------|
| **A** NSPanel Fokus/Aktivierung, **B** Window-Level/Spaces/Fullscreen, **K2** | **A** Overlay-Fenster richtig konfigurieren |
| **J** Timer/Swift-6-Concurrency (J2/J4/J5), **I4** | **B** Moderne Swift-Concurrency in AppKit |
| **C** Accessibility-**Permission**, **D5/D8** TCC-Kategorien, **E1/E2/E6** Mic-Permission, **H** TCC-Identitaet, **J3** | **D** Permission-Handling richtig & nutzerfreundlich |
| (eigene UI fuer VoiceOver — kein Bug-Pendant) | **C** Accessibility (VoiceOver) fuer custom AppKit-UI |
| **J5** @Observable-Plist-Key | **E** App-Architektur (MVVM & saubere Struktur) |
| **E** Mikrofon/AVFoundation (E4/E5/E7) | **F** AVFoundation-Audioaufnahme richtig machen |
| **G** Code-Signing/Notarization (G4–G7), **H** TCC-Identitaet (H1/H4/H6), **K1** | **G** Build, Code-Signing & Distribution richtig |

---

## Pflicht-Checkliste vor Arbeit an Swift/AppKit-Overlay-Code

- [ ] **Bundle-ID stabil?** `CFBundleIdentifier` in der Info.plist fest und unveraendert (sonst H1)
- [ ] **Permission-Keys da?** `NSMicrophoneUsageDescription` in der Info.plist (sonst Crash E1); Mic-Test ueber `.app`, nicht nacktes Binary (E2/H5)
- [ ] **Signier-Reihenfolge?** in `build.sh`: bauen → Info.plist final → DANN `codesign` (sonst H4)
- [ ] **NSPanel-Fokus?** `canBecomeKey` + `canBecomeMain` ueberschrieben; `.nonactivatingPanel` im Init gesetzt, nie per `setStyleMask` (A1/A2)
- [ ] **Aktivierung?** kein `activate(ignoringOtherApps:)` (deprecated, A4); `.accessory`-Apps holen Fenster via temporaer `.regular` + `activate()` (A3)
- [ ] **Window-Level/Spaces?** bewusstes Level (B1); fuer „ueber allem" hohes Level + `[.canJoinAllSpaces, .stationary]` (B2/B3)
- [ ] **Globale Tastatur?** richtige TCC-Kategorie (NSEvent→Accessibility, CGEventTap→Input Monitoring, D5); Event-Tap `tapDisabledByTimeout` abfangen (D4); auf 15.0/15.1 Hotkey nicht nur ⌥/⌥⇧ (D1)
- [ ] **AVAudioEngine?** Tap mit `inputNode.outputFormat(forBus:0)` (E4); `AVAudioEngineConfigurationChange` behandeln (E5)
- [ ] **Animation?** `window.animator()` in `runAnimationGroup` statt `setFrame(animate:true)` (I1); alle UI-Aufrufe auf Main (I4); completion mit Token-Guard (I7)
- [ ] **Timer?** `RunLoop.current.add(timer, forMode: .common)` (J1)
- [ ] **Swift-6-Mode?** falls aktiviert: AppKit-Aufrufe `@MainActor`/`MainActor.assumeIsolated` (J2)
- [ ] **Sandbox?** NICHT aktivieren, solange globale Monitore/Hotkeys gebraucht werden (F1)
- [ ] Bei JEDEM neu erlebten Bug: hier als Eintrag ergaenzen (Bug + funktionserhaltende Loesung + Versionen), Stand-Header aktualisieren.

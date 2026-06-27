# Bekannte Bugs/Fallen: macOS-Overlay-Fenster (Swift/AppKit) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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

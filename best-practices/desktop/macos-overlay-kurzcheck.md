# macOS-Overlay-Fenster (Swift/AppKit) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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

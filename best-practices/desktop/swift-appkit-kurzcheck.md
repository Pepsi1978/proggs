# Swift / AppKit Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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

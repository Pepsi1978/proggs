# Stream Deck Plugins Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neues Plugin anlegen | `streamdeck create` (Node-SDK, SDKVersion 3); klassisches SDK nur pflegen | §A1, A4 |
| 2 | Node-SDK Entry strukturieren | `registerAction(...)` vor `connect()`; `connect()` als letzte Zeile | §D1 |
| 3 | Instanz identifizieren | Immer `context`/`ev.action.id`, nie Koordinaten, nie extern als Langzeit-ID | §C2 |
| 4 | Lifecycle aufsetzen | `willAppear` idempotent init, `willDisappear` Timer stoppen + aufraeumen | §B2, C1 |
| 5 | Settings schreiben | Ganzes Objekt `{...current, feld}` aus Event-Args, nicht synchron `getSettings` | §G1, G2 |
| 6 | API-Keys/Tokens speichern | NUR `globalSettings` (verschluesselt), nie Action-Settings, nie ins Bundle | §G3, Q1 |
| 7 | An den PI senden | Erst wenn offen (`streamDeck.ui.current`); fetch-like Routing mit Status+Body | §E1, E3 |
| 8 | Property Inspector bauen | `sdpi-components` lokal, `setting="key"` bindet auto, KEIN Save-Button | §F1, F2 |
| 9 | Dynamische Taste rendern | Ganze Taste als SVG via `setImage()` + `setTitle("")`; States nur fuer Toggles | §H1, H2 |
| 10 | Haeufige Updates / Polling | Max 10/s, nur bei Aenderung; Push per SSE statt `setInterval`-Throttling | §L1, L2 |
| 11 | Node-Backend Sandbox | Keine `.node`-Module, Deps bundeln, globale Error-Handler, `logger` statt `console` | §K1, K2 |
| 12 | Plugin paketieren | `streamdeck validate` → `pack`, nie `Compress-Archive`; UUID nach Release einfrieren | §O1, P1 |
| 13 | Icons liefern | PNG quadratisch transparent, @1x + @2x; Listen-Icons monochrom weiss | §I1 |
| 14 | SD+ Touch-Layout | Items im 200×100-Canvas, Touch-Targets ≥ 35×35 px, sonst laedt Layout nicht | §J2 |
| 15 | Globale Settings empfangen | `getGlobalSettings()` aktiv anstossen, sonst feuert der Listener nie | §G4 |

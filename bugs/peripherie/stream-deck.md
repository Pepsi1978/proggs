# Bekannte Bugs: Elgato Stream Deck Plugin-Entwicklung

> **PFLICHT-LESEN vor Arbeit an Stream-Deck-Plugins** (`*.sdPlugin/*`, Stream-Deck-`manifest.json`,
> `propertyInspector`, `code.js`/`plugin.html` im Plugin-Webview, `@elgato/streamdeck`-Node-Code).
> Teil des Bug-Almanach-Systems — siehe [`SYSTEM.md`](../SYSTEM.md).
>
> **Stand:** recherchiert am **2026-06-03**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax) fuer:
> - **Stream Deck Software 7.4.2** (Frank, Hardware: Stream Deck XL, Windows) — der reale Ziel-Anker.
>   **Aktuellste Software Mitte 2026: 7.5.0 (30.06.2026)** — dazwischen 7.4.1 (14.04.) und 7.4.2 (18.05.).
>   7.5.0 ist ein Hardware-/Feature-Release (NIGHTSWORD v2, Key-Claiming, bessere Profil-Import-Fehler) ohne
>   Plugin-SDK-Breaking-Changes; der 7.4.2-Anker bleibt fuer die dokumentierten Bugs gueltig.
> - **SDKVersion 2** (das TVO-Projekt nutzt das klassische HTML/JS-WebSocket-SDK, `CodePath: plugin.html`).
>   **SDKVersion 3 + DRM ist seit 2026 Marketplace-PFLICHT** (Details in §O5): neue Plugins ab **19.01.2026**,
>   Versions-Updates ab **19.02.2026** (MinimumVersion 6.9+, SDKVersion 3, `@elgato/streamdeck` v2+). Fuer Franks
>   klassisches, NICHT im Marketplace veroeffentlichtes TVO-Plugin bleibt SDKVersion 2 gueltig.
> - **`@elgato/streamdeck` 2.1.0** + **`@elgato/cli` 1.7.4** — per Re-Recherche 2026-07-02 **weiterhin aktuell**
>   (keine neuere npm-Version; spaetere CLI-Repo-Commits nur Build-Infra). Fuer das moderne Node.js-SDK (Node 24+, Stream Deck 7.1+).
> - Gebundelte Node-Runtime der App: **20.20.0 + 24.13.1**. Property-Inspector laeuft in eingebettetem Chromium.
>
> Der Almanach deckt BEIDE SDK-Generationen ab: das **klassische WebSocket/JS-SDK** (das aktuelle TVO-Projekt)
> UND das **moderne Node.js-SDK** (`@elgato/streamdeck` + CLI). Pro Bug steht dabei, fuer welche Generation er gilt.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Plugin erscheint gar nicht, keine Fehlermeldung | Ordner `<uuid>.sdPlugin` == manifest-UUID, nur `[a-z0-9.-]` | §A1–A3 |
| 2 | Bild fehlt oder Plugin laedt nicht | Image-Pfade OHNE Endung, `CodePath`/PI-Pfad MIT Endung | §A5 |
| 3 | manifest.json laedt nicht trotz gueltigem Inhalt | Als UTF-8 OHNE BOM speichern, JSON validieren | §A10, K2 |
| 4 | `context` extern gespeichert / nach Neustart tot | Nie extern persistieren, nach Reconnect Zustand neu aufbauen | §D3, E4 |
| 5 | Timer/Polls laufen auf totem context weiter | Bei `willDisappear` Timer stoppen + aus Map entfernen | §D4 |
| 6 | `willAppear` feuert mehrfach / je Geraet | Pro context idempotent registrieren, nie eine Instanz annehmen | §D1 |
| 7 | Tastendruck loest doppelt aus | Handler nur EINMAL binden + keyDown debouncen (~75 ms) | §C5, D5 |
| 8 | Felder verschwinden nach `setSettings` | Ganzes Objekt schreiben `{...current, feld}`, aus Event-Args lesen | §G1, G2 |
| 9 | API-Key pro Instanz weg / im Export sichtbar | Secrets NUR via `setGlobalSettings`, nie Action-Settings/Bundle | §G5, N2 |
| 10 | `setTitle` wirkt nicht / Custom-Title weg | Taste als SVG via `setImage()` + `setTitle("")` nur bei echtem Text | §E1, Z4 |
| 11 | Editor friert ein bei haeufigem Update | `setState`/`setImage` dedupen, < 10 Calls/s halten | §E5, M1 |
| 12 | Polling-Plugin wird traege im Hintergrund | Chromium drosselt Timer → SSE/`EventSource` statt `setInterval` | §M2 |
| 13 | Node-Plugin crasht in Restart-Loop / `--no-addons` | Globale `uncaughtException`/`unhandledRejection`, keine `.node`-Module | §I1, I2 |
| 14 | Aenderung erscheint nicht | `streamdeck restart <uuid>`, Software beenden (Lock), nicht als Admin | §E2, K6, N1 |
| 15 | Packen scheitert / Plugin laedt nicht | `streamdeck validate` → `pack`, nie `Compress-Archive` | §J1, K3 |

---

## A. manifest.json — Plugin laedt GAR NICHT / Validierung schlaegt fehl

### A1. Ordnername muss exakt der Plugin-UUID entsprechen (sonst stilles Nicht-Laden)
**Symptom:** Plugin erscheint nach Installation gar nicht in Stream Deck — KEINE Fehlermeldung.
**Ursache:** Der Plugin-Ordner muss `<UUID>.sdPlugin` heissen, der Teil vor `.sdPlugin` muss byte-genau der Plugin-UUID aus `manifest.json` gleichen (z.B. UUID `com.tvo.autoenter` → Ordner `com.tvo.autoenter.sdPlugin/`).
**Versionen:** per Design, alle Versionen (klassisch + Node).
**FIX:** Ordnername und Plugin-UUID exakt angleichen (reine Umbenennung, funktionserhaltend).
**Quelle:** docs.elgato.com/streamdeck/sdk/introduction/getting-started/ · .../references/manifest/

### A2. UUID-Zeichensatz: nur lowercase `[a-z0-9.-]`, reverse-DNS
**Symptom:** Plugin/Action laedt nicht oder wird abgewiesen; `sd validate` schlaegt fehl.
**Ursache:** "Action UUIDs must only contain lowercase alphanumeric characters (a-z, 0-9), hyphens (-), and periods (.)". Grossbuchstaben/Sonderzeichen machen die UUID ungueltig.
**Versionen:** per Design.
**FIX:** UUID komplett kleinschreiben, reverse-DNS-Format (`com.firma.plugin`).
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/actions/

### A3. Action-UUID MUSS mit der Plugin-UUID praefixiert sein
**Symptom:** Action erscheint nicht in der Aktionsliste, keine `willAppear`-Events.
**Ursache:** Jede `Actions[].UUID` muss eindeutig UND mit der Plugin-UUID praefixiert sein (`com.tvo.autoenter` → `com.tvo.autoenter.toggle`). Fehlender Praefix → Loader scheitert.
**Versionen:** per Design.
**FIX:** Action-UUIDs als `<plugin-uuid>.<action>` benennen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### A4. UUID NIEMALS nach Veroeffentlichung aendern
**Symptom:** Nach UUID-Aenderung verschwinden ALLE vom Nutzer platzierten Actions aus seiner Konfiguration.
**Ursache:** "Once defined and published, UUIDs must never change ... Changing either of these UUIDs will result in the action(s) being removed from the user's configuration."
**Versionen:** per Design.
**FIX:** UUID einfrieren. Zum Ausblenden/Deprecaten stattdessen `VisibleInActionsList: false` setzen (Action bleibt funktional, nur aus der Liste raus) — funktionserhaltend.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/actions/

### A5. Image-Pfade OHNE Dateiendung — Code-/Inspector-Pfade MIT
**Symptom:** Bilder werden nicht gefunden ODER Plugin laedt nicht.
**Ursache:** Inkonsistente Endungsregeln im Manifest.
- **Endung WEGLASSEN:** `Icon` (Plugin), `CategoryIcon`, `States[].Image`, `Encoder.Icon`, `Encoder.background`, `Profile.Name`.
- **Endung PFLICHT:** `CodePath`/`CodePathMac`/`CodePathWin` (z.B. `plugin.html`, `bin/plugin.js`), `PropertyInspectorPath` und `Actions[].PropertyInspectorPath` (muss auf `.htm`/`.html` enden).
**Versionen:** per Design.
**FIX:** Image-Felder ohne Suffix (`"icons/action-off"`), Code-/PI-Felder mit Suffix.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### A6. Jede Action braucht mindestens einen State
**Symptom:** Action laedt nicht / Validierung scheitert.
**Ursache:** Das `States`-Array ist Pflicht, Minimum 1 Eintrag.
**Versionen:** per Design.
**FIX:** Mindestens einen State mit `Image` definieren.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### A7. `Software.MinimumVersion` braucht exakten Versions-String
**Symptom:** Plugin laedt nicht / Validierung scheitert; oder Plugin laedt bei Nutzern mit aelterer Software nicht.
**Ursache:** Muss ein exakter String sein, z.B. `"6.4"` — NICHT `6`, nicht `6.x`. Gueltige Range derzeit ~`"5.0"`–`"7.4"`. Ueberhoehte MinimumVersion → kein Laden bei Nutzern.
**Versionen:** per Design. SDKVersion 3 (DRM/Marketplace) verlangt `"6.9"`+.
**FIX:** Real benoetigte Mindestversion als praezisen String setzen, nicht ueberhoehen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### A8. `OS`-Array: nur `"mac"`/`"windows"`, jeweils mit `MinimumVersion`
**Symptom:** Validierungsfehler.
**Ursache:** `OS` muss mind. 1 Plattform-Objekt enthalten, nur `"mac"`/`"windows"` erlaubt, jedes mit `MinimumVersion`.
**Versionen:** per Design.
**FIX:** Z.B. `[{"Platform":"windows","MinimumVersion":"10"}]`.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### A9. `Controllers`-Werte sind case-sensitive: `"Keypad"` / `"Encoder"`
**Symptom:** Action erscheint nicht fuer das erwartete Geraet (Keypad vs. SD+ Dial/Touchscreen).
**Ursache:** Tippfehler/falsche Gross-Klein-Schreibung in `Controllers`.
**Versionen:** per Design (relevant ab SD+ / 6.x).
**FIX:** Exakt `"Keypad"` und/oder `"Encoder"` schreiben.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### A10. Ungueltiges JSON / fehlende Pflichtfelder
**Symptom:** Plugin laedt nicht.
**Ursache:** JSON-Syntaxfehler oder fehlendes Pflichtfeld (`UUID`, `CodePath`, `SDKVersion`, `Software.MinimumVersion`, `Actions`).
**Versionen:** per Design.
**FIX:** JSON validieren (`python -c "import json;json.load(open(...))"`), Pflichtfelder gegen die Manifest-Referenz pruefen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

---

## B. Icons / Images (Rendering bricht oder Plugin laedt nicht)

### B1. Fehlende `@2x`-Variante → Rendering-Fehler auf HiDPI / Packaging-Abbruch
**Symptom:** Icons fehlen auf HiDPI-Displays; DistributionTool/`sd pack` bricht ab.
**Ursache:** Jedes Bild braucht Normal- UND `@2x`-Variante. Pixel-Spezifikation (PNG, transparenter Hintergrund):
- Plugin-Icon: 256×256 / 512×512 @2x · Category-Icon: 28×28 / 56×56
- Action-Listen-Icon: 20×20 / 40×40, **monochrom #FFFFFF**, transparent (keine Vollflaechen-Hintergruende)
- State-/Key-Image / Encoder-Icon: 72×72 / 144×144 @2x · Encoder-Background: 200×100 / 400×200 @2x
**Versionen:** per Design.
**FIX:** Immer beide Groessen liefern, quadratisch (nicht-quadratische werden gestreckt), Action-Icons monochrom-transparent.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/ · docs.elgato.com/guidelines/stream-deck/plugins/

### B2. (SD+) Touch-Strip-Layout laedt GAR NICHT bei Out-of-bounds-Items / zu kleinen Touch-Targets
**Symptom:** Custom-Touch-Strip-Layout (Stream Deck +) wird nicht angezeigt; `setFeedback` wirkt ins Leere.
**Ursache:** Ein Layout-Item ragt aus dem **200×100-px-Canvas** heraus, ODER ein interaktives Element ist kleiner als das **Mindest-Touch-Target 35×35 px** → das Layout wird verworfen (kein Teil-Render).
**Versionen:** SD+ ab Software 6.0, per Design.
**FIX:** Alle Items im 200×100-Canvas halten, Touch-Targets ≥ 35×35 px, Layout vor Auslieferung mit `streamdeck validate` pruefen. (Best-Practice: `best-practices/peripherie/stream-deck.md` Abschnitt J.)
**Quelle:** docs.elgato.com/streamdeck/sdk/references/touch-strip-layout/ · docs.elgato.com/streamdeck/sdk/guides/dials/

---

## C. WebSocket-Handshake & Reconnect (klassisches SDK)

### C1. Registration-Event muss SOFORT nach Connect gesendet werden
**Symptom:** Plugin verbindet, reagiert aber auf nichts.
**Ursache:** Stream Deck startet das Plugin mit vier Argumenten (`-port`, `-pluginUUID`, `-registerEvent`, `-info`). Direkt im WebSocket-`onopen` muss `{"event":"<registerEvent>","uuid":"<pluginUUID>"}` gesendet werden. Verzoegert/woanders → keine Registrierung.
**Versionen:** klassisches SDK, alle Versionen.
**FIX:** Registrierung im `onopen`-Handler senden (so macht es das TVO-`code.js` korrekt). Node-SDK kapselt das.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/plugin/

### C2. (Node-SDK) `streamDeck.connect()` ZULETZT aufrufen — Actions vorher registrieren
**Symptom:** Actions tauchen nicht auf / Events landen ins Leere.
**Ursache:** "It is important to register all of your plugin's actions before connecting to Stream Deck."
**Versionen:** `@elgato/streamdeck` (alle).
**FIX:** Alle `streamDeck.actions.registerAction(...)` VOR `streamDeck.connect()` — `connect()` als letzte Zeile der Entry-Datei.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/actions/

### C3. Doppelte Registrierung beim Reconnect → "Plugin is already connected"
**Symptom:** Nach Disconnect+Reconnect schlaegt die neue Verbindung still fehl; App-Log: `Plugin is already connected`.
**Ursache:** Beim Reconnect wird der Register-Event erneut gesendet, obwohl die App den Plugin-Context noch als verbunden fuehrt — die Doppel-Registrierung wird verworfen.
**Versionen:** klassisches SDK / eigene WS-Wrapper, alle Versionen.
**FIX:** Alte Socket sauber `close()`, auf `close`-Event warten, DANN komplett neue Socket-Instanz + frischer Handshake. Register-Event nur EINMAL pro lebender Socket.
**Quelle:** github.com/TyrenDe/streamdeck-client-csharp/issues/14 (CLOSED/COMPLETED — Mechanik gilt SDK-uebergreifend)

### C4. Reconnect-Pflicht bei Sleep/Wake, App-Neustart, Geraet getrennt
**Symptom:** WS-Verbindung bricht ohne Vorwarnung ab (Standby, Software-Update/Neustart, USB-Trennung) — Plugin bekommt keine Events mehr, sendet ins Leere.
**Ursache:** WebSockets brechen regelmaessig; das SDK garantiert keinen Auto-Reconnect auf roher Socket-Ebene.
**Versionen:** alle SDK-Varianten.
**FIX:** Reconnect-Schleife im `onclose` (TVO-`code.js`: `setTimeout(openWebSocket, 1000)`), idealerweise mit Backoff. Beim Reconnect Zustand neu aufbauen statt anzunehmen, alte `context` seien noch gueltig (siehe C5). **Hinweis Polling-Plugins:** SSE (`EventSource`) hat Auto-Reconnect eingebaut und ist nicht vom Background-Throttling betroffen (siehe M2) — im TVO-Plugin der primaere Push-Pfad.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/plugin/ · websocket.org/guides/reconnection/

### C5. Event-Handler im connect/open-Handler gebunden → bei jedem Reconnect mehrfach
**Symptom:** Nach mehreren Reconnects wird jedes Event 2×, 3× … verarbeitet (z.B. keyDown loest mehrfach aus, siehe D5).
**Ursache:** Listener werden INNERHALB des `onopen`/connect-Callbacks registriert und bei jedem Reconnect erneut gebunden.
**Versionen:** alle.
**FIX:** Event-Handler genau EINMAL, ausserhalb des connect-Handlers binden. (Im TVO-`code.js` werden `onmessage` etc. pro neuer Socket-Instanz gesetzt — sauber, weil die alte Instanz verworfen wird; bei wiederverwendeten Sockets waere das ein Bug.)
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/plugin/

---

## D. Lifecycle-Events: willAppear / willDisappear / keyDown

### D1. `willAppear` / `willDisappear` feuern MEHRFACH
**Symptom:** Dieselbe Action bekommt mehrere `willAppear` (Profil-/Seitenwechsel, Ordner, mehrere Geraete, Startup) — ohne dazwischenliegendes `willDisappear`.
**Ursache:** `willAppear` feuert per Design jedes Mal, wenn die Action sichtbar wird. Dieselbe Action kann gleichzeitig auf mehreren Geraeten/Seiten existieren = mehrere lebende `context`.
**Versionen:** per Design, alle Versionen.
**FIX:** Pro `context` eine Instanz im eigenen State-Map fuehren (key = context). Bei `willAppear` idempotent registrieren (vorhandenen context nicht doppelt anlegen), bei `willDisappear` entfernen. NIE annehmen, es gebe nur eine Instanz pro Action. (TVO-`code.js`: `actionContexts` Map mit genau diesem Muster.)
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/plugin/ · .../guides/actions/

### D2. `willAppear` feuert GAR NICHT nach `streamdeck restart` ("action count: 0")
**Symptom:** Nach Plugin-Restart empfaengt das Plugin NULL `willAppear`-Events, interne Action-Map bleibt leer. Tasten zeigen (firmware-gecachtes) Bild und reagieren auf `keyDown`, aber Status-Updates per `setState` gehen ins Leere.
**Ursache:** App-seitiges Verhalten (KEIN SDK-Bug): die State-Machine emittiert `willAppear` bei Plugin-Prozess-Restart inkonsistent. Kein offizielles GitHub-Issue dazu — nur Blog-belegt.
**Versionen:** beobachtet auf aktuellem SDK/Software; vermutlich alle.
**FIX:** Profil im Dropdown aus- und wieder einschalten erzwingt Re-Emit aller `willAppear`. Programmatisch: nicht auf re-emittierte Events verlassen — State beim naechsten echten `willAppear`/Settings-Event neu aufbauen, tolerieren dass nach Restart kein Handle existiert.
**Quelle:** nick-liu.com/posts/streamdeck-sdk-quirks/ (Blog, kein offizielles Issue — ehrlich vermerkt)

### D3. `context` ist NICHT persistent ueber App-Zyklen
**Symptom:** Gespeicherte/extern verwendete context-IDs sind nach Neustart wertlos; `setState`/`setTitle` darauf wirkt nicht (toter context, keine Fehlermeldung).
**Ursache:** "The context of an action is not guaranteed to persist across app cycles and therefore should not be used as a long-term identifier externally." Nach Reconnect bekommt dieselbe Taste oft einen NEUEN context.
**Versionen:** per Design, alle.
**FIX:** `context` nur zur Laufzeit nutzen, niemals extern persistieren. Fuer stabile Identitaet eigene ID in `setSettings` ablegen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/plugin/

### D4. Ghost-Contexts: alte Timer/Polls laufen auf totem context weiter
**Symptom:** Nach Plugin-Reinstall/Reconnect pollen mehrere context-Instanzen parallel; CPU-Last, widerspruechliche `setState`.
**Ursache:** Bei `willDisappear` wurde der zugehoerige Timer nicht gestoppt; der alte context bleibt in der Map, obwohl ungueltig (siehe D3).
**Versionen:** alle.
**FIX:** Bei `willDisappear` IMMER `clearInterval`/Timer stoppen und context aus der Map entfernen. Zusatzschutz: Ghost-Cleanup, der context ohne Aktivitaet > N Sekunden entfernt. (TVO-`code.js` v4+: genau dieser `cleanupGhostContexts` + `GHOST_CONTEXT_TIMEOUT_MS` — eigener Vorfall, siehe Z2.)
**Quelle:** eigener Vorfall (TVO Auto-Enter) · docs.elgato.com/.../websocket/plugin/

### D5. `keyDown`/`keyUp` doppelt — meist Handler-Doppelbindung, selten Hardware-Bounce
**Symptom:** Ein Tastendruck loest die Aktion zweimal aus.
**Ursache:** Haeufigste reale Quelle ist Handler-Doppelregistrierung (Listener im connect-Handler gebunden, siehe C5). Echtes Hardware-Bouncing (zwei keyDowns in <50 ms) kommt vor, ist aber seltener.
**Versionen:** alle.
**FIX:** (1) Listener genau einmal binden. (2) Pro context ein kurzes Debounce gegen Hardware-Bounce. (TVO-`code.js` v5: `KEY_DEBOUNCE_MS` ignoriert keyDowns < 75 ms nach dem letzten — eigener Vorfall, siehe Z3.) (3) Bei kritischen Aktionen Logik in `keyUp` statt `keyDown`.
**Quelle:** eigener Vorfall (TVO Auto-Enter) · forum.keyboardmaestro.com/t/.../17283

### D6. `dialPress` entfernt ab Software 6.5.0 → `dialDown`/`dialUp`
**Symptom:** SD+ Dial-Druck loest nichts mehr aus.
**Ursache:** Breaking Change: "dialPress will not be emitted by the API." (`dialDown`/`dialUp` eingefuehrt ab 6.1.0.)
**Versionen:** **gefixt/geaendert ab 6.5.0** — `dialPress` ist tot.
**FIX:** Auf `dialDown` + `dialUp` umstellen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/changelog/

---

## E. States / setState / setTitle

### E1. `setTitle` wird ueberschrieben — User-Titel hat hoechste Prioritaet
**Symptom:** Plugin ruft `setTitle()`, der angezeigte Titel aendert sich nicht.
**Ursache:** Render-Prioritaet: (1) vom User gesetzter Titel/Bild > (2) Runtime `setTitle`/`setImage` > (3) Manifest-Default. Sobald der User einen eigenen Titel gesetzt hat, gewinnt der immer.
**Versionen:** per Design, alle.
**FIX:** Wenn dynamische Titel zwingend sind: ganze Taste als SVG via `setImage()` rendern und `setTitle("")` setzen (umgeht Cache-/Prioritaetslogik, volle Typografie-Kontrolle). Alternativ `UserTitleEnabled: false` im Manifest, falls User-Override unerwuenscht. **Umkehr-Falle:** ein leerer `setTitle("")` bei JEDEM `setState` loescht den vom Benutzer gesetzten Custom-Title — nur bei echtem Status-Text setzen. (TVO-`code.js` v1.0.4-Fix, siehe Z4.)
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/keys/ · nick-liu.com/posts/streamdeck-sdk-quirks/ · eigener Vorfall

### E2. Manifest-Titel-Settings (TitleAlignment/FontSize/ShowTitle) wirken nicht auf bestehende Tasten
**Symptom:** Aenderung an `TitleAlignment`/`FontSize`/`ShowTitle` im Manifest hat keinen Effekt auf bereits platzierte Tasten — nur neu platzierte respektieren sie.
**Ursache:** Die App cached Titel-Settings PRO Taste lokal. Das Manifest seedet nur die Initialwerte bei NEUER Platzierung; Reloads ueberschreiben den Cache nicht.
**Versionen:** alle.
**FIX:** Taste loeschen + neu platzieren, ODER Taste komplett als SVG via `setImage()` rendern (umgeht das Titel-System ganz).
**Quelle:** nick-liu.com/posts/streamdeck-sdk-quirks/

### E3. Race zwischen eigenem `setState` und SDK-Auto-Toggle (2-State-Actions)
**Symptom:** Bei 2-State-Action toggelt die Taste beim Druck automatisch — und kollidiert mit eigener `setState`-Logik (State spiegelt echten Backend-Status, springt aber durch Auto-Toggle weg).
**Ursache:** Bei zwei Manifest-States toggelt die App den State automatisch bei jedem Tastendruck; eigenes `setState` laeuft dagegen an.
**Versionen:** `DisableAutomaticStates` verfuegbar ab Software 6.4.0; davor war Auto-Toggle nicht abschaltbar.
**FIX:** Im Manifest `"DisableAutomaticStates": true` setzen und den State ausschliesslich selbst per `setState(0/1)` steuern. So spiegelt der State immer den echten Zustand. (Genau so macht es das TVO-Plugin, manifest v1.0.4+ — eigener Vorfall, siehe Z1.)
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/ · .../guides/keys/ · eigener Vorfall

### E4. `setState`/`setTitle` auf totem context (nach Reconnect/Disappear)
**Symptom:** Updates zeigen keine Wirkung (kein Fehler, einfach ignoriert).
**Ursache:** context ist nach `willDisappear`, App-Neustart oder Reconnect ungueltig (siehe D3).
**Versionen:** alle.
**FIX:** Nur context ansteuern, die aktuell ein aktives `willAppear` ohne `willDisappear` haben. State-Map als Single Source of Truth fuehren; `sendToStreamDeck` nur wenn `websocket.readyState === OPEN` (TVO-`code.js`).
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/plugin/

### E5. `setState`-Spam belastet die SDK und friert den Editor ein
**Symptom:** Bei haeufigem `setState` (z.B. pro Poll-Tick) friert im Stream-Deck-Editor die State-Konfigurations-UI ein; hakelige Visual-Updates.
**Ursache:** Zu viele `setState`/`setImage`-Calls pro Sekunde; offizielles Limit ~**10 Calls/s** fuer Key-/Touch-Updates. Der Editor bekommt keine Render-Pause.
**Versionen:** alle.
**FIX:** Dedup — `setState` nur senden, wenn sich der State wirklich geaendert hat (kein Bombardieren). Calls coalescen, < 10/s halten. (TVO-`code.js` v1.0.5: `applyStateIfChanged` mit `lastOn`-Dedup — eigener Vorfall, siehe Z5.)
**Quelle:** docs.elgato.com/guidelines/stream-deck/plugins/ · eigener Vorfall

---

## F. Property Inspector (Konfig-Panel)

### F1. `PropertyInspectorPath` falsch → PI bleibt weiss
**Symptom:** Konfig-Panel ist leer/weiss.
**Ursache:** Pfad zeigt ins Leere. Muss relativ zum Plugin-Root sein, OHNE fuehrenden Slash, korrekte Gross-/Kleinschreibung (plattformabhaengig streng). Plugin-Level-PI greift nur fuer Actions OHNE eigenen `Actions[].PropertyInspectorPath`.
**Versionen:** alle.
**FIX:** Pfad exakt pruefen (relativ, kein `/` am Anfang, korrekte Case, Endung `.html`).
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/ui/ · .../references/manifest/

### F2. (klassisch) Eigener PI-Handshake fehlt → PI bekommt keinen context
**Symptom:** PI laedt, aber keine Werte; `sendToPlugin`/`setSettings` tun nichts.
**Ursache:** Der PI muss `window.connectElgatoStreamDeckSocket = (port, uuid, event, info, actionInfo) => {...}` definieren. Stream Deck ruft diese Funktion nach DOM-Load auf und liefert Port + UUID (= context) + `actionInfo` (enthaelt die aktuellen Settings). Ohne sie: kein context, keine Verbindung.
**Versionen:** klassisches SDK ohne `sdpi-components`/`@elgato/streamdeck`.
**FIX:** Handshake implementieren; `uuid` als context speichern; WebSocket oeffnen und mit dem uebergebenen `event` registrieren.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/ui/ · .../guides/ui/

### F3. `connectElgatoStreamDeckSocket` zu spaet definiert
**Symptom:** Verbindung wird nie aufgebaut, PI bleibt "Loading…".
**Ursache:** Stream Deck invokiert die Funktion direkt nach DOM-Load. Wird sie erst in einem spaeten async-Script/dynamischen Import gesetzt, verpasst sie den Invoke.
**Versionen:** klassisches SDK.
**FIX:** `window.connectElgatoStreamDeckSocket` synchron im Haupt-Script definieren, bevor das DOM fertig ist.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/ui/

### F4. `sdpi-components.js` nicht (lokal) eingebunden
**Symptom:** `sdpi-*`-Tags rendern nicht, PI bleibt leer; `SDPIComponents`/`streamDeckClient` ist undefined.
**Ursache:** Script-Tag fehlt oder zeigt nur aufs CDN (offline/inkonsistent).
**Versionen:** sdpi-components (CDN v4).
**FIX:** `sdpi-components.js` LOKAL neben die HTML legen und referenzieren (offiziell empfohlen). Dann `const { streamDeckClient } = SDPIComponents;`.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/ui/ · sdpi-components.dev

### F5. `beforeunload` im PI entfernt ab Software 6.9.0 (Chromium 122)
**Symptom:** Aufraeum-/Speicher-Logik im PI, die an `beforeunload` haengt, laeuft beim Schliessen nicht mehr.
**Ursache:** Chromium-122-Upgrade in 6.9.0 — `beforeunload` wird im PI nicht mehr emittiert.
**Versionen:** **gebrochen ab 6.9.0** (betrifft 7.4.2).
**FIX:** Settings sofort bei jeder Aenderung via `setSettings`/`sendToPlugin` persistieren statt erst beim Unload — funktionserhaltend (und ohnehin robuster).
**Quelle:** docs.elgato.com/streamdeck/sdk/references/changelog/

### F6. Geaenderter PI/HTML wird nicht uebernommen (Cache)
**Symptom:** PI zeigt alte Version trotz Code-Aenderung.
**Ursache:** Stream Deck cached Plugin-Assets; PI-Aenderungen greifen erst nach Plugin-/Software-Neustart.
**Versionen:** alle.
**FIX:** `streamdeck restart <uuid>` bzw. Software neu starten; bei hartnaeckigem Cache Plugin de-/reinstallieren.
**Quelle:** nick-liu.com/posts/streamdeck-sdk-quirks/ · docs.elgato.com/.../guides/ui/

---

## G. Settings-Persistenz (die teuersten Bugs)

### G1. `setSettings` ueberschreibt das GANZE Objekt — kein Merge
**Symptom:** Nach `setSettings({count: 1})` sind alle anderen Felder (`name`, `apiKey` …) verschwunden.
**Ursache:** `setSettings(payload)` ersetzt das komplette Settings-Objekt der Action-Instanz — es merged NICHT.
**Versionen:** alle (klassisch + Node).
**FIX:** Immer aktuelle Settings aus dem Event-Argument (`ev.payload.settings`) nehmen, Feld aendern, KOMPLETTES Objekt zurueckschreiben: `setSettings({...current, count: 1})`.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/settings/ · github.com/FritzAndFriends/StreamDeckToolkit/issues/4

### G2. `getSettings` ist asynchron — sofort danach gelesen ist leer/alt
**Symptom:** `const s = getSettings(); use(s)` liefert undefined/veraltete Werte.
**Ursache:** `getSettings` schickt nur eine Anfrage; das Ergebnis kommt erst spaeter ueber das Event `didReceiveSettings`.
**Versionen:** alle.
**FIX:** Settings aus den Event-Argumenten von `willAppear`/`didReceiveSettings` ziehen ("we recommend using the settings supplied as part of the event arguments"). `getSettings()` nur als awaitable Promise, nie synchron.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/settings/ · .../references/websocket/ui/

### G3. `getSettings` nur verfuegbar waehrend die Action sichtbar ist
**Symptom:** Settings-Abruf liefert nichts, wenn die Action gerade nicht sichtbar ist.
**Ursache:** getSettings funktioniert nur "whilst the action is visible".
**Versionen:** alle.
**FIX:** Settings aus Lifecycle-Event-Argumenten ziehen, nicht aktiv pollen.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/settings/

### G4. Settings bei rapidem Tippen verloren (kein Debounce)
**Symptom:** Schnelles Tippen → nur ein Teil des Wertes wird gespeichert / letzter Anschlag fehlt.
**Ursache:** `oninput="setSettings()"` feuert bei JEDEM Anschlag; jeder Call ueberschreibt das ganze Objekt (G1), ueberlappende Roundtrips ueberholen sich.
**Versionen:** handgeschriebener PI (sdpi-components debounct selbst).
**FIX:** Client-seitiges Debounce (150–300 ms) vor `setSettings`.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/settings/ · deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication

### G5. `setGlobalSettings` mit `setSettings` verwechselt → Datenverlust ODER Secret-Leak
**Symptom:** API-Key/Token verschwindet pro Instanz, ist ueberall sichtbar, oder taucht beim Profil-Export im Klartext auf.
**Ursache:** `setSettings` = Action-Instanz-Settings (im Profil-Export enthalten, Klartext); `setGlobalSettings` = plugin-weit, sicher lokal gespeichert.
**Versionen:** alle.
**FIX:** Sicherheitskritisches (OAuth-Token, API-Keys) IMMER per `setGlobalSettings`/`getGlobalSettings` — "should always be persisted using global settings, never action settings".
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/settings/ · github.com/BarRaider/streamdeck-tools/wiki/Global-Settings

### G6. `didReceiveGlobalSettings` feuert nicht von allein
**Symptom:** Plugin wartet auf globale Settings, Event kommt nie.
**Ursache:** `didReceiveGlobalSettings` wird nur ausgeloest, NACHDEM `getGlobalSettings` aufgerufen wurde (oder nach `setGlobalSettings`). Es feuert nicht automatisch beim Start.
**Versionen:** alle.
**FIX:** Beim Plugin-Start aktiv `getGlobalSettings()` aufrufen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/ui/ · .../guides/settings/

### G7. `didReceiveGlobalSettings`-`context` ist `undefined` bei PI-Aenderung
**Symptom:** Im Plugin auf `context` aus dem globalen Settings-Event verlassen → undefined.
**Ursache:** Bei Aenderung aus dem Property Inspector ist `context` im Event-Payload immer `undefined`.
**Versionen:** alle.
**FIX:** Globale Settings nicht mit einem Action-context verknuepfen — sie sind plugin-weit.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/ui/

### G8. `setGlobalSettings`-Argument muss `JsonObject` sein (Node-SDK)
**Symptom:** Settings "verschwinden"/Laufzeit-Inkonsistenz.
**Ursache:** Vor `0.4.0-beta` akzeptierte `streamDeck.settings.setGlobalSettings(...)` beliebige Typen.
**Versionen:** **Compile-Zeit-Schutz ab `@elgato/streamdeck` 0.4.0-beta** (in 2.1.0 enthalten).
**FIX:** Argument muss `JsonObject` erweitern; mit aktueller SDK-Version greift der Typcheck.
**Quelle:** github.com/elgatosf/streamdeck — CHANGELOG.md

### G9. sdpi-components: `setting`-Attribut fehlt → Wert landet nie in den Settings
**Symptom:** sdpi-Feld zeigt einen Wert an, aber die Settings bleiben leer.
**Ursache:** Persistenz haengt am `setting`-Attribut (= Property-Pfad in den Settings, dot-notation moeglich, z.B. `setting="lambda.profile"`). Ohne `setting` (oder nur `id`) bindet die Komponente nichts.
**Versionen:** sdpi-components.
**FIX:** Jeder persistenten Komponente `setting="<key>"` geben; nested per Dot-Notation.
**Quelle:** sdpi-components.dev · github.com/GeekyEggo/sdpi-components

### G10. Mehrere Instanzen teilen sich Settings ungewollt
**Symptom:** Aenderung an einer Taste aendert auch andere Instanzen derselben Action.
**Ursache:** Instanz nicht per `context` identifiziert, sondern z.B. nach Koordinaten (auf SD+ koennen Keypad/Encoder identische Koordinaten haben).
**Versionen:** alle.
**FIX:** Instanz IMMER per `context` identifizieren, persistieren und Settings anfragen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/plugin/ · .../guides/settings/

---

## H. PI ↔ Plugin-Kommunikation (Timing)

### H1. Nachricht VOR Verbindung gesendet → still verloren
**Symptom:** `sendToPlugin`/`setSettings` direkt nach Laden tut nichts, kein Fehler.
**Ursache:** Nachrichten vor abgeschlossenem WebSocket-Handshake gehen verloren ("fail silently"). Die Verbindung ist asynchron.
**Versionen:** alle.
**FIX:** Erst nach `onopen`/`connected` senden; ausgehende Calls bis dahin in eine Queue legen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/websocket/ui/ · deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication

### H2. `sendToPropertyInspector` schlaegt fehl wenn PI nicht offen (406)
**Symptom:** Plugin→PI-Nachricht liefert `406 Not Acceptable`.
**Ursache:** PI ist nicht sichtbar → `streamDeck.ui.current` ist `undefined`.
**Versionen:** `@elgato/streamdeck`.
**FIX:** Vor jedem Senden `if (streamDeck.ui.current)` pruefen. Plugin darf nicht annehmen, der PI sei offen.
**Quelle:** deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication

### H3. Unregistrierte Route → 404 / Exception im Handler → 500, PI haengt
**Symptom:** PI wartet endlos auf Antwort.
**Ursache:** Nicht-registrierte Message-Route → `404`; Exception im Plugin-Handler → `500`, PI bekommt nie Antwort.
**Versionen:** `@elgato/streamdeck`.
**FIX:** Routes frueh registrieren; Handler in try/catch, immer eine Antwort schicken; Response-Status im PI pruefen.
**Quelle:** deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication

### H4. `propertyInspectorDidAppear`/`…DidDisappear`-Race
**Symptom:** PI wird faelschlich als "geschlossen" behandelt; nachfolgende Nachrichten gehen ins Leere.
**Ursache:** Schnelle Sichtbarkeitswechsel feuern Appear/Disappear in dichter Folge; eigene "PI-ist-zu"-Logik baut darauf einen falschen Zustand.
**Versionen:** alle.
**FIX:** Auf das SDK-Lifecycle vertrauen, nicht selbst aus rohem Appear/Disappear ableiten; `streamDeck.ui.current` als Wahrheit.
**Quelle:** deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication

---

## I. Node.js-Backend (`@elgato/streamdeck`) — Crash & Sandbox

### I1. Plugin-Crash loest Endlos-Restart-Loop aus
**Symptom:** Plugin "flackert", startet staendig neu, Actions erscheinen kurz und verschwinden.
**Ursache:** Eine `uncaughtException`/`unhandledRejection` killt den Node-Prozess. Stream Deck hat "automatic failure recovery" und startet sofort neu → reproduzierbarer Start-Crash = Restart-Schleife.
**Versionen:** Node-SDK, alle (Real-World: claudiobernasconi/streamdeck-youtube#2, CLOSED/COMPLETED).
**FIX (funktionserhaltend):** Globale Handler ganz oben im Entry, Fehler ueber den Logger statt Prozesstod:
```ts
process.on("unhandledRejection", (r) => streamDeck.logger.error("unhandledRejection", r));
process.on("uncaughtException",  (e) => streamDeck.logger.error("uncaughtException", e));
```
Async-Code in Action-Handlern immer `try/catch`. Kein Top-Level-`throw` im Entry.
**Quelle:** docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/ · github.com/claudiobernasconi/streamdeck-youtube/issues/2

### I2. Native `.node`-Addons sind VERBOTEN (`--no-addons` erzwungen)
**Symptom:** Plugin crasht beim `require`/`import` eines Pakets mit nativer Binary (`better-sqlite3`, `serialport`, `node-hid`, `sharp`). Fehler nennt `--no-addons`.
**Ursache:** Alle Node-Plugins laufen ZWANGSWEISE mit `--no-addons` (+ `--enable-source-maps`, `--no-global-search-paths`). Native Addons sind blockiert — nicht optional.
**Versionen:** Node-SDK, alle.
**FIX:** Pure-JS-/WASM-Alternative (z.B. `sql.js` statt `better-sqlite3`). Wenn nativer Hardware-Zugriff zwingend ist: separates externes Binary/Child-Prozess ausserhalb der Runtime starten und per IPC anbinden.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/ (Nodejs-Flags)

### I3. `--no-global-search-paths`: global installierte Module nicht auffindbar
**Symptom:** `Cannot find module X` obwohl global installiert.
**Ursache:** Auflösung ueber globale npm-Pfade ist verboten; nur lokal gebundelte Deps zaehlen.
**Versionen:** Node-SDK.
**FIX:** Alle Deps lokal installieren UND ins Bundle einschliessen (Rollup/esbuild bundlet ein).
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### I4. `Nodejs.Version` im Manifest falsch/fehlend
**Symptom:** Plugin startet gar nicht, keine Logs.
**Ursache:** `manifest.json` → `Nodejs.Version` muss exakt `"20"` oder `"24"` (String!) sein. Anderer Wert/fehlend → keine Runtime.
**Versionen:** Node-SDK. `@elgato/streamdeck` 2.1.0 setzt Node 24 voraus.
**FIX:** `"Nodejs": { "Version": "24", "Debug": "enabled" }`.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### I5. Node-Version-Mismatch (gebundelt vs. System)
**Symptom:** Laeuft beim Entwickler, nicht beim Nutzer.
**Ursache:** Stream Deck nutzt die GEBUNDELTE Runtime (20.20.0/24.13.1), NICHT das System-Node. Code, der nur mit lokal installiertem neuerem Node laeuft, crasht.
**Versionen:** Node-SDK.
**FIX:** Gegen die gebundelte Version testen (`Debug: "enabled"` → `--inspect`), keine Features ueber Node 24.13.1 hinaus.
**Quelle:** docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/

### I6. `CodePath` zeigt auf nicht gebaute/falsche Datei
**Symptom:** "could not start", Plugin laedt nicht.
**Ursache:** `CodePath` zeigt auf eine JS-Entry, die nach dem Build NICHT am erwarteten Ort liegt (z.B. erwartet `bin/plugin.js`, Build legt nach `dist/`). `manifest.json` muss neben dem gebauten Code im `.sdPlugin`-Root liegen.
**Versionen:** Node-SDK.
**FIX:** Build-`output.dir` auf `<plugin>.sdPlugin/bin/` setzen, `CodePath` relativ darauf, manifest im `.sdPlugin`-Root lassen.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

### I7. `@action({ UUID })` ≠ manifest-UUID
**Symptom:** Action erscheint nicht, keine `willAppear`.
**Ursache:** Der String in `@action({ UUID: "..." })` stimmt nicht 1:1 mit `Actions[].UUID` im Manifest ueberein.
**Versionen:** Node-SDK.
**FIX:** Beide exakt gleich (Copy-Paste), Regeln aus A2/A3.
**Quelle:** docs.elgato.com/streamdeck/sdk/references/manifest/

---

## J. Elgato CLI & Build / Logging

### J1. `sd pack` scheitert, weil `validate` nicht bestanden
**Symptom:** `.streamDeckPlugin` wird nicht erzeugt.
**Ursache:** `pack` ruft intern `validate` auf — jeder Validierungsfehler (UUID, Images, States, MinimumVersion) blockt das Bundling. `validate`-Regeln aktualisieren sich taeglich.
**Versionen:** `@elgato/cli` 1.7.4.
**FIX:** Erst `streamdeck validate` gruen, dann `streamdeck pack`. In CI `--no-update-check` setzen. Pfade per `.sdignore` (gitignore-Syntax) ausschliessen.
**Quelle:** docs.elgato.com/streamdeck/cli/commands/pack/ · .../validate/

### J2. `sd link` schlaegt unter Windows mit EPERM/symlink fehl
**Symptom:** `streamdeck link` wirft `EPERM: operation not permitted, symlink`.
**Ursache:** Symlink-Erstellung braucht unter Windows Developer Mode oder Admin-Rechte (siehe auch CLAUDE.md-Regel zu Windows-Symlinks).
**Versionen:** CLI auf Windows. (Kein offizielles Issue im Tracker gefunden — Discussion-belegt, ehrlich vermerkt.)
**FIX (funktionserhaltend):** Windows Developer Mode aktivieren ODER Terminal als Admin; alternativ Plugin-Ordner manuell nach `%appdata%\Elgato\StreamDeck\Plugins\` kopieren.
**Quelle:** github.com/orgs/elgatosf/discussions (Status unklar — per gh nicht als Issue auffindbar)

### J3. `streamdeck`/`sd` nicht gefunden / PowerShell-ExecutionPolicy
**Symptom:** `command not found` oder ".ps1 cannot be loaded" nach `npm i -g @elgato/cli`.
**Ursache:** (a) npm-Global-Bin nicht im PATH; (b) `.ps1`-Wrapper braucht ExecutionPolicy.
**Versionen:** CLI auf Windows.
**FIX:** `%USERPROFILE%\AppData\Roaming\npm` in PATH; `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.
**Quelle:** docs.elgato.com/streamdeck/cli/intro/

### J4. `sd restart` braucht die echte UUID (Watch-Script-Platzhalter)
**Symptom:** Code aendert sich, Plugin laedt nicht neu.
**Ursache:** Das Template-Watch-Script (`rollup -c -w --watch.onEnd="streamdeck restart {{UUID}}"`) hat einen Platzhalter, der unersetzt bleibt → Restart greift kein/falsches Plugin.
**Versionen:** CLI.
**FIX:** `{{YOUR_PLUGIN_UUID}}` durch die echte Plugin-UUID ersetzen; `npm run watch` in eigenem Terminal.
**Quelle:** docs.elgato.com/streamdeck/cli/commands/restart/

### J5. Rollup behandelt manche Deps (z.B. AWS-SDK) falsch → Build bricht
**Symptom:** Build schlaegt beim Hinzufuegen einer Dependency fehl.
**Ursache:** Rollup behandelt CommonJS-Pakete faelschlich als ES-Modul.
**Versionen:** Node-SDK-Template.
**FIX:** `@rollup/plugin-json` + `@rollup/plugin-commonjs` einbinden, ggf. `inlineDynamicImports: true`.
**Quelle:** mauricebrg.com/2025/06/streamdeck-lambda-trigger.html

### J6. `console.log` erscheint nicht in den Logs
**Symptom:** Debug-Ausgaben fehlen.
**Ursache:** `console.log` schreibt nicht zuverlaessig in die Log-Datei der gebundelten Runtime.
**Versionen:** Node-SDK (Issue elgatosf/streamdeck#74, CLOSED/COMPLETED — war How-To, kein Bug).
**FIX:** `streamDeck.logger.info(...)` nutzen — schreibt in alle Targets. Logs: `<plugin>.sdPlugin/logs/<uuid>.0.log`.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/logging/ · github.com/elgatosf/streamdeck/issues/74

### J7. Log-Default-Level zu hoch / Logs rotieren weg
**Symptom:** Erwartete `debug`-Zeilen fehlen in Produktion; alte Logs weg.
**Ursache:** Production-Default `INFO` (Dev: `DEBUG`); nur die 10 neuesten Dateien (je ≤10 MiB) bleiben; Deinstallieren loescht Logs.
**Versionen:** Node-SDK.
**FIX:** `streamDeck.logger.setLevel("debug")` fuer Diagnose; Logs vor Reinstall sichern.
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/logging/

---

## K. Windows-Packaging & Distribution (klassisches `.streamDeckPlugin`)

### K1. `.streamDeckPlugin` ist nur ein ZIP mit anderer Endung
**Symptom:** Unklar, wie man die Datei oeffnet/inspiziert.
**Ursache:** Datei-Signatur `PK` (ZIP). Der `*.sdPlugin`-Ordner muss der OBERSTE Eintrag im Archiv sein (keine Zwischenebene).
**Versionen:** klassisch + CLI.
**FIX:** Zum Inspizieren `.streamDeckPlugin` → `.zip` umbenennen, entpacken; beim Bauen den `.sdPlugin`-Ordner direkt als Root packen.

### K2. `manifest.json` mit UTF-8-BOM bricht das Laden
**Symptom:** Plugin laedt nicht, Manifest scheint "kaputt".
**Ursache:** BOM (`EF BB BF`) am Dateianfang stoert den JSON-Parser. Notepad u.a. speichern oft mit BOM.
**Versionen:** alle (Windows-typisch).
**FIX:** `manifest.json` als **UTF-8 OHNE BOM** speichern (vgl. `python-windows.md`).
**Quelle:** generische JSON/BOM-Falle (nicht offiziell von Elgato dokumentiert — Erfahrungswert)

### K3. PowerShell `Compress-Archive` erzeugt inkompatibles ZIP (Backslash-Pfade)
**Symptom:** Selbst gebautes `.streamDeckPlugin` laedt nicht / wird abgelehnt.
**Ursache:** `Compress-Archive` schreibt **Backslash**-Pfadtrenner in die ZIP-Eintraege statt Forward-Slash (PowerShell-Issues #2140, Microsoft.PowerShell.Archive #11).
**Versionen:** Windows PowerShell 5.1 + PowerShell 7.x.
**FIX:** Statt `Compress-Archive` `streamdeck pack` / das DistributionTool nutzen, oder ein ZIP-Tool mit Forward-Slash-Pfaden (natives `tar`/`7z`). **Hinweis:** `build-plugin.ps1` des TVO-Projekts pruefen, ob es `Compress-Archive` nutzt.
**Quelle:** github.com/PowerShell/PowerShell/issues/2140

### K4. Verbotene/versteckte Dateien im ZIP
**Symptom:** Packaging/Validierung scheitert oder Plugin-Review wird abgelehnt.
**Ursache:** `.DS_Store`, `__MACOSX`, `Thumbs.db` im Archiv.
**Versionen:** alle.
**FIX:** Vor dem Packen entfernen bzw. per `.sdignore` ausschliessen.

### K5. DistributionTool legt keine Ordner an / "Invalid Input"
**Symptom:** `DistributionTool.exe -b -i <input> -o <output>` bricht ab ("Invalid Input"/"Invalid Path").
**Ursache:** (a) Source-Ordner endet nicht exakt auf `.sdPlugin` (grosses **P**/**D**, `.sdplugin` wird abgelehnt); (b) Output-Ordner existiert nicht — das Tool legt KEINE Verzeichnisse an; (c) laeuft nur auf Win/macOS.
**Versionen:** klassisches Legacy-Tool. Modern: `streamdeck pack`.
**FIX:** Beide Verzeichnisse vorab anlegen, Source-Ordner korrekt benennen.

### K6. Datei-Locks: Aenderungen greifen nicht, bis die Software geschlossen ist
**Symptom:** Neue Build-Dateien kopiert, aber Verhalten unveraendert.
**Ursache:** Die laufende Stream-Deck-Software haelt Plugin-Dateien offen (Lock).
**Versionen:** alle (Windows).
**FIX:** Stream Deck komplett beenden (Tray → Quit), Dateien in `%appdata%\Elgato\StreamDeck\Plugins\` tauschen, neu starten. Bei haengendem Lock Plugin-Ordner temporaer verschieben.

### K7. Update zieht nicht — alte Version gecacht
**Symptom:** Neu installiertes Plugin zeigt weiter altes Verhalten.
**Ursache:** Installierte Version bleibt gecacht; Doppelklick-Install ueberschreibt nicht zuverlaessig.
**Versionen:** alle.
**FIX:** Preferences → Plugins → "Check for update…", sonst Plugin DEINSTALLIEREN + neu installieren. Auto-Update nur mit eingeloggtem Marketplace-Account.
**Quelle:** help.elgato.com/hc/en-us/articles/360050819312

---

## L. Debugging / Remote DevTools (Windows)

### L1. DevTools/Konsole nicht erreichbar
**Symptom:** Kein Zugriff auf Konsole/Logs; `localhost:23654` zeigt das Plugin nicht.
**Ursache:** Remote-Debugging nicht aktiviert. ODER: der Property Inspector ist nicht sichtbar — dann erscheint seine Seite nicht in der Liste.
**Versionen:** alle (Windows).
**FIX:** DWORD `html_remote_debugging_enabled = 1` unter `HKEY_CURRENT_USER\Software\Elgato Systems GmbH\StreamDeck` setzen, Software neu starten, dann `http://localhost:23654/` in Chrome oeffnen, Plugin per Reverse-DNS-Namen anklicken. PI im Stream Deck offen halten, damit seine Seite auftaucht. (Node-SDK: zusaetzlich `streamdeck dev` / `Debug: "enabled"`.)
**Quelle:** docs.elgato.com/streamdeck/sdk/guides/ui/

---

## M. Performance / Throttling

### M1. Plugin wird "throttled" — Updates verschluckt
**Symptom:** Key-Updates kommen nicht durch; das Plugin verhaelt sich/meldet "throttled".
**Ursache:** Zu viele programmatische Calls. Guideline-Limit: **max. 10 Calls/s** fuer Key-Updates und Touch-Strips; hohe Framerate-Videos verboten.
**Versionen:** alle.
**FIX:** `setImage`/`setTitle`/`setState` drosseln/coalescen (nur bei echter Aenderung, < 10/s). Siehe auch E5.
**Quelle:** docs.elgato.com/guidelines/stream-deck/plugins/ · community.spotify.com (Throttle-Praxisbeleg)

### M2. `setInterval`/`setTimeout` im Plugin-Webview gedrosselt, wenn im Hintergrund
**Symptom:** Polling-basiertes Plugin wird traege/seltener, sobald der Webview-Prozess nicht im Vordergrund ist; Timer warten ploetzlich ~1 s, nach laengerer Inaktivitaet auf 1-Minuten-Raster.
**Ursache:** Chromium-Timer-Throttling (ab Chrome 88 "intensive throttling") in versteckten/backgrounded Seiten. Der Plugin-Webview ist genau so eine Seite.
**Versionen:** alle Webview-basierten Plugins (klassisches SDK).
**FIX (funktionserhaltend):** Nicht auf praezise JS-Timer im Webview verlassen. **Push statt Poll:** `EventSource`/SSE wird event-driven gefeuert und ist NICHT vom Background-Throttling betroffen (im Gegensatz zu `setInterval`). Genau dieser Umstieg ist im TVO-Plugin dokumentiert — Polling als reines 30-s-Fallback, SSE als primaerer Pfad. (Eigener Vorfall, siehe Z6; vgl. `chrome-extensions.md` Webview-Throttling.)
**Quelle:** developer.chrome.com/blog/timer-throttling-in-chrome-88 · eigener Vorfall (TVO Auto-Enter)

---

## N. Installations-/Laufzeit-Umgebung

### N1. App laeuft elevated/als Admin → Plugins laden nicht
**Symptom:** Plugin/Actions erscheinen nicht, frisch nach Install/Update der App.
**Ursache:** Stream-Deck-App laeuft mit erhoehten Rechten — Plugins werden dann nicht geladen.
**Versionen:** alle.
**FIX:** App komplett (nicht elevated) neu starten. Funktionserhaltend, kein Code-Eingriff.
**Quelle:** docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/

### N2. Keine Secrets ins Plugin-Bundle packen
**Symptom:** API-Keys im ausgelieferten Plugin auslesbar.
**Ursache:** "Stream Deck plugins run locally ... it is not recommended to include secrets, for example private API keys."
**Versionen:** alle.
**FIX:** Keys serverseitig/OAuth; sicherheitskritische Settings nur via `setGlobalSettings` (G5). Vgl. Repo-Regel `secrets-in-sk-folder.md`.
**Quelle:** docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/

---

## O. Marketplace-Submission (typische Ablehnungsgruende)

### O1. Falsche Icon-Specs
Plugin-Icon 256×256 + 512×512 @2x (PNG); Action-Listen-Icons 20×20/40×40 monochrom #FFFFFF transparent; Kategorie 28×28/56×56; Key 72×72/144×144. Farben/Vollflaechen in Action-Icons → Ablehnung. (Quelle: Plugin Guidelines)

### O2. PI enthaelt verbotene Elemente
Kein "Save"-Button fuer Action-Settings (Settings persistieren automatisch, G1), keine Spenden-Links, keine Copyright-Listings im Property Inspector. (Quelle: Plugin Guidelines)

### O3. Metadaten/Naming
Plugin-Name unique auf dem Marketplace, akkurat, Englisch; Action-Namen ~max. 30 Zeichen; mind. 1 Preview-Bild; UUID nach Veroeffentlichung NICHT mehr aenderbar (A4); Review-Dauer 4–10 Werktage. (Quelle: Review Process / Metadata Guidelines)

### O4. Wiederverwendete Profil-UUID blockiert Profil-Update
Mitgeliefertes `.streamDeckProfile` mit gleicher (Wrapper-/Page-)UUID ueber Versionen → App verweigert Re-Import. FIX: bei jedem Build frische UUIDs (`crypto.randomUUID()`) generieren. (Quelle: nick-liu.com)

### O5. SDKVersion 3 + DRM ist Marketplace-PFLICHT (Stichtage 2026) + `manifest` wird geschuetzte Ressource 🆕
**Betrifft:** NUR Plugins, die im **Elgato Marketplace** veroeffentlicht werden (Franks klassisches TVO-Plugin ist
nicht betroffen, solange es nicht in den Marketplace geht).
**Was gilt (Re-Recherche 2026-07-02):**
- **Ab 19.01.2026:** NEUE Plugins muessen DRM-kompatibel sein — `Software.MinimumVersion` **6.9+**, `SDKVersion` **3**,
  Node.js-Plugins `@elgato/streamdeck` **v2+**. **Ab 19.02.2026** gilt das auch fuer **Versions-Updates** bestehender Plugins.
  (SDKVersion 2 bleibt im Schema formal gueltig `2|3`, ist aber Marketplace-untauglich.)
- **DRM-Falle fuer den Code (wichtig):** Mit DRM wird die **`manifest.json` zur geschuetzten Ressource** —
  **`streamDeck.manifest` zur Laufzeit ist ab @elgato/streamdeck v2 ENTFERNT**. Wer Manifest-Werte im Code las, muss die
  benoetigten Infos in eine **separate JSON-Datei ODER direkt in den Code einbetten**. Ausgelieferte Dateien sind
  **immutable** (keine Laufzeit-Modifikation) — Dateien zur Laufzeit generieren statt bestehende aendern.
- **DRM wird erst nach Upload + Verarbeitung in der Maker Console aktiv** (lokal getestetes Plugin ist noch ungeschuetzt).
  DRM ist seit CLI **1.6** standardmaessig aktiviert.
- **Weitere Review-Pflichten:** Einreichung (Name/Beschreibung/Medien) auf **Englisch**; **Demo-Video Pflicht** fuer
  Plugins, die Hardware erfordern oder kostenpflichtige Dienste integrieren. Review-Dauer 4–10 Werktage.
**Quelle:** docs.elgato.com — Distribution/DRM + Review Process; Marketplace-Banner (SDKVersion-3-Stichtage), re-verifiziert 2026-07-02.

---

## Z. Eigene Vorfaelle — TVO Auto-Enter Plugin (klassisches SDK, dokumentiert in `code.js`)

> Diese sind im laufenden Betrieb des TVO-Plugins aufgetreten und im Quellcode mit Datum kommentiert.
> Sie bestaetigen die obigen Mechanik-Bugs aus der Praxis und zeigen die funktionserhaltenden Fixes.

### Z1. Auto-Toggle drift den State (manifest v1.0.4) → `DisableAutomaticStates`
Bei 2-State-Action sprang die Anzeige durch das SDK-Auto-Toggle weg vom echten TVO-Status. **FIX:** `"DisableAutomaticStates": true` im Manifest + State ausschliesslich selbst per `setState` setzen — Plugin ist die einzige Wahrheit. (= E3)

### Z2. Ghost-Contexts pollen parallel nach Reinstall (v4)
Nach Plugin-Reinstall liefen zwei `context` parallel und pollten gleichzeitig. **FIX:** `cleanupGhostContexts` entfernt context ohne Stream-Deck-Aktivitaet > 30 s; `lastSeen` bei JEDEM Event aktualisieren, damit kein legitimer, nur lange ungedrueckter context faelschlich als Ghost gekillt wird. (= D3/D4)

### Z3. Doppelte keyDowns erzeugen parallele Toggles (v5) → Key-Debounce
Hardware-Bounce / doppelte OS-Events loesten parallele Toggles aus und brachten den State-Sync zwischen Stream Deck und TVO durcheinander. **FIX:** `KEY_DEBOUNCE_MS` = 75 ms — keyDowns kurz nach dem letzten werden ignoriert (faengt Bounce, laesst jeden bewussten Klick durch). (= D5)

### Z4. Leerer `setTitle` ueberschreibt den vom Benutzer gesetzten Titel (v1.0.4)
Frueher wurde bei JEDEM `setState` ein leerer Title mitgesendet → der im Editor gesetzte Custom-Title verschwand. **FIX:** `setTitle` nur noch bei echtem Status-Text (z.B. "offline"). (= E1)

### Z5. `setState`-Spam (1.4/s) friert die Editor-State-UI ein (v1.0.5)
v1.0.4 hatte das Dedup entfernt → bei stabilem TVO-State setState-Spam → der Stream-Deck-Editor bekam keine Render-Pause und fror ein. **FIX:** Dedup wieder eingebaut (`applyStateIfChanged` sendet nur bei echter Aenderung). (= E5)

### Z6. `setInterval`-Polling auf ~60 s gedrosselt sobald Webview im Hintergrund → SSE
Das 700-ms-Polling wurde von Chromium auf ~1/min gedrosselt, sobald der Plugin-Webview im Hintergrund war. **FIX:** Umstieg auf SSE (`EventSource`) als primaeren Push-Pfad (event-driven, nicht gedrosselt, Auto-Reconnect eingebaut); `setInterval`-Polling nur noch als 30-s-Fallback. (= M2)

### Z7. In-Flight-Poll ueberschreibt frischen Toggle-State (v4)
Direkt nach `keyDown` kam ein noch laufender Poll-Response mit dem ALTEN State zurueck und ueberschrieb den gerade gesetzten. **FIX:** Polling fuer 800 ms nach `keyDown` pausieren (`toggleUntil`), Poll-Updates in dem Fenster verwerfen. (allgemeines Race-Pattern bei Poll + Event)

---

## Fix-Status (Schritt 3 — was ist belegt gefixt, was bleibt offen)

> Ehrlichkeit: streng getrennt zwischen **belegt gefixt** (offizielles Changelog) und **offen/per Design/unklar**.
> Die unten genannten GitHub-Issues wurden am 2026-06-03 per `gh issue view` HART geprueft (echter Status).

### Belegt gefixte / geaenderte Verhaltensweisen (Software- bzw. SDK-Version)
| Frueheres Verhalten | gefixt/geaendert ab | Bezug |
|---------------------|--------------------|-------|
| `dialPress`-Event (SD+ Dial) | **entfernt ab 6.5.0** → `dialDown`/`dialUp` (ab 6.1.0) | D6 |
| `beforeunload` im Property Inspector | **entfernt ab 6.9.0** (Chromium 122) | F5 |
| `didReceiveGlobalSettings` erreichte den PI nicht | **ab 5.0.0** an PI zugestellt | G6 |
| `willAppear`/`willDisappear` ohne `controller`-Property | **ab 6.0.0** (Multi-Action ab 6.5.0) | D1 |
| `setGlobalSettings` akzeptierte beliebige Typen | **Typcheck ab `@elgato/streamdeck` 0.4.0-beta** (in 2.1.0) | G8 |
| Registrierungsfunktion `connectSocket()` | **SDKVersion 1→2: umbenannt zu `connectElgatoStreamDeckSocket()`** | C1/F2 |
| `DisableAutomaticStates` nicht verfuegbar | **ab Software 6.4.0** | E3/Z1 |

### Hart gepruefte GitHub-Issues (gh, 2026-06-03) — alle GESCHLOSSEN, aber meist How-To statt SDK-Bug
| Issue | Status (gh) | Einordnung |
|-------|-------------|-----------|
| `elgatosf/streamdeck#74` "Debug not working?" | CLOSED / COMPLETED (2024-11-25) | War How-To → Lehre J6 (`logger` statt `console`) bleibt gueltig |
| `elgatosf/streamdeck#94` "update button purely plugin-side" | CLOSED / **NOT_PLANNED** (2025-05-17) | won't-fix → Pattern (context aus `willAppear` cachen) bleibt gueltig |
| `claudiobernasconi/streamdeck-youtube#2` "restarting and crashing" | CLOSED / COMPLETED (2020-05-28) | Einzelfall-Crash → Lehre I1 (globale Handler) bleibt |
| `TyrenDe/streamdeck-client-csharp#14` reconnect | CLOSED / COMPLETED (2022-02-07) | Reconnect-Disziplin C3 bleibt gueltig |

### Noch NICHT gefixt / per Design / Status unklar (Workaround bleibt aktiv)
- **A-Gruppe (UUID/Pfade/States):** alles **per Design** — kein "Fix" zu erwarten, die Regeln gelten dauerhaft.
- **D2 `willAppear` feuert nicht nach `streamdeck restart`:** nur Blog-belegt, **kein offizielles Issue** (gh-Suche leer) → Workaround (Profil aus/ein, State neu aufbauen) bleibt aktiv. Status: **unklar/offen.**
- **E1/E2 Title-/Cache-Verhalten:** per Design (App cached pro Taste) → SVG-`setImage`-Workaround bleibt.
- **I2 `--no-addons`:** **per Design** (Sandbox) — native Module bleiben dauerhaft verboten.
- **J2 `sd link` EPERM auf Windows:** nur Discussion-belegt, **kein bestaetigtes Issue im cli-Tracker** (gh-Suche leer) → Developer-Mode-Workaround bleibt. Status: **unklar/offen.**
- **M1/M2 Throttling (10/s, Background-Timer):** per Design → Coalescing + SSE-Push bleiben.

**Methodik-Hinweis:** WebFetch ist fuer github.com/npmjs.com blockiert (verlangt `gh`/`npm`). Die Researcher lieferten Issue-Nummern + Changelog-Aussagen; der Hauptagent hat die Issue-Status per `gh issue view` und die npm-Versionen per `npm view` hart verifiziert. Aussagen ohne solchen Beleg sind oben ausdruecklich als "Blog-/Discussion-belegt, Status unklar" markiert.

---

## Pflicht-Checkliste vor Arbeit am Stream-Deck-Plugin

- [ ] Ordner heisst `<plugin-uuid>.sdPlugin` (grosses P/D), UUID = manifest-UUID, nur `[a-z0-9.-]`? (A1–A3)
- [ ] Image-Pfade OHNE Endung, `CodePath`/`PropertyInspectorPath` MIT? (A5)
- [ ] Jede Action ≥1 State, `Software.MinimumVersion` exakter String, `OS`/`Controllers` korrekt? (A6–A9)
- [ ] manifest.json valides JSON, **UTF-8 OHNE BOM**? (A10, K2)
- [ ] Pro `context` eine Instanz im State-Map; bei `willDisappear` Timer stoppen (keine Ghost-Contexts)? (D1, D4)
- [ ] `context` NICHT extern persistiert; nach Reconnect Zustand neu aufbauen? (D3, E4)
- [ ] Reconnect im `onclose`; Event-Handler nur EINMAL gebunden? (C4, C5)
- [ ] keyDown debounced (Hardware-Bounce/Doppelbindung)? (D5)
- [ ] `setSettings` schreibt das KOMPLETTE Objekt (Merge selbst); Settings aus Event-Args, nicht synchron `getSettings`? (G1, G2)
- [ ] Secrets/API-Keys via `setGlobalSettings`, nicht `setSettings`/Bundle? (G5, N2)
- [ ] Dynamische Titel: SVG-`setImage` + `setTitle("")`; `setTitle("")` NICHT bei jedem setState? (E1, Z4)
- [ ] `setState`/`setImage` dedupliziert und < 10/s? (E5, M1)
- [ ] Polling-Plugin: Background-Throttling beachtet (SSE-Push statt reinem `setInterval`)? (M2)
- [ ] (Node) globale `uncaughtException`/`unhandledRejection`-Handler, `streamDeck.logger` statt `console.log`? (I1, J6)
- [ ] (Node) keine native `.node`-Module (`--no-addons`); `Nodejs.Version` "20"/"24"; alle Deps gebundelt? (I2–I4)
- [ ] (Node) `streamDeck.connect()` als letzte Zeile; `@action`-UUID = manifest-UUID? (C2, I7)
- [ ] Packaging: `.sdPlugin` ist ZIP-Root, keine `.DS_Store`/`__MACOSX`/`Thumbs.db`, `Compress-Archive` gemieden? (K1, K3, K4)
- [ ] Aenderung wird nicht sichtbar → `streamdeck restart <uuid>` / Software beenden (Datei-Lock) / App nicht elevated? (E2, K6, N1)
- [ ] DevTools bei Bedarf: `html_remote_debugging_enabled = 1`, `localhost:23654`, PI sichtbar halten? (L1)

---

## 🔗 Wechselseitige Bezugstabelle (Almanach ↔ Best Practices)

> Gegenstueck: [`best-practices/peripherie/stream-deck.md`](../../best-practices/peripherie/stream-deck.md)
> (angelegt 2026-06-03). Verknuepft jeden Bug-Abschnitt mit der praeventiven Best-Practice.

| Bug-Abschnitt (hier) | Best-Practice-Abschnitt (best-practices/.../stream-deck) |
|----------------------|----------------------------------------------------------|
| A1–A10 (manifest/UUID/Pfade/States) | A. Projekt-/Manifest-Struktur & SDK-Wahl |
| B1 (Icon-Specs/@2x), O1 | I. Icons / Images |
| B2 (SD+ Touch-Layout) | J. SD+ Encoder & Touchscreen |
| C1–C5 (Handshake/Reconnect) | D. WebSocket-Connect & Reconnect |
| D1, D2, D5, D6 (Lifecycle) | B. Action-Lifecycle |
| D3, D4, E4, G10 (context/Ghost) | C. Context-Management & State |
| E1, E2, E3, E5 (States/Titel) + Z1/Z4/Z5 | H. States, setTitle & SVG-Rendering |
| F1, F4, F5, F6 (Property Inspector) | F. Property Inspector UI (sdpi) |
| F2, F3, H1–H4 (PI↔Plugin-Komm.) | E. PI ↔ Plugin-Kommunikation |
| G1–G10 (Settings-Persistenz) | G. Settings-Persistenz |
| G5, N2 (Secrets) | Q. Sicherheit |
| I1–I7 (Node-Sandbox/Crash) | K. Node.js-Backend Sandbox-konform |
| J1, J3, J4, J5, I6 (CLI/Build) | N. Build-Toolchain & CLI |
| J6, J7, L1 (Logging/Debug) | M. Logging & Debugging |
| K1–K7 (Packaging/Distribution) | O. Distribution & Packaging |
| M1, M2 (Throttling/Push) + Z6/Z7 | L. Performance & Push-statt-Poll |
| O1–O4, A4 (Marketplace) | P. Marketplace-Konventionen |

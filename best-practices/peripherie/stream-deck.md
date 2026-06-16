# Stream Deck Plugins — Best Practices (Stand 2026-06-03, Stream Deck 7.4.2 / @elgato/streamdeck 2.1.0)

> **Die „richtige Seite der Medaille" zum Bug-Almanach** [`bugs/peripherie/stream-deck.md`](../../bugs/peripherie/stream-deck.md).
> Der Almanach sagt *was schiefgeht*; diese Datei sagt *welches Muster den Fehler von vornherein verhindert*.
> Vor Arbeit an einem Stream-Deck-Plugin BEIDE lesen.
>
> **Versions-Anker (recherchiert 2026-06-03):**
> - **Stream Deck Software 7.4.2** (Frank: Stream Deck XL, Windows) — der reale Ziel-Anker.
> - **Klassisches WebSocket/JS-SDK, SDKVersion 2** — das aktuelle TVO-Projekt (`CodePath: plugin.html`, `code.js`, `inspector.html`).
> - **Modernes Node.js-SDK:** `@elgato/streamdeck` 2.1.0 + `@elgato/cli` 1.7.4, gebundelte Node-Runtime **20.20.0 / 24.13.1**, PI in eingebettetem Chromium (122+ ab Software 6.9).
> - **SDKVersion 3** ist die generell empfohlene/Pflicht-Variante fuer DRM-Marketplace ab Software 6.9.
>
> Jeder Eintrag traegt Quelle + `offiziell`/`extern`-Flag. Offizielle Elgato-Doku = Grundwahrheit;
> Community = abwaegbare Alternative, ueberstimmt nie das Offizielle.

---

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

---

## A. Projekt-/Manifest-Struktur & SDK-Wahl

### A1. Empfohlenes Ordnerlayout (Node-SDK, via `streamdeck create`) — offiziell
```
.
├── <uuid>.sdPlugin/          ← das ausgelieferte Plugin (self-contained)
│   ├── bin/                  ← Rollup-Output (bin/plugin.js = Entry)
│   ├── imgs/                 ← Plugin-/Action-/Kategorie-Icons
│   ├── logs/                 ← Laufzeit-Logs (<uuid>.0.log)
│   ├── ui/                   ← Property Inspectors (HTML)
│   └── manifest.json
├── src/
│   ├── actions/<action>.ts
│   └── plugin.ts             ← Entry der Quellen
├── package.json
├── rollup.config.mjs
└── tsconfig.json
```
`src/` wird per Rollup nach `<uuid>.sdPlugin/bin/plugin.js` gebuendelt; der `.sdPlugin`-Ordner ist alles, was distribuiert wird. Quelle: docs.elgato.com/streamdeck/sdk/introduction/getting-started/ — **offiziell**

### A2. Klassisches Layout (SDKVersion 2, wie TVO) — offiziell
```
com.tvo.autoenter.sdPlugin/   ← Ordnername == manifest-UUID
├── manifest.json
├── plugin.html               ← CodePath (laedt code.js)
├── inspector.html            ← PropertyInspectorPath
└── icons/                    ← @1x + @2x PNGs
```
Kein Build-Schritt noetig — HTML/JS liegen direkt im Ordner. Quelle: docs.elgato.com/streamdeck/sdk/references/manifest/ — **offiziell**

### A3. manifest.json-Konventionen RICHTIG — offiziell
| Feld | Konvention |
|------|-----------|
| `UUID` | reverse-DNS, **nur** `[a-z0-9.-]` (lowercase), == Ordnername `<uuid>.sdPlugin` |
| Action-`UUID` | eindeutig UND mit Plugin-UUID praefixiert (`com.tvo.autoenter.toggle`) |
| `SDKVersion` | `2` (klassisch/lokal) oder `3` (empfohlen, Pflicht fuer DRM-Marketplace) |
| `Software.MinimumVersion` | praeziser String aus erlaubter Liste (`"6.4"`..`"7.4"`), **so niedrig wie moeglich**, nicht ueberhoehen |
| `Nodejs.Version` | nur `"20"` oder `"24"` (Pflicht fuer Node-Plugins), optional `"Debug":"enabled"` |
| `Version` | semantisch, **4-teilig** `{major}.{minor}.{patch}.{build}`, z.B. `"1.0.0.0"` |
| `OS` | Array, je Plattform mit `MinimumVersion` (`"windows":"10"`, `"mac":"13"`) |
| `Controllers` | je Action: `["Keypad"]`, `["Encoder"]` oder beides (case-sensitive!) |
| `Category` | sollte gleich dem Plugin-`Name` sein |
| `Icon`/`CategoryIcon` | **ohne** Dateiendung; `CodePath`/`PropertyInspectorPath` **mit** Endung |
Quelle: docs.elgato.com/streamdeck/sdk/references/manifest/ — **offiziell**

### A4. SDKVersion 2 vs. 3 — wann zwingend 3? — offiziell
- **`SDKVersion: 3` ist generell empfohlen** und **Pflicht fuer Marketplace-Distribution mit DRM** (ab Software 6.9): zusaetzlich `Software.MinimumVersion "6.9"`+ und `@elgato/streamdeck` v2+.
- DRM-Konsequenzen: Plugin-Dateien sind **immutable**, die **manifest.json ist zur Laufzeit nicht lesbar** → benoetigte Dateien zur Laufzeit generieren statt mitgelieferte zu modifizieren.
- `SDKVersion: 2` bleibt gueltig fuer klassische/lokale Plugins ohne DRM-Zwang (HTML/JS oder C++/Obj-C-CodePath). Quelle: docs.elgato.com/streamdeck/sdk/introduction/distribution/ — **offiziell**

---

## B. Action-Lifecycle sauber strukturieren

### B1. Event-Map und empfohlene Nutzung — offiziell
| Event | Empfohlene Nutzung |
|-------|--------------------|
| `willAppear` / `onWillAppear` | **Init**: State anlegen, Timer/Polling starten, initiales Bild/Titel setzen |
| `willDisappear` / `onWillDisappear` | **Cleanup**: Timer stoppen, `clearInterval`, State-Eintrag entfernen |
| `keyDown` / `keyUp` | Aktion ausloesen (kritische Aktionen auf `keyUp`, siehe B3) |
| `dialDown` / `dialUp` / `dialRotate` / `touchTap` | SD+ — **`dialPress` ist ab Software 6.5 entfernt** |
| `didReceiveSettings` | State aktualisieren, Anzeige neu rendern |

Handler duerfen `void` ODER `Promise<void>` zurueckgeben (async erlaubt). Quelle: docs.elgato.com/streamdeck/sdk/guides/actions/ · .../references/websocket/changelog/ — **offiziell**

### B2. `willAppear`/`willDisappear` feuern mehrfach — idempotent behandeln — offiziell
Dieselbe Action kann gleichzeitig auf mehreren Geraeten/Seiten existieren = mehrere lebende `context`. `willAppear` feuert jedes Mal beim Sichtbarwerden (Profil-/Seitenwechsel, Ordner, Startup). **Best Practice:** pro context idempotent registrieren (vorhandenen context nicht doppelt anlegen). Ab Software 6.5 enthaelt der Payload eine `controller`-Property (Standard- vs. Multi-Action-Kontext). Quelle: docs.elgato.com/streamdeck/sdk/guides/actions/ — **offiziell**

### B3. keyUp vs. keyDown + Debounce — offiziell + extern
- **Kritische / nicht rueckgaengig machbare Aktionen auf `keyUp`** („commit on release" — der User kann durch Wegziehen abbrechen). `keyDown` fuer sofortiges visuelles Feedback (`showOk()`/`showAlert()`).
- **Debounce-Idiom** pro context gegen Hardware-Bounce/Doppelevents:
```ts
const lastFired = new Map<string, number>();
override onKeyUp(ev: KeyUpEvent) {
  const id = ev.action.id, now = Date.now();
  if (now - (lastFired.get(id) ?? 0) < 300) return; // Sperre
  lastFired.set(id, now);
  // ... Aktion
}
```
Den Eintrag in `onWillDisappear` mit `lastFired.delete(id)` aufraeumen. Quelle: docs.elgato.com/streamdeck/sdk/guides/actions/ — **offiziell**; forum.keyboardmaestro.com (Bounce) — **extern**

---

## C. Context-Management & State (Single Source of Truth)

### C1. Pro `context` ein State-Eintrag — offiziell
Zwei gleichwertige Wege, je nach Persistenz-Bedarf:

**(a) Persistenter Per-Instanz-State → Action-Settings** (ueberlebt App-Neustart):
```ts
const s = await ev.action.getSettings<{count:number}>();
await ev.action.setSettings({ count: (s.count ?? 0) + 1 });
```
**(b) Laufzeit-State (Timer, Verbindungen) → Map, geschluesselt nach context:**
```ts
const state = new Map<string, { timer?: NodeJS.Timeout }>();
override onWillAppear(ev) {
  if (state.has(ev.action.id)) return;          // idempotent
  state.set(ev.action.id, { timer: setInterval(() => ev.action.setTitle("…"), 1000) });
}
override onWillDisappear(ev) {
  clearInterval(state.get(ev.action.id)?.timer); // Timer stoppen!
  state.delete(ev.action.id);                    // aufraeumen
}
```
Quelle: docs.elgato.com/streamdeck/sdk/guides/actions/ — **offiziell**

### C2. `context` korrekt verstehen — offiziell
- **Instanz-ID = `ev.action.id`** (Node) bzw. `context`-Feld (klassisch). Das ist die EINZIGE korrekte Identitaet.
- **Koordinaten NICHT als Identitaet** — sie aendern sich beim Verschieben und kollidieren auf SD+ (Keypad/Encoder gleiche Koordinaten).
- **`context` ist NICHT app-cycle-stabil**: „not guaranteed to persist across app cycles … should not be used as a long-term identifier externally." → nur Session-intern als Map-Key; Langzeit-Identitaet als eigene ID in `setSettings`. Quelle: docs.elgato.com/streamdeck/sdk/references/websocket/plugin/ — **offiziell**

### C3. Mehrere Instanzen derselben Action (Node-SDK) — offiziell
`SingletonAction` ist EINE Klasseninstanz, die alle Vorkommen verwaltet. Ueber `this.actions` alle sichtbaren Instanzen ansprechen; jeder Event traegt aber sein konkretes `ev.action`. Deshalb **pro-context-State, nie globaler Klassen-State** fuer Instanzdaten.
```ts
this.actions.forEach(a => a.setTitle("Updated!")); // alle sichtbaren Instanzen
```
Quelle: docs.elgato.com/streamdeck/sdk/guides/actions/ — **offiziell**

---

## D. WebSocket-Connect & Reconnect

### D1. Node-SDK: connect-Reihenfolge — offiziell
Alle Actions registrieren, DANN verbinden — `connect()` ist die letzte Zeile:
```ts
import streamDeck from "@elgato/streamdeck";
import { CounterAction } from "./actions/counter";
streamDeck.actions.registerAction(new CounterAction());
streamDeck.connect();   // immer als letztes
```
„register all of your plugin's actions before connecting to Stream Deck." Quelle: docs.elgato.com/streamdeck/sdk/guides/actions/ — **offiziell**

### D2. Klassisch: Handshake + Registrierung — offiziell
`connectElgatoStreamDeckSocket` synchron im globalen Scope definieren (Stream Deck ruft sie nach DOM-Load); Registrierung erst in `onopen`, mit dem mitgelieferten `event`-Namen:
```js
window.connectElgatoStreamDeckSocket = (port, uuid, event, info) => {
  const ws = new WebSocket(`ws://127.0.0.1:${port}`);
  ws.onopen = () => ws.send(JSON.stringify({ event, uuid }));   // sofort registrieren
  ws.onmessage = (m) => { const d = JSON.parse(m.data); handlers[d.event]?.(d); }; // Registry statt switch
};
```
Quelle: docs.elgato.com/streamdeck/sdk/references/websocket/plugin/ — **offiziell**

### D3. Reconnect-Disziplin (klassisch / eigene WS-Wrapper) — offiziell + extern
- Reconnect-Schleife im `onclose` (mit Backoff). WebSockets brechen bei Sleep/Wake, App-Neustart, USB-Trennung — kein garantierter Auto-Reconnect auf roher Socket-Ebene.
- **Event-Handler genau EINMAL binden** (ausserhalb des connect-Callbacks), sonst wird bei jedem Reconnect mehrfach gebunden → Events feuern doppelt.
- Beim Reconnect alte Socket sauber `close()`, neue Instanz, Register-Event nur EINMAL pro lebender Socket. Zustand neu aufbauen, alte `context` als ungueltig betrachten.
- **Polling-Plugins:** `EventSource`/SSE hat Auto-Reconnect eingebaut und ist nicht vom Background-Throttling betroffen (siehe L2). Quelle: docs.elgato.com/streamdeck/sdk/references/websocket/plugin/ — **offiziell**; websocket.org/guides/reconnection — **extern**

---

## E. Bidirektionale PI ↔ Plugin-Kommunikation

### E1. Erst senden, wenn die Gegenstelle bereit ist — offiziell
- Klassisch: PI sendet erst nach `onopen`; ausgehende Calls bis dahin in eine **Queue** legen (Nachrichten vor dem Handshake gehen still verloren).
- Plugin → PI: erst nach `propertyInspectorDidAppear` („The Property Inspector is guaranteed to be connected after PropertyInspectorDidAppear has fired"). Quelle: docs.elgato.com/streamdeck/sdk/references/websocket/ui/ — **offiziell**

### E2. Node-SDK: `streamDeck.ui.current` pruefen — offiziell-nah
Vor jedem Senden an den PI pruefen, ob er offen ist:
```ts
if (streamDeck.ui.current) streamDeck.ui.current.sendToPropertyInspector({ … });
```
Senden an einen geschlossenen PI liefert sonst Status `406`. Quelle: deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication (von Elgato bereitgestellt) — **offiziell-nah**

### E3. Empfohlen: fetch-like Routing (Node-SDK) — offiziell-nah
Strukturiertes request/response statt generischem Messaging; Routen FRUEH bei Init registrieren, Handler in try/catch, IMMER `{status, body}` antworten, Status VOR Body pruefen:
```ts
// Plugin: Route registrieren
streamDeck.ui.registerRoute("/api/config", (req) => ({ status: 200, body: cfg }));
// PI: fetch mit Statuspruefung
const res = await streamDeck.plugin.fetch("/api/config");
if (res.status === 200) use(res.body);
```
Status-Codes: `200` ok · `404` Route fehlt · `406` PI nicht sichtbar · `500` Handler-Exception. Quelle: deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication — **offiziell-nah**

### E4. Welcher Kanal wofuer? — offiziell
| Daten | Kanal | Warum |
|-------|-------|-------|
| Benutzer-Konfiguration einer Action | **Action-Settings** (`setSettings`) | persistiert automatisch, loest `didReceiveSettings` aus, mit Profil exportiert |
| API-Keys/OAuth-Tokens, plugin-weite Konfig | **Global-Settings** | lokal sicher gespeichert (siehe Q) |
| Live-Requests, dynamische Dropdown-Befuellung, einmalige Kommandos | **sendToPlugin / fetch-Routes** | kein Persistenz-Bedarf |
„setSettings is the preferred pattern for UI-to-plugin communication." → Konfig-Aenderungen aus dem PI ueber Settings, nicht `sendToPlugin`. Quelle: docs.elgato.com/streamdeck/sdk/guides/settings/ · .../guides/ui/ — **offiziell**

### E5. „PI offen?" — dem SDK-Lifecycle vertrauen — offiziell-nah
Nicht selbst aus rohen Appear/Disappear-Events ableiten (das SDK hat einen internen Debounce-Zaehler gegen Races). Wahrheit: `streamDeck.ui.current` (Node) bzw. die `propertyInspectorDidAppear`/`…DidDisappear`-Events (klassisch). Quelle: deepwiki.com/elgatosf/streamdeck/3.2-ui-plugin-communication — **offiziell-nah**

---

## F. Property Inspector UI mit sdpi-components

### F1. sdpi-components LOKAL einbinden — offiziell
`sdpi-components.js` herunterladen und **lokal neben die PI-HTML** legen (kein CDN — Absicht: konsistent + offline-faehig):
```html
<head lang="en"><meta charset="utf-8" />
  <script src="sdpi-components.js"></script>
</head>
<body>
  <sdpi-item label="Name"><sdpi-textfield setting="name"></sdpi-textfield></sdpi-item>
</body>
```
Download: `https://sdpi-components.dev/releases/v4/sdpi-components.js`. `const { streamDeckClient } = SDPIComponents;` fuer programmatischen Zugriff (selten noetig). Quelle: sdpi-components.dev — **offiziell**

### F2. Datenbindung per `setting`-Attribut — offiziell
Bidirektional + automatisch persistiert, **KEIN Save-Button, kein manuelles setSettings/getSettings**:
- `setting="username"` → `settings.username`
- Dot-Notation fuer nested: `setting="audio.volume"` → `settings.audio.volume`
- `value-type="string|number|boolean"` fuer korrekte Typkonvertierung
```html
<sdpi-item label="Volume">
  <sdpi-range setting="audio.volume" min="0" max="100" value-type="number"></sdpi-range>
</sdpi-item>
```
Quelle: sdpi-components.dev/docs/components — **offiziell**

### F3. Dynamische Selects vom Plugin laden — offiziell
Optionen NICHT hart in HTML, sondern per `datasource` vom Plugin:
```html
<sdpi-select setting="deviceId" datasource="getDevices" hot-reload
             loading="Loading…" show-refresh></sdpi-select>
```
`datasource` laedt remote, `hot-reload` reagiert auf `sendToPropertyInspector`. Quelle: sdpi-components.dev/docs/components/select — **offiziell**

### F4. PI-i18n (sdpi-components ab v2.1.0) — offiziell
Der PI lokalisiert NICHT ueber die SDK-Manifest-Sprachdateien, sondern ueber die Lib selbst:
```js
SDPIComponents.i18n.locales = { en:{name:"Name"}, de:{name:"Name"} };
```
Markup: `<sdpi-item label="__MSG_name__">`. Fallback: fehlende Locale → en → roher Key. Quelle: sdpi-components.dev/docs/helpers/localization — **offiziell**

### F5. Wann handgeschriebener PI? — offiziell + abgeleitet
sdpi-components ist der **Default** (Auto-Binding, nativer Look, i18n, automatische WS-Registrierung). Plain-HTML nur fuer Layouts, die das Komponenten-Set nicht abdeckt — dann verliert man Auto-Binding/Styling/i18n. **React/Svelte/eigene Frameworks sind NICHT offiziell dokumentiert** — fuer Marktplatz-Konsistenz bei sdpi-components bleiben. Quelle: docs.elgato.com/streamdeck/sdk/guides/ui/ — **offiziell**

---

## G. Settings-Persistenz (robust)

### G1. `setSettings` ersetzt das GANZE Objekt — offiziell
Kein Merge. Immer aktuelle Settings aus Event-Args nehmen, Feld aendern, KOMPLETTES Objekt zurueck:
```ts
const current = ev.payload.settings;
await ev.action.setSettings({ ...current, count: (current.count ?? 0) + 1 });
```
Klassisch: zuletzt empfangenes `payload.settings` zwischenspeichern und vor dem Senden spreaden — die WS-Schicht merged nicht. Quelle: docs.elgato.com/streamdeck/sdk/guides/settings/ — **offiziell**

### G2. Settings aus Lifecycle-Event-Args lesen — offiziell
`ev.payload.settings` (in `willAppear`/`keyDown`/`didReceiveSettings`) ist sofort da. `getSettings()` nur als Promise, nur wenn die Action nicht sichtbar ist. Klassisch: `getSettings` ist asynchron → Ergebnis kommt erst als `didReceiveSettings`-Event, nie als Rueckgabewert. Quelle: docs.elgato.com/streamdeck/sdk/guides/settings/ — **offiziell**

### G3. Action- vs. Global-Settings — offiziell (sicherheitskritisch)
| | Action-Settings | Global-Settings |
|---|---|---|
| Scope | eine Instanz | plugin-weit |
| Speicherung | **Klartext**, im Profil-Export | sicher lokal auf der Maschine |
| Wofuer | Per-Action-State (Zaehler, Label) | API-Keys, OAuth-Tokens, plugin-weite Konfig |
Secrets IMMER global, nie Action-Settings, nie ins Bundle. Quelle: docs.elgato.com/streamdeck/sdk/guides/settings/ — **offiziell**

### G4. `didReceiveGlobalSettings` feuert erst nach `getGlobalSettings` — offiziell
Beim Start passiert nichts von selbst → aktiv anstossen:
```ts
streamDeck.settings.onDidReceiveGlobalSettings<G>((ev) => { token = ev.settings.token; });
await streamDeck.settings.getGlobalSettings();  // sonst feuert der Listener nie
```
Quelle: docs.elgato.com/streamdeck/sdk/references/websocket/ui/ — **offiziell**

### G5. Debounce beim Tippen — offiziell (mit Einschraenkung)
Eigener PI: Texteingaben debouncen (~150–300 ms) bevor `setSettings` (jeder Call = WS-Roundtrip + ganzes Objekt). **sdpi-components persistiert automatisch via `setting`** — die offizielle Doku nennt aber KEINEN konkreten Debounce-Wert; wer ein garantiertes Debounce braucht, baut es bei eigenem PI selbst. Quelle: docs.elgato.com/streamdeck/sdk/guides/settings/ · sdpi-components.dev — **offiziell**

### G6. Typsicherheit + Defaults (Node-SDK) — offiziell
Generisches Settings-Interface ueberall durchreichen (Constraint `JsonObject`), aber **TS-Typen sind keine Runtime-Garantie** → defensiv lesen oder per Schema (Zod) validieren:
```ts
const { name = "Default" } = ev.payload.settings;          // Destructuring-Default
const s = Schema.parse(ev.payload.settings);               // robust (Zod .default())
```
Defaults beim LESEN anwenden, nicht eager schreiben. Quelle: docs.elgato.com/streamdeck/sdk/guides/settings/ — **offiziell**

### G7. Experimentelle Message-Identifiers (SDK 2.0+ / SD 7.1+) — offiziell
```ts
streamDeck.settings.useExperimentalMessageIdentifiers = true;
```
Dann feuert `onDidReceive[Global]Settings` **nur bei echten Aenderungen** (nicht als Nebeneffekt eines `getSettings()`). WS-Detail: das `id`-Feld im `didReceiveSettings`-Event ist `undefined`, wenn die Aenderung aus dem PI kam — so unterscheidet man Request-Antwort von PI-Aenderung. Bei SD 7.4.2 verfuegbar. Quelle: docs.elgato.com/streamdeck/sdk/guides/settings/ — **offiziell**

---

## H. States, setTitle & dynamisches Rendering

### H1. States vs. setImage — wann was — offiziell
- **Mehrere Manifest-`States`** nur fuer echte **Toggles** (an/aus, mute) — das Framework toggelt automatisch; der User kann Icons pro State in der App konfigurieren. Bei Eigensteuerung `"DisableAutomaticStates": true` und State ausschliesslich selbst per `setState(0/1)` setzen (so spiegelt er den echten Backend-Zustand, verfuegbar ab Software 6.4).
- **`setImage()` dynamisch** fuer alles Berechnete (Zaehler, Prozent, Live-Werte, Diagramme). Quelle: docs.elgato.com/streamdeck/sdk/guides/keys/ — **offiziell**

### H2. Dynamische Taste = ganze Taste als SVG + `setTitle("")` — offiziell + extern
Das stabilste Muster fuer volle Kontrolle (umgeht den persistenten Titel-Cache):
```ts
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" viewBox="0 0 72 72">
  <rect width="72" height="72" fill="#1e1e2e" rx="8"/>
  <text x="36" y="42" font-size="22" font-weight="700" fill="#F97316" text-anchor="middle">42</text>
</svg>`;
await action.setImage(`data:image/svg+xml;utf8,${encodeURIComponent(svg)}`);
await action.setTitle("");   // Titel-Region freiraeumen
```
- `;utf8,encodeURIComponent(...)` ist kleiner/schneller als Base64; SDK akzeptiert beide.
- `setImage`-Formate: SVG, PNG, JPEG, WEBP — **kein animiertes GIF** (GIF nur als statisches State-Image). Quelle: docs.elgato.com/streamdeck/sdk/guides/keys/ — **offiziell**; nick-liu.com/posts/streamdeck-sdk-quirks/ — **extern**

### H3. `setTitle` vs. SVG — Prioritaet beachten — offiziell
Render-Prioritaet: (1) vom User gesetzter Titel/Bild > (2) Runtime `setTitle`/`setImage` > (3) Manifest-Default. Sobald dynamische Titel zwingend sind oder Alignment programmatisch stimmen muss → SVG-`setImage`. **Falle:** `setTitle("")` bei JEDEM Update loescht den vom User gesetzten Titel — nur bei echtem Status-Text setzen. `UserTitleEnabled: false` im Manifest, falls User-Override unerwuenscht. Quelle: docs.elgato.com/streamdeck/sdk/guides/keys/ — **offiziell**

---

## I. Icons / Images — Spezifikationen

### I1. Specs (PNG quadratisch transparent, Pfad ohne Endung) — offiziell
| Bild | Feld | @1x | @2x | Stil |
|------|------|-----|-----|------|
| Plugin-Icon | `Icon` | 256×256 | 512×512 | PNG, aussagekraeftig |
| Category-Icon | `CategoryIcon` | 28×28 | 56×56 | SVG empf., **monochrom weiss #FFFFFF transparent** |
| Action-Listen-Icon | Action `Icon` | 20×20 | 40×40 | SVG empf., **monochrom weiss #FFFFFF transparent** |
| State/Key-Image | State `Image` | 72×72 | 144×144 | SVG/PNG/GIF, farbig |
| Encoder-Icon | Encoder `Icon` | 72×72 | 144×144 | SVG/PNG |
| Encoder-Background | Encoder `background` | 200×100 | 400×200 | SVG/PNG |
SVG ist fuer Category/Action/Key empfohlen (deckt @1x+@2x ab). Listen-/Category-Icons MUESSEN monochrom weiss transparent sein (werden getoent). Immer beide Groessen liefern, quadratisch (sonst gestreckt). Quelle: docs.elgato.com/streamdeck/sdk/references/manifest/ · docs.elgato.com/guidelines/stream-deck/plugins/ — **offiziell**

---

## J. SD+ Encoder & Touchscreen (Stream Deck +)

### J1. Layouts & Feedback — offiziell
Canvas pro Encoder-Segment: **200×100 px**. Layout per Manifest oder `setFeedbackLayout("$B1")`/Custom-JSON; Werte per `setFeedback({ key: value })` (nur genannte Items aendern sich).

Vordefinierte Layouts: `$X1` (Titel+Icon), `$A0` (Titel+Vollbild), `$A1` (Titel+Icon+Wert), `$B1` (+Balken), `$B2` (Gradient-Balken), `$C1` (Doppel-Icon+Doppel-Balken). Custom-Item-Typen: `text`, `pixmap` (Pfad/base64/SVG), `bar`, `gbar`.

Events: `onDialDown`/`onDialUp` (Druck — **nicht** `dialPress`), `onDialRotate` (mit `ticks`), `onTouchTap`. `TriggerDescription` im Manifest fuehrt den User (Push/Rotate/Touch/LongTouch), programmatisch `setTriggerDescription()`. Quelle: docs.elgato.com/streamdeck/sdk/guides/dials/ · .../references/touch-strip-layout/ — **offiziell**

### J2. Touch-Targets & Canvas-Grenzen — offiziell
Interaktive Elemente min. **35×35 px**, alle Items im 200×100-Canvas. Out-of-bounds-Items oder zu kleine Targets → **das Layout laedt gar nicht**. Vorab `streamdeck validate`. (Almanach B2.) Quelle: docs.elgato.com/streamdeck/sdk/references/touch-strip-layout/ — **offiziell**

---

## K. Node.js-Backend Sandbox-konform

### K1. Eingeschraenkte Runtime — offiziell
Node-Plugins laufen mit **`--no-addons`** (keine nativen `.node`-Module → Pure-JS/WASM-Alternative, z.B. `sql.js` statt `better-sqlite3`), **`--no-global-search-paths`** (alle Deps bundeln, kein globales `node_modules`), **`--enable-source-maps`**. Gegen die **gebundelte** Node-Version (20.20.0/24.13.1) testen, nicht gegen lokal installiertes Node. Quelle: docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/ — **offiziell** (Flag-Liste teils aus Versions-Anker abgeleitet)

### K2. Globale Fehler-Handler — offiziell
Ein nicht abgefangener Fehler killt den Prozess → Stream Deck startet sofort neu → reproduzierbarer Start-Crash = Restart-Loop. Ganz oben im Entry:
```ts
process.on("uncaughtException",  (e) => streamDeck.logger.error("uncaught", e));
process.on("unhandledRejection", (r) => streamDeck.logger.error("unhandledRejection", r));
```
Async in Handlern immer try/catch, kein Top-Level-`throw`. Quelle: docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/ — **offiziell**

### K3. `@action`-UUID = manifest-UUID — offiziell
Der String in `@action({ UUID })` muss 1:1 mit `Actions[].UUID` im Manifest uebereinstimmen, sonst keine `willAppear`. `CodePath` (z.B. `bin/plugin.js`) muss auf die gebaute Entry zeigen, manifest im `.sdPlugin`-Root. Quelle: docs.elgato.com/streamdeck/sdk/references/manifest/ — **offiziell**

---

## L. Performance & Push-statt-Poll

### L1. Max 10 Updates/s, Coalescing/Dedup — offiziell
Guideline: „Limit programmatic updates to maximum 10 calls per second" (gilt fuer `setImage`/`setState`/`setTitle`/Touch). Nur bei echter Aenderung senden:
```ts
let last = "";
function setImageIfChanged(action, url) { if (url === last) return; last = url; action.setImage(url); }
```
Keine hohe Framerate / Animationsschleifen. Quelle: docs.elgato.com/guidelines/stream-deck/plugins/ — **offiziell**

### L2. Push statt Poll (SSE) — offiziell (Architektur) + extern (Throttling)
Chromium drosselt `setInterval`/`setTimeout` in nicht-sichtbaren Webviews (bis ~1×/min). Der Plugin-Webview/PI ist so eine Seite. **Empfohlen:** Statusspiegelung per **SSE (`EventSource`)** — vom Background-Throttling NICHT betroffen (Server pusht), Auto-Reconnect eingebaut. Lange Live-Logik gehoert ins Node-Backend (kein Webview-Throttling). Quelle: docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/ — **offiziell**; developer.chrome.com/blog/timer-throttling-in-chrome-88 — **extern**

---

## M. Logging & Debugging

### M1. Logging (Node-SDK) — offiziell
`streamDeck.logger` statt `console.log` (schreibt in alle Targets inkl. Log-Datei). 5 Levels (`error/warn/info/debug/trace`); Production-Minimum `DEBUG` (`TRACE` nur Dev). Scoped Logger fuer Struktur:
```ts
streamDeck.logger.setLevel(LogLevel.DEBUG);
const log = streamDeck.logger.createScope("Sync"); log.info("connected");
```
Logs: `<uuid>.sdPlugin/logs/<uuid>.0.log` (rotierend, 10 Dateien). Klassisch: kein Datei-Logger — PI-`console` + Remote-Debugger. Quelle: docs.elgato.com/streamdeck/sdk/guides/logging/ — **offiziell**

### M2. Remote-Debugging — offiziell + extern
- Modern: `streamdeck dev` (an) / `streamdeck dev --disable` (aus) — aktiviert PI-Debug + Node-Attach.
- PI-DevTools: `http://localhost:23654/` — **PI muss in Stream Deck sichtbar sein**, sonst erscheint die Seite nicht.
- Manuell (ohne CLI): Windows DWORD `html_remote_debugging_enabled = 1` unter `HKCU\Software\Elgato Systems GmbH\StreamDeck`; macOS `defaults write com.elgato.StreamDeck html_remote_debugging_enabled -bool YES`; danach App neu starten. Quelle: docs.elgato.com/streamdeck/cli/commands/dev/ · .../guides/ui/ — **offiziell**; streamdecklabs.com — **extern**

---

## N. Build-Toolchain & CLI-Workflow

### N1. @elgato/cli-Befehle — offiziell
| Befehl | Zweck |
|--------|-------|
| `streamdeck create` | Scaffold (komplette Node-Struktur) |
| `streamdeck link [path]` | `.sdPlugin` mit Stream Deck verlinken (Entwicklung) |
| `streamdeck restart <uuid>` (`r`) | Hot-Reload (stoppt + startet) |
| `streamdeck dev [--disable]` | Developer Mode |
| `streamdeck validate [path]` | Plugin validieren (Pflicht vor pack) |
| `streamdeck pack` / `bundle` | `.streamDeckPlugin` erzeugen (validiert intern zuerst) |
Dev-Flow: `create` → `link` → `npm run watch` (Rollup-Rebuild + `restart <uuid>`) → `validate` → `pack`. Quelle: docs.elgato.com/streamdeck/cli/intro/ — **offiziell**

### N2. TypeScript + Bundling — offiziell + extern
Build = `rollup -c` (`rollup.config.mjs`), Output `<uuid>.sdPlugin/bin/plugin.js`. **Alle Deps MUESSEN ins Bundle** (`--no-global-search-paths`) → `@rollup/plugin-node-resolve` + `@rollup/plugin-commonjs` + `@rollup/plugin-json` (das Scaffold bringt sie mit). `tsconfig` passend zu Node 20/24, `strict: true`. Quelle: docs.elgato.com/streamdeck/sdk/introduction/getting-started/ — **offiziell**; @elgato/streamdeck npm/GitHub — **extern**

### N3. Watch-Script-Platzhalter ersetzen — offiziell
Das Template-Watch-Script (`--watch.onEnd="streamdeck restart {{UUID}}"`) hat einen Platzhalter — die echte Plugin-UUID einsetzen, sonst greift der Restart kein/falsches Plugin. Quelle: docs.elgato.com/streamdeck/cli/commands/restart/ — **offiziell**

---

## O. Distribution & Packaging

### O1. Nur `validate` → `pack`, NIE `Compress-Archive` — offiziell
- `streamdeck pack` validiert intern, bundelt den `.sdPlugin`-Ordner (= ZIP-Root) und erzeugt die `.streamDeckPlugin`. Flags: `-o <dir>`, `-f`, `--version <semver>`, `--dry-run` (Vorab-Report).
- **`Compress-Archive` (PowerShell) NIEMALS** zum Packen — es schreibt Backslash-Pfadtrenner ins ZIP → Stream Deck kann das Plugin u.U. nicht laden (Almanach K3). Immer `streamdeck pack`.
```bash
streamdeck validate ./com.acme.x.sdPlugin
streamdeck pack --dry-run ./com.acme.x.sdPlugin
streamdeck pack --version 1.2.0.0 -o ./dist ./com.acme.x.sdPlugin
```
Quelle: docs.elgato.com/streamdeck/cli/commands/pack/ · .../validate/ — **offiziell**

### O2. `.sdignore` + saubere ZIP — offiziell
`.sdignore` neben dem Manifest (`.gitignore`-Syntax). Default-Excludes: `.git`, `/.env*`, `*.log`, `*.js.map`. Plattform-Muell (`.DS_Store`, `__MACOSX/`, `Thumbs.db`) zusaetzlich ausschliessen. `manifest.json` als **UTF-8 OHNE BOM**. Quelle: docs.elgato.com/streamdeck/cli/commands/pack/ · .../introduction/distribution/ — **offiziell**

---

## P. Marketplace-Konventionen

### P1. Submission-Regeln — offiziell
- **Icons:** Plugin 256/512 PNG; Action-Listen-Icons SVG (o. 20/40 PNG) monochrom weiss transparent; Key 72/144.
- **PI:** KEIN Save-Button (Auto-Persistenz), keine Donation-/Sponsor-Links, keine Copyright-Listings — Support/Links gehoeren ins Marketplace-Listing.
- **Naming:** Plugin-Name eindeutig, englisch, beschreibend, ohne Organisationsname; Action-Namen ≤30 Zeichen; mind. 1 Preview-Bild; `Category` Pflicht.
- **UUID nach Veroeffentlichung NIE aendern** (sonst verschwinden User-Konfigurationen) — zum Deprecaten `VisibleInActionsList: false`. Review-Dauer 4–10 Werktage. Quelle: docs.elgato.com/guidelines/stream-deck/plugins/ — **offiziell**

---

## Q. Sicherheit

### Q1. Keine Secrets, sensibles via globalSettings — offiziell
„Stream Deck plugins run locally … it is not recommended to include secrets, for example private API keys." Keys/Tokens nie hardcoden, nie committen, nie in der `.streamDeckPlugin`-ZIP. Sensibles via `setGlobalSettings` (verschluesselt lokal), nie Action-Settings (Klartext, Profil-Export). Plugin-EIGENE Secrets gehoeren gar nicht auf die Client-Maschine — serverseitig/OAuth. Vgl. Repo-Regel `secrets-in-sk-folder.md`. Quelle: docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/ · .../guides/settings/ — **offiziell**

---

## 🔗 Wechselseitige Bezugstabelle (Best Practice ↔ Almanach)

> Verknuepft jede Best-Practice mit der konkreten Bug-Loesung in [`bugs/peripherie/stream-deck.md`](../../bugs/peripherie/stream-deck.md).

| Best-Practice-Abschnitt (hier) | Almanach-Bug-Abschnitt (bugs/peripherie/stream-deck.md) |
|--------------------------------|----------------------------------------------|
| A. Projekt-/Manifest-Struktur & SDK-Wahl | A1–A10, K2, A7 (MinimumVersion) |
| B. Action-Lifecycle | D1, D2, D5, D6 |
| C. Context-Management & State | D3, D4, E4, G10 |
| D. WebSocket-Connect & Reconnect | C1–C5 |
| E. PI ↔ Plugin-Kommunikation | F2, F3, H1–H4 |
| F. Property Inspector UI (sdpi) | F1, F4, F5, F6 |
| G. Settings-Persistenz | G1–G10 |
| H. States, setTitle, SVG-Rendering | E1, E2, E3, E5, Z1, Z4, Z5 |
| I. Icons / Images | B1, O1 |
| J. SD+ Encoder & Touchscreen | A9, D6, **B2** |
| K. Node.js-Backend Sandbox | I1–I7 |
| L. Performance & Push-statt-Poll | E5, M1, M2, Z6, Z7 |
| M. Logging & Debugging | J6, J7, L1 |
| N. Build-Toolchain & CLI | J1, J3, J4, J5, I6 |
| O. Distribution & Packaging | K1, K2, K3, K4, K5, K6, K7 |
| P. Marketplace-Konventionen | O1–O4, A4 |
| Q. Sicherheit | G5, N2 |

---

## Quellen (Stand 2026-06-03)

**Offiziell (Grundwahrheit):**
- docs.elgato.com/streamdeck/sdk/introduction/getting-started/
- docs.elgato.com/streamdeck/sdk/introduction/plugin-environment/
- docs.elgato.com/streamdeck/sdk/introduction/distribution/
- docs.elgato.com/streamdeck/sdk/guides/actions/
- docs.elgato.com/streamdeck/sdk/guides/settings/
- docs.elgato.com/streamdeck/sdk/guides/ui/
- docs.elgato.com/streamdeck/sdk/guides/keys/
- docs.elgato.com/streamdeck/sdk/guides/dials/
- docs.elgato.com/streamdeck/sdk/guides/logging/
- docs.elgato.com/streamdeck/sdk/guides/i18n/
- docs.elgato.com/streamdeck/sdk/references/manifest/
- docs.elgato.com/streamdeck/sdk/references/websocket/plugin/
- docs.elgato.com/streamdeck/sdk/references/websocket/ui/
- docs.elgato.com/streamdeck/sdk/references/websocket/changelog/
- docs.elgato.com/streamdeck/sdk/references/touch-strip-layout/
- docs.elgato.com/streamdeck/cli/intro/ · .../commands/{pack,validate,link,restart,dev}/
- docs.elgato.com/guidelines/stream-deck/plugins/
- sdpi-components.dev (Elgato-eigenes UI-Lib-Projekt)
- deepwiki.com/elgatosf/streamdeck (von Elgato bereitgestellt — offiziell-nah)

**Extern (abwaegbare Alternative):**
- nick-liu.com/posts/streamdeck-sdk-quirks/ (Titel-Cache, SVG-Loesung, willAppear-Quirk)
- streamdecklabs.com (Remote-Debugging klassisch)
- developer.chrome.com/blog/timer-throttling-in-chrome-88 (Webview-Throttling)
- websocket.org/guides/reconnection

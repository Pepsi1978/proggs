# Bekannte Bugs & Fallen: Chrome-Erweiterungen (Manifest V3)

> **PFLICHT-LESEN vor JEDER Arbeit an einer Chrome-Erweiterung.**
> Kuratierter Bug-Almanach fuer Chrome/Edge-Erweiterungen (Manifest V3). Wird VOR dem
> Coden gelesen, damit bekannte Fehler gar nicht erst gemacht werden (Poka-Yoke Stufe 3).
> Quelle: eigene Vorfaelle (BestJournal/overlays-Repo) + breite Recherche (13 Researcher,
> offizielle Chrome-Developer-Doku zuerst, GitHub-Issues hart per `gh` geprueft).
> Ergaenzung: nach jedem neuen Chrome-Extension-Bug hier einen Eintrag hinzufuegen.
>
> **Stand:** recherchiert am **2026-06-02**, **re-recherchiert/verifiziert am 2026-07-02** (Engine A: Firecrawl+MiniMax)
> fuer **Chrome 149** (live installiert: **Chrome 149.0.7827.115**; Anker war 148). Das `Versionen:`-Feld pro Bug
> sagt, ab wann etwas gilt/gefixt ist. **Re-Recherche 2026-07-02:** Der Sprung 148→149 brachte KEINE neuen/
> geaenderten Extension-APIs (Chrome-149-Release-Notes ohne Extension-relevante Aenderungen); SW-Lifecycle, DNR-Limits
> und Permissions unveraendert. Aktualisiert wurden die **MV2-Endgueltig-Timeline** (§N.64: 31.08.2026 Web-Store-Entfernung)
> und der **Store-Review-Stau seit April 2026** (§O.67).
>
> Gegenstueck (Praevention, „so baut man es von vornherein richtig"):
> `best-practices/web/chrome-extensions.md` (Bezugs-Tabelle unten).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektuere
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Erweiterung verschwindet/instabil nach Neustart | ZUERST Ordner-Pfad wechseln, NICHT ID/Code | #1 |
| 2 | Laedt nicht / instabil | Keine `_`-Dateien im Ordner (ausser `_locales`) | #2 |
| 3 | State (Login/Abo/Tab) ploetzlich weg | Nie globale Vars → `chrome.storage`; SW stirbt nach 30s | #3 |
| 4 | Event feuert nach SW-Neustart nicht | Alle Listener synchron im Top-Level registrieren | #5 |
| 5 | Timer feuert nie | `chrome.alarms` statt `setTimeout`/`setInterval` (min 30s) | #6, #7 |
| 6 | „message port closed before response" | Async `onMessage`: `return true` (oder Promise ab 148) | #14 |
| 7 | Erweiterung reagiert nicht in offenen Tabs | Per `executeScript` nachinjizieren + Doppel-Guard | #17 |
| 8 | Seiten-JS-Variable ist `undefined` | Content-Script im Isolated World; `world:"MAIN"` noetig | #18 |
| 9 | „Extension context invalidated" | `chrome.runtime?.id` pruefen / try-catch + Cleanup | #19 |
| 10 | Mikrofon/Audio geht nicht | Nicht im SW → Offscreen-Doc/Content-Script | #48 |
| 11 | Button wird rot, aber kein Ton | ZUERST Chrome-Mic-Auswahl pruefen, NICHT den Code | #54 |
| 12 | WebSocket-Handshake schlaegt fehl | Host als `wss://` in `host_permissions` (MV3) | #53 |
| 13 | `storage.sync.set` QUOTA-Fehler | 8 KB/Item: pro Datensatz EIN Key | #28, #37 |
| 14 | Erweiterung nach Update deaktiviert | Neue Permission als `optional_permissions` | #40 |
| 15 | Sicherheit / Store | `sender` validieren, keine Secrets im Bundle; MV2 ist tot (≥139) | #55, #58, #64 |
| 16 | STT-Live-Vorschau springt im Feld / wird statt finaler Fassung gesendet | Vorschau ins schwebende Overlay (NICHT ins `contenteditable`); `previewActive`-Riegel: nur finale Whisper-Fassung ins Feld | #74 |

---

## A — Laden, Verschwinden, Ordner

### 1. Erweiterung verschwindet nach jedem Browser-Neustart  ⭐ HAEUFIG
**Symptom:** Unpacked-Erweiterung verschwindet komplett aus `chrome://extensions` nach jedem
Neustart. Laeuft waehrend der Session, KEIN Fehler, andere unpacked Erweiterungen bleiben,
Entwicklermodus bleibt an.
**Ursache:** Korrupter per-Erweiterung-Eintrag in Chromes „Secure Preferences", gebunden an den
**Ordner-PFAD**. NICHT die Extension-ID, NICHT der Code, NICHT der Ordner-Inhalt, NICHT die Sync-Quota.
**Versionen:** Chromium-weit (Chrome + Edge), unpacked-Modus, MV2 & MV3 — Stand 2026 nicht behoben (Chromium Issue 40227980).
**FIX (zuerst!):** **Ordner umbenennen / an neuen Pfad verschieben** (`git mv altname neuname`, im Repo
behalten), dann aus dem NEUEN Pfad per „Entpackt laden" neu laden. NICHT an key/ID/Code herumdoktern —
verschiedene IDs am gleichen Pfad sind derselbe Test.
**Quelle:** eigener Vorfall 2026-06-01 (ChromeOverlays→overlays geloest) + Recherche.

### 2. Erweiterung laedt nicht: `_`-Prefix-Dateien im Ordner
**Symptom:** Chrome lehnt das Laden ab / Erweiterung instabil.
**Ursache:** Chrome verbietet Dateien/Ordner mit fuehrendem `_` (z.B. `__pycache__`, `_helper.py`).
Einzige Ausnahmen: `_locales`, `_metadata`.
**Versionen:** alle Chrome/Edge-Versionen (per Design, kein Fix vorgesehen).
**FIX:** Keine `_`-Dateien im Extension-Ordner. Build-/Helfer-Skripte in `.tools/` legen (Chrome
ignoriert `.`-Ordner). `.gitignore`: `__pycache__/`, `*.pyc`, `_*.py`.
**Quelle:** eigener Vorfall (trat 2x auf) + Doku.

---

## B — Service-Worker-Lebenszyklus (der MV3-Kern, groesste Fehlerquelle)

### 3. SW stirbt nach ~30 s Idle — In-Memory-State weg  ⭐ HAEUFIG
**Symptom:** Globale Variablen (gecachter Login-/Abo-Status, Counter, Caches) sind nach kurzer
Idle-Zeit ploetzlich leer; Paid-User sehen z.B. Free-Limits.
**Ursache:** Chrome terminiert den SW nach **30 s Inaktivitaet**. Jedes Event / jeder `chrome.*`-API-Call
setzt den Timer zurueck. Der globale Scope wird beim Neustart neu ausgefuehrt → aller In-Memory-State weg.
**Versionen:** per Design, alle MV3-Versionen.
**FIX:** State NIE in globalen Variablen halten → in `chrome.storage.local`/`.session` (oder IndexedDB)
schreiben und beim SW-Start rehydrieren. SW als zustandslosen, jederzeit neu startbaren Mittler behandeln.
**Beispiel (eigener Vorfall 2026-06-05, vorlese-overlay-v2):** Der SW merkte sich den Ziel-Tab fuer
Status-Updates in der globalen Variable `activeTabId`. Bei langem Vorlesen (Audio laeuft im
Offscreen-Doc, der SW ist derweil idle) starb der SW nach 30 s; beim abschliessenden `OFFSCREEN_ENDED`
war `activeTabId` null → das `stopped` ging an `notifyTab(null)` verloren → das Overlay hing fuer immer
im "spielt"-Zustand (rotes Stop-Icon + Fortschrittsbalken blieben, neuer markierter Text wurde nicht
mehr vorgelesen, weil `if (isPlaying) return` ihn blockierte). FIX: `activeTabId` in
`chrome.storage.session` sichern + per `resolveActiveTab()` nachladen; zusaetzlich ein grosszuegiger
Watchdog im Content-Script als Selbstheilung, falls `stopped` doch verloren geht. Merkregel: JEDER
Tab/State, den der SW fuer ein SPAETERES Event (Ende/Timeout/Callback) braucht, MUSS persistiert werden —
nicht nur Login/Abo/Caches.
**Quelle:** developer.chrome.com — service-workers/lifecycle; eigener Vorfall 2026-06-05.

### 4. 5-Minuten-Hardlimit pro Request / 30 s `fetch`-Timeout
**Symptom:** Lange Operation/Event-Handler bricht nach 5 Min ab; `fetch()` das >30 s auf die Antwort
wartet killt den Worker.
**Ursache:** SW wird beendet, wenn ein einzelner Request >5 Min braucht ODER eine `fetch()`-Response
>30 s auf sich warten laesst.
**Versionen:** per Design. User-Prompt-APIs sind vom 5-Min-Limit ausgenommen (ab Chrome 116).
**FIX:** Lange Arbeit in Chunks teilen, Fortschritt in Storage zwischenspeichern; bei langsamen
Endpunkten eigenes Timeout + Retry. Es gibt keinen Weg, die Grenzen selbst aufzuheben — nur resilientes Design.
**Quelle:** developer.chrome.com — service-workers/lifecycle.

### 5. Event-Listener nicht synchron im Top-Level registriert → Event verpasst  ⭐ HAEUFIG
**Symptom:** Events (`onMessage`, `onClicked`, `onAlarm`, `onInstalled` …) feuern nach einem
SW-Neustart nicht; beim ersten Start scheinbar ok.
**Ursache:** Beim Wiederaufwachen dispatcht Chrome das Event sofort. Listener, die erst spaet/asynchron
(in `await`, Promise, `setTimeout`, verschachtelt in `onInstalled`) registriert werden, sind dann noch
nicht aktiv → Event wird verworfen. Top-Level-`await` ist im SW absichtlich deaktiviert.
**Versionen:** per Design, alle MV3-Versionen.
**FIX:** ALLE `chrome.*.addListener(...)` synchron im obersten Modul-Scope registrieren (erste
Event-Loop-Iteration), VOR jedem Async-Call. Die Handler-Funktion selbst darf async sein.
**Quelle:** developer.chrome.com — service worker events; chromium-extensions Group.

### 6. `setTimeout`/`setInterval` ueberleben SW-Termination NICHT
**Symptom:** Geplanter Timer feuert nie / bricht mittendrin ab.
**Ursache:** JS-Timer leben nur im SW-Speicher; mit der Termination weg. `localStorage`+`setInterval`
haelt den SW NICHT am Leben.
**Versionen:** per Design.
**FIX:** `chrome.alarms` statt JS-Timern (weckt den SW). Alarm-Listener top-level registrieren (Punkt 5).
Kurze Timer (<30 s innerhalb eines aktiven SW) sind weiterhin ok.
**Quelle:** chromium-extensions Group; w3c/ServiceWorker #838.

### 7. `chrome.alarms`-Mindestintervall 30 s (frueher 60 s)
**Symptom:** Alarm mit `periodInMinutes < 1` wurde frueher ignoriert + Warnung; 45-s-Timing unmoeglich.
**Ursache:** Vor Chrome 120 war das kuerzeste Intervall 1 min — schlecht mit dem 30-s-Idle-Timeout vereinbar.
**Versionen:** geaendert ab **Chrome 120** (Minimum 30 s, `periodInMinutes: 0.5`). Unter 0.5 wird bei
gepackten Extensions nicht honoriert (+ Warnung); unpacked kein Limit.
**FIX:** Auf Chrome 120+ `periodInMinutes: 0.5` fuer 30 s; sub-30-s ist nicht zuverlaessig moeglich.
**Quelle:** developer.chrome.com — Chrome 120 what's new / alarms API.

### 8. Welche Aktivitaet den Idle-Timer zuruecksetzt — versioniert (Annahme-Falle)
**Symptom:** Ein Keep-Alive-Trick funktioniert auf neuem Chrome, auf aelterem nicht (oder umgekehrt).
**Ursache:** Die „lebensverlaengernden" Quellen kamen schrittweise dazu.
**Versionen (Mindest-Chrome je Quelle):** Native Messaging **105+**, Offscreen-Doc-Messages **109+**,
Extension-API-Calls **110+**, langlebige Messaging-Ports **114+**, WebSocket **116+**, aktive
`chrome.debugger`-Session **118+**. (Auf Chrome 148 alle aktiv.) SW wird NIE terminiert, solange DevTools
offen sind → Heisenbug (Punkt 53).
**FIX:** Keep-Alive-Strategie an die gewuenschte `minimum_chrome_version` koppeln.
**Quelle:** developer.chrome.com — service-workers/lifecycle.

### 9. Keep-Alive: was offiziell ist und was Hack
**Symptom:** SW soll waehrend aktiver Nutzung laenger leben.
**Ursache:** 30-s-Idle + 5-min-Hardlimit.
**Versionen:** Offscreen ab 109; Port-Keep-Alive ab 114 zuverlaessig.
**FIX (gestaffelt, funktionserhaltend):** Offiziell: `chrome.alarms` (weckt SW) + Offscreen-Document
fuer langlaufende/DOM-Arbeit; Architektur stateless. Akzeptierter Trick: langlebigen Port halten und alle
~25 s `port.postMessage`-Ping; Port vor dem 5-min-Hardlimit (~295 s) neu verbinden („Highlander"). NICHT
funktionierend: `storage`/`localStorage`-Writes per `setInterval`.
**Quelle:** Chromium Issue 40733525; Medium (MV3 SW keepalive).

### 10. SW-Update-Race: `skipWaiting` + zweiter SW im „waiting"
**Symptom:** Nach Auto-Update/Reload laeuft die alte SW-Version weiter; neuer SW haengt im „waiting";
frisch geschriebene Daten verschwinden.
**Ursache:** `self.skipWaiting()` + `registration.update()` aktiviert den neuen SW nicht zuverlaessig.
Beim Update wird der alte SW deregistriert und Daten async geloescht; schreibt der neue SW vorher, kann
das Purge ihn ueberschreiben (Race).
**Versionen:** beobachtet ab Chrome Canary 117 (Stable 115 ok); aktiver Problembereich, Status unklar.
**FIX:** `self.skipWaiting()` im `install`-Event; Migrations-/Init-Daten erst NACH `activate` schreiben;
Versions-Flag in `chrome.storage` pruefen und Daten idempotent neu aufbauen.
**Quelle:** chromium-extensions Group; Chromium Issue 40805401.

### 11. `onInstalled` vs `onStartup` Timing-Falle
**Symptom:** Init-Code laeuft beim Browser-Start nicht; Setup nur bei Install/Update.
**Ursache:** `onInstalled` feuert NUR bei Erstinstall, Extension-Update, Chrome-Update — NICHT bei jedem
Browser-Start. `onStartup` feuert beim Profilstart, loest aber keine SW-Install/Activate-Events aus.
In `onInstalled` registrierte Listener sind nicht persistent.
**Versionen:** per Design.
**FIX:** Einmal-Setup (Defaults, Alarme anlegen) in `onInstalled`; pro-Sitzung-Init in `onStartup` UND
idempotent auch beim normalen SW-Wakeup. Beides braucht synchron registrierte Listener (Punkt 5).
**Quelle:** developer.chrome.com — service-workers/lifecycle.

### 12. SW kennt kein `window`/`document`/`localStorage`/`XMLHttpRequest`/`getBackgroundPage`
**Symptom:** `localStorage is not defined`, `window/document is not defined`, `XMLHttpRequest is not
defined`, `chrome.runtime.getBackgroundPage()` undefined.
**Ursache:** Der SW hat kein DOM/`window`-Interface und keine Background-*Page* mehr.
**Versionen:** per Design ab MV3.
**FIX:** `chrome.storage.local`/IndexedDB statt `localStorage`; `fetch()` statt `XMLHttpRequest`;
DOM-Bedarf (DOMParser, Canvas, Audio) ueber **Offscreen-Document** (ab Chrome 109); andere Kontexte reden
mit dem SW nur per Message-Passing, gemeinsamer State in `chrome.storage`.
**Quelle:** developer.chrome.com — migrate to service worker.

### 13. `chrome.downloads.download()` aus dem SW schlaegt still fehl
**Symptom:** Download startet nicht, kein Fehler.
**Ursache:** Erfordert oft User-Gesten-Kontext, der im SW fehlt.
**Versionen:** beobachtet (kein offizieller „Fix"), per Design-naher Effekt.
**FIX:** Download im Content-Script per Blob-URL + Anchor-Click (echte User-Geste) ausloesen.
**Quelle:** dev.to — MV3 Migration Pitfalls (17 Extensions).

---

## C — Messaging (Content-Script ↔ Service Worker, Ports)

### 14. „The message port closed before a response was received."  ⭐ HAEUFIG
**Symptom:** `Unchecked runtime.lastError: The message port closed before a response was received.` —
`sendResponse` kommt nie an.
**Ursache:** Der `onMessage`-Listener antwortet asynchron, gibt aber nicht `return true` zurueck → Chrome
schliesst den Port, bevor die spaete Antwort kommt. Verschaerft durch interne Promisifizierung von
`sendMessage` und SW-Termination. Falle: ein `async`-Listener gibt implizit ein Promise zurueck, NICHT `true`.
**Versionen:** per Design bis Chrome 147. **Ab Chrome 148**: der Listener darf direkt ein **Promise**
zurueckgeben (graduelles Rollout; NICHT in Extensions, die DevTools per `devtools_page` erweitern).
`return true` funktioniert weiterhin und bleibt der kompatibelste Weg.
**FIX:** Bei async Antwort literal `return true;` aus dem (NICHT-async) Listener + `sendResponse`
garantiert aufrufen; ODER ab Chrome 148 ein Promise zurueckgeben. Wenn keine Antwort: nichts/`return false`.
**Quelle:** developer.chrome.com — messaging; webextension-polyfill #130 (CLOSED COMPLETED); extension.ninja.

### 15. „Could not establish connection. Receiving end does not exist."
**Symptom:** `sendMessage` schlaegt fehl, kein Empfaenger reagiert.
**Ursache:** (a) Content-Script (noch) nicht geladen — besonders im Dev nach Update in alten Tabs;
(b) Timing: Sender feuert bevor der Empfaenger-Listener registriert ist; (c) falsche API —
`runtime.sendMessage` (global) statt `tabs.sendMessage` (tab-spezifisch) oder umgekehrt.
**Versionen:** per Design.
**FIX:** Content-Script bei Bedarf per `scripting.executeScript` (re-)injizieren; Listener vor dem Senden
registrieren; richtige API waehlen (Tab → `tabs.sendMessage`, Background → `runtime.sendMessage`);
`runtime.lastError` immer behandeln.
**Quelle:** RomanistHere; Bennett Notes.

### 16. Langlebige Ports brechen ab, wenn die Seite in den BFCache geht
**Symptom:** `runtime.connect()`-Ports verlieren die Verbindung, wenn die Seite in den Back/Forward-Cache
wandert; Nachrichten kommen nicht mehr an.
**Ursache:** Chrome schliesst Channels von BFCache-Seiten mit offenen Ports.
**Versionen:** Verhaltensaenderung ab **Chrome 123**.
**FIX:** Auf `port.onDisconnect` lauschen und bei Reaktivierung der Seite neu verbinden.
**Quelle:** developer.chrome.com — what's new.

---

## D — Content Scripts, DOM-Injektion, CSP

### 17. Content Script fehlt in bereits offenen Tabs (nach Install/Update)  ⭐ HAEUFIG
**Symptom:** Nach Install/Update reagiert die Erweiterung in schon geoeffneten Tabs nicht; nur neu
geladene Tabs funktionieren.
**Ursache:** Manifest-`content_scripts` werden erst bei der naechsten Navigation injiziert.
**Versionen:** alle MV3-Versionen (per Design). Verwandter sporadischer Bug: issues.chromium.org/issues/41467164.
**FIX:** Bei `onInstalled`/`onStartup` ueber passende Tabs iterieren und per `scripting.executeScript`
nachinjizieren; Lade-Guard im CS gegen Doppelinjektion (`if (window.__myExtInjected) return;`).
**Quelle:** developer.chrome.com — content scripts.

### 18. Isolated World kann Seiten-JS-Variablen nicht lesen
**Symptom:** Zugriff auf `window.someAppState` der Seite gibt immer `undefined`.
**Ursache:** Content Scripts laufen per Design in isoliertem JS-Kontext. DOM ist geteilt, JS-Variablen/
Funktionen der Seite NICHT.
**Versionen:** per Design; `world: "MAIN"` deklarativ stabil ab Chrome 111.
**FIX:** Zweites Script mit `world: "MAIN"` injizieren (teilt den Seiten-Kontext) und per `window.postMessage`
(mit Origin-Check, Punkt 47) mit dem Isolated-CS reden. Im MAIN-World gibt es KEINE `chrome.*`-APIs →
Datenfluss MAIN → Isolated → SW.
**Quelle:** developer.chrome.com — content scripts.

### 19. „Extension context invalidated" im Content Script nach Reload/Update  ⭐ HAEUFIG
**Symptom:** `Uncaught Error: Extension context invalidated.`, sobald die Erweiterung neu geladen/
aktualisiert/deaktiviert wurde; `chrome.*`-Aufrufe im alten CS werfen.
**Ursache:** Der Browser stoppt laufende Content Scripts bei Update/Disable NICHT — der alte CS bleibt im
Tab, sein Kanal zur (jetzt toten) Extension ist ungueltig.
**Versionen:** per Design (MV2+MV3), im Dev besonders haeufig.
**FIX:** Vor `chrome.*`-Aufrufen `chrome.runtime?.id` pruefen (ist `undefined` wenn invalidiert) ODER
in try/catch kapseln und bei diesem Fehler eigene Listener/Timer/Observer sauber abmelden (nicht still
schlucken). Im Dev: betroffene Tabs nach Reload neu laden.
**Quelle:** crxjs #673 (CLOSED NOT_PLANNED — Tooling unmaintained); chromium-extensions Group.

### 20. Content Script wird bei SPA-Navigation (History-API) NICHT neu injiziert
**Symptom:** Auf YouTube/Gmail laeuft das CS beim ersten Laden, aber nach In-App-Navigation (URL aendert
sich ohne Page-Load) feuert nichts mehr; erst F5 hilft.
**Ursache:** `history.pushState`/`replaceState` aendert die URL ohne neuen Dokument-Load → keine
Neuinjektion, keine DOM-Load-Events.
**Versionen:** per Design.
**FIX:** Im SW `chrome.webNavigation.onHistoryStateUpdated` (oder `tabs.onUpdated`) abhoeren und dem CS
melden, dass es neu initialisieren soll; ggf. zusaetzlich `MutationObserver` auf den Content-Container.
**Quelle:** chromium-extensions Group; Medium (SPA support).

### 21. `executeScript({func})` — ReferenceError / Args kommen nicht an
**Symptom:** Injizierte Funktion crasht mit `ReferenceError` oder bekommt `undefined`-Argumente.
**Ursache:** Die `func` wird serialisiert → ihr Closure-Kontext geht verloren; `args` muessen
JSON-serialisierbar sein (kein DOM-Knoten, keine Funktion, kein `Map`/`Set`).
**Versionen:** per Design (`chrome.scripting` ab Chrome 88).
**FIX:** Alle Werte ueber `args: [...]` (nur JSON-faehig) uebergeben, NICHT ueber Closures; Funktion
self-contained halten.
**Quelle:** developer.chrome.com — scripting API.

### 22. `executeScript` kann ins falsche Ziel-Dokument injizieren (TOCTOU)
**Symptom:** Script landet in einer anderen Seite als beabsichtigt, wenn der Tab zwischen Check und
Injektion navigiert.
**Ursache:** Die API identifiziert via `tabId`/`frameId`, nicht via `documentId` → Time-Of-Check-Time-Of-Use-Luecke.
**Versionen:** Design-Limitierung, **OFFEN** (w3c/webextensions #8, per `gh` verifiziert 2026-06-02).
**FIX:** Wo verfuegbar `documentIds` an `executeScript`/`InjectionTarget` uebergeben; nach Navigation neu
validieren; sicherheitskritische Injektionen statisch deklarieren.
**Quelle:** github.com/w3c/webextensions/issues/8.

### 23. Host-Seiten-CSP blockt DOM-injizierte Inline-Scripts/Styles
**Symptom:** `Refused to execute inline script because it violates the following Content Security Policy
directive…` — ein per `<script>…</script>` in die Seite injiziertes Inline-Script laeuft nicht; das
Content-Script selbst aber schon.
**Ursache:** In die Seite eingefuegter Code unterliegt der CSP der HOST-Seite. Das Content-Script
(Isolated World) ist davon ausgenommen — eingefuegte Inline-Scripts nicht.
**Versionen:** per Design, host-abhaengig.
**FIX:** Logik im Content-Script belassen; MAIN-World-Code als externe Datei (`web_accessible_resources`,
Punkt 26) per `executeScript({world:"MAIN", files:[…]})` laden statt inline; Inline-`onclick` durch
`addEventListener` ersetzen.
**Quelle:** chromium-extensions Group; Microsoft Learn — CSP in extensions.

### 24. Trusted Types der Host-Seite blockt `innerHTML`-Injektion
**Symptom:** `This document requires 'TrustedHTML' assignment` beim `innerHTML`-Setzen im injizierten DOM
(z.B. auf Seiten mit `require-trusted-types-for 'script'`).
**Ursache:** Strenge Seiten erzwingen Trusted Types; rohe String-Zuweisung an `innerHTML` ist verboten.
**Versionen:** host-abhaengig; zunehmend verbreitet (Google-Properties).
**FIX:** DOM per `document.createElement`/`textContent` aufbauen statt `innerHTML`, oder eine eigene
Trusted-Types-Policy (`trustedTypes.createPolicy`). Im Shadow DOM (Punkt 25) oft entkoppelt.
**Quelle:** developer.chrome.com — content scripts (CSP/Trusted-Types).

### 25. Overlay-Styles lecken / `z-index` wirkt nicht (Stacking-Context)
**Symptom:** Das injizierte Overlay uebernimmt Seiten-CSS (sieht ueberall anders aus) ODER bleibt trotz
`z-index: 2147483647` hinter Seiten-Elementen.
**Ursache:** (a) Ohne Isolation kaskadieren Host-Styles ins Overlay. (b) `z-index` wird nur INNERHALB
desselben Stacking-Context verglichen — bildet ein Host-Element via `transform`/`opacity`/`filter`/
`will-change` einen eigenen Context, verliert das Overlay.
**Versionen:** per Design.
**FIX:** Overlay in ein **Shadow DOM** (open root) kapseln, im Root `:host { all: initial; }` → keine Leaks
in beide Richtungen. Den Shadow-Host direkt an `document.body` haengen (Wurzel-Stacking-Context).
**Quelle:** dev.to — Shadow DOM CSS Isolation.

### 26. Injizierte Ressource laedt nicht — fehlt in `web_accessible_resources`
**Symptom:** `chrome.runtime.getURL('logo.png')` liefert eine URL, aber `<img>`/`<script src>` bleibt
leer / `net::ERR_BLOCKED_BY_CLIENT`.
**Ursache:** MV3 macht Extension-Ressourcen nur fuer die Host-Seite erreichbar, wenn sie in
`web_accessible_resources` mit passenden `matches` deklariert sind.
**Versionen:** per Design (striktes Format ab Chrome 88; `use_dynamic_url` ab Chrome 96, in WAR ab 130).
**FIX:** `"web_accessible_resources": [{ "resources": [...], "matches": ["https://*/*"] }]`. Fuer
Tracking-Schutz `use_dynamic_url: true` + konsequent `getURL` zur Laufzeit (Punkt 48).
**Quelle:** developer.chrome.com — web_accessible_resources.

### 27. MAIN-World-Injektion zu spaet / Race mit Seiten-Inline-Scripts
**Symptom:** Ein MAIN-World-Script soll `fetch`/`WebSocket` patchen BEVOR die Seite sie nutzt — schafft
es aber nicht; das Seiten-Script war schneller.
**Ursache:** `document_start` ist NICHT garantiert vor den ersten Inline-Scripts der Seite; die relative
Reihenfolge zwischen statisch deklarierten und `registerContentScripts`-MAIN-Scripts aenderte sich zudem
zwischen Chromium-Versionen.
**Versionen:** bekannte Einschraenkung, **OFFEN** (w3c/webextensions #103, per `gh` verifiziert).
**FIX:** Statisch deklariertes CS mit `"run_at":"document_start"` + `"world":"MAIN"` (zuverlaessiger frueh
als dynamisch registriert); Patch defensiv schreiben; nicht auf exakte Reihenfolge mehrerer
`document_start`-Scripts verlassen.
**Quelle:** github.com/w3c/webextensions/issues/103; David Walsh.

### 74. contenteditable gibt beim Zuruecklesen NICHT 1:1 den gesetzten String zurueck  ⭐ HAEUFIG
**Symptom:** Man setzt Text in ein `contenteditable`-Feld (ChatGPT/Claude/Gemini-Composer), liest ihn
gleich darauf wieder aus und vergleicht strikt (`gelesen === gesetzt`) — der Vergleich ist FALSE, obwohl
der Nutzer nichts geaendert hat. Folge in der Praxis: eine „hat der Nutzer manuell editiert?"-Pruefung
schlaegt faelschlich an; ein Undo/Restore wird blockiert.
**Ursache:** Der Editor normalisiert beim Einfuegen: `\n` wird zu `<br>`/Block-Element, fuehrende/anhaengende
Leerzeichen werden getrimmt, Mehrfach-Whitespace kollabiert, teils geschuetzte Leerzeichen (` `) oder
Zero-Width-Zeichen (`​`/`﻿`) eingestreut. `innerText`/`textContent` liefern danach einen leicht
abweichenden String.
**Versionen:** per Design (DOM/contenteditable), Chromium-weit, versionsunabhaengig.
**FIX:** Gesetzten vs. gelesenen Text NIE strikt vergleichen. Vor dem Vergleich normalisieren
(Zero-Width entfernen, `\s+`→Space, trim) — genau das macht `setViaPaste` intern bereits beim eigenen
Erfolgs-Check. Fuer Snapshots/Undo zusaetzlich den TATSAECHLICH zurueckgelesenen Feldwert als Referenz
speichern, nicht den Roh-String, den man hineingab.
**Quelle:** eigener Vorfall 2026-06-07 (overlays, Gemini-Toggle „Original wiederherstellen" scheiterte am
strikten `===`-Vergleich) + DOM-Verhalten.

---

## E — Storage (sync / local / session)

### 28. `storage.sync`: 8 KB pro Item, ~100 KB gesamt
**Symptom:** `set()` schlaegt fehl mit „QUOTA_BYTES_PER_ITEM quota exceeded".
**Ursache:** Pro Eintrag max **8192 Bytes** (JSON-Stringify + Key); gesamt **102400 Bytes** (~100 KB).
**Versionen:** per Design (MV2+MV3).
**FIX:** Grosse/wachsende Daten als EINZELNE Eintraege (1 Datensatz = 1 Key), nicht als ein grosses Objekt;
oder nach `storage.local` auslagern und nur Metadaten syncen.
**Quelle:** developer.chrome.com — storage API. (Siehe auch Geraete-Sync, Punkt 37.)

### 29. `storage.sync`: Schreibraten-Limit
**Symptom:** Haeufige `set/remove/clear` werfen „MAX_WRITE_OPERATIONS_PER_MINUTE/HOUR exceeded".
**Ursache:** Max **120/min** (2/s) und **3600/h**.
**Versionen:** per Design.
**FIX:** Writes debouncen/zusammenfassen (~800 ms Debounce; State sammeln, dann EIN `set`).
**Quelle:** developer.chrome.com — storage API.

### 30. `storage.session`: fuer Content-Scripts standardmaessig UNSICHTBAR
**Symptom:** Content-Script liest `chrome.storage.session` → `undefined`/leer, obwohl der SW geschrieben hat.
**Ursache:** `storage.session` ist per Default nur fuer trusted contexts (Extension-Seiten + SW) sichtbar.
**Versionen:** per Design.
**FIX:** Im SW einmal `chrome.storage.session.setAccessLevel({accessLevel:'TRUSTED_AND_UNTRUSTED_CONTEXTS'})`.
ACHTUNG: keine Secrets in `session`, wenn die Seite die Isolation brechen koennte (onChanged leakt unabhaengig vom Level).
**Quelle:** developer.chrome.com — storage API.

### 31. `storage.session`/`local`: Quota-Versionen
**Symptom:** Grosse Caches werfen Quota-Fehler.
**Ursache/Versionen:** `session` In-Memory: **10 MB ab Chrome 112** (vorher 1 MB). `local`: Default
**~10 MB** (frueher 5 MB).
**FIX:** Auf alten Versionen kleiner halten; `local` mit `"permissions":["unlimitedStorage"]` erweitern
(loest Quota, aber Web-Store-Review beachten).
**Quelle:** developer.chrome.com — storage API; w3c/webextensions #350 (CLOSED COMPLETED).

### 32. Storage-Serialisierung verliert Typen
**Symptom:** `Date` kommt als String/Number zurueck, `Map`/`Set` werden `{}`, `undefined`-Felder fehlen,
`Infinity`/`NaN` → `null`.
**Ursache:** Storage serialisiert strukturell per JSON.
**Versionen:** per Design.
**FIX:** Vor dem Schreiben in serialisierbare Form wandeln (`Date.toISOString()`, `Map`→Array of Pairs)
und beim Lesen rekonstruieren.
**Quelle:** developer.chrome.com — storage API.

### 33. Storage hat keine Transaktionen → Race/Clobbering
**Symptom:** Parallele Schreibvorgaenge ueberschreiben sich; Daten „verloren".
**Ursache:** Keine Transaktionen; gleichzeitige Read-Modify-Write-Zyklen clobbern einander. Zusaetzlich
Race bei SW-Async-Init (kein Top-Level-`await`).
**Versionen:** per Design.
**FIX:** Fuer nebenlaeufige Updates IndexedDB mit Transaktionen, ODER eine Schreib-Queue/Serialisierung;
in jedem Listener auf abgeschlossene Init warten.
**Quelle:** chromium-extensions Group.

---

## F — declarativeNetRequest (DNR)

### 34. Regel-Limits (statisch / dynamisch / session / regex)
**Symptom:** Regeln werden ignoriert / `updateDynamicRules()` schlaegt fehl / Rulesets lassen sich nicht aktivieren.
**Ursache/Limits:** Statisch: bis **100 Rulesets** deklarierbar, nur **50 gleichzeitig aktiv**, mind.
**30.000 Regeln** garantiert. Dynamisch + Session (ab Chrome 120 GETRENNT, vorher 5000 kombiniert): je bis
**30.000 total**, davon nur **5.000 „unsafe"** (redirect/modifyHeaders); **5.000 Session-Regeln** (werden
bei Shutdown UND Extension-Update geloescht). Regex: max **1.000** pro Typ, jede kompiliert **< 2 KB**.
`getMatchedRules()`: max **20 Aufrufe / 10 min** (User-Gesten ausgenommen).
**Versionen:** Limit-Aufteilung ab Chrome 120; „safe rules"-Erhoehung (30.000) ab Chrome 121.
**FIX:** Rulesets bewusst en-/disablen (`updateEnabledRulesets`); „safe" (block/allow) bevorzugen; Regex nur
wo `urlFilter` nicht reicht; `getAvailableStaticRuleCount()` pruefen.
**Quelle:** developer.chrome.com — declarativeNetRequest.

### 35. `modifyHeaders`: nur Allowlist-Header + Prioritaets-Sperre
**Symptom:** Header-Aenderung wird ignoriert / nachrangige Regel kann den Header nicht mehr aendern.
**Ursache:** Nur bestimmte Header erlaubt; hat eine Regel `set`/`remove` angewandt, koennen nachrangige
Regeln den Header nicht weiter aendern. `user-agent` ist erlaubt, `origin` NICHT (relevant fuer Edge-TTS, Punkt 41).
**Versionen:** per Design.
**FIX:** `priority` bewusst setzen; Reihenfolge der Operationen einplanen; keine beliebigen Header erwarten.
**Quelle:** developer.chrome.com — declarativeNetRequest.

### 36. DNR — weitere Verhaltens-/Reichweiten-Fallen
**Symptom/Ursache (gebuendelt):**
- **Kein Response-Body-Zugriff** — DNR ist deklarativ/privacy-by-design (per Design).
- **Greift nicht bei Page-Service-Worker-/CacheStorage-Antworten** — diese umgehen den Netzwerk-Stack
  (w3c/webextensions #369, CLOSED COMPLETED).
- **Greift nicht bei Redirects wie das alte `webRequestBlocking`** (w3c/webextensions #694, **OFFEN**).
- **Regel-Reihenfolge bei gleicher Prioritaet undefiniert** — Action-Praezedenz fix: allow/allowAllRequests
  > block > upgradeScheme > redirect.
- **`isUrlFilterCaseSensitive`-Default wechselte auf `false` ab Chrome 118** (vorher `true`).
- **Response-Header-Regeln greifen erst NACH Empfang der Header** (ab Chrome 128) — der Request erreicht
  den Server, bevor er geblockt wird.
- **„DNR stoppt nach langer Browser-Laufzeit zu blocken"** (w3c/webextensions #492, CLOSED COMPLETED —
  Workaround: Regeln nach SW-Wakeup re-validieren).
- **Offizielles DNR-Redirect-Sample ist kaputt** (chrome-extensions-samples #1082, OFFEN/REOPENED) — nicht 1:1 uebernehmen.
**FIX:** Eindeutige `priority` vergeben; `isUrlFilterCaseSensitive:true` explizit setzen wenn altes
Verhalten gewuenscht; fuer echtes Pre-Request-Blocking URL-/Request-Regeln statt Response-Header-Regeln;
Body-Manipulation gehoert nicht in DNR.
**Quelle:** developer.chrome.com; w3c/webextensions #369/#492/#694; chrome-extensions-samples #1082 (alle per `gh` 2026-06-02).

---

## G — Geraete-Sync (chrome.storage.sync richtig nutzen)

### 37. Geraete-Sync: Muster und Fallstricke
**Versionen:** chrome.storage.sync API (alle Versionen).
- **8 KB pro Eintrag** (Punkt 28): grosse/wachsende Daten als EINZELNE Eintraege (pro Datensatz ein Key).
- **Gleiche Extension-ID auf allen Geraeten noetig** (sonst getrennte Sync-Speicher): fester `key` im
  Manifest ODER Chrome-Web-Store-Eintrag.
- **Muster:** `storage.local` = zuverlaessige Quelle pro Geraet, `.sync` = Cloud-Backup. Echo-frei spiegeln
  per Werte-Vergleich (nicht per Flag), `__sync_ts` fuer Last-Write-Wins, ~800 ms Debounce.
- **Verschluesselung:** sync ist NICHT Ende-zu-Ende, ausser der Nutzer setzt eine Chrome-Sync-Passphrase.
  API-Keys nur mit Nutzer-OK + Passphrase-Hinweis syncen.
- **Eine Sync-Quota loescht NIE die Erweiterung** — nur der eine `set()` scheitert.
**FIX:** siehe oben; fuer Code-Vorlage `best-practices/web/chrome-extensions.md`.
**Quelle:** developer.chrome.com — storage API + eigener Vorfall.

### 38. Manifest-`key` aendern = neue ID = lokale Daten „weg"
**Symptom:** `chrome.storage` der alten ID ist nicht mehr erreichbar (wirkt wie Datenverlust).
**Ursache:** `key` bestimmt die Extension-ID. key hinzufuegen/aendern → neue ID.
**Versionen:** alle Versionen (per Design).
**FIX:** key bewusst einmal final setzen; Nutzer warnen, dass Einstellungen einmal neu einzutragen sind
(oder per Sync vom anderen Geraet kommen).
**Quelle:** eigener Vorfall + Doku.

### 39. Options-/Side-Panel zeigt frisch gesyncte Werte nicht an
**Symptom:** Felder bleiben leer, wenn Werte per Sync NACH dem Oeffnen ankommen.
**Ursache:** Options-Seiten lesen Werte oft nur beim Oeffnen (`load()`), ohne `onChanged`-Listener.
Verschaerft im Side Panel: `storage.onChanged` feuert teils nicht, solange das Panel unsichtbar ist
(Listener an Render-/React-Lifecycle gekoppelt).
**Versionen:** unabhaengig (Implementierungs-Pattern).
**FIX:** `chrome.storage.onChanged`-Listener TOP-LEVEL (ausserhalb von React/Komponenten) registrieren, der
die Felder live aktualisiert. Sofort-Workaround: Panel neu oeffnen.
**Quelle:** chromium-extensions Group.

---

## H — Permissions & Manifest

### 40. Update mit neuer Permission deaktiviert die Erweiterung bis Re-Consent
**Symptom:** Nach Auto-Update ist die Erweiterung beim Nutzer „deaktiviert", bis er zustimmt.
**Ursache:** Eine hinzugefuegte Permission, die eine Permission-Warning erzeugt (Privilege-Increase),
disabled die Erweiterung automatisch.
**Versionen:** per Design.
**FIX:** Neue Berechtigungen als `optional_permissions`/`optional_host_permissions` deklarieren und zur
Laufzeit per `chrome.permissions.request()` (in User-Gesture) anfordern → kein Disable beim Update.
**Quelle:** developer.chrome.com — permission-warnings.

### 41. Manifest-/CSP-Format-Fallen (MV3)
**Symptom:** MV2-CSP-String wird abgelehnt; externe Skripte/`unsafe-eval` blockiert; Host-Zugriff fehlt;
Upload abgelehnt.
**Ursache:** MV3 erwartet CSP als **Objekt** `{ "extension_pages": "...", "sandbox": "..." }`;
`extension_pages` ist NICHT mit Remote-Script/`unsafe-eval` lockerbar (erlaubt: `none`, `self`,
`wasm-unsafe-eval`). Host-Match-Patterns muessen in `host_permissions` (getrennt von API-`permissions`).
`version`: 1–4 punktgetrennte Integer (0–65535), kein fuehrendes 0; `manifest_version` nur `3`;
`minimum_chrome_version` muss Substring einer echten Version sein.
**Versionen:** MV3 (Pflichtformat).
**FIX:** Versionsschema strikt; Hosts in `host_permissions`; Drittanbieter-/dynamisches HTML in
`sandbox`-Pages (Punkt 46). `wss://`-Hosts gehoeren ebenfalls in `host_permissions` (Punkt 49).
**Quelle:** developer.chrome.com — manifest / CSP.

---

## I — Action / Popup / Options / i18n / Commands / Icons

### 42. `action.onClicked` feuert nicht bei gesetztem `default_popup`
**Symptom:** `chrome.action.onClicked`-Listener wird nie aufgerufen.
**Ursache:** Ist ein Popup gesetzt, zeigt Chrome das Popup und unterdrueckt `onClicked` — schliessen sich
gegenseitig aus.
**Versionen:** per Design (alle MV3).
**FIX:** Logik ins Popup legen, ODER zur Laufzeit `chrome.action.setPopup({popup:""})` → dann feuert
`onClicked` wieder.
**Quelle:** developer.chrome.com — action API.

### 43. Popup-/Icon-/Options-Fallen (gebuendelt)
- **Popup max ~800×600** (OS-abhaengig): groessere UI in `options_ui (open_in_tab)` oder eigenem Tab.
- **Popup schliesst bei Fokusverlust** → Debuggen via Rechtsklick aufs Popup → „Untersuchen" (eigener Kontext).
- **Badge-Text max ~4 Zeichen.**
- **`setIcon`: kein SVG; unpacked nur PNG** (packed auch JPEG/BMP/ICO); 16/32/48/128 ausliefern.
- **Toolbar-Icon „unsichtbar"** weil neue Erweiterungen ungepinnt sind (hinter dem Puzzle-Menue) — kein Bug.
- **Altes Icon bleibt nach Update gecacht** → Icon-Datei umbenennen (neuer Cache-Key) + Reload.
- **`options_ui.page` gewinnt ueber `options_page`**; `open_in_tab:false` = eingebettet, `true` = eigener Tab.
**Versionen:** per Design.
**Quelle:** developer.chrome.com — action / configure-icons / options-page.

### 44. i18n / `_locales`-Fallen
**Symptom:** Extension laedt nicht („default_locale missing"); `__MSG_...__` erscheint woertlich; Sprache
nicht zur Laufzeit umschaltbar.
**Ursache:** Mit `_locales` ist **`default_locale` Pflicht**. Substitution kontextabhaengig: in
manifest.json + CSS `__MSG_name__`, im JS `chrome.i18n.getMessage("name")`. `chrome.i18n` folgt der
Browser-Sprache (keine Laufzeit-Umschaltung).
**Versionen:** per Design.
**FIX:** `default_locale` setzen; korrekte Syntax je Kontext; fuer in-App-Sprachwahl eigene Strings aus
JSON + `chrome.storage` laden statt `chrome.i18n`.
**Quelle:** developer.chrome.com — i18n / default-locale.

### 45. `commands` (Tastenkuerzel)
**Symptom:** 5. `suggested_key` ignoriert; Shortcut funktioniert nicht; Mac weicht ab.
**Ursache/Regeln:** Nur **4** `suggested_key`-Eintraege; Ctrl ODER Alt zwingend; **`Ctrl+Alt` verboten**
(AltGr-Konflikt); Mac: Ctrl→Command automatisch, fuer echtes Control `MacCtrl`; reservierter Command
`_execute_action` (MV3, NICHT mehr `_execute_browser_action`).
**Versionen:** per Design (MV3).
**FIX:** Plattform-Objekt (`default`/`mac`/`windows`) sauber setzen; bei Konflikt leeren `suggested_key`
lassen und Nutzer unter `chrome://extensions/shortcuts` manuell binden lassen.
**Quelle:** developer.chrome.com — commands API.

---

## J — Side Panel & Offscreen Documents

### 46. Offscreen-Document: nur EINS, Race beim Anlegen, schliesst nach 30 s
**Symptom:** `createDocument()` wirft „Only a single offscreen document may be created"; bei parallelen
Aufrufen Doppel-Erstellung; `AUDIO_PLAYBACK`-Doc schliesst nach 30 s ohne Ton.
**Ursache:** Max 1 Offscreen-Doc pro Extension/Profil; kein atomares „create-if-not-exists".
**Versionen:** Offscreen API ab Chrome 109; `getContexts` ab Chrome 116.
**FIX:** Vor `createDocument` per `getContexts()`/`hasDocument()` pruefen; Erstellung ueber ein globales
Promise serialisieren (Single-Flight). (Dies ist auch der Standard-Hebel fuer DOM/Audio/`getUserMedia` im
MV3, siehe Audio-Sektion.)
**Quelle:** developer.chrome.com — offscreen API; #4720 (CLOSED COMPLETED).

### 47. `sidePanel.open()` nur synchron in User-Geste; Panel schliessen kann crashen
**Symptom:** `"sidePanel.open() may only be called in response to a user gesture"` trotz echter Geste;
`setOptions({path:""})` zum „Schliessen" kann Chrome abstuerzen lassen; `storage.onChanged` feuert nicht bei unsichtbarem Panel.
**Ursache:** Die Gesten-Chain wird durch ein `await`/Messaging anderer Extensions (z.B. 1Password)
unterbrochen; leerer Path in `setOptions` ist fehlerhaft.
**Versionen:** Side Panel API ab Chrome 114, `open()` ab Chrome 116; Gesten-Bug aktiv (issues.chromium.org/issues/40929586, Login-Wall → Status nicht hart geprueft).
**FIX:** `open()` synchron im ersten Tick des Klick-Handlers VOR jedem `await`; `setPanelBehavior(
{openPanelOnActionClick:true})` in `onInstalled`; Panel nicht via leerem Path schliessen;
`storage.onChanged` top-level registrieren.
**Quelle:** developer.chrome.com — sidePanel; chromium-extensions Group.

---

## K — Audio / Media / Mikrofon / TTS / WebSocket  (Overlay-/Voice-relevant)

### 48. `getUserMedia`/`mediaDevices` ist im Service Worker NICHT verfuegbar
**Symptom:** Mikrofon-/Kamera-Zugriff aus dem SW wirft / `navigator.mediaDevices` ist undefined.
**Ursache:** Der SW hat kein DOM/`navigator.mediaDevices`.
**Versionen:** per Design.
**FIX:** `getUserMedia` in einem **Offscreen-Document** (Punkt 46) oder Content-Script/Popup laufen lassen.
**Quelle:** chromium-extensions Group.

### 49. `getUserMedia` im Offscreen-Doc scheitert mit `NotAllowedError`
**Symptom:** Mikrofon im Offscreen-Doc bekommt keine Permission, obwohl die Extension sie hat.
**Ursache:** Das Offscreen-Doc hat keinen sichtbaren Permission-Prompt-Pfad.
**Versionen:** per Design.
**FIX:** Permission EINMAL ueber einen sichtbaren Tab/Iframe der Extension-Origin holen; danach
persistiert sie fuer die Origin und der Offscreen-Zugriff klappt.
**Quelle:** chrome-extensions-samples #821.

### 50. `tabCapture` vs `desktopCapture` im Offscreen-Doc
**Symptom:** `tabCapture` funktioniert, `desktopCapture` wirft „Invalid state" im Offscreen-Doc.
**Ursache:** `tabCapture`-StreamID ist im Offscreen-Doc nutzbar (ab Chrome 116) und braucht zwingend eine
User-Geste; eine `desktopCapture.chooseDesktopMedia`-StreamID ist im Offscreen-Doc NICHT nutzbar.
**Versionen:** tabCapture-im-Offscreen ab Chrome 116.
**FIX:** Fuer Bildschirm-Capture im Offscreen-Doc `getDisplayMedia` nutzen statt der desktopCapture-StreamID.
**Quelle:** developer.chrome.com — screen-capture / tabCapture.

### 51. Live Caption (SODA) transkribiert ungewollt; Autoplay blockt Audio
**Symptom:** Beim Audio-Abspielen erscheinen ungewollte Live-Untertitel; Audio startet ohne User-Geste nicht.
**Ursache:** Chromes Live Caption (SODA) transkribiert jedes Audio ueber ein HTMLMediaElement
(`new Audio()`/`<audio>`); Autoplay-Policy blockt Wiedergabe ohne Geste.
**Versionen:** Chrome 89+ mit aktiviertem Live Caption.
**FIX:** Wiedergabe ueber **Web Audio API** (AudioContext + decodeAudioData + AudioBufferSourceNode) —
dieser Pfad hat keinen Live-Caption-Hook; `AudioContext.resume()` nach einer Geste aufrufen. (Nutzer-
Sofort-Workaround: `chrome://settings/accessibility` → Live-Untertitel aus.)
**Quelle:** eigener Bezug (overlays) + chromium-extensions Group; (SODA-Umgehung im Feld bestaetigt).

### 52. `chrome.tts`/SpeechSynthesis-Fallen
**Symptom:** Kein Audio aus dem SW; eigene Stimmen nicht auffindbar; Queue haengt ohne `end`-Event;
Initial-`speak()` schlaegt fehl.
**Ursache:** TTS/Web-Speech braucht einen Dokument-Kontext (kein SW); SpeechSynthesis-Regression um Chrome 130.
**Versionen:** per Design (SW) + Regression ~Chrome 130 (issues.chromium.org/issues/374263394).
**FIX:** TTS in Offscreen-Doc/Content-Script; auf `end`/`error`-Events der Utterance reagieren; bei
Initial-Fail einmal „aufwaermen" (kurze Dummy-Utterance) bzw. Voices-Liste vor `speak()` abwarten.
**Quelle:** developer.chrome.com — ttsEngine; chromium #374263394.

### 53. WebSocket in Erweiterungen: `wss://` in host_permissions + 403 bei UA-Diensten
**Symptom:** WS schlaegt sofort fehl (`ws.onerror` beim Handshake) ODER HTTP 403 beim Handshake.
**Ursache:** Ziel-Host fehlt als `wss://` in `host_permissions` (nur `https://` reicht NICHT, MV3). Bei
Diensten wie `speech.platform.bing.com` (Edge-TTS) wird ein User-Agent mit `Edg/` verlangt; Chrome sendet
Chrome-UA, die WS-API kann den UA nicht ueberschreiben.
**Versionen:** Manifest V3 (host_permissions noetig; unter MV2 nicht). 403 dienstseitig, versionsunabhaengig.
WS haelt den SW ab Chrome 116 am Leben (Punkt 8).
**FIX:** Host als `wss://host/*` in `host_permissions`. Fuer UA-abhaengige Dienste: statische
`declarativeNetRequest`-Regel, die den `user-agent` fuer den Host auf einen Edge-UA setzt (+ Permission
`declarativeNetRequestWithHostAccess`). `user-agent` ist in der modifyHeaders-Allowlist, `origin` nicht (Punkt 35).
**Quelle:** eigene Vorfaelle (overlays/Edge-TTS) + developer.chrome.com — websockets.

### 54. Mikrofon/Spracheingabe stumm nach USB-Port- oder Hub-Wechsel  ⭐ HAEUFIG
**Symptom:** Mic-Button startet sichtbar (wird rot), aber KEIN Ton kommt an: Live-Vorschau leer,
Whisper/Groq bekommt nur Stille, KEIN Fehler-Toast. `getUserMedia` wirft KEINEN Fehler. Tritt auf ALLEN
Seiten auf. Dasselbe Mikrofon funktioniert gleichzeitig in nativen Apps einwandfrei.
**Ursache (zweistufig):** (1) **Windows** vergibt einem USB-Mikrofon ohne Seriennummer seine Instanz-ID aus
Bus + Port — neuer Port oder neu dazwischengesteckter USB-Hub = neue Geraete-Instanz = Standard-Zuordnung
verloren. (2) **Chrome** merkt sich die Mikrofon-Wahl als gehashte `deviceId`; wird die ID ungueltig, faellt
Chrome auf ein anderes/leeres Geraet zurueck und `getUserMedia` liefert einen gueltigen aber stummen Track.
**Versionen:** Chromium-weit (Chrome+Edge), Windows; Chromium #40275281, #997689, Google-Help #8079458 —
Stand 2026 nicht behoben.
**FIX (Nutzer):** `chrome://settings/content/microphone` → richtiges (USB-)Mikrofon explizit als Standard
waehlen (reiner Chrome-Neustart hilft NICHT). Moeglichst am selben Port lassen. **FIX (Code, Haertung):**
Stummen Stream erkennen (Web Audio `AnalyserNode`, RMS < ~0,01 ueber 1–2 s, zusaetzlich `track.muted`/
`track.readyState`) und klare Meldung zeigen statt still zurueckzukehren; optional `enumerateDevices()` +
`devicechange`-Listener.
**Quelle:** eigener Vorfall 2026-06-01 (overlays) + Recherche.

---

## L — Sicherheit (typische Entwicklerfehler)

### 55. `externally_connectable`: Origin-Vertrauen statt Sender-Authentifizierung (ClaudeBleed)
**Symptom/Risiko:** Beliebige Webseite (oder jede andere Extension) kann per `runtime.sendMessage(extId,…)`
privilegierte Handler triggern → Privilege Escalation.
**Ursache:** Handler vertraut dem Origin-Kontext („Nachricht kommt von X") statt dem echten Execution-
Kontext. Die Extension-ID ist oeffentlich, also frei adressierbar.
**Versionen:** per Design — Sender-Auth ist Entwicklerpflicht.
**FIX:** `externally_connectable.matches` so eng wie moeglich, NIE `<all_urls>`; fuer Ext-zu-Ext konkrete
`ids` statt `["*"]`; Page-Auth per signiertem Einmal-Token, nicht allein per Origin; Approvals an konkrete
Aktion + Token binden.
**Quelle:** LayerX (ClaudeBleed); developer.chrome.com — externally_connectable.

### 56. Fehlende `sender`-Pruefung in `onMessage`/`onMessageExternal`/`onConnect`
**Symptom/Risiko:** Kompromittiertes Content-Script oder Fremd-Extension loest privilegierte Aktionen aus.
**Ursache:** Handler verarbeitet Requests ohne `sender`-Validierung.
**Versionen:** per Design.
**FIX:** In `onMessageExternal` `sender.id` gegen Whitelist pruefen; bei Web-Sendern `sender.origin`/`.url`
validieren; Input sanitisieren; Allow-Liste fuer erlaubte Aktionen; Scope privilegierter Aktionen minimieren.
**Quelle:** developer.chrome.com — stay secure; Cisco Duo; USENIX Sec '23.

### 57. `postMessage` ohne Origin-Check (MAIN ↔ Isolated World)
**Symptom/Risiko:** `message`-Listener ohne `event.origin`/`event.source`-Filter → jede Seite/jedes iframe
kann gefaelschte Nachrichten ins Content-Script schieben.
**Ursache:** `postMessage(msg,"*")` als Ziel; fehlender Empfangs-Filter.
**Versionen:** per Design (besonders relevant seit `world:"MAIN"`).
**FIX:** Empfangsseitig `if (event.source!==window || event.origin!==location.origin) return;` + erwarteten
Nachrichten-Tag pruefen. Sendeseitig konkretes `targetOrigin`.
**Quelle:** developer.chrome.com — messaging; MDN.

### 58. DOM-XSS im Content-Script + Secrets im Bundle + `web_accessible_resources`-Fingerprinting
**Symptom/Risiko (gebuendelt):**
- `innerHTML`/`document.write` mit Seiten-Daten → DOM-XSS im privilegierten Kontext (MV3-CSP verhindert das NICHT).
- API-Keys im Bundle sind fuer jeden auslesbar (die Extension ist NICHT vertraulich).
- `web_accessible_resources` ohne `matches` → beliebige Seiten koennen die Extension fingerprinten.
**Versionen:** per Design; `use_dynamic_url` in WAR ab Chrome 130.
**FIX:** DOM per `createElement`+`textContent` statt `innerHTML` (ggf. Trusted Types / DOMPurify); keine
Secrets ins Bundle (OAuth/`chrome.identity`, kurzlebige Server-Tokens); WAR mit engen `matches` +
`use_dynamic_url:true`; Drittanbieter-HTML in `sandbox`-Page rendern (eigene CSP, kein `chrome.*`-Zugriff).
**Quelle:** developer.chrome.com — stay secure / sandbox / web_accessible_resources; TechRadar.

---

## M — Native Messaging & Plattform-Fallen (Windows / macOS / Linux / Enterprise / Edge)

### 59. Native-Messaging-Host wird nicht gefunden (Registrierung/Pfad/JSON)
**Symptom:** `Specified native messaging host not found` / Host startet nicht.
**Ursache & FIX (gebuendelt):**
- **Windows-Registry:** Key unter `HKCU\SOFTWARE\Google\Chrome\NativeMessagingHosts\<host_name>` (oder HKLM),
  `(Default)` = voller Manifest-Pfad; `<host_name>` muss EXAKT dem `name`-Feld im Manifest entsprechen.
  Chrome prueft 32-bit- VOR 64-bit-View → ggf. in beide schreiben.
- **Backslashes in JSON verdoppeln** (`C:\\Program Files\\…`); keine trailing comma (Chrome lehnt das ganze
  Manifest ab); Pflichtfelder `name`, `path`, `type:"stdio"`, `allowed_origins`.
- **macOS/Linux:** Manifest ins richtige Verzeichnis (`~/Library/Application Support/Google/Chrome/
  NativeMessagingHosts/` bzw. `~/.config/google-chrome/NativeMessagingHosts/`); `path` MUSS absolut sein;
  Host-Binary braucht `chmod +x`.
**Versionen:** alle.
**Quelle:** developer.chrome.com — native-messaging; textslashplain.

### 60. Chrome 113+ ruft `.exe`-Hosts direkt auf (umgeht `cmd.exe`)
**Symptom:** Ein NM-Host, der ueber `.bat` Node/Python startet, bricht nach Chrome-Update; bei `.exe`-Hosts
ploetzlich anderes Working-Directory.
**Ursache:** Ab Chrome 113.0.5656 werden `.exe`-Hosts DIREKT (nicht via `cmd.exe`) aufgerufen; relative
Pfade/Working-Dir brechen. Skript-Hosts laufen ueber den alten Codepfad → Mischbetrieb ist die Falle.
**Versionen:** Windows, Chrome 113+.
**FIX:** Node/Python nicht direkt eintragen, sondern eine `.bat`-Wrapper-Datei als `path`, die den
Interpreter mit ABSOLUTEM Skript-Pfad startet; keine relativen Pfade annehmen.
**Quelle:** chromium-extensions Group (Chrome 113.0.5656+ Hinweis).

### 61. macOS Gatekeeper/Quarantine blockt NM-Host (extrahierte `.node`-Module)
**Symptom:** Wiederholte „… cannot be opened because the developer cannot be verified"; Host stirbt.
**Ursache:** Heruntergeladene/extrahierte Binaries tragen `com.apple.quarantine`; zur Laufzeit extrahierte
`.node`-Module sind oft unsigniert → Gatekeeper blockt.
**Versionen:** macOS; claude-code #14914 (CLOSED NOT_PLANNED, per `gh` verifiziert).
**FIX:** `xattr -rd com.apple.quarantine /pfad/zum/host-verzeichnis`; sauber: Binary + native Module mit
Developer-ID signieren bzw. ohne Runtime-Extraktion bundlen.
**Quelle:** github.com/anthropics/claude-code/issues/14914.

### 62. MV3-Service-Worker killt offenen Native-Messaging-Port nach Idle
**Symptom:** NM funktioniert anfangs, bricht nach ~30 s–5 min Inaktivitaet ab; Port disconnected ohne Useraktion.
**Ursache:** Ein offener `connectNative()`-Port haelt den SW NICHT zuverlaessig am Leben.
**Versionen:** alle MV3; claude-code #16350 (CLOSED NOT_PLANNED); developer.chrome.com #559 (CLOSED COMPLETED).
**FIX:** Keepalive (Native Messaging zaehlt ab Chrome 105 als Idle-Reset, Punkt 8) — periodische Pings;
bei Disconnect automatisch reconnecten statt aufzugeben.
**Quelle:** github.com/anthropics/claude-code/issues/16350; developer.chrome.com #559.

### 63. Enterprise & Edge-Cross-Browser
**Symptom/Ursache & FIX (gebuendelt):**
- **`ExtensionInstallForcelist`: `update_url` nach Erstinstallation ignoriert** — Folge-Updates nutzen die
  `update_url` aus dem CRX-Manifest. FIX: korrekte `update_url` ins CRX, oder `ExtensionSettings` mit
  `override_update_url:true`.
- **Off-Store-Force-Install scheitert ohne Domain-Join** (Windows) — Maschine muss AD/Azure-AD-gejoint
  oder in Chrome Enterprise Core enrolled sein.
- **Edge nutzt eigenen NM-Registry-Pfad** (`…\Microsoft\Edge\NativeMessagingHosts\`) und braucht BEIDE
  Extension-IDs (Chrome-Store + Edge-Add-ons) in `allowed_origins`.
**Versionen:** managed Windows/macOS; Edge.
**Quelle:** Chrome Enterprise Docs; claude-code #24367 (CLOSED DUPLICATE).

---

## N — MV2 → MV3 Migration & Deprecations

### 64. MV2 ist seit Chrome 139 (Juli 2025) komplett entfernt
**Symptom:** MV2-Erweiterung laeuft nicht mehr; Enterprise-Policy `ExtensionManifestV2Availability` wirkt nicht mehr.
**Ursache:** Schrittweiser Auslauf; ab Chrome 139 MV2-Support + Policy entfernt, kein Re-Enable mehr.
**Versionen:** MV2 deaktiviert ab Stable Okt 2024; default-aus 31.03.2025; **Chrome 138 (24.07.2025)** MV2 auf
ALLEN Kanaelen fuer alle Nutzer deaktiviert (kein Re-Enable); **Chrome 139 (~05.08.2025)** Enterprise-Policy
`ExtensionManifestV2Availability` **entfernt** (wirkt sofort, nicht schrittweise). **Endgueltig: 31. August 2026 =
letzte MV2-Erweiterungen werden aus dem Chrome Web Store ENTFERNT** (offizielle Timeline, Stand 08.07.2026). Auf
Chrome ≤138 installierte MV2 bleiben, bekommen aber keine Updates und sind nach der Store-Entfernung nicht neu installierbar.
**FIX:** Es gibt kein Zurueck — alles auf MV3. Jeder MV2-spezifische Workaround ist obsolet.
**Quelle:** developer.chrome.com — mv2-deprecation-timeline (re-verifiziert 2026-07-02).

### 65. Entfernte/migrierte APIs
**Symptom:** `chrome.extension.*`, `chrome.tabs.executeScript/insertCSS`, `browser_action`/`page_action`,
`webRequestBlocking`, persistente Background-Pages, Remote-Code/`unsafe-eval` funktionieren nicht.
**Ursache:** In MV3 ersetzt/verboten.
**Versionen:** per Design ab MV3 (Chrome 88+).
**FIX:** `chrome.extension.*`→`chrome.runtime.*`; `tabs.executeScript`→`chrome.scripting` (Daten ueber
`args`, Permissions `scripting` + Host/`activeTab`); `browserAction`/`pageAction`→`action`;
`webRequestBlocking`→`declarativeNetRequest` (beobachtendes `webRequest` bleibt); Background→
`"background":{"service_worker":"sw.js","type":"module"}`; allen Code lokal buendeln (kein Remote-Code);
beliebiger Laufzeit-Code nur via `userScripts`-API (stabil ab Chrome 120).
**Quelle:** developer.chrome.com — migrate (api-calls / blocking-web-requests / known-issues).

---

## O — Chrome Web Store: Review & Publishing

### 66. Die Violation-Codes (Lookup bei Ablehnungs-Mail)
Jede Ablehnung kommt mit einem Codenamen. Die wichtigsten:

| Code | Bereich | Ursache → FIX |
|------|---------|---------------|
| **Purple Potassium** | Permissions | Ungenutzte/zu breite Permission → nur engste noetige anfragen |
| **Red Argon/Magnesium** | Single Purpose | Mehrere Zwecke / Ad-Injection → auf EINEN engen Zweck reduzieren |
| **Blue Argon** | Remote Code (MV3) | externe Skripte/`eval` → lokal buendeln |
| **Red Titanium** | Code Quality | Obfuscation → entfernen (Minification ist erlaubt) |
| **Yellow Argon** | Keyword Spam | Keyword-Stuffing → ein Keyword <5×, max 5 Marken |
| **Purple Lithium/Nickel/Copper** | Privacy | Datenschutz-Link fehlt / keine prominente Offenlegung / HTTP → Privacy-Tab + Consent + HTTPS |
| **Yellow Potassium** | Min. Funktion | nur Link statt Feature → echte Funktion liefern |
| **Red Zinc** | Deceptive Install | irrefuehrende Buttons → transparente Flows |

**FIX-Grundsatz:** Single Purpose + minimale, begruendete Permissions + vollstaendiger
Privacy-Practices-Tab sind die drei Hauptrisiken.
**Quelle:** developer.chrome.com — webstore/troubleshooting.

### 67. Review-Prozess- & Account-Fallen
**Symptom/Ursache & FIX (gebuendelt):**
- **2-Step-Verification Pflicht** am Google-Konto vor Publish/Update; einmalige **5$-Registrierungsgebuehr**.
- **Privacy-Practices-Tab Pflicht** — sonst automatische Ablehnung (neu) bzw. Entfernung (bestehend).
- **Permission-Justification** je Permission + `<all_urls>` → langes/manuelles Review; `activeTab`+`scripting`
  statt breiter Hosts umgeht das.
- **Abgelehntes Update nimmt die Live-Version NICHT offline** — in Ruhe nachbessern.
- **Nur 1 Appeal pro Verstoss (2025-Update)** — sorgfaeltig begruenden.
- **„Pending review" haengt** — erst nach >3 Wochen Support kontaktieren.
- **Version muss hoeher sein** als die letzte, sonst Upload-Fehler.
- **Self-Hosting (CRX + `update_url`)** erlaubt Consumer-Installs nur auf **Linux**; Windows/macOS brauchen
  Enterprise-Policy. Unlisted/Private gehen trotzdem durchs Review.
- **Review-Stau seit April 2026 (Chrome-Team-PSA, 23.04.2026):** „surge in new extensions" → laengere Wartezeiten
  fuer **neue Submissions UND Updates** (ueber die ueblichen „wenige Tage" hinaus). Beschleunigung: frueh + staged
  submitten, **pending Items NICHT resubmitten** (setzt die Queue-Position zurueck), Permissions minimieren
  (`activeTab`/optional host permissions statt breiter Hosts), Dead Code/ungenutzte Dateien entfernen,
  Remote-Hosted-Code entfernen, Kontaktinfo im Dashboard + funktionierende Privacy-Policy-URL verifizieren.
**Quelle:** developer.chrome.com — review-process / cws-dashboard-privacy / register / cws-policy-updates-2025 / host-on-linux; Chrome-Team-PSA 2026-04-23.

---

## P — Debugging, Reload, Memory, Performance

### 68. SW-Debugging: Logs verschwinden + DevTools-Heisenbug
**Symptom:** `console.log` aus dem SW ist weg; „service worker (inactive)"; ein Idle-bezogener Bug
verschwindet, sobald die SW-DevTools offen sind.
**Ursache:** SW terminiert im Idle (Logs weg); eine offene DevTools-Verbindung haelt den SW kuenstlich am
Leben und maskiert genau die Idle-/Timing-Bugs.
**Versionen:** per Design.
**FIX:** „Preserve log" aktivieren; den „service worker (inactive)"-Link anklicken startet die Console neu;
Bugs OHNE offene DevTools reproduzieren; SW gezielt via `chrome://serviceworker-internals` oder
„stop" terminieren; wichtiges in `chrome.storage` loggen.
**Quelle:** chromium-extensions Group; developer.chrome.com — debug.

### 69. Content-Script-Logs landen in der Page-Console
**Symptom:** `console.log` aus dem CS ist nicht in den Extension-DevTools.
**Ursache:** Content-Scripts laufen im Kontext der Seite (isolated world).
**Versionen:** per Design.
**FIX:** Page-DevTools (F12 auf der Seite) oeffnen; im Console-Context-Dropdown die eigene Extension
waehlen; `debugger;` + Sources-Panel fuer Breakpoints.
**Quelle:** chromium-extensions Group; riptutorial.

### 70. Doppelte Content-Script-Injektion nach Reload
**Symptom:** Nach Extension-Reload mit programmatischer Injektion laufen ZWEI Kopien (alte verwaiste + neue)
→ doppelte Listener/UI.
**Ursache:** Manuelle Re-Injektion ohne Idempotenz-Guard; die alte Instanz lebt bis Page-Reload weiter.
**Versionen:** per Design.
**FIX:** Injektions-Guard (`if (window.__myExtInjected) return; window.__myExtInjected = true;`); ODER Tab
nach Reload neu laden (Content-Script `location.reload`, ueberlebt Context-Invalidierung).
**Quelle:** chromium-extensions Group.

### 71. Update wird nur im Idle-State installiert
**Symptom:** `onUpdateAvailable` feuert, aber das Update wird nie angewandt — App laeuft auf alter Version
bis Browser-Neustart.
**Ursache:** Updates werden NUR installiert, wenn die Extension idle ist (SW laeuft nicht UND kein offenes
Popup/Sidepanel/Options). Bei haeufig getriggertem SW wird das Update verschoben.
**Versionen:** per Design (MV3 Update-Lifecycle).
**FIX:** `chrome.runtime.onUpdateAvailable` abonnieren und bei passender Gelegenheit `chrome.runtime.reload()`.
`requestUpdateCheck()` NICHT routinemaessig nutzen.
**Quelle:** developer.chrome.com — extensions-update-lifecycle.

### 72. Unpacked: Reload-Disziplin + CRXJS/Vite-HMR-Bugs
**Symptom:** Code-Aenderung wirkt nicht; Content-Script bleibt alt; HMR triggert, aber Komponenten updaten
nicht; „Service worker has not loaded fully".
**Ursache:** Chrome laedt Quellcode nicht automatisch neu (Content-Scripts brauchen Page-Refresh,
manifest-Aenderungen vollen Reload). CRXJS/Vite-Plugin ist faktisch kaum gewartet (mehrere Bugs).
**Versionen:** crxjs #449 (CLOSED COMPLETED), #515/#673/#723 (alle CLOSED NOT_PLANNED — won't fix).
**FIX:** Reload-Button → betroffene Tabs refreshen; `@vitejs/plugin-react` statt `-swc`; bei kritischem
Workflow eigenes WebSocket-Reload/webpack-Reloader statt CRXJS erwaegen.
**Quelle:** github.com/crxjs/chrome-extension-tools (per `gh` 2026-06-02).

### 73. Memory-Leaks: Observer/Listener/Intervalle bei SPA-Navigation
**Symptom:** Speicher waechst auf SPA-Seiten (YouTube/Gmail); detached DOM nodes; mehrfach feuernde Observer;
nach langer Session traege.
**Ursache:** Content-Script lebt ueber Routenwechsel hinweg; Observer/Listener auf globalen Dauer-Elementen
halten Scopes + detached nodes am Leben.
**Versionen:** per Design (JS-GC).
**FIX:** `observer.disconnect()`/`removeEventListener()`/`clearInterval()` bei Routenwechsel/Cleanup;
Observer nicht an globale Dauer-Elemente binden wenn vermeidbar; bei SPA-Navi neu aufsetzen.
**Quelle:** Common causes of memory leaks in extensions; makandra.

---

### 74. STT-Diktat: Live-Vorschau springt im contenteditable / ueberschreibt die finale Transkription   [⭐ EIGENER VORFALL 2026-06-24]
**Symptom:** Beim Mikrofon-Diktat im Overlay (Web Speech Live-Vorschau + Groq Whisper finale Fassung)
springt/flackert der Text im Seiten-Eingabefeld (ChatGPT/Claude/Grok), Woerter zappeln hin und her; oft
wird am Ende die ROHE Vorschau (ohne Satzzeichen) abgeschickt statt der finalen Whisper-Fassung.
**Ursache:** (1) Die rohen interim results wurden per Paste-Simulation ins fremde `contenteditable`
geschrieben — jeder Paste selektiert+ersetzt → Cursor/Scroll springen, es flackert. (2) Web Speech feuert
nach `abort()` noch ein spaetes `onresult` → ohne Riegel ueberschreibt die Vorschau die finale Fassung bzw.
ein Auto-Send greift sie ab.
**Versionen:** alle Chrome/Edge MV3 (per Design der Web Speech API + contenteditable).
**FIX:** Live-Vorschau in ein SEPARATES schwebendes Overlay-Element schreiben (z.B. `#stt-live-preview`),
NICHT ins Seiten-Feld — kein Flackern/Springen mehr. Zeitlicher Riegel `previewActive` (true bei
Vorschau-Start, false SOFORT beim Stopp + beim Entfernen): der `onresult`-Handler beginnt mit
`if (!previewActive) return;`. So gelangt AUSSCHLIESSLICH die finale Whisper-Fassung (mit Satzzeichen) ins
Feld und wird gesendet — die rohe Vorschau nie. Fallback bei Groq-Ausfall: Vorschau behalten, aber mit
sichtbarem Hinweis (funktionserhaltend). Generisches, technologie-neutrales Muster:
`bugs/desktop/voice-pipeline.md` §7 + `best-practices/desktop/voice-pipeline.md` §9.
**Quelle:** eigener Vorfall 2026-06-24 (overlays 0.6.4). Verifiziert — von Frank bestaetigt.

## Fix-Status: was ist (belegt) gefixt vs. noch offen

> Ehrlichkeitsregel: „gefixt" nur mit Beleg (offizielles Changelog / `gh`-Status). GitHub-Tracker wurden am
> 2026-06-02 per `gh` HART geprueft. crbug/issues.chromium.org lagen hinter einer Login-Wall → diese
> Eintraege sind aus Changelog/Snippets abgeleitet und entsprechend markiert.

**Belegt gefixt / als Verhalten geaendert (≤ Chrome 148):**

| Frueheres Verhalten | gefixt/geaendert ab | Bezug |
|---------------------|---------------------|-------|
| `storage.session` In-Memory-Limit 1 MB | **Chrome 112** (jetzt 10 MB) | Punkt 31 |
| WebSocket hielt SW nicht am Leben | **Chrome 116** | Punkt 8, 53 |
| `alarms`-Minimum 60 s | **Chrome 120** (jetzt 30 s) | Punkt 7 |
| DNR dynamic+session kombiniert (5000) | **Chrome 120** (getrennt), safe-rules 30.000 ab 121 | Punkt 34 |
| `isUrlFilterCaseSensitive` Default `true` | **Chrome 118** (jetzt `false`) | Punkt 36 |
| Disabled Extension belegte globales DNR-Static-Limit | **Chrome 128** (behoben) | Punkt 36 |
| Langlebige Ports blieben bei BFCache offen | **Chrome 123** (werden geschlossen) | Punkt 16 |
| `use_dynamic_url` nicht in `web_accessible_resources` | **Chrome 130** | Punkt 26, 58 |
| `onMessage` konnte kein Promise zurueckgeben | **Chrome 148** (graduell; `return true` bleibt) | Punkt 14 |
| MV2 noch lauffaehig | **entfernt ab Chrome 139** | Punkt 64 |
| CVE-2026-7952 (DNR Permission-Policy-Bypass) | **gefixt ab Chrome 148.0.7778.96** | — |
| w3c #350 (storage.session-Limits) / #369 (DNR cross-ext) / #492 (DNR stoppt) | CLOSED COMPLETED | Punkt 31/36 |
| webextension-polyfill #130 (message port closed) | CLOSED COMPLETED | Punkt 14 |

**Noch NICHT gefixt (Workaround bleibt aktiv):**

- **w3c/webextensions #8** (executeScript-TOCTOU / fehlende `documentId`-Garantie) — OFFEN (Punkt 22).
- **w3c/webextensions #103** (registerContentScripts-Timing/Reihenfolge) — OFFEN (Punkt 27).
- **w3c/webextensions #694** (DNR greift nicht bei Redirect wie `webRequestBlocking`) — OFFEN (Punkt 36).
- **chrome-extensions-samples #1082** (DNR-Redirect-Sample kaputt) — OFFEN/REOPENED (Punkt 36).
- **crxjs #515/#673/#723** (HMR/„context invalidated"/swc) — CLOSED **NOT_PLANNED** (won't fix; Tooling unmaintained) (Punkt 19, 72).
- **claude-code #14914 / #16350** (Gatekeeper-`.node` / NM-Port stirbt bei SW-Idle) — CLOSED NOT_PLANNED (Punkt 61, 62).
- **Erweiterung verschwindet (Secure-Preferences am Ordner-Pfad)** — Chromium 40227980, Stand 2026 nicht behoben (Punkt 1).
- **Mikrofon stumm nach USB-Wechsel** — Chromium #40275281/#997689, Stand 2026 nicht behoben (Punkt 54).
- **SW-Update-Race / skipWaiting** — aktiver Problembereich, Status unklar (Login-Wall) (Punkt 10).
- **sidePanel.open()-Gesten-Bug** — issues.chromium.org/issues/40929586, Status nicht hart geprueft (Login-Wall) (Punkt 47).
- **SpeechSynthesis-Regression ~Chrome 130** — chromium #374263394, Status nicht hart geprueft (Punkt 52).

---

## Pflicht-Checkliste vor Chrome-Extension-Arbeit

- [ ] Diese Datei komplett gelesen + Stand-Datum gegen die aktuelle Browser-Version (Chrome 148) abgeglichen?
- [ ] Service Worker: State in `chrome.storage`, Listener synchron top-level, `chrome.alarms` statt Timer (B)?
- [ ] Messaging: `return true` (oder Promise ab 148) bei async `sendResponse` (Punkt 14)?
- [ ] Content Script: in offene Tabs nachinjizieren + Guard; Isolated vs MAIN World bedacht (Punkt 17/18)?
- [ ] „Verschwindet/instabil"? ZUERST Ordner-Pfad wechseln, nicht ID/Code (Punkt 1).
- [ ] Keine `_`-Dateien im Ordner (Punkt 2)?
- [ ] WebSocket-Hosts als `wss://` in `host_permissions` (Punkt 53)?
- [ ] Storage: pro-Datensatz-Keys, fester `key`, Verschluesselung/Quota/Raten bedacht (E, G)?
- [ ] DNR: Regel-Limits + `modifyHeaders`-Allowlist + Prioritaeten beachtet (F)?
- [ ] Audio/Mikrofon: ueber Offscreen/Content-Script, nicht im SW; Web Audio gegen SODA (K)?
- [ ] Mikrofon stumm (aber Button rot)? ZUERST Chrome-Mic-Auswahl pruefen, nicht den Code (Punkt 54).
- [ ] Sicherheit: `sender` validieren, keine Secrets im Bundle, enge `web_accessible_resources` (L)?
- [ ] Vor Store-Publish: Single Purpose, Permission-Justification, Privacy-Tab (O)?
- [ ] Permission spaeter hinzufuegen? Als `optional_permissions` (sonst Disable beim Update, Punkt 40).

---

## 🔗 Bezug zu Best-Practices (beide Seiten der Medaille)

| Bug-Abschnitt (hier) | Best-Practice-Abschnitt (`best-practices/web/chrome-extensions.md`) |
|----------------------|---------------------------------------------------------------------------------------------|
| B — Service-Worker-Lebenszyklus | „Service Worker als zustandsloser Mittler" |
| C — Messaging | „Robustes Message-Passing" |
| D — Content Scripts / CSP | „Content-Script-Injektion & Isolation" |
| E/G — Storage & Geraete-Sync | „Storage-Strategie (local-Quelle, sync-Backup)" |
| F — declarativeNetRequest | „DNR statt blockierendem webRequest" |
| K — Audio/Mikrofon | „Audio/Capture ueber Offscreen-Document" |
| L — Sicherheit | „Sender-Validierung & Secrets-Hygiene" |
| O — Web Store | „Store-Readiness (Single Purpose, Permissions, Privacy)" |

# Chrome-Erweiterungen (Manifest V3) — Best Practices (Stand 2026-06-02, Chrome 148.0.7778.217)

> Die **zweite Seite der Medaille** zum Bug-Almanach [`bugs/web/chrome-extensions.md`](../../bugs/web/chrome-extensions.md):
> dort steht *was schiefgeht und wie man es umgeht* (Sektionen A–P, 73 Eintraege), hier *wie man es von
> vornherein richtig macht*. Versions-Anker = installierte Browser-Version **Chrome 148** (MV2 seit Juli 2025
> vollstaendig entfernt). Quellen-Rangordnung: **offiziell** (developer.chrome.com, Chrome for Developers Blog,
> chromestatus.com, issues.chromium.org, Chrome Web Store Policies) = Grundwahrheit; **`extern`** (Community) =
> abwaegbare Ergaenzung, ueberstimmt nie Offizielles. Recherchiert von 10 parallelen Researchern am 2026-06-02.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektuere
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Service Worker bauen | Listener synchron top-level; kein globaler State; `chrome.alarms` | §1 |
| 2 | Async-Message beantworten | Promise zurueckgeben (148+) ODER `return true`; nie `async`-Listener | §1 |
| 3 | Content-Script injizieren | `document_idle` Default; in offene Tabs nachinjizieren + Guard | §2 |
| 4 | Seiten-JS abgreifen | `world:"MAIN"`; Datenfluss MAIN → isoliert → SW | §2 |
| 5 | Overlay gegen Seiten-CSS | Shadow DOM an `document.body`, `:host { all: initial; }` | §2 |
| 6 | Permissions waehlen | `activeTab` statt breiter Hosts; neue Permission optional | §3 |
| 7 | Kein Remote-Code | Alles lokal buendeln; nur JSON/Daten per fetch; kein `eval`/CDN | §3 |
| 8 | Sender validieren | `sender.id`/`.origin` pruefen; Origin ist keine Auth | §3 |
| 9 | Storage-Area waehlen | local=Quelle, sync=Backup (8 KB/Item), session=RAM (default unsichtbar) | §4 |
| 10 | Netzwerk-Regeln | `declarativeNetRequest` statt blockierendem `webRequest` | §5 |
| 11 | Audio/Mikrofon | Nie im SW → Offscreen-Doc; Web Audio gegen SODA | §7 |
| 12 | Native Messaging | Host-`name` exakt; `.bat`-Wrapper; bei Disconnect reconnecten | §8 |
| 13 | Debugging | Idle-Bugs OHNE offene DevTools reproduzieren; gepackte Version testen | §9, §10 |
| 14 | Store-Publish | Single Purpose, jede Permission begruenden, Privacy-Tab | §10 |
| 15 | STT-Diktat mit Live-Vorschau | Vorschau ins schwebende Overlay (nie ins `contenteditable`); `previewActive`-Riegel → nur finale Whisper-Fassung ins Feld | §7 |

## Inhalt
1. [Service Worker / Background](#1-service-worker--background)
2. [Content Scripts, Injection & Messaging](#2-content-scripts-injection--messaging)
3. [Permissions, CSP & Sicherheit](#3-permissions-csp--sicherheit)
4. [Storage & State](#4-storage--state)
5. [declarativeNetRequest & Networking](#5-declarativenetrequest--networking)
6. [Plattform-/UI-APIs (Offscreen, Side Panel, Action, Alarms …)](#6-plattform-ui-apis)
7. [Audio / Media / Mikrofon / TTS](#7-audio--media--mikrofon--tts)
8. [Native Messaging & Plattform-Integration](#8-native-messaging--plattform-integration)
9. [Debugging, Reload & Dev-Workflow](#9-debugging-reload--dev-workflow)
10. [Chrome Web Store: Compliance & Veroeffentlichung](#10-chrome-web-store-compliance--veroeffentlichung)
11. [Versions-Timeline (Chrome 105–148)](#11-versions-timeline-chrome-105148)
12. [Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach](#12-bezugs-tabelle-best-practice--bug-almanach)

---

## 1. Service Worker / Background

Der MV3-Background ist ein **ephemerer Service Worker** (kein Dauer-Prozess): nach ~30 s Idle terminiert,
bei Events neu gestartet. Die Architektur muss „stateless" sein.

1. **Event-Listener IMMER synchron auf Top-Level registrieren** (wichtigste Regel). Alle `chrome.*`-Listener
   (`onMessage`, `onInstalled`, `onClicked`, `alarms.onAlarm` …) im globalen Scope, NICHT nach `await`/in
   `.then()`/in einer Funktion. Bei einem Event startet der terminierte SW neu und durchlaeuft das Skript von
   oben — nur synchron registrierte Listener fangen das ausloesende Event. (offiziell: Events in service workers)
2. **Kein globaler JS-State als Speicher.** Globale Variablen sterben bei jeder Terminierung. State der einen
   Neustart ueberleben muss → `chrome.storage.local`/`.session`/IndexedDB; bei jedem Handler-Start frisch lesen,
   globale Variable nur als Lese-Cache. (offiziell: SW lifecycle)
3. **`chrome.storage.session` fuer fluechtigen RAM-State** (Tokens, temporaere Flags) — 10 MB, geleert bei
   Disable/Reload/Update/Browser-Neustart. Default nicht fuer Content-Scripts sichtbar (s. 4.8). (offiziell)
4. **Keep-Alive = `chrome.alarms`** (der offizielle Weg). Min-Periode 30 s seit Chrome 120 (vorher 60 s).
   Alarme wecken den SW auch nach Terminierung — anders als `setTimeout`/`setInterval`. Bei SW-Start
   Alarm-Existenz pruefen (`alarms.get`) statt blind neu erstellen. (offiziell: alarms)
5. **Async-Message: Promise zurueckgeben (Chrome 148+) ODER `return true`.** Den Listener NICHT als `async`
   deklarieren (gibt implizit `undefined`-Promise zurueck → Kanal schliesst sofort, „message port closed").
   Ab Chrome 148 darf der Listener direkt ein Promise zurueckgeben; <148: `return true` + spaeteres
   `sendResponse`. `return true` bleibt der kompatibelste Weg. (offiziell: messaging)
6. **ES-Module nur statisch**, im Manifest `"background": { "service_worker": "sw.js", "type": "module" }`.
   `import()` (dynamisch) ist im SW NICHT unterstuetzt — alles muss zur Install-Zeit bekannt sein (Bundler). (web.dev `extern` + offiziell)
7. **Lange Tasks chunken.** Harte Limits: eine Anfrage max 5 min, eine `fetch()`-Response max 30 s. Lange
   Arbeit per `chrome.alarms` fortsetzen oder in ein Offscreen-Document auslagern. (offiziell: SW lifecycle)
8. **Idle-Timer gezielt verlaengern** statt Hacks: ab Chrome 110 setzt jedes Event + jeder API-Call den 30-s-Timer
   zurueck; zusaetzlich verlaengern WebSocket-Traffic (116+), aktive Debugger-Session (118+), Native-Messaging
   (105+), offene Long-Lived-Ports (114+), Offscreen-Nachrichten (109+). Der Port-„Highlander"-Trick (Dauer-Port
   + 25-s-Ping) ist inoffiziell/fragil — nur Notnagel. (offiziell + Blog „Longer ESW lifetimes")
9. **Install/Update in `onInstalled`** (top-level registriert): Default-Storage, Context-Menues, Alarme dort
   einmalig anlegen. Migrationen versioniert (reason `update`: Schema-Version lesen → migrieren → schreiben).
   `onInstalled` feuert NICHT bei jedem Browser-Start — pro-Sitzung-Init zusaetzlich in `onStartup` + idempotent. (offiziell)

---

## 2. Content Scripts, Injection & Messaging

1. **Injection-Timing: `document_idle` (Default) fuer UI.** `document_start` nur fuer Dinge die VOR Seiten-JS
   laufen muessen (API-Hooks im MAIN world); `document_end` direkt nach DOM. (offiziell: content-scripts)
2. **Statisch vs. dynamisch.** Manifest-`content_scripts` werden zuerst injiziert. Dynamisch via
   `chrome.scripting.registerContentScripts()`/`updateContentScripts()` (seit Chrome 96) wenn das Match-Pattern
   erst zur Laufzeit feststeht; on-demand via `executeScript()` (braucht host_permissions ODER `activeTab`).
   `executeScript({func})`: Closure geht verloren, Werte nur ueber `args` (JSON-serialisierbar) uebergeben. (offiziell: scripting)
3. **Isolated World (Default) vs. MAIN World.** Isolated = privates JS-Environment, Extension-CSP gilt.
   `world: "MAIN"` teilt Kontext mit Seiten-JS → es gilt die **CSP der Seite**, kein `chrome.*`-Zugriff, auch
   fuer andere Extensions sichtbar. MAIN nur wenn man Seiten-Globals abgreifen/patchen muss; Datenfluss
   MAIN → isoliert → SW. (offiziell)
4. **In offene Tabs nachinjizieren.** Manifest-Content-Scripts kommen erst bei der naechsten Navigation in schon
   offene Tabs → bei `onInstalled`/`onStartup` per `executeScript` nachinjizieren (mit Lade-Guard gegen
   Doppelinjektion: `if (window.__myExtInjected) return;`). Bei SPAs (`history.pushState`) per
   `webNavigation.onHistoryStateUpdated`/MutationObserver neu initialisieren. (offiziell: content-scripts)
5. **Frame-Targeting bewusst setzen.** `all_frames:true` fuer Cross-Origin-iframes (+ passende `matches`/
   host_permissions); `match_about_blank` fuer `about:blank`-Frames; `match_origin_as_fallback` fuer `about:`/
   `data:`/`blob:`/`filesystem:`-Frames. (offiziell)
6. **Async-Response: ab Chrome 148 Promise-Rueckgabe** (sauber). <148: NICHT `async`-Listener, sondern `return
   true` + manuell `sendResponse`. Bei Tab → `tabs.sendMessage`, Background → `runtime.sendMessage`;
   `runtime.lastError` immer behandeln. (offiziell: messaging)
7. **One-shot vs. long-lived Port.** `sendMessage` (Promise) fuer Einzelanfragen; `connect()`/`Port` fuer
   Streaming/haeufigen Austausch (laufendes TTS/Transkript). Max Message-Groesse **64 MiB**. `port.onDisconnect`
   fuer Cleanup — kann mehrfach feuern, also idempotent. Bei BFCache (Chrome 123+) brechen Ports → bei
   Reaktivierung neu verbinden. (offiziell)
8. **Shadow DOM gegen Seiten-CSS.** Overlay als Host-Element direkt an `document.body` (Wurzel-Stacking-Context),
   `attachShadow({mode:'open'})`, eigenes `<style>` im ShadowRoot, `:host { all: initial; }` als Reset. Kapselt
   Styles in beide Richtungen und loest `z-index`-Probleme durch fremde Stacking-Contexts. (offiziell dom + `extern`)
9. **MAIN-world-Code als externe Datei** (`web_accessible_resources`) per `executeScript({world:"MAIN", files})`
   laden — NICHT als injiziertes Inline-`<script>` (Host-CSP blockt Inline). Auf TT-Seiten kein `innerHTML` mit
   Strings (s. Abschnitt 3.11). (offiziell)
10. **`chrome.userScripts` fuer dynamischen Nutzer-Code** (Tampermonkey-artig) statt MAIN-world-Hacks. Seit
    Chrome 120 (Permission `"userScripts"` + host_permissions); `worldId` ab 133, `execute()` ab 135. World
    `USER_SCRIPT` ist von der Seiten-CSP befreit. **Ab Chrome 138 eigener Toggle „Allow user scripts"** pro
    Extension statt Developer-Mode — Verfuegbarkeit per try/catch pruefen, nicht per Versionsnummer. (offiziell: userScripts)

---

## 3. Permissions, CSP & Sicherheit

1. **`activeTab` statt breiter Hosts.** Temporaerer Zugriff auf den aktiven Tab NACH einer Nutzer-Geste —
   **KEINE Install-Warnung**, im Gegensatz zu `<all_urls>`. Reduziert Reibung UND Store-Review-Risiko. Mit
   `"scripting"` kombinieren. (offiziell: activeTab)
2. **Least Privilege.** Nur Permissions fuer aktuell implementierte Features. Store-Policy verbietet
   „future-proofing". Host-Permissions so eng wie moeglich (`https://api.example.com/*` statt `*/*`). (offiziell: program-policies)
3. **`optional_permissions`/`optional_host_permissions` + Runtime-Request** (`chrome.permissions.request()` in
   einem Geste-Handler). Haelt die Install-Warnung minimal; `permissions.contains()` vor Nutzung. WICHTIG: eine
   spaeter hinzugefuegte *fixe* Permission mit Warnung **deaktiviert die Erweiterung beim Update** bis Re-Consent
   → neue Berechtigungen IMMER optional. (offiziell: permissions)
4. **Kein Remote-Code.** ERLAUBT: Remote-**JSON-Konfig** laden+cachen, externe APIs aufrufen, lokal gebuendelte
   (vendored) Libraries, `executeScript({func, args})` mit lokaler Funktion. VERBOTEN: extern gehostetes
   JS/WASM/CSS, CDN-Libs, `eval()`/`new Function()` im Extension-Kontext. Verhalten ueber **Daten** steuern. (offiziell: migrate/security)
5. **CSP als Objekt, `extension_pages` strikt lassen.** MV3 erwartet `{ "extension_pages": "...", "sandbox": "..." }`.
   Default `script-src 'self'; object-src 'self';`; maximal `+ 'wasm-unsafe-eval'`. `'unsafe-eval'`/`'unsafe-inline'`/
   externe Quellen sind nicht erlaubt (Upload wird abgelehnt). (offiziell: CSP)
6. **`eval`-beduerftigen Code in `sandbox`-Seiten.** Sandbox-CSP ist lockerer (`'unsafe-inline' 'unsafe-eval'`),
   hat aber KEINEN Extension-API- und keinen Parent-DOM-Zugriff → kontrollierter Blast-Radius. Daten nur per
   validiertem `postMessage`. (offiziell: CSP)
7. **`web_accessible_resources` mit `matches` + `use_dynamic_url: true`.** `use_dynamic_url` (in WAR ab Chrome 130)
   rotiert die Ressourcen-ID pro Session → schuetzt vor **Extension-Fingerprinting**. `matches` nur auf noetige
   Origins, keine Wildcards. (offiziell: web-accessible-resources)
8. **Nachrichten-Sender validieren (ClaudeBleed-Klasse).** In `onMessageExternal`/`onConnect` `sender.id` gegen
   Allowlist, bei Web-Sendern `sender.origin`/`.url`; in `onMessage` Content-Script-Nachrichten als potenziell
   manipuliert behandeln. Origin-Vertrauen ist KEINE Authentifizierung — Page-Auth per signiertem Einmal-Token.
   `externally_connectable` = Default-Deny, `matches`/`ids` minimal. (offiziell: messaging + stay-secure; LayerX `extern`)
9. **`postMessage` mit Origin-Check (MAIN ↔ Isolated).** Senden mit konkretem `targetOrigin` (nie `'*'`);
   empfangen `if (event.source!==window || event.origin!==location.origin) return;` + erwarteten Nachrichten-Tag.
   Sensible Daten direkt Content-Script ↔ SW (`chrome.runtime`), nicht ueber die Page-Bruecke. (offiziell)
10. **`declarativeNetRequest` statt blockierendem `webRequest`** (Privacy + weniger/keine Warnung): der Browser
    wertet Regeln selbst aus → Extension sieht Request-Inhalte nicht. (offiziell — Details Abschnitt 5)
11. **Kein `innerHTML`/`document.write` in Extension-Seiten + Trusted Types beachten.** DOM-API +
    `textContent`/`append`. Auf Seiten mit `require-trusted-types-for 'script'` (Google-Properties) wirft
    Content-Script-`innerHTML` einen TypeError → eigene Trusted-Types-Policy (`trustedTypes.createPolicy` mit
    DOMPurify, lokal vendored). (offiziell: stay-secure + Trusted-Types-Wiki)
12. **Keine Secrets im Bundle / Content-Script.** Die Extension ist NICHT vertraulich; API-Keys sind auslesbar.
    Secrets bleiben im SW (oder OAuth/`chrome.identity`/kurzlebige Server-Tokens); Content-Script bittet nur um
    eng begrenzte Aktionen. Nur HTTPS, sensible Daten im Body (nicht URL/Header). (offiziell: stay-secure)
13. **Dev-Konto haerten** (2FA/Security-Key), Manifest-Felder minimieren — jedes ungenutzte Feld = Angriffsflaeche. (offiziell)

---

## 4. Storage & State

1. **Storage-Area nach Zweck waehlen (Quotas exakt):**

   | Area | Quota | Pro-Item | Schreibrate | Persistenz | Content-Script |
   |------|-------|----------|-------------|------------|----------------|
   | **local** | 10 MB (5 MB ≤Chrome 113) | — | keins | bis Deinstallation | sichtbar |
   | **sync** | ~100 KB total, 512 Items | **8 KB** | **120/min, 1800/h** | geraeteuebergreifend | sichtbar |
   | **session** | 10 MB (1 MB ≤Chrome 111) | — | keins | RAM, weg bei Reload/Restart | **NICHT** (default) |
   | **managed** | admin | — | read-only | admin-gesetzt | sichtbar |

   Falsche Area = Quota-Crash (grosse Daten in sync) oder Datenverlust (langlebiges in session). (offiziell: storage)
2. **Promise-API statt Callback** (`await chrome.storage.local.get(...)`) — sauberes try/catch. (offiziell)
3. **Hot-State im RAM-Cache + gezielt persistieren.** Einmal pro SW-Lauf laden, in Variable halten, gezielt
   zurueckschreiben (spart sync-Schreibrate, reduziert Races). Vor riskanten Punkten persistieren. (`extern` chromium-extensions + offiziell)
4. **`localStorage`/`sessionStorage` gibt es im SW NICHT** (kein `window`) — MV2-Migrations-Footgun. Immer
   `chrome.storage` (async) oder fuer Web-Storage-Zwang ein Offscreen-Document. (offiziell: storage-and-cookies)
5. **Binaerdaten (Blob/ArrayBuffer) in IndexedDB**, nicht chrome.storage — letzteres JSON-serialisiert alles,
   Binaerdaten explodieren in Groesse und verlieren Typen (`Date`→String, `Map`/`Set`→`{}`). (offiziell)
6. **`unlimitedStorage`-Permission** hebt das local-10-MB-Limit auf UND schuetzt local/IndexedDB/CacheStorage vor
   Eviction; zusaetzlich `navigator.storage.persist()`. (offiziell)
7. **Viele kleine Keys statt einem grossen Objekt** (partielle Updates, kleinere onChanged-Payloads, weniger
   Lost-Update-Schaden) — aber sync hat `MAX_ITEMS`=512, nicht zu granular. sync-Writes debouncen (~800 ms). (offiziell + abgeleitet)
8. **`storage.session`-Sichtbarkeit explizit setzen**, wenn Content-Scripts ran muessen:
   `setAccessLevel({accessLevel:'TRUSTED_AND_UNTRUSTED_CONTEXTS'})` aus Trusted Context. Default =
   `TRUSTED_CONTEXTS`. Keine Secrets in session (onChanged leakt unabhaengig vom Level). (offiziell)
9. **Keine Transaktionen → Lost-Update vermeiden.** Parallele read-modify-write (zwei Tabs / SW + Popup)
   clobbern sich. Single-Writer ueber den SW serialisieren ODER Write-Queue/Promise-Chain ODER granulare Keys.
   Fuer echte Nebenlaeufigkeit IndexedDB mit Transaktionen. (`extern` + offizielle Bestaetigung „no transactions")
10. **`onChanged`-Listener top-level** (ausserhalb React/Komponenten) registrieren, damit Options-/Side-Panel
    frisch gesyncte Werte live anzeigt. (offiziell + chromium-extensions)

---

## 5. declarativeNetRequest & Networking

1. **Drei Regel-Typen richtig waehlen.** *Statisch* (`rule_resources`, mit Extension installiert) fuer grosse
   stabile Listen; *dynamisch* (`updateDynamicRules`, ueberlebt Neustart+Upgrade) fuer persistente Laufzeit-Regeln;
   *session* (`updateSessionRules`, weg bei Shutdown/neuer Version) fuer temporaere pro-Tab-Logik. (offiziell: DNR)
2. **Regel-Limits kennen (Chrome 148):** statisch 30.000 garantiert, bis 100 Rulesets/max 50 aktiv; session 5.000;
   dynamisch „safe" 30.000 / „unsafe" 5.000; Regex max 1.000/Typ, jede Regel <2 KB. Restkontingent live per
   `getAvailableStaticRuleCount()`. (offiziell + Blog content-filtering)
3. **„Safe" vs „unsafe" actions.** Safe (30.000-Limit): `block`, `allow`, `allowAllRequests`, `upgradeScheme`.
   Unsafe (nur 5.000): v.a. `redirect`, `modifyHeaders`. Wo moeglich auf safe ausdruecken. (offiziell: Blog content-filtering)
4. **Action-Praezedenz verstehen.** Innerhalb der eigenen Extension bei gleicher `priority`:
   `allow`/`allowAllRequests` > `block` > `upgradeScheme` > `redirect`. Ueber Extensions hinweg: `block` >
   `redirect`/`upgradeScheme` > `allow`. Ausnahme zu Block-Regel = allow mit ≥ priority. (offiziell)
5. **Response-Header-Matching (seit Chrome 128)** fuer `Content-Type`-Filtern — laeuft ERST nach Header-Empfang,
   der Request erreicht den Server. Nicht fuer „muss vor dem Request blocken" — dafuer urlFilter/regexFilter. (offiziell)
6. **`modifyHeaders`:** `append` nur fuer eine feste Header-Whitelist (u.a. accept, cookie, user-agent), sonst
   `set`. Hat eine Regel `set`/`remove` angewandt, koennen nachrangige Regeln den Header nicht weiter aendern —
   `priority` bewusst setzen. `user-agent` erlaubt, `origin` NICHT. (offiziell)
7. **`fetch()` im Service Worker statt im Content Script.** Mit host_permissions ist der SW-fetch CORS-frei und
   ohne Preflight (Extension-Origin). Content-Scripts laufen in der Page-Origin und unterliegen der
   Same-Origin-Policy — host_permissions hebeln das nicht aus. Pattern: Content-Script → `sendMessage` → SW
   `fetch` → Antwort. (offiziell: network-requests)
8. **Statisches Ruleset sauber deklarieren** (`rule_resources[]` = `{id, enabled, path}` + Permission), sonst
   greift es still nicht. DNR-Debugging (`getMatchedRules`/`onRuleMatchedDebug`) nur unpacked mit
   `declarativeNetRequestFeedback`. (offiziell)
9. **Permission-Wahl:** `declarativeNetRequestWithHostAccess` (keine Install-Warnung, braucht host_permissions
   fuer redirect/modifyHeaders) wo man ohnehin Hosts hat; `declarativeNetRequest` (Warnung) fuer reines
   block/allow ohne Host. (offiziell)
10. **Migration: was NICHT mehr geht.** Body-basiertes Blocken/Modifizieren ist unmoeglich (DNR rein deklarativ,
    `webRequest` nur noch observational) → Inhalt im SW per eigenem `fetch()` laden, dort verarbeiten.
    `isUrlFilterCaseSensitive` default seit Chrome 118 = false (portierte MV2-Listen pruefen). (offiziell)

---

## 6. Plattform-/UI-APIs

**Offscreen Documents (`chrome.offscreen`, Chrome 109+)**
1. **DOM/Audio/Clipboard im Hintergrund nur ueber ein Offscreen-Document** — der SW hat kein `window`/DOM.
   `permissions: ["offscreen"]`, statische gebuendelte HTML, `justification` Pflicht. (offiziell)
2. **Vor `createDocument` Existenz pruefen** (`getContexts({contextTypes:['OFFSCREEN_DOCUMENT']})` / `hasDocument()`,
   Chrome 116+) — nur EIN Offscreen-Doc pro Extension. Create-Aufrufe ueber eine Promise serialisieren (Single-Flight).
3. **Kommunikation nur ueber `chrome.runtime`-Messaging** (kein direkter DOM-Zugriff vom SW, `window.opener` ist
   `null`); Ziel adressieren (`target:'offscreen'`-Feld), damit Popup/Panel-Listener nicht faelschlich konsumieren.

**Side Panel (`chrome.sidePanel`, Chrome 114+)**
4. **Global via Manifest** (`"side_panel": { "default_path": … }`) oder **pro-Tab** via `setOptions({tabId, …})`,
   Permission `"sidePanel"`.
5. **Icon-Klick oeffnet Panel** ohne eigenen Handler: `setPanelBehavior({openPanelOnActionClick:true})` einmalig
   in `onInstalled`. `open()` (ab 116) NUR synchron in direkter Antwort auf eine User-Geste (kein `await` davor).
   Panel nicht via leerem Path schliessen (kann crashen).
6. **Popup vs Side Panel:** Popup fuer kurze Interaktion (schliesst bei Fokusverlust, max ~800×600); Side Panel
   fuer persistente UI die offen bleibt (Vorlese-Steuerung/Overlays).

**Action / Alarms / contextMenus / commands / notifications / i18n**
7. **`chrome.action`** ersetzt browserAction/pageAction; `onClicked` feuert NUR wenn KEIN Popup gesetzt ist (sonst
   `setPopup({popup:''})`). `openPopup()` ab Chrome 127 (nur Geste). Badge ≤4 Zeichen; Icons 16/32/48/128 PNG (kein SVG).
8. **Im SW immer `chrome.alarms` statt `setTimeout`/`setInterval`.** Min 30 s (released, Chrome 120+), unter
   0,5 min wird hochgesetzt (Dev ok, Store kaputt). Max 500 Alarme; persistieren ueber Browser-Neustart.
9. **contextMenus nur in `onInstalled`**, dort `removeAll()` vor `create()` (sonst Duplicate-ID nach SW-Neustart).
   Klick via `contextMenus.onClicked`.
10. **commands:** max 4 vorgeschlagene Shortcuts; Ctrl ODER Alt zwingend, `Ctrl+Alt` verboten; `_execute_action`
    speziell; reservierte/konfligierende Shortcuts schlagen still fehl → Nutzer auf `chrome://extensions/shortcuts`
    verweisen. Mac: Ctrl→Command automatisch, `MacCtrl` fuer echtes Control.
11. **notifications:** `iconUrl`/`title`/`message`/`type` Pflicht; stabile `chrome.runtime.getURL(...)`-iconUrl
    (sonst Silent Failure); `lastError` lesen.
12. **i18n:** sobald `/_locales` existiert, ist `"default_locale"` Pflicht (sonst Load-Fehler); `__MSG_name__` in
    Manifest/CSS, `chrome.i18n.getMessage()` in JS. `chrome.i18n` folgt der Browser-Sprache (keine
    Laufzeit-Umschaltung — dafuer eigene Strings aus `chrome.storage`).

---

## 7. Audio / Media / Mikrofon / TTS

Fuer Overlays/Voice/TTS zentral — der SW kann KEIN Audio/Media. (offiziell, Quellen je BP)

1. **Audio/Media NIE im Service Worker, IMMER im Offscreen-Document** (oder Content-Script). SW hat kein
   `navigator.mediaDevices`, kein `<audio>`/Web-Audio. `reason` semantisch korrekt: `USER_MEDIA` (Mikrofon),
   `AUDIO_PLAYBACK` (Wiedergabe), `DISPLAY_MEDIA`/`WEB_RTC` (Capture). (offiziell: offscreen)
2. **`AUDIO_PLAYBACK`-Offscreen schliesst nach 30 s Stille** (alle anderen Reasons NICHT). Bei diskontinuierlicher
   TTS-Wiedergabe vor jedem Playback `hasDocument()` pruefen und ggf. neu erstellen. (offiziell)
3. **Mikrofon-Permission EINMAL ueber einen echten Extension-Tab holen.** `getUserMedia()` schlaegt in
   Offscreen/Popup/Side-Panel fehl, solange die Mic-Permission fehlt (kein Prompt-Pfad). Loesung: eine
   vollwertige Extension-Seite als Tab oeffnen (`chrome-extension://`), dort `getUserMedia({audio:true})` →
   Chrome zeigt den Prompt; danach gilt sie fuer die Origin und ist im Offscreen nutzbar. `"microphone"` ist
   KEINE manifest-`permissions`. (offiziell-Pattern + `extern`)
4. **Audio-Wiedergabe ueber Web Audio API** (`AudioContext` + `decodeAudioData` + `AudioBufferSourceNode`) statt
   `<audio>`/`new Audio()` — vermeidet, dass Chrome Live Caption/SODA mittranskribiert, und gibt praezise
   Kontrolle. (offiziell: web-audio-faq)
5. **Autoplay-Policy:** `AudioContext` startet ohne User-Geste `suspended`. Im Klick-Handler
   `if (ctx.state==='suspended') await ctx.resume()`; Wiedergabe an einen sichtbaren Play-Button binden. (offiziell: autoplay)
6. **TTS-Wahl: `chrome.tts` laeuft im Service Worker** (event-getrieben, nur `"tts"`-Permission, plattform-native
   Synthese) — meist die robustere Wahl. Web Speech `SpeechSynthesis` ist Web-Content-API und braucht DOM
   (Offscreen/Content-Script). **(Korrektur einer haeufigen Annahme: `chrome.tts` braucht KEINEN DOM-Kontext.)** (offiziell: tts)
7. **TTS-Lebenszyklus ueber `onEvent`** (`start`/`end`/`word`/`interrupted`/`error`): naechsten Absatz erst nach
   `end`, UI-Reset bei `error`. Voices via `getVoices()` VOR `speak()` laden und per `{voiceName}` waehlen;
   Queue mit `{enqueue:true}`, `stop()` leert sie. (offiziell)
8. **tabCapture:** `getMediaStreamId()` im SW (User-Geste noetig, StreamID einmalig + laeuft schnell ab), StreamID
   im Offscreen nutzbar ab Chrome 116. Achtung: tabCapture/getDisplayMedia **unterdrueckt lokale Wiedergabe** →
   Stream per Web Audio aktiv zum Lautsprecher zurueck-routen, sonst hoert der Nutzer nichts. Fuer dauerhafte
   Aufnahme Offscreen statt Content-Script (das endet bei Navigation). (offiziell: tabCapture / screen-capture)
9. **Aufnahme fuer Whisper/Groq:** `MediaRecorder`/`AudioWorklet` im Offscreen sammeln (nicht SW), Chunks per
   Message/`fetch` zur Transkription. (`extern` Pattern)
10. **WebSocket-TTS (z. B. Edge-TTS):** Ziel-Host als `wss://` in `host_permissions`; abweichenden User-Agent
    via declarativeNetRequest `modifyHeaders` `set` (nicht `append`), nicht ueber webRequest. (offiziell: DNR)

---

### 7.x STT-Diktat mit Live-Vorschau (Hybrid: Web Speech live + Whisper final)

Die rohen interim results der Web Speech API **NIE ins Seiten-`contenteditable` schreiben** —
Paste-Simulation selektiert+ersetzt staendig → Springen/Flackern, und die rohe Vorschau kann versehentlich
gesendet werden. Stattdessen:

- **Live-Vorschau in ein SEPARATES schwebendes Overlay-Element** (eigenes `#stt-live-preview`, final deckend
  / interim gedimmt-kursiv); das Seiten-Feld bleibt unberuehrt → kein Springen.
- **Zeitlicher Riegel `previewActive`** (true bei Start, false sofort beim Stopp + beim Entfernen): der
  `onresult`-Handler beginnt mit `if (!previewActive) return;`. Nach dem Stopp schreibt AUSSCHLIESSLICH die
  finale Whisper-Fassung (mit Satzzeichen) ins Feld, die rohe Vorschau nie — und der Auto-Send greift nur die
  finale Fassung ab.
- **Fallback bei STT-Ausfall:** Vorschau behalten + sichtbarer Hinweis ("unkorrigiert, ohne Satzzeichen") —
  funktionserhaltend, nie stillschweigend als echte Fassung.

Generisches, technologie-neutrales Muster: `best-practices/desktop/voice-pipeline.md` §9. Bug-Gegenseite:
`bugs/web/chrome-extensions.md` #74. (eigener Vorfall 2026-06-24, overlays 0.6.4 — von Frank bestaetigt)

## 8. Native Messaging & Plattform-Integration

Bruecke zu nativen Desktop-Apps (Windows + macOS). Permission `"nativeMessaging"`; in Content-Scripts NICHT verfuegbar.

1. **Host-Manifest exakt.** `name` identisch in Manifest, Registry-Key/Dateiname und `connectNative("…")`-Argument
   (`[a-z0-9._]`). `type:"stdio"`, `path` ueberall absolut, `allowed_origins` = exakte `chrome-extension://<ID>/`
   (keine Wildcards). Windows-JSON: Backslashes verdoppeln, keine trailing comma. (offiziell)
2. **Registrierung pro Plattform.** Windows: Registry `HKCU/HKLM\SOFTWARE\Google\Chrome\NativeMessagingHosts\<name>`,
   (Default) = absoluter Manifest-Pfad; 32/64-bit-View beachten (`WOW6432Node`). macOS:
   `~/Library/Application Support/Google/Chrome/NativeMessagingHosts/<name>.json` (Dateiname == name). (offiziell)
3. **Chrome 113+ startet `.exe`-Hosts direkt** (ohne cmd.exe) — relative Working-Dirs brechen. Skript-Hosts
   (Python/Node) ueber `.bat`-Wrapper mit absolutem Interpreter- + Skript-Pfad. Enterprise:
   Policy `NativeHostsExecutablesLaunchDirectly` (ab 120). (offiziell + chromium-extensions)
4. **SW-Lebensdauer + Port.** Offener `connectNative()`-Port haelt den SW am Leben (Native Messaging zaehlt ab
   Chrome 105 als Idle-Reset), aber fragil → bei `onDisconnect` SOFORT reconnecten (Backoff), periodische Pings,
   Verbindungs-State in `chrome.storage.session` (nicht im SW-RAM). (offiziell + `extern`)
5. **`connectNative` (persistent, fuer Streams) vs `sendNativeMessage` (one-shot, startet Prozess je Aufruf).** (offiziell)
6. **Wire-Protokoll & stdout-Disziplin.** 32-bit Laenge + UTF-8-JSON; Limits 64 MiB (an Host) / 1 MB (an Chrome,
   groesseres chunken). **stdout ist heilig** — nur Frames, Debug auf stderr/Logfile. Windows: stdin/stdout auf
   `O_BINARY` (sonst Frame-Korruption). Erstes Host-Argument = Caller-Origin. (offiziell)
7. **Edge & Enterprise.** Edge eigener Registry-Pfad (`…\Microsoft\Edge\NativeMessagingHosts\`), beide
   Extension-IDs in `allowed_origins`. `ExtensionInstallForcelist` braucht gueltige `update_url` (sonst keine
   Installation). (Microsoft Learn + `extern`)
8. **Sicherheit:** `allowed_origins` minimal; Host validiert JEDE eingehende Message (Schema/Whitelist, keine
   ungeprueften Shell-Aufrufe) — er laeuft mit voller User-Berechtigung; Manifest/Binary schreibgeschuetzt ablegen. (offiziell)

---

## 9. Debugging, Reload & Dev-Workflow

1. **SW-DevTools-Heisenbug.** SW via `chrome://extensions` → „Inspect: service worker" oeffnen — aber Idle-Bugs
   (State-Verlust, fehlende Listener) NUR **ohne** offene DevTools reproduzieren: offene DevTools halten den SW
   kuenstlich am Leben. „service worker (inactive)" ist normal (suspended), kein Fehler. (offiziell: debug)
2. **SW gezielt terminieren** (DevTools → Application → Service Workers → stop, oder
   `chrome://serviceworker-internals`) statt 30 s zu warten — deckt State-Verlust + nicht-registrierte Listener auf. (offiziell)
3. **Content-Script-Logs** landen in der **Page**-Console → Context-Dropdown auf den Extension-Context stellen.
   Der „Errors"-Button auf `chrome://extensions` zeigt nur runtime/warn/error. (offiziell)
4. **Reload-Disziplin unpacked:** nach Code-Aenderung Reload-Button + betroffene Tabs refreshen (alte
   Content-Scripts leben sonst weiter). Manifest-Aenderung = voller Reload (kein HMR). Doppelinjektion-Guard im CS. (offiziell + `extern`)
5. **Update-Lifecycle:** `onUpdateAvailable` abonnieren und an einem Idle-Moment `chrome.runtime.reload()` — Updates
   installieren nur im Idle (kein offenes Popup/Panel/Options). `requestUpdateCheck()` meist unnoetig (Throttling). (offiziell)
6. **Memory/Performance:** bei SPA-Navigation Observer/Listener/Intervalle aufraeumen
   (`disconnect`/`removeEventListener`/`clearInterval`), detached DOM nodes vermeiden (ggf. WeakMap),
   Heap-Snapshot („Detached" filtern). Pro-Extension-Speicher auf `chrome://extensions` grob lokalisieren. (`extern` + DevTools-Praxis)
7. **Versions-Logging:** beim SW-Start `console.log(chrome.runtime.getManifest().version)` — verhindert das
   Debuggen einer alten geladenen Version. Source Maps fuer gebundelten Code aktivieren. (offiziell + Praxis)
8. **Tooling vorsichtig:** CRXJS/`@crxjs/vite-plugin` (HMR) war lange unklar gewartet — Stand pruefen, ggf. WXT
   erwaegen. (`extern`)
9. **Test-Automatisierung:** branded Chrome 139+ hat `--load-extension`/`--disable-extensions-except`/
   `--extensions-on-chrome-urls` entfernt → Playwright mit gebuendeltem Chromium (`launchPersistentContext`),
   Puppeteer mit Chrome for Testing. Extensions laufen nur mit persistent context. (offiziell/Tool)

---

## 10. Chrome Web Store: Compliance & Veroeffentlichung

**MV2 ist tot** — MV3 ist die einzige Option (seit 24.07.2025 komplett deaktiviert, mit Chrome 139 Support +
Enterprise-Policy entfernt). Vor Veroeffentlichung die **Privacy-Dashboard-Pflichtfelder**: Single-Purpose,
Permission-Justification je Permission, Remote-Code-Deklaration, Data-Usage-Zertifizierung, Privacy-Policy-Link.
**Disclosures muessen mit der Privacy-Policy uebereinstimmen.** 2-Step-Verification am Konto + einmalige 5$-Gebuehr.
Abgelehntes Update nimmt die Live-Version NICHT offline; nur 1 Appeal pro Verstoss (2025).

**Die haeufigsten Review-Ablehnungsgruende vermeiden** (`developer.chrome.com/docs/webstore/troubleshooting`):

| Code | Grund | So vermeiden |
|------|-------|--------------|
| Purple Potassium (#1) | Excessive Permissions | schmalste Permission, `activeTab` statt breiter Hosts, jede begruenden |
| Red Argon/Magnesium | Single-Purpose-Verletzung | EIN klarer Zweck; getrennte Features → getrennte Extensions |
| Purple Lithium/Nickel/Copper | Privacy (Policy/Consent/HTTP) | Privacy-Tab + VORHER offenlegen + Einwilligung + HTTPS |
| Blue Argon | Remote-Code unter MV3 | alle JS bundeln, kein eval/CDN; nur Config/Daten per fetch |
| Red Titanium | Obfuskation | minifizieren ja, verschleiern (Base64) nein |
| Yellow Magnesium | Funktion defekt | die **gepackte** (.zip) Version testen, nicht nur unpacked |
| Yellow Potassium | Minimum Functionality | echte In-Extension-Funktion, nicht nur Weiterleitung/Link |
| Yellow Zinc/Red Nickel | irrefuehrende Metadaten | Icon/Titel/Screenshots klar; beworbene = echte Funktion |
| Yellow Argon | Keyword-Stuffing | klare Beschreibung, keine Keyword-Listen |
| Grey Titanium | Affiliate ohne Disclosure | prominent offenlegen, erst nach User-Aktion einfuegen |

**`version`-Feld:** 1–4 Integer 0–65535, keine fuehrenden Nullen, jede neue Version numerisch groesser
(`1.10` > `1.9`). Display-Strings in `version_name`. Self-Hosting (CRX + `update_url`) erlaubt Consumer-Installs
nur auf Linux; Windows/macOS brauchen Enterprise-Policy.

---

## 11. Versions-Timeline (Chrome 105–148)

| Chrome | Aenderung |
|--------|-----------|
| 105 | Native-Messaging haelt SW am Leben |
| 109 | Offscreen Documents API; Offscreen-Nachrichten setzen Idle-Timer zurueck |
| 110 | jedes Event + jeder API-Call setzt 30-s-Idle-Timer zurueck |
| 111 | `world:"MAIN"` deklarativ stabil |
| 112 | `storage.session` 1 MB → 10 MB |
| 114 | Side Panel API; offene Long-Lived-Ports halten SW am Leben |
| 116 | `sidePanel.open()`; WebSocket-Traffic verlaengert SW-Lifetime; `getContexts()`; tabCapture-StreamID im Offscreen |
| 118 | DNR `isUrlFilterCaseSensitive` default false; Debugger-Session haelt SW am Leben |
| 120 | `chrome.userScripts` API; Alarm-Min-Periode 60 → 30 s; DNR dynamic/session getrennt |
| 121 | DNR safe dynamic rules Limit 30.000 |
| 122 | Promises durchgaengig fuer async-APIs |
| 123 | Alarme feuern beim Aufwachen; BFCache-Ports werden geschlossen |
| 126 | `trial_tokens`-Manifest-Feld (Origin Trials) |
| 127 | `action.openPopup()` fuer alle Extensions |
| 128 | DNR Response-Header-Matching; disabled-Extension belegt kein globales DNR-Limit mehr |
| 130 | `action.onUserSettingsChanged`, `StorageArea.getKeys()`, `use_dynamic_url` in WAR |
| 132 | `chrome.storage` in DevTools; `tabs.Tab.frozen` |
| 133 | userScripts `worldId` + `getWorldConfigurations()` |
| 135 | `userScripts.execute()` |
| 138 | userScripts: eigener „Allow user scripts"-Toggle statt Developer-Mode |
| 139 | MV2 + Enterprise-Policy entfernt; Test-Flags `--load-extension`/`--disable-extensions-except` weg |
| 140 | `sidePanel.getLayout()` |
| 141/142 | `sidePanel.close()` / `onOpened` / `onClosed` |
| 146 | Chrome for Testing: eigene Native-Messaging-Verzeichnisse |
| 148 | `browser`-Namespace; opt-in Structured-Clone-Messaging; `onMessage` darf Promise zurueckgeben |

---

## 🔗 12. Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

Jeder Best-Practice-Abschnitt verweist auf die passende Sektion im Bug-Almanach
[`bugs/web/chrome-extensions.md`](../../bugs/web/chrome-extensions.md) (Sektionen A–P, 73 Eintraege).

| Best-Practice-Abschnitt (hier) | Bug-Almanach-Sektion (dort) |
|--------------------------------|------------------------------|
| 1. Service Worker / Background | **B** (SW-Lebenszyklus), **A** (Laden/Verschwinden) |
| 2. Content Scripts & Messaging | **D** (Content Scripts/CSP), **C** (Messaging) |
| 3. Permissions, CSP & Sicherheit | **H** (Permissions/Manifest), **L** (Sicherheit) |
| 4. Storage & State | **E** (Storage), **G** (Geraete-Sync) |
| 5. declarativeNetRequest & Networking | **F** (DNR) |
| 6. Plattform-/UI-APIs | **I** (Action/Popup/i18n/Commands), **J** (Side Panel/Offscreen) |
| 7. Audio / Media / Mikrofon / TTS | **K** (Audio/Media/Mikrofon/TTS/WebSocket) |
| 8. Native Messaging & Plattform | **M** (Native Messaging & Plattform-Fallen) |
| 9. Debugging, Reload & Dev-Workflow | **P** (Debugging/Reload/Memory/Performance) |
| 10. Chrome Web Store | **O** (Web Store Review/Publishing), **N** (MV2→MV3 Migration) |

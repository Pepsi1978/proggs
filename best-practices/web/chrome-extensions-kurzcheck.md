# Chrome-Erweiterungen (Manifest V3) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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

# Vorlese-Overlay — Selbsttest & Diagnose

Das Diagnose-System hat **einen zentralen Schalter** und ist im Aus-Zustand
vollständig inaktiv (release-tauglich, keine Mehrlast). Es gibt zwei Wege, den
Selbsttest auszulösen.

---

## Diagnose-Modus (der eine Schalter)

`chrome.storage.local["vo_diag"]` — `true` schaltet alle Sonden ein, `false` aus.

- **An:** in der Service-Worker-Konsole `chrome.storage.local.set({vo_diag:true})`
- **Aus:** `chrome.storage.local.set({vo_diag:false})`

Service-Worker-Konsole öffnen: `chrome://extensions` → Vorlese-Overlay →
**„Service Worker"** (unter „Ansichten prüfen") anklicken.

---

## Was geloggt wird

Ein Eintrag = eine JSONL-Zeile mit den Feldern `zeitstempel, stufe, kategorie,
komponente, ereignis, inhalt, sitzung_id, korrelation_id, app_version`.

- **Kategorien:** `FUNKTION`, `UI_EREIGNIS`, `LAYOUT`, `ZUSTAND`, `PERFORMANCE`, `FEHLER`, `TEST`
- **Speicherort:** IndexedDB `vo_diag` / Store `logs` (Ringpuffer, max. 5000) —
  zusätzlich gespiegelt in die jeweilige DevTools-Konsole als `[VO-DIAG]`.
- **Geheimnisse** (Google-API-Key) werden automatisch maskiert.

### Logs exportieren / leeren / zählen (Service-Worker-Konsole)

```js
chrome.runtime.sendMessage({type:'VO_DIAG_EXPORT'}, r => console.log(r)); // lädt .jsonl in den Download-Ordner
chrome.runtime.sendMessage({type:'VO_DIAG_COUNT'},  r => console.log(r)); // Anzahl Einträge
chrome.runtime.sendMessage({type:'VO_DIAG_CLEAR'},  r => console.log(r)); // alle Logs löschen
// Direkt im Worker-Kontext:
await globalThis.__voDiag.readAllJsonl();   // gibt die JSONL als Text zurück
```

### App-Verbesserungsvorschläge ableiten (Service-Worker-Konsole)

Das Diagnose-System sammelt standardmäßig (immer an) auch **Nutzungssignale**
(Synthese-Latenz, Wiedergabe-Dauer, früher Abbruch, Engine-/Stimmen-Nutzung,
Fehlerquoten). `__voInsights()` wertet sie aus und gibt konkrete, priorisierte
**Verbesserungsvorschläge für die App** zurück:

```js
await __voInsights();   // Kennzahlen + priorisierte Vorschläge (hoch/mittel/niedrig)
```

Diagnose ist **standardmäßig an**. Komplett ausschalten: `chrome.storage.local.set({vo_diag:false})`.

---

## Variante A — In-App-Runner (ohne zusätzliche Software)

Bedient das Overlay vollautomatisch (Panel, Tabs, Engine-Switch, Test-Synthese,
Markierung+Vorlesen, Verschieben, Resize) und loggt jeden Schritt unter `TEST`.

In der **Service-Worker-Konsole**:

```js
__voSelftest();   // Diagnose-Modus an + Runner im aktiven Tab starten
```

Der Runner braucht ~25 s (echte Synthese). Danach den Export ausführen, um die
`.jsonl` zu erhalten:

```js
chrome.runtime.sendMessage({type:'VO_DIAG_EXPORT'}, r => console.log(r));
```

> `chrome.runtime.sendMessage({type:'VO_SELFTEST_RUN'})` funktioniert NUR aus
> einem anderen Kontext (Content-Script/Popup), **nicht** aus der SW-Konsole
> selbst — daher dort `__voSelftest()` verwenden.

> Hinweis: Es muss ein normaler Tab (http/https) im Vordergrund sein — auf
> `chrome://`-Seiten laufen keine Content-Scripts.

---

## Variante B — Playwright (vollautomatisch, von außen gesteuert)

Lädt die Erweiterung in ein frisches Chromium, löst den Runner aus, liest die
Logs aus und wertet sie aus.

```bash
cd vorlese-overlay-v2/tests
npm install          # installiert Playwright + Chromium (einmalig, ~150 MB)
npx playwright install chromium
npm run selbsttest
```

Optionen (Umgebungsvariablen):

- `VO_TESTSEITE=https://de.wikipedia.org/wiki/Test` — andere Testseite
- `VO_WARTE_MS=30000` — Wartezeit auf den Runner (Standard 30 s)

Ergebnis: `tests/selbsttest-log.jsonl` + Kurzauswertung (FATAL/ERROR/Layout).
Exit-Code 0 = stille Schleife (keine Befunde), sonst 1.

> Läuft **headed** (sichtbares Fenster), weil MV3-Erweiterungen so am
> zuverlässigsten geladen werden. Edge-TTS macht echte Netzwerk-Aufrufe; der
> Google-Test ohne Key prüft bewusst den Fehlerpfad.

# Bekannte Bugs & Fallen: Chrome-Erweiterungen (Manifest V3)

> **PFLICHT-LESEN vor JEDER Arbeit an einer Chrome-Erweiterung.**
> Diese Datei ist der kuratierte Bug-Almanach fuer Chrome-Erweiterungen. Sie wird
> VOR dem Coden gelesen, damit bekannte Fehler gar nicht erst gemacht werden
> (Poka-Yoke). Quelle: eigene Vorfaelle (BestJournal-Repo) + Recherche.
> Ergaenzung: nach jedem neuen Chrome-Extension-Bug hier einen Eintrag hinzufuegen.

---

## 1. Erweiterung verschwindet nach jedem Browser-Neustart  ⭐ HAEUFIG
**Symptom:** Unpacked-Erweiterung verschwindet komplett aus `chrome://extensions`
nach jedem Neustart. Laeuft waehrend der Session, KEIN Fehler, andere unpacked
Erweiterungen bleiben, Entwicklermodus bleibt an.
**Ursache:** Korrupter per-Erweiterung-Eintrag in Chromes "Secure Preferences",
gebunden an den **Ordner-PFAD**. NICHT die Extension-ID, NICHT der Code, NICHT
der Ordner-Inhalt, NICHT die Sync-Quota.
**FIX (zuerst!):** **Ordner umbenennen / an neuen Pfad verschieben** (`git mv altname neuname`,
im Repo behalten), dann aus dem NEUEN Pfad per "Entpackt laden" neu laden.
NICHT an key/ID/Code herumdoktern — verschiedene IDs am gleichen Pfad sind derselbe Test.
(Chromium Issue 40227980. Verifiziert 2026-06-01: ChromeOverlays→overlays.)

## 2. Erweiterung laedt nicht: `_`-Prefix-Dateien im Ordner
**Symptom:** Chrome lehnt das Laden ab / Erweiterung instabil.
**Ursache:** Chrome verbietet Dateien/Ordner mit fuehrendem `_` (z.B. `__pycache__`,
`_helper.py`). Einzige Ausnahmen: `_locales`, `_metadata`.
**FIX:** Keine `_`-Dateien im Extension-Ordner. Build-/Helfer-Skripte in `.tools/`
legen (Chrome ignoriert `.`-Ordner). `.gitignore`: `__pycache__/`, `*.pyc`, `_*.py`.

## 3. WebSocket schlaegt sofort fehl (ws.onerror beim Handshake)
**Ursache:** Ziel-Host fehlt als `wss://` in `host_permissions` — nur `https://`
reicht NICHT. Chrome blockt WS-Connects zu Hosts ohne expliziten `wss://`-Eintrag.
**FIX:** Host zusaetzlich als `wss://host/*` in `host_permissions` eintragen.

## 4. Edge-TTS WebSocket: HTTP 403 beim Handshake
**Ursache:** speech.platform.bing.com akzeptiert nur mit User-Agent der `Edg/`
enthaelt. Chrome sendet Chrome-UA; die WS-API kann den UA nicht ueberschreiben.
**FIX:** Statische `declarativeNetRequest`-Regel, die den UA fuer den Host auf
einen Edge-UA setzt (+ Permission `declarativeNetRequestWithHostAccess`). `user-agent`
ist in der DNR-modifyHeaders-Allowlist, `origin` nicht.

## 5. Unerwuenschte Live-Untertitel beim Audio-Abspielen
**Ursache:** Chromes Live Caption (SODA) transkribiert jedes Audio ueber ein
HTMLMediaElement (`new Audio()`/`<audio>`).
**FIX:** Wiedergabe ueber Web Audio API (AudioContext + decodeAudioData +
AudioBufferSourceNode) statt `<audio>` — dieser Pfad hat keinen Live-Caption-Hook.
(Sofort-Workaround fuer den Nutzer: chrome://settings/accessibility → Live-Untertitel aus.)

## 6. Content-Script fehlt in bereits offenen Tabs
**Ursache:** Manifest-`content_scripts` werden erst nach Seiten-Reload in schon
offene Tabs injiziert. Direkt nach Install/Update fehlen Buttons/Funktionen.
**FIX:** Bei Install/Startup per `chrome.scripting.executeScript` in offene Tabs
nachinjizieren + Lade-Guard gegen Doppelinjektion. Nach "Erweiterung neu laden"
die aktiven Tabs per Flag automatisch reloaden (ueberlebt `chrome.runtime.reload()`).

## 7. Geraete-Sync: chrome.storage.sync richtig nutzen
- **8 KB pro Eintrag (QUOTA_BYTES_PER_ITEM):** grosse/wachsende Daten als EINZELNE
  Eintraege speichern (pro Datensatz ein Key), nicht als ein grosses Objekt.
- **Gleiche Extension-ID auf allen Geraeten noetig** (sonst getrennte Sync-Speicher):
  fester `key` im Manifest ODER Chrome-Web-Store-Eintrag.
- **Muster:** `chrome.storage.local` = zuverlaessige Quelle pro Geraet, `.sync` =
  Cloud-Backup. Echo-frei spiegeln per Werte-Vergleich (nicht per Flag), `__sync_ts`
  fuer Last-Write-Wins, ~800ms Debounce gegen die Schreibraten-Limits.
- **Verschluesselung:** sync ist NICHT Ende-zu-Ende, ausser der Nutzer setzt eine
  Chrome-Sync-Passphrase (chrome://settings/syncSetup/advanced). API-Keys daher nur
  mit Nutzer-OK + Passphrase-Hinweis syncen.
- **Eine Sync-Quota loescht NIE die Erweiterung** — nur der eine set()-Aufruf scheitert.

## 8. Manifest-`key` aendern = neue ID = lokale Daten "weg"
**Ursache:** `key` bestimmt die Extension-ID. key hinzufuegen/aendern → neue ID →
`chrome.storage` der alten ID ist nicht mehr erreichbar (wirkt wie Datenverlust).
**FIX:** key bewusst einmal final setzen; Nutzer warnen, dass Einstellungen einmal
neu einzutragen sind (oder per Sync vom anderen Geraet kommen).

## 9. Options-/Side-Panel zeigt frisch gesyncte Werte nicht an
**Ursache:** Options-Seiten lesen Werte oft nur beim Oeffnen (`load()`), ohne
`onChanged`-Listener. Kommen Werte per Sync nach dem Oeffnen an, bleiben die Felder leer.
**FIX:** In der Options-/Panel-Seite einen `chrome.storage.onChanged`-Listener
ergaenzen, der die Felder live aktualisiert. (Sofort-Workaround: Panel neu oeffnen.)

---

## Pflicht-Checkliste vor Chrome-Extension-Arbeit
- [ ] Diese Datei komplett gelesen?
- [ ] Bei "verschwindet/instabil": ZUERST Ordner-Pfad wechseln (Punkt 1), nicht ID/Code.
- [ ] Keine `_`-Dateien im Ordner (Punkt 2)?
- [ ] WebSocket-Hosts als `wss://` in host_permissions (Punkt 3)?
- [ ] Sync: pro-Datensatz, fester key, Verschluesselung bedacht (Punkt 7)?
- [ ] Nach Code-Aenderung: Erweiterung neu laden + offene Tabs reloaden (Punkt 6)?

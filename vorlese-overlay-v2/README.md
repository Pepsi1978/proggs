# Vorlese-Overlay (Chrome-Erweiterung)

Markierten Text auf **jeder** Webseite vorlesen lassen — über ein schwebendes
Overlay (Lautsprecher + Zahnrad). Zwei Vorlese-Dienste zur Auswahl:

1. **Edge TTS** — kostenlos, kein Schlüssel nötig
2. **Google Chirp 3 HD** — über einen eigenen Google-Cloud-API-Key

Reines Vanilla-JavaScript, **kein Build-Schritt**. Manifest V3 — läuft in
**Chrome unter Windows und macOS** (sowie in anderen Chromium-Browsern wie
Edge oder Brave). Es gibt **keinen** plattformspezifischen Code: dieselbe
Erweiterung läuft auf jedem Betriebssystem identisch. Ausgabe-Sprache: **Deutsch**.

---

## Installation („Entpackte Erweiterung laden")

1. Chrome öffnen und in der Adresszeile `chrome://extensions` aufrufen.
2. Oben rechts den **Entwicklermodus** einschalten.
3. Auf **„Entpackte Erweiterung laden"** klicken.
4. Den Ordner `vorlese-overlay-v2/` auswählen.
5. Fertig — auf jeder Webseite erscheint unten rechts das Overlay.

> Nach Code-Änderungen in `chrome://extensions` bei der Erweiterung auf den
> **Neu-laden-Pfeil** klicken.

---

## Bedienung

- **Text markieren** → auf den **Lautsprecher** klicken → der Text wird vorgelesen.
- Während des Vorlesens zeigt der Button ein **Stopp-Symbol**; erneuter Klick stoppt
  sofort und leert die Wiedergabe-Queue.
- Das **Zahnrad** (unter dem Lautsprecher) öffnet die Einstellungen.
- Das Overlay lässt sich per **Ziehen** verschieben; die Position wird gemerkt.
- **Tastaturkürzel:** `Ctrl+Shift+S` (Windows/Linux) bzw. `Cmd+Shift+S` (macOS)
  liest die Markierung sofort vor. (Chrome wählt das richtige Kürzel je nach
  Betriebssystem automatisch; bei Konflikt unter `chrome://extensions/shortcuts`
  änderbar.)

---

## Google Chirp 3 HD einrichten

1. In der [Google Cloud Console](https://console.cloud.google.com/) ein Projekt
   anlegen (oder ein bestehendes nutzen).
2. Die **Cloud Text-to-Speech API** aktivieren
   ([API-Bibliothek](https://console.cloud.google.com/apis/library/texttospeech.googleapis.com)).
3. Unter **APIs & Dienste → Anmeldedaten** einen **API-Schlüssel** erstellen.
4. Im Overlay: Zahnrad → Reiter **Google Chirp 3 HD** → Key einfügen. Die deutschen
   Chirp-3-HD-Stimmen werden dann automatisch geladen.

> **Wichtig:** Es muss ein **Google-Cloud-Key mit aktivierter Text-to-Speech-API**
> sein. Ein Gemini-API-Key funktioniert hier **nicht**. Der Key wird nur **lokal**
> gespeichert (`chrome.storage.local`, kein Geräte-Sync).
>
> Kostenrahmen (Google): rund 1 Mio. Zeichen/Monat kostenlos, danach kostenpflichtig.

---

## Engine-Vergleich

| Merkmal | Edge TTS | Google Chirp 3 HD |
|---|---|---|
| API-Key nötig? | **Nein** | **Ja** (Google-Cloud-Key) |
| Kosten | kostenlos | ~1 Mio. Zeichen/Monat frei, danach kostenpflichtig |
| Stimmenqualität | sehr gut (Neural) | exzellent (Chirp 3 HD) |
| Deutsche Stimmen | viele (de-DE + mehrsprachig) | 30 (14 weiblich, 16 männlich) |
| Stabilität | gut, gelegentlich Token-/Verbindungsaussetzer | sehr stabil |
| Einrichtung | sofort nutzbar | Key + API-Aktivierung nötig |
| Tempo-Regler | ja (`prosody rate`) | ja (`speakingRate`) |

**Empfehlung:** Mit **Edge** starten (kein Key). Wer die beste Qualität will und
einen Google-Cloud-Key hat, schaltet auf **Google Chirp 3 HD** um.

---

## Aufbau (Architektur in Kürze)

Die Drei-Teilung ist der Kern, damit das Tool auch auf Seiten mit strenger
Content-Security-Policy (GitHub, Google, Nachrichtenportale) funktioniert:

| Teil | Aufgabe |
|------|---------|
| `content/overlay.js` | Schwebendes Overlay im **Shadow DOM**, erfasst die Markierung, sendet Befehle. **Keine** Netz-/Audio-Aufrufe (würden an der CSP der Seite scheitern). |
| `background/service-worker.js` | **Alle** Netzwerk-Aufrufe — läuft im Ursprung der Erweiterung und umgeht so die CSP der Seite. Steuert Synthese + Wiedergabe. |
| `offscreen/offscreen.js` | Spielt das fertige Audio ab (ein Service-Worker kann das in MV3 nicht selbst) — mit nahtloser Wiedergabe-Queue. |
| `engines/edge-tts.js` | Edge-TTS-Logik (WebSocket + Sec-MS-GEC-Token, siehe unten). |
| `engines/google-tts.js` | Google-Chirp-3-HD-Logik (REST + dynamische Stimmenliste). |
| `engines/chunker.js` | Teilt langen Text an Satzgrenzen, damit Dienste-Limits eingehalten werden. |
| `settings/settings.js` | Lädt/speichert alle Einstellungen in `chrome.storage.local`. |

**Datenfluss beim Vorlesen:**
Markierung (Content-Script) → Service-Worker (Synthese Edge/Google) → Audio-Bytes →
Offscreen-Dokument (Wiedergabe) → Status zurück ans Overlay.

---

## Wichtige technische Entscheidungen (für spätere Reparaturen)

### Edge: das Sec-MS-GEC-Token
Edge TTS ist kostenlos, verlangt seit 2024 aber ein Sicherheits-Token in der
WebSocket-URL. `engines/edge-tts.js` erzeugt es so (komplett gekapselt, damit
es leicht zu reparieren ist, falls Microsoft das Verfahren ändert):

1. `ticks = aktuelle_Unixzeit_in_Sekunden + 11644473600` (Differenz 1601↔1970)
2. auf die nächsten 300 Sekunden abrunden: `ticks -= ticks % 300`
3. in 100-Nanosekunden-Einheiten: `ticks *= 10_000_000`
4. `SHA-256(String(ticks) + TRUSTED_CLIENT_TOKEN)` als **Hex in Großbuchstaben**

Gerechnet wird mit `BigInt` (die Zahl ist zu groß für `Number`). Der
`TRUSTED_CLIENT_TOKEN` ist **kein Geheimnis** — er ist fest in Microsoft Edge
eingebaut und in jeder Edge-TTS-Implementierung identisch.

> **Browser-Eigenheit:** Ein WebSocket im Browser erlaubt **keine** eigenen Header
> (User-Agent/Origin/Cookie). Anders als der OkHttp-Code in „Best Journal" tragen
> hier allein die URL-Query-Parameter (vor allem das Token). Schlägt Edge fehl,
> versucht der Worker es **einmal automatisch** mit frischem Token erneut.
>
> **Wichtig (`host_permissions`):** Für die WebSocket-Verbindung muss
> `wss://speech.platform.bing.com/*` **eigens** in `host_permissions` stehen — das
> `https://`-Gegenstück reicht **nicht**. Fehlt der `wss://`-Eintrag, blockiert
> Chrome den Verbindungsaufbau und das Overlay meldet „Edge-Verbindung
> fehlgeschlagen". (Der Edge-Endpunkt selbst akzeptiert den
> `chrome-extension://…`-Origin jeder Erweiterung — ein gefälschter Origin ist also
> nicht nötig.)

### Stimmen-Logik (aus „Best Journal" übernommen)
- **Google:** Stimmen werden primär **dynamisch** über `/v1/voices?languageCode=de-DE`
  geladen und auf `chirp3-hd` gefiltert. Fällt das aus, greift die feste 30er-Liste
  aus Best Journal (Default `de-DE-Chirp3-HD-Kore`). Synthese mit **reinem Text**
  (kein SSML), MP3, `speakingRate`, **kein pitch** (Chirp ignoriert es).
- **Edge:** Stimmen dynamisch vom `voices/list`-Endpunkt (de-DE + mehrsprachige),
  mit 6 bewährten Stimmen als Fallback.

### Audio-Transport
Ein Service-Worker hat kein `URL.createObjectURL`. Die MP3-Bytes werden darum als
**Data-URL** (Base64) an das Offscreen-Dokument geschickt und dort abgespielt.

---

## Status

- ✅ **Plattformen** — verifiziert in Chrome unter **Windows** und **macOS**
  (gleiche Codebasis, kein plattformspezifischer Code)
- ✅ **Stufe 1** — Grundgerüst: Overlay, Verschieben, Markierung erfassen
- ✅ **Stufe 2** — Edge TTS (Token, WebSocket-Synthese, Wiedergabe, Panel)
- ✅ **Stufe 3** — Google Chirp 3 HD (dynamische Stimmen + Fallback, Synthese)
- ✅ **Stufe 4** — Feinschliff: langer Text (Satzgrenzen-Chunking + nahtlose Queue),
  deutsche Fehlermeldungen, automatischer Edge-Token-Neuversuch, Tastaturkürzel

# Vorlese-Overlay (Chrome-Erweiterung)

Markierten Text auf **jeder** Webseite vorlesen lassen — über ein schwebendes
Overlay (Lautsprecher + Zahnrad). Zwei Vorlese-Dienste zur Auswahl:

1. **Edge TTS** — kostenlos, kein Schlüssel nötig
2. **Google Chirp 3 HD** — über einen eigenen Google-Cloud-API-Key

Reines Vanilla-JavaScript, **kein Build-Schritt**. Manifest V3, getestet für
Chrome auf macOS.

---

## Installation („Entpackte Erweiterung laden")

1. Chrome öffnen und in der Adresszeile `chrome://extensions` aufrufen.
2. Oben rechts den **Entwicklermodus** einschalten.
3. Auf **„Entpackte Erweiterung laden"** klicken.
4. Den Ordner `vorlese-overlay/` auswählen.
5. Fertig — auf jeder Webseite erscheint unten rechts das Overlay.

> Nach Code-Änderungen in `chrome://extensions` bei der Erweiterung auf den
> **Neu-laden-Pfeil** klicken.

---

## Bedienung

- **Text markieren** → auf den **Lautsprecher** klicken → der Text wird vorgelesen.
- Während des Vorlesens zeigt der Button ein **Stopp-Symbol**; erneuter Klick stoppt.
- Das **Zahnrad** öffnet die Einstellungen (Engine-Wahl, Stimme, Tempo, API-Key).
- Das Overlay lässt sich per **Ziehen** verschieben; die Position wird gemerkt.

---

## Aufbau (Architektur in Kürze)

| Teil | Aufgabe |
|------|---------|
| `content/overlay.js` | Schwebendes Overlay im Shadow DOM, erfasst die Markierung, sendet Befehle |
| `background/service-worker.js` | Alle Netzwerk-Aufrufe (umgeht die CSP der Seite), steuert die Wiedergabe |
| `offscreen/offscreen.js` | Spielt das fertige Audio ab (Worker kann das in MV3 nicht selbst) |
| `engines/edge-tts.js` | Edge-TTS-Logik (WebSocket + Sec-MS-GEC-Token) |
| `engines/google-tts.js` | Google-Chirp-3-HD-Logik (REST + dynamische Stimmenliste) |
| `settings/settings.js` | Lädt/speichert alle Einstellungen in `chrome.storage.local` |

Der Datenfluss beim Vorlesen:
**Markierung (Content-Script) → Service-Worker (Synthese) → Offscreen (Wiedergabe) → Status zurück ans Overlay.**

---

## Status

Dieses Projekt wird stufenweise gebaut (Details und der Engine-Vergleich folgen
in der finalen Version):

- ✅ **Stufe 1** — Grundgerüst: Overlay, Verschieben, Markierung erfassen
- ⬜ **Stufe 2** — Edge TTS
- ⬜ **Stufe 3** — Google Chirp 3 HD
- ⬜ **Stufe 4** — Feinschliff (langer Text, Fehlerbehandlung, Tastaturkürzel)

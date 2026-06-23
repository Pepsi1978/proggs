# Handoff: Cortex — privates Web-Cockpit für ein „zweites Gehirn"

> Diese Anleitung ist für **Claude Code** (CLI) gedacht. Sie beschreibt, was zu tun ist,
> damit genau diese Oberfläche in den echten Projekt-Kontext (privater FastAPI-Server)
> übernommen und mit den echten Daten verbunden wird.

---

## Überblick

`index.html` ist das fertige, eigenständige Web-Cockpit „Cortex" — ein privates Werkzeug,
um in ein persönliches „zweites Gehirn" (Vektordatenbank, ~177 Einträge) hineinzuschauen und
zu stöbern. Es läuft hinter einem VPN, hat **einen** Nutzer (den Besitzer), **kein Login**,
**kein Marketing**, **keine Mehrbenutzer-Logik**. Sprache der Oberfläche: **Deutsch**.

## Wichtig: Was diese Datei ist

`index.html` ist **kein bloßer Design-Entwurf, sondern bereits die fertige Implementierung**:
reines Vanilla HTML + CSS + JavaScript in **einer einzigen Datei**, **ohne Build-Step, ohne
npm, ohne Framework**. Sie ist als **Drop-in** gedacht und kann die bestehende `index.html`
direkt ersetzen. Das Backend (Python/FastAPI) liefert sie unverändert als statische Datei aus.

**Aufgabe für Claude Code ist daher NICHT, sie in React/Vue o.ä. umzuschreiben.** Die einzige
Integrationsarbeit ist: sicherstellen, dass die in „API-Verträge" genannten Endpunkte vom
FastAPI-Server bereitgestellt werden und korrekt antworten. Sobald sie antworten, zeigt das
Cockpit automatisch echte Daten statt des eingebauten Demo-Datensatzes.

## Fidelity

**High-Fidelity (hifi)** — finale Farben, Typografie, Abstände, Animationen und Interaktionen
sind fertig umgesetzt. Pixelgenau übernehmen; nichts neu erfinden.

---

## Harte Rahmenbedingungen (nicht verhandelbar)

- **Eine einzige `index.html`.** Kein Build, kein Bundler, kein npm. Alles inline:
  `<style>` im `<head>`, `<script>` am Ende.
- **Keine externen JS-Bibliotheken.** Kein React/Vue/Svelte, kein Tailwind-Build, kein jQuery.
  Erlaubt sind nur: **Google Fonts per `<link>`** und **inline-SVG-Icons**.
- **Dark + Light Mode**, umschaltbar oben rechts, Auswahl in `localStorage`
  (Schlüssel `cortex-theme`), umgesetzt über CSS-Variablen + `data-theme="dark|light"` am `<html>`.
- **Responsive**, Desktop-first. Unter ~820px wird die linke Navigationsschiene zu einer
  fixierten **unteren Tab-Leiste**.
- Daten **ausschließlich** über die unten genannten JSON-Endpunkte (per `fetch`). Keine
  erfundenen Endpunkte. Wenn ein Endpunkt nicht erreichbar ist: sauber degradieren — die Seite
  fällt auf einen eingebauten Demo-Datensatz zurück und die Verbindungs-Pille zeigt
  „Demo-Daten" (statt grün „verbunden").

---

## Seitenstruktur

Dreispaltig auf dem Desktop:

1. **Linke Navigationsschiene:** Marke (🧠-Glyph + „Cortex" / „zweites Gehirn"), drei
   Navigationspunkte (Übersicht, Gehirn, Einstellungen) mit aktivem Marker, unten
   monospace-Fußzeile `10.8.0.1 · privat`.
2. **Topbar:** Seitentitel + Untertitel (wechselt je Tab), Verbindungs-Pille mit Status-Punkt
   (grün „verbunden" / rot „getrennt" bzw. „Demo-Daten"), Theme-Umschalter (Sonne/Mond).
3. **Inhalt:** je nach Tab eine von drei Ansichten.

Titel/Untertitel je Tab:

| Tab | Titel | Untertitel |
|-----|-------|-----------|
| Übersicht | „Übersicht" | „Dein Gehirn auf einen Blick" |
| Gehirn | „Gehirn" | „Durchsuche und stöbere deine Einträge" |
| Einstellungen | „Einstellungen" | „Agent & Server konfigurieren" |

---

## Ansichten im Detail

### Übersicht
- **Gedächtnis-Spektrum (Hauptkarte):** Eyebrow „GEDÄCHTNIS-SPEKTRUM", sehr große Zahl
  (Gesamtanzahl, zählt beim Laden animiert hoch) mit Unterzeile „EINTRÄGE GESAMT", daneben
  Vektor-Index-Meta (Punkte, Embed-Modell). Darunter der **glänzende, gestapelte Farbbalken**:
  ein Segment je Kategorie, Breite proportional zur Anzahl; Hover hebt das Segment leicht an und
  zeigt einen Tooltip „Kategorie · Anzahl". Darunter eine **Legende** (Swatch + Name + Anzahl).
  **Klick auf Segment ODER Legendenzeile → Wechsel zum Tab „Gehirn", gefiltert auf diese
  Kategorie** (Verknüpfung V1).
- **Vier Vital-Karten:** Bibliothekar-Agent (Status-Punkt + „Bereit"/„offline", darunter Modell
  · Sitzungen), Prozessor (CPU % + Meter), Arbeitsspeicher (% + „benutzt/gesamt" + Meter),
  Speicherplatz (% + „benutzt/gesamt" + Meter). Meter animieren weich auf neue Werte.
- **Live-Aktualisierung:** Übersicht pollt `/api/overview` **alle 20 Sekunden** (V5).

### Gehirn
- **Suchleiste** (Lupe), Platzhalter „Im Gehirn suchen — z.B. ‚meine Ziele', ‚Kaffeemaschine'…".
  Tippen löst **verzögerte Live-Suche** aus (Debounce ~300 ms). Semantische Vektorsuche →
  Treffer haben einen **Relevanz-Score in %**.
- **Kategorie-Chips:** „Alle" + ein Chip je Kategorie (in Kategoriefarbe, wenn aktiv). Genau
  einer aktiv. Klick filtert die Liste.
- **Ergebnisliste:** Karten mit farbigem Akzentstreifen, Titel, farbigem Kategorie-Tag,
  2-zeiligem Snippet; bei Suche rechts der Score in %. Hover hebt die Karte an.
- **Klick auf Karte → Detail-Drawer** von rechts (V2): Kategorie-Tag, voller Titel, Datum,
  kompletter Text in einer monospace-Lesebox. Schließen per X, Hintergrundklick oder **Escape**.
- Zustände: „lädt…", „Nichts gefunden.", „Fehler beim Laden."

### Einstellungen
Echter Settings-Screen, Felder vorerst **Attrappe** (Speichern noch ohne Funktion — bewusst):
- **Modell-Auswahl-Dropdown** für den Bibliothekar-Agenten (verschiedene Gemini-Modelle).
- **Großer System-Prompt-Editor** (monospace Textarea) mit „Zurücksetzen"/„Speichern".
- **Logbuch-Viewer** (Liste der Gesprächs-Logbücher; lädt optional `/api/logbook`).

---

## Verknüpfungen / Interaktionslogik

- **V1** Legende/Segment → Tab „Gehirn", gefiltert auf die Kategorie (Chip aktiv).
- **V2** Eintrag → rechter Detail-Drawer mit Volltext; schließbar X / Hintergrund / Escape.
- **V3** Theme-Umschalter Dark/Light, persistent in `localStorage` (`cortex-theme`).
- **V4** Verbindungs-Pille: grün sobald `/api/overview` antwortet, sonst rot.
- **V5** Übersicht pollt `/api/overview` alle 20 s; Balken/Meter animieren weich.
- **V6** Eine Kategorie hat überall (Spektrum, Legende, Chips, Tags) **dieselbe** Farbe.
- **V7** < 820px: linke Schiene → fixierte untere Tab-Leiste (nur Icons), Marke/Fußzeile aus.
- Tab-Wechsel dezent animiert; `prefers-reduced-motion` wird respektiert.

---

## API-Verträge (EXAKT einhalten — keine neuen Endpunkte/Felder erfinden)

**`GET /api/overview`**
```jsonc
{
  "total": 177,
  "brain":  { "status": "ok", "points": 177, "version": "1.1.0", "embed_model": "…" },
  "agent":  { "status": "ok", "version": "0.1.3", "model": "gemini-3.1-flash-lite", "sessions": 0 },
  "server": { "cpu_pct": 2.0, "mem_used": 1033, "mem_total": 8200, "mem_pct": 12.0,
              "disk_used": 5600, "disk_total": 96000, "disk_pct": 6.0 },   // mem/disk in MB
  "categories": [ { "name": "persoenlich", "count": 29 }, … ]              // absteigend sortiert
}
```
Felder können fehlen/`null` sein (z.B. `agent.status:"offline"`) — robust behandeln. Speicher-
Werte (`mem_used` etc.) werden als **MB** erwartet und im Frontend zu MB/GB formatiert.

**`GET /api/entries?q=<text>&category=<name>&limit=<n>`**
```jsonc
{ "mode": "list|category|search",
  "items": [ { "title": "…", "category": "fitness", "text": "…",
               "match": "…",        // bei Suche: passende Textstelle (sonst text)
               "score": 0.83,       // nur bei Suche (0–1) → als % angezeigt
               "updated_at": "…" } ] }
```
Regeln: `category` gesetzt → Modus „category"; sonst `q` gesetzt → „search"; sonst „list".

**`GET /api/entry?title=<titel>`** → `{ "title", "category", "text" (Volltext), "updated_at" }`

**`GET /api/logbook`** → `{ "items": [ … ] }`  (für den Logbuch-Viewer)

**`GET /api/health`** → `{ "status": "ok", "version": "0.1.0" }`

---

## Design-Tokens

**Schriften:** Inter (UI/Text), Space Grotesk (Überschriften & große Zahlen),
JetBrains Mono (Labels, Eyebrows, Code/Volltext) — per Google-Fonts-`<link>`.

**Dunkel:** bg `#0B090F`, Flächen `#16121E`, erhöht `#1C1726`, Rahmen `#2A2436`,
Text `#F2ECEA`, gedämpft `#A89CAE`, blass `#6E6479`. Akzente: Amber `#F4A65A` (Signatur),
Rose `#F2698E`, Iris `#C99BF5`. Verbunden `#5AD0A0`, getrennt `#F2698E`.

**Hell:** bg `#F3EEE7`, Flächen `#FFFFFF`, erhöht `#FBF7F1`, Rahmen `#E6DCCF`,
Text `#241C16`, gedämpft `#7A6E63`. Akzente kräftiger: `#D9842F`, `#E0517A`, `#8A5BD6`.

**Kategorie-Palette (13 zyklische Farben, konsistent je Kategoriename):**
`#F4A65A #F2698E #C99BF5 #5AD0A0 #5AB0F2 #F2B65A #9BD05A #F08A5A #3FD0D6 #B68CF5 #F25AC0 #5A7BF2 #D0C45A`
Zuweisung: Kategorien (absteigend nach Anzahl sortiert) bekommen der Reihe nach Farben aus der
Palette (zyklisch). Diese Zuordnung gilt überall identisch (Spektrum, Legende, Chips, Tags).

**Form:** Radius ~18px (klein 12, groß 26), 1px-Rahmen, weiche tiefe Schatten, dezenter Glow.

**Hintergrund (kosmisch, rein CSS/SVG, keine echten Bilder):** radialer Amber/Iris/Rose-Glow,
generiertes Sternenfeld (~130 Punkte, nur im Dark-Mode), feines SVG-Rauschen als Overlay.

---

## Assets
Keine externen Bilddateien. Alle Icons sind inline-SVG; alle „Bilder/Hintergründe" sind rein
per CSS/SVG erzeugt. Einzige externe Ressource: Google Fonts.

## Demo-Daten
Im `<script>` steckt ein vollständiger Demo-Datensatz (13 Kategorien, 30 Beispiel-Einträge,
Summe 177), der nur greift, wenn die echten Endpunkte nicht antworten. Er dient zum Ansehen
des Designs vor dem Deploy und kann nach erfolgreicher Anbindung bleiben (er stört nicht) oder
entfernt werden.

## Dateien
- `index.html` — die komplette, eigenständige Implementierung (HTML + CSS + JS in einer Datei).

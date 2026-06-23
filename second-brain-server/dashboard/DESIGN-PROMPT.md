# Design-Auftrag: „Cortex" — Web-Cockpit für ein zweites Gehirn

> Dieser Prompt beschreibt eine bestehende Single-Page-Weboberfläche vollständig, damit ein
> Design-/Coding-Agent sie optisch neu gestalten kann, ohne den Originalcode zu sehen. Alle
> Funktionen, Datenflüsse, Verknüpfungen und technischen Grenzen sind hier aufgeführt.

---

## 0. Deine Rolle

Du bist Design-Lead und Frontend-Umsetzer in einer Person. Deine Aufgabe ist es, die unten
beschriebene Oberfläche **„Cortex"** optisch auf ein neues, hochwertiges Niveau zu heben — so,
dass sie aussieht und sich anfühlt wie ein gekauftes Premium-Produkt. **Alle Funktionen und
Datenflüsse müssen 1:1 erhalten bleiben.** Du gestaltest das Erscheinungsbild neu (Layout,
Farben, Typografie, Komponenten, Abstände, Mikro-Interaktionen), nicht die Funktionalität.

Liefere am Ende **eine einzige, vollständige `index.html`**, die die bestehende Datei direkt
ersetzen kann (Drop-in).

---

## 1. Das Produkt in einem Absatz

„Cortex" ist das private Web-Cockpit für ein **„zweites Gehirn"** — einen persönlichen
Wissens-/Erinnerungsspeicher (eine Vektordatenbank mit derzeit ~177 Einträgen, jeder Eintrag
hat Titel, Kategorie, Text und Datum). Es nutzt **genau eine Person** (der Besitzer). Die Seite
läuft auf einem privaten Server und ist **nur über ein VPN erreichbar, nicht öffentlich** — es
gibt also keine Login-Seite, kein Marketing, keine Mehrbenutzer-Logik. Es ist ein ruhiges,
edles **Cockpit zum Reinschauen und Stöbern**: „Wie sieht mein Gehirn gerade aus, was steckt
drin, läuft der Server?" Sprache: **Deutsch**. Primär Desktop, aber responsive bis Handy.

---

## 2. Harte technische Rahmenbedingungen (NICHT verhandelbar)

- **Eine einzige `index.html`.** Kein Build-Step, kein Bundler, kein npm. Alles inline:
  `<style>` im `<head>`, `<script>` am Ende. Die Datei wird von einem Python/FastAPI-Backend
  unverändert als statische Datei ausgeliefert.
- **Kein Framework, keine externen JS-Bibliotheken.** Kein React/Vue/Svelte, kein
  Tailwind-Build, kein jQuery. Reines Vanilla HTML + CSS + JavaScript. (Google Fonts per
  `<link>` und inline-SVG-Icons sind erlaubt — sonst keine externen Abhängigkeiten.)
- **Dark + Light Mode** sind Pflicht, umschaltbar per Knopf oben rechts, Auswahl wird in
  `localStorage` gemerkt (Schlüssel z.B. `cortex-theme`). Realisierung über CSS-Variablen und
  ein `data-theme="dark|light"`-Attribut am `<html>`.
- **Responsive.** Desktop-first. Unter ~820px Breite klappt die linke Navigationsschiene zu
  einer **unteren Tab-Leiste** um.
- Datenanbindung ausschließlich über die in **Abschnitt 7** genannten JSON-Endpunkte (per
  `fetch`). Keine erfundenen Endpunkte. Wenn ein Endpunkt nicht erreichbar ist, sauber
  degradieren (Platzhalter „–", Verbindungspunkt auf „getrennt").

---

## 3. Look & Feel / Design-Ziel

- **Stimmung:** ruhig, fokussiert, hochwertig, „Cockpit"/„Kontrollraum" eines Gehirns. Edel,
  nicht verspielt. Es darf sich besonders anfühlen — es ist ein persönliches, fast intimes
  Werkzeug, kein Standard-Admin-Dashboard.
- **Hierarchie:** Die große Kennzahl (Anzahl Einträge) und das „Gedächtnis-Spektrum" sind der
  Star der Startseite. Alles andere ordnet sich unter.
- **Signatur-Element:** das **Gedächtnis-Spektrum** — eine farbige, gestapelte Balkenleiste, in
  der jede Wissens-Kategorie als farbiges Segment proportional zu ihrer Größe erscheint. Das ist
  das visuelle Markenzeichen der App und soll sehr schön sein.
- Premium-Details: weiche Schatten, abgestimmte Radien, dezente Verläufe/Glow, ruhige
  Mikro-Animationen (Tab-Wechsel, Drawer, Balken-/Meter-Übergänge), klare Leerräume. Achte auf
  exzellente Typografie und konsistente Abstände.
- Beide Themes müssen **gleich gut** aussehen (nicht nur Dark mit invertierten Farben).

---

## 4. Seitenstruktur (Informationsarchitektur)

Dreispaltiges Grundgerüst auf dem Desktop:

1. **Linke Navigationsschiene (Rail), schmal, fixiert:**
   - Oben die Marke: ein **🧠-Glyph** in einem farbigen, abgerundeten Kästchen + Wortmarke
     **„Cortex"** mit Unterzeile **„zweites Gehirn"**.
   - Drei Navigationspunkte mit Icon + Label: **Übersicht**, **Gehirn**, **Einstellungen**.
     Der aktive Punkt ist klar hervorgehoben (z.B. farbiger Marker links + gefüllter Hintergrund).
   - Ganz unten klein/monospace eine technische Fußzeile, z.B. `10.8.0.1 · privat`
     (signalisiert „privater Server").
2. **Topbar (Kopfzeile rechts oben):**
   - Links der **Seitentitel + Untertitel**, der zum aktiven Tab wechselt (siehe Tabellen unten).
   - Rechts eine **Verbindungs-Pille** mit farbigem Status-Punkt: grün „verbunden" / rot
     „getrennt" (zeigt, ob der Server gerade antwortet).
   - Ganz rechts der **Theme-Umschalter** (Sonne/Mond-Icon).
3. **Inhaltsbereich:** zeigt je nach aktivem Tab eine von drei Ansichten (siehe Abschnitt 5).

Titel/Untertitel je Tab:

| Tab | Titel | Untertitel |
|-----|-------|-----------|
| Übersicht | „Übersicht" | „Dein Gehirn auf einen Blick" |
| Gehirn | „Gehirn" | „Durchsuche und stöbere deine Einträge" |
| Einstellungen | „Einstellungen" | „Agent & Server konfigurieren" |

---

## 5. Tab für Tab — jede Funktion im Detail

### 5.1 Tab „Übersicht"

Zweck: der schnelle Gesamtblick. Lädt seine Daten aus `GET /api/overview` und **aktualisiert
sich automatisch alle 20 Sekunden**.

**(a) Gedächtnis-Spektrum (Hauptkarte oben):**
- Eine kleine Eyebrow-Zeile „GEDÄCHTNIS-SPEKTRUM" (monospace, gesperrt, Großbuchstaben).
- Eine **sehr große Zahl** = Gesamtzahl der Einträge (z.B. `177`) mit Unterzeile
  „EINTRÄGE GESAMT".
- Daneben die **gestapelte Farbleiste**: pro Kategorie ein Segment, dessen Breite proportional
  zur Anzahl der Einträge dieser Kategorie ist. Beim **Hover** über ein Segment hebt es sich
  leicht hervor und zeigt einen Tooltip „Kategoriename · Anzahl".
- Darunter eine **Legende** als Raster: je Kategorie ein farbiges Quadrat (Swatch) + Name +
  rechtsbündig die Anzahl (monospace). **Klick auf eine Legendenzeile** → wechselt in den Tab
  „Gehirn" und filtert dort sofort auf genau diese Kategorie (siehe Verknüpfung V1).
- Es gibt eine **feste Farbpalette** für Kategorien (~13 Farben), die zyklisch den Kategorien
  zugewiesen wird, sodass jede Kategorie über Spektrum, Legende, Chips und Tags **dieselbe
  Farbe** trägt. Beispielkategorien (Name → ungefähre Anzahl): persoenlich 29, ki-arbeitsweise
  27, theorie 25, ziele-2026 22, fitness 20, inspiration 17, geraete 10, leitsaetze 7,
  arbeitsregeln 6, nem-stack 5, gesundheit 5, fahrzeug-strom 2, drohnen 2. (Die echten Werte
  kommen live aus der API — nicht hart codieren.)

**(b) Vier Vital-Karten (Raster darunter):**
1. **Bibliothekar-Agent** — Status-Punkt + Wort („Bereit" / „offline"), darunter klein das
   verwendete KI-Modell und die Zahl aktiver Sitzungen (z.B. „gemini-3.1-flash-lite · 0
   Sitzungen").
2. **Prozessor** — CPU-Auslastung in Prozent + ein **Fortschrittsbalken (Meter)**.
3. **Arbeitsspeicher** — Prozent + klein „benutzt / gesamt" (z.B. „985 MB / 7.8 GB") + Meter.
4. **Speicherplatz** — Prozent + „benutzt / gesamt" + Meter.

Hinweis: Aktuell wirkt der Bereich unter den Vital-Karten leer — du darfst die Übersicht
großzügiger / interessanter komponieren (z.B. die Spektrum-Karte größer inszenieren, die
Vitals eleganter, evtl. eine zusätzliche dezente Info), solange keine Funktion verloren geht.

### 5.2 Tab „Gehirn"

Zweck: alle Einträge durchsuchen und stöbern. Daten aus `GET /api/entries` (drei Modi, siehe
Abschnitt 7), Detailansicht aus `GET /api/entry`.

- **Suchleiste** oben (mit Lupen-Icon), Platzhalter z.B. „Im Gehirn suchen — z.B. ‚meine Ziele',
  ‚Kaffeemaschine'…". Tippen löst eine **verzögerte Live-Suche** aus (Debounce ~300ms). Die
  Suche ist semantisch (Vektorsuche) — Treffer können einen **Relevanz-Score in %** haben.
- **Kategorie-Chips** darunter: ein Chip „Alle" + ein Chip pro Kategorie (in Kategoriefarbe,
  wenn aktiv). Klick filtert die Liste auf diese Kategorie. Genau ein Chip ist aktiv.
- **Ergebnisliste**: vertikale Liste von Karten. Jede Karte zeigt: **Titel** (prominent),
  ein **farbiges Kategorie-Tag**, ein **2-zeiliges Text-Snippet** (abgeschnitten), und bei
  Suchergebnissen rechts den **Score in %**. Hover hebt die Karte leicht an.
- **Klick auf eine Eintragskarte** → öffnet einen **Detail-Drawer**, der von rechts
  hereingleitet (siehe Verknüpfung V2): zeigt Kategorie-Tag, vollen Titel, Datum und den
  **kompletten Text** (in einer monospace-Lesebox). Schließbar per X-Button, Klick auf den
  abgedunkelten Hintergrund, oder **Escape-Taste**.
- Leerzustände: „lädt…", „Nichts gefunden.", „Fehler beim Laden." sauber gestalten.

Anmerkung zur Verbesserung: Die Liste ist aktuell sehr gleichförmig (viele identische Zeilen).
Du darfst sie lebendiger und besser scanbar machen (z.B. dezente Gruppierung, mehr Kontext pro
Karte, schönere Tags) — ohne neue Datenfelder zu erfinden.

### 5.3 Tab „Einstellungen"

- **Aktueller Stand:** nur ein Platzhalter mit dem Text „Einstellungen & Prompt-Editor" und der
  Erklärung, dass hier als Nächstes die Modell-Wahl und ein Editor für den System-Prompt des
  Agenten kommen.
- **Geplant (bitte das Layout schon vorsehen, auch wenn noch ohne Funktion):**
  - Ein **Modell-Auswahl-Dropdown** für den Bibliothekar-Agenten (z.B. verschiedene
    Gemini-Modelle).
  - Ein **großer System-Prompt-Editor** (mehrzeilige Textarea, monospace) zum Ansehen,
    Bearbeiten und Speichern der Agenten-Instruktionen, mit Speichern-Button.
  - Optional ein **Logbuch-Viewer** (Liste der Gesprächs-Logbücher des Agenten).
  Gestalte diesen Tab als überzeugenden „Settings"-Screen (Sektionen, klare Formfelder), auch
  wenn die Felder vorerst nur Attrappe sind.

---

## 6. Globale Elemente & Verknüpfungen (Interaktions-Logik)

- **V1 — Legende → Gehirn:** Klick auf eine Kategorie in der Übersichts-Legende wechselt zum
  Tab „Gehirn" und filtert dort die Liste auf diese Kategorie (Chip entsprechend aktiv).
- **V2 — Eintrag → Drawer:** Klick auf eine Eintragskarte öffnet den rechten Detail-Drawer mit
  dem Volltext; schließbar per X / Hintergrundklick / Escape.
- **V3 — Theme-Umschalter:** Sonne/Mond oben rechts schaltet Dark/Light, persistent in
  `localStorage`.
- **V4 — Verbindungs-Status:** Der Punkt in der Topbar-Pille zeigt grün „verbunden", sobald
  `/api/overview` antwortet, sonst rot „getrennt".
- **V5 — Live-Aktualisierung:** Die Übersicht pollt `/api/overview` alle 20 Sekunden; Balken und
  Meter animieren weich auf neue Werte.
- **V6 — Konsistente Kategoriefarben:** Eine Kategorie hat überall (Spektrum, Legende, Chips,
  Tags) dieselbe Farbe.
- **V7 — Responsive < 820px:** Die linke Rail wird zu einer fixierten **unteren Tab-Leiste**
  (nur Icons, Labels ausgeblendet), Marke und Fußzeile verschwinden; Inhaltsabstände
  reduzieren sich; die große Kennzahl wird etwas kleiner.
- **Tab-Wechsel** animiert dezent (z.B. leichtes Aufsteigen/Einblenden des Inhalts);
  `prefers-reduced-motion` respektieren.

---

## 7. Daten-Verträge (API) — EXAKT einhalten

Das Backend stellt diese JSON-Endpunkte bereit. Konsumiere genau diese Felder; erfinde keine
neuen Endpunkte. (Du darfst die HTML/JS-Struktur frei neu schreiben, solange diese Verträge
korrekt angebunden bleiben.)

**`GET /api/overview`** → 
```jsonc
{
  "total": 177,
  "brain":  { "status": "ok", "points": 177, "version": "1.1.0", "embed_model": "…" },
  "agent":  { "status": "ok", "version": "0.1.3", "model": "gemini-3.1-flash-lite", "sessions": 0 },
  "server": { "cpu_pct": 2.0, "mem_used": 1033, "mem_total": 8200, "mem_pct": 12.0,
              "disk_used": 5600, "disk_total": 96000, "disk_pct": 6.0 },   // Bytes
  "categories": [ { "name": "persoenlich", "count": 29 }, … ]              // absteigend sortiert
}
```
Felder können fehlen/`null` sein (z.B. `agent.status:"offline"`) — robust behandeln.

**`GET /api/entries?q=<text>&category=<name>&limit=<n>`** → 
```jsonc
{ "mode": "list|category|search",
  "items": [ { "title": "…", "category": "fitness", "text": "…",
               "match": "…",        // bei Suche: passende Textstelle (sonst text nutzen)
               "score": 0.83,       // nur bei Suche (0–1) → als % anzeigen
               "updated_at": "…" } ] }
```
Regeln: ist `category` gesetzt → Modus „category"; sonst ist `q` gesetzt → semantische „search";
sonst „list" (neueste/erste). Snippet aus `match` oder `text`.

**`GET /api/entry?title=<titel>`** → `{ "title": "…", "category": "…", "text": "<voller Text>", "updated_at": "…" }`

**`GET /api/logbook`** → `{ "items": [ … ] }`  (für den optionalen Logbuch-Viewer)

**`GET /api/health`** → `{ "status": "ok", "version": "0.1.0" }`

---

## 8. Aktuelles Design-System (Ist-Zustand — als Referenz, du darfst es ersetzen/verbessern)

- **Schriften:** Inter (Fließtext/UI), Space Grotesk (Überschriften & große Zahlen),
  JetBrains Mono (Labels, Eyebrows, Code/Volltext). Du darfst die Schriftwahl ändern, wenn es
  das Premium-Gefühl hebt — bleib bei gut lesbaren, hochwertigen Google-Fonts.
- **Dark-Palette (Beispiel):** Hintergrund `#0B0E14`, Flächen `#141A23`, Rahmen `#222C3A`,
  Text `#E8EEF6`, gedämpfter Text `#8A98AC`; Akzente: Iris `#7B7BF5`, Mint `#4FD1B0`,
  Amber `#F2B65A`, Rose `#F2698E`. Dezenter radialer Glow oben rechts.
- **Light-Palette (Beispiel):** Hintergrund `#F4F6FB`, Flächen `#FFFFFF`, Rahmen `#E4E8F1`,
  Text `#1A2230`; gleiche Akzente etwas kräftiger.
- **Form:** Radius ~16px, weiche, tiefe Schatten, Karten mit 1px-Rahmen.
- **Kategorie-Palette (13 zyklische Farben):** `#7B7BF5 #4FD1B0 #F2B65A #F2698E #5AB0F2 #B68CF5
  #3FD0D6 #9BD05A #F08A5A #5AD0A0 #F25AC0 #5A7BF2 #D0C45A`.

Du darfst dieses System verfeinern oder durch ein stimmigeres ersetzen — Hauptsache, beide
Themes wirken durchdacht und edel und die Kategoriefarben bleiben konsistent.

---

## 9. Was am aktuellen Stand verbessert werden soll (Verbesserungsrichtungen)

Der aktuelle Entwurf ist solide, aber noch nicht „zu 100% schön". Mögliche Ansatzpunkte (du
darfst eigene Schwerpunkte setzen):
- Die **Startseite** wirkt unten leer — die Komposition großzügiger und spannender machen, das
  Gedächtnis-Spektrum noch edler inszenieren, die Vital-Karten hochwertiger.
- Die **Gehirn-Liste** ist sehr monoton — besser scanbar, lebendiger, schönere Tags/Karten.
- **Einstellungen** ist nur ein leerer Platzhalter — als echter, überzeugender Settings-Screen
  gestalten (Layout schon vorbereiten).
- Insgesamt mehr **Premium-Gefühl**: feinere Typo-Hierarchie, stimmigere Abstände, schönere
  Mikro-Interaktionen, ein klein wenig mehr Charakter/„Seele".

> **[Hier kann der Besitzer konkrete Wünsche ergänzen: bestimmte Farbrichtung, Layout-Ideen,
> Vorbilder/Referenzen, was ihm aktuell besonders missfällt. Diese Wünsche haben Vorrang.]**

---

## 10. Was du abliefern sollst (Output)

- **Eine vollständige, lauffähige `index.html`** (HTML + inline CSS + inline JS), die die
  bestehende Datei ersetzt — drop-in, ohne Build.
- Alle Funktionen aus Abschnitt 5–6 erhalten, alle API-Verträge aus Abschnitt 7 korrekt
  angebunden, Dark + Light, responsive (Breakpoint ~820px).
- Sauberer, kommentierter Code; keine externen JS-Libs; keine toten Verweise.

## 11. Unbedingt erhalten / Tabu

- Keine Funktion entfernen (Spektrum, Legende→Filter, Suche, Chips, Drawer, Theme-Toggle,
  Live-Polling, Verbindungsstatus, Vital-Karten, geplanter Settings-Bereich).
- Keine externen Abhängigkeiten außer Google Fonts + inline-SVG.
- Keine erfundenen API-Endpunkte oder Datenfelder.
- Deutschsprachige Oberfläche beibehalten.
- Nicht öffentlich denken (kein Login/Marketing/Mehrbenutzer) — es bleibt ein privates Cockpit.

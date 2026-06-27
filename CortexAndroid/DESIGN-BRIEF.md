# Design-Auftrag: „Cortex" — Android-App fürs zweite Gehirn

> Dieser Text beschreibt eine Android-Handy-App **vollständig aus Design-Sicht**, damit ein
> Designer ein sehr schönes, hochwertiges Erscheinungsbild entwerfen kann — **ohne den Code zu
> sehen**. Alle Bildschirme, Komponenten, Zustände, Interaktionen und **beide Farbmodi
> (Dunkel + Hell)** stehen drin. Sprache der App: **Deutsch**. Es ist eine **private App** (nur
> für die eigenen Geräte des Besitzers, kein App Store, kein Login, kein Marketing).

---

## 0. Deine Rolle

Du bist Produkt-Designer. Gestalte das komplette visuelle Erscheinungsbild der App **„Cortex"** —
Layout, Farben, Typografie, Komponenten, Abstände, Icons, Zustände und Mikro-Interaktionen — so,
dass es **aussieht und sich anfühlt wie ein gekauftes Premium-Produkt**. Die Funktionen liegen
fest (unten beschrieben); du gestaltest das Aussehen, nicht die Funktion.

**Abzuliefern:** ein vollständiger Screen-Satz **in Dunkel UND Hell** (jeder Bildschirm in beiden
Modi), inklusive der wichtigen Zwischenzustände (leer, lädt, Fehler, VPN getrennt, Aufnahme,
Vorlesen), plus die ausdefinierte Design-Sprache (Farbtokens, Typo-Skala, Komponenten,
Abstände, Bewegung). Format frei (Figma o.ä.).

---

## 1. Das Produkt in einem Absatz

„Cortex" ist die mobile Fassung eines privaten **zweiten Gehirns** — eines persönlichen Wissens-
und Erinnerungsspeichers. Mit der App kann der Besitzer **mit seinem Gehirn reden** (Dinge ablegen
und nachschlagen; ein Assistent antwortet), **reinschauen und stöbern** (eine schöne Statistik
seines Wissens + alle Einträge durchsuchen, ansehen, bearbeiten, löschen) und **alles per Sprache**
bedienen (reinsprechen, Antworten werden vorgelesen). Es ist ein ruhiges, edles, fast intimes
**Cockpit** — kein Standard-Admin-Dashboard. Die App ist nur über ein privates VPN mit dem Server
verbunden; deshalb gibt es oben einen **VPN-Schalter**, der zur Identität des Produkts gehört
(„ich öffne die Tür zu meinem Gehirn"). Stimmung: **kosmisch, fokussiert, hochwertig.**

---

## 2. Plattform-Rahmen

- **Android-Handy, Hochformat zuerst.** Einhändig bedienbar — wichtige Aktionen in Daumenreichweite
  (unten). Material 3 als Basis, aber mit eigener, edler Handschrift (nicht „Standard-Material").
- **Dunkel- UND Hell-Modus sind Pflicht**, gleichwertig schön gestaltet (Hell ist NICHT nur ein
  invertiertes Dunkel). Umschaltbar oben rechts; die Wahl bleibt erhalten.
- **Deutsch**, durchgehend. Privat — kein Login, keine Mehrbenutzer-Logik, kein Onboarding-Marketing.
- Ziel-Gefühl: **ruhig, tief, klar.** Viel Schwarzraum/Atemraum, weiche Tiefe, dezenter „Weltraum"-Glow.

---

## 3. Look & Feel / Design-Ziel

- **Stimmung:** „Kontrollraum eines Gehirns" — edel, ruhig, konzentriert, ein bisschen kosmisch.
  Nicht verspielt, nicht technisch-kühl. Es darf sich **besonders** anfühlen.
- **Signatur-Element Nr. 1 — das Gedächtnis-Spektrum:** eine farbige, gestapelte Balkenleiste, in
  der jede Wissens-Kategorie als Segment proportional zu ihrer Größe erscheint. Das ist das
  visuelle Markenzeichen und soll sehr schön sein (sanfte Farbübergänge, weiche Kanten, leichtes
  Leuchten).
- **Signatur-Element Nr. 2 — der VPN-Schalter:** der Moment des Verbindens soll sich gut anfühlen
  (sanfter Übergang grau→lebendig, dezenter Puls, „verbunden"-Bestätigung). Es ist die Tür ins Gehirn.
- **Hierarchie:** große Kennzahl + Spektrum sind die Stars des Dashboards. Im Gespräch ist die
  Konversation der Star, die Werkzeuge ordnen sich unter.
- **Premium-Details:** weiche, tiefe Schatten; abgestimmte Radien (~16–20 dp); dezente Verläufe und
  Glow; ruhige Mikro-Animationen; exzellente Typografie; großzügige, konsistente Abstände.

---

## 4. Globales Gerüst (auf jedem Bildschirm)

**(a) Topbar (oben):**
- Links die **Marke**: ein **🧠-Glyph** in einem farbig getönten, abgerundeten Kästchen + Wortmarke
  **„Cortex"** mit kleiner Unterzeile **„zweites Gehirn"** (monospace, gesperrt).
- Mitte/rechts der **VPN-Schalter** als auffällige Pille/Segment mit Status:
  **getrennt (grau/rot) · verbinde… (gelb, pulsierend) · verbunden (grün, ruhig leuchtend)**.
  Im verbundenen Zustand kann ein feiner Punkt/Glow signalisieren „Tunnel offen".
- Ganz rechts der **Theme-Umschalter** (Sonne/Mond), mit weichem Übergang beim Wechsel.

**(b) Untere Tab-Leiste (Bottom Navigation), 3 Ziele** — in Daumenreichweite:
**Gespräch · Dashboard · Einstellungen.** Aktiver Tab klar hervorgehoben (gefülltes Icon +
Akzentfarbe + zarter Hintergrund/Indikator). Start-Tab = **Gespräch**.

**(c) Verbindungsgefühl global:** Ist das VPN getrennt, sollen datenabhängige Bereiche ruhig
„schlafen" (gedämpft, Platzhalter) statt Fehler zu schreien — mit einem freundlichen Hinweis
„VPN aktivieren, um dein Gehirn zu erreichen" + dem Schalter.

---

## 5. Tab „Gespräch" (Hauptbildschirm)

Der zentrale Bildschirm: ein schönes Chat-Fenster, mit dem man redet (ablegen UND nachschlagen).

**Aufbau (von oben nach unten):**
1. **Nachrichten-Verlauf** (scrollbar): Sprechblasen. **Nutzer rechts** (in Akzentfarbe getönt),
   **Assistent links** (auf Flächenfarbe, ruhig). Großzügige Zeilenhöhe, angenehme Lesetypo.
2. **Meta-Zeile** dezent unter einer Assistenten-Blase, je nach Ereignis (kleine, gesperrte
   monospace-Schrift, gedämpft):
   - „↳ abgelegt in „<Kategorie>"" (etwas wurde gespeichert)
   - „↳ nachgeschlagen · N Treffer" (es wurde im Gehirn gesucht)
   - „↳ Rückfrage…" (der Assistent fragt nach, bevor er speichert)
   - „↳ nicht gespeichert" (abgebrochen)
3. **Optionen-Knöpfe** (wenn der Assistent eine Rückfrage stellt): anklickbare Chips/Buttons unter
   der Blase, z.B. **„Ja" / „Nein"** — klar, fingerfreundlich, in Akzentfarbe.
4. **Eingabeblock** (unten fixiert, über der Tastatur): besteht aus einer **Werkzeugzeile** und
   darunter dem **Textfeld** (mehrzeilig, wächst mit, Platzhalter „Ablegen oder nachschlagen…").

**Werkzeugzeile — diese 7 Elemente** (Designer löst die platzsparende Anordnung auf dem Handy,
z.B. Titel + Kategorie in einer einklappbaren oberen Zeile, die Aktions-Icons in einer Reihe):
| Element | Aussehen / Farbe | Bedeutung |
|---|---|---|
| **Titel** (Textfeld, optional) | dezent, klein | optionaler Titel für einen neuen Eintrag |
| **Kategorie** (Auswahl) | Pille/Dropdown | „Auto-Kategorie" (Standard) + Liste + „➕ Neue Kategorie…" |
| **Leeren (X)** | **rot/rose** | leert nur das Textfeld |
| **Vorlesen** (Lautsprecher) | **orange**, Toggle (Standard AN) | liest die Antworten vor (an/aus) |
| **Mikrofon** | **orange** | Spracheingabe (sprechen statt tippen) |
| **Verbessern „G"** | **grün** | glättet den Text (Grammatik/Zeichensetzung) |
| **Senden** (Papierflieger) | **orange** | Nachricht abschicken |

> Hinweis zur Farblogik: **Alles Audio-/Sprach-bezogene ist Orange** (Vorlesen, Mikrofon, Senden),
> **Verbessern ist Grün**, **Löschen/Leeren ist Rot** — das ist eine feste Hausfarben-Regel.

**Wichtige Zustände dieses Bildschirms (alle gestalten):**
- **Leerzustand:** noch keine Nachrichten → einladende, ruhige Mitte (z.B. das 🧠-Motiv dezent +
  „Sag oder tippe etwas — ich lege es ab oder schlage es nach.").
- **Aufnahme (Mikrofon aktiv):** auffälliger, schöner Aufnahme-Zustand — pulsierende
  Pegel-/Wellen-Animation in Orange, klarer **Stopp**-Knopf, Live-Vorschau des erkannten Textes.
- **Verarbeitung:** während der Assistent denkt/nachschlägt, ein ruhiger „tippt…"-Indikator in der
  Assistenten-Blase (kann ein paar Sekunden dauern).
- **Vorlesen aktiv:** die gerade gesprochene Blase ist sanft markiert (z.B. leichter Glow/Linie),
  mit kleinem **Stopp**-Knopf.
- **„Neue Kategorie"-Dialog:** ein hübscher kleiner Eingabedialog („Name der neuen Kategorie.
  Tipp: ‚Haupt/Unter' legt eine Unterkategorie an").

---

## 6. Tab „Dashboard" (Übersicht + Stöbern)

Zwei Bereiche untereinander auf demselben Bildschirm (scrollbar). **Aktualisiert sich ruhig von
selbst** (Werte animieren weich auf neue Stände). Oben in der Topbar zeigt die Verbindungs-Pille,
ob der Server antwortet.

### 6a. Übersicht (oben) — der schöne Gesamtblick
- **Hauptkarte „Gedächtnis-Spektrum":**
  - kleine Eyebrow-Zeile „GEDÄCHTNIS-SPEKTRUM" (monospace, gesperrt, Großbuchstaben).
  - eine **sehr große Zahl** = Gesamtzahl der Einträge, mit Unterzeile „EINTRÄGE GESAMT".
    Die Zahl soll beim Laden weich hochzählen.
  - die **gestapelte Farbleiste**: pro Kategorie ein Segment proportional zur Größe. Tippen auf ein
    Segment → springt in den Stöber-Bereich (6b), gefiltert auf diese Kategorie. Weiche Kanten,
    leichtes Leuchten, schöner Farbverlauf zwischen den Segmenten.
  - **Legende** darunter (scrollbar): je Kategorie ein farbiges Quadrat (Swatch) + Name +
    rechtsbündig die Anzahl (monospace). Tippen filtert ebenfalls.
  - **Konsistente Kategoriefarben:** dieselbe Kategorie hat überall (Spektrum, Legende, Chips, Tags)
    exakt dieselbe Farbe — feste 13-Farben-Palette (siehe §9), zyklisch zugewiesen.
- **Vier Vital-Karten** (auf dem Handy als **2×2-Raster**), jede klein und elegant:
  1. **Assistent** — Status-Punkt + Wort („Bereit" / „offline"), klein das verwendete Modell + Zahl
     aktiver Sitzungen.
  2. **Prozessor** — CPU in % + feiner Fortschrittsbalken (Meter).
  3. **Arbeitsspeicher** — % + klein „benutzt / gesamt" + Meter.
  4. **Speicherplatz** — % + „benutzt / gesamt" + Meter.
  Meter animieren weich; bei hohen Werten dezent wärmer (z.B. Amber/Rose) einfärben.

### 6b. Stöbern / Suchen (darunter)
- **Suchfeld** mit Lupe, Platzhalter „Im Gehirn suchen — z.B. ‚meine Ziele', ‚Kaffeemaschine'…".
  Tippen löst eine ruhige Live-Suche aus. Treffer sind semantisch und können einen
  **Relevanz-Wert in %** tragen.
- **Kategorie-Chips:** „Alle" + ein Chip je Kategorie (in Kategoriefarbe, wenn aktiv). Genau einer aktiv.
- **Trefferliste** (Karten): je Karte **Titel** (prominent), ein **farbiges Kategorie-Tag**, ein
  **2-zeiliges Text-Snippet** (abgeschnitten), bei Suche rechts der **Score in %**. Antippen öffnet
  die Detailansicht. Liste soll lebendig und gut scanbar sein (nicht monoton).
- **Detailansicht = Bottom-Sheet** (gleitet von unten herein, handy-typisch): Kategorie-Tag, voller
  **Titel**, **Datum**, kompletter **Text** in einer ruhigen Lesebox (monospace). Aktionen im Sheet:
  **Bearbeiten**, **Kategorie ändern**, **Löschen** (mit kurzer Bestätigung; Löschen = rot). Sheet
  schließbar per Wisch nach unten / X / Tippen auf den abgedunkelten Hintergrund.
- **Leer/Lade/Fehler:** „lädt…", „Nichts gefunden.", „Fehler beim Laden." ruhig und schön gestalten.

---

## 7. Tab „Einstellungen"

Ein überzeugender Settings-Screen mit klaren Sektionen (gruppierte Listen, edle Formfelder).

1. **Verbindung & VPN**
   - WireGuard-Konfiguration: **importieren** (Datei wählen) **oder** einfügen (großes Textfeld) —
     mit Statuszeile „Konfiguration vorhanden / fehlt".
   - Server-Adresse (Host + drei Ports) als ruhige Felder mit sinnvollen Vorbelegungen.
2. **Schlüssel** (Secrets)
   - Drei maskierte Felder (Passwort-Stil mit Auge-Umschalter): **Server-Schlüssel**,
     **Groq-Schlüssel** (Spracheingabe), **Gemini-Schlüssel** (Vorlesen + Verbessern).
3. **Sprache & Stimme**
   - Vorlesen an/aus (Standard AN), Stimmen-Auswahl, evtl. Sprechtempo.
4. **Darstellung**
   - Theme: Dunkel / Hell / (optional „System").
5. **Über**
   - App-**Version** sichtbar, Hinweis „privat".

Speichern-Feedback dezent (Häkchen/Toast). Felder mit klaren Labels + kurzen Hilfetexten.

---

## 8. Globale Interaktions-Logik (Verknüpfungen)

- **V1 — Spektrum/Legende → Stöbern:** Tippen auf eine Kategorie im Dashboard filtert den
  Stöber-Bereich auf genau diese Kategorie.
- **V2 — Eintrag → Detail-Sheet:** Tippen auf eine Trefferkarte öffnet das Bottom-Sheet mit
  Volltext + Aktionen.
- **V3 — Theme-Umschalter:** Sonne/Mond oben rechts, persistent, weicher Übergang.
- **V4 — Verbindungsstatus:** Die Pille in der Topbar zeigt grün „verbunden" / rot „getrennt".
- **V5 — VPN-Schalter:** der zentrale Akt — getrennt → verbinde… (Puls) → verbunden.
- **V6 — Konsistente Kategoriefarben** überall.
- **V7 — Vorlesen/Aufnahme** haben sichtbare, schöne Aktiv-Zustände (Orange, Puls, Stopp).

---

## 9. Farbsystem — KOMPLETT (Dunkel + Hell)

Beide Paletten sind vollwertig zu gestalten. Diese Werte sind die **Basis/Referenz** (du darfst sie
verfeinern, solange beide Modi durchdacht und edel wirken und die Kategoriefarben konsistent bleiben).

### 9a. Dunkel-Modus (Standard, kosmisch)
| Rolle | Farbe |
|---|---|
| Hintergrund (App) | `#0B0E14` (tiefes Weltraum-Blauschwarz) |
| Fläche / Karte | `#141A23` |
| Fläche erhöht (Sheet/Dialog) | etwas heller als Karte, z.B. `#1A2230` |
| Rahmen / Trennlinie | `#222C3A` |
| Text (primär) | `#E8EEF6` |
| Text (gedämpft) | `#8A98AC` |
| Akzent „Iris" (primär) | `#7B7BF5` |
| Akzent „Mint" | `#4FD1B0` |
| Akzent „Amber" | `#F2B65A` |
| Akzent „Rose" | `#F2698E` |
| Hintergrund-Glow | dezenter radialer Schimmer oben (Iris/Violett, sehr subtil) |

### 9b. Hell-Modus (gleichwertig schön, NICHT nur invertiert)
| Rolle | Farbe |
|---|---|
| Hintergrund (App) | `#F4F6FB` (weiches, kühles Off-White) |
| Fläche / Karte | `#FFFFFF` |
| Fläche erhöht (Sheet/Dialog) | `#FFFFFF` mit etwas mehr Schatten |
| Rahmen / Trennlinie | `#E4E8F1` |
| Text (primär) | `#1A2230` |
| Text (gedämpft) | gedämpftes Blaugrau, z.B. `#5C6B82` |
| Akzente | dieselben Iris/Mint/Amber/Rose, **etwas kräftiger/satter** für Kontrast |
| Hintergrund-Glow | sehr zarter, heller Verlauf statt dunklem Glow |

### 9c. Funktionsfarben (in beiden Modi)
| Bedeutung | Farbe |
|---|---|
| **Audio (Vorlesen, Mikrofon, Senden)** | **Orange `#F97316`** |
| **Verbessern (G)** | **Grün** (Mint `#4FD1B0` oder kräftigeres Grün) |
| **Löschen / Leeren** | **Rot/Rose** (`#F2698E` bzw. ein klares Rot) |
| Status verbunden | Grün |
| Status verbinde… | Amber/Gelb (pulsierend) |
| Status getrennt | Rot/Grau |

### 9d. Kategorie-Palette (13 Farben, zyklisch, in BEIDEN Modi konsistent)
`#7B7BF5` `#4FD1B0` `#F2B65A` `#F2698E` `#5AB0F2` `#B68CF5` `#3FD0D6` `#9BD05A` `#F08A5A`
`#5AD0A0` `#F25AC0` `#5A7BF2` `#D0C45A`
(Eine Kategorie behält ihre Farbe über Spektrum, Legende, Chips und Tags — in Dunkel wie Hell.)

---

## 10. Typografie

- **Space Grotesk** — Überschriften und die großen Zahlen (Dashboard-Kennzahl). Charaktervoll, edel.
- **Inter** — UI und Fließtext (Chat-Blasen, Listen, Formulare). Sehr gut lesbar.
- **JetBrains Mono** — Labels/Eyebrows („GEDÄCHTNIS-SPEKTRUM"), Meta-Zeilen, die Volltext-Lesebox
  im Detail-Sheet, die technische Fußzeile.
- Klare Größen-Hierarchie (z.B. Display/Headline/Title/Body/Label/Caption), großzügige Zeilenhöhen,
  keine zu engen Absätze. Du darfst die Schriftwahl verfeinern, solange es das Premium-Gefühl hebt.

---

## 11. Form, Tiefe, Icons

- **Radius:** ~16–20 dp für Karten/Sheets, ~12 dp für Felder/Chips, voll gerundet für Pillen/Toggles.
- **Tiefe:** weiche, tiefe Schatten in Dunkel sehr subtil (eher Glow/Rahmen), in Hell etwas klarer.
  Karten mit 1 dp-Rahmen in der Rahmenfarbe.
- **Glow:** dezenter radialer Schimmer im Hintergrund (Weltraum-Gefühl), nie aufdringlich.
- **Icons:** ein konsistentes, feines Linien-Icon-Set (Material-Symbols-Stil ok). Gefüllt nur für
  aktive Zustände/Akzent-Aktionen. Das 🧠-Markenmotiv darf ein eigenes, hübsches Glyph sein.

---

## 12. Bewegung & Mikro-Interaktionen

- **Tab-Wechsel:** dezentes Aufsteigen/Einblenden des Inhalts.
- **VPN-Verbinden:** sanfter Farbübergang grau→lebendig, kurzer Puls, „verbunden"-Bestätigung.
- **Aufnahme:** pulsierende Pegel-/Wellen-Animation (Orange).
- **Spektrum & Meter:** Balken/Meter animieren weich auf neue Werte (kein hartes Springen).
- **Detail-Sheet:** gleitet weich von unten herein, Hintergrund dimmt sanft.
- **Zahl im Dashboard:** zählt beim Laden weich hoch.
- **Reduced Motion:** Wenn das System „weniger Bewegung" verlangt, Animationen stark reduzieren
  (nur Ein-/Ausblenden).

---

## 13. Durchgängige Zustände (für JEDEN datenabhängigen Bereich gestalten)

- **Lädt:** ruhige Skeletons/Schimmer statt harter Spinner.
- **Leer:** freundliche, gestaltete Leerzustände (kurzer Satz + dezentes Motiv).
- **Fehler:** ruhig, nicht alarmierend; „Erneut versuchen"-Möglichkeit.
- **VPN getrennt / Server offline:** Inhalte gedämpft „schlafend", zentraler Hinweis „VPN
  aktivieren" + Schalter. Keine roten Fehlerwände.

---

## 14. Barrierefreiheit & Handhabung

- **Kontrast** ausreichend in beiden Modi (Text auf Flächen gut lesbar, auch gedämpfter Text).
- **Touch-Ziele** mind. ~48 dp; Aktions-Icons nicht zu eng (Werkzeugzeile sorgfältig lösen).
- **Dynamische Schrift** respektieren (Layout darf bei größerer Systemschrift nicht brechen).
- **Reduced Motion** respektieren (siehe §12).
- Wichtige Aktionen (Senden, Mikrofon, VPN) in **Daumenreichweite**.

---

## 15. Was abzuliefern ist

Pro **Dunkel** und **Hell** mindestens diese Screens/Zustände:
1. **Gespräch** — Leerzustand, laufende Unterhaltung (mit Meta-Zeile + Optionen-Knöpfen),
   Aufnahme-Zustand, Vorlesen-Zustand, „Neue Kategorie"-Dialog.
2. **Dashboard** — Übersicht (Kennzahl + Spektrum + Legende + 2×2-Vitals), Stöbern (Suche +
   Chips + Trefferliste mit Score), Detail-Bottom-Sheet (Lesen + Bearbeiten + Löschen-Bestätigung).
3. **Einstellungen** — alle Sektionen, ein Key-Feld maskiert/aufgedeckt, VPN-Config-Import,
   „Über" mit Version.
4. **Globale Zustände** — VPN getrennt (schlafender Inhalt + Hinweis), VPN verbinde… (Puls),
   VPN verbunden; Lade- und Fehlerzustände.
5. Die **Design-Sprache** dokumentiert: Farbtokens (beide Modi), Typo-Skala, Komponenten
   (Buttons/Chips/Felder/Karten/Sheet/Toggle/Vital-Karte/Spektrum), Abstände, Icon-Stil, Bewegung.

---

## 16. Unbedingt erhalten / Tabu

- **Beide Modi gleichwertig** schön (Hell nicht nur invertiert).
- **Kategoriefarben konsistent** über Spektrum/Legende/Chips/Tags, in beiden Modi.
- **Audio = Orange, Verbessern = Grün, Löschen = Rot** beibehalten.
- Keine Funktion „verstecken": VPN-Schalter, alle 7 Werkzeuge im Gespräch, Spektrum, Stöbern mit
  Bearbeiten/Löschen, Vital-Karten, alle Einstellungs-Sektionen müssen Platz im Design haben.
- **Deutschsprachig**, privat (kein Login/Marketing/Mehrbenutzer).
- Handy-Hochformat, einhändig bedienbar, untere Tab-Leiste.
- Ruhig und edel — kein lautes Standard-Admin-Look.
```

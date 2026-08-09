---
name: design-umsetzer
description: >-
  Stufe 3 der Programm-Pipeline und zugleich der eigenstaendige Design-Umsetzer.
  Baut aus einem Bau-Spec-Paket (Specs/<App>/v2/BAU-AUFTRAG.md mit Funktions-, UI- und
  Motion-Spec) ODER aus einem blossen Design-Entwurf ein fertiges Programm — auf Android
  (Kotlin/Jetpack Compose), Windows (C#/.NET WPF) oder macOS (Swift/SwiftUI). Liegt ein
  Funktions-Spec vor, wird nicht nur die Oberflaeche gebaut, sondern auch das dahinter
  liegende Verhalten (Daten, Regeln, Persistenz). Zusaetzliche Trigger fuer den
  Pipeline-Fall: "Bau-Auftrag umsetzen", "Spec umsetzen", "Spec-Paket bauen",
  "Programm aus dem Spec bauen", "Stufe 3", "bau die App aus dem Spec", "v2 umsetzen".
  Setzt einen mit Claude Designs erstellten und im Repo unter Designs/Outbox/ gespeicherten
  Design-Entwurf 1:1 exakt in Jetpack Compose um — Farben, Abstaende, Schriftgroessen,
  Schriftarten, Eckenradien, Schatten, Anordnung und Animationen exakt aus den
  Design-Dateien und analysiert den gesamten Projektordner rekursiv inklusive aller
  Begleitdateien wie Audio, Bilder, Fonts, Animationen und Daten. Uebernimmt ALLE
  im Design enthaltenen Theme-Varianten (Light/Dark und Zusatz-Themes) als umschaltbare
  Themes. Setzt sowohl bestehende Apps neu um (Redesign vorhandener Screens) als auch
  komplett neue Apps aus dem Design. Programmiert Funktionen, die NEU im Design dazugekommen
  sind und in der App noch fehlen, mit ein. Bei einer neuen App oder bei neuen Bereichen
  schreibt er zuerst ein design-treues SPEC (SPEC.md im App-Projekt) und baut danach; bei
  reiner Design-Anpassung einer bestehenden App uebernimmt er das Design direkt 1:1 ohne SPEC.
  Findet der Skill den passenden Design-Ordner
  nicht automatisch, fragt er den Benutzer nach dem Pfad. Nutze diesen Skill IMMER wenn der
  Benutzer sagt "Design umsetzen", "Design-Umsetzer", "setze das Design um", "setze mein
  Cloud-Design um", "Design aus dem Designs-Ordner umsetzen", "Werft-Studio-Design umsetzen", "mach das Design in die App",
  "Design 1 zu 1 umsetzen", "Design in Jetpack Compose umsetzen", "uebernimm das Design",
  "setze den Design-Entwurf um", "implementiere das Design", "/design-umsetzer", oder
  irgendetwas das damit zu tun hat einen fertigen Design-Entwurf (Claude Designs / Cloud
  Designs) exakt in eine Android-App umzusetzen. Auch bei "bau das Design nach" oder
  "setze das Prototyp-Design in die App um".
---

# Design-Umsetzer: Claude-Design 1:1 in Jetpack Compose umsetzen

**Version:** v1.2.0 - 28.07.2026 20:50 Uhr

Du bist der weltbeste Android-Umsetzer fuer fertige Design-Entwuerfe. Deine einzige
Aufgabe: Ein mit **Claude Designs** ("Cloud Designs") erstellter und im Repo abgelegter
Design-Entwurf wird **exakt, pixelgenau, 1 zu 1** in eine echte Android-App mit
**Jetpack Compose** uebertragen — Farben, Abstaende, Schriftgroessen, Schriftarten,
Eckenradien, Schatten, Anordnung, Zustaende und Animationen **exakt aus den Design-Dateien**.

Der Design-Entwurf ist die **einzige Wahrheit**. Wenn das bestehende App-Design vom
Entwurf abweicht, gewinnt IMMER der Entwurf. Du erfindest nichts dazu, du laesst nichts
weg, du "verbesserst" nichts eigenmaechtig. Was im Entwurf steht, kommt exakt so in die App.

**Merksatz (vom Benutzer vorgegeben):** "Das ist der Design-Entwurf fuer die gesamte App.
Setze genau dieses Design mit Jetpack Compose um — Farben, Abstaende, Schriftgroessen und
Anordnung exakt aus der Datei."

## Vollstaendigkeit — 100%, nichts weglassen (oberste Regel)

Das **komplette** Design wird umgesetzt, nicht nur ein Teil. Ausdruecklich zu **100%**:

- **JEDER Bildschirm** und **jeder Unterbildschirm** — Haupt-Screens, Detail-Screens,
  Dialoge, Bottom-Sheets, Onboarding, leere/fehler/lade-Zustaende, Overlays, Tooltips.
- **JEDES Untermenue** — Tabs, Bottom-Nav-Ziele, Drawer, Dropdowns, Kontextmenues,
  aufklappbare Bereiche, Einstellungs-Unterseiten.
- **JEDE Verknuepfung/Navigation** — jeder Button, jedes Nav-Item, jeder Link fuehrt
  genau dorthin, wohin der Entwurf ihn fuehrt. Keine toten Buttons, keine Sackgassen.
- **JEDES visuelle Detail** — jede Farbe, jede Fettschrift (font-weight), jede Schriftart,
  jeder Abstand, jeder Eckenradius, jeder Schatten, jeder Gradient, jeder Glow/Blur,
  jede Animation. Alle Themes (Light/Dark + Zusatz).
- **JEDE Begleitdatei** im Designprojekt — insbesondere Audio, Bilder, Icons, Fonts,
  Videos, Animationen und strukturierte Daten. Relevante Dateien werden nicht nur
  inventarisiert, sondern unveraendert uebernommen und funktional passend eingebunden.

Ein zu 90% umgesetztes Design ist **nicht** erledigt. Bevor der Skill fertig meldet,
muss **jeder** Eintrag des Screen-/Navigations-/Token-Inventars aus Phase 1 im Code
nachweisbar vorhanden sein (Vollstaendigkeits-Check in Phase 8). Nichts wird
stillschweigend ausgelassen, gekuerzt oder "spaeter"-vertagt.

---

## Aufruf

Der Benutzer sagt z.B.:
- "Setze das Design um" / "Design-Umsetzer starten" / "/design-umsetzer"
- "Setze das Fisetin-Design 1 zu 1 um"
- "Uebernimm das Cloud-Design in die App"

Ein Parameter (App- oder Design-Name) kann angegeben sein, muss aber nicht.
Fehlt er, ermittelst du den Design-Ordner in Phase 0 selbst.

---

## Zwei Betriebsarten — zuerst feststellen, welche vorliegt

| Betriebsart | Erkennungsmerkmal | Was verbindlich ist |
|-------------|-------------------|---------------------|
| **P — Pipeline (Stufe 3)** | Es gibt `~/proggs/Specs/<App>/v2/BAU-AUFTRAG.md` | Das **Spec-Paket**: `01-FUNKTIONS-SPEC.md` fuers Verhalten, `02-UI-SPEC.md` fuers Aussehen, `03-MOTION-SPEC.md` fuer jede Bewegung. Der Design-Ordner ist ergaenzender Augenschein |
| **D — Nur Design** | Es gibt nur einen Design-Ordner unter `Designs/Outbox/<Name>/` | Der **Design-Entwurf** ist die einzige Wahrheit (der klassische Lauf dieses Skills) |

Pruefe das **als Allererstes**: `ls ~/proggs/Specs/<App>/v2/`. Existiert der Ordner,
laeufst du in Betriebsart **P** — dann ist das Spec-Paket der Einstieg und nicht der
Design-Ordner. In Betriebsart P entfaellt Phase 4 (SPEC schreiben) vollstaendig: das
Spec-Paket **ist** das SPEC, es wurde in Stufe 2 geschrieben und vom Benutzer freigegeben.
Der Gesamtablauf der Pipeline steht in `~/proggs/Specs/README.md`.

**Achtung Pfad:** Design-Entwuerfe liegen seit der Umstellung auf die Pipeline **nicht mehr**
direkt in `Designs/`, sondern in `~/proggs/Designs/Outbox/<Name>/`. `Designs/` enthaelt nur
noch die beiden Briefkaesten `Inbox/` (raus zum Designer) und `Outbox/` (zurueck).

---

## Zielplattform und Technik-Weg

Der Skill baut auf drei Plattformen. Die Zielplattform steht in Betriebsart P in
`Specs/<App>/v2/00-PROJEKT.md` §2; in Betriebsart D leitest du sie aus dem Design ab
(`design-tokens.json` → `plattform`) und laesst sie bestaetigen.

| Plattform | Technik-Weg | Theme-Schicht | Bewegung |
|-----------|-------------|---------------|----------|
| **Android** | Kotlin + Jetpack Compose, Material 3 | `Color.kt` / `Type.kt` / `Shape.kt` / `Theme.kt`, eigene `LocalAppColors`-Palette | `animate*AsState`, `AnimatedVisibility`, `updateTransition`, `infiniteRepeatable` |
| **Windows** | C# / .NET + WPF | `ResourceDictionary` je Erscheinung (`Colors.xaml`, `Typography.xaml`, `Shapes.xaml`), Umschaltung ueber `MergedDictionaries` | `Storyboard` + `DoubleAnimation`/`ColorAnimation`, `KeyTime`, `EasingFunction` (`CubicBezier` ueber `KeySpline`) |
| **macOS** | Swift + SwiftUI | `Color`-Extension je Erscheinung, `Environment`-basierte Theme-Auswahl | `withAnimation`, `.animation(_:value:)`, `Animation.timingCurve(...)`, `.repeatForever()` |

Steht im Repo bereits ein Projekt derselben Plattform, richtest du dich nach dessen Aufbau
und Versionen (Windows: `*.csproj`; macOS: `Package.swift`/`*.xcodeproj`). Bei mehreren
Zielplattformen im Spec baust du die **fuehrende** zuerst vollstaendig und danach die
weiteren mit demselben Verhalten — dafuer gilt zusaetzlich der `cross-platform`-Skill.

Eine `cubic-bezier(x1,y1,x2,y2)`-Kurve aus dem Design wird auf jeder Plattform als genau
diese Kurve umgesetzt (Compose: `CubicBezierEasing(x1,y1,x2,y2)`; WPF: `KeySpline="x1,y1 x2,y2"`;
SwiftUI: `.timingCurve(x1,y1,x2,y2)`). Sie wird **nie** durch eine eingebaute Standardkurve
ersetzt.

---

## Ueberblick der Phasen (in dieser Reihenfolge)

0. **Quelle finden** — Betriebsart P: Spec-Paket; Betriebsart D: Design-Ordner
   (raten + bestaetigen lassen; sonst Pfad erfragen)
1. **Design und alle Begleitdateien vollstaendig einlesen** und exakt inventarisieren
2. **App-Ordner zuordnen** (bestehende App redesignen ODER neue App anlegen)
3. **IST-Analyse** der bestehenden App + **Umsetzungs-Fall** bestimmen (A/B/C)
4. **SPEC schreiben** (bedingt — bei neuer App und bei neuen Bereichen; siehe Fall-Logik)
5. **Mapping** Design-Tokens → Compose-Theme + Screens planen
6. **1:1-Umsetzung** in Jetpack Compose (Code schreiben)
7. **Neue Funktionen** aus dem Design identifizieren und mit-implementieren
8. **Verifikation** (Build + visueller Abgleich gegen das Design)

Melde zu Beginn kurz: "Design-Umsetzer gestartet. Ich suche zuerst den Design-Ordner,
lese den Entwurf komplett ein und setze ihn dann 1:1 in Jetpack Compose um."

**Zweck des Skills (Leitidee):** Aus einem Design eine fertige App bauen — ODER auf
eine bestehende App ein neues Design 1:1 obendrauf legen. Das Design ist immer die
Referenz; alles (Farben, Abstaende, Effekte) orientiert sich exakt daran.

---

## Phase 0 — Quelle finden

### Betriebsart P zuerst pruefen

`ls ~/proggs/Specs/` und, wenn ein App-Name bekannt ist, `ls ~/proggs/Specs/<App>/v2/`.
Findest du dort ein `BAU-AUFTRAG.md`, ist **das** deine Quelle:

1. `BAU-AUFTRAG.md` lesen — es nennt die Zielplattform(en), die verbindlichen Quellen und
   in §4 die vollstaendige Abhakliste aller Kennungen (`B-`, `F-`, `M-`, `A-`).
2. Den Design-Ordner, auf den es verweist, zusaetzlich heranziehen (Augenschein).
3. Stehen in `BAU-AUFTRAG.md` §5 noch **offene Fragen**, diese dem Benutzer vorlegen und
   klaeren, **bevor** gebaut wird. Ein Bau-Auftrag mit offenen Fragen wird nicht losgebaut.
4. Bestaetigen lassen: "Ich baue `<App>` aus `Specs/<App>/v2/` fuer `<Plattform>`. Richtig?"

Danach direkt weiter zu Phase 1. Findest du kein `v2`, laeufst du in Betriebsart D:

### Betriebsart D — Design-Ordner finden

**Feste Ordner-Struktur (wichtig):** Der Standard-Container fuer ALLE Design-Entwuerfe
ist der Ordner **`~/proggs/Designs/Outbox/`** — dort liegt alles, was aus dem Designer
zurueckkommt. Er ist kein Design-Ordner, sondern
nur die Sammlung. **Jedes einzelne Designprojekt liegt als eigener Unterordner darin**
— z.B.
`~/proggs/Designs/Outbox/Fisetin-Begleiter-Design-Update/`. Der Unterordner ist das,
was 1:1 umgesetzt wird. Ordnernamen koennen Bindestriche ODER Leerzeichen enthalten.

`~/proggs/Designs/Inbox/` enthaelt **keine Designs**, sondern die Spec-Dateien, die an den
Designer gehen. Sie ist nie eine Umsetzungsquelle.

Im `Outbox/`-Ordner liegt ausserdem eine **`README.md`** (Index, listet die Projekte auf).
Diese README ist **kein** Designprojekt — sie NIE als Design-Ordner behandeln, aber ihren
Inhalt gerne lesen, um die vorhandenen Projekte/Namen zu erkennen.

Ein Projekt-Unterordner enthaelt typischerweise:

| Datei | Inhalt | Rolle fuer die Umsetzung |
|-------|--------|--------------------------|
| `*.dc.html` | Der komplette Design-Prototyp (HTML/CSS/Handlebars) | **PRIMAERE Quelle** — hier stehen ALLE exakten Werte |
| `android-frame.jsx` | Material-3-Geraeterahmen (Statusbar, AppBar, NavBar) | Referenz fuer M3-Kontext, NICHT selbst nachbauen |
| `support.js` | Generierte React-Runtime | **Ignorieren** (nur Rendering-Maschinerie, kein Design) |
| `.thumbnail` | Vorschaubild des Designs (**optional**, nicht immer vorhanden) | Falls da: visueller Gesamteindruck / Abgleich |
| Weitere Dateien/Unterordner | Audio, Bilder, Icons, Fonts, Videos, Animationen, JSON/CSV/Texte usw. | **Vollstaendig analysieren und bei Relevanz 1:1 umsetzen** |

Diese Tabelle ist nur eine Orientierung, keine Positivliste. Nach Auswahl des
Projekt-Unterordners immer dessen **gesamten Verzeichnisbaum rekursiv** erfassen. Auch
ungewoehnliche Dateiendungen, versteckte Dateien und tief verschachtelte Assets gehoeren
zum Designprojekt und duerfen nicht uebersehen werden.

**Zweite Herkunft: Werft Studio.** Neben den Claude-Designs (`*.dc.html`) gibt es Entwuerfe,
die mit **Werft Studio** gebaut und dort ueber "Projekt als ZIP herunterladen" ausgepackt
wurden. Die erkennst du sicher an einem Unterordner **`WERFT-DESIGN/`**. Er ist dann die
PRIMAERE Quelle — nicht die uebrigen Projektdateien, die daneben liegen:

| Pfad im Design-Ordner | Inhalt | Rolle |
|-----------------------|--------|-------|
| `WERFT-DESIGN/design-tokens.json` | Alle gemessenen Werte maschinenlesbar | **VERBINDLICH** — Erscheinungen mit vollstaendigen Token-Tabellen, Bildschirme, Farben, Masse, Typografie, Radien, Effekte, Assets, Texte |
| `WERFT-DESIGN/DESIGN-SPEC.md` | Dieselben Werte lesbar + Bildschirm-Tabelle | Checkliste fuer den Vollstaendigkeits-Abgleich |
| `WERFT-DESIGN/bildschirme/<erscheinung>/<nr>-<name>.html` | JEDER Bildschirm in JEDER Erscheinung als eigene Datei | So muss der Bildschirm in genau dieser Erscheinung aussehen |
| `WERFT-DESIGN/bildschirme/design.css` | Gemeinsames Stylesheet aller Bildschirme | Die Regeln, die in den Einzeldateien greifen |
| `WERFT-DESIGN/design.html` | Das durchklickbare Gesamtdesign | Klickweg/Navigation, Umschalter fuer Bildschirm und Erscheinung |
| Uebriger Ordnerinhalt | Originalprojekt mit Bildern, Fonts, Audio, Daten | Wie gehabt vollstaendig inventarisieren |

Liegt `WERFT-DESIGN/` vor, entfaellt die Suche nach `*.dc.html`: `design-tokens.json` ersetzt
die Token-Extraktion aus dem HTML. Fehlt der Ordner, ist es ein Claude-Design (Tabelle oben).

**Ablauf:**

1. `Glob("Designs/Outbox/*")` bzw. `ls ~/proggs/Designs/Outbox/` — alle **Unterordner** auflisten
   (Dateien im Root wie `README.md` sind KEINE Projekte). Optional die `README.md` lesen.
2. **Automatisch raten + bestaetigen:**
   - Bei genau EINEM Projekt-Unterordner: diesen vorschlagen.
   - Bei mehreren: den plausibelsten anhand des Aufruf-Parameters / App-Namens
     waehlen (z.B. Parameter "Fisetin" → Ordner `Fisetin-Begleiter-Design-Update`).
   - Dem Benutzer den Fund zeigen und **bestaetigen lassen**:
     "Ich habe den Design-Ordner `Designs/<Name>` gefunden. Diesen umsetzen? (ja / anderer)"
3. **Fallback — Ordner nicht gefunden oder unklar:** Den Benutzer direkt fragen:
   "Ich konnte den Design-Ordner nicht eindeutig finden. Bitte kopiere mir den Pfad
   des Design-Ordners hier rein." Danach mit dem genannten Pfad weiterarbeiten.

Erst weitermachen, wenn der Projekt-Unterordner feststeht. Danach mit einem rekursiven
Glob (`Designs/<Name>/**/*`) das vollstaendige Datei- und Unterordner-Inventar erstellen.

---

## Phase 1 — Quelle vollstaendig einlesen

### Betriebsart P — das Spec-Paket lesen

Alle vier Dateien aus `~/proggs/Specs/<App>/v2/` **vollstaendig** lesen, keine ueberfliegen:

| Datei | Wofuer sie verbindlich ist |
|-------|----------------------------|
| `00-PROJEKT.md` | Zielplattform(en), Zielgeraet, Sprache, Abnahmekriterien `A-` |
| `01-FUNKTIONS-SPEC.md` | Jedes Verhalten: Ausloeser, Ablauf, Daten, Ergebnis, Fehlerfall, Regeln, Datenmodell |
| `02-UI-SPEC.md` | Jede Farbe, jedes Mass, jede Schrift, jeder Bildschirm `B-`, jeder Zustand |
| `03-MOTION-SPEC.md` | Jede Bewegung `M-` mit Dauer, Kurve, Verzoegerung, Wiederholung |
| `04-ONBOARDING-SPEC.md` | Falls vorhanden: der Erststart-Ablauf, Berechtigungen und ihr Zeitpunkt |
| `05-RECHT-SPEC.md` | Falls vorhanden: Pflichttexte, Einwilligungen, Datenverarbeitung, Loeschung |
| `AENDERUNGEN.md` | Nur zur Einordnung — was der Designer gegenueber v1 geaendert hat |

Die Werte darin sind bereits gemessen und freigegeben. Du **extrahierst hier nichts mehr
selbst** und rechnest nichts um — du liest ab. Den Design-Ordner ziehst du zusaetzlich
heran, um das Ergebnis optisch abgleichen zu koennen (Abschnitte unten), nicht um die
Werte zu ersetzen. Weicht der Design-Ordner vom Spec-Paket ab, gilt das Spec-Paket, und du
meldest die Abweichung.

Uebernimm die Abhakliste aus `BAU-AUFTRAG.md` §4 als deine Arbeitsliste — sie ersetzt das
Inventar aus E2 und ist die Grundlage des Vollstaendigkeits-Abgleichs in Phase 8.

### Betriebsart D — Design vollstaendig einlesen und Tokens extrahieren

**Grundsatz:** Wer den Entwurf und seine Begleitdateien nicht komplett analysiert hat,
kann ihn nicht 1:1 umsetzen.
Lies die `*.dc.html` **vollstaendig** (sie kann gross sein, 50-100 KB — trotzdem ganz lesen,
notfalls in mehreren Read-Baecken mit `offset`/`limit`). Lies zusaetzlich `android-frame.jsx`
und sieh dir `.thumbnail` visuell an, **falls vorhanden** (Read-Tool ist multimodal;
die Datei fehlt in manchen Projekten — dann ohne sie weiterarbeiten). `support.js` nur als
Runtime klassifizieren und nicht als Design-Quelle lesen; Referenzen daraus auf externe
Assets duerfen bei der Vollstaendigkeitspruefung jedoch nicht verloren gehen.

### Pflicht-Inventar aller Begleitdateien

Vor der Token-Extraktion jede gefundene Datei nach Pfad, Typ, Groesse, Rolle und
Verwendung erfassen. Geeignete Datei-Tools bzw. Metadaten-Tools verwenden und mindestens
folgende Gruppen pruefen:

- **Audio:** `.wav`, `.mp3`, `.ogg`, `.m4a`, `.aac`, `.flac` usw. — Format, Dauer,
  Kanaele und erkennbare Verwendungsstelle erfassen. Referenzen in HTML/CSS/JS/JSON,
  Dateinamen und Begleitdokumentation pruefen. Ist die Zuordnung zu einer Aktion nicht
  eindeutig, den Benutzer fragen statt einen Trigger zu erfinden.
- **Grafik/Medien:** Rasterbilder, SVGs, Icons, Videos und Animationsdateien (z.B.
  Lottie) visuell bzw. strukturell pruefen und ihre konkrete UI-Verwendung erfassen.
- **Fonts:** Fontdateien, Schnitte, Gewichte und Lizenzen erfassen; vorhandene Dateien
  bevorzugen, statt dieselbe Schrift erneut extern zu laden.
- **Daten/Metadaten:** JSON, CSV, XML, TXT, Markdown und Konfigurationsdateien lesen und
  pruefen, ob sie Inhalte, Zustandsvarianten, Navigation oder Asset-Zuordnungen enthalten.
- **Unbekannte Dateien:** Nicht still ignorieren. Typ ermitteln, Relevanz dokumentieren
  und bei unklarer fachlicher Bedeutung gezielt nachfragen.

Fuer jede relevante Begleitdatei festhalten: **Quelle → Zielpfad in der Android-App →
Verwendungsstelle/Trigger**. Binaerdateien inhaltlich bytegenau uebernehmen, nicht neu
kodieren, komprimieren, ersetzen oder durch aehnliche Stock-Assets austauschen. Nur wenn
Androids Ressourcenregeln es erzwingen, Dateinamen legal normalisieren und die Umbenennung
im Quelle-Ziel-Mapping dokumentieren.

### Werft-Studio-Paket: Werte LESEN statt extrahieren

Enthaelt der Design-Ordner `WERFT-DESIGN/`, laeuft Phase 1 anders — und deutlich genauer:

1. `WERFT-DESIGN/design-tokens.json` **vollstaendig** lesen. Alles darin ist deterministisch
   aus den Quellen gemessen; nichts davon schaetzen, runden oder "vereinheitlichen".
   - `erscheinungen[]` — jede mit `id`, `art` (light/dark/other) und **vollstaendiger**
     `tokens`-Tabelle. ALLE werden als umschaltbare Themes umgesetzt, nicht nur die erste.
   - `bildschirme[]` — nummeriert, mit `istStart`, `navigiertZu` und je Erscheinung dem Pfad
     zur Einzeldatei. Das ist das Screen-Inventar aus E2, fertig und ohne Raten.
   - `farben`, `masse`, `typografie`, `formen`, `effekte`, `assets`, `texte` — die Abschnitte
     A bis D und H dieser Phase, bereits exakt.
   - `vollstaendigkeit.nichtAufgebaut` — hier stehen Bildschirme, die dem Design FEHLEN. Ist
     die Liste nicht leer, dem Benutzer melden, BEVOR gebaut wird.
2. Jeden Bildschirm in **JEDER** Erscheinung ansehen
   (`bildschirme/<erscheinung>/<nr>-<name>.html`); dort steht das fertige Markup mit seinen
   Klassen, die zugehoerigen Regeln in `bildschirme/design.css`. Ein Bildschirm gilt erst als
   erfasst, wenn er in allen Erscheinungen angesehen wurde.
3. `design.html` fuer den Klickweg: `data-werft-navigate="<ziel-id>"` ist die Navigation,
   `data-screen-id`/`data-screen-name` die Bildschirm-Kennung.
4. Die Punkte A bis H unten nur noch fuer das ergaenzen, was im JSON nicht steht.

> Technische Details zum Aufbau der Claude-Design-Dateien (wo genau welche Werte stehen,
> wie `<x-dc>`, Props, `data-t`-Themes und die `{{ }}`-Platzhalter funktionieren):
> siehe **[references/design-format.md](references/design-format.md)** — bei Bedarf laden.

Extrahiere in ein internes **Design-Token-Dokument** (nichts schaetzen, alles woertlich
aus der Datei uebernehmen):

**A) Farben & Themes**
- Alle CSS-Custom-Properties im `#...{ }`-Block und in den `[data-t...]`-Selektoren
  (z.B. `--bg0`, `--bg1`, `--card`, `--text`, `--sub`, `--acc`, `--acc2`, `--onAcc`,
  `--ok`, `--warn`, `--bad`, `--line`, `--navBg`, `--glow` …) mit **exaktem** Hex-/RGBA-Wert.
- **JEDE** Theme-Variante als eigenes Set erfassen (Basis-Theme, `*-dark`, sowie
  Zusatz-Themes wie `vital`, `ember` inkl. deren `-dark`). Der Benutzer will **alle**
  Themes umgesetzt (siehe Phase 5, Multi-Theme).
- Alpha/Transparenzen exakt uebernehmen (rgba mit Kommazahl), nicht runden.

**B) Typografie**
- Schriftfamilie(n) aus dem `<helmet>`/`@import`/`font-family` (z.B. `Outfit` von Google
  Fonts) inkl. **aller** eingebundenen Schriftgewichte (z.B. 300;400;500;600;700;800).
- Jede vorkommende `font-size` (px), `font-weight`, `line-height`, `letter-spacing`.
  Baue daraus eine Typo-Skala (Titel/Body/Label …) mit den EXAKTEN Werten des Entwurfs.

**C) Abstaende & Groessen**
- Alle `padding`, `margin`, `gap`, feste `width`/`height`, `top/left`-Offsets in px.
- Erkenne das genutzte Raster (oft 4/8-System, aber uebernimm die realen Werte, nicht das Ideal).

**D) Formen & Tiefe**
- Alle `border-radius` (px) je Komponente, `border`/`--cardBrd`, `box-shadow`/`--shadow`,
  `filter: blur(...)`, Gradients (`linear-/radial-gradient` mit exakten Stops).

**E) Layout & Struktur (Anordnung)**
- Screen-fuer-Screen: welche Elemente in welcher Reihenfolge, Ausrichtung
  (flex-direction, align/justify), Verschachtelung, Karten, Listen, Chips.
- Navigation: Bottom-Nav / Tabs / AppBar — welche Eintraege, welche Icons, welcher
  aktive Zustand. `android-frame.jsx` zeigt den M3-Rahmen-Kontext.
- Zustaende: aktiv/inaktiv, ausgewaehlt, leer, Fehler, Ladephasen — falls im Entwurf vorhanden.

**E2) VOLLSTAENDIGES Screen- & Navigations-Inventar (Pflicht fuer 100%)**
- Erstelle eine **nummerierte Liste ALLER Bildschirme und Unterbildschirme** des
  Entwurfs — inkl. Dialoge, Bottom-Sheets, Overlays, Onboarding, Menue-Unterseiten.
- **Verborgene/bedingte Screens finden:** Der Prototyp blendet Ansichten oft ueber
  `{{ #if }}`/`{{ #each }}`-Bloecke, `data-*`-Zustaende, Tab-/Routen-Variablen oder
  verschiedene `{{ themeKey }}`/Props-Werte ein. JEDE dieser Varianten ist ein
  umzusetzender Zustand/Screen — auch was nur unter einer Bedingung sichtbar wird.
  Prueft dazu auch das Props-JSON (`data-props`) auf weitere Ansichts-/Tab-Werte.
- **Navigations-/Verknuepfungs-Karte:** Fuer JEDES interaktive Element (Button, Nav-Item,
  Chip, Link, Listeneintrag) notieren, **wohin** es fuehrt. Diese Karte wird spaeter
  1:1 als Navigation verdrahtet — kein Button bleibt ohne Ziel.
- Dieses Inventar ist die **Checkliste** fuer den Vollstaendigkeits-Abgleich in Phase 8.

**F) Animationen & Motion**
- Alle `@keyframes` (Name + Verlauf), `animation:`-Kurzformen (Dauer, Easing, Delay,
  iteration), `transition:`-Angaben. Diese in Compose mit `animate*AsState`,
  `AnimatedVisibility`, `updateTransition`, `infiniteRepeatable` etc. nachbilden.

**G) Inhalte & Funktionen**
- Welche Screens/Funktionen zeigt der Entwurf (z.B. Dashboard, Detail, Einstellungen,
  Onboarding, Timer, Statistik …)? Liste sie auf — Basis fuer Phase 7 (neue Funktionen).

**H) Asset- und Daten-Inventar**
- Alle Begleitdateien mit Quelle, Zielpfad und Verwendung auflisten.
- Audio-Trigger, Wiedergabeverhalten (einmalig/Loop), Lautstaerke und ggf. Stopp-Regeln
  exakt aus dem Design bzw. seinen Daten uebernehmen. Fehlen diese Angaben, nachfragen.
- Wenn Audio vorhanden ist, den `android-audio`-Skill fuer die konkrete Android-
  Integration laden. Kurze UI-Sounds typischerweise mit `SoundPool`, laengere Musik/
  Sprache mit Media3 umsetzen; die tatsaechliche Wahl am Nutzungsszenario ausrichten.

Am Ende von Phase 1 hast du eine vollstaendige, exakte Token-, Screen- und Asset-Liste.
Kurz an den Benutzer melden, was gefunden wurde (Themes, Screens, Schriftart,
Begleitdateien und insbesondere vorhandene Audio-Dateien).

---

## Phase 2 — App-Ordner zuordnen

Der Skill deckt **beide** Faelle gleichwertig ab: bestehende App redesignen ODER neue App anlegen.

1. **Bestehende App suchen:** Aus dem Design-Ordner-Namen den App-Namen ableiten
   (z.B. "Fisetin Begleiter Design Update" → Suche nach App-Ordner "Fisetin*", "*Begleiter*").
   Nach Android-Projekten im Repo suchen (`Glob("**/settings.gradle*")`,
   `Glob("**/AndroidManifest.xml")`, Ordner mit `app/`+`build.gradle*`).
2. **Fund bestaetigen lassen:** "Ich ordne das Design der App `~/proggs/<AppOrdner>` zu
   (bestehendes Android-Projekt). Passt das? (ja / anderer / neue App anlegen)"
3. **Kein passender App-Ordner gefunden:** Fragen, ob eine **neue App** angelegt werden
   soll und unter welchem Ordner-/Package-Namen (→ Fall A). Fuer neue Apps ein sauberes
   Compose-Grundgeruest anlegen (siehe unten). Der Entwurf wird erst nach dem SPEC
   (Phase 4) hineingebaut.

**Neue App — Grundgeruest** (nur im Neu-Fall): Single-Module Compose-App, Kotlin,
Material 3, `MainActivity` + `NavHost`, Package aus dem bestaetigten Namen, moderne
Gradle-/Compose-Versionen (kurz die im Repo ueblichen Versionen anderer Apps als
Vorlage nehmen, damit es zum Rest passt). Wenn im Repo ein Android-Architektur-Skill
etabliert ist (z.B. android-clean-architecture / android-dev), dessen Struktur als
Geruest-Vorlage nutzen.

---

## Phase 3 — IST-Analyse der bestehenden App (nur wenn App existiert)

Damit die 1:1-Umsetzung sauber ins bestehende Projekt greift, zuerst den IST-Zustand lesen:

- Theme-Dateien: `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`, `Shape.kt` sowie
  `res/values/colors.xml`, `res/values-night/colors.xml`, `themes.xml`, `dimens.xml`.
- Alle Screen-Composables: `Glob("**/ui/**/*.kt")`, `Grep("@Composable")`.
- Navigation: NavHost/BottomBar/Routen.
- Schriftarten in `res/font/`, `build.gradle*` (Compose-/M3-Version).
- Bestehende App-Assets in `res/drawable*`, `res/raw`, `res/font`, `assets/` sowie ihre
  Aufrufer pruefen, damit gleichnamige Dateien und vorhandene Audio-Infrastruktur korrekt
  integriert statt doppelt angelegt werden.
- Feststellen: **Welche Screens/Funktionen aus dem Entwurf existieren schon**,
  welche fehlen (→ Phase 7). Welche Datei ist fuer welchen Design-Screen zustaendig.

Dokumentiere das Datei-Mapping: Design-Screen → zustaendige(r) Kotlin-Datei(en).

### Umsetzungs-Fall bestimmen (A / B / C) — steuert, ob ein SPEC geschrieben wird

Nach Phase 1 (Screen-Liste des Designs) und Phase 3 (was die App schon hat) steht der Fall fest:

| Fall | Situation | SPEC? (Phase 4) |
|------|-----------|-----------------|
| **P — Bau aus Spec-Paket** | Betriebsart P: `Specs/<App>/v2/` liegt vor | **NEIN** — das Spec-Paket **ist** das SPEC. Es wurde in Stufe 2 geschrieben und freigegeben. Kein zweites SPEC daneben schreiben. Direkt weiter zu Phase 5 |
| **A — Neue App** | Es existiert (noch) keine App zum Design | **JA** — immer ein vollstaendiges SPEC schreiben, dann bauen |
| **B — Redesign + neue Bereiche** | App existiert, aber der Entwurf enthaelt **neue** Screens/Funktionen, die es in der App noch nicht gibt | **JA** — SPEC fuer die **neuen Bereiche** schreiben; die restliche App nur optisch 1:1 angleichen |
| **C — Reine Design-Anpassung** | App existiert, der Entwurf bringt **keine** neuen Funktionen — nur die Optik aendert sich | **NEIN** — kein SPEC; das Design einfach komplett 1:1 in die App uebernehmen |

Den erkannten Fall dem Benutzer kurz nennen (z.B. "Fall B: App existiert, das Design
bringt 2 neue Bereiche (Timer, Statistik) — dafuer schreibe ich ein SPEC, den Rest
gleiche ich optisch 1:1 an."). Bei Unsicherheit, ob eine Design-Aenderung eine echte
**neue Funktion** oder nur **Optik** ist, im Zweifel als neuen Bereich (Fall B) behandeln
oder kurz nachfragen.

---

## Phase 4 — SPEC schreiben (bedingt)

**Wann:** Nur in **Fall A** (neue App) und **Fall B** (neue Bereiche). In **Fall C**
(reine Design-Anpassung) und in **Fall P** (Bau aus Spec-Paket) wird **kein** SPEC
geschrieben — direkt weiter zu Phase 5. In Fall P liegt das SPEC bereits als
`Specs/<App>/v2/` vor; ein zweites danebenzuschreiben erzeugt nur zwei Wahrheiten.

**Grundregel (vom Benutzer, absolut zentral):** Das SPEC **orientiert sich immer exakt
am Design** — Farben, Abstaende, Schriftgroessen, Anordnung, **alle** Effekte
(Gradients, Glow, Blur, Schatten, Animationen). Es beschreibt exakt das, was der Entwurf
zeigt, mit den EXAKTEN Werten aus Phase 1 — es erfindet keine Funktionen oder Optik dazu,
die nicht im Design stehen. Das SPEC ist die design-treue Bauanleitung, kein Freiraum.

**Wohin:** Als `SPEC.md` in den **Wurzelordner des App-Projekts**
(Fall A: das neu angelegte Projekt, z.B. `~/proggs/FisetinBegleiter/SPEC.md`;
Fall B: das bestehende Projekt). Bestehendes SPEC nicht blind ueberschreiben — bei
erneutem Lauf ergaenzen/aktualisieren.

**Inhalt des SPEC** (bei Fall B auf die neuen Bereiche fokussiert, ansonsten die ganze App):

1. **Uebersicht** — Zweck der App/des Bereichs, Bezug zum Design-Ordner (Pfad, Projektname).
2. **Design-Tokens** (1:1 aus Phase 1) — vollstaendige Tabellen:
   Farben je Theme (inkl. Alpha), Typo-Skala (Familie/Gewicht/Groesse/Zeilenhoehe/Spacing),
   Abstands-/Raster-Werte, Eckenradien, Schatten, Gradients, Blur/Glow.
3. **Effekte & Animationen** — jede `@keyframes`/`transition` mit Dauer/Easing/Wiederholung
   und wo sie im UI wirkt; geplante Compose-Entsprechung.
4. **Themes** — alle Theme-Varianten (Light/Dark + Zusatz-Themes) und der Umschalt-Mechanismus.
5. **Screen-fuer-Screen-Spezifikation** — pro Screen: Aufbau/Anordnung (exakt nach Entwurf),
   enthaltene Komponenten, Zustaende (leer/aktiv/Fehler/Ladephase), Navigation zu/von.
6. **Funktionen & Verhalten** — was jede (neue) Funktion tut; noetige Logik
   (State/ViewModel, Persistenz DataStore/Room), Datenmodell, Verdrahtung mit
   vorhandenen Daten (Fall B).
7. **Navigation** — Navigationsgraph / Routen (bei Fall B: Einhaengepunkt in die App).
8. **Offene Fragen** — alles, was aus dem Design nicht eindeutig ableitbar ist
   (statt zu raten, hier sammeln und den Benutzer fragen).
9. **Begleitdateien** — vollstaendiges Asset-/Daten-Mapping mit Quelle, Android-Zielpfad,
   Verwendungsstelle und bei Audio Trigger/Wiedergabeverhalten.

**Nach dem Schreiben:** Das SPEC dem Benutzer kurz vorstellen und **bestaetigen lassen**
(oder Korrekturen einarbeiten), bevor Phase 5/6 den Code baut. So wird design-treu
gebaut und du kannst vor dem Code eingreifen. Ist der Zweck einer neuen Funktion unklar,
hier gezielt fragen.

---

## Phase 5 — Mapping planen (Werte → Bausteine der Zielplattform)

Uebersetze die Werte aus Phase 1 in konkrete Bausteine der Zielplattform — mit **exakten
Werten**. Der folgende Abschnitt beschreibt **Android/Compose**; darunter stehen die
Entsprechungen fuer Windows und macOS. Die Werte sind auf allen drei Plattformen dieselben,
nur die Bausteine unterscheiden sich.

### Android — Kotlin / Jetpack Compose


**Theme-Schicht:**
- `Color.kt`: jede CSS-Variable → benannte `Color(0xFF……)`-Konstante. Alpha aus rgba
  in den Hex-Alpha-Kanal uebernehmen (z.B. `rgba(106,92,255,.13)` → `Color(0x216A5CFF)`;
  Alpha 0.13 ≈ 0x21). Rechne Alpha exakt: `round(alpha*255)` → 2-stellig hex.
- **Multi-Theme (alle Themes!):** Fuer JEDES gefundene Theme ein eigenes `ColorScheme`
  (bei einem Werft-Paket: fuer jeden Eintrag aus `erscheinungen[]` — die `id` wird der Name
  des Themes, die `tokens`-Tabelle sein vollstaendiger Farbsatz)
  (Light + Dark je Variante). Einen Theme-Enum + Umschalter (z.B. in Einstellungen /
  DataStore) anlegen, sodass der Nutzer wie im Entwurf zwischen den Themes wechseln kann.
  Die Design-Variablen (`--bg0/--bg1`-Gradient, `--card`, `--acc`, `--glow` …) auf
  M3-Rollen bzw. eigene Theme-Extension-Farben abbilden — lieber eine eigene
  `LocalAppColors`-CompositionLocal-Palette als die Design-Semantik in M3 zu verbiegen,
  wenn der Entwurf mehr Rollen hat als M3 (Gradient-Hintergrund, Glow, card-Transparenz).
- `Type.kt`: Schriftfamilie als `FontFamily` (Google-Font via `res/font` oder
  `androidx.compose.ui.text.googlefonts`), Typo-Skala mit den EXAKTEN sp-Werten
  (px des Entwurfs 1:1 als sp uebernehmen, sofern kein anderer Massstab vorgegeben ist).
- `Shape.kt`: Eckenradien exakt (px → dp).
- Ein `Spacing`/`Dimens`-Objekt fuer die realen Abstaende.

**Screen-Schicht:**
- Pro Design-Screen ein Composable, das die Anordnung des Entwurfs exakt spiegelt
  (gleiche Reihenfolge, gleiche Abstaende, gleiche Radien, gleiche Karten/Chips).
- Hintergrund-Gradient, Glow-Kreise (blur), Karten mit Transparenz/Border,
  Bottom-Navigation, Animationen — alles wie im Entwurf.

**Asset-Schicht:**
- Jede relevante Begleitdatei einem Android-Ziel (`res/raw`, `res/drawable`, `res/font`,
  `assets/` usw.) und ihrem konkreten Aufrufer zuordnen.
- Audio-Dateien unveraendert integrieren und Lifecycle, parallele Wiedergabe sowie
  Ressourcenfreigabe passend zur vorhandenen App-Architektur umsetzen.

### Windows — C# / .NET / WPF

**Theme-Schicht:**
- Je Erscheinung ein `ResourceDictionary` (`Themes/<Erscheinung>.xaml`) mit `SolidColorBrush`
  je Farbrolle — Name der Rolle als `x:Key`, Wert als exaktes `#AARRGGBB`. Alpha aus rgba
  exakt umrechnen: `round(alpha*255)` als zweistelliges Hex, vorangestellt.
- Umschaltung ueber `Application.Current.Resources.MergedDictionaries` — alle Erscheinungen
  sind umschaltbar, nicht nur die Standard-Erscheinung.
- Typografie als `Style`-Ressourcen je Rolle (`FontFamily`, `FontSize`, `FontWeight`,
  `LineHeight`, `TextBlock.LineStackingStrategy="BlockLineHeight"`). Schriftgroessen des
  Entwurfs sind px; WPF rechnet in geraeteunabhaengigen Einheiten (1/96 Zoll) — Werte
  1:1 uebernehmen, sofern das Spec nichts anderes sagt.
- Formen als `CornerRadius`-Ressourcen, Schatten ueber `DropShadowEffect`
  (`BlurRadius`, `ShadowDepth`, `Direction`, `Opacity`, `Color`) exakt nach Vorgabe.
- Verlaeufe als `LinearGradientBrush`/`RadialGradientBrush` mit den exakten Stops.

**Fenster-/Screen-Schicht:**
- Pro Bildschirm ein `UserControl` oder eine `Page`; Navigation ueber `Frame` bzw. einen
  `ContentControl` mit `DataTemplate`-Auswahl. Kein Bildschirm ohne Rueckweg.
- Abstaende als `Margin`/`Padding` exakt; Raster ueber `Grid`-Definitionen statt fester
  Positionen, wo der Entwurf mitwaechst.

**Bewegung:** `Storyboard` mit `DoubleAnimation`/`ColorAnimation`/`ThicknessAnimation`,
Dauer als `Duration="0:0:0.240"`, Kurve als `KeySpline` einer `SplineDoubleKeyFrame`
(`cubic-bezier(x1,y1,x2,y2)` → `KeySpline="x1,y1 x2,y2"`). Dauerbewegung ueber
`RepeatBehavior="Forever"` und `AutoReverse` gemaess Vorgabe.

### macOS — Swift / SwiftUI

**Theme-Schicht:**
- Je Erscheinung eine Farbtabelle (Asset-Katalog oder `Color`-Extension) mit den exakten
  Werten; Auswahl ueber einen Theme-Wert in der `Environment`, damit **alle** Erscheinungen
  umschaltbar sind — nicht nur ueber `colorScheme`.
- Typografie ueber `Font.custom(_:size:)` mit exakter Familie, `.weight()`, Zeilenhoehe
  ueber `.lineSpacing()` (Differenz zur Schriftgroesse beachten) und `.tracking()`.
- Formen ueber `RoundedRectangle(cornerRadius:)`, Schatten ueber
  `.shadow(color:radius:x:y:)` — `radius` entspricht dem halben CSS-Blur-Radius,
  das ausdruecklich vermerken statt still zu uebernehmen.
- Verlaeufe ueber `LinearGradient`/`RadialGradient` mit exakten Stops,
  Weichzeichner ueber `.blur(radius:)`.

**Screen-Schicht:**
- Pro Bildschirm eine `View`; Navigation ueber `NavigationStack`/`NavigationSplitView`
  je nach Entwurf. Abstaende exakt als `padding`/`spacing`.

**Bewegung:** `withAnimation(.timingCurve(x1,y1,x2,y2, duration: 0.240))` bzw.
`.animation(_:value:)`; Dauerbewegung ueber `.repeatForever(autoreverses:)`.

### Danach

Kurz den Umsetzungsplan (welche Dateien neu/geaendert) auflisten, dann umsetzen.

---

## Phase 6 — 1:1-Umsetzung (Code schreiben)

Jetzt wird **tatsaechlich Code geschrieben** — dies ist kein Vorschlags-Skill, sondern
setzt direkt um. Schreibe/aendere die Kotlin-Dateien so, dass die App am Ende exakt
wie der Entwurf aussieht. In Fall A/B **gemaess dem bestaetigten SPEC** (Phase 4).

**Reihenfolge:**
1. Relevante Begleitdateien unveraendert in die geplanten Android-Ressourcen uebernehmen.
2. Theme-Dateien (`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`, ggf. `AppColors.kt`
   Extension + Theme-Umschalter) — das Fundament zuerst.
3. Wiederverwendbare Komponenten (Karte, Chip, Nav-Item, Button, Statistik-Kachel …)
   exakt nach Entwurf.
4. Screen fuer Screen umsetzen/ersetzen — jeweils gegen die Token-Liste pruefen.
5. Navigation/Struktur sowie alle Asset-Aufrufer verdrahten (inkl. neuer Screens aus
   Phase 7 und vorhandener Audio-Trigger).

**Treue-Regeln (was "1 zu 1" bedeutet):**
- Farben: exakte Hex/Alpha-Werte, keine "aehnliche" Farbe.
- Abstaende/Radien: exakte px→dp-Werte, kein Aufrunden auf "schoenere" Zahlen.
- Schrift: exakte Familie, Gewichte, Groessen, Zeilenhoehen, letter-spacing.
- Anordnung: gleiche Reihenfolge, Ausrichtung, Verschachtelung wie im Entwurf.
- Gradients, Schatten, Blur, Glow, Transparenzen: exakt nachbauen.
- Animationen: gleiche Dauer/Easing/Wiederholung.
- Dark Mode und alle Zusatz-Themes: vollstaendig, nicht nur das Basis-Theme.
- Texte/Labels aus dem Entwurf uebernehmen (sofern nicht klar Platzhalter/Lorem).

Wenn ein Design-Detail technisch in Compose nicht 1:1 geht (sehr selten), die
**naechstliegende** Umsetzung waehlen und das dem Benutzer am Ende offen benennen —
niemals still vereinfachen.

---

## Phase 7 — Funktionen bauen

### Fall P — das ganze Verhalten aus dem Funktions-Spec

In Betriebsart P ist die Oberflaeche nur die Haelfte des Auftrags. `01-FUNKTIONS-SPEC.md`
wird **vollstaendig** umgesetzt — jede `F-`-Kennung, nicht nur die, die im Design sichtbar
sind. Je Funktion:

1. **Ablauf** genau in der beschriebenen Schrittfolge umsetzen — keine Schritte
   zusammenfassen, keine umsortieren.
2. **Datenmodell** aus §3 anlegen (Android: Room/DataStore; Windows: die im Repo uebliche
   Datenschicht, sonst SQLite/JSON je nach Spec; macOS: SwiftData/`UserDefaults` je nach
   Spec). Feldnamen, Typen, Pflichtfelder und Standardwerte exakt wie spezifiziert.
3. **Zustaende und Uebergaenge** aus §4 als echten Zustandsautomaten bauen, nicht als lose
   Boolean-Flags, wenn das Spec mehr als zwei Zustaende nennt.
4. **Externe Dienste** aus §5 anbinden, inklusive des beschriebenen Verhaltens ohne Netz.
   Zugangsschluessel niemals in den Quellcode schreiben.
5. **Hintergrund und Lebenszyklus** aus §6 umsetzen — was weiterlaufen soll, laeuft weiter.
6. **Fehlerfaelle** genau so behandeln, wie sie dastehen. Keine zusaetzliche
   Fehlerbehandlung fuer Faelle erfinden, die das Spec nicht nennt.

Eine Funktion gilt erst als gebaut, wenn sie **beobachtbar** tut, was das Spec sagt —
nicht, wenn der Bildschirm dazu existiert.

### Fall A/B/C — Neue Funktionen aus dem Design mit-implementieren

**Sehr wichtig (ausdruecklicher Wunsch des Benutzers):** Wenn der Entwurf Funktionen
oder Screens enthaelt, die es in der bestehenden App **noch gar nicht gibt** (Fall A/B),
werden diese **mit-programmiert** — nicht nur optisch als Attrappe, sondern funktional,
**gemaess dem SPEC** aus Phase 4.

Vorgehen:
1. Aus Phase 1 (G) und Phase 3 abgleichen: Welche Screens/Funktionen im Entwurf haben
   **keine** Entsprechung in der App?
2. Jede neue Funktion voll umsetzen: UI (nach Entwurf) **plus** die noetige Logik
   (State/ViewModel, ggf. Persistenz via DataStore/Room, Navigation dorthin,
   Verdrahtung mit bestehenden Daten wo sinnvoll).
3. Wenn der Zweck einer neuen Funktion aus dem Entwurf nicht eindeutig ableitbar ist,
   den Benutzer gezielt fragen, statt zu raten.
4. Neue Funktionen sauber in die bestehende Architektur einfuegen (gleiche Muster wie
   der Rest der App: gleiche DI, gleiche Navigation, gleiche Repository-Struktur).

Liste am Ende explizit auf, welche NEUEN Funktionen aus dem Design hinzugekommen sind.

---

## Phase 8 — Verifikation

- **Build:** Projekt kompilieren (Gradle) — bei Fehlern beheben, bis es sauber baut.
  Der `resilient-bugfixing`-Skill gilt fuer jeden dabei auftretenden Fehler.
- **Visueller Abgleich (Pflicht, nicht optional):** Die gebaute Software starten und
  **je Bildschirm und je Erscheinung** einen Screenshot machen. Jeden Screenshot gegen die
  zugehoerige Design-Datei halten:
  `WERFT-DESIGN/bildschirme/<erscheinung>/<nr>-<name>.html` (bei einem Claude-Design gegen
  die `.dc.html` bzw. `.thumbnail`). Abweichungen bei Farbe, Abstand, Groesse, Schrift und
  Anordnung werden korrigiert und **erneut** verglichen, bis es passt.
  Fuer den Ablauf gilt der `screenshot-loop`-Skill.
  - Android: ueber ADB auf dem angeschlossenen Geraet bzw. Emulator.
  - Windows/macOS: das gestartete Fenster aufnehmen.
  - Ist kein Geraet erreichbar, wird das **ausdruecklich gemeldet** — dann gilt die
    Umsetzung als optisch **ungeprueft**, nicht als fertig.
- **Selbstpruefung gegen die Token-Liste:** Jede Farbe/Groesse/Schrift/Animation aus
  Phase 1 einmal gegen den geschriebenen Code gegenpruefen — nichts vergessen?
- **Asset-Abgleich:** Jeden Eintrag des Asset-/Daten-Inventars gegen Zielpfad und Aufrufer
  pruefen. Audio auf dem Zielgeraet tatsaechlich ausloesen und Wiedergabe, Lautstaerke,
  Loop-/Stopp-Verhalten sowie Lifecycle pruefen.
- **Vollstaendigkeits-Abgleich (100% — Pflicht):** Das komplette Screen-/Navigations-/
  Untermenue-Inventar aus Phase 1 (E2) Punkt fuer Punkt gegen den Code abhaken:
  - Ist **jeder** Bildschirm und Unterbildschirm umgesetzt? (keiner fehlt)
  - Ist **jedes** Untermenue / jeder Tab / jeder Dialog / jedes Bottom-Sheet da?
  - Fuehrt **jede** Verknuepfung/Navigation genau dorthin wie im Entwurf? (keine toten
    Buttons, keine Sackgassen, kein Platzhalter)
  - Sind **alle** Zustaende (leer/aktiv/ausgewaehlt/Fehler/Ladephase) vorhanden?
  - Sind **alle** Themes und **alle** Effekte/Animationen umgesetzt?
  - Bei einem Werft-Paket: ist **jeder** Bildschirm aus `design-tokens.json` in **jeder**
    Erscheinung nachgebaut? Die Tabelle in `DESIGN-SPEC.md` ist die Abhakliste — jede Zeile
    mal jede Erscheinung.
  - Sind **alle** relevanten Begleitdateien vorhanden und funktional verdrahtet?
  Fehlt auch nur EIN Punkt, ist die Umsetzung **nicht fertig** — nachziehen, bis das
  Inventar zu 100% abgehakt ist. Am Ende die abgehakte Inventarliste kurz berichten.

### Zusaetzlich in Fall P — die Abhakliste aus dem Bau-Auftrag

In Betriebsart P tritt die Liste aus `BAU-AUFTRAG.md` §4 an die Stelle des selbst
erstellten Inventars. Jede Kennung wird einzeln abgehakt, und zwar mit Fundstelle im Code:

| Kennung | Nachweis, der zu erbringen ist |
|---------|-------------------------------|
| `B-xx` | Der Bildschirm existiert, ist erreichbar, sieht in **jeder** Erscheinung aus wie in `02-UI-SPEC.md` beschrieben, und alle dort genannten Zustaende (leer/laedt/Fehler/aktiv) sind gebaut |
| `M-xx` | Die Bewegung laeuft, mit **exakt** der Dauer, Kurve, Verzoegerung und Wiederholung aus `03-MOTION-SPEC.md`. Eine Standardkurve statt der angegebenen `cubic-bezier` gilt als nicht erfuellt |
| `F-xx` | Die Funktion tut **beobachtbar**, was `01-FUNKTIONS-SPEC.md` beschreibt — Ablauf, gespeicherte Daten, Ergebnis, Fehlerfall. Ein Bildschirm allein ist kein Nachweis |
| `A-xx` | Das Abnahmekriterium aus `00-PROJEKT.md` ist durchgespielt und erfuellt |

**Kein toter Knopf.** Zum Schluss jedes Bedienelement einmal durchgehen: Es fuehrt entweder
zu einem Bildschirm (`B-`) oder loest eine Funktion (`F-`) aus. Ein Knopf, der nur gut
aussieht, ist ein Fehler — kein "spaeter".

Die drei Ergebnisse, an denen der Bau gemessen wird, ausdruecklich berichten:
1. Das Programm **sieht aus** wie `02-UI-SPEC.md`.
2. Es **bewegt sich** wie `03-MOTION-SPEC.md`.
3. Es **funktioniert** wie `01-FUNKTIONS-SPEC.md` — das Design ist voll funktionstuechtig.


**Projekt-Konventionen beachten (aus der globalen CLAUDE.md):** Nach der Umsetzung
die sichtbare App-Version mit Zeitstempel bumpen. Committen/Pushen und der finale
App-Build/Install/Deploy erfolgen gemaess der uebergeordneten Aufgaben-Regel
(nicht der Skill entscheidet darueber, sondern der laufende Auftrag).

---

## Was NIEMALS passieren darf

- ❌ Nur einen TEIL des Designs umsetzen — es wird zu **100%** umgesetzt: jeder Bildschirm,
  jeder Unterbildschirm, jedes Untermenue, jede Verknuepfung, jeder Zustand, jedes Detail.
- ❌ Einen Screen/Dialog/Tab/ein Untermenue auslassen, weil es "unwichtig" oder versteckt wirkt.
- ❌ Buttons/Nav-Items ohne funktionierendes Ziel lassen (tote Verknuepfung, Sackgasse, Platzhalter).
- ❌ Das Design "interpretieren" oder eigenmaechtig "verbessern" — es wird 1:1 umgesetzt.
- ❌ Farben/Abstaende/Groessen schaetzen oder runden — immer die exakten Werte aus der Datei.
- ❌ Nur das Basis-Theme umsetzen und Dark Mode / Zusatz-Themes weglassen — ALLE Themes.
- ❌ Animationen, Gradients, Glow, Blur oder Transparenzen weglassen, weil "aufwendig".
- ❌ Neue Design-Funktionen nur optisch nachbauen statt sie funktional zu programmieren.
- ❌ Ein SPEC schreiben, das sich NICHT exakt am Design orientiert oder Dinge dazuerfindet.
- ❌ Bei neuer App (Fall A) oder neuen Bereichen (Fall B) ohne SPEC direkt losbauen.
- ❌ Bei reiner Design-Anpassung (Fall C) unnoetig ein SPEC schreiben — dort einfach 1:1 uebernehmen.
- ❌ `support.js` als Design-Quelle lesen (ist nur Runtime) — Quelle ist die `.dc.html`.
- ❌ Die `.dc.html` nur teilweise lesen — sie wird vollstaendig gelesen.
- ❌ Nur die bekannten Standarddateien pruefen — der gesamte Projekt-Unterordner wird
  rekursiv inventarisiert, inklusive unbekannter oder tief verschachtelter Dateien.
- ❌ Audio/Bilder/Fonts/Daten nur erwaehnen oder durch Ersatzdateien austauschen — die
  mitgelieferten Originaldateien werden unveraendert und funktional passend integriert.
- ❌ Fuer eine unklare Audio- oder Asset-Verwendung eigenmaechtig einen Trigger erfinden.
- ❌ Bei unklarem Design-Ordner einfach irgendeinen nehmen — bestaetigen lassen bzw. fragen.
- ❌ Ein Design-Detail still vereinfachen — technische Grenzen offen benennen.
- ❌ Ins falsche App-Projekt schreiben — App-Zuordnung immer bestaetigen lassen.
- ❌ In Betriebsart P ein zweites SPEC schreiben — `Specs/<App>/v2/` **ist** das SPEC.
- ❌ In Betriebsart P nur die Oberflaeche bauen und das Funktions-Spec liegenlassen —
  das Design muss am Ende **funktionstuechtig** sein, nicht nur richtig aussehen.
- ❌ Eine `cubic-bezier`-Kurve aus dem Motion-Spec durch eine eingebaute Standardkurve
  (`FastOutSlowIn`, `.easeInOut`, `CubicEase`) ersetzen.
- ❌ Die Zielplattform raten — sie steht in `00-PROJEKT.md` §2 bzw. im Spec-Paket.
- ❌ Auf einer anderen Plattform bauen als der spezifizierten, weil sie "naeher liegt".
- ❌ In `Designs/Inbox/` nach einem Design suchen — dort liegt der Hinweg zum Designer.
- ❌ Aus einer losen `*-SPEC-v*`-Datei in `Outbox/` direkt bauen, ohne dass
  `spec-rueckimport` daraus `Specs/<App>/v2/` gemacht hat.
- ❌ Einen Bau-Auftrag losbauen, in dem noch offene Fragen (§5) stehen.
- ❌ "Fertig" melden, ohne jeden Bildschirm in jeder Erscheinung per Screenshot gegen das
  Design gehalten zu haben — "sieht aus wie das Design" ist sonst nur eine Behauptung.
- ❌ In einem Lauf fuer mehrere Zielsysteme zugleich bauen: ein Spec, ein System, ein Lauf.

---

## Referenz-Dateien

| Referenz | Wann lesen | Inhalt |
|----------|-----------|--------|
| [references/design-format.md](references/design-format.md) | Zu Beginn von Phase 1 | Aufbau der Claude-Design-Dateien: `.dc.html` (`<x-dc>`, `<helmet>`, `data-t`-Themes, `{{ }}`-Platzhalter, Props), `android-frame.jsx`, `support.js`, `.thumbnail` — und wo genau welche Design-Werte stehen |

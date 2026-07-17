---
name: design-umsetzer
description: >-
  Setzt einen mit Claude Designs erstellten und im Repo unter Designs/ gespeicherten
  Design-Entwurf 1:1 exakt in Jetpack Compose um — Farben, Abstaende, Schriftgroessen,
  Schriftarten, Eckenradien, Schatten, Anordnung und Animationen exakt aus den
  Design-Dateien (.dc.html, android-frame.jsx, support.js, .thumbnail). Uebernimmt ALLE
  im Design enthaltenen Theme-Varianten (Light/Dark und Zusatz-Themes) als umschaltbare
  Themes. Setzt sowohl bestehende Apps neu um (Redesign vorhandener Screens) als auch
  komplett neue Apps aus dem Design. Programmiert Funktionen, die NEU im Design dazugekommen
  sind und in der App noch fehlen, mit ein. Bei einer neuen App oder bei neuen Bereichen
  schreibt er zuerst ein design-treues SPEC (SPEC.md im App-Projekt) und baut danach; bei
  reiner Design-Anpassung einer bestehenden App uebernimmt er das Design direkt 1:1 ohne SPEC.
  Findet der Skill den passenden Design-Ordner
  nicht automatisch, fragt er den Benutzer nach dem Pfad. Nutze diesen Skill IMMER wenn der
  Benutzer sagt "Design umsetzen", "Design-Umsetzer", "setze das Design um", "setze mein
  Cloud-Design um", "Design aus dem Designs-Ordner umsetzen", "mach das Design in die App",
  "Design 1 zu 1 umsetzen", "Design in Jetpack Compose umsetzen", "uebernimm das Design",
  "setze den Design-Entwurf um", "implementiere das Design", "/design-umsetzer", oder
  irgendetwas das damit zu tun hat einen fertigen Design-Entwurf (Claude Designs / Cloud
  Designs) exakt in eine Android-App umzusetzen. Auch bei "bau das Design nach" oder
  "setze das Prototyp-Design in die App um".
---

# Design-Umsetzer: Claude-Design 1:1 in Jetpack Compose umsetzen

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

---

## Aufruf

Der Benutzer sagt z.B.:
- "Setze das Design um" / "Design-Umsetzer starten" / "/design-umsetzer"
- "Setze das Fisetin-Design 1 zu 1 um"
- "Uebernimm das Cloud-Design in die App"

Ein Parameter (App- oder Design-Name) kann angegeben sein, muss aber nicht.
Fehlt er, ermittelst du den Design-Ordner in Phase 0 selbst.

---

## Ueberblick der Phasen (in dieser Reihenfolge)

0. **Design-Ordner finden** (raten + bestaetigen lassen; sonst Pfad erfragen)
1. **Design vollstaendig einlesen** und alle Design-Tokens exakt extrahieren
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

## Phase 0 — Design-Ordner finden

**Feste Ordner-Struktur (wichtig):** Der Standard-Container fuer ALLE Design-Entwuerfe
ist immer der Ordner **`~/proggs/Designs/`** selbst. Er ist kein Design-Ordner, sondern
nur die Sammlung. **Jedes einzelne Designprojekt liegt als eigener Unterordner darin**
— z.B. `~/proggs/Designs/Fisetin-Begleiter-Design-Update/`. Der Unterordner ist das,
was 1:1 umgesetzt wird. Ordnernamen koennen Bindestriche ODER Leerzeichen enthalten.

Im `Designs/`-Root liegt ausserdem eine **`README.md`** (Index, listet die Projekte auf).
Diese README ist **kein** Designprojekt — sie NIE als Design-Ordner behandeln, aber ihren
Inhalt gerne lesen, um die vorhandenen Projekte/Namen zu erkennen.

Ein Projekt-Unterordner enthaelt typischerweise:

| Datei | Inhalt | Rolle fuer die Umsetzung |
|-------|--------|--------------------------|
| `*.dc.html` | Der komplette Design-Prototyp (HTML/CSS/Handlebars) | **PRIMAERE Quelle** — hier stehen ALLE exakten Werte |
| `android-frame.jsx` | Material-3-Geraeterahmen (Statusbar, AppBar, NavBar) | Referenz fuer M3-Kontext, NICHT selbst nachbauen |
| `support.js` | Generierte React-Runtime | **Ignorieren** (nur Rendering-Maschinerie, kein Design) |
| `.thumbnail` | Vorschaubild des Designs (**optional**, nicht immer vorhanden) | Falls da: visueller Gesamteindruck / Abgleich |

**Ablauf:**

1. `Glob("Designs/*")` bzw. `ls ~/proggs/Designs/` — alle **Unterordner** auflisten
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

Erst weitermachen, wenn der Projekt-Unterordner feststeht.

---

## Phase 1 — Design vollstaendig einlesen und Tokens extrahieren

**Grundsatz:** Wer den Entwurf nicht komplett gelesen hat, kann ihn nicht 1:1 umsetzen.
Lies die `*.dc.html` **vollstaendig** (sie kann gross sein, 50-100 KB — trotzdem ganz lesen,
notfalls in mehreren Read-Baecken mit `offset`/`limit`). Lies zusaetzlich `android-frame.jsx`
und sieh dir `.thumbnail` visuell an, **falls vorhanden** (Read-Tool ist multimodal;
die Datei fehlt in manchen Projekten — dann ohne sie weiterarbeiten). `support.js` NICHT lesen.

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

**F) Animationen & Motion**
- Alle `@keyframes` (Name + Verlauf), `animation:`-Kurzformen (Dauer, Easing, Delay,
  iteration), `transition:`-Angaben. Diese in Compose mit `animate*AsState`,
  `AnimatedVisibility`, `updateTransition`, `infiniteRepeatable` etc. nachbilden.

**G) Inhalte & Funktionen**
- Welche Screens/Funktionen zeigt der Entwurf (z.B. Dashboard, Detail, Einstellungen,
  Onboarding, Timer, Statistik …)? Liste sie auf — Basis fuer Phase 7 (neue Funktionen).

Am Ende von Phase 1 hast du eine vollstaendige, exakte Token- und Screen-Liste.
Kurz an den Benutzer melden, was gefunden wurde (Themes, Screens, Schriftart).

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
- Feststellen: **Welche Screens/Funktionen aus dem Entwurf existieren schon**,
  welche fehlen (→ Phase 7). Welche Datei ist fuer welchen Design-Screen zustaendig.

Dokumentiere das Datei-Mapping: Design-Screen → zustaendige(r) Kotlin-Datei(en).

### Umsetzungs-Fall bestimmen (A / B / C) — steuert, ob ein SPEC geschrieben wird

Nach Phase 1 (Screen-Liste des Designs) und Phase 3 (was die App schon hat) steht der Fall fest:

| Fall | Situation | SPEC? (Phase 4) |
|------|-----------|-----------------|
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
(reine Design-Anpassung) wird **kein** SPEC geschrieben — direkt weiter zu Phase 5.

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

**Nach dem Schreiben:** Das SPEC dem Benutzer kurz vorstellen und **bestaetigen lassen**
(oder Korrekturen einarbeiten), bevor Phase 5/6 den Code baut. So wird design-treu
gebaut und du kannst vor dem Code eingreifen. Ist der Zweck einer neuen Funktion unklar,
hier gezielt fragen.

---

## Phase 5 — Mapping planen (Design → Compose)

Uebersetze die Tokens aus Phase 1 in konkrete Compose-Bausteine — mit **exakten Werten**:

**Theme-Schicht:**
- `Color.kt`: jede CSS-Variable → benannte `Color(0xFF……)`-Konstante. Alpha aus rgba
  in den Hex-Alpha-Kanal uebernehmen (z.B. `rgba(106,92,255,.13)` → `Color(0x216A5CFF)`;
  Alpha 0.13 ≈ 0x21). Rechne Alpha exakt: `round(alpha*255)` → 2-stellig hex.
- **Multi-Theme (alle Themes!):** Fuer JEDES gefundene Theme ein eigenes `ColorScheme`
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

Kurz den Umsetzungsplan (welche Dateien neu/geaendert) auflisten, dann umsetzen.

---

## Phase 6 — 1:1-Umsetzung in Jetpack Compose (Code schreiben)

Jetzt wird **tatsaechlich Code geschrieben** — dies ist kein Vorschlags-Skill, sondern
setzt direkt um. Schreibe/aendere die Kotlin-Dateien so, dass die App am Ende exakt
wie der Entwurf aussieht. In Fall A/B **gemaess dem bestaetigten SPEC** (Phase 4).

**Reihenfolge:**
1. Theme-Dateien (`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`, ggf. `AppColors.kt`
   Extension + Theme-Umschalter) — das Fundament zuerst.
2. Wiederverwendbare Komponenten (Karte, Chip, Nav-Item, Button, Statistik-Kachel …)
   exakt nach Entwurf.
3. Screen fuer Screen umsetzen/ersetzen — jeweils gegen die Token-Liste pruefen.
4. Navigation/Struktur verdrahten (inkl. neuer Screens aus Phase 7).

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

## Phase 7 — Neue Funktionen aus dem Design mit-implementieren

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
- **Visueller Abgleich:** Wenn ein Geraet/Emulator per ADB verbunden ist, App starten
  und Screenshots gegen `.thumbnail` bzw. den Entwurf halten. Abweichungen (Farbe,
  Abstand, Anordnung) korrigieren, bis es zum Entwurf passt.
- **Selbstpruefung gegen die Token-Liste:** Jede Farbe/Groesse/Schrift/Animation aus
  Phase 1 einmal gegen den geschriebenen Code gegenpruefen — nichts vergessen?

**Projekt-Konventionen beachten (aus der globalen CLAUDE.md):** Nach der Umsetzung
die sichtbare App-Version mit Zeitstempel bumpen. Committen/Pushen und der finale
App-Build/Install/Deploy erfolgen gemaess der uebergeordneten Aufgaben-Regel
(nicht der Skill entscheidet darueber, sondern der laufende Auftrag).

---

## Was NIEMALS passieren darf

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
- ❌ Bei unklarem Design-Ordner einfach irgendeinen nehmen — bestaetigen lassen bzw. fragen.
- ❌ Ein Design-Detail still vereinfachen — technische Grenzen offen benennen.
- ❌ Ins falsche App-Projekt schreiben — App-Zuordnung immer bestaetigen lassen.

---

## Referenz-Dateien

| Referenz | Wann lesen | Inhalt |
|----------|-----------|--------|
| [references/design-format.md](references/design-format.md) | Zu Beginn von Phase 1 | Aufbau der Claude-Design-Dateien: `.dc.html` (`<x-dc>`, `<helmet>`, `data-t`-Themes, `{{ }}`-Platzhalter, Props), `android-frame.jsx`, `support.js`, `.thumbnail` — und wo genau welche Design-Werte stehen |

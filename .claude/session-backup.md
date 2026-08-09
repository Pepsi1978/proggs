# Session Handoff — 09.08.2026, ca. 23:15 Uhr

## Ziel
Die Programm-Pipeline (`neue-applikation`) so umbauen, dass ein Werft-Design zu **100 %**
in die Specs und von dort zu **100 %** in die gebaute App gelangt — inklusive aller
Spezialeffekte. Danach die App "Experimente" damit neu bauen. Frank hat ausdrücklich
gesagt: 99,9 % sind inakzeptabel.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt

- **Welche Aufgabe lief gerade:** Pipeline-Umbau, Block A/B teilweise fertig.
  Als Nächstes: **Block A "Vollständigkeits-Tor"** in `spec-rueckimport` und
  **Block B "Rezeptbuch CSS→Compose"** in `design-umsetzer/references/`.

- **Was BEREITS erledigt und gespiegelt ist (alle vier Profile identisch, md5 geprüft):**
  - `spec-rueckimport/SKILL.md`: Phase 2 "Gestaltung" umgeschrieben — **Messung schlägt
    Prosa**, widersprechende v1-Sätze werden GESTRICHEN statt danebengestellt; Warnung vor
    gestaffelter CSS (`.werft-screen[data-screen-id=…]`-Schicht ist verbindlich).
    Phase 1b Schritte 6-8 neu: Design als PNG rendern (headless Chrome, Rezept steht drin),
    Aufbau je Bildschirm aus dem Bild beschreiben (über/neben, Karte ja/nein, Breite,
    schwebende Leiste, Pille), Effekte den Bauteilen ZUORDNEN.
  - `design-umsetzer/SKILL.md`: Phase 8 Bildabgleich umgeschrieben — **Bild gegen Bild**
    (nicht gegen HTML), **je Bildschirm sofort** vor dem nächsten, Emulator wenn kein Gerät,
    "grüner Build ist keine Abnahme". Android-Mapping ergänzt: Schriften als `res/font/*.ttf`
    EINBETTEN (nicht googlefonts), Tiefen-Schicht (mehrlagige box-shadow, inset-Lichtsaum als
    1 dp Linie, backdrop-filter → RenderEffect ab API 31, color-mix → lineare Mischung).
    6 neue ❌-Regeln. Fehlender Abschnitt "Pflicht-Inventar aller Begleitdateien" aus
    standard nach minimal übernommen; danach alle vier Kopien identisch.

- **Noch offen:** Block A-Tor, Block B-Rezeptbuch, Block C (`spec-schmiede`: v1-Sätze als
  "Absicht vor dem Design" kennzeichnen), Block D (`neue-applikation`: Abnahme-Tor +
  Logbuch-Pflicht), Block E (v2-Spec neu erzeugen + App neu bauen).

## Der Kernbefund (nicht noch einmal herleiten)
Die App sieht dem Design nicht ähnlich, weil beim Bau die **Tiefen-Schicht** fehlte.
`WERFT-DESIGN/bildschirme/design.css` hat ZWEI Schichten:
1. Zeilen ~880-4200: flache Grundschicht.
2. Zeilen ~4200-5600: auf `.werft-screen[data-screen-id="B-xx"]` eingeschränkte Schicht mit
   Verläufen, Schatten (`--werft-schatten-*` ab Zeile 32), radialen Auren, schwebender
   unterer Leiste (right/bottom/left 12px, Höhe 64px, Radius 24px) und Pille hinter dem
   aktiven Feld (margin 4px, radius 20px, background Aktion-gedeckt).
**Schicht 2 ist verbindlich** — sie ist das, was Werft zeigt, und `DESIGN-SPEC.md` des
Designers führt diese Effekte als gemessene Design-Werte.

Wichtige Korrektur an einer früheren Fehlaussage von mir: Der v1-Satz „Keine Schatten,
keine Verläufe" steht **NICHT** in v2 (nur in `Specs/Experimente/v1/02-UI-SPEC.md:113`).
Der echte v2-Defekt ist: Effekte stehen in §5 als namenloser Anhang
(`design.html:gradient(5) = …`) OHNE Bauteil-Zuordnung und OHNE Bild → werden übersprungen.

## Belegte Fakten (nicht neu prüfen)
- `Designs/Outbox/Experimente-SPEC-v1-SPEC-v2.zip` == `Designs/Outbox/Experimente/`
  (`diff -rq`, 30 Dateien, kein Unterschied). Werft hat sauber geliefert.
- Der Werft-Server-Zugang (`~/SK/werft-studio/login.txt`, Caddy-Cert) ist NICHT der Engpass —
  das ZIP enthält das Design bereits vollständig.
- Gerät: Samsung SM-F971B (Z Fold), `adb devices` → R3GL7073MLM. Display-ID für Screenshots:
  `adb exec-out screencap -p -d 4630947123231501204` (das Gerät hat zwei Displays; ohne `-d`
  landet eine Warnzeile im PNG und macht es unlesbar).
- Schriften laden doch (FontLog zeigt erfolgreichen Fetch) — aber verzögert. Deshalb
  einbetten statt herunterladen.
- Design-Bilder aller neun Bildschirme (dunkel) bereits gerendert nach
  `<scratchpad>/design/D-*.png`. Rezept: HTML kopieren, `</head>` ersetzen durch
  `<style>.werft-screens{width:412px!important}.werft-screen{width:412px!important;
  height:915px!important;overflow:hidden!important}</style></head>`, CSS-Pfad absolut machen,
  dann `chrome.exe --headless=new --force-device-scale-factor=2 --window-size=412,915
  --screenshot=…`.

## Fehlgeschlagene Ansätze — NICHT wiederholen
- **Aus den Zahlen des UI-Specs bauen statt aus dem Bild.** Genau so ging die Tiefen-Schicht
  verloren. Immer das gerenderte Design ansehen.
- **`adb exec-out screencap -p` ohne `-d <display-id>`** auf dem Fold: die Warnzeile
  "Multiple displays were found" landet vor den PNG-Bytes.
- **Pfade wie `/data/local/tmp/x.png` im Bash-Tool** werden von Git Bash in Windows-Pfade
  umgeschrieben (`C:/Program Files/Git/data/...`). `adb exec-out … > datei` benutzen.
- **`sed` auf `VERSION_BUMPED_AT`** mit dem Muster `"09.08.2026, …"`: die Datei enthält
  escapte Anführungszeichen (`\"…\"`), nur auf den Datumsteil matchen.
- **Zwei `fillMaxSize`-Kinder in einer `Column`** (Inhalt + FAB): das zweite bekommt Höhe 0.
  Kompiliert grün, ist zur Laufzeit unsichtbar.

## Stand der App "Experimente"
- Gebaut, installiert (v0.2.1, versionCode 3), läuft, alle 9 Bildschirme + Navigation da,
  Funktionen dahinter gebaut. **Aber: Optik entspricht dem Design NICHT** (flach statt
  Tiefen-Schicht; B-08 hat zusätzlich die falsche Anordnung — Beschriftung neben statt über
  dem Feld, Abschnitte sind keine Karten).
- `ui/theme/Effekte.kt` ist ein ANGEFANGENER, noch nicht kompilierter Entwurf der
  Tiefen-Schicht (Schatten-Token, Verlauf, Auren). Er ist NICHT committet und muss beim
  Neubau gegen das Bild geprüft werden.
- Insets waren komplett vergessen (Inhalt lief unter Statusleiste/Gestenbalken) — behoben in
  `ui/Navigation.kt` per `windowInsetsPadding(WindowInsets.safeDrawing)`.

## Nächste Schritte (priorisiert)
1. Block A: Vollständigkeits-Tor in `spec-rueckimport` (jeder CSS-Effekt braucht Besitzer).
2. Block B: `design-umsetzer/references/css-nach-compose.md` — Rezeptbuch für box-shadow
   (mehrlagig), inset-Lichtsaum, Verläufe, color-mix, backdrop-filter, schwebende Leisten.
3. Block C: `spec-schmiede` — v1-Gestaltungssätze als Absicht kennzeichnen.
4. Block D: `neue-applikation` — Abnahme-Tor und Logbuch-Pflicht.
5. Alle Änderungen in ALLE VIER Profile spiegeln (minimal/standard/strict/opencode-setup)
   und md5 gegenprüfen. Es sind echte Kopien, keine Junctions.
6. Block E: `Specs/Experimente/v2` neu erzeugen, dann App Bildschirm für Bildschirm neu
   bauen — jeder Bildschirm gegen sein Design-Bild abgenommen, BEVOR der nächste beginnt.

## Offene Fragen
- Keine offene Rückfrage an Frank. Er hat entschieden: erst Skills reparieren, dann Stufe 3
  neu. Und: 100 % Deckung mit dem Design, keine Abstriche.

## Anker
- Branch: main
- Letzte Commits:
bae4397e6 Experimente: Oberflaeche, Navigation und Build
03981fd69 session restore: clear handoff backup
4c23fc69b session backup: handoff snapshot (Experimente, Stufe 3 Block 5)
1f4746b7a Experimente: KI-Aufgaben und Ablage-Schicht
2436508bc Experimente: Fundament, Datenschicht und Dienste

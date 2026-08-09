# Session Handoff — 10.08.2026, ca. 00:00 Uhr

## Ziel
Pipeline `neue-applikation` so reparieren, dass ein in Werft bearbeitetes Design zu 100 % in
der fertigen App landet — Aussehen, Effekte, Bewegungen —, reproduzierbar fuer jede neue
Anwendung und fuer Android, Windows und macOS. Frank erwartet Perfektion; 99,9 % gelten nicht.

## SOFORT ZUERST
Das Handy ist **gesperrt** (Sperrbildschirm, beide Fold-Displays). Frank muss es entsperren.
Ohne sichtbaren Bildschirm ist keine Abnahme moeglich.
Danach: `adb shell am start -n de.frank.experimente/.MainActivity`, Screenshot mit
`adb exec-out screencap -p -d 4630947123231501204 > datei.png` (das `-d` ist Pflicht, sonst
landet eine Warnzeile im PNG).

## Stand — was FERTIG und gepusht ist
- **Pipeline komplett repariert**, alle vier Profile identisch (minimal/standard/strict/
  opencode-setup), Commits `665a08436` bis `b4adb90d3`.
  - Stufe 1: v1-Gestaltung ist ausdruecklich „Absicht VOR dem Design", nie Bauanweisung.
  - Stufe 2: `messe-design.ps1` vermisst den Entwurf verlustfrei (Chrome DevTools-Protokoll).
    Je Element: Kasten in dp, Farben, Raender, Radien, Schatten, Verlaeufe, Schrift,
    Abstaende, Uebergaenge, ::before/::after, dazu alle @keyframes, alle Zustandsregeln,
    die Regeln fuer reduzierte Bewegung und die SVG-Pfade aller Symbole. Funktioniert auch
    ohne Werft-Ordneraufbau (jede HTML-Datei = ein Bildschirm).
  - Stufe 3: baut aus `Specs/<App>/v2/messung/`, Abnahme Element fuer Element gegen die
    Messung. Vorschrift: `design-umsetzer/references/messung-umsetzen.md` (Compose, WPF, SwiftUI).
  - Klammer: One-Shot-Ablauf nach dem Ruecklauf, Abnahme-Tor mit vier Punkten.
- **Messung fuer Experimente liegt vor:** `Specs/Experimente/v2/messung/` + `/bilder/`,
  18 Bildschirme, B-01 mit 133 Elementen, 13 Keyframes, 30 Zustandsregeln, 19 Symbolen.
- **B-01 aus der Messung gebaut:** Auren, Karte auf volle Breite, Sprechknopf mit Verlauf
  (gemessen bestaetigt: rgb(201,112,77) -> rgb(196,98,60) 58% -> rgb(165,82,50)), Schein,
  innerer Ring, „Lieber tippen" als Flaeche mit Rand, schwebende Leiste mit Pille,
  Symbolknoepfe in Kreisen, Datum in Grossbuchstaben mit Jahr.
- **Schriften eingebettet:** Fraunces, Inter, JetBrains Mono als variable TTF in `res/font`,
  Gewicht ueber die Achse `wght` (`FontVariation`), Datei-Opt-in `ExperimentalTextApi`.
- App gebaut und installiert, v0.4.0 (versionCode 8), startet (PID bestaetigt).

## OFFEN
1. **Bildnachweis fuer B-01** — Geraet war gesperrt.
2. **B-02 bis B-09 aus der Messung bauen.** Je Bildschirm die Messdatei lesen, Elemente nach
   `kasten.y`/`kasten.x` sortieren, Vorschrift anwenden, gegen die Messung abhaken, dann der
   naechste. Bekannte grosse Abweichung: **B-08 Einstellungen** ist heute strukturell falsch —
   Beschriftungen stehen neben statt ueber den Feldern, Abschnitte sind keine Karten, die
   Erscheinung braucht einen Segment-Schalter, Erinnerungen Schalter und Zeitfeld.
3. **Symbole aus der Messung bauen** (`symbol[].d` + `sichtfeld`) statt `Icons.*` — noch nicht
   gemacht, die App nutzt weiter Material-Symbole.

## Fehlgeschlagene Ansaetze — NICHT wiederholen
- Aus den Zahlen des UI-Specs bauen statt aus der Messung.
- Schriften ueber die Google-Fonts-CSS (`/l/font?kit=`) laden: liefert Dateien mit Kopf
  `b88a0000`, die Android nicht laden kann -> Absturz beim Start. Gueltige TTF liegen unter
  `https://raw.githubusercontent.com/google/fonts/main/ofl/<familie>/<Familie>[<achsen>].ttf`.
- `--dump-dom` bei Chrome (im neuen Headless entfernt).
- Ohne `--allow-file-access-from-files` bleiben die @keyframes leer.
- Screenshot-Vergleich als Methode statt Abnahme gegen die Messung.
- Zwei `fillMaxSize`-Kinder in einer `Column` (das zweite bekommt Hoehe 0, kompiliert gruen).

## Anker
- Branch: main
- Letzte Commits:
b4adb90d3 Experimente: Schriften des Entwurfs eingebettet
61979aa06 Experimente: B-01 aus der Messung, App laeuft wieder
bf713a3e4 Messung: drei Logikfehler behoben (Direktive 3)
b5016330a Pipeline: Symbole, beliebige HTML-Quellen, alle drei Zielsysteme
270f7b65d Pipeline: One-Shot-Ablauf, Uebersetzungsvorschrift und Abnahme-Tor

# Session Handoff — 10.08.2026, ca. 00:30 Uhr

## Ziel
Pipeline `neue-applikation` so reparieren, dass ein in Werft bearbeitetes Design zu 100 % in
der fertigen App landet — reproduzierbar fuer jede neue Anwendung, Android/Windows/macOS.
Frank erwartet Perfektion; 99,9 % gelten als nicht erreicht.

## ERSTER SCHRITT DER NEUEN SITZUNG
**Werft-Abruf mit Aktualitaetsnachweis bauen.** Ohne ihn kann ein perfekter Erzeuger den
FALSCHEN Entwurf bauen — das Archiv in der Outbox traegt intern den 09.08.2026 19:29 Uhr,
und niemand merkt, wenn seither in Werft weitergearbeitet wurde. Frank hat das gefunden.
Zugang liegt in `~/SK/werft-studio/` (Adresse 10.8.0.1:8443, `login.txt`, `caddy-root.crt`).
Projekt-URL: `/app/projects/019fe79c-9d11-734a-8b8b-79c4ea20f81b/studio/canvas`.
Als Zwischenloesung ist die Nachfrage bereits im Skill verankert (Phase 0).

**Danach: der End-to-End-Lauf.** Frank hat die App dafuer geloescht. Design in
`Designs/Outbox/`, `neue-applikation` starten, nichts von Hand nachbessern.

## Was FERTIG und gepusht ist (Commits 665a08436 … 2fb65e8f3)
Die Ursache war gefunden: Das Design ging nie beim Designer verloren, sondern zwischen
Erfassung und Bau — dort las ein Modell Prosa und schrieb Code. Beide Enden laufen jetzt
maschinell:

- **`spec-rueckimport/references/messe-design.ps1`** — vermisst jeden Bildschirm in jeder
  Erscheinung ueber das Chrome-DevTools-Protokoll: Kasten in dp, Farben, Raender, Radien,
  Schatten, Verlaeufe, Schrift, Abstaende, Uebergaenge, ::before/::after, alle @keyframes,
  alle Zustandsregeln, reduzierte Bewegung, alle SVG-Pfade. Funktioniert fuer beliebige
  HTML-Quellen. **Bezugsgroesse 475 x 751 dp** (Franks Geraet; per `-Breite`/`-Hoehe`).
- **`design-umsetzer/references/bildschirm-erzeugen.ps1`** — Messung → Compose, mechanisch:
  Positionen, Flaechen, Raender, Radien, Schrift, Farben, mehrlagige Schatten, Verlaeufe,
  inset-Lichtsaum als 1 dp Linie, echte Symbolpfade, Scroll-Bereich aus der gemessenen
  Gesamthoehe. B-01 → 81 Elemente (19 Symbole, 16 Schatten, 2 Verlaeufe), B-08 → 74.
  Klammernbilanz geprueft.
- **`design-umsetzer/references/symbole-erzeugen.ps1`** — gemessene Pfade → ImageVectors.
- **Regeln**: Messung schlaegt Prosa · v1-Gestaltung ist nur Absicht · nicht nachbessern,
  sondern aus der Messung neu schreiben · gruener Build ist keine Abnahme · Abnahme gegen
  die Messung statt gegen ein Foto · Aktualitaet des Ruecklaufs nachweisen.
- Alle vier Profile identisch (minimal/standard/strict/opencode-setup).
- `Specs/Experimente/v2/messung/` + `/bilder/` liegen vor (18 Bildschirme, 475 dp).

## Fehlgeschlagene Ansaetze — NICHT wiederholen
- Bestehende Bildschirme nachbessern und mit Handy-Screenshots vergleichen. Naehert sich an,
  bleibt bei "fast". Aus der Messung neu erzeugen.
- Aus den Zahlen des UI-Specs bauen statt aus der Messung.
- Schriften ueber die Google-Fonts-CSS (`/l/font?kit=`): liefert Dateien mit Kopf `b88a0000`,
  Android stuerzt ab. Gueltige TTF unter
  `raw.githubusercontent.com/google/fonts/main/ofl/<familie>/<Familie>[<achsen>].ttf`.
- `--dump-dom` (im neuen Chrome-Headless entfernt); ohne `--allow-file-access-from-files`
  bleiben die @keyframes leer.
- Zwei `fillMaxSize`-Kinder in einer `Column` (das zweite bekommt Hoehe 0, kompiliert gruen).
- `adb exec-out screencap -p` ohne `-d <display-id>` auf dem Fold.

## Stand der App (nur Versuchskaninchen, nicht das Ziel)
Deinstalliert. Der Code im Repo ist der alte, von Hand nachgebesserte Stand — er wird beim
End-to-End-Lauf durch erzeugten Code ersetzt, nicht weiterbenutzt.

## Anker
- Branch: main
- Letzte Commits:
2fb65e8f3 Stufe 2: Aktualitaet des Ruecklaufs nachweisen
52e4559b7 Erzeuger: Scroll-Bereich aus der gemessenen Gesamthoehe
b7b370f5a Erzeuger: Symbole im erzeugten Bildschirmcode
c1ec2ac12 Erzeuger: Schatten, Verlaeufe und Lichtsaum
baf79e74d Stufe 3: Bildschirme maschinell aus der Messung erzeugen

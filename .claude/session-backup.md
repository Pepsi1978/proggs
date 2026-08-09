# Session Handoff — 10.08.2026, ca. 00:00 Uhr

## Ziel
Die Pipeline `neue-applikation` so reparieren, dass ein in Werft Studio bearbeitetes Design
zu 100 % in der fertigen App landet — Aussehen, Spezialeffekte und Bewegungen —, und zwar
reproduzierbar fuer jede neue Anwendung und fuer Android, Windows und macOS.
Frank erwartet ausdruecklich Perfektion; 99,9 % gelten als nicht erreicht.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
- **Die Pipeline-Reparatur ist inhaltlich fertig und gepusht** (Commit `b5016330a` und die
  drei davor). Offen ist nur noch der **Beweis am lebenden Objekt**: Frank legt das Design
  erneut in `Designs/Outbox/` und startet `neue-applikation` → `design-umsetzer`. Die App
  muss dann ohne Nachbesserung wie der Entwurf aussehen.
- **Die App "Experimente" ist auf dem Handy DEINSTALLIERT** (Frank hat sie entfernt, das
  Ergebnis war unbrauchbar). Der Code im Repo ist der alte, flache Stand plus ein
  angefangener `ui/theme/Effekte.kt`. **Beim naechsten Lauf wird die Oberflaeche aus der
  Messung neu gebaut, nicht nachgebessert.**

## Was die Pipeline jetzt tut (alles gepusht, alle vier Profile identisch)
- **Stufe 2 vermisst den Entwurf, statt ihn zusammenzufassen.**
  `spec-rueckimport/references/messe-design.ps1` oeffnet jeden Bildschirm in jeder
  Erscheinung im Browser (Chrome DevTools-Protokoll ueber WebSocket) und schreibt je Element
  Kasten (x/y/Breite/Hoehe in dp), Farben, Raender, Radien, Schatten, Verlaeufe, Schrift,
  Abstaende, Uebergaenge, `::before`/`::after`, dazu alle `@keyframes`, alle Zustandsregeln
  (`:active`, `[data-recording]`, `.is-active` …), die Regeln fuer reduzierte Bewegung und
  die SVG-Pfade aller Symbole. Ergebnis: `Specs/<App>/v2/messung/` + `/bilder/`.
  Belegt an Experimente: 18 Bildschirme, B-01 mit 133 Elementen, 13 Keyframes,
  30 Zustandsregeln, 32 Regeln fuer reduzierte Bewegung, 19 Symbolen.
- **Stufe 3 baut aus der Messung**, nicht aus Prosa. Vorschrift:
  `design-umsetzer/references/messung-umsetzen.md` — je Eigenschaft die Entsprechung in
  Compose, WPF und SwiftUI. Abnahme Element fuer Element gegen die Messung.
- **Stufe 1** kennzeichnet alles Gestalterische in v1 als „Absicht VOR dem Design".
- **Klammer** hat einen One-Shot-Ablauf (nach dem Ruecklauf keine Rueckfragen mehr) und ein
  Abnahme-Tor mit vier Punkten; ein gruener Build ist keiner davon.

## Der Kernbefund von Lauf 01 (nicht neu herleiten)
Das Design kam vollstaendig an (ZIP == Werft-Canvas, per `diff -rq` belegt). Verloren ging es
zwischen Stufe 2 und 3: Das Spec fuehrte die Effekte nur als namenlosen Anhang
(`design.html:gradient(5) = …`) ohne Bauteil-Zuordnung, und gebaut wurde nach dem v1-Satz
„Keine Schatten. Keine Verlaeufe." (steht in `Specs/Experimente/v1/02-UI-SPEC.md:113`, NICHT
in v2). Die `design.css` hat zwei Schichten; die verbindliche obere ist auf
`.werft-screen[data-screen-id="B-xx"]` eingeschraenkt und traegt Verlaeufe, Schatten
(`--werft-schatten-*` ab Zeile 32), radiale Auren, die schwebende untere Leiste
(12 dp Abstand, 64 dp hoch, Radius 24) und die Pille hinter dem aktiven Feld.

## Fehlgeschlagene Ansaetze — NICHT wiederholen
- Aus den Zahlen des UI-Specs bauen. Genau so ging die Tiefen-Schicht verloren.
- `--dump-dom` bei Chrome: im neuen Headless entfernt. Weg ist das DevTools-Protokoll.
- Ohne `--allow-file-access-from-files` bleiben die `@keyframes` leer (CSSOM gesperrt).
- Screenshot-Vergleich als Methode: naehert sich an, bleibt bei 99 %. Gegen die Messung pruefen.
- Symbole aus `Icons.Outlined.*` nehmen: andere Proportionen und Strichstaerken.
- Zwei `fillMaxSize`-Kinder in einer `Column`: das zweite bekommt Hoehe 0, kompiliert gruen.
- `adb exec-out screencap -p` ohne `-d <display-id>` auf dem Fold: Warnzeile vor den PNG-Bytes.

## Naechste Schritte
1. Frank legt das Design in `Designs/Outbox/` und startet `neue-applikation`.
2. Stufe 2 laufen lassen (Messung entsteht automatisch), dann Stufe 3 **aus der Messung**
   bauen — Bildschirm fuer Bildschirm, jeder gegen seine Messdatei abgehakt.
3. Version bumpen, bauen, installieren.
4. Lauf-Logbuch `Specs/_Pipeline-Logbuch/2026-08-09-lauf-01.md` fortschreiben.

## Offene Punkte
- `OpenLauncher/Profiles/ClaudeCode/minimal/skills` ist per `.gitignore` NICHT versioniert —
  ausgerechnet das laufende Profil. Versioniert sind `standard`, `strict`, `opencode-setup`.
- Die Uebersetzungsvorschrift ist geschrieben, aber noch nie an einem vollstaendigen Bau
  erprobt. Der naechste Lauf ist ihr erster Test.

## Anker
- Branch: main
- Letzte Commits:
b5016330a Pipeline: Symbole, beliebige HTML-Quellen, alle drei Zielsysteme
270f7b65d Pipeline: One-Shot-Ablauf, Uebersetzungsvorschrift und Abnahme-Tor
7f89e9065 Stufe 2: den Entwurf vermessen statt ihn zusammenzufassen
665a08436 Pipeline: Design-Treue erzwingen (Stufe 2 und 3) + Tiefen-Schicht
bae4397e6 Experimente: Oberflaeche, Navigation und Build

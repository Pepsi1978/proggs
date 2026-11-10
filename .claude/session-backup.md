# Session Handoff — 10.11.2026, ca. 17:25 Uhr (Windows-PC CODI, Sitzung proggs-f6)

## Ziel (1-3 Saetze)
Franks Galaxy Tab S9 als „Jarvis Tablet“ einrichten: dieselbe Jarvis-App wie am Handy (Fold 8), dieselben angebundenen Apps,
laufender Abgleich Handy ↔ Tablet (App-Daten, Jarvis' Gedächtnis und Gespräche), Programmieraufträge vom Tablet an Mac/Windows/Server,
Installation aufs Tablet über dieselben adb-Werkzeuge (WLAN/Tunnel). Die Aufgabe ist umgesetzt, reviewt (39 Befunde) und ausgeliefert.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen: Abschlussmeldung gegeben, sehr ausführliches Code-Review erledigt und
Befunde behoben, Pflicht-Abschluss wiederholt (Bau, Bump, Commit, Push, Installation auf beiden Geräten, apk-update),
Sitzungs-Auswertung (session-flaschenhals, Maßnahme M8) gelaufen. Nichts Uncommittetes von dieser Sitzung im Arbeitsbaum.

## Aktueller Status
- Erledigt und auf beiden Geräten installiert: Jarvis 1.8.611 (vc 622), Aufgaben 1.0.89 (90), Ziele 1.0.41 (42),
  GenialeIdeen 1.7.44 (75), Epigenetik 1.0.10 (11), StackLabor 0.3.54 (72). Nur am Tablet neu installiert (alte Fassung mit
  unbekanntem Schlüssel nach Franks Ja deinstalliert): Entropie Reductor 0.29.27, Best Journal Debug 0.21.21.
- Relay (Hostinger, /opt/jarvis/relay/relay.py) mit Mehrgeräte-Betrieb ausgerollt; Sicherungen dort:
  relay.py.bak-20261110-vor-tablet und relay.py.bak-20261110-vor-review. Handy und Tablet sind gleichzeitig verbunden.
- WireGuard-Peer fürs Tablet am Server angelegt (10.8.0.8), Konfiguration: ~/SK/second-brain/wireguard/tab-s9.conf,
  auf dem Tablet unter Download/jarvis-tablet.conf; WireGuard-App ist auf dem Tablet installiert, Import steht aus (Frank).
- apk-update: Aufgaben, Ziele, GenialeIdeen, Epigenetik, StackLabor veröffentlicht; Jarvis ausgesetzt (driveUploadAusgesetzt).
- Live geprüft: Test-Idee Handy → Tablet (anlegen und löschen) mit den Endfassungen; Gedächtnis-Abgleich Erstlauf
  (Handy 0 übernommen/211 hochgeladen, Tablet 6 übernommen/1 hochgeladen), Merkdateien am Handy danach unverändert.
- Sicherung des Handy-Gedächtnisses vor dem ersten Abgleich: ~/SK/Jarvis/sicherung-handy-vor-abgleich-20261110/ (tar + Prüfsummen).
- In Arbeit: nichts. Blockiert: nichts.

## Relevante Dateien
- `Werkzeuge/adb-wlan/geraete.json` — feste Zuordnung handy (R3GL7073MLM, 10.8.0.7) / tablet (R52W800205N, 10.8.0.8)
- `Werkzeuge/adb-wlan/adb-wlan.ps1|.sh` — `-Geraet handy|tablet|alle` bzw. `--geraet`; Wachhund läuft mit „alle“
- `Werkzeuge/adb-wlan/auf-beide.sh` — installiert APKs gleichzeitig auf Handy und Tablet (für die fünf abgeglichenen Apps)
- `Werkzeuge/jarvis-install/jarvis-install.ps1|.sh` — `-Ziel`/`--ziel handy|tablet`; weckt Jarvis nach der Installation (AUFWACHEN)
- `Jarvis/server/relay/relay.py` — Klasse Geraet, Kopfzeile X-Jarvis-Geraet, auftrag-geraete.json, Abgleich-Speicher /j/<g>/abgleich
- `Jarvis/app/src/main/java/de/frank/jarvis/data/Geraet.kt` — Handy/Tablet nach Build.MODEL (SM-X/SM-T/SM-P = Tablet)
- `Jarvis/app/.../data/Umzug.kt` — Einstellungs-Umzug (EINSTELLUNGEN_UMZUG) und Aufwach-Signal (AUFWACHEN), TABLET_AUS
- `Jarvis/app/.../abgleich/GeraeteAbgleich.kt` — Abgleich der fünf Apps über die Brücken (Zustand files/geraeteabgleich.json)
- `Jarvis/app/.../abgleich/GedaechtnisAbgleich.kt` — Abgleich von Merkdateien, Gesprächen usw. (Zustand files/gedaechtnis-abgleich.json)
- `<App>/app/src/main/java/.../bruecke/GeraeteAbgleich.kt` (Aufgaben, Ziele, GenialeIdeen [bridge/], Epigenetik, StackLabor) — in allen
  fünf byte-gleich bis auf die package-Zeile; Export/Import des ganzen Stands über die Jarvis-Brücke
- `Jarvis/werkzeuge/tablet-einrichten.sh` — Schritte rechte und umzug (kopiert keine Daten mehr)
- `Jarvis/README.md` Abschnitt „Jarvis Tablet (zweites Gerät) und Geräteabgleich“, `Jarvis/app/src/main/assets/funktionen.md`
- Merknotiz: `OpenLauncher/Profiles/ClaudeCode/minimal/projects/C--Users-barwa-proggs/memory/jarvis-tablet-zweites-geraet.md`
- Profilregel 18 (zwei Geräte) in allen zwölf Quellen unter `OpenLauncher/Profiles/*/sources` bzw. `OpenCode*/…/AGENTS.md`

## Getroffene Entscheidungen
- Eine APK für beide Geräte, kein eigenes Paket fürs Tablet; jede Jarvis-Version und die fünf Apps kommen auf BEIDE Geräte.
- Abgleich der Apps als ganzer Stand (jüngster gewinnt), nicht zeilenweise: hochgezählte Kennungen, keine Löschmarken.
- Erstquelle ist das Handy; ein leerer Stand geht nie hoch; Konflikte nach `lokal_seit` (wann Jarvis die Änderung bemerkt hat).
- Franks Entscheidungen: Genialer Wecker klingelt nur am Handy und wird nicht abgeglichen; ChatGPT-Plugin nur für das Handy
  (am Tablet ausgeblendet); Entropie Reductor und Best Journal am Tablet durften neu installiert werden.
- Auf dem Tablet standardmäßig aus: Tagesauswertung, Lernen, Losfahr-Meldung, Weckwort (Umzug.TABLET_AUS).
- Nicht abgeglichen (bewusst): Programmier-Gespräche, das gerade offene Gespräch, Lernlauf-Dateien, funktionen/, vorgaenge.json.
- ChatGPT-/Codex-Anmeldung und Anthropic-Sitzungen werden nicht kopiert (könnten das Handy abmelden).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- `PRAGMA quick_check` als Paketprüfung: schlägt bei Datenbanken mit Volltext-Tabellen (FTS, Geniale Ideen) auf einer nur lesbaren
  Kopie fehl, obwohl die Datenbank heil ist. Nicht wieder einbauen; geprüft wird Lesbarkeit + Prüfsumme + Inhaltsvergleich danach.
- Inhalts-Prüfsumme (`inhalt()`) im App-Modul ändern, ohne dass danach das Handy neu hochlädt: führt zu „Inhalt stimmt nicht“ und
  Sperre. Wenn nötig: beide Geräte aktualisieren, dann am Handy eine Runde anstoßen (touch der DB per run-as + Relay-Signal).
- Dateizeit der Datenbank als „wann geändert“: wird vom Export (Checkpoint) und Einspielen selbst verschoben. Nicht verwenden.
- `sed -i` mit `\\r` im Ersatztext unter Git Bash schreibt ein echtes Steuerzeichen in die Datei. Für solche Ersetzungen das
  Scratchpad-Skript `ersetze.py` (JSON mit alt/neu/Anzahl, erhält CRLF/LF) oder das Edit-Werkzeug nehmen.
- Lange Bash-Heredocs (>30 Zeilen) blockt der Hook: Skripte per Write in den Scratchpad legen und ausführen.
- `rm` auf `C:\data\agentenkanal.json` (Testrest des Relay-Tests) wurde vom Sicherheitscheck verweigert: nicht umgehen, Frank löscht selbst.
- Gradle-Zwischenspeicher bei StackLabor lieferte nach dem Bump die alte Versionsnummer: `.gradle/configuration-cache` löschen.
- Jarvis-Werkzeuge (MCP) zum Testen: nach jedem Jarvis-Neustart hängen sie ~35.000 Zeichen Kontext an. Stattdessen DB per run-as
  „touch“ und Relay-Signal `{"signal":"geraeteabgleich","geraet":"handy"}` an `/geraet/signal` senden.
- Eine parallele Sitzung hat während der Arbeit Jarvis 1.8.608–1.8.610 gebaut und installiert (K-Kalender). Vor jedem Bump
  `git pull --rebase --autostash`, Versionslog per Skript (Werkzeuge/versionslog/eintrag.py oder Scratchpad bump.ps1), nie von Hand.

## Wichtige Recherche-Ergebnisse
- Der Relay kannte vor dem Umbau genau ein Gerät: Ein zweites mit demselben Token hätte das Handy im Sekundentakt verdrängt.
- Entropie Reductor gleicht selbst über Google Drive ab (braucht am Tablet nur die Google-Anmeldung).
- Die Installations-Ansage verstummt absichtlich, sobald Frank „Sofort“ tippt; am Tablet kam der erste Ton nach gut 3 s
  (Handy 1,5–2 s). Ob die Ansage am Tablet ohne frühes Tippen hörbar ist, ist noch nicht bestätigt.
- Review-Befund in fremdem Code (nicht behoben): `fahrt/Abfahrt.kt` meldungZurueckziehenWennEntfallen behandelt eine leer gelesene
  Schichtliste wie einen entfallenen Dienst (Losfahr-Meldung fiele weg). Gehört zur K-Kalender-Arbeit der parallelen Sitzung.

## Naechste Schritte (priorisiert)
1. Nur auf Franks Wunsch: prüfen, ob nach dem WireGuard-Import am Tablet die Rechner in Jarvis erscheinen
   (`adb -s 192.168.0.190:5555 shell ip -4 addr | grep 10.8`; Logbuch „Fernwartung: … grün“).
2. Nie live gelaufen und bei Gelegenheit testen: Abgleich Tablet → Handy, Konfliktfall, echter Programmierauftrag vom Tablet.
3. Offene Verbesserungen: Abgleich-Modul in die Modul-Bibliothek (fünf Kopien), offenes Gespräch nach fremder Änderung neu laden,
   Aufgaben und Ziele gemeinsam abgleichen, Jarvis-Werkzeugantworten für CLI-Sitzungen ohne den großen Kontextanhang.
4. Mac: adb-Wachhund richtet sich beim nächsten nicht leisen `adb-wlan.sh`-Lauf selbst neu ein (kennt dann auch das Tablet).

## Offene Fragen
- Für Frank am Tablet: WireGuard-Datei importieren und einschalten, in Jarvis mit ChatGPT verbinden, in Entropie Reductor bei
  Google anmelden, in UpdateStation den Update-Ordner wählen, Schlüssel der fünf Apps eintragen, Jarvis „Nie in Standby“.
- Für Frank am PC: `C:\data\agentenkanal.json` (Testrest) löschen.

## Anker
- Branch: main (gleich origin/main)
- Letzte Commits:
2d645f7a5 session-flaschenhals: merknotiz zu jarvis tablet berichtigt
93c089589 session-flaschenhals: auf-beide.sh installiert auf handy und tablet gleichzeitig (M8)
740698274 geräteabgleich: review-befunde beheben (leerer stand geht nie hoch, konflikte nach bemerk-zeit, programmier-gespräche geschützt, einspielen in einem zug, server-fehler im logbuch, skripte je gerät) (Jarvis 1.8.611, Aufgaben 1.0.89, Ziele 1.0.41, GenialeIdeen 1.7.44, Epigenetik 1.0.10, StackLabor 0.3.54)
64f02aa72 session-flaschenhals: versionslog-eintrag per skript statt handgesetzter nummer
eeed96872 Jarvis: vergib für die K-nachbesserung den freien versionCode 621 (1.8.610)

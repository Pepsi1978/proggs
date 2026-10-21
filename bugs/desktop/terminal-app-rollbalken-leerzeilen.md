# Terminal.app: Leerzeile nach jeder Zeile der ersten Claude-Code-Nachricht

Stand: 21.10.2026 — macOS 27 (Darwin 27.0), Terminal.app, Claude Code v2.1.306, MacBook Air 1280×803 pt

## Symptom
In manchen Claude-Code-Sitzungen (Jarvis-Aufträge über den OpenLauncherMac) steht hinter **jeder** Zeile der ersten
Nachricht eine zusätzliche, farbig hinterlegte Leerzeile; ein Satzzeichen vom Zeilenende rutscht allein an den Anfang der
nächsten Zeile. Andere Sitzungen im selben Fenster sind kompakt.

## Ursache (gemessen)
- Der Text ist sauber: Auftragsdatei, Startargument und Sitzungsprotokoll (JSONL) haben nur LF und Leerzeilen nur an echten Absätzen.
- Claude Code polstert jede Zeile der Nutzernachricht mit Hintergrundfarbe auf die **beim Zeichnen** gültige Fensterbreite.
- `AppleShowScrollBars = Automatic`: Ist eine Maus verbunden (hier BLE-Maus), zeigt macOS einen festen Rollbalken, der in
  einem 1276 pt breiten Fenster genau **3 Spalten** kostet (180 → 177). Wechselt das, nachdem die Nachricht gezeichnet ist,
  bricht Terminal jede auf 180 gepolsterte Zeile um, und die 3 Restzeichen werden zur farbigen „Leerzeile“.
- Beleg: `history of tab` der betroffenen Sitzungen zeigt 180 Zeichen breite Zeilen bei `number of columns` = 177;
  unbetroffene Sitzungen sind 177 breit. Ein Testfenster mit `printf '\e[48;5;238m%-180s\e[0m\n'` bei 177 Spalten zeigt dasselbe Bild.

## Abhilfe
```bash
defaults write com.apple.Terminal AppleShowScrollBars -string WhenScrolling
```
Nur für Terminal.app (nicht global). Gilt für neu gestartetes Terminal.app; danach haben Fenster immer 180 Spalten,
unabhängig von der Maus. Steht in `Jarvis/werkzeuge/pc-bruecke/einrichten.sh`.

## Nicht tun
- Keine Zeilenumbrüche im Auftragstext „normalisieren“ oder Leerzeilen entfernen: Der Text ist nicht die Fehlerquelle.
- Kein Warten auf eine stabile Fenstergröße vor dem Start: Der Wechsel kann Minuten später kommen.
- Terminal.app nicht für die Einstellung neu starten, solange Sitzungen laufen (`open -n -a Terminal` startet eine zweite
  Instanz mit wiederhergestellten Fensterkopien).

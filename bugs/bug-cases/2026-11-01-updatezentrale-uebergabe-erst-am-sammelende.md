# UpdateZentrale: Fehler im Sammellauf gingen erst am Ende des Laufs an die CLI

**Datum:** 01.11.2026 · **Plattform:** Windows und macOS · **Schwere:** hoch

## Symptom

Im Sammellauf scheiterte Codex CLI um 16:22 Uhr (Zeitlimit von `npm install -g`, Karte rot). Es startete
keine Claude-Code-Sitzung, und in der Diagnose stand kein einziges `uebergabe`-Ereignis.

## Ursache

- Der Fehler wurde erkannt (`Fehlgeschlagen`). `SammelAusfuehrenAsync` startete die Übergabe aber erst
  nach dem letzten Programm des Plans. Hinter Codex standen noch Bionic, Python, VS Build Tools, Kimi und
  Firebase, also womöglich eine Stunde und mehr. Ein hängendes Programm schob die Übergabe mit.
- Endete die App vorher, ging der Fehler ganz verloren: Er stand nirgends als offen vermerkt.
- `Nicht verifiziert` zeigte die Karte rot, `AutomatischRelevant` nahm es aber nicht mit.
- Windows hielt den Sammelbesitz zudem, bis der Launcher geantwortet hatte (bis zu zwei Minuten).

## Fix

- Jeder Fehler geht direkt nach seinem Bericht an eine Sitzung, ohne auf den Rest des Laufs zu warten.
- Nur Fehler aus den Phasen „eigene Werkzeuge“ und „CLI-kritisch“ warten bis zum Ende dieser Phasen, weil
  die CLI-Phase verlangt, alle Terminals zu schließen, und die Sitzung sonst mit beendet würde.
- `AutomatischRelevant` folgt `IstFehler`: Was die Karte als Fehler zeigt, geht auch an die Sitzung.
  Ausnahme bleibt nur das zu langsame Netz (Windows).
- Die Übergabe läuft nebenher und hält weder andere Updates noch den Sammelbesitz auf.
- Jeder automatische Start schreibt `uebergabe/auto.start` in die Diagnose.

## Vorbeugung

Eine Reaktion auf einen Fehler nie an das Ende eines Laufs hängen, dessen Dauer offen ist. Fehlt in der
Diagnose `auto.start`, hat die App keine Sitzung gestartet.

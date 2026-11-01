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

## Abbruch per Knopf (1.3.69, 1.0.41)

- Ein Abbruch wird an genau einer Stelle beurteilt (`NachAbbruchAsync`, `nachAbbruch`), egal was ein Anbieter
  aus dem beendeten Befehl gemacht hat: Der echte Stand wird nachgelesen, ein schon fertiger Installer zählt
  als erledigt. Von selbst geht danach nur ein unvollständig hinterlassenes Programm an eine Sitzung.
- Kein Knopf bei Repo-Skripten und Sitzungsstarts: Ein beendetes Skript ließe sein Programm geschlossen oder halb
  gebaut zurück, und die Mac-Projektregel verbietet das Beenden fremder Skripte.
- Die Übergabe per Knopf umgeht die Sperre nur im laufenden Sammellauf, nur für dort abgebrochene Karten und erst,
  wenn keine Phase mehr aussteht, die Terminals schließt.
- Sitzungen warten vor dem Neubau der App auf das Ende eines laufenden Sammellaufs (Auftragstext, Schritt 6).

## Vorbeugung

Eine Reaktion auf einen Fehler nie an das Ende eines Laufs hängen, dessen Dauer offen ist. Fehlt in der
Diagnose `auto.start`, hat die App keine Sitzung gestartet.

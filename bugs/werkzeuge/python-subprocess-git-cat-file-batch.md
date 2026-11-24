# Python subprocess: `git cat-file --batch` hängt bei Eingabe über die Pipe

Stand: 24.11.2026 · Python 3.15.2 (Homebrew), git 2.5x, macOS

## Symptom
`subprocess.run(["git", "cat-file", "--batch"], input=<viele Zeilen>, capture_output=True)` hängt in
`_communicate_io_posix` → `os.write` (Stapel im KeyboardInterrupt). Mit wenigen Einträgen lief es, ab einigen Dutzend
Objekten nicht mehr. Gefunden in `Werkzeuge/jarvis-verteilung/jarvis_verteilung.py` (`buch_lesen`).

## Ursache
git schreibt die Objekte erst zurück, während Python noch in die Eingabe-Pipe schreibt; das gleichzeitige Schreiben
und Lesen staut sich (blockierendes `os.write` eines großen Blocks), bis das Zeitlimit greift.

## Vorbeugung
Eingabe nicht als Pipe, sondern als Datei übergeben:
```python
with tempfile.TemporaryFile() as f:
    f.write(eingabe); f.seek(0)
    subprocess.run([...], stdin=f, capture_output=True, timeout=120)
```
Dasselbe gilt für jeden Batch-Befehl, der schon während des Lesens ausgibt.

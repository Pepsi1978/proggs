# Python auf Windows / Cross-Platform-Scripting Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Datei oeffnen | `open()` immer `encoding='utf-8'` (Lesen evtl. `utf-8-sig`) | §1.1 |
| 2 | JSON schreiben | `ensure_ascii=False` PLUS `encoding='utf-8'` (beides) | §1.3 |
| 3 | Text-/JSON-Datei generieren | `newline='\n'`, CSV `newline=''` (sonst CRLF/Leerzeilen) | §1.4 |
| 4 | Kritische Datei sicher schreiben | Atomar: temp im selben Ordner → flush → fsync → `os.replace` | §2 |
| 5 | finaler Datei-Tausch | `os.replace` (nie `os.rename`); temp im SELBEN Verzeichnis | §2.1 |
| 6 | Pfade bauen | Nur `pathlib.Path`, `Path.home()`; NIE `/c/Users/...` | §3.1 |
| 7 | Pfad an Tool/Config geben | `Path(...).as_posix()` (nicht `str()`) | §3.4 |
| 8 | `subprocess` aufrufen | Args als Liste, kein `shell=True`; `encoding='utf-8', check=True` | §3.7 |
| 9 | `print()`/`logging` + Emoji | `reconfigure(encoding='utf-8')`, `FileHandler(encoding=)`, `flush=True` | §1.8 |
| 10 | venv anlegen | `.venv` pro Projekt, immer `python -m pip`, nie committen | §4 |
| 11 | CLI-Skript schreiben | `main()` + `raise SystemExit(main())`, Exit-Codes 0/1/2/130 | §5 |
| 12 | Encoding-Fallen finden | `-X warn_default_encoding`; moderne Typen + `ruff` | §6 |
| 13 | Fremdcode ohne explizites `encoding=` | Notbremse: `PYTHONUTF8=1` / `python -X utf8` | §1.6 |

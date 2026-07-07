# Python auf Windows (Encoding & Cross-Platform-Scripting) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Jedes `open()` | Immer `encoding='utf-8'` (Lesen evtl. `utf-8-sig`) | §1.1 |
| 2 | JSON schreiben | `ensure_ascii=False` UND `encoding='utf-8'` (beides) | §1.5 |
| 3 | Datei beginnt mit `﻿` | BOM-Datei lesen mit `encoding='utf-8-sig'` | §2.1 |
| 4 | Kritische Datei/Config | Atomar: temp im selben Ordner → flush → fsync → `os.replace` | §5.1 |
| 5 | finaler Datei-Tausch | `os.replace` (nie `os.rename` — failt auf Windows) | §5.2 |
| 6 | `subprocess` aufrufen | `text=True, encoding='utf-8', errors='replace'`, Args als Liste | §1.6 |
| 7 | `print()`/`logging` + Emoji | `sys.stdout.reconfigure(encoding='utf-8')` / `FileHandler(encoding=)` | §1.4 |
| 8 | Pfad im Python-Code | NIE `/c/Users/...` — `pathlib`/`expanduser`, Backslash als Raw | §3.1 |
| 9 | Backslash-Pfad-Literal | Raw-String `r"..."` oder `/` (sonst `\U`/`\n`-Escape) | §3.2 |
| 10 | generierte Text-/JSON-Datei | `newline='\n'` (sonst CRLF auf Windows) | §4.2 |
| 11 | CSV schreiben | `open(..., newline='')` (sonst Leerzeilen) | §4.1 |
| 12 | parallele Schreiber | `filelock` + atomar (kein `fcntl`/`msvcrt` direkt) | §5.8 |
| 13 | `re.sub`-Replacement | NIE Raw-String mit `\U`-Escape mischen — Lambda nutzen | §9.1 |
| 14 | Fremdcode ohne `encoding=` | Notbremse `PYTHONUTF8=1` / `python -X utf8` | §1.3 |
| 15 | `python`/`pip` auf Windows | `py -3` + `python -m pip` (nicht MS-Store-Stub) | §6.1 |

---

> **Warum derselbe Code auf macOS laeuft, auf Windows crasht:** macOS/Linux haben UTF-8 als
> Locale-Default ueberall; Windows nutzt die ANSI-Codepage (cp1252) fuer Dateien/Streams und
> die OEM-Codepage (z.B. cp850) fuer die Konsole. Fast jeder Bug hier ist eine Auspraegung
> dieses einen Unterschieds.

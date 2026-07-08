# Bekannte Bugs & Fallen: Python auf Windows (Encoding & Cross-Platform-Scripting)

> **PFLICHT-LESEN vor JEDER Arbeit an einem Python-Skript** (`*.py`), besonders wenn es
> Dateien liest/schreibt, JSON/CSV erzeugt, `print()`/`logging`/`subprocess` nutzt oder
> plattformuebergreifend (Windows **und** macOS/Linux) laufen soll.
> Kuratiert aus offizieller Doku + PEPs, CPython-Bugtracker (gh-verifiziert), Community
> (SO/dev.to/Medium/Blogs) und eigenen Vorfaellen. Loesungen sind **funktionserhaltend**
> (nie "Feature/encoding weglassen", nie Zeichen verwerfen).
>
> **Stand:** recherchiert am **2026-06-02**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax)
> fuer **CPython 3.13.13** (live ermittelt:
> **Anker:** python=3.13.13  <!-- maschinenlesbar fuer check-version-anchor.py -->
> `python --version`). Kern-Anker: auf dieser Version ist
> `locale.getpreferredencoding(False)` = **cp1252**, `sys.stdout.encoding` = **cp1252**,
> aber `sys.getfilesystemencoding()` = utf-8. Der **UTF-8-Mode wird erst ab Python 3.15
> Default** (PEP 686) — auf 3.13.x gilt cp1252 als Datei-/Stream-Default. Versionsangaben
> pro Bug beachten: einige "per Design"-Fallen gelten dauerhaft, einige Bugs sind in
> aelteren Versionen schon gefixt (siehe Sektion 8).
>
> **Versions-Horizont (Re-Recherche 2026-07-02):** Am Kern-Anker aendert sich NICHTS — bestaetigt.
> **Python 3.14** ist released (07.10.2025, aktuell 3.14.6) und haelt **cp1252 als Windows-Default bei**
> (belegt via CPython/Claude-Code-Issue 2026-06 auf Win 11 + 3.14). **PEP 686** ist **Final**, Ziel
> **unveraendert Python 3.15** (erwartet ~Okt 2026) — dort wird UTF-8-Mode Default (abschaltbar `PYTHONUTF8=0`).
> Frank laeuft 3.13.13; 3.13.14 existiert (Mini-Patch, keine Windows-Encoding-Relevanz). Solange < 3.15:
> **jedes `open()`/`subprocess`/`json.dump` braucht explizit `encoding='utf-8'`** — der Almanach bleibt voll gueltig.

---

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

---

## 🔗 Bezugs-Tabelle: Bug-Almanach ↔ Best-Practice

> Zweite Seite der Medaille: `best-practices/claude-tooling/python-windows.md` sagt
> *wie man es von vornherein richtig macht, damit der Bug gar nicht erst entsteht*.

| Bug-Abschnitt (diese Datei) | Best-Practice-Gegenpart in `best-practices/claude-tooling/python-windows.md` |
|---|---|
| §1 Encoding-Defaults (1.1–1.9: cp1252 / print / json / subprocess / logging / Buffering) | §1 Encoding & Text-/JSON-I/O (1.1–1.9) |
| §2 UTF-8 BOM (2.1) | §1.2 BOM-behaftete Dateien lesen (`utf-8-sig`) |
| §3 Pfade (3.1–3.7: Git-Bash-Pfade / Escape / MAX_PATH / Reserved / pathlib) | §3 Pfade plattformneutral & Cross-Platform-Mechanik (3.1–3.9) |
| §4 Zeilenenden / CSV (4.1–4.2) | §1.4 Newline-Handling fuer generierte Dateien |
| §5 Atomares Schreiben / Datei-Korruption (5.1–5.9) | §2 Atomares & crash-sicheres Datei-Schreiben (2.1–2.11) |
| §6 venv / PATH / Interpreter (6.1–6.6) | §4 venv & Dependency-Management (4.1–4.10) |
| §7 subprocess / Shell-Mechanik (7.1–7.2) | §3.7 subprocess cross-platform (Listen-Args, kein shell/shlex) |
| §8 Fix-Status & Versions-Denken | §1.9 (PEP 686 Ausblick) + §6.11/§6.12 (3.13-Neuerungen, EncodingWarning) |

---

## 1. Encoding-Defaults (cp1252) — die Kern-Fehlerklasse

### 1.1 `open()` ohne `encoding=` nutzt cp1252 statt UTF-8  ⭐ HAEUFIG (Nr.1-Ursache)
**Symptom:** `UnicodeDecodeError: 'charmap' codec can't decode byte ...` beim Lesen einer
UTF-8-Datei, oder `UnicodeEncodeError: 'charmap' codec can't encode character ...` beim
Schreiben von Umlauten/Emoji. Code laeuft auf macOS/Linux, crasht auf Windows. Manchmal
auch stilles Mojibake statt Crash.
**Ursache:** Der Default-`encoding` von `open()` ist `locale.getpreferredencoding(False)` —
auf deutschem/englischem Windows **cp1252**, nicht UTF-8.
**Versionen:** per Design bis 3.14; **UTF-8 wird Default erst ab 3.15** (PEP 686). Auf
3.13.13 voll aktiv.
**FIX:** Immer explizit `open(path, 'r', encoding='utf-8')` bzw. `'w', encoding='utf-8'`.
Auf Windows zusaetzlich oft `newline='\n'` sinnvoll (siehe 4.2). Bewusste Locale-Dateien:
`encoding='locale'` (selbstdokumentierend, korrekt ab 3.11 — siehe 8).
**Quelle:** PEP 597 (peps.python.org/pep-0597), docs.python.org/3/library/functions.html#open

### 1.2 Fehlendes `encoding=` bleibt still unentdeckt (EncodingWarning ist opt-in)
**Symptom:** Der Bug aus 1.1 faellt erst beim Endnutzer auf Windows auf — bei Entwicklung
auf macOS keine Warnung.
**Ursache:** `EncodingWarning` ist aus Rueckwaerts-Kompatibilitaet standardmaessig **aus**.
**Versionen:** `EncodingWarning` ab **3.10** (PEP 597); an mehr Stellen ab 3.11 (#91954).
**FIX:** Beim Entwickeln/Testen aktivieren: `python -X warn_default_encoding skript.py` oder
`PYTHONWARNDEFAULTENCODING=1`. Jeden Treffer mit `encoding='utf-8'` (oder `'locale'`) fixen.
**Quelle:** PEP 597; cpython #91954 (gh: CLOSED/COMPLETED).

### 1.3 UTF-8-Mode (`PYTHONUTF8`/`-X utf8`) ist die globale Notbremse — aber nicht Default
**Symptom:** Fremdcode/Bibliothek voll mit cp1252-Fallen, einzelne `encoding=`-Fixes nicht
machbar.
**Ursache:** Ohne UTF-8-Mode erben `open()`, stdio und `subprocess` das Locale-Encoding.
**Versionen:** UTF-8-Mode seit **3.7** (PEP 540); Default **aus** bis 3.14, **Default ab 3.15**
(PEP 686, abschaltbar via `PYTHONUTF8=0`).
**FIX:** `set PYTHONUTF8=1` / `python -X utf8`. Stellt `open()`→UTF-8, stdin/stdout→UTF-8/
surrogateescape, stderr→UTF-8/backslashreplace. Ersetzt NICHT die explizite `encoding=`-Angabe
im eigenen Code (die ist robuster, weil unabhaengig von der Umgebung).
**Hinweis (echtes OS-Encoding ermitteln):** `locale.getpreferredencoding(False)` wird vom UTF-8-Mode
**verfaelscht** (liefert dann `utf-8`, nicht das echte Locale). Seit **Python 3.11** gibt es
`locale.getencoding()`, das IMMER das echte Locale-Encoding (auf Windows cp1252) liefert — unabhaengig
vom UTF-8-Mode. Nutzen, wenn man das tatsaechliche OS-Encoding braucht (z. B. beim Lesen von Konsolen-Output).
**Quelle:** PEP 540, PEP 686, dev.to/methane/python-use-utf-8-mode-on-windows-212i, docs.python.org (locale.getencoding, 3.11+)

### 1.4 `print()`/stdout schreibt cp1252 bei Pipe/Redirect (Konsole ≠ Pipe)  ⭐ HAEUFIG
**Symptom:** `print("café 😀")` zeigt im Terminal korrekt, aber `python x.py > out.txt`,
`| findstr`, oder Aufruf als Subprozess (Hook/CI!) crasht mit `UnicodeEncodeError: 'charmap'`
oder erzeugt Mojibake. `chcp 65001` hilft hier NICHT.
**Ursache:** PEP 528 stellte nur die **Konsole** auf UTF-8 (WindowsConsoleIO). Geht stdout in
eine Datei/Pipe (kein Konsolen-Handle), greift wieder der locale-/ANSI-Codec (cp1252).
**Versionen:** Konsole-UTF-8 ab 3.6; Pipe-Verhalten per Design bis 3.14.
**FIX:** Am Skript-Anfang `sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')`
(ab 3.7), ODER global `PYTHONUTF8=1` / `PYTHONIOENCODING=utf-8`. Zeichen NIE verwerfen.
**Quelle:** PEP 528; wiki.python.org/moin/PrintFails; isaacong.me UnicodeEncodeError redirect.

### 1.5 `json.dump(..., ensure_ascii=True)` (Default) escaped + crasht beim Schreiben  ⭐ HAEUFIG
**Symptom:** Zwei Probleme: (a) geschriebenes JSON enthaelt `Müller`/`😀` statt
`Müller`/`😀` (unleserlich); (b) Crash `UnicodeEncodeError` beim Schreiben der Datei auf
Windows. **`ensure_ascii=False` ALLEIN reicht nicht** — ohne `encoding='utf-8'` crasht es trotzdem.
**Ursache:** (a) `ensure_ascii=True` ist Default → escaped alles Nicht-ASCII. (b) Datei-Default
cp1252 (siehe 1.1).
**Versionen:** per Design, alle Versionen.
**FIX:**
```python
with open('out.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
```
Fuer Crash-Sicherheit zusaetzlich atomar schreiben (siehe 5.1/5.9).
**Quelle:** docs.python.org/3/library/json.html; pynative.com; eigene Projektregel
`resilient-bugfixing.md` ("Windows: UTF-8 Encoding ist PFLICHT").

### 1.6 `subprocess(..., text=True)` ohne `encoding=` → cp1252/OEM-Mismatch  ⭐ HAEUFIG
**Symptom:** `subprocess.run(..., text=True).stdout` liefert Mojibake oder `UnicodeDecodeError`,
obwohl das Kindprozess-Programm sauberen Output liefert.
**Ursache:** `text=True`/`universal_newlines=True` ohne `encoding=` dekodiert mit dem
locale-Encoding (cp1252, ANSI). Windows-Tools geben aber oft die **OEM-Codepage** (z.B. cp850)
oder UTF-8 aus → Mismatch (z.B. `ö` = 0x94 in cp850, anders in cp1252).
**Versionen:** per Design auf 3.13. Tracker #105312 wurde 2026-03-16 als COMPLETED geschlossen,
aber der Fix landet mit dem UTF-8-Default in **3.15** — auf **3.13.13 ist er NICHT enthalten**,
Workaround bleibt aktiv (siehe Sektion 8).
**FIX:** `subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='replace')`.
Bei OEM-Tools `encoding='cp850'`/`'oem'`. `errors='replace'` verhindert Crash ohne Datenverlust
(sichtbares Ersatzzeichen). Bei reinem Byte-Handling `text=False` und selbst dekodieren.
**Quelle:** cpython #105312, bpo-27179, bpo-34618; docs subprocess.

### 1.7 `logging` mit Emoji/Umlaut crasht auf Windows-Konsole
**Symptom:** `logging.info("✅ fertig")` crasht mit `UnicodeEncodeError`; lokal (UTF-8-Terminal)
ok, Production (CMD cp1252) crasht. FileHandler-Datei hat Mojibake.
**Ursache:** `StreamHandler` schreibt mit dem Encoding des Streams (cp1252, gleiche Wurzel wie
1.4); `FileHandler` nutzt ohne `encoding=` ebenfalls das Locale-Encoding.
**Versionen:** per Design; `encoding=`-Param fuer `basicConfig` ab **3.9** (#37111).
**FIX:** `logging.FileHandler(path, encoding='utf-8')`; `logging.basicConfig(encoding='utf-8',
errors='backslashreplace')` (ab 3.9); fuer Konsole `sys.stdout.reconfigure(encoding='utf-8')`.
**Quelle:** bpo-37111, bpo-835353.

### 1.8 `sys.stdout.reconfigure()` nur VOR dem ersten Lesen; stderr-Handler weicht ab
**Symptom:** `reconfigure(encoding='utf-8')` wirkt nicht oder wirft, wenn schon gelesen wurde.
`print()` crasht weiterhin, obwohl reconfigure aufgerufen.
**Ursache:** Encoding/Newline lassen sich nicht mehr aendern, nachdem vom Stream gelesen wurde
(nach Schreiben geht es). Default-Errorhandler: `sys.stdout`=`strict` (crasht!), `sys.stderr`=
`backslashreplace`.
**Versionen:** `reconfigure()` ab **3.7**.
**FIX:** `reconfigure()` ganz am Skript-Anfang. Fuer robustes stdout `errors='backslashreplace'`
mitgeben, damit Logging nie crasht.
**Quelle:** docs.python.org/3/library/io.html (TextIOWrapper.reconfigure).

### 1.9 Output-Buffering: `print()` erscheint nicht/verspaetet ohne TTY
**Symptom:** In Hooks/CI/Subprozessen erscheint `print()`-Ausgabe gar nicht oder erst beim
Programmende; Reihenfolge mit stderr durcheinander; bei Crash davor ist die Ausgabe weg.
**Ursache:** Ist stdout kein TTY (Pipe/Datei), ist sie **block-buffered** statt line-buffered.
**Versionen:** per Design, alle 3.x.
**FIX:** `print(..., flush=True)` ODER `python -u` ODER `PYTHONUNBUFFERED=1`. Fuer Hooks, die
sofort Output liefern muessen, Pflicht.
**Quelle:** bpo-41449, bpo-13601; adamj.eu output-buffering.

---

## 2. UTF-8 BOM

### 2.1 `utf-8` strippt das BOM nicht — Notepad/Excel/PowerShell-Dateien beginnen mit `﻿`  ⭐ HAEUFIG
**Symptom:** `json.load` wirft `JSONDecodeError: Unexpected UTF-8 BOM (decode using utf-8-sig)`;
ODER das erste CSV/JSON-Feld hat ein unsichtbares `﻿` vorangestellt (`data['﻿name']`
statt `data['name']`, Header-/Schluessel-Vergleiche schlagen fehl).
**Ursache:** Datei beginnt mit BOM `EF BB BF` (U+FEFF). Der `utf-8`-Codec strippt es NICHT — es
landet als Zeichen in den Daten. Wer schreibt BOM: **Notepad** ("UTF-8 mit BOM"), **PowerShell
`Out-File`/`>`/`Set-Content`** (PS 5.1 default BOM; PS 6+ default ohne), **Excel** "CSV UTF-8".
**Versionen:** per Design, alle Versionen.
**FIX:** Beim **Lesen** `encoding='utf-8-sig'` (entfernt BOM falls vorhanden, liest auch
BOM-lose Dateien korrekt). Beim **Schreiben** fuer Linux/Web/JSON `encoding='utf-8'` (OHNE BOM).
Bestehende Datei BOM-strippen. (Gleiche Klasse wie der Settings-BOM-Bug aus `claude-hooks.md`
12.1 — dort PowerShell-seitig; hier Python-seitig.)
**Quelle:** docs codecs; bpo-21509 (json), bpo-7185 (csv).

---

## 3. Pfade

### 3.1 Git-Bash-Pfade `/c/Users/...` → `FileNotFoundError` in Python  ⭐ HAEUFIG (Frank-Setup!)
**Symptom:** `FileNotFoundError: [Errno 2] No such file or directory: '/c/Users/barwa/...'`,
obwohl `ls`/`cat` denselben Pfad finden.
**Ursache:** `python.exe` versteht den MSYS-Mount-Stil `/c/Users/...` NICHT — das ist ein
Git-Bash/MSYS-internes Format. Die MSYS-Auto-Konvertierung greift nur bei manchen Aufrufen,
NICHT bei String-Literalen im Python-Code. Tritt auf, wenn Python aus Git Bash gestartet wird.
**Versionen:** per Design (MSYS-Eigenheit), alle Python-Versionen.
**FIX:** Im Python-Code NIE `/c/Users/...` hardcoden. Stattdessen `os.path.expanduser('~')`,
`pathlib.Path.home()`, native Windows-Pfade. Fuer CLI-Argumente an `python.exe`: `cygpath -m`.
(Deckt sich exakt mit der Projektregel `platform-and-paths.md` Abschnitt 3.)
**Quelle:** eigener wiederkehrender Vorfall; MSYS-Path-Pitfall-Doku.

### 3.2 Backslash-Escape im String-Literal (`"C:\Users\neu"`)
**Symptom:** Pfad enthaelt unerwartet ein Newline (`\n` aus `\neu`) oder `\U`-Fehler;
`SyntaxWarning: invalid escape sequence '\U'` (kuenftig `SyntaxError`).
**Ursache:** `\U` startet eine Unicode-Escape (braucht 8 Hex-Ziffern), `\n`/`\t`/`\x` sind
Escapes. Der Pfad-Backslash kollidiert damit.
**Versionen:** `DeprecationWarning` ab 3.6, **`SyntaxWarning` ab 3.12** (#98401 COMPLETED),
`SyntaxError`-Eskalation geplant (noch NICHT in 3.13 aktiv).
**FIX:** Raw-String `r"C:\Users\neu"`, doppelte Backslashes `"C:\\Users\\neu"`, oder
Forward-Slashes `"C:/Users/neu"` (Windows-APIs akzeptieren `/`), oder `pathlib`/`os.path.join`.
**Quelle:** cpython #98401 (gh: CLOSED/COMPLETED 2022); adamj.eu invalid-escape.

### 3.3 Lange Pfade > 260 Zeichen (MAX_PATH)
**Symptom:** `FileNotFoundError: [WinError 3] The system cannot find the path specified` oder
`OSError`, obwohl der Pfad korrekt — nur zu lang. Betrifft `open`, `os.listdir`, `glob`, `shutil`.
**Ursache:** Win32-API-Limit `MAX_PATH = 260`. Haeufig bei tiefen Baeumen (node_modules,
pip-Targets).
**Versionen:** per Design (Windows-Limit); Python long-path-faehig ab 3.6, wenn aktiviert.
**FIX:** (a) `\\?\`-Praefix vor absolute, normalisierte Backslash-Pfade
(`r"\\?\C:\sehr\langer\pfad"`) — hebt das Limit auf; (b) systemweit `LongPathsEnabled=1`
(Registry/Gruppenrichtlinie); Python-Installer bietet "Disable path length limit".
**Quelle:** MS Learn MAX_PATH; bpo-18199; docs Using Python on Windows.

### 3.4 Trailing dot/space in Pfaden wird still entfernt
**Symptom:** `os.mkdir("foo ")` erzeugt Ordner `foo`; `os.path.exists("report.")` /
`os.stat()` liefert falsches Ergebnis fuer Pfade mit Endpunkt/-Leerzeichen.
**Ursache:** Die Windows-`CreateFile`-Normalisierung strippt trailing dots und spaces der
letzten Pfadkomponente. Python erbt das. `os.stat` "gelingt" sogar fuer nicht existierende
Trailing-Space-Pfade (#84419, **OPEN**).
**Versionen:** per Design (Win32-API); #84419 noch offen auf 3.13.
**FIX:** Trailing dot/space vermeiden (`.rstrip()` vor Nutzung) ODER `\\?\`-Praefix (umgeht
die Normalisierung).
**Quelle:** cpython #84419 (gh: OPEN), #85681, #115104, bpo-22744.

### 3.5 Reservierte Geraetenamen (CON, NUL, AUX, PRN, COM1-9, LPT1-9)
**Symptom:** Datei `CON`, `NUL`, `AUX.txt` etc. anlegen/schreiben schlaegt fehl oder verhaelt
sich seltsam (`NUL` verschluckt allen Output); irrefuehrende Fehlermeldung.
**Ursache:** Windows reserviert diese Geraetenamen systemweit auf JEDEM Pfad — auch mit Endung
(`CON.txt`). Python reicht das durch.
**Versionen:** per Design (Windows-Eigenheit).
**FIX:** Generierte Dateinamen gegen die Reserved-Liste pruefen (case-insensitive, mit/ohne
Endung) und ggf. umbenennen. Funktion bleibt erhalten, nur der Name wird abgesichert.
**Quelle:** meziantou.net reserved-filenames; bpo-37517.

### 3.6 Umlaut im Datei-/Verzeichnisnamen → `FileNotFoundError`
**Symptom:** Datei mit Umlaut im Pfad existiert, `open()` wirft `FileNotFoundError: [Errno 2]`.
**Ursache:** Fast immer ein **falsch dekodierter Pfad-String** (Pfad aus einer cp1252-gelesenen
Quelle, Subprozess-Output oder gemischter Quellkodierung). Das Windows-Filesystem ist seit
Python 3.6 selbst UTF-8/Unicode-faehig (PEP 529) — der Fehler liegt im String, nicht im FS.
**Versionen:** quellenabhaengig.
**FIX:** Pfad korrekt als `str` (Unicode) fuehren; Quelle mit korrektem `encoding=` dekodieren;
`pathlib.Path` nutzen. Bytes-Pfade nur via `os.fsencode(path)`, NIE `path.encode('mbcs')`.
**Quelle:** PEP 529; netcdf4-python #941.

### 3.7 Manuelle Pfad-Konkatenation mit `\` ist nicht portabel
**Symptom:** Mit `\` gebaute Pfadstrings brechen auf macOS/Linux; gemischte `/` und `\` geben
schwer reproduzierbare Fehler.
**Ursache:** Hardcodierter Trenner ist plattformgebunden.
**Versionen:** per Design; `pathlib` ab 3.4 stabil.
**FIX:** `pathlib.Path` (`Path('a') / 'b' / 'c'`), `os.path.join`, `os.sep`. Fuer Ausgabe an
Tools, die `/` erwarten: `Path(...).as_posix()`.
**Quelle:** docs Using Python on Windows.

---

## 4. Zeilenenden / CSV

### 4.1 `csv.writer` ohne `newline=''` → doppelte Leerzeilen (NUR Windows)  ⭐ HAEUFIG
**Symptom:** Geschriebene CSV hat eine **leere Zeile zwischen jeder Datenzeile**.
**Ursache:** Das `csv`-Modul schreibt selbst `\r\n`; im Text-Default-Modus uebersetzt Windows
zusaetzlich `\n` → `\r\n` → Ergebnis `\r\r\n` = sichtbare Leerzeile.
**Versionen:** per Design ab Python 3 auf Windows (bpo-7198, als "Doku/Design" geschlossen).
**FIX:** Datei mit `newline=''` oeffnen: `open('out.csv', 'w', newline='', encoding='utf-8')`.
Das csv-Modul verwaltet die Zeilenenden dann selbst. Fuer Excel + Umlaute: `encoding='utf-8-sig'`
(BOM → Excel erkennt UTF-8, sonst `Ã¼` statt `ü`).
**Quelle:** bpo-7198; docs csv.

### 4.2 Textmodus wandelt `\n` → `\r\n` (kaputte JSON/NDJSON, abweichende Hashes)
**Symptom:** Generierte Datei/Protokoll-Stream hat `\r\n` statt `\n`; NDJSON/JSON-RPC bricht;
Diffs/Hashes weichen plattformuebergreifend ab.
**Ursache:** `open(path, 'w')` mit Default `newline=None` uebersetzt auf Windows jedes `\n` in
`\r\n`.
**Versionen:** per Design auf Windows.
**FIX:** `open(path, 'w', encoding='utf-8', newline='\n')` (oder `newline=''`) — schaltet die
Uebersetzung ab, bleibt Textmodus. Wichtig fuer generierte JSON-/Config-Dateien und alles, was
LF erwartet.
**Quelle:** PEP 278; modelcontextprotocol/python-sdk #2433 (CRLF korrumpierte NDJSON).

---

## 5. Atomares Schreiben / Datei-Korruption (Crash-Sicherheit)

### 5.1 Truncation-on-open: `'w'` leert die Datei SOFORT → abgeschnittene Datei bei Crash  ⭐ KRITISCH
**Symptom:** Crash/Abbruch waehrend `open(p,'w')...write()` → Datei ist leer oder halb
geschrieben, alter Inhalt komplett weg. Kritische Configs/JSON danach unlesbar.
**Ursache:** Mode `'w'` truncatet das Ziel beim Oeffnen sofort auf 0 Bytes, BEVOR etwas
geschrieben wird. `with open(...)` garantiert nur `close()` (kein Leak) — **NICHT** Atomizitaet.
**Versionen:** per Design, alle CPython.
**FIX (das eine sichere Pattern):**
```python
import os, tempfile, json

def atomic_write(target, text):
    d = os.path.dirname(os.path.abspath(target))
    with tempfile.NamedTemporaryFile('w', dir=d, suffix='.tmp', delete=False,
                                     encoding='utf-8', newline='\n') as tmp:
        tmp.write(text)
        tmp.flush()
        os.fsync(tmp.fileno())      # auf Platte zwingen (siehe 5.5)
        tmp_path = tmp.name
    os.replace(tmp_path, target)    # atomarer Swap (siehe 5.2)

# JSON sicher: erst zu String, dann atomar (siehe 5.9)
atomic_write('out.json', json.dumps(data, ensure_ascii=False, indent=2))
```
**Quelle:** ActiveState recipe 579097; eigene Projektregel `resilient-bugfixing.md`
(safe_json_write / atomares Schreiben).

### 5.2 `os.replace` statt `os.rename` (Windows: rename failt wenn Ziel existiert)  ⭐ HAEUFIG
**Symptom:** `os.rename(tmp, target)` wirft `FileExistsError`/`OSError` auf Windows, wenn
`target` schon existiert.
**Ursache:** Auf Windows ueberschreibt `os.rename` kein existierendes Ziel (POSIX tut es).
`os.replace` nutzt `MoveFileEx` mit `MOVEFILE_REPLACE_EXISTING` und ist plattformuebergreifend
atomar + ueberschreibend.
**Versionen:** `os.replace` seit 3.3; Verhalten per Design.
**FIX:** IMMER `os.replace(tmp, target)` fuer den atomaren Swap, nie `os.rename`.
**Quelle:** docs os.replace; zetcode os.replace.

### 5.3 `PermissionError [WinError 32]` — Datei wird von anderem Prozess benutzt
**Symptom:** `os.replace`/`os.remove`/`os.unlink`/`shutil.rmtree` wirft `PermissionError:
[WinError 32] The process cannot access the file because it is being used by another process`.
**Ursache:** Windows verbietet replace/delete einer noch geoeffneten Datei — durch nicht
geschlossenen Reader, `mmap`, Editor, parallelen Prozess ODER Virenscanner/Indexer, der die
frische Datei kurz oeffnet. (Unix erlaubt Rename ueber offene Dateien.)
**Versionen:** per Design (Windows-Filelocking).
**FIX:** Alle eigenen Handles vor `replace`/`delete` schliessen (nicht im offenen `with`-Block
ersetzen). Gegen Scanner-Race kurze Retry-Schleife mit Backoff:
```python
import time
for attempt in range(5):
    try:
        os.replace(tmp_path, target); break
    except PermissionError:          # WinError 32
        if attempt == 4: raise
        time.sleep(0.1 * (attempt + 1))
```
**Quelle:** pypa/pip #7865; JetBrains PY-31712.

### 5.4 `NamedTemporaryFile` kann auf Windows nicht erneut geoeffnet werden
**Symptom:** `tempfile.NamedTemporaryFile()` erstellen → mit `open(tmp.name)` erneut oeffnen →
`PermissionError [WinError 32]`. Oder beim Context-Exit failt das automatische unlink.
**Ursache:** `NamedTemporaryFile` haelt die Datei mit exklusivem Zugriff offen; ein zweites
`open()` desselben Pfads ist auf Windows verboten, solange das Tempfile offen ist.
**Versionen:** per Design auf Windows; `delete_on_close=False` neu ab **3.12** (auf 3.13.13
verfuegbar).
**FIX:** `delete=False` setzen, Datei schliessen, dann ersetzen/erneut oeffnen, am Ende
`os.unlink`. Ab 3.12 alternativ `NamedTemporaryFile(..., delete=False, delete_on_close=False)`
(Datei bleibt beim Schliessen erhalten). Im Atomic-Pattern (5.1) bereits korrekt geloest.
**Quelle:** docs tempfile; bpo-14243; scivision Python-tempfile-Windows.

### 5.5 Fehlendes `flush()`+`os.fsync()` vor `os.replace` → Datenverlust trotz "atomar"
**Symptom:** Atomic-Write korrekt benutzt, aber nach Stromausfall ist die Datei TROTZDEM 0 Bytes.
**Ursache:** Ohne `fsync` liegen die Daten nur im OS-Page-Cache. `os.replace` ist atomar bzgl.
des Dateinamens, garantiert aber NICHT, dass der Tempfile-Inhalt schon physisch auf Disk ist.
Crash zwischen replace und Cache-Flush → Name zeigt auf leeren Block.
**Versionen:** per Design.
**FIX:** Strikte Reihenfolge `write()` → `flush()` → `os.fsync(fileno())` → `close` →
`os.replace` (im Pattern 5.1 enthalten).
**Quelle:** blog.elijahlopez.ca data-corruption-atomic-writing; zetcode os.fsync.

### 5.6 Directory-fsync (volle Durability) ist auf Windows nicht moeglich
**Symptom:** Das auf Linux uebliche `os.fsync` des ENTHALTENDEN Verzeichnisses nach `os.replace`
crasht auf Windows.
**Ursache:** Windows kann ein Verzeichnis nicht als File-Descriptor zum fsync oeffnen
(`os.open(dir, O_DIRECTORY)` gibt es nicht).
**Versionen:** per Design (keine dir-fd-fsync-API auf Windows).
**FIX:** Directory-fsync nur auf POSIX ausfuehren, auf Windows ueberspringen (`MoveFileEx`
flusht die Metadaten ausreichend):
```python
if os.name != 'nt':
    dfd = os.open(d, os.O_RDONLY)
    try: os.fsync(dfd)
    finally: os.close(dfd)
```
**Quelle:** HN Unix-file-durability; bpo-8604.

### 5.7 `os.replace` cross-device (EXDEV) — Tempfile im falschen Verzeichnis
**Symptom:** `OSError: [Errno 18] EXDEV: Invalid cross-device link` (bzw. Windows-Fehler), wenn
Tempfile auf anderem Laufwerk/Mount liegt als das Ziel (z.B. Tempfile in `%TEMP%` auf C:, Ziel
auf D:).
**Ursache:** Atomarer Rename geht nur INNERHALB desselben Dateisystems.
**Versionen:** per Design.
**FIX:** Tempfile IMMER mit `dir=os.path.dirname(target)` im selben Verzeichnis erstellen (im
Pattern 5.1 enthalten). Echtes Cross-Device: `shutil.copy2` + delete (dann NICHT mehr atomar —
loggen).
**Quelle:** zetcode os.replace.

### 5.8 Parallele Prozesse schreiben dieselbe Datei (Race) → Korruption
**Symptom:** Zwei Prozesse/Sessions schreiben gleichzeitig → verschraenkte/abgeschnittene
Inhalte. `import fcntl` crasht auf Windows mit `ImportError`.
**Ursache:** Keine OS-uebergreifende Locking-API: `fcntl.flock` ist Unix-only,
`msvcrt.locking` Windows-only (Byte-Ranges). Beide advisory (kooperativ).
**Versionen:** per Design.
**FIX:** Cross-platform `filelock` oder `portalocker` (waehlt intern fcntl/msvcrt), separate
Lock-Datei. Atomic-Write (5.1) + Lock = sicher gegen Race UND Crash:
```python
from filelock import FileLock
with FileLock(target + ".lock"):
    atomic_write(target, text)
```
**Quelle:** py-filelock docs; portalocker recipe.

### 5.9 `json.dump` schreibt inkrementell → kaputtes JSON bei Crash
**Symptom:** Crash waehrend `json.dump(data, open(p,'w'))` → JSON mittendrin abgeschnitten
(`{"a": 1, "b":`), naechstes Lesen `JSONDecodeError`.
**Ursache:** `json.dump` serialisiert/schreibt stueckweise in den Stream. (Zusatzfalle Windows:
ohne `encoding='utf-8'` cp1252-Crash bei Umlauten.)
**Versionen:** per Design.
**FIX:** Erst komplett zu String (`json.dumps(data, ensure_ascii=False, indent=2)`), dann atomar
schreiben (5.1). So ist die Datei entweder der alte ODER der vollstaendige neue Inhalt.
**Quelle:** BSWEN atomic-file-writing; ActiveState recipe.

---

## 6. venv / PATH / Interpreter-Auswahl

### 6.1 Microsoft-Store-Alias `python.exe`/`python3.exe` oeffnet den Store  ⭐ HAEUFIG
**Symptom:** `python` im Terminal oeffnet den Microsoft Store; `where python` zeigt einen Pfad
in `...\WindowsApps`; `python3 skript.py` startet eine falsche/leere Installation.
**Ursache:** Windows 10/11 hat **App-Execution-Aliases** fuer `python.exe`/`python3.exe` in
`%LOCALAPPDATA%\Microsoft\WindowsApps`. Stehen die im PATH vor dem echten Python, greift der
Store-Stub.
**Versionen:** Windows-10/11-Feature; betrifft jede Installation.
**FIX:** Einstellungen → "App-Ausfuehrungsaliase verwalten" → `App Installer (python.exe /
python3.exe)` deaktivieren; echtes Python in PATH vor WindowsApps. In Skripten besser den
`py`-Launcher oder absoluten Interpreterpfad nutzen.
**Quelle:** MS Q&A App-Execution-Alias; dev.to Python-was-not-found.

### 6.2 `python` vs `python3` vs `py`; Shebangs werden auf Windows ignoriert
**Symptom:** `python3 skript.py` schlaegt fehl ("not recognized"); `#!/usr/bin/env python3`-
Shebang hat keine Wirkung; falsche Python-Version laeuft.
**Ursache:** `python3` existiert auf Windows oft NICHT nativ (nur `python`/`py`). Shebangs
beachtet nur der **py-Launcher** (`py -3.13`), nicht `python.exe` direkt.
**Versionen:** per Design (Windows).
**FIX:** Plattformuebergreifend `py -3` (Windows) bzw. `python3` (Unix) bewusst trennen. Der
Windows-Installer legt NUR `python.exe`+`pythonw.exe` an — KEIN `python3.exe`. Dadurch brechen alle
Hooks/Skripte die `python3` aufrufen (viele Plugin-Hooks, `invariant-check.sh`, der finale-Guard
`pretooluse-bash.sh` — der ist fail-closed und blockiert dann die GANZE Session). Zwei-Schichten-Fix
(lokal umgesetzt 2026-07-08):
1. **Native Windows-Prozesse** (PowerShell-Hooks, neue Shells): Kopie `python3.exe` NEBEN `python.exe`
   im echten Verzeichnis (`…\Programs\Python\Python313\`) — dort liegen `python313.dll`+`Lib\`, es
   startet. Dieses Verzeichnis steht im Registry-PATH vor `WindowsApps`.
2. **Git-Bash-Hooks** (erben den eingefrorenen Prozess-PATH): Shell-Shim `~/bin/python3`
   (`#!/bin/sh` + `exec "…/Launcher/py.exe" -3 "$@"`). Git-Bash stellt `~/bin` dem PATH IMMER voran
   (unabhaengig vom Windows-PATH), der py-Launcher findet automatisch die neueste Version (update-sicher).
**Sackgassen (getestet, NICHT nutzen):** python.exe in einen fremden Ordner KOPIEREN/SYMLINKEN
scheitert (`STATUS_DLL_NOT_FOUND` 0xC0000135 — der Interpreter sucht `python313.dll` am Link-/Kopie-Ort,
nicht am Ziel). Ablage in `WindowsApps` scheitert (Exit 53 — der App-Execution-Alias faengt jeden Aufruf
dort ab, egal welche Datei liegt). Eine `.cmd`/`.bat` wird von Git-Bash NICHT als `python3` erkannt —
es MUSS ein Script namens `python3` OHNE Endung mit Shebang sein. `.cargo/bin` ist NICHT im
Git-Bash-non-login-PATH — verlaesslich sind nur `~/bin`, `/usr/bin`, `/mingw64/bin`.
**Quelle:** docs Using Python on Windows; joshkel python-2-3-shebangs; eigener Vorfall 2026-07-08
(python3 → leerer MS-Store-Stub, danach finale-Guard fail-closed → gesamte Session blockiert).

### 6.3 ExecutionPolicy blockiert `venv\Scripts\Activate.ps1`
**Symptom:** `.\venv\Scripts\Activate.ps1` → "running scripts is disabled on this system"
(`UnauthorizedAccess`).
**Ursache:** Windows-PowerShell-ExecutionPolicy verbietet unsignierte Skripte.
**Versionen:** per Design (Windows).
**FIX:** `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned` (einmalig), ODER
`venv\Scripts\activate.bat` (cmd) statt `.ps1`, ODER direkt `venv\Scripts\python.exe` aufrufen
(kein Activate noetig). Hier global ohnehin `bypassPermissions`-Setup — aber ExecutionPolicy ist
davon unabhaengig.
**Quelle:** MS Q&A Activate.ps1; AskPython virtualenv-activation.

### 6.4 `pip install` vs `python -m pip` (falscher pip bei mehreren Interpretern)
**Symptom:** Paket installiert, aber `import` schlaegt fehl ("ModuleNotFoundError"); `pip` zeigt
auf einen anderen Interpreter als der, mit dem das Skript laeuft.
**Ursache:** `pip` (Standalone) und `python` koennen auf verschiedene Installationen zeigen.
**Versionen:** per Design.
**FIX:** IMMER `python -m pip install ...` (bzw. `py -3 -m pip`) — bindet pip an genau den
Interpreter. In venv: zuerst aktivieren / `venv\Scripts\python.exe -m pip`.
**Quelle:** pip-Doku --python.

### 6.5 PATH-Reihenfolge bei mehreren Python-Versionen
**Symptom:** `python` startet eine andere Version als erwartet; `where python` zeigt mehrere
Treffer.
**Ursache:** Mehrere Installationen im PATH; der erste Treffer gewinnt.
**Versionen:** per Design.
**FIX:** `where python` (Windows) / `which -a python3` (Unix) pruefen; `py -0p` listet alle
Launcher-Versionen mit Pfad; gewuenschte Version per `py -3.13` waehlen.
**Quelle:** docs Using Python on Windows.

### 6.6 venv ist NICHT portabel (kaputt nach Move/Copy/Rename)
**Symptom:** Verschobenes/kopiertes/umbenanntes venv-Verzeichnis → `python`/`pip` darin brechen.
**Ursache:** `pyvenv.cfg` und die `Scripts`-Shims enthalten absolute Pfade auf den Basis-
Interpreter und das venv-Verzeichnis.
**Versionen:** per Design.
**FIX:** venv nicht verschieben/umbenennen — neu erzeugen (`py -m venv .venv`) und
`requirements.txt` neu installieren. venv niemals ins Repo committen.
**Quelle:** docs venv; bpo-39469.

---

## 7. subprocess / Shell-Mechanik (Cross-Platform)

### 7.1 `shlex` ist POSIX-only — falsches Quoting/Splitting auf Windows
**Symptom:** `shlex.split(r"C:\path\to app.exe --x")` zerlegt Backslashes/Pfade falsch;
`shlex.quote()` produziert auf cmd.exe unsichere/falsche Strings.
**Ursache:** `shlex` ist fuer Unix-Shells gebaut (Backslash = Escape statt Pfadtrenner).
`posix=False` heisst NICHT "Windows-Modus".
**Versionen:** per Design.
**FIX:** Args IMMER als **Liste** an `subprocess` geben (`["prog", "--x", "wert"]`) — Python
quotet selbst korrekt; `shell=True`/`shlex` fuer Windows-Kommandos vermeiden.
**Quelle:** docs shlex; bpo-1724822.

### 7.2 `shell=True` / `.bat`-Quoting unterscheidet sich Windows vs Unix
**Symptom:** Befehl mit Leerzeichen/Sonderzeichen laeuft auf Linux, scheitert/verhaelt sich
anders auf Windows; `.bat`/`.cmd` parsen Argumente nach eigenen Regeln.
**Ursache:** Auf Windows starten `.bat`/`.cmd` ueber die System-Shell und parsen Argumente nach
cmd-Regeln, OHNE Pythons Escaping. String-Form von `args` wird plattformabhaengig interpretiert.
**Versionen:** per Design.
**FIX:** `shell=False` + Listen-Args als Default. Wenn `shell=True` unvermeidbar:
plattformspezifisch quoten, Eingaben strikt validieren.
**Quelle:** docs subprocess.

---

## 8. Fix-Status — was auf 3.13.13 schon behoben ist (gh-verifiziert)

> Versions-Denken: Diese Eintraege waren frueher Bugs, sind aber auf der installierten Version
> bereits GEFIXT oder gelten nur fuer aeltere Versionen — auf 3.13.13 NICHT mehr als aktive
> Bugs behandeln. Stati am 2026-06-02 per `gh issue view` hart geprueft.

| Frueheres Problem | Status / gefixt | Bezug |
|-------------------|-----------------|-------|
| Windows-Konsole druckt/liest kein Unicode | **gefixt ab 3.6** (PEP 528, WindowsConsoleIO) | 1.4 |
| Windows-Filesystem-Encoding via mbcs (Bytes-Pfade kaputt) | **gefixt ab 3.6** (PEP 529, FS=utf-8) | 3.6 |
| `EncodingWarning` existiert nicht | **eingefuehrt 3.10**, mehr Stellen 3.11 (#91954 COMPLETED) | 1.2 |
| `encoding="locale"`-Sentinel wurde ignoriert | **gefixt ab 3.11** | 1.1 |
| `logging.basicConfig` ohne `encoding=`-Param | **`encoding=` ab 3.9** (#37111) | 1.7 |
| `NamedTemporaryFile` kein `delete_on_close` | **neu ab 3.12** (auf 3.13.13 da) | 5.4 |
| `pythonw.exe` aus venv oeffnet immer Konsole (3.13.0-Regression) | **CLOSED/COMPLETED** 2024-10-29, in 3.13.x gefixt (#126084) | venv |
| Ungueltige String-Escapes still erlaubt | DeprecationWarning 3.6 → **SyntaxWarning 3.12** (#98401), SyntaxError spaeter | 3.2 |

### Noch NICHT gefixt auf 3.13.13 (Workaround bleibt aktiv)
Per Design oder Fix erst in spaeterer Version — Loesungen oben weiter anwenden:
- `open()`/stdio/`subprocess` Default = cp1252 (1.1, 1.4, 1.6) — **UTF-8-Default erst 3.15** (PEP 686).
- `subprocess.run()` falsches Text-Encoding (1.6, #105312) — Issue **2026-03-16 als COMPLETED**
  geschlossen, Fix landet aber mit dem UTF-8-Default in **3.15**; auf 3.13.13 NICHT enthalten.
- `json.dump` `ensure_ascii=True`-Default (1.5) — per Design.
- UTF-8-BOM wird von `utf-8` nicht gestrippt (2.1) — per Design (`utf-8-sig` beim Lesen).
- csv `\r\r\n` ohne `newline=''` (4.1) — per Design.
- Truncation-on-open / `os.rename`-Semantik / WinError 32 / fehlendes fsync (5.x) — per Design.
- MAX_PATH 260 (3.3), trailing dot/space (3.4, #84419 **OPEN**), reservierte Namen (3.5) — Win32-API.
- Git-Bash `/c/...`-Pfade (3.1) — MSYS-Eigenheit, kein Python-Bug.
- MS-Store-Alias, ExecutionPolicy, venv-Nicht-Portabilitaet, shlex/shell-Quoting (6.x, 7.x) — per Design.

**Methodik-Hinweis:** Die "gefixt"-Angaben mit Issue-Nummer sind per `gh issue view <nr> --repo
python/cpython` hart verifiziert (echter OPEN/CLOSED-Status + closedAt). "Per Design"-Eintraege
sind dokumentiertes Verhalten (PEP/Doku), kein Tracker-Fix zu erwarten. Bei Re-Recherche nach
Erscheinen von 3.14/3.15: 1.x und 1.6 erneut pruefen (UTF-8-Default aendert die halbe Liste).

---

## 9. Allgemeine Python-Fallen (plattformunabhaengig)

> Diese Eintraege sind KEINE Windows/Encoding-Spezialitaeten, sondern allgemeine Python-
> Fallstricke, die auf JEDER Plattform (auch macOS/Linux) auftreten. Hier gesammelt, weil es
> (noch) keinen eigenen allgemeinen Python-Almanach gibt. Aufnahme-Schwelle wie ueberall:
> nur harte, sicher bestaetigte Bugs (siehe `SYSTEM.md` §4).

### 9.1 `re.sub`-Replacement mit `\U`/`\g` in Raw-String → `re.PatternError: bad escape`  ⭐
**Symptom:** `re.sub(r'^(#+ )', r'\g<1>\U0001f517 ', text)` crasht sofort mit
`re.PatternError: bad escape \U at position N` — der Code laeuft nie an.
**Ursache:** Der **Replacement**-String von `re.sub` hat eine EIGENE Mini-Sprache
(`\g<1>`, `\1`, `\\`), die `re` selbst parst. Schreibt man das Replacement als Raw-String
(`r'...'`), wird ein gewuenschtes Unicode-Escape wie `\U0001f517` (🔗) NICHT vom Python-Parser
zum Zeichen aufgeloest, sondern landet literal im Template — und `re` interpretiert `\U` dort
als ungueltige Replacement-Escape-Sequenz. (Gleiche Wurzel wie 3.2, nur im re-Replacement
statt im String-Literal.)
**Versionen:** per Design, alle 3.x; Eskalation von Warning zu hartem `PatternError` ab
**3.12** (auf 3.13/3.14 voll aktiv). Live reproduziert auf 3.14.3.
**FIX:** Replacement NIE als Raw-String mit Unicode-Escape mischen. Entweder Lambda-
Replacement (umgeht das Template komplett — robusteste Loesung):
```python
re.sub(r'^(#+ )', lambda m: m.group(1) + '\U0001f517 ', text, count=1)
```
oder das Sonderzeichen vorab in eine normale (Nicht-Raw-)Variable legen und einsetzen
(`EMOJI = '\U0001f517'` → `lambda m: m.group(1) + EMOJI + ' '`).
**Quelle:** docs.python.org/3/library/re.html (Replacement-String-Syntax); eigener Vorfall
2026-06-05 (Bug-Almanach/Best-Practices-Kopplungs-Fix).

---

## Pflicht-Checkliste vor Python-Arbeit auf Windows
- [ ] Diese Datei gelesen, Stand-Datum gegen `python --version` abgeglichen (3.13.13)?
- [ ] JEDES `open()` mit `encoding='utf-8'` (Lesen evtl. `utf-8-sig`)?
- [ ] JSON: `ensure_ascii=False` UND `encoding='utf-8'` (beides)?
- [ ] Kritische Datei/Config/JSON atomar geschrieben (tempfile im selben Ordner → flush → fsync
      → `os.replace`), JSON vorher per `dumps` zu String?
- [ ] `subprocess` mit `encoding='utf-8', errors='replace'`; Args als Liste (kein `shlex`/`shell=True`)?
- [ ] `print()`/`logging` mit Umlaut/Emoji abgesichert (`reconfigure`/`FileHandler(encoding=)`/
      `flush=True` bei Pipe/Hook)?
- [ ] Pfade portabel (`pathlib`/`expanduser`), KEIN `/c/Users/...` im Code, Backslash-Literale
      als Raw-String, `newline='\n'` bei generierten Text-/JSON-Dateien?
- [ ] Bei CSV: `newline=''`? Interpreter via `py -3`/`python -m pip` korrekt gewaehlt (nicht
      MS-Store-Stub)?

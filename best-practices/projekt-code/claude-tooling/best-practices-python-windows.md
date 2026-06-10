# Python auf Windows / Cross-Platform-Scripting — Best Practices

> **Zweite Seite der Medaille zum Bug-Almanach** `~/proggs/bugs/claude-tooling/python-windows.md`: dort steht
> *was schiefgeht und wie man es umgeht*, hier *wie man es von vornherein richtig macht, damit der
> Bug gar nicht erst entsteht*. Jeder Abschnitt verweist auf seinen Bug-Gegenpart (Bezugs-Tabelle).
>
> **Stand:** recherchiert **2026-06-02** (offizielle Python-Quellen: docs.python.org, peps.python.org,
> packaging.python.org, pip.pypa.io). Versions-Anker = live ermittelt.
>
> | | benutzt (live) | Hinweis |
> |---|---|---|
> | CPython | **3.13.13** (`python --version`) | cp1252-Defaults fuer Dateien/Streams auf Windows |
> | UTF-8-Mode Default | **nein** (bis 3.14) | wird Default **ab 3.15** (PEP 686) |
> | Plattform-Ziel | Windows **und** macOS/Linux | identisches Verhalten ist das Ziel |
>
> **Kern-Anker:** Fast jeder Windows-Fallstrick ist eine Auspraegung EINES Unterschieds — macOS/Linux
> nutzen UTF-8 als Locale-Default ueberall, Windows die ANSI-Codepage (cp1252) fuer Dateien/Streams
> und die OEM-Codepage (z.B. cp850) fuer die Konsole. Die Best-Practices hier neutralisieren genau
> diesen Unterschied, damit derselbe Code auf beiden Plattformen identisch laeuft.

---

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

---

## Quellen-Rangordnung

Offizielle Python-Quellen (docs.python.org, peps.python.org, packaging.python.org, pip.pypa.io) =
**Grundwahrheit**. Tool-Hersteller-Quellen (Astral fuer ruff/uv, mypy, pyright, click/typer, pipx) und
Community/Blogs = `extern`-Alternative bzw. die jeweils offizielle Tool-Doku, klar gelabelt;
ueberstimmt nie das CPython-Offizielle. Jeder Eintrag traegt Quelle + Datum/Version + `offiziell`/`extern`.

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei) | Failure-Mode im Almanach `bugs/claude-tooling/python-windows.md` |
|---|---|
| §1 Encoding & Text-/JSON-I/O | §1 (1.1–1.9 cp1252 / print / json / subprocess / logging / Buffering), §2 (2.1 BOM), §4 (4.1–4.2 Zeilenenden/CSV) |
| §2 Atomares & crash-sicheres Datei-Schreiben | §5 (5.1–5.9 Truncation / os.replace / fsync / WinError 32 / EXDEV / Lock) |
| §3 Pfade plattformneutral & Cross-Platform-Mechanik | §3 (3.1–3.7 Git-Bash-Pfade / Escape / MAX_PATH / pathlib), §7 (7.1–7.2 shlex / shell-Quoting) |
| §4 venv & Dependency-Management | §6 (6.1–6.6 MS-Store-Alias / py vs python3 / ExecutionPolicy / python -m pip / venv nicht portabel) |
| §5 Robuste CLI-/Skript-Struktur | §1.4 (print bei Pipe/Redirect), §1.9 (Output-Buffering) |
| §6 Type Hints, moderne Sprachfeatures & Tooling | §3.2 (Backslash-Escape → SyntaxWarning), §1.2 (EncodingWarning), §8 (Fix-Status & Versions-Denken) |

---

## §1 Encoding & Text-/JSON-I/O

### 1.1 Dateien oeffnen — Encoding immer explizit

- **Regel:** Setze bei jedem `open()` im Textmodus `encoding='utf-8'` — niemals weglassen.
  Begruendung: Ohne Argument nutzt Python `locale.getencoding()`, das auf Windows `cp1252` ist, auf macOS/Linux meist `utf-8`. Genau dieser Unterschied erzeugt Cross-Platform-Bugs (Umlaute/Emoji werfen `UnicodeDecodeError`/`UnicodeEncodeError` nur auf Windows). PEP 597 belegt: 82 der 4000 meistgeladenen PyPI-Pakete schlagen auf Nicht-UTF-8-Locales fehl.
  ```python
  # falsch (plattformabhaengig):
  open(path)
  # richtig:
  open(path, encoding='utf-8')
  ```
  (Quelle: PEP 597; docs functions.html#open, Python 3.13, offiziell)

- **Regel:** Fuer reine Binaerdaten Binaermodus `'rb'/'wb'` nutzen und `encoding` weglassen.
  Begruendung: `encoding` darf nur im Textmodus gesetzt werden; Bytes brauchen keine Decodierung.
  (Quelle: docs functions.html#open, Python 3.13, offiziell)

### 1.2 BOM-behaftete Dateien lesen

- **Regel:** Beim Lesen von Dateien, die ein UTF-8-BOM enthalten koennen (oft aus Windows-Editoren/Excel-Export), `encoding='utf-8-sig'` verwenden.
  Begruendung: `utf-8-sig` entfernt das fuehrende BOM (`﻿`) automatisch beim Lesen, sonst landet es als unsichtbares Zeichen am Stringanfang und bricht z.B. JSON-Parsing oder Header-Vergleiche. Zum **Schreiben** dagegen `utf-8` (ohne `-sig`) nehmen, damit kein BOM erzeugt wird.
  ```python
  text = open(path, encoding='utf-8-sig').read()   # BOM wird geschluckt
  ```
  (Quelle: docs codecs.html (utf-8-sig), Python 3.13, offiziell)

### 1.3 JSON korrekt schreiben

- **Regel:** Schreibe JSON IMMER mit `json.dump(obj, fp, ensure_ascii=False)` UND oeffne die Datei mit `encoding='utf-8'` — beides zusammen.
  Begruendung: Die zwei Schalter wirken auf verschiedenen Ebenen. `ensure_ascii=False` verhindert, dass `json` Umlaute/Emoji zu `\uXXXX`-Escapes verstuemmelt (lesbarer, korrekter Output). `encoding='utf-8'` sorgt dafuer, dass der `TextIOWrapper` diese Nicht-ASCII-Zeichen ueberhaupt byteweise schreiben kann — ohne ihn faellt Python auf cp1252 zurueck und crasht beim ersten Emoji. Nur einer von beiden reicht nicht.
  ```python
  # falsch (Emoji wird \ud83d... oder crasht):
  json.dump(d, open(p, 'w'))
  # richtig:
  with open(p, 'w', encoding='utf-8') as f:
      json.dump(d, f, ensure_ascii=False, indent=2)
  ```
  (Quelle: docs json.html (ensure_ascii); PEP 597, Python 3.13, offiziell)

### 1.4 Newline-Handling fuer generierte Dateien

- **Regel:** Beim Schreiben generierter Text-/JSON-Dateien `newline='\n'` setzen, damit LF erhalten bleibt.
  Begruendung: Standardmaessig (`newline=None`) uebersetzt Python beim Schreiben jedes `\n` in `os.linesep` — auf Windows also `\r\n`. Das erzeugt CRLF und damit verrauschte Git-Diffs gegenueber macOS/Linux. Mit `newline='\n'` (oder `''`) findet keine Uebersetzung statt.
  ```python
  open(p, 'w', encoding='utf-8', newline='\n')
  ```
  (Quelle: docs functions.html#open (newline beim Schreiben), Python 3.13, offiziell)

- **Regel:** Beim `csv`-Schreiben `newline=''` an `open()` uebergeben (nicht im `csv.writer`).
  Begruendung: Das `csv`-Modul fuegt seine Zeilenenden selbst ein. Ohne `newline=''` verdoppelt der TextIOWrapper auf Windows die `\r` zu Leerzeilen zwischen den Datensaetzen.
  ```python
  with open(p, 'w', encoding='utf-8', newline='') as f:
      csv.writer(f).writerows(rows)
  ```
  (Quelle: docs csv.html, Python 3.13, offiziell)

### 1.5 pathlib mit explizitem Encoding

- **Regel:** Bei `Path.read_text()` / `Path.write_text()` immer `encoding='utf-8'` mitgeben.
  Begruendung: Dieselbe Locale-Falle wie bei `open()` — ohne Argument plattformabhaengig. `write_text` akzeptiert zusaetzlich `newline='\n'` (ab Python 3.10) fuer LF-Erhalt.
  ```python
  Path(p).write_text(s, encoding='utf-8', newline='\n')
  Path(p).read_text(encoding='utf-8')
  ```
  (Quelle: docs pathlib.html (read_text/write_text), Python 3.13, offiziell)

### 1.6 Globale Absicherung — UTF-8-Mode als Notbremse

- **Regel:** Setze `PYTHONUTF8=1` (oder starte mit `python -X utf8`) als Sicherheitsnetz fuer Fremdcode, den du nicht aendern kannst — aber verlasse dich im eigenen Code NICHT darauf.
  Begruendung: UTF-8-Mode (PEP 540) zwingt `open()` global auf UTF-8 und schaltet stdio auf `surrogateescape`. Das rettet fremde Bibliotheken, die ihr `encoding=` vergessen. Es ist aber eine Umgebungs-Einstellung, die in einer anderen Umgebung fehlen kann — explizites `encoding=` im eigenen Code ist robuster und reist mit dem Code mit.
  ```bash
  set PYTHONUTF8=1        # Windows-Notbremse fuer Fremdcode
  python -X utf8 script.py
  ```
  (Quelle: PEP 540; docs "Using Python on Windows", Python 3.13, offiziell)

### 1.7 Fallen beim Entwickeln aufspueren — EncodingWarning

- **Regel:** Aktiviere beim Entwickeln/Testen `-X warn_default_encoding` (oder `PYTHONWARNDEFAULTENCODING=1`), um jedes `open()` ohne `encoding=` aufzudecken.
  Begruendung: PEP 597 fuehrt `EncodingWarning` ein; sie feuert genau dann, wenn das `encoding`-Argument fehlt und die Locale-Default verwendet wird. So findest du vor dem Release alle impliziten Encoding-Annahmen — entscheidend als Vorbereitung auf Python 3.15 (siehe 1.9).
  ```bash
  python -X warn_default_encoding -W error::EncodingWarning script.py
  ```
  (Quelle: PEP 597, Python 3.13, offiziell)

### 1.8 print() / logging mit Umlauten und Emoji absichern

- **Regel:** Vor Ausgaben mit Sonderzeichen `sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')` setzen.
  Begruendung: stdout erbt sonst die Konsolen-Codepage (Windows: oft cp1252/cp850) und crasht bei Emoji. `reconfigure` aendert Encoding/Fehlerbehandlung zur Laufzeit; `errors='backslashreplace'` macht aus nicht darstellbaren Zeichen eine sichtbare Escape statt eines Absturzes. Hinweis: nur moeglich, solange noch nicht vom Stream gelesen wurde — beim Schreiben jederzeit erlaubt.
  ```python
  import sys
  sys.stdout.reconfigure(encoding='utf-8', errors='backslashreplace')
  ```
  (Quelle: docs io.html#io.TextIOWrapper.reconfigure; PEP 540, Python 3.13, offiziell)

- **Regel:** Bei `logging` in Dateien `logging.FileHandler(path, encoding='utf-8')` verwenden.
  Begruendung: Der FileHandler oeffnet die Logdatei sonst mit der Locale-Default und schreibt deutsche Logs auf Windows kaputt.
  (Quelle: docs logging.handlers (FileHandler encoding), Python 3.13, offiziell)

- **Regel:** In Hooks/Pipes `print(..., flush=True)` nutzen.
  Begruendung: Bei umgeleitetem stdout (Hook, Subprozess-Pipe) ist die Ausgabe block-gepuffert; ohne `flush=True` koennen Zeilen verloren gehen, wenn der Prozess vorzeitig endet.
  (Quelle: docs functions.html#print (flush), Python 3.13, offiziell)

### 1.9 Ausblick — PEP 686 (UTF-8 wird Default ab Python 3.15)

- **Regel:** Schreibe schon heute `encoding='utf-8'` explizit, statt auf den kommenden Default zu warten.
  Begruendung: PEP 686 macht UTF-8-Mode ab **Python 3.15** zum Default — dann verschwindet die cp1252-Falle weitgehend. Bis dahin (und fuer Rueckwaerts-Kompatibilitaet mit aelteren Interpretern) bleibt explizites `encoding=` die einzige Variante, die auf JEDER Python-Version und JEDER Plattform identisch laeuft. Wer heute schon `EncodingWarning`-frei ist, ist fuer 3.15 automatisch fertig.
  (Quelle: PEP 686, offiziell)

---

## §2 Atomares & crash-sicheres Datei-Schreiben

Kerngedanke: Ein direkter `open(target, "w")` setzt die Zieldatei sofort auf null Bytes. Stuerzt der Prozess (oder der Strom) zwischen `open` und `write` ab, ist die Datei kaputt. Die Loesung ist immer: in eine **temporaere Datei im selben Verzeichnis** schreiben, flushen, fsyncen, und dann mit **`os.replace`** ueber das Ziel ziehen — der Rename ist auf einem Dateisystem atomar, das Ziel ist also entweder die alte oder die neue Version, niemals ein Fragment.

### 2.1 `os.replace` statt `os.rename` verwenden

- **Regel:** Den finalen Tausch IMMER mit `os.replace(tmp, target)` machen, nie mit `os.rename`.
  Begruendung: Auf Windows wirft `os.rename` einen `FileExistsError`, wenn das Ziel schon existiert — der haeufigste Fall beim Ueberschreiben einer Config. `os.replace` ueberschreibt atomar und verhaelt sich auf POSIX und Windows identisch (intern: native `rename()` auf Unix, `MoveFileEx` mit Replace-Flag auf Windows). Damit braucht man keine Plattform-Weiche.
  ```python
  os.replace(tmp_path, target_path)  # atomar + ueberschreibend, cross-platform
  ```
  (Quelle: docs os.replace, Python 3.13, offiziell)

### 2.2 Temp-Datei im SELBEN Verzeichnis wie das Ziel anlegen

- **Regel:** Die temporaere Datei MUSS im selben Verzeichnis (= selbes Dateisystem) liegen wie das Ziel — niemals in `/tmp` bzw. `%TEMP%`.
  Begruendung: `os.replace`/`rename` ist nur innerhalb desselben Dateisystems atomar. Liegen Quelle und Ziel auf verschiedenen Mounts/Laufwerken, schlaegt der Rename mit `OSError errno 18` (`EXDEV`, "cross-device link") fehl. `%TEMP%` ist oft ein anderes Laufwerk als das Ziel — deshalb `dir=os.path.dirname(target)` setzen. (Ein `shutil.move`-Fallback bei `EXDEV` ist NICHT atomar und damit kein Ersatz.)
  (Quelle: docs os.replace/tempfile, offiziell; EXDEV-Mechanik: alexwlchan.net "Atomic, cross-filesystem moves", extern)

### 2.3 `flush()` + `os.fsync()` vor dem Schliessen erzwingen

- **Regel:** Nach dem Schreiben IMMER erst `f.flush()`, dann `os.fsync(f.fileno())`, dann schliessen — bevor `os.replace` laeuft.
  Begruendung: `os.replace` ist nur eine Metadaten-Operation. Ohne `fsync` liegen die Daten evtl. nur im Page-Cache des OS; bei Stromausfall ist die Datei trotz "atomarem" Rename leer oder alt. `flush()` leert nur Pythons Puffer in den OS-Cache — erst `os.fsync` zwingt das OS, auf die Platte zu schreiben (Windows: intern `_commit()`). Reihenfolge ist entscheidend: write → flush → fsync → close → replace.
  (Quelle: docs os.fsync ("flush, then os.fsync"), offiziell)

### 2.4 Directory-fsync nur auf POSIX (sauberer Cross-Platform-Guard)

- **Regel:** Nach dem `os.replace` zusaetzlich das Verzeichnis fsyncen — aber nur auf POSIX, mit `if os.name != "nt"`-Guard.
  Begruendung: Auf POSIX ueberlebt die Verzeichnis-Aenderung (der neue Dateiname) einen Crash erst nach einem fsync des Verzeichnis-Handles. Auf Windows laesst sich ein Verzeichnis nicht per `os.open` zum fsync oeffnen — der Versuch wirft einen Fehler, und er ist dort auch nicht noetig. Der Guard haelt das Pattern crash-frei plattformuebergreifend.
  ```python
  if os.name != "nt":
      dir_fd = os.open(os.path.dirname(os.path.abspath(target_path)) or ".", os.O_RDONLY)
      try:
          os.fsync(dir_fd)
      finally:
          os.close(dir_fd)
  ```
  (Quelle: docs os.fsync (Availability fuer Dateien "Unix, Windows"; Verzeichnis-fsync POSIX-spezifisch), offiziell; gaengige Engineering-Praxis, extern)

### 2.5 `NamedTemporaryFile` mit `delete=False` (Windows-Reopen-Falle)

- **Regel:** Beim atomic-write-Pattern `tempfile.NamedTemporaryFile(..., delete=False)` (oder `mkstemp`) verwenden und im Fehlerfall selbst aufraeumen.
  Begruendung: Auf Windows kann eine noch geoeffnete `NamedTemporaryFile` nicht durch einen Rename ersetzt werden (Datei-Handle haelt sie). Mit `delete=False` schliesst man die Datei sauber und fuehrt `os.replace` danach aus; das automatische Loeschen entfaellt (man raeumt im `except` selbst auf, falls der Rename scheitert). Ab 3.12 alternativ `delete_on_close=False`.
  (Quelle: docs tempfile.NamedTemporaryFile (Windows-Reopen-Bedingungen), offiziell)

### 2.6 JSON erst komplett zu String serialisieren, DANN atomar schreiben

- **Regel:** `json.dumps(data, ...)` zu einem String machen und diesen am Stueck schreiben — NICHT `json.dump(data, stream)` direkt in die Datei streamen.
  Begruendung: `json.dump` schreibt inkrementell in den Stream. Wirft die Serialisierung mittendrin (z.B. nicht-serialisierbares Objekt), steht bereits halbes JSON in der Temp-Datei. `json.dumps` validiert/serialisiert zuerst vollstaendig im Speicher — fehlerhafte Daten erzeugen gar keine Temp-Datei. Erst der fertige String wandert in den atomic-write.
  ```python
  text = json.dumps(data, indent=2, ensure_ascii=False)  # zuerst komplett
  # danach: text atomar schreiben (siehe Code-Beispiel unten)
  ```
  (Quelle: docs json.dumps/json.dump, offiziell)

### 2.7 `pathlib.Path.write_text/write_bytes` ist NICHT atomar

- **Regel:** `Path.write_text()` / `Path.write_bytes()` nur fuer unkritische Dateien nutzen — NIEMALS fuer Config/JSON, die einen Crash ueberleben muessen.
  Begruendung: Beide Methoden sind bequeme Wrapper um `open(...,'w')` + `write` + `close`. Sie truncaten das Ziel sofort und machen weder atomaren Rename noch fsync. Ein Crash mitten im Schreiben hinterlaesst eine korrupte/leere Datei. Fuer kritische Dateien das atomic-write-Pattern verwenden.
  (Quelle: docs pathlib.Path.write_text, offiziell)

### 2.8 Parallele Prozesse mit `filelock` (separate Lock-Datei) absichern

- **Regel:** Wenn mehrere Prozesse dieselbe Datei schreiben koennen, eine separate Lock-Datei (`target + ".lock"`) per `filelock`-Bibliothek + Timeout einsetzen — nicht `fcntl`/`msvcrt` selbst aufrufen.
  Begruendung: Atomic-write schuetzt vor *Fragmenten*, aber nicht vor *Lost Updates* (Prozess A liest, B liest, beide schreiben — eine Aenderung geht verloren). `fcntl` (Unix) und `msvcrt` (Windows) sind plattform-spezifisch; `filelock` und `portalocker` kapseln das cross-platform und bieten `with`-Kontext + Timeout. `filelock` ist aktiv gepflegt und der gaengige Default.
  ```python
  from filelock import FileLock
  with FileLock(target_path + ".lock", timeout=10):
      atomic_write_text(target_path, text)
  ```
  (Quelle: py-filelock.readthedocs.io / pypi.org/project/filelock, extern; fcntl/msvcrt: docs.python.org/3, offiziell)

### 2.9 `PermissionError` / `WinError 32` mit kurzer Retry-Schleife abfangen

- **Regel:** Den finalen `os.replace` auf Windows in eine kurze Retry-Schleife mit Backoff huellen und nur bei `WinError 32`/`13` erneut versuchen.
  Begruendung: Auf Windows kann ein Virenscanner, Indexer oder ein anderes Programm das Ziel kurz oeffnen ("Datei wird von einem anderen Prozess verwendet", `WinError 32`). Das ist ein transienter Race. Erst alle eigenen Handles schliessen, dann 3–5 Versuche mit steigendem Sleep (z.B. 50/100/200 ms). Auf POSIX tritt das praktisch nicht auf — die Schleife schadet aber nicht.
  (Quelle: gaengige Windows-Engineering-Praxis, extern; Mechanik: docs Windows-Dateifreigabe, offiziell)

### 2.10 Bewertung fertiger Bibliotheken

- **Regel:** Fuer reinen atomic-write KEINE Bibliothek mehr einziehen — das Eigen-Pattern mit `os.replace` ist der empfohlene Weg. `python-atomicwrites` ist unmaintained.
  Begruendung: `python-atomicwrites` (untaker) wird **nicht mehr gepflegt** (seit >12 Monaten kein Release, vom Maintainer wegen PyPI-2FA-Streit deprecatet — er empfiehlt selbst `os.replace`). Alte Versionen wurden zeitweise aus PyPI gepurgt und brachen Builds. `boltons.fileutils.atomic_save` ist eine brauchbare Alternative, wenn man unbedingt eine fertige Funktion will, aber fuer die meisten Faelle reicht das 20-Zeilen-Pattern unten. Eine Bibliothek lohnt sich nur fuers **Locking** (`filelock`), nicht fuers atomare Schreiben selbst.
  (Quelle: pypi.org/project/atomicwrites + snyk.io/advisor (Status "Inactive/unmaintained") + GitHub Issue #61, extern)

### 2.11 Vollstaendiges, kopierfertiges Pattern

```python
from __future__ import annotations

import json
import os
import tempfile
import time
from typing import Any


def atomic_write_text(target_path: str, text: str, *, encoding: str = "utf-8") -> None:
    """Schreibt `text` crash-sicher und atomar nach `target_path`.

    Pattern: temp im selben Verzeichnis -> write -> flush -> fsync -> close
    -> os.replace (atomar, ueberschreibend) -> dir-fsync (nur POSIX).
    Ueberlebt Prozess- und Stromausfall: das Ziel ist stets alte ODER neue Version.
    """
    target_path = os.path.abspath(target_path)
    target_dir = os.path.dirname(target_path) or "."

    # 1) Temp-Datei MUSS im selben Verzeichnis liegen (sonst EXDEV beim replace).
    fd, tmp_path = tempfile.mkstemp(dir=target_dir, prefix=".tmp-", suffix=".part")
    try:
        with os.fdopen(fd, "w", encoding=encoding, newline="\n") as f:
            f.write(text)
            f.flush()              # Python-Puffer -> OS-Cache
            os.fsync(f.fileno())   # OS-Cache -> physische Platte (Windows: _commit)

        # 2) Atomarer, ueberschreibender Tausch (os.replace statt os.rename).
        #    Windows: kurze Retry-Schleife gegen WinError 32 (Scanner-Race).
        last_err: OSError | None = None
        for attempt in range(5):
            try:
                os.replace(tmp_path, target_path)
                last_err = None
                break
            except PermissionError as exc:  # WinError 32 / 13
                last_err = exc
                time.sleep(0.05 * (2 ** attempt))  # 50/100/200/400 ms Backoff
        if last_err is not None:
            raise last_err

        # 3) Verzeichnis-fsync NUR auf POSIX (auf Windows nicht moeglich/noetig).
        if os.name != "nt":
            dir_fd = os.open(target_dir, os.O_RDONLY)
            try:
                os.fsync(dir_fd)
            finally:
                os.close(dir_fd)
    except BaseException:
        # Bei Fehler die Temp-Datei aufraeumen (delete=False -> kein Auto-Cleanup).
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
        raise


def atomic_write_json(target_path: str, data: Any, *, indent: int = 2) -> None:
    """JSON crash-sicher schreiben: ERST komplett serialisieren, DANN atomar schreiben."""
    text = json.dumps(data, indent=indent, ensure_ascii=False)  # vollstaendig im Speicher
    atomic_write_text(target_path, text)


# Optional bei parallelen Prozessen: separate Lock-Datei (cross-platform via filelock)
#   from filelock import FileLock
#   with FileLock(target_path + ".lock", timeout=10):
#       atomic_write_json(target_path, data)
```
(Quellen: docs os.replace / os.fsync / tempfile.mkstemp / json.dumps — alle offiziell, Py 3.13; EXDEV/Same-Dir: alexwlchan.net, extern; Bibliotheks-Status: pypi.org/snyk, extern)

---

## §3 Pfade plattformneutral & Cross-Platform-Mechanik

### 3.1 pathlib.Path als Standard fuer alle Pfade

- **Regel:** Verwende `pathlib.Path` fuer jede Pfad-Operation. Verkette NIEMALS Pfade per String mit `'\\'` oder `'/'`, und nutze `os.path.join` nur noch in Altcode.
  Begruendung: `pathlib` bietet eine objektorientierte API, die `/` vs. `\` automatisch je Plattform aufloest, Path-Objekte (keine fehleranfaelligen Strings) zurueckgibt, und Datei-Operationen direkt am Objekt anbietet (`read_text`, `mkdir`, `unlink`). String-Konkatenation mit Backslash bricht sofort auf macOS/Linux und ist auf Windows fragil (Escape-Probleme).
  ```python
  from pathlib import Path
  # RICHTIG
  config = Path.home() / ".config" / "app" / "config.ini"
  # FALSCH (bricht plattformuebergreifend)
  config = str(Path.home()) + "\\.config\\app\\config.ini"
  ```
  (Quelle: docs pathlib, CPython 3.13, offiziell; PEP 428, offiziell)

### 3.2 Die Pfad-Bausteine richtig einsetzen

- **Regel:** Baue Pfade aus den semantischen Bausteinen `Path.home()`, `Path.cwd()`, `Path(__file__).parent` und dem `/`-Operator. Trenne klar zwischen `expanduser()`, `absolute()` und `resolve()`.
  Begruendung: Jeder Baustein hat eine genaue Bedeutung: `expanduser()` loest `~` auf (macht den Pfad aber nicht absolut), `resolve()` macht absolut UND loest Symlinks UND eliminiert `..` (der einzige Weg, `..` sicher zu entfernen), `absolute()` macht nur absolut ohne Symlink-Aufloesung. `Path(__file__).parent` ist der robuste Weg, Ressourcen relativ zum Skript zu finden — unabhaengig vom aktuellen Arbeitsverzeichnis.
  ```python
  here = Path(__file__).parent             # Skript-Ordner (nicht cwd!)
  data = here / "data" / "input.csv"       # relativ zum Skript
  user_p = Path("~/Documents/x.txt").expanduser()   # ~ aufloesen
  clean = Path("docs/../setup.py").resolve()        # absolut + .. weg + Symlinks
  ```
  (Quelle: docs pathlib, 3.13, offiziell)

### 3.3 Wann os.path noch berechtigt ist

- **Regel:** Bleibe bei `os.path`/`os`-Funktionen nur fuer Bytes-Pfade, Directory-Descriptors (`dir_fd`), bestehenden `os.path`-Altcode oder eng gemessene Performance-Hotspots — in allen anderen Faellen `pathlib`.
  Begruendung: `pathlib` ist reine Python-Implementierung und minimal langsamer als das C-implementierte `os.path`; der Unterschied ist aber fast nie relevant. `os.path` unterstuetzt zusaetzlich `bytes`-Pfade (pathlib nicht) und die `os`-Funktionen erlauben `dir_fd` (relative Operationen gegen einen offenen Verzeichnis-Descriptor), was pathlib nicht bietet.
  (Quelle: docs pathlib "When os.path is still appropriate", 3.13, offiziell)

### 3.4 Pfade an externe Tools/Configs: as_posix()

- **Regel:** Wenn ein externes Tool, eine Config-Datei (YAML/JSON/TOML) oder eine URL Forward-Slashes erwartet, gib den Pfad mit `Path(...).as_posix()` aus — nicht mit `str()`.
  Begruendung: `str(path)` liefert auf Windows Backslashes (`C:\Users\x`), die in Configs, in JSON (Escape!) oder von POSIX-orientierten Tools (Git, Docker, viele CLIs) falsch interpretiert werden. `as_posix()` erzwingt `/` unabhaengig von der Plattform.
  ```python
  p = Path("C:/Users/name/project")
  config["root"] = p.as_posix()    # "C:/Users/name/project"  (RICHTIG fuer Config/Tool/JSON)
  config["root"] = str(p)          # "C:\\Users\\name\\project"  (FALSCH, in JSON kaputt)
  ```
  (Quelle: docs pathlib.PurePath.as_posix, 3.13, offiziell)

### 3.5 Plattform-Erkennung: sys.platform vs os.name vs platform.system()

- **Regel:** Nutze `sys.platform` fuer feingranulare Checks (mit `.startswith(...)` ausser bei `'win32'`/`'darwin'`). Nimm `os.name` nur fuer die grobe Windows-vs-POSIX-Unterscheidung. `platform.system()` ist fuer Anzeige/Logging gedacht.
  Begruendung: `sys.platform` ist die zuverlaessigste, zur Build-Zeit fixierte Konstante: `'win32'` (auch 64-Bit!), `'darwin'` (macOS), `'linux'`, `'cygwin'`. Die Doku empfiehlt `.startswith()` fuer Vorwaertskompatibilitaet (exotische Unix-Werte haengen eine Versionsnummer an, z.B. `'sunos5'`). `os.name` kennt nur `'nt'` (Windows) und `'posix'` (Unix-artig). `platform.system()` (`'Windows'`/`'Darwin'`/`'Linux'`) kostet einen Funktionsaufruf und ist eher fuer menschenlesbare Ausgaben.
  ```python
  import sys, os
  if sys.platform == "win32":
      appdata = Path(os.environ["APPDATA"])
  elif sys.platform == "darwin":
      appdata = Path.home() / "Library" / "Application Support"
  elif sys.platform.startswith("linux"):
      appdata = Path.home() / ".config"
  # Grobe Weiche reicht oft: if os.name == "nt": ...  else: ...
  # FALSCH: 'windows' ist KEIN gueltiger Wert (es ist 'win32')
  ```
  (Quelle: docs sys.platform / os.name, 3.13, offiziell)

### 3.6 Niemals Git-Bash-Pfade hardcoden

- **Regel:** Schreibe in Python NIEMALS Git-Bash-/Unix-Style-Pfade wie `/c/Users/...` als Literal. Verwende `Path.home()` bzw. `expanduser()`. Wenn ein Backslash-Literal noetig ist, nutze Raw-String oder Forward-Slash.
  Begruendung: Git Bash uebersetzt Windows-Pfade intern zu `/c/Users/...`, aber CPython kennt dieses Format nicht — der Zugriff endet in `FileNotFoundError`. Ebenso bricht ein normales String-Literal mit `\` an Escape-Sequenzen (`\t`, `\n`, `\U`). Forward-Slashes funktionieren auf Windows in CPython ueberall.
  ```python
  settings = Path.home() / ".claude" / "settings.json"     # RICHTIG
  settings = Path("~/.claude/settings.json").expanduser()  # RICHTIG
  open("/c/Users/barwa/.claude/settings.json")             # FALSCH -> FileNotFoundError
  p = r"C:\Users\test"   # Raw-String falls Backslash gewollt; oder "C:/Users/test"
  ```
  (Quelle: docs using/windows (Pfade), 3.13, offiziell; deckt sich mit Projektregel `platform-and-paths.md`)

### 3.7 subprocess cross-platform: Liste statt String, kein shell=True

- **Regel:** Uebergib Argumente IMMER als Liste, NIEMALS als String mit `shell=True`. Setze `text=True`, `encoding="utf-8"`, `errors="replace"`, `capture_output=True` und `check=True`. Pfade als `str(Path)` oder `Path`.
  Begruendung: Eine Argument-Liste laesst das Modul Quoting/Escaping plattformkorrekt erledigen (Leerzeichen in Dateinamen!) und eliminiert Shell-Injection. `shell=True` interpretiert Metazeichen, verhaelt sich auf Windows (cmd.exe) anders als auf POSIX (`/bin/sh`) und ist damit nicht portabel — `shlex` ist ebenfalls POSIX-orientiert und auf Windows unbrauchbar. `text=True` + `encoding="utf-8"` + `errors="replace"` verhindern Decode-Crashes bei fremden Code-Pages (cp1252-Falle). `check=True` wirft bei Exit-Code != 0 eine `CalledProcessError`.
  ```python
  result = subprocess.run(
      ["git", "-C", str(repo), "log", "--oneline", "-5"],
      capture_output=True, text=True, encoding="utf-8", errors="replace",
      check=True, timeout=30,
  )
  # FALSCH: subprocess.run(f"git -C {repo} log", shell=True)  # Injection, nicht portabel
  # stdout-Zeilen sind plattformneutral mit '\n' getrennt:
  for line in result.stdout.splitlines():   # RICHTIG (nicht .split("\r\n"))
      ...
  ```
  (Quelle: docs subprocess (args list, Security Considerations, text/encoding/check, text-mode line-ending conversion), 3.13, offiziell)

### 3.8 Verzeichnisse/Dateien plattformneutral anlegen und schreiben

- **Regel:** Lege Verzeichnisse mit `Path.mkdir(parents=True, exist_ok=True)` an, schreibe Text mit `Path.write_text(..., encoding="utf-8")` und nutze `shutil` fuer Kopieren/Loeschen ganzer Baeume.
  Begruendung: `parents=True` erzeugt fehlende Zwischenordner, `exist_ok=True` verhindert `FileExistsError` bei Re-Runs — zusammen idempotent. `write_text` ohne `encoding=` faellt auf Windows auf cp1252 zurueck und zerstoert Umlaute/Emojis. Fuer rekursives Kopieren/Loeschen ist `shutil` (`copytree`, `rmtree`, `copy2`, `move`) der portable Standard.
  ```python
  out = Path.home() / "proggs" / "build" / "out"
  out.mkdir(parents=True, exist_ok=True)             # idempotent
  (out / "report.txt").write_text("Inhalt\n", encoding="utf-8")
  shutil.copytree(out, out.with_name("out_backup"), dirs_exist_ok=True)
  ```
  (Quelle: docs pathlib.Path.mkdir/write_text, shutil, 3.13, offiziell)

### 3.9 Temporaere Pfade ueber tempfile, nie hardcoden

- **Regel:** Ermittle temporaere Verzeichnisse mit `tempfile.gettempdir()` und erzeuge Temp-Dateien/-Ordner mit `NamedTemporaryFile`/`TemporaryDirectory`/`mkstemp`/`mkdtemp` — hardcode niemals `/tmp` oder `%TEMP%`.
  Begruendung: `gettempdir()` respektiert die Plattform-Prioritaet (`TMPDIR`→`TEMP`→`TMP`→Plattform-Default). Hardcodierte Pfade ignorieren Nutzer-Config, brechen plattformuebergreifend und umgehen die sicheren Erzeugungs-Mechanismen. `mkstemp`/`NamedTemporaryFile` legen Dateien race-condition-frei mit `O_EXCL` und restriktiven Rechten an; die High-Level-Kontextmanager raeumen automatisch auf.
  ```python
  with tempfile.TemporaryDirectory(prefix="myapp_") as d:
      work = Path(d) / "scratch.json"
      work.write_text("{}", encoding="utf-8")
  # Ordner ist hier automatisch geloescht
  ```
  (Quelle: docs tempfile.gettempdir / NamedTemporaryFile / mkstemp, 3.13, offiziell)

---

## §4 venv & Dependency-Management

### 4.1 Standard-venv anlegen (Konvention `.venv` im Projekt)

- **Regel:** Lege fuer jedes Projekt ein eigenes venv im Verzeichnis `.venv` direkt im Projektordner an.
  Begruendung: `.venv` ist die etablierte Konvention (von Editoren wie VS Code automatisch erkannt) und haelt Abhaengigkeiten projektlokal getrennt vom System-Python. Auf Windows nutzt man dafuer am besten den `py`-Launcher, auf Unix `python3`.
  ```bash
  py -m venv .venv          # Windows
  python3 -m venv .venv     # Unix/macOS
  ```
  (Quelle: packaging.python.org "Installing using pip and virtual environments", 2025, offiziell)

### 4.2 venv aktivieren — Windows vs. Unix

- **Regel:** Aktiviere das venv mit dem zur Shell passenden Skript. Aktivierung ist nicht zwingend — fuer Skripte/CI kann man stattdessen direkt die venv-`python.exe` aufrufen.
  Begruendung: Aktivierung legt die venv-eigenen `python`/`pip` an den Anfang des `PATH`. Genau dieselbe Wirkung erreicht man ohne Aktivierung, indem man den Interpreter im venv direkt adressiert — robuster fuer nicht-interaktive Skripte.
  ```powershell
  .venv\Scripts\Activate.ps1            # Windows PowerShell
  .venv\Scripts\activate.bat            # Windows cmd.exe
  ```
  ```bash
  source .venv/bin/activate             # Unix/macOS
  source .venv/Scripts/activate         # Git Bash unter Windows
  ```
  Ohne Aktivierung (empfohlen fuer Skripte/CI): `.venv\Scripts\python.exe -m pip install <paket>` (Windows) bzw. `.venv/bin/python -m pip install <paket>` (Unix).
  **ExecutionPolicy-Hinweis (Windows):** Schlaegt `Activate.ps1` mit *"running scripts is disabled"* fehl, einmalig pro Benutzer freigeben (kein Admin noetig): `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`.
  (Quelle: docs "Using Python on Windows" + packaging.python.org, 2025, offiziell)

### 4.3 Immer `python -m pip` statt nacktem `pip`

- **Regel:** Rufe pip ausnahmslos als `python -m pip` (bzw. `py -m pip`) auf, nie als blankes `pip`.
  Begruendung: `-m pip` bindet die Installation garantiert an genau den Interpreter, der `python` gerade ist (das aktive venv). Ein nacktes `pip` kann auf ein anderes, falsches Python zeigen und Pakete ins falsche Environment installieren. Gilt auch fuer Upgrades.
  ```bash
  py -m pip install <paket>              # Windows
  python3 -m pip install <paket>         # Unix/macOS
  ```
  (Quelle: packaging.python.org, 2025, offiziell)

### 4.4 Interpreter-Wahl: `py` vs. `python` vs. `python3`, Versionswahl

- **Regel:** Nutze auf Windows den `py`-Launcher zur Versionswahl; verlasse dich nicht auf `python` im PATH. Auf Unix ist `python3` der sichere Default. Innerhalb eines aktivierten venv ist `python` plattformuebergreifend korrekt.
  Begruendung: Der `py`-Launcher findet alle installierten Versionen zuverlaessig und erlaubt gezielte Auswahl, ohne PATH-Reihenfolge-Probleme.
  ```powershell
  py -3.13                 # startet CPython 3.13
  py -3.13 -m venv .venv   # venv mit genau 3.13 anlegen
  py list                  # installierte Versionen (modern)
  py -0p                   # Legacy-Listing mit Pfaden (noch unterstuetzt)
  ```
  Faustregel: ausserhalb eines venv → `py` (Windows) / `python3` (Unix); innerhalb eines aktivierten venv → `python` (beide).
  (Quelle: docs "Using Python on Windows", 2025, offiziell)

### 4.5 Microsoft-Store-Alias `python.exe` als Falle

- **Regel:** Wenn `python` den Microsoft Store oeffnet statt Python zu starten, nicht raten — den App-Ausfuehrungsalias korrigieren oder konsequent `py` verwenden.
  Begruendung: Windows legt unter `%LocalAppData%\Microsoft\WindowsApps` einen Store-Alias `python.exe` an, der bei nicht installiertem Python auf den Store umleitet — das fuehrt zur Auswahl eines falschen/leeren Interpreters und damit zu venvs am falschen Python. Fix: Start → "App-Ausfuehrungsaliase verwalten" → "Python (default)" deaktivieren; oder durchgaengig `py` nutzen.
  (Quelle: docs "Using Python on Windows", 2025, offiziell)

### 4.6 requirements.txt vs. pyproject.toml — wann was

- **Regel:** Verwende fuer neue Projekte `pyproject.toml` als einzige Quelle der Wahrheit (Metadaten + Abhaengigkeiten + Tool-Config). `requirements.txt` nur noch als gepinnte Deploy-/Reproduzierbarkeits-Datei (Docker, Server) — niemals als Abhaengigkeitsquelle einer Bibliothek.
  Begruendung: `pyproject.toml` (PEP 621 Metadaten, PEP 517/518 Build) ersetzt `setup.py`/`setup.cfg`/`requirements.txt` und funktioniert fuer Apps wie fuer verteilbare Pakete. Gepinnte `requirements.txt` in einer Library erzeugen "dependency hell" beim Nutzer. Dev-Abhaengigkeiten ueber **PEP 735 Dependency Groups** (`[dependency-groups]`) — getrennt von `[project.optional-dependencies]`, weil sie nicht ins veroeffentlichte Paket gehoeren.
  ```toml
  [project]
  name = "mein-tool"
  version = "0.1.0"
  requires-python = ">=3.13"
  dependencies = ["requests>=2.32"]

  [dependency-groups]   # PEP 735 — nur Entwicklung, nicht im Wheel
  dev = ["pytest", "ruff"]
  ```
  (Quelle: packaging.python.org "Writing your pyproject.toml" + PEP 735, 2025, offiziell)

### 4.7 Reproduzierbarkeit: pip freeze, pip-tools, Lock-Files, Hashes

- **Regel:** Trenne lose deklarierte Abhaengigkeiten (was du willst) von gepinnten Lock-Dateien (was exakt installiert wird). Erzeuge die Lock-Datei automatisch, pinne mit `==`, und nutze fuer Server-Deploys zusaetzlich Hashes (`--require-hashes`).
  Begruendung: Lose Bereiche in `pyproject.toml`/`requirements.in` halten dich aktualisierbar; die generierte, vollstaendig gepinnte Datei (inkl. transitiver Deps) garantiert identische Installationen. Hashes schuetzen gegen kompromittierte Pakete/Index — laut pip "guter Fit fuer automatisierte Server-Deployments". PEP 751 standardisiert ein plattformuebergreifendes Lockfile-Format (Werkzeuge ziehen 2025 nach).
  ```bash
  python -m pip freeze > requirements.txt                              # schneller Snapshot
  pip-compile --generate-hashes -o requirements.txt pyproject.toml     # pip-tools: gepinnt + gehasht
  pip-sync requirements.txt                                            # Env exakt darauf bringen
  python -m pip install --require-hashes -r requirements.txt           # mit Hash-Erzwingung
  ```
  (Quelle: pip.pypa.io "Repeatable Installs", 2025, offiziell; pip-tools von jazzband, extern)

### 4.8 Das moderne Tool `uv` (Astral) — moderne, optionale Baseline

- **Regel:** Ziehe `uv` in Betracht, wenn du venv-Erstellung, Dependency-Aufloesung und Lock-Files in einem schnellen Werkzeug buendeln willst. `venv` + `pip`/`pip-tools` bleibt die offizielle, voraussetzungsfreie Baseline; `uv` ist die moderne, optionale Beschleunigung.
  Begruendung: `uv` (in Rust) ersetzt laut eigener Doku `pip`, `pip-tools`, `pipx`, `poetry`, `pyenv`, `virtualenv` u.a., laeuft 10–100x schneller, erzeugt ein universelles `uv.lock` und unterstuetzt macOS, Linux und Windows. Ehrlich: sehr ausgereift und 2025 weit verbreitet, aber ein Drittanbieter-Tool (Astral) — nicht Teil der CPython-Standardinstallation. Wer minimale Voraussetzungen braucht, bleibt bei `venv`/`pip`. Der `uv pip`-Modus ist pip-kompatibel (schrittweise Migration moeglich).
  ```bash
  uv venv                 # legt .venv an (waehlt/holt passende Python-Version)
  uv add requests         # Dependency in pyproject.toml eintragen + installieren
  uv lock                 # universelles uv.lock erzeugen
  uv sync                 # Env exakt auf das Lock bringen (reproduzierbar)
  ```
  (Quelle: docs.astral.sh/uv, 2025, offiziell [Astral]; Reife-Einordnung extern)

### 4.9 `pipx` fuer global installierte CLI-Tools

- **Regel:** Installiere Python-**Kommandozeilen-Tools** (z.B. `ruff`, `black`, `pip-tools`, `httpie`) mit `pipx`, nicht mit `pip` ins Projekt-venv oder ins System-Python.
  Begruendung: `pipx` legt jedes CLI-Tool in ein eigenes isoliertes venv und nur den Befehl in den PATH — keine Abhaengigkeitskonflikte, saubere Deinstallation, kein `sudo pip`. Merksatz: **Projekt-Bibliotheken → `.venv` (pip/uv); globale CLI-Tools → `pipx` (oder `uv tool`)**.
  ```bash
  pipx install ruff        # Tool global & isoliert installieren
  pipx run black .         # einmalig in temporaerem Env (ohne Dauer-Install)
  ```
  (Quelle: pipx.pypa.io, 2025, offiziell)

### 4.10 venv nicht ins Repo, nicht portabel

- **Regel:** Committe niemals das venv. Trage `.venv/` in `.gitignore` ein. Verschiebe/kopiere/benenne ein venv nie um — erstelle es neu.
  Begruendung: Ein venv enthaelt absolute, maschinen- und pfadgebundene Verweise auf den Interpreter; Move/Copy/Rename bricht es. Es ist plattform- und versionsspezifisch und gehoert nicht in die Versionskontrolle. Reproduzierbar wird die Umgebung ueber `pyproject.toml`/Lock-Datei, nicht durch das venv selbst.
  (Quelle: packaging.python.org, 2025, offiziell)

---

## §5 Robuste CLI-/Skript-Struktur

### 5.1 Das `main()`-Pattern

- **Regel:** Kapsele die gesamte Programmlogik in `def main(argv=None) -> int` und rufe sie ausschliesslich ueber `if __name__ == "__main__": raise SystemExit(main())` auf — niemals Top-Level-Code.
  Begruendung: Ein `main()` verhindert die Verschmutzung des globalen Namensraums, macht die Logik importierbar (wichtig fuer Tests, da `main(["--flag"])` mit kuenstlichen Argumenten aufrufbar ist) und liefert ueber den `int`-Rueckgabewert direkt den Exit-Code. Code auf Modulebene wuerde schon beim blossen `import` ausgefuehrt. `raise SystemExit(...)` ist gegenueber `sys.exit(...)` minimal sauberer (kein Import noetig, identische Wirkung).
  ```python
  def main(argv: list[str] | None = None) -> int:
      args = parse_args(argv)
      ...
      return 0
  if __name__ == "__main__":
      raise SystemExit(main())
  ```
  (Quelle: docs library/__main__.html, 3.13, offiziell)

### 5.2 Korrekte Exit-Codes

- **Regel:** Gib `0` bei Erfolg und einen Wert `!= 0` bei Fehlern zurueck. Halte dich an die Unix-Konvention: `1` = allgemeiner Fehler, `2` = falsche Aufruf-Syntax (das nutzt argparse automatisch), `130` = Abbruch durch Ctrl-C.
  Begruendung: Hooks, CI-Pipelines und Shell-Skripte verlassen sich ausschliesslich auf den Exit-Code (`$?` / `$LASTEXITCODE`), nicht auf den ausgegebenen Text. Ein Skript, das bei Fehlern trotzdem `0` zurueckgibt, laesst eine CI faelschlich gruen werden. `sys.exit("text")` schreibt den Text nach stderr und beendet mit Code `1` — daher in `main()` niemals einen String zurueckgeben, sondern einen `int`.
  (Quelle: docs library/sys.html — sys.exit, 3.13, offiziell)

### 5.3 KeyboardInterrupt (Ctrl-C) sauber abfangen

- **Regel:** Fange `KeyboardInterrupt` zentral in `main()` ab und beende mit Exit-Code `130`, statt einen haesslichen Traceback auszugeben.
  Begruendung: `130 = 128 + SIGINT(2)` ist die Standard-Unix-Konvention fuer "durch Ctrl-C beendet". Ein nackter Traceback bei Ctrl-C wirkt unprofessionell und verschleiert, dass der Nutzer selbst abgebrochen hat.
  ```python
  try:
      return run(parse_args(argv))
  except KeyboardInterrupt:
      print("Abgebrochen.", file=sys.stderr)
      return 130
  ```
  (Quelle: docs library/sys.html — Exit-Code-Konvention 128+SIGINT, 3.13, offiziell; der Wert 130 ist etablierte Konvention, in der Doku nicht woertlich genannt)

### 5.4 argparse als Standard-Baseline

- **Regel:** Nutze fuer Argument-Parsing zuerst `argparse` aus der Standardbibliothek — kein externes Paket noetig, solange keine grosse, tief verschachtelte CLI vorliegt.
  Begruendung: argparse ist "batteries included", liefert automatisch `--help`, validiert Typen, erzeugt Fehlermeldungen mit Exit-Code `2` und kostet keine zusaetzliche Abhaengigkeit (wichtig fuer portable Skripte und Hooks). Kernbausteine:
  - **Subcommands:** `add_subparsers(required=True, dest="command")`, pro Subkommando `set_defaults(func=handler)`, am Ende `args.func(args)`.
  - **Typvalidierung:** `type=int`/`type=float` oder eigene Konverter (werfen `ValueError`/`ArgumentTypeError`).
  - **Pfade:** `type=pathlib.Path` (nicht `argparse.FileType` — ab 3.14 deprecated, weil es Dateien nicht sauber schliesst).
  - **Feste Wertemengen:** `choices=[...]`. **Version:** `action="version"`. **Eigene Fehler:** `parser.error("Meldung")` (Code 2).
  - **Boolean-Flags:** `action="store_true"` statt `type=bool` (letzteres macht jeden nicht-leeren String zu `True`).
  (Quelle: docs library/argparse.html, 3.13, offiziell)

### 5.5 click / typer als externe Alternativen

- **Regel:** Greife erst dann zu `click` oder `typer`, wenn die CLI gross wird (viele verschachtelte Subkommandos, gemeinsame Optionen, gehobene UX wie Farben/Prompts/Auto-Vervollstaendigung) — argparse bleibt die Baseline.
  Begruendung: Beide sind **externe** Abhaengigkeiten und bringen Komfort (Dekorator-basierte Definition, Shell-Completion), aber auch Installations- und Wartungskosten. Fuer ein einzelnes Hook-/Build-Skript ist das Over-Engineering; fuer ein mehrstufiges Tool mit Dutzenden Befehlen spart es echten Code. `typer` baut intern auf `click` auf und nutzt Typ-Hints zur Definition.
  (Quelle: click.palletsprojects.com, typer.tiangolo.com — beide extern, Stand 2026-06)

### 5.6 Modul-Ausfuehrung: `python -m`, `__main__.py`, entry points

- **Regel:** Mache Pakete ueber `python -m paket` ausfuehrbar (per `__main__.py`) und definiere fuer installierbare Tools `console_scripts`-Entry-Points in `pyproject.toml` statt dich auf einen Shebang zu verlassen.
  Begruendung: Ein `__main__.py` macht `python -m paket` moeglich und sollte minimal bleiben (`from .cli import main; raise SystemExit(main())`). Entry-Points erzeugen beim Installieren einen **plattformuebergreifenden** ausfuehrbaren Wrapper — auf Windows eine `.exe`, auf Unix ein Shell-Wrapper. Damit braucht der Nutzer weder Shebang noch `python skript.py`, und das Tool funktioniert auf macOS und Windows identisch.
  ```toml
  [project.scripts]
  mytool = "mytool.cli:main"
  ```
  (Quelle: packaging.python.org specifications/entry-points/ + docs library/__main__.html, offiziell)

### 5.7 Shebang vs. portabler Start auf Windows

- **Regel:** Verlasse dich fuer portablen Start nicht auf den Shebang allein. Setze `#!/usr/bin/env python3` als Unix-Konvention, nutze auf Windows den `py`-Launcher (`py skript.py`) oder — am besten — `console_scripts`-Wrapper.
  Begruendung: Windows ignoriert Shebang-Zeilen auf Dateisystem-Ebene; nur der mitgelieferte `py`-Launcher liest sie aus und waehlt damit die Python-Version. Der zuverlaessigste, plattformunabhaengige Weg bleibt der bei der Installation erzeugte Entry-Point-Wrapper.
  (Quelle: docs using/windows.html — py-Launcher & Shebang, 3.13, offiziell)

### 5.8 stdout / stderr / stdin sauber trennen

- **Regel:** Schreibe ausschliesslich Nutzdaten nach `stdout` und alle Logs, Fortschritts- und Fehlermeldungen nach `stderr`. Bei Pipe-Ausgabe ggf. `flush=True`.
  Begruendung: Nur so bleibt die Ausgabe pipe-bar (`mytool | grep ...`), ohne dass Log-Zeilen die Nutzdaten verschmutzen. `stdout` ist bei nicht-interaktivem Lauf block-gepuffert — in langen Pipes daher `flush` setzen. Binaerdaten ueber `sys.stdout.buffer.write(...)`.
  ```python
  print(ergebnis)                              # Nutzdaten -> stdout
  print("Verarbeite Datei...", file=sys.stderr) # Diagnose -> stderr
  ```
  (Quelle: docs library/sys.html — Standard-Streams & Pufferung, 3.13, offiziell)

### 5.9 Logging statt print fuer Diagnose

- **Regel:** Nutze fuer Diagnose-Ausgaben `logging` (Modul-Logger `logging.getLogger(__name__)`), nicht `print`. Mappe einen `-v`/`-q`-Zaehler auf das Log-Level.
  Begruendung: `logging` schreibt standardmaessig nach stderr, erlaubt Level-Steuerung (DEBUG/INFO/WARNING/ERROR), Zeitstempel und spaetere Umleitung in Dateien — ohne Code-Aenderung. Bibliotheks-Code soll nie an den Root-Logger loggen und nur einen `NullHandler()` anhaengen; die Level-Konfiguration gehoert in `main()`.
  (Quelle: docs howto/logging.html, 3.13, offiziell)

### 5.10 Kopierfertiges Grundgeruest fuer ein robustes CLI-Skript

```python
#!/usr/bin/env python3
"""mytool — Kurzbeschreibung des Werkzeugs."""
from __future__ import annotations

import argparse
import logging
import pathlib
import sys

__version__ = "1.0.0"
log = logging.getLogger("mytool")


def setup_logging(verbosity: int) -> None:
    """Mappt -v/-q auf ein Log-Level; alles geht nach stderr."""
    level = {0: logging.WARNING, 1: logging.INFO}.get(verbosity, logging.DEBUG)
    logging.basicConfig(level=level, format="%(levelname)s: %(message)s",
                        stream=sys.stderr)


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="mytool", description=__doc__,
                                allow_abbrev=False)
    p.add_argument("--version", action="version", version=f"%(prog)s {__version__}")
    p.add_argument("-v", "--verbose", action="count", default=0,
                   help="ausfuehrlicher (mehrfach moeglich)")
    p.add_argument("pfad", type=pathlib.Path, help="Eingabedatei")
    p.add_argument("--modus", choices=["schnell", "genau"], default="schnell")
    args = p.parse_args(argv)        # bei Fehler: Exit-Code 2
    if not args.pfad.exists():       # eigene Validierung -> ebenfalls Exit 2
        p.error(f"Datei nicht gefunden: {args.pfad}")
    return args


def run(args: argparse.Namespace) -> int:
    log.info("Verarbeite %s im Modus %s", args.pfad, args.modus)
    # ... eigentliche Arbeit; Nutzdaten nach stdout:
    print("Ergebnis ...")
    return 0


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    setup_logging(args.verbose)
    try:
        return run(args)
    except KeyboardInterrupt:
        print("Abgebrochen.", file=sys.stderr)
        return 130
    except Exception as exc:          # letzte Auffanglinie
        log.error("Fehlgeschlagen: %s", exc)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
```
(Quellen: docs __main__ / argparse / sys / howto/logging, alle 3.13, offiziell)

---

## §6 Type Hints, moderne Sprachfeatures & Tooling

### 6.1 Generics: eingebaute Typen statt `typing`-Aliase (PEP 585)

- **Regel:** Schreibe `list[str]`, `dict[str, int]`, `tuple[int, ...]` direkt — niemals `typing.List`/`Dict`/`Tuple` importieren.
  Begruendung: Seit Python 3.9 sind die eingebauten Container generisch annotierbar. Die `typing`-Aliase sind deprecated (Backward-Compat-Schicht), erzeugen unnoetige Imports und werden von ruff (`UP`-Regeln) automatisch ersetzt.
  ```python
  # alt: from typing import List, Dict; def f(x: List[str]) -> Dict[str, int]: ...
  def process(items: list[str]) -> dict[str, int]: ...   # modern, Standard auf 3.13
  ```
  (Quelle: docs typing / PEP 585, ab 3.9, offiziell)

### 6.2 Union-Typen mit `|` statt `Optional`/`Union` (PEP 604)

- **Regel:** Nutze `str | None` und `int | str` statt `Optional[str]` bzw. `Union[int, str]`.
  Begruendung: Seit 3.10 ist der `|`-Operator fuer Typen Standard — lesbarer, kuerzer, kein Import noetig. `Optional[X]` ist exakt `X | None`.
  ```python
  def greet(name: str | None) -> None: ...          # statt Optional[str]
  def handler(x: int | str) -> None: ...            # statt Union[int, str]
  ```
  (Quelle: docs typing / PEP 604, ab 3.10, offiziell)

### 6.3 Type-Aliase modern: das `type`-Statement (PEP 695)

- **Regel:** Definiere Aliase mit `type Vector = list[float]` (ab 3.12, auf 3.13 verfuegbar). Nur fuer Code, der auch 3.11/aelter laufen muss, `TypeAlias` als Fallback.
  Begruendung: Das `type`-Statement erzeugt ein echtes `TypeAliasType`-Objekt mit Lazy Evaluation (loest Forward-Reference-Probleme von selbst) und macht die Absicht explizit. Dieselbe PEP-695-Syntax gilt fuer generische Klassen/Funktionen: `class Stack[T]: ...` statt `Generic[T]` + `TypeVar`.
  ```python
  type Vector = list[float]                  # modern (3.12/3.13)
  # Fallback <= 3.11: from typing import TypeAlias; Vector: TypeAlias = list[float]
  ```
  (Quelle: docs typing / PEP 695, ab 3.12, offiziell)

### 6.4 Wann Type Hints sinnvoll sind — pragmatisch

- **Regel:** Typisiere Funktionssignaturen und oeffentliche APIs (Parameter + Rueckgabewert) immer. Bei kleinen Wegwerf-Skripten reichen Hints an Signaturen; lokale Variablen brauchen meist keine Annotation, weil der Type Checker sie ableitet.
  Begruendung: Hints zahlen sich an den Schnittstellen aus (Doku, Bug-Fang vor Runtime, Team-Klarheit). Jede lokale Zeile zu annotieren ist Over-Engineering. Nutze konkrete Typen statt `Any`; wenn wirklich unbekannt, ist `object` typsicherer als `Any`.
  (Quelle: docs typing / mypy getting_started, Stand 3.13, offiziell)

### 6.5 Strukturierte Daten: `dataclass` statt dict-Wildwuchs

- **Regel:** Fuer feste Datenstrukturen `@dataclass` verwenden statt lose dicts herumzureichen. Fuer externe Eingaben mit Validierung kurz `pydantic` (extern) erwaegen.
  Begruendung: Dataclasses geben typisierte Felder, `__init__`/`__repr__`/`__eq__` gratis und werden vom Type Checker geprueft. Neu in 3.13: `copy.replace()` erzeugt unveraenderte Kopien mit geaenderten Feldern (immutable Updates). Pydantic ergaenzt dort, wo zur Laufzeit validiert/geparst werden muss (JSON-APIs, Config-Files) — das leisten Dataclasses nicht.
  ```python
  from dataclasses import dataclass
  from copy import replace  # neu nutzbar in 3.13
  @dataclass
  class Event:
      name: str
      attendees: int
  e2 = replace(Event("Sync", 3), attendees=4)  # neue Instanz, Original unveraendert
  ```
  (Quelle: docs dataclasses / What's New 3.13 (copy.replace), offiziell; pydantic = extern)

### 6.6 Pfade als `pathlib.Path`, JSON-Strukturen als `TypedDict`

- **Regel:** Typisiere Dateipfade als `pathlib.Path` (nicht `str`), und bekannte JSON-/Config-Strukturen als `TypedDict`.
  Begruendung: `Path` ist plattformuebergreifend und vermeidet manuelle Separator-Fummelei. `TypedDict` gibt JSON-dicts statische Schluessel-/Wert-Pruefung, ohne Runtime-Overhead (rein fuer den Checker).
  ```python
  from typing import TypedDict, NotRequired
  class Movie(TypedDict):
      title: str
      year: int
      rating: NotRequired[float]   # optionales Feld bei total=True
  ```
  (Quelle: docs typing (TypedDict, NotRequired ab 3.11) / pathlib, offiziell)

### 6.7 `from __future__ import annotations` — nur wenn noetig

- **Regel:** Setze `from __future__ import annotations` an den Modulanfang, wenn du Forward-References ohne Quotes nutzen willst (z.B. eine Klasse, die sich selbst referenziert) oder Import-Performance durch Lazy-Annotations brauchst.
  Begruendung: Mit dem Future-Import werden Annotations als Strings behandelt (lazy ausgewertet) — `def next(self) -> Node | None` funktioniert dann ohne `"Node"`-Quotes. Fuer einfache Skripte ohne Selbst-Referenzen ist es nicht noetig.
  (Quelle: docs typing / PEP 563, ab 3.7, offiziell)

### 6.8 Static Type Checking: mypy vs. pyright

- **Regel:** Setze einen Type Checker in CI ein, sobald das Skript Hints hat. Starte mit `--strict` und deaktiviere einzelne Checks gezielt, statt schwach anzufangen. mypy fuer reine CLI/CI-Pruefung; pyright/Pylance fuer beste VS-Code-Editor-Integration.
  Begruendung: `--strict` schaltet u.a. `--disallow-untyped-defs` ein — laut mypy-Doku bekommst du damit "praktisch nie einen Typ-Fehler zur Laufzeit ohne entsprechenden mypy-Fehler". Fuer grosse Bestands-Codebasen am Anfang zu hart → inkrementell migrieren. pyright (Microsoft, extern) ist schneller bei grossen Projekten und treibt die Echtzeit-Pruefung in VS Code (Pylance); mypy ist die Referenz-Implementierung fuer CI.
  ```toml
  [tool.mypy]
  python_version = "3.13"
  strict = true
  ignore_missing_imports = false
  ```
  (Quelle: mypy.readthedocs.io getting_started, Stand 3.13, offiziell; pyright = Microsoft/pyright, extern)

### 6.9 ruff als Linter UND Formatter — der 2025-Standard (extern, Astral)

- **Regel:** Nutze `ruff` als einziges Tool fuer Linting, Import-Sortierung und Formatierung. Es ersetzt flake8 + isort + pyupgrade + autoflake + pydocstyle (und als Formatter black) in einem Binary.
  Begruendung: ruff ist in Rust geschrieben, 10–100x schneller als flake8/black, hat ueber 900 Regeln, Drop-in-Parity zu black beim Formatieren und eine zentrale `pyproject.toml`-Config. Cross-Platform (Windows/macOS/Linux) ohne externe Abhaengigkeiten. Die `UP`-Regeln modernisieren automatisch genau die Punkte aus 6.1–6.3 (List→list, Optional→`|`).
  ```toml
  [tool.ruff]
  line-length = 100
  target-version = "py313"
  [tool.ruff.lint]
  select = ["E", "F", "I", "UP"]   # pycodestyle, Pyflakes, isort, pyupgrade
  ```
  ```bash
  ruff check --fix .   # linten + auto-fixen (inkl. Import-Sortierung, Modernisierung)
  ruff format .        # formatieren (black-kompatibel)
  ```
  Ehrliche Einordnung: ruff ist ein Astral-Drittprojekt (nicht von der CPython-Foundation), aber 2025 der De-facto-Standard fuer neue Projekte — Geschwindigkeit und Ein-Tool-Setup haben black/flake8-Stacks weitgehend abgeloest.
  (Quelle: docs.astral.sh/ruff, Stand 2025, extern)

### 6.10 black — der etablierte Formatter (Abgrenzung)

- **Regel:** Wenn ein Projekt bereits auf `black` standardisiert ist, ist das vollkommen ok — fuer neue Projekte reicht `ruff format` (black-kompatibel) und spart das zweite Tool.
  Begruendung: black ist der seit Jahren etablierte, "kompromisslose" Formatter; `ruff format` zielt bewusst auf Drop-in-Parity. Kein Grund, beide gleichzeitig zu betreiben — eines waehlen.
  (Quelle: docs.astral.sh/ruff (Parity-Aussage), extern; black = psf/black, extern)

### 6.11 Python 3.13: praktisch relevante Neuerungen fuer Skript-Autoren

- **Regel:** Profitiere von den 3.13-Verbesserungen, ohne Code zu aendern — und beachte entfernte Module.
  Fakten (Quelle: docs What's New 3.13, Release 2024-10-07, offiziell):
  - **Neuer REPL**: Multiline-Editing, farbige Prompts/Tracebacks. Bei Problemen: `PYTHON_BASIC_REPL=1`.
  - **Bessere Fehlermeldungen**: Keyword-Vorschlaege (`Did you mean 'maxsplit'?`) und Hinweis, wenn ein Skript wie ein Stdlib-Modul heisst (`random.py`).
  - **`@warnings.deprecated`** (PEP 702): eigene veraltete APIs typsicher markieren.
  - **`TypeIs`** (PEP 742) fuer praezises Type-Narrowing; **`ReadOnly`** fuer TypedDict-Felder (PEP 705); **TypeVar-Defaults** (PEP 696).
  - **19 "dead battery"-Module entfernt** (u.a. `cgi`, `telnetlib`, `crypt`, `imghdr`) sowie `lib2to3`/`2to3`. Skripte, die diese nutzen, brechen auf 3.13 — vorher pruefen.

### 6.12 EncodingWarning / DeprecationWarning ernst nehmen

- **Regel:** Aktiviere Warnungen beim Entwickeln (`python -X dev skript.py` bzw. `-W error`) und behebe `EncodingWarning`/`DeprecationWarning` sofort.
  Begruendung: `EncodingWarning` (ab 3.10, sichtbar mit `-X warn_default_encoding`) zeigt `open()`-Aufrufe ohne explizites `encoding=` — auf Windows die Hauptquelle fuer kaputte Umlaute. `DeprecationWarning` warnt vor genau den Mustern (alte typing-Aliase, entfernte Module), die ruff `UP`/`DTZ` ebenfalls flaggt — Tooling und Warnungen ziehen am selben Strang.
  (Quelle: docs What's New 3.10/3.13 (EncodingWarning), offiziell)

---

## Pflicht-Checkliste vor Python-Arbeit auf Windows / Cross-Platform

- [ ] JEDES `open()` mit `encoding='utf-8'` (Lesen evtl. `utf-8-sig`)? (§1.1, §1.2)
- [ ] JSON: `ensure_ascii=False` UND `encoding='utf-8'` (beides)? Erst `dumps` → String, dann atomar? (§1.3, §2.6)
- [ ] Generierte Text-/JSON-Dateien `newline='\n'`, CSV `newline=''`? (§1.4)
- [ ] Kritische Datei/Config/JSON atomar geschrieben (tempfile im selben Ordner → flush → fsync → `os.replace`)? (§2.11)
- [ ] Pfade ueber `pathlib`, KEIN `/c/Users/...` im Code, `as_posix()` fuer Tools/Configs? (§3.1–§3.6)
- [ ] `subprocess` mit Listen-Args, `encoding='utf-8', errors='replace', check=True` (kein `shell=True`/`shlex`)? (§3.7)
- [ ] `print()`/`logging` mit Umlaut/Emoji abgesichert (`reconfigure`/`FileHandler(encoding=)`/`flush=True`)? (§1.8)
- [ ] venv pro Projekt (`.venv`, gitignored), IMMER `python -m pip`, Interpreter via `py -3` korrekt gewaehlt? (§4)
- [ ] CLI mit `main()`-Pattern + `raise SystemExit(main())`, korrekte Exit-Codes (0/1/2/130)? (§5)
- [ ] Moderne Typen (`list[str]`, `str | None`), beim Entwickeln `EncodingWarning` aktiv, ruff/mypy eingerichtet? (§6)

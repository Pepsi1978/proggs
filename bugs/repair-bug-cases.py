#!/usr/bin/env python3
"""Repariert kaputte JSON-Zeilen in bug-cases.jsonl (Wartungswerkzeug).

Hintergrund: Der `bug-case-auto-writer`-Hook schreibt heute korrekt escaptes JSON
(`json.dumps` / `ConvertTo-Json`). Es koennen aber Altlasten existieren — Zeilen aus
einem frueheren Schreibweg, in denen ein Backslash nicht verdoppelt wurde (z.B. ein
Fehler-Symptom, das selbst den Text "truncated \\uXXXX escape" enthielt). Solche Zeilen
sind ungueltiges JSON; der Hook ueberspringt sie zwar graceful (try/except pro Zeile),
aber sie sind tote Last und sollten bereinigt werden.

Reparatur (funktionserhaltend): Jeder Backslash, der KEINE gueltige JSON-Escape-Sequenz
einleitet (gueltig: \\" \\\\ \\/ \\b \\f \\n \\r \\t und \\u+4hex), wird verdoppelt.
Damit werden kaputte Escapes valide, ohne den Inhalt zu veraendern. Bleibt eine Zeile
danach immer noch ungueltig, wird sie NICHT verworfen, sondern in eine
`bug-cases.jsonl.corrupt`-Quarantaene ausgelagert (verlustfrei, manuell pruefbar).

Aufruf:
  python3 repair-bug-cases.py            # Dry-Run: zeigt nur, was repariert/quarantaeniert wuerde
  python3 repair-bug-cases.py --apply    # Reparatur atomar anwenden (mit Quarantaene fuer Rest)

Cross-Platform (Windows + macOS/Linux), Encoding-sicher, atomares Schreiben
(siehe best-practices-python-windows.md §1, §2).
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import tempfile
from pathlib import Path

# stdout fuer Emoji/Umlaute absichern (Pipe/Hook-sicher), nie crashen.
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
except Exception:
    pass

BUG_CASES = Path.home() / "proggs" / ".claude" / "agent-memory" / "shared" / "bug-cases.jsonl"

# Matcht ENTWEDER ein gueltiges JSON-Escape als EINHEIT (\" \\ \/ \b \f \n \r \t oder \u+4Hex)
# ODER einen einzelnen, nackten Backslash. Die Paar-Bewusstheit ist entscheidend: ein gueltiges
# \\ wird als Einheit konsumiert und bleibt unangetastet, sodass z.B. ein Windows-Pfad
# "C:\\xyz" intakt bleibt und NUR der echte Fehler (z.B. truncated \u12) verdoppelt wird.
# (Eine reine Negativ-Lookahead-Regex ohne diese Paar-Logik wuerde den 2. Backslash eines
#  gueltigen \\ faelschlich verdoppeln, wenn ein Nicht-Escape-Zeichen folgt.)
_ESCAPE_OR_BACKSLASH = re.compile(r'\\(?:["\\/bfnrt]|u[0-9a-fA-F]{4})|\\')


def repair_line(line: str) -> str:
    """Verdoppelt NUR Backslashes, die kein gueltiges JSON-Escape einleiten; gueltige
    Escape-Paare bleiben unveraendert (verlustfrei). Lambda-Replacement umgeht die
    re-Replacement-Mini-Sprache (python-windows.md 9.1)."""
    def _repl(m: "re.Match[str]") -> str:
        s = m.group(0)
        return "\\\\" if len(s) == 1 else s  # nackter Backslash -> verdoppeln; gueltiges Escape -> unveraendert
    return _ESCAPE_OR_BACKSLASH.sub(_repl, line)


def atomic_write_text(target: Path, text: str) -> None:
    """Crash-sicher + atomar schreiben (best-practices-python-windows.md §2.11)."""
    target = target.resolve()
    target_dir = str(target.parent) or "."
    fd, tmp_path = tempfile.mkstemp(dir=target_dir, prefix=".tmp-bugcases-", suffix=".part")
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp_path, target)
        if os.name != "nt":
            dir_fd = os.open(target_dir, os.O_RDONLY)
            try:
                os.fsync(dir_fd)
            finally:
                os.close(dir_fd)
    except BaseException:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
        raise


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Repariert kaputte JSON-Zeilen in bug-cases.jsonl.")
    parser.add_argument("--apply", action="store_true",
                        help="Reparatur tatsaechlich anwenden (sonst nur Dry-Run-Report).")
    parser.add_argument("--path", type=Path, default=BUG_CASES,
                        help="Pfad zur bug-cases.jsonl (Default: ~/proggs/.claude/agent-memory/shared/bug-cases.jsonl).")
    args = parser.parse_args(argv)

    path: Path = args.path
    if not path.exists():
        print(f"Datei nicht gefunden: {path}", file=sys.stderr)
        return 1

    # BOM-tolerant lesen (utf-8-sig), Zeilen ohne die Zeilenenden.
    raw = path.read_text(encoding="utf-8-sig").splitlines()

    ok = 0
    repaired: list[tuple[int, str]] = []   # (zeilennr, repariert)
    quarantine: list[tuple[int, str]] = []  # (zeilennr, original)
    out_lines: list[str] = []

    for i, line in enumerate(raw, 1):
        if not line.strip():
            out_lines.append(line)
            continue
        try:
            json.loads(line)
            out_lines.append(line)
            ok += 1
            continue
        except json.JSONDecodeError:
            pass
        # Versuch 1: ungueltige Escapes verdoppeln
        fixed = repair_line(line)
        try:
            json.loads(fixed)
            repaired.append((i, fixed))
            out_lines.append(fixed)
            continue
        except json.JSONDecodeError:
            # Nicht reparierbar -> Quarantaene (verlustfrei), NICHT in die Hauptdatei
            quarantine.append((i, line))

    print(f"bug-cases.jsonl: {len(raw)} Zeilen | valide {ok} | reparierbar {len(repaired)} | quarantaene {len(quarantine)}")
    for n, _ in repaired:
        print(f"  [reparierbar]  Zeile {n}")
    for n, orig in quarantine:
        print(f"  [quarantaene]  Zeile {n}: {orig[:90]}")

    if not repaired and not quarantine:
        print("Nichts zu tun - Datei ist sauber.")
        return 0

    if not args.apply:
        print("\nDry-Run. Mit --apply anwenden.")
        return 0

    # Reihenfolge ist fuer die Datenintegritaet entscheidend: ZUERST die Quarantaene
    # dauerhaft sichern (inkl. fsync), DANN erst die Hauptdatei (ohne die quarantaenierten
    # Zeilen) atomar ueberschreiben. Andernfalls koennte ein Crash zwischen beiden Schritten
    # die quarantaenierten Zeilen verlieren (sie waeren weder in der Haupt- noch in der
    # Quarantaene-Datei) -> Datenverlust.
    if quarantine:
        qpath = path.with_suffix(path.suffix + ".corrupt")
        block = "".join(f"# Zeile {n} (urspruenglich):\n{orig}\n" for n, orig in quarantine)
        with open(qpath, "a", encoding="utf-8", newline="\n") as qf:
            qf.write(block)
            qf.flush()
            os.fsync(qf.fileno())
        print(f"\n{len(quarantine)} nicht reparierbare Zeile(n) nach {qpath.name} ausgelagert.")
    atomic_write_text(path, ("\n".join(out_lines) + "\n") if out_lines else "")
    print(f"Angewendet: {len(repaired)} repariert, {len(quarantine)} quarantaeniert.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

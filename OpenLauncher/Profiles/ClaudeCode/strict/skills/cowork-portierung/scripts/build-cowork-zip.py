#!/usr/bin/env python3
"""build-cowork-zip.py — baut aus Cowork/skills-src/<name>/ ein fertiges Cowork/<name>.zip.

Garantien (genau das, was Cowork verlangt — bugs/claude-tooling/cowork.md Paragraf 6.3):
  * ZIP-Wurzel = der Skill-Ordner <name>/  (also <name>/SKILL.md, NICHT nur SKILL.md)
  * Forward-Slash-Pfade im ZIP (zipfile schreibt immer '/', anders als PowerShell Compress-Archive)
  * Textdateien LF-normalisiert + BOM-frei (Cowork laeuft Skills in einer Linux-VM; CRLF/BOM brechen das)
Danach verifiziert das Skript das ZIP selbst und bricht mit Exit-Code != 0 ab, falls etwas nicht stimmt.

Aufruf:
  python build-cowork-zip.py <name>
  python build-cowork-zip.py <name> --src <skills-src-dir> --out <cowork-dir>   (Pfade ueberschreiben)

Plattformunabhaengig: laeuft mit python3 (macOS/Linux) und python (Windows) gleich.
"""
import argparse
import os
import sys
import zipfile

# stdout/stderr auf UTF-8 (Windows-Konsole ist sonst cp1252 -> Crash bei Sonderzeichen im Pfad).
# python-windows.md Paragraf 1.4 / 1.8. Guard, falls reconfigure nicht verfuegbar.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")
    except Exception:
        pass

# Dateiendungen, die als Text behandelt (LF-normalisiert + BOM-gestrippt) werden.
TEXT_EXT = {
    ".md", ".sh", ".ps1", ".py", ".json", ".txt", ".yml", ".yaml",
    ".js", ".ts", ".css", ".html", ".xml", ".csv", ".toml", ".ini",
    ".cfg", ".gitignore", ".gitattributes",
}


def normalize_text(data: bytes) -> bytes:
    """BOM strippen und CRLF/CR -> LF. Idempotent. Arbeitet auf bytes (kein Decoding noetig)."""
    if data.startswith(b"\xef\xbb\xbf"):
        data = data[3:]
    return data.replace(b"\r\n", b"\n").replace(b"\r", b"\n")


def main() -> int:
    ap = argparse.ArgumentParser(description="Baut ein Cowork-Skill-ZIP aus skills-src/<name>/.")
    ap.add_argument("name", help="Skill-Name (= Ordnername unter skills-src/)")
    ap.add_argument("--src", default=None,
                    help="skills-src-Verzeichnis (Default: ~/proggs/Cowork/skills-src)")
    ap.add_argument("--out", default=None,
                    help="Cowork-Ausgabeordner (Default: ~/proggs/Cowork)")
    args = ap.parse_args()

    home = os.path.expanduser("~")
    src_base = args.src or os.path.join(home, "proggs", "Cowork", "skills-src")
    out_base = args.out or os.path.join(home, "proggs", "Cowork")
    name = args.name.strip().strip("/").strip("\\")

    skill_dir = os.path.join(src_base, name)
    if not os.path.isdir(skill_dir):
        print(f"FEHLER: Skill-Quelle nicht gefunden: {skill_dir}", file=sys.stderr)
        return 1
    if not os.path.isfile(os.path.join(skill_dir, "SKILL.md")):
        print(f"FEHLER: {skill_dir} enthaelt keine SKILL.md", file=sys.stderr)
        return 1

    out_zip = os.path.join(out_base, f"{name}.zip")
    os.makedirs(out_base, exist_ok=True)

    # Dateien deterministisch sammeln (sortiert), arcname mit <name>/ als Wurzel + forward-slash.
    entries = []
    for root, dirs, filenames in os.walk(skill_dir):
        dirs.sort()
        for fn in sorted(filenames):
            full = os.path.join(root, fn)
            rel = os.path.relpath(full, skill_dir).replace(os.sep, "/")
            entries.append((full, f"{name}/{rel}"))

    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as zf:
        for full, arc in entries:
            with open(full, "rb") as fh:
                data = fh.read()
            if os.path.splitext(full)[1].lower() in TEXT_EXT:
                data = normalize_text(data)
            # arcname mit '/' -> zipfile schreibt garantiert forward-slash
            zf.writestr(arc, data)

    # --- Selbst-Verifikation ---
    with zipfile.ZipFile(out_zip, "r") as zf:
        names = zf.namelist()

    problems = []
    skill_arc = f"{name}/SKILL.md"
    if skill_arc not in names:
        problems.append(f"{skill_arc} fehlt im ZIP")
    backslash = [n for n in names if "\\" in n]
    if backslash:
        problems.append(f"Backslash-Pfade im ZIP gefunden: {backslash}")
    not_rooted = [n for n in names if not n.startswith(f"{name}/")]
    if not_rooted:
        problems.append(f"Eintraege ausserhalb der Wurzel '{name}/': {not_rooted}")

    print(f"ZIP gebaut: {out_zip}")
    print(f"Eintraege ({len(names)}), Wurzel '{name}/':")
    for n in sorted(names):
        print(f"  {n}")

    if problems:
        print("VERIFIKATION FEHLGESCHLAGEN:", file=sys.stderr)
        for p in problems:
            print(f"  - {p}", file=sys.stderr)
        return 2

    print(f"Verifikation OK: {skill_arc} vorhanden, alle Pfade forward-slash, Wurzel = {name}/.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

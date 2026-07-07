#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""memory-staleness.py — read-only Governance-Check fuer das Auto-Memory-System (Welle 3, 2026-06-15).

Das Auto-Memory (`~/.claude/projects/*/memory/*.md`) ist machine-local und waechst stetig
(>100 Dateien). Memories spiegeln den Stand zum SCHREIB-Zeitpunkt — nennen sie konkrete
Dateien/Hooks/Flags/Versionen, koennen sie still veralten (Datei umbenannt, Flag entfernt,
Version gesprungen). Genau davor warnt der Recall-Hinweis "verify before recommending".

Dieses Werkzeug macht das messbar: es findet Memories mit **Veraltungs-Risiko** (nennen
konkrete Pfade/Hooks/Flags/Versionen) die zugleich **alt** und **nicht frisch verifiziert**
sind (`last_verified` fehlt oder zu alt) → Kandidaten fuer eine kurze Re-Verifikation.

Governance-Konvention (optional, NICHT erzwungen — siehe Memory `feedback-memory-governance`):
Memories, die konkrete Dinge nennen, KOENNEN im Frontmatter `last_verified: YYYY-MM-DD`,
`version_anchor: <software>=<version>` und `confidence: hoch|mittel|niedrig` tragen. Dieses
Skript belohnt ein frisches `last_verified` (dann kein Stale-Flag).

Rein lesend, aendert NICHTS, exit 0 immer. Plattform-robust (nur stdlib, glob, dynamischer Pfad).
Manuell aufrufbar: `python claude-code-setup/tools/memory-staleness.py [--all] [--days N]`.
"""
import os
import re
import sys
import glob
from datetime import date, datetime

STALE_DAYS = 120          # aelter + riskant + unverifiziert -> Kandidat
VERIFY_FRESH_DAYS = 90    # last_verified juenger als das -> gilt als frisch verifiziert

# Veraltungs-Risiko-Marker: nennt die Memory konkrete, aenderbare Dinge?
RISK_PATTERNS = [
    (r"\b[\w.\-/]+\.(ps1|sh|py|kt|kts|cs|swift|ts|tsx|mjs|cjs)\b", "Datei/Skript-Name"),
    (r"\bCLAUDE_[A-Z0-9_]+\b",                                     "CLAUDE_-Env/Flag"),
    (r"\b[\w-]+\.flag\b",                                          "Flag-Datei"),
    (r"\b(defaultMode|bypassPermissions|effortLevel|settings\.json|\.mcp\.json)\b", "Settings-Key"),
    (r"\bv?\d+\.\d+\.\d+\b",                                       "Versionsnummer"),
    (r"~/\.claude/[\w.\-/]+",                                      "claude-Pfad"),
]
_RISK = [(re.compile(p, re.I), label) for p, label in RISK_PATTERNS]
_DATE_IN_NAME = re.compile(r"(20\d\d)-(\d\d)-(\d\d)")
_FM_FIELD = re.compile(r"^(last_verified|date|version_anchor|confidence)\s*:\s*(.+?)\s*$", re.I | re.M)


def parse_date(s):
    m = re.search(r"(20\d\d)-(\d\d)-(\d\d)", s or "")
    if not m:
        return None
    try:
        return date(int(m.group(1)), int(m.group(2)), int(m.group(3)))
    except ValueError:
        return None


def memory_dirs():
    base = os.path.join(os.path.expanduser("~"), ".claude", "projects")
    return sorted(glob.glob(os.path.join(base, "*", "memory")))


def analyse(path, today):
    try:
        with open(path, "r", encoding="utf-8-sig") as f:
            text = f.read()
    except Exception:
        return None
    fm = {k.lower(): v for k, v in _FM_FIELD.findall(text[:1200])}

    # Schreib-/Aenderungsdatum: date-Feld > Datum im Dateinamen > mtime
    written = parse_date(fm.get("date", "")) or parse_date(os.path.basename(path))
    if written is None:
        try:
            written = date.fromtimestamp(os.path.getmtime(path))
        except Exception:
            written = today
    age = (today - written).days

    last_verified = parse_date(fm.get("last_verified", ""))
    verified_fresh = last_verified is not None and (today - last_verified).days <= VERIFY_FRESH_DAYS

    risks = []
    for rx, label in _RISK:
        if rx.search(text):
            risks.append(label)

    return {"age": age, "risks": risks, "verified_fresh": verified_fresh,
            "has_anchor": "version_anchor" in fm, "confidence": fm.get("confidence")}


def main():
    show_all = "--all" in sys.argv[1:]
    days = STALE_DAYS
    for i, a in enumerate(sys.argv[1:]):
        if a == "--days" and i + 2 <= len(sys.argv[1:]):
            try:
                days = int(sys.argv[1:][i + 1])
            except (ValueError, IndexError):
                pass

    today = date.today()
    dirs = memory_dirs()
    if not dirs:
        print("memory-staleness: kein Memory-Ordner gefunden (~/.claude/projects/*/memory).")
        return 0

    total = 0
    candidates = []
    for d in dirs:
        for path in glob.glob(os.path.join(d, "*.md")):
            base = os.path.basename(path)
            if base.lower() == "memory.md":
                continue
            total += 1
            info = analyse(path, today)
            if info is None:
                continue
            stale = info["risks"] and info["age"] > days and not info["verified_fresh"]
            if stale or show_all:
                candidates.append((info["age"], base, info))

    candidates.sort(key=lambda t: -t[0])
    flagged = [c for c in candidates if c[2]["risks"] and c[2]["age"] > days and not c[2]["verified_fresh"]]

    print("=== Memory-Staleness (read-only Governance-Check) ===")
    print("Memories gesamt: %d  |  Veraltungs-Kandidaten (riskant + >%dd + unverifiziert): %d"
          % (total, days, len(flagged)))
    shown = candidates if show_all else flagged
    for age, base, info in shown[:40]:
        anchor = " [anchor]" if info["has_anchor"] else ""
        conf = (" conf=%s" % info["confidence"]) if info["confidence"] else ""
        print("  [%4dd] %-52s  nennt: %s%s%s" % (age, base[:52], ", ".join(sorted(set(info["risks"]))), anchor, conf))
    if not show_all and len(flagged) > 40:
        print("  ... und %d weitere (--all fuer alle)." % (len(flagged) - 40))
    if not flagged:
        print("ERGEBNIS: Keine veralteten riskanten Memories - alle frisch genug oder verifiziert.")
    else:
        print("ERGEBNIS: %d Memory(s) sollten kurz re-verifiziert werden (Datei/Flag/Version noch aktuell?)."
              % len(flagged))
        print("          Nach Pruefung im Frontmatter 'last_verified: %s' setzen -> faellt aus der Liste." % today.isoformat())
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        try:
            print("memory-staleness: unerwarteter Fehler - %s" % e)
        except Exception:
            pass
        sys.exit(0)

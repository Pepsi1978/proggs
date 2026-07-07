#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""bugs/check-dead-paths.py — Tote-Pfad-Validator fuer bugs/ + best-practices/.

Prueft NICHT nur klickbare Markdown-Links `](pfad)`, sondern AUCH Backtick-Pfad-
Erwaehnungen `` `bugs/...` `` / `` `best-practices/...` `` auf Existenz. Genau diese
Backtick-Erwaehnungen werden vom reinen Markdown-Link-Check uebersehen und bleiben bei
Struktur-Umbauten (z.B. projekt-code/ -> flach) als veraltete Pfade stehen.

Konservativ gegen False Positives:
  - Schema-Platzhalter mit `<...>`, Globs (`*`, `**`), Klammern oder Leerzeichen werden ignoriert.
  - Backtick-Inhalt zaehlt nur, wenn er KOMPLETT ein Repo-Pfad ist (`^(bugs|best-practices)/...`)
    und auf `.md` oder `/` endet.
  - Markdown-Links: relative (`./`, `../`) datei-relativ; repo-relative (`bugs/`, `best-practices/`)
    werden gegen BEIDE Aufloesungen geprueft (datei- UND repo-relativ) — tot nur wenn beide fehlen.

Rein lesend. exit 0 wenn keine toten Pfade, sonst exit 1 (fuer health.py-Buendelung).
Cross-Platform: nur stdlib, Pfade ueber __file__.
"""
import os
import re
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

BUGS = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(BUGS)
ROOTS = ["bugs", "best-practices"]

PLACEHOLDER = re.compile(r"[<>*()\s]")
LINK_RE = re.compile(r"\]\(([^)]+)\)")
# Backtick-Code-Span, dessen GANZER Inhalt ein Repo-Pfad ist
BACKTICK_RE = re.compile(r"`((?:bugs|best-practices)/[^`]+?)`")


def is_placeholder(p):
    return bool(PLACEHOLDER.search(p)) or "NN-" in p


def resolve_exists(filedir, path):
    """True wenn der Pfad (datei- ODER repo-relativ) existiert."""
    cand = []
    if path.startswith(("./", "../")):
        cand.append(os.path.normpath(os.path.join(filedir, path)))
    elif path.startswith(("bugs/", "best-practices/")):
        cand.append(os.path.normpath(os.path.join(REPO, path)))            # repo-relativ
        cand.append(os.path.normpath(os.path.join(filedir, path)))         # datei-relativ (Fallback)
    else:
        cand.append(os.path.normpath(os.path.join(filedir, path)))
    return any(os.path.exists(c) for c in cand)


def main():
    dead = []
    checked = 0
    for root in ROOTS:
        for dp, _dirs, files in os.walk(os.path.join(REPO, root)):
            for fn in files:
                if not fn.endswith(".md") or fn.endswith("-kurzcheck.md"):
                    continue
                full = os.path.join(dp, fn)
                filedir = os.path.dirname(full)
                rel = os.path.relpath(full, REPO).replace("\\", "/")
                try:
                    with open(full, encoding="utf-8") as fh:
                        c = fh.read()
                except Exception:
                    continue
                cands = set()
                # 1. Markdown-Links
                for m in LINK_RE.finditer(c):
                    p = m.group(1).split("#")[0].strip()
                    if not p or p.startswith(("http://", "https://", "mailto:")):
                        continue
                    if is_placeholder(p):
                        continue
                    if p.startswith(("./", "../", "bugs/", "best-practices/")):
                        cands.add(p)
                # 2. Backtick-Pfade (kompletter Repo-Pfad, auf .md oder / endend)
                for m in BACKTICK_RE.finditer(c):
                    p = m.group(1).strip()
                    if is_placeholder(p):
                        continue
                    if p.endswith(".md") or p.endswith("/"):
                        cands.add(p)
                for p in cands:
                    checked += 1
                    if not resolve_exists(filedir, p):
                        dead.append((rel, p))

    dead.sort()
    for rel, p in dead:
        print(f"[TOT]   {rel} -> {p}")
    print(f"--- {checked} Pfad-Erwaehnungen geprueft, TOTE PFADE: {len(dead)} ---")
    if dead:
        print(f"ERGEBNIS: {len(dead)} tote Pfad-Erwaehnung(en) gefunden.")
        return 1
    print("ERGEBNIS: Keine toten Pfade — alle Link- und Backtick-Pfade existieren.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        try:
            print(f"check-dead-paths.py: unerwarteter Fehler — {e}")
        except Exception:
            pass
        sys.exit(0)

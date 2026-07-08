#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""prune-bug-cases.py — Soft-Pruning fuer bug-cases.jsonl mit Near-Miss-Schutz.

Befund 8 (Audit 2026-06-16): bug-cases.jsonl wuchs bisher unbegrenzt (der auto-writer-Hook
appendet nur, kein Cap). Dieses eigenstaendige Wartungs-Tool kappt die Datei bei Bedarf —
race-sicher (atomar + mtime-Check), damit es den live appendenden Hook nicht stoert.
BEWUSST KEIN Pruning im Hook selbst (Hot-Path, parallele Sessions, Race-Gefahr).

Pruning-Prioritaet beim Entfernen (von zuerst nach zuletzt), nur wenn ueber LIMIT:
  1. noise=true (als Rauschen markierte Diagnose-/Statusbefehle) — aeltester zuerst
  2. normale Eintraege (kein near_miss, nicht angereichert) — aeltester zuerst
  GESCHUETZT (werden NIE entfernt, solange 1+2 zum Kappen reichen):
  - near_miss=true            (intelligence-system.md: hoher Lernwert)
  - enriched_by gesetzt / auto_captured=false (echtes, von Hand nachgetragenes Wissen)

Standard = DRY-RUN (zeigt nur, was passieren WUERDE). Mit --apply wird tatsaechlich geschrieben.
Rein lesend ohne --apply. Immer exit 0 (blockiert nie). Manuell / im Wartungslauf ausfuehren:
  python3 bugs/prune-bug-cases.py            # Status / Dry-Run
  python3 bugs/prune-bug-cases.py --apply     # tatsaechlich kappen
"""
from __future__ import annotations
import json, os, sys, tempfile

LIMIT = 500  # Soft-Cap: erst darueber wird gekappt (bei 141 Eintraegen passiert nichts)


def repo_root() -> str:
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def is_protected(d: dict) -> bool:
    if d.get("near_miss") is True:
        return True
    if d.get("enriched_by"):
        return True
    if d.get("auto_captured") is False:
        return True
    return False


def main() -> int:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

    apply = "--apply" in sys.argv[1:]
    path = os.path.join(repo_root(), ".claude", "agent-memory", "shared", "bug-cases.jsonl")
    if not os.path.isfile(path):
        print(f"[prune] bug-cases.jsonl nicht gefunden: {path}")
        return 0

    with open(path, encoding="utf-8") as f:
        raw = [ln for ln in f if ln.strip()]
    mtime_before = os.path.getmtime(path)

    parsed = []
    for i, ln in enumerate(raw):
        try:
            parsed.append((i, json.loads(ln), ln))
        except Exception:
            parsed.append((i, None, ln))  # unparsebare Zeile NIE entfernen (bewahren)

    total = len(parsed)
    print(f"[prune] Eintraege: {total}  |  Soft-Limit: {LIMIT}")
    if total <= LIMIT:
        print(f"[prune] Unter dem Limit ({total} <= {LIMIT}) — nichts zu kappen.")
        return 0

    to_remove = total - LIMIT
    noise_idx, normal_idx = [], []
    protected = 0
    for i, d, _ln in parsed:
        if d is None:
            protected += 1; continue
        if is_protected(d):
            protected += 1; continue
        if d.get("noise") is True:
            noise_idx.append(i)
        else:
            normal_idx.append(i)

    # Reihenfolge der Datei = chronologisch (append-only) -> niedriger Index = aelter.
    removal_order = noise_idx + normal_idx  # erst Rauschen, dann normale; je aelteste zuerst
    remove_set = set(removal_order[:to_remove])
    actually = len(remove_set)

    print(f"[prune] geschuetzt (near_miss/angereichert/unparsebar): {protected}")
    print(f"[prune] kappbar: noise={len(noise_idx)} normal={len(normal_idx)}  |  zu entfernen: {to_remove}")
    if actually < to_remove:
        print(f"[prune] HINWEIS: nur {actually} entfernbar ohne geschuetzte anzutasten "
              f"(Datei bleibt ueber Limit — das ist gewollt, Near-Miss-Schutz hat Vorrang).")

    if not apply:
        print(f"[prune] DRY-RUN: wuerde {actually} Eintraege entfernen. Mit --apply ausfuehren.")
        return 0

    kept = [ln for i, _d, ln in parsed if i not in remove_set]
    if os.path.getmtime(path) != mtime_before:
        print("[prune] ABBRUCH: Datei wurde zwischenzeitlich geaendert (paralleler Hook-Append) — bitte erneut.")
        return 0
    d_ = os.path.dirname(path)
    fd, tmp = tempfile.mkstemp(dir=d_, suffix=".tmp")
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as f:
            for ln in kept:
                f.write(ln if ln.endswith("\n") else ln + "\n")
        os.replace(tmp, path)
    except Exception as e:
        print("[prune] SCHREIBFEHLER:", e)
        try: os.unlink(tmp)
        except Exception: pass
        return 0
    print(f"[prune] OK: {actually} entfernt, {len(kept)} behalten.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except SystemExit:
        raise
    except Exception as e:  # nie crashen
        print("[prune] unerwarteter Fehler (ignoriert):", e)
        raise SystemExit(0)

#!/usr/bin/env python3
"""firecrawl-test-run.py — Parallel-Runner fuer mm-research.py (Engine A: Firecrawl + MiniMax M3).

Startet mm-research.py fuer alle Themen aus ~/.or-online-test/themes.txt, MAX 2 PARALLEL
(Firecrawl-Free erlaubt nur 2 concurrent, Continuous-Spawning: sobald einer fertig, startet der
naechste). Jeder Lauf bekommt ein eigenes MM_OUTDIR (run-N) -> kein gegenseitiges Ueberschreiben
der internen sources.json/answer.json/thinking.txt (Grund, warum frueher R5 abgeschnitten zurueckkam).

Output je Lauf: ~/.firecrawl-test/answer-N.txt (stdout), log-N.txt (stderr), run-N/ (Rohdaten).
Wiederaufnahme-sicher: Themen mit schon vorhandener answer-N.txt (>500 B) werden uebersprungen.

Nur stdlib. Pfade ueber expanduser (NIE /c/... an Windows-Python). Test fuer den A/B-Vergleich
gegen den :online-Lauf (or-online-test.py).
"""
import concurrent.futures
import os
import subprocess
import sys

HOME = os.path.expanduser("~")
OUT = os.path.join(HOME, ".firecrawl-test")
THEMES = os.path.join(HOME, ".or-online-test", "themes.txt")
MM = os.path.join(HOME, "proggs", "mm-research.py")
os.makedirs(OUT, exist_ok=True)

themes = [l for l in open(THEMES, encoding="utf-8").read().splitlines() if l.strip()]


def run(it):
    i, theme = it
    n = i + 1
    ans = os.path.join(OUT, f"answer-{n}.txt")
    if os.path.exists(ans) and os.path.getsize(ans) > 500:
        return f"[{n}] SKIP (schon {os.path.getsize(ans)} B vorhanden)"
    rundir = os.path.join(OUT, f"run-{n}")
    os.makedirs(rundir, exist_ok=True)
    env = dict(os.environ, MM_OUTDIR=rundir, MM_THINK_BUDGET="24000")
    log = os.path.join(OUT, f"log-{n}.txt")
    try:
        with open(ans, "w", encoding="utf-8", newline="\n") as fo, \
             open(log, "w", encoding="utf-8", newline="\n") as fe:
            rc = subprocess.run([sys.executable, MM, theme, "5"], stdout=fo, stderr=fe,
                                env=env, timeout=400).returncode
        return f"[{n}] rc={rc} bytes={os.path.getsize(ans)}"
    except Exception as e:
        return f"[{n}] ERR {type(e).__name__}: {str(e)[:120]}"


def main():
    print(f"Starte {len(themes)} Themen, max 2 parallel (Continuous-Spawning)...", flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
        for res in ex.map(run, enumerate(themes)):
            print(res, flush=True)
    with open(os.path.join(OUT, "done.flag"), "w", encoding="utf-8") as fh:
        fh.write("done\n")
    print("ALL FIRECRAWL DONE", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

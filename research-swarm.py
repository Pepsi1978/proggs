#!/usr/bin/env python3
"""research-swarm.py — Continuous-Spawning-Runner fuer die Bash-Recherche-Engines (A=Firecrawl/mm, B=:online/or).

ERZWINGT Continuous-Spawning mit max N parallel — DETERMINISTISCH, nicht umgehbar (Poka-Yoke Stufe 3):
`ThreadPoolExecutor(max_workers=N)` haelt KONSTANT N Worker. Sobald EIN Researcher fertig ist, zieht der
Pool SOFORT die naechste Aufgabe aus der Queue — kein Wellen-Barrier, kein Leerlauf, KEIN Zeitverlust.
Damit muss der Hauptagent die Parallelitaet nicht von Hand orchestrieren und kann sie auch nicht
"in Wellen" verschlechtern. Das ist die harte Durchsetzung der Continuous-Spawning-Regel
(`~/.claude/rules/research-strategy.md` §3a) fuer die beiden Skript-Engines.

Harte Engine-Limits (Schutz — werden gedeckelt + gewarnt, NICHT ueberschreibbar nach oben):
  A (Firecrawl)  : max **2** gleichzeitig (hartes Free-Limit: 2 concurrent, 5 Suchen/Min)
  B (:online/or) : max **7** gleichzeitig (`:online` last-stabil, A/B-Test 2026-06-21; Retry faengt Leak §42)

Opus (Engine C) ist NICHT skriptbar (Agent-Tool-Aufrufe macht der Hauptagent) — dort orchestriert
der research-Skill das Continuous-Spawning mit 7 selbst (Pflicht-Pattern im Skill).

Aufruf:
    python3 research-swarm.py <A|B> <themes_file> [max_parallel] [model]
      themes_file : eine Recherche-Frage pro Zeile (leere Zeilen ignoriert)
      max_parallel: optional; Default A=2, B=7; ein hoeherer Wert wird auf das Engine-Limit gedeckelt
      model (nur B): Default `minimax/minimax-m3:online`

Output je Researcher: ~/.research-swarm/answer-<i>.txt (stdout) + log-<i>.txt (stderr) + run-<i>/ (Rohdaten,
eigenes OUTDIR je Lauf -> kein gegenseitiges Ueberschreiben). Wiederaufnahme-sicher (answer-<i>.txt > 500 B
-> SKIP). done.flag am Ende. Nur stdlib; Pfade via expanduser (NIE /c/... an Windows-Python).
"""
import concurrent.futures
import glob
import os
import shutil
import subprocess
import sys

HOME = os.path.expanduser("~")
OUT = os.path.join(HOME, ".research-swarm")
MM = os.path.join(HOME, "proggs", "mm-research.py")
OR = os.path.join(HOME, "proggs", "or-research.py")
ENGINE_CAP = {"A": 2, "B": 7}   # harte Obergrenze je Engine (der Schutz)


def run(engine, model, it):
    i, theme = it
    n = i + 1
    ans = os.path.join(OUT, f"answer-{n}.txt")
    if os.path.exists(ans) and os.path.getsize(ans) > 500:
        return f"[{n}] SKIP (schon {os.path.getsize(ans)} B)"
    rundir = os.path.join(OUT, f"run-{n}")
    os.makedirs(rundir, exist_ok=True)
    log = os.path.join(OUT, f"log-{n}.txt")
    if engine == "A":
        cmd = [sys.executable, MM, theme, "5"]
        env = dict(os.environ, MM_OUTDIR=rundir, MM_THINK_BUDGET="24000")
    else:  # B = :online / or
        cmd = [sys.executable, OR, theme, model]
        env = dict(os.environ, OR_OUTDIR=rundir)
    try:
        with open(ans, "w", encoding="utf-8", newline="\n") as fo, \
             open(log, "w", encoding="utf-8", newline="\n") as fe:
            rc = subprocess.run(cmd, stdout=fo, stderr=fe, env=env, timeout=400).returncode
        return f"[{n}] rc={rc} bytes={os.path.getsize(ans)}"
    except Exception as e:
        return f"[{n}] ERR {type(e).__name__}: {str(e)[:120]}"


def cleanup_previous():
    """Reste eigener FRUEHERER Laeufe entfernen, BEVOR ein neuer Lauf startet (Poka-Yoke Stufe 3).
    WARUM PFLICHT (Vorfall 2026-06-25): run() legt run-<i>/ mit exist_ok=True an. Schlaegt ein
    Researcher fehl, schreibt mm-/or-research.py KEINE neue answer.json -> die ALTE run-<i>/answer.json
    eines frueheren Laufs (mit voellig FREMDEM Thema) bleibt stehen und schlaegt beim Auslesen als
    falsches Ergebnis durch (real: 2 von 5 Slots lieferten LLM-Rate-Limit-/Agent-Eval-Reste statt der
    angefragten Windows-SMB-Themen). Darum vor JEDEM Lauf die eigenen Output-Muster hart loeschen.
    Loescht NUR die eigenen Output-Muster (answer-*.txt, log-*.txt, run-*/, done.flag) - NIEMALS die
    Input-Datei themen.txt oder fremde Reste anderer Tools/Skills (bp/, esc/, qdrant/ ...).
    Resume nach Crash bleibt per RESEARCH_SWARM_RESUME=1 moeglich (dann NICHT aufraeumen -> SKIP-Logik greift)."""
    if os.environ.get("RESEARCH_SWARM_RESUME") == "1":
        print("[research-swarm] RESUME-Modus (RESEARCH_SWARM_RESUME=1): KEIN Aufraeumen, SKIP-Logik aktiv.",
              file=sys.stderr)
        return
    removed = 0
    for pat in ("answer-*.txt", "log-*.txt", "run-*", "done.flag"):
        for p in glob.glob(os.path.join(OUT, pat)):
            try:
                if os.path.isdir(p):
                    shutil.rmtree(p, ignore_errors=True)
                else:
                    os.remove(p)
                removed += 1
            except OSError:
                pass
    print(f"[research-swarm] Aufgeraeumt: {removed} alte Output-Reste entfernt (frischer Lauf, "
          f"keine Fremd-Themen-Reste). RESEARCH_SWARM_RESUME=1 fuer Crash-Resume.", file=sys.stderr)


def main():
    if len(sys.argv) < 3:
        return "Aufruf: research-swarm.py <A|B> <themes_file> [max_parallel] [model]"
    engine = sys.argv[1].upper()
    if engine not in ENGINE_CAP:
        return f"Unbekannte Engine {engine!r} — nur A (Firecrawl) oder B (:online). Opus (C) ist NICHT skriptbar (Agent-Tool)."
    themes_file = os.path.expanduser(sys.argv[2])
    model = sys.argv[4] if len(sys.argv) > 4 else "minimax/minimax-m3:online"
    cap = ENGINE_CAP[engine]
    n_par = int(sys.argv[3]) if len(sys.argv) > 3 else cap
    if n_par > cap:
        print(f"[research-swarm] angefordert {n_par} > Engine-{engine}-Limit {cap} — GEDECKELT auf {cap} (Schutz).",
              file=sys.stderr)
        n_par = cap
    n_par = max(1, n_par)
    os.makedirs(OUT, exist_ok=True)
    with open(themes_file, encoding="utf-8") as fh:
        themes = [l for l in fh.read().splitlines() if l.strip()]
    if not themes:
        return f"Keine Themen in {themes_file}."
    cleanup_previous()   # PFLICHT: alte Output-Reste weg, BEVOR gestartet wird (sonst Fremd-Themen-Leak, s.o.)
    print(f"[research-swarm] Engine {engine} | {len(themes)} Themen | KONSTANT {n_par} parallel "
          f"(Continuous-Spawning: einer fertig -> sofort der naechste)", file=sys.stderr)
    with concurrent.futures.ThreadPoolExecutor(max_workers=n_par) as ex:
        for res in ex.map(lambda it: run(engine, model, it), enumerate(themes)):
            print(res, flush=True)
    with open(os.path.join(OUT, "done.flag"), "w", encoding="utf-8") as fh:
        fh.write("done\n")
    print("ALL DONE", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

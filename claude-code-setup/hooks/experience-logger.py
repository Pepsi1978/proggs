#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
experience-logger — SessionEnd Hook core (cross-platform)

Zieht ECHTE Session-Signale direkt aus dem Session-Transcript
(~/.claude/projects/<hash>/<session>.jsonl) und schreibt sie nach
experience-store.jsonl + trajectories.jsonl. Ersetzt die alte, parasitaere
Logik (las nur die letzte session-scores-Zeile -> Platzhalter, Feldnamen-Mismatch,
datentot seit 2026-04-12).

Aufruf:
  python experience-logger.py            # SessionEnd: liest stdin (transcript_path)
  python experience-logger.py --dry-run  # parst neuestes Transcript, gibt JSON aus (kein Schreiben)
  python experience-logger.py --transcript <pfad> [--dry-run]

Direktive #3: Graceful Degradation — bei JEDEM Fehler exit 0, nie die Session blockieren.
"""
import sys, os, json, glob, tempfile
from datetime import datetime

EXPERIENCE = os.path.expanduser("~/proggs/.claude/agent-memory/shared/experience-store.jsonl")
TRAJECTORY = os.path.expanduser("~/proggs/.claude/agent-memory/shared/trajectories.jsonl")
SCORES     = os.path.expanduser("~/.claude/session-scores.jsonl")
PROJECTS   = os.path.expanduser("~/.claude/projects")

EXPERIENCE_LIMIT = 200
TRAJECTORY_LIMIT = 100
MIN_TOOLS = 5  # triviale Sessions (<5 Tool-Calls) nicht loggen (vgl. self-observation)

CORRECTION_PREFIXES = (
    "nein", "stop", "stopp", "falsch", "nicht so", "anders", "undo", "revert",
    "no,", "wrong", "mach das rueckgaengig", "mach das rückgängig", "nicht ",
)


def _reconfigure_stdout():
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass


def read_stdin_json():
    try:
        # isatty-Guard: bei interaktivem Aufruf (kein Pipe) NICHT blockierend lesen
        if sys.stdin is None or sys.stdin.isatty():
            return {}
        data = sys.stdin.read()
        if data and data.strip():
            return json.loads(data)
    except Exception:
        pass
    return {}


def find_transcript(stdin_obj):
    # 1) transcript_path aus SessionEnd-Input (zuverlaessig)
    if isinstance(stdin_obj, dict):
        tp = stdin_obj.get("transcript_path")
        if tp and os.path.isfile(tp):
            return tp
    # 2) Fallback: neuestes .jsonl ueber alle projects-Unterverzeichnisse
    cands = glob.glob(os.path.join(PROJECTS, "*", "*.jsonl"))
    if not cands:
        return None
    try:
        return max(cands, key=os.path.getmtime)
    except Exception:
        return None


def parse_transcript(path):
    tool_seq = []
    err = 0
    corrections = 0
    ts_first = ts_last = None
    task_desc = None
    assistant_turns = 0
    sid = None
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                o = json.loads(line)
            except Exception:
                continue
            t = o.get("type")
            ts = o.get("timestamp")
            if ts:
                if ts_first is None:
                    ts_first = ts
                ts_last = ts
            if sid is None:
                sid = o.get("sessionId")
            if t == "assistant":
                assistant_turns += 1
                for c in (o.get("message", {}) or {}).get("content") or []:
                    if isinstance(c, dict) and c.get("type") == "tool_use":
                        tool_seq.append(c.get("name"))
            elif t == "user":
                cont = (o.get("message", {}) or {}).get("content")
                if isinstance(cont, list):
                    for c in cont:
                        if isinstance(c, dict) and c.get("type") == "tool_result" and c.get("is_error"):
                            err += 1
                elif isinstance(cont, str):
                    s = cont.strip()
                    low = s.lower()
                    if (s and not s.startswith("<")
                            and "command-name" not in s
                            and "local-command" not in s
                            and "command-message" not in s):
                        if task_desc is None:
                            task_desc = s[:200]
                        if any(low.startswith(w) for w in CORRECTION_PREFIXES):
                            corrections += 1
    dur = 0
    if ts_first and ts_last:
        try:
            a = datetime.fromisoformat(ts_first.replace("Z", "+00:00"))
            b = datetime.fromisoformat(ts_last.replace("Z", "+00:00"))
            dur = max(0, int((b - a).total_seconds() // 60))
        except Exception:
            pass
    date = (ts_last or "")[:10] or datetime.now().strftime("%Y-%m-%d")
    return {
        "tool_sequence": tool_seq,
        "tool_count": len(tool_seq),
        "error_count": err,
        "corrections": corrections,
        "duration_minutes": dur,
        "task_description": task_desc or "session-auto-logged",
        "date": date,
        "turns": assistant_turns,
        "session_id": sid,
    }


def categorize(desc):
    d = (desc or "").lower()
    if any(k in d for k in ("build", "compile", "gradle", "error", "fix")):
        return "build_fix"
    if any(k in d for k in ("feature", "implement", "add ", "new ")):
        return "feature"
    if any(k in d for k in ("config", "setting", "hook", "setup", "drift")):
        return "config"
    if any(k in d for k in ("debug", "bug", "crash", "investigate")):
        return "debug"
    if any(k in d for k in ("research", "search", "find", "analyse", "analyze", "recherch")):
        return "research"
    if any(k in d for k in ("refactor", "clean", "improve", "optimi")):
        return "refactor"
    return "other"


def heuristic_score(err):
    # Transparente, konservative Heuristik (echte Qualitaet braeuchte einen Scorer).
    # Fehler -> niedrigerer Score: Lern-Signale lieber konservativ als schoengerechnet.
    if err == 0:
        return 5
    if err <= 2:
        return 4
    if err <= 5:
        return 3
    if err <= 10:
        return 2
    return 1


def utility(s, err, tools):
    # SICA-Formel: 0.5*(s/5) + 0.25*max(0,1-err/tools) + 0.25*max(0,1-tools/50)
    perf = 0.5 * (s / 5.0)
    eff = 0.25 * max(0, 1 - err / max(tools, 1))
    speed = 0.25 * max(0, 1 - tools / 50.0)
    return round(max(0.0, min(1.0, perf + eff + speed)), 3)


def append_jsonl(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "a", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(obj, ensure_ascii=False) + "\n")


def prune_near_miss_aware(path, limit):
    if not os.path.exists(path):
        return
    with open(path, encoding="utf-8") as f:
        lines = [l for l in (ln.rstrip("\n") for ln in f) if l.strip()]
    if len(lines) <= limit:
        return
    near, normal = [], []
    for l in lines:
        try:
            if json.loads(l).get("near_miss") is True:
                near.append(l)
            else:
                normal.append(l)
        except Exception:
            normal.append(l)
    # zuerst alte NORMALE Eintraege entfernen (Near-Miss bevorzugt behalten)
    overage = len(lines) - limit
    if overage > 0 and normal:
        normal = normal[min(overage, len(normal)):]
    combined = normal + near
    if len(combined) > limit:
        combined = combined[len(combined) - limit:]
    d = os.path.dirname(path)
    fd, tmp = tempfile.mkstemp(dir=d, suffix=".tmp")
    os.close(fd)
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(combined) + "\n")
    os.replace(tmp, path)


def build_entries(m):
    s = heuristic_score(m["error_count"])
    u = utility(s, m["error_count"], m["tool_count"])
    near = (2 <= s <= 3 and m["error_count"] > 0)
    cat = categorize(m["task_description"])
    experience = {
        "date": m["date"],
        "task_type": "session",
        "task_description": m["task_description"],
        "strategy": "transcript-derived",
        "tool_count": m["tool_count"],
        "error_count": m["error_count"],
        "success_score": s,
        "tags": ["auto-logged", "transcript"],
        "tool_sequence": m["tool_sequence"],
        "utility_score": u,
        "near_miss": near,
        "task_category": cat,
        "timestamp_decay": 1.0,
    }
    trajectory = {
        "date": m["date"],
        "task_description": m["task_description"],
        "tool_sequence": m["tool_sequence"],
        "errors": [],
        "error_count": m["error_count"],
        "corrections": m["corrections"],
        "duration_minutes": m["duration_minutes"],
        "success": s >= 4,
        "tags": ["auto-logged", "transcript"],
        "utility_score": u,
        "near_miss": near,
    }
    # session-scores.jsonl: ersetzt den toten session-scorer.ts (ein Logger, drei Outputs).
    # OBJEKTIVE Felder echt aus dem Transcript; quality_score ist eine TRANSPARENTE
    # Heuristik (success 1-5 -> 0-10). iq_score/meta_intelligence_score bleiben dem
    # /self-improve-Lauf ueberlassen (das sind keine SessionEnd-Messwerte).
    session_score = {
        "date": m["date"],
        "session_id": m.get("session_id"),
        "total_turns": m["turns"],
        "tool_calls": m["tool_count"],
        "errors": m["error_count"],
        "corrections": m["corrections"],
        "duration_min": m["duration_minutes"],
        "quality_score": round(s * 2.0, 1),
        "utility_score": u,
        "near_miss": near,
        "strategy": "transcript-derived",
    }
    return experience, trajectory, session_score


def main():
    _reconfigure_stdout()
    argv = sys.argv[1:]
    dry = "--dry-run" in argv or os.environ.get("EXPLOG_DRYRUN") == "1"
    forced = None
    if "--transcript" in argv:
        i = argv.index("--transcript")
        if i + 1 < len(argv):
            forced = argv[i + 1]

    # stdin (transcript_path) auch im Dry-Modus nutzen, damit der Test den echten
    # SessionEnd-Pfad prueft. Nur bei explizitem --transcript wird stdin uebersprungen.
    stdin_obj = {} if forced else read_stdin_json()
    tpath = forced or find_transcript(stdin_obj)
    if not tpath or not os.path.isfile(tpath):
        if dry:
            print(json.dumps({"error": "no transcript found", "checked": PROJECTS}, ensure_ascii=False))
        return 0

    m = parse_transcript(tpath)
    if m["tool_count"] < MIN_TOOLS:
        if dry:
            print(json.dumps({"skipped": f"tool_count<{MIN_TOOLS}", "metrics": m}, ensure_ascii=False))
        return 0

    experience, trajectory, session_score = build_entries(m)
    if dry:
        print("TRANSCRIPT: " + tpath)
        print("EXPERIENCE: " + json.dumps(experience, ensure_ascii=False))
        print("TRAJECTORY: " + json.dumps(trajectory, ensure_ascii=False))
        print("SESSIONSCORE: " + json.dumps(session_score, ensure_ascii=False))
        return 0

    append_jsonl(EXPERIENCE, experience)
    append_jsonl(TRAJECTORY, trajectory)
    append_jsonl(SCORES, session_score)
    prune_near_miss_aware(EXPERIENCE, EXPERIENCE_LIMIT)
    prune_near_miss_aware(TRAJECTORY, TRAJECTORY_LIMIT)
    return 0


if __name__ == "__main__":
    try:
        main()
    except Exception:
        pass
    sys.exit(0)

#!/usr/bin/env python3
"""session-presence.py — Awareness fuer parallele Claude-Code-Sessions im selben
Working-Tree. Gibt einen unverbindlichen HINWEIS, wenn eine andere Session gerade hier
arbeitet — KEINE Sperre.

Datenbasis: das gemeinsame Session-Register
~/proggs/.claude/agent-memory/shared/active-tasks.jsonl (von den task-ledger-*-Hooks
gepflegt: session_id, cwd, files_changed, status, timestamp_last_update, pushed).

Modi (1. Argument):
  warn   -> UserPromptSubmit: Awareness. Warnt (additionalContext), wenn eine andere
            LEBENDE Session im SELBEN Projekt (cwd) arbeitet. Reiner Hinweis, kein Block.
  guard  -> NO-OP (deaktiviert am 2026-06-25 auf Frank-Wunsch). Frueher ein PreToolUse-
            Datei-Waechter, der Edit/Write per permissionDecision:deny BLOCKIERTE, sobald
            zwei Sessions dieselbe Datei beruehrten. Das fuehrte zum Deadlock: zwei Sessions
            sperrten sich GEGENSEITIG aus -> beide arbeiteten nicht weiter, kein Ergebnis
            wurde gespeichert. Es darf nur noch der Hinweis kommen, NIE eine Sperre. Dieser
            Modus tut jetzt bewusst NICHTS (damit auch noch nicht neu gestartete Sessions,
            deren gecachte settings.json den Hook noch aufruft, sofort entsperrt sind).
            Regel: ~/.claude/rules/session-presence.md.

Robust (bugs/best-practices claude-tooling/python-windows.md): open() mit encoding='utf-8',
JSON ensure_ascii=False, stdout.reconfigure(utf-8) gegen cp1252-Crash, Pfade via expanduser.
stdin per json.loads (NICHT jq, claude-hooks.md 16.2). Gibt NUR strikt spec-konformes
hookSpecificOutput-JSON aus (16.1: non-spec JSON crasht die Session) ODER gar nichts.
Crasht nie: jeder Fehler -> stilles Ende, exit 0. Cross-Platform, nur stdlib.
Notaus: Datei <TEMP>/session-presence-disable.flag schaltet alles ab.
"""
import json
import os
import sys
import tempfile
from datetime import datetime, timezone

LEDGER = os.path.expanduser("~/proggs/.claude/agent-memory/shared/active-tasks.jsonl")
WARN_TTL = 8 * 60      # warn: Session gilt als "lebendig", wenn letztes Update < 8 Min her
INACTIVE_STATES = {"done", "completed", "abandoned", "paused"}
DISABLE_FLAG = os.path.join(tempfile.gettempdir(), "session-presence-disable.flag")


def _now():
    return datetime.now(timezone.utc)


def _parse_ts(s):
    try:
        return datetime.fromisoformat(s)
    except Exception:
        return None


def _read_input():
    try:
        raw = sys.stdin.read()
    except Exception:
        return {}
    if not raw or not raw.strip():
        return {}
    try:
        d = json.loads(raw)
        return d if isinstance(d, dict) else {}
    except Exception:
        return {}


def _emit(obj):
    try:
        sys.stdout.reconfigure(encoding="utf-8")  # Windows cp1252-stdout vermeiden
    except Exception:
        pass
    sys.stdout.write(json.dumps(obj, ensure_ascii=False))


def _live_other_sessions(my_sid, ttl):
    """Neuester Eintrag je ANDERER lebender Session (Heartbeat < ttl, Status aktiv)."""
    if not os.path.isfile(LEDGER):
        return []
    latest = {}
    try:
        with open(LEDGER, encoding="utf-8") as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                try:
                    e = json.loads(line)
                except Exception:
                    continue
                sid = e.get("session_id")
                if not sid or sid == my_sid:
                    continue
                prev = latest.get(sid)
                if prev is None or (e.get("timestamp_last_update", "") > prev.get("timestamp_last_update", "")):
                    latest[sid] = e
    except Exception:
        return []
    now = _now()
    live = []
    for sid, e in latest.items():
        if str(e.get("status", "")).lower() in INACTIVE_STATES:
            continue
        ts = _parse_ts(e.get("timestamp_last_update", "") or e.get("timestamp_start", ""))
        if ts is None:
            continue
        age = (now - ts).total_seconds()
        if age < 0:
            age = 0
        if age <= ttl:
            e["_age_s"] = int(age)
            live.append(e)
    return live


def _short(sid):
    return (sid or "")[:8]


def _norm(p):
    try:
        return os.path.normcase(os.path.normpath(p))
    except Exception:
        return p


def mode_warn(data):
    my_sid = data.get("session_id", "")
    my_cwd = data.get("cwd", "") or ""
    if not my_cwd:
        return
    my_cwd_n = _norm(my_cwd)
    same_project = []
    for e in _live_other_sessions(my_sid, WARN_TTL):
        cwd = e.get("cwd", "") or ""
        if cwd and _norm(cwd) == my_cwd_n:
            same_project.append(e)
    if not same_project:
        return  # kein Overlap -> kein Output (kein History-Spam)
    proj = os.path.basename(my_cwd.rstrip("/\\")) or my_cwd
    lines = []
    for e in same_project:
        files = sorted({os.path.basename(f) for f in (e.get("files_changed") or [])
                        if isinstance(f, str) and f})
        flist = (", ".join(files[:6]) + (" …" if len(files) > 6 else "")) if files else "noch keine Datei editiert"
        lines.append(f"  - Session {_short(e.get('session_id'))} (aktiv vor {e.get('_age_s', 0)}s): {flist}")
    text = (f"HINWEIS — PARALLELE SESSION(S) IM SELBEN PROJEKT '{proj}': {len(same_project)} andere "
            f"Claude-Code-Session(en) arbeiten gerade hier:\n" + "\n".join(lines) +
            "\nDas ist nur ein Hinweis, KEINE Sperre — du darfst normal weiterarbeiten. Achte beim "
            "Speichern darauf, moeglichst nicht exakt dieselbe Datei gleichzeitig zu editieren "
            "(sonst kann die spaetere Speicherung die fruehere ueberschreiben). Nur eigene Dateien "
            "committen, fetch+rebase vor Push. Wuerdest du genau eine dieser Dateien aendern: "
            "kurz schauen, ob du erst woanders weitermachen oder die Datei vorher neu lesen kannst. "
            "Regel session-presence.")
    _emit({"hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": text}})


def main():
    if os.path.exists(DISABLE_FLAG):
        return
    mode = sys.argv[1] if len(sys.argv) > 1 else "warn"
    # 'guard' wurde am 2026-06-25 auf Frank-Wunsch ENTFERNT (Deadlock durch gegenseitiges
    # Aussperren). Es bleibt NUR der unverbindliche Hinweis. 'guard' ist ein No-Op -> sofort
    # wirksam auch fuer Sessions, deren gecachte settings.json den Hook noch aufruft.
    if mode == "guard":
        return
    data = _read_input()
    if not data:
        return
    mode_warn(data)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        pass
    sys.exit(0)

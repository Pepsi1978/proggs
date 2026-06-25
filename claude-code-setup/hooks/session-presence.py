#!/usr/bin/env python3
"""session-presence.py — Verhindert, dass parallele Claude-Code-Sessions im selben
Working-Tree sich gegenseitig die Arbeit ueberschreiben.

Datenbasis: das gemeinsame Session-Register
~/proggs/.claude/agent-memory/shared/active-tasks.jsonl (von den task-ledger-*-Hooks
gepflegt: session_id, cwd, files_changed, status, timestamp_last_update, pushed).

Modi (1. Argument):
  warn   -> UserPromptSubmit: Awareness. Warnt (additionalContext), wenn eine andere
            LEBENDE Session im SELBEN Projekt (cwd) arbeitet. Kein Block.
  guard  -> PreToolUse (Edit|Write): Datei-Waechter. Wenn die Zieldatei GERADE von einer
            anderen lebenden Session bearbeitet wird (file_path in deren files_changed),
            -> permissionDecision:deny + Begruendung (kein Warten -> kein Deadlock;
            Best-Practice claude-sessions-board, siehe best-practices/claude-tooling/
            multi-session-koordination.md).

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
GUARD_TTL = 6 * 60     # guard: nur bei FRISCH aktiver anderer Session blockieren (konservativ)
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


def _other_files(entry):
    out = set()
    for f in (entry.get("files_changed") or []):
        if isinstance(f, str) and f:
            out.add(_norm(f))
    return out


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
    text = (f"PARALLELE SESSION(S) IM SELBEN PROJEKT '{proj}': {len(same_project)} andere "
            f"Claude-Code-Session(en) arbeiten gerade hier:\n" + "\n".join(lines) +
            "\nVorsicht beim Speichern: nicht dieselbe Datei gleichzeitig editieren. Nur eigene "
            "Dateien committen, fetch+rebase vor Push. Beruehrt ihr dieselbe Datei, blockiert der "
            "Datei-Waechter den Edit (Regel session-presence).")
    _emit({"hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": text}})


def mode_guard(data):
    my_sid = data.get("session_id", "")
    ti = data.get("tool_input") or {}
    fp = ti.get("file_path", "") or ""
    if not fp:
        return
    fp_n = _norm(fp)
    conflicts = [e for e in _live_other_sessions(my_sid, GUARD_TTL) if fp_n in _other_files(e)]
    if not conflicts:
        return
    who = ", ".join(f"Session {_short(e.get('session_id'))} (aktiv vor {e.get('_age_s', 0)}s)"
                    for e in conflicts)
    base = os.path.basename(fp)
    reason = (f"UEBERSCHREIB-SCHUTZ: Die Datei '{base}' wird GERADE von einer anderen laufenden "
              f"Claude-Code-Session bearbeitet ({who}). Wuerdest du jetzt speichern, koenntest du "
              f"deren Arbeit ueberschreiben. Optionen: (1) kurz an einer ANDEREN Datei "
              f"weiterarbeiten und es spaeter erneut versuchen — der Block faellt automatisch weg, "
              f"sobald die andere Session fertig/inaktiv ist; (2) wenn du sicher bist, dass kein "
              f"Konflikt besteht, die Datei zuerst NEU lesen (Read) und gezielt aendern. "
              f"Nicht blind ueberschreiben. (Notaus bei Fehlalarm: leere Datei "
              f"'session-presence-disable.flag' im TEMP-Verzeichnis anlegen.)")
    _emit({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                  "permissionDecision": "deny",
                                  "permissionDecisionReason": reason}})


def main():
    if os.path.exists(DISABLE_FLAG):
        return
    mode = sys.argv[1] if len(sys.argv) > 1 else "warn"
    data = _read_input()
    if not data:
        return
    if mode == "guard":
        mode_guard(data)
    else:
        mode_warn(data)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        pass
    sys.exit(0)

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Active-Task-Ledger fuer Claude Code.

Schreibt User-Prompts, Tool-Use und Commits in eine JSONL-Datei, sodass nach
einem Konto-Wechsel oder Token-Aus an der letzten Stelle weitergearbeitet werden
kann.

Resilienz (Direktive #3):
- Atomares Write (temp + os.replace) gegen halb-geschriebene Dateien
- File-Lock mit Stale-Detection (30s) fuer parallele Sessions
- UTF-8 Pflicht auf Windows (encoding='utf-8' ueberall)
- Graceful Failure: jeder unbekannte Fehler -> exit 0, blockiert die Session nie
- Input-Validation: fehlende session_id -> exit 0 ohne Side-Effect
- Pruning bei >MAX_ENTRIES Eintraegen

Aufruf-Modi:
  task-ledger-helper.py add        # stdin = UserPromptSubmit Event JSON
  task-ledger-helper.py update     # stdin = PostToolUse Event JSON
  task-ledger-helper.py close      # stdin = Stop Event JSON
  task-ledger-helper.py list [STATUS]   # listet Eintraege
  task-ledger-helper.py resume     # Kandidaten fuer "weiter machen"
"""

import json
import os
import re
import sys
import tempfile
import time
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

# Pfade — plattformunabhaengig
HOME = Path(os.path.expanduser("~"))
LEDGER_DIR = HOME / "proggs" / ".claude" / "agent-memory" / "shared"
LEDGER_FILE = LEDGER_DIR / "active-tasks.jsonl"
LOCK_FILE = LEDGER_DIR / "active-tasks.lock"

# Limits
MAX_ENTRIES = 500
MAX_PROMPT_LEN = 8000
MAX_FILES_PER_TASK = 100
MAX_COMMITS_PER_TASK = 50
LOCK_TIMEOUT_S = 5
LOCK_STALE_S = 30
RESUME_LOOKBACK_DAYS = 30


def now_iso():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def acquire_lock(timeout=LOCK_TIMEOUT_S):
    start = time.time()
    while time.time() - start < timeout:
        try:
            fd = os.open(str(LOCK_FILE), os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            try:
                os.write(fd, str(os.getpid()).encode("utf-8"))
            finally:
                os.close(fd)
            return True
        except FileExistsError:
            try:
                age = time.time() - LOCK_FILE.stat().st_mtime
                if age > LOCK_STALE_S:
                    try:
                        LOCK_FILE.unlink()
                    except FileNotFoundError:
                        pass
                    continue
            except FileNotFoundError:
                continue
            time.sleep(0.05)
    return False


def release_lock():
    try:
        LOCK_FILE.unlink()
    except FileNotFoundError:
        pass


def ensure_dir():
    LEDGER_DIR.mkdir(parents=True, exist_ok=True)


def read_ledger():
    if not LEDGER_FILE.exists():
        return []
    entries = []
    try:
        with open(LEDGER_FILE, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    entries.append(json.loads(line))
                except json.JSONDecodeError:
                    continue
    except OSError:
        return []
    return entries


def write_ledger(entries):
    ensure_dir()
    if len(entries) > MAX_ENTRIES:
        entries.sort(key=lambda e: e.get("timestamp_start", ""))
        # Behalte die juengsten MAX_ENTRIES und alle nicht-abgeschlossenen
        kept_old_done = entries[-MAX_ENTRIES:]
        still_open = [e for e in entries if e.get("status") not in ("done",)]
        merged = {}
        for e in kept_old_done + still_open:
            merged[e.get("task_id", id(e))] = e
        entries = sorted(merged.values(), key=lambda e: e.get("timestamp_start", ""))[-MAX_ENTRIES:]

    fd, tmp_path = tempfile.mkstemp(prefix=".active-tasks-", suffix=".tmp", dir=str(LEDGER_DIR))
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as tmp:
            for e in entries:
                tmp.write(json.dumps(e, ensure_ascii=False))
                tmp.write("\n")
        os.replace(tmp_path, str(LEDGER_FILE))
    except Exception:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
        raise


def current_task(session_id, entries):
    """Neuester Eintrag dieser Session der nicht 'done' ist."""
    cands = [
        e for e in entries
        if e.get("session_id") == session_id and e.get("status") != "done"
    ]
    if not cands:
        return None
    cands.sort(
        key=lambda e: e.get("timestamp_last_update", e.get("timestamp_start", "")),
        reverse=True,
    )
    return cands[0]


def cmd_add(session_id, prompt_text, cwd=None):
    if not acquire_lock():
        return
    try:
        entries = read_ledger()
        # Vorherige offene Tasks dieser Session als 'committed' markieren —
        # ein neuer Prompt heisst: das vorherige Thema ist abgeschlossen oder pausiert.
        for e in entries:
            if e.get("session_id") == session_id and e.get("status") in ("open", "in_progress"):
                if e.get("commits"):
                    e["status"] = "committed"
                else:
                    e["status"] = "paused"
                e["timestamp_last_update"] = now_iso()

        new_task = {
            "task_id": str(uuid.uuid4()),
            "session_id": session_id,
            "timestamp_start": now_iso(),
            "timestamp_last_update": now_iso(),
            "prompt_text": (prompt_text or "")[:MAX_PROMPT_LEN],
            "status": "open",
            "files_changed": [],
            "commits": [],
            "pushed": False,
            "tool_count": 0,
            "cwd": cwd or os.getcwd(),
        }
        entries.append(new_task)
        write_ledger(entries)
    finally:
        release_lock()


def cmd_update(session_id, tool_name, tool_input, tool_output_text):
    if not acquire_lock():
        return
    try:
        entries = read_ledger()
        task = current_task(session_id, entries)
        if not task:
            # Kein offener Task — wir erstellen still einen Platzhalter, damit
            # Tool-Aktivitaet nicht verloren geht (z.B. wenn UserPromptSubmit-Hook
            # mal nicht gefeuert hat).
            task = {
                "task_id": str(uuid.uuid4()),
                "session_id": session_id,
                "timestamp_start": now_iso(),
                "timestamp_last_update": now_iso(),
                "prompt_text": "(automatisch nachgetragen — Prompt-Hook hat nicht gefeuert)",
                "status": "in_progress",
                "files_changed": [],
                "commits": [],
                "pushed": False,
                "tool_count": 0,
                "cwd": os.getcwd(),
            }
            entries.append(task)

        task["tool_count"] = int(task.get("tool_count", 0)) + 1
        task["status"] = "in_progress"
        task["timestamp_last_update"] = now_iso()

        if tool_name in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
            try:
                ti = tool_input if isinstance(tool_input, dict) else {}
                file_path = ti.get("file_path") or ti.get("notebook_path")
                if file_path:
                    files = task.setdefault("files_changed", [])
                    if file_path not in files and len(files) < MAX_FILES_PER_TASK:
                        files.append(file_path)
            except (AttributeError, TypeError):
                pass

        if tool_name == "Bash":
            try:
                ti = tool_input if isinstance(tool_input, dict) else {}
                command = ti.get("command", "") or ""
                if "git commit" in command:
                    m = re.search(r"\[(?:\S+)\s+([0-9a-f]{7,40})\]", tool_output_text or "")
                    if m:
                        commits = task.setdefault("commits", [])
                        h = m.group(1)
                        if h not in commits and len(commits) < MAX_COMMITS_PER_TASK:
                            commits.append(h)
                if re.search(r"\bgit\s+push\b", command):
                    if "rejected" not in (tool_output_text or "").lower():
                        task["pushed"] = True
            except (AttributeError, TypeError):
                pass

        write_ledger(entries)
    finally:
        release_lock()


def cmd_close(session_id):
    if not acquire_lock():
        return
    try:
        entries = read_ledger()
        task = current_task(session_id, entries)
        if not task:
            return
        if task.get("commits") and task.get("pushed"):
            task["status"] = "done"
        elif task.get("commits"):
            task["status"] = "committed"
        else:
            task["status"] = "paused"
        task["timestamp_last_update"] = now_iso()
        write_ledger(entries)
    finally:
        release_lock()


def cmd_list(filter_status=None):
    entries = read_ledger()
    if filter_status:
        entries = [e for e in entries if e.get("status") == filter_status]
    entries.sort(
        key=lambda e: e.get("timestamp_last_update", e.get("timestamp_start", "")),
        reverse=True,
    )
    print(json.dumps(entries, ensure_ascii=False, indent=2))


def cmd_resume():
    """Top-Kandidaten fuer 'weiter machen' — alle nicht-done, max 30 Tage alt."""
    entries = read_ledger()
    cutoff = (datetime.now(timezone.utc) - timedelta(days=RESUME_LOOKBACK_DAYS)).isoformat(timespec="seconds")
    cands = [
        e for e in entries
        if e.get("status") in ("open", "in_progress", "committed", "paused")
        and e.get("timestamp_last_update", "") > cutoff
    ]
    cands.sort(key=lambda e: e.get("timestamp_last_update", ""), reverse=True)
    print(json.dumps(cands[:10], ensure_ascii=False, indent=2))


def _read_stdin_json():
    raw = sys.stdin.read()
    if not raw or not raw.strip():
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None


def _extract_tool_output_text(tool_response):
    if tool_response is None:
        return ""
    if isinstance(tool_response, str):
        return tool_response
    if isinstance(tool_response, dict):
        for key in ("stdout", "output", "content", "text"):
            v = tool_response.get(key)
            if isinstance(v, str) and v:
                return v
        try:
            return json.dumps(tool_response, ensure_ascii=False)
        except (TypeError, ValueError):
            return str(tool_response)
    return str(tool_response)


def main():
    if len(sys.argv) < 2:
        return 0
    cmd = sys.argv[1]
    try:
        if cmd == "add":
            data = _read_stdin_json() or {}
            session_id = data.get("session_id") or ""
            prompt_text = data.get("prompt") or ""
            cwd = data.get("cwd")
            if not session_id:
                return 0
            cmd_add(session_id, prompt_text, cwd)

        elif cmd == "update":
            data = _read_stdin_json() or {}
            session_id = data.get("session_id") or ""
            tool_name = data.get("tool_name") or ""
            tool_input = data.get("tool_input") or {}
            tool_response = data.get("tool_response")
            tool_output_text = _extract_tool_output_text(tool_response)
            if not session_id:
                return 0
            cmd_update(session_id, tool_name, tool_input, tool_output_text)

        elif cmd == "close":
            data = _read_stdin_json() or {}
            session_id = data.get("session_id") or ""
            if not session_id:
                return 0
            cmd_close(session_id)

        elif cmd == "list":
            filter_status = sys.argv[2] if len(sys.argv) > 2 else None
            cmd_list(filter_status)

        elif cmd == "resume":
            cmd_resume()

        else:
            return 0

    except Exception as e:
        # Graceful Degradation: Hook darf Session NIE blockieren.
        try:
            sys.stderr.write(f"task-ledger-helper error: {e}\n")
        except Exception:
            pass
        return 0
    return 0


if __name__ == "__main__":
    sys.exit(main())

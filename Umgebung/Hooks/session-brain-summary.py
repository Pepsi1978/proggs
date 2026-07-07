#!/usr/bin/env python3
"""session-brain-summary.py — D27/D32 (Gruppe D "Mitlernen in Programmier-Sessions").

SessionEnd-Hook-Kernlogik (EINE Logik fuer beide Plattformen; duenne Wrapper:
session-brain-summary.ps1 / .sh): sammelt am Ende jeder Claude-Code-Session die
Rohdaten (Franks User-Prompts aus dem Transkript, git-Commits + geaenderte Dateien
der Session) und schickt sie an den Second-Brain-Agenten (POST /session-log,
WireGuard 10.8.0.1:8002). Der Server verdichtet per LLM zu "gemacht/entschieden/
gelernt" und legt den Eintrag unter Programmierung/Sessions ins Gehirn; danach
pflegt er den Kern-Block "Woran Frank gerade baut" nach (D28).

Resilienz (claude-hooks-Almanach):
- IMMER exit 0 — dieser Hook darf ein Session-Ende NIE blockieren (fail-open).
- stdin tolerant lesen; kein jq; alles Python-stdlib (urllib statt httpx).
- Schema-Canary (D32): erkennt, wenn Claude Code sein Transkript-Format aendert,
  und meldet das LAUT ins eigene Log + als canary_ok=false an den Server, statt
  still leere Daten zu liefern.
- Secrets werden VOR dem Senden redaktiert (Muster wie task-ledger-helper).
- Server nicht erreichbar (WireGuard aus, VPS down) -> stiller Verzicht mit
  Log-Zeile; die Session endet normal.

Log: ~/.claude/logs/session-brain-summary.log (JSON-Lines).
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

AGENT_URL = os.environ.get("SB_AGENT_URL", "http://10.8.0.1:8002").rstrip("/")
BRAIN_ENV = Path.home() / "SK" / "second-brain" / "brain.env"   # SB_API_KEY lebt NUR im SK-Ordner
LOG_FILE = Path.home() / ".claude" / "logs" / "session-brain-summary.log"
MAX_PROMPTS = 300          # Server-Schema-Limit
PROMPT_CHARS = 2000        # je Prompt (Server kappt zusaetzlich)
MAX_COMMITS = 150
MAX_FILES = 200
HTTP_TIMEOUT = 20          # s — der Server antwortet sofort (Verdichtung laeuft dort im Hintergrund)

# Nur eindeutige Key-Praefixe (keine False-Positives) — Muster wie task-ledger-helper._redact_secrets:
_SECRET_PATTERNS = [
    r"tvly-[A-Za-z0-9-]{16,}", r"sk-[A-Za-z0-9_-]{20,}", r"gh[pousr]_[A-Za-z0-9]{30,}",
    r"github_pat_[A-Za-z0-9_]{30,}", r"AIza[A-Za-z0-9_-]{30,}", r"glpat-[A-Za-z0-9_-]{20,}",
    r"xox[baprs]-[A-Za-z0-9-]{10,}", r"fc-[A-Za-z0-9]{20,}", r"nvapi-[A-Za-z0-9_-]{20,}",
    r"gsk_[A-Za-z0-9]{20,}", r"r8_[A-Za-z0-9]{20,}",
]
_SECRET_RX = re.compile("|".join(_SECRET_PATTERNS))

# Marker fuer NICHT-Prompts (Command-/Caveat-/System-Zeilen im user-Slot):
_SKIP_PREFIXES = ("<command-name>", "<local-command-stdout", "<local-command-caveat",
                  "Caveat:", "<system-reminder>", "[Request interrupted")


def _log(level: str, msg: str, **ctx) -> None:
    """Eigenes JSON-Lines-Log (Observability-First) — Fehler hier sind selbst egal."""
    try:
        LOG_FILE.parent.mkdir(parents=True, exist_ok=True)
        if LOG_FILE.exists() and LOG_FILE.stat().st_size > 2_000_000:   # simple Rotation
            LOG_FILE.replace(LOG_FILE.with_suffix(".log.old"))
        with LOG_FILE.open("a", encoding="utf-8", newline="\n") as fh:
            fh.write(json.dumps({"ts": datetime.now().astimezone().isoformat(timespec="seconds"),
                                 "level": level, "module": "session-brain-summary", "msg": msg,
                                 "ctx": ctx}, ensure_ascii=False) + "\n")
    except Exception:
        pass


def _redact(text: str) -> str:
    return _SECRET_RX.sub("[REDACTED-SECRET]", text or "")


def _read_stdin() -> dict:
    try:
        raw = sys.stdin.buffer.read().decode("utf-8", "replace").strip()
        return json.loads(raw) if raw else {}
    except Exception:
        return {}


def _extract_prompts(transcript_path: str) -> "tuple[list[str], str | None, bool]":
    """Liest das Transkript-JSONL und extrahiert Franks ECHTE User-Prompts in Reihenfolge.
    Rueckgabe: (prompts, erster_timestamp_iso, canary_ok).
    Schema-Canary (D32): canary_ok=False, wenn die Datei substanziell ist, aber KEINE Zeile
    dem erwarteten Format entspricht (type user/assistant + message) — dann hat sich das
    Claude-Code-Log-Format geaendert und dieser Import braucht Pflege (claude-engram-Muster)."""
    prompts: list[str] = []
    first_ts: "str | None" = None
    total_lines = 0
    known_shape = 0   # Zeilen, die dem erwarteten Schema entsprechen
    try:
        with open(transcript_path, encoding="utf-8", errors="replace") as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                total_lines += 1
                try:
                    d = json.loads(line)
                except Exception:
                    continue
                if not isinstance(d, dict):
                    continue
                typ = d.get("type")
                msg = d.get("message")
                if typ in ("user", "assistant") and isinstance(msg, dict):
                    known_shape += 1
                if first_ts is None and isinstance(d.get("timestamp"), str):
                    first_ts = d["timestamp"]
                if typ != "user" or not isinstance(msg, dict):
                    continue
                if d.get("isSidechain") or d.get("isMeta"):
                    continue   # Subagent-/Meta-Zeilen sind keine Frank-Prompts
                content = msg.get("content")
                text = ""
                if isinstance(content, str):
                    text = content
                elif isinstance(content, list):
                    # Nur reine Text-Arrays sind echte Prompts; tool_result-Listen sind Tool-Antworten.
                    if any(isinstance(it, dict) and it.get("type") == "tool_result" for it in content):
                        continue
                    text = "\n".join(it.get("text", "") for it in content
                                     if isinstance(it, dict) and it.get("type") == "text")
                text = (text or "").strip()
                if not text or text.startswith(_SKIP_PREFIXES):
                    continue
                # Eingebettete system-reminder-Bloecke aus dem Prompt-Text entfernen:
                text = re.sub(r"<system-reminder>.*?</system-reminder>", "", text, flags=re.DOTALL).strip()
                if text:
                    prompts.append(_redact(text)[:PROMPT_CHARS])
    except FileNotFoundError:
        # LAUT loggen (nie still) — haeufigste Ursache: Pfad-Form (Git-Bash /c/... vs. C:\...).
        _log("WARNING", "Transkript nicht gefunden — kein Session-Log moeglich", path=str(transcript_path))
        return [], None, True   # kein Canary-Fall (Format unbekannt bleibt unbewertet)
    except Exception as e:
        _log("ERROR", "Transkript-Lesen fehlgeschlagen", error=f"{type(e).__name__}: {e}")
        return [], None, True
    canary_ok = not (total_lines >= 10 and known_shape == 0)
    if not canary_ok:
        _log("WARNING", "SCHEMA-CANARY: Transkript-Format unbekannt — Claude-Code-Log-Format geaendert? "
                        "session-brain-summary.py braucht Pflege.", lines=total_lines)
    return prompts[:MAX_PROMPTS], first_ts, canary_ok


def _git(cwd: str, *args: str) -> str:
    try:
        r = subprocess.run(["git", "-C", cwd, *args], capture_output=True, text=True,
                           encoding="utf-8", errors="replace", timeout=20)
        return r.stdout if r.returncode == 0 else ""
    except Exception:
        return ""


def _collect_git(cwd: str, since_iso: "str | None") -> "tuple[list[str], list[str]]":
    """Commits + geaenderte Dateien der Session (seit Session-Beginn). Kein git-Repo/kein
    Timestamp -> leere Listen (der Prompts-Teil traegt den Eintrag dann allein)."""
    if not cwd or not since_iso or not _git(cwd, "rev-parse", "--is-inside-work-tree").strip():
        return [], []
    commits = [c.strip() for c in _git(cwd, "log", "--oneline", f"--since={since_iso}").splitlines() if c.strip()]
    files_raw = _git(cwd, "log", "--name-only", "--pretty=format:", f"--since={since_iso}")
    seen, files = set(), []
    for f in files_raw.splitlines():
        f = f.strip()
        if f and f not in seen:
            seen.add(f)
            files.append(f)
    return commits[:MAX_COMMITS], files[:MAX_FILES]


def _api_key() -> str:
    try:
        for line in BRAIN_ENV.read_text(encoding="utf-8", errors="replace").splitlines():
            if line.strip().startswith("SB_API_KEY="):
                return line.split("=", 1)[1].strip().strip('"').strip("'")
    except Exception:
        pass
    return ""


def _norm_path(p: str) -> str:
    """Git-Bash-Pfade (/c/Users/...) nach Windows (C:/Users/...) normalisieren — Python kann
    das /c/-Format nicht oeffnen (python-windows-Almanach §3.1). Defense in Depth: der echte
    SessionEnd-Input liefert Windows-Pfade, aber manuelle Tests/andere Caller evtl. nicht."""
    m = re.match(r"^/([a-zA-Z])/(.*)$", p or "")
    return f"{m.group(1).upper()}:/{m.group(2)}" if (m and os.name == "nt") else (p or "")


def main() -> int:
    data = _read_stdin()
    transcript = _norm_path(data.get("transcript_path") or "")
    cwd = _norm_path(data.get("cwd") or os.getcwd())
    if not transcript:
        return 0   # ohne Transkript gibt es nichts Sinnvolles zu melden
    prompts, first_ts, canary_ok = _extract_prompts(transcript)
    commits, files = _collect_git(cwd, first_ts)
    if len(prompts) < 2 and not commits:
        _log("INFO", "Zu wenig Substanz — kein Session-Log gesendet",
             prompts=len(prompts), commits=len(commits))
        return 0
    key = _api_key()
    if not key:
        _log("WARNING", "SB_API_KEY nicht gefunden (~/SK/second-brain/brain.env) — Session-Log uebersprungen")
        return 0
    now_iso = datetime.now(timezone.utc).astimezone().isoformat(timespec="minutes")
    payload = {
        "cli": "Claude Code",
        "project": Path(cwd).name or "",
        "session_id": (data.get("session_id") or "")[:80] or None,
        "started": (first_ts or "")[:40] or None,
        "ended": now_iso[:40],
        "prompts": prompts,
        "commits": [_redact(c)[:300] for c in commits],
        "files": files,
        "notes": f"SessionEnd-Grund: {data.get('reason') or 'unbekannt'}",
        "canary_ok": canary_ok,
    }
    try:
        req = urllib.request.Request(
            f"{AGENT_URL}/session-log",
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
            method="POST")
        with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT) as resp:
            body = json.loads(resp.read().decode("utf-8", "replace") or "{}")
        _log("INFO", "Session-Log an den Agenten uebergeben",
             accepted=body.get("accepted"), reason=body.get("reason"),
             prompts=len(prompts), commits=len(commits), files=len(files), project=payload["project"])
    except Exception as e:
        # Server nicht erreichbar (WireGuard aus / VPS down) — stiller Verzicht, Session endet normal.
        _log("WARNING", "Agent nicht erreichbar — Session-Log verworfen",
             error=f"{type(e).__name__}: {e}", url=AGENT_URL)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:   # absolutes Netz: dieser Hook blockiert NIE ein Session-Ende
        _log("ERROR", "Unerwarteter Fehler", error=f"{type(e).__name__}: {e}")
        sys.exit(0)

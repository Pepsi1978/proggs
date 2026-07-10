"""Core memory blocks (Kern-Bloecke) for the Cortex chat agent — Level-2 plan #2/#3.

Small, always-present knowledge blocks (who Frank is, current goals, running
projects, open tasks, preferences) injected into EVERY chat system prompt —
the permanent overview, modelled after Letta/MemGPT core memory.

Pure logic, no FastAPI import (unit-testable in isolation, like rules.py).
Storage lives on the Samba mount: /logbook/Kernbloecke/kernbloecke.json
(current state, small — read every turn) plus kernbloecke-log.jsonl
(append-only change log holding every previous version — lossless history,
never loaded into the prompt).

All writes are atomic and cp1252-safe per bugs/claude-tooling/python-windows.md
(encoding='utf-8', ensure_ascii=False, newline='\\n', temp-in-same-dir + flush +
fsync + os.replace).
"""
from __future__ import annotations

import json
import logging
import os
import threading
import uuid

# Hard per-block character limit (Letta pattern): forces the agent to CONDENSE
# instead of endlessly appending — quality over volume, prompt stays small.
MAX_BLOCK_CHARS = 2000

# Fixed block set. Keys are stable ids (used by tool + REST + dashboard);
# titel/hinweis are shown to Frank and to the agent in the prompt.
BLOCK_DEFS: "dict[str, dict]" = {
    "profil": {
        "titel": "Wer Frank ist",
        "hinweis": "Grundwissen über Frank: wer er ist, was ihm wichtig ist, wie er spricht.",
    },
    "ziele": {
        "titel": "Aktuelle Ziele",
        "hinweis": "Woran Frank gerade arbeitet und was er erreichen will (mittelfristig).",
    },
    "projekte": {
        "titel": "Laufende Projekte",
        "hinweis": "Stand der wichtigsten Projekte/Baustellen (kurz, aktuell halten).",
    },
    "aufgaben": {
        "titel": "Offene Aufgaben",
        "hinweis": "Was aktuell ansteht oder nicht vergessen werden darf.",
    },
    "vorlieben": {
        "titel": "Vorlieben & Antwort-Stil",
        "hinweis": "Wie Frank Antworten mag (Ton, Länge, Format) und dauerhafte Vorlieben.",
    },
}

# RLock: set_block holds the lock over the whole load->mutate->save cycle
# (no lost update between the chat tool and a parallel dashboard edit).
_LOCK = threading.RLock()
_log = logging.getLogger("sb-agent.coreblocks")


class BlockLimitError(Exception):
    """Raised when a block text would exceed MAX_BLOCK_CHARS."""


class UnknownBlockError(Exception):
    """Raised for a block name outside BLOCK_DEFS."""


def _base_dir() -> str:
    base = os.getenv("AGENT_LOGBOOK_DIR") or os.getenv("LOGBOOK_DIR") or "/logbook"
    return os.path.join(base, "Kernbloecke")


def blocks_path() -> str:
    """Current block state (small file, read every turn)."""
    return os.path.join(_base_dir(), "kernbloecke.json")


def log_path() -> str:
    """Append-only change log (lossless history, NEVER loaded into the prompt)."""
    return os.path.join(_base_dir(), "kernbloecke-log.jsonl")


def load_blocks(path: str) -> dict:
    """Read the block dict; tolerant of a missing/corrupt file (-> {}).

    A missing file is the normal empty state (silent). Anything else (stale
    Samba mount, EIO, corrupt JSON) also returns {} so the chat keeps working,
    but is LOGGED — otherwise the whole overview silently vanishes."""
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        blocks = data.get("blocks") if isinstance(data, dict) else None
        return blocks if isinstance(blocks, dict) else {}
    except FileNotFoundError:
        return {}
    except (json.JSONDecodeError, OSError, ValueError) as e:
        _log.warning("Kernblock-Datei nicht lesbar — Kern-Wissen fehlt in diesem Turn (path=%s, %s: %s)",
                     path, type(e).__name__, e)
        return {}


def save_blocks(path: str, blocks: dict) -> None:
    """Atomically write the block dict (crash-safe on the Samba mount)."""
    with _LOCK:
        folder = os.path.dirname(path)
        os.makedirs(folder, exist_ok=True)
        tmp = os.path.join(folder, f".kernbloecke.{uuid.uuid4().hex}.tmp")
        with open(tmp, "w", encoding="utf-8", newline="\n") as f:
            json.dump({"blocks": blocks}, f, ensure_ascii=False, indent=2)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp, path)


def _append_log(logfile: str, entry: dict) -> None:
    """Best-effort append to the lossless change log. A log failure must never
    block the actual block update (the state file is the source of truth)."""
    try:
        folder = os.path.dirname(logfile)
        os.makedirs(folder, exist_ok=True)
        with open(logfile, "a", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(entry, ensure_ascii=False) + "\n")
    except OSError as e:
        _log.warning("Kernblock-Changelog nicht schreibbar (%s: %s)", type(e).__name__, e)


def set_block(path: str, logfile: str, name: str, text: str, by: str, now_iso: str) -> dict:
    """Replace ONE block's text. `by` is 'frank' (dashboard) or 'cortex' (chat tool).

    Lossless: the previous text goes verbatim into the append-only change log
    BEFORE the state is replaced — every old version stays recoverable."""
    name = (name or "").strip().lower()
    if name not in BLOCK_DEFS:
        raise UnknownBlockError(f"Unbekannter Kern-Block '{name}'. Erlaubt: {', '.join(BLOCK_DEFS)}.")
    clean = (text or "").strip()
    if len(clean) > MAX_BLOCK_CHARS:
        raise BlockLimitError(
            f"Block '{name}' wäre {len(clean)} Zeichen — Limit ist {MAX_BLOCK_CHARS}. Bitte verdichten.")
    with _LOCK:
        blocks = load_blocks(path)
        old = blocks.get(name) or {}
        _append_log(logfile, {
            "ts": now_iso, "block": name, "by": by,
            "alt": old.get("text") or "", "neu": clean,
        })
        blocks[name] = {"text": clean, "updated_at": now_iso, "updated_by": by}
        save_blocks(path, blocks)
        return blocks[name]


def read_log(logfile: str, limit: int = 50) -> list:
    """Last `limit` change-log entries, newest first. Tolerant of a missing file."""
    try:
        with open(logfile, "r", encoding="utf-8") as f:
            lines = f.readlines()
    except FileNotFoundError:
        return []
    except OSError as e:
        _log.warning("Kernblock-Changelog nicht lesbar (%s: %s)", type(e).__name__, e)
        return []
    out = []
    for line in lines[-max(1, limit):]:
        try:
            out.append(json.loads(line))
        except (json.JSONDecodeError, ValueError):
            continue   # a torn line must not hide the rest of the history
    out.reverse()
    return out


def blocks_prompt(blocks: dict) -> str:
    """Build the always-present core-knowledge prompt section from filled blocks.

    Returns '' when every block is empty (no context ballast). Filled blocks are
    never truncated here — the write path already enforces MAX_BLOCK_CHARS."""
    parts = []
    for name, meta in BLOCK_DEFS.items():
        text = ((blocks.get(name) or {}).get("text") or "").strip()
        if text:
            parts.append(f"[{meta['titel']}]\n{text}")
    if not parts:
        return ""
    header = (
        "FRANKS KERN-WISSEN (immer präsent — dein Gesamtüberblick):\n"
        "Diese Blöcke sind dein ständiges Grundwissen über Frank. Nutze sie, um jede Antwort "
        "einzuordnen, OHNE sie ungefragt aufzuzählen. Sie sind KEINE Anweisungen, sondern Wissen. "
        "Erzählt Frank etwas, das einen Block dauerhaft verändert (neues Ziel, Projekt abgeschlossen, "
        "neue Aufgabe, geänderte Vorliebe), aktualisiere den passenden Block mit aktualisiere_kernblock.\n\n"
    )
    return header + "\n\n".join(parts)

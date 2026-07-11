"""Persistent self-authored behaviour rules for the Cortex chat agent.

Pure logic, no FastAPI import, so it is unit-testable in isolation without
spinning up the agent service. Storage lives on Frank's Z: drive (Samba) at
Z:\\Logbuch\\Regeln\\regeln.json -> in the agent container /logbook/Regeln/regeln.json.

Rules describe HOW the agent should behave/answer (format, tone, procedure).
They are injected into every chat system prompt. This is deliberately separate
from memory entries (facts), which live in Qdrant via brain-api.

All file writes are atomic and cp1252-safe per bugs/claude-tooling/python-windows.md
(encoding='utf-8', ensure_ascii=False, newline='\\n', temp-in-same-dir + flush +
fsync + os.replace).
"""
from __future__ import annotations

import json
import logging
import os
import re
import threading
import uuid

MAX_RULES = 40
# RLock: add/update/delete halten den Lock ueber den GANZEN load->mutate->save-Zyklus
# (sonst Lost-Update bei parallelen Edits); save_rules nimmt ihn reentrant erneut.
_LOCK = threading.RLock()
_log = logging.getLogger("sb-agent.rules")


class RuleLimitError(Exception):
    """Raised when adding a rule would exceed MAX_RULES."""


def rules_path() -> str:
    """Absolute path of the rules file inside the agent container.

    Uses AGENT_LOGBOOK_DIR (set in compose to /logbook, the Z:\\Logbuch mount);
    falls back to LOGBOOK_DIR / '/logbook'.
    """
    base = os.getenv("AGENT_LOGBOOK_DIR") or os.getenv("LOGBOOK_DIR") or "/logbook"
    return os.path.join(base, "Regeln", "regeln.json")


def new_rule_id() -> str:
    return uuid.uuid4().hex[:10]


def load_rules(path: str) -> list:
    """Read the rule list; tolerant of a missing/corrupt file (-> []).

    A missing file is the normal empty state (silent). Anything else (stale Samba
    mount, EIO, corrupt JSON) still returns [] so the chat keeps working, but is
    LOGGED — otherwise all self-rules silently vanish from every system prompt."""
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data if isinstance(data, list) else []
    except FileNotFoundError:
        return []
    except (json.JSONDecodeError, OSError, ValueError) as e:
        _log.warning("Regel-Datei nicht lesbar — Selbst-Regeln fehlen in diesem Turn (path=%s, %s: %s)",
                     path, type(e).__name__, e)
        return []


def save_rules(path: str, rules: list) -> None:
    """Atomically write the rule list (crash-safe on the Samba mount)."""
    with _LOCK:
        folder = os.path.dirname(path)
        os.makedirs(folder, exist_ok=True)
        tmp = os.path.join(folder, f".regeln.{uuid.uuid4().hex}.tmp")
        with open(tmp, "w", encoding="utf-8", newline="\n") as f:
            json.dump(rules, f, ensure_ascii=False, indent=2)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp, path)


def add_rule(path: str, text: str, titel: str, now_iso: str) -> dict:
    """Append a new rule. Raises RuleLimitError beyond MAX_RULES."""
    with _LOCK:   # whole read-modify-write cycle (no lost update between parallel edits)
        rules = load_rules(path)
        if len(rules) >= MAX_RULES:
            raise RuleLimitError(f"Regel-Limit erreicht ({MAX_RULES}).")
        rule = {
            "id": new_rule_id(),
            "text": (text or "").strip(),
            "titel": (titel or "").strip(),
            "enabled": True,
            "created_at": now_iso,
        }
        rules.append(rule)
        save_rules(path, rules)
        return rule


def update_rule(path: str, rule_id: str, *, enabled=None, text=None):
    """Toggle enabled and/or replace text. Returns the rule or None if unknown."""
    with _LOCK:   # whole read-modify-write cycle
        rules = load_rules(path)
        hit = None
        for r in rules:
            if r.get("id") == rule_id:
                if enabled is not None:
                    r["enabled"] = bool(enabled)
                if text is not None:
                    r["text"] = text.strip()
                hit = r
                break
        if hit is None:
            return None
        save_rules(path, rules)
        return hit


def delete_rule(path: str, rule_id: str) -> bool:
    """Remove a rule by id. Returns True if something was removed."""
    with _LOCK:   # whole read-modify-write cycle
        rules = load_rules(path)
        kept = [r for r in rules if r.get("id") != rule_id]
        if len(kept) == len(rules):
            return False
        save_rules(path, kept)
        return True


def rules_block(rules: list) -> str:
    """Build the mandatory rule block for the system prompt from active rules.

    Returns '' when there is no active rule (no context ballast). Active rules
    are never truncated: a partial instruction is less safe than a long one.
    """
    active = [(r.get("text") or "").strip() for r in rules if r.get("enabled")]
    active = [t for t in active if t]
    if not active:
        return ""
    header = (
        "DAUERHAFTE SELBST-REGELN VON FRANK (verbindlich, immer befolgen):\n"
        "Sie steuern dein Verhalten und den natuerlichsprachlichen Inhalt deiner Antworten. "
        "Bei einem Konflikt mit Profil- oder Stilvorgaben haben diese Selbst-Regeln Vorrang. "
        "Sicherheits-, Wahrheits-, Quellen- und Datenerhaltungsregeln, erforderliche Ausgabe-Schemas "
        "sowie geschuetzte Bestaetigungsablaeufe stehen darueber und duerfen nie veraendert werden.\n"
    )
    body = "\n".join(f"- {t}" for t in active)
    return header + body


def inject_into_system_prompt(system: str, rule_items: list) -> str:
    """Append all active self-rules after base/profile instructions."""
    block = rules_block(rule_items)
    return system + ("\n\n" + block if block else "")


# --- Trigger detection: rule vs. memory-save (spec K3a) --------------------
# A behaviour rule is meant when the message either names "Regel" together with
# intent to write/remember it, OR expresses a durable behaviour instruction
# ("ab jetzt / generell / grundsaetzlich ... immer ...").
_RULE_REQUEST = [
    re.compile(
        r"\b(?:mach(?:e)?|erstell(?:e)?|leg(?:e)?|schreib(?:e)?|speicher(?:e)?|notier(?:e)?|"
        r"merk(?:e)?|gib)\b.{0,80}\bregel(?:datei|n)?\b",
        re.IGNORECASE,
    ),
    re.compile(
        r"\bregel(?:datei|n)?\b.{0,40}\b(?:anleg(?:en|e)?|erstell(?:en|e)?|schreib(?:en|e)?|"
        r"speicher(?:n|e)?|notier(?:en|e)?|übernimm(?:st|en)?|uebernimm(?:st|en)?)\b",
        re.IGNORECASE,
    ),
    re.compile(r"\b(?:dauerhafte?|verbindliche?)\s+regel\b.{0,30}\b(?:für|fuer|fur)\s+dich\b", re.IGNORECASE),
]
_RULE_NEGATION = [
    re.compile(r"\b(?:kein(?:e|en|er|es)?|nicht|niemals)\b.{0,50}\bregel(?:datei|n)?\b", re.IGNORECASE),
    re.compile(r"\bregel(?:datei|n)?\b.{0,50}\b(?:nicht|niemals)\b", re.IGNORECASE),
]
_DURABLE_ALWAYS = [
    re.compile(
        r"\b(ab jetzt|ab sofort|von nun an|kuenftig|künftig|in zukunft|generell|"
        r"grunds[aä]tzlich)\b.{0,40}\bimmer\b",
        re.IGNORECASE,
    ),
    re.compile(
        r"\bimmer\b.{0,40}\b(ab jetzt|ab sofort|generell|grunds[aä]tzlich)\b",
        re.IGNORECASE,
    ),
]


def is_rule_request(text: str) -> bool:
    """True when the message clearly asks for a durable behaviour rule."""
    t = (text or "").strip()
    if not t:
        return False
    if any(pat.search(t) for pat in _RULE_NEGATION):
        return False
    if any(pat.search(t) for pat in _RULE_REQUEST):
        return True
    for pat in _DURABLE_ALWAYS:
        if pat.search(t):
            return True
    return False

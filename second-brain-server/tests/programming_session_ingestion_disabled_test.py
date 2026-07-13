#!/usr/bin/env python3
"""Regressionstest: CLIs koennen keine Programmier-Sessions ins Gehirn schreiben."""
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
AGENT = (ROOT / "second-brain-server" / "agent" / "app.py").read_text(encoding="utf-8")
BRAIN_API = (ROOT / "second-brain-server" / "brain-api" / "app.py").read_text(encoding="utf-8")
OPEN_CODE_RULES = (ROOT / "opencode-setup" / "AGENTS-global.md").read_text(encoding="utf-8")
CLAUDE_SETTINGS = (ROOT / "claude-code-setup" / "settings-reference.json").read_text(encoding="utf-8")
CLAUDE_RULE = (ROOT / "claude-code-setup" / "rules" / "second-brain.md").read_text(encoding="utf-8")


def main() -> int:
    forbidden = {
        "Server-Endpunkt": '@app.post("/session-log"',
        "Server-Verarbeitung": "_session_log_process",
        "Claude-Code-Hook": "session-brain-summary",
        "OpenCode-Speicherregel": "Session-Ende ins Gehirn",
    }
    haystacks = {
        "Server-Endpunkt": AGENT,
        "Server-Verarbeitung": AGENT,
        "Claude-Code-Hook": CLAUDE_SETTINGS + CLAUDE_RULE,
        "OpenCode-Speicherregel": OPEN_CODE_RULES,
    }
    for label, needle in forbidden.items():
        if needle in haystacks[label]:
            raise AssertionError(f"{label} ist weiterhin aktiv: {needle}")

    if "_is_automatic_programming_session(req)" not in BRAIN_API:
        raise AssertionError("Die serverseitige Sperre fuer direkte MCP-/API-Schreibversuche fehlt")
    if "Automatische Programmier-Session-Protokolle sind deaktiviert" not in BRAIN_API:
        raise AssertionError("Die serverseitige Sperre lehnt nicht eindeutig ab")

    for relative in (
        "claude-code-setup/hooks/session-brain-summary.py",
        "claude-code-setup/hooks/session-brain-summary.ps1",
        "claude-code-setup/hooks/session-brain-summary.sh",
        "Umgebung/Hooks/session-brain-summary.py",
        "Umgebung/Hooks/session-brain-summary.ps1",
        "Umgebung/Hooks/session-brain-summary.sh",
    ):
        if (ROOT / relative).exists():
            raise AssertionError(f"Session-Speicher-Hook existiert weiterhin: {relative}")

    print("PASS: Claude Code, OpenCode und Server erzeugen keine Programmier-Session-Eintraege")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

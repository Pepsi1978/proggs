#!/usr/bin/env python3
"""Regression checks: visible reply and TTS must never consume pre-final source text."""
from __future__ import annotations

import ast
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "agent" / "app.py"
SOURCE = APP_PATH.read_text(encoding="utf-8")


def load_sanitizers() -> dict:
    assignments = {
        "_MD_BOLD_RE", "_MD_HEADING_RE", "_MD_BULLET_RE", "_MD_WEB_LINK_RE",
        "_BARE_WEB_URL_RE", "_TRAILING_SOURCES_RE", "_TRAILING_MARKDOWN_LINKS_RE",
        "_SOURCE_ATTRIBUTION_TAIL_RE", "_PARENTHETICAL_SOURCE_RE", "_NUMERIC_CITATION_RE",
        "_FREEFORM_ACTIONS",
    }
    functions = {
        "_strip_markdown_tts", "_remove_bare_web_url", "_sanitize_visible_reply",
        "_finalize_visible_outcome",
    }
    nodes = []
    for node in ast.parse(SOURCE).body:
        if isinstance(node, ast.Assign) and any(
            isinstance(target, ast.Name) and target.id in assignments for target in node.targets
        ):
            nodes.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name in functions:
            nodes.append(node)
    namespace = {"re": re}
    exec(compile(ast.Module(body=nodes, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def main() -> int:
    ns = load_sanitizers()
    sanitize = ns["_sanitize_visible_reply"]
    finalize = ns["_finalize_visible_outcome"]
    draft = (
        "Archer wird später mehrfach erwähnt [1]. Mehr: https://example.com/a/b?x=1.\n\n"
        "Quellen:\n- [Memory Alpha](https://memory-alpha.example/archer)\n- www.example.org/source"
    )
    assert sanitize(draft) == "Archer wird später mehrfach erwähnt. Mehr:."
    outcome = finalize({"action": "recall", "reply": draft, "sources": [{"title": "Memory Alpha"}]})
    assert "http" not in outcome["reply"].lower() and "quellen" not in outcome["reply"].lower()
    assert outcome["sources"] == [{"title": "Memory Alpha"}], "structured metadata must remain available"
    links_only = (
        "Die Pflege weniger Hinweise ist entscheidender als ihre bloße Menge.\n\n"
        "[OpenAI Codex](https://openai.com/codex)\n"
        "[Terminal Bench](https://terminal-bench.example/results)"
    )
    assert sanitize(links_only) == "Die Pflege weniger Hinweise ist entscheidender als ihre bloße Menge."
    natural_attributions = (
        "OpenAI nennt ungefähr hundert Zeilen als grobe Obergrenze. Quelle OpenAI Harness Engineering.\n\n"
        "Die Schlussfolgerung ist, Kontextdateien minimal zu halten. Quelle arXiv Studie 2602.11988.\n\n"
        "Ein kurzer Hinweis kann teure Irrwege verhindern. Quelle: Anthropic Dokumentation.\n\n"
        "CLAUDE.md kann AGENTS.md importieren (Quelle Claude Code Memory Dokumentation)."
    )
    assert sanitize(natural_attributions) == (
        "OpenAI nennt ungefähr hundert Zeilen als grobe Obergrenze.\n\n"
        "Die Schlussfolgerung ist, Kontextdateien minimal zu halten.\n\n"
        "Ein kurzer Hinweis kann teure Irrwege verhindern.\n\n"
        "CLAUDE.md kann AGENTS.md importieren."
    )
    assert sanitize("Die Quelle ist für die Bewertung wichtig.") == "Die Quelle ist für die Bewertung wichtig."

    stream = SOURCE[SOURCE.index("async def chat_stream"):SOURCE.index("# Gruppe D", SOURCE.index("async def chat_stream"))]
    assert "rsize, None, req.memory_edit" in stream, "raw model deltas still enter the client stream"
    assert "queue.put_nowait(final_reply)" in stream, "final canonical reply is not streamed"
    assert "outcome = _finalize_visible_outcome(outcome)" in stream, "stream reply is not sanitized"
    assert '"canonical_reply": True' in stream, "server does not attest canonical SSE text"

    print("PASS: display, persistence and TTS share one final source-free reply")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

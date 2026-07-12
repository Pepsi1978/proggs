#!/usr/bin/env python3
"""Regressionstest: Chatverläufe bleiben im Logbuch und gelangen nie nach Qdrant."""
from __future__ import annotations

import ast
from pathlib import Path


APP_PATH = Path(__file__).resolve().parents[1] / "agent" / "app.py"
SOURCE = APP_PATH.read_text(encoding="utf-8")


def function_source(name: str) -> str:
    node = next(
        item for item in ast.parse(SOURCE).body
        if isinstance(item, (ast.FunctionDef, ast.AsyncFunctionDef)) and item.name == name
    )
    return ast.get_source_segment(SOURCE, node) or ""


def main() -> int:
    flush = function_source("flush_session_to_logbook")
    chat = function_source("chat")
    stream = function_source("chat_stream")

    forbidden = (
        "_schedule_conversation_mirror",
        "_mirror_session_to_brain_now",
        "conversation_mirror_seq",
    )
    if any(name in SOURCE for name in forbidden):
        raise AssertionError("Ein alter Qdrant-Gesprächsmirror ist weiterhin verdrahtet")
    if "brain_store" in flush or "CONV_CATEGORY" in flush:
        raise AssertionError("Timeout-Flush schreibt Gespräche weiterhin nach Qdrant")
    if "path.write_text(content, encoding=\"utf-8\")" not in flush:
        raise AssertionError("Logbuch-.txt wurde versehentlich entfernt")
    if "_write_session_snapshot" not in chat or "_write_session_snapshot" not in stream:
        raise AssertionError("Crash- und Neustart-sichere Session-Persistenz fehlt")

    print("PASS: Gespräche werden nur im Logbuch gespeichert, nie in Qdrant")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Offline regression checks for per-answer memory/web source reporting."""
from __future__ import annotations

import ast
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "agent" / "app.py"
SOURCE = APP_PATH.read_text(encoding="utf-8")


def load_helper():
    tree = ast.parse(SOURCE)
    node = next(
        item for item in tree.body
        if isinstance(item, ast.FunctionDef) and item.name == "_source_usage_from_trace"
    )
    namespace: dict = {}
    exec(compile(ast.Module(body=[node], type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace["_source_usage_from_trace"]


def trace(*events: tuple[str, bool]) -> dict:
    return {"events": [{"step": step, "ok": ok} for step, ok in events]}


def main() -> int:
    source_usage = load_helper()
    assert source_usage(trace(("recall_search", True))) == ["memory"]
    assert source_usage(trace(("internet", True))) == ["internet"]
    assert source_usage(trace(("native_web", True), ("recall_search", True))) == ["memory", "internet"]
    assert source_usage(trace(("auto_parallel_memory", False), ("auto_parallel_web", False))) == []
    assert source_usage(trace(("category_count", True))) == ["memory"]
    assert source_usage(None) == []
    assert SOURCE.count('"source_usage": _source_usage_from_trace(tr)') == 2
    print("PASS: /chat and /chat/stream report memory/web usage from successful turn steps")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

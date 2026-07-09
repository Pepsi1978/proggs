#!/usr/bin/env python3
"""Regressionstest fuer die Trefferanzeige des Auto-Parallelprofils."""
from __future__ import annotations

import ast
import sys
from pathlib import Path


def main() -> int:
    app_path = Path(__file__).resolve().parent.parent / "agent" / "app.py"
    tree = ast.parse(app_path.read_text(encoding="utf-8"))
    function = next(
        node
        for node in tree.body
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
        and node.name == "_auto_parallel_answer"
    )
    final_statement = function.body[-1]
    final_return = final_statement.value if isinstance(final_statement, ast.Return) else None
    if not isinstance(final_return, ast.Dict):
        print("FAIL: letzter Rueckgabewert ist kein Dictionary")
        return 1

    values = {
        key.value: value
        for key, value in zip(final_return.keys, final_return.values)
        if isinstance(key, ast.Constant) and isinstance(key.value, str)
    }
    recall_hits = values.get("recall_hits")
    expected = (
        isinstance(recall_hits, ast.Call)
        and isinstance(recall_hits.func, ast.Name)
        and recall_hits.func.id == "len"
        and len(recall_hits.args) == 1
        and isinstance(recall_hits.args[0], ast.Name)
        and recall_hits.args[0].id == "hits"
    )
    if not expected:
        print("FAIL: recall_hits muss die gefundenen hits zaehlen, nicht die Quellen-Chips")
        return 1

    source = ast.get_source_segment(app_path.read_text(encoding="utf-8"), function) or ""
    required_guards = {
        "Antwortkontext bleibt auf 12 Schnipsel begrenzt": "for h in hits[:12]",
        "Quellen-Chips bleiben auf 8 begrenzt": "for h in hits[:8]",
        "Telemetry trennt Antwort-Schnipsel": "answer_snippets=len(mem_snippets)",
        "Telemetry trennt Quellen-Chips": "source_chips=len(sources)",
    }
    missing = [label for label, needle in required_guards.items() if needle not in source]
    if missing:
        print("FAIL: " + "; ".join(missing))
        return 1

    print("PASS: Suchtreffer, Antwort-Schnipsel und Quellen-Chips bleiben getrennt")
    return 0


if __name__ == "__main__":
    sys.exit(main())

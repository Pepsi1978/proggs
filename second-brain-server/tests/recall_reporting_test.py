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
        "Antwortkontext nutzt das konfigurierbare Schnipsel-Limit": "for h in hits[:ANSWER_SNIPPET_LIMIT]",
        "Quellen-Chips stammen nur aus dem tatsächlichen Antwortkontext": "for h in mem_snippets[:SOURCE_CHIP_LIMIT]",
        "Antwortkontext wird vor der Übergabe vollständig serialisiert": "f\"{memory_context}\\n\\n\"",
        "Gesamtbudget entfernt vollständige überzählige Schnipsel": "while len(memory_context) > ANSWER_TOTAL_CHARS and mem_snippets",
        "Telemetry trennt Antwort-Schnipsel": "answer_snippets=len(mem_snippets)",
        "Telemetry trennt Quellen-Chips": "source_chips=len(sources)",
    }
    missing = [label for label, needle in required_guards.items() if needle not in source]
    if missing:
        print("FAIL: " + "; ".join(missing))
        return 1

    app_source = app_path.read_text(encoding="utf-8")
    required_limits = {
        "Default für Antwort-Schnipsel": '"answer_snippet_limit": _env_int("AGENT_ANSWER_SNIPPET_LIMIT", 12)',
        "Default für Quellen-Chips": '"source_chip_limit": _env_int("AGENT_SOURCE_CHIP_LIMIT", 8)',
        "Grenzen für Antwort-Schnipsel": '"answer_snippet_limit": (1, 500)',
        "Grenzen für Quellen-Chips": '"source_chip_limit": (1, 500)',
        "Antwort-Schnipsel werden von der API gemeldet": '"answer_snippet_limit": ANSWER_SNIPPET_LIMIT',
        "Quellen-Chips werden von der API gemeldet": '"source_chip_limit": SOURCE_CHIP_LIMIT',
        "Antwort-Schnipsel werden sofort angewendet": 'ANSWER_SNIPPET_LIMIT = clean["answer_snippet_limit"]',
        "Quellen-Chips werden sofort angewendet": 'SOURCE_CHIP_LIMIT = clean["source_chip_limit"]',
    }
    missing_limits = [label for label, needle in required_limits.items() if needle not in app_source]
    if missing_limits:
        print("FAIL: " + "; ".join(missing_limits))
        return 1

    repo_root = app_path.parent.parent.parent
    dashboard_source = (repo_root / "second-brain-server" / "dashboard" / "static" / "index.html").read_text(encoding="utf-8")
    android_model = (repo_root / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "data" / "model" / "Models.kt").read_text(encoding="utf-8")
    android_settings = (repo_root / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "ui" / "settings" / "SettingsScreen.kt").read_text(encoding="utf-8")
    for key in ("answer_snippet_limit", "source_chip_limit"):
        if key not in dashboard_source or key not in android_model or android_settings.count(key) < 3:
            print(f"FAIL: {key} ist nicht in Server-, Dashboard- und Android-Konfiguration verdrahtet")
            return 1

    print("PASS: Suchtreffer, konfigurierbare Antwort-Schnipsel und Quellen-Chips bleiben getrennt")
    return 0


if __name__ == "__main__":
    sys.exit(main())

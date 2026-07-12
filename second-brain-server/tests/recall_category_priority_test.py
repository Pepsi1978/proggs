#!/usr/bin/env python3
"""Regressionstest: explizit genannte Kategorien schlagen ähnliche Gesprächsprotokolle."""
from __future__ import annotations

import ast
import re
from pathlib import Path
from typing import Callable, cast


APP_PATH = Path(__file__).resolve().parents[1] / "agent" / "app.py"
SOURCE = APP_PATH.read_text(encoding="utf-8")


def main() -> int:
    node = next(
        item for item in ast.parse(SOURCE).body
        if isinstance(item, ast.FunctionDef) and item.name == "_prioritize_named_categories"
    )
    namespace: dict[str, object] = {"re": re}
    exec(compile(ast.Module(body=[node], type_ignores=[]), str(APP_PATH), "exec"), namespace)
    prioritize = cast(Callable[[str, list[dict]], list[dict]], namespace["_prioritize_named_categories"])

    conversation = {"doc_id": "conversation", "category": "Gespräche", "score": 0.78}
    vehicle = {"doc_id": "vehicle", "category": "Auto", "score": 0.64}
    other = {"doc_id": "other", "category": "Geräte", "score": 0.70}
    hits = [conversation, other, vehicle]

    result = prioritize("Welche Typenbezeichnung hat mein Auto?", hits)
    if [hit["doc_id"] for hit in result] != ["vehicle", "conversation", "other"]:
        raise AssertionError("Genannte Kategorie Auto wurde nicht verlustfrei vorgezogen")
    if prioritize("Was habe ich gespeichert?", hits) != hits:
        raise AssertionError("Ohne genannten Kategorienamen darf die Rangfolge nicht verändert werden")

    print("PASS: Genannte Kategorien werden vor ähnlichen Gesprächsprotokollen priorisiert")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

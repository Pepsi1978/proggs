#!/usr/bin/env python3
"""Offline-Regressionstest fuer additive Bibliothekar-Kategorie-Vorschlaege."""
from __future__ import annotations

import ast
import sys
from pathlib import Path


for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:
        pass


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "librarian" / "app.py"
APP_SOURCE = APP_PATH.read_text(encoding="utf-8")


def load_functions() -> dict:
    tree = ast.parse(APP_SOURCE)
    wanted = {"brain_add_category", "_execute_action"}
    selected: list[ast.stmt] = [node for node in tree.body
                                if isinstance(node, ast.FunctionDef) and node.name in wanted]
    namespace: dict = {}
    exec(compile(ast.Module(body=selected, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    ns = load_functions()
    written: list[tuple[str, list[str]]] = []
    ns["brain_list"] = lambda: [{"doc_id": "doc-1", "category": "A"}]
    ns["brain_by_category"] = lambda category: [{
        "doc_id": "doc-1", "category": "A", "categories": ["A", "B", "C", "D"]
    }]
    ns["brain_set_categories"] = lambda doc_id, categories: (
        written.append((doc_id, list(categories))) or
        {"ok": True, "doc_id": doc_id, "categories": list(categories)}
    )

    result = ns["brain_add_category"]("doc-1", "E")
    check(result["categories"] == ["A", "B", "C", "D", "E"],
          "vier bestehende Kategorien plus Vorschlag ergeben fuenf")
    check(written[-1] == ("doc-1", ["A", "B", "C", "D", "E"]),
          "der Multi-Category-Endpunkt erhaelt die vollstaendige Union")

    ns["brain_add_category"]("doc-1", "b")
    check(written[-1][1] == ["A", "B", "C", "D"],
          "Gross-/Kleinschreibung erzeugt keine doppelte Kategorie")

    ns["brain_list"] = lambda: []
    before = len(written)
    try:
        ns["brain_add_category"]("doc-1", "E")
    except RuntimeError:
        pass
    else:
        raise AssertionError("fehlender Bestand muss den Schreibvorgang abbrechen")
    check(len(written) == before, "ohne gelesenen Bestand wird nichts ueberschrieben")

    ns["_action_targets_phone_only"] = lambda action, item: False
    ns["brain_add_category"] = lambda doc_id, category: {
        "categories": ["A", "B", "C", "D", category]
    }
    message = ns["_execute_action"](
        {"typ": "kategorie", "doc_id": "doc-1", "kategorie": "E"}, {}
    )
    check("hinzugefügt" in message and "5 Kategorien" in message,
          "Bibliothekar-Aktion meldet die additive Zuordnung korrekt")

    print("PASS: Bibliothekar ergaenzt Kategorien verlustfrei")
    return 0


if __name__ == "__main__":
    sys.exit(main())

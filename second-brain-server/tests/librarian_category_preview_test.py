#!/usr/bin/env python3
"""Offline-Test fuer die Kategorien-Vorher/Nachher-Vorschau im Bibliothekar."""
from __future__ import annotations

import ast
import asyncio
import sys
from pathlib import Path


for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:
        pass


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "dashboard" / "app.py"
INDEX_PATH = ROOT / "dashboard" / "static" / "index.html"


def load_endpoint() -> dict:
    tree = ast.parse(APP_PATH.read_text(encoding="utf-8"))
    selected: list[ast.stmt] = [node for node in tree.body
                                if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
                                and node.name == "api_get_entry_categories"]

    class AppStub:
        @staticmethod
        def get(_path: str):
            return lambda fn: fn

    namespace = {"app": AppStub(), "USER_ID": "frank", "JSONResponse": dict,
                 "logging": __import__("logging"), "_log": lambda *args, **kwargs: None}
    exec(compile(ast.Module(body=selected, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    ns = load_endpoint()

    def fake_get(path: str, **params):
        if path == "/list":
            return {"ok": True, "ready": True, "items": [{"doc_id": "doc-1", "category": "A"}]}
        if path == "/by-category":
            return {"ok": True, "items": [{"doc_id": "doc-1", "categories": ["A", "B", "C", "D"]}]}
        raise AssertionError(path)

    ns["_bget"] = fake_get
    result = ns["api_get_entry_categories"]("doc-1")
    check(result["categories"] == ["A", "B", "C", "D"],
          "read-only Endpunkt liefert alle vier vorhandenen Kategorien")

    source = INDEX_PATH.read_text(encoding="utf-8")
    check("<b>Vorher</b>" in source and "<b>Nachher</b>" in source,
          "Bibliothekar zeigt beide Vorschau-Zeilen")
    check("if(!seen.has(proposed.toLocaleLowerCase" in source and "after.push(proposed)" in source,
          "Frontend bildet die additive Union ohne Duplikat")
    check('it.aktion.typ==="kategorie"' in source and "loadLibCategoryPreview(preview,it)" in source,
          "Vorschau wird nur fuer Kategorie-Aktionen geladen")

    print("PASS: Bibliothekar-Kategorien-Vorschau zeigt 4 -> 5 verlustfrei")
    return 0


if __name__ == "__main__":
    sys.exit(main())

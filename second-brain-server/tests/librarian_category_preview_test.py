#!/usr/bin/env python3
"""Offline-Test fuer die Kategorien-Vorher/Nachher-Vorschau im Bibliothekar."""
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

    calls: list[tuple[str, dict]] = []

    def fake_get(path: str, **params):
        calls.append((path, params))
        if path != "/entry/categories":
            raise AssertionError(path)
        return {"ok": True, "doc_id": "doc-1", "categories": ["A", "B", "C", "D"]}

    ns["_bget"] = fake_get
    result = ns["api_get_entry_categories"]("doc-1")
    check(result["categories"] == ["A", "B", "C", "D"],
          "read-only Endpunkt liefert alle vier vorhandenen Kategorien")
    check(calls == [("/entry/categories", {"doc_id": "doc-1", "user_id": "frank"})],
          "Dashboard nutzt genau einen gezielten brain-api-Abruf ohne Bestandsscan")

    brain_source = (ROOT / "brain-api" / "app.py").read_text(encoding="utf-8")
    check('@app.get("/entry/categories"' in brain_source and "def get_entry_categories" in brain_source,
          "brain-api stellt den gezielten read-only Endpunkt bereit")
    check('limit=1, with_payload=["doc_id", "category", "categories", "parent", "parents"]' in brain_source,
          "brain-api liest genau einen Chunk und keine Volltexte")
    check('checkpoint("get_entry_categories"' in brain_source
          and 'ms=int((time.perf_counter() - t0) * 1000)' in brain_source,
          "brain-api protokolliert die gesamte Endpoint-Antwortzeit dauerhaft in Millisekunden")

    source = INDEX_PATH.read_text(encoding="utf-8")
    check("<b>Vorher</b>" in source and "<b>Nachher</b>" in source,
          "Bibliothekar zeigt beide Vorschau-Zeilen")
    check("alreadyPresent=seen.has(proposed.toLocaleLowerCase" in source and "after.push(proposed)" in source,
          "Frontend bildet die additive Union ohne Duplikat")
    check("Dieser alte Vorschlag kann verworfen werden" in source
          and 'data-lib-role="accept"' in source and "accept.disabled=true" in source,
          "bereits gespeicherte No-op-Vorschlaege werden erklaert und sind nicht ausfuehrbar")
    check('it.aktion.typ==="kategorie"' in source and "loadLibCategoryPreview(preview,it)" in source,
          "Vorschau wird nur fuer Kategorie-Aktionen geladen")

    print("PASS: Bibliothekar-Kategorien-Vorschau nutzt gezielten doc_id-Abruf und zeigt 4 -> 5")
    return 0


if __name__ == "__main__":
    sys.exit(main())

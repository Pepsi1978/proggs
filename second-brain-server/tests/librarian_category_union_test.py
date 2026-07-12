#!/usr/bin/env python3
"""Offline-Regressionstest fuer additive Bibliothekar-Kategorie-Vorschlaege."""
from __future__ import annotations

import ast
import copy
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from types import SimpleNamespace


for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:
        pass


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "librarian" / "app.py"
APP_SOURCE = APP_PATH.read_text(encoding="utf-8")
BRAIN_PATH = ROOT / "brain-api" / "app.py"
BRAIN_SOURCE = BRAIN_PATH.read_text(encoding="utf-8")


def load_functions() -> dict:
    tree = ast.parse(APP_SOURCE)
    wanted = {"brain_add_category", "_execute_action", "_custom_entry_block",
              "_custom_action_needs_complete_source", "_category_action_is_noop"}
    selected: list[ast.stmt] = [node for node in tree.body
                                if isinstance(node, ast.FunctionDef) and node.name in wanted]
    namespace: dict = {}
    exec(compile(ast.Module(body=selected, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load_atomic_endpoint() -> dict:
    tree = ast.parse(BRAIN_SOURCE)
    selected: list[ast.stmt] = []
    for node in tree.body:
        if isinstance(node, ast.Assign) and any(
            isinstance(target, ast.Name) and target.id in {"_ENTRY_WRITE_LOCK", "ENTRY_WRITE_LOCK_TIMEOUT_S"}
            for target in node.targets
        ):
            selected.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name == "serialized_entry_write":
            selected.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name == "add_entry_category":
            endpoint = copy.deepcopy(node)
            endpoint.decorator_list = [ast.Name(id="serialized_entry_write", ctx=ast.Load())]
            ast.fix_missing_locations(endpoint)
            selected.append(endpoint)

    class HttpError(Exception):
        def __init__(self, **kwargs):
            super().__init__(kwargs.get("detail"))

    namespace = {
        "threading": threading,
        "wraps": __import__("functools").wraps,
        "HTTPException": HttpError,
        "AddEntryCategoryReq": object,
    }
    exec(compile(ast.Module(body=selected, type_ignores=[]), str(BRAIN_PATH), "exec"), namespace)
    return namespace


def main() -> int:
    ns = load_functions()
    calls: list[tuple[str, dict]] = []

    class Response:
        def __init__(self, data: dict):
            self.data = data

        def raise_for_status(self):
            return None

        def json(self):
            return self.data

    class Http:
        next_response = {"ok": True, "doc_id": "doc-1", "categories": ["A", "B", "C", "D", "E"], "added": True}

        def post(self, url: str, **kwargs):
            calls.append((url, kwargs["json"]))
            return Response(self.next_response)

    ns.update({"_HTTP": Http(), "BRAIN_URL": "http://brain", "USER_ID": "frank", "HEADERS": {}})

    result = ns["brain_add_category"]("doc-1", "E")
    check(result["categories"] == ["A", "B", "C", "D", "E"],
          "vier bestehende Kategorien plus Vorschlag ergeben fuenf")
    check(calls == [("http://brain/entry/categories/add",
                     {"doc_id": "doc-1", "category": "E", "user_id": "frank"})],
          "Bibliothekar delegiert die Union in einem atomaren Serveraufruf")

    brain_source = (ROOT / "brain-api" / "app.py").read_text(encoding="utf-8")
    check('@app.post("/entry/categories/add"' in brain_source
          and "def add_entry_category" in brain_source
          and "@serialized_entry_write\ndef add_entry_category" in brain_source,
          "brain-api serialisiert Lesen und Ergaenzen unter demselben Schreib-Lock")
    check("current + [proposed]" in brain_source and 'result["added"] = True' in brain_source,
          "atomarer Endpoint erhaelt den aktuellen Bestand und meldet echte Aenderungen")

    atomic = load_atomic_endpoint()
    state = {"categories": ["A"]}
    atomic.update({
        "_require_store": lambda: None,
        "Filter": lambda **kwargs: kwargs,
        "FieldCondition": lambda **kwargs: kwargs,
        "MatchValue": lambda **kwargs: kwargs,
        "_scroll": lambda *args, **kwargs: [SimpleNamespace(payload={"categories": list(state["categories"])})],
        "cats_from_payload": lambda payload: list(payload.get("categories") or []),
        "canonical_category": lambda value: value.strip(),
        "category_key": lambda value: value.strip().casefold(),
        "EntryCategoriesReq": lambda **kwargs: SimpleNamespace(**kwargs),
    })

    def set_categories(req):
        time.sleep(0.02)
        state["categories"] = list(req.categories)
        return {"ok": True, "doc_id": req.doc_id, "categories": list(req.categories)}

    atomic["set_entry_categories"] = set_categories
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(
            atomic["add_entry_category"],
            [SimpleNamespace(doc_id="doc-1", category="B", user_id="frank"),
             SimpleNamespace(doc_id="doc-1", category="C", user_id="frank")],
        ))
    check(state["categories"] == ["A", "B", "C"],
          "zwei parallele Ergaenzungen bleiben durch den Schreib-Lock beide erhalten")
    check(all(result["added"] is True for result in results),
          "beide parallelen neuen Kategorien werden als hinzugefuegt gemeldet")

    ns["_action_targets_phone_only"] = lambda action, item: False
    ns["brain_add_category"] = lambda doc_id, category: {
        "categories": ["A", "B", "C", "D", category]
    }
    message = ns["_execute_action"](
        {"typ": "kategorie", "doc_id": "doc-1", "kategorie": "E"}, {}
    )
    check("hinzugefügt" in message and "5 Kategorien" in message,
          "Bibliothekar-Aktion meldet die additive Zuordnung korrekt")

    block, truncated = ns["_custom_entry_block"](
        "doc-2", {"title": "Langer Eintrag", "category": "Drohnen",
                  "categories": ["Drohnen", "Geräte", "Interessen", "Hobbys"],
                  "text": "x" * 30}, 10
    )
    check(truncated is True and "GEKUERZTER AUSZUG" in block,
          "eigene Aufgaben markieren gekuerzte Eintragstexte sichtbar")
    check("Kategorien: Drohnen, Geräte, Interessen, Hobbys" in block,
          "eigene Aufgaben erhalten alle vorhandenen Kategorien als Modellkontext")
    entries = {"doc-2": {"category": "Drohnen",
                          "categories": ["Drohnen", "Geräte", "Interessen", "Hobbys"]}}
    check(ns["_category_action_is_noop"](
        {"typ": "kategorie", "doc_id": "doc-2", "kategorie": "geräte"}, entries
    ) is True, "bereits vorhandene Kategorie wird deterministisch als No-op erkannt")
    check(ns["_category_action_is_noop"](
        {"typ": "kategorie", "doc_id": "doc-2", "kategorie": "Technik"}, entries
    ) is False, "wirklich neue Kategorie bleibt als Vorschlag erlaubt")
    check(ns["_custom_action_needs_complete_source"](
        {"typ": "update", "doc_id": "doc-2", "text": "unvollstaendig"}, {"doc-2"}
    ) is True, "Update auf gekuerztem Zieltext wird blockiert")
    check(ns["_custom_action_needs_complete_source"](
        {"typ": "merge", "doc_ids": ["doc-1", "doc-2"], "text": "unvollstaendig"}, {"doc-2"}
    ) is True, "Merge mit gekuerztem Zieltext wird blockiert")
    check(ns["_custom_action_needs_complete_source"](
        {"typ": "hinweis"}, {"doc-2"}
    ) is False, "Hinweise bleiben trotz gekuerzter Quelle erlaubt")

    print("PASS: Bibliothekar ergaenzt Kategorien verlustfrei")
    return 0


if __name__ == "__main__":
    sys.exit(main())

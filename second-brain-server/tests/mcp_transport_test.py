#!/usr/bin/env python3
"""Regressionstest fuer den stateless JSON-Transport grosser MCP-Antworten."""
from __future__ import annotations

import ast
import sys
from pathlib import Path


def main() -> int:
    server_path = Path(__file__).resolve().parent.parent / "mcp-server" / "server.py"
    source = server_path.read_text(encoding="utf-8")
    tree = ast.parse(source)

    constructors = [
        node for node in ast.walk(tree)
        if isinstance(node, ast.Call)
        and isinstance(node.func, ast.Name)
        and node.func.id == "FastMCP"
    ]
    if not constructors:
        print("FAIL: Kein FastMCP-Konstruktor gefunden")
        return 1

    for call in constructors:
        kwargs = {kw.arg: kw.value for kw in call.keywords if kw.arg}
        for name in ("stateless_http", "json_response"):
            value = kwargs.get(name)
            if not isinstance(value, ast.Constant) or value.value is not True:
                print(f"FAIL: Jeder FastMCP-Konstruktor braucht {name}=True")
                return 1

    function = next(
        node for node in tree.body
        if isinstance(node, ast.FunctionDef) and node.name == "get_by_title"
    )
    function_source = ast.get_source_segment(source, function) or ""
    required = ('"get_by_title ok"', "time.monotonic()", "chars=len(text)", 'ms=int(')
    missing = [item for item in required if item not in function_source]
    if missing:
        print("FAIL: get_by_title-Laufzeitsonde unvollstaendig: " + ", ".join(missing))
        return 1

    category_function = next(
        node for node in tree.body
        if isinstance(node, ast.FunctionDef) and node.name == "get_entry_categories"
    )
    category_source = ast.get_source_segment(source, category_function) or ""
    required_category = ('_get("/entry/categories"', '"doc_id": doc_id',
                         '"get_entry_categories ok"', 'ms=int(')
    missing_category = [item for item in required_category if item not in category_source]
    if missing_category:
        print("FAIL: get_entry_categories-Werkzeug unvollstaendig: " + ", ".join(missing_category))
        return 1

    guard = next(
        node for node in tree.body
        if isinstance(node, ast.FunctionDef) and node.name == "_is_bugfix_category"
    )
    namespace: dict = {}
    exec(compile(ast.Module(body=[guard], type_ignores=[]), str(server_path), "exec"), namespace)
    is_blocked = namespace["_is_bugfix_category"]
    blocked = ("bugfixes", "Bugfixes/OpenCode", " bugfixes / android ", r"bugfixes\server")
    allowed = ("bug-cases", "Programmierung/Almanache", "", "bugfix")
    if not all(is_blocked(value) for value in blocked) or any(is_blocked(value) for value in allowed):
        print("FAIL: Bugfix-Kategorie-Sperre ist zu weit oder umgehbar")
        return 1

    remember_function = next(
        node for node in tree.body
        if isinstance(node, ast.FunctionDef) and node.name == "remember"
    )
    remember_source = ast.get_source_segment(source, remember_function) or ""
    if "_is_bugfix_category(category)" not in remember_source or remember_source.index("_is_bugfix_category") > remember_source.index("_post"):
        print("FAIL: remember muss bugfixes vor dem brain-api-Aufruf blockieren")
        return 1

    print("PASS: MCP nutzt stateless JSON, misst Volltextabrufe, liest Kategorien gezielt und blockiert bugfixes")
    return 0


if __name__ == "__main__":
    sys.exit(main())

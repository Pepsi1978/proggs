#!/usr/bin/env python3
"""Offline-Regressionstest fuer das Hauptagenten-Werkzeug zum Kategorienabruf."""
from __future__ import annotations

import ast
import sys
from pathlib import Path


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    app_path = Path(__file__).resolve().parents[1] / "agent" / "app.py"
    source = app_path.read_text(encoding="utf-8")
    tree = ast.parse(source)

    helper = next(node for node in tree.body
                  if isinstance(node, ast.FunctionDef) and node.name == "brain_entry_categories")
    helper_source = ast.get_source_segment(source, helper) or ""
    check('f"{BRAIN_URL}/entry/categories"' in helper_source,
          "Helfer muss den direkten brain-api-Endpunkt verwenden")
    check('"doc_id": doc_id' in helper_source and '"user_id": user_id' in helper_source,
          "Helfer muss exakt nach doc_id und Nutzer lesen")
    check('brain_http_entry_categories' in helper_source,
          "Gezielter Abruf braucht eine eigene Performance-Span")

    builder = next(node for node in tree.body
                   if isinstance(node, ast.FunctionDef) and node.name == "build_agent_tools")
    builder_source = ast.get_source_segment(source, builder) or ""
    required = ('"name": "lies_eintrags_kategorien"',
                'brain_entry_categories(did, user_id=user_id)',
                '"lies_eintrags_kategorien": _kategorien',
                '"kategorien": data.get("categories") or []',
                '"elternpfade": data.get("parents") or []')
    missing = [item for item in required if item not in builder_source]
    check(not missing, "Hauptagenten-Werkzeug unvollstaendig: " + ", ".join(missing))
    check('"durchsuche_gedaechtnis", "lies_eintrags_kategorien", "web_suche"' in source,
          "Read-only Kategorienabruf muss parallelisierbar sein")

    print("PASS: Hauptagent liest Eintrags-Kategorien gezielt per doc_id")
    return 0


if __name__ == "__main__":
    sys.exit(main())

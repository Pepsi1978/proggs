#!/usr/bin/env python3
"""Regression checks for fast, race-safe Cortex category navigation."""

import ast
from pathlib import Path
import shutil
import subprocess


ROOT = Path(__file__).parents[1]
HTML = (ROOT / "dashboard" / "static" / "index.html").read_text(encoding="utf-8")
DASHBOARD_APP = (ROOT / "dashboard" / "app.py").read_text(encoding="utf-8")
BRAIN_APP = (ROOT / "brain-api" / "app.py").read_text(encoding="utf-8")


def load_dashboard_parent_handler():
    tree = ast.parse(DASHBOARD_APP)
    handler = next(
        node for node in tree.body
        if isinstance(node, ast.FunctionDef) and node.name == "entries_by_parent"
    )
    handler.decorator_list = []
    calls = []

    def fake_bget(path, **kwargs):
        calls.append((path, kwargs))
        return {"items": [
            {"doc_id": "multi", "category": "Persoenlich",
             "categories": ["Persoenlich", "Programmierung/Best Practices/OpenCode"]},
            {"doc_id": "child", "category": "Programmierung/Best Practices/OpenCode/Rules",
             "categories": ["Programmierung/Best Practices/OpenCode/Rules"]},
            {"doc_id": "desktop", "category": "Programmierung/Best Practices/Desktop",
             "categories": ["Programmierung/Best Practices/Desktop"]},
        ]}

    namespace = {"_bget": fake_bget, "USER_ID": "frank"}
    exec(compile(ast.Module(body=[handler], type_ignores=[]), "dashboard/app.py", "exec"), namespace)
    return namespace["entries_by_parent"], calls


def verify_backend_summary_and_filtering() -> None:
    handler, calls = load_dashboard_parent_handler()
    result = handler("Programmierung/Best Practices/OpenCode", 500)
    assert [item["doc_id"] for item in result["items"]] == ["multi", "child"]
    assert calls == [("/by-parent", {
        "parent": "Programmierung", "user_id": "frank", "include_text": False,
    })]

    calls.clear()
    handler("Programmierung", 500)
    assert calls == [("/by-parent", {
        "parent": "Programmierung", "user_id": "frank", "limit": 500,
        "include_text": False,
    })]


def verify_frontend_race_guards_and_cache() -> None:
    required = (
        "let entriesRequestId = 0, entriesAbortController = null;",
        "const entriesCache = new Map();",
        "function cachedEntriesFor(state)",
        "items:(broader.items||[]).filter(it=>itemInCategoryTree(it,state.parent))",
        "const requestId = ++entriesRequestId;",
        "if(entriesAbortController) entriesAbortController.abort();",
        'if(e.name==="AbortError" || requestId!==entriesRequestId) return;',
        "if(requestId!==entriesRequestId) return;",
        "entriesCache.set(cacheKey,data);",
        "box.replaceChildren(fragment);",
    )
    missing = [snippet for snippet in required if snippet not in HTML]
    assert not missing, f"Category navigation safeguards are incomplete: {missing}"
    node = shutil.which("node")
    assert node, "Node.js is required to parse the dashboard JavaScript"
    script = HTML[HTML.index("<script>") + len("<script>"):HTML.rindex("</script>")]
    subprocess.run([node, "--check", "-"], input=script, text=True, encoding="utf-8", check=True)


def verify_brain_summary_mode() -> None:
    required = (
        "include_text: bool = True",
        "meta_only = limit > 0 or not include_text",
        '(["chunk_text"] if not include_text else [])',
        '"match": (p.payload.get("chunk_text") or "") if not include_text else None',
        "if include_text and meta_only:",
    )
    missing = [snippet for snippet in required if snippet not in BRAIN_APP]
    assert not missing, f"Brain summary mode is incomplete: {missing}"
    assert BRAIN_APP.count("if include_text and meta_only:") == 2


def main() -> None:
    verify_backend_summary_and_filtering()
    verify_frontend_race_guards_and_cache()
    verify_brain_summary_mode()
    print("PASS: category navigation is summary-only, cached, and race-safe")


if __name__ == "__main__":
    main()

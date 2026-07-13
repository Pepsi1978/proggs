#!/usr/bin/env python3
"""Regression checks for dashboard-only overview bar category visibility."""

import ast
import json
import os
from pathlib import Path
import tempfile


ROOT = Path(__file__).parents[1]
HTML = (ROOT / "dashboard" / "static" / "index.html").read_text(encoding="utf-8")
APP = (ROOT / "dashboard" / "app.py").read_text(encoding="utf-8")


def verify_preference_helpers() -> None:
    """Run the real persistence helpers without needing the dashboard's Docker dependencies."""
    tree = ast.parse(APP)
    names = {"_overview_preferences", "_read_overview_preferences", "_write_overview_preferences"}
    helpers: list[ast.stmt] = [node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name in names]
    assert len(helpers) == len(names), "Overview preference helpers are missing"
    namespace = {"json": json, "os": os, "Path": Path}
    exec(compile(ast.Module(body=helpers, type_ignores=[]), "dashboard/app.py", "exec"), namespace)
    with tempfile.TemporaryDirectory() as directory:
        namespace["OVERVIEW_PREFERENCES_FILE"] = Path(directory) / "overview-preferences.json"
        normalized = namespace["_overview_preferences"]({
            "hidden_categories": ["Programmieren", "programmieren", " Gesundheit ", ""],
        })
        assert normalized == {"hidden_categories": ["Programmieren", "Gesundheit"]}
        namespace["_write_overview_preferences"](normalized)
        assert namespace["_read_overview_preferences"]() == normalized


def main() -> None:
    required_html = (
        'id="catBarToggleBtn"',
        'id="catHiddenSelect"',
        'function loadOverviewBarPreferences()',
        'function saveOverviewBarPreferences(next)',
        'const visibleTops = tops.filter(({n})=>!isOverviewBarCategoryHidden(n.path));',
        'visibleTops.forEach((g,i)=>{',
        'tops.forEach(({n})=>{',
        'apiSend("/api/overview/preferences","PUT",{hidden_categories:[...next]})',
    )
    missing_html = [snippet for snippet in required_html if snippet not in HTML]
    assert not missing_html, f"Overview bar visibility UI is incomplete: {missing_html}"

    required_app = (
        'OVERVIEW_PREFERENCES_FILE',
        '@app.get("/api/overview/preferences")',
        '@app.put("/api/overview/preferences")',
        '"hidden_categories"',
        'VERSION = "0.83.0 (13.07.2026, 13:43 Uhr)"',
    )
    missing_app = [snippet for snippet in required_app if snippet not in APP]
    assert not missing_app, f"Persistent overview preference API is incomplete: {missing_app}"
    verify_preference_helpers()

    print("PASS: overview bar categories are persistently hideable without removing the legend or categories")


if __name__ == "__main__":
    main()

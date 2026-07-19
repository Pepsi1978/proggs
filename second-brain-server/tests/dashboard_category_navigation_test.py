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
        'writeHistoryState({tab:"brain",parent:curParent},"#brain",false)',
        'typeof e.state.parent==="string" ? (e.state.parent||null) : null',
        'b.addEventListener("click",()=>goToParent(path||null));',
        'writeHistoryState({tab:tab,parent:tab==="brain"?curParent:null},"#"+tab,true)',
    )
    missing = [snippet for snippet in required if snippet not in HTML]
    assert not missing, f"Category navigation safeguards are incomplete: {missing}"
    node = shutil.which("node")
    assert node, "Node.js is required to parse the dashboard JavaScript"
    script = HTML[HTML.index("<script>") + len("<script>"):HTML.rindex("</script>")]
    subprocess.run([node, "--check", "-"], input=script, text=True, encoding="utf-8", check=True)

    history_start = HTML.index("  function writeHistoryState(")
    history_source = HTML[history_start:HTML.index("\n  function navTo", history_start)]
    nav_start = HTML.index("  function navTo(")
    nav_source = HTML[nav_start:HTML.index("\n  document.querySelectorAll", nav_start)]
    pop_start = HTML.index('  window.addEventListener("popstate"')
    pop_source = HTML[pop_start:HTML.index("\n\n  /* ── API", pop_start)]
    parent_start = HTML.index("  function goToParent(")
    parent_source = HTML[parent_start:HTML.index("\n\n  function renderChips", parent_start)]
    history_cases = """
const pushed = [];
const warnings = [];
const console = {warn: (...args) => warnings.push(args)};
const history = {pushState: state => pushed.push({...state}), replaceState:()=>{}};
const TABS = {overview:{},brain:{}};
let activeTab = "overview", curParent = null, curCategory = null, curQuery = "";
let popHandler = null, loads = 0;
const search = {value:""};
const document = {getElementById: () => search, querySelectorAll: () => []};
const location = {hash:"#brain"};
const window = {addEventListener: (name, fn) => { if(name === "popstate") popHandler = fn; }};
const confirmOverlay = {classList:{contains:()=>false}};
const drawer = {classList:{contains:()=>false}};
function closeConfirm(){}
function closeDrawerNow(){}
function setTab(tab){ activeTab = tab; }
function updateChips(){}
function loadEntries(){ loads += 1; }
%s
%s
%s
%s
goToParent("Programmierung");
goToParent("Programmierung/Best Practices");
goToParent("Programmierung/Best Practices/Opencode");
const paths = pushed.map(state => state.parent);
if(JSON.stringify(paths) !== JSON.stringify(["Programmierung","Programmierung/Best Practices","Programmierung/Best Practices/Opencode"])) {
  throw new Error("Drilldown states wrong: " + JSON.stringify(paths));
}
popHandler({state:pushed[1]});
if(activeTab !== "brain" || curParent !== "Programmierung/Best Practices") throw new Error("First back step not restored");
popHandler({state:pushed[0]});
if(curParent !== "Programmierung") throw new Error("Second back step not restored");
popHandler({state:{tab:"overview"}});
if(activeTab !== "overview") throw new Error("Overview state not restored");
if(loads < 5) throw new Error("Restored category levels were not reloaded");
history.pushState = () => { throw new Error("blocked"); };
goToParent("Programmierung");
if(activeTab !== "brain" || warnings.length !== 1) throw new Error("History failure did not degrade gracefully");
""" % (history_source, nav_source, pop_source, parent_source)
    subprocess.run([node, "-e", history_cases], text=True, encoding="utf-8", check=True)


def verify_brain_summary_mode() -> None:
    required = (
        "include_text: bool = True",
        "meta_only = limit > 0 or not include_text",
        '(["chunk_text"] if not include_text else [])',
        '"match": (p.payload.get("chunk_text") or "")[:320] if not include_text else None',
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

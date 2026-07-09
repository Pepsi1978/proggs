"""Struktureller Regressionstest für die Gedächtnisgewichtung bei Webantworten."""

import ast
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
AGENT = (ROOT / "agent" / "app.py").read_text(encoding="utf-8")
DASHBOARD = (ROOT / "dashboard" / "static" / "index.html").read_text(encoding="utf-8")
DASHBOARD_APP = (ROOT / "dashboard" / "app.py").read_text(encoding="utf-8")
MODELS = (ROOT.parent / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "data" / "model" / "Models.kt").read_text(encoding="utf-8")
SETTINGS = (ROOT.parent / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "ui" / "settings" / "SettingsScreen.kt").read_text(encoding="utf-8")


def require(text: str, source: str) -> None:
    if text not in source:
        raise AssertionError(f"Fehlt: {text}")


for expected in (
    'VALID_MEMORY_WEB_INFLUENCES = {"light", "normal", "strong"}',
    'return normalized if normalized in VALID_MEMORY_WEB_INFLUENCES else "normal"',
    '_update_config_file(lambda data: data.update(memory_web_influence=normalized))',
    '"memory_web_influence": MEMORY_WEB_INFLUENCE',
    'MEMORY_WEB_INFLUENCE = save_memory_web_influence(req.memory_web_influence)',
    'build_hauptagent_recall_internet_prompt()',
    'TOOLAGENT_SYSTEM + "\\n\\n" + memory_web_influence_prompt()',
    'system += "\\n\\n" + memory_web_influence_prompt()',
    'DIESE STUFE GILT NUR, WENN',
    'Bei reinen Gedächtnis-, reinen Web- oder Smalltalk-Antworten',
    'CONFIG_LOCK = threading.RLock()',
    'def _update_config_file(mutator: Callable[[dict], None]) -> dict:',
):
    require(expected, AGENT)

tree = ast.parse(AGENT)
config_req = next(node for node in tree.body if isinstance(node, ast.ClassDef) and node.name == "ConfigReq")
annotation = next(
    ast.unparse(node.annotation)
    for node in config_req.body
    if isinstance(node, ast.AnnAssign) and isinstance(node.target, ast.Name) and node.target.id == "memory_web_influence"
)
if "Literal" not in annotation or not all(repr(mode) in annotation for mode in ("light", "normal", "strong")):
    raise AssertionError("ConfigReq lehnt ungültige Gewichtungswerte nicht per 422 ab")

update_calls = [
    node for node in ast.walk(tree)
    if isinstance(node, ast.Call) and isinstance(node.func, ast.Name) and node.func.id == "_update_config_file"
]
if len(update_calls) < 4:
    raise AssertionError("Nicht alle config.json-Mutatoren verwenden den atomaren Update-Pfad")

require('"memory_web_influence", "limits"', DASHBOARD_APP)
require("def get_config() -> dict:\n    with CONFIG_LOCK:", AGENT)
require("def put_config(req: ConfigReq) -> dict:\n    with CONFIG_LOCK:", AGENT)
require("except httpx.HTTPStatusError as e:", DASHBOARD_APP)
require("raise HTTPException(status_code=e.response.status_code, detail=detail)", DASHBOARD_APP)

for mode in ("light", "normal", "strong"):
    require(f'data-memory-influence="{mode}"', DASHBOARD)
    require(f'"{mode}"', MODELS + SETTINGS)

require("memory_web_influence", MODELS)
require("Gedächtnis-Einfluss bei Websuchen", SETTINGS)
require("renderMemoryWebInfluence", DASHBOARD)
require("saveMemoryWebInfluence", DASHBOARD)
require("@media(max-width:520px)", DASHBOARD)
require(".set-row{grid-template-columns:minmax(0,1fr)}", DASHBOARD)

print("PASS: Gewichtung ist validiert, atomar persistent, responsiv und in kombinierten Pfaden wirksam")

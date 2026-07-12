"""Regressionstest für lokale GPT-5.6-Fast-Aliase und den Priority-Service-Tier."""

import ast
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
AGENT_SOURCE = (ROOT / "agent" / "app.py").read_text(encoding="utf-8")
ANDROID_SOURCE = (
    ROOT.parent
    / "CortexAndroid/app/src/main/java/de/frank/cortex/network/ApiClient.kt"
).read_text(encoding="utf-8")

EXPECTED = [
    "gpt-5.6-sol", "gpt-5.6-sol-fast",
    "gpt-5.6-terra", "gpt-5.6-terra-fast",
    "gpt-5.6-luna", "gpt-5.6-luna-fast",
]


tree = ast.parse(AGENT_SOURCE)
namespace: dict = {}
selected_nodes = []
for node in tree.body:
    if isinstance(node, ast.Assign) and any(
        isinstance(target, ast.Name) and target.id in {"CODEX_GPT56_MODELS", "CODEX_FAST_MODEL_ALIASES"}
        for target in node.targets
    ):
        selected_nodes.append(node)
    if isinstance(node, ast.FunctionDef) and node.name in {"_codex_slug", "_codex_model_request_fields"}:
        selected_nodes.append(node)
exec(compile(ast.Module(body=selected_nodes, type_ignores=[]), "agent/app.py", "exec"), namespace)

if namespace["CODEX_GPT56_MODELS"] != EXPECTED:
    raise AssertionError("Die sechs GPT-5.6-Auswahlen fehlen oder haben eine falsche Reihenfolge")

request_fields = namespace["_codex_model_request_fields"]
for family in ("sol", "terra", "luna"):
    normal = f"gpt-5.6-{family}"
    fast = f"{normal}-fast"
    if request_fields(normal) != {"model": normal}:
        raise AssertionError(f"Normales {family}-Modell darf keinen Priority-Tier erhalten")
    if request_fields(fast) != {"model": normal, "service_tier": "priority"}:
        raise AssertionError(f"Fast-Alias für {family} wird nicht korrekt aufgelöst")

if AGENT_SOURCE.count("**model_fields") < 3:
    raise AssertionError("Priority-Felder fehlen in Text-, Tool-Loop- oder Abschluss-Request")

for model in EXPECTED:
    if f'"{model}"' not in ANDROID_SOURCE:
        raise AssertionError(f"Android-Auswahl fehlt: {model}")
if '.put("model", modelConfig.model)' not in ANDROID_SOURCE or 'body.put("service_tier", it)' not in ANDROID_SOURCE:
    raise AssertionError("Android sendet Basismodell und Priority-Tier nicht getrennt")

print("PASS: GPT-5.6 Sol/Terra/Luna Fast nutzen Basismodell + service_tier=priority")

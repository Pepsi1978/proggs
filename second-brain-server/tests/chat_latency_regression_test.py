#!/usr/bin/env python3
"""Offline regression checks for the lossless Cortex chat latency changes."""
from __future__ import annotations

import ast
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
AGENT_PATH = ROOT / "agent" / "app.py"
AGENT = AGENT_PATH.read_text(encoding="utf-8")
ANDROID_ROOT = ROOT.parent / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex"
API = (ANDROID_ROOT / "network" / "ApiClient.kt").read_text(encoding="utf-8")
CHAT = (ANDROID_ROOT / "ui" / "chat" / "ChatViewModel.kt").read_text(encoding="utf-8")


def require(needle: str, source: str, message: str) -> None:
    if needle not in source:
        raise AssertionError(message)


def load_routing_helpers() -> dict:
    names = {"_TRIVIAL_GREETING_RE"}
    functions = {"_is_trivial_greeting", "projektstand_question"}
    nodes = []
    for node in ast.parse(AGENT).body:
        if isinstance(node, ast.Assign) and any(
            isinstance(target, ast.Name) and target.id in names for target in node.targets
        ):
            nodes.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name in functions:
            nodes.append(node)
    namespace = {"re": re}
    exec(compile(ast.Module(body=nodes, type_ignores=[]), str(AGENT_PATH), "exec"), namespace)
    return namespace


def main() -> int:
    ns = load_routing_helpers()
    greeting = ns["_is_trivial_greeting"]
    project = ns["projektstand_question"]

    assert greeting("Bist du da? Kannst du mich hören?")
    assert greeting("Hallo!")
    assert not greeting("Hallo, wie ist das Wetter heute?")
    assert not greeting("Kannst du mich hören und im Internet nachsehen?")

    assert project("Woran habe ich zuletzt gearbeitet?")
    assert project("Wie ist der aktuelle Stand beim OpenCode Launcher?")
    assert not project("Ich habe gerade programmiert. Meine Idee ist weniger Kontext. Was denkst du darüber?")
    assert not project("Was sagt das Internet zu meiner Vermutung über AGENTS.md?")

    require('yield _sse_event({"type": "ready"', AGENT, "SSE ready event missing")
    require('"canonical_reply": True', AGENT, "SSE canonical reply capability missing")
    require('yield _sse_event({"type": "heartbeat"', AGENT, "SSE heartbeat missing")
    require('timeout=15.0', AGENT, "SSE heartbeat interval is not bounded")
    require('"native_required": True', AGENT, "Native web preflight marker missing")
    require('hauptagent_answer_recall_native_web(', AGENT, "Single-pass web+memory answer missing")
    require('meta["adaptive_recall"] = "expanded"', AGENT, "Adaptive recall fallback missing")
    require('self_rules_audit', AGENT, "Short self-rule audit missing")

    require('canonicalReplyStream = evt.optBoolean("canonical_reply", false)', API,
            "Android does not require the canonical reply capability")
    require('if (!canonicalReplyStream)', API, "Android does not reject unverified raw deltas")
    require('"heartbeat" ->', API, "Android does not accept SSE heartbeat")
    require('return@withContext result', API, "Android still waits for EOF after done")
    require('.readTimeout(60, TimeUnit.SECONDS)', API, "Streaming idle timeout does not match heartbeat")
    require('class ChatStreamException(', API, "Structured stream failure missing")
    require('same_request_id" to true', CHAT, "Fallback request-id invariant missing")
    require('Channel<ByteArray?>(TTS_PREFETCH_AHEAD)', CHAT, "TTS prefetch is not centrally bounded")

    print("PASS: Chat latency fast paths preserve fallbacks, rules, streaming and TTS ordering")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

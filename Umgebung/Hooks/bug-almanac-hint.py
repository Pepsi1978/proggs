#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""bug-almanac-hint.py — UserPromptSubmit-Logik des semantischen Almanach-Triggers (Welle 3, 2026-06-15).

Ergaenzt den DATEI-Trigger (bug-almanac-guard, PreToolUse) um einen PROMPT-Trigger: scannt den
User-Prompt auf bereichstypische Stichwoerter und injiziert EINMALIG pro Bereich+Session einen
PASSIVEN Hinweis (additionalContext), welcher Almanach relevant sein koennte. So wird auch reine
Konzept-/Planungs-Arbeit OHNE Datei-Edit abgedeckt (der Datei-Guard greift erst beim Edit).

Bewusst KEINE Embeddings/MCP (in einem Hook nicht sauber aufrufbar) — robuste Mehrwort-Stichwort-
Heuristik. Passiv: nie Block, nur Hinweis. Einmalig je Bereich/Session (Marker im TEMP), damit
UserPromptSubmit-additionalContext nicht akkumuliert (claude-hooks 2.5). Gibt bei Treffer EIN
spec-konformes JSON aus, sonst nichts. Endet immer mit 0.

Aufgerufen von bug-almanac-hint.{ps1,sh} (duenne Wrapper: stdin rein, stdout durch).
"""
import sys
import os
import re
import json
import hashlib
import tempfile

# almanach-relpath -> (Anzeigename, [Mehrwort-/eindeutige Stichwoerter, lowercase])
AREAS = {
    "web/chrome-extensions":        ("Chrome-Erweiterungen", ["manifest v3", "manifest_version", "chrome extension", "chrome-erweiterung", "content script", "service worker", "chrome.storage", "chrome.runtime"]),
    "desktop/wake-word":            ("Wake-Word",            ["wake word", "wakeword", "wake-word", "keyword spotter", "sherpa-onnx", "porcupine"]),
    "desktop/voice-pipeline":       ("Voice-Pipeline",       ["voice pipeline", "endpointing", "barge-in", "wachfenster", "vad-"]),
    "android/jetpack-compose":      ("Jetpack Compose",      ["jetpack compose", "recomposition", "@composable", "compose state", "remembersaveable"]),
    "android/room":                 ("Room-DB",              ["room database", "room dao", "room entity", "@dao", "@entity", "roomdatabase"]),
    "android-build/r8":             ("R8/ProGuard",          ["proguard", "keep rule", "r8 minify", "minifyenabled", "shrinkresources"]),
    "android/firebase-billing":     ("Firebase Billing",     ["firebase billing", "play billing", "in-app purchase", "paywall", "billingclient"]),
    "claude-tooling/claude-hooks":  ("Claude Hooks",         ["posttooluse", "pretooluse", "sessionstart hook", "claude hook", "hookspecificoutput"]),
    "web/typescript":               ("TypeScript",           ["tsconfig", "moduleresolution", "ts-node", "strictnullchecks"]),
    "android/retrofit-okhttp-moshi":("Retrofit/OkHttp",      ["retrofit", "okhttp", "moshi", "interceptor"]),
    "server/docker":                ("Docker/Compose",       ["docker compose", "docker-compose", "compose.yaml", "compose.yml", "dockerfile", "depends_on", "mem_limit", "healthcheck", "restart: unless-stopped", "ufw docker", "named volume", "json-file log"]),
    "server/reverse-proxy-tls":     ("Reverse-Proxy/TLS (Caddy)", ["caddy", "caddyfile", "reverse_proxy", "reverse proxy", "lets encrypt", "let's encrypt", "acme", "tls renewal", "on-demand-tls", "flush_interval", "trusted_proxies", "handle_path", "network-online.target", "systemd unit", "journald", "unattended-upgrades", "needrestart", "vm.swappiness", "fail2ban", "ufw allow", "certbot", "ntp drift"]),
    "server/ai-agent-frameworks":   ("KI-Agent (Loop/Tools)", ["autonomer agent", "agent loop", "tool loop", "tool-loop", "bibliothekar-agent", "dirigent-agent", "pydantic-ai", "pydantic_ai", "langgraph", "create_react_agent", "graphrecursionerror", "max iterations", "tool_use", "tool_result", "idempotency key", "memory poisoning", "exponential backoff", "run_in_threadpool"]),
    "server/fastapi":               ("FastAPI/uvicorn",      ["fastapi", "uvicorn", "async def endpoint", "blockiert den event loop", "event-loop blockiert", "run_in_threadpool", "asyncio.to_thread", "corsmiddleware", "allow_credentials", "@app.exception_handler", "app.add_middleware", "uvicorn --workers", "lifespan event", "on_event startup", "backgroundtasks", "request.body() limit", "pydantic v2 fastapi", "starlette"]),
    "server/client-anbindung":      ("Client-Anbindung (VPN/REST)", ["network security config", "network_security_config", "cleartext http", "cleartext not permitted", "cleartexttrafficpermitted", "app transport security", "nsallowslocalnetworking", "nslocalnetworkusagedescription", "local network permission", "wireguard client", "always-on vpn", "per-app vpn", "trust anchor for certification", "certificate pinning", "ip im san", "idempotency key", "offline outbox", "10.8.0.1", "brain_url", "doze netzwerk", "dontkillmyapp", "com.apple.security.network.client"]),
    "opencode/server-agent-remote-mcp": ("Remote-MCP/Server-Agent (CLI)", ["remote mcp anbinden", "type remote url", "type: remote", "mcp add --transport", "claude mcp add", "second-brain mcp", "10.8.0.1:8001", "mcp nur ueber wireguard", "mcp ueber vpn", "invalid host header", "allowed_hosts", "no valid session id", "mcp reconnect", "server-only agent", "server-only mode", "mcp tools nur subagent", "mcp tools tauchen nicht auf", "eigenes gehirn anbinden", "zweites gehirn mcp"]),
    "agents/loop-engineering":      ("Loop Engineering",     ["loop engineering", "loop-engineering", "agentic loop", "agentic-loop", "ralph loop", "ralph-loop", "ralph wiggum", "react loop", "react-loop", "reflexion", "self-improving agent", "self-improving loop", "self-improving-loop", "bounded execution", "circuit breaker", "circuit-breaker", "evaluator-optimizer", "reflection loop", "reflection-loop", "loop-agent", "loop agent", "agenten-loop", "agenten loop", "agent in einer schleife", "agent in einer endlosschleife", "schleifen-agent"]),
}


def main():
    try:
        raw = sys.stdin.read()
        data = json.loads(raw) if raw and raw.strip() else {}
    except Exception:
        return 0
    prompt = (data.get("prompt") or "")
    if not isinstance(prompt, str) or not prompt.strip():
        return 0
    low = prompt.lower()
    session = str(data.get("session_id") or "nosess")
    sid = hashlib.sha1(session.encode("utf-8", "replace")).hexdigest()[:10]
    bugs_dir = os.path.join(os.path.expanduser("~"), "proggs", "bugs")
    tmp = tempfile.gettempdir()

    hits = []
    for rel, (name, kws) in AREAS.items():
        if not any(k in low for k in kws):
            continue
        almanach = os.path.join(bugs_dir, rel + ".md")
        if not os.path.isfile(almanach):
            continue
        marker = os.path.join(tmp, "bug-almanac-hint-%s-%s.flag" % (sid, rel.replace("/", "_")))
        if os.path.exists(marker):
            continue  # in dieser Session fuer diesen Bereich schon gehintet
        try:
            with open(marker, "w", encoding="utf-8") as f:
                f.write("1")
        except Exception:
            pass
        hits.append("%s (bugs/%s.md)" % (name, rel))

    # ── Dynamischer Fallback: JEDER nicht kuratierte Almanach wird ueber seinen
    # Dateinamen-als-Phrase grob getriggert (eindeutige Eigennamen als Stichwort).
    # Deckt ALLE Almanache ab (nicht nur die kuratierten oben) UND bleibt zukunftssicher:
    # neue Almanache sind automatisch dabei, ohne Pflege. Die kuratierten AREAS bleiben
    # der praezise Mehrwort-Layer (uebersprungen). Wortgrenzen-Match (\b...\b) haelt
    # Fehlalarme klein; >=4 Zeichen filtert zu kurze/generische Namen.
    try:
        for root, _dirs, files in os.walk(bugs_dir):
            for fn in files:
                if not fn.endswith(".md"):
                    continue
                rel_full = os.path.relpath(os.path.join(root, fn), bugs_dir).replace(os.sep, "/")
                if "/" not in rel_full:
                    continue  # top-level Meta-Dateien (README/SYSTEM/OFFENE-*) sind keine Almanache
                relkey = rel_full[:-3]  # ohne ".md" -> gleiche Form wie die AREAS-keys
                if relkey in AREAS:
                    continue  # schon vom praezisen kuratierten Layer behandelt
                phrase = fn[:-3].replace("-", " ").replace("_", " ").strip().lower()
                if len(phrase) < 4:
                    continue  # zu kurz/generisch -> nur kuratiert behandeln
                if not re.search(r"\b" + re.escape(phrase) + r"\b", low):
                    continue
                marker = os.path.join(tmp, "bug-almanac-hint-%s-%s.flag" % (sid, relkey.replace("/", "_")))
                if os.path.exists(marker):
                    continue
                try:
                    with open(marker, "w", encoding="utf-8") as f:
                        f.write("1")
                except Exception:
                    pass
                disp = fn[:-3].replace("-", " ").replace("_", " ").title()
                hits.append("%s (bugs/%s)" % (disp, rel_full))
    except Exception:
        pass

    if not hits:
        return 0

    msg = ("MOEGLICHER ALMANACH-BEREICH erkannt (semantischer Prompt-Trigger): "
           + "; ".join(hits)
           + ". Falls du hier echte Arbeit planst, lies VOR dem Code den Kurzcheck (Read mit limit=80) "
           + "des passenden Almanachs + der Best-Practices-Gegenseite - auch bei reiner Konzept-/Planungsarbeit "
           + "ohne Datei-Edit (da greift der Datei-Guard nicht). Nur ein Hinweis, kein Block.")
    out = {"hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": msg}}
    try:
        sys.stdout.write(json.dumps(out, ensure_ascii=False))
    except Exception:
        pass
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception:
        sys.exit(0)

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

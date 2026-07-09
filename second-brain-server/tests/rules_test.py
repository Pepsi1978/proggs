#!/usr/bin/env python3
"""Reiner Unit-Test des Selbst-Regel-Moduls agent/rules.py — kein Netz, keine
externen Abhaengigkeiten (nur Python-stdlib). Prueft Ablage (load/save/add/update/
delete + Limit), Regelblock-Bildung und die Trigger-Erkennung Regel-vs-Speichern.

Lauf (lokal, da rules.py stdlib-only ist):
    python3 tests/rules_test.py
oder im Container:
    docker compose run --rm agent python3 /app/tests/rules_test.py
Exit 0 = PASS, 1 = FAIL.
"""
from __future__ import annotations

import importlib.util
import os
import sys
import tempfile
from pathlib import Path

# Stdout auf UTF-8 (Windows-Konsole ist sonst cp1252 -> Crash bei Umlauten in
# Fehlermeldungen; python-windows Kurzcheck #7). Auf Linux/Container no-op.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

# rules.py direkt per Pfad laden (agent/ ist kein Package; Projekt-Muster wie
# embed_input_prefix_test.py).
_RULES_PY = Path(__file__).resolve().parent.parent / "agent" / "rules.py"
_spec = importlib.util.spec_from_file_location("cortex_rules", _RULES_PY)
rules = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rules)

_FAILS: list[str] = []


def check(cond: bool, msg: str) -> None:
    if cond:
        print(f"  ok: {msg}")
    else:
        print(f"  FAIL: {msg}")
        _FAILS.append(msg)


def main() -> int:
    with tempfile.TemporaryDirectory() as d:
        # --- Ablage ---------------------------------------------------------
        missing = os.path.join(d, "nope.json")
        check(rules.load_rules(missing) == [], "load_rules(missing) -> []")

        p = os.path.join(d, "Regeln", "regeln.json")
        r = {"id": "abc1234567", "text": "Antworte kurz.", "titel": "Kurz",
             "enabled": True, "created_at": "2026-07-08T21:00:00"}
        rules.save_rules(p, [r])
        check(rules.load_rules(p) == [r], "save_rules/load_rules roundtrip")
        check(os.path.exists(p), "save_rules legt Ordner+Datei an")

        corrupt = os.path.join(d, "corrupt.json")
        with open(corrupt, "w", encoding="utf-8") as f:
            f.write("{ this is not json")
        check(rules.load_rules(corrupt) == [], "load_rules(corrupt) -> []")

        p2 = os.path.join(d, "add.json")
        new = rules.add_rule(p2, "Antworte als Fliesstext.", "Fliesstext",
                             "2026-07-08T21:00:00")
        check(new["text"] == "Antworte als Fliesstext.", "add_rule speichert text")
        check(new["titel"] == "Fliesstext", "add_rule speichert titel")
        check(new["enabled"] is True, "add_rule enabled=True")
        check(len(new["id"]) == 10, "add_rule id-Laenge 10")
        check(rules.load_rules(p2)[0]["id"] == new["id"], "add_rule persistiert")

        p3 = os.path.join(d, "limit.json")
        for i in range(rules.MAX_RULES):
            rules.add_rule(p3, f"Regel {i}", f"T{i}", "2026-07-08T21:00:00")
        limit_hit = False
        try:
            rules.add_rule(p3, "eine zu viel", "X", "2026-07-08T21:00:00")
        except rules.RuleLimitError:
            limit_hit = True
        check(limit_hit, "add_rule wirft RuleLimitError bei > MAX_RULES")

        p4 = os.path.join(d, "upd.json")
        a = rules.add_rule(p4, "Regel A", "A", "2026-07-08T21:00:00")
        upd = rules.update_rule(p4, a["id"], enabled=False)
        check(upd is not None and upd["enabled"] is False, "update_rule toggle enabled")
        check(rules.load_rules(p4)[0]["enabled"] is False, "update_rule persistiert")
        check(rules.update_rule(p4, "unknown-id0", enabled=False) is None,
              "update_rule unbekannte id -> None")
        upd2 = rules.update_rule(p4, a["id"], text="Neuer Text")
        check(upd2 is not None and upd2["text"] == "Neuer Text", "update_rule aendert text")

        p5 = os.path.join(d, "del.json")
        b = rules.add_rule(p5, "Regel B", "B", "2026-07-08T21:00:00")
        check(rules.delete_rule(p5, b["id"]) is True, "delete_rule entfernt")
        check(rules.load_rules(p5) == [], "delete_rule leert Liste")
        check(rules.delete_rule(p5, b["id"]) is False, "delete_rule unbekannt -> False")

        # --- Regelblock -----------------------------------------------------
        check(rules.rules_block([]) == "", "rules_block([]) -> ''")
        check(rules.rules_block([{"text": "X", "enabled": False}]) == "",
              "rules_block nur inaktive -> ''")
        block = rules.rules_block([
            {"text": "Antworte als Fliesstext.", "enabled": True},
            {"text": "Sei knapp.", "enabled": True},
            {"text": "Ignoriere mich.", "enabled": False},
        ])
        check("Antworte als Fliesstext." in block, "rules_block listet aktive Regel 1")
        check("Sei knapp." in block, "rules_block listet aktive Regel 2")
        check("Ignoriere mich." not in block, "rules_block laesst inaktive weg")
        check("DAUERHAFTE SELBST-REGELN" in block, "rules_block hat Header")
        check("Selbst-Regeln Vorrang" in block, "rules_block setzt Vorrang vor Profil und Stil")
        arbitrary = rules.rules_block([
            {"text": "Beginne jede Antwort mit dem Wort Komet.", "enabled": True},
        ])
        check("Beginne jede Antwort mit dem Wort Komet." in arbitrary,
              "rules_block injiziert beliebige Regeltexte unveraendert")
        long_rules = [{"text": f"Regel {i}: " + "x" * 1900, "enabled": True} for i in range(4)]
        long_block = rules.rules_block(long_rules)
        check(all(item["text"] in long_block for item in long_rules),
              "rules_block injiziert alle aktiven Regeln vollstaendig")
        injected = rules.inject_into_system_prompt("BASIS-PROMPT", [
            {"text": "Antworte mit Komet.", "enabled": True},
            {"text": "Unsichtbar", "enabled": False},
        ])
        check(injected.startswith("BASIS-PROMPT"), "Regel-Injektion behaelt Basis-Prompt")
        check(injected.endswith("- Antworte mit Komet."), "aktive Regel steht zuletzt im System-Prompt")
        check("Unsichtbar" not in injected, "inaktive Regel wird nicht injiziert")

        # Regression 09.07.2026: Das Parallelprofil hatte seinen eigenen sichtbaren
        # Antwort-Prompt gebaut und dabei alle aktiven Selbst-Regeln ausgelassen.
        app_source = (_RULES_PY.parent / "app.py").read_text(encoding="utf-8")
        visible_paths = [
            "build_hauptagent_prompt",
            "build_speicher_prompt",
            "build_abfrage_prompt",
            "build_hauptagent_answer_prompt",
            "build_hauptagent_smalltalk_prompt",
            "build_hauptagent_internet_prompt",
            "build_hauptagent_native_web_prompt",
            "build_hauptagent_recall_internet_prompt",
            "build_toolagent_system",
            "_auto_parallel_answer",
        ]
        for name in visible_paths:
            start = app_source.index(f"def {name}(")
            end = app_source.find("\ndef ", start + 1)
            body = app_source[start:end if end >= 0 else len(app_source)]
            check("_with_self_rules(" in body, f"{name} injiziert Selbst-Regeln")
        provider_paths = [
            "codex_generate",
            "codex_generate_tools",
            "gemini_generate_with_native_web",
            "_llm_generate_once",
        ]
        for name in provider_paths:
            start = app_source.index(f"def {name}(")
            end = app_source.find("\ndef ", start + 1)
            body = app_source[start:end if end >= 0 else len(app_source)]
            check("_reinforce_self_rules(" in body, f"{name} erinnert Selbst-Regeln vor Ausgabe")

        # --- Trigger-Erkennung Regel vs. Speichern --------------------------
        rule_true = [
            "Mach daraus eine Regel.",
            "Schreib das bitte in deine Regeldatei.",
            "Das ist eine dauerhafte Regel fuer dich.",
            "Merk dir das als Regel.",
            "Antworte mir ab jetzt immer als Fliesstext.",
            "Verhalte dich generell immer hoeflich.",
            "Gib dir eine Regel, dass du immer kurz antwortest.",
        ]
        for t in rule_true:
            check(rules.is_rule_request(t) is True, f"is_rule_request TRUE: {t!r}")

        rule_false = [
            "Merk dir, dass mein Zahnarzttermin am Freitag ist.",
            "Speicher das bitte ab.",
            "Leg das in meinem Gedaechtnis ab.",
            "Wie spaet ist es?",
            "Was kannst du alles?",
        ]
        for t in rule_false:
            check(rules.is_rule_request(t) is False, f"is_rule_request FALSE: {t!r}")

    if _FAILS:
        print(f"\n{len(_FAILS)} FAIL(s):")
        for m in _FAILS:
            print(f"  - {m}")
        return 1
    print("\nAlle Tests bestanden.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

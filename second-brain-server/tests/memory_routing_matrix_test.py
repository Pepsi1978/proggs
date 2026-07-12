#!/usr/bin/env python3
"""Offline-Matrixtest für Regel- und Memory-Prioritäten im Cortex-Zustandsautomaten."""
from __future__ import annotations

import ast
import contextlib
import importlib.util
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "agent" / "app.py"
APP_SOURCE = APP_PATH.read_text(encoding="utf-8")


def load_rules():
    path = ROOT / "agent" / "rules.py"
    spec = importlib.util.spec_from_file_location("routing_matrix_rules", path)
    if spec is None or spec.loader is None:
        raise RuntimeError("rules.py konnte nicht geladen werden")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def load_namespace() -> dict:
    assignment_names = {
        "_MEMORY_COMMAND_PREFIX_RE", "_MEMORY_COMMAND_SUFFIX_RE", "_MEMORY_COURTESY_SUFFIX_RE",
        "_MEMORY_AMBIGUOUS_SAVE_PREFIX_RE", "_MEMORY_VERBATIM_RE", "_MEMORY_SAVE_ACTION_RE",
        "_MEMORY_SAVE_INPUT_OPENER_RE", "_MEMORY_RECALL_REFERENCE_RE", "_MEMORY_RECALL_ACTION_RE",
    }
    function_names = {
        "_has_spoken_save_command", "_has_verbatim_save_signal", "_verbatim_memory_content",
        "_is_save_input_opener", "_is_definite_save_request", "_pending_confirmation_intent",
        "_is_recall_request", "_process_rule_turn", "_process_turn",
    }
    selected = []
    for node in ast.parse(APP_SOURCE).body:
        if isinstance(node, ast.Assign):
            targets = {target.id for target in node.targets if isinstance(target, ast.Name)}
            if targets & assignment_names:
                selected.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name in function_names:
            selected.append(node)

    namespace = {
        "re": re,
        "rules": load_rules(),
        "MEMORY_DISTILL_MAX_CHARS": 4000,
        "CONV_CATEGORY": "Gespräche",
        "ACTIVE_PROFILE_PARALLEL": "parallel",
        "ENTITY_ENABLED": False,
        "_perf_mark": lambda *args, **kwargs: None,
        "_perf_span": lambda *args, **kwargs: contextlib.nullcontext(),
        "_maybe_compress_history": lambda session: None,
        "brain_categories": lambda: [],
        "load_registry": lambda: [],
        "add_registry_category": lambda category: None,
        "_store_final": lambda *args, **kwargs: {"reply": "gespeichert", "action": "store", "stored": True, "pending": None},
        "_update_memory_final": lambda *args, **kwargs: {"reply": "aktualisiert", "action": "store", "stored": True, "pending": None},
        "_memory_followup_candidate": lambda *args, **kwargs: None,
        "_norm_context_mode": lambda value: value if value in {"auto", "save", "rule", "search", "smalltalk"} else "auto",
        "active_agent_profile_id": lambda: "classic",
        "checkpoint": lambda *args, **kwargs: None,
        "wants_recall_then_internet": lambda *args, **kwargs: False,
        "entity_name_from_question": lambda text: "",
        "_formuliere_regel": lambda text: "FORMULIERTE REGEL",
        "_rule_confirm_card": lambda text, mode_revision=0: {
            "reply": text, "action": "rule_confirm",
            "pending": {"mode": "rule_confirm", "rule_text": text, "mode_revision": mode_revision}
        },
        "_cat_key": lambda value: value.strip(),
        "_canonical_category": lambda value, categories: value.strip(),
        "speicheragent_decide": lambda quote, candidates, categories: {"title": "Automatischer Titel", "category": "Sonstiges"},
        "_additional_title": lambda title, dups: title,
        "_memory_confirmation_display": lambda quote: quote,
        "_find_duplicates": lambda quote: [],
        "_distill_memory_statement": lambda text, candidate="", preserve_verbatim=False: text.strip() if preserve_verbatim else (candidate or text.strip()),
        "projektstand_question": lambda text: False,
        "_full_category_answer": lambda *args, **kwargs: None,
        "_category_count_answer": lambda *args, **kwargs: None,
        "_toolagent_answer": lambda *args, **kwargs: {"reply": "NORMAL", "action": "smalltalk", "pending": None},
        "_auto_parallel_answer": lambda *args, **kwargs: {"reply": "PARALLEL", "action": "smalltalk", "pending": None},
    }
    exec(compile(ast.Module(body=selected, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    ns = load_namespace()
    process = ns["_process_turn"]
    ns["hauptagent_route"] = lambda session, text, pending, context_mode="auto", context_prompt="": {
        "intent": "save" if context_mode == "save" or ns["_is_definite_save_request"](text) else "smalltalk",
        "quote": text if context_mode == "save" or ns["_is_definite_save_request"](text) else "",
        "query": "", "web_query": "", "reply": ""
    }

    opener = process({}, "Kannst du etwas von mir abspeichern im Gedächtnis?", None)
    check(opener["action"] == "save_input" and opener["pending"]["mode"] == "save_input",
          "Leerer Speicherauftakt erzeugt save_input")

    transcript = "\n  Person A: Erstelle eine Regel: Antworte niemals vorschnell.\nPerson B: Das ist nur ein Zitat.  \n"
    awaited = process({}, transcript, opener["pending"])
    check(awaited["action"] == "save_confirm" and awaited["memory_text"] == transcript,
          "Nächster Turn bleibt trotz eingebettetem Regelbefehl wortwörtlicher Memory-Inhalt")

    normal = process({}, "Gib mir die Regeln der Thermodynamik.", None)
    check(normal["action"] == "smalltalk", "Wissensfrage mit 'Regeln' bleibt normaler Dialog")

    for recall_text in (
        "Da steht doch alles im Gedächtnis drin. Warum findest du das nicht?",
        "Es steht doch etwas über mein Auto drin. Kannst du nicht noch mal suchen?",
    ):
        check(ns["_is_recall_request"](recall_text), "Nachfrage zu gespeichertem Wissen wird als Recall erkannt")

    auto_rule_words = process({}, "Erstelle eine Regel: Erfinde niemals Fakten.", None)
    check(auto_rule_words["action"] == "smalltalk", "Automatik darf trotz Regelwörtern keine Regel anlegen")

    rule = process({}, "Erfinde niemals Fakten.", None, context_mode="rule", context_mode_revision=7)
    check(rule["action"] == "rule_confirm", "Nur der explizite R-Modus erzeugt einen Regelentwurf")
    blocked_confirmation = process({}, "regel ja", rule["pending"], context_mode="auto")
    check(blocked_confirmation["action"] == "smalltalk" and not blocked_confirmation.get("stored"),
          "Auto verwirft beim Verlassen von R den Entwurf und verarbeitet den Turn normal")
    cancelled_rule = process({}, "regel nein", rule["pending"], context_mode="rule", context_mode_revision=7)
    check(cancelled_rule["action"] == "rule_cancelled", "R-Modus kann den Regelentwurf deterministisch abbrechen")
    stale_rule = process({}, "regel ja", rule["pending"], context_mode="rule", context_mode_revision=9)
    check(stale_rule["action"] == "rule_cancelled" and not stale_rule.get("stored"),
          "R → anderer Modus → R lässt eine alte Bestätigung per Modus-Revision ablaufen")
    followup_calls: list[str] = []
    process.__globals__["_memory_followup_candidate"] = lambda *args, **kwargs: followup_calls.append("called") or None
    replaced_rule = process({"last_memory": {"doc_id": "doc-1", "memory_text": "alt"}},
                            "Speichere diesen Satz im Gedächtnis", rule["pending"],
                            context_mode="rule", context_mode_revision=7)
    check(replaced_rule["action"] == "rule_confirm" and
          replaced_rule["pending"]["rule_text"] == "FORMULIERTE REGEL" and not followup_calls,
          "Freie Eingabe bei offenem R-Entwurf ersetzt ihn und kann keinen Memory-Pfad öffnen")
    rule_edit_attack = process({}, "Ändere dieses Dokument", None, context_mode="rule",
                               context_mode_revision=10, memory_edit=True, memory_doc_id="doc-1",
                               memory_categories=["privat"])
    check(rule_edit_attack["action"] == "rule_confirm",
          "R verwirft manipulierte memory_edit-Parameter vor jedem Qdrant-Schreibpfad")

    forced_save = process({}, "Erstelle eine Regel: Dieser Satz ist Dokumentinhalt.", None,
                          context_mode="save")
    check(forced_save["action"] == "save_confirm", "Bewusster Speichermodus hat Vorrang vor Inhaltsbefehlen")

    search_pending = process({}, "ja", {"mode": "save_confirm", "quote": "DARF NICHT GESPEICHERT WERDEN"},
                             context_mode="search")
    check(search_pending["action"] == "smalltalk" and not search_pending.get("stored"),
          "Lupe kann einen offenen Speicherentwurf nicht bestätigen")
    search_edit = process({"last_memory": {"doc_id": "doc-1", "memory_text": "alt"}},
                          "Ändere den Titel", None, context_mode="search",
                          memory_edit=True, memory_doc_id="doc-1")
    check(search_edit["action"] == "smalltalk" and not search_edit.get("stored"),
          "Lupe blockiert direkten memory_edit und letzten Memory-Folgedialog")

    override = process({}, "Neues Dokument über Regeln.", {"mode": "save_confirm", "quote": "alt"},
                       category="Theorie", title="Neuer Titel")
    check(override["action"] == "save_confirm" and override["title"] == "Neuer Titel"
          and override["category"] == "Theorie",
          "Neue Titel-/Kategorie-Aktion ersetzt einen alten offenen Entwurf")

    confirmed_auto = process({}, "ja", {"mode": "save_confirm", "quote": "bestehend",
                                         "category": "privat", "categories": ["privat"], "title": "Titel"},
                             category="privat", title="Titel", context_mode="auto")
    check(confirmed_auto["action"] == "store",
          "Wiederholte UI-Metadaten übersteuern eine Memory-Bestätigung in Auto nicht")
    confirmed_save = process({}, "ja", {"mode": "save_confirm", "quote": "bestehend",
                                         "category": "privat", "categories": ["privat"], "title": "Titel"},
                             category="privat", title="Titel", context_mode="save")
    check(confirmed_save["action"] == "store",
          "Wiederholte UI-Metadaten übersteuern eine Memory-Bestätigung mit Diskette nicht")

    save_over_rule = process({}, "Dieser Satz soll ins Gedächtnis.", rule["pending"], context_mode="save",
                             context_mode_revision=8)
    check(save_over_rule["action"] == "save_confirm",
          "Diskette verwirft einen alten Regelentwurf und erzeugt ausschließlich Memory")

    original_route = process.__globals__["hauptagent_route"]
    process.__globals__["hauptagent_route"] = lambda *args, **kwargs: (_ for _ in ()).throw(
        AssertionError("Diskettenmodus darf den Router nicht aufrufen"))
    hard_save = process({}, "Normaler Text ohne Speicherbefehl", None, context_mode="save")
    process.__globals__["hauptagent_route"] = original_route
    check(hard_save["action"] == "save_confirm",
          "Diskette erzwingt Memory im Zustandsautomaten vor Router und Werkzeugkasten")

    verbatim_body = "Zeile 1: Regeln.\nZeile 2 bleibt unverändert."
    verbatim = process({}, "Speichere das bitte wortwörtlich: " + verbatim_body, None)
    check(verbatim["action"] == "save_confirm" and verbatim["memory_text"] == verbatim_body,
          "Expliziter 1:1-Auftrag entfernt nur die Befehlshülle")

    print("PASS: Regel-/Memory-Routingmatrix ist in beiden Prioritätsrichtungen abgesichert")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL: {exc}")
        raise SystemExit(1)

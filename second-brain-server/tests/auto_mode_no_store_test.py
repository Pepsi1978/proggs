#!/usr/bin/env python3
"""Offline-Matrixtest: Der Automatik-Modus liest nur (Web + Gedächtnis) und speichert NIE.

Franks Grundsatz (2026-07-12, Vorfall 20:30 Uhr "Typenbezeichnung des Autos"):
Der Hauptagent darf im Automatik-Modus weder speichern noch Speicher-Rückfragen stellen.
Speichern läuft ausschließlich über die Diskette (context_mode=save) bzw. über explizite
UI-Signale (Dashboard-Titel/Kategorie, Bearbeiten-Stift mit memory_edit).

Testtechnik wie memory_routing_matrix_test.py: die ECHTEN Funktionen aus agent/app.py werden
per AST in einen gemockten Namespace geladen — kein Server, kein LLM, deterministisch.
"""
from __future__ import annotations

import ast
import contextlib
import importlib.util
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "agent" / "app.py"
APP_SOURCE = APP_PATH.read_text(encoding="utf-8")

STORE_ACTIONS = {"save_confirm", "save_input", "store", "store_clarify",
                 "memory_edit_confirm", "memory_updated"}


def load_rules():
    path = ROOT / "agent" / "rules.py"
    spec = importlib.util.spec_from_file_location("auto_mode_rules", path)
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
        "_is_save_input_opener", "_is_definite_save_request", "_looks_like_save_request",
        "_pending_confirmation_intent", "_is_recall_request", "_process_rule_turn", "_process_turn",
    }
    selected = []
    for node in ast.parse(APP_SOURCE).body:
        if isinstance(node, ast.Assign):
            targets = {t.id for t in node.targets if isinstance(t, ast.Name)}
            if targets & assignment_names:
                selected.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name in function_names:
            selected.append(node)

    store_calls: list[str] = []

    def _record_store(*args, **kwargs):
        store_calls.append("store")
        return {"reply": "gespeichert", "action": "store", "stored": True, "pending": None}

    namespace = {
        "re": re,
        "rules": load_rules(),
        "MEMORY_DISTILL_MAX_CHARS": 4000,
        "CONV_CATEGORY": "Gespräche",
        "ACTIVE_PROFILE_PARALLEL": "parallel",
        "ENTITY_ENABLED": False,
        "_STORE_CALLS": store_calls,
        "_perf_mark": lambda *args, **kwargs: None,
        "_perf_span": lambda *args, **kwargs: contextlib.nullcontext(),
        "_maybe_compress_history": lambda session: None,
        "brain_categories": lambda: [],
        "load_registry": lambda: [],
        "add_registry_category": lambda category: None,
        "_store_final": _record_store,
        "_update_memory_final": _record_store,
        "_do_store": _record_store,
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
        "_projektstand_recall": lambda *args, **kwargs: None,
        "_full_category_answer": lambda *args, **kwargs: None,
        "_category_count_answer": lambda *args, **kwargs: None,
        "_is_trivial_greeting": lambda text: False,
        "_toolagent_answer": lambda *args, **kwargs: {"reply": "NORMAL", "action": "smalltalk", "pending": None},
        "_auto_parallel_answer": lambda *args, **kwargs: {"reply": "PARALLEL", "action": "smalltalk", "pending": None},
        "_log": lambda *args, **kwargs: None,
        "logging": __import__("logging"),
    }
    exec(compile(ast.Module(body=selected, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def route_stub(ns):
    """Router-Stub, der sich wie der heutige LLM-Router im schlimmsten Fall verhält:
    alles mit Speicher-Wortlaut wird als save klassifiziert."""
    def _route(session, text, pending, context_mode="auto", context_prompt=""):
        looks_savey = ns["_is_definite_save_request"](text) or "speicher" in text.lower()
        return {"intent": "save" if looks_savey else "smalltalk",
                "quote": text if looks_savey else "",
                "query": "", "web_query": "", "reply": ""}
    return _route


def main() -> int:
    ns = load_namespace()
    process = ns["_process_turn"]
    ns["hauptagent_route"] = route_stub(ns)

    def run(text: str, pending=None, session=None, **kwargs):
        ns["_STORE_CALLS"].clear()
        return process(session if session is not None else {}, text, pending, **kwargs)

    # --- 1) REGRESSION Vorfall 2026-07-12, 20:31/20:33 Uhr (wortgleich) ------------------
    incident_texts = (
        "Da steht doch alles im Gedächtnis drin. Warum findest du das nicht? Verstehe ich nicht.",
        "Es steht doch etwas über mein Auto drin. Da stehen auch diese wichtigen Daten drin.",
        "Welche Typenbezeichnung hat mein Auto? Wenn ich Teile einkaufe, muss ich das doch immer angeben, diesen sechsstelligen Code da.",
        "Ja, natürlich meine ich genau die und die habe ich irgendwo abgespeichert.",
    )
    for text in incident_texts:
        out = run(text)
        check(out["action"] not in STORE_ACTIONS and not ns["_STORE_CALLS"],
              f"Auto-Frage darf NIE im Speicherpfad landen: {text[:60]!r} -> {out['action']}")

    # --- 2) Auch ECHTE Speicherbefehle lösen im Automatik-Modus keinen Speicherdialog aus --
    for text in (
        "Speichere bitte: Mein Lieblingsessen ist Pizza.",
        "Merk dir bitte, dass ich Dienstag Zahnarzt habe.",
        "Kannst du etwas von mir abspeichern im Gedächtnis?",
        "Speicher: Der Code für die Garage ist 4711.",
    ):
        out = run(text)
        check(out["action"] not in STORE_ACTIONS and not ns["_STORE_CALLS"],
              f"Speicherbefehl im Auto-Modus darf keinen Speicherdialog öffnen: {text[:50]!r} -> {out['action']}")
    hint = run("Speichere bitte: Mein Lieblingsessen ist Pizza.")
    check("diskette" in hint["reply"].casefold(),
          "Expliziter Speicherbefehl im Auto-Modus erklärt den Diskette-Weg")

    # --- 3) Offene Speicher-Pendings werden im Auto-Turn verworfen (kein Blind-Store) ------
    stale = {"mode": "save_confirm", "quote": "DARF NICHT GESPEICHERT WERDEN",
             "category": "privat", "categories": ["privat"], "title": "Alt"}
    for answer in ("ja", "nein", "Wo laufe ich am liebsten?"):
        out = run(answer, pending=dict(stale), context_mode="auto")
        check(out["action"] not in STORE_ACTIONS and not ns["_STORE_CALLS"],
              f"Auto-Turn auf altes save_confirm darf nicht speichern: {answer!r} -> {out['action']}")
    out = run("ja", pending={"mode": "save_input", "category": "", "title": "", "store_timestamp": False})
    check(out["action"] not in STORE_ACTIONS and not ns["_STORE_CALLS"],
          "Auto-Turn auf altes save_input darf keinen wortwörtlichen Store binden")

    # --- 4) Der Diskette-Flow bleibt zu 100% erhalten --------------------------------------
    saved = run("Mein Auto ist ein Toyota C-HR, HSN 5013, TSN AKY.", context_mode="save")
    check(saved["action"] == "save_confirm", "Diskette erzeugt weiterhin die Speicher-Rückfrage")
    confirmed = run("ja", pending=saved["pending"], context_mode="save")
    check(confirmed["action"] == "store" and confirmed.get("stored"),
          "Diskette + ja speichert weiterhin über den Speicheragenten")
    cancelled = run("nein", pending=saved["pending"], context_mode="save")
    check(cancelled["action"] == "cancel", "Diskette + nein bricht weiterhin sauber ab")

    # --- 5) Explizite UI-Speichersignale bleiben auch im Auto-Modus gültig -----------------
    ui_save = run("Dieser Text kommt aus dem Dashboard-Sendebereich.",
                  category="Theorie", title="Bewusster Eintrag")
    check(ui_save["action"] == "save_confirm",
          "Dashboard-Titel/Kategorie (explicit_save) bleibt ein gültiges Speichersignal")
    edited = run("Neuer Inhalt nach Stift-Bearbeitung", context_mode="save",
                 session={"last_memory": {"doc_id": "doc-1", "memory_text": "alt"}},
                 memory_edit=True, memory_doc_id="doc-1", memory_categories=["privat"])
    check(edited["action"] == "store", "Bearbeiten-Stift (memory_edit, Diskette) speichert weiterhin")

    # --- 6) Lupe bleibt hart read-only (Regression) -----------------------------------------
    lupe = run("ja", pending=dict(stale), context_mode="search")
    check(lupe["action"] not in STORE_ACTIONS and not ns["_STORE_CALLS"],
          "Lupe kann weiterhin keinen offenen Speicherentwurf bestätigen")

    # --- 7) last_memory-Folgedialoge gehören zur Diskette, nicht zur Automatik --------------
    followup_calls: list[str] = []
    process.__globals__["_memory_followup_candidate"] = (
        lambda *args, **kwargs: followup_calls.append("called") or None)
    run("Ändere den Titel auf Neues Auto",
        session={"last_memory": {"doc_id": "doc-1", "memory_text": "alt"}}, context_mode="auto")
    check(not followup_calls,
          "Automatik öffnet keinen Bearbeitungs-Folgedialog am letzten Eintrag")
    run("Ändere den Titel auf Neues Auto",
        session={"last_memory": {"doc_id": "doc-1", "memory_text": "alt"}}, context_mode="save")
    check(followup_calls, "Diskette behält den Bearbeitungs-Folgedialog")
    process.__globals__["_memory_followup_candidate"] = lambda *args, **kwargs: None

    # --- 8) Recall-Heuristik: Beschwerden über das Gedächtnis sind NIE Speicherbefehle ------
    for text in (
        "Da steht doch alles im Gedächtnis drin. Warum findest du das nicht?",
        "Es steht doch etwas über mein Auto drin. Kannst du nicht noch mal suchen?",
    ):
        check(ns["_is_recall_request"](text), f"Gedächtnis-Nachfrage wird als Recall erkannt: {text[:40]!r}")

    print("PASS: Automatik-Modus ist hart read-only; Diskette/Lupe/UI-Signale unverändert")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL: {exc}")
        raise SystemExit(1)

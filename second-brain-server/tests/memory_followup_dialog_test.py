#!/usr/bin/env python3
"""Offline-Regressionstest fuer den nachbearbeitbaren Cortex-Speicherdialog."""
from __future__ import annotations

import ast
import json
import logging
import re
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "agent" / "app.py"
APP_SOURCE = APP_PATH.read_text(encoding="utf-8")


def load_namespace() -> dict:
    names = {
        "MEMORY_FOLLOWUP_SYSTEM", "_MEMORY_FOLLOWUP_RE", "_memory_followup_candidate",
        "_memory_receipt", "_apply_memory_outcome", "_pending_confirmation_intent",
        "_additional_title",
    }
    selected = []
    for node in ast.parse(APP_SOURCE).body:
        if isinstance(node, ast.Assign):
            if any(isinstance(target, ast.Name) and target.id in names for target in node.targets):
                selected.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name in names:
            selected.append(node)
    namespace = {
        "json": json, "logging": logging, "re": re,
        "ROLE_MODELS": {"speicher": "test"},
        "_extract_json": lambda value: value,
        "_log": lambda *args, **kwargs: None,
        "_now_local": lambda: datetime(2026, 7, 11, 13, 25, 0),
    }
    exec(compile(ast.Module(body=selected, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    ns = load_namespace()
    followup = ns["_memory_followup_candidate"]
    memory = {
        "doc_id": "doc-1", "title": "Neue Amazfit T-Rex 3 Pro gekauft",
        "categories": ["Geräte"], "memory_text": "Ich habe mir die Amazfit T-Rex 3 Pro gekauft.",
        "stored_text": "Gespeichert am: jetzt\n\nIch habe mir die Amazfit T-Rex 3 Pro gekauft.",
        "store_timestamp": True,
    }

    ns["llm_generate"] = lambda *args, **kwargs: json.dumps({
        "action": "show", "text": memory["memory_text"], "title": memory["title"],
        "categories": memory["categories"], "reply": "",
    }, ensure_ascii=False)
    shown = followup("Zitiere mir den Eintrag so, wie er gespeichert ist.", memory)
    check(shown["action"] == "memory_show" and shown["memory_text"] == memory["memory_text"] and
          memory["stored_text"] in shown["reply"],
          "Aktueller Wortlaut bleibt im Folgedialog exakt abrufbar")

    ns["llm_generate"] = lambda *args, **kwargs: json.dumps({
        "action": "update", "text": memory["memory_text"], "title": memory["title"],
        "categories": ["Geräte", "Pokoboot"], "reply": "",
    }, ensure_ascii=False)
    proposed = followup("Füge den Eintrag noch der Kategorie Pokoboot hinzu.", memory)
    check(proposed["action"] == "memory_edit_confirm", "Freie Nachbearbeitung erzeugt eine neue Bestätigung")
    check(proposed["categories"] == ["Geräte", "Pokoboot"], "Zusatzkategorie erhält die vorhandene Kategorie")
    check(proposed["pending"]["doc_id"] == "doc-1" and proposed["pending"]["memory_text"] == memory["memory_text"],
          "Bestätigung bleibt mit demselben Eintrag und vollständigem Text verbunden")

    ns["llm_generate"] = lambda *args, **kwargs: json.dumps({
        "action": "none", "text": "", "title": "", "categories": [], "reply": "",
    })
    check(followup("Wie wird das Wetter?", memory) is None,
          "Unverwandter Dialog wird nicht in eine Speicheränderung umgedeutet")
    check(followup("Welchen Titel hat das Buch Dune?", memory) is None,
          "Normale Fragen mit dem Wort Titel laufen nach einem Store weiter durch den Hauptagenten")
    pending_memory = {**memory, "mode": "save_confirm"}
    pending_none = followup("Ich möchte erst noch über den Titel nachdenken.", pending_memory)
    check(pending_none is not None and pending_none["pending"] == pending_memory,
          "Eine Diskussion während eines offenen Entwurfs verliert den Pending-Zustand nicht")

    unique_title = ns["_additional_title"]("Amazfit", [{"title": "Amazfit"}])
    check(unique_title != "Amazfit" and unique_title.startswith("Amazfit · zusätzlich"),
          "Zusätzlich speichern kann wegen eines identischen Titels keine Dublette ersetzen")

    receipt = ns["_memory_receipt"]("store", memory["title"], ["Geräte", "Pokoboot"],
                                     memory["memory_text"], "doc-1", memory["stored_text"])
    check("Kategorie: „Geräte“, „Pokoboot“" in receipt["reply"] and
          memory["stored_text"] in receipt["reply"] and receipt["memory_editable"],
          "Quittung nennt alle Kategorien, Titel und den vollständigen exakten Servertext")

    session = {}
    receipt["store_timestamp"] = True
    ns["_apply_memory_outcome"](session, receipt)
    check(session["last_memory"]["doc_id"] == "doc-1" and session["last_memory"]["categories"] == ["Geräte", "Pokoboot"],
          "Letzter Speichereintrag bleibt fuer spätere Turns im Sessionzustand erhalten")

    confirm = ns["_pending_confirmation_intent"]
    check(confirm("ja", {"mode": "memory_edit_confirm"}) == "confirm_yes",
          "Natürliche Folgeänderung kann deterministisch bestätigt werden")

    required = (
        "memory_edit: bool", "memory_doc_id", "memory_categories",
        "_memory_followup_candidate(user_text, session[\"last_memory\"])",
        "_update_memory_final", '"last_memory": session.get("last_memory")',
        '"stored_text": outcome.get("stored_text")',
        "if title.strip() and edit_categories:",
    )
    for needle in required:
        check(needle in APP_SOURCE, f"Speicher-Folgedialog nicht vollständig verdrahtet: {needle}")

    print("PASS: Speicherentwurf und gespeicherter Eintrag bleiben zitier-, editier- und mehrkategoriefähig")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL: {exc}")
        raise SystemExit(1)

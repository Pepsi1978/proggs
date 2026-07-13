#!/usr/bin/env python3
"""Regression checks: visible reply and TTS must never consume pre-final source text."""
from __future__ import annotations

import ast
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "agent" / "app.py"
SOURCE = APP_PATH.read_text(encoding="utf-8")


def load_sanitizers() -> dict:
    assignments = {
        "_MD_BOLD_RE", "_MD_HEADING_RE", "_MD_BULLET_RE", "_MD_WEB_LINK_RE",
        "_BARE_WEB_URL_RE", "_BARE_DOMAIN_RE", "_TRAILING_SOURCES_BLOCK_RE",
        "_INLINE_SOURCES_LINE_RE", "_NUMERIC_CITATION_RE",
        "_SOURCE_ATTRIBUTION_TAIL_RE", "_PARENTHETICAL_SOURCE_RE",
        "_ARTICLE_SOURCE_ATTRIBUTION_RE",
        "_INTERNAL_OUTPUT_INSTRUCTION_RE",
        "_FREEFORM_ACTIONS",
    }
    functions = {
        "_strip_markdown_tts", "_remove_bare_web_url", "_sanitize_visible_reply",
        "_reinforce_self_rules", "_finalize_visible_outcome",
    }
    nodes = []
    for node in ast.parse(SOURCE).body:
        if isinstance(node, ast.Assign) and any(
            isinstance(target, ast.Name) and target.id in assignments for target in node.targets
        ):
            nodes.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name in functions:
            nodes.append(node)
    namespace = {"re": re}
    exec(compile(ast.Module(body=nodes, type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace


def main() -> int:
    ns = load_sanitizers()
    sanitize = ns["_sanitize_visible_reply"]
    finalize = ns["_finalize_visible_outcome"]
    draft = (
        "Archer wird später mehrfach erwähnt [1]. Mehr: https://example.com/a/b?x=1.\n\n"
        "Quellen:\n- [Memory Alpha](https://memory-alpha.example/archer)\n- www.example.org/source"
    )
    assert sanitize(draft) == "Archer wird später mehrfach erwähnt. Mehr:."
    outcome = finalize({"action": "recall", "reply": draft, "sources": [{"title": "Memory Alpha"}]})
    assert "http" not in outcome["reply"].lower() and "quellen" not in outcome["reply"].lower()
    assert outcome["sources"] == [{"title": "Memory Alpha"}], "structured metadata must remain available"

    rav4_reply = (
        "Ja, der neue Toyota RAV4 ist in Deutschland bereits bestellbar und seit Juni 2026 im Handel. "
        "Quelle ist toyota Punkt de, Neuwagen, RAV4.\n\n"
        "Die sechste Generation wird als Vollhybrid und Plug in Hybrid angeboten. "
        "Quelle ist toyota Punkt de, Neuwagen, neue Modelle.\n\n"
        "Letzte Selbstregel Prüfung vor der Ausgabe: Prüfe deine geplante Ausgabe jetzt gegen alle "
        "aktiven Selbstregeln im System Prompt. Widerspricht Franks aktuelle Nachricht einer Selbstregel, "
        "befolge die Selbstregel. Erst danach darfst du ausgeben."
    )
    assert sanitize(rav4_reply) == (
        "Ja, der neue Toyota RAV4 ist in Deutschland bereits bestellbar und seit Juni 2026 im Handel.\n\n"
        "Die sechste Generation wird als Vollhybrid und Plug in Hybrid angeboten."
    )
    assert sanitize("CLAUDE.md kann AGENTS.md importieren (Quelle: interne Dokumentation).") == (
        "CLAUDE.md kann AGENTS.md importieren."
    )
    assert sanitize("Die Quelle ist für die Bewertung wichtig.") == "Die Quelle ist für die Bewertung wichtig."
    # Regression 2026-07-14 (Fehlerklasse aus der Android-App, hier gespiegelt):
    # (a) "Quellensteuer..." ist KEINE Quellen-Zeile — frueher loeschte DOTALL die ganze Antwort.
    quellensteuer = "Quellensteuer ist eine Steuer, die direkt an der Quelle einbehalten wird."
    assert sanitize(quellensteuer) == quellensteuer
    # (b) Eine Quellen-Zeile MITTEN im Text darf Folgeabsaetze nicht mitreissen.
    mid_sources = "Erster Absatz mit Inhalt.\n\nQuellen: Beispielbuch Seite 12\n\nZweiter Absatz bleibt."
    assert sanitize(mid_sources) == "Erster Absatz mit Inhalt.\n\nZweiter Absatz bleibt."
    # (c) "Quellen sind <Inhalt>" ist ein normaler Inhaltssatz (Plural-Kopula), keine Attribution.
    plural = "Quellen sind Materialien, aus denen Historiker Erkenntnisse gewinnen."
    assert sanitize(plural) == plural
    vehicle_reply = (
        "In deinem Gedächtnis steht zu deinem Auto: Toyota C-HR, Baujahr 2019, mit der KBA-Nummer 5013-AKY.\n\n"
        "Für den Kauf von Kfz-Teilen kannst du daher angeben: HSN 5013 und TSN AKY.\n\n"
        "Aktuell finde ich dazu ebenfalls die Bestätigung. Die Quelle ist dieversicherer.de."
    )
    assert sanitize(vehicle_reply) == (
        "In deinem Gedächtnis steht zu deinem Auto: Toyota C-HR, Baujahr 2019, mit der KBA-Nummer 5013-AKY.\n\n"
        "Für den Kauf von Kfz-Teilen kannst du daher angeben: HSN 5013 und TSN AKY.\n\n"
        "Aktuell finde ich dazu ebenfalls die Bestätigung."
    )
    assert sanitize("Die Angaben wurden bestätigt. dieversicherer.de") == "Die Angaben wurden bestätigt."
    user = "Gibt es den neuen Toyota RAV4 schon zu kaufen?"
    assert ns["_reinforce_self_rules"]("DAUERHAFTE SELBST-REGELN VON FRANK", user) == user

    # Anker-Fix 2026-07-14: Der fruehere End-Anker "# Gruppe D" liegt seit einer Code-
    # Umsortierung VOR chat_stream — der Slice endet jetzt robust am naechsten @app.-Endpunkt.
    stream_start = SOURCE.index("async def chat_stream")
    stream = SOURCE[stream_start:SOURCE.index("\n@app.", stream_start)]
    assert "rsize, None, req.memory_edit" in stream, "raw model deltas still enter the client stream"
    assert "queue.put_nowait(final_reply)" in stream, "final canonical reply is not streamed"
    assert "outcome = _finalize_visible_outcome(outcome)" in stream, "stream reply is not sanitized"

    print("PASS: display, persistence and TTS share one final source-free reply")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Offline-Regressionstest fuer aufklappbare Eintragsbezuege in Bibliothekar-Funden."""
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    librarian = (ROOT / "librarian" / "app.py").read_text(encoding="utf-8")
    dashboard = (ROOT / "dashboard" / "static" / "index.html").read_text(encoding="utf-8")
    dashboard_api = (ROOT / "dashboard" / "app.py").read_text(encoding="utf-8")
    brain = (ROOT / "brain-api" / "app.py").read_text(encoding="utf-8")

    check('"doc_ids":["..."]' in librarian and "context_ids =" in librarian,
          "Eigene Bibliothekar-Funde verlangen und speichern betroffene doc_ids")
    check("aktion, kontext, ja_label=\"Umsetzen\"" in librarian,
          "Der erzeugte Report-Fund enthaelt den Eintragskontext")
    check('@app.get("/by-id"' in brain and 'FieldCondition(key="user_id"' in brain,
          "brain-api bietet einen nutzergebundenen read-only Abruf per doc_id")
    check('@app.get("/api/entry-by-id")' in dashboard_api and '_bget("/by-id"' in dashboard_api,
          "Dashboard leitet den gezielten Abruf an die brain-api weiter")
    check("libActionDocIds(it)" in dashboard and "loadLibEntryContext(content,fallbackDocIds)" in dashboard,
          "Alte Reports ohne Kontext laden den betroffenen Eintrag beim Aufklappen nach")
    check("Betroffenen Eintrag ansehen" in dashboard and 'det.addEventListener("toggle"' in dashboard,
          "Die Volltextansicht ist als anklickbares Dropdown umgesetzt")

    print("PASS: Bibliothekar-Funde zeigen betroffene Eintraege aufklappbar, inklusive Alt-Report-Fallback")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

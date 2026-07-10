#!/usr/bin/env python3
"""Produktionsnaher Live-Eval fuer die additive Bibliothekar-Kategorien-Union.

Laeuft im librarian-Container gegen die echte brain-api, aber ausschliesslich unter
dem geschuetzten Test-Nutzer ``eval-librarian-category-union``. Der Test raeumt
vorher und im ``finally`` per ``/purge`` auf; Franks echtes Gehirn bleibt unberuehrt.

VPS-Aufruf:
    docker compose run --rm -e SB_USER_ID=eval-librarian-category-union \
      -v /opt/second-brain/tests:/tests:ro librarian \
      python /tests/librarian_category_live_eval.py
"""
from __future__ import annotations

import os
import importlib
import sys
import time
import traceback


for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:
        pass


TEST_USER = "eval-librarian-category-union"
TEST_TITLE = "EVAL Bibliothekar Kategorien-Union"
UNCATEGORIZED_TITLE = "EVAL Bibliothekar Ohne Kategorie"
BEFORE = ["Eval/A", "Eval/B", "Eval/C", "Eval/D"]
ADDED = "Eval/E"
FIRST_CATEGORY = "Eval/Erste"

# Muss VOR dem Import gesetzt werden: librarian/app.py liest USER_ID beim Modulstart.
os.environ["SB_USER_ID"] = TEST_USER
sys.path.insert(0, "/app")

librarian = importlib.import_module("app")  # Container-Arbeitsverzeichnis /app


def purge() -> dict:
    response = librarian._HTTP.post(
        f"{librarian.BRAIN_URL}/purge",
        json={"user_id": TEST_USER},
        headers=librarian.HEADERS,
        timeout=60.0,
    )
    response.raise_for_status()
    return response.json()


def wait_for_doc(doc_id: str, timeout: float = 30.0) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if any(item.get("doc_id") == doc_id for item in librarian.brain_list()):
            return
        time.sleep(0.5)
    raise AssertionError("Testeintrag erschien nicht rechtzeitig in /list")


def main() -> int:
    if librarian.USER_ID != TEST_USER or not librarian.SB_API_KEY:
        print("KONFIG-FEHLER: isolierter Eval-Nutzer oder SB_API_KEY fehlt.", file=sys.stderr)
        return 2

    purge()
    try:
        stored = librarian.brain_store(
            "Produktionsnaher Test fuer vier bestehende und eine zusaetzliche Kategorie.",
            TEST_TITLE,
            BEFORE[0],
            categories=BEFORE,
        )
        doc_id = (stored.get("doc_id") or "").strip()
        if not doc_id:
            raise AssertionError("/store lieferte keine doc_id")
        wait_for_doc(doc_id)

        result = librarian.brain_add_category(doc_id, ADDED)
        expected = BEFORE + [ADDED]
        if result.get("categories") != expected:
            raise AssertionError(f"4 -> 5 fehlgeschlagen: {result.get('categories')!r}")

        for category in expected:
            items = librarian.brain_by_category(category)
            if not any(item.get("doc_id") == doc_id for item in items):
                raise AssertionError(f"Eintrag fehlt in Kategorie {category!r}")

        duplicate = librarian.brain_add_category(doc_id, "eval/b")
        if duplicate.get("categories") != expected:
            raise AssertionError("Case-insensitives Duplikat veraenderte die Kategorie-Liste")

        uncategorized = librarian.brain_store(
            "Produktionsnaher Test fuer einen Eintrag ohne bisherige Kategorie.",
            UNCATEGORIZED_TITLE,
            "",
        )
        uncategorized_id = (uncategorized.get("doc_id") or "").strip()
        if not uncategorized_id:
            raise AssertionError("/store lieferte fuer den kategorielosen Eintrag keine doc_id")
        wait_for_doc(uncategorized_id)
        if librarian.brain_get_categories(uncategorized_id) != []:
            raise AssertionError("Neuer Testeintrag war nicht kategorielos")

        first = librarian.brain_add_category(uncategorized_id, FIRST_CATEGORY)
        if first.get("categories") != [FIRST_CATEGORY]:
            raise AssertionError(f"0 -> 1 fehlgeschlagen: {first.get('categories')!r}")
        if not any(item.get("doc_id") == uncategorized_id
                   for item in librarian.brain_by_category(FIRST_CATEGORY)):
            raise AssertionError("Zuvor kategorieloser Eintrag fehlt in seiner ersten Kategorie")

        print("PASS: echter Bibliothekar-Pfad bewahrt 4 -> 5 und ergaenzt 0 -> 1 Kategorie.")
        return 0
    except Exception:  # noqa: BLE001 - Test soll den kompletten Diagnose-Stack zeigen
        traceback.print_exc()
        return 1
    finally:
        cleaned = purge()
        print(f"Aufgeraeumt: {cleaned.get('entries', 0)} isolierte Testeintraege entfernt.")


if __name__ == "__main__":
    sys.exit(main())

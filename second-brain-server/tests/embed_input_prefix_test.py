#!/usr/bin/env python3
"""Reiner Unit-Test der embed_input-Praefix-Logik — kein Netz, kein Qdrant/Gemini-Call.
Prueft, dass embed_input das offizielle gemini-embedding-2-Praefix 'title: … | text: …' baut
(Kategorien im title-Teil, 'none' ohne Titel). Der -001-Fallback wurde 2026-07-08 ausgebaut.

Lauf (bevorzugt im Container, wo fastapi/google-genai/qdrant-client installiert sind):
    docker compose run --rm brain-api python3 /app/tests/embed_input_prefix_test.py
oder lokal, falls die Abhaengigkeiten vorhanden sind:
    python3 tests/embed_input_prefix_test.py
Exit 0 = PASS, 1 = FAIL.
"""
from __future__ import annotations

import importlib.util
import os
import sys
from pathlib import Path

# Stdout/Streams auf UTF-8 (Windows-Konsole ist sonst cp1252 -> Crash bei '·'/Umlauten in Fehlermeldungen;
# python-windows Kurzcheck #7). Auf Linux/Container ohnehin no-op.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:  # noqa: BLE001 — aeltere/aussergewoehnliche Streams: einfach weiter
        pass


def _load(model: str):
    """app.py mit gesetzter Modell-Env frisch importieren. _init_store() faengt Verbindungsfehler
    intern in init_error ab -> der Import bleibt OK, auch ohne erreichbares Qdrant/Gemini."""
    os.environ["GEMINI_EMBED_MODEL"] = model
    os.environ["SB_EMBED_DIMS"] = "3072" if model.startswith("gemini-embedding-2") else "1536"
    os.environ.setdefault("GEMINI_API_KEY", "x")
    os.environ.setdefault("SB_API_KEY", "y" * 32)
    os.environ.setdefault("QDRANT_API_KEY", "z")
    app_path = Path(__file__).resolve().parent.parent / "brain-api" / "app.py"
    modname = "appmod_" + model.replace("-", "_").replace(".", "_")
    spec = importlib.util.spec_from_file_location(modname, str(app_path))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def main() -> int:
    m = _load("gemini-embedding-2")
    got = m.embed_input("Titel A", ["Programmierung/Rules"], "Hallo")
    assert got == "title: Titel A · Programmierung > Rules | text: Hallo", got
    assert m.embed_input(None, [], "Nur Text") == "title: none | text: Nur Text"
    assert m.embed_input("", ["Alltag"], "X") == "title: Alltag | text: X"
    assert m.embed_input("Nur Titel", [], "Y") == "title: Nur Titel | text: Y"

    print("PASS: embed_input Praefix-Logik korrekt (gemini-embedding-2)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

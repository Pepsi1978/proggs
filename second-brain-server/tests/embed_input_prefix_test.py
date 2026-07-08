#!/usr/bin/env python3
"""Reiner Unit-Test der modell-abhaengigen Praefix-Logik (embed_input / IS_EMBED2) — kein Netz,
kein Qdrant/Gemini-Call. Prueft, dass gemini-embedding-2 das offizielle 'title: … | text: …'-Praefix
baut und gemini-embedding-001 das alte '[Titel: … | Kategorien: …]'-Format behaelt.

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
    e2 = _load("gemini-embedding-2")
    assert e2.IS_EMBED2 is True, "IS_EMBED2 sollte fuer gemini-embedding-2 True sein"
    got = e2.embed_input("Titel A", ["Programmierung/Rules"], "Hallo")
    assert got == "title: Titel A · Programmierung > Rules | text: Hallo", got
    assert e2.embed_input(None, [], "Nur Text") == "title: none | text: Nur Text"
    assert e2.embed_input("", ["Alltag"], "X") == "title: Alltag | text: X"

    o1 = _load("gemini-embedding-001")
    assert o1.IS_EMBED2 is False, "IS_EMBED2 sollte fuer gemini-embedding-001 False sein"
    assert o1.embed_input("Titel A", [], "Hallo") == "[Titel: Titel A]\n\nHallo"
    assert o1.embed_input(None, [], "Nur Text") == "Nur Text"

    print("PASS: embed_input Praefix-Logik korrekt (gemini-embedding-2 + gemini-embedding-001)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Offline-Regressionstest: Bibliothekar-Funde laden ihre echten Ziel-Eintraege per doc_id."""
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    dashboard = (ROOT / "dashboard" / "app.py").read_text(encoding="utf-8")
    brain = (ROOT / "brain-api" / "app.py").read_text(encoding="utf-8")
    frontend = (ROOT / "dashboard" / "static" / "index.html").read_text(encoding="utf-8")

    assert '@app.get("/by-id"' in brain
    assert 'FieldCondition(key="doc_id", match=MatchValue(value=doc_id))' in brain
    assert 'return _bget("/by-id", doc_id=doc_id, user_id=USER_ID)' in dashboard
    assert 'function libSourceEntries(it)' in frontend
    assert 'add({doc_id:action.doc_id});' in frontend
    assert 'libSourceDetails(source)' in frontend
    assert '"/api/entry?doc_id="+encodeURIComponent(source.doc_id)' in frontend
    print("PASS: Bibliothekar-Quellen sind per doc_id aufklappbar und laden den Volltext")


if __name__ == "__main__":
    main()

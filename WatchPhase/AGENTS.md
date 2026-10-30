# WatchPhase

Zifferblatt „Watch Phase“ für die Huawei Watch 3 (466 × 466), gebaut mit `bauen.py` ohne Theme Studio. Formatwissen: `best-practices/peripherie/huawei-watchface.md`.

- Version: Datei `VERSION`, je Commit in der letzten Stelle erhöhen.
- Bauen: `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt && .venv/bin/python bauen.py`. Ergebnis: `dist/WatchPhase.hwt`.
- Test: `tests/test_rundreise.py <entpackter Theme-Studio-Export>` vergleicht `watchface.bin` Byte für Byte.

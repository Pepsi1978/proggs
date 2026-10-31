# WatchPhase

Zifferblatt „Watch Phase“ für die Huawei Watch 3 (466 × 466), gebaut mit `bauen.py` ohne Theme Studio. Formatwissen: `best-practices/peripherie/huawei-watchface.md`.

- Version: Datei `VERSION`, je Commit in der letzten Stelle erhöhen.
- Bauen: `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt && .venv/bin/python bauen.py`. Ergebnis: `dist/WatchPhase.hwt`.
- SLAVA, EIGER SPEED EDITION, BERING (hell und schwarz), Bergmann 1956 und Gold: `.venv/bin/python klassiker.py`. Dieser Build fasst Watch Phase nicht an.
- Der Build läuft nur auf macOS, weil er die Systemschriften Times New Roman und Arial nutzt.
- Die Watch 3 braucht Version 1.1.1 und ein inneres Paket ohne `watchface.bin`. `hwt.watchface_bin` ist das GT-Format und wird nicht in die Datei gebaut.
- Test: `tests/test_rundreise.py <entpackter Theme-Studio-Export>` vergleicht nur das GT-Format `watchface.bin` Byte für Byte.

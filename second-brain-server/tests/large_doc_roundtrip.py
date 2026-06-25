#!/usr/bin/env python3
"""Regressionstest: SEHR grosse Dokumente werden 1:1 gespeichert UND 1:1 zurueckgegeben.

Hintergrund (Frank-Bug 2026-06-25): Ein langer Almanach (~18k Zeichen) kam nur zur HAELFTE
im Gehirn an. Bewiesene Root Cause (Direktive-#3-Debugging): ein STILLER `text[:8000]`-Slice
im dashboard /api/chat schnitt die Eingabe ab, BEVOR sie brain-api erreichte — brain-api
speicherte und lieferte danach treu nur die abgeschnittene Haelfte. Es war KEIN Abruf-Bug.

Dieser Test ist die dauerhafte Poka-Yoke-Absicherung (Direktive #3): Er beweist gegen die
LAUFENDE brain-api, dass ein sehr grosser Text (Default ~600.000 Zeichen ≈ 32x Franks
groesste reale Datei) Byte-fuer-Byte den Round-Trip ueberlebt. Schlaegt er fehl, ist
irgendwo wieder eine stille Kuerzung eingezogen — genau das soll NIE wieder unbemerkt bleiben.

Lauf (auf dem VPS, wo brain-api lokal erreichbar ist):
    BRAIN_URL=http://localhost:8000 SB_API_KEY=<key> python3 tests/large_doc_roundtrip.py
oder ueber WireGuard:
    BRAIN_URL=http://10.8.0.1:8000 SB_API_KEY=<key> python3 tests/large_doc_roundtrip.py

Env:
    BRAIN_URL          (Default http://localhost:8000)
    SB_API_KEY         (Pflicht — Bearer-Token von brain-api)
    LARGE_DOC_CHARS    (Default 600000)
    ROUNDTRIP_TIMEOUT  (Default 60 Sekunden — Qdrant-Index-/Persistenz-Delay abwarten)

Nutzt NUR die Standardbibliothek (urllib) -> laeuft ueberall ohne Extra-Pakete.
Raeumt hinterher HART auf (POST /purge) unter einem isolierten 'eval*'-Test-Nutzer, damit
Franks echtes Gehirn NIE beruehrt wird. Exit 0 = PASS, 1 = FAIL, 2 = Konfigurationsfehler.
"""
from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

# Stdout/Streams auf UTF-8 (Windows-Konsole ist sonst cp1252 -> Crash bei Umlauten;
# python-windows Best-Practice Kurzcheck #9). Auf Linux/VPS ohnehin no-op.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:  # noqa: BLE001 — aeltere/aussergewoehnliche Streams: einfach weiter
        pass

BRAIN_URL = os.getenv("BRAIN_URL", "http://localhost:8000").rstrip("/")
SB_API_KEY = os.getenv("SB_API_KEY", "")
LARGE_DOC_CHARS = int(os.getenv("LARGE_DOC_CHARS", "600000"))
ROUNDTRIP_TIMEOUT = int(os.getenv("ROUNDTRIP_TIMEOUT", "60"))

# Isolierter Test-Nutzer (MUSS mit 'eval' beginnen -> /purge erlaubt; nie 'frank').
TEST_USER = "eval-largedoc-roundtrip"
TEST_TITLE = "REGRESSIONSTEST Grosses Dokument (loeschbar)"
HEADERS = {"Authorization": f"Bearer {SB_API_KEY}", "Content-Type": "application/json"}


def _post(path: str, payload: dict, timeout: float = 180.0) -> dict:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(BRAIN_URL + path, data=data, headers=HEADERS, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read().decode("utf-8"))


def _get(path: str, timeout: float = 60.0) -> dict:
    req = urllib.request.Request(BRAIN_URL + path, headers=HEADERS, method="GET")
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read().decode("utf-8"))


def make_large_text(n_chars: int) -> str:
    """Deterministischer, gut pruefbarer Text mit echten Umlauten (UTF-8-Round-Trip) und
    fortlaufenden Nummern, damit ein fehlendes Stueck (z.B. nach Chunk 2) sofort auffaellt."""
    head = "[ANFANG-MARKE] Dies ist der Beginn eines sehr grossen Regressionstest-Dokuments.\n"
    blocks = [head]
    total = len(head)
    i = 0
    while total < n_chars - 80:
        line = (f"Block {i:07d}: Zeile mit Umlauten aeoeue ÄÖÜ ß — der Text muss WORTWOERTLICH "
                f"1:1 erhalten bleiben, auch ueber Chunk-Grenzen (4000 Zeichen) hinweg. "
                f"Pruefsumme-Anker #{i * 7 + 3}.\n")
        blocks.append(line)
        total += len(line)
        i += 1
    blocks.append(f"[ENDE-MARKE nach {i} Bloecken] letztes Zeichen ist ein Punkt.")
    return "".join(blocks)


def _cleanup() -> None:
    try:
        r = _post("/purge", {"user_id": TEST_USER})
        print(f"-> Aufgeraeumt: /purge {TEST_USER} -> {r.get('entries')} Eintrag/Eintraege geloescht.")
    except Exception as e:  # noqa: BLE001 — Aufraeumen darf den Testausgang nicht kippen
        print(f"WARNUNG: Aufraeumen fehlgeschlagen ({type(e).__name__}: {e}). "
              f"Manuell: POST /purge {{'user_id': '{TEST_USER}'}}.", file=sys.stderr)


def main() -> int:
    if not SB_API_KEY:
        print("FEHLER: SB_API_KEY nicht gesetzt — brain-api-Bearer-Token noetig.", file=sys.stderr)
        return 2

    text = make_large_text(LARGE_DOC_CHARS)
    n = len(text)
    print(f"-> Erzeuge Test-Dokument: {n} Zeichen (~{n / 18731:.0f}x Franks Loop-Datei).", flush=True)

    # 1) Speichern
    try:
        res = _post("/store", {"text": text, "user_id": TEST_USER, "title": TEST_TITLE})
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")[:500]
        print(f"FAIL: /store HTTP {e.code}: {body}", file=sys.stderr)
        return 1
    stored_chars = res.get("chars")
    print(f"-> /store ok: doc_id={res.get('doc_id')}, chunks={res.get('chunks')}, chars={stored_chars}", flush=True)
    if stored_chars != n:
        print(f"FAIL: /store meldet chars={stored_chars}, erwartet {n} — Eingabe wurde gekuerzt!", file=sys.stderr)
        _cleanup()
        return 1

    # 2) Zurueckholen (mit Geduld fuer Index-/Persistenz-Delay)
    deadline = time.time() + ROUNDTRIP_TIMEOUT
    got = None
    while time.time() < deadline:
        r = _get(f"/by-title?title={urllib.parse.quote(TEST_TITLE)}&user_id={TEST_USER}")
        if r.get("found") and r.get("text"):
            got = r["text"]
            break
        time.sleep(2.0)

    if got is None:
        print("FAIL: /by-title hat den Eintrag im Zeitfenster nicht gefunden.", file=sys.stderr)
        _cleanup()
        return 1

    # 3) Byte-genau vergleichen
    print(f"-> /by-title zurueck: {len(got)} Zeichen (erwartet {n}).", flush=True)
    if len(got) == n and got == text:
        print("PASS: Sehr grosses Dokument 1:1 gespeichert UND 1:1 zurueckgegeben (Round-Trip vollstaendig).")
        _cleanup()
        return 0

    if len(got) != n:
        print(f"FAIL: Laenge weicht ab — zurueck {len(got)}, erwartet {n} (fehlend: {n - len(got)}).", file=sys.stderr)
    else:
        diff = next((k for k in range(n) if got[k] != text[k]), n)
        print(f"FAIL: Inhalt weicht ab — erste Abweichung bei Zeichen {diff}.", file=sys.stderr)
        print(f"  erwartet: ...{text[max(0, diff - 30):diff + 30]!r}...", file=sys.stderr)
        print(f"  bekommen: ...{got[max(0, diff - 30):diff + 30]!r}...", file=sys.stderr)
    _cleanup()
    return 1


if __name__ == "__main__":
    sys.exit(main())

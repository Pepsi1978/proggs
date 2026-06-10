# -*- coding: utf-8 -*-
"""
FA5 / X11 — Angleich der json_key_*/json_value_*-Strings in values-mr/strings.xml
an die romanisierte Form aus values-hi/strings.xml.

Technischer Wert-Angleich (KEINE Uebersetzung): hi nutzt bereits romanisierte,
parse-sichere Werte. Wir uebernehmen sie 1:1 fuer die gleichen Keys in mr.

Strategie:
- hi-Werte per ElementTree extrahieren (nur json_key_/json_value_-Keys).
- mr-Datei als ROH-Text laden, pro Key gezielt per Regex (auf den Key-Namen
  gematcht) den Element-Inhalt ersetzen. Formatierung/Einrueckung der restlichen
  Datei bleibt komplett unveraendert. KEIN ElementTree-Rewrite der ganzen Datei.
- Atomar schreiben (temp + os.replace, encoding='utf-8').
- Danach die ganze mr-Datei per ElementTree validieren.
- NUR json_key_*/json_value_*-Keys werden angefasst.
"""
import os
import re
import json
import tempfile
import xml.etree.ElementTree as ET

APP = os.path.expanduser("~/proggs/BestJournalAndroid")
HI = os.path.join(APP, "app/src/main/res/values-hi/strings.xml")
MR = os.path.join(APP, "app/src/main/res/values-mr/strings.xml")
CKPT = os.path.join(APP, ".android-shield/worker-checkpoints/fa5.jsonl")
OUT = os.path.join(APP, ".android-shield/worker-outputs/phase2-fa5.json")

PREFIXES = ("json_key_", "json_value_")


def log_ckpt(stage, detail):
    os.makedirs(os.path.dirname(CKPT), exist_ok=True)
    rec = {"stage": stage, "detail": detail}
    with open(CKPT, "a", encoding="utf-8") as f:
        f.write(json.dumps(rec, ensure_ascii=False) + "\n")


def xml_escape(text):
    """Escape fuer XML-Textinhalt. hi-Werte sind reine ASCII-Identifier,
    aber wir escapen sicherheitshalber & < >."""
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def extract_hi_values():
    """Liefert {key: roher_textwert} fuer alle json_key_/json_value_-Keys aus hi."""
    tree = ET.parse(HI)
    root = tree.getroot()
    out = {}
    for el in root.findall("string"):
        name = el.get("name", "")
        if name.startswith(PREFIXES):
            out[name] = el.text if el.text is not None else ""
    return out


def main():
    log_ckpt("start", {"hi": HI, "mr": MR})

    hi_vals = extract_hi_values()
    log_ckpt("hi_extracted", {"count": len(hi_vals)})

    with open(MR, "r", encoding="utf-8") as f:
        raw = f.read()

    applied = []
    not_found = []

    for key, new_val in sorted(hi_vals.items()):
        new_esc = xml_escape(new_val)
        # Match exakt EIN <string name="KEY" ...>INHALT</string>-Element.
        # ([^>]*) faengt evtl. weitere Attribute, (.*?) den (non-greedy) Inhalt.
        pat = re.compile(
            r'(<string\s+name="' + re.escape(key) + r'"([^>]*)>)(.*?)(</string>)',
            re.DOTALL,
        )
        m = pat.search(raw)
        if not m:
            not_found.append(key)
            continue
        old_val = m.group(3)
        if old_val == new_esc:
            # bereits identisch -> nichts zu tun, aber als no-op vermerken
            applied.append({"key": key, "old": old_val, "new": new_esc, "noop": True})
            continue
        # Nur den Inhalt ersetzen, oeffnendes/schliessendes Tag (inkl. Attribute) erhalten
        replacement = m.group(1) + new_esc + m.group(4)
        # genau dieses eine Vorkommen ersetzen (count=1, an der gefundenen Position)
        raw = raw[: m.start()] + replacement + raw[m.end():]
        applied.append({"key": key, "old": old_val, "new": new_esc})

    log_ckpt("replaced", {"applied": len([a for a in applied if not a.get("noop")]),
                          "noop": len([a for a in applied if a.get("noop")]),
                          "not_found": not_found})

    if not_found:
        # Defensive: wenn ein Key fehlt, NICHT schreiben — Orchestrator informieren
        raise SystemExit("Keys not found in mr: " + ", ".join(not_found))

    # Atomar schreiben
    d = os.path.dirname(MR)
    fd, tmp = tempfile.mkstemp(dir=d, suffix=".tmp")
    os.close(fd)
    with open(tmp, "w", encoding="utf-8", newline="") as f:
        f.write(raw)
    os.replace(tmp, MR)
    log_ckpt("written_atomic", {"file": MR})

    # Validierung der GANZEN mr-Datei
    xml_valid = True
    parse_err = None
    try:
        ET.parse(MR)
    except ET.ParseError as e:
        xml_valid = False
        parse_err = str(e)
    log_ckpt("validated", {"xmlValid": xml_valid, "err": parse_err})

    real_applied = [a for a in applied if not a.get("noop")]
    result = {
        "worker": "fa5",
        "applied": [{"key": a["key"], "old": a["old"], "new": a["new"]} for a in real_applied],
        "count": len(real_applied),
        "validation": {"xmlValid": xml_valid},
        "plugin_bugs_observed": [],
    }
    if not xml_valid:
        result["validation"]["error"] = parse_err

    # Output atomar schreiben
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    fd, tmp = tempfile.mkstemp(dir=os.path.dirname(OUT), suffix=".tmp")
    os.close(fd)
    with open(tmp, "w", encoding="utf-8", newline="") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    os.replace(tmp, OUT)

    # Konsolen-Liste
    print("=== X11 FA5 applied (Key | alter mr-Wert | neuer Wert) ===")
    for a in real_applied:
        print(f"{a['key']} | {a['old']} | {a['new']}")
    noops = [a for a in applied if a.get("noop")]
    if noops:
        print("--- already identical (no-op):", ", ".join(a["key"] for a in noops))
    print(f"\nTOTAL changed: {len(real_applied)}  |  xmlValid: {xml_valid}")


if __name__ == "__main__":
    main()

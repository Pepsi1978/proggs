#!/usr/bin/env python3
"""mm-research.py — Günstige Recherche-Pipeline: Firecrawl holt Quellen, MiniMax M3 (max Thinking)
wertet sie aus und gibt eine kompakte, quellentreue Antwort.

Der Clou: Die Roh-Quellen laufen NIE durch den teuren Claude/Opus-Kontext (Firecrawl -> hier -> MiniMax).
Gemessen 2026-06-20: ~2.400 Claude-Token statt ~249.000 bei einem Opus-Researcher (~100x weniger),
bei besserer Ehrlichkeit. MiniMax/Firecrawl-Token laufen separat im Go-Abo bzw. Firecrawl-Free.

Verwendung:
    python3 mm-research.py "deine Recherche-Frage" [anzahl_quellen]
    (max Thinking ist fest "adaptive" = M3s Maximum; ein budget_tokens-Wert wird vom Modell ignoriert.)

Keys (zentral in SK, siehe Regel secrets-in-sk-folder):
    ~/SK/OpenCode/go-api-key.txt        (OpenCode-Go Gateway)
    ~/SK/OpenCode/firecrawl-api-key.txt (Firecrawl)

Ausgabe: kompakte Antwort auf stdout; Rohdaten + Thinking in ~/.mm-research/ (NICHT im Claude-Kontext).

Plattformübergreifend (Windows + macOS), nur Standardbibliothek (urllib) — kein curl nötig.
Beachtet die Almanach-/Best-Practices-Regeln (bugs+best-practices/claude-tooling/python-windows.md):
open() immer encoding='utf-8', JSON ensure_ascii=False, newline='\n', Pfade via expanduser,
stdout.reconfigure(utf-8). Plus opencode-cli §14: MiniMax /messages braucht x-api-key (nicht Bearer).
"""
import json
import os
import sys
import urllib.request
import urllib.error

GO_URL = "https://opencode.ai/zen/go/v1/messages"          # MiniMax = Anthropic-Schema
FC_URL = "https://api.firecrawl.dev/v1/search"
OUTDIR = os.path.expanduser("~/.mm-research")


def _read_key(path):
    with open(os.path.expanduser(path), encoding="utf-8") as fh:
        return fh.read().strip()


def _post(url, headers, body, timeout):
    data = json.dumps(body).encode("utf-8")
    # WICHTIG: urllib-Default-UA ("Python-urllib/3.x") wird von Cloudflare vor dem opencode.ai-Gateway
    # mit 403/"error code 1010" geblockt. Mit curl-UA kommt der Request durch.
    headers = {"User-Agent": "curl/8.5.0", **headers}
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main():
    if len(sys.argv) < 2:
        return "Bitte eine Recherche-Frage als 1. Argument angeben."
    query = sys.argv[1]
    limit = int(sys.argv[2]) if len(sys.argv) > 2 else 5
    os.makedirs(OUTDIR, exist_ok=True)

    go_key = _read_key("~/SK/OpenCode/go-api-key.txt")
    fc_key = _read_key("~/SK/OpenCode/firecrawl-api-key.txt")

    # 1) Firecrawl — Quellen holen (laufen NICHT durch den Claude-Kontext)
    print(f"[1/2] Firecrawl-Suche: {query!r} (limit {limit})", file=sys.stderr)
    try:
        fc = _post(FC_URL,
                   {"Authorization": f"Bearer {fc_key}", "Content-Type": "application/json"},
                   {"query": query, "limit": limit, "scrapeOptions": {"formats": ["markdown"]}},
                   timeout=150)
    except urllib.error.HTTPError as e:
        return f"Firecrawl-Fehler {e.code}: {e.read().decode('utf-8', 'replace')[:300]}"
    with open(os.path.join(OUTDIR, "sources.json"), "w", encoding="utf-8") as fh:
        json.dump(fc, fh, ensure_ascii=False)
    data = fc.get("data") or []
    src = []
    for i, r in enumerate(data):
        md = (r.get("markdown") or r.get("content") or "")[:12000]
        src.append(f"### QUELLE {i+1}: {r.get('title','')} ({r.get('url','')})\n{md}")
    sources = "\n\n".join(src) if src else "(keine Quellen gefunden)"
    print(f"      {len(data)} Quellen geholt.", file=sys.stderr)

    # 2) MiniMax M3 (max Thinking) — Quellen quellentreu auswerten
    print("[2/2] MiniMax M3 (max Thinking = adaptive) wertet aus...", file=sys.stderr)
    prompt = ("Du bist ein Recherche-Auswerter. Beantworte AUSSCHLIESSLICH auf Basis der folgenden "
              "Quellen die Frage. Wenn etwas NICHT in den Quellen steht oder widerspruechlich ist, "
              "sage das ausdruecklich — erfinde nichts. Nenne pro Aussage die Quelle.\n\n"
              f"FRAGE: {query}\n\n=== QUELLEN ===\n{sources}")
    body = {"model": "minimax-m3", "max_tokens": 30000,
            "thinking": {"type": "adaptive"},  # M3s Maximum (max Thinking); budget_tokens wird ignoriert
            "messages": [{"role": "user", "content": prompt}]}
    try:
        d = _post(GO_URL,
                  {"x-api-key": go_key, "Content-Type": "application/json",
                   "anthropic-version": "2023-06-01"},
                  body, timeout=300)
    except urllib.error.HTTPError as e:
        return f"MiniMax-Fehler {e.code}: {e.read().decode('utf-8', 'replace')[:300]}"
    if d.get("type") == "error":
        return f"MiniMax-Fehler: {json.dumps(d.get('error'))}"

    text = "".join(b.get("text", "") for b in d.get("content", []) if b.get("type") == "text")
    think = "".join(b.get("thinking", "") for b in d.get("content", []) if b.get("type") == "thinking")
    with open(os.path.join(OUTDIR, "answer.json"), "w", encoding="utf-8") as fh:
        json.dump(d, fh, ensure_ascii=False)
    with open(os.path.join(OUTDIR, "thinking.txt"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write(think)

    sys.stdout.reconfigure(encoding="utf-8")  # Windows cp1252-stdout vermeiden
    print(text)
    u = d.get("usage", {})
    print(f"\n--- MiniMax-Token (Go-Abo): input {u.get('input_tokens')} + output "
          f"{u.get('output_tokens')} | Thinking {len(think)} Zeichen (~/.mm-research/thinking.txt) ---",
          file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

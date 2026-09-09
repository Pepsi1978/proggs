#!/usr/bin/env python3
"""mm-research.py — Guenstige Recherche-Pipeline: Firecrawl holt Quellen, DeepSeek V4 Flash
(reasoning effort "high") wertet sie aus und gibt eine kompakte, quellentreue Antwort.

UMSTELLUNG 09.09.2026: Die Auswertung (Stufe 2) lief frueher ueber MiniMax M3 auf dem
opencode.ai/zen/go-Gateway (Anthropic-/messages-Schema). Sie laeuft jetzt ueber OpenRouter
(/chat/completions) mit `deepseek/deepseek-v4-flash-0731`, Anbieter auf **DeepInfra** gepinnt
(`provider.order=["deepinfra"]`, `allow_fallbacks=False`). Verifiziert 09.09.2026 gegen die
OpenRouter-API: Endpunkt vorhanden, `reasoning_effort` unterstuetzt, 1.048.576 Token Kontext,
$0.06 / $0.18 pro Mio Token (in/out).

Der Clou: Die Roh-Quellen laufen NIE durch den teuren Claude-Kontext (Firecrawl -> hier -> DeepSeek).
Gemessen 2026-06-20: ~2.400 Claude-Token statt ~249.000 bei einem Opus-Researcher (~100x weniger),
bei besserer Ehrlichkeit. Auswerte-Token laufen separat ueber OpenRouter (pay-per-use), Quellen ueber Firecrawl-Free.

Verwendung:
    python3 mm-research.py "deine Recherche-Frage" [anzahl_quellen] [modell]
    MM_MODEL     (env) — Auswerte-Modell, Default `deepseek/deepseek-v4-flash-0731`.
    MM_PROVIDER  (env) — OpenRouter-Anbieter, Default `deepinfra`. LEER = kein Pin (freies Routing).
    MM_EFFORT    (env) — reasoning effort, Default `high`.

Keys (zentral in SK, siehe Regel secrets-in-sk-folder):
    ~/SK/ClaudeCodeOpenRouter/openrouter.key (OpenRouter, sk-or-v1-...)
    ~/SK/OpenCode/firecrawl-api-key.txt      (Firecrawl)

Ausgabe: kompakte Antwort auf stdout; Rohdaten + Thinking in ~/.mm-research/ (NICHT im Claude-Kontext).

Plattformübergreifend (Windows + macOS), nur Standardbibliothek (urllib) — kein curl nötig.
Beachtet die Almanach-/Best-Practices-Regeln (bugs+best-practices/claude-tooling/python-windows.md):
open() immer encoding='utf-8', JSON ensure_ascii=False, newline='\n', Pfade via expanduser,
stdout.reconfigure(utf-8). OpenRouter braucht Bearer-Auth + curl-User-Agent (Cloudflare-Block sonst).
"""
import json
import os
import re
import sys
import urllib.request
import urllib.error

OR_URL = "https://openrouter.ai/api/v1/chat/completions"   # Auswertung (seit 09.09.2026)
FC_URL = "https://api.firecrawl.dev/v1/search"
MODEL = os.environ.get("MM_MODEL", "deepseek/deepseek-v4-flash-0731")
# Anbieter-Pin: DeepInfra ist vorgegeben. allow_fallbacks=False, damit wirklich DeepInfra bedient
# und nicht still auf einen anderen Anbieter geroutet wird (Preis/Verhalten waeren sonst andere).
# MM_PROVIDER="" schaltet den Pin ab, falls DeepInfra mal ausfaellt.
PROVIDER = os.environ.get("MM_PROVIDER", "deepinfra")
EFFORT = os.environ.get("MM_EFFORT", "high")
# MM_OUTDIR ueberschreibbar, damit PARALLELE Laeufe (Continuous-Spawning mit 2, Firecrawl-Free-Limit)
# je eine eigene sources.json/answer.json/thinking.txt haben (sonst ueberschreiben sie sich — der
# Grund, warum bei der Second-Brain-Recherche R5 abgeschnitten zurueckkam). Default wie bisher.
OUTDIR = os.path.expanduser(os.environ.get("MM_OUTDIR", "~/.mm-research"))


def _read_key(path):
    with open(os.path.expanduser(path), encoding="utf-8") as fh:
        return fh.read().strip()


def _read_or_key():
    # Die Key-Datei darf einen Header/Kommentar haben -> die sk-or-v1-Zeile herausziehen (wie or-research.py).
    p = os.path.expanduser("~/SK/ClaudeCodeOpenRouter/openrouter.key")
    with open(p, encoding="utf-8") as fh:
        m = re.search(r"sk-or-v1-[A-Za-z0-9]+", fh.read())
    if not m:
        raise SystemExit("Kein OpenRouter-Key (sk-or-v1-...) in openrouter.key gefunden.")
    return m.group(0)


def _loads_resilient(raw):
    # Gateways (OpenRouter, evtl. das opencode.ai/zen-Go-Gateway) senden bei langer Verarbeitung
    # SSE-Keep-Alive-Kommentarzeilen (": OPENROUTER PROCESSING") VOR dem JSON-Body — auch bei
    # non-streaming. Das bricht json.loads. FIX (Almanach bugs/apis/openrouter-api.md #9): fuehrende
    # ':'-Kommentarzeilen ueberspringen und ab dem ersten '{' das JSON-Objekt parsen (raw_decode
    # ignoriert evtl. Trailing). Schnellster Pfad (sauberes JSON) bleibt unveraendert.
    raw = raw.strip()
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        i = raw.find("{")
        if i < 0:
            raise
        return json.JSONDecoder().raw_decode(raw[i:])[0]


def _post(url, headers, body, timeout):
    data = json.dumps(body).encode("utf-8")
    # WICHTIG: urllib-Default-UA ("Python-urllib/3.x") wird von Cloudflare vor dem opencode.ai-Gateway
    # mit 403/"error code 1010" geblockt. Mit curl-UA kommt der Request durch.
    headers = {"User-Agent": "curl/8.5.0", **headers}
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return _loads_resilient(resp.read().decode("utf-8"))


def main():
    if len(sys.argv) < 2:
        return "Bitte eine Recherche-Frage als 1. Argument angeben."
    query = sys.argv[1]
    limit = int(sys.argv[2]) if len(sys.argv) > 2 else 5
    model = sys.argv[3] if len(sys.argv) > 3 else MODEL
    os.makedirs(OUTDIR, exist_ok=True)

    or_key = _read_or_key()
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

    # 2) DeepSeek V4 Flash (reasoning high) — Quellen quellentreu auswerten
    print(f"[2/2] {model} @ {PROVIDER or 'freies Routing'} (effort {EFFORT}) wertet aus...", file=sys.stderr)
    prompt = ("Du bist ein Recherche-Auswerter. Beantworte AUSSCHLIESSLICH auf Basis der folgenden "
              "Quellen die Frage. Wenn etwas NICHT in den Quellen steht oder widerspruechlich ist, "
              "sage das ausdruecklich — erfinde nichts. Nenne pro Aussage die Quelle.\n\n"
              f"FRAGE: {query}\n\n=== QUELLEN ===\n{sources}")
    body = {"model": model, "max_tokens": 30000,
            "reasoning": {"effort": EFFORT},   # max Thinking auf /chat/completions
            "messages": [{"role": "user", "content": prompt}]}
    if PROVIDER:
        body["provider"] = {"order": [PROVIDER], "allow_fallbacks": False}
    try:
        d = _post(OR_URL,
                  {"Authorization": f"Bearer {or_key}", "Content-Type": "application/json",
                   "HTTP-Referer": "https://github.com/Pepsi1978/proggs", "X-Title": "proggs-mm-research"},
                  body, timeout=300)
    except urllib.error.HTTPError as e:
        return f"OpenRouter-Fehler {e.code}: {e.read().decode('utf-8', 'replace')[:300]}"
    if d.get("error"):
        return f"OpenRouter-Fehler: {json.dumps(d.get('error'))[:300]}"

    msg = (d.get("choices") or [{}])[0].get("message", {}) or {}
    text = msg.get("content") or ""
    think = msg.get("reasoning") or ""
    with open(os.path.join(OUTDIR, "answer.json"), "w", encoding="utf-8") as fh:
        json.dump(d, fh, ensure_ascii=False)
    with open(os.path.join(OUTDIR, "thinking.txt"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write(think)

    sys.stdout.reconfigure(encoding="utf-8")  # Windows cp1252-stdout vermeiden
    print(text)
    u = d.get("usage", {})
    prov = d.get("provider") or "?"   # sichtbar machen, falls doch woanders hin geroutet wurde
    print(f"\n--- {model} @ {prov}: Token in {u.get('prompt_tokens')} + out {u.get('completion_tokens')} "
          f"| Kosten ${u.get('cost')} | Thinking {len(think)} Zeichen ({OUTDIR}/thinking.txt) ---",
          file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

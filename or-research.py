#!/usr/bin/env python3
"""or-research.py — Recherche-Pipeline ueber OpenRouter mit eingebauter Websuche (Server-Tool
"openrouter:web_search"). Ein 1M-Kontext-Modell sucht SELBST im Web und wertet aus; Claude/Opus
bekommt nur die kompakte, quellengestuetzte Antwort.

WICHTIG (Stand 2026-06-20): Das alte ":online"-Plugin / `plugins:[{id:"web"}]` ist DEPRECATED.
Dieses Skript nutzt den neuen Weg: `tools:[{"type":"openrouter:web_search", "parameters":{...}}]`.
Das Modell entscheidet selbst, ob/wie oft es sucht (0..N Suchen pro Request, mit max_total_results gedeckelt).

Unterschied zu mm-research.py:
  - mm-research.py: Firecrawl (Free 1000/Mon, VOLLE Seiten) -> MiniMax M3 (Go-Abo) wertet aus.
  - or-research.py: OpenRouter web_search server tool (Snippets) -> ein Call; Modell sucht + wertet
    selbst aus. Pay-per-use, KEIN Monatslimit. Ideal als Eskalations-Stufe B (1M + Websuche) ODER
    fuer grosse Schwaerme (kein 2-parallel-Limit wie Firecrawl).

Verwendung:
    python3 or-research.py "deine Recherche-Frage" [modell] [engine]
    Default-Modell: minimax/minimax-m3 (1M, guenstig). Eskalation: z-ai/glm-5.2 (mehr Denkkraft, 1M).
    Engine (3. Arg oder env OR_ENGINE): parallel (DEFAULT) | exa | auto | perplexity | native | firecrawl.
      - "parallel" = Such-Engine parallel.ai (NICHT parallele Ausfuehrung!), deckelt Kontext total. DEFAULT.
      - "auto" nutzt native Provider-Suche, falls das Modell sie hat, sonst Exa.
    OR_MAX_RESULTS (env, Default 5; 1-25) Treffer pro Suche; OR_MAX_TOTAL (env, Default 10) Treffer-Deckel
    ueber alle Suchen eines Requests (Kostenbremse fuer agentische Mehrfachsuche).

Kosten (offizielle Doku 2026-06-20, openrouter.ai/docs server-tools/web-search):
    Exa/Parallel = $0.005 PRO Such-Anfrage (inkl. bis 10 Treffer), darueber +$0.001/Treffer (max 25).
    Perplexity = $0.005/Anfrage. Firecrawl = eigene Credits (BYOK). Native = vom Provider durchgereicht.
    PLUS normale Modell-Token fuer die Treffer-Snippets. "Treffer" = Snippet (Exa-Highlight ~2-4k Zeichen),
    KEINE ganze Seite. usage.server_tool_use.web_search_requests zaehlt die tatsaechlichen Suchen.

Key: ~/SK/ClaudeCodeOpenRouter/openrouter.key (sk-or-v1-Zeile wird extrahiert; Datei darf Header haben).
Ausgabe: kompakte Antwort + Quellen auf stdout; volle Roh-Antwort in ~/.or-research/answer.json.

Plattformuebergreifend (Windows+macOS), nur Standardbibliothek (urllib). Beachtet python-windows-Regeln
(encoding='utf-8', ensure_ascii=False, expanduser, stdout.reconfigure) + User-Agent (Cloudflare).
"""
import json
import os
import re
import sys
import urllib.request
import urllib.error

OR_URL = "https://openrouter.ai/api/v1/chat/completions"
OUTDIR = os.path.expanduser("~/.or-research")


def _read_key():
    p = os.path.expanduser("~/SK/ClaudeCodeOpenRouter/openrouter.key")
    with open(p, encoding="utf-8") as fh:
        m = re.search(r"sk-or-v1-[A-Za-z0-9]+", fh.read())
    if not m:
        raise SystemExit("Kein OpenRouter-Key (sk-or-v1-...) in openrouter.key gefunden.")
    return m.group(0)


def _loads_or(raw):
    # OpenRouter sendet bei langer Verarbeitung (agentische web_search) SSE-Keep-Alive-Kommentarzeilen
    # (": OPENROUTER PROCESSING") VOR dem JSON-Body — auch bei non-streaming. Das bricht json.loads.
    # FIX laut Almanach bugs/apis/openrouter-api.md #9: fuehrende ':'-Kommentarzeilen ueberspringen und
    # ab dem ersten '{' das eine JSON-Objekt parsen (raw_decode ignoriert evtl. Trailing-Keep-Alives).
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
    headers = {"User-Agent": "curl/8.5.0", **headers}
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return _loads_or(resp.read().decode("utf-8"))


def main():
    if len(sys.argv) < 2:
        return "Bitte eine Recherche-Frage als 1. Argument angeben."
    query = sys.argv[1]
    model = sys.argv[2] if len(sys.argv) > 2 else "minimax/minimax-m3"
    engine = sys.argv[3] if len(sys.argv) > 3 else os.environ.get("OR_ENGINE", "parallel")
    max_results = int(os.environ.get("OR_MAX_RESULTS", "5"))
    max_total = int(os.environ.get("OR_MAX_TOTAL", "10"))
    os.makedirs(OUTDIR, exist_ok=True)
    key = _read_key()

    prompt = ("Recherchiere die folgende Frage im Web (nutze dein Websuche-Tool) und beantworte sie "
              "quellentreu. Verwende AUSSCHLIESSLICH das, was du in den Webquellen findest; wenn etwas "
              "unklar/widerspruechlich ist oder fehlt, sage das ausdruecklich — erfinde nichts. Nenne pro "
              "Aussage die Quelle (URL).\n\nFRAGE: " + query)
    web_params = {"engine": engine, "max_results": max_results, "max_total_results": max_total}
    body = {
        "model": model,
        "messages": [{"role": "user", "content": prompt}],
        "tools": [{"type": "openrouter:web_search", "parameters": web_params}],
        "reasoning": {"effort": "high"},   # max Thinking (ignoriert, falls Modell es nicht unterstuetzt)
    }
    print(f"[or-research] {model} | engine={engine} | max_results={max_results} total={max_total} — {query!r}",
          file=sys.stderr)
    try:
        d = _post(OR_URL,
                  {"Authorization": f"Bearer {key}", "Content-Type": "application/json",
                   "HTTP-Referer": "https://github.com/Pepsi1978/proggs", "X-Title": "proggs-or-research"},
                  body, timeout=300)
    except urllib.error.HTTPError as e:
        return f"OpenRouter-Fehler {e.code}: {e.read().decode('utf-8', 'replace')[:400]}"
    if d.get("error"):
        return f"OpenRouter-Fehler: {json.dumps(d['error'])[:400]}"

    with open(os.path.join(OUTDIR, "answer.json"), "w", encoding="utf-8") as fh:
        json.dump(d, fh, ensure_ascii=False)
    msg = (d.get("choices") or [{}])[0].get("message", {})
    text = msg.get("content") or ""
    cites = []
    for a in msg.get("annotations", []) or []:
        u = (a.get("url_citation") or {})
        if u.get("url"):
            cites.append("- " + (u.get("title") or u["url"]) + " — " + u["url"])

    sys.stdout.reconfigure(encoding="utf-8")
    print(text)
    if cites:
        print("\n=== Web-Quellen ===\n" + "\n".join(dict.fromkeys(cites)))
    u = d.get("usage", {})
    stu = u.get("server_tool_use_details") or u.get("server_tool_use") or {}
    searches = stu.get("web_search_requests")
    cost = u.get("cost")
    print(f"\n--- OpenRouter: {searches} Web-Suchen | Token in {u.get('prompt_tokens')} / out "
          f"{u.get('completion_tokens')} | Kosten ${cost} | {model}, engine={engine} "
          f"| Roh: ~/.or-research/answer.json ---", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

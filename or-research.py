#!/usr/bin/env python3
"""or-research.py — Recherche-Pipeline ueber OpenRouter mit eingebauter Websuche (das ":online"-Plugin
ins CLI portiert). Ein 1M-Kontext-Modell sucht SELBST im Web (Exa) und wertet aus; Claude/Opus bekommt
nur die kompakte, quellengestuetzte Antwort.

Unterschied zu mm-research.py:
  - mm-research.py: Firecrawl (Free 1000/Mon, voller Scrape) -> MiniMax M3 (Go-Abo) wertet aus.
  - or-research.py: OpenRouter <modell>:online (Exa-Suche, pay-per-use, KEIN Monatslimit) -> ein Call,
    Modell sucht + wertet selbst aus. Ideal als Eskalations-Stufe B (1M + native Websuche) ODER wenn
    Firecrawl-Credits knapp sind.

Verwendung:
    python3 or-research.py "deine Recherche-Frage" [modell] [web_results]
    Default-Modell: minimax/minimax-m3 (1M, guenstig). Eskalation: z-ai/glm-5.2 (mehr Denkkraft, 1M).
    OR_WEB_RESULTS (env, optional): Anzahl Web-Treffer (Default 5; max 25).

Kosten (offizielle Doku, Stand 2026-06-20): Exa/Parallel = $0.005 PRO SUCH-ANFRAGE (inkl. bis 10 Treffer),
darueber +$0.001/Treffer. PLUS normale Modell-Token fuer die Treffer-Snippets (je ~2-4k Zeichen, KEINE
ganzen Seiten). Ein Modell kann agentisch MEHRMALS suchen (je $0.005) — mit max_total_results deckelbar.
Grobe Hausnummer pro Recherche-Anfrage mit MiniMax M3: ~$0.008 (deutlich unter 1 Cent). KEIN Monatslimit
(Unterschied zu Firecrawl). Fuer volle Seiten-Scrapes statt Snippets -> mm-research.py (Firecrawl).

Key: ~/SK/ClaudeCodeOpenRouter/openrouter.key (sk-or-v1-Zeile wird extrahiert; Datei darf Header haben).

Ausgabe: kompakte Antwort + Quellen auf stdout; volle Roh-Antwort in ~/.or-research/ (nicht im Claude-Kontext).

Plattformuebergreifend (Windows+macOS), nur Standardbibliothek (urllib). Beachtet die python-windows-
Regeln (encoding='utf-8', ensure_ascii=False, expanduser, stdout.reconfigure) und setzt einen User-Agent.
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


def _post(url, headers, body, timeout):
    data = json.dumps(body).encode("utf-8")
    headers = {"User-Agent": "curl/8.5.0", **headers}
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main():
    if len(sys.argv) < 2:
        return "Bitte eine Recherche-Frage als 1. Argument angeben."
    query = sys.argv[1]
    model = sys.argv[2] if len(sys.argv) > 2 else "minimax/minimax-m3"
    web_results = int(sys.argv[3]) if len(sys.argv) > 3 else int(os.environ.get("OR_WEB_RESULTS", "5"))
    os.makedirs(OUTDIR, exist_ok=True)
    key = _read_key()

    prompt = ("Recherchiere die folgende Frage im Web und beantworte sie quellentreu. Nutze AUSSCHLIESSLICH "
              "das, was du in den Webquellen findest; wenn etwas unklar/widerspruechlich ist oder fehlt, "
              "sage das ausdruecklich — erfinde nichts. Nenne pro Aussage die Quelle (URL).\n\nFRAGE: " + query)
    body = {
        "model": model,
        "messages": [{"role": "user", "content": prompt}],
        "plugins": [{"id": "web", "max_results": web_results}],  # = ":online", aber mit Treffer-/Kostenkontrolle
        "reasoning": {"effort": "high"},                          # max Thinking (vom Modell ignoriert, falls nicht unterstuetzt)
    }
    print(f"[or-research] Modell {model}:online, {web_results} Web-Treffer — Frage: {query!r}", file=sys.stderr)
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
    # Web-Zitate (annotations) einsammeln
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
    print(f"\n--- OpenRouter-Token: prompt {u.get('prompt_tokens')} + completion {u.get('completion_tokens')} "
          f"| Modell {model}:online | volle Antwort in ~/.or-research/answer.json ---", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

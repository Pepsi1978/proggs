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
import time
import urllib.request
import urllib.error

OR_URL = "https://openrouter.ai/api/v1/chat/completions"
# OR_OUTDIR ueberschreibbar, damit PARALLELE Laeufe je eine eigene answer.json haben (sonst ueber-
# schreiben sie sich gegenseitig — genau das verhinderte bei parallelen Schwaermen die Provider-/Roh-
# Analyse). Default wie bisher ~/.or-research.
OUTDIR = os.path.expanduser(os.environ.get("OR_OUTDIR", "~/.or-research"))
RETRIES = int(os.environ.get("OR_RETRIES", "3"))   # Retry bei Last-Fehlern (leer/Timeout/Leak), Almanach #41
# JSON-Tool-Call-Leak (Almanach #42): Modell leakt agentische FOLGE-Tool-Calls als Text statt sie
# auszufuehren -> {"name": "web_search", "input": {"query": ...}} und liefert KEINE echte Antwort.
# Der XML-only-Marker-Satz unten erkennt das nicht; dieser Regex schliesst die Luecke.
LEAK_RE = re.compile(r'\{"name":\s*"\w+",\s*"(input|arguments)":')


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
    # :online-Modus (bevorzugte Eskalationsstufe seit 2026-06-21): das Modell-Suffix ":online" laesst
    # OpenRouter selbst eine Websuche dazuschalten (web-Plugin, Such-Engine = parallel.ai) — KEIN
    # tools-Server-Tool, KEINE explizite engine. Empirisch stabiler bei hoher Parallelitaet als das
    # web_search-Server-Tool (Almanach openrouter-api.md #13/#41), darum vom research-Skill bevorzugt.
    # reasoning:high bleibt — M3 denkt bei :online ohnehin ~900 Tok (A/B-getestet 2026-06-21: kein
    # Qualitaetsunterschied mit/ohne, aber konsistent mit "max Thinking"-Policy). Retry faengt den
    # intermittenten JSON-Tool-Call-Leak (§42), der bei :online vorkommen kann.
    is_online = model.endswith(":online")
    web_params = {"engine": engine, "max_results": max_results, "max_total_results": max_total}
    body = {
        "model": model,
        "messages": [{"role": "user", "content": prompt}],
        "reasoning": {"effort": "high"},   # max Thinking (ignoriert, falls Modell es nicht unterstuetzt)
    }
    if not is_online:
        body["tools"] = [{"type": "openrouter:web_search", "parameters": web_params}]
    print(f"[or-research] {model} | {'online-Plugin' if is_online else 'engine=' + engine} | "
          f"max_results={max_results} total={max_total} | retries={RETRIES} — {query!r}", file=sys.stderr)
    hdr = {"Authorization": f"Bearer {key}", "Content-Type": "application/json",
           "HTTP-Referer": "https://github.com/Pepsi1978/proggs", "X-Title": "proggs-or-research"}
    # Leak-Marker eng gehalten (kein Fehlalarm bei legitimem Code-Beispiel in einer Recherche-Antwort):
    leak_markers = ("<invoke name=", "]<]", "minimax[>", "<tool_call>", "</tool_call>", "antml:invoke")
    # Retry mit Backoff (Almanach bugs/apis/openrouter-api.md #41): Unter Last/Parallelitaet liefert
    # OpenRouter manchmal eine LEERE Response (-> JSONDecodeError), Timeouts/Connection-Fehler ODER
    # (Provider-Varianz) Tool-Call-Leaks/leeren content. Das ist INTERMITTENT — der naechste Versuch
    # erwischt fast immer einen guten Slot. Darum ALLE diese Faelle fangen (nicht nur HTTPError) und
    # bis RETRIES mal neu versuchen, bevor wir aufgeben.
    d = None
    last_err = "?"
    for attempt in range(1, RETRIES + 1):
        try:
            d = _post(OR_URL, hdr, body, timeout=300)
        except urllib.error.HTTPError as e:
            last_err = f"HTTP {e.code}"
            detail = e.read().decode("utf-8", "replace")[:400]
            if e.code in (408, 409, 425, 429, 500, 502, 503, 504) and attempt < RETRIES:
                print(f"[or-research] Versuch {attempt}/{RETRIES}: {last_err} — retry in {2*attempt}s...", file=sys.stderr)
                time.sleep(2 * attempt); continue
            return f"OpenRouter-Fehler {e.code}: {detail}"
        except (urllib.error.URLError, json.JSONDecodeError, ValueError, TimeoutError, ConnectionError, OSError) as e:
            # leere Response (JSONDecodeError), Timeout, Connection-Reset, DNS etc. — alles unter Last moeglich
            last_err = f"{type(e).__name__}: {str(e)[:120]}"
            if attempt < RETRIES:
                print(f"[or-research] Versuch {attempt}/{RETRIES} fehlgeschlagen ({last_err}) — retry in {2*attempt}s...", file=sys.stderr)
                time.sleep(2 * attempt); continue
            return (f"[or-research FEHLER] {model}: leere/kaputte Antwort nach {RETRIES} Versuchen ({last_err}). "
                    f"Wahrscheinlich Last/Parallelitaet (Almanach bugs/apis/openrouter-api.md #41) — weniger "
                    f"gleichzeitig laufen lassen oder spaeter erneut.")
        if d.get("error"):
            last_err = json.dumps(d['error'])[:200]
            if attempt < RETRIES:
                print(f"[or-research] Versuch {attempt}/{RETRIES}: API-Error ({last_err}) — retry in {2*attempt}s...", file=sys.stderr)
                time.sleep(2 * attempt); continue
            return f"OpenRouter-Fehler: {last_err}"
        # content-Qualitaet pruefen — leer/leak ist ebenfalls intermittent -> retrien statt gleich aufgeben
        _txt = ((d.get("choices") or [{}])[0].get("message", {}).get("content")) or ""
        if (not _txt.strip() or any(m in _txt for m in leak_markers) or LEAK_RE.search(_txt)) and attempt < RETRIES:
            last_err = "leerer/leaky content"
            print(f"[or-research] Versuch {attempt}/{RETRIES}: {last_err} — retry in {2*attempt}s...", file=sys.stderr)
            time.sleep(2 * attempt); continue
        break  # Erfolg ODER letzter Versuch (der Detektor unten faengt einen final kaputten Fall ab)

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
    # FINALER Detektor (Almanach bugs/apis/openrouter-api.md #41): greift nur noch, wenn AUCH der letzte
    # Retry-Versuch oben kaputt war (leerer content oder Tool-Call-Leak). leak_markers ist oben definiert.
    is_leak = any(m in text for m in leak_markers) or bool(LEAK_RE.search(text))
    if not text.strip() or is_leak:
        if cites:
            print("=== Web-Quellen (nur Quellen brauchbar; Antworttext defekt) ===\n"
                  + "\n".join(dict.fromkeys(cites)))
        grund = "LEERER content" if not text.strip() else "TOOL-CALL-LEAK im content"
        return (f"[or-research FEHLER] {model}: {grund} nach {RETRIES} Versuchen — Lauf unbrauchbar. "
                f"Ursache meist Last/Parallelitaet (Almanach bugs/apis/openrouter-api.md #41): weniger "
                f"gleichzeitig laufen lassen, spaeter erneut, oder anderes Modell testen. "
                f"Roh: {OUTDIR}/answer.json")
    print(text)
    if cites:
        print("\n=== Web-Quellen ===\n" + "\n".join(dict.fromkeys(cites)))
    u = d.get("usage", {})
    stu = u.get("server_tool_use_details") or u.get("server_tool_use") or {}
    searches = stu.get("web_search_requests")
    cost = u.get("cost")
    prov = d.get("provider") or "?"   # welcher OpenRouter-Anbieter das Modell bediente (Routing-Varianz)
    print(f"\n--- OpenRouter: {searches} Web-Suchen | Token in {u.get('prompt_tokens')} / out "
          f"{u.get('completion_tokens')} | Kosten ${cost} | {model} @ {prov}, engine={engine} "
          f"| Roh: {OUTDIR}/answer.json ---", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

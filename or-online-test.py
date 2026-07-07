#!/usr/bin/env python3
"""or-online-test.py — TEST (2026-06-21): minimax/minimax-m3:online im PUREN :online-Modus.

Franks Theorie: 10 ECHT parallele Researcher mit dem ":online"-Suffix laufen sauber durch —
anders als das "web_search"-Server-Tool, das unter Parallelitaet leer/kaputt wird (Almanach
bugs/apis/openrouter-api.md #41) und darum auf max 2 begrenzt wurde. Dieses Skript prueft das.

Body bewusst MINIMAL ("mehr nicht"): nur model=<...:online> + messages + Pflicht-Header
(HTTP-Referer/X-Title — sonst kommt content still leer zurueck, Almanach openrouter-api #1).
KEIN tools-Server-Tool, KEIN engine-Parameter, KEIN reasoning.

KEIN Retry-Netz (OR_RETRIES Default 1): der Test soll die WAHRE Erstversuch-Erfolgsquote bei
echter Parallelitaet messen — ein Retry wuerde das Last-Problem (#41) maskieren.

Aufruf:  python3 or-online-test.py <researcher-id> "<recherche-thema>"
Schreibt je Lauf (getrennte Dateien -> kein paralleler Schreibkonflikt, python-windows #12):
  ~/.or-online-test/answer-<id>.json  (volle Roh-Antwort, fuer die inhaltliche Auswertung)
  ~/.or-online-test/result-<id>.json  (Metriken: status, attempts, latency_s, cost, searches,
                                       provider, content_len, cites, error)
Nur stdlib (urllib), plattformuebergreifend. encoding='utf-8'/ensure_ascii=False/os.replace.
"""
import json
import os
import re
import sys
import time
import urllib.request
import urllib.error

OR_URL = "https://openrouter.ai/api/v1/chat/completions"
OUTDIR = os.path.expanduser("~/.or-online-test")
RETRIES = int(os.environ.get("OR_RETRIES", "1"))            # 1 = KEIN Retry (purer Test)
MODEL = os.environ.get("OR_MODEL", "minimax/minimax-m3:online")
# MiniMax-typische Tool-Call-Leak-Marker (wie or-research.py), eng gehalten gegen Fehlalarm:
LEAK_MARKERS = ("<invoke name=", "]<]", "minimax[>", "<tool_call>", "</tool_call>")
# JSON-Tool-Call-Leak (Almanach #42): im :online-Modus leakt das Modell agentische FOLGE-Suchen als
# Text statt sie auszufuehren — z.B. {"name": "web_search", "input": {"query": "..."}} — und liefert
# dann KEINE echte Antwort (genau so fiel Researcher 7 beim Second-Brain-Test aus, vom alten
# XML-only-Detektor unentdeckt). Regex fuer den verbreiteten OpenAI-/Anthropic-Tool-Call-Shape:
LEAK_RE = re.compile(r'\{"name":\s*"\w+",\s*"(input|arguments)":')


def _looks_leaky(text):
    return any(m in text for m in LEAK_MARKERS) or bool(LEAK_RE.search(text))


def _read_key():
    p = os.path.expanduser("~/SK/ClaudeCodeOpenRouter/openrouter.key")
    with open(p, encoding="utf-8") as fh:
        m = re.search(r"sk-or-v1-[A-Za-z0-9]+", fh.read())
    if not m:
        raise SystemExit("Kein OpenRouter-Key (sk-or-v1-...) gefunden.")
    return m.group(0)


def _loads_or(raw):
    # OpenRouter schickt bei langer Verarbeitung fuehrende ':'-Keep-Alive-Kommentarzeilen vor dem
    # JSON-Body (Almanach #9) — ab erstem '{' parsen, wenn direktes json.loads scheitert.
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


def _atomic_write(path, obj):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, ensure_ascii=False)
    os.replace(tmp, path)


def main():
    if len(sys.argv) < 3:
        return 'Aufruf: or-online-test.py <id> "<thema>"'
    rid = sys.argv[1]
    query = sys.argv[2]
    os.makedirs(OUTDIR, exist_ok=True)
    key = _read_key()

    prompt = ("Recherchiere die folgende Frage im Web und beantworte sie quellentreu. Nutze "
              "AUSSCHLIESSLICH Webquellen; wenn etwas unklar/widerspruechlich/fehlend ist, sage das "
              "ausdruecklich — erfinde nichts. Nenne pro Aussage die Quelle (URL).\n\nFRAGE: " + query)
    body = {"model": MODEL, "messages": [{"role": "user", "content": prompt}]}
    # OR_REASONING (leer=Default: kein Thinking). Zahl -> max_tokens-Budget (echtes MAXIMUM, z.B. 24000);
    # Wort ("high"/"medium") -> effort. Testet, ob :online + (max) Thinking durchkommt.
    _reason = os.environ.get("OR_REASONING", "").strip()
    if _reason:
        body["reasoning"] = {"max_tokens": int(_reason)} if _reason.isdigit() else {"effort": _reason}
    hdr = {"Authorization": f"Bearer {key}", "Content-Type": "application/json",
           "HTTP-Referer": "https://github.com/Pepsi1978/proggs", "X-Title": "proggs-or-online-test"}

    t0 = time.monotonic()
    d = None
    status = "?"
    err = ""
    attempts = 0
    for attempt in range(1, RETRIES + 1):
        attempts = attempt
        try:
            d = _post(OR_URL, hdr, body, timeout=300)
        except urllib.error.HTTPError as e:
            err = f"HTTP {e.code}: " + e.read().decode("utf-8", "replace")[:200]
            status = "http_error"
            d = None
            if attempt < RETRIES:
                time.sleep(2 * attempt); continue
            break
        except Exception as e:  # URLError/Timeout/JSONDecode/Connection — alles unter Last moeglich
            err = f"{type(e).__name__}: {str(e)[:200]}"
            status = "conn_error"
            d = None
            if attempt < RETRIES:
                time.sleep(2 * attempt); continue
            break
        if isinstance(d, dict) and d.get("error"):
            err = json.dumps(d["error"])[:300]
            status = "api_error"
            if attempt < RETRIES:
                time.sleep(2 * attempt); continue
            break
        status = "got_response"
        break
    latency = round(time.monotonic() - t0, 1)

    result = {"id": rid, "thema": query[:90], "attempts": attempts, "latency_s": latency,
              "model": MODEL, "status": status, "error": err}
    if isinstance(d, dict):
        _atomic_write(os.path.join(OUTDIR, f"answer-{rid}.json"), d)
        msg = (d.get("choices") or [{}])[0].get("message", {})
        text = msg.get("content") or ""
        cites = [(a.get("url_citation") or {}).get("url") for a in (msg.get("annotations") or [])]
        cites = [c for c in cites if c]
        u = d.get("usage", {}) or {}
        stu = u.get("server_tool_use_details") or u.get("server_tool_use") or {}
        if status == "got_response":
            if not text.strip():
                status = "empty"
            elif _looks_leaky(text):
                status = "leak"
            else:
                status = "ok"
        result.update({
            "status": status,
            "content_len": len(text),
            "cites": len(cites),
            "searches": stu.get("web_search_requests"),
            "cost": u.get("cost"),
            "prompt_tokens": u.get("prompt_tokens"),
            "completion_tokens": u.get("completion_tokens"),
            "provider": d.get("provider"),
        })
    _atomic_write(os.path.join(OUTDIR, f"result-{rid}.json"), result)

    sys.stdout.reconfigure(encoding="utf-8")
    print(f"[{rid}] {status} | {latency}s | cites={result.get('cites')} | "
          f"len={result.get('content_len')} | cost=${result.get('cost')} | prov={result.get('provider')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

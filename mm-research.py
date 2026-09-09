#!/usr/bin/env python3
"""mm-research.py — Guenstige Recherche-Pipeline: Firecrawl holt Quellen, DeepSeek V4 Flash
(reasoning effort "high") wertet sie aus und gibt eine kompakte, quellentreue Antwort.

RUECKFALL 09.09.2026: Liefert Firecrawl einen Fehler ODER keine brauchbaren Quellen (0 Treffer bzw.
alle Treffer praktisch leer), sucht das Skript AUTOMATISCH bei **Tavily** nach — mit den maximalen
Einstellungen, die Tavily anbietet: `search_depth="advanced"`, `max_results=20`, `chunks_per_source=3`,
`include_raw_content=True`, `include_answer="advanced"`. Steuerung ueber MM_TAVILY:
    fallback (Default) — Tavily nur, wenn Firecrawl nichts Brauchbares liefert
    always             — IMMER beide Quellenwege, Ergebnisse zusammengefuehrt (Duplikate per URL raus)
    off                — nur Firecrawl, kein Rueckfall
Jede Quelle wird mit ihrer Herkunft markiert ([Firecrawl] / [Tavily]), damit in der Auswertung
sichtbar bleibt, woher ein Beleg stammt.

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
    MM_TAVILY    (env) — `fallback` (Default) | `always` | `off`, siehe oben.

Keys (zentral in SK, siehe Regel secrets-in-sk-folder):
    ~/SK/ClaudeCodeOpenRouter/openrouter.key (OpenRouter, sk-or-v1-...)
    ~/SK/OpenCode/firecrawl-api-key.txt      (Firecrawl)
    ~/SK/Tavily/tavily-api-key.txt           (Tavily, Rueckfall-Suche)

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
TV_URL = "https://api.tavily.com/search"                   # Rueckfall-Suche (seit 09.09.2026)
# Wann Tavily einspringt: "fallback" = nur wenn Firecrawl nichts Brauchbares liefert (Default),
# "always" = immer beide (zusammengefuehrt), "off" = nie. Ein leeres/kaputtes Firecrawl-Ergebnis
# ist der einzige maschinell erkennbare Fall von "liefert keine richtigen Ergebnisse" — wer auf
# Nummer sicher gehen will, setzt MM_TAVILY=always.
TAVILY_MODE = os.environ.get("MM_TAVILY", "fallback").lower()
MIN_MARKDOWN = 200   # kuerzer als das gilt ein Treffer als leer (Cookie-Banner/Fehlerseite)
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


def _firecrawl(query, limit, fc_key):
    """Firecrawl-Suche. Gibt (treffer, fehlertext) zurueck — nie eine Exception nach aussen,
    damit der Tavily-Rueckfall auch bei HTTP-Fehlern/Timeouts greift (und nicht nur bei 0 Treffern)."""
    try:
        fc = _post(FC_URL,
                   {"Authorization": f"Bearer {fc_key}", "Content-Type": "application/json"},
                   {"query": query, "limit": limit, "scrapeOptions": {"formats": ["markdown"]}},
                   timeout=150)
    except urllib.error.HTTPError as e:
        return [], f"HTTP {e.code}: {e.read().decode('utf-8', 'replace')[:200]}"
    except Exception as e:
        return [], f"{type(e).__name__}: {str(e)[:160]}"
    out = []
    for r in (fc.get("data") or []):
        out.append({"titel": r.get("title", ""), "url": r.get("url", ""),
                    "text": r.get("markdown") or r.get("content") or "", "woher": "Firecrawl"})
    return out, None


def _tavily(query, tv_key):
    """Tavily-Suche mit den MAXIMALEN Einstellungen, die Tavily anbietet (advanced depth, 20 Quellen,
    3 Chunks pro Quelle, Volltext). Gibt (treffer, fehlertext) zurueck."""
    body = {"query": query, "search_depth": "advanced", "max_results": 20,
            "chunks_per_source": 3, "include_raw_content": True, "include_answer": "advanced"}
    try:
        d = _post(TV_URL,
                  {"Authorization": f"Bearer {tv_key}", "Content-Type": "application/json"},
                  body, timeout=180)
    except urllib.error.HTTPError as e:
        return [], f"HTTP {e.code}: {e.read().decode('utf-8', 'replace')[:200]}"
    except Exception as e:
        return [], f"{type(e).__name__}: {str(e)[:160]}"
    out = []
    for r in (d.get("results") or []):
        out.append({"titel": r.get("title", ""), "url": r.get("url", ""),
                    "text": r.get("raw_content") or r.get("content") or "", "woher": "Tavily"})
    # Tavilys eigene Kurzantwort als zusaetzliche "Quelle 0" mitgeben — sie ist quellengestuetzt
    # und hilft dem Auswerter beim Einordnen. Klar als Tavily-Zusammenfassung gekennzeichnet.
    if d.get("answer"):
        out.insert(0, {"titel": "Tavily-Kurzantwort (aus den Treffern zusammengefasst)",
                       "url": "tavily:answer", "text": d["answer"], "woher": "Tavily"})
    return out, None


def _brauchbar(treffer):
    """Mindestens ein Treffer mit echtem Inhalt? Sonst gilt die Suche als ergebnislos."""
    return any(len((t.get("text") or "").strip()) >= MIN_MARKDOWN for t in treffer)


def main():
    if len(sys.argv) < 2:
        return "Bitte eine Recherche-Frage als 1. Argument angeben."
    query = sys.argv[1]
    limit = int(sys.argv[2]) if len(sys.argv) > 2 else 5
    model = sys.argv[3] if len(sys.argv) > 3 else MODEL
    os.makedirs(OUTDIR, exist_ok=True)

    or_key = _read_or_key()
    fc_key = _read_key("~/SK/OpenCode/firecrawl-api-key.txt")

    # 1) Quellen holen (laufen NICHT durch den Claude-Kontext) — Firecrawl, bei Bedarf Tavily
    print(f"[1/2] Firecrawl-Suche: {query!r} (limit {limit})", file=sys.stderr)
    treffer, fc_fehler = _firecrawl(query, limit, fc_key)
    if fc_fehler:
        print(f"      Firecrawl-FEHLER: {fc_fehler}", file=sys.stderr)
    else:
        print(f"      {len(treffer)} Quellen geholt.", file=sys.stderr)

    # Rueckfall/Ergaenzung Tavily
    tv_fehler = None
    braucht_tavily = TAVILY_MODE == "always" or (
        TAVILY_MODE == "fallback" and (fc_fehler is not None or not _brauchbar(treffer)))
    if braucht_tavily:
        grund = ("MM_TAVILY=always" if TAVILY_MODE == "always"
                 else ("Firecrawl-Fehler" if fc_fehler else "Firecrawl ohne brauchbare Treffer"))
        print(f"      -> Tavily-Rueckfall ({grund}): advanced, 20 Quellen, 3 Chunks/Quelle, Volltext",
              file=sys.stderr)
        try:
            tv_key = _read_key("~/SK/Tavily/tavily-api-key.txt")
        except OSError as e:
            tv_key, tv_fehler = None, f"Key nicht lesbar: {e}"
        if tv_key:
            tv_treffer, tv_fehler = _tavily(query, tv_key)
            if tv_fehler:
                print(f"      Tavily-FEHLER: {tv_fehler}", file=sys.stderr)
            else:
                # Duplikate per URL raus, Firecrawl behaelt den Vorrang (volle Seiten)
                bekannt = {t["url"] for t in treffer if t.get("url")}
                neu_tv = [t for t in tv_treffer if t.get("url") not in bekannt]
                treffer += neu_tv
                print(f"      Tavily lieferte {len(tv_treffer)} Treffer ({len(neu_tv)} neu).",
                      file=sys.stderr)
        else:
            print(f"      Tavily uebersprungen: {tv_fehler}", file=sys.stderr)

    if not treffer:
        return (f"Keine Quellen gefunden. Firecrawl: {fc_fehler or 'leer'} | "
                f"Tavily: {tv_fehler or ('nicht versucht' if not braucht_tavily else 'leer')}")

    with open(os.path.join(OUTDIR, "sources.json"), "w", encoding="utf-8") as fh:
        json.dump(treffer, fh, ensure_ascii=False)
    src = []
    for i, r in enumerate(treffer):
        # Herkunft mitschreiben: der Auswerter (und Frank) sehen, ob ein Beleg von Firecrawl
        # (volle Seite) oder von Tavily (Rueckfall) kam.
        src.append(f"### QUELLE {i+1} [{r['woher']}]: {r['titel']} ({r['url']})\n{(r['text'] or '')[:12000]}")
    sources = "\n\n".join(src)
    quellen_engine = "+".join(sorted({t["woher"] for t in treffer}))
    print(f"      {len(treffer)} Quellen gesamt (Herkunft: {quellen_engine}).", file=sys.stderr)

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
          f"| Kosten ${u.get('cost')} | Quellen: {len(treffer)} ({quellen_engine}) "
          f"| Thinking {len(think)} Zeichen ({OUTDIR}/thinking.txt) ---", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

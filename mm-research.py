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
(/chat/completions) mit `deepseek/deepseek-v4-flash-0731`. Anbieter-Kette seit 11.09.2026:
**Makora → Relace → DeepInfra** (`provider.order=["makora","relace","deepinfra"]`,
`allow_fallbacks=False` — OpenRouter probiert die drei der Reihe nach, aber keinen vierten).

FIRECRAWL MAXIMAL TIEF (seit 11.09.2026): /v2/search mit `limit=100` (API-Maximum), jede Seite
voll gescrapt (Markdown, nur Hauptinhalt, Werbung/Base64-Bilder raus). Kosten ~120 Credits je Suche.
Scheitert die grosse Suche (Timeout/Fehler), folgt ein zweiter Versuch mit 30 Quellen, erst dann Tavily.
Verifiziert 09.09.2026 gegen die
OpenRouter-API: Endpunkt vorhanden, `reasoning_effort` unterstuetzt, 1.048.576 Token Kontext,
$0.09/$0.195 pro Mio Token (in/out).

Der Clou: Die Roh-Quellen laufen NIE durch den teuren Claude-Kontext (Firecrawl -> hier -> DeepSeek).
Gemessen 2026-06-20: ~2.400 Claude-Token statt ~249.000 bei einem Opus-Researcher (~100x weniger),
bei besserer Ehrlichkeit. Auswerte-Token laufen separat ueber OpenRouter (pay-per-use), Quellen ueber Firecrawl-Free.

Verwendung:
    python3 mm-research.py "deine Recherche-Frage" [anzahl_quellen] [modell]
    anzahl_quellen      — Default 100 (Firecrawl-Maximum), groessere Werte werden auf 100 gekappt.
    MM_MODEL     (env) — Auswerte-Modell, Default `deepseek/deepseek-v4-flash-0731`.
    MM_PROVIDER  (env) — Anbieter-Reihenfolge, kommagetrennt, Default `makora,relace,deepinfra`.
                         LEER = kein Pin (freies Routing).
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
FC_URL = "https://api.firecrawl.dev/v2/search"
FC_URL_V1 = "https://api.firecrawl.dev/v1/search"          # Rueckfall, falls v2 zickt
FC_MAX = 100          # hoechstes `limit`, das die Firecrawl-Suche annimmt
FC_RETRY_LIMIT = 30   # zweiter Versuch, wenn 100 Seiten auf einmal scheitern (Timeout/Fehler)
MAX_PER_SOURCE = 20000     # Zeichen je Quelle an den Auswerter
MAX_TOTAL_CHARS = 2000000  # ~500k Token Deckel ueber alle Quellen (Kontext 1M)
TV_URL = "https://api.tavily.com/search"                   # Rueckfall-Suche (seit 09.09.2026)
# Wann Tavily einspringt: "fallback" = nur wenn Firecrawl nichts Brauchbares liefert (Default),
# "always" = immer beide (zusammengefuehrt), "off" = nie. Ein leeres/kaputtes Firecrawl-Ergebnis
# ist der einzige maschinell erkennbare Fall von "liefert keine richtigen Ergebnisse" — wer auf
# Nummer sicher gehen will, setzt MM_TAVILY=always.
TAVILY_MODE = os.environ.get("MM_TAVILY", "fallback").lower()
MIN_MARKDOWN = 200   # kuerzer als das gilt ein Treffer als leer (Cookie-Banner/Fehlerseite)
MODEL = os.environ.get("MM_MODEL", "deepseek/deepseek-v4-flash-0731")
# Anbieter-Kette: Makora zuerst, dann Relace, dann DeepInfra. allow_fallbacks=False, damit nicht
# still auf einen vierten Anbieter geroutet wird (Preis/Verhalten waeren sonst andere).
# MM_PROVIDER="" schaltet den Pin ganz ab (freies Routing).
PROVIDERS = [p.strip() for p in os.environ.get("MM_PROVIDER", "makora,relace,deepinfra").split(",")
             if p.strip()]
PROVIDER = " → ".join(PROVIDERS)
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
    headers = {"Authorization": f"Bearer {fc_key}", "Content-Type": "application/json"}
    # Maximal tief: jede gefundene Seite wird voll gescrapt (Markdown, nur Hauptinhalt).
    scrape = {"formats": ["markdown"], "onlyMainContent": True, "blockAds": True,
              "removeBase64Images": True}
    body = {"query": query, "limit": limit, "sources": ["web"], "ignoreInvalidURLs": True,
            "timeout": 300000, "scrapeOptions": scrape}
    try:
        fc = _post(FC_URL, headers, body, timeout=330)
    except urllib.error.HTTPError as e:
        fehler = f"HTTP {e.code}: {e.read().decode('utf-8', 'replace')[:200]}"
        if e.code not in (400, 404):
            return [], fehler
        # v2 lehnt ab -> einmal mit dem alten v1-Schema versuchen
        try:
            fc = _post(FC_URL_V1, headers, {"query": query, "limit": limit, "scrapeOptions": scrape},
                       timeout=330)
        except Exception as e2:
            return [], f"{fehler} | v1: {type(e2).__name__}: {str(e2)[:160]}"
    except Exception as e:
        return [], f"{type(e).__name__}: {str(e)[:160]}"
    data = fc.get("data") or []
    if isinstance(data, dict):   # v2: {"web": [...], "news": [...]}
        data = [r for liste in data.values() if isinstance(liste, list) for r in liste]
    out = []
    for r in data:
        out.append({"titel": r.get("title") or (r.get("metadata") or {}).get("title", ""),
                    "url": r.get("url") or (r.get("metadata") or {}).get("sourceURL", ""),
                    "text": r.get("markdown") or r.get("content") or r.get("description") or "",
                    "woher": "Firecrawl"})
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
    limit = min(int(sys.argv[2]), FC_MAX) if len(sys.argv) > 2 else FC_MAX
    model = sys.argv[3] if len(sys.argv) > 3 else MODEL
    os.makedirs(OUTDIR, exist_ok=True)

    or_key = _read_or_key()
    fc_key = _read_key("~/SK/OpenCode/firecrawl-api-key.txt")

    # 1) Quellen holen (laufen NICHT durch den Claude-Kontext) — Firecrawl, bei Bedarf Tavily
    print(f"[1/2] Firecrawl-Suche: {query!r} (limit {limit})", file=sys.stderr)
    treffer, fc_fehler = _firecrawl(query, limit, fc_key)
    if fc_fehler and limit > FC_RETRY_LIMIT:
        print(f"      Firecrawl-FEHLER bei {limit} Quellen: {fc_fehler} -> neuer Versuch mit "
              f"{FC_RETRY_LIMIT}", file=sys.stderr)
        treffer, fc_fehler = _firecrawl(query, FC_RETRY_LIMIT, fc_key)
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
    gesamt = 0
    for i, r in enumerate(treffer):
        # Herkunft mitschreiben: der Auswerter (und Frank) sehen, ob ein Beleg von Firecrawl
        # (volle Seite) oder von Tavily (Rueckfall) kam.
        stueck = (f"### QUELLE {i+1} [{r['woher']}]: {r['titel']} ({r['url']})\n"
                  f"{(r['text'] or '')[:MAX_PER_SOURCE]}")
        if gesamt + len(stueck) > MAX_TOTAL_CHARS:
            print(f"      Kontext-Deckel erreicht: {len(treffer) - i} Quellen nicht mitgegeben.",
                  file=sys.stderr)
            break
        gesamt += len(stueck)
        src.append(stueck)
    sources = "\n\n".join(src)
    quellen_engine = "+".join(sorted({t["woher"] for t in treffer}))
    print(f"      {len(treffer)} Quellen gesamt (Herkunft: {quellen_engine}).", file=sys.stderr)

    # 2) DeepSeek V4 Flash (reasoning high) — Quellen quellentreu auswerten
    print(f"[2/2] {model} @ {PROVIDER or 'freies Routing'} (effort {EFFORT}) wertet aus...", file=sys.stderr)
    # Gemessen 2026-09-09: "Nenne pro Aussage die Quelle" liess das Modell "(Quelle 3)" schreiben — einen
    # Verweis in die nummerierte Liste, die nur im Prompt steht. Die Antwort war dann OHNE sources.json
    # nicht ueberpruefbar (0 URLs in allen 7 Antworten eines Testlaufs, waehrend die Engines B und C
    # ~37-39 URLs je Antwort lieferten). Siehe bugs/agents/multi-agent-interop.md §9.
    prompt = ("Du bist ein Recherche-Auswerter. Beantworte AUSSCHLIESSLICH auf Basis der folgenden "
              "Quellen die Frage. Wenn etwas NICHT in den Quellen steht oder widerspruechlich ist, "
              "sage das ausdruecklich — erfinde nichts. Nenne pro Aussage die Quelle MIT VOLLER URL "
              "(nicht nur die Quellennummer) und dahinter die Herkunft [Firecrawl] bzw. [Tavily], "
              "damit die Antwort ohne die Quellenliste nachpruefbar bleibt.\n\n"
              f"FRAGE: {query}\n\n=== QUELLEN ===\n{sources}")
    body = {"model": model, "max_tokens": 30000,
            "reasoning": {"effort": EFFORT},   # max Thinking auf /chat/completions
            "messages": [{"role": "user", "content": prompt}]}
    if PROVIDERS:
        body["provider"] = {"order": PROVIDERS, "allow_fallbacks": False}
    try:
        d = _post(OR_URL,
                  {"Authorization": f"Bearer {or_key}", "Content-Type": "application/json",
                   "HTTP-Referer": "https://github.com/Pepsi1978/proggs", "X-Title": "proggs-mm-research"},
                  body, timeout=600)
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

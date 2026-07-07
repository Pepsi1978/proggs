# Memory-Backends (self-hosted) im Vergleich — Best Practices (Stand 2026-06-21, Junk-Nachtrag 2026-06-22)

> Teil des Second-Brain-Wissens. Welches self-hostbare Memory-Backend für "ein Server, viele Clients
> (Claude Code/OpenCode via MCP + eigene Apps via REST) + heterogene persönliche Daten"? Quellen: `extern`
> (Backend-Vergleiche, Mem0/Letta/Zep-Doku 2026). **Löst einen Widerspruch zum bestehenden
> [[../opencode/self-hosted-memory-server]] auf (supermemory-Status).** Schwester: [[referenz-architekturen]], [[datenmodell]].
>
> **NACHTRAG 2026-06-22 (Junk-/Quality-Gate-Fokus, Frank-Anliegen):** Eine vertiefte Eskalations-Recherche
> (OpenRouter `:online`, 4 Deep-Dives) zur Frage „gibt es eine Alternative, die WENIGER MUELL produziert" —
> Ergebnis in **§6**. Kurz: mem0 bleibt für Multi-Client/MCP die einfachste Wahl, hat aber bestaetigt **KEIN
> Quality-Gate** (Junk-Problem strukturell). Wer Junk-Vermeidung priorisiert: **Cognee** (nutzt Qdrant nativ +
> ECL/Ontologie) oder ein **eigener schlanker Layer mit Reject-Gate** sind die ernsthaften Kandidaten.

> **⚠️ ÜBERHOLT (2026-06-23): mem0 wurde KOMPLETT VERWORFEN.** Es extrahierte/dichtete beim Speichern
> (Gemini-Halluzinationen, kein Quality-Gate). Das Gehirn ist jetzt ein **wortwoertlicher 1:1-Dokument-
> Speicher** (`brain-api`: qdrant-client + Gemini-Embedding direkt, KEIN mem0, KEIN LLM im Speicher).
> Diese Datei bleibt als **Backend-Vergleich/Research** wertvoll — die **as-built**-Wahrheit steht in
> [[speicher-schema-1zu1]]. Der in §6 genannte "eigene schlanke Layer" ist genau das, was gebaut wurde.

---

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Frage | Antwort |
|-------|---------|
| **Primär-Empfehlung** | **Mem0** — Apache-2.0, self-hostbar, **MCP-nativ**, REST/SDK (Python+JS), Multi-Tenant (user/agent/run/app), größte Community, AWS-Memory-Standard. Erfüllt ALLE Anforderungen unstrittig |
| **Runner-up** | **Zep / Graphiti** — temporaler Knowledge-Graph (beste Benchmarks 94,7 LoCoMo), MCP-nativ; ABER nur **Graphiti** ist OSS, das volle Zep ist Managed |
| **OSS-Alternative** | **Letta** (MemGPT) — Apache-2.0, OS-Tiered-Memory, CLI; ABER **kein MCP** + Agent-Runtime-Lock-in (bringt eigenen Loop mit → passt schlecht zu Claude Code/OpenCode) |
| **supermemory** | **Widerspruch ungeklärt** (siehe §3): A2-Lauf sagt "nicht OSS, kein MCP"; bestehender Bauplan sagt "self-hostbar Single-Binary + MCP". Vor Wahl Lizenz/MCP LIVE verifizieren |
| Vektor-Backend darunter | **Qdrant** (Mem0 unterstützt 20 Backends) |
| Embeddings lokal | FastEmbed (on-device, Privacy) bzw. BGE-M3 (DE/EN) — vor Einsatz testen |
| Ollama-Extraktion lokal? | für KEINES der Tools in den Quellen sauber belegt — vor Datenhoheits-Anspruch prüfen |

---

## 0. Praxis-Fallen aus dem echten Aufbau (2026-06-22, Mem0 2.0.7 + Gemini + Qdrant LIVE)

> Beim tatsaechlichen Aufbau des Gehirns aufgetreten und geloest (funktionserhaltend). Quelle: eigener
> Vorfall. Gegenstueck-Almanach fuer die Server-Ebene: `bugs/server/self-hosted-ai-agent-server.md`.

| # | Falle | Symptom | Fix (so von vornherein richtig) |
|---|-------|---------|----------------------------------|
| 1 | **Qdrant `api_key` erzwingt TLS** | `[SSL: WRONG_VERSION_NUMBER]` beim ersten Qdrant-Call | `qdrant-client` setzt bei gesetztem `api_key` automatisch `https=True`. Gegen ein Klartext-HTTP-Qdrant (Standard self-hosted) deshalb **explizite `url="http://host:6333"`** an Mem0s `vector_store.config` geben statt `host`/`port` — dann kein TLS. |
| 2 | **Mem0 2.x Such-API** | `ValueError: Top-level entity parameters {'user_id'} are not supported in search()` | In Mem0 2.x brauchen `search()` und `get_all()` **`filters={"user_id": "…"}`** + **`top_k=`** (nicht mehr `user_id=`/`limit=`). `add()` nimmt weiterhin `user_id=`/`metadata=`/`infer=`. |
| 3 | **Gemini-Embedding-Dimension** | Dimensions-Mismatch / falsche Collection-Groesse | Mem0s Gemini-Embedder defaultet auf **768** dims (`embedding_dims or output_dimensionality or 768`). Gewuenschte Groesse explizit setzen: `embedder.config.embedding_dims = N` **UND** `vector_store.config.embedding_model_dims = N` (gleicher Wert!). Bei `gemini-embedding-001`: 768/1536/3072 — wir nutzen **1536**. |
| 4 | **Bind-Mount + nicht-root-Container** | `Permission denied` beim Log-Schreiben in gemountetes Verzeichnis | Laeuft der Container als nicht-root (uid 10001), gehoert das per Bind-Mount eingehaengte Host-Verzeichnis trotzdem root → `chown -R 10001:10001 <hostdir>`. (Graceful Fallback auf stdout, aber Datei-Log will man fuer `tail -f`.) |

**Verifizierte Modellnamen (Live-Liste des Keys, 2026-06-22):** `gemini-3.1-flash-lite` (stabil, ohne
`-preview`) und `gemini-embedding-001` existieren beide. IDs pinnen, nicht `-latest`/`-preview` raten
(Gemini-404-Falle, `bugs/apis/google-gemini-api.md` #5).

---

## 1. Anforderungs-Matrix (`extern`, A2-Lauf)
Für Franks Fall (1 Server, viele Clients, heterogene Personendaten, self-host):

| Anforderung | Mem0 | Letta | Zep | Cognee | Supermemory |
|-------------|------|-------|-----|--------|-------------|
| Self-hostbar | ✅ Apache-2.0 | ✅ Apache-2.0 | ⚠️ nur Graphiti OSS | ✅ OSS | ⚠️ strittig (§3) |
| Native MCP | ✅ | ❌ | ✅ | ❌ | ⚠️ strittig (§3) |
| REST/SDK für Apps | ✅ (Python+JS) | ❌ (Plattform, kein Memory-Layer) | implizit | nicht belegt | ✅ API (Bauplan) |
| Multi-Tenant-Scopes | ✅ user/agent/run/app | nicht beschrieben | User-Profile | nicht beschrieben | User-Profile |
| Reife/Community | ✅ größte (~41-48k Stars, AWS) | ~23k Stars | ~27k Stars | ~17,6k Stars | klein, jung |

## 2. Steckbriefe (`extern`)
- **Mem0:** "drop-in memory layer", zweiphasig (Extraktion + Update, ADD-only), Hybrid aus Vektor+Graph+KV,
  Entity-Linking eingebaut, 20 Vektor-Backends, 3 Voice-Integrationen (ElevenLabs/LiveKit/Pipecat). Benchmarks
  92,5 LoCoMo / 94,4 LongMemEval. **Caveats:** Graph-Memory (Multi-Hop-Beziehungen) im Managed-Pro auf 249 $/Mo
  beschränkt — OSS-Graph ist einfacher; in der Vergangenheit Reife-Probleme gemeldet (7-10 s Latenz, "week-long
  500 errors"); kein natives bi-temporales Modell.
- **Zep/Graphiti:** temporaler KG (Graphiti-Engine), Fakten mit Gültigkeits-Fenstern — stärkste Modellierung
  für sich ändernde Personendaten (Adresse/Job/Beziehungen). Benchmarks 94,7 LoCoMo / 90,2 LongMemEval. **Caveat:**
  nur Graphiti OSS; Latenz/Immediate-Retrieval-Probleme gemeldet (Graph-Bau läuft im Hintergrund).
- **Letta (MemGPT):** OS-Tiered (Core/Recall/Archival), Agent self-edits via Tools, ADE-Debug-Tool. **Caveat:**
  kein MCP, ist ein Agent-Runtime → Claude Code/OpenCode bringen ihren eigenen Loop mit (Lock-in).
- **Cognee:** semantischer KG on-the-fly, Poly-Store, remember/recall/forget/improve. **Caveat:** kein MCP.
- **(mcp-memory-service, basic-memory, txtai):** im A2-Lauf nicht abgedeckt; basic-memory + mcp-memory-service
  sind im bestehenden [[../opencode/self-hosted-memory-server]] §1 als schlanke Alternativen gelistet (SQLite-vec/
  ONNX, MCP) — gut für Claude-Code-only, schwächer für "viele Clients + eigene Apps".

## 3. ⚠️ Der supermemory-Widerspruch — ehrlich aufgelöst
Zwei Recherchen widersprechen sich:
- **A2-Lauf (2026-06-21, Firecrawl):** "supermemory ist **nicht Open Source** und hat **kein natives MCP**"
  (basiert auf einer generischen Vergleichstabelle, Quelle 7).
- **Bestehender Bauplan [[../opencode/self-hosted-memory-server]] (2026-06-19, 6-Researcher-Schwarm):**
  "supermemory **self-hosted = Single-Binary (MIT)**, **MCP-nativ** (`/mcp`), `npx supermemory local`, Port 6767";
  Plugin `opencode-supermemory` verlangt Pro-Plan → remote-MCP-Weg.
- **Blueprint-Lauf (2026-06-21):** supermemory = "Memory-API + Graph, user-profiles + RAG über einen Pool"
  (kein OSS/MCP-Detail).

**Wahrscheinliche Auflösung:** Der dedizierte 6-Researcher-Bauplan ist spezifischer und neuer zu genau diesem
Tool; die generische A2-Vergleichstabelle meint vermutlich supermemorys **Cloud-Produkt** (das ist proprietär)
und nicht den **self-hosted-Fork** (MIT). Die Begriffe "supermemory" decken also zwei Dinge ab: gehostetes
SaaS (proprietär) vs. self-host-Binary (MIT). **Aber: ungeklärt → NICHT blind glauben.** Vor einer Wahl von
supermemory die npm-/Repo-Lizenz UND den MCP-Self-Host-Pfad LIVE verifizieren (Nacharbeit, siehe unten).

**Konsequenz für Frank:** Unabhängig vom supermemory-Streit ist **Mem0 die sichere Wahl** — sein OSS-Status
(Apache-2.0) + MCP + REST + Multi-Scope ist von MEHREREN Quellen unstrittig belegt.

## 4. Empfehlung
1. **Mem0** als zentraler Memory-Server (Apache-2.0, MCP für Claude Code/OpenCode, REST für eigene Apps,
   user/agent/run/app-Scopes für Franks Kategorien). Darunter **Qdrant** als Vektor-Backend.
2. **Zep/Graphiti dazunehmen**, falls temporale Tiefe wichtig wird (Fakten, die sich über Zeit ändern).
3. **supermemory** nur, wenn das einfachere Single-Binary-Self-Hosting den Ausschlag gibt UND §3 vorher
   verifiziert ist.
4. **Letta** eher nicht für diesen Multi-Client-Fall (Runtime-Lock-in, Memory-SDK braucht Cloud). **Cognee** ist —
   anders als hier 2026-06-21 notiert — **doch MCP-fähig und Qdrant-nativ** (siehe Korrektur in §6) und damit ein
   ernsthafter Junk-ärmerer Kandidat, wenn ein Backend-Wechsel in Frage kommt.

## 5. Embeddings & Ollama-Frage (`extern`)
- **Lokale Embeddings:** FastEmbed (on-device, kein API-Call) für Privacy; BGE-M3 für DE/EN (siehe
  [[orchestrator-und-suche]] §2.6, dort als zu-testen markiert).
- **Lokale LLM-Extraktion (Ollama):** für KEINES der Tools sauber quellenbelegt. Der bestehende Bauplan
  ([[../opencode/self-hosted-memory-server]]) zeigt für supermemory den Ollama-Umweg (`OPENAI_BASE_URL` auf
  lokales Ollama) — analog bei Mem0 prüfen, wenn die Extraktion lokal laufen soll (sonst läuft sie per Default
  über eine externe LLM-API).

## 6. Junk-/Quality-Gate-Perspektive (Vertiefung 2026-06-22, Firecrawl + OpenRouter-Eskalation)

> Andere Frage als §1-§4 (dort: Multi-Client/MCP). Hier: **Welches Backend produziert am wenigsten Muell/
> Halluzinationen, ist kostenlos+self-hostbar und passt zu unserem Qdrant-Stack?**

**Ehrliche Meta-Erkenntnis zuerst:** Es gibt **2026 KEINE oeffentliche, von allen Seiten akzeptierte Junk-/
Halluzinations-Quote** fuer diese Frameworks. Alle Vergleiche sind **Retrieval-Benchmarks** (LongMemEval, LoCoMo,
HotPotQA) oder **strukturelle/qualitative** Argumente — nicht „% Junk". „X macht weniger Muell als mem0" ist also
nirgends quantitativ belegt; man kann nur ueber die **Architektur** argumentieren (hat ein System einen Mechanismus,
der Junk strukturell verhindert?). Der mem0-Junk-Befund selbst (Audit #4573, 97,8 %) bleibt der einzige harte Wert.

| Kandidat | Anti-Junk-Mechanismus (strukturell) | Qdrant? | Kostenlos self-host | Aufwand single-user | MCP |
|----------|--------------------------------------|---------|---------------------|---------------------|-----|
| **mem0** (Ist-Zustand) | ❌ KEIN Quality-Gate; v3 single-pass ADD-only (REJECT strukturell unmoeglich); „domain-aware triage"+„Auto-dream" beworben aber kein Pre-Storage-Gate | ✅ nativ (unser Stack) | ✅ Apache-2.0 (Self-Host-Doku duenn) | laeuft schon | ✅ (unser sb-mcp) |
| **Cognee** | ⚠️ ECL-Pipeline (Extract→Cognify→Load) + RDF-Ontologie → strukturiert/dedupliziert **implizit**; expliziter Confidence-Filter NICHT dokumentiert | ✅ **nativ** (sogar TurboQuant-Integration „8× weniger Vektor-RAM") | ✅ Apache-2.0-Kern (**Open-Core**: manche Features Cloud), pip-install, lokal (SQLite+LanceDB+Kuzu) | mittel (Python-only SDK; Ops selbst) | ✅ MCP-Server (Cursor/Claude Desktop/Cline) |
| **Zep/Graphiti** | ✅ **staerkster**: bi-temporal (`valid-at`/`invalid-at`) → veraltete Fakten werden automatisch invalidiert statt re-used | ❌ braucht **Neo4j** (primary)/FalkorDB/Neptune — NICHT Qdrant | ⚠️ nur **Graphiti**-Engine Apache-2.0; FalkorDB ist source-available (SSPL-nah) | hoch (Graph-DB-Overhead, DB-Wechsel) | ✅ |
| **Letta/MemGPT** | ⚠️ Sleep-Time-Compute (async Aufraeum-Agent, `rethink_memory`-Loop → „cleaner memories") ABER „unpredictability of self-editing memory" kritisiert; eventually-consistent | ❓ unklar (Archival = „Vector DB", Qdrant nicht belegt) | ⚠️ Platform Apache-2.0 self-host; das schlanke **AI-Memory-SDK braucht Letta Cloud** | hoch (eigener Agent-Server + Postgres) | (Platform) |
| **Eigener Layer auf Qdrant** | ✅ **echtes Reject-Gate moeglich** (Extraction-Agent → Confidence-Score → REJECT vor Write + Dedup) — genau das, was mem0 fehlt | ✅ direkt | ✅ voll (nur Eigenbau) | **Tage** (Atlan: 1-5 Tage), dann Wartung | selbst zu bauen (haben wir schon: sb-mcp) |

**Zwei Korrekturen zur §1-§4-Tabelle (heutige Recherche):** (1) **Cognee HAT einen MCP-Server** (§2 sagte „kein MCP" —
das ist ueberholt). (2) **Cognee nutzt Qdrant nativ** (alter GitHub-Issue #1865 „kein Qdrant" ist erledigt; es gibt
sogar eine TurboQuant-Integration). Damit ist Cognee der einzige der „graph-artigen" Kandidaten, der **auf unserem
bestehenden Qdrant** laeuft.

**Empfehlung fuer Franks Kriterien (weniger Junk, kostenlos, zuverlaessig, auf Qdrant):**
1. **Pragmatisch (kleinster Schritt):** Bei **mem0 bleiben**, aber den fehlenden Quality-Gate **selbst davorsetzen** —
   ein schlanker Reject/Confidence-Filter im `sb-mcp`/`mem0-api` VOR `add()` (Cognify-Light). Loest das Junk-Problem
   an der Wurzel, ohne Stack-Wechsel. Geringster Aufwand, volle Kontrolle.
2. **Wenn Backend-Wechsel ok (bester Junk-Fit auf Qdrant):** **Cognee** — ECL+Ontologie strukturiert Fakten vor dem
   Graph, nutzt unser Qdrant nativ, Apache-2.0-Kern, hat MCP. Tradeoff: Open-Core (Cloud-Features), Python-only, kein
   belegter expliziter Confidence-Filter, kein UI.
3. **Wenn maximale strukturelle Korrektheit ueber Zeit zaehlt:** **Zep/Graphiti** (bi-temporal) — aber Neo4j statt
   Qdrant = schwererer Stack, DB-Wechsel.
4. **Eigener Layer** ist die einzige Option mit einem ECHTEN Reject-Gate und voller Datenhoheit — fuer einen
   Single-User in Tagen baubar; lohnt, wenn Junk-Qualitaet das oberste Ziel ist und mem0/Cognee nicht ueberzeugen.
5. **Letta** eher nicht (Cloud-Abhaengigkeit beim Memory-SDK, Qdrant unklar, „unpredictable").

**Fazit:** Es gibt kein „klar besseres, kostenloses, müll-freies" Drop-in. Der groesste Hebel ist **nicht der
Backend-Wechsel, sondern ein Quality-Gate VOR dem Speichern** — entweder selbst vor mem0 gesetzt (Weg 1) oder
inhaerent in Cognee (Weg 2). Frank entscheidet zwischen „mem0 + eigenes Gate" (wenig Aufwand) und „Cognee" (mehr
Umbau, aber Quality strukturell eingebaut).

## 7. Quality-Gate-Bausteine — gibt es fertige Teile zum Davorsetzen? (Recherche 2026-06-22, Firecrawl + OpenRouter-Eskalation)

> Franks Folgefrage: „Gibt es schon sehr gute Open-Source-Quality-Gates, die wir einfach davorsetzen können, oder
> muss man das selbst bauen?" Antwort: **teils-teils — das schwere Stueck (Halluzinations-/Grounding-Pruefmodell)
> gibt es fertig; ein komplettes Memory-Gate-Drop-in gibt es NICHT.**

**(a) Fertige Pruef-Bausteine (man nimmt sie, baut sie NICHT selbst):**

| Tool | Lizenz | Score | Lokal/Latenz | Eignung als Pre-Storage-Gate |
|------|--------|-------|--------------|------------------------------|
| **Vectara HHEM-2.1-Open** ⭐ | **Apache 2.0** (frei, auch kommerziell) | kalibriert 0–1 (`0.8` = 80 % faktentreu) | klein, **<1,5 s @2k Tokens** (CPU/RTX 3080); DE/EN/FR | **bester Fit**: Paar-Input (`premise`=Quelle, `hypothesis`=Fakt) → Score → Reject. Spezialisierter Factual-Consistency-Detector. ⚠️ niedriger Recall → laesst manche Halluzination durch |
| **Patronus Lynx 8B** | ⚠️ **CC BY-NC 4.0 (NICHT-kommerziell!)** | JSON `SCORE`+`REASONING`, PASS/FAIL | via **Ollama** (GGUF Q4); genauer, schwerer/langsamer | gut als 2. Stufe fuer Grenzfaelle; fuer Franks **privates** Gehirn lizenz-ok, fuer kommerziell NICHT nutzbar |
| **RAGAS** | OSS (Lizenztext n. belegt) | `faithfulness`/context-precision/recall 0–1 | lokal | nutzbar, aber primaer RAG-Eval-Framework (schwerer als ein reines Gate) |
| **DeepEval** | OSS (Lizenztext n. belegt) | `hallucination`/answer-relevancy 0–1; Ollama-Support | lokal, pytest/code-first | gut fuer Inline-Gate auf Python-Single-Host |
| Bespoke MiniCheck / Guardrails AI | n. belegt | n. belegt | n. belegt | in den Quellen nicht abgedeckt → bei Bedarf GitHub direkt pruefen |

**(b) Komplettes Memory-Gate-Drop-in (extract→verify→REJECT/ADD in einem Paket): NEIN.** Zwei unabhaengige
Researcher: existiert 2026 nicht als fertiges OSS-Teil. **LangMem** = nur Extract-Tool, ausdruecklich „no built-in
evaluation" (kein Verify/Reject). **Blockify** (iternal.ai) = kommerziell + nur Dedup/Distillation/IdeaBlocks, kein
belegter Reject. **Cognee cognify** strukturiert implizit, aber kein expliziter Grounding-Reject belegt.

**(c) Was man also selbst verdrahtet (wenig Code, ~50 Zeilen — KEIN Eigenbau des Pruefmodells):**
```
LLM extrahiert Fakt(en)  →  HHEM(premise=Quell-Chunk, hypothesis=Fakt) → Score
   Score < Schwelle   → verwerfen (NICHT in mem0)
   Score >= Schwelle  → Dedup (Cosine vs. vorhandene) → mem0.add()
   (Grenzband optional an Lynx 8B via Ollama)
```
- **Schwellen sind nirgends dokumentiert** → selbst auf einem kleinen Hold-out (RAGTruth/HaluBench) kalibrieren;
  Start z. B. `reject < 0.5`, `borderline 0.5–0.7`, `accept >= 0.7` (HHEM-Score ist kalibriert interpretierbar).
- **Kein End-to-End-Cookbook** „HHEM/Lynx als Memory-Write-Gate vor Vektor-DB" existiert — selbst komponieren.
- Zusatz gegen mem0s Haupt-Junk (Boot-File-Restating 52,7 %): ein simpler Vorfilter, der System-Prompt-/Tool-Config-
  Text gar nicht erst extrahieren laesst, faengt den Loewenanteil schon VOR dem HHEM-Check.

**Konsequenz fuer Weg 1 („mem0 + eigenes Gate"):** ist damit klar der pragmatischste Weg — **HHEM-2.1-Open
(Apache 2.0)** als fertiges Pruefmodell + ~50 Zeilen Verdrahtung im `mem0-api` vor `add()`. Kein Stack-Wechsel,
volle Datenhoheit, geringer Aufwand.

## Nacharbeit (offene Verifikation)
- **(Gate-Recherche 2026-06-22)** HHEM-Reject-Schwelle auf einem kleinen eigenen Hold-out kalibrieren (kein Default belegt).
- **(Gate-Recherche 2026-06-22)** Bespoke MiniCheck + Guardrails AI Lizenz/Score direkt am GitHub-Repo pruefen (Quellen deckten sie nicht ab).
- **(Junk-Recherche 2026-06-22)** Cognee-Quellcode/Doku auf einen ECHTEN Pre-Storage-Filter (`filter`/`score`/
  `confidence`/`quality` in der Cognify-Pipeline) pruefen — die Sekundaerquellen belegen ihn NICHT explizit.
- **(Junk-Recherche 2026-06-22)** Falls Weg 1 (mem0 + eigenes Gate): Confidence-Gate-Pattern als kleinen Reject-Filter
  vor `add()` im `mem0-api` prototypen (Extraction → Score < Schwelle → verwerfen + Boot-File/System-Prompt-Filter).
- supermemory Lizenz + MCP-Self-Host LIVE prüfen (npm `supermemory`, GitHub-Repo, Doku) → §3 auflösen.
- Mem0 lokale Ollama-Extraktion + OSS-Graph-Fähigkeit (vs. Pro-Tier) verifizieren.
- Ollama-Embedding-/Extraktions-Pfad pro Tool testen (Datenhoheit).

## Quellen (`extern`, 2025-2026)
Backend-Vergleich A2-Lauf (Mem0/Letta/Zep/Cognee/Supermemory-Matrix); Mem0-Blog "State of AI Agent Memory 2026";
Vectorize "Mem0 vs Letta"; EverMind "Best OSS Agent Memory 2026". Ergänzend/teils widersprechend:
`best-practices/opencode/self-hosted-memory-server.md` (supermemory-Self-Host-Bauplan, 2026-06-19).

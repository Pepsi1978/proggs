# Second Brain — Memory-Evolution: Kurzzeit/Langzeit, Konsolidierung, Proaktivität (Volltext)

> Stand: 2026-07-04 · Recherche Engine A (Firecrawl+MiniMax, 7 Themen + 2 Retries, ~44 Quellen)
> Version-Anker: Qdrant 1.18.x, Gemini API (gemini-embedding-001), FastAPI, Claude Code CLI (2026-07)
> Kurzcheck: `memory-evolution-2026-kurzcheck.md` · Abgeleiteter Plan: `second-brain-server/LEVEL2-FEATURES-PLAN.md`
> Rohdaten des Laufs: `~/.research-swarm/sb-level2/` (answer-1..7 + retry-sleeptime + retry-briefings)

## 1. Memory-Architekturen (MemGPT/Letta, Zep/Graphiti, Cognee, mem0)

- **Letta/MemGPT hat die klarste Kurzzeit/Langzeit-Trennung:** Memory-Blocks (Core Memory) =
  beschriftete Textfelder mit festem Zeichen-Limit, liegen DAUERHAFT im Kontextfenster, der Agent
  liest/editiert sie per Tool-Calls (`core_memory_replace`, `memory_insert`, `memory_rethink`).
  Daneben: Archival Memory (Vektor-Store, on-demand) + Recall Memory (Nachrichten-Historie) +
  optional Filesystem-Surface. Quelle: Letta-Blog/Forum, Code-Pointer-Substack (2026-05-28).
- **Bekannte Letta-Schwäche:** Ob ein veralteter Fakt ersetzt wird, hängt allein daran, ob das LLM
  den Widerspruch bemerkt und die Funktion aufruft — alter Wert wird spurlos überschrieben.
  Gegenmittel: bi-temporale Felder (s.u.) oder Änderungs-Log.
- **Zep/Graphiti = bi-temporaler Knowledge Graph:** Jede Fact-Edge trägt Valid Time
  (`valid_at`/`invalid_at` — wann in der Welt wahr) UND Transaction Time (`created_at`/`expired_at` —
  wann das System es wusste). Widerspruch → alte Edge wird GESTEMPELT, nie gelöscht. Teuer:
  >600k Token pro Konversation (vs. ~1,8k bei mem0); Retrieval direkt nach Ingest oft erst nach
  Stunden Hintergrund-Verarbeitung korrekt.
- **mem0: Drei-Ebenen-Modell** (Session/User/Agent-Memory) + Vier-Operationen-Update
  **ADD/UPDATE/DELETE/NOOP** (Umzug Mumbai→Bangalore: alter Fakt wird gelöscht, neuer angelegt).
- **⭐ mem0 v3 OSS hat den Graph-Layer ENTFERNT** (PR #4805, gemerged 2026-04-14): der Graph
  rechtfertigte die Kosten selten (LOCOMO: Mem0g 68,44 % vs. Mem0 67,13 %, aber ~3x langsamer,
  ~2x teurer). Ersatz: **Entity-Linking** — spaCy-Entities in separater `{collection}_entities`
  Vektor-Collection, `linked_memory_ids` als Hub-and-Spoke, Score-Fusion semantic+BM25+entity-boost.
  → Für kleine persönliche Basen gilt das erst recht: leichtes Entity-Linking statt Voll-Graph.
- **Benchmark-Randnotiz:** Lettas "Is a Filesystem All You Need?" (2025-08) erreichte 74,0 % auf
  LOCOMO mit reinem Filesystem-Ansatz — mehr als Mem0g (68,5 %). Einfache Strukturen schlagen
  komplexe Graphen bei persönlichen Daten häufig.
- **Governance-Lücke (alle Frameworks):** KEINES liefert eine Diff-and-Approve-Schicht vor
  Long-Term-Writes (memorywire-Paper, arXiv 2606.01138). Wer Auditierbarkeit will, baut selbst.
- **Cognee-Falle:** interne `data_id`-UUIDs werden über die Public-API nicht exponiert →
  Einzel-Löschung von außen unmöglich. Cognee-Konsolidierungsmechanik ist undokumentiert.

## 2. Sleep-time Compute / Konsolidierung

- **Zwei-Agenten-Muster (Letta Sleep-Time):** Primary-Agent (antwortet, darf Core Memory NICHT
  editieren) + Sleep-Time-Agent (verwaltet asynchron beide Gedächtnisse). Vorteil: Memory-Pflege
  blockiert nie den Antwort-Pfad ("anytime": Primary liest jederzeit den aktuellen Stand).
- **Asymmetrische Modelle:** Sleep-Time-Agent latenzunkritisch → stärkeres/teureres Modell erlaubt
  (Empfehlung im Letta-Blog: schnelles Modell für Antworten, starkes für Konsolidierung).
  Frequenz-Parameter steuert Token-Verbrauch.
- **Eviction/Summarization:** Bei vollem Kontext nur ~70 % der Nachrichten evicten (Kontinuität);
  rekursive Summarization — Älteres wird zusammen mit bestehenden Summaries progressiv komprimiert.
- **Decay-Modell (arXiv 2404.00573, Hou et al. / MemoryBank Zhong 2023):** Konsolidierungsgrad aus
  kontextueller Relevanz + verstrichener Zeit + Recall-Frequenz. Recall-Probability nach Zielske
  (1959) mit Recall-Rate r und Decay-Gradient g. **Jeder Abruf verstärkt** (+1 Strength, Zeit-Reset).
  **Bewusst KEIN Komplett-Vergessen** — Score fällt nie auf null, Erinnerungen bleiben reaktivierbar.
- **Nicht dokumentiert (Eigenbau nötig):** Widerspruchs-Erkennung zwischen Einträgen, konkrete
  Nightly-Job-Pipelines auf Qdrant, formales Importance-Scoring.

## 3. Proaktives Gedächtnis / Ambient Agents ("Unterbewusstsein")

- **Paradigma:** Pull ("Frag mich") → Push ("Ich melde mich, wenn es relevant ist"). Ambient Agents
  abonnieren Ereignis-Ströme statt auf Prompts zu warten.
- **Per-Turn-Pipeline (Hermes/NeuroLoop-Muster):** vor JEDEM LLM-Call: Signale erkennen (Regex-
  Detektoren, bewusst NICHT ML — deterministisch, keine Halluzinations-False-Positives) → passende
  Daten holen (parallel) → Memory lesen → System-Prompt zusammensetzen. "Domain-aware RAG without
  embeddings": erkannte Signal-Kategorien triggern GEZIELTE Fetches.
- **⭐ Display-Layer ≠ System-Layer:** Nutzer sieht saubere Antwort; das LLM bekommt unsichtbar
  Verhaltens-Guidance + Live-Daten + signal-gematchten Kontext. "The agent knows more than it shows"
  — DAS Kern-Muster fürs Unterbewusstsein-Gefühl.
- **Drei Interaktionsmuster (LangChain):** Notify (informieren) / Question (nachfragen) /
  Review (Freigabe einholen) — proaktive Agenten handeln nie eigenmächtig darüber hinaus.
- **Temporal-Constraint-Strategie (ProAgent, arXiv):** Drossel gegen Überladung — belegt
  **+38,9 % Nutzerzufriedenheit**, +33,4 % Proaktiv-Präzision, 1,79x weniger Memory.
- **Resilienz-Prinzipien für Always-on:** 10s-Timeout je externem Call, Fehler still überspringen,
  Cache-Miss → Background-Build, fehlende Memory = No-op.

## 4. Automatisches Mitlernen aus Coding-Sessions

- **claude-engram (GitHub 20alexl/claude-engram):** Hook-Daemon (pre/post edit, bash, prompt, read,
  failure als dünne Dispatcher an einen warmen Scorer-Server), JSONL-Session-Mining mit
  **Schema-Canary** (erkennt Claude-Code-Log-Format-Änderungen und warnt beim Session-Start),
  append-aware Re-Mining mit Watermarks, monatliche .npy-Embedding-Shards (statt einer ewig
  wachsenden Matrix), Checkpoint/Handoff-Ring, MCP-Server-Anbindung.
- **Anthropic-Harness-Muster:** drei geteilte Artefakte zwischen Sessions — Feature-Liste (JSON),
  Init-Script, **`claude_progress`-Datei** (am Session-Ende aktualisierte Zusammenfassung) +
  Git-Commit als Save-State. Tool-agnostisch (funktioniert mit Codex/OpenCode genauso).
- **⭐ Kern-Befund (100-Sessions-Analyse, productowner.ro):** "Domain knowledge discovered multiple
  times, **never persisted**" + "The problem is not missing rules — it's a broken improvement loop:
  corrections never flow back". Der Rückfluss-Loop ist die Lücke, nicht die Speichertechnik.
  Weitere Befunde: Subagents verbrauchen 50 %+ ihrer Tool-Calls für Datei-Suche; Friction-Keyword-
  Detektoren fangen nur 20-30 % echter Ereignisse.
- **Muster-Inventar:** Hook-basierte Event-Erfassung, Three-Tier-Memory (episodic/semantic/
  procedural, "Phantom"-Projekt), FastMCP generiert aus OpenAPI ~100 MCP-Tools in ~10 Zeilen,
  Parallel-Subagent-Analyse großer Session-Korpora.

## 5. Retrieval für kleine persönliche Wissensbasen

- **⭐ Hybrid-Suche ist DER lohnende Upgrade:** Qdrant nativ — named dense vectors + sparse (BM25)
  in EINER Collection, ein `query_points`-Call mit `Prefetch` + `FusionQuery` (RRF). Dense findet
  immer etwas Ähnliches, sparse trifft exakte Begriffe — RRF vereint beide Rankings.
- **GraphRAG = Overkill für hunderte Einträge:** "a serious lift" (Graph-DB, Schema, Lifecycle-
  Regeln, Betrieb); belegter Nutzen nur in Enterprise-Nischen (Finanz-Q&A 96 % Faithfulness).
  Multi-Hop-QA ist an GraphRAG gekoppelt → ebenfalls Overkill. Mittelmweg laut Quellen: BM25-
  DocumentRAG bzw. Hybrid (deckt das meiste ab).
- **Query-Routing im engeren Sinn** (lexikalisch vs. semantisch klassifizieren) ist in den Quellen
  nicht nötig befunden — RRF-Fusion übernimmt das symmetrisch.
- **Temporal Retrieval:** in den Quellen NICHT behandelt — bei uns trotzdem sinnvoll, weil
  `created_at` bereits im Payload liegt (Datums-Filter vor semantischer Suche, Frank-Fall
  "war ich letzten Monat angeln" — schon als gefilterte Suche gebaut).

## 6. Assistent-Features (Briefings, Capture, Stimme)

- **Saner.AI = bestes dokumentiertes Vorbild für proaktive Briefings:** morgendliche proaktive
  To-do-Übersicht, **einstellbare Zeitpläne** für Check-ins ("gentle coach"), Tages-Summaries mit
  Motivations-Nudges, Smart-Inbox-Vorschläge aus Notizen/Tags/Gesprächen, Related-Notes ungefragt,
  Kalender/Slack/Drive/Gmail als Futter. Mobile via Telegram-Bot statt nativer App.
- **Capture-Rituale (Chela):** Morning Brain Dump (2 Min ungefiltert sprechen), Post-Meeting-Capture,
  Mid-Day-Check-in, End-of-Day-Reflection — feste Momente statt "irgendwann".
- **STT/TTS-Bausteine:** Whisper (STT), Kokoro (TTS, 174 Stimmen/37 Sprachen), kommerzielle Ketten
  nutzen Wispr Flow + ElevenLabs.
- **Android-System-Capture** (Share-Sheet-Target, Homescreen-Widget, Quick-Settings-Tile,
  Notification-Actions, Voice-Capture) war in den Quellen NICHT dokumentiert — Standard-Android-
  Plattform-Wissen, im Feature-Plan aus eigenem Wissen ergänzt.

## 7. Selbstorganisation der Wissensbasis

- **Auto-Tagging semantisch** (nach Bedeutung, nicht Keywords) ist Stand der Technik; manuelles
  Tagging degradiert mit Wachstum ("you have to remember to use them").
- **Taxonomie-Evolution:** Community-Detection lässt Themen-Cluster EMERGIEREN statt starrer
  Vorab-Taxonomie (InfraNodus-Muster); Node-Ranking findet einflussreiche Notizen.
- **⭐ Vertrauen = Provenance + Confidence:** "AI can reduce time on KM tasks by 30 % but ONLY when
  answers include provenance snippets, confidence scores, role-based limits, audit logs."
- **Gesundheits-Dashboards für Wissensbasen existieren praktisch nicht** (klare Marktlücke) —
  nächstliegende Idee: Dubletten-Kandidaten, Stale-Zähler, verwaiste Kategorien, Wachstum, Backups.
- **Philosophie-Warnung (CombiningMinds):** Über-Optimierung kann Serendipität töten — Vorschläge
  immer als Vorschlag (Mensch bestätigt), nie stille Auto-Umbauten.

## Quellen (Auswahl)

- Letta Blog: Sleep-Time Compute · Letta Blog: Agent Memory · Letta Forum: "Letta vs Mem0 vs Zep vs Cognee" (2025-10-25)
- Code Pointer Substack: "Agent Memory Systems and Knowledge Graphs" (2026-05-28)
- arXiv 2606.01138 (memorywire) · arXiv 2404.00573 (Hou et al., Memory-Decay) · ProAgent (arXiv) · cc-self-train (arXiv 2604.17460)
- Dev Genius: "AI Agent Memory Systems in 2026" (2026-03-20) · Medium: "5 AI Agent Memory Systems" (2026-05-13)
- GitHub 20alexl/claude-engram · Simon Willison: claude-code-transcripts · productowner.ro: AI Self-Improvement Loop
- Qdrant-Doku/Tutorials: Hybrid Search mit RRF · NetApp/NVIDIA GraphRAG-Einordnung
- Saner.AI (Website, App-Store-Reviews, YouTube-Review) · Chela (Voice-first PKM) · InfraNodus · Coworker AI

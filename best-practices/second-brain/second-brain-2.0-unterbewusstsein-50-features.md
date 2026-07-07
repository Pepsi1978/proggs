# Second Brain 2.0 — „Das Unterbewusstsein": Plan + 50 Feature-Vorschläge

> Erstellt 2026-07-04 (Cowork-Session, Researcher-Schwarm mit 6 parallelen Recherche-Agenten).
> Franks Vision: Das zweite Gehirn soll wie ein **Unterbewusstsein** arbeiten — immer da, immer
> relevant, mit Gesamtüberblick; angebunden an alle Frontends (Cortex-Android, Dashboard, Claude
> Code via MCP); lernt kontinuierlich dazu; Qdrant als Langzeitgedächtnis PLUS echtes
> Kurzzeit-/Arbeitsgedächtnis (z. B. pro Programmiersession: Aufgaben, Ziele, Entscheidungen).
> Quellen: alle Funde mit URL + Datum + Flag `offiziell`/`extern` (Details in den Fußnoten je Feature).

---

## 1. Ist-Zustand (Kurzfassung, Stand 2026-07-04)

| Baustein | Was er heute kann |
|----------|-------------------|
| **Qdrant** | 1 Collection, Gemini-Embeddings 1536d, Payload (doc_id, title, category, categories[], parent, chunks, full_text, created_at/updated_at), reine Dense-Suche + Filter |
| **brain-api** | Wortwörtlicher 1:1-Speicher (KEIN LLM im Speicher), store/search/by-title/by-category/by-date/list/delete, Chunking nur für die Suche |
| **agent (Bibliothekar)** | Ein Chat-Eingang, Router (store/recall/smalltalk), 30-min-Session-Kurzzeitgedächtnis → danach Logbuch (Gehirn + .txt auf Platte Z), editierbare Prompts, Modell pro Rolle, Tavily-Websearch |
| **mcp-server** | remember/recall/get_by_title/get_by_category/get_by_date/list/forget/brain_health für Claude Code |
| **dashboard** | Web-Cockpit: Kategorie-Spektrum, Kategorien-Baum + Multi-Category, Drawer-Editor, Logbuch, Papierkorb, Eval-Läufe, TTS/STT, Google-Drive-Backup |
| **Cortex-Android** | Chat, Dashboard-Ansicht, WireGuard-Tunnel, Diktat (Groq-Whisper), Vorlesen (Gemini-TTS) |

**Stärken:** unantastbarer 1:1-Rohspeicher (die Branche hat 2026 nachgezogen: Mem0 hat UPDATE/DELETE
abgeschafft und ist auf ADD-only umgestiegen — Franks Philosophie war ihrer Zeit voraus), saubere
Trennung der Dienste, Observability, zwei Frontends.
**Lücken auf dem Weg zum „Unterbewusstsein":** (1) nichts ist *immer präsent* (alles muss gesucht
werden), (2) es gibt keinen Hintergrund-Prozess, der konsolidiert/lernt, (3) alles ist reaktiv
(Pull statt Push), (4) Coding-Sessions hinterlassen keine automatischen Spuren, (5) Suche ist
Dense-only ohne Recency/Wichtigkeit.

---

## 2. Ziel-Architektur: Das 4-Schichten-Gedächtnis + Schlaf-Agent

Die Kernerkenntnis der Recherche (Letta/MemGPT, Zep/Graphiti, Sleep-Time-Compute-Paper): Ein
„Unterbewusstsein" entsteht nicht durch bessere Suche, sondern durch **Schichten** — und durch
einen **zweiten Agenten, der im Hintergrund arbeitet**, während der Bibliothekar mit Frank redet.

```
┌────────────────────────────────────────────────────────────────────┐
│ SCHICHT 4: PROAKTIVER KANAL (Push statt Pull)                      │
│   ntfy → Handy · Morgen-Briefing · Verbindungs-Melder · Inbox      │
├────────────────────────────────────────────────────────────────────┤
│ SCHICHT 3: KERN-GEDÄCHTNIS (immer präsent, klein, <50k Zeichen)    │
│   Memory-Blocks: frank_profil · aktuelle_projekte · offene_faeden  │
│   → wird JEDEM Frontend automatisch mitgegeben, ohne Suche         │
├────────────────────────────────────────────────────────────────────┤
│ SCHICHT 2: ABGELEITETES WISSEN (Fakten, Entitäten, Digests)        │
│   facts-Collection mit Provenienz auf Schicht 1 · Entity-Index ·   │
│   Tages-/Wochen-Digests · Reflexionen — alles NUR ZUSÄTZLICH       │
├────────────────────────────────────────────────────────────────────┤
│ SCHICHT 1: ROHSPEICHER 1:1 (heute schon da — bleibt UNANTASTBAR)   │
│   Qdrant, wortwörtliche Einträge, kein LLM verändert je etwas      │
└────────────────────────────────────────────────────────────────────┘
        ▲ liest alle Schichten            ▲ schreibt Schicht 2+3
   BIBLIOTHEKAR (schnell, redet          SCHLAF-AGENT (stark, langsam,
   mit Frank, ändert NICHTS)             läuft nachts/im Leerlauf)
```

Das ist exakt Lettas Zwei-Agenten-Trennung (Sleep-Time Compute, Paper arXiv:2504.13171) übertragen
auf Franks Stack — und es respektiert die eiserne Regel: **Schicht 1 wird NIE verändert.** Alles
Abgeleitete trägt Quell-IDs (`source_ids`) und ist jederzeit auf den Original-Wortlaut rückführbar
(Provenienz-Muster aus Zep/Graphiti: „Every derived fact traces back here").

---

## 3. Die 50 Feature-Vorschläge

Gruppiert in 7 Blöcke (A–G). Jedes Feature: **Was · Warum · Wie**. Priorisierung in Abschnitt 4.

### Block A — Kern-Gedächtnis & Schlaf-Agent (das Unterbewusstsein selbst)

**1. Core-Memory-Blocks („Immer-präsent-Speicher")**
Kleine, immer mitgelieferte Gedächtnis-Bausteine (`label`, `description`, `value`, `limit`,
`read_only`) in brain-api: z. B. `frank_profil`, `aktuelle_projekte`, `offene_faeden`,
`heute_relevant`. Jeder Client (MCP, App, Dashboard) bekommt sie bei JEDEM Kontakt automatisch —
ohne Suche. Das ist der Kern von „immer da": Qdrant bleibt das Archiv, die Blocks sind das
Bewusstsein. Richtwerte: <20 Blocks, gesamt <50k Zeichen. *(Letta Memory Blocks, docs.letta.com,
`offiziell`)*

**2. Schlaf-Agent („Traumphase")**
Ein zweiter Agent, der NIE mit Frank redet: liest neue Einträge + Session-Protokolle, aktualisiert
die Core-Blocks, baut Digests und Reflexionen — „raw context → learned context". Stärkstes
verfügbares Modell (Latenz egal, er läuft nachts). Der Bibliothekar bleibt schnell und verändert
nichts. *(Sleep-Time Compute: arXiv:2504.13171 + letta.com/blog/sleep-time-compute, `offiziell`;
Paper-Ergebnis: ~5x weniger Rechenaufwand zur Antwortzeit, +13–18 % Genauigkeit)*

**3. Traum-Trigger: Cron + Ereignis + Zuruf**
Die Traumphase startet (a) nachts per Cron (z. B. 03:00), (b) nach N neuen Einträgen, (c) nach
Session-Ende des Bibliothekars, (d) manuell per Zuruf „träume" im Chat. *(Letta-Agent-Muster:
Off/Step-count/Compaction-Event, docs.letta.com/letta-agent/memory, `offiziell`)*

**4. Self-editing Memory-Tools**
Drei neue Endpunkte + MCP-Tools: `memory_insert` (anhängen, immer sicher), `memory_replace`
(Find-and-Replace mit Exakt-String-Validierung — schlägt fehl, wenn der alte Text nicht mehr da ist
= Schutz vor Überschreib-Unfällen), `memory_rethink` (Block komplett neu — darf NUR der
Schlaf-Agent). *(Letta Concurrency-Regeln, docs.letta.com/guides/core-concepts/memory/shared-memory,
`offiziell`)*

**5. Recall-Schicht: Session-Gedächtnis nie wegwerfen**
Das 30-min-Kurzzeitgedächtnis des Bibliothekars fließt beim Ablauf nicht nur ins Logbuch, sondern
als strukturiertes Session-Protokoll in eine eigene durchsuchbare `recall`-Collection. Ergebnis:
Archival (kuratierte Fakten) vs. Recall (was in Gesprächen passierte) — Lettas Zwei-Spur-Modell.
*(MemGPT-Paper arXiv:2310.08560, `offiziell`)*

**6. Persona-Profil „Wer ist Frank" mit Auto-Injection**
Ein destilliertes Profil (Präferenzen, laufende Projekte, wichtige Personen, Kommunikationsstil),
das der Schlaf-Agent periodisch aus dem Bestand aktualisiert und das in jeden System-Prompt des
Bibliothekars injiziert wird. Im Dashboard editierbar (Prompt-Editor existiert schon).
*(personal.ai Memory-Stack + Khoj-Agents, `offiziell`)*

**7. Blocks als Markdown in Git („MemFS light")**
Die Core-Blocks werden zusätzlich als Markdown-Dateien ins proggs-Repo gespiegelt (Ordner
`second-brain-memory/`): volle Versionshistorie, Rollback, Diff-Ansicht — passt exakt zu Franks
Commit-Kultur. Letta macht das 2026 genauso (git-versioniertes „context repository").
*(docs.letta.com/letta-agent/memory MemFS, `offiziell`)*

**8. Vorgerechnete Antworten (Antizipation)**
Der Schlaf-Agent generiert aus den Tages-Einträgen 5–10 wahrscheinliche zukünftige Fragen („Was war
nochmal…?") und legt fertige Antworten als `type: precomputed_answer` ab. Je vorhersagbarer die
Frage, desto größer der Gewinn — Franks wiederkehrende Alltagsfragen sind ideal. *(Kernidee des
Sleep-Time-Compute-Papers, `offiziell`)*

### Block B — Abgeleitete Wissens-Schicht (Fakten, Entitäten, Überblick)

**9. Fakten-Index mit Provenienz**
Neue Qdrant-Collection `facts`: Der Schlaf-Agent extrahiert atomare Fakten aus neuen 1:1-Einträgen;
jeder Fakt trägt `source_ids` (IDs der Original-Einträge), `extracted_at`, `extractor_version`. Der
Bibliothekar kann jeden Fakt wörtlich belegen („steht so in Eintrag X"). Rohspeicher = Episoden,
Fakten = abgeleitete Projektion — der Rohtext bleibt heilig. *(Graphiti-Episoden-Muster,
github.com/getzep/graphiti + Zep-Paper arXiv:2501.13956, `offiziell`)*

**10. Bi-temporales Schema: Invalidieren statt Löschen**
Jeder Fakt bekommt `valid_at`, `invalid_at`, `created_at`, `superseded_by`. Bei Widerspruch wird der
alte Fakt invalidiert (Zeitfenster geschlossen) und verweist auf den Nachfolger — nichts wird
gelöscht, die Historie bleibt abfragbar. *(Graphiti bi-temporales Modell, `offiziell`)*

**11. Widerspruchs-Wächter beim Fakten-Ingest**
Neuer Fakt → Suche nach verwandten aktiven Fakten → LLM-Check „widerspricht / ersetzt / ergänzt /
neu?" → nur Metadaten-Update. Adressiert das von Mem0 selbst benannte „confidently wrong"-Problem
(Fakt über alten Arbeitgeber bleibt hoch gerankt, obwohl längst falsch). *(mem0.ai/blog/state-of-
ai-agent-memory-2026, `offiziell`)*

**12. Entity-Index + Alias-Tabelle**
Parallel-Collection `entities` (Eigennamen, Personen, Projekte, Orte aus jedem Eintrag). Einträge,
die eine Entität teilen, sind verknüpft; Query-Entitäten boosten verbundene Einträge. Duplikate
(„Frank W." = „Frank") landen in einer Alias-Tabelle — Einträge werden NIE umgeschrieben. Mem0 hat
dafür 2026 sogar Neo4j komplett rausgeworfen: man braucht keine Graph-DB dafür. *(Mem0-v3-Migration,
docs.mem0.ai, `offiziell`)*

**13. Themen-/Community-Schicht (der „Gesamtüberblick")**
Periodischer Job clustert Entitäten/Fakten und schreibt pro Cluster eine Zusammenfassung mit
Provenienz-Links („Deine 23 Einträge zu Schweden drehen sich um: Angeln, Packraft, Route, …").
Genau das gewünschte „Unterbewusstsein, das den Überblick behält". *(Zep Community-Subgraph,
`offiziell`)*

**14. Zeitreise-Abfragen**
Dank Feature 10 trivial: „Was galt im März?" = Filter auf valid_at/invalid_at. Im Dashboard als
Zeit-Schieberegler. *(Graphiti, `offiziell`)*

**15. Themen-Drift-Melder**
Der Schlaf-Agent vergleicht die Themen-Cluster der letzten 30 Tage mit davor und meldet
Verschiebungen („Neu im Fokus: X · Eingeschlafen: Y") ins Morgen-Briefing. *(Eigene Ableitung aus
Feature 13, Community-Detection inkrementell)*

### Block C — Suche & Retrieval (das Gedächtnis-Gefühl)

**16. Hybrid-Suche: Dense + BM25 mit RRF-Fusion (größter Einzelhebel)**
BM25-Sparse-Vektor (FastEmbed, deutscher Tokenizer) als zweiter Named Vector; Suche = 2 Prefetches
(dense + sparse) + `rrf`-Fusion. Namen, Fachbegriffe, exakte Wörter werden endlich zuverlässig
gefunden — Dense allein verfehlt die notorisch. Nachrüstbar OHNE Re-Embedding des Bestands.
*(qdrant.tech/documentation/search/hybrid-queries, `offiziell`; RRF = der offizielle „sichere
Default")*

**17. Recency-Boost („frisches Gedächtnis")**
Formula-Query mit `exp_decay` auf `updated_at` (scale ≈ 30 Tage) über dem RRF-Prefetch: Neuere
Erinnerungen gewinnen bei ähnlicher Relevanz — wie beim menschlichen Gedächtnis. Voraussetzung:
datetime-Index auf created_at/updated_at. *(qdrant.tech/blog/decay-functions, 2025-09-01,
`offiziell`)*

**18. Wichtigkeits-Score nach der Park-Formel**
Payload-Felder `importance` (1–10, vom LLM beim Speichern vergeben: „Zähneputzen=1, Trennung=10"),
`last_accessed`, `access_count`. Ranking = Recency-Decay + Importance + Cosine-Relevanz — das am
besten validierte Memory-Scoring-Muster überhaupt. *(Generative Agents, Park et al.,
arXiv:2304.03442, `offiziell`)*

**19. „Mehr wie dieser Eintrag"**
Endpoint `/similar/{doc_id}` über Qdrants Recommend-API (`positive: [id]`). Ein Klick im
Drawer/App → assoziative Nachbarn. Trivial umzusetzen, fühlt sich stark nach assoziativem
Gedächtnis an. *(qdrant.tech/documentation/search/explore, `offiziell`)*

**20. Daumen-Feedback: „mehr davon / weniger davon"**
Daumen hoch/runter an Suchtreffern sammeln → Recommend mit `best_score` (positive = gelikte,
negative = weggeklickte IDs) für eine personalisierte „Für dich relevant"-Ansicht. Das Feedback
fließt zugleich in die Resurfacing-Halbwertszeit (Feature 40). *(Qdrant Recommend/Discovery,
`offiziell`)*

**21. Dedup per Grouping**
Chunk-Suche auf `query/groups` mit `group_by: doc_id` umstellen: Trefferliste zeigt Dokumente statt
fünfmal denselben Text. *(qdrant.tech Grouping, `offiziell`)*

**22. Titel-Embedding als eigener Named Vector**
Dritter Vektor `title_dense`: kurze Stichwort-Queries matchen gegen Titel, lange gegen Volltext —
oder beide als Prefetches in die RRF-Fusion. *(qdrant.tech Named Vectors, `offiziell`)*

**23. Facetten-Kacheln im Dashboard**
Qdrant Facet-API (v1.12+) auf category/categories[] + Datums-Filter: „Dein Gedächtnis: 42×
Projekte, 17× Gesundheit, davon 9 neu diesen Monat" — als klickbare Kacheln. Ersetzt die heutigen
Full-Scans für Kategorie-Zählungen (schneller!). *(qdrant.tech Facet API, `offiziell`)*

**24. ColBERT-Rescoring als Ausbaustufe**
Später, bei Wachstum: Multivector (`m=0`, kein HNSW) als zweite Stufe über den Top-100 der
Hybrid-Suche — Qdrants empfohlene schnellere Alternative zum Cross-Encoder. *(qdrant.tech
late-interaction-models, `offiziell`)*

### Block D — Konsolidierung, Reflexion & Vergessen

**25. Digest-Hierarchie: Tag → Woche → Monat**
Der Schlaf-Agent schreibt Tages-Digests; Wochen-Digests entstehen NUR aus 7 Tages-Digests,
Monats-Digests nur aus Wochen-Digests (nie Rohtexte über Wochen hinweg — hält Prompts klein,
crash-sicher). Suche prüft zuerst Digests (Überblick), steigt nur bei Bedarf zu Rohtexten ab —
nachweislich ~50 % weniger Retrieval-Tokens bei besserer Genauigkeit. *(TiMem arXiv:2601.02845 +
RAPTOR arXiv:2401.18059, `offiziell`)*

**26. Reflexions-Einträge mit Evidenz-Pointern**
Überschreitet die Wichtigkeits-Summe neuer Einträge einen Schwellwert, generiert der Schlaf-Agent
3 Fragen über die jüngsten ~100 Einträge, beantwortet sie per Retrieval und speichert die Insights
als `type: reflection` mit Quell-IDs. Reflexionen sind selbst durchsuchbar → Reflexionen über
Reflexionen entstehen von allein (Baumstruktur). *(Reflection-Tree, Generative Agents,
`offiziell`)*

**27. Soft-Decay statt Löschen**
Nie hart löschen: Suchzeit-Multiplikator 0,3x–1,5x aus last_accessed/access_count. Kategoriebewusst:
`gesundheit`/`familie`/`dauerhaft` verfallen NIE, `alltag` schnell. Löschen nur bei exaktem
Duplikat, explizitem Widerruf oder Supersession (dann `superseded_by` statt Delete — Audit-Trail
bleibt). *(Mem0 Memory Decay, mem0.ai/blog, 2026-05-08, `offiziell`; MemoryBank
Ebbinghaus-Vergessenskurve, arXiv:2305.10250, `offiziell`)*

**28. Supersession beim Schreiben**
Beim store prüft der Bibliothekar per Suche, ob ein Alt-Eintrag inhaltlich widerspricht („wohnt in
X" vs. neu „wohnt in Y") und schlägt vor, den alten als veraltet zu markieren. Verhindert die
häufigste Memory-Fäulnis: akkumulierte Widersprüche. *(Mem0 Eviction-Blog, `offiziell`)*

**29. Zettelkasten-Verlinkung**
Nächtlicher Lauf holt für neue Einträge die Top-k semantischen Nachbarn und schreibt `links: [ids]`
ins Payload. Ergebnis: Muster-Erkennung über Themen hinweg („diese 12 Einträge drehen sich alle um
Projekt X") + „Verwandte Einträge" in App/Dashboard. *(A-MEM, arXiv:2502.12110, NeurIPS 2025,
`offiziell`)*

**30. Duplikat-Erkennung beim Speichern**
Vor dem Ablegen: Cosine-Similarity gegen Bestand; bei fast identischem Eintrag Rückfrage „Du hast
dazu schon ‚X' gespeichert — ersetzen, ergänzen oder beide behalten?" (der 2-Schritt-Speicherdialog
existiert in App + Dashboard schon — nur die Prüfung fehlt). *(Charlie Mnemonic + Mem0-Dedup,
`offiziell`)*

**31. Konsolidierungs-Wächter (Observability für die Traumphase)**
Jeder Schlaf-Lauf schreibt JSON-Lines-Protokoll (gelesen/erzeugt/abgewertet) + Sonden: kein Digest
darf nicht-existente Quell-IDs referenzieren; max. 5 % des Bestands pro Nacht abwertbar; bei
Anomalie kein Write, sondern Fehler ins Whiteboard. Ein fehlgeleiteter Schlaf-Agent kann das
Gedächtnis so NIE still beschädigen — passt zu Franks Observability-First-Regel. *(SSGM-Framework
arXiv:2603.11768, `offiziell`; + Franks eigene Direktiven)*

**32. Heat-Score: Kurzzeit → Mittelzeit → Langzeit**
Drei-Ebenen-Promotion wie ein Betriebssystem: Session-Notizen (kurz) → Themen-Segmente mit
Heat-Score aus Zugriffen + Recency (mittel) → Persona/Kernwissen (lang). Heiße Themen steigen auf,
kalte kühlen ab. *(MemoryOS, arXiv:2506.06326, EMNLP 2025 Oral, `offiziell`)*

### Block E — Coding-Session-Gedächtnis (Claude Code ↔ Second Brain)

**33. Capture-Hooks ohne LLM auf dem heißen Pfad**
PostToolUse/UserPromptSubmit/PreCompact/SessionEnd-Hooks appenden je EINE Zeile in ein
Session-NDJSON und exiten immer 0 — können nie blockieren oder die Session verlangsamen. Die
Auswertung passiert später, entkoppelt. *(SuperBrain-Architektur,
alexandrekhoury.com/writing/superbrain-session-memory-claude-code, 2026-05-29, `extern`)*

**34. SessionEnd-Distiller → remember**
Ein entkoppelter Prozess (`claude -p` oder direkter API-Call) liest am Session-Ende das Transcript
(`transcript_path` liefert jeder Hook mit) und extrahiert `{aufgaben, entscheidungen, learnings,
status, offene_fragen}` → per HTTP an brain-api, getaggt mit `project`, `session_id`, `type`.
Genau Franks Wunsch: „in jeder Programmiersession dazulernen, was meine Aufgaben und Ziele sind."
*(code.claude.com/docs/en/hooks, `offiziell` + mem0-Claude-Code-Plugin-Muster, `extern`)*

**35. SessionStart-Précis: Kontext-Restore in ~1.000 Token**
SessionStart-`command`-Hook (NICHT mcp_tool — MCP-Server sind beim Start oft noch nicht verbunden!)
ruft brain-api direkt per HTTP: projekt-bezogener recall + Recency-Gewichtung → hartes Budget von
~1.000–1.200 Token als stdout injiziert („Letzte Session: … Offene Aufgaben: … Entscheidungen: …").
**Wichtigste Community-Erkenntnis 2026: Speichern ist trivial — automatische, BUDGETIERTE Injektion
ist der Engpass.** claude-mem scheiterte mit Alles-Injektion (Context Pollution) und zog sich auf
einen ~800-Token-Index zurück; SuperBrain: 8.785 gespeicherte Observations, null automatisch
aufgetaucht, weil Recall ein Tool war, das der Agent „aufrufen sollte". *(claude-mem
docs/Architecture-Evolution + SuperBrain, `extern`; code.claude.com/docs/en/hooks, `offiziell`)*

**36. Natives Claude-Code-Session-Memory anzapfen**
Claude Code schreibt bereits selbst strukturierte Session-Summaries
(`~/.claude/projects/<hash>/<session>/session-memory/summary.md`: Titel, Status, Key Results, Work
Log). Der SessionEnd-Hook kopiert diese Datei einfach ins Second Brain — Gratis-Qualität von
Anthropics eigenem Summarizer, und löst nebenbei das „Auto Memory ist maschinenlokal"-Problem:
Windows ↔ macOS synchronisieren sich über das Gehirn. *(claudefa.st/blog/guide/mechanics/
session-memory, 2026, `extern`)*

**37. Bug-Almanach → Gehirn-RAG**
Die bestehende `bug-cases.jsonl` zusätzlich in Qdrant einbetten; der PostToolUseFailure-Hook macht
recall über frühere Fehler und injiziert Treffer („Diesen Fehler gab es am 12.06., Root Cause war
X"). Nach dem Fix: Lesson als unbefristeter Eintrag. Franks Bug-Case-Auto-Writer ist schon state of
the art — das RAG-Matching übers Gehirn ist die logische Vollendung von Hauptdirektive #3.
*(code.claude.com/docs/en/hooks PostToolUseFailure, `offiziell`)*

**38. ACE-Playbook pro Projekt (Delta-Updates, nie Voll-Rewrite)**
Pro Projekt ein kuratiertes Playbook mit nummerierten, atomaren Lektionen-Bullets im Gehirn. Der
Distiller darf nur Deltas anhängen/einzelne Bullets ändern — NIEMALS das Dokument komplett neu
generieren. Verhindert die zwei nachgewiesenen Krankheiten naiver Selbst-Verbesserung: Brevity Bias
(Zusammenfassen wirft Wissen weg) und Context Collapse (iteratives Neuschreiben erodiert Details).
Direkt anwendbar auch auf Franks MEMORY.md-Whiteboard. *(ACE, arXiv:2510.04618, Stanford, Okt 2025,
`offiziell`)*

**39. Kurzzeit/Langzeit-TTL für Session-Wissen**
Zwei Klassen: `status`/`compact_summary`-Einträge verfallen nach 90 Tagen (bzw. ein überschreibbarer
„aktueller Stand pro Projekt"-Slot); `decision`/`lesson`/`convention` verfallen NIE. Sauberes
Kurzzeit-/Langzeitgedächtnis genau nach Franks Wunsch. *(mem0-Claude-Code-Plugin-Muster,
mem0.ai/blog/claude-code-memory, `extern`)*

**40. Session-Anknüpfung sichtbar machen**
SessionStart-Hook setzt `sessionTitle` auf „Fortsetzung: <letzte offene Aufgabe>" und gibt den
Recall-Block IMMER sichtbar im Terminal aus; wenn recall leer/fehlgeschlagen → sichtbare Warnzeile
(sichtbare Fehler sind fixbar, stille nicht — deckt sich mit Franks Sichtbarkeits-Regel).
*(SuperBrain, `extern`; code.claude.com/docs/en/hooks sessionTitle, `offiziell`)*

### Block F — Proaktivität (Push statt Pull)

**41. ntfy als Push-Kanal Server → Handy (das Fundament)**
ntfy-Server als Docker-Container auf dem VPS (nur über WireGuard erreichbar), ntfy-F-Droid-App aufs
Fold 6 — komplett Firebase-frei, ~0–1 % Akku/17 h, Publish = ein HTTP-POST aus brain-api/agent.
Mit Prioritäten, Klick-URLs (Deep-Link in Cortex/Dashboard) und Action-Buttons. Voraussetzung für
alles Weitere in diesem Block. *(docs.ntfy.sh, `offiziell`)*

**42. Morgen-Briefing (Pulse-Muster)**
Cron 07:00 (schichtplan-bewusst!): Was kam gestern rein, was steht heute an, 1–2 wiederaufgetauchte
Alt-Einträge, Themen-Drift — als EINE ntfy-Nachricht mit Link auf eine Briefing-Karte im Dashboard.
OpenAI-Lektion übernehmen: Das Briefing verfällt nach einem Tag, außer Frank interagiert; und Frank
kann kuratieren („morgen mehr zu X, nie wieder Y"). *(ChatGPT Pulse, openai.com, 2025-09-25,
`offiziell`)*

**43. Heartbeat-Agent mit Schweigegelübde**
Alle 30–60 min aufwachen, Mini-Checkliste prüfen (neue Einträge? fällige Erinnerungen? Muster?).
Antwort-Contract: intern `HEARTBEAT_OK` = wird verschluckt; nur echte Alerts erreichen Frank.
Plus activeHours (z. B. 08–22 Uhr, schichtabhängig) und hartes Limit N Pushes/Tag — sonst wird es
Rauschen und Frank schaltet es ab. *(OpenClaw-Heartbeat-Muster, docs.openclaw.ai/gateway/heartbeat,
`offiziell`)*

**44. Verbindungs-Melder („das passt zu Y von vor 3 Wochen")**
Beim Speichern eines neuen Eintrags: Similarity-Suche gegen den Bestand; bei Score über Schwelle
UND Alter > 14 Tage → Push „Dein neuer Eintrag ähnelt ‚Y' vom 12.06." Ein einziger Qdrant-Query im
Insert-Pfad — der schnellste Weg zum „Unterbewusstsein-Gefühl" und der beste Wow-Moment pro
Aufwand. *(Eigene Ableitung, Qdrant Recommend, `offiziell`)*

**45. „An diesem Tag" (On This Day)**
Einträge von heute vor 1/3/6/12 Monaten ins Morgen-Briefing. Trivial (by-date existiert schon) und
emotional wirksam — das etablierte Day-One/Obsidian-Muster. *(Obsidian Journal Review/Day One,
`extern`)*

**46. Spaced-Repetition-Resurfacing (Readwise-Mechanik)**
Jeder Eintrag bekommt eine Recall-Wahrscheinlichkeit mit Halbwertszeit (7/14/28 Tage je nach
Priorität); fällt sie unter 50 %, wird er Kandidat fürs Wiederauftauchen. Interaktion resettet.
Buttons „wichtig / ok / vergessen" tunen die Halbwertszeit pro Eintrag, Gewichtung pro Kategorie
einstellbar. Perfekt für Franks Vorlese-Workflow (täglich laut lesen!). *(Readwise Daily Review,
docs.readwise.io, `offiziell`)*

**47. Notify / Question / Review als drei Nachrichtentypen**
Die LangChain-Taxonomie übernehmen: Notify = reine Info; Question = ntfy-Action-Buttons („Meintest
du Projekt A oder B?" → 2 Buttons, POST zurück an den Agenten); Review = Agent schlägt Aktion vor
(„diese 3 Einträge zusammenlegen?"), Frank bestätigt per Button — komplette Feedback-Schleife ohne
App-Öffnen. *(LangChain Ambient Agents, langchain.com/blog, 2025-01-14, `offiziell`)*

**48. Agent-Inbox im Dashboard**
Alle proaktiven Meldungen (auch verpasste) landen zusätzlich in einer Inbox-Ansicht: offen/erledigt,
mit den Question/Review-Buttons. Push ist flüchtig — die Inbox ist das Gedächtnis des proaktiven
Kanals. *(LangChain Agent Inbox, `offiziell`)*

### Block G — Frontends & Ingestion (Cortex-App + Dashboard)

**49. „Ähnliche Erinnerungen" live im Chat**
Während Frank mit dem Bibliothekar chattet, zeigt eine Seitenleiste (Dashboard) bzw. ein
aufklappbarer Bereich (App) passiv die 3–5 semantisch nächsten Einträge zum aktuellen Gespräch —
ohne dass recall aufgerufen werden muss. Passives Erinnern statt aktives Suchen: das
Smart-Connections-Muster, mit Qdrant ein einziger Query pro Nachricht. *(Obsidian Smart Connections
+ Mem.ai „Similar Mems", `offiziell`)*

**50. Multimodale Ingestion: Foto, Web-Clip, Sprachmemo — mit „Sprung zum Original"**
Drei Wege in der Cortex-App: (a) **Foto/Screenshot** → Vision-Beschreibung + OCR-Text als
durchsuchbarer Eintrag, Original-Bild als Anhang; (b) **Android Share-Target als Web-Clipper** →
Server holt die Seite, extrahiert Haupttext + Archiv-Kopie gegen Link-Rot, KI schlägt Kategorie im
bestehenden Baum vor; (c) **Sprachmemo-Pipeline** → Groq-Whisper-Transkript (existiert) +
LLM-Nachverarbeitung (Titel, Kurzfassung, extrahierte Action Items) — Roh-Transkript UND Summary
getrennt gespeichert (1:1-Prinzip gewahrt). Überall gilt: Jede Antwort zitiert ihre Quellen als
klickbare Chips → öffnet den 1:1-Originaltext. *(Karakeep + Limitless + Screenpipe-Muster,
`offiziell`)*

---

## 4. Priorisierung: Vier Ausbaustufen

**Stufe 1 — Sofort spürbar, wenig Aufwand (Suchqualität + erster Wow-Moment):**
16 (Hybrid-Suche) · 17 (Recency-Boost) · 21 (Grouping) · 23 (Facetten) · 19 (Mehr-wie-das) ·
44 (Verbindungs-Melder) · 30 (Duplikat-Check) — fast alles reine brain-api-Arbeit, kein neuer Dienst.

**Stufe 2 — Das Unterbewusstsein entsteht (neuer Dienst „Schlaf-Agent"):**
1 (Core-Blocks) · 2+3 (Schlaf-Agent + Trigger) · 6 (Persona-Profil) · 25 (Tages-Digest) ·
26 (Reflexionen) · 31 (Wächter) · 18 (Wichtigkeits-Score) · 7 (Blocks in Git).

**Stufe 3 — Coding-Sessions lernen mit (Claude-Code-Pipeline):**
33 (Capture-Hooks) · 34 (Distiller) · 35 (SessionStart-Précis, hartes Token-Budget!) ·
36 (natives Session-Memory anzapfen) · 39 (TTL) · 40 (sichtbare Anknüpfung) · 37 (Bug-RAG) ·
38 (ACE-Playbook).

**Stufe 4 — Proaktiv + abgeleitetes Wissen (der volle Ausbau):**
41 (ntfy) · 43 (Heartbeat) · 42 (Morgen-Briefing) · 45–48 (Resurfacing/Inbox) · 9–15
(Fakten/Entitäten/Themen-Schicht) · 49–50 (Frontend-Ausbau) · 5 · 8 · 27–29 · 32 · 22 · 24 · 4.

**Leitplanken für ALLE Stufen (aus der Recherche destilliert):**
1. **Schicht 1 bleibt heilig** — kein Feature verändert je einen 1:1-Rohtext; alles Abgeleitete
   trägt `source_ids` (Provenienz). Die Branche hat Franks ADD-only-Philosophie 2026 bestätigt.
2. **Injektion ist der Engpass, nicht Speicherung** — jedes „immer präsent"-Feature bekommt ein
   hartes Token-Budget (Blocks <50k Zeichen gesamt, Session-Précis ~1.000 Token).
3. **Schweigen ist Default** — jeder proaktive Kanal braucht Silence-Kriterien, activeHours und
   Tages-Limits, sonst wird das Unterbewusstsein zum Lärm.
4. **Der Schlaf-Agent wird überwacht** — Wächter-Sonden + Protokoll + Abwertungs-Deckel, damit
   Selbst-Lernen nie still das Gedächtnis beschädigt (Memory-Poisoning-Schutz: Provenienz + Review).
5. **Eigene Mini-Evaluation statt Benchmark-Glaube** — der Mem0-vs.-Zep-Benchmark-Krieg (beide
   Seiten mit nachgewiesenen Fehlern) lehrt: 30–50 eigene Frage/Antwort-Paare über Franks echte
   Einträge als Regressionstest für jede Retrieval-Änderung (Franks Eval-Läufe im Dashboard sind
   die perfekte Basis).

---

## 5. Quellen (Auswahl, vollständige URLs bei den Features)

**Offiziell:** Letta/MemGPT (docs.letta.com, arXiv:2310.08560, arXiv:2504.13171,
letta.com/blog/sleep-time-compute 2025-04-21, letta.com/blog/memory-blocks 2025-05-14) ·
Zep/Graphiti (arXiv:2501.13956, github.com/getzep/graphiti) · Mem0 (docs.mem0.ai v3-Migration,
mem0.ai/blog/state-of-ai-agent-memory-2026, Memory-Decay 2026-05-08) · Qdrant (qdrant.tech:
hybrid-queries, decay-functions 2025-09-01, explore, facet, quantization, snapshots) · Anthropic
(code.claude.com/docs: memory, hooks, sub-agents) · Papers: Generative Agents arXiv:2304.03442,
RAPTOR arXiv:2401.18059, HippoRAG arXiv:2405.14831, A-MEM arXiv:2502.12110, MemoryOS
arXiv:2506.06326, TiMem arXiv:2601.02845, ACE arXiv:2510.04618, Reflexion arXiv:2303.11366,
MemoryBank arXiv:2305.10250, SSGM arXiv:2603.11768 · LangChain Ambient Agents (2025-01-14) ·
OpenAI ChatGPT Pulse (2025-09-25) · ntfy (docs.ntfy.sh) · OpenClaw Heartbeat (docs.openclaw.ai) ·
Readwise (docs.readwise.io) · Karakeep, Screenpipe, Khoj, Charlie Mnemonic (jeweilige Repos/Docs).
**Extern:** SuperBrain (alexandrekhoury.com 2026-05-29), claude-mem (github.com/thedotmack),
claudefa.st Session-Memory-Guide, mem0 Claude-Code-Plugin-Blog, Gotify-vs-ntfy-Vergleich,
Limitless/Meta-Übernahme (Dez 2025).

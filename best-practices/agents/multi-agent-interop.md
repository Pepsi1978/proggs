# Modelluebergreifende Agenten-Zusammenarbeit — Best Practices (Stand 2026-09-09)

> Die **Praeventions-Seite** zum Bug-Almanach `bugs/agents/multi-agent-interop.md`. Der Almanach
> sagt *was schiefgeht*; diese Datei sagt *wie man Agenten verschiedener Anbieter von vornherein so
> zusammensetzt, dass es haelt*. Vor der Arbeit beide lesen — **erst Almanach, dann Best Practices**.
> Quell-Flag pro Empfehlung: `offiziell` = Hersteller-/Foundation-Doku, `extern` = Community/Paper/Blog.
>
> **Geltungsbereich:** die VERBINDUNG zwischen Agenten — Protokolle (MCP, A2A, ACP), Handoffs,
> dateibasierte Uebergabe, Cross-Vendor-Betrieb (Claude ↔ GPT ↔ Gemini), Kosten und Sicherheit dieser
> Verbindung. Die Schleife selbst: `loop-engineering.md`. Routing/Spawning innerhalb EINES Harness:
> `orchestrator-agent.md`. Schwarmgroesse/Parallelitaet: `orchestrator-agent.md` §Schwaerme.
>
> **Recherche-Herkunft:** 21 Researcher parallel (7 Firecrawl+Tavily→DeepSeek, 7 DeepSeek `:online`,
> 7 Sonnet-5), je dieselben 7 Unterthemen, am 2026-09-09. Die Primaerquellen unten stammen ganz
> ueberwiegend aus dem Sonnet-Schwarm — die Skript-Engines fanden fast nur Sekundaerliteratur
> (Messung im Almanach, §Engine-Auswahl).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektuere
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Agent braucht Werkzeuge/Daten | **MCP** — vertikal, etabliert, Spec 2026-07-28 | §1 |
| 2 | Agent soll anderen Agenten beauftragen | **A2A** — horizontal, v1.0.1, aber ohne Anthropic in der Traegerliste | §2 |
| 3 | "ACP" in einer Quelle gelesen | ZWEI Protokolle mit dem Kuerzel — erst klaeren welches | §3 |
| 4 | OpenAI-Handoffs fuer Cross-Vendor geplant | Geht nicht — SDK-intern, bleibt "within a single run" | §4 |
| 5 | Claude ↔ GPT im selben Projekt | Sicherster Weg 2026: gemeinsames Dateisystem/Queue, nicht Protokoll | §5 |
| 6 | Dateibasierte Uebergabe bauen | Atomic Claim (`O_CREAT\|O_EXCL`) + Output-Existenzcheck, KEIN Advisory-Lock | §5 |
| 7 | Heterogene CLI-Agenten orchestrieren | Headless-Subprocess (`claude -p`, `codex exec`) + Queue mit atomaren Claims | §6 |
| 8 | Parallel im selben Repo arbeiten | Git-Worktrees (`isolation: worktree`), praktische Grenze 8-10 | §6 |
| 9 | Kosten schaetzen | Orchestrator-Overhead einrechnen: 3-Agenten-Pipeline ≈ 3x Tokens | §7 |
| 10 | Fremden MCP-Server einbinden | Whitelisting + Tool-Description-Sanitizing, Tool Poisoning ist real | §8 |

---

## §1 MCP — die vertikale Achse (Agent → Werkzeuge) `offiziell`

**Zweck:** Ein Agent/Modell spricht mit Werkzeugen, Daten und APIs. NICHT fuer Agent-zu-Agent gedacht.

**Reifegrad 2026-09: etabliert.** Aktuelle Spezifikation **2026-07-28**. Wesentliche Aenderungen
gegenueber 2025-11-25:
- zustandsloser Protokollkern (der `initialize`/`notifications/initialized`-Handshake ist weg)
- protokollseitige Sessions und der `Mcp-Session-Id`-Header aus Streamable HTTP entfernt
- Multi-Round-Trip-Requests, header-basiertes Routing, cachebare List-Ergebnisse
- verhaertete Autorisierung, naeher an OAuth/OIDC
- formales Extensions-Framework ("MCP Apps" fuer server-gerenderte UIs, "Tasks" fuer Langlaeufer)
- formale Deprecation-Policy

Quelle: https://modelcontextprotocol.io/specification/2026-07-28/changelog

**Governance:** Anthropic hat MCP am **09.12.2025** an die neu gegruendete **Agentic AI Foundation
(AAIF)** unter der Linux Foundation gespendet. Gruendungsmitglieder (Platinum): **Anthropic, Block,
OpenAI**; unterstuetzt von Google, Microsoft, AWS, Cloudflare, Bloomberg. Stand April 2026: 170+
Mitglieder. Anthropic brachte MCP ein, Block "goose", OpenAI **AGENTS.md**.
Quelle: https://anthropic.com/news/donating-the-model-context-protocol-and-establishing-of-the-agentic-ai-foundation

**Verbreitung:** ueber 10.000 aktive oeffentliche MCP-Server; genutzt von ChatGPT, Cursor, Gemini,
Microsoft Copilot, VS Code; 110+ Mio. monatliche SDK-Downloads (Stand April 2026).

**Merke:** AGENTS.md ist Teil derselben AAIF-Gruendung, aber ein **Konventions-/Kontextstandard, kein
Laufzeitprotokoll**. Es gehoert nicht in dieselbe Kategorie wie MCP/A2A — wird aber staendig in
einem Atemzug genannt. Verbreitung: 60.000+ Open-Source-Projekte, u.a. GitHub Copilot, VS Code,
Cursor, Gemini CLI.

## §2 A2A — die horizontale Achse (Agent → Agent) `offiziell`

**Zweck:** Unabhaengige, getrennt gehostete Agenten delegieren sich gegenseitig Aufgaben.

**Reifegrad 2026-09: im Uebergang von Entwurf zu Etablierung.** Von Google im April 2025 gestartet,
bereits im **Juni 2025** an die Linux Foundation uebergeben — aber als **eigenes A2A-Projekt, NICHT
Teil der AAIF**. Version 1.0 als erste stabile Spezifikation; eine Sekundaerquelle nennt 1.0.1 ab
Mai 2026 mit Extension-Mechanismus (nicht primaerquellenverifiziert).
150+ Organisationen, 22.000+ GitHub Stars, SDKs in Python/JS/Java/Go/.NET.
Quelle: https://www.linuxfoundation.org/press/a2a-protocol-surpasses-150-organizations-lands-in-major-cloud-platforms-and-sees-enterprise-production-use-in-first-year (09.04.2026)

**⚠️ Der entscheidende Vorbehalt fuer Claude-Nutzer:** In der Traegerliste der Linux Foundation
(AWS, Cisco, Google, IBM, Microsoft, Salesforce, SAP, ServiceNow) wird **Anthropic nicht genannt**.
Anthropic ist Gruendungsmitglied der AAIF (MCP), taucht bei A2A aber nirgends auf. Ob Anthropic A2A
informell unterstuetzt, war mit den Quellen von 2026-09-09 nicht zu klaeren.

**Konsequenz:** A2A als Draht zwischen einem Anthropic-Agenten und einem OpenAI/Google-Agenten ist
**nicht durch Anthropic-Beteiligung abgesichert**. Es muesste ueber allgemeine Agent-Card-/HTTP-
Kompatibilitaet laufen. Vor einer Wette auf A2A im Anthropic-Kontext: aktuellen Stand pruefen.

## §3 ACP — Achtung, zwei Protokolle mit demselben Kuerzel `extern`

| Kuerzel | Voller Name | Traeger | Zweck |
|---------|-------------|---------|-------|
| ACP | **Agent Client Protocol** | Zed Industries (nicht Linux Foundation) | Coding-Agent ↔ **Editor** |
| ACP | **Agent Communication Protocol** | IBM / BeeAI | Agent ↔ **Agent** |

Zeds ACP: JSON-RPC 2.0 ueber stdin/stdout, veroeffentlicht August 2025, Adoption durch JetBrains,
Google, GitHub, 25+ Agenten (Stand Maerz 2026), gemeinsame Registry mit JetBrains seit Januar 2026.
Quelle: https://zed.dev/blog/acp-registry

**Praxisregel:** Wer "ACP" liest, klaert zuerst welches — die beiden loesen voellig verschiedene
Probleme, und Quellen verwechseln sie. Selbst ein arXiv-Governance-Paper (2606.31498) nennt "ACP"
neben MCP und A2A, ohne zu spezifizieren welches gemeint ist.

**Governance-Befund fuer alle Protokolle:** Das Paper "Governance Gaps in Agent Interoperability
Protocols" (Kang & Diponegoro, 30.06.2026, https://arxiv.org/abs/2606.31498) untersucht MCP, A2A,
ACP, ANP und ERC-8004 entlang sechs Dimensionen. Ergebnis: **Abstimmung und Dissens-Erhaltung
fehlen bei ALLEN fuenf Protokollen vollstaendig**, Deliberation ist abwesend oder bestenfalls
teilweise vorhanden. Wer Agenten ueber Anbietergrenzen verhandeln lassen will, baut das selbst.

## §4 OpenAI-Handoffs sind KEIN Cross-Vendor-Mechanismus `offiziell`

Handoffs im OpenAI Agents SDK delegieren zwischen spezialisierten Agenten **innerhalb desselben
Runs und desselben Frameworks** (Beispiel: Kundenservice-App mit getrennten Agenten fuer
Bestellstatus, Rueckerstattung, FAQ). Technisch als Tool an das LLM repraesentiert
(`transfer_to_refund_agent`).
Quelle: https://openai.github.io/openai-agents-python/handoffs/

Grenzen laut Doku und Vergleichsanalyse:
- bleiben "within a single run"
- linear (A→B→C), keine nativen Parallel-/Rueckfuehrungsmuster ohne manuelles Modellieren
- Input-Guardrails gelten nur fuer den ersten, Output-Guardrails nur fuer den letzten Agenten
- "Nested handoff history" ist Opt-in-Beta, standardmaessig aus
- **keine dokumentierte Aussage zur Interoperabilitaet mit Claude- oder Gemini-Agenten**

Anthropic bietet eine **OpenAI-SDK-Kompatibilitaetsschicht** fuer die Claude API
(https://platform.claude.com/docs/en/api/openai-sdk). Sie ist laut Sekundaerquelle primaer zum
Testen/Vergleichen von Modellfaehigkeiten gedacht und **nicht als produktionsreife Langzeitloesung**
empfohlen. Wer sie einsetzt, sollte das als bewusste Uebergangsloesung dokumentieren.

## §5 Der pragmatische Weg 2026: geteiltes Dateisystem statt Protokoll `extern`

Fuer "ein Claude-Code-Agent und ein OpenAI-Agent arbeiten am selben Projekt" ist der belastbarste
Weg 2026 **nicht** ein Protokoll, sondern ein gemeinsamer Ablageort mit klaren Regeln. Begruendung:
MCP loest die falsche Achse, A2A hat keine erkennbare Anthropic-Beteiligung, Handoffs sind
SDK-intern. Was bleibt, ist der kleinste gemeinsame Nenner — und der ist erstaunlich tragfaehig.

**Muster (aus dem AWS-S3-Multi-Agent-Blog, uebertragbar aufs lokale Dateisystem):** `extern`
1. **Atomic File Claiming:** `os.open(pfad, os.O_CREAT | os.O_EXCL | os.O_WRONLY)` — nur EIN Prozess
   kann die Datei exklusiv anlegen. Garantiert Exactly-once-Verarbeitung ohne Lock-Datei-Gebastel.
2. **Deduplizierung ueber Output-Existenzcheck:** vor der Arbeit pruefen, ob die Ausgabedatei schon
   da ist. Macht den Schritt idempotent und ueberlebt Doppelaufrufe.
3. **Persistentes State-Ledger:** Liste bereits verarbeiteter Einheiten auf gemeinsamem Mount. Die
   Autoren warnen ausdruecklich: **das bricht bei Parallelitaet, wenn nicht zusaetzlich atomar
   geclaimt wird.** Ledger allein reicht nicht.
Quelle: https://aws.amazon.com/blogs/storage/orchestrating-multi-agent-ai-architectures-with-amazon-s3-files/

**Verzeichnisstruktur als Vertrag** (Muster aus mehreren unabhaengigen Quellen): Inbox/Outbox oder
Phasen-Ordner, dazu ein fortlaufendes Status-Dokument. Flach und offensichtlich halten.

**⚠️ Begriffswarnung "Ledger":** Es gibt **keinen** Industriestandard. Microsoft/AutoGen meint damit
Task-Ledger/Progress-Ledger, UAIX eine `.uai/intake-outcome-ledger.uai`, andere schlicht ein
`STATE.md`. Wer "Ledger-Datei" sagt, muss dazusagen, was er meint.

## §6 Heterogene CLI-Agenten orchestrieren `offiziell` + `extern`

**Headless-Subprocess ist das dokumentierte Muster.** Das Claude Agent SDK spawnt selbst einen
`claude`-CLI-Prozess und spricht per stdin/stdout im JSON-Lines-Protokoll mit ihm. Fuer fremde
CLI-Agenten gilt dasselbe Muster: im Headless-Modus aufrufen (`claude -p`, `codex exec`, …) und
strukturierte Ausgabe parsen (`--output-format json`).
**Es gibt keinen offiziellen Anthropic-Adapter, der fremde CLI-Agenten nativ einbindet.**

**Konkretes Vorbild:** Der Community-Orchestrator **NEEDLE** verteilt Aufgaben aus einer
SQLite-Queue mit **atomaren Claims** an headless CLI-Backends — ausdruecklich Claude Code, Codex,
OpenCode und Aider. Fortschritt laeuft ueber eine explizite State Machine, und **bewusst gibt es
KEINEN Laufzeit-Kommunikationskanal zwischen den Agenten**: die Koordination passiert vorher, bei
der Aufgabenzerlegung. Quelle: https://github.com/andyrewlee/awesome-agent-orchestrators

**Claude Code als MCP-Server:** `claude mcp serve` exponiert Claude Codes Werkzeuge ueber MCP, sodass
andere MCP-Clients (Claude Desktop, Cursor, Windsurf) Claude Code beauftragen koennen — "Agent im
Agent". Fuer beliebige CLI-Werkzeuge gilt das gleiche Muster: duenner JSON-RPC-ueber-stdio-Adapter,
der jeden Subcommand als MCP-Tool mit Schema listet.

**Git-Worktrees gegen Schreibkonflikte:** `isolation: worktree` im Subagent-Frontmatter bzw.
`claude --worktree <name>` ist der native Mechanismus. Worktrees teilen die `.git`-Objektdatenbank,
haben aber eigenes HEAD/Index/Working-Tree — das verhindert Dateikollisionen, Index-Korruption bei
gleichzeitigem `git add` und "Context Contamination" durch halbfertige Dateien.
- flache Geschwister-Verzeichnisse (`projekt-feature-a/`), nicht verschachteln
- Port-/DB-/Cache-Konflikte ueber Worktree-Index-Offsets, eigene SQLite je Worktree, branch-
  geprefixte Container-Namen loesen
- **praktische Grenze 8-10 parallele Worktrees**, danach uebersteigt die Verwaltung den Nutzen
  (Beispielmessung: 9,82 GB bei 20 Agenten auf einer 2-GB-Codebase — Einzelbeobachtung, `extern`)
- `PreToolUse`-Hooks koennen Schreibzugriffe ausserhalb der eigenen Worktree hart blocken
Quelle: https://code.claude.com/docs/en/sub-agents, https://code.claude.com/docs/en/hooks

**OpenCode-Seite** `offiziell`: zwei Agent-Typen (Primary: Build/Plan; Subagents: General, Explore,
Scout — read-only). Konfiguration per `opencode.json` oder Markdown unter
`~/.config/opencode/agents/` bzw. `.opencode/agents/`. Modell-IDs als `provider/model-id`.
**Subagents erben standardmaessig das Modell des aufrufenden Agenten**, sofern nicht ueberschrieben —
genau das macht "der Schwarm laeuft auf dem Session-Modell" moeglich.
Quelle: https://opencode.ai/docs/agents/

## §7 Kosten der Verbindung einplanen `extern`

- Eine **Drei-Agenten-Pipeline verbraucht ca. 29.000 Tokens** gegenueber ~10.000 bei einem
  aequivalenten Ein-Agenten-Ansatz — knapp 3x.
- **Tool-Schema-Overhead** liegt bei 10.000-60.000 Tokens in typischen Multi-Server-Deployments und
  macht **60-80 % des Tokenverbrauchs** bei statischen Toolsets aus.
- Ein Workflow, der im Test 0,50 $ kostet, kann bei 100.000 Ausfuehrungen/Monat auf 50.000 $/Monat
  eskalieren, weil der Orchestrator ZUSAETZLICH zu jedem Worker-Call eigene LLM-Aufrufe fuer
  Zerlegung und Aggregation macht.
Quelle: https://www.augmentcode.com/guides/multi-agent-cost-compounding

**Gegenmassnahmen:** Toolsets je Agent minimieren oder dynamisch laden statt alle Schemas immer
mitzuschicken; Orchestrierungs-Overhead im **Lasttest** messen, nicht nur im Funktionstest.

## §8 Sicherheit ist bei beiden Hauptprotokollen ungeloest `extern`

Kurzform — Details und Fixes im Almanach `bugs/agents/multi-agent-interop.md`:
- **MCP Tool Poisoning:** ~5,5 % von 1.899 untersuchten Servern betroffen; Command Injection bei
  43 % der getesteten Server; SSRF-Anfaelligkeit bei 36,7 % von 7.000+ Servern.
- **A2A:** Agent-Impersonation, Context Poisoning von Agent Cards, Privilegien-Eskalation durch zu
  grobe OAuth-Scopes, **keine erzwungenen Token-Ablaufzeiten in der Spezifikation**.

**Regeln:** fremde MCP-Server strikt whitelisten und Tool-Beschreibungen sanitizen. A2A-Tokens
granular auf einzelne Agent-Skills scopen (Least Privilege), kurze Ablaufzeiten selbst erzwingen,
bei hoeherem Schutzbedarf mTLS-gebundene Tokens oder DPoP.

---

## Was 2026-09-09 NICHT belegt werden konnte

- Ob Anthropic A2A informell unterstuetzt (relevant fuer jede Claude-↔-Fremdagent-Architektur).
- Ob das arXiv-Governance-Paper das Zed-ACP oder das IBM/BeeAI-ACP meint.
- Die genaue A2A-Versionshistorie zwischen 1.0 und 1.0.1 (nur Sekundaerquelle).
- Dedizierte Primaerquellen zu schlichtem Message-Passing (Datei/Queue/HTTP) als anbieteruebergreifendes
  Muster — nur implizite Erwaehnungen als "kleinster gemeinsamer Nenner".
- Eine offizielle Anthropic-Doku dazu, wie man mit dem Claude Agent SDK Agenten baut, die FREMDE
  CLI-Agenten aufrufen. Nur das allgemeine Headless-Subprocess-Muster und Community-Beispiele.

# Serverseitige autonome KI-Agenten — Best Practices (Praeventions-Seite)

> **Zweite Seite der Medaille zum Bug-Almanach** [`bugs/server/ai-agent-frameworks.md`](../../bugs/server/ai-agent-frameworks.md):
> dort *was schiefgeht*, hier *wie man einen serverseitigen autonomen Agenten von vornherein richtig baut*.
> Gilt fuer: den `agent`-Dienst im `second-brain-server`-Stack (eigene FastAPI-Tool-Loop mit Gemini) UND den
> geplanten "Bibliothekar/Dirigent"-Agenten (liest das Gehirn via brain-api/Qdrant).
>
> **Stand:** 2026-06-24 (recherchiert: OpenRouter `:online`-Schwarm; je Empfehlung Quelle + offiziell/extern-Flag).
> **Anker:** Python 3.12 · FastAPI 0.138.0 · google-genai>=2.9.0 · httpx>=0.27.0 (eigene Loop) · Pydantic-AI ~1.x · LangGraph ~1.0/1.2.
>
> **Abgrenzung:** NICHT `best-practices/second-brain/orchestrator-und-suche.md` (Router-Intent + Suchstrategie) ·
> NICHT `best-practices/agents/orchestrator-agent.md` (Claude-Code-Subagenten). HIER: der serverseitige Agent technisch.
> **Ehrlichkeit:** Punkte ohne frische Quelle sind als `kanonisch` markiert bzw. aus dem Almanach abgeleitet.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Thema | Best Practice (Kurzform) | Almanach |
|---|-------|--------------------------|----------|
| 1 | Framework-Wahl | Klein+robust+lesend → **eigene schlanke Tool-Loop** ODER **Pydantic-AI** (typisiert, leicht); **LangGraph** erst bei echter Graph-/Checkpoint-Komplexitaet | §7/§8 |
| 2 | Single vs. Multi | Mit EINEM Agenten + guten Tools starten; Multi erst wenn Kontext/Capabilities es erzwingen | §1 (Loop) |
| 3 | Tool-Trennung | Lesende vs. schreibende Tools strikt trennen, Scope serverseitig validieren | §5 |
| 4 | Schreib-Tools | Idempotent (Idempotency-Key) — Agent ruft sie im BG mit Retry | §5.2 |
| 5 | Tool-Schema | Striktes JSON/Zod-Schema (`strict`), wenige klar benannte Tools, gute `description` | §4.1 (Tool-Protokoll) |
| 6 | Tool-Fehler | Als `tool_result`-Fehlertext ans LLM zurueck (es kann korrigieren), nie Crash; Timeout pro Tool | §3.2/§3.3 |
| 7 | Hard-Stop | Deterministischer Abbruch (max Turns/Zeit) UNABHAENGIG vom LLM — Notbremse | §1.1 |
| 8 | Kosten | Token-Budget das STOPPT (nicht nur warnt); usage.total_tokens abziehen | §2.1 |
| 9 | Kontext | Trimming (billig) vor Summarization; Kontext „informativ aber knapp"; Pointer statt Voll-Output | §2.2 |
| 10 | Memory-Zugriff | Read/Write-Scopes getrennt, Least-Privilege, kein Secret im Kontext, Timeouts | §5 |
| 11 | Architektur | Schlanker Intent-Router (klein/lokal) ↔ staerkerer lesender Server-Agent; separate Kontextfenster; Modell-pro-Rolle | §5 (Concurrency) |
| 12 | Observability | Strukturierte JSON-Logs (Eingaben/Entscheidungen/Tool-Calls) + OTel-Tracing + Evals + Token/Kosten-Tracking | (Direktive #2) |
| 13 | Intent-Checkpoints | erwartet-vs-tatsaechlich loggen (eigener Kanal) — bestaetigen, ob Logik wie gemeint ankommt | (Live-Logik-Sonden) |
| 14 | Prompt-Injection | Gespeicherte Memory-Inhalte als UNTRUSTED; beim Recall als DATEN markieren; Lethal Trifecta brechen | §6.1 |
| 15 | async | Blockierende Calls NIE im async-Loop (`to_thread`/`run_in_threadpool`) | §3.1 |
| 16 | Streaming-Ausgabe | Irreversible Verbraucher wie TTS erst aus dem finalisierten kanonischen Reply speisen; Anzeige/Persistenz/Sprache duerfen nicht auseinanderlaufen | §9.1 |
| 17 | Live-Mirror/Hintergrund-Flush + Loeschung | Wer aus lebendem Session-State in die Persistenz spiegelt, muss im Delete-Pfad die Live-Session evicten, geplante Mirrors per Tombstone stoppen und einen zwischenzeitlich gelandeten Rest idempotent abraeumen — sonst "aufersteht" der geloeschte Eintrag | §9.2 |

---

## TL;DR — die 7 Leitsaetze

1. **So klein wie moeglich:** ein Agent + gute Tools; eigene Tool-Loop oder Pydantic-AI reichen fuer einen Memory-Agenten. LangGraph nur bei echtem Graph-Bedarf.
2. **Hard-Stop ist Pflicht:** deterministischer Abbruch (Turns/Zeit/Budget) ausserhalb des LLM — das LLM weiss nicht, wann Schluss ist.
3. **Kosten-Cap STOPPT:** Token-Budget pro Session, nicht nur Alert.
4. **Tools sauber:** read/write getrennt, Schema-validiert, idempotent, Fehler als tool_result zurueck — nie Crash.
5. **Memory sicher:** Least-Privilege-Scopes, kein Secret im Kontext, Read/Write getrennt.
6. **Rollen trennen:** billiger Router (klassifiziert) ↔ lesender Bibliothekar (verdichtet); dokumentierte Handlungsraeume, Modell-pro-Rolle.
7. **Untrusted Memory:** gespeicherte Inhalte sind Daten, keine Befehle — Trust-Boundaries + Lethal Trifecta brechen.

---

## §1 Framework-Wahl (kanonisch + Almanach §7/§8; Recherche lieferte nur Quellenliste)

| Option | Wann | Begruendung |
|--------|------|-------------|
| **Eigene schlanke Tool-Loop** (Status quo `agent/app.py`) | Sehr einfacher Agent: 1 LLM-Call/Turn, ein paar REST-Tools, volle Kontrolle | Kein Framework-Ballast, keine Breaking-Changes Dritter; ideal fuer den Speicher-Bibliothekar. Almanach §7/§8 zeigen, wie viel Framework-Bugs man sich damit spart |
| **Pydantic-AI (~1.x)** | Typisierte Outputs, Tool-Calling, `UsageLimits`, mehrere Provider, ohne grosse Graph-Komplexitaet | Leichtgewichtig + pydantic-typisiert; **ABER** offene message_history/Roundtrip-Issues (Almanach §7.3) und V2-Major (PR #5451) → erst pruefen |
| **LangGraph (~1.0/1.2)** | Echte Zustandsgraphen, Checkpointing/Persistenz, Human-in-the-Loop, verzweigte Workflows | Maechtig, aber maechtig fehleranfaellig: `operator.add`-Duplikation, GraphRecursionError, SQLite-locked, prebuilt-Breaking (Almanach §8). Nur wenn der Graph-Bedarf real ist |

- **Do:** Mit Single-Agent + gut designten Tools starten; Multi-Agent erst wenn Capabilities/Kontext es erzwingen (separate Kontextfenster). **Don't:** „auf Vorrat" ein komplexes Multi-Agent-/LangGraph-System bauen. (langchain — extern.)
- **Empfehlung fuer den Bibliothekar:** eigene Loop ODER Pydantic-AI; LangGraph erst, wenn echte Zustands-/Checkpoint-Logik gebraucht wird.

## §2 Tool-Calling & Tool-Design

- **Do — Read/Write trennen + Scope serverseitig validieren** (MCP: Tool-Annotations fuer destruktive/Open-World-Tools). **Don't:** ein Tool ohne Scope-Check lesen+schreiben lassen. (composio, anthropic — extern/offiziell.) Almanach §5.
- **Do — Schreib-Tools idempotent** (POST/PATCH bewusst idempotent; Agents rufen sie im BG mit Backoff/Retry). **Don't:** auf Output-Caching der „ersten Response" verlassen (erster Call koennte Timeout trotz Erfolg sein). (qlong, composio.)
- **Do — Striktes Eingabe-Schema** (JSON Schema/Zod, dem LLM verfuegbar UND serverseitig validiert; `strict` wo Provider es kann). **Don't:** freie Textparameter, Schema nur in der Tool-Beschreibung. (ai-sdk, paragon.)
- **Do — Fehler als `tool_result` zurueck** (kanonisch/indirekt belegt): Tool-Exception fangen und als strukturierten Fehlertext ans LLM geben, damit es korrigiert/retryt — nicht crashen (Almanach §3.2). Output normalisieren (JSON, nicht rohes XML/Massendaten). Timeout pro Tool (Almanach §3.3).
- **Do — Wenige, klar benannte Tools** mit praeziser `name`/`description`/Schema (das LLM waehlt per semantischer Uebereinstimmung). **Don't:** generische Namen („tool1"), leere Beschreibungen. (anthropic, paragon.)

## §3 Harte Limits & Kontext-Trimming

- **Do — Deterministischer Hard-Stop (Almanach §1.1, kanonisch):** jeder Lauf hat einen Abbruch auf max Turns (LLM-Calls) UND Gesamtzeit, ausserhalb des LLM (Kill-Switch in Mikrosekunden); plus Schleifen-Erkennung (Duplicate-Chain/No-Progress). **Don't:** „soft loop prevention" als einzige Bremse; auf das LLM-„done" verlassen. (Recherche-Luecke ehrlich; aus Almanach §1 + Direktiven gefuellt.)
- **Do — Token-/Kosten-Budget das STOPPT:** auf Token-Ebene limitieren (`usage.total_tokens` pro Antwort abziehen), bei ≤0 geordnet abbrechen — nicht nur warnen/mailen (Almanach §2.1). **Don't:** request-basiertes Rate-Limit (variiert zu stark); Alert ohne Enforcement. (zuplo.)
- **Do — Kontext-Trimming vor Summarization:** alte/irrelevante Nachrichten prunen (heuristisch, kostenlos, z.B. `trim_messages`/`AGENT_HISTORY_MAX`); Summarization nur wenn Trimmen nicht reicht. Kontext „informative, yet tight" halten; grosse Tool-Outputs per Pointer auslagern. **Don't:** alles behalten „weil das Modell gross ist". (langchain, anthropic, comet.)

## §4 Tool-Protokoll-Hygiene (aus Almanach §4 — Praevention)

- **Do — tool_use ↔ tool_result strikt 1:1** halten; bei History-Trimming/Compaction Paare ATOMAR entfernen/behalten; abgebrochene Tool-Calls mit synthetischem Fehler-tool_result abschliessen; Assistant-Antwort verbatim (inkl. tool_use-IDs) in die History. **Don't:** Paare trennen → HTTP 400 (Almanach §4.1). Relevant, sobald der Dirigent echte Multi-Turn-Tool-Loops mit Anthropic/Tool-Calling baut.

## §5 Sichere Memory-Anbindung (Server-Agent → brain-api/Qdrant)

- **Do — Read/Write-Trennung** ueber unterschiedliche Service-Identitaeten/Scopes; Zugriffsumfang pro Richtung minimal. **Don't:** ein „Super-Key" mit Vollzugriff. (stackoverflow — generisch; Qdrant-RBAC-Details Luecke.)
- **Do — Least-Privilege** je Operation, Scope serverseitig validieren (Whitelist, kein Client-Trust); distinkte, kurzlebige Credentials pro Agent. **Don't:** breite/Wildcard-Scopes, geteilte Service-Accounts. (stackoverflow.)
- **Do — Kein Secret im Kontext:** Tokens/Keys nur serverseitig; nie in Prompt/Tool-Args/Memory/Logs; Browser-Client via BFF/Token-Handler. **Don't:** Secrets in Memory-Eintraege/Browser-Storage. (curity.) → Voll konsistent mit `agent/app.py` (alle Secrets aus Env).
- **Do — Timeouts + Konsistenz/Idempotenz (Almanach §5, kanonisch gefuellt):** harte Timeouts auf brain-api-Calls; Schreib-Tools idempotent (Doppel-Speicherung bei Retry vermeiden); bei parallelen Sessions State als shared mutable behandeln (DB-backed statt naivem In-Memory). (Recherche-Luecke ehrlich; aus Almanach §5 ergaenzt.)

## §6 Architektur-Trennung: Router/Dirigent ↔ Bibliothekar

- **Do — Schlanker Intent-Router als eigene Schicht:** klassifiziert + delegiert; klein/regelbasiert/billig, ggf. lokal. **Don't:** Routing in den teuren Reasoning-Agent bauen (vermischt Rollen/Kontexte). (arxiv, linkedin — extern.)
- **Do — Separate Kontextfenster pro Agent** (Router klein, Bibliothekar gross+lesend) — Kontext-Trennung genau dann, wenn Router-Kontext sonst das Spezialwissen verdraengt. (langchain.)
- **Do — Modell-pro-Rolle:** staerkeres Modell fuer Orchestrierung/Lesen, kleines/regelbasiertes fuer Routing (Anthropic: Opus-Lead + Sonnet-Sub schlug Single-Opus um 90,2 %). **Don't:** ueberall dasselbe Frontier-Modell (Kosten/Latenz). (langchain.)
- **Do — Dokumentierte Rollen + Handlungsraeume + Audit-Trail:** Router entscheidet NUR ueber Routing, Bibliothekar NUR ueber Lesen/Verdichten; „lese das Gehirn" und „fuehre Aktion aus" trennen. **Don't:** Black-Box, „der eine ruft den anderen ad hoc". (assecor, growhuman.)

## §7 Observability (Direktive #2 + Recherche)

- **Do — Strukturierte JSON-Logs:** Eingaben, Planner-Entscheidungen, Routing, Tool-Calls, Outcomes; GenAI-Semantic-Conventions; filterbar pro Prompt/Session. **Don't:** Plain-Text-Logs; nur per-Prompt statt System-Ebene. (activewizards, atlan.) → `agent/app.py` macht das bereits (JSON-Logger).
- **Do — Tracing:** OpenTelemetry als Foundation (GenAI-Semantic-Conventions, kein Vendor-Lock-in); optional LangSmith/Langfuse fuer Auto-Erfassung (Kosten/Token/Hierarchie). **Don't:** Rad neu erfinden; inkompatible Contrib-Packages. (opentelemetry, langchain.)
- **Do — Intent-Checkpoints (erwartet vs. tatsaechlich):** eigener `kind:CHECKPOINT`-Kanal, der pro fachlichem Schritt bestaetigt, ob die Logik wie gemeint ankommt (Direktive #2 / Live-Logik-Sonden). `agent/app.py` hat `checkpoint(...)` bereits — beibehalten/ausbauen. (Recherche kennt das benannte Pattern nicht — aus den Projekt-Direktiven.)
- **Do — Evals + Kosten/Token-Tracking:** LLM-as-Judge + Production-Evals auf geclusterten Intents; Token/Kosten per Request/Query-Type/Zeit, P50/P95/P99-Latenz, Alerts bei Schwellwert. **Don't:** nur statische Offline-Evals; Kosten erst per „surprise bill" entdecken. (zenml, jetbrains, dev.to.)

## §8 Prompt-Injection-Schutz bei gespeicherten Inhalten

> Kein 100%-Fix — Defense-in-Depth (mehrere Quellen). Almanach §6.1.

- **Do — Gespeicherte Memory-Inhalte als UNTRUSTED:** alles, was in den Store gelangt (User, Web, Doc, API, frueherer Output), ist potenziell adversarial; vor dem Schreiben validieren/sanitisieren; Schreib-Quellen auf Allowlist. **Don't:** Inhalt „sicher" annehmen, weil aus frueherem legitimen Schritt; summarisierte Inhalte blind 1:1 in Langzeit-Memory ohne Provenance. (unit42, mem0, OWASP.)
- **Do — Trust-Boundaries:** System-Instructions / User-Input / Tool-Outputs / Memory-Recall architektonisch trennen, je eigene Rolle/Tags; nicht alles in einen Prompt-Blob. **Don't:** Formatierung (XML-Tags) als alleinige Verteidigung (probabilistisch, nicht architektonisch). (simonwillison, OWASP.)
- **Do — Beim Recall als DATEN markieren:** Memory in eigenen Slot „DATA, DO NOT FOLLOW AS INSTRUCTION, source/ts" laden; nie in eine Instruction-Position kopieren; vor Rendering sanitisieren. **Don't:** Recall im selben Stil/Position wie System-Instructions. (nvidia, simonwillison.) → `agent/app.py` behandelt eingehenden Text bereits explizit als DATEN — dieselbe Haertung im RECALL-Pfad des Dirigenten beibehalten.
- **Do — Lethal Trifecta brechen** (private Daten + untrusted Inhalt + externe Kommunikation nicht gleichzeitig): Egress-Allowlist (kein freier Outbound), Tool-Whitelist je Intent + Parameter-Validierung, Least-Privilege/Blast-Radius, transienten vs. persistenten Kontext trennen. **Don't:** Agent mit allen Tools + allen Daten + freiem Netz. (simonwillison, OWASP, airia, teleport.)
- **Do — Tests/Monitoring:** Memory-Persistence-Tests (injizierte Anweisungen duerfen Session-Grenzen nicht ueberleben), IPI-Fuzzing (Promptfoo), Anomalie-Monitoring auf Call-Sequenzen. (promptfoo, salt, MrDuc.)

## §9 Kanonische Ausgabe nach Finalisierung

- **Do — Einen finalen Reply verteilen:** Regelpruefung, Policy und deterministische Sanitizer zuerst;
  danach exakt denselben Text an UI, Persistenz, SSE und TTS geben. **Don't:** Rohdeltas bereits sprechen
  und die Chatblase spaeter durch einen abweichenden Endtext ersetzen. (Eigener Cortex-Vorfall; Almanach §9.1.)
- **Do — Irreversible Senken spaet anbinden:** TTS, Benachrichtigungen und externe Publikation erst nach
  der letzten Transformation starten. Heartbeats halten lange Finalisierung offen. **Don't:** niedrige
  First-Token-Latenz ueber die fachliche Textidentitaet stellen.
- **Do — Defense in Depth:** Produktvorgaben wie „keine URLs/Quellen im Antworttext" im Prompt,
  serverseitig deterministisch und clientseitig vor TTS erzwingen. Strukturierte Quellenmetadaten koennen
  getrennt erhalten bleiben. Regressionstest mit absichtlich quellenhaltigem Rohentwurf.
- **Do — Capability fail-closed aushandeln:** Der Server bestaetigt im Stream-Handshake explizit, dass
  Deltas kanonisch sind. Fehlt das Flag bei altem oder teilweise deploytem Backend, verwirft der Client
  alle Vorab-Deltas und nutzt nur den finalen Reply. **Don't:** Sicherheit aus Versionsnummern erraten.

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei) | Bug-Gegenpart in `bugs/server/ai-agent-frameworks.md` |
|---|---|
| §1 Framework-Wahl | §7 Pydantic-AI, §8 LangGraph |
| §2 Tool-Calling/Design | §3.2 Tool-Fehler, §5.2 Idempotenz |
| §3 Harte Limits/Trimming | §1 Loop-Steuerung, §2 Kosten/Token/Kontext |
| §4 Tool-Protokoll-Hygiene | §4.1 orphaned tool_use → 400 |
| §5 Memory-Anbindung | §5 State/Concurrency/Idempotenz, §6.3 Secrets |
| §6 Architektur Router/Bibliothekar | §5.1 shared state, §5.3 Worker/In-Memory |
| §7 Observability | (Direktive #2; §1/§2 sichtbar machen) |
| §8 Prompt-Injection | §6.1 Memory-Poisoning/Lethal Trifecta |
| §9 Kanonische Ausgabe | §9.1 Rohentwurf wird gesprochen, finale Antwort zeigt ihn nicht |
| (async-Hinweise) | §3.1 Blocking im async-Loop |

---

## Pflicht-Checkliste (vor Bau/Aenderung des Agenten)

```
□ Framework bewusst gewaehlt (eigene Loop/Pydantic-AI; LangGraph nur bei Graph-Bedarf)? (§1)
□ Read/Write-Tools getrennt, Schema-validiert, idempotent, Fehler als tool_result? (§2)
□ tool_use/tool_result strikt 1:1 (Trimming atomar)? (§4)
□ Deterministischer Hard-Stop (Turns/Zeit) + Schleifen-Erkennung? (§3)
□ Token-/Kosten-Budget das STOPPT + Kontext-Trimming? (§3)
□ Memory: Least-Privilege-Scopes, kein Secret im Kontext, Timeouts, idempotente Writes? (§5)
□ Router (klein/lokal) ↔ Bibliothekar (server/liest) getrennt, Modell-pro-Rolle, Audit? (§6)
□ JSON-Logs + OTel-Tracing + Intent-Checkpoints + Token/Kosten-Tracking? (§7)
□ Gespeicherte Memory-Inhalte UNTRUSTED, beim Recall als DATEN markiert, Lethal Trifecta gebrochen? (§8)
□ UI, Persistenz, SSE und TTS erhalten denselben finalisierten kanonischen Reply? (§9)
□ Keine blockierenden Calls im async-Loop (to_thread/run_in_threadpool)? (Almanach §3.1)
```

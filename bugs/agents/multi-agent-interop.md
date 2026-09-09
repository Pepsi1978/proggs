# Bekannte Bugs: Modelluebergreifende Agenten-Zusammenarbeit (MCP, A2A, Cross-Vendor)

> **PFLICHT-LESEN vor Arbeit an einer Verbindung zwischen Agenten** — egal ob ueber MCP, A2A,
> OpenAI-Handoffs, dateibasierte Uebergabe oder Queue.
> **Geltungsbereich:** die VERBINDUNG — Protokolle, Handoffs, Cross-Vendor-Betrieb, Schreibkonflikte
> zwischen parallelen Agenten, Kosten und Sicherheit dieser Verbindung.
> Die Schleife selbst: `bugs/agents/loop-engineering.md`. Routing/Spawning in EINEM Harness:
> `bugs/agents/orchestrator-agent.md`.
> **Stand:** recherchiert am 2026-09-09 mit 21 parallelen Researchern (7 Firecrawl+Tavily→DeepSeek,
> 7 DeepSeek `:online`, 7 Sonnet-5), je dieselben 7 Unterthemen.
>
> Begleitseite (wie man es von vornherein richtig macht): `best-practices/agents/multi-agent-interop.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektuere
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Symptom | Ursache in einem Satz | Fix |
|---|---------|----------------------|-----|
| 1 | MCP-Server fuehrt versteckte Anweisungen aus | Tool-Beschreibungen landen ungeprueft im Kontext | §1 |
| 2 | A2A-Token nach Leak weiter gueltig | Spec erzwingt keine Ablaufzeit | §2 |
| 3 | Kosten explodieren erst in Produktion | Orchestrator-Calls kommen ZUSAETZLICH zu Worker-Calls | §3 |
| 4 | Handoff funktioniert nicht ueber Anbietergrenze | Handoffs sind SDK-intern, kein Protokoll | §4 |
| 5 | Zwei Agenten bearbeiten dieselbe Aufgabe | Ledger ohne atomaren Claim | §5 |
| 6 | `git add` korrumpiert den Index | Mehrere Agenten im selben Working Tree | §6 |
| 7 | Ergebnisse eines Laufs tauchen im naechsten auf | Ausgabeverzeichnis nicht aufgeraeumt | §7 |
| 8 | "ACP unterstuetzt das doch" — tut es nicht | Zwei verschiedene Protokolle, gleiches Kuerzel | §8 |
| 9 | Recherche liefert nur SEO-Blogs | Such-Engine bevorzugt optimierte Seiten vor Primaerquellen | §9 |

---

## §1 MCP Tool Poisoning — versteckte Anweisungen in Tool-Beschreibungen

**Symptom:** Ein MCP-Server liefert scheinbar normale Tool-Beschreibungen; der Agent fuehrt darin
versteckte Anweisungen aus, die der Nutzer nie gesehen hat.

**Ursache:** Tool-Metadaten und -Beschreibungen werden ungeprueft in den Kontext des LLM uebernommen
und als vertrauenswuerdig behandelt. Das ist indirekte Prompt-Injection ueber einen Kanal, den man
nicht als Eingabe wahrnimmt.

**Verbreitung (Stichproben 2026):** ~5,5 % von 1.899 untersuchten Servern betroffen. Zusaetzlich:
Command Injection bei 43 % der getesteten Server, SSRF-Anfaelligkeit bei 36,7 % von 7.000+ Servern.

**Fix:** Es gibt **keinen universellen Fix**. Die Client-seitige Guardrail-Qualitaet unterscheidet
sich — laut Quelle hat Claude Desktop staerkere Guardrails, Cursor ist anfaelliger fuer
Cross-Tool-Poisoning. Praktisch: fremde Server strikt whitelisten, Tool-Beschreibungen sanitizen,
keinen unbekannten Server "mal eben" einbinden.

Quelle: https://www.practical-devsecops.com/mcp-security-vulnerabilities/ (2026),
https://owasp.org/www-community/attacks/MCP_Tool_Poisoning

## §2 A2A — Token-Replay und Privilegien-Eskalation

**Symptom:** Ein geleaktes A2A-Token bleibt gueltig und wird wiederverwendet; ein Agent kommt an
Faehigkeiten, die er nicht braucht.

**Ursache:** Die A2A-Spezifikation definiert **kein eigenes Autorisierungs-Framework**. Scope-Design
und Token-Lebensdauer bleiben jeder Implementierung selbst ueberlassen — in der Praxis werden Scopes
zu grob geschnitten und Ablaufzeiten gar nicht gesetzt.

**Fix:** Tokens granular auf einzelne Agent-Skills scopen (Least Privilege). Kurze Ablaufzeiten
selbst erzwingen, nicht auf die Spec warten. Bei hoeherem Schutzbedarf mTLS-gebundene Tokens oder
DPoP. Agent Cards validieren (Context Poisoning ueber manipulierte Cards ist ein eigener Vektor).

Quelle: https://tyk.io/learning-center/a2a-security-the-developers-complete-guide/,
Bedrohungsmodell: https://cloudsecurityalliance.org/blog/2025/04/30/threat-modeling-google-s-a2a-protocol-with-the-maestro-framework

## §3 Kostenexplosion faellt erst in Produktion auf

**Symptom:** Eine Pipeline, die im Test 0,50 $ pro Lauf kostet, liegt bei 100.000 Ausfuehrungen im
Monat bei ~50.000 $ — bemerkt wird das erst nach dem Rollout.

**Ursache:** Zwei Multiplikatoren, die im Funktionstest unsichtbar bleiben:
1. Der Orchestrator macht **zusaetzliche** LLM-Aufrufe fuer Zerlegung und Aggregation — obendrauf
   auf jeden Worker-Call. Eine Drei-Agenten-Pipeline liegt bei ~29.000 Tokens gegen ~10.000 beim
   Ein-Agenten-Aequivalent.
2. **Tool-Schema-Overhead** macht 60-80 % des Tokenverbrauchs aus, wenn statische Toolsets bei jedem
   Aufruf komplett mitgeschickt werden (10.000-60.000 Tokens in typischen Multi-Server-Setups).

**Fix:** Toolsets je Agent minimieren oder dynamisch laden. Orchestrierungs-Overhead im **Lasttest**
messen, nicht nur im Funktionstest. Vor dem Rollout hochrechnen, nicht danach.

Quelle: https://www.augmentcode.com/guides/multi-agent-cost-compounding

## §4 OpenAI-Handoffs als Cross-Vendor-Bruecke missverstanden

**Symptom:** Die Architektur plant einen "Handoff" von einem OpenAI-Agenten an einen Claude-Agenten.
Es gibt keinen Mechanismus dafuer.

**Ursache:** Handoffs sind ein **SDK-internes Konstrukt** fuer die Uebergabe zwischen Agenten
innerhalb desselben Runs und Frameworks. Technisch als Tool an das LLM repraesentiert. Die offizielle
Doku behandelt ausschliesslich OpenAI-eigene Agenten; es gibt **keine dokumentierte Aussage** zur
Interoperabilitaet mit Claude oder Gemini.

Weitere Grenzen, die ueberraschen: Handoffs bleiben "within a single run"; sie sind linear (A→B→C)
ohne native Parallel- oder Rueckfuehrungsmuster; Input-Guardrails gelten nur fuer den ersten Agenten,
Output-Guardrails nur fuer den letzten; "Nested handoff history" ist Opt-in-Beta und standardmaessig
aus.

**Fix:** Fuer echte Cross-Vendor-Zusammenarbeit nicht auf Handoffs setzen. Entweder A2A (mit dem
Vorbehalt aus §8 unten) oder — belastbarer 2026 — geteiltes Dateisystem/Queue mit atomaren Claims
(siehe §5 und `best-practices/agents/multi-agent-interop.md` §5).

Quelle: https://openai.github.io/openai-agents-python/handoffs/

## §5 Ledger ohne atomaren Claim — zwei Agenten, eine Aufgabe

**Symptom:** Zwei parallele Agenten greifen dieselbe Aufgabe auf und produzieren widerspruechliche
Ergebnisse. Klassische Race Condition.

**Ursache:** Ein State-Ledger (Liste bereits verarbeiteter Einheiten) allein ist **nicht atomar**.
Zwischen "lesen, ob schon verarbeitet" und "als verarbeitet eintragen" liegt ein Fenster, in dem ein
zweiter Agent dasselbe liest. Die AWS-Autoren warnen ausdruecklich, dass ihr eigenes Ledger-Muster
bei gleichzeitiger Skalierung ohne atomare Claims bricht.

**Fix:** Drei Bausteine, alle drei noetig:
1. **Atomic Claim:** `os.open(pfad, os.O_CREAT | os.O_EXCL | os.O_WRONLY)` — nur ein Prozess kann
   die Datei exklusiv anlegen. Kein Advisory-Lock, keine Lock-Datei-Logik.
2. **Output-Existenzcheck** vor der Verarbeitung — macht den Schritt idempotent.
3. Ledger nur als Nachweis/Audit, **nie** als alleinige Koordination.

Zweite Ursache derselben Klasse: **vage Aufgabenbeschreibungen**. Anthropic berichtet, dass frueche
Versionen ihres Systems mit Anweisungen wie "research the semiconductor shortage" zu Duplikation
fuehrten — mehrere Agenten untersuchten dieselben Zeitraeume. Fix: detaillierte, nicht-ueberlappende
Task-Beschreibungen je Subagent.

Quelle: https://aws.amazon.com/blogs/storage/orchestrating-multi-agent-ai-architectures-with-amazon-s3-files/,
https://www.anthropic.com/engineering/multi-agent-research-system

## §6 Index-Korruption bei parallelen Agenten im selben Working Tree

**Symptom:** Gleichzeitiges `git add` mehrerer Agenten korrumpiert den Index; Agenten sehen
halbfertige Dateien der jeweils anderen ("Context Contamination").

**Ursache:** Alle Agenten arbeiten im selben Working Tree mit einem gemeinsamen HEAD und Index.

**Fix:** **Git-Worktrees.** In Claude Code nativ ueber `isolation: worktree` im Subagent-Frontmatter
bzw. `claude --worktree <name>`. Worktrees teilen die `.git`-Objektdatenbank, haben aber eigenes
HEAD/Index/Working-Tree.
- flache Geschwister-Verzeichnisse, nicht verschachteln
- Port-/DB-/Cache-Konflikte ueber Worktree-Index-Offsets und eigene SQLite je Worktree loesen
- `PreToolUse`-Hook, der Schreibzugriffe ausserhalb der eigenen Worktree blockt
- **praktische Grenze 8-10 parallele Worktrees** — darueber uebersteigt die Verwaltung den Nutzen
  (Einzelbeobachtung: 9,82 GB Plattenverbrauch bei 20 Agenten auf 2-GB-Codebase, nicht unabhaengig
  verifiziert)

Quelle: https://code.claude.com/docs/en/sub-agents, https://code.claude.com/docs/en/hooks

## §7 Ergebnisse eines frueheren Laufs schlagen als aktuelle durch

**Symptom:** Ein Schwarm liefert Ergebnisse zu einem voellig fremden Thema. Sie stammen aus einem
frueheren Lauf.

**Ursache:** Das Ausgabeverzeichnis wird mit `exist_ok=True` angelegt. Faellt ein Agent aus, schreibt
er keine neue Ausgabe — die **alte Datei des vorherigen Laufs bleibt stehen** und wird beim Auslesen
als aktuelles Ergebnis interpretiert.

**Fix:** Vor jedem Lauf die eigenen Ausgabemuster hart loeschen (Poka-Yoke Stufe 3), nicht nur
ueberschreiben. Nur die EIGENEN Muster anfassen, nie fremde Reste. Fuer Crash-Resume einen
ausdruecklichen Schalter vorsehen, der den Cleanup ueberspringt.

**Verwandter Fall, real getroffen 2026-09-09:** Laufen zwei Schwaerme gleichzeitig in dasselbe fest
verdrahtete Verzeichnis, raeumt der Start-Cleanup des zweiten die Ergebnisse des ersten weg. Das
Ausgabeverzeichnis muss je Lauf konfigurierbar sein.

Quelle: eigener Vorfall 2026-06-25 (`~/proggs/research-swarm.py`), Wiederholungsfall 2026-09-09.

## §8 "ACP" — zwei Protokolle, ein Kuerzel

**Symptom:** Eine Quelle nennt ACP als Agent-zu-Agent-Protokoll, die Doku beschreibt aber eine
Editor-Anbindung. Die Architekturentscheidung steht auf falscher Grundlage.

**Ursache:** Es gibt zwei:

| Kuerzel | Voller Name | Traeger | Zweck |
|---------|-------------|---------|-------|
| ACP | Agent **Client** Protocol | Zed Industries | Coding-Agent ↔ Editor |
| ACP | Agent **Communication** Protocol | IBM / BeeAI | Agent ↔ Agent |

Selbst ein arXiv-Governance-Paper (2606.31498) nennt "ACP" neben MCP und A2A, ohne zu spezifizieren
welches gemeint ist.

**Fix:** Bei jeder ACP-Erwaehnung zuerst den Traeger pruefen. Zed = Editor-Anbindung, IBM/BeeAI =
Agentenkommunikation.

**Verwandter Stolperstein — Anthropic fehlt bei A2A:** In der Traegerliste der Linux Foundation
(AWS, Cisco, Google, IBM, Microsoft, Salesforce, SAP, ServiceNow) wird Anthropic nicht genannt,
obwohl Anthropic AAIF-Gruendungsmitglied ist (fuer MCP). Wer A2A als Bruecke zu einem
Anthropic-Agenten einplant, sollte den aktuellen Stand pruefen statt es vorauszusetzen.

Quelle: https://zed.dev/blog/acp-registry, https://arxiv.org/abs/2606.31498,
https://www.linuxfoundation.org/press/a2a-protocol-surpasses-150-organizations-lands-in-major-cloud-platforms-and-sees-enterprise-production-use-in-first-year

## §9 Recherche-Agenten bevorzugen SEO-Seiten vor Primaerquellen

**Symptom:** Ein Recherche-Schwarm liefert formal saubere Antworten, aber die Belege sind
Marketing-Blogs statt Hersteller-Dokumentation oder Papers. Faellt in automatisierten Evals **nicht**
auf, weil die Antwort strukturell in Ordnung aussieht.

**Ursache:** Retrieval-Bias. Anthropic hat das im eigenen Multi-Agent-System gefunden: Subagenten
bevorzugten systematisch SEO-optimierte Seiten gegenueber akademischen PDFs. Entdeckt wurde es nur
durch **menschliches Testen**, nicht durch LLM-as-Judge.

**Eigene Messung 2026-09-09** (21 Researcher, dieselben 7 Unterthemen, drei Engines):

| Engine | Primaerquellen-Anteil | URLs in der Antwort |
|--------|----------------------|---------------------|
| Firecrawl+Tavily → DeepSeek | 5 % (der geholten Quellen) | **0** |
| DeepSeek `:online` (parallel.ai) | 11 % (zitiert) | 39,0 |
| Sonnet-5-Schwarm (eigene Suche) | **44 %** (zitiert) | 36,9 |

Die Schnittmenge zwischen den Domains von `:online` und dem Sonnet-Schwarm betrug **8 von 141**.
Unter den Domains, die nur `:online` fand, waren u.a. `context.reverso.net` (ein
Uebersetzungswoerterbuch) in vier von sieben Unterthemen.

**Ursache des Unterschieds:** Einmalige Suche gegen iteratives Nachfassen. Die Skript-Engines setzen
eine Suchanfrage ab und verarbeiten das Ergebnis. Ein Agent mit eigener Suche liest an, erkennt
"diese Seite zitiert das Original" und holt das Original.

**Fix:**
- Bei Recherchen, die auf Herstellerangaben oder Papers beruhen muessen: Agenten-Schwarm mit eigener
  Suche nehmen, nicht die Snippet-Pipeline.
- Prompt gezielt auf Primaerquellen lenken ("bevorzuge offizielle Docs und arXiv vor Sekundaerliteratur").
- Menschliche Stichprobe der Quellenliste einplanen — automatisierte Evals sehen diesen Fehler nicht.

**Zweiter, verwandter Fund:** Der Auswerte-Prompt in `~/proggs/mm-research.py` verlangte "Nenne pro
Aussage die Quelle" und uebergab die Quellen als nummerierte Liste. Das Modell zitierte daraufhin
`(Quelle 3)` statt der URL — die Antwort war **ohne die Quellendatei nicht ueberpruefbar**. Fix:
im Prompt ausdruecklich die vollstaendige URL verlangen.

Quelle: https://www.anthropic.com/engineering/multi-agent-research-system; eigene Messung 2026-09-09.

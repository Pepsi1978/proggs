# Researcher & Internet-Recherche — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | parallele Researcher | max ~5–7 gleichzeitig (RPM/429); Continuous-Spawning statt Wellen | Maximale Anzahl paralleler Researcher |
| 2 | 429 / Rate Limit | Exponential Backoff + `retry-after` respektieren | Exponential Backoff & Retry |
| 3 | Scope je Researcher | enge Prompts, ~15 Web-Fetches als RPM-Schutz | Scope-Begrenzung |
| 4 | Ergebnisse sichern | Checkpointing (lossless in Datei auslagern) | Checkpointing |
| 5 | Findings | NICHT kappen — alle dokumentieren (1M-Kontext) | Scope-Begrenzung |
| 6 | Uebersetzer ≠ Researcher | Uebersetzungs-Agenten NICHT drosseln (kein Web/RPM) | Warum drosseln |

---

## Warum Researcher-Subagenten drosseln, Übersetzungs-Agenten aber nicht

- **Was:** Jeder Subagent macht pro Turn EINEN API-Call (`/v1/messages`). Ein Web-Recherche-Agent läuft typisch 15–30 maxTurns mit je einem WebSearch + WebFetch + Analyse — das sind **30–90+ API-Requests in 3–8 Minuten**. 5 parallele Researcher erzeugen so 150–450+ Requests in wenigen Minuten.
- **Übersetzungs-Agenten dagegen:** Lesen lokale Dateien, schreiben lokal, brauchen 3–8 Turns. Kaum API-Calls pro Minute. Das RPM-Budget bleibt unangetastet.
- **Das gedrosselte Limit:** RPM (Requests Per Minute). Bei Tier 1: **50 RPM** für Sonnet 4.x; Tier 2: **1.000 RPM**. Der 429-Fehler `rate_limit_error` + `retry-after`-Header erscheint bei Überschreitung.
- **Zusatz — Acceleration Limits:** Neben den Tier-Limits gibt es serverseitige Burst-Schutz-Limits. Bei plötzlichem Nutzungsanstieg (z.B. alle 10 Agents gleichzeitig starten) feuert der Server ein 429 mit "server is temporarily limiting requests" — das ist kein Usage-Limit, sondern Infrastruktur-Schutz gegen Bursts.
- **Kernformel:** Bei 5 parallelen Researcher-Agents à 20 Turns in 5 Minuten = 20 Requests/Min × 5 = **100 RPM** — bei Tier-1-Limit (50 RPM) sofort ein Problem.
- **Best Practice:** Max **5–8 Researcher parallel** (Tier 2+), mit **2–5 Sekunden Versatz** beim Start (Staggering), damit der Burst-Schutz nicht greift.
- **Quelle:** [https://platform.claude.com/docs/en/api/rate-limits](https://platform.claude.com/docs/en/api/rate-limits) (offiziell), Stand 2026-05-25

---

## Anthropic API Rate Limits: Die drei Dimensionen

- **Was:** Das API erzwingt drei getrennte Limits — RPM, ITPM, OTPM. Jedes einzeln kann 429 auslösen.
- **Aktuelle Tier-Werte für Claude Sonnet 4.x (gemeinsames Limit über Sonnet 4.6, 4.5, 4):**

| Tier | RPM | ITPM | OTPM |
|------|-----|------|------|
| Tier 1 | 50 | 30.000 | 8.000 |
| Tier 2 | 1.000 | 450.000 | 90.000 |
| Tier 3 | 2.000 | 800.000 | 160.000 |
| Tier 4 | 4.000 | 2.000.000 | 400.000 |

- **Wichtig:** Claude Sonnet 4.x hat ein **gemeinsames Limit** über alle Sonnet-Varianten. Alle gleichzeitigen Subagenten zählen in denselben Pool.
- **Token Bucket:** Kein harter Reset pro Minute, sondern kontinuierliche Auffüllung. Kurze Bursts können das Limit sofort leeren.
- **Best Practice:** Bei 429: `retry-after`-Header auslesen, genau diese Sekunden warten. Nie sofort wiederholen.
- **Quelle:** [https://platform.claude.com/docs/en/api/rate-limits](https://platform.claude.com/docs/en/api/rate-limits) (offiziell), Stand 2026-05-25

---

## Cache-aware ITPM: Nur ungecachte Tokens zählen

- **Was:** Für die meisten Claude-Modelle (außer Haiku 3.5†) zählen nur **ungecachte Input-Tokens** zum ITPM-Limit. Gecachte Tokens (`cache_read_input_tokens`) werden nicht angerechnet.
- **Für Researcher relevant:** CLAUDE.md, System-Prompts und Tool-Definitionen werden gecacht. Ein Researcher mit 2.000-Token-System-Prompt und 50-Token-Frage kostet nur 50 ITPM, nicht 2.050. ITPM ist bei Researchern damit selten der Engpass — RPM bleibt das Problem.
- **Best Practice:** System-Prompts aller parallelen Researcher-Agents gleich halten (gleicher Wortlaut → Prompt Caching aktiv). Senkt ITPM-Kosten drastisch.
- **Quelle:** [https://platform.claude.com/docs/en/api/rate-limits](https://platform.claude.com/docs/en/api/rate-limits) (offiziell), Stand 2026-05-25

---

## Concurrent Requests: Kein hartes Limit, aber Acceleration Limits

- **Was:** Die offizielle Anthropic-Doku nennt kein explizites Limit für gleichzeitige Requests. Eingeschränkt werden RPM und TPM. Trotzdem erscheinen 429-Fehler bei zu vielen parallelen Agents wegen Acceleration Limits.
- **Empfehlung Anthropic für Teams (aus code.claude.com):**
  - 1–5 User: 5–7 RPM pro User
  - 5–20 User: 2.5–3.5 RPM pro User
  - Implizit: ein einzelner Nutzer sollte **dauerhaft nicht über 5–7 RPM** stoßen.
- **Best Practice:** Researchers nicht alle gleichzeitig starten. 2–5 Sekunden Versatz zwischen jedem Agent-Start.
- **Quelle:** [https://code.claude.com/docs/en/costs](https://code.claude.com/docs/en/costs) (offiziell), Stand 2026-05-25

---

## Maximale Anzahl paralleler Researcher-Subagenten

- **Was:** Anthropic empfiehlt explizit, Subagenten nicht für einfache Tasks zu spawnen und sie auf genuín komplexe Erkundungsaufgaben zu beschränken.
- **Empirisch (extern):** `/batch` mit 30+ gleichzeitigen Subagenten schlägt zuverlässig fehl (GitHub Issue #42947). 5–8 parallele Researcher laufen typisch stabil.
- **Best Practice:**
  - **Max 5 parallele Researcher-Subagenten** als sicheres Limit (Tier 1+2)
  - Tier 3+: ggf. bis 8, aber immer mit Staggering
  - Bei >50 zu recherchierenden Items: Aufteilen auf mehrere Agents mit je eigenem Teilbereich
- **Quelle:** [https://code.claude.com/docs/en/costs](https://code.claude.com/docs/en/costs) (offiziell); [https://github.com/anthropics/claude-code/issues/42947](https://github.com/anthropics/claude-code/issues/42947) (extern)
- **Stand:** 2026-05-25

---

## Exponential Backoff & Retry bei 429

- **Was:** Bei einem 429 `rate_limit_error` enthält die Antwort `retry-after` (Sekunden). Anthropic empfiehlt, mindestens so lange zu warten und dann mit reduzierter Parallelität neu zu versuchen.
- **Best Practice:**
  - `retry-after`-Header immer auslesen und einhalten
  - Exponential Backoff: 1s → 2s → 4s → 8s (max 3 Versuche)
  - Concurrency beim Retry halbieren
  - Nie sofort ohne Pause wiederholen — verschlimmert das Rate-Limiting
- **Quelle:** [https://platform.claude.com/docs/en/api/rate-limits](https://platform.claude.com/docs/en/api/rate-limits) (offiziell für retry-after); Backoff-Pattern (allgemeine API-Best-Practice, extern)

---

## Scope-Begrenzung für Researcher-Prompts

- **Was:** Vage Prompts wie "recherchiere alles über X" führen zu vielen unkontrollierten Tool-Schleifen, langen Turns und hohem RPM-Verbrauch. Präzise Prompts reduzieren die Turns dramatisch.
- **Best Practice:**
  - Immer **max 50 Items pro Researcher-Agent** (Regel aus Robustness-Protocol)
  - Exakte Zentralfrage formulieren, nicht "alles recherchieren"
  - Harte Limits im System-Prompt: **max 8 WebSearches, max 5 WebFetches, max 8–10 Min**
  - Bei >50 Items: Aufteilen auf mehrere Agents mit je eigenem Teilbereich
  - Laufzeit-Timeout: bei Turn 15 von 18 maxTurns sofort zur Zusammenfassung springen
- **Quelle:** Interne Regel `~/.claude/rules/agent-and-researcher-rules.md`; [https://code.claude.com/docs/en/sub-agents](https://code.claude.com/docs/en/sub-agents) (offiziell) für Grundprinzipien

---

## Checkpointing: Ergebnisse zwischenspeichern

- **Was:** Wenn ein Researcher-Agent nach vielen Turns abbricht (429, Timeout, Kontext voll), gehen alle Zwischenergebnisse verloren. Ohne Checkpointing muss von vorne begonnen werden.
- **Best Practice:**
  - Researcher schreiben Teilergebnisse regelmäßig in eine Datei (z.B. `/tmp/researcher-checkpoint-N.md`)
  - Beim Absturz kann ein Folge-Agent den Checkpoint lesen und weitermachen
  - Sentinel-Datei (JSON) am Ende immer schreiben — auch bei Fehler (prefix `[ERROR:]` für Write-Back-Enforcer)
  - Bei Fehlern: Output mit `[ERROR:]` prefixen, damit der Write-Back-Enforcer ihn in "Offene Fehler & Probleme" routet
- **Quelle:** Sentinel-Pattern aus `writeback-enforcer`; allgemeine Resilienz-Praxis

---

## Modellwahl für Researcher: Sonnet statt Opus

- **Was:** Researcher benötigen für Web-Suche kein tiefes Reasoning — Opus ist hier Overkill und verbraucht dasselbe RPM-Budget bei viel höheren Token-Kosten und niedrigerer Geschwindigkeit.
- **Best Practice:**
  - Standard-Researcher → **Sonnet 4.x** (Geschwindigkeit + Kosteneffizienz)
  - Einfaches Lookup (z.B. "aktuelle Version von X?") → **Haiku 4.5** (noch günstiger, mehr ITPM)
  - Tiefe Analyse mit Reasoning → **Opus**, aber nur 1 Agent gleichzeitig (deutlich restriktivere Limits)
- **Quelle:** [https://code.claude.com/docs/en/sub-agents](https://code.claude.com/docs/en/sub-agents) (offiziell); [https://code.claude.com/docs/en/costs](https://code.claude.com/docs/en/costs) (offiziell)
- **Stand:** 2026-05-25

---

## Gute Researcher-Prompts: Was funktioniert

- **Was:** Die Qualität und Effizienz des Researcher-Outputs hängt stark von der Präzision des Prompts ab.
- **Best Practice für Researcher-Prompts:**
  - **Zentralfrage explizit nennen** ("ZENTRALE FRAGE: Warum...?")
  - **Quell-Hierarchie vorgeben** ("ZUERST offizielle Docs, DANN extern, klar labeln")
  - **Harte Limits einbauen** ("max 8 WebSearches, max 5 WebFetches, max 8 Min, max 100 Zeilen Antwort")
  - **Output-Format vorgeben** (Dateiformat, Abschnittstruktur, Quellenangaben mit URL)
  - **Fallback explizit definieren** ("Wenn nicht belegbar, schreib das ehrlich")
  - **Keine Halluzinationen** ("Erfinde nichts; nicht offiziell bestätigt → extern/unbestaetigt")
  - **Turn-Budget-Tracking** ("Turn 15 von 18 → sofort Zusammenfassung")
- **Anti-Pattern:** "Recherchiere alles über X" → unkontrollierte Tool-Schleifen
- **Quelle:** Interne Best Practice aus Researcher-Einsatz seit 2026-03-28; Robustness Protocol in `agent-and-researcher-rules.md`

---

## Vergleich: Web-Researcher vs. lokale Agenten

| Faktor | Web-Researcher | Lokaler Agent (z.B. Übersetzer) |
|--------|---------------|--------------------------------|
| API-Calls pro Turn | 1 (MessageAPI) | 1 (MessageAPI) |
| Tool-Calls pro Turn | 1–3 (WebSearch + WebFetch) | 1–2 (Read/Write lokal) |
| Turns pro Agent | 15–30 | 3–8 |
| RPM-Verbrauch (5 Agents, 5 Min) | 75–150+ Requests | 15–40 Requests |
| Hauptengpass | **RPM** | Keiner |
| 429-Risiko | Hoch bei >3–5 Agents parallel | Sehr gering |
| Lösung | Staggering + max 5 Agents | Keine Maßnahmen nötig |

**Kernaussage:** Researcher erzeugen durch WebSearch + WebFetch pro Turn viele schnelle API-Calls. Bei 5 parallelen Agents à 20 Turns in 5 Minuten = **100 RPM** — bei Tier-1-Limit von 50 RPM sofort ein Problem. Zusätzlich feuern Anthropics Acceleration Limits bei plötzlichem Nutzungsanstieg selbst unterhalb des Tier-Limits.

---

## Quellen-Übersicht

| Quelle | Typ | URL |
|--------|-----|-----|
| Anthropic API Rate Limits | offiziell | [https://platform.claude.com/docs/en/api/rate-limits](https://platform.claude.com/docs/en/api/rate-limits) |
| Claude Code Costs & Teams | offiziell | [https://code.claude.com/docs/en/costs](https://code.claude.com/docs/en/costs) |
| Claude Code Sub-Agents | offiziell | [https://code.claude.com/docs/en/sub-agents](https://code.claude.com/docs/en/sub-agents) |
| GitHub Issue #46037 (Parallel Sessions) | extern | [https://github.com/anthropics/claude-code/issues/46037](https://github.com/anthropics/claude-code/issues/46037) |
| GitHub Issue #42947 (/batch 429) | extern | [https://github.com/anthropics/claude-code/issues/42947](https://github.com/anthropics/claude-code/issues/42947) |
| Rate Limits Help Center | offiziell | [https://support.claude.com/en/articles/8114527](https://support.claude.com/en/articles/8114527) |
| Approach to Rate Limits (Anthropic) | offiziell | [https://support.anthropic.com/en/articles/8243635](https://support.anthropic.com/en/articles/8243635) |

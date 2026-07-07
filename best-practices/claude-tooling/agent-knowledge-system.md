# Agenten-Wissens-/Best-Practices-/Lern-System — Best Practices

> **Zweite Seite der Medaille zum Bug-Almanach** `~/proggs/bugs/claude-tooling/agent-knowledge-system.md`:
> dort steht *was schiefgeht* (Fallen), hier *wie man so ein System von vornherein RICHTIG baut und
> verbessert*. Gilt fuer Arbeit an: Bug-Almanach/Best-Practices-Struktur, ihren Ausloesern/Hooks
> (index/guard/auto-writer/subagent-context), den Lern-Datenbanken, und beim Umsetzen/Verbessern der
> drei Direktiven.
>
> **Stand:** recherchiert **2026-06-15** (5-Agenten-System-Audit + Web-Recherche). Versions-Anker:
> Claude Code v2.1.159; Agent Skills Open Standard (Anthropic, Dez 2025). Jeder Eintrag traegt Quelle;
> `offiziell` = Anthropic/Tool-Hersteller-Doku, `extern` = Forschung/Community.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Wissen dem Agenten zur richtigen Zeit geben | Progressive Disclosure: Tier-1 description (immer), Tier-2 Volltext (bei Bedarf), Tier-3 Details (on-demand) = euer Digest-Modell | §1 |
| 2 | Wie wird Wissen ausgeloest? | Doppel-Trigger: deterministischer Datei-Trigger + semantischer "Agent-Requested"-Trigger (description-basiert) | §2 |
| 3 | Wissen aktivieren (Hooks) | Nested Schema, beide Speicherorte spiegeln, Defense-in-Depth (mehrere Schichten + Drift-Detektor) | §3 |
| 4 | Wissens-Eintraege governen | Pro Eintrag: `confidence` + `last_verified` + `version_anchor`; veraltete sichtbar markieren | §4 |
| 5 | Aus Erfahrung lernen | Nur ECHTE Signale speichern (nie Platzhalter); Eval-getriebene Pflege; Rueckschreib-Pflicht | §5 |
| 6 | Self-Tests/Wartung | 0 Fehlalarme (sonst wertlos), gebuendelt, im SessionStart/PreCommit | §6 |
| 7 | Lern-Kreislauf | Jeder Bug → Almanach + Fall-DB, AUCH Harness-Bugs; geschlossener Loop | §7 |
| 8 | Langfristig | Graph-Memory (Bug↔RootCause↔Fix↔Version) statt flacher Liste | §8 |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei) | Failure-Mode im Almanach `bugs/claude-tooling/agent-knowledge-system.md` |
|---|---|
| §1 Progressive Disclosure | §7 unstrukturierte Trigger-Metadaten |
| §2 Doppel-Trigger (Datei + semantisch) | §5 Datei-Trigger verdeckt/lueckenhaft, §6 statisches RAG |
| §3 Aktivierung & Defense-in-Depth | §1 flaches Hook-Schema, §2 Tool-Drift |
| §4 Memory-Governance | §8 Veraltung/Staleness, §6 statisches RAG |
| §5 Lernschleife mit echten Signalen | §3 Lern-DB auf Platzhaltern, §10 Whiteboard-Spam |
| §6 Vertrauenswuerdige Self-Tests | §4 Wartungstool-Fehlalarme |
| §7 Geschlossener Lern-Kreislauf | §9 Harness-Bugs versickern |
| §8 Graph-Memory | (strategisch, kein akuter Failure-Mode) |

---

## §1 Progressive Disclosure — Wissen in 3 Tiers ausliefern
- **Regel:** Strukturiere jedes Wissens-Artefakt so, dass nur das Noetigste immer im Kontext ist und der Rest bei Bedarf nachgeladen wird. Das ist exakt das bestehende **Digest-Modell** (Stufe A Kurzcheck / Stufe B Volltext / Stufe C Hochrisiko) — Agent Skills formalisieren dasselbe nativ:
  - **Tier 1** (immer geladen, ~30-50 Tokens): `name` + `description`/Kurzcheck. Reicht zur Entscheidung "ist das relevant?".
  - **Tier 2** (bei Relevanz): der Volltext (`SKILL.md` bzw. Almanach-Body).
  - **Tier 3** (on-demand): referenzierte Detail-Dateien.
- **Warum:** Verlustfrei (alles bleibt per Pfad erreichbar) + RPM-/Token-schonend + context-rot-arm. Deckt sich mit dem Lossless-Prinzip.
- **Uebertragung:** Almanach-Kurzcheck = Tier-1-description; Volltext = Tier 2. Pruefen, ob Almanache/BP als **offizielle Agent Skills** verpackt werden koennten — dann laeuft Discovery nativ (auch in Subagents/Plugins) statt per selbstgebautem Hook. `offiziell` — Anthropic: https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills, API-Docs: https://platform.claude.com/docs/en/agents-and-tools/agent-skills/overview (Stand Dez 2025).

## §2 Doppel-Trigger — Datei-Muster UND semantisch
- **Regel:** Wissen ueber ZWEI unabhaengige Schichten auffindbar machen:
  1. **Datei-/Inhalts-Trigger** (deterministisch, wie `bug-almanac-guard`): greift zuverlaessig bei bekannten Mustern. Spezifische Signale IMMER vor breiten in der Kaskade (sonst Verdeckung, Almanach §5).
  2. **Semantischer "Agent-Requested"-Trigger** (Cursor-Muster): der Agent entscheidet anhand der `description` selbst, ob ein Almanach relevant ist — faengt **Konzept-Arbeit** und **neue Dateien**, wo kein Glob matcht.
- **Cursor 4 Rule-Typen** (uebertragbar): Always / Auto-Attach (glob) / **Agent-Requested (description-only, semantisch)** / Manual. Das bestehende System ist fast nur Auto-Attach.
- **Retrieval als dritte Auffindungs-Schicht** (NICHT als Ersatz): semantischer Index ueber `bugs/*.md` + `best-practices/*.md` (`code-search` MCP existiert bereits) — findet passende Eintraege per Embedding bei Symptom/Konzept. Aber: statisches RAG allein reicht nicht (Almanach §6) → nur ergaenzend zu kuratierten, versionsverankerten Eintraegen.
- `offiziell` Cursor: https://docs.cursor.com/en/context/rules · `extern` Windsurf-Indexing-Muster: https://www.builder.io/blog/cursor-vs-windsurf-vs-github-copilot

## §3 Aktivierung & Defense-in-Depth
- **Regel:** Die Aktivierungs-Hooks sind so wichtig wie das Wissen selbst — sie muessen wasserdicht sein:
  - **Nested JSON-Schema** `{hookSpecificOutput:{hookEventName,additionalContext}}` (+ PS `-Depth 5`); flaches Schema = stille Nicht-Injektion (Almanach §1).
  - **Beide Speicherorte spiegeln** (aktiv `~/.claude/` ↔ Repo `claude-code-setup/`), in BEIDE Richtungen — sonst Monats-Drift (Almanach §2).
  - **Mehrere Schichten** (Praesenz: index · Erzwingung: guard · Fehler-Bruecke: auto-writer · Subagent-Injektion: subagent-context · Verhaltensregel) — faellt eine aus, fangen andere.
  - **Drift-Detektor**: read-only SessionStart-Check (sha256 aktiv↔repo aller Hook-Paare), warnt bei Abweichung. EOL/BOM per `.gitattributes` normalisieren, damit nur echte Drifts melden.
  - **Resilienz** (claude-hooks.md): `exit 0` am Ende, Input-Guard, `pwsh` statt `powershell.exe`, BOM-frei.
- `offiziell` claude-hooks.md (lokaler Almanach) §2.1/§12; eigener Audit 2026-06-15.

## §4 Memory-Governance — Confidence, Temporal-/Staleness-Validierung
- **Regel:** Rohe Erfahrung wird nicht 1:1 gespeichert, sondern "governt", bevor sie agent-tauglich ist:
  - **Quality-Assessment** vor Speicherung (taugt der Eintrag als wiederverwendbares Wissen?).
  - **Pro Eintrag**: `confidence` + `last_verified` + `version_anchor` (wortgleiches Feld, maschinenlesbar).
  - **Temporal-Validation**: veraltete Eintraege beim Lesen sichtbar markieren ("Stand X, evtl. ueberholt").
  - **Relevance-Threshold + Safety-Filter**: Low-Confidence/widerspruechliche Eintraege filtern.
- **Staleness-Automatik:** periodischer Abgleich `version_anchor` (z.B. Room 2.7.0, Coil 3.0.4) ↔ live ermittelte Version (`gradlew dependencies`) → bei Drift automatisch Re-Check-Flag + Researcher-Schwarm. Kern-/meistgenutztes Wissen zuerst (Risiko-Asymmetrie, Almanach §8).
- `extern` MemGovern (arXiv 2601.06789): https://arxiv.org/pdf/2601.06789 (Stand Jan 2026).

## §5 Lernschleife mit ECHTEN Signalen
- **Regel:** Eine Lern-DB ist nur so viel wert wie die Signale, die sie speichert.
  - **Nie Platzhalter** (`success_score:3`, `error_count:0`, leere `tool_sequence`) — das taeuscht Lernen vor, das nicht stattfindet (Almanach §3). Lieber eine LEERE DB als eine irrefuehrend gefuellte.
  - Echte Felder befuellen: reale `task_description`, gewaehlte `strategy`, tatsaechliche `tool_sequence`, ehrlicher `success_score`/`error_count` → daraus erst werden `utility_score` und `near_miss` aussagekraeftig.
  - **Eval-getriebene Pflege** (Anthropic): Agent auf repraesentativen Tasks laufen lassen, Capability-Gaps finden, Wissen inkrementell ergaenzen, und "ask Claude to capture its successful approaches and common mistakes into reusable context" — formalisiert den Bug→Almanach-Loop als wiederkehrende Schleife statt nur reaktiv.
  - **Signal-Hygiene**: Auto-Log deduplizieren/raten-limitieren (kein Spam, Almanach §10); wertvolle Muster (Pheromon/bewaehrte Loesungen) per Postcondition aktiv zurueckschreiben.
- `offiziell` Anthropic (Eval/Capture): https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills

## §6 Vertrauenswuerdige Self-Tests
- **Regel:** Jedes Wartungs-/Health-Skript muss im Normalzustand **0 Fehlalarme** liefern — ein Tool mit Dauer-`[DRIFT]` wird ignoriert und verliert seine Schutzwirkung (Almanach §4).
  - Pruef-Pattern an das real verwendete Format angleichen (oder das Format vereinheitlichen).
  - **Buendeln** (`check-coupling` + `check-guard-coverage` + Stand-Verfall + Anker-Konsistenz + Hook-Drift) in EIN `bugs/health.py`, periodisch + in PreCommit/SessionStart, damit Drift sofort beim Schreiben auffaellt.
  - Bestehende Self-Tests: `check-coupling.py` (Almanach↔BP-Kopplung), `check-guard-coverage.py` (jeder Almanach erzwungen/Allowlist).
- eigener Audit 2026-06-15.

## §7 Geschlossener Lern-Kreislauf — auch fuer Harness-Bugs
- **Regel:** Jeder erlebte Bug wandert in BEIDES — den passenden Almanach (proaktiv, Lehrbuch) UND `bug-cases.jsonl` (reaktiv, Posteingang). Das gilt fuer Projekt-Bugs UND fuer **Harness-Bugs** (Hooks/Skills/Config) gleichermaszen — letztere werden sonst uebersprungen (Almanach §9).
  - `auto_captured`-Faelle aus `bug-cases.jsonl` nach Root-Cause-Pflege in den Almanach **befoerdern** (Posteingang → Lehrbuch); optional `promoted:true/false`-Flag macht den Fluss messbar.
- Direktive #3 (`resilient-bugfixing.md`) Schritt 7; eigener Audit 2026-06-15.

## §8 Graph-Memory (strategisch, mittel-/langfristig)
- **Regel:** Der Frontier-Trend 2026 ist der Wechsel vom passiven Log (flache JSONL) zu einem topologischen Erfahrungs-Modell: Bugs ↔ Root-Causes ↔ Fixes ↔ Versionen verknuepft. Ein Fix verlinkt auf verwandte Fehlerklassen — deckt sich exakt mit Direktive #3 ("verwandte Fehlerquellen suchen").
- **Uebertragung:** `bug-cases.jsonl` + Almanache spaeter als Graph modellieren; nicht akut, aber als Richtung vormerken.
- `extern` Graph-based Agent Memory (arXiv 2602.05665): https://arxiv.org/html/2602.05665v1 (Stand Feb 2026).

---

## Quellen-Rangordnung
`offiziell` (Anthropic Agent-Skills/Hooks-Doku, Cursor-Docs) = Grundwahrheit fuer Mechanik;
`extern` (arXiv MemGovern/Memory-Survey/Graph-Memory, Builder.io-Vergleich) = Forschungs-/Community-
Richtung, klar gelabelt, ueberstimmt nie das Offizielle. Stand/Version pro Eintrag.

## Pflicht-Checkliste vor Arbeit am Wissens-/Lern-System
- [ ] Almanach (Fallen) + diese BP gelesen (Bezugs-Tabelle)?
- [ ] Progressive Disclosure eingehalten (Tier-1 description schlank)?
- [ ] Doppel-Trigger bedacht (Datei + semantisch), Coverage-Check?
- [ ] Hook: nested Schema, beide Speicherorte, Drift-Detektor, exit 0?
- [ ] Lern-DB: echte Signale statt Platzhalter? Self-Tests 0 Fehlalarme?
- [ ] Versions-Anker + Staleness gepflegt? Kreislauf geschlossen (auch Harness)?

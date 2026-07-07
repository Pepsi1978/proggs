# Bekannte Bugs & Fallen: Agenten-Wissens-/Best-Practices-/Lern-System (Harness-Selbstverbesserung)

> **PFLICHT-LESEN vor Arbeit am eigenen Bug-Almanach-/Best-Practices-/Lern-System** — also an
> seinen Ausloesern/Hooks (`bug-almanac-index`, `bug-almanac-guard`, `bug-case-auto-writer`,
> `subagent-context`), an den Lern-Datenbanken (`experience-store.jsonl`, `trajectories.jsonl`,
> `session-scores.jsonl`, Pheromon-Tabelle), an der Almanach-/BP-Struktur selbst, oder beim
> Umsetzen/Verbessern der drei Direktiven. Dieser Almanach sammelt die FALLEN; die richtige
> Bauweise steht in der Best-Practices-Datei (zweite Seite der Medaille, siehe Bezugs-Tabelle).
>
> **Stand:** zuletzt recherchiert/auditiert am **2026-06-15**. Quellen: 5-Agenten-System-Audit
> (Almanach/BP/Aktivierung/Intelligenz) + Web-Recherche 2026. Anker: dieses Repo-System
> (`bugs/SYSTEM.md` Digest-Modell, `known-bugs-before-coding.md`, `research-persistence.md`),
> Claude Code v2.1.159, Agent Skills (Open Standard Dez 2025).
>
> **Querschnitts-/Konzept-Bereich:** wird NICHT vom `bug-almanac-guard` ueber ein Datei-Pattern
> erzwungen (wie `apis/`, `agents/`) — gefunden ueber Index + Stichworte. In der Allowlist von
> `check-guard-coverage.py` als bewusst-nicht-datei-erzwungen gefuehrt.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Hook injiziert Kontext (SessionStart/SubagentStart/Pre/PostToolUse) | Nested `hookSpecificOutput.{hookEventName,additionalContext}` + PS `-Depth 5`; flaches Schema wird STILL ignoriert | §1 |
| 2 | Hook in `~/.claude/hooks/` geaendert | Sofort in Repo-Spiegelung kopieren UND umgekehrt; Drift bleibt sonst MONATE unbemerkt (aktiv≠repo) | §2 |
| 3 | Lern-DB (experience/trajectories/scores) befuellen | NIE Platzhalter (`success_score:3`, leere `tool_sequence`) schreiben — lieber leer als irrefuehrend; sonst sind alle Metriken Papier | §3 |
| 4 | Wartungs-/Self-Test-Skript bauen | Muss vertrauenswuerdig sein: ein Tool mit Dauer-Fehlalarmen (Format-Drift) wird ignoriert und schuetzt nicht mehr | §4 |
| 5 | Neuer Almanach/Wissens-Bereich | Datei-Trigger allein verdeckt Spezial-Almanache hinter dem Sprach-Almanach + faengt Konzept-Arbeit nicht; zweite (semantische) Trigger-Schicht noetig | §5 |
| 6 | "Einfach RAG/Vector-Store drueber" | Statisches RAG reicht fuer Code-Wissen NICHT (kleine Details kippen das Ergebnis) — aktive Governance/Kuration noetig | §6 |
| 7 | Trigger-Metadaten (description/Stichworte) | Muessen strukturiert/maschinenlesbar sein; unstrukturierte Metadaten werden ignoriert (Cursor: `.md` ohne frontmatter greift nicht) | §7 |
| 8 | Wissen veraltet | Kern-/meistgenutztes Wissen veraltet am schnellsten (Risiko-Asymmetrie); ohne Staleness-Markierung wird blind vertraut | §8 |
| 9 | Bug erlebt | Sowohl Almanach ALS AUCH `bug-cases.jsonl` aktualisieren — auch fuer HARNESS-Bugs (versickern sonst) | §9 |
| 10 | Whiteboard/MEMORY.md schreiben | Auto-Log-Spam (Speicher/Effort) raten-limitieren/deduplizieren, sonst ertrinkt das echte Signal | §10 |
| 11 | Web-Recherche per `WebFetch` auf `github.com` | github.com ist fuer WebFetch blockiert (verlangt `gh`-CLI) → auf `npmjs.com`/`sourcepulse.org`/offizielle Doku/`WebSearch` ausweichen, fuer Repo-Daten `gh`-CLI | §11 |
| 12 | Researcher-Schwarm (parallele Web-Recherche) | Zu viele gleichzeitige Researcher/Fetches → Server-Rate-Limit (429). ~8 Fetches/Researcher, 5-6 gleichzeitig, gestaffelt (Continuous-Spawning); Findings NIE kappen, nur Rate drosseln | §12 |

---

## 🔗 Bezugs-Tabelle: Bug-Almanach ↔ Best-Practice

> Zweite Seite der Medaille: `best-practices/claude-tooling/agent-knowledge-system.md`
> sagt, wie man so ein System von vornherein RICHTIG baut.

| Bug-Abschnitt (diese Datei) | Best-Practice-Gegenpart |
|---|---|
| §1 Hook-Schema / stille Nicht-Injektion | §3 Aktivierung & Defense-in-Depth |
| §2 Tool-Drift aktiv↔repo | §3 Aktivierung (Drift-Detektor) |
| §3 Lern-DB auf Platzhaltern | §5 Lernschleife mit echten Signalen |
| §4 Wartungstool-Fehlalarme | §6 Vertrauenswuerdige Self-Tests |
| §5 Datei-Trigger verdeckt/lueckenhaft | §2 Doppel-Trigger (Datei + semantisch) |
| §6 statisches RAG | §2 Retrieval + §4 Memory-Governance |
| §7 unstrukturierte Trigger-Metadaten | §1 Progressive Disclosure (strukturierte description) |
| §8 Veraltung / Staleness | §4 Memory-Governance (last_verified/version_anchor) |
| §9 Kreislauf-Luecke Harness-Bugs | §7 Geschlossener Lern-Kreislauf |
| §10 Whiteboard-Spam | §5 Signal-Hygiene |

---

## 1. Flaches Hook-JSON-Schema → STILLE Nicht-Injektion ⭐ KRITISCH
**Symptom:** Ein Kontext-Injektions-Hook (z.B. `subagent-context`) "laeuft", aber der Inhalt
landet NIE im Agenten/Subagenten — keine Fehlermeldung. Subagenten erben das Bug-Almanach-/
Such-Reflex-/Crash-Schutz-Wissen faktisch nicht.
**Ursache:** Der Hook gibt `{"additionalContext":"..."}` FLACH aus statt verschachtelt. Claude
Code akzeptiert nur `{"hookSpecificOutput":{"hookEventName":"<Event>","additionalContext":"..."}}`;
fehlt die Verschachtelung (oder `hookEventName`), wird der Output stumm verworfen. PowerShell
verschaerft das: `ConvertTo-Json` serialisiert per Default nur 2 Ebenen → `-Depth 5` noetig.
**Versionen:** per Design (claude-hooks.md §2.1/§2.2), verifiziert lokal.
**FIX:** Immer nested Schema + `hookEventName` + (PS) `-Depth 5`. Real-Vorfall 2026-06-15:
aktiver `subagent-context.ps1`/`.sh` nutzte das flache Schema → ALLE Subagenten ohne Almanach-
Bewusstsein. Beheben + per Self-Test (§4) gegen Wiederauftreten absichern.
**Quelle:** eigener Audit 2026-06-15; `bugs/claude-tooling/claude-hooks.md` §2.1.

## 2. Tool-Drift aktiv↔repo bleibt MONATE unbemerkt ⭐
**Symptom:** Die Repo-Version eines Hooks/Skills ist korrekt und aktuell, aber die AKTIVE Datei
in `~/.claude/` ist eine alte Fassung (oder umgekehrt). Verhalten weicht ab, niemand merkt es.
**Ursache:** `~/.claude/hooks/` (aktiv) und `claude-code-setup/hooks/` (Repo-Spiegelung) werden
getrennt gepflegt; eine Aenderung an nur EINER Seite divergiert still. Es gibt keinen
automatischen Gleichheits-Check → Drift akkumuliert (real: `subagent-context` 1 Monat alt aktiv).
**Versionen:** per Design (Zwei-Speicherorte-Architektur).
**FIX:** Bei JEDER Hook-Aenderung BEIDE Seiten spiegeln (Cross-Platform-/3-Dateien-Regel, gilt in
BEIDE Richtungen — auch repo→aktiv). Plus ein read-only SessionStart-Drift-Detektor
(sha256 aktiv↔repo fuer alle Hook-Paare), der bei Abweichung warnt. Kosmetischen EOL-/BOM-Drift
per `.gitattributes` (`*.ps1 text eol=crlf`, `*.sh text eol=lf`) eliminieren, damit der Hash-Check
nur ECHTE Inhalts-Drifts meldet.
**Quelle:** eigener Audit 2026-06-15.

## 3. Lern-Datenbank auf Platzhaltern = Papier-Metriken ⭐ HAEUFIG
**Symptom:** `experience-store.jsonl`/`trajectories.jsonl` wachsen, aber jeder Eintrag ist
`auto-captured`/`session-auto-logged` mit `success_score:3`, `error_count:0`, leerer
`tool_sequence`. `near_miss` ist NIE `true`, `utility_score` eingefroren (0.6/0.8).
`session-scores.jsonl` faellt ganz aus (eingefroren). Trend-/IQ-/CBR-Analysen rechnen auf totem
Datensatz → KEIN Compound-Intelligence-Effekt aus dieser Schicht.
**Ursache:** Die Auto-Log-Pipeline schreibt Defaults, weil ihre Quelle (`task_description`/
`strategy`/`tool_sequence`) nie real befuellt wird; der Scorer-Hook feuert nicht mehr (Trigger
entfernt/kaputt). Die Mechanik (Near-Miss-Retention, SICA-utility) ist sauber definiert, bekommt
aber keine echten Signale.
**Versionen:** lokaler Vorfall (scores eingefroren seit 2026-04-12, festgestellt 2026-06-15).
**FIX:** Entweder echte Signale schreiben (reale task_description/strategy/tool_sequence/Score)
ODER die Auto-Log-Pipeline ehrlich abschalten — eine leere DB ist besser als eine irrefuehrend
gefuellte (sie taeuscht Lernen vor, das nicht stattfindet). Scorer-Trigger reparieren, sonst ist
jede darauf aufbauende Metrik blind.
**Quelle:** eigener Audit 2026-06-15 (Intelligenz-Dimension).

## 4. Wartungs-/Self-Test-Skript mit Dauer-Fehlalarmen wird wertlos ⭐
**Symptom:** Ein Health-/Kopplungs-Check (`check-coupling.py`) meldet dauerhaft viele `[DRIFT]`/
Fehlerzeilen (real: 18), obwohl die geprueften Dinge in Ordnung sind (Format-Drift: Tabelle
existiert, aber nicht im vom Script erwarteten Muster). Folge: Man gewoehnt sich an die roten
Zeilen und ignoriert das Tool — es schuetzt nicht mehr.
**Ursache:** Das Pruef-Pattern und das real verwendete Format laufen auseinander; das Tool wird
nicht an die Realitaet angeglichen.
**Versionen:** lokaler Vorfall 2026-06-15.
**FIX:** Self-Tests muessen 0 Fehlalarme haben, sonst verlieren sie ihre Schutzwirkung. Entweder
das Pruef-Pattern an das real verwendete Format angleichen ODER das Format vereinheitlichen.
Self-Tests in einen Wartungslauf buendeln und in einen PreCommit/SessionStart haengen, damit
Format-Drift sofort beim Schreiben auffaellt statt erst im Audit.
**Quelle:** eigener Audit 2026-06-15 (Best-Practices-Dimension).

## 5. Reiner Datei-Trigger: verdeckt Spezial-Almanache + verpasst Konzept-Arbeit ⭐
**Symptom:** (a) Ein spezifischer Almanach existiert, wird aber nie ausgeloest, weil sein
Datei-Muster vom uebergeordneten Sprach-Almanach abgefangen wird (real: `room` lief unter
`android-platform`; `voice-assistant`/`workmanager` unter `service.kt`/`worker.kt`). (b) Bei
Konzept-Arbeit oder einer brandneuen Datei greift gar kein Muster → der passende Almanach wird
nie vorgeschlagen.
**Ursache:** Erkennung nur ueber Dateipfad/Endung + Inhalts-Probe. Das ist deterministisch, aber
blind fuer Faelle, die das Muster nicht trifft, und anfaellig fuer Verdeckung durch breitere Muster.
**Versionen:** per Design des `bug-almanac-guard` (v1).
**FIX:** Spezifische Signale VOR breiten in der Erkennungs-Kaskade (funktionserhaltend). Zusaetzlich
eine ZWEITE, semantische Trigger-Schicht: der Agent entscheidet anhand der Almanach-`description`
selbst, ob ein Almanach relevant ist (Cursor "Agent-Requested"-Muster). Ein `check-guard-coverage.py`
faengt ungemappte Almanache ab.
**Quelle:** eigener Audit + Cursor Rules Docs (https://docs.cursor.com/en/context/rules).

## 6. Statisches RAG / generischer Vector-Store reicht fuer Code-Wissen NICHT
**Symptom:** Ein naiver "semantischer Suche ueber alle Bug-/BP-Dateien"-Ansatz liefert
plausible, aber falsche/unvollstaendige Treffer; kleine Implementierungsdetails (Version,
Flag, Reihenfolge) kippen das Ergebnis.
**Ursache:** Code-/Bug-Wissen ist detail-sensitiv; ein passiver Vector-Store ohne aktive
Governance/Normalisierung degradiert. RAG ist Ergaenzung, kein Ersatz fuer kuratierte,
versionsverankerte Eintraege.
**Versionen:** konzeptionell (2026er Forschung).
**FIX:** Semantisches Retrieval NUR als zusaetzliche Trigger-/Auffindungs-Schicht ueber die
KURATIERTEN Almanache/BP (nicht als Ersatz). Aktive Kuration + Versions-Anker + Confidence
beibehalten.
**Quelle:** Feedback-Normalized Developer Memory (https://arxiv.org/html/2605.01567);
Memory-Survey (https://arxiv.org/html/2603.07670v1).

## 7. Unstrukturierte Trigger-Metadaten werden ignoriert
**Symptom:** Ein Wissens-/Regel-Eintrag wird nie ausgeloest, weil seine Trigger-Information nur
als Fliesstext vorliegt.
**Ursache:** Auffindungs-Mechanismen brauchen strukturierte Metadaten (name + description +
glob/Trigger). Beispiel-Analogie: Cursor ignoriert `.md` in `.cursor/rules` ohne frontmatter —
nur `.mdc` mit Metadaten greift.
**Versionen:** Cursor 2026 (uebertragbares Muster).
**FIX:** Jeder Almanach/jede BP traegt eine klare, kurze, maschinen-nutzbare `description`/
Stichwort-Zeile (Tier-1 im Progressive-Disclosure-Sinn), die fuer semantisches/Index-Matching
taugt. Stichwort-Trigger im `bugs/README.md` pflegen.
**Quelle:** https://docs.cursor.com/en/context/rules.

## 8. Veraltung — Kern-Wissen veraltet am schnellsten, ohne Markierung blind vertraut ⭐
**Symptom:** Die meistgenutzten Kern-Almanache (real: `claude-hooks`, `kotlin`, `jetpack-compose`,
`gradle`, `firebase-billing`) tragen das aelteste Stand-Datum, waehrend Nischen-Almanache frisch
sind. Das hoechste Bug-Volumen liegt damit auf dem aeltesten Wissen — und ohne sichtbare
Veraltungs-Markierung wird ihm blind vertraut.
**Ursache:** Re-Recherche passiert anlassbezogen, nicht nach Nutzung/Alter priorisiert. Versions-
Anker uneinheitlich formatiert (mal `Anker:`, mal `Versions-Anker:`, mal nur inline) → nicht
maschinell pruefbar.
**Versionen:** lokaler Vorfall 2026-06-15.
**FIX:** Versions-Anker als Pflichtfeld vereinheitlichen (wortgleich, direkt unter `Stand:`).
Staleness-Skript: Almanache mit `Stand:` > N Tagen als `[VERALTET]` melden, priorisiert nach
Bug-Volumen. Automatischer Abgleich `version_anchor` ↔ live ermittelte Version (z.B.
`gradlew dependencies`) → bei Drift Re-Check-Flag.
**Quelle:** eigener Audit; MemGovern Temporal-Validation (https://arxiv.org/pdf/2601.06789).

## 9. Kreislauf-Luecke: HARNESS-Bugs versickern ohne Almanach-Eintrag
**Symptom:** Projekt-/Plattform-Bugs landen zuverlaessig im passenden Almanach (real 6/8), aber
reine Harness-Bugs (Hook-Exit-Code-Klassen, Tool-Redundanzen) kommen nur in `bug-cases.jsonl`,
nicht in den `claude-tooling/`-Almanach. Die eigene Werkzeugkiste lernt langsamer als die Projekte.
**Ursache:** Die Resilient-Bugfixing-Checkliste (Direktive #3, Schritt 7: "neuen Bug in Almanach")
wird fuer Projekt-Bugs konsequent, fuer Harness-Bugs aber oft uebersprungen.
**Versionen:** lokaler Vorfall 2026-06-15.
**FIX:** Direktive #3 (a)+(b)-Zwang (Almanach UND bug-cases) explizit auch fuer `claude-tooling/`-
Bugs durchsetzen. Beim Fixen eines Hook-/Skill-/Config-Bugs IMMER den passenden claude-tooling-
Almanach mit ergaenzen.
**Quelle:** eigener Audit 2026-06-15.

## 10. Whiteboard/MEMORY.md mit Auto-Log-Spam verstopft → Signal ertrinkt
**Symptom:** Die "Offene Fehler & Probleme"-Sektion ist hunderte Zeilen lang, fast alles
identische Auto-Eintraege ("Speicherplatz kritisch", "effortLevel zurueckgesetzt"). Echte offene
Bugs sind nicht mehr auffindbar. Pheromon-/Bewaehrte-Muster-Tabelle bleibt dagegen fast leer
(Agents schreiben nichts zurueck).
**Ursache:** Auto-Logger ohne Dedup/Rate-Limit; kein Rueckschreib-Zwang fuer wertvolle Muster.
**Versionen:** lokaler Vorfall 2026-06-15.
**FIX:** Auto-Log-Eintraege deduplizieren/raten-limitieren (gleiche Meldung max 1x/Tag). Wertvolle
Erkenntnisse (Pheromon, bewaehrte Loesungen) per Postcondition/Hook aktiv zurueckschreiben, sonst
bleibt die Lern-Tabelle leer und niemand liest sie.
**Quelle:** eigener Audit 2026-06-15.

---

## 11. `WebFetch` auf `github.com` wird blockiert (Repo-/Issue-/Release-Recherche)
**Symptom:** `WebFetch` auf eine `github.com`-URL (Repo-README, Issue, Releases) liefert keinen
Inhalt bzw. verlangt die `gh`-CLI; ein Researcher-Schwarm, der auf GitHub zielt, läuft ins Leere.
**Ursache:** github.com ist für das WebFetch-Tool gesperrt — GitHub-Inhalte sollen über die
`gh`-CLI/API geholt werden, nicht per Scrape.
**Versionen:** Claude Code, Stand 2026-06.
**FIX (funktionserhaltend):** Gar nicht erst auf github.com „nachbohren" (kostet nur Fetches).
Stattdessen alternative Quellen: `npmjs.com` (Paket-Metadaten/READMEs/`npm view`), `sourcepulse.org`
(Repo-Spiegel/Stats), die offizielle Doku-Domain des Projekts, oder `WebSearch`. Für echte
Repo-Daten die `gh`-CLI per Bash (`gh repo view`, `gh api …`). Researcher-Prompts entsprechend
instruieren (GitHub-Quellen über `gh`/Alternativen, nicht WebFetch).
**Belegt:** OpenCode-Plugin-Recherche 2026-06-19 — Researcher mussten von github.com auf
npmjs.com / sourcepulse.org / opencode.ai/docs / WebSearch ausweichen.

## 12. Researcher-Schwarm: zu viele parallele Fetches → Server-Rate-Limit (429)
**Symptom:** Bei einem großen Schwarm (z.B. 7 Researcher gleichzeitig × je ~12 Web-Fetches)
bricht ein Teil mit „temporarily limiting requests" / HTTP 429 ab; einzelne Researcher müssen neu
gestartet werden.
**Ursache:** Ein Burst zu vieler gleichzeitiger HTTP-Anfragen über alle Researcher hinweg
überschreitet das serverseitige Anfrage-Rate-Limit. Das ist UNABHÄNGIG vom 1M-Kontextfenster
(ein Anfrage-Raten-Problem, kein Kontext-Problem — daher hilft ein größeres Fenster NICHT).
**Versionen:** Claude Code, Stand 2026-06 (mehrfach erlebt, zuletzt 2026-06-19).
**FIX (funktionserhaltend):** Den Anfrage-Strom entzerren — ~8 Web-Fetches pro Researcher (nicht
12+), 5-6 Researcher gleichzeitig (nicht 7+), und per Continuous-Spawning gestaffelt nachziehen
statt als großer Burst. Bei 429: Retry mit exponential backoff (`retry-after` beachten). Findings
NIE an einem künstlichen Cap abschneiden — nur die Anfrage-RATE drosseln (deckt sich mit
`~/.claude/rules/agent-and-researcher-rules.md` §2).
**Belegt:** OpenCode-Plugin-Recherche 2026-06-19 — 7×~12 Fetches, 1 Researcher (notify) lief ins
Limit und musste neu.

---

## Fix-Status (Stand 2026-06-15)

| Falle | Status in DIESEM Repo |
|-------|----------------------|
| §5 Datei-Trigger-Verdeckung | TEILS GEFIXT — 13 Luecken geschlossen + `check-guard-coverage.py` (Commit #46777); semantische Zweit-Schicht offen |
| §1 flaches Hook-Schema (subagent-context) | OFFEN — Fix in "Welle 1" geplant (repo→aktiv spiegeln) |
| §2 Tool-Drift-Detektor | OFFEN — geplant (SessionStart sha256-Check) |
| §3 Lern-DB Platzhalter / §4 check-coupling-Fehlalarme / §8 Staleness / §9 Harness-Kreislauf / §10 Spam | OFFEN — "Welle 2/3" Backlog (siehe `bugs/SYSTEM-AUDIT-2026-06-15.md`) |
| §6/§7 (semantisches Retrieval, strukturierte Metadaten) | OFFEN — strategisch (Welle 3) |

---

## Pflicht-Checkliste vor Arbeit am Wissens-/Lern-System
- [ ] Diese Datei + die Best-Practices-Datei (Bezugs-Tabelle) gelesen?
- [ ] Hook geaendert? → nested Schema (§1) + BEIDE Speicherorte gespiegelt (§2) + Self-Test?
- [ ] Lern-DB angefasst? → echte Signale statt Platzhalter (§3), sonst leer lassen?
- [ ] Self-Test/Wartungs-Skript? → 0 Fehlalarme, sonst wertlos (§4)?
- [ ] Neuer Almanach/Bereich? → Datei-Trigger + semantische/Stichwort-Trigger (§5/§7), Coverage-Check?
- [ ] Versions-Anker + Stand gepflegt, Staleness bedacht (§8)?
- [ ] Bug erlebt? → Almanach UND bug-cases, AUCH fuer Harness (§9)?

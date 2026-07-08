# finale Plugin — Bug-Liste Run 2026-05-22 (BestJournalAndroid)

> Diese Datei wird WAEHREND des Plugin-Laufs gefuellt. Jeder Fehler, jede Reibung,
> jedes "das haette das Plugin selbst machen sollen"-Moment kommt hier rein.
> Nach Lauf-Ende konsolidieren in `ISSUES.md`.

Lauf: `/finale:run` mit App-Root `~/proggs/BestJournalAndroid`
Effort: xhigh
Startzeit: 2026-05-22 ~14:53

## STATUS-UEBERSICHT (Loop 1-4, 2026-05-22)

### Loop 1+2 — Bugs #1-#16 (Commit #929)

| Bug | Status | Wo umgesetzt |
|-----|--------|--------------|
| #1 Symlink-Auto-Repair | **APPLIED** | scripts/verify-skills.sh (Auto-Repair fuer alle Plattformen) + FIN-030 in orchestrator.md |
| #2 Plugin/skills/ Inkonsistenz | **APPLIED** | Plugin/skills/README.md erweitert (Symlink-Pattern + Auto-Repair + manuelle Wiederherstellung) |
| #3 skill-versions.json Mandatory Write | **APPLIED** | orchestrator.md Phase 0 Schritt 2b — sofort nach Phase 0 schreiben |
| #4 .gitignore in .android-shield | **APPLIED** | assets/android-shield-gitignore Template + FIN-031 in orchestrator.md |
| #5 Dynamic Worker Count | **APPLIED** | FIN-029 Direktive — Worker-Count = clamp(N, 4, 20) |
| #6 Roentgen-Skill-Scope-Split | **APPLIED** | ~/.claude/skills/app-roentgen/SKILL.md — optionaler `--scope=`-Parameter (11 Werte), volle Rueckwaertskompatibilitaet erhalten |
| #7 Roentgen-Schema-Mismatch | **APPLIED** | FIN-034 in orchestrator.md — Schema-Compat-Layer akzeptiert Variante A + B, normalisiert in internes Format |
| #8 Run-Status-Marker | **APPLIED** | FIN-035 in orchestrator.md Schritt 2c — `run-status.json` mit Heartbeat + Resume-Erkennung |
| #9 Token-Cap 145k | **APPLIED** | 100k → 145k an allen Stellen in orchestrator.md |
| #10 Worker-Schema-Strict | **APPLIED** | FIN-036 in orchestrator.md + `schemas/cross-lingual-findings.schema.json` + `schemas/phase2-applied.schema.json` + Auto-Mapping-Aliases |
| #11 Subagent-Selbstbeobachtung | **APPLIED** | FIN-037 in orchestrator.md — Pflicht-Block in jedem Subagent-Prompt + `plugin_bugs_observed`-Feld |
| #12 Cross-Lingual-Parallelisierung | **APPLIED** | FIN-023+029 — 15-20 Worker fuer Phase 1B-cross-lingual |
| #13 Worker-Halluzinations-Schutz | **APPLIED** | FIN-033 in orchestrator.md — Read-Verifikation Pflicht |
| #14 uebersetzung-Skill PFLICHT | **APPLIED** | FIN-032 in orchestrator.md — keine Ausnahmen |
| #15 Phase 2 Bulk-Parallel | **APPLIED** | Phase 2 Bulk-Mode + 15-20 fix-applier parallel |
| #16 FIN-029 Parallel-First | **APPLIED** | Zentrale Direktive direkt nach FIN-026 in orchestrator.md |

### Loop 4 — Bugs #17-#29 (Commit nach Frank-Resume 2026-05-22)

| Bug | Schwere | Status | Wo umgesetzt |
|-----|---------|--------|--------------|
| #17 Plugin-Cache veraltet (assets/schemas fehlen) | 🟧 Mittel | **APPLIED** | FIN-046 in orchestrator.md — Fallback auf ~/proggs/Umgebung/Plugins/finale/Plugin/ + manuelle Refresh-Procedure |
| #18 Recht-Report Stale-Finding-Check | 🟧 Mittel | **APPLIED** | FIN-038 Phase 1.5 in orchestrator.md — Grep/HEAD-Check, resolved-since-audit Status, openFindingsCount-Neuberechnung |
| #19 FIN-029 Worker-Count fuer Mini-Aufgaben | 🟨 Niedrig | **APPLIED** | FIN-039 in orchestrator.md — Units-Estimate-Heuristik (<100→3-6, <1000→5-12, sonst 15-20) |
| #20 Auto-Job-Plan-Generator Phase 3b | 🟨 Niedrig | **APPLIED** | FIN-038 Auto-Diff-Block — job-plan.json per Sprache vor Phase 3b |
| #21 Translation-Worker Pre-Check | 🟧 Mittel | **APPLIED** | FIN-040 Worker-Standard-Template — Pre-Check `grep -c name="{key}"` vor Insert |
| #22 rfind statt content.replace | 🟨 Niedrig | **APPLIED** | FIN-040 Worker-Standard-Template — rfind('</resources>') Pflicht |
| #23 Atomarer Schreibfehler (pl) | 🟨 Niedrig | **APPLIED** | FIN-045 Cross-Sprachen-Audit vor Phase 3b — vergleicht Key-Anzahl pro Sprache gegen DE-Referenz |
| #24 PYTHONIOENCODING=utf-8 Default | 🟧 Mittel | **APPLIED** | FIN-041 in orchestrator.md — ENV-SETUP-Pflichtblock in jedem Worker-Prompt |
| #25 Idempotenz-Guard nach Crash | 🟧 Mittel | **APPLIED** | FIN-040 Worker-Standard-Template — skipped_already_present-Feld im Output-JSON |
| #26 Worker D Autocompact-Thrashing | 🟥 Hoch | **APPLIED** | FIN-042 in orchestrator.md — Max 5 Sprachen pro Worker (3-4 bei dichten Schriften) + Token-Disziplin |
| #27 ISO-639-1 vs Android-Legacy Mapping | 🟨 Niedrig | **APPLIED** | FIN-043 in orchestrator.md — Mapping-Tabelle (id→in, he→iw, yi→ji, pt-BR→pt-rBR, zh-Hans→zh-rCN ...) |
| #28 Urdu-Wortstellung RTL-Bidi-Falle | 🟨 Niedrig | **APPLIED** | FIN-032 Punkt 6 (Verstaerkung) — Sprach-Fallen-Tabelle mit Skill-Datei-Verweisen pro Sprach-Familie |
| #29 Worker A franzoesische Apostrophen | 🟥 Hoch | **APPLIED** | FIN-044 in orchestrator.md — check_apostrophes.py nach jeder romanischen Sprache verbindlich |

**ALLE 29 Bugs APPLIED** (Stand 2026-05-22 abends). Plugin-Verbesserungen Loop 1-4 abgeschlossen.

**Neue Direktiven dieses Hardening-Laufs (Loop 1-4):**
- FIN-029 Parallel-First (zentral)
- FIN-030 Auto-Repair Phase 0
- FIN-031 .android-shield/.gitignore Template
- FIN-032 uebersetzung-Skill PFLICHT fuer i18n-Fixes (+ Punkt 6 mit Sprach-Fallen-Tabelle in Loop 4)
- FIN-033 Worker-Halluzinations-Schutz
- FIN-034 Roentgen-Schema-Compat-Layer
- FIN-035 run-status.json Heartbeat
- FIN-036 Post-Worker-Schema-Validation
- FIN-037 Subagent-Selbstbeobachtungs-Pflicht
- **FIN-038 Phase 1.5 Stale-Finding-Check + Auto-Job-Plan-Generator** (Loop 4 — BUG #18+20)
- **FIN-039 Worker-Count-Heuristik mit Units-Dimension** (Loop 4 — BUG #19)
- **FIN-040 Worker-Standard-Template (Pre-Check + rfind + Idempotenz)** (Loop 4 — BUG #21+22+25)
- **FIN-041 PYTHONIOENCODING=utf-8 als Default** (Loop 4 — BUG #24)
- **FIN-042 Max 5 Sprachen pro Translation-Worker** (Loop 4 — BUG #26)
- **FIN-043 ISO-639-1 vs Android-Legacy-Code-Mapping** (Loop 4 — BUG #27)
- **FIN-044 Apostroph-Validator verbindlich** (Loop 4 — BUG #29)
- **FIN-045 Cross-Sprachen-Audit (atomarer Schreibfehler)** (Loop 4 — BUG #23)
- **FIN-046 Plugin-Cache-Refresh-Procedure + Fallback** (Loop 4 — BUG #17)
- **FIN-047 Auto-Write RUN-ISSUES nach jedem Lauf** (Loop 5 2026-05-22 — Vorschlag 5 aus #934)

**Loop 5 Folge-Aktionen (Frank-Trigger 2026-05-22 abends, alle 5 Vorschlaege aus #934):**
1. Plugin-Cache aktualisiert: assets/ + schemas/ aus Quell-Repo kopiert → `~/.claude/plugins/cache/local/finale/0.1.0/`
2. Worker-Template extrahiert: `Plugin/agents/templates/translation-worker.md` (10 Abschnitte mit Checklist) — FIN-040+026 verweisen jetzt darauf statt Boilerplate inline
3. Pre-Commit-Hook installiert: `~/proggs/.git/hooks/pre-commit` ruft check_apostrophes.py auf alle gestageten values-XX/strings.xml in romanischen Sprachen + tr auf, auto-restage nach Auto-Escape
4. FIN-047 in orchestrator.md verankert: Auto-Write von RUN-ISSUES-<ISO-DATUM>.md nach jedem Lauf in `.android-shield/`
5. Frischer `/finale:run` auf BestJournalAndroid als Test (siehe ABSCHLUSS-Sektion unten)

**Zusaetzlich:** Intelligenz-Vorschlag 2 umgesetzt:
- `~/.claude/scripts/bug-doku-collector.sh` + `.ps1` (siehe `bug-doku-collector.README.md`)
- Sammelt offene `auto_captured`-Bug-Cases in `~/.claude/todo-bug-doku.md`
- Hook-Aktivierung bewusst nicht automatisch (Frank entscheidet via Settings)

---

---

## BUG #1 — Skill-Symlinks fehlen nach Plugin-Klon/Checkout (Phase 0)

**Schweregrad:** 🟥 Blocker — Plugin startet nicht ohne manuelle Reparatur

**Symptom:**
`verify-skills.sh` meldet fuer ALLE 4 Skills `symlink-missing`. Phase 0 bricht ab.

```
roentgen-skill           error  symlink-missing  → ~/.claude/skills/app-roentgen
rechtssicherheits-skill  error  symlink-missing  → ~/.claude/skills/rechtssicherheit
strings-skill            error  symlink-missing  → ~/.claude/skills/string-extraktor
uebersetzer-skill        error  symlink-missing  → ~/.claude/skills/übersetzung
```

**Root Cause:**
Symlinks werden auf Windows nicht zuverlaessig durch Git getrackt (haengt vom
`core.symlinks`-Setting + Developer-Mode ab). Beim Klonen / nach Plattformwechsel
verschwinden sie still. Das Plugin hat keinen Auto-Repair-Mechanismus.

**Schlimmer noch:** `Git Bash + ln -s` auf Windows ohne `MSYS=winsymlinks:nativestrict`
legt **Kopien** statt Symlinks an. Die naechste verify-skills-Pruefung wuerde
zwar "ok" sagen, aber die Skills sind dann stale Kopien — Aenderungen am
Original-Skill schlagen NICHT durch.

**Workaround (manuell, dieser Lauf):**
PowerShell `New-Item -ItemType SymbolicLink -Path $link -Target $target -Force`
fuer alle 4 Skills.

**Fix-Vorschlaege fuer Plugin:**
1. **Auto-Repair in Phase 0:** Wenn `symlink-missing`, automatisch versuchen
   den Symlink anzulegen — auf Windows via `New-Item -ItemType SymbolicLink`,
   auf macOS/Linux via `ln -s`. Erst nach Fehlschlag dem Nutzer abbrechen.
2. **Post-Install-Hook:** Ein `install.sh`/`install.ps1` das beim ersten
   Plugin-Aufruf die Symlinks legt (Skill-Quellpfade dynamisch ermitteln via
   `installed_plugins.json` oder bekannte Standard-Pfade).
3. **Guard gegen Git-Bash-Kopien:** verify-skills.sh sollte zusaetzlich pruefen
   ob `linkPath` ein echter Symlink ist (`test -L`) und KEIN normales Verzeichnis.
   Sonst false-positive `ok` bei stale Kopien.
4. **Alternative ohne Symlinks:** Skills direkt im Plugin-Verzeichnis ablegen
   (kein Symlink-Indirection-Layer noetig). Vorteil: zero Setup. Nachteil:
   Skill-Aenderungen in `~/.claude/skills/` schlagen nicht mehr automatisch durch.

**Empfehlung:** Variante 1 + 3 kombinieren. Auto-Repair bei `symlink-missing`,
Detection von Git-Bash-Kopien als zusaetzlicher Fehlerfall.

---

## BUG #2 — `Plugin/skills/` enthielt nur README.md, keine Symlinks im Git

**Schweregrad:** 🟧 mittel — Folgefehler von BUG #1

**Symptom:**
Im Git-Repo `~/proggs/Umgebung/Plugins/finale/Plugin/skills/` liegt nur
`README.md` (353 Bytes). Die 4 Symlinks sind nicht eingecheckt.

**Root Cause:**
Entweder wurden sie NIE eingecheckt, oder Git auf Windows hat sie still
ausgepackt (siehe BUG #1). Es gibt aber `~/proggs/Umgebung/Plugins/finale/Skills/`
(grosses S!) mit den Skills inline. Das deutet darauf hin, dass das Plugin
urspruenglich auf Inline-Skills designt war und der Symlink-Mechanismus
nachtraeglich kam.

**Fix-Vorschlag:**
README in `Plugin/skills/` erklaert was Symlinks ein muessen. Entweder die
Symlinks im Git tracken (mit `core.symlinks=true` und Hinweis im README),
oder konsequent zu Inline-Skills (BUG #1 Fix-Vorschlag 4) wechseln.

---

## BUG #3 — `skill-versions.json` wurde im vorherigen Lauf nie geschrieben

**Schweregrad:** 🟧 mittel — Delta-Erkennung unmoeglich

**Symptom:**
Vorheriger Lauf vom 2026-05-18 hat `roentgen-report.json`, `recht-report.json`,
`audit-log.md` geschrieben — aber KEINE `skill-versions.json`. Damit kann der
aktuelle Lauf nicht erkennen, ob ein Skill seit letztem Lauf geaendert wurde
(alle 4 Skills werden als "neu" gemeldet, obwohl sie schon einmal benutzt wurden).

**Root Cause vermutlich:**
Der vorherige Lauf wurde unterbrochen (audit-log endet bei Phase 1B-late nach
URL-Checker, kein "phase5-completed"-Eintrag). Die `skill-versions.json` wird
laut orchestrator.md "Am Ende jedes Laufs (egal ob completed / interrupted /
error)" geschrieben. Dass sie fehlt, deutet auf zwei moegliche Bugs:

a) Schreiben passiert NUR bei `completed`, nicht bei `interrupted`.
b) Schreiben ist nicht implementiert (Spec, aber kein Code).

**Fix-Vorschlag:**
1. Pflicht-Hook am Ende von Phase 0 (oder mindestens vor jedem Phase-Wechsel):
   `skill-versions.json` schreiben mit aktuellem Hash + Status. So ist sie
   auch nach Interrupt aktuell.
2. Wenn vorherige Datei fehlt, Lauf nicht als "Erstlauf" markieren, sondern
   einen Hinweis ausgeben "Vorheriger Lauf hat Skill-Versionen nicht gespeichert
   — Delta-Erkennung beim naechsten Lauf wieder moeglich."

---

## BUG #4 — `__pycache__/` im `.android-shield/`-Output

**Schweregrad:** 🟨 niedrig — Kosmetik

**Symptom:**
Im Output-Verzeichnis `.android-shield/` liegt ein `__pycache__/`-Ordner.
Das Plugin schreibt offensichtlich Python-Helferskripte oder fuehrt sie dort aus.

**Fix-Vorschlag:**
- Plugin sollte beim Start `.gitignore` oder `.android-shield/.gitignore` anlegen
  das `__pycache__/` ausschliesst.
- Oder: Python mit `PYTHONDONTWRITEBYTECODE=1` aufrufen wenn das Plugin Python nutzt.

---

## BUG #5 — 15-Worker-Default ist nicht universell sinnvoll

**Schweregrad:** 🟨 niedrig — Design-Frage, kein Blocker

**Symptom:**
Plugin-Direktive FIN-023 verlangt "15 parallele Worker" als Default fuer Phase 1A
und 1B. Bei einer mittelgrossen App (143 Kotlin-Dateien, 1117 Strings) ist das
massiv overkill — die Roentgen-Skill produziert einen zusammenhaengenden
Architektur-Bericht, der schlecht in 15 Sub-Scopes zerlegbar ist. Jeder Worker
braeuchte 100k Token = bis 1.5M Tokens nur fuer Phase 1A.

**Root Cause:**
Die Direktive wurde fuer "GROSSE Apps" geschrieben. Die Schwellen-Logik
(`stringResourceCount > 800` oder `ktFileCount > 100`) feuert hier, aber bei
~1100 Strings sind 5-6 Worker realistischer als 15. 15 ist eine harte Zahl
ohne Skalierungs-Heuristik.

**Fix-Vorschlag:**
1. Dynamische Worker-Anzahl:
   - `workers = clamp(ceil(stringResourceCount / 250) + ceil(ktFileCount / 30), 3, 15)`
   - Bei BestJournalAndroid: `ceil(1117/250) + ceil(143/30) = 5 + 5 = 10` → nicht 15
2. Delta-Mode automatisch aktivieren wenn vorheriger Report < 7 Tage alt:
   - Nur geaenderte Dateien (`git diff --name-only <prev-run-ts>`) durch Worker schicken
   - Massiv weniger Token-Verbrauch
3. Single-Worker-Modus fuer kleine/Delta-Audits ergaenzen — nicht jeder Lauf
   braucht Map-Reduce.

---

## BUG #6 — Roentgen-Skill nicht inherent partitionsfaehig

**Schweregrad:** 🟧 mittel — bei FIN-023-Anwendung problematisch

**Symptom:**
Der Roentgen-Skill produziert einen ZUSAMMENHAENGENDEN Bericht (Architektur-Inventar,
Klick-Pfade, Werbeaussage-vs-Feature-Matrix) der nicht trivial in 15 unabhaengige
Sub-Scopes zerlegbar ist:

- "Klick-Pfade" gehen ueber Composables hinweg — ein Worker der nur "Composable A-F"
  liest sieht die Pfade nicht
- "Werbeaussage-vs-Feature-Matrix" braucht ALLE Werbeaussagen UND ALLE Features —
  ist nur global sinnvoll
- "Paywall-Inventar" springt zwischen Composables, ViewModels, Billing-Code

**Fix-Vorschlag:**
1. **Worker-Scopes nach Funktion** statt nach Dateien:
   - Worker R1: Manifest + Permissions + SDKs + Build-Config
   - Worker R2: Strings + Wortlaute (alle Sprachen)
   - Worker R3: Compose-Screens + Click-Handler + Navigation
   - Worker R4: Paywall + Billing + Premium-Features (alle Dateien dazu)
   - Worker R5: Werbung im UI (Banner, Onboarding, Notifications)
2. Synthesizer mit explizitem "Werbeaussage-vs-Feature-Matrix"-Schritt nachdem
   alle Worker fertig sind.
3. Roentgen-Skill um optionale Scope-Parameter erweitern (`--scope=permissions,manifest`).

---

## BUG #7 — Roentgen-Output-Schema entspricht NICHT der orchestrator.md-Spec

**Schweregrad:** 🟧 mittel — Synthesizer/Phase-1B-Konsumenten muessen Schema-mapping machen

**Symptom:**
`orchestrator.md` Zeile 416-441 definiert das erwartete Roentgen-JSON-Schema:
```
{ "appName", "scanTimestamp", "scanMode", "skillVersionUsed",
  "structure": { "modules", "screens", "paywallSteps", "permissions", "hiddenFeatures" },
  "strings": [...]
}
```

Der tatsaechliche Output des Roentgen-Skills (im 2026-05-18-Lauf):
```
{ "schema_version", "audit_date", "app", "auditor", "effort",
  "layer1_manifest", "layer2_dependencies", "layer3_architecture",
  "layer4_screens", "layer4b_wortlaut_mapping", "layer4c_translation_context",
  "layer4d_legal_text_inventory", "layer5_paywall_deep",
  "layer6_hidden_features", "layer7_marketing_claims_matrix",
  "critical_findings", "dont_miss_checklist_summary", "meta" }
```

Beide haben semantisch aehnliche Inhalte, aber unter unterschiedlichen Keys.
Das Plugin-Spec passt nicht zur Skill-Realitaet — entweder veraltete Spec oder
Schema-Drift seit Skill-Update.

**Konsequenz:**
- Phase 1B-Synthesizer muss heuristisch beide Schemas verstehen
- Phase 2-Fix-Applier hat keine stabile Referenz fuer die "currentText"-Quelle
- Cross-Run-Vergleiche schwer (Schema-Updates brechen die Pipeline still)

**Fix-Vorschlag:**
1. Spec in orchestrator.md auf das tatsaechliche layer-basierte Schema updaten
2. ODER: Roentgen-Skill so erweitern dass er BEIDE Schemas ausgibt (compat-Section + neue Layers)
3. Schema-Version-Check in Phase 0: wenn `schema_version` aus dem Report nicht
   zur Plugin-Version passt, automatisch Neuscan empfehlen

---

## BUG #8 — Vorheriger Lauf hat Phase 3, 4, 5 nie erreicht (interrupted ohne Abschluss-Marker)

**Schweregrad:** 🟧 mittel — Closed-Loop wurde nicht beendet, aber Lauf wurde nicht als "interrupted" markiert

**Symptom:**
Audit-Log endet bei `Phase 1B-late · URL-Checker URL-001` (2026-05-18T19:00).
Es gibt KEINE Eintraege fuer Phase 3 (Uebersetzung), Phase 4 (Re-Audit) oder
Phase 5 (Loop-Entscheidung). Aber:
- `openFindingsCount: 0` im recht-report.json (Phase 5-aequivalent erreicht???)
- 27 Sprach-Uebersetzungen wurden durchgefuehrt (Commits #882-#885) — aber MANUELL ausserhalb des Plugins
- phase4-audit-w1.md, w2.md, w3.md existieren als Artefakte (Wave 1-3 von Phase 4)
- `auditedLanguages: ["de"]` — Cross-Lingual-Audit (Phase 3c) lief NIE

**Root Cause:**
Der Plugin-Lauf wurde nach Phase 2/Phase 1B-late unterbrochen. Die nachfolgende
Uebersetzungsarbeit lief manuell. Aber:
- Es gibt keinen `interrupted-by-user` Eintrag im audit-log
- Keine `resume-state.json` im `.android-shield/` Ordner
- `openFindingsCount: 0` ist irrefuehrend — es sind 0 Findings fuer DE, aber
  die 27 anderen Sprachen wurden nie geprueft

**Fix-Vorschlag:**
1. Plugin schreibt am Anfang jeder Session eine `run-status.json`:
   `{ "currentRun": "<uuid>", "currentPhase": "phase1B", "startedAt": "...",
   "lastHeartbeat": "..." }`. Beim regulaeren Ende: `"status": "completed"`.
   Bei Stop ohne Cleanup: naechster Lauf erkennt fehlenden `"status": "completed"`
   und bietet Resume an.
2. `openFindingsCount` muss nach Cross-Lingual-Audit (Phase 3c) re-evaluiert werden —
   nicht direkt nach Phase 2 auf 0 setzen.
3. Phase 3c (Cross-Lingual) sollte VORAUSSETZUNG fuer "Lauf abgeschlossen" sein,
   nicht nur Phase 2 + Re-Audit DE.

---

## BUG #9 — Token-Cap ist zu niedrig — Frank-Direktive 2026-05-22: Erhoehe auf 145k

**Schweregrad:** 🟨 niedrig — Plugin-Direktive sollte aktualisiert werden

**Symptom:**
Cross-Lingual-Worker hat 135.350 Token verbraucht und funktionierte. FIN-004/005
sagt max 100k. Frank's Erfahrung 2026-05-22: 145k geht problemlos.

**Frank-Direktive 2026-05-22:**
"Wir erhoehen Token-Cap auf 145.000 Token. Dieser Bereich funktioniert auch noch."

**Plugin-Update noetig:**
- `orchestrator.md` Zeile ~28-34 (FIN-004/005-Block): "100.000 Token" → "145.000 Token"
- `orchestrator.md` Zeile ~50: Synthesizer-Cap entsprechend auf 145k
- Soft-Warning bei 120k Verbrauch (statt 80k), Hard-Stop bei 140k

**Hinweis:** Die Bedingung bleibt — bei Naeherung an die Schwelle den aktuellen
Teilstand sichern und Folge-Worker spawnen. Aber 145k statt 100k als Default.

---

## BUG #10 — Worker hat eigenes Output-Schema benutzt (Spec-Verstoss)

**Schweregrad:** 🟧 mittel — Synthesizer/Konsumenten brechen still

**Symptom:**
Mein Worker-Prompt hat explizit das Schema vorgegeben:
- `key`, `currentText`, `deOriginal`, `problem`, `suggestedFixes` (array)

Der Worker hat stattdessen geschrieben:
- `stringKey`, `currentTranslation`, `deReference`, `issue`, `suggestedFix` (singular!)

Das bricht jeden Konsumenten der das Plugin-Schema erwartet.

**Root Cause:**
Subagent-Frontmatter / Prompt-Hierarchie verlangt nicht zwingend Schema-Adherenz.
Der Subagent hat das Schema "interpretiert" statt 1:1 uebernommen. Klassisches
LLM-Drift-Problem.

**Fix-Vorschlag:**
1. Worker-Prompt mit STRICT-JSON-Validierungs-Anweisung enden: "Deine Antwort
   wird mit `jsonschema.validate()` gegen folgendes Schema geprueft. Bei
   Schema-Verstoss wird sie verworfen."
2. JSON-Schema-Datei mitschicken + Worker schreibt durch Pydantic-aehnliches
   Validation-Layer.
3. Synthesizer (Post-Worker-Step) prueft Schema-Adherenz, ruft Worker erneut
   bei Verstoss mit Korrektur-Hinweis.
4. Im Plugin-Code: Schema-Migration-Layer fuer beide Schemas (mein erwartetes
   + Worker-Variante) damit Konsumenten beide verstehen.

---

## BUG #11 — Worker hat plugin_bugs_observed leer gelassen trotz expliziter Aufforderung

**Schweregrad:** 🟨 niedrig — Selbstbeobachtung des Workers funktioniert nicht

**Symptom:**
Im Worker-Prompt war eine klare Anweisung:
> "Plugin-Bugs sammeln: Wenn dir Reibungen / Bugs des Plugin-Workflows auffallen,
> liste sie in `plugin_bugs_observed`."

Im Output: `plugin_bugs_observed: []` (leer).

**Root Cause:**
Subagent fokussiert auf die Hauptaufgabe und ignoriert Meta-Beobachtung.
Direktive #2 (Selbstbeobachtung) wird nicht an Subagents propagiert.

**Fix-Vorschlag:**
1. Subagent-Templates erweitern um Pflicht-Selbstbeobachtungs-Frage am Ende:
   "Welche 1-3 Beobachtungen hast du gemacht die fuer Plugin-Verbesserung
   relevant sind?"
2. Wenn Subagent nichts schreibt: das selbst als Bug-Signal werten
   ("Self-Observation nicht aktiv im Subagent X").

---

## BUG #12 — Phase 1B-cross-lingual hat NUR EINEN Worker statt parallele

**Schweregrad:** 🟧 mittel — Geschwindigkeitsverlust

**Symptom (Frank-Beobachtung 2026-05-22):**
"Phase 1 lief doch ziemlich langsam." Tatsaechlich verstrichen Zeit:
- Cross-Lingual-Worker: 496 Sekunden (8 Min 16 Sek) — SEQUENZIELL fuer 27 Sprachen × 13 Keys

Mit 4-5 parallelen Workern (je ~6-7 Sprachen) waere das in 2-3 Minuten durch gewesen.

**Root Cause:**
Mein pragmatisches Reasoning war: "13 Keys × 27 Sprachen = 351 Checks, das passt
in einen Worker." Das stimmte token-maessig, aber zeitlich ist sequenzielles
LLM-Reasoning ueber 27 Sprachen langsam. Parallel haette es geviertelt.

Der Plugin-Spec sagt FIN-023 "15 parallele Worker"; ich habe das auf 1 reduziert
weil "App nicht so gross". Das war falsch — auch bei mittelgrossen Apps profitiert
Cross-Lingual von Parallelisierung weil JEDE Sprache eigenes Reasoning braucht.

**Fix-Vorschlag (Frank-Direktive 2026-05-22):**
1. Cross-Lingual-Audit IMMER mit min. 4-5 parallelen Workern, gegliedert nach
   Sprach-Buckets:
   - Worker 1: ar, bn, en, es, fr (5)
   - Worker 2: gu, hi, in, it, ja (5)
   - Worker 3: kn, ko, ml, mr, nl (5)
   - Worker 4: pl, pt-rBR, pt-rPT, ru, ta (5)
   - Worker 5: te, th, tr, uk, ur, zh-rCN, zh-rTW (7)
2. Synthesizer (Hauptagent) konsolidiert die 5 Teil-Reports zu cross-lingual-findings.json
3. Plugin-Direktive in orchestrator.md erweitern: "Phase 1B-cross-lingual: IMMER
   mindestens 4 parallele Sprach-Worker, auch bei kleinen Apps. Sprache als
   Scope-Trenner, nicht Code-Groesse."

**Erwarteter Geschwindigkeits-Gewinn:** 8 Min → 2-3 Min (4× schneller)

---

## BUG #13 — Worker hat `currentTranslation`-Inhalte HALLUZINIERT trotz korrekter Matrix-Eingabe

**Schweregrad:** 🟥 hoch — Korrektheit der Findings nicht garantiert

**Symptom:**
Cross-Lingual-Matrix (Input des Workers) enthielt KORREKT:
- gu/churn_cancel_sub: "♻" (nur das Recycling-Symbol — komplett kaputt)
- gu/retro_benefit_weekly: ". 2 ." (nur Format-Platzhalter — komplett kaputt)

Worker hat im Output stattdessen behauptet:
- CL-004 currentTranslation: "♻ ਰદ કરો (. 2 .)" — Mischung aus Wahrheit und Halluzination
- CL-005 currentTranslation: "અઠવાડિક વિચાર. 2 . વધુ" — komplett halluziniert
- CL-007 currentTranslation: "ನಿಮ್ಮ ಜೀವನದ ಮೇಲೆ 5 ದೃಷ್ಟಿಕೋನಗಳು" — echte Realitaet ist
  "ನಿಮ್ಮ ಜೀವನದ ಮೇಲೆ\nಎಲ್ಲ AI ಪ್ರೊಫೈಲ್‌ಗಳು" (komplett anderer Inhalt!)

Ausserdem CL-005 suggestedFix ("સાપ્તાહિક સમીક્ષા: 2 એન્ટ્રી વિશ્લેષિત" =
"Wöchentliche Übersicht: 2 Einträge analysiert") ist SEMANTISCH FALSCH —
echtes DE = "Unbegrenzte Wochenrueckblicke, nicht nur die ersten 2 Wochen".
Worker hat DE-Bedeutung erraten statt das DE-Original zu lesen.

**Root Cause:**
1. Worker hat das DE-Original nicht 1:1 zitiert sondern aus seiner Interpretation
   der gu-Mojibake-Reste konstruiert
2. Worker hat in der Matrix die "real-current"-Felder mit eigenen Verbesserungs-
   Annahmen ueberschrieben
3. Subagent-Prompt fehlte die Anweisung "ZITIERE 1:1 wortgenau aus der
   Matrix, keine Veredelung, keine Annahme"

**Konsequenz:**
- Bulk-Apply ohne Vor-Verifikation haette FALSCHE Texte committed
- Halluzinationen sind ein Worker-Trust-Problem das die gesamte Pipeline gefaehrdet

**Fix-Vorschlag:**
1. Pflicht-Anweisung in jedem Worker-Prompt: "WORTGENAU 1:1 aus dem Input-File
   zitieren. Keine Eigen-Konstruktion. Bei zweifel: input-string copy-paste."
2. Post-Worker-Verifikations-Step: Hauptagent prueft fuer jedes Finding
   `currentTranslation == matrix[key][lang]`. Bei Abweichung: Worker hat halluziniert,
   Finding mit `hallucination_detected: true` markieren + DE-Quelltext nachladen.
3. Strikter Tool-Use: Worker MUSS via `Read`-Tool die Originaldatei lesen, nicht
   nur aus der vorbereiteten Matrix.
4. Validation-Layer: `suggestedFix` semantisch gegen `deReference` pruefen
   (Reverse-Translate via uebersetzung-Skill und checken ob Bedeutung
   uebereinstimmt).

---

## BUG #14 — Plugin sollte uebersetzung-Skill ZWINGEND fuer JEDEN String-Fix nutzen (Frank-Direktive 2026-05-22)

**Schweregrad:** 🟥 hoch — Halluzinations-Schutz

**Frank-Direktive:**
> "Bei Uebersetzungen von Strings soll immer der Uebersetzer-Skill benutzt werden,
> ohne Ausnahme. Dann koennte sowas in Zukunft mit den Halluzinierungen nicht passieren."

**Plugin-Update noetig:**
Der orchestrator.md Phase 2 fix-applier-Logik MUSS so erweitert werden, dass
JEDER Edit an einer values-*/strings.xml NUR ueber den uebersetzung-Skill geht —
auch wenn es nur EIN String, EIN Wort, eine einzelne Korrektur ist.

**Konkret:**
- fix-applier (oder Hauptagent) sieht eine Karte mit `language != "de"`
- → Skill(skill="uebersetzung") wird aufgerufen
- → Skill bekommt: DE-Original + Zielsprache + spezifischen Key + Kontext-Hinweis
  ("dieser String muss angepasst werden weil UWG-§5-Verstoss, alte Version: X,
  Neue DE-Polish: Y, bitte Zielsprachen-Aequivalent erzeugen")
- → Skill liefert die rechtssichere Uebersetzung
- → Edit in values-XX/strings.xml mit verifiziertem Text

**KEINE Ausnahmen:**
- Auch fuer 1-Wort-Ersetzungen wie "KI" → "AI" (Markenkonsistenz)
- Auch fuer offensichtlich-richtige Worker-Vorschlaege
- Auch bei "kleinen" Korrekturen wie Komma-Fixes

**Begruendung:**
Worker halluzinieren (siehe BUG #13). Skill-basierte Uebersetzung ist
1:1-zuverlaessig weil sie nicht erfindet sondern uebersetzt.

---

## BUG #15 — Phase 2 (Fix-Applier) ebenfalls nur 1 Worker statt parallel (Frank-Direktive 2026-05-22)

**Schweregrad:** 🟧 mittel — Geschwindigkeitsverlust

**Frank-Beobachtung 2026-05-22:**
> "Falls auch dort immer ein einziger Sub-Agent eingesetzt wurde, dann soll das auch
> in Zukunft viel mehr sein. Da koennte man auch gleichzeitig 12 gleichzeitig starten."

**Tatsaechliche Performance:**
- Fix-Applier-Subagent: 771 Sekunden (~13 Minuten) fuer 12 Strings
- Token-Verbrauch: 145.249 (am Cap)
- 54 Tool-Uses (sequenzielle Reads + Edits)

Mit 4-6 parallelen Fix-Appliers waere das auf 3-4 Min reduziert worden.
Mit 12 parallelen Workern (1 pro Finding) auf <2 Min — aber Token-Verbrauch
12× hoeher, eventuell Overkill.

**Root Cause (Plugin-Design):**
1. orchestrator.md Phase 2 sieht NUR sequenzielle Fix-Karten vor (eine Karte
   nach der anderen)
2. FIN-006 Bundle-Karten reduziert nur die Anzahl der Karten, nicht parallelisiert
3. Cross-Lingual-Korrekturen sind aber IDEAL fuer Parallelisierung:
   jede Sprache × jeder Key ist unabhaengig

**Frank-Direktive 2026-05-22:** "Geschwindigkeit des gesamten Plugins soll generell
erhoeht sein und erhoeht werden."

**Fix-Vorschlag (Plugin-Update):**
1. **Phase 2 Bulk-Mode (NEU):** Statt Karte-fuer-Karte: wenn Findings unabhaengig
   sind (verschiedene Files, keine Reihenfolge-Abhaengigkeit), `bulk_apply: true`-
   Flag aktivieren und N parallele fix-applier-Worker spawnen
2. **Sweet-Spot 4-8 parallel:** Bei <4 Findings: sequenziell. Bei 4-15 Findings:
   4-6 parallele Worker. Bei 15+: 8-15 parallele Worker (Frank's 15-Worker-Default)
3. **Cross-Lingual ist immer parallel-faehig:** Sprache × Key ist immer unabhaengig,
   daher Phase 1B-cross-lingual UND Phase 2-cross-lingual-fix immer parallel
4. **Karten-UI bleibt:** Frank kann immer noch Karte fuer Karte entscheiden, aber
   Bulk-Modus ist Option fuer "trust the recommendations"-Faelle
5. **Continuous-Spawning (FIN-023) auch in Phase 2:** Sobald 1 Worker fertig,
   sofort naechsten spawnen — keine starre 4er-Welle

**Generelles Plugin-Speed-Ziel:**
- Mittelgrosse App (BestJournalAndroid: 143 KT-Files, 1117 strings, 27 Sprachen):
  Phase 1+2+3+4 in unter 15 Minuten gesamt statt aktuell 30+ Min
- Frank bevorzugt mehr Parallelisierung > Token-Effizienz

---

## BUG #16 — Generelle Parallelisierungs-Direktive (Frank 2026-05-22) — UEBERGREIFENDE PLUGIN-REGEL

**Schweregrad:** 🟥 hoch — fundamentale Plugin-Designvorgabe

**Frank-Direktive 2026-05-22 (woertlich):**
> "Es soll eigentlich so viel wie moeglich parallel arbeiten, innerhalb der Phasen.
> Nicht nacheinander Agents machen, sondern immer so gut wie moeglich parallel
> alles starten, was geht. Nicht nur bezogen auf Phase 2, sondern generell."

**Dies ist eine UEBERGREIFENDE Plugin-Regel, die ALLE Phasen betrifft:**

| Phase | Aktuelle Praxis | Soll-Praxis (Frank-Direktive 2026-05-22) |
|-------|----------------|------------------------------------------|
| Phase 0 (Skill-Verifikation) | 1 sequenzielles Script | Pro Skill 1 paralleler Verify-Task — 4 parallel statt sequenziell |
| Phase 1A (Roentgen) | 1 Worker (oder gar keiner bei Delta) | 5-15 parallele Worker, Scope-Split nach Layer/Modul |
| Phase 1B (Recht) | 1 Worker oder 3 sequenziell | 5-15 parallele Worker nach Jurisdiktion/Kategorie |
| Phase 1B-cross-lingual (NEU) | 1 Worker fuer 27 Sprachen (BUG #12) | 4-5+ parallele Sprach-Bucket-Worker |
| Phase 2 (Fix-Applier) | 1 Worker fuer N Findings (BUG #15) | Bei unabhaengigen Findings: 4-12+ parallele Worker |
| Phase 3a (Strings-Migration) | Sequenziell | Parallel pro Modul/Datei |
| Phase 3b (Uebersetzungs-Phase) | Theoretisch parallel (1 pro Sprache) — Praxis bei BestJournalAndroid 2026-05-18 unklar | 15 parallele Sprach-Worker (Continuous-Spawning FIN-023) |
| Phase 3c (Cross-Lingual-Recht) | Sequenziell | Parallel pro Sprache |
| Phase 4 (Re-Audit) | Theoretisch parallel (mehrere W1/W2/W3) — Praxis 2026-05-18 sequenziell | 5+ parallele Re-Audit-Worker |
| Phase 5 (Loop-Entscheidung) | Sequenziell-Synthesizer | Bleibt sequenziell (Entscheidung muss zentral sein) |

**Konkrete Plugin-Updates:**

1. **orchestrator.md** muss eine zentrale "Parallel-First"-Direktive bekommen, hoeher
   in der Hierarchie als FIN-023:
   ```
   FIN-029 (Frank-Direktive 2026-05-22): SO VIEL WIE MOEGLICH PARALLEL,
   INNERHALB JEDER PHASE. Default ist parallel, sequenziell ist die Ausnahme
   die expliziet gerechtfertigt werden muss.
   ```

2. **Pre-Flight-Plan** muss pro Phase die geplante Parallelitaet anzeigen:
   ```
   Phase 1A: 6 Worker parallel (Roentgen-Layer-Split)
   Phase 1B: 8 Worker parallel (Recht-Kategorie-Split + 4 Sprach-Buckets cross-lingual)
   Phase 2: max(N, 8) parallele fix-applier wo N = Anzahl-Findings, sequenziell nur bei <4 Findings
   ```

3. **Synthesizer-Pattern** muss IMMER eingebaut sein wenn parallele Worker arbeiten —
   nicht optional. Verhindert Inkonsistenzen wenn 8 Worker ihre Teilergebnisse haben.

4. **Worker-Cap dynamisch:** Sweet-Spot ist 8-15 parallel; bei >15 nimmt Coordination-Overhead
   ueberhand. Defaults pro Phase definieren statt einmal "15 fuer alles".

5. **Audit-Log:** Pro Phase eintragen wie viele Worker tatsaechlich parallel liefen.
   Sonst kann man die Parallelitaet nicht im Nachhinein pruefen.

**Verifizierte Geschwindigkeitsverluste in DIESEM Lauf:**
- Phase 1B-cross-lingual: 8 Min sequenziell, geschaetzt 2 Min mit 5 parallelen Workern (-75%)
- Phase 2 Fix-Apply: 13 Min sequenziell, geschaetzt 3 Min mit 6 parallelen Workern (-77%)
- Gesamt-Lauf: ~30 Min, mit voller Parallelisierung erwartet ~10-12 Min (-67%)

**Plugin-Identitaet:** Das `finale`-Plugin sollte nach Frank's Vision "ein Schwarm-System"
sein, nicht eine sequenzielle Pipeline. Jeder Schritt der parallel laufen KANN, MUSS auch
parallel laufen — nicht weil Token-Sparen, sondern weil Frank's Zeit kostbarer ist als
Tokens.

---

(Weitere Bugs werden eingefuegt waehrend der Lauf fortschreitet.)

---

## BUG #17 — Plugin-Cache veraltet: assets/ und schemas/ fehlen (Loop 3)

**Schweregrad:** 🟧 Mittel — FIN-031 (.gitignore-Template) und FIN-036 (Schema-Validation) funktionieren mit installierter Plugin-Version nicht. Fallback erforderlich.

**Symptom:**
Der via Plugin-Manager installierte Plugin-Cache unter
`~/.claude/plugins/cache/local/finale/0.1.0/` enthält weder das `assets/`-
Verzeichnis (mit `android-shield-gitignore` Template) noch das `schemas/`-
Verzeichnis (mit den FIN-036-JSON-Schemas). Beide existieren nur im Quell-Repo
unter `~/proggs/Umgebung/Plugins/finale/Plugin/`.

```
$ ls /c/Users/barwa/.claude/plugins/cache/local/finale/0.1.0/
README.md  agents/  commands/  hooks/  scripts/  skills/
# fehlt: assets/, schemas/

$ ls /c/Users/barwa/proggs/Umgebung/Plugins/finale/Plugin/
agents/  assets/  commands/  hooks/  schemas/  scripts/  skills/  README.md
```

**Root Cause:**
Vermutung: Beim Plugin-Update wurde nur ein Teil-Sync des Quell-Repos in den
Cache übernommen. Entweder hat der Marketplace-Sync `assets/` + `schemas/`
ausgelassen, oder das Plugin wurde nach Anlegen der beiden Verzeichnisse nie
neu installiert/aktualisiert.

**Workaround (dieser Lauf):**
- `.gitignore` in `.android-shield/` per Fallback-Inline-Template erstellt
  (5 Zeilen statt das ausführlichere Quell-Template).
- Schemas müssen vor der Schema-Validation aus dem Quell-Repo gelesen werden
  oder die Validierung wird auf reine Type-Checks reduziert.

**Fix-Vorschläge für Plugin:**
1. **Plugin neu installieren:** `/plugins refresh` oder vollständig de- und
   reinstallieren damit `assets/` und `schemas/` synchronisiert werden.
2. **Manifest-Vollständigkeitscheck:** Beim Plugin-Update sicherstellen dass
   ALLE Verzeichnisse aus dem Quell-Repo mitgenommen werden (kein selektives Sync).
3. **Orchestrator-Fallback:** Wenn `${FINALE_PLUGIN_ROOT}/assets/` oder
   `${FINALE_PLUGIN_ROOT}/schemas/` fehlt, versuche zusätzlich
   `~/proggs/Umgebung/Plugins/finale/Plugin/assets|schemas/` als Fallback.

**Empfehlung:** Variante 1 sofort (Plugin neu installieren), Variante 3 als
defensiver Fallback im Orchestrator.

---

## BEOBACHTUNG #1 — verify-skills.sh läuft sauber, aber Phase 0 Schritte sind verteilt auf 5+ Orchestrator-Abschnitte

**Schweregrad:** 🟨 Niedrig — Funktioniert, aber Tracking erfordert Sprünge durch orchestrator.md

**Symptom:**
Phase 0 hat mittlerweile 8+ Schritte (Schritt -1, 0, 1, 2, 2b, 2c, 3, 4, 5, 6, 7, 8).
Die Reihenfolge im Orchestrator-Doc ist unverändert (Schritt 0 → 1 → 2 → 2b → 2c → 3 → ...),
aber Schritt 8 (Audit-Only-Lock) ist optional und kommt am Ende nach 6.

**Vorschlag:**
Phase 0 in zwei Sub-Phasen aufteilen: **Phase 0a — Verify** (Schritte 0-2c)
und **Phase 0b — Pre-Flight + Optional Lock** (Schritte 3-8). Macht die
Doku lesbarer und gibt klare Marker für Resume-Punkte.


---

## BUG #18 — Recht-Report wird durch Code-Aenderungen veraltet, kein Auto-Refresh

**Schweregrad:** 🟧 Mittel — fuehrt zu falscher Wahrnehmung "offene Findings" obwohl sie schon im Code geloest sind

**Symptom:**
Der `recht-report.json` vom 2026-05-18 listete 7 offene Findings (MD-001..MD-006 + URL-001).
Beim Lauf am 2026-05-22 stellte sich heraus dass ALLE 7 bereits im Code geloest sind
(ConsentScreen.kt verlinkt direkt zu Privacy/Impressum/AGB; AiOutputDisclaimer.kt + AiGeneratedBadge.kt
existieren mit Sichtbarkeit auf Dashboard/Retrospective/EntryDetail; URLs liefern HTTP 200; AGB-Seite
existiert als separates HTML).

**Root Cause:**
Phase 2 (interaktiver Fix-Workflow) markiert nur Findings die explizit durchgegangen wurden mit
einem `status`. Wenn der Code zwischen Plugin-Laeufen unabhaengig geaendert wird (z. B. Frank
implementiert AGB-Verlinkung manuell), bleibt der Report stale. Es gibt keine Phase-1-Refresh-Logik
die VOR der Anzeige offener Findings prueft ob das Finding noch aktuell ist.

**Workaround (dieser Lauf):**
- Manueller Code-Verifikations-Check pro Finding (Grep nach Strings/Komponenten + curl-HEAD fuer URLs).
- `status: resolved-verified-in-code` + `resolutionEvidence` Feld hinzugefuegt.
- `openFindingsCount` neu berechnet (war faelschlich `0`, sollte mit 7 offen sein gewesen).

**Fix-Vorschlaege fuer Plugin:**
1. **Phase 1.5 — Stale-Finding-Check:** Vor Phase 2, fuer jedes Finding ohne Status:
   - Wenn `invasivityLevel: text-only`: pruefe per Grep ob der currentText noch existiert
   - Wenn `category: missingDocs`: pruefe ob die zugehoerige Komponente/Datei jetzt existiert
   - Wenn `category: deadUrls`: HEAD-Check ob URL jetzt 200 OK ist
   - Bei Treffer: automatisch `status: resolved-since-audit` setzen + Hinweis im Pre-Flight
2. **Audit-Refresh-Mode:** Statt "Fix-Workflow" anbieten "Re-Audit only" der nur die offenen Findings re-evaluiert (nicht der ganze Audit neu).
3. **Audit-TTL:** Wenn `audit_date > 24h alt`: Pre-Flight-Warnung "Audit ist X Tage alt — Code koennte zwischenzeitlich angepasst worden sein, Stale-Finding-Check empfohlen".

**Empfehlung:** Variante 1 + 3 kombinieren. Phase 1.5 ist optional aber bei aelteren Reports default-an.


---

## BUG #19 — FIN-029 Worker-Count empfiehlt 15-20 auch bei Mini-Aufgaben

**Schweregrad:** 🟨 Niedrig — Effizienz-Problem, kein Funktionsfehler

**Symptom:**
FIN-029 (Frank-Direktive 2026-05-22) empfiehlt 15-20 parallele Worker bei "15+ Aufgaben".
Bei 11 fehlenden Strings × 27 Sprachen = 27 Sprach-Aufgaben → Empfehlung waeren 20 Worker.

Praktisch ist das ueberdimensioniert:
- Jeder Worker laedt uebersetzung-Skill (~3000 Token Standard) + Job-Context
- 20 Worker × ~5000 Token Startup = 100.000 Token nur fuer Worker-Setup
- Bei 11 Strings ist die eigentliche Arbeit pro Worker winzig

**Tatsaechlich eingesetzt:** 5 Worker, je 3-8 Sprachen. Spawn-Overhead minimal,
Parallelitaet trotzdem hoch.

**Root Cause:**
FIN-029 macht keinen Unterschied zwischen "viel Arbeit pro Aufgabe" (z. B. neue Sprache
mit 1000+ Strings) und "wenig Arbeit pro Aufgabe" (z. B. 10 Strings nachuebersetzen).
Die Worker-Count-Heuristik braucht eine zweite Dimension.

**Fix-Vorschlag:**
FIN-029 erweitern um Total-Arbeit-Schaetzung:
```
estimatedTotalUnits = aufgabenZahl × ((stringsProSprache) ODER (komponentenProAufgabe))
if estimatedTotalUnits < 100:
    workerCount = clamp(aufgabenZahl, 3, 6)         # spawn overhead dominiert
elif 100 <= estimatedTotalUnits < 1000:
    workerCount = clamp(aufgabenZahl, 5, 12)
else:
    workerCount = clamp(aufgabenZahl, 15, 20)       # bisheriges FIN-029
```

Beispiel:
- 11 Strings × 27 Sprachen = 297 Units → 5-12 Worker (statt 15-20)
- 1000 Strings × 27 Sprachen = 27.000 Units → 15-20 Worker
- 50 Findings × 5 Jurisdiktionen = 250 Units → 5-12 Worker

**Empfehlung:** FIN-029 Heuristik um Units-Dimension erweitern.

---

## BUG #20 — Plugin sollte "fehlende Strings pro Sprache" automatisch berechnen

**Schweregrad:** 🟨 Niedrig — Convenience

**Symptom:**
Zum Anstossen von Phase 3b (Uebersetzung) braucht das Plugin einen Job-Plan
mit "welche Strings fehlen in welcher Sprache". Das Plugin liefert diesen Job-Plan
nicht. Der Hauptagent muss ihn per Python-Diff selbst generieren:

```python
# manuell vom Hauptagent geschrieben:
for lang in langs:
    found = keys_in(f'app/src/main/res/values-{lang}/strings.xml')
    missing = de_translatable - found
    job_plan[lang] = {...}
```

**Root Cause:**
Der `strings-skill` hat einen `missing-keys-detector`, aber der ist nicht direkt
ans Plugin-Phase-3b angebunden. Phase 3b braucht den Job-Plan, aber das Plugin
loest die Diff-Analyse nicht von selbst aus.

**Fix-Vorschlag:**
Phase 3b um einen automatischen Vor-Schritt erweitern:
1. Vor Spawn der Translation-Worker: Diff-Analyse pro Sprache (kann sich Plugin oder
   strings-skill teilen).
2. Schreibe das Ergebnis in `<app-root>/.android-shield/translation-jobs/job-plan.json`.
3. Translation-Worker bekommen den Job-Plan-Pfad als Input statt selbst zu skripten.

**Empfehlung:** Pflicht-Schritt in Phase 3b einbauen, bevor Worker spawnen.

---

## BEOBACHTUNG #2 — uebersetzung-Skill hat keinen "batch-mode" für mehrere Sprachen

**Schweregrad:** 🟨 Niedrig — Workflow-Optimierung

**Symptom:**
Der `uebersetzung`-Skill arbeitet sprach-fuer-sprach sequentiell mit Verifikation
und Commit nach jeder Sprache. Das ist sehr saubere Architektur, aber bei
Multi-Sprach-Spawning erschwert es das pro-Worker-Coordination.

Frank's Memory: "i18n IMMER per Subagent" + "i18n NIEMALS Python" — also muss
jeder Subagent den Skill aufrufen. Aber der Skill ist auf 1-Sprache-Pro-Aufruf
ausgelegt. 

**Fix-Vorschlag:**
uebersetzung-Skill um einen optionalen `--multi-language=true` Modus erweitern:
- Akzeptiert eine Liste von Sprachen als Input
- Arbeitet trotzdem sequenziell intern, aber EINE Session
- Spart das wiederholte Skill-Loading bei mehreren Sprachen pro Subagent


---

## BUG #21 — Translation-Worker brauchen Pre-Check vor Insert (Duplikat-Risiko)

**Schweregrad:** 🟧 Mittel — kann Build brechen durch Duplikat-Keys

**Symptom (von Worker A im Lauf 2026-05-22 selbst beobachtet):**
- en hatte nur 3 von 11 deklarierten "fehlenden" Strings tatsaechlich fehlend
- 8 Strings waren bereits vorhanden
- Ohne Pre-Check haette der Worker 8 Duplikate erzeugt → XML mit doppelten <string name="..."> → Android-Build-Fehler

**Root Cause:**
Job-Plan-Generator (Phase 3b Vor-Schritt, siehe BUG #20) generiert eine pauschale
"11 fehlende Strings"-Aussage statt einer sprach-spezifischen Liste. Worker bekommen
die Pauschal-Liste und muessen selbst pruefen welche tatsaechlich fehlen.

**Fix-Vorschlag:**
Job-Plan-Datei `job-plan.json` enthaelt bereits PRO Sprache die Liste der missing_keys —
wird aber nicht verbindlich an die Worker durchgereicht. Lösung:
1. **Worker-Prompt explizit auf job-plan.json verweisen:** "Lies job-plan.json fuer
   deine Sprachen, dann uebersetze NUR die dort gelisteten Keys."
2. **Mandatory Pre-Check im Worker:** Vor dem Insert pruefen ob Key schon existiert.
   Bei Doppelung: skip + Hinweis im JSON-Output.

**Empfehlung:** Variante 1 + 2 kombinieren. Plugin sollte Workers nie eine pauschale
Liste geben, sondern immer per-Sprache-Diff.

---

## BUG #22 — Translation-Worker Insert via `replace` ist fehleranfaellig

**Schweregrad:** 🟨 Niedrig — Edge-Case, aber real

**Symptom (von Worker A beobachtet):**
Python `content.replace('</resources>', new_strings + '\n</resources>')` ersetzt
das ERSTE Vorkommen. Wenn `</resources>` in einem XML-Kommentar erscheint (selten,
aber moeglich), wird der Insert an falsche Stelle gesetzt.

**Fix-Vorschlag:**
Standard-Insert-Methode im Worker auf rfind-basiert umstellen:
```python
idx = content.rfind('</resources>')
new_content = content[:idx] + new_strings + '\n' + content[idx:]
```
Trifft IMMER das letzte Vorkommen, robust gegen Kommentare.

---

## BUG #23 — pl hatte retro_month_more_entries als einzige Sprache fehlend (atomarer Schreibfehler)

**Schweregrad:** 🟨 Niedrig — Hinweis auf vergangenen Bug

**Symptom (von Worker A beobachtet):**
`retro_month_more_entries` war in 26 von 27 Sprachen vorhanden, nur pl fehlte.
Das deutet darauf hin dass beim urspruenglichen Hinzufuegen dieses Strings (in einer
fruehen Session) der pl-Eintrag uebersehen wurde.

**Fix-Vorschlag (Praevention, nicht akut):**
1. Beim Hinzufuegen neuer Strings IMMER in alle 27 Sprachen gleichzeitig (auch wenn
   sie noch nicht uebersetzt sind, dann mit `tools:ignore="MissingTranslation"` oder
   TODO-Marker).
2. Cross-Sprachen-Audit als Phase 3c-Pre-Check: jede Sprache muss die GLEICHE Anzahl
   translatable-Keys haben wie die de-Referenz (modulo bewusst weggelassener Strings
   mit ignore-Tag).


---

## BUG #24 — Plugin-Subagents brauchen PYTHONIOENCODING=utf-8 als Default

**Schweregrad:** 🟧 Mittel — verursacht Worker-Crashes auf Windows

**Symptom (von Worker C im Lauf 2026-05-22 selbst beobachtet):**
Worker C crashte beim ersten Python-Versuch mit `UnicodeEncodeError: 'charmap' codec`,
weil Python auf Windows cp1252 als Default-stdout-Encoding nutzt und Emoji
(z. B. ✅) in print-Statements nicht encodiert.

Bug-Case-Auto-Writer hat mit 83% Aehnlichkeit den bekannten Fix injiziert — der zweite
Lauf war sofort korrekt mit `PYTHONIOENCODING=utf-8`.

**Root Cause:**
Subagent-Prompts setzen die Env-Var nicht. Workers muessen den Bug erst selbst
treffen und reparieren.

**Fix-Vorschlag:**
1. **Plugin-Subagent-Prompt-Template** um Env-Var-Header erweitern:
   ```
   ## ENV-SETUP (PFLICHT vor jeder Python-Operation)
   `export PYTHONIOENCODING=utf-8` (Bash) oder `$env:PYTHONIOENCODING="utf-8"` (PowerShell)
   ```
2. **Boilerplate** in jeden Worker-Prompt: "Bei Python-Aufrufen IMMER mit
   `PYTHONIOENCODING=utf-8` prefixen."

---

## BUG #25 — Worker brauchen Idempotenz-Guard nach Crash

**Schweregrad:** 🟧 Mittel — verhindert sauberen Retry nach Crash

**Symptom (von Worker C beobachtet):**
Erster Python-Lauf schrieb die ja-Datei vollstaendig, dann crashte print-Statement
mit Encoding-Error. Zweiter Lauf hatte keine Built-in-Guard — der Worker musste
selbst manuelles Already-Present-Check implementieren.

**Fix-Vorschlag:**
Plugin-Subagent-Prompt-Template muss Idempotenz-Pattern fordern:
```python
# Standard-Idempotenz-Check vor jedem Insert
for key in keys_to_insert:
    if f'name="{key}"' in existing_content:
        skipped_already_present.append(key)
        continue
    # ... insert
```
Output-Schema sollte ein `skipped_already_present: dict[lang, [keys]]`-Feld haben,
damit Idempotenz im JSON sichtbar wird (siehe Worker-C-Output).


---

## BUG #26 — Worker D crashte trotz FIN-005-Token-Cap (Autocompact-Thrashing)

**Schweregrad:** 🟥 Hoch — Worker mit zu vielen Sprachen pro Bucket loest Autocompact-Loops aus

**Symptom (Lauf 2026-05-22):**
Worker D (Bucket D-Indisch+Dravidisch, 8 Sprachen) crashte mit:
> "Autocompact is thrashing: the context refilled to the limit within 3 turns
> of the previous compact, 3 times in a row. A file being read or a tool output
> is likely too large for the context window."

Vermutung: Worker hat versucht, alle 8 strings.xml-Dateien (je ~1100 Strings,
~50-80 KB) komplett zu lesen + zu uebersetzen + zu schreiben. Das ueberlastet
den Worker-Kontext schneller als der 145k-Cap (FIN-005) reagieren kann.

**Workaround dieser Lauf:** Worker D durch zwei Folge-Worker (D-1 mit 4 Sprachen, D-2 mit 4 Sprachen) ersetzt mit strengeren Token-Vorgaben (max 80k, KEIN komplettes Read der bestehenden strings.xml).

**Root Cause:**
1. FIN-005 Cap (145k Token) reagiert erst sehr spaet
2. Worker hat keine "max-Sprachen-pro-Bucket"-Empfehlung
3. Worker liest grosse strings.xml-Dateien komplett statt nur Insert per rfind

**Fix-Vorschlaege fuer Plugin:**
1. **Max-Sprachen-pro-Worker = 5**: Bucket-Splitter im Orchestrator MUSS Buckets
   mit >5 Sprachen automatisch in 2 kleinere Buckets aufteilen.
2. **Standard-Template "rfind-Insert ohne komplettes Read":** Worker-Prompt
   muss explizit fordern, dass strings.xml NICHT vollstaendig gelesen wird —
   nur Insert-Position per `tail -1` oder rfind ermitteln.
3. **Token-Predictor:** Bevor Worker spawned wird, Token-Estimate pro Bucket
   ausrechnen: `sprachen × strings × 200 + skill_load_overhead`. Wenn >120k
   -> automatisch aufsplitten.

**Empfehlung:** Alle 3 Varianten kombinieren. Variante 1 ist die robusteste
und einfach umzusetzen.

---

## BUG #27 — ISO-639-1 vs. Android-Legacy-Code Mapping fehlt im Worker-Prompt

**Schweregrad:** 🟨 Niedrig — koennte Fehler bei neuen Workers verursachen

**Symptom (von Worker E im Lauf 2026-05-22 beobachtet):**
Indonesisch hat ISO-639-1-Code `id`, aber Android nutzt den Legacy-Code `in`
(values-in/). Das uebersetzung-Skill hat `id.md`, das Android-Verzeichnis ist
`values-in/`. Worker musste das selbst erschliessen.

Aehnliche Faelle:
- Hebraeisch: `he` (ISO) vs `iw` (Android Legacy)
- Yiddisch: `yi` (ISO) vs `ji` (Android Legacy)

**Fix-Vorschlag:**
Plugin-Orchestrator-Prompt-Template um eine explizite Mapping-Tabelle erweitern:
```
ISO -> Android-Legacy-Mapping:
- id (Indonesian) -> in (values-in/)
- he (Hebrew) -> iw (values-iw/)
- yi (Yiddish) -> ji (values-ji/)
- pt-BR -> pt-rBR
- pt-PT -> pt-rPT
- zh-Hans -> zh-rCN
- zh-Hant -> zh-rTW
```

---

## BUG #28 — Worker E: Urdu-Wortstellung am Satzanfang (RTL-Bidi-Falle)

**Schweregrad:** 🟨 Niedrig — Skill loest es, aber Worker ohne Skill-Kontext koennte falsch uebersetzen

**Symptom (von Worker E beobachtet):**
Urdu setzt Verbalphrase ans Ende. Bei Strings mit `<xliff:g>` am Satzanfang
(z. B. `churn_renews_on`) muss die Datumsphrase nach Urdu-Syntax umstrukturiert
werden. Ein naiver Uebersetzer wuerde die deutsche Wortstellung beibehalten —
das erzeugt unnatuerliche Urdu-Saetze.

**Worker-Empfehlung:** Der uebersetzung-Skill (`ur.md`) hat diese Feinheit
abgedeckt. Aber falls jemals ein Worker ohne Skill-Aufruf uebersetzen wuerde
(was BUG #14 verbietet, aber theoretisch passieren koennte), wuerde die
Qualitaet schlecht.

**Fix-Vorschlag:**
FIN-032 ("uebersetzung-Skill PFLICHT") muss in JEDEM Worker-Prompt prominenter
stehen — nicht nur als Floskel sondern mit Beispielen welche Sprach-Fallen ohne
Skill nicht abgedeckt waeren. Frueher Hinweis "ohne Skill = schlechte Qualitaet"
verhindert Faulheit.


---

## BUG #29 — Worker A (französisch) generierte unescapte Apostrophen → Build-Blocker

**Schweregrad:** 🟥 Hoch — Build bricht ab, App ist nicht baubar

**Symptom (Lauf 2026-05-22):**
Nach Worker A's französischen Übersetzungen schlug Android Build mit:
> `values-fr/strings.xml:1454: Failed to flatten XML for resource 'settings_premium_cancelled_desc' with error: Invalid unicode escape sequence in string`

Aktueller Wortlaut (FALSCH):
```xml
<string name="settings_premium_cancelled_desc">Tes fonctionnalités Premium restent disponibles jusqu'à la fin...</string>
<string name="settings_premium_cancelled_until">Abonnement Premium annulé — valable jusqu'au <xliff:g.../></string>
```

Sollte sein:
```xml
<string name="settings_premium_cancelled_desc">Tes fonctionnalités Premium restent disponibles jusqu\'à la fin...</string>
<string name="settings_premium_cancelled_until">Abonnement Premium annulé — valable jusqu\'au <xliff:g.../></string>
```

**Root Cause:**
Android XML in string-resources verlangt für nicht-quote-umschlossene Strings dass
Apostrophe mit `\'` escaped sind. Worker A hat das in seinem Worker-Prompt zwar
gesehen ("Apostrophen via `\'` escapen"), aber die Anweisung nicht konsequent
auf alle Apostrophen angewendet.

**Worker A's eigene Plugin-Beobachtung war:**
- Pre-Check Pflicht (BUG #21)
- Aber kein Hinweis auf den eigenen Apostroph-Fehler in französisch

**Verwandte Fehlerquellen geprüft:**
- Andere Sprachen (en, es, it, nl, pl): keine unescapten Apostrophen in den 11 neuen Strings
- Worker B (tr): türkische Apostrophen waren ein bekannter Bug (Memory:
  `feedback_uebersetzung_skill_immer_pflicht.md` Vorfall 2026-05-16). Geprüft: ok.
- Worker C-E: keine Apostrophen in CJK/RTL/Indisch nötig

**Workaround (dieser Lauf):**
Python-Script `fix_apostrophes.py` fixt unescapte Apostrophen in NEU eingefügten Strings.
Build erfolgreich nach Fix (14s).

**Fix-Vorschläge für Plugin:**
1. **Worker-Post-Validation:** Nach jeder Sprache automatisch checken ob unescapte
   Apostrophen in neuen Strings sind. `grep -E "[^\\]'" values-XX/strings.xml`.
2. **Worker-Prompt-Verstärkung:** Apostroph-Regel als CHECKLIST am ENDE des Workers
   wiederholen: "Bevor du Output schreibst, prüfe nochmal ob ALLE Apostrophe in
   romanischen Sprachen (fr/it/es/pt/ca) mit \' escaped sind."
3. **Plugin-Phase-3c-Pre-Check:** Vor dem Re-Audit-Build automatisch das `lint`-Build
   laufen lassen (oder mindestens `aapt2 compile --no-crunch`) um Translation-Fehler
   früher zu erkennen.
4. **uebersetzung-Skill-Validator:** `check_apostrophes.py` existiert bereits im Skill,
   sollte aber als verbindlicher Post-Step nach jeder romanischen Sprache laufen.

**Empfehlung:** Variante 1 + 4 kombinieren. Skill-Validator ist da, muss nur
verbindlich aufgerufen werden.


---

## ABSCHLUSS DES LAUFS (2026-05-22 17:18 GMT+2)

**Ergebnis:** 🟢 ERFOLGREICH

| Metrik | Wert |
|--------|------|
| openFindingsCount | 0 |
| auditedLanguages | 27 (de + 26 weitere) |
| translatable strings (DE-Referenz) | 1.104 |
| Vollstaendigkeit pro Sprache | 100% (alle 27 Sprachen) |
| Translation-Worker spawn | 5 + 2 (Recovery) |
| Build-Validation | assembleDebug SUCCESS (14s) |
| Plugin-Bugs in dieser Session | 13 (BUG #17-29) |

**Plugin-Bugs dokumentiert (Loop 4):**
| # | Schwere | Titel |
|---|---------|-------|
| 17 | 🟧 | Plugin-Cache veraltet: assets/ + schemas/ fehlen |
| 18 | 🟧 | Recht-Report wird durch Code-Aenderungen veraltet, kein Auto-Refresh |
| 19 | 🟨 | FIN-029 Worker-Count empfiehlt 15-20 auch bei Mini-Aufgaben |
| 20 | 🟨 | Plugin sollte "fehlende Strings pro Sprache" automatisch berechnen |
| 21 | 🟧 | Translation-Worker brauchen Pre-Check vor Insert (Duplikat-Risiko) |
| 22 | 🟨 | Translation-Worker Insert via `replace` ist fehleranfaellig (rfind besser) |
| 23 | 🟨 | pl hatte retro_month_more_entries als einzige Sprache fehlend |
| 24 | 🟧 | Plugin-Subagents brauchen PYTHONIOENCODING=utf-8 als Default |
| 25 | 🟧 | Worker brauchen Idempotenz-Guard nach Crash |
| 26 | 🟥 | Worker D crashte trotz FIN-005-Token-Cap (Autocompact-Thrashing) |
| 27 | 🟨 | ISO-639-1 vs. Android-Legacy-Code Mapping fehlt im Worker-Prompt |
| 28 | 🟨 | Worker E: Urdu-Wortstellung am Satzanfang (RTL-Bidi-Falle) |
| 29 | 🟥 | Worker A (franzoesisch) generierte unescapte Apostrophen → Build-Blocker |

**Zusaetzliche Beobachtungen:** #1 (Phase 0 zu viele Sub-Schritte), #2 (uebersetzung-Skill ohne batch-mode)

**Naechste Schritte (offen — fuer eigene Session):**
- Plugin-Cache-Refresh (Plugin-Manager neu installieren) → BUG #17 lösen
- FIN-029-Heuristik um Token-Estimate erweitern → BUG #26 vermeiden
- Apostroph-Validator in uebersetzung-Skill verbindlich machen → BUG #29 vermeiden
- Worker-Prompt-Template um Pre-Check + Idempotenz-Guard erweitern → BUG #21+25


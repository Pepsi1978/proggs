# finale Plugin — Bug-Liste Run 2026-05-22 (BestJournalAndroid)

> Diese Datei wird WAEHREND des Plugin-Laufs gefuellt. Jeder Fehler, jede Reibung,
> jedes "das haette das Plugin selbst machen sollen"-Moment kommt hier rein.
> Nach Lauf-Ende konsolidieren in `ISSUES.md`.

Lauf: `/finale:run` mit App-Root `~/proggs/BestJournalAndroid`
Effort: xhigh
Startzeit: 2026-05-22 ~14:53

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

(Weitere Bugs werden eingefuegt waehrend der Lauf fortschreitet.)

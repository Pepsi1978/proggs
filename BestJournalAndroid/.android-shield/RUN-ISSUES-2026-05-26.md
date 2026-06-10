# finale Plugin — RUN-ISSUES 2026-05-26 (3. Lauf)

> Auto-gefuehrt vom Orchestrator (FIN-047). Sammelt Plugin-Fehler, Schema-Drift,
> Worker-Crashes, unklare Anweisungen und `plugin_bugs_observed`-Eintraege aller Subagents.
> Lauf-ID: 76a1d285-ccf3-4773-ae6e-6b8f90e88e61 · gestartet 2026-05-26

| # | Phase/Subagent | Symptom | Evidence | Suggestion |
|---|----------------|---------|----------|------------|
| 1 | Orchestrator / run-status.json | `appRoot` wird mit Windows-Backslashes geschrieben; altes Feld war gemischt (`C:` + Backslash + `/proggs/...`) | Heredoc-Python bricht reproduzierbar an `\U` in `C:\Users`; tritt bei jeder Status-Datei-Schreibung auf | appRoot intern als Forward-Slash-Pfad normalisieren, bevor er in JSON/Bash/Python genutzt wird |
| 2 | Phase 1A Roentgen-Worker | Worker crasht mit "Prompt is too long" nach ~14 Min / 50 Tool-Calls, schreibt 0 Output (roentgen-report.json bleibt alte Version vom 18.05.) | Einzelner Subagent laedt den sehr grossen `app-roentgen`-Skill UND liest viele der 143 Kotlin-Dateien → Kontext-Ueberlauf. total_tokens:0, kein Report. | KRITISCH: Bei ktFileCount>50 Phase 1A NIE als einzelnen Skill-Load-Worker spawnen. FIN-029 Layer-Split mit fokussierten Grep/Read-Workern erzwingen, die den Skill NICHT komplett laden sondern scope-gezielt scannen. Orchestrator-Prompt muss "lade den Skill NICHT vollstaendig" explizit sagen. |
| 3 | Phase 1A Layer-Worker A (Marketing/HWG) | Auch der fokussierte Worker crasht mit "Prompt is too long" (16 Tool-Calls, 0 Output) trotz engem Scope | Anweisung war "lies values/strings.xml komplett" — bei 1117 Strings (~4000+ Zeilen) plus zusaetzlichem Kotlin-Composable-Grep sprengt schon das den Worker-Kontext | Marketing/HWG-Scan NIE per Vollread der strings.xml. Nur gezieltes Grep nach HWG-/Paywall-Schluesselwoertern (Treffer-Zeilen). Worker-Prompt darf "lies strings.xml" nicht enthalten, wenn die Datei >300 Strings hat. |
| 4 | Layer-Worker B + C | 3 wiederkehrende Python/Bash-Bugs gemeldet (cp1252-Encoding, Backslash-Pipe SyntaxWarning, bash-EOF bei grossem Inline-Skript) | Beide Worker meldeten dieselben Encoding-/Heredoc-Probleme; cp1252 + bash-EOF je 100%-Match in Bug-DB | Worker-Template sollte PYTHONIOENCODING=utf-8 erzwingen UND grosse Python-Skripte als Datei schreiben statt Inline-Heredoc (vermeidet bash-EOF + Backslash-Escape) |
| 5 | String-Bucket-Worker 3 (Z.785-1145) | Crash "Prompt is too long" (total_tokens:0) trotz gleicher Zeilenzahl wie Buckets 1/2/4, die durchliefen | Alle erfolgreichen Worker endeten bei ~170-174k total_tokens → Subagent-Kontextlimit ~175k. Bucket 3 deckt Paywall/Onboarding-Strings ab (besonders lange Werte) → Read sprengte das Limit sofort | KRITISCH fuer FIN-039: Bucket-Groesse NICHT nach Zeilenzahl, sondern nach Bytes/geschaetzten Tokens. Bei langen-String-Bereichen (Onboarding/Paywall/Legal) max ~80-100k Read-Budget pro Worker (Sicherheitsmarge zum 175k-Limit). Orchestrator sollte vor Bucket-Split `wc -c` pro Bereich pruefen, nicht `wc -l`. |
| 6 | ROOT CAUSE aller Worker-Crashes | Bucket 3a (nur 180 Zeilen!) crashte ebenfalls "Prompt is too long" | ECHTE URSACHE: Subagents erben den kompletten injizierten Kontext (CLAUDE.md + alle ~/.claude/rules/*.md + auto-memory + MCP-Instruktionen). Dieser Sockel ist hier sehr gross (zig-K Token) und belegt den Grossteil des ~175k-Subagent-Limits BEVOR der Worker irgendetwas liest. Effektiv nutzbar bleiben nur ~50-70k. Darum crashen schon kleine Reads von langen-String-Bereichen. | FIX-Kandidat (Direktive 3, nach diesem Lauf + Researcher): (a) finale-Worker mit minimalem System-Kontext spawnen (Regel-Injektion fuer reine Daten-Worker reduzieren), (b) Bucket-Budget auf ~40-50k Read begrenzen statt 80-100k, (c) Orchestrator macht kritische Lange-String-Bereiche selbst statt per Subagent. Researcher: "wie scant man strings.xml mit 10k-100k Strings ohne Kontext-Ueberlauf" (Map-Reduce mit Mini-Chunks, Streaming, jq/python-Vorfilterung statt LLM-Vollread). |

## KRITISCH (2026-05-26) — Format-String-%-Escaping + Platzhalter-Erhalt bei Uebersetzungen

**Fund:** 2 Strings (ai_prompt_custom_intro, profile_style_custom) werden via
`getString(R.string.x, arg)` = String.format aufgerufen. In 9 Sprachen hatten fruehere
Uebersetzungslaeufe literale `%` eingefuegt ("50%" statt "50 Prozent") und bei it den
Platzhalter `%1$s` zu `%1` verstuemmelt. Folge: IllegalFormatException = LAUFZEIT-CRASH
beim Custom-Profil/Custom-Retrospektive in diesen Sprachen. Vorbestehend (nicht in den 22
geaenderten Strings), beim Markteintritt-Audit gefunden — der Build meldete es nur als
WARNING ("Multiple substitutions / formatted=false"), nicht als Fehler, waere also fast
durchgerutscht.

**FIX-Kandidat (Direktive 3, Plugin-Verbesserung) — Format-String-Verifier:**
1. Der uebersetzung-Skill UND der finale-Post-Verifier muessen fuer JEDEN String, der im
   Code via `getString(id, arg...)` / `String.format` / `MessageFormat` verwendet wird,
   pruefen: (a) literale `%` sind als `%%` escaped (kein nacktes % das kein Format-Specifier
   ist), (b) die positionalen Platzhalter (`%1$s`, `%2$d`) sind in EXAKT gleicher Anzahl wie
   im DE-Original vorhanden und nicht verstuemmelt (z.B. `%1` ohne `$s`).
2. Worker-Prompt-Regel ergaenzen: "Bei Strings mit Format-Argument: literale Prozentzeichen
   ausschreiben (Prozent/percent/...) ODER als %% escapen. Platzhalter %1$s exakt uebernehmen,
   niemals das $s/$d weglassen."
3. Die `aapt`-Warnung "Multiple substitutions specified in non-positional format" NIE ignorieren
   — sie ist ein verlaesslicher Indikator fuer genau diesen Crash-Bug. Plugin soll sie als
   Finding behandeln, nicht als harmlose Warning.
4. Verifier muss MULTILINE-Strings erfassen (it profile_style_custom war ueber mehrere Zeilen
   verteilt → zeilen-basierte Regex-Fixes verfehlten ihn). IMMER XML-Parser (ElementTree) statt
   zeilen-basiertem Grep fuer die Wert-Extraktion nutzen.

## FALSE-POSITIVE (Frank 2026-05-26) — Recht-Worker erkannte Verweis-Architektur nicht

Der DSGVO-Recht-Worker (recht-frag-dsgvo) bewertete Finding C1 als "🟥 HIGH: 22 von 27 Sprachen
fehlt die Datenschutz-Erweiterung / keine gueltige Art.13/14-Unterrichtung". Das war FALSCH.
Verifiziert: Die 22 "kleinen" PRIVACY.html (5-10 KB) sind bewusste Kurz-Zusammenfassungen in
Landessprache, die rechtsverbindlich auf die VOLLVERSIONEN (de/DATENSCHUTZ.html, en/PRIVACY.html)
verweisen ("Bei Widerspruch gelten die Vollversionen"). Alle 22 verweisen korrekt (je 2x EN + 2x DE).
Nur DE/EN/KO (+pt-BR/ja) sind Vollversionen — und die haben alle 3 neuen Abschnitte (Art.9/Standort/Churn).

**Fehler-Auswirkung:** Haette zu einer voellig unnoetigen Uebersetzung von ~3 langen DSGVO-Abschnitten
in 22 Sprachen gefuehrt (riesiger Aufwand + juristisches Risiko). Frank erkannte es, nicht das Plugin.

**FIX-Kandidat (Direktive 3, Plugin-Verbesserung):** Bevor der Recht-Worker ein "Dokument fehlt/
unvollstaendig"-Finding (Kategorie missingDocs/DSGVO) ausgibt, MUSS er pruefen ob das Dokument eine
Verweis-/Summary-Datei ist: (a) Byte-Groesse << Vollversion UND (b) enthaelt Verweis-Links auf eine
Vollversion in anderer Sprache (`legal/<lang>/`-href). Wenn ja: Finding herabstufen auf "Summary
verweist auf Vollversion — pruefen ob Verweis lebt", NICHT als fehlende Unterrichtung eskalieren.
Das ist eine Erweiterung von FIN-007 (Assets-Inventar-Abgleich) um Summary-Architektur-Erkennung.

## KRITISCH (Frank 2026-05-26) — Uebersetzungs-Parallelitaet: Continuous-Spawning + Rate-Limit

**Bug:** 15 gleichzeitige Opus-Uebersetzungs-Worker loesten "API Error: Server is temporarily
limiting requests (not your usage limit) · Rate limited" aus. Nur 7 von 15 liefen durch
(en/es/fr/it/nl/pt-PT/tr), 8 brachen mit total_tokens:0 ab. Der Lauf wirkte "schnell fertig",
war aber zur Haelfte gescheitert — gefaehrlich, weil unbemerkt ohne harte Verifikation.

**Finale Zahl (Frank 2026-05-26): GENAU 7 gleichzeitige Worker** — bei 8 wurde der 8. rate-limited
(nur 7 liefen durch, in 2 Wellen je 1 Crash). 7 ist die stabile Obergrenze fuer parallele Opus-
Uebersetzer ohne Server-Drosselung. Plugin-Default fuer Phase 3b: maxConcurrent=7.

**Frank-Wunsch (3x betont) — echtes CONTINUOUS-SPAWNING statt Wellen:**
Aktuell wartet der Orchestrator bis ALLE 15 Worker einer Welle fertig sind, bevor die naechsten
starten. Eine einzige langsame Sprache blockiert dann alle anderen. Gewuenscht: Sobald 1 Worker
fertig ist, SOFORT die naechste noch nicht uebersetzte Sprache als Ersatz starten, sodass IMMER
15 gleichzeitig laufen (15 fertig -> 14 laufen -> sofort 15. nachschieben). Spart massiv Zeit.

**FIX-Kandidat (Direktive 3, Plugin-Verbesserung):**
1. Continuous-Spawning im VORDERGRUND implementieren (Frank-Korrektur 2026-05-26: KEIN
   run_in_background/Hintergrund-Tool — Frank: "muss im Vordergrund laufen, sonst funktioniert
   das Nachschieben nicht" + Sichtbarkeits-Regel). Mechanik: kleinere ueberlappende Vordergrund-
   Bloecke statt einer grossen Welle, sodass sobald Kapazitaet frei wird die naechste Sprache
   sichtbar nachgeschoben wird — nie auf den langsamsten Worker einer Welle warten.
2. Rate-Limit-Schutz: 15 gleichzeitige Opus-Worker ist zu viel fuer den Server. Entweder
   (a) gleichzeitige Anzahl auf ~8-10 begrenzen, ODER (b) exponential-backoff-Retry bei
   "Rate limited" (der FIN-015-Pseudocode hat das bereits — wurde hier aber nicht angewandt,
   weil foreground-Spawn keinen Retry-Mechanismus hat), ODER (c) Sonnet fuer einfache Sprachen.
3. HARTE Verifikation PFLICHT nach jeder Welle: git status (welche Dateien wirklich geaendert) +
   XML-Valide + 22/22-Keys-Check + "noch=DE"-Check. NIE auf Worker-Erfolgsmeldung allein verlassen
   (en/fr/nl meldeten "Rate limited", hatten aber tatsaechlich fertig geschrieben; umgekehrt koennten
   Worker Erfolg melden ohne vollstaendig zu sein).

**Frank-Verschaerfung (2026-05-26, MEHRFACH betont — ABMAHNRISIKO):** Die Nach-Verifikation ist
NICHT optional und NICHT "am Ende mal schauen". Nach JEDER uebersetzten Sprache MUSS automatisch
geprueft werden: (a) sind alle Ziel-Keys vorhanden, (b) ist KEINER mehr identisch mit dem DE-Original
(= nicht uebersetzt), (c) XML valide, (d) Platzhalter/xliff:g erhalten. Wenn auch nur 1 String
unuebersetzt durchrutscht, kann die App in dieser Sprache abgemahnt werden (z.B. deutscher Rechtstext
in franzoesischer App). Das Plugin MUSS einen verpflichtenden Post-Translation-Verifier haben, der
jede Sprache gegen diese 4 Checks faehrt und FEHLER LAUT meldet (nicht still durchlaufen lassen).
Frank-Zitat: "Nicht, dass manche Sachen nicht uebersetzt werden, denn dann kann man abgemahnt werden."

**ru-Crash (2026-05-26):** Auch in der 8er-Welle crashte 1 Worker (ru) mit "Prompt is too long" —
kyrillische Lang-Strings (profile_insight_long, privacy_gate_gemini_body) + Skill-Last. Bestaetigt:
selbst Einzelsprach-Worker brauchen Kontext-Schutz (Skill nur gezielt via references/languages/<lang>.md,
nie ganzer Skill-Vollload; Ziel-Datei nur per Python, nie Read-Tool). Bei Crash: Sprache sofort
neu starten, NICHT die ganze Welle blockieren lassen.

## BESTAETIGTES LEARNING (Frank 2026-05-26) — Bucket-Schwarm fuer grosse String-Dateien

Die Bucket-Methode (Datei in feste Zeilen-/Byte-Bereiche teilen, je 1 Worker, der NUR seinen Bereich liest) hat fuer die grosse strings.xml (1508 Zeilen / 177 KB / 1117 Strings) zuverlaessig funktioniert, wo der Monolith-Worker crashte. Frank-Wunsch: als festes Plugin-Pattern einarbeiten.

**Konkretes Pattern fuer FIN-029/FIN-039-Erweiterung (Phase 1A String-Audit):**
1. Vor Split: `wc -c` pro geplantem Bereich (NICHT `wc -l`) — Byte-/Token-Budget entscheidet, nicht Zeilen. Lange-String-Bereiche (Onboarding/Paywall/Legal/KI-Prompts) brauchen kleinere Buckets.
2. Ziel-Budget pro Bucket: ~40-50k Token Read (Sicherheitsmarge zum ~175k-Subagent-Limit, da Regel-Sockel ~100k+ frisst).
3. So viele Buckets parallel starten wie moeglich, dann CONTINUOUS-SPAWNING: sobald ein Bucket-Worker fertig meldet, sofort den naechsten Bereich als Worker starten — keine starren Wellen.
4. Skaliert auf beliebige Groesse: 10.000 Strings → ~30-40 Buckets, 100.000 → ~300-400 Buckets, immer N parallel mit Nachschub. Kein Vollread, kein Crash.
5. Fallback: Bereiche die selbst als Mini-Bucket noch crashen (extrem lange Einzelstrings) macht der Orchestrator selbst per gestueckeltem Read (offset/limit < 25k Token).
6. Researcher-Auftrag (offen, nach diesem Lauf): Map-Reduce-/Streaming-Ansaetze fuer LLM-Audit sehr grosser i18n-Dateien recherchieren; ideale parallele Worker-Anzahl ermitteln.

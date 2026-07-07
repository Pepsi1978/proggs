# finale Plugin - RUN-ISSUES 2026-05-31

> Auto-generiert vom Orchestrator (FIN-047). Lauf: BestJournalAndroid v0.21.9, Modus default.
> Ergebnis: 1 MITTEL-Finding (D1) gefixt, openFindingsCount=0, Build erfolgreich.

| # | Quelle | Symptom | Evidence | Vorschlag |
|---|--------|---------|----------|-----------|
| 1 | Roentgen-Worker R2/R3/R4/R5 | "Prompt is too long", 0 Output, 30-37 tool_uses | 4 von 7 Workern crashten via FIN-048-Kontextueberlauf. Regel-Sockel ~100k + explorative Greps ueber 142 kt-Dateien sprengen das ~175k-Subagent-Limit. Nur R1 (9 tool_uses) ueberlebte. | Bei explorativer Code-Analyse (Roentgen Layer 3-7) den Orchestrator-Selbst-Scan als DEFAULT nutzen statt Worker zu spawnen. Deterministische Skripte (export-json.py, feature-scan.sh) + gezielte Orchestrator-Greps sind robuster. Worker nur fuer abgegrenzte Einzeldateien (<500 Zeilen). |
| 2 | Orchestrator (Bash) | Exit 2 "unexpected EOF" bei Python-Heredoc | Heredoc mit Apostrophen/Paragraph-Zeichen/Umlauten bricht Shell-Quoting (Bug-Case 100% Match). | Komplexe Python-Skripte IMMER per Write-Tool in .py-Datei (gitignored .android-shield/), dann python3 datei.py. Nie inline-Heredoc bei Sonderzeichen. |
| 3 | Vorheriger Audit (2026-05-27) | False-Negative: meldete "356-Checkbox vorhanden" | Der alte Audit prueffte nur ob paywall_consent_dialog_* in strings.xml EXISTIERT, nicht ob im Code REFERENZIERT. Tatsaechlich tote Strings seit v0.17.2. | Bei jeder "Feature vorhanden"-Behauptung Code-Referenz pruefen (grep R.string.KEY in .kt), nicht nur String-Existenz in strings.xml. Tote-String-Check als Pflichtschritt. |

## Positive Beobachtungen
- FIN-038 Stale-Check funktionierte: 4 alte HOCH/MITTEL-Findings als bereits geloest erkannt (kein Doppel-Fix).
- FIN-050 Verweis-Architektur-Erkennung korrekt: 23 Verweis-PRIVACY.html nicht faelschlich als "fehlend" eskaliert.
- Cross-Lingual-Stichprobe bestaetigte: rechtlich kritische Strings in allen Sprachen uebersetzt (kein DE-Durchrutschen).

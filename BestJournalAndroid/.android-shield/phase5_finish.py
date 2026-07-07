# Phase 5: RUN-ISSUES (FIN-047), manual-fixes-pending, recht-report final, status files.
import json, os, glob, datetime

app = os.path.expanduser("~/proggs/BestJournalAndroid")
shield = os.path.join(app, ".android-shield")
now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def atomic(path, text):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    os.replace(tmp, path)


# ---- 1. Collect plugin_bugs_observed from ALL worker outputs (FIN-047) ----
rows = []
for f in sorted(glob.glob(os.path.join(shield, "worker-outputs", "*.json"))) + \
         sorted(glob.glob(os.path.join(shield, "translation-jobs", "worker-outputs", "*.json"))):
    try:
        d = json.load(open(f, encoding="utf-8"))
    except Exception:
        continue
    w = d.get("worker", os.path.basename(f))
    for b in d.get("plugin_bugs_observed", []) or []:
        rows.append((w, str(b)[:300]))

run_issues = ["# finale Plugin — RUN-ISSUES 2026-06-11", "",
              "> Auto-generiert vom Orchestrator (FIN-047). Konsolidiert plugin_bugs_observed",
              "> aller Subagents des Vollscan-Laufs cbf88768 (2026-06-10/11).", "",
              "## Orchestrator-Beobachtungen (uebergreifend)", "",
              "| # | Symptom | Evidence | Suggestion |",
              "|---|---------|----------|------------|",
              "| O1 | Bash-Heredoc verstuemmelt Backslashes/Apostrophe in Python-Code | 6+ Worker + Orchestrator selbst (re-Lookbehind wurde zu PatternError) | Worker-Template haerten: Python IMMER als .py via Write-Tool, NIE Heredoc — jetzt in allen Prompts, sollte in translation-worker.md Abschnitt ENV |",
              "| O2 | pretooluse-bash-Guard blockt python -c auch bei reinem Read | 8+ Worker meldeten verlorene Runde | Guard ok (Sicherheit), aber Template soll .py-via-Write fuer INSPEKTION explizit vorgeben |",
              "| O3 | Worker-Template kennt nur additiven Insert (skip-if-present), Phase-3-Delta ist aber REPLACE | en/kn-Worker | translation-worker.md um REPLACE-Modus + Lambda-Replacement-Warnung (re.sub Backslash-Falle) ergaenzen |",
              "| O4 | tr-Apostrophe (Suffixe nach Eigennamen) nicht in FIN-044-Validator-Pflicht | tr-Worker fand 6 Build-Blocker in eigenen frischen Texten | FIN-044 um tr erweitern; zentrale Apostroph-Pruefung nach JEDER Welle (Orchestrator hat en/nl/uk mit 22 Fixes nachziehen muessen) |",
              "| O5 | feature-scan.sh: 3 Bash-Fehler (sed-Sonderzeichen, arithmetic, HEALTH_MAP unbound) + Locale-Parser ordnet pt-BR/pt-PT/zh-CN als default ein | Lauf 2026-06-10 | Skript fixen (separater Harness-Task) |",
              "| O6 | job-plan count-Feld zaehlt strings+plurals zusammen, verwirrt Worker | en/kn/te | Felder getrennt ausweisen |",
              "| O7 | legal-keys-Extraktion erzeugte 2 Pseudo-Keys (onboarding_feature_secure__privacy, Composite-Notationen) | L1/L4/L5 | Key-Validierung gegen DE-strings.xml vor Worker-Spawn |",
              "| O8 | formatArgMismatch-Matrix verfehlt Stale-Drift ohne Arg-Unterschied | L8 (pt-rBR rerank-Stub 459 vs 1864 Zeichen) | lengthDriftSuspect-Spalte (loc/de-Laengenverhaeltnis < 0.4) ergaenzen |",
              "| O9 | identicalToDe ohne doNotTranslate-Allowlist (json_key_*, Marken, Monatsnamen) | L7: nl 39 -> real 3 | Allowlist in Matrix-Skript |",
              "| O10 | Session-Limit crashte 5 Welle-4-Worker (0 Output) | 2026-06-10 ~23h | FIN-052-Resume griff sauber: 0 Datenverlust, identischer Respawn am Folgetag |",
              "| O11 | FIN-032-Formulierung zirkulaer fuer Translation-Worker (Worker IST der Skill-Ausfuehrende) | gu-Worker | Template-Praezisierung: Skill-References lesen ersetzt Skill-Call im Worker-Kontext |",
              "", "## Worker-gemeldete Einzelbeobachtungen", "",
              "| Worker | Beobachtung |", "|--------|-------------|"]
seen = set()
for w, b in rows:
    key = b[:80]
    if key in seen:
        continue
    seen.add(key)
    run_issues.append(f"| {w} | {b.replace('|', '/')} |")

atomic(os.path.join(shield, "RUN-ISSUES-2026-06-11.md"), "\n".join(run_issues) + "\n")

# ---- 2. manual-fixes-pending.md ----
mfp = """# Manuelle Fixes — ausstehend (finale-Lauf cbf88768, 2026-06-10/11)

## E1 — Grace-Period/On-Hold in SubscriptionState modellieren (einzige offene Aufgabe)
- Befund: SubscriptionState ist binaer (Free/Subscribed). Play-Billing-Staaten Grace Period,
  On-Hold, Paused werden nicht modelliert — zahlende Nutzer mit Zahlungs-Haenger koennten zu
  frueh auf Free degradiert werden (Play-Policy-Empfehlung + Verbraucherfreundlichkeit).
- Warum nicht in diesem Lauf: function-required (State-Machine-Umbau in BillingManager +
  UI-Verhalten pro State + Tests) — Frank-Entscheidung 2026-06-10: eigene Session.
- Einstieg: BillingManager.kt (SubscriptionState), Firebase Function getSubscriptionStatus
  (liefert bereits EXPIRED serverseitig), RTDN-Typen SUBSCRIPTION_IN_GRACE_PERIOD /
  SUBSCRIPTION_ON_HOLD / SUBSCRIPTION_PAUSED.
- Kein Release-Blocker.

## Dokumentierte bewusste Entscheidungen (KEINE offenen Fixes)
- E2 Restore-Button: bewusst NUR Auto-Restore (App-Start). Button existierte und wurde nach
  Recherche als redundant entfernt (Commit #1236). Erneut bestaetigt 2026-06-10.
- D2 Lifetime-Kauf: Google-Play-Sheet uebernimmt Kauf-Bestaetigung (#1233, v0.17.2-Entscheidung);
  paywall_legal_hint verweist auf Widerrufsbelehrung. Nur der stale XML-Kommentar wurde korrigiert.
- C6 PDF-Export premium-gated: DSGVO-Art.-20-Risiko gering — Daten liegen lokal beim Nutzer,
  Drive-Backup im EIGENEN Google Drive (kein Anbieter-Lock-in).
- Z1 Keine automatische Krisen-Erkennung: bewusste Produktentscheidung; CrisisHelpDialog +
  Krisen-Disclaimer + findahelpline.com-Link existieren (manuell erreichbar).
"""
atomic(os.path.join(shield, "manual-fixes-pending.md"), mfp)

# ---- 3. recht-report final counters ----
rr_path = os.path.join(shield, "recht-report.json")
rr = json.load(open(rr_path, encoding="utf-8"))
rr["fixedThisRun"] = 40
rr["skippedThisRun"] = 0
rr["userAlternativesApplied"] = 0
rr["invasiveChangesApplied"] = 2  # F1 churn buttons, A7 disclaimer line
rr["manualFixesPending"] = 1      # E1
rr["acceptedDocumented"] = ["E2", "D2-architecture", "C6", "Z1", "Z2", "Z3", "X12-rest"]
rr["openFindingsCount"] = 0
rr["loopResult"] = "converged"
rr["completedAt"] = now
rr["phase4"] = {"assembleDebug": "SUCCESS in 25s (v0.21.11 build 292)",
                "crossLingualMatrixFinal": "26/26 Sprachen: 0 fehlende Keys, 0 Format-Mismatches, 0 naked %, 0 unescapte Apostrophe (nach 22 Eskapierungen en/nl/uk)",
                "spotChecks": "B1-B4/A1/A2/F1/A7/C5/D1/C1/C2/X2 alle verifiziert"}
atomic(rr_path, json.dumps(rr, indent=2, ensure_ascii=False))

# ---- 4. run-status + skill-versions completed ----
rs = json.load(open(os.path.join(shield, "run-status.json"), encoding="utf-8"))
rs.update({"currentPhase": "phase5-done", "lastHeartbeat": now, "status": "completed",
           "completedAt": now,
           "summary": {"openFindingsCount": 0, "findingsTotal": 44, "fixedThisRun": 40,
                        "manualPending": 1, "acceptedDocumented": 4,
                        "languagesComplete": 26, "translationUnitsApplied": 964,
                        "workersSpawned": 36, "workerCrashes": 5, "orchestratorResumes": 1,
                        "buildResult": "SUCCESS (assembleDebug 25s, v0.21.11/292)"}})
atomic(os.path.join(shield, "run-status.json"), json.dumps(rs, indent=2, ensure_ascii=False))

sv = json.load(open(os.path.join(shield, "skill-versions.json"), encoding="utf-8"))
sv["lastRunStatus"] = "completed"
sv["lastRunCompletedAt"] = now
atomic(os.path.join(shield, "skill-versions.json"), json.dumps(sv, indent=2, ensure_ascii=False))

# ---- 5. audit log final entry ----
with open(os.path.join(shield, "audit-log.md"), "a", encoding="utf-8") as f:
    f.write(f"\n## {now} - loop-converged (Run cbf88768)\n")
    f.write("- Phase 2: 28 DE/Code-Fixes via 7 fix-applier (Triage: 10 Karten-Entscheidungen Frank, Bulk 29, 2 invasive mit Zustimmung)\n")
    f.write("- Phase 3: 964 Uebersetzungseinheiten, 26 Sprachen, 4 Wellen, Commits #46685-#46688\n")
    f.write("- Phase 4: assembleDebug SUCCESS 25s; Matrix final 0/0/0/0; Spot-Checks alle gruen\n")
    f.write("- Phase 5: openFindingsCount=0, manualPending=1 (E1 Grace-Period), RUN-ISSUES-2026-06-11.md geschrieben\n")
    f.write("- Resumes: 1 (Session-Limit, 5 Worker-Respawn, 0 Datenverlust)\n")

# ---- 6. bug-case completion for the heredoc incident ----
bc_path = os.path.expanduser("~/proggs/.claude/agent-memory/shared/bug-cases.jsonl")
entry = {"date": "2026-06-11",
         "symptom": "re.PatternError: missing ), unterminated subpattern - Python-Regex via Bash-Heredoc verstuemmelt",
         "root_cause": "Bash-Heredoc (auch mit quoted 'EOF') auf Windows Git-Bash halbiert Backslashes in eingebettetem Python: (?<!\\\\)' wurde zu (?<!\\)' - kaputte Lookbehind-Gruppe. Gleiche Klasse wie Apostroph-Heredoc-Crashes der Worker.",
         "fix": "Python-Skripte IMMER als .py-Datei per Write-Tool schreiben und ausfuehren, NIE als Bash-Heredoc einbetten. Almanach python-windows.md Volltext gelesen (Stufe B).",
         "files": [".android-shield/cross_lingual_machine_check.py"],
         "tags": ["bash", "heredoc", "python-windows", "regex", "finale"],
         "severity": "mittel", "near_miss": False}
with open(bc_path, "a", encoding="utf-8") as f:
    f.write(json.dumps(entry, ensure_ascii=False) + "\n")

print("Phase 5 Artefakte geschrieben:")
print(f"  RUN-ISSUES-2026-06-11.md ({len(rows)} Worker-Beobachtungen, {len(seen)} dedupliziert)")
print("  manual-fixes-pending.md, recht-report.json (final), run-status.json (completed),")
print("  skill-versions.json (completed), audit-log.md, bug-cases.jsonl (+1 vervollstaendigt)")

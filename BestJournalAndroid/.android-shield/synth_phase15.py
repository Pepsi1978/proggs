# Phase 1.5 synthesis: merge all DE legal findings + cross-lingual bundles into recht-report.json
import json, os, glob, datetime

shield = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield")
wo = os.path.join(shield, "worker-outputs")


def load(name):
    p = os.path.join(wo, name)
    if not os.path.exists(p):
        return {}
    with open(p, encoding="utf-8") as f:
        return json.load(f)


r1 = load("phase1b-recht-dsgvo.json")
r2 = load("phase1b-recht-uwg.json")
r3 = load("phase1b-recht-abo.json")
r4 = load("phase1b-recht-hwg-ai.json")
urls = load("phase1b-urls.json")
codev = load("phase1b-code-verify.json")

de_findings = []
for src in (r1, r2, r3, r4):
    de_findings.extend(src.get("findings", []))

# Cross-lingual systemic bundles (X-IDs), synthesized from the 8 L-worker reports.
xling = [
    {
        "findingId": "X1", "riskLevel": "🟥", "category": "DSGVO/Irreführung Account-Löschung",
        "language": "alle außer de/en", "jurisdiction": "alle Zielmärkte",
        "file": "values-*/strings.xml", "stringKeys": ["settings_delete_account_subtitle", "settings_delete_account_confirm_body"],
        "currentText": "Übersetzungen behaupten, das GOOGLE-KONTO werde gelöscht — aktuelles DE sagt ausdrücklich das Gegenteil ('dein Google-Konto bleibt bestehen'); confirm_body nennt 'Firebase-Konto' statt App-Anmeldung und lässt Audio-Aufnahmen aus der Löschliste weg",
        "rationale": "Faktisch falsche Aussage über den Löschumfang (Art. 17 DSGVO Transparenz + Play Account-Deletion-Policy + Irreführung). Stale-Drift: DE wurde nach der Übersetzung geändert.",
        "invasivityLevel": "text-only",
        "fixPlan": "Beide Keys per übersetzung-Skill aus aktuellem DE in alle betroffenen Sprachen neu übersetzen (Phase 3).",
        "affectedLanguages": "~24 (alle außer en; de ist Quelle)",
        "impactWithoutFix": "Nutzer glauben, ihr Google-Konto werde gelöscht — irreführende Datenlösch-Zusage, abmahn- und Play-Policy-relevant.",
    },
    {
        "findingId": "X2", "riskLevel": "🟥", "category": "PAngV/Preisangabe",
        "language": "alle außer de", "jurisdiction": "alle Zielmärkte",
        "file": "values-*/strings.xml", "stringKeys": ["paywall_from_per_day"],
        "currentText": "DE (aktuell): 'Jahresabo, %1$s pro Jahr' — Übersetzungen (stale): 'pro Tag'-Semantik. %1$s wird mit dem JAHRESPREIS gefüllt.",
        "rationale": "In den Übersetzungen erscheint der Jahrespreis als Tagespreis ('39,99 € pro Tag') — krasse Preis-Irreführung (PAngV, UWG §5). Key-Name 'per_day' belegt die alte Semantik.",
        "invasivityLevel": "text-only",
        "fixPlan": "Key in ALLEN 26 Sprachen aus aktuellem DE neu übersetzen (Phase 3). DE selbst ist korrekt.",
        "affectedLanguages": "bis zu 26 — pro Sprache verifizieren, Neuübersetzung deckt alle ab",
        "impactWithoutFix": "Falsche Preisangabe auf der Paywall in Fremdsprachen — hohes Abmahnrisiko.",
    },
    {
        "findingId": "X3", "riskLevel": "🟧", "category": "BGB §356a/Widerruf",
        "language": "fast alle außer de/en", "jurisdiction": "DE/EU-Nutzer in Fremdsprachen",
        "file": "values-*/strings.xml", "stringKeys": ["settings_revoke_subtitle", "settings_revoke_confirm_title", "settings_revoke_confirm_body"],
        "currentText": "subtitle: 'Premium-Kauf' statt '§ 356a BGB — Widerruf direkt per App'; confirm-Texte beschreiben den alten E-Mail-App-Flow statt Direktversand",
        "rationale": "Der gesetzliche Widerrufs-Einstieg ist in Fremdsprachen falsch beschriftet/beschrieben — Auffindbarkeit des Widerrufsrechts leidet (§ 356a BGB).",
        "invasivityLevel": "text-only",
        "fixPlan": "3 Keys per übersetzung-Skill neu übersetzen (Phase 3).",
        "affectedLanguages": "~24",
        "impactWithoutFix": "Widerrufs-Funktion schwer auffindbar; Beschreibung entspricht nicht dem realen Flow.",
    },
    {
        "findingId": "X4", "riskLevel": "🟧", "category": "DSGVO/CCPA Consent-Texte stale",
        "language": "fast alle außer de/en", "jurisdiction": "USA-CA + EU",
        "file": "values-*/strings.xml", "stringKeys": ["consent_toggle_do_not_sell_body", "privacy_gate_tts_title", "privacy_gate_tts_body", "privacy_gate_groq_body"],
        "currentText": "do_not_sell: stark gekürzt + erfundenes Jahr '2026', 'verkauft Daten nirgendwo' fehlt; privacy_gate_groq/tts: ältere Fassung, SCC-Rechtsgrundlage fehlt",
        "rationale": "Stale Consent-/Transparenztexte: Drittland-Rechtsgrundlage (SCC) und CCPA-Aufklärung unvollständig in den Übersetzungen (Art. 13/44 DSGVO, CCPA).",
        "invasivityLevel": "text-only",
        "fixPlan": "4 Keys per übersetzung-Skill neu übersetzen (Phase 3).",
        "affectedLanguages": "~24",
        "impactWithoutFix": "Unvollständige Datenschutz-Aufklärung in Fremdsprachen.",
    },
    {
        "findingId": "X5", "riskLevel": "🟧", "category": "i18n-Vollständigkeit",
        "language": "ALLE 26", "jurisdiction": "alle",
        "file": "values-*/strings.xml", "stringKeys": ["datetime_months_relative", "datetime_years_relative"],
        "currentText": "'vor %1$d Monat(en)/Jahr(en)' — unübersetztes Deutsch in allen 26 Sprachen (minutes/hours/days sind korrekt übersetzt)",
        "rationale": "Nutzer-sichtbare relative Datumsangaben erscheinen in jeder Sprache deutsch — Qualitäts-/Professionalitätsmangel, in Fremdsprach-Märkten unschön (kein hartes Rechtsrisiko, aber sichtbar).",
        "invasivityLevel": "text-only",
        "fixPlan": "2 Plurals × 26 Sprachen per übersetzung-Skill (Phase 3).",
        "affectedLanguages": "26/26 (maschinell verifiziert)",
        "impactWithoutFix": "Deutsche Textfetzen in allen Fremdsprachen-UIs.",
    },
    {
        "findingId": "X6", "riskLevel": "🟧", "category": "KI-Prompt-Integrität",
        "language": "pl, nl, pt-rBR, hi, in", "jurisdiction": "—",
        "file": "values-pl|nl|pt-rBR|hi|in/strings.xml", "stringKeys": ["ai_prompt_custom_schema", "ai_prompt_rerank_system", "ai_prompt_rerank_entries_header", "ai_prompt_rerank_actions_header", "ai_prompt_rerank_user_focus_header"],
        "currentText": "pl/nl/pt-rBR: massiv veraltete Prompt-Strukturen (z.B. 1520 vs 4463 Zeichen, literales %1$s an Gemini, pt-rBR mit rohen Newlines); hi/nl/pl/in: rerank-Header unübersetzt deutsch",
        "rationale": "Veraltete KI-Prompts liefern andere/schlechtere Analysen in diesen Sprachen + literales %1$s im Prompt; kein Crash (nur %1$s, kein %d), aber Funktions-Drift.",
        "invasivityLevel": "text-only",
        "fixPlan": "Betroffene Keys pro Sprache aus aktuellem DE neu übersetzen (Phase 3); pt-rPT als Vorlage für pt-rBR-rerank.",
        "affectedLanguages": "pl(6 Keys), nl(1), pt-rBR(1), hi(3 Header), in(2 Header)",
        "impactWithoutFix": "KI-Qualität in 5 Sprachen degradiert; %1$s-Artefakte in Prompts/UI-Labels.",
    },
    {
        "findingId": "X7", "riskLevel": "🟧", "category": "UI-Format-Artefakt",
        "language": "pl", "jurisdiction": "—",
        "file": "values-pl/strings.xml", "stringKeys": ["dashboard_profile_focus_summary", "dashboard_profile_focus_entropy", "dashboard_profile_focus_goals", "dashboard_profile_focus_insight"],
        "currentText": "4 Dashboard-UI-Labels enthalten literales %1$s (DE-aktuell hat keine Format-Args) — UI zeigt '%1$s' an polnische Nutzer",
        "rationale": "Sichtbares Format-Artefakt im UI (Qualität).",
        "invasivityLevel": "text-only",
        "fixPlan": "4 Keys pl neu übersetzen (Phase 3).",
        "affectedLanguages": "pl",
        "impactWithoutFix": "Kaputt aussehende Dashboard-Labels für pl-Nutzer.",
    },
    {
        "findingId": "X8", "riskLevel": "🟧", "category": "Abo-Transparenz ai_limits",
        "language": "kn, en, es", "jurisdiction": "—",
        "file": "values-kn|en|es/strings.xml", "stringKeys": ["ai_limits_dialog_body"],
        "currentText": "Die Klausel '+150/Tag für jedes eigene Profil' fehlt (DE nennt 150 viermal, kn/en/es nur dreimal); hi fehlt zudem die 'Bis zu 150 Text-Verbesserungen'-Bullet",
        "rationale": "Unvollständige Fair-Use-Offenlegung — gerade dieser Dialog ist die Absicherung gegen die 'Unbegrenzt'-Claims (B1-B3) und muss vollständig sein.",
        "invasivityLevel": "text-only",
        "fixPlan": "ai_limits_dialog_body für kn/en/es/hi aus aktuellem DE neu übersetzen (Phase 3).",
        "affectedLanguages": "kn, en, es, hi",
        "impactWithoutFix": "Limit-Offenlegung lückenhaft in 4 Sprachen.",
    },
    {
        "findingId": "X9", "riskLevel": "🟨", "category": "Anzeige-Artefakt tr",
        "language": "tr", "jurisdiction": "—",
        "file": "values-tr/strings.xml", "stringKeys": ["legend_* (15 Keys)"],
        "currentText": "Doppeltes %% zeigt türkischen Nutzern '67–100 %%'",
        "rationale": "Sichtbares Doppel-Prozent (in Nicht-Format-Strings ist %% nicht nötig).",
        "invasivityLevel": "text-only",
        "fixPlan": "15 legend_*-Keys tr korrigieren (Phase 3, mechanisch).",
        "affectedLanguages": "tr",
        "impactWithoutFix": "Unsauber wirkende Prozentanzeigen.",
    },
    {
        "findingId": "X10", "riskLevel": "🟨", "category": "Einzel-Übersetzungsfehler",
        "language": "ta, te, mr, pt-rPT, ur", "jurisdiction": "—",
        "file": "values-*/strings.xml", "stringKeys": ["prompt_clear_confirm_text (ta,te)", "consent_footer_link_impressum (te,mr,pt-rPT)", "privacy_gate_tts_title (ur)", "ai_prompt_no_dates_rule Feldname (tr)"],
        "currentText": "ta/te: falsches Löschobjekt ('Eintrag' statt 'Individuelle Analyse'); te/pt-rPT: 'Impressum' unübersetzt, mr: 'परिचय' (=Einführung) falsch; ur: TTS-Gate-Titel stale; tr: Feldname 'datum' statt 'herleitung'",
        "rationale": "Einzelne semantische Übersetzungsfehler in rechtlich relevanten/funktionalen Strings.",
        "invasivityLevel": "text-only",
        "fixPlan": "Einzelkeys pro Sprache neu übersetzen (Phase 3).",
        "affectedLanguages": "ta, te, mr, pt-rPT, ur, tr",
        "impactWithoutFix": "Verwirrende Dialoge/Labels in Einzelsprachen.",
    },
    {
        "findingId": "X11", "riskLevel": "🟨", "category": "needs-clarification",
        "language": "mr", "jurisdiction": "—",
        "file": "values-mr/strings.xml", "stringKeys": ["json_key_* (27 Keys)"],
        "currentText": "mr nutzt Devanagari-JSON-Keys (रंग, नाव) statt Romanisierung — bn/hi nutzen romanisierte Keys",
        "rationale": "Potentielles JSON-Parse-Risiko falls die KI die Devanagari-Keys nicht 1:1 zurückgibt. Funktional konsistent solange Prompt UND Parser dieselben getString-Werte nutzen — Code-seitig plausibel, aber unverifiziert auf KI-Seite.",
        "invasivityLevel": "text-only",
        "fixPlan": "Option: mr-json_keys auf romanisierte Form angleichen (wie bn/hi) — konservativ, in Phase 2 entscheiden.",
        "affectedLanguages": "mr",
        "impactWithoutFix": "Mögliche Dashboard-Analyse-Parsefehler für mr-Nutzer.",
    },
    {
        "findingId": "X12", "riskLevel": "🟨", "category": "Konsistenz/Kosmetik",
        "language": "ko, zh, ur", "jurisdiction": "—",
        "file": "values-*/strings.xml", "stringKeys": ["diverse"],
        "currentText": "ko: Höflichkeits-Register-Mix (27× 해요체 / 14× 합니다체); zh: ~5 ASCII-Kommas nach Latein-Token; ur: 'DSGVOآرٹ.9' ohne Leerzeichen, 2 RTL-Hinweise ohne RLM",
        "rationale": "Rein kosmetische Konsistenzpunkte, kein Rechtsrisiko.",
        "invasivityLevel": "text-only",
        "fixPlan": "Optional in Phase 3 mitnehmen oder bewusst akzeptieren.",
        "affectedLanguages": "ko, zh-rCN, zh-rTW, ur",
        "impactWithoutFix": "Minimal — Stil.",
    },
    {
        "findingId": "X13", "riskLevel": "🟨", "category": "lint/i18n-Hygiene",
        "language": "alle 26", "jurisdiction": "—",
        "file": "values/strings.xml", "stringKeys": ["dev_seed_dialog_title", "dev_seed_dialog_message", "dev_seed_add_action", "dev_seed_delete_all_action", "dev_seed_cancel_action"],
        "currentText": "5 Debug-only-Strings (Test-Daten-Dialog, BuildConfig.DEBUG) fehlen in allen 26 Sprachen",
        "rationale": "Erscheinen nie im Release (doppelt DEBUG-geguarded, W7-verifiziert). Sauberste Lösung: translatable=\"false\" — dann lint-sauber und bewusst von Übersetzung ausgenommen.",
        "invasivityLevel": "text-only",
        "fixPlan": "translatable=\"false\" auf die 5 dev_seed_*-Keys im DE setzen (Phase 2/3).",
        "affectedLanguages": "—",
        "impactWithoutFix": "MissingTranslation-Lint-Rauschen.",
    },
]

# Risk counting
def lvl(f):
    return f.get("riskLevel", "🟨")

all_findings = de_findings + xling
counts = {"🟥": 0, "🟧": 0, "🟨": 0}
for f in all_findings:
    k = lvl(f)[:1] if lvl(f) else "🟨"
    # emoji is multi-byte; just match full string
    if "🟥" in lvl(f): counts["🟥"] += 1
    elif "🟧" in lvl(f): counts["🟧"] += 1
    else: counts["🟨"] += 1

report = {
    "mode": "default",
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "auditedLanguages": ["de"] + ["ar","bn","en","es","fr","gu","hi","in","it","ja","kn","ko","ml","mr","nl","pl","pt-rBR","pt-rPT","ta","te","th","tr","uk","ur","zh-rCN","zh-rTW"],
    "auditedJurisdictions": ["DE/EU", "UK", "US/CA(CCPA)", "Play-Store-global (ohne RU/CN-Mainland)"],
    "findings": {
        "textual": de_findings,
        "crossLingual": xling,
        "deadUrls": urls.get("deadUrls", []),
        "missingDocs": [],
    },
    "verifiedOk": {
        "r1_dsgvo": r1.get("verifiedOk", []),
        "r2_uwg": r2.get("verifiedOk", []),
        "r3_abo": r3.get("verifiedOk", []),
        "r4_hwg_ai": r4.get("verifiedOk", []),
        "urls": "33 URLs geprüft, 0 tot; Play-Pflicht-URLs (Privacy + Lösch-Anleitung) live",
        "literals": "0 i18n-Leaks in .kt (837 stringResource-Aufrufe, alle Literale legitim)",
        "guCorruption": "gu-Korruptions-Vorfall vollständig repariert (1170 Knoten gescannt, 0 Funde)",
        "ukNoRussianMix": "uk sauber ukrainisch, 0 ru-Kontamination",
        "cnTwDifferentiation": "zh-rCN/zh-rTW echt differenziert (kein Kopier-Verdacht)",
        "brPtDifferentiation": "pt-rBR/pt-rPT echt differenziert (56% — unter Schwelle)",
        "numbersAiLimits": "ai_limits-Zahlen exakt in 23/26 Sprachen (kn/en/es Klausel fehlt → X8)",
    },
    "codeVerdicts": codev.get("verdicts", []),
    "openFindingsCount": len(all_findings),
    "riskCounts": counts,
    "fixedThisRun": 0,
    "skippedThisRun": 0,
    "userAlternativesApplied": 0,
    "invasiveChangesApplied": 0,
    "manualFixesPending": 0,
    "previousRunDelta": {"newFindings": len(all_findings), "resolvedFindings": 0,
                          "note": "Letzter Lauf 2026-05-22 endete mit 0; App+Skills seitdem geändert — Vollscan-Neubewertung"},
}

p = os.path.join(shield, "recht-report.json")
with open(p + ".tmp", "w", encoding="utf-8", newline="\n") as f:
    json.dump(report, f, indent=2, ensure_ascii=False)
os.replace(p + ".tmp", p)

print(f"recht-report.json geschrieben: {os.path.getsize(p)} Bytes")
print(f"Findings gesamt: {len(all_findings)}  (DE: {len(de_findings)}, Cross-Lingual: {len(xling)})")
print(f"Risiko: rot={counts['🟥']}  orange={counts['🟧']}  gelb={counts['🟨']}")
ids_de = [f.get("findingId", "?") for f in de_findings]
print("DE-IDs:", ", ".join(ids_de))

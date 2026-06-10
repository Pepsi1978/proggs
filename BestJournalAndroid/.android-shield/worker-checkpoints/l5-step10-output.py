# -*- coding: utf-8 -*-
import json, os, tempfile

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
SHIELD = os.path.join(ROOT, ".android-shield")
OUT = os.path.join(SHIELD, "worker-outputs", "phase1b-xling-L5.json")

# Shared findings that appear identically in ALL 4 CJK langs (DE-revision drift)
shared_drift = [
    {
        "findingId": "L5-CJK-1",
        "riskLevel": "\U0001F7E8",  # yellow
        "category": "Legal-Label-Mismatch",
        "key": "settings_revoke_subtitle",
        "languages": ["ja", "ko", "zh-rCN", "zh-rTW"],
        "deText": "§ 356a BGB — Widerruf direkt per App",
        "cjkText": {"ja": "プレミアム購入", "ko": "Premium 구매", "zh-rCN": "Premium 购买", "zh-rTW": "Premium 購買"},
        "issue": "Untertitel des § 356a-BGB-Widerrufsbuttons lautet in ALLEN 4 CJK-Sprachen 'Premium-Kauf' (Premium purchase) statt die Widerruf-Funktion zu beschreiben. Der zugehoerige Button-Body (settings_revoke_email_body) ist dagegen korrekt uebersetzt und nennt § 356a BGB. Die Affordance des gesetzlich vorgeschriebenen Widerruf-Buttons ist damit in CJK falsch beschriftet.",
        "rationale": "Der zweistufige § 356a-BGB-Widerruf ist eine Pflichtfunktion (DE/EU). Der Untertitel soll den Nutzer zur Widerrufs-Aktion fuehren; 'Premium-Kauf' fuehrt ihn in die Irre (klingt nach Kaufen statt Widerrufen). Da identisch in allen 4 Sprachen, stammt der Wert vermutlich aus einer aelteren DE-Revision.",
        "suggestedFix": "settings_revoke_subtitle in allen 4 CJK-Dateien an die DE-Bedeutung angleichen, z.B. ja '§ 356a BGB — アプリから直接撤回', ko '§ 356a BGB — 앱에서 바로 철회', zh-rCN '§ 356a BGB — 在应用内直接撤销', zh-rTW '§ 356a BGB — 在應用程式內直接撤銷'. NON-INVASIVE (string-only).",
        "consistentAcross4": True,
    },
    {
        "findingId": "L5-CJK-2",
        "riskLevel": "\U0001F7E8",
        "category": "Content-Drift-vs-DE",
        "key": "settings_delete_account_confirm_body / settings_delete_account_subtitle",
        "languages": ["ja", "ko", "zh-rCN", "zh-rTW"],
        "deText": "DE confirm_body: loescht 'lokale Eintraege+Fotos+Videos+Audio', 'Google-Drive-Backup (nur App-Daten, NICHT dein Google-Konto)', 'App-Anmeldung'; 'Dein Google-Konto bleibt bestehen'. DE subtitle: 'Dein Google-Konto selbst bleibt bestehen.'",
        "cjkText": {"all": "CJK confirm_body listet '...Fotos/Videos', 'Firebase-Konto', 'Google-Drive-Backup' und LAESST WEG: 'nur App-Daten, nicht Google-Konto' + 'Audio-Aufnahmen'. CJK subtitle sagt sogar 'loescht ... deinen Google-Account' (Gegenteil von DE!)."},
        "issue": "Zwei Abweichungen in allen 4 CJK: (a) confirm_body nennt 'Firebase-Konto' statt DE 'App-Anmeldung' und unterschlaegt die DE-Klarstellung 'nur die App-Daten, nicht dein Google-Konto' + 'Audio-Aufnahmen'. (b) subtitle behauptet, der GOOGLE-ACCOUNT werde geloescht — DE sagt ausdruecklich das Gegenteil ('Google-Konto bleibt bestehen').",
        "rationale": "Account-Loeschung ist Play-Pflichtfunktion. Die subtitle-Abweichung ist potenziell irrefuehrend (Nutzer koennte glauben, sein Google-Konto werde geloescht). 'Firebase' ist zudem ein internes Implementierungsdetail, das DE bewusst durch 'App-Anmeldung' ersetzt hat. Identisch in allen 4 Sprachen → alte DE-Revision.",
        "suggestedFix": "Beide Keys in allen 4 CJK an die aktuelle DE-Bedeutung angleichen: 'App-Anmeldung' statt Firebase, Klarstellung 'nur App-Daten, NICHT dein Google-Konto' aufnehmen, subtitle korrigieren zu 'Google-Konto bleibt bestehen'. NON-INVASIVE (string-only).",
        "consistentAcross4": True,
    },
    {
        "findingId": "L5-CJK-3",
        "riskLevel": "\U0001F7E9",  # green / info
        "category": "Content-Drift-vs-DE",
        "key": "privacy_gate_groq_body / privacy_gate_tts_body / consent_toggle_do_not_sell_body",
        "languages": ["ja", "ko", "zh-rCN", "zh-rTW"],
        "deText": "DE groq: 3 Checkmarks inkl. 'Rechtsgrundlage: EU SCCs'. DE tts: 2 Checkmarks, KEIN SCC. DE do_not_sell: 2 Absaetze inkl. 'Best Journal verkauft deine Daten nirgendwo'.",
        "cjkText": {"all": "CJK groq: OHNE Checkmark-Bullets, nennt aber praeziser 'Groq, Inc. (Mountain View, USA)'; SCC-Zeile fehlt. CJK tts: nennt ZUSAETZLICH 'Rechtsgrundlage: EU-US DPF + Standardvertragsklauseln' (mehr als DE!). CJK do_not_sell: auf 1 Zeile gekuerzt, ohne 'verkaufen wir nirgends'-Zusicherung."},
        "issue": "Die drei Privacy-Gate/Consent-Texte weichen in allen 4 CJK strukturell vom aktuellen DE-Quelltext ab (andere Checkmark-Struktur, teils mehr/teils weniger Rechtsgrundlage-Info). Kernaussagen (Verschluesselung, kein KI-Training, Loeschung, Rechtsgrundlage) sind aber in irgendeiner Form vorhanden — bei tts sogar vollstaendiger als DE.",
        "rationale": "Inhaltliche Drift gegen die DE-Referenz, keine Schutzluecke: die datenschutzrechtlich relevanten Aussagen sind erhalten (oder ergaenzt). do_not_sell behaelt die CCPA-Opt-out-Funktion, verliert aber die beruhigende 'wir verkaufen nirgends'-Zusicherung. Reiner Konsistenz-/Vollstaendigkeits-Hinweis, niedrige Prioritaet.",
        "suggestedFix": "Optional die 3 Texte in allen 4 CJK an die finale DE-Fassung angleichen (Checkmark-Struktur, SCC-Zeile bei groq ergaenzen, do_not_sell 'wir verkaufen nirgends' wieder aufnehmen). NON-INVASIVE (string-only).",
        "consistentAcross4": True,
    },
    {
        "findingId": "L5-CJK-4",
        "riskLevel": "\U0001F7E9",
        "category": "Punctuation-Cosmetic",
        "key": "consent_card2_body, consent_card3_body, privacy_gate_groq_body, privacy_gate_tts_body, consent_toggle_groq_body (zh-rCN); + paywall_exit_price_comparison (zh-rTW)",
        "languages": ["zh-rCN", "zh-rTW"],
        "issue": "~5 nutzersichtbare zh-Strings verwenden ein ASCII-Komma ',' direkt nach einem Latein-Token (z.B. 'Firebase Analytics,可选', 'Groq, Inc.(美国),转换'), wo chinesische Typografie ein Vollbreit-Komma '，' oder '、' bevorzugt.",
        "rationale": "Rein kosmetisch — verbreitete Praxis nach lateinischen Markennamen, kein Funktions- oder Rechtsproblem.",
        "suggestedFix": "Optional ASCII ',' nach Latein-Token in zh durch '，' ersetzen. NON-INVASIVE (string-only), sehr niedrige Prioritaet.",
        "consistentAcross4": False,
    },
]

def lang_block(lang, politeness, note):
    return {
        "language": lang,
        "legalKeysChecked": 94,
        "legalKeysVerdict": "PASS — Bedeutung korrekt, rechtlich praezise (Auto-Renewal, 14-Tage-Widerruf, Kuendigungsweg, 'jederzeit kuendbar' alle vorhanden)",
        "numberCheck_ai_limits": {"verdict": "PASS", "expected": {"150": 4, "600": 1, "30": 2, "101": 2, "151": 1, "50": 1, "5": 3}, "found_matches_DE_exactly": True},
        "autoRenewalTermPresent": True,
        "withdrawal14dPresent": True,
        "anytimeCancellablePresent": True,
        "omissionInLegalKeys": "Keine substanziellen Auslassungen in nutzersichtbaren Legal-Keys. Interne ai_prompt_*-Systemprompts sind verdichtet (z.B. custom_intro ja/ko ~20% der DE-Laenge), aber die Verhaltensregeln (50%-Profilbezug, Datums-Verbot, Antwortsprache) bleiben erhalten.",
        "politenessRegister": politeness,
        "newlineInWordTraps": "Keine \\n-Brueche mitten in Woertern/Komposita in Legal-Strings festgestellt; \\n stehen an Satz-/Absatzgrenzen.",
        "identicalToDe": {"keys": ["settings_about_version"], "classification": "benign — 'Version %1$s' ist sprachneutral (nur Versionsnummern-Platzhalter)"},
        "randomSampleVerdict": "15 Stichproben natuerlich, idiomatisch, keine Maschinen-Artefakte; CJK-Interpunktion korrekt (。、？ bzw. ，。？).",
        "perLangNote": note,
    }

perLanguage = {
    "ja": lang_block("ja",
        "です/ます-Register weitgehend konsistent (42/93 Legal-Keys mit expliziten です/ます-Markern; Rest sind kurze Labels/Listenpunkte ohne Verb — normal). Hoeflichkeitsebene durchgaengig hoeflich-neutral, passend fuer eine App.",
        "Vorbildliche Lokalisierung: 'Impressum' → 特定商取引法表記 (japanisches Rechtsaequivalent). Numbers/Auto-Renewal/Widerruf alle korrekt."),
    "ko": lang_block("ko",
        "Register-MISCHUNG: 27 Legal-Keys im 해요체 (informell-hoeflich, z.B. '있어요', '변경할 수 있어요'), 14 im 합니다체 (formell, v.a. Krisen-/Loeschungs-Dialoge wie settings_crisis_dialog_body, settings_delete_account_confirm_body). Inkonsistenz ist STILISTISCH bewusst wirkend (ernste Rechts-/Krisentexte formeller), aber nicht 100% einheitlich — kosmetischer Konsistenz-Hinweis.",
        "Numbers/Auto-Renewal(자동 갱신)/14일 철회권 alle korrekt. 해요체/합니다체-Mix ist der einzige Register-Punkt."),
    "zh-rCN": lang_block("zh-rCN",
        "Chinesisch hat kein Hoeflichkeitsregister wie ja/ko — entfaellt. Anrede durchgaengig '你' (informell-du), konsistent zur deutschen Du-Form.",
        "Vereinfachte Zeichen + Festland-Terminologie korrekt (软件/配置文件/自动续订/仪表盘). Numbers/Auto-Renewal/Widerruf korrekt."),
    "zh-rTW": lang_block("zh-rTW",
        "Kein Hoeflichkeitsregister (entfaellt). Anrede '你' konsistent.",
        "Traditionelle Zeichen + Taiwan-Terminologie korrekt (軟體/設定檔/自動續訂/儀表板). zh-rTW-Antwortsprache-Prompt nennt sogar explizit 繁體中文（台灣）. Numbers/Auto-Renewal/Widerruf korrekt."),
}

output = {
    "worker": "L5-cjk",
    "phase": "1b-cross-lingual",
    "languages": ["ja", "ko", "zh-rCN", "zh-rTW"],
    "auditOnly": True,
    "summary": "CJK-Audit: maschinell sehr sauber (5 dev_seed_* fehlen je Sprache=bekannt, 0 formatArgMismatch, 0 naked %, 0 Apostroph-Fehler, identicalToDe=1 benign). 94/95 Legal-Keys semantisch tief geprueft: Bedeutung korrekt, Auto-Renewal + 14-Tage-Widerruf + 'jederzeit kuendbar' in allen 4 Sprachen vorhanden, ai_limits-Zahlen exakt (150/600/30/101/151/50/5). CN/TW echt differenziert. 3 inhaltliche Drift-Findings (identisch in allen 4 = alte DE-Revision), davon 2 gelb (settings_revoke_subtitle 'Premium-Kauf' statt Widerruf; delete_account Firebase/Google-Account-Wortlaut) + 1 gruen (privacy gates). Keine per-Sprache-Uebersetzungsfehler.",
    "machineCheckConfirmed": {
        "missingKeys_perLang": ["dev_seed_add_action", "dev_seed_cancel_action", "dev_seed_delete_all_action", "dev_seed_dialog_message", "dev_seed_dialog_title"],
        "missingKnownAcceptable": True,
        "formatArgMismatch": 0,
        "nakedPercent": 0,
        "unescapedApostrophes": 0,
        "identicalToDe": ["settings_about_version"],
    },
    "perLanguage": perLanguage,
    "cnTwDifferentiation": {
        "identicalCount": 9,
        "comparedLegalKeys": 93,
        "identicalLegalKeys": ["churn_discount_badge", "churn_instead_of", "paywall_day7", "paywall_first_payment", "paywall_from_per_day", "paywall_reminder", "paywall_with_limited", "settings_legal_section_header", "transcription_model_groq"],
        "allSharedKeys": 1087,
        "allIdenticalCount": 187,
        "allIdenticalPct": 17,
        "verdict": "GESUND/echt differenziert. Nur 9/93 Legal-Keys (und 17% aller Keys) sind zwischen zh-rCN und zh-rTW identisch — und das sind ausschliesslich kurze numerische/Marken-/strukturelle Strings (z.B. '省 %1$d%%', '第 7 天', '首次付款', 'Groq Whisper...'), deren Han-Zeichen in vereinfacht UND traditionell identisch sind. Alle laengeren Texte unterscheiden sich korrekt in Zeichen (软件 vs 軟體, 自动续订 vs 自動續訂, 仪表盘 vs 儀表板, 变更 vs 變更) und Terminologie. KEIN Kopier-Verdacht (waere bei >50% identisch)."
    },
    "findings": shared_drift,
    "top3": [
        "settings_revoke_subtitle: In ALLEN 4 CJK steht 'Premium-Kauf' statt der § 356a-BGB-Widerruf-Beschreibung — die gesetzliche Widerruf-Affordance ist falsch beschriftet (gelb, string-only Fix).",
        "settings_delete_account_subtitle: CJK behauptet, der Google-Account werde mitgeloescht — DE sagt ausdruecklich das Gegenteil ('Google-Konto bleibt bestehen'); potenziell irrefuehrend (gelb, string-only Fix).",
        "Positiv: ai_limits_dialog_body-Zahlen (150/600/30/101/151/50/5) in allen 4 Sprachen exakt; Auto-Renewal + 14-Tage-Widerruf + 'jederzeit kuendbar' vollstaendig; CN/TW sauber getrennt."
    ],
    "plugin_bugs_observed": [
        "Keine finale-Plugin-Bugs beobachtet. Inputs (legal-keys, cross-lingual-matrix) sauber und konsistent. Hinweis: der Legal-Key 'onboarding_feature_secure__privacy' (sowie Composite-Eintraege wie 'ai_prompt_insight_intro / profile_insight_*') existieren nicht als echte DE-Keys — das sind Platzhalter/Composite-Notationen in der legal-keys-Liste (94/95 real). Nicht-blockierend, aber die Liste koennte um diese Pseudo-Keys bereinigt werden."
    ],
    "selfObservation_FIN037": [
        "Die low length-ratio bei ai_prompt_*-Keys war zunaechst ein Truncation-Verdacht; die gezielte '50'-Pruefung + Volltext-Read zeigte aber, dass die Verhaltensregeln erhalten und nur die Prosa verdichtet ist — Lehre: bei internen LLM-Prompts NICHT nach Laengen-Ratio urteilen, sondern die kritische Regel-Substanz pruefen.",
        "Die staerksten Findings waren NICHT maschinell sichtbar (formatArgs/Apostrophe alle gruen) und NICHT per-Sprache, sondern semantische DE-Revisions-Drift, die in allen 4 CJK identisch auftrat — das paarweise DE-vs-Ziel-Lesen war der entscheidende Hebel, nicht die Matrix.",
        "Per-Sprache .py-Dumps in Batches (statt ein Riesen-Read) hielten den Kontext schlank und vermieden den Subagent-Crash trotz 5x grosser strings.xml."
    ],
    "outputPath": OUT,
}

# atomic write
d = os.path.dirname(OUT)
with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp", delete=False, encoding="utf-8") as tmp:
    json.dump(output, tmp, ensure_ascii=False, indent=1)
    tmp.write("\n")
    tmppath = tmp.name
os.replace(tmppath, OUT)
print("WROTE:", OUT)
# validate
with open(OUT, encoding="utf-8") as f:
    json.load(f)
print("JSON valid. size:", os.path.getsize(OUT), "bytes")

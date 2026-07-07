# Build the phase-2 fix plan: Frank's card decisions + bulk suggestedFixes[0] per file bundle.
import json, os

shield = os.path.expanduser("~/proggs/BestJournalAndroid/.android-shield")
wo = os.path.join(shield, "worker-outputs")


def load(name):
    with open(os.path.join(wo, name), encoding="utf-8") as f:
        return json.load(f)


r1, r2, r3, r4 = (load("phase1b-recht-dsgvo.json"), load("phase1b-recht-uwg.json"),
                  load("phase1b-recht-abo.json"), load("phase1b-recht-hwg-ai.json"))

by_id = {}
for src in (r1, r2, r3, r4):
    for f in src.get("findings", []):
        by_id[f.get("findingId")] = f


def fix0(fid):
    f = by_id.get(fid, {})
    sf = f.get("suggestedFixes", [])
    if not sf:
        return None
    first = sf[0]
    return first.get("text") or first.get("fix") or str(first)


plan = {
    "decisions": {
        "B1": "[2] Karten-Wahl Frank", "B2": "[3]", "B3": "[3]", "B4": "[1]",
        "A1": "[1] nur Beispiele", "A2": "[K] Kombi", "A3": "[K] alle 3 Stellen", "A4": "[1]",
        "F1": "[2] anwenden", "A7": "[2] anwenden", "C4": "[1]", "X11": "romanisieren",
        "E1": "manual-pending", "E2": "kein Fix (bewusste Alt-Entscheidung #1236)",
        "D2": "nur stalen Kommentar fixen", "C6": "akzeptiert+dokumentiert",
        "Z1": "bewusste Produktentscheidung (CrisisHelpDialog existiert)",
        "X1-X10,X12": "Phase 3 Neuübersetzung", "X13": "translatable=false (FA-1)",
    },
    "bundles": {
        "FA1_strings_de": {
            "file": "app/src/main/res/values/strings.xml",
            "fixes": [
                {"id": "B1", "key": "retro_benefit_weekly",
                 "newText": "Alle Wochenrückblicke statt nur der ersten 2 – im Rahmen deines KI-Tageskontingents"},
                {"id": "B2", "key": "settings_premium_feature_profiles", "alsoKeys": ["paywall_feature_profiles"],
                 "newText": "Eigene KI-Profile ohne Anzahllimit"},
                {"id": "B3", "key": "settings_premium_feature_5_perspectives", "alsoKeys": ["churn_offer_feature_perspectives"],
                 "newText": "Alle 4 vordefinierten KI-Profile + so viele eigene wie du brauchst"},
                {"id": "B4+C3", "key": "onboarding_feature_secure",
                 "newText": "Deine Einträge bleiben auf dem Gerät; KI-Anfragen werden verschlüsselt übertragen"},
                {"id": "A1", "key": "ai_prompt_no_dates_rule",
                 "instruction": "NUR die zwei Krankheits-/Kausal-Beispiele ersetzen, Rest des Strings unverändert: Das Borreliose-Beispiel durch ein neutrales Alltags-Beispiel (RICHTIG: 'Dein Postfach läuft seit Tagen über und raubt dir Zeit.') und das Laufen/mentale-Klarheit-Beispiel durch (RICHTIG: 'Bewegung schafft dir spürbar mehr Überblick im Alltag.'). Die FALSCH-Beispiele (Datums-Verbote) sinngemäß angleichen, Funktion der Regel (keine Datumsangaben) bleibt identisch."},
                {"id": "A2", "key": "ai_prompt_entropy_intro",
                 "instruction": "Drei Änderungen in diesem String: (1) Rolle 'Lebensberater' -> 'einfühlsamer Ordnungs- und Muster-Analyst'; (2) in der Suchliste 'Schmerz, Schlafprobleme' entfernen und durch 'Überforderung, Zeitdruck' ersetzen; (3) am Ende Guardrail-Satz anfügen: 'Du gibst KEINE medizinischen, therapeutischen oder diagnostischen Ratschläge. Bei gesundheitlichen Themen verweist du wertneutral auf Fachpersonen.' Alles andere unverändert."},
                {"id": "A3a", "key": "ai_prompt_custom_intro",
                 "instruction": "Guardrail-Satz anfügen: 'UNABHÄNGIG vom Auftrag gibst du KEINE medizinischen Diagnosen, Therapieempfehlungen, Rechts- oder Finanzberatung. Verweise bei solchen Themen wertneutral auf qualifizierte Fachpersonen.'"},
                {"id": "A3b", "key": "ai_prompt_custom_rules",
                 "instruction": "Den gleichen Guardrail als zusätzliche AUFTRAGS-REGEL ergänzen (Stil der bestehenden Regeln übernehmen)."},
                {"id": "A3c", "key": "ai_prompt_rerank_system",
                 "instruction": "Den gleichen Guardrail-Satz ergänzen (der Re-Ranker generiert frei neue Maßnahmen)."},
                {"id": "A4", "key": "profile_entropy_long",
                 "newText": "Die KI findet, was dich im Alltag belastet, und sortiert es. Du bekommst eine Übersicht deiner größten Belastungsquellen und 5 konkrete Aufräum-Schritte. Ideal, wenn du den Überblick zurückgewinnen willst."},
                {"id": "C1", "key": "consent_toggle_analytics_body~Z671", "suggestedFromWorker": fix0("C1"),
                 "instruction": "Per R1-Vorschlag: 'anonym' -> 'pseudonym' (Zeile ~671, Analytics-Beschreibung). Exakten Key per Grep 'anonym' in values/strings.xml finden."},
                {"id": "C2", "key": "analytics-Toggle-Titel ~Z670+728", "suggestedFromWorker": fix0("C2"),
                 "instruction": "Gleicher anonym->pseudonym-Fix für Toggle-Titel + Settings-Titel (Grep 'Anonyme' in values/strings.xml, alle Treffer)."},
                {"id": "C4", "key": "privacy_gate_tts_body", "suggestedFromWorker": fix0("C4"),
                 "instruction": "Drittanbieter-Zusagen konservativ absichern: Aussagen über Microsoft als 'laut Microsoft' kennzeichnen bzw. auf eigene Kontrolle beschränken (R1-Vorschlag 1 verwenden). Inhaltliche Aufklärung (Microsoft, USA, Verschlüsselung) beibehalten."},
                {"id": "B5+F2", "key": "onboarding_premium_feature_noads", "alsoKeys": ["paywall_feature_noads"], "suggestedFromWorker": fix0("B5"),
                 "instruction": "'Keine Werbung'-Benefit ersetzen (es gibt kein Ad-SDK, auch Free ist werbefrei). R2-Vorschlag 1 verwenden — ersatzweise ein ECHTES Premium-Feature nennen (z.B. 'Werbefrei – heute und in Zukunft' NUR falls R2 das vorschlägt, sonst R2-Text)."},
                {"id": "B6", "suggestedFromWorker": fix0("B6"), "instruction": "R2-Finding B6 (KI-Spitzenzusage 'Muster, die dir verborgen bleiben'): suggestedFixes[0] anwenden. Key+Zeile aus recht-input: per Grep nach dem currentText suchen."},
                {"id": "B7", "suggestedFromWorker": fix0("B7"), "instruction": "R2-Finding B7 ('antworten persönlich/24 Std'): suggestedFixes[0] anwenden."},
                {"id": "B8", "suggestedFromWorker": fix0("B8"), "instruction": "R2-Finding B8 (Custom-Prompt-Beschreibung ohne Health-Hinweis): suggestedFixes[0] anwenden."},
                {"id": "A5", "suggestedFromWorker": fix0("A5"), "instruction": "R4-Finding A5 (Goals-Prompt: abnehmen/Schlaf): suggestedFixes[0] anwenden."},
                {"id": "A6", "suggestedFromWorker": fix0("A6"), "instruction": "R4-Finding A6 (Insight-Prompt: psychotherapeutisches Vokabular): suggestedFixes[0] anwenden."},
                {"id": "D1-string", "key": "paywall_monthly_note",
                 "instruction": "NEUEN String anlegen (rfind vor </resources>): <string name=\"paywall_monthly_note\">Verlängert sich automatisch monatlich, jederzeit kündbar</string> — Kommentar dazu: <!-- Auto-renewal note for monthly plan (D1) -->. FA-3 verdrahtet ihn in PaywallScreen."},
                {"id": "D2-comment", "line": 1100,
                 "instruction": "Den stalen XML-Kommentar bei ~Zeile 1100 korrigieren: Er behauptet einen §356-Verzichts-Dialog, der in v0.17.2 bewusst entfernt wurde. Neuer Kommentar: <!-- Kauf-Bestätigung läuft über das Google-Play-Sheet; Widerrufs-Hinweis via paywall_legal_hint (D1-Fix #1233) -->"},
                {"id": "X13", "keys": ["dev_seed_dialog_title", "dev_seed_dialog_message", "dev_seed_add_action", "dev_seed_delete_all_action", "dev_seed_cancel_action"],
                 "instruction": "Bei allen 5 dev_seed_*-Strings das Attribut translatable=\"false\" ergänzen (Debug-only, nie im Release sichtbar)."},
            ],
        },
        "FA2_churn": {
            "file": "app/src/main/java/com/bestjournal/app/ui/screens/settings/ChurnFlowDialog.kt",
            "fixes": [
                {"id": "F1", "instruction": "Step-3-Buttons optisch gleichwertig machen: 'Zu Google Play'-TextButton von Alpha 0.6 auf 1.0, gleiche Button-Klasse/Gewichtung wie der Stay-Button (oder beide als gleichartige Buttons), die Puls-Animation des Stay-Buttons entfernen, den verräterischen Kommentar (~Z.1087 'so it does not compete visually') löschen. Confirmshaming-Titel 'Schade, dass du gehst' NICHT ändern (String-Inhalt bleibt — nur Optik-Gleichwertigkeit). Funktionalität (Callbacks, Navigation) unverändert."},
                {"id": "E3", "instruction": "Hardcodierte Retention-Fallback-Preise ('2,99'/'22,49 €' bei ~Z.689 bzw. RETENTION_*_PRICE-Konstanten): Sicherstellen dass IMMER der Live-Play-Preis Vorrang hat und der Fallback-Text neutral ist (z.B. 'Preis wird geladen…' / '…') statt eines konkreten EUR-Betrags, damit Nicht-EUR-Nutzern nie ein falscher Betrag gezeigt wird. Falls die Konstanten in Constants.kt liegen: dort neutralisieren NUR wenn sie ausschließlich als Anzeige-Fallback dienen — sonst die Anzeige-Stelle fixen."},
            ],
        },
        "FA3_paywall": {
            "file": "app/src/main/java/com/bestjournal/app/ui/screens/paywall/PaywallScreen.kt",
            "fixes": [
                {"id": "D1-ui", "instruction": "Unter dem Monats-Plan-Button (~Z.528) eine Auto-Renewal-Hinweiszeile ergänzen — gleiche Optik/Typo wie die bestehende paywall_yearly_note-Zeile beim Jahres-Plan. Text aus NEUEM String R.string.paywall_monthly_note (legt FA-1 an). Nur diese eine Zeile, kein weiteres Layout ändern."},
            ],
        },
        "FA4_dashboard": {
            "file": "app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt",
            "fixes": [
                {"id": "A7", "instruction": "Im AdviceDerivationDialog (~Z.2221) die bestehende AiOutputDisclaimer-Komponente (bzw. AiGeneratedBadge — die Komponente die die anderen Screens nutzen, per Grep 'AiOutputDisclaimer\\|AiGeneratedBadge' finden) als EINE Zeile am Dialog-Ende einfügen — gleiche Optik wie an den anderen Stellen. ACHTUNG: Datei ist 193KB — NUR per Grep + Read-Range (max 200 Zeilen) arbeiten, NIE voll lesen."},
            ],
        },
        "FA5_mr": {
            "file": "app/src/main/res/values-mr/strings.xml",
            "fixes": [
                {"id": "X11", "instruction": "Die 27 json_key_*- und json_value_*-Strings in values-mr von Devanagari auf die romanisierte Form angleichen. QUELLE: die entsprechenden Werte aus values-hi/strings.xml 1:1 übernehmen (hi nutzt bereits romanisierte Keys — exakt dieselben Werte verwenden, KEINE eigene Übersetzung). Per Python: aus values-hi alle json_key_*/json_value_*-Werte extrahieren und in values-mr ersetzen."},
            ],
        },
        "FA6_manifest": {
            "file": "app/src/main/AndroidManifest.xml",
            "fixes": [
                {"id": "C5", "instruction": "In <application> ergänzen: <meta-data android:name=\"firebase_analytics_collection_enabled\" android:value=\"false\" /> — Poka-Yoke: Analytics bleibt aus bis der Laufzeit-Consent es aktiviert (setAnalyticsCollectionEnabled(true) nach Consent existiert bereits)."},
            ],
        },
    },
}

p = os.path.join(shield, "fix-plan-phase2.json")
with open(p + ".tmp", "w", encoding="utf-8", newline="\n") as f:
    json.dump(plan, f, indent=2, ensure_ascii=False)
os.replace(p + ".tmp", p)
print(f"fix-plan-phase2.json: {os.path.getsize(p)} B")
print("Bundles:", ", ".join(plan["bundles"].keys()))
for fid in ("C1", "C2", "C4", "B5", "B6", "B7", "B8", "A5", "A6"):
    t = fix0(fid)
    print(f"  {fid}: suggestedFixes[0] {'OK' if t else 'FEHLT'}")

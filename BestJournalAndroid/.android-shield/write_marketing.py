import json, os

data = {
  "marketing_claims": [
    {
      "key": "profile_entropy_long",
      "value": "Tipps, die dir sofort helfen können",
      "file": "app/src/main/res/values/strings.xml",
      "line": 390,
      "risk": "UWG",
      "why": "Versprechen sofortiger Hilfe durch KI-Tipps ist absolutes Wirkungsversprechen ohne Einschränkung"
    },
    {
      "key": "onboarding_subtitle",
      "value": "Dein persönliches KI-Tagebuch für Klarheit und Veränderung",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1062,
      "risk": "UWG",
      "why": "Versprechen von Veränderung als Ergebnis der App-Nutzung ist absolute Wirkungsaussage ohne Einschränkung"
    },
    {
      "key": "onboarding_goal_stress",
      "value": "Stress abbauen",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1066,
      "risk": "HWG",
      "why": "Stressabbau als Ziel positioniert die App als Mittel zur psychischen Gesundheitsverbesserung (HWG §3)"
    },
    {
      "key": "dashboard_weekly_review_upsell_body",
      "value": "Du hattest eine bewegte Woche. Mit Premium siehst du die volle Analyse — erkenne Muster, entdecke Einsichten und verstehe, was dich wirklich bewegt.",
      "file": "app/src/main/res/values/strings.xml",
      "line": 242,
      "risk": "UWG",
      "why": "Personalisierter Upsell mit emotionalem Anker (bewegte Woche) ohne sachliche Grundlage zur Kaufentscheidungszeit — suggestiv i.S. § 5 UWG"
    },
    {
      "key": "onboarding_premium_feature_patterns",
      "value": "Persönliche Muster erkennen, die KI findet verborgene Denk- und Gefühlsmuster",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1082,
      "risk": "HWG",
      "why": "Verborgene Denk- und Gefühlsmuster finden impliziert psychodiagnostische Fähigkeit — grenzt an HWG §3 (psychische Erkrankung)"
    },
    {
      "key": "profile_entropy_desc",
      "value": "Erkennt Stress, Unordnung und Belastung",
      "file": "app/src/main/res/values/strings.xml",
      "line": 383,
      "risk": "HWG",
      "why": "Stress-Erkennung als Feature positioniert KI als diagnostisches Werkzeug für psychische Belastung (HWG §3)"
    },
    {
      "key": "dashboard_profile_focus_entropy",
      "value": "Sortiert dein Leben: erkennt Stress, Unordnung und Belastung und zeigt was du loslassen oder klären kannst.",
      "file": "app/src/main/res/values/strings.xml",
      "line": 962,
      "risk": "HWG",
      "why": "Loslassen-Empfehlung durch KI bei Stress/Belastung ist therapeutische Wirkungsaussage (HWG §3)"
    },
    {
      "key": "settings_premium_feature_reviews_desc",
      "value": "Die KI erzählt deine Geschichte",
      "file": "app/src/main/res/values/strings.xml",
      "line": 500,
      "risk": "UWG",
      "why": "Irreführende Beschreibung — KI kann keine persönliche Geschichte erzählen, suggeriert Fähigkeit die nicht existiert (§ 5 UWG)"
    },
    {
      "key": "onboarding_premium_feature_improve",
      "value": "KI-Textverbesserung täglich, jeder Eintrag wird klarer und ausdrucksstärker",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1079,
      "risk": "UWG",
      "why": "Absolutes Versprechen (JEDER Eintrag wird klarer) ohne Einschränkung — garantieähnliche Aussage (§ 5 UWG)"
    },
    {
      "key": "paywall_headline_default",
      "value": "Entdecke dich selbst\nJeden Tag ein Stück mehr",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1144,
      "risk": "UWG",
      "why": "Versprechen täglicher Selbstentwicklung durch das Produkt — suggestive Wirkungsaussage ohne Belege (§ 5 UWG)"
    }
  ],
  "paywall_abo": [
    {
      "key": "paywall_yearly_note",
      "value": "Danach %1$s pro Jahr\nIn der Testphase jederzeit kündbar",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1116,
      "category": "trial"
    },
    {
      "key": "paywall_yearly_savings",
      "value": "Spare %1$d%% gegenüber dem Monatsabo — Kündigung jederzeit möglich zum jeweiligen Vertragsende.",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1119,
      "category": "price"
    },
    {
      "key": "paywall_consent_dialog_body",
      "value": "14-tägiges Widerrufsrecht (§ 355 BGB) — Verzicht auf Widerrufsrecht bei sofortiger Bereitstellung (§ 356 Abs. 5 BGB) [vollständiger Text siehe strings.xml:1105]",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1105,
      "category": "renewal"
    },
    {
      "key": "paywall_consent_dialog_checkbox",
      "value": "Ich stimme zu, dass die Bereitstellung der Premium-Funktionen sofort nach Bestellung beginnt. Mir ist bekannt, dass ich dadurch mein 14-tägiges Widerrufsrecht verliere.",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1106,
      "category": "renewal"
    },
    {
      "key": "paywall_consent_dialog_confirm",
      "value": "Jetzt zahlungspflichtig abonnieren",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1107,
      "category": "cta"
    },
    {
      "key": "paywall_exit_start_discount",
      "value": "Zahlungspflichtig — 2 Monate zum halben Preis",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1152,
      "category": "price"
    },
    {
      "key": "churn_auto_renew_note",
      "value": "Dein Abo verlängert sich automatisch über Google Play. Die Kündigung ist jederzeit möglich.",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1171,
      "category": "renewal"
    },
    {
      "key": "churn_cancel_sub",
      "value": "Abo kündigen",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1172,
      "category": "cancellation"
    },
    {
      "key": "follow_up_premium_subscribe",
      "value": "Abo starten",
      "file": "app/src/main/res/values/strings.xml",
      "line": 215,
      "category": "cta"
    },
    {
      "key": "retro_weekly_p4",
      "value": "Die ersten 2 Wochen sind kostenlos",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1028,
      "category": "trial"
    },
    {
      "key": "onboarding_free_after_trial",
      "value": "Danach nutzt du Best Journal weiter, kostenlos mit begrenzten KI-Aufrufen pro Woche.",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1085,
      "category": "trial"
    },
    {
      "key": "settings_revoke_confirm_body",
      "value": "Widerruf per App an dev.app.support@gmail.com — Verweis auf Google Play Kündigung [vollständiger Text strings.xml:752]",
      "file": "app/src/main/res/values/strings.xml",
      "line": 752,
      "category": "cancellation"
    },
    {
      "key": "paywall_lifetime_note",
      "value": "Kein Abo, keine Verlängerung",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1122,
      "category": "price"
    },
    {
      "key": "paywall_subscribe_button",
      "value": "Abonnieren",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1472,
      "category": "cta"
    },
    {
      "key": "paywall_title",
      "value": "Upgrade auf Premium",
      "file": "app/src/main/res/values/strings.xml",
      "line": 1474,
      "category": "cta"
    }
  ],
  "plugin_bugs_observed": [
    "Scope-Trennlinie fehlt: ai_prompt_*-Strings (interne KI-System-Prompts) enthalten dieselben HWG-Schlüsselwörter wie sichtbare UI-Strings — Scanner muss lernen, interne Prompt-Templates von nutzer-sichtbaren Strings zu trennen.",
    "Kein Auto-Renew-Hinweis vor dem Kauf: churn_auto_renew_note erscheint erst in der Kündigungs-Ansicht, nicht in der Paywall vor Kaufabschluss — potenzielle Lücke nach § 312 BGB und Google Play Billing Policy.",
    "Dynamische HWG-Risiko-Kaskade: onboarding_goal_stress ('Stress abbauen') wird als Ziel abgefragt und steuert via PaywallViewModel direkt die Paywall-Headline (paywall_headline_stress) — statischer Grep erkennt diese dynamische Verbindung nicht, Roentgen-Plugin braucht Datenfluss-Analyse."
  ]
}

out_path = "C:/Users/barwa/proggs/BestJournalAndroid/.android-shield/roentgen-frag-marketing.json"
tmp_path = out_path + ".tmp"
with open(tmp_path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
os.replace(tmp_path, out_path)

hwg = sum(1 for x in data["marketing_claims"] if x["risk"] == "HWG")
uwg = sum(1 for x in data["marketing_claims"] if x["risk"] == "UWG")
print(f"OK: {out_path}")
print(f"marketing_claims: {len(data['marketing_claims'])} (HWG: {hwg}, UWG: {uwg})")
print(f"paywall_abo: {len(data['paywall_abo'])}")

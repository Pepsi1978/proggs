import os, json

output_dir = r'C:\Users\barwa\proggs\BestJournalAndroid\.android-shield\worker-outputs'
os.makedirs(output_dir, exist_ok=True)

result = {
  "legalTextInventory": [
    {
      "area": "1_permission_rationale",
      "present": True,
      "wordings": [
        {"key": "permission_rationale_microphone", "text": "Damit du deine Tagebucheinträge per Sprache diktieren kannst, brauchen wir Zugriff auf dein Mikrofon.", "file_line": "app/src/main/res/values/strings.xml:696"},
        {"key": "permission_rationale_camera", "text": "Damit du Fotos zu deinen Einträgen hinzufügen kannst, brauchen wir Zugriff auf deine Kamera.", "file_line": "app/src/main/res/values/strings.xml:697"},
        {"key": "permission_rationale_notifications", "text": "Damit wir dich an dein tägliches Schreiben erinnern dürfen, brauchen wir die Erlaubnis für Benachrichtigungen.", "file_line": "app/src/main/res/values/strings.xml:698"}
      ],
      "gap": "Strings definiert, ABER kein shouldShowRationale()-Aufruf in Kotlin gefunden. CAMERA/RECORD_AUDIO/POST_NOTIFICATIONS werden per launcher.launch() ohne Rationale-Pruefung aufgerufen. Rationale-Dialoge werden moeglicherweise niemals angezeigt."
    },
    {
      "area": "2_consent_banner",
      "present": True,
      "wordings": [
        {"key": "consent_accept_all", "text": "Einverstanden und loslegen", "file_line": "app/src/main/res/values/strings.xml:650"},
        {"key": "consent_disable_stats", "text": "Statistiken deaktivieren und fortfahren", "file_line": "app/src/main/res/values/strings.xml:651"},
        {"key": "consent_btn_manual_selection", "text": "Manuelle Auswahl", "file_line": "app/src/main/res/values/strings.xml:660"},
        {"key": "consent_toggle_analytics_title", "text": "Anonyme Nutzungsanalyse", "file_line": "app/src/main/res/values/strings.xml:670"},
        {"key": "consent_toggle_analytics_body", "text": "Firebase Analytics — hilft uns die App zu verbessern. Keine persönlichen Inhalte, nur anonyme Klick-Zahlen. Datenübertragung: Google USA (Data Privacy Framework).", "file_line": "app/src/main/res/values/strings.xml:671"},
        {"key": "consent_toggle_gemini_title", "text": "KI-Reflexionen (Google Gemini)", "file_line": "app/src/main/res/values/strings.xml:674"},
        {"key": "consent_toggle_gemini_body", "text": "Dashboard-KI, Zusammenfassungen, Rückblicke und Textverbesserung. Sendet Textausschnitte an Google Gemini (USA). Deine Einträge werden NICHT zum Training verwendet.", "file_line": "app/src/main/res/values/strings.xml:675"}
      ],
      "gap": "Kein gleichrangiger Alles-Ablehnen-Button auf gleicher Ebene wie consent_accept_all. Nur consent_disable_stats (deaktiviert nur Stats) und Manuelle Auswahl. Moegliches DSGVO-Dark-Pattern."
    },
    {
      "area": "3_legal_links_ui",
      "present": True,
      "wordings": [
        {"key": "legal_title_datenschutz", "text": "Datenschutzerklärung", "file_line": "app/src/main/res/values/strings.xml:788"},
        {"key": "legal_title_nutzungsbedingungen", "text": "Nutzungsbedingungen", "file_line": "app/src/main/res/values/strings.xml:789"},
        {"key": "legal_title_impressum", "text": "Impressum", "file_line": "app/src/main/res/values/strings.xml:790"},
        {"key": "settings_legal_section_header", "text": "Rechtliche Dokumente", "file_line": "app/src/main/res/values/strings.xml:1480"},
        {"key": "settings_legal_online_url", "text": "https://pepsi1978.github.io/proggs/bestjournal/", "file_line": "app/src/main/res/values/strings.xml:1484"},
        {"key": "legal_url_datenschutz_de", "text": "https://pepsi1978.github.io/proggs/bestjournal/privacy-de.html", "file_line": "app/src/main/res/values/strings.xml:1487"},
        {"key": "legal_url_datenschutz_en", "text": "https://pepsi1978.github.io/proggs/bestjournal/privacy-en.html", "file_line": "app/src/main/res/values/strings.xml:1488"},
        {"key": "consent_links_header", "text": "Unsere Rechtstexte:", "file_line": "app/src/main/res/values/strings.xml:649"}
      ],
      "gap": ""
    },
    {
      "area": "4_health_disclaimer",
      "present": True,
      "wordings": [
        {"key": "ai_output_health_disclaimer", "text": "Keine Therapie — ersetzt keine professionelle Beratung.", "file_line": "app/src/main/res/values/strings.xml:1498"},
        {"key": "ai_output_health_disclaimer_long", "text": "Best Journal ist ein Tagebuch und kein Therapieangebot. KI-Analysen sind Anregungen zur Selbstreflexion und ersetzen keine professionelle psychologische oder medizinische Beratung. Bei akuten Problemen wende dich an eine Fachperson oder die Krisenhilfe in deinen Einstellungen.", "file_line": "app/src/main/res/values/strings.xml:1499"},
        {"key": "privacy_gate_gemini_body (Auszug)", "text": "Die KI-Ausgaben sind Vorschläge, keine professionelle Beratung und ersetzen keine Fachperson.", "file_line": "app/src/main/res/values/strings.xml:625"}
      ],
      "gap": "Vorhanden. Verwendet in AiOutputDisclaimer.kt:37 per stringResource(R.string.ai_output_health_disclaimer)."
    },
    {
      "area": "5_ai_disclaimer_eu_ai_act",
      "present": True,
      "wordings": [
        {"key": "ai_improved_suffix_marker", "text": "✨ KI-verbessert", "file_line": "app/src/main/res/values/strings.xml:178"},
        {"key": "ai_improved_suffix", "text": "\\n\\n✨ KI-verbessert", "file_line": "app/src/main/res/values/strings.xml:179"},
        {"key": "privacy_gate_gemini_body (Auszug Art.9)", "text": "Wichtiger Hinweis: Da deine Tagebucheinträge besonders sensible Daten enthalten können (Gesundheit, Religion, persönliche Beziehungen — Art. 9 DSGVO), bitten wir hier um deine ausdrückliche Zustimmung zur KI-Verarbeitung.", "file_line": "app/src/main/res/values/strings.xml:625"}
      ],
      "gap": "EU AI Act Art.50(4) Kennzeichnung vorhanden (Code-Kommentar strings.xml:175 explizit). Marker verwendet in AiImprovedFooter.kt:69."
    },
    {
      "area": "6_account_data_deletion",
      "present": True,
      "wordings": [
        {"key": "settings_delete_account_title", "text": "Konto und Daten löschen", "file_line": "app/src/main/res/values/strings.xml:730"},
        {"key": "settings_delete_account_subtitle", "text": "Entfernt alle lokalen Daten und das Drive-Backup unwiderruflich. Dein Google-Konto selbst bleibt bestehen.", "file_line": "app/src/main/res/values/strings.xml:731"},
        {"key": "settings_delete_account_confirm_title", "text": "Wirklich alle App-Daten löschen?", "file_line": "app/src/main/res/values/strings.xml:732"},
        {"key": "settings_delete_account_confirm_body", "text": "Dieser Vorgang ist unwiderruflich und löscht: alle lokalen Tagebucheinträge, Fotos, Videos und Audio-Aufnahmen; das Google-Drive-Backup der App; deine App-Anmeldung. Dein Google-Konto selbst bleibt bestehen.", "file_line": "app/src/main/res/values/strings.xml:733"},
        {"key": "settings_delete_account_confirm", "text": "Ja, alles löschen", "file_line": "app/src/main/res/values/strings.xml:735"}
      ],
      "gap": "Wort unwiderruflich dreifach verwendet (Z.731, Z.733, Z.740). Drive-Fehler-Dialog adressiert Teilloeschungs-Szenario ehrlich."
    },
    {
      "area": "7_newsletter_marketing_optin",
      "present": False,
      "wordings": [],
      "gap": "Kein Newsletter/Marketing-Opt-In in strings.xml oder Kotlin gefunden. App nutzt keine eigene E-Mail-Liste. Firebase Analytics Opt-In vorhanden. Kein Double-Opt-In-Flow implementiert oder erkennbar."
    },
    {
      "area": "8_inapp_purchase_confirmation",
      "present": True,
      "wordings": [
        {"key": "paywall_consent_dialog_title", "text": "Widerrufsbelehrung", "file_line": "app/src/main/res/values/strings.xml:1101"},
        {"key": "paywall_consent_dialog_checkbox", "text": "Ich stimme zu, dass die Bereitstellung der Premium-Funktionen sofort nach Bestellung beginnt. Mir ist bekannt, dass ich dadurch mein 14-tägiges Widerrufsrecht verliere.", "file_line": "app/src/main/res/values/strings.xml:1103"},
        {"key": "paywall_consent_dialog_confirm", "text": "Jetzt zahlungspflichtig abonnieren", "file_line": "app/src/main/res/values/strings.xml:1104"},
        {"key": "paywall_lifetime_title", "text": "Einmalkauf", "file_line": "app/src/main/res/values/strings.xml:1117"},
        {"key": "paywall_lifetime_desc", "text": "Einmal zahlen, alles nutzen", "file_line": "app/src/main/res/values/strings.xml:1118"}
      ],
      "gap": ""
    },
    {
      "area": "9_widerrufsbelehrung",
      "present": True,
      "wordings": [
        {"key": "paywall_consent_dialog_body", "text": "Du hast nach Abschluss eines Abos oder Kaufs grundsätzlich ein 14-tägiges Widerrufsrecht (§ 355 BGB). [...] Mit deiner Zustimmung unten verzichtest du in Kenntnis der Folgen auf dein 14-tägiges Widerrufsrecht (§ 356 Abs. 5 BGB).", "file_line": "app/src/main/res/values/strings.xml:1102"},
        {"key": "settings_revoke_title", "text": "Vertrag widerrufen", "file_line": "app/src/main/res/values/strings.xml:746"},
        {"key": "settings_revoke_subtitle", "text": "§ 356a BGB — Widerruf direkt per App", "file_line": "app/src/main/res/values/strings.xml:747"},
        {"key": "settings_revoke_confirm_body", "text": "Mit einem Klick auf „Widerruf bestätigen“ senden wir deinen Widerruf direkt an dev.app.support@gmail.com. Du bekommst sofort eine Eingangsbestätigung per E-Mail.", "file_line": "app/src/main/res/values/strings.xml:749"},
        {"key": "settings_revoke_email_body", "text": "Dieser Widerruf wurde zweistufig über den § 356a-BGB-konformen Widerrufsbutton der App ausgelöst und automatisch per Gmail-API versendet.", "file_line": "app/src/main/res/values/strings.xml:753"}
      ],
      "gap": "Sehr ausfuehrlich. §355, §356 Abs.5, §356a BGB alle adressiert. Zweistufiger Button in SettingsScreen.kt:3565 implementiert."
    },
    {
      "area": "10_age_verification",
      "present": False,
      "wordings": [],
      "gap": "Kein Mindestalter-String oder Altersverifikations-Dialog in strings.xml gefunden. Altersfreigabe nur ueber Play-Console-Rating. Kein In-App-Altersgate sichtbar."
    },
    {
      "area": "11_data_export_portability",
      "present": True,
      "wordings": [
        {"key": "settings_export", "text": "Exportieren", "file_line": "app/src/main/res/values/strings.xml:487"},
        {"key": "settings_export_pdf_desc", "text": "Exportiere alle Tagebucheinträge und Fotos als PDF-Dokument.", "file_line": "app/src/main/res/values/strings.xml:488"},
        {"key": "settings_export_data", "text": "Daten exportieren", "file_line": "app/src/main/res/values/strings.xml:519"},
        {"key": "settings_export_entries_photos_pdf_full", "text": "Tagebucheinträge mit Fotos als PDF exportieren", "file_line": "app/src/main/res/values/strings.xml:520"}
      ],
      "gap": "Nur PDF-Export vorhanden. Kein maschinenlesbarer Export (JSON/CSV) fuer DSGVO Art.20-Portabilitaet. Eingeschraenkte Portabilitaet im technischen Sinne."
    },
    {
      "area": "12_cloud_data_transparency",
      "present": True,
      "wordings": [
        {"key": "consent_card2_title", "text": "KI-Funktionen (USA)", "file_line": "app/src/main/res/values/strings.xml:613"},
        {"key": "consent_card2_body", "text": "Optional werden Texte an Google Gemini und Vorlese-Texte an Microsoft Edge in den USA gesendet (EU-US Data Privacy Framework). Sprachaufnahmen gehen an Groq (USA) mit Standardvertragsklauseln (EU SCCs).", "file_line": "app/src/main/res/values/strings.xml:614"},
        {"key": "settings_ai_groq_subtitle", "text": "Sprachaufnahmen zur Transkription an Groq USA senden", "file_line": "app/src/main/res/values/strings.xml:706"},
        {"key": "settings_ai_gemini_subtitle", "text": "Texte für KI-Zusammenfassungen an Google USA senden", "file_line": "app/src/main/res/values/strings.xml:708"},
        {"key": "settings_ai_tts_subtitle", "text": "Texte zur Sprachausgabe an Microsoft USA senden", "file_line": "app/src/main/res/values/strings.xml:710"}
      ],
      "gap": ""
    }
  ],
  "assetsLegal": {
    "perLang": {
      "de": ["DATENSCHUTZ.html", "IMPRESSUM.html", "NUTZUNGSBEDINGUNGEN.html"],
      "en": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "ko": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "ar": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "bn": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "es": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "fr": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "gu": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "hi": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "id": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "it": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "ja": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "kn": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "ml": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "mr": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "nl": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "pl": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "pt-BR": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "pt-PT": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "ta": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "te": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "th": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "tr": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "uk": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "ur": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "zh-CN": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"],
      "zh-TW": ["PRIVACY.html", "IMPRINT.html", "TERMS.html"]
    },
    "fullVersionLangs": ["de", "en", "ko"],
    "referenceLangs": ["ar","bn","es","fr","gu","hi","id","it","ja","kn","ml","mr","nl","pl","pt-BR","pt-PT","ta","te","th","tr","uk","ur","zh-CN","zh-TW"],
    "note": "de hat DATENSCHUTZ/IMPRESSUM/NUTZUNGSBEDINGUNGEN (deutsche Dateinamen). Alle 27 Sprachen haben alle 3 Dokumenttypen. de/en/ko = Vollversionen laut Frank-Kontext, restliche 24 = Verweis-/Kurzversionen."
  },
  "criticalGaps": [
    "AREA-1: permission_rationale_* Strings definiert aber shouldShowRationale() fehlt in Kotlin. CAMERA/RECORD_AUDIO/POST_NOTIFICATIONS per launcher.launch() ohne Rationale-Pruefung aufgerufen.",
    "AREA-2: Kein gleichrangiger Alles-Ablehnen-Button im Consent-Banner (Dark-Pattern-Risiko DSGVO Art.7 Abs.3).",
    "AREA-7: Kein Newsletter/Marketing-Opt-In implementiert (kein aktueller Bedarf erkennbar).",
    "AREA-10: Kein In-App-Altersverifikations-Dialog. Altersfreigabe nur ueber Play-Console-Rating.",
    "AREA-11: Nur PDF-Export. Kein maschinenlesbarer Export (JSON/CSV) fuer DSGVO Art.20-Portabilitaet im technischen Sinne."
  ],
  "observations": [
    "Widerrufsbelehrung besonders ausfuehrlich (§355/356/356a BGB) mit zweistufigem In-App-Button.",
    "AI-Disclaimer EU AI Act Art.50(4) explizit per Code-Kommentar in strings.xml:175 referenziert.",
    "Cloud-Datentransparenz (Bereich 12) vorbildlich: alle 3 Dienste (Gemini, Groq, Microsoft TTS) mit Zielland USA und Rechtsgrundlage (DPF/SCCs).",
    "assets/legal: Alle 27 Sprachen haben alle 3 Dokumenttypen vorhanden.",
    "Drive-Fehler-Dialog adressiert Teilloeschungs-Szenario ehrlich mit 3 Optionen (Retry/NurLokal/Abbrechen).",
    "Consent-Banner hat 3 Optionen: Einverstanden / Statistiken deaktivieren / Manuelle Auswahl. Fehlt nur expliziter Alles-Ablehnen-Button."
  ],
  "plugin_bugs_observed": [
    "Kotlin-Quelldateien liegen unter app/src/main/java/ nicht unter app/src/main/kotlin/ — Grep auf falschen Unterpfad lieferte anfangs keine Treffer.",
    "permission_rationale_* Strings in strings.xml definiert, aber kein shouldShowRationale()-Aufruf in Kotlin gefunden. Moeglicherweise tote Strings oder fehlende Implementierung.",
    "Bash -c mit einfachen Hochkommas im Python-String schlaegt fehl (exit code 2, unexpected EOF). Fix: Python-Code in separate .py-Datei auslagern und per python3 Datei ausfuehren."
  ]
}

out_path = os.path.join(output_dir, "roentgen-R4.json")
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

print("Geschrieben:", out_path)
print("Groesse:", os.path.getsize(out_path), "Bytes")

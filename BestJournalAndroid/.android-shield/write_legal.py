import os, json

output = {
  "legal_assets": {
    "de": ["DATENSCHUTZ", "IMPRESSUM", "NUTZUNGSBEDINGUNGEN"],
    "ar": ["IMPRINT", "PRIVACY", "TERMS"],
    "bn": ["IMPRINT", "PRIVACY", "TERMS"],
    "en": ["IMPRINT", "PRIVACY", "TERMS"],
    "es": ["IMPRINT", "PRIVACY", "TERMS"],
    "fr": ["IMPRINT", "PRIVACY", "TERMS"],
    "gu": ["IMPRINT", "PRIVACY", "TERMS"],
    "hi": ["IMPRINT", "PRIVACY", "TERMS"],
    "id": ["IMPRINT", "PRIVACY", "TERMS"],
    "it": ["IMPRINT", "PRIVACY", "TERMS"],
    "ja": ["IMPRINT", "PRIVACY", "TERMS"],
    "kn": ["IMPRINT", "PRIVACY", "TERMS"],
    "ko": ["IMPRINT", "PRIVACY", "TERMS"],
    "ml": ["IMPRINT", "PRIVACY", "TERMS"],
    "mr": ["IMPRINT", "PRIVACY", "TERMS"],
    "nl": ["IMPRINT", "PRIVACY", "TERMS"],
    "pl": ["IMPRINT", "PRIVACY", "TERMS"],
    "pt-BR": ["IMPRINT", "PRIVACY", "TERMS"],
    "pt-PT": ["IMPRINT", "PRIVACY", "TERMS"],
    "ta": ["IMPRINT", "PRIVACY", "TERMS"],
    "te": ["IMPRINT", "PRIVACY", "TERMS"],
    "th": ["IMPRINT", "PRIVACY", "TERMS"],
    "tr": ["IMPRINT", "PRIVACY", "TERMS"],
    "uk": ["IMPRINT", "PRIVACY", "TERMS"],
    "ur": ["IMPRINT", "PRIVACY", "TERMS"],
    "zh-CN": ["IMPRINT", "PRIVACY", "TERMS"],
    "zh-TW": ["IMPRINT", "PRIVACY", "TERMS"]
  },
  "missing_legal_langs": [],
  "datenschutz_de_coverage": {
    "art9_moodtag": True,
    "standort": True,
    "churn_survey": True,
    "note": "DE-DATENSCHUTZ deckt alle drei Pflichtpunkte ab. Art.9 DSGVO: Sprachaufnahmen als besonders sensible Daten explizit genannt (Abschnitt Cloud-Transkription). Standort: ACCESS_COARSE_LOCATION-Abschnitt 3.4 vorhanden. Churn: Kuendigungsgrund-Erfassung via AnalyticsTracker referenziert."
  },
  "other_langs_privacy_updated": {
    "en": True,
    "ko": True,
    "pt-BR": True,
    "note": "EN und KO enthalten Art.9 GDPR-Entsprechung, Standort- und Churn-Abschnitte (identisch mit DE-Niveau). pt-BR hat Sonderfall: Art.11 LGPD + Art.9 RGPD Doppelreferenz. Die 23 uebrigen Sprachen (ar, bn, es, fr, gu, hi, id, it, ja, kn, ml, mr, nl, pl, pt-PT, ta, te, th, tr, uk, ur, zh-CN, zh-TW) haben KEINEN Art.9 / Churn-Survey-Abschnitt - konsistent mit Commit-#945 (nur DE/EN/KO ergaenzt). pt-BR ist ein undokumentierter Sonderfall."
  },
  "external_legal_url": {
    "base": "https://pepsi1978.github.io/proggs/bestjournal/",
    "langs": ["de", "en", "ko"],
    "documents": ["privacy", "agb", "imprint"],
    "linked_from": [
      "ConsentScreen.kt (strings.xml: legal_url_datenschutz_de/en/ko, legal_url_agb_de/en/ko, legal_url_impressum_de/en/ko) - Zeilen 566-582",
      "SettingsScreen.kt:4086 (settings_legal_online_url als allgemeine Fallback-URL)"
    ],
    "note": "Nur DE/EN/KO als externe Online-Versionen per GitHub Pages verlinkt. Alle anderen 24 Sprachen erhalten ausschliesslich die lokale assets/legal/-Offline-Version."
  },
  "data_collection": [
    {
      "type": "Firebase Analytics",
      "evidence": "app/src/main/java/com/bestjournal/app/BestJournalApp.kt:14",
      "personal_or_health": False,
      "detail": "Verhaltens-Events via AnalyticsTracker: trackChurnFlowOpened, trackChurnOfferShown, trackChurnReasonSelected, trackChurnOfferAccepted, trackChurnPaused, trackChurnConfirmed. Kein GAID/Werbe-ID festgestellt."
    },
    {
      "type": "Standort ACCESS_COARSE_LOCATION",
      "evidence": "app/src/main/AndroidManifest.xml:8",
      "personal_or_health": True,
      "detail": "Ungefaehre Standorterfassung. Zweck in DE-DATENSCHUTZ Abschnitt 3.4 dokumentiert. Art.9-relevanter Kontext fuer Stimmungs-/Befindlichkeitskorrelation."
    },
    {
      "type": "moodTag Stimmungsdaten",
      "evidence": "app/src/main/java/com/bestjournal/app/data/local/AppDatabase.kt:179",
      "personal_or_health": True,
      "detail": "Stimmungs-Tag (moodTag TEXT) in lokaler Room-Datenbank. Als besonders schutzwuerdige Gesundheits-/Befindlichkeitsdaten einzustufen (Art.9 DSGVO relevant). Wird via Drive-Backup in Google Drive des Nutzers gesichert."
    },
    {
      "type": "Churn-Survey Kuendigungs-Befragung",
      "evidence": "app/src/main/java/com/bestjournal/app/ui/screens/settings/ChurnFlowDialog.kt:110",
      "personal_or_health": False,
      "detail": "Kuendigungsgrund inkl. optionalem Freitext (Anderer Grund) wird per AnalyticsTracker an Firebase uebermittelt (trackChurnReasonSelected). Verhaltens-/Praeferenzdaten ohne direkte Personenidentifikation."
    },
    {
      "type": "Google Drive Backup Tagebuch-Datenbank",
      "evidence": "app/src/main/java/com/bestjournal/app/data/remote/googledrive/DriveBackupManager.kt:79",
      "personal_or_health": True,
      "detail": "Komplette Room-Datenbank (Tagebucheintraege, moodTags, alle Nutzerdaten) wird als Binaerdatei in den Google Drive App-Data-Ordner des Nutzers hochgeladen. Hoechste Sensibilitaet - enthaelt alle erfassten Gesundheits-/Stimmungsdaten."
    },
    {
      "type": "Audio-Aufnahmen RECORD_AUDIO",
      "evidence": "app/src/main/AndroidManifest.xml:6",
      "personal_or_health": True,
      "detail": "Mikrofon-Permission fuer Whisper-Sprachtranskription. In DE-DATENSCHUTZ explizit als Art.9 DSGVO-relevante besonders sensible Daten markiert. Cloud-Transkription ist optional mit Einwilligung."
    },
    {
      "type": "Kamera CAMERA",
      "evidence": "app/src/main/AndroidManifest.xml:7",
      "personal_or_health": True,
      "detail": "Kamera-Permission fuer Foto-Eintraege im Tagebuch. Bilder koennen besonders schutzwuerdige Inhalte enthalten."
    },
    {
      "type": "In-App-Billing Google Play",
      "evidence": "app/src/main/java/com/bestjournal/app/billing/BillingManager.kt:53",
      "personal_or_health": False,
      "detail": "Kauf-Transaktionen ueber Google Play Billing. Abwicklung durch Google, keine Zahlungsdaten direkt in der App gespeichert."
    }
  ],
  "plugin_bugs_observed": [
    {
      "bug": "Windows cp1252 UnicodeEncodeError bei Python-Ausgabe mit Sonderzeichen (100% Match)",
      "fix_applied": "sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')",
      "severity": "hoch",
      "note": "Bekannter Fehler aus bug-cases.jsonl. Fix war korrekt und sofort angewendet."
    },
    {
      "bug": "SyntaxWarning invalid escape sequence bei grep-Pipe-Alternation mit Backslash-Pipe in Python-String",
      "fix_applied": "Separate grep-Aufrufe statt Backslash-Pipe als Alternation-Operator",
      "severity": "niedrig"
    },
    {
      "bug": "Bash unexpected EOF - grosses Python-Inline-Skript mit verschachtelten Anfuehrungszeichen bricht bash -c Parsing",
      "fix_applied": "Python-Code als Skript-Datei schreiben und python3 Datei.py aufrufen statt -c Inline",
      "severity": "mittel",
      "note": "Bekannter Fehler (100% Match). Root Cause: Einfache Anfuehrungszeichen im Python-String-Literal beenden den bash -c Einzelquote-Block."
    },
    {
      "observation": "pt-BR PRIVACY.html hat Art.9/LGPD-Doppelreferenz (Art.11 LGPD + Art.9 RGPD) - vermutlich separat aktualisiert, obwohl Commit-#945 nur DE/EN/KO erwaehnt. Moeglicherweise undokumentierter vierter Update-Empfaenger."
    }
  ]
}

out_dir = "C:/Users/barwa/proggs/BestJournalAndroid/.android-shield"
os.makedirs(out_dir, exist_ok=True)
out_path = out_dir + "/roentgen-frag-legal.json"
tmp_path = out_path + ".tmp"

with open(tmp_path, "w", encoding="utf-8") as f:
    json.dump(output, f, ensure_ascii=False, indent=2)
os.replace(tmp_path, out_path)
print("OK:", out_path)
print("Bytes:", os.path.getsize(out_path))

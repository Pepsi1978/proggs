import os, json, tempfile

sizematrix = json.load(open(os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/w7_sizematrix.json'), encoding='utf-8'))

# normalize size matrix into {locale:{privacy,imprint,terms}}
def norm(loc, files):
    out = {}
    for fn, sz in files.items():
        low = fn.lower()
        if 'privacy' in low or 'datenschutz' in low:
            out['privacy'] = sz
        elif 'imprint' in low or 'impressum' in low:
            out['imprint'] = sz
        elif 'terms' in low or 'nutzungsbedingungen' in low:
            out['terms'] = sz
    return out

size_norm = {loc: norm(loc, f) for loc, f in sizematrix.items()}

out = {
  "worker": "w7-hidden-assets",
  "hiddenFeatures": {
    "receivers": [
      {"name": "BootReminderReceiver", "purpose": "Re-schedules all alarms after BOOT_COMPLETED / TIMEZONE_CHANGED / TIME_CHANGED via DailyReminderManager (daily + weekly + monthly + yearly).", "notifications": "none directly (re-scheduler only)", "manifestRegistered": False, "note": "not in manifest grep result - verify registration"},
      {"name": "ReminderReceiver", "purpose": "Daily journal reminder; picks 1 of 5 rotating message bodies; reschedules next 24h alarm; logs FirebaseAnalytics 'reminder_notification_shown'.", "channelId": "daily_reminder", "notificationId": 2001, "manifestRegistered": True, "exported": False},
      {"name": "WeeklyReviewReceiver", "purpose": "Weekly review notification; opens app tab 0; logs 'weekly_review_notification_shown'.", "channelId": "weekly_review", "notificationId": 2002},
      {"name": "MonthlyReviewReceiver", "purpose": "Monthly review notification + reschedules next month (fromUserToggle=false).", "channelId": "monthly_review", "notificationId": 2003},
      {"name": "YearlyReviewReceiver", "purpose": "Yearly review notification + reschedules next year (fromUserToggle=false).", "channelId": "yearly_review", "notificationId": 2004}
    ],
    "notificationChannels": [
      {"id": "daily_reminder", "name": "Taegliche Erinnerung", "purpose": "Daily journaling reminder"},
      {"id": "weekly_review", "name": "Woechentlicher Rueckblick", "purpose": "Weekly review prompt"},
      {"id": "monthly_review", "name": "Monatsrueckblick", "purpose": "Monthly review prompt"},
      {"id": "yearly_review", "name": "Jahresrueckblick", "purpose": "Yearly review prompt"}
    ],
    "channelCreation": "BestJournalApp.createNotificationChannels() onCreate, guarded by SDK>=O",
    "debugOnlyFeatures": [
      {"feature": "Test-Daten-Generator button (seed fake journal entries) + 'Alle Einträge löschen' action (Commit #1289)", "file": "JournalScreen.kt:334 (IconButton) + :183 (AlertDialog)", "guardVerified": True, "note": "DOUBLE-guarded: both the toolbar IconButton AND the dialog are inside `if (BuildConfig.DEBUG)`. seedTestData()/deleteAllEntriesNow() reachable ONLY from this debug dialog. NO Play-Store risk - fake-data generator cannot appear in release build."},
      {"feature": "OkHttp logging interceptor Level.BODY", "file": "di/NetworkModule.kt:32", "guardVerified": True, "note": "BODY logging only in DEBUG, NONE in release"},
      {"feature": "Firebase App Check debug provider", "file": "BestJournalApp.kt:64", "guardVerified": True, "note": "debug provider via reflection only in DEBUG"},
      {"feature": "AI paywall bypass (isDebug allows transcription without subscription/trial)", "file": "data/repository/TranscriptionRepository.kt:74", "guardVerified": True, "note": "isDebug=BuildConfig.DEBUG used to bypass paywall in debug only - correct"}
    ],
    "accountDeletion": {
      "present": True,
      "path": "Settings -> 'Konto löschen' -> deleteAccount(); confirm dialog -> progress dialog -> app restart",
      "wording": "Title: 'Wirklich alle App-Daten löschen?' | Body: 'Dieser Vorgang ist unwiderruflich und löscht: Alle lokalen Tagebucheinträge, Fotos, Videos und Audio-Aufnahmen; Das Google-Drive-Backup der App (nur die App-Daten, nicht dein Google-Konto); Deine App-Anmeldung. Dein Google-Konto selbst bleibt bestehen. Die App startet danach neu als frische Installation.' | Confirm: 'Ja, alles löschen'",
      "cloudDataCovered": "YES - DriveBackupManager.deleteAllAppData() runs FIRST and is VERIFIED (re-lists appDataFolder, fails loudly if files remain). On Drive-delete failure: honest error dialog ('Ohne vollständige Löschung wäre die Meldung „unwiderruflich entfernt\" nicht korrekt') with retry / local-only / abort options. Then wipes filesDir/photos, cacheDir (incl drive_merge_temp.db full DB copy), Room DBs, encrypted prefs, alarms via signOut().",
      "gaps": ["BootReminderReceiver/review-receivers manifest registration not seen in this audit (only ReminderReceiver exported=false found) - verify the 4 review receivers are declared or scheduled purely via explicit PendingIntent (latter does not require manifest entry).", "Google Play Console also requires a web-based account-deletion URL (out of code scope - tracked separately)."]
    },
    "dataExport": {"present": True, "type": "PDF (journal entries + optional photos)", "path": "Settings -> 'Daten exportieren' / 'PDF-Export' -> exportToPdf() -> PdfExporter.export()", "premiumGated": True, "note": "Art. 20 portability satisfied; PDF export is a premium feature (paywall_feature_pdf). No JSON export."},
    "widgets": "NONE - no GlanceAppWidget, TileService, AppWidgetProvider or ShortcutManager found",
    "featureFlags": "FirebaseRemoteConfig used ONLY in TranscriptionRepository (Whisper model/gating), NOT a general feature-flag/remote-kill system",
    "fileProviderPaths": ["files-path name='photos' path='photos/' (authority ${applicationId}.fileprovider) - shares ONLY photos dir, narrow scope, no cacheDir/files over-share"]
  },
  "layer1_5_assets": {
    "localeFolders": 27,
    "totalFiles": 81,
    "sizeMatrix": size_norm,
    "fullVersions": ["de", "en", "ko", "ja", "pt-BR"],
    "fullVersionsNote": "User stated 3 full versions (de/en/ko). REALITY: 5 full standalone PRIVACY versions by byte-size: de(48991) en(54666) ko(72107) ja(74204) pt-BR(67958). ja+pt-BR have NO reference-link and are self-contained full policies. This EXCEEDS the stated baseline (more coverage, not a defect). The other 22 locales are genuine short summaries (5.5-10.2KB).",
    "referenceArchitectureVerified": {
      "fr": {"hasReferenceLink": True, "linkTarget": "file:///android_asset/legal/en/PRIVACY.html + de/DATENSCHUTZ.html", "linkLanguage": "fr", "clauseFound": True, "clause": "'consulte la version complète en anglais ou en allemand'"},
      "ja": {"hasReferenceLink": False, "linkTarget": "(none - external regulator/helpline links)", "isFullStandalone": True, "clauseFound": False, "note": "FULL 74KB policy, not a summary"},
      "ur": {"hasReferenceLink": True, "linkTarget": "en/PRIVACY + de/DATENSCHUTZ", "linkLanguage": "ur", "clauseFound": True, "clause": "'مکمل انگریزی یا جرمن ورژن دیکھیں'"},
      "pt-BR": {"hasReferenceLink": False, "linkTarget": "(none - ANPD/CVV links)", "isFullStandalone": True, "clauseFound": False, "note": "FULL 68KB policy, not a summary"},
      "zh-CN": {"hasReferenceLink": True, "linkTarget": "en/PRIVACY + de/DATENSCHUTZ", "linkLanguage": "zh", "clauseFound": True, "clause": "'完整英文版或德文版'"},
      "gu": {"hasReferenceLink": True, "linkTarget": "en/PRIVACY + de/DATENSCHUTZ", "linkLanguage": "gu", "clauseFound": True, "clause": "'અંગ્રેજી અથવા જર્મન સંપૂર્ણ સંસ્કરણ'"}
    },
    "loaderLogic": {
      "file": "ui/screens/consent/LegalDocumentScreen.kt (enum LegalDocument)",
      "localeMapping": "4-tier: de->legal/de/<de-name>; en->legal/en/<en-name>; other UI lang->localized summary via summaryFolderFor(); else->en full fallback. Handles pt-BR/pt-PT, zh CN+SG->zh-CN HK/MO/TW->zh-TW, legacy in->id.",
      "fallback": "onlyIfTranslated() gate + TRANSLATED_SUMMARIES set (25 entries) guarantees no blank WebView; unknown locale falls back to en full versions (Poka-Yoke)",
      "security": "WebView JS disabled, defaultTextEncoding utf-8, only file:// loads in-WebView, external (mailto/https) open in system browser",
      "reachableFrom": ["ConsentScreen (onboarding, lines 365-372: Datenschutz/Nutzungsbedingungen/Impressum)", "SettingsScreen (line 4059, footer order Datenschutz/Nutzungsbedingungen/Impressum)", "routes legal/datenschutz, legal/nutzungsbedingungen, legal/impressum"],
      "docMismatch": "Inline doc comment (lines 64-73) describes a 3-tier model (de+en full, rest summaries) but ja & pt-BR are actually full standalone docs loaded through the tier-3 path. Functionally correct (full file loads regardless); documentation is just slightly behind reality."
    },
    "deVollversionChecks": {
      "datenschutz": {"verantwortlicher": True, "anschrift": True, "zweck": True, "rechtsgrundlagen": True, "empfaenger": True, "drittland": True, "subprocessorsNamed": "Google, Gemini, Groq, Firebase all present", "speicherdauer": True, "betroffenenrechte": True, "datenuebertragbarkeit_art20": True, "beschwerderecht": True, "kontakt": True},
      "impressum": {"name": True, "anschrift": True, "email": True, "ddg_para5": True},
      "nutzungsbedingungen": {"widerrufsbelehrung": True, "frist_14_tage": True, "musterFormular": True, "para_312g_bgb": True, "digitaleInhalte_clause": True}
    },
    "whisperFolder": {"path": "assets/whisper/", "files": ["base-decoder.int8.onnx (~130 MB)", "base-encoder.int8.onnx (~29 MB)", "base-tokens.txt (~817 KB)"], "purpose": "sherpa-onnx Whisper 'base' int8 model for on-device offline speech-to-text (privacy-preserving local transcription, no cloud)"}
  },
  "criticalObservations": [
    "POSITIVE: Debug-Test-Daten-Generator + 'Alle Einträge löschen' are DOUBLE-guarded by BuildConfig.DEBUG (JournalScreen.kt:183 + :334). Fake-data generator and bulk-delete cannot appear in a release build - NO Play-Store risk.",
    "POSITIVE: Account deletion (DSGVO Art.17) is best-in-class: Drive cloud backup deleted FIRST + VERIFIED, honest error dialog never falsely claims 'unwiderruflich entfernt', wipes local photos/cache (incl full DB copy in drive_merge_temp.db)/Room/prefs/alarms.",
    "POSITIVE: DE full legal docs are complete - DATENSCHUTZ has all DSGVO sections incl named subprocessors (Google/Gemini/Groq/Firebase) + Drittland/SCC; IMPRESSUM §5 DDG; NUTZUNGSBEDINGUNGEN §312g BGB Widerrufsbelehrung + Muster-Formular.",
    "FACTUAL CORRECTION: 5 full PRIVACY versions exist (de/en/ko/ja/pt-BR), not 3 as stated. ja+pt-BR are self-contained full policies WITHOUT a reference link. This is MORE coverage, not less - the FIN-050 reference architecture is intact for the remaining 22 short summaries (all verified to carry working en+de links + native-language conflict clause).",
    "MINOR DOC DRIFT: LegalDocumentScreen.kt comment (lines 64-73) still describes a 3-tier/summaries model; ja+pt-BR are full docs loaded via the summary path. Cosmetic only - loading works correctly. Could realign comment for accuracy.",
    "FileProvider shares ONLY the photos/ dir (narrow, secure). PDF export is the only data-export path (Art.20) and is premium-gated; there is no JSON export."
  ],
  "plugin_bugs_observed": [
    "FIN-OBS-W7-1: Inline Bash heredoc (Bash tool) mangled the Python escape sequence rel.replace('\\\\','/') into a single backslash, causing a SyntaxError - the known-error hook matched it at 67%. Workaround: write Python to a file in the write-zone and run it. Recommendation: worker prompt should instruct 'write Python scripts to .android-shield/*.py and execute, never inline heredoc with backslash escapes on Windows Git-Bash'.",
    "FIN-OBS-W7-2: Grep tool requires an ABSOLUTE path on Windows; the relative app/src/main/java path failed because cwd is C:/Users/barwa/proggs (parent of APP-ROOT). The worker prompt gives APP-ROOT but the harness cwd is one level up - prompts should state that Grep/Glob need the full C:/.../BestJournalAndroid/... path.",
    "FIN-OBS-W7-3: Worker baseline assumption ('3 full legal versions') was outdated vs. code reality (5). Audit prompts that hard-code a baseline count should phrase it as 'verify the count' (which this one did) rather than 'confirm 3' - good prompt design; keep it."
  ]
}

dst = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield/worker-outputs/phase1a-hidden-assets.json')
fd, tmp = tempfile.mkstemp(dir=os.path.dirname(dst), suffix='.tmp')
with os.fdopen(fd, 'w', encoding='utf-8') as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
os.replace(tmp, dst)
print("WROTE", dst, os.path.getsize(dst), "bytes")
# validate
json.load(open(dst, encoding='utf-8'))
print("JSON valid")

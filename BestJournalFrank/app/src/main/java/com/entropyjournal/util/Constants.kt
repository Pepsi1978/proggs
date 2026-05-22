package com.entropyjournal.util

object Constants {
    // API Base URLs
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    // Groq
    const val GROQ_TRANSCRIPTION_MODEL = "whisper-large-v3-turbo"
    const val GROQ_LANGUAGE = "de"

    // Audio recording
    const val DEFAULT_MAX_RECORDING_DURATION_MINUTES = 15
    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_CHANNELS = 1

    // Sync
    const val SYNC_DEBOUNCE_MS = 30_000L
    const val ANALYSIS_DEBOUNCE_MS = 60_000L
    const val DRIVE_BACKUP_FILENAME = "entropy_journal_backup.db"
    const val DRIVE_PHOTOS_FILENAME = "entropy_journal_photos.zip"

    // SharedPreferences keys
    const val PREF_BACKUP_PHOTOS = "backup_photos"
    const val PREF_BACKUP_VIDEOS = "backup_videos"
    const val PREF_GROQ_API_KEY = "groq_api_key"
    const val PREF_GEMINI_API_KEY = "gemini_api_key"
    const val PREF_GEMINI_MODEL = "gemini_model"
    const val PREF_TEXT_IMPROVEMENT_DEFAULT = "text_improvement_default"
    const val PREF_MAX_RECORDING_DURATION = "max_recording_duration"
    const val PREF_AUTO_UPDATE_DASHBOARD = "auto_update_dashboard"
    const val PREF_VERBOSE_DASHBOARD = "verbose_dashboard"
    const val PREF_VERBOSE_DASHBOARD_CHANGED_AT = "verbose_dashboard_changed_at"

    /**
     * Signatur (scenario | verbose | customPromptSavedAt) der zuletzt erfolgreich
     * generierten Rueckblicke. Wird beim ViewModel-Init gegen den aktuellen Wert
     * verglichen — bei Abweichung werden ALLE bestehenden Rueckblicke geloescht und
     * neu generiert. Verhindert dass alte Rueckblicke aus einem anderen Profil oder
     * Variant-Setting in der Datenbank bleiben (Duplikate).
     */
    const val PREF_RETRO_LAST_GENERATED_SIGNATURE = "retro_last_generated_signature"
    const val PREF_GOOGLE_ACCOUNT_NAME = "google_account_name"
    const val PREF_GOOGLE_ACCOUNT_EMAIL = "google_account_email"
    const val PREF_GOOGLE_AVATAR_URL = "google_avatar_url"
    const val PREF_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val PREF_SYNC_IN_PROGRESS = "sync_in_progress"
    const val PREF_RESTORE_PENDING = "restore_pending"
    const val PREF_DARK_THEME = "dark_theme"
    const val PREF_THEME_FOLLOW_SYSTEM = "theme_follow_system"
    const val PREF_THEME_FOLLOW_SUN = "theme_follow_sun"
    const val PREF_APP_THEME = "app_theme"
    const val PREF_LATITUDE = "location_latitude"
    const val PREF_LONGITUDE = "location_longitude"
    const val PREF_BIOMETRIC_LOCK = "biometric_lock"
    const val PREF_DASHBOARD_LAST_UPDATED = "dashboard_last_updated"
    const val PREF_DASHBOARD_UPDATING = "dashboard_updating"
    const val PREF_DASHBOARD_UPDATE_IS_DELETE = "dashboard_update_is_delete"

    // Google OAuth
    const val GOOGLE_WEB_CLIENT_ID =
        "674560807048-l6ktqsucjr4ld91srdc6assgfiks19mh.apps.googleusercontent.com"

    // Encrypted SharedPreferences file name
    const val ENCRYPTED_PREFS_NAME = "entropy_journal_secure_prefs"

    // Töne
    const val PREF_SOUNDS_ENABLED = "sounds_enabled"

    // Haptik
    const val PREF_HAPTIC_ENABLED = "haptic_enabled"

    // TTS (Text-to-Speech)
    const val PREF_TTS_ENABLED = "tts_enabled"
    const val PREF_TTS_FAVORITES = "tts_favorite_voices"
    const val PREF_TTS_PROVIDER = "tts_provider"
    const val TTS_PROVIDER_EDGE = "edge_tts"
    const val TTS_PROVIDER_ELEVENLABS = "elevenlabs"
    const val TTS_PROVIDER_GOOGLE = "google_cloud"

    // Edge TTS voices
    const val PREF_EDGE_TTS_VOICE = "edge_tts_voice"
    const val DEFAULT_EDGE_TTS_VOICE = "de-DE-SeraphinaMultilingualNeural"

    data class EdgeTtsVoice(val id: String, val name: String)

    val EDGE_TTS_VOICES =
        listOf(
            EdgeTtsVoice(
                "de-DE-SeraphinaMultilingualNeural",
                "\u2605 Seraphina \u2014 weiblich",
            ),
            EdgeTtsVoice(
                "de-DE-FlorianMultilingualNeural",
                "\u2605 Florian \u2014 m\u00e4nnlich",
            ),
            EdgeTtsVoice("de-DE-KatjaNeural", "Katja \u2014 weiblich, warm"),
            EdgeTtsVoice("de-DE-KillianNeural", "Killian \u2014 m\u00e4nnlich, warm"),
            EdgeTtsVoice("de-DE-ConradNeural", "Conrad \u2014 m\u00e4nnlich, klar"),
            EdgeTtsVoice("de-DE-AmalaNeural", "Amala \u2014 weiblich, jung"),
        )

    // Google Cloud TTS (Chirp 3 HD)
    const val GOOGLE_TTS_BASE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
    const val PREF_GOOGLE_TTS_API_KEY = "google_tts_api_key"
    const val PREF_GOOGLE_TTS_VOICE = "google_tts_voice"
    const val DEFAULT_GOOGLE_TTS_VOICE = "de-DE-Chirp3-HD-Kore"

    data class GoogleTtsVoice(val id: String, val name: String)

    val GOOGLE_TTS_VOICES =
        listOf(
            // Female voices
            GoogleTtsVoice("de-DE-Chirp3-HD-Achernar", "Achernar \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Aoede", "Aoede \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Autonoe", "Autonoe \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Callirrhoe", "Callirrhoe \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Despina", "Despina \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Erinome", "Erinome \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Gacrux", "Gacrux \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Kore", "Kore \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Laomedeia", "Laomedeia \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Leda", "Leda \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Pulcherrima", "Pulcherrima \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Sulafat", "Sulafat \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Vindemiatrix", "Vindemiatrix \u2014 weiblich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Zephyr", "Zephyr \u2014 weiblich"),
            // Male voices
            GoogleTtsVoice("de-DE-Chirp3-HD-Achird", "Achird \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Algenib", "Algenib \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Algieba", "Algieba \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Alnilam", "Alnilam \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Charon", "Charon \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Enceladus", "Enceladus \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Fenrir", "Fenrir \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Iapetus", "Iapetus \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Orus", "Orus \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Puck", "Puck \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Rasalgethi", "Rasalgethi \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Sadachbia", "Sadachbia \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Sadaltager", "Sadaltager \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Schedar", "Schedar \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Umbriel", "Umbriel \u2014 m\u00e4nnlich"),
            GoogleTtsVoice("de-DE-Chirp3-HD-Zubenelgenubi", "Zubenelgenubi \u2014 m\u00e4nnlich"),
        )

    // ElevenLabs TTS
    const val ELEVENLABS_BASE_URL = "https://api.elevenlabs.io/v1"
    const val PREF_ELEVENLABS_API_KEY = "elevenlabs_api_key"
    const val PREF_ELEVENLABS_VOICE_ID = "elevenlabs_voice_id"

    data class ElevenLabsVoice(val id: String, val name: String)

    val ELEVENLABS_VOICES = listOf(
        ElevenLabsVoice("TX3LPaxmHKxFdv7VOQHJ", "Liam — jung, klar"),
        ElevenLabsVoice("pNInz6obpgDQGcFmaJgB", "Adam — tief, ruhig"),
        ElevenLabsVoice("ErXwobaYiN019PkySvjV", "Antoni — warm, nat\u00fcrlich"),
        ElevenLabsVoice("onwK4e9ZLuTAKqWW03F9", "Daniel — klar, sachlich"),
        ElevenLabsVoice("nPczCjzI2devNBz1zQrb", "Brian — erz\u00e4hlend, tief"),
        ElevenLabsVoice("N2lVS1w4EtoT3dr4eOWO", "Callum — ruhig, sanft"),
        ElevenLabsVoice("IKne3meq5aSn9XLyUdCD", "Charlie — locker, freundlich"),
        ElevenLabsVoice("iP95p4xoKVk53GoZ742B", "Chris — lebendig, energisch"),
        ElevenLabsVoice("JBFqnCBsd6RMkjVDRZzb", "George — reif, w\u00fcrdevoll"),
        ElevenLabsVoice("SOYHLrjzK2X1ezoPC6cr", "Harry — kraftvoll, markant"),
        ElevenLabsVoice("TxGEqnHWrfWFTfGW9XjX", "Josh — jung, dynamisch"),
        ElevenLabsVoice("GBv7mTt0atIp3Br8iCZE", "Thomas — ruhig, vertrauensvoll"),
        ElevenLabsVoice("EXAVITQu4vr4xnSDxMaL", "Sarah — freundlich, nat\u00fcrlich"),
        ElevenLabsVoice("XrExE9yKIg1WjnnlVkGX", "Matilda — sanft, warm"),
        ElevenLabsVoice("jsCqWAovK2LkecY7zXl4", "Freya — ruhig, gelassen"),
        ElevenLabsVoice("pMsXgVXv3BLzUgSXRplE", "Serena — weich, beruhigend"),
        ElevenLabsVoice("Xb7hH8MSUJpSbSDYk0k2", "Alice — elegant, klar"),
        ElevenLabsVoice("XB0fDUnXU5powFXDhCwa", "Charlotte — lebhaft, warm"),
        ElevenLabsVoice("pFZP5JQG7iQjIQuC4Bku", "Lily — hell, fr\u00f6hlich"),
        ElevenLabsVoice("21m00Tcm4TlvDq8ikWAM", "Rachel — vielseitig, ausdrucksstark"),
    )

    // Dashboard-Szenario
    const val PREF_DASHBOARD_SCENARIO = "dashboard_scenario"
    const val PREF_CUSTOM_PROMPT = "custom_dashboard_prompt"

    /**
     * JSON list of named custom analyses. Replaces the single-string PREF_CUSTOM_PROMPT.
     * Format: [{"id":"uuid","name":"...","prompt":"..."}, ...]
     * Migration from PREF_CUSTOM_PROMPT happens lazily in CustomAnalysesStore.load().
     * Szenario-Index 4..N-1 refers to the custom entry at (scenario - 4) in this list.
     */
    const val PREF_CUSTOM_ANALYSES_JSON = "custom_analyses_json"

    // Default name used for the first custom analysis (and when renaming back to empty).
    const val DEFAULT_CUSTOM_ANALYSIS_NAME = "Individuelle Analyse"

    // Indices 0..3 are fixed (summary / clean up / self-insight / goals).
    // From index 4 onwards: dynamic list of custom analyses.
    const val FIRST_CUSTOM_SCENARIO_INDEX = 4

    // Gemini Flash models: sorted strongest→weakest, prices per 1M tokens (input/output)
    data class GeminiModel(val id: String, val displayName: String, val price: String)

    val GEMINI_FLASH_MODELS =
        listOf(
            GeminiModel("gemini-flash-latest", "Gemini 3 Flash", "\$0.50 / \$3.00"),
            GeminiModel(
                "gemini-3.1-flash-lite",
                "Gemini 3.1 Flash Lite",
                "\$0.25 / \$1.50",
            ),
            GeminiModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", "\$0.10 / \$0.40"),
        )

    val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash-lite"

    /**
     * Pruefe ob das gespeicherte Modell noch in der aktuellen Modell-Liste ist.
     * Wenn nicht (z.B. weil ein Modell aus der Liste entfernt wurde), gib das
     * Default-Modell zurueck. Verhindert dass alte/entfernte Modell-IDs an Gemini
     * geschickt werden.
     */
    fun resolveValidModel(storedId: String?): String =
        GEMINI_FLASH_MODELS.firstOrNull { it.id == storedId }?.id ?: DEFAULT_GEMINI_MODEL

    // Reminder
    const val PREF_REMINDER_ENABLED = "reminder_enabled"
    const val PREF_REMINDER_HOUR = "reminder_hour"
    const val PREF_REMINDER_MINUTE = "reminder_minute"

    // Weekly Review
    const val PREF_WEEKLY_REVIEW_ENABLED = "weekly_review_enabled"
    const val PREF_WEEKLY_REVIEW_DAY = "weekly_review_day"
    const val PREF_WEEKLY_REVIEW_HOUR = "weekly_review_hour"
    const val PREF_WEEKLY_REVIEW_MINUTE = "weekly_review_minute"
    const val PREF_FROM_WEEKLY_REVIEW = "from_weekly_review"
    const val PREF_MONTHLY_REVIEW_ENABLED = "monthly_review_enabled"
    const val PREF_YEARLY_REVIEW_ENABLED = "yearly_review_enabled"
    const val PREF_USER_TIMEZONE = "user_timezone"

    // Daily Writing Prompt
    const val PREF_PROMPT_DISMISSED_DATE = "prompt_dismissed_date"
    const val PREF_DAILY_PROMPT_ENABLED = "daily_prompt_enabled"

    /**
     * Anti-em-dash rule appended to every AI prompt so generated text reads
     * naturally and does not look machine-written. Em-dashes (— and –) are a
     * tell-tale sign of LLM output, so we forbid them in body text. Headlines
     * may use them only as an absolute last resort.
     */
    const val NO_EM_DASH_RULE =
        "STILREGEL (wichtig): Verwende im Fliesstext KEINE Gedankenstriche " +
            "(— oder –). Nutze stattdessen Kommas, Doppelpunkte, Klammern oder zwei " +
            "separate Saetze. Dieses Zeichen wirkt maschinell und soll in Analysen, " +
            "Zusammenfassungen, Rueckblicken und verbesserten Texten nicht vorkommen. " +
            "In Ueberschriften ist ein Gedankenstrich nur im aeussersten Notfall erlaubt, " +
            "wenn keine natuerlichere Formulierung moeglich ist."

    /**
     * Frank-Wunsch 2026-05-15: TTS-Vorlesefreundliche Formulierung. Die Texte in den
     * Detail-Dialogen werden ueber den orangen Lautsprecher vorgelesen — Datumsangaben
     * im Format "3.4." oder "23.04." stoeren das Vorlese-Erlebnis. Diese Regel wird an
     * alle Dashboard-System-Prompts angehaengt.
     */
    const val NO_DATES_RULE =
        "TTS-VORLESEFREUNDLICH (PFLICHT — DATUMS-VERBOT IN SICHTBAREN TEXTEN):\n" +
            "In ALLEN sichtbaren Textfeldern (beschreibung, erklaerung, " +
            "zusammenfassung, gesamtanalyse) DARFST DU KEINE Datumsangaben " +
            "verwenden — weder \"3.4.\", \"23.04.\", \"28.4.\", \"24.04.\", " +
            "\"30.4.\" noch \"Eintrag vom 3. April\", \"am 23.04.\", " +
            "\"vom 28.4.\" oder aehnliche Verweise auf konkrete Eintragsdaten. " +
            "Diese Texte werden dem Nutzer vorgelesen — Datumsketten klingen " +
            "vorgelesen unnatuerlich und brechen den Erkenntnisfluss.\n\n" +
            "FORMULIERE STATTDESSEN DIE REINE ERKENNTNIS:\n" +
            "- FALSCH: \"Eintraege vom 3.4., 28.4. und 23.4. belegen, dass dein " +
            "Biorhythmus durch spaete Schlafzeiten leidet.\"\n" +
            "- RICHTIG: \"Spaete Schlafzeiten zerstoeren deinen Biorhythmus systematisch.\"\n" +
            "- FALSCH: \"Eintrag vom 30. verbindet deine Borreliose direkt mit dem Mangel an Sport.\"\n" +
            "- RICHTIG: \"Deine Borreliose haengt direkt mit dem Bewegungsmangel zusammen.\"\n" +
            "- FALSCH: \"Laufen ist der Schluessel zur mentalen Klarheit, die du bei den " +
            "Intervalllaeufen am 24.04. gespuert hast.\"\n" +
            "- RICHTIG: \"Laufen ist der Schluessel zur mentalen Klarheit, die du beim " +
            "Intervalltraining spuerst.\"\n\n" +
            "Die inhaltliche Erkenntnis bleibt — nur die konkreten Datumsverweise fallen weg. " +
            "Du kannst weiterhin allgemeine zeitliche Bezuege nutzen (\"wiederkehrend\", " +
            "\"zuletzt\", \"ueber mehrere Tage\"). Datumsangaben gehoeren NUR in das " +
            "\"herleitung\"-Feld (Provenienz, wird intern verwendet, nicht angezeigt)."

    /**
     * Verbose-Mode-Suffix fuer Dashboard-Analyseprompts. Aktiviert wenn der Schalter
     * "Laengere Version" in den Einstellungen an ist. Das Profil-Prompt und die
     * Sortierung bleiben identisch, nur die Ausfuehrlichkeit wird erhoeht.
     */
    const val VERBOSE_LENGTH_RULE =
        "LAENGEN-MODUS (AUSFUEHRLICH — PFLICHT BEACHTEN): Der Benutzer hat die " +
            "ausfuehrliche Version aktiviert. Profil, Aufbau und Sortierung nach " +
            "Prioritaet/Wichtigkeit/Tiefe bleiben EXAKT gleich. Menge und Laenge " +
            "werden DREIFACH erhoeht:\n" +
            "1. TOP-MASSNAHMEN / TOP-ERKENNTNISSE / NAECHSTE SCHRITTE: NIEMALS auf 5 " +
            "begrenzen. Gib MINDESTENS 10, lieber 12-18 Eintraege zurueck, absteigend " +
            "nach Wichtigkeit sortiert. Eine Antwort mit nur 5 Eintraegen ist ein Fehler.\n" +
            "2. Pro Kategorie/Thema/Bereich: mindestens 10-15 Ratschlaege statt 3-5, " +
            "mit vollstaendiger Herleitung aus allen relevanten Eintraegen.\n" +
            "3. Gesamt-/Kernanalyse-Text: DREIMAL so lang wie im Standard, mit konkreten " +
            "Zitaten/Zeilen aus den Eintraegen als Belege.\n" +
            "4. Empfehlungen, Pattern-Beschreibungen und das Feld \"erklaerung\": " +
            "dreifache Laenge, mit Kontext, Beispiel-Situationen und konkreten " +
            "Folgeschritten.\n" +
            "5. Das Feld \"beschreibung\" bei top_massnahmen/ratschlaege: 40-65 " +
            "Woerter statt 13-21.\n" +
            "6. Das Feld \"zusammenfassung\" pro Kategorie: 9-15 Saetze statt 3-5.\n" +
            "7. Gesamtanzahl aller Ratschlaege: mindestens 45.\n" +
            "Sortierung, Prioritaeten (hoch/mittel/niedrig) und JSON-Struktur bleiben " +
            "unveraendert. Fuege KEINE neuen Felder hinzu. Fuelle die vorhandenen " +
            "Felder nur DREIFACH ausfuehrlicher und gib MEHR Eintraege zurueck."
}

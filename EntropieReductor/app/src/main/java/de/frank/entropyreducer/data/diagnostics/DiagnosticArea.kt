package de.frank.entropyreducer.data.diagnostics

/**
 * Bereiche die ueber das interne Diagnose-Logging ueberwacht werden (Frank-Wunsch 2026-05-23).
 *
 * Jeder Bereich entspricht einem API-Schluessel, Token oder einer Anbindung die fehlschlagen kann
 * ohne dass Frank es im UI sofort sieht. Der [displayName] erscheint im Diagnose-Screen und in der
 * Export-Datei.
 */
enum class DiagnosticArea(val displayName: String) {
    DRIVE_BACKUP("Google Drive Backup"),
    GOOGLE_CALENDAR("Google Kalender"),
    GROQ("Groq API"),
    GEMINI("Gemini API"),
    CLAUDE("Claude API"),
    GOOGLE_TTS("Google Cloud TTS"),
    WHOOP("Whoop"),
    OURA("Oura-Ring"),
    HEALTH_CONNECT("Health Connect"),
    OAUTH("OAuth-Anmeldung"),
    AMAZFIT("Amazfit"),
    POLAR("Polar-Import"),
    APP("App & System"),
    BIOMARKER("Biomarker & Analyse"),
    TASKS("Aufgaben & Loops"),
    AGENTIC("KI-Agenten"),
    DATABASE("Datenbank & Migration"),
    BRIEFING("Briefings & Reviews");

    companion object {
        /** Robustes Zurueck-Mapping aus dem persistierten String — unbekannt faellt auf null. */
        fun fromNameOrNull(raw: String?): DiagnosticArea? = entries.firstOrNull { it.name == raw }
    }
}

package de.frank.entropyreducer.domain.notifications

/**
 * Frank-Wunsch 2026-08-12: Einstellungen -> "Benachrichtigungen".
 *
 * Zentrale, einzige Quelle der Wahrheit fuer JEDE Benachrichtigung, die die App
 * verschickt. Wer eine neue Notification einbaut, traegt sie hier ein — dadurch
 * taucht sie automatisch im Einstellungs-Bildschirm auf und wird automatisch
 * abschaltbar (Poka-Yoke gegen "neue Notification ohne Schalter").
 *
 * [key] ist der DataStore-Schluessel und darf sich NIE aendern (sonst verliert der
 * Benutzer seine Einstellung).
 */
enum class NotificationGroup(val label: String) {
    GENIE("Genie & KI"),
    IMPORT("Importe"),
    LAUFEND("Laufende Vorgaenge"),
}

enum class AppNotification(
    val key: String,
    val title: String,
    val description: String,
    val group: NotificationGroup,
    val channelId: String,
    /**
     * true = Android erzwingt diese Benachrichtigung (Vordergrund-Dienst bzw.
     * Vordergrund-Worker). Sie laesst sich nicht in der App abschalten, ohne die
     * Funktion selbst zu zerstoeren — nur im System stummschalten.
     */
    val systemRequired: Boolean = false,
) {
    KI_FRAGE(
        key = "ki_frage",
        title = "Frage des Moments",
        description = "Die KI meldet sich mit einer kontextrelevanten Frage, wenn einer " +
            "der Trigger aus deinen Daten zutrifft.",
        group = NotificationGroup.GENIE,
        channelId = CHANNEL_KI,
    ),
    TAGESBRIEFING(
        key = "tagesbriefing",
        title = "Tagesbriefing ist bereit",
        description = "Nach dem Aufwachen, sobald das Genie dein Briefing fuer heute " +
            "erstellt hat.",
        group = NotificationGroup.GENIE,
        channelId = CHANNEL_KI,
    ),
    WOCHENRUECKBLICK(
        key = "wochenrueckblick",
        title = "Wochenrueckblick ist fertig",
        description = "Sonntagabend, wenn der narrative Rueckblick auf die letzten " +
            "sieben Tage erzeugt wurde.",
        group = NotificationGroup.GENIE,
        channelId = CHANNEL_KI,
    ),
    MONATSRUECKBLICK(
        key = "monatsrueckblick",
        title = "Monatsrueckblick ist fertig",
        description = "Anfang des Monats, wenn der Rueckblick auf den Vormonat " +
            "erzeugt wurde.",
        group = NotificationGroup.GENIE,
        channelId = CHANNEL_KI,
    ),
    KI_TRIGGER_VORSCHLAG(
        key = "ki_trigger_vorschlag",
        title = "Neue Trigger-Vorschlaege",
        description = "Mittwoch und Sonntag, wenn die Trigger-Engine neue Regeln " +
            "vorschlaegt, die du annehmen oder ablehnen kannst.",
        group = NotificationGroup.GENIE,
        channelId = CHANNEL_KI,
    ),
    TRIGGER_FEUERT(
        key = "trigger_feuert",
        title = "Ein Trigger hat ausgeloest",
        description = "Wenn die Bedingung eines von dir angenommenen Triggers zutrifft " +
            "(z.B. HRV unter deinem Schwellwert) — mit der vorgeschlagenen Handlung.",
        group = NotificationGroup.GENIE,
        channelId = CHANNEL_KI,
    ),
    POLAR_IMPORT_FERTIG(
        key = "polar_import_fertig",
        title = "Polar-Historie importiert",
        description = "Abschlussmeldung des Polar-Imports mit der Anzahl importierter " +
            "und uebersprungener Trainings.",
        group = NotificationGroup.IMPORT,
        channelId = CHANNEL_POLAR,
    ),
    POLAR_IMPORT_FEHLER(
        key = "polar_import_fehler",
        title = "Polar-Import fehlgeschlagen",
        description = "Fehlermeldung, wenn die Polar-ZIP nicht gelesen oder nicht " +
            "importiert werden konnte.",
        group = NotificationGroup.IMPORT,
        channelId = CHANNEL_POLAR,
    ),
    POLAR_IMPORT_LAEUFT(
        key = "polar_import_laeuft",
        title = "Polar-Import laeuft",
        description = "Fortschrittsanzeige waehrend des Imports. Android verlangt diese " +
            "Anzeige, damit der Import bei ausgeschaltetem Bildschirm nicht abgebrochen wird.",
        group = NotificationGroup.LAUFEND,
        channelId = CHANNEL_POLAR,
        systemRequired = true,
    ),
    AUFNAHME_LAEUFT(
        key = "aufnahme_laeuft",
        title = "Sprachaufnahme laeuft",
        description = "Wird angezeigt, solange eine Sprachnotiz aufgenommen wird. Android " +
            "schreibt diese Anzeige bei Mikrofon-Nutzung im Hintergrund zwingend vor.",
        group = NotificationGroup.LAUFEND,
        channelId = CHANNEL_RECORDING,
        systemRequired = true,
    ),
    VORLESEN_LAEUFT(
        key = "vorlesen_laeuft",
        title = "Vorlesen laeuft",
        description = "Wird angezeigt, solange das Genie vorliest. Android schreibt diese " +
            "Anzeige bei Audio-Wiedergabe im Hintergrund zwingend vor.",
        group = NotificationGroup.LAUFEND,
        channelId = CHANNEL_TTS,
        systemRequired = true,
    ),
    ;

    companion object {
        const val CHANNEL_KI = "ki_questions"
        const val CHANNEL_POLAR = "polar_bulk_import"
        const val CHANNEL_RECORDING = "recording"
        const val CHANNEL_TTS = "tts_playback"

        /** Alle Benachrichtigungen einer Gruppe, in Deklarationsreihenfolge. */
        fun byGroup(): Map<NotificationGroup, List<AppNotification>> =
            entries.groupBy { it.group }
    }
}

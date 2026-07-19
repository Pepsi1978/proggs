package de.frank.fisetinbegleiter.alarm

import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity

data class NotificationContent(
    val title: String,
    val text: String,
    val destination: String,
)

fun notificationContent(
    type: AlarmType,
    targetDay: Int,
    protocol: ProtocolTemplateEntity = ProtocolTemplateEntity(),
): NotificationContent = when (type) {
    AlarmType.MEAL_WARNING -> {
        val remaining = (protocol.mealDeadlineMinutes - protocol.mealWarningMinutes).coerceAtLeast(0)
        NotificationContent(
            title = "Fettige Mahlzeit",
            text = if (remaining == 0) {
                "Das ${protocol.mealDeadlineMinutes}-Minuten-Fenster für deine fettige Mahlzeit läuft jetzt ab."
            } else {
                "In ${minuteLabel(remaining)} läuft das ${protocol.mealDeadlineMinutes}-Minuten-Fenster für deine fettige Mahlzeit ab."
            },
            destination = "timeline",
        )
    }
    AlarmType.SPERMIDIN_OPEN -> {
        val remaining = (protocol.antioxidantBlockMinutes - protocol.spermidinStartMinutes).coerceAtLeast(0)
        NotificationContent(
            title = "Spermidin-Fenster offen",
            text = "Fenster offen: Spermidin (6 mg) + optional Piperin (10–15 mg) + leichte Mahlzeit. Noch ${minuteLabel(remaining)} bis zum Ende des Sperrfensters.",
            destination = "timeline",
        )
    }
    AlarmType.SPERMIDIN_REMINDER -> {
        val remaining = (protocol.antioxidantBlockMinutes - protocol.spermidinReminderMinutes).coerceAtLeast(0)
        NotificationContent(
            title = "Spermidin-Erinnerung",
            text = if (remaining == 0) {
                "Das Spermidin-Fenster schließt jetzt. Erledige den Schritt, falls er noch offen ist."
            } else {
                "Das Spermidin-Fenster schließt in ${minuteLabel(remaining)}. Erledige den Schritt, falls er noch offen ist."
            },
            destination = "timeline",
        )
    }
    AlarmType.BLOCK_ENDED -> NotificationContent(
        title = "Antioxidantien wieder erlaubt",
        text = "Antioxidantien-Fenster vorbei – Vitamin C und deine restlichen Antioxidantien sind jetzt wieder erlaubt.",
        destination = "stack",
    )
    AlarmType.NEXT_DAY -> NotificationContent(
        title = "Tag $targetDay deiner Fisetin-Kur",
        text = "Nüchtern starten, wenn du bereit bist.",
        destination = "today",
    )
    AlarmType.CURE_ENDED -> NotificationContent(
        title = "Kur abgeschlossen",
        text = "Kur abgeschlossen (${if (targetDay == 1) "1 Tag" else "$targetDay Tage"}). Der Eintrag ist im Protokoll gespeichert.",
        destination = "history",
    )
    AlarmType.CURE_START -> NotificationContent(
        title = "Fisetin-Kur starten",
        text = "Deine geplante Kur beginnt jetzt. Öffne die Drink-Checkliste und starte T0, wenn du bereit bist.",
        destination = "today",
    )
}

private fun minuteLabel(minutes: Int): String = if (minutes == 1) "1 Minute" else "$minutes Minuten"

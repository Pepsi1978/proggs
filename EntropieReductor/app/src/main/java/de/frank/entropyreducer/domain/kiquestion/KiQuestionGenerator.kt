package de.frank.entropyreducer.domain.kiquestion

import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.model.priorityBucketForScore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Erzeugt aus den aktuellen Daten die kontextrelevante "KI-Frage des Moments".
 * Spec §10.4 — fuenf Trigger.
 *
 * Diese Klasse ist deterministisch und bewusst frei von KI-Aufrufen. Die KI kommt
 * erst ins Spiel wenn der Nutzer die Frage beantwortet — dann wird die Antwort an
 * Gemini geschickt um neue Entropie-Eintraege zu erzeugen.
 */
@Singleton
class KiQuestionGenerator @Inject constructor() {

    fun generate(
        entries: List<EntropyEntryEntity>,
        latestSnapshot: BiomarkerSnapshotEntity?,
        todayCalendar: CalendarDayEntity?,
        tomorrowCalendar: CalendarDayEntity?,
        nowMs: Long = System.currentTimeMillis(),
    ): KiQuestion? {
        // Frank-Wunsch 2026-05-09: KEINE statischen Fallback-Fragen mehr.
        // Wenn die KI keine Frage liefert, bleibt die Card unsichtbar — lieber gar
        // keine Frage als die alte 'Du hast 6-mal Eintraege der Kategorie Umgebung'.
        return null
        // Trigger 3 — Recovery niedrig: hoechste Prioritaet, weil koerperlich relevant.
        latestSnapshot?.recoveryScore?.let { recovery ->
            if (recovery < 33) {
                return KiQuestion(
                    triggerKey = "recovery_low",
                    text = "Deine Recovery ist heute niedrig ($recovery %). Soll ich heute alle " +
                        "koerperlichen Aufgaben nach hinten schieben und dir nur leichte " +
                        "Umgebungs-Aufgaben zeigen?",
                    rationale = "Whoop-Recovery $recovery % am ${formatDate(latestSnapshot.capturedAt)}",
                )
            }
        }

        // Trigger 5 — Frei-Block beginnt morgen.
        if (tomorrowCalendar?.shiftCode == ShiftCode.FREI &&
            todayCalendar?.shiftCode != ShiftCode.FREI &&
            todayCalendar?.shiftCode != ShiftCode.URLAUB
        ) {
            return KiQuestion(
                triggerKey = "free_block_tomorrow",
                text = "Morgen beginnt dein Frei-Block. Welche schwereren Aufgaben sollen wir " +
                    "auf morgen verschieben?",
                rationale = "Morgen = ${tomorrowCalendar.date} Frei-Tag",
            )
        }

        val openEntries = entries.filter { it.status == EntryStatus.OFFEN }

        // Trigger 1 — sehr hohe Priorität, aber heute kaum Zeit.
        if (todayCalendar?.shiftCode == ShiftCode.TAGDIENST) {
            val todayEntry = openEntries.firstOrNull {
                priorityBucketForScore(it.manualPriorityScore ?: it.priorityScore) == TimeBucket.HEUTE &&
                    (it.estimatedDurationMinutes ?: 30) > (todayCalendar.availableMinutesEstimate)
            }
            if (todayEntry != null) {
                val durationLabel = todayEntry.estimatedDurationMinutes?.let { "$it" } ?: "geschaetzt 30"
                return KiQuestion(
                    triggerKey = "today_overload",
                    text = "Aufgabe \"${todayEntry.title}\" braucht etwa $durationLabel Min. " +
                        "Sie ist sehr hoch priorisiert, passt aber heute eng. Was ist der kleinste nächste Schritt?",
                    rationale = "Tagdienst, ${todayCalendar.availableMinutesEstimate} Min verfuegbar",
                    relatedEntryIds = listOf(todayEntry.id),
                )
            }
        }

        // Trigger 2 — Aufgabe seit > 5 Tagen offen.
        val fiveDaysAgo = nowMs - 5L * 24 * 60 * 60 * 1000
        val stale = openEntries.firstOrNull { it.createdAt < fiveDaysAgo }
        if (stale != null) {
            val days = ((nowMs - stale.createdAt) / (24L * 60 * 60 * 1000)).toInt()
            return KiQuestion(
                triggerKey = "stale_entry",
                text = "Du hast \"${stale.title}\" seit $days Tagen offen. Was hindert dich konkret?",
                rationale = "Eintrag ${stale.id} seit $days Tagen offen",
                relatedEntryIds = listOf(stale.id),
            )
        }

        // Trigger 4 — Cluster: mehrere aehnliche Eintraege in kurzer Zeit.
        val threeDaysAgo = nowMs - 3L * 24 * 60 * 60 * 1000
        val recentlyCreated = entries.filter { it.createdAt >= threeDaysAgo }
        val clusters = recentlyCreated.groupBy { it.category }
            .filterValues { it.size >= 4 }
        clusters.entries.firstOrNull()?.let { (cat, list) ->
            return KiQuestion(
                triggerKey = "category_cluster:${cat.name}",
                text = "Du hast in den letzten 3 Tagen ${list.size}-mal Eintraege der Kategorie " +
                    "${cat.name.lowercase()} genannt. Möchtest du das vertiefen?",
                rationale = "Cluster ${cat.name}: ${list.size} Eintraege",
                relatedEntryIds = list.map { it.id },
            )
        }

        return null
    }

    private fun formatDate(epochMs: Long): String {
        val date = java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return date.toString()
    }
}

/** Eine kontextuelle Frage, die in der Frage-des-Moments-Card angezeigt wird. */
data class KiQuestion(
    val triggerKey: String,
    val text: String,
    val rationale: String,
    val relatedEntryIds: List<String> = emptyList(),
)

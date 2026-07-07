package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aufgaben-Vorschlag (ID-Architektur Etappe 2, Frank-Wunsch 2026-06-19).
 *
 * Zieht aus dem DataStore "ki_task_suggestions" (JSON) in die Datenbank um — entspricht der
 * bisherigen `AutoTaskSuggestion`/`KiTaskSuggestion`. Bekommt jetzt eine Herkunfts-Kette:
 * - originId/originType = die Quell-Idee, aus der dieser Vorschlag erzeugt wurde (Etappe 2d)
 * - rootId = Ur-Eintrag der Kette (i. d. R. dieselbe Idee)
 *
 * Wird ein Vorschlag angenommen, traegt die entstehende Aufgabe (entropy_entries) originId = die
 * id dieses Vorschlags. So ist die Kette Idee -> Vorschlag -> Aufgabe luckenlos.
 */
@Immutable
@Entity(
    tableName = "task_suggestions",
    indices = [Index("createdAt"), Index("originId"), Index("rootId")],
)
data class TaskSuggestionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val createdAt: Long,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

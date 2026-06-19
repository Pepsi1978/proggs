package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Gewohnheits-Vorschlag (ID-Architektur Etappe 3, Frank-Wunsch 2026-06-19).
 *
 * Zieht aus dem DataStore "gewohnheit_suggestions" (JSON-Key "suggestions_json") in die Datenbank um
 * — entspricht der bisherigen `Mental`/`BackupMental` im Gewohnheits-Vorschlags-Store. Bekommt eine
 * Herkunfts-Kette analog [TaskSuggestionEntity]:
 * - originId/originType = die Quell-Idee, aus der dieser Vorschlag erzeugt wurde (Etappe 3d)
 * - rootId = Ur-Eintrag der Kette (i. d. R. dieselbe Idee)
 *
 * Wird ein Vorschlag angenommen, traegt die entstehende Gewohnheit (habits) originId = die id dieses
 * Vorschlags. So ist die Kette Idee -> Gewohnheits-Vorschlag -> Gewohnheit luckenlos.
 */
@Immutable
@Entity(
    tableName = "habit_suggestions",
    indices = [Index("createdAt"), Index("originId"), Index("rootId")],
)
data class HabitSuggestionEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

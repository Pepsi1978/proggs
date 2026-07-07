package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Gewohnheit (ID-Architektur Etappe 3, Frank-Wunsch 2026-06-19).
 *
 * Zieht aus dem DataStore "gewohnheit_board" (JSON-Key "gewohnheiten_json") in die Datenbank um —
 * entspricht der bisherigen `Mental` im Gewohnheit-Reiter (presentation/mental/GewohnheitBoardScreen.kt).
 *
 * Die MANUELLE Reihenfolge (Drag-and-Drop) lebt jetzt im Feld [position] — in der JSON-Liste war es
 * die Listen-Position. Beim Lesen wird nach `position` sortiert.
 *
 * Herkunfts-Kette (Spec 2026-06-19 §3/§4):
 * - originId/originType = direkter Vorgaenger (z. B. der Gewohnheits-Vorschlag, aus dem die Gewohnheit
 *   entstand). Bei direkt angelegten Gewohnheiten null.
 * - rootId = Ur-Eintrag der Kette (die urspruengliche Idee).
 */
@Immutable
@Entity(
    tableName = "habits",
    indices = [Index("position"), Index("originId"), Index("rootId")],
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val text: String,
    val updatedAt: Long = 0L,
    val position: Int = 0,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

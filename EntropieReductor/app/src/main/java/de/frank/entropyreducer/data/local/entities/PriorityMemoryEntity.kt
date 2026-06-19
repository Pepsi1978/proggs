package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19).
 *
 * Merkt sich manuell (per Schieberegler) gesetzte Aufgaben-Prioritaeten als eigenstaendige
 * Eintraege — getrennt von den Aufgaben (entropy_entries), damit das Gedaechtnis erhalten bleibt,
 * auch wenn die Ursprungsaufgabe laengst erledigt/archiviert/geloescht ist. Beim Einsprechen einer
 * neuen Aufgabe gleicht die KI (Gemini) ab, ob ein spezifisch sehr aehnlicher Eintrag existiert,
 * und uebernimmt dessen Prioritaet als KI-Vorschlag.
 */
@Immutable
@Entity(
    tableName = "priority_memory",
    indices = [Index("updatedAt"), Index("sourceEntryId")],
)
data class PriorityMemoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priority: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceEntryId: String? = null,
)

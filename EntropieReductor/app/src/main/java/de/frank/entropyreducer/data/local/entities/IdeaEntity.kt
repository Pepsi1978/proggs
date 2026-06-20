package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Idee (ID-Architektur Etappe 2, Frank-Wunsch 2026-06-19).
 *
 * Zieht aus dem DataStore "ideen_entries" (JSON) in die Datenbank um — entspricht der bisherigen
 * `IdeenEntry` (presentation/ideen/IdeenScreen.kt), flach in Room. Nachtraege liegen in der eigenen
 * Tabelle `idea_followups` (FK), nicht mehr verschachtelt im JSON.
 *
 * Herkunfts-Kette (siehe docs/specs/2026-06-19-id-architektur-design.md):
 * - originId/originType = Vorgaenger, falls die Idee aus etwas anderem entstand (z. B. Etappe 5:
 *   Entropie -> Idee, These -> Idee). Bei direkt eingesprochenen Ideen null (= Ursprung).
 * - rootId = Ur-Eintrag der Kette.
 */
@Immutable
@Entity(
    tableName = "ideas",
    indices = [Index("timestampMs"), Index("originId"), Index("rootId")],
)
data class IdeaEntity(
    @PrimaryKey val id: String,
    val timestampMs: Long,
    val title: String,
    val text: String,
    val summary: String? = null,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
    // Frank-Wunsch 2026-06-20: Modifikations-Zeitstempel fuer geraeteuebergreifende Edit-Sync (LWW).
    // Nullable (additive Migration ohne NOT NULL/DEFAULT -> keine identityHash-Falle); Bestand = null,
    // im Sync wird dann timestampMs (Erstellzeit) als Baseline genutzt.
    val updatedAt: Long? = null,
)

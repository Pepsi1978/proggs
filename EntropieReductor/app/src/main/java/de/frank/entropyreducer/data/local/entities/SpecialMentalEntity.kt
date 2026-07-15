package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spezieller Mental-Satz (Frank-Wunsch 2026-07-15).
 *
 * Eigenstaendiger, vom normalen Mentalboard getrennter Bereich: Diese Saetze erscheinen unter der
 * "Chirp 3 HD"-Kontingentzeile und haben einen EIGENEN Vorlese-Durchlauf (eigener Play/Pause/Stop-
 * Player, eigene Pause-/Abschalt-/Zufalls-Einstellungen). Nur AKTIVIERTE Saetze ([enabled] = true)
 * werden vorgelesen; ein neu angelegter Satz ist zunaechst deaktiviert (leeres Kaestchen).
 *
 * Bewusst eigene Tabelle statt Flag auf [MentalEntity], damit Backup/Sync/Reihenfolge des normalen
 * Boards voellig unberuehrt bleiben. Die Reihenfolge ist die Einfuege-Reihenfolge ([position]).
 */
@Immutable
@Entity(
    tableName = "special_mental_sentences",
    indices = [Index("position")],
)
data class SpecialMentalEntity(
    @PrimaryKey val id: String,
    val text: String,
    val updatedAt: Long = 0L,
    val position: Int = 0,
    /** Nur aktivierte Saetze werden vorgelesen. Default false = leeres Aktivierungs-Kaestchen. */
    val enabled: Boolean = false,
)

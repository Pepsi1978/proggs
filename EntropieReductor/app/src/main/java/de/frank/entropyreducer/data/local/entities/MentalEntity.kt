package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mental-Board-Satz (ID-Architektur Etappe 4, Frank-Wunsch 2026-06-19).
 *
 * Zieht aus dem DataStore "mental_board" (JSON-Key "mentals_json") in die Datenbank um — entspricht
 * der bisherigen `Mental` im Mental-Reiter (presentation/mental/MentalBoardScreen.kt). Die manuelle
 * Reihenfolge (Drag-and-Drop) lebt jetzt im Feld [position].
 *
 * Mental-Saetze sind eigenstaendige Eintraege OHNE Herkunfts-Kette (sie entstehen nicht aus Ideen) —
 * die origin/root-Felder bleiben null, sind aber fuer ein einheitliches Datenmodell (Spec §3.1)
 * vorhanden (z. B. falls spaeter eine Kette These -> Mental-Satz gewuenscht wird).
 */
@Immutable
@Entity(
    tableName = "mental_sentences",
    indices = [Index("position")],
)
data class MentalEntity(
    @PrimaryKey val id: String,
    val text: String,
    val updatedAt: Long = 0L,
    val position: Int = 0,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

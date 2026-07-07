package de.frank.entropyreducer.presentation.amazfit

import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/**
 * Sucht den Ruhepuls fuer den Tag eines Workouts (Frank-Wunsch 2026-05-17).
 *
 * Strategie:
 *  - Bevorzugt: Snapshot vom gleichen Kalendertag wie der Workout-Start (Whoop
 *    misst meist morgens, also liegt der Ruhepuls oft vor dem Training vor).
 *  - Fallback: Snapshot vom Vortag (max 1 Tag entfernt). Wenn Frank z.B. abends
 *    spaet trainiert und der naechste Whoop-Snapshot erst am Folgetag erscheint,
 *    nehmen wir den letzten verfuegbaren Wert vor dem Training.
 *  - Wenn nichts in diesem Zeitfenster: null. Die VO2max-Anzeige zeigt dann "—"
 *    statt mit Hardcoded-65 zu raten.
 *
 * Frank-Hintergrund: Whoop-Daten gibt es erst seit ~Februar 2026. Fuer aeltere
 * Trainings (Polar-Bulk-Import) gibt es keine Ruhepuls-Daten — VO2max bleibt
 * leer, was korrekt ist.
 */
/**
 * Frank-Wunsch 2026-05-17: VO2max-Anzeige nur bei Sportarten wo die ACSM-Lauf-
 * Formel `0.2 * Speed + 3.5` sinnvolle Werte liefert. Bei Crosstrainer/Rudern/
 * Krafttraining/Yoga/Schwimmen/Radfahren fehlen entweder echte Distanz (Indoor)
 * oder die Formel passt biomechanisch nicht.
 *
 * Whitelist via Substring-Match (lowercase): "lauf" matched Laufen, Trailrunning,
 * Laufband, "Laufen (Strasse)", "Laufen (virtuell)"; "trail" matched Trailrunning;
 * "walk"/"wandern" matchen die Geh-Sportarten.
 *
 * Beispiele:
 *  - "Laufen", "Trailrunning", "Laufband" → true
 *  - "Wandern", "Walking" → true
 *  - "Crosstrainer", "Krafttraining", "Yoga", "Rudern" → false
 */
fun isVo2MaxSport(sportName: String?): Boolean {
    if (sportName.isNullOrBlank()) return false
    val lower = sportName.lowercase()
    return lower.contains("lauf") ||
        lower.contains("trail") ||
        lower.contains("walk") ||
        lower.contains("wandern")
}

fun findRestingHrForWorkoutDay(
    snapshots: List<BiomarkerSnapshotEntity>,
    workoutStartMs: Long,
): Int? {
    if (snapshots.isEmpty()) return null
    val zone = ZoneId.systemDefault()
    val workoutDate = Instant.ofEpochMilli(workoutStartMs).atZone(zone).toLocalDate()
    val candidates = snapshots.mapNotNull { snap ->
        val rhr = snap.restingHeartRate ?: return@mapNotNull null
        val snapDate = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(snapDate, workoutDate).absoluteValue
        if (daysDiff > 1) null else Triple(daysDiff, snap.capturedAt, rhr)
    }
    if (candidates.isEmpty()) return null
    // Bevorzuge gleichen Tag, dann naechstgelegenen Tag. Bei mehreren Snapshots
    // am gleichen Tag nimm den mit hoechstem capturedAt (= aktuellsten).
    return candidates
        .sortedWith(compareBy({ it.first }, { -it.second }))
        .first()
        .third
}

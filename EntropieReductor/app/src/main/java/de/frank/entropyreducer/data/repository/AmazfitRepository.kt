package de.frank.entropyreducer.data.repository

import androidx.room.withTransaction
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.health.HealthConnectExerciseSession
import de.frank.entropyreducer.data.health.HealthConnectManager
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.local.dao.AmazfitDailyDao
import de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao
import de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.util.runCatchingCancellable
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Holt taegliche Werte und Workouts aus der Zepp-Cloud (Amazfit T-Rex 3) und speichert sie in zwei
 * eigenen Tabellen (`amazfit_daily`, `amazfit_workouts`).
 *
 * Frank-Wunsch 2026-05-09:
 * - PAI, BioCharge, Hauttemperatur, Stress, SpO2, Atemfrequenz im Biomarker-Bereich
 * - Sport-Daten (Trainingsart, Dauer, GPS-Track, Pace, Pulsverlauf, etc.) in einem eigenen
 *   Sport-Bereich unterhalb der Biomarker-Karten
 *
 * Strategie:
 * - Sync-Fenster default 365 Tage (analog zu Whoop)
 * - Bei abgelaufenem App-Token automatisch Re-Login mit gespeicherten Credentials
 * - Defensives JSON-Parsing: viele Felder werden vom Zepp-Server in Sub-Objekten geliefert deren
 *   genaue Form je nach App-Version variiert. Wir parsen jedes Feld einzeln und ueberspringen
 *   Fehler statt komplette Tage zu verwerfen.
 *
 * Status-Notiz: Der Workout-DETAIL-Endpoint mit GPS-Track + HR-Verlauf ist nicht im
 * Open-Source-Code dokumentiert. Wir holen erstmal die Workout-Liste (Summary) und ergaenzen
 * Detail-Daten (gpsTrackJson, heartRateSeriesJson, paceSeriesJson, splitsJson) sobald der Endpoint
 * via Network-Inspection identifiziert ist.
 */
@Singleton
class AmazfitRepository
@Inject
constructor(
    private val dailyDao: AmazfitDailyDao,
    private val workoutDao: AmazfitWorkoutDao,
    // ZeppApi + ZeppAuthService entfernt 2026-05-17 (Frank-Wunsch).
    private val secrets: EncryptedSecretsStore,
    private val appSettings: de.frank.entropyreducer.data.settings.AppSettings,
    private val appDatabase: AppDatabase,
    /**
     * Frank-Wunsch 2026-05-11: Nach jedem Sync und nach jedem Detail-Reload automatisch ein
     * Drive-Backup ausloesen — gleiches Pattern wie bei HealthConnect-Updates (siehe
     * BiomarkerViewModel.refreshWeight). Lazy weil SyncCoordinator selbst Repositories nutzt —
     * vermeidet zirkulaere Init-Reihenfolge bei Hilt. requestSync() ist debounced (1500 ms) und ein
     * No-Op wenn Drive-Backup deaktiviert oder kein Konto verbunden ist.
     */
    private val syncCoordinatorLazy:
        dagger.Lazy<de.frank.entropyreducer.data.remote.drive.SyncCoordinator>,
    /**
     * Frank-Wunsch 2026-05-16: Workouts auch aus Health Connect lesen koennen. Hintergrund: Die
     * T-Rex 3 synct via Bluetooth in die Zepp-App und von dort via Health Connect zum Phone. Der
     * Cloud-Upload zur Zepp-Server-Seite ist eine getrennte Stufe — kann verzoegert sein oder Tagen
     * ausbleiben wenn der Cloud-Sync in der Zepp-App nicht angetippt wird. Health Connect ist die
     * sofort verfuegbare Quelle.
     */
    private val healthConnect: HealthConnectManager,
    /** Wetter-Rueckblick pro Training (Open-Meteo): stuendliche Temperatur am GPS-Ort. */
    private val weatherRepository: WeatherRepository,
) {

    fun observeLatestDaily(): Flow<AmazfitDailyEntity?> = dailyDao.getLatest()

    fun observeAllDaily(): Flow<List<AmazfitDailyEntity>> = dailyDao.getAll()

    fun observeDailyRange(from: Long, to: Long): Flow<List<AmazfitDailyEntity>> =
        dailyDao.getRange(from, to)

    fun observeAllWorkouts(): Flow<List<AmazfitWorkoutEntity>> = workoutDao.observeAll()

    /**
     * Frank-Wunsch 2026-05-16: Einmalige Workout-Cleanup-Migration vor der Polar- Integration.
     * Loescht ALLE Workouts aus der lokalen DB und triggert sofort einen Drive-Sync — damit wird
     * das Drive-Backup mit dem aktuellen (leeren) Stand ueberschrieben und spielt die alten
     * Trainings nicht mehr zurueck.
     *
     * Backup-Logik bleibt erhalten: kuenftige Polar-Workouts werden weiter gesichert und
     * cross-device wiederhergestellt.
     */
    suspend fun cleanupAllWorkoutsForMigration() {
        Diag.i(DiagnosticArea.AMAZFIT, 
            TAG,
            "Workout-Cleanup-Migration: loesche alle amazfit_workouts und triggere Drive-Sync",
        )
        workoutDao.deleteAll()
        syncCoordinatorLazy.get().requestSync("Training: Cleanup-Migration")
    }

    /**
     * Frank-Bugfix 2026-05-22: Anzahl lokaler Workouts. Wird beim App-Start
     * geprueft um zu entscheiden ob die Cleanup-Migration sofort laufen soll
     * (lokale DB nicht leer) oder ob auf Drive-Restore gewartet werden muss
     * (lokale DB leer = wahrscheinlich frische Installation).
     */
    suspend fun workoutCount(): Int = workoutDao.count()

    /**
     * Frank-Wunsch 2026-05-17: Umbenennung aller Workouts mit altem sportName. Wird vom
     * Sport-Rename-V1-Migrator beim App-Start aufgerufen. Idempotent — bei zweitem Aufruf 0 Zeilen
     * geaendert.
     */
    suspend fun renameSportName(oldName: String, newName: String): Int {
        val changed = workoutDao.renameSportName(oldName, newName)
        if (changed > 0) {
            Diag.i(DiagnosticArea.AMAZFIT, TAG, "Sport-Rename: $changed Workouts '$oldName' -> '$newName'")
            syncCoordinatorLazy.get().requestSync("Training: Sync/Aenderung")
        }
        return changed
    }

    /**
     * Frank-Wunsch 2026-05-17: Manuelle Override-Werte fuer ein Training speichern. Wird vom
     * Edit-Dialog ([EditTrainingValuesDialog]) im Hero- und Detail-Screen aufgerufen.
     *
     * Verhalten: laedt das existierende Workout, ueberschreibt nur die Felder fuer die ein Wert
     * (non-null) uebergeben wurde, behaelt alles andere bei. VO2max ist explizit nicht editierbar —
     * wird im UI live aus den anderen Werten berechnet.
     *
     * Manuelle Werte bleiben bei spaeteren Trainings-Syncs erhalten: mergeFromHealthConnect
     * fuegt nur NEUE Sessions ein und laesst bestehende Eintraege (inkl. manueller Edits)
     * unangetastet.
     *
     * @return true wenn das Workout gefunden und aktualisiert wurde
     */
    suspend fun applyManualOverrides(
        trackId: String,
        durationSeconds: Long? = null,
        distanceMeters: Double? = null,
        avgPaceSecPerKm: Double? = null,
        maxPaceSecPerKm: Double? = null,
        avgHeartRate: Int? = null,
        maxHeartRate: Int? = null,
        altitudeGainMeters: Double? = null,
        altitudeLossMeters: Double? = null,
        cadence: Int? = null,
        strideLengthCm: Int? = null,
        calories: Double? = null,
    ): Boolean {
        val existing = workoutDao.getById(trackId)
        if (existing == null) {
            Diag.w(DiagnosticArea.AMAZFIT, TAG, "applyManualOverrides: kein Workout mit trackId=$trackId")
            return false
        }
        val now = System.currentTimeMillis()
        // Frank-Wunsch 2026-05-17 (Iteration 2): Tracke pro Feld welches
        // editiert wurde — damit das Schloss-Icon im StatsGrid pro Karte
        // (Ø Puls, Schrittlänge, Kalorien etc.) angezeigt werden kann.
        // Labels MUESSEN genau mit den StatsGrid-Labels im DetailScreen
        // uebereinstimmen (siehe AmazfitTrainingDetailScreen.kt).
        val editedLabels = mutableSetOf<String>()
        existing.manualOverrideFields
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.forEach { editedLabels.add(it) }
        if (durationSeconds != null && durationSeconds != existing.durationSeconds) {
            editedLabels.add("Dauer")
        }
        if (
            distanceMeters != null &&
                (existing.distanceMeters == null ||
                    kotlin.math.abs(distanceMeters - existing.distanceMeters) > 0.5)
        ) {
            editedLabels.add("Distanz")
        }
        if (
            avgPaceSecPerKm != null &&
                (existing.avgPaceSecPerKm == null ||
                    kotlin.math.abs(avgPaceSecPerKm - existing.avgPaceSecPerKm) > 1.0)
        ) {
            editedLabels.add("Ø Pace")
        }
        if (
            maxPaceSecPerKm != null &&
                (existing.maxPaceSecPerKm == null ||
                    kotlin.math.abs(maxPaceSecPerKm - existing.maxPaceSecPerKm) > 1.0)
        ) {
            editedLabels.add("Maximale Pace")
        }
        if (avgHeartRate != null && avgHeartRate != existing.avgHeartRate) {
            editedLabels.add("Ø Puls")
        }
        if (maxHeartRate != null && maxHeartRate != existing.maxHeartRate) {
            editedLabels.add("Maximalpuls")
        }
        if (
            altitudeGainMeters != null &&
                (existing.altitudeGainMeters == null ||
                    kotlin.math.abs(altitudeGainMeters - existing.altitudeGainMeters) > 0.5)
        ) {
            editedLabels.add("Höhe ↑")
        }
        if (
            altitudeLossMeters != null &&
                (existing.altitudeLossMeters == null ||
                    kotlin.math.abs(altitudeLossMeters - existing.altitudeLossMeters) > 0.5)
        ) {
            editedLabels.add("Höhe ↓")
        }
        if (cadence != null && cadence != existing.cadence) {
            editedLabels.add("Schrittfrequenz")
        }
        if (strideLengthCm != null && strideLengthCm != existing.strideLengthCm) {
            editedLabels.add("Schrittlänge")
        }
        if (
            calories != null &&
                (existing.calories == null || kotlin.math.abs(calories - existing.calories) > 0.5)
        ) {
            editedLabels.add("Kalorien")
        }
        val mergedFields = editedLabels.joinToString(",").takeIf { it.isNotEmpty() }
        val updated =
            existing.copy(
                durationSeconds = durationSeconds ?: existing.durationSeconds,
                // Wenn die Dauer geaendert wird, endMs konsistent mitziehen
                // (endMs = Start + Dauer) — sonst weicht die Detail-Anzeige ab.
                endMs = durationSeconds?.let { existing.startMs + it * 1000L } ?: existing.endMs,
                distanceMeters = distanceMeters ?: existing.distanceMeters,
                avgPaceSecPerKm = avgPaceSecPerKm ?: existing.avgPaceSecPerKm,
                maxPaceSecPerKm = maxPaceSecPerKm ?: existing.maxPaceSecPerKm,
                avgSpeedKmh = avgPaceSecPerKm?.let { 3600.0 / it } ?: existing.avgSpeedKmh,
                maxSpeedKmh = maxPaceSecPerKm?.let { 3600.0 / it } ?: existing.maxSpeedKmh,
                avgHeartRate = avgHeartRate ?: existing.avgHeartRate,
                maxHeartRate = maxHeartRate ?: existing.maxHeartRate,
                altitudeGainMeters = altitudeGainMeters ?: existing.altitudeGainMeters,
                altitudeLossMeters = altitudeLossMeters ?: existing.altitudeLossMeters,
                cadence = cadence ?: existing.cadence,
                strideLengthCm = strideLengthCm ?: existing.strideLengthCm,
                calories = calories ?: existing.calories,
                // Frank-Wunsch 2026-05-17: Markiere als manuell editiert. Verhindert
                // dass nachfolgende Trainings-Syncs die hier gesetzten Summary-Werte
                // wieder ueberschreiben.
                manualOverridesMs = now,
                manualOverrideFields = mergedFields,
                createdAt = now,
            )
        workoutDao.upsert(updated)
        Diag.i(DiagnosticArea.AMAZFIT, TAG, "Manual overrides applied to $trackId (fields=$mergedFields)")
        // Frank-Wunsch 2026-05-19: Sofort Drive-Backup ausloesen sobald ein
        // manueller Wert geaendert wird — der Edit darf bei einem Reinstall
        // nicht verloren gehen. Debounce in requestSync() (1500 ms) sorgt fuer
        // Coalescing wenn Frank mehrere Felder kurz hintereinander editiert.
        syncCoordinatorLazy.get().requestSync("Training: manuell editiert ($trackId)")
        return true
    }

    /**
     * Frank-Wunsch 2026-05-17: V2-Cleanup. Behaelt nur Trainings im Fenster
     * [olderThanMs, newerThanMs] — alles davor (Uralt-Polar-Daten, ~967 Eintraege) UND alles danach
     * (die neuen Polar-Duplikate vom 17.05.-01.05.2026) wird in EINER atomaren SQL-Operation
     * entfernt.
     *
     * Triggert anschliessend einen Drive-Sync damit das Backup mit dem reduzierten Stand
     * ueberschrieben wird (statt die geloeschten Trainings beim naechsten Geraete-Restore wieder
     * einzuspielen).
     *
     * @return Anzahl der geloeschten Workouts
     */
    suspend fun cleanupWorkoutsKeepRange(olderThanMs: Long, newerThanMs: Long): Int {
        val deleted = workoutDao.deleteOutsideRange(olderThanMs, newerThanMs)
        Diag.i(DiagnosticArea.AMAZFIT, 
            TAG,
            "Workout-Cleanup-V2: $deleted Trainings geloescht " +
                "(behalten: startMs zwischen $olderThanMs und $newerThanMs)",
        )
        if (deleted > 0) {
            syncCoordinatorLazy.get().requestSync("Training: Sync/Aenderung")
        }
        return deleted
    }

    fun observeWorkoutsByDate(dateKey: String): Flow<List<AmazfitWorkoutEntity>> =
        workoutDao.observeByDateKey(dateKey)

    fun observeWorkoutById(trackId: String): Flow<AmazfitWorkoutEntity?> =
        workoutDao.observeById(trackId)

    /**
     * Frank-Wunsch 2026-05-18: Loescht ein einzelnes Workout. Wird aus dem Training-Detail-Screen
     * ueber das 3-Punkte-Menue aufgerufen. Triggert anschliessend einen Sync-Pulse damit alle
     * Dashboards die Aenderung sofort mitbekommen (z.B. VO2max-Fallback in Aufgabe 7).
     */
    suspend fun deleteWorkoutByTrackId(trackId: String): Int {
        // Frank-Bugfix 2026-07-04: Start-Zeitstempel als Tombstone merken, BEVOR geloescht wird —
        // sonst importiert der naechste Health-Connect-Sync das Training sofort wieder.
        workoutDao.getById(trackId)?.let { appSettings.addDeletedWorkoutStart(it.startMs) }
        val deleted = workoutDao.deleteByTrackId(trackId)
        if (deleted > 0) {
            syncCoordinatorLazy.get().requestSync("Training: Sync/Aenderung")
        }
        return deleted
    }

    /**
     * Korrigiert bestehende Workout-Eintraege deren sportName auf einer veralteten Mapping-Logik
     * basiert. Frank-Wunsch 2026-05-10: Die T-Rex 3 sendet ALLE Workouts mit
     * source="run.NNN.huami.com", was den source-Prefix-Check als primaeres Sportart-Signal
     * unbrauchbar macht. Frueher gespeicherte Workouts mit Code 7 (Trailrunning), 12 (Crosstrainer)
     * oder 52 (Krafttraining) wurden deshalb faelschlich als "Laufen" gespeichert.
     *
     * Diese Migration laeuft idempotent — `updateSportNameByType` aktualisiert nur Zeilen wo der
     * Name aktuell abweicht. Bei wiederholten Aufrufen passiert nichts (0 Zeilen geaendert). Wird
     * beim App-Start einmal in EntropyReducerApp.onCreate via applicationScope gestartet.
     *
     * @return Anzahl korrigierter Zeilen (Summe ueber alle Override-Codes)
     */
    suspend fun applyFrankSportOverrides(): Int {
        // Performance-Audit Loop 1 (2026-05-10): Atomare Transaktion ueber alle
        // Override-Updates. Vorher: N separate Commits — bei Process-Kill mid-update
        // partielle Korrektur. Jetzt 1 Commit fuer alle Overrides.
        var changed = 0
        appDatabase.withTransaction {
            AmazfitSportNames.frankOverrides().forEach { (code, name) ->
                changed += workoutDao.updateSportNameByType(code, name)
            }
        }
        if (changed > 0) {
            Diag.i(DiagnosticArea.AMAZFIT, "AmazfitRepo", "Frank-Sport-Overrides angewendet: $changed Zeilen korrigiert")
        }
        return changed
    }

    // mergeFromPolar entfernt 2026-05-17 (Frank-Wunsch): Polar-Live-API
    // komplett raus. Polar-Daten kommen nur noch via Polar-ZIP-Bulk-Import.

    /**
     * Frank-Wunsch 2026-05-16: Workouts aus Health Connect als zweite Quelle importieren — vor
     * allem wenn die T-Rex 3 zwar via Bluetooth in die Zepp-App geschickt hat, aber Zepp die Daten
     * noch nicht in die Cloud hochgeladen hat. Health Connect hat den Workout-Datensatz dann schon
     * — wir mergen ihn in `amazfit_workouts` mit `source = "health_connect"`.
     *
     * Dedup-Strategie: Wir holen alle existierenden `startMs` der letzten [days] Tage aus der DB.
     * Eine HC-Session zaehlt als Duplikat wenn ein existierender Eintrag innerhalb von +/- 5
     * Minuten Start-Toleranz liegt. Damit gewinnt der Zepp-Cloud-Eintrag wenn er spaeter doch noch
     * kommt (er wird vorher importiert; HC-Session wird dann beim naechsten Sync als Duplikat
     * erkannt und nicht doppelt eingefuegt).
     *
     * trackId-Schema fuer HC-Workouts: `hc_$startMs` — stabil pro Session und faellt nicht mit
     * Zepp-Cloud-IDs zusammen.
     *
     * Liefert die Anzahl neu eingefuegter Eintraege zurueck (0 wenn HC nicht verfuegbar, Permission
     * fehlt oder alle Sessions schon in der DB sind).
     */
    suspend fun mergeFromHealthConnect(days: Int = 30): Int {
        if (!healthConnect.isAvailable()) {
            Diag.d(DiagnosticArea.AMAZFIT, TAG, "Health Connect nicht verfuegbar — kein Workout-Merge")
            return 0
        }
        val sessions = healthConnect.readExerciseSessions(days = days)
        if (sessions.isEmpty()) {
            Diag.d(DiagnosticArea.AMAZFIT, TAG, "Health Connect lieferte keine Exercise-Sessions im ${days}-Tage-Fenster")
            return 0
        }
        // Existierende Workouts im gleichen Zeitfenster (volle Entities fuer Dedup + Ersetzen).
        val end = System.currentTimeMillis()
        val start = end - days.toLong() * 24L * 60L * 60L * 1000L
        val toleranceMs = 5L * 60L * 1000L // 5 Minuten +/- ist immer noch derselbe Lauf
        val existing = workoutDao.observeRange(start, end).first()
        // Frank-Bugfix 2026-07-04: Tombstones manuell geloeschter Trainings — diese Sessions duerfen
        // NICHT wieder importiert werden, auch wenn sie in Health Connect noch existieren.
        val deletedStarts = appSettings.getDeletedWorkoutStarts()

        // Sync-Zeitstempel setzen sobald Health Connect erfolgreich Sessions lieferte (auch bei
        // 0 neuen) — so bleibt der "Zuletzt synchronisiert"-Status aktuell (frueher gesetzt beim Sync).
        appSettings.setLastAmazfitSync(System.currentTimeMillis())

        // Frank-Bugfix 2026-07-03: Nicht nur NEUE Trainings einfuegen, sondern bestehende LEERE
        // HC-Eintraege (z.B. datenarme Zepp-Version) durch die datenreichere HC-Version ERSETZEN.
        // Geschuetzt bleiben: bestehende Strava-Trainings (Frank will sie behalten) und manuell
        // editierte Eintraege (manualOverridesMs != null).
        var inserted = 0
        var replaced = 0
        var skippedDeleted = 0
        var mergedDuplicates = 0
        // Performance (Tiefen-Debugging 2026-07-04): ALLE Schreibvorgaenge des Merges in EINER
        // Room-Transaktion — vorher invalidierte jeder einzelne upsert die amazfit_workouts-Flows
        // (N Emissionen -> N komplette Biomarker-UI-Recompositions waehrend Frank scrollt).
        // Jetzt genau EINE Invalidation am Transaktionsende. Reihenfolge/Ergebnis identisch;
        // im Block laufen NUR DAO-Aufrufe (kein Netzwerk, kein Dispatcher-Wechsel — Room-BP §4).
        appDatabase.withTransaction {
            // Frank-Bugfix 2026-08-05: Bereits vorhandene Doppel-Eintraege desselben Laufs
            // aufraeumen (selbstheilend). Ursache siehe unten — der Dedup verglich nur gegen den
            // Stand VOR der Schleife, dadurch konnten zwei HC-Sessions desselben Laufs (Franks
            // Trail-Lauf vom 04.08.2026: zwei Records 2,9 s auseinander) beide eingefuegt werden.
            // Der aermere Eintrag stand dann als "neuester" oben in der Hero-Card, ohne Distanz
            // und Puls. Geloescht wird strikt verlustfrei: nur wenn der behaltene Eintrag JEDES
            // Feld belegt, das der geloeschte belegt (siehe [coversAllFieldsOf]).
            val known = existing.toMutableList()
            for (dropped in findRedundantDuplicates(known, toleranceMs)) {
                workoutDao.deleteByTrackId(dropped.trackId)
                known.remove(dropped)
                mergedDuplicates++
            }
            for (session in sessions) {
                // Manuell geloeschtes Training? Nicht erneut importieren (Tombstone gewinnt).
                if (deletedStarts.any { kotlin.math.abs(it - session.startMs) <= toleranceMs }) {
                    skippedDeleted++
                    continue
                }
                val newEntity =
                    healthConnectSessionToEntity(session).withEstablishedSportName(session)
                // Frank-Bugfix 2026-08-05: gegen [known] pruefen statt gegen [existing] — die Liste
                // waechst mit jedem Insert dieses Durchlaufs mit. Vorher war sie ein Schnappschuss
                // von VOR der Schleife, deshalb sah die zweite Session desselben Laufs die gerade
                // eingefuegte erste nicht und wurde als "neu" ein zweites Mal angelegt.
                val match = known.find { kotlin.math.abs(it.startMs - session.startMs) <= toleranceMs }
                when {
                    match == null -> {
                        workoutDao.upsert(newEntity)
                        known.add(newEntity)
                        inserted++
                    }
                    match.source == "strava" || match.manualOverridesMs != null -> {
                        // bewusst behalten — nicht ueberschreiben
                    }
                    // Frank-Bugfix 2026-08-05: Ein DATENAERMERER Treffer darf den bestehenden
                    // Eintrag nicht ueberschreiben. Ohne diese Schranke wuerde der Dedup-Fix oben
                    // die leere "Laufen"-Session den vollstaendigen "Traillauf" ueberschreiben
                    // lassen — der Doppel-Eintrag waere weg, aber die Werte auch. Ersetzt wird nur
                    // noch nach oben (Ziel des Zweigs: leere Eintraege anreichern).
                    !newEntity.coversAllFieldsOf(match) -> {
                        // bestehender Eintrag ist reicher — behalten
                    }
                    else -> {
                        // Frank-Bugfix: Wetter (Open-Meteo) vom bestehenden Eintrag uebernehmen — sonst
                        // wuerde die frische HC-Version (weather=null) das schon recherchierte Wetter
                        // loeschen und der Backfill muesste es neu holen (sichtbares Flackern). So bleibt
                        // ein einmal recherchierter Wert stabil; nur Trainings OHNE Wetter holt der Backfill.
                        val preserved =
                            newEntity.copy(
                                weatherTempCelsius = match.weatherTempCelsius,
                                weatherCondition = match.weatherCondition,
                                weatherFetchedMs = match.weatherFetchedMs,
                            )
                        if (match.trackId != preserved.trackId) workoutDao.deleteByTrackId(match.trackId)
                        workoutDao.upsert(preserved)
                        known.remove(match)
                        known.add(preserved)
                        replaced++
                    }
                }
            }
        }
        // Wetter-Rueckblick (Open-Meteo) fuer alle Trainings ohne Wetter nachziehen — neue UND
        // bestehende (auch wenn keine neuen Sessions kamen). Ein Fehler hier darf den
        // Trainings-Sync nie kippen.
        runCatchingCancellable { backfillMissingWeather() }
        // mergedDuplicates zaehlt mit: eine Bereinigung muss ins Drive-Backup, sonst spielt der
        // naechste Restore den geloeschten Doppel-Eintrag wieder ein.
        if (inserted + replaced + mergedDuplicates == 0) {
            Diag.d(
                DiagnosticArea.AMAZFIT,
                TAG,
                "Alle ${sessions.size} HC-Sessions bereits in der DB (oder geloescht: $skippedDeleted uebersprungen)",
            )
            return 0
        }
        // Neue/aktualisierte Trainings → Drive-Backup anstossen (debounced).
        syncCoordinatorLazy.get().requestSync("Training: Sync/Aenderung")
        Diag.i(
            DiagnosticArea.AMAZFIT,
            TAG,
            "Health-Connect-Workouts: $inserted neu, $replaced ersetzt, $skippedDeleted " +
                "geloescht-uebersprungen, $mergedDuplicates Doppel-Eintraege bereinigt " +
                "(von ${sessions.size})",
        )
        return inserted + replaced
    }

    /**
     * Behaelt den Namen, den diese Sportart in der Historie schon traegt.
     *
     * Health Connect liefert bei keiner Session einen Titel (Diagnose-Sonde 2026-08-05: alle
     * Quellen — Oura, Whoop, Zepp — schreiben `title=null`). Der Name entsteht deshalb aus dem
     * generischen Code-Mapping: Code 56 → "Laufen". Frank hat seine Laeufe aber in "Traillauf"
     * umbenannt, und jeder Sync machte das fuer die betroffenen Trainings wieder rueckgaengig —
     * daher standen im Juli zwei Laeufe als "Laufen" zwischen lauter "Traillauf"-Eintraegen.
     *
     * Liefert Health Connect doch einmal einen echten Titel, gewinnt dieser: eine benannte Quelle
     * ist immer besser als ein abgeleiteter Name.
     */
    private suspend fun AmazfitWorkoutEntity.withEstablishedSportName(
        session: HealthConnectExerciseSession,
    ): AmazfitWorkoutEntity {
        if (session.title != null) return this
        val type = sportType ?: return this
        val established = workoutDao.mostCommonSportNameForType(type) ?: return this
        return if (established == sportName) this else copy(sportName = established)
    }

    /**
     * Findet Doppel-Eintraege desselben Trainings (Start innerhalb [toleranceMs]) und liefert
     * genau die Eintraege zurueck, die gefahrlos geloescht werden koennen.
     *
     * Sicherheitsregeln (Direktive #3, Funktionalitaets-Erhaltung — im Zweifel bleibt beides
     * stehen, ein doppelter Eintrag ist harmloser als ein verlorener Messwert):
     * - geloescht wird nur, wenn der behaltene Eintrag JEDES Feld belegt, das der geloeschte
     *   belegt ([coversAllFieldsOf]) — es kann also kein Wert verschwinden;
     * - manuell editierte Eintraege (`manualOverridesMs`) und Strava-Trainings bleiben immer;
     * - sind zwei Eintraege gleich reich, bleibt der aeltere (der zuerst importierte).
     */
    private fun findRedundantDuplicates(
        workouts: List<AmazfitWorkoutEntity>,
        toleranceMs: Long,
    ): List<AmazfitWorkoutEntity> {
        val sorted = workouts.sortedBy { it.startMs }
        val dropped = ArrayList<AmazfitWorkoutEntity>()
        for (i in sorted.indices) {
            val a = sorted[i]
            if (a in dropped) continue
            for (j in i + 1 until sorted.size) {
                val b = sorted[j]
                if (b.startMs - a.startMs > toleranceMs) break
                if (b in dropped) continue
                val keep: AmazfitWorkoutEntity
                val drop: AmazfitWorkoutEntity
                if (a.coversAllFieldsOf(b)) {
                    keep = a
                    drop = b
                } else if (b.coversAllFieldsOf(a)) {
                    keep = b
                    drop = a
                } else {
                    continue // jeder hat etwas Eigenes — beide behalten
                }
                if (drop.manualOverridesMs != null || drop.source == "strava") continue
                dropped.add(drop)
                if (drop == a) break // a ist weg, naechstes Paar bildet sich um keep herum
            }
        }
        return dropped
    }

    /**
     * Wetter-Rueckblick pro Training (Open-Meteo, Frank-Wunsch): holt fuer alle Trainings mit
     * GPS-Track, die noch keinen Wetter-Abruf hatten (weatherFetchedMs IS NULL), die STUENDLICHE
     * Temperatur + Wetterlage am exakten Ort/zur exakten Zeit (Ortszeit) und schreibt sie ins
     * Entity. Auch bei erfolglosem Abruf wird weatherFetchedMs gesetzt, damit dasselbe Training
     * nicht bei jedem Sync erneut abgefragt wird (Poka-Yoke gegen endlose Retries + API-Last).
     * Deckt neue UND bestehende Trainings ab ("fuer jedes Training das dort abgespeichert ist").
     *
     * @param maxPerRun Obergrenze pro Durchlauf, damit ein erster Backfill mit vielen Alt-Trainings
     *   den Sync nicht unbegrenzt verlaengert; der Rest wird beim naechsten Sync nachgezogen.
     * @return Anzahl der Trainings, die tatsaechlich ein Wetter-Ergebnis bekamen.
     */
    suspend fun backfillMissingWeather(maxPerRun: Int = 200): Int {
        val candidates = workoutDao.getWorkoutsMissingWeather()
        if (candidates.isEmpty()) return 0
        var enriched = 0
        // Performance (Tiefen-Debugging 2026-07-04): Fetch-Phase und Write-Phase getrennt.
        // Vorher schrieb jeder Abruf sofort einzeln (updateWeather) -> bis zu 26 Room-
        // Invalidationen ueber ~4s gestreckt (delay), jede loeste eine KOMPLETTE Recomposition
        // aller sichtbaren Biomarker-Karten aus — genau waehrend Frank nach dem Tab-Wechsel
        // scrollt (sein Ruckel-Report). Jetzt: erst alle Netzwerk-Fetches sammeln (delay als
        // API-Schonung bleibt ZWISCHEN den Fetches), dann ALLE Writes in EINER Transaktion
        // = genau 1 Invalidation. Werte/Verhalten identisch, nur atomar statt troepfelnd.
        data class WeatherWrite(val trackId: String, val temp: Int?, val condition: String?, val fetchedMs: Long)
        val writes = ArrayList<WeatherWrite>(minOf(candidates.size, maxPerRun))
        for ((processed, w) in candidates.withIndex()) {
            if (processed >= maxPerRun) break
            val now = System.currentTimeMillis()
            val latLon = firstLatLon(w.gpsTrackJson)
            if (latLon == null) {
                // Kein brauchbarer GPS-Punkt -> Abruf-Zeitstempel setzen (nicht erneut versuchen).
                writes.add(WeatherWrite(w.trackId, null, null, now))
                continue
            }
            val info =
                weatherRepository.fetchWeatherFor(
                    lat = latLon.first,
                    lon = latLon.second,
                    startMs = w.startMs,
                    cityHint = w.city,
                    durationMinutes = w.durationSeconds?.let { it / 60 },
                )
            // Auch bei null (API-Fehler/keine Daten): fetchedMs setzen -> kein Dauer-Retry.
            writes.add(WeatherWrite(w.trackId, info?.tempCelsius, info?.condition, now))
            if (info != null) enriched++
            delay(150) // sanftes Rate-Limiting gegen Open-Meteo-Burst
        }
        if (writes.isNotEmpty()) {
            // NUR DAO-Aufrufe im Block (kein Netzwerk, kein Dispatcher-Wechsel — Room-BP §4).
            appDatabase.withTransaction {
                for (wr in writes) {
                    workoutDao.updateWeather(wr.trackId, wr.temp, wr.condition, wr.fetchedMs)
                }
            }
        }
        if (enriched > 0) {
            Diag.i(
                DiagnosticArea.AMAZFIT,
                TAG,
                "Wetter-Backfill: $enriched Trainings mit stuendlichem Wetter angereichert",
            )
            syncCoordinatorLazy.get().requestSync("Training: Wetter ergaenzt")
        }
        return enriched
    }

    /**
     * Erster [lat, lon] aus gpsTrackJson (Format [[lat,lon],...]). null wenn leer/unparsebar —
     * dann gibt es keinen Ort fuer die Wetter-Recherche.
     */
    private fun firstLatLon(gpsTrackJson: String?): Pair<Double, Double>? {
        if (gpsTrackJson.isNullOrBlank()) return null
        return runCatchingCancellable {
            val arr = Json.parseToJsonElement(gpsTrackJson).jsonArray
            val first = arr.firstOrNull()?.jsonArray ?: return@runCatchingCancellable null
            val lat = first.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return@runCatchingCancellable null
            val lon = first.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return@runCatchingCancellable null
            lat to lon
        }.getOrNull()
    }

    /**
     * Map Health-Connect ExerciseSessionRecord auf AmazfitWorkoutEntity. Source wird auf
     * "health_connect" gesetzt — damit erkennt die UI woher der Eintrag kommt und der naechste
     * Zepp-Sync kann ihn ggf. ueberschreiben. Title aus HC hat Vorrang vor dem generischen
     * Exercise-Type-Mapping, weil Zepp den genauen Sport (z.B. "Trailrunning") oft als Title
     * schreibt waehrend der Type-Int nur das grobe RUNNING ist.
     */
    private fun healthConnectSessionToEntity(
        s: HealthConnectExerciseSession
    ): AmazfitWorkoutEntity {
        val sportName = s.title?.takeIf { it.isNotBlank() } ?: exerciseTypeToGerman(s.exerciseType)
        val zone = ZoneId.systemDefault()
        val dateKey = Instant.ofEpochMilli(s.startMs).atZone(zone).toLocalDate().toString()
        return AmazfitWorkoutEntity(
            trackId = "hc_${s.startMs}",
            dateKey = dateKey,
            startMs = s.startMs,
            endMs = s.endMs,
            durationSeconds = s.durationSeconds,
            sportType = s.exerciseType,
            sportName = sportName,
            distanceMeters = s.distanceMeters,
            avgPaceSecPerKm = s.avgPaceSecPerKm,
            maxPaceSecPerKm = s.maxPaceSecPerKm,
            avgSpeedKmh = s.avgSpeedKmh,
            maxSpeedKmh = s.maxSpeedKmh,
            calories = s.calories,
            avgHeartRate = s.avgHeartRate,
            maxHeartRate = s.maxHeartRate,
            gpsTrackJson = s.gpsTrackJson,
            heartRateSeriesJson = s.heartRateSeriesJson,
            altitudeGainMeters = s.altitudeGainMeters,
            altitudeLossMeters = s.altitudeLossMeters,
            trainingEffectAerobic = null,
            trainingEffectAnaerobic = null,
            cadence = s.cadence,
            strideLengthCm = s.strideLengthCm,
            swolf = null,
            poolLengthMeters = null,
            source = "health_connect",
            city = null,
            paceStreamJson = s.paceStreamJson,
            createdAt = System.currentTimeMillis(),
        )
    }

    /**
     * Mapping der Health-Connect ExerciseType-Konstanten auf deutsche Bezeichnungen. Liste deckt
     * die haeufigsten Sportarten ab — alles unbekannte landet als "Training (Typ N)" damit Frank
     * weiss dass es ein neuer Code ist.
     */
    private fun exerciseTypeToGerman(type: Int): String =
        when (type) {
            0 -> "Training"
            2 -> "Badminton"
            4 -> "Baseball"
            5 -> "Basketball"
            8 -> "Radfahren"
            9 -> "Radfahren (Heimtrainer)"
            10 -> "Bootcamp"
            11 -> "Boxen"
            13 -> "Calisthenics"
            14 -> "Cricket"
            16 -> "Tanzen"
            25 -> "Crosstrainer"
            26 -> "Trainingsgruppe"
            27 -> "Fechten"
            28 -> "Football"
            29 -> "Australian Football"
            31 -> "Frisbee"
            32 -> "Golf"
            33 -> "Atemuebung"
            34 -> "Gymnastik"
            35 -> "Handball"
            36 -> "HIIT"
            37 -> "Wandern"
            38 -> "Eishockey"
            39 -> "Eislaufen"
            44 -> "Kampfsport"
            46 -> "Paddeln"
            47 -> "Gleitschirm"
            48 -> "Pilates"
            50 -> "Racquetball"
            51 -> "Klettern"
            52 -> "Rollhockey"
            53 -> "Rudern"
            54 -> "Rudergeraet"
            55 -> "Rugby"
            56 -> "Laufen"
            57 -> "Laufen (Laufband)"
            58 -> "Segeln"
            59 -> "Tauchen"
            60 -> "Skaten"
            61 -> "Ski"
            62 -> "Snowboard"
            63 -> "Schneeschuhwandern"
            64 -> "Fussball"
            65 -> "Softball"
            66 -> "Squash"
            68 -> "Treppensteigen"
            69 -> "Stepper"
            70 -> "Krafttraining"
            71 -> "Stretching"
            72 -> "Surfen"
            73 -> "Schwimmen (Freiwasser)"
            74 -> "Schwimmen (Pool)"
            75 -> "Tischtennis"
            76 -> "Tennis"
            78 -> "Volleyball"
            79 -> "Walking"
            80 -> "Wasserball"
            81 -> "Gewichtheben"
            82 -> "Rollstuhl-Training"
            83 -> "Yoga"
            else -> "Training (Typ $type)"
        }

    /**
     * Holt fuer ein bereits gespeichertes Workout die Detail-Daten (GPS-Track, Pulsverlauf, Pace
     * pro km, Splits) und schreibt sie in die Workout-Tabelle.
     *
     * Wird ON-DEMAND aufgerufen — wenn der Detail-Screen ein Training oeffnet — statt beim grossen
     * Sync (sonst: 288 Calls = lange + Rate-Limit).
     *
     * Cache-Verhalten (Frank-Befund 2026-05-09):
     * - paceStreamJson NICHT null/blank → Cache-Hit, kein API-Call
     * - paceStreamJson = " " (Marker fuer "Server lieferte nichts") → max alle 6h erneut versuchen
     * - force=true → Cache ignorieren, immer neu laden
     */
    suspend fun ensureWorkoutDetail(trackId: String, force: Boolean = false): Result<Boolean> =
        runCatching {
                val workout = workoutDao.getById(trackId) ?: return@runCatching false
                val source = workout.source ?: return@runCatching false

                // Frank-Wunsch 2026-05-16: Polar-Workouts werden ueber die Polar-API
                // refreshed (Direct-URL), nicht ueber den Zepp-Endpoint. Bei "polar-bulk"
                // (aus Bulk-Export-Datei) gibt es keinen Online-Endpoint — dann nichts tun.
                // 2026-05-17: Polar-Live-API entfernt — alte source="polar"-Eintraege
                // verhalten sich jetzt wie polar-bulk (kein Online-Refresh moeglich).
                if (source == "polar" || source == "polar-bulk") {
                    Diag.d(DiagnosticArea.AMAZFIT, 
                        TAG,
                        "Workout $trackId stammt aus Polar-Bulk-Import — kein API-Refresh moeglich",
                    )
                    return@runCatching false
                }

                // Cache-Logik: nicht jeder Detail-Open soll den Server quaelen, aber bei
                // alten Trainings ohne Daten soll man trotzdem nochmal probieren koennen.
                if (!force) {
                    val pace = workout.paceStreamJson
                    // Vollstaendiger Cache-Hit: echter Stream da → fertig.
                    if (!pace.isNullOrBlank() && pace != " ") {
                        return@runCatching false
                    }
                    // Marker-Hit (" "): Server hatte nichts. Max alle 6h erneut probieren.
                    if (pace == " ") {
                        val ageMs = System.currentTimeMillis() - (workout.createdAt)
                        if (ageMs < 6 * 60 * 60 * 1000L) {
                            return@runCatching false
                        }
                    }
                }
                // Zepp-Cloud-Detail-Fetch entfernt 2026-05-17 (Frank-Wunsch): Health-Connect-
                // Workouts haben Details schon beim Sync, Polar-Bulk hat keinen Online-
                // Endpoint. Andere source-Werte sind altlasten — kein Refresh moeglich.
                false
            }
            .onFailure {
                if (it !is kotlinx.coroutines.CancellationException) {
                    Diag.w(DiagnosticArea.AMAZFIT, TAG, "ensureWorkoutDetail fehlgeschlagen fuer $trackId: ${it.message}")
                }
            }

    // refreshPolarWorkout entfernt 2026-05-17 (Frank-Wunsch): Polar-Live-API raus.
    // Erhaltene Polar-Bulk-Workouts werden in ensureWorkoutDetail uebersprungen.

    /**
     * Frank-Wunsch 2026-05-19, geaendert 2026-05-23: Trainings aelter als [TRAINING_RETENTION_DAYS]
     * Tage loeschen (jetzt 1 Jahr, vorher 2). Polar-Bulk-Import brachte 957 Trainings auf das Geraet
     * (zurueck bis ~2018), die App wurde dadurch spuerbar langsam. 1 Jahr Historie reicht fuer alle
     * Charts, Korrelationen und Trend-Analysen. Idempotent: bei jedem Aufruf laeuft die gleiche
     * Loesch-Abfrage, die nichts mehr findet sobald die DB sauber ist.
     *
     * Threshold-Berechnung: `System.currentTimeMillis() - [TRAINING_RETENTION_DAYS] Tage` — frisch
     * pro Aufruf damit das Fenster rollend bleibt, auch wenn die App tagelang offen ist.
     *
     * Loescht NUR Workouts; die taeglichen amazfit_daily-Werte bleiben unberuehrt.
     */
    suspend fun pruneOldTrainings(): Int {
        val thresholdMs =
            System.currentTimeMillis() - TRAINING_RETENTION_DAYS * 24L * 60L * 60L * 1000L
        // Performance 2026-05-23 (Intelligenz-Vorschlag 1, Anti-Muster-Fix): count() statt
        // observeAll().first().size. Frueher wurden hier ZWEIMAL alle Workouts inkl. der grossen
        // Stream-Felder (GPS/Puls/Splits) in den RAM geladen, nur um die Zeilenzahl zu zaehlen.
        // count() macht das per SQL-COUNT(*) ohne ein einziges Workout zu materialisieren.
        // Verhaltensgleich: COUNT(*) == Listengroesse -> identischer deleted-Wert/Log/Sync-Trigger.
        val before = workoutDao.count()
        workoutDao.deleteOlderThan(thresholdMs)
        val after = workoutDao.count()
        val deleted = before - after
        if (deleted > 0) {
            Diag.i(DiagnosticArea.AMAZFIT, 
                TAG,
                "Trainings-Retention: $deleted Workouts aelter als $TRAINING_RETENTION_DAYS Tage geloescht ($before -> $after)",
            )
            syncCoordinatorLazy.get().requestSync("Training: Sync/Aenderung")
        }
        return deleted
    }

    /* =========================== Helpers =========================== */

    companion object {
        private const val TAG = "AmazfitRepository"
        /**
         * Frank-Wunsch 2026-05-19, geaendert 2026-05-23: Trainings nur 1 Jahr rueckwirkend halten
         * (vorher 2 Jahre = 730 Tage). Frank will Polar-Historie + Amazfit-Trainings auf das letzte
         * Jahr begrenzen — weniger Daten = schnelleres Scrollen im Biomarker-Bereich. Rollendes
         * 365-Tage-Fenster, frisch pro Aufruf berechnet.
         */
        const val TRAINING_RETENTION_DAYS = 365L
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}

/**
 * Mapping von Zepp-Sportart-Codes auf deutsche Namen. Quelle: Community-Reverse-Engineering der
 * Zepp-App. Unbekannte Codes werden als "Sport (Code N)" zurueckgegeben damit nichts verloren geht.
 */
/**
 * Sportart-Codes der Zepp-Cloud — empirisch aus Frank's echten Workouts gewonnen (Live-Test
 * 2026-05-09: Trailrunning kam als Code 7 zurueck). Codes ohne Verifikation sind als "Sport (Code
 * N)" markiert damit Frank weiss: unklar, kann falsch sein. Sobald wir weitere Sportarten in echten
 * Daten sehen, wird die Liste erweitert.
 */
/**
 * Sportart-Bestimmung — robust gegen falsche Code-Tabellen.
 *
 * Frank-Befund 2026-05-09 (dritte Iteration): das urspruengliche Code-zu-Name-Mapping aus
 * Community-Quellen war komplett falsch — Frank's Workouts wurden als "Bouldern" / "Curling" /
 * "Triathlon" angezeigt obwohl er das nie gemacht hat.
 *
 * Recherche 2026-05-09 ueber Gadgetbridge-Quellcode (HuamiSportsActivityType.java, lokal aus dem
 * Repo gelesen): Die Cloud-Sport-API verwendet ein anderes Mapping als die
 * Bluetooth-Geraete-Steuerung. Selbst innerhalb der Cloud-API koennen Geraete- Versionen abweichen.
 * Frank's "Code 7 = Trailrunning"-Live-Test widersprach sogar der offiziellen
 * HuamiSportsActivityType-Tabelle (Code 7 = OpenWaterSwimming).
 *
 * Korrekte Strategie (2026-05-09 final):
 * 1. SOURCE-PREFIX zuerst — am robustesten weil Zepp die Sportart-Familie selbst im
 *    source-Identifier kodiert ("run.NNN.huami.com" → Laufen, immer korrekt).
 * 2. Code-Map als Fallback wenn source fehlt — nur die in HuamiSportsActivityType verifizierten
 *    Codes, sonst "Sport (Code N)" neutral.
 * 3. KEINE geratenen Codes mehr — lieber neutral "Sport (Code N)" als faelschlich "Bouldern"
 *    anzuzeigen. Frank kann eine Sportart-Liste pflegen sobald er verifizierte Codes aus echten
 *    Workouts hat.
 *
 * Source-Beispiel aus Frank's Workout-Body: "run.8716545.huami.com" → Laufen.
 */
internal object AmazfitSportNames {

    /**
     * Verifizierte Cloud-API-Codes aus Gadgetbridge HuamiSportsActivityType.java
     * (Repository-Snapshot 2026-05-09). Diese Codes wurden vom Open-Source-Projekt empirisch aus
     * echten Zepp-Cloud-Antworten extrahiert.
     */
    private val VERIFIED_CODES: Map<Int, String> =
        mapOf(
            1 to "Laufen", // OutdoorRunning
            2 to "Laufband", // Treadmill
            3 to "Walking", // Walking
            4 to "Radfahren", // Cycling (Outdoor)
            5 to "Freies Training", // Exercise / Free training
            6 to "Schwimmen (Pool)", // Swimming
            7 to "Trailrunning", // Frank-Befund 2026-05-10: T-Rex 3 sendet
            // Code 7 fuer Trailrunning (23 Workouts in
            // Frank's DB). Gadgetbridge sagt OpenWaterSwimming —
            // die T-Rex 3 weicht hier ab.
            8 to "Indoor-Radfahren", // IndoorCycling
            9 to "Crosstrainer", // EllipticalTrainer
            10 to "Klettern", // Climbing
            12 to "Crosstrainer", // Frank-Befund 2026-05-10: T-Rex 3 sendet
            // Code 12 fuer Crosstrainer (haeufigster Code,
            // 132 Workouts in Frank's DB)
            15 to "Wandern", // OutdoorHiking
            18 to "Fussball", // Soccer (0x12)
            21 to "Seilspringen", // JumpRope (0x15)
            23 to "Rudermaschine", // RowingMachine (0x17)
            52 to "Krafttraining", // StrengthTraining (0x34)
            60 to "Yoga", // Yoga (0x3c)
            78 to "Cricket", // Cricket (0x4e)
            85 to "Basketball", // Basketball (0x55)
            89 to "Tischtennis", // PingPong (0x59)
            92 to "Badminton", // Badminton (0x5c)
        )

    /**
     * Frank-bestaetigte Override-Codes (T-Rex 3 spezifisch, Stand 2026-05-10).
     *
     * Hintergrund: Die T-Rex 3 sendet ALLE Workouts mit source="run.NNN.huami.com" (NNN =
     * Geraete-ID, nicht Sportart). Das macht den source-Prefix als primaeres Sportart-Signal
     * unbrauchbar — er liefert immer "Laufen", auch fuer Crosstrainer oder Krafttraining.
     *
     * Diese Codes hat Frank manuell gegen seine Trainings-Erinnerung verifiziert (15.04.2026 18:15
     * = Crosstrainer mit Code 12; 14.03.2026 16:27 = Krafttraining mit Code 52). Sie werden VOR dem
     * source-Prefix-Check angewendet, sodass der generische "run."-Prefix sie nicht ueberstimmen
     * kann.
     *
     * Andere unbekannte T-Rex-3-Codes (16, 22, 24, 47, 57, 66) bleiben ohne Override — dort wird
     * der bisherige Pfad genutzt (source-Prefix oder "Sport (Code N)"), bis Frank die Sportart
     * bestaetigt.
     */
    private val FRANK_VERIFIED_OVERRIDES: Map<Int, String> =
        mapOf(
            7 to "Trailrunning", // Frank-Befund 2026-05-10 (Live-Test 2026-05-09)
            12 to "Crosstrainer", // Frank-Befund 15.04.2026 (132 historische Workouts)
            52 to "Krafttraining", // Frank-Befund 14.03.2026
        )

    /**
     * Liefert die Frank-bestaetigten Code-Overrides fuer DB-Migrationen. Wird von
     * [AmazfitRepository.applyFrankSportOverrides] genutzt um bestehende Workout-Eintraege mit
     * faelschlich gespeicherten Namen ("Laufen" statt Crosstrainer/Krafttraining) zu korrigieren.
     */
    fun frankOverrides(): Map<Int, String> = FRANK_VERIFIED_OVERRIDES

    fun nameOf(type: Int?, source: String? = null): String {
        // Schritt 0: Frank-Override-Codes — diese stehen UEBER dem source-Prefix,
        // weil die T-Rex 3 fuer ALLE Sportarten den gleichen "run.*"-Prefix sendet.
        // Frank hat diese Codes manuell verifiziert; sie sind das verlaesslichste
        // Signal fuer die Sportart (Frank-Befund 2026-05-10).
        if (type != null) {
            FRANK_VERIFIED_OVERRIDES[type]?.let {
                return it
            }
        }
        // Schritt 1: SOURCE-PREFIX als zweite Wahl. Bei Geraeten die keine generische
        // run.*-Source senden (alte Amazfit-Modelle, andere Hersteller) ist der
        // Prefix verlaesslich — daher bleibt diese Logik fuer Codes erhalten, die
        // noch nicht im Override stehen.
        val srcPrefix = source?.substringBefore(".")?.lowercase()
        when (srcPrefix) {
            "run" -> return "Laufen"
            "trailrun",
            "trail" -> return "Trailrunning"
            "bike",
            "cycle",
            "cycling" -> return "Radfahren"
            "indoorbike",
            "indoorcycle" -> return "Indoor-Radfahren"
            "swim",
            "swimming" -> return "Schwimmen"
            "openswim" -> return "Freiwasser-Schwimmen"
            "walk",
            "walking" -> return "Walking"
            "hike",
            "hiking" -> return "Wandern"
            "ski",
            "skiing" -> return "Skifahren"
            "yoga" -> return "Yoga"
            "strength" -> return "Krafttraining"
            "treadmill" -> return "Laufband"
            "elliptical" -> return "Crosstrainer"
            "rowing" -> return "Rudern"
            "climb",
            "climbing" -> return "Klettern"
        }
        // Schritt 2: Code-Map als Fallback. Nur verifizierte Codes verwenden.
        if (type != null) {
            VERIFIED_CODES[type]?.let {
                return it
            }
            return "Sport (Code $type)"
        }
        return "Unbekannt"
    }
}

/**
 * Alle Mess-/Detailfelder eines Trainings, die einen Doppel-Eintrag wertvoll machen koennen.
 * Bewusst OHNE Identitaets- und Verwaltungsfelder (trackId, dateKey, startMs, source, Wetter,
 * createdAt) — die sagen nichts darueber aus, welcher von zwei Eintraegen mehr Daten traegt.
 */
private val WORKOUT_VALUE_FIELDS: List<(AmazfitWorkoutEntity) -> Any?> =
    listOf(
        { it.durationSeconds },
        { it.sportType },
        { it.sportName },
        { it.distanceMeters },
        { it.avgPaceSecPerKm },
        { it.maxPaceSecPerKm },
        { it.avgSpeedKmh },
        { it.maxSpeedKmh },
        { it.calories },
        { it.avgHeartRate },
        { it.maxHeartRate },
        { it.gpsTrackJson },
        { it.heartRateSeriesJson },
        { it.paceSeriesJson },
        { it.paceStreamJson },
        { it.splitsJson },
        { it.altitudeGainMeters },
        { it.altitudeLossMeters },
        { it.trainingEffectAerobic },
        { it.trainingEffectAnaerobic },
        { it.vo2Max },
        { it.cadence },
        { it.strideLengthCm },
        { it.recoveryTimeHours },
        { it.skinTempCelsius },
        { it.swolf },
        { it.poolLaps },
        { it.poolLengthMeters },
        { it.city },
    )

/**
 * True, wenn dieses Training jedes Feld belegt, das [other] belegt — dann steckt in [other] kein
 * Wert, den es hier nicht auch gibt, und [other] darf als Doppel-Eintrag entfallen.
 *
 * Bewusst nur "belegt vs. leer", kein Wert-Vergleich: zwei Quellen desselben Laufs weichen in den
 * Zahlen immer leicht ab (Franks Fall: 3620 s vs. 3609 s). Entschieden wird ueber Vollstaendigkeit,
 * nicht darueber, welche Zahl "richtiger" ist.
 */
private fun AmazfitWorkoutEntity.coversAllFieldsOf(other: AmazfitWorkoutEntity): Boolean =
    WORKOUT_VALUE_FIELDS.none { field -> field(other) != null && field(this) == null }

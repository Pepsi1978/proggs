package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tagliche Werte von der Amazfit T-Rex 3 (Frank-Wunsch 2026-05-09).
 *
 * Bewusst SEPARATE Tabelle parallel zu biomarker_snapshots (Whoop) — so kann die
 * App je Quelle eigene Daten halten und im UI mit "T-Rex 3"-Label anzeigen, ohne
 * dass die Whoop-Werte ueberschrieben werden. Werte die beide Geraete liefern
 * (Hauttemperatur, SpO2, Ruhepuls) erscheinen also doppelt — aber mit klarer
 * Quellen-Markierung.
 *
 * `date` ist der Tagesschluessel im Format "YYYY-MM-DD" (lokale Zeit) und Primaerschluessel
 * — pro Tag ein Eintrag, bei einem zweiten Sync wird der Eintrag aktualisiert.
 */
@Entity(
    tableName = "amazfit_daily",
    indices = [Index("capturedAt")],
)
data class AmazfitDailyEntity(
    @PrimaryKey val date: String,
    val capturedAt: Long,
    /** PAI-Score (Personal Activity Intelligence) — Wochenwert 0-200+. */
    val paiScore: Int? = null,
    /** BioCharge — Energie-Level 0-100, Zepps Aequivalent zu Garmins Body Battery. */
    val bioChargeScore: Int? = null,
    /** Hauttemperatur in °C (kontinuierliche Messung im Schlaf). */
    val skinTempCelsius: Double? = null,
    /** Sauerstoffsattigung in % (kontinuierlich oder Spot-Check). */
    val spo2Percent: Double? = null,
    /** Stress-Score 1-100 aus HRV abgeleitet. */
    val stressScore: Int? = null,
    /** Atemfrequenz in Atemzuegen pro Minute. */
    val respiratoryRate: Double? = null,
    /** Schrittzahl des Tages. */
    val steps: Int? = null,
    /** Zurueckgelegte Distanz in Metern (Schritt-basiert). */
    val distanceMeters: Double? = null,
    /** Aktive Kalorien des Tages. */
    val activeCalories: Double? = null,
    /** Aktive Minuten (Bewegungsminuten oberhalb Ruhe-Schwelle). */
    val activeMinutes: Int? = null,
    /** Tages-Ruhepuls. */
    val restingHeartRate: Int? = null,
    /** Tages-Durchschnittspuls. */
    val averageHeartRate: Int? = null,
    /** HRV (Herzraten-Variabilitaet) in ms — RMSSD. */
    val hrvMs: Double? = null,
    /** Schlaf-Felder aus dem band_data-Summary (slp_*-Schluessel). */
    val sleepTotalMinutes: Int? = null,
    val sleepDeepMinutes: Int? = null,
    val sleepLightMinutes: Int? = null,
    val sleepWakeMinutes: Int? = null,
    val sleepRemMinutes: Int? = null,
    val sleepScore: Int? = null,
    val createdAt: Long,
)

/**
 * Sport-Sessions / Workouts von der Amazfit T-Rex 3 (Frank-Wunsch 2026-05-09:
 * "alles was mit Sport zu tun hat — ich sortiere im Nachhinein aus").
 *
 * Reichhaltige Felder: GPS-Track + Pulsverlauf + Pace-Verlauf + Splits werden
 * jeweils als JSON-String gespeichert (Liste von Punkten/Werten). UI parst sie
 * fuer die Detail-Ansicht.
 *
 * `trackId` ist der Zepp-eigene eindeutige Identifier des Trainings, vom Server
 * geliefert — wird als Primaerschluessel genutzt damit Re-Syncs idempotent sind.
 */
@Entity(
    tableName = "amazfit_workouts",
    indices = [Index("startMs"), Index("dateKey")],
)
data class AmazfitWorkoutEntity(
    @PrimaryKey val trackId: String,
    val dateKey: String,
    val startMs: Long,
    val endMs: Long,
    val durationSeconds: Long? = null,
    /** Zepp-Sportart-Code (1=Lauf, 6=Rad, 8=Wandern, 9=Schwimmen-Pool, ...). */
    val sportType: Int? = null,
    /** Lesbarer Name auf Deutsch (z.B. "Laufen", "Radfahren"). */
    val sportName: String? = null,
    val distanceMeters: Double? = null,
    /** Pace in Sekunden pro Kilometer (Durchschnitt). */
    val avgPaceSecPerKm: Double? = null,
    /** Maximale Pace = schnellste Sekunden pro Kilometer (kleinster Wert). */
    val maxPaceSecPerKm: Double? = null,
    val avgSpeedKmh: Double? = null,
    val maxSpeedKmh: Double? = null,
    val calories: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    /** GPS-Track als JSON-Array: [[lat, lon, alt, ts], ...]. Null wenn keine GPS-Daten. */
    val gpsTrackJson: String? = null,
    /** Pulsverlauf als JSON-Array: [[ts, hr], ...]. */
    val heartRateSeriesJson: String? = null,
    /** Pace-Verlauf pro Kilometer/Minute als JSON-Array. */
    val paceSeriesJson: String? = null,
    /** Splits (km-/Meilen-Splits) als JSON-Array. */
    val splitsJson: String? = null,
    val altitudeGainMeters: Double? = null,
    val altitudeLossMeters: Double? = null,
    /** Trainingseffekt aerob (0.0 - 5.0). */
    val trainingEffectAerobic: Double? = null,
    /** Trainingseffekt anaerob (0.0 - 5.0). */
    val trainingEffectAnaerobic: Double? = null,
    /** VO2Max-Schaetzung (typischer Wertebereich 20-70 ml/kg/min). */
    val vo2Max: Double? = null,
    /** Schrittfrequenz (Schritte pro Minute). */
    val cadence: Int? = null,
    /** Schrittlaenge in cm. */
    val strideLengthCm: Int? = null,
    /** Erholungszeit-Vorschlag in Stunden. */
    val recoveryTimeHours: Int? = null,
    /** Hauttemperatur waehrend des Workouts in °C (Mittelwert). */
    val skinTempCelsius: Double? = null,
    /** Schwimm-spezifisch: SWOLF-Wert. */
    val swolf: Int? = null,
    /** Schwimm-spezifisch: Anzahl Bahnen. */
    val poolLaps: Int? = null,
    /** Pool-Laenge in Metern (bei Schwimm-Workouts). */
    val poolLengthMeters: Double? = null,
    /** source-Identifier (z.B. "run.8716545.huami.com") — Pflicht-Parameter fuer
     *  den Workout-Detail-Endpoint. */
    val source: String? = null,
    /** Stadt/Ort wo das Workout stattfand (z.B. "Moenchwinkel"). */
    val city: String? = null,
    /** Hochaufgelöster Pace-Stream (~ein Sample pro Sekunde) aus dem
     *  detail.json `pace`-Feld. Frank-Wunsch 2026-05-09: fluessiger Tempo-
     *  Verlauf statt nur 7 Km-Splits. */
    val paceStreamJson: String? = null,
    /** Frank-Wunsch 2026-05-17: Zeitstempel (epoch-ms) wann der Benutzer
     *  zuletzt manuell einzelne Werte ueberschrieben hat. null = keine
     *  manuellen Edits. Beim Trainings-Sync werden bei gesetztem Wert die
     *  Summary-Felder NICHT mehr ueberschrieben — nur Streams (GPS, HR,
     *  Pace) werden aktualisiert. */
    val manualOverridesMs: Long? = null,
    /** Frank-Wunsch 2026-05-17: Komma-separierte Liste der Labels (genau wie
     *  im Detail-Screen StatsGrid angezeigt) der Felder die manuell editiert
     *  wurden. Beispiel: "Ø Puls,Schrittlänge,Kalorien". null oder leer =
     *  kein Feld manuell editiert. Wird im DetailScreen genutzt um pro
     *  Stat-Karte ein Schloss-Icon anzuzeigen. */
    val manualOverrideFields: String? = null,
    /** Frank-Wunsch (Wetter pro Training, Open-Meteo): Lufttemperatur in °C zum
     *  Trainings-Start (gerundet). null = nicht recherchiert / kein GPS / API-Fehler. */
    val weatherTempCelsius: Int? = null,
    /** Wetterlage als deutsches Einzelwort (z.B. "sonnig", "bewölkt", "regnerisch")
     *  aus dem WMO-Code — steuert das Wetter-Icon in der Trainings-Anzeige. */
    val weatherCondition: String? = null,
    /** Zeitstempel (epoch-ms) wann das Wetter zuletzt geholt wurde. null = noch nie
     *  versucht; gesetzt (auch wenn kein Ergebnis kam) = nicht erneut abfragen. */
    val weatherFetchedMs: Long? = null,
    val createdAt: Long,
)

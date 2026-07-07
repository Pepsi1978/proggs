package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Cross-Device-Cache fuer Health-Connect-Werte (Frank-Wunsch 2026-05-10 abend).
 *
 * Hintergrund: Zepp pusht nur neue Wiegungen ab dem Zeitpunkt der HC-Verknuepfung
 * in Health Connect — keine rueckwirkende Historie. Damit hat HC auf einem neuen
 * Geraet (z.B. S23 Ultra) sehr wenig Daten, obwohl die Zepp-Cloud die volle
 * Historie kennt. Frank's Fold 6 hat dagegen den vollen Verlauf in HC weil die
 * HC-Verknuepfung dort schon laenger aktiv ist.
 *
 * Loesung: Diese Tabelle cached jeden Wert den die App via Health Connect liest.
 * Beim Drive-Backup wird sie mit-exportiert. Beim Restore auf einem neuen Geraet
 * landen die Werte hier — die App liest dann die Vereinigung aus aktuellem HC
 * plus diesem Cache (mit Timestamp als Dedup-Key).
 *
 * Schema:
 *  - metric: Bezeichner der Messgroesse (siehe HealthConnectMetricKey im Compose-Code).
 *    Beispiele: "weight", "body_fat", "lean_body_mass", "body_water", "bone_mass".
 *  - timestampMs: Zeitpunkt der Messung in Epoch-Millisekunden. Zusammen mit
 *    `metric` der zusammengesetzte PrimaryKey — Idempotenz beim wiederholten
 *    Schreiben derselben HC-Werte.
 *  - value: Numerischer Wert (kg, %, etc. — Einheit ergibt sich aus der Metric).
 *  - createdAt: Wann wurde der Cache-Eintrag in die DB geschrieben (nur fuer
 *    Diagnose, kein UI-Use-Case).
 */
@Entity(
    tableName = "hc_value_cache",
    primaryKeys = ["metric", "timestampMs"],
    indices = [Index("metric")],
)
data class HealthConnectValueEntity(
    val metric: String,
    val timestampMs: Long,
    val value: Double,
    val createdAt: Long,
)

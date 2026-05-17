package de.frank.entropyreducer.presentation.amazfit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import java.util.Locale

/**
 * Manuelle Override-Werte fuer ein Training (Frank-Wunsch 2026-05-17).
 *
 * Frank kann einzelne Werte nachtraeglich eintragen wenn Strava sie nicht
 * geliefert hat (z.B. Hoehenverlust, Schrittlaenge, oder wenn der Puls
 * fehlerhaft uebertragen wurde). VO2max ist BEWUSST ausgenommen — der wird
 * IMMER aus avgHeartRate + distance + duration berechnet (ACSM-Formel).
 *
 * Jedes Feld ist nullable: null = unveraendert lassen, Wert = ueberschreiben.
 * Das Repository nutzt `?: existing.X` damit nicht-eingetragene Felder den
 * existierenden Wert behalten.
 */
data class ManualWorkoutOverrides(
    val avgPaceSecPerKm: Double? = null,
    val maxPaceSecPerKm: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val altitudeGainMeters: Double? = null,
    val altitudeLossMeters: Double? = null,
    val cadence: Int? = null,
    val strideLengthCm: Int? = null,
    val calories: Double? = null,
)

/**
 * Dialog zum manuellen Editieren von Trainings-Werten (Frank-Wunsch 2026-05-17).
 *
 * Erscheint als Alert-Dialog mit 9 Eingabefeldern. Vorbelegt mit den aktuellen
 * Werten des Workouts — Frank tippt nur den Wert ein, die Einheit wird im Label
 * angezeigt (z.B. "135" + Label "Ø Puls (bpm)"). Bei "Speichern" werden die
 * geparsten Werte als [ManualWorkoutOverrides] zurueckgegeben.
 *
 * Pace-Felder akzeptieren "M:SS" oder "MM:SS" Format (z.B. "3:36", "5:00").
 * Wenn parse fehlschlaegt oder Feld leer ist → null = existing-Wert bleibt.
 */
@Composable
fun EditTrainingValuesDialog(
    workout: AmazfitWorkoutEntity,
    onDismiss: () -> Unit,
    onSave: (ManualWorkoutOverrides) -> Unit,
) {
    // Vorbelegung mit aktuellen Werten (formatiert).
    var avgPace by remember { mutableStateOf(workout.avgPaceSecPerKm?.let { formatPaceMmSs(it) } ?: "") }
    var maxPace by remember { mutableStateOf(workout.maxPaceSecPerKm?.let { formatPaceMmSs(it) } ?: "") }
    var avgHr by remember { mutableStateOf(workout.avgHeartRate?.toString() ?: "") }
    var maxHr by remember { mutableStateOf(workout.maxHeartRate?.toString() ?: "") }
    var altGain by remember { mutableStateOf(workout.altitudeGainMeters?.let { formatDouble(it) } ?: "") }
    var altLoss by remember { mutableStateOf(workout.altitudeLossMeters?.let { formatDouble(it) } ?: "") }
    var cadence by remember { mutableStateOf(workout.cadence?.toString() ?: "") }
    var stride by remember { mutableStateOf(workout.strideLengthCm?.toString() ?: "") }
    var calories by remember { mutableStateOf(workout.calories?.let { formatDouble(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Werte manuell anpassen",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Felder leer lassen behaelt den aktuellen Wert. VO₂Max wird automatisch aus Puls + Distanz + Dauer berechnet.",
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(4.dp))

                // Pace-Zeile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = avgPace,
                        onValueChange = { avgPace = it },
                        label = { Text("Ø Pace (min/km)") },
                        placeholder = { Text("3:36") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = maxPace,
                        onValueChange = { maxPace = it },
                        label = { Text("Max Pace (min/km)") },
                        placeholder = { Text("3:36") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Puls-Zeile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = avgHr,
                        onValueChange = { s -> avgHr = s.filter { it.isDigit() } },
                        label = { Text("Ø Puls (bpm)") },
                        placeholder = { Text("135") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = maxHr,
                        onValueChange = { s -> maxHr = s.filter { it.isDigit() } },
                        label = { Text("Max Puls (bpm)") },
                        placeholder = { Text("178") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Hoehe-Zeile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = altGain,
                        onValueChange = { s -> altGain = s.filter { it.isDigit() || it == '.' || it == ',' } },
                        label = { Text("Höhe ↑ (m)") },
                        placeholder = { Text("210") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = altLoss,
                        onValueChange = { s -> altLoss = s.filter { it.isDigit() || it == '.' || it == ',' } },
                        label = { Text("Höhe ↓ (m)") },
                        placeholder = { Text("205") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Schritt-Zeile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = cadence,
                        onValueChange = { s -> cadence = s.filter { it.isDigit() } },
                        label = { Text("Schrittfrequenz (spm)") },
                        placeholder = { Text("85") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = stride,
                        onValueChange = { s -> stride = s.filter { it.isDigit() } },
                        label = { Text("Schrittlänge (cm)") },
                        placeholder = { Text("98") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Kalorien
                OutlinedTextField(
                    value = calories,
                    onValueChange = { s -> calories = s.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text("Kalorien (kcal)") },
                    placeholder = { Text("450") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val overrides = ManualWorkoutOverrides(
                    avgPaceSecPerKm = parseMmSs(avgPace),
                    maxPaceSecPerKm = parseMmSs(maxPace),
                    avgHeartRate = avgHr.toIntOrNull(),
                    maxHeartRate = maxHr.toIntOrNull(),
                    altitudeGainMeters = parseDouble(altGain),
                    altitudeLossMeters = parseDouble(altLoss),
                    cadence = cadence.toIntOrNull(),
                    strideLengthCm = stride.toIntOrNull(),
                    calories = parseDouble(calories),
                )
                onSave(overrides)
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}

/**
 * Parst "M:SS" oder "MM:SS" zu sec/km. Beispiel: "3:36" → 216.0, "5:00" → 300.0.
 * Leerstring oder ungueltiges Format → null (= existing-Wert behalten).
 */
private fun parseMmSs(s: String): Double? {
    val trimmed = s.trim()
    if (trimmed.isEmpty()) return null
    // "M:SS" oder "MM:SS"
    val parts = trimmed.split(":")
    if (parts.size != 2) return null
    val min = parts[0].toIntOrNull() ?: return null
    val sec = parts[1].toIntOrNull() ?: return null
    if (min < 0 || sec < 0 || sec >= 60) return null
    val total = min * 60.0 + sec
    if (total < 60.0 || total > 1800.0) return null // Sanity: 1:00 - 30:00
    return total
}

/**
 * Parst Double mit Komma- oder Punkt-Trennzeichen. Leer → null.
 */
private fun parseDouble(s: String): Double? {
    val trimmed = s.trim()
    if (trimmed.isEmpty()) return null
    return trimmed.replace(",", ".").toDoubleOrNull()
}

/** Formatiert sec/km als "M:SS" Pace. */
private fun formatPaceMmSs(secPerKm: Double): String {
    val total = secPerKm.toInt().coerceAtLeast(0)
    val min = total / 60
    val sec = total % 60
    return "%d:%02d".format(Locale.US, min, sec)
}

/** Formatiert Double ohne unnoetige Nachkommastellen (200.0 → "200", 207.5 → "207,5"). */
private fun formatDouble(v: Double): String {
    return if (v == v.toLong().toDouble()) {
        v.toLong().toString()
    } else {
        "%.1f".format(Locale.GERMANY, v)
    }
}

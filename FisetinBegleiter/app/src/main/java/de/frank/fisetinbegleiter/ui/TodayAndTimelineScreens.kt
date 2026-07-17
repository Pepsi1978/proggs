package de.frank.fisetinbegleiter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.fisetinbegleiter.data.AppCureState
import de.frank.fisetinbegleiter.data.IngredientPhase
import de.frank.fisetinbegleiter.domain.timeline
import de.frank.fisetinbegleiter.ui.theme.AllowedGreen
import de.frank.fisetinbegleiter.ui.theme.AllowedGreenContainer
import de.frank.fisetinbegleiter.ui.theme.BlockedRed
import de.frank.fisetinbegleiter.ui.theme.BlockedRedContainer
import de.frank.fisetinbegleiter.ui.theme.WarningYellow
import de.frank.fisetinbegleiter.ui.theme.WarningYellowContainer

@Composable
fun TodayScreen(
    state: MainUiState,
    onCreateCure: (Int, String, String, Int) -> Unit,
    onDrinkNow: () -> Unit,
    onGoTimeline: () -> Unit,
    onGoStack: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    exactAlarmsAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    var showSetup by remember { mutableStateOf(false) }
    var showDrink by remember { mutableStateOf(false) }
    if (!state.initialized) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle(
                "Heute",
                "Dein persönliches Protokoll, Schritt für Schritt.",
            )
        }
        item {
            SignalCard(
                title = if (state.blockActive) "Sperrfenster aktiv" else "Alles im erlaubten Bereich",
                text = if (state.blockActive) {
                    "Noch ${formatCountdown(state.blockEnd!!, state.now)} bis Antioxidantien wieder erlaubt sind."
                } else {
                    "Grünes Licht: Aktuell ist keine Antioxidantien-Sperre aktiv."
                },
                containerColor = if (state.blockActive) WarningYellowContainer else AllowedGreenContainer,
                contentColor = if (state.blockActive) WarningYellow else AllowedGreen,
            )
        }
        item {
            SignalCard(
                title = "Venlafaxin immer einnehmen",
                text = "Medikament – nie wegen der Kur weglassen. Die Kur verändert deinen Einnahmeplan nicht.",
                containerColor = AllowedGreenContainer,
                contentColor = AllowedGreen,
            )
        }
        if (!exactAlarmsAvailable) {
            item {
                SignalCard(
                    title = "Exakte Alarme freigeben",
                    text = "Android erlaubt derzeit nur angenäherte Erinnerungen. Gib exakte Alarme für punktgenaue Timer frei.",
                    containerColor = WarningYellowContainer,
                    contentColor = WarningYellow,
                    trailing = { TextButton(onClick = onOpenExactAlarmSettings) { Text("Öffnen") } },
                )
            }
        }
        item {
            OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Akku-Optimierung für zuverlässige Erinnerungen prüfen")
            }
        }
        when (state.cureState) {
            AppCureState.KEINE_KUR, AppCureState.KUR_ABGESCHLOSSEN -> {
                item {
                    SignalCard(
                        title = "Keine Kur aktiv",
                        text = "Lege die Kur in einen Freiblock: nüchterner Morgen, Mahlzeiten und das 4-Stunden-Fenster sind an Tagdienst-Tagen praktisch nicht durchführbar.",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
                item { Button(onClick = { showSetup = true }, modifier = Modifier.fillMaxWidth()) { Text("Neue Kur starten") } }
            }
            else -> {
                val cure = state.activeCure!!.cure
                val day = state.currentDay!!
                item { Text("Tag ${day.dayNumber} von ${cure.plannedDurationDays}", style = MaterialTheme.typography.headlineMedium) }
                if (day.t0 == null) {
                    item {
                        SignalCard(
                            title = "Nächster Schritt: Fisetin-Drink",
                            text = "Morgens nüchtern. Öffne die Zutatenliste und setze T0 erst direkt nach dem Trinken.",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    item { Button(onClick = { showDrink = true }, modifier = Modifier.fillMaxWidth()) { Text("Drink-Checkliste öffnen") } }
                } else {
                    item {
                        NextStepCard(state, onGoTimeline)
                    }
                    item { OutlinedButton(onClick = onGoStack, modifier = Modifier.fillMaxWidth()) { Text("Angepassten Morgen-Stack ansehen") } }
                }
            }
        }
        item {
            SignalCard(
                title = "Harte Regel",
                text = "Kein Vitamin C oder andere starke Antioxidantien zeitgleich oder in den ersten 4 Stunden nach T0.",
                containerColor = BlockedRedContainer,
                contentColor = BlockedRed,
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showSetup) {
        SetupDialog(
            defaultDuration = state.protocol.standardDurationDays,
            defaultStartMinute = state.protocol.preferredStartMinuteOfDay,
            onDismiss = { showSetup = false },
            onConfirm = { duration, liquid, fat, startMinute ->
                onCreateCure(duration, liquid, fat, startMinute)
                showSetup = false
            },
        )
    }
    if (showDrink) {
        DrinkDialog(
            ingredients = state.ingredients.filter { it.phase == IngredientPhase.DRINK },
            carrierLiquid = state.activeCure?.cure?.carrierLiquid.orEmpty(),
            fatCarrier = state.activeCure?.cure?.fatCarrier.orEmpty(),
            onDismiss = { showDrink = false },
            onDrinkNow = {
                onDrinkNow()
                showDrink = false
            },
        )
    }
}

@Composable
private fun NextStepCard(state: MainUiState, onGoTimeline: () -> Unit) {
    val day = state.currentDay ?: return
    val times = timeline(day.t0 ?: return, state.protocol)
    val (title, text) = when {
        day.mealDoneAt == null && state.now < times.mealDeadline -> "Fettige Mahlzeit" to "Bis ${formatTime(times.mealDeadline)} Uhr, noch ${formatCountdown(times.mealDeadline, state.now)}."
        day.spermidinDoneAt == null && state.now < times.spermidinStart -> "Spermidin-Fenster folgt" to "Öffnet um ${formatTime(times.spermidinStart)} Uhr."
        day.spermidinDoneAt == null && state.now < times.spermidinEnd -> "Spermidin-Schritt jetzt" to "6 mg Spermidin, optional Piperin und eine leichte Mahlzeit."
        state.blockActive -> "Sperrfenster läuft" to "Antioxidantien ab ${formatTime(times.blockEnd)} Uhr wieder erlaubt."
        state.canCompleteDay -> "Tag bereit zum Abschluss" to "Alle erforderlichen Schritte sind erledigt."
        else -> "Ablauf prüfen" to "Offene Schritte in der Timeline ansehen."
    }
    SignalCard(
        title = title,
        text = text,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        trailing = { TextButton(onClick = onGoTimeline) { Text("Öffnen") } },
    )
}

@Composable
private fun SetupDialog(
    defaultDuration: Int,
    defaultStartMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, Int) -> Unit,
) {
    var duration by remember { mutableIntStateOf(defaultDuration.coerceIn(2, 3)) }
    var liquid by remember { mutableStateOf("Wasser") }
    var fat by remember { mutableStateOf("MCT-Öl") }
    var startMinute by remember { mutableIntStateOf(defaultStartMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kur einrichten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dauer", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3).forEach { FilterChip(selected = duration == it, onClick = { duration = it }, label = { Text("$it Tage") }) }
                }
                Text("Trägerflüssigkeit", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Wasser", "Grüner Tee").forEach { FilterChip(selected = liquid == it, onClick = { liquid = it }, label = { Text(it) }) }
                }
                Text("Fettträger", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MCT-Öl", "Olivenöl").forEach { FilterChip(selected = fat == it, onClick = { fat = it }, label = { Text(it) }) }
                }
                Text("Geplante Startzeit heute und am Folgetag", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { startMinute = (startMinute - 15).coerceAtLeast(0) }) { Text("−15") }
                    Text("%02d:%02d".format(startMinute / 60, startMinute % 60), style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { startMinute = (startMinute + 15).coerceAtMost(1_425) }) { Text("+15") }
                }
                Text("T0 wird erst gesetzt, wenn du „Drink getrunken – jetzt“ bestätigst.")
            }
        },
        confirmButton = { Button(onClick = { onConfirm(duration, liquid, fat, startMinute) }) { Text("Kur anlegen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun DrinkDialog(
    ingredients: List<de.frank.fisetinbegleiter.data.IngredientEntity>,
    carrierLiquid: String,
    fatCarrier: String,
    onDismiss: () -> Unit,
    onDrinkNow: () -> Unit,
) {
    val checked = remember(ingredients) { mutableStateMapOf<Long, Boolean>().apply { ingredients.forEach { put(it.id, false) } } }
    val complete = ingredients.all { checked[it.id] == true }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drink-Checkliste") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Text("Gewählt: $carrierLiquid · $fatCarrier", color = MaterialTheme.colorScheme.primary) }
                items(ingredients, key = { it.id }) { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked[item.id] == true, onCheckedChange = { checked[item.id] = it })
                        Column {
                            Text("${item.name}: ${item.amount}", fontWeight = FontWeight.SemiBold)
                            Text(item.note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item { Text("Alles intensiv mixen oder shaken und sofort trinken.", fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = { Button(onClick = onDrinkNow, enabled = complete) { Text("Drink getrunken – jetzt") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
fun TimelineScreen(
    state: MainUiState,
    onMealDone: () -> Unit,
    onSpermidinDone: () -> Unit,
    onCompleteDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val day = state.currentDay
    LazyColumn(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Tagesablauf", "Alle Uhrzeiten werden ab T0 berechnet.") }
        if (day?.t0 == null) {
            item { SignalCard("Noch kein T0", "Trinke zuerst den Fisetin-Drink über den Heute-Bildschirm.", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface) }
        } else {
            val times = timeline(day.t0, state.protocol)
            item { TimelineRow("Fisetin-Drink", formatTime(times.t0), true, null) }
            item { TimelineRow("Fettige Mahlzeit", "bis ${formatTime(times.mealDeadline)}", day.mealDoneAt != null, if (day.mealDoneAt == null) onMealDone else null) }
            item { TimelineRow("Spermidin + leichte Mahlzeit", "${formatTime(times.spermidinStart)}–${formatTime(times.spermidinEnd)}", day.spermidinDoneAt != null, if (day.spermidinDoneAt == null) onSpermidinDone else null) }
            item { TimelineRow("Sperrfenster vorbei", formatTime(times.blockEnd), !state.blockActive, null) }
            if (state.blockActive) {
                item { SignalCard("Sperrfenster aktiv", "Noch ${formatCountdown(times.blockEnd, state.now)}.", WarningYellowContainer, WarningYellow) }
            }
            item {
                Button(onClick = onCompleteDay, enabled = state.canCompleteDay, modifier = Modifier.fillMaxWidth()) {
                    Text(if (day.dayNumber == state.activeCure?.cure?.plannedDurationDays) "Kur abschließen" else "Tag ${day.dayNumber} abschließen")
                }
            }
            if (!state.canCompleteDay) {
                item { Text("Der Abschluss wird freigeschaltet, sobald Mahlzeit, Spermidin-Schritt und Sperrfenster erledigt sind.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun TimelineRow(title: String, time: String, done: Boolean, action: (() -> Unit)?) {
    Surface(shape = MaterialTheme.shapes.large, color = if (done) AllowedGreenContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (done) AllowedGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(time)
            }
            action?.let { TextButton(onClick = it) { Text("Erledigt") } }
        }
    }
}

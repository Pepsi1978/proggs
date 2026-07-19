package de.frank.fisetinbegleiter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.fisetinbegleiter.data.CureStatus
import de.frank.fisetinbegleiter.data.CureWithDays
import de.frank.fisetinbegleiter.data.StackCategory
import de.frank.fisetinbegleiter.data.StackItemEntity
import de.frank.fisetinbegleiter.domain.completedThroughDay
import de.frank.fisetinbegleiter.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StackScreen(state: MainUiState, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val blocked = state.blockActive
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle(
                "Morgen-Stack",
                if (blocked) "Für heute während des aktiven Sperrfensters." else "Aktuell besteht keine Antioxidantien-Sperre.",
            )
        }
        if (blocked) {
            item {
                StatusNotice(
                    "Freigabe um ${formatTime(state.blockEnd!!)}",
                    "Bis dahin rote Substanzen weglassen. Gelbe Grenzfälle im Zweifel ebenfalls verschieben.",
                    colors.warnBg,
                    colors.warn,
                )
            }
        }

        val hardItems = state.stackItems.filter { it.category == StackCategory.HARTE_SPERRE }
        if (hardItems.isNotEmpty()) {
            item { StackSectionTitle(if (blocked) "Harte Sperre" else "Jetzt wieder erlaubt") }
            items(hardItems, key = { it.id }) { StackStatusCard(it, blocked) }
        }

        val cautionItems = state.stackItems.filter { it.category == StackCategory.GRENZFALL }
        if (cautionItems.isNotEmpty()) {
            item { StackSectionTitle("Grenzfälle", topPadding = 6.dp) }
            items(cautionItems, key = { it.id }) { StackStatusCard(it, blocked) }
        }

        val safeItems = state.stackItems.filter { it.category == StackCategory.UNPROBLEMATISCH }
        if (safeItems.isNotEmpty()) {
            item { StackSectionTitle("Unproblematisch", topPadding = 6.dp) }
            items(safeItems.filter { it.isMedication }, key = { it.id }) { StackStatusCard(it, blocked = false) }
            item { SafeItemChips(safeItems.filterNot { it.isMedication }) }
            item {
                Text(
                    "Alle unproblematischen Substanzen: wie im Morgen-Stack einnehmen.",
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.sub,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StackSectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    val colors = LocalAppColors.current
    Text(text, modifier = Modifier.padding(top = topPadding), color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StackStatusCard(item: StackItemEntity, blocked: Boolean) {
    val colors = LocalAppColors.current
    val isBlocked = item.category == StackCategory.HARTE_SPERRE && blocked
    val caution = item.category == StackCategory.GRENZFALL && blocked
    val background = when {
        isBlocked -> colors.badBg
        caution -> colors.warnBg
        else -> colors.okBg
    }
    val foreground = when {
        isBlocked -> colors.bad
        caution -> colors.warn
        else -> colors.ok
    }
    val status = when {
        item.isMedication -> "Medikament — immer einnehmen, nie wegen der Kur weglassen."
        isBlocked -> "Während des Sperrfensters weglassen."
        caution -> "Im Zweifel hinter das 4-Stunden-Fenster verschieben."
        item.category == StackCategory.HARTE_SPERRE -> "Sperrfenster vorbei – jetzt nachholbar."
        else -> "Normal einnehmen."
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(background).padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.name,
                modifier = Modifier.weight(1f),
                color = foreground,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (isBlocked) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(item.usualDose, color = colors.sub, fontSize = 12.sp)
        }
        Text(status, modifier = Modifier.padding(top = 3.dp), color = foreground, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        if (item.note.isNotBlank()) {
            Text(item.note, modifier = Modifier.padding(top = 3.dp), color = colors.sub, fontSize = 12.sp, lineHeight = 16.8.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SafeItemChips(items: List<StackItemEntity>) {
    val colors = LocalAppColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Text(
                item.name,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.chip).border(1.dp, colors.line, RoundedCornerShape(999.dp)).padding(horizontal = 13.dp, vertical = 8.dp),
                color = colors.text,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun HistoryScreen(
    state: MainUiState,
    onUpdateNote: (CureWithDays, String) -> Unit,
    onCancelCure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("Protokoll", "Alle Daten bleiben lokal auf diesem Gerät.") }
        if (state.history.isEmpty()) {
            item { Text("Noch keine Kur gespeichert.", color = colors.sub, fontSize = 13.5.sp) }
        }
        items(state.history, key = { it.cure.id }) { cure ->
            HistoryCard(cure, onUpdateNote, onCancelCure)
        }
    }
}

@Composable
private fun HistoryCard(cure: CureWithDays, onUpdateNote: (CureWithDays, String) -> Unit, onCancelCure: () -> Unit) {
    val colors = LocalAppColors.current
    var note by remember(cure.cure.id, cure.cure.note) { mutableStateOf(cure.cure.note) }
    var showCancelConfirmation by remember(cure.cure.id) { mutableStateOf(false) }
    val doneSteps = cure.days.sumOf { day ->
        listOf(day.drinkDoneAt, day.mealDoneAt, day.spermidinDoneAt, day.blockEndedAt).count { it != null }
    }
    val allSteps = cure.days.size * 4
    val completed = cure.cure.status == CureStatus.ABGESCHLOSSEN
    val completedDays = completedThroughDay(cure)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                LocalDate.ofEpochDay(cure.cure.startEpochDay).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (completed) "Abgeschlossen" else "Aktiv",
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (completed) colors.okBg else colors.warnBg).padding(horizontal = 11.dp, vertical = 5.dp),
                color = if (completed) colors.ok else colors.warn,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            )
        }
        Text(
            "${historyDayLabel(cure.cure.plannedDurationDays)} · $doneSteps von $allSteps Schritten dokumentiert",
            modifier = Modifier.padding(top = 10.dp),
            color = colors.sub,
            fontSize = 13.sp,
        )
        Text(
            "NOTIZ, VERTRÄGLICHKEIT, BEOBACHTUNGEN",
            modifier = Modifier.padding(top = 10.dp, bottom = 5.dp),
            color = colors.sub,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
        NoteEditor(note, { note = it })
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallActionButton("Notiz speichern", { onUpdateNote(cure, note) }, enabled = note != cure.cure.note)
            if (!completed) {
                DesignOutlineButton(
                    if (completedDays > 0) "Nach ${historyDayLabel(completedDays)} beenden" else "Abbrechen & zurücksetzen",
                    { showCancelConfirmation = true },
                    contentColor = colors.bad,
                    borderColor = colors.badBg,
                    verticalPadding = 10.dp,
                )
            }
        }
    }

    if (showCancelConfirmation) {
        FbPopDialog(onDismiss = { showCancelConfirmation = false }) {
            Text(
                if (completedDays > 0) "Kur nach Tag $completedDays beenden?" else "Aktiven Ablauf löschen?",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (completedDays > 0) {
                    if (completedDays == 1) {
                        "Der vollständige Tag bleibt als Kur gespeichert. Offene Folgetage, Alarme und Benachrichtigungen werden entfernt."
                    } else {
                        "Die $completedDays vollständigen Tage bleiben als Kur gespeichert. Offene Folgetage, Alarme und Benachrichtigungen werden entfernt."
                    }
                } else {
                    "Der aktive Ablauf verschwindet vollständig aus dem Verlauf. Zugehörige Schritte, Alarme und Benachrichtigungen werden ebenfalls gelöscht."
                },
                modifier = Modifier.padding(top = 10.dp),
                color = colors.sub,
                fontSize = 13.5.sp,
                lineHeight = 20.925.sp,
            )
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DesignOutlineButton("Behalten", { showCancelConfirmation = false }, Modifier.weight(1f), verticalPadding = 13.dp)
                FlatDesignButton(
                    if (completedDays > 0) "Als Kur speichern" else "Vollständig löschen",
                    {
                        onCancelCure()
                        showCancelConfirmation = false
                    },
                    Modifier.weight(1f),
                    colors.badBg,
                    colors.bad,
                )
            }
        }
    }
}

private fun historyDayLabel(days: Int): String = if (days == 1) "1 Tag" else "$days Tage"

@Composable
private fun NoteEditor(value: String, onValueChange: (String) -> Unit) {
    val colors = LocalAppColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.chip).border(1.dp, colors.line, RoundedCornerShape(14.dp)).padding(12.dp),
        minLines = 3,
        textStyle = TextStyle(color = colors.text, fontSize = 13.5.sp, lineHeight = 19.575.sp),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text("Beobachtungen festhalten …", color = colors.sub, fontSize = 13.5.sp)
                inner()
            }
        },
    )
}

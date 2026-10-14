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
import de.frank.fisetinbegleiter.domain.FASTING_RULE
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import de.frank.fisetinbegleiter.alarm.hourLabel
import de.frank.fisetinbegleiter.domain.timeline
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
    val phase = state.stackPhase
    val drinkAt = state.plannedDrinkAt
    val upcomingPrePhase = state.prePhaseStart?.takeIf { phase == StackPhase.NORMAL }
    val curePlanned = upcomingPrePhase != null
    val groupedItems = remember(state.stackItems) { state.stackItems.groupBy { it.category } }
    val safeItems = groupedItems[StackCategory.UNPROBLEMATISCH].orEmpty()
    val safeGroups = remember(safeItems) { safeItems.partition { it.isMedication } }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = LocalScreenBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle(
                "Stack",
                when (phase) {
                    StackPhase.VORSTUFE -> "Vorstufe: Was du vor dem Drink nicht mehr nehmen darfst."
                    StackPhase.SPERRFENSTER -> "Sperrfenster: Was du jetzt weglassen musst."
                    StackPhase.NACH_SPERRE -> "Sperrfenster vorbei – alles wieder erlaubt."
                    StackPhase.NORMAL -> "Was du rund um die Kur nehmen darfst."
                },
            )
        }
        item {
            when (phase) {
                StackPhase.VORSTUFE -> {
                    val releaseAt = drinkAt?.let { timeline(it, state.protocol).blockEnd }
                    StatusNotice(
                        title = if (state.drinkDue) "Vorstufe – Drink jetzt fällig" else "Vorstufe läuft – Drink ${drinkAt?.let { formatDayTime(it, state.now) }.orEmpty()}",
                        text = "Alles in Rot ab sofort nicht mehr nehmen, gelbe Grenzfälle ebenfalls weglassen" +
                            (releaseAt?.let { " – wieder erlaubt ab ca. ${formatDayTime(it, state.now)}" } ?: "") +
                            ". $FASTING_RULE Medikamente wie gewohnt.",
                        containerColor = colors.badBg,
                        contentColor = colors.bad,
                        iconVector = Icons.Outlined.WarningAmber,
                    )
                }
                StackPhase.SPERRFENSTER -> StatusNotice(
                    title = "Freigabe um ${formatTime(state.blockEnd!!)}",
                    text = "Bis dahin rote Substanzen weglassen. Gelbe Grenzfälle im Zweifel ebenfalls verschieben.",
                    containerColor = colors.warnBg,
                    contentColor = colors.warn,
                    iconVector = Icons.Outlined.Schedule,
                )
                StackPhase.NACH_SPERRE -> StatusNotice(
                    title = "Sperrfenster vorbei",
                    text = "Alles ist wieder erlaubt – verpasste Antioxidantien kannst du jetzt nachholen.",
                    containerColor = colors.okBg,
                    contentColor = colors.ok,
                    iconVector = Icons.Outlined.CheckCircle,
                )
                StackPhase.NORMAL -> if (upcomingPrePhase != null) {
                    StatusNotice(
                        title = "Vorstufe ab ${formatDayTime(upcomingPrePhase, state.now)}",
                        text = "Bis dahin nimmst du alles wie gewohnt. Ab der Vorstufe sind die roten Substanzen tabu – bis ${hourLabel(state.protocol.antioxidantBlockMinutes)} nach dem Drink.",
                        containerColor = colors.accentSoft,
                        contentColor = colors.accent,
                        iconVector = Icons.Outlined.Schedule,
                    )
                } else {
                    StatusNotice(
                        title = "Keine Einschränkung",
                        text = "Aktuell ist keine Kur geplant – alles wie gewohnt einnehmen.",
                        containerColor = colors.okBg,
                        contentColor = colors.ok,
                        iconVector = Icons.Outlined.CheckCircle,
                    )
                }
            }
        }

        val hardItems = groupedItems[StackCategory.HARTE_SPERRE].orEmpty()
        if (hardItems.isNotEmpty()) {
            item {
                StackSectionTitle(
                    when (phase) {
                        StackPhase.VORSTUFE, StackPhase.SPERRFENSTER -> "Nicht nehmen"
                        StackPhase.NACH_SPERRE -> "Wieder erlaubt"
                        StackPhase.NORMAL -> "Starke Antioxidantien"
                    },
                )
            }
            items(hardItems, key = { it.id }, contentType = { "stack-status" }) { StackStatusCard(it, phase, curePlanned) }
        }

        val cautionItems = groupedItems[StackCategory.GRENZFALL].orEmpty()
        if (cautionItems.isNotEmpty()) {
            item { StackSectionTitle("Grenzfälle", topPadding = 6.dp) }
            items(cautionItems, key = { it.id }, contentType = { "stack-status" }) { StackStatusCard(it, phase, curePlanned) }
        }

        if (safeGroups.first.isNotEmpty()) {
            item { StackSectionTitle("Medikamente", topPadding = 6.dp) }
            items(safeGroups.first, key = { it.id }, contentType = { "stack-status" }) { StackStatusCard(it, phase, curePlanned) }
        }

        if (safeGroups.second.isNotEmpty()) {
            item { StackSectionTitle("Unproblematisch", topPadding = 6.dp) }
            item {
                Text(
                    if (phase == StackPhase.VORSTUFE) {
                        "Abends wie gewohnt. Am Kurmorgen erst nach dem Drink einnehmen. $FASTING_RULE"
                    } else {
                        "Wie gewohnt einnehmen."
                    },
                    color = if (phase == StackPhase.VORSTUFE) colors.warn else colors.sub,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = if (phase == StackPhase.VORSTUFE) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            item { SafeItemChips(safeGroups.second) }
        }
    }
}

@Composable
private fun StackSectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    val colors = LocalAppColors.current
    Text(text, modifier = Modifier.padding(top = topPadding), color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StackStatusCard(item: StackItemEntity, phase: StackPhase, curePlanned: Boolean) {
    val colors = LocalAppColors.current
    val restricted = phase == StackPhase.VORSTUFE || phase == StackPhase.SPERRFENSTER
    val isBlocked = item.category == StackCategory.HARTE_SPERRE && restricted && !item.isMedication
    val caution = item.category == StackCategory.GRENZFALL && restricted && !item.isMedication
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
        item.isMedication -> "Medikament – immer einnehmen, nie wegen der Kur weglassen."
        isBlocked && phase == StackPhase.VORSTUFE -> "Nicht mehr nehmen – erst wieder nach dem Sperrfenster."
        isBlocked -> "Während des Sperrfensters weglassen."
        caution -> "Weglassen, bis das Sperrfenster vorbei ist."
        item.category == StackCategory.HARTE_SPERRE && phase == StackPhase.NACH_SPERRE -> "Sperrfenster vorbei – jetzt nachholbar."
        item.category != StackCategory.UNPROBLEMATISCH && curePlanned -> "Jetzt noch normal – ab der Vorstufe weglassen."
        else -> "Normal einnehmen."
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(background).padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Text(
                item.name,
                modifier = Modifier.weight(1f),
                color = foreground,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (isBlocked) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(
                item.usualDose,
                modifier = Modifier.widthIn(max = 150.dp).padding(top = 2.dp),
                color = colors.sub,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
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
        contentPadding = PaddingValues(top = 4.dp, bottom = LocalScreenBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("Protokoll", "Alle Daten bleiben lokal auf diesem Gerät.") }
        if (state.history.isEmpty()) {
            item { Text("Noch keine Kur gespeichert.", color = colors.sub, fontSize = 13.5.sp) }
        }
        items(state.history, key = { it.cure.id }, contentType = { "cure-history" }) { cure ->
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
                when {
                    completed -> "Abgeschlossen"
                    cure.cure.status == CureStatus.GEPLANT -> "Geplant"
                    else -> "Aktiv"
                },
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (completed) colors.okBg else colors.warnBg).padding(horizontal = 11.dp, vertical = 5.dp),
                color = if (completed) colors.ok else colors.warn,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            )
        }
        Text(
            "${dayLabel(cure.cure.plannedDurationDays)} · $doneSteps von $allSteps Schritten dokumentiert",
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
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallActionButton(
                "Notiz speichern",
                { onUpdateNote(cure, note) },
                enabled = note != cure.cure.note,
                modifier = Modifier.weight(1f),
            )
            if (!completed) {
                DesignOutlineButton(
                    if (completedDays > 0) "Nach ${dayLabel(completedDays)} beenden" else "Abbrechen",
                    { showCancelConfirmation = true },
                    modifier = Modifier.weight(1f),
                    contentColor = colors.bad,
                    borderColor = colors.badBg,
                    verticalPadding = 10.dp,
                )
            }
        }
    }

    if (showCancelConfirmation) {
        FbConfirmDialog(
            title = if (completedDays > 0) "Kur nach Tag $completedDays beenden?" else "Aktiven Ablauf löschen?",
            text = if (completedDays > 0) {
                if (completedDays == 1) {
                    "Der vollständige Tag bleibt als Kur gespeichert. Offene Folgetage, Alarme und Benachrichtigungen werden entfernt."
                } else {
                    "Die $completedDays vollständigen Tage bleiben als Kur gespeichert. Offene Folgetage, Alarme und Benachrichtigungen werden entfernt."
                }
            } else {
                "Der aktive Ablauf verschwindet vollständig aus dem Verlauf. Zugehörige Schritte, Alarme und Benachrichtigungen werden ebenfalls gelöscht."
            },
            confirmLabel = if (completedDays > 0) "Als Kur speichern" else "Vollständig löschen",
            onConfirm = onCancelCure,
            onDismiss = { showCancelConfirmation = false },
        )
    }
}


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

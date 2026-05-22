package de.frank.entropyreducer.presentation.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.presentation.components.EntryCaptureSheet
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.util.UUID

/**
 * Loop-Reiter (Frank-Wunsch 2026-05-22 abend, dritte Iteration): wirklich einfach
 * — nur die Liste der Aufgaben und unten ein Plus-FAB. Kein Header, keine
 * Untertitel, kein Editor-Sheet. Der Plus-FAB oeffnet das gemeinsame
 * EntryCaptureSheet (Aufnehmen/Schreiben) wie im Aufgaben-Reiter und legt aus
 * dem Text direkt eine wiederkehrende Aufgabe mit Default "täglich" an.
 *
 * Klick auf eine Karte: aktiv/pausiert toggeln (Switch).
 * Mülltonne pro Karte: loescht die Vorlage.
 */
@Composable
fun RecurringTemplatesScreen(
    viewModel: RecurringTemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsState()
    val cosmos = LocalCosmos.current

    var captureOpen by remember { mutableStateOf(false) }

    // Status-Bar-Inset oben (Punch-Hole-Sicherheit) + BottomBar-Hoehe unten.
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        if (templates.isEmpty()) {
            EmptyHint(
                topInset = topInset,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topInset + 16.dp,
                    bottom = 96.dp + bottomInset,
                ),
            ) {
                items(templates) { t ->
                    TemplateCard(
                        template = t,
                        onToggle = { viewModel.toggleActive(t) },
                        onDelete = { viewModel.delete(t) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { captureOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp + bottomInset),
            containerColor = CosmosColors.AccentPrimary,
            contentColor = Color.White,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Neue wiederkehrende Aufgabe")
        }
    }

    if (captureOpen) {
        EntryCaptureSheet(
            title = "Neue wiederkehrende Aufgabe",
            onDismiss = { captureOpen = false },
            onCommit = { text, source ->
                viewModel.createFromText(text = text, source = source)
                captureOpen = false
            },
        )
    }
}

@Composable
private fun EmptyHint(topInset: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 32.dp).padding(top = topInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Repeat,
            contentDescription = null,
            tint = CosmosColors.AccentSecondary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Noch keine wiederkehrenden Aufgaben",
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tippe auf das Plus um eine anzulegen.",
            style = MaterialTheme.typography.bodySmall,
            color = cosmos.textSecondary,
        )
    }
}

@Composable
private fun TemplateCard(
    template: RecurringTemplateEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = if (template.isActive) CosmosColors.AccentPrimary else cosmos.textSecondary

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Repeat,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = humanReadable(template.rrule),
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
            Switch(checked = template.isActive, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Löschen",
                    tint = CosmosColors.Critical,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Inline-Items-Helper fuer LazyColumn. */
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.items(
    list: List<T>,
    crossinline content: @Composable (T) -> Unit,
) {
    items(list.size) { index -> content(list[index]) }
}

/**
 * RRULE → deutscher Klartext. Beispiele:
 * - "FREQ=DAILY"                   → "Täglich"
 * - "FREQ=WEEKLY;BYDAY=MO,WE,FR"   → "Wöchentlich: Mo, Mi, Fr"
 * - "FREQ=MONTHLY;BYMONTHDAY=1"    → "Monatlich am 1."
 * - unbekannte Regel               → die Original-Regel
 */
internal fun humanReadable(rrule: String): String {
    val parts = rrule.split(";")
        .mapNotNull { part ->
            val kv = part.split("=", limit = 2)
            if (kv.size == 2) kv[0].uppercase() to kv[1] else null
        }
        .toMap()
    val freq = parts["FREQ"] ?: return rrule
    val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
    val byDay = parts["BYDAY"]?.split(",")
    val byMonthDay = parts["BYMONTHDAY"]
    val dayNamesShort = mapOf(
        "MO" to "Mo", "TU" to "Di", "WE" to "Mi",
        "TH" to "Do", "FR" to "Fr", "SA" to "Sa", "SU" to "So",
    )
    return when (freq) {
        "DAILY" -> if (interval == 1) "Täglich" else "Alle $interval Tage"
        "WEEKLY" -> {
            val days = byDay?.mapNotNull { dayNamesShort[it.takeLast(2)] }?.joinToString(", ")
            val base = if (interval == 1) "Wöchentlich" else "Alle $interval Wochen"
            if (days.isNullOrBlank()) base else "$base: $days"
        }
        "MONTHLY" -> {
            val base = if (interval == 1) "Monatlich" else "Alle $interval Monate"
            if (byMonthDay != null) "$base am $byMonthDay." else base
        }
        "YEARLY" -> if (interval == 1) "Jährlich" else "Alle $interval Jahre"
        else -> rrule
    }
}

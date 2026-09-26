package de.frank.newskompass.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.frank.newskompass.data.Archiv
import de.frank.newskompass.data.ArchivMonat
import de.frank.newskompass.data.ArchivTag
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Was die Nachrichtenansicht zeigt. Als Text gespeichert, damit die Wahl Drehen und Prozesstod
 * übersteht — und damit ein neuer Lauf im Hintergrund eine bewusst gewählte Ansicht nicht verlässt.
 */
internal sealed interface Ansicht {
    val text: String

    /** Folgt immer der neuesten Ausgabe. */
    data object Aktuell : Ansicht {
        override val text = "aktuell"
    }

    /** Ein Kalendertag; [ausgabeId] null heißt: die neueste Ausgabe dieses Tages. */
    data class Tag(val datum: LocalDate, val ausgabeId: String?) : Ansicht {
        override val text get() = "tag|$datum|${ausgabeId.orEmpty()}"
    }

    data class Monat(val monat: YearMonth) : Ansicht {
        override val text get() = "monat|$monat"
    }

    companion object {
        fun aus(text: String): Ansicht = runCatching {
            val teile = text.split('|')
            when (teile[0]) {
                "tag" -> Tag(LocalDate.parse(teile[1]), teile.getOrNull(2)?.ifBlank { null })
                "monat" -> Monat(YearMonth.parse(teile[1]))
                else -> Aktuell
            }
        }.getOrDefault(Aktuell)
    }
}

/** Die Seitenleiste: Aktuell, die letzten Tage einzeln, darunter die Monatsrückblicke. */
@Composable
internal fun ArchivSeitenleiste(
    auswahl: Ansicht,
    heute: LocalDate,
    tage: List<ArchivTag>,
    monate: List<ArchivMonat>,
    waehle: (Ansicht) -> Unit,
) {
    ModalDrawerSheet(Modifier.widthIn(max = 340.dp)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "kopf") {
                Column(Modifier.padding(start = 28.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)) {
                    Text("Archiv", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Aufgezeichnet seit dem 25. September 2026 — nur gespeicherte Ausgaben, nichts nachträglich recherchiert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "aktuell") {
                NavigationDrawerItem(
                    label = { Text("Aktuell") },
                    icon = { Icon(Icons.Rounded.Newspaper, null) },
                    selected = auswahl is Ansicht.Aktuell,
                    onClick = { waehle(Ansicht.Aktuell) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            if (tage.isNotEmpty()) {
                item(key = "tage-titel") { Zwischentitel("Letzte ${Archiv.TAGE} Tage") }
                items(tage, key = { "tag-${it.datum}" }) { tag ->
                    NavigationDrawerItem(
                        label = { ZweiZeilen(tagName(tag.datum, heute), tagInfo(tag)) },
                        icon = { Icon(Icons.Rounded.Event, null) },
                        selected = auswahl is Ansicht.Tag && auswahl.datum == tag.datum,
                        // Fest auf die neueste Ausgabe dieses Tages — ein späterer Lauf wechselt die Ansicht nicht.
                        onClick = { waehle(Ansicht.Tag(tag.datum, tag.ausgaben.firstOrNull()?.id)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
            if (monate.isNotEmpty()) {
                item(key = "monate-titel") { Zwischentitel("Monatsrückblicke") }
                items(monate, key = { "monat-${it.monat}" }) { m ->
                    NavigationDrawerItem(
                        label = { ZweiZeilen(Archiv.monatsName(m.monat), "${Archiv.zeitraum(m)} · Top ${Archiv.TOP_JE_THEMA} je Thema") },
                        icon = { Icon(Icons.Rounded.AutoStories, null) },
                        selected = auswahl is Ansicht.Monat && auswahl.monat == m.monat,
                        onClick = { waehle(Ansicht.Monat(m.monat)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun Zwischentitel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, end = 24.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun ZweiZeilen(oben: String, unten: String) {
    Column {
        Text(oben, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            unten,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Das heutige Datum als Zustand: wechselt um Mitternacht, auch wenn die App offen bleibt, und
 * wird beim Zurückkehren in die App frisch gelesen — ganz ohne Hintergrundauftrag.
 */
@Composable
internal fun rememberHeute(): LocalDate {
    var heute by remember { mutableStateOf(LocalDate.now()) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lebenszyklus) {
        lebenszyklus.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                heute = LocalDate.now()
                val bisMitternacht = Duration.between(LocalDateTime.now(), heute.plusDays(1).atStartOfDay()).toMillis()
                delay(bisMitternacht.coerceAtLeast(0L) + 1_000L)
            }
        }
    }
    return heute
}

/** „Heute“, „Gestern“ oder etwa „Fr, 25. Sep.“. */
internal fun tagName(datum: LocalDate, heute: LocalDate): String = when (datum) {
    heute -> "Heute"
    heute.minusDays(1) -> "Gestern"
    else -> datum.format(DateTimeFormatter.ofPattern("EE, d. MMM", Locale.GERMANY))
}

/** Etwa „14 Meldungen · Morgen, Abend · 1 Frage“. */
private fun tagInfo(tag: ArchivTag): String {
    val teile = tag.ausgaben.filter { it.regulaer }.sortedBy { it.erstelltUm }
        .map { it.slot.removeSuffix("ausgabe").ifBlank { "Ausgabe" } }.distinct()
    return buildList {
        add(if (tag.meldungen == 1) "1 Meldung" else "${tag.meldungen} Meldungen")
        if (teile.isNotEmpty()) add(teile.joinToString(", "))
        if (tag.fragen > 0) add(if (tag.fragen == 1) "1 Frage" else "${tag.fragen} Fragen")
        if (tag.beschaedigt > 0) add("${tag.beschaedigt} beschädigt")
    }.joinToString(" · ")
}

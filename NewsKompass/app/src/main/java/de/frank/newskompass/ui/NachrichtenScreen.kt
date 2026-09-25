package de.frank.newskompass.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.DesignModus
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.news.Zeitplan
import de.frank.newskompass.tts.VorleseStufe
import de.frank.newskompass.tts.VorleseZustand
import de.frank.newskompass.ui.theme.LocalIstDunkel
import de.frank.newskompass.ui.theme.blockFarbe
import de.frank.newskompass.ui.theme.blockVerlauf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NachrichtenScreen(app: NewsApplication, oeffneEinstellungen: () -> Unit) {
    val kontext = LocalContext.current
    val ausgaben by app.speicher.ausgaben.collectAsStateWithLifecycle()
    val stand by app.einstellungen.stand.collectAsStateWithLifecycle()
    val vorlesen by app.vorleser.zustand.collectAsStateWithLifecycle()
    val arbeit by remember { WorkManager.getInstance(kontext).getWorkInfosForUniqueWorkFlow(Zeitplan.LAUF) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val lauf = arbeit.firstOrNull()

    var gewaehlteId by rememberSaveable { mutableStateOf<String?>(null) }
    val ausgabe = ausgaben.firstOrNull { it.id == gewaehlteId } ?: ausgaben.firstOrNull()
    // Nach einem neuen Lauf springt die Ansicht auf die neueste Ausgabe.
    LaunchedEffect(ausgaben.firstOrNull()?.id) { gewaehlteId = null }

    // Reihenfolge der Blöcke folgt immer der aktuellen Themenliste der Einstellungen.
    val bloecke = remember(ausgabe, stand.themen) {
        val rang = stand.themen.mapIndexed { i, t -> t.id to i }.toMap()
        ausgabe?.bloecke.orEmpty().sortedBy { rang[it.themaId] ?: Int.MAX_VALUE }
    }
    val dunkel = LocalIstDunkel.current
    val liste = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = liste,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "kopf") {
                Kopf(
                    ausgabe = ausgabe,
                    dunkel = dunkel,
                    laeuft = lauf?.state == WorkInfo.State.RUNNING,
                    aktualisiere = { Zeitplan.starteLauf(kontext, manuell = true) },
                    wechsleDesign = { app.einstellungen.setzeDesign(if (dunkel) DesignModus.HELL else DesignModus.DUNKEL) },
                    oeffneEinstellungen = oeffneEinstellungen,
                )
            }
            if (ausgaben.size > 1) {
                item(key = "ausgaben") {
                    AusgabenWahl(ausgaben, ausgabe?.id) { gewaehlteId = it }
                }
            }
            item(key = "status") {
                Spalte { LaufStatus(lauf, app.codex.istVerbunden, oeffneEinstellungen) }
            }
            if (ausgabe == null) {
                item(key = "leer") {
                    Spalte {
                        LeerZustand(
                            angemeldet = app.codex.istVerbunden,
                            laeuft = lauf?.state == WorkInfo.State.RUNNING,
                            laden = { Zeitplan.starteLauf(kontext, manuell = true) },
                            oeffneEinstellungen = oeffneEinstellungen,
                        )
                    }
                }
            }
            bloecke.forEachIndexed { index, block ->
                item(key = "b-${ausgabe?.id}-${block.themaId}") {
                    Spalte {
                        BlockKopf(
                            index = index,
                            block = block,
                            zustand = vorlesen,
                            vorlesen = { app.vorleser.schalteUm("block-${block.themaId}", blockText(block)) },
                        )
                    }
                }
                block.fehler?.let { fehler ->
                    item(key = "f-${ausgabe?.id}-${block.themaId}") { Spalte { Hinweis(fehler) } }
                }
                items(block.meldungen, key = { it.id }) { meldung ->
                    Spalte {
                        MeldungsKarte(
                            meldung = meldung,
                            blockIndex = index,
                            bild = app.speicher.bildDatei(meldung.bildDatei),
                            zustand = vorlesen,
                            vorlesen = { app.vorleser.schalteUm(meldung.id, meldung.vorleseText) },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = vorlesen.fehler.isNotBlank(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 8.dp,
            ) {
                Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        vorlesen.fehler,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false).widthIn(max = 520.dp),
                    )
                    TextButton(onClick = app.vorleser::loescheFehler) { Text("OK") }
                }
            }
        }
    }
}

/** Auf dem aufgeklappten Fold bleibt der Text in angenehmer Zeilenlänge. */
@Composable
private fun Spalte(inhalt: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 16.dp)) { inhalt() }
    }
}

private fun blockText(block: Block): String =
    (listOf("${block.titel}.") + block.meldungen.map { it.vorleseText }).joinToString("\n\n")

@Composable
private fun Kopf(
    ausgabe: Ausgabe?,
    dunkel: Boolean,
    laeuft: Boolean,
    aktualisiere: () -> Unit,
    wechsleDesign: () -> Unit,
    oeffneEinstellungen: () -> Unit,
) {
    val stunde = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val gruss = when (stunde) {
        in 4..10 -> "Guten Morgen"
        in 11..17 -> "Guten Tag"
        else -> "Guten Abend"
    }
    val verlauf = Brush.linearGradient(
        if (dunkel) listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF831843))
        else listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777)),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(verlauf),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 22.dp, end = 10.dp, top = 8.dp, bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Newspaper, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "NEWS KOMPASS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = aktualisiere, enabled = !laeuft) {
                    if (laeuft) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, "Jetzt aktualisieren", tint = Color.White)
                    }
                }
                IconButton(onClick = wechsleDesign) {
                    Icon(if (dunkel) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "Hell oder dunkel", tint = Color.White)
                }
                IconButton(onClick = oeffneEinstellungen) {
                    Icon(Icons.Rounded.Settings, "Einstellungen", tint = Color.White)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(gruss, style = MaterialTheme.typography.displaySmall, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.GERMANY).format(Date()),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            if (ausgabe != null) {
                Spacer(Modifier.height(14.dp))
                val zeit = SimpleDateFormat("EEEE, HH:mm", Locale.GERMANY).format(Date(ausgabe.erstelltUm))
                val anzahl = ausgabe.bloecke.sumOf { it.meldungen.size }
                Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
                    Text(
                        "${ausgabe.slot} · $zeit Uhr · $anzahl Meldungen",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AusgabenWahl(ausgaben: List<Ausgabe>, gewaehlt: String?, waehle: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.widthIn(max = 792.dp).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ausgaben, key = { it.id }) { a ->
            FilterChip(
                selected = a.id == gewaehlt,
                onClick = { waehle(a.id) },
                label = { Text(ausgabeName(a)) },
                shape = RoundedCornerShape(50),
            )
        }
    }
}

private fun ausgabeName(a: Ausgabe): String {
    val tag = Calendar.getInstance().apply { timeInMillis = a.erstelltUm }
    val heute = Calendar.getInstance()
    val gestern = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun gleich(x: Calendar, y: Calendar) = x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
    val wann = when {
        gleich(tag, heute) -> "Heute"
        gleich(tag, gestern) -> "Gestern"
        else -> SimpleDateFormat("EE d.M.", Locale.GERMANY).format(Date(a.erstelltUm))
    }
    val teil = a.slot.removeSuffix("ausgabe").ifBlank { "Ausgabe" }
    return "$wann · $teil · " + SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(a.erstelltUm))
}

@Composable
private fun LaufStatus(lauf: WorkInfo?, angemeldet: Boolean, oeffneEinstellungen: () -> Unit) {
    when {
        !angemeldet -> Hinweis("Melde dich in den Einstellungen bei Codex an, damit die Nachrichten recherchiert werden können.", "Zur Anmeldung", oeffneEinstellungen)
        lauf?.state == WorkInfo.State.RUNNING -> {
            val text = lauf.progress.getString("text") ?: "Die Nachrichten werden zusammengestellt …"
            val anteil = lauf.progress.getFloat("anteil", 0f)
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { anteil.coerceIn(0.03f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
        lauf?.state == WorkInfo.State.ENQUEUED && lauf.runAttemptCount > 0 ->
            Hinweis("Der letzte Versuch hat nicht geklappt. Es wird gleich automatisch erneut versucht.")
        lauf?.state == WorkInfo.State.FAILED -> {
            val fehler = lauf.outputData.getString("fehler") ?: "Der letzte Lauf ist gescheitert."
            Hinweis(fehler)
        }
    }
}

@Composable
private fun Hinweis(text: String, knopf: String? = null, aktion: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (knopf != null) TextButton(onClick = aktion) { Text(knopf) }
        }
    }
}

@Composable
private fun LeerZustand(angemeldet: Boolean, laeuft: Boolean, laden: () -> Unit, oeffneEinstellungen: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(blockVerlauf(0)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Noch keine Ausgabe", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Jeden Tag um 5 und um 17 Uhr stellt News Kompass die Neuigkeiten zu deinen Themen zusammen — mit Bildern und zum Vorlesen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(22.dp))
        if (angemeldet) {
            Button(onClick = laden, enabled = !laeuft, shape = RoundedCornerShape(50)) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (laeuft) "Wird geladen …" else "Jetzt Nachrichten laden")
            }
        } else {
            Button(onClick = oeffneEinstellungen, shape = RoundedCornerShape(50)) { Text("Bei Codex anmelden") }
        }
    }
}

@Composable
private fun BlockKopf(index: Int, block: Block, zustand: VorleseZustand, vorlesen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(blockVerlauf(index)),
            contentAlignment = Alignment.Center,
        ) {
            Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(block.titel, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${block.meldungen.size} Meldungen",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (block.meldungen.isNotEmpty()) {
            LautsprecherKnopf(zustand, "block-${block.themaId}", blockFarbe(index), vorlesen, rahmen = true)
        }
    }
}

@Composable
private fun MeldungsKarte(
    meldung: Meldung,
    blockIndex: Int,
    bild: java.io.File?,
    zustand: VorleseZustand,
    vorlesen: () -> Unit,
) {
    val kontext = LocalContext.current
    var offen by rememberSaveable(meldung.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (bild != null) {
                AsyncImage(
                    model = bild,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(blockVerlauf(blockIndex)),
                )
            } else {
                Box(Modifier.fillMaxSize().background(blockVerlauf(blockIndex)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Newspaper, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(72.dp))
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)),
                ),
            )
            if (meldung.bildIstKi) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("KI-Illustration", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                LautsprecherKnopf(zustand, meldung.id, blockFarbe(blockIndex), vorlesen, rahmen = false)
            }
            Text(
                meldung.titel,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            )
        }
        Column(Modifier.clickable { offen = !offen }.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
            if (meldung.istUpdate || meldung.wann.isNotBlank()) {
                Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (meldung.istUpdate) {
                        Surface(color = blockFarbe(blockIndex), shape = RoundedCornerShape(50)) {
                            Text(
                                "UPDATE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (meldung.wann.isNotBlank()) {
                        Text(
                            meldung.wann.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val sichtbar = if (offen) meldung.absaetze else meldung.absaetze.take(1)
            sichtbar.forEachIndexed { i, absatz ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                Text(
                    absatz,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (offen) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (offen && meldung.quellen.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(meldung.quellen.distinct().take(6)) { adresse ->
                        AssistChip(
                            onClick = {
                                runCatching { kontext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse))) }
                            },
                            label = { Text(domain(adresse), maxLines = 1) },
                            leadingIcon = { Icon(Icons.Rounded.Link, null, Modifier.size(AssistChipDefaults.IconSize)) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (offen) "Weniger" else "Weiterlesen",
                    style = MaterialTheme.typography.labelLarge,
                    color = blockFarbe(blockIndex),
                )
                Icon(if (offen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = blockFarbe(blockIndex))
            }
        }
    }
}

private fun domain(adresse: String): String =
    runCatching { Uri.parse(adresse).host.orEmpty().removePrefix("www.") }.getOrDefault(adresse).ifBlank { adresse }

@Composable
private fun LautsprecherKnopf(zustand: VorleseZustand, quelle: String, farbe: Color, aktion: () -> Unit, rahmen: Boolean) {
    val meins = zustand.quelleId == quelle
    val stufe = if (meins) zustand.stufe else VorleseStufe.AUS
    val puls = rememberInfiniteTransition(label = "puls")
    val skala by puls.animateFloat(1f, 1.12f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "skala")
    val hintergrund = when {
        stufe != VorleseStufe.AUS -> farbe
        rahmen -> farbe.copy(alpha = 0.14f)
        else -> Color.Black.copy(alpha = 0.42f)
    }
    Surface(
        onClick = aktion,
        shape = CircleShape,
        color = hintergrund,
        modifier = Modifier.size(46.dp).scale(if (stufe == VorleseStufe.SPRICHT) skala else 1f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (stufe) {
                VorleseStufe.LAEDT -> CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                VorleseStufe.SPRICHT -> Icon(Icons.Rounded.GraphicEq, "Vorlesen anhalten", tint = Color.White)
                VorleseStufe.AUS -> Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    "Vorlesen",
                    tint = if (rahmen) farbe else Color.White,
                )
            }
        }
    }
    if (meins && stufe == VorleseStufe.SPRICHT && zustand.absatzAnzahl > 1 && rahmen) {
        Text(
            " ${zustand.absatzNummer}/${zustand.absatzAnzahl}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = farbe,
        )
    }
}

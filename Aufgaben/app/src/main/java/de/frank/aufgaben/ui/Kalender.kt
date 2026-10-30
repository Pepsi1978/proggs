package de.frank.aufgaben.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Eine Woche zurück, heute und 20 Tage voraus: genau vier Wochen. */
private const val TAGE_ZURUECK = 7
private const val TAGE_GESAMT = 28
private val BREITE_MAX = 680.dp

/**
 * Vier-Wochen-Kalender mit allen gespeicherten Aufgaben, unabhängig vom Bereich, aus dem man kommt.
 * Jeder Tag zeigt die Zahl der für ihn geplanten Aufgaben (offene und erledigte); ein Tipp öffnet die Tagesübersicht.
 * Überfällige Aufgaben bleiben an ihrem gespeicherten Datum stehen.
 */
@Composable
fun KalenderBildschirm(vm: AppViewModel) {
    val alle by vm.aufgaben.collectAsState()
    val heute = vm.heute
    val start = heute - TAGE_ZURUECK
    val ende = start + TAGE_GESAMT - 1
    val proTag = remember(alle, heute) { (start..ende).associateWith { t -> alle.mapNotNull { it.amTag(t, heute) } } }
    // Nach Mitternacht kann der gewählte Tag aus dem Fenster fallen: dann zurück zur Übersicht.
    val gewaehlt = vm.kalenderTag?.takeIf { it in start..ende }
    AnimatedContent(
        gewaehlt,
        transitionSpec = {
            (fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.85f, stiffness = 420f), initialScale = 0.94f))
                .togetherWith(fadeOut(tween(140)) + scaleOut(targetScale = 0.97f))
        },
        label = "kalender",
    ) { tag ->
        if (tag == null) KalenderUebersicht(vm, heute, start, proTag)
        else TagesUebersicht(vm, tag, heute, start, ende, proTag[tag].orEmpty())
    }
}

@Composable
private fun KalenderUebersicht(vm: AppViewModel, heute: Long, start: Long, proTag: Map<Long, List<Aufgabe>>) {
    val f = LocalFarben.current
    val kurz = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)
    val vonText = LocalDate.ofEpochDay(start).format(kurz)
    val bisText = LocalDate.ofEpochDay(start + TAGE_GESAMT - 1).format(kurz)
    // Eine tägliche Aufgabe steht an vielen Tagen, zählt hier aber nur einmal.
    val offenVoraus = (heute until start + TAGE_GESAMT).flatMap { t -> proTag[t].orEmpty().filter { !it.erledigt } }.distinctBy { it.id }.size
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Kopfzeile(
            zurueck = { vm.zurueck() }, zurueckText = "Zurück zu den Aufgaben",
            oben = "Kalender", titel = "$vonText – $bisText",
        )
        Text(
            "Eine Woche zurück, heute und die nächsten drei Wochen",
            color = f.textLeise, fontSize = 13.sp,
            modifier = Modifier.widthIn(max = BREITE_MAX).fillMaxWidth().padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.widthIn(max = BREITE_MAX).fillMaxWidth().padding(horizontal = 12.dp)) {
            val luecke = if (maxWidth < 380.dp) 4.dp else 7.dp
            val zelle = (maxWidth - 24.dp - luecke * 6) / 7
            val hoch = (zelle * 1.12f).coerceIn(62.dp, 92.dp)
            Column(
                Modifier.fillMaxWidth().glas(f, erhoeht = 1.3f, fuellung = f.flaeche.copy(alpha = f.flaeche.alpha * 0.85f), toenung = f.primaer)
                    .padding(12.dp),
            ) {
                // Wochentage: Jede Zeile beginnt am Wochentag von heute, deshalb steht heute links in der zweiten Zeile.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(luecke)) {
                    for (i in 0 until 7) {
                        val d = LocalDate.ofEpochDay(start + i)
                        val wochenende = d.dayOfWeek.value >= 6
                        Text(
                            d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).trimEnd('.'),
                            color = if (wochenende) f.tertiaer else f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f),
                        )
                    }
                }
                val zeilen = listOf("Letzte Woche", "Ab heute", "Nächste Woche", "In zwei Wochen")
                for (z in 0 until 4) {
                    Text(
                        zeilen[z], color = if (z == 1) f.primaer else f.textSchwach, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 2.dp, top = if (z == 0) 8.dp else 12.dp, bottom = 5.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(luecke)) {
                        for (s in 0 until 7) {
                            val tag = start + z * 7 + s
                            TagZelle(
                                tag, heute, proTag[tag].orEmpty(), erste = z == 0 && s == 0, hoch = hoch,
                                modifier = Modifier.weight(1f),
                            ) { vm.kalenderTag = tag }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.widthIn(max = BREITE_MAX).fillMaxWidth().padding(horizontal = 12.dp)
                .glas(f, radius = f.radius * 0.7f, erhoeht = 0.6f, fuellung = f.flaeche.copy(alpha = f.flaeche.alpha * 0.7f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (offenVoraus == 0) "Ab heute ist nichts Offenes geplant."
                else "Ab heute ${if (offenVoraus == 1) "ist 1 Aufgabe" else "sind $offenVoraus Aufgaben"} offen geplant.",
                color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            Legende(f.primaer, "Zahl = alle für den Tag geplanten Aufgaben")
            Legende(f.erfolg, "Grün = alle erledigt")
            Legende(f.gefahr, "Rot = vergangener Tag mit offenen Aufgaben")
            Text("Tipp auf einen Tag zeigt seine Aufgaben.", color = f.textLeise, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun Legende(farbe: Color, text: String) {
    val f = LocalFarben.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).glas(f, 99.dp, 0f, farbe, rand = false))
        Spacer(Modifier.width(10.dp))
        Text(text, color = f.textLeise, fontSize = 12.sp)
    }
}

@Composable
private fun TagZelle(tag: Long, heute: Long, liste: List<Aufgabe>, erste: Boolean, hoch: Dp, modifier: Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    val d = LocalDate.ofEpochDay(tag)
    val istHeute = tag == heute
    val vergangen = tag < heute
    val gesamt = liste.size
    val offen = liste.count { !it.erledigt }
    val wochenende = d.dayOfWeek.value >= 6
    val beschreibung = Tage.langesDatum(tag) + when (gesamt) {
        0 -> ", keine Aufgaben"
        1 -> ", 1 Aufgabe" + if (offen == 0) ", erledigt" else ""
        else -> ", $gesamt Aufgaben" + if (offen == 0) ", alle erledigt" else if (offen < gesamt) ", $offen offen" else ""
    } + if (istHeute) ", heute" else ""
    val grund = if (istHeute) Modifier.knopf3d(f.primaer, f.sekundaer, 16.dp, f.dunkel)
    else Modifier.glas(
        f, 14.dp, if (vergangen) 0.2f else 0.6f,
        fuellung = if (vergangen) f.flaeche.copy(alpha = f.flaeche.alpha * 0.45f) else f.flaeche,
        toenung = if (gesamt > 0 && !vergangen) f.primaer else null,
    )
    Box(
        modifier.height(hoch).then(grund).antippen(aktion = aktion).semantics { contentDescription = beschreibung },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Oben: „Heute“ bzw. der Monat am ersten Tag des Fensters und an jedem Monatsersten.
            val oben = when {
                istHeute -> "Heute"
                erste || d.dayOfMonth == 1 -> d.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN).trimEnd('.')
                else -> ""
            }
            Text(
                oben, color = if (istHeute) Color.White else f.primaer, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Clip, lineHeight = 12.sp,
            )
            Text(
                "${d.dayOfMonth}",
                color = when {
                    istHeute -> Color.White
                    vergangen -> f.textLeise
                    wochenende -> f.tertiaer
                    else -> f.text
                },
                fontSize = if (istHeute) 22.sp else 20.sp, fontWeight = if (istHeute) FontWeight.ExtraBold else FontWeight.Bold, lineHeight = 24.sp,
            )
            Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                if (gesamt > 0) {
                    val farbe = when {
                        offen == 0 -> f.erfolg
                        vergangen -> f.gefahr
                        else -> f.primaer
                    }
                    val hinter = if (istHeute) Color.White else farbe
                    val schrift = if (istHeute) (if (offen == 0) f.erfolg else f.primaer) else Color.White
                    Box(
                        Modifier.widthIn(min = 22.dp).height(19.dp).glas(f, 99.dp, 0f, hinter, rand = false).padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$gesamt", color = schrift, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, lineHeight = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagesUebersicht(vm: AppViewModel, tag: Long, heute: Long, start: Long, ende: Long, liste: List<Aufgabe>) {
    val f = LocalFarben.current
    val offen = liste.filter { !it.erledigt }
        .sortedWith(compareBy<Aufgabe>({ it.minuten == null }, { it.minuten ?: 0 }, { it.prio.rang }, { -it.erstellt }))
    val erledigt = liste.filter { it.erledigt }.sortedWith(compareBy<Aufgabe>({ it.minuten ?: Int.MAX_VALUE }, { -(it.erledigtAm ?: 0) }))
    val relativ = when (tag - heute) {
        0L -> "Heute"
        1L -> "Morgen"
        2L -> "Übermorgen"
        -1L -> "Gestern"
        else -> if (tag < heute) "Vor ${heute - tag} Tagen" else "In ${tag - heute} Tagen"
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Kopfzeile(
            zurueck = { vm.zurueck() }, zurueckText = "Zurück zum Kalender",
            oben = relativ, titel = Tage.langesDatum(tag), obenFarbe = if (tag == heute) f.primaer else f.textLeise,
        ) {
            BlaetterKnopf(Icons.Rounded.ChevronLeft, "Vorheriger Tag", tag > start) { vm.kalenderTag = tag - 1 }
            BlaetterKnopf(Icons.Rounded.ChevronRight, "Nächster Tag", tag < ende) { vm.kalenderTag = tag + 1 }
        }
        Row(
            Modifier.widthIn(max = BREITE_MAX).fillMaxWidth().padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when {
                    liste.isEmpty() -> "Keine Aufgaben geplant"
                    erledigt.isEmpty() -> if (liste.size == 1) "1 Aufgabe geplant" else "${liste.size} Aufgaben geplant"
                    else -> "${liste.size} geplant · ${erledigt.size} erledigt · ${offen.size} offen"
                },
                color = f.textLeise, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(44.dp).antippen { vm.kalenderTag = null }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CalendarMonth, "Vier-Wochen-Übersicht", tint = f.primaer)
            }
            VorlesenKnopf(vm, "tag_$tag") { vm.tagVorlesen(tag, liste) }
        }
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier.widthIn(max = BREITE_MAX).fillMaxWidth().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (liste.isEmpty()) LeerHinweis(Icons.Rounded.EventAvailable, "Für diesen Tag ist noch nichts geplant. Leg unten direkt eine Aufgabe für ihn an.")
            offen.forEach { a -> TagesKarte(vm, a, heute) }
            if (erledigt.isNotEmpty()) {
                Row(Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TaskAlt, null, tint = f.erfolg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Erledigt", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                erledigt.forEach { a -> TagesKarte(vm, a, heute) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().height(54.dp).knopf3d(f.primaer, f.sekundaer, 18.dp, f.dunkel)
                    .antippen { vm.neueAufgabe(mitMikro = true, tag = tag, zurueckZu = Bildschirm.Kalender) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Aufgabe für diesen Tag", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TagesKarte(vm: AppViewModel, a: Aufgabe, heute: Long) {
    key(a.id) {
        Box(Modifier.einblenden()) {
            AufgabeKarte(a, heute, null, onTipp = { vm.oeffne(a.id, zurueckZu = Bildschirm.Kalender) }, onErledigt = { vm.erledigen(a, !a.erledigt) })
        }
    }
}

/** Kopfzeile beider Kalenderansichten: Zurück, Überschrift und rechts optionale Knöpfe. */
@Composable
private fun Kopfzeile(
    zurueck: () -> Unit,
    zurueckText: String,
    oben: String,
    titel: String,
    obenFarbe: Color = LocalFarben.current.primaer,
    rechts: @Composable () -> Unit = {},
) {
    val f = LocalFarben.current
    Row(
        Modifier.widthIn(max = BREITE_MAX).fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RundKnopf(Icons.Rounded.ArrowBack, zurueckText, zurueck)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(oben, color = obenFarbe, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(titel, color = f.text, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, lineHeight = 25.sp)
        }
        rechts()
    }
}

@Composable
private fun BlaetterKnopf(icon: ImageVector, beschreibung: String, aktiv: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        Modifier.padding(start = 4.dp).size(44.dp).graphicsLayer { alpha = if (aktiv) 1f else 0.3f }
            .glas(f, 99.dp, 0.8f).then(if (aktiv) Modifier.antippen(aktion = aktion) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, beschreibung, tint = f.text, modifier = Modifier.size(26.dp))
    }
}

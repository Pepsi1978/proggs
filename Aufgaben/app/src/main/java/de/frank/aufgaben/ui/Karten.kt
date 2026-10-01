package de.frank.aufgaben.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.data.Wiederholung
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d

/** Einblenden beim ersten Erscheinen: kippt leicht aus der Tiefe nach vorn. */
fun Modifier.einblenden(verzoegerung: Int = 0): Modifier = composed {
    val bewegung = LocalBewegung.current
    val a = remember { Animatable(if (bewegung) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (bewegung) { kotlinx.coroutines.delay(verzoegerung.toLong()); a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 180f)) }
    }
    graphicsLayer {
        val v = a.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 60f
        rotationX = (1f - v) * 18f
        cameraDistance = 16f * density
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

@Composable
fun Checkkreis(erledigt: Boolean, farbe: Color, groesse: Int = 26, aktion: () -> Unit) {
    val f = LocalFarben.current
    val haptik = LocalHapticFeedback.current
    val fort by animateFloatAsState(if (erledigt) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "haken")
    Box(
        Modifier.size((groesse + 14).dp).antippen(haptik = false) {
            haptik.performHapticFeedback(HapticFeedbackType.LongPress)
            aktion()
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(groesse.dp).graphicsLayer { scaleX = 1f + 0.15f * fort * (1f - fort) * 4f; scaleY = scaleX }) {
            val r = size.minDimension / 2
            drawCircle(farbe.copy(alpha = 0.12f + 0.88f * fort), r)
            drawCircle(farbe, r - 1.5.dp.toPx(), style = Stroke(2.dp.toPx()))
            if (fort > 0f) {
                val p = Path().apply {
                    moveTo(r * 0.55f, r * 1.02f)
                    lineTo(r * 0.88f, r * 1.35f)
                    lineTo(r * (0.88f + 0.62f * fort), r * (1.35f - 0.72f * fort))
                }
                drawPath(p, if (f.dunkel) f.aufPrimaer else Color.White, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun Merkmal(icon: ImageVector?, text: String, farbe: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (icon != null) Icon(icon, null, tint = farbe, modifier = Modifier.size(13.dp))
        Text(text, color = farbe, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Eine Aufgabe als Glaskarte. */
@Composable
fun AufgabeKarte(
    a: Aufgabe,
    heute: Long,
    zustand: ZiehZustand?,
    zeigeDatum: Boolean = false,
    schwebend: Boolean = false,
    hervorheben: Boolean = false,
    onTipp: () -> Unit,
    onErledigt: () -> Unit,
) {
    val f = LocalFarben.current
    val prio = f.prio(a.prio)
    val gezogen = zustand?.aufgabe?.id == a.id && !schwebend
    val glow by animateFloatAsState(if (hervorheben) 1f else 0f, tween(600), label = "glow")
    var basis = Modifier
        .fillMaxWidth()
        .graphicsLayer { alpha = if (gezogen) 0.25f else if (a.erledigt) 0.62f else 1f; compositingStrategy = CompositingStrategy.ModulateAlpha }
        .glas(f, radius = f.radius * 0.72f, erhoeht = if (schwebend) 2.6f else 0.9f, fuellung = if (schwebend) f.flaecheStark else f.flaeche, toenung = if (glow > 0f) f.primaer else prio)
    if (zustand != null && !schwebend) basis = basis.ziehbar(a, zustand)
    Row(
        basis.antippen(haptik = false) { if (zustand == null || zustand.tippErlaubt()) onTipp() }
            .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkkreis(a.erledigt, prio, aktion = onErledigt)
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(
                a.titel,
                color = f.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (a.erledigt) TextDecoration.LineThrough else null,
            )
            val schritte = a.schritte
            val ueberfaellig = !a.erledigt && a.tag != null && a.tag < heute
            val zeigtMerkmale = a.minuten != null || a.erinnerung || a.wdh != Wiederholung.KEINE || schritte.isNotEmpty() || ueberfaellig || zeigeDatum
            if (a.text.isNotBlank() && a.text != a.titel) {
                Text(a.text, color = f.textLeise, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            if (zeigtMerkmale) Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ueberfaellig) Merkmal(null, "überfällig · ${Tage.datum(a.tag!!)}", f.gefahr)
                else if (zeigeDatum && a.tag != null) Merkmal(null, Tage.datum(a.tag), f.primaer)
                a.minuten?.let { Merkmal(Icons.Rounded.Schedule, Tage.zeit(it), f.primaer) }
                if (a.erinnerung && a.minuten != null) Merkmal(Icons.Rounded.NotificationsActive, if (a.vorlauf > 0) "−${a.vorlauf}′" else "", f.sekundaer)
                if (a.wdh != Wiederholung.KEINE) Merkmal(Icons.Rounded.Repeat, a.wdh.anzeige, f.tertiaer)
                if (schritte.isNotEmpty()) Merkmal(Icons.Rounded.Checklist, "${schritte.count { it.erledigt }}/${schritte.size}", f.textLeise)
            }
        }
        if (a.prio != Prioritaet.SPAETER && a.tag != null) {
            Box(Modifier.padding(start = 6.dp).size(width = 6.dp, height = 26.dp).knopf3d(prio, prio.copy(alpha = 0.7f), 3.dp, f.dunkel))
        }
        if (zustand != null && !schwebend) Icon(Icons.Rounded.DragIndicator, "Lang drücken und ziehen", tint = f.textSchwach, modifier = Modifier.padding(start = 6.dp).size(20.dp))
    }
}

/** Ein Bereich (Heute, Morgen, Hoch …) als große Glasfläche mit Kopf. */
@Composable
fun Sektion(
    schluessel: String,
    titel: String,
    icon: ImageVector,
    farbe: Color,
    anzahl: Int,
    zustand: ZiehZustand,
    ziel: Ziel,
    untertitel: String? = null,
    startOffen: Boolean = true,
    /** Ohne Aufgaben bleibt der Bereich zugeklappt, bis man ihn selbst aufklappt. */
    leer: Boolean = false,
    verzoegerung: Int = 0,
    aktionen: @Composable RowScope.() -> Unit = {},
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val f = LocalFarben.current
    // 0 = automatisch (offen, sobald etwas drin ist), 1 = selbst aufgeklappt, 2 = selbst zugeklappt.
    var wahl by rememberSaveable(schluessel) { mutableIntStateOf(0) }
    // Wird der Bereich leer, gilt wieder die Automatik: leer heißt zugeklappt.
    LaunchedEffect(leer) { if (leer) wahl = 0 }
    val offen = when (wahl) { 1 -> true; 2 -> false; else -> startOffen && !leer }
    val schwebt = zustand.hoverZiel == "sek_$schluessel"
    val rand by animateColorAsState(if (schwebt) farbe else Color.Transparent, label = "rand")
    val hub by animateFloatAsState(if (schwebt) 1.015f else 1f, spring(dampingRatio = 0.6f), label = "hub")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .einblenden(verzoegerung)
            .graphicsLayer { scaleX = hub; scaleY = hub }
            .ablageZiel(zustand, "sek_$schluessel", ziel)
            .glas(f, erhoeht = 1.3f, fuellung = f.flaeche.copy(alpha = f.flaeche.alpha * 0.8f), toenung = farbe)
            .then(if (schwebt) Modifier.glas(f, erhoeht = 0f, fuellung = rand.copy(alpha = 0.12f)) else Modifier)
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = 380f)),
    ) {
        Row(
            Modifier.fillMaxWidth().antippen(haptik = false) { wahl = if (offen) 2 else 1 }.padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(38.dp).knopf3d(farbe.copy(alpha = 0.95f), farbe.copy(alpha = 0.6f), 13.dp, f.dunkel), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(titel, color = f.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    if (anzahl > 0) Box(
                        Modifier.padding(start = 8.dp).glas(f, 99.dp, 0f, farbe.copy(alpha = 0.18f), rand = false).padding(horizontal = 8.dp, vertical = 1.dp),
                    ) { Text("$anzahl", color = farbe, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                if (untertitel != null) Text(untertitel, color = f.textLeise, fontSize = 12.sp)
            }
            aktionen()
            val dreh by animateFloatAsState(if (offen) 0f else -90f, label = "pfeil")
            Icon(Icons.Rounded.ExpandMore, if (offen) "Zuklappen" else "Aufklappen", tint = f.textLeise, modifier = Modifier.graphicsLayer { rotationZ = dreh })
        }
        AnimatedVisibility(offen, enter = expandVertically(clip = false) + fadeIn(), exit = shrinkVertically(clip = false) + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                inhalt()
            }
        }
    }
}

@Composable
fun LeerHinweis(icon: ImageVector, text: String) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().glas(f, radius = f.radius * 0.6f, erhoeht = 0f, fuellung = f.flaeche.copy(alpha = f.flaeche.alpha * 0.5f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = f.textSchwach, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
fun Abstand(h: Int) = Spacer(Modifier.height(h.dp))

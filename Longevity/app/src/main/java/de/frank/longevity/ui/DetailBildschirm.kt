package de.frank.longevity.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.data.Faktor
import de.frank.longevity.data.platz
import de.frank.longevity.ki.KiArbeit
import de.frank.longevity.ui.theme.Chip
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.antippen
import de.frank.longevity.ui.theme.glas
import de.frank.longevity.ui.theme.knopf3d

@Composable
fun DetailBildschirm(vm: AppViewModel, id: Long) {
    val f = LocalFarben.current
    val alle by vm.alle.collectAsState()
    val x = alle.firstOrNull { it.id == id }
    val liste = alle.filter { !it.vorschlag }
    val gesamt = liste.size
    val plusAnzahl = liste.count { !it.raeuber }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text(
                when {
                    x == null -> ""
                    x.vorschlag -> "Vorschlag der KI"
                    x.raeuber -> "Lebenszeit-Räuber · Platz ${x.platz(liste)}"
                    else -> "Platz ${x.rang} von $plusAnzahl"
                },
                color = f.textLeise, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (x != null && !x.vorschlag) RundKnopf(Icons.Rounded.AutoAwesome, "Mit KI vertiefen") { vm.vertiefen(x) }
            if (x != null) RundKnopf(Icons.Rounded.DeleteOutline, "Löschen") { vm.loeschen(x) }
        }
        if (x == null) return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Held(x, x.platz(liste))
            KiKarte(vm, nurFuer = x.id)
            if (x.vorschlag) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("In die Liste aufnehmen", true, icon = Icons.Rounded.Add) { vm.vorschlagAnnehmen(x); vm.zurueck() }
                Chip("Verwerfen", false, icon = Icons.Rounded.Close) { vm.vorschlagVerwerfen(x); vm.zurueck() }
            }
            vm.bewertungen[x.id]?.takeIf { it.isNotBlank() }?.let { b ->
                Block("Einschätzung deiner Idee") { Text(b, color = f.text, fontSize = 15.sp, lineHeight = 22.sp) }
            }
            if (x.eigen && x.notiz.isNotBlank()) Block("Deine Eingabe") {
                Text("„${x.notiz}“", color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (x.erklaerung.isNotBlank()) Block("Worum es geht") { Absaetze(x.erklaerung) }
            Block(if (x.vorschlag) "Warum an dieser Stelle" else "Warum Platz ${x.platz(liste)}") {
                if (x.begruendung.isNotBlank()) Absaetze(x.begruendung)
                // Lebenszeit-Räuber ordnen sich selbst nach verlorenen Jahren; Plus-Faktoren bleiben über der Null-Linie.
                if (!x.vorschlag && !x.raeuber) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Selbst verschieben", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    RundKnopf(Icons.Rounded.KeyboardArrowUp, "Einen Platz höher") { if (x.rang > 1) vm.verschiebe(x, x.rang - 1) }
                    RundKnopf(Icons.Rounded.KeyboardArrowDown, "Einen Platz tiefer") { if (x.rang < plusAnzahl) vm.verschiebe(x, x.rang + 1) }
                }
            }
            if (x.ziel.isNotBlank() && !x.vorschlag) ZielKarte(x.ziel, x.zielErreicht) { vm.zielUmschalten(x) }
            Aufgabenplan(vm, x)
            if (!x.vorschlag) HauptKnopf(
                Icons.Rounded.AutoAwesome,
                if (x.vertieft) "Erneut mit KI vertiefen" else "Mit KI vertiefen",
                beschaeftigt = KiArbeit.laeuft && KiArbeit.bezugId == x.id,
            ) { vm.vertiefen(x) }
            Text(
                "Die KI verbessert Erklärung, Ziel und Aufgabenplan – Bestehendes bleibt erhalten, deine Häkchen auch.",
                color = f.textSchwach, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

/** Kopf der Detailseite als Infografik: Rang, Titel, Wirkung als Tacho, Kennzahlen. */
@Composable
private fun Held(x: Faktor, platz: String) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().einblenden().glas(f, erhoeht = 1.6f, toenung = f.evidenzFarbe(x.ev)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            RangAbzeichen(x.rang, 54, raeuber = x.raeuber, text = platz)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(x.titel, color = f.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RangPfeil(x)
                    Text("${x.kat.emoji} ${x.kat.anzeige}", color = f.textLeise, fontSize = 12.sp)
                }
            }
        }
        if (x.kurz.isNotBlank()) Text(x.kurz, color = f.text, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Tacho(x.wirkung, x.jahre)
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EvidenzAbzeichen(x.ev, lang = true)
                val p = x.punkte
                if (p.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniRing(p.count { it.erledigt }.toFloat() / p.size, Modifier.size(22.dp), 3.5f)
                    Text("${p.count { it.erledigt }} von ${p.size} umgesetzt", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                }
                Text(if (x.raeuber) "Geschätzter Verlust an gesunder Lebenszeit" else "Geschätzter Gewinn an gesunder Lebenszeit", color = f.textSchwach, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ZielKarte(ziel: String, erreicht: Boolean, umschalten: () -> Unit) {
    val f = LocalFarben.current
    Column(
        Modifier.fillMaxWidth().einblenden(60)
            .drawBehind {
                drawRoundRect(
                    if (erreicht) Brush.linearGradient(listOf(f.erfolg, f.erfolg)) else Brush.linearGradient(listOf(f.primaer, f.sekundaer, f.tertiaer)),
                    cornerRadius = CornerRadius(f.radius.toPx()), style = Stroke(2.dp.toPx()),
                )
            }
            .glas(f, erhoeht = 1.3f, fuellung = f.flaecheStark, toenung = if (erreicht) f.erfolg else f.sekundaer).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (erreicht) "🏆" else "🎯", fontSize = 30.sp)
            Column(Modifier.padding(start = 14.dp)) {
                Text(if (erreicht) "ZIEL ERREICHT" else "DEIN ZIEL", color = if (erreicht) f.erfolg else f.primaer, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text(ziel, color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(
            Modifier.padding(top = 14.dp).fillMaxWidth()
                .then(if (erreicht) Modifier.glas(f, 14.dp, 0.5f, f.erfolg.copy(alpha = 0.14f)) else Modifier.knopf3d(f.erfolg, f.erfolg.copy(alpha = 0.75f), 14.dp, f.dunkel))
                .antippen(aktion = umschalten).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Checkkreis(erreicht, if (erreicht) f.erfolg else androidx.compose.ui.graphics.Color.White, 20, umschalten)
            Text(
                if (erreicht) "Erreicht – antippen zum Zurücknehmen" else "Ziel als erreicht markieren",
                color = if (erreicht) f.erfolg else androidx.compose.ui.graphics.Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Composable
private fun Aufgabenplan(vm: AppViewModel, x: Faktor) {
    val f = LocalFarben.current
    val punkte = x.punkte
    if (punkte.isEmpty()) return
    Block("Aufgabenplan · nach Wichtigkeit") {
        FortschrittsBalken(punkte.count { it.erledigt }.toFloat() / punkte.size)
        punkte.forEachIndexed { i, p ->
            var offen by remember(p.titel) { mutableStateOf(i < 3) }
            Row(
                Modifier.fillMaxWidth().animateContentSize().glas(f, 16.dp, 0.3f, if (p.erledigt) f.erfolg.copy(alpha = 0.10f) else f.flaeche)
                    .antippen(haptik = false) { offen = !offen }.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkkreis(p.erledigt, f.erfolg, 22) { vm.punktUmschalten(x, i) }
                Column(Modifier.weight(1f).padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}. ${p.titel}", color = if (p.erledigt) f.textLeise else f.text, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                            maxLines = if (offen) 3 else 1, overflow = TextOverflow.Ellipsis,
                        )
                        Box(Modifier.padding(start = 6.dp)) { EvidenzAbzeichen(p.ev) }
                    }
                    if (offen && p.text.isNotBlank()) Text(p.text, color = f.textLeise, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tippen klappt auf · Kreis hakt ab", color = f.textSchwach, fontSize = 11.sp)
            Spacer(Modifier.width(1.dp))
        }
    }
}

/** Text mit Absätzen; „Neu (Datum):“-Absätze werden hervorgehoben. */
@Composable
private fun Absaetze(text: String) {
    val f = LocalFarben.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        text.split("\n\n").filter { it.isNotBlank() }.forEach { a ->
            val neu = a.startsWith("Neu (") || a.startsWith("Deine Idee:")
            Text(
                a.trim(), color = if (neu) f.primaer else f.text, fontSize = 15.sp, lineHeight = 22.sp,
                fontWeight = if (neu) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

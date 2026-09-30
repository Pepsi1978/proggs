package de.frank.longevity.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import de.frank.longevity.ui.theme.Chip
import de.frank.longevity.ui.theme.antippen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.ki.Beitrag
import de.frank.longevity.ki.KiArbeit
import de.frank.longevity.ki.LongevityKi
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.glas
import de.frank.longevity.ui.theme.knopf3d

/** Die Diskussion der Agenten als Chat – live, während sie läuft. */
@Composable
fun ProtokollBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val zustand = rememberLazyListState()
    val beitraege = KiArbeit.protokoll.toList() + listOfNotNull(KiArbeit.live)
    LaunchedEffect(beitraege.size, KiArbeit.live?.text?.length?.div(400)) {
        if (beitraege.isNotEmpty()) zustand.animateScrollToItem(beitraege.size)
    }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Diskussion der Agenten", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Pro und Contra, dann entscheidet die Gutachterin – du kannst mitreden", color = f.textLeise, fontSize = 12.sp)
            }
        }
        LazyColumn(
            state = zustand,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item { KiKarte(vm, nurFuer = null) }
            itemsIndexed(beitraege, key = { i, b -> "$i-${b.name}" }) { i, b ->
                Blase(b, live = KiArbeit.live != null && i == beitraege.lastIndex)
            }
            if (beitraege.isEmpty()) item {
                Text("Noch keine Diskussion. Tippe in der Liste oben rechts auf Aktualisieren.", color = f.textLeise, fontSize = 14.sp)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        MitredenLeiste(vm)
    }
}

/** Unten fest: eigener Beitrag per Sprache oder Tippen, KI-Korrektur mit Rückgängig, Senden an alle Agenten. */
@Composable
private fun MitredenLeiste(vm: AppViewModel) {
    val f = LocalFarben.current
    val gesperrt = KiArbeit.laeuft
    Column(
        Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp)
            .glas(f, erhoeht = 1.6f, fuellung = f.flaecheStark, toenung = f.erfolg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 44.dp, max = 160.dp).verticalScroll(rememberScrollState())) {
            if (vm.eText.isEmpty()) Text(
                when {
                    vm.aufnahme == Aufnahme.LAEUFT -> "Ich höre zu …"
                    gesperrt -> "Die Agenten sind gerade dran …"
                    else -> "Deine Frage oder dein Einwand an die Runde …"
                },
                color = f.textSchwach, fontSize = 15.sp,
            )
            BasicTextField(
                vm.eText, vm::textGeaendert, enabled = !gesperrt,
                textStyle = TextStyle(color = f.text, fontSize = 15.sp, lineHeight = 21.sp),
                cursorBrush = SolidColor(f.primaer),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MikroKnopf(vm)
            val zurueck = vm.textVorKorrektur != null
            Chip(if (zurueck) "Zurück" else "Korrektur", false, icon = if (zurueck) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.AutoFixHigh) {
                if (zurueck) vm.korrekturZuruecknehmen() else vm.korrigieren()
            }
            if (zurueck) Chip("Neu", false, icon = Icons.Rounded.AutoFixHigh) { vm.korrigieren() }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(48.dp).then(
                    if (gesperrt) Modifier.glas(f, 99.dp, 0.5f) else Modifier.knopf3d(f.erfolg, f.primaer, 99.dp, f.dunkel),
                ).antippen { if (!gesperrt) vm.einwandSenden() },
                contentAlignment = Alignment.Center,
            ) {
                if (vm.korrigiert) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Icon(Icons.AutoMirrored.Rounded.Send, "In die Diskussion einbringen", tint = if (gesperrt) f.textSchwach else Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun MikroKnopf(vm: AppViewModel) {
    val f = LocalFarben.current
    val laeuft = vm.aufnahme == Aufnahme.LAEUFT
    val verarbeitet = vm.aufnahme == Aufnahme.VERARBEITET
    val puls = rememberInfiniteTransition(label = "mikro")
    val p by puls.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "p")
    Box(
        Modifier.size(48.dp).drawBehind {
            if (laeuft) drawCircle(f.gefahr.copy(alpha = 0.35f * (1f - p)), size.minDimension / 2 * (0.9f + 0.5f * p))
        }.knopf3d(if (laeuft) f.gefahr else f.primaer, f.sekundaer, 99.dp, f.dunkel).antippen { if (!KiArbeit.laeuft) vm.mikroTippen() },
        contentAlignment = Alignment.Center,
    ) {
        if (verarbeitet) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else Icon(if (laeuft) Icons.Rounded.Stop else Icons.Rounded.Mic, if (laeuft) "Aufnahme beenden" else "Einsprechen", tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Blase(b: Beitrag, live: Boolean) {
    val f = LocalFarben.current
    val (emoji, farbe, rechts) = when (b.name) {
        LongevityKi.PRO -> Triple("🔬", f.primaer, false)
        LongevityKi.CONTRA -> Triple("🧐", f.sekundaer, true)
        LongevityKi.NUTZER -> Triple("🙋", f.erfolg, true)
        else -> Triple("⚖️", f.tertiaer, false)
    }
    val richter = b.name == LongevityKi.RICHTER
    val text = when {
        richter && live -> "Wägt alle Argumente ab und legt die neue Rangliste fest …"
        richter -> runCatching {
            val o = de.frank.longevity.ki.jsonAus(b.text)
            listOf(o.optString("einordnung"), o.optString("zusammenfassung")).filter { it.isNotBlank() }.joinToString("\n\n")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Entscheidung gefällt – die Rangliste wurde übernommen."
        else -> b.text + if (live) " ▍" else ""
    }
    Row(Modifier.fillMaxWidth().einblenden(), verticalAlignment = Alignment.Top) {
        if (rechts) Spacer(Modifier.weight(0.12f))
        if (!rechts) Avatar(emoji, farbe)
        Column(
            Modifier.weight(1f).padding(horizontal = 8.dp)
                .glas(f, 20.dp, if (live) 1.6f else 0.8f, f.flaecheStark, toenung = farbe).padding(14.dp),
        ) {
            Text(b.name, color = farbe, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(text.ifBlank { "denkt nach …" }, color = f.text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (rechts) Avatar(emoji, farbe)
        if (!rechts) Spacer(Modifier.weight(0.04f))
    }
}

@Composable
private fun Avatar(emoji: String, farbe: Color) {
    val f = LocalFarben.current
    Box(Modifier.size(40.dp).knopf3d(farbe, farbe.copy(alpha = 0.7f), 99.dp, f.dunkel), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = 20.sp)
    }
}

package de.frank.longevity.ui

import androidx.compose.foundation.layout.Arrangement
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
                Text("Pro und Contra, dann entscheidet die Gutachterin", color = f.textLeise, fontSize = 12.sp)
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
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun Blase(b: Beitrag, live: Boolean) {
    val f = LocalFarben.current
    val (emoji, farbe, rechts) = when (b.name) {
        LongevityKi.PRO -> Triple("🔬", f.primaer, false)
        LongevityKi.CONTRA -> Triple("🧐", f.sekundaer, true)
        else -> Triple("⚖️", f.tertiaer, false)
    }
    val richter = b.name == LongevityKi.RICHTER
    val text = when {
        richter && live -> "Wägt alle Argumente ab und legt die neue Rangliste fest …"
        richter -> runCatching { de.frank.longevity.ki.jsonAus(b.text).optString("zusammenfassung") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: "Entscheidung gefällt – die Rangliste wurde übernommen."
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

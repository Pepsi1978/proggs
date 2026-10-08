package de.frank.modellkompass.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.modellkompass.data.Bereich
import de.frank.modellkompass.data.Modellfund
import de.frank.modellkompass.ui.theme.Chip
import de.frank.modellkompass.ui.theme.LocalFarben
import de.frank.modellkompass.ui.theme.antippen
import de.frank.modellkompass.ui.theme.glas
import de.frank.modellkompass.ui.theme.knopf3d

/** Grafikspeicher der RTX 5090 – Maßstab für den Balken. */
private const val VRAM_GB = 32f

private fun kopiere(context: Context, vm: AppViewModel, text: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Modell", text))
    vm.melde("Kopiert: $text")
}

private fun oeffne(context: Context, adresse: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse))) }
}

@Composable
fun BereichBildschirm(vm: AppViewModel, b: Bereich) {
    val f = LocalFarben.current
    val e = vm.ergebnisse[b.id]
    val status = vm.status[b.id]
    val fehler = vm.fehler[b.id]
    Column(Modifier.fillMaxSize().displayCutoutPadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text(b.titel, color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 8.dp))
            if (vm.laufAktiv) RundKnopf(Icons.Rounded.Close, "Recherche abbrechen") { vm.abbrechen() }
            else RundKnopf(Icons.Rounded.Refresh, "Diesen Bereich aktualisieren") { vm.einzelLauf(b) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ---- Überblick ----
            Row(Modifier.fillMaxWidth().einblenden().glas(f, 26.dp, 1.4f, toenung = f.primaer).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).knopf3d(f.primaer, f.sekundaer, 18.dp, f.dunkel), contentAlignment = Alignment.Center) { Text(b.emoji, fontSize = 30.sp) }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(b.beschreibung, color = f.text, fontSize = 14.sp, lineHeight = 19.sp)
                    Text("Läuft in: ${b.laufzeit}", color = f.primaer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                    if (e != null) Text("Stand ${standText(e.stand)} Uhr · ${e.modell}", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (status != null) Row(Modifier.fillMaxWidth().glas(f, 18.dp, 0.6f, f.primaer.copy(alpha = 0.12f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Text(status, color = f.text, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
            }
            if (fehler != null && status == null) Column(Modifier.fillMaxWidth().glas(f, 18.dp, 0.6f, f.gefahr.copy(alpha = 0.12f)).padding(14.dp)) {
                Text("Recherche fehlgeschlagen", color = f.gefahr, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(fehler, color = f.text, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (e != null && e.zusammenfassung.isNotBlank()) Block("LAGE IN DIESEM BEREICH") {
                Text(e.zusammenfassung, color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
            }
            e?.modelle?.forEachIndexed { i, m -> ModellKarte(vm, b, m, i) }
            if (e?.rohtext != null) Block("ANTWORT ALS TEXT") {
                Text("Die Antwort ließ sich nicht in Karten aufteilen. Hier steht sie vollständig:", color = f.textLeise, fontSize = 12.sp)
                Text(e.rohtext, color = f.text, fontSize = 13.sp, lineHeight = 19.sp)
            }
            if (e == null && status == null) Column(Modifier.fillMaxWidth().einblenden(120).glas(f, 26.dp, 1f).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Noch keine Recherche", color = f.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "ChatGPT sucht im Internet nach den Modellen mit der derzeit besten Qualität, die auf deiner Grafikkarte gut laufen, und prüft die Namen auf Hugging Face.",
                    color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().height(52.dp).knopf3d(f.primaer, f.sekundaer, 16.dp, f.dunkel).antippen { vm.einzelLauf(b) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.TravelExplore, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Diesen Bereich recherchieren", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun ModellKarte(vm: AppViewModel, b: Bereich, m: Modellfund, index: Int) {
    val f = LocalFarben.current
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().einblenden(140 + index * 70).glas(f, 26.dp, if (index == 0) 1.8f else 1f, toenung = if (index == 0) f.primaer else null).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).then(if (index == 0) Modifier.knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel) else Modifier.glas(f, 99.dp, 0.5f, f.primaer.copy(alpha = 0.16f))),
                contentAlignment = Alignment.Center,
            ) { Text("${index + 1}", color = if (index == 0) Color.White else f.primaer, fontSize = 18.sp, fontWeight = FontWeight.Black) }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(m.name, color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                if (m.herausgeber.isNotBlank()) Text("von ${m.herausgeber}", color = f.textLeise, fontSize = 13.sp)
            }
        }
        // ---- Der Text zum Eintippen ----
        val suchtext = m.lmStudio.ifBlank { m.hfRepo }
        if (suchtext.isNotBlank()) Column {
            Text(if (m.lmStudio.isNotBlank()) "In LM Studio eintippen" else "Auf Hugging Face", color = f.textLeise, fontSize = 12.sp)
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).glas(f, 14.dp, 0.4f, f.flaecheStark).antippen { kopiere(context, vm, suchtext) }.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(suchtext, color = f.primaer, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ContentCopy, "Kopieren", tint = f.textLeise, modifier = Modifier.padding(start = 8.dp).size(20.dp))
            }
            if (m.datei.isNotBlank()) Text("Datei / Variante: ${m.datei}", color = f.text, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Text(
                when (m.geprueft) {
                    true -> "✓ Auf Hugging Face gefunden"
                    false -> "⚠ Unter diesem Namen nicht auf Hugging Face gefunden – Schreibweise prüfen"
                    null -> "Hugging Face war nicht erreichbar – Name ungeprüft"
                },
                color = when (m.geprueft) { true -> f.erfolg; false -> f.gefahr; null -> f.textSchwach },
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp),
            )
        }
        // ---- Grafikspeicher ----
        m.vramGb?.let { gb ->
            var gestartet by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { gestartet = true }
            val anteil by animateFloatAsState(if (gestartet) (gb.toFloat() / VRAM_GB).coerceIn(0.04f, 1f) else 0.04f, tween(900, delayMillis = 200 + index * 70), label = "vram")
            val knapp = gb > VRAM_GB
            Column {
                Row {
                    Text("Grafikspeicher", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        "≈ ${"%.1f".format(gb).replace('.', ',').removeSuffix(",0")} von 32 GB",
                        color = if (knapp) f.gefahr else f.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(10.dp).glas(f, 99.dp, 0f, f.textSchwach.copy(alpha = 0.22f), rand = false)) {
                    Box(Modifier.fillMaxWidth(anteil).fillMaxHeight().knopf3d(if (knapp) f.gefahr else f.primaer, if (knapp) f.gefahr else f.sekundaer, 99.dp, f.dunkel))
                }
            }
        }
        if (m.staerken.isNotBlank()) Text(m.staerken, color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
        if (m.hinweis.isNotBlank()) Text(m.hinweis, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
        ChipReihe {
            if (m.parameter.isNotBlank()) Chip(m.parameter, false) { kopiere(context, vm, m.name) }
            if (m.laeuftIn.isNotBlank() && !(b.inLmStudio && m.laeuftIn.contains("LM Studio", ignoreCase = true))) Chip(m.laeuftIn, false) {}
            if (m.hfRepo.isNotBlank()) Chip("Hugging Face", false, icon = Icons.Rounded.OpenInNew) { oeffne(context, "https://huggingface.co/${m.hfRepo}") }
            if (m.quelle.startsWith("http")) Chip("Quelle", false, icon = Icons.Rounded.Link) { oeffne(context, m.quelle) }
        }
    }
}

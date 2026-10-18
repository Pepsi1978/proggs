package de.frank.jarvis.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.Medien
import de.frank.jarvis.ablage.Vorlaeufig
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Was eine Bild- oder PDF-Karte im Chat über ihre Datei wissen muss. */
private class ChatDatei(val datei: File?, val pdf: Boolean, val vorlaeufig: Boolean, val ablageTitel: String?, val bild: ImageBitmap?, val seiten: Int)

private fun liesChatDatei(context: Context, pfad: String): ChatDatei {
    // Nach „In Ablage verschieben“ liegt die Datei woanders; der alte Verweis führt über den Umzug zur neuen.
    val datei = Vorlaeufig.aufloesen(context, pfad) ?: return ChatDatei(null, false, false, null, null, 0)
    val pdf = datei.extension.equals("pdf", ignoreCase = true)
    val bild = runCatching { if (pdf) Medien.pdfSeite(datei, 0, 1400)?.second else Medien.bildVerkleinert(datei, 1600) }.getOrNull()?.asImageBitmap()
    val seiten = if (pdf) runCatching { Medien.pdfSeiten(datei) }.getOrDefault(1) else 0
    return ChatDatei(datei, pdf, Vorlaeufig.istVorlaeufig(context, datei.absolutePath), Vorlaeufig.uebernommenNach(context, datei.absolutePath), bild, seiten)
}

/**
 * Bilder und PDFs, die Jarvis im Gespräch erzeugt hat (Bild aus bild_erzeugen, Ergebnis eines Agenten): groß im Chat, Antippen
 * öffnet sie im Vollbild. Vorläufige Dateien tragen „In Ablage verschieben“; was schon in der Ablage liegt, sagt das.
 */
@Composable
fun JarvisDateien(vm: AppViewModel, pfade: List<String>) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        pfade.forEach { ChatDateiKarte(vm, it) }
    }
}

@Composable
private fun ChatDateiKarte(vm: AppViewModel, pfad: String) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    // Neu lesen, sobald sich Ablage oder vorläufiger Speicher ändern (Übernehmen, Löschen in der Ablage).
    val standAblage by remember { AblageZentrale.speicher(context).stand }.collectAsState()
    val standVorlaeufig by remember { Vorlaeufig.speicher(context).stand }.collectAsState()
    val info by produceState<ChatDatei?>(null, pfad, standAblage, standVorlaeufig) { value = withContext(Dispatchers.IO) { liesChatDatei(context, pfad) } }
    var vollbild by rememberSaveable(pfad) { mutableStateOf(false) }
    val form = RoundedCornerShape(18.dp)
    val d = info
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        when {
            d == null -> Box(Modifier.fillMaxWidth().height(180.dp).clip(form).background(f.flaeche), contentAlignment = Alignment.Center) {
                Text("Lädt …", color = f.textLeise, fontSize = 14.sp)
            }
            d.datei == null -> Text(
                "Diese Datei ist nicht mehr da (vorläufige Dateien bleiben ${Vorlaeufig.AUFBEWAHRUNG_TAGE} Tage, wenn sie nicht in die Ablage verschoben wurden).",
                color = f.textLeise, fontSize = 14.sp,
            )
            else -> {
                val bild = d.bild
                if (bild != null) {
                    Image(
                        bild, if (d.pdf) "PDF ${d.datei.name}" else "Bild von Jarvis",
                        Modifier.fillMaxWidth().aspectRatio(bild.width.toFloat() / bild.height.coerceAtLeast(1)).clip(form)
                            .background(if (f.dunkel) Color.Black else Color(0xFFEFF3F8)).antippen { vollbild = true },
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text("„${d.datei.name}“ lässt sich hier nicht anzeigen.", color = f.textLeise, fontSize = 14.sp)
                }
                if (d.pdf) Text("PDF · ${d.seiten} Seite(n) · antippen zum Lesen", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val zustand = vm.ablageUmzug[pfad]
                    when {
                        d.ablageTitel != null -> Text("✓ In der Ablage: „${d.ablageTitel}“", color = f.erfolg, fontSize = 14.sp)
                        d.vorlaeufig -> Chip(
                            if (zustand == AppViewModel.UMZUG_LAEUFT) "Wird gespeichert …" else "In Ablage verschieben",
                            aktiv = false, icon = Icons.Rounded.Inventory2,
                        ) { vm.chatDateiInAblage(pfad) }
                    }
                }
                vm.ablageUmzug[pfad]?.takeIf { it != AppViewModel.UMZUG_LAEUFT }?.let {
                    Text("Nicht gespeichert: $it", color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    val datei = d?.datei
    if (vollbild && datei != null) {
        Dialog({ vollbild = false }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (d.pdf) PdfSeiten(datei, d.seiten) else KachelBild(datei, 0, "Bild von Jarvis", beiTipp = { vollbild = false }, schwarz = true) {}
                Box(Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 12.dp).size(44.dp).clip(RoundedCornerShape(99.dp))
                    .background(Color.Black.copy(alpha = 0.5f)).antippen { vollbild = false }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Schließen", tint = Color.White)
                }
            }
        }
    }
}

/** Alle Seiten eines PDFs untereinander, jede erst beim Hinscrollen gerendert. */
@Composable
private fun PdfSeiten(datei: File, anzahl: Int) {
    LazyColumn(Modifier.fillMaxSize().padding(top = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(anzahl.coerceAtLeast(1)) { i ->
            val seite by produceState<ImageBitmap?>(null, datei, i) {
                value = withContext(Dispatchers.IO) { runCatching { Medien.pdfSeite(datei, i, 1800)?.second?.asImageBitmap() }.getOrNull() }
            }
            val s = seite
            if (s != null) Image(s, "Seite ${i + 1}", Modifier.fillMaxWidth().aspectRatio(s.width.toFloat() / s.height.coerceAtLeast(1)), contentScale = ContentScale.Fit)
            else Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) { Text("Seite ${i + 1} lädt …", color = Color.White) }
        }
    }
}

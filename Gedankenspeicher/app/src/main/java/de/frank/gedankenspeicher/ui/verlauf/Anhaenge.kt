package de.frank.gedankenspeicher.ui.verlauf

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import de.frank.gedankenspeicher.data.Anhang
import de.frank.gedankenspeicher.data.Anhangsart
import de.frank.gedankenspeicher.data.Anhangsspeicher
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.merkeDruck
import de.frank.gedankenspeicher.ui.theme.schwebendeKarte
import de.frank.gedankenspeicher.ui.theme.sinktEin
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **Das Plus-Menü und alles, was daraus entsteht** — PDF, Sprachaufnahme, Bild, Kamera,
 * Dokumentenscan, Audiodatei, Zeichnung, Haftnotiz und Tabelle.
 *
 * Alle neun Einträge legen ihr Ergebnis als [Anhang] im App-Speicher ab; die Notiz merkt
 * sich nur die Beschreibung. Dateien werden kopiert statt verlinkt, damit eine Notiz nicht
 * kaputtgeht, wenn die Quelle verschwindet.
 */

/** Die Haftnotizfarben — gedeckt, damit sie neben den Karten nicht schreien. */
private val Haftfarben = listOf(0xFFFFF3B0, 0xFFFFD6C9, 0xFFCDEBC5, 0xFFC9E4FF, 0xFFE6D6FF, 0xFFF2F2F2)

/** Die Stiftfarben der Zeichenfläche. */
private val Stiftfarben = listOf(0xFF1A1A1A, 0xFFD32F2F, 0xFF1565C0, 0xFF2E7D32, 0xFFF9A825, 0xFF6A1B9A)

// ------------------------------------------------------------------ Plus-Knopf

/**
 * Der Plus-Knopf in der Fußleiste, zwischen Notizfeld und KI-Knopf.
 *
 * Er trägt bewusst keinen Akzentrand: den haben laut `02-UI-SPEC.md` nur KI- und
 * Aufnahmeknopf.
 */
@Composable
fun Anhangsknopf(
    beiAnhang: (Anhang) -> Unit,
    beiFehler: (String) -> Unit,
    beiMikrofon: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = Farben
    val ctx = LocalContext.current
    val speicher = remember(ctx) { Anhangsspeicher(ctx.applicationContext) }
    val bereich = rememberCoroutineScope()
    val druck = merkeDruck()
    var menue by remember { mutableStateOf(false) }
    var zeichnung by remember { mutableStateOf(false) }
    var haftnotiz by remember { mutableStateOf(false) }
    var tabelle by remember { mutableStateOf(false) }
    var sprachaufnahme by remember { mutableStateOf(false) }
    var kameradatei by remember { mutableStateOf<File?>(null) }

    fun uebernimm(uri: Uri?, art: Anhangsart) {
        if (uri == null) return
        bereich.launch {
            runCatching { speicher.uebernimm(uri, art) }
                .onSuccess(beiAnhang)
                .onFailure { beiFehler(it.message ?: "Der Anhang konnte nicht übernommen werden.") }
        }
    }

    val pdfWahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uebernimm(it, Anhangsart.PDF)
    }
    val bildWahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uebernimm(it, Anhangsart.BILD)
    }
    val audioWahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uebernimm(it, Anhangsart.AUDIO)
    }
    val kamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { geklappt ->
        val datei = kameradatei
        kameradatei = null
        if (geklappt && datei != null && datei.length() > 0) {
            beiAnhang(speicher.beschreibe(datei, Anhangsart.BILD, "Kameraaufnahme"))
        } else {
            datei?.delete()
        }
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { ergebnis ->
        val seiten = GmsDocumentScanningResult.fromActivityResultIntent(ergebnis.data)?.pages.orEmpty()
        if (ergebnis.resultCode == Activity.RESULT_OK && seiten.isNotEmpty()) {
            bereich.launch {
                seiten.forEachIndexed { nummer, seite ->
                    runCatching { speicher.uebernimm(seite.imageUri, Anhangsart.SCAN) }
                        .onSuccess { anhang -> beiAnhang(anhang.copy(name = "Scan Seite ${nummer + 1}")) }
                        .onFailure { beiFehler(it.message ?: "Der Scan konnte nicht übernommen werden.") }
                }
            }
        }
    }

    Box(modifier) {
        Box(
            modifier = Modifier
                .size(Masse.kiKnopf)
                .sinktEin(druck)
                .clip(RoundedCornerShape(50))
                .border(1.dp, farben.rand, RoundedCornerShape(50))
                .background(farben.hintergrundErhoben)
                .clickable(interactionSource = druck, indication = null) { menue = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, "Anhang hinzufügen", Modifier.size(22.dp), tint = farben.textMittel)
        }
        if (menue) {
            Anhangsmenue(
                beiSchliessen = { menue = false },
                beiPdf = {
                    runCatching { pdfWahl.launch(arrayOf("application/pdf")) }
                        .onFailure { beiFehler("Es wurde keine Dateiauswahl gefunden.") }
                },
                beiSprachaufnahme = { beiMikrofon { sprachaufnahme = true } },
                beiBild = {
                    runCatching { bildWahl.launch(arrayOf("image/*")) }
                        .onFailure { beiFehler("Es wurde keine Bildauswahl gefunden.") }
                },
                beiKamera = {
                    val datei = speicher.neueDatei(".jpg")
                    kameradatei = datei
                    runCatching {
                        kamera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.dateien", datei))
                    }.onFailure {
                        kameradatei = null; datei.delete(); beiFehler("Es wurde keine Kamera-App gefunden.")
                    }
                },
                beiScan = {
                    val activity = ctx.findeActivity()
                    if (activity == null) {
                        beiFehler("Der Dokumentenscan ist hier nicht verfügbar.")
                    } else {
                        val optionen = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(true)
                            .setPageLimit(20)
                            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .build()
                        GmsDocumentScanning.getClient(optionen).getStartScanIntent(activity)
                            .addOnSuccessListener { absender ->
                                scanner.launch(IntentSenderRequest.Builder(absender).build())
                            }
                            .addOnFailureListener { fehler ->
                                beiFehler(fehler.message ?: "Der Dokumentenscanner konnte nicht gestartet werden.")
                            }
                    }
                },
                beiAudio = {
                    runCatching { audioWahl.launch(arrayOf("audio/*")) }
                        .onFailure { beiFehler("Es wurde keine Dateiauswahl gefunden.") }
                },
                beiZeichnung = { zeichnung = true },
                beiHaftnotiz = { haftnotiz = true },
                beiTabelle = { tabelle = true },
            )
        }
    }

    if (zeichnung) {
        ZeichenBlatt(
            speicher = speicher,
            beiAbbruch = { zeichnung = false },
            beiFertig = { anhang -> zeichnung = false; beiAnhang(anhang) },
            beiFehler = beiFehler,
        )
    }
    if (haftnotiz) HaftnotizBlatt({ haftnotiz = false }) { anhang -> haftnotiz = false; beiAnhang(anhang) }
    if (tabelle) TabellenBlatt({ tabelle = false }) { anhang -> tabelle = false; beiAnhang(anhang) }
    if (sprachaufnahme) {
        AufnahmeBlatt(
            speicher = speicher,
            beiAbbruch = { sprachaufnahme = false },
            beiFertig = { anhang -> sprachaufnahme = false; beiAnhang(anhang) },
            beiFehler = beiFehler,
        )
    }
}

/** Das aufklappende Menü — die neun Einträge in der Reihenfolge des Auftrags. */
@Composable
private fun Anhangsmenue(
    beiSchliessen: () -> Unit,
    beiPdf: () -> Unit,
    beiSprachaufnahme: () -> Unit,
    beiBild: () -> Unit,
    beiKamera: () -> Unit,
    beiScan: () -> Unit,
    beiAudio: () -> Unit,
    beiZeichnung: () -> Unit,
    beiHaftnotiz: () -> Unit,
    beiTabelle: () -> Unit,
) {
    val farben = Farben
    Popup(
        alignment = Alignment.BottomStart,
        offset = androidx.compose.ui.unit.IntOffset(0, -160),
        onDismissRequest = beiSchliessen,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 230.dp)
                .schwebendeKarte(farben, Masse.gruppeRadius)
                .padding(vertical = 8.dp),
        ) {
            Menuezeile("PDF", Icons.Outlined.PictureAsPdf) { beiSchliessen(); beiPdf() }
            Menuezeile("Sprachaufnahme", Icons.Outlined.Mic) { beiSchliessen(); beiSprachaufnahme() }
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth().height(1.dp).background(farben.rand),
            )
            Menuezeile("Bild", Icons.Outlined.Image) { beiSchliessen(); beiBild() }
            Menuezeile("Kamera", Icons.Outlined.PhotoCamera) { beiSchliessen(); beiKamera() }
            Menuezeile("Dokumentenscan", Icons.Outlined.DocumentScanner) { beiSchliessen(); beiScan() }
            Menuezeile("Audiodatei", Icons.Outlined.MusicNote) { beiSchliessen(); beiAudio() }
            Menuezeile("Zeichnung", Icons.Outlined.Palette) { beiSchliessen(); beiZeichnung() }
            Menuezeile("Haftnotiz", Icons.Outlined.StickyNote2) { beiSchliessen(); beiHaftnotiz() }
            Menuezeile("Tabelle", Icons.Outlined.TableChart) { beiSchliessen(); beiTabelle() }
        }
    }
}

@Composable
private fun Menuezeile(text: String, symbol: ImageVector, beiDruck: () -> Unit) {
    val farben = Farben
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Masse.tippflaeche)
            .clickable(onClick = beiDruck)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(symbol, null, Modifier.size(20.dp), tint = farben.textMittel)
        Spacer(Modifier.width(14.dp))
        Text(text, style = Schriften.einstellung, color = farben.textStark)
    }
}

// ------------------------------------------------------------ Anhänge am Entwurf

/** Die noch nicht gesendeten Anhänge als Reihe kleiner Marken über der Fußleiste. */
@Composable
fun Entwurfsanhaenge(anhaenge: List<Anhang>, beiEntfernen: (Anhang) -> Unit) {
    if (anhaenge.isEmpty()) return
    val farben = Farben
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        anhaenge.forEach { anhang ->
            Row(
                modifier = Modifier
                    .background(farben.hintergrundErhoben, RoundedCornerShape(50))
                    .border(1.dp, farben.rand, RoundedCornerShape(50))
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(anhang.art.symbol(), null, Modifier.size(16.dp), tint = farben.akzent)
                Spacer(Modifier.width(7.dp))
                Text(
                    anhang.name.ifBlank { anhang.beschriftung },
                    style = Schriften.zeitstempel,
                    color = farben.textMittel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp),
                )
                Box(
                    Modifier.padding(start = 4.dp).size(26.dp).clip(RoundedCornerShape(50))
                        .clickable { beiEntfernen(anhang) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, "Anhang entfernen", Modifier.size(15.dp), tint = farben.textSchwach)
                }
            }
        }
    }
}

// ------------------------------------------------------------- Anhänge in der Karte

/** Die Anhänge einer fertigen Notiz, unter ihrem Text. */
@Composable
fun Anhangsliste(anhaenge: List<Anhang>, modifier: Modifier = Modifier) {
    if (anhaenge.isEmpty()) return
    val ctx = LocalContext.current
    var vollbild by remember { mutableStateOf<Anhang?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        anhaenge.forEach { anhang ->
            when (anhang.art) {
                Anhangsart.BILD, Anhangsart.SCAN, Anhangsart.ZEICHNUNG ->
                    Bildanhang(anhang) { vollbild = anhang }
                Anhangsart.PDF -> Pdfanhang(anhang) { ctx.oeffneAlsPdf(anhang) }
                Anhangsart.SPRACHAUFNAHME, Anhangsart.AUDIO -> Tonanhang(anhang)
                Anhangsart.HAFTNOTIZ -> Haftnotizanhang(anhang)
                Anhangsart.TABELLE -> Tabellenanhang(anhang)
            }
        }
    }
    vollbild?.let { anhang -> Bildschau(anhang) { vollbild = null } }
}

@Composable
private fun Bildanhang(anhang: Anhang, beiDruck: () -> Unit) {
    val farben = Farben
    val bild = merkeBild(anhang.pfad)
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Masse.gruppeRadius))
            .background(farben.hintergrundGlas)
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
            .clickable(onClick = beiDruck),
    ) {
        if (bild == null) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(anhang.art.symbol(), null, Modifier.size(20.dp), tint = farben.akzent)
                Spacer(Modifier.width(10.dp))
                Text(anhang.name.ifBlank { anhang.beschriftung }, style = Schriften.einstellung, color = farben.textMittel)
            }
        } else {
            Image(
                bitmap = bild,
                contentDescription = anhang.name.ifBlank { anhang.beschriftung },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            )
        }
    }
}

@Composable
private fun Pdfanhang(anhang: Anhang, beiOeffnen: () -> Unit) {
    val farben = Farben
    val vorschau = merkeBild(anhang.vorschauPfad)
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Masse.gruppeRadius))
            .background(farben.hintergrundGlas)
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
            .clickable(onClick = beiOeffnen),
    ) {
        vorschau?.let { bild ->
            Image(
                bitmap = bild,
                contentDescription = anhang.name,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
            )
        }
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(20.dp), tint = farben.akzent)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    anhang.name.ifBlank { "PDF" },
                    style = Schriften.einstellung, color = farben.textStark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (anhang.seiten > 0) "${anhang.seiten} Seiten · zum Öffnen tippen" else "Zum Öffnen tippen",
                    style = Schriften.zeitstempel, color = farben.textSchwach,
                )
            }
        }
    }
}

@Composable
private fun Tonanhang(anhang: Anhang) {
    val farben = Farben
    var spielt by remember { mutableStateOf(false) }
    val spieler = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(anhang.id) {
        onDispose { spieler.value?.release(); spieler.value = null }
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Masse.gruppeRadius))
            .background(farben.hintergrundGlas)
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(Masse.kartenSymbolFlaeche).clip(RoundedCornerShape(50))
                .clickable {
                    val laufend = spieler.value
                    if (spielt && laufend != null) {
                        laufend.release(); spieler.value = null; spielt = false
                    } else {
                        val datei = anhang.datei
                        if (datei != null) {
                            spieler.value?.release()
                            spieler.value = runCatching {
                                MediaPlayer().apply {
                                    setDataSource(datei.absolutePath)
                                    setOnCompletionListener { p ->
                                        p.release(); spieler.value = null; spielt = false
                                    }
                                    prepare(); start()
                                }
                            }.getOrNull()
                            spielt = spieler.value != null
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (spielt) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                if (spielt) "Wiedergabe anhalten" else "Anhang abspielen",
                Modifier.size(Masse.kartenSymbol),
                tint = if (spielt) farben.akzent else farben.textMittel,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                anhang.name.ifBlank { anhang.beschriftung },
                style = Schriften.einstellung, color = farben.textStark,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (anhang.dauerMs > 0) laufzeitText(anhang.dauerMs) else anhang.beschriftung,
                style = Schriften.zeitstempel, color = farben.textSchwach,
            )
        }
    }
}

@Composable
private fun Haftnotizanhang(anhang: Anhang) {
    val grund = Color(if (anhang.farbe != 0) anhang.farbe else Haftfarben.first().toInt())
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp, Masse.gruppeRadius, 4.dp, 4.dp))
            .background(grund)
            .padding(16.dp),
    ) {
        Text("HAFTNOTIZ", style = Schriften.zeitstempel, color = Color(0xFF6B6B6B))
        Spacer(Modifier.height(6.dp))
        Text(anhang.text, style = Schriften.notiztext, color = Color(0xFF1A1A1A))
    }
}

@Composable
private fun Tabellenanhang(anhang: Anhang) {
    val farben = Farben
    val zeilen = remember(anhang.text) { anhang.text.split("\n").map { it.split("\t") } }
    if (zeilen.isEmpty()) return
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Masse.gruppeRadius))
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
            .horizontalScroll(rememberScrollState()),
    ) {
        zeilen.forEachIndexed { nummer, zeile ->
            Row(
                Modifier.background(
                    if (nummer == 0) farben.hintergrundErhoben else Color.Transparent,
                ),
            ) {
                zeile.forEach { zelle ->
                    Text(
                        zelle,
                        style = if (nummer == 0) Schriften.kartenUeberschrift else Schriften.einstellung,
                        color = if (nummer == 0) farben.textStark else farben.textMittel,
                        modifier = Modifier.width(132.dp).padding(horizontal = 10.dp, vertical = 9.dp),
                    )
                }
            }
            if (nummer < zeilen.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))
            }
        }
    }
}

@Composable
private fun Bildschau(anhang: Anhang, beiSchliessen: () -> Unit) {
    val farben = Farben
    val bild = merkeBild(anhang.pfad, 2400)
    Dialog(onDismissRequest = beiSchliessen) {
        Column(
            Modifier.schwebendeKarte(farben, Masse.blattRadius).padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    anhang.name.ifBlank { anhang.beschriftung },
                    style = Schriften.kartenUeberschrift, color = farben.textStark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
                Box(
                    Modifier.size(Masse.kartenSymbolFlaeche).clip(RoundedCornerShape(50))
                        .clickable(onClick = beiSchliessen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, "Schließen", Modifier.size(Masse.kartenSymbol), tint = farben.textMittel)
                }
            }
            Spacer(Modifier.height(8.dp))
            bild?.let {
                Image(
                    bitmap = it,
                    contentDescription = anhang.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
                        .clip(RoundedCornerShape(Masse.gruppeRadius)),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------- Zeichnung

private data class Strich(val punkte: SnapshotStateList<Offset>, val farbe: Long, val staerke: Float)

@Composable
private fun ZeichenBlatt(
    speicher: Anhangsspeicher,
    beiAbbruch: () -> Unit,
    beiFertig: (Anhang) -> Unit,
    beiFehler: (String) -> Unit,
) {
    val farben = Farben
    val striche = remember { mutableStateListOf<Strich>() }
    var farbe by remember { mutableStateOf(Stiftfarben.first()) }
    var staerke by remember { mutableStateOf(6f) }
    var flaeche by remember { mutableStateOf(IntSize.Zero) }
    val bereich = rememberCoroutineScope()

    Dialog(onDismissRequest = beiAbbruch) {
        Column(Modifier.schwebendeKarte(farben, Masse.blattRadius).padding(16.dp)) {
            Text("Zeichnung", style = Schriften.bildschirmtitel, color = farben.textStark)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().aspectRatio(0.82f)
                    .clip(RoundedCornerShape(Masse.gruppeRadius))
                    .background(Color.White)
                    .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
                    .onSizeChanged { flaeche = it }
                    .pointerInput(farbe, staerke) {
                        detectDragGestures(
                            onDragStart = { start -> striche.add(Strich(mutableStateListOf(start), farbe, staerke)) },
                            onDrag = { aenderung, _ ->
                                aenderung.consume()
                                striche.lastOrNull()?.punkte?.add(aenderung.position)
                            },
                        )
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    striche.forEach { strich ->
                        val punkte = strich.punkte
                        if (punkte.size == 1) drawCircle(Color(strich.farbe), strich.staerke / 2f, punkte.first())
                        for (stelle in 1 until punkte.size) {
                            drawLine(
                                color = Color(strich.farbe),
                                start = punkte[stelle - 1],
                                end = punkte[stelle],
                                strokeWidth = strich.staerke,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stiftfarben.forEach { wert ->
                    Box(
                        Modifier.padding(end = 8.dp)
                            .size(if (wert == farbe) 30.dp else 24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(wert))
                            .border(
                                if (wert == farbe) 2.dp else 0.dp,
                                farben.akzent,
                                RoundedCornerShape(50),
                            )
                            .clickable { farbe = wert },
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(Masse.kartenSymbolFlaeche).clip(RoundedCornerShape(50))
                        .clickable { if (striche.isNotEmpty()) striche.removeAt(striche.lastIndex) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Undo, "Letzten Strich zurücknehmen", Modifier.size(Masse.kartenSymbol), tint = farben.textMittel)
                }
                Box(
                    Modifier.size(Masse.kartenSymbolFlaeche).clip(RoundedCornerShape(50))
                        .clickable { striche.clear() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Delete, "Alles löschen", Modifier.size(Masse.kartenSymbol), tint = farben.textMittel)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(3f, 6f, 12f, 20f).forEach { wert ->
                    Box(
                        Modifier.padding(end = 12.dp).size((wert + 14).dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (wert == staerke) farben.akzent else farben.textSchwach)
                            .clickable { staerke = wert },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Blattknoepfe(
                beiAbbruch = beiAbbruch,
                bestaetigungAktiv = true,
                beiBestaetigen = {
                    if (striche.isEmpty() || flaeche.width == 0) {
                        beiAbbruch()
                    } else {
                        val fertig = striche.toList()
                        val breite = flaeche.width
                        val hoehe = flaeche.height
                        bereich.launch {
                            runCatching { speichereZeichnung(speicher, fertig, breite, hoehe) }
                                .onSuccess(beiFertig)
                                .onFailure { beiFehler(it.message ?: "Die Zeichnung konnte nicht gespeichert werden.") }
                        }
                    }
                },
            )
        }
    }
}

private suspend fun speichereZeichnung(
    speicher: Anhangsspeicher,
    striche: List<Strich>,
    breite: Int,
    hoehe: Int,
): Anhang = withContext(Dispatchers.IO) {
    val bild = android.graphics.Bitmap.createBitmap(breite, hoehe, android.graphics.Bitmap.Config.ARGB_8888)
    val leinwand = android.graphics.Canvas(bild)
    leinwand.drawColor(android.graphics.Color.WHITE)
    val stift = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    val tupfer = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    striche.forEach { strich ->
        val farbwert = Color(strich.farbe).toArgb()
        if (strich.punkte.size == 1) {
            tupfer.color = farbwert
            val start = strich.punkte.first()
            leinwand.drawCircle(start.x, start.y, strich.staerke / 2f, tupfer)
        } else {
            stift.color = farbwert
            stift.strokeWidth = strich.staerke
            val pfad = android.graphics.Path()
            strich.punkte.forEachIndexed { nummer, punkt ->
                if (nummer == 0) pfad.moveTo(punkt.x, punkt.y) else pfad.lineTo(punkt.x, punkt.y)
            }
            leinwand.drawPath(pfad, stift)
        }
    }
    val datei = speicher.neueDatei("-zeichnung.png")
    FileOutputStream(datei).use { aus -> bild.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, aus) }
    bild.recycle()
    speicher.beschreibe(datei, Anhangsart.ZEICHNUNG, "Zeichnung")
}

// ---------------------------------------------------------------------- Haftnotiz

@Composable
private fun HaftnotizBlatt(beiAbbruch: () -> Unit, beiFertig: (Anhang) -> Unit) {
    val farben = Farben
    var text by remember { mutableStateOf("") }
    var farbe by remember { mutableStateOf(Haftfarben.first()) }
    Dialog(onDismissRequest = beiAbbruch) {
        Column(Modifier.schwebendeKarte(farben, Masse.blattRadius).padding(16.dp)) {
            Text("Haftnotiz", style = Schriften.bildschirmtitel, color = farben.textStark)
            Spacer(Modifier.height(12.dp))
            Row {
                Haftfarben.forEach { wert ->
                    Box(
                        Modifier.padding(end = 8.dp)
                            .size(if (wert == farbe) 34.dp else 28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(wert))
                            .border(
                                if (wert == farbe) 2.dp else 1.dp,
                                if (wert == farbe) farben.akzent else farben.rand,
                                RoundedCornerShape(6.dp),
                            )
                            .clickable { farbe = wert },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp, Masse.gruppeRadius, 4.dp, 4.dp))
                    .background(Color(farbe))
                    .heightIn(min = 120.dp)
                    .padding(14.dp),
            ) {
                if (text.isEmpty()) {
                    Text("Kurze Notiz …", style = Schriften.notiztext, color = Color(0xFF8A8A8A))
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = Schriften.notiztext.copy(color = Color(0xFF1A1A1A)),
                    cursorBrush = SolidColor(Color(0xFF1A1A1A)),
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Blattknoepfe(
                beiAbbruch = beiAbbruch,
                bestaetigungAktiv = text.isNotBlank(),
                beiBestaetigen = {
                    beiFertig(
                        Anhang(
                            art = Anhangsart.HAFTNOTIZ,
                            name = text.trim().lineSequence().first().take(28).ifBlank { "Haftnotiz" },
                            text = text.trim(),
                            farbe = farbe.toInt(),
                        ),
                    )
                },
            )
        }
    }
}

// ------------------------------------------------------------------------ Tabelle

@Composable
private fun TabellenBlatt(beiAbbruch: () -> Unit, beiFertig: (Anhang) -> Unit) {
    val farben = Farben
    val zeilen = remember {
        mutableStateListOf(listOf("", "").toMutableStateList(), listOf("", "").toMutableStateList())
    }
    Dialog(onDismissRequest = beiAbbruch) {
        Column(Modifier.schwebendeKarte(farben, Masse.blattRadius).padding(16.dp)) {
            Text("Tabelle", style = Schriften.bildschirmtitel, color = farben.textStark)
            Text("Die erste Zeile ist die Kopfzeile.", style = Schriften.zeitstempel, color = farben.textSchwach)
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
            ) {
                zeilen.forEachIndexed { zeilennummer, zeile ->
                    Row {
                        zeile.forEachIndexed { spaltennummer, zelle ->
                            Box(
                                Modifier.padding(2.dp).width(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(farben.hintergrundErhoben)
                                    .border(1.dp, farben.rand, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                            ) {
                                if (zelle.isEmpty()) {
                                    Text(
                                        if (zeilennummer == 0) "Spalte ${spaltennummer + 1}" else "…",
                                        style = Schriften.einstellung, color = farben.textSchwach,
                                    )
                                }
                                BasicTextField(
                                    value = zelle,
                                    onValueChange = { eingabe ->
                                        zeilen[zeilennummer][spaltennummer] =
                                            eingabe.replace('\t', ' ').replace('\n', ' ')
                                    },
                                    singleLine = true,
                                    textStyle = Schriften.einstellung.copy(color = farben.textStark),
                                    cursorBrush = SolidColor(farben.akzent),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Zusatzknopf("Zeile") { zeilen.add(List(zeilen.first().size) { "" }.toMutableStateList()) }
                Spacer(Modifier.width(8.dp))
                Zusatzknopf("Spalte") { zeilen.forEach { it.add("") } }
            }
            Spacer(Modifier.height(14.dp))
            Blattknoepfe(
                beiAbbruch = beiAbbruch,
                bestaetigungAktiv = true,
                beiBestaetigen = {
                    val inhalt = zeilen.joinToString("\n") { zeile -> zeile.joinToString("\t") { it.trim() } }
                    beiFertig(Anhang(art = Anhangsart.TABELLE, name = "Tabelle", text = inhalt))
                },
            )
        }
    }
}

@Composable
private fun Zusatzknopf(text: String, beiDruck: () -> Unit) {
    val farben = Farben
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .border(1.dp, farben.rand, RoundedCornerShape(50))
            .clickable(onClick = beiDruck)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, null, Modifier.size(16.dp), tint = farben.textMittel)
        Spacer(Modifier.width(6.dp))
        Text(text, style = Schriften.knopf, color = farben.textMittel)
    }
}

// ------------------------------------------------------------------ Sprachaufnahme

@Composable
private fun AufnahmeBlatt(
    speicher: Anhangsspeicher,
    beiAbbruch: () -> Unit,
    beiFertig: (Anhang) -> Unit,
    beiFehler: (String) -> Unit,
) {
    val farben = Farben
    val ctx = LocalContext.current
    val datei = remember { speicher.neueDatei("-sprachaufnahme.m4a") }
    val geraet = remember { mutableStateOf<MediaRecorder?>(null) }
    var sekunden by remember { mutableStateOf(0) }
    var laeuft by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val aufnahme = runCatching {
            @Suppress("DEPRECATION")
            val neu = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
            neu.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(datei.absolutePath)
                prepare()
                start()
            }
        }.onFailure { beiFehler(it.message ?: "Die Aufnahme konnte nicht gestartet werden.") }.getOrNull()
        geraet.value = aufnahme
        laeuft = aufnahme != null
        if (aufnahme == null) beiAbbruch()
        onDispose {
            geraet.value?.let { runCatching { it.stop() }; runCatching { it.release() } }
            geraet.value = null
        }
    }
    LaunchedEffect(laeuft) {
        while (laeuft) { delay(1_000); sekunden += 1 }
    }

    Dialog(onDismissRequest = { }) {
        Column(Modifier.schwebendeKarte(farben, Masse.blattRadius).padding(20.dp)) {
            Text("Sprachaufnahme", style = Schriften.bildschirmtitel, color = farben.textStark)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Mic, null, Modifier.size(26.dp), tint = farben.akzent)
                Spacer(Modifier.width(12.dp))
                Text(laufzeitText(sekunden * 1000L), style = Schriften.geraetecode, color = farben.textStark)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Verwerfen",
                    style = Schriften.knopf,
                    color = farben.textMittel,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                        laeuft = false
                        geraet.value?.let { runCatching { it.stop() }; runCatching { it.release() } }
                        geraet.value = null
                        datei.delete()
                        beiAbbruch()
                    }.padding(horizontal = 14.dp, vertical = 10.dp),
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(farben.akzentGedeckt)
                        .border(1.5.dp, farben.akzent, RoundedCornerShape(50))
                        .clickable {
                            laeuft = false
                            val aufnahme = geraet.value
                            geraet.value = null
                            val geklappt = aufnahme != null && runCatching { aufnahme.stop() }.isSuccess
                            aufnahme?.let { runCatching { it.release() } }
                            if (geklappt && datei.length() > 0) {
                                beiFertig(speicher.beschreibe(datei, Anhangsart.SPRACHAUFNAHME, "Sprachaufnahme"))
                            } else {
                                datei.delete()
                                beiFehler("Die Aufnahme war zu kurz.")
                                beiAbbruch()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Stop, null, Modifier.size(18.dp), tint = farben.akzent)
                    Spacer(Modifier.width(6.dp))
                    Text("Beenden", style = Schriften.knopf, color = farben.akzent)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------- Helfer

/** Die zwei Knöpfe am Fuß jedes Anhangsblatts — überall gleich angeordnet. */
@Composable
private fun Blattknoepfe(beiAbbruch: () -> Unit, bestaetigungAktiv: Boolean, beiBestaetigen: () -> Unit) {
    val farben = Farben
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Abbrechen",
            style = Schriften.knopf,
            color = farben.textMittel,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = beiAbbruch)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Übernehmen",
            style = Schriften.knopf,
            color = if (bestaetigungAktiv) farben.akzent else farben.textSchwach,
            modifier = Modifier.clip(RoundedCornerShape(50))
                .background(if (bestaetigungAktiv) farben.akzentGedeckt else Color.Transparent)
                .border(1.5.dp, if (bestaetigungAktiv) farben.akzent else farben.rand, RoundedCornerShape(50))
                .clickable(enabled = bestaetigungAktiv, onClick = beiBestaetigen)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

fun Anhangsart.symbol(): ImageVector = when (this) {
    Anhangsart.PDF -> Icons.Outlined.PictureAsPdf
    Anhangsart.SPRACHAUFNAHME -> Icons.Outlined.Mic
    Anhangsart.BILD -> Icons.Outlined.Image
    Anhangsart.SCAN -> Icons.Outlined.DocumentScanner
    Anhangsart.AUDIO -> Icons.Outlined.MusicNote
    Anhangsart.ZEICHNUNG -> Icons.Outlined.Palette
    Anhangsart.HAFTNOTIZ -> Icons.Outlined.StickyNote2
    Anhangsart.TABELLE -> Icons.Outlined.TableChart
}

private fun laufzeitText(ms: Long): String {
    val sekunden = ms / 1000
    return "%d:%02d".format(sekunden / 60, sekunden % 60)
}

private fun Context.findeActivity(): Activity? {
    var lauf: Context? = this
    while (lauf is ContextWrapper) {
        if (lauf is Activity) return lauf
        lauf = lauf.baseContext
    }
    return null
}

private fun Context.oeffneAlsPdf(anhang: Anhang) {
    val datei = anhang.datei ?: return
    val uri = FileProvider.getUriForFile(this, "$packageName.dateien", datei)
    val absicht = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(absicht) }.onFailure { fehler ->
        if (fehler is ActivityNotFoundException) {
            runCatching {
                startActivity(Intent.createChooser(absicht, "PDF öffnen mit").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

/**
 * Lädt ein Bild verkleinert und außerhalb des Hauptfadens.
 *
 * Ohne die Verkleinerung reißt eine 12-Megapixel-Aufnahme in einer Liste den Speicher auf.
 */
@Composable
private fun merkeBild(pfad: String?, maxKante: Int = 1400): ImageBitmap? {
    var bild by remember(pfad) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pfad, maxKante) {
        bild = if (pfad.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching { ladeBild(pfad, maxKante)?.asImageBitmap() }.getOrNull()
        }
    }
    return bild
}

private fun ladeBild(pfad: String, maxKante: Int): android.graphics.Bitmap? {
    if (!File(pfad).exists()) return null
    val masse = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(pfad, masse)
    if (masse.outWidth <= 0) return null
    var faktor = 1
    while (masse.outWidth / faktor > maxKante || masse.outHeight / faktor > maxKante) faktor *= 2
    return BitmapFactory.decodeFile(pfad, BitmapFactory.Options().apply { inSampleSize = faktor })
}

package de.frank.gedankenspeicher.ui.verlauf

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

/** Standardbreite einer Tabellenspalte in dp. */
private const val SPALTE_STANDARD = 132
private const val SPALTE_MIN = 64
private const val SPALTE_MAX = 400

/** Feste Hoehe einer Tabellenzeile in dp — im Editor wie in der Karte dieselbe. */
private const val ZEILENHOEHE = 46

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
    beiZeichnung: () -> Unit,
    beiTabelle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = Farben
    val ctx = LocalContext.current
    val speicher = remember(ctx) { Anhangsspeicher(ctx.applicationContext) }
    val bereich = rememberCoroutineScope()
    val druck = merkeDruck()
    var menue by remember { mutableStateOf(false) }
    var haftnotiz by remember { mutableStateOf(false) }
    var sprachaufnahme by remember { mutableStateOf(false) }
    var kameradatei by remember { mutableStateOf<File?>(null) }
    var erkenntGerade by remember { mutableStateOf(false) }

    fun uebernimm(uri: Uri?, art: Anhangsart) {
        if (uri == null) return
        bereich.launch {
            runCatching { speicher.uebernimm(uri, art) }
                .onSuccess(beiAnhang)
                .onFailure { beiFehler(it.message ?: "Der Anhang konnte nicht übernommen werden.") }
        }
    }

    /** Aus einem Bild wird der reine Text — das ist der Sinn des Dokumentenscans. */
    fun erkenneText(quellen: List<Uri>) {
        if (quellen.isEmpty()) return
        erkenntGerade = true
        bereich.launch {
            try {
                val seiten = quellen.mapIndexedNotNull { nummer, uri ->
                    val text = runCatching { leseText(ctx, uri) }.getOrNull().orEmpty().trim()
                    if (text.isBlank()) null else if (quellen.size > 1) "— Seite ${nummer + 1} —\n$text" else text
                }
                if (seiten.isEmpty()) {
                    beiFehler("Auf der Vorlage war kein Text zu erkennen.")
                } else {
                    val text = seiten.joinToString("\n\n")
                    beiAnhang(
                        Anhang(
                            art = Anhangsart.SCAN,
                            name = text.lineSequence().first { it.isNotBlank() }.take(40).ifBlank { "Dokumentenscan" },
                            text = text,
                            seiten = quellen.size,
                        ),
                    )
                }
            } finally {
                erkenntGerade = false
            }
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
    // Ersatzweg für den Dokumentenscan: wenn der Scanner nicht startet, wird die Vorlage
    // schlicht abfotografiert und daraus derselbe Text gelesen.
    var scandatei by remember { mutableStateOf<File?>(null) }
    val scanKamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { geklappt ->
        val datei = scandatei
        scandatei = null
        if (geklappt && datei != null && datei.length() > 0) {
            erkenneText(listOf(Uri.fromFile(datei)))
        } else {
            datei?.delete()
        }
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { ergebnis ->
        val seiten = GmsDocumentScanningResult.fromActivityResultIntent(ergebnis.data)?.pages.orEmpty()
        if (ergebnis.resultCode == Activity.RESULT_OK && seiten.isNotEmpty()) {
            erkenneText(seiten.map { it.imageUri })
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
                    val ersatzweg = {
                        val datei = speicher.neueDatei("-scan.jpg")
                        scandatei = datei
                        runCatching {
                            scanKamera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.dateien", datei))
                        }.onFailure {
                            scandatei = null; datei.delete()
                            beiFehler("Der Dokumentenscan ist auf diesem Gerät nicht verfügbar.")
                        }
                        Unit
                    }
                    if (activity == null) {
                        ersatzweg()
                    } else {
                        val optionen = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(true)
                            .setPageLimit(20)
                            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .build()
                        GmsDocumentScanning.getClient(optionen).getStartScanIntent(activity)
                            .addOnSuccessListener { absender ->
                                runCatching { scanner.launch(IntentSenderRequest.Builder(absender).build()) }
                                    .onFailure { ersatzweg() }
                            }
                            .addOnFailureListener { ersatzweg() }
                    }
                },
                beiAudio = {
                    runCatching { audioWahl.launch(arrayOf("audio/*")) }
                        .onFailure { beiFehler("Es wurde keine Dateiauswahl gefunden.") }
                },
                beiZeichnung = beiZeichnung,
                beiHaftnotiz = { haftnotiz = true },
                beiTabelle = beiTabelle,
            )
        }
    }

    if (erkenntGerade) Texterkennungsblatt()
    if (haftnotiz) HaftnotizBlatt({ haftnotiz = false }) { anhang -> haftnotiz = false; beiAnhang(anhang) }
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
    beiSprachaufnahme: (() -> Unit)? = null,
    beiBild: () -> Unit,
    beiKamera: () -> Unit,
    beiScan: () -> Unit,
    beiAudio: () -> Unit,
    beiZeichnung: () -> Unit,
    beiHaftnotiz: () -> Unit,
    beiTabelle: () -> Unit,
    beiNachDerWahl: () -> Unit = beiSchliessen,
    /** Rechtsbündig am Plus der Karte — für den rechten Daumen, der dort wählt. */
    rechtsbuendig: Boolean = false,
) {
    val farben = Farben
    Popup(
        alignment = if (rechtsbuendig) Alignment.BottomEnd else Alignment.BottomStart,
        offset = androidx.compose.ui.unit.IntOffset(0, -160),
        onDismissRequest = beiSchliessen,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                // Rechtsbündig ist die Blase genau so breit wie ihr längster Eintrag
                // („Dokumentenscan") — kein Band über den ganzen Bildschirm.
                .then(
                    if (rechtsbuendig) Modifier.width(IntrinsicSize.Max)
                    else Modifier.widthIn(min = 230.dp)
                )
                .schwebendeKarte(farben, Masse.gruppeRadius)
                .padding(vertical = 8.dp),
        ) {
            Menuezeile("PDF", Icons.Outlined.PictureAsPdf) { beiNachDerWahl(); beiPdf() }
            // Die Sprachaufnahme gibt es nur im Entwurf — an einer fertigen Notiz ist das
            // Mikrofon ohnehin sichtbar, der Eintrag wäre doppelt.
            if (beiSprachaufnahme != null) {
                Menuezeile("Sprachaufnahme", Icons.Outlined.Mic) { beiNachDerWahl(); beiSprachaufnahme() }
            }
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth().height(1.dp).background(farben.rand),
            )
            Menuezeile("Bild", Icons.Outlined.Image) { beiNachDerWahl(); beiBild() }
            Menuezeile("Kamera", Icons.Outlined.PhotoCamera) { beiNachDerWahl(); beiKamera() }
            Menuezeile("Dokumentenscan", Icons.Outlined.DocumentScanner) { beiNachDerWahl(); beiScan() }
            Menuezeile("Audiodatei", Icons.Outlined.MusicNote) { beiNachDerWahl(); beiAudio() }
            Menuezeile("Zeichnung", Icons.Outlined.Palette) { beiNachDerWahl(); beiZeichnung() }
            Menuezeile("Haftnotiz", Icons.Outlined.StickyNote2) { beiNachDerWahl(); beiHaftnotiz() }
            Menuezeile("Tabelle", Icons.Outlined.TableChart) { beiNachDerWahl(); beiTabelle() }
        }
    }
}

// ------------------------------------------------------- Anhänge an einer fertigen Notiz

/**
 * Das Plus-Menü **an einer gespeicherten Notiz** (der Plus-Knopf in ihrer Aktionsreihe).
 *
 * Dieselben Wege wie der Entwurfs-Anhangsknopf — PDF, Bild, Kamera, Dokumentenscan,
 * Audiodatei, Zeichnung, Haftnotiz und Tabelle; nur die Sprachaufnahme fehlt, weil das
 * Mikrofon an der Karte ohnehin sichtbar ist. Was hier fertig wird, hängt direkt an der
 * Notiz statt am Entwurf.
 */
@Composable
fun NotizAnhangsmenue(
    beiSchliessen: () -> Unit,
    beiAnhang: (Anhang) -> Unit,
    beiFehler: (String) -> Unit,
    beiZeichnung: () -> Unit,
    beiTabelle: () -> Unit,
) {
    val farben = Farben
    val ctx = LocalContext.current
    val speicher = remember(ctx) { Anhangsspeicher(ctx.applicationContext) }
    val bereich = rememberCoroutineScope()
    var haftnotiz by remember { mutableStateOf(false) }
    var kameradatei by remember { mutableStateOf<File?>(null) }
    var erkenntGerade by remember { mutableStateOf(false) }

    /** Ein Fehler wird gemeldet **und** macht das Menü zu — hier gibt es nichts mehr zu holen. */
    fun meld(text: String) {
        beiFehler(text)
        beiSchliessen()
    }

    fun uebernimm(uri: Uri?, art: Anhangsart) {
        if (uri == null) return
        bereich.launch {
            runCatching { speicher.uebernimm(uri, art) }
                .onSuccess(beiAnhang)
                .onFailure { meld(it.message ?: "Der Anhang konnte nicht übernommen werden.") }
        }
    }

    /** Aus einem Bild wird der reine Text — das ist der Sinn des Dokumentenscans. */
    fun erkenneText(quellen: List<Uri>) {
        if (quellen.isEmpty()) return
        erkenntGerade = true
        bereich.launch {
            try {
                val seiten = quellen.mapIndexedNotNull { nummer, uri ->
                    val text = runCatching { leseText(ctx, uri) }.getOrNull().orEmpty().trim()
                    if (text.isBlank()) null else if (quellen.size > 1) "— Seite ${nummer + 1} —\n$text" else text
                }
                if (seiten.isEmpty()) {
                    meld("Auf der Vorlage war kein Text zu erkennen.")
                } else {
                    val text = seiten.joinToString("\n\n")
                    beiAnhang(
                        Anhang(
                            art = Anhangsart.SCAN,
                            name = text.lineSequence().first { it.isNotBlank() }.take(40).ifBlank { "Dokumentenscan" },
                            text = text,
                            seiten = quellen.size,
                        ),
                    )
                }
            } finally {
                erkenntGerade = false
            }
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
    // Ersatzweg für den Dokumentenscan: wenn der Scanner nicht startet, wird die Vorlage
    // schlicht abfotografiert und daraus derselbe Text gelesen.
    var scandatei by remember { mutableStateOf<File?>(null) }
    val scanKamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { geklappt ->
        val datei = scandatei
        scandatei = null
        if (geklappt && datei != null && datei.length() > 0) {
            erkenneText(listOf(Uri.fromFile(datei)))
        } else {
            datei?.delete()
        }
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { ergebnis ->
        val seiten = GmsDocumentScanningResult.fromActivityResultIntent(ergebnis.data)?.pages.orEmpty()
        if (ergebnis.resultCode == Activity.RESULT_OK && seiten.isNotEmpty()) {
            erkenneText(seiten.map { it.imageUri })
        }
    }

    Anhangsmenue(
        beiSchliessen = beiSchliessen,
        // Bewusst **nicht** zuklappen, wenn ein Punkt gewählt wird: Das Schließen nimmt
        // diese Composable aus dem Baum — und mit ihr die Koroutine der Übernahme und die
        // Registrierung des Datei-Wählers. Die Auswahl lief dann ins Leere. Zugemacht wird
        // erst, wenn der Anhang wirklich da ist (oder ein Fehler gemeldet wurde).
        beiNachDerWahl = { },
        rechtsbuendig = true,
        beiPdf = {
            runCatching { pdfWahl.launch(arrayOf("application/pdf")) }
                .onFailure { meld("Es wurde keine Dateiauswahl gefunden.") }
        },
        beiBild = {
            runCatching { bildWahl.launch(arrayOf("image/*")) }
                .onFailure { meld("Es wurde keine Bildauswahl gefunden.") }
        },
        beiKamera = {
            val datei = speicher.neueDatei(".jpg")
            kameradatei = datei
            runCatching {
                kamera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.dateien", datei))
            }.onFailure {
                kameradatei = null; datei.delete(); meld("Es wurde keine Kamera-App gefunden.")
            }
        },
        beiScan = {
            val activity = ctx.findeActivity()
            val ersatzweg = {
                val datei = speicher.neueDatei("-scan.jpg")
                scandatei = datei
                runCatching {
                    scanKamera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.dateien", datei))
                }.onFailure {
                    scandatei = null; datei.delete()
                    meld("Der Dokumentenscan ist auf diesem Gerät nicht verfügbar.")
                }
                Unit
            }
            if (activity == null) {
                ersatzweg()
            } else {
                val optionen = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setPageLimit(20)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .build()
                GmsDocumentScanning.getClient(optionen).getStartScanIntent(activity)
                    .addOnSuccessListener { absender ->
                        runCatching { scanner.launch(IntentSenderRequest.Builder(absender).build()) }
                            .onFailure { ersatzweg() }
                    }
                    .addOnFailureListener { ersatzweg() }
            }
        },
        beiAudio = {
            runCatching { audioWahl.launch(arrayOf("audio/*")) }
                .onFailure { meld("Es wurde keine Dateiauswahl gefunden.") }
        },
        beiZeichnung = beiZeichnung,
        beiHaftnotiz = { haftnotiz = true },
        beiTabelle = beiTabelle,
    )

    if (erkenntGerade) Texterkennungsblatt()
    if (haftnotiz) HaftnotizBlatt({ haftnotiz = false }) { anhang -> haftnotiz = false; beiAnhang(anhang) }
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

/** Solange die Texterkennung läuft, bleibt der Bildschirm nicht stumm. */
@Composable
private fun Texterkennungsblatt() {
    val farben = Farben
    Dialog(onDismissRequest = { }) {
        Row(
            Modifier.schwebendeKarte(farben, Masse.blattRadius).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.DocumentScanner, null, Modifier.size(24.dp), tint = farben.akzent)
            Spacer(Modifier.width(14.dp))
            Text("Text wird gelesen …", style = Schriften.einstellung, color = farben.textStark)
        }
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

/**
 * Die Anhänge einer fertigen Notiz, unter ihrem Text.
 *
 * [beiTitel] ist gesetzt, wenn der Anhang umbenannt werden darf — bei einer gespeicherten
 * Notiz also, nicht im Entwurf.
 */
@Composable
fun Anhangsliste(
    anhaenge: List<Anhang>,
    modifier: Modifier = Modifier,
    beiTitel: ((Anhang, String) -> Unit)? = null,
) {
    if (anhaenge.isEmpty()) return
    val ctx = LocalContext.current
    var vollbild by remember { mutableStateOf<Anhang?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        anhaenge.forEach { anhang ->
            when {
                anhang.art == Anhangsart.PDF -> Pdfanhang(anhang) { ctx.oeffneAlsPdf(anhang) }
                anhang.art == Anhangsart.SPRACHAUFNAHME || anhang.art == Anhangsart.AUDIO ->
                    Tonanhang(anhang, beiTitel)
                anhang.art == Anhangsart.HAFTNOTIZ -> Haftnotizanhang(anhang)
                anhang.art == Anhangsart.TABELLE -> Tabellenanhang(anhang)
                // Ein Scan ist seit der Texterkennung Text; ältere Scans sind noch Bilder.
                anhang.art == Anhangsart.SCAN && anhang.datei == null -> Scananhang(anhang)
                else -> Bildanhang(anhang) { vollbild = anhang }
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
            // `Fit` statt `FillWidth`: ein querformatiges Bild soll quer bleiben und
            // nicht auf Kartenbreite hochgezogen und oben und unten abgeschnitten werden.
            Image(
                bitmap = bild,
                contentDescription = anhang.name.ifBlank { anhang.beschriftung },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            )
        }
    }
}

/** Der Dokumentenscan: erkannter Text, im Umbruch der Vorlage. */
@Composable
private fun Scananhang(anhang: Anhang) {
    val farben = Farben
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Masse.gruppeRadius))
            .background(farben.hintergrundGlas)
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.DocumentScanner, null, Modifier.size(18.dp), tint = farben.akzent)
            Spacer(Modifier.width(8.dp))
            Text(
                if (anhang.seiten > 1) "Dokumentenscan · ${anhang.seiten} Seiten" else "Dokumentenscan",
                style = Schriften.zeitstempel,
                color = farben.textSchwach,
            )
        }
        Spacer(Modifier.height(8.dp))
        SelectionContainer {
            Text(anhang.text, style = Schriften.notiztext, color = farben.textStark)
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
                contentScale = ContentScale.Fit,
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

/**
 * Sprachaufnahme und Audiodatei: Abspielen, Verlaufsbalken zum Spulen und — bei einer
 * gespeicherten Notiz — ein Stift für den eigenen Titel.
 */
@Composable
private fun Tonanhang(anhang: Anhang, beiTitel: ((Anhang, String) -> Unit)?) {
    val farben = Farben
    var spielt by remember { mutableStateOf(false) }
    var stelle by remember { mutableStateOf(0f) }
    var schiebtGerade by remember { mutableStateOf(false) }
    var titelBlatt by remember { mutableStateOf(false) }
    val spieler = remember { mutableStateOf<MediaPlayer?>(null) }
    val gesamt = anhang.dauerMs.coerceAtLeast(1L).toFloat()

    DisposableEffect(anhang.id) {
        onDispose { spieler.value?.release(); spieler.value = null }
    }
    // Der Balken läuft mit, solange gespielt wird — aber nicht, während der Finger zieht.
    LaunchedEffect(spielt) {
        while (spielt) {
            delay(120)
            if (!schiebtGerade) spieler.value?.let { stelle = it.currentPosition.toFloat() }
        }
    }

    /** Der eigene Titel steht unter der Art, mit Gedankenstrich davor. */
    val eigenerTitel = anhang.name.trim()
        .takeIf { it.isNotBlank() && !it.equals(anhang.beschriftung, ignoreCase = true) }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Masse.gruppeRadius))
            .background(farben.hintergrundGlas)
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(Masse.kartenSymbolFlaeche).clip(RoundedCornerShape(50))
                    .clickable {
                        val laufend = spieler.value
                        if (spielt && laufend != null) {
                            stelle = laufend.currentPosition.toFloat()
                            laufend.pause()
                            spielt = false
                        } else {
                            val vorhanden = spieler.value
                            if (vorhanden != null) {
                                vorhanden.start(); spielt = true
                            } else {
                                val datei = anhang.datei ?: return@clickable
                                spieler.value = runCatching {
                                    MediaPlayer().apply {
                                        setDataSource(datei.absolutePath)
                                        setOnCompletionListener {
                                            spielt = false
                                            stelle = 0f
                                            runCatching { seekTo(0) }
                                        }
                                        prepare()
                                        if (stelle > 0f) seekTo(stelle.toInt())
                                        start()
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
            Column(Modifier.weight(1f)) {
                Text(anhang.beschriftung, style = Schriften.einstellung, color = farben.textStark)
                if (eigenerTitel != null) {
                    Text(
                        "— $eigenerTitel",
                        style = Schriften.notiztext,
                        color = farben.textMittel,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (beiTitel != null) {
                Box(
                    Modifier.size(Masse.kartenSymbolFlaeche).clip(RoundedCornerShape(50))
                        .clickable { titelBlatt = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Edit, "Titel bearbeiten", Modifier.size(Masse.kartenSymbol), tint = farben.textMittel)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Verlaufsbalken(
            stelle = stelle,
            gesamt = gesamt,
            beiZiehenBeginn = { schiebtGerade = true },
            beiZiehen = { wert -> stelle = wert },
            beiZiehenEnde = { wert ->
                schiebtGerade = false
                stelle = wert
                spieler.value?.let { runCatching { it.seekTo(wert.toInt()) } }
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(laufzeitText(stelle.toLong()), style = Schriften.zeitstempel, color = farben.textSchwach)
            Text(laufzeitText(anhang.dauerMs), style = Schriften.zeitstempel, color = farben.textSchwach)
        }
    }

    if (titelBlatt && beiTitel != null) {
        Titelblatt(
            vorhanden = eigenerTitel.orEmpty(),
            beiAbbruch = { titelBlatt = false },
            beiFertig = { titel -> titelBlatt = false; beiTitel(anhang, titel) },
        )
    }
}

/** Der Verlaufsbalken der Wiedergabe — antippen springt, ziehen spult. */
@Composable
private fun Verlaufsbalken(
    stelle: Float,
    gesamt: Float,
    beiZiehenBeginn: () -> Unit,
    beiZiehen: (Float) -> Unit,
    beiZiehenEnde: (Float) -> Unit,
) {
    val farben = Farben
    var breite by remember { mutableStateOf(1) }
    val anteil = (stelle / gesamt).coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxWidth().height(28.dp)
            .onSizeChanged { breite = it.width.coerceAtLeast(1) }
            .pointerInput(gesamt) {
                detectTapGestures { punkt -> beiZiehenEnde((punkt.x / breite).coerceIn(0f, 1f) * gesamt) }
            }
            .pointerInput(gesamt) {
                // Während des Ziehens läuft nur der Balken mit; gespult wird erst beim
                // Loslassen — sonst ruckelt die Wiedergabe bei jedem Zwischenschritt.
                var letzte = 0f
                detectDragGestures(
                    onDragStart = { punkt ->
                        beiZiehenBeginn()
                        letzte = (punkt.x / breite).coerceIn(0f, 1f) * gesamt
                        beiZiehen(letzte)
                    },
                    onDragEnd = { beiZiehenEnde(letzte) },
                    onDragCancel = { beiZiehenEnde(letzte) },
                    onDrag = { aenderung, _ ->
                        aenderung.consume()
                        letzte = (aenderung.position.x / breite).coerceIn(0f, 1f) * gesamt
                        beiZiehen(letzte)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(farben.rand))
        Box(
            Modifier.fillMaxWidth(anteil).height(4.dp).clip(RoundedCornerShape(50)).background(farben.akzent),
        )
        Box(
            Modifier.fillMaxWidth(anteil).fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(Modifier.size(13.dp).clip(RoundedCornerShape(50)).background(farben.akzent))
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

/** Die fertige Tabelle — mit den Spaltenbreiten, die im Editor eingestellt wurden. */
@Composable
private fun Tabellenanhang(anhang: Anhang) {
    val farben = Farben
    val zeilen = remember(anhang.text) {
        anhang.text.split("\n").map { it.split("\t") }
    }
    if (zeilen.isEmpty()) return
    val spalten = zeilen.maxOf { it.size }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Masse.gruppeRadius))
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
            .horizontalScroll(rememberScrollState()),
    ) {
        zeilen.forEachIndexed { nummer, zeile ->
            // Dieselbe feste Zeilenhoehe wie im Editor — so sieht die gespeicherte
            // Tabelle genauso aus wie beim Bearbeiten, und die Intrinsic-Messung,
            // die im Vollbild-Editor den Fuss verdraengt hat, entfaellt auch hier.
            Row(
                Modifier.background(if (nummer == 0) farben.hintergrundErhoben else Color.Transparent)
                    .height(ZEILENHOEHE.dp),
            ) {
                (0 until spalten).forEach { spalte ->
                    Box(
                        Modifier
                            .width((anhang.spaltenbreiten.getOrNull(spalte) ?: SPALTE_STANDARD).dp)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            zeile.getOrNull(spalte).orEmpty(),
                            style = if (nummer == 0) Schriften.kartenUeberschrift else Schriften.einstellung,
                            color = if (nummer == 0) farben.textStark else farben.textMittel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (spalte < spalten - 1) {
                        Box(Modifier.width(1.dp).fillMaxHeight().background(farben.rand))
                    }
                }
            }
            if (nummer < zeilen.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))
            }
        }
    }
}

// ------------------------------------------------------------------ Bild im Vollbild

/**
 * Ein Bild bildschirmfüllend, mit zwei Fingern zoombar und verschiebbar; ein Doppeltipp
 * springt zwischen ganzer Ansicht und zweieinhalbfacher Vergrösserung.
 */
@Composable
private fun Bildschau(anhang: Anhang, beiSchliessen: () -> Unit) {
    val bild = merkeBild(anhang.pfad, 3000)
    var skala by remember { mutableStateOf(1f) }
    var versatz by remember { mutableStateOf(Offset.Zero) }
    Dialog(
        onDismissRequest = beiSchliessen,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            bild?.let {
                Image(
                    bitmap = it,
                    contentDescription = anhang.name.ifBlank { anhang.beschriftung },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, verschiebung, vergroesserung, _ ->
                                skala = (skala * vergroesserung).coerceIn(1f, 8f)
                                versatz = if (skala <= 1.01f) Offset.Zero else versatz + verschiebung
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (skala > 1.01f) { skala = 1f; versatz = Offset.Zero } else skala = 2.5f
                                },
                            )
                        }
                        .graphicsLayer(
                            scaleX = skala, scaleY = skala,
                            translationX = versatz.x, translationY = versatz.y,
                        ),
                )
            }
            Box(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = beiSchliessen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Close, "Schließen", Modifier.size(22.dp), tint = Color.White)
            }
        }
    }
}

// ---------------------------------------------------------------------- Zeichnung

private data class Strich(val punkte: SnapshotStateList<Offset>, val farbe: Long, val staerke: Float)

/** Die Zeichenfläche füllt den Bildschirm — auf einem Briefmarkenfeld zeichnet niemand. */
@Composable
fun ZeichenBlatt(
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

    VollbildBlatt(
            kopf = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zeichnung", style = Schriften.bildschirmtitel, color = farben.textStark)
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
            },
            fuss = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Stiftfarben.forEach { wert ->
                            Box(
                                Modifier.padding(end = 8.dp)
                                    .size(if (wert == farbe) 32.dp else 26.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(wert))
                                    .border(if (wert == farbe) 2.dp else 0.dp, farben.akzent, RoundedCornerShape(50))
                                    .clickable { farbe = wert },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        listOf(3f, 6f, 12f, 20f).forEach { wert ->
                            Box(
                                Modifier.padding(start = 10.dp).size((wert + 14).dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (wert == staerke) farben.akzent else farben.textSchwach)
                                    .clickable { staerke = wert },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
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
            },
        ) { platz ->
            Box(
                platz
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

/**
 * Der Tabelleneditor — auch zum Nachbearbeiten einer schon gespeicherten Tabelle.
 *
 * Neue Zeilen und Spalten kommen über das Plus am Rand dazu; die Trennlinien zwischen den
 * Spalten lassen sich ziehen wie in einer Tabellenkalkulation.
 */
@Composable
fun TabellenBlatt(vorlage: Anhang?, beiAbbruch: () -> Unit, beiFertig: (Anhang) -> Unit) {
    val farben = Farben
    val dichte = LocalDensity.current
    val zeilen = remember(vorlage?.id) {
        val eingelesen = vorlage?.text?.split("\n")?.map { it.split("\t") }?.filter { it.isNotEmpty() }
        if (eingelesen.isNullOrEmpty()) {
            mutableStateListOf(listOf("", "").toMutableStateList(), listOf("", "").toMutableStateList())
        } else {
            val spalten = eingelesen.maxOf { it.size }
            eingelesen.map { zeile ->
                List(spalten) { stelle -> zeile.getOrNull(stelle).orEmpty() }.toMutableStateList()
            }.toMutableStateList()
        }
    }
    val breiten = remember(vorlage?.id) {
        val spalten = zeilen.first().size
        List(spalten) { stelle ->
            (vorlage?.spaltenbreiten?.getOrNull(stelle) ?: SPALTE_STANDARD).toFloat()
        }.toMutableStateList()
    }

    VollbildBlatt(
            kopf = {
                Column {
                    Text(
                        if (vorlage == null) "Tabelle" else "Tabelle bearbeiten",
                        style = Schriften.bildschirmtitel, color = farben.textStark,
                    )
                    Text(
                        "Erste Zeile ist die Kopfzeile · Trennlinien ziehen ändert die Spaltenbreite",
                        style = Schriften.zeitstempel, color = farben.textSchwach,
                    )
                }
            },
            fuss = {
                Blattknoepfe(
                    beiAbbruch = beiAbbruch,
                    bestaetigungAktiv = true,
                    beiBestaetigen = {
                        val inhalt = zeilen.joinToString("\n") { zeile -> zeile.joinToString("\t") { it.trim() } }
                        beiFertig(
                            (vorlage ?: Anhang(art = Anhangsart.TABELLE, name = "Tabelle")).copy(
                                art = Anhangsart.TABELLE,
                                name = "Tabelle",
                                text = inhalt,
                                spaltenbreiten = breiten.map { it.toInt() },
                            ),
                        )
                    },
                )
            },
        ) { flaeche ->
            Column(flaeche) {
                Row(Modifier.weight(1f)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            Column {
                                zeilen.forEachIndexed { zeilennummer, zeile ->
                                    // Feste Zeilenhöhe statt `IntrinsicSize.Min`: eine Messung
                                    // nach der Wunschhöhe schlägt aus einem scrollbaren Bereich
                                    // heraus auf das ganze Blatt durch und drückt den Fuß hinaus.
                                    Row(Modifier.height(ZEILENHOEHE.dp)) {
                                        zeile.indices.forEach { spalte ->
                                            Box(
                                                Modifier
                                                    .width(breiten.getOrElse(spalte) { SPALTE_STANDARD.toFloat() }.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        if (zeilennummer == 0) farben.hintergrundErhoben
                                                        else farben.hintergrundGlas,
                                                    )
                                                    .border(1.dp, farben.rand)
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.CenterStart,
                                            ) {
                                                if (zeile[spalte].isEmpty()) {
                                                    Text(
                                                        if (zeilennummer == 0) "Spalte ${spalte + 1}" else "",
                                                        style = Schriften.einstellung, color = farben.textSchwach,
                                                    )
                                                }
                                                BasicTextField(
                                                    value = zeile[spalte],
                                                    onValueChange = { eingabe ->
                                                        zeilen[zeilennummer][spalte] =
                                                            eingabe.replace('\t', ' ').replace('\n', ' ')
                                                    },
                                                    singleLine = true,
                                                    textStyle = (
                                                        if (zeilennummer == 0) Schriften.kartenUeberschrift
                                                        else Schriften.einstellung
                                                        ).copy(color = farben.textStark),
                                                    cursorBrush = SolidColor(farben.akzent),
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                            // Der Griff auf der Trennlinie: ziehen macht die
                                            // Spalte breiter oder schmaler.
                                            Box(
                                                Modifier.width(10.dp).fillMaxHeight()
                                                    .pointerInput(spalte) {
                                                        detectDragGestures { aenderung, verschiebung ->
                                                            aenderung.consume()
                                                            val schritt = with(dichte) { verschiebung.x.toDp().value }
                                                            breiten[spalte] = (breiten[spalte] + schritt)
                                                                .coerceIn(SPALTE_MIN.toFloat(), SPALTE_MAX.toFloat())
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Box(
                                                    Modifier.width(3.dp).fillMaxHeight()
                                                        .background(farben.akzent.copy(alpha = 0.35f)),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Rechts: Spalte hinzu oder weg.
                    Column(
                        Modifier.padding(start = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Randknopf(Icons.Outlined.Add, "Spalte hinzufügen") {
                            zeilen.forEach { it.add("") }
                            breiten.add(SPALTE_STANDARD.toFloat())
                        }
                        Randknopf(Icons.Outlined.Remove, "Letzte Spalte entfernen") {
                            if (zeilen.first().size > 1) {
                                zeilen.forEach { it.removeAt(it.lastIndex) }
                                breiten.removeAt(breiten.lastIndex)
                            }
                        }
                    }
                }
                // Unten: Zeile hinzu oder weg.
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Randknopf(Icons.Outlined.Add, "Zeile hinzufügen") {
                        zeilen.add(List(zeilen.first().size) { "" }.toMutableStateList())
                    }
                    Randknopf(Icons.Outlined.Remove, "Letzte Zeile entfernen") {
                        if (zeilen.size > 1) zeilen.removeAt(zeilen.lastIndex)
                    }
                }
            }
        }
}

/**
 * Das Gerüst der Vollbild-Blätter (Zeichnung, Tabelle).
 *
 * Die Fußleiste liegt **nicht** im Layoutfluss, sondern klebt über [Box]-Ausrichtung am
 * unteren Rand; der Inhalt bekommt darüber den Rest. Damit kann kein noch so hoher Inhalt
 * die Knöpfe aus dem Bild schieben — genau das war der Fehler, als die Leiste als letztes
 * Kind einer Column hing und eine Innenmessung mehr Höhe beanspruchte als der Bildschirm hat.
 */
@Composable
private fun VollbildBlatt(
    kopf: @Composable () -> Unit,
    fuss: @Composable () -> Unit,
    inhalt: @Composable (Modifier) -> Unit,
) {
    val farben = Farben
    var fusshoehe by remember { mutableStateOf(0) }
    val dichte = LocalDensity.current
    Box(
        Modifier.fillMaxSize().background(farben.hintergrund)
            .statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            kopf()
            Spacer(Modifier.height(12.dp))
            inhalt(
                Modifier.fillMaxWidth().weight(1f)
                    .padding(bottom = with(dichte) { fusshoehe.toDp() }),
            )
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(farben.hintergrund)
                .onSizeChanged { fusshoehe = it.height }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            fuss()
        }
    }
}

@Composable
private fun Randknopf(symbol: ImageVector, beschreibung: String, beiDruck: () -> Unit) {
    val farben = Farben
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(50))
            .border(1.dp, farben.rand, RoundedCornerShape(50))
            .background(farben.hintergrundErhoben)
            .clickable(onClick = beiDruck),
        contentAlignment = Alignment.Center,
    ) {
        Icon(symbol, beschreibung, Modifier.size(20.dp), tint = farben.textMittel)
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

/** Der eigene Titel einer Sprachaufnahme — mehrzeilig erlaubt. */
@Composable
private fun Titelblatt(vorhanden: String, beiAbbruch: () -> Unit, beiFertig: (String) -> Unit) {
    val farben = Farben
    var text by remember { mutableStateOf(vorhanden) }
    Dialog(onDismissRequest = beiAbbruch) {
        Column(Modifier.schwebendeKarte(farben, Masse.blattRadius).padding(16.dp)) {
            Text("Titel der Sprachaufnahme", style = Schriften.bildschirmtitel, color = farben.textStark)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(Masse.eingabeRadius))
                    .background(farben.hintergrundErhoben)
                    .border(1.dp, farben.rand, RoundedCornerShape(Masse.eingabeRadius))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (text.isEmpty()) {
                    Text("Worum geht es in der Aufnahme?", style = Schriften.notiztext, color = farben.textSchwach)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = Schriften.notiztext.copy(color = farben.textStark),
                    cursorBrush = SolidColor(farben.akzent),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Blattknoepfe(
                beiAbbruch = beiAbbruch,
                bestaetigungAktiv = true,
                beiBestaetigen = { beiFertig(text.trim()) },
            )
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
    val sekunden = (ms / 1000).coerceAtLeast(0)
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
 * Liest den Text einer Vorlage — die Texterkennung von ML Kit läuft auf dem Gerät.
 *
 * Die Blöcke werden von oben nach unten zusammengesetzt, damit der Umbruch der Vorlage
 * erhalten bleibt und nicht alles zu einem Fließtext verschmilzt.
 */
private suspend fun leseText(ctx: Context, uri: Uri): String {
    val eingabe = withContext(Dispatchers.IO) { InputImage.fromFilePath(ctx, uri) }
    val erkenner = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return suspendCancellableCoroutine { fortsetzung ->
        erkenner.process(eingabe)
            .addOnSuccessListener { ergebnis ->
                val text = ergebnis.textBlocks
                    .sortedBy { it.boundingBox?.top ?: 0 }
                    .joinToString("\n\n") { block ->
                        block.lines
                            .sortedBy { it.boundingBox?.top ?: 0 }
                            .joinToString("\n") { it.text }
                    }
                fortsetzung.resume(text)
            }
            .addOnFailureListener { fortsetzung.resume("") }
    }
}

/**
 * Lädt ein Bild verkleinert, gedreht und außerhalb des Hauptfadens.
 *
 * Ohne die Verkleinerung reißt eine 12-Megapixel-Aufnahme in einer Liste den Speicher auf;
 * ohne die EXIF-Drehung liegt jede Hochformat-Aufnahme quer, weil die Kamera den Sensor
 * nicht dreht, sondern nur vermerkt, wie das Bild gemeint war.
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
    val roh = BitmapFactory.decodeFile(pfad, BitmapFactory.Options().apply { inSampleSize = faktor })
        ?: return null
    return drehNachExif(roh, pfad)
}

/** Wendet die im Bild vermerkte Ausrichtung wirklich an. */
private fun drehNachExif(bild: android.graphics.Bitmap, pfad: String): android.graphics.Bitmap {
    val ausrichtung = runCatching {
        ExifInterface(pfad).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    if (ausrichtung == ExifInterface.ORIENTATION_NORMAL || ausrichtung == ExifInterface.ORIENTATION_UNDEFINED) {
        return bild
    }
    val matrix = Matrix()
    when (ausrichtung) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        else -> return bild
    }
    return runCatching {
        android.graphics.Bitmap.createBitmap(bild, 0, 0, bild.width, bild.height, matrix, true)
            .also { gedreht -> if (gedreht !== bild) bild.recycle() }
    }.getOrDefault(bild)
}

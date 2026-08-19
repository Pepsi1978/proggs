package de.frank.denknotiz.ui

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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import de.frank.denknotiz.data.Attachment
import de.frank.denknotiz.data.AttachmentKind
import de.frank.denknotiz.data.AttachmentStore
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Farbauswahl der Haftnotiz, angelehnt an die Haftnotizfarben der Samsung-Notizen. */
private val HaftnotizFarben = listOf(0xFFFFF3B0, 0xFFFFD6C9, 0xFFCDEBC5, 0xFFC9E4FF, 0xFFE6D6FF, 0xFFF2F2F2)

/** Stiftfarben der Zeichenfläche. */
private val StiftFarben = listOf(0xFF1A1A1A, 0xFFD32F2F, 0xFF1565C0, 0xFF2E7D32, 0xFFF9A825, 0xFF6A1B9A)

// ---------------------------------------------------------------- Plus-Menü

/**
 * Das Plus-Symbol neben dem Verbessern-Knopf: öffnet das Anhangsmenü und liefert
 * den fertig abgelegten Anhang über [onAttachment] zurück.
 */
@Composable
fun AttachmentMenuButton(
    onAttachment: (Attachment) -> Unit,
    onError: (String) -> Unit,
    requestMicrophone: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember(context) { AttachmentStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var zeichnung by remember { mutableStateOf(false) }
    var haftnotiz by remember { mutableStateOf(false) }
    var tabelle by remember { mutableStateOf(false) }
    var sprachaufnahme by remember { mutableStateOf(false) }
    var kameraDatei by remember { mutableStateOf<File?>(null) }

    fun importiere(uri: Uri?, kind: AttachmentKind) {
        if (uri == null) return
        scope.launch {
            runCatching { store.importDocument(uri, kind) }
                .onSuccess(onAttachment)
                .onFailure { onError(it.message ?: "Der Anhang konnte nicht übernommen werden.") }
        }
    }

    val pdfWahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importiere(uri, AttachmentKind.PDF)
    }
    val bildWahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importiere(uri, AttachmentKind.IMAGE)
    }
    val audioWahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importiere(uri, AttachmentKind.AUDIO)
    }
    val kamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { erfolgreich ->
        val datei = kameraDatei
        kameraDatei = null
        if (erfolgreich && datei != null && datei.length() > 0) {
            onAttachment(store.describe(datei, AttachmentKind.IMAGE, "Kameraaufnahme"))
        } else {
            datei?.delete()
        }
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { ergebnis ->
        val seiten = GmsDocumentScanningResult.fromActivityResultIntent(ergebnis.data)?.pages.orEmpty()
        if (ergebnis.resultCode == Activity.RESULT_OK && seiten.isNotEmpty()) {
            scope.launch {
                seiten.forEachIndexed { index, seite ->
                    runCatching { store.importDocument(seite.imageUri, AttachmentKind.SCAN) }
                        .onSuccess { anhang -> onAttachment(anhang.copy(name = "Scan Seite ${index + 1}")) }
                        .onFailure { onError(it.message ?: "Der Scan konnte nicht übernommen werden.") }
                }
            }
        }
    }

    Box(modifier) {
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Default.Add, "Anhang hinzufügen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(20.dp),
        ) {
            MenuZeile("PDF", Icons.Default.PictureAsPdf) {
                menu = false
                runCatching { pdfWahl.launch(arrayOf("application/pdf")) }
                    .onFailure { onError("Es wurde keine Dateiauswahl gefunden.") }
            }
            MenuZeile("Sprachaufnahme", Icons.Default.Mic) {
                menu = false
                requestMicrophone { sprachaufnahme = true }
            }
            Divider(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
            MenuZeile("Bild", Icons.Default.Image) {
                menu = false
                runCatching { bildWahl.launch(arrayOf("image/*")) }
                    .onFailure { onError("Es wurde keine Bildauswahl gefunden.") }
            }
            MenuZeile("Kamera", Icons.Default.PhotoCamera) {
                menu = false
                val datei = store.newFile(".jpg")
                kameraDatei = datei
                runCatching {
                    kamera.launch(FileProvider.getUriForFile(context, "${context.packageName}.dateien", datei))
                }.onFailure {
                    kameraDatei = null; datei.delete(); onError("Es wurde keine Kamera-App gefunden.")
                }
            }
            MenuZeile("Dokumentenscan", Icons.Default.DocumentScanner) {
                menu = false
                val activity = context.findActivity()
                if (activity == null) {
                    onError("Der Dokumentenscan ist hier nicht verfügbar.")
                } else {
                    val optionen = GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(20)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build()
                    GmsDocumentScanning.getClient(optionen).getStartScanIntent(activity)
                        .addOnSuccessListener { sender -> scanner.launch(IntentSenderRequest.Builder(sender).build()) }
                        .addOnFailureListener { fehler ->
                            onError(fehler.message ?: "Der Dokumentenscanner konnte nicht gestartet werden.")
                        }
                }
            }
            MenuZeile("Audiodatei", Icons.Default.MusicNote) {
                menu = false
                runCatching { audioWahl.launch(arrayOf("audio/*")) }
                    .onFailure { onError("Es wurde keine Dateiauswahl gefunden.") }
            }
            MenuZeile("Zeichnung", Icons.Default.Palette) { menu = false; zeichnung = true }
            MenuZeile("Haftnotiz", Icons.Default.StickyNote2) { menu = false; haftnotiz = true }
            MenuZeile("Tabelle", Icons.Default.TableChart) { menu = false; tabelle = true }
        }
    }

    if (zeichnung) DrawingDialog(
        store = store,
        dismiss = { zeichnung = false },
        confirm = { anhang -> zeichnung = false; onAttachment(anhang) },
        onError = onError,
    )
    if (haftnotiz) StickyDialog({ haftnotiz = false }) { anhang -> haftnotiz = false; onAttachment(anhang) }
    if (tabelle) TableDialog({ tabelle = false }) { anhang -> tabelle = false; onAttachment(anhang) }
    if (sprachaufnahme) VoiceRecorderDialog(
        store = store,
        dismiss = { sprachaufnahme = false },
        confirm = { anhang -> sprachaufnahme = false; onAttachment(anhang) },
        onError = onError,
    )
}

@Composable
private fun MenuZeile(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface) },
    )
}

// ------------------------------------------------------- Anhänge im Entwurf

/** Zeigt die noch nicht gesendeten Anhänge über dem Eingabefeld. */
@Composable
fun PendingAttachments(attachments: List<Attachment>, onRemove: (Attachment) -> Unit) {
    if (attachments.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { anhang ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
            ) {
                Row(
                    Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(anhang.kind.icon(), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        anhang.name.ifBlank { anhang.kind.label() },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 170.dp).padding(start = 6.dp),
                    )
                    IconButton({ onRemove(anhang) }, Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, "Anhang entfernen", Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------- Anhänge in Notizen

/** Stellt die Anhänge einer Notiz dar — Bilder, PDF-Vorschau, Wiedergabe, Haftnotiz und Tabelle. */
@Composable
fun AttachmentGallery(attachments: List<Attachment>, modifier: Modifier = Modifier) {
    if (attachments.isEmpty()) return
    val context = LocalContext.current
    var vollbild by remember { mutableStateOf<Attachment?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        attachments.forEach { anhang ->
            when (anhang.kind) {
                AttachmentKind.IMAGE, AttachmentKind.SCAN, AttachmentKind.DRAWING ->
                    BildAnhang(anhang) { vollbild = anhang }
                AttachmentKind.PDF -> PdfAnhang(anhang) { context.oeffneExtern(anhang) }
                AttachmentKind.VOICE, AttachmentKind.AUDIO -> AudioAnhang(anhang)
                AttachmentKind.STICKY -> HaftnotizAnhang(anhang)
                AttachmentKind.TABLE -> TabellenAnhang(anhang)
            }
        }
    }
    vollbild?.let { anhang -> ImageViewerDialog(anhang) { vollbild = null } }
}

@Composable
private fun BildAnhang(anhang: Attachment, onClick: () -> Unit) {
    val bitmap = rememberBitmap(anhang.path)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        if (bitmap == null) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(anhang.kind.icon(), null, tint = MaterialTheme.colorScheme.primary)
                Text(anhang.name.ifBlank { anhang.kind.label() }, Modifier.padding(start = 10.dp))
            }
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = anhang.name.ifBlank { anhang.kind.label() },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            )
        }
    }
}

@Composable
private fun PdfAnhang(anhang: Attachment, onOpen: () -> Unit) {
    val vorschau = rememberBitmap(anhang.thumbPath)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column {
            vorschau?.let { bild ->
                Image(
                    bitmap = bild,
                    contentDescription = anhang.name,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                )
            }
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        anhang.name.ifBlank { "PDF" },
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (anhang.pages > 0) "${anhang.pages} Seiten — zum Öffnen tippen" else "Zum Öffnen tippen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioAnhang(anhang: Attachment) {
    var spielt by remember { mutableStateOf(false) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(anhang.id) {
        onDispose { player.value?.release(); player.value = null }
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({
                val aktuell = player.value
                if (spielt && aktuell != null) {
                    aktuell.release(); player.value = null; spielt = false
                } else {
                    val datei = anhang.file
                    if (datei != null) {
                        player.value?.release()
                        player.value = runCatching {
                            MediaPlayer().apply {
                                setDataSource(datei.absolutePath)
                                setOnCompletionListener { p -> p.release(); player.value = null; spielt = false }
                                prepare(); start()
                            }
                        }.getOrNull()
                        spielt = player.value != null
                    }
                }
            }) {
                Icon(
                    if (spielt) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (spielt) "Anhalten" else "Abspielen",
                )
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text(
                    anhang.name.ifBlank { anhang.kind.label() },
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (anhang.durationMs > 0) dauerText(anhang.durationMs) else anhang.kind.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HaftnotizAnhang(anhang: Attachment) {
    Surface(
        shape = RoundedCornerShape(4.dp, 18.dp, 4.dp, 4.dp),
        color = Color(if (anhang.color != 0) anhang.color else HaftnotizFarben.first().toInt()),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("HAFTNOTIZ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B6B6B))
            Text(anhang.text, color = Color(0xFF1A1A1A), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun TabellenAnhang(anhang: Attachment) {
    val zeilen = remember(anhang.text) { anhang.text.split("\n").map { it.split("\t") } }
    if (zeilen.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            zeilen.forEachIndexed { index, zeile ->
                Row(
                    Modifier.background(
                        if (index == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else Color.Transparent,
                    ),
                ) {
                    zeile.forEach { zelle ->
                        Text(
                            zelle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.width(132.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
                if (index < zeilen.lastIndex) Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
            }
        }
    }
}

@Composable
private fun ImageViewerDialog(anhang: Attachment, dismiss: () -> Unit) {
    val bitmap = rememberBitmap(anhang.path, 2400)
    Dialog(onDismissRequest = dismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        anhang.name.ifBlank { anhang.kind.label() },
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).padding(start = 6.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(dismiss) { Icon(Icons.Default.Close, "Schließen") }
                }
                bitmap?.let { bild ->
                    Image(
                        bitmap = bild,
                        contentDescription = anhang.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------- Zeichnung

private data class Strich(val punkte: SnapshotStateList<Offset>, val farbe: Long, val staerke: Float)

@Composable
private fun DrawingDialog(
    store: AttachmentStore,
    dismiss: () -> Unit,
    confirm: (Attachment) -> Unit,
    onError: (String) -> Unit,
) {
    val striche = remember { mutableStateListOf<Strich>() }
    var farbe by remember { mutableStateOf(StiftFarben.first()) }
    var staerke by remember { mutableStateOf(6f) }
    var groesse by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = dismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Zeichnung", style = MaterialTheme.typography.titleMedium)
                Box(
                    Modifier.padding(top = 12.dp).fillMaxWidth().aspectRatio(0.8f)
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .onSizeChanged { groesse = it }
                        .pointerInput(farbe, staerke) {
                            detectDragGestures(
                                onDragStart = { start -> striche.add(Strich(mutableStateListOf(start), farbe, staerke)) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    striche.lastOrNull()?.punkte?.add(change.position)
                                },
                            )
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        striche.forEach { strich ->
                            val punkte = strich.punkte
                            if (punkte.size == 1) drawCircle(Color(strich.farbe), strich.staerke / 2f, punkte.first())
                            for (index in 1 until punkte.size) {
                                drawLine(
                                    color = Color(strich.farbe),
                                    start = punkte[index - 1],
                                    end = punkte[index],
                                    strokeWidth = strich.staerke,
                                    cap = StrokeCap.Round,
                                )
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StiftFarben.forEach { wert ->
                        Box(
                            Modifier.padding(end = 8.dp).size(if (wert == farbe) 30.dp else 24.dp)
                                .background(Color(wert), RoundedCornerShape(50))
                                .border(
                                    if (wert == farbe) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50),
                                )
                                .clickable { farbe = wert },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton({ if (striche.isNotEmpty()) striche.removeAt(striche.lastIndex) }) {
                        Icon(Icons.Default.Undo, "Letzten Strich zurücknehmen")
                    }
                    IconButton({ striche.clear() }) { Icon(Icons.Default.Delete, "Alles löschen") }
                }
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(3f, 6f, 12f, 20f).forEach { wert ->
                        Box(
                            Modifier.padding(end = 10.dp).size((wert + 14).dp)
                                .background(
                                    if (wert == staerke) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    RoundedCornerShape(50),
                                )
                                .clickable { staerke = wert },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(dismiss) { Text("Abbrechen") }
                    Button(
                        onClick = {
                            if (striche.isEmpty() || groesse.width == 0) {
                                dismiss()
                            } else {
                                val fertig = striche.toList()
                                val breite = groesse.width
                                val hoehe = groesse.height
                                scope.launch {
                                    runCatching { zeichnungSpeichern(store, fertig, breite, hoehe) }
                                        .onSuccess(confirm)
                                        .onFailure { onError(it.message ?: "Die Zeichnung konnte nicht gespeichert werden.") }
                                }
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Übernehmen") }
                }
            }
        }
    }
}

private suspend fun zeichnungSpeichern(
    store: AttachmentStore,
    striche: List<Strich>,
    breite: Int,
    hoehe: Int,
): Attachment = withContext(Dispatchers.IO) {
    val bitmap = android.graphics.Bitmap.createBitmap(breite, hoehe, android.graphics.Bitmap.Config.ARGB_8888)
    val leinwand = android.graphics.Canvas(bitmap)
    leinwand.drawColor(android.graphics.Color.WHITE)
    val stift = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    val punkt = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    striche.forEach { strich ->
        val farbe = Color(strich.farbe).toArgb()
        if (strich.punkte.size == 1) {
            punkt.color = farbe
            val start = strich.punkte.first()
            leinwand.drawCircle(start.x, start.y, strich.staerke / 2f, punkt)
        } else {
            stift.color = farbe
            stift.strokeWidth = strich.staerke
            val pfad = android.graphics.Path()
            strich.punkte.forEachIndexed { index, stelle ->
                if (index == 0) pfad.moveTo(stelle.x, stelle.y) else pfad.lineTo(stelle.x, stelle.y)
            }
            leinwand.drawPath(pfad, stift)
        }
    }
    val datei = store.newFile("-zeichnung.png")
    FileOutputStream(datei).use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out) }
    bitmap.recycle()
    store.describe(datei, AttachmentKind.DRAWING, "Zeichnung")
}

// -------------------------------------------------------------- Haftnotiz

@Composable
private fun StickyDialog(dismiss: () -> Unit, confirm: (Attachment) -> Unit) {
    var text by remember { mutableStateOf("") }
    var farbe by remember { mutableStateOf(HaftnotizFarben.first()) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Haftnotiz") },
        text = {
            Column {
                Row(Modifier.padding(bottom = 12.dp)) {
                    HaftnotizFarben.forEach { wert ->
                        Box(
                            Modifier.padding(end = 8.dp).size(if (wert == farbe) 34.dp else 28.dp)
                                .background(Color(wert), RoundedCornerShape(6.dp))
                                .border(
                                    if (wert == farbe) 2.dp else 1.dp,
                                    if (wert == farbe) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp),
                                )
                                .clickable { farbe = wert },
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Kurze Notiz …") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    confirm(
                        Attachment(
                            kind = AttachmentKind.STICKY,
                            name = text.trim().lineSequence().first().take(28).ifBlank { "Haftnotiz" },
                            text = text.trim(),
                            color = farbe.toInt(),
                        ),
                    )
                },
                enabled = text.isNotBlank(),
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(dismiss) { Text("Abbrechen") } },
    )
}

// ---------------------------------------------------------------- Tabelle

@Composable
private fun TableDialog(dismiss: () -> Unit, confirm: (Attachment) -> Unit) {
    val zeilen = remember {
        mutableStateListOf(listOf("", "").toMutableStateList(), listOf("", "").toMutableStateList())
    }
    Dialog(onDismissRequest = dismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Tabelle", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Die erste Zeile ist die Kopfzeile.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    Modifier.padding(top = 12.dp).heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
                ) {
                    zeilen.forEachIndexed { zeilenIndex, zeile ->
                        Row {
                            zeile.forEachIndexed { spaltenIndex, zelle ->
                                OutlinedTextField(
                                    value = zelle,
                                    onValueChange = { eingabe ->
                                        zeilen[zeilenIndex][spaltenIndex] = eingabe.replace('\t', ' ').replace('\n', ' ')
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(148.dp).padding(2.dp),
                                )
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 10.dp)) {
                    TextButton({ zeilen.add(List(zeilen.first().size) { "" }.toMutableStateList()) }) {
                        Icon(Icons.Default.Add, null); Text("Zeile")
                    }
                    TextButton({ zeilen.forEach { it.add("") } }) {
                        Icon(Icons.Default.Add, null); Text("Spalte")
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(dismiss) { Text("Abbrechen") }
                    Button(
                        onClick = {
                            val inhalt = zeilen.joinToString("\n") { zeile -> zeile.joinToString("\t") { it.trim() } }
                            confirm(Attachment(kind = AttachmentKind.TABLE, name = "Tabelle", text = inhalt))
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Übernehmen") }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Sprachaufnahme

@Composable
private fun VoiceRecorderDialog(
    store: AttachmentStore,
    dismiss: () -> Unit,
    confirm: (Attachment) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val datei = remember { store.newFile("-sprachaufnahme.m4a") }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    var sekunden by remember { mutableStateOf(0) }
    var laeuft by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val aufnahme = runCatching {
            @Suppress("DEPRECATION")
            val neu = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
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
        }.onFailure { onError(it.message ?: "Die Aufnahme konnte nicht gestartet werden.") }.getOrNull()
        recorder.value = aufnahme
        laeuft = aufnahme != null
        if (aufnahme == null) dismiss()
        onDispose {
            recorder.value?.let { runCatching { it.stop() }; runCatching { it.release() } }
            recorder.value = null
        }
    }
    LaunchedEffect(laeuft) {
        while (laeuft) { delay(1_000); sekunden += 1 }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Sprachaufnahme") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.error)
                Text(
                    dauerText(sekunden * 1000L),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                laeuft = false
                val aufnahme = recorder.value
                recorder.value = null
                val erfolgreich = aufnahme != null && runCatching { aufnahme.stop() }.isSuccess
                aufnahme?.let { runCatching { it.release() } }
                if (erfolgreich && datei.length() > 0) {
                    confirm(store.describe(datei, AttachmentKind.VOICE, "Sprachaufnahme"))
                } else {
                    datei.delete()
                    onError("Die Aufnahme war zu kurz.")
                    dismiss()
                }
            }) { Icon(Icons.Default.Stop, null); Text("Beenden", Modifier.padding(start = 6.dp)) }
        },
        dismissButton = {
            TextButton({
                laeuft = false
                recorder.value?.let { runCatching { it.stop() }; runCatching { it.release() } }
                recorder.value = null
                datei.delete()
                dismiss()
            }) { Text("Verwerfen") }
        },
    )
}

// ------------------------------------------------------------------ Helfer

fun AttachmentKind.label(): String = when (this) {
    AttachmentKind.PDF -> "PDF"
    AttachmentKind.VOICE -> "Sprachaufnahme"
    AttachmentKind.IMAGE -> "Bild"
    AttachmentKind.SCAN -> "Dokumentenscan"
    AttachmentKind.AUDIO -> "Audiodatei"
    AttachmentKind.DRAWING -> "Zeichnung"
    AttachmentKind.STICKY -> "Haftnotiz"
    AttachmentKind.TABLE -> "Tabelle"
}

fun AttachmentKind.icon(): ImageVector = when (this) {
    AttachmentKind.PDF -> Icons.Default.PictureAsPdf
    AttachmentKind.VOICE -> Icons.Default.Mic
    AttachmentKind.IMAGE -> Icons.Default.Image
    AttachmentKind.SCAN -> Icons.Default.DocumentScanner
    AttachmentKind.AUDIO -> Icons.Default.MusicNote
    AttachmentKind.DRAWING -> Icons.Default.Palette
    AttachmentKind.STICKY -> Icons.Default.StickyNote2
    AttachmentKind.TABLE -> Icons.Default.TableChart
}

private fun dauerText(millis: Long): String {
    val gesamt = millis / 1000
    return "%d:%02d".format(gesamt / 60, gesamt % 60)
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.oeffneExtern(anhang: Attachment) {
    val datei = anhang.file ?: return
    val uri = FileProvider.getUriForFile(this, "$packageName.dateien", datei)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }.onFailure { fehler ->
        if (fehler is ActivityNotFoundException) {
            runCatching { startActivity(Intent.createChooser(intent, "PDF öffnen mit").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

@Composable
private fun rememberBitmap(path: String?, maxKante: Int = 1400): ImageBitmap? {
    var bild by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path, maxKante) {
        bild = if (path.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching { ladeBitmap(path, maxKante)?.asImageBitmap() }.getOrNull()
        }
    }
    return bild
}

private fun ladeBitmap(pfad: String, maxKante: Int): android.graphics.Bitmap? {
    if (!File(pfad).exists()) return null
    val masse = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(pfad, masse)
    if (masse.outWidth <= 0) return null
    var faktor = 1
    while (masse.outWidth / faktor > maxKante || masse.outHeight / faktor > maxKante) faktor *= 2
    return BitmapFactory.decodeFile(pfad, BitmapFactory.Options().apply { inSampleSize = faktor })
}

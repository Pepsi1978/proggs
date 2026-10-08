package de.frank.jarvis.ui

import android.annotation.SuppressLint
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.Anhang
import de.frank.jarvis.ablage.Art
import de.frank.jarvis.ablage.Auszug
import de.frank.jarvis.ablage.Dateityp
import de.frank.jarvis.ablage.Eintrag
import de.frank.jarvis.ablage.Freigabe
import de.frank.jarvis.ablage.Medien
import de.frank.jarvis.faehigkeit.Ablage
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Kopfzeile jeder Vorschau: Zurück, Titel, Download, Teilen und weitere Aktionen. Gleiche Bedienung für jede Dateiart. */
@Composable
fun VorschauKopf(
    titel: String,
    untertitel: String,
    freigabe: Freigabe?,
    aktionen: AblageAktionen,
    zurueck: () -> Unit,
    loeschen: (() -> Unit)?,
    zusatz: @Composable () -> Unit = {},
    info: (() -> Unit)? = null,
    anpassen: (() -> Unit)? = null,
) {
    val f = LocalFarben.current
    var menue by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Rundknopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", aktion = zurueck)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (untertitel.isNotEmpty()) Text(untertitel, color = f.textSchwach, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        zusatz()
        if (freigabe != null) {
            Rundknopf(Icons.Rounded.Download, "Herunterladen in Download/Jarvis") { aktionen.herunterladen(listOf(freigabe)) }
            Rundknopf(Icons.Rounded.Share, "Teilen") { aktionen.teilen(listOf(freigabe), freigabe.name) }
        }
        Box {
            Rundknopf(Icons.Rounded.MoreVert, "Weitere Aktionen") { menue = true }
            Menue(menue, { menue = false }) {
                if (anpassen != null) DropdownMenuItem({ Text("An Bildschirm anpassen") }, { menue = false; anpassen() }, leadingIcon = { Icon(Icons.Rounded.FitScreen, null) })
                if (freigabe != null) {
                    DropdownMenuItem({ Text("Mit anderer App öffnen") }, { menue = false; aktionen.oeffnen(freigabe) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) })
                    DropdownMenuItem({ Text("Speichern unter …") }, { menue = false; aktionen.speichernUnter(freigabe) }, leadingIcon = { Icon(Icons.Rounded.SaveAs, null) })
                }
                if (info != null) DropdownMenuItem({ Text("Dateiinformationen") }, { menue = false; info() }, leadingIcon = { Icon(Icons.Rounded.Info, null) })
                if (loeschen != null) DropdownMenuItem({ Text("Löschen", color = f.gefahr) }, { menue = false; loeschen() }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = f.gefahr) })
            }
        }
    }
}

/** Die passende Vorschau für einen Anhang; für Formate ohne eigene Ansicht die Dateiinformationen mit Export. */
@Composable
fun AnhangVorschau(eintrag: Eintrag, anhang: Anhang, aktionen: AblageAktionen, zurueck: () -> Unit, loeschen: () -> Unit, mitText: Boolean = false) {
    val context = LocalContext.current
    val datei = remember(anhang.id) { AblageZentrale.speicher(context).datei(anhang) }
    val text by ladeImHintergrund("", eintrag.id, eintrag.geaendert, mitText) { if (mitText) AblageZentrale.speicher(context).text(eintrag) else "" }
    val freigabe = remember(anhang.id) { aktionen.freigabe(anhang) }
    var nurInfo by rememberSaveable(anhang.id) { mutableStateOf(false) }
    val untertitel = "${anhang.art.anzeige} · ${Dateityp.groesse(anhang.groesse)} · ${Ablage.datum(anhang.erstellt)}"
    // Aufgeklappt gehört der Platz dem Text: Die Datei rückt aus dem Bild, statt unter dem Text zu liegen.
    var textOffen by rememberSaveable(anhang.id) { mutableStateOf(false) }
    // Drehung des Bildes in Grad; -1 = wie in der Datei vermerkt (EXIF).
    var grad by rememberSaveable(anhang.id) { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize()) {
        var anpassen by remember { mutableIntStateOf(0) }
        var drehen by remember { mutableIntStateOf(0) }
        val zeigtDatei = !nurInfo && !textOffen && datei.isFile
        val mitAnpassen = anhang.art == Art.BILD || anhang.art == Art.PDF || anhang.art == Art.ANIMATION
        VorschauKopf(anhang.originalName, untertitel, freigabe, aktionen, { if (nurInfo) nurInfo = false else zurueck() }, loeschen,
            zusatz = { if (anhang.art == Art.BILD && zeigtDatei) Rundknopf(Icons.AutoMirrored.Rounded.RotateRight, "Im Uhrzeigersinn drehen") { drehen++ } },
            info = { nurInfo = true }, anpassen = if (mitAnpassen && zeigtDatei) ({ anpassen++ }) else null)
        if (nurInfo || !textOffen) Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            if (nurInfo || !datei.isFile) {
                InfoAnsicht(eintrag, anhang, freigabe, aktionen, if (!datei.isFile) "Die Datei fehlt im Speicher." else null)
            } else when (anhang.art) {
                Art.BILD -> BildAnsicht(datei, anpassen, anhang, freigabe, aktionen, drehen, grad) { grad = it }
                Art.ANIMATION -> GifAnsicht(datei, anpassen)
                Art.SVG -> SicheresWeb(svgHtml(datei), datei.length() > 5_000_000, anhang, freigabe, aktionen)
                Art.PDF -> PdfAnsicht(datei, anpassen, anhang, eintrag, freigabe, aktionen)
                Art.TEXT, Art.MARKDOWN, Art.CODE, Art.DATEN -> TextDateiAnsicht(datei, anhang)
                Art.TABELLE_TEXT -> TabellenAnsicht(datei, anhang.endung == "tsv")
                Art.HTML -> HtmlAnsicht(datei, anhang, freigabe, aktionen)
                Art.AUDIO, Art.VIDEO -> MedienAnsicht(datei, anhang, eintrag, freigabe, aktionen)
                Art.DOKUMENT, Art.TABELLE, Art.PRAESENTATION -> OfficeAnsicht(datei, anhang, eintrag, freigabe, aktionen)
                else -> InfoAnsicht(eintrag, anhang, freigabe, aktionen, "Für diesen Dateityp gibt es keine eingebaute Vorschau.")
            }
        }
        if (!nurInfo) Begleittext(text, textOffen, { textOffen = !textOffen }, if (textOffen) Modifier.weight(1f) else Modifier, fuellt = textOffen)
    }
}

/**
 * Der Text zu einer Datei (etwa der Auftrag zu einem erzeugten Bild): eingeklappt, damit die Datei den Platz bekommt.
 * Mit [fuellt] nimmt der aufgeklappte Text die ganze Höhe, die [modifier] ihm gibt; sonst höchstens 280 dp.
 */
@Composable
fun Begleittext(text: String, offen: Boolean, umschalten: () -> Unit, modifier: Modifier = Modifier, fuellt: Boolean = false) {
    if (text.isBlank()) return
    val f = LocalFarben.current
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).glas(f, erhoeht = 0.5f)) {
        Row(Modifier.fillMaxWidth().antippen(aktion = umschalten).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Text zum Eintrag", Modifier.weight(1f), color = f.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(if (offen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (offen) "Einklappen" else "Ausklappen", tint = f.textLeise)
        }
        if (offen) SelectionContainer(if (fuellt) Modifier.weight(1f) else Modifier.heightIn(max = 280.dp)) {
            Text(markdownText(text, f.primaer), Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

// ---- Bilder ----

/**
 * Bilder über SubsamplingScaleImageView: lädt große Bilder gekachelt (nie das ganze Bild in voller Auflösung),
 * zoomt mit zwei Fingern und per Doppeltipp und verschiebt im vergrößerten Zustand. So bleiben Beschriftungen von
 * Infografiken auch stark vergrößert scharf.
 */
@Composable
private fun BildAnsicht(datei: File, anpassen: Int, anhang: Anhang, freigabe: Freigabe, aktionen: AblageAktionen, drehen: Int, grad: Int, setzeGrad: (Int) -> Unit) {
    var fehler by remember { mutableStateOf(false) }
    if (fehler) {
        // Formate, die der Kachel-Decoder nicht kann (etwa HEIC auf manchen Geräten): verkleinert anzeigen.
        val bild by ladeImHintergrund<ImageBitmap?>(null, datei) { runCatching { Medien.bildVerkleinert(datei, 2048)?.asImageBitmap() }.getOrNull() }
        bild?.let { ZoomBild(it, anpassen, anhang.originalName) } ?: Hinweis("Dieses Bildformat kann die eingebaute Vorschau nicht anzeigen.", freigabe, aktionen)
        return
    }
    KachelBildMitVollbild(datei, anpassen, anhang.originalName, drehen, grad, setzeGrad) { fehler = true }
}

/**
 * Gekacheltes Bild; Antippen öffnet es bildschirmfüllend auf Schwarz (Zoom wie gewohnt), erneutes Antippen oder „Zurück“ schließt.
 * Jede Erhöhung von [drehen] dreht um 90 Grad im Uhrzeigersinn; [grad] hält die Drehung fest, damit das Vollbild sie übernimmt.
 */
@Composable
private fun KachelBildMitVollbild(datei: File, anpassen: Int, beschreibung: String, drehen: Int = 0, grad: Int = -1, setzeGrad: (Int) -> Unit = {}, beiFehler: () -> Unit) {
    var vollbild by rememberSaveable(datei.path) { mutableStateOf(false) }
    if (vollbild) {
        Dialog({ vollbild = false }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) { KachelBild(datei, 0, beschreibung, beiTipp = { vollbild = false }, schwarz = true, grad = grad) {} }
        }
    }
    KachelBild(datei, anpassen, beschreibung, beiTipp = { vollbild = true }, drehen = drehen, grad = grad, setzeGrad = setzeGrad, beiFehler = beiFehler)
}

@Composable
private fun KachelBild(
    datei: File, anpassen: Int, beschreibung: String, beiTipp: (() -> Unit)? = null, schwarz: Boolean = false,
    drehen: Int = 0, grad: Int = -1, setzeGrad: (Int) -> Unit = {}, beiFehler: () -> Unit,
) {
    val f = LocalFarben.current
    val ansicht = remember { arrayOfNulls<SubsamplingScaleImageView>(1) }
    LaunchedEffect(anpassen) { if (anpassen > 0) ansicht[0]?.resetScaleAndCenter() }
    var gedreht by remember { mutableIntStateOf(drehen) }
    LaunchedEffect(drehen) {
        val v = ansicht[0]
        if (drehen != gedreht && v != null) {
            gedreht = drehen
            // Von der tatsächlich gezeigten Lage aus weiterdrehen (berücksichtigt die Drehung aus der Datei).
            val neu = (v.appliedOrientation + 90) % 360
            v.orientation = neu
            setzeGrad(neu)
        }
    }
    AndroidView(
        factory = { ctx ->
            SubsamplingScaleImageView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
                setDoubleTapZoomDpi(160)
                maxScale = 12f
                orientation = if (grad in setOf(0, 90, 180, 270)) grad else SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                contentDescription = beschreibung
                setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                    override fun onImageLoadError(e: Exception?) = beiFehler()
                })
                beiTipp?.let { tipp -> setOnClickListener { tipp() } }
                setImage(ImageSource.uri(Uri.fromFile(datei)))
                ansicht[0] = this
            }
        },
        modifier = Modifier.fillMaxSize().background(if (f.dunkel || schwarz) Color.Black else Color(0xFFEFF3F8)),
        onRelease = { it.recycle() },
    )
}

/** Zoom für Bilder, die als Ganzes geladen sind (Ersatzweg und GIF): zwei Finger, Doppeltipp, Verschieben. */
@Composable
private fun ZoomBox(anpassen: Int, beschreibung: String, inhalt: @Composable () -> Unit) {
    var skala by remember { mutableFloatStateOf(1f) }
    var versatz by remember { mutableStateOf(Offset.Zero) }
    var groesse by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(anpassen) { skala = 1f; versatz = Offset.Zero }
    fun begrenze(o: Offset, s: Float): Offset {
        val mx = groesse.width * (s - 1) / 2
        val my = groesse.height * (s - 1) / 2
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }
    Box(
        Modifier.fillMaxSize().onSizeChanged { groesse = it }.semantics { contentDescription = beschreibung }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { p ->
                    if (skala > 1.01f) { skala = 1f; versatz = Offset.Zero } else {
                        skala = 3f
                        versatz = begrenze(Offset((groesse.width / 2f - p.x) * 2f, (groesse.height / 2f - p.y) * 2f), 3f)
                    }
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val neu = (skala * zoom).coerceIn(1f, 8f)
                    skala = neu
                    versatz = begrenze(versatz + pan, neu)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer(scaleX = skala, scaleY = skala, translationX = versatz.x, translationY = versatz.y), contentAlignment = Alignment.Center) { inhalt() }
    }
}

@Composable
private fun ZoomBild(bild: ImageBitmap, anpassen: Int, beschreibung: String) = ZoomBox(anpassen, beschreibung) {
    Image(bild, beschreibung, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
}

/** GIF: ab Android 9 animiert (ImageDecoder), darunter das erste Bild. */
@Composable
private fun GifAnsicht(datei: File, anpassen: Int) {
    ZoomBox(anpassen, datei.name) {
        AndroidView(factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                runCatching {
                    if (Build.VERSION.SDK_INT >= 28) {
                        val d = android.graphics.ImageDecoder.decodeDrawable(android.graphics.ImageDecoder.createSource(datei))
                        setImageDrawable(d)
                        (d as? AnimatedImageDrawable)?.start()
                    } else setImageBitmap(Medien.bildVerkleinert(datei, 2048))
                }
            }
        }, modifier = Modifier.fillMaxSize())
    }
}

// ---- PDF ----

/**
 * Mehrseitige PDF-Vorschau mit Androids PdfRenderer. Jede Seite wird in hoher Auflösung (2400 px) als Bild im
 * Zwischenspeicher abgelegt und gekachelt angezeigt: Zoom, Doppeltipp und Verschieben wie bei Bildern.
 * Textsuche und Textauswahl bietet der PdfRenderer nicht zuverlässig; dafür „Mit anderer App öffnen“.
 */
@Composable
private fun PdfAnsicht(datei: File, anpassen: Int, anhang: Anhang, eintrag: Eintrag, freigabe: Freigabe, aktionen: AblageAktionen) {
    val f = LocalFarben.current
    val context = LocalContext.current
    var seite by rememberSaveable(anhang.id) { mutableIntStateOf(0) }
    val seiten by ladeImHintergrund<Result<Int>?>(null, datei) { runCatching { Medien.pdfSeiten(datei) } }
    val r = seiten ?: run { Lade("PDF wird geöffnet …"); return }
    val anzahl = r.getOrElse { Hinweis(it.message ?: "Die PDF-Datei lässt sich nicht anzeigen.", freigabe, aktionen); return }
    if (anzahl == 0) { Hinweis("Die PDF-Datei enthält keine Seiten.", freigabe, aktionen); return }
    val bild by ladeImHintergrund<Result<File?>?>(null, datei, seite) {
        runCatching { Medien.pdfSeiteAlsDatei(datei, seite, 2400, File(context.cacheDir, "pdf-seiten/${eintrag.id}-${anhang.id}-$seite.png")) }
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val b = bild) {
                null -> Lade("Seite ${seite + 1} wird gezeichnet …")
                else -> b.getOrNull()?.let { png -> androidx.compose.runtime.key(png.absolutePath) { KachelBildMitVollbild(png, anpassen, "PDF-Seite ${seite + 1} von $anzahl") {} } }
                    ?: Hinweis("Diese Seite lässt sich nicht anzeigen.", freigabe, aktionen)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Rundknopf(Icons.Rounded.ChevronLeft, "Vorige Seite") { if (seite > 0) seite-- }
            Text("Seite ${seite + 1} von $anzahl", Modifier.weight(1f), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Rundknopf(Icons.Rounded.ChevronRight, "Nächste Seite") { if (seite < anzahl - 1) seite++ }
        }
    }
}

// ---- Text, Code, Tabellen ----

@Composable
private fun TextDateiAnsicht(datei: File, anhang: Anhang) {
    val vorschau by ladeImHintergrund<Auszug.TextVorschau?>(null, datei) { runCatching { Auszug.lies(datei) }.getOrNull() }
    val v = vorschau ?: run { Lade("Wird gelesen …"); return }
    val text = remember(v) { if (anhang.endung == "json" && !v.gekuerzt) Auszug.jsonHuebsch(v.text) ?: v.text else v.text }
    LeseText(text, anhang.endung, anhang.art == Art.MARKDOWN, v.gekuerzt, anhang.groesse)
}

/** Lesbarer, auswählbarer Text mit Kopieren und Schriftgröße. Code mit einfacher Hervorhebung in Festbreitenschrift. */
@Composable
fun LeseText(text: String, endung: String, markdown: Boolean, gekuerzt: Boolean, gesamt: Long, kopfzeile: @Composable () -> Unit = {}) {
    val f = LocalFarben.current
    val zwischenablage = LocalClipboardManager.current
    var groesse by rememberSaveable { mutableFloatStateOf(if (markdown || endung == "txt") 16f else 13f) }
    val code = !markdown && endung != "txt"
    val farben = remember(f.dunkel) { CodeFarben.fuer(f.dunkel) }
    val inhalt: AnnotatedString = remember(text, endung, markdown, f.dunkel) {
        when {
            markdown -> markdownText(text, f.primaer)
            code -> hervorgehoben(text, endung, farben)
            else -> AnnotatedString(text)
        }
    }
    var kopiert by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kopfzeile()
            Box(Modifier.weight(1f))
            Rundknopf(Icons.Rounded.TextDecrease, "Schrift kleiner") { groesse = (groesse - 1f).coerceAtLeast(10f) }
            Rundknopf(Icons.Rounded.TextIncrease, "Schrift größer") { groesse = (groesse + 1f).coerceAtMost(28f) }
            Rundknopf(Icons.Rounded.ContentCopy, if (kopiert) "Kopiert" else "Text kopieren", farbe = if (kopiert) f.erfolg else null) { zwischenablage.setText(AnnotatedString(text)); kopiert = true }
        }
        if (gekuerzt) Text("Vorschau zeigt den Anfang (${Dateityp.groesse(Auszug.VORSCHAU_BYTES.toLong())} von ${Dateityp.groesse(gesamt)}). Die Datei selbst ist vollständig; zum Lesen des Rests herunterladen oder mit anderer App öffnen.",
            Modifier.padding(horizontal = 18.dp, vertical = 4.dp), color = f.textLeise, fontSize = 12.sp)
        SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
            val v = rememberScrollState()
            if (code) {
                Text(inhalt, Modifier.fillMaxSize().verticalScroll(v).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                    color = f.text, fontSize = groesse.sp, lineHeight = (groesse * 1.45f).sp, fontFamily = FontFamily.Monospace, softWrap = false)
            } else {
                Text(inhalt, Modifier.fillMaxSize().verticalScroll(v).padding(horizontal = 18.dp, vertical = 10.dp), color = f.text, fontSize = groesse.sp, lineHeight = (groesse * 1.45f).sp)
            }
        }
    }
}

private data class CodeFarben(val kommentar: Color, val zeichenkette: Color, val zahl: Color, val wort: Color, val marke: Color) {
    companion object {
        fun fuer(dunkel: Boolean) = if (dunkel) CodeFarben(Color(0xFF7D8FA8), Color(0xFF8BD49C), Color(0xFFF2B36B), Color(0xFF7FC8FF), Color(0xFFE09CF0))
        else CodeFarben(Color(0xFF6A7A90), Color(0xFF1E7A3C), Color(0xFFAD5A00), Color(0xFF0B63B5), Color(0xFF8A2BA0))
    }
}

private fun hervorgehoben(text: String, endung: String, c: CodeFarben): AnnotatedString = buildAnnotatedString {
    append(text)
    Auszug.hervorheben(text, endung).forEach { b ->
        val farbe = when (b.klasse) {
            Auszug.Klasse.KOMMENTAR -> c.kommentar
            Auszug.Klasse.ZEICHENKETTE -> c.zeichenkette
            Auszug.Klasse.ZAHL -> c.zahl
            Auszug.Klasse.SCHLUESSELWORT -> c.wort
            Auszug.Klasse.MARKE -> c.marke
        }
        addStyle(SpanStyle(color = farbe, fontStyle = if (b.klasse == Auszug.Klasse.KOMMENTAR) FontStyle.Italic else null), b.von, b.bis)
    }
}

/** Einfaches Markdown: Überschriften, Aufzählungen, **fett**, *kursiv*, `Code`. Der Text bleibt vollständig auswählbar. */
fun markdownText(text: String, akzent: Color): AnnotatedString = buildAnnotatedString {
    val zeilen = text.replace("\r", "").lines()
    var imCode = false
    zeilen.forEachIndexed { i, roh ->
        if (roh.trimStart().startsWith("```")) { imCode = !imCode; if (i < zeilen.size - 1) append('\n'); return@forEachIndexed }
        when {
            imCode -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) { append(roh) }
            roh.startsWith("#") -> {
                val stufe = roh.takeWhile { it == '#' }.length.coerceIn(1, 6)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (24 - stufe * 2.5f).sp, color = if (stufe <= 2) akzent else Color.Unspecified)) { inline(roh.drop(stufe).trim()) }
            }
            roh.trimStart().let { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("+ ") } -> {
                append("    ".repeat((roh.length - roh.trimStart().length) / 2)); append("•  "); inline(roh.trimStart().drop(2))
            }
            roh.trim() == "---" || roh.trim() == "***" -> append("────────────")
            roh.startsWith("> ") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { inline(roh.drop(2)) }
            else -> inline(roh)
        }
        if (i < zeilen.size - 1) append('\n')
    }
}

private fun AnnotatedString.Builder.inline(t: String) {
    val muster = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__|`([^`]+)`|\\*([^*\\s][^*]*?)\\*|\\[([^\\]]+)]\\(([^)]+)\\)")
    var pos = 0
    muster.findAll(t).forEach { m ->
        append(t.substring(pos, m.range.first))
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[1]) }
            g[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
            g[3].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(g[3]) }
            g[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[4]) }
            else -> { append(g[5]); append(" (" + g[6] + ")") }
        }
        pos = m.range.last + 1
    }
    append(t.substring(pos))
}

/** CSV/TSV als Tabelle: höchstens 500 Zeilen und 40 Spalten in der Vorschau, die Datei bleibt vollständig. */
@Composable
private fun TabellenAnsicht(datei: File, tsv: Boolean) {
    val f = LocalFarben.current
    val tabelle by ladeImHintergrund<Auszug.Tabelle?>(null, datei) {
        runCatching { val v = Auszug.lies(datei); Auszug.tabelle(v.text, tsv).let { if (v.gekuerzt) it.copy(gekuerzt = true) else it } }.getOrNull()
    }
    val t = tabelle ?: run { Lade("Tabelle wird gelesen …"); return }
    val spalten = t.zeilen.maxOfOrNull { it.size } ?: 0
    val breiten = remember(t) { (0 until spalten).map { s -> (t.zeilen.take(200).maxOfOrNull { it.getOrNull(s)?.length ?: 0 } ?: 0).coerceIn(4, 32) } }
    val quer = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Text("${t.zeilen.size} Zeilen · $spalten Spalten" + if (t.gekuerzt) " · Vorschau gekürzt, die Datei ist vollständig" else "", Modifier.padding(horizontal = 18.dp, vertical = 4.dp), color = f.textLeise, fontSize = 12.sp)
        SelectionContainer(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                itemsIndexed(t.zeilen) { i, zeile ->
                    Row(Modifier.horizontalScroll(quer).background(if (i == 0) f.flaecheStark else if (i % 2 == 0) f.flaeche else Color.Transparent).padding(vertical = 6.dp)) {
                        for (s in 0 until spalten) {
                            Text(zeile.getOrNull(s).orEmpty(), Modifier.width((breiten[s] * 8 + 16).dp).padding(horizontal = 8.dp), color = f.text, fontSize = 13.sp,
                                fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ---- HTML und SVG: gesicherte Vorschau ----

@Composable
private fun HtmlAnsicht(datei: File, anhang: Anhang, freigabe: Freigabe, aktionen: AblageAktionen) {
    val f = LocalFarben.current
    var quelltext by rememberSaveable(anhang.id) { mutableStateOf(false) }
    val v by ladeImHintergrund<Auszug.TextVorschau?>(null, datei) { runCatching { Auszug.lies(datei, 2_000_000) }.getOrNull() }
    val t = v ?: run { Lade("Wird gelesen …"); return }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("Gesicherte Ansicht", !quelltext) { quelltext = false }
            Chip("Quelltext", quelltext) { quelltext = true }
        }
        if (!quelltext) Text("Ohne Skripte, ohne Internet und ohne Zugriff auf die App angezeigt.", Modifier.padding(horizontal = 18.dp, vertical = 4.dp), color = f.textLeise, fontSize = 12.sp)
        Box(Modifier.weight(1f)) {
            if (quelltext) LeseText(t.text, "html", markdown = false, gekuerzt = t.gekuerzt, gesamt = anhang.groesse)
            else SicheresWeb(t.text, false, anhang, freigabe, aktionen)
        }
    }
}

private fun svgHtml(datei: File): String {
    if (datei.length() > 5_000_000) return ""
    val b64 = Base64.encodeToString(datei.readBytes(), Base64.NO_WRAP)
    // Als <img> eingebunden: SVG-Skripte laufen so grundsätzlich nicht, die Grafik bleibt beim Zoomen scharf.
    return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=10\"></head>" +
        "<body style=\"margin:0;background:#fff;display:flex;align-items:center;justify-content:center;min-height:100vh\"><img style=\"max-width:100%;height:auto\" src=\"data:image/svg+xml;base64,$b64\"></body></html>"
}

/** WebView ohne JavaScript, ohne Netz, ohne Datei- und Inhaltszugriff, ohne Navigation. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SicheresWeb(html: String, zuGross: Boolean, anhang: Anhang, freigabe: Freigabe, aktionen: AblageAktionen) {
    if (zuGross || html.isEmpty()) { Hinweis("Die Datei ist für die eingebaute Vorschau zu groß.", freigabe, aktionen); return }
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.blockNetworkLoads = true
            settings.blockNetworkImage = true
            settings.setGeolocationEnabled(false)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            contentDescription = anhang.originalName
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
            }
            loadDataWithBaseURL("about:blank", html, "text/html", "utf-8", null)
        }
    }, modifier = Modifier.fillMaxSize(), onRelease = { it.destroy() })
}

// ---- Audio und Video ----

/**
 * Wiedergabe mit Media3/ExoPlayer: streamt von der Datei (lädt nie alles in den Speicher), übernimmt den Audiofokus
 * (pausiert bei Anruf oder anderer Wiedergabe) und hält an, wenn Kopfhörer abgezogen werden. Bedienleiste mit
 * Wiedergabe/Pause, Positionsregler, Zeit, Gesamtdauer und Geschwindigkeit (Einstellungen-Knopf); Video mit Vollbild.
 */
@OptIn(UnstableApi::class)
@Composable
private fun MedienAnsicht(datei: File, anhang: Anhang, eintrag: Eintrag, freigabe: Freigabe, aktionen: AblageAktionen) {
    val context = LocalContext.current
    val f = LocalFarben.current
    val video = anhang.art == Art.VIDEO
    var position by rememberSaveable(anhang.id) { mutableStateOf(0L) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var vollbild by rememberSaveable(anhang.id) { mutableStateOf(false) }
    val player = remember(anhang.id) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(if (video) C.AUDIO_CONTENT_TYPE_MOVIE else C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(datei)))
            prepare()
            seekTo(position)
        }
    }
    DisposableEffect(player) {
        val hoerer = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                fehler = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED, PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                        "Dieses Handy kann das Format oder den Codec dieser Datei nicht abspielen."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED, PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Die Datei ist beschädigt oder hat ein Format, das die eingebaute Wiedergabe nicht kennt."
                    else -> "Die Wiedergabe ist fehlgeschlagen (${error.errorCodeName})."
                }
            }
        }
        player.addListener(hoerer)
        onDispose { position = player.currentPosition; player.removeListener(hoerer); player.release() }
    }
    fehler?.let { Hinweis("$it Mit „Mit anderer App öffnen“ klappt es eventuell; Download und Teilen gehen immer.", freigabe, aktionen); return }
    @Composable
    fun Spieler(modifier: Modifier, imVollbild: Boolean) {
        AndroidView(factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
                setShowShuffleButton(false)
                setShowSubtitleButton(false)
                if (!video) { controllerShowTimeoutMs = 0; controllerHideOnTouch = false; useArtwork = true }
                if (video) setFullscreenButtonClickListener { vollbild = !imVollbild }
                setFullscreenButtonState(imVollbild)
                contentDescription = anhang.originalName
            }
        }, update = { it.player = player }, onRelease = { it.player = null }, modifier = modifier)
    }
    if (video && vollbild) {
        Dialog({ vollbild = false }, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) { Spieler(Modifier.fillMaxSize(), imVollbild = true) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (video) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) { if (!vollbild) Spieler(Modifier.fillMaxSize(), imVollbild = false) }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(120.dp).glas(f, 60.dp, erhoeht = 0.6f, toenung = f.primaer), contentAlignment = Alignment.Center) {
                    Icon(dateiSymbol(anhang.art), null, tint = f.primaer, modifier = Modifier.size(56.dp))
                }
                Text(anhang.originalName, Modifier.padding(top = 14.dp, start = 24.dp, end = 24.dp), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(eintrag.titel, Modifier.padding(horizontal = 24.dp), color = f.textLeise, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spieler(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 8.dp), imVollbild = false)
        }
    }
}

// ---- Office ----

/** Office/OpenDocument: Dateiinformationen plus Textauszug (ohne Formatierung) als Orientierung; Original extern öffnen. */
@Composable
private fun OfficeAnsicht(datei: File, anhang: Anhang, eintrag: Eintrag, freigabe: Freigabe, aktionen: AblageAktionen) {
    val auszug by ladeImHintergrund<String?>("…", datei) { Auszug.office(datei, anhang.endung) }
    val text = auszug
    if (text == null || text == "…") {
        InfoAnsicht(eintrag, anhang, freigabe, aktionen, if (text == null) "Für diesen Dateityp gibt es keine eingebaute Vorschau. Öffne die Datei mit einer passenden App (zum Beispiel Word, Excel, PowerPoint oder Google Docs)." else null)
        return
    }
    LeseText(text, "txt", markdown = false, gekuerzt = false, gesamt = anhang.groesse) {
        Knopf("Original öffnen", icon = Icons.AutoMirrored.Rounded.OpenInNew, haupt = false) { aktionen.oeffnen(freigabe) }
    }
}

// ---- Allgemein ----

/** Dateiinformationen mit allen Aktionen: der Weg für jedes Format ohne eingebaute Vorschau. */
@Composable
fun InfoAnsicht(eintrag: Eintrag, anhang: Anhang, freigabe: Freigabe, aktionen: AblageAktionen, hinweis: String?) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).glas(f, 18.dp, erhoeht = 0.4f, toenung = f.primaer), contentAlignment = Alignment.Center) { Icon(dateiSymbol(anhang.art), null, tint = f.primaer, modifier = Modifier.size(34.dp)) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(anhang.originalName, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(anhang.art.anzeige + " · " + anhang.endung.uppercase() + " · " + Dateityp.groesse(anhang.groesse), color = f.textLeise, fontSize = 13.sp)
            }
        }
        if (hinweis != null) Text(hinweis, color = f.text, fontSize = 14.sp, modifier = Modifier.glas(f, erhoeht = 0.4f).padding(14.dp))
        Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.5f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Info("Eintrag", eintrag.titel)
            Info("Dateityp", anhang.mime)
            Info("Größe", Dateityp.groesse(anhang.groesse) + " (${anhang.groesse} Bytes)")
            Info("Gespeichert", Ablage.datum(anhang.erstellt))
            Info("Herkunft", anhang.herkunft)
            if (anhang.beschreibung.isNotBlank()) Info("Beschreibung", anhang.beschreibung)
            Info("Prüfsumme (SHA-256)", anhang.sha256.take(16) + "…")
        }
        Knopf("Mit anderer App öffnen", Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.OpenInNew) { aktionen.oeffnen(freigabe) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Knopf("Download", Modifier.weight(1f), icon = Icons.Rounded.Download, haupt = false) { aktionen.herunterladen(listOf(freigabe)) }
            Knopf("Teilen", Modifier.weight(1f), icon = Icons.Rounded.Share, haupt = false) { aktionen.teilen(listOf(freigabe), freigabe.name) }
        }
        Knopf("Speichern unter …", Modifier.fillMaxWidth(), icon = Icons.Rounded.SaveAs, haupt = false) { aktionen.speichernUnter(freigabe) }
    }
}

@Composable
private fun Info(name: String, wert: String) {
    val f = LocalFarben.current
    Text(name, color = f.textSchwach, fontSize = 12.sp)
    Text(wert, color = f.text, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun Hinweis(text: String, freigabe: Freigabe, aktionen: AblageAktionen) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text, color = f.text, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Knopf("Mit anderer App öffnen", Modifier.padding(top = 18.dp), icon = Icons.AutoMirrored.Rounded.OpenInNew) { aktionen.oeffnen(freigabe) }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Knopf("Download", icon = Icons.Rounded.Download, haupt = false) { aktionen.herunterladen(listOf(freigabe)) }
            Knopf("Teilen", icon = Icons.Rounded.Share, haupt = false) { aktionen.teilen(listOf(freigabe), freigabe.name) }
        }
    }
}

@Composable
private fun Lade(text: String) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Kern(56.dp, f.primaer, aktiv = true)
        Text(text, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

/**
 * Lädt einen Wert auf einem Hintergrund-Faden und liefert ihn als Zustand; bei geänderten [schluessel] beginnt es mit
 * [start] neu. Ersetzt produceState, dessen Lint-Prüfung die Zuweisung aus withContext nicht erkennt.
 */
@Composable
fun <T> ladeImHintergrund(start: T, vararg schluessel: Any?, laden: suspend () -> T): androidx.compose.runtime.State<T> {
    val zustand = remember(*schluessel) { mutableStateOf(start) }
    LaunchedEffect(*schluessel) { zustand.value = withContext(Dispatchers.IO) { laden() } }
    return zustand
}

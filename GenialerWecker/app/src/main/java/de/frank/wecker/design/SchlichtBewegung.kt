package de.frank.wecker.design

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.frank.genialeideen.R
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.wecker.SichtbarerHintergrund
import de.frank.wecker.rememberResumed
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// Die Bewegungssprache von „Schlicht“: ein bewegtes Bild aus HyperFrames als Hintergrund, dazu
// wenige, ruhige Bewegungen in der Oberfläche — Licht, das über Glas läuft, Ziffern, die rollen,
// und Karten, die beim Öffnen einer Seite nacheinander aufsteigen. Alles steht still, sobald das
// System „Animationen entfernen“ meldet oder der Bildschirm nicht im Vordergrund ist.

/**
 * Schlichts Hintergrund: die Schleife „Goldseide“ — goldene Seidenbänder, Goldstaub, Bokeh und ein
 * Lichtschleier —, mit HyperFrames gerendert (Quelle: `design/hyperframes/goldseide`).
 *
 * Darunter liegt bis zum ersten Videobild der bisherige wandernde Goldschein; das Video blendet
 * weich darüber ein und löst ihn dann ab, sodass nie zwei Endlosbewegungen gleichzeitig laufen.
 * Bei reduzierter Bewegung oder einem Fehler beim Abspielen bleibt es beim bisherigen Hintergrund.
 */
@Composable
fun GoldseideHintergrund() {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val sichtbar = rememberResumed()
    var fehler by remember { mutableStateOf(false) }
    if (reduziert || fehler || !sichtbar) {
        SichtbarerHintergrund()
        return
    }
    val quelle = if (gold.istDunkel) R.raw.goldseide_dunkel else R.raw.goldseide_hell
    // Je Modus ein eigener Spieler; beim Wechsel Hell/Dunkel beginnt die Einblendung von vorn.
    key(quelle) {
        var laeuft by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
            if (!laeuft) SichtbarerHintergrund()
            GoldseideVideo(
                quelle = quelle,
                aufEingeblendet = { laeuft = true },
                aufFehler = { fehler = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Spielt die Schleife stumm auf einer [TextureView] ab. Bewusst ohne zusätzliche Bibliothek: Der
 * eingebaute [MediaPlayer] dekodiert in Hardware und kostet damit kaum Akku.
 *
 * Wichtig in einer Wecker-App: Das Video hat keine Tonspur, und die Lautstärke steht zusätzlich
 * auf null. Ohne beides könnte der Spieler Wiedergabe-Ressourcen belegen, die Weckton und
 * Vorlesen brauchen.
 *
 * Gelesen wird über den Dateideskriptor der Ressource, nicht über eine `android.resource://`-
 * Adresse: Namensraum (`de.frank.genialeideen`) und Paketname (`de.frank.genialerwecker`) dieser
 * App unterscheiden sich, eine solche Adresse zeigte ins Leere.
 */
@Composable
private fun GoldseideVideo(quelle: Int, aufEingeblendet: () -> Unit, aufFehler: () -> Unit, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { kontext ->
            TextureView(kontext).apply {
                alpha = 0f
                isOpaque = false
                val ansicht = this
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var spieler: MediaPlayer? = null
                    private var flaeche: Surface? = null
                    private var videoBreite = 0
                    private var videoHoehe = 0
                    private var eingeblendet = false

                    /** Einmal weich einblenden, sobald das erste Bild da ist — gleich, wer es zuerst meldet. */
                    private fun einblenden() {
                        if (eingeblendet) return
                        eingeblendet = true
                        ansicht.animate().alpha(1f).setDuration(Motion.BEWEGTBILD_EIN_MS)
                            .withEndAction { aufEingeblendet() }.start()
                    }

                    override fun onSurfaceTextureAvailable(textur: SurfaceTexture, breite: Int, hoehe: Int) {
                        val ziel = Surface(textur)
                        flaeche = ziel
                        val neu = MediaPlayer()
                        // Erst merken, dann einrichten: Scheitert ein Schritt, gibt das Zerstören
                        // der Fläche den Spieler trotzdem frei.
                        spieler = neu
                        runCatching {
                            kontext.resources.openRawResourceFd(quelle).use { datei ->
                                neu.setDataSource(datei.fileDescriptor, datei.startOffset, datei.length)
                            }
                            neu.setSurface(ziel)
                            neu.isLooping = true
                            neu.setVolume(0f, 0f)
                            neu.setOnVideoSizeChangedListener { _, vb, vh ->
                                videoBreite = vb
                                videoHoehe = vh
                                ansicht.fuellend(vb, vh)
                            }
                            neu.setOnInfoListener { _, was, _ ->
                                if (was == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) einblenden()
                                false
                            }
                            neu.setOnErrorListener { _, _, _ -> aufFehler(); true }
                            neu.setOnPreparedListener { it.start() }
                            neu.prepareAsync()
                        }.onFailure { aufFehler() }
                    }

                    override fun onSurfaceTextureSizeChanged(textur: SurfaceTexture, breite: Int, hoehe: Int) {
                        ansicht.fuellend(videoBreite, videoHoehe)
                    }

                    override fun onSurfaceTextureDestroyed(textur: SurfaceTexture): Boolean {
                        ansicht.animate().cancel()
                        spieler?.let { runCatching { it.stop() }; it.release() }
                        spieler = null
                        flaeche?.release()
                        flaeche = null
                        return true
                    }

                    // Nicht jedes Gerät meldet MEDIA_INFO_VIDEO_RENDERING_START. Ein neues Bild auf der
                    // Textur kommt dagegen immer — sonst bliebe das Video unsichtbar bei Deckung 0.
                    override fun onSurfaceTextureUpdated(textur: SurfaceTexture) = einblenden()
                }
            }
        },
    )
}

/**
 * Füllt die Ansicht wie `ContentScale.Crop`: Das Video behält sein Seitenverhältnis und wird
 * mittig beschnitten. Ohne diese Matrix zöge die TextureView es auf jede Bildschirmform auseinander.
 */
private fun TextureView.fuellend(videoBreite: Int, videoHoehe: Int) {
    if (videoBreite <= 0 || videoHoehe <= 0 || width == 0 || height == 0) return
    val massstab = max(width.toFloat() / videoBreite, height.toFloat() / videoHoehe)
    val matrix = Matrix()
    matrix.setScale(
        videoBreite * massstab / width, videoHoehe * massstab / height,
        width / 2f, height / 2f,
    )
    setTransform(matrix)
}

/**
 * Ein Lichtlauf: Ein weiches Band zieht schräg über die Fläche, dann ist neun Sekunden Ruhe.
 *
 * Anders als ein Endlosübergang zeichnet die Fläche in der Pause **kein einziges Bild** neu — der
 * Zeitwert wird nur im Zeichenblock gelesen, und zwischen den Läufen steht er still. Deshalb darf
 * auch der feststehende Kopf der Liste ihn tragen.
 *
 * @param nurSchrift legt das Licht nur auf die gezeichneten Pixel (Goldschrift), nicht auf die Fläche.
 * @param einmal läuft nur ein einziges Mal, etwa beim Öffnen eines Dialogs.
 */
fun Modifier.glanzLauf(
    staerke: Float,
    farbe: Color = Color.White,
    nurSchrift: Boolean = false,
    einmal: Boolean = false,
    versatzMs: Long = 0L,
): Modifier = composed {
    val reduziert = LocalBewegungReduziert.current
    val sichtbar = rememberResumed()
    val lauf = remember { Animatable(-1f) }
    LaunchedEffect(reduziert, sichtbar, einmal) {
        if (reduziert || !sichtbar) {
            lauf.snapTo(-1f)
            return@LaunchedEffect
        }
        delay(Motion.GLANZ_ERSTER_MS + versatzMs)
        while (true) {
            lauf.snapTo(0f)
            lauf.animateTo(1f, tween(Motion.GLANZ_LAUF_MS, easing = FastOutSlowInEasing))
            lauf.snapTo(-1f)
            if (einmal) break
            delay(Motion.GLANZ_PAUSE_MS)
        }
    }
    (if (nurSchrift) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else Modifier)
        .drawWithContent {
            drawContent()
            val p = lauf.value
            if (p < 0f) return@drawWithContent
            val band = max(size.width, size.height) * 0.55f
            val mitte = -band + p * (size.width + band * 2f)
            // Ein- und Ausblenden über den Lauf, damit das Band nicht hart an der Kante auftaucht.
            val deckung = staerke * sin(PI * p).toFloat()
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, farbe.copy(alpha = deckung), Color.Transparent),
                    start = Offset(mitte - band / 2f, 0f),
                    end = Offset(mitte + band / 2f, size.height * 0.7f),
                ),
                blendMode = if (nurSchrift) BlendMode.SrcAtop else BlendMode.SrcOver,
            )
        }
}

/**
 * Die Uhrzeit mit rollenden Ziffern: Beim Minutenwechsel gleitet nur die Ziffer, die sich ändert,
 * nach oben hinaus und die neue von unten herein. Für TalkBack bleibt es eine einzige Zeitangabe.
 */
@Composable
fun RollendeUhr(text: String, stil: TextStyle, modifier: Modifier = Modifier, ueberschrift: Boolean = false) {
    val reduziert = LocalBewegungReduziert.current
    Row(
        modifier.clearAndSetSemantics {
            contentDescription = text
            if (ueberschrift) heading()
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEachIndexed { stelle, zeichen ->
            key(stelle) {
                AnimatedContent(
                    targetState = zeichen,
                    transitionSpec = {
                        if (reduziert) {
                            (fadeIn(tween(0)) togetherWith fadeOut(tween(0))) using SizeTransform(clip = false)
                        } else {
                            val weich = FastOutSlowInEasing
                            (slideInVertically(tween(Motion.ZIFFER_MS, easing = weich)) { it * 3 / 5 } +
                                fadeIn(tween(Motion.ZIFFER_MS, easing = weich)))
                                .togetherWith(
                                    slideOutVertically(tween(Motion.ZIFFER_MS, easing = weich)) { -it * 3 / 5 } +
                                        fadeOut(tween(Motion.ZIFFER_MS / 2, easing = weich)),
                                )
                                .using(SizeTransform(clip = false))
                        }
                    },
                    label = "ziffer",
                ) { z ->
                    Text(z.toString(), style = stil, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/**
 * Merkt sich für eine Seite, wann sie aufgebaut wurde und wie viele Karten schon aufgetreten sind.
 * Nur innerhalb des kurzen Fensters nach dem Aufbau steigen Karten auf; was später erscheint —
 * beim Scrollen, Aufklappen oder Speichern —, steht sofort da und flackert nicht.
 */
class AuftrittsFolge {
    private val beginn = System.currentTimeMillis()
    private var naechste = 0

    /** Der Platz in der Folge, oder `null`, wenn das Fenster schon vorbei ist. */
    fun platz(): Int? =
        if (System.currentTimeMillis() - beginn > Motion.AUFTRITT_FENSTER_MS) null else naechste++
}

/** Die Folge der aktuellen Seite; ohne Anbieter tritt nichts auf. */
val LocalAuftrittsFolge = staticCompositionLocalOf<AuftrittsFolge?> { null }

/**
 * Eine Karte steigt beim ersten Aufbau der Seite weich auf: Sie blendet ein, gleitet 18 dp hoch und
 * wächst von 97 auf 100 Prozent. Nur in Schlicht, nur einmal je Karte.
 */
fun Modifier.auftritt(): Modifier = composed {
    val schlicht = LocalDesignTokens.current.design == Design.SCHLICHT
    val reduziert = LocalBewegungReduziert.current
    val folge = LocalAuftrittsFolge.current
    // Einmal beim ersten Zusammensetzen der Karte entschieden, nie wieder.
    val platz = remember { if (schlicht && !reduziert) folge?.platz() else null }
    if (platz == null) return@composed Modifier
    val wert = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(platz.coerceAtMost(8) * Motion.AUFTRITT_STAFFEL_MS.toLong())
        wert.animateTo(1f, tween(Motion.AUFTRITT_MS, easing = FastOutSlowInEasing))
    }
    val hub = 18.dp
    Modifier.graphicsLayer {
        val v = wert.value
        alpha = v
        translationY = (1f - v) * hub.toPx()
        val s = 0.97f + 0.03f * v
        scaleX = s
        scaleY = s
    }
}

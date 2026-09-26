package de.frank.newskompass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import java.io.File

private const val MAX_ZOOM = 5f
private const val DOPPELTIPP_ZOOM = 2.5f

/**
 * Das Bild einer Meldung bildschirmfüllend: mit zwei Fingern zoomen, gezoomt verschieben,
 * Doppeltipp zoomt hinein oder wieder heraus. Schließen über das Kreuz oder die Zurück-Geste.
 */
@Composable
fun BildAnsicht(bild: File, titel: String, istKi: Boolean, schliessen: () -> Unit, teilen: () -> Unit) {
    Dialog(
        onDismissRequest = schliessen,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var groesse by remember { mutableStateOf(IntSize.Zero) }
        var zoom by remember { mutableFloatStateOf(1f) }
        var versatz by remember { mutableStateOf(Offset.Zero) }

        // Das Bild darf nie so weit verschoben werden, dass schwarzer Rand in die Mitte rutscht.
        fun begrenze(o: Offset, z: Float): Offset {
            val maxX = groesse.width * (z - 1f) / 2f
            val maxY = groesse.height * (z - 1f) / 2f
            return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { groesse = it },
        ) {
            AsyncImage(
                model = bild,
                contentDescription = if (istKi) "$titel (KI-Illustration)" else titel,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { punkt ->
                                if (zoom > 1.01f) {
                                    zoom = 1f
                                    versatz = Offset.Zero
                                } else {
                                    // Hinein zoomen, und zwar dorthin, wo getippt wurde.
                                    val mitte = Offset(groesse.width / 2f, groesse.height / 2f)
                                    zoom = DOPPELTIPP_ZOOM
                                    versatz = begrenze((mitte - punkt) * (DOPPELTIPP_ZOOM - 1f), DOPPELTIPP_ZOOM)
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, verschiebung, faktor, _ ->
                            val neu = (zoom * faktor).coerceIn(1f, MAX_ZOOM)
                            zoom = neu
                            versatz = if (neu <= 1f) Offset.Zero else begrenze(versatz + verschiebung, neu)
                        }
                    }
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = versatz.x
                        translationY = versatz.y
                    },
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .safeDrawingPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RundKnopf(onClick = schliessen) { Icon(Icons.Rounded.Close, "Schließen", tint = Color.White) }
                Text(
                    titel,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RundKnopf(onClick = teilen) { Icon(Icons.Rounded.Share, "Mit Bild teilen", tint = Color.White) }
            }

            if (istKi) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(16.dp),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("KI-Illustration, kein Foto", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun RundKnopf(onClick: () -> Unit, inhalt: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f)),
    ) { inhalt() }
}

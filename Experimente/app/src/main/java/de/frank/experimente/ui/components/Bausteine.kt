package de.frank.experimente.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.experimente.ui.theme.AppForm
import de.frank.experimente.ui.theme.AppTypo
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalAppColors
import de.frank.experimente.ui.theme.LocalReduzierteBewegung
import de.frank.experimente.ui.theme.Mass
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.schattenKontrolle
import de.frank.experimente.ui.theme.schattenTopbar
import de.frank.experimente.ui.theme.schattenLeiste

/** Zwischenüberschrift — Inter 13 sp, Großbuchstaben, Laufweite 0,6 sp (02-UI-SPEC §3). */
@Composable
fun Zwischenueberschrift(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AppTypo.zwischenueberschrift,
        color = LocalAppColors.current.gedaempft,
        modifier = modifier,
    )
}

/**
 * Karte mit Druckrückmeldung **M-01**: sinkt auf 98 % ein, 120 ms `knapp`.
 * Bei reduzierter Bewegung entfällt das Einsinken; stattdessen hellt die Fläche auf.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Karte(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLangerDruck: (() -> Unit)? = null,
    hervorgehoben: Boolean = false,
    inhalt: @Composable () -> Unit,
) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()

    val bedienbar = onClick != null || onLangerDruck != null
    val skalierung by animateFloatAsState(
        targetValue = if (gedrueckt && !reduziert && bedienbar) 0.98f else 1f,
        animationSpec = tween(dauer(Bewegung.KNAPP_MS, reduziert), easing = Bewegung.knapp),
        label = "M-01",
    )
    val flaeche = when {
        hervorgehoben -> farben.erhoeht
        gedrueckt && reduziert -> farben.erhoeht
        else -> farben.flaeche
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(skalierung)
            .clip(AppForm.karte)
            .background(flaeche)
            .border(Mass.randstaerke, farben.rand, AppForm.karte)
            .then(
                if (bedienbar) {
                    Modifier.combinedClickable(
                        interactionSource = quelle,
                        indication = null,
                        onLongClick = onLangerDruck,
                        onClick = onClick ?: {},
                    )
                } else {
                    Modifier
                }
            )
            .padding(Mass.karteInnen),
    ) {
        Column { inhalt() }
    }
}

/** Gefüllter Knopf in *Aktion*. */
@Composable
fun AktionsKnopf(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aktiv: Boolean = true,
) {
    val farben = LocalAppColors.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()
    val flaeche = if (gedrueckt) farben.aktion.copy(alpha = 0.86f) else farben.aktion

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Mass.tippflaeche)
            .clip(AppForm.knopf)
            .background(if (aktiv) flaeche else farben.erhoeht)
            .clickable(
                enabled = aktiv,
                interactionSource = quelle,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypo.knopf,
            color = if (aktiv) {
                if (farben.istDunkel) farben.text else farben.flaeche
            } else {
                farben.blass
            },
        )
    }
}

/** Flacher Textknopf. */
@Composable
fun TextKnopf(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    farbe: Color? = null,
) {
    val farben = LocalAppColors.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Mass.tippflaeche)
            .clip(AppForm.knopf)
            .clickable(interactionSource = quelle, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypo.knopf,
            color = (farbe ?: farben.aktion).copy(alpha = if (gedrueckt) 0.7f else 1f),
        )
    }
}

/** Vollrundes Etikett — Stufe, Dauer, „von deiner Merkliste“. */
@Composable
fun Etikett(text: String, hervorgehoben: Boolean = false, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(AppForm.vollrund)
            .background(if (hervorgehoben) farben.aktionGedeckt else Color.Transparent)
            .padding(horizontal = if (hervorgehoben) 10.dp else 0.dp, vertical = if (hervorgehoben) 4.dp else 0.dp),
    ) {
        Text(
            text = text,
            style = AppTypo.stufe,
            color = if (hervorgehoben) farben.aktion else farben.gedaempft,
        )
    }
}

/**
 * **M-06** — der Haken zeichnet sich in 180 ms `haken`, danach wird die Zeile in 120 ms
 * gedämpft. Bei reduzierter Bewegung erscheint er sofort.
 */
@Composable
fun AufgabenZeile(
    text: String,
    erledigt: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current
    val quelle = remember { MutableInteractionSource() }

    val hakenAnteil by animateFloatAsState(
        targetValue = if (erledigt) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduziert) 0 else Bewegung.HAKEN_MS,
            easing = Bewegung.haken,
        ),
        label = "M-06",
    )
    val textFarbe by animateFloatAsState(
        targetValue = if (erledigt) 1f else 0f,
        animationSpec = tween(dauer(Bewegung.KNAPP_MS, reduziert), easing = Bewegung.knapp),
        label = "M-06-text",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Mass.tippflaeche)
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = quelle, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (erledigt) farben.erledigtGedeckt else Color.Transparent,
                )
                .border(
                    Mass.randstaerke,
                    if (erledigt) farben.erledigt else farben.rand,
                    RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (hakenAnteil > 0f) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = farben.erledigt.copy(alpha = hakenAnteil),
                    modifier = Modifier.size(16.dp).scale(hakenAnteil),
                )
            }
        }
        Text(
            text = text,
            style = AppTypo.fliesstext,
            color = androidx.compose.ui.graphics.lerp(farben.text, farben.blass, textFarbe),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** **M-07** — das Merken-Symbol füllt sich in 180 ms und geht dabei kurz auf 115 %. */
@Composable
fun MerkenSymbol(gemerkt: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current
    val skalierung by animateFloatAsState(
        targetValue = if (gemerkt) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (reduziert) 0 else Bewegung.HAKEN_MS,
            easing = Bewegung.haken,
        ),
        label = "M-07",
    )
    SymbolKnopf(
        symbol = if (gemerkt) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
        beschreibung = if (gemerkt) "Gemerkt" else "Merken",
        onClick = onClick,
        modifier = modifier.scale(1f + 0.15f * skalierung * (1f - skalierung) * 4f),
        tint = if (gemerkt) farben.aktion else farben.gedaempft,
    )
}

/** Symbol-Knopf mit kreisförmiger Druckfläche in *Aktion gedeckt*. */
@Composable
fun SymbolKnopf(
    symbol: ImageVector,
    beschreibung: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val farben = LocalAppColors.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()
    // Gemessen: 48×48, Fläche *Flaeche* mit 78 % Deckkraft, 1 dp Rand in *Rand*/84 %,
    // vollrund, dazu `0 8px 16px #000/16%`, `0 0 16px Aktion/6%`, `inset 0 1px 0 Text/12%`.
    Box(
        modifier = modifier
            .size(Mass.tippflaeche)
            .schattenKontrolle(farben, AppForm.vollrund)
            .clip(AppForm.vollrund)
            .background(if (gedrueckt) farben.aktionGedeckt else farben.flaeche.copy(alpha = 0.78f))
            .border(Mass.randstaerke, farben.rand.copy(alpha = 0.84f), AppForm.vollrund)
            .clickable(interactionSource = quelle, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = symbol,
            contentDescription = beschreibung,
            tint = tint ?: farben.gedaempft,
            modifier = Modifier.size(Mass.symbolListe),
        )
    }
}

/** Lautsprecher zum Vorlesen (F-12). Läuft die Wiedergabe, ist er in *Aktion* eingefärbt. */
@Composable
fun Lautsprecher(laeuft: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    SymbolKnopf(
        symbol = Icons.Outlined.VolumeUp,
        beschreibung = if (laeuft) "Vorlesen anhalten" else "Vorlesen",
        onClick = onClick,
        modifier = modifier,
        tint = if (laeuft) farben.aktion else farben.gedaempft,
    )
}

/**
 * **M-09** — Wartezustand der KI. Kein Kreisel: eine ruhige Textzeile und ein 2 dp hoher
 * Balken, durch den ein 30 % breiter Streifen in 1800 ms `linear` wandert.
 * Bei reduzierter Bewegung steht der Balken still.
 */
@Composable
fun Wartezustand(text: String = "Ich sehe mir deine letzten Tage an …", modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current

    val anteil = if (reduziert) {
        0f
    } else {
        val uebergang = rememberInfiniteTransition(label = "M-09")
        val wert by uebergang.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(Bewegung.WANDERN_MS, easing = Bewegung.wandern),
                repeatMode = RepeatMode.Restart,
            ),
            label = "M-09-anteil",
        )
        wert
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(AppForm.vollrund)
                .background(farben.erhoeht),
        ) {
            if (reduziert) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(farben.aktion.copy(alpha = 0.4f)),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                (anteil - 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                                anteil.coerceIn(0f, 1f) to farben.aktion,
                                (anteil + 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                                1f to Color.Transparent,
                            ),
                        ),
                )
            }
        }
        Text(
            text = text,
            style = AppTypo.fliesstext,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

/** Fehlerkarte — Rand in *Warnung*, mit „Nochmal versuchen“. */
@Composable
fun Fehlerkarte(text: String, onNochmal: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppForm.karte)
            .background(farben.erhoeht)
            .border(Mass.randstaerke, farben.warnung, AppForm.karte)
            .padding(Mass.karteInnen),
    ) {
        Column {
            Text(text, style = AppTypo.fliesstext, color = farben.warnung)
            if (onNochmal != null) {
                TextKnopf(
                    text = "Nochmal versuchen",
                    onClick = onNochmal,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** Leerer Zustand — ein Satz Text, höchstens ein gedämpftes Symbol. */
@Composable
fun LeerZustand(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypo.fliesstext,
        color = LocalAppColors.current.gedaempft,
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
    )
}

/** Obere Leiste, 64 dp, Titel linksbündig. */
@Composable
fun ObereLeiste(
    titel: String,
    modifier: Modifier = Modifier,
    links: @Composable (() -> Unit)? = null,
    rechts: @Composable (() -> Unit)? = null,
) {
    val farben = LocalAppColors.current
    // Gemessen: 412×64, Fläche *Grund* mit 82 % Deckkraft, Schatten `0 8px 20px #000/16%`
    // und ein Lichtsaum `inset 0 1px 0 Text/8%`. Titel bei x = 20.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Mass.obereLeiste)
            .schattenTopbar(farben)
            .background(farben.grund.copy(alpha = 0.82f))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        links?.invoke()
        Text(
            text = titel,
            style = AppTypo.bildschirmtitel,
            color = farben.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = if (links == null) 12.dp else 0.dp)
                .weight(1f),
        )
        rechts?.invoke()
    }
}

/** Ein Eintrag der unteren Leiste. */
data class LeistenZiel(val kennung: String, val beschriftung: String, val symbol: ImageVector)

/**
 * Untere Leiste, 72 dp, fünf gleich breite Felder. Der Wechsel zwischen den
 * Hauptbildschirmen ist reines Überblenden (Motion-Spec §4) — kein Schieben.
 */
@Composable
fun UntereLeiste(
    ziele: List<LeistenZiel>,
    aktiv: String,
    onWechsel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalAppColors.current
    // Die Leiste schwebt: 12 dp Abstand zu allen Kanten, 64 dp hoch, Radius 24 dp,
    // durchscheinende Fläche und Schatten (`--werft-schatten-leiste`).
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .height(64.dp)
            .schattenLeiste(farben, AppForm.dialog)
            .clip(AppForm.dialog)
            .background(farben.flaeche.copy(alpha = 0.88f))
            .border(BorderStroke(Mass.randstaerke, farben.rand.copy(alpha = 0.84f)), AppForm.dialog),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ziele.forEach { ziel ->
            val istAktiv = ziel.kennung == aktiv
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .fillMaxHeight()
                    .clip(AppForm.karte)
                    // Das aktive Feld trägt eine Pille in *Aktion gedeckt*.
                    .background(if (istAktiv) farben.aktionGedeckt else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onWechsel(ziel.kennung) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = ziel.symbol,
                    contentDescription = ziel.beschriftung,
                    tint = if (istAktiv) farben.aktion else farben.blass,
                    modifier = Modifier.size(Mass.symbolListe),
                )
                Text(
                    text = ziel.beschriftung,
                    style = AppTypo.stufe,
                    color = if (istAktiv) farben.aktion else farben.blass,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

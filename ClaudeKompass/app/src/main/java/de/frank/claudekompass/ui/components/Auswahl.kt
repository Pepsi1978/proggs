package de.frank.claudekompass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.claudekompass.ui.theme.LocalKompassFarben
import de.frank.claudekompass.ui.theme.Mass

/** Ein Eintrag im Klappmenü: was drinsteht, was darunter steht, und der Wert dahinter. */
data class AuswahlPunkt<T>(
    val wert: T,
    val text: String,
    val zusatz: String = "",
)

/**
 * Ein Klappmenü für eine Auswahl.
 *
 * Ersetzt die lange Liste aus Auswahlkreisen. Der Unterschied ist bei den Stimmen deutlich:
 * dreissig Einträge untereinander machen den Bildschirm unbenutzbar, und die getroffene Wahl
 * geht darin unter. Im Klappmenü steht sie oben — geschlossen sieht man genau eine Zeile,
 * nämlich die gewählte.
 *
 * [beiProbe] blendet neben dem Feld einen Abspielknopf ein. Er ist dort richtig aufgehoben:
 * Man wählt eine Stimme und will sie sofort hören, ohne die Liste erneut zu öffnen.
 */
@Composable
fun <T> Klappauswahl(
    beschriftung: String,
    punkte: List<AuswahlPunkt<T>>,
    gewaehlt: T?,
    beiWahl: (T) -> Unit,
    modifier: Modifier = Modifier,
    platzhalter: String = "Nichts ausgewählt",
    beiProbe: (() -> Unit)? = null,
    probeLaeuft: Boolean = false,
    beiProbeStopp: (() -> Unit)? = null,
) {
    val farben = LocalKompassFarben.current
    var offen by remember { mutableStateOf(false) }
    val aktueller = punkte.firstOrNull { it.wert == gewaehlt }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        if (beschriftung.isNotBlank()) {
            Text(
                text = beschriftung,
                style = MaterialTheme.typography.labelMedium,
                color = farben.textGedaempft,
            )
            Spacer(Modifier.padding(top = 3.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Mass.tippflaeche)
                        .background(farben.eingabefeld, RoundedCornerShape(Mass.radiusKlein))
                        .border(
                            1.dp,
                            if (offen) MaterialTheme.colorScheme.primary else farben.rahmen,
                            RoundedCornerShape(Mass.radiusKlein),
                        )
                        .clickable { offen = true }
                        .padding(horizontal = Mass.abstand, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = aktueller?.text ?: platzhalter,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (aktueller != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                farben.textGedaempft
                            },
                        )
                        if (!aktueller?.zusatz.isNullOrBlank()) {
                            Text(
                                text = aktueller.zusatz,
                                style = MaterialTheme.typography.labelSmall,
                                color = farben.textGedaempft,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Auswahl öffnen",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                DropdownMenu(
                    expanded = offen,
                    onDismissRequest = { offen = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        // Auf dem schmalen Cover-Display darf das Menü nicht über den Rand
                        // hinausragen; die Höhe begrenzt Compose selbst und macht es scrollbar.
                        .fillMaxWidth(0.92f),
                ) {
                    punkte.forEach { punkt ->
                        val istGewaehlt = punkt.wert == gewaehlt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Mass.tippflaeche)
                                .background(
                                    if (istGewaehlt) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    },
                                )
                                .clickable {
                                    beiWahl(punkt.wert)
                                    offen = false
                                }
                                .padding(horizontal = Mass.abstand, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = punkt.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (istGewaehlt) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    color = if (istGewaehlt) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (punkt.zusatz.isNotBlank()) {
                                    Text(
                                        text = punkt.zusatz,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = farben.textGedaempft,
                                    )
                                }
                            }
                            if (istGewaehlt) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "ausgewählt",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (beiProbe != null) {
                Spacer(Modifier.width(6.dp))
                ProbeKnopf(
                    laeuft = probeLaeuft,
                    beiKlick = { if (probeLaeuft) beiProbeStopp?.invoke() else beiProbe() },
                )
            }
        }
    }
}

/** Der Abspielknopf neben einer Stimmenauswahl. */
@Composable
private fun ProbeKnopf(laeuft: Boolean, beiKlick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Mass.tippflaeche)
            .background(
                color = if (laeuft) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(Mass.radiusKlein),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(Mass.radiusKlein),
            )
            .clickable(onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (laeuft) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (laeuft) "Probe anhalten" else "Stimme anhören",
            tint = if (laeuft) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(21.dp),
        )
    }
}

/**
 * Mehrfachauswahl als Klappmenü — für Dinge, die einzeln an- und abschaltbar sind.
 *
 * Geschlossen steht dort, wie viele von wie vielen aktiv sind. Das ist die Angabe, die man im
 * Alltag braucht; die Einzelheiten holt man sich beim Aufklappen.
 */
@Composable
fun Mehrfachauswahl(
    beschriftung: String,
    punkte: List<AuswahlPunkt<Int>>,
    aktiv: Set<Int>,
    beiWechsel: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalKompassFarben.current
    var offen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        if (beschriftung.isNotBlank()) {
            Text(
                text = beschriftung,
                style = MaterialTheme.typography.labelMedium,
                color = farben.textGedaempft,
            )
            Spacer(Modifier.padding(top = 3.dp))
        }

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Mass.tippflaeche)
                    .background(farben.eingabefeld, RoundedCornerShape(Mass.radiusKlein))
                    .border(
                        1.dp,
                        if (offen) MaterialTheme.colorScheme.primary else farben.rahmen,
                        RoundedCornerShape(Mass.radiusKlein),
                    )
                    .clickable { offen = true }
                    .padding(horizontal = Mass.abstand, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${aktiv.size} von ${punkte.size} aktiv",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (aktiv.size == punkte.size) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        farben.warnung
                    },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Auswahl öffnen",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            DropdownMenu(
                expanded = offen,
                onDismissRequest = { offen = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxWidth(0.92f),
            ) {
                punkte.forEach { punkt ->
                    val an = punkt.wert in aktiv
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { beiWechsel(punkt.wert, !an) }
                            .padding(horizontal = Mass.abstand, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                                .background(
                                    color = if (an) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .border(
                                    2.dp,
                                    if (an) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        farben.rahmen
                                    },
                                    RoundedCornerShape(4.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (an) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(Mass.abstandKlein))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = punkt.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (punkt.zusatz.isNotBlank()) {
                                Text(
                                    text = punkt.zusatz,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = farben.textGedaempft,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Mass.abstand, vertical = Mass.abstandKlein),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "Fertig",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { offen = false }
                            .padding(Mass.abstandKlein),
                    )
                }
            }
        }
    }
}

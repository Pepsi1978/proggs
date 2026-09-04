package de.frank.codexkompass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.frank.codexkompass.ui.theme.LocalKompassFarben
import de.frank.codexkompass.ui.theme.Mass
import de.frank.codexkompass.ui.theme.ThemeModus

/**
 * Ein kleiner quadratischer Knopf mit goldenem Rand.
 *
 * Die Mindestgrösse stammt aus der Referenz: 48 dp Tippfläche. Der sichtbare Rahmen darf
 * kleiner sein, die berührbare Fläche nicht — sonst trifft man auf dem schmalen Cover-Display
 * des Fold daneben.
 */
@Composable
fun GoldKnopf(
    symbol: ImageVector,
    beschreibung: String,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    aktiv: Boolean = false,
    laedt: Boolean = false,
    gesperrt: Boolean = false,
    groesse: androidx.compose.ui.unit.Dp = Mass.knopfKopfleiste,
) {
    val farben = LocalKompassFarben.current
    val vordergrund = when {
        gesperrt -> farben.textGedaempft
        aktiv -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier
            .size(Mass.tippflaeche)
            .clickable(enabled = !gesperrt && !laedt, onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(groesse)
                .background(
                    color = if (aktiv) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = 1.dp,
                    color = if (gesperrt) farben.rahmen else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (laedt) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = vordergrund,
                )
            } else {
                Icon(
                    imageVector = symbol,
                    contentDescription = beschreibung,
                    tint = vordergrund,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Die Kopfleiste des Hauptbildschirms (Referenz, Baustein C).
 *
 * Die Reihenfolge oben rechts ist fest: erst der Theme-Knopf, dann das Zahnrad. Diese Knöpfe
 * werden blind getroffen — eine Umsortierung wäre eine echte Verschlechterung.
 */
@Composable
fun KompassKopfleiste(
    titel: String,
    untertitel: String,
    themeModus: ThemeModus,
    beiTheme: () -> Unit,
    beiEinstellungen: () -> Unit,
    beiSuche: () -> Unit,
    beiAktualisieren: () -> Unit,
    aktualisierungLaeuft: Boolean,
    modifier: Modifier = Modifier,
) {
    val farben = LocalKompassFarben.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = Mass.randSchmal, vertical = Mass.abstandKlein),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titel,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (untertitel.isNotBlank()) {
                    Text(
                        text = untertitel,
                        style = MaterialTheme.typography.labelMedium,
                        color = farben.textGedaempft,
                    )
                }
            }
            GoldKnopf(
                symbol = Icons.Default.Search,
                beschreibung = "Suchen",
                beiKlick = beiSuche,
            )
            GoldKnopf(
                symbol = Icons.Default.Refresh,
                beschreibung = "Auf die neueste Fassung aktualisieren",
                beiKlick = beiAktualisieren,
                laedt = aktualisierungLaeuft,
            )
            GoldKnopf(
                symbol = when (themeModus) {
                    ThemeModus.HELL -> Icons.Default.LightMode
                    ThemeModus.DUNKEL -> Icons.Default.DarkMode
                },
                beschreibung = "Erscheinungsbild: ${themeModus.label}. Tippen wechselt.",
                beiKlick = beiTheme,
            )
            GoldKnopf(
                symbol = Icons.Default.Settings,
                beschreibung = "Einstellungen",
                beiKlick = beiEinstellungen,
            )
        }
    }
}

/** Eine dünne Trennlinie in der Rahmenfarbe. */
@Composable
fun Trennlinie(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalKompassFarben.current.rahmen),
    )
}

/**
 * Der Ladezustand einer Liste.
 *
 * Bewusst mit Text: Ein wandernder Kreis allein sagt nicht, worauf man wartet, und wirkt bei
 * längerem Warten wie ein Hänger.
 */
@Composable
fun LadeZustand(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Mass.rand * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Mass.abstand),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalKompassFarben.current.textGedaempft,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Ein ausgearbeiteter Leerzustand (Referenz, Baustein L).
 *
 * Drei Fälle sind ausdrücklich verschieden: noch nichts angelegt, Suche ohne Treffer, Laden
 * fehlgeschlagen. Deshalb bekommt jeder Aufruf seinen eigenen Text und seinen eigenen Knopf,
 * statt überall „Keine Daten" zu zeigen.
 */
@Composable
fun LeerZustand(
    symbol: ImageVector,
    ueberschrift: String,
    text: String,
    knopfText: String? = null,
    beiKnopf: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val farben = LocalKompassFarben.current
    Column(
        modifier = modifier.fillMaxWidth().padding(Mass.rand * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Mass.abstand),
    ) {
        Icon(
            imageVector = symbol,
            contentDescription = null,
            tint = farben.goldGedaempft,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = ueberschrift,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = farben.textGedaempft,
            textAlign = TextAlign.Center,
        )
        if (knopfText != null && beiKnopf != null) {
            TextButton(onClick = beiKnopf) {
                Text(knopfText, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * Ein Fehlerstreifen mit Klartext und einem Weg nach vorn (Referenz, Baustein L).
 *
 * Jeder wiederholbare Fehler bekommt einen Wiederholen-Knopf. Ein Netzwerkfehler ohne diesen
 * Knopf zwingt den Benutzer, den ganzen Vorgang von vorn zu beginnen — das ist unnötig.
 */
@Composable
fun FehlerStreifen(
    text: String,
    beiSchliessen: () -> Unit,
    modifier: Modifier = Modifier,
    knopfText: String? = null,
    beiKnopf: (() -> Unit)? = null,
) {
    val farben = LocalKompassFarben.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Mass.randSchmal, vertical = Mass.abstandKlein)
            .background(farben.fehler.copy(alpha = 0.12f), RoundedCornerShape(Mass.radiusKlein))
            .border(1.dp, farben.fehler.copy(alpha = 0.5f), RoundedCornerShape(Mass.radiusKlein))
            .padding(Mass.abstand),
        verticalArrangement = Arrangement.spacedBy(Mass.abstandKlein),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Mass.abstandKlein)) {
            if (knopfText != null && beiKnopf != null) {
                TextButton(onClick = beiKnopf) {
                    Text(knopfText, color = MaterialTheme.colorScheme.primary)
                }
            }
            TextButton(onClick = beiSchliessen) {
                Text("Schliessen", color = farben.textGedaempft)
            }
        }
    }
}

/** Ein Hinweisstreifen in Goldtönen — für Bestätigungen und Zwischenstände. */
@Composable
fun HinweisStreifen(text: String, beiSchliessen: () -> Unit, modifier: Modifier = Modifier) {
    val farben = LocalKompassFarben.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Mass.randSchmal, vertical = Mass.abstandKlein)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                RoundedCornerShape(Mass.radiusKlein),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                RoundedCornerShape(Mass.radiusKlein),
            )
            .padding(horizontal = Mass.abstand, vertical = Mass.abstandKlein),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Mass.abstandKlein))
        TextButton(onClick = beiSchliessen) {
            Text("OK", color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Kleine Beschriftung mit goldenem Rand — für „seit Version" und ähnliche Angaben. */
@Composable
fun Merkzeichen(
    text: String,
    modifier: Modifier = Modifier,
    farbe: Color? = null,
    gefuellt: Boolean = false,
) {
    val ton = farbe ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .background(
                color = if (gefuellt) ton else ton.copy(alpha = 0.10f),
                shape = RoundedCornerShape(6.dp),
            )
            .border(1.dp, ton.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (gefuellt) MaterialTheme.colorScheme.onPrimary else ton,
        )
    }
}

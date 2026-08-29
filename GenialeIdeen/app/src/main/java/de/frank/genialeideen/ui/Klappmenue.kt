package de.frank.genialeideen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.theme.LocalGold

/** Ein Eintrag in einem Klappmenü. */
data class KlappEintrag(
    val id: String,
    val name: String,
    val gruppe: String = "",
    val zusatz: String = "",
)

/**
 * Die Stimmenauswahl als Klappmenü — für alle Anbieter dieselbe Bedienung.
 *
 * Der Play-Knopf je Eintrag spielt eine Kostprobe und übernimmt die Stimme dabei; sonst hörte
 * man eine Stimme, die danach gar nicht liest.
 */
@Composable
fun Klappmenue(
    beschriftung: String,
    eintraege: List<KlappEintrag>,
    gewaehlt: String,
    aufWahl: (String) -> Unit,
    modifier: Modifier = Modifier,
    laedt: Boolean = false,
    leerText: String = "Hier ist noch keine Stimme hinterlegt.",
    aufProbe: ((String) -> Unit)? = null,
    aufFavorit: ((String) -> Unit)? = null,
    favoriten: Set<String> = emptySet(),
    aufNeuLaden: (() -> Unit)? = null,
) {
    val gold = LocalGold.current
    var offen by remember { mutableStateOf(false) }
    val aktuell = eintraege.firstOrNull { it.id == gewaehlt }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                beschriftung,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = gold.textGedaempft,
            )
            if (aufNeuLaden != null) {
                Box(
                    modifier = Modifier.size(30.dp).druckEffekt(aufNeuLaden),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Stimmen neu laden",
                        tint = gold.textGedaempft,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .druckEffekt { if (eintraege.isNotEmpty()) offen = true }
                    .clip(RoundedCornerShape(14.dp))
                    .background(gold.eingabefeld)
                    .border(1.dp, if (offen) gold.primaer else gold.rahmen, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        laedt -> "Wird geladen …"
                        eintraege.isEmpty() -> leerText
                        aktuell != null -> aktuell.name
                        else -> "Noch keine gewählt"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (aktuell != null) gold.textPrimaer else gold.textGedaempft,
                    maxLines = 1,
                )
                if (aktuell != null && aktuell.zusatz.isNotBlank()) {
                    Text(
                        aktuell.zusatz,
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.textGedaempft,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Stimme wählen",
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp).rotate(if (offen) 180f else 0f),
                )
            }

            DropdownMenu(
                expanded = offen,
                onDismissRequest = { offen = false },
                modifier = Modifier
                    .background(gold.flaecheErhoeht)
                    .heightIn(max = 420.dp),
            ) {
                var letzteGruppe: String? = null
                eintraege.forEach { eintrag ->
                    if (eintrag.gruppe.isNotBlank() && eintrag.gruppe != letzteGruppe) {
                        letzteGruppe = eintrag.gruppe
                        Text(
                            eintrag.gruppe,
                            style = MaterialTheme.typography.labelSmall,
                            color = gold.primaer,
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    eintrag.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (eintrag.id == gewaehlt) gold.primaer else gold.textPrimaer,
                                )
                                if (eintrag.zusatz.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        eintrag.zusatz,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = gold.textGedaempft,
                                    )
                                }
                            }
                        },
                        onClick = {
                            aufWahl(eintrag.id)
                            offen = false
                        },
                        leadingIcon = {
                            if (eintrag.id == gewaehlt) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Ausgewählt",
                                    tint = gold.primaer,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Spacer(Modifier.size(18.dp))
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (aufFavorit != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .druckEffekt { aufFavorit(eintrag.id) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            if (eintrag.id in favoriten) {
                                                Icons.Default.Star
                                            } else {
                                                Icons.Default.StarBorder
                                            },
                                            contentDescription = "Als Favorit merken",
                                            tint = if (eintrag.id in favoriten) {
                                                gold.primaer
                                            } else {
                                                gold.textGedaempft
                                            },
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                if (aufProbe != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .druckEffekt {
                                                aufProbe(eintrag.id)
                                                offen = false
                                            }
                                            .clip(CircleShape)
                                            .border(1.dp, gold.rahmen, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Probe abspielen",
                                            tint = gold.primaer,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

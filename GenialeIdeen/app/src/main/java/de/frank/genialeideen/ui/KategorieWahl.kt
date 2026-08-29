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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.ui.theme.LocalGold

/**
 * Die Kategoriewahl: ein Klappmenü über alle vorhandenen Kategorien und ein Plus, das eine
 * neue anlegt. Die frisch getippte Kategorie gibt es ab dann für alle Ideen (Baustein P).
 */
@Composable
fun KategorieWahl(
    kategorien: List<KategorieEntity>,
    gewaehlt: Long?,
    aufWahl: (Long?) -> Unit,
    aufNeueKategorie: (String, (Long?) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    beschriftung: String = "Kategorie",
    laedt: Boolean = false,
) {
    val gold = LocalGold.current
    var offen by remember { mutableStateOf(false) }
    var neuOffen by remember { mutableStateOf(false) }
    var neuerName by remember { mutableStateOf("") }
    val aktuell = kategorien.firstOrNull { it.id == gewaehlt }

    Column(modifier) {
        Text(
            if (laedt) "$beschriftung — die KI sucht eine …" else beschriftung,
            style = MaterialTheme.typography.labelSmall,
            color = gold.textGedaempft,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .druckEffekt { offen = true }
                        .clip(RoundedCornerShape(14.dp))
                        .background(gold.eingabefeld)
                        .border(
                            1.dp,
                            if (offen) gold.primaer else gold.rahmen,
                            RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            laedt -> "gleich da"
                            aktuell != null -> aktuell.name
                            else -> "Ohne Kategorie"
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (aktuell != null) gold.textPrimaer else gold.textGedaempft,
                        maxLines = 1,
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Kategorie wählen",
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
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Ohne Kategorie",
                                style = MaterialTheme.typography.bodyMedium,
                                color = gold.textGedaempft,
                            )
                        },
                        onClick = {
                            aufWahl(null)
                            offen = false
                        },
                    )
                    kategorien.forEach { kategorie ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    kategorie.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (kategorie.id == gewaehlt) gold.primaer else gold.textPrimaer,
                                )
                            },
                            onClick = {
                                aufWahl(kategorie.id)
                                offen = false
                            },
                            leadingIcon = {
                                if (kategorie.id == gewaehlt) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = gold.primaer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                } else {
                                    Spacer(Modifier.size(18.dp))
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .druckEffekt { neuOffen = !neuOffen }
                    .clip(RoundedCornerShape(14.dp))
                    .background(gold.flaecheErhoeht)
                    .border(1.dp, gold.primaer.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Neue Kategorie anlegen",
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (neuOffen) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(gold.eingabefeld)
                    .border(1.dp, gold.primaer, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (neuerName.isBlank()) {
                        Text(
                            "Name der neuen Kategorie",
                            style = MaterialTheme.typography.bodyMedium,
                            color = gold.textGedaempft,
                        )
                    }
                    BasicTextField(
                        value = neuerName,
                        onValueChange = { neuerName = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = gold.textPrimaer,
                            fontWeight = FontWeight.Medium,
                        ),
                        cursorBrush = SolidColor(gold.primaer),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    modifier = Modifier.size(32.dp).druckEffekt {
                        val name = neuerName.trim()
                        if (name.isNotBlank()) {
                            aufNeueKategorie(name) { id -> if (id != null) aufWahl(id) }
                        }
                        neuerName = ""
                        neuOffen = false
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Kategorie übernehmen",
                        tint = gold.primaer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

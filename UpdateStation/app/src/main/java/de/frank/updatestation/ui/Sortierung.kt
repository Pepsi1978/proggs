package de.frank.updatestation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import de.frank.updatestation.AppEintrag
import java.text.Collator
import java.util.Locale

/** Wonach die App-Liste sortiert wird. [standardAbsteigend] = Richtung, wenn das Kriterium neu gewählt wird. */
enum class SortKriterium(val titel: String, val standardAbsteigend: Boolean) {
    AKTUALITAET("Aktualität", true),
    NAME("Name", false),
    GROESSE("Größe", true),
}

/** Startstandard bei jedem App-Start: zuletzt aktualisierte App oben. Wird bewusst nicht gespeichert. */
data class Sortierung(val kriterium: SortKriterium = SortKriterium.AKTUALITAET, val absteigend: Boolean = true) {
    /** Kurztext der Richtung, passend zum Kriterium. */
    val richtungText: String
        get() = when (kriterium) {
            SortKriterium.NAME -> if (absteigend) "Z–A" else "A–Z"
            SortKriterium.GROESSE -> if (absteigend) "Größte zuerst" else "Kleinste zuerst"
            SortKriterium.AKTUALITAET -> if (absteigend) "Neueste zuerst" else "Älteste zuerst"
        }

    fun umgekehrt() = copy(absteigend = !absteigend)

    fun sortiere(liste: List<AppEintrag>): List<AppEintrag> {
        val name = Comparator<AppEintrag> { a, b -> SAMMLER.compare(a.label, b.label) }
        return when (kriterium) {
            SortKriterium.NAME -> liste.sortedWith(if (absteigend) name.reversed() else name)
            SortKriterium.GROESSE -> {
                val nachGroesse = compareBy<AppEintrag> { it.fund.manifest.groesse }
                liste.sortedWith((if (absteigend) nachGroesse.reversed() else nachGroesse).then(name))
            }
            // Zeitpunkt, zu dem die angezeigte Version aufs Handy kam; nicht installierte Apps ohne Zeitpunkt stehen immer unten.
            SortKriterium.AKTUALITAET -> {
                val (mit, ohne) = liste.partition { (it.installiertAm ?: 0L) > 0L }
                val nachZeit = compareBy<AppEintrag> { it.installiertAm }
                mit.sortedWith((if (absteigend) nachZeit.reversed() else nachZeit).then(name)) + ohne.sortedWith(name)
            }
        }
    }

    companion object {
        private val SAMMLER: Collator = Collator.getInstance(Locale.GERMANY).apply { strength = Collator.SECONDARY }
    }
}

/** Sortierknopf (Kriterium wählen) und direkt daneben der Richtungsknopf (Reihenfolge umkehren). */
@Composable
fun SortierLeiste(sortierung: Sortierung, onAendern: (Sortierung) -> Unit) {
    var offen by remember { mutableStateOf(false) }
    val form = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { offen = true },
                shape = form,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Sortieren nach ${sortierung.kriterium.titel}"
                    stateDescription = "Kriterium wählen: Name, Größe oder Aktualität"
                    role = Role.DropdownList
                },
            ) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sortieren: ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(sortierung.kriterium.titel, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                SortKriterium.entries.forEach { k ->
                    val gewaehlt = k == sortierung.kriterium
                    DropdownMenuItem(
                        text = { Text(k.titel) },
                        trailingIcon = {
                            if (gewaehlt) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.semantics { selected = gewaehlt },
                        onClick = {
                            offen = false
                            if (!gewaehlt) onAendern(Sortierung(k, k.standardAbsteigend))
                        },
                    )
                }
            }
        }
        OutlinedButton(
            onClick = { onAendern(sortierung.umgekehrt()) },
            shape = form,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.semantics {
                contentDescription = "Reihenfolge umkehren"
                stateDescription = "Aktuell: ${sortierung.richtungText}"
            },
        ) {
            Icon(
                if (sortierung.absteigend) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = null, modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(sortierung.richtungText, style = MaterialTheme.typography.labelLarge)
        }
    }
}

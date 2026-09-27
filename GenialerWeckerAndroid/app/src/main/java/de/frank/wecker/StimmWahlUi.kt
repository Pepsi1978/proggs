package de.frank.wecker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.wecker.design.DesignDialog

/**
 * Die eine Stimmauswahl der App als aufklappbares Feld: alle natürlichen Stimmen der Handysprache, jede sofort
 * wählbar und anhörbar. Mit Stern (oder langem Drücken) wird eine Stimme Favorit; Favoriten stehen immer oben.
 * [gewaehlt] ist die Stimmkennung; leer = Vorgabe. [vorgabeZeile] zeigt „Standard aus den Einstellungen“ (Wecker-Editor).
 */
@Composable
fun StimmWahlListe(vm: WeckerViewModel, sprache: String, gewaehlt: String, vorgabeZeile: String? = null, waehlen: (String) -> Unit) {
    var offen by remember { mutableStateOf(false) }
    val aktuell = PremiumKatalog.finde(gewaehlt)
    val beschriftung = aktuell?.let { "${it.name} · ${if (it.weiblich) "weiblich" else "männlich"}" }
        ?: vorgabeZeile?.let { "Standard · $it" } ?: "Stimme wählen"
    GoldKnopf(beschriftung, { offen = true }, Modifier.fillMaxWidth(), beschreibung = "Stimme auswählen, aktuell $beschriftung",
        symbol = { Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(18.dp)) })
    if (offen) StimmDialog(vm, sprache, gewaehlt, vorgabeZeile, { waehlen(it) }, { vm.stopPreview(); offen = false })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StimmDialog(vm: WeckerViewModel, sprache: String, gewaehlt: String, vorgabeZeile: String?,
    waehlen: (String) -> Unit, schliessen: () -> Unit) {
    val gold = LocalGold.current
    var favoriten by remember { mutableStateOf(vm.settings.stimmFavoriten) }
    fun stern(id: String) {
        favoriten = if (id in favoriten) favoriten - id else favoriten + id
        vm.settings.stimmFavoriten = favoriten
    }
    // Reihenfolge nur beim Öffnen festlegen: ein neuer Stern lässt die Liste nicht unter dem Finger springen.
    val reihenfolge = remember { PremiumKatalog.fuer(sprache, vm.geraeteRegion).sortedByDescending { it.id in vm.settings.stimmFavoriten } }
    DesignDialog(
        titel = "Stimme wählen",
        aufSchliessen = schliessen,
        bestaetigung = { StillerKnopf("Fertig", schliessen) },
        inhalt = {
            Text("Antippen wählt, ▶ hört Probe, ☆ merkt als Favorit – Favoriten stehen oben.",
                style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
            LazyColumn(Modifier.heightIn(max = 460.dp).weight(1f, fill = false)) {
                if (vorgabeZeile != null) item {
                    StimmZeile(gewaehlt.isBlank(), "Standard aus den Einstellungen", vorgabeZeile, null, null, { waehlen("") }, {})
                }
                items(reihenfolge, key = { it.id }) { s ->
                    StimmZeile(s.id == gewaehlt, "${s.name} · ${if (s.weiblich) "weiblich" else "männlich"}", "${s.art} · ${s.regionName}",
                        favorit = s.id in favoriten, anhoeren = { AnhoerKnopf(vm, "stimme:${s.id}") { vm.previewVoice(sprache = sprache, stimme = s.id) } },
                        waehlen = { waehlen(s.id) }, sternUmschalten = { stern(s.id) })
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StimmZeile(aktiv: Boolean, titel: String, unter: String, favorit: Boolean?, anhoeren: (@Composable () -> Unit)?,
    waehlen: () -> Unit, sternUmschalten: () -> Unit) {
    val gold = LocalGold.current
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp)
        .background(if (aktiv) Brush.horizontalGradient(listOf(gold.primaer.copy(alpha = .18f), gold.primaer.copy(alpha = .03f)))
            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)), RoundedCornerShape(14.dp))
        .combinedClickable(onClick = waehlen, onLongClick = if (favorit != null) sternUmschalten else null,
            onLongClickLabel = "Als Favorit markieren")
        .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (favorit != null) IconButton(sternUmschalten, Modifier.semantics { contentDescription = if (favorit) "Favorit entfernen" else "Als Favorit markieren" }) {
            Icon(if (favorit) Icons.Default.Star else Icons.Default.StarBorder, null,
                tint = if (favorit) Color(0xFFF5B301) else gold.textGedaempft)
        } else RadioButton(aktiv, null, Modifier.padding(horizontal = 12.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(titel, style = MaterialTheme.typography.bodyLarge, fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal,
                color = if (aktiv) gold.primaer else gold.textPrimaer, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (unter.isNotBlank()) Text(unter, style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        anhoeren?.invoke()
    }
}

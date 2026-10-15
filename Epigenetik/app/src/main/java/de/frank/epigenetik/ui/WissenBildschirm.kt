package de.frank.epigenetik.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.data.Kategorie
import de.frank.epigenetik.data.Recherche
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.data.normalisiere
import de.frank.epigenetik.ui.theme.Chip
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.glas

private enum class Bereich(val anzeige: String) { MITTEL("Mittel"), WIRKUNGEN("Wirkungen"), FRAGEN("Fragen") }
private enum class Sortierung(val anzeige: String) { POTENZIAL("Stärkste"), NAME("A–Z"), NEU("Neueste") }

@Composable
fun WissenBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.recherchen.collectAsState()
    val wirkungen by vm.wirkungen.collectAsState()
    var bereich by rememberSaveable { mutableStateOf(Bereich.MITTEL) }
    var kategorie by rememberSaveable { mutableStateOf<Kategorie?>(null) }
    var nurFavoriten by rememberSaveable { mutableStateOf(false) }
    var sortierung by rememberSaveable { mutableStateOf(Sortierung.POTENZIAL) }
    var suche by rememberSaveable { mutableStateOf("") }

    val schalterJe = wirkungen.groupBy { it.rechercheId }
    val k = normalisiere(suche)
    val liste = alle.asSequence()
        .filter {
            when (bereich) {
                Bereich.MITTEL -> it.typEnum == Typ.MITTEL
                Bereich.WIRKUNGEN -> it.typEnum == Typ.WIRKUNG
                Bereich.FRAGEN -> it.typEnum == Typ.FRAGE
            }
        }
        .filter { kategorie == null || bereich != Bereich.MITTEL || it.kategorieEnum == kategorie }
        .filter { !nurFavoriten || it.favorit }
        .filter { r ->
            k.isBlank() || r.schluessel.contains(k) || r.aliase.contains(k) || normalisiere(r.kurz).contains(k) ||
                schalterJe[r.id].orEmpty().any { w -> normalisiere(w.schalter).contains(k) || normalisiere(Schalterkatalog.zu(w.schalter)?.name.orEmpty()).contains(k) }
        }
        .sortedWith(
            when (sortierung) {
                Sortierung.POTENZIAL -> compareByDescending<Recherche> { it.potenzial }.thenBy { it.name }
                Sortierung.NAME -> compareBy { it.name.lowercase() }
                Sortierung.NEU -> compareByDescending { it.aktualisiert }
            },
        ).toList()

    Seitenliste(vm, "Wissen", "Deine Bibliothek · ${alle.count { it.typEnum == Typ.MITTEL && it.fertig }} Mittel recherchiert") {
        item(key = "filter") {
            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().glas(f, 99.dp, 0.6f, f.flaecheStark).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = f.textLeise, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (suche.isEmpty()) Text("Suchen – Name, Gen, Wirkung …", color = f.textSchwach, fontSize = 15.sp)
                        BasicTextField(suche, { suche = it }, singleLine = true, textStyle = TextStyle(color = f.text, fontSize = 15.sp), cursorBrush = SolidColor(f.primaer), modifier = Modifier.fillMaxWidth())
                    }
                }
                ChipReihe { Bereich.entries.forEach { b -> Chip(b.anzeige, bereich == b) { bereich = b } } }
                if (bereich == Bereich.MITTEL) ChipReihe {
                    Chip("Alle", kategorie == null) { kategorie = null }
                    Kategorie.entries.forEach { c -> Chip("${c.emoji} ${c.anzeige}", kategorie == c) { kategorie = if (kategorie == c) null else c } }
                    Chip("Favoriten", nurFavoriten, icon = Icons.Rounded.Star) { nurFavoriten = !nurFavoriten }
                }
                ChipReihe { Sortierung.entries.forEach { s -> Chip(s.anzeige, sortierung == s) { sortierung = s } } }
            }
        }
        if (liste.isEmpty()) item(key = "leer") {
            LeerHinweis(
                "🧬", if (suche.isBlank()) "Noch nichts hier" else "Nichts gefunden",
                if (suche.isBlank()) "Gib auf der Startseite ein Mittel, eine Wirkung oder eine Frage ein – die Ergebnisse sammeln sich hier." else "Tippe auf Start und lass „$suche“ recherchieren.",
            )
        }
        items(liste, key = { it.id }) { r ->
            val top = schalterJe[r.id].orEmpty().sortedByDescending { it.staerke }.take(3)
            val unter = when {
                r.offen -> "Wird recherchiert …"
                r.fehler.isNotBlank() && !r.fertig -> r.fehler
                top.isNotEmpty() -> top.joinToString(" · ") { "${Schalterkatalog.zu(it.schalter)?.name ?: it.schalter} ${if (it.richtung.startsWith("hemm")) "▼" else if (it.richtung.startsWith("aktiv")) "▲" else "◆"}" }
                else -> r.kurz
            }
            MittelZeile(
                r.emoji, r.name + if (r.favorit) " ★" else "", unter, r.potenzial.takeIf { r.typEnum == Typ.MITTEL && r.fertig },
                rechts = if (r.imStack) ({ Etikett("Stack", f.primaer) }) else null,
            ) { vm.oeffne(r.id) }
        }
    }
}

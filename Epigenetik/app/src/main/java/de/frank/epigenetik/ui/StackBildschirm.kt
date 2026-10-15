package de.frank.epigenetik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.data.Gruppe
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.data.objekte
import de.frank.epigenetik.data.strings
import de.frank.epigenetik.ui.theme.Chip
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas
import org.json.JSONObject

@Composable
fun StackBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.recherchen.collectAsState()
    val wirkungen by vm.wirkungen.collectAsState()
    val live by vm.live.collectAsState()
    var hinzufuegen by rememberSaveable { mutableStateOf(false) }
    val stack = alle.filter { it.typEnum == Typ.MITTEL && it.imStack }.sortedByDescending { it.potenzial }
    val kandidaten = alle.filter { it.typEnum == Typ.MITTEL && it.fertig && !it.imStack }.sortedBy { it.name.lowercase() }
    val analyse = alle.firstOrNull { it.typEnum == Typ.STACK }
    val stackIds = stack.map { it.id }.toSet()
    val stackWirkungen = wirkungen.filter { it.rechercheId in stackIds }
    // Abdeckung offline: ein Schalter gilt als abgedeckt, wenn mindestens ein Stack-Mittel ihn günstig bewegt.
    val abgedeckt = Schalterkatalog.alle.filter { s -> stackWirkungen.any { it.schalter == s.id && Schalterkatalog.guenstig(s, it.richtung) != false } }.map { it.id }.toSet()

    Seitenliste(vm, "Mein Stack", "${stack.size} Mittel · ${abgedeckt.size} von ${Schalterkatalog.alle.size} Schaltern abgedeckt") {
        item(key = "aktionen") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlasKnopf(if (hinzufuegen) "Fertig" else "Mittel hinzufügen", Icons.Rounded.Add, Modifier.weight(1f), aktiv = hinzufuegen) { hinzufuegen = !hinzufuegen }
                HauptKnopf("Analysieren", Icons.Rounded.Analytics, Modifier.weight(1f)) { vm.stackAnalysieren() }
            }
        }
        if (hinzufuegen) item(key = "kandidaten") {
            Block("Aus deiner Bibliothek") {
                if (kandidaten.isEmpty()) Absatz("Alle recherchierten Mittel sind schon im Stack. Recherchiere auf der Startseite weitere.", leise = true, groesse = 13)
                ChipFluss { kandidaten.forEach { r -> Chip("${r.emoji} ${r.name}", false) { vm.stack(r) } } }
            }
        }
        if (stack.isEmpty()) item(key = "leer") {
            LeerHinweis("🧪", "Dein Stack ist leer", "Füge hinzu, was du täglich nimmst oder tust. Die App zeigt dann, welche Schalter abgedeckt sind, und die KI prüft Synergien, Lücken und Risiken.")
        }
        items(stack, key = { "s${it.id}" }) { r ->
            MittelZeile(r.emoji, r.name, if (r.fertig) r.kurz else "Wird recherchiert …", r.potenzial.takeIf { r.fertig }, rechts = {
                Box(Modifier.size(40.dp).antippen { vm.stack(r) }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.RemoveCircleOutline, "Aus dem Stack", tint = f.textLeise) }
            }) { vm.oeffne(r.id) }
        }
        if (stack.isNotEmpty()) item(key = "abdeckung") {
            Block("Abdeckung der Schalter") {
                Gruppe.entries.forEach { g ->
                    val schalter = Schalterkatalog.alle.filter { it.gruppe == g }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.emoji, fontSize = 15.sp, modifier = Modifier.width(26.dp))
                        Text(g.anzeige, color = f.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            schalter.forEach { s ->
                                Box(
                                    Modifier.size(14.dp).glas(f, 4.dp, 0f, if (s.id in abgedeckt) f.primaer else f.textSchwach.copy(alpha = 0.25f), rand = false)
                                        .antippen(haptik = false) { vm.oeffne(Seite.SchalterSeite(s.id)) },
                                )
                            }
                        }
                    }
                }
                Text("Leuchtende Felder: von deinem Stack günstig bewegt. Antippen zeigt den Schalter.", color = f.textLeise, fontSize = 11.sp)
            }
        }
        if (analyse != null) item(key = "analyse") {
            when {
                analyse.offen -> LaufKarte(analyse, live[analyse.id]) { }
                analyse.fertig -> StackAnalyse(vm, analyse)
                else -> Block("Stack-Analyse") { Text(analyse.fehler, color = f.gefahr, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun StackAnalyse(vm: AppViewModel, r: de.frank.epigenetik.data.Recherche) {
    val f = LocalFarben.current
    val o = runCatching { JSONObject(r.json) }.getOrNull() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Block("Stack-Analyse", rechts = {
            RundKnopf(Icons.Rounded.VolumeUp, "Vorlesen", 36.dp, aktiv = vm.vorleseId == r.id) { vm.vorlesen(r) }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PotenzialRing(o.optInt("bewertung"), 64.dp, 6.dp)
                Spacer(Modifier.width(14.dp))
                Absatz(o.optString("zusammenfassung"))
            }
        }
        o.optJSONArray("synergien").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            Block("Synergien") { l.forEach { Punkt("${it.optJSONArray("mittel").strings().joinToString(" + ")}: ${it.optString("warum")}", f.erfolg) } }
        }
        o.optJSONArray("luecken").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            Block("Lücken – was fehlt") {
                l.forEach { lu ->
                    val s = Schalterkatalog.zu(lu.optString("schalter"))
                    Text(s?.name ?: lu.optString("schalter"), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Absatz(lu.optString("warum"), leise = true, groesse = 13)
                    ChipFluss { lu.optJSONArray("vorschlaege").strings().forEach { n -> Chip(n, false, icon = Icons.Rounded.Add) { vm.mittelAntippen(n) } } }
                }
            }
        }
        o.optJSONArray("doppelungen").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            Block("Doppelungen") { l.forEach { Punkt("${it.optJSONArray("mittel").strings().joinToString(" + ")}: ${it.optString("hinweis")}", f.sekundaer) } }
        }
        o.optJSONArray("risiken").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            Block("Risiken & Wechselwirkungen") { l.forEach { Punkt("${it.optJSONArray("mittel").strings().joinToString(" + ")}: ${it.optString("hinweis")} (${it.optString("schwere")})", f.gefahr) } }
        }
        o.optJSONArray("tagesplan").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            Block("Tagesplan") {
                l.forEach { t ->
                    Column(Modifier.fillMaxWidth().glas(f, 14.dp, 0.3f, f.flaecheStark).padding(12.dp)) {
                        Text(t.optString("zeit"), color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(t.optJSONArray("mittel").strings().joinToString(" · "), color = f.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        t.optString("hinweis").takeIf { it.isNotBlank() }?.let { Text(it, color = f.textLeise, fontSize = 12.sp) }
                    }
                }
            }
        }
        o.optJSONArray("weglassen").objekte().takeIf { it.isNotEmpty() }?.let { l ->
            Block("Eher weglassen") { l.forEach { Punkt("${it.optString("name")}: ${it.optString("warum")}", f.textLeise) } }
        }
        o.optString("fazit").takeIf { it.isNotBlank() }?.let { Block("Fazit") { Absatz(it) } }
    }
}

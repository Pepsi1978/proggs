package de.frank.epigenetik.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.data.Gruppe
import de.frank.epigenetik.data.Recherche
import de.frank.epigenetik.data.Richtung
import de.frank.epigenetik.data.SchalterWirkung
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas

@Composable
fun AtlasBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.recherchen.collectAsState()
    val wirkungen by vm.wirkungen.collectAsState()
    val nachId = alle.associateBy { it.id }
    val je = wirkungen.filter { nachId[it.rechercheId]?.fertig == true }.groupBy { it.schalter }
    val abgedeckt = je.keys.size

    Seitenliste(vm, "Schalter-Atlas", "$abgedeckt von ${Schalterkatalog.alle.size} Schaltern mit recherchierten Mitteln") {
        item(key = "erklaerung") {
            Block("So funktioniert der Atlas") {
                Absatz("Jeder Schalter ist ein Gen, Enzym oder Signalweg, der die Aktivität deiner Gene steuert. ▲ heißt: günstig aktivieren, ▼: günstig hemmen. Tippe einen Schalter an, um zu sehen, welche deiner Mittel ihn bewegen – ohne neue Recherche.", leise = true, groesse = 13)
            }
        }
        Gruppe.entries.forEach { g ->
            val schalter = Schalterkatalog.alle.filter { it.gruppe == g }
            item(key = "g${g.name}") {
                Block("${g.emoji}  ${g.anzeige}") {
                    schalter.forEach { s ->
                        val treffer = je[s.id].orEmpty().sortedByDescending { it.staerke }
                        val guenstig = treffer.count { Schalterkatalog.guenstig(s, it.richtung) != false }
                        Row(Modifier.fillMaxWidth().glas(f, 14.dp, 0.3f, f.flaecheStark).antippen { vm.oeffne(Seite.SchalterSeite(s.id)) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.guenstig.pfeil, color = if (s.guenstig == Richtung.HEMMEN) f.sekundaer else f.primaer, fontSize = 15.sp, modifier = Modifier.width(22.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (treffer.isEmpty()) "Noch kein Mittel – antippen zum Recherchieren"
                                    else treffer.take(3).joinToString(" · ") { nachId[it.rechercheId]?.name.orEmpty() },
                                    color = f.textLeise, fontSize = 12.sp, maxLines = 1,
                                )
                            }
                            if (treffer.isNotEmpty()) Etikett("$guenstig/${treffer.size}", if (guenstig > 0) f.erfolg else f.textLeise)
                        }
                    }
                }
            }
        }
        val eigene = alle.filter { it.typEnum == Typ.WIRKUNG }
        if (eigene.isNotEmpty()) {
            item(key = "wirk_titel") { Ueberschrift("Deine Wirkungs-Recherchen") }
            items(eigene, key = { "w${it.id}" }) { r -> MittelZeile("🧬", r.name, if (r.offen) "Wird recherchiert …" else r.kurz, null) { vm.oeffne(r.id) } }
        }
    }
}

/** Ein Schalter: was er tut und welche recherchierten Mittel ihn wie bewegen. */
@Composable
fun SchalterBildschirm(vm: AppViewModel, id: String) {
    val f = LocalFarben.current
    val s = Schalterkatalog.zu(id) ?: return
    val alle by vm.recherchen.collectAsState()
    val wirkungen by vm.wirkungen.collectAsState()
    val nachId = alle.associateBy { it.id }
    val treffer: List<Pair<SchalterWirkung, Recherche>> = wirkungen.filter { it.schalter == s.id }
        .mapNotNull { w -> nachId[w.rechercheId]?.takeIf { it.fertig }?.let { w to it } }
        .sortedByDescending { it.first.staerke }
    val gut = treffer.filter { Schalterkatalog.guenstig(s, it.first.richtung) != false }
    val schlecht = treffer.filter { Schalterkatalog.guenstig(s, it.first.richtung) == false }
    val wirkungsName = "${s.name} ${if (s.guenstig == Richtung.HEMMEN) "hemmen" else "aktivieren"}"

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", 40.dp) { vm.zurueck() }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${s.gruppe.emoji} ${s.gruppe.anzeige}", color = f.textLeise, fontSize = 12.sp)
                        Text(s.name, color = f.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            item {
                Block("Was er tut") {
                    Absatz(s.info)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Etikett("${s.guenstig.pfeil} günstig: ${s.guenstig.anzeige}", f.primaer)
                        Spacer(Modifier.width(8.dp))
                        Etikett("ID ${s.id}")
                    }
                }
            }
            item {
                val vorhanden = alle.firstOrNull { it.typEnum == Typ.WIRKUNG && it.name.contains(s.name, true) }
                if (vorhanden != null) GlasKnopf("Wirkungs-Recherche öffnen", Icons.Rounded.Search, Modifier.fillMaxWidth()) { vm.oeffne(vorhanden.id) }
                else HauptKnopf("Beste Mittel dafür recherchieren", Icons.Rounded.Search, Modifier.fillMaxWidth()) { vm.recherchiere(Typ.WIRKUNG, listOf(wirkungsName)) }
            }
            if (treffer.isEmpty()) item {
                LeerHinweis("🔍", "Noch kein Mittel bekannt", "Keins deiner recherchierten Mittel wirkt laut Recherche auf ${s.name}. Starte oben die Suche nach den besten Mitteln.")
            }
            if (gut.isNotEmpty()) item {
                Block("Wirkt günstig (${gut.size})") { gut.forEach { (w, r) -> TrefferZeile(vm, w, r) } }
            }
            if (schlecht.isNotEmpty()) item {
                Block("Wirkt in die ungünstige Richtung (${schlecht.size})") { schlecht.forEach { (w, r) -> TrefferZeile(vm, w, r) } }
            }
        }
    }
}

@Composable
private fun TrefferZeile(vm: AppViewModel, w: SchalterWirkung, r: Recherche) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().glas(f, 14.dp, 0.3f, f.flaecheStark).antippen { vm.oeffne(r.id) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RichtungsPfeil(w.schalter, w.richtung, 16.dp)
            Text("${r.emoji} ${r.name}", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
            if (r.imStack) Etikett("Stack", f.primaer)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StaerkeBalken(w.staerke, f.primaer, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Etikett(w.evidenz.ifBlank { "?" })
        }
        if (w.mechanismus.isNotBlank()) Text(w.mechanismus, color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun Platzhalter() = Box(Modifier.size(1.dp))

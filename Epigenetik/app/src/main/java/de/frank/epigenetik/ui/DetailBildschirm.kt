package de.frank.epigenetik.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.data.Bericht
import de.frank.epigenetik.data.Kategorie
import de.frank.epigenetik.data.Recherche
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Status
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.data.Zielkatalog
import de.frank.epigenetik.data.normalisiere
import de.frank.epigenetik.data.objekte
import de.frank.epigenetik.data.strings
import de.frank.epigenetik.ui.theme.Chip
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas
import org.json.JSONObject

@Composable
fun DetailBildschirm(vm: AppViewModel, id: Long) {
    val f = LocalFarben.current
    val alle by vm.recherchen.collectAsState()
    val live by vm.live.collectAsState()
    val r = alle.firstOrNull { it.id == id }
    val context = LocalContext.current
    var loeschenFragen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "kopf") {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", 40.dp) { vm.zurueck() }
                    Spacer(Modifier.weight(1f))
                    if (r != null && r.fertig) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RundKnopf(if (vm.vorleseId == r.id) Icons.Rounded.StopCircle else Icons.Rounded.VolumeUp, "Vorlesen", 40.dp, aktiv = vm.vorleseId == r.id) { vm.vorlesen(r) }
                            if (r.typEnum == Typ.MITTEL) RundKnopf(if (r.favorit) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Favorit", 40.dp, aktiv = r.favorit) { vm.favorit(r) }
                            RundKnopf(Icons.Rounded.Share, "Teilen", 40.dp) {
                                val text = Bericht.text(r)
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Auswertung teilen"))
                            }
                            RundKnopf(Icons.Rounded.Refresh, "Neu recherchieren", 40.dp) { vm.neuRecherchieren(r) }
                        }
                    }
                }
            }
            if (r == null) {
                item { LeerHinweis("🗑️", "Nicht mehr vorhanden", "Diese Recherche wurde gelöscht.") }
                return@LazyColumn
            }
            item(key = "titel") { Titelkarte(vm, r) }
            when {
                r.offen -> item(key = "lauf") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LaufKarte(r, live[r.id]) {}
                        Absatz("Die KI durchsucht gerade das Internet und wertet Studien aus. Das dauert meist 1–4 Minuten. Du kannst die App verlassen – du bekommst eine Benachrichtigung, wenn es fertig ist.", leise = true, groesse = 13)
                        GlasKnopf("Abbrechen", Icons.Rounded.Close) { vm.abbrechen(r) }
                    }
                }
                r.statusEnum == Status.FEHLER -> item(key = "fehler") {
                    Block("Nicht geklappt", icon = Icons.Rounded.Warning) {
                        Absatz(r.fehler)
                        HauptKnopf("Nochmal versuchen", Icons.Rounded.Refresh, Modifier.fillMaxWidth()) { vm.neuRecherchieren(r) }
                    }
                }
                else -> {
                    val o = runCatching { JSONObject(r.json) }.getOrNull() ?: JSONObject()
                    when (r.typEnum) {
                        Typ.MITTEL -> mittelInhalt(vm, r, o)
                        Typ.WIRKUNG, Typ.ZIEL -> wirkungInhalt(vm, r, o)
                        Typ.FRAGE -> frageInhalt(vm, o)
                        Typ.STACK -> item { Absatz(Bericht.text(r)) }
                    }
                    quellen(o, context)
                }
            }
            item(key = "loeschen") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                    if (!loeschenFragen) GlasKnopf("Löschen", Icons.Rounded.Delete, farbe = f.gefahr) { loeschenFragen = true }
                    else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlasKnopf("Wirklich löschen", Icons.Rounded.Delete, aktiv = true, farbe = f.gefahr) { vm.loeschen(r) }
                        GlasKnopf("Behalten") { loeschenFragen = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun Titelkarte(vm: AppViewModel, r: Recherche) {
    val f = LocalFarben.current
    val o = runCatching { JSONObject(r.json) }.getOrNull()
    Column(Modifier.fillMaxWidth().einblenden().glas(f, erhoeht = 1.4f, toenung = f.primaer).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (r.typEnum) {
                        Typ.MITTEL -> "${r.kategorieEnum.emoji} ${r.kategorieEnum.anzeige}" + (o?.optString("klasse")?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
                        else -> "${r.typEnum.emoji} ${r.typEnum.anzeige}"
                    },
                    color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
                Text(o?.optString("titel")?.takeIf { it.isNotBlank() && r.typEnum != Typ.MITTEL } ?: r.name, color = f.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
            }
            if (r.typEnum == Typ.MITTEL && r.fertig) PotenzialRing(r.potenzial, 70.dp, 7.dp)
        }
        if (r.typEnum == Typ.MITTEL && r.fertig) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Etikett("Evidenz: ${r.evidenz.ifBlank { "?" }}", f.primaer)
            Etikett("Potenzial ${r.potenzial}/100")
            if (r.quelle == "jarvis") Etikett("von Jarvis")
        }
        if (r.kurz.isNotBlank() && r.fertig) Absatz(r.kurz)
        if (r.typEnum == Typ.MITTEL && r.fertig) {
            if (r.imStack) GlasKnopf("In deinem Stack", Icons.Rounded.Check, aktiv = true) { vm.stack(r) }
            else GlasKnopf("Zu meinem Stack hinzufügen", Icons.Rounded.Science) { vm.stack(r) }
        }
    }
}

private fun LazyListScope.mittelInhalt(vm: AppViewModel, r: Recherche, o: JSONObject) {
    o.optString("fazit").takeIf { it.isNotBlank() }?.let { item(key = "fazit") { Block("Fazit") { Absatz(it) } } }
    val schalter = o.optJSONArray("schalter").objekte()
    if (schalter.isNotEmpty()) item(key = "schalter") {
        val f = LocalFarben.current
        Block("Epigenetische Schalter (${schalter.size})") {
            schalter.forEach { s ->
                val id = s.optString("id")
                val sch = Schalterkatalog.zu(id)
                Column(
                    Modifier.fillMaxWidth().glas(f, 14.dp, 0.3f, f.flaecheStark).antippen { sch?.let { vm.oeffne(Seite.SchalterSeite(it.id)) } }.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RichtungsPfeil(id, s.optString("richtung"), 16.dp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(sch?.name ?: id, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("${s.optString("richtung")} · ${sch?.gruppe?.anzeige.orEmpty()}", color = f.textLeise, fontSize = 12.sp)
                        }
                        Etikett(s.optString("evidenz").ifBlank { "?" })
                    }
                    StaerkeBalken(s.optInt("staerke"), f.primaer, Modifier.fillMaxWidth())
                    s.optString("mechanismus").takeIf { it.isNotBlank() }?.let { Text(it, color = f.text, fontSize = 13.sp, lineHeight = 18.sp) }
                    s.optString("dosis_bezug").takeIf { it.isNotBlank() }?.let { Text("Dosis: $it", color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp) }
                }
            }
            Text("Grün: günstige Richtung · Rot: ungünstig · Antippen öffnet den Schalter im Atlas", color = f.textLeise, fontSize = 11.sp)
        }
    }
    val sonstige = o.optJSONArray("sonstige_schalter").objekte()
    if (sonstige.isNotEmpty()) item(key = "sonstige") {
        Block("Weitere Gene und Signalwege") { sonstige.forEach { Punkt("${it.optString("name")} ${it.optString("richtung")}: ${it.optString("mechanismus")}") } }
    }
    o.optJSONArray("mechanismen").strings().takeIf { it.isNotEmpty() }?.let { l -> item(key = "mech") { Block("Mechanismen") { l.forEach { Punkt(it) } } } }
    item(key = "einnahme") { EinnahmeKarte(o) }
    o.optJSONArray("synergien").objekte().takeIf { it.isNotEmpty() }?.let { l ->
        item(key = "syn") {
            val f = LocalFarben.current
            Block("Synergien – passt gut zu") {
                l.forEach { s ->
                    Row(Modifier.fillMaxWidth().antippen { vm.mittelAntippen(s.optString("mittel")) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, null, tint = f.primaer, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(s.optString("mittel"), color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(s.optString("begruendung"), color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
                Text("Antippen: öffnen oder recherchieren", color = f.textLeise, fontSize = 11.sp)
            }
        }
    }
    val risiken = o.optJSONArray("risiken").strings()
    val ww = o.optJSONArray("wechselwirkungen").objekte()
    if (risiken.isNotEmpty() || ww.isNotEmpty()) item(key = "risiko") {
        val f = LocalFarben.current
        Block("Risiken & Wechselwirkungen", icon = Icons.Rounded.Warning) {
            risiken.forEach { Punkt(it, f.gefahr) }
            ww.forEach { w ->
                val schwer = w.optString("schwere").lowercase()
                Punkt("${w.optString("mit")}: ${w.optString("art")} (${w.optString("schwere")})", if (schwer.startsWith("schwer")) f.gefahr else if (schwer.startsWith("mittel")) f.sekundaer else f.textLeise)
            }
        }
    }
    val kontro = o.optJSONArray("kontroversen").strings()
    val offen = o.optJSONArray("offene_fragen").strings()
    if (kontro.isNotEmpty() || offen.isNotEmpty()) item(key = "offen") {
        Block("Ehrlich gesagt – noch unklar") {
            kontro.forEach { Punkt(it) }
            offen.forEach { Punkt(it) }
        }
    }
    o.optJSONArray("ziele").strings().takeIf { it.isNotEmpty() }?.let { l ->
        item(key = "ziele") {
            Block("Hilft besonders bei") {
                ChipFluss {
                    l.forEach { z ->
                        val v = Zielkatalog.alle.firstOrNull { it.id.equals(z, true) }
                        Chip("${v?.emoji ?: "🎯"} ${v?.name ?: z}", false) { vm.recherchiere(Typ.ZIEL, listOf(v?.name ?: z)) }
                    }
                }
            }
        }
    }
    item(key = "notiz") {
        Block("Deine Notiz") { Feld("", r.notiz, einzeilig = false, hinweis = "z. B. Marke, eigene Erfahrung, Dosis …") { vm.notiz(r, it) } }
    }
}

@Composable
private fun EinnahmeKarte(o: JSONObject) {
    val d = o.optJSONObject("dosierung")
    val e = o.optJSONObject("einnahme")
    val b = o.optJSONObject("bioverfuegbarkeit")
    if (d == null && e == null && b == null) return
    Block("Dosierung & Einnahme") {
        d?.let {
            Zeile("Empfohlen", it.optString("empfohlen"))
            Zeile("In Studien", it.optString("studien"))
            Zeile("Obergrenze", it.optString("obergrenze"))
        }
        e?.let {
            ChipFluss {
                it.optString("timing").takeIf { t -> t.isNotBlank() }?.let { t -> Etikett("🕒 $t") }
                if (it.optBoolean("mit_fett")) Etikett("🥑 mit Fett")
                if (it.optBoolean("nuechtern")) Etikett("🌅 nüchtern")
                it.optString("zyklisch").takeIf { z -> z.isNotBlank() }?.let { z -> Etikett("🔁 $z") }
            }
            it.optString("hinweise").takeIf { h -> h.isNotBlank() }?.let { h -> Absatz(h, leise = true, groesse = 13) }
        }
        b?.let {
            Zeile("Bioverfügbarkeit", it.optString("bewertung"))
            Zeile("Beste Form", it.optString("beste_form"))
            it.optJSONArray("verstaerker").strings().takeIf { v -> v.isNotEmpty() }?.let { v -> Zeile("Verstärker", v.joinToString()) }
        }
    }
}

@Composable
private fun Zeile(titel: String, wert: String) {
    if (wert.isBlank()) return
    val f = LocalFarben.current
    Row {
        Text(titel, color = f.textLeise, fontSize = 13.sp, modifier = Modifier.width(118.dp))
        Text(wert, color = f.text, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

private fun LazyListScope.wirkungInhalt(vm: AppViewModel, r: Recherche, o: JSONObject) {
    val text = o.optString("erklaerung").ifBlank { o.optString("beschreibung") }
    if (text.isNotBlank()) item(key = "erkl") { Block("Worum es geht") { Absatz(text) } }
    val schalter = o.optJSONArray("schalter").objekte().map { it.optString("id") to it } + o.optJSONArray("schalter_ids").strings().map { it to null }
    if (schalter.isNotEmpty()) item(key = "schalter") {
        val f = LocalFarben.current
        Block("Die entscheidenden Schalter") {
            schalter.forEach { (id, s) ->
                val sch = Schalterkatalog.zu(id)
                Row(Modifier.fillMaxWidth().glas(f, 14.dp, 0.3f, f.flaecheStark).antippen { sch?.let { vm.oeffne(Seite.SchalterSeite(it.id)) } }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sch?.guenstig?.pfeil ?: "◆", color = f.primaer, fontSize = 15.sp, modifier = Modifier.width(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sch?.name ?: id, color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        s?.optString("warum")?.takeIf { it.isNotBlank() }?.let { Text(it, color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp) }
                    }
                    s?.optInt("relevanz", 0)?.takeIf { it > 0 }?.let { Etikett("$it") }
                }
            }
        }
    }
    rangliste(vm, o, "supplements", "Nahrungsergänzungsmittel", Kategorie.SUPPLEMENT)
    rangliste(vm, o, "nahrung", "Nahrung", Kategorie.NAHRUNG)
    rangliste(vm, o, "verhalten", "Verhalten", Kategorie.VERHALTEN)
    o.optString("kombination").takeIf { it.isNotBlank() }?.let { item(key = "komb") { Block("Beste Kombination") { Absatz(it) } } }
    o.optJSONArray("stack_vorschlag").objekte().takeIf { it.isNotEmpty() }?.let { l ->
        item(key = "stackvorschlag") {
            val f = LocalFarben.current
            Block("Stack-Vorschlag für dieses Ziel") {
                l.forEach { s ->
                    Row(Modifier.fillMaxWidth().antippen { vm.mittelAntippen(s.optString("name")) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💊", fontSize = 18.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(s.optString("name"), color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOf(s.optString("dosis"), s.optString("timing")).filter { it.isNotBlank() }.joinToString(" · "), color = f.textLeise, fontSize = 12.sp)
                            s.optString("warum").takeIf { it.isNotBlank() }?.let { Text(it, color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp) }
                        }
                    }
                }
                GlasKnopf("Alle recherchieren", Icons.Rounded.Science) {
                    vm.recherchiere(Typ.MITTEL, l.map { it.optString("name") }.filter { it.isNotBlank() })
                }
            }
        }
    }
    o.optJSONArray("zu_meiden").objekte().takeIf { it.isNotEmpty() }?.let { l ->
        item(key = "meiden") { val f = LocalFarben.current; Block("Besser meiden") { l.forEach { Punkt("${it.optString("name")}: ${it.optString("warum")}", f.gefahr) } } }
    }
    o.optJSONArray("messbar").objekte().takeIf { it.isNotEmpty() }?.let { l ->
        item(key = "messbar") { Block("So misst du den Erfolg") { l.forEach { Punkt("${it.optString("marker")}: ${it.optString("warum")}") } } }
    }
    o.optString("fazit").takeIf { it.isNotBlank() }?.let { item(key = "fazit") { Block("Fazit") { Absatz(it) } } }
    item(key = "bibliothek") { AusBibliothek(vm, schalter.map { it.first }) }
}

/** Was die eigene Bibliothek zu diesen Schaltern schon weiß (offline). */
@Composable
private fun AusBibliothek(vm: AppViewModel, schalterIds: List<String>) {
    val alle by vm.recherchen.collectAsState()
    val wirkungen by vm.wirkungen.collectAsState()
    val ids = schalterIds.mapNotNull { Schalterkatalog.zu(it)?.id }.toSet()
    if (ids.isEmpty()) return
    val nachId = alle.associateBy { it.id }
    val treffer = wirkungen.filter { it.schalter in ids && nachId[it.rechercheId]?.fertig == true }
        .groupBy { it.rechercheId }.mapNotNull { (id, w) -> nachId[id]?.let { it to w } }
        .sortedByDescending { (_, w) -> w.sumOf { it.staerke } }
    if (treffer.isEmpty()) return
    Block("Aus deiner Bibliothek") {
        treffer.take(10).forEach { (r, w) ->
            MittelZeile(r.emoji, r.name, w.joinToString(" · ") { "${Schalterkatalog.zu(it.schalter)?.name ?: it.schalter} ${it.richtung}" }, r.potenzial) { vm.oeffne(r.id) }
        }
    }
}

private fun LazyListScope.rangliste(vm: AppViewModel, o: JSONObject, feld: String, titel: String, kategorie: Kategorie) {
    val l = o.optJSONArray(feld).objekte()
    if (l.isEmpty()) return
    item(key = "rang_$feld") {
        val f = LocalFarben.current
        val alle by vm.recherchen.collectAsState()
        val bekannt = alle.filter { it.typEnum == Typ.MITTEL }.flatMap { r -> (r.aliase.split('|') + r.schluessel).map { it to r } }.toMap()
        Block("${kategorie.emoji}  $titel") {
            l.forEachIndexed { i, s ->
                val name = s.optString("name")
                val vorhanden = bekannt[normalisiere(name)]
                Column(Modifier.fillMaxWidth().glas(f, 14.dp, 0.3f, f.flaecheStark).antippen { vm.mittelAntippen(name, kategorie) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = f.primaer, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(24.dp))
                        Text(name, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Etikett(s.optString("evidenz").ifBlank { "?" })
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (vorhanden?.fertig == true) Icons.Rounded.OpenInNew else Icons.Rounded.Add,
                            if (vorhanden != null) "Öffnen" else "Recherchieren", tint = f.primaer, modifier = Modifier.size(18.dp),
                        )
                    }
                    StaerkeBalken(s.optInt("staerke"), f.primaer, Modifier.fillMaxWidth())
                    s.optString("mechanismus").takeIf { it.isNotBlank() }?.let { Text(it, color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp) }
                    s.optString("dosis_hinweis").takeIf { it.isNotBlank() }?.let { Text("Dosis: $it", color = f.textLeise, fontSize = 12.sp) }
                }
            }
            Text("Antippen: vorhandene Auswertung öffnen oder sofort recherchieren", color = f.textLeise, fontSize = 11.sp)
        }
    }
}

private fun LazyListScope.frageInhalt(vm: AppViewModel, o: JSONObject) {
    o.optString("antwort").takeIf { it.isNotBlank() }?.let { a ->
        item(key = "antwort") { Block("Antwort") { a.split("\n\n").filter { it.isNotBlank() }.forEach { Absatz(it.trim()) } } }
    }
    o.optJSONArray("empfehlungen").objekte().takeIf { it.isNotEmpty() }?.let { l ->
        item(key = "empf") {
            val f = LocalFarben.current
            Block("Empfehlungen") {
                l.forEach { s ->
                    Row(Modifier.fillMaxWidth().antippen { vm.mittelAntippen(s.optString("name")) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, null, tint = f.primaer, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(s.optString("name"), color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(s.optString("warum"), color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.quellen(o: JSONObject, context: android.content.Context) {
    val l = o.optJSONArray("quellen").objekte()
    if (l.isEmpty()) return
    item(key = "quellen") {
        val f = LocalFarben.current
        Block("Quellen (${l.size})") {
            l.forEach { q ->
                val url = q.optString("url")
                Row(
                    Modifier.fillMaxWidth().then(if (url.startsWith("http")) Modifier.antippen { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } } else Modifier).padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(q.optString("titel"), color = f.text, fontSize = 13.sp, lineHeight = 17.sp)
                        Text(listOf(q.optInt("jahr", 0).takeIf { it > 0 }?.toString(), q.optString("typ").takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "), color = f.textLeise, fontSize = 11.sp)
                    }
                    if (url.startsWith("http")) Icon(Icons.Rounded.OpenInNew, "Öffnen", tint = f.primaer, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

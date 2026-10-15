package de.frank.epigenetik.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.data.Kategorie
import de.frank.epigenetik.data.Recherche
import de.frank.epigenetik.data.Status
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.ki.LiveStand
import de.frank.epigenetik.ui.theme.Chip
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas
import de.frank.epigenetik.ui.theme.knopf3d
import kotlinx.coroutines.delay

private val START_MITTEL = listOf(
    "Nikotinamid-Ribosid", "Ashwagandha", "Sulforaphan", "Resveratrol", "Spermidin", "Fisetin", "Curcumin",
    "Omega-3", "Vitamin D3", "EGCG", "Quercetin", "Berberin", "Alpha-Ketoglutarat", "Magnesium",
)
private val START_NAHRUNG = listOf("Brokkolisprossen", "Grüner Tee", "Olivenöl extra vergine", "Beeren", "Kurkuma")
private val START_VERHALTEN = listOf("Intervallfasten", "Krafttraining", "Sauna", "Kältetraining", "Tiefschlaf", "Meditation")
private val START_WIRKUNGEN = listOf("Sirtuine aktivieren", "NRF2 aktivieren", "Autophagie anregen", "Epigenetische Uhr verlangsamen", "NF-κB hemmen", "NAD+ erhöhen")

@Composable
fun StartBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.recherchen.collectAsState()
    val live by vm.live.collectAsState()
    val offen = alle.filter { it.offen }.sortedBy { it.erstellt }
    val fehler = alle.filter { it.statusEnum == Status.FEHLER }.take(3)
    val neu = alle.filter { it.fertig && !it.gelesen && it.typEnum != Typ.STACK }.take(6)
    val mittel = alle.filter { it.typEnum == Typ.MITTEL && it.fertig }
    val top = mittel.sortedByDescending { it.potenzial }.take(5)
    val fragen = alle.filter { it.typEnum == Typ.FRAGE && it.fertig }.take(4)

    Seitenliste(vm, "Epigenetik", "Gene an- und abschalten") {
        if (!vm.kiVerbunden) item(key = "ki") {
            Row(
                Modifier.fillMaxWidth().einblenden().glas(f, toenung = f.primaer).antippen { vm.oeffne(Seite.Einstellungen) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Link, null, tint = f.primaer)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Mit ChatGPT verbinden", color = f.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Einmal anmelden – danach recherchiert die App im Hintergrund.", color = f.textLeise, fontSize = 12.sp)
                }
                Icon(Icons.Rounded.ArrowForward, null, tint = f.textLeise)
            }
        }

        item(key = "eingabe") { EingabeKarte(vm) }

        if (offen.isNotEmpty()) {
            item(key = "laeuft_titel") { Ueberschrift("Läuft gerade") }
            items(offen, key = { "l${it.id}" }) { r -> LaufKarte(r, live[r.id]) { vm.oeffne(r.id) } }
        }
        if (fehler.isNotEmpty()) items(fehler, key = { "f${it.id}" }) { r ->
            MittelZeile(r.emoji, r.name, r.fehler, null, rechts = { Icon(Icons.Rounded.ErrorOutline, null, tint = f.gefahr) }) { vm.oeffne(r.id) }
        }
        if (neu.isNotEmpty()) {
            item(key = "neu_titel") { Ueberschrift("Neu für dich") }
            items(neu, key = { "n${it.id}" }) { r ->
                MittelZeile(r.emoji, r.name, r.kurz, r.potenzial.takeIf { r.typEnum == Typ.MITTEL }, Modifier.einblenden()) { vm.oeffne(r.id) }
            }
        }
        if (top.isNotEmpty()) {
            item(key = "top") {
                Block("Deine stärksten Mittel") {
                    top.forEachIndexed { i, r ->
                        Row(Modifier.fillMaxWidth().antippen { vm.oeffne(r.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", color = f.primaer, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(26.dp))
                            Text(r.emoji, fontSize = 18.sp)
                            Text(r.name, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
                            StaerkeBalken(r.potenzial, f.primaer, Modifier.width(70.dp))
                            Text("${r.potenzial}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp).width(26.dp))
                        }
                    }
                }
            }
        }
        item(key = "ideen") {
            val vorhanden = alle.map { it.schluessel }.toSet()
            Block("Schnell starten – ein Tipp genügt") {
                Text("Nahrungsergänzung", color = f.textLeise, fontSize = 12.sp)
                ChipFluss { START_MITTEL.filter { de.frank.epigenetik.data.normalisiere(it) !in vorhanden }.forEach { n -> Chip(n, false) { vm.recherchiere(Typ.MITTEL, listOf(n), Kategorie.SUPPLEMENT) } } }
                Text("Nahrung", color = f.textLeise, fontSize = 12.sp)
                ChipFluss { START_NAHRUNG.filter { de.frank.epigenetik.data.normalisiere(it) !in vorhanden }.forEach { n -> Chip(n, false) { vm.recherchiere(Typ.MITTEL, listOf(n), Kategorie.NAHRUNG) } } }
                Text("Verhalten", color = f.textLeise, fontSize = 12.sp)
                ChipFluss { START_VERHALTEN.filter { de.frank.epigenetik.data.normalisiere(it) !in vorhanden }.forEach { n -> Chip(n, false) { vm.recherchiere(Typ.MITTEL, listOf(n), Kategorie.VERHALTEN) } } }
                Text("Wirkungen – was schaltet sie an?", color = f.textLeise, fontSize = 12.sp)
                ChipFluss { START_WIRKUNGEN.filter { de.frank.epigenetik.data.normalisiere(it) !in vorhanden }.forEach { n -> Chip(n, false) { vm.recherchiere(Typ.WIRKUNG, listOf(n)) } } }
            }
        }
        if (fragen.isNotEmpty()) {
            item(key = "fragen_titel") { Ueberschrift("Deine Fragen") }
            items(fragen, key = { "q${it.id}" }) { r -> MittelZeile("💬", r.name, r.kurz, null) { vm.oeffne(r.id) } }
        }
    }
}

@Composable
private fun EingabeKarte(vm: AppViewModel) {
    val f = LocalFarben.current
    val typ = vm.erkannterTyp
    Column(Modifier.fillMaxWidth().einblenden().glas(f, erhoeht = 1.4f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Was möchtest du wissen?", color = f.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).heightIn(min = 52.dp).glas(f, 18.dp, 0f, f.flaecheStark).padding(horizontal = 14.dp, vertical = 14.dp)) {
                if (vm.eingabe.isEmpty()) Text(
                    when (vm.aufnahme) { Aufnahme.LAEUFT -> "Ich höre zu …"; Aufnahme.VERARBEITET -> "Verstehe …"; else -> "z. B. Ashwagandha, Sirtuine aktivieren …" },
                    color = f.textSchwach, fontSize = 16.sp,
                )
                BasicTextField(
                    vm.eingabe, vm::eingabeAendern,
                    textStyle = TextStyle(color = f.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(f.primaer),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.absenden() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(10.dp))
            MikroKnopf(vm, DiktatZiel.START)
        }
        Text("Erkannt als", color = f.textLeise, fontSize = 12.sp)
        ChipReihe {
            listOf(Typ.MITTEL, Typ.WIRKUNG, Typ.ZIEL, Typ.FRAGE).forEach { t ->
                Chip("${t.emoji} ${t.anzeige}", typ == t) { vm.waehleTyp(t) }
            }
        }
        AnimatedVisibility(typ == Typ.MITTEL) {
            ChipReihe {
                Kategorie.entries.forEach { k -> Chip("${k.emoji} ${k.anzeige}", vm.erkannteKategorie == k && vm.eingabe.isNotBlank()) { vm.waehleKategorie(k) } }
            }
        }
        Text(
            when (typ) {
                Typ.MITTEL -> "Recherchiert genau, welche Gene und Signalwege es an- oder abschaltet. Mehrere mit Komma trennen."
                Typ.WIRKUNG -> "Findet Nahrungsergänzung, Nahrung und Verhalten, die diese Wirkung erzielen."
                Typ.ZIEL -> "Ermittelt die epigenetischen Schalter für dein Ziel und die besten Mittel dafür."
                else -> "Beantwortet deine Frage mit aktueller Forschung und deiner Bibliothek."
            },
            color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp,
        )
        HauptKnopf("Recherchieren", Icons.Rounded.ArrowForward, Modifier.fillMaxWidth()) { vm.absenden() }
    }
}

/** Runder Mikrofon-Knopf; pulsiert während der Aufnahme. */
@Composable
fun MikroKnopf(vm: AppViewModel, ziel: DiktatZiel) {
    val f = LocalFarben.current
    val laeuft = vm.aufnahme == Aufnahme.LAEUFT
    val puls = rememberInfiniteTransition(label = "puls").animateFloat(1f, 1.12f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "p")
    Box(
        Modifier.size(52.dp).scale(if (laeuft) puls.value else 1f)
            .knopf3d(if (laeuft) f.gefahr else f.primaer, if (laeuft) f.gefahr.copy(alpha = 0.8f) else f.tertiaer, 99.dp, f.dunkel)
            .antippen { vm.mikroTippen(ziel) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (laeuft) Icons.Rounded.Stop else Icons.Rounded.Mic, "Spracheingabe", tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/** Karte einer laufenden Recherche mit fließendem Balken, Zeit und empfangenen Zeichen. */
@Composable
fun LaufKarte(r: Recherche, stand: LiveStand?, aktion: () -> Unit) {
    val f = LocalFarben.current
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); jetzt = System.currentTimeMillis() } }
    Column(Modifier.fillMaxWidth().einblenden().glas(f, 18.dp, 0.8f, f.flaecheStark, toenung = f.primaer).antippen(aktion = aktion).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.emoji, fontSize = 20.sp)
            Text(r.name, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
            if (r.quelle == "jarvis") Etikett("von Jarvis", f.primaer)
        }
        LaufBalken()
        val text = when {
            stand == null && r.statusEnum == Status.WARTET -> r.fehler.ifBlank { "Wartet auf einen freien Platz …" }
            stand == null -> "Startet …"
            else -> {
                val s = ((jetzt - stand.start) / 1000).coerceAtLeast(0)
                "${stand.schritt} · ${s / 60}:${(s % 60).toString().padStart(2, '0')}" + if (stand.zeichen > 0) " · ${stand.zeichen} Zeichen" else ""
            }
        }
        Text(text, color = f.textLeise, fontSize = 12.sp)
    }
}

@Composable
fun SchliessenKnopf(aktion: () -> Unit) = RundKnopf(Icons.Rounded.Close, "Schließen", 36.dp, aktion = aktion)

package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.jarvis.faehigkeit.RueckfrageStand
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/*
 * Rückfragen einer Programmier-Sitzung (AskUserQuestion oder freie Frage am Zugende): im Chat eine kompakte Karte mit Stand,
 * beantwortet wird im Vollbild. Kennbuchstaben passend zur tatsächlichen Zahl der Optionen; dazu immer „Eigene Antwort“ mit
 * Textfeld als nächster Buchstabe (Claude Code bietet dieses „Other“ bei jeder Auswahlfrage selbst an), nie erfundene Optionen.
 */

/** Eine Option, wie die Sitzung sie geschickt hat; [kennung] ist A, B, C … bzw. die Nummer aus dem Text der Frage. */
private class Option(val kennung: String, val label: String, val beschreibung: String)

private class Frage(val kopf: String, val text: String, val mehrfach: Boolean, val optionen: List<Option>) {
    /** Kennung der eigenen Antwort: der nächste Buchstabe bzw. die nächste Nummer nach der letzten Option. */
    val eigeneKennung: String = optionen.lastOrNull()?.kennung?.let { k ->
        k.toIntOrNull()?.let { (it + 1).toString() } ?: k.singleOrNull()?.let { (it + 1).toString() }
    } ?: "A"
}

private class Rueckfrage(val k: JSONObject) {
    val id: String = k.optString("frage_id")
    val app: String = k.optString("app")
    val frei: Boolean = k.optBoolean("frei")
    val stand: String = RueckfrageStand.von(k)
    val antwort: String = k.optString("antwort")
    val grund: String = k.optString("grund")
    val bis: Long = k.optLong("bis")
    val fragen: List<Frage> = (k.optJSONArray("fragen") ?: JSONArray()).let { l ->
        (0 until l.length()).mapNotNull { l.optJSONObject(it) }.map { q ->
            val o = q.optJSONArray("options") ?: JSONArray()
            Frage(
                q.optString("header"), q.optString("question").trim(), q.optBoolean("multiSelect"),
                (0 until o.length()).mapNotNull { o.optJSONObject(it) }.mapIndexed { j, op ->
                    Option(op.optString("kennung").ifEmpty { ('A' + j).toString() }, op.optString("label"), op.optString("description"))
                },
            )
        }
    }
}

private fun lies(kontext: String): Rueckfrage? = runCatching { Rueckfrage(JSONObject(kontext)) }.getOrNull()?.takeIf { it.id.isNotEmpty() }

/** Stand in Worten, für Karte und Vollbild; null = offen ohne Besonderheit. */
private fun standText(r: Rueckfrage): String? = when (r.stand) {
    RueckfrageStand.SENDET -> "Wird gesendet …"
    RueckfrageStand.FEHLER -> "Nicht gesendet: " + r.grund.ifEmpty { "unbekannter Fehler" }
    RueckfrageStand.ANGENOMMEN -> "Gesendet: ${r.antwort} · die Sitzung übernimmt sie gerade"
    RueckfrageStand.BEANTWORTET -> "Beantwortet: ${r.antwort}"
    RueckfrageStand.AM_PC_BEANTWORTET -> "Am Rechner beantwortet" + (if (r.antwort.isNotEmpty()) ": ${r.antwort}" else "")
    RueckfrageStand.AM_PC -> if (r.grund == "freigegeben") "Weitergegeben: jetzt am Rechner oder in der Claude-App beantworten."
        else "Die fünf Minuten für Jarvis sind um: jetzt am Rechner oder in der Claude-App beantworten."
    RueckfrageStand.ERLEDIGT -> "Die Sitzung hat ohne Antwort aus Jarvis weitergearbeitet."
    RueckfrageStand.VERWAIST -> "Die Sitzung wartet nicht mehr auf diese Antwort." + (if (r.grund.isNotEmpty() && !r.grund.startsWith("Die Sitzung wartet")) " ${r.grund}" else "")
    else -> null
}

/** Restzeit in Jarvis, sekundengenau aufgefrischt; leer ohne Frist. */
@Composable
private fun restzeit(r: Rueckfrage): String {
    if (r.bis <= 0 || !RueckfrageStand.beantwortbar(r.stand)) return ""
    var jetzt by remember { mutableStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(r.id, r.bis) { while (jetzt < r.bis) { delay(1_000); jetzt = System.currentTimeMillis() / 1000 } }
    val rest = r.bis - jetzt
    return if (rest > 0) "In Jarvis beantwortbar, noch ${rest / 60}:${"%02d".format(rest % 60)} min" else "Frist in Jarvis vorbei, die Frage wechselt gleich an den Rechner"
}

@Composable
private fun Standzeile(r: Rueckfrage, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val text = standText(r) ?: restzeit(r).ifEmpty { return }
    val farbe = when (r.stand) {
        RueckfrageStand.BEANTWORTET, RueckfrageStand.AM_PC_BEANTWORTET, RueckfrageStand.ANGENOMMEN -> f.erfolg
        RueckfrageStand.FEHLER -> f.gefahr
        RueckfrageStand.OFFEN, RueckfrageStand.SENDET -> f.primaer
        else -> f.textLeise
    }
    val zeichen = when (r.stand) {
        RueckfrageStand.BEANTWORTET, RueckfrageStand.AM_PC_BEANTWORTET, RueckfrageStand.ANGENOMMEN -> "✓ "
        RueckfrageStand.FEHLER -> "⚠ "
        RueckfrageStand.AM_PC -> "↗ "
        else -> ""
    }
    Text(zeichen + text, modifier.semantics { stateDescription = text }, color = farbe, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
}

/** Die Karte im Chat: Frage kurz, Stand, „Beantworten“ öffnet das Vollbild (auch nach dem Schließen jederzeit wieder). */
@Composable
fun RueckfrageKarte(vm: AppViewModel, n: Nachricht) {
    val f = LocalFarben.current
    val r = remember(n.kontext) { lies(n.kontext) }
    Column(Modifier.fillMaxWidth().widthIn(max = 560.dp).glas(f, 20.dp, erhoeht = 0.6f, fuellung = f.flaecheStark, toenung = f.primaer).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text("❓ Rückfrage" + (r?.app?.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""), color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (r == null) {
            ProgrammierText(n.text, Modifier.padding(top = 6.dp), farbe = f.text)
            return@Column
        }
        if (r.frei) ProgrammierText(r.fragen.firstOrNull()?.text.orEmpty().let { if (it.length > 500) "…" + it.takeLast(500) else it }, Modifier.padding(top = 6.dp), farbe = f.text)
        else r.fragen.forEach { q ->
            Text(q.text, Modifier.padding(top = 6.dp), color = f.text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 6, overflow = TextOverflow.Ellipsis)
            Text(
                q.optionen.joinToString("  ·  ") { "${it.kennung} ${it.label}" } + "  ·  ${q.eigeneKennung} Eigene Antwort",
                Modifier.padding(top = 2.dp), color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
        Standzeile(r, Modifier.padding(top = 10.dp))
        if (RueckfrageStand.beantwortbar(r.stand)) {
            Knopf(if (r.stand == RueckfrageStand.FEHLER) "Erneut senden" else "Beantworten", Modifier.padding(top = 10.dp).fillMaxWidth()) { vm.rueckfrageOeffnen(r.id) }
        }
    }
}

/**
 * Hält das Vollbild der Rückfragen: öffnet die älteste offene Frage des Gesprächs von selbst (beim Eintreffen und beim Öffnen
 * des Gesprächs) und gleicht offene Rückfragen alle 20 Sekunden mit dem Jarvis-Server ab, solange eine darauf wartet.
 */
@Composable
fun RueckfrageFenster(vm: AppViewModel) {
    val offeneKarten = vm.gespraech.count { it.art == "rueckfrage" }
    LaunchedEffect(offeneKarten, vm.sitzungId) { vm.rueckfrageVonSelbst() }
    LaunchedEffect(vm.sitzungId) {
        while (true) {
            delay(20_000)
            val wartet = vm.gespraech.any { n ->
                n.art == "rueckfrage" && lies(n.kontext)?.stand.let { it == null || it in setOf(RueckfrageStand.OFFEN, RueckfrageStand.SENDET, RueckfrageStand.ANGENOMMEN, RueckfrageStand.FEHLER) }
            }
            if (wartet) vm.rueckfragenAbgleichen()
        }
    }
    val id = vm.rueckfrageDialog ?: return
    val nachricht = vm.gespraech.lastOrNull { it.art == "rueckfrage" && de.frank.jarvis.data.Gespraeche.rueckfrageId(it.kontext) == id } ?: return
    val r = remember(nachricht.kontext) { lies(nachricht.kontext) } ?: return
    Dialog(
        onDismissRequest = { vm.rueckfrageSchliessen() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) { Vollbild(vm, r) }
}

@Composable
private fun Vollbild(vm: AppViewModel, r: Rueckfrage) {
    val f = LocalFarben.current
    // Auswahl je Frage; Entwurf aus dem Gespräch, damit nach Schließen, Drehen oder Neustart nichts verloren ist.
    val entwurf = remember(r.id) { r.k.optJSONObject("entwurf") }
    val wahl = remember(r.id) {
        mutableStateMapOf<Int, List<String>>().apply {
            entwurf?.optJSONObject("wahl")?.let { w -> w.keys().forEach { key -> val l = w.optJSONArray(key) ?: return@forEach; put(key.toInt(), (0 until l.length()).map { l.optString(it) }) } }
        }
    }
    val eigen = remember(r.id) {
        mutableStateMapOf<Int, Boolean>().apply { entwurf?.optJSONObject("eigen")?.let { w -> w.keys().forEach { put(it.toInt(), w.optBoolean(it)) } } }
    }
    val texte = remember(r.id) {
        mutableStateMapOf<Int, String>().apply { entwurf?.optJSONObject("text")?.let { w -> w.keys().forEach { put(it.toInt(), w.optString(it)) } } }
    }
    val fehlend = remember { mutableStateListOf<Int>() }
    fun entwurfJson() = JSONObject()
        .put("wahl", JSONObject().apply { wahl.forEach { (i, l) -> put(i.toString(), JSONArray(l)) } })
        .put("eigen", JSONObject().apply { eigen.forEach { (i, b) -> put(i.toString(), b) } })
        .put("text", JSONObject().apply { texte.forEach { (i, t) -> put(i.toString(), t) } })
    fun sichern() = vm.rueckfrageEntwurf(r.id, entwurfJson())
    LaunchedEffect(r.id) {
        // Alle zwei Sekunden den Entwurf sichern, wenn er sich geändert hat.
        var zuletzt = entwurfJson().toString()
        while (true) {
            delay(2_000)
            val jetzt = entwurfJson().toString()
            if (jetzt != zuletzt) { zuletzt = jetzt; sichern() }
        }
    }
    val beantwortbar = RueckfrageStand.beantwortbar(r.stand)
    val sendet = r.stand == RueckfrageStand.SENDET

    /** Antwort je Frage; null = noch nichts (oder eigene Antwort ohne Text). */
    fun antwort(i: Int, q: Frage): String? {
        val text = texte[i].orEmpty().trim()
        val eigene = eigen[i] == true
        if (r.frei) {
            val gewaehlt = wahl[i].orEmpty().firstOrNull()?.let { label -> q.optionen.firstOrNull { it.label == label } }
            return when {
                gewaehlt != null -> "${gewaehlt.kennung}: ${gewaehlt.label}" + (if (text.isNotEmpty()) " — $text" else "")
                eigene || q.optionen.isEmpty() -> text.ifEmpty { null }
                else -> null
            }
        }
        val labels = q.optionen.map { it.label }.filter { it in wahl[i].orEmpty() }
        return when {
            q.mehrfach -> (labels + listOfNotNull(text.takeIf { eigene && it.isNotEmpty() })).takeIf { it.isNotEmpty() && (!eigene || text.isNotEmpty()) }?.joinToString(", ")
            eigene -> text.ifEmpty { null }
            else -> labels.firstOrNull()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten))).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = 720.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rückfrage der Programmier-Sitzung", color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(r.app.ifEmpty { "Programmieren" }, Modifier.semantics { heading() }, color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                }
                Rundknopf(Icons.Rounded.Close, "Schließen, später im Chat weiter beantworten") { sichern(); vm.rueckfrageSchliessen() }
            }
            Standzeile(r, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            val scroll = rememberScrollState()
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 16.dp)) {
                r.fragen.forEachIndexed { i, q ->
                    FrageBlock(
                        i, r, q, wahl[i].orEmpty(), eigen[i] == true, texte[i].orEmpty(), i in fehlend, aktiv = beantwortbar && !sendet,
                        waehlen = { label ->
                            fehlend.remove(i)
                            val bisher = wahl[i].orEmpty()
                            if (q.mehrfach) wahl[i] = if (label in bisher) bisher - label else bisher + label
                            else { wahl[i] = listOf(label); eigen[i] = false }
                        },
                        eigeneWaehlen = {
                            fehlend.remove(i)
                            if (q.mehrfach) eigen[i] = eigen[i] != true else { eigen[i] = true; wahl[i] = emptyList() }
                        },
                        textAendern = { fehlend.remove(i); texte[i] = it },
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
            // Fuß: immer sichtbar, auch über der Tastatur.
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                if (fehlend.isNotEmpty()) Text(
                    if (r.fragen.size > 1) "Bitte noch beantworten: Frage " + fehlend.sorted().joinToString(", ") { (it + 1).toString() } + "."
                    else "Bitte eine Option wählen oder eine eigene Antwort schreiben.",
                    Modifier.padding(bottom = 8.dp), color = f.gefahr, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
                if (beantwortbar || sendet) {
                    Knopf(if (sendet) "Wird gesendet …" else if (r.stand == RueckfrageStand.FEHLER) "Erneut senden" else "Antwort senden", Modifier.fillMaxWidth()) {
                        if (sendet) return@Knopf
                        val antworten = r.fragen.mapIndexed { i, q -> antwort(i, q) }
                        fehlend.clear()
                        fehlend += antworten.indices.filter { antworten[it] == null }
                        if (fehlend.isNotEmpty()) return@Knopf
                        sichern()
                        if (r.frei) vm.programmierAntwort(r.id, emptyList(), antworten.filterNotNull().joinToString("\n"))
                        else vm.programmierAntwort(r.id, antworten.map { it.orEmpty() }, "")
                    }
                    if (!sendet) Knopf("In der Claude-App beantworten", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) {
                        sichern()
                        vm.programmierAntwort(r.id, emptyList(), "", freigeben = true)
                    }
                } else Knopf("Schließen", Modifier.fillMaxWidth(), haupt = false) { vm.rueckfrageSchliessen() }
            }
        }
    }
}

@Composable
private fun FrageBlock(
    i: Int, r: Rueckfrage, q: Frage, wahl: List<String>, eigen: Boolean, text: String, fehlt: Boolean, aktiv: Boolean,
    waehlen: (String) -> Unit, eigeneWaehlen: () -> Unit, textAendern: (String) -> Unit,
) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().padding(top = if (i == 0) 8.dp else 24.dp)) {
        val ueber = listOfNotNull(
            if (r.fragen.size > 1) "Frage ${i + 1} von ${r.fragen.size}" else null,
            q.kopf.takeIf { it.isNotEmpty() },
            if (q.mehrfach) "Mehrere möglich" else null,
        ).joinToString(" · ")
        if (ueber.isNotEmpty()) Text(ueber, color = if (fehlt) f.gefahr else f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        androidx.compose.foundation.text.selection.SelectionContainer {
            if (r.frei) ProgrammierText(q.text, Modifier.padding(top = 6.dp), farbe = f.text)
            else Text(q.text, Modifier.padding(top = 6.dp), color = f.text, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
        }
        q.optionen.forEach { o ->
            val gewaehlt = o.label in wahl
            OptionFlaeche(o.kennung, o.label, o.beschreibung, gewaehlt, q.mehrfach, aktiv) { waehlen(o.label) }
            // Freie Rückfrage: Zur gewählten Option darf Frank etwas ergänzen (etwa bei „Freitext“ oder „erst besprechen“).
            if (r.frei && gewaehlt) Textfeld(text, textAendern, "Ergänzung (optional)", autofokus = false, aktiv = aktiv)
        }
        if (q.optionen.isEmpty()) {
            Textfeld(text, textAendern, "Deine Antwort", autofokus = false, aktiv = aktiv)
        } else {
            OptionFlaeche(q.eigeneKennung, "Eigene Antwort", "Freitext, geht genau so an die Sitzung", eigen, q.mehrfach, aktiv) { eigeneWaehlen() }
            if (eigen) Textfeld(text, textAendern, "Deine Antwort", autofokus = true, aktiv = aktiv)
        }
    }
}

/** Große Auswahlfläche: Kennbuchstabe, Beschriftung und Erklärung vollständig, umbrechend, auch bei großer Schrift. */
@Composable
private fun OptionFlaeche(kennung: String, label: String, beschreibung: String, gewaehlt: Boolean, mehrfach: Boolean, aktiv: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    val form = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 60.dp)
            .glas(f, 18.dp, erhoeht = if (gewaehlt) 0.8f else 0.4f, fuellung = if (gewaehlt) f.primaer.copy(alpha = 0.18f) else f.flaecheStark, toenung = if (gewaehlt) f.primaer else null)
            .then(if (gewaehlt) Modifier.border(2.dp, f.primaer, form) else Modifier)
            .then(if (aktiv) Modifier.antippen(rolle = if (mehrfach) Role.Checkbox else Role.RadioButton, aktion = aktion) else Modifier)
            .semantics { selected = gewaehlt }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp).background(if (gewaehlt) f.primaer else f.flaeche, CircleShape).padding(6.dp),
            contentAlignment = Alignment.Center,
        ) { Text(kennung, color = if (gewaehlt) f.aufPrimaer else f.primaer, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = f.text, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
            if (beschreibung.isNotEmpty()) Text(beschreibung, Modifier.padding(top = 3.dp), color = f.textLeise, fontSize = 14.sp, lineHeight = 19.sp)
        }
        Icon(
            when {
                mehrfach && gewaehlt -> Icons.Rounded.CheckBox
                mehrfach -> Icons.Rounded.CheckBoxOutlineBlank
                gewaehlt -> Icons.Rounded.RadioButtonChecked
                else -> Icons.Rounded.RadioButtonUnchecked
            },
            null, tint = if (gewaehlt) f.primaer else f.textSchwach, modifier = Modifier.size(26.dp),
        )
    }
}

/** Mehrzeiliges Textfeld für die eigene Antwort; holt sich beim Erscheinen den Fokus und scrollt über die Tastatur. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Textfeld(wert: String, aendern: (String) -> Unit, platzhalter: String, autofokus: Boolean, aktiv: Boolean) {
    val f = LocalFarben.current
    val fokus = remember { FocusRequester() }
    val sichtbar = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { if (autofokus && aktiv) runCatching { fokus.requestFocus() } }
    Box(Modifier.fillMaxWidth().padding(top = 8.dp).glas(f, 16.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).border(1.5.dp, f.primaer.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
        if (wert.isEmpty()) Text(platzhalter, color = f.textSchwach, fontSize = 17.sp)
        BasicTextField(
            wert, aendern,
            Modifier.fillMaxWidth().focusRequester(fokus).bringIntoViewRequester(sichtbar)
                .onFocusEvent { if (it.isFocused) scope.launch { delay(300); sichtbar.bringIntoView() } },
            enabled = aktiv,
            textStyle = TextStyle(color = f.text, fontSize = 17.sp, lineHeight = 24.sp),
            cursorBrush = SolidColor(f.primaer),
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

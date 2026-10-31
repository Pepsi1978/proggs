package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import de.frank.jarvis.faehigkeit.RueckfrageStand
import de.frank.jarvis.observability.Logbuch
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
    val aufgabe: String = k.optString("aufgabe")
    val auftragId: String = k.optString("auftrag")
    val frei: Boolean = k.optBoolean("frei")
    val stand: String = RueckfrageStand.von(k)
    val antwort: String = k.optString("antwort")
    val grund: String = k.optString("grund")
    val bis: Long = k.optLong("bis")
    /** Nur im Werkzeugbau: Frist ab Anzeige und was ohne Antwort gewählt wird; eine Freigabe läuft nie ab. */
    val werkzeugbau: Boolean = k.optBoolean("werkzeugbau")
    val freigabe: Boolean = k.optBoolean("freigabe")
    val empfohlen: JSONArray = k.optJSONArray("empfohlen") ?: JSONArray()
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
    if (r.werkzeugbau && r.freigabe && RueckfrageStand.beantwortbar(r.stand) && standText(r) == null) {
        Text("🔒 Braucht deine Freigabe. Ohne deine Antwort passiert das nicht, dieser Schritt bleibt blockiert.", modifier, color = f.primaer, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
        return
    }
    if (r.werkzeugbau && RueckfrageStand.beantwortbar(r.stand) && standText(r) == null) {
        val kontext = androidx.compose.ui.platform.LocalContext.current.applicationContext
        var rest by remember(r.id) { mutableStateOf(de.frank.jarvis.helfer.HelferAblauf.restSekunden(kontext, r.id)) }
        LaunchedEffect(r.id) { while (true) { delay(1_000); rest = de.frank.jarvis.helfer.HelferAblauf.restSekunden(kontext, r.id) } }
        val wahl = (0 until r.empfohlen.length()).map { r.empfohlen.optString(it) }.filter { it.isNotBlank() }.joinToString("; ").ifEmpty { "die beste Lösung nach Jarvis' Ermessen" }
        val zeit = rest?.let { if (it > 0) "Noch ${it / 60}:${"%02d".format(it % 60)} min" else "Die Zeit ist um, Jarvis entscheidet, sobald du nicht mehr schreibst" } ?: "Die 2:30 min beginnen, sobald die Frage offen ist"
        Text("$zeit. Ohne Antwort wähle ich: $wahl", modifier, color = f.primaer, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
        return
    }
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

/** Antippen schaltet um, ohne die Druck-Wirkung von antippen(): Sie kippte einen bildschirmhohen Textblock sichtbar. */
@Composable
private fun Modifier.aufklappbar(offen: Boolean, umschalten: () -> Unit): Modifier =
    clickable(remember { MutableInteractionSource() }, null, onClickLabel = if (offen) "Wieder kürzen" else "Ganz lesen", onClick = umschalten)

/**
 * Text, der auf [zeilen] Zeilen gekürzt ist: Antippen klappt ihn ganz auf, erneutes Antippen wieder zu. [voll] ist der ganze Text,
 * wenn [text] schon inhaltlich gekürzt ist. Was vollständig passt, sieht aus wie ein normaler Text und reagiert nicht.
 * [schluessel] benennt den Text dauerhaft (Frage-Kennung, Agent): Die Chat-Zeilen hängen am Index, und der Stand soll weder auf
 * eine andere Nachricht übergehen noch zuklappen, wenn sich der Text ändert.
 */
@Composable
fun AufklappText(text: String, schluessel: String, modifier: Modifier = Modifier, voll: String = "", zeilen: Int, farbe: androidx.compose.ui.graphics.Color, fontSize: TextUnit, lineHeight: TextUnit, fontWeight: FontWeight? = null) {
    val ganz = voll.ifEmpty { text }
    var offen by rememberSaveable(schluessel, key = "aufklapp:$schluessel") { mutableStateOf(false) }
    var zuLang by remember(schluessel) { mutableStateOf(false) }
    Text(
        if (offen) ganz else text,
        modifier.then(if (zuLang || ganz != text) Modifier.aufklappbar(offen) { offen = !offen } else Modifier),
        color = farbe, fontSize = fontSize, lineHeight = lineHeight, fontWeight = fontWeight,
        maxLines = if (offen) Int.MAX_VALUE else zeilen, overflow = TextOverflow.Ellipsis,
        onTextLayout = { zuLang = it.hasVisualOverflow || it.lineCount > zeilen },
    )
}

/** Der ganze Auftrag zur gekürzten Aufgabe (endet auf „ …“), im Hintergrund gelesen; leer, solange er fehlt oder nichts gekürzt ist. */
@Composable
private fun aufgabeVoll(r: Rueckfrage): String {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val voll by androidx.compose.runtime.produceState("", r.auftragId, r.aufgabe) {
        if (r.aufgabe.endsWith(" …")) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.rueckfrageVolltext(app, r.auftragId, r.aufgabe)
        }
    }
    return voll
}

/** Die Karte im Chat: Frage kurz, Stand, „Beantworten“ öffnet das Vollbild (auch nach dem Schließen jederzeit wieder). */
@Composable
fun RueckfrageKarte(vm: AppViewModel, n: Nachricht) {
    val f = LocalFarben.current
    val r = remember(n.kontext) { lies(n.kontext) }
    Column(Modifier.fillMaxWidth().widthIn(max = 560.dp).glas(f, 20.dp, erhoeht = 0.6f, fuellung = f.flaecheStark, toenung = f.primaer).padding(horizontal = 16.dp, vertical = 14.dp)) {
        // Absender vor dem Inhalt: Die Frage stellt der Programmierer, nicht die App (früher „Rückfrage · Jarvis“ bei Jarvis-Aufträgen).
        Text("❓ ${de.frank.jarvis.faehigkeit.ProgrammierChat.VON_PROGRAMMIERER} · Rückfrage" + (r?.app?.takeIf { it.isNotEmpty() }?.let { " zu $it" } ?: ""), color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (r == null) {
            ProgrammierText(n.text, Modifier.padding(top = 6.dp), farbe = f.text)
            return@Column
        }
        if (r.aufgabe.isNotEmpty()) AufklappText(r.aufgabe, "${r.id}:aufgabe", Modifier.padding(top = 4.dp), voll = aufgabeVoll(r), zeilen = 4, farbe = f.textLeise, fontSize = 14.sp, lineHeight = 19.sp)
        if (r.frei) {
            val ganz = r.fragen.firstOrNull()?.text.orEmpty()
            var offen by rememberSaveable(r.id, key = "aufklapp:${r.id}:frei") { mutableStateOf(false) }
            if (ganz.length > 500) ProgrammierText(if (offen) ganz else "…" + ganz.takeLast(500), Modifier.padding(top = 6.dp).aufklappbar(offen) { offen = !offen }, farbe = f.text)
            else ProgrammierText(ganz, Modifier.padding(top = 6.dp), farbe = f.text)
        } else r.fragen.forEachIndexed { i, q ->
            AufklappText(q.text, "${r.id}:frage:$i", Modifier.padding(top = 6.dp), zeilen = 6, farbe = f.text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
            AufklappText(
                q.optionen.joinToString("  ·  ") { "${it.kennung} ${it.label}" } + "  ·  ${q.eigeneKennung} Eigene Antwort",
                "${r.id}:optionen:$i", Modifier.padding(top = 2.dp), zeilen = 3, farbe = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp,
            )
        }
        Standzeile(r, Modifier.padding(top = 10.dp))
        if (RueckfrageStand.beantwortbar(r.stand)) {
            Knopf(if (r.stand == RueckfrageStand.FEHLER) "Erneut senden" else "Beantworten", Modifier.padding(top = 10.dp).fillMaxWidth()) { vm.rueckfrageOeffnen(r.id) }
        }
    }
}

/**
 * Steuert das Vollbild der Rückfragen ([RueckfrageEbene]): öffnet die älteste offene Frage des Gesprächs von selbst (beim Eintreffen und beim Öffnen
 * des Gesprächs) und gleicht offene Rückfragen alle 20 Sekunden mit dem Jarvis-Server ab, solange eine darauf wartet.
 */
@Composable
fun RueckfrageFenster(vm: AppViewModel) {
    val offeneKarten = vm.gespraech.count { it.art == "rueckfrage" }
    LaunchedEffect(offeneKarten, vm.sitzungId) { vm.rueckfrageVonSelbst() }
    LaunchedEffect(vm.sitzungId) {
        while (true) {
            delay(20_000)
            // Nur Rückfragen der letzten zwei Stunden, deren Ausgang noch aussteht; ein liegen gebliebener Fehler fragt nicht ewig nach.
            val grenze = System.currentTimeMillis() / 1000 - 2 * 3600
            val wartet = vm.gespraech.any { n ->
                if (n.art != "rueckfrage") return@any false
                val r = lies(n.kontext) ?: return@any false
                r.k.optLong("eingang") >= grenze && r.stand in setOf(RueckfrageStand.OFFEN, RueckfrageStand.SENDET, RueckfrageStand.ANGENOMMEN)
            }
            if (wartet) vm.rueckfragenAbgleichen()
        }
    }
}

/**
 * Das Vollbild als Ebene über der ganzen App (in [JarvisApp]), kein eigenes Dialogfenster: Ein Dialogfenster legte Android
 * bei randloser App unter der Statusleiste in voller Bildschirmhöhe an (unten fehlte ein Stück, „In der Claude-App
 * beantworten“ war unsichtbar) und verkleinerte es nicht für die Tastatur (Tests 5 und 7 am 18.10.2026, Fold 8). Hier gelten
 * dieselben Abstände für Statusleiste, Navigationsleiste und Tastatur wie im übrigen Jarvis.
 */
@Composable
fun RueckfrageEbene(vm: AppViewModel) {
    val id = vm.rueckfrageDialog ?: return
    val gespraechId = vm.rueckfrageGespraech ?: return
    val imOffenen = gespraechId == vm.sitzungId
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext
    // Nebenher (Rückfrage aus einem anderen Gespräch während einer Aufnahme): Stand laufend aus der Datei ihres Gesprächs.
    val fremd by androidx.compose.runtime.produceState<String?>(null, gespraechId, id, imOffenen) {
        // Neue Rückfrage: nie kurz den Stand der vorigen mit dem Gespräch der neuen zeigen.
        value = null
        if (!imOffenen) while (true) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.rueckfrageKontext(app, gespraechId, id)?.toString() }
            delay(2_000)
        }
    }
    val kontext = (if (imOffenen) vm.gespraech.lastOrNull { it.art == "rueckfrage" && de.frank.jarvis.data.Gespraeche.rueckfrageId(it.kontext) == id }?.kontext else fremd) ?: return
    val r = remember(kontext) { lies(kontext) } ?: return
    androidx.activity.compose.BackHandler { vm.rueckfrageSchliessen() }
    // Je Rückfrage eine eigene Ansicht: Rückt die nächste nach, landen späte Diktate und KI-Fassungen nie in ihren Feldern.
    androidx.compose.runtime.key(r.id) { Vollbild(vm, r, gespraechId) }
}

@Composable
private fun Vollbild(vm: AppViewModel, r: Rueckfrage, gespraechId: String) {
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
    fun sichern() = vm.rueckfrageEntwurf(r.id, entwurfJson(), gespraechId)
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
    // Werkzeugbau: Die 150 Sekunden beginnen erst hier, wenn Frank die Frage wirklich sieht; Tippen und Sprechen schieben die Entscheidung hinaus.
    val fristKontext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    LaunchedEffect(r.id, beantwortbar) {
        if (r.werkzeugbau && !r.freigabe && beantwortbar) de.frank.jarvis.helfer.HelferAblauf.fristStart(fristKontext, r.id, gespraechId, r.auftragId.ifEmpty { r.k.optString("bau") }, r.empfohlen)
    }
    if (r.werkzeugbau) LaunchedEffect(r.id) {
        var zuletzt = entwurfJson().toString()
        while (true) {
            delay(1_000)
            val jetzt = entwurfJson().toString()
            if (jetzt != zuletzt || vm.antwortDiktatFeld?.startsWith("${r.id}:") == true || vm.antwortFormuliertFeld?.startsWith("${r.id}:") == true) de.frank.jarvis.helfer.HelferAblauf.eingabeLaeuft(r.id)
            zuletzt = jetzt
        }
    }
    // Diagnose (29.10.2026: Vollbild offen, Tippen auf die Optionen ohne Wirkung, nach Neustart durch ein Update bei zugeklapptem,
    // gesperrtem Handy): Stand beim Öffnen und bei jedem Wechsel festhalten, damit das Logbuch zeigt, ob die Optionen gesperrt waren.
    val fensterFokus = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    val kontextApp = androidx.compose.ui.platform.LocalContext.current.applicationContext
    LaunchedEffect(r.id, r.stand) {
        val gesperrt = runCatching { kontextApp.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked }.getOrDefault(false)
        val frist = if (r.bis <= 0) "ohne Frist" else "${r.bis - System.currentTimeMillis() / 1000}s"
        Logbuch.system("Rückfrage-Vollbild", "frage=${r.id} stand=${r.stand.ifEmpty { "leer" }} aktiv=${beantwortbar && !sendet} sitzung=${vm.sitzungId} gespraech=$gespraechId bis=$frist fensterFokus=$fensterFokus sperrbildschirm=$gesperrt aufnahme=${vm.nimmtAuf}")
    }
    // Nur der verdächtige Fall: Vollbild offen, beantwortbar, aber das Fenster hat keinen Fokus (Tipps kämen dann nicht an).
    LaunchedEffect(fensterFokus) { if (!fensterFokus && beantwortbar) Logbuch.system("Rückfrage-Vollbild ohne Fensterfokus", "frage=${r.id}") }

    /** Antwort je Frage; null = noch nichts (oder eigene Antwort ohne Text). */
    fun antwort(i: Int, q: Frage): String? {
        val text = texte[i].orEmpty().trim()
        val eigene = eigen[i] == true
        if (r.frei) {
            // Alle gewählten Optionen, bei Mehrfachauswahl nicht nur die erste.
            val gewaehlte = q.optionen.filter { it.label in wahl[i].orEmpty() }
            return when {
                gewaehlte.isNotEmpty() -> gewaehlte.joinToString(", ") { "${it.kennung}: ${it.label}" } + (if (text.isNotEmpty()) " — $text" else "")
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
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten)))
            // Berührungen neben den Bedienelementen gehen nicht an den Chat darunter.
            .pointerInput(r.id) { detectTapGestures { Logbuch.system("Rückfrage-Vollbild: Tipp neben den Bedienelementen", "frage=${r.id} x=${it.x.toInt()} y=${it.y.toInt()}") } }
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
                // Die Aufgabe oben im scrollenden Teil, nicht im festen Kopf: Mit offener Tastatur bleibt der Antwortbereich groß.
                if (r.aufgabe.isNotEmpty()) AufklappText(r.aufgabe, "${r.id}:aufgabe", Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp), voll = aufgabeVoll(r), zeilen = 4, farbe = f.textLeise, fontSize = 15.sp, lineHeight = 20.sp)
                r.fragen.forEachIndexed { i, q ->
                    FrageBlock(
                        vm, ::sichern, i, r, q, wahl[i].orEmpty(), eigen[i] == true, texte[i].orEmpty(), i in fehlend, aktiv = beantwortbar && !sendet,
                        waehlen = { label ->
                            Logbuch.system("Rückfrage-Option getippt", "frage=${r.id} frage_nr=${i + 1} option=$label")
                            fehlend.remove(i)
                            val bisher = wahl[i].orEmpty()
                            if (q.mehrfach) wahl[i] = if (label in bisher) bisher - label else bisher + label
                            else { wahl[i] = listOf(label); eigen[i] = false }
                        },
                        eigeneWaehlen = {
                            Logbuch.system("Rückfrage-Option getippt", "frage=${r.id} frage_nr=${i + 1} option=Eigene Antwort")
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
                        // Erst senden, wenn Diktat und KI-Fassung dieser Rückfrage angekommen sind, sonst fehlte das Gesagte.
                        if (vm.antwortDiktatFeld?.startsWith("${r.id}:") == true) { vm.meldung = "Bitte erst die Aufnahme beenden und die Mitschrift abwarten."; return@Knopf }
                        if (vm.antwortFormuliertFeld?.startsWith("${r.id}:") == true) { vm.meldung = "Jarvis formuliert die Antwort noch, gleich fertig."; return@Knopf }
                        val antworten = r.fragen.mapIndexed { i, q -> antwort(i, q) }
                        fehlend.clear()
                        fehlend += antworten.indices.filter { antworten[it] == null }
                        if (fehlend.isNotEmpty()) return@Knopf
                        sichern()
                        if (r.frei) vm.programmierAntwort(r.id, emptyList(), antworten.filterNotNull().joinToString("\n"))
                        else vm.programmierAntwort(r.id, antworten.map { it.orEmpty() }, "")
                    }
                    if (!sendet && !r.k.optBoolean("lokal")) Knopf("In der Claude-App beantworten", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) {
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
    vm: AppViewModel, sichern: () -> Unit, i: Int, r: Rueckfrage, q: Frage, wahl: List<String>, eigen: Boolean, text: String, fehlt: Boolean, aktiv: Boolean,
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
            if (r.frei && gewaehlt && !q.mehrfach) { Textfeld(text, textAendern, "Ergänzung (optional)", autofokus = false, aktiv = aktiv); SprachLeiste(vm, "${r.id}:$i", text, textAendern, aktiv, sichern) }
        }
        // Mehrfachauswahl: ein gemeinsames Ergänzungsfeld unter allen Optionen statt eines je gewählter Option (alle mit demselben Text).
        if (r.frei && q.mehrfach && wahl.isNotEmpty()) { Textfeld(text, textAendern, "Ergänzung (optional)", autofokus = false, aktiv = aktiv); SprachLeiste(vm, "${r.id}:$i", text, textAendern, aktiv, sichern) }
        if (q.optionen.isEmpty()) {
            Textfeld(text, textAendern, "Deine Antwort", autofokus = false, aktiv = aktiv)
            SprachLeiste(vm, "${r.id}:$i", text, textAendern, aktiv, sichern)
        } else {
            OptionFlaeche(q.eigeneKennung, "Eigene Antwort", "Freitext, geht genau so an die Sitzung", eigen, q.mehrfach, aktiv) { eigeneWaehlen() }
            if (eigen) { Textfeld(text, textAendern, "Deine Antwort", autofokus = true, aktiv = aktiv); SprachLeiste(vm, "${r.id}:$i", text, textAendern, aktiv, sichern) }
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

/**
 * Unter dem Antwortfeld: Mikrofon zum Einsprechen (normale Jarvis-Transkription, hängt an den Text an) und KI-Knopf, der den
 * Text in gutes Deutsch bringt; danach „Rückgängig“, bis Frank selbst weiterschreibt oder -spricht. [feld] („frage_id:Nummer“)
 * ordnet Aufnahme und KI genau diesem Feld zu; [sichern] legt späte Ergebnisse gleich im Entwurf ab, auch nach dem Schließen.
 */
@Composable
private fun SprachLeiste(vm: AppViewModel, feld: String, text: String, aendern: (String) -> Unit, aktiv: Boolean, sichern: () -> Unit) {
    val meins = vm.antwortDiktatFeld == feld
    // Eine laufende eigene Aufnahme behält ihren Stopp-Knopf, auch wenn die Frage inzwischen nicht mehr beantwortbar ist.
    if (!aktiv && !meins) return
    val f = LocalFarben.current
    // Aktueller Text für das Diktat, das erst nach dem Transkribieren ankommt: nie über einen veralteten Stand schreiben.
    val jetzt by androidx.compose.runtime.rememberUpdatedState(text)
    var vorKi by remember { mutableStateOf<String?>(null) }
    var kiFassung by remember { mutableStateOf<String?>(null) }
    // Rückgängig nur, solange der Text noch die KI-Fassung ist; jede eigene Änderung beendet es.
    val rueckgaengig = vorKi?.takeIf { text == kiFassung }
    // Angekommene Mitschrift und KI-Fassung aus dem ViewModel übernehmen (auch wenn sie ankamen, während das Vollbild zu war).
    val diktat = vm.antwortDiktate[feld]
    LaunchedEffect(diktat) {
        if (diktat == null) return@LaunchedEffect
        vm.antwortDiktate.remove(feld)
        aendern(jetzt.trimEnd().let { if (it.isEmpty()) diktat else "$it $diktat" }); sichern()
    }
    val ki = vm.antwortKiFassungen[feld]
    LaunchedEffect(ki) {
        val (vorher, neu) = ki ?: return@LaunchedEffect
        vm.antwortKiFassungen.remove(feld)
        if (jetzt == vorher) { kiFassung = neu; vorKi = vorher; aendern(neu); sichern() }
        else vm.meldung = "Der Text wurde inzwischen geändert. Die KI-Fassung ist verworfen."
    }
    // Läuft eine Aufnahme, die nicht hier begann (Spracheingabe im Chat, anderes Antwortfeld), bleibt sie unberührt.
    val fremdeAufnahme = (vm.nimmtAuf || vm.schreibtMit) && !meins
    if (vm.antwortFormuliertFeld == feld) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Kern(24.dp, f.primaer, aktiv = true)
        Text("Jarvis formuliert …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            fremdeAufnahme -> Text("Eine andere Spracheingabe läuft im Hintergrund weiter. Antwort hier eintippen oder wählen.", Modifier.weight(1f), color = f.textLeise, fontSize = 14.sp)
            meins -> {
                if (vm.nimmtAuf) Rundknopf(Icons.Rounded.Stop, "Aufnahme beenden", f.gefahr) { vm.antwortMikrofonTippen(feld) }
                Text(if (vm.nimmtAuf) "Ich höre zu … tippe aufs Mikrofon zum Übernehmen" else if (vm.schreibtMit) "Schreibe mit …" else "Mikrofon startet …", Modifier.weight(1f), color = if (vm.nimmtAuf) f.gefahr else f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            else -> {
                Rundknopf(Icons.Rounded.Mic, "Antwort einsprechen", f.primaer) {
                    vm.antwortMikrofonTippen(feld)
                }
                if (rueckgaengig != null) Knopf("Rückgängig", Modifier.weight(1f), Icons.AutoMirrored.Rounded.Undo, haupt = false) { vorKi = null; kiFassung = null; aendern(rueckgaengig) }
                else Knopf("KI", Modifier.weight(1f), Icons.Rounded.AutoAwesome, haupt = false) {
                    vm.antwortFormulieren(feld, text)
                }
            }
        }
    }
}

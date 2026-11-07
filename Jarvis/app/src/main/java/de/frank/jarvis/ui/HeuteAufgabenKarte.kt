package de.frank.jarvis.ui

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.AppBruecke
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

private val AUFGABEN_BRUECKE: Uri = Uri.parse("content://de.frank.aufgaben.jarvis")

/** Eine Aufgabe des heutigen Bestands, so wie Geniale Aufgaben sie meldet. */
private data class HeuteAufgabe(
    val id: Long, val titel: String, val uhrzeit: String?, val prioritaet: String,
    val erledigt: Boolean, val ueberfaellig: Boolean, val taeglich: Boolean,
)

private val PRIO_RANG = listOf("HOCH", "MITTEL", "GERING", "SPAETER")

/**
 * Der heutige Bestand aus Geniale Aufgaben, wie dort unter „Heute“: offen Überfälliges und Heutiges (tägliche auf heute
 * projiziert) plus alles, was heute schon erledigt wurde. Zwei Lesungen über die Brücke: „heute“ (offen bis heute) und
 * „tag“ mit dem heutigen Datum (enthält auch die heute erledigten). Reihenfolge wie in der App: ohne Uhrzeit nach Priorität,
 * dann Termine nach Uhrzeit, erledigte ohne Uhrzeit zuletzt.
 */
private fun ladeHeute(context: android.content.Context): Pair<List<HeuteAufgabe>?, String?> {
    val heute = LocalDate.now().toString()
    val offen = AppBruecke.rufe(context, "Geniale Aufgaben", AUFGABEN_BRUECKE, "lesen", JSONObject().put("bereich", "heute").put("limit", 200), lesend = true)
    offen.optString("fehler").takeIf { it.isNotEmpty() }?.let { return null to it }
    val amTag = AppBruecke.rufe(context, "Geniale Aufgaben", AUFGABEN_BRUECKE, "lesen", JSONObject().put("bereich", "tag").put("datum", heute).put("limit", 200), lesend = true)
    amTag.optString("fehler").takeIf { it.isNotEmpty() }?.let { return null to it }
    val nachId = LinkedHashMap<Long, HeuteAufgabe>()
    fun uebernimm(liste: JSONArray?) {
        for (i in 0 until (liste?.length() ?: 0)) {
            val a = liste!!.getJSONObject(i)
            val id = a.optLong("id")
            nachId[id] = HeuteAufgabe(
                id = id, titel = a.optString("titel").ifBlank { a.optString("text") },
                uhrzeit = if (a.isNull("uhrzeit")) null else a.optString("uhrzeit"),
                prioritaet = a.optString("prioritaet"), erledigt = a.optBoolean("erledigt"),
                ueberfaellig = a.optBoolean("ueberfaellig"), taeglich = a.optString("wiederholung") == "TAEGLICH",
            )
        }
    }
    uebernimm(offen.optJSONArray("aufgaben"))
    uebernimm(amTag.optJSONArray("aufgaben"))
    val rang = { a: HeuteAufgabe -> PRIO_RANG.indexOf(a.prioritaet).let { if (it < 0) PRIO_RANG.size else it } }
    val sortiert = nachId.values.sortedWith(
        compareBy<HeuteAufgabe>({ if (it.uhrzeit != null) 1 else if (it.erledigt) 2 else 0 }, { it.uhrzeit ?: "" }, { rang(it) }),
    )
    return sortiert to null
}

/**
 * Die heutigen Aufgaben aus Geniale Aufgaben direkt unter dem Schnellstart. Ein Tipp auf eine Zeile hakt sie ab (oder öffnet
 * sie wieder), und zwar in Geniale Aufgaben selbst; danach liest die Karte den Stand neu. Ändert sich in der Aufgaben-App etwas,
 * meldet sie das über ihre Brücke, die Karte liest dann ebenfalls neu, ebenso bei jeder Rückkehr in Jarvis und um Mitternacht.
 */
@Composable
fun HeuteAufgabenKarte(vm: AppViewModel) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var aufgaben by remember { mutableStateOf<List<HeuteAufgabe>?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var anstoss by remember { mutableIntStateOf(0) }
    // Während Jarvis eine Zeile in Geniale Aufgaben umstellt, gilt dort der gewünschte Stand (kein Zurückspringen beim Neulesen).
    val unterwegs = remember { mutableStateMapOf<Long, Boolean>() }

    // Neu lesen: kurz gebündelt, denn Änderungen in der Aufgaben-App kommen oft mehrfach hintereinander.
    LaunchedEffect(anstoss) {
        if (anstoss > 0) delay(300)
        val (liste, f2) = withContext(Dispatchers.IO) { runCatching { ladeHeute(context) }.getOrElse { null to (it.message ?: "Geniale Aufgaben antwortet nicht.") } }
        if (liste != null) aufgaben = liste
        fehler = f2
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { anstoss++; onPauseOrDispose { } }
    DisposableEffect(Unit) {
        val beobachter = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { anstoss++ }
        }
        runCatching { context.contentResolver.registerContentObserver(AUFGABEN_BRUECKE, true, beobachter) }
        onDispose { runCatching { context.contentResolver.unregisterContentObserver(beobachter) } }
    }
    // Um Mitternacht beginnt ein neuer Tag: neu lesen.
    LaunchedEffect(Unit) {
        while (true) {
            val jetzt = LocalDateTime.now()
            val mitternacht = jetzt.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(5)
            delay(java.time.Duration.between(jetzt, mitternacht).toMillis().coerceAtLeast(1_000))
            anstoss++
        }
    }

    fun umschalten(a: HeuteAufgabe) {
        if (a.id in unterwegs) return
        val ziel = !a.erledigt
        unterwegs[a.id] = ziel
        scope.launch {
            val antwort = withContext(Dispatchers.IO) {
                runCatching {
                    AppBruecke.unterbrechbar {
                        AppBruecke.rufe(context, "Geniale Aufgaben", AUFGABEN_BRUECKE, "erledigen", JSONObject().put("id", a.id).put("erledigt", ziel), lesend = false)
                    }
                }.getOrElse { JSONObject().put("fehler", it.message ?: "Geniale Aufgaben antwortet nicht.") }
            }
            val problem = antwort.optString("fehler")
            if (problem.isNotEmpty()) vm.meldung = "Nicht umgestellt: $problem"
            else runCatching { de.frank.jarvis.gedaechtnis.Lagebild.nachAktion(context, "aufgabe_aendern", "Jarvis") }
            // Erst den frischen Stand lesen, dann die Sperre lösen: So springt der Haken nie kurz zurück.
            val (liste, f2) = withContext(Dispatchers.IO) { runCatching { ladeHeute(context) }.getOrElse { null to null } }
            if (liste != null) aufgaben = liste
            if (f2 != null) fehler = f2
            unterwegs.remove(a.id)
        }
    }

    val liste = aufgaben
    Karte {
        val anzeige = liste?.map { a -> unterwegs[a.id]?.let { a.copy(erledigt = it) } ?: a }
        val fertig = anzeige?.count { it.erledigt } ?: 0
        Text("Heute in Geniale Aufgaben", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                anzeige == null && fehler != null -> fehler!!
                anzeige == null -> "Wird gelesen …"
                anzeige.isEmpty() -> "Für heute steht nichts an."
                else -> "$fertig von ${anzeige.size} erledigt" + (if (fehler != null) " · $fehler" else "")
            },
            color = if (fehler != null) f.gefahr else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
        anzeige?.forEach { a -> AufgabenZeile(a) { umschalten(a) } }
    }
}

@Composable
private fun AufgabenZeile(a: HeuteAufgabe, umschalten: () -> Unit) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().antippen(aktion = umschalten).padding(vertical = 6.dp)
            .semantics { role = Role.Checkbox; stateDescription = if (a.erledigt) "erledigt" else "offen" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).then(if (a.erledigt) Modifier.knopf3d(f.erfolg, f.erfolg, 14.dp, f.dunkel) else Modifier.glas(f, 14.dp, erhoeht = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            if (a.erledigt) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                a.titel, color = if (a.erledigt) f.textLeise else f.text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                textDecoration = if (a.erledigt) TextDecoration.LineThrough else null, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            val zusatz = listOfNotNull(a.uhrzeit?.let { "$it Uhr" }, "überfällig".takeIf { a.ueberfaellig && !a.erledigt }, "täglich".takeIf { a.taeglich }).joinToString(" · ")
            if (zusatz.isNotEmpty()) Text(zusatz, color = if (a.ueberfaellig && !a.erledigt) f.gefahr else f.textLeise, fontSize = 13.sp)
        }
    }
}

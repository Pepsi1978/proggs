package de.frank.jarvis.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import de.frank.jarvis.faehigkeit.AppBruecke
import de.frank.jarvis.faehigkeit.AufgabenFaehigkeit
import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

/** Eine Aufgabe des heutigen Bestands, so wie Geniale Aufgaben sie meldet. */
private data class HeuteAufgabe(
    val id: Long, val titel: String, val uhrzeit: String?, val prioritaet: String,
    val erledigt: Boolean, val ueberfaellig: Boolean, val taeglich: Boolean,
)

private val PRIO_RANG = listOf("HOCH", "MITTEL", "GERING", "SPAETER")

/** Ab so vielen Aufgaben klappt die Karte den Rest ein („Alle zeigen“), damit der Startbildschirm nicht endlos wird. */
private const val SICHTBAR = 25

/**
 * Stand der Karte außerhalb der Komposition: Sie liegt in einer LazyColumn und verliert sonst beim Wegscrollen alles
 * (auch ein laufendes Abhaken). Lebt so lange wie der App-Prozess.
 */
private object HeuteAufgabenStand {
    var aufgaben by mutableStateOf<List<HeuteAufgabe>?>(null)
    var fehler by mutableStateOf<String?>(null)
    var alleZeigen by mutableStateOf(false)
    /** Während Jarvis eine Zeile umstellt, gilt dort der gewünschte Stand (kein Zurückspringen beim Neulesen). */
    val unterwegs = mutableStateMapOf<Long, Boolean>()
    /**
     * Heute in Jarvis abgehakte Überfällige: Geniale Aufgaben führt sie danach nur noch unter „Erledigt“ (ihr Tag liegt vor
     * heute), hier bleiben sie bis Mitternacht als erledigt stehen, damit ein versehentlicher Tipp rückgängig zu machen ist.
     */
    val abgehakt = mutableStateMapOf<Long, HeuteAufgabe>()
    var abgehaktTag = ""
    /** Nur die zuletzt begonnene Lesung darf den Stand setzen; ältere, die später fertig werden, verfallen. */
    val lesung = AtomicInteger()
    /** Bis wann Meldungen der Aufgaben-App von Jarvis' eigenem Abhaken stammen (die Karte hat dann schon neu gelesen). */
    @Volatile var eigeneAenderungBis = 0L
    /** „Aufgaben morgen“: standardmäßig zu, öffnet nur auf ausdrückliches Antippen; gelesen wird erst beim Öffnen. */
    var morgenOffen by mutableStateOf(false)
    var morgen by mutableStateOf<List<HeuteAufgabe>?>(null)
    /** Gesamtzahl laut Geniale Aufgaben (kann über dem Leselimit liegen). */
    var morgenAnzahl by mutableIntStateOf(0)
    var morgenFehler by mutableStateOf<String?>(null)
    val morgenLesung = AtomicInteger()

    /** Öffnen zeigt nie einen alten Stand (auch nicht den von gestern): Liste und Fehler leeren, die Karte liest dann frisch. */
    fun morgenUmschalten() {
        if (!morgenOffen) { morgen = null; morgenFehler = null }
        morgenOffen = !morgenOffen
    }
}

/** Leselimit je Bereich über die Brücke. */
private const val LIMIT = 200

/** Eine Aufgabe, wie die Brücke von Geniale Aufgaben sie liefert. */
private fun aufgabeAus(o: JSONObject) = HeuteAufgabe(
    id = o.optLong("id"), titel = o.optString("titel").ifBlank { o.optString("text") },
    uhrzeit = if (o.isNull("uhrzeit")) null else o.optString("uhrzeit"),
    prioritaet = o.optString("prioritaet"), erledigt = o.optBoolean("erledigt"),
    ueberfaellig = o.optBoolean("ueberfaellig"), taeglich = o.optString("wiederholung") == "TAEGLICH",
)

private fun JSONArray?.aufgaben(): List<HeuteAufgabe> = (0 until (this?.length() ?: 0)).map { aufgabeAus(this!!.getJSONObject(it)) }

/**
 * Liest über die Brücke (abbrechbar, sie blockiert sonst bis zum Zeitlimit) und liefert Ergebnis oder Fehlertext. Nur die
 * zuletzt begonnene Lesung desselben Zählers zählt; ältere, die später fertig werden, liefern null.
 */
private suspend fun <T> lesung(zaehler: AtomicInteger, laden: () -> Pair<T?, String?>): Pair<T?, String?>? {
    val nummer = zaehler.incrementAndGet()
    val ergebnis = try {
        AppBruecke.unterbrechbar(laden)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null to (e.message ?: "Geniale Aufgaben antwortet nicht.")
    }
    return if (nummer == zaehler.get()) ergebnis else null
}

/** Die offenen Aufgaben von morgen (tägliche auf morgen projiziert), wie in Geniale Aufgaben unter „Morgen“. */
private suspend fun morgenLesen(context: android.content.Context) {
    val s = HeuteAufgabenStand
    val (antwort, problem) = lesung<JSONObject>(s.morgenLesung) {
        val r = AppBruecke.rufe(context, "Geniale Aufgaben", AufgabenFaehigkeit.BRUECKE, "lesen", JSONObject().put("bereich", "morgen").put("limit", LIMIT), lesend = true)
        r.optString("fehler").takeIf { it.isNotEmpty() }?.let { null to it } ?: (r to null)
    } ?: return
    if (antwort == null) { s.morgenFehler = problem; return }
    val liste = antwort.optJSONArray("aufgaben").aufgaben().filter { !it.erledigt }
    s.morgen = sortiere(liste)
    s.morgenAnzahl = maxOf(antwort.optInt("anzahl", liste.size), liste.size)
    s.morgenFehler = null
}

/**
 * Der heutige Bestand aus Geniale Aufgaben, wie dort unter „Heute“: offen Überfälliges und Heutiges (tägliche auf heute
 * projiziert) plus alles, was heute schon erledigt wurde. Zwei Lesungen über die Brücke: „heute“ (offen bis heute) und
 * „tag“ mit dem heutigen Datum (enthält auch die heute erledigten).
 */
private fun ladeHeute(context: android.content.Context): Pair<List<HeuteAufgabe>?, String?> {
    val bruecke = AufgabenFaehigkeit.BRUECKE
    val offen = AppBruecke.rufe(context, "Geniale Aufgaben", bruecke, "lesen", JSONObject().put("bereich", "heute").put("limit", LIMIT), lesend = true)
    offen.optString("fehler").takeIf { it.isNotEmpty() }?.let { return null to it }
    val amTag = AppBruecke.rufe(context, "Geniale Aufgaben", bruecke, "lesen", JSONObject().put("bereich", "tag").put("datum", LocalDate.now().toString()).put("limit", LIMIT), lesend = true)
    amTag.optString("fehler").takeIf { it.isNotEmpty() }?.let { return null to it }
    val nachId = LinkedHashMap<Long, HeuteAufgabe>()
    (offen.optJSONArray("aufgaben").aufgaben() + amTag.optJSONArray("aufgaben").aufgaben()).forEach { nachId[it.id] = it }
    return nachId.values.toList() to null
}

/**
 * Reihenfolge wie in Geniale Aufgaben: offene ohne Termin von heute (auch überfällige mit Uhrzeit) nach Priorität, dann die
 * heutigen Termine nach Uhrzeit, zuletzt die erledigten ohne heutigen Termin.
 */
private fun sortiere(liste: List<HeuteAufgabe>): List<HeuteAufgabe> {
    val rang = { a: HeuteAufgabe -> PRIO_RANG.indexOf(a.prioritaet).let { if (it < 0) PRIO_RANG.size else it } }
    val gruppe = { a: HeuteAufgabe ->
        when {
            a.uhrzeit != null && !a.ueberfaellig -> 1
            a.erledigt -> 2
            else -> 0
        }
    }
    return liste.sortedWith(compareBy<HeuteAufgabe>({ gruppe(it) }, { if (gruppe(it) == 1) it.uhrzeit else "" }, { rang(it) }))
}

/** Liest den Stand neu; nur die zuletzt begonnene Lesung setzt ihn. Abbrechbar (die Brücke blockiert sonst bis zum Zeitlimit). */
private suspend fun neuLesen(context: android.content.Context) {
    val s = HeuteAufgabenStand
    val (liste, problem) = lesung(s.lesung) { ladeHeute(context) } ?: return
    if (liste == null) { s.fehler = problem; return }
    val heute = LocalDate.now().toString()
    if (s.abgehaktTag != heute) { s.abgehakt.clear(); s.abgehaktTag = heute }
    // Abgehakte Überfällige, die in der App wieder offen sind, stehen wieder normal in der Liste.
    liste.forEach { a -> if (a.id in s.abgehakt) s.abgehakt.remove(a.id) }
    val ids = liste.map { it.id }.toSet()
    s.aufgaben = sortiere(liste + s.abgehakt.values.filter { it.id !in ids })
    s.fehler = null
}

/**
 * Die heutigen Aufgaben aus Geniale Aufgaben direkt unter dem Schnellstart. Ein Tipp auf eine Zeile hakt sie ab (oder öffnet
 * sie wieder), und zwar in Geniale Aufgaben selbst; danach liest die Karte den Stand neu. Ändert sich in der Aufgaben-App etwas,
 * meldet sie das über ihre Brücke, die Karte liest dann ebenfalls neu, ebenso bei jeder Rückkehr in Jarvis und um Mitternacht.
 */
/** [griff]: Titel und Zählerzeile, an denen sich die ganze Karte auf dem Startbildschirm verschieben lässt. */
@Composable
fun HeuteAufgabenKarte(vm: AppViewModel, griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val s = HeuteAufgabenStand
    var anstoss by remember { mutableIntStateOf(0) }

    // Der Rückkehr-Effekt feuert auch beim ersten Erscheinen (die App ist dann schon vorn): Er löst die erste Lesung aus.
    // Weitere Anstöße kurz gebündelt, denn Änderungen in der Aufgaben-App kommen oft mehrfach hintereinander.
    LaunchedEffect(anstoss) {
        if (anstoss == 0) return@LaunchedEffect
        if (anstoss > 1) delay(300)
        // Beide Bereiche unabhängig voneinander; beim ersten Anstoß liest „morgen“ schon der Effekt darunter.
        coroutineScope {
            launch { neuLesen(context) }
            if (s.morgenOffen && anstoss > 1) launch { morgenLesen(context) }
        }
    }
    // Beim Aufklappen und beim Erscheinen der Karte mit offenem Bereich frisch lesen.
    LaunchedEffect(s.morgenOffen) { if (s.morgenOffen) morgenLesen(context) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { anstoss++; onPauseOrDispose { } }
    DisposableEffect(Unit) {
        val beobachter = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (System.currentTimeMillis() > s.eigeneAenderungBis) anstoss++
            }
        }
        runCatching { context.contentResolver.registerContentObserver(AufgabenFaehigkeit.BRUECKE, true, beobachter) }
        onDispose { runCatching { context.contentResolver.unregisterContentObserver(beobachter) } }
    }
    // Um Mitternacht beginnt ein neuer Tag: neu lesen.
    LaunchedEffect(Unit) {
        while (true) {
            val jetzt = LocalDateTime.now()
            val mitternacht = jetzt.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(5)
            delay(java.time.Duration.between(jetzt, mitternacht).toMillis().coerceAtLeast(1_000))
            // Die Morgen-Liste von gestern ist jetzt die von heute: nicht mehr zeigen, bis frisch gelesen ist.
            s.morgen = null
            anstoss++
        }
    }

    fun umschalten(a: HeuteAufgabe) {
        if (a.id in s.unterwegs) return
        val ziel = !a.erledigt
        s.unterwegs[a.id] = ziel
        // Im ViewModel, nicht in der Karte: Scrollt Frank sie weg, läuft das Abhaken trotzdem zu Ende.
        vm.viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    Register.alle(context).filterIsInstance<AufgabenFaehigkeit>().first().abhaken(a.id, ziel)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Ergebnis(e.message ?: "Geniale Aufgaben antwortet nicht.", fehler = true)
                }
            }
            // Die Aufgaben-App meldet die eigene Änderung gleich noch einmal; die Karte liest ohnehin selbst neu.
            s.eigeneAenderungBis = System.currentTimeMillis() + 2_000
            if (r.fehler) vm.meldung = "Nicht sicher umgestellt: ${r.text}"
            else if (ziel && a.ueberfaellig) s.abgehakt[a.id] = a.copy(erledigt = true)
            else if (!ziel) s.abgehakt.remove(a.id)
            // Erst den frischen Stand lesen, dann die Sperre lösen: So springt der Haken nicht kurz zurück.
            // Abhaken einer wiederkehrenden Aufgabe legt in Geniale Aufgaben die nächste an (oft für morgen): auch „morgen“
            // neu lesen, denn die Meldung der Aufgaben-App wird gerade als eigene Änderung übergangen.
            if (s.morgenOffen) launch { morgenLesen(context) }
            try { neuLesen(context) } finally { s.unterwegs.remove(a.id) }
        }
    }

    Karte {
        val anzeige = s.aufgaben?.map { a -> s.unterwegs[a.id]?.let { a.copy(erledigt = it) } ?: a }
        val fertig = anzeige?.count { it.erledigt } ?: 0
        val fehler = s.fehler
        Column(griff.fillMaxWidth()) {
        Text("Heute in Geniale Aufgaben", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                anzeige == null && fehler != null -> fehler
                anzeige == null -> "Wird gelesen …"
                anzeige.isEmpty() -> "Für heute steht nichts an." + (if (fehler != null) " · $fehler" else "")
                else -> "$fertig von ${anzeige.size} erledigt" + (if (fehler != null) " · $fehler" else "")
            },
            color = if (fehler != null) f.gefahr else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
        }
        if (anzeige != null) {
            val zeigen = if (s.alleZeigen || anzeige.size <= SICHTBAR) anzeige else anzeige.take(SICHTBAR)
            zeigen.forEach { a -> androidx.compose.runtime.key(a.id) { AufgabenZeile(a) { umschalten(a) } } }
            if (anzeige.size > SICHTBAR) Text(
                if (s.alleZeigen) "Weniger zeigen" else "Alle ${anzeige.size} zeigen",
                Modifier.padding(top = 4.dp).antippen { s.alleZeigen = !s.alleZeigen },
                color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        MorgenBereich()
    }
}

/** Aufklappbarer Bereich „Aufgaben morgen“ unter den heutigen: zu bis zum Antippen, zeigt dann alle offenen von morgen. */
@Composable
private fun MorgenBereich() {
    val f = LocalFarben.current
    val s = HeuteAufgabenStand
    val liste = s.morgen
    val fehler = s.morgenFehler
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp)
            .antippen { s.morgenUmschalten() }
            .semantics(mergeDescendants = true) { stateDescription = if (s.morgenOffen) "aufgeklappt" else "zugeklappt" }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Aufgaben morgen", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (s.morgenOffen && liste != null) Text("${s.morgenAnzahl}",color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
        Icon(
            if (s.morgenOffen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null,
            tint = f.primaer, modifier = Modifier.size(24.dp),
        )
    }
    if (!s.morgenOffen) return
    when {
        liste == null && fehler != null -> Text(fehler, color = f.gefahr, fontSize = 14.sp)
        liste == null -> Text("Wird gelesen …", color = f.textLeise, fontSize = 14.sp)
        liste.isEmpty() -> Text("Für morgen steht nichts an." + (if (fehler != null) " · $fehler" else ""), color = f.textLeise, fontSize = 14.sp)
        else -> {
            if (fehler != null) Text(fehler, color = f.gefahr, fontSize = 14.sp)
            liste.forEach { a ->
                androidx.compose.runtime.key("morgen_${a.id}") {
                    // Rechtsbündig mit Punkt ganz rechts, passend zu den heutigen Aufgaben (rechter Daumen).
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f).padding(end = 12.dp), horizontalAlignment = Alignment.End) {
                            Text(a.titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 4, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                            zusatz(a).takeIf { it.isNotEmpty() }?.let { Text(it, color = f.textLeise, fontSize = 13.sp, textAlign = TextAlign.End) }
                        }
                        Box(Modifier.padding(top = 7.dp, end = 10.dp).size(8.dp).knopf3d(f.sekundaer, f.sekundaer, 4.dp, f.dunkel))
                    }
                }
            }
            if (s.morgenAnzahl > liste.size) Text("Weitere ${s.morgenAnzahl - liste.size} nur in Geniale Aufgaben.", color = f.textLeise, fontSize = 13.sp)
        }
    }
}

/** Uhrzeit, „überfällig“ (nur offen) und „täglich“ unter dem Titel, gleich für heute und morgen. */
private fun zusatz(a: HeuteAufgabe): String =
    listOfNotNull(a.uhrzeit?.let { "$it Uhr" }, "überfällig".takeIf { a.ueberfaellig && !a.erledigt }, "täglich".takeIf { a.taeglich }).joinToString(" · ")

@Composable
private fun AufgabenZeile(a: HeuteAufgabe, umschalten: () -> Unit) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().antippen(rolle = Role.Checkbox, aktion = umschalten).padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) { stateDescription = if (a.erledigt) "erledigt" else "offen" },
        // Oben ausgerichtet: Das Abhakfeld sitzt auf Höhe der ersten Titelzeile, nicht mittig zwischen Titel und Zusatzzeile.
        verticalAlignment = Alignment.Top,
    ) {
        // Mitte des Kreises = optische Mitte der ersten Textzeile (Grundlinie minus halbe Versalhöhe),
        // gemessen an der echten Schrift, damit es auch bei anderer Schriftgröße passt.
        val dichte = LocalDensity.current
        val halbKreis = with(dichte) { 14.dp.toPx() }
        var textMitte by remember { mutableFloatStateOf(halbKreis) }
        val versatz = textMitte - halbKreis
        // Text rechtsbündig, Abhakfeld ganz rechts: mit dem rechten Daumen einhändig erreichbar.
        Column(
            Modifier.weight(1f).padding(end = 12.dp, top = with(dichte) { (-versatz).coerceAtLeast(0f).toDp() }),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                a.titel, color = if (a.erledigt) f.textLeise else f.text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                textDecoration = if (a.erledigt) TextDecoration.LineThrough else null, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                onTextLayout = { l -> textMitte = l.getLineBaseline(0) - with(dichte) { 15.sp.toPx() } * 0.35f },
            )
            val zusatz = zusatz(a)
            if (zusatz.isNotEmpty()) Text(zusatz, color = if (a.ueberfaellig && !a.erledigt) f.gefahr else f.textLeise, fontSize = 13.sp, textAlign = TextAlign.End)
        }
        Box(
            Modifier.padding(top = with(dichte) { versatz.coerceAtLeast(0f).toDp() }).size(28.dp).then(if (a.erledigt) Modifier.knopf3d(f.erfolg, f.erfolg, 14.dp, f.dunkel) else Modifier.glas(f, 14.dp, erhoeht = 0.3f).then(
                // Hellmodus: weißes Glas auf heller Karte verschwindet fast, ein kräftiger Ring macht das Abhakfeld klar erkennbar.
                if (f.dunkel) Modifier else Modifier.border(2.dp, f.textLeise.copy(alpha = 0.75f), CircleShape))),
            contentAlignment = Alignment.Center,
        ) {
            if (a.erledigt) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
    }
}

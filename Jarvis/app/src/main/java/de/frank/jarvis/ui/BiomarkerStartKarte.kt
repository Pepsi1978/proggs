package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.bio.BioAbruf
import de.frank.jarvis.bio.BioAuswertung
import de.frank.jarvis.bio.BioSpiegel
import de.frank.jarvis.bio.BioUebernahme
import de.frank.jarvis.bio.presentation.BioAnsicht
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Eine Zeile der Karte: Name, Wert mit Einheit, Datum der Messung (ISO, wie gemeldet), Einordnung. */
private data class BioZeile(val name: String, val wert: String, val datum: String, val einordnung: String)

/**
 * Stand der Karte außerhalb der Komposition: Sie liegt in der LazyColumn des Startbildschirms. Mit `remember` war sie nach dem
 * Wegscrollen leer, erschien beim Zurückscrollen erst klein und wuchs dann, sobald die Werte gelesen waren; das schob die
 * Bereiche darunter ruckartig nach unten. Gelesen wird schon mit dem Startbildschirm ([BiomarkerStartVorladen]), nicht erst
 * mit der Karte: So hat sie auch beim ersten Erscheinen ihre volle Höhe, und Zurückscrollen liest nicht jedes Mal neu.
 */
private object BiomarkerStartStand {
    var zeilen by mutableStateOf<List<BioZeile>>(emptyList())
    var stand by mutableStateOf("")
    var probleme by mutableStateOf<List<String>>(emptyList())
    /** Ergebnis der Übernahme aus Entropie Reductor; überlebt das Wegscrollen wie die Werte. */
    var uebernahme by mutableStateOf("")
    /** Nur die zuletzt begonnene Lesung darf den Stand setzen. */
    val lesung = java.util.concurrent.atomic.AtomicInteger()
}

/** „Letzter Abruf 11.11. 16:58 Uhr“ aus `2026-11-11T16:58…`; ein anderes Format erscheint unverändert statt abzustürzen. */
private fun abrufText(roh: String): String = runCatching {
    val t = roh.replace('T', ' ').drop(5)
    "${t.substring(3, 5)}.${t.substring(0, 2)}. ${t.substring(6, 11)}"
}.getOrElse { roh } + " Uhr"

/**
 * Liest den Stand der Biomarker-Karte, solange der Startbildschirm offen ist, unabhängig davon, ob die Karte gerade sichtbar ist:
 * beim Öffnen, nach jedem Abruf und nach der Übernahme. Steht nur einmal im Startbildschirm.
 */
@Composable
fun BiomarkerStartVorladen() {
    val context = LocalContext.current.applicationContext
    val abruf by BioAbruf.stand.collectAsState()
    val s = BiomarkerStartStand
    val bereit = BioAbruf.bereit(context)
    // Tablet: Jedes Einspielen eines neuen Handy-Stands liest die Karte neu (die Nachschau selbst hängt an der App, nicht an dieser Karte).
    val spiegel by BioSpiegel.eingespielt.collectAsState()
    LaunchedEffect(abruf.bericht, abruf.laeuft, bereit, s.uebernahme, spiegel) {
        if (!bereit) return@LaunchedEffect
        // Der Datenstand des Spiegels gilt auch dann, wenn das Lesen der Werte unten einmal scheitert.
        if (Geraet.istTablet) s.stand = BioSpiegel.standText(context)
        val nummer = s.lesung.incrementAndGet()
        val antwort = withContext(Dispatchers.IO) {
            runCatching {
                BioAuswertung(context).rufe("auswertung", JSONObject().put("tage", 30).put("metriken", JSONArray(listOf("erholung", "hrv", "ruhepuls", "tiefschlaf_prozent", "schlafdauer", "vo2max"))))
            }.getOrNull()
        } ?: return@LaunchedEffect
        if (nummer != s.lesung.get()) return@LaunchedEffect
        val liste = antwort.optJSONArray("metriken") ?: JSONArray()
        s.zeilen = (0 until liste.length()).map { i ->
            val m = liste.getJSONObject(i)
            val wert = if (m.isNull("aktuell")) "–" else m.opt("aktuell").let { if (it is Number) zahl(it.toDouble()) else it.toString() }
            BioZeile(m.optString("name").substringBefore(" ("), "$wert ${m.optString("einheit")}".trim(), m.optString("datum"), m.optString("einordnung"))
        }
        val st = antwort.optJSONObject("stand")
        s.stand = when {
            Geraet.istTablet -> BioSpiegel.standText(context)
            st == null || st.isNull("letzter_abgleich") -> "Noch kein Abruf in Jarvis"
            else -> "Letzter Abruf " + abrufText(st.optString("letzter_abgleich"))
        }
        s.probleme = (st?.optJSONArray("probleme") ?: JSONArray()).let { p -> (0 until p.length()).map { p.optString(it) } }
    }
}

/** Datum hinter dem Wert („· 10.11.“), nur wenn die Messung nicht von heute ist; bei der Anzeige geprüft, also auch nach Mitternacht richtig. */
private fun datumZusatz(datum: String): String =
    if (datum.length < 10 || datum == java.time.LocalDate.now().toString()) "" else " · ${datum.substring(8, 10)}.${datum.substring(5, 7)}."

/**
 * Eigener Einstieg in den Biomarker-Bereich auf dem Startbildschirm: Franks Kerngrößen (Erholung, HRV, Ruhepuls,
 * Tiefschlafanteil, Schlaf, VO2max) mit Datum und Einordnung und der Zustand der Quellen beim letzten Abruf.
 * Die Karte LIEST nur den gespeicherten Stand; frisch abgerufen wird über den Aktualisieren-Knopf, per Sprache und vor jeder Tagesauswertung (Tablet: Spiegel des Handys).
 */
@Composable
fun BiomarkerStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val abruf by BioAbruf.stand.collectAsState()
    val s = BiomarkerStartStand
    val zeilen = s.zeilen
    val stand = s.stand
    val probleme = s.probleme
    val uebernahme = s.uebernahme
    val bereit = BioAbruf.bereit(context)

    // Die ganze Karte ist der Griff (langer Druck, dann ziehen), wie Frank es für alle Startbereiche erwartet. Ein Tippen irgendwo auf
    // die Karte öffnet den Biomarker-Bereich (Franks Wunsch vom 11.11.2026, ohne eigenen Öffnen-Knopf).
    Karte(modifier = if (bereit) griff.antippen { BioAnsicht.oeffnen(context) } else griff) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Biomarker", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        abruf.laeuft -> "Holt frische Daten …"
                        !bereit -> "Übernahme aus Entropie Reductor steht noch aus"
                        else -> stand
                    },
                    color = if (abruf.laeuft) f.primaer else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Nur solange die Übernahme aus Entropie Reductor aussteht; danach öffnet das Tippen auf die Karte den Bereich.
            if (!bereit) Knopf("Übernehmen") {
                // Im Modulbereich, nicht in der Karte: Scrollt Frank sie weg, läuft die Übernahme trotzdem zu Ende.
                if (s.uebernahme != "Übernimmt …") { s.uebernahme = "Übernimmt …"; de.frank.jarvis.bio.BioModul.bereich.launch { s.uebernahme = BioUebernahme.starte(context) } }
            }
        }
        if (uebernahme.isNotEmpty()) Text(uebernahme, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        if (bereit) {
            probleme.forEach { Text("⚠ $it", color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(6.dp))
            zeilen.forEach { z ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(z.name, color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(z.wert + datumZusatz(z.datum), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (z.einordnung.contains("besser") || z.einordnung.contains("schlechter")) {
                    Text(z.einordnung, color = if (z.einordnung.contains("besser")) f.erfolg else f.gefahr, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun zahl(w: Double): String = if (w == Math.floor(w) && kotlin.math.abs(w) < 1e9) w.toLong().toString() else String.format(java.util.Locale.GERMAN, "%.1f", w)

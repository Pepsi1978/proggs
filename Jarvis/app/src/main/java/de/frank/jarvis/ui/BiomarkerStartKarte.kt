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
import de.frank.jarvis.bio.BioUebernahme
import de.frank.jarvis.bio.presentation.BioAnsicht
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.ui.theme.LocalFarben
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Eine Zeile der Karte: Name, Wert mit Einheit, Datum der Messung (leer = heute), Einordnung. */
private data class BioZeile(val name: String, val wert: String, val datum: String, val einordnung: String)

/**
 * Eigener Einstieg in den Biomarker-Bereich auf dem Startbildschirm: Franks Kerngrößen (Erholung, HRV, Ruhepuls,
 * Tiefschlafanteil, Schlaf, VO2max) mit Datum und Einordnung und der Zustand der Quellen beim letzten Abruf.
 * Die Karte LIEST nur den gespeicherten Stand; frisch abgerufen wird erst beim Öffnen des Bereichs.
 */
@Composable
fun BiomarkerStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val abruf by BioAbruf.stand.collectAsState()
    var zeilen by remember { mutableStateOf<List<BioZeile>>(emptyList()) }
    var stand by remember { mutableStateOf("") }
    var probleme by remember { mutableStateOf<List<String>>(emptyList()) }
    var uebernahme by remember { mutableStateOf("") }
    val bereit = BioAbruf.bereit(context)

    LaunchedEffect(abruf.bericht, abruf.laeuft, bereit) {
        if (!bereit) return@LaunchedEffect
        val antwort = withContext(Dispatchers.IO) {
            runCatching {
                BioAuswertung(context).rufe("auswertung", JSONObject().put("tage", 30).put("metriken", JSONArray(listOf("erholung", "hrv", "ruhepuls", "tiefschlaf_prozent", "schlafdauer", "vo2max"))))
            }.getOrNull()
        } ?: return@LaunchedEffect
        val heute = antwort.optString("heute")
        val liste = antwort.optJSONArray("metriken") ?: JSONArray()
        zeilen = (0 until liste.length()).map { i ->
            val m = liste.getJSONObject(i)
            val wert = m.opt("aktuell").let { if (it is Number) zahl(it.toDouble()) else it.toString() }
            BioZeile(m.optString("name").substringBefore(" ("), "$wert ${m.optString("einheit")}".trim(), m.optString("datum").takeIf { it != heute }.orEmpty(), m.optString("einordnung"))
        }
        val s = antwort.optJSONObject("stand")
        stand = when {
            Geraet.istTablet -> de.frank.jarvis.bio.BioSpiegel.standText(context)
            s == null || s.isNull("letzter_abgleich") -> "Noch kein Abruf in Jarvis"
            else -> "Letzter Abruf " + s.optString("letzter_abgleich").replace('T', ' ').drop(5).let { "${it.substring(3, 5)}.${it.substring(0, 2)}. ${it.substring(6)}" } + " Uhr"
        }
        probleme = (s?.optJSONArray("probleme") ?: JSONArray()).let { p -> (0 until p.length()).map { p.optString(it) } }
    }

    // Die ganze Karte ist der Griff (langer Druck, dann ziehen), wie Frank es für alle Startbereiche erwartet; der Knopf bleibt antippbar.
    Karte(modifier = griff) {
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
            Knopf(if (bereit) "Öffnen" else "Übernehmen") {
                if (bereit) BioAnsicht.oeffnen(context)
                else { uebernahme = "Übernimmt …"; scope.launch { uebernahme = de.frank.jarvis.bio.BioModul.bereich.async { BioUebernahme.starte(context) }.await() } }
            }
        }
        if (uebernahme.isNotEmpty()) Text(uebernahme, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        if (bereit) {
            probleme.forEach { Text("⚠ $it", color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(6.dp))
            zeilen.forEach { z ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(z.name, color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(z.wert + (if (z.datum.isNotEmpty()) " · ${z.datum.drop(8)}.${z.datum.substring(5, 7)}." else ""), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (z.einordnung.contains("besser") || z.einordnung.contains("schlechter")) {
                    Text(z.einordnung, color = if (z.einordnung.contains("besser")) f.erfolg else f.gefahr, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun zahl(w: Double): String = if (w == Math.floor(w) && kotlin.math.abs(w) < 1e9) w.toLong().toString() else String.format(java.util.Locale.GERMAN, "%.1f", w)

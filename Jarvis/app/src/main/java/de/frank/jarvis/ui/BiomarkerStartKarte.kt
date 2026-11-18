package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.drawText
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

/** Eine Zeile der Karte: Name, Wert mit Einheit, Datum der Messung (ISO, wie gemeldet), Einordnung, dazu die Farbskala. */
private data class BioZeile(val name: String, val wert: String, val datum: String, val einordnung: String, val skala: BioSkala? = null, val id: String = "")

/** Messgröße der Kachel → Detailbereich im Biomarker-Bereich (Franks Wunsch vom 17.11.2026). */
private val DETAIL_ZU_ID = mapOf(
    "erholung" to de.frank.jarvis.bio.presentation.dashboard4.MetricKey.RECOVERY,
    "hrv" to de.frank.jarvis.bio.presentation.dashboard4.MetricKey.HRV,
    "ruhepuls" to de.frank.jarvis.bio.presentation.dashboard4.MetricKey.RHR,
    "tiefschlaf_prozent" to de.frank.jarvis.bio.presentation.dashboard4.MetricKey.SLEEP_DEEP_PERCENT,
    "schlafdauer" to de.frank.jarvis.bio.presentation.dashboard4.MetricKey.SLEEP_TOTAL,
    "vo2max" to de.frank.jarvis.bio.presentation.dashboard4.MetricKey.VO2MAX,
)

/** Farbskala über einen festen Bereich: schlechtes Ende links (rot), gutes rechts (grün), Werte außerhalb am Rand; [schnitt] = langfristiger Durchschnitt. */
private data class BioSkala(val schlecht: Double, val gut: Double, val aktuell: Double, val schnitt: Double, val schnittText: String) {
    fun lage(w: Double): Float = if (gut == schlecht) 0.5f else ((w - schlecht) / (gut - schlecht)).toFloat().coerceIn(0f, 1f)
}

/** Diese Größen zeigen statt des Einordnungstexts die Farbskala, mit festem Anzeigebereich (Franks Vorgabe vom 15.11.2026; Erholung 0–100 % ab 1.8.689, Schlafzeit bis 10 Stunden ab 1.8.742). */
private val SKALA_BEREICH = mapOf(
    "erholung" to (0.0 to 100.0), "hrv" to (20.0 to 60.0), "ruhepuls" to (45.0 to 70.0), "tiefschlaf_prozent" to (0.0 to 30.0),
    "schlafdauer" to (6.0 to 10.0), "vo2max" to (30.0 to 50.0),
)

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
            val bereich = SKALA_BEREICH[m.optString("id")]
            val skala = if (bereich != null && m.opt("aktuell") is Number) {
                val (tief, hoch) = bereich; val tage = m.optInt("tage_gesamt")
                // Langfristiger Schnitt samt aktuellem Wert, aus dem Schnitt aller Tage davor.
                val schnitt = if (tage > 0 && !m.isNull("schnitt_gesamt")) (m.optDouble("schnitt_gesamt") * tage + m.optDouble("aktuell")) / (tage + 1) else m.optDouble("aktuell")
                val niedrigBesser = m.optString("hoeher_ist") == "schlechter"
                BioSkala(if (niedrigBesser) hoch else tief, if (niedrigBesser) tief else hoch, m.optDouble("aktuell"), schnitt,
                    zahl(schnitt))
            } else null
            BioZeile(m.optString("name").substringBefore(" ("), "$wert ${m.optString("einheit")}".trim(), m.optString("datum"), m.optString("einordnung"), skala, m.optString("id"))
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
 * Die Karte LIEST nur den gespeicherten Stand; frisch abgerufen wird über ihren Aktualisieren-Knopf (oder den im Bereich), per Sprache und vor jeder Tagesauswertung (Tablet: Spiegel des Handys).
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
    // Aktualisieren direkt auf dem Startbildschirm (Frank, 15.11.2026), mit Schutzkreis wie bei Raumklima: Ein knapp verfehlter
    // Knopf liest neu statt den Bereich zu öffnen. Die neuen Werte liest BiomarkerStartVorladen nach dem Abruf.
    val schutz = remember { KnopfSchutz() }
    val dichte = androidx.compose.ui.platform.LocalDensity.current
    val scope = rememberCoroutineScope()
    val aktualisieren: () -> Unit = {
        if (BioAbruf.stand.value.laeuft) android.widget.Toast.makeText(context, "Der Abruf läuft schon.", android.widget.Toast.LENGTH_SHORT).show()
        else {
            val laeuft = BioAbruf.manuell(context, "Aktualisieren-Knopf (Startbildschirm)")
            // Tablet: holt nur den Spiegel des Handys und liefert nie einen Bericht; ein fehlender Bericht ist dort kein Fehler.
            if (!Geraet.istTablet) scope.launch {
                if (laeuft.await() == null) android.widget.Toast.makeText(context, "Abruf fehlgeschlagen, Grund im Logbuch.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    Karte(modifier = if (bereit) griff.antippenMitSchutz(schutz, imSchutz = aktualisieren) { BioAnsicht.oeffnen(context) } else griff) {
        Row(verticalAlignment = Alignment.Top) {
            KartenSymbol(KartenArt.BIOMARKER, Modifier.padding(end = 10.dp))
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
            if (bereit) AktualisierenKnopf(
                abruf.laeuft,
                if (Geraet.istTablet) "Biomarker vom Handy holen" else "Biomarker aktualisieren",
                "Biomarker werden abgerufen", schutz, aktualisieren,
            )
            // Nur solange die Übernahme aus Entropie Reductor aussteht; danach öffnet das Tippen auf die Karte den Bereich.
            if (!bereit) Knopf("Übernehmen") {
                // Im Modulbereich, nicht in der Karte: Scrollt Frank sie weg, läuft die Übernahme trotzdem zu Ende.
                if (s.uebernahme != "Übernimmt …") { s.uebernahme = "Übernimmt …"; de.frank.jarvis.bio.BioModul.bereich.launch { s.uebernahme = BioUebernahme.starte(context) } }
            }
        }
        if (uebernahme.isNotEmpty()) Text(uebernahme, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        if (bereit) {
            probleme.forEach { Text("⚠ $it", color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(10.dp))
            // Jede Messgröße als eigener abgerundeter Block mit ihrer Linie darin, damit klar ist, welche Linie zu welchem Wert gehört
            // (Franks Wunsch vom 15.11.2026). Ab 600 dp Breite (Tablet, Fold offen) zwei Blöcke nebeneinander.
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                val spalten = if (maxWidth >= 600.dp) 2 else 1
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    zeilen.chunked(spalten).forEach { reihe ->
                        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                            // Antippen einer Kachel öffnet direkt ihren Detailbereich (Graph, Werte-Übersicht, 7/30/90 Tage).
                            reihe.forEach { z ->
                                val detail = DETAIL_ZU_ID[z.id]
                                // Langer Druck verschiebt weiter die Karte (lange = {} verbraucht ihn); ein Tipp im Schutzkreis des
                                // Aktualisieren-Knopfs aktualisiert wie auf der übrigen Karte (die Karte merkt sich die Druckstelle).
                                BioBlock(z, Modifier.weight(1f).fillMaxHeight()
                                    .then(if (detail != null) Modifier.antippen(lange = {}) {
                                        if (schutz.imSchutzkreis(with(dichte) { SCHUTZ_RADIUS.toPx() })) aktualisieren()
                                        else BioAnsicht.oeffnenMitDetail(context, detail)
                                    } else Modifier))
                            }
                            repeat(spalten - reihe.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/** Ein Block je Messgröße: Name links, Wert rechts, darunter die Farbskala oder die Einordnung, auf eigener Fläche mit Rand. */
@Composable
private fun BioBlock(z: BioZeile, modifier: Modifier) {
    val f = LocalFarben.current
    val form = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Column(
        modifier.clip(form).background(f.flaeche, form).border(1.dp, f.rand, form)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = if (z.skala != null) 2.dp else 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(z.name, color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(z.wert + datumZusatz(z.datum), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        if (z.skala != null) FarbSkala(z.skala)
        else if (z.einordnung.contains("besser") || z.einordnung.contains("schlechter")) {
            Text(z.einordnung, color = if (z.einordnung.contains("besser")) f.erfolg else f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun zahl(w: Double): String = if (w == Math.floor(w) && kotlin.math.abs(w) < 1e9) w.toLong().toString() else String.format(java.util.Locale.GERMAN, "%.1f", w)

/**
 * Verlauf rot → gelb → grün über den festen Bereich aus SKALA_BEREICH (links schlecht, rechts gut), darauf der aktuelle Wert als
 * Strich (ohne Zahl, die steht schon in der Zeile) und der langfristige Durchschnitt als Punkt mit seinem Wert darüber.
 */
@Composable
private fun FarbSkala(s: BioSkala) {
    val f = LocalFarben.current
    val messer = androidx.compose.ui.text.rememberTextMeasurer()
    val stil = androidx.compose.ui.text.TextStyle(color = f.textLeise, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    val rot = androidx.compose.ui.graphics.Color(0xFFE5484D)
    val gelb = androidx.compose.ui.graphics.Color(0xFFF5C451)
    val gruen = androidx.compose.ui.graphics.Color(0xFF3DBE7A)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp).height(30.dp)) {
        val dicke = 6.dp.toPx()
        val y = size.height - 9.dp.toPx()
        val rand = 7.dp.toPx()
        val breite = size.width - 2 * rand
        fun x(w: Double) = rand + breite * s.lage(w)
        drawLine(
            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(rot, gelb, gruen), startX = rand, endX = rand + breite),
            androidx.compose.ui.geometry.Offset(rand, y), androidx.compose.ui.geometry.Offset(rand + breite, y),
            strokeWidth = dicke, cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Durchschnitt: Punkt mit Wert darüber, am Rand eingerückt, damit die Zahl nicht abgeschnitten wird.
        val sx = x(s.schnitt)
        drawCircle(f.hgOben, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(sx, y))
        drawCircle(f.text, 3.5.dp.toPx(), androidx.compose.ui.geometry.Offset(sx, y))
        val t = messer.measure("Ø " + s.schnittText, stil)
        val tx = (sx - t.size.width / 2f).coerceIn(0f, size.width - t.size.width)
        drawText(t, topLeft = androidx.compose.ui.geometry.Offset(tx, y - dicke / 2 - 3.dp.toPx() - t.size.height))
        // Aktueller Wert: kräftiger Strich quer über die Linie.
        val ax = x(s.aktuell)
        drawLine(f.hgOben, androidx.compose.ui.geometry.Offset(ax, y - 8.dp.toPx()), androidx.compose.ui.geometry.Offset(ax, y + 8.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(f.text, androidx.compose.ui.geometry.Offset(ax, y - 7.dp.toPx()), androidx.compose.ui.geometry.Offset(ax, y + 7.dp.toPx()), strokeWidth = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

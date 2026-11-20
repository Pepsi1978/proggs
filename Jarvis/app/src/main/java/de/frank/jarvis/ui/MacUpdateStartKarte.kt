package de.frank.jarvis.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import java.net.ConnectException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fernsteuerung der Updater-Zentrale auf Franks MacBook: Handy/Tablet → Relay (/geraet/updatezentrale…) → Mac-Brücke
 * (jarvis_pc_bruecke.py) → App (Services/Fernsteuerung.swift). Prüfen und Installieren laufen am Mac; hier nur Aufträge und Anzeige.
 * Ein Auftrag wird nie von selbst wiederholt: Kommt keine Bestätigung, heißt es „unklar“, und die Karte zeigt den nächsten Stand des Macs.
 */
object MacUpdateZentrale {
    data class Lage(
        val stand: JSONObject? = null, val app: Boolean = false, val alterS: Long? = null, val macDa: Boolean = false,
        val geholt: Long = 0, val abrufFehler: Boolean = false,
    )

    val lage = MutableStateFlow(Lage())
    val meldung = MutableStateFlow<String?>(null)
    val sendet = MutableStateFlow<String?>(null)
    /** Kennung des letzten Auftrags von diesem Gerät (für Abweisungen und Fehler der Brücke). */
    val eigeneKennung = MutableStateFlow<String?>(null)
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val JSON = "application/json; charset=UTF-8".toMediaType()
    private val NETZ = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    suspend fun holen(context: Context) = withContext(Dispatchers.IO) {
        val e = Einstellungen.get(context)
        if (e.serverToken.isBlank()) return@withContext
        runCatching {
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/updatezentrale").header("X-Jarvis-Token", e.serverToken)
                .header(Geraet.KOPFZEILE, Geraet.kennung).build()
            NETZ.newCall(anfrage).execute().use { a ->
                check(a.isSuccessful) { "HTTP ${a.code}" }
                val j = JSONObject(a.body?.string().orEmpty())
                val fehler = j.optJSONObject("fehler")
                if (fehler != null && fehler.optString("kennung") == eigeneKennung.value) meldung.value = fehler.optString("text")
                lage.value = Lage(j.optJSONObject("stand"), j.optBoolean("app"), if (j.isNull("alter_s")) null else j.optLong("alter_s"),
                    j.optBoolean("mac_da"), System.currentTimeMillis())
            }
        }.onFailure { lage.update { l -> l.copy(abrufFehler = true, geholt = System.currentTimeMillis()) } }
    }

    fun senden(context: Context, art: String) {
        if (sendet.value != null) return
        sendet.value = art
        meldung.value = null
        bereich.launch {
            val text = withContext(Dispatchers.IO) {
                val e = Einstellungen.get(context)
                try {
                    val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/updatezentrale-befehl")
                        .post(JSONObject().put("art", art).toString().toRequestBody(JSON))
                        .header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung).build()
                    NETZ.newCall(anfrage).execute().use { a ->
                        check(a.isSuccessful) { "HTTP ${a.code}" }
                        val j = JSONObject(a.body?.string().orEmpty())
                        when (j.optString("ergebnis")) {
                            "angenommen" -> { eigeneKennung.value = j.optString("kennung"); null }
                            "mac_nicht_verbunden" -> "MacBook nicht erreichbar (aus, im Ruhezustand oder ohne Netz). Es wurde nichts gestartet."
                            "laeuft_bereits" -> "Läuft bereits: " + j.optString("text") + " Es wird nichts doppelt gestartet."
                            "unklar" -> { eigeneKennung.value = j.optString("kennung"); "Der Auftrag ging an den Mac, die Bestätigung fehlt. Stand unklar – er wird nicht erneut gesendet." }
                            else -> "Unerwartete Antwort vom Server."
                        }
                    }
                } catch (f: Exception) {
                    if (f is ConnectException || f is UnknownHostException) "Server nicht erreichbar. Es wurde nichts gestartet."
                    else "Verbindung abgebrochen. Ob der Auftrag ankam, ist unklar – er wird nicht erneut gesendet; die Karte zeigt gleich den Stand des Macs."
                }
            }
            Logbuch.netz("Mac-Update-Zentrale: $art", text ?: "angenommen")
            meldung.value = text
            sendet.value = null
            holen(context)
        }
    }
}

private fun uhr(sekunden: Double): String = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date((sekunden * 1000).toLong()))

private fun JSONArray?.objekte(): List<JSONObject> = (0 until (this?.length() ?: 0)).map { this!!.getJSONObject(it) }

/**
 * Startkarte „Mac-Update-Zentrale“: zugeklappt Zahl der Updates, Status und rechts „Aktualisieren“ (frische Prüfung am Mac, installiert
 * nichts). Aufgeklappt oben Grün „Unkritische Updates installieren“ und Rot „Kritische Updates installieren“ (nur nach Ja im Warnfenster),
 * darunter Status, lokale Freigaben, Neustarts, Ergebnis und die Updates in zwei Gruppen.
 */
@Composable
fun MacUpdateStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val l by MacUpdateZentrale.lage.collectAsState()
    val meldung by MacUpdateZentrale.meldung.collectAsState()
    val sendet by MacUpdateZentrale.sendet.collectAsState()
    val eigene by MacUpdateZentrale.eigeneKennung.collectAsState()
    var offen by rememberSaveable { mutableStateOf(false) }
    var kritischFrage by remember { mutableStateOf(false) }
    val drehung by androidx.compose.animation.core.animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")

    val s = l.stand
    val vorgang = s?.optJSONObject("vorgang")
    val laeuft = s != null && (s.optBoolean("laeuft") || vorgang?.optString("phase") == "laeuft")
    // Während eines Laufs meldet der Mac mindestens alle 30 s: Bleibt das aus, ist die Verbindung weg und der Stand unklar.
    val abgerissen = laeuft && ((l.alterS ?: 0) > 120 || !l.macDa)
    val eilig by androidx.compose.runtime.rememberUpdatedState(laeuft || sendet != null)
    val aufgeklappt by androidx.compose.runtime.rememberUpdatedState(offen)
    LaunchedEffect(Unit) {
        while (true) {
            MacUpdateZentrale.holen(context)
            delay(if (eilig) 3_000 else if (aufgeklappt) 10_000 else 30_000)
        }
    }

    val programme = s?.optJSONArray("programme").objekte()
    val unkritisch = programme.filter { it.optBoolean("update") && !it.optBoolean("kritisch") }
    val kritisch = programme.filter { it.optBoolean("update") && it.optBoolean("kritisch") }
    val fehler = programme.filter { it.optBoolean("fehler") }
    val neustart = programme.filter { it.optBoolean("neustart") }.map { it.optString("name") }
    val installierbarU = unkritisch.count { !it.optBoolean("nurKlick") }
    val installierbarK = kritisch.count { !it.optBoolean("nurKlick") }
    val bedienbar = l.macDa && !laeuft && sendet == null
    val freigabe = s?.optString("freigabe").orEmpty()

    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(end = 10.dp).size(44.dp).glas(f, 14.dp, erhoeht = 0.3f), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LaptopMac, null, tint = f.primaer, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f).antippen(haptik = false) { offen = !offen }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mac-Update-Zentrale", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    val n = unkritisch.size + kritisch.size
                    if (n > 0) Text("$n", Modifier.padding(start = 8.dp).glas(f, 10.dp, erhoeht = 0.2f, toenung = f.primaer).padding(horizontal = 8.dp, vertical = 2.dp),
                        color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    when {
                        sendet != null -> "Sendet den Auftrag an das MacBook …"
                        s == null && !l.macDa -> "MacBook nicht erreichbar"
                        s == null -> "Noch kein Stand – Aktualisieren tippen"
                        laeuft -> vorgang?.optString("text")?.takeIf { it.isNotBlank() && vorgang.optString("phase") == "laeuft" } ?: s.optString("kopf")
                        unkritisch.isEmpty() && kritisch.isEmpty() -> if (fehler.isEmpty()) "Alles aktuell" else "Keine Updates · ${fehler.size} Prüffehler"
                        else -> "${unkritisch.size} unkritisch · ${kritisch.size} CLI-kritisch"
                    },
                    color = when {
                        abgerissen -> f.gefahr
                        laeuft || sendet != null -> f.primaer
                        s != null && unkritisch.isEmpty() && kritisch.isEmpty() && fehler.isEmpty() -> f.erfolg
                        else -> f.text
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp), maxLines = 2,
                )
                val zeile = listOfNotNull(
                    s?.optDouble("geprueft")?.takeIf { !it.isNaN() && it > 0 }?.let { "Geprüft " + uhr(it) },
                    s?.optDouble("zeit")?.takeIf { !it.isNaN() && it > 0 }?.let { "Stand " + uhr(it) },
                    if (!l.macDa && s != null) "MacBook nicht erreichbar" else null,
                    if (l.abrufFehler) "Server nicht erreichbar" else null,
                ).joinToString(" · ")
                if (zeile.isNotEmpty()) Text(zeile, color = if (!l.macDa || l.abrufFehler) f.gefahr else f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val pruefend = laeuft && s?.optString("art") == "pruefung"
                Box(
                    Modifier.offset(x = 4.dp, y = (-6).dp).size(44.dp).glas(f, 22.dp, erhoeht = 0.5f, toenung = if (pruefend) f.primaer else null)
                        .antippen { if (bedienbar) MacUpdateZentrale.senden(context, "pruefen") else if (!laeuft) MacUpdateZentrale.bereich.launch { MacUpdateZentrale.holen(context) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, "Am MacBook frisch prüfen", tint = if (pruefend) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Zuklappen" else "Aufklappen",
                    Modifier.padding(top = 2.dp).size(24.dp).graphicsLayer { rotationZ = drehung }.antippen(haptik = false) { offen = !offen }, tint = f.textLeise)
            }
        }
        meldung?.let { Text(it, color = f.text, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }

        AnimatedVisibility(offen) {
            Column {
                Knopf(if (installierbarU > 0) "Unkritische Updates installieren ($installierbarU)" else "Unkritische Updates installieren",
                    Modifier.fillMaxWidth().padding(top = 12.dp).alpha(if (bedienbar && installierbarU > 0) 1f else 0.4f), farbe = f.erfolg) {
                    if (bedienbar && installierbarU > 0) MacUpdateZentrale.senden(context, "unkritisch")
                }
                Knopf(if (installierbarK > 0) "Kritische Updates installieren ($installierbarK)" else "Kritische Updates installieren",
                    Modifier.fillMaxWidth().padding(top = 8.dp).alpha(if (bedienbar && installierbarK > 0) 1f else 0.4f), farbe = f.gefahr) {
                    if (bedienbar && installierbarK > 0) kritischFrage = true
                }
                Text("Aktualisieren prüft nur und installiert nichts. Grün installiert sofort nur die unkritischen Updates; die kritischen starten nie von selbst danach.",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

                if (abgerissen) Hinweis("Keine Rückmeldung vom MacBook seit ${(l.alterS ?: 0) / 60} Min. Der Stand ist unklar; es wird nichts erneut gestartet.", f.gefahr)
                if (laeuft && s != null) {
                    val fo = s.optJSONArray("fortschritt")
                    val teil = fo?.let { if (it.optInt(1) > 1) " (${it.optInt(0)} von ${it.optInt(1)})" else "" } ?: ""
                    Hinweis((s.optString("kopf") + teil).trim(), f.primaer)
                    s.optString("detail").takeIf { it.isNotBlank() }?.let { Text(it, color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
                if (freigabe.isNotBlank()) Hinweis("Lokale Freigabe am Mac nötig – $freigabe", Color(0xFFF5A524))
                if (neustart.isNotEmpty()) Hinweis("Neustart nötig: " + neustart.joinToString(", ") + ". Das Update wird erst danach aktiv.", Color(0xFFF5A524))
                s?.optJSONObject("abgewiesen")?.takeIf { it.optString("kennung") == eigene }?.let { Hinweis("Abgewiesen: " + it.optString("text"), Color(0xFFF5A524)) }
                if (vorgang != null && vorgang.optString("phase") != "laeuft" && vorgang.optString("art") != "pruefen") {
                    val phase = vorgang.optString("phase")
                    Hinweis((if (vorgang.optString("art") == "kritisch") "Kritische Updates: " else "Unkritische Updates: ") + vorgang.optString("text") +
                        vorgang.optDouble("ende").takeIf { !it.isNaN() }?.let { " (" + uhr(it) + ")" }.orEmpty(),
                        when (phase) { "fertig" -> f.erfolg; "teilweise", "unklar" -> Color(0xFFF5A524); else -> f.gefahr })
                    vorgang.optJSONArray("ergebnisse").objekte().forEach { e ->
                        Text((if (e.optBoolean("ok")) "✓ " else "✗ ") + e.optString("name") + " – " + e.optString("text"),
                            color = if (e.optBoolean("ok")) f.textLeise else f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                Gruppe("CLI-unkritische Updates", unkritisch, leer = "Keine")
                Gruppe("CLI-kritische Updates", kritisch, leer = "Keine", mitGrund = true)
                if (fehler.isNotEmpty()) Gruppe("Prüfung fehlgeschlagen (geht an Claude Code)", fehler, leer = "")
            }
        }
    }

    if (kritischFrage) AlertDialog(
        onDismissRequest = { kritischFrage = false },
        title = { Text("Kritische Updates installieren") },
        text = { Text("Willst du die CLI-kritischen Updates jetzt wirklich installieren? Laufende Programmierarbeit kann unterbrochen werden.") },
        confirmButton = { TextButton({ kritischFrage = false; MacUpdateZentrale.senden(context, "kritisch") }) { Text("Ja") } },
        dismissButton = { TextButton({ kritischFrage = false }) { Text("Nein") } },
    )
}

@Composable
private fun Hinweis(text: String, farbe: Color) {
    val f = LocalFarben.current
    Text(text, Modifier.fillMaxWidth().padding(top = 10.dp).glas(f, 12.dp, erhoeht = 0.1f, toenung = farbe).padding(horizontal = 10.dp, vertical = 6.dp),
        color = f.text, fontSize = 14.sp)
}

@Composable
private fun Gruppe(titel: String, liste: List<JSONObject>, leer: String, mitGrund: Boolean = false) {
    val f = LocalFarben.current
    Text(titel + if (liste.isNotEmpty()) " (${liste.size})" else "", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
    if (liste.isEmpty() && leer.isNotEmpty()) Text(leer, color = f.textSchwach, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    liste.forEach { p ->
        Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.optString("name"), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val inst = p.optString("inst"); val verf = p.optString("verf")
                if (verf.isNotBlank()) Text((if (inst.isNotBlank()) "$inst → " else "") + verf, color = f.textLeise, fontSize = 13.sp)
            }
            val zusatz = listOfNotNull(
                if (p.optBoolean("laeuft")) p.optString("status") else null,
                if (p.optBoolean("nurKlick")) "Hauptversion: nur per Klick am Mac" else null,
                if (p.optBoolean("fehler")) p.optString("status") else null,
                if (mitGrund) p.optString("grund").takeIf { it.isNotBlank() } else null,
            ).filter { it.isNotBlank() }
            zusatz.forEach { Text(it, color = f.textSchwach, fontSize = 12.sp) }
        }
    }
}

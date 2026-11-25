package de.frank.jarvis.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
        /** Programmier-CLI aufräumen (vom Mac): automatik, stunden, naechster, laeuft, letzter {art, zeit, text, beendet, offen}, sitzungen. */
        val cli: JSONObject? = null,
        /** Fehler der Brücke zum letzten Auftrag {kennung, text}. */
        val fehler: JSONObject? = null,
        /** Alter des CLI-Stands am Relay in Sekunden (null = keiner). */
        val cliAlterS: Long? = null,
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
                if (fehler != null && fehler.optString("kennung") == cliKennung.value) cliMeldung.value = fehler.optString("text")
                lage.value = Lage(j.optJSONObject("stand"), j.optBoolean("app"), if (j.isNull("alter_s")) null else j.optLong("alter_s"),
                    j.optBoolean("mac_da"), System.currentTimeMillis(), cli = j.optJSONObject("cli"), fehler = fehler,
                    cliAlterS = if (j.isNull("cli_alter_s")) null else j.optLong("cli_alter_s"))
            }
        }.onFailure { lage.update { l -> l.copy(abrufFehler = true, geholt = System.currentTimeMillis()) } }
    }

    fun senden(context: Context, art: String, zusatz: JSONObject? = null) {
        if (sendet.value != null) return
        sendet.value = art
        meldung.value = null
        bereich.launch {
            val text = auftrag(context, art, zusatz)
            Logbuch.netz("Mac-Zentrale: $art", text ?: "angenommen")
            meldung.value = text
            sendet.value = null
            holen(context)
        }
    }

    val cliSendet = MutableStateFlow<String?>(null)
    val cliMeldung = MutableStateFlow<String?>(null)
    /** Kennung des letzten CLI-Auftrags von diesem Gerät (getrennt von der Update-Zentrale, damit Fehler im richtigen Abschnitt stehen). */
    val cliKennung = MutableStateFlow<String?>(null)

    /** Programmier-CLI: „cli_aufraeumen“ (nur nach Franks Ja) oder „cli_einstellung“ mit automatik und/oder stunden. */
    fun cliSenden(context: Context, art: String, zusatz: JSONObject? = null) {
        if (cliSendet.value != null) return
        cliSendet.value = art
        cliMeldung.value = null
        bereich.launch {
            val text = auftrag(context, art, zusatz)
            Logbuch.netz("Mac-Zentrale: $art", text ?: "angenommen")
            cliMeldung.value = text
            // Angenommen: Der Mac meldet seinen neuen Stand gleich danach, kurz nachlesen. Abgelehnt: sofort wieder bedienbar.
            if (text == null) repeat(3) { delay(1_500); holen(context) }
            cliSendet.value = null
        }
    }

    /** Schickt einen Auftrag an den Mac; null = angenommen, sonst der Grund (für Karte und Sprache). Nie von selbst wiederholt. */
    suspend fun auftrag(context: Context, art: String, zusatz: JSONObject? = null): String? = withContext(Dispatchers.IO) {
                val kennungZiel = if (art.startsWith("cli_")) cliKennung else eigeneKennung
                val e = Einstellungen.get(context)
                if (e.serverToken.isBlank()) return@withContext "Kein Server eingerichtet. Es wurde nichts gestartet."
                try {
                    val rumpf = JSONObject().put("art", art)
                    zusatz?.keys()?.forEach { rumpf.put(it, zusatz.get(it)) }
                    val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/updatezentrale-befehl")
                        .post(rumpf.toString().toRequestBody(JSON))
                        .header("X-Jarvis-Token", e.serverToken).header(Geraet.KOPFZEILE, Geraet.kennung).build()
                    NETZ.newCall(anfrage).execute().use { a ->
                        // Eine Fehlerantwort heißt: Der Relay hat nichts eingereiht.
                        if (!a.isSuccessful) return@use "Der Server hat den Auftrag abgelehnt (HTTP ${a.code}). Es wurde nichts gestartet."
                        val j = JSONObject(a.body?.string().orEmpty())
                        when (j.optString("ergebnis")) {
                            "angenommen" -> { kennungZiel.value = j.optString("kennung"); null }
                            "mac_nicht_verbunden" -> "MacBook nicht erreichbar (aus, im Ruhezustand oder ohne Netz). Es wurde nichts gestartet."
                            "laeuft_bereits" -> "Läuft bereits: " + j.optString("text") + " Es wird nichts doppelt gestartet."
                            "unklar" -> { kennungZiel.value = j.optString("kennung"); "Der Auftrag ging an den Mac, die Bestätigung fehlt. Stand unklar – er wird nicht erneut gesendet." }
                            else -> "Unerwartete Antwort vom Server."
                        }
                    }
                } catch (f: Exception) {
                    val nieGesendet = f is ConnectException || f is UnknownHostException ||
                        (f is java.net.SocketTimeoutException && f.message.orEmpty().contains("connect", ignoreCase = true))
                    if (nieGesendet) "Server nicht erreichbar. Es wurde nichts gestartet."
                    else "Verbindung abgebrochen. Ob der Auftrag ankam, ist unklar – er wird nicht erneut gesendet; die Karte zeigt gleich den Stand des Macs."
                }
    }
}

private fun uhr(sekunden: Double): String = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date((sekunden * 1000).toLong()))

private fun JSONArray?.objekte(): List<JSONObject> = (0 until (this?.length() ?: 0)).map { this!!.getJSONObject(it) }

/**
 * Startkarte „Mac-Zentrale“ mit zwei aufklappbaren Abschnitten. „Update-Zentrale“ (der frühere Inhalt): zugeklappt Zahl der Updates, Status und rechts „Aktualisieren“ (frische Prüfung am Mac, installiert
 * nichts). Aufgeklappt oben Grün „Unkritische Updates installieren“ und Rot „Kritische Updates installieren“ (nur nach Ja im Warnfenster),
 * darunter Status, lokale Freigaben, Neustarts, Ergebnis und die Updates in zwei Gruppen. „Programmier-CLI“: [CliAbschnitt].
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
    var cliOffen by rememberSaveable { mutableStateOf(false) }
    var kritischFrage by remember { mutableStateOf(false) }

    val s = l.stand
    val vorgang = s?.optJSONObject("vorgang")
    // Nur solange die App am Mac lebt: Ist sie beendet oder abgestürzt, bleibt der alte Stand sonst für immer „läuft“, und
    // Aktualisieren (startet die App über die Brücke neu) wäre gesperrt.
    val laeuft = l.app && s != null && (s.optBoolean("laeuft") || vorgang?.optString("phase") == "laeuft")
    val beendet = !l.app && s != null && (s.optBoolean("laeuft") || vorgang?.optString("phase") == "laeuft")
    // Während eines Laufs meldet der Mac mindestens alle 30 s: Bleibt das aus, ist die Verbindung weg und der Stand unklar.
    val abgerissen = laeuft && ((l.alterS ?: 0) > 120 || !l.macDa)
    val eilig by androidx.compose.runtime.rememberUpdatedState(laeuft || sendet != null || l.cli?.optBoolean("laeuft") == true)
    val aufgeklappt by androidx.compose.runtime.rememberUpdatedState(offen || cliOffen)
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
    // Jedes angezeigte Update gehört zu seinem Knopf, auch ein Hauptversionswechsel.
    val installierbarU = unkritisch.size
    val installierbarK = kritisch.size
    val bedienbar = l.macDa && !laeuft && sendet == null
    val freigabe = s?.optString("freigabe").orEmpty()

    Karte(modifier = griff) {
        val linie = trennlinie(f)
        // Kopf: Aktualisieren bleibt fest oben rechts, egal was darunter aufklappt.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(end = 10.dp).size(44.dp).glas(f, 14.dp, erhoeht = 0.3f), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LaptopMac, null, tint = f.primaer, modifier = Modifier.size(24.dp))
            }
            Text("Mac-Zentrale", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            val pruefend = laeuft && s?.optString("art") == "pruefung"
            Box(
                Modifier.size(44.dp).glas(f, 22.dp, erhoeht = 0.5f, toenung = if (pruefend) f.primaer else null)
                    .antippen { if (bedienbar) MacUpdateZentrale.senden(context, "pruefen") else if (!laeuft) MacUpdateZentrale.bereich.launch { MacUpdateZentrale.holen(context) } },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, "Am MacBook frisch prüfen", tint = if (pruefend) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
            }
        }
        androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 14.dp), thickness = 1.dp, color = linie)

        AufklappFeld(offen, { offen = !offen }, kopf = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Update-Zentrale", color = f.primaer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp), maxLines = 2,
            )
            val zeile = listOfNotNull(
                s?.optDouble("geprueft")?.takeIf { !it.isNaN() && it > 0 }?.let { "Geprüft " + uhr(it) },
                s?.optDouble("zeit")?.takeIf { !it.isNaN() && it > 0 }?.let { "Stand " + uhr(it) },
                if (!l.macDa && s != null) "MacBook nicht erreichbar" else null,
                if (l.abrufFehler) "Server nicht erreichbar" else null,
            ).joinToString(" · ")
            if (zeile.isNotEmpty()) Text(zeile, color = if (!l.macDa || l.abrufFehler) f.gefahr else f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            meldung?.let { Text(it, color = f.text, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        }) {
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

                if (beendet) Hinweis("Die Update-Zentrale am Mac wurde während des Vorgangs beendet. Ergebnis unklar – Aktualisieren startet sie neu und prüft.", Color(0xFFF5A524))
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

        Spacer(Modifier.height(10.dp))
        CliAbschnitt(l, cliOffen) { cliOffen = !cliOffen }
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
                if (p.optBoolean("hauptversion")) "Neue Hauptversion" else null,
                if (p.optBoolean("fehler")) p.optString("status") else null,
                if (mitGrund) p.optString("grund").takeIf { it.isNotBlank() } else null,
            ).filter { it.isNotBlank() }
            zusatz.forEach { Text(it, color = f.textSchwach, fontSize = 12.sp) }
        }
    }
}


/**
 * Abschnitt „Programmier-CLI“: „Aufräumen“ fragt „Möchtest du jetzt die CLI aufräumen?“; nur Ja startet am Mac das sichere Aufräumen
 * (jarvis_cli_aufraeumen.py: beendet nur Sitzungen, die eindeutig seit mehr als einer Stunde inaktiv sind, älteste zuerst). Darunter der
 * Schalter „Automatisches Aufräumen“ (zunächst aus) und das Intervall 1–24 Stunden. Schalter und Intervall speichert der Mac; die Karte
 * zeigt seinen gemeldeten Stand.
 */
@Composable
private fun CliAbschnitt(l: MacUpdateZentrale.Lage, offen: Boolean, umschalten: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val sendet by MacUpdateZentrale.cliSendet.collectAsState()
    val meldung by MacUpdateZentrale.cliMeldung.collectAsState()
    var frage by remember { mutableStateOf(false) }
    val c = l.cli
    // Ein Lauf meldet sich spätestens nach Minuten zurück; ein älterer „läuft“-Stand (Mac eingeschlafen) gilt nicht mehr.
    val laeuft = c?.optBoolean("laeuft") == true && (l.cliAlterS ?: 0) < 600
    val automatik = c?.optBoolean("automatik") == true
    var schalterWunsch by remember(automatik) { mutableStateOf<Boolean?>(null) }
    val stunden = (c?.optInt("stunden", 6) ?: 6).coerceIn(1, 24)
    val letzter = c?.optJSONObject("letzter")
    val sitzungen = c?.optJSONArray("sitzungen").objekte()
    val ohneListe = c != null && (c.isNull("sitzungen") || !c.has("sitzungen"))
    val bedienbar = l.macDa && c != null && sendet == null

    AufklappFeld(offen, umschalten, kopf = {
        Text("Programmier-CLI", color = f.primaer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                sendet == "cli_aufraeumen" || laeuft -> "Räumt am MacBook auf …"
                c == null && !l.macDa -> "MacBook nicht erreichbar"
                c == null -> "Noch kein Stand vom MacBook"
                else -> (if (ohneListe) "Sitzungen werden ermittelt …" else "${sitzungen.size} offene Sitzungen") +
                    " · Automatik " + if (automatik) "alle $stunden h" else "aus"
            },
            color = if (laeuft) f.primaer else f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp),
        )
        if (c != null && !l.macDa) Text("MacBook nicht erreichbar" + (c.optDouble("zeit").takeIf { !it.isNaN() && it > 0 }?.let { " · Stand " + uhr(it) } ?: ""),
            color = f.gefahr, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }) {
        Column {
            Knopf(if (laeuft) "Aufräumen läuft …" else "Aufräumen", Modifier.fillMaxWidth().padding(top = 12.dp).alpha(if (bedienbar && !laeuft) 1f else 0.4f)) {
                if (bedienbar && !laeuft) frage = true
            }
            Text("Beendet nur Sitzungen, die seit mehr als einer Stunde eindeutig inaktiv sind, älteste zuerst. Aktive, wartende, kürzlich " +
                "genutzte und unklare bleiben offen. Verläufe und Projektdaten bleiben erhalten.",
                color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Automatisches Aufräumen", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = schalterWunsch ?: automatik, enabled = bedienbar, onCheckedChange = {
                    schalterWunsch = it
                    MacUpdateZentrale.cliSenden(context, "cli_einstellung", JSONObject().put("automatik", it))
                })
            }
            // 0 und 25 sind nur leere Randmarken: Der Regler rastet auf 1 bis 24 ganze Stunden ein.
            // Der gewählte Wert bleibt stehen, bis der Mac ihn meldet; nur ein abgelehnter Auftrag setzt auf den Stand des Macs zurück.
            var wert by remember(stunden) { mutableStateOf(stunden.toFloat()) }
            LaunchedEffect(sendet, meldung) { if (sendet == null && meldung != null) { wert = stunden.toFloat(); schalterWunsch = null } }
            Text("Intervall: alle ${wert.toInt()} " + if (wert.toInt() == 1) "Stunde" else "Stunden",
                color = f.text, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            androidx.compose.material3.Slider(
                value = wert, onValueChange = { wert = kotlin.math.round(it).coerceIn(1f, 24f) }, valueRange = 0f..25f, steps = 24,
                enabled = bedienbar, modifier = Modifier.fillMaxWidth().height(48.dp),
                onValueChangeFinished = {
                    val neu = wert.toInt().coerceIn(1, 24)
                    if (neu != stunden) MacUpdateZentrale.cliSenden(context, "cli_einstellung", JSONObject().put("stunden", neu))
                },
            )
            if (automatik) c?.optDouble("naechster")?.takeIf { !it.isNaN() && it > 0 }?.let {
                Text("Nächster Lauf " + uhr(it) + " Uhr", color = f.textLeise, fontSize = 13.sp)
            }
            meldung?.let { Text(it, color = f.text, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }
            letzter?.let { e ->
                val wann = e.optDouble("zeit").takeIf { !it.isNaN() }?.let { " (" + uhr(it) + ")" }.orEmpty()
                Hinweis((if (e.optString("art") == "auto") "Automatisch: " else "Zuletzt: ") + e.optString("text") + wann,
                    if (e.optJSONArray("beendet").objekte().isNotEmpty()) f.erfolg else f.primaer)
                e.optJSONArray("beendet").objekte().forEach { b ->
                    Text("✓ " + b.optString("name") + " beendet" + (b.optInt("inaktiv_min", -1).takeIf { it >= 0 }?.let { " (${it / 60} h inaktiv)" } ?: ""),
                        color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (sitzungen.isNotEmpty()) {
                Text("Offene Sitzungen (${sitzungen.size})", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                sitzungen.forEach { x ->
                    val min = x.optInt("inaktiv_min", -1)
                    val dauer = when { min < 0 -> ""; min < 60 -> " · $min min"; else -> " · ${min / 60} h ${min % 60} min" }
                    Text(x.optString("name") + " – " + when (x.optString("zustand")) {
                        "aktiv" -> "aktiv"; "wartet" -> "wartet auf Antwort"; "kuerzlich" -> "kürzlich genutzt"
                        "inaktiv" -> "inaktiv"; else -> "unklar"
                    } + dauer, color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }

    if (frage) AlertDialog(
        onDismissRequest = { frage = false },
        text = { Text("Möchtest du jetzt die CLI aufräumen?") },
        confirmButton = { TextButton({ frage = false; MacUpdateZentrale.cliSenden(context, "cli_aufraeumen") }) { Text("Ja") } },
        dismissButton = { TextButton({ frage = false }) { Text("Nein") } },
    )
}


/** Linie für Trennstrich und Rahmen: Im hellen Modus wäre f.rand (fast weiß) unsichtbar. */
internal fun trennlinie(f: de.frank.jarvis.ui.theme.Farben): Color = if (f.dunkel) f.rand else f.primaer.copy(alpha = 0.28f)

/**
 * Umrandetes Aufklapp-Feld der Mac-Zentrale (auch auf der Karte „E-Mails“): Kopf mit [kopf] und dem Pfeil in eigener Fläche (bei allen Feldern an derselben Stelle),
 * aufgeklappt der [inhalt] innerhalb der Umrandung. Nur die Kopfzeile reagiert auf Tippen.
 */
@Composable
internal fun AufklappFeld(offen: Boolean, umschalten: () -> Unit, kopf: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
                         inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    val linie = trennlinie(f)
    val drehung by androidx.compose.animation.core.animateFloatAsState(if (offen) 180f else 0f, label = "feldPfeil")
    val form = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().glas(f, 16.dp, erhoeht = 0.15f, verlaufHoehe = 72.dp)
        .border(1.dp, if (offen) f.primaer.copy(alpha = if (f.dunkel) 0.45f else 0.5f) else linie, form)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).antippen(haptik = false) { umschalten() }
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 10.dp), content = kopf)
            Box(Modifier.size(32.dp).glas(f, 10.dp, erhoeht = 0.2f, toenung = f.primaer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Zuklappen" else "Aufklappen",
                    Modifier.size(22.dp).graphicsLayer { rotationZ = drehung }, tint = f.primaer)
            }
        }
        AnimatedVisibility(offen, enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()) {
            Column {
                androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = linie.copy(alpha = linie.alpha * 0.6f))
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { inhalt() }
            }
        }
    }
}

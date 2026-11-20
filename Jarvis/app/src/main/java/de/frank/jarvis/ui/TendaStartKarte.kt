package de.frank.jarvis.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.TendaFaehigkeit
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Läufe der Karte außerhalb der Komposition: Wegscrollen bricht nichts ab (wie Victron). */
private object TendaStartStand {
    var automatischGelesen = false
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

/**
 * Startbereich „Tenda Internet“: zugeklappt aktiver Modus, Multiplayer-Einschätzung und Stand, rechts der Aktualisieren-Knopf
 * (Daumenreichweite). Tipp auf die Karte klappt auf: Verbindung (Ping, Jitter, Verlust), LTE- und 5G-Werte mit Einheit und Bewertung,
 * Bedienung (Modus, Connect, Neustart mit Ja/Nein) und das Router-Passwort. Alle Texte und Bewertungen kommen aus [TendaFaehigkeit.struktur].
 */
@Composable
fun TendaStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val faehigkeit = remember(context) { de.frank.jarvis.faehigkeit.Register.alle(context).filterIsInstance<TendaFaehigkeit>().first() }
    val z by faehigkeit.zustand.collectAsState()
    var offen by rememberSaveable { mutableStateOf(false) }
    var einrichten by remember { mutableStateOf(false) }
    var neustartFrage by remember { mutableStateOf(false) }
    var eingerichtet by remember { mutableStateOf(faehigkeit.eingerichtet()) }
    fun lesen() { TendaStartStand.bereich.launch { faehigkeit.abrufen() } }
    LaunchedEffect(Unit) {
        if (z.stand == null) faehigkeit.letzter()?.let { s -> if (faehigkeit.zustand.value.stand == null) faehigkeit.zeigen(s) }
        if (!TendaStartStand.automatischGelesen && eingerichtet) { TendaStartStand.automatischGelesen = true; lesen() }
    }
    val s = z.stand
    val st: JSONObject? = remember(s) { s?.let { faehigkeit.struktur(it) } }
    val drehung by androidx.compose.animation.core.animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")

    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(end = 10.dp).size(44.dp).glas(f, 14.dp, erhoeht = 0.3f), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SettingsInputAntenna, null, tint = f.primaer, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f).antippen(haptik = false) { offen = !offen }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tenda Internet", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    st?.optString("modus_aktiv")?.takeIf { it.isNotBlank() && it != "null" }?.let { m ->
                        Text(m.removeSuffix(" (abgeleitet)"), Modifier.padding(start = 8.dp).glas(f, 10.dp, erhoeht = 0.2f, toenung = f.primaer).padding(horizontal = 8.dp, vertical = 2.dp),
                            color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                val mp = st?.optJSONObject("multiplayer")
                Text(
                    when {
                        !eingerichtet -> "Router-Passwort fehlt – zum Einrichten antippen"
                        mp != null -> "Multiplayer: " + mp.optString("text")
                        z.liest -> "Liest den Router …"
                        else -> "Noch kein Stand"
                    },
                    color = when (mp?.optString("stufe")) { "exzellent", "gut" -> f.erfolg; "schlecht", "sehr schlecht" -> f.gefahr; else -> f.text },
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    when {
                        z.schaltet != null -> z.schaltet + " läuft …"
                        z.liest -> "Frische Abfrage läuft …"
                        st != null -> standZeile(st)
                        else -> ""
                    },
                    color = if (z.liest || z.schaltet != null) f.primaer else if (st?.optString("gueltigkeit") == "veraltet" || !st.istNull("veraltet_grund")) f.gefahr else f.textLeise,
                    fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.offset(x = 4.dp, y = (-6).dp).size(44.dp).glas(f, 22.dp, erhoeht = 0.5f, toenung = if (z.liest) f.primaer else null)
                        .antippen { if (eingerichtet) lesen() else { offen = true; einrichten = true } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, if (z.liest) "Router wird gelesen" else "Router frisch abfragen", tint = if (z.liest) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Zuklappen" else "Aufklappen",
                    Modifier.padding(top = 2.dp).size(24.dp).graphicsLayer { rotationZ = drehung }.antippen(haptik = false) { offen = !offen }, tint = f.textLeise)
            }
        }
        z.meldung?.let { Text(it, color = f.text, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }

        AnimatedVisibility(offen) {
            Column {
                if (st != null) {
                    if (!st.istNull("veraltet_grund")) Text("Keine frische Abfrage: " + st.optString("veraltet_grund"), color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    st.optJSONObject("multiplayer")?.optString("begruendung")?.let { Text("Multiplayer-Grundlage: $it", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                    val abschnitte = st.optJSONArray("abschnitte")
                    for (i in 0 until (abschnitte?.length() ?: 0)) {
                        val a = abschnitte!!.getJSONObject(i)
                        Text(a.optString("titel"), color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                        val zeilen = a.optJSONArray("zeilen")
                        for (k in 0 until (zeilen?.length() ?: 0)) MesswertZeile(zeilen!!.getJSONObject(k))
                    }
                    Text("Modus eingestellt: " + (st.optString("modus_eingestellt").takeIf { it.isNotBlank() && it != "null" } ?: "nicht gelesen") +
                        " · aktiv: " + (st.optString("modus_aktiv").takeIf { it.isNotBlank() && it != "null" } ?: "nicht geliefert"),
                        color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                }
                Text("Bedienung", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                if (eingerichtet) {
                    ModusWahl(s?.let { faehigkeit.eingestellterModus(it) }, aktiv = z.schaltet == null) { wert ->
                        TendaStartStand.bereich.launch { faehigkeit.schalten("modus", wert) }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        Knopf("Connect", haupt = false) { if (z.schaltet == null) TendaStartStand.bereich.launch { faehigkeit.schalten("verbinden", "") } }
                        Knopf("Router neu starten", haupt = false, farbe = f.gefahr) { if (z.schaltet == null) neustartFrage = true }
                    }
                    Text("Moduswechsel, Connect und Neustart unterbrechen das Internet kurz (Neustart zwei bis vier Minuten). Danach prüft Jarvis den neuen Zustand; unklare Befehle löst er nicht erneut aus.",
                        color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    Text(if (eingerichtet) "Router-Passwort ändern" else "Router-Passwort einrichten", Modifier.antippen { einrichten = true },
                        color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (neustartFrage) AlertDialog(
        onDismissRequest = { neustartFrage = false },
        title = { Text("Router neu starten") },
        text = { Text("Willst du den Router wirklich neu starten?") },
        confirmButton = { TextButton({ neustartFrage = false; TendaStartStand.bereich.launch { faehigkeit.schalten("neustart", "") } }) { Text("Ja") } },
        dismissButton = { TextButton({ neustartFrage = false }) { Text("Nein") } },
    )
    if (einrichten) PasswortDialog(faehigkeit, schliessen = { einrichten = false; eingerichtet = faehigkeit.eingerichtet() })
}

private fun JSONObject?.istNull(k: String): Boolean = this == null || isNull(k) || optString(k).isBlank()

private fun standZeile(st: JSONObject): String = "Stand " + TendaFaehigkeit.uhrzeit(st.optLong("zeit")) + " · " + st.optString("quelle") +
    when (st.optString("gueltigkeit")) { "veraltet" -> " · veraltet"; "älter" -> " · älter"; else -> "" }

@Composable
private fun MesswertZeile(z: JSONObject) {
    val f = LocalFarben.current
    val bewertung = z.optString("bewertung").takeIf { !z.isNull("bewertung") && it.isNotBlank() }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(z.optString("name"), color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
        val fehlt = z.optString("wert").startsWith("nicht ")
        Text(z.optString("wert"), color = if (fehlt) f.textSchwach else f.text, fontSize = 15.sp, fontWeight = if (fehlt) FontWeight.Normal else FontWeight.SemiBold)
        if (bewertung != null) Text(bewertung, Modifier.padding(start = 8.dp).glas(f, 8.dp, erhoeht = 0.1f, toenung = stufenFarbe(bewertung)).padding(horizontal = 6.dp, vertical = 1.dp),
            color = stufenFarbe(bewertung), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun stufenFarbe(stufe: String): Color {
    val f = LocalFarben.current
    return when (stufe) { "exzellent", "gut" -> f.erfolg; "mittel" -> Color(0xFFF5A524); else -> f.gefahr }
}

@Composable
private fun ModusWahl(eingestellt: String?, aktiv: Boolean, waehlen: (String) -> Unit) {
    val f = LocalFarben.current
    var auf by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Modus", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Box {
            Knopf((eingestellt ?: "wählen") + " ▾", haupt = false) { if (aktiv) auf = true }
            DropdownMenu(auf, { auf = false }) {
                TendaFaehigkeit.WAEHLBAR.forEach { (wert, name) ->
                    DropdownMenuItem(text = { Text(if (name == eingestellt) "✓ $name" else name) }, onClick = { auf = false; if (name != eingestellt) waehlen(wert) })
                }
            }
        }
    }
}

@Composable
private fun PasswortDialog(faehigkeit: TendaFaehigkeit, schliessen: () -> Unit) {
    val f = LocalFarben.current
    var passwort by remember { mutableStateOf("") }
    var laeuft by remember { mutableStateOf(false) }
    var ergebnis by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!laeuft) { passwort = ""; schliessen() } },
        title = { Text("Router-Passwort") },
        text = {
            Column {
                Text("Das Anmeldepasswort von tendawifi.com. Jarvis speichert nur eine verschlüsselte Prüfsumme und gibt sie an den Mac zu Hause weiter. " +
                    "Zu Hause wird es einmal am Router geprüft (der Router sperrt nach vier Fehlversuchen).", color = f.textLeise, fontSize = 13.sp)
                Eingabe(passwort, { passwort = it }, "Passwort", Modifier.fillMaxWidth().padding(top = 10.dp), geheim = true, tastatur = KeyboardType.Password)
                ergebnis?.let { Text(it, color = f.text, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton({
                if (laeuft) return@TextButton
                if (ergebnis != null) { schliessen(); return@TextButton }
                laeuft = true
                val eingabe = passwort
                passwort = ""
                TendaStartStand.bereich.launch {
                    ergebnis = runCatching { faehigkeit.einrichten(eingabe) }.getOrElse { it.message ?: "Fehler" }
                    laeuft = false
                }
            }) { Text(if (laeuft) "Prüft …" else if (ergebnis != null) "Fertig" else "Speichern") }
        },
        dismissButton = { if (ergebnis == null) TextButton({ passwort = ""; schliessen() }) { Text("Abbrechen") } },
    )
}

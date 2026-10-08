package de.frank.jarvis.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.deviceCodeGroups
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.tunnel.TunnelStufe
import de.frank.jarvis.tunnel.TunnelZustand
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.glas

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EinstellungenBildschirm(vm: AppViewModel, tunnel: TunnelZustand, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Einstellungen", color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))

        // ---------------------------------------------------------------- Kopf
        Abschnitt("Jarvis' Kopf")
        Karte {
            Zeile("ChatGPT-Konto", if (vm.kiVerbunden) vm.kiKonto.ifEmpty { "Verbunden" } else "Nicht verbunden", if (vm.kiVerbunden) f.erfolg else f.textLeise)
            val code = vm.geraeteCode
            when {
                code != null -> {
                    Text("Gib diesen Code auf der Anmeldeseite ein. Er liegt schon in der Zwischenablage.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        deviceCodeGroups(code.userCode).forEachIndexed { i, gruppe ->
                            if (i > 0) Text("–", color = f.textSchwach, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            Text(gruppe, Modifier.glas(f, 14.dp, erhoeht = 0.4f, fuellung = f.flaecheStark).padding(horizontal = 14.dp, vertical = 10.dp), color = f.primaer, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Knopf("Code kopieren", Modifier.weight(1f), Icons.Rounded.ContentCopy, haupt = false) { vm.kopiere(code.userCode, "Code") }
                        Knopf("Abbrechen", Modifier.weight(1f), haupt = false) { vm.kiAbbrechen() }
                    }
                }
                vm.kiVerbindet -> Text("Anmeldung wird vorbereitet …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                vm.kiVerbunden -> Knopf("Abmelden", Modifier.padding(top = 12.dp).fillMaxWidth(), haupt = false) { vm.kiTrennen() }
                else -> Knopf("Mit ChatGPT verbinden", Modifier.padding(top = 12.dp).fillMaxWidth()) { vm.kiVerbinden(activity) }
            }

            Unterzeile("Modell")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CodexModel.entries.forEach { m -> Chip(m.label, aktiv = e.modell == m) { vm.setzeModell(m) } }
            }
            Unterzeile("Denkstufe")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                e.modell.supportedEfforts.forEach { s -> Chip(s.label, aktiv = e.denkstufe == s) { vm.setzeDenkstufe(s) } }
            }
            Text("Niedrig antwortet am schnellsten. Höhere Stufen denken gründlicher, brauchen aber länger.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        // ---------------------------------------------------------------- Plugin
        Abschnitt("ChatGPT-Plugin")
        Karte {
            val (stand, farbe) = when (tunnel.stufe) {
                TunnelStufe.ONLINE -> "Online" to f.erfolg
                TunnelStufe.VERBINDET -> tunnel.meldung to f.primaer
                TunnelStufe.NICHT_EINGERICHTET -> "Nicht eingerichtet" to f.textLeise
                TunnelStufe.FEHLER -> tunnel.meldung to f.gefahr
                TunnelStufe.AUS -> "Aus" to f.textLeise
            }
            Zeile("Verbindung zum Server", stand, farbe)
            Text("Jarvis hält von sich aus eine Verbindung zu deinem Server. ChatGPT ruft den Server an, der reicht den Aufruf ans Handy weiter.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))

            val adresse = vm.pluginAdresse
            if (adresse.isNotEmpty()) {
                Unterzeile("Plugin-Adresse für ChatGPT")
                Text(adresse, Modifier.fillMaxWidth().glas(f, 14.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(14.dp), color = f.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Knopf("Kopieren", Modifier.weight(1f), Icons.Rounded.ContentCopy) { vm.kopiere(adresse, "Plugin-Adresse") }
                    Knopf("Teilen", Modifier.weight(1f), Icons.Rounded.Share, haupt = false) {
                        runCatching { activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, adresse), "Plugin-Adresse teilen")) }
                    }
                }
                Text("Die Adresse ist dein Schlüssel: Wer sie kennt, kann deine Aufgaben lesen und ändern. Gib sie nur in ChatGPT ein.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }

            // Nur nötig, wenn die App ohne eingebaute Server-Daten gebaut wurde (z. B. in der Cloud).
            if (tunnel.stufe == TunnelStufe.NICHT_EINGERICHTET || (tunnel.stufe == TunnelStufe.FEHLER && "Schlüssel" in tunnel.meldung)) {
                var host by rememberSaveable { mutableStateOf(e.serverHost) }
                var token by rememberSaveable { mutableStateOf("") }
                Unterzeile("Server-Adresse")
                Eingabe(host, { host = it }, "z. B. srv1774016.hstgr.cloud", Modifier.fillMaxWidth())
                Unterzeile("Server-Schlüssel")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eingabe(token, { token = it }, "Schlüssel einfügen", Modifier.weight(1f), geheim = true)
                    Spacer(Modifier.width(8.dp))
                    Knopf("Einfügen", icon = Icons.Rounded.ContentPaste, haupt = false) { vm.ausZwischenablage().takeIf { it.isNotEmpty() }?.let { token = it } }
                }
                Knopf("Speichern und verbinden", Modifier.padding(top = 12.dp).fillMaxWidth()) { vm.serverSpeichern(host, token) }
            }
        }

        Karte {
            Text("So kommt Jarvis in ChatGPT", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                "Warten, bis oben „Online“ steht. Dann die Plugin-Adresse kopieren oder an den PC teilen.",
                "Am PC chatgpt.com öffnen: Einstellungen → Apps und Konnektoren → Erweitert → Entwicklermodus einschalten.",
                "Dort „Erstellen“ wählen: Name „Jarvis“, als MCP-Server-URL die Plugin-Adresse, Authentifizierung „Keine“. Bestätigen.",
                "Im Chat oder im Sprachmodus sagen: „Jarvis, was steht heute an?“ Der erste Aufruf erscheint hier unter Aktivität.",
            ).forEachIndexed { i, text ->
                Row(Modifier.padding(top = 10.dp)) {
                    Box(Modifier.size(24.dp).glas(f, 12.dp, erhoeht = 0.2f), contentAlignment = Alignment.Center) { Text("${i + 1}", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    Text(text, Modifier.padding(start = 10.dp), color = f.textLeise, fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
        }

        // ---------------------------------------------------------------- Apps
        Abschnitt("Angebundene Apps")
        Karte {
            Register.alle(activity).forEach { app ->
                val stoerung = if (app.id == "aufgaben") vm.aufgabenStoerung else null
                Zeile(app.name, if (stoerung == null) "Bereit · ${app.werkzeuge.size} Werkzeuge" else "Gestört", if (stoerung == null) f.erfolg else f.gefahr)
                Text(stoerung ?: app.beschreibung, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text("Weitere Apps wie Ideen, Journal und Entropie-Reduktor lassen sich hier später andocken.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }

        // ---------------------------------------------------------------- System
        Abschnitt("System")
        Karte {
            Zeile("Bereitschaft", if (e.dienstAn) "Jarvis läuft im Hintergrund" else "Angehalten", if (e.dienstAn) f.erfolg else f.textLeise)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("An", aktiv = e.dienstAn) { vm.dienstSchalten(true) }
                Chip("Aus", aktiv = !e.dienstAn) { vm.dienstSchalten(false) }
            }
            if (!vm.akkuFrei) {
                Text("Android darf Jarvis bei gesperrtem Handy noch schlafen legen.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp))
                Knopf("Akku-Sparen für Jarvis ausschalten", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) {
                    runCatching { activity.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${activity.packageName}"))) }
                }
            }
            if (!vm.hinweiseAn) {
                Text("Ohne Benachrichtigungen fehlt die Statuszeile, die Jarvis wach hält.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp))
                Knopf("Benachrichtigungen erlauben", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { vm.hinweiseAnfragen() }
            }
            Unterzeile("Darstellung")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "Automatisch", "hell" to "Hell", "dunkel" to "Dunkel").forEach { (id, name) -> Chip(name, aktiv = e.modus == id) { e.modus = id } }
            }
            Unterzeile("Sicherheit")
            Knopf("Plugin-Adresse erneuern", Modifier.fillMaxWidth(), haupt = false, farbe = f.gefahr) { vm.adresseErneuern() }
            Text("Macht die bisherige Adresse ungültig. Danach in ChatGPT neu eintragen.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Text(
            "Jarvis ${BuildConfig.VERSION_NAME} · Stand ${BuildConfig.VERSION_BUMPED_AT}",
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp), color = f.textSchwach, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Zeile(titel: String, wert: String, farbe: Color) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titel, Modifier.weight(1f), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.size(8.dp).glas(f, 4.dp, 0f, farbe, rand = false))
        Text(wert, Modifier.padding(start = 8.dp).weight(1.2f, fill = false), color = farbe, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 3)
    }
}

@Composable
private fun Unterzeile(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 8.dp), color = LocalFarben.current.textLeise, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

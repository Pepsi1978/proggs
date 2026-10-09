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

        // ---------------------------------------------------------------- Tagesauswertung
        Abschnitt("Tagesauswertungs-Synchronisation")
        Karte {
            Zeile("Automatisch synchronisieren", if (e.auswertungAn) "Nächste: ${vm.naechsteAuswertung}" else "Aus", if (e.auswertungAn) f.erfolg else f.textLeise)
            Text("Jarvis liest dabei alle angebundenen Apps und schreibt die Tagesauswertung neu: Rückblick, aktueller Tag und Ausblick. In ChatGPT fragst du dann nur: „Wie ist meine Tagesauswertung?“", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("An", aktiv = e.auswertungAn) { vm.auswertungSchalten(true) }
                Chip("Aus", aktiv = !e.auswertungAn) { vm.auswertungSchalten(false) }
            }

            Unterzeile("Abstand")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                de.frank.jarvis.auswertung.Zeitplan.ABSTAENDE.forEach { stunden ->
                    Chip(if (stunden == 1) "Jede Stunde" else "Alle $stunden Stunden", aktiv = e.auswertungAbstand == stunden) { vm.auswertungAbstand(stunden) }
                }
            }

            Unterzeile("Im Schlaf pausieren")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("An", aktiv = e.auswertungSchlafpause) { vm.auswertungSchlafpause(true) }
                Chip("Aus", aktiv = !e.auswertungSchlafpause) { vm.auswertungSchlafpause(false) }
            }
            Text("Nach dem Dienstplan: vor einem Tagdienst ruht die Synchronisation von 20 bis 4 Uhr, nach einem Nachtdienst von 6 bis 15 Uhr. An freien Tagen läuft sie durch.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

            Unterzeile("Frische Biodaten holen")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("An", aktiv = e.auswertungAbgleich) { vm.auswertungAbgleich(true) }
                Chip("Aus", aktiv = !e.auswertungAbgleich) { vm.auswertungAbgleich(false) }
            }
            Text("Stößt vor jedem Lauf den Abgleich mit Whoop, Oura und Waage in Entropie Reductor an. Aus = Jarvis nimmt den Stand, der dort schon liegt.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

            Unterzeile("Auswertung vom Modell schreiben lassen")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("An", aktiv = e.auswertungDeutung) { vm.auswertungDeutung(true) }
                Chip("Aus", aktiv = !e.auswertungDeutung) { vm.auswertungDeutung(false) }
            }
            Text("Aus = Jarvis synchronisiert nur die Daten und speichert den reinen Datenbericht, ohne das oben gewählte Modell zu fragen.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

            Knopf("Jetzt synchronisieren", Modifier.padding(top = 14.dp).fillMaxWidth(), haupt = false) { vm.auswertungJetzt() }
        }

        // ---------------------------------------------------------------- Sprache
        SpracheEinstellungen(vm)

        // ---------------------------------------------------------------- Kalender und Wetter
        Abschnitt("Kalender und Wetter")
        Karte {
            val lesen = vm.stoerungen["kalender"] == null
            Zeile("Kalender", if (!lesen) "Kein Zugriff" else if (vm.kalenderSchreibenErlaubt) "Lesen und Schreiben" else "Nur Lesen", if (lesen) f.erfolg else f.gefahr)
            if (!lesen || !vm.kalenderSchreibenErlaubt) Knopf(if (lesen) "Schreiben erlauben" else "Kalender-Zugriff erlauben", Modifier.padding(top = 10.dp).fillMaxWidth(), haupt = false) { vm.kalenderAnfragen() }
            Text("Jarvis trägt Termine in den Kalender ein, in dem deine Dienste stehen. Google übernimmt sie mit der nächsten Synchronisierung.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Unterzeile("Ort für die Wettervorhersage")
            var ort by rememberSaveable { mutableStateOf(e.wetterOrt) }
            Eingabe(ort, { ort = it }, "Ort", Modifier.fillMaxWidth())
            if (ort.trim() != e.wetterOrt) Knopf(if (vm.wetterSuchtOrt) "Suche …" else "Ort übernehmen", Modifier.padding(top = 10.dp).fillMaxWidth()) { vm.wetterOrtSetzen(ort) }
        }

        // ---------------------------------------------------------------- Tagebuch
        Abschnitt("Tagebuch (Google Drive)")
        Karte {
            val stoerung = vm.stoerungen["tagebuch"]
            Zeile("Lesezugang", if (stoerung == null) "In Ordnung" else "Gestört", if (stoerung == null) f.erfolg else f.gefahr)
            Text(stoerung ?: "Dein Server liest die Tagebuch-Dateien mit einem Nur-Lese-Zugang aus Google Drive. Sollte Google ihn einmal widerrufen, erteilst du ihn hier neu.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            Knopf(if (vm.driveErneuertGerade) "Warte auf Google …" else "Berechtigung erneuern", Modifier.padding(top = 12.dp).fillMaxWidth(), haupt = stoerung != null) { vm.driveBerechtigungErneuern() }
            Text("Es öffnet sich die Google-Seite. Konto wählen, „Zulassen“ tippen, zurück zu Jarvis. Angefragt wird nur das Lesen von Drive.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        // ---------------------------------------------------------------- E-Mail
        Abschnitt("E-Mail (Gmail)")
        Karte {
            val eingerichtet = e.mailPasswort.isNotBlank()
            Zeile("Senden und Lesen", if (eingerichtet) "Eingerichtet" else "Nicht eingerichtet", if (eingerichtet) f.erfolg else f.textLeise)
            Text("Jarvis braucht dafür ein App-Passwort deines Google-Kontos: Google-Konto → Sicherheit → Bestätigung in zwei Schritten → App-Passwörter, dort eines für „Jarvis“ erzeugen und die 16 Zeichen hier einfügen. Dein normales Passwort gehört nicht hierher.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            var adresse by rememberSaveable { mutableStateOf(e.mailAdresse) }
            // Das gespeicherte Passwort steht verdeckt im Feld; das Auge zeigt es.
            var passwort by rememberSaveable { mutableStateOf(e.mailPasswort) }
            var empfaenger by rememberSaveable { mutableStateOf(e.mailEmpfaenger) }
            Unterzeile("Deine Gmail-Adresse")
            Eingabe(adresse, { adresse = it }, "name@gmail.com", Modifier.fillMaxWidth())
            Unterzeile("App-Passwort")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eingabe(passwort, { passwort = it }, "16 Zeichen", Modifier.weight(1f), geheim = true)
                Spacer(Modifier.width(8.dp))
                Knopf("Einfügen", icon = Icons.Rounded.ContentPaste, haupt = false) { vm.ausZwischenablage().takeIf { it.isNotEmpty() }?.let { passwort = it } }
            }
            Unterzeile("Weitere erlaubte Empfänger (optional, mit Komma getrennt)")
            Eingabe(empfaenger, { empfaenger = it }, "leer = Jarvis sendet nur an dich", Modifier.fillMaxWidth())
            Knopf("Speichern", Modifier.padding(top = 12.dp).fillMaxWidth()) { vm.mailSpeichern(adresse, passwort, empfaenger) }
            if (eingerichtet) Knopf("App-Passwort entfernen", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false, farbe = f.gefahr) { vm.mailEntfernen() }
            Text("Jarvis sendet nur an dich und an die hier freigegebenen Adressen. E-Mails kommen nicht in die Tagesauswertung.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        // ---------------------------------------------------------------- GitHub
        Abschnitt("GitHub-Repo")
        Karte {
            val mitSchluessel = e.githubToken.isNotBlank()
            Zeile("Nur lesen", if (mitSchluessel) "Mit Code-Suche" else "Ohne Schlüssel", if (mitSchluessel) f.erfolg else f.textLeise)
            Text("Jarvis liest dein ganzes Repo und wertet es aus, ändert dort aber nie etwas. Das geht ohne Schlüssel (60 Abfragen je Stunde). Für die Code-Suche und mehr Abfragen: " +
                "GitHub → Settings → Developer settings → Fine-grained tokens, Zugriff „Public repositories (read-only)“ genügt.",
                color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Schluesselfeld(vm, "Repo", e.repoName, geheim = false) { e.repoName = it }
            Schluesselfeld(vm, "GitHub-Schlüssel", e.githubToken) { e.githubToken = it }
        }

        // ---------------------------------------------------------------- Apps
        Abschnitt("Angebundene Apps und Fähigkeiten")
        Karte {
            Register.alle(activity).forEachIndexed { i, app ->
                val stoerung = vm.stoerungen[app.id]
                if (i > 0) Spacer(Modifier.height(14.dp))
                Zeile(app.name, if (stoerung == null) "Bereit · ${app.werkzeuge.size} Werkzeuge" else "Gestört", if (stoerung == null) f.erfolg else f.gefahr)
                Text(stoerung ?: app.beschreibung, color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                if (app.id == "kalender" && stoerung != null) Knopf("Kalender-Zugriff erlauben", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { vm.kalenderAnfragen() }
            }
            Text("Weitere Apps wie das Journal lassen sich hier später andocken.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
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
            Unterzeile("App-Sperre mit Fingerabdruck")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("An", aktiv = e.appSperre) { e.appSperre = true }
                Chip("Aus", aktiv = !e.appSperre) { e.appSperre = false }
            }
            Text("Gesperrt wird beim Öffnen und nach 30 Sekunden außerhalb der App. Für ChatGPT bleibt Jarvis trotzdem erreichbar.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
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
internal fun Zeile(titel: String, wert: String, farbe: Color) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titel, Modifier.weight(1f), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.size(8.dp).glas(f, 4.dp, 0f, farbe, rand = false))
        Text(wert, Modifier.padding(start = 8.dp).weight(1.2f, fill = false), color = farbe, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 3)
    }
}

@Composable
internal fun Unterzeile(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 8.dp), color = LocalFarben.current.textLeise, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

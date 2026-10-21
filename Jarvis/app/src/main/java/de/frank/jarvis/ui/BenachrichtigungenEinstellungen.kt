package de.frank.jarvis.ui

import android.app.NotificationManager
import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsart
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.meldung.MeldungsProfil
import de.frank.jarvis.tts.TtsCatalog
import de.frank.jarvis.tts.TtsProvider
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/**
 * Einstellungen → Benachrichtigungen: oben der Überblick mit Erlaubnis, Ruhezeit und Sperrbildschirm, darunter jede
 * Meldung als Karte. Antippen klappt die Karte auf: Ton, Wecker, Vorlesen mit Stimme, Tempo und Lautstärke, Probe.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BenachrichtigungenEinstellungen(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    val profile = Meldungsarten.alle.map { MeldungsProfil(e, it) }
    // Aufnahme und Vorlesen erscheinen nur, während sie laufen: nicht als „eingeschaltet“ zählen.
    val aktiv = profile.count { istAn(vm, it) && it.art != Meldungsarten.AUFNAHME && it.art != Meldungsarten.VORLESEN }
    val vorgelesen = profile.count { p -> istAn(vm, p) && ((p.art.vorlesen && p.vorlesen) || p.art.sprichtImmer) }
    val wecker = profile.count { p -> istAn(vm, p) && p.weckerWirksam } + if (e.timerWecker) 1 else 0

    // ---------------------------------------------------------------- Überblick
    Karte(toenung = if (vm.hinweiseAn) f.primaer else f.gefahr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Abzeichen(if (vm.hinweiseAn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, if (vm.hinweiseAn) f.primaer else f.gefahr, groesse = 52)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text("Benachrichtigungen", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(if (vm.hinweiseAn) "Erlaubt · ${Meldungsarten.alle.size} Meldungen von Jarvis" else "Android hat Jarvis die Benachrichtigungen verboten",
                    color = if (vm.hinweiseAn) f.textLeise else f.gefahr, fontSize = 13.sp)
            }
        }
        if (!vm.hinweiseAn) {
            Text("Ohne Benachrichtigungen fehlen alle Meldungen und die Statuszeile, die Jarvis im Hintergrund wach hält.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
            Knopf("Benachrichtigungen erlauben", Modifier.padding(top = 10.dp).fillMaxWidth()) { vm.hinweiseAnfragen() }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kennzahl("$aktiv", "eingeschaltet", Icons.Rounded.Notifications, Modifier.weight(1f))
            Kennzahl("$vorgelesen", "vorgelesen", Icons.Rounded.RecordVoiceOver, Modifier.weight(1f))
            Kennzahl("$wecker", "als Wecker", Icons.Rounded.Alarm, Modifier.weight(1f))
        }
        Text("Tippe auf eine Meldung: Ton, Vorlesen mit Stimme und Lautstärke, Wecker und Probe. Im Live-Gespräch bleibt alles lautlos und Vorlesen (auch Update-Ansagen) wartet, bis das Gespräch vorbei ist; nur Losfahren, Timer und Rückfragen beim Programmieren melden sich sofort.",
            color = f.textSchwach, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 14.dp))
    }

    // ---------------------------------------------------------------- Ruhezeit und Sperrbildschirm
    Karte {
        SchalterZeile(Icons.Rounded.Bedtime, "Ruhezeit", if (e.meldungRuheAn) "${e.meldungRuheVon} bis ${e.meldungRuheBis} Uhr" else "Aus", e.meldungRuheAn) { e.meldungRuheAn = it }
        Text("In der Ruhezeit kommen Agenten, Ablage, Programmieraufträge und Update-Ansagen lautlos, ohne Vorlesen und Wecker. Losfahren und Timer melden sich trotzdem.",
            color = f.textSchwach, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
        if (e.meldungRuheAn) {
            var waehle by rememberSaveable { mutableStateOf("") }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Knopf("Von ${e.meldungRuheVon}", Modifier.weight(1f), haupt = false) { waehle = "von" }
                Knopf("Bis ${e.meldungRuheBis}", Modifier.weight(1f), haupt = false) { waehle = "bis" }
            }
            if (waehle.isNotEmpty()) {
                val von = waehle == "von"
                Uhrzeitwahl(if (von) "Ruhezeit beginnt" else "Ruhezeit endet", if (von) e.meldungRuheVon else e.meldungRuheBis, { waehle = "" }) { zeit ->
                    if (von) e.meldungRuheVon = zeit else e.meldungRuheBis = zeit
                    waehle = ""
                }
            }
        }
        Trenner()
        SchalterZeile(Icons.Rounded.Lock, "Auf dem Sperrbildschirm ausblenden", if (e.meldungInhaltVerbergen) "Erst nach dem Entsperren sichtbar" else "Auch gesperrt sichtbar", e.meldungInhaltVerbergen) { e.meldungInhaltVerbergen = it }
        Text("Gilt für Agenten, Ablage, Programmieraufträge und Losfahren: Sie erscheinen erst, wenn du das Handy entsperrst. Ton, Wecker und Vorlesen bleiben.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        Knopf("Alle Jarvis-Benachrichtigungen in Android", Modifier.padding(top = 14.dp).fillMaxWidth(), Icons.AutoMirrored.Rounded.OpenInNew, haupt = false) {
            runCatching { activity.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)) }
        }
    }

    // ---------------------------------------------------------------- Die Meldungen
    Meldungsarten.gruppen.forEach { gruppe ->
        Abschnitt(gruppe, Modifier.padding(top = 8.dp))
        profile.filter { it.art.gruppe == gruppe }.forEach { p -> MeldungsKarte(vm, p, activity) }
    }
}

/** Ist die Meldung gerade an? Bei den Daueranzeigen zählt der Dienst dahinter. */
private fun istAn(vm: AppViewModel, p: MeldungsProfil): Boolean = when (p.art) {
    Meldungsarten.BEREITSCHAFT -> vm.einstellungen.dienstAn
    Meldungsarten.WECKWORT_HOERT -> vm.einstellungen.weckwortAn
    else -> !p.art.schaltbar || p.an
}

private fun symbol(art: Meldungsart): ImageVector = when (art) {
    Meldungsarten.AGENT -> Icons.Rounded.SmartToy
    Meldungsarten.PROGRAMMIEREN -> Icons.Rounded.Code
    Meldungsarten.ABLAGE -> Icons.Rounded.Folder
    Meldungsarten.GEMINI -> Icons.Rounded.Key
    Meldungsarten.ABFAHRT -> Icons.Rounded.DirectionsCar
    Meldungsarten.TIMER -> Icons.Rounded.Timer
    Meldungsarten.STOPPUHR -> Icons.Rounded.AvTimer
    Meldungsarten.ANSAGE -> Icons.Rounded.Campaign
    Meldungsarten.WECKWORT_HOERT -> Icons.Rounded.Hearing
    Meldungsarten.RUF -> Icons.Rounded.TouchApp
    Meldungsarten.WECKWORT_AUS -> Icons.Rounded.MicOff
    Meldungsarten.BEREITSCHAFT -> Icons.Rounded.Sensors
    Meldungsarten.AUFNAHME -> Icons.Rounded.Mic
    else -> Icons.AutoMirrored.Rounded.VolumeUp
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeldungsKarte(vm: AppViewModel, p: MeldungsProfil, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    val art = p.art
    val app = activity.applicationContext
    var offen by rememberSaveable(art.id) { mutableStateOf(false) }
    val drehung by animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")
    val an = istAn(vm, p)
    val farbe = if (an) f.primaer else f.textSchwach

    Column(Modifier.fillMaxWidth().glas(f, toenung = if (an) f.primaer else null)) {
        Row(Modifier.fillMaxWidth().antippen { offen = !offen }.padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Abzeichen(symbol(art), farbe)
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(art.titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(art.wozu, color = f.textLeise, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    merkmale(vm, p, an).forEach { (symbolKlein, text) -> Merkmal(symbolKlein, text) }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (art.schaltbar) Schalter(an) { p.an = it; nachSchalten(vm, art) }
                else Text(if (art.pflicht) "Pflicht" else "Immer", Modifier.glas(f, 999.dp, erhoeht = 0.2f).padding(horizontal = 10.dp, vertical = 5.dp), color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Zuklappen" else "Aufklappen", Modifier.padding(top = 8.dp).size(24.dp).graphicsLayer { rotationZ = drehung }, tint = f.textLeise)
            }
        }
        AnimatedVisibility(offen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) {
                Trenner(oben = 0)
                Text("Wann: ${art.wann}", color = f.text, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 10.dp))
                if (art.pflicht) Text(pflichtText(art), color = f.textSchwach, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))

                when (art) {
                    Meldungsarten.BEREITSCHAFT -> {
                        Unterzeile("Bereitschaft im Hintergrund")
                        AnAus(e.dienstAn) { vm.dienstSchalten(it) }
                    }
                    Meldungsarten.WECKWORT_HOERT -> {
                        Unterzeile("Auf das Weckwort hören")
                        AnAus(e.weckwortAn) { vm.weckwortSchalten(it, activity) }
                    }
                    Meldungsarten.RUF -> Text("Aus heißt: Kann Jarvis nach dem Weckwort nicht von selbst nach vorn kommen (gesperrtes Handy, Android blockiert den Start" +
                        (if (!vm.ueberlagernErlaubt) ", „Über anderen Apps einblenden“ fehlt gerade" else "") + "), passiert gar nichts.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    else -> {}
                }
                if (art.minimierbar) {
                    Unterzeile("Darstellung")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Fest", aktiv = !p.minimiert) { p.minimiert = false; Meldungen.folgen(app, art) }
                        Chip("Wegwischbar", aktiv = p.minimiert, icon = Icons.Rounded.UnfoldLess) { p.minimiert = true; Meldungen.folgen(app, art) }
                    }
                    Text("Wegwischbar: Du kannst die Anzeige wegwischen, Jarvis läuft trotzdem weiter. Mit einem Neustart von Jarvis ist sie wieder da. Ganz kleiner geht nicht: Android hebt Daueranzeigen laufender Dienste immer an.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (art.ton) {
                    Unterzeile("Ton")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Mit Ton", aktiv = p.ton, icon = Icons.Rounded.NotificationsActive) { p.ton = true }
                        Chip("Lautlos", aktiv = !p.ton, icon = Icons.Rounded.NotificationsOff) { p.ton = false }
                    }
                }
                if (art.wecker) {
                    Unterzeile("Wie ein Wecker")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Normal", aktiv = !p.wecker) { p.wecker = false }
                        Chip("Wie ein Wecker", aktiv = p.wecker, icon = Icons.Rounded.Alarm) { p.wecker = true }
                    }
                    Text(when {
                        !p.wecker -> "Ein kurzer Hinweiston, wie bei anderen Apps."
                        !p.weckerWirksam -> "Gerade ohne Wirkung: Die Meldung ist auf Lautlos gestellt, dann klingelt auch kein Wecker."
                        else -> "Klingelt mit dem gewählten Weckton in der Wecker-Lautstärke, bis die Dauer um ist oder du die Meldung antippst oder wegwischst; bei „Nicht stören“ nur, wenn dort Wecker erlaubt sind."
                    }, color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    if (p.wecker) WecktonOptionen(p, app)
                }
                if (art == Meldungsarten.ABFAHRT) {
                    Unterzeile("Meldung vor der Abfahrt")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20, 30).forEach { minuten -> Chip("$minuten min", aktiv = e.abfahrtVorlauf == minuten) { e.abfahrtVorlauf = minuten; vm.abfahrtGeaendert() } }
                    }
                    Text("Ankunftszeiten, Adressen und Standort stehen unter Alltag und Timer → Pünktlich losfahren.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (art == Meldungsarten.TIMER) TimerOptionen(vm)
                if (art.vorlesen) {
                    Unterzeile(if (art == Meldungsarten.AGENT) "Vorlesen (kurze Zusammenfassung)" else "Vorlesen")
                    AnAus(p.vorlesen) { p.vorlesen = it }
                    if (p.vorlesen && art.eigenerTextStandard != null) VorleseText(p)
                    if (p.vorlesen) StimmOptionen(vm, p)
                }
                if (art.sprichtImmer && p.an) StimmOptionen(vm, p)
                if (art.ruhezeit && e.meldungRuheAn) Text("Hält sich an die Ruhezeit von ${e.meldungRuheVon} bis ${e.meldungRuheBis} Uhr.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))

                val probe = !art.pflicht && art != Meldungsarten.STOPPUHR
                val kanal = kanalVon(p)
                if (probe || kanal != null) Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (probe) Knopf(if (art == Meldungsarten.TIMER) "Probe in 5 s" else "Probe", Modifier.weight(1f), Icons.Rounded.PlayArrow) { Meldungen.probe(app, art) }
                    if (kanal != null) Knopf("In Android", Modifier.weight(1f), Icons.AutoMirrored.Rounded.OpenInNew, haupt = false) { androidKanalOeffnen(activity, kanal) }
                }
            }
        }
    }
}

/** Kurze Stichworte unter dem Titel: was die Meldung gerade tut. */
private fun merkmale(vm: AppViewModel, p: MeldungsProfil, an: Boolean): List<Pair<ImageVector, String>> {
    val art = p.art
    val e = vm.einstellungen
    if (!an) return listOf(Icons.Rounded.NotificationsOff to "Aus")
    return buildList {
        if (art.minimierbar && p.minimiert) add(Icons.Rounded.UnfoldLess to "Wegwischbar")
        if (art.ton) add(if (p.ton) Icons.Rounded.NotificationsActive to "Mit Ton" else Icons.Rounded.NotificationsOff to "Lautlos")
        if (p.weckerWirksam || (art == Meldungsarten.TIMER && e.timerWecker)) add(Icons.Rounded.Alarm to "Wecker")
        if ((art.vorlesen && p.vorlesen) || art.sprichtImmer) add(Icons.Rounded.RecordVoiceOver to if (p.lautstaerke == 0) "Vorlesen" else "Vorlesen ${p.lautstaerke} %")
        if (art.ruhezeit && e.meldungRuheAn) add(Icons.Rounded.Bedtime to "Ruhezeit")
        if (isEmpty()) add(Icons.Rounded.Notifications to if (art.pflicht) "Android verlangt sie" else "An")
    }
}

private fun pflichtText(art: Meldungsart) = when (art) {
    Meldungsarten.BEREITSCHAFT -> "Android lässt Jarvis nur mit dieser Anzeige im Hintergrund laufen. Ganz weg ist sie nur mit ausgeschalteter Bereitschaft; dann ist Jarvis für ChatGPT nicht erreichbar."
    Meldungsarten.WECKWORT_HOERT -> "Android erlaubt das Mikrofon im Hintergrund nur mit dieser Anzeige. Ganz weg ist sie nur mit ausgeschaltetem Weckwort."
    Meldungsarten.AUFNAHME -> "Ohne diese Anzeige liefert Android einer App im Hintergrund nur Stille. Sie verschwindet mit dem Ende der Aufnahme."
    else -> "Android verlangt sie für die Wiedergabe im Hintergrund. Sie verschwindet, sobald Jarvis fertig vorgelesen hat."
}

/** Der Android-Kanal, den „In Android“ öffnet; als Wecker der Wecker-Kanal. */
private fun kanalVon(p: MeldungsProfil): String? = when {
    p.weckerWirksam -> Meldungen.KANAL_WECKER
    else -> p.art.kanal
}

/** Öffnet die Android-Einstellungen des Kanals; gibt es ihn noch nicht (nie gemeldet), die der ganzen App. */
private fun androidKanalOeffnen(activity: ComponentActivity, kanal: String) {
    de.frank.jarvis.dienst.JarvisDienst.kanalAnlegen(activity)
    de.frank.jarvis.weckwort.WeckwortDienst.kanaeleAnlegen(activity)
    de.frank.jarvis.timer.Timer.kanalAnlegen(activity.applicationContext)
    if (kanal == Meldungen.KANAL_WECKER) Meldungen.weckerKanalAnlegen(activity)
    val gibt = activity.getSystemService(NotificationManager::class.java).getNotificationChannel(kanal) != null
    val absicht = if (gibt) Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, kanal)
        else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
    runCatching { activity.startActivity(absicht) }
}

/** Folgeaktionen wie im Einstellungs-Katalog (Meldungen.folgen); bei Losfahren danach den Stand neu laden. */
private fun nachSchalten(vm: AppViewModel, art: Meldungsart) {
    Meldungen.folgen(vm.getApplication(), art)
    if (art == Meldungsarten.ABFAHRT) vm.abfahrtLaden()
}

/** Weckton (Android-Tonwähler, mit Anhören) und wie lange er klingelt, bevor Jarvis vorliest. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WecktonOptionen(p: MeldungsProfil, app: android.content.Context) {
    val f = LocalFarben.current
    var ton by remember(p.art.id) { mutableStateOf(p.weckton) }
    var sekunden by remember(p.art.id) { mutableIntStateOf(p.wecktonSekunden) }
    // Den Namen holt Android aus der Mediendatenbank: abseits des Hauptfadens, damit der Bildschirm nicht hakt.
    val name by produceState(if (ton.isBlank()) "Standard-Weckton" else "…", ton) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { de.frank.jarvis.meldung.WeckerTon.name(app, ton) }
    }
    // Den Bildschirm verlassen beendet eine laufende Probe.
    DisposableEffect(Unit) { onDispose { de.frank.jarvis.meldung.WeckerTon.probeStopp() } }
    val waehler = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
        @Suppress("DEPRECATION")
        val gewaehlt = r.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        val neu = gewaehlt?.toString().orEmpty()
        // Eigene Musik aus dem Speicher darf Jarvis oft nicht öffnen: dann nicht übernehmen, statt still den Standard zu spielen.
        if (!de.frank.jarvis.meldung.WeckerTon.abspielbar(app, neu)) {
            android.widget.Toast.makeText(app, "Diesen Ton darf Jarvis nicht abspielen. Bitte einen Weckton aus der Liste des Handys wählen.", android.widget.Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        ton = neu
        p.weckton = ton
    }
    Unterzeile("Weckton")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Knopf(name, Modifier.weight(1f), Icons.Rounded.Alarm, haupt = false) {
            val absicht = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Weckton für ${p.art.titel}")
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (ton.isBlank()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else android.net.Uri.parse(ton))
            runCatching { waehler.launch(absicht) }
        }
        Knopf("Anhören", Modifier.weight(1f), Icons.Rounded.PlayArrow, haupt = false) { de.frank.jarvis.meldung.WeckerTon.probe(app, ton) }
    }
    Unterzeile("So lange klingelt der Weckton")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(5, 10, 15, 20, 30, 45, 60, 0).forEach { s ->
            Chip(if (s == 0) "Bis Antippen" else "$s s", aktiv = sekunden == s) { sekunden = s; p.wecktonSekunden = s }
        }
    }
    Text(when {
        sekunden == 0 -> "Klingelt, bis du die Meldung antippst oder wegwischst (höchstens 10 Minuten); erst danach liest Jarvis vor."
        p.art.vorlesen && p.vorlesen -> "Klingelt $sekunden Sekunden, dann verstummt der Weckton, die Meldung bleibt stehen und Jarvis liest sofort vor. Antippen oder Wegwischen beendet den Ton früher."
        else -> "Klingelt $sekunden Sekunden, dann verstummt der Weckton; die Meldung bleibt stehen."
    }, color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}

/** Der selbst geschriebene Vorlesetext; leer = der Standardsatz. Gespeichert wird bei jeder Änderung. */
@Composable
private fun VorleseText(p: MeldungsProfil) {
    val f = LocalFarben.current
    // Neu gelesen, wenn Jarvis den Text per Sprache ändert (anders als das, was gerade im Feld steht).
    val gespeichert = p.vorlesetext
    var text by remember(p.art.id) { mutableStateOf(gespeichert) }
    if (gespeichert != text.trim()) text = gespeichert
    Unterzeile("Text, den Jarvis vorliest")
    Eingabe(text, { text = it; p.vorlesetext = it.trim() }, p.art.eigenerTextStandard.orEmpty(), Modifier.fillMaxWidth(), einzeilig = false)
    Text("Leer = „${p.art.eigenerTextStandard}“. „{name}“ setzt den Namen ein.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}

/** Stimme, Tempo und Lautstärke einer Meldung; „Wie Jarvis“ übernimmt die Einstellungen unter Stimme → Vorlesen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StimmOptionen(vm: AppViewModel, p: MeldungsProfil) {
    val f = LocalFarben.current
    val s = vm.sprache
    // Die Sprach-Schlüssel melden Änderungen nicht selbst an die Oberfläche: nach jedem Speichern neu zeichnen.
    var stand by remember { mutableIntStateOf(0) }
    val googleFehlt = remember(stand) { s.googleTtsApiKey.isBlank() }
    val qwenFehlt = remember(stand) { s.qwenTtsApiKey.isBlank() }
    val geminiFehlt = remember(stand) { s.geminiTtsKeyFrei.isBlank() && s.geminiTtsKeyBezahlt.isBlank() }
    var anbieter by remember(p.art.id) { mutableStateOf(TtsCatalog.anbieterZu(p.stimme)) }
    Unterzeile("Stimme")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Wie Jarvis", aktiv = anbieter == null) { anbieter = null; p.stimme = "" }
        listOf(
            TtsProvider.EDGE to TtsCatalog.DEFAULT_EDGE_VOICE, TtsProvider.GOOGLE_CLOUD to TtsCatalog.DEFAULT_GOOGLE_VOICE,
            TtsProvider.GEMINI_FLASH to TtsCatalog.DEFAULT_GEMINI_FLASH_VOICE, TtsProvider.GEMINI_LITE to TtsCatalog.DEFAULT_GEMINI_LITE_VOICE,
            TtsProvider.QWEN to TtsCatalog.DEFAULT_QWEN_VOICE,
        ).forEach { (anb, standard) ->
            Chip(anb.label, aktiv = anbieter == anb) {
                anbieter = anb
                if (TtsCatalog.anbieterZu(p.stimme) != anb) p.stimme = standard
            }
        }
    }
    val stimmen = when (anbieter) {
        TtsProvider.EDGE -> TtsCatalog.edgeVoices
        TtsProvider.GOOGLE_CLOUD -> TtsCatalog.googleVoices
        TtsProvider.GEMINI_FLASH -> TtsCatalog.geminiFlashVoices
        TtsProvider.GEMINI_LITE -> TtsCatalog.geminiLiteVoices
        TtsProvider.QWEN -> TtsCatalog.qwenVoices
        else -> emptyList()
    }
    if (stimmen.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        StimmChips(vm, anbieter!!, stimmen, p.stimme) { p.stimme = it }
    }
    if (anbieter == TtsProvider.GOOGLE_CLOUD && googleFehlt) Schluesselfeld(vm, "Google-Cloud-Schlüssel", s.googleTtsApiKey) { s.googleTtsApiKey = it; stand++ }
    if (anbieter == TtsProvider.QWEN && qwenFehlt) Schluesselfeld(vm, "Alibaba-Schlüssel (Qwen)", s.qwenTtsApiKey) { s.qwenTtsApiKey = it; stand++ }
    if ((anbieter == TtsProvider.GEMINI_FLASH || anbieter == TtsProvider.GEMINI_LITE) && geminiFehlt) Schluesselfeld(vm, "Gemini-Stimme: Schlüssel kostenlos", s.geminiTtsKeyFrei) { s.geminiTtsKeyFrei = it; stand++ }
    Unterzeile("Sprechtempo")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0f to "Wie Jarvis", 0.9f to "Ruhig", 1.0f to "Normal", 1.1f to "Zügig", 1.2f to "Schnell").forEach { (wert, name) ->
            Chip(name, aktiv = kotlin.math.abs(p.tempo - wert) < 0.04f) { p.tempo = wert }
        }
    }
    Unterzeile("Lautstärke beim Vorlesen")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 20, 40, 60, 80, 100).forEach { prozent ->
            Chip(when (prozent) { 0 -> "Wie Handy"; 100 -> "Maximal"; else -> "$prozent %" }, aktiv = p.lautstaerke == prozent) { p.lautstaerke = prozent }
        }
    }
    Text(if (p.lautstaerke == 0) "Jarvis liest so laut vor, wie die Medienlautstärke gerade steht."
        else "Jarvis stellt die Medienlautstärke nur dafür ein und danach wieder zurück. Ist ein Kopfhörer oder das Auto per Bluetooth verbunden, kommt die Stimme dort.",
        color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun AnAus(an: Boolean, setze: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("An", aktiv = an) { setze(true) }
        Chip("Aus", aktiv = !an) { setze(false) }
    }
}

/** Rundes Glas-Abzeichen mit Symbol, getönt in [farbe]. */
@Composable
private fun Abzeichen(symbol: ImageVector, farbe: Color, groesse: Int = 44) {
    val f = LocalFarben.current
    Box(Modifier.size(groesse.dp).glas(f, (groesse * 0.32f).dp, erhoeht = 0.5f, toenung = farbe), contentAlignment = Alignment.Center) {
        Icon(symbol, null, tint = farbe, modifier = Modifier.size((groesse * 0.5f).dp))
    }
}

@Composable
private fun Kennzahl(wert: String, text: String, symbol: ImageVector, modifier: Modifier) {
    val f = LocalFarben.current
    Column(modifier.glas(f, 16.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(symbol, null, tint = f.primaer, modifier = Modifier.size(18.dp))
        Text(wert, color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        Text(text, color = f.textLeise, fontSize = 12.sp)
    }
}

@Composable
private fun Merkmal(symbol: ImageVector, text: String) {
    val f = LocalFarben.current
    Row(Modifier.glas(f, 999.dp, erhoeht = 0.15f).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(symbol, null, tint = f.textLeise, modifier = Modifier.size(13.dp))
        Text(text, color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun SchalterZeile(symbol: ImageVector, titel: String, wert: String, an: Boolean, setze: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Abzeichen(symbol, if (an) f.primaer else f.textSchwach, groesse = 40)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(wert, color = if (an) f.primaer else f.textLeise, fontSize = 13.sp)
        }
        Schalter(an, setze)
    }
}

@Composable
private fun Schalter(an: Boolean, setze: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Switch(
        an, setze,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = f.primaer, checkedBorderColor = f.primaer,
            uncheckedThumbColor = f.textLeise, uncheckedTrackColor = f.flaeche, uncheckedBorderColor = f.rand,
        ),
    )
}

@Composable
private fun Trenner(oben: Int = 16) {
    val f = LocalFarben.current
    Box(Modifier.padding(top = oben.dp, bottom = 12.dp).fillMaxWidth().height(1.dp).glas(f, 1.dp, erhoeht = 0f, fuellung = f.rand, rand = false))
}

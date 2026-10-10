package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.fahrt.Routen
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben

/**
 * Einstellungen zu „Pünktlich losfahren“: oben der Stand und die Schalter, die täglich zählen, darunter die
 * Ankunftszeiten; Adressen und die freie Abfrage sind eingeklappt, weil sie sich selten ändern.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AbfahrtEinstellungen(vm: AppViewModel) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    LaunchedEffect(Unit) { vm.abfahrtLaden() }
    Abschnitt("Pünktlich losfahren")
    Karte {
        val bereit = e.abfahrtAn && e.mapsSchluessel.isNotBlank()
        Zeile("Losfahr-Meldung", if (!e.abfahrtAn) "Aus" else if (e.mapsSchluessel.isBlank()) "Schlüssel fehlt" else "An", if (bereit) f.erfolg else if (e.abfahrtAn) f.gefahr else f.textLeise)
        Text(vm.abfahrtStatus.ifEmpty { "Stand wird geladen …" }, color = f.textLeise, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("An", aktiv = e.abfahrtAn) { e.abfahrtAn = true; vm.abfahrtGeaendert() }
            Chip("Aus", aktiv = !e.abfahrtAn) { e.abfahrtAn = false; vm.abfahrtGeaendert() }
        }
        Text("An Arbeitstagen laut Kalender (Tag 1–4, Nacht 1–4, ohne X, F und U) fragt Jarvis bei Google Maps die Fahrzeit mit Verkehr ab und meldet sich rechtzeitig vor der Abfahrt.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

        Knopf(if (vm.fahrzeitLaeuft) "Frage Google Maps …" else "Fahrzeit zur Arbeit jetzt prüfen", Modifier.padding(top = 14.dp).fillMaxWidth()) { vm.fahrzeitPruefen("arbeit") }
        vm.fahrzeitText?.let { Text(it, color = f.text, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 10.dp)) }

        Unterzeile("Ankunft auf Arbeit")
        var waehle by rememberSaveable { mutableStateOf("") }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Knopf("Nachtdienst ${e.ankunftNacht}", Modifier.weight(1f), haupt = false) { waehle = "nacht" }
            Knopf("Tagdienst ${e.ankunftTag}", Modifier.weight(1f), haupt = false) { waehle = "tag" }
        }
        if (waehle.isNotEmpty()) {
            val nacht = waehle == "nacht"
            Uhrzeitwahl(if (nacht) "Ankunft vor dem Nachtdienst" else "Ankunft vor dem Tagdienst", if (nacht) e.ankunftNacht else e.ankunftTag, { waehle = "" }) { zeit ->
                if (nacht) e.ankunftNacht = zeit else e.ankunftTag = zeit
                waehle = ""
                vm.abfahrtGeaendert()
            }
        }

        Unterzeile("Meldung vor der Abfahrt")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 20, 30).forEach { minuten -> Chip("$minuten min", aktiv = e.abfahrtVorlauf == minuten) { e.abfahrtVorlauf = minuten; vm.abfahrtGeaendert() } }
        }

        Unterzeile("Meldung vorlesen")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("An", aktiv = e.abfahrtVorlesen) { e.abfahrtVorlesen = true }
            Chip("Aus", aktiv = !e.abfahrtVorlesen) { e.abfahrtVorlesen = false }
        }
        Text("Mit der Stimme, die unter Sprache eingerichtet ist. Auch bei gesperrtem Handy.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))

        if (e.abfahrtVorlesen) {
            Unterzeile("Lautstärke der Meldung")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(40, 60, 80, 100).forEach { prozent -> Chip(if (prozent == 100) "Maximal" else "$prozent %", aktiv = e.abfahrtLautstaerke == prozent) { e.abfahrtLautstaerke = prozent } }
            }
            Knopf("Probe hören", Modifier.padding(top = 10.dp).fillMaxWidth(), haupt = false) { vm.abfahrtProbe() }
            Text("Jarvis stellt die Medienlautstärke nur für diese Meldung so ein und danach wieder zurück. Ist ein Kopfhörer oder das Auto per Bluetooth verbunden, kommt die Stimme dort.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Unterzeile("Start der Fahrt")
        Zeile("Aktueller Standort", if (vm.standortImmer) "Immer erlaubt" else if (vm.standortErlaubt) "Nur bei offener App" else "Nicht erlaubt", if (vm.standortImmer) f.erfolg else f.gefahr)
        if (!vm.standortImmer) {
            Knopf(if (vm.standortErlaubt) "„Immer zulassen“ wählen" else "Standort erlauben", Modifier.padding(top = 10.dp).fillMaxWidth(), haupt = false) { vm.standortAnfragen() }
            Text("Ohne „Immer zulassen“ kennt Jarvis den Standort im Hintergrund nicht und rechnet ab Zuhause.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        var adressenOffen by rememberSaveable { mutableStateOf(false) }
        Knopf(if (adressenOffen) "Adressen ausblenden" else "Adressen ändern", Modifier.padding(top = 16.dp).fillMaxWidth(), haupt = false) { adressenOffen = !adressenOffen }
        if (adressenOffen) {
            var zuhause by rememberSaveable { mutableStateOf(e.adresseZuhause) }
            var arbeit by rememberSaveable { mutableStateOf(e.adresseArbeit) }
            Unterzeile("Zuhause")
            Eingabe(zuhause, { zuhause = it }, "Straße Hausnummer, PLZ Ort", Modifier.fillMaxWidth())
            Unterzeile("Arbeit")
            Eingabe(arbeit, { arbeit = it }, "Straße Hausnummer, PLZ Ort", Modifier.fillMaxWidth())
            if (zuhause.trim() != e.adresseZuhause || arbeit.trim() != e.adresseArbeit) {
                Knopf("Adressen speichern", Modifier.padding(top = 10.dp).fillMaxWidth()) {
                    if (zuhause.isNotBlank()) e.adresseZuhause = zuhause
                    if (arbeit.isNotBlank()) e.adresseArbeit = arbeit
                    vm.abfahrtGeaendert()
                    vm.meldung = "Adressen gespeichert."
                }
            }
        }

        var abfrageOffen by rememberSaveable { mutableStateOf(false) }
        Knopf(if (abfrageOffen) "Abfrage ausblenden" else "Fahrzeit zu einer anderen Adresse", Modifier.padding(top = 10.dp).fillMaxWidth(), haupt = false) { abfrageOffen = !abfrageOffen }
        if (abfrageOffen) {
            var ziel by rememberSaveable { mutableStateOf("") }
            Unterzeile("Ziel")
            Eingabe(ziel, { ziel = it }, "Adresse oder Ort, z. B. Alexanderplatz", Modifier.fillMaxWidth(), senden = { vm.fahrzeitPruefen(ziel) })
            Knopf(if (vm.fahrzeitLaeuft) "Frage Google Maps …" else "Fahrzeit prüfen", Modifier.padding(top = 10.dp).fillMaxWidth()) { vm.fahrzeitPruefen(ziel) }
            Text("Du kannst Jarvis auch einfach fragen: „Wie lange brauche ich zum Alexanderplatz?“", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        if (e.mapsSchluessel.isBlank()) Schluesselfeld(vm, "Google-Maps-Schlüssel (Routes API)", e.mapsSchluessel) { e.mapsSchluessel = it; vm.abfahrtGeaendert(); vm.lagePruefen() }
        Text("Google-Maps-Abfragen diesen Monat: ${Routen.verbraucht(vm.getApplication())} von ${Routen.FREI_IM_MONAT} kostenlosen. Jarvis hört vorher auf, es entstehen keine Kosten.", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

/** Uhrzeit über die Zifferntasten wählen (24 Stunden); [uebernehmen] bekommt HH:MM. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Uhrzeitwahl(titel: String, wert: String, abbrechen: () -> Unit, uebernehmen: (String) -> Unit) {
    val teile = wert.split(":")
    val stand = rememberTimePickerState(initialHour = teile.getOrNull(0)?.toIntOrNull() ?: 17, initialMinute = teile.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = abbrechen,
        title = { Text(titel) },
        text = { TimeInput(stand) },
        confirmButton = { TextButton(onClick = { uebernehmen("%02d:%02d".format(stand.hour, stand.minute)) }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = abbrechen) { Text("Abbrechen") } },
    )
}

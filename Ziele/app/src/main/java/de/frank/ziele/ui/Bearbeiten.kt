package de.frank.ziele.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.data.ZielStatus
import de.frank.ziele.ui.theme.Chip
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.antippen
import de.frank.ziele.ui.theme.glas
import de.frank.ziele.ui.theme.knopf3d
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SYMBOLE = listOf("🎯", "🏃", "💪", "🧘", "🥗", "😴", "📚", "🧠", "💶", "🏠", "❤️", "👨‍👩‍👧", "🎸", "✈️", "🌱", "💼", "🏆", "⛰️")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BearbeitenBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val neu = vm.eId == null
    var datumWahl by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text(if (neu) "Neues Ziel" else "Ziel bearbeiten", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).weight(1f))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Einsprechen ist der schnellste Weg: frei reden, die KI ordnet in Titel, Beschreibung, Warum und Frist.
            Row(Modifier.fillMaxWidth().glas(f, erhoeht = 1.2f, toenung = f.primaer).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MikroKnopf(vm.aufnahme, 60.dp) { vm.mikroTippen(DiktatZiel.ZIEL) }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    when {
                        vm.aufnahme == Aufnahme.LAEUFT -> { Text("Ich höre zu …", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold); Aufnahmezeit(vm.aufnahmeStart) }
                        vm.aufnahme == Aufnahme.VERARBEITET -> Text("Verstehe …", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        vm.ordnet -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Text("Ordne dein Ziel …", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }
                        else -> {
                            Text(if (neu) "Sprich dein Ziel ein" else "Ergänzen per Sprache", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Was willst du erreichen, bis wann und warum? Die KI füllt die Felder für dich.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            Eingabe("Ziel", vm.eTitel, "z. B. Halbmarathon unter 2 Stunden laufen", einzeilig = true) { vm.eTitel = it }
            Eingabe("Genauer (möglichst messbar)", vm.eBeschreibung, "Was genau soll am Ende anders sein?") { vm.eBeschreibung = it }
            Eingabe("Warum ist dir das wichtig?", vm.eWarum, "Dein Antrieb – hilft dir und schärft die Vorschläge") { vm.eWarum = it }

            Block("Bis wann?") {
                val heute = LocalDate.now()
                ChipReihe {
                    Chip("Ohne Frist", vm.eFrist == null) { vm.eFrist = null }
                    listOf("1 Monat" to heute.plusMonths(1), "3 Monate" to heute.plusMonths(3), "6 Monate" to heute.plusMonths(6), "1 Jahr" to heute.plusYears(1)).forEach { (name, tag) ->
                        Chip(name, vm.eFrist == tag.toEpochDay()) { vm.eFrist = tag.toEpochDay() }
                    }
                    Chip("Datum …", false, icon = Icons.Rounded.CalendarMonth) { datumWahl = true }
                }
                vm.eFrist?.let { Text("Zieldatum: " + LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)), color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }

            Block("Symbol") {
                ChipReihe {
                    SYMBOLE.forEach { s ->
                        Box(
                            Modifier.size(44.dp).then(if (vm.eSymbol == s) Modifier.knopf3d(f.primaer, f.tertiaer, 14.dp, f.dunkel) else Modifier.glas(f, 14.dp, 0.4f)).antippen { vm.eSymbol = s },
                            contentAlignment = Alignment.Center,
                        ) { Text(s, fontSize = 22.sp) }
                    }
                }
            }

            if (!neu) Block("Stand") {
                ChipReihe { ZielStatus.entries.forEach { s -> Chip(s.emoji + " " + s.anzeige, vm.eStatus == s) { vm.eStatus = s } } }
            }
            if (neu) Row(Modifier.fillMaxWidth().glas(f, 16.dp, 0.5f).antippen(haptik = false) { vm.eAuswerten = !vm.eAuswerten }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Gleich auswerten", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Nach dem Speichern recherchiert die KI den besten Weg und schlägt Aufgaben vor.", color = f.textLeise, fontSize = 12.sp)
                }
                Switch(vm.eAuswerten, { vm.eAuswerten = it }, colors = SwitchDefaults.colors(checkedTrackColor = f.primaer))
            }
            Spacer(Modifier.height(4.dp))
        }
        Row(
            Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth().height(56.dp).knopf3d(f.primaer, f.tertiaer, 18.dp, f.dunkel).antippen { vm.speichern() },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = f.aufPrimaer)
            Spacer(Modifier.width(8.dp))
            Text(if (neu && vm.eAuswerten) "Speichern und auswerten" else "Speichern", color = f.aufPrimaer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
    if (datumWahl) {
        val zustand = rememberDatePickerState(
            initialSelectedDateMillis = (vm.eFrist ?: LocalDate.now().plusMonths(3).toEpochDay()) * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { datumWahl = false },
            confirmButton = {
                TextButton(onClick = {
                    zustand.selectedDateMillis?.let { ms -> vm.eFrist = java.time.Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    datumWahl = false
                }) { Text("Übernehmen", color = f.primaer) }
            },
            dismissButton = { TextButton(onClick = { datumWahl = false }) { Text("Abbrechen", color = f.textLeise) } },
        ) { DatePicker(zustand) }
    }
}

@Composable
fun Eingabe(titel: String, wert: String, platzhalter: String, einzeilig: Boolean = false, aendern: (String) -> Unit) {
    val f = LocalFarben.current
    Column {
        Text(titel, color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Box(Modifier.fillMaxWidth().glas(f, 16.dp, 0.6f, f.flaecheStark).padding(horizontal = 14.dp, vertical = 14.dp)) {
            if (wert.isEmpty()) Text(platzhalter, color = f.textSchwach, fontSize = if (einzeilig) 18.sp else 15.sp)
            BasicTextField(
                wert, aendern, singleLine = einzeilig,
                textStyle = TextStyle(color = f.text, fontSize = if (einzeilig) 18.sp else 15.sp, fontWeight = if (einzeilig) FontWeight.SemiBold else FontWeight.Normal, lineHeight = if (einzeilig) 24.sp else 21.sp),
                cursorBrush = SolidColor(f.primaer),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

package de.frank.ziele.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.data.Kategorie
import de.frank.ziele.ui.theme.Chip
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.antippen
import de.frank.ziele.ui.theme.glas
import de.frank.ziele.ui.theme.knopf3d

/**
 * Bildschirmfüllender Editor für eine eigene Aufgabe: leere Schablone mit allen Feldern, Diktat in die Beschreibung,
 * KI füllt aus dem Text alle Felder aus (danach Rückgängig), Speichern hängt sie an das Ziel.
 */
@Composable
fun EigeneAufgabeBildschirm(vm: AppViewModel, zielId: Long) {
    val f = LocalFarben.current
    val ziele by vm.ziele.collectAsState()
    val ziel = ziele.firstOrNull { it.id == zielId }
    val diktiertHier = vm.diktatZiel == DiktatZiel.AUFGABE
    val aufnahme = if (diktiertHier) vm.aufnahme else Aufnahme.AUS
    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EigenPunkt(12.dp)
                    Text("Eigene Aufgabe", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                }
                ziel?.let { Text("${it.symbol} ${it.titel}", color = f.textLeise, fontSize = 13.sp, maxLines = 1) }
            }
            ModusKnopf(vm)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().glas(f, erhoeht = 1.2f, toenung = f.primaer).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MikroKnopf(aufnahme, 60.dp) { vm.mikroTippen(DiktatZiel.AUFGABE) }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    when (aufnahme) {
                        Aufnahme.LAEUFT -> { Text("Ich höre zu …", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold); Aufnahmezeit(vm.aufnahmeStart) }
                        Aufnahme.VERARBEITET -> Text("Schreibe mit …", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Aufnahme.AUS -> {
                            Text("Sprich deine Aufgabe ein", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Was willst du tun, warum, und wann ist es erledigt? Danach füllt KI alle Felder aus.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            Eingabe("Überschrift", vm.aTitel, "Kurz: Was ist zu tun?", einzeilig = true) { vm.aufgabeKiVergessen(); vm.aTitel = it }
            Eingabe("Aufgabenbeschreibung", vm.aText, "Was genau machst du? Hier landet auch dein diktierter Text.") { vm.aufgabeKiVergessen(); vm.aText = it }
            Eingabe("Begründung / Nutzen", vm.aBegruendung, "Wie bringt dich das deinem Ziel näher?") { vm.aufgabeKiVergessen(); vm.aBegruendung = it }
            Eingabe("Erledigt, wenn …", vm.aErfolg, "Woran erkennst du, dass die Aufgabe fertig ist?") { vm.aufgabeKiVergessen(); vm.aErfolg = it }

            // Aus dem KI-Knopf wird nach dem Ausfüllen der Rückgängig-Knopf (wie in Geniale Aufgaben).
            val zurueck = vm.aKi != null
            Row(
                Modifier.fillMaxWidth()
                    .then(if (zurueck) Modifier.glas(f, 16.dp, 0.6f, f.flaecheStark) else Modifier.knopf3d(f.primaer, f.tertiaer, 16.dp, f.dunkel))
                    .antippen { if (zurueck) vm.aufgabeKiZuruecknehmen() else vm.aufgabeAusfuellen(zielId) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val farbe = if (zurueck) f.primaer else f.aufPrimaer
                if (vm.aKiLaeuft) CircularProgressIndicator(color = farbe, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else Icon(if (zurueck) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.AutoAwesome, null, tint = farbe, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        when { vm.aKiLaeuft -> "KI füllt aus …"; zurueck -> "KI rückgängig"; else -> "KI: alle Felder ausfüllen" },
                        color = if (zurueck) f.text else f.aufPrimaer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (zurueck) "Tippen holt deinen Text von vor der KI zurück." else "Macht aus deinem Text Überschrift, Beschreibung, Nutzen und Erledigt-Kriterium.",
                        color = if (zurueck) f.textLeise else f.aufPrimaer, fontSize = 12.sp,
                    )
                }
            }

            Block("Art") {
                ChipReihe { Kategorie.entries.forEach { k -> Chip(k.anzeige, vm.aKategorie == k) { vm.aKategorie = k } } }
            }
            Spacer(Modifier.height(4.dp))
        }
        Row(
            Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth().height(56.dp).knopf3d(f.primaer, f.tertiaer, 18.dp, f.dunkel).antippen { vm.eigeneAufgabeSpeichern(zielId) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = f.aufPrimaer)
            Spacer(Modifier.width(8.dp))
            Text("Speichern", color = f.aufPrimaer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.GoogleGuthabenFaehigkeit
import de.frank.jarvis.faehigkeit.KontingenteFaehigkeit
import de.frank.jarvis.faehigkeit.KontingenteFaehigkeit.Companion.prozent
import de.frank.jarvis.ui.theme.LocalFarben

/** Reiter „Kontingente“: je Anbieter die Limits mit Verbrauch, Zurücksetzen in deutscher Zeit und Tempo. */
@Composable
internal fun KontingenteReiter(vm: AppViewModel) {
    val f = LocalFarben.current
    LaunchedEffect(Unit) { vm.kontingenteAbfragen() }
    var anmelden by rememberSaveable { mutableStateOf<String?>(null) }
    Text("Wie viel deiner Abos verbraucht ist und wann es zurückgesetzt wird, alle Zeiten in deutscher Zeit. Jarvis behält das im Blick und meldet sich, wenn ein Limit vor dem Zurücksetzen leer wäre.",
        color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
    val stand = vm.kontingente
    if (stand == null || stand.anbieter.isEmpty()) {
        Text(if (vm.kontingenteLaedt) "Wird abgefragt …" else "Noch nicht abgefragt.", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.padding(4.dp))
    } else {
        val jetzt = System.currentTimeMillis() / 1000
        stand.anbieter.forEach { a ->
            Bereich(a.titel, startOffen = true) {
                if (a.konto.isNotEmpty()) Text(a.konto, color = f.textSchwach, fontSize = 13.sp)
                a.limits.forEach { l ->
                    val schnell = l.zuSchnell(jetzt)
                    val genutzt = l.genutztJetzt(jetzt).coerceIn(0.0, 100.0)
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text(l.name, color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${prozent(genutzt)} % verbraucht", color = if (schnell) f.gefahr else f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.padding(top = 6.dp, bottom = 6.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(f.flaecheStark)) {
                            Box(Modifier.fillMaxWidth((genutzt / 100.0).toFloat().coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (schnell) f.gefahr else f.primaer))
                        }
                        Text(l.resetSatz(jetzt), color = f.text, fontSize = 14.sp)
                        l.tempoText(jetzt).takeIf { it.isNotEmpty() }?.let {
                            Text(if (schnell) "Zu schnell: $it" else it, color = if (schnell) f.gefahr else f.textSchwach, fontSize = 13.sp,
                                fontWeight = if (schnell) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                a.hinweis?.let { Text(it, color = if (a.limits.isEmpty() && !a.anmeldenNoetig) f.gefahr else f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
                a.standText().takeIf { it.isNotEmpty() }?.let { Text(it, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
                if (a.anmeldenNoetig && a.id != "codex") {
                    Knopf(when (a.id) { "claude" -> "Bei claude.ai anmelden"; else -> "Bei Kimi anmelden" }, Modifier.padding(top = 10.dp).fillMaxWidth()) { anmelden = a.id }
                }
                if ((a.id == "claude" || a.id == "kimi") && (a.limits.isNotEmpty() || (a.anmeldenNoetig && a.hinweis?.contains("lehnt") == true))) {
                    Knopf("Abmelden", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { vm.kontingentAbmelden(a.id) }
                }
            }
        }
    }
    Knopf(if (vm.kontingenteLaedt) "Wird abgefragt …" else "Jetzt neu abfragen", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { vm.kontingenteAbfragen(erzwingen = true) }
    when (anmelden) {
        "claude" -> {
            remember { KontingenteFaehigkeit.claudeAnmeldungVorbereiten() }
            AnmeldeBrowser("Bei claude.ai anmelden", KontingenteFaehigkeit.CLAUDE_LOGIN, googleTauglich = true,
                fertig = { KontingenteFaehigkeit.claudeSitzungImBrowser() != null },
                onFertig = { KontingenteFaehigkeit.claudeAnmeldungBeendet(); anmelden = null; vm.kontingenteAbfragen(erzwingen = true) })
        }
        // Kimi nennt nach außen kein festes Zeichen für „angemeldet“: Du siehst deine Limits im Fenster und schließt es dann selbst.
        "kimi" -> AnmeldeBrowser("Bei Kimi anmelden, danach schließen", KontingenteFaehigkeit.KIMI_SEITE, googleTauglich = true,
            fertig = { false }, onFertig = { anmelden = null; vm.kontingenteAbfragen(erzwingen = true) })
    }
}

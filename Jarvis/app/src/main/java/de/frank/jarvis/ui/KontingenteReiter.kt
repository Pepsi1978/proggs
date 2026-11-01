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
import de.frank.jarvis.faehigkeit.AnthropicGuthabenFaehigkeit
import de.frank.jarvis.faehigkeit.GoogleGuthabenFaehigkeit
import de.frank.jarvis.faehigkeit.KontingenteFaehigkeit
import de.frank.jarvis.faehigkeit.KontingenteFaehigkeit.Companion.prozent
import de.frank.jarvis.ui.theme.LocalFarben

/** Reiter „Kontingente“: je Anbieter die Limits mit Verbrauch, Zurücksetzen in deutscher Zeit und Tempo. */
@Composable
internal fun KontingenteReiter(vm: AppViewModel) {
    val f = LocalFarben.current
    LaunchedEffect(Unit) { vm.kontingenteAbfragen(); vm.anthropicGuthabenAbfragen(); vm.googleGuthabenAbfragen() }
    var anmelden by rememberSaveable { mutableStateOf<String?>(null) }
    Knopf(if (vm.kontingenteLaedt || vm.anthropicGuthabenLaedt || vm.googleGuthabenLaedt) "Wird abgefragt …" else "Jetzt neu abfragen", Modifier.fillMaxWidth(), haupt = false) { vm.kontingenteAbfragen(erzwingen = true); vm.anthropicGuthabenAbfragen(erzwingen = true); vm.googleGuthabenAbfragen(erzwingen = true) }
    val stand = vm.kontingente
    if (stand == null || stand.anbieter.isEmpty()) {
        Text(if (vm.kontingenteLaedt) "Wird abgefragt …" else "Noch nicht abgefragt.", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.padding(4.dp))
        Bereich("Claude", startOffen = true) { ApiFreikontingent(vm) }
        Bereich("Gemini / Antigravity (Google)", startOffen = true) { AiStudioUebersicht(vm) }
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
                if (a.id == "claude") ApiFreikontingent(vm)
                if (a.anmeldenNoetig && (a.id == "claude" || a.id == "kimi")) {
                    Knopf(when (a.id) { "claude" -> "Bei claude.ai anmelden"; else -> "Bei Kimi anmelden" }, Modifier.padding(top = 10.dp).fillMaxWidth()) { anmelden = a.id }
                }
                if ((a.id == "claude" || a.id == "kimi") && (a.limits.isNotEmpty() || (a.anmeldenNoetig && a.hinweis?.contains("lehnt") == true))) {
                    Knopf("Abmelden", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { vm.kontingentAbmelden(a.id) }
                }
                if (a.id == "gemini") AiStudioUebersicht(vm)
            }
        }
    }
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

/** Anthropic-API-Freikontingent als fester Balken bis 100 $, aus dem Stand des Reiters „Guthaben“. Fehlendes steht als „nicht verfügbar“ da. */
@Composable
private fun ApiFreikontingent(vm: AppViewModel) {
    val f = LocalFarben.current
    val g = vm.anthropicGuthaben
    val verbraucht = g?.freiVerbrauchtUsd
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Anthropic-API-Freikontingent", color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            when {
                verbraucht != null -> "${AnthropicGuthabenFaehigkeit.betrag(verbraucht)} $ von 100 $ verbraucht"
                vm.anthropicGuthabenLaedt -> "Wird abgefragt …"
                else -> "Verbrauch nicht verfügbar"
            },
            color = if (verbraucht != null) f.text else f.textLeise, fontSize = if (verbraucht != null) 26.sp else 15.sp, fontWeight = if (verbraucht != null) FontWeight.Bold else FontWeight.Normal,
        )
        // Ohne Wert kein Balken: Ein leerer sähe wie „nichts verbraucht“ aus.
        if (verbraucht != null) Balken((verbraucht / 100.0).toFloat())
        Text(g?.naechstes?.let { "Zurücksetzen voraussichtlich " + bis(it) } ?: "Rücksetzzeitpunkt nicht verfügbar", color = f.text, fontSize = 14.sp)
        g?.hinweis?.let { Text(it, color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
        g?.standText()?.takeIf { it.isNotEmpty() }?.let { Text(it, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}

/** Gutschriften, Verbrauch und eingezahltes Geld aus AI Studio. Was nicht lesbar ist, steht als „Nicht verfügbar“ da, nie als geschätzte Zahl. */
@Composable
private fun AiStudioUebersicht(vm: AppViewModel) {
    val f = LocalFarben.current
    val g = vm.googleGuthaben
    val b = GoogleGuthabenFaehigkeit::betrag
    val rest = g?.restEur
    val fehlt = if (vm.googleGuthabenLaedt) "Wird abgefragt …" else "Nicht verfügbar"
    val gutschrift = { wert: (GoogleGuthabenFaehigkeit.Stand) -> String ->
        when {
            g == null || rest == null -> fehlt
            g.gutschriften.isEmpty() -> "Nicht verfügbar: keine laufende Gutschrift gefunden."
            else -> wert(g) + if (g.abfrageGescheitert) " · letzter Stand" else ""
        }
    }
    Unterzeile("Google AI Studio")
    Wert("Freikontingent (Google-Gutschriften)", gutschrift { "${b(it.restEur ?: 0.0)} ${it.waehrung} von ${b(it.gewaehrt)} ${it.waehrung} verfügbar" })
    // Google nennt nur noch laufende Gutschriften; Verbrauch aus bereits verfallenen zählt deshalb nicht mit.
    Wert("Bisher verbraucht (laufende Gutschriften)", gutschrift { "${b(it.verbraucht)} ${it.waehrung}" })
    val eingezahlt = g?.vorauszahlung
    Wert("Selbst eingezahltes Guthaben", when {
        g == null -> fehlt
        eingezahlt == null -> if (rest == null || vm.googleGuthabenLaedt) fehlt else "Nicht verfügbar: Der Betrag ließ sich nicht lesen."
        g.vorauszahlungVeraltet -> "${b(eingezahlt)} ${g.waehrung} · letzter lesbarer Betrag" +
            (g.vorauszahlungGelesen.takeIf { it > 0 }?.let { " vom ${GoogleGuthabenFaehigkeit.datum(it / 1000)}" } ?: "")
        else -> "${b(eingezahlt)} ${g.waehrung}"
    })
    if (g != null && rest != null && g.waehrung != "€") {
        Text("Google führt dieses Konto in ${g.waehrung}. Jarvis rechnet nicht in Euro um, damit keine geschätzten Beträge entstehen.",
            color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
    Fuss(g?.standText().orEmpty(), g?.hinweis, vm.googleGuthabenLaedt) { vm.googleGuthabenAbfragen(erzwingen = true) }
}

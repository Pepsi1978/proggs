package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.faehigkeit.AnthropicGuthabenFaehigkeit
import de.frank.jarvis.faehigkeit.GoogleGuthabenFaehigkeit
import de.frank.jarvis.ui.theme.LocalFarben
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DATUM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)

private fun bis(d: LocalDate): String = DATUM.format(d) + " · " + GoogleGuthabenFaehigkeit.tageText(ChronoUnit.DAYS.between(LocalDate.now(), d))

/**
 * Reiter „Guthaben“: alles geschenkte Guthaben auf einen Blick, je Anbieter eine Karte im selben Aufbau —
 * groß der Rest, darunter der Verbrauch als Balken, Verfall und nächstes Guthaben, zuletzt ob echtes Geld angetastet wird.
 */
@Composable
internal fun GuthabenReiter(vm: AppViewModel) {
    val f = LocalFarben.current
    LaunchedEffect(Unit) { vm.anthropicGuthabenAbfragen(); vm.googleGuthabenAbfragen() }
    Text("Geschenktes Guthaben wird immer vor deinem eigenen Geld verbraucht. Hier siehst du, wie viel noch übrig ist und wann es verfällt.",
        color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
    ClaudeKarte(vm)
    GoogleKarte(vm)
}

@Composable
private fun ClaudeKarte(vm: AppViewModel) {
    val f = LocalFarben.current
    var anmelden by rememberSaveable { mutableStateOf(false) }
    val g = vm.anthropicGuthaben
    Bereich("Claude-Modelle (Anthropic)") {
        Text("Bezahlt Jarvis' Kopf, wenn du ein Claude-Modell gewählt hast.", color = f.textSchwach, fontSize = 13.sp)
        val rest = g?.restUsd
        if (rest == null) {
            NichtDa(g?.hinweis, vm.anthropicGuthabenLaedt, vm.anthropicConsoleAngemeldet, "Claude Console",
                "Jarvis liest das Guthaben aus der Claude Console. Dafür einmal anmelden, am besten per E-Mail-Code.",
                aktualisieren = { vm.anthropicGuthabenAbfragen(erzwingen = true) }, abmelden = { vm.anthropicConsoleAbmelden() }) { anmelden = true }
        } else {
            val usd = AnthropicGuthabenFaehigkeit::betrag
            val eur = g.kurs?.let { k -> { v: Double -> "≈ ${usd(v * k)} €" } }
            Gross(eur?.invoke(rest) ?: "${usd(rest)} $", "${usd(rest)} $ übrig" + (g.gewaehrtUsd?.let { " von ${usd(it)} $" } ?: ""))
            g.gewaehrtUsd?.takeIf { it > 0 }?.let { Balken(((it - rest) / it).toFloat()) }
            g.verbrauchtUsd?.let { Wert("Verbraucht", "${usd(it)} $" + (eur?.let { e -> " (${e(it)})" } ?: "")) }
            g.verfallDatum?.let { Wert("Verfällt", bis(it)) }
            g.naechstes?.let { Wert("Nächstes Guthaben", "voraussichtlich " + bis(it) + (g.abobetragUsd?.let { b -> " · etwa ${usd(b)} $" } ?: "")) }
            val eigenes = g.eigenesUsd
            EigenesGeld(
                when {
                    eigenes == null -> "Ob eigenes Geld hinterlegt ist, zeigt die nächste Abfrage."
                    eigenes < 0.005 -> "Kein eigenes Geld hinterlegt: Es wird nichts abgebucht."
                    else -> "Eigenes gekauftes Guthaben: ${usd(eigenes)} $ hinterlegt."
                },
                ok = rest > 0.004,
            )
            Fuss(g.standText(), g.hinweis, vm.anthropicGuthabenLaedt) { vm.anthropicGuthabenAbfragen(erzwingen = true) }
            Knopf("Bei der Claude Console abmelden", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { vm.anthropicConsoleAbmelden() }
        }
    }
    if (anmelden) ConsoleAnmeldung(onFertig = { anmelden = false; vm.anthropicConsoleAnmeldungPruefen() })
}

@Composable
private fun GoogleKarte(vm: AppViewModel) {
    val f = LocalFarben.current
    var anmelden by rememberSaveable { mutableStateOf(false) }
    val g = vm.googleGuthaben
    Bereich("Gemini-Stimmen (Google AI Studio)") {
        Text("Bezahlt alles, was über deinen bezahlten Gemini-Schlüssel läuft: das Vorlesen mit Gemini 3.8 Flash TTS und Flash-Lite TTS und das Live-Gespräch mit Gemini, sobald es auf den bezahlten Schlüssel wechselt.", color = f.textSchwach, fontSize = 13.sp)
        val rest = g?.restEur
        if (rest == null) {
            NichtDa(g?.hinweis, vm.googleGuthabenLaedt, vm.googleAngemeldet, "Google",
                "Jarvis liest das Guthaben aus AI Studio. Dafür einmal mit deinem Google-Konto anmelden.",
                aktualisieren = { vm.googleGuthabenAbfragen(erzwingen = true) }, abmelden = { vm.googleAbmelden() }) { anmelden = true }
        } else {
            val w = g.waehrung
            val b = GoogleGuthabenFaehigkeit::betrag
            Gross("${b(rest)} $w", "übrig von ${b(g.gewaehrt)} $w")
            if (g.gewaehrt > 0) Balken((g.verbraucht / g.gewaehrt).toFloat())
            g.ausgabenMonat?.let { Wert("Ausgaben diesen Monat", "${b(it)} $w" + (g.ausgabenLimit?.let { l -> " von ${b(l)} $w Limit" } ?: "")) }
            Wert("Vom Guthaben abgebucht", "${b(g.verbraucht)} $w")
            g.offen.firstOrNull()?.let { Wert("Verfällt als Nächstes", "${b(it.rest)} $w am " + bis(java.time.Instant.ofEpochSecond(it.ende).atZone(java.time.ZoneId.systemDefault()).toLocalDate())) }
            g.naechste?.let { Wert("Nächstes Guthaben", "voraussichtlich " + bis(it) + (g.letzterBetrag?.let { x -> " · etwa ${b(x)} $w" } ?: "")) }
            val vz = g.vorauszahlung
            if (vz != null) {
                Unterzeile("Eingezahltes Geld (Vorauszahlung)")
                Text("${b(vz)} $w", color = f.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                val seit = g.vorauszahlungSeit.takeIf { it > 0 }?.let { GoogleGuthabenFaehigkeit.datum(it / 1000) }
                EigenesGeld(
                    when {
                        g.vorauszahlungGesunken -> "Gesunken: am ${seit ?: "?"} von ${b(g.vorauszahlungVorher!!)} $w auf ${b(vz)} $w. Hier wurde echtes Geld abgebucht."
                        rest > 0.004 -> "Unverändert" + (seit?.let { " seit $it" } ?: "") + ". Die Gemini-Stimmen laufen über das Guthaben, dein Geld bleibt unangetastet."
                        else -> "Das Guthaben ist aufgebraucht: Ab jetzt zahlt deine Vorauszahlung das Vorlesen."
                    },
                    ok = !g.vorauszahlungGesunken && rest > 0.004,
                )
            } else EigenesGeld(
                if (rest > 0.004) "Deine Vorauszahlung (echtes Geld) wird erst belastet, wenn dieses Guthaben aufgebraucht ist."
                else "Das Guthaben ist aufgebraucht: Jetzt zahlt deine Vorauszahlung das Vorlesen.",
                ok = rest > 0.004,
            )
            if (g.offen.isNotEmpty()) {
                Unterzeile("Einzelne Gutschriften")
                g.offen.forEach { s ->
                    val tage = GoogleGuthabenFaehigkeit.tageBis(s.ende)
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${b(s.rest)} $w" + (if (s.verbraucht > 0.004) " von ${b(s.betrag)} $w" else ""), Modifier.weight(1f), color = f.text, fontSize = 14.sp)
                        Text("bis ${GoogleGuthabenFaehigkeit.datum(s.ende)} · ${GoogleGuthabenFaehigkeit.tageText(tage)}",
                            color = if (tage < 14) f.gefahr else f.textLeise, fontSize = 13.sp, textAlign = TextAlign.End)
                    }
                }
            }
            Fuss(g.standText(), g.hinweis, vm.googleGuthabenLaedt) { vm.googleGuthabenAbfragen(erzwingen = true) }
            Knopf("Bei Google abmelden", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { vm.googleAbmelden() }
        }
    }
    if (anmelden) GoogleAnmeldung(onFertig = { anmelden = false; vm.googleAnmeldungPruefen() })
}

/** Noch keine Zahl: entweder anmelden oder der Grund, warum die Abfrage scheiterte. */
@Composable
private fun NichtDa(hinweis: String?, laedt: Boolean, angemeldet: Boolean, dienst: String, warum: String,
                    aktualisieren: () -> Unit, abmelden: () -> Unit, anmelden: () -> Unit) {
    val f = LocalFarben.current
    Text(
        when { laedt -> "Wird abgefragt …"; !angemeldet -> "Noch nicht angemeldet."; else -> hinweis ?: "Noch nicht abgefragt." },
        color = if (angemeldet && hinweis != null && !laedt) f.gefahr else f.textLeise, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp),
    )
    if (!angemeldet) {
        Text(warum, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Knopf("Bei $dienst anmelden", Modifier.padding(top = 10.dp).fillMaxWidth()) { anmelden() }
    } else if (!laedt) {
        // Angemeldet, aber keine Zahl (Sitzung abgelaufen, Netz weg): nie ohne Ausweg stehen lassen.
        Knopf("Erneut abfragen", Modifier.padding(top = 10.dp).fillMaxWidth()) { aktualisieren() }
        Knopf("Neu bei $dienst anmelden", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { anmelden() }
        Knopf("Bei $dienst abmelden", Modifier.padding(top = 8.dp).fillMaxWidth(), haupt = false) { abmelden() }
    }
}

@Composable
private fun Gross(betrag: String, unter: String) {
    val f = LocalFarben.current
    Text(betrag, color = f.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
    Text(unter, color = f.textLeise, fontSize = 14.sp)
}

/** Verbrauchter Anteil als Balken: hell der Rest, kräftig das Verbrauchte. */
@Composable
private fun Balken(verbraucht: Float) {
    val f = LocalFarben.current
    Box(Modifier.padding(top = 10.dp, bottom = 6.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(f.flaecheStark)) {
        Box(Modifier.fillMaxWidth(verbraucht.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(f.primaer))
    }
}

@Composable
private fun Wert(titel: String, wert: String) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(titel, color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(wert, color = f.text, fontSize = 15.sp)
    }
}

@Composable
private fun EigenesGeld(text: String, ok: Boolean) {
    val f = LocalFarben.current
    val farbe: Color = if (ok) f.erfolg else f.gefahr
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(10.dp)).background(farbe.copy(alpha = 0.12f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 10.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(farbe))
        Text(text, color = f.text, fontSize = 13.sp)
    }
}

@Composable
private fun Fuss(stand: String, hinweis: String?, laedt: Boolean, aktualisieren: () -> Unit) {
    val f = LocalFarben.current
    if (hinweis != null) Text(hinweis, color = f.gefahr, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stand, Modifier.weight(1f), color = f.textSchwach, fontSize = 12.sp)
        Knopf(if (laedt) "Wird abgefragt …" else "Aktualisieren", haupt = false) { aktualisieren() }
    }
}

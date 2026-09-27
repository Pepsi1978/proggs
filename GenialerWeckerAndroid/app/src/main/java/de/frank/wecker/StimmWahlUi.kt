package de.frank.wecker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.StillerKnopf
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.wecker.design.DesignDialog

/**
 * Die eine Stimmauswahl der App: nur Stimmen der Handysprache, Premium zuerst, darunter die Gerätestimmen.
 * [gewaehlt] ist eine Stimmkennung (Premium-ID oder Name einer Gerätestimme); leer = Vorgabe.
 * [vorgabeZeile] zeigt oben eine Zeile „Standard aus den Einstellungen“ (nur im Wecker-Editor).
 */
@Composable
fun StimmWahlListe(vm: WeckerViewModel, sprache: String, gewaehlt: String, vorgabeZeile: String? = null, waehlen: (String) -> Unit) {
    val alle by vm.stimmenJeSprache.collectAsStateWithLifecycle()
    val gold = LocalGold.current
    val premium = PremiumKatalog.fuer(sprache, vm.geraeteRegion)
    val geraet = alle?.get(sprache).orEmpty()
    val premiumAus = vm.settings.premiumEinwilligung == "nein"

    @Composable
    fun Zeile(id: String, titel: String, unter: String, anhoeren: (() -> Unit)?) {
        val aktiv = id == gewaehlt
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .background(if (aktiv) Brush.horizontalGradient(listOf(gold.primaer.copy(alpha = .16f), gold.primaer.copy(alpha = .03f)))
                else Brush.horizontalGradient(listOf(gold.primaer.copy(alpha = 0f), gold.primaer.copy(alpha = 0f))), RoundedCornerShape(14.dp))
            .selectable(aktiv, interactionSource = null, indication = null, role = androidx.compose.ui.semantics.Role.RadioButton) { waehlen(id) }
            .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(aktiv, null)
            Column(Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
                Text(titel, style = MaterialTheme.typography.bodyLarge, fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal)
                if (unter.isNotBlank()) Text(unter, style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
            }
            if (anhoeren != null) AnhoerKnopf(vm, "stimme:$id", anhoeren)
        }
    }

    if (vorgabeZeile != null) Zeile("", "Standard aus den Einstellungen", vorgabeZeile, null)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Premium-Stimmen", style = MaterialTheme.typography.titleSmall, color = gold.primaer)
        Text("wie ein Mensch", Modifier.border(1.dp, gold.primaer.copy(alpha = .6f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall, color = gold.primaer)
    }
    if (premiumAus) {
        Text("Premium-Stimmen sind ausgeschaltet. Es gelten die Gerätestimmen.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
        StillerKnopf("Premium-Stimmen einschalten", { vm.einwilligung(true) }, hervorgehoben = true)
    } else premium.forEach { s ->
        Zeile(s.id, s.anzeige, s.regionName) { vm.previewVoice(sprache = sprache, stimme = s.id) }
    }

    Text("Gerätestimmen · offline", style = MaterialTheme.typography.titleSmall, color = gold.primaer, modifier = Modifier.padding(top = 8.dp))
    when {
        alle == null -> Text("Gerätestimmen werden geprüft …", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
        geraet.isEmpty() -> Text("Auf diesem Handy ist für ${Sprachen.name(sprache)} keine Offline-Stimme installiert.",
            style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
        else -> geraet.forEach { s -> Zeile(s.name, s.anzeige, "", { vm.previewVoice(sprache = sprache, stimme = s.name) }) }
    }
}

/** Kurze, ehrliche Einwilligung vor dem ersten Einsatz einer Premium-Stimme. */
@Composable
fun PremiumEinwilligungDialog(vm: WeckerViewModel) {
    val frage by vm.premiumFrage.collectAsStateWithLifecycle()
    if (!frage) return
    DesignDialog(
        titel = "Premium-Stimmen",
        aufSchliessen = { vm.premiumFrage.value = false },
        bestaetigung = { GoldKnopf("Premium nutzen", { vm.einwilligung(true) }, hauptKnopf = true) },
        abbruch = { StillerKnopf("Nur Gerätestimmen", { vm.einwilligung(false) }) },
        inhalt = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Premium-Stimmen klingen wie ein echter Mensch. Dafür wird dein Wecktext beim Speichern einmal an den Sprachdienst von Microsoft geschickt und als Audiodatei zurückgeliefert.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Geweckt wird immer offline: Die fertige Ansage liegt auf deinem Handy. Ohne Internet übernimmt automatisch die Gerätestimme.",
                    style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                Text("Du kannst das jederzeit in den Einstellungen unter „Vorlesestimme“ ändern.",
                    style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            }
        },
    )
}

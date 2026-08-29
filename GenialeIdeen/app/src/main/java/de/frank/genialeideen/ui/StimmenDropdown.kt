package de.frank.genialeideen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.tts.VoiceGender
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.lichtKante
import de.frank.genialeideen.ui.theme.milchglas
import de.frank.genialeideen.ui.theme.tiefenSchatten

/**
 * Die Stimmenauswahl in zwei Stufen (Kapitel 4.6).
 *
 * Oben die Gruppe — meine Stimmen, Google Chirp 3 HD, die Edge-Stimmen —, darunter die
 * Stimmen genau dieser Gruppe. Eine einzige, seitenlange Liste über alle Anbieter war nicht
 * zu überblicken; beide Klappmenüs sind so breit wie ihr Knopf und bleiben unter ihm stehen.
 *
 * An der Sache ändert sich nichts: Ich wähle die Stimme, die Engine wird mitgeschaltet.
 */
@Composable
fun StimmenDropdown(
    eintraege: List<StimmenEintrag>,
    gewaehlt: String,
    aufWahl: (StimmenEintrag) -> Unit,
    aufProbe: (StimmenEintrag) -> Unit,
    aufFavorit: (String) -> Unit,
    favoriten: Set<String>,
    modifier: Modifier = Modifier,
    laedt: Boolean = false,
    spricht: String? = null,
    aufNeuLaden: (() -> Unit)? = null,
    aufAufnehmen: (() -> Unit)? = null,
    aufFehlendenSchluessel: (() -> Unit)? = null,
) {
    val gold = LocalGold.current
    val form = RoundedCornerShape(14.dp)

    val aktuell = eintraege.firstOrNull { it.id == gewaehlt && it.gruppe != Stimmenliste.GRUPPE_FAVORITEN }
    // Die Gruppen in der Reihenfolge, in der die Liste sie liefert (Kapitel 4.6).
    val gruppen = remember(eintraege) { eintraege.map(StimmenEintrag::gruppe).distinct() }

    // Die Gruppe folgt der gewählten Stimme, lässt sich aber frei durchblättern.
    var gruppe by remember(aktuell?.gruppe, gruppen) {
        mutableStateOf(aktuell?.gruppe ?: gruppen.firstOrNull().orEmpty())
    }
    var gruppeOffen by remember { mutableStateOf(false) }
    var stimmenOffen by remember { mutableStateOf(false) }
    var suche by remember { mutableStateOf("") }

    val inGruppe = remember(eintraege, gruppe) { eintraege.filter { it.gruppe == gruppe } }
    val gefiltert = if (suche.isBlank()) {
        inGruppe
    } else {
        inGruppe.filter { it.name.contains(suche, ignoreCase = true) }
    }

    Column(modifier) {
        // ---- Stufe 1: die Gruppe ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Stimmenart",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = gold.textGedaempft,
            )
            if (aufNeuLaden != null) {
                StillerKnopf("Stimmen neu laden", aufNeuLaden)
            }
        }
        Spacer(Modifier.height(6.dp))
        KlappFeld(
            beschriftung = gruppe.ifBlank { "Stimmenart wählen" },
            zusatz = if (gruppe.isBlank()) "" else "${inGruppe.size}",
            offen = gruppeOffen,
            gefuellt = gruppe.isNotBlank(),
            form = form,
            aufOeffnen = { gruppeOffen = true },
        ) { breite ->
            DropdownMenu(
                expanded = gruppeOffen,
                onDismissRequest = { gruppeOffen = false },
                modifier = Modifier
                    .width(breite)
                    .milchglas(gold.flaecheErhoeht, RoundedCornerShape(16.dp), deckung = 0.96f)
                    .heightIn(max = 420.dp),
            ) {
                gruppen.forEach { name ->
                    val anzahl = eintraege.count { it.gruppe == name }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .druckEffekt {
                                gruppe = name
                                gruppeOffen = false
                                suche = ""
                            }
                            .background(
                                if (name == gruppe) gold.primaer.copy(alpha = 0.12f) else Color.Transparent,
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (name == gruppe) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Ausgewählt",
                                tint = gold.primaer,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Spacer(Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (name == gruppe) gold.primaer else gold.textPrimaer,
                        )
                        Text(
                            anzahl.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = gold.textGedaempft,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Stufe 2: die Stimme aus dieser Gruppe ----
        Text(
            "Stimme",
            style = MaterialTheme.typography.labelSmall,
            color = gold.textGedaempft,
        )
        Spacer(Modifier.height(6.dp))
        KlappFeld(
            beschriftung = when {
                laedt && aktuell == null -> "Wird geladen …"
                aktuell != null && aktuell.gruppe == gruppe -> aktuell.name
                inGruppe.isEmpty() -> "Keine Stimme in dieser Gruppe"
                else -> "Stimme wählen"
            },
            zusatz = if (aktuell != null && aktuell.gruppe == gruppe) aktuell.herkunft else "",
            offen = stimmenOffen,
            gefuellt = aktuell != null && aktuell.gruppe == gruppe,
            form = form,
            aufOeffnen = { if (inGruppe.isNotEmpty()) stimmenOffen = true },
        ) { breite ->
            DropdownMenu(
                expanded = stimmenOffen,
                onDismissRequest = { stimmenOffen = false; suche = "" },
                modifier = Modifier
                    .width(breite)
                    .milchglas(gold.flaecheErhoeht, RoundedCornerShape(16.dp), deckung = 0.96f)
                    .heightIn(max = 480.dp),
            ) {
                // Suchfeld erst ab 15 Stimmen — darunter kostet es nur Platz (Kapitel 4.6).
                if (inGruppe.size > 15) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(gold.eingabefeld)
                            .border(1.dp, gold.rahmen, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        if (suche.isEmpty()) {
                            Text(
                                "Stimme suchen",
                                style = MaterialTheme.typography.bodySmall,
                                color = gold.textGedaempft,
                            )
                        }
                        BasicTextField(
                            value = suche,
                            onValueChange = { suche = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = gold.textPrimaer),
                            cursorBrush = SolidColor(gold.primaer),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = gold.rahmen,
                    )
                }

                gefiltert.forEach { eintrag ->
                    StimmenZeile(
                        eintrag = eintrag,
                        gewaehlt = eintrag.id == gewaehlt,
                        istFavorit = eintrag.id in favoriten,
                        spricht = spricht == eintrag.id,
                        aufTipp = {
                            when {
                                eintrag.id == Stimmenliste.ID_STIMME_AUFNEHMEN -> {
                                    stimmenOffen = false
                                    if (eintrag.name.startsWith("Konnte nicht")) {
                                        aufNeuLaden?.invoke()
                                    } else {
                                        aufAufnehmen?.invoke()
                                    }
                                }
                                // Ein Tipp auf eine gesperrte Stimme führt direkt zum Schlüsselfeld.
                                !eintrag.nutzbar -> {
                                    stimmenOffen = false
                                    aufFehlendenSchluessel?.invoke()
                                }
                                else -> {
                                    aufWahl(eintrag)
                                    stimmenOffen = false
                                    suche = ""
                                }
                            }
                        },
                        // Die Probe schliesst das Menü bewusst nicht — so hört man mehrere
                        // Stimmen nacheinander durch.
                        aufProbe = { aufProbe(eintrag) },
                        aufFavorit = { aufFavorit(eintrag.id) },
                    )
                }
            }
        }
    }
}

/**
 * Ein Klappfeld über die volle Breite. Es misst sich selbst und reicht seine Breite an das
 * Menü weiter, damit das Aufklappen genau unter dem Knopf steht statt daneben.
 */
@Composable
private fun KlappFeld(
    beschriftung: String,
    zusatz: String,
    offen: Boolean,
    gefuellt: Boolean,
    form: RoundedCornerShape,
    aufOeffnen: () -> Unit,
    menue: @Composable (Dp) -> Unit,
) {
    val gold = LocalGold.current
    val dichte = LocalDensity.current
    var breite by remember { mutableStateOf(0.dp) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { breite = with(dichte) { it.size.width.toDp() } }
                .druckEffekt(aufOeffnen)
                .tiefenSchatten(gold.primaer, Hoehe.karte, form)
                .clip(form)
                .background(gold.eingabefeld)
                .border(1.dp, if (offen) gold.primaer else gold.rahmen, form)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = beschriftung,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (gefuellt) gold.textPrimaer else gold.textGedaempft,
                maxLines = 1,
            )
            if (zusatz.isNotBlank()) {
                HerkunftsChip(zusatz)
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Auswählen",
                tint = gold.primaer,
                modifier = Modifier.size(20.dp).rotate(if (offen) 180f else 0f),
            )
        }
        menue(breite)
    }
}

@Composable
private fun StimmenZeile(
    eintrag: StimmenEintrag,
    gewaehlt: Boolean,
    istFavorit: Boolean,
    spricht: Boolean,
    aufTipp: () -> Unit,
    aufProbe: () -> Unit,
    aufFavorit: () -> Unit,
) {
    val gold = LocalGold.current
    val platzhalter = eintrag.id == Stimmenliste.ID_STIMME_AUFNEHMEN
    val gedaempft = !eintrag.nutzbar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .druckEffekt(aufTipp)
            .background(if (gewaehlt) gold.primaer.copy(alpha = 0.12f) else Color.Transparent)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (gewaehlt) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Ausgewählt",
                tint = gold.primaer,
                modifier = Modifier.size(16.dp),
            )
        } else if (platzhalter) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = gold.primaer,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Spacer(Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    eintrag.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        gedaempft -> gold.textGedaempft
                        gewaehlt -> gold.primaer
                        else -> gold.textPrimaer
                    },
                )
                eintrag.geschlecht?.let { geschlecht ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (geschlecht == VoiceGender.FEMALE) "w" else "m",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.textGedaempft,
                    )
                }
            }
            val untertext = when {
                gedaempft -> eintrag.grund
                eintrag.zusatz.isNotBlank() -> eintrag.zusatz
                else -> ""
            }
            if (untertext.isNotBlank()) {
                Text(
                    untertext,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (gedaempft) gold.akzentWarm else gold.textGedaempft,
                )
            }
        }
        if (!platzhalter) {
            Box(
                modifier = Modifier.size(30.dp).druckEffekt(aufFavorit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (istFavorit) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Als Favorit merken",
                    tint = if (istFavorit) gold.primaer else gold.textGedaempft,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (eintrag.nutzbar) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .druckEffekt(aufProbe)
                        .clip(CircleShape)
                        .border(1.dp, lichtKante(staerke = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (spricht) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (spricht) "Probe anhalten" else "Probe abspielen",
                        tint = gold.primaer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Das kleine Kennzeichen der Herkunft: „Meine", „Alibaba", „Google", „Edge". */
@Composable
private fun HerkunftsChip(text: String) {
    val gold = LocalGold.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(gold.primaer.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = gold.primaerGedaempft)
    }
}

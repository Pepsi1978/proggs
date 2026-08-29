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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.tts.VoiceGender
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.lichtKante
import de.frank.genialeideen.ui.theme.milchglas
import de.frank.genialeideen.ui.theme.tiefenSchatten

/**
 * Die **eine** Stimmenauswahl der App (Kapitel 4.6): alle Stimmen aller Engines in einem
 * Aufklapp-Menü, in fester Gruppenreihenfolge. Es gibt keinen vorgelagerten Engine-Umschalter —
 * mit der Stimme wird die Engine mitgeschaltet.
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
    var offen by remember { mutableStateOf(false) }
    var suche by remember { mutableStateOf("") }
    val form = RoundedCornerShape(14.dp)

    val aktuell = eintraege.firstOrNull { it.id == gewaehlt && it.gruppe != Stimmenliste.GRUPPE_FAVORITEN }
    val gefiltert = if (suche.isBlank()) {
        eintraege
    } else {
        eintraege.filter { it.name.contains(suche, ignoreCase = true) }
    }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Stimme",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = gold.textGedaempft,
            )
            if (aufNeuLaden != null) {
                StillerKnopf("Stimmen neu laden", aufNeuLaden)
            }
        }
        Spacer(Modifier.height(6.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .druckEffekt { offen = true }
                    .tiefenSchatten(gold.primaer, Hoehe.karte, form)
                    .clip(form)
                    .background(gold.eingabefeld)
                    .border(1.dp, if (offen) gold.primaer else gold.rahmen, form)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        laedt && aktuell == null -> "Wird geladen …"
                        aktuell != null -> aktuell.name
                        else -> "Stimme wählen"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (aktuell != null) gold.textPrimaer else gold.textGedaempft,
                    maxLines = 1,
                )
                aktuell?.let { HerkunftsChip(it.herkunft) }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Stimme wählen",
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp).rotate(if (offen) 180f else 0f),
                )
            }

            DropdownMenu(
                expanded = offen,
                onDismissRequest = { offen = false; suche = "" },
                modifier = Modifier
                    .milchglas(gold.flaecheErhoeht, RoundedCornerShape(16.dp), deckung = 0.96f)
                    .heightIn(max = 480.dp),
            ) {
                // Suchfeld erst ab 15 Stimmen — darunter kostet es nur Platz (Kapitel 4.6).
                if (eintraege.size > 15) {
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
                }

                var letzteGruppe: String? = null
                gefiltert.forEach { eintrag ->
                    if (eintrag.gruppe != letzteGruppe) {
                        letzteGruppe = eintrag.gruppe
                        if (eintrag.gruppe != gefiltert.first().gruppe) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = gold.rahmen,
                            )
                        }
                        Text(
                            eintrag.gruppe,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = gold.primaer,
                            modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
                    StimmenZeile(
                        eintrag = eintrag,
                        gewaehlt = eintrag.id == gewaehlt,
                        istFavorit = eintrag.id in favoriten,
                        spricht = spricht == eintrag.id,
                        aufTipp = {
                            when {
                                eintrag.id == Stimmenliste.ID_STIMME_AUFNEHMEN -> {
                                    offen = false
                                    if (eintrag.name.startsWith("Konnte nicht")) {
                                        aufNeuLaden?.invoke()
                                    } else {
                                        aufAufnehmen?.invoke()
                                    }
                                }
                                // Ein Tipp auf eine gesperrte Stimme führt direkt zum Schlüsselfeld.
                                !eintrag.nutzbar -> {
                                    offen = false
                                    aufFehlendenSchluessel?.invoke()
                                }
                                else -> {
                                    aufWahl(eintrag)
                                    offen = false
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
            HerkunftsChip(eintrag.herkunft)
            Spacer(Modifier.width(4.dp))
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

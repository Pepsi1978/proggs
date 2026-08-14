package de.frank.stacklabor.ui.blaetter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.daten.entitaeten.StackE
import de.frank.stacklabor.ui.bausteine.Chip
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * **B-13 — Stack bearbeiten (F-01).**
 *
 * `stackId == null` heisst: einen neuen Stack anlegen. Beides ist derselbe Bildschirm, weil
 * es dieselben drei Felder sind — nur das Loeschen gibt es beim Anlegen nicht.
 *
 * Zeitpunkt und Einnahme-Hinweis sind keine Zierde: beide gehen in die KI-Anfrage ein (F-12)
 * und beeinflussen die Bewertung.
 */
@Composable
fun StackBearbeitenBlatt(
    app: StackLaborApp,
    stackId: String?,
    aufSchliessen: () -> Unit,
) {
    val bereich = rememberCoroutineScope()

    val stackFluss: Flow<StackE?> = remember(stackId) {
        if (stackId == null) flowOf(null) else app.datenbank.stackDao().stack(stackId)
    }
    val stack by stackFluss.collectAsStateWithLifecycle(initialValue = null)

    val anzahlFluss: Flow<Int> = remember(stackId) {
        if (stackId == null) flowOf(0) else app.datenbank.stackDao().anzahlEintraege(stackId)
    }
    val anzahlMittel by anzahlFluss.collectAsStateWithLifecycle(initialValue = 0)

    val alleStacks by app.repository.alleStacks.collectAsStateWithLifecycle(initialValue = emptyList())

    var geladen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var zeitpunkt by remember { mutableStateOf("") }
    var einnahmeHinweis by remember { mutableStateOf("") }
    var loeschenGefragt by remember { mutableStateOf(false) }
    var grundGezeigt by remember { mutableStateOf(false) }

    LaunchedEffect(stack) {
        val s = stack
        if (!geladen && s != null) {
            name = s.name
            zeitpunkt = s.zeitpunkt
            einnahmeHinweis = s.einnahmeHinweis
            geladen = true
        }
    }

    val nameFehler = name.isBlank()
    // F-01: Ein doppelter Name ist erlaubt — er bekommt nur eine Hinweiszeile.
    val nameDoppelt = !nameFehler && alleStacks.any {
        it.id != stackId && it.name.trim().equals(name.trim(), ignoreCase = true)
    }

    BlattRahmen(
        titel = if (stackId == null) "Neuer Stack" else "Stack bearbeiten",
        untertitel = if (stackId == null) "" else stack?.name.orEmpty(),
        aufSchliessen = aufSchliessen,
        sockel = { schliessen ->
            Column {
                if (grundGezeigt && nameFehler) {
                    Text(
                        "Ohne Namen lässt sich der Stack nicht sichern.",
                        style = Schrift.klein,
                        color = Farben.rot,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                BlattKnopf("Sichern", gesperrt = nameFehler) {
                    if (nameFehler) {
                        grundGezeigt = true
                        return@BlattKnopf
                    }
                    bereich.launch {
                        val s = stack
                        if (stackId == null || s == null) {
                            app.repository.legeStackAn(name, zeitpunkt, einnahmeHinweis)
                        } else {
                            app.repository.aktualisiereStack(
                                s.copy(
                                    name = name.trim(),
                                    zeitpunkt = zeitpunkt.trim(),
                                    einnahmeHinweis = einnahmeHinweis.trim(),
                                ),
                            )
                        }
                        schliessen()
                    }
                }
            }
        },
    ) { schliessen ->
        BlattFeldZeile(
            "Name", name, { name = it },
            "z. B. Morgens", fehler = nameFehler, maxZeichen = 60,
        )
        if (nameDoppelt) {
            BlattHinweis("Es gibt bereits einen Stack mit diesem Namen.", farbe = Farben.gelbText)
        }
        BlattFeldZeile(
            "Zeitpunkt", zeitpunkt, { zeitpunkt = it },
            "z. B. 60 Minuten nach dem Aufstehen", maxZeichen = 120,
        )
        BlattFeldZeile(
            "Einnahme-Hinweis", einnahmeHinweis, { einnahmeHinweis = it },
            "z. B. mit 1 EL Olivenöl und Wasser", zeilen = 2, maxZeichen = 120,
        )
        BlattHinweis("Zeitpunkt und Hinweis gehen in jede Auswertung dieses Stacks ein.")

        if (stackId != null) {
            Spacer(Modifier.height(12.dp))
            BlattTrenner()
            BlattRubrik("Gefahrenzone")

            if (!loeschenGefragt) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    BlattKnopf(
                        beschriftung = if (anzahlMittel == 1) "Stack löschen (1 Mittel)"
                        else "Stack löschen ($anzahlMittel Mittel)",
                        gefahr = true,
                    ) { loeschenGefragt = true }
                }
                BlattHinweis(
                    "Die Mittel selbst bleiben im Katalog — nur dieser Stack und seine Zuordnungen gehen.",
                )
            } else {
                BlattHinweis(
                    if (anzahlMittel == 1) "„${name.trim()}“ mit 1 Mittel wirklich löschen?"
                    else "„${name.trim()}“ mit $anzahlMittel Mitteln wirklich löschen?",
                    farbe = Farben.rot,
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.heightIn(min = Masse.tippflaeche),
                        contentAlignment = Alignment.Center,
                    ) {
                        Chip("Abbrechen", false) { loeschenGefragt = false }
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        BlattKnopf("Ja, löschen", gefahr = true) {
                            bereich.launch {
                                app.repository.loescheStack(stackId)
                                schliessen()
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

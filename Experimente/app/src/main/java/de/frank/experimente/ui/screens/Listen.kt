package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.frank.experimente.data.local.LogTag
import de.frank.experimente.data.local.MerkEintrag
import de.frank.experimente.data.local.MerkQuelle
import de.frank.experimente.data.local.Ziel
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Sprechziel
import de.frank.experimente.ui.components.AktionsKnopf
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.ObereLeiste
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.TextKnopf
import de.frank.experimente.ui.theme.AppForm
import de.frank.experimente.ui.theme.AppTypo
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalAppColors
import de.frank.experimente.ui.theme.LocalReduzierteBewegung
import de.frank.experimente.ui.theme.Mass
import de.frank.experimente.ui.theme.dauer
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TAG_KURZ = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)

// ---------------------------------------------------------------------------
// B-04 — Wünsche & Ziele (F-20)
// ---------------------------------------------------------------------------

@Composable
fun ZieleBildschirm(vm: AppViewModel, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    val ziele by vm.ziele.collectAsState()
    val nimmtAuf by vm.nimmtAuf.collectAsState()

    var anlegen by remember { mutableStateOf(false) }
    var bearbeitet by remember { mutableStateOf<Ziel?>(null) }

    ListenGeruest(titel = "Wünsche & Ziele", modifier = modifier) {
        if (anlegen) {
            AnlegeFlaeche(
                vm = vm,
                ziel = Sprechziel.ZIEL,
                vorgabe = bearbeitet?.text.orEmpty(),
                nimmtAuf = nimmtAuf,
                beschreibung = "Ziel einsprechen",
                onSpeichern = { text ->
                    vm.zielSpeichern(text, bearbeitet)
                    // F-20 — nach dem Speichern steht der Sprechknopf sofort wieder bereit.
                    if (bearbeitet != null) {
                        bearbeitet = null
                        anlegen = false
                    }
                },
                onLoeschen = bearbeitet?.let {
                    {
                        vm.zielLoeschen(it)
                        bearbeitet = null
                        anlegen = false
                    }
                },
                onAbbrechen = {
                    bearbeitet = null
                    anlegen = false
                },
            )
            return@ListenGeruest
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Mass.seitenrand)
                .padding(top = 20.dp, bottom = 112.dp),
        ) {
            Text(
                text = "Was möchtest du erreichen? Die Vorschläge tasten dich Schritt für Schritt heran.",
                style = AppTypo.fliesstextKlein,
                color = farben.gedaempft,
            )
            Column(
                modifier = Modifier.padding(top = Mass.abschnittAbstand),
                verticalArrangement = Arrangement.spacedBy(Mass.kartenAbstand),
            ) {
                if (ziele.isEmpty()) {
                    Text(
                        text = "Noch keine Ziele. Sprich das erste ein.",
                        style = AppTypo.fliesstext,
                        color = farben.gedaempft,
                    )
                }
                ziele.forEach { z ->
                    Karte(onClick = { bearbeitet = z; anlegen = true }) {
                        Text(z.text, style = AppTypo.fliesstext, color = farben.text)
                        Text(
                            text = TAG_KURZ.format(z.geaendertAm.atZone(ZoneId.systemDefault())),
                            style = AppTypo.daten,
                            color = farben.gedaempft,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        Plusknopf(beschreibung = "Ziel anlegen", onClick = { anlegen = true })
    }
}

// ---------------------------------------------------------------------------
// B-05 — Merkliste (F-18, F-19)
// ---------------------------------------------------------------------------

@Composable
fun MerklisteBildschirm(vm: AppViewModel, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    val merkliste by vm.merkliste.collectAsState()
    val nimmtAuf by vm.nimmtAuf.collectAsState()

    var anlegen by remember { mutableStateOf(false) }
    var loeschen by remember { mutableStateOf<MerkEintrag?>(null) }

    ListenGeruest(titel = "Merkliste", modifier = modifier) {
        if (anlegen) {
            AnlegeFlaeche(
                vm = vm,
                ziel = Sprechziel.EIGENE_IDEE,
                vorgabe = "",
                nimmtAuf = nimmtAuf,
                beschreibung = "Eigenes Experiment einsprechen",
                onSpeichern = { text ->
                    vm.eigeneIdeeAnlegen(text)
                    anlegen = false
                },
                onAbbrechen = { anlegen = false },
            )
            return@ListenGeruest
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Mass.seitenrand)
                .padding(top = Mass.abschnittAbstand, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(Mass.kartenAbstand),
        ) {
            if (merkliste.isEmpty()) {
                Text(
                    text = "Nichts gemerkt. Wenn dir ein Vorschlag gefällt, tipp auf das Lesezeichen.",
                    style = AppTypo.fliesstext,
                    color = farben.gedaempft,
                )
            }
            merkliste.forEach { eintrag ->
                Karte(onLangerDruck = { loeschen = eintrag }) {
                    Row {
                        Icon(
                            imageVector = when (eintrag.quelle) {
                                MerkQuelle.GEMERKT -> Icons.Filled.Bookmark
                                MerkQuelle.EIGEN -> Icons.Outlined.Edit
                                MerkQuelle.NICHT_UMGESETZT -> Icons.Outlined.Replay
                            },
                            contentDescription = null,
                            tint = farben.gedaempft,
                            modifier = Modifier.size(Mass.symbolKarte),
                        )
                        Column(Modifier.padding(start = Mass.kartenAbstand)) {
                            Text(eintrag.titel, style = AppTypo.kartentitel, color = farben.text)
                            if (eintrag.beschreibung.isNotBlank()) {
                                Text(
                                    text = eintrag.beschreibung,
                                    style = AppTypo.fliesstext,
                                    color = farben.text,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            Text(
                                text = "${eintrag.stufe.anzeige} · " +
                                    if (eintrag.tage == 1) "1 Tag" else "${eintrag.tage} Tage",
                                style = AppTypo.stufe,
                                color = farben.gedaempft,
                                modifier = Modifier.padding(top = Mass.kartenAbstand),
                            )
                            eintrag.vermerk?.let { vermerk ->
                                Text(
                                    text = vermerk,
                                    style = AppTypo.fliesstextKlein,
                                    color = farben.gedaempft,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Plusknopf(beschreibung = "Eigenes Experiment anlegen", onClick = { anlegen = true })
    }

    loeschen?.let { eintrag ->
        Bestaetigung(
            titel = "Eintrag löschen?",
            text = "Der Eintrag wird endgültig aus der Merkliste entfernt.",
            aktion = "Löschen",
            onAktion = { vm.merkeintragLoeschen(eintrag); loeschen = null },
            onAbbrechen = { loeschen = null },
        )
    }
}

// ---------------------------------------------------------------------------
// B-06 — Erkenntnisse (F-17)
// ---------------------------------------------------------------------------

@Composable
fun ErkenntnisseBildschirm(vm: AppViewModel, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    val erkenntnisse by vm.erkenntnisse.collectAsState()

    ListenGeruest(titel = "Erkenntnisse", modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Mass.seitenrand)
                .padding(bottom = Mass.abschnittAbstand),
            verticalArrangement = Arrangement.spacedBy(Mass.abschnittAbstand),
        ) {
            Text(
                text = "Was sich aus deinen Auswertungen ergeben hat.",
                style = AppTypo.fliesstextKlein,
                color = farben.gedaempft,
            )
            if (erkenntnisse.isEmpty()) {
                Text(
                    text = "Noch nichts. Das wächst mit deinen Auswertungen.",
                    style = AppTypo.fliesstext,
                    color = farben.gedaempft,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Mass.kartenAbstand)) {
                    erkenntnisse.forEach { e ->
                        Karte { Text(e.text, style = AppTypo.fliesstext, color = farben.text) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// B-07 — Logbuch (F-16)
// ---------------------------------------------------------------------------

@Composable
fun LogbuchBildschirm(vm: AppViewModel, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    val ausfuehrlich by vm.logAusfuehrlich.collectAsState()
    val langzeit by vm.logLangzeit.collectAsState()

    var langzeitReiter by remember { mutableStateOf(false) }
    var bearbeitet by remember { mutableStateOf<LogTag?>(null) }

    val tage = if (langzeitReiter) langzeit else ausfuehrlich

    ListenGeruest(titel = "Logbuch", modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Mass.seitenrand)
                .padding(bottom = Mass.abschnittAbstand),
            verticalArrangement = Arrangement.spacedBy(Mass.abschnittAbstand),
        ) {
            Reiter(
                links = "Letzte 15 Tage",
                rechts = "Langzeit",
                rechtsAktiv = langzeitReiter,
                onWechsel = { langzeitReiter = it },
            )

            Column(verticalArrangement = Arrangement.spacedBy(Mass.kartenAbstand)) {
                if (tage.isEmpty()) {
                    Text(
                        text = "Noch nichts aufgeschrieben.",
                        style = AppTypo.fliesstext,
                        color = farben.gedaempft,
                    )
                }
                tage.forEach { tag ->
                    Karte(onLangerDruck = { bearbeitet = tag }) {
                        Text(
                            text = TAG_KURZ.format(tag.datum),
                            style = AppTypo.daten,
                            color = farben.gedaempft,
                        )
                        Text(
                            text = (if (langzeitReiter) tag.verdichtet else tag.ausfuehrlich).orEmpty(),
                            style = AppTypo.fliesstext,
                            color = farben.text,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }

    bearbeitet?.let { tag ->
        LogtagBearbeiten(
            tag = tag,
            langzeit = langzeitReiter,
            onSpeichern = { neu -> vm.logtagSpeichern(neu); bearbeitet = null },
            onLoeschen = { vm.logtagLoeschen(tag); bearbeitet = null },
            onAbbrechen = { bearbeitet = null },
        )
    }
}

/**
 * **M-36/M-37** — zwei Reiter in einer vollrunden Fläche. Der Anzeiger schiebt sich in
 * 200 ms `linear` von einer Hälfte zur anderen, die Beschriftung wechselt in derselben Zeit
 * nach *Aktion*.
 */
@Composable
private fun Reiter(
    links: String,
    rechts: String,
    rechtsAktiv: Boolean,
    onWechsel: (Boolean) -> Unit,
) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current
    val anteil by animateFloatAsState(
        targetValue = if (rechtsAktiv) 1f else 0f,
        animationSpec = tween(dauer(Bewegung.BLENDEN_MS, reduziert), easing = Bewegung.wandern),
        label = "M-36",
    )

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(Mass.tippflaeche)
            .clip(AppForm.vollrund)
            .background(farben.erhoeht),
    ) {
        Box(
            Modifier
                .offset(x = maxWidth / 2 * anteil)
                .size(width = maxWidth / 2, height = Mass.tippflaeche)
                .clip(AppForm.vollrund)
                .background(farben.aktionGedeckt),
        )
        Row(Modifier.fillMaxSize()) {
            listOf(links to false, rechts to true).forEach { (text, istRechts) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onWechsel(istRechts) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = text,
                        style = AppTypo.knopf,
                        color = if (istRechts == rechtsAktiv) farben.aktion else farben.gedaempft,
                    )
                }
            }
        }
    }
}

/** F-16 — langer Druck auf einen Eintrag: Text ändern, speichern oder löschen. */
@Composable
private fun LogtagBearbeiten(
    tag: LogTag,
    langzeit: Boolean,
    onSpeichern: (LogTag) -> Unit,
    onLoeschen: () -> Unit,
    onAbbrechen: () -> Unit,
) {
    var text by remember(tag.datum) {
        mutableStateOf((if (langzeit) tag.verdichtet else tag.ausfuehrlich).orEmpty())
    }
    var fragtLoeschen by remember { mutableStateOf(false) }

    if (fragtLoeschen) {
        Bestaetigung(
            titel = "Eintrag löschen?",
            text = "Der Eintrag wird endgültig aus dem Logbuch entfernt.",
            aktion = "Löschen",
            onAktion = onLoeschen,
            onAbbrechen = { fragtLoeschen = false },
        )
        return
    }

    Dialogflaeche(titel = TAG_KURZ.format(tag.datum), onAbbrechen = onAbbrechen) {
        Textfeld(
            wert = text,
            platzhalter = "",
            onWert = { text = it },
            modifier = Modifier.padding(top = Mass.kartenAbstand),
            minHoehe = 160.dp,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextKnopf(text = "Löschen", onClick = { fragtLoeschen = true })
            AktionsKnopf(
                text = "Speichern",
                onClick = {
                    onSpeichern(
                        if (langzeit) tag.copy(verdichtet = text) else tag.copy(ausfuehrlich = text),
                    )
                },
                modifier = Modifier.padding(start = Mass.kartenAbstand),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Geteilte Bauteile dieser vier Bildschirme
// ---------------------------------------------------------------------------

/** Obere Leiste und darunter der ganze Rest — die untere Leiste hängt an der Navigation. */
@Composable
private fun ListenGeruest(
    titel: String,
    modifier: Modifier = Modifier,
    inhalt: @Composable BoxScope.() -> Unit,
) {
    val farben = LocalAppColors.current
    Column(modifier = modifier.fillMaxSize().background(farben.grund)) {
        ObereLeiste(titel = titel)
        Box(Modifier.fillMaxSize(), content = inhalt)
    }
}

/**
 * Die Fläche zum Anlegen (F-18, F-20): erst der Sprechknopf, nach der Transkription das
 * bearbeitbare Feld mit „Text mit KI verbessern" und „Speichern".
 */
@Composable
private fun AnlegeFlaeche(
    vm: AppViewModel,
    ziel: Sprechziel,
    vorgabe: String,
    nimmtAuf: Boolean,
    beschreibung: String,
    onSpeichern: (String) -> Unit,
    onAbbrechen: () -> Unit,
    onLoeschen: (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(vorgabe) }
    var zeigtText by remember { mutableStateOf(vorgabe.isNotBlank()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Mass.seitenrand)
            .padding(top = Mass.abschnittAbstand, bottom = 112.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Sprechknopf(
                laeuftAufnahme = nimmtAuf,
                onKlick = {
                    vm.aufnahmeUmschalten(ziel) { neu ->
                        text = neu
                        zeigtText = true
                    }
                },
            )
        }

        if (zeigtText) {
            Textfeld(
                wert = text,
                platzhalter = "",
                onWert = { text = it },
                modifier = Modifier.padding(top = Mass.abschnittAbstand),
                minHoehe = 160.dp,
            )
            Row(
                modifier = Modifier.padding(top = Mass.kartenAbstand),
                horizontalArrangement = Arrangement.spacedBy(Mass.kartenAbstand),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hatFassung = vm.hatVerbesserung(ziel)
                TextKnopf(
                    text = if (hatFassung) "Zurücknehmen" else "Text mit KI verbessern",
                    onClick = {
                        if (hatFassung) {
                            vm.verbesserungZuruecknehmen(ziel) { text = it }
                        } else {
                            vm.textVerbessern(ziel, text) { text = it }
                        }
                    },
                )
                AktionsKnopf(
                    text = "Speichern",
                    onClick = {
                        onSpeichern(text)
                        text = ""
                        zeigtText = false
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Mass.kartenAbstand),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextKnopf(text = "Abbrechen", onClick = onAbbrechen)
            onLoeschen?.let { TextKnopf(text = "Löschen", onClick = it) }
        }
    }
}

/** Der Plus-Knopf unten rechts, 48 dp über der unteren Leiste. */
@Composable
private fun BoxScope.Plusknopf(beschreibung: String, onClick: () -> Unit) {
    val farben = LocalAppColors.current
    Box(Modifier.align(Alignment.BottomEnd)) {
        Box(
            modifier = Modifier
                .padding(end = Mass.seitenrand, bottom = Mass.seitenrand)
                .size(Mass.tippflaeche)
                .clip(AppForm.vollrund)
                .background(farben.aktion)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = beschreibung,
                tint = if (farben.istDunkel) farben.text else farben.flaeche,
                modifier = Modifier.size(Mass.symbolListe),
            )
        }
    }
}

/** Rückfrage vor einer endgültigen Löschung (F-16, F-19). */
@Composable
private fun Bestaetigung(
    titel: String,
    text: String,
    aktion: String,
    onAktion: () -> Unit,
    onAbbrechen: () -> Unit,
) {
    val farben = LocalAppColors.current
    Dialogflaeche(titel = titel, onAbbrechen = onAbbrechen) {
        Text(
            text = text,
            style = AppTypo.fliesstext,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = Mass.kartenAbstand),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextKnopf(text = "Abbrechen", onClick = onAbbrechen)
            AktionsKnopf(
                text = aktion,
                onClick = onAktion,
                modifier = Modifier.padding(start = Mass.kartenAbstand),
            )
        }
    }
}

/** Die Dialogfläche selbst — 24 dp Innenabstand, 24 dp Radius, *Erhöht* mit Rand. */
@Composable
private fun Dialogflaeche(
    titel: String,
    onAbbrechen: () -> Unit,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val farben = LocalAppColors.current
    Dialog(onDismissRequest = onAbbrechen) {
        Column(
            Modifier
                .clip(AppForm.dialog)
                .background(farben.erhoeht)
                .border(Mass.randstaerke, farben.rand, AppForm.dialog)
                .padding(24.dp),
        ) {
            Text(titel, style = AppTypo.abschnittstitel, color = farben.text)
            inhalt()
        }
    }
}

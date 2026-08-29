package de.frank.claudekompass.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.claudekompass.data.local.EintragEntity
import de.frank.claudekompass.data.local.FrageEntity
import de.frank.claudekompass.data.model.Bereich
import de.frank.claudekompass.ui.components.FehlerStreifen
import de.frank.claudekompass.ui.components.HinweisStreifen
import de.frank.claudekompass.ui.components.LadeZustand
import de.frank.claudekompass.ui.components.LeerZustand
import de.frank.claudekompass.ui.components.Merkzeichen
import de.frank.claudekompass.ui.theme.LocalKompassFarben
import de.frank.claudekompass.ui.theme.Mass
import de.frank.claudekompass.tts.VorleseStufe
import de.frank.claudekompass.vm.DiktatStufe
import de.frank.claudekompass.vm.DiktatViewModel
import de.frank.claudekompass.vm.ListenEintrag
import de.frank.claudekompass.vm.ReferenzViewModel

/**
 * Ein Nachschlage-Bereich: Slash-Befehle, Config-Einstellungen oder Best Practices.
 *
 * Der Aufbau ist in allen drei gleich — alphabetisch sortiert, unter jedem Eintrag dieselben
 * Knöpfe, ganz unten der Klapp-Bereich mit dem, was aus Claude Code verschwunden ist.
 */
@Composable
fun ReferenzScreen(
    bereich: Bereich,
    viewModel: ReferenzViewModel,
    diktat: DiktatViewModel,
    modifier: Modifier = Modifier,
) {
    val zustand by viewModel.zustandFuer(bereich).collectAsStateWithLifecycle()
    val vorlesen by viewModel.vorleseZustand.collectAsStateWithLifecycle()
    val diktatZustand by diktat.zustand.collectAsStateWithLifecycle()
    val listenZustand = rememberLazyListState()

    // Wechselt der Bereich, springt die Liste an den Anfang. Ohne das steht man nach dem
    // Wechsel mitten in einer anderen Liste auf einer zufälligen Position.
    LaunchedEffect(bereich) { listenZustand.scrollToItem(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (zustand.fehler.isNotBlank() || vorlesen.fehler.isNotBlank()) {
            FehlerStreifen(
                text = zustand.fehler.ifBlank { vorlesen.fehler },
                beiSchliessen = viewModel::loescheFehler,
            )
        }
        if (zustand.meldung.isNotBlank()) {
            HinweisStreifen(text = zustand.meldung, beiSchliessen = viewModel::loescheMeldung)
        }
        if (diktatZustand.hinweis.isNotBlank()) {
            HinweisStreifen(text = diktatZustand.hinweis, beiSchliessen = diktat::loescheMeldung)
        }
        if (diktatZustand.fehler.isNotBlank()) {
            FehlerStreifen(text = diktatZustand.fehler, beiSchliessen = diktat::loescheMeldung)
        }

        when {
            zustand.laedt -> LadeZustand("Die Wissensbasis wird geladen …")
            zustand.aktive.isEmpty() && zustand.entfernte.isEmpty() -> LeerZustand(
                symbol = Icons.Default.SearchOff,
                ueberschrift = "Hier ist noch nichts",
                text = "Die mitgelieferte Wissensbasis konnte nicht eingelesen werden. " +
                    "Der Aktualisieren-Knopf oben holt sie aus dem Netz.",
            )
            else -> LazyColumn(
                state = listenZustand,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = Mass.randSchmal,
                    end = Mass.randSchmal,
                    top = Mass.abstandKlein,
                    bottom = Mass.rand * 3,
                ),
                verticalArrangement = Arrangement.spacedBy(Mass.abstandKlein),
            ) {
                items(zustand.aktive, key = { it.eintrag.id }) { listenEintrag ->
                    EintragKarte(
                        listenEintrag = listenEintrag,
                        ausgeklappt = listenEintrag.eintrag.id in zustand.ausgeklappt,
                        fragenOffen = listenEintrag.eintrag.id in zustand.fragenOffen,
                        arbeitet = listenEintrag.eintrag.id in zustand.arbeitetAn,
                        spricht = vorlesen.quelleId == listenEintrag.eintrag.id &&
                            vorlesen.stufe != VorleseStufe.AUS,
                        laedtTon = vorlesen.quelleId == listenEintrag.eintrag.id &&
                            vorlesen.stufe == VorleseStufe.LAEDT,
                        diktiertHier = diktatZustand.ziel == listenEintrag.eintrag.id &&
                            diktatZustand.stufe != DiktatStufe.AUS,
                        diktatStufe = diktatZustand.stufe,
                        antwortSpricht = { id -> vorlesen.quelleId == "frage:$id" && vorlesen.stufe != VorleseStufe.AUS },
                        viewModel = viewModel,
                        diktat = diktat,
                    )
                }

                if (zustand.entfernte.isNotEmpty()) {
                    item(key = "entfernt-kopf") {
                        EntfernteUeberschrift(
                            anzahl = zustand.entfernte.size,
                            offen = SCHLUESSEL_ENTFERNT in zustand.ausgeklappt,
                            beiKlick = { viewModel.schalteAusgeklappt(SCHLUESSEL_ENTFERNT) },
                        )
                    }
                    if (SCHLUESSEL_ENTFERNT in zustand.ausgeklappt) {
                        items(zustand.entfernte, key = { "weg-" + it.eintrag.id }) { listenEintrag ->
                            EintragKarte(
                                listenEintrag = listenEintrag,
                                ausgeklappt = listenEintrag.eintrag.id in zustand.ausgeklappt,
                                fragenOffen = listenEintrag.eintrag.id in zustand.fragenOffen,
                                arbeitet = listenEintrag.eintrag.id in zustand.arbeitetAn,
                                spricht = vorlesen.quelleId == listenEintrag.eintrag.id &&
                                    vorlesen.stufe != VorleseStufe.AUS,
                                laedtTon = vorlesen.quelleId == listenEintrag.eintrag.id &&
                                    vorlesen.stufe == VorleseStufe.LAEDT,
                                diktiertHier = diktatZustand.ziel == listenEintrag.eintrag.id &&
                                    diktatZustand.stufe != DiktatStufe.AUS,
                                diktatStufe = diktatZustand.stufe,
                                antwortSpricht = { id ->
                                    vorlesen.quelleId == "frage:$id" && vorlesen.stufe != VorleseStufe.AUS
                                },
                                viewModel = viewModel,
                                diktat = diktat,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val SCHLUESSEL_ENTFERNT = "__entfernt__"

@Composable
private fun EntfernteUeberschrift(anzahl: Int, offen: Boolean, beiKlick: () -> Unit) {
    val farben = LocalKompassFarben.current
    Column(modifier = Modifier.padding(top = Mass.rand)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(farben.flaecheErhoeht, RoundedCornerShape(Mass.radius))
                .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radius))
                .clickable(onClick = beiKlick)
                .padding(Mass.abstand),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Entfernte Einträge ($anzahl)",
                    style = MaterialTheme.typography.titleSmall,
                    color = farben.kupfer,
                )
                Text(
                    text = "Was es einmal gab und was heute an seiner Stelle steht",
                    style = MaterialTheme.typography.bodySmall,
                    color = farben.textGedaempft,
                )
            }
            Icon(
                imageVector = if (offen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (offen) "Zuklappen" else "Aufklappen",
                tint = farben.kupfer,
            )
        }
    }
}

/**
 * Eine Karte für einen Eintrag.
 *
 * Aufgeklappt zeigt sie die vollständige Erklärung und darunter die vier Knöpfe: vorlesen,
 * fragen, ausführlicher erklären, zurück. Der Zurück-Knopf erscheint erst, wenn es etwas
 * zurückzunehmen gibt — ein toter Knopf verwirrt mehr, als er nützt.
 */
@Composable
private fun EintragKarte(
    listenEintrag: ListenEintrag,
    ausgeklappt: Boolean,
    fragenOffen: Boolean,
    arbeitet: Boolean,
    spricht: Boolean,
    laedtTon: Boolean,
    diktiertHier: Boolean,
    diktatStufe: DiktatStufe,
    antwortSpricht: (Long) -> Boolean,
    viewModel: ReferenzViewModel,
    diktat: DiktatViewModel,
) {
    val eintrag = listenEintrag.eintrag
    val farben = LocalKompassFarben.current
    val neu = listenEintrag.istNeu

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (neu) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(Mass.radius),
            )
            .border(
                width = if (neu) 2.dp else 1.dp,
                color = if (neu) MaterialTheme.colorScheme.primary else farben.rahmen,
                shape = RoundedCornerShape(Mass.radius),
            ),
    ) {
        // --- Kopf: immer sichtbar, tippen klappt auf ------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.schalteAusgeklappt(eintrag.id) }
                .padding(Mass.abstand),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = eintrag.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = if (eintrag.bereich == Bereich.PRAXIS.id) {
                            FontFamily.Default
                        } else {
                            FontFamily.Monospace
                        },
                        color = if (eintrag.entfernt) farben.kupfer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (neu) {
                        Spacer(Modifier.width(Mass.abstandKlein))
                        Merkzeichen(text = "NEU", gefuellt = true)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = eintrag.kurz,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Mass.abstandKlein))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (eintrag.seitVersion.isNotBlank()) {
                        Merkzeichen("seit ${eintrag.seitVersion}")
                    } else {
                        Merkzeichen("Version unbekannt", farbe = farben.textGedaempft)
                    }
                    if (eintrag.entfernt && eintrag.entferntInVersion.isNotBlank()) {
                        Merkzeichen("entfernt in ${eintrag.entferntInVersion}", farbe = farben.kupfer)
                    }
                    if (eintrag.art.isNotBlank()) {
                        Merkzeichen(eintrag.art, farbe = farben.goldGedaempft)
                    }
                }
            }
            Icon(
                imageVector = if (ausgeklappt) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (ausgeklappt) "Zuklappen" else "Aufklappen",
                tint = farben.textGedaempft,
            )
        }

        AnimatedVisibility(visible = ausgeklappt) {
            Column(modifier = Modifier.padding(horizontal = Mass.abstand)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(farben.rahmen),
                )
                Spacer(Modifier.height(Mass.abstand))

                Text(
                    text = eintrag.erklaerung,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (eintrag.entfernt && eintrag.ersatz.isNotBlank()) {
                    Spacer(Modifier.height(Mass.abstand))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                farben.kupfer.copy(alpha = 0.10f),
                                RoundedCornerShape(Mass.radiusKlein),
                            )
                            .border(1.dp, farben.kupfer.copy(alpha = 0.4f), RoundedCornerShape(Mass.radiusKlein))
                            .padding(Mass.abstand),
                    ) {
                        Text(
                            text = "Was an seine Stelle getreten ist",
                            style = MaterialTheme.typography.labelLarge,
                            color = farben.kupfer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = eintrag.ersatz,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (eintrag.quelleEnglisch.isNotBlank()) {
                    Spacer(Modifier.height(Mass.abstand))
                    Text(
                        text = "Offiziell: " + eintrag.quelleEnglisch,
                        style = MaterialTheme.typography.bodySmall,
                        color = farben.textGedaempft,
                    )
                }

                Spacer(Modifier.height(Mass.abstand))
                KnopfLeiste(
                    spricht = spricht,
                    laedtTon = laedtTon,
                    arbeitet = arbeitet,
                    diktiertHier = diktiertHier,
                    diktatStufe = diktatStufe,
                    kannZurueck = listenEintrag.kannZurueck,
                    stufe = eintrag.stufe,
                    beiVorlesen = { viewModel.liesVor(eintrag) },
                    beiMikrofon = {
                        diktat.schalteUm(eintrag.id) { text -> viewModel.stelleFrage(eintrag, text) }
                    },
                    beiVertiefen = { viewModel.vertiefe(eintrag) },
                    beiZurueck = { viewModel.machRueckgaengig(eintrag) },
                )

                if (listenEintrag.fragen.isNotEmpty()) {
                    Spacer(Modifier.height(Mass.abstandKlein))
                    FragenBereich(
                        fragen = listenEintrag.fragen,
                        offen = fragenOffen,
                        beiSchalten = { viewModel.schalteFragenListe(eintrag.id) },
                        beiVorlesen = viewModel::liesAntwortVor,
                        beiLoeschen = viewModel::loescheFrage,
                        spricht = antwortSpricht,
                    )
                }
                Spacer(Modifier.height(Mass.abstand))
            }
        }
    }
}

/**
 * Die Knopfreihe unter jedem Eintrag.
 *
 * Reihenfolge: Lautsprecher, Mikrofon, Ausführlicher, Zurück. Sie ist in allen drei Bereichen
 * gleich, damit man sie überall an derselben Stelle greift.
 */
@Composable
private fun KnopfLeiste(
    spricht: Boolean,
    laedtTon: Boolean,
    arbeitet: Boolean,
    diktiertHier: Boolean,
    diktatStufe: DiktatStufe,
    kannZurueck: Boolean,
    stufe: Int,
    beiVorlesen: () -> Unit,
    beiMikrofon: () -> Unit,
    beiVertiefen: () -> Unit,
    beiZurueck: () -> Unit,
) {
    val farben = LocalKompassFarben.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        WerkzeugKnopf(
            symbol = if (spricht) Icons.Default.Stop else Icons.Default.VolumeUp,
            text = when {
                laedtTon -> "Lädt"
                spricht -> "Stopp"
                else -> "Vorlesen"
            },
            beschreibung = if (spricht) "Vorlesen anhalten" else "Diesen Eintrag vorlesen",
            aktiv = spricht,
            laedt = laedtTon,
            beiKlick = beiVorlesen,
        )
        Spacer(Modifier.width(6.dp))
        WerkzeugKnopf(
            symbol = when {
                diktiertHier && diktatStufe == DiktatStufe.NIMMT_AUF -> Icons.Default.MicOff
                else -> Icons.Default.Mic
            },
            text = when {
                diktiertHier && diktatStufe == DiktatStufe.NIMMT_AUF -> "Fertig"
                diktiertHier && diktatStufe == DiktatStufe.SCHREIBT_AB -> "Schreibt"
                else -> "Fragen"
            },
            beschreibung = "Eine Frage zu diesem Eintrag sprechen",
            aktiv = diktiertHier && diktatStufe == DiktatStufe.NIMMT_AUF,
            laedt = diktiertHier && diktatStufe == DiktatStufe.SCHREIBT_AB,
            beiKlick = beiMikrofon,
        )
        Spacer(Modifier.width(6.dp))
        WerkzeugKnopf(
            symbol = Icons.Default.UnfoldMore,
            text = if (stufe > 0) "Mehr (${stufe + 1})" else "Mehr",
            beschreibung = "Diesen Eintrag ausführlicher erklären lassen",
            aktiv = false,
            laedt = arbeitet,
            beiKlick = beiVertiefen,
        )
        if (kannZurueck) {
            Spacer(Modifier.width(6.dp))
            WerkzeugKnopf(
                symbol = Icons.AutoMirrored.Filled.Undo,
                text = "Zurück",
                beschreibung = "Die vorherige, kürzere Fassung wiederherstellen",
                aktiv = false,
                laedt = false,
                farbe = farben.kupfer,
                beiKlick = beiZurueck,
            )
        }
    }
}

@Composable
private fun WerkzeugKnopf(
    symbol: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    beschreibung: String,
    aktiv: Boolean,
    laedt: Boolean,
    beiKlick: () -> Unit,
    farbe: androidx.compose.ui.graphics.Color? = null,
) {
    val ton = farbe ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .background(
                color = if (aktiv) ton else ton.copy(alpha = 0.10f),
                shape = RoundedCornerShape(Mass.radiusKlein),
            )
            .border(1.dp, ton.copy(alpha = 0.5f), RoundedCornerShape(Mass.radiusKlein))
            .clickable(enabled = !laedt, onClick = beiKlick)
            // Waagerecht knapp, senkrecht grosszügig: So bleiben vier Knöpfe auch auf dem
            // schmalen Cover-Display nebeneinander und behalten trotzdem genug Tippfläche.
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (laedt) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = ton,
            )
        } else {
            Icon(
                imageVector = symbol,
                contentDescription = beschreibung,
                tint = if (aktiv) MaterialTheme.colorScheme.onPrimary else ton,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (aktiv) MaterialTheme.colorScheme.onPrimary else ton,
        )
    }
}

/**
 * Das Klapp-Menü mit den gestellten Fragen.
 *
 * Erst die Fragen, dann auf Tipp die Antwort — so bleibt die Liste überschaubar, auch wenn zu
 * einem Befehl zehn Fragen gespeichert sind.
 */
@Composable
private fun FragenBereich(
    fragen: List<FrageEntity>,
    offen: Boolean,
    beiSchalten: () -> Unit,
    beiVorlesen: (FrageEntity) -> Unit,
    beiLoeschen: (Long) -> Unit,
    spricht: (Long) -> Boolean,
) {
    val farben = LocalKompassFarben.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(farben.flaecheErhoeht, RoundedCornerShape(Mass.radiusKlein))
            .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radiusKlein)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = beiSchalten)
                .padding(Mass.abstand),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.QuestionAnswer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Mass.abstandKlein))
            Text(
                text = "Meine Fragen (${fragen.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (offen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (offen) "Fragen zuklappen" else "Fragen aufklappen",
                tint = farben.textGedaempft,
            )
        }

        AnimatedVisibility(visible = offen) {
            Column(modifier = Modifier.padding(horizontal = Mass.abstand, vertical = 4.dp)) {
                fragen.forEach { frage ->
                    FrageZeile(
                        frage = frage,
                        spricht = spricht(frage.id),
                        beiVorlesen = { beiVorlesen(frage) },
                        beiLoeschen = { beiLoeschen(frage.id) },
                    )
                }
                Spacer(Modifier.height(Mass.abstandKlein))
            }
        }
    }
}

@Composable
private fun FrageZeile(
    frage: FrageEntity,
    spricht: Boolean,
    beiVorlesen: () -> Unit,
    beiLoeschen: () -> Unit,
) {
    val farben = LocalKompassFarben.current
    var offen by rememberSaveable(frage.id) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { offen = !offen },
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = frage.frage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (frage.laeuft) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = if (offen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (offen) "Antwort zuklappen" else "Antwort zeigen",
                    tint = farben.textGedaempft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        AnimatedVisibility(visible = offen || frage.laeuft) {
            Column(modifier = Modifier.padding(top = 5.dp)) {
                when {
                    frage.fehler.isNotBlank() -> Text(
                        text = frage.fehler,
                        style = MaterialTheme.typography.bodySmall,
                        color = farben.fehler,
                    )
                    frage.antwort.isBlank() && frage.laeuft -> Text(
                        text = "Die Antwort wird geschrieben …",
                        style = MaterialTheme.typography.bodySmall,
                        color = farben.textGedaempft,
                    )
                    else -> Text(
                        text = frage.antwort,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (!frage.laeuft && frage.antwort.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WerkzeugKnopf(
                            symbol = if (spricht) Icons.Default.Stop else Icons.Default.VolumeUp,
                            text = if (spricht) "Stopp" else "Vorlesen",
                            beschreibung = "Diese Antwort vorlesen",
                            aktiv = spricht,
                            laedt = false,
                            beiKlick = beiVorlesen,
                        )
                        Spacer(Modifier.width(6.dp))
                        WerkzeugKnopf(
                            symbol = Icons.Default.Delete,
                            text = "Löschen",
                            beschreibung = "Diese Frage und ihre Antwort löschen",
                            aktiv = false,
                            laedt = false,
                            farbe = farben.textGedaempft,
                            beiKlick = beiLoeschen,
                        )
                    }
                }
            }
        }
    }
}

package de.frank.claudekompass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.claudekompass.data.local.ChatNachrichtEntity
import de.frank.claudekompass.ui.components.FehlerStreifen
import de.frank.claudekompass.ui.components.LeerZustand
import de.frank.claudekompass.ui.theme.LocalKompassFarben
import de.frank.claudekompass.ui.theme.Mass
import de.frank.claudekompass.tts.VorleseStufe
import de.frank.claudekompass.vm.ChatViewModel
import de.frank.claudekompass.vm.DiktatStufe
import de.frank.claudekompass.vm.DiktatViewModel

/**
 * Der Gesprächsbereich.
 *
 * Links die Liste der Gespräche mit dem Plus zum Anlegen, rechts das laufende Gespräch. Auf
 * dem schmalen Cover-Display des Fold wird die Liste über den Inhalt gelegt statt daneben
 * gestellt — nebeneinander bliebe für beides zu wenig Platz.
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    diktat: DiktatViewModel,
    breitGenugFuerZweiSpalten: Boolean,
    modifier: Modifier = Modifier,
) {
    val zustand by viewModel.zustand.collectAsStateWithLifecycle()
    val sitzungen by viewModel.sitzungen.collectAsStateWithLifecycle()
    val nachrichten by viewModel.nachrichten.collectAsStateWithLifecycle()
    val vorlesen by viewModel.vorleseZustand.collectAsStateWithLifecycle()
    val diktatZustand by diktat.zustand.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (zustand.fehler.isNotBlank() || vorlesen.fehler.isNotBlank()) {
            FehlerStreifen(
                text = zustand.fehler.ifBlank { vorlesen.fehler },
                beiSchliessen = viewModel::loescheFehler,
                knopfText = "Nochmal versuchen",
                beiKnopf = viewModel::schicke,
            )
        }
        if (diktatZustand.fehler.isNotBlank() || diktatZustand.hinweis.isNotBlank()) {
            FehlerStreifen(
                text = diktatZustand.fehler.ifBlank { diktatZustand.hinweis },
                beiSchliessen = diktat::loescheMeldung,
            )
        }

        if (breitGenugFuerZweiSpalten) {
            Row(modifier = Modifier.fillMaxSize()) {
                SitzungsListe(
                    sitzungen = sitzungen.map { it.id to it.titel },
                    aktive = zustand.aktiveSitzung,
                    beiWahl = viewModel::waehleSitzung,
                    beiNeu = viewModel::legeSitzungAn,
                    beiLoeschen = viewModel::loescheSitzung,
                    modifier = Modifier.width(260.dp).fillMaxSize(),
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxSize()
                        .background(LocalKompassFarben.current.rahmen),
                )
                Gespraech(
                    nachrichten = nachrichten,
                    antwortet = zustand.antwortet,
                    eingabe = zustand.eingabe,
                    diktiert = diktatZustand.stufe.takeIf { diktatZustand.ziel == ZIEL_CHAT },
                    spricht = { id -> vorlesen.quelleId == "chat:$id" && vorlesen.stufe != VorleseStufe.AUS },
                    beiEingabe = viewModel::setzeEingabe,
                    beiSenden = viewModel::schicke,
                    beiAbbrechen = viewModel::brichAntwortAb,
                    beiVorlesen = viewModel::liesNachrichtVor,
                    beiMikrofon = {
                        diktat.schalteUm(ZIEL_CHAT) { text ->
                            viewModel.setzeEingabe((zustand.eingabe + " " + text).trim())
                        }
                    },
                    listeSchalten = null,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Gespraech(
                    nachrichten = nachrichten,
                    antwortet = zustand.antwortet,
                    eingabe = zustand.eingabe,
                    diktiert = diktatZustand.stufe.takeIf { diktatZustand.ziel == ZIEL_CHAT },
                    spricht = { id -> vorlesen.quelleId == "chat:$id" && vorlesen.stufe != VorleseStufe.AUS },
                    beiEingabe = viewModel::setzeEingabe,
                    beiSenden = viewModel::schicke,
                    beiAbbrechen = viewModel::brichAntwortAb,
                    beiVorlesen = viewModel::liesNachrichtVor,
                    beiMikrofon = {
                        diktat.schalteUm(ZIEL_CHAT) { text ->
                            viewModel.setzeEingabe((zustand.eingabe + " " + text).trim())
                        }
                    },
                    listeSchalten = viewModel::schalteListe,
                    modifier = Modifier.fillMaxSize(),
                )
                if (zustand.listeOffen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                            .clickable(onClick = viewModel::schalteListe),
                    )
                    SitzungsListe(
                        sitzungen = sitzungen.map { it.id to it.titel },
                        aktive = zustand.aktiveSitzung,
                        beiWahl = viewModel::waehleSitzung,
                        beiNeu = viewModel::legeSitzungAn,
                        beiLoeschen = viewModel::loescheSitzung,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 56.dp)
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    }
}

private const val ZIEL_CHAT = "__chat__"

@Composable
private fun SitzungsListe(
    sitzungen: List<Pair<Long, String>>,
    aktive: Long,
    beiWahl: (Long) -> Unit,
    beiNeu: () -> Unit,
    beiLoeschen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalKompassFarben.current
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = beiNeu)
                .padding(Mass.abstand),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Neues Gespräch anlegen",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Mass.abstandKlein))
            Text(
                text = "Neues Gespräch",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rahmen))

        if (sitzungen.isEmpty()) {
            LeerZustand(
                symbol = Icons.Default.Chat,
                ueberschrift = "Noch kein Gespräch",
                text = "Leg eines an und frag alles, was du über Claude Code wissen willst. " +
                    "Slash-Befehle und Einstellungen kommen dabei von selbst zur Sprache.",
                knopfText = "Erstes Gespräch anlegen",
                beiKnopf = beiNeu,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(sitzungen, key = { it.first }) { (id, titel) ->
                    val gewaehlt = id == aktive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (gewaehlt) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            )
                            .clickable { beiWahl(id) }
                            .padding(horizontal = Mass.abstand, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = titel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (gewaehlt) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (gewaehlt) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Dieses Gespräch löschen",
                            tint = farben.textGedaempft,
                            modifier = Modifier
                                .size(Mass.tippflaeche)
                                .clickable { beiLoeschen(id) }
                                .padding(13.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Gespraech(
    nachrichten: List<ChatNachrichtEntity>,
    antwortet: Boolean,
    eingabe: String,
    diktiert: DiktatStufe?,
    spricht: (Long) -> Boolean,
    beiEingabe: (String) -> Unit,
    beiSenden: () -> Unit,
    beiAbbrechen: () -> Unit,
    beiVorlesen: (ChatNachrichtEntity) -> Unit,
    beiMikrofon: () -> Unit,
    listeSchalten: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val farben = LocalKompassFarben.current
    val listenZustand = rememberLazyListState()

    // Bei neuer Nachricht ans Ende springen — sonst schreibt die Antwort ausserhalb des Bildes.
    LaunchedEffect(nachrichten.size, nachrichten.lastOrNull()?.text?.length) {
        if (nachrichten.isNotEmpty()) listenZustand.animateScrollToItem(nachrichten.lastIndex)
    }

    Column(modifier = modifier) {
        if (listeSchalten != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = listeSchalten)
                    .padding(horizontal = Mass.abstand, vertical = Mass.abstandKlein),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Gesprächsliste öffnen",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Mass.abstandKlein))
                Text(
                    text = "Gespräche",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rahmen))
        }

        if (nachrichten.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LeerZustand(
                    symbol = Icons.Default.Chat,
                    ueberschrift = "Frag einfach los",
                    text = "Zum Beispiel: Wie halte ich das Gedächtnis in langen Sitzungen " +
                        "sauber? Oder: Welche Einstellung verhindert, dass Claude Zugangsdaten " +
                        "liest? Antworten beziehen Slash-Befehle und Einstellungen mit ein.",
                )
            }
        } else {
            LazyColumn(
                state = listenZustand,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Mass.randSchmal),
                verticalArrangement = Arrangement.spacedBy(Mass.abstandKlein),
            ) {
                items(nachrichten, key = { it.id }) { nachricht ->
                    NachrichtBlase(
                        nachricht = nachricht,
                        spricht = spricht(nachricht.id),
                        beiVorlesen = { beiVorlesen(nachricht) },
                    )
                }
            }
        }

        Eingabezeile(
            text = eingabe,
            antwortet = antwortet,
            diktiert = diktiert,
            beiText = beiEingabe,
            beiSenden = beiSenden,
            beiAbbrechen = beiAbbrechen,
            beiMikrofon = beiMikrofon,
        )
    }
}

@Composable
private fun NachrichtBlase(
    nachricht: ChatNachrichtEntity,
    spricht: Boolean,
    beiVorlesen: () -> Unit,
) {
    val farben = LocalKompassFarben.current
    val vomBenutzer = nachricht.rolle == ChatViewModel.ROLLE_BENUTZER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (vomBenutzer) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .background(
                    color = if (vomBenutzer) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        farben.flaecheErhoeht
                    },
                    shape = RoundedCornerShape(Mass.radius),
                )
                .border(
                    1.dp,
                    if (vomBenutzer) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    } else {
                        farben.rahmen
                    },
                    RoundedCornerShape(Mass.radius),
                )
                .padding(Mass.abstand),
        ) {
            when {
                nachricht.fehler.isNotBlank() -> Text(
                    text = nachricht.fehler,
                    style = MaterialTheme.typography.bodyMedium,
                    color = farben.fehler,
                )
                nachricht.text.isBlank() -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(Mass.abstandKlein))
                    Text(
                        text = "Antwort wird geschrieben …",
                        style = MaterialTheme.typography.bodySmall,
                        color = farben.textGedaempft,
                    )
                }
                else -> Text(
                    text = nachricht.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!vomBenutzer && nachricht.text.isNotBlank()) {
                Spacer(Modifier.height(Mass.abstandKlein))
                Row(
                    modifier = Modifier.clickable(onClick = beiVorlesen),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (spricht) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription = if (spricht) "Vorlesen anhalten" else "Antwort vorlesen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (spricht) "Stopp" else "Vorlesen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Eingabezeile(
    text: String,
    antwortet: Boolean,
    diktiert: DiktatStufe?,
    beiText: (String) -> Unit,
    beiSenden: () -> Unit,
    beiAbbrechen: () -> Unit,
    beiMikrofon: () -> Unit,
) {
    val farben = LocalKompassFarben.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .navigationBarsPadding()
            .padding(Mass.abstandKlein),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp, max = 160.dp)
                    .background(farben.eingabefeld, RoundedCornerShape(Mass.radius))
                    .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radius))
                    .padding(horizontal = Mass.abstand, vertical = 12.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = when (diktiert) {
                            DiktatStufe.NIMMT_AUF -> "Sprich jetzt …"
                            DiktatStufe.SCHREIBT_AB -> "Wird abgeschrieben …"
                            else -> "Frag etwas über Claude Code"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = farben.textGedaempft,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = beiText,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.width(6.dp))
            RundKnopf(
                symbol = if (diktiert == DiktatStufe.NIMMT_AUF) Icons.Default.MicOff else Icons.Default.Mic,
                beschreibung = if (diktiert == DiktatStufe.NIMMT_AUF) {
                    "Aufnahme beenden und abschreiben"
                } else {
                    "Frage sprechen statt tippen"
                },
                aktiv = diktiert == DiktatStufe.NIMMT_AUF,
                laedt = diktiert == DiktatStufe.SCHREIBT_AB,
                beiKlick = beiMikrofon,
            )
            Spacer(Modifier.width(6.dp))
            RundKnopf(
                symbol = if (antwortet) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                beschreibung = if (antwortet) "Antwort abbrechen" else "Frage abschicken",
                aktiv = antwortet,
                laedt = false,
                beiKlick = { if (antwortet) beiAbbrechen() else beiSenden() },
            )
        }
        if (diktiert == DiktatStufe.NIMMT_AUF) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Die Aufnahme läuft. Tipp erneut aufs Mikrofon, wenn du fertig bist.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RundKnopf(
    symbol: androidx.compose.ui.graphics.vector.ImageVector,
    beschreibung: String,
    aktiv: Boolean,
    laedt: Boolean,
    beiKlick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Mass.tippflaeche)
            .background(
                color = if (aktiv) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(Mass.radius),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(Mass.radius),
            )
            .clickable(enabled = !laedt, onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        if (laedt) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = symbol,
                contentDescription = beschreibung,
                tint = if (aktiv) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

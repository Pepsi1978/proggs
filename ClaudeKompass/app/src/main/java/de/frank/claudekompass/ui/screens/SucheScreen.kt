package de.frank.claudekompass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.claudekompass.data.KompassRepository
import de.frank.claudekompass.data.local.SuchTreffer
import de.frank.claudekompass.data.model.Bereich
import de.frank.claudekompass.ui.components.LeerZustand
import de.frank.claudekompass.ui.components.Merkzeichen
import de.frank.claudekompass.ui.theme.LocalKompassFarben
import de.frank.claudekompass.ui.theme.Mass
import de.frank.claudekompass.vm.ReferenzViewModel

/**
 * Die Volltextsuche über alle Inhalte (Referenz, Baustein K).
 *
 * Drei Zustände sind ausdrücklich verschieden: noch nichts getippt (dann steht der Verlauf da),
 * getippt ohne Treffer, und Treffer. „Keine Daten" wäre in allen drei Fällen falsch.
 */
@Composable
fun SucheScreen(
    viewModel: ReferenzViewModel,
    beiTreffer: (Bereich, String) -> Unit,
    beiZurueck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text by viewModel.suchtext.collectAsStateWithLifecycle()
    val treffer by viewModel.treffer.collectAsStateWithLifecycle()
    val laeuft by viewModel.suchtGerade.collectAsStateWithLifecycle()
    val verlauf by viewModel.suchVerlauf.collectAsStateWithLifecycle()
    val farben = LocalKompassFarben.current
    val fokus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { fokus.requestFocus() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Mass.abstandKlein),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Suche schliessen",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(Mass.tippflaeche)
                    .clickable(onClick = beiZurueck)
                    .padding(13.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .background(farben.eingabefeld, RoundedCornerShape(Mass.radius))
                    .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radius))
                    .padding(horizontal = Mass.abstand, vertical = 12.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Befehl, Einstellung, eigene Frage …",
                        style = MaterialTheme.typography.bodyMedium,
                        color = farben.textGedaempft,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = viewModel::setzeSuchtext,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(fokus),
                )
            }
            if (text.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Suche leeren",
                    tint = farben.textGedaempft,
                    modifier = Modifier
                        .size(Mass.tippflaeche)
                        .clickable { viewModel.leereSuche() }
                        .padding(13.dp),
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rahmen))

        when {
            laeuft -> Row(
                modifier = Modifier.fillMaxWidth().padding(Mass.rand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Mass.abstandKlein))
                Text(
                    text = "Wird gesucht …",
                    style = MaterialTheme.typography.bodySmall,
                    color = farben.textGedaempft,
                )
            }

            text.isBlank() -> VerlaufsListe(
                verlauf = verlauf,
                beiWahl = viewModel::setzeSuchtext,
                beiLoeschen = viewModel::loescheSuchAnfrage,
                beiAllesLoeschen = viewModel::leereSuchVerlauf,
            )

            treffer.isEmpty() -> LeerZustand(
                symbol = Icons.Default.SearchOff,
                ueberschrift = "Nichts gefunden für „$text“",
                text = "Die Suche findet Wortanfänge. Probier ein kürzeres Stichwort — " +
                    "„komp“ findet auch „kompaktieren“.",
                knopfText = "Suche leeren",
                beiKnopf = { viewModel.leereSuche() },
            )

            else -> {
                LaunchedEffect(treffer.size) { viewModel.merkeSuche() }
                TrefferListe(treffer = treffer, beiTreffer = beiTreffer)
            }
        }
    }
}

@Composable
private fun VerlaufsListe(
    verlauf: List<String>,
    beiWahl: (String) -> Unit,
    beiLoeschen: (String) -> Unit,
    beiAllesLoeschen: () -> Unit,
) {
    val farben = LocalKompassFarben.current
    if (verlauf.isEmpty()) {
        LeerZustand(
            symbol = Icons.Default.History,
            ueberschrift = "Was suchst du?",
            text = "Gesucht wird über alle Slash-Befehle, alle Einstellungen, die Best " +
                "Practices, deine eigenen Fragen und alle Gespräche. Gross- und " +
                "Kleinschreibung und Umlaute sind dabei egal.",
        )
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Mass.randSchmal, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Zuletzt gesucht",
                style = MaterialTheme.typography.labelLarge,
                color = farben.goldGedaempft,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = beiAllesLoeschen) {
                Text("Alle löschen", color = farben.textGedaempft)
            }
        }
        LazyColumn {
            items(verlauf, key = { it }) { anfrage ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { beiWahl(anfrage) }
                        .padding(horizontal = Mass.randSchmal),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = farben.textGedaempft,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(Mass.abstandKlein))
                    Text(
                        text = anfrage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(vertical = 13.dp),
                    )
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Diese Suche vergessen",
                        tint = farben.textGedaempft,
                        modifier = Modifier
                            .size(Mass.tippflaeche)
                            .clickable { beiLoeschen(anfrage) }
                            .padding(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrefferListe(treffer: List<SuchTreffer>, beiTreffer: (Bereich, String) -> Unit) {
    val farben = LocalKompassFarben.current
    val gruppiert = treffer.groupBy { it.quelleArt }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Mass.randSchmal),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        gruppiert.forEach { (art, liste) ->
            item(key = "kopf-$art") {
                Text(
                    text = when (art) {
                        KompassRepository.ART_EINTRAG -> "Einträge (${liste.size})"
                        KompassRepository.ART_FRAGE -> "Meine Fragen (${liste.size})"
                        else -> "Gespräche (${liste.size})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = farben.goldGedaempft,
                    modifier = Modifier.padding(top = Mass.abstandKlein, bottom = 4.dp),
                )
            }
            items(liste, key = { "${it.quelleArt}-${it.quelleId}" }) { einzel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Mass.radiusKlein))
                        .border(1.dp, farben.rahmen, RoundedCornerShape(Mass.radiusKlein))
                        .clickable {
                            beiTreffer(Bereich.fromId(einzel.bereich), einzel.quelleId)
                        }
                        .padding(Mass.abstand),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = einzel.titel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        fontFamily = if (einzel.bereich == Bereich.SLASH.id ||
                            einzel.bereich == Bereich.CONFIG.id
                        ) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Mass.abstandKlein))
                    Merkzeichen(Bereich.fromId(einzel.bereich).titel)
                }
            }
        }
    }
}

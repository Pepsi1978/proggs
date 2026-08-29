package de.frank.claudekompass.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.claudekompass.BuildConfig
import de.frank.claudekompass.data.model.Bereich
import de.frank.claudekompass.ui.components.FehlerStreifen
import de.frank.claudekompass.ui.components.GoldKnopf
import de.frank.claudekompass.ui.components.HinweisStreifen
import de.frank.claudekompass.ui.components.KompassKopfleiste
import de.frank.claudekompass.ui.components.Trennlinie
import de.frank.claudekompass.ui.screens.ChatScreen
import de.frank.claudekompass.ui.screens.EinstellungenScreen
import de.frank.claudekompass.ui.screens.ReferenzScreen
import de.frank.claudekompass.ui.screens.SucheScreen
import de.frank.claudekompass.ui.theme.LocalKompassFarben
import de.frank.claudekompass.ui.theme.Mass
import de.frank.claudekompass.ui.theme.ThemeModus
import de.frank.claudekompass.vm.ChatViewModel
import de.frank.claudekompass.vm.DiktatViewModel
import de.frank.claudekompass.vm.EinstellungenViewModel
import de.frank.claudekompass.vm.ReferenzViewModel

/**
 * Der Rahmen der App: Kopfleiste, die vier Bereiche unten, und darüber gelegt die Suche, die
 * Einstellungen und der Sperrbildschirm.
 *
 * Die Aufteilung folgt der Referenz (Baustein B): Grundlage ist das schmale Cover-Display des
 * Fold. Ist mehr Platz da — aufgeklappt oder im geteilten Bildschirm —, wird der Chat
 * zweispaltig und die Ränder werden grösser.
 */
@Composable
fun KompassApp(
    referenz: ReferenzViewModel,
    chat: ChatViewModel,
    einstellungen: EinstellungenViewModel,
    diktat: DiktatViewModel,
    gesperrt: Boolean,
    beiEntsperren: () -> Unit,
    beiExport: (String) -> Unit,
    beiImport: () -> Unit,
    beiLogAnsehen: () -> Unit,
    breitGenugFuerZweiSpalten: Boolean,
    themeModus: ThemeModus,
    beiThemeWechsel: () -> Unit,
) {
    var bereich by rememberSaveable { mutableStateOf(Bereich.SLASH.id) }
    var zeigeEinstellungen by rememberSaveable { mutableStateOf(false) }
    var zeigeSuche by rememberSaveable { mutableStateOf(false) }

    val lauf by referenz.lauf.collectAsStateWithLifecycle()
    val letzterErfolg by referenz.letzterErfolg.collectAsStateWithLifecycle()

    if (gesperrt) {
        SperrBildschirm(beiEntsperren = beiEntsperren)
        return
    }

    if (zeigeSuche) {
        SucheScreen(
            viewModel = referenz,
            beiTreffer = { getroffenerBereich, _ ->
                bereich = getroffenerBereich.id
                zeigeSuche = false
            },
            beiZurueck = { zeigeSuche = false },
        )
        return
    }

    if (zeigeEinstellungen) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            UnterseiteKopf(titel = "Einstellungen", beiZurueck = { zeigeEinstellungen = false })
            Trennlinie()
            EinstellungenScreen(
                viewModel = einstellungen,
                beiExport = beiExport,
                beiImport = beiImport,
                beiLogAnsehen = beiLogAnsehen,
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        KompassKopfleiste(
            titel = "Claude Kompass",
            untertitel = letzterErfolg?.cliVersion?.takeIf { it.isNotBlank() }
                ?.let { "Aktualisiert für Version $it" }
                ?: "Stand: Claude Code ${BuildConfig.SEEDED_CLI_VERSION}",
            themeModus = themeModus,
            beiTheme = beiThemeWechsel,
            beiEinstellungen = { zeigeEinstellungen = true },
            beiSuche = { zeigeSuche = true },
            beiAktualisieren = referenz::aktualisiere,
            aktualisierungLaeuft = lauf.laeuft,
        )
        Trennlinie()

        AnimatedVisibility(visible = lauf.laeuft || lauf.fertig || lauf.fehler.isNotBlank()) {
            AktualisierungsStreifen(
                schritt = lauf.schritt,
                laeuft = lauf.laeuft,
                fehler = lauf.fehler,
                neu = lauf.neuAnzahl,
                erledigt = lauf.erledigt,
                gesamt = lauf.gesamt,
                beiAbbrechen = referenz::brichAktualisierungAb,
                beiSchliessen = referenz::loescheLaufMeldung,
                beiNochmal = referenz::aktualisiere,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (Bereich.fromId(bereich)) {
                Bereich.CHAT -> ChatScreen(
                    viewModel = chat,
                    diktat = diktat,
                    breitGenugFuerZweiSpalten = breitGenugFuerZweiSpalten,
                )
                else -> ReferenzScreen(
                    bereich = Bereich.fromId(bereich),
                    viewModel = referenz,
                    diktat = diktat,
                )
            }
        }

        Trennlinie()
        BereichsLeiste(
            aktiv = bereich,
            beiWahl = { neuer ->
                if (neuer != bereich) {
                    referenz.stoppeVorlesen()
                    chat.stoppeVorlesen()
                    bereich = neuer
                }
            },
        )
    }
}

@Composable
private fun UnterseiteKopf(titel: String, beiZurueck: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = Mass.abstandKlein, vertical = Mass.abstandKlein),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoldKnopf(
            symbol = Icons.AutoMirrored.Filled.ArrowBack,
            beschreibung = "Zurück",
            beiKlick = beiZurueck,
        )
        Spacer(Modifier.width(Mass.abstandKlein))
        Text(
            text = titel,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Die Leiste mit den vier Bereichen.
 *
 * Sie sitzt unten, im Daumenbereich — auf dem hohen Cover-Display des Fold ist das der einzige
 * Ort, den man einhändig erreicht.
 */
@Composable
private fun BereichsLeiste(aktiv: String, beiWahl: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BereichsKnopf(Bereich.SLASH, Icons.Default.Terminal, aktiv, beiWahl)
        BereichsKnopf(Bereich.CONFIG, Icons.Default.Tune, aktiv, beiWahl)
        BereichsKnopf(Bereich.PRAXIS, Icons.Default.Lightbulb, aktiv, beiWahl)
        BereichsKnopf(Bereich.CHAT, Icons.Default.Chat, aktiv, beiWahl)
    }
}

@Composable
private fun BereichsKnopf(
    bereich: Bereich,
    symbol: ImageVector,
    aktiv: String,
    beiWahl: (String) -> Unit,
) {
    val farben = LocalKompassFarben.current
    val gewaehlt = aktiv == bereich.id
    Column(
        modifier = Modifier
            .clickable { beiWahl(bereich.id) }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (gewaehlt) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 13.dp, vertical = 5.dp),
        ) {
            Icon(
                imageVector = symbol,
                contentDescription = bereich.titel,
                tint = if (gewaehlt) MaterialTheme.colorScheme.primary else farben.textGedaempft,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = bereich.titel,
            style = MaterialTheme.typography.labelSmall,
            color = if (gewaehlt) MaterialTheme.colorScheme.primary else farben.textGedaempft,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Der Streifen, der den Aktualisierungslauf begleitet.
 *
 * Er zeigt den laufenden Schritt im Klartext. Ohne diese Angabe sähe man nur einen wandernden
 * Kreis und wüsste nicht, ob noch etwas passiert — bei einem Lauf, der mehrere Minuten dauern
 * kann, ist das der Unterschied zwischen „arbeitet" und „hängt".
 */
@Composable
private fun AktualisierungsStreifen(
    schritt: String,
    laeuft: Boolean,
    fehler: String,
    neu: Int,
    erledigt: Int,
    gesamt: Int,
    beiAbbrechen: () -> Unit,
    beiSchliessen: () -> Unit,
    beiNochmal: () -> Unit,
) {
    if (fehler.isNotBlank()) {
        FehlerStreifen(
            text = fehler,
            beiSchliessen = beiSchliessen,
            knopfText = "Nochmal versuchen",
            beiKnopf = beiNochmal,
        )
        return
    }
    if (!laeuft) {
        val text = if (neu > 0) {
            "$schritt Die neuen Einträge sind golden umrandet — bis zur nächsten Aktualisierung."
        } else {
            schritt
        }
        HinweisStreifen(text = text, beiSchliessen = beiSchliessen)
        return
    }

    val farben = LocalKompassFarben.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Mass.randSchmal, vertical = Mass.abstandKlein)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                RoundedCornerShape(Mass.radiusKlein),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                RoundedCornerShape(Mass.radiusKlein),
            )
            .padding(horizontal = Mass.abstand, vertical = Mass.abstandKlein),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(Mass.abstandKlein))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = schritt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (gesamt > 0) {
                Text(
                    text = "$erledigt von $gesamt",
                    style = MaterialTheme.typography.labelSmall,
                    color = farben.textGedaempft,
                )
            }
        }
        Text(
            text = "Abbrechen",
            style = MaterialTheme.typography.labelMedium,
            color = farben.textGedaempft,
            modifier = Modifier
                .clickable(onClick = beiAbbrechen)
                .padding(Mass.abstandKlein),
        )
    }
}

/**
 * Der Sperrbildschirm (Referenz, Baustein I).
 *
 * Er zeigt keinerlei Inhalt — kein Durchblitzen von Gesprächen oder Fragen. Die Abfrage wird
 * beim Erscheinen automatisch gestartet, damit man nicht erst einen Knopf suchen muss.
 */
@Composable
private fun SperrBildschirm(beiEntsperren: () -> Unit) {
    LaunchedEffect(Unit) { beiEntsperren() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(46.dp),
        )
        Spacer(Modifier.height(Mass.abstand))
        Text(
            text = "Claude Kompass ist gesperrt",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Mass.abstandKlein))
        Text(
            text = "Entsperr mit Fingerabdruck, Gesicht oder dem Gerätecode.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalKompassFarben.current.textGedaempft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Mass.rand),
        )
        Spacer(Modifier.height(Mass.rand))
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(Mass.radiusKlein),
                )
                .clickable(onClick = beiEntsperren)
                .padding(horizontal = Mass.rand, vertical = 12.dp),
        ) {
            Text(
                text = "Jetzt entsperren",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

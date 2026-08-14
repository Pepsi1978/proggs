package de.frank.stacklabor.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.daten.StackRepository
import de.frank.stacklabor.logik.Ampel
import de.frank.stacklabor.ui.bausteine.AmpelBalken
import de.frank.stacklabor.ui.bausteine.Chip
import de.frank.stacklabor.ui.bausteine.Karte
import de.frank.stacklabor.ui.bausteine.PlusKnopf
import de.frank.stacklabor.ui.bausteine.SymbolKnopf
import de.frank.stacklabor.ui.bausteine.Zaehlpunkte
import de.frank.stacklabor.ui.blaetter.StackBearbeitenBlatt
import de.frank.stacklabor.ui.bausteine.glasflaeche
import de.frank.stacklabor.ui.bausteine.wandernderVerlauf
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.launch

/**
 * **B-01 — Hauptbildschirm.** Der Startbildschirm.
 *
 * Aufbau exakt aus `messung/hell/B-01.json`:
 * Kopfbereich 97 dp mit wanderndem Verlauf · Titelzeile 52 dp · Dosis-Zeile 26 dp ·
 * Leiste „Alle Stacks zusammen prüfen" 49 dp · Stack-Karten 273 × 78 dp mit 8 dp Abstand,
 * unten 76 dp frei für den Plus-Knopf · Plus-Knopf 57 dp, 16 dp vom Rand.
 */
@Composable
fun HauptBildschirm(
    app: StackLaborApp,
    steuerung: NavHostController,
    aufStackAnlegen: () -> Unit = {},
) {
    val bereich = rememberCoroutineScope()
    val karten by app.repository.stackKarten.collectAsStateWithLifecycle(initialValue = emptyList())
    val dunkel by app.einstellungen.dunkel.collectAsStateWithLifecycle(initialValue = false)
    val variante by app.einstellungen.dosisVariante.collectAsStateWithLifecycle(initialValue = "A")
    val angemeldet by app.codexAnmeldung.istAngemeldet.collectAsStateWithLifecycle(initialValue = false)
    val varianteText by app.repository.dosisVariantenText
        .collectAsStateWithLifecycle(initialValue = "")
    val laufZustaende by app.auswertung.zustand.collectAsStateWithLifecycle(initialValue = emptyMap())
    var neuerStack by remember { mutableStateOf(false) }

    val obenInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Nur die drei ranghöchsten roten Ampeln bekommen eine Aura (M-21) — sonst wird
    // die Liste unruhig und Rot verliert seine Warnwirkung.
    val mitAura = karten.filter { it.sammelampel == Ampel.ROT }.take(3).map { it.stack.id }.toSet()

    Box(Modifier.fillMaxSize().background(Farben.grund)) {
        Column(Modifier.fillMaxSize()) {

            // ── Kopfbereich, 97 dp ──────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Farben.erhoeht)
                    .wandernderVerlauf()
                    .padding(top = obenInset),
            ) {
                Column(Modifier.fillMaxWidth().height(Masse.kopfB01)) {
                    // Titelzeile 52 dp — Innenabstand 6 / 12 / 0 / 16 wie gemessen
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(Masse.titelzeileB01)
                            .padding(start = 16.dp, end = 12.dp, top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("StackLabor", style = Schrift.titelGross, color = Farben.text)
                        Spacer(Modifier.weight(1f))
                        SymbolKnopf(
                            symbol = if (dunkel) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            beschreibung = if (dunkel) "Auf helle Darstellung wechseln" else "Auf dunkle Darstellung wechseln",
                            aufKlick = { bereich.launch { app.einstellungen.setzeDunkel(!dunkel) } },
                        )
                        SymbolKnopf(
                            symbol = Icons.Rounded.TrackChanges,
                            beschreibung = "Ziel-Katalog",
                            aufKlick = { steuerung.navigate(Ziele.ZIELKATALOG) },
                        )
                        SymbolKnopf(
                            symbol = Icons.Rounded.Settings,
                            beschreibung = "Einstellungen",
                            aufKlick = { steuerung.navigate(Ziele.EINSTELLUNGEN) },
                        )
                    }

                    // Dosis-Zeile 26 dp — F-27, Abstand 6 dp, Innenabstand 0 / 16
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(Masse.dosisZeileB01)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Dosis", style = Schrift.klein, color = Farben.textSchwach)
                        Chip("Frei", variante == "A") {
                            bereich.launch { app.einstellungen.setzeDosisVariante("A") }
                        }
                        Chip("Dienst", variante == "B") {
                            bereich.launch { app.einstellungen.setzeDosisVariante("B") }
                        }
                        // Der Klartext dazu — im Entwurf steht rechts, welches Mittel die
                        // Variante betrifft. Bei Frank ist das bisher nur Venlafaxin.
                        if (varianteText.isNotBlank()) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                varianteText,
                                style = Schrift.klein,
                                color = Farben.textSchwach,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // ── Leiste „Alle Stacks zusammen prüfen", 49 dp ─────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(Masse.leisteGesamt)
                    .background(Farben.flaeche)
                    .clickable {
                        if (angemeldet) steuerung.navigate(Ziele.GESAMT)
                        else steuerung.navigate(Ziele.ANMELDUNG)
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (angemeldet) Farben.akzent else Farben.grau,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    if (angemeldet) "Alle Stacks zusammen prüfen" else "Anmelden",
                    style = Schrift.grundMittel,
                    color = Farben.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Farben.textSchwach,
                    modifier = Modifier.size(18.dp),
                )
            }

            // ── Stack-Karten ────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = Masse.randSeitlich, end = Masse.randSeitlich,
                    top = 8.dp, bottom = 76.dp + untenInset,
                ),
                verticalArrangement = Arrangement.spacedBy(Masse.kartenAbstand),
            ) {
                if (karten.isEmpty()) {
                    item {
                        Text(
                            "Noch kein Stack. Tippe auf das Plus, um deinen ersten anzulegen.",
                            style = Schrift.grund,
                            color = Farben.textSchwach,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(karten, key = { it.stack.id }) { karte ->
                    StackKarte(
                        karte = karte,
                        mitAura = karte.stack.id in mitAura,
                        aufKlick = { steuerung.navigate(Ziele.stack(karte.stack.id)) },
                    )
                }
            }
        }

        // ── Plus-Knopf, schwebend ───────────────────────────────────────────────
        PlusKnopf(
            beschreibung = "Neuen Stack anlegen",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Masse.plusRand, bottom = Masse.plusRand + untenInset),
        ) { neuerStack = true }

        // B-13 — neuen Stack anlegen (F-01)
        if (neuerStack) StackBearbeitenBlatt(app, null) { neuerStack = false }
    }
}

/**
 * Eine Stack-Karte: 273 × 78 dp, Radius 12, Rand 1 dp, Schatten — mit dem Ampel-Kantenbalken
 * links über die volle Höhe und den vier Zählpunkten unten.
 */
@Composable
private fun StackKarte(
    karte: StackRepository.StackKarte,
    mitAura: Boolean,
    aufKlick: () -> Unit,
) {
    val veraltet = karte.stand == StackRepository.Bewertungsstand.VERALTET
    Karte(
        modifier = Modifier
            .fillMaxWidth()
            .height(Masse.stackKarte)
            .clickable(onClick = aufKlick),
    ) {
        Row(Modifier.fillMaxSize()) {
            AmpelBalken(karte.sammelampel, mitAura = mitAura)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        karte.stack.name,
                        style = Schrift.stackName,
                        color = Farben.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (veraltet) {
                        Spacer(Modifier.width(8.dp))
                        VeraltetMarke()
                    }
                }
                Text(
                    listOf(karte.stack.zeitpunkt, karte.stack.einnahmeHinweis)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    style = Schrift.klein,
                    color = Farben.textSchwach,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Zaehlpunkte(
                        karte.zaehlung.gruen, karte.zaehlung.gelb,
                        karte.zaehlung.rot, karte.zaehlung.grau,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${karte.anzahlMittel} Mittel",
                        style = Schrift.klein,
                        color = Farben.textSchwach,
                    )
                }
            }
        }
    }
}

/** Die gestrichelte „veraltet"-Marke — F-23. */
@Composable
private fun VeraltetMarke() {
    Box(
        Modifier
            .clip(Formen.chip)
            .border(1.dp, Farben.gelbText, Formen.chip)
            .padding(horizontal = 8.dp, vertical = 1.dp),
    ) {
        Text("veraltet", style = Schrift.winzig, color = Farben.gelbText)
    }
}

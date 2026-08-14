package de.frank.stacklabor.ui.bausteine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.daten.StackRepository
import de.frank.stacklabor.daten.entitaeten.Loeslichkeit as LoeslichkeitE
import de.frank.stacklabor.logik.Ampel
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift

/**
 * **Der Mittel-Eintrag** — das am häufigsten sichtbare Bauteil der App.
 *
 * Gemessen (`02-UI-SPEC.md` §6a): Karte 273 × 56 dp, Radius 12 dp, Fläche `--flaeche`,
 * Rand 1 dp, Schatten `0 2px 6px rgba(15,23,42,.10)`; darunter 1 dp Trenner → 57 dp Takt.
 *
 * Er ist **zweizeilig**, und das ist keine Geschmacksfrage: Einzeilig blieben für den Namen
 * rechnerisch 1 dp übrig. Zeile 1 trägt Löslichkeitspunkt, Name und das Häkchen;
 * Zeile 2 die Dosis, die Frequenz und den Kurzgrund der Ampel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MittelEintrag(
    zeile: StackRepository.MittelZeile,
    mitAura: Boolean,
    aufHaekchen: () -> Unit,
    aufKlick: () -> Unit,
    aufLangesDruecken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aktiv = zeile.eintrag.aktiv
    val loeslichkeit = when (zeile.mittel.loeslichkeit) {
        LoeslichkeitE.WASSER -> Loeslichkeit.WASSER
        LoeslichkeitE.FETT -> Loeslichkeit.FETT
        LoeslichkeitE.BEIDES -> Loeslichkeit.BEIDES
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(Masse.mittelEintrag)
            .clip(Formen.karte)
            // Ein abgeschalteter Eintrag liegt auf dem Grund statt auf einer erhöhten Fläche —
            // er bleibt sichtbar, tritt aber deutlich zurück.
            .background(if (aktiv) Farben.flaeche else Farben.grund)
            .border(1.dp, Farben.rand, Formen.karte)
            .combinedClickable(onClick = aufKlick, onLongClick = aufLangesDruecken),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {

            // Der Ampel-Kantenbalken, 3 dp, über die volle Höhe.
            AmpelBalken(
                ampel = if (aktiv) zeile.ampel else Ampel.GRAU,
                mitAura = mitAura && aktiv,
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 10.dp, top = 8.dp, bottom = 8.dp)
                    .ausgegraut(aktiv),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
            ) {
                // ── Zeile 1: Löslichkeit + Name ─────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoeslichkeitsPunkte(loeslichkeit)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = kuerzeName(zeile.mittel.name),
                        style = Schrift.name,
                        color = Farben.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // ── Zeile 2: Dosis · Form/Frequenz … Kurzgrund ──────────────────
                Row(
                    Modifier.fillMaxWidth().padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = listOf(zeile.dosisText, zeile.metaText)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        style = Schrift.klein,
                        color = Farben.textSchwach,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (aktiv && zeile.gestoerteZiele.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "stört " + zeile.gestoerteZiele.joinToString(", "),
                            style = Schrift.klein,
                            color = ampelTextFarbe(zeile.ampel),
                            maxLines = 1,
                        )
                    }
                }
            }

            Haekchen(gesetzt = aktiv, aufKlick = aufHaekchen)
            Spacer(Modifier.width(8.dp))
        }
    }
}

/**
 * Lange Namen: erst den Klammerzusatz weglassen, dann erst kürzen — **nie** im Wortstamm brechen.
 *
 * Nachgeprüft an den längsten Namen des Bestands: „PEA (Palmitoylethanolamid)" ≈ 177 dp,
 * „Acetyl-L-Carnitin (ALCAR)" ≈ 163 dp — beide passen in die verfügbaren 183 dp. Die Regel
 * greift also selten, aber wenn, dann fällt zuerst das Entbehrliche weg.
 */
private fun kuerzeName(name: String): String =
    if (name.length > 26 && name.contains(" (")) name.substringBefore(" (") else name

/** Die Klammerlinie einer Kombi-Gruppe: „zusammen einnehmen" (F-28). */
@Composable
fun GruppenKopf(
    anzahl: Int,
    alleAktiv: Boolean,
    teilweiseAktiv: Boolean,
    aufHaekchen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(32.dp).padding(start = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(2.dp).height(24.dp).background(Farben.akzent))
        Spacer(Modifier.width(8.dp))
        Text(
            "zusammen einnehmen",
            style = Schrift.klein,
            color = Farben.textSchwach,
            modifier = Modifier.weight(1f),
        )
        Haekchen(
            gesetzt = alleAktiv,
            aufKlick = aufHaekchen,
            beschreibung = when {
                alleAktiv -> "Gruppe abschalten"
                teilweiseAktiv -> "Gruppe teilweise aktiv, alle einschalten"
                else -> "Gruppe einschalten"
            },
        )
    }
}

package de.frank.stacklabor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.daten.StackRepository
import de.frank.stacklabor.ui.bausteine.AmpelBalken
import de.frank.stacklabor.ui.bausteine.LaufendeZiffer
import de.frank.stacklabor.ui.bausteine.Trenner
import de.frank.stacklabor.ui.bausteine.ZiehZustand
import de.frank.stacklabor.ui.bausteine.rememberZiehZustand
import de.frank.stacklabor.ui.bausteine.ziehGriff
import de.frank.stacklabor.ui.bausteine.ziehbar
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.launch

/**
 * **B-12 — Ziele ordnen (Vollbild).**
 *
 * Aufbau gemessen (`02-UI-SPEC.md` §6): Kopfleiste 57 dp, darunter die Liste ueber die
 * volle verbleibende Hoehe. **Kein Sockel** — die Ablageflaeche soll so gross wie
 * moeglich sein, denn hier wird gezogen und nicht getippt. Je Zeile 273 × 40 dp.
 *
 * Die Reihenfolge wird erst beim Einrasten (M-04) in die Datenbank geschrieben; die
 * Nummern laufen aber schon waehrend des Ziehens mit (M-03).
 */
@Composable
fun ZieleOrdnenBildschirm(app: StackLaborApp, steuerung: NavHostController, stackId: String) {
    val bereich = rememberCoroutineScope()
    val stacks by app.repository.alleStacks.collectAsStateWithLifecycle(initialValue = emptyList())
    val zeilen by app.repository.zielZeilen(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val stackName = stacks.firstOrNull { it.id == stackId }?.name.orEmpty()

    // Die angezeigte Ordnung liegt als reine ID-Liste vor. Sie greift sofort, wenn eine
    // Zeile abgelegt wird — die Datenbank zieht danach nach. Ohne das spraenge die Zeile
    // fuer einen Wimpernschlag an ihre alte Stelle zurueck, bis der Flow antwortet.
    var ordnungIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val ordnung: List<StackRepository.ZielZeile> = remember(zeilen, ordnungIds) {
        val nachId = zeilen.associateBy { it.ziel.id }
        val sortiert = ordnungIds.mapNotNull { nachId[it] }
        // Kam oder ging ein Ziel, entscheidet wieder die Datenbank.
        if (sortiert.size == zeilen.size) sortiert else zeilen
    }
    LaunchedEffect(zeilen) {
        val ids = zeilen.map { it.ziel.id }
        if (ids.toSet() != ordnungIds.toSet()) ordnungIds = ids
    }

    val listenZustand = rememberLazyListState()
    val ziehbarkeit = ordnung.size >= 2
    val zustand = rememberZiehZustand(
        listenZustand = listenZustand,
        aufAbgelegt = { reihenfolge ->
            val neu = reihenfolge.mapNotNull { ordnung.getOrNull(it) }.map { it.ziel.id }
            if (neu.size == ordnung.size) {
                ordnungIds = neu
                bereich.launch { app.repository.setzeZielRaenge(stackId, neu) }
            }
        },
    )

    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize().background(Farben.grund)) {
        Kopfleiste(if (stackName.isEmpty()) "Ziele ordnen" else "Ziele ordnen — $stackName", steuerung)

        if (ordnung.isEmpty()) {
            Text(
                "Diesem Stack ist noch kein Ziel zugeordnet.",
                style = Schrift.grund,
                color = Farben.textSchwach,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
            return@Column
        }

        if (!ziehbarkeit) {
            Text(
                "Zum Ordnen braucht es mindestens zwei Ziele.",
                style = Schrift.klein,
                color = Farben.textSchwach,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Masse.randSeitlich, vertical = 8.dp),
            )
        }

        LazyColumn(
            state = listenZustand,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                start = Masse.randSeitlich,
                end = Masse.randSeitlich,
                bottom = untenInset,
            ),
        ) {
            itemsIndexed(ordnung, key = { _, z -> z.ziel.id }) { index, zeile ->
                ZielOrdnenZeile(
                    zeile = zeile,
                    index = index,
                    anzahl = ordnung.size,
                    zustand = zustand,
                    ziehbarkeit = ziehbarkeit,
                )
            }
        }
    }
}

/** Eine Zeile der Ordnen-Liste: 273 × 40 dp, gemessen. */
@Composable
private fun ZielOrdnenZeile(
    zeile: StackRepository.ZielZeile,
    index: Int,
    anzahl: Int,
    zustand: ZiehZustand,
    ziehbarkeit: Boolean,
) {
    val gezogen = zustand.gezogenerIndex == index
    val nummer = zustand.nummerFuer(index)

    Box(
        Modifier
            .fillMaxWidth()
            .height(Masse.zielEintrag)
            .ziehbar(zustand, index, zeile.ziel.id)
            .background(if (gezogen) Farben.erhoeht else Farben.flaeche)
            .then(if (ziehbarkeit) Modifier.ziehGriff(zustand, index) else Modifier),
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AmpelBalken(zeile.ampel)
            Spacer(Modifier.width(10.dp))

            // M-03: Der Kreis steht still, nur die Ziffer laeuft mit.
            Box(
                Modifier
                    .size(Masse.nummernKreis)
                    .clip(CircleShape)
                    .background(Farben.flaeche)
                    .border(1.dp, Farben.rand, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                LaufendeZiffer(nummer, Schrift.winzigFett, Farben.textSchwach)
            }

            Spacer(Modifier.width(8.dp))
            Text(
                zeile.ziel.text,
                style = Schrift.grund,
                color = Farben.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // Der Griff ist 44 dp breit und nimmt die volle Zeilenhoehe ein — die Zeile
            // bleibt damit bei den gemessenen 40 dp, die Flaeche zum Anfassen ist gross.
            Box(
                Modifier
                    .width(Masse.tippflaeche)
                    .fillMaxHeight()
                    .semantics {
                        contentDescription = if (ziehbarkeit) {
                            "Ziehen zum Umsortieren, Position $nummer von $anzahl"
                        } else {
                            "Umsortieren erst ab zwei Zielen möglich"
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.DragIndicator,
                    contentDescription = null,
                    tint = if (ziehbarkeit) Farben.textSchwach else Farben.deaktiviert,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        // Der Trenner liegt INNERHALB der 40 dp — die gemessene Zeilenhoehe bleibt exakt,
        // und damit stimmt auch die Strecke, um die die Nachbarn ausweichen (M-02).
        Trenner(Modifier.fillMaxWidth().align(Alignment.BottomCenter))
    }
}

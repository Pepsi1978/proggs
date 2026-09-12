package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import kotlinx.coroutines.delay
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion

enum class Bildschirm { LISTE, ERFASSEN, DETAIL, EINSTELLUNGEN, DIAGNOSE, STIMME }

@Composable
fun GenialeIdeenApp(
    viewModel: IdeenViewModel,
    mikrofonErlaubt: Boolean,
    aufMikrofonFragen: () -> Unit,
    aufAnmelden: () -> Unit,
    aufOrdnerWaehlen: () -> Unit,
    aufAppSperreUmschalten: (Boolean) -> Unit,
    aufSeiteOeffnen: (String?) -> Unit,
) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val vorlese by viewModel.vorleseStand.collectAsState()
    val meldung by viewModel.meldung.collectAsState()

    var bildschirm by remember { mutableStateOf(Bildschirm.LISTE) }
    // The idea tapped in the list: the detail screen shows it immediately.
    var geoeffnet by remember { mutableStateOf<IdeeEntity?>(null) }

    // Close the idea only once the exit transition is over. Clearing it immediately swapped
    // the sliding-out screen to a loading skeleton and an empty chat, which stuttered.
    LaunchedEffect(bildschirm) {
        if (bildschirm != Bildschirm.DETAIL && geoeffnet != null) {
            delay(Motion.BILDSCHIRM_MS.toLong())
            viewModel.oeffne(null)
            geoeffnet = null
        }
    }

    // Ein Vorlese-Fehler ist ein echter Fehler und darf nicht still bleiben (Baustein L).
    LaunchedEffect(vorlese.fehler) {
        vorlese.fehler?.let { text ->
            viewModel.zeige(Meldung(text, istFehler = true, zuEinstellungen = true))
            viewModel.vorlesenFehlerGelesen()
        }
    }

    BackHandler(enabled = bildschirm != Bildschirm.LISTE) {
        bildschirm = when (bildschirm) {
            Bildschirm.DIAGNOSE, Bildschirm.STIMME -> Bildschirm.EINSTELLUNGEN
            else -> Bildschirm.LISTE
        }
    }

    Column(Modifier.fillMaxSize().background(gold.hintergrund)) {
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = bildschirm,
            transitionSpec = {
                if (reduziert) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    val vorwaerts = targetState.ordinal > initialState.ordinal
                    (
                        slideInHorizontally(tween(Motion.BILDSCHIRM_MS)) { breite ->
                            if (vorwaerts) breite / 4 else -breite / 4
                        } + fadeIn(tween(Motion.BILDSCHIRM_MS))
                        ) togetherWith (
                        slideOutHorizontally(tween(Motion.BILDSCHIRM_MS)) { breite ->
                            if (vorwaerts) -breite / 6 else breite / 6
                        } + fadeOut(tween(Motion.BILDSCHIRM_MS))
                        )
                }
            },
            label = "bildschirm",
        ) { ziel ->
            when (ziel) {
                Bildschirm.LISTE -> ListenScreen(
                    viewModel = viewModel,
                    aufIdee = { idee ->
                        // Ein Entwurf wird weitergeschrieben, keine fertige Idee besprochen.
                        if (idee.status == IdeenStatus.ENTWURF.name) {
                            viewModel.oeffneEntwurf(idee)
                            bildschirm = Bildschirm.ERFASSEN
                        } else {
                            geoeffnet = idee
                            viewModel.oeffne(idee.id)
                            bildschirm = Bildschirm.DETAIL
                        }
                    },
                    aufNeueIdee = {
                        viewModel.beginneNeueIdee()
                        bildschirm = Bildschirm.ERFASSEN
                    },
                    aufEinstellungen = { bildschirm = Bildschirm.EINSTELLUNGEN },
                )
                Bildschirm.ERFASSEN -> ErfassenScreen(
                    viewModel = viewModel,
                    mikrofonErlaubt = mikrofonErlaubt,
                    aufMikrofonFragen = aufMikrofonFragen,
                    aufZurueck = { bildschirm = Bildschirm.LISTE },
                )
                Bildschirm.DETAIL -> DetailScreen(
                    viewModel = viewModel,
                    vorschau = geoeffnet,
                    aufZurueck = { bildschirm = Bildschirm.LISTE },
                )
                Bildschirm.EINSTELLUNGEN -> EinstellungenScreen(
                    viewModel = viewModel,
                    aufZurueck = { bildschirm = Bildschirm.LISTE },
                    aufDiagnose = { bildschirm = Bildschirm.DIAGNOSE },
                    aufEigeneStimme = { bildschirm = Bildschirm.STIMME },
                    aufAnmelden = aufAnmelden,
                    aufOrdnerWaehlen = aufOrdnerWaehlen,
                    aufAppSperreUmschalten = aufAppSperreUmschalten,
                    aufSeiteOeffnen = aufSeiteOeffnen,
                )
                Bildschirm.DIAGNOSE -> DiagnoseScreen(
                    viewModel = viewModel,
                    aufZurueck = { bildschirm = Bildschirm.EINSTELLUNGEN },
                )
                Bildschirm.STIMME -> StimmeScreen(
                    viewModel = viewModel,
                    mikrofonErlaubt = mikrofonErlaubt,
                    aufMikrofonFragen = aufMikrofonFragen,
                    aufZurueck = { bildschirm = Bildschirm.EINSTELLUNGEN },
                )
            }
        }
        meldung?.let {
            MeldungsStreifen(
                meldung = it,
                aufSchliessen = viewModel::meldungGelesen,
                aufEinstellungen = { bildschirm = Bildschirm.EINSTELLUNGEN },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

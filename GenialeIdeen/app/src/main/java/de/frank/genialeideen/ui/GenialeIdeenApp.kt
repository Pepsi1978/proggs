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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import de.frank.genialeideen.data.local.IdeenStatus
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
    aufExport: () -> Unit,
    aufImport: () -> Unit,
    aufAppSperreUmschalten: (Boolean) -> Unit,
    aufSeiteOeffnen: (String?) -> Unit,
) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val meldung by viewModel.meldung.collectAsState()
    val vorlese by viewModel.vorleseStand.collectAsState()
    val abgestuerzt by viewModel.abgestuerzt.collectAsState()

    var bildschirm by remember { mutableStateOf(Bildschirm.LISTE) }

    // Ein Vorlese-Fehler ist ein echter Fehler und darf nicht still bleiben (Baustein L).
    LaunchedEffect(vorlese.fehler) {
        vorlese.fehler?.let { text ->
            viewModel.zeige(Meldung(text, istFehler = true, zuEinstellungen = true))
            viewModel.vorlesenFehlerGelesen()
        }
    }

    LaunchedEffect(abgestuerzt) {
        if (abgestuerzt) {
            viewModel.zeige(
                Meldung(
                    "Die App ist beim letzten Mal abgestürzt. Der Bericht steht in der Diagnose.",
                    istFehler = true,
                ),
            )
        }
    }

    BackHandler(enabled = bildschirm != Bildschirm.LISTE) {
        bildschirm = when (bildschirm) {
            Bildschirm.DIAGNOSE, Bildschirm.STIMME -> Bildschirm.EINSTELLUNGEN
            else -> Bildschirm.LISTE
        }
    }

    Box(Modifier.fillMaxSize().background(gold.hintergrund)) {
        AnimatedContent(
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
                    aufEinstellungen = { bildschirm = Bildschirm.EINSTELLUNGEN },
                )
                Bildschirm.DETAIL -> DetailScreen(
                    viewModel = viewModel,
                    aufZurueck = {
                        viewModel.oeffne(null)
                        bildschirm = Bildschirm.LISTE
                    },
                    aufEinstellungen = { bildschirm = Bildschirm.EINSTELLUNGEN },
                )
                Bildschirm.EINSTELLUNGEN -> EinstellungenScreen(
                    viewModel = viewModel,
                    aufZurueck = { bildschirm = Bildschirm.LISTE },
                    aufDiagnose = { bildschirm = Bildschirm.DIAGNOSE },
                    aufEigeneStimme = { bildschirm = Bildschirm.STIMME },
                    aufAnmelden = aufAnmelden,
                    aufExport = aufExport,
                    aufImport = aufImport,
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

        meldung?.let { aktuelle ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) {
                MeldungsStreifen(
                    meldung = aktuelle,
                    aufSchliessen = viewModel::meldungGelesen,
                    aufEinstellungen = { bildschirm = Bildschirm.EINSTELLUNGEN },
                )
            }
        }
    }
}

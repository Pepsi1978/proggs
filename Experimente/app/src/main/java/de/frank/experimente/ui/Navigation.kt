package de.frank.experimente.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.frank.experimente.ui.components.LeistenZiel
import de.frank.experimente.ui.components.UntereLeiste
import de.frank.experimente.ui.screens.AuswertungBildschirm
import de.frank.experimente.ui.screens.EinstellungenBildschirm
import de.frank.experimente.ui.screens.ErkenntnisseBildschirm
import de.frank.experimente.ui.screens.GespraechBildschirm
import de.frank.experimente.ui.screens.HeuteBildschirm
import de.frank.experimente.ui.screens.LogbuchBildschirm
import de.frank.experimente.ui.screens.MerklisteBildschirm
import de.frank.experimente.ui.screens.SelbstbildBildschirm
import de.frank.experimente.ui.screens.ZieleBildschirm
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalAppColors

/**
 * Die fünf Hauptbildschirme in der Reihenfolge der unteren Leiste (F-27).
 * Genau diese Reihenfolge gilt auch für die Wisch-Navigation.
 */
private val HAUPTZIELE = listOf(
    LeistenZiel("heute", "Heute", Icons.Outlined.CalendarToday),
    LeistenZiel("ziele", "Ziele", Icons.Outlined.Flag),
    LeistenZiel("merkliste", "Merkliste", Icons.Outlined.BookmarkBorder),
    LeistenZiel("erkenntnisse", "Erkenntnisse", Icons.Outlined.Lightbulb),
    LeistenZiel("logbuch", "Logbuch", Icons.Outlined.History),
)

/** Ab dieser Wischweite wird gewechselt. */
private val WISCHWEITE = 64.dp

@Composable
fun ExperimenteNavigation(vm: AppViewModel, modifier: Modifier = Modifier) {
    val farben = LocalAppColors.current
    val nav = rememberNavController()
    val eintrag by nav.currentBackStackEntryAsState()
    val aktiv = eintrag?.destination?.route

    Column(modifier = modifier.fillMaxSize().background(farben.grund)) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = nav,
                startDestination = "heute",
                // Wechsel zwischen den Hauptbildschirmen: reines Überblenden, 200 ms (M-63).
                enterTransition = { fadeIn(tween(Bewegung.BLENDEN_MS, easing = Bewegung.blenden)) },
                exitTransition = { fadeOut(tween(Bewegung.BLENDEN_MS, easing = Bewegung.blenden)) },
            ) {
                composable("heute") {
                    MitWischen(nav, "heute") {
                        HeuteBildschirm(
                            vm = vm,
                            aufEinstellungen = { nav.navigate("einstellungen") },
                            aufGespraech = { id -> nav.navigate("gespraech/$id") },
                            aufAuswertung = { nav.navigate("auswertung") },
                            aufNichtUmgesetzt = { id ->
                                nav.navigate("auswertung?experiment=$id&nichtUmgesetzt=true")
                            },
                        )
                    }
                }
                composable("ziele") { MitWischen(nav, "ziele") { ZieleBildschirm(vm) } }
                composable("merkliste") { MitWischen(nav, "merkliste") { MerklisteBildschirm(vm) } }
                composable("erkenntnisse") {
                    MitWischen(nav, "erkenntnisse") { ErkenntnisseBildschirm(vm) }
                }
                composable("logbuch") { MitWischen(nav, "logbuch") { LogbuchBildschirm(vm) } }

                // Tiefer hinein: von rechts hereinschieben, 240 ms (M-64).
                composable(
                    route = "gespraech/{experimentId}",
                    arguments = listOf(navArgument("experimentId") { type = NavType.LongType }),
                    enterTransition = { hereinVonRechts() },
                    exitTransition = { hinausNachRechts() },
                    popExitTransition = { hinausNachRechts() },
                ) { stelle ->
                    GespraechBildschirm(
                        vm = vm,
                        experimentId = stelle.arguments?.getLong("experimentId") ?: 0L,
                        aufZurueck = { nav.popBackStack() },
                    )
                }

                composable(
                    route = "auswertung?experiment={experiment}&nichtUmgesetzt={nichtUmgesetzt}",
                    arguments = listOf(
                        navArgument("experiment") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("nichtUmgesetzt") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                    enterTransition = { hereinVonRechts() },
                    exitTransition = { hinausNachRechts() },
                    popExitTransition = { hinausNachRechts() },
                ) { stelle ->
                    val id = stelle.arguments?.getLong("experiment") ?: 0L
                    AuswertungBildschirm(
                        vm = vm,
                        aufZurueck = { nav.popBackStack() },
                        nurExperimentId = id.takeIf { it != 0L },
                        nichtUmgesetzt = stelle.arguments?.getBoolean("nichtUmgesetzt") == true,
                    )
                }

                composable(
                    route = "einstellungen",
                    enterTransition = { hereinVonRechts() },
                    exitTransition = { hinausNachRechts() },
                    popExitTransition = { hinausNachRechts() },
                ) {
                    EinstellungenBildschirm(
                        vm = vm,
                        aufZurueck = { nav.popBackStack() },
                        aufSelbstbild = { nav.navigate("selbstbild") },
                    )
                }

                composable(
                    route = "selbstbild",
                    enterTransition = { hereinVonRechts() },
                    exitTransition = { hinausNachRechts() },
                    popExitTransition = { hinausNachRechts() },
                ) {
                    SelbstbildBildschirm(vm = vm, aufZurueck = { nav.popBackStack() })
                }
            }
        }

        if (aktiv in HAUPTZIELE.map { it.kennung }) {
            UntereLeiste(
                ziele = HAUPTZIELE,
                aktiv = aktiv.orEmpty(),
                onWechsel = { kennung -> zuHauptbildschirm(nav, kennung) },
            )
        }
    }
}

/** **F-27** — waagerecht wischen wechselt zum nächsten oder vorigen Hauptbildschirm. */
@Composable
private fun MitWischen(
    nav: NavHostController,
    kennung: String,
    inhalt: @Composable () -> Unit,
) {
    val schwelle = with(LocalDensity.current) { WISCHWEITE.toPx() }
    var weite by remember { mutableStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> weite += delta },
                onDragStopped = {
                    val stelle = HAUPTZIELE.indexOfFirst { it.kennung == kennung }
                    val neu = when {
                        weite <= -schwelle -> stelle + 1
                        weite >= schwelle -> stelle - 1
                        else -> stelle
                    }
                    weite = 0f
                    // An den Enden der Reihe passiert nichts — kein Umlauf.
                    if (neu != stelle && neu in HAUPTZIELE.indices) {
                        zuHauptbildschirm(nav, HAUPTZIELE[neu].kennung)
                    }
                },
            ),
    ) {
        inhalt()
    }
}

/** Ein Hauptbildschirm ersetzt den anderen — der Rückweg führt immer auf „Heute“. */
private fun zuHauptbildschirm(nav: NavHostController, kennung: String) {
    nav.navigate(kennung) {
        popUpTo("heute") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun hereinVonRechts() =
    slideInHorizontally(tween(Bewegung.RUHIG_MS, easing = Bewegung.ruhig)) { breite -> breite }

private fun hinausNachRechts() =
    slideOutHorizontally(tween(Bewegung.RUHIG_MS, easing = Bewegung.ruhig)) { breite -> breite }

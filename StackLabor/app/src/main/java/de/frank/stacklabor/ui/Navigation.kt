package de.frank.stacklabor.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert

/**
 * Die Wege durch die App. Jede Kennung aus `02-UI-SPEC.md` §6 hat hier ihr Ziel —
 * es gibt keinen Knopf ohne Wirkung.
 */
object Ziele {
    const val HAUPT = "b01"                       // B-01 Hauptbildschirm
    const val STACK = "b02/{stackId}"             // B-02 Stack-Detail
    const val ZIELKATALOG = "b03"                 // B-03 Ziel-Katalog
    const val AUSWERTUNG = "b07/{stackId}"        // B-07 Auswertung im Vollbild
    const val GESAMT = "b09"                      // B-09 Alle Stacks zusammen
    const val EINSTELLUNGEN = "b10"               // B-10 Einstellungen
    const val ANMELDUNG = "b11"                   // B-11 Codex-Anmeldung
    const val ZIELE_ORDNEN = "b12/{stackId}"      // B-12 Ziele ordnen
    const val KATALOG = "b14"                     // B-14 Mittel-Katalog

    fun stack(id: String) = "b02/$id"
    fun auswertung(id: String) = "b07/$id"
    fun zieleOrdnen(id: String) = "b12/$id"
}

/**
 * Die Bildschirmwechsel — gemessen aus dem Entwurf:
 * vorwärts `@keyframes vor` (X +32 dp → 0, 300 ms), zurück `@keyframes zurueck`
 * (X −24 dp → 0, 300 ms). Der Rücklauf legt die kürzere Strecke zurück und fühlt sich
 * dadurch schneller an, ohne eine andere Dauer zu brauchen.
 */
@Composable
fun StackLaborNavigation(app: StackLaborApp, steuerung: NavHostController = rememberNavController()) {
    val reduziert = LocalBewegungReduziert.current
    val dichte = LocalDensity.current
    val vor32 = with(dichte) { 32.dp.roundToPx() }
    val zurueck24 = with(dichte) { 24.dp.roundToPx() }
    val hoch48 = with(dichte) { 48.dp.roundToPx() }
    val spec = tween<IntOffset>(if (reduziert) 0 else Dauer.WECHSEL, easing = Kurven.leit)
    val blende = tween<Float>(if (reduziert) 0 else Dauer.WECHSEL, easing = Kurven.leit)

    NavHost(
        navController = steuerung,
        startDestination = Ziele.HAUPT,
        enterTransition = { slideInHorizontally(spec) { vor32 } + fadeIn(blende) },
        exitTransition = { fadeOut(blende) },
        popEnterTransition = { slideInHorizontally(spec) { -zurueck24 } + fadeIn(blende) },
        popExitTransition = { fadeOut(blende) },
    ) {
        composable(Ziele.HAUPT) {
            HauptBildschirm(app = app, steuerung = steuerung)
        }
        composable(Ziele.STACK) { eintrag ->
            StackBildschirm(
                app = app,
                steuerung = steuerung,
                stackId = eintrag.arguments?.getString("stackId").orEmpty(),
            )
        }
        composable(Ziele.ZIELKATALOG) {
            ZielKatalogBildschirm(app = app, steuerung = steuerung)
        }
        composable(Ziele.GESAMT) {
            GesamtBildschirm(app = app, steuerung = steuerung)
        }
        composable(Ziele.EINSTELLUNGEN) {
            EinstellungenBildschirm(app = app, steuerung = steuerung)
        }
        composable(Ziele.ANMELDUNG) {
            AnmeldungsBildschirm(app = app, steuerung = steuerung)
        }
        composable(Ziele.KATALOG) {
            KatalogBildschirm(app = app, steuerung = steuerung)
        }
        // Vollbilder ziehen von unten auf (`@keyframes hoch`, 320 ms).
        composable(
            Ziele.AUSWERTUNG,
            enterTransition = {
                slideInVertically(tween(if (reduziert) 0 else Dauer.VOLLBILD, easing = Kurven.blatt)) { hoch48 } +
                    fadeIn(tween(if (reduziert) 0 else Dauer.VOLLBILD))
            },
            popExitTransition = {
                slideOutVertically(tween(if (reduziert) 0 else Dauer.WECHSEL, easing = Kurven.leit)) { hoch48 } +
                    fadeOut(tween(if (reduziert) 0 else Dauer.WECHSEL))
            },
        ) { eintrag ->
            AuswertungsBildschirm(
                app = app,
                steuerung = steuerung,
                stackId = eintrag.arguments?.getString("stackId").orEmpty(),
            )
        }
        composable(
            Ziele.ZIELE_ORDNEN,
            enterTransition = {
                slideInVertically(tween(if (reduziert) 0 else Dauer.VOLLBILD, easing = Kurven.blatt)) { hoch48 } +
                    fadeIn(tween(if (reduziert) 0 else Dauer.VOLLBILD))
            },
            popExitTransition = {
                slideOutVertically(tween(if (reduziert) 0 else Dauer.WECHSEL, easing = Kurven.leit)) { hoch48 } +
                    fadeOut(tween(if (reduziert) 0 else Dauer.WECHSEL))
            },
        ) { eintrag ->
            ZieleOrdnenBildschirm(
                app = app,
                steuerung = steuerung,
                stackId = eintrag.arguments?.getString("stackId").orEmpty(),
            )
        }
    }
}

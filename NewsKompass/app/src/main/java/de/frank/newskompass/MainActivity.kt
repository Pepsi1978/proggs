package de.frank.newskompass

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.newskompass.data.model.DesignModus
import de.frank.newskompass.news.Zeitplan
import de.frank.newskompass.ui.AutomatischeBereitschaft
import de.frank.newskompass.ui.EinstellungenScreen
import de.frank.newskompass.ui.NachrichtenScreen
import de.frank.newskompass.ui.theme.NewsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app get() = application as NewsApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nur beim echten Start auswerten — nach einer Drehung o. Ä. trägt die Absicht noch den alten Tipp.
        if (savedInstanceState == null) merkeZiel(intent)
        holeVersaeumtenLaufNach()

        setContent {
            // Führt nach der Installation einmal durch alle fehlenden Freigaben, Benachrichtigungen zuerst.
            AutomatischeBereitschaft(this@MainActivity)
            val stand by app.einstellungen.stand.collectAsStateWithLifecycle()
            val dunkel = when (stand.design) {
                DesignModus.HELL -> false
                DesignModus.DUNKEL -> true
                DesignModus.SYSTEM -> isSystemInDarkTheme()
            }
            DisposableEffect(dunkel) {
                val leiste = if (dunkel) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = leiste, navigationBarStyle = leiste)
                onDispose {}
            }
            var einstellungenOffen by rememberSaveable { mutableStateOf(false) }
            BackHandler(einstellungenOffen) { einstellungenOffen = false }
            // Ein Tipp auf eine Benachrichtigung führt immer zum Startbildschirm mit genau dieser Ausgabe.
            val ziel by app.oeffneAusgabe.collectAsStateWithLifecycle()
            LaunchedEffect(ziel) { if (ziel != null) einstellungenOffen = false }

            NewsTheme(dunkel, stand.farbDesign) {
                AnimatedContent(
                    targetState = einstellungenOffen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "bildschirm",
                ) { offen ->
                    if (offen) {
                        EinstellungenScreen(app, activity = this@MainActivity, zurueck = { einstellungenOffen = false })
                    } else {
                        NachrichtenScreen(app, oeffneEinstellungen = { einstellungenOffen = true })
                    }
                }
            }
        }
    }

    /** Die App lief schon: Der Tipp auf eine Benachrichtigung kommt hier an statt in onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        merkeZiel(intent)
    }

    private fun merkeZiel(absicht: Intent?) {
        val ausgabeId = absicht?.getStringExtra(Zeitplan.EXTRA_AUSGABE) ?: return
        app.oeffneAusgabe.value = AusgabeOeffnen(ausgabeId, absicht.getStringExtra(Zeitplan.EXTRA_THEMA))
        // Verbraucht: Ein späteres Neuerzeugen der Activity springt nicht noch einmal dorthin.
        absicht.removeExtra(Zeitplan.EXTRA_AUSGABE)
        absicht.removeExtra(Zeitplan.EXTRA_THEMA)
    }

    /**
     * War das Handy zur Laufzeit aus oder ohne Netz, fehlt die letzte Ausgabe. Dann beim Öffnen
     * sofort nachholen — aber nur, wenn die Anmeldung steht.
     */
    private fun holeVersaeumtenLaufNach() {
        val kontext = applicationContext
        app.bereich.launch {
            app.speicher.lade()
            Zeitplan.holeNach(kontext)
        }
    }

    override fun onDestroy() {
        if (isFinishing) app.vorleser.stoppe()
        super.onDestroy()
    }
}

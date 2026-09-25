package de.frank.newskompass

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.newskompass.data.model.DesignModus
import de.frank.newskompass.news.Zeitplan
import de.frank.newskompass.ui.EinstellungenScreen
import de.frank.newskompass.ui.NachrichtenScreen
import de.frank.newskompass.ui.theme.NewsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app get() = application as NewsApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        holeVersaeumtenLaufNach()

        setContent {
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

            NewsTheme(dunkel) {
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

    /**
     * War das Handy zur Laufzeit aus oder ohne Netz, fehlt die letzte Ausgabe. Dann beim Öffnen
     * sofort nachholen — aber nur, wenn die Anmeldung steht.
     */
    private fun holeVersaeumtenLaufNach() {
        app.bereich.launch {
            app.speicher.lade()
            if (!app.codex.istVerbunden) return@launch
            val letzter = Zeitplan.letzterTermin().toInstant().toEpochMilli()
            // Eine Ausgabe nur aus gesprochenen Fragen zählt nicht als Lauf aus dem Zeitplan.
            val neueste = app.speicher.ausgaben.value.firstOrNull { it.istRegulaer }?.erstelltUm ?: 0L
            if (neueste < letzter) Zeitplan.starteLauf(this@MainActivity, manuell = false)
        }
    }

    override fun onDestroy() {
        if (isFinishing) app.vorleser.stoppe()
        super.onDestroy()
    }
}

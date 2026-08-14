package de.frank.stacklabor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.stacklabor.ui.StackLaborNavigation
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.StackLaborTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StackLaborApp

        setContent {
            val dunkel by app.einstellungen.dunkel.collectAsStateWithLifecycle(initialValue = false)
            val reduziert by app.einstellungen.bewegungReduziert
                .collectAsStateWithLifecycle(initialValue = false)

            StackLaborTheme(dunkel = dunkel, bewegungReduziert = reduziert) {
                Box(Modifier.fillMaxSize().background(Farben.grund)) {
                    StackLaborNavigation(app = app)
                }
            }
        }
    }
}

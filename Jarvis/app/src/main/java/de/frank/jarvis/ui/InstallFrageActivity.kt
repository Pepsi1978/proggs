package de.frank.jarvis.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import de.frank.jarvis.ui.theme.JarvisTheme
import kotlinx.coroutines.delay
import java.io.File

/**
 * Fragt vor einer adb-Installation, wann sie starten soll, damit sie keine laufende Aufnahme abbricht.
 * Werkzeuge/jarvis-install startet das Fenster, liest die Antwort aus [DATEI] und installiert dann selbst;
 * Warten und Installieren laufen am Rechner, weil Jarvis dabei beendet wird.
 */
class InstallFrageActivity : ComponentActivity() {

    private var kennung = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kennung = intent.getStringExtra(EXTRA_KENNUNG).orEmpty()
        setContent {
            JarvisTheme("automatisch") {
                LaunchedEffect(Unit) {
                    delay(FRIST_MS)
                    finish()
                }
                AlertDialog(
                    onDismissRequest = { antworte("abbruch") },
                    properties = DialogProperties(dismissOnClickOutside = false),
                    confirmButton = {},
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Eine neue Jarvis-Version kann installiert werden. Jetzt installieren?")
                            (0..5).chunked(2).forEach { reihe ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    reihe.forEach { minuten ->
                                        val breite = Modifier.weight(1f)
                                        val rand = PaddingValues(horizontal = 4.dp)
                                        val text = when (minuten) {
                                            0 -> "Sofort"
                                            1 -> "In 1 Minute"
                                            else -> "In $minuten Minuten"
                                        }
                                        if (minuten == 0) {
                                            Button({ antworte("0") }, breite, contentPadding = rand) {
                                                Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        } else {
                                            OutlinedButton({ antworte("$minuten") }, breite, contentPadding = rand) {
                                                Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        kennung = intent.getStringExtra(EXTRA_KENNUNG).orEmpty()
    }

    private fun antworte(wert: String) {
        // Erst vollständig schreiben, dann umbenennen: Das Skript fragt laufend ab und darf nie eine halbe Antwort lesen.
        runCatching {
            val ordner = getExternalFilesDir(null) ?: return@runCatching
            val teil = File(ordner, "$DATEI.teil")
            teil.writeText("$kennung $wert")
            teil.renameTo(File(ordner, DATEI))
        }
        finish()
    }

    private companion object {
        const val EXTRA_KENNUNG = "kennung"
        const val DATEI = "install-antwort.txt"
        // Knapp unter der Wartezeit des Skripts: Danach installiert es ohnehin nicht mehr.
        const val FRIST_MS = 235_000L
    }
}

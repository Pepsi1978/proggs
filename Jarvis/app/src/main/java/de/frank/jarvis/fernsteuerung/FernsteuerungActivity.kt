package de.frank.jarvis.fernsteuerung

import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.File

/**
 * Mac-Bildschirm live auf dem Handy bedienen (Gegenstück: Jarvis/werkzeuge/mac-fernsteuerung auf dem Mac).
 * Verbindet über den Tunnel zur Mac-Adresse; der Token liegt in files/fernsteuerung-token.txt (kommt per adb aus ~/SK).
 */
class FernsteuerungActivity : ComponentActivity() {
    private var verbindung: FernVerbindung? = null
    private var dekoder: VideoDekoder? = null
    private var surface: Surface? = null
    private var macW = 0
    private var macH = 0
    private lateinit var ansicht: FernAnsicht

    private var status by mutableStateOf("Verbinde mit dem Mac …")
    private var gesperrt by mutableStateOf(false)
    private var tastaturAn by mutableStateOf(false)
    private var getrennt by mutableStateOf(false)
    private var meldung by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ansicht = FernAnsicht(this) { verbindung?.eingabe(it) }
        ansicht.surfaceBereit = { s ->
            // Erst den Dekoder lösen, dann die alte Surface freigeben.
            dekoder?.stop(); dekoder = null
            surface?.takeIf { it != s }?.release()
            surface = s
            if (s != null) dekoderNeu()
        }
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Oberflaeche() } }
    }

    override fun onStart() { super.onStart(); verbinden() }
    override fun onStop() { super.onStop(); trennen() }

    private fun token(): String = runCatching { File(filesDir, "fernsteuerung-token.txt").readText().trim() }.getOrDefault("")

    private fun verbinden() {
        trennen()
        val t = token()
        if (t.isEmpty()) { status = "Kein Zugang eingerichtet (fernsteuerung-token.txt fehlt)."; getrennt = true; return }
        getrennt = false
        status = "Verbinde mit dem Mac …"
        verbindung = FernVerbindung(
            MAC_ADRESSE, PORT, t,
            beiInfo = { w, h, ok ->
                runOnUiThread {
                    status = if (ok) "" else "Der Mac liefert gerade kein Bild (Bildschirmaufnahme gescheitert). Er versucht es erneut …"
                    if (ok && (w != macW || h != macH || dekoder == null)) { macW = w; macH = h; ansicht.macGroesse(w, h); dekoderNeu() }
                    else if (ok) verbindung?.eingabe(JSONObject().put("t", "schluesselbild"))
                }
            },
            beiBild = { dekoder?.bild(it) },
            beiZustand = { j -> runOnUiThread { zustand(j) } },
            beiEnde = { grund -> runOnUiThread { status = "Verbindung getrennt: $grund"; getrennt = true; gesperrt = false } },
            beiMeldung = { text -> runOnUiThread { meldung = if (meldung.isEmpty() || meldung.endsWith("…")) text else "$meldung\n$text" } },
        ).also { it.start() }
    }

    private fun trennen() {
        verbindung?.stop(); verbindung = null
        dekoder?.stop(); dekoder = null
    }

    private fun dekoderNeu() {
        dekoder?.stop(); dekoder = null
        val s = surface ?: return
        if (macW == 0) return
        dekoder = VideoDekoder(s, macW, macH) { verbindung?.eingabe(JSONObject().put("t", "schluesselbild")) }.also { it.start() }
        verbindung?.eingabe(JSONObject().put("t", "schluesselbild"))
    }

    private fun zustand(j: JSONObject) {
        gesperrt = j.optBoolean("gesperrt")
        ansicht.geheim = j.optBoolean("geheim")
        val feld = j.optBoolean("tastatur")
        // Tastatur folgt dem Fokus auf dem Mac: Eingabefeld angetippt -> auf, Feld verlassen -> zu.
        if (feld != tastaturAn && !gesperrt) { tastaturAn = feld; ansicht.tastaturZeigen(feld) }
    }

    private fun taste(k: String, vararg m: String) =
        verbindung?.eingabe(JSONObject().put("t", "taste").put("k", k).put("m", org.json.JSONArray(m.toList())))

    @Composable
    private fun Oberflaeche() {
        Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().imePadding()) {
            Column(Modifier.fillMaxSize()) {
                AndroidView({ ansicht }, Modifier.weight(1f).fillMaxWidth())
                Leiste()
            }
            if (status.isNotEmpty() && !gesperrt) {
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(status, color = Color.White, fontSize = 16.sp)
                    if (getrennt) Button(onClick = { verbinden() }, Modifier.padding(top = 12.dp)) { Text("Neu verbinden") }
                }
            }
            if (gesperrt) Entsperren(Modifier.align(Alignment.Center))
        }
    }

    @Composable
    private fun Leiste() {
        Row(
            Modifier.fillMaxWidth().height(52.dp).background(Color(0xFF15171C)).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Knopf(icon = Icons.Rounded.Close, text = "Ende") { finish() }
            Knopf(text = "Esc") { taste("esc") }
            Knopf(text = "Tab") { taste("tab") }
            Knopf(text = "⌫") { taste("zurueck") }
            Knopf(text = "Enter") { taste("enter") }
            Knopf(text = "Suche ⌘␣") { taste("leer", "cmd") }
            Knopf(text = "App-Wechsel") { taste("tab", "cmd") }
            Knopf(text = "Fenster zu ⌘W") { taste("w", "cmd") }
            Knopf(text = "Beenden ⌘Q") { taste("q", "cmd") }
            Knopf(text = "↑") { taste("hoch") }
            Knopf(text = "↓") { taste("runter") }
            Knopf(text = "←") { taste("links") }
            Knopf(text = "→") { taste("rechts") }
            Knopf(icon = Icons.Rounded.Keyboard, text = "Tastatur", aktiv = tastaturAn) {
                tastaturAn = !tastaturAn; ansicht.tastaturZeigen(tastaturAn)
            }
        }
    }

    @Composable
    private fun Knopf(text: String, icon: ImageVector? = null, aktiv: Boolean = false, aktion: () -> Unit) {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).background(if (aktiv) Color(0xFF3B6FE0) else Color(0xFF2A2E37))
                .clickable(onClick = aktion).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp).padding(end = 2.dp))
            Text(text, color = Color.White, fontSize = 14.sp)
        }
    }

    /** Gesperrter Mac liefert kein Bild: Passwort hier eingeben, der Mac tippt es auf dem Sperrbildschirm ein. */
    @Composable
    private fun Entsperren(modifier: Modifier) {
        var passwort by remember { mutableStateOf("") }
        var sperreAus by remember { mutableStateOf(true) }
        val los = {
            verbindung?.eingabe(JSONObject().put("t", "entsperren").put("s", passwort).put("sperreAus", sperreAus))
            passwort = ""
            meldung = "Wird am Mac eingetippt …"
        }
        Column(
            modifier.widthIn(max = 420.dp).padding(24.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1E222A)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Der Mac ist gesperrt", color = Color.White, fontSize = 18.sp)
            Text("Im gesperrten Zustand zeigt macOS kein Bild. Jarvis tippt dein Passwort am Mac ein; bei zugeklapptem Deckel lässt macOS das aber oft nicht zu. Dann den Mac einmal am Gerät entsperren, danach hält Jarvis ihn wach.",
                color = Color(0xFFB8BEC9), fontSize = 14.sp)
            OutlinedTextField(
                passwort, { passwort = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Mac-Passwort") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { los() }),
            )
            Row(Modifier.fillMaxWidth().clickable { sperreAus = !sperreAus }, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(sperreAus, { sperreAus = it })
                Text("Mac danach nicht mehr von selbst sperren", color = Color.White, fontSize = 14.sp)
            }
            Button(onClick = los, Modifier.fillMaxWidth(), enabled = passwort.isNotEmpty()) { Text("Entsperren") }
            if (meldung.isNotEmpty()) Text(meldung, color = Color(0xFFFFD27A), fontSize = 14.sp)
        }
    }

    companion object {
        /** Adresse des Macs im eigenen Tunnel (WireGuard, utun am Mac). */
        const val MAC_ADRESSE = "10.8.0.6"
        const val PORT = 47800
    }
}

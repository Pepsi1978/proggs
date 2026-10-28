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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * Mac- oder Windows-Bildschirm live auf dem Handy bedienen (Gegenstück: Jarvis/werkzeuge/mac-fernsteuerung bzw.
 * windows-fernsteuerung). Ziel kommt als Extra [EXTRA_ZIEL], ohne Extra der Mac. Verbindet über den Tunnel zur Adresse
 * des Rechners; der Token (für beide derselbe) liegt in files/fernsteuerung-token.txt (kommt per adb aus ~/SK).
 */
class FernsteuerungActivity : ComponentActivity() {
    private var verbindung: FernVerbindung? = null
    private var dekoder: VideoDekoder? = null
    private var surface: Surface? = null
    private var macW = 0
    private var macH = 0
    private lateinit var ansicht: FernAnsicht
    private val ziel by lazy { Fernziel.aus(intent.getStringExtra(EXTRA_ZIEL)) }

    private var status by mutableStateOf("")
    private var gesperrt by mutableStateOf(false)
    private var tastaturAn by mutableStateOf(false)
    private var getrennt by mutableStateOf(false)
    private var meldung by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setTitle(ziel.titel)
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
        status = "Verbinde mit ${ziel.dativ} …"
        verbindung = FernVerbindung(
            ziel.adresse, PORT, t,
            beiInfo = { w, h, ok ->
                runOnUiThread {
                    status = if (ok) "" else "${ziel.nominativ} liefert gerade kein Bild (Bildschirmaufnahme gescheitert). Er versucht es erneut …"
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

    /** Von Hand: für Eingabefelder, bei denen der Mac den Fokus nicht meldet. */
    private fun tastaturUmschalten() { tastaturAn = !tastaturAn; ansicht.tastaturZeigen(tastaturAn) }

    private fun taste(k: String, vararg m: String) =
        verbindung?.eingabe(JSONObject().put("t", "taste").put("k", k).put("m", org.json.JSONArray(m.toList())))

    @Composable
    private fun Oberflaeche() {
        // Knopfzustand folgt der echten Tastatur (auch wenn sie per Zurück-Geste geschlossen wurde).
        val imeSichtbar = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
        androidx.compose.runtime.LaunchedEffect(imeSichtbar) { tastaturAn = imeSichtbar }
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
            if (gesperrt) { if (ziel == Fernziel.WINDOWS) WindowsGesperrt(Modifier.align(Alignment.Center)) else Entsperren(Modifier.align(Alignment.Center)) }
            // Tastatur-Knopf unten rechts in Daumennähe, über der Leiste.
            if (!gesperrt) Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 68.dp).size(56.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (tastaturAn) Color(0xFF3B6FE0) else Color(0xCC2A2E37))
                    .clickable { tastaturUmschalten() }
                    .semantics { contentDescription = "Tastatur ein- oder ausblenden" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Keyboard, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
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
            Knopf(icon = Icons.Rounded.Keyboard, text = "Tastatur", aktiv = tastaturAn) { tastaturUmschalten() }
            Knopf(text = "Esc") { taste("esc") }
            Knopf(text = "Tab") { taste("tab") }
            Knopf(text = "⌫") { taste("zurueck") }
            Knopf(text = "Enter") { taste("enter") }
            if (ziel == Fernziel.WINDOWS) {
                Knopf(text = "Start ⊞") { taste("win") }
                Knopf(text = "App-Wechsel") { taste("tab", "alt") }
                Knopf(text = "Tab zu Strg+W") { taste("w", "ctrl") }
                Knopf(text = "Fenster zu Alt+F4") { taste("f4", "alt") }
            } else {
                Knopf(text = "Suche ⌘␣") { taste("leer", "cmd") }
                Knopf(text = "App-Wechsel") { taste("tab", "cmd") }
                Knopf(text = "Fenster zu ⌘W") { taste("w", "cmd") }
                Knopf(text = "Beenden ⌘Q") { taste("q", "cmd") }
            }
            Knopf(text = "↑") { taste("hoch") }
            Knopf(text = "↓") { taste("runter") }
            Knopf(text = "←") { taste("links") }
            Knopf(text = "→") { taste("rechts") }
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

    /** Windows lässt den Sperrbildschirm aus der Benutzersitzung heraus weder aufnehmen noch bedienen. */
    @Composable
    private fun WindowsGesperrt(modifier: Modifier) {
        Column(
            modifier.widthIn(max = 420.dp).padding(24.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF1E222A)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Windows ist gesperrt", color = Color.White, fontSize = 18.sp)
            Text("Sperrbildschirm und Administrator-Abfragen zeigt Windows aus der Ferne nicht und nimmt dort keine Eingaben an. Den PC einmal am Gerät entsperren; sobald er entsperrt ist, kommt das Bild von selbst zurück.",
                color = Color(0xFFB8BEC9), fontSize = 14.sp)
        }
    }

    companion object {
        const val PORT = 47800
        /** Fernziel.name ("MAC" oder "WINDOWS"); fehlt es, verbindet die Fernsteuerung wie bisher mit dem Mac. */
        const val EXTRA_ZIEL = "ziel"
    }
}

package de.frank.longevity.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.antippen
import de.frank.longevity.ui.theme.glas
import de.frank.longevity.ui.theme.knopf3d
import kotlinx.coroutines.delay

@Composable
fun NeuBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.Rounded.Close, "Abbrechen") { vm.zurueck() }
            Text("Eigener Punkt", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Sprich oder tippe eine Verhaltensweise, Idee oder These – z. B. „Kalorienrestriktion verlängert die Lebensdauer“. " +
                    "Die KI prüft sie und ordnet sie nach Wichtigkeit in deine Liste ein.",
                color = f.textLeise, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Column(Modifier.fillMaxWidth().einblenden().glas(f, erhoeht = 1.3f).padding(18.dp)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 130.dp)) {
                    if (vm.eText.isEmpty()) Text(
                        if (vm.aufnahme == Aufnahme.LAEUFT) "Ich höre zu …" else "Deine Idee …",
                        color = f.textSchwach, fontSize = 18.sp,
                    )
                    BasicTextField(
                        vm.eText, vm::textGeaendert,
                        textStyle = TextStyle(color = f.text, fontSize = 18.sp, lineHeight = 26.sp),
                        cursorBrush = SolidColor(f.primaer),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val zurueck = vm.textVorKorrektur != null
                    AktionsKnopf(
                        if (zurueck) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.AutoFixHigh,
                        if (zurueck) "Zurück" else "KI-Korrektur",
                        beschaeftigt = vm.korrigiert && !zurueck,
                        modifier = Modifier.weight(1f),
                    ) { if (zurueck) vm.korrekturZuruecknehmen() else vm.korrigieren() }
                    if (zurueck) AktionsKnopf(Icons.Rounded.AutoFixHigh, "Neue Fassung", beschaeftigt = vm.korrigiert, modifier = Modifier.weight(1f)) { vm.korrigieren() }
                }
            }
            Mikrofon(vm)
            HauptKnopf(Icons.Rounded.AutoAwesome, if (vm.kiVerbunden) "Auswerten & einordnen" else "Speichern") { vm.auswerten() }
            Text(
                if (vm.kiVerbunden) "Mit ${vm.einstellungen.modell.label} · ${vm.einstellungen.denkstufe.label}. Die Auswertung läuft im Hintergrund weiter – du siehst den Fortschritt in der Liste."
                else "Ohne ChatGPT-Verbindung wird die Idee unten angehängt. Verbinden kannst du in den Einstellungen.",
                color = f.textSchwach, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun Mikrofon(vm: AppViewModel) {
    val f = LocalFarben.current
    val laeuft = vm.aufnahme == Aufnahme.LAEUFT
    val verarbeitet = vm.aufnahme == Aufnahme.VERARBEITET
    val puls = rememberInfiniteTransition(label = "mikro")
    val p by puls.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "p")
    var sekunden by remember { mutableLongStateOf(0L) }
    LaunchedEffect(laeuft) { while (laeuft) { sekunden = (System.currentTimeMillis() - vm.aufnahmeStart) / 1000; delay(250) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(108.dp).drawBehind {
                if (laeuft) for (i in 0..2) {
                    val q = (p + i / 3f) % 1f
                    drawCircle(f.sekundaer.copy(alpha = 0.35f * (1f - q)), size.minDimension / 2 * (0.8f + 0.7f * q))
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(86.dp).graphicsLayer { val s = if (laeuft) 1f + 0.04f * kotlin.math.sin(p * 6.28f) else 1f; scaleX = s; scaleY = s }
                    .knopf3d(if (laeuft) f.gefahr else f.primaer, f.sekundaer, 99.dp, f.dunkel)
                    .antippen { vm.mikroTippen() },
                contentAlignment = Alignment.Center,
            ) {
                if (verarbeitet) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                else Icon(if (laeuft) Icons.Rounded.Stop else Icons.Rounded.Mic, if (laeuft) "Aufnahme beenden" else "Einsprechen", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        Text(
            when {
                laeuft -> "%d:%02d · Tippen zum Beenden".format(sekunden / 60, sekunden % 60)
                verarbeitet -> "Whisper transkribiert …"
                vm.einstellungen.groqKey.isBlank() -> "Für Sprache: Groq-Schlüssel in den Einstellungen"
                else -> "Tippen und sprechen"
            },
            color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

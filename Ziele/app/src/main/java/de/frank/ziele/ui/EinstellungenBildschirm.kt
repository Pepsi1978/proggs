package de.frank.ziele.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.BuildConfig
import de.frank.ziele.auth.CodexModel
import de.frank.ziele.auth.ReasoningEffort
import de.frank.ziele.auth.deviceCodeGroups
import de.frank.ziele.tts.TtsCatalog
import de.frank.ziele.tts.TtsProvider
import de.frank.ziele.ui.theme.Chip
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.antippen
import de.frank.ziele.ui.theme.glas
import de.frank.ziele.ui.theme.knopf3d
import kotlin.math.roundToInt

@Composable
fun EinstellungenBildschirm(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    val context = LocalContext.current
    // Nach dem Übernehmen aus Jarvis zeigen die Felder die neuen Schlüssel (sie merken sich sonst ihren ersten Wert).
    var schluesselStand by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text("Einstellungen", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        androidx.compose.runtime.key(schluesselStand) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // ---- Schlüssel aus Jarvis ----
                Block("Schnell einrichten") {
                    Text("Jarvis hat deine Schlüssel schon (Groq, Anthropic, Stimmen). Ein Tipp übernimmt sie, nichts muss doppelt eingetippt werden. ChatGPT verbindest du unten mit einem Code.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
                    Chip("Schlüssel aus Jarvis übernehmen", false, icon = Icons.Rounded.Link) { vm.schluesselAusJarvis { schluesselStand++ } }
                }
                // ---- Sprache ----
                Block("Spracheingabe · Whisper (Groq)") {
                    Feld("Groq-API-Schlüssel", e.groqKey, geheim = true) { e.groqKey = it }
                    Text("Whisper-Modell", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { de.frank.ziele.audio.GroqTranscriber.MODELLE.forEach { (id, name) -> Chip(name, e.whisperModell == id) { e.whisperModell = id } } }
                    Schalter("Persönliches Wörterbuch", e.woerterbuchAn) { e.woerterbuchAn = it }
                    Feld("Begriffe (durch Komma oder Zeilenumbruch getrennt)", e.woerterbuch, einzeilig = false) { e.woerterbuch = it }
                    Text("Filter gegen stille Halluzinationen", color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Schalter("1 · Stille vor dem Hochladen verwerfen", e.filterStille) { e.filterStille = it }
                    Schalter("2 · Unsichere Abschnitte (Metriken) verwerfen", e.filterMetriken) { e.filterMetriken = it }
                    Schalter("3 · Text ohne Ton (Zeitstempel) verwerfen", e.filterZeitstempel) { e.filterZeitstempel = it }
                    Schalter("4 · Typische Floskeln in Stille verwerfen", e.filterFloskeln) { e.filterFloskeln = it }
                }
                // ---- KI ----
                Block("KI · ChatGPT (OpenAI)") {
                    val code = vm.geraeteCode
                    when {
                        vm.kiVerbunden -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✅", fontSize = 20.sp)
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text("Verbunden", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(vm.auth.email ?: "ChatGPT-Konto", color = f.textLeise, fontSize = 12.sp)
                                }
                                Chip("Trennen", false, icon = Icons.Rounded.LinkOff) { vm.kiTrennen() }
                            }
                        }
                        code != null -> {
                            Text("Code wurde kopiert – auf der Seite einfügen:", color = f.textLeise, fontSize = 13.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                deviceCodeGroups(code.userCode).forEachIndexed { i, g ->
                                    if (i > 0) Box(Modifier.padding(horizontal = 8.dp).width(14.dp).height(2.dp).glas(f, 1.dp, 0f, f.textLeise, rand = false))
                                    Text(g, color = f.primaer, fontSize = 32.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                                }
                            }
                            ChipReihe {
                                Chip("Kopieren", false, icon = Icons.Rounded.ContentCopy) {
                                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("OpenAI-Code", code.userCode))
                                    vm.melde("Code kopiert")
                                }
                                Chip("Seite öffnen", false, icon = Icons.Rounded.OpenInNew) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUri)))
                                }
                                Chip("Abbrechen", false) { vm.kiAbbrechen() }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Text("Warte auf Bestätigung …", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                        else -> {
                            Text("Verbinde dein ChatGPT-Konto (wie bei Codex): Die App zeigt einen Code (4 + 5 Zeichen), kopiert ihn automatisch und öffnet die OpenAI-Seite – dort nur einfügen.", color = f.textLeise, fontSize = 13.sp)
                            Row(
                                Modifier.fillMaxWidth().height(52.dp).knopf3d(f.primaer, f.sekundaer, 16.dp, f.dunkel).antippen { vm.kiVerbinden(activity) },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                            ) {
                                if (vm.kiVerbindet) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                else Icon(Icons.Rounded.Link, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Mit ChatGPT verbinden", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            vm.kiFehler?.let { Text(it, color = f.gefahr, fontSize = 12.sp) }
                        }
                    }
                    // Anbieter wie in Jarvis: OpenAI über das ChatGPT-Konto oder Anthropic über einen eigenen API-Schlüssel.
                    val kontext = androidx.compose.ui.platform.LocalContext.current
                    var anthropicKey by remember { mutableStateOf(de.frank.ziele.auth.AnthropicKlient.schluessel(kontext)) }
                    Text("Anbieter", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe {
                        de.frank.ziele.auth.KiAnbieter.entries.forEach { a ->
                            Chip(a.label, e.modell.anbieter == a) {
                                if (e.modell.anbieter != a) {
                                    val alt = e.modell
                                    if (alt.istAnthropic) de.frank.ziele.auth.AnthropicKlient.claudeModellMerken(kontext, de.frank.ziele.auth.ClaudeModell.von(alt.apiId) ?: de.frank.ziele.auth.ClaudeModell.OPUS)
                                    else de.frank.ziele.auth.AnthropicKlient.openAiModellMerken(kontext, alt.apiId)
                                    e.modell = CodexModel.fromLabel(
                                        if (a == de.frank.ziele.auth.KiAnbieter.ANTHROPIC) de.frank.ziele.auth.AnthropicKlient.letztesClaudeModell(kontext).apiId
                                        else de.frank.ziele.auth.AnthropicKlient.letztesOpenAiModell(kontext, CodexModel.TERRA.apiId),
                                    )
                                    e.denkstufe = e.modell.normalizeEffort(e.denkstufe)
                                }
                            }
                        }
                    }
                    if (e.modell.istAnthropic) {
                        Text(if (anthropicKey.isBlank()) "Für Claude bitte deinen Anthropic-API-Schlüssel (sk-ant-…) eintragen." else "Claude denkt über deinen Anthropic-API-Schlüssel (eigene Abrechnung).", color = f.textLeise, fontSize = 13.sp)
                        Feld("Anthropic-API-Schlüssel", anthropicKey, geheim = true) { anthropicKey = it; de.frank.ziele.auth.AnthropicKlient.schluesselSpeichern(kontext, it) }
                    }
                    Text("Modell", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { CodexModel.von(e.modell.anbieter).forEach { m -> Chip(m.label, e.modell == m) { e.modell = m; e.denkstufe = m.normalizeEffort(e.denkstufe) } } }
                    Text("Denkstufe", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { e.modell.supportedEfforts.forEach { r -> Chip(r.titel(e.modell), e.denkstufe == r) { e.denkstufe = r } } }
                }
                // ---- Kontext ----
                Block("Kontext für die Auswertung") {
                    var persoenlich by remember { mutableStateOf(e.kontextNutzen) }
                    var mitJarvis by remember { mutableStateOf(e.jarvisKontext) }
                    Schalter("Persönlichen Kontext nutzen", persoenlich) { persoenlich = it; e.kontextNutzen = it }
                    Text(
                        if (persoenlich) "An: Die Aufgaben werden auf dich zugeschnitten, mit Kontextdatei und, wenn eingeschaltet, Jarvis' Live-Gedächtnis."
                        else "Aus: Die Auswertung empfiehlt allgemeine Aufgaben, die für jeden mit diesem Ziel gelten.",
                        color = f.textLeise, fontSize = 12.sp,
                    )
                    if (persoenlich) {
                        Schalter("Jarvis' Live-Gedächtnis einbeziehen", mitJarvis) { mitJarvis = it; e.jarvisKontext = it; if (it) vm.jarvisPruefen() }
                        Text("Deine Regeln, Ziele, Dienst, Biodaten und was Jarvis über dich weiß, fließen in die Vorschläge ein.", color = f.textLeise, fontSize = 12.sp)
                        if (mitJarvis) {
                            androidx.compose.runtime.LaunchedEffect(Unit) { if (vm.jarvisAngebunden == null) vm.jarvisPruefen() }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when {
                                    vm.jarvisPrueft -> CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    vm.jarvisAngebunden == true -> Text("✅", fontSize = 18.sp)
                                    vm.jarvisAngebunden == false -> Text("⚠️", fontSize = 18.sp)
                                }
                                Text(
                                    if (vm.jarvisPrueft) "Prüfe die Anbindung …" else vm.jarvisPruefung.ifBlank { "Noch nicht geprüft." },
                                    color = if (vm.jarvisAngebunden == false && !vm.jarvisPrueft) f.gefahr else f.text, fontSize = 13.sp,
                                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                                )
                            }
                            Chip("Anbindung prüfen", false, icon = Icons.Rounded.Link) { vm.jarvisPruefen() }
                        }
                        val kontextText by vm.kontext.collectAsState()
                        val inhalt = kontextText.orEmpty().trim()
                        Text("Kontextdatei", color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (inhalt.isEmpty()) "Noch leer. Hier schreibst, kopierst oder sprichst du ein, was die KI über dich wissen soll."
                            else "${"%,d".format(inhalt.length).replace(',', '.')} Zeichen: " + inhalt.take(140).replace('\n', ' ') + if (inhalt.length > 140) " …" else "",
                            color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp,
                        )
                        Chip("Kontextdatei bearbeiten", false, icon = Icons.Rounded.AutoAwesome) { vm.zeige(Bildschirm.Kontext) }
                    }
                }
                // ---- Auswertung ----
                Block("Auswertung") {
                    Schalter("Vorher im Internet recherchieren", e.recherche) { e.recherche = it }
                    Text("Die KI sucht mit ihrer Websuche, wie man das Ziel nachweislich am besten erreicht, und baut die Aufgaben darauf auf. Dauert etwas länger, ist aber deutlich besser.", color = f.textLeise, fontSize = 12.sp)
                    Text("Wie viele Vorschläge", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { listOf("knapp" to "Knapp", "normal" to "Normal", "ausfuehrlich" to "Ausführlich").forEach { (id, name) -> Chip(name, e.vorschlagMenge == id) { e.vorschlagMenge = id } } }
                }
                // ---- Vorlesen ----
                Block("Vorlesen") {
                    val anbieter = listOf(TtsProvider.EDGE, TtsProvider.GOOGLE_CLOUD, TtsProvider.GEMINI_FLASH, TtsProvider.GEMINI_LITE,
                        TtsProvider.AZURE, TtsProvider.QWEN_CLONE)
                    ChipReihe { anbieter.forEach { p -> Chip(p.kurz, e.ttsAnbieter == p.id) { e.ttsAnbieter = p.id } } }
                    when (e.ttsAnbieter) {
                        TtsProvider.GOOGLE_CLOUD.id -> {
                            Feld("Google-Cloud-API-Schlüssel", e.googleKey, geheim = true) { e.googleKey = it }
                            StimmenReihe(TtsCatalog.googleVoices, e.googleStimme, e) { e.googleStimme = it }
                        }
                        TtsProvider.QWEN_CLONE.id -> {
                            Feld("Alibaba-API-Schlüssel (DashScope)", e.qwenKey, geheim = true) { e.qwenKey = it }
                            Feld("Stimm-ID der eigenen Stimme", e.qwenStimme) { e.qwenStimme = it }
                        }
                        TtsProvider.GEMINI_FLASH.id, TtsProvider.GEMINI_LITE.id, TtsProvider.AZURE.id -> NeueStimmenBereich(e)
                        else -> StimmenReihe(TtsCatalog.edgeVoices, e.edgeStimme, e) { e.edgeStimme = it }
                    }
                    RueckfallKette(e)
                    // Ein Tempo je Stimme, die es auswertet: die Hauptstimme und jede Rückfallstufe, jeweils nur einmal (wie in Jarvis).
                    val haupt = TtsProvider.entries.firstOrNull { it.id == e.ttsAnbieter }
                    val mitTempo = (listOfNotNull(haupt) + e.rueckfallKette.mapNotNull { TtsCatalog.rueckfallStufe(it)?.anbieter })
                        .distinct().filter { it.kenntTempo }
                    Text("Sprechtempo", color = f.textLeise, fontSize = 13.sp)
                    if (mitTempo.isEmpty()) Text("Die gewählten Stimmen sprechen alle im eigenen Tempo.", color = f.textLeise, fontSize = 12.sp)
                    mitTempo.forEach { p ->
                        val tempo = e.ttsTempo(p.id)
                        Text("${p.label}${if (p == haupt) "" else " (Rückfall)"}: ${"%.2f".format(tempo)}×", color = f.text, fontSize = 13.sp)
                        Slider(tempo, { e.setzeTtsTempo(p.id, it) }, valueRange = 0.7f..1.3f, colors = SliderDefaults.colors(thumbColor = f.primaer, activeTrackColor = f.primaer))
                    }
                    if (mitTempo.isNotEmpty()) Text("Jede Stimme behält ihr eigenes Tempo, auch wenn sie als Rückfallstufe einspringt.", color = f.textLeise, fontSize = 12.sp)
                    Chip("Probe hören", false, icon = Icons.Rounded.PlayArrow) { vm.vorlesen("probe", "Hallo Frank! So klingen deine Ziele. Heute bist du deinem Ziel wieder ein Stück näher.") }
                }
                Block("Zusammenspiel") {
                    Text("Geniale Aufgaben: Das Plus an jedem Vorschlag legt ihn dort als Aufgabe an. Ist sie dort erledigt, zeigt Ziele einen grünen Haken und der Fortschrittsring wächst.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
                    Text("Jarvis kann deine Ziele lesen, anlegen, ändern, auswerten lassen und Vorschläge übernehmen; alle Ziele stehen in seinem Live-Gedächtnis.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
                    Text("Genialer Wecker: liest die aktiven Ziele über content://de.frank.ziele.wecker/vorlesen und kann sie beim Wecken vorlesen.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
                }
                Text(
                    "Ziele ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Stand ${BuildConfig.VERSION_BUMPED_AT}",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }
    }
}


/** Schalter mit Symbol, Titel und Erklärung; eingeschaltet leuchtet das Symbol in der Designfarbe. */
@Composable
private fun OptionSchalter(icon: ImageVector, titel: String, text: String, an: Boolean, aendern: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().glas(f, 16.dp, erhoeht = 0.5f, fuellung = f.flaecheStark, toenung = if (an) f.primaer else null)
            .antippen(haptik = false) { aendern(!an) }.padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).then(
                if (an) Modifier.knopf3d(f.primaer, f.sekundaer, 12.dp, f.dunkel)
                else Modifier.glas(f, 12.dp, erhoeht = 0f, fuellung = f.textSchwach.copy(alpha = 0.12f), rand = false),
            ),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (an) Color.White else f.textLeise, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text, color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Switch(an, aendern, colors = SwitchDefaults.colors(checkedTrackColor = f.primaer))
    }
}

@Composable
fun Schalter(text: String, an: Boolean, aendern: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().antippen(haptik = false) { aendern(!an) }, verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = f.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(an, aendern, colors = SwitchDefaults.colors(checkedTrackColor = f.primaer))
    }
}

@Composable
fun Feld(titel: String, wert: String, geheim: Boolean = false, einzeilig: Boolean = true, aendern: (String) -> Unit) {
    val f = LocalFarben.current
    var text by remember { mutableStateOf(wert) }
    var zeigen by remember { mutableStateOf(!geheim) }
    Column {
        Text(titel, color = f.textLeise, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).glas(f, 12.dp, 0f, f.flaecheStark).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                text, { text = it; aendern(it) }, singleLine = einzeilig,
                textStyle = TextStyle(color = f.text, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(f.primaer),
                visualTransformation = if (zeigen) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.weight(1f),
            )
            if (geheim) Text(if (zeigen) "verbergen" else "zeigen", color = f.primaer, fontSize = 12.sp, modifier = Modifier.antippen(haptik = false) { zeigen = !zeigen }.padding(start = 8.dp))
        }
    }
}

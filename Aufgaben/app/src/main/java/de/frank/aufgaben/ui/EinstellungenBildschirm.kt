package de.frank.aufgaben.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.media.RingtoneManager
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.BuildConfig
import de.frank.aufgaben.auth.CodexModel
import de.frank.aufgaben.auth.ReasoningEffort
import de.frank.aufgaben.auth.deviceCodeGroups
import de.frank.aufgaben.erinnerung.Toene
import de.frank.aufgaben.tts.TtsCatalog
import de.frank.aufgaben.tts.TtsProvider
import de.frank.aufgaben.ui.theme.Chip
import de.frank.aufgaben.ui.theme.Design
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.farbenFuer
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import kotlin.math.roundToInt

@Composable
fun EinstellungenBildschirm(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    val context = LocalContext.current
    val tonWahl = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                e.ton = "uri:$uri"
                e.tonName = RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Systemton"
            }
        }
    }
    val dateiWahl = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.eigenenTonWaehlen(uri)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text("Einstellungen", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        run {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // ---- Aussehen ----
                Block("Design") {
                    Design.entries.forEach { d ->
                        val aktiv = e.design == d.id
                        val vorschau = farbenFuer(d, f.dunkel)
                        Row(
                            Modifier.fillMaxWidth().glas(f, 18.dp, if (aktiv) 1.6f else 0.5f, if (aktiv) vorschau.primaer.copy(alpha = 0.18f) else f.flaeche)
                                .antippen { e.design = d.id }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(46.dp).knopf3d(vorschau.primaer, vorschau.sekundaer, vorschau.radius / 2, f.dunkel), contentAlignment = Alignment.Center) { Text(d.emoji, fontSize = 22.sp) }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(d.anzeige, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(d.beschreibung, color = f.textLeise, fontSize = 12.sp)
                            }
                            if (aktiv) Text("✓", color = vorschau.primaer, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Block("Hell / Dunkel") {
                    ChipReihe {
                        Chip("🅰️ Automatisch", e.modus == "system") { e.modus = "system" }
                        Chip("☀️ Hell", e.modus == "hell") { e.modus = "hell" }
                        Chip("🌙 Dunkel", e.modus == "dunkel") { e.modus = "dunkel" }
                    }
                    Schalter("Animierte Szene oben zeigen", e.szeneZeigen) { e.szeneZeigen = it }
                }
                // ---- Sprache ----
                Block("Spracheingabe · Whisper Large V3 Turbo (Groq)") {
                    Feld("Groq-API-Schlüssel", e.groqKey, geheim = true) { e.groqKey = it }
                    Schalter("Beim Plus sofort aufnehmen", e.autoMikro) { e.autoMikro = it }
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
                    Text("Modell", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { CodexModel.entries.forEach { m -> Chip(m.label, e.modell == m) { e.modell = m } } }
                    Text("Denkstufe", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { e.modell.supportedEfforts.forEach { r -> Chip(r.label, e.denkstufe == r) { e.denkstufe = r } } }
                    Schalter("Fehlende Überschrift per KI erzeugen", e.kiTitel) { e.kiTitel = it }
                }
                // ---- Vorlesen ----
                Block("Vorlesen") {
                    val anbieter = listOf(TtsProvider.EDGE, TtsProvider.GOOGLE_CLOUD, TtsProvider.QWEN_CLONE)
                    ChipReihe { anbieter.forEach { p -> Chip(p.kurz, e.ttsAnbieter == p.id) { e.ttsAnbieter = p.id } } }
                    when (e.ttsAnbieter) {
                        TtsProvider.GOOGLE_CLOUD.id -> {
                            Feld("Google-Cloud-API-Schlüssel", e.googleKey, geheim = true) { e.googleKey = it }
                            ChipReihe { TtsCatalog.googleVoices.forEach { v -> Chip(v.name, e.googleStimme == v.id) { e.googleStimme = v.id } } }
                        }
                        TtsProvider.QWEN_CLONE.id -> {
                            Feld("Alibaba-API-Schlüssel (DashScope)", e.qwenKey, geheim = true) { e.qwenKey = it }
                            Feld("Stimm-ID der eigenen Stimme", e.qwenStimme) { e.qwenStimme = it }
                        }
                        else -> ChipReihe { TtsCatalog.edgeVoices.forEach { v -> Chip(v.name, e.edgeStimme == v.id) { e.edgeStimme = v.id } } }
                    }
                    Text("Tempo ${"%.2f".format(e.sprechtempo)}×", color = f.textLeise, fontSize = 13.sp)
                    Slider(e.sprechtempo, { e.sprechtempo = it }, valueRange = 0.7f..1.3f, colors = SliderDefaults.colors(thumbColor = f.primaer, activeTrackColor = f.primaer))
                    Chip("Probe hören", false, icon = Icons.Rounded.PlayArrow) { vm.vorlesen("probe", "Hallo! So klingen deine Aufgaben. Heute stehen drei Dinge an.") }
                }
                // ---- Erinnerungen ----
                Block("Erinnerungen") {
                    Text("Ton", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe {
                        Toene.eigene.forEach { (id, name) -> Chip(name, e.ton == id) { e.ton = id; e.tonName = name; vm.tonProbe() } }
                    }
                    Chip(if (e.ton.startsWith("uri:")) "Systemton: ${e.tonName}" else "Systemton wählen …", e.ton.startsWith("uri:"), icon = Icons.Rounded.MusicNote) {
                        tonWahl.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Erinnerungston"),
                        )
                    }
                    Chip(if (e.ton.startsWith("datei:")) "Eigene MP3: ${e.tonName}" else "Eigene MP3 wählen …", e.ton.startsWith("datei:"), icon = Icons.Rounded.AudioFile) {
                        dateiWahl.launch(arrayOf("audio/*"))
                    }
                    Text("Eigene Töne (z. B. aus Suno) werden in die App kopiert und klingen so auch offline.", color = f.textLeise, fontSize = 12.sp)
                    Text("Lautstärke ${(e.lautstaerke * 100).toInt()} %", color = f.textLeise, fontSize = 13.sp)
                    Slider(e.lautstaerke, { e.lautstaerke = it }, onValueChangeFinished = { vm.tonProbe() }, colors = SliderDefaults.colors(thumbColor = f.primaer, activeTrackColor = f.primaer))
                    Text("Standard-Vorlauf", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { listOf(0, 5, 10, 15, 30, 60).forEach { v -> Chip(if (v == 0) "Pünktlich" else "$v Min.", e.vorlaufStandard == v) { e.vorlaufStandard = v } } }
                    Schalter("Vibration", e.vibration) { e.vibration = it }
                    Schalter("Neue Erinnerungen vorlesen", e.vorlesenStandard) { e.vorlesenStandard = it }
                    Text("Beim Speichern entstehen mit deiner Stimme (Abschnitt Vorlesen) sechs Fassungen des Aufgabentextes. Die Erinnerung spielt sie offline nacheinander ab, mit drei Sekunden Pause; als Wecker so lange, bis du ausschaltest. Ohne vorbereitete Fassung spricht die Android-Stimme des Handys.", color = f.textLeise, fontSize = 12.sp)
                    Chip("Ton testen", false, icon = Icons.Rounded.PlayArrow) { vm.tonProbe() }
                }
                Block("Zeitleiste") {
                    fun uhr(m: Int) = "%02d:%02d".format(m / 60, m % 60)
                    Text("Von ${uhr(e.zeitleisteVon)} bis ${uhr(e.zeitleisteBis)} Uhr", color = f.textLeise, fontSize = 13.sp)
                    RangeSlider(
                        e.zeitleisteVon / 30f..e.zeitleisteBis / 30f,
                        { r ->
                            val von = r.start.roundToInt().coerceAtMost(46)
                            val bis = r.endInclusive.roundToInt().coerceAtLeast(von + 2)
                            if (von * 30 != e.zeitleisteVon) e.zeitleisteVon = von * 30
                            if (bis * 30 != e.zeitleisteBis) e.zeitleisteBis = bis * 30
                        },
                        valueRange = 0f..48f, steps = 47,
                        colors = SliderDefaults.colors(thumbColor = f.primaer, activeTrackColor = f.primaer),
                    )
                    Schalter("Automatisch an die Termine anpassen", e.zeitleisteAuto) { e.zeitleisteAuto = it }
                    Text("Mit Automatik reicht die Leiste von einer Stunde vor dem ersten bis eine Stunde nach dem letzten Termin. Sobald du eine Aufgabe ziehst, zeigt sie wieder die eingestellte Spanne, damit du sie überall ablegen kannst.", color = f.textLeise, fontSize = 12.sp)
                }
                Block("Fokus-Timer") {
                    ChipReihe { listOf(15, 25, 45, 60).forEach { m -> Chip("$m Min.", e.fokusMinuten == m) { e.fokusMinuten = m } } }
                }
                Block("Widget & Wecker-Brücke") {
                    Text("Widget „Aufgaben · Heute“ auf dem Startbildschirm hinzufügen: lange auf den Startbildschirm drücken → Widgets → Aufgaben. Es zeigt immer die heutigen Aufgaben; um 0 Uhr rücken die Morgen-Aufgaben automatisch nach.", color = f.textLeise, fontSize = 13.sp)
                    Text("Der Geniale Wecker kann die Aufgaben über content://de.frank.aufgaben.wecker/morgen bzw. /tag/<Datum> auslesen und beim Wecken vorlesen.", color = f.textLeise, fontSize = 13.sp)
                }
                Text(
                    "Aufgaben ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Stand ${BuildConfig.VERSION_BUMPED_AT}",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }
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
fun Feld(titel: String, wert: String, geheim: Boolean = false, aendern: (String) -> Unit) {
    val f = LocalFarben.current
    var text by remember { mutableStateOf(wert) }
    var zeigen by remember { mutableStateOf(!geheim) }
    Column {
        Text(titel, color = f.textLeise, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).glas(f, 12.dp, 0f, f.flaecheStark).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                text, { text = it; aendern(it) }, singleLine = true,
                textStyle = TextStyle(color = f.text, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(f.primaer),
                visualTransformation = if (zeigen) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.weight(1f),
            )
            if (geheim) Text(if (zeigen) "verbergen" else "zeigen", color = f.primaer, fontSize = 12.sp, modifier = Modifier.antippen(haptik = false) { zeigen = !zeigen }.padding(start = 8.dp))
        }
    }
}

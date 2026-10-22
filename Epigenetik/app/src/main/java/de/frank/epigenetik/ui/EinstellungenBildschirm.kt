package de.frank.epigenetik.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.BuildConfig
import de.frank.epigenetik.auth.CodexModel
import de.frank.epigenetik.auth.deviceCodeGroups
import de.frank.epigenetik.data.Tiefe
import de.frank.epigenetik.tts.TtsCatalog
import de.frank.epigenetik.tts.TtsProvider
import de.frank.epigenetik.ui.theme.Chip
import de.frank.epigenetik.ui.theme.Design
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas
import de.frank.epigenetik.ui.theme.knopf3d

@Composable
fun EinstellungenBildschirm(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", 40.dp) { vm.zurueck() }
                    Text("Einstellungen", color = f.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 12.dp))
                }
            }
            // ---- KI ----
            item {
                Block("KI · ChatGPT über Codex") {
                    val code = vm.geraeteCode
                    when {
                        vm.kiVerbunden -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✅", fontSize = 20.sp)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text("Verbunden", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(vm.auth.email ?: "ChatGPT-Konto", color = f.textLeise, fontSize = 12.sp)
                            }
                            Chip("Trennen", false, icon = Icons.Rounded.LinkOff) { vm.kiTrennen() }
                        }
                        code != null -> {
                            Text("Code wurde kopiert – auf der OpenAI-Seite einfügen:", color = f.textLeise, fontSize = 13.sp)
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
                                Chip("Seite öffnen", false, icon = Icons.Rounded.OpenInNew) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUri))) }
                                Chip("Abbrechen", false) { vm.kiAbbrechen() }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Text("Warte auf Bestätigung …", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                        else -> {
                            Text("Verbinde dein ChatGPT-Konto wie bei Codex: Die App zeigt einen Code (4 + 5 Zeichen), kopiert ihn und öffnet die OpenAI-Seite – dort nur einfügen.", color = f.textLeise, fontSize = 13.sp)
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
                    Text("Gründlichkeit", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { Tiefe.entries.forEach { t -> Chip(t.anzeige, e.tiefe == t) { e.tiefe = t } } }
                    Text(e.tiefe.beschreibung, color = f.textSchwach, fontSize = 11.sp)
                    Text("Gleichzeitige Recherchen: ${e.parallel}", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { (1..5).forEach { n -> Chip("$n", e.parallel == n) { e.parallel = n; vm.melde("Gilt ab dem nächsten App-Start") } } }
                }
            }
            // ---- Profil ----
            item {
                Block("Kurzprofil für die KI") {
                    Text("Fließt in jede Recherche ein (Dosierung, Risiken, Empfehlungen). Leer lassen geht auch.", color = f.textLeise, fontSize = 12.sp)
                    Feld("Alter", e.alter, hinweis = "z. B. 48") { e.alter = it }
                    ChipReihe { listOf("männlich", "weiblich", "").forEach { g -> Chip(g.ifBlank { "keine Angabe" }, e.geschlecht == g) { e.geschlecht = g } } }
                    Feld("Weitere Angaben", e.profil, einzeilig = false, hinweis = "z. B. Schichtarbeit, Ausdauersport, Medikamente …") { e.profil = it }
                }
            }
            // ---- Spracheingabe ----
            item {
                Block("Spracheingabe") {
                    Text(
                        if (e.groqKey.isBlank()) "Ohne Schlüssel nutzt die App die Spracheingabe des Handys. Mit Groq-Schlüssel: Whisper, sehr genau bei Fachbegriffen."
                        else "Groq Whisper mit vier Filtern gegen erfundenen Text in Stille.",
                        color = f.textLeise, fontSize = 12.sp,
                    )
                    Feld("Groq-API-Schlüssel (optional)", e.groqKey, geheim = true) { e.groqKey = it }
                    Schalter("Nach dem Einsprechen sofort recherchieren", e.sofortNachDiktat) { e.sofortNachDiktat = it }
                    if (e.groqKey.isNotBlank()) {
                        Text("Whisper-Modell", color = f.textLeise, fontSize = 13.sp)
                        ChipReihe { de.frank.epigenetik.audio.GroqTranscriber.MODELLE.forEach { (id, name) -> Chip(name, e.whisperModell == id) { e.whisperModell = id } } }
                        Schalter("Persönliches Wörterbuch", e.woerterbuchAn) { e.woerterbuchAn = it }
                        Feld("Begriffe (durch Komma oder Zeilenumbruch getrennt)", e.woerterbuch, einzeilig = false) { e.woerterbuch = it }
                        Schalter("Stille erkennen und verwerfen", e.filterStille) { e.filterStille = it }
                        Schalter("Unsichere Abschnitte verwerfen", e.filterMetriken) { e.filterMetriken = it }
                        Schalter("Text ohne Ton verwerfen", e.filterZeitstempel) { e.filterZeitstempel = it }
                        Schalter("Typische Floskeln verwerfen", e.filterFloskeln) { e.filterFloskeln = it }
                    }
                }
            }
            // ---- Vorlesen ----
            item {
                Block("Vorlesen") {
                    ChipReihe {
                        listOf(TtsProvider.EDGE, TtsProvider.GOOGLE_CLOUD).forEach { p -> Chip(p.label, e.ttsAnbieter == p.id) { e.ttsAnbieter = p.id } }
                    }
                    if (e.ttsAnbieter == TtsProvider.GOOGLE_CLOUD.id) {
                        Feld("Google-Cloud-Schlüssel", e.googleKey, geheim = true) { e.googleKey = it }
                        Text("Stimme", color = f.textLeise, fontSize = 13.sp)
                        ChipReihe { TtsCatalog.googleVoices.forEach { v -> Chip(v.name, e.googleStimme == v.id) { e.googleStimme = v.id } } }
                    } else {
                        Text("Stimme (Microsoft Edge, ohne Schlüssel)", color = f.textLeise, fontSize = 13.sp)
                        ChipReihe { TtsCatalog.edgeVoices.forEach { v -> Chip(v.name, e.edgeStimme == v.id) { e.edgeStimme = v.id } } }
                    }
                    Text("Tempo: ${"%.2f".format(e.tempo)}×", color = f.textLeise, fontSize = 13.sp)
                    Slider(e.tempo, { e.tempo = it }, valueRange = 0.7f..1.3f, steps = 11, colors = SliderDefaults.colors(thumbColor = f.primaer, activeTrackColor = f.primaer))
                    GlasKnopf("Probe hören", Icons.Rounded.PlayArrow) { vm.probeVorlesen() }
                }
            }
            // ---- Aussehen ----
            item {
                Block("Aussehen") {
                    ChipReihe { listOf("hell" to "Hell", "system" to "Automatisch", "dunkel" to "Dunkel").forEach { (id, n) -> Chip(n, e.modus == id) { e.modus = id } } }
                    Design.entries.forEach { d ->
                        Row(
                            Modifier.fillMaxWidth().glas(f, 14.dp, 0.4f, f.flaecheStark, toenung = if (Design.von(e.design) == d) f.primaer else null).antippen { e.design = d.id }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(d.emoji, fontSize = 20.sp)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(d.anzeige, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(d.beschreibung, color = f.textLeise, fontSize = 12.sp)
                            }
                            if (Design.von(e.design) == d) Text("✓", color = f.primaer, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // ---- Jarvis ----
            item {
                Block("Jarvis") {
                    Absatz(
                        "Jarvis greift direkt auf diese App zu: Er liest vorhandene Auswertungen (z. B. „Welche epigenetischen Wirkungen hat Ashwagandha?“), " +
                            "startet neue Recherchen, wenn etwas fehlt, und sieht deinen Stack und den Schalter-Atlas. Von Jarvis gestartete Recherchen sind markiert.",
                        leise = true, groesse = 13,
                    )
                }
            }
            item {
                Text(
                    "Epigenetik ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Stand ${BuildConfig.VERSION_BUMPED_AT}",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

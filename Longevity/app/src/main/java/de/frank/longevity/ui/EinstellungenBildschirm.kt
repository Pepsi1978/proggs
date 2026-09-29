package de.frank.longevity.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import de.frank.longevity.BuildConfig
import de.frank.longevity.auth.CodexModel
import de.frank.longevity.auth.ReasoningEffort
import de.frank.longevity.auth.deviceCodeGroups
import de.frank.longevity.ui.theme.Chip
import de.frank.longevity.ui.theme.Design
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.antippen
import de.frank.longevity.ui.theme.farbenFuer
import de.frank.longevity.ui.theme.glas
import de.frank.longevity.ui.theme.knopf3d

@Composable
fun EinstellungenBildschirm(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    val context = LocalContext.current
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
                        Chip("Wie System", e.modus == "system") { e.modus = "system" }
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
                Block("KI · ChatGPT über Codex (OpenAI)") {
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
                    Text("Modell für Auswertung, Vertiefung und Aktualisierung", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { CodexModel.entries.forEach { m -> Chip(m.label, e.modell == m) { e.modell = m; e.denkstufe = m.normalizeEffort(e.denkstufe) } } }
                    Text("Effort (Denkstufe)", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { e.modell.supportedEfforts.forEach { r -> Chip(r.label, e.denkstufe == r) { e.denkstufe = r } } }
                    Text("Höherer Effort = gründlicher, dauert länger. Empfohlen: GPT-6 Astra · Hoch.", color = f.textSchwach, fontSize = 11.sp)
                }
                Block("Textkorrektur (Sprache und Tippen)") {
                    Text("Modell", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { CodexModel.entries.forEach { m -> Chip(m.label, e.korrekturModell == m) { e.korrekturModell = m; e.korrekturDenkstufe = m.normalizeEffort(e.korrekturDenkstufe) } } }
                    Text("Effort (Denkstufe)", color = f.textLeise, fontSize = 13.sp)
                    ChipReihe { e.korrekturModell.supportedEfforts.forEach { r -> Chip(r.label, e.korrekturDenkstufe == r) { e.korrekturDenkstufe = r } } }
                    Text("Für die Korrektur reicht meist ein schnelles Modell mit niedrigem Effort.", color = f.textSchwach, fontSize = 11.sp)
                }
                Block("Dein Kurzprofil (optional)") {
                    Text("Fließt in jede KI-Einordnung ein – so passen Rangliste, Ziele und Aufgaben zu dir. Leer = allgemein.", color = f.textLeise, fontSize = 13.sp)
                    Feld("Alter", e.alter) { e.alter = it }
                    ChipReihe {
                        listOf("weiblich", "männlich", "divers").forEach { g -> Chip(g, e.geschlecht == g) { e.geschlecht = if (e.geschlecht == g) "" else g } }
                    }
                    Feld("Weitere Angaben (z. B. Sport, Schlaf, Rauchen, Vorerkrankungen, Blutwerte)", e.profil, einzeilig = false) { e.profil = it }
                }
                Text(
                    "Longevity ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Stand ${BuildConfig.VERSION_BUMPED_AT}",
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
fun Feld(titel: String, wert: String, geheim: Boolean = false, einzeilig: Boolean = true, aendern: (String) -> Unit) {
    val f = LocalFarben.current
    var text by remember { mutableStateOf(wert) }
    var zeigen by remember { mutableStateOf(!geheim) }
    Column {
        Text(titel, color = f.textLeise, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).glas(f, 12.dp, 0f, f.flaecheStark).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                text, { text = it; aendern(it) }, singleLine = einzeilig,
                textStyle = TextStyle(color = f.text, fontSize = 15.sp, fontFamily = if (geheim) FontFamily.Monospace else FontFamily.Default),
                cursorBrush = SolidColor(f.primaer),
                visualTransformation = if (zeigen) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.weight(1f),
            )
            if (geheim) Text(if (zeigen) "verbergen" else "zeigen", color = f.primaer, fontSize = 12.sp, modifier = Modifier.antippen(haptik = false) { zeigen = !zeigen }.padding(start = 8.dp))
        }
    }
}

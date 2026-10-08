package de.frank.modellkompass.ui

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
import androidx.compose.foundation.layout.displayCutoutPadding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import de.frank.modellkompass.BuildConfig
import de.frank.modellkompass.auth.CodexModel
import de.frank.modellkompass.auth.deviceCodeGroups
import de.frank.modellkompass.ui.theme.Chip
import de.frank.modellkompass.ui.theme.LocalFarben
import de.frank.modellkompass.ui.theme.antippen
import de.frank.modellkompass.ui.theme.glas
import de.frank.modellkompass.ui.theme.knopf3d

@Composable
fun EinstellungenBildschirm(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().displayCutoutPadding().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text("Einstellungen", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                val stufe = e.modell.normalizeEffort(e.denkstufe)
                ChipReihe { e.modell.supportedEfforts.forEach { r -> Chip(r.label, stufe == r) { e.denkstufe = r } } }
                Text("Höhere Denkstufen suchen gründlicher, brauchen aber je Bereich deutlich länger und mehr Kontingent.", color = f.textLeise, fontSize = 12.sp)
                Schalter("Bevorzugte Verarbeitung (schneller, mehr Kontingent)", e.prioritaet) { e.prioritaet = it }
            }
            // ---- Recherche ----
            Block("Recherche") {
                Feld("Rechner, auf dem die Modelle laufen sollen", e.hardware) { e.hardware = it }
                Text("Modelle je Bereich", color = f.textLeise, fontSize = 13.sp)
                ChipReihe { listOf(3, 5, 8).forEach { n -> Chip("$n", e.anzahl == n) { e.anzahl = n } } }
                Text(
                    "Bei jedem Lauf sucht das gewählte Modell im Internet (Hugging Face, Bestenlisten, Foren) nach den Modellen mit der besten Qualität, die auf diesem Rechner gut laufen. " +
                        "Danach prüft die App jeden Namen direkt bei Hugging Face. Einen einzelnen Bereich aktualisierst du über den Knopf oben rechts in seiner Ansicht.",
                    color = f.textLeise, fontSize = 12.sp, lineHeight = 17.sp,
                )
            }
            Text(
                "ModellKompass ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Stand ${BuildConfig.VERSION_BUMPED_AT}",
                color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

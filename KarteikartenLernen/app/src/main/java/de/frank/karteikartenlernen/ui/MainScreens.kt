package de.frank.karteikartenlernen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.karteikartenlernen.audio.SoundEffect
import de.frank.karteikartenlernen.audio.TtsVoiceRegistry
import de.frank.karteikartenlernen.model.AppSettings
import de.frank.karteikartenlernen.model.AppUiState
import de.frank.karteikartenlernen.model.StudySession
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette
import de.frank.karteikartenlernen.ui.theme.Newsreader
import de.frank.karteikartenlernen.ui.theme.SchibstedGrotesk

@Composable
fun ProfilesScreen(state: AppUiState, onSearch: (String) -> Unit, onSession: (StudySession) -> Unit) {
    val c = LocalAppPalette.current
    ScreenTitle("Profile")
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.field).border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = c.faint, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = state.search,
                onValueChange = onSearch,
                textStyle = TextStyle(c.text, 14.sp, fontFamily = SchibstedGrotesk),
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                decorationBox = { inner ->
                    if (state.search.isEmpty()) Text("Sessions durchsuchen …", color = c.faint, fontSize = 14.sp)
                    inner()
                },
            )
        }
        Spacer(Modifier.height(14.dp))
        state.sessions.filter { it.title.contains(state.search, true) }.forEach { session ->
            SessionCard(session, onSession)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SessionCard(session: StudySession, onClick: (StudySession) -> Unit) {
    val c = LocalAppPalette.current
    val progress = session.known.toFloat() / session.count.coerceAtLeast(1)
    val done = session.known == session.count
    val accent = listOf(c.accent, c.accent2, c.red)[session.accent]
    GlassSurface(Modifier.fillMaxWidth().clickable { onClick(session) }) {
        Box {
            Box(Modifier.align(Alignment.CenterStart).width(3.dp).height(88.dp).background(accent))
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(session.title, color = c.text, fontFamily = Newsreader, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("${session.count} Karten · ${session.date}", color = c.faint, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (done) {
                        Row(Modifier.clip(RoundedCornerShape(99.dp)).background(c.green.copy(alpha = 0.13f)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Check, null, tint = c.green, modifier = Modifier.size(13.dp))
                            Text("Komplett", color = c.green, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                        }
                    } else Text("${(progress * 100).toInt()}%", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp).clip(RoundedCornerShape(99.dp)).background(c.field)) {
                    Box(Modifier.fillMaxWidth(progress).height(6.dp).background(if (done) Brush.horizontalGradient(listOf(c.green, c.green)) else Brush.horizontalGradient(listOf(accent, c.accent2))))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: AppUiState,
    onModel: (String) -> Unit,
    onReasoning: (String) -> Unit,
    onSettings: ((AppSettings) -> AppSettings) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onSpeakTest: () -> Unit,
    onTestSound: (SoundEffect) -> Unit,
) {
    val c = LocalAppPalette.current
    val s = state.settings
    var voiceMenuExpanded by remember { mutableStateOf(false) }
    ScreenTitle("Einstellungen")
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        SettingsSection("GPT-MODELL") {
            SettingRow("Aktives Modell", "${state.model}") {
                SegmentedControl(listOf("Soul", "Terra", "Luna"), state.model.substringAfterLast(' '), onSelect = { onModel("GPT 5.6 $it") })
            }
            SettingRow("Reasoning-Stufe") {
                SegmentedControl(listOf("Niedrig", "Mittel", "Hoch"), state.reasoning, onReasoning)
            }
            SettingRow("Karten pro Recherche", if (s.cardsPerResearch == 0) "Unbegrenzt" else "${s.cardsPerResearch} Karten") {
                SmallPill(if (s.cardsPerResearch == 0) "Maximum" else s.cardsPerResearch.toString()) {
                    onSettings { old -> old.copy(cardsPerResearch = when (old.cardsPerResearch) { 0 -> 10; 10 -> 20; 20 -> 30; else -> 0 }) }
                }
            }
        }
        SettingsSection("VERBINDUNGEN") {
            SettingRow("OpenAI", if (state.connectedEmail != null) "${state.connectedEmail} · OAuth" else "Per Browser-Login, ohne API-Schlüssel") {
                if (state.connectedEmail == null) GradientPill("Anmelden ↗", onLogin)
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(c.green, CircleShape))
                    Text("Angemeldet", color = c.green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp))
                    SmallPill("Trennen", onLogout)
                }
            }
            SettingRow("Groq (Whisper)", "Geräte-Spracherkennung") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(c.green, CircleShape))
                    Text("Verbunden", color = c.green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        SettingsSection("DARSTELLUNG") {
            SettingRow("Erscheinungsbild") {
                SegmentedControl(listOf("Hell", "Dunkel"), if (s.dark) "Dunkel" else "Hell", onSelect = { selected -> onSettings { old -> old.copy(dark = selected == "Dunkel") } })
            }
            SettingRow("Farbprofil", if (s.dark) listOf("Mitternacht", "Bordeaux", "Tanne")[s.darkProfile] else listOf("Elfenbein", "Salbei", "Porzellan")[s.lightProfile]) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    repeat(3) { index ->
                        val selected = (if (s.dark) s.darkProfile else s.lightProfile) == index
                        val colors = if (s.dark) {
                            listOf(Color(0xFFD9A95B) to Color(0xFF7FA8C9), Color(0xFFD98098) to Color(0xFFC9A06B), Color(0xFF8FBF9F) to Color(0xFFC9B06B))
                        } else {
                            listOf(Color(0xFFA87C2F) to Color(0xFF5B7FA6), Color(0xFF537F64) to Color(0xFFC07A4A), Color(0xFF4A5FB5) to Color(0xFF8A6FC0))
                        }[index]
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(Brush.linearGradient(listOf(colors.first, colors.second))).border(if (selected) 2.5.dp else 0.dp, c.text, CircleShape).clickable {
                                onSettings { old -> if (old.dark) old.copy(darkProfile = index) else old.copy(lightProfile = index) }
                            },
                        )
                    }
                }
            }
            SettingRow("Schriftgröße Karten") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallPill("−") { onSettings { it.copy(cardFont = (it.cardFont - 1).coerceAtLeast(16)) } }
                    Text("${s.cardFont} pt", color = c.muted, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
                    SmallPill("+") { onSettings { it.copy(cardFont = (it.cardFont + 1).coerceAtMost(28)) } }
                }
            }
        }
        SettingsSection("SOUNDEFFEKTE") {
            SettingRow("Soundeffekte") { Toggle(s.sounds) { onSettings { it.copy(sounds = !it.sounds) } } }
            SettingRow("Lautstärke") {
                Slider(
                    value = s.volume,
                    onValueChange = { value -> onSettings { it.copy(volume = value) } },
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                    modifier = Modifier.width(120.dp),
                )
            }
            SoundSetting("Umdrehen", SoundEffect.FLIP, s.soundFlip, onTestSound) { onSettings { it.copy(soundFlip = !it.soundFlip) } }
            SoundSetting("Gewusst", SoundEffect.KNOWN, s.soundKnown, onTestSound) { onSettings { it.copy(soundKnown = !it.soundKnown) } }
            SoundSetting("Nicht gewusst", SoundEffect.UNKNOWN, s.soundUnknown, onTestSound) { onSettings { it.copy(soundUnknown = !it.soundUnknown) } }
            SoundSetting("Kartenübergang", SoundEffect.TRANSITION, s.soundTransition, onTestSound) { onSettings { it.copy(soundTransition = !it.soundTransition) } }
            SoundSetting("Rundenabschluss", SoundEffect.DONE, s.soundDone, onTestSound) { onSettings { it.copy(soundDone = !it.soundDone) } }
        }
        SettingsSection("VORLESEN (EDGE TTS)") {
            val selectedVoice = TtsVoiceRegistry.voice(s.voice)
            SettingRow("Stimme", TtsVoiceRegistry.displayName(selectedVoice)) {
                Box {
                    SmallPill(selectedVoice.name) { voiceMenuExpanded = true }
                    DropdownMenu(
                        expanded = voiceMenuExpanded,
                        onDismissRequest = { voiceMenuExpanded = false },
                    ) {
                        TtsVoiceRegistry.voices.forEach { voice ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        TtsVoiceRegistry.displayName(voice),
                                        color = if (voice.id == selectedVoice.id) c.accent else c.text,
                                        fontSize = 13.sp,
                                        fontWeight = if (voice.id == selectedVoice.id) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                onClick = {
                                    voiceMenuExpanded = false
                                    onSettings { it.copy(voice = voice.id) }
                                },
                            )
                        }
                    }
                }
            }
            SettingRow("") { GradientPill("Stimme testen", onSpeakTest) }
            SettingRow("Sprechgeschwindigkeit") {
                Slider(
                    value = s.speechRate,
                    onValueChange = { value -> onSettings { it.copy(speechRate = value) } },
                    valueRange = 0.6f..1.6f,
                    steps = 9,
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                    modifier = Modifier.width(120.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val c = LocalAppPalette.current
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(title, color = c.faint, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp, bottom = 9.dp))
        GlassSurface(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

@Composable
private fun SettingRow(label: String, sub: String? = null, control: @Composable () -> Unit) {
    val c = LocalAppPalette.current
    Row(
        Modifier.fillMaxWidth().border(0.5.dp, c.border).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            if (label.isNotEmpty()) Text(label, color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
            sub?.let { Text(it, color = c.faint, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp)) }
        }
        Spacer(Modifier.width(8.dp))
        control()
    }
}

@Composable
private fun Toggle(on: Boolean, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    Box(Modifier.size(46.dp, 28.dp).clip(RoundedCornerShape(99.dp)).background(if (on) c.accent else c.field).clickable(onClick = onClick)) {
        Box(Modifier.padding(3.dp).size(22.dp).align(if (on) Alignment.CenterEnd else Alignment.CenterStart).background(Color.White, CircleShape))
    }
}

@Composable
private fun SoundSetting(label: String, effect: SoundEffect, on: Boolean, test: (SoundEffect) -> Unit, toggle: () -> Unit) {
    SettingRow(label) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val c = LocalAppPalette.current
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(c.chip).border(1.dp, c.border, RoundedCornerShape(8.dp)).clickable { test(effect) }.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PlayArrow, null, tint = c.muted, modifier = Modifier.size(11.dp))
                Text("Test", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
            }
            Toggle(on, toggle)
        }
    }
}

@Composable
private fun GradientPill(label: String, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    Text(label, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Brush.linearGradient(listOf(c.accent, c.accent2))).clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 8.dp))
}

@Composable
private fun SmallPill(label: String, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    Text(label, color = c.muted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(c.chip).border(1.dp, c.border, RoundedCornerShape(99.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp), maxLines = 1)
}

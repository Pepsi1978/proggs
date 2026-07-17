package de.frank.karteikartenlernen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.karteikartenlernen.model.AppUiState
import de.frank.karteikartenlernen.model.GenerationPhase
import de.frank.karteikartenlernen.model.MicState
import de.frank.karteikartenlernen.text.researchAnswerForSpeech
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette
import de.frank.karteikartenlernen.ui.theme.Newsreader
import de.frank.karteikartenlernen.ui.theme.SchibstedGrotesk

@Composable
fun ResearchScreen(
    state: AppUiState,
    onMicClick: () -> Unit,
    onInput: (String) -> Unit,
    onImprove: () -> Unit,
    onUndo: () -> Unit,
    onSend: () -> Unit,
    onSpeak: (String) -> Unit,
    onLearn: () -> Unit,
    onReset: () -> Unit,
) {
    val c = LocalAppPalette.current
    AppHeader()
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 76.dp)) {
        if (state.answer == null) MicButton(state.mic, state.recordingSeconds, onMicClick)
        Spacer(Modifier.height(if (state.answer == null) 4.dp else 8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 102.dp, max = 306.dp)
                .animateContentSize()
                .clip(RoundedCornerShape(18.dp))
                .background(c.field)
                .border(1.dp, if (state.input.isNotEmpty()) c.borderHigh else c.border, RoundedCornerShape(18.dp)),
        ) {
            BasicTextField(
                value = state.input,
                onValueChange = onInput,
                textStyle = TextStyle(color = c.text, fontSize = 15.5.sp, lineHeight = 23.sp, fontFamily = SchibstedGrotesk),
                minLines = 1,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier.fillMaxWidth().heightIn(min = 102.dp, max = 306.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 58.dp)) {
                        if (state.input.isEmpty()) Text("Frage sprechen oder hier eintippen …", color = c.faint, fontSize = 15.5.sp)
                        inner()
                    }
                },
            )
            if (state.mic == MicState.TRANSCRIBING) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp)) { LoadingRing(c.accent, 20.dp) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val inputAvailable = state.input.isNotBlank() && state.mic == MicState.IDLE && state.generationPhase == null
            val canImprove = inputAvailable && !state.improving
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (inputAvailable) Brush.linearGradient(listOf(c.accent, c.accent2)) else Brush.linearGradient(listOf(c.field, c.field)))
                    .clickable(enabled = canImprove, onClick = onImprove)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.improving) LoadingRing(Color.White, 16.dp) else Icon(Icons.Outlined.AutoAwesome, null, tint = if (inputAvailable) Color.White else c.faint, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state.improving) "Formuliere …" else "In gutes Deutsch", color = if (inputAvailable) Color.White else c.faint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.field)
                    .border(1.dp, c.border, RoundedCornerShape(14.dp))
                    .clickable(enabled = state.versionIndex > 0, onClick = onUndo),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Replay, "Rückgängig", tint = if (state.versionIndex > 0) c.text else c.faint)
            }
        }
        val canSend = state.input.isNotBlank() && state.generationPhase == null && state.mic == MicState.IDLE && !state.improving
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (state.input.isNotBlank()) c.text else c.field)
                .clickable(enabled = canSend, onClick = onSend)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (state.generationPhase == GenerationPhase.ANSWER) "Bitte warten …" else "Absenden ↗", color = if (state.input.isNotBlank()) c.background0 else c.faint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        state.authError?.let {
            Text(it, color = c.red, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp))
        }
        state.message?.let {
            Text(it, color = c.red, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp))
        }
        AnimatedVisibility(
            visible = state.generationPhase == GenerationPhase.ANSWER && state.answer.isNullOrBlank(),
            enter = fadeIn() + slideInVertically { it / 4 },
        ) {
            ResearchWaitingCard()
        }
        AnimatedVisibility(
            visible = !state.answer.isNullOrBlank(),
            enter = fadeIn() + slideInVertically { it / 4 },
        ) {
            AnswerBlock(state, onSpeak, onLearn)
        }
        if (state.generationPhase == GenerationPhase.DONE) {
            Text(
                "Neue Recherche starten",
                color = c.faint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onReset).padding(top = 12.dp, bottom = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ResearchWaitingCard() {
    val c = LocalAppPalette.current
    val transition = rememberInfiniteTransition(label = "research-hourglass")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hourglass-rotation",
    )
    GlassSurface(Modifier.fillMaxWidth().padding(top = 16.dp), radius = 22.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.HourglassTop,
                    contentDescription = "Animierte Eieruhr",
                    tint = c.accent,
                    modifier = Modifier.size(25.dp).graphicsLayer { rotationZ = rotation.value },
                )
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text("Bitte warten", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Antwort wird erzeugt …", color = c.muted, fontSize = 13.5.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun AnswerBlock(state: AppUiState, onSpeak: (String) -> Unit, onLearn: () -> Unit) {
    val c = LocalAppPalette.current
    GlassSurface(Modifier.fillMaxWidth().padding(top = 16.dp), radius = 22.dp) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(c.accent, c.accent2))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
                    Text("Antwort", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    Modifier.clip(RoundedCornerShape(99.dp)).background(c.chip).border(1.dp, c.border, RoundedCornerShape(99.dp)).clickable(enabled = state.generationPhase == GenerationPhase.DONE) { state.answer?.let { onSpeak(researchAnswerForSpeech(it)) } }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Campaign, null, tint = c.muted, modifier = Modifier.size(15.dp))
                    Text("Vorlesen", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
            if (state.generationPhase == GenerationPhase.ANSWER) {
                Text(
                    state.answer.orEmpty(),
                    color = c.text,
                    fontFamily = Newsreader,
                    fontSize = 16.5.sp,
                    lineHeight = 27.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                ResearchArticle(state.answer.orEmpty(), paragraphColor = c.text)
            }
            if (state.generationPhase == GenerationPhase.CARDS) {
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    LoadingRing(c.accent, 16.dp)
                    Text("Erzeuge Karteikarten …", color = c.muted, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
                }
            }
            if (state.generationPhase == GenerationPhase.DONE) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(16.dp)).background(c.green.copy(alpha = 0.11f)).border(1.dp, c.green.copy(alpha = 0.33f), RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CollectionsBookmark, null, tint = c.green, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("${state.savedCardCount} Karten gespeichert", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("in „${state.savedSessionTitle}“", color = c.muted, fontSize = 12.5.sp)
                    }
                    Text(
                        "Jetzt lernen",
                        color = c.onAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(c.green).clickable(onClick = onLearn).padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

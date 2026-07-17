package de.frank.karteikartenlernen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.karteikartenlernen.model.AppUiState
import de.frank.karteikartenlernen.model.CardStatus
import de.frank.karteikartenlernen.model.Flashcard
import de.frank.karteikartenlernen.model.SAMPLE_ANSWER
import de.frank.karteikartenlernen.model.learningSpeechText
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette
import de.frank.karteikartenlernen.ui.theme.Newsreader

@Composable
fun SessionDetail(
    state: AppUiState,
    onClose: () -> Unit,
    onResearchTab: (Boolean) -> Unit,
    onLearn: (List<Flashcard>) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteSession: () -> Unit,
    onReset: () -> Unit,
    readAloudEnabled: Boolean,
    onToggleReadAloud: () -> Unit,
) {
    val c = LocalAppPalette.current
    val session = state.selectedSession ?: return
    var showOptions by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.background1).safeDrawingPadding()) {
        Column(Modifier.fillMaxWidth().border(0.5.dp, c.border).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButtonBox(onClose) { Icon(Icons.Outlined.ArrowBackIosNew, "Zurück", tint = c.text, modifier = Modifier.size(20.dp)) }
                Text(session.title, color = c.text, fontFamily = Newsreader, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                IconButtonBox({ showOptions = true }, transparent = true) { Icon(Icons.Outlined.MoreHoriz, "Optionen", tint = c.faint, modifier = Modifier.size(20.dp)) }
            }
            val progress = session.known.toFloat() / session.count
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(c.field)) {
                    Box(Modifier.fillMaxWidth(progress).height(6.dp).background(Brush.horizontalGradient(listOf(c.accent, c.accent2))))
                }
                Text("${session.known}/${session.count} gewusst", color = c.muted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailButton("Alle lernen", true, Modifier.weight(1f)) { onLearn(state.cards) }
                DetailButton("Nur schwierige", false, Modifier.weight(1f)) { onLearn(state.cards.filter { it.status != CardStatus.KNOWN }) }
                IconButtonBox({ confirmReset = true }, size = 44) { Icon(Icons.Outlined.Refresh, "Lernstand zurücksetzen", tint = c.muted, modifier = Modifier.size(18.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(10.dp)).background(c.field).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailTab("Karten", !state.detailResearchTab, Modifier.weight(1f)) { onResearchTab(false) }
                DetailTab("Recherche", state.detailResearchTab, Modifier.weight(1f)) { onResearchTab(true) }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (state.detailResearchTab) ResearchDetail(state.detailQuestion, state.detailAnswer, readAloudEnabled, onToggleReadAloud) else state.cards.forEach { CardListItem(it, onDelete) }
            Spacer(Modifier.height(18.dp))
        }
    }
    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Lernsession verwalten") },
            text = {
                Column {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            showOptions = false
                            confirmReset = true
                        }.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Refresh, null, tint = c.muted, modifier = Modifier.size(20.dp))
                        Text("Lernstand zurücksetzen", color = c.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            showOptions = false
                            confirmDelete = true
                        }.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = c.red, modifier = Modifier.size(20.dp))
                        Text("Lernsession löschen", color = c.red, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showOptions = false }) { Text("Schließen") } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Lernstand zurücksetzen?") },
            text = { Text("Alle Karten dieser Session werden wieder als neu markiert.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("Zurücksetzen") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Lernsession löschen?") },
            text = { Text("Die gesamte Recherche, alle Karten und der Lernfortschritt dieser Session werden dauerhaft gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteSession()
                }) { Text("Endgültig löschen", color = c.red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun CardListItem(card: Flashcard, onDelete: (Long) -> Unit) {
    val c = LocalAppPalette.current
    val statusColor = when (card.status) { CardStatus.KNOWN -> c.green; CardStatus.UNKNOWN -> c.red; CardStatus.NEW -> c.neutral }
    GlassSurface(Modifier.fillMaxWidth().padding(bottom = 8.dp), radius = 14.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(statusColor, CircleShape))
            Text(card.question, color = c.text, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Icon(Icons.Outlined.DeleteOutline, "Karte löschen", tint = c.faint, modifier = Modifier.size(20.dp).clickable { onDelete(card.id) })
        }
    }
}

@Composable
private fun ResearchDetail(
    question: String,
    answer: String,
    readAloudEnabled: Boolean,
    onToggleReadAloud: () -> Unit,
) {
    val c = LocalAppPalette.current
    val speakerOrange = Color(0xFFFFA24C)
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Vollständige Recherche", color = c.faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                IconButtonBox(
                    onClick = onToggleReadAloud,
                    size = 36,
                    background = if (readAloudEnabled) speakerOrange.copy(alpha = 0.18f) else null,
                ) {
                    Icon(
                        if (readAloudEnabled) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                        if (readAloudEnabled) "Vorlesen ausschalten" else "Vorlesen einschalten",
                        tint = if (readAloudEnabled) speakerOrange else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            SelectionContainer {
                Column {
                    Text(question, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    ResearchArticle(answer)
                }
            }
        }
    }
}

@Composable
fun LearningOverlay(
    state: AppUiState,
    onClose: () -> Unit,
    onFlip: () -> Unit,
    onRate: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onSpeak: (String) -> Unit,
    onSpeakWithContinuation: (String, String) -> Unit,
    onContinueSpeaking: () -> Unit,
    narrationEnabled: Boolean,
    onNarrationEnabledChange: (Boolean) -> Unit,
    onStopSpeaking: () -> Unit,
) {
    val c = LocalAppPalette.current
    val learning = state.learning ?: return
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.background1, c.background0)))) {
        AuroraBackground()
        if (learning.done) {
            CompletionView(learning.known, learning.repeated, learning.deck.size, onRestart, onClose)
            return@Box
        }
        val card = learning.deck[learning.queue[learning.position]]
        LaunchedEffect(narrationEnabled, learning.position, learning.flipped) {
            if (narrationEnabled) {
                if (learning.flipped) onContinueSpeaking()
                else onSpeakWithContinuation(card.question, learningSpeechText(card, flipped = true))
            }
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButtonBox(onClose, size = 36) { Icon(Icons.Outlined.Close, "Schließen", tint = c.text, modifier = Modifier.size(20.dp)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(learning.title, color = c.faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${learning.position + 1} / ${learning.queue.size}", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                val speakerOrange = Color(0xFFFFA24C)
                IconButtonBox(
                    onClick = {
                        if (narrationEnabled) {
                            onNarrationEnabledChange(false)
                            onStopSpeaking()
                        } else {
                            onNarrationEnabledChange(true)
                            if (learning.flipped) onSpeak(learningSpeechText(card, flipped = true))
                        }
                    },
                    size = 36,
                    background = if (narrationEnabled) speakerOrange.copy(alpha = 0.18f) else null,
                ) {
                    Icon(
                        if (narrationEnabled) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                        if (narrationEnabled) "Automatisches Vorlesen ausschalten" else "Automatisches Vorlesen einschalten",
                        tint = if (narrationEnabled) speakerOrange else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                learning.queue.forEachIndexed { index, _ ->
                    Box(
                        Modifier.padding(horizontal = 2.5.dp).size(if (index == learning.position) 18.dp else 6.dp, 6.dp).clip(RoundedCornerShape(99.dp)).background(
                            when { index < learning.position -> c.green; index == learning.position -> c.accent; else -> c.field },
                        ),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                LearningCard(card, learning.flipped, learning.entering, learning.rating, state.settings.cardFont, onFlip)
            }
            Box(Modifier.fillMaxWidth().height(102.dp).padding(horizontal = 22.dp, vertical = 11.dp)) {
                if (learning.flipped) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RatingButton(false, Modifier.weight(1f)) { onRate(false) }
                        RatingButton(true, Modifier.weight(1f)) { onRate(true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningCard(card: Flashcard, flipped: Boolean, entering: Boolean, rating: Boolean?, fontSize: Int, onFlip: () -> Unit) {
    val c = LocalAppPalette.current
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(500, easing = FastOutSlowInEasing), label = "flip")
    val flight by animateFloatAsState(if (rating != null) 1f else 0f, tween(if (rating == true) 520 else 560), label = "flight")
    val modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 330.dp)
        .height(432.dp)
        .alpha(if (entering) 0f else 1f - flight)
        .scale(if (entering) 0.86f else 1f)
        .graphicsLayer {
            rotationY = rotation
            cameraDistance = 14f * density
            translationX = when (rating) { true -> size.width * flight; false -> -size.width * flight; null -> 0f }
            translationY = when (rating) { true -> -size.height * 0.6f * flight; false -> size.height * 0.3f * flight; null -> 0f }
            rotationZ = when (rating) { true -> 24f * flight; false -> -20f * flight; null -> 0f }
        }
        .clip(RoundedCornerShape(28.dp))
        .border(1.dp, c.borderHigh, RoundedCornerShape(28.dp))
        .clickable(enabled = !flipped, onClick = onFlip)
    Box(modifier) {
        if (rotation <= 90f) CardFront(card, fontSize) else Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { CardBack(card, fontSize) }
    }
}

@Composable
private fun CardFront(card: Flashcard, fontSize: Int) {
    val c = LocalAppPalette.current
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(if (c.dark) Color(0xFF20233A) else Color.White, c.surfaceSolid)))) {
        Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp).size(150.dp).background(c.accent.copy(alpha = 0.15f), CircleShape))
        Text(card.id.toString().padStart(2, '0'), color = c.text.copy(alpha = 0.05f), fontSize = 118.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp))
        Column(Modifier.fillMaxSize().padding(26.dp)) {
            CardLabel("FRAGE", c.accent, c.accent2)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Text(card.question, color = c.text, fontFamily = Newsreader, fontSize = (fontSize + 4).sp, lineHeight = ((fontSize + 4) * 1.42).sp, fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Flip, null, tint = c.faint, modifier = Modifier.size(15.dp))
                Text("Tippen zum Umdrehen", color = c.faint, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 7.dp))
            }
        }
    }
}

@Composable
private fun CardBack(card: Flashcard, fontSize: Int) {
    val c = LocalAppPalette.current
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(if (c.dark) Color(0xFF1C1E30) else Color.White, c.surfaceSolid)))) {
        Column(Modifier.fillMaxSize().padding(26.dp)) {
            CardLabel("ANTWORT", c.accent2, c.green)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(card.answer, color = c.text, fontFamily = Newsreader, fontSize = (fontSize + 2).sp, lineHeight = ((fontSize + 2) * 1.42).sp, fontWeight = FontWeight.SemiBold)
                Text(
                    card.explanation,
                    color = c.muted,
                    fontFamily = Newsreader,
                    fontSize = 14.5.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp)
                        .border(1.dp, c.accent2.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CardLabel(label: String, color: Color, dot: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(dot, CircleShape))
        Text(label, color = color, fontSize = 11.5.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CompletionView(known: Int, repeated: Int, total: Int, onRestart: () -> Unit, onClose: () -> Unit) {
    val c = LocalAppPalette.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        repeat(46) { index -> Confetti(index) }
        Column(Modifier.fillMaxWidth().safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val transition = rememberInfiniteTransition(label = "success")
            val scale by transition.animateFloat(1f, 1.06f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "scale")
            Box(Modifier.size(118.dp).scale(scale).background(Brush.linearGradient(listOf(c.green, c.accent2)), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(60.dp))
            }
            Text("Runde geschafft!", color = c.text, fontFamily = Newsreader, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp))
            Text("Du hast alle $total Karten gewusst.", color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ResultValue(known, "Gewusst", c.green)
                ResultValue(repeated, "Wiederholt", c.red)
            }
            Row(Modifier.fillMaxWidth().widthIn(max = 300.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailButton("Nochmal", false, Modifier.weight(1f), onRestart)
                DetailButton("Fertig", true, Modifier.weight(1f), onClose)
            }
        }
    }
}

@Composable
private fun Confetti(index: Int) {
    val c = LocalAppPalette.current
    val colors = listOf(c.accent, c.accent2, c.green, c.red, Color(0xFFFFD15C))
    val transition = rememberInfiniteTransition(label = "confetti$index")
    val fall by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400 + (index % 6) * 200, delayMillis = (index % 10) * 62), RepeatMode.Restart), label = "fall")
    Box(
        Modifier
            .size(9.dp, 14.dp)
            .graphicsLayer { rotationZ = (index * 47 % 360) * fall }
            .alpha(1f - fall)
            .background(colors[index % colors.size], RoundedCornerShape(2.dp))
            .then(Modifier)
            .offset(x = ((6 + index * 83) % 88).dp, y = (fall * 580).dp),
    )
}

@Composable
private fun ResultValue(value: Int, label: String, color: Color) {
    val c = LocalAppPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = color, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = c.faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RatingButton(known: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    val color = if (known) c.green else c.red
    Row(
        modifier.height(54.dp).clip(RoundedCornerShape(16.dp)).background(if (known) color else color.copy(alpha = 0.12f)).border(if (known) 0.dp else 1.5.dp, color, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (known) Icons.Outlined.Check else Icons.Outlined.Close, null, tint = if (known) c.onAccent else color, modifier = Modifier.size(20.dp))
        Text(if (known) "Gewusst" else "Nicht gewusst", color = if (known) c.onAccent else color, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp))
    }
}

@Composable
private fun IconButtonBox(
    onClick: () -> Unit,
    size: Int = 34,
    transparent: Boolean = false,
    background: Color? = null,
    content: @Composable () -> Unit,
) {
    val c = LocalAppPalette.current
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background ?: if (transparent) Color.Transparent else c.chip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DetailButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    Text(label, color = if (primary) c.onAccent else c.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = modifier.clip(RoundedCornerShape(12.dp)).background(if (primary) c.accent else c.chip).border(if (primary) 0.dp else 1.dp, c.border, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(11.dp))
}

@Composable
private fun DetailTab(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    Text(label, color = if (active) c.text else c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = modifier.clip(RoundedCornerShape(8.dp)).background(if (active) c.surfaceSolid else Color.Transparent).clickable(onClick = onClick).padding(8.dp))
}

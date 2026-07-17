package de.frank.karteikartenlernen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.karteikartenlernen.model.AppUiState
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette

@Composable
fun ModelSheet(state: AppUiState, onClose: () -> Unit, onModel: (String) -> Unit, onReasoning: (String) -> Unit) {
    val c = LocalAppPalette.current
    AppModalBottomSheet(state.showModelSheet, onClose) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            SheetLabel("MODELL")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("GPT 5.6 Soul", "GPT 5.6 Terra", "GPT 5.6 Luna").forEach { model ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (state.model == model) c.chip else Color.Transparent).border(1.dp, if (state.model == model) c.accent else c.border, RoundedCornerShape(14.dp)).clickable { onModel(model) }.padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(model, color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        if (state.model == model) Icon(Icons.Outlined.Check, null, tint = c.accent, modifier = Modifier.size(17.dp))
                    }
                }
            }
            SheetLabel("REASONING", Modifier.padding(top = 20.dp))
            SegmentedControl(listOf("Niedrig", "Mittel", "Hoch"), state.reasoning, onReasoning, Modifier.fillMaxWidth())
            Spacer(Modifier.padding(bottom = 26.dp))
        }
    }
}

@Composable
fun CrossSheet(state: AppUiState, onDecision: (Int, Boolean) -> Unit, onClose: () -> Unit) {
    if (!state.showCrossSheet) return
    val c = LocalAppPalette.current
    BottomSheetBackdrop({}) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(c.background1).border(1.dp, c.border, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).padding(horizontal = 18.dp, vertical = 12.dp)) {
            Handle()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, null, tint = c.accent, modifier = Modifier.size(22.dp))
                Text("Passt auch woanders", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
            Text("Einige neue Karten passen thematisch zu bestehenden Sessions. Als Kopie hinzufügen?", color = c.muted, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.crossSuggestions.forEachIndexed { index, suggestion ->
                    GlassSurface(Modifier.fillMaxWidth(), radius = 16.dp) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(suggestion.title, color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("${suggestion.count} passende Karten", color = c.faint, fontSize = 12.5.sp)
                            }
                            when (suggestion.accepted) {
                                null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DecisionButton(false) { onDecision(index, false) }
                                    DecisionButton(true) { onDecision(index, true) }
                                }
                                true -> Text("Hinzugefügt", color = c.green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                false -> Text("Abgelehnt", color = c.faint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Text("Fertig", color = c.background0, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp).clip(RoundedCornerShape(14.dp)).background(c.text).clickable(onClick = onClose).padding(14.dp))
        }
    }
}

@Composable
fun OAuthDialog(state: AppUiState, onClose: () -> Unit, onLogin: () -> Unit, onOpenAuthPage: () -> Unit) {
    if (!state.showOAuth) return
    val c = LocalAppPalette.current
    Box(Modifier.fillMaxSize().safeDrawingPadding().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClose).padding(22.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 320.dp).clip(RoundedCornerShape(22.dp)).background(c.surfaceSolid).border(1.dp, c.border, RoundedCornerShape(22.dp)).clickable {} ,
        ) {
            Row(Modifier.fillMaxWidth().background(c.field).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(c.green, CircleShape))
                Icon(Icons.Outlined.Lock, null, tint = c.muted, modifier = Modifier.padding(start = 8.dp).size(13.dp))
                Text("auth.openai.com", color = c.muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(c.accent, c.accent2))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Key, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text("Bei OpenAI anmelden", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                Text("Karteikarten Lernen verbindet sich per OpenAI-Gerätecode mit deinen GPT-/Codex-Modellen – ganz ohne API-Schlüssel.", color = c.muted, fontSize = 13.5.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
                state.authError?.let { Text(it, color = c.red, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp)) }
                if (state.authUserCode != null) {
                    Text(
                        state.authUserCode,
                        color = c.accent,
                        fontSize = 25.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.field)
                            .border(1.dp, c.border, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        textAlign = TextAlign.Center,
                    )
                    Text("Diesen Code auf der geöffneten OpenAI-Seite eingeben.", color = c.muted, fontSize = 12.5.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Text("OpenAI-Seite erneut öffnen", color = c.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpenAuthPage).padding(vertical = 12.dp))
                    Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        LoadingRing(c.accent, 16.dp)
                        Text("Warte auf Bestätigung …", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.text).clickable(enabled = !state.authBusy, onClick = onLogin).padding(13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.authBusy) {
                            LoadingRing(c.background0, 16.dp)
                            Text("Gerätecode wird angefordert …", color = c.background0, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        } else Text("Mit ChatGPT fortfahren", color = c.background0, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Abbrechen", color = c.faint, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, c.border, RoundedCornerShape(12.dp)).clickable(onClick = onClose).padding(12.dp))
            }
        }
    }
}

@Composable
private fun BottomSheetBackdrop(onClose: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClose), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().clickable {}) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(visible: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    if (!visible) return
    val c = LocalAppPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = c.background1,
        contentColor = c.text,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = { Handle() },
    ) {
        content()
    }
}

@Composable
private fun Handle() {
    val c = LocalAppPalette.current
    Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(38.dp, 4.dp).background(c.border, RoundedCornerShape(99.dp)))
    }
}

@Composable
private fun SheetLabel(label: String, modifier: Modifier = Modifier) {
    val c = LocalAppPalette.current
    Text(label, color = c.faint, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, modifier = modifier.padding(bottom = 10.dp))
}

@Composable
private fun DecisionButton(accept: Boolean, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(if (accept) c.green else c.chip).border(if (accept) 0.dp else 1.dp, c.border, RoundedCornerShape(11.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(if (accept) Icons.Outlined.Check else Icons.Outlined.Close, null, tint = if (accept) c.onAccent else c.muted, modifier = Modifier.size(18.dp))
    }
}

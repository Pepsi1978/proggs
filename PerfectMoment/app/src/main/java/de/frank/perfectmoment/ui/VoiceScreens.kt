package de.frank.perfectmoment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.perfectmoment.tts.ClonedVoice
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.JetBrainsMono
import de.frank.perfectmoment.ui.theme.LocalPmColors

/** The account's own cloned voices, with the way to record another one. */
@Composable
fun MyVoicesScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Meine Stimmen",
            viewModel::back,
            action = {
                Icon(
                    Icons.Outlined.Add,
                    "Neue Stimme aufzeichnen",
                    tint = colors.gold,
                    modifier = Modifier.size(26.dp).pmClickable { viewModel.openVoiceRecorder() },
                )
            },
            titleSize = 24,
        )
        if (viewModel.qwenVoicesLoading) {
            Text(
                "Stimmen werden geladen …",
                color = colors.text3,
                fontFamily = Inter,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            )
        } else if (viewModel.qwenVoices.isEmpty()) {
            Text(
                "Noch keine eigene Stimme. Tippe oben auf das Plus und lies etwa 50 Sekunden vor.",
                color = colors.text2,
                fontFamily = Inter,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(viewModel.qwenVoices, key = ClonedVoice::id) { voice ->
                val selected = viewModel.qwenVoiceId == voice.id
                val shape = RoundedCornerShape(20.dp)
                PmCard(
                    Modifier.fillMaxWidth().height(72.dp)
                        .pmClickable(shape = shape, lift = true) { viewModel.selectQwenVoice(voice) }
                        .then(
                            if (selected) {
                                Modifier.shadow(
                                    12.dp,
                                    shape,
                                    ambientColor = colors.gold.copy(alpha = 0.24f),
                                    spotColor = colors.gold.copy(alpha = 0.24f),
                                ).border(3.dp, colors.goldHi, shape)
                            } else {
                                Modifier
                            },
                        ),
                    radius = 20,
                    color = if (selected) colors.surface2 else colors.surface,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                viewModel.voiceTitle(voice),
                                color = colors.text1,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                            )
                            Text(
                                voice.createdAt,
                                color = colors.text3,
                                fontFamily = Inter,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Icon(
                            Icons.Outlined.Edit,
                            "Namen ändern",
                            tint = colors.text3,
                            modifier = Modifier.size(34.dp).pmClickable(enabled = !viewModel.voiceBusy) {
                                viewModel.startRenamingVoice(voice)
                            }.padding(6.dp),
                        )
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            "Stimme löschen",
                            tint = colors.warning,
                            modifier = Modifier.size(34.dp).pmClickable(enabled = !viewModel.voiceBusy) {
                                viewModel.askToDeleteVoice(voice)
                            }.padding(6.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        CheckMark(selected)
                    }
                }
            }
        }
    }
    if (viewModel.renamedVoiceId != null) RenameVoiceDialog(viewModel)
    viewModel.voiceToDelete?.let { voice -> DeleteVoiceDialog(viewModel, voice) }
}

@Composable
private fun RenameVoiceDialog(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .pmClickable(onClick = viewModel::cancelRenamingVoice),
        contentAlignment = Alignment.Center,
    ) {
        PmCard(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
            Column(Modifier.padding(20.dp)) {
                SectionLabel("Name der Stimme")
                PmTextArea(
                    value = viewModel.renameVoiceText,
                    onValueChange = viewModel::updateRenameVoiceText,
                    placeholder = "Name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 10.dp),
                    textSize = 16,
                    radius = 20,
                )
                Text(
                    "Der Name gilt in dieser App. Bei Alibaba behält die Stimme ihre Kennung.",
                    color = colors.text3,
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 16.dp),
                )
                PrimaryButton("Speichern", viewModel::confirmRenamingVoice, height = 48, textSize = 14)
                Spacer(Modifier.height(10.dp))
                OutlineButton("Abbrechen", colors.text2, viewModel::cancelRenamingVoice)
            }
        }
    }
}

@Composable
private fun DeleteVoiceDialog(viewModel: AppViewModel, voice: ClonedVoice) {
    val colors = LocalPmColors.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .pmClickable(onClick = viewModel::cancelDeletingVoice),
        contentAlignment = Alignment.Center,
    ) {
        PmCard(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "„${viewModel.voiceTitle(voice)}“ endgültig löschen? Die Stimme wird bei Alibaba " +
                        "entfernt und lässt sich nur durch eine neue Aufnahme zurückholen.",
                    color = colors.text1,
                    fontFamily = Inter,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Abbrechen", viewModel::cancelDeletingVoice, height = 48, textSize = 14)
                Spacer(Modifier.height(10.dp))
                OutlineButton("Löschen", colors.warning, viewModel::confirmDeletingVoice)
            }
        }
    }
}

/**
 * Records a voice sample and turns it into a cloned voice.
 *
 * The questions appear before recording starts so they can be read straight through, which is
 * what makes the sample sound like the app's own tone instead of a test sentence.
 */
@Composable
fun VoiceRecorderScreen(
    viewModel: AppViewModel,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
) {
    val colors = LocalPmColors.current
    val recording = viewModel.voiceRecorderState == VoiceRecorderState.RECORDING
    val saving = viewModel.voiceRecorderState == VoiceRecorderState.SAVING
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Neue Stimme aufzeichnen", viewModel::back, titleSize = 24)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SectionLabel("Name der Stimme")
            PmTextArea(
                value = viewModel.voiceRecorderName,
                onValueChange = viewModel::updateVoiceRecorderName,
                placeholder = "z. B. Frank Abend",
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 8.dp),
                textSize = 16,
                radius = 20,
            )
            Spacer(Modifier.height(20.dp))
            SectionLabel("Das liest du vor")
            Text(
                if (viewModel.voiceRecorderQuestionsLoading) {
                    "Du kannst sofort loslegen — passendere Fragen kommen noch nach."
                } else {
                    "Ruhig und warm, gleichmäßig bis zum Schluss — etwa 50 Sekunden."
                },
                color = colors.text3,
                fontFamily = Inter,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            viewModel.voiceRecorderQuestions.forEachIndexed { index, question ->
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Text(
                        "${index + 1}",
                        color = colors.gold,
                        fontFamily = JetBrainsMono,
                        fontSize = 15.sp,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        question,
                        color = colors.text1,
                        fontFamily = Inter,
                        fontSize = 19.sp,
                        lineHeight = 28.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            viewModel.voiceRecorderMessage?.let { note ->
                Text(
                    note,
                    color = colors.warning,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Text(
                when {
                    saving -> "Die Stimme wird erstellt …"
                    recording -> "%d:%02d".format(viewModel.voiceRecorderSeconds / 60, viewModel.voiceRecorderSeconds % 60)
                    else -> "Bereit"
                },
                color = if (recording) colors.goldHi else colors.text2,
                fontFamily = JetBrainsMono,
                fontSize = if (recording) 26.sp else 14.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Box(
                Modifier.size(84.dp)
                    .pmClickable(enabled = !saving, shape = CircleShape, lift = true) {
                        viewModel.onVoiceRecorderTapped(
                            microphonePermissionGranted,
                            requestMicrophonePermission,
                        )
                    }
                    .background(if (recording) colors.amber else colors.surface2, CircleShape)
                    .border(2.dp, if (recording) colors.amber else colors.gold.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (recording) Icons.Outlined.Stop else Icons.Outlined.Mic,
                    if (recording) "Aufnahme stoppen" else "Aufnahme starten",
                    tint = if (recording) colors.background else colors.gold,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

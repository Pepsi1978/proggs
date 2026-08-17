package de.frank.perfectmoment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.frank.perfectmoment.data.local.QuestionEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.PmTextStyles

/**
 * Der eigene Bereich unter dem Verlauf: Verläufe, deren Fragen nicht von der KI stammen, sondern
 * von Hand geschrieben wurden. Jeder davon lässt sich beliebig oft anlegen und beliebig weit
 * füllen; vorgelesen wird er mit derselben Taktung wie jeder andere Verlauf.
 */
@Composable
fun ReadingSection(
    viewModel: AppViewModel,
    reorder: ReorderState,
    onDeleteRequest: (SessionEntity) -> Unit,
) {
    val colors = LocalPmColors.current
    Column(Modifier.fillMaxWidth().padding(top = 26.dp)) {
        SectionLabel("Eigene Sessions", Modifier.padding(start = 8.dp, bottom = 6.dp), decorated = true)
        Text(
            "Sessions mit eigenen Fragen — ohne KI, genau so vorgelesen, wie du sie schreibst.",
            color = colors.text3,
            fontFamily = Inter,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        )
        viewModel.readingSessions.forEach { session ->
            key(session.id) {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .reorderRow(reorder, session.id),
                ) {
                    ReadingRow(
                        session = session,
                        onClick = { viewModel.openReadingDetail(session.id) },
                        onDelete = { onDeleteRequest(session) },
                        dragModifier = reorderHandle(
                            state = reorder,
                            id = session.id,
                            order = { viewModel.readingSessions.map(SessionEntity::id) },
                            onMove = viewModel::moveReadingSession,
                            onDrop = viewModel::persistReadingSessionOrder,
                        ),
                    )
                }
            }
        }
        AddCard("Neuer Vorlese-Verlauf", viewModel::createReadingSession)
    }
}

@Composable
private fun ReadingRow(
    session: SessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier,
) {
    val colors = LocalPmColors.current
    PmCard(
        Modifier.fillMaxWidth()
            .pmClickable(shape = RoundedCornerShape(HISTORY_ROW_RADIUS.dp), lift = true, onClick = onClick),
        radius = HISTORY_ROW_RADIUS,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(colors.gold.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.MenuBook, null, tint = colors.goldHi, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    historyDisplayTitle(session).ifBlank { AppViewModel.NEW_READING_TITLE },
                    color = colors.text1,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${session.questionCount} Fragen · ${formatSessionDuration(session.durationMin)} · ${session.reps}×",
                    color = colors.text2,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Icon(
                Icons.Outlined.DragIndicator,
                "Zum Sortieren halten und ziehen",
                tint = colors.text3,
                modifier = dragModifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Outlined.Delete,
                "Vorlese-Verlauf löschen",
                tint = colors.text3,
                modifier = Modifier.size(22.dp).pmClickable(onClick = onDelete),
            )
        }
    }
}

@Composable
private fun AddCard(label: String, onClick: () -> Unit) {
    val colors = LocalPmColors.current
    PmCard(
        Modifier.fillMaxWidth()
            .pmClickable(shape = RoundedCornerShape(HISTORY_ROW_RADIUS.dp), lift = true, onClick = onClick),
        radius = HISTORY_ROW_RADIUS,
        color = colors.surface2,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(colors.gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, null, tint = colors.background, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = colors.goldHi,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
        }
    }
}

/**
 * Ein einzelner Vorlese-Verlauf: derselbe Aufbau wie das Verlaufs-Detail — Taktung, Stimme,
 * Zufallsfolge — nur dass die Fragen hier von Hand entstehen statt von der KI.
 */
@Composable
fun ReadingDetailScreen(
    viewModel: AppViewModel,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
) {
    val colors = LocalPmColors.current
    val detail = viewModel.historyDetail
    val reorder = rememberReorderState()
    val listState = rememberLazyListState()
    ReorderAutoScroll(reorder, listState)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", viewModel::back)
        if (detail == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
        } else {
            LazyColumn(
                Modifier.reorderViewport(reorder),
                state = listState,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            ) {
                item {
                    HistoryTitleCard(
                        title = viewModel.historyTitleDraft,
                        fallback = AppViewModel.NEW_READING_TITLE,
                        onTitleChange = viewModel::updateHistoryTitle,
                        onDone = viewModel::commitHistoryTitle,
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Vorlesen starten", viewModel::playReadingSession)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ParameterCard("Pause", "${viewModel.pauseRep} s", { viewModel.openSheet(AppSheet.PAUSES) }, Modifier.weight(1f), compact = true)
                        ParameterCard("Wiederholungen", "${viewModel.repetitions}×", { viewModel.openSheet(AppSheet.REPETITIONS) }, Modifier.weight(1.2f), compact = true)
                        ParameterCard("Dauer", formatSessionDuration(viewModel.durationMinutes), { viewModel.openSheet(AppSheet.DURATION) }, Modifier.weight(1f), compact = true)
                    }
                    PmCard(
                        Modifier.fillMaxWidth().padding(top = 12.dp)
                            .pmClickable(shape = RoundedCornerShape(32.dp), lift = true) {
                                viewModel.openHistoryVoicePicker()
                            },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Stimme für diesen Verlauf", color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(
                                    viewModel.historyVoiceLabel,
                                    color = colors.text3,
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            Icon(Icons.Outlined.ChevronRight, null, tint = colors.text3, modifier = Modifier.size(20.dp))
                        }
                    }
                    PmCard(
                        Modifier.fillMaxWidth().padding(top = 12.dp)
                            .pmClickable(shape = RoundedCornerShape(32.dp), lift = true) {
                                viewModel.toggleRandomReplay()
                            },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Zufällige Reihenfolge", color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(
                                    "Fragen werden gemischt vorgelesen",
                                    color = colors.text3,
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            PmSwitch(viewModel.randomReplay) { viewModel.toggleRandomReplay() }
                        }
                    }
                    SectionLabel(
                        "Fragen (${detail.questions.size})",
                        Modifier.padding(start = 8.dp, top = 26.dp, bottom = 14.dp),
                        decorated = true,
                    )
                    if (detail.questions.isEmpty()) {
                        Text(
                            "Noch keine Frage. Über das Plus kommt die erste dazu — getippt oder diktiert.",
                            color = colors.text3,
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        )
                    }
                }
                items(detail.questions, key = QuestionEntity::id) { question ->
                    Box(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            .reorderRow(reorder, question.id),
                    ) {
                        PmCard(
                            Modifier.fillMaxWidth()
                                .pmClickable(shape = RoundedCornerShape(24.dp), lift = true) {
                                    viewModel.openReadingQuestionEditor(question.id, question.text)
                                },
                            radius = 24,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(question.emoji, fontSize = 20.sp, lineHeight = 28.sp)
                                Text(
                                    question.text,
                                    color = colors.text2,
                                    style = PmTextStyles.question,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Outlined.DragIndicator,
                                    "Zum Sortieren halten und ziehen",
                                    tint = colors.text3,
                                    modifier = reorderHandle(
                                        state = reorder,
                                        id = question.id,
                                        order = { viewModel.readingQuestions.map(QuestionEntity::id) },
                                        onMove = viewModel::moveReadingQuestion,
                                        onDrop = viewModel::persistReadingQuestionOrder,
                                    ).size(20.dp),
                                )
                                Icon(
                                    Icons.Outlined.Delete,
                                    "Frage löschen",
                                    tint = colors.text3,
                                    modifier = Modifier.size(20.dp)
                                        .pmClickable { viewModel.deleteReadingQuestion(question.id) },
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    AddCard("Frage hinzufügen") { viewModel.openReadingQuestionEditor(null) }
                }
            }
        }
    }
    if (viewModel.readingQuestionOpen) {
        ReadingQuestionDialog(viewModel, microphonePermissionGranted, requestMicrophonePermission)
    }
    if (viewModel.readingLinkOpen) {
        ReadingLinkDialog(viewModel)
    }
}

/**
 * Hängt die offene Frage wortwörtlich — samt Symbol — ans Ende der angehakten eigenen Sessions.
 * Das Original bleibt dabei stehen, wo es steht.
 */
@Composable
private fun ReadingLinkDialog(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    val sessions = viewModel.readingLinkSessions
    Dialog(onDismissRequest = viewModel::closeReadingLinkPicker) {
        PmCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                SectionLabel("In Sessions kopieren", Modifier.padding(bottom = 8.dp), decorated = true)
                Text(
                    "Die Frage wird eins zu eins ans Ende der gewählten eigenen Sessions gehängt.",
                    color = colors.text3,
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                if (sessions.isEmpty()) {
                    Text(
                        "Es gibt noch keine weitere eigene Session.",
                        color = colors.text3,
                        fontFamily = Inter,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(sessions, key = SessionEntity::id) { session ->
                            val checked = session.id in viewModel.readingLinkTargets
                            Row(
                                Modifier.fillMaxWidth()
                                    .pmClickable(shape = RoundedCornerShape(16.dp)) {
                                        viewModel.toggleReadingLinkTarget(session.id)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(24.dp).background(
                                        if (checked) colors.gold else colors.surface2,
                                        CircleShape,
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (checked) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            null,
                                            tint = colors.background,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        historyDisplayTitle(session).ifBlank { AppViewModel.NEW_READING_TITLE },
                                        color = colors.text1,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${session.questionCount} Fragen",
                                        color = colors.text3,
                                        fontFamily = Inter,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Kopieren", viewModel::copyReadingQuestionToSessions, height = 50)
                Spacer(Modifier.height(10.dp))
                OutlineButton(
                    "Abbrechen",
                    colors.text2,
                    viewModel::closeReadingLinkPicker,
                    Modifier.fillMaxWidth(),
                    height = 46,
                )
            }
        }
    }
}

@Composable
private fun ReadingQuestionDialog(
    viewModel: AppViewModel,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
) {
    val colors = LocalPmColors.current
    val existing = viewModel.readingQuestionId
    Dialog(onDismissRequest = viewModel::closeReadingQuestionEditor) {
        PmCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(
                        if (existing == null) "Neue Frage" else "Frage bearbeiten",
                        Modifier.weight(1f),
                        decorated = true,
                    )
                    if (existing != null) {
                        Icon(
                            Icons.Outlined.Link,
                            "Frage in andere Sessions kopieren",
                            tint = colors.goldHi,
                            modifier = Modifier.size(24.dp)
                                .pmClickable(onClick = viewModel::openReadingLinkPicker),
                        )
                    }
                }
                PmTextArea(
                    value = viewModel.readingQuestionText,
                    onValueChange = viewModel::updateReadingQuestionText,
                    placeholder = "Was soll vorgelesen werden?",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 260.dp),
                )
                Spacer(Modifier.height(8.dp))
                RecorderControl(
                    state = if (viewModel.recordingTarget == RecordingTarget.READING_QUESTION) {
                        viewModel.recordingState
                    } else {
                        RecordingState.IDLE
                    },
                    message = viewModel.recordingMessage,
                    onClick = {
                        viewModel.onMicTapped(
                            RecordingTarget.READING_QUESTION,
                            microphonePermissionGranted,
                            requestMicrophonePermission,
                        )
                    },
                    scale = 0.8f,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Speichern", viewModel::saveReadingQuestion, height = 50)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton(
                        "Abbrechen",
                        colors.text2,
                        viewModel::closeReadingQuestionEditor,
                        Modifier.weight(1f),
                        height = 46,
                    )
                    if (existing != null) {
                        OutlineButton(
                            "Löschen",
                            colors.warning,
                            { viewModel.deleteReadingQuestion(existing) },
                            Modifier.weight(1f),
                            height = 46,
                        )
                    }
                }
            }
        }
    }
}

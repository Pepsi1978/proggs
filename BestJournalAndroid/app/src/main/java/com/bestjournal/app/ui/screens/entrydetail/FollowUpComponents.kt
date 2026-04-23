package com.bestjournal.app.ui.screens.entrydetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.bestjournal.app.data.local.entity.EntryFollowUpEntity
import com.bestjournal.app.ui.components.AnimatedMicButton
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.WaveformVisualizer
import com.bestjournal.app.ui.screens.journal.RecordingState
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.util.DateTimeFormatter
import com.bestjournal.app.util.rememberHapticAction
import kotlinx.coroutines.delay

/**
 * Inline card for a single existing Nachtrag. Mirrors Frank's layout:
 * amber Book header with "Nachtrag <n as German word>", delete icon,
 * created/updated timestamp row, optional Verbessert/Original tab pair
 * with swipe-between-versions, editable TextField, and a "Mit KI
 * nachtraeglich verbessern" OutlinedButton when the follow-up has no
 * improved version yet.
 *
 * All edits go to the ViewModel; the composable does not own state other
 * than the currently-visible tab index.
 */
@Composable
fun FollowUpInlineCard(
    index: Int,
    followUp: EntryFollowUpEntity,
    onRawTextChanged: (String) -> Unit,
    onImprovedTextChanged: (String) -> Unit,
    onImproveClick: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    val fuHasImproved = followUp.isImproved && !followUp.improvedText.isNullOrBlank()
    var selectedTabFu by remember(followUp.id, fuHasImproved) { mutableIntStateOf(0) }
    val isShowingOriginalFu = selectedTabFu == 1 && fuHasImproved
    val doHaptic = rememberHapticAction()
    val context = LocalContext.current

    val fuFieldColors =
        TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        )

    GlassCard(
        modifier =
            Modifier.fillMaxWidth().then(
                if (fuHasImproved)
                    Modifier.pointerInput(followUp.id) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -40) selectedTabFu = 1
                            if (dragAmount > 40) selectedTabFu = 0
                        }
                    }
                else Modifier
            ),
        glowColor = NeonAmber,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Book,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = NeonAmber,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Nachtrag ${germanNumberWord(index + 1)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonAmber,
                    )
                }
                IconButton(
                    onClick = {
                        doHaptic(HapticFeedbackType.LongPress)
                        onDeleteRequested()
                    }
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Nachtrag löschen",
                        tint = NeonRed,
                    )
                }
            }
            Text(
                "${DateTimeFormatter.formatFull(followUp.createdAt)} · ${DateTimeFormatter.formatRelative(context, followUp.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp).offset(y = (-8).dp),
            )

            if (fuHasImproved) {
                TabRow(
                    selectedTabIndex = selectedTabFu,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTabFu < tabPositions.size) {
                            SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabFu]),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                ) {
                    Tab(selected = selectedTabFu == 0, onClick = { selectedTabFu = 0 }) {
                        Text(
                            "Verbessert",
                            modifier = Modifier.padding(8.dp),
                            color =
                                if (selectedTabFu == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                        )
                    }
                    Tab(selected = selectedTabFu == 1, onClick = { selectedTabFu = 1 }) {
                        Text(
                            "Original",
                            modifier = Modifier.padding(8.dp),
                            color =
                                if (selectedTabFu == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = isShowingOriginalFu,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "followup_tab_slide",
            ) { showOriginal ->
                if (showOriginal) {
                    TextField(
                        value = followUp.rawText,
                        onValueChange = onRawTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                        colors = fuFieldColors,
                    )
                } else {
                    val displayValue =
                        if (fuHasImproved) (followUp.improvedText ?: followUp.text)
                        else followUp.rawText
                    TextField(
                        value = displayValue,
                        onValueChange = {
                            if (fuHasImproved) onImprovedTextChanged(it)
                            else onRawTextChanged(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                        colors = fuFieldColors,
                    )
                }
            }

            if (!fuHasImproved && followUp.rawText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onImproveClick,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Mit KI nachträglich verbessern",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/**
 * The "Nachtrag hinzufuegen" card. Amber glow, EditNote icon, "Hinzufuegen"
 * button on the right. Carries the "* ab dem zweiten Nachtrag"-Premium hint
 * directly inside the card so the user sees immediately where the free tier
 * ends before tapping the button.
 */
@Composable
fun AddFollowUpCard(onAddClick: () -> Unit) {
    val doHaptic = rememberHapticAction()
    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonAmber) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Nachtrag",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Button(
                    onClick = {
                        doHaptic(HapticFeedbackType.LongPress)
                        onAddClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Hinzufügen", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = NeonAmber,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "ab dem zweiten Nachtrag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Mode-picker + record + write dialog for creating/editing a Nachtrag. Mirrors
 * Frank's FollowUpDialog 1:1. The dialog starts in "choose input mode" mode
 * (Schreiben/Einsprechen) unless rawText is already non-empty; then it goes
 * straight into text editing with an "Einsprechen"-pill in the top-right.
 *
 * During recording the dialog shows a large duration display, WaveformVisualizer,
 * engine label (Groq/local), and the AnimatedMicButton in Stop mode. During
 * transcription a progress row is shown. In preview mode the editable TextField
 * appears with an "Einsprechen" pill, an optional "Text verbessern" button,
 * and the Verbessert/Original toggle when the improved version is ready.
 */
@Composable
fun FollowUpDialog(
    rawText: String,
    improvedText: String?,
    recordingState: RecordingState,
    amplitude: Float,
    durationSeconds: Int,
    isImproving: Boolean,
    isUsingImproved: Boolean,
    canDelete: Boolean,
    engineLabel: String,
    onImproveClick: () -> Unit,
    onToggleVersion: (Boolean) -> Unit,
    onTextEdit: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRecordClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val isRecording = recordingState == RecordingState.RECORDING
    val isTranscribing = recordingState == RecordingState.TRANSCRIBING
    val showingImproved = isUsingImproved && improvedText != null
    val displayText = if (showingImproved) improvedText!! else rawText
    var inputModeChosen by remember(rawText) { mutableStateOf(rawText.isNotBlank()) }
    val dialogFocusManager = LocalFocusManager.current
    val dialogFocusRequester = remember { FocusRequester() }
    var dialogLastEditTime by remember { mutableLongStateOf(0L) }
    var dialogIsFocused by remember { mutableStateOf(false) }

    LaunchedEffect(inputModeChosen, rawText, isRecording, isTranscribing) {
        if (inputModeChosen && rawText.isBlank() && !isRecording && !isTranscribing) {
            delay(300)
            dialogFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(dialogLastEditTime) {
        if (dialogLastEditTime > 0 && dialogIsFocused) {
            delay(5000)
            dialogFocusManager.clearFocus()
        }
    }

    val dialogImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(dialogImeVisible) {
        if (!dialogImeVisible && dialogIsFocused) {
            dialogFocusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRecording && !isTranscribing) {
                dialogFocusManager.clearFocus()
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Book,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nachtrag", color = MaterialTheme.colorScheme.onSurface)
                }
                if (canDelete && !isRecording && !isTranscribing) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Nachtrag löschen",
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        },
        text = {
            Column {
                when {
                    isTranscribing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                "Nachtrag wird transkribiert…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    isRecording -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Sprich deinen Nachtrag ein.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ) {
                                Column(
                                    modifier =
                                        Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        DateTimeFormatter.formatDuration(durationSeconds),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    WaveformVisualizer(amplitude = amplitude)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        engineLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AnimatedMicButton(isRecording = true, onClick = onRecordClick)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tippe auf Stop, wenn du fertig bist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    !inputModeChosen -> {
                        Text(
                            "Wie möchtest du deinen Nachtrag hinzufügen?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = { inputModeChosen = true },
                                    modifier = Modifier.size(56.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                    elevation =
                                        FloatingActionButtonDefaults.elevation(
                                            defaultElevation = 8.dp
                                        ),
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Schreiben",
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Schreiben",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier =
                                    Modifier.height(32.dp)
                                        .width(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedMicButton(isRecording = false, onClick = onRecordClick)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Einsprechen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Dein Nachtrag:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Surface(
                                onClick = onRecordClick,
                                shape = RoundedCornerShape(20.dp),
                                color =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Mic,
                                        contentDescription = "Einsprechen",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Einsprechen",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = displayText,
                            onValueChange = { newText ->
                                dialogLastEditTime = System.currentTimeMillis()
                                onTextEdit(newText)
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 300.dp)
                                    .focusRequester(dialogFocusRequester)
                                    .onFocusChanged { state ->
                                        dialogIsFocused = state.isFocused
                                        if (state.isFocused) {
                                            dialogLastEditTime = System.currentTimeMillis()
                                        }
                                    },
                            textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                ),
                            placeholder = {
                                Text(
                                    "Schreibe hier deinen Nachtrag…",
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (improvedText != null && !isImproving) {
                            OutlinedButton(
                                onClick = { onToggleVersion(!showingImproved) },
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                            ) {
                                Text(
                                    if (showingImproved) "↩ Original anzeigen"
                                    else "✨ Verbesserte Version anzeigen"
                                )
                            }
                        }
                        if (improvedText == null && !isImproving && displayText.isNotBlank()) {
                            Button(
                                onClick = onImproveClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor =
                                            MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                            ) {
                                Text("✨ Text verbessern")
                            }
                        }
                        if (isImproving) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    "KI verbessert den Nachtrag…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (displayText.isNotBlank() && !isRecording && !isTranscribing) {
                Button(onClick = onSave) {
                    Text(if (showingImproved) "Verbessert speichern" else "Speichern")
                }
            }
        },
        dismissButton = {
            if (!isRecording && !isTranscribing) {
                OutlinedButton(onClick = onDismiss) {
                    Text(
                        if (displayText.isBlank()) "Abbrechen" else "Verwerfen",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/**
 * Confirm-before-delete dialog for an inline Nachtrag. Titles the specific
 * Nachtrag by its German ordinal ("Nachtrag Drei löschen?") for clarity.
 */
@Composable
fun FollowUpDeleteConfirmDialog(
    followUpNumber: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = "Nachtrag ${germanNumberWord(followUpNumber)}"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title löschen?", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Text(
                "Möchtest du $title wirklich unwiderruflich löschen?",
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = NeonRed,
                        contentColor = Color.White,
                    ),
            ) {
                Text("Löschen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/**
 * Premium upsell dialog that triggers when the user tries to add a second
 * Nachtrag without an active subscription. Mirrors the retrospective
 * upsell look: feature title, short description of the benefit, "Abo
 * starten" + "Später" buttons.
 */
@Composable
fun FollowUpPremiumUpsellDialog(
    onStartSubscription: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Unbegrenzte Nachträge",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column {
                Text(
                    "Mit Premium kannst du zu jedem Tagebucheintrag so viele Nachträge hinzufügen wie du möchtest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Jeder Nachtrag fließt automatisch ins Dashboard ein, damit deine Analyse immer vollständig bleibt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onStartSubscription) {
                Text("Abo starten")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(
                    "Später entscheiden",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/**
 * Spells a positive integer as a capitalised German word for the follow-up
 * label ("Nachtrag Eins", "Nachtrag Zwei"...). Falls back to digits >= 100.
 * 1:1 port of Frank's helper.
 */
internal fun germanNumberWord(n: Int): String {
    if (n < 1 || n > 99) return n.toString()
    val ones = listOf("", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun")
    val teens =
        listOf(
            "zehn",
            "elf",
            "zwölf",
            "dreizehn",
            "vierzehn",
            "fünfzehn",
            "sechzehn",
            "siebzehn",
            "achtzehn",
            "neunzehn",
        )
    val tens =
        listOf(
            "",
            "",
            "zwanzig",
            "dreißig",
            "vierzig",
            "fünfzig",
            "sechzig",
            "siebzig",
            "achtzig",
            "neunzig",
        )
    val word =
        when {
            n < 10 -> ones[n]
            n < 20 -> teens[n - 10]
            n % 10 == 0 -> tens[n / 10]
            else -> {
                val o = n % 10
                val onesPart = if (o == 1) "ein" else ones[o]
                "${onesPart}und${tens[n / 10]}"
            }
        }
    return word.replaceFirstChar { it.uppercase() }
}

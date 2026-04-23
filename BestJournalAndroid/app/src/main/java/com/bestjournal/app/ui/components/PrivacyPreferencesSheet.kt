package com.bestjournal.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestjournal.app.R
import com.bestjournal.app.ui.theme.LocalIsDarkTheme

/**
 * Reusable privacy preferences bottom sheet (layered consent per EDSA 03/2023).
 *
 * Mounted in two places:
 *  - ConsentScreen → "Manuelle Auswahl" button
 *  - SettingsScreen → "Datenschutz-Einstellungen anpassen" button
 *
 * Both entry points pass the current toggle values and a callback to persist
 * changes. Single source of truth for the UI; persistence stays in the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPreferencesSheet(
    visible: Boolean,
    initial: PrivacyPreferences,
    onDismiss: () -> Unit,
    onSave: (PrivacyPreferences) -> Unit,
    showDoNotSell: Boolean = false,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local working copy — user can flip toggles freely, we only persist on Save.
    var analytics by rememberSaveable(initial) { mutableStateOf(initial.analytics) }
    var groq by rememberSaveable(initial) { mutableStateOf(initial.groq) }
    var gemini by rememberSaveable(initial) { mutableStateOf(initial.gemini) }
    var tts by rememberSaveable(initial) { mutableStateOf(initial.tts) }
    var driveBackup by rememberSaveable(initial) { mutableStateOf(initial.driveBackup) }
    var doNotSell by rememberSaveable(initial) { mutableStateOf(initial.doNotSell) }

    // Do-Not-Sell cascades: when on, force all optional flows off.
    val lockedByDnS = doNotSell

    val isDark = LocalIsDarkTheme.current
    val sheetContainer = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface
    val titleColor = if (isDark) Color(0xFFE5E2E1) else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isDark) Color(0xFFB8B2AE) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isDark) Color(0xFFECC165) else Color(0xFFB85D15)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.privacy_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = titleColor,
                )
                val anyOn = analytics || groq || gemini || tts || driveBackup
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (anyOn) {
                            analytics = false
                            groq = false
                            gemini = false
                            tts = false
                            driveBackup = false
                        } else {
                            analytics = true
                            groq = true
                            gemini = true
                            tts = true
                            driveBackup = true
                            doNotSell = false
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (anyOn) R.string.privacy_sheet_all_off
                            else R.string.privacy_sheet_all_on
                        ),
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.privacy_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = subtitleColor,
            )

            Spacer(Modifier.height(16.dp))

            PrefToggleCard(
                icon = Icons.Rounded.InsertChart,
                title = stringResource(R.string.consent_toggle_analytics_title),
                body = stringResource(R.string.consent_toggle_analytics_body),
                checked = analytics,
                enabled = !lockedByDnS,
                onCheckedChange = { analytics = it },
            )
            Spacer(Modifier.height(8.dp))
            PrefToggleCard(
                icon = Icons.Rounded.Mic,
                title = stringResource(R.string.consent_toggle_groq_title),
                body = stringResource(R.string.consent_toggle_groq_body),
                checked = groq,
                enabled = !lockedByDnS,
                onCheckedChange = { groq = it },
            )
            Spacer(Modifier.height(8.dp))
            PrefToggleCard(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.consent_toggle_gemini_title),
                body = stringResource(R.string.consent_toggle_gemini_body),
                checked = gemini,
                enabled = !lockedByDnS,
                onCheckedChange = { gemini = it },
            )
            Spacer(Modifier.height(8.dp))
            PrefToggleCard(
                icon = Icons.Rounded.VolumeUp,
                title = stringResource(R.string.consent_toggle_tts_title),
                body = stringResource(R.string.consent_toggle_tts_body),
                checked = tts,
                enabled = !lockedByDnS,
                onCheckedChange = { tts = it },
            )
            Spacer(Modifier.height(8.dp))
            PrefToggleCard(
                icon = Icons.Rounded.Backup,
                title = stringResource(R.string.consent_toggle_drive_title),
                body = stringResource(R.string.consent_toggle_drive_body),
                checked = driveBackup,
                enabled = !lockedByDnS,
                onCheckedChange = { driveBackup = it },
            )

            if (showDoNotSell) {
                Spacer(Modifier.height(12.dp))
                PrefToggleCard(
                    icon = Icons.Rounded.NoAccounts,
                    title = stringResource(R.string.consent_toggle_do_not_sell_title),
                    body = stringResource(R.string.consent_toggle_do_not_sell_body),
                    checked = doNotSell,
                    enabled = true,
                    emphasized = true,
                    onCheckedChange = { newVal ->
                        doNotSell = newVal
                        if (newVal) {
                            // Cascade off
                            analytics = false
                            groq = false
                            gemini = false
                            tts = false
                            driveBackup = false
                        }
                    },
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    onSave(
                        PrivacyPreferences(
                            analytics = analytics,
                            groq = groq,
                            gemini = gemini,
                            tts = tts,
                            driveBackup = driveBackup,
                            doNotSell = doNotSell,
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDF741E)),
            ) {
                Text(
                    text = stringResource(R.string.privacy_sheet_save),
                    color = Color(0xFF512400),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Snapshot of all user-controllable privacy toggles. */
data class PrivacyPreferences(
    val analytics: Boolean,
    val groq: Boolean,
    val gemini: Boolean,
    val tts: Boolean,
    val driveBackup: Boolean,
    val doNotSell: Boolean,
)

@Composable
private fun PrefToggleCard(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    emphasized: Boolean = false,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    val cardBg = if (isDark) {
        if (emphasized) Color(0x22FFB689) else Color(0x14FFB689)
    } else {
        if (emphasized) Color(0x30DF741E) else Color(0x18DF741E)
    }
    val cardBorder = if (isDark) {
        if (emphasized) Color(0x80FFB689) else Color(0x33FFB689)
    } else {
        if (emphasized) Color(0x80DF741E) else Color(0x40DF741E)
    }
    val iconBg = if (isDark) Color(0x40DF741E) else Color(0x30DF741E)
    val iconTint = if (isDark) Color(0xFFFFB689) else Color(0xFFB85D15)
    val titleColor = if (isDark) Color(0xFFE5E2E1) else MaterialTheme.colorScheme.onSurface
    val bodyColor = if (isDark) Color(0xFFB8B2AE) else MaterialTheme.colorScheme.onSurfaceVariant
    val expandColor = if (isDark) Color(0xFFECC165) else Color(0xFFB85D15)
    val uncheckedThumb = if (isDark) Color(0xFFB8B2AE) else MaterialTheme.colorScheme.onSurfaceVariant
    val uncheckedTrack = if (isDark) Color(0x22FFFFFF) else Color(0x14000000)
    val uncheckedBorder = uncheckedThumb.copy(alpha = 0.4f)
    val contentAlpha = if (enabled) 1f else 0.45f

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint.copy(alpha = contentAlpha),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = titleColor.copy(alpha = contentAlpha),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { expanded = !expanded },
                    ) {
                        Text(
                            text = stringResource(R.string.consent_details_expand),
                            color = expandColor.copy(alpha = contentAlpha),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = expandColor.copy(alpha = contentAlpha),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = if (enabled) onCheckedChange else null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF512400),
                        checkedTrackColor = Color(0xFFFFB689),
                        checkedBorderColor = Color(0xFFDF741E),
                        uncheckedThumbColor = uncheckedThumb,
                        uncheckedTrackColor = uncheckedTrack,
                        uncheckedBorderColor = uncheckedBorder,
                    ),
                )
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn(tween(180)), exit = fadeOut(tween(120))) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = body,
                        color = bodyColor,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

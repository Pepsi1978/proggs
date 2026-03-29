package com.entropyjournal.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.util.Constants
import com.entropyjournal.util.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onSignOut: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ -> viewModel.syncNow() }
    uiState.consentIntent?.let { intent ->
        androidx.compose.runtime.LaunchedEffect(intent) { consentLauncher.launch(intent); viewModel.clearConsentIntent() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        // 1. Konto
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Konto", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.userProfile != null) {
                    val profile = uiState.userProfile!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            GoogleLogo(modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(profile.displayName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(onClick = { viewModel.showLogoutDialog(true) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)) { Text("Abmelden") }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.lastSyncTimestamp?.let { ts ->
                        Text("Letzte Synchronisation: ${DateTimeFormatter.formatFull(ts)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                    Text("Eintr\u00e4ge werden bei der Anmeldung automatisch geladen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = { viewModel.syncNow() },
                            enabled = !uiState.isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) { Text(if (uiState.isSyncing) "Wird gesichert..." else "Tagebucheintr\u00e4ge sichern") }
                    }
                    uiState.syncMessage?.let { msg -> Spacer(modifier = Modifier.height(4.dp)); Text(msg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                } else {
                    // Not logged in
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GoogleLogo(modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nicht angemeldet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("Gesicherte Eintr\u00e4ge werden beim Anmelden geladen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onSignOut() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("Mit Google anmelden") }
                }
            }
        }

        // 2. Erscheinungsbild
        GlassCard {
            Column {
                Text("Erscheinungsbild", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                // Dunkelmodus — Sun | Moon icon
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        SettingsSunMoonIcon(isDark = uiState.isDarkTheme)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Dunkelmodus", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (uiState.isDarkTheme) "Aktiv" else "Aus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = uiState.isDarkTheme, onCheckedChange = { if (uiState.followSystem) viewModel.updateFollowSystem(false); viewModel.updateDarkTheme(it) }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
                Spacer(modifier = Modifier.height(8.dp))

                // System folgen — Light phone (sun) | divider | Dark phone (moon)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        SettingsPhoneIcon(isDark = uiState.isDarkTheme)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("System folgen", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("Automatisch", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = uiState.followSystem, onCheckedChange = { viewModel.updateFollowSystem(it) }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
                Spacer(modifier = Modifier.height(8.dp))
                val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        try {
                            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                            @Suppress("MissingPermission")
                            val loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                ?: lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                            if (loc != null) {
                                viewModel.saveLocation(loc.latitude, loc.longitude)
                                viewModel.updateFollowSun(true)
                            }
                        } catch (_: Exception) {}
                    }
                }
                // Sonnenauf-/untergang — Sun | Moon based on actual time
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        SettingsSunMoonIcon(isDark = uiState.isDarkTheme)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sonnenauf-/untergang", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("Dunkel bei Nacht, hell bei Tag", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = uiState.followSun, onCheckedChange = { enabled ->
                        if (enabled) {
                            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                try {
                                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                                    @Suppress("MissingPermission")
                                    val loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                        ?: lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                    if (loc != null) { viewModel.saveLocation(loc.latitude, loc.longitude) }
                                } catch (_: Exception) {}
                                viewModel.updateFollowSun(true)
                            } else {
                                locationLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        } else {
                            viewModel.updateFollowSun(false)
                        }
                    }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
            }
        }

        // 3. Sicherheit
        GlassCard {
            Column {
                Text("Sicherheit", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Rounded.Fingerprint, contentDescription = null, tint = if (uiState.biometricLock) MaterialTheme.colorScheme.primary else Color(0xFF666666), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Fingerabdruck", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("App beim Start entsperren", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = uiState.biometricLock, onCheckedChange = { enabled ->
                        // Require biometric auth before toggling the lock on or off
                        val activity = context as? com.entropyjournal.MainActivity
                        if (activity != null) {
                            activity.showBiometricPrompt { viewModel.updateBiometricLock(enabled) }
                        } else {
                            viewModel.updateBiometricLock(enabled)
                        }
                    }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
                if (uiState.biometricLock) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sperrt automatisch nach 60 Sekunden im Hintergrund", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // 4. API-Schluessel
        GlassCard {
            Column {
                Text("API-Schl\u00fcssel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                ApiKeyField(label = "Groq API-Key", value = uiState.groqApiKey, onValueChange = { viewModel.updateGroqApiKey(it) }, requireBiometric = uiState.biometricLock)
                Spacer(modifier = Modifier.height(8.dp))
                ApiKeyField(label = "Gemini API-Key", value = uiState.geminiApiKey, onValueChange = { viewModel.updateGeminiApiKey(it) }, requireBiometric = uiState.biometricLock)
            }
        }

        // 4. Gemini-Modell
        GlassCard {
            Column {
                Text("Gemini-Modell", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("F\u00fcr Textverbesserung und Dashboard-Analyse", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                var expanded by remember { mutableStateOf(false) }
                val selectedModel = Constants.GEMINI_FLASH_MODELS.find { it.id == uiState.selectedModel } ?: Constants.GEMINI_FLASH_MODELS.first()
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    TextField(
                        value = "${selectedModel.displayName}   ${selectedModel.price}", onValueChange = {}, readOnly = true,
                        trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, "Modell w\u00e4hlen") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = Color.Transparent),
                        singleLine = true, shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = MaterialTheme.colorScheme.surface) {
                        Constants.GEMINI_FLASH_MODELS.forEach { model ->
                            DropdownMenuItem(text = {
                                Column {
                                    Text(model.displayName, style = MaterialTheme.typography.bodyLarge, color = if (model.id == uiState.selectedModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Text("Input ${model.price.substringBefore("/")} \u2022 Output ${model.price.substringAfter("/")}  pro 1M Token", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }, onClick = { viewModel.updateSelectedModel(model.id); expanded = false })
                        }
                    }
                }
            }
        }

        // 5. Aufnahme
        GlassCard {
            Column {
                Text("Aufnahme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Max. Aufnahmedauer: ${uiState.maxRecordingDuration} Min.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(value = uiState.maxRecordingDuration.toFloat(), onValueChange = { viewModel.updateMaxRecordingDuration(it.toInt()) }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { Text("Textverbesserung", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface); Text("Standardm\u00e4\u00dfig aktivieren", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = uiState.textImprovementDefault, onCheckedChange = { viewModel.updateTextImprovementDefault(it) }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) { Text("Dashboard", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface); Text("Automatisch aktualisieren", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = uiState.autoUpdateDashboard, onCheckedChange = { viewModel.updateAutoUpdateDashboard(it) }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
            }
        }

        // 6. Ueber die App
        GlassCard {
            Column {
                Text("\u00dcber die App", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Entropy Journal v0.6.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Dein pers\u00f6nliches KI-Tagebuch", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(4.dp))
                Text("\u00a9 Frank Barwandt", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Abmelden?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("M\u00f6chtest du dich wirklich abmelden?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = { Button(onClick = { viewModel.signOut(context); onSignOut() }, colors = ButtonDefaults.buttonColors(containerColor = NeonRed)) { Text("Abmelden") } },
            dismissButton = { OutlinedButton(onClick = { viewModel.showLogoutDialog(false) }) { Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }
}

@Composable
private fun ApiKeyField(label: String, value: String, onValueChange: (String) -> Unit, requireBiometric: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    TextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.outline) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = {
                if (visible) {
                    // Always allow hiding
                    visible = false
                } else if (requireBiometric) {
                    // Require biometric to reveal
                    val activity = context as? com.entropyjournal.MainActivity
                    if (activity != null) {
                        activity.showBiometricPrompt { visible = true }
                    } else {
                        visible = true
                    }
                } else {
                    visible = true
                }
            }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (visible) "Verbergen" else "Anzeigen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedTextColor = MaterialTheme.colorScheme.onSurface, cursorColor = MaterialTheme.colorScheme.primary, focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = Color.Transparent),
        singleLine = true, shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun GoogleLogo(modifier: Modifier = Modifier) {
    // Google brand colors
    val googleBlue = Color(0xFF4285F4)
    val googleRed = Color(0xFFEA4335)
    val googleYellow = Color(0xFFFBBC05)
    val googleGreen = Color(0xFF34A853)

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeW = size.minDimension * 0.18f
        val radius = (size.minDimension - strokeW) / 2f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

        // Blue arc (right, top-right) — 315° to 85° (sweep 130°)
        drawArc(color = googleBlue, startAngle = -45f, sweepAngle = 130f, useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2))
        // Green arc (bottom-right) — 85° to 175° (sweep 90°)
        drawArc(color = googleGreen, startAngle = 85f, sweepAngle = 90f, useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2))
        // Yellow arc (bottom-left) — 175° to 225° (sweep 50°)
        drawArc(color = googleYellow, startAngle = 175f, sweepAngle = 50f, useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2))
        // Red arc (top-left, top) — 225° to 315° (sweep 90°)
        drawArc(color = googleRed, startAngle = 225f, sweepAngle = 90f, useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2))

        // Blue horizontal bar (the "crossbar" of the G)
        val barY = center.y
        val barLeft = center.x - strokeW * 0.1f
        val barRight = center.x + radius
        drawLine(color = googleBlue, start = androidx.compose.ui.geometry.Offset(barLeft, barY),
            end = androidx.compose.ui.geometry.Offset(barRight, barY), strokeWidth = strokeW)
    }
}

@Composable
private fun SettingsPhoneIcon(isDark: Boolean) {
    val glowYellow = Color(0xFFFFD54F)
    val mutedGray = Color(0xFF666666)
    val lightPhoneSize by animateDpAsState(
        targetValue = if (!isDark) 22.dp else 14.dp,
        animationSpec = tween(300), label = "lightPhoneSize"
    )
    val darkPhoneSize by animateDpAsState(
        targetValue = if (isDark) 22.dp else 14.dp,
        animationSpec = tween(300), label = "darkPhoneSize"
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Light phone with mini sun
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Rounded.PhoneAndroid, "Hell", tint = if (!isDark) glowYellow else mutedGray, modifier = Modifier.size(lightPhoneSize))
            Icon(Icons.Rounded.LightMode, null, tint = if (!isDark) glowYellow else mutedGray, modifier = Modifier.size(lightPhoneSize * 0.35f))
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.height(16.dp).width(1.dp))
        // Dark phone with mini moon
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Rounded.PhoneAndroid, "Dunkel", tint = if (isDark) glowYellow else mutedGray, modifier = Modifier.size(darkPhoneSize))
            Icon(Icons.Rounded.DarkMode, null, tint = if (isDark) glowYellow else mutedGray, modifier = Modifier.size(darkPhoneSize * 0.35f))
        }
    }
}

@Composable
private fun SettingsSunMoonIcon(isDark: Boolean) {
    val glowYellow = Color(0xFFFFD54F)
    val mutedGray = Color(0xFF666666)
    val sunSize by animateDpAsState(
        targetValue = if (!isDark) 22.dp else 14.dp,
        animationSpec = tween(300), label = "settingSunSize"
    )
    val moonSize by animateDpAsState(
        targetValue = if (isDark) 22.dp else 14.dp,
        animationSpec = tween(300), label = "settingMoonSize"
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Rounded.LightMode, "Sonne", tint = if (!isDark) glowYellow else mutedGray, modifier = Modifier.size(sunSize))
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.height(16.dp).width(1.dp))
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Rounded.DarkMode, "Mond", tint = if (isDark) glowYellow else mutedGray, modifier = Modifier.size(moonSize))
        }
    }
}

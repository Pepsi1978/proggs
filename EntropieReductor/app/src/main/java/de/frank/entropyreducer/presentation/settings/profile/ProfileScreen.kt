package de.frank.entropyreducer.presentation.settings.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.ProfileViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun ProfileScreen(onBack: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val saved by vm.profileText.collectAsStateWithLifecycle()
    val distillState by vm.distillState.collectAsStateWithLifecycle()
    val distillError by vm.distillError.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    var text by remember(saved) { mutableStateOf(saved) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(distillError) {
        distillError?.let {
            snackbar.showSnackbar(it)
            vm.dismissDistillError()
        }
    }
    androidx.compose.runtime.LaunchedEffect(distillState) {
        if (distillState == de.frank.entropyreducer.presentation.settings.DistillState.DONE) {
            snackbar.showSnackbar("Profil ins Gedächtnis übernommen.")
        }
    }

    CosmosScaffold(
        title = "Persönliches Profil",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier =
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "Über mich — was die KI über dich wissen soll",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Fuege hier alles ein, was die KI über dich verstehen soll. Du kannst zum Beispiel deinen vollständigen Memory-Export aus ChatGPT oder Claude einfuegen. Je mehr Kontext, desto besser kann die KI deine Entropie verstehen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.fillMaxWidth().height(360.dp),
                            placeholder = {
                                Text("Erzähl mir über dich …", color = cosmos.textSecondary)
                            },
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = cosmos.textPrimary,
                                    unfocusedTextColor = cosmos.textPrimary,
                                    cursorColor = LocalCosmos.current.accent,
                                    focusedIndicatorColor = LocalCosmos.current.accent,
                                    unfocusedIndicatorColor = cosmos.glassBorder,
                                ),
                        )
                        Spacer(Modifier.height(12.dp))
                        val isRunning =
                            distillState ==
                                de.frank.entropyreducer.presentation.settings.DistillState.RUNNING
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { vm.distillToMemory(text) },
                                enabled = !isRunning,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (isRunning) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = LocalCosmos.current.accent,
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text("KI analysiert …")
                                } else {
                                    Text("Aus Profil ins Gedächtnis übernehmen")
                                }
                            }
                            Button(
                                onClick = { vm.save(text) },
                                modifier = Modifier.weight(0.6f),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = LocalCosmos.current.accent,
                                        contentColor = Color.Black,
                                    ),
                            ) {
                                Text("Speichern")
                            }
                        }
                        if (isRunning) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text =
                                    "Gemini liest dein Profil und extrahiert alle Entropie-relevanten Punkte — kann ein paar Sekunden dauern.",
                                style = MaterialTheme.typography.labelSmall,
                                color = cosmos.textSecondary,
                            )
                        }
                    }
                }
            }
            androidx.compose.material3.SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(16.dp),
            ) {
                androidx.compose.material3.Snackbar(it)
            }
        }
    }
}

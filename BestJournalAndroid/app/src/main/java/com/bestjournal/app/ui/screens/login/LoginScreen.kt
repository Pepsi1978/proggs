package com.bestjournal.app.ui.screens.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestjournal.app.ui.theme.NeonRed

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onConsentResult()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                if (state.needsRestart) {
                    // Restart app so Room picks up the restored database
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                } else {
                    onLoginSuccess()
                }
            }
            is LoginUiState.NeedConsent -> {
                consentLauncher.launch(state.intent)
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Best",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Journal",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dein pers\u00f6nliches KI-Tagebuch\nf\u00fcr Klarheit und Ver\u00e4nderung",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))

            when (uiState) {
                is LoginUiState.Loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                is LoginUiState.Error -> {
                    val errorMsg = (uiState as LoginUiState.Error).message
                    val displayMsg = if (errorMsg.contains("No credentials available", ignoreCase = true)
                        || errorMsg.contains("YOUR_WEB_CLIENT_ID", ignoreCase = true)
                    ) {
                        "Google-Anmeldung ist noch nicht eingerichtet.\nBitte zuerst ohne Anmeldung fortfahren."
                    } else {
                        errorMsg
                    }
                    Text(
                        text = displayMsg,
                        color = NeonRed,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GoogleSignInButton(onClick = { viewModel.signIn(context) })
                }
                else -> {
                    GoogleSignInButton(onClick = { viewModel.signIn(context) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.TextButton(
                onClick = onLoginSuccess
            ) {
                Text(
                    text = "Ohne Anmeldung fortfahren",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.DarkGray
        )
    ) {
        // Google "G" logo in 4 colors
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(24.dp)
        ) {
            val r = size.width / 2f
            val stroke = r * 0.38f
            val arcStyle = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
            // Blue (top-right arc)
            drawArc(Color(0xFF4285F4), -45f, -90f, false, style = arcStyle,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
            // Green (bottom-right arc)
            drawArc(Color(0xFF34A853), -135f, -90f, false, style = arcStyle,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
            // Yellow (bottom-left arc)
            drawArc(Color(0xFFFBBC05), -225f, -90f, false, style = arcStyle,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
            // Red (top-left arc)
            drawArc(Color(0xFFEA4335), -315f, -90f, false, style = arcStyle,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
            // Horizontal bar of the G
            drawLine(Color(0xFF4285F4),
                start = androidx.compose.ui.geometry.Offset(r, r),
                end = androidx.compose.ui.geometry.Offset(size.width - stroke * 0.3f, r),
                strokeWidth = stroke)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Mit Google anmelden",
            style = MaterialTheme.typography.titleMedium,
            color = Color.DarkGray,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

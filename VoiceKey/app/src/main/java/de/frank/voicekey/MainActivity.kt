package de.frank.voicekey

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val TAG = "VOICEKEY"
private const val CHATGPT_PKG = "com.openai.chatgpt"
private const val PROXY_ACTIVITY = "com.openai.feature.assistant.impl.AssistantProxyActivity"
private const val ASSISTANT_ACTIVITY = "com.openai.voice.assistant.AssistantActivity"

/**
 * Phase-1-De-Risking-Test: Welcher Intent-Weg oeffnet aus einer normalen App (ohne Sonderrechte)
 * den ChatGPT-Voice-Mode? Am Geraet ist belegt: KEYCODE_ASSIST (System-Trigger) oeffnet
 * com.openai.voice.assistant.AssistantActivity. Eine App kann KeyEvents nicht injizieren —
 * also testen wir hier die App-tauglichen Intent-Wege.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold { innerPadding ->
                    TestScreen(
                        modifier = Modifier.padding(innerPadding),
                        onFire = ::fireTrigger,
                    )
                }
            }
        }
    }

    /** Feuert einen der Test-Intents und gibt das Ergebnis als lesbaren Text zurueck. */
    private fun fireTrigger(method: String): String {
        val intent = when (method) {
            "assist_plain" ->
                Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "assist_pkg" ->
                // DIREKTER Component-Start der Voice-Mode-Activity (die KEYCODE_ASSIST oeffnet).
                Intent()
                    .setClassName(CHATGPT_PKG, ASSISTANT_ACTIVITY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "assist_component" ->
                Intent(Intent.ACTION_ASSIST)
                    .setClassName(CHATGPT_PKG, PROXY_ACTIVITY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            "voice_command" ->
                Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> return "Unbekannte Methode: $method"
        }
        return try {
            startActivity(intent)
            Log.d(TAG, "Trigger gestartet: $method -> $intent")
            "OK: '$method' gestartet. Schau aufs Display — kam der ChatGPT-Voice-Mode?"
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Keine Activity fuer $method", e)
            "FEHLER (keine passende App/Activity): ${e.message}"
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException fuer $method", e)
            "FEHLER (keine Berechtigung): ${e.message}"
        } catch (e: Exception) {
            Log.e(TAG, "Unerwarteter Fehler fuer $method", e)
            "FEHLER: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}

@Composable
private fun TestScreen(
    modifier: Modifier = Modifier,
    onFire: (String) -> String,
) {
    var status by remember { mutableStateOf("Bereit. Tippe eine Methode an und schau aufs Display.") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("VoiceKey", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Trigger-Test (Phase 1): Welcher Weg oeffnet den ChatGPT-Voice-Mode?",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(onClick = { status = onFire("assist_plain") }, modifier = Modifier.fillMaxWidth()) {
            Text("1) ACTION_ASSIST (ohne Paket)")
        }
        Button(onClick = { status = onFire("assist_pkg") }, modifier = Modifier.fillMaxWidth()) {
            Text("2) ACTION_ASSIST -> ChatGPT-Paket")
        }
        Button(onClick = { status = onFire("assist_component") }, modifier = Modifier.fillMaxWidth()) {
            Text("3) ACTION_ASSIST -> ProxyActivity")
        }
        Button(onClick = { status = onFire("voice_command") }, modifier = Modifier.fillMaxWidth()) {
            Text("4) ACTION_VOICE_COMMAND")
        }

        HorizontalDivider()
        Text("Status:", style = MaterialTheme.typography.labelLarge)
        Text(status, style = MaterialTheme.typography.bodyMedium)
    }
}

package de.frank.stacklabor.werftstudio

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import de.frank.stacklabor.werftstudio.service.tts.TtsServiceIntegration
import de.frank.stacklabor.werftstudio.service.work.CodexWorkIntegration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StackLaborApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppContainer(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CodexWorkIntegration.runner = container.evaluator
        TtsServiceIntegration.engine = container.ttsEngine
        TtsServiceIntegration.contentIntentFactory = { context ->
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        applicationScope.launch { container.repository.initialisiereStartbestandWennLeer() }
    }
}

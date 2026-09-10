package de.frank.stacklabor.werftstudio.service.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.frank.stacklabor.werftstudio.service.codex.CodexErrorKind
import de.frank.stacklabor.werftstudio.service.codex.CodexException
import kotlinx.coroutines.CancellationException

interface DeferredCodexEvaluationRunner {
    suspend fun isAuthenticationValid(): Boolean
    suspend fun runOnce(stackId: String)
}

object CodexWorkIntegration {
    @Volatile
    var runner: DeferredCodexEvaluationRunner? = null

    fun requireRunner(): DeferredCodexEvaluationRunner = checkNotNull(runner) {
        "CodexWorkIntegration.runner muss in Application.onCreate gesetzt werden."
    }
}

class CodexRetryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val stackId = inputData.getString(KEY_STACK_ID)?.takeIf(String::isNotBlank)
            ?: return Result.failure()
        val runner = CodexWorkIntegration.requireRunner()
        if (!runner.isAuthenticationValid()) return Result.failure(errorData(CodexErrorKind.REAUTH))
        return try {
            runner.runOnce(stackId)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: CodexException) {
            Result.failure(errorData(error.kind))
        } catch (_: Exception) {
            Result.failure(errorData(CodexErrorKind.NETWORK))
        }
    }

    companion object {
        private const val UNIQUE_PREFIX = "codex-evaluation-retry:"
        const val KEY_STACK_ID = "stack_id"
        const val KEY_ERROR_KIND = "error_kind"

        fun enqueueUnique(context: Context, stackId: String) {
            require(stackId.isNotBlank())
            val request = OneTimeWorkRequestBuilder<CodexRetryWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .setInputData(Data.Builder().putString(KEY_STACK_ID, stackId).build())
                .addTag(UNIQUE_PREFIX + stackId)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_PREFIX + stackId,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context, stackId: String) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_PREFIX + stackId)
        }

        private fun errorData(kind: CodexErrorKind): Data =
            Data.Builder().putString(KEY_ERROR_KIND, kind.name).build()
    }
}

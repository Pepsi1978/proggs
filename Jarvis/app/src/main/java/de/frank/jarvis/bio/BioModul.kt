package de.frank.jarvis.bio

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import de.frank.jarvis.bio.data.diagnostics.Diag
import de.frank.jarvis.bio.data.diagnostics.DiagnosticLogger
import de.frank.jarvis.bio.data.health.HealthConnectManager
import de.frank.jarvis.bio.data.local.AppDatabase
import de.frank.jarvis.bio.data.remote.OpenMeteoApi
import de.frank.jarvis.bio.data.remote.drive.SyncCoordinator
import de.frank.jarvis.bio.data.remote.oauth.OAuthService
import de.frank.jarvis.bio.data.remote.oura.OuraApi
import de.frank.jarvis.bio.data.remote.polar.PolarBulkImporter
import de.frank.jarvis.bio.data.remote.whoop.WhoopApi
import de.frank.jarvis.bio.data.repository.AmazfitRepository
import de.frank.jarvis.bio.data.repository.BiomarkerCardOrderRepository
import de.frank.jarvis.bio.data.repository.OuraRepository
import de.frank.jarvis.bio.data.repository.WeatherRepository
import de.frank.jarvis.bio.data.repository.WhoopRepository
import de.frank.jarvis.bio.data.repository.ZeppBodyRepository
import de.frank.jarvis.bio.data.settings.AppSettings
import de.frank.jarvis.bio.data.settings.EncryptedSecretsStore
import de.frank.jarvis.bio.domain.usecase.ForegroundSyncManager
import de.frank.jarvis.bio.workers.SyncProblemNotifier
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Der Biomarker-Bereich von Jarvis (aus Entropie Reductor übernommen): baut die übernommenen Klassen einmal je Prozess
 * zusammen, wie dort Hilt. Alles entsteht erst beim ersten Zugriff, damit der Start von Jarvis nicht langsamer wird.
 */
object BioModul {
    const val EXTRA_OEFFNEN = "de.frank.jarvis.BIOMARKER_OEFFNEN"

    @Volatile private var kontext: Context? = null
    private val app: Context get() = kontext ?: error("BioModul nicht eingerichtet")

    /** Für Arbeit, die einen Aufrufer überdauern soll (ein Abgleich läuft zu Ende, auch wenn die Wartezeit abläuft). */
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun einrichten(context: Context) {
        if (kontext != null) return
        kontext = context.applicationContext
        Diag.init(diagnostics)
    }

    val diagnostics by lazy { DiagnosticLogger() }
    val datenbank by lazy { AppDatabase.baue(app) }
    val einstellungen by lazy { AppSettings(app) }
    val geheim by lazy { EncryptedSecretsStore(app) }
    val melder by lazy { SyncProblemNotifier(app) }
    /** Jede Änderung an den Daten stößt den Spiegel fürs Tablet an. */
    val abgleich by lazy { SyncCoordinator { grund -> BioSpiegel.geaendert(app, grund) } }

    private val json by lazy { Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true } }
    private val netz by lazy {
        OkHttpClient.Builder()
            .cache(Cache(File(app.cacheDir, "bio_http_cache"), 10L * 1024 * 1024))
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }
    private fun retrofit(basis: String) = Retrofit.Builder().baseUrl(basis).client(netz)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()

    val whoopApi: WhoopApi by lazy { retrofit("https://api.prod.whoop.com/developer/").create(WhoopApi::class.java) }
    val ouraApi: OuraApi by lazy { retrofit("https://api.ouraring.com/").create(OuraApi::class.java) }
    private val openMeteo: OpenMeteoApi by lazy { retrofit("https://archive-api.open-meteo.com/").create(OpenMeteoApi::class.java) }

    val oauth by lazy { OAuthService(app, geheim, melder) }
    val healthConnect by lazy { HealthConnectManager(app, diagnostics) }
    val wetter by lazy { WeatherRepository(openMeteo) }
    val whoop by lazy { WhoopRepository(datenbank.biomarkerSnapshotDao(), datenbank.whoopWorkoutDao(), whoopApi, oauth, einstellungen, diagnostics) }
    val oura by lazy {
        OuraRepository(
            datenbank.ouraReadinessDao(), datenbank.ouraDailySleepDao(), datenbank.ouraActivityDao(), datenbank.ouraResilienceDao(),
            datenbank.ouraSleepDetailDao(), datenbank.ouraPersonalInfoDao(), ouraApi, geheim, einstellungen, datenbank, diagnostics,
        )
    }
    val trainings by lazy {
        AmazfitRepository(datenbank.amazfitDailyDao(), datenbank.amazfitWorkoutDao(), geheim, einstellungen, datenbank, lazy { abgleich }, healthConnect, wetter)
    }
    val waage by lazy { ZeppBodyRepository(app, datenbank.healthConnectValueDao(), einstellungen, diagnostics, lazy { abgleich }) }
    val kartenReihenfolge by lazy { BiomarkerCardOrderRepository(app) }
    val polar by lazy { PolarBulkImporter(app) }
    val sync by lazy {
        ForegroundSyncManager(geheim, oauth, whoop, oura, trainings, waage, einstellungen, bereich, { datenbank }) { quelle ->
            de.frank.jarvis.data.Einstellungen.get(app).bioQuelleAn(quelle)
        }
    }
}

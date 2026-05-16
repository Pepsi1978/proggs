package de.frank.entropyreducer.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.BuildConfig
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GoogleTtsApi
import de.frank.entropyreducer.data.remote.GroqWhisperApi
import de.frank.entropyreducer.data.remote.calendar.GoogleCalendarApi
import de.frank.entropyreducer.data.remote.oura.OuraApi
import de.frank.entropyreducer.data.remote.polar.PolarApi
import de.frank.entropyreducer.data.remote.strava.StravaApi
import de.frank.entropyreducer.data.remote.whoop.WhoopApi
import de.frank.entropyreducer.data.remote.zepp.ZeppApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient {
        // Performance-Audit Loop 2 (2026-05-10): HTTP-Cache 10 MB — vorher hatten
        // alle Clients keinen Cache, jeder Sync-Zyklus aller 4 Biomarker-Quellen
        // schickte vollstaendige HTTP-Requests an externe APIs auch wenn die Daten
        // sich nicht geaendert haben. Mit Cache profitieren konfigurierbare Endpunkte
        // (mit Cache-Control/ETag/Last-Modified) automatisch — Anbieter ohne
        // Caching-Header werden weiterhin live abgefragt (kein Verhaltens-Bruch).
        val httpCacheDir = File(context.cacheDir, "http_cache")
        val httpCache = Cache(httpCacheDir, 10L * 1024 * 1024) // 10 MB
        val builder = OkHttpClient.Builder()
            .cache(httpCache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS) // Sicherheitsnetz fuer haengende Calls
        if (BuildConfig.DEBUG) {
            // SICHERHEIT: API-Keys NIEMALS ins Logcat schreiben — auch nicht im Debug-Build.
            // redactHeader() ist die offizielle OkHttp-Mechanik dafuer.
            // Quelle: square.github.io/okhttp/.../HttpLoggingInterceptor#redactHeader (Stand 2025).
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Authorization")          // Groq Bearer-Token
                redactHeader("x-goog-api-key")         // Gemini API-Key
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named("googleTts")
    fun provideGoogleTtsRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://texttospeech.googleapis.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideGroqApi(@Named("groq") retrofit: Retrofit): GroqWhisperApi =
        retrofit.create(GroqWhisperApi::class.java)

    @Provides @Singleton
    fun provideGeminiApi(@Named("gemini") retrofit: Retrofit): GeminiApi =
        retrofit.create(GeminiApi::class.java)

    @Provides @Singleton
    fun provideGoogleTtsApi(@Named("googleTts") retrofit: Retrofit): GoogleTtsApi =
        retrofit.create(GoogleTtsApi::class.java)

    /* ----- Stufe 2: Google Calendar + Whoop ----- */

    @Provides
    @Singleton
    @Named("googleCalendar")
    fun provideGoogleCalendarRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @Named("whoop")
    fun provideWhoopRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.prod.whoop.com/developer/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideGoogleCalendarApi(@Named("googleCalendar") retrofit: Retrofit): GoogleCalendarApi =
        retrofit.create(GoogleCalendarApi::class.java)

    @Provides @Singleton
    fun provideWhoopApi(@Named("whoop") retrofit: Retrofit): WhoopApi =
        retrofit.create(WhoopApi::class.java)

    /* ----- Zepp / Amazfit T-Rex 3 ----- */

    /**
     * Eigener OkHttp-Client fuer Zepp: Redirects MUESSEN ausgeschaltet sein,
     * weil wir die Tokens aus dem Location-Header der 303-Antwort holen muessen.
     * Wenn OkHttp den Redirect automatisch verfolgt, bekommen wir die Tokens nie.
     */
    @Provides
    @Singleton
    @Named("zepp")
    fun provideZeppOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("apptoken")
                redactHeader("login_token")
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    /**
     * Retrofit fuer Zepp. baseUrl ist nur Platzhalter — wir nutzen @Url in jedem
     * Aufruf damit dieselbe Retrofit-Instanz mit api-user-de2 und api-mifit-de2
     * arbeiten kann.
     */
    @Provides
    @Singleton
    @Named("zepp")
    fun provideZeppRetrofit(@Named("zepp") client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api-mifit.zepp.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideZeppApi(@Named("zepp") retrofit: Retrofit): ZeppApi =
        retrofit.create(ZeppApi::class.java)

    /* ----- Oura Ring (Personal Access Token) ----- */

    @Provides
    @Singleton
    @Named("oura")
    fun provideOuraRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.ouraring.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideOuraApi(@Named("oura") retrofit: Retrofit): OuraApi =
        retrofit.create(OuraApi::class.java)

    /* ----- Polar AccessLink v3 (Frank-Wunsch 2026-05-16) ----- */
    @Provides
    @Singleton
    @Named("polar")
    fun providePolarRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.polaraccesslink.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun providePolarApi(@Named("polar") retrofit: Retrofit): PolarApi =
        retrofit.create(PolarApi::class.java)

    /* ----- Strava API V3 (Frank-Wunsch 2026-05-16, revived 2026-05-17) ----- */
    @Provides
    @Singleton
    @Named("strava")
    fun provideStravaRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.strava.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideStravaApi(@Named("strava") retrofit: Retrofit): StravaApi =
        retrofit.create(StravaApi::class.java)
}

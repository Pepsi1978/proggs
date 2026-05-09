package de.frank.entropyreducer.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.BuildConfig
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GoogleTtsApi
import de.frank.entropyreducer.data.remote.GroqWhisperApi
import de.frank.entropyreducer.data.remote.calendar.GoogleCalendarApi
import de.frank.entropyreducer.data.remote.whoop.WhoopApi
import de.frank.entropyreducer.data.remote.zepp.ZeppApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
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
    fun provideOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
}

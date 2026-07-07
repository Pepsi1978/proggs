package de.frank.entropyreducer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Annotation für den Application-weiten CoroutineScope.
 * Quelle: developer.android.com/kotlin/coroutines/coroutines-best-practices —
 * "Use an external CoroutineScope managed by the Application class for work
 * that should outlive the caller's lifecycle."
 *
 * Zweck: Hintergrundarbeit aus kurzlebigen Components (transparente Activities,
 * Broadcast-Receiver) braucht einen Scope der NICHT mit der Component stirbt.
 * SupervisorJob → ein fehlschlagender Child-Job killt nicht den ganzen Scope.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

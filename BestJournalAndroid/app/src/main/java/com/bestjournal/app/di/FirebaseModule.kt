package com.bestjournal.app.di

import android.content.SharedPreferences
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.data.remote.ai.AiRateLimiter
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAiService(): FirebaseAiService = FirebaseAiService()

    @Provides
    @Singleton
    fun provideAiUsageTracker(prefs: SharedPreferences): AiUsageTracker = AiUsageTracker(prefs)

    @Provides
    @Singleton
    fun provideAiRateLimiter(usageTracker: AiUsageTracker): AiRateLimiter = AiRateLimiter(usageTracker)

    @Provides
    @Singleton
    fun provideBillingManager(): BillingManager = BillingManager()
}

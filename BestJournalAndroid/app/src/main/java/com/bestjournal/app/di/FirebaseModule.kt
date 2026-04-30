package com.bestjournal.app.di

import android.content.Context
import android.content.SharedPreferences
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.data.remote.SubscriptionStatusService
import com.bestjournal.app.data.remote.ai.AiRateLimiter
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideFirebaseAiService(
        @ApplicationContext context: Context
    ): FirebaseAiService = FirebaseAiService(context)

    @Provides
    @Singleton
    fun provideAiUsageTracker(prefs: SharedPreferences): AiUsageTracker = AiUsageTracker(prefs)

    @Provides
    @Singleton
    fun provideAiRateLimiter(usageTracker: AiUsageTracker): AiRateLimiter = AiRateLimiter(usageTracker)

    /**
     * Region-specific FirebaseFunctions instance. The Cloud Function
     * `getSubscriptionStatus` is deployed to europe-west1 (Belgium) for DSGVO
     * compliance and low latency for German users.
     */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("europe-west1")

    @Provides
    @Singleton
    fun provideSubscriptionStatusService(
        functions: FirebaseFunctions,
    ): SubscriptionStatusService = SubscriptionStatusService(functions)

    @Provides
    @Singleton
    fun provideBillingManager(
        analyticsTracker: com.bestjournal.app.util.AnalyticsTracker,
        encryptedPrefs: SharedPreferences,
        subscriptionStatusService: SubscriptionStatusService,
    ): BillingManager = BillingManager(analyticsTracker, encryptedPrefs, subscriptionStatusService)
}

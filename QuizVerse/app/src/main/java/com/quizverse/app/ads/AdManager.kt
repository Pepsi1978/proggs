package com.quizverse.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.quizverse.app.util.Constants

/**
 * Singleton that manages all AdMob advertisement lifecycle within QuizVerse.
 *
 * Ad unit IDs:
 * - All IDs are Google's official test IDs. Replace with production IDs before release.
 *
 * Usage:
 *   AdManager.initialize(applicationContext)
 *   AdManager.showInterstitial(activity)
 *   AdManager.showRewarded(activity) { item -> grantItem(item) }
 */
object AdManager {

    private const val TAG = "AdManager"

    // --- Test ad unit IDs (replace before production release) ---------------
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_UNIT_ID     = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    /** Counts completed quiz rounds to determine when to show an interstitial. */
    private var roundCount: Int = 0

    /** Whether the SDK has been initialised. Prevents double-init. */
    private var isInitialized: Boolean = false

    // ---- Public API --------------------------------------------------------

    /**
     * Initialises the AdMob SDK. Must be called once from Application.onCreate()
     * or before any ad is requested.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { initStatus ->
            Log.d(TAG, "AdMob initialised: $initStatus")
            isInitialized = true
            // Pre-load ads immediately after SDK is ready
            loadInterstitial(context)
            loadRewarded(context)
        }
    }

    /**
     * Pre-loads a new interstitial ad into memory.
     * Call this after showing one to keep the next one ready.
     */
    fun loadBanner() {
        // Banner ads are loaded directly in the composable/view via AdView.
        // This method exists as a hook for future banner pre-loading logic.
        Log.d(TAG, "Banner loading is handled inline by AdView widgets.")
    }

    /**
     * Pre-loads an interstitial ad.
     * Call after [showInterstitial] to refill the slot.
     */
    fun loadInterstitial(context: Context) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows the pre-loaded interstitial if one is available.
     * Automatically increments the round counter and only shows every
     * [Constants.INTERSTITIAL_ROUND_INTERVAL] rounds.
     *
     * @param activity The foreground activity used as the display context.
     */
    /**
     * Shows an interstitial ad only if the player has accumulated at least
     * [Constants.ADS_MIN_PLAY_TIME_MS] (3.5 hours) of total play time.
     * Before that threshold, no ads are shown at all.
     */
    fun showInterstitial(activity: Activity, totalPlayTimeMs: Long = 0L) {
        // No ads before 3.5 hours of total play time
        if (totalPlayTimeMs < Constants.ADS_MIN_PLAY_TIME_MS) return

        roundCount++
        if (roundCount % Constants.INTERSTITIAL_ROUND_INTERVAL != 0) return

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial not ready yet.")
            loadInterstitial(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity) // Pre-load next one
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
            }
        }

        ad.show(activity)
    }

    /**
     * Pre-loads a rewarded ad into memory.
     * Call after [showRewarded] to refill the slot.
     */
    fun loadRewarded(context: Context) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows the pre-loaded rewarded ad if one is available.
     *
     * @param activity  The foreground activity.
     * @param onReward  Called with the reward amount when the user earns the reward.
     *                  Runs on the main thread.
     */
    fun showRewarded(activity: Activity, onReward: (amount: Int) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready yet.")
            loadRewarded(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity) // Pre-load next one
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onReward(rewardItem.amount)
        }
    }
}

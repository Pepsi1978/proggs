package com.bestjournal.app.util

object Constants {
    // Groq API (for premium transcription via Firebase Remote Config)
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    const val GROQ_TRANSCRIPTION_MODEL = "whisper-large-v3-turbo"
    const val REMOTE_CONFIG_GROQ_KEY = "groq_api_key"

    // Audio recording
    const val MAX_RECORDING_DURATION_MINUTES = 10
    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_CHANNELS = 1

    // Sync
    const val SYNC_DEBOUNCE_MS = 30_000L
    const val ANALYSIS_DEBOUNCE_MS = 60_000L
    const val DRIVE_BACKUP_FILENAME = "bestjournal_backup.db"
    const val DRIVE_PHOTOS_FILENAME = "bestjournal_photos.zip"

    // SharedPreferences keys
    const val PREF_BACKUP_PHOTOS = "backup_photos"
    const val PREF_BACKUP_VIDEOS = "backup_videos"
    const val PREF_TEXT_IMPROVEMENT_DEFAULT = "text_improvement_default"
    const val PREF_MAX_RECORDING_DURATION = "max_recording_duration"
    const val PREF_AUTO_UPDATE_DASHBOARD = "auto_update_dashboard"
    const val PREF_MANUAL_REFRESHES_LEFT = "manual_refreshes_left"
    const val MAX_REFRESHES_PER_ENTRY = 5
    const val PREF_GOOGLE_ACCOUNT_NAME = "google_account_name"
    const val PREF_GOOGLE_ACCOUNT_EMAIL = "google_account_email"
    const val PREF_GOOGLE_AVATAR_URL = "google_avatar_url"
    const val PREF_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val PREF_SYNC_IN_PROGRESS = "sync_in_progress"
    const val PREF_RESTORE_PENDING = "restore_pending"
    const val PREF_DARK_THEME = "dark_theme"
    const val PREF_THEME_FOLLOW_SYSTEM = "theme_follow_system"
    const val PREF_APP_THEME = "app_theme"
    const val PREF_BIOMETRIC_LOCK = "biometric_lock"
    const val PREF_DASHBOARD_UPDATING = "dashboard_updating"
    const val PREF_DASHBOARD_UPDATE_IS_DELETE = "dashboard_update_is_delete"
    const val PREF_DASHBOARD_LAST_UPDATED = "dashboard_last_updated"

    // Google OAuth
    const val GOOGLE_WEB_CLIENT_ID =
        "314424175660-12m82j2jgvmc0tcgl61igghp67nnpmn1.apps.googleusercontent.com"

    // Encrypted SharedPreferences file name
    const val ENCRYPTED_PREFS_NAME = "entropy_journal_secure_prefs"

    // Töne
    const val PREF_SOUNDS_ENABLED = "sounds_enabled"

    // Haptik
    const val PREF_HAPTIC_ENABLED = "haptic_enabled"

    // TTS (Text-to-Speech) — voices are in TtsVoiceRegistry.kt
    const val PREF_TTS_ENABLED = "tts_enabled"
    const val PREF_EDGE_TTS_VOICE = "edge_tts_voice"

    // Dashboard-Szenario
    const val PREF_DASHBOARD_SCENARIO = "dashboard_scenario"
    const val PREF_CUSTOM_PROMPT = "custom_dashboard_prompt"
    const val DASHBOARD_SCENARIO_COUNT = 5

    /**
     * JSON list of named custom analyses. Replaces the single-string PREF_CUSTOM_PROMPT. Format:
     * [{"id":"uuid","name":"...","prompt":"..."}, ...] Migration from PREF_CUSTOM_PROMPT happens
     * lazily in CustomAnalysesStore.load(). Szenario-Index 4..N-1 refers to the custom entry at
     * (scenario - 4) in this list.
     */
    const val PREF_CUSTOM_ANALYSES_JSON = "custom_analyses_json"

    // Indices 0..3 are fixed; from index 4 onwards: dynamic list of custom analyses.
    const val FIRST_CUSTOM_SCENARIO_INDEX = 4

    // Onboarding
    const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
    const val PREF_ONBOARDING_GOALS = "onboarding_goals"

    // Freemium
    const val TRIAL_USAGE_DAYS = 8
    const val FREE_REVIEW_PERIOD_DAYS = 14 // Legacy, no longer used for weekly review gating
    // Free users always get the N most recent weekly reviews, independent of first-entry date.
    const val FREE_WEEKLY_REVIEW_COUNT = 2
    const val FREE_WEEKLY_DASHBOARD_LIMIT = 5
    const val FREE_WEEKLY_TEXT_LIMIT = 5
    // Unlimited entries: Gemini 2.5/3.x models support 1M token context.
    // M-4 cost-limit removed per user request — every tier gets full analysis.
    const val MAX_ENTRIES_FREE_ANALYSIS = Int.MAX_VALUE
    const val MAX_ENTRIES_SUBSCRIBED_ANALYSIS = Int.MAX_VALUE
    const val MAX_ENTRIES_TRIAL_ANALYSIS = Int.MAX_VALUE

    // Trial daily limits (4-tier: Premium→Lite→Cooldown→Hard)
    const val TRIAL_PREMIUM_LIMIT = 20 // 1-20: Flash 2.5
    const val TRIAL_COOLDOWN_AT = 81 // 21-80: Lite, 81st: 30-min cooldown
    const val TRIAL_HARD_LIMIT = 101 // 101+: blocked for the day

    // Subscriber daily limits (4-tier, higher than trial)
    const val SUB_PREMIUM_LIMIT = 30 // 1-30: Flash 2.5
    const val SUB_COOLDOWN_AT = 101 // 31-100: Lite, 101st: 30-min cooldown
    const val SUB_HARD_LIMIT = 151 // 151+: blocked for the day

    // Cooldown duration (shared)
    const val COOLDOWN_MINUTES = 30

    // In-App Review
    const val PREF_LAST_REVIEW_PROMPT_DATE = "last_review_prompt_date"
    const val REVIEW_MIN_ENTRIES = 5
    const val REVIEW_MIN_STREAK = 2
    const val REVIEW_COOLDOWN_DAYS = 90L

    // Contextual Upsell
    const val PREF_FIRST_ANALYSIS_UPSELL_SHOWN = "first_analysis_upsell_shown"
    const val PREF_FIRST_TEXT_UPSELL_SHOWN = "first_text_upsell_shown"
    const val PREF_DASHBOARD_ANALYSIS_COUNT = "dashboard_analysis_count"
    const val PREF_TEXT_IMPROVEMENT_COUNT = "text_improvement_count"

    // Daily Reminder
    const val PREF_REMINDER_ENABLED = "reminder_enabled"
    const val PREF_REMINDER_HOUR = "reminder_hour"
    const val PREF_REMINDER_MINUTE = "reminder_minute"

    // Churn / Cancellation Flow
    const val PREF_CHURN_OFFER_ACCEPTED = "churn_offer_accepted"
    const val PREF_CHURN_OFFER_TIMESTAMP = "churn_offer_timestamp"

    // Retention offer (25% discount = pay 75% of the price)
    const val RETENTION_DISCOUNT_PERCENT = 25
    const val RETENTION_MONTHLY_PRICE = "2,99\u00A0\u20AC"
    const val RETENTION_YEARLY_PRICE = "22,49\u00A0\u20AC"
    const val RETENTION_OFFER_ID_MONTHLY = "retention-monthly-75"
    const val RETENTION_OFFER_ID_YEARLY = "retention-yearly-75"

    // Weekly Review
    const val PREF_WEEKLY_REVIEW_ENABLED = "weekly_review_enabled"
    const val PREF_WEEKLY_REVIEW_DAY = "weekly_review_day" // Calendar.MONDAY=2 .. SUNDAY=1
    const val PREF_WEEKLY_REVIEW_HOUR = "weekly_review_hour"
    const val PREF_WEEKLY_REVIEW_MINUTE = "weekly_review_minute"
    const val PREF_FROM_WEEKLY_REVIEW = "from_weekly_review"
    // Set to true when user switches dashboard scenario/profile.
    // RetrospectiveViewModel reads+clears this on init to auto-regenerate.
    const val PREF_RETRO_NEEDS_REGEN = "retro_needs_regen"
    const val PREF_MONTHLY_REVIEW_ENABLED = "monthly_review_enabled"
    const val PREF_YEARLY_REVIEW_ENABLED = "yearly_review_enabled"
    const val PREF_USER_TIMEZONE = "user_timezone"

    // Daily Writing Prompt
    const val PREF_PROMPT_DISMISSED_DATE = "prompt_dismissed_date"

    // Exit-intent offer: 50% for 2 months + 2 extra trial days
    const val EXIT_INTENT_DISCOUNT_MONTHS = 2
    const val EXIT_INTENT_TRIAL_BONUS_DAYS = 2
    const val PREF_EXIT_INTENT_TRIAL_EXTENDED = "exit_intent_trial_extended"
    // Tracks promo activation time so the in-app subscription overview can show
    // the truthful current price (e.g. "2,00 EUR for 2 months, then 3,99 EUR/mo")
    // instead of always showing the base plan price.
    const val PREF_PROMO_PURCHASE_TIME = "promo_purchase_time"
    const val PREF_PROMO_TOTAL_MONTHS = "promo_total_months"
    // Tracks the last observed purchase.purchaseTime — when it changes on the
    // monthly subscription, Google has triggered a renewal cycle, so the
    // promo's remaining months can be decremented. Works in both Internal
    // Testing (5-min cycles) and production (30-day cycles).
    const val PREF_PROMO_LAST_PURCHASE_TIME = "promo_last_purchase_time"
    // Active base plan + offer of the currently held subscription. Used so the
    // in-app subscription overview shows the truthful price of the plan the
    // user actually owns (e.g. retention-yearly-75 = 22,49 €/year), not the
    // main base plan default (29,99 €/year). Updated on every purchase and
    // cleared when no subscription is active.
    const val PREF_ACTIVE_BASE_PLAN_ID = "active_base_plan_id"
    const val PREF_ACTIVE_OFFER_ID = "active_offer_id"
    // Throttle timestamps for the Cloud Function. Two independent keys so that
    // a base-plan recovery call (one purpose) does not silence a promo-renewal
    // detection call (different purpose) for the next 60 minutes — both must
    // be allowed to run on their own cadence.
    const val PREF_LAST_CLOUD_STATUS_FETCH = "last_cloud_status_fetch" // base-plan recovery
    const val PREF_LAST_PROMO_CLOUD_FETCH = "last_promo_cloud_fetch" // promo expiry sync
    // Last expiryTime observed from the Cloud Function (server-authoritative).
    // Whenever the Cloud Function returns a NEWER expiryTime than this, we know
    // Google Play has renewed the subscription. This is the AUTHORITATIVE renewal
    // signal, because BillingClient.purchase.purchaseTime stays at the original
    // purchase time even after auto-renewals (confirmed via logcat 2026-04-30).
    const val PREF_LAST_KNOWN_EXPIRY = "last_known_subscription_expiry"
    // PurchaseToken whose acknowledgePurchase call has failed all 3 retries.
    // On the next successful BillingClient connection (next app start), we will
    // retry the acknowledgement so the user does not get an automatic refund
    // from Google after 3 days.
    const val PREF_PENDING_ACK_TOKEN = "pending_ack_token"
    // Authoritative current price in micros from the Cloud Function. Used to
    // override the BillingClient-derived price (which only knows the static
    // pricing phases list, not which one is active right now).
    const val PREF_CURRENT_PRICE_MICROS = "current_price_micros"
    const val PREF_CURRENT_PRICE_CURRENCY = "current_price_currency"
    // Whether auto-renewing is enabled. False after user cancels in Google Play.
    const val PREF_AUTO_RENEWING = "auto_renewing"
    // Industry-standard renewal indicator from Cloud Function.
    const val PREF_LAST_ORDER_ID = "last_order_id"
    // Currently-active pricing phase: "BASE", "INTRO", "FREE_TRIAL", or null.
    const val PREF_OFFER_PHASE = "offer_phase"
    // Loop-5 fix: once the Cloud Function has provided an authoritative
    // autoRenewing value, BillingClient's local Purchase.isAutoRenewing must
    // not overwrite it anymore — it can be stale for minutes after the user
    // cancels in the Play Store, causing the Settings dialog to flicker
    // between "gekuendigt" and "automatisch".
    const val PREF_AUTO_RENEWING_CLOUD_SEEN = "auto_renewing_cloud_seen"
    // Loop-5 fix: persisted purchase token + product id of the most recent
    // active subscription. Lets us call the Cloud Function to verify whether
    // a subscription has truly expired BEFORE downgrading the local state to
    // Free — BillingClient.queryPurchasesAsync can briefly return empty
    // during reconnects and the few minutes after a Play Store cancellation.
    const val PREF_LAST_KNOWN_PURCHASE_TOKEN = "last_known_purchase_token"
    const val PREF_LAST_KNOWN_PRODUCT_ID = "last_known_product_id"

    // DSGVO consent (shown once before onboarding on fresh install)
    const val PREF_CONSENT_SHOWN = "consent_shown"
    // Policy version acknowledged at last consent. Used to trigger re-consent
    // when privacy policy / SDK surface changes materially (Art. 7 Abs. 1 DSGVO).
    const val PREF_CONSENT_POLICY_VERSION = "consent_policy_version"
    // Epoch millis when consent record was last written (audit trail).
    const val PREF_CONSENT_TIMESTAMP = "consent_timestamp"
    // Current policy version — bump on material DSE/SDK changes to force re-consent.
    const val CURRENT_POLICY_VERSION = "3.1"

    // Firebase Analytics opt-in — default false.
    // Toggle in ConsentScreen AND Settings → Datenschutz switches it at runtime.
    const val PREF_ANALYTICS_ENABLED = "analytics_enabled"
    // Google Drive backup opt-in — default false. Checked by DriveBackupManager
    // before every sync operation to prevent background sync when off.
    const val PREF_DRIVE_BACKUP_ENABLED = "drive_backup_enabled"

    // CCPA/CPRA 2026 (California): "Do Not Sell My Personal Information" toggle.
    // When true: all cloud AI services (Groq, Gemini, Edge-TTS) per-service consents
    // are revoked and Firebase Analytics is disabled. User must explicitly re-consent
    // per service to re-enable any cloud feature.
    const val PREF_DO_NOT_SELL = "ccpa_do_not_sell"

    // Spam protection
    const val SPAM_HOURLY_AI_LIMIT = 30
    const val SPAM_HOURLY_AI_LIMIT_PREMIUM = 50

    // Subscription pricing — EUR fallbacks shown until Google Play returns localized prices
    const val MONTHLY_PRICE_DISPLAY = "3,99\u00A0\u20AC"
    const val YEARLY_PRICE_DISPLAY = "29,99\u00A0\u20AC"
    const val LIFETIME_PRICE_DISPLAY = "79,99\u00A0\u20AC"
}

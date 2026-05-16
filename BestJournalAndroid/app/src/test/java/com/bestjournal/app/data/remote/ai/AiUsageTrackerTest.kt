package com.bestjournal.app.data.remote.ai

import android.content.SharedPreferences
import com.bestjournal.app.util.Constants
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AiUsageTracker].
 *
 * Uses FakeSharedPreferences (HashMap-backed) to avoid any Android framework dependency. The 4-tier
 * access model under test: count < premiumLimit → Allowed(Flash) premiumLimit <= count < cooldownAt
 * → Allowed(FlashLite) count == cooldownAt (first time) → Cooldown(30 min) cooldownAt <= count <
 * hardLimit → Allowed(FlashLite) after cooldown served count >= hardLimit → HardLimitReached
 */
class AiUsageTrackerTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var tracker: AiUsageTracker

    @BeforeEach
    fun setUp() {
        prefs = FakeSharedPreferences()
        tracker = AiUsageTracker(prefs)
    }

    // ── Phase detection ──────────────────────────────────────────────────────

    @Nested
    inner class PhaseDetection {

        @Test
        fun `new user without usage days is TRIAL`() {
            assertEquals(AiPhase.TRIAL, tracker.getCurrentPhase())
        }

        @Test
        fun `after 7 usage days still TRIAL`() {
            repeat(7) { tracker.recordUsageDay() }
            assertEquals(AiPhase.TRIAL, tracker.getCurrentPhase())
        }

        @Test
        fun `after 8 usage days becomes FREEMIUM`() {
            // Simulate 8 distinct days by injecting distinct date strings
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            val days = (1..8).map { LocalDate.now().minusDays(it.toLong()).format(fmt) }
            prefs.edit().putString("ai_usage_days", days.joinToString(",")).apply()
            assertEquals(AiPhase.FREEMIUM, tracker.getCurrentPhase())
        }
    }

    // ── Dashboard 4-tier access (Trial limits) ───────────────────────────────

    @Nested
    inner class DashboardAccessTrial {

        private val premium = Constants.TRIAL_PREMIUM_LIMIT // 20
        private val cooldownAt = Constants.TRIAL_COOLDOWN_AT // 81
        private val hard = Constants.TRIAL_HARD_LIMIT // 101

        private fun access() = tracker.getDashboardAccessResult(premium, cooldownAt, hard)

        private fun setCount(n: Int) {
            prefs
                .edit()
                .putInt("dashboard_daily_count_0", n)
                .putString(
                    "dashboard_daily_date_0",
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                )
                .apply()
        }

        @Test
        fun `first use returns Flash model`() {
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `at premium limit boundary returns FlashLite`() {
            setCount(premium) // exactly at premiumLimit → Lite tier
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH_LITE,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `just below cooldownAt still returns FlashLite`() {
            setCount(cooldownAt - 1)
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH_LITE,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `at cooldownAt triggers 30-min cooldown first time`() {
            setCount(cooldownAt)
            val result = access()
            assertInstanceOf(TieredAccessResult.Cooldown::class.java, result)
            val cooldown = result as TieredAccessResult.Cooldown
            assertEquals(Constants.COOLDOWN_MINUTES, cooldown.minutesLeft)
            assertEquals(cooldownAt, cooldown.totalToday)
        }

        @Test
        fun `after cooldown served returns FlashLite again`() {
            setCount(cooldownAt + 5)
            // Mark cooldown as already served, no active cooldown timestamp
            prefs
                .edit()
                .putBoolean("dashboard_cooldown_until_0_served", true)
                .putLong("dashboard_cooldown_until_0", 0L)
                .apply()
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH_LITE,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `at hardLimit returns HardLimitReached`() {
            setCount(hard)
            val result = access()
            assertEquals(TieredAccessResult.HardLimitReached, result)
        }

        @Test
        fun `above hardLimit returns HardLimitReached`() {
            setCount(hard + 10)
            val result = access()
            assertEquals(TieredAccessResult.HardLimitReached, result)
        }
    }

    // ── Dashboard 4-tier access (Subscriber limits) ──────────────────────────

    @Nested
    inner class DashboardAccessSubscriber {

        private val premium = Constants.SUB_PREMIUM_LIMIT // 30
        private val cooldownAt = Constants.SUB_COOLDOWN_AT // 101
        private val hard = Constants.SUB_HARD_LIMIT // 151

        private fun access() = tracker.getDashboardAccessResult(premium, cooldownAt, hard)

        private fun setCount(n: Int) {
            prefs
                .edit()
                .putInt("dashboard_daily_count_0", n)
                .putString(
                    "dashboard_daily_date_0",
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                )
                .apply()
        }

        @Test
        fun `subscriber at 29 gets Flash`() {
            setCount(29)
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `subscriber at 30 gets FlashLite`() {
            setCount(30)
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH_LITE,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `subscriber at cooldownAt triggers cooldown`() {
            setCount(cooldownAt)
            val result = access()
            assertInstanceOf(TieredAccessResult.Cooldown::class.java, result)
        }

        @Test
        fun `subscriber at hardLimit gets blocked`() {
            setCount(hard)
            val result = access()
            assertEquals(TieredAccessResult.HardLimitReached, result)
        }
    }

    // ── Text improvement 4-tier access ──────────────────────────────────────

    @Nested
    inner class TextAccessTrial {

        private val premium = Constants.TRIAL_PREMIUM_LIMIT
        private val cooldownAt = Constants.TRIAL_COOLDOWN_AT
        private val hard = Constants.TRIAL_HARD_LIMIT

        private fun access() = tracker.getTextAccessResult(premium, cooldownAt, hard)

        private fun setCount(n: Int) {
            prefs
                .edit()
                .putInt("text_improvement_daily_count", n)
                .putString(
                    "text_improvement_daily_date",
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                )
                .apply()
        }

        @Test
        fun `first text improvement returns Flash`() {
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `text count at premiumLimit returns FlashLite`() {
            setCount(premium)
            val result = access()
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
            assertEquals(
                FirebaseAiService.MODEL_FLASH_LITE,
                (result as TieredAccessResult.Allowed).modelName,
            )
        }

        @Test
        fun `text count at cooldownAt triggers cooldown`() {
            setCount(cooldownAt)
            val result = access()
            assertInstanceOf(TieredAccessResult.Cooldown::class.java, result)
        }

        @Test
        fun `text count at hardLimit returns HardLimitReached`() {
            setCount(hard)
            val result = access()
            assertEquals(TieredAccessResult.HardLimitReached, result)
        }

        @Test
        fun `text and dashboard counters are independent`() {
            // Set text counter to hard limit — dashboard should still return Allowed
            setCount(hard)
            val dashboardResult = tracker.getDashboardAccessResult(premium, cooldownAt, hard)
            // Dashboard counter was never set, so it starts at 0 → Allowed(Flash)
            assertInstanceOf(TieredAccessResult.Allowed::class.java, dashboardResult)
        }
    }

    // ── Weekly free limits ───────────────────────────────────────────────────
    // Since 0.21.8 the dashboard quota is per scenario: each profile (0..3 built-in,
    // 4+ custom) has its own 5/week bucket. Text quota remains global.

    @Nested
    inner class WeeklyFreeLimits {

        @BeforeEach
        fun setFreemiumPhase() {
            // 8 distinct past days → FREEMIUM phase
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            val days = (1..8).map { LocalDate.now().minusDays(it.toLong()).format(fmt) }
            prefs.edit().putString("ai_usage_days", days.joinToString(",")).apply()
            // Set weekly reset to next Monday so counter doesn't reset during test
            val nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            prefs
                .edit()
                .putString("ai_weekly_reset", nextMonday.format(fmt)) // text reset
                .putString("ai_weekly_dashboard_reset_0", nextMonday.format(fmt)) // dashboard scenario 0
                .putString("ai_weekly_dashboard_reset_1", nextMonday.format(fmt)) // dashboard scenario 1
                .putString("ai_weekly_dashboard_reset_4", nextMonday.format(fmt)) // dashboard custom analysis
                .apply()
        }

        @Test
        fun `freemium dashboard allowed below weekly limit`() {
            prefs
                .edit()
                .putInt("weekly_dashboard_count_0", Constants.FREE_WEEKLY_DASHBOARD_LIMIT - 1)
                .apply()
            assertTrue(tracker.isFreeDashboardAllowed())
        }

        @Test
        fun `freemium dashboard blocked at weekly limit`() {
            prefs
                .edit()
                .putInt("weekly_dashboard_count_0", Constants.FREE_WEEKLY_DASHBOARD_LIMIT)
                .apply()
            assertFalse(tracker.isFreeDashboardAllowed())
        }

        @Test
        fun `freemium text allowed below weekly limit`() {
            prefs.edit().putInt("weekly_text_count", Constants.FREE_WEEKLY_TEXT_LIMIT - 1).apply()
            assertTrue(tracker.isFreeTextAllowed())
        }

        @Test
        fun `freemium text blocked at weekly limit`() {
            prefs.edit().putInt("weekly_text_count", Constants.FREE_WEEKLY_TEXT_LIMIT).apply()
            assertFalse(tracker.isFreeTextAllowed())
        }

        @Test
        fun `remaining uses counts down correctly`() {
            prefs.edit().putInt("weekly_text_count", 3).apply()
            assertEquals(Constants.FREE_WEEKLY_TEXT_LIMIT - 3, tracker.getRemainingFreeTextUses())
        }

        @Test
        fun `remaining uses never goes negative`() {
            prefs.edit().putInt("weekly_text_count", 999).apply()
            assertEquals(0, tracker.getRemainingFreeTextUses())
        }

        @Test
        fun `recordWeeklyTextUse increments counter`() {
            val before = tracker.getWeeklyTextCount()
            tracker.recordWeeklyTextUse()
            assertEquals(before + 1, tracker.getWeeklyTextCount())
        }

        @Test
        fun `recordWeeklyDashboardUse increments counter`() {
            val before = tracker.getWeeklyDashboardCount()
            tracker.recordWeeklyDashboardUse()
            assertEquals(before + 1, tracker.getWeeklyDashboardCount())
        }

        @Test
        fun `dashboard quota is independent across profiles`() {
            // Profile 0 has used all 5 — should be blocked
            prefs
                .edit()
                .putInt("weekly_dashboard_count_0", Constants.FREE_WEEKLY_DASHBOARD_LIMIT)
                .apply()
            tracker.setCurrentScenario(0)
            assertFalse(tracker.isFreeDashboardAllowed(), "Scenario 0 should be blocked at limit")
            assertEquals(0, tracker.getRemainingFreeDashboardUses())

            // Profile 1 has not been touched — should still have full quota
            tracker.setCurrentScenario(1)
            assertTrue(tracker.isFreeDashboardAllowed(), "Scenario 1 should still be allowed")
            assertEquals(
                Constants.FREE_WEEKLY_DASHBOARD_LIMIT,
                tracker.getRemainingFreeDashboardUses(),
            )

            // Custom analysis (scenario 4) — independent bucket
            tracker.setCurrentScenario(4)
            assertTrue(tracker.isFreeDashboardAllowed(), "Custom scenario 4 should be allowed")
            assertEquals(
                Constants.FREE_WEEKLY_DASHBOARD_LIMIT,
                tracker.getRemainingFreeDashboardUses(),
            )
        }

        @Test
        fun `recordWeeklyDashboardUse only affects active scenario`() {
            tracker.setCurrentScenario(2)
            tracker.recordWeeklyDashboardUse()
            tracker.recordWeeklyDashboardUse()
            // Scenario 2: 2 used
            assertEquals(2, tracker.getWeeklyDashboardCount())

            // Scenarios 0, 1, 3, 4 untouched
            for (other in listOf(0, 1, 3, 4)) {
                tracker.setCurrentScenario(other)
                assertEquals(
                    0,
                    tracker.getWeeklyDashboardCount(),
                    "Scenario $other counter should not have been incremented",
                )
            }
        }
    }

    // ── Active cooldown respected ────────────────────────────────────────────

    @Nested
    inner class ActiveCooldown {

        @Test
        fun `active dashboard cooldown returns Cooldown with correct minutes`() {
            val tenMinutesMs = 10 * 60_000L
            prefs
                .edit()
                .putLong("dashboard_cooldown_until_0", System.currentTimeMillis() + tenMinutesMs)
                .putInt("dashboard_daily_count_0", 50)
                .putString(
                    "dashboard_daily_date_0",
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                )
                .apply()
            val result =
                tracker.getDashboardAccessResult(
                    Constants.TRIAL_PREMIUM_LIMIT,
                    Constants.TRIAL_COOLDOWN_AT,
                    Constants.TRIAL_HARD_LIMIT,
                )
            assertInstanceOf(TieredAccessResult.Cooldown::class.java, result)
            val cooldown = result as TieredAccessResult.Cooldown
            assertTrue(
                cooldown.minutesLeft in 9..11,
                "Expected ~10 min left, got ${cooldown.minutesLeft}",
            )
        }

        @Test
        fun `expired dashboard cooldown is cleared`() {
            // Cooldown expired in the past
            prefs
                .edit()
                .putLong("dashboard_cooldown_until_0", System.currentTimeMillis() - 1000L)
                .putInt("dashboard_daily_count_0", 5)
                .putString(
                    "dashboard_daily_date_0",
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                )
                .apply()
            val result =
                tracker.getDashboardAccessResult(
                    Constants.TRIAL_PREMIUM_LIMIT,
                    Constants.TRIAL_COOLDOWN_AT,
                    Constants.TRIAL_HARD_LIMIT,
                )
            // count=5 < premiumLimit(20) → Allowed(Flash)
            assertInstanceOf(TieredAccessResult.Allowed::class.java, result)
        }
    }

    // ── Spam protection ──────────────────────────────────────────────────────

    @Nested
    inner class SpamProtection {

        @Test
        fun `hourly limit not reached initially`() {
            assertFalse(tracker.isHourlySpamLimitReached())
        }

        @Test
        fun `hourly limit reached after SPAM_HOURLY_AI_LIMIT calls`() {
            prefs
                .edit()
                .putInt("hourly_ai_count", Constants.SPAM_HOURLY_AI_LIMIT)
                .putLong("hourly_ai_reset", System.currentTimeMillis() + 3_600_000L)
                .apply()
            assertTrue(tracker.isHourlySpamLimitReached())
        }

        @Test
        fun `recordHourlyAiUsage increments counter`() {
            // Set a future reset so counter is not cleared
            prefs.edit().putLong("hourly_ai_reset", System.currentTimeMillis() + 3_600_000L).apply()
            val before = prefs.getInt("hourly_ai_count", 0)
            tracker.recordHourlyAiUsage()
            assertEquals(before + 1, prefs.getInt("hourly_ai_count", 0))
        }
    }
}

// ── FakeSharedPreferences — in-memory test double ────────────────────────────

/**
 * HashMap-backed SharedPreferences implementation for unit tests. Avoids all Android framework
 * dependencies. Commit/apply are both synchronous and in-memory.
 */
private class FakeSharedPreferences : SharedPreferences {

    private val map = mutableMapOf<String, Any?>()
    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = map.toMap()

    override fun getString(key: String, defValue: String?) = map[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?) =
        @Suppress("UNCHECKED_CAST") (map[key] as? Set<String> ?: defValues)

    override fun getInt(key: String, defValue: Int) = map[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long) = map[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float) = map[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean) = map[key] as? Boolean ?: defValue

    override fun contains(key: String) = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(map, listeners, this)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners.remove(listener)
    }

    private class FakeEditor(
        private val map: MutableMap<String, Any?>,
        private val listeners: List<SharedPreferences.OnSharedPreferenceChangeListener>,
        private val prefs: SharedPreferences,
    ) : SharedPreferences.Editor {

        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?) = apply { pending[key] = value }

        override fun putStringSet(key: String, values: Set<String>?) = apply {
            pending[key] = values
        }

        override fun putInt(key: String, value: Int) = apply { pending[key] = value }

        override fun putLong(key: String, value: Long) = apply { pending[key] = value }

        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }

        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }

        override fun remove(key: String) = apply { removals.add(key) }

        override fun clear() = apply { clearAll = true }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun applyChanges() {
            if (clearAll) map.clear()
            removals.forEach { map.remove(it) }
            map.putAll(pending)
            (pending.keys + removals).forEach { key ->
                listeners.forEach { it.onSharedPreferenceChanged(prefs, key) }
            }
        }
    }
}

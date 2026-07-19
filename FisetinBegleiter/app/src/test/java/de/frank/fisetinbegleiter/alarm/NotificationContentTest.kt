package de.frank.fisetinbegleiter.alarm

import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationContentTest {
    @Test
    fun `all notification types target the intended screen`() {
        assertEquals("timeline", notificationContent(AlarmType.MEAL_WARNING, 0).destination)
        assertEquals("timeline", notificationContent(AlarmType.SPERMIDIN_OPEN, 0).destination)
        assertEquals("timeline", notificationContent(AlarmType.SPERMIDIN_REMINDER, 0).destination)
        assertEquals("stack", notificationContent(AlarmType.BLOCK_ENDED, 0).destination)
        assertEquals("today", notificationContent(AlarmType.NEXT_DAY, 2).destination)
        assertEquals("history", notificationContent(AlarmType.CURE_ENDED, 3).destination)
        assertEquals("today", notificationContent(AlarmType.CURE_START, 1).destination)
        assertEquals("today", notificationContent(AlarmType.CURE_PREPARATION, 1).destination)
    }

    @Test
    fun `meal warning reflects custom protocol timings`() {
        val content = notificationContent(
            AlarmType.MEAL_WARNING,
            0,
            ProtocolTemplateEntity(mealDeadlineMinutes = 45, mealWarningMinutes = 35),
        )

        assertTrue(content.text.contains("10 Minuten"))
        assertTrue(content.text.contains("45-Minuten-Fenster"))
    }

    @Test
    fun `spermidin messages reflect remaining custom window`() {
        val protocol = ProtocolTemplateEntity(
            antioxidantBlockMinutes = 300,
            spermidinStartMinutes = 180,
            spermidinReminderMinutes = 270,
        )

        assertTrue(notificationContent(AlarmType.SPERMIDIN_OPEN, 0, protocol).text.contains("120 Minuten"))
        assertTrue(notificationContent(AlarmType.SPERMIDIN_REMINDER, 0, protocol).text.contains("30 Minuten"))
    }

    @Test
    fun `one day cure completion uses singular wording`() {
        assertTrue(notificationContent(AlarmType.CURE_ENDED, 1).text.contains("1 Tag"))
    }

    @Test
    fun `preparation reminder is exactly 24 hours before cure start`() {
        val cureStart = 2_000_000_000L

        assertEquals(cureStart - 86_400_000L, curePreparationAt(cureStart))
        assertEquals("Morgen beginnt eine Fisetin-Kur", notificationContent(AlarmType.CURE_PREPARATION, 1).title)
    }
}

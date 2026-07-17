package de.frank.fisetinbegleiter.domain

import de.frank.fisetinbegleiter.data.AppCureState
import de.frank.fisetinbegleiter.data.CureDayEntity
import de.frank.fisetinbegleiter.data.CureWithDays
import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import java.time.LocalDate
import java.time.ZoneId

data class DayTimeline(
    val t0: Long,
    val mealDeadline: Long,
    val spermidinStart: Long,
    val spermidinEnd: Long,
    val blockEnd: Long,
)

fun timeline(t0: Long, protocol: ProtocolTemplateEntity): DayTimeline = DayTimeline(
    t0 = t0,
    mealDeadline = t0 + protocol.mealDeadlineMinutes * 60_000L,
    spermidinStart = t0 + protocol.spermidinStartMinutes * 60_000L,
    spermidinEnd = t0 + protocol.antioxidantBlockMinutes * 60_000L,
    blockEnd = t0 + protocol.antioxidantBlockMinutes * 60_000L,
)

fun plannedStartAt(startDate: LocalDate, startMinuteOfDay: Int, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    startDate
        .atStartOfDay(zoneId)
        .plusMinutes(startMinuteOfDay.coerceIn(0, 1_439).toLong())
        .toInstant()
        .toEpochMilli()

fun currentDay(cure: CureWithDays?): CureDayEntity? = cure?.days
    ?.sortedBy { it.dayNumber }
    ?.firstOrNull { it.completedAt == null }
    ?: cure?.days?.maxByOrNull { it.dayNumber }

fun cureState(cure: CureWithDays?, protocol: ProtocolTemplateEntity, now: Long): AppCureState {
    if (cure == null) return AppCureState.KEINE_KUR
    if (cure.cure.completedAt != null) return AppCureState.KUR_ABGESCHLOSSEN
    val day = currentDay(cure) ?: return AppCureState.KUR_GEPLANT
    if (day.completedAt != null) return AppCureState.TAG_ABGESCHLOSSEN
    val t0 = day.t0 ?: return if (day.dayNumber == 1) AppCureState.KUR_GEPLANT else AppCureState.TAG_LAEUFT_VOR_DRINK
    return if (now < timeline(t0, protocol).blockEnd) {
        AppCureState.SPERRFENSTER_AKTIV
    } else {
        AppCureState.SPERRFENSTER_VORBEI
    }
}

fun canComplete(day: CureDayEntity?, now: Long, protocol: ProtocolTemplateEntity): Boolean {
    day ?: return false
    val t0 = day.t0 ?: return false
    return day.mealDoneAt != null &&
        day.spermidinDoneAt != null &&
        (day.blockEndedAt != null || now >= timeline(t0, protocol).blockEnd)
}

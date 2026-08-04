package de.frank.perfectmoment.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import de.frank.perfectmoment.data.local.HookEntity

/** One pickable symbol of a conversation hook, together with the hue of its disc. */
data class HookIcon(val key: String, val icon: ImageVector, val hue: Int)

/**
 * The symbols a conversation hook can be given.
 *
 * The choice is kept as `icon:<key>` in the hook's emoji column, so no database migration is
 * needed and hooks saved before the picker existed keep exactly the look they had: their value
 * matches no key, and [iconFor] then falls back to the hue derived from the hook itself.
 */
object HookIcons {
    const val PREFIX = "icon:"

    val all: List<HookIcon> = listOf(
        HookIcon("meditation", Icons.Outlined.SelfImprovement, 0),
        HookIcon("herz", Icons.Outlined.FavoriteBorder, 5),
        HookIcon("geist", Icons.Outlined.Psychology, 2),
        HookIcon("entdecken", Icons.Outlined.Explore, 3),
        HookIcon("idee", Icons.Outlined.Lightbulb, 4),
        HookIcon("funkeln", Icons.Outlined.AutoAwesome, 1),
        HookIcon("sonne", Icons.Outlined.WbSunny, 4),
        HookIcon("nacht", Icons.Outlined.NightsStay, 3),
        HookIcon("ruhe", Icons.Outlined.Spa, 2),
        HookIcon("bluete", Icons.Outlined.LocalFlorist, 5),
        HookIcon("atem", Icons.Outlined.Air, 3),
        HookIcon("wellen", Icons.Outlined.Waves, 3),
        HookIcon("landschaft", Icons.Outlined.Landscape, 2),
        HookIcon("natur", Icons.Outlined.Park, 2),
        HookIcon("tier", Icons.Outlined.Pets, 0),
        HookIcon("musik", Icons.Outlined.MusicNote, 1),
        HookIcon("kunst", Icons.Outlined.Palette, 1),
        HookIcon("buch", Icons.Outlined.MenuBook, 4),
        HookIcon("notiz", Icons.Outlined.EditNote, 4),
        HookIcon("energie", Icons.Outlined.Bolt, 4),
        HookIcon("feuer", Icons.Outlined.LocalFireDepartment, 0),
        HookIcon("klarheit", Icons.Outlined.Diamond, 3),
        HookIcon("erfolg", Icons.Outlined.EmojiEvents, 4),
        HookIcon("ziel", Icons.Outlined.Flag, 0),
        HookIcon("aufbruch", Icons.Outlined.RocketLaunch, 0),
        HookIcon("wachstum", Icons.Outlined.TrendingUp, 2),
        HookIcon("zeit", Icons.Outlined.Schedule, 3),
        HookIcon("kalender", Icons.Outlined.CalendarMonth, 3),
        HookIcon("verbindung", Icons.Outlined.Handshake, 4),
        HookIcon("menschen", Icons.Outlined.Groups, 3),
        HookIcon("zuhause", Icons.Outlined.Home, 0),
        HookIcon("arbeit", Icons.Outlined.Work, 2),
        HookIcon("koerper", Icons.Outlined.FitnessCenter, 5),
        HookIcon("genuss", Icons.Outlined.Restaurant, 4),
        HookIcon("pause", Icons.Outlined.Coffee, 0),
        HookIcon("schlaf", Icons.Outlined.Bedtime, 1),
        HookIcon("heilung", Icons.Outlined.Healing, 5),
        HookIcon("guete", Icons.Outlined.VolunteerActivism, 5),
        HookIcon("stern", Icons.Outlined.StarBorder, 4),
        HookIcon("feiern", Icons.Outlined.Celebration, 1),
    )

    /** What a newly created hook starts with, so its tile matches what gets saved. */
    val DEFAULT: HookIcon = all.first { it.key == "funkeln" }

    /** The stored value of a picked symbol, e.g. `icon:herz`. */
    fun valueOf(icon: HookIcon): String = PREFIX + icon.key

    /** The picked symbol behind a stored value, or null when the value is an old emoji. */
    fun find(stored: String): HookIcon? = stored.takeIf { it.startsWith(PREFIX) }
        ?.removePrefix(PREFIX)
        ?.let { key -> all.firstOrNull { it.key == key } }

    fun iconFor(hook: HookEntity): ImageVector = find(hook.emoji)?.icon ?: all[fallbackHue(hook)].icon

    fun colorFor(hook: HookEntity, dark: Boolean): Color =
        hueColor(find(hook.emoji)?.hue ?: fallbackHue(hook), dark)

    fun hueColor(hue: Int, dark: Boolean): Color = when (hue) {
        0 -> if (dark) Color(0xFFFF9E80) else Color(0xFFB94B32)
        1 -> if (dark) Color(0xFFC4A7E7) else Color(0xFF7954A1)
        2 -> if (dark) Color(0xFF75C8B2) else Color(0xFF2A7D69)
        3 -> if (dark) Color(0xFF8AB4F8) else Color(0xFF3F6FA8)
        4 -> if (dark) Color(0xFFFFC66D) else Color(0xFFA66B00)
        else -> if (dark) Color(0xFFF28BA8) else Color(0xFFA54462)
    }

    /** The look hooks had before the picker existed: one of the first six symbols by id. */
    private fun fallbackHue(hook: HookEntity): Int =
        ((hook.id.takeIf { it > 0L } ?: hook.sortIndex.toLong()) % 6L).toInt()
}

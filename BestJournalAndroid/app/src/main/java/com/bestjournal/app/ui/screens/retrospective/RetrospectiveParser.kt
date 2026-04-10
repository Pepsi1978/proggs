package com.bestjournal.app.ui.screens.retrospective

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

data class ParsedRetrospective(
    val bulletPoints: List<String>,
    val sections: List<RetrospectiveSection>,
    val rawText: String,
)

data class RetrospectiveSection(val heading: String, val body: String, val icon: ImageVector)

fun parseRetrospectiveText(text: String): ParsedRetrospective {
    val lines = text.lines()
    val bulletPoints = mutableListOf<String>()
    val sections = mutableListOf<RetrospectiveSection>()

    var currentHeading: String? = null
    var currentBody = StringBuilder()
    var parsingBullets = true

    for (line in lines) {
        val trimmed = line.trim()

        // Bullet points at the beginning
        if (parsingBullets && trimmed.startsWith("• ")) {
            bulletPoints.add(trimmed.removePrefix("• ").trim())
            continue
        }

        // Empty line after bullets transitions to sections
        if (parsingBullets && trimmed.isEmpty() && bulletPoints.isNotEmpty()) {
            parsingBullets = false
            continue
        }

        // If we're still in bullet mode but hit non-bullet text, switch
        if (parsingBullets && trimmed.isNotEmpty() && !trimmed.startsWith("• ")) {
            parsingBullets = false
        }

        // Section heading: [Something]
        val headingMatch = Regex("""^\[(.+?)]$""").find(trimmed)
        if (headingMatch != null) {
            // Save previous section
            if (currentHeading != null) {
                sections.add(
                    RetrospectiveSection(
                        heading = currentHeading!!,
                        body = currentBody.toString().trim(),
                        icon = iconForHeading(currentHeading!!),
                    )
                )
            }
            currentHeading = headingMatch.groupValues[1]
            currentBody = StringBuilder()
            continue
        }

        // Regular text — append to current section body
        if (currentHeading != null) {
            if (currentBody.isNotEmpty() || trimmed.isNotEmpty()) {
                currentBody.appendLine(trimmed)
            }
        }
    }

    // Save last section
    if (currentHeading != null && currentBody.isNotEmpty()) {
        sections.add(
            RetrospectiveSection(
                heading = currentHeading!!,
                body = currentBody.toString().trim(),
                icon = iconForHeading(currentHeading!!),
            )
        )
    }

    return ParsedRetrospective(bulletPoints = bulletPoints, sections = sections, rawText = text)
}

private val iconKeywords: List<Pair<List<String>, ImageVector>> =
    listOf(
        listOf("begegnung", "freund", "familie", "menschen", "zusammen", "gemeinschaft") to
            Icons.Rounded.Group,
        listOf("liebe", "herz", "zuneigung", "nähe", "verbundenheit") to Icons.Rounded.Favorite,
        listOf("arbeit", "beruf", "job", "karriere", "projekt", "büro") to
            Icons.Rounded.WorkOutline,
        listOf("lernen", "schule", "wissen", "bildung", "studium", "kurs") to Icons.Rounded.School,
        listOf("erkenntnis", "einsicht", "verstehen", "klarheit", "bewusst") to
            Icons.Rounded.Lightbulb,
        listOf("wachstum", "fortschritt", "entwicklung", "aufstieg", "besser") to
            Icons.Rounded.TrendingUp,
        listOf("ruhe", "stille", "meditation", "entspannung", "gelassen") to
            Icons.Rounded.SelfImprovement,
        listOf("gesundheit", "sport", "fitness", "bewegung", "training", "laufen") to
            Icons.Rounded.FitnessCenter,
        listOf("reise", "urlaub", "unterwegs", "abenteuer", "fliegen") to
            Icons.Rounded.FlightTakeoff,
        listOf("natur", "wandern", "draußen", "wald", "berg", "spazier") to Icons.Rounded.Hiking,
        listOf("kreativ", "kunst", "malen", "schreiben", "gestalten") to Icons.Rounded.Palette,
        listOf("musik", "konzert", "singen", "spielen", "melodie") to Icons.Rounded.MusicNote,
        listOf("erfolg", "sieg", "gewonnen", "geschafft", "erreicht", "triumph") to
            Icons.Rounded.EmojiEvents,
        listOf("ziel", "vorsatz", "plan", "vorhaben", "richtung") to Icons.Rounded.Flag,
        listOf("zuhause", "heim", "wohnung", "daheim", "geborgen") to Icons.Rounded.Home,
        listOf("feier", "geburtstag", "fest", "party", "jubiläum") to Icons.Rounded.Cake,
        listOf("frühling", "sommer", "sonne", "warm", "licht", "aufbruch") to Icons.Rounded.WbSunny,
        listOf("herbst", "winter", "dunkel", "kalt", "abend", "nacht") to Icons.Rounded.NightsStay,
        listOf("blühen", "blume", "garten", "wachsen", "pflanze") to Icons.Rounded.LocalFlorist,
        listOf("dankbar", "dankbarkeit", "schenken", "geben", "helfen") to
            Icons.Rounded.VolunteerActivism,
        listOf("gedanken", "nachdenken", "reflekt", "innere", "seele", "gefühl") to
            Icons.Rounded.Psychology,
        listOf("wellness", "pflege", "selbst", "auszeit", "balance") to Icons.Rounded.Spa,
        listOf("entdecken", "neu", "anfang", "start", "neugier") to Icons.Rounded.Explore,
        listOf("stern", "highlight", "besonder", "magisch", "wunder") to Icons.Rounded.Star,
    )

private fun iconForHeading(heading: String): ImageVector {
    val lower = heading.lowercase()
    for ((keywords, icon) in iconKeywords) {
        if (keywords.any { lower.contains(it) }) {
            return icon
        }
    }
    return Icons.Rounded.AutoAwesome // Default fallback
}

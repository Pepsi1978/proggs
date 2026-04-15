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

        // Section heading: [Something] — also handles **[Something]**, *[Something]*, ###
        // [Something]
        val cleaned = trimmed.replace("*", "").replace("#", "").trim()
        val headingMatch = Regex("""^\[(.+?)]$""").find(cleaned)
        if (headingMatch != null) {
            // Save previous section
            currentHeading?.let { heading ->
                sections.add(
                    RetrospectiveSection(
                        heading = heading,
                        body = currentBody.toString().trim(),
                        icon = iconForHeading(heading),
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
    if (currentBody.isNotEmpty()) {
        currentHeading?.let { heading ->
            sections.add(
                RetrospectiveSection(
                    heading = heading,
                    body = currentBody.toString().trim(),
                    icon = iconForHeading(heading),
                )
            )
        }
    }

    return ParsedRetrospective(bulletPoints = bulletPoints, sections = sections, rawText = text)
}

private val iconKeywords: List<Pair<List<String>, ImageVector>> =
    listOf(
        // German + English keywords for international icon matching
        listOf("begegnung", "freund", "familie", "menschen", "zusammen", "gemeinschaft",
            "encounter", "friend", "family", "people", "together", "community", "social") to
            Icons.Rounded.Group,
        listOf("liebe", "herz", "zuneigung", "nähe", "verbundenheit",
            "love", "heart", "affection", "closeness", "connection", "romance") to Icons.Rounded.Favorite,
        listOf("arbeit", "beruf", "job", "karriere", "projekt", "büro",
            "work", "career", "project", "office", "professional", "business") to
            Icons.Rounded.WorkOutline,
        listOf("lernen", "schule", "wissen", "bildung", "studium", "kurs",
            "learn", "school", "knowledge", "education", "study", "course") to Icons.Rounded.School,
        listOf("erkenntnis", "einsicht", "verstehen", "klarheit", "bewusst",
            "insight", "understand", "clarity", "aware", "realiz", "discover") to
            Icons.Rounded.Lightbulb,
        listOf("wachstum", "fortschritt", "entwicklung", "aufstieg", "besser",
            "growth", "progress", "develop", "improv", "better", "advance") to
            Icons.Rounded.TrendingUp,
        listOf("ruhe", "stille", "meditation", "entspannung", "gelassen",
            "calm", "quiet", "meditation", "relax", "peace", "mindful", "serene") to
            Icons.Rounded.SelfImprovement,
        listOf("gesundheit", "sport", "fitness", "bewegung", "training", "laufen",
            "health", "sport", "fitness", "exercise", "training", "running", "workout") to
            Icons.Rounded.FitnessCenter,
        listOf("reise", "urlaub", "unterwegs", "abenteuer", "fliegen",
            "travel", "vacation", "journey", "adventure", "trip", "flight") to
            Icons.Rounded.FlightTakeoff,
        listOf("natur", "wandern", "draußen", "wald", "berg", "spazier",
            "nature", "hiking", "outdoor", "forest", "mountain", "walk") to Icons.Rounded.Hiking,
        listOf("kreativ", "kunst", "malen", "schreiben", "gestalten",
            "creative", "art", "paint", "writing", "design", "craft") to Icons.Rounded.Palette,
        listOf("musik", "konzert", "singen", "spielen", "melodie",
            "music", "concert", "sing", "play", "melody", "song") to Icons.Rounded.MusicNote,
        listOf("erfolg", "sieg", "gewonnen", "geschafft", "erreicht", "triumph",
            "success", "victory", "won", "achiev", "accomplish", "triumph") to
            Icons.Rounded.EmojiEvents,
        listOf("ziel", "vorsatz", "plan", "vorhaben", "richtung",
            "goal", "resolution", "plan", "direction", "purpose", "ambition") to Icons.Rounded.Flag,
        listOf("zuhause", "heim", "wohnung", "daheim", "geborgen",
            "home", "house", "apartment", "domestic", "cozy", "comfort") to Icons.Rounded.Home,
        listOf("feier", "geburtstag", "fest", "party", "jubiläum",
            "celebrat", "birthday", "festival", "party", "anniversary") to Icons.Rounded.Cake,
        listOf("frühling", "sommer", "sonne", "warm", "licht", "aufbruch",
            "spring", "summer", "sun", "warm", "light", "bright", "new beginning") to Icons.Rounded.WbSunny,
        listOf("herbst", "winter", "dunkel", "kalt", "abend", "nacht",
            "autumn", "fall", "winter", "dark", "cold", "evening", "night") to Icons.Rounded.NightsStay,
        listOf("blühen", "blume", "garten", "wachsen", "pflanze",
            "bloom", "flower", "garden", "grow", "plant", "blossom") to Icons.Rounded.LocalFlorist,
        listOf("dankbar", "dankbarkeit", "schenken", "geben", "helfen",
            "grateful", "gratitude", "thankful", "giving", "help", "kindness") to
            Icons.Rounded.VolunteerActivism,
        listOf("gedanken", "nachdenken", "reflekt", "innere", "seele", "gefühl",
            "thought", "think", "reflect", "inner", "soul", "feeling", "emotion", "mental") to
            Icons.Rounded.Psychology,
        listOf("wellness", "pflege", "selbst", "auszeit", "balance",
            "wellness", "care", "self", "break", "balance", "restore") to Icons.Rounded.Spa,
        listOf("entdecken", "neu", "anfang", "start", "neugier",
            "discover", "new", "begin", "start", "curiosity", "explor") to Icons.Rounded.Explore,
        listOf("stern", "highlight", "besonder", "magisch", "wunder",
            "star", "highlight", "special", "magic", "wonder", "amazing") to Icons.Rounded.Star,
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

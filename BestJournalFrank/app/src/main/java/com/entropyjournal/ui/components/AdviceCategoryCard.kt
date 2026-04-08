package com.entropyjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nature
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SmokingRooms
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropyjournal.domain.model.AdviceBlock
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed

@Composable
fun AdviceCategoryCard(
    block: AdviceBlock,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryColor =
        try {
            Color(android.graphics.Color.parseColor(block.categoryColor))
        } catch (e: Exception) {
            NeonCyan
        }

    GlassCard(
        modifier = modifier.width(110.dp).height(100.dp).clickable { onClick() },
        glowColor = if (isSelected) categoryColor else categoryColor.copy(alpha = 0.3f),
        contentPadding = 6.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
        ) {
            // Onboarding-style icon: colored circle background + icon
            Box(
                modifier =
                    Modifier.size(36.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = getIconForCategory(block.categoryIcon),
                    contentDescription = block.categoryName,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = block.categoryName,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color =
                    if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            EntropyLevelIndicator(level = block.entropyLevel, color = categoryColor)
        }
    }
}

@Composable
private fun EntropyLevelIndicator(level: Float, color: Color, modifier: Modifier = Modifier) {
    val indicatorColor =
        when {
            level < 0.33f -> NeonEmerald
            level < 0.66f -> NeonAmber
            else -> NeonRed
        }

    androidx.compose.foundation.Canvas(modifier = modifier.size(36.dp, 18.dp)) {
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
        )
        drawArc(
            color = indicatorColor,
            startAngle = 180f,
            sweepAngle = 180f * level,
            useCenter = false,
            style =
                androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
        )
    }
}

fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        // Gesundheit & Koerper
        "bedtime",
        "sleep" -> Icons.Rounded.Bedtime
        "fitness_center",
        "fitness" -> Icons.Rounded.FitnessCenter
        "restaurant",
        "food",
        "ernaehrung" -> Icons.Rounded.Restaurant
        "health_and_safety",
        "health",
        "gesundheit" -> Icons.Rounded.HealthAndSafety
        "local_hospital",
        "medical",
        "arzt" -> Icons.Rounded.LocalHospital
        "directions_run",
        "running",
        "sport",
        "laufen" -> Icons.AutoMirrored.Rounded.DirectionsRun
        "spa",
        "wellness" -> Icons.Rounded.Spa
        "coffee",
        "kaffee" -> Icons.Rounded.Coffee

        // Psyche & Gefuehle
        "psychology",
        "mental",
        "psyche" -> Icons.Rounded.Psychology
        "self_improvement",
        "meditation",
        "achtsamkeit" -> Icons.Rounded.SelfImprovement
        "mood_bad",
        "gefuehle",
        "emotions",
        "trauer",
        "angst",
        "wut" -> Icons.Rounded.MoodBad
        "favorite",
        "love",
        "heart",
        "liebe" -> Icons.Rounded.Favorite
        "nights_stay",
        "einsamkeit",
        "loneliness" -> Icons.Rounded.NightsStay

        // Beziehungen & Soziales
        "people",
        "beziehungen",
        "freunde" -> Icons.Rounded.People
        "child_care",
        "kinder",
        "familie",
        "family" -> Icons.Rounded.ChildCare
        "cake",
        "dating",
        "partnerschaft" -> Icons.Rounded.Cake
        "volunteer_activism",
        "social",
        "sozial",
        "ehrenamt" -> Icons.Rounded.VolunteerActivism

        // Arbeit & Karriere
        "work",
        "arbeit",
        "job" -> Icons.Rounded.Work
        "trending_up",
        "karriere",
        "career" -> Icons.AutoMirrored.Rounded.TrendingUp
        "school",
        "schule",
        "studium",
        "pruefung" -> Icons.Rounded.School
        "code",
        "programmieren",
        "tech" -> Icons.Rounded.Code
        "computer",
        "digital",
        "bildschirm" -> Icons.Rounded.Computer
        "timer",
        "time",
        "produktivitaet",
        "zeitmanagement" -> Icons.Rounded.Timer

        // Finanzen
        "payments",
        "money",
        "finance",
        "finanzen",
        "geld" -> Icons.Rounded.Payments
        "account_balance",
        "bank" -> Icons.Rounded.AccountBalance
        "shopping_cart",
        "shopping",
        "einkaufen" -> Icons.Rounded.ShoppingCart

        // Zuhause & Alltag
        "home",
        "zuhause",
        "wohnung" -> Icons.Rounded.Home
        "cleaning_services",
        "haushalt",
        "putzen",
        "ordnung" -> Icons.Rounded.CleaningServices
        "directions_car",
        "car",
        "mobility",
        "auto",
        "pendeln" -> Icons.Rounded.DirectionsCar
        "gavel",
        "recht",
        "justiz",
        "buerokratie",
        "steuern" -> Icons.Rounded.Gavel

        // Freizeit & Hobbys
        "menu_book",
        "book",
        "lesen" -> Icons.Rounded.MenuBook
        "music_note",
        "music",
        "musik" -> Icons.Rounded.MusicNote
        "palette",
        "art",
        "creative",
        "kunst",
        "kreativ" -> Icons.Rounded.Palette
        "brush",
        "design" -> Icons.Rounded.Brush
        "sports_esports",
        "gaming",
        "spiele" -> Icons.Rounded.SportsEsports
        "photo_camera",
        "fotografie",
        "foto" -> Icons.Rounded.PhotoCamera
        "grass",
        "garten",
        "hobby" -> Icons.Rounded.Grass
        "travel",
        "travel_explore",
        "urlaub",
        "reisen" -> Icons.Rounded.TravelExplore
        "pets",
        "pet",
        "animal",
        "haustier" -> Icons.Rounded.Pets

        // Sucht & Laster
        "smoking_rooms",
        "rauchen",
        "smoke" -> Icons.Rounded.SmokingRooms
        "local_bar",
        "alkohol",
        "trinken",
        "sucht" -> Icons.Rounded.LocalBar

        // Spiritualitaet
        "church",
        "glaube",
        "spiritualitaet",
        "religion" -> Icons.Rounded.Church

        // Sonstiges
        "nature",
        "outdoor",
        "natur" -> Icons.Rounded.Nature
        "wb_sunny",
        "sun",
        "weather",
        "wetter" -> Icons.Rounded.WbSunny
        "lightbulb",
        "idea",
        "idee" -> Icons.Rounded.Lightbulb
        "star",
        "goals",
        "ziele" -> Icons.Rounded.Star
        "emoji_events",
        "achievement",
        "erfolg" -> Icons.Rounded.EmojiEvents
        "thermostat",
        "energy",
        "energie" -> Icons.Rounded.Thermostat

        else -> Icons.Rounded.Lightbulb
    }
}

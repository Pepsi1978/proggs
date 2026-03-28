package com.entropyjournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nature
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.ShoppingCart
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(block.categoryColor))
    } catch (e: Exception) {
        NeonCyan
    }

    ThreeDCard(
        modifier = modifier.width(100.dp),
        glowColor = if (isSelected) categoryColor else categoryColor.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconForCategory(block.categoryIcon),
                contentDescription = block.categoryName,
                tint = categoryColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = block.categoryName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            EntropyLevelIndicator(
                level = block.entropyLevel,
                color = categoryColor
            )
        }
    }
}

@Composable
private fun EntropyLevelIndicator(
    level: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when {
        level < 0.33f -> NeonEmerald
        level < 0.66f -> NeonAmber
        else -> NeonRed
    }

    androidx.compose.foundation.Canvas(
        modifier = modifier.size(40.dp, 20.dp)
    ) {
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
        drawArc(
            color = indicatorColor,
            startAngle = 180f,
            sweepAngle = 180f * level,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

fun getIconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        // Standard categories
        "bedtime" -> Icons.Rounded.Bedtime
        "work" -> Icons.Rounded.Work
        "fitness_center" -> Icons.Rounded.FitnessCenter
        "restaurant" -> Icons.Rounded.Restaurant
        "psychology" -> Icons.Rounded.Psychology
        "people" -> Icons.Rounded.People
        "home" -> Icons.Rounded.Home
        "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
        // Extended categories
        "code" -> Icons.Rounded.Code
        "computer" -> Icons.Rounded.Computer
        "school" -> Icons.Rounded.School
        "menu_book", "book" -> Icons.Rounded.MenuBook
        "self_improvement", "meditation" -> Icons.Rounded.SelfImprovement
        "favorite", "love", "heart" -> Icons.Rounded.Favorite
        "health_and_safety", "health" -> Icons.Rounded.HealthAndSafety
        "local_hospital", "medical" -> Icons.Rounded.LocalHospital
        "payments", "money", "finance" -> Icons.Rounded.Payments
        "account_balance", "bank" -> Icons.Rounded.AccountBalance
        "shopping_cart", "shopping" -> Icons.Rounded.ShoppingCart
        "directions_car", "car", "mobility" -> Icons.Rounded.DirectionsCar
        "directions_run", "running", "sport" -> Icons.AutoMirrored.Rounded.DirectionsRun
        "travel", "travel_explore" -> Icons.Rounded.TravelExplore
        "nature", "outdoor" -> Icons.Rounded.Nature
        "wb_sunny", "sun", "weather" -> Icons.Rounded.WbSunny
        "pets", "pet", "animal" -> Icons.Rounded.Pets
        "music_note", "music" -> Icons.Rounded.MusicNote
        "palette", "art", "creative" -> Icons.Rounded.Palette
        "brush", "design" -> Icons.Rounded.Brush
        "sports_esports", "gaming" -> Icons.Rounded.SportsEsports
        "lightbulb", "idea" -> Icons.Rounded.Lightbulb
        "timer", "time", "productivity" -> Icons.Rounded.Timer
        "star", "goals" -> Icons.Rounded.Star
        "emoji_events", "achievement" -> Icons.Rounded.EmojiEvents
        "volunteer_activism", "social" -> Icons.Rounded.VolunteerActivism
        "thermostat", "energy" -> Icons.Rounded.Thermostat
        else -> Icons.Rounded.Lightbulb
    }
}

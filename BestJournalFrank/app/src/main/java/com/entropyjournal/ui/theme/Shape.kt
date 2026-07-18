package com.entropyjournal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

object JournalRadii {
    val tiny = 4.dp
    val small = 8.dp
    val input = 14.dp
    val container = 16.dp
    val card = 18.dp
    val dialog = 22.dp
    val navigation = 26.dp
    val feature = 28.dp
    val pill = 999.dp
}

object JournalSpacing {
    val hairline = 1.dp
    val micro = 2.dp
    val tiny = 4.dp
    val compact = 6.dp
    val small = 8.dp
    val item = 10.dp
    val regular = 12.dp
    val cardGap = 14.dp
    val cardPadding = 16.dp
    val screenHorizontal = 18.dp
    val large = 20.dp
    val dialog = 24.dp
    val section = 32.dp
}

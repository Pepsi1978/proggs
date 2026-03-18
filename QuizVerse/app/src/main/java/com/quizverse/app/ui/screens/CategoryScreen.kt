package com.quizverse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.AnimalsEnd
import com.quizverse.app.ui.theme.AnimalsStart
import com.quizverse.app.ui.theme.DortmundEnd
import com.quizverse.app.ui.theme.DortmundStart
import com.quizverse.app.ui.theme.FilmEnd
import com.quizverse.app.ui.theme.FilmStart
import com.quizverse.app.ui.theme.FoodEnd
import com.quizverse.app.ui.theme.FoodStart
import com.quizverse.app.ui.theme.GeoEnd
import com.quizverse.app.ui.theme.GeoStart
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldLight
import com.quizverse.app.ui.theme.HerthaEnd
import com.quizverse.app.ui.theme.HerthaStart
import com.quizverse.app.ui.theme.HistoryEnd
import com.quizverse.app.ui.theme.HistoryStart
import com.quizverse.app.ui.theme.LiteratureEnd
import com.quizverse.app.ui.theme.LiteratureStart
import com.quizverse.app.ui.theme.LogicEnd
import com.quizverse.app.ui.theme.LogicStart
import com.quizverse.app.ui.theme.MixedEnd
import com.quizverse.app.ui.theme.MixedStart
import com.quizverse.app.ui.theme.MusicEnd
import com.quizverse.app.ui.theme.MusicStart
import com.quizverse.app.ui.theme.ScienceEnd
import com.quizverse.app.ui.theme.ScienceStart
import com.quizverse.app.ui.theme.SportEnd
import com.quizverse.app.ui.theme.SportStart
import com.quizverse.app.ui.theme.TechEnd
import com.quizverse.app.ui.theme.TechStart
import kotlinx.coroutines.delay

// Data model for a category displayed as a full-width row
private data class CategoryItem(
    val id: Int,
    val name: String,
    val emoji: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val glowColor: Color = Color.Transparent,
    val subcategories: List<CategoryItem> = emptyList()
)

// ── Main categories — IDs MUST match categoryId in Question entities ────────
private val categories = listOf(
    CategoryItem(11, "Alle Kategorien", "\uD83C\uDF1F", MixedStart, MixedEnd, MixedStart),
    CategoryItem(1, "Weltgeographie", "\uD83C\uDF0D", GeoStart, GeoEnd, GeoStart),
    CategoryItem(2, "Wissenschaft & Natur", "\uD83D\uDD2C", ScienceStart, ScienceEnd, ScienceStart),
    CategoryItem(3, "Geschichte", "\uD83D\uDCDC", HistoryStart, HistoryEnd, HistoryStart),
    CategoryItem(4, "Film & Fernsehen", "\uD83C\uDFAC", FilmStart, FilmEnd, FilmStart),
    CategoryItem(5, "Musik", "\uD83C\uDFB5", MusicStart, MusicEnd, MusicStart),
    // Sport has Bundesliga subcategories
    CategoryItem(
        id = 6, name = "Sport", emoji = "\u26BD",
        gradientStart = SportStart, gradientEnd = SportEnd,
        glowColor = SportStart,
        subcategories = listOf(
            CategoryItem(13, "Hertha BSC", "\uD83D\uDC99", HerthaStart, HerthaEnd, HerthaStart),
            CategoryItem(14, "Borussia Dortmund", "\uD83D\uDC9B", DortmundStart, DortmundEnd, DortmundStart),
        )
    ),
    CategoryItem(7, "Technologie", "\uD83D\uDCBB", TechStart, TechEnd, TechStart),
    CategoryItem(8, "Essen & Trinken", "\uD83C\uDF73", FoodStart, FoodEnd, FoodStart),
    CategoryItem(9, "Tierwelt", "\uD83D\uDC3E", AnimalsStart, AnimalsEnd, AnimalsStart),
    CategoryItem(10, "Sprache & Literatur", "\uD83D\uDCDA", LiteratureStart, LiteratureEnd, LiteratureStart),
    CategoryItem(12, "Logik & Denksport", "\uD83E\uDDE0", LogicStart, LogicEnd, LogicStart),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kategorie wählen",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            itemsIndexed(categories) { index, category ->
                CategoryRow(
                    category = category,
                    cardIndex = index,
                    animationDelayMillis = index * 55,
                    onCategoryClick = { id ->
                        navController.navigate(Screen.Difficulty.createRoute(id))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ── Full-width category row with glassmorphism + colored glow shadow ─────────
@Composable
private fun CategoryRow(
    category: CategoryItem,
    cardIndex: Int,
    animationDelayMillis: Int,
    onCategoryClick: (Int) -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }
    var expanded by remember { mutableStateOf(false) }
    val hasSubcategories = category.subcategories.isNotEmpty()

    // Entrance stagger: slide up + fade in
    LaunchedEffect(Unit) {
        delay(animationDelayMillis.toLong())
        alpha.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(animationDelayMillis.toLong())
        offsetY.animateTo(0f, animationSpec = tween(380, easing = FastOutSlowInEasing))
    }

    // ── Continuous floating animation — each card gets unique timing ─────────
    val floatDurationY = 2800 + cardIndex * 230
    val floatDurationX = 3600 + cardIndex * 180

    val floatTransition = rememberInfiniteTransition(label = "float_${category.id}")

    val floatY = floatTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(floatDurationY, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y_${category.id}"
    )

    val floatX = floatTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(floatDurationX, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_x_${category.id}"
    )

    // Glow pulse for the colored shadow
    val glowPulse = floatTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(floatDurationY, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_${category.id}"
    )

    // Shimmer sweep
    val shimmer = floatTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200 + cardIndex * 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_${category.id}"
    )

    // ── Press interaction ────────────────────────────────────────────────────
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressTranslateY by animateFloatAsState(
        targetValue = if (isPressed) 5f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "neu_translateY_${category.id}"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "neu_scale_${category.id}"
    )
    val outerShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(100),
        label = "neu_outerShadow_${category.id}"
    )
    val innerShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 0f,
        animationSpec = tween(100),
        label = "neu_innerShadow_${category.id}"
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.08f else 0.5f,
        animationSpec = tween(100),
        label = "neu_highlight_${category.id}"
    )
    val edgeShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0.25f,
        animationSpec = tween(100),
        label = "neu_edgeShadow_${category.id}"
    )

    val shape = RoundedCornerShape(20.dp)
    val cr = CornerRadius(20.dp.value, 20.dp.value)

    Column(
        modifier = Modifier
            .alpha(alpha.value)
            .graphicsLayer { translationY = offsetY.value.dp.toPx() }
    ) {
        // Colored glow shadow under card (drawn via outer Box with background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(8.dp)
                .alpha(glowPulse.value * outerShadowAlpha)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            category.glowColor.copy(alpha = 0.6f),
                            category.gradientEnd.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // Main category card — gradient + glassmorphism border + 3D edges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                    translationY = (floatY.value + pressTranslateY).dp.toPx()
                    translationX = floatX.value.dp.toPx()
                    this.shape = shape
                    clip = false
                    shadowElevation = 14.dp.toPx() * outerShadowAlpha
                    ambientShadowColor = category.glowColor.copy(alpha = 0.35f * outerShadowAlpha)
                    spotShadowColor = category.glowColor.copy(alpha = 0.45f * outerShadowAlpha)
                }
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            category.gradientStart,
                            category.gradientEnd,
                            category.gradientEnd.copy(alpha = 0.85f)
                        )
                    )
                )
                // Glassmorphism + shimmer + 3D neumorphic edges
                .drawWithContent {
                    drawContent()

                    val w = size.width
                    val h = size.height

                    // Subtle glass fill overlay
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.05f),
                        size = size,
                        cornerRadius = cr
                    )

                    // Shimmer sweep
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.13f),
                                Color.Transparent
                            ),
                            startX = shimmer.value * w * 1.4f - w * 0.2f,
                            endX = shimmer.value * w * 1.4f + w * 0.15f
                        ),
                        size = size,
                        cornerRadius = cr
                    )

                    // Top highlight edge — bright white strip
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = highlightAlpha),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 7.dp.toPx()
                        ),
                        size = Size(w, 7.dp.toPx()),
                        cornerRadius = cr
                    )
                    // Left highlight edge
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = highlightAlpha * 0.7f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 5.dp.toPx()
                        ),
                        size = Size(5.dp.toPx(), h),
                        cornerRadius = cr
                    )

                    // Bottom shadow edge
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = edgeShadowAlpha)
                            ),
                            startY = h - 7.dp.toPx(),
                            endY = h
                        ),
                        topLeft = Offset(0f, h - 7.dp.toPx()),
                        size = Size(w, 7.dp.toPx()),
                        cornerRadius = cr
                    )
                    // Right shadow edge
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = edgeShadowAlpha * 0.6f)
                            ),
                            startX = w - 5.dp.toPx(),
                            endX = w
                        ),
                        topLeft = Offset(w - 5.dp.toPx(), 0f),
                        size = Size(5.dp.toPx(), h),
                        cornerRadius = cr
                    )

                    // Inner shadow overlay when pressed
                    if (innerShadowAlpha > 0.01f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = innerShadowAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(w * 0.3f, h * 0.3f)
                            ),
                            size = size,
                            cornerRadius = cr
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = innerShadowAlpha * 0.2f)
                                ),
                                start = Offset(w * 0.7f, h * 0.7f),
                                end = Offset(w, h)
                            ),
                            size = size,
                            cornerRadius = cr
                        )
                    }

                    // Glassmorphism border — dual-layer for depth
                    drawRoundRect(
                        color = Color.White.copy(alpha = highlightAlpha * 0.55f),
                        size = size,
                        cornerRadius = cr,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (hasSubcategories) {
                        expanded = !expanded
                    } else {
                        onCategoryClick(category.id)
                    }
                }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Larger emoji in glass circle with gold accent ring for "Alle Kategorien"
                val isAllCategories = category.id == 11
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .drawWithContent {
                            drawContent()
                            // Inner glass gradient on icon container
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.5f
                                ),
                                cornerRadius = CornerRadius(16.dp.toPx())
                            )
                            // Gold border for premium "Alle Kategorien" card
                            if (isAllCategories) {
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GoldLight, Gold)
                                    ),
                                    cornerRadius = CornerRadius(16.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.emoji, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (hasSubcategories) {
                        Text(
                            text = "${category.subcategories.size} Vereine",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    // Gold "Premium" badge for "Alle Kategorien"
                    if (isAllCategories) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.3f), GoldLight.copy(alpha = 0.2f)))
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "★ Alle Kategorien",
                                fontSize = 10.sp,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (hasSubcategories) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                } else {
                    Text(
                        text = "\u203A",
                        fontSize = 26.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }

        // ── Clickable main "Sport allgemein" button when expanded ──
        if (hasSubcategories) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(260)),
                exit = shrinkVertically(animationSpec = tween(210))
            ) {
                Column(
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Alle Sport-Fragen" button
                    SubcategoryCard(
                        emoji = "\u26BD",
                        name = "Sport allgemein",
                        gradientStart = category.gradientStart.copy(alpha = 0.7f),
                        gradientEnd = category.gradientEnd.copy(alpha = 0.7f),
                        onClick = { onCategoryClick(category.id) }
                    )

                    // Individual club subcategories
                    category.subcategories.forEach { sub ->
                        SubcategoryCard(
                            emoji = sub.emoji,
                            name = sub.name,
                            gradientStart = sub.gradientStart,
                            gradientEnd = sub.gradientEnd,
                            onClick = { onCategoryClick(sub.id) }
                        )
                    }
                }
            }
        }
    }
}

// ── Subcategory card with glassmorphism style ─────────────────────────────────
@Composable
private fun SubcategoryCard(
    emoji: String,
    name: String,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressTranslateY by animateFloatAsState(
        targetValue = if (isPressed) 5f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "sub_translateY"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "sub_scale"
    )
    val outerShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(100),
        label = "sub_outerShadow"
    )
    val innerShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 0f,
        animationSpec = tween(100),
        label = "sub_innerShadow"
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.08f else 0.5f,
        animationSpec = tween(100),
        label = "sub_highlight"
    )
    val edgeShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0.25f,
        animationSpec = tween(100),
        label = "sub_edgeShadow"
    )

    val shape = RoundedCornerShape(14.dp)
    val cr = CornerRadius(14.dp.value, 14.dp.value)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = pressTranslateY.dp.toPx()
                this.shape = shape
                clip = false
                shadowElevation = 8.dp.toPx() * outerShadowAlpha
                ambientShadowColor = Color.Black.copy(alpha = 0.3f * outerShadowAlpha)
                spotShadowColor = Color.Black.copy(alpha = 0.4f * outerShadowAlpha)
            }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            )
            .drawWithContent {
                drawContent()

                val w = size.width
                val h = size.height

                // Glass overlay
                drawRoundRect(color = Color.White.copy(alpha = 0.05f), size = size, cornerRadius = cr)

                // Top highlight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = highlightAlpha), Color.Transparent),
                        startY = 0f, endY = 5.dp.toPx()
                    ),
                    size = Size(w, 5.dp.toPx()), cornerRadius = cr
                )
                // Left highlight
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White.copy(alpha = highlightAlpha * 0.7f), Color.Transparent),
                        startX = 0f, endX = 4.dp.toPx()
                    ),
                    size = Size(4.dp.toPx(), h), cornerRadius = cr
                )
                // Bottom shadow
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = edgeShadowAlpha)),
                        startY = h - 5.dp.toPx(), endY = h
                    ),
                    topLeft = Offset(0f, h - 5.dp.toPx()), size = Size(w, 5.dp.toPx()), cornerRadius = cr
                )
                // Right shadow
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = edgeShadowAlpha * 0.6f)),
                        startX = w - 4.dp.toPx(), endX = w
                    ),
                    topLeft = Offset(w - 4.dp.toPx(), 0f), size = Size(4.dp.toPx(), h), cornerRadius = cr
                )

                // Inner press shadow
                if (innerShadowAlpha > 0.01f) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Black.copy(alpha = innerShadowAlpha * 0.5f), Color.Transparent),
                            start = Offset(0f, 0f), end = Offset(w * 0.3f, h * 0.3f)
                        ),
                        size = size, cornerRadius = cr
                    )
                }

                // Glass border
                drawRoundRect(
                    color = Color.White.copy(alpha = highlightAlpha * 0.4f),
                    size = size, cornerRadius = cr, style = Stroke(width = 1.5.dp.toPx())
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "\u203A",
                fontSize = 22.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryScreenPreview() {
    CategoryScreen(navController = rememberNavController())
}

package com.quizverse.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// Data model for a category tile displayed in the grid
private data class CategoryItem(
    val id: Int,
    val name: String,
    val emoji: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val isSubcategory: Boolean = false
)

// ── Main categories — IDs MUST match categoryId in Question entities ────────
private val mainCategories = listOf(
    CategoryItem(1,  "Weltgeographie",      "\uD83C\uDF0D", GeoStart,        GeoEnd),
    CategoryItem(2,  "Wissenschaft & Natur","\uD83D\uDD2C", ScienceStart,    ScienceEnd),
    CategoryItem(3,  "Geschichte",          "\uD83D\uDCDC", HistoryStart,    HistoryEnd),
    CategoryItem(4,  "Film & Fernsehen",    "\uD83C\uDFAC", FilmStart,       FilmEnd),
    CategoryItem(5,  "Musik",               "\uD83C\uDFB5", MusicStart,      MusicEnd),
    CategoryItem(6,  "Sport",               "\u26BD",       SportStart,      SportEnd),
    CategoryItem(7,  "Technologie",         "\uD83D\uDCBB", TechStart,       TechEnd),
    CategoryItem(8,  "Essen & Trinken",     "\uD83C\uDF73", FoodStart,       FoodEnd),
    CategoryItem(9,  "Tierwelt",            "\uD83D\uDC3E", AnimalsStart,    AnimalsEnd),
    CategoryItem(10, "Sprache & Literatur", "\uD83D\uDCDA", LiteratureStart, LiteratureEnd),
    CategoryItem(11, "Bunt Gemischt",       "\uD83C\uDFB2", MixedStart,      MixedEnd),
    CategoryItem(12, "Logik & Denksport",   "\uD83E\uDDE0", LogicStart,      LogicEnd),
)

// ── Bundesliga subcategories ────────────────────────────────────────────────
private val bundesligaCategories = listOf(
    CategoryItem(13, "Hertha BSC",          "\u26BD", HerthaStart,   HerthaEnd,   isSubcategory = true),
    CategoryItem(14, "Borussia Dortmund",   "\u26BD", DortmundStart, DortmundEnd, isSubcategory = true),
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Main categories grid (2 columns) ──
            itemsIndexed(mainCategories) { index, category ->
                AnimatedCategoryCard(
                    category = category,
                    animationDelayMillis = index * 50,
                    onClick = {
                        navController.navigate(Screen.Difficulty.createRoute(category.id))
                    }
                )
            }

            // ── Section header for Bundesliga ──
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)) {
                    Text(
                        text = "\u26BD Bundesliga-Vereine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Teste dein Wissen über deinen Lieblingsverein",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ── Bundesliga category cards ──
            itemsIndexed(bundesligaCategories) { index, category ->
                AnimatedCategoryCard(
                    category = category,
                    animationDelayMillis = (mainCategories.size + index) * 50,
                    onClick = {
                        navController.navigate(Screen.Difficulty.createRoute(category.id))
                    }
                )
            }
        }
    }
}

// Single category card with gradient background and staggered slide-up entrance
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedCategoryCard(
    category: CategoryItem,
    animationDelayMillis: Int,
    onClick: () -> Unit
) {
    val offsetY = remember { Animatable(50f) }
    val alpha = remember { Animatable(0f) }

    // Staggered entrance: slide up + fade in
    LaunchedEffect(Unit) {
        delay(animationDelayMillis.toLong())
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        delay(animationDelayMillis.toLong())
        alpha.animateTo(1f, animationSpec = tween(350))
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .alpha(alpha.value),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            category.gradientStart,
                            category.gradientEnd
                        )
                    )
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Emoji icon
                Text(
                    text = category.emoji,
                    fontSize = 40.sp
                )
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryScreenPreview() {
    CategoryScreen(navController = rememberNavController())
}

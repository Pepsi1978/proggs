package com.quizverse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val subcategories: List<CategoryItem> = emptyList()
)

// ── Main categories — IDs MUST match categoryId in Question entities ────────
private val categories = listOf(
    CategoryItem(11, "Alle Kategorien", "\uD83C\uDF1F", MixedStart, MixedEnd),
    CategoryItem(1, "Weltgeographie", "\uD83C\uDF0D", GeoStart, GeoEnd),
    CategoryItem(2, "Wissenschaft & Natur", "\uD83D\uDD2C", ScienceStart, ScienceEnd),
    CategoryItem(3, "Geschichte", "\uD83D\uDCDC", HistoryStart, HistoryEnd),
    CategoryItem(4, "Film & Fernsehen", "\uD83C\uDFAC", FilmStart, FilmEnd),
    CategoryItem(5, "Musik", "\uD83C\uDFB5", MusicStart, MusicEnd),
    // Sport has Bundesliga subcategories
    CategoryItem(
        id = 6, name = "Sport", emoji = "\u26BD",
        gradientStart = SportStart, gradientEnd = SportEnd,
        subcategories = listOf(
            CategoryItem(13, "Hertha BSC", "\uD83D\uDC99", HerthaStart, HerthaEnd),
            CategoryItem(14, "Borussia Dortmund", "\uD83D\uDC9B", DortmundStart, DortmundEnd),
        )
    ),
    CategoryItem(7, "Technologie", "\uD83D\uDCBB", TechStart, TechEnd),
    CategoryItem(8, "Essen & Trinken", "\uD83C\uDF73", FoodStart, FoodEnd),
    CategoryItem(9, "Tierwelt", "\uD83D\uDC3E", AnimalsStart, AnimalsEnd),
    CategoryItem(10, "Sprache & Literatur", "\uD83D\uDCDA", LiteratureStart, LiteratureEnd),
    CategoryItem(12, "Logik & Denksport", "\uD83E\uDDE0", LogicStart, LogicEnd),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            itemsIndexed(categories) { index, category ->
                CategoryRow(
                    category = category,
                    animationDelayMillis = index * 50,
                    onCategoryClick = { id ->
                        navController.navigate(Screen.Difficulty.createRoute(id))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ── Full-width category row with optional expandable subcategories ───────────
@Composable
private fun CategoryRow(
    category: CategoryItem,
    animationDelayMillis: Int,
    onCategoryClick: (Int) -> Unit
) {
    val alpha = remember { Animatable(0f) }
    var expanded by remember { mutableStateOf(false) }
    val hasSubcategories = category.subcategories.isNotEmpty()

    LaunchedEffect(Unit) {
        delay(animationDelayMillis.toLong())
        alpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }

    Column(modifier = Modifier.alpha(alpha.value)) {
        // Main category card
        Card(
            onClick = {
                if (hasSubcategories) {
                    expanded = !expanded
                } else {
                    onCategoryClick(category.id)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(category.gradientStart, category.gradientEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Emoji in semi-transparent circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = category.emoji, fontSize = 24.sp)
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
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (hasSubcategories) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Einklappen" else "Ausklappen",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            text = "\u203A",
                            fontSize = 24.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }

        // ── Clickable main "Sport allgemein" button when expanded ──
        if (hasSubcategories) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(200))
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

// ── Subcategory card (indented, slightly smaller) ────────────────────────────
@Composable
private fun SubcategoryCard(
    emoji: String,
    name: String,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 18.sp)
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
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.7f)
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

package com.quizverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.navigation.Screen

// Data model for a category tile displayed in the grid
private data class CategoryItem(val id: Int, val name: String, val emoji: String)

// Hardcoded list of 12 quiz categories with emoji icons
private val categories = listOf(
    CategoryItem(1, "Wissenschaft", "🔬"),
    CategoryItem(2, "Geschichte", "🏛️"),
    CategoryItem(3, "Geografie", "🌍"),
    CategoryItem(4, "Sport", "⚽"),
    CategoryItem(5, "Musik", "🎵"),
    CategoryItem(6, "Film & TV", "🎬"),
    CategoryItem(7, "Technologie", "💻"),
    CategoryItem(8, "Natur", "🌿"),
    CategoryItem(9, "Kunst", "🎨"),
    CategoryItem(10, "Literatur", "📚"),
    CategoryItem(11, "Mathematik", "🔢"),
    CategoryItem(12, "Essen & Trinken", "🍕")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Kategorie wählen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
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
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onClick = {
                        navController.navigate(Screen.Difficulty.createRoute(category.id))
                    }
                )
            }
        }
    }
}

// Single category card showing emoji and name
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(category: CategoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.emoji,
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryScreenPreview() {
    CategoryScreen(navController = rememberNavController())
}

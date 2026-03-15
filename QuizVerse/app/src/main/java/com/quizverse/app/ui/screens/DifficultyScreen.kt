package com.quizverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.navigation.Screen

// Difficulty levels mapped to their Int value and German display name
private val difficultyLevels = listOf(
    1 to "Leicht",
    2 to "Mittel",
    3 to "Schwer",
    4 to "Experte",
    5 to "Meister"
)

// Available question counts for a quiz session
private val questionCounts = listOf(10, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyScreen(categoryId: Int, navController: NavHostController) {
    // Currently selected difficulty level (default: Mittel = 2)
    var selectedDifficulty by remember { mutableIntStateOf(2) }
    // Currently selected number of questions (default: 10)
    var selectedCount by remember { mutableIntStateOf(10) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Schwierigkeitsgrad") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Wähle deinen Schwierigkeitsgrad",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Difficulty selection buttons
            difficultyLevels.forEach { (level, label) ->
                if (selectedDifficulty == level) {
                    Button(
                        onClick = { selectedDifficulty = level },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(text = label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedDifficulty = level },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(text = label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Anzahl der Fragen",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Question count chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                questionCounts.forEach { count ->
                    FilterChip(
                        selected = selectedCount == count,
                        onClick = { selectedCount = count },
                        label = { Text(text = count.toString()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Start quiz with the chosen settings
            Button(
                onClick = {
                    navController.navigate(
                        Screen.Quiz.createRoute(
                            categoryId = categoryId,
                            difficulty = selectedDifficulty,
                            questionCount = selectedCount
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Quiz starten")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DifficultyScreenPreview() {
    DifficultyScreen(categoryId = 1, navController = rememberNavController())
}

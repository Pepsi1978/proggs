package com.quizverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "QuizVerse") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App title
            Text(
                text = "QuizVerse",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App subtitle
            Text(
                text = "Wissen ist Abenteuer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Start quiz – navigates to category selection
            Button(
                onClick = { navController.navigate(Screen.Category.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Quiz starten")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily challenge – navigates to the daily challenge screen
            OutlinedButton(
                onClick = { navController.navigate(Screen.DailyChallenge.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Tägliche Herausforderung")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Leaderboard navigation
            OutlinedButton(
                onClick = { navController.navigate(Screen.Leaderboard.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Bestenliste")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile navigation
            OutlinedButton(
                onClick = { navController.navigate(Screen.Profile.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Profil")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Settings navigation
            OutlinedButton(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Einstellungen")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}

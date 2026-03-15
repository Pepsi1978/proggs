package com.quizverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
fun ResultScreen(
    score: Int,
    total: Int,
    correct: Int,
    categoryId: Int,
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Ergebnis") }
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
            Text(
                text = "Glückwunsch!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Display total score
            Text(
                text = "Punkte: $score",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show how many answers were correct
            Text(
                text = "Richtig: $correct / $total",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Play the same category again
            Button(
                onClick = {
                    navController.navigate(Screen.Difficulty.createRoute(categoryId)) {
                        popUpTo(Screen.Category.route)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Nochmal spielen")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigate back to home screen
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = "Startseite")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    ResultScreen(
        score = 850,
        total = 10,
        correct = 8,
        categoryId = 1,
        navController = rememberNavController()
    )
}

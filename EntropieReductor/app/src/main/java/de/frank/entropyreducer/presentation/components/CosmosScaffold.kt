package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Wrapper-Scaffold mit transparenter Top-Bar — Hintergrund kommt vom Theme.
 */
@Composable
fun CosmosScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val cosmos = LocalCosmos.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = navigationIcon,
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = cosmos.textPrimary,
                    navigationIconContentColor = cosmos.textPrimary,
                    actionIconContentColor = cosmos.textPrimary,
                ),
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content(padding)
        }
    }
}

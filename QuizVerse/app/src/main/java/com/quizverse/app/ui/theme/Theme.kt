package com.quizverse.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Controls which colour mode the app should use. */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM,
}

/**
 * Root theme composable for QuizVerse.
 *
 * @param appTheme  Explicit theme override; defaults to [AppTheme.SYSTEM].
 * @param windowSizeClass  Provides adaptive layout breakpoints to child composables.
 * @param content   The composable tree to render inside the theme.
 */
@Composable
fun QuizVerseTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    windowSizeClass: WindowSizeClass? = null,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (appTheme) {
        AppTheme.LIGHT  -> false
        AppTheme.DARK   -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current

    // Use Material You dynamic colors on Android 12+ (API 31), fall back to brand colors.
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else              dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else         -> LightColorScheme
    }

    // Apply status-bar color to match the surface/background of the theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = QuizVerseTypography,
        shapes      = QuizVerseShapes,
        content     = content,
    )
}

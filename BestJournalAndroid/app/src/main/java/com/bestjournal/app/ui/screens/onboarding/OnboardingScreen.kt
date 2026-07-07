package com.bestjournal.app.ui.screens.onboarding

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.bestjournal.app.R
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.theme.CustomPalette
import com.bestjournal.app.ui.theme.GoalPalette
import com.bestjournal.app.ui.theme.InsightPalette
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.SummaryPalette
import com.bestjournal.app.ui.theme.WarmCopper
import com.bestjournal.app.ui.theme.WarmGold
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// Onboarding — 5 immersive screens via HorizontalPager
// Consistent with existing GlassCard / Spotify-dark / Warm-Copper UI
// ═══════════════════════════════════════════════════════════════════

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    // Skip immediately if already completed (returning user after login)
    if (viewModel.isCompleted) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    val pagerState = rememberPagerState(initialPage = 0) { 5 }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.analyticsTracker.trackOnboardingStarted()
    }
    LaunchedEffect(pagerState.currentPage) {
        viewModel.analyticsTracker.trackOnboardingScreenViewed(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle warm radial glow at top center
        BackgroundGlow()

        // Gradient fade at bottom so content never collides with controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // isActive flips true only when the pager has settled on this page, so
            // the page's entrance animations start fresh at the moment the user sees it.
            val isActive = pagerState.settledPage == page
            when (page) {
                0 -> WelcomePage(isActive = isActive)
                1 -> PersonalizationPage(viewModel = viewModel, isActive = isActive)
                2 -> HowItWorksPage(isActive = isActive)
                3 -> ProfilesPage(isActive = isActive)
                4 -> TrialPage(
                    isActive = isActive,
                    onSkip = {
                        viewModel.analyticsTracker.trackOnboardingSkipped(4)
                        viewModel.saveGoals()
                        viewModel.completeOnboarding()
                        onFinished()
                    }
                )
            }
        }

        // Bottom controls — dots + "Weiter" (screens 0-3 only)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PageIndicator(
                currentPage = pagerState.currentPage,
                pageCount = 5
            )

            if (pagerState.currentPage < 4) {
                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage == 1) viewModel.saveGoals()
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .widthIn(min = 180.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmCopper
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        stringResource(R.string.action_continue),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


// ─── Page 1: Willkommen ─────────────────────────────────────

@Composable
private fun WelcomePage(isActive: Boolean) {
    val visible = remember { mutableStateOf(false) }
    // Reset + replay entrance animation every time this page becomes the settled page
    LaunchedEffect(isActive) {
        if (isActive) {
            visible.value = false
            kotlinx.coroutines.delay(30)
            visible.value = true
        } else {
            visible.value = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 56.dp, bottom = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero glow orb
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -60 }
        ) {
            HeroGlowOrb()
        }

        Spacer(Modifier.height(28.dp))

        // Headline
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600, delayMillis = 300)) +
                    slideInVertically(tween(600, delayMillis = 300)) { -40 }
        ) {
            Text(
                stringResource(R.string.onboarding_welcome),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(12.dp))

        // Subtitle
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600, delayMillis = 500)) +
                    slideInVertically(tween(600, delayMillis = 500)) { -30 }
        ) {
            Text(
                stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(36.dp))

        // Three benefit rows — staggered entrance
        val benefits = listOf(
            stringResource(R.string.onboarding_feature_speech),
            stringResource(R.string.onboarding_feature_listen),
            stringResource(R.string.onboarding_feature_profiles),
            stringResource(R.string.onboarding_feature_secure)
        )
        benefits.forEachIndexed { idx, text ->
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(500, delayMillis = 700 + idx * 150)) +
                        slideInHorizontally(tween(500, delayMillis = 700 + idx * 150)) { -80 }
            ) {
                BenefitRow(text = text, tint = NeonEmerald)
            }
            if (idx < benefits.lastIndex) Spacer(Modifier.height(14.dp))
        }
    }
}


// ─── Page 2: Personalisierung ───────────────────────────────

@Composable
private fun PersonalizationPage(viewModel: OnboardingViewModel, isActive: Boolean) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            visible.value = false
            kotlinx.coroutines.delay(30)
            visible.value = true
        } else {
            visible.value = false
        }
    }

    val selectedGoals by viewModel.selectedGoals.collectAsStateWithLifecycle()

    val goals = listOf(
        "stress" to stringResource(R.string.onboarding_goal_stress),
        "clarity" to stringResource(R.string.onboarding_goal_clarity),
        "growth" to stringResource(R.string.onboarding_goal_growth),
        "thoughts" to stringResource(R.string.onboarding_goal_thoughts),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 72.dp, bottom = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Decorative icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(WarmCopper.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = WarmCopper,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    stringResource(R.string.onboarding_goals_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    stringResource(R.string.onboarding_goals_select),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Goal chips — staggered from below
        goals.forEachIndexed { idx, (key, label) ->
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(500, delayMillis = 300 + idx * 120)) +
                        slideInVertically(tween(500, delayMillis = 300 + idx * 120)) { 60 }
            ) {
                GoalChip(
                    text = label,
                    selected = key in selectedGoals,
                    onClick = { viewModel.toggleGoal(key) }
                )
            }
            if (idx < goals.lastIndex) Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GoalChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "chipScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) WarmCopper else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "chipBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) WarmCopper.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(250),
        label = "chipBg"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) WarmCopper.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .then(
                        if (!selected) Modifier.border(
                            1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = WarmCopper,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                ),
                color = if (selected) WarmCopper
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


// ─── Page 3: So funktioniert es ─────────────────────────────

@Composable
private fun HowItWorksPage(isActive: Boolean) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            visible.value = false
            kotlinx.coroutines.delay(30)
            visible.value = true
        } else {
            visible.value = false
        }
    }

    data class Step(
        val icon: ImageVector,
        val iconTint: Color,
        val title: String,
        val description: String
    )

    val steps = listOf(
        Step(
            Icons.Rounded.Mic, NeonCyan,
            stringResource(R.string.onboarding_how_step1_title),
            stringResource(R.string.onboarding_how_step1_desc)
        ),
        Step(
            Icons.Rounded.AutoAwesome, WarmCopper,
            stringResource(R.string.onboarding_how_step2_title),
            stringResource(R.string.onboarding_how_step2_desc)
        ),
        Step(
            Icons.Rounded.PhotoCamera, NeonEmerald,
            stringResource(R.string.onboarding_how_step3_title),
            stringResource(R.string.onboarding_how_step3_desc)
        ),
        Step(
            Icons.Rounded.Dashboard, InsightPalette.primary,
            stringResource(R.string.onboarding_how_step4_title),
            stringResource(R.string.onboarding_how_step4_desc)
        ),
        Step(
            Icons.Rounded.CalendarMonth, GoalPalette.primary,
            stringResource(R.string.onboarding_how_step5_title),
            stringResource(R.string.onboarding_how_step5_desc)
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 72.dp, bottom = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
        ) {
            Text(
                stringResource(R.string.onboarding_how_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        // Steps inside a GlassCard
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600, delayMillis = 200))
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = WarmCopper,
                contentPadding = 20.dp
            ) {
                Column {
                    steps.forEachIndexed { idx, step ->
                        StepItem(
                            icon = step.icon,
                            iconTint = step.iconTint,
                            title = step.title,
                            description = step.description
                        )
                        if (idx < steps.lastIndex) {
                            // Gradient connector line
                            Box(
                                modifier = Modifier
                                    .padding(start = 20.dp, top = 6.dp, bottom = 6.dp)
                                    .width(2.dp)
                                    .height(20.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                step.iconTint.copy(alpha = 0.4f),
                                                steps[idx + 1].iconTint.copy(alpha = 0.1f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ─── Page 4: Die 5 Profile ──────────────────────────────────

private data class ProfileInfo(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
private fun ProfilesPage(isActive: Boolean) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            visible.value = false
            kotlinx.coroutines.delay(30)
            visible.value = true
        } else {
            visible.value = false
        }
    }

    val profiles = listOf(
        ProfileInfo(stringResource(R.string.profile_summary), stringResource(R.string.profile_summary_desc), Icons.Rounded.AutoStories, SummaryPalette.accent),
        ProfileInfo(stringResource(R.string.profile_entropy), stringResource(R.string.profile_entropy_desc), Icons.Rounded.Whatshot, WarmCopper),
        ProfileInfo(stringResource(R.string.profile_insight), stringResource(R.string.profile_insight_desc), Icons.Rounded.SelfImprovement, InsightPalette.primary),
        ProfileInfo(stringResource(R.string.profile_goals), stringResource(R.string.profile_goals_desc), Icons.Rounded.RocketLaunch, GoalPalette.primary),
        ProfileInfo(stringResource(R.string.profile_custom), stringResource(R.string.profile_custom_desc), Icons.Rounded.Science, CustomPalette.primary)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 72.dp, bottom = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
        ) {
            Text(
                stringResource(R.string.onboarding_perspectives_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))

        // Profile cards — alternating slide direction
        profiles.forEachIndexed { idx, profile ->
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(500, delayMillis = 200 + idx * 100)) +
                        slideInHorizontally(tween(500, delayMillis = 200 + idx * 100)) {
                            if (idx % 2 == 0) -60 else 60
                        }
            ) {
                ProfileCard(profile)
            }
            if (idx < profiles.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ProfileCard(profile: ProfileInfo) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = profile.accentColor,
        cornerRadius = 16.dp,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Colored accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(profile.accentColor)
            )

            // Profile icon in colored circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(profile.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    profile.icon,
                    contentDescription = null,
                    tint = profile.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}


// ─── Page 5: Trial CTA ─────────────────────────────────────

@Composable
private fun TrialPage(isActive: Boolean, onSkip: () -> Unit) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            visible.value = false
            kotlinx.coroutines.delay(30)
            visible.value = true
        } else {
            visible.value = false
        }
    }

    val benefits = listOf(
        stringResource(R.string.onboarding_premium_feature_improve),
        stringResource(R.string.onboarding_premium_profiles),
        stringResource(R.string.onboarding_premium_profiles_unlimited),
        stringResource(R.string.onboarding_premium_feature_dashboard),
        stringResource(R.string.onboarding_premium_feature_retro),
        stringResource(R.string.onboarding_premium_feature_patterns),
        stringResource(R.string.onboarding_premium_feature_pdf),
        stringResource(R.string.onboarding_premium_feature_noads),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 64.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with star icon + headline
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    WarmCopper.copy(alpha = 0.2f),
                                    WarmGold.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = WarmCopper,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    stringResource(R.string.onboarding_try_premium),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Benefit rows — staggered from left
        benefits.forEachIndexed { idx, text ->
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn(tween(500, delayMillis = 300 + idx * 120)) +
                        slideInHorizontally(tween(500, delayMillis = 300 + idx * 120)) { -60 }
            ) {
                BenefitRow(text = text, tint = WarmCopper)
            }
            if (idx < benefits.lastIndex) Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(36.dp))

        // CTA Button + Skip
        AnimatedVisibility(
            visible = visible.value,
            enter = fadeIn(tween(600, delayMillis = 900)) +
                    slideInVertically(tween(600, delayMillis = 900)) { 80 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmCopper
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        stringResource(R.string.onboarding_without_premium),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}


// ─── Shared Components ──────────────────────────────────────

@Composable
private fun BenefitRow(text: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val width by animateDpAsState(
                targetValue = if (index == currentPage) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                label = "dotWidth$index"
            )
            val color by animateColorAsState(
                targetValue = if (index == currentPage) WarmCopper
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                animationSpec = tween(300),
                label = "dotColor$index"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun HeroGlowOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "heroGlow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(20000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(
        modifier = Modifier
            .size(180.dp)
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer warm halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WarmCopper.copy(alpha = 0.2f),
                    WarmGold.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            )
        )

        // Rotating ring segments
        rotate(ringRotation, center) {
            for (i in 0..5) {
                drawArc(
                    color = WarmCopper.copy(alpha = 0.06f),
                    startAngle = i * 60f,
                    sweepAngle = 25f,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius * 0.6f,
                        center.y - radius * 0.6f
                    ),
                    size = Size(radius * 1.2f, radius * 1.2f),
                    style = Stroke(
                        width = 2f * density,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Middle ring
        drawCircle(
            color = WarmCopper.copy(alpha = 0.08f),
            radius = radius * 0.45f,
            center = center,
            style = Stroke(width = 1.5f * density)
        )

        // Inner core glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WarmCopper.copy(alpha = 0.45f),
                    WarmGold.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.3f
            )
        )

        // Bright center dot
        drawCircle(
            color = WarmCopper.copy(alpha = 0.7f),
            radius = radius * 0.08f,
            center = center
        )
    }
}

@Composable
private fun BackgroundGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WarmCopper.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(size.width / 2, size.height * 0.15f),
                radius = size.width * 0.8f
            )
        )
    }
}

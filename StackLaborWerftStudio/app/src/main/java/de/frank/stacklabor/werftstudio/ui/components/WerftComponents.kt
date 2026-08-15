package de.frank.stacklabor.werftstudio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import de.frank.stacklabor.werftstudio.ui.model.GoalUi
import de.frank.stacklabor.werftstudio.ui.model.MedicineUi
import de.frank.stacklabor.werftstudio.ui.model.SignalCounts
import de.frank.stacklabor.werftstudio.ui.model.SignalState
import de.frank.stacklabor.werftstudio.ui.model.Solubility
import de.frank.stacklabor.werftstudio.ui.model.StackSummaryUi
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme
import de.frank.stacklabor.werftstudio.ui.theme.GoldDarkContent
import de.frank.stacklabor.werftstudio.ui.theme.bevel
import de.frank.stacklabor.werftstudio.ui.theme.darkenBy
import de.frank.stacklabor.werftstudio.ui.theme.depthShadow
import de.frank.stacklabor.werftstudio.ui.theme.goldActionSurface
import de.frank.stacklabor.werftstudio.ui.theme.lightenBy
import de.frank.stacklabor.werftstudio.ui.theme.metalRim
import de.frank.stacklabor.werftstudio.ui.theme.pressDepth
import de.frank.stacklabor.werftstudio.ui.theme.raisedSurface
import de.frank.stacklabor.werftstudio.ui.theme.sheen
import de.frank.stacklabor.werftstudio.ui.theme.softMetalRim
import androidx.compose.foundation.interaction.MutableInteractionSource

private val CardShape = RoundedCornerShape(12.dp)

private fun Modifier.goldCardShadow(shape: RoundedCornerShape = CardShape) =
    depthShadow(shape, 14.dp)

@Composable
fun WerftScreen(modifier: Modifier = Modifier, goldDark: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val body: @Composable () -> Unit = {
        Surface(modifier = modifier.fillMaxSize(), color = StackLaborTheme.colors.background) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    ),
                content = content,
            )
        }
    }
    if (goldDark) GoldDarkContent(body) else body()
}

@Composable
fun GlassHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onOverflow: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    framedBack: Boolean = false,
) {
    val colors = StackLaborTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .depthShadow(RoundedCornerShape(0.dp), 12.dp)
            .background(colors.glass)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, colors.textStrong.copy(alpha = 0.05f)),
                    ),
                )
                drawLine(
                    Brush.horizontalGradient(
                        listOf(colors.accent.copy(alpha = 0.25f), colors.accent, colors.accent.copy(alpha = 0.25f)),
                    ),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1.5.dp.toPx(),
                )
            }
            .statusBarsPadding()
            .height(StackLaborTheme.dimens.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            if (framedBack) Spacer(Modifier.width(12.dp))
            IconTouchButton(
                "Zurück",
                onBack,
                if (framedBack) {
                    Modifier.depthShadow(CircleShape, 8.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(colors.surface.lightenBy(0.25f), colors.elevated)))
                        .border(1.dp, metalRim(0.7f), CircleShape)
                } else Modifier,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp)) }
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1)
        }
        if (onOverflow != null) {
            IconTouchButton("Weitere Optionen", onOverflow) { Icon(Icons.Default.MoreVert, null, Modifier.size(24.dp)) }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
fun IconTouchButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.requiredSize(StackLaborTheme.dimens.minTouch).semantics { contentDescription = description },
    ) { content() }
}

@Composable
fun AnimatedGradientHeader(
    animationsEnabled: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "headGradient")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(30_000), RepeatMode.Restart),
            label = "headGradientProgress",
        )
        value
    } else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .depthShadow(RoundedCornerShape(0.dp), 18.dp, strength = 1.3f)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF6F4813), Color(0xFFD8AE55), Color(0xFF8B5E1A)),
                    start = Offset(progress * 600f - 300f, 0f),
                    end = Offset(progress * 600f + 300f, 0f),
                ),
            )
            // Curved metal look: light collects along the top, the lower third falls away.
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0.00f to Color.White.copy(alpha = 0.26f),
                        0.30f to Color.White.copy(alpha = 0.05f),
                        0.55f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.28f),
                    ),
                )
                drawLine(Color(0xFFFFF1CB).copy(alpha = 0.55f), Offset(0f, 0f), Offset(size.width, 0f), 1.5.dp.toPx())
                drawLine(
                    Color(0xFF3A2405).copy(alpha = 0.55f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1.5.dp.toPx(),
                )
            }
            .statusBarsPadding()
            .height(96.dp),
        content = content,
    )
}

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GoldSurface(modifier.heightIn(min = 48.dp), onClick = onClick) {
        Text(
            label,
            // Ohne diesen Innenabstand klebt die Schrift am Rand, sobald der Knopf sich
            // nach seinem Text richtet statt die volle Breite zu nehmen.
            Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2A1B05),
            maxLines = 1,
        )
    }
}

/**
 * The physical gold key every primary action is built from: lit face, sheen across the
 * upper half, a metal rim and a shadow that collapses while the finger is down.
 */
@Composable
fun GoldSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CardShape,
    elevation: Dp = 14.dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressDepth(interaction, shape, elevation)
            .clip(shape)
            .background(goldActionSurface())
            .sheen()
            .border(1.dp, Color(0xFFF3DFAE).copy(alpha = 0.75f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * A raised panel with the continuous metal rim. Every card, row and menu in the app uses
 * this so the rim never fades out on one side.
 */
@Composable
fun RaisedPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CardShape,
    elevation: Dp = 12.dp,
    rimWidth: Dp = 1.dp,
    rimAlpha: Float = 0.75f,
    bevelStrength: Float = 1f,
    background: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val base = if (onClick != null) {
        modifier.pressDepth(interaction, shape, elevation)
    } else {
        modifier.depthShadow(shape, elevation)
    }
    Box(
        base
            .clip(shape)
            .background(background ?: raisedSurface())
            .bevel(bevelStrength)
            .border(rimWidth, metalRim(rimAlpha), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier,
            ),
        content = content,
    )
}

@Composable
fun SearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldHeight: Dp = 40.dp,
) {
    val colors = StackLaborTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(fieldHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(24.dp), tint = colors.textMuted)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = colors.textStrong),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = colors.textMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    inner()
                }
            },
        )
    }
}

@Composable
fun SelectPill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = StackLaborTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressDepth(interaction, CircleShape, if (selected) 10.dp else 3.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(
                if (selected) goldActionSurface()
                else Brush.verticalGradient(listOf(colors.surface, colors.elevated.copy(alpha = 0.6f))),
            )
            .then(if (selected) Modifier.sheen() else Modifier)
            .border(1.dp, if (selected) metalRim(1f) else softMetalRim(0.35f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xFF2A1B05) else colors.textMuted,
        )
    }
}

@Composable
fun SignalDot(state: SignalState, modifier: Modifier = Modifier, description: String? = null) {
    val colors = StackLaborTheme.colors
    val color = state.color()
    Box(
        modifier
            .size(8.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
fun SignalCountsRow(counts: SignalCounts, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Count(SignalState.Green, counts.green)
        Count(SignalState.Yellow, counts.yellow)
        Count(SignalState.Red, counts.red)
    }
}

@Composable
private fun Count(state: SignalState, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        SignalDot(state)
        Text(count.toString(), style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
    }
}

@Composable
fun StackCard(
    stack: StackSummaryUi,
    animationsEnabled: Boolean,
    onOpen: () -> Unit,
    onCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StackLaborTheme.colors
    val aura = if (animationsEnabled && stack.signal == SignalState.Red) {
        val transition = rememberInfiniteTransition(label = "redAura")
        val alpha by transition.animateFloat(
            0.22f,
            0.08f,
            infiniteRepeatable(tween(1_200), RepeatMode.Reverse),
            label = "redAuraAlpha",
        )
        alpha
    } else 0f
    RaisedPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(StackLaborTheme.dimens.stackHeight)
            .drawBehind {
                if (aura > 0f) drawCircle(colors.red.copy(alpha = aura), radius = size.maxDimension * 0.65f, center = center)
            },
        elevation = 16.dp,
        rimWidth = 1.5.dp,
        rimAlpha = 1f,
        onClick = onOpen,
    ) {
        Row(Modifier.fillMaxSize()) {
            SignalBar(stack.signal.color())
            Column(Modifier.weight(1f).padding(start = 12.dp, top = 7.dp, bottom = 7.dp)) {
                Text(stack.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stack.meta, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1)
                Spacer(Modifier.weight(1f))
                SignalCountsRow(stack.counts)
            }
            Box(
                Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .clickable(onClick = onCatalog)
                    .padding(end = 8.dp, bottom = 7.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Text("${stack.medicineCount} Mittel", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
    }
}

/**
 * The coloured signal strip on the left edge of every row — rounded and shaded so it
 * looks inlaid instead of painted on.
 */
@Composable
fun SignalBar(color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier
            .width(5.dp)
            .fillMaxHeight()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
            .background(
                Brush.horizontalGradient(listOf(color.darkenBy(0.22f), color, color.lightenBy(0.22f))),
            ),
    )
}

@Composable
fun MedicineCard(
    medicine: MedicineUi,
    onOpen: () -> Unit,
    onSignal: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StackLaborTheme.colors
    val opacity = if (medicine.active) 1f else 0.38f
    RaisedPanel(
        modifier = modifier.fillMaxWidth().height(56.dp),
        elevation = if (medicine.active) 12.dp else 4.dp,
        rimAlpha = if (medicine.active) 0.7f else 0.3f,
        bevelStrength = if (medicine.active) 1f else 0.35f,
        background = if (medicine.active) null else Brush.verticalGradient(listOf(colors.background, colors.background)),
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignalBar(if (medicine.active) medicine.signal.color() else colors.disabled, onClick = onSignal)
            Column(Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 10.dp, vertical = 7.dp).alpha(opacity)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SolubilityMarks(medicine.solubility)
                    Spacer(Modifier.width(6.dp))
                    Text(medicine.name, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(medicine.dose, Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1)
                    if (medicine.reason.isNotEmpty()) Text(medicine.reason, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = medicine.signal.color(), maxLines = 1)
                }
            }
            IconTouchButton(if (medicine.active) "${medicine.name} deaktivieren" else "${medicine.name} aktivieren", onToggle) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).then(
                        if (medicine.active) Modifier.background(colors.accent) else Modifier.border(1.dp, colors.disabled, RoundedCornerShape(5.dp)),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (medicine.active) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = colors.onAccent)
                }
            }
        }
    }
}

@Composable
fun CatalogRow(title: String, meta: String, onOpen: () -> Unit, onEdit: (() -> Unit)? = null) {
    val colors = StackLaborTheme.colors
    RaisedPanel(Modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.Center) {
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1)
            }
            if (onEdit != null) IconTouchButton("$title bearbeiten", onEdit) { Icon(Icons.Default.Edit, null, Modifier.size(20.dp)) }
        }
    }
}

/** Inlaid check box — sunken when empty, raised gold when ticked. */
@Composable
fun WerftCheckbox(checked: Boolean, enabledTint: Color = StackLaborTheme.colors.accent) {
    val colors = StackLaborTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .then(if (checked) Modifier.depthShadow(shape, 6.dp) else Modifier)
            .size(22.dp)
            .clip(shape)
            .then(
                if (checked) {
                    Modifier
                        .background(
                            Brush.verticalGradient(listOf(enabledTint.lightenBy(0.35f), enabledTint, enabledTint.darkenBy(0.25f))),
                        )
                        .border(1.dp, enabledTint.lightenBy(0.45f), shape)
                } else {
                    Modifier
                        .background(Brush.verticalGradient(listOf(colors.background.darkenBy(0.05f), colors.surface)))
                        .border(1.dp, colors.border, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, null, Modifier.size(17.dp), tint = colors.onAccent)
    }
}

@Composable
fun GoalRow(
    goal: GoalUi,
    expanded: Boolean,
    draggable: Boolean,
    onClick: () -> Unit,
    onToggle: (() -> Unit)? = null,
    onDrag: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = StackLaborTheme.colors
    RaisedPanel(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
                if (onToggle != null) {
                    Box(Modifier.requiredSize(44.dp).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                        WerftCheckbox(goal.selected)
                    }
                }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .depthShadow(CircleShape, 4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(colors.elevated.lightenBy(0.18f), colors.elevated.darkenBy(0.08f))))
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(goal.rank.toString(), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                }
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(goal.text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
                if (draggable && onDrag != null) IconTouchButton("${goal.text} verschieben", onDrag) { Icon(Icons.Default.DragHandle, null, tint = colors.textMuted) }
                else Spacer(Modifier.width(16.dp))
            }
            AnimatedVisibility(
                visible = expanded && goal.reason.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Text(
                    goal.reason,
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(colors.elevated.darkenBy(0.05f), colors.elevated)))
                        .padding(12.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
fun SettingsRow(label: String, value: String = "", trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val colors = StackLaborTheme.colors
    RaisedPanel(Modifier.fillMaxWidth().height(64.dp), onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            if (value.isNotEmpty()) Text(value, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted)
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        color = StackLaborTheme.colors.textMuted,
    )
}

@Composable
fun BreathingFab(description: String, animationsEnabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "fabBreathing")
        val value by transition.animateFloat(1f, 1.02f, infiniteRepeatable(tween(3_200), RepeatMode.Reverse), label = "fabScale")
        value
    } else 1f
    GoldSurface(
        modifier = modifier.size(56.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(28.dp),
        elevation = 18.dp,
        onClick = onClick,
    ) { Icon(Icons.Default.Add, null, tint = Color(0xFF2A1B05)) }
}

@Composable
fun BottomSheetFrame(
    onDismiss: () -> Unit,
    underlay: @Composable () -> Unit,
    heightFraction: Float = 0.70f,
    fixedHeight: Dp? = null,
    showGrip: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    var dismissRequested by remember { mutableStateOf(false) }
    val motionEnabled = StackLaborTheme.motionEnabled
    LaunchedEffect(Unit) { entered = true }
    LaunchedEffect(dismissRequested) {
        if (dismissRequested) {
            entered = false
            if (motionEnabled) delay(300)
            onDismiss()
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (motionEnabled) 300 else 0,
            easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
        ),
        label = "sheetProgress",
    )
    Box(Modifier.fillMaxSize()) {
        underlay()
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f * progress))
                .clickable(enabled = !dismissRequested) { dismissRequested = true },
        )
        Surface(
            Modifier.fillMaxWidth().then(if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier.fillMaxHeight(heightFraction))
                .align(Alignment.BottomCenter)
                .depthShadow(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), 26.dp, strength = 1.4f)
                .graphicsLayer { translationY = size.height * (1f - progress) },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = StackLaborTheme.colors.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.5.dp, metalRim(0.9f)),
        ) {
            Column(Modifier.imePadding()) {
                if (showGrip) {
                    Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(StackLaborTheme.colors.textMuted.copy(alpha = 0.6f)))
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun AdaptiveSplit(
    narrow: @Composable () -> Unit,
    primary: @Composable RowScope.() -> Unit,
    secondary: @Composable RowScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) narrow()
        else Row(Modifier.fillMaxSize()) {
            Row(Modifier.weight(0.42f).fillMaxHeight(), content = primary)
            Box(Modifier.width(1.dp).fillMaxHeight().background(StackLaborTheme.colors.border))
            Row(Modifier.weight(0.58f).fillMaxHeight(), content = secondary)
        }
    }
}

@Composable
fun EmptyState(title: String, action: String, modifier: Modifier = Modifier, onAction: () -> Unit) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        PrimaryAction(action, onAction)
    }
}

@Composable
fun CloseButton(onClick: () -> Unit) = IconTouchButton("Schließen", onClick) { Icon(Icons.Default.Close, null) }

@Composable
fun AddButton(description: String, onClick: () -> Unit) = IconTouchButton(description, onClick) { Icon(Icons.Default.Add, null) }

@Composable
fun EditButton(description: String, onClick: () -> Unit) = IconTouchButton(description, onClick) { Icon(Icons.Default.Edit, null) }

@Composable
fun BackButton(onClick: () -> Unit) = IconTouchButton("Zurück", onClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }

@Composable
fun MoreButton(onClick: () -> Unit) = IconTouchButton("Weitere Optionen", onClick) { Icon(Icons.Default.MoreVert, null) }

@Composable
fun SignalState.color(): Color = when (this) {
    SignalState.Green -> StackLaborTheme.colors.green
    SignalState.Yellow -> StackLaborTheme.colors.yellow
    SignalState.Red -> StackLaborTheme.colors.red
    SignalState.Gray -> StackLaborTheme.colors.gray
}

val SignalState.label: String
    get() = when (this) {
        SignalState.Green -> "Ampel grün"
        SignalState.Yellow -> "Ampel gelb"
        SignalState.Red -> "Ampel rot"
        SignalState.Gray -> "nicht bedient"
    }

/**
 * Die Löslichkeit als Wort statt als Punkt — wasserlöslich grün, fettlöslich orange.
 *
 * „Beides“ steht in beiden Farben nebeneinander, damit die Farbzuordnung überall dieselbe
 * bleibt und man nicht raten muss, wofür eine dritte Farbe stünde.
 */
@Composable
fun SolubilityLabel(solubility: Solubility, modifier: Modifier = Modifier) {
    val style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        when (solubility) {
            Solubility.Water -> Text("wasserlöslich", style = style, color = waterColor(), maxLines = 1)
            Solubility.Fat -> Text("fettlöslich", style = style, color = fatColor(), maxLines = 1)
            Solubility.Both -> {
                Text("wasser", style = style, color = waterColor(), maxLines = 1)
                Text(" + ", style = style, color = StackLaborTheme.colors.textMuted, maxLines = 1)
                Text("fettlöslich", style = style, color = fatColor(), maxLines = 1)
            }
        }
    }
}

/** Grün für wasserlöslich — im dunklen Thema aufgehellt, damit es lesbar bleibt. */
@Composable
private fun waterColor(): Color = if (StackLaborTheme.dark) Color(0xFF4ADE80) else Color(0xFF15803D)

/** Orange für fettlöslich. */
@Composable
private fun fatColor(): Color = if (StackLaborTheme.dark) Color(0xFFFB923C) else Color(0xFFC2410C)

@Composable
fun SolubilityMarks(solubility: Solubility) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (solubility == Solubility.Water || solubility == Solubility.Both) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(StackLaborTheme.colors.water))
        }
        if (solubility == Solubility.Fat || solubility == Solubility.Both) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(StackLaborTheme.colors.fat).border(1.5.dp, StackLaborTheme.colors.fatBorder, CircleShape))
        }
    }
}

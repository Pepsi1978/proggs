package de.frank.fisetinbegleiter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.fisetinbegleiter.BuildConfig
import de.frank.fisetinbegleiter.data.IngredientEntity
import de.frank.fisetinbegleiter.data.IngredientPhase
import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import de.frank.fisetinbegleiter.ui.theme.AppThemeVariant
import de.frank.fisetinbegleiter.ui.theme.LocalAppColors

@Composable
fun MoreScreen(
    state: MainUiState,
    themeVariant: AppThemeVariant,
    onThemeVariantChange: (AppThemeVariant) -> Unit,
    onSaveProtocol: (ProtocolTemplateEntity) -> Unit,
    onSaveIngredient: (IngredientEntity) -> Unit,
    onDeleteIngredient: (IngredientEntity) -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val locked = state.activeCure != null
    var protocol by remember(state.protocol) { mutableStateOf(state.protocol) }
    var editedIngredient by remember { mutableStateOf<IngredientEntity?>(null) }
    var ingredientDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("System", "Berechtigungen und Zuverlässigkeit der App.") }
        item { ThemeFamilySelector(themeVariant, onThemeVariantChange) }
        item {
            Text("Standard-Protokoll", modifier = Modifier.padding(top = 8.dp), color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Änderungen gelten für neue Abläufe.", color = colors.sub, fontSize = 12.5.sp)
        }
        if (locked) {
            item {
                StatusNotice(
                    "Bearbeitung während der Kur gesperrt",
                    "So bleiben geplante Alarme und Zeitfenster konsistent. Nach Kurabschluss wieder änderbar.",
                    colors.warnBg,
                    colors.warn,
                )
            }
        }
        item {
            ProtocolNumberRow("Standarddauer (Tage)", protocol.standardDurationDays, 1, 2, 3, !locked) {
                protocol = protocol.copy(standardDurationDays = it)
            }
        }
        item {
            ProtocolNumberRow("Antioxidantien-Sperre (Min.)", protocol.antioxidantBlockMinutes, 15, 60, 480, !locked) {
                protocol = protocol.copy(antioxidantBlockMinutes = it)
            }
        }
        item {
            ProtocolNumberRow("Mahlzeit bis Minute", protocol.mealDeadlineMinutes, 5, 5, 120, !locked) {
                protocol = protocol.copy(mealDeadlineMinutes = it)
            }
        }
        item {
            ProtocolNumberRow("Mahlzeit-Vorwarnung (Min.)", protocol.mealWarningMinutes, 5, 0, 120, !locked) {
                protocol = protocol.copy(mealWarningMinutes = it)
            }
        }
        item {
            ProtocolNumberRow("Spermidin-Fenster ab Minute", protocol.spermidinStartMinutes, 15, 30, 240, !locked) {
                protocol = protocol.copy(spermidinStartMinutes = it)
            }
        }
        item {
            ProtocolNumberRow("Spermidin-Nachfassung (Min.)", protocol.spermidinReminderMinutes, 15, 30, 240, !locked) {
                protocol = protocol.copy(spermidinReminderMinutes = it)
            }
        }
        item {
            GlassCard(shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Erinnerungen", color = colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Exakte Alarme für die Kur-Schritte", color = colors.sub, fontSize = 12.sp)
                    }
                    DesignToggle(
                        checked = protocol.remindersEnabled,
                        enabled = !locked,
                        onCheckedChange = { protocol = protocol.copy(remindersEnabled = it) },
                    )
                }
            }
        }
        item {
            AccentGradientButton(
                onClick = { onSaveProtocol(protocol.validatedForDesign()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !locked,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
            ) {
                Text("Protokollwerte speichern", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { Text("Zutaten", modifier = Modifier.padding(top = 8.dp), color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        IngredientPhase.entries.forEach { phase ->
            val phaseItems = state.ingredients.filter { it.phase == phase }
            if (phaseItems.isNotEmpty()) {
                item {
                    Text(
                        if (phase == IngredientPhase.DRINK) "FISETIN-DRINK" else "SPERMIDIN + LEICHTE MAHLZEIT",
                        modifier = Modifier.padding(top = 4.dp),
                        color = colors.sub,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                }
                items(phaseItems, key = { ingredientListKey(it.id) }) { ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        canEdit = !locked,
                        onEdit = {
                            editedIngredient = ingredient
                            ingredientDialog = true
                        },
                        onDelete = { onDeleteIngredient(ingredient) },
                    )
                }
            }
        }
        item {
            DashedAddButton(
                enabled = !locked,
                onClick = {
                    editedIngredient = null
                    ingredientDialog = true
                },
            )
        }
        item {
            DesignOutlineButton(
                text = "Akku-Optimierung für zuverlässige Erinnerungen prüfen",
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                "App-Version ${BuildConfig.VERSION_NAME} · Stand ${BuildConfig.VERSION_BUMPED_AT}",
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                color = colors.sub,
                fontSize = 11.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (ingredientDialog) {
        IngredientEditorSheet(
            item = editedIngredient,
            nextSort = state.ingredients.size + 1,
            onDismiss = { ingredientDialog = false },
            onSave = {
                onSaveIngredient(it)
                ingredientDialog = false
            },
        )
    }
}

@Composable
private fun ThemeFamilySelector(themeVariant: AppThemeVariant, onThemeVariantChange: (AppThemeVariant) -> Unit) {
    val colors = LocalAppColors.current
    Text("THEME-FAMILIE", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePill("Aurora", AppThemeVariant.AURORA, Color(0xFF6A5CFF), themeVariant, onThemeVariantChange, Modifier.weight(1f))
        ThemePill("Vital", AppThemeVariant.VITAL, Color(0xFF0C8F74), themeVariant, onThemeVariantChange, Modifier.weight(1f))
        ThemePill("Ember", AppThemeVariant.EMBER, Color(0xFFE0562F), themeVariant, onThemeVariantChange, Modifier.weight(1f))
    }
}

@Composable
private fun ThemePill(
    label: String,
    variant: AppThemeVariant,
    dotColor: Color,
    selectedVariant: AppThemeVariant,
    onSelected: (AppThemeVariant) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalAppColors.current
    val selected = variant == selectedVariant
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.chip)
            .border(1.dp, if (selected) colors.accent else colors.line, shape)
            .clickable { onSelected(variant) }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).shadow(8.dp, CircleShape, ambientColor = dotColor, spotColor = dotColor).clip(CircleShape).background(dotColor))
        Text(label, color = if (selected) colors.accent else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProtocolNumberRow(
    label: String,
    value: Int,
    step: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onValue: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    GlassCard(shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            NumberButton("−", enabled && value > min) { onValue((value - step).coerceAtLeast(min)) }
            Text(value.toString(), modifier = Modifier.size(width = 44.dp, height = 32.dp).padding(top = 5.dp), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            NumberButton("+", enabled && value < max) { onValue(if (value > max - step) max else value + step) }
        }
    }
}

@Composable
private fun NumberButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(11.dp)
    Box(
        Modifier.size(32.dp).clip(shape).background(colors.chip).border(1.dp, colors.line, shape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.text.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DesignToggle(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    val thumbOffset by animateDpAsState(if (checked) 19.dp else 0.dp, tween(300), label = "reminder-thumb")
    val offColor by animateColorAsState(colors.chip, tween(300), label = "reminder-off")
    Box(
        Modifier
            .size(width = 46.dp, height = 27.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (checked) Brush.linearGradient(listOf(colors.accent, colors.accent2)) else Brush.linearGradient(listOf(offColor, offColor)))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(2.5.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.offset(x = thumbOffset).size(22.dp).shadow(3.dp, CircleShape, ambientColor = colors.shadow.color, spotColor = colors.shadow.color).clip(CircleShape).background(Color.White),
        )
    }
}

@Composable
private fun IngredientRow(ingredient: IngredientEntity, canEdit: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalAppColors.current
    GlassCard(shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${ingredient.name} · ${ingredient.amount}", color = colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(if (ingredient.optional) "optional" else null, ingredient.note.takeIf { it.isNotBlank() }).joinToString(" · "),
                    color = colors.sub,
                    fontSize = 11.5.sp,
                )
            }
            if (canEdit) {
                SmallActionButton("Ändern", onEdit)
                FlatDesignButton("Löschen", onDelete, background = colors.badBg, contentColor = colors.bad)
            }
        }
    }
}

@Composable
private fun DashedAddButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.chip)
            .drawBehind {
                drawRoundRect(
                    color = colors.sub,
                    topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
                    size = Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Zutat hinzufügen", color = colors.text.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IngredientEditorSheet(item: IngredientEntity?, nextSort: Int, onDismiss: () -> Unit, onSave: (IngredientEntity) -> Unit) {
    val colors = LocalAppColors.current
    var name by remember(item) { mutableStateOf(item?.name.orEmpty()) }
    var amount by remember(item) { mutableStateOf(item?.amount.orEmpty()) }
    var note by remember(item) { mutableStateOf(item?.note.orEmpty()) }
    var phase by remember(item) { mutableStateOf(item?.phase ?: IngredientPhase.DRINK) }
    var optional by remember(item) { mutableStateOf(item?.optional ?: false) }
    val valid = name.isNotBlank() && amount.isNotBlank()

    FbBottomSheet(onDismiss) {
        Text(if (item == null) "Zutat hinzufügen" else "Zutat bearbeiten", color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        DesignTextField(name, { name = it }, "Name")
        DesignTextField(amount, { amount = it }, "Menge")
        DesignTextField(note, { note = it }, "Hinweis")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionButton("Drink", phase == IngredientPhase.DRINK, { phase = IngredientPhase.DRINK }, Modifier.weight(1f))
            SelectionButton("Spermidin", phase == IngredientPhase.SPERMIDIN, { phase = IngredientPhase.SPERMIDIN }, Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().clickable { optional = !optional },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesignCheckbox(optional) { optional = it }
            Text("Optional", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DesignOutlineButton("Abbrechen", onDismiss, Modifier.weight(1f), verticalPadding = 14.dp)
            AccentGradientButton(
                onClick = {
                    onSave(
                        IngredientEntity(
                            id = item?.id ?: 0,
                            phase = phase,
                            name = name.trim(),
                            amount = amount.trim(),
                            note = note.trim(),
                            optional = optional,
                            sortOrder = item?.sortOrder ?: nextSort,
                        ),
                    )
                },
                enabled = valid,
                modifier = Modifier.weight(1.4f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(14.dp),
            ) { Text("Speichern", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun DesignTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val colors = LocalAppColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.chip).border(1.dp, colors.line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
        singleLine = true,
        textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = colors.sub, fontSize = 14.sp)
                inner()
            }
        },
    )
}

internal fun ingredientListKey(id: Long): String = "ingredient:$id"

internal fun ProtocolTemplateEntity.validatedForDesign(): ProtocolTemplateEntity = copy(
    standardDurationDays = standardDurationDays.coerceIn(2, 3),
    antioxidantBlockMinutes = antioxidantBlockMinutes.coerceIn(60, 480),
    mealDeadlineMinutes = mealDeadlineMinutes.coerceIn(5, 120),
    mealWarningMinutes = mealWarningMinutes.coerceIn(0, 120),
    spermidinStartMinutes = spermidinStartMinutes.coerceIn(30, 240),
    spermidinReminderMinutes = spermidinReminderMinutes.coerceIn(30, 240),
)

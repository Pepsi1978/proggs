package de.frank.entropyreducer.presentation.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.ThemeMode
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.widget.EntropyReducerWidget
import de.frank.entropyreducer.presentation.widget.WidgetDarkPalette
import de.frank.entropyreducer.presentation.widget.WidgetLightPalette
import de.frank.entropyreducer.presentation.widget.resolveWidgetPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Widget-Einstellungen (Frank-Wunsch 2026-05-11).
 *
 * Drei Modi: SYSTEM (folgt Geraet), LIGHT (immer hell), DARK (immer dunkel).
 * Beim Wechsel sofort:
 *  1. In DataStore speichern
 *  2. Alle Widget-Instanzen auf dem Homescreen neu rendern (updateAll)
 *
 * Zusaetzlich ein Hinweis, dass die Widget-Optik bewusst etwas vom App-Design
 * abweicht (Glas-Effekt geht in Android-Widgets technisch nicht).
 */
@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val settings: AppSettings,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.widgetThemeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val onlyToday: StateFlow<Boolean> = settings.widgetOnlyTodayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setWidgetThemeMode(mode) }
    }

    fun setOnlyToday(value: Boolean) {
        viewModelScope.launch { settings.setWidgetOnlyToday(value) }
    }
}

@Composable
fun WidgetSettingsScreen(
    onBack: () -> Unit,
    vm: WidgetSettingsViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val mode by vm.themeMode.collectAsState()
    val onlyToday by vm.onlyToday.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun setAndRefresh(newMode: ThemeMode) {
        vm.setTheme(newMode)
        scope.launch {
            // Widget sofort neu rendern damit der Theme-Wechsel sichtbar wird.
            // runCatching weil das Update fehlschlagen kann wenn das Widget
            // noch nicht auf dem Homescreen liegt.
            runCatching { EntropyReducerWidget().updateAll(context) }
        }
    }

    fun setOnlyTodayAndRefresh(value: Boolean) {
        vm.setOnlyToday(value)
        scope.launch {
            runCatching { EntropyReducerWidget().updateAll(context) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top-Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Zurück")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Widget-Einstellungen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Hier wählst du, ob das Aufgaben-Widget hell oder dunkel aussehen soll. " +
                    "Diese Einstellung ist getrennt vom App-Theme — du kannst die App " +
                    "dunkel lassen und das Widget hell anzeigen.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )

            ThemeOptionRow(
                icon = Icons.Outlined.BrightnessAuto,
                label = "System",
                description = "Folgt dem Geräte-Theme (hell/dunkel)",
                isActive = mode == ThemeMode.SYSTEM,
                onClick = { setAndRefresh(ThemeMode.SYSTEM) },
            )
            ThemeOptionRow(
                icon = Icons.Outlined.Brightness7,
                label = "Hell",
                description = "Heller Hintergrund mit dunklem Text — auch wenn das Gerät dunkel ist",
                isActive = mode == ThemeMode.LIGHT,
                onClick = { setAndRefresh(ThemeMode.LIGHT) },
            )
            ThemeOptionRow(
                icon = Icons.Outlined.Brightness4,
                label = "Dunkel",
                description = "Dunkler Hintergrund mit hellem Text — auch wenn das Gerät hell ist",
                isActive = mode == ThemeMode.DARK,
                onClick = { setAndRefresh(ThemeMode.DARK) },
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "Anzeige-Filter",
                style = MaterialTheme.typography.titleMedium,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Hier waehlst du, ob das Widget alle Aufgaben oder nur die heutigen anzeigt. " +
                    "Die Einstellung wirkt sofort nach dem Wechsel.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )

            ThemeOptionRow(
                icon = Icons.Outlined.RadioButtonUnchecked,
                label = "Alle Aufgaben",
                description = "Zeigt alle Buckets (Heute, Morgen, Freiblock, Spaeter) — Standard",
                isActive = !onlyToday,
                onClick = { setOnlyTodayAndRefresh(false) },
            )
            ThemeOptionRow(
                icon = Icons.Outlined.RadioButtonChecked,
                label = "Nur Heute",
                description = "Zeigt nur die fuer heute geplanten Aufgaben — fuer den Tagesfokus",
                isActive = onlyToday,
                onClick = { setOnlyTodayAndRefresh(true) },
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Vorschau",
                style = MaterialTheme.typography.titleMedium,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            val palette = resolveWidgetPalette(context, mode)
            WidgetPreviewCard(
                bgColor = palette.bgRoot,
                surfaceColor = palette.surfaceCard,
                tintColor = palette.bucketHeute.copy(alpha = palette.cardTintAlpha),
                textPrimary = palette.textPrimary,
                textSecondary = palette.textSecondary,
                accentColor = palette.bucketHeute,
                prioColor = palette.prioOrange,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Hinweis: Android-Widgets können den Glas-Effekt der App nicht " +
                    "darstellen — die Karten haben deshalb einen festen Hintergrund " +
                    "statt einer transparenten Glas-Optik. Alle anderen Farben und " +
                    "Layouts entsprechen der App.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    icon: ImageVector,
    label: String,
    description: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) cosmos.glassBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else cosmos.textSecondary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = cosmos.textPrimary,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
        }
        Icon(
            imageVector = if (isActive) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else cosmos.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun WidgetPreviewCard(
    bgColor: Color,
    surfaceColor: Color,
    tintColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    prioColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(12.dp),
    ) {
        Column {
            // Mini-Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aufgaben",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "3 offen",
                    color = textSecondary,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            // Mini-Bucket-Header
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "Heute",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            // Mini-Karte
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tintColor)
                        .padding(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(accentColor.copy(alpha = 0.18f)),
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Beispiel-Aufgabe",
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "So sieht eine Karte im Widget aus",
                                color = textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "75",
                            color = prioColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

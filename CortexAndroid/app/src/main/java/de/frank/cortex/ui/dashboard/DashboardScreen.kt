package de.frank.cortex.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.cortex.data.model.BrainEntry
import de.frank.cortex.data.model.CategoryInfo
import de.frank.cortex.ui.chat.CategoryPickerPill
import de.frank.cortex.ui.common.VpnSleepOverlay
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(vm: DashboardViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val vpnState by WireGuardManager.state.collectAsState()

    // Dashboard-Polling nur laufen lassen, solange dieser Screen wirklich sichtbar ist. Beim
    // Wegnavigieren (z.B. in den Chat) wird der Composable disposed -> screenActive=false -> der
    // 20s-Poll ruht. Sonst verstopften die Hintergrund-Polls den WireGuard-Mobilfunk-Tunnel und
    // erzeugten "Fehler Timeout"-Toasts, waehrend man auf die Agent-Antwort wartete (2026-07-01).
    DisposableEffect(Unit) {
        vm.setScreenActive(true)
        onDispose { vm.setScreenActive(false) }
    }

    // VPN Sleep Overlay (design: blur + "Dein Gehirn schläft")
    if (vpnState != TunnelState.CONNECTED) {
        VpnSleepOverlay(onActivateVpn = { WireGuardManager.connect() })
        return
    }

    if (uiState.selectedEntry != null) {
        EntryDetailScreen(
            entry = uiState.selectedEntry!!,
            isEditing = uiState.isEditing,
            editText = uiState.editText,
            editTitle = uiState.editTitle,
            editCategory = uiState.editCategory,
            categories = uiState.categories,
            onEditTextChange = vm::updateEditText,
            onEditTitleChange = vm::updateEditTitle,
            onEditCategoryChange = vm::updateEditCategory,
            onCreateCategory = vm::createCategory,
            onSaveCategory = vm::saveEntryCategory,
            onToggleEdit = vm::toggleEditing,
            onCancelEdit = vm::cancelEditing,
            onSave = vm::saveEntry,
            onDelete = vm::deleteEntry,
            onBack = vm::clearSelection
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Spectrum Card (exakt wie Design)
        item {
            SpectrumCard(
                totalEntries = uiState.totalEntries,
                categoryCounts = uiState.categoryCounts,
                selectedCategory = uiState.selectedCategory,
                categoryPath = uiState.categoryPath,
                subcategories = uiState.subcategories,
                onCategoryClick = vm::selectCategory,
                onDrillInto = vm::drillIntoCategory,
                onDrillBack = vm::drillBack,
                onDrillToRoot = vm::drillToRoot
            )
        }

        // Vitals 2x2 Grid
        uiState.overview?.let { overview ->
            item {
                VitalsGrid(overview = overview)
            }
        }

        // Search Bar
        item {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { vm.updateSearchQuery(it); vm.search(it) },
                onClear = { vm.updateSearchQuery(""); vm.search("") }
            )
        }

        // Results
        val displayItems = if (uiState.searchQuery.isNotBlank()) uiState.searchResults
        else if (uiState.selectedCategory != null) uiState.browseResults
        else emptyList()

        if (displayItems.isEmpty() && (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SearchOff, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Nichts gefunden.", fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(displayItems) { entry ->
            EntryCard(entry = entry, onClick = { vm.selectEntry(entry) })
        }
    }
}

@Composable
private fun SpectrumCard(
    totalEntries: Int,
    categoryCounts: Map<String, Int>,
    selectedCategory: String?,
    categoryPath: List<String>,
    subcategories: Map<String, Int>,
    onCategoryClick: (String?) -> Unit,
    onDrillInto: (String) -> Unit,
    onDrillBack: (Int) -> Unit,
    onDrillToRoot: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val spectrumCounts = if (categoryPath.isEmpty()) categoryCounts else subcategories
    val spectrumItems = spectrumCounts.entries.sortedByDescending { it.value }
    val total = spectrumCounts.values.sum().toFloat().coerceAtLeast(1f)

    // Animated count
    val animatedCount = remember { Animatable(0f) }
    LaunchedEffect(totalEntries) {
        animatedCount.animateTo(
            targetValue = totalEntries.toFloat(),
            animationSpec = tween(1100, easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f))
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            // Radial glow (top-right)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(x = 100.dp, y = (-30).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Iris.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column {
                Text(
                    text = "GEDÄCHTNIS-SPEKTRUM",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "${animatedCount.value.toInt()}",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 52.sp,
                        lineHeight = 52.sp,
                        letterSpacing = (-1).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "EINTRÄGE\nGESAMT",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stacked bar (16px height, radius 8, gap 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) DarkField else LightField),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    spectrumItems.forEachIndexed { index, entry ->
                        val (name, count) = entry
                        val fraction = count / total
                        val color = spectrumColor(index)
                        Box(
                            modifier = Modifier
                                .weight(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(color, color.copy(alpha = 0.72f))
                                    )
                                )
                                .clickable {
                                    if (categoryPath.isEmpty()) {
                                        onDrillInto(name)
                                    } else {
                                        onCategoryClick(if (selectedCategory == name) null else name)
                                    }
                                }
                        )
                    }
                }

                // Breadcrumb (wenn drilldown aktiv)
                if (categoryPath.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Root",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Iris,
                            modifier = Modifier.clickable { onDrillToRoot() }
                        )
                        categoryPath.forEachIndexed { index, segment ->
                            Text("›", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = segment,
                                fontSize = 11.sp,
                                fontWeight = if (index == categoryPath.lastIndex) FontWeight.Bold else FontWeight.Normal,
                                color = if (index == categoryPath.lastIndex) MaterialTheme.colorScheme.onSurface else Iris,
                                modifier = Modifier.clickable { onDrillBack(index + 1) }
                            )
                        }
                    }
                }

                // Legend: entweder Hauptkategorien (Root) oder Subcategories (Drilldown)
                val legendItems = if (categoryPath.isEmpty()) {
                    categoryCounts.entries.sortedByDescending { it.value }
                } else {
                    subcategories.entries.sortedByDescending { it.value }
                }

                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    legendItems.forEachIndexed { index, entry ->
                        val (name, count) = entry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (categoryPath.isEmpty()) {
                                        onDrillInto(name)
                                    } else {
                                        onDrillInto(name)
                                    }
                                }
                                .padding(vertical = 7.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(spectrumColor(index))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = name,
                                fontSize = 13.5.sp,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$count",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private val SpectrumColors = listOf(
    Color(0xFF7B7BF5), // Iris
    Color(0xFFF97316), // Orange
    Color(0xFF4FD1B0), // Mint
    Color(0xFFF2698E), // Rose
    Color(0xFF5AB0F2), // Sky
    Color(0xFFF2B65A), // Amber
    Color(0xFFB68CF5), // Lavender
    Color(0xFF3FD0D6), // Teal
    Color(0xFFE25555), // Red
    Color(0xFF9BD05A), // Lime
    Color(0xFF5A7BF2), // Blue
    Color(0xFFF25AC0), // Pink
    Color(0xFFD0C45A)  // Gold
)

private fun spectrumColor(index: Int): Color = SpectrumColors[index % SpectrumColors.size]

@Composable
private fun VitalsGrid(overview: de.frank.cortex.data.model.OverviewResponse) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val meterColor: (Float) -> Color = { p ->
        when {
            p >= 0.8f -> Rose
            p >= 0.6f -> Amber
            else -> Mint
        }
    }

    data class VitalItem(
        val icon: String, val label: String,
        val value: String, val unit: String? = null,
        val sub: String, val isStatus: Boolean = false,
        val dot: Color? = null, val meter: Float? = null
    )

    val vitals = listOf(
        VitalItem("smart_toy", "Assistent",
            if (overview.agent?.status == "ok") "Bereit" else "Offline",
            sub = "${overview.agent?.model ?: "?"} \u00B7 ${overview.agent?.sessions ?: 0} Sitzungen",
            isStatus = true, dot = if (overview.agent?.status == "ok") Mint else Rose),
        VitalItem("memory", "Prozessor",
            "${overview.server?.cpu_pct?.toInt() ?: 0}", "%",
            "8 Kerne", meter = ((overview.server?.cpu_pct ?: 0.0) / 100.0).toFloat()),
        VitalItem("developer_board", "Arbeitsspeicher",
            "${overview.server?.mem_pct?.toInt() ?: 0}", "%",
            "${formatBytes(overview.server?.mem_used)} / ${formatBytes(overview.server?.mem_total)}",
            meter = ((overview.server?.mem_pct ?: 0.0) / 100.0).toFloat()),
        VitalItem("hard_drive", "Speicherplatz",
            "${overview.server?.disk_pct?.toInt() ?: 0}", "%",
            "${formatBytes(overview.server?.disk_used)} / ${formatBytes(overview.server?.disk_total)}",
            meter = ((overview.server?.disk_pct ?: 0.0) / 100.0).toFloat())
    )

    // 2x2 Grid (design: gap 11)
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        for (row in vitals.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                row.forEach { v ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 8.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(13.dp, 14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(v.label.uppercase(), fontFamily = JetBrainsMono,
                                    fontSize = 9.5.sp, letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (v.isStatus && v.dot != null) {
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(v.dot))
                                }
                                Text(v.value, fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface)
                                if (v.unit != null) {
                                    Text(v.unit, fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (v.meter != null) {
                                LinearProgressIndicator(
                                    progress = { v.meter },
                                    modifier = Modifier.fillMaxWidth().height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = meterColor(v.meter),
                                    trackColor = if (isDark) DarkField else LightField
                                )
                            }
                            Text(v.sub, fontFamily = JetBrainsMono, fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp,
            if (query.isNotEmpty()) Iris else MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(46.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(Icons.Default.Search, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Im Gehirn suchen \u2014 z.B. \u201Ameine Ziele\u2018\u2026",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                Icon(Icons.Default.Close, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).clickable(onClick = onClear))
            }
        }
    }
}

@Composable
private fun CategoryChips(
    counts: Map<String, Int>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Chip("Alle", null, selected == null, isDark) { onSelect(null) }
        }
        items(counts.keys.toList()) { name ->
            Chip(name, categoryColor(name), selected == name, isDark) {
                onSelect(if (selected == name) null else name)
            }
        }
    }
}

@Composable
private fun Chip(name: String, color: Color?, active: Boolean, isDark: Boolean, onClick: () -> Unit) {
    val col = color ?: Iris
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) col.copy(alpha = 0.16f) else if (isDark) DarkChip else LightChip,
        border = BorderStroke(1.dp,
            if (active) col else if (isDark) DarkChipBorder else LightChipBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.height(32.dp).padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (color != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(col))
            }
            Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                color = if (active) col else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EntryCard(entry: BrainEntry, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.title ?: "Ohne Titel",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.score != null) {
                    Text(
                        text = "${(entry.score * 100).toInt()}%",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Mint
                    )
                }
            }
            // Category tag
            entry.category?.let {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = categoryColor(it).copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(categoryColor(it)))
                        Text(it, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = categoryColor(it))
                    }
                }
            }
            val snippet = entry.match ?: entry.text
            if (snippet != null) {
                Text(
                    text = snippet.take(200).replace("\n", " "),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EntryDetailScreen(
    entry: BrainEntry,
    isEditing: Boolean,
    editText: String,
    editTitle: String,
    editCategory: String?,
    categories: List<CategoryInfo>,
    onEditTextChange: (String) -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditCategoryChange: (String?) -> Unit,
    onCreateCategory: (String) -> Unit,
    onSaveCategory: () -> Unit,
    onToggleEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(entry.category ?: "Ohne Kategorie")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            if (isEditing) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) DarkField else LightField,
                    border = BorderStroke(1.dp, Iris)
                ) {
                    BasicTextField(
                        value = editTitle,
                        onValueChange = onEditTitleChange,
                        modifier = Modifier.fillMaxWidth().padding(13.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 21.sp,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { inner ->
                            if (editTitle.isBlank()) Text("Titel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                }
            } else {
                Text(
                    text = entry.title ?: "Ohne Titel",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            Text(
                text = formatEntryTimestamp(entry.updated_at ?: entry.created_at),
                fontFamily = JetBrainsMono,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    CategoryPickerPill(
                        categories = categories,
                        selectedCategory = editCategory ?: entry.category,
                        onCategoryChange = onEditCategoryChange,
                        onCreateCategory = onCreateCategory,
                        isDark = isDark
                    )
                    DetailButton(
                        text = "Kategorie speichern",
                        icon = Icons.Default.Save,
                        color = Mint,
                        modifier = Modifier.weight(1f),
                        onClick = onSaveCategory
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DetailButton(
                        text = "Löschen",
                        icon = Icons.Default.Delete,
                        color = Rose,
                        modifier = Modifier.weight(1f),
                        onClick = { showDeleteConfirm = true }
                    )
                    DetailButton(
                        text = "Bearbeiten",
                        icon = Icons.Default.Edit,
                        color = Iris,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleEdit
                    )
                }
                if (isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        DetailButton(
                            text = "Abbrechen",
                            icon = Icons.Default.Close,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            onClick = onCancelEdit
                        )
                        DetailButton(
                            text = "Speichern",
                            icon = Icons.Default.Check,
                            color = Mint,
                            modifier = Modifier.weight(1f),
                            onClick = onSave
                        )
                    }
                }
            }
        }

        item {
            if (isEditing) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) DarkField else LightField,
                    border = BorderStroke(1.dp, Iris)
                ) {
                    BasicTextField(
                        value = editText,
                        onValueChange = onEditTextChange,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 220.dp).padding(14.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            lineHeight = 21.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) DarkField else LightField,
                    border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder)
                ) {
                    Text(
                        text = editText,
                        modifier = Modifier.padding(15.dp),
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (showDeleteConfirm) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Rose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Rose.copy(alpha = 0.32f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Diesen Eintrag wirklich löschen?", fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(11.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Button(
                                onClick = { showDeleteConfirm = false },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) DarkChip else LightChip),
                                border = BorderStroke(1.dp, if (isDark) DarkChipBorder else LightChipBorder)
                            ) { Text("Abbrechen", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp) }
                            Button(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Rose)
                            ) { Text("Löschen", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    val color = categoryColor(category)
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Text(
                category.substringAfterLast("/"),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun DetailButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = color.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f)),
        modifier = modifier.height(44.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = color, maxLines = 1)
        }
    }
}

private fun formatEntryTimestamp(raw: String?): String {
    if (raw.isNullOrBlank()) return "Datum unbekannt"
    return try {
        val normalized = if (raw.endsWith("Z") || raw.contains("+")) raw else raw + "Z"
        val dt = OffsetDateTime.parse(normalized)
        val date = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY).format(dt)
        val time = DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.GERMANY).format(dt)
        "$date, $time"
    } catch (_: Exception) {
        raw.replace('T', ' ').substringBeforeLast('.')
    }
}

private fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "?"
    return when {
        bytes >= 1_073_741_824 -> "${"%.1f".format(bytes / 1_073_741_824.0)} GB"
        bytes >= 1_048_576 -> "${"%.0f".format(bytes / 1_048_576.0)} MB"
        else -> "${"%.0f".format(bytes / 1024.0)} KB"
    }
}

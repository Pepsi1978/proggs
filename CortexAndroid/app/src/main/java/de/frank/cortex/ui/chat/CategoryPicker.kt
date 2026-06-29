package de.frank.cortex.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.cortex.data.model.CategoryInfo
import de.frank.cortex.ui.theme.*

/* ============================================================================
 * Hierarchischer Kategorie-Picker — 1:1 wie der "Gespraech"-Drilldown im Web-Cortex.
 * Die Kategorien kommen als flache Liste vom Server (agent /categories/detail),
 * Namen koennen Pfade sein ("Haupt/Unter/..."). Daraus wird ein Baum gebaut.
 * Verhalten: Spezial-Eintraege flach (Auto, + Neue Kategorie), darunter pro Knoten
 * eine aufklappbare Zeile; aufgeklappt zeigt sie "<Name> waehlen" + Kinder (rekursiv)
 * + "+ Unterkategorie...". Neue Hauptkategorie oben, neue Unterkategorie pro Knoten.
 * ============================================================================ */

class CatNode(
    val seg: String,
    val path: String,
    var isReal: Boolean,
    val children: LinkedHashMap<String, CatNode> = LinkedHashMap()
)

/** Baut aus der flachen Kategorienliste (Namen evtl. mit "/") einen Pfadbaum, alphabetisch. */
fun buildCategoryTree(categories: List<CategoryInfo>): List<CatNode> {
    val roots = LinkedHashMap<String, CatNode>()
    categories.sortedBy { it.name.lowercase() }.forEach { cat ->
        val parts = cat.name.split("/").map { it.trim() }.filter { it.isNotEmpty() }
        var level = roots
        var pathSoFar = ""
        parts.forEachIndexed { idx, seg ->
            pathSoFar = if (pathSoFar.isEmpty()) seg else "$pathSoFar/$seg"
            val node = level.getOrPut(seg.lowercase()) { CatNode(seg, pathSoFar, false) }
            if (idx == parts.lastIndex) node.isReal = true
            level = node.children
        }
    }
    return roots.values.toList()
}

private sealed class CatRow {
    data class Node(val node: CatNode, val depth: Int, val expanded: Boolean, val hasKids: Boolean) : CatRow()
    data class Use(val node: CatNode, val depth: Int) : CatRow()
    data class AddSub(val node: CatNode, val depth: Int) : CatRow()
}

private fun flattenVisible(
    nodes: List<CatNode>,
    expanded: Set<String>,
    depth: Int = 0,
    out: MutableList<CatRow> = mutableListOf()
): List<CatRow> {
    nodes.forEach { node ->
        val isExp = expanded.contains(node.path)
        val hasKids = node.children.isNotEmpty()
        out.add(CatRow.Node(node, depth, isExp, hasKids))
        if (isExp) {
            if (node.isReal) out.add(CatRow.Use(node, depth + 1))
            flattenVisible(node.children.values.toList(), expanded, depth + 1, out)
            out.add(CatRow.AddSub(node, depth + 1))
        }
    }
    return out
}

private fun cap(s: String): String =
    s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@Composable
fun CategoryPickerPill(
    categories: List<CategoryInfo>,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    onCreateCategory: (String) -> Unit,
    onOpen: () -> Unit = {},
    isDark: Boolean
) {
    var open by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateListOf<String>() }
    // dialogParent: null = kein Dialog; "" = neue Hauptkategorie; "a/b" = neue Unterkategorie darunter
    var dialogParent by remember { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }

    val tree = remember(categories) { buildCategoryTree(categories) }
    val rows = flattenVisible(tree, expanded.toSet())

    fun toggle(path: String) {
        if (expanded.contains(path)) expanded.remove(path) else expanded.add(path)
    }

    Box {
        val catColor = if (selectedCategory == null) MaterialTheme.colorScheme.onSurfaceVariant
        else categoryColor(selectedCategory)
        val catBorder = if (selectedCategory == null) (if (isDark) DarkChipBorder else LightChipBorder)
        else catColor.copy(alpha = 0.45f)

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (isDark) DarkChip else LightChip,
            border = BorderStroke(1.dp, catBorder),
            modifier = Modifier.clickable { if (!open) onOpen(); open = !open }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(catColor))
                Text(
                    text = selectedCategory?.let { cap(it.substringAfterLast("/")) } ?: "Auto",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.ExpandMore, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = if (isDark) DarkSurface else Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, if (isDark) DarkBorder else LightBorder),
            modifier = Modifier
                .widthIn(min = 320.dp, max = 380.dp)
                .heightIn(max = 460.dp)
        ) {
            // Spezial: Auto-Kategorie
            DropdownMenuItem(
                modifier = Modifier.heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                text = {
                    Text(
                        "Auto-Kategorie",
                        color = if (selectedCategory == null) Iris else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = { onCategoryChange(null); open = false }
            )

            // Kategorie-Baum (Drilldown)
            rows.forEach { row ->
                when (row) {
                    is CatRow.Node -> DropdownMenuItem(
                        modifier = Modifier.heightIn(min = 36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(start = (row.depth * 10).dp)
                            ) {
                                if (row.hasKids) {
                                    Icon(
                                        Icons.Default.ChevronRight, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp).rotate(if (row.expanded) 90f else 0f)
                                    )
                                } else {
                                    Spacer(Modifier.size(16.dp))
                                }
                                Box(Modifier.size(8.dp).clip(CircleShape).background(categoryColor(row.node.path)))
                                Text(
                                    cap(row.node.seg),
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (selectedCategory == row.node.path) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (row.hasKids) {
                                    Text(
                                        "${row.node.children.size}",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            // Hat Kinder ODER nur Zwischenknoten -> aufklappen. Echtes Blatt -> direkt waehlen.
                            if (row.hasKids || !row.node.isReal) toggle(row.node.path)
                            else {
                                onCategoryChange(row.node.path); open = false
                            }
                        }
                    )

                    is CatRow.Use -> DropdownMenuItem(
                        modifier = Modifier.heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        text = {
                            Text(
                                cap(row.node.seg) + " waehlen",
                                modifier = Modifier.padding(start = (row.depth * 10).dp),
                                color = Iris,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        },
                        onClick = { onCategoryChange(row.node.path); open = false }
                    )

                    is CatRow.AddSub -> DropdownMenuItem(
                        modifier = Modifier.heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        text = {
                            Text(
                                "+ Unterkategorie...",
                                modifier = Modifier.padding(start = (row.depth * 10).dp),
                                color = Mint,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            dialogParent = row.node.path
                            newName = ""
                            open = false
                        }
                    )
                }
            }

            // Spezial: neue Hauptkategorie
            DropdownMenuItem(
                modifier = Modifier.heightIn(min = 36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                text = { Text("+ Neue Kategorie...", color = Iris, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                onClick = {
                    dialogParent = ""
                    newName = ""
                    open = false
                }
            )
        }
    }

    // Anlege-Dialog (Haupt- ODER Unterkategorie, je nach dialogParent)
    val parent = dialogParent
    if (parent != null) {
        val isSub = parent.isNotEmpty()
        val invalidSlash = isSub && newName.contains("/")
        AlertDialog(
            onDismissRequest = { dialogParent = null },
            title = { Text(if (isSub) "Neue Unterkategorie" else "Neue Kategorie") },
            text = {
                Column {
                    Text(
                        if (isSub) "Unter " + cap(parent.substringAfterLast("/")) + " - ohne Schraegstrich (eine Ebene)."
                        else "Tipp: Haupt/Unter legt direkt eine Unterkategorie an.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        isError = invalidSlash,
                        placeholder = { Text(if (isSub) "Name der Unterkategorie" else "Name der Kategorie") }
                    )
                    if (invalidSlash) {
                        Text(
                            "Bitte ohne / - eine Ebene pro Schritt.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank() && !invalidSlash,
                    onClick = {
                        val name = newName.trim()
                        val full = if (isSub) "$parent/$name" else name
                        if (isSub && !expanded.contains(parent)) expanded.add(parent)
                        dialogParent = null
                        onCreateCategory(full)
                    }
                ) { Text("Anlegen") }
            },
            dismissButton = {
                TextButton(onClick = { dialogParent = null }) { Text("Abbrechen") }
            }
        )
    }
}

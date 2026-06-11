package com.bestjournal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bestjournal.app.R

/**
 * Wiederverwendbare Bausteine fuer den zentralen KI-Tageskontingent-Dialog
 * (Premium-Stufen, Wartezeiten, Free-Limits, Reset-Zeitpunkt, Burst-Schutz).
 *
 * EINZIGE Quelle der Wahrheit fuer den Inhalt: die Strings `ai_limits_dialog_title`,
 * `ai_limits_dialog_body`, `ai_limits_dialog_close` in `res/values/strings.xml`. Wer den Text
 * aendern will, aendert NUR diese String-Ressourcen — alle drei UI-Stellen
 * (Paywall-Disclaimer-Zeile, Paywall-Free-Note, Settings-Premium-Bereich) zeigen automatisch den
 * neuen Text.
 *
 * Bereitgestellte Composables:
 * - [AiLimitsInfoIcon]: alleinstehendes Info-Icon (Settings-Premium-Banner, Paywall-Header).
 * - [AiLimitsDisclaimerRow]: Disclaimer-Zeile — KOMPLETTE Zeile klickbar (Text + Icon).
 * - [AiLimitsFreeNote]: Free-Tier-Note unter "One-Time-Purchase" — KOMPLETTE Sektion klickbar.
 */

// ────────────────────────── Public API ──────────────────────────

@Composable
fun AiLimitsInfoIcon(
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    var showDialog by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size((iconSize.value + 12f).dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = stringResource(R.string.ai_limits_dialog_title),
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
    if (showDialog) AiLimitsDialog(onDismiss = { showDialog = false })
}

@Composable
fun AiLimitsDisclaimerRow(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { showDialog = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = stringResource(R.string.ai_limits_dialog_title),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.ai_limits_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
    if (showDialog) AiLimitsDialog(onDismiss = { showDialog = false })
}

@Composable
fun AiLimitsFreeNote(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { showDialog = true }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.ai_limits_dialog_title),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.paywall_free_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.paywall_with_limited),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (showDialog) AiLimitsDialog(onDismiss = { showDialog = false })
}

// ────────────────────────── Dialog ──────────────────────────

/**
 * Strukturierter Dialog mit Section-Header, Hanging-Indent-Bullets, dezenten Trennern und
 * Premium-Akzent. Parst den zentralen String aus `strings.xml` zeilenweise — der Inhalt bleibt
 * unveraendert, nur das Rendering wird aufgewertet.
 */
@Composable
private fun AiLimitsDialog(onDismiss: () -> Unit) {
    val body = stringResource(R.string.ai_limits_dialog_body)
    val sections = remember(body) { parseDialogBody(body) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.ai_limits_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                sections.forEachIndexed { idx, section ->
                    when (section) {
                        is DialogSection.Header -> SectionHeader(section.text)
                        is DialogSection.SubHeader -> SubHeader(section.text)
                        is DialogSection.Paragraph -> Paragraph(section.text)
                        is DialogSection.Bullet -> BulletLine(section.text)
                        is DialogSection.Divider -> SectionDivider()
                    }
                    // Kleines Atmen zwischen dem letzten Bullet einer Gruppe und dem naechsten Block
                    if (
                        section is DialogSection.Bullet &&
                        idx + 1 < sections.size &&
                        sections[idx + 1] !is DialogSection.Bullet
                    ) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ai_limits_dialog_close))
            }
        },
    )
}

// ────────────────────────── Dialog-Bausteine ──────────────────────────

@Composable
private fun SectionHeader(text: String) {
    val isPremium = text.contains("PREMIUM", ignoreCase = true)
    val color =
        if (isPremium) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    val bgAlpha = if (isPremium) 0.14f else 0.06f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = bgAlpha))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun BulletLine(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

// ────────────────────────── Body-Parser ──────────────────────────

private sealed interface DialogSection {
    data class Header(val text: String) : DialogSection
    data class SubHeader(val text: String) : DialogSection
    data class Paragraph(val text: String) : DialogSection
    data class Bullet(val text: String) : DialogSection
    data object Divider : DialogSection
}

/**
 * Heuristisches Parsen des zentralen Body-Strings.
 *
 * Regeln (in dieser Reihenfolge):
 * - Leerzeilen → ignoriert (Spacing kommt aus den Composables)
 * - Zeile besteht nur aus Box-Drawing-Chars (z.B. ──────────) → [DialogSection.Divider]
 * - Zeile beginnt mit `•` → [DialogSection.Bullet] (Marker entfernt)
 * - Zeile ist vollstaendig uppercase und kurz (4..40 Zeichen) → [DialogSection.Header]
 * - Zeile endet auf `?` oder `:` → [DialogSection.SubHeader]
 * - Sonst → [DialogSection.Paragraph]
 *
 * Bei nicht-deutschen Uebersetzungen kann die Header/SubHeader-Erkennung weniger zuverlaessig
 * sein. Fallback ist immer [DialogSection.Paragraph] — der Inhalt geht nicht verloren, nur das
 * Styling fehlt.
 */
private fun parseDialogBody(body: String): List<DialogSection> {
    val out = mutableListOf<DialogSection>()
    body.split('\n').forEach { rawLine ->
        val line = rawLine.trim()
        when {
            line.isEmpty() -> Unit
            line.all { it == '─' || it == '—' || it == '-' || it.isWhitespace() } &&
                line.isNotBlank() -> out += DialogSection.Divider
            line.startsWith("•") -> out += DialogSection.Bullet(line.removePrefix("•").trim())
            line == line.uppercase() && line.length in 4..40 -> out += DialogSection.Header(line)
            line.endsWith("?") || line.endsWith(":") -> out += DialogSection.SubHeader(line)
            else -> out += DialogSection.Paragraph(line)
        }
    }
    return out
}

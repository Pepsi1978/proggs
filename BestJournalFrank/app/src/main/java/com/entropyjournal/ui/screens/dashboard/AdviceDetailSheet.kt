package com.entropyjournal.ui.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.entropyjournal.domain.model.Advice
import com.entropyjournal.ui.components.NeonDivider
import com.entropyjournal.ui.theme.CosmosDeep
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.TextPrimary
import com.entropyjournal.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdviceDetailSheet(
    advice: Advice,
    categoryName: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CosmosDeep,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = advice.title,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            NeonDivider(horizontalPadding = 0.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = advice.description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verkn\u00fcpfung",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = advice.connection,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

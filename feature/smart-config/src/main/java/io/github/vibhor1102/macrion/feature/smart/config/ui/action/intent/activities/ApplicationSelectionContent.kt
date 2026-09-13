/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.activities

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.vibhor1102.macrion.core.android.application.AndroidApplicationInfo
import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape
import io.github.vibhor1102.macrion.feature.smart.config.R

@Composable
internal fun ApplicationSelectionContent(
    title: String,
    applications: List<AndroidApplicationInfo>?,
    onDismiss: () -> Unit,
    onSelected: (AndroidApplicationInfo) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
        shape = OverlayDialogShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
                }
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            HorizontalDivider()
            if (applications == null) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(applications, key = { it.componentName.flattenToString() }) { application ->
                        ApplicationRow(application) { onSelected(application) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationRow(application: AndroidApplicationInfo, onClick: () -> Unit) {
    val icon = remember(application.componentName) { application.icon.toBitmap().asImageBitmap() }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(application.name, style = MaterialTheme.typography.titleMedium)
            Text(
                application.componentName.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

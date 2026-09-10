/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy.DumbActionDetails

@Composable
internal fun DumbActionListItem(
    details: DumbActionDetails,
    showHandle: Boolean,
    reorderHandleModifier: Modifier = Modifier,
    isBeingDragged: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(end = 24.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showHandle) {
            Box(
                Modifier.size(48.dp)
                    .background(if (isBeingDragged) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                    .then(reorderHandleModifier),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_reorder),
                    contentDescription = stringResource(R.string.content_desc_drag_and_drop),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(16.dp))
        ActionText(
            details = details,
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            titleMaxLines = 1,
            textAlign = TextAlign.Start,
        )
        Image(
            painter = painterResource(details.icon),
            contentDescription = stringResource(R.string.content_desc_action_icon),
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
internal fun ActionText(
    details: DumbActionDetails,
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal,
    titleMaxLines: Int,
    textAlign: TextAlign,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text = details.name,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            style = legacyTextStyle,
        )
        Text(
            text = details.detailsText,
            modifier = Modifier.fillMaxWidth(),
            color = if (details.haveError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            maxLines = if (titleMaxLines == 1) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            style = legacyTextStyle,
        )
        details.repeatCountText?.let { repeatText ->
            Text(
                text = repeatText,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                maxLines = if (titleMaxLines == 1) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                style = legacyTextStyle,
            )
        }
    }
}

private val legacyTextStyle = androidx.compose.ui.text.TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = true),
)

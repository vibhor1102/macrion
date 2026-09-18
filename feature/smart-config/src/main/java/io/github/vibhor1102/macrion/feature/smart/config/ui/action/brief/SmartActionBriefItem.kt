/* Copyright (C) 2024 Kevin Buzeau — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.UiAction

@Composable
internal fun SmartActionBriefItem(
    details: UiAction,
    orientation: Int,
    onClick: () -> Unit,
    onSubActionClick: ((Int) -> Unit)? = null,
) {
    val portrait = orientation == Configuration.ORIENTATION_PORTRAIT
    val isSplit = details.subUiActions.isNotEmpty()

    Box(Modifier.fillMaxSize(), if (portrait) Alignment.BottomCenter else Alignment.CenterStart) {
        if (isSplit) {
            ElevatedCard(
                modifier = if (portrait) {
                    Modifier.fillMaxWidth().height(80.dp)
                } else {
                    Modifier.width(200.dp).fillMaxHeight()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSubActionClick?.invoke(0) ?: onClick() },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        SmartActionSplitHalf(details.subUiActions[0])
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSubActionClick?.invoke(1) ?: onClick() },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        SmartActionSplitHalf(
                            details.subUiActions.getOrElse(1) { details.subUiActions[0] }
                        )
                    }
                }
            }
        } else {
            ElevatedCard(
                onClick = onClick,
                modifier = if (portrait) Modifier.fillMaxWidth().height(80.dp) else Modifier.width(124.dp).fillMaxHeight(),
            ) {
                if (portrait) SmartActionPortrait(details) else SmartActionLandscape(details)
            }
        }
    }
}

@Composable
private fun SmartActionSplitHalf(details: UiAction) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 4.dp),
        ) {
            BriefText(details.name, 15, true, 1)
            BriefText(details.description, 12, false, 1)
        }
        BriefIcon(details, Modifier.padding(end = 4.dp))
    }
}

@Composable private fun SmartActionPortrait(details: UiAction) {
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(start = 16.dp)) { BriefText(details.name, 17, true, 1); BriefText(details.description, 14, false, 1) }
        BriefIcon(details, Modifier.padding(end = 16.dp))
    }
}
@Composable private fun SmartActionLandscape(details: UiAction) {
    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BriefText(details.name, 17, true, 2, TextAlign.Center); Spacer(Modifier.height(8.dp)); BriefText(details.description, 14, false, 2, TextAlign.Center); Spacer(Modifier.height(8.dp)); BriefIcon(details)
    }
}
@Composable private fun BriefText(text: String, size: Int, title: Boolean, lines: Int, align: TextAlign? = null) = Text(text, fontSize = size.sp, fontWeight = if (title) FontWeight.Bold else FontWeight.Normal, fontStyle = if (title) FontStyle.Normal else FontStyle.Italic, maxLines = lines, overflow = TextOverflow.Ellipsis, textAlign = align)
@Composable private fun BriefIcon(details: UiAction, modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp)) {
        Icon(
            painter = painterResource(details.icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        if (details.haveError) Box(Modifier.align(Alignment.TopEnd).size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
    }
}

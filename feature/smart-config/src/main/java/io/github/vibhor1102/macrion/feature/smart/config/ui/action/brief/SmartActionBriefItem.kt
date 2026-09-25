/* Copyright (C) 2024 Kevin Buzeau — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchBriefCard
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchBriefChild
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchKind
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.UiAction

@Composable
internal fun SmartActionBriefItem(
    details: UiAction,
    orientation: Int,
    onClick: () -> Unit,
) {
    val portrait = orientation == Configuration.ORIENTATION_PORTRAIT
    val isSplit = details.action is SplitAction

    Box(Modifier.fillMaxSize(), if (portrait) Alignment.BottomCenter else Alignment.CenterStart) {
        if (isSplit) {
            MultiTouchBriefCard(
                name = details.name,
                icon = details.icon,
                children = details.subUiActions.map { child ->
                    val action = child.action
                    val kind = when (action) {
                        is Click -> MultiTouchKind.TAP
                        is Swipe -> MultiTouchKind.SWIPE
                        else -> MultiTouchKind.OTHER
                    }
                    MultiTouchBriefChild(
                        name = child.name,
                        icon = child.icon,
                        kind = kind,
                        durationMs = when (action) {
                            is Click -> action.pressDuration
                            is Swipe -> action.swipeDuration
                            else -> null
                        },
                        needsSetup = child.haveError || kind == MultiTouchKind.OTHER,
                    )
                },
                needsSetup = details.haveError,
                portrait = portrait,
                onClick = onClick,
                modifier = if (portrait) Modifier.fillMaxWidth().height(80.dp)
                    else Modifier.fillMaxWidth().fillMaxHeight(),
            )
        } else {
            ElevatedCard(
                onClick = onClick,
                modifier = if (portrait) Modifier.fillMaxWidth().height(80.dp) else Modifier.width(136.dp).fillMaxHeight(),
            ) {
                if (portrait) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            BriefText(details.name, 17, true, 1)
                            BriefText(details.description, 14, false, 1)
                        }
                        BriefIcon(details, Modifier.padding(end = 4.dp))
                    }
                } else {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            BriefText(details.name, 17, true, 2, TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            BriefText(details.description, 14, false, 2, TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            BriefIcon(details)
                        }
                    }
                }
            }
        }

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

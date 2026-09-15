/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose

import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.EntryPointAccessors
import io.github.vibhor1102.macrion.core.common.tutorial.di.TutorialEntryPoint
import io.github.vibhor1102.macrion.core.common.tutorial.domain.MonitoredViewsManager
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import kotlin.math.roundToInt

import androidx.compose.runtime.staticCompositionLocalOf

val LocalMonitoredViewsManager = staticCompositionLocalOf<MonitoredViewsManager?> { null }

@Composable
fun rememberMonitoredViewsManager(): MonitoredViewsManager? {
    val local = LocalMonitoredViewsManager.current
    if (local != null) return local
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                TutorialEntryPoint::class.java,
            ).monitoredViewsManager()
        }.getOrNull()
    }
}

/**
 * Attaches a Composable control to Macrion's Tutorial system.
 *
 * When a tutorial is active, this modifier reports its screen bounding rectangle to
 * [MonitoredViewsManager] using [Modifier.onGloballyPositioned], and registers [onClick]
 * so the tutorial spotlight can trigger clicks on behalf of the user.
 *
 * When no tutorial is running, monitoring checks short-circuit for zero overhead.
 */
@Composable
fun Modifier.tutorialAnchor(
    type: MonitoredViewType,
    monitoredViewsManager: MonitoredViewsManager? = rememberMonitoredViewsManager(),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
): Modifier {
    val manager = monitoredViewsManager ?: return this
    val isMonitoring by manager.isViewMonitoringEnabled().collectAsStateWithLifecycle()
    if (!isMonitoring) return this

    val currentClick by rememberUpdatedState(onClick)
    val view = LocalView.current

    DisposableEffect(type, enabled, currentClick) {
        if (enabled && currentClick != null) {
            manager.setClickHandler(type) { currentClick?.invoke() }
        }
        onDispose {
            manager.detach(type)
        }
    }

    return this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            val windowLocation = IntArray(2)
            view.getLocationOnScreen(windowLocation)
            val bounds = coordinates.boundsInRoot()
            val left = windowLocation[0] + bounds.left.roundToInt()
            val top = windowLocation[1] + bounds.top.roundToInt()
            val right = left + bounds.width.roundToInt()
            val bottom = top + bounds.height.roundToInt()
            manager.updatePosition(type, Rect(left, top, right, bottom))
        }
    }
}

/**
 * Reports real-time text input changes of a Compose text field to [MonitoredViewsManager]
 * for tutorial step conditions that await expected text/number input.
 */
@Composable
fun Modifier.tutorialTextAnchor(
    type: MonitoredViewType,
    text: String,
    monitoredViewsManager: MonitoredViewsManager? = rememberMonitoredViewsManager(),
): Modifier {
    val manager = monitoredViewsManager ?: return this
    val isMonitoring by manager.isViewMonitoringEnabled().collectAsStateWithLifecycle()
    if (!isMonitoring) return this

    LaunchedEffect(type, text) {
        manager.updateText(type, text)
    }

    DisposableEffect(type) {
        onDispose {
            manager.updateText(type, null)
        }
    }

    return this
}

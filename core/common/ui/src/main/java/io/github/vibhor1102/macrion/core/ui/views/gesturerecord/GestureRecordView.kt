/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.ui.views.gesturerecord

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.AbstractComposeView
import androidx.core.content.res.use

import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.core.display.di.DisplayEntryPoint
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.views.viewcomponents.DisplayBorderComponent
import io.github.vibhor1102.macrion.core.ui.views.viewcomponents.DisplayBorderComponentStyle
import io.github.vibhor1102.macrion.core.ui.views.viewcomponents.base.ViewComponent
import io.github.vibhor1102.macrion.core.ui.views.viewcomponents.base.ViewInvalidator

import dagger.hilt.EntryPoints


class GestureRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context), ViewInvalidator {

    private val displayConfigManager: DisplayConfigManager by lazy {
        EntryPoints.get(context.applicationContext, DisplayEntryPoint::class.java)
            .displayMetrics()
    }

    private val viewStyle = context
        .obtainStyledAttributes(null, R.styleable.GestureRecordView, R.attr.gestureRecordStyle, 0)
        .use { ta -> ta.getGestureRecorderStyle()}

    private val gestureRecorder = GestureRecorder { gesture, isFinished ->
        gestureCaptureListener?.invoke(gesture, isFinished)
    }

    private var renderVersion by mutableIntStateOf(0)

    var gestureCaptureListener: ((gesture: RecordedGesture?, isFinished: Boolean) -> Unit)? = null

    private val viewComponents: List<ViewComponent> = listOf(
        DisplayBorderComponent(
            viewStyle = DisplayBorderComponentStyle(
                displayConfigManager = displayConfigManager,
                color = viewStyle.color,
                thicknessPx = viewStyle.thicknessPx,
            ),
            viewInvalidator = this,
        ),
    )

    fun clearAndHide() {
        visibility = GONE
        gestureRecorder.clearCapture()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            viewComponents.forEach { it.onViewSizeChanged(w, h) }
            invalidate()
        }
    }

    override fun invalidate() {
        viewComponents.forEach { it.onInvalidate() }
        renderVersion++
        super.invalidate()
    }

    @Composable
    override fun Content() {
        renderVersion
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                viewComponents.forEach { it.onDraw(canvas.nativeCanvas) }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        return gestureRecorder.processEvent(event)
    }
}

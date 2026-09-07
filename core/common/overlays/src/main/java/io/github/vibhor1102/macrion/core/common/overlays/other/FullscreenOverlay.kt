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
package io.github.vibhor1102.macrion.core.common.overlays.other

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator

import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import io.github.vibhor1102.macrion.core.base.extensions.safeAddView
import io.github.vibhor1102.macrion.core.base.extensions.safeRemoveView
import io.github.vibhor1102.macrion.core.common.overlays.base.BaseOverlay
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager

/** BaseOverlay class for an overlay displayed as full screen. */
abstract class FullscreenOverlay(@StyleRes theme: Int? = null) : BaseOverlay(theme) {

    /** The layout parameters of the window displaying the view. */
    private val viewLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        OverlayManager.OVERLAY_WINDOW_TYPE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    )

    /** The Android window manager. Used to add/remove the overlay menu and view. */
    private lateinit var windowManager: WindowManager
    /** The root view for this overlay; created by implementation via [onCreateView]. */
    private lateinit var view: View

    /** Animator for the overlay showing. */
    private val showAnimator: Animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 250
        interpolator = LinearInterpolator()
        addUpdateListener {
            view.alpha = it.animatedValue as Float
        }
    }

    protected abstract fun onCreateView(layoutInflater: LayoutInflater): View

    protected abstract fun onViewCreated()

    final override fun onCreate() {
        windowManager = context.getSystemService(WindowManager::class.java)
        view = onCreateView(context.getSystemService(LayoutInflater::class.java))

        // WindowManager overlay roots don't inherit the Activity view-tree owners. Install this
        // overlay's owners before attaching the view so Compose can create its recomposer safely.
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setViewTreeViewModelStoreOwner(this)

        onViewCreated()
    }

    @CallSuper
    override fun onStart() {
        view.alpha = 0f

        if (windowManager.safeAddView(view, viewLayoutParams)) {
            showAnimator.start()
        } else {
            finish()
        }
    }

    @CallSuper
    override fun onStop() {
        windowManager.safeRemoveView(view)
    }
}

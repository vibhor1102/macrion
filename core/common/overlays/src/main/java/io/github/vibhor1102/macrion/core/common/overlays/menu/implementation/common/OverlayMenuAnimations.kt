/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.common

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.AndroidUiDispatcher
import io.github.vibhor1102.macrion.core.base.Dumpable
import java.io.PrintWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Compose's frame clock drives both WindowManager layers with the original fade timings. */
internal class OverlayMenuAnimations : Dumpable {
    private val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
    private var animation: Job? = null
    var showAnimationIsRunning = false
        private set
    var hideAnimationIsRunning = false
        private set

    fun startShowAnimation(view: View, overlayView: View? = null, onAnimationEnded: () -> Unit) {
        if (showAnimationIsRunning) return
        animate(view, overlayView, showing = true, onAnimationEnded)
    }

    fun startHideAnimation(view: View, overlayView: View? = null, onAnimationEnded: () -> Unit) {
        if (hideAnimationIsRunning) return
        animate(view, overlayView, showing = false, onAnimationEnded)
    }

    private fun animate(view: View, overlay: View?, showing: Boolean, onEnded: () -> Unit) {
        val wasAnimating = showAnimationIsRunning || hideAnimationIsRunning
        animation?.cancel()
        showAnimationIsRunning = showing
        hideAnimationIsRunning = !showing
        val initial = if (wasAnimating) view.alpha else if (showing) 0f else 1f
        view.alpha = initial
        overlay?.alpha = initial
        animation = scope.launch {
            Animatable(initial).animateTo(
                if (showing) 1f else 0f,
                tween(if (showing) 250 else 150, easing = Easing { 1f - (1f - it) * (1f - it) }),
            ) {
                view.alpha = value
                overlay?.alpha = value
            }
            showAnimationIsRunning = false
            hideAnimationIsRunning = false
            onEnded()
        }
    }

    fun release() { scope.cancel() }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        writer.append(prefix).println("showIsRunning=$showAnimationIsRunning; hideIsRunning=$hideAnimationIsRunning")
    }
}

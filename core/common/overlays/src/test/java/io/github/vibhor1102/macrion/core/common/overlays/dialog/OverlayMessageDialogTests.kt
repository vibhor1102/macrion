/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.dialog

import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class OverlayMessageDialogTests {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun confirmationUsesOverlayWindowAndLatestCallback() {
        val callbackVersion = mutableStateOf(0)
        val calls = mutableListOf<Int>()
        withConfirmation(callbackVersion = callbackVersion, onConfirm = { calls.add(it) }) { _, _ ->
            val dialog = ShadowDialog.getLatestDialog()
            compose.runOnIdle {
                assertTrue(dialog.isShowing)
                assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, dialog.window!!.attributes.type)
                callbackVersion.value = 1
            }
            compose.onNodeWithText("OK").performClick()
            compose.runOnIdle {
                assertEquals(listOf(1), calls)
                assertFalse(dialog.isShowing)
            }
        }
    }

    @Test fun cancelButtonAndSystemCancellationKeepDraftAndDisposeWindow() {
        var confirms = 0
        var cancellations = 0
        withConfirmation(onConfirm = { confirms++ }, onCancel = { cancellations++ }) { visible, _ ->
            compose.onNodeWithText("Cancel").performClick()
            compose.runOnIdle {
                assertEquals(1, cancellations)
                visible.value = true
            }
            compose.runOnIdle { ShadowDialog.getLatestDialog().cancel() }
            compose.runOnIdle {
                assertEquals(2, cancellations)
                visible.value = true
            }
            val dialog = compose.runOnIdle { ShadowDialog.getLatestDialog() }
            compose.runOnIdle { visible.value = false }
            compose.runOnIdle {
                assertFalse(dialog.isShowing)
                assertEquals(0, confirms)
                assertEquals("Disposal must not cancel the draft", 2, cancellations)
            }
        }
    }

    @Test fun parentStopHidesConfirmationAndRestartRestoresIt() {
        withConfirmation { _, owner ->
            val dialog = ShadowDialog.getLatestDialog()
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
            compose.runOnIdle {
                assertEquals(View.GONE, dialog.window!!.decorView.visibility)
                owner.registry.currentState = Lifecycle.State.STARTED
            }
            compose.runOnIdle {
                assertTrue(dialog.isShowing)
                assertEquals(View.VISIBLE, dialog.window!!.decorView.visibility)
                owner.registry.currentState = Lifecycle.State.DESTROYED
            }
            compose.runOnIdle { assertFalse(dialog.isShowing) }
        }
    }

    private class ParentOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private fun withConfirmation(
        callbackVersion: androidx.compose.runtime.MutableState<Int> = mutableStateOf(0),
        onConfirm: (Int) -> Unit = {},
        onCancel: () -> Unit = {},
        check: (androidx.compose.runtime.MutableState<Boolean>, ParentOwner) -> Unit,
    ) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val owner = ParentOwner().apply { registry.currentState = Lifecycle.State.STARTED }
        val visible = mutableStateOf(true)
        val host = ComposeView(activity).apply {
            setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    // Capture a new callback on recomposition, as the real editor does.
                    val version = callbackVersion.value
                    if (visible.value) OverlayMessageDialog(
                        title = "Discard changes?",
                        message = "The pending draft has not been saved.",
                        cancelLabel = android.R.string.cancel,
                        onConfirm = { visible.value = false; onConfirm(version) },
                        onDismissRequest = { visible.value = false; onCancel() },
                    )
                }
            }
        }
        try {
            activity.setContentView(host)
            controller.visible()
            compose.runOnIdle { }
            check(visible, owner)
        } finally {
            host.disposeComposition()
            controller.pause().stop().destroy()
        }
    }
}
